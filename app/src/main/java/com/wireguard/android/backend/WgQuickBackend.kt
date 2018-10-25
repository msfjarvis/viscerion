/*
 * Copyright © 2017-2018 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.wireguard.android.backend

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.wireguard.android.Application
import com.wireguard.android.R
import com.wireguard.android.activity.MainActivity
import com.wireguard.android.configStore.FileConfigStore.Companion.CONFIGURATION_FILE_SUFFIX
import com.wireguard.android.model.Tunnel
import com.wireguard.android.model.Tunnel.State
import com.wireguard.android.model.Tunnel.Statistics
import com.wireguard.android.model.TunnelManager
import com.wireguard.config.Config
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.util.ArrayList
import java.util.Objects

/**
 * WireGuard backend that uses `wg-quick` to implement tunnel configuration.
 */

class WgQuickBackend(context: Context) : Backend {

    private val localTemporaryDir: File = File(context.cacheDir, "tmp")
    private var notificationManager: NotificationManagerCompat = NotificationManagerCompat.from(context)
    private var cachedContext: Context = context

    @Throws(Exception::class)
    override fun getVersion(): String {
        val output = ArrayList<String>()
        if (Application.rootShell
                .run(output, "cat /sys/module/wireguard/version") != 0 || output.isEmpty()
        )
            throw Exception("Unable to determine kernel module version")
        return output[0]
    }

    override fun getTypeName(): String {
        return "Kernel module"
    }

    @Throws(Exception::class)
    override fun applyConfig(tunnel: Tunnel?, config: Config?): Config? {
        if (tunnel?.state == State.UP) {
            // Restart the tunnel to apply the new config.
            setStateInternal(tunnel, tunnel.getConfig(), State.DOWN)
            try {
                setStateInternal(tunnel, config, State.UP)
            } catch (e: Exception) {
                // The new configuration didn't work, so try to go back to the old one.
                setStateInternal(tunnel, tunnel.getConfig(), State.UP)
                throw e
            }
        }
        return config
    }

    override fun enumerate(): Set<String> {
        val output = ArrayList<String>()
        // Don't throw an exception here or nothing will show up in the UI.
        try {
            Application.toolsInstaller.ensureToolsAvailable()
            if (Application.rootShell.run(output, "wg show interfaces") != 0 || output.isEmpty())
                return emptySet()
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Unable to enumerate running tunnels")
            return emptySet()
        }

        // wg puts all interface names on the same line. Split them into separate elements.
        return output[0].split(" ".toRegex()).toSet()
    }

    override fun getState(tunnel: Tunnel?): State? {
        return if (enumerate().contains(tunnel?.name)) State.UP else State.DOWN
    }

    override fun getStatistics(tunnel: Tunnel?): Statistics? {
        return Statistics()
    }

    @Throws(Exception::class)
    override fun setState(tunnel: Tunnel?, state: State?): State? {
        var stateToSet = state
        val originalState = getState(tunnel)
        if (stateToSet == State.TOGGLE)
            stateToSet = if (originalState == State.UP) State.DOWN else State.UP
        if (stateToSet == originalState)
            return originalState
        Timber.tag(TAG).d("Changing tunnel %s to state %s", tunnel?.name, stateToSet)
        Application.toolsInstaller.ensureToolsAvailable()
        setStateInternal(tunnel, tunnel?.getConfig(), stateToSet)
        return getState(tunnel)
    }

    @Throws(Exception::class)
    private fun setStateInternal(tunnel: Tunnel?, config: Config?, state: State?) {
        Objects.requireNonNull<Config>(config, "Trying to set state with a null config")

        val tempFile = File(localTemporaryDir, tunnel?.name + CONFIGURATION_FILE_SUFFIX)
        FileOutputStream(
            tempFile,
            false
        ).use { stream -> stream.write(config?.toString()?.toByteArray(StandardCharsets.UTF_8)) }
        var command = String.format(
            "wg-quick %s '%s'",
            state.toString().toLowerCase(), tempFile.absolutePath
        )
        if (state == State.UP)
            command = "cat /sys/module/wireguard/version && $command"
        val result = Application.rootShell.run(null, command)

        tempFile.delete()
        when (result) {
            0 -> postNotification(state, tunnel)
            else -> throw Exception("Unable to configure tunnel (wg-quick returned $result)")
        }
    }

    private fun postNotification(state: State?, tunnel: Tunnel?) {
        if (tunnel == null || state == null)
            return
        if (state == State.UP) {
            val intent = Intent(cachedContext, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            val pendingIntent = PendingIntent.getActivity(cachedContext, 0, intent, 0)
            val builder = NotificationCompat.Builder(
                cachedContext,
                TunnelManager.NOTIFICATION_CHANNEL_ID
            )
            builder.setContentTitle(cachedContext.getString(R.string.notification_channel_wgquick_title))
                .setContentText(tunnel.name)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(Notification.FLAG_ONGOING_EVENT)
                .setSmallIcon(R.drawable.ic_stat_wgquick)
            notificationManager.notify(tunnel.name.hashCode(), builder.build())
        } else if (state == State.DOWN) {
            notificationManager.cancel(tunnel.name.hashCode())
        }
    }

    companion object {
        private val TAG = "WireGuard/" + WgQuickBackend::class.java.simpleName
    }
}
