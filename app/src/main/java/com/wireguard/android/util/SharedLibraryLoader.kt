/*
 * Copyright © 2017-2019 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.util

import android.content.Context
import android.os.Build
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipFile
import timber.log.Timber

object SharedLibraryLoader {

    fun extractNativeLibrary(
        context: Context,
        libName: String,
        destination: File
    ): Boolean {
        val apkPath = getApkPath(context)
        Timber.d("apkPath: $apkPath")
        val zipFile: ZipFile
        try {
            zipFile = ZipFile(File(apkPath), ZipFile.OPEN_READ)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

        val mappedLibName = if (libName.contains(".so")) libName else System.mapLibraryName(libName)
        for (abi in Build.SUPPORTED_ABIS) {
            val libZipPath = "lib" + File.separatorChar + abi + File.separatorChar + mappedLibName
            val zipEntry = zipFile.getEntry(libZipPath) ?: continue
            try {
                Timber.d("Extracting apk:/$libZipPath to ${destination.absolutePath} and loading")
                FileOutputStream(destination).use { out ->
                    zipFile.getInputStream(zipEntry).use { inputStream ->
                        inputStream.copyTo(out)
                    }
                }
                zipFile.close()
                return true
            } catch (e: Exception) {
                Timber.d(e, "Failed to load library apk:/$libZipPath")
                throw e
            }
        }
        zipFile.close()
        return false
    }

    @Suppress("UnsafeDynamicallyLoadedCode")
    fun loadSharedLibrary(context: Context, libName: String) {
        var noAbiException: Throwable
        try {
            System.loadLibrary(libName)
            return
        } catch (e: UnsatisfiedLinkError) {
            Timber.d(e, "Failed to load library normally, so attempting to extract from apk")
            noAbiException = e
        }

        var f: File? = null
        try {
            f = File.createTempFile("lib", "so", context.codeCacheDir)
            if (extractNativeLibrary(context, libName, f)) {
                System.load(f.absolutePath)
                return
            }
        } catch (e: Exception) {
            Timber.d("Failed to load library apk:/$libName")
            Timber.d(e)
            noAbiException = e
        } finally {
            f?.delete()
        }

        if (noAbiException is RuntimeException) {
            throw noAbiException
        }
        throw RuntimeException(noAbiException)
    }

    private fun getApkPath(context: Context): String {
        val splitDirs = context.applicationInfo.splitSourceDirs
        if (!splitDirs.isNullOrEmpty()) {
            for (abi in Build.SUPPORTED_ABIS) {
                for (splitDir in splitDirs) {
                    val splits = splitDir.split("/")
                    val apkName = splits[splits.size - 1]
                    if (apkName.contains(abi.replace("-", "_"))) {
                        return splitDir
                    }
                }
            }
        }
        return context.applicationInfo.sourceDir
    }
}
