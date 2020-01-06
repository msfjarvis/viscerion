/*
 * Copyright © 2017-2020 WireGuard LLC.
 * Copyright © 2018-2020 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.activity

import android.content.Context
import android.content.Intent
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
import com.wireguard.android.di.injector
import com.wireguard.android.fragment.AppListDialogFragment
import com.wireguard.android.model.TunnelManager
import com.wireguard.android.util.ApplicationPreferences
import com.wireguard.android.util.AsyncWorker
import com.wireguard.android.util.AuthenticationResult
import com.wireguard.android.util.Authenticator
import com.wireguard.android.util.BackendAsync
import com.wireguard.android.util.ExceptionLoggers
import com.wireguard.android.util.ZipExporter
import com.wireguard.android.util.humanReadablePath
import com.wireguard.android.util.isSystemDarkThemeEnabled
import com.wireguard.android.util.updateAppTheme
import java.io.File
import javax.inject.Inject
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
        @Inject lateinit var prefs: ApplicationPreferences
        @Inject lateinit var asyncWorker: AsyncWorker
        @Inject lateinit var backendAsync: BackendAsync
        @Inject lateinit var tunnelManager: TunnelManager

        override fun onAttach(context: Context) {
            injector.inject(this)
            super.onAttach(context)
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, key: String?) {
            addPreferencesFromResource(R.xml.preferences)
            val screen = preferenceScreen
            val ctx = requireContext()
            val wgQuickOnlyPrefs = arrayOf(
                    screen.findPreference<Preference>("tools_installer"),
                    screen.findPreference<CheckBoxPreference>("restore_on_boot")
            )
            val wgOnlyPrefs = arrayOf(
                    screen.findPreference<CheckBoxPreference>("whitelist_exclusions")
            )
            val exclusionsPref = preferenceManager.findPreference<Preference>("global_exclusions")
            val taskerPref = preferenceManager.findPreference<SwitchPreferenceCompat>("allow_tasker_integration")
            val integrationSecretPref =
                    preferenceManager.findPreference<EditTextPreference>("intent_integration_secret")
            val darkThemePref = preferenceManager.findPreference<CheckBoxPreference>("dark_theme")
            val zipExporterPref = preferenceManager.findPreference<Preference>("zip_exporter")
            val fingerprintPref = preferenceManager.findPreference<SwitchPreferenceCompat>("fingerprint_auth")
            for (pref in wgQuickOnlyPrefs + wgOnlyPrefs)
                pref?.isVisible = false

            if (BuildConfig.DEBUG && File("/sys/module/wireguard").exists()) {
                addPreferencesFromResource(R.xml.debug_preferences)
            }

            backendAsync.thenAccept { backend ->
                wgQuickOnlyPrefs.filterNotNull().forEach {
                    if (backend is WgQuickBackend) {
                        it.isVisible = true
                    } else {
                        screen.removePreference(it)
                    }
                }
                wgOnlyPrefs.filterNotNull().forEach {
                    if (backend is GoBackend) {
                        it.isVisible = true
                    } else {
                        screen.removePreference(it)
                    }
                }
            }

            zipExporterPref?.onPreferenceClickListener = ClickListener {
                createExportFile()
                true
            }

            integrationSecretPref?.isVisible = prefs.allowTaskerIntegration

            exclusionsPref?.onPreferenceClickListener = ClickListener {
                val fragment = AppListDialogFragment.newInstance(prefs.exclusions, true, this)
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
                ) {
                    getString(R.string.tasker_integration_summary_empty_secret)
                } else {
                    getString(R.string.tasker_integration_secret_summary)
                }
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
            tunnelManager.getTunnels().thenAccept { tunnels ->
                ZipExporter.exportZip(asyncWorker, ctx.contentResolver, fileUri, tunnels) { throwable ->
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
            if (prefs.exclusions == excludedApps) return
            tunnelManager.getTunnels().thenAccept { tunnels ->
                if (excludedApps.isNotEmpty()) {
                    tunnels.forEach { tunnel ->
                        val oldConfig = tunnel.getConfig()
                        if (oldConfig != null) {
                            prefs.exclusions.forEach {
                                oldConfig.interfaze.excludedApplications.remove(it)
                            }
                            oldConfig.interfaze.excludedApplications.addAll(ArrayList(excludedApps))
                            tunnel.setConfig(oldConfig)
                        }
                    }
                    prefs.exclusions = excludedApps.toSet()
                } else {
                    tunnels.forEach { tunnel ->
                        prefs.exclusions.forEach { exclusion ->
                            tunnel.getConfig()?.interfaze?.excludedApplications?.remove(exclusion)
                        }
                    }
                    prefs.exclusions = emptySet()
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
