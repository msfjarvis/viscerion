/*
 * Copyright © 2018 Samuel Holland <samuel@sholland.org>
 * Copyright © 2018 Jason A. Donenfeld <Jason@zx2c4.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.wireguard.android.backend;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.wireguard.android.Application;
import com.wireguard.android.R;
import com.wireguard.android.activity.MainActivity;
import com.wireguard.android.model.Tunnel;
import com.wireguard.android.model.Tunnel.State;
import com.wireguard.android.model.Tunnel.Statistics;
import com.wireguard.android.model.TunnelManager;
import com.wireguard.config.Config;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import java9.util.stream.Collectors;
import java9.util.stream.Stream;

/**
 * WireGuard backend that uses {@code wg-quick} to implement tunnel configuration.
 */

public final class WgQuickBackend implements Backend {
    private static final String TAG = "WireGuard/" + WgQuickBackend.class.getSimpleName();
    @Nullable private final NotificationManager notificationManager;
    private final Context cachedContext;

    private final File localTemporaryDir;

    public WgQuickBackend(final Context context) {
        localTemporaryDir = new File(context.getCacheDir(), "tmp");
        cachedContext = context;
        notificationManager = (NotificationManager) cachedContext.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Override
    public String getVersion() throws Exception {
        final List<String> output = new ArrayList<>();
        if (Application.getRootShell()
                .run(output, "cat /sys/module/wireguard/version") != 0 || output.isEmpty())
            throw new Exception("Unable to determine kernel module version");
        return output.get(0);
    }

    @Override
    public String getTypeName() { return "Kernel module"; }

    @Override
    public Config applyConfig(final Tunnel tunnel, final Config config) throws Exception {
        if (tunnel.getState() == State.UP) {
            // Restart the tunnel to apply the new config.
            setStateInternal(tunnel, tunnel.getConfig(), State.DOWN);
            try {
                setStateInternal(tunnel, config, State.UP);
            } catch (final Exception e) {
                // The new configuration didn't work, so try to go back to the old one.
                setStateInternal(tunnel, tunnel.getConfig(), State.UP);
                throw e;
            }
        }
        return config;
    }

    @Override
    public Set<String> enumerate() {
        final List<String> output = new ArrayList<>();
        // Don't throw an exception here or nothing will show up in the UI.
        try {
            Application.getToolsInstaller().ensureToolsAvailable();
            if (Application.getRootShell().run(output, "wg show interfaces") != 0 || output.isEmpty())
                return Collections.emptySet();
        } catch (final Exception e) {
            Log.w(TAG, "Unable to enumerate running tunnels", e);
            return Collections.emptySet();
        }
        // wg puts all interface names on the same line. Split them into separate elements.
        return Stream.of(output.get(0).split(" ")).collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public State getState(final Tunnel tunnel) {
        return enumerate().contains(tunnel.getName()) ? State.UP : State.DOWN;
    }

    @Override
    public Statistics getStatistics(final Tunnel tunnel) {
        return new Statistics();
    }

    @Override
    public State setState(final Tunnel tunnel, State state) throws Exception {
        final State originalState = getState(tunnel);
        if (state == State.TOGGLE)
            state = originalState == State.UP ? State.DOWN : State.UP;
        if (state == originalState)
            return originalState;
        Log.d(TAG, "Changing tunnel " + tunnel.getName() + " to state " + state);
        postNotification(state, tunnel);
        Application.getToolsInstaller().ensureToolsAvailable();
        setStateInternal(tunnel, tunnel.getConfig(), state);
        return getState(tunnel);
    }

    private void setStateInternal(final Tunnel tunnel, @Nullable final Config config, final State state) throws Exception {
        Objects.requireNonNull(config, "Trying to set state with a null config");

        final File tempFile = new File(localTemporaryDir, tunnel.getName() + ".conf");
        try (final FileOutputStream stream = new FileOutputStream(tempFile, false)) {
            stream.write(config.toString().getBytes(StandardCharsets.UTF_8));
        }
        String command = String.format("wg-quick %s '%s'",
                state.toString().toLowerCase(), tempFile.getAbsolutePath());
        if (state == State.UP)
            command = "cat /sys/module/wireguard/version && " + command;
        final int result = Application.getRootShell().run(null, command);
        // noinspection ResultOfMethodCallIgnored
        tempFile.delete();
        if (result != 0)
            throw new Exception("Unable to configure tunnel (wg-quick returned " + result + ')');
    }

    private void postNotification(final State state, final Tunnel tunnel) {
        if (notificationManager == null)
            return;
        if (state == State.UP) {
            final Intent intent = new Intent(cachedContext, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            final PendingIntent pendingIntent = PendingIntent.getActivity(cachedContext, 0, intent,0);
            final NotificationCompat.Builder builder = new NotificationCompat.Builder(cachedContext,
                    TunnelManager.NOTIFICATION_CHANNEL_ID);
            builder.setContentTitle(cachedContext.getString(R.string.notification_channel_wgquick_title))
                    .setContentText(tunnel.getName())
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .setPriority(Notification.FLAG_ONGOING_EVENT)
                    .setSmallIcon(R.drawable.ic_stat_wgquick);
            notificationManager.notify(TunnelManager.NOTIFICATION_ID, builder.build());
        } else if (state == State.DOWN) {
            notificationManager.cancel(TunnelManager.NOTIFICATION_ID);
        }
    }
}
