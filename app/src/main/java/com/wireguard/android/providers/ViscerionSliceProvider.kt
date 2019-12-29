/*
 * Copyright © 2017-2019 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.providers

import android.app.PendingIntent
import android.net.Uri
import androidx.slice.Slice
import androidx.slice.SliceProvider
import androidx.slice.builders.ListBuilder
import androidx.slice.builders.SliceAction
import androidx.slice.builders.list
import androidx.slice.builders.row
import com.wireguard.android.di.injector
import com.wireguard.android.model.Tunnel
import com.wireguard.android.model.TunnelManager
import javax.inject.Inject

@Suppress("Slices")
class ViscerionSliceProvider : SliceProvider() {

    @Inject lateinit var tunnelManager: TunnelManager

    override fun onCreateSliceProvider(): Boolean {
        injector.inject(this)
        return true
    }

    override fun onBindSlice(sliceUri: Uri): Slice? {
        return when (sliceUri.path) {
            "/vpn" -> return createLastUsedTunnelSlice(sliceUri)
            else -> null
        }
    }

    private fun createLastUsedTunnelSlice(sliceUri: Uri): Slice? {
        val lastUsedTunnel = tunnelManager.getLastUsedTunnel()
        return if (lastUsedTunnel != null) {
            val isTunnelUp = lastUsedTunnel.state == Tunnel.State.UP
            val pendingIntent =
                    PendingIntent.getBroadcast(context, 0, lastUsedTunnel.createToggleIntent(), 0)
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
}
