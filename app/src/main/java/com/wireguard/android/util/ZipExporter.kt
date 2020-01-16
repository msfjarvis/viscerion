/*
 * Copyright © 2017-2020 WireGuard LLC.
 * Copyright © 2018-2020 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.util

import android.content.ContentResolver
import android.net.Uri
import com.wireguard.android.model.Tunnel
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java9.util.concurrent.CompletableFuture
import me.msfjarvis.viscerion.config.Config

object ZipExporter {
    fun exportZip(
        asyncWorker: AsyncWorker,
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
                asyncWorker.runAsync {
                    if (exception != null) {
                        throw exception
                    }
                    try {
                        contentResolver.openFileDescriptor(fileUri, "w")?.use { pfd ->
                            ZipOutputStream(FileOutputStream(pfd.fileDescriptor)).use { zip ->
                                for (i in futureConfigs.indices) {
                                    zip.putNextEntry(ZipEntry("${tunnels[i].name}.conf"))
                                    zip.write(
                                        futureConfigs[i].getNow(null).toWgQuickString().toByteArray(
                                            StandardCharsets.UTF_8
                                        )
                                    )
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
