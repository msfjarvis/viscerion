/*
 * Copyright © 2017-2019 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.activity

import android.content.ComponentName
import android.os.Bundle
import android.service.quicksettings.TileService
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.wireguard.android.R
import com.wireguard.android.di.injector
import com.wireguard.android.model.Tunnel
import com.wireguard.android.model.TunnelManager
import com.wireguard.android.services.QuickTileService
import com.wireguard.android.util.ErrorMessages
import javax.inject.Inject
import timber.log.Timber

@RequiresApi(24)
class TunnelToggleActivity : AppCompatActivity() {

    @Inject lateinit var tunnelManager: TunnelManager

    override fun onCreate(savedInstanceState: Bundle?) {
        injector.inject(this)
        super.onCreate(savedInstanceState)
        val tunnel = tunnelManager.getLastUsedTunnel() ?: return
        tunnel.setState(Tunnel.State.TOGGLE).whenComplete { _, throwable ->
            TileService.requestListeningState(this, ComponentName(this, QuickTileService::class.java))
            onToggleFinished(throwable)
            finishAffinity()
        }
    }

    private fun onToggleFinished(throwable: Throwable?) {
        throwable ?: return
        val error = ErrorMessages[throwable]
        val message = getString(R.string.toggle_error, error)
        Timber.e(throwable)
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
