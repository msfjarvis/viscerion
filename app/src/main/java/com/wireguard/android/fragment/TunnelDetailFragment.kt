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

/**
 * Fragment that shows details about a specific tunnel.
 */

class TunnelDetailFragment : BaseFragment() {
    private var binding: TunnelDetailFragmentBinding? = null

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
        binding ?: return
        binding?.tunnel = newTunnel
        if (newTunnel == null)
            binding?.config = null
        else
            newTunnel.configAsync.thenAccept { a -> binding?.config = a }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        binding ?: return

        binding?.fragment = this
        onSelectedTunnelChanged(null, selectedTunnel)
        super.onViewStateRestored(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        context?.let {
            activity?.window?.navigationBarColor = it.resolveAttribute(android.R.attr.navigationBarColor)
        }
    }
}
