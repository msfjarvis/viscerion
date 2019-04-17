/*
 * Copyright © 2017-2018 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.providers

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import androidx.slice.Slice
import androidx.slice.SliceProvider
import androidx.slice.builders.ListBuilder
import androidx.slice.builders.SliceAction
import androidx.slice.builders.list
import androidx.slice.builders.row
import com.wireguard.android.BuildConfig
import com.wireguard.android.model.Tunnel
import com.wireguard.android.model.TunnelManager
import org.koin.android.ext.android.inject

class ViscerionSliceProvider : SliceProvider() {
    override fun onCreateSliceProvider(): Boolean {
        return true
    }

    override fun onBindSlice(sliceUri: Uri): Slice? {
        return when (sliceUri.path) {
            "/vpn" -> return createLastUsedTunnelSlice(sliceUri)
            else -> null
        }
    }

    private fun createLastUsedTunnelSlice(sliceUri: Uri): Slice? {
        val lastUsedTunnel = inject<TunnelManager>().value.getLastUsedTunnel()
        return if (lastUsedTunnel != null) {
            val isTunnelUp = lastUsedTunnel.state == Tunnel.State.UP
            val pendingIntent =
                    PendingIntent.getBroadcast(context, 0, createToggleIntent(lastUsedTunnel, isTunnelUp), 0)
            val sliceAction = SliceAction.createToggle(pendingIntent, "", isTunnelUp)
            list(requireNotNull(context), sliceUri, ListBuilder.INFINITY) {
                row {
                    title = lastUsedTunnel.name
                    primaryAction = sliceAction
                }
            }
        } else {
            null
        }
    }

    private fun createToggleIntent(tunnel: Tunnel, isUp: Boolean): Intent {
        return Intent().apply {
            `package` = BuildConfig.APPLICATION_ID
            action = "${BuildConfig.APPLICATION_ID}.SET_TUNNEL_${if (isUp) "DOWN" else "UP"}"
            putExtra(TunnelManager.TUNNEL_NAME_INTENT_EXTRA, tunnel.name)
        }
    }
}
