/*
 * Copyright © 2017-2019 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.util

import android.content.Context
import android.system.OsConstants
import com.wireguard.android.BuildConfig
import com.wireguard.android.util.RootShell.NoRootException
import dagger.Reusable
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import javax.inject.Inject
import timber.log.Timber

/**
 * Helper to install WireGuard tools to the system partition.
 */

@Reusable
class ToolsInstaller @Inject constructor(private val context: Context, private val rootShell: RootShell) {

    private val localBinaryDir = File(context.codeCacheDir, "bin")
    private val magiskDir by lazy { getMagiskDirectory() }
    private var areToolsAvailable: Boolean? = null
    private var installAsMagiskModule: Boolean? = null

    @Throws(NoRootException::class)
    fun areInstalled(): Int {
        if (INSTALL_DIR == null) {
            return ERROR
        }
        val script = StringBuilder()
        for (name in EXECUTABLES) {
            script.append(
                    "cmp -s '${File(localBinaryDir, name).absolutePath}' '${File(INSTALL_DIR, name).absolutePath}' && "
            )
        }
        script.append("exit ").append(OsConstants.EALREADY).append(';')
        return try {
            val ret = rootShell.run(null, script.toString())
            if (ret == OsConstants.EALREADY) {
                if (willInstallAsMagiskModule()) {
                    YES or MAGISK
                } else {
                    YES or SYSTEM
                }
            } else {
                if (willInstallAsMagiskModule()) {
                    NO or MAGISK
                } else {
                    NO or SYSTEM
                }
            }
        } catch (_: IOException) {
            ERROR
        }
    }

    @Throws(FileNotFoundException::class)
    @Synchronized
    fun ensureToolsAvailable() {
        if (areToolsAvailable == null) {
            areToolsAvailable = try {
                Timber.d(if (extract()) "Tools are now extracted into our private binary dir" else "Tools were already extracted into our private binary dir")
                true
            } catch (e: IOException) {
                Timber.e("The wg and wg-quick tools are not available")
                Timber.e(e)
                false
            }
        }
        if (areToolsAvailable == false) {
            throw FileNotFoundException("Required tools unavailable")
        }
    }

    @Synchronized
    private fun willInstallAsMagiskModule(): Boolean {
        if (!isMagiskSu()) {
            return false
        }
        if (installAsMagiskModule == null) {
            installAsMagiskModule = try {
                rootShell.run(
                        null,
                        "[ -d $magiskDir -a ! -f /cache/.disable_magisk ]"
                ) == OsConstants.EXIT_SUCCESS
            } catch (_: Exception) {
                false
            }
        }
        return installAsMagiskModule == true
    }

    @Throws(NoRootException::class, IOException::class)
    private fun installSystem(): Int {
        if (INSTALL_DIR == null) {
            return OsConstants.ENOENT
        }
        extract()
        val script = StringBuilder("set -ex; ")
        script.append("trap 'mount -o ro,remount /system' EXIT; mount -o rw,remount /system; ")
        for (name in EXECUTABLES) {
            val destination = File(INSTALL_DIR, name)
            script.append(
                    "cp '${File(localBinaryDir, name)}' '$destination'; chmod 755 '$destination'; restorecon '$destination' || true; "
            )
        }
        return try {
            if (rootShell.run(null, script.toString()) == 0) YES or SYSTEM else ERROR
        } catch (_: IOException) {
            ERROR
        }
    }

    @Throws(NoRootException::class, IOException::class)
    private fun installMagisk(): Int {
        extract()
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
        for (name in EXECUTABLES) {
            val destination = File("$magiskDirectory/$INSTALL_DIR", name)
            script.append(
                    "cp '${File(localBinaryDir, name)}' '$destination'; chmod 755 '$destination'; chcon 'u:object_r:system_file:s0' '$destination' || true; "
            )
        }
        script.append("trap - INT TERM EXIT;")

        return try {
            if (rootShell.run(null, script.toString()) == 0) {
                YES or MAGISK
            } else {
                ERROR
            }
        } catch (_: IOException) {
            ERROR
        }
    }

    @Throws(NoRootException::class, IOException::class)
    fun install(): Int {
        return if (willInstallAsMagiskModule()) {
            installMagisk()
        } else {
            installSystem()
        }
    }

    @Throws(IOException::class)
    fun extract(): Boolean {
        localBinaryDir.mkdirs()
        val files: Array<File?> = arrayOfNulls(EXECUTABLES.size)
        var allExist = false
        for (i in files.indices) {
            files[i] = File(localBinaryDir, EXECUTABLES[i])
            allExist = allExist and requireNotNull(files[i]).exists()
        }
        if (allExist) return false
        for (i in files.indices) {
            val file = requireNotNull(files[i])
            if (!SharedLibraryLoader.extractNativeLibrary(context, EXECUTABLES[i], file)) {
                throw FileNotFoundException("Unable to find ${EXECUTABLES[i]}")
            }
            if (!file.setExecutable(true, false)) {
                throw IOException("Unable to mark ${file.absolutePath} as executable")
            }
        }
        return true
    }

    private fun getMagiskDirectory(): String {
        val output = ArrayList<String>()
        rootShell.run(
                output,
                "su -V"
        )
        val magiskVer = output[0].toInt()
        return when {
            magiskVer in 18000..18100 -> "/sbin/.magisk/img"
            magiskVer >= 18101 -> "/data/adb/modules"
            else -> "/sbin/.core/img"
        }
    }

    private fun isMagiskSu(): Boolean {
        val output = ArrayList<String>()
        rootShell.run(output, "su --version")
        return output[0].contains("MAGISKSU")
    }

    companion object {
        const val ERROR = 0x0
        const val YES = 0x1
        const val NO = 0x2
        const val MAGISK = 0x4
        const val SYSTEM = 0x8

        private val EXECUTABLES = arrayOf("wg", "wg-quick")
        private val INSTALL_DIRS = arrayOf(File("/system/xbin"), File("/system/bin"))
        private val INSTALL_DIR by lazy { getInstallDir() }

        private fun getInstallDir(): File? {
            val path = System.getenv("PATH") ?: return INSTALL_DIRS[0]
            val paths = path.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toList()
            for (dir in INSTALL_DIRS) {
                if (paths.contains(dir.path) && dir.isDirectory) {
                    return dir
                }
            }
            return null
        }
    }
}
