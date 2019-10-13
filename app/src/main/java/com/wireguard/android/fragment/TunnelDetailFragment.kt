/*
 * Copyright © 2017-2019 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.fragment

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import com.wireguard.android.R
import com.wireguard.android.databinding.TunnelDetailFragmentBinding
import com.wireguard.android.di.ext.getPrefs
import com.wireguard.android.model.Tunnel
import com.wireguard.android.ui.EdgeToEdge
import com.wireguard.android.util.isSystemDarkThemeEnabled
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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.tunnel_detail, menu)
        menu.findItem(R.id.menu_search).isVisible = false
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
        if (binding == null) return
        binding?.tunnel = newTunnel
        if (newTunnel == null)
            binding?.config = null
        else
            newTunnel.configAsync.thenAccept { a -> binding?.config = a }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        if (binding == null) return
        binding?.fragment = this
        onSelectedTunnelChanged(null, selectedTunnel)
        super.onViewStateRestored(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        requireActivity().window?.apply {
            val ctx = requireContext()
            navigationBarColor = ctx.resolveAttribute(android.R.attr.navigationBarColor)
            if (Build.VERSION.SDK_INT >= 27 &&
                    (!getPrefs().useDarkTheme && !ctx.isSystemDarkThemeEnabled())) {
                // Restore window flags
                decorView.systemUiVisibility =
                        View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        }
    }
}
