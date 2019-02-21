/*
 * Copyright © 2017-2018 WireGuard LLC.
 * Copyright © 2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.util

import android.content.Context
import android.os.Build
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipFile

object SharedLibraryLoader {

    fun extractNativeLibrary(
        context: Context,
        libName: String,
        useActualName: Boolean = false,
        skipDeletion: Boolean = false
    ): String {
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
            var f: File? = null
            try {
                f = if (useActualName)
                    File(context.cacheDir.absolutePath + File.separatorChar + mappedLibName)
                else
                    File.createTempFile("lib", ".so", context.cacheDir)
                Timber.d("Extracting apk:/$libZipPath to ${f?.absolutePath} and loading")
                FileOutputStream(f).use { out ->
                    zipFile.getInputStream(zipEntry).use { inputStream ->
                        inputStream.copyTo(out)
                    }
                }
                return f!!.absolutePath
            } catch (e: Exception) {
                Timber.d(e, "Failed to load library apk:/$libZipPath")
                throw e
            } finally {
                if (!skipDeletion) f?.delete()
            }
        }
        return ""
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

        val libPath = extractNativeLibrary(context, libName)
        if (libPath.isNotEmpty()) {
            try {
                System.load(libPath)
            } catch (e: Exception) {
                Timber.d(e, "Failed to load library apk:/$libPath")
                noAbiException = e
            }
        }

        if (noAbiException is RuntimeException)
            throw noAbiException
        throw RuntimeException(noAbiException)
    }

    private fun getApkPath(context: Context): String {
        val splitDirs = context.applicationInfo.splitSourceDirs
        if (!splitDirs.isNullOrEmpty()) {
            for (abi in Build.SUPPORTED_ABIS) {
                for (splitDir in splitDirs) {
                    val splits = splitDir.split("/")
                    val apkName = splits[splits.size - 1]
                    if (apkName.contains(abi.replace("-", "_")))
                        return splitDir
                }
            }
        }
        return context.applicationInfo.sourceDir
    }
}
