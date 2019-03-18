/*
 * Copyright © 2017-2018 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.wireguard.android.Application
import com.wireguard.android.BuildConfig
import com.wireguard.android.model.Tunnel
import com.wireguard.android.model.TunnelManager
import timber.log.Timber

class TaskerIntegrationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null || intent.action == null)
            return

        val manager = Application.tunnelManager
        val tunnelName: String? = intent.getStringExtra(TunnelManager.TUNNEL_NAME_INTENT_EXTRA)
        val integrationSecret: String? = intent.getStringExtra(TunnelManager.INTENT_INTEGRATION_SECRET_EXTRA)

        var state: Tunnel.State? = null
        Timber.tag("IntentReceiver")
        when (intent.action) {
            "${BuildConfig.APPLICATION_ID}.SET_TUNNEL_UP" -> {
                state = Tunnel.State.UP
            }
            "${BuildConfig.APPLICATION_ID}.SET_TUNNEL_DOWN" -> {
                state = Tunnel.State.DOWN
            }
            else -> Timber.d("Invalid intent action: ${intent.action}")
        }
        if (!Application.appPrefs.allowTaskerIntegration || Application.appPrefs.taskerIntegrationSecret.isEmpty()) {
            Timber.e("Tasker integration is disabled! Not allowing tunnel state change to pass through.")
            return
        }
        if (tunnelName != null && state != null && integrationSecret == Application.appPrefs.taskerIntegrationSecret) {
            Timber.d("Setting $tunnelName's state to $state")
            manager.getTunnels().thenAccept { tunnels ->
                val tunnel = tunnels[tunnelName]
                tunnel?.let {
                    manager.setTunnelState(it, state)
                }
            }
        } else if (tunnelName == null) {
            Timber.d("Intent parameter ${TunnelManager.TUNNEL_NAME_INTENT_EXTRA} not set!")
        } else {
            Timber.e("Intent integration secret mis-match! Exiting...")
        }
    }
}
