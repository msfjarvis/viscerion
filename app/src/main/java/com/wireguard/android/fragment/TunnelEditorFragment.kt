/*
 * Copyright © 2017-2018 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.wireguard.android.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.databinding.Observable
import androidx.databinding.ObservableList
import com.google.android.material.snackbar.Snackbar
import com.wireguard.android.Application
import com.wireguard.android.BR
import com.wireguard.android.R
import com.wireguard.android.databinding.TunnelEditorFragmentBinding
import com.wireguard.android.fragment.AppListDialogFragment.AppExclusionListener
import com.wireguard.android.model.Tunnel
import com.wireguard.android.util.ExceptionLoggers
import com.wireguard.config.Attribute
import com.wireguard.config.Config
import com.wireguard.config.Peer
import timber.log.Timber
import java.util.Objects

/**
 * Fragment for editing a WireGuard configuration.
 */

class TunnelEditorFragment : BaseFragment(), AppExclusionListener {
    private val breakObjectOrientedLayeringHandlerReceivers = ArrayList<Any>()
    private var binding: TunnelEditorFragmentBinding? = null
    private val breakObjectOrientedLayeringHandler: Observable.OnPropertyChangedCallback =
        object : Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable, propertyId: Int) {
                if (binding == null)
                    return
                val config = binding?.config ?: return
                if (propertyId == BR.config) {
                    config.addOnPropertyChangedCallback(this)
                    breakObjectOrientedLayeringHandlerReceivers.add(config)
                    config.interfaceSection.addOnPropertyChangedCallback(this)
                    breakObjectOrientedLayeringHandlerReceivers.add(config.interfaceSection)
                    config.peers.addOnListChangedCallback(breakObjectListOrientedLayeringHandler)
                    breakObjectOrientedLayeringHandlerReceivers.add(config.peers)
                } else if (propertyId == BR.dnses || propertyId == BR.peers)
                else
                    return
                val numSiblings = config.peers.size - 1
                for (peer in config.peers) {
                    peer.setInterfaceDNSRoutes(config.interfaceSection.getDnses())
                    peer.setNumSiblings(numSiblings)
                }
            }
        }
    private val breakObjectListOrientedLayeringHandler: ObservableList.OnListChangedCallback<ObservableList<Peer.Observable>> =
        object : ObservableList.OnListChangedCallback<ObservableList<Peer.Observable>>() {
            override fun onChanged(sender: ObservableList<Peer.Observable>) {}

            override fun onItemRangeChanged(
                sender: ObservableList<Peer.Observable>,
                positionStart: Int,
                itemCount: Int
            ) {
            }

            override fun onItemRangeMoved(
                sender: ObservableList<Peer.Observable>,
                fromPosition: Int,
                toPosition: Int,
                itemCount: Int
            ) {
            }

            override fun onItemRangeInserted(
                sender: ObservableList<Peer.Observable>,
                positionStart: Int,
                itemCount: Int
            ) {
                if (binding != null)
                    breakObjectOrientedLayeringHandler.onPropertyChanged(binding?.config, BR.peers)
            }

            override fun onItemRangeRemoved(
                sender: ObservableList<Peer.Observable>,
                positionStart: Int,
                itemCount: Int
            ) {
                if (binding != null)
                    breakObjectOrientedLayeringHandler.onPropertyChanged(binding?.config, BR.peers)
            }
        }
    private var tunnel: Tunnel? = null

    private fun onConfigLoaded(name: String, config: Config) {
        if (binding != null) {
            binding?.config = Config.Observable(config, name)
        }
    }

    private fun onConfigSaved(
        savedTunnel: Tunnel,
        throwable: Throwable?
    ) {
        val message: String
        if (throwable == null) {
            message = getString(R.string.config_save_success, savedTunnel.name)
            Timber.d(message)
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            onFinished()
        } else {
            val error = ExceptionLoggers.unwrapMessage(throwable)
            message = getString(R.string.config_save_error, savedTunnel.name, error)
            Timber.e(throwable)
            binding?.let { Snackbar.make(it.mainContainer, message, Snackbar.LENGTH_LONG).show() }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        Timber.tag(TAG)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.config_editor, menu)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = TunnelEditorFragmentBinding.inflate(inflater, container, false)
        binding?.let {
            it.addOnPropertyChangedCallback(breakObjectOrientedLayeringHandler)
            breakObjectOrientedLayeringHandlerReceivers.add(it)
            it.executePendingBindings()
        }
        return binding?.root
    }

    override fun onDestroyView() {
        binding = null
        for (o in breakObjectOrientedLayeringHandlerReceivers) {
            @Suppress("UNCHECKED_CAST")
            if (o is Observable)
                o.removeOnPropertyChangedCallback(breakObjectOrientedLayeringHandler)
            else (o as ObservableList<Peer.Observable>).removeOnListChangedCallback(
                breakObjectListOrientedLayeringHandler
            )
        }
        super.onDestroyView()
    }

    private fun onFinished() {
        // Hide the keyboard; it rarely goes away on its own.
        val activity = activity ?: return
        val focusedView = activity.currentFocus
        focusedView?.let {
            val inputManager = context?.getSystemService<InputMethodManager>()
            inputManager?.hideSoftInputFromWindow(
                it.windowToken,
                InputMethodManager.HIDE_NOT_ALWAYS
            )
        }
        // Tell the activity to finish itself or go back to the detail view.
        activity.runOnUiThread {
            // TODO(smaeul): Remove this hack when fixing the Config ViewModel
            // The selected tunnel has to actually change, but we have to remember this one.
            val savedTunnel = tunnel
            if (savedTunnel == selectedTunnel)
                selectedTunnel = null
            selectedTunnel = savedTunnel
        }
    }

    override fun onResume() {
        super.onResume()

        context?.let {
            activity?.window?.navigationBarColor = ContextCompat.getColor(it, R.color.accent_darker)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_action_save -> {
                val newConfig = Config()
                try {
                    binding?.config?.commitData(newConfig)
                } catch (e: Exception) {
                    val error = ExceptionLoggers.unwrapMessage(e)
                    val tunnelName = if (tunnel == null) binding?.config?.name else tunnel?.name
                    val message = getString(R.string.config_save_error, tunnelName, error)
                    Timber.e(message)
                    binding?.let { Snackbar.make(it.mainContainer, error, Snackbar.LENGTH_LONG).show() }
                    return false
                }

                when {
                    tunnel == null -> {
                        Timber.d("Attempting to create new tunnel %s", binding?.config?.name)
                        val manager = Application.tunnelManager
                        manager.create(binding?.config?.name ?: "", newConfig)
                            .whenComplete { newTunnel, throwable ->
                                this.onTunnelCreated(
                                    newTunnel,
                                    throwable
                                )
                            }
                    }
                    tunnel?.name != binding?.config?.name -> {
                        tunnel?.let {
                            Timber.d("Attempting to rename tunnel to %s", binding?.config?.name)
                            it.setName(binding?.config?.name!!).whenComplete { _, b -> onTunnelRenamed(it, newConfig, b) }
                        }
                    }
                    else -> {
                        tunnel?.let {
                            Timber.d("Attempting to save config of %s", it.name)
                            it.setConfig(newConfig).whenComplete { _, b -> onConfigSaved(it, b) }
                        }
                    }
                }
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(KEY_LOCAL_CONFIG, binding?.config)
        outState.putString(KEY_ORIGINAL_NAME, tunnel?.name)
        super.onSaveInstanceState(outState)
    }

    override fun onSelectedTunnelChanged(oldTunnel: Tunnel?, newTunnel: Tunnel?) {
        tunnel = newTunnel
        if (binding == null)
            return
        binding?.config = Config.Observable(null, null)
        tunnel?.let {
            it.configAsync.thenAccept { a -> onConfigLoaded(it.name, a) }
        }
    }

    private fun onTunnelCreated(newTunnel: Tunnel, throwable: Throwable?) {
        val message: String
        if (throwable == null) {
            tunnel = newTunnel
            message = getString(R.string.tunnel_create_success, tunnel?.name)
            Timber.d(message)
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            onFinished()
        } else {
            val error = ExceptionLoggers.unwrapMessage(throwable)
            message = getString(R.string.tunnel_create_error, error)
            Timber.e(throwable)
            binding?.let { Snackbar.make(it.mainContainer, message, Snackbar.LENGTH_LONG).show() }
        }
    }

    private fun onTunnelRenamed(
        renamedTunnel: Tunnel?,
        newConfig: Config,
        throwable: Throwable?
    ) {
        val message: String
        if (throwable == null) {
            message = getString(R.string.tunnel_rename_success, renamedTunnel?.name)
            Timber.d(message)
            // Now save the rest of configuration changes.
            Timber.d("Attempting to save config of renamed tunnel %s", tunnel?.name)
            renamedTunnel?.setConfig(newConfig)?.whenComplete { _, b -> onConfigSaved(renamedTunnel, b) }
        } else {
            val error = ExceptionLoggers.unwrapMessage(throwable)
            message = getString(R.string.tunnel_rename_error, error)
            Timber.e(throwable)
            binding?.let { Snackbar.make(it.mainContainer, message, Snackbar.LENGTH_LONG).show() }
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        if (binding == null) {
            return
        }

        binding?.fragment = this

        if (savedInstanceState == null) {
            onSelectedTunnelChanged(null, selectedTunnel)
        } else {
            tunnel = selectedTunnel
            val config = savedInstanceState.getParcelable<Config.Observable>(KEY_LOCAL_CONFIG)
            val originalName = savedInstanceState.getString(KEY_ORIGINAL_NAME)
            if (tunnel != null && tunnel?.name != originalName)
                onSelectedTunnelChanged(null, tunnel)
            else
                binding?.config = config
        }

        super.onViewStateRestored(savedInstanceState)
    }

    @Suppress("Unused", "UNUSED_PARAMETER")
    fun onRequestSetExcludedApplications(view: View) {
        val fragmentManager = fragmentManager
        if (fragmentManager != null && binding != null) {
            val excludedApps = Attribute.stringToList(binding?.config?.interfaceSection?.getExcludedApplications())
            val fragment = AppListDialogFragment.newInstance(excludedApps, target = this)
            fragment.show(fragmentManager, null)
        }
    }

    override fun onExcludedAppsSelected(excludedApps: List<String>) {
        Objects.requireNonNull<TunnelEditorFragmentBinding>(
            binding,
            "Tried to set excluded apps while no view was loaded"
        )
        binding?.config?.interfaceSection?.setExcludedApplications(Attribute.iterableToString(excludedApps))
    }

    companion object {
        private const val KEY_LOCAL_CONFIG = "local_config"
        private const val KEY_ORIGINAL_NAME = "original_name"
        private val TAG = "WireGuard/" + TunnelEditorFragment::class.java.simpleName
    }
}
