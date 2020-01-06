/*
 * Copyright © 2017-2020 WireGuard LLC.
 * Copyright © 2018-2020 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.fragment

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.wireguard.android.R
import com.wireguard.android.databinding.TunnelDetailFragmentBinding
import com.wireguard.android.databinding.TunnelDetailPeerBinding
import com.wireguard.android.di.injector
import com.wireguard.android.model.Tunnel
import com.wireguard.android.model.Tunnel.State
import com.wireguard.android.ui.EdgeToEdge
import com.wireguard.android.util.ApplicationPreferences
import com.wireguard.android.util.isSystemDarkThemeEnabled
import com.wireguard.android.util.resolveAttribute
import java.util.Timer
import java.util.TimerTask
import javax.inject.Inject
import me.msfjarvis.viscerion.crypto.Key

/**
 * Fragment that shows details about a specific tunnel.
 */

class TunnelDetailFragment : BaseFragment() {
    private var binding: TunnelDetailFragmentBinding? = null
    private var timer: Timer? = null
    private var lastState: State? = State.TOGGLE
    @Inject lateinit var prefs: ApplicationPreferences

    class StatsTimerTask(private val tdf: TunnelDetailFragment) : TimerTask() {
        override fun run() {
            tdf.updateStats()
        }
    }

    override fun onAttach(context: Context) {
        injector.inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.tunnel_detail, menu)
        menu.findItem(R.id.menu_search).isVisible = false
    }

    override fun onStop() {
        super.onStop()
        if (timer != null) {
            timer!!.cancel()
            timer = null
        }
    }

    override fun onResume() {
        super.onResume()
        timer = Timer()
        timer!!.scheduleAtFixedRate(StatsTimerTask(this), 0, 1000)
        requireActivity().window?.apply {
            val ctx = requireContext()
            navigationBarColor = ctx.resolveAttribute(android.R.attr.navigationBarColor)
            if (Build.VERSION.SDK_INT >= 27 &&
                (!prefs.useDarkTheme && !ctx.isSystemDarkThemeEnabled())) {
                // Restore window flags
                decorView.systemUiVisibility =
                    View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = TunnelDetailFragmentBinding.inflate(inflater, container, false)
        binding?.executePendingBindings()
        binding?.let {
            EdgeToEdge.setUpRoot(it.root as ViewGroup)
            EdgeToEdge.setUpScrollingContent(it.tunnelDetailCard, null)
        }
        return binding?.root
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    override fun onSelectedTunnelChanged(oldTunnel: Tunnel?, newTunnel: Tunnel?) {
        if (binding == null) {
            return
        }
        binding?.tunnel = newTunnel
        if (newTunnel == null) {
            binding?.config = null
        } else {
            newTunnel.configAsync.thenAccept { a -> binding?.config = a }
        }
        lastState = State.TOGGLE
        updateStats()
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        if (binding == null) return
        binding?.fragment = this
        onSelectedTunnelChanged(null, selectedTunnel)
        super.onViewStateRestored(savedInstanceState)
    }

    private fun formatBytes(bytes: Long): String? {
        when {
            bytes < 1024L -> return context!!.getString(
                R.string.transfer_bytes,
                bytes
            )
            bytes < 1024L * 1024L -> return context!!.getString(
                R.string.transfer_kibibytes,
                bytes / 1024.0
            )
            bytes < 1024L * 1024L * 1024L -> return context!!.getString(
                R.string.transfer_mibibytes,
                bytes / (1024.0 * 1024.0)
            )
            bytes < 1024L * 1024L * 1024L * 1024L -> return context!!.getString(
                R.string.transfer_gibibytes,
                bytes / (1024.0 * 1024.0 * 1024.0)
            )
            else -> return context!!.getString(
                R.string.transfer_tibibytes,
                bytes / (1024.0 * 1024.0 * 1024.0) / 1024.0
            )
        }
    }

    private fun updateStats() {
        if (binding == null || !isResumed) {
            return
        }

        val state = binding!!.tunnel!!.state
        if (state != State.UP && lastState == state) {
            return
        }

        lastState = state
        binding!!.tunnel!!.statisticsAsync.whenComplete { statistics, throwable ->
            if (throwable != null) {
                for (i in 0 until binding!!.peersLayout.childCount) {
                    val peer: TunnelDetailPeerBinding =
                        DataBindingUtil.getBinding(binding!!.peersLayout.getChildAt(i))
                            ?: continue
                    peer.transferLabel.visibility = View.GONE
                    peer.transferText.visibility = View.GONE
                }
                return@whenComplete
            }
            for (i in 0 until binding!!.peersLayout.childCount) {
                val peer: TunnelDetailPeerBinding =
                    DataBindingUtil.getBinding(binding!!.peersLayout.getChildAt(i))
                        ?: continue
                val publicKey: Key = peer.item!!.publicKey
                val rx = statistics.peerRx(publicKey)
                val tx = statistics.peerTx(publicKey)
                if (rx == 0L && tx == 0L) {
                    peer.transferLabel.visibility = View.GONE
                    peer.transferText.visibility = View.GONE
                    continue
                }
                peer.transferText.text = context!!.getString(
                    R.string.transfer_rx_tx,
                    formatBytes(rx),
                    formatBytes(tx)
                )
                peer.transferLabel.visibility = View.VISIBLE
                peer.transferText.visibility = View.VISIBLE
            }
        }
    }
}
