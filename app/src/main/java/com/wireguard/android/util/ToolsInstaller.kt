/*
 * Copyright © 2017-2018 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.util

import android.content.Context
import android.system.OsConstants
import com.wireguard.android.BuildConfig
import com.wireguard.android.util.RootShell.NoRootException
import com.wireguard.android.util.SharedLibraryLoader.extractNativeLibrary
import org.koin.core.KoinComponent
import org.koin.core.inject
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

/**
 * Helper to install WireGuard tools to the system partition.
 */

class ToolsInstaller(val context: Context) {

    private val localBinaryDir: File by lazy { File(context.cacheDir, "bin") }
    private val nativeLibraryDir: File by lazy { getNativeLibraryDir(context) }
    private var areToolsAvailable: Boolean? = null
    private var installAsMagiskModule: Boolean? = null

    @Throws(NoRootException::class)
    fun areInstalled(): Int {
        if (INSTALL_DIR == null)
            return ERROR
        val script = StringBuilder()
        for (names in EXECUTABLES) {
            script.append(
                    "[ -f '${File(INSTALL_DIR, names[1])}' ] && cmp -s '${File(nativeLibraryDir, names[0])}' '${File(
                            INSTALL_DIR,
                            names[1]
                    )}' && "
            )
        }
        script.append("exit ").append(OsConstants.EALREADY).append(';')
        return try {
            val ret = rootShell.run(null, script.toString())
            if (ret == OsConstants.EALREADY) {
                if (willInstallAsMagiskModule()) YES or MAGISK else YES or SYSTEM
            } else {
                if (willInstallAsMagiskModule()) NO or MAGISK else NO or SYSTEM
            }
        } catch (ignored: IOException) {
            ERROR
        }
    }

    @Throws(FileNotFoundException::class, NoRootException::class)
    @Synchronized
    fun ensureToolsAvailable() {
        if (areToolsAvailable == null) {
            val ret = symlink()
            areToolsAvailable = when (ret) {
                OsConstants.EALREADY -> {
                    Timber.d("Tools were already symlinked into our private binary dir")
                    true
                }
                OsConstants.EXIT_SUCCESS -> {
                    Timber.d("Tools are now symlinked into our private binary dir")
                    true
                }
                else -> {
                    Timber.e("For some reason, wg and wg-quick are not available at all")
                    false
                }
            }
        }
        if (areToolsAvailable == false)
            throw FileNotFoundException("Required tools unavailable")
    }

    @Synchronized
    private fun willInstallAsMagiskModule(): Boolean {
        if (!isMagiskSu()) return false
        if (installAsMagiskModule == null) {
            installAsMagiskModule = try {
                rootShell.run(
                        null,
                        "[ -d $magiskDir -a ! -f /cache/.disable_magisk ]"
                ) == OsConstants.EXIT_SUCCESS
            } catch (ignored: Exception) {
                false
            }
        }
        return installAsMagiskModule == true
    }

    @Throws(NoRootException::class)
    private fun installSystem(): Int {
        if (INSTALL_DIR == null)
            return OsConstants.ENOENT
        val script = StringBuilder("set -ex; ")
        script.append("trap 'mount -o ro,remount /system' EXIT; mount -o rw,remount /system; ")
        for (names in EXECUTABLES) {
            val destination = File(INSTALL_DIR, names[1])
            script.append(
                    "cp '${File(
                            nativeLibraryDir,
                            names[0]
                    )}' '$destination'; chmod 755 '$destination'; restorecon '$destination' || true; "
            )
        }
        return try {
            if (rootShell.run(null, script.toString()) == 0) YES or SYSTEM else ERROR
        } catch (ignored: IOException) {
            ERROR
        }
    }

    @Throws(NoRootException::class)
    private fun installMagisk(): Int {
        val script = StringBuilder("set -ex; ")
        val magiskDirectory = "$magiskDir/wireguard"

        script.append("trap 'rm -rf $magiskDirectory' INT TERM EXIT; ")
        script.append(
                "rm -rf $magiskDirectory/; mkdir -p $magiskDirectory/$INSTALL_DIR; "
        )
        script.append(
                "printf 'name=Viscerion Command Line Tools\nversion=${BuildConfig.VERSION_NAME}\nversionCode=${BuildConfig.VERSION_CODE}\nauthor=msfjarvis\ndescription=Command line tools for Viscerion\nminMagisk=1800\n' > $magiskDirectory/module.prop; "
        )
        script.append("touch $magiskDirectory/auto_mount; ")
        for (names in EXECUTABLES) {
            val destination = File("$magiskDirectory/$INSTALL_DIR", names[1])
            script.append(
                    "cp '${File(
                            nativeLibraryDir,
                            names[0]
                    )}' '$destination'; chmod 755 '$destination'; chcon 'u:object_r:system_file:s0' '$destination' || true; "
            )
        }
        script.append("trap - INT TERM EXIT;")

        return try {
            if (rootShell.run(null, script.toString()) == 0) YES or MAGISK else ERROR
        } catch (ignored: IOException) {
            ERROR
        }
    }

    @Throws(NoRootException::class)
    fun install(): Int {
        return if (willInstallAsMagiskModule()) installMagisk() else installSystem()
    }

    @Throws(NoRootException::class)
    fun symlink(): Int {
        val script = StringBuilder("set -x; ")
        for (names in EXECUTABLES) {
            script.append(
                    "test '${File(nativeLibraryDir, names[0])}' -ef '${File(localBinaryDir, names[1])}' && "
            )
        }
        script.append("exit ").append(OsConstants.EALREADY).append("; set -e; ")

        for (names in EXECUTABLES) {
            script.append(
                    "ln -fns '${File(nativeLibraryDir, names[0])}' '${File(localBinaryDir, names[1])}'; "
            )
        }
        script.append("exit ").append(OsConstants.EXIT_SUCCESS).append(';')

        return try {
            rootShell.run(null, script.toString())
        } catch (ignored: IOException) {
            OsConstants.EXIT_FAILURE
        }
    }

    companion object : KoinComponent {
        const val ERROR = 0x0
        const val YES = 0x1
        const val NO = 0x2
        const val MAGISK = 0x4
        const val SYSTEM = 0x8

        private val EXECUTABLES = arrayOf(arrayOf("libwg.so", "wg"), arrayOf("libwg-quick.so", "wg-quick"))
        private val INSTALL_DIRS = arrayOf(File("/system/xbin"), File("/system/bin"))
        private val INSTALL_DIR by lazy { getInstallDir() }
        private val magiskDir by lazy { getMagiskDirectory() }
        private val rootShell by inject<RootShell>()

        private fun getMagiskDirectory(): String {
            val output = ArrayList<String>()
            rootShell.run(
                    output,
                    "su -V"
            )
            val magiskVer = output[0].toInt()
            return when {
                magiskVer in (18000..18100) -> "/sbin/.magisk/img"
                magiskVer >= 18101 -> "/data/adb/modules"
                else -> "/sbin/.core/img"
            }
        }

        private fun getInstallDir(): File? {
            val path = System.getenv("PATH") ?: return INSTALL_DIRS[0]
            val paths = path.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toList()
            for (dir in INSTALL_DIRS) {
                if (paths.contains(dir.path) && dir.isDirectory)
                    return dir
            }
            return null
        }

        private fun isMagiskSu(): Boolean {
            val output = ArrayList<String>()
            rootShell.run(output, "su --version")
            return output[0].contains("MAGISKSU")
        }

        private fun getNativeLibraryDir(context: Context): File {
            return if (context.applicationInfo.splitSourceDirs.isNullOrEmpty()) {
                File(context.applicationInfo.nativeLibraryDir)
            } else {
                // App bundles, unpack executables from the split config APK.
                EXECUTABLES.forEach {
                    extractNativeLibrary(
                            context,
                            it[0],
                            useActualName = true,
                            skipDeletion = true
                    )
                }
                context.cacheDir
            }
        }
    }
}
