/*
 * Copyright © 2017-2018 WireGuard LLC. All Rights Reserved.
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
    private val TAG = "WireGuard/" + SharedLibraryLoader::class.java.simpleName

    @Suppress("UnsafeDynamicallyLoadedCode")
    fun loadSharedLibrary(context: Context, libName: String) {
        var noAbiException: Throwable
        try {
            System.loadLibrary(libName)
            return
        } catch (e: UnsatisfiedLinkError) {
            Timber.tag(TAG).d(e, "Failed to load library normally, so attempting to extract from apk")
            noAbiException = e
        }

        val zipFile: ZipFile
        try {
            zipFile = ZipFile(File(context.applicationInfo.sourceDir), ZipFile.OPEN_READ)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

        val mappedLibName = System.mapLibraryName(libName)
        for (abi in Build.SUPPORTED_ABIS) {
            val libZipPath = "lib" + File.separatorChar + abi + File.separatorChar + mappedLibName
            val zipEntry = zipFile.getEntry(libZipPath) ?: continue
            var f: File? = null
            try {
                f = File.createTempFile("lib", ".so", context.cacheDir)
                Timber.tag(TAG).d("Extracting apk:/$libZipPath to ${f?.absolutePath} and loading")
                FileOutputStream(f).use { out ->
                    zipFile.getInputStream(zipEntry).use { inputStream ->
                        inputStream.copyTo(out)
                    }
                }
                System.load(f.absolutePath)
                return
            } catch (e: Exception) {
                Timber.tag(TAG).d(e, "Failed to load library apk:/$libZipPath")
                noAbiException = e
            } finally {
                f?.delete()
            }
        }
        if (noAbiException is RuntimeException)
            throw noAbiException
        throw RuntimeException(noAbiException)
    }
}
