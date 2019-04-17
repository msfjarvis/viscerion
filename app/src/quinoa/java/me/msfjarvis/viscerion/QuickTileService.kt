/*
 * Copyright © 2017-2018 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package me.msfjarvis.viscerion

import android.annotation.TargetApi
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import androidx.databinding.Observable
import androidx.databinding.Observable.OnPropertyChangedCallback
import com.wireguard.android.BR
import com.wireguard.android.R
import com.wireguard.android.model.Tunnel
import com.wireguard.android.model.Tunnel.State
import com.wireguard.android.model.TunnelManager
import com.wireguard.android.util.ErrorMessages
import org.koin.android.ext.android.inject
import timber.log.Timber

/**
 * Service that maintains the application's custom Quick Settings tile. This service is bound by the
 * system framework as necessary to update the appearance of the tile in the system UI, and to
 * forward click events to the application.
 */

@TargetApi(Build.VERSION_CODES.N)
class QuickTileService : TileService() {

    private val onStateChangedCallback = OnStateChangedCallback()
    private val onTunnelChangedCallback = OnTunnelChangedCallback()
    private val tunnelManager by inject<TunnelManager>()
    private var tunnel: Tunnel? = null

    /* This works around an annoying unsolved frameworks bug some people are hitting. */
    override fun onBind(intent: Intent): IBinder? {
        return try {
            super.onBind(intent)
        } catch (e: Exception) {
            Timber.d(e, "Failed to bind to TileService")
            null
        }
    }

    override fun onClick() {
        if (tunnel != null) {
            qsTile?.updateTile()
            tunnel?.setState(State.TOGGLE)?.whenComplete { _, throwable ->
                this.onToggleFinished(throwable)
            }
        }
    }

    override fun onStartListening() {
        tunnelManager.addOnPropertyChangedCallback(onTunnelChangedCallback)
        tunnel?.addOnPropertyChangedCallback(onStateChangedCallback)
        updateTile()
    }

    override fun onStopListening() {
        tunnel?.removeOnPropertyChangedCallback(onStateChangedCallback)
        tunnelManager.removeOnPropertyChangedCallback(onTunnelChangedCallback)
    }

    private fun onToggleFinished(throwable: Throwable?) {
        throwable ?: return
        val error = ErrorMessages[throwable]
        val message = getString(R.string.toggle_error, error)
        Timber.e(throwable)
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun updateTile() {
        // Update the tunnel.
        val newTunnel = tunnelManager.getLastUsedTunnel()
        if (newTunnel != tunnel) {
            tunnel?.removeOnPropertyChangedCallback(onStateChangedCallback)
            tunnel = newTunnel
            tunnel?.addOnPropertyChangedCallback(onStateChangedCallback)
        }
        // Update the tile contents.
        val tile = qsTile ?: return
        if (tunnel == null) {
            tile.label = getString(R.string.app_name)
            tile.state = Tile.STATE_UNAVAILABLE
        } else {
            tile.label = tunnel?.name
            tile.state = if (tunnel?.state == State.UP)
                Tile.STATE_ACTIVE
            else
                Tile.STATE_INACTIVE
            tile.subtitle = getString(R.string.app_name)
        }
        tile.updateTile()
    }

    private inner class OnStateChangedCallback : OnPropertyChangedCallback() {
        override fun onPropertyChanged(sender: Observable, propertyId: Int) {
            if (sender != tunnel) {
                sender.removeOnPropertyChangedCallback(this)
                return
            }
            if (propertyId != 0 && propertyId != BR.state)
                return
            updateTile()
        }
    }

    private inner class OnTunnelChangedCallback : OnPropertyChangedCallback() {
        override fun onPropertyChanged(sender: Observable, propertyId: Int) {
            if (propertyId != 0 && propertyId != BR.lastUsedTunnel)
                return
            updateTile()
        }
    }
}
