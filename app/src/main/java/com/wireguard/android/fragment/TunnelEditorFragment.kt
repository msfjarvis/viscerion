/*
 * Copyright © 2017-2018 WireGuard LLC.
 * Copyright © 2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.fragment

import android.os.Build
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
import com.google.android.material.snackbar.Snackbar
import com.wireguard.android.Application
import com.wireguard.android.R
import com.wireguard.android.activity.MainActivity
import com.wireguard.android.databinding.TunnelEditorFragmentBinding
import com.wireguard.android.fragment.AppListDialogFragment.AppExclusionListener
import com.wireguard.android.model.Tunnel
import com.wireguard.android.util.ApplicationPreferences
import com.wireguard.android.util.ErrorMessages
import com.wireguard.android.util.requireNonNull
import com.wireguard.android.viewmodel.ConfigProxy
import com.wireguard.android.widget.KeyInputFilter
import com.wireguard.android.widget.NameInputFilter
import com.wireguard.config.Config
import timber.log.Timber

/**
 * Fragment for editing a WireGuard configuration.
 */

class TunnelEditorFragment : BaseFragment(), AppExclusionListener {
    private var binding: TunnelEditorFragmentBinding? = null
    private var tunnel: Tunnel? = null

    private fun onConfigLoaded(config: Config) {
        if (binding != null) {
            binding?.config = ConfigProxy(config)
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
            val error = ErrorMessages[throwable]
            message = getString(R.string.config_save_error, savedTunnel.name, error)
            Timber.e(throwable)
            binding?.let { Snackbar.make(it.mainContainer, message, Snackbar.LENGTH_LONG).show() }
        }
    }

    override fun onPause() {
        super.onPause()
        onFinished(false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
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
        binding?.executePendingBindings()
        binding?.interfaceNameText?.filters = arrayOf(NameInputFilter())
        binding?.privateKeyText?.filters = arrayOf(KeyInputFilter())
        return binding?.root
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private fun onFinished(informActivity: Boolean = true) {
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
        if (informActivity) {
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
    }

    override fun onResume() {
        super.onResume()
        if (!MainActivity.isTwoPaneLayout)
            context?.let {
                activity?.window?.apply {
                    navigationBarColor = ContextCompat.getColor(it, R.color.accent_darker)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 && !ApplicationPreferences.useDarkTheme) {
                        // Clear window flags to let navigation bar be dark
                        decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            navigationBarDividerColor = ContextCompat.getColor(it, android.R.color.transparent)
                        }
                    }
                }
            }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_action_save -> {
                val newConfig: Config?
                try {
                    newConfig = binding?.config?.resolve()
                } catch (e: Exception) {
                    val error = ErrorMessages[e]
                    val tunnelName = if (tunnel == null) binding?.name else tunnel?.name
                    val message = getString(R.string.config_save_error, tunnelName, error)
                    Timber.e(message)
                    binding?.let { Snackbar.make(it.mainContainer, error, Snackbar.LENGTH_LONG).show() }
                    return false
                }

                when {
                    tunnel == null -> {
                        Timber.d("Attempting to create new tunnel %s", binding?.name)
                        val manager = Application.tunnelManager
                        manager.create(binding?.name ?: "", newConfig)
                            .whenComplete { newTunnel, throwable ->
                                this.onTunnelCreated(
                                    newTunnel,
                                    throwable
                                )
                            }
                    }
                    tunnel?.name != binding?.name -> {
                        tunnel?.let {
                            Timber.d("Attempting to rename tunnel to %s", binding?.name)
                            it.setName(binding?.name ?: "")
                                .whenComplete { _, b -> onTunnelRenamed(it, newConfig!!, b) }
                        }
                    }
                    else -> {
                        tunnel?.let {
                            Timber.d("Attempting to save config of %s", it.name)
                            it.setConfig(newConfig!!).whenComplete { _, b -> onConfigSaved(it, b) }
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
        binding?.config = ConfigProxy()
        if (tunnel != null) {
            binding?.name = tunnel?.name
            tunnel?.configAsync?.thenAccept(this::onConfigLoaded)
        } else {
            binding?.name = ""
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
            val error = ErrorMessages[throwable]
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
            val error = ErrorMessages[throwable]
            message = getString(R.string.tunnel_rename_error, error)
            Timber.e(throwable)
            binding?.let { Snackbar.make(it.mainContainer, message, Snackbar.LENGTH_LONG).show() }
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        if (binding == null)
            return

        binding?.fragment = this

        if (savedInstanceState == null) {
            onSelectedTunnelChanged(null, selectedTunnel)
        } else {
            tunnel = selectedTunnel
            val config = savedInstanceState.getParcelable<ConfigProxy>(KEY_LOCAL_CONFIG)
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
            val excludedApps = ArrayList<String>(binding?.config?.`interface`?.getExcludedApplications())
            val fragment = AppListDialogFragment.newInstance(excludedApps, target = this)
            fragment.show(fragmentManager, null)
        }
    }

    override fun onExcludedAppsSelected(excludedApps: List<String>) {
        binding.requireNonNull<TunnelEditorFragmentBinding>("Tried to set excluded apps while no view was loaded")
        val excludedApplications = binding?.config?.`interface`?.getExcludedApplications()
        excludedApplications?.clear()
        excludedApplications?.addAll(excludedApps)
    }

    companion object {
        private const val KEY_LOCAL_CONFIG = "local_config"
        private const val KEY_ORIGINAL_NAME = "original_name"
    }
}
