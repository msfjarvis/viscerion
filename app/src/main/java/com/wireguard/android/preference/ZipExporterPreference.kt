/*
 * Copyright © 2017-2018 WireGuard LLC. All Rights Reserved.
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
import com.wireguard.android.model.Tunnel
import com.wireguard.android.util.ExceptionLoggers
import com.wireguard.android.util.parentActivity
import com.wireguard.config.Config
import java9.util.concurrent.CompletableFuture
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.ArrayList
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Preference implementing a button that asynchronously exports config zips.
 */

class ZipExporterPreference(context: Context, attrs: AttributeSet) : Preference(context, attrs) {

    private var exportedFilePath: String? = null

    private fun exportZip() {
        Application.tunnelManager.completableTunnels.thenAccept { this.exportZip(it) }
    }

    private fun exportZip(tunnels: List<Tunnel>) {
        val futureConfigs = ArrayList<CompletableFuture<Config>>(tunnels.size)
        tunnels.forEach { futureConfigs.add(it.configAsync.toCompletableFuture()) }
        if (futureConfigs.isEmpty()) {
            exportZipComplete(null, IllegalArgumentException("No tunnels exist"))
            return
        }
        CompletableFuture.allOf(*futureConfigs.toTypedArray())
            .whenComplete { _, exception ->
                Application.asyncWorker.supplyAsync {
                    if (exception != null)
                        throw exception
                    val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val file = File(path, "wireguard-export.zip")
                    if (!path.isDirectory && !path.mkdirs())
                        throw IOException("Cannot create output directory")
                    try {
                        ZipOutputStream(FileOutputStream(file)).use { zip ->
                            for (i in futureConfigs.indices) {
                                zip.putNextEntry(ZipEntry(tunnels[i].name + ".conf"))
                                zip.write(futureConfigs[i].getNow(null).toWgQuickString().toByteArray(StandardCharsets.UTF_8))
                            }
                            zip.closeEntry()
                        }
                    } catch (e: Exception) {

                        file.delete()
                        throw e
                    }

                    file.absolutePath
                }.whenComplete(this::exportZipComplete)
            }
    }

    private fun exportZipComplete(filePath: String?, throwable: Throwable?) {
        if (throwable != null) {
            val error = ExceptionLoggers.unwrapMessage(throwable)
            val message = context.getString(R.string.zip_export_error, error)
            Timber.tag(TAG).e(message)
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
            context.getString(R.string.zip_export_summary)
        else
            context.getString(R.string.zip_export_success, exportedFilePath)
    }

    override fun getTitle(): CharSequence {
        return context.getString(R.string.zip_export_title)
    }

    override fun onClick() {
        parentActivity?.ensurePermissions(
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        ) { _, granted ->
            if (granted.isNotEmpty() && granted[0] == PackageManager.PERMISSION_GRANTED) {
                isEnabled = false
                exportZip()
            }
        }
    }

    companion object {
        private val TAG = "WireGuard/" + ZipExporterPreference::class.java.simpleName
    }
}
