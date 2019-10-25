/*
 * Copyright © 2017-2019 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.activity

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
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
import com.wireguard.android.di.ext.getBackendAsync
import com.wireguard.android.di.ext.getPrefs
import com.wireguard.android.di.ext.getTunnelManager
import com.wireguard.android.fragment.AppListDialogFragment
import com.wireguard.android.util.AuthenticationResult
import com.wireguard.android.util.Authenticator
import com.wireguard.android.util.ExceptionLoggers
import com.wireguard.android.util.ZipExporter
import com.wireguard.android.util.asString
import com.wireguard.android.util.humanReadablePath
import com.wireguard.android.util.isSystemDarkThemeEnabled
import com.wireguard.android.util.updateAppTheme
import java.io.File
import timber.log.Timber

/**
 * Interface for changing application-global persistent settings.
 */

typealias ClickListener = Preference.OnPreferenceClickListener

typealias ChangeListener = Preference.OnPreferenceChangeListener
typealias SummaryProvider<T> = Preference.SummaryProvider<T>

class SettingsActivity : AppCompatActivity() {

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

    class SettingsFragment : PreferenceFragmentCompat(), AppListDialogFragment.AppExclusionListener {
        private val prefs = getPrefs()

        override fun onCreatePreferences(savedInstanceState: Bundle?, key: String?) {
            addPreferencesFromResource(R.xml.preferences)
            val screen = preferenceScreen
            val ctx = requireContext()
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
            val zipExporterPref = preferenceManager.findPreference<Preference>("zip_exporter")
            val fingerprintPref = preferenceManager.findPreference<SwitchPreferenceCompat>("fingerprint_auth")
            for (pref in wgQuickOnlyPrefs + wgOnlyPrefs + debugOnlyPrefs)
                pref?.isVisible = false

            if (BuildConfig.DEBUG && File("/sys/module/wireguard").exists())
                debugOnlyPrefs.filterNotNull().forEach { it.isVisible = true }

            getBackendAsync().thenAccept { backend ->
                wgQuickOnlyPrefs.filterNotNull().forEach {
                    if (backend is WgQuickBackend)
                        it.isVisible = true
                    else
                        screen.removePreference(it)
                }
                wgOnlyPrefs.filterNotNull().forEach {
                    if (backend is GoBackend)
                        it.isVisible = true
                    else
                        screen.removePreference(it)
                }
            }

            zipExporterPref?.onPreferenceClickListener = ClickListener {
                createExportFile()
                true
            }

            integrationSecretPref?.isVisible = prefs.allowTaskerIntegration

            exclusionsPref?.onPreferenceClickListener = ClickListener {
                val fragment = AppListDialogFragment.newInstance(prefs.exclusionsArray, true, this)
                fragment.show(parentFragmentManager, null)
                true
            }

            taskerPref?.onPreferenceChangeListener = ChangeListener { _, newValue ->
                integrationSecretPref?.isVisible = newValue as Boolean
                true
            }

            integrationSecretPref?.summaryProvider = SummaryProvider<EditTextPreference> { preference ->
                if (prefs.allowTaskerIntegration &&
                        preference.isEnabled &&
                        prefs.taskerIntegrationSecret.isEmpty()
                )
                    getString(R.string.tasker_integration_summary_empty_secret)
                else
                    getString(R.string.tasker_integration_secret_summary)
            }

            altIconPref?.onPreferenceClickListener = ClickListener {
                val checked = (it as CheckBoxPreference).isChecked
                ctx.packageManager.apply {
                    setComponentEnabledSetting(
                            ComponentName(ctx.packageName, "${ctx.packageName}.LauncherActivity"),
                            if (checked)
                                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                            else
                                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                            PackageManager.DONT_KILL_APP
                    )
                    setComponentEnabledSetting(
                            ComponentName(ctx.packageName, "${ctx.packageName}.AltIconLauncherActivity"),
                            if (checked)
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

            darkThemePref?.apply {
                val isSystemDark = ctx.isSystemDarkThemeEnabled()
                val darkThemeOverride = prefs.useDarkTheme
                summaryProvider = SummaryProvider<CheckBoxPreference> {
                    if (isSystemDark) {
                        getString(R.string.dark_theme_summary_auto)
                    } else {
                        getString(R.string.pref_dark_theme_summary)
                    }
                }
                onPreferenceClickListener = ClickListener {
                    updateAppTheme(prefs.useDarkTheme)
                    true
                }
                if (isSystemDark && !darkThemeOverride) {
                    isEnabled = false
                    isChecked = true
                    /*
                    HACK ALERT: Open for better solutions
                    Toggling checked state here causes the preference key's value to flip as well
                    which causes a plethora of bugs later on. So as a "fix" we'll just restore the
                    original value back to avoid the whole mess.
                     */
                    prefs.useDarkTheme = darkThemeOverride
                } else {
                    isEnabled = true
                    isChecked = darkThemeOverride
                }
            }
            fingerprintPref?.apply {
                val isFingerprintSupported = BiometricManager.from(requireContext()).canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS
                if (!isFingerprintSupported) {
                    isEnabled = false
                    isChecked = false
                    summary = getString(R.string.biometric_auth_summary_error)
                } else {
                    setOnPreferenceClickListener {
                        val checked = isChecked
                        Authenticator(requireActivity()) { result ->
                            when (result) {
                                is AuthenticationResult.Success -> {
                                    // Apply the changes
                                    prefs.fingerprintAuth = checked
                                }
                                else -> {
                                    // If any error occurs, revert back to the previous state. This
                                    // catch-all clause includes the cancellation case.
                                    prefs.fingerprintAuth = !checked
                                    isChecked = !checked
                                }
                            }
                        }.authenticate()
                        true
                    }
                }
            }
        }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            data?.data?.also { uri ->
                Timber.d("Exporting configs as ZIP to ${uri.path}")
                if (requestCode == REQUEST_LOG_SAVE && resultCode == RESULT_OK) {
                    exportZip(uri)
                } else {
                    super.onActivityResult(requestCode, resultCode, data)
                }
            }
        }

        private fun exportZip(fileUri: Uri) {
            val ctx = requireContext()
            val snackbarView = requireNotNull(requireActivity().findViewById<View>(android.R.id.content))
            getTunnelManager().getTunnels().thenAccept { tunnels ->
                ZipExporter.exportZip(ctx.contentResolver, fileUri, tunnels) { throwable ->
                    if (throwable != null) {
                        val error = ExceptionLoggers.unwrapMessage(throwable)
                        val message = ctx.getString(R.string.zip_export_error, error)
                        Timber.e(message)
                        Snackbar.make(snackbarView, message, Snackbar.LENGTH_LONG).show()
                    } else {
                        Snackbar.make(snackbarView, ctx.getString(R.string.zip_export_success, fileUri.humanReadablePath), Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }

        override fun onExcludedAppsSelected(excludedApps: List<String>) {
            if (excludedApps.asString() == prefs.exclusions) return
            getTunnelManager().getTunnels().thenAccept { tunnels ->
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

        private fun createExportFile() {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/zip"
                putExtra(Intent.EXTRA_TITLE, "viscerion-export.zip")
            }
            startActivityForResult(intent, REQUEST_LOG_SAVE)
        }

        companion object {
            const val REQUEST_LOG_SAVE = 1234
        }
    }
}
