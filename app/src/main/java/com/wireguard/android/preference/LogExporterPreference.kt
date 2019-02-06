/*
 * Copyright © 2019 Harsh Shandilya. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.preference

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Environment
import android.util.AttributeSet
import android.view.View
import androidx.preference.Preference
import com.google.android.material.snackbar.Snackbar
import com.wireguard.android.Application
import com.wireguard.android.R
import com.wireguard.android.util.ExceptionLoggers
import com.wireguard.android.util.parentActivity
import timber.log.Timber
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader

/**
 * Preference implementing a button that asynchronously exports logs.
 */

class LogExporterPreference(context: Context, attrs: AttributeSet) : Preference(context, attrs) {

    private var exportedFilePath: String? = null

    private fun exportLog() {
        Application.asyncWorker.supplyAsync {
            val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(path, "wireguard-log.txt")
            if (!path.isDirectory && !path.mkdirs())
                throw IOException("Cannot create output directory")

            /* We would like to simply run `builder.redirectOutput(file);`, but this is API 26.
             * Instead we have to do this dance, since logcat appends.
             */
            FileOutputStream(file).close()

            try {
                val process = Runtime.getRuntime()
                    .exec(arrayOf("logcat", "-b", "all", "-d", "-v", "threadtime", "-f", file.absolutePath, "*:V"))
                if (process.waitFor() != 0) {
                    BufferedReader(InputStreamReader(process.errorStream)).use { reader ->
                        val errors = StringBuilder()
                        errors.append("Unable to run logcat: ")
                        reader.readLines().forEach { if (it.isNotEmpty()) errors.append(it) }
                        throw Exception(errors.toString())
                    }
                }
            } catch (e: Exception) {

                file.delete()
                throw e
            }

            file.absolutePath
        }.whenComplete(this::exportLogComplete)
    }

    private fun exportLogComplete(filePath: String, throwable: Throwable?) {
        if (throwable != null) {
            val error = ExceptionLoggers.unwrapMessage(throwable)
            val message = context.getString(R.string.log_export_error, error)
            Timber.e(throwable)
            parentActivity?.findViewById<View>(android.R.id.content)?.let {
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
            context.getString(R.string.log_export_summary)
        else
            context.getString(R.string.log_export_success, exportedFilePath)
    }

    override fun getTitle(): CharSequence {
        return context.getString(R.string.log_export_title)
    }

    override fun onClick() {
        parentActivity?.ensurePermissions(
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        ) { _, granted ->
            if (granted.isNotEmpty() && granted[0] == PackageManager.PERMISSION_GRANTED) {
                isEnabled = false
                exportLog()
            }
        }
    }
}
