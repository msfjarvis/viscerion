/*
 * Copyright © 2017-2019 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.util

import android.content.ContentResolver
import android.net.Uri
import com.wireguard.android.di.ext.getAsyncWorker
import com.wireguard.android.model.Tunnel
import com.wireguard.config.Config
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java9.util.concurrent.CompletableFuture
import org.koin.core.KoinComponent

object ZipExporter : KoinComponent {
    fun exportZip(
        contentResolver: ContentResolver,
        fileUri: Uri,
        tunnels: List<Tunnel>,
        onExportCompleteCallback: (throwable: Throwable?) -> Unit
    ) {
        val futureConfigs = ArrayList<CompletableFuture<Config>>(tunnels.size)
        tunnels.forEach { futureConfigs.add(it.configAsync.toCompletableFuture()) }
        if (futureConfigs.isEmpty()) {
            onExportCompleteCallback(IllegalArgumentException("No tunnels exist"))
            return
        }
        CompletableFuture.allOf(*futureConfigs.toTypedArray())
                .whenComplete { _, exception ->
                    getAsyncWorker().runAsync {
                        if (exception != null) {
                            throw exception
                        }
                        try {
                            contentResolver.openFileDescriptor(fileUri, "w")?.use { pfd ->
                                ZipOutputStream(FileOutputStream(pfd.fileDescriptor)).use { zip ->
                                    for (i in futureConfigs.indices) {
                                        zip.putNextEntry(ZipEntry("${tunnels[i].name}.conf"))
                                        zip.write(futureConfigs[i].getNow(null).toWgQuickString(exporting = true).toByteArray(StandardCharsets.UTF_8))
                                    }
                                    zip.closeEntry()
                                }
                            }
                        } catch (e: Exception) {
                            throw e
                        }
                    }.whenComplete { _, throwable ->
                        onExportCompleteCallback(throwable)
                    }
                }
    }
}
