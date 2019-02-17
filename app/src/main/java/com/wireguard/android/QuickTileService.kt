/*
 * Copyright © 2017-2018 WireGuard LLC.
 * Copyright © 2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android

import android.annotation.TargetApi
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Icon
import android.os.Build
import android.os.IBinder
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import androidx.databinding.Observable
import androidx.databinding.Observable.OnPropertyChangedCallback
import com.wireguard.android.activity.MainActivity
import com.wireguard.android.model.Tunnel
import com.wireguard.android.model.Tunnel.State
import com.wireguard.android.util.ErrorMessages
import com.wireguard.android.widget.SlashDrawable
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
    private var tunnel: Tunnel? = null
    private var iconOn: Icon? = null
    private var iconOff: Icon? = null

    /* This works around an annoying unsolved frameworks bug some people are hitting. */
    override fun onBind(intent: Intent): IBinder? {
        var ret: IBinder? = null
        try {
            ret = super.onBind(intent)
        } catch (e: Exception) {
            Timber.d(e, "Failed to bind to TileService")
        }
        return ret
    }

    override fun onCreate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            iconOn = Icon.createWithResource(this, R.mipmap.ic_launcher)
            iconOff = iconOn
            return
        }
        val icon = SlashDrawable(
            resources.getDrawable(
                R.mipmap.ic_launcher,
                Application.get().theme
            )
        )
        /* Unfortunately we can't have animations, since Icons are marshaled. */
        icon.setAnimationEnabled(false)
        icon.setSlashed(false)
        var b = Bitmap.createBitmap(
            icon.intrinsicWidth, icon.intrinsicHeight, Bitmap.Config.ARGB_8888
        )
        var c = Canvas(b)
        icon.setBounds(0, 0, c.width, c.height)
        icon.draw(c)
        iconOn = Icon.createWithBitmap(b)
        icon.setSlashed(true)
        b = Bitmap.createBitmap(icon.intrinsicWidth, icon.intrinsicHeight, Bitmap.Config.ARGB_8888)
        c = Canvas(b)
        icon.setBounds(0, 0, c.width, c.height)
        icon.draw(c)
        iconOff = Icon.createWithBitmap(b)
    }

    override fun onClick() {
        if (tunnel != null) {
            val tile = qsTile
            if (tile != null) {
                tile.icon = if (tile.icon == iconOn) iconOff else iconOn
                tile.updateTile()
            }
            tunnel?.setState(State.TOGGLE)?.whenComplete { _, throwable ->
                this.onToggleFinished(throwable)
            }
        } else {
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivityAndCollapse(intent)
        }
    }

    override fun onStartListening() {
        Application.tunnelManager.addOnPropertyChangedCallback(onTunnelChangedCallback)
        tunnel?.addOnPropertyChangedCallback(onStateChangedCallback)
        updateTile()
    }

    override fun onStopListening() {
        tunnel?.removeOnPropertyChangedCallback(onStateChangedCallback)
        Application.tunnelManager.removeOnPropertyChangedCallback(onTunnelChangedCallback)
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
        val newTunnel = Application.tunnelManager.getLastUsedTunnel()
        if (newTunnel != tunnel) {
            tunnel?.removeOnPropertyChangedCallback(onStateChangedCallback)
            tunnel = newTunnel
            tunnel?.addOnPropertyChangedCallback(onStateChangedCallback)
        }
        // Update the tile contents.
        val label: String
        val state: Int
        val tile = qsTile
        if (tunnel != null) {
            label = tunnel?.name ?: ""
            state = if (tunnel?.state == Tunnel.State.UP)
                Tile.STATE_ACTIVE
            else
                Tile.STATE_INACTIVE
        } else {
            label = getString(R.string.app_name)
            state = Tile.STATE_INACTIVE
        }
        if (tile == null)
            return
        tile.label = label
        if (tile.state != state) {
            tile.icon = if (state == Tile.STATE_ACTIVE) iconOn else iconOff
            tile.state = state
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
