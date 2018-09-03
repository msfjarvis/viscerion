/*
 * Copyright © 2018 Samuel Holland <samuel@sholland.org>
 * Copyright © 2018 Jason A. Donenfeld <Jason@zx2c4.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.wireguard.android.fragment

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Lunchbar
import com.wireguard.android.Application
import com.wireguard.android.R
import com.wireguard.android.activity.BaseActivity
import com.wireguard.android.activity.BaseActivity.OnSelectedTunnelChangedListener
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.databinding.TunnelDetailFragmentBinding
import com.wireguard.android.databinding.TunnelListItemBinding
import com.wireguard.android.model.Tunnel
import com.wireguard.android.model.Tunnel.State
import com.wireguard.android.util.ExceptionLoggers
import timber.log.Timber

/**
 * Base class for fragments that need to know the currently-selected tunnel. Only does anything when
 * attached to a `BaseActivity`.
 */

abstract class BaseFragment : Fragment(), OnSelectedTunnelChangedListener {

    private var activity: BaseActivity? = null
    private var pendingTunnel: Tunnel? = null
    private var pendingTunnelUp: Boolean? = null

    protected var selectedTunnel: Tunnel?
        get() = if (activity != null) activity!!.selectedTunnel else null
        set(tunnel) {
            activity?.selectedTunnel = tunnel
        }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is BaseActivity) {
            activity = context
            activity?.addOnSelectedTunnelChangedListener(this)
        } else {
            activity = null
        }
        Timber.tag(TAG)
    }

    override fun onDetach() {
        activity?.removeOnSelectedTunnelChangedListener(this)
        activity = null
        super.onDetach()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_VPN_PERMISSION) {
            if (pendingTunnel != null && pendingTunnelUp != null)
                setTunnelStateWithPermissionsResult(pendingTunnel!!, pendingTunnelUp!!)
            pendingTunnel = null
            pendingTunnelUp = null
        }
    }

    fun setTunnelState(view: View, checked: Boolean) {
        val binding = DataBindingUtil.findBinding<ViewDataBinding>(view)
        val tunnel: Tunnel?
        tunnel = when (binding) {
            is TunnelDetailFragmentBinding -> binding.tunnel
            is TunnelListItemBinding -> binding.item
            else -> return
        }
        if (tunnel == null)
            return

        Application.backendAsync.thenAccept { backend ->
            if (backend is GoBackend) {
                val intent = GoBackend.VpnService.prepare(view.context)
                if (intent != null) {
                    pendingTunnel = tunnel
                    pendingTunnelUp = checked
                    startActivityForResult(intent, REQUEST_CODE_VPN_PERMISSION)
                    return@thenAccept
                }
            }

            setTunnelStateWithPermissionsResult(tunnel, checked)
        }
    }

    private fun setTunnelStateWithPermissionsResult(tunnel: Tunnel, checked: Boolean) {
        tunnel.setState(State.of(checked)).whenComplete { _, throwable ->
            if (throwable == null) return@whenComplete
            val error = ExceptionLoggers.unwrapMessage(throwable)
            val messageResId = if (checked) R.string.error_up else R.string.error_down
            val message = context!!.getString(messageResId, error)
            val view = view
            if (view != null)
                Lunchbar.make(view, message, Lunchbar.LENGTH_LONG).show()
            else
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            Timber.e(throwable, message)
        }
    }

    companion object {
        private val TAG = "WireGuard/" + BaseFragment::class.java.simpleName
        private const val REQUEST_CODE_VPN_PERMISSION = 23491
    }
}
