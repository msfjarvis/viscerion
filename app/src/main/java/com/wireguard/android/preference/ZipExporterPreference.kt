/*
 * Copyright © 2017-2018 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.preference

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.AttributeSet
import android.view.View
import androidx.preference.Preference
import com.google.android.material.snackbar.Snackbar
import com.wireguard.android.R
import com.wireguard.android.activity.SettingsActivity
import com.wireguard.android.model.TunnelManager
import com.wireguard.android.util.ExceptionLoggers
import com.wireguard.android.util.ZipExporter
import com.wireguard.android.util.getParentActivity
import org.koin.core.KoinComponent
import org.koin.core.inject
import timber.log.Timber

/**
 * Preference implementing a button that asynchronously exports config zips.
 */

class ZipExporterPreference(context: Context, attrs: AttributeSet) : Preference(context, attrs), KoinComponent {
    private var exportedFilePath: String? = null

    private fun exportZip() {
        inject<TunnelManager>().value.getTunnels().thenAccept {
            ZipExporter.exportZip(it) { filePath, throwable ->
                exportZipComplete(filePath, throwable)
            }
        }
    }

    private fun exportZipComplete(filePath: String?, throwable: Throwable?) {
        if (throwable != null) {
            val error = ExceptionLoggers.unwrapMessage(throwable)
            val message = context.getString(R.string.zip_export_error, error)
            Timber.e(message)
            getParentActivity<SettingsActivity>()?.findViewById<View>(android.R.id.content)?.let {
                Snackbar.make(it, message, Snackbar.LENGTH_LONG).show()
            }
            isEnabled = true
        } else {
            exportedFilePath = filePath
            notifyChanged()
        }
    }

    override fun getSummary(): CharSequence {
        return if (exportedFilePath == null)
            context.getString(R.string.zip_export_summary)
        else
            context.getString(R.string.zip_export_success, exportedFilePath)
    }

    override fun getTitle(): CharSequence {
        return context.getString(R.string.zip_export_title)
    }

    override fun onClick() {
        getParentActivity<SettingsActivity>()?.ensurePermissions(
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        ) { _, granted ->
            if (granted.isNotEmpty() && granted[0] == PackageManager.PERMISSION_GRANTED) {
                isEnabled = false
                exportZip()
            }
        }
    }
}
