package com.wireguard.android.util

import android.content.Context
import com.wireguard.android.BuildConfig
import com.wireguard.android.R
import eu.darken.rxshell.cmd.Cmd
import eu.darken.rxshell.cmd.RxCmdShell
import eu.darken.rxshell.root.Root
import timber.log.Timber
import java.io.File
import java.io.IOException

class RootShell(private var context: Context) {
    private val deviceNotRootedMessage by lazy { context.getString(R.string.error_root) }
    private val cacheDir by lazy { context.cacheDir }
    private val localBinaryDir by lazy { File(cacheDir, "bin") }
    private val localTemporaryDir by lazy { File(cacheDir, "tmp") }
    private var builder: RxCmdShell

    init {
        builder = RxCmdShell.builder()
            .root(true)
            .shellEnvironment("LC_ALL", "C")
            .shellEnvironment("CALLING_PACKAGE", BuildConfig.APPLICATION_ID)
            .shellEnvironment("PATH", "$localBinaryDir:\$PATH")
            .shellEnvironment("TMPDIR", localTemporaryDir.absolutePath)
            .build()
        Timber.tag(TAG)
    }

    private fun isRootAvailable(): Boolean {
        val root = Root.Builder().build().blockingGet()
        return root.state == Root.State.ROOTED
    }

    fun start() {
        if (!isRootAvailable())
            throw IOException(deviceNotRootedMessage)
    }

    @Throws(IOException::class, NoRootException::class)
    fun run(output: ArrayList<String>? = null, command: String): Int {
        var returnCode = 0
        if (!isRootAvailable())
            throw NoRootException(deviceNotRootedMessage)
        Cmd.builder(command).submit(builder).subscribe { result ->
            output?.addAll(result.output)
            output?.addAll(result.errors)
            Timber.d("executing: %s", result.cmd)
            Timber.d("stdout: %s", result.output)
            Timber.d("stderr: %s", result.errors)
            Timber.d("exit: %s", result.exitCode)
            returnCode = result.exitCode
        }.dispose()
        return returnCode
    }

    class NoRootException internal constructor(message: String) : Exception(message)

    companion object {
        private val TAG = "WireGuard" + RootShell::class.java.simpleName
    }
}