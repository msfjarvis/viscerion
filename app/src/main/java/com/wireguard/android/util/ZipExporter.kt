/*
 * Copyright © 2017-2018 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.util

import android.os.Environment
import com.wireguard.android.model.Tunnel
import com.wireguard.config.Config
import java9.util.concurrent.CompletableFuture
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ZipExporter : KoinComponent {
    fun exportZip(
        tunnels: List<Tunnel>,
        onExportCompleteCallback: (filePath: String?, throwable: Throwable?) -> Unit
    ) {
        val futureConfigs = ArrayList<CompletableFuture<Config>>(tunnels.size)
        tunnels.forEach { futureConfigs.add(it.configAsync.toCompletableFuture()) }
        if (futureConfigs.isEmpty()) {
            onExportCompleteCallback(null, IllegalArgumentException("No tunnels exist"))
            return
        }
        CompletableFuture.allOf(*futureConfigs.toTypedArray())
                .whenComplete { _, exception ->
                    inject<AsyncWorker>().value.supplyAsync {
                        if (exception != null)
                            throw exception
                        val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                        val file = File(path, "viscerion-export.zip")
                        if (!path.isDirectory && !path.mkdirs())
                            throw IOException("Cannot create output directory")
                        try {
                            ZipOutputStream(FileOutputStream(file)).use { zip ->
                                for (i in futureConfigs.indices) {
                                    zip.putNextEntry(ZipEntry("${tunnels[i].name}.conf"))
                                    zip.write(futureConfigs[i].getNow(null).toWgQuickString().toByteArray(StandardCharsets.UTF_8))
                                }
                                zip.closeEntry()
                            }
                        } catch (e: Exception) {
                            file.delete()
                            throw e
                        }

                        file.absolutePath
                    }.whenComplete { path, throwable ->
                        onExportCompleteCallback(path, throwable)
                    }
                }
    }
}
