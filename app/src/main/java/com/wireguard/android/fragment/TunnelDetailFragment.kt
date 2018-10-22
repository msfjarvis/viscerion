/*
 * Copyright © 2017-2018 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.wireguard.android.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import com.wireguard.android.R
import com.wireguard.android.databinding.TunnelDetailFragmentBinding
import com.wireguard.android.model.Tunnel
import com.wireguard.android.util.resolveAttribute
import com.wireguard.config.Config

/**
 * Fragment that shows details about a specific tunnel.
 */

class TunnelDetailFragment : BaseFragment() {
    private var binding: TunnelDetailFragmentBinding? = null

    private fun onConfigLoaded(name: String, config: Config) {
        binding?.config = Config.Observable(config, name)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.tunnel_detail, menu)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = TunnelDetailFragmentBinding.inflate(inflater, container, false)
        binding?.executePendingBindings()
        return binding?.root
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    override fun onSelectedTunnelChanged(oldTunnel: Tunnel?, newTunnel: Tunnel?) {
        if (binding == null)
            return
        binding?.tunnel = newTunnel
        if (newTunnel == null)
            binding?.config = null
        else
            newTunnel.configAsync.thenAccept { a -> onConfigLoaded(newTunnel.getName(), a) }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        if (binding == null) {
            return
        }

        binding?.fragment = this
        onSelectedTunnelChanged(null, selectedTunnel)
        super.onViewStateRestored(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()

        activity?.window?.navigationBarColor = context!!.resolveAttribute(android.R.attr.navigationBarColor)
    }
}
