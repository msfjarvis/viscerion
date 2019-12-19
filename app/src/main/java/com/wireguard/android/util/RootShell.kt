/*
 * Copyright © 2017-2019 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.util

import android.content.Context
import com.wireguard.android.BuildConfig
import com.wireguard.android.R
import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.util.UUID
import timber.log.Timber

class RootShell(val context: Context) {

    private val deviceNotRootedMessage: String by lazy { context.getString(R.string.error_root) }
    private val localBinaryDir: File = File(context.codeCacheDir, "bin")
    private val localTemporaryDir: File = File(context.cacheDir, "tmp")
    private val preamble: String = "export CALLING_PACKAGE=${BuildConfig.APPLICATION_ID} PATH=\"$localBinaryDir:\$PATH\" TMPDIR='$localTemporaryDir'; id -u\n"
    private var process: Process? = null
    private lateinit var stderr: BufferedReader
    private lateinit var stdin: OutputStreamWriter
    private lateinit var stdout: BufferedReader
    private val isSuAvailable: Boolean
        get() {
            val path = System.getenv("PATH") ?: return false
            for (dir in path.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
                if (File(dir, SU).canExecute()) {
                    return true
                }
            }
            return false
        }

    @Synchronized
    private fun isRunning(): Boolean {
        return try {
            // Throws an exception if the process hasn't finished yet.
            process?.exitValue()
            false
        } catch (_: IllegalThreadStateException) {
            // The existing process is still running.
            true
        }
    }

    @Synchronized
    @Throws(IOException::class, NoRootException::class)
    fun run(output: ArrayList<String>? = null, command: String): Int {
        start()
        val marker = UUID.randomUUID().toString()
        val script = "echo $marker; echo $marker >&2; ($command); ret=$?; echo $marker \$ret; echo $marker \$ret >&2\n"
        Timber.d("executing: %s", command)
        stdin.write(script)
        stdin.flush()
        var line: String?
        var errnoStdout = Integer.MIN_VALUE
        var errnoStderr = Integer.MAX_VALUE
        var markersSeen = 0
        while (true) {
            line = stdout.readLine() ?: break
            if (line.startsWith(marker)) {
                ++markersSeen
                if (line.length > marker.length + 1) {
                    errnoStdout = Integer.valueOf(line.substring(marker.length + 1))
                    break
                }
            } else if (markersSeen > 0) {
                output?.add(line)
                Timber.d("stdout: %s", line)
            }
        }
        while (true) {
            line = stderr.readLine() ?: break
            if (line.startsWith(marker)) {
                ++markersSeen
                if (line.length > marker.length + 1) {
                    errnoStderr = Integer.valueOf(line.substring(marker.length + 1))
                    break
                }
            } else if (markersSeen > 2) {
                Timber.d("stdout: %s", line)
            }
        }
        if (markersSeen != 4) {
            throw IOException(context.getString(R.string.shell_marker_count_error, markersSeen))
        }
        if (errnoStdout != errnoStderr) {
            throw IOException("Unable to read exit status")
        }
        Timber.d("exit: %s", errnoStdout)
        return errnoStdout
    }

    @Synchronized
    @Throws(IOException::class, NoRootException::class)
    fun start() {
        if (!isSuAvailable) {
            throw NoRootException(deviceNotRootedMessage)
        }
        if (isRunning()) {
            return
        }
        if (!localBinaryDir.isDirectory && !localBinaryDir.mkdirs()) {
            throw FileNotFoundException("Could not create local binary directory")
        }
        if (!localTemporaryDir.isDirectory && !localTemporaryDir.mkdirs()) {
            throw FileNotFoundException("Could not create local temporary directory")
        }
        try {
            val builder = ProcessBuilder().command(SU)
            builder.environment()["LC_ALL"] = "C"
            try {
                process = builder.start()
            } catch (e: IOException) {
                // A failure at this stage means the device isn't rooted.
                throw NoRootException(deviceNotRootedMessage, e)
            }

            stdin = OutputStreamWriter(process!!.outputStream, StandardCharsets.UTF_8)
            stdout = BufferedReader(
                    InputStreamReader(
                            process!!.inputStream,
                            StandardCharsets.UTF_8
                    )
            )
            stderr = BufferedReader(
                    InputStreamReader(
                            process!!.errorStream,
                            StandardCharsets.UTF_8
                    )
            )
            stdin.write(preamble)
            stdin.flush()
            // Check that the shell started successfully.
            val uid = stdout.readLine()
            if ("0" != uid) {
                Timber.w("Root check did not return correct UID: %s", uid)
                throw NoRootException(deviceNotRootedMessage)
            }
            if (!isRunning()) {
                var line: String?
                while (true) {
                    line = stderr.readLine() ?: break
                    Timber.w("Root check returned an error: %s", line)
                    if (line.contains("Permission denied")) {
                        throw NoRootException(deviceNotRootedMessage)
                    }
                }
                throw IOException(context.getString(R.string.shell_start_error, process!!.exitValue()))
            }
        } catch (e: IOException) {
            stop()
            throw e
        } catch (e: NoRootException) {
            stop()
            throw e
        }
    }

    @Synchronized
    fun stop() {
        process?.let {
            it.destroy()
            process = null
        }
    }

    class NoRootException : Exception {
        internal constructor(message: String, cause: Throwable) : super(message, cause)

        internal constructor(message: String) : super(message)
    }

    companion object {
        private const val SU = "su"
    }
}
