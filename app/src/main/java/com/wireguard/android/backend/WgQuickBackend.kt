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
import com.wireguard.android.util.requireNonNull
import com.wireguard.config.Config
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.util.ArrayList
import java.util.Locale

/**
 * WireGuard backend that uses `wg-quick` to implement tunnel configuration.
 */

class WgQuickBackend(private var context: Context) : Backend {

    private val localTemporaryDir: File = File(context.cacheDir, "tmp")
    private var notificationManager: NotificationManagerCompat = NotificationManagerCompat.from(context)

    @Throws(Exception::class)
    override fun getVersion(): String {
        val output = ArrayList<String>()
        if (Application.rootShell
                .run(output, "cat /sys/module/wireguard/version") != 0 || output.isEmpty()
        )
            throw Exception(context.getString(R.string.module_version_error))
        return output[0]
    }

    override fun getTypePrettyName(): String {
        return context.getString(R.string.type_name_kernel_module)
    }

    @Throws(Exception::class)
    override fun applyConfig(tunnel: Tunnel, config: Config): Config {
        if (tunnel.state == State.UP) {
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
            Timber.w(e, "Unable to enumerate running tunnels")
            return emptySet()
        }

        // wg puts all interface names on the same line. Split them into separate elements.
        return output[0].split(" ".toRegex()).toSet()
    }

    override fun getState(tunnel: Tunnel): State {
        return if (enumerate().contains(tunnel.name)) State.UP else State.DOWN
    }

    override fun getStatistics(tunnel: Tunnel): Statistics {
        return Statistics()
    }

    @Throws(Exception::class)
    override fun setState(tunnel: Tunnel, state: State): State {
        var stateToSet = state
        val originalState = getState(tunnel)
        if (stateToSet == State.TOGGLE)
            stateToSet = if (originalState == State.UP) State.DOWN else State.UP
        if (stateToSet == originalState)
            return originalState
        Timber.d("Changing tunnel %s to state %s", tunnel.name, stateToSet)
        Application.toolsInstaller.ensureToolsAvailable()
        setStateInternal(tunnel, tunnel.getConfig(), stateToSet)
        return getState(tunnel)
    }

    @Throws(Exception::class)
    private fun setStateInternal(tunnel: Tunnel?, config: Config?, state: State?) {
        config.requireNonNull<Config>("Trying to set state with a null config")

        val tempFile = File(localTemporaryDir, tunnel?.name + CONFIGURATION_FILE_SUFFIX)
        FileOutputStream(
            tempFile,
            false
        ).use { stream -> stream.write(config?.toWgQuickString()?.toByteArray(StandardCharsets.UTF_8)) }
        var command = String.format(
            "wg-quick %s '%s'",
            state.toString().toLowerCase(Locale.ENGLISH), tempFile.absolutePath
        )
        if (state == State.UP)
            command = "cat /sys/module/wireguard/version && $command"
        val result = Application.rootShell.run(null, command)

        tempFile.delete()
        when (result) {
            0 -> postNotification(state, tunnel)
            else -> throw Exception(context.getString(R.string.tunnel_config_error))
        }
    }

    private fun postNotification(state: State?, tunnel: Tunnel?) {
        if (tunnel == null || state == null)
            return
        if (state == State.UP) {
            val intent = Intent(context, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            val pendingIntent = PendingIntent.getActivity(context, 0, intent, 0)
            val builder = NotificationCompat.Builder(
                context,
                TunnelManager.NOTIFICATION_CHANNEL_ID
            )
            builder.setContentTitle(context.getString(R.string.notification_channel_wgquick_title))
                .setContentText(tunnel.name)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(Notification.FLAG_ONGOING_EVENT)
                .setSmallIcon(R.mipmap.ic_launcher)
            notificationManager.notify(tunnel.name.hashCode(), builder.build())
        } else if (state == State.DOWN) {
            notificationManager.cancel(tunnel.name.hashCode())
        }
    }
}
