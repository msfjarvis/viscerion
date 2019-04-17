/*
 * Copyright © 2017-2018 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.activity

import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.SparseArray
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat.makeCustomAnimation
import androidx.fragment.app.commit
import androidx.preference.CheckBoxPreference
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.snackbar.Snackbar
import com.wireguard.android.BuildConfig
import com.wireguard.android.R
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.WgQuickBackend
import com.wireguard.android.fragment.AppListDialogFragment
import com.wireguard.android.model.TunnelManager
import com.wireguard.android.util.ApplicationPreferences
import com.wireguard.android.util.BackendAsync
import com.wireguard.android.util.asString
import com.wireguard.android.util.isPermissionGranted
import com.wireguard.android.util.updateAppTheme
import org.koin.android.ext.android.inject
import java.io.File
import java.util.Arrays

/**
 * Interface for changing application-global persistent settings.
 */

class SettingsActivity : AppCompatActivity() {
    private val permissionRequestCallbacks by lazy { SparseArray<(permissions: Array<String>, granted: IntArray) -> Unit>() }
    private var permissionRequestCounter: Int = 0

    fun ensurePermissions(
        permissions: Array<String>,
        function: (permissions: Array<String>, granted: IntArray) -> Unit
    ) {
        val needPermissions = ArrayList<String>(permissions.size)
        for (permission in permissions) {
            if (!this.isPermissionGranted(permission))
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
            supportFragmentManager.commit {
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
        private val prefs by inject<ApplicationPreferences>()

        override fun onCreatePreferences(savedInstanceState: Bundle?, key: String?) {
            addPreferencesFromResource(R.xml.preferences)
            val screen = preferenceScreen
            val wgQuickOnlyPrefs = arrayOf(
                    screen.findPreference<Preference>("tools_installer"),
                    screen.findPreference<CheckBoxPreference>("restore_on_boot")
            )
            val debugOnlyPrefs = arrayOf(
                    screen.findPreference<SwitchPreferenceCompat>("force_userspace_backend")
            )
            val wgOnlyPrefs = arrayOf(
                    screen.findPreference<CheckBoxPreference>("whitelist_exclusions")
            )
            val exclusionsPref = preferenceManager.findPreference<Preference>("global_exclusions")
            val taskerPref = preferenceManager.findPreference<SwitchPreferenceCompat>("allow_tasker_integration")
            val integrationSecretPref =
                    preferenceManager.findPreference<EditTextPreference>("intent_integration_secret")
            val altIconPref = preferenceManager.findPreference<CheckBoxPreference>("use_alt_icon")
            val darkThemePref = preferenceManager.findPreference<CheckBoxPreference>("dark_theme")
            for (pref in wgQuickOnlyPrefs + wgOnlyPrefs + debugOnlyPrefs)
                pref?.isVisible = false

            if (BuildConfig.DEBUG && File("/sys/module/wireguard").exists())
                for (pref in debugOnlyPrefs)
                    pref?.isVisible = true

            inject<BackendAsync>().value.thenAccept { backend ->
                for (pref in wgQuickOnlyPrefs) {
                    pref?.let {
                        if (backend is WgQuickBackend)
                            it.isVisible = true
                        else
                            screen.removePreference(it)
                    }
                }
                for (pref in wgOnlyPrefs) {
                    pref?.let {
                        if (backend is GoBackend)
                            it.isVisible = true
                        else
                            screen.removePreference(it)
                    }
                }
            }

            integrationSecretPref?.isVisible = prefs.allowTaskerIntegration

            exclusionsPref?.setOnPreferenceClickListener {
                val fragment = AppListDialogFragment.newInstance(prefs.exclusionsArray, true, this)
                fragment.show(requireFragmentManager(), null)
                true
            }

            taskerPref?.setOnPreferenceChangeListener { _, newValue ->
                integrationSecretPref?.isVisible = (newValue as Boolean)
                true
            }

            integrationSecretPref?.setSummaryProvider { preference ->
                if (prefs.allowTaskerIntegration &&
                        preference.isEnabled &&
                        prefs.taskerIntegrationSecret.isEmpty()
                )
                    getString(R.string.tasker_integration_summary_empty_secret)
                else
                    getString(R.string.tasker_integration_secret_summary)
            }

            altIconPref?.setOnPreferenceClickListener {
                val pref = it as CheckBoxPreference
                val ctx = requireContext()
                ctx.packageManager.apply {
                    setComponentEnabledSetting(
                            ComponentName(ctx.packageName, "${ctx.packageName}.LauncherActivity"),
                            if (pref.isChecked)
                                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                            else
                                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                            PackageManager.DONT_KILL_APP
                    )
                    setComponentEnabledSetting(
                            ComponentName(ctx.packageName, "${ctx.packageName}.AltIconLauncherActivity"),
                            if (pref.isChecked)
                                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                            else
                                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            PackageManager.DONT_KILL_APP
                    )
                }
                Snackbar.make(
                        requireView(),
                        getString(R.string.pref_alt_icon_apply_message),
                        Snackbar.LENGTH_SHORT
                ).show()
                true
            }

            darkThemePref?.setOnPreferenceClickListener {
                val ctx = requireContext()
                val activity = requireActivity()
                updateAppTheme(prefs)
                val bundle = makeCustomAnimation(ctx, R.anim.fade_in, R.anim.fade_out).toBundle()
                activity.finish()
                startActivity(activity.intent, bundle)
                true
            }
        }

        override fun onExcludedAppsSelected(excludedApps: List<String>) {
            if (excludedApps.asString() == prefs.exclusions) return
            inject<TunnelManager>().value.getTunnels().thenAccept { tunnels ->
                if (excludedApps.isNotEmpty()) {
                    tunnels.forEach { tunnel ->
                        val oldConfig = tunnel.getConfig()
                        oldConfig?.let {
                            prefs.exclusionsArray.forEach { exclusion ->
                                it.`interface`.excludedApplications.remove(
                                        exclusion
                                )
                            }
                            it.`interface`.excludedApplications.addAll(excludedApps.toCollection(ArrayList()))
                            tunnel.setConfig(it)
                        }
                    }
                    prefs.exclusions = excludedApps.asString()
                } else {
                    tunnels.forEach { tunnel ->
                        prefs.exclusionsArray.forEach { exclusion ->
                            tunnel.getConfig()?.`interface`?.excludedApplications?.remove(exclusion)
                        }
                    }
                    prefs.exclusions = ""
                }
            }
        }
    }
}
