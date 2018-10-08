/*
 * Copyright © 2017-2018 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.wireguard.android.fragment

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.wireguard.android.Application
import com.wireguard.android.R
import com.wireguard.android.databinding.AppListDialogFragmentBinding
import com.wireguard.android.model.ApplicationData
import com.wireguard.android.util.ApplicationPreferences
import com.wireguard.android.util.ExceptionLoggers
import com.wireguard.android.util.ObservableKeyedArrayList
import java.util.ArrayList
import java.util.Arrays
import java.util.Comparator

class AppListDialogFragment : DialogFragment() {

    private var currentlyExcludedApps: List<String>? = null
    private var isGlobalExclusionsDialog = false
    private val appData = ObservableKeyedArrayList<String, ApplicationData>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        currentlyExcludedApps = Arrays.asList(*arguments!!.getStringArray(KEY_EXCLUDED_APPS)!!)
        isGlobalExclusionsDialog = arguments!!.getBoolean(KEY_GLOBAL_EXCLUSIONS)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val alertDialogBuilder = AlertDialog.Builder(activity!!)
        alertDialogBuilder.setTitle(R.string.excluded_applications)

        val binding = AppListDialogFragmentBinding.inflate(activity!!.layoutInflater, null, false)
        binding.executePendingBindings()
        alertDialogBuilder.setView(binding.root)

        alertDialogBuilder.setPositiveButton(R.string.set_exclusions) { _, _ -> setExclusionsAndDismiss() }
        alertDialogBuilder.setNeutralButton(R.string.deselect_all) { _, _ -> }

        binding.fragment = this
        binding.appData = appData

        loadData()

        val dialog = alertDialogBuilder.create()
        dialog.setOnShowListener { _ ->
            dialog.getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener {
                for (app in appData)
                    app.isExcludedFromTunnel = false
            }
        }
        return dialog
    }

    @SuppressLint("InlinedApi") // Handled in the code
    private fun loadData() {
        val activity = activity ?: return
        val seenPackages: ArrayList<String> = ArrayList()

        val pm = activity.packageManager
        Application.asyncWorker.supplyAsync<List<ApplicationData>> {
            val launcherIntent = Intent(Intent.ACTION_MAIN, null)
            launcherIntent.addCategory(Intent.CATEGORY_LAUNCHER)
            val resolveInfos = pm.queryIntentActivities(
                launcherIntent, when {
                    Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP -> PackageManager.MATCH_DISABLED_COMPONENTS
                    else -> 0
                }
            )
            val appData = ArrayList<ApplicationData>()
            for (resolveInfo in resolveInfos) {
                val packageName = resolveInfo.activityInfo.packageName
                if (packageName in seenPackages) {
                    continue
                } else {
                    seenPackages.add(packageName)
                }
                appData.add(
                    ApplicationData(
                        resolveInfo.loadIcon(pm),
                        resolveInfo.loadLabel(pm).toString(),
                        packageName,
                        currentlyExcludedApps!!.contains(packageName),
                        if (isGlobalExclusionsDialog) false else ApplicationPreferences.exclusionsArray.contains(
                            packageName
                        )
                    )
                )
            }

            appData.sortWith(Comparator { lhs, rhs -> lhs.name.toLowerCase().compareTo(rhs.name.toLowerCase()) })
            appData
        }.whenComplete { data, throwable ->
            if (data != null) {
                appData.clear()
                appData.addAll(data)
            } else {
                val error = if (throwable != null) ExceptionLoggers.unwrapMessage(throwable) else "Unknown"
                val message = activity.getString(R.string.error_fetching_apps, error)
                Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
                dismissAllowingStateLoss()
            }
        }
    }

    private fun setExclusionsAndDismiss() {
        val excludedApps = ArrayList<String>()
        for (data in appData) {
            if (data.isExcludedFromTunnel) {
                excludedApps.add(data.packageName)
            }
        }

        (targetFragment as AppExclusionListener).onExcludedAppsSelected(excludedApps)
        Toast.makeText(context, getString(R.string.applist_dialog_success), Toast.LENGTH_SHORT).show()
        dismiss()
    }

    interface AppExclusionListener {
        fun onExcludedAppsSelected(excludedApps: List<String>)
    }

    companion object {

        private const val KEY_EXCLUDED_APPS = "excludedApps"
        private const val KEY_GLOBAL_EXCLUSIONS = "globalExclusions"

        fun <T> newInstance(
            excludedApps: Array<String>,
            isGlobalExclusions: Boolean = false,
            target: T
        ): AppListDialogFragment where T : Fragment, T : AppListDialogFragment.AppExclusionListener {
            val extras = Bundle()
            extras.putStringArray(KEY_EXCLUDED_APPS, excludedApps)
            extras.putBoolean(KEY_GLOBAL_EXCLUSIONS, isGlobalExclusions)
            val fragment = AppListDialogFragment()
            fragment.setTargetFragment(target, 0)
            fragment.arguments = extras
            return fragment
        }
    }
}
