/*
 * Copyright © 2019 Harsh Shandilya. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.activity

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.SparseArray
import android.view.MenuItem
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.transaction
import androidx.preference.PreferenceFragmentCompat
import com.wireguard.android.Application
import com.wireguard.android.BuildConfig
import com.wireguard.android.R
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.WgQuickBackend
import com.wireguard.android.fragment.AppListDialogFragment
import com.wireguard.android.util.ApplicationPreferences
import com.wireguard.android.util.asString
import com.wireguard.android.util.restartApplication
import java.util.ArrayList
import java.util.Arrays

/**
 * Interface for changing application-global persistent settings.
 */

class SettingsActivity : ThemeChangeAwareActivity() {
    private val permissionRequestCallbacks = SparseArray<(permissions: Array<String>, granted: IntArray) -> Unit>()
    private var permissionRequestCounter: Int = 0

    fun ensurePermissions(
        permissions: Array<String>,
        function: (permissions: Array<String>, granted: IntArray) -> Unit
    ) {
        val needPermissions = ArrayList<String>(permissions.size)
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED)
                needPermissions.add(permission)
        }
        if (needPermissions.isEmpty()) {
            val granted = IntArray(permissions.size)
            Arrays.fill(granted, PackageManager.PERMISSION_GRANTED)
            function(permissions, granted)
            return
        }
        val idx = permissionRequestCounter++
        permissionRequestCallbacks.put(idx, function)
        ActivityCompat.requestPermissions(
            this,
            needPermissions.toTypedArray(), idx
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (supportFragmentManager.findFragmentById(android.R.id.content) == null) {
            supportFragmentManager.transaction {
                add(android.R.id.content, SettingsFragment())
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        val f = permissionRequestCallbacks.get(requestCode)
        if (f != null) {
            permissionRequestCallbacks.remove(requestCode)
            f(permissions, grantResults)
        }
    }

    class SettingsFragment : PreferenceFragmentCompat(), AppListDialogFragment.AppExclusionListener {
        override fun onCreatePreferences(savedInstanceState: Bundle?, key: String?) {
            addPreferencesFromResource(R.xml.preferences)
            val screen = preferenceScreen
            val wgQuickOnlyPrefs = arrayOf(
                preferenceManager.findPreference("tools_installer"),
                preferenceManager.findPreference("restore_on_boot")
            )
            val debugOnlyPrefs = arrayOf(
                preferenceManager.findPreference(ApplicationPreferences.forceUserspaceBackendkey)
            )
            val wgOnlyPrefs = arrayOf(
                preferenceManager.findPreference(ApplicationPreferences.whitelistAppsKey)
            )
            if (!BuildConfig.DEBUG || !Application.supportsKernelModule)
                debugOnlyPrefs.forEach {
                    screen.removePreference(it)
                }
            for (pref in wgQuickOnlyPrefs)
                pref.isVisible = false
            for (pref in wgOnlyPrefs)
                pref.isVisible = false
            Application.backendAsync.thenAccept { backend ->
                for (pref in wgQuickOnlyPrefs) {
                    if (backend is WgQuickBackend)
                        pref.isVisible = true
                    else
                        screen.removePreference(pref)
                }
                for (pref in wgOnlyPrefs) {
                    if (backend is GoBackend)
                        pref.isVisible = true
                    else
                        screen.removePreference(pref)
                }
            }
            preferenceManager.findPreference(ApplicationPreferences.globalExclusionsKey).setOnPreferenceClickListener {
                val excludedApps = ArrayList<String>(ApplicationPreferences.exclusionsArray)
                val fragment = AppListDialogFragment.newInstance(excludedApps, true, this)
                fragment.show(fragmentManager, null)
                true
            }
            preferenceManager.findPreference(ApplicationPreferences.whitelistAppsKey)?.setOnPreferenceClickListener {
                Application.tunnelManager.restartActiveTunnels()
                true
            }
            preferenceManager.findPreference(ApplicationPreferences.forceUserspaceBackendkey)
                ?.setOnPreferenceClickListener {
                    context?.restartApplication()
                    true
                }
        }

        override fun onExcludedAppsSelected(excludedApps: List<String>) {
            if (excludedApps.asString() == ApplicationPreferences.exclusions) return
            if (excludedApps.isNotEmpty()) {
                Application.tunnelManager.getTunnels().thenAccept { tunnels ->
                        tunnels.forEach { tunnel ->
                            val oldConfig = tunnel.getConfig()
                            oldConfig?.let {
                                ApplicationPreferences.exclusionsArray.forEach { exclusion -> it.`interface`.excludedApplications.remove(exclusion) }
                                it.`interface`.excludedApplications.addAll(excludedApps.toCollection(ArrayList()))
                                tunnel.setConfig(it)
                            }
                        }
                    }
                ApplicationPreferences.exclusions = excludedApps.asString()
            } else {
                Application.tunnelManager.getTunnels().thenAccept { tunnels ->
                        tunnels.forEach { tunnel ->
                            ApplicationPreferences.exclusionsArray.forEach { exclusion -> tunnel.getConfig()?.`interface`?.excludedApplications?.remove(exclusion) }
                        }
                    }
                ApplicationPreferences.exclusions = ""
            }
            Application.tunnelManager.restartActiveTunnels()
        }
    }
}
