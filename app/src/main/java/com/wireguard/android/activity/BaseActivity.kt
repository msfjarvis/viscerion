/*
 * Copyright © 2017-2020 WireGuard LLC.
 * Copyright © 2018-2020 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.CallbackRegistry
import androidx.databinding.CallbackRegistry.NotifierCallback
import com.wireguard.android.di.injector
import com.wireguard.android.model.Tunnel
import com.wireguard.android.model.TunnelManager
import javax.inject.Inject

/**
 * Base class for activities that need to remember the currently-selected tunnel.
 */

abstract class BaseActivity : AppCompatActivity() {

    @Inject lateinit var tunnelManager: TunnelManager

    private val selectionChangeRegistry = SelectionChangeRegistry()
    var selectedTunnel: Tunnel? = null
        set(tunnel) {
            val oldTunnel = this.selectedTunnel
            if (oldTunnel == tunnel) {
                return
            }
            field = tunnel
            onSelectedTunnelChanged(oldTunnel, tunnel)
            selectionChangeRegistry.notifyCallbacks(oldTunnel, 0, tunnel)
        }

    fun addOnSelectedTunnelChangedListener(listener: OnSelectedTunnelChangedListener) {
        selectionChangeRegistry.add(listener)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        injector.inject(this)
        // Restore the saved tunnel if there is one; otherwise grab it from the arguments.
        val savedTunnelName: String? = when {
            savedInstanceState != null -> savedInstanceState.getString(KEY_SELECTED_TUNNEL)
            intent != null -> intent.getStringExtra(KEY_SELECTED_TUNNEL)
            else -> null
        }

        savedTunnelName?.let {
            tunnelManager.getTunnels().thenAccept { tunnels ->
                selectedTunnel = tunnels[it]
            }
        }

        // The selected tunnel must be set before the superclass method recreates fragments.
        super.onCreate(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        selectedTunnel?.let {
            outState.putString(KEY_SELECTED_TUNNEL, it.name)
        }
        super.onSaveInstanceState(outState)
    }

    protected abstract fun onSelectedTunnelChanged(oldTunnel: Tunnel?, newTunnel: Tunnel?)

    fun removeOnSelectedTunnelChangedListener(
        listener: OnSelectedTunnelChangedListener
    ) {
        selectionChangeRegistry.remove(listener)
    }

    interface OnSelectedTunnelChangedListener {
        fun onSelectedTunnelChanged(oldTunnel: Tunnel?, newTunnel: Tunnel?)
    }

    private class SelectionChangeNotifier : NotifierCallback<OnSelectedTunnelChangedListener, Tunnel, Tunnel>() {
        override fun onNotifyCallback(
            listener: OnSelectedTunnelChangedListener,
            oldTunnel: Tunnel?,
            ignored: Int,
            newTunnel: Tunnel?
        ) {
            listener.onSelectedTunnelChanged(oldTunnel, newTunnel)
        }
    }

    private class SelectionChangeRegistry :
        CallbackRegistry<OnSelectedTunnelChangedListener, Tunnel, Tunnel>(SelectionChangeNotifier())

    companion object {
        private const val KEY_SELECTED_TUNNEL = "selected_tunnel"
    }
}
