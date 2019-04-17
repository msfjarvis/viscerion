/*
 * Copyright © 2017-2018 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.util

import android.os.Environment
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.wireguard.android.R
import org.koin.core.KoinComponent
import org.koin.core.inject
import timber.log.Timber
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader

object LogExporter : KoinComponent {

    fun exportLog(activity: AppCompatActivity) {
        inject<AsyncWorker>().value.supplyAsync {
            val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(path, "viscerion-log.txt")
            if (!path.isDirectory && !path.mkdirs())
                throw IOException("Cannot create output directory")

            /* We would like to simply run `builder.redirectOutput(file);`, but this is API 26.
             * Instead we have to do this dance, since logcat appends.
             */
            FileOutputStream(file).close()
            try {
                val process = Runtime.getRuntime()
                        .exec(arrayOf("logcat", "-b", "all", "-d", "-v", "threadtime", "-f", file.absolutePath, "*:V"))
                if (process.waitFor() != 0) {
                    BufferedReader(InputStreamReader(process.errorStream)).use { reader ->
                        val errors = StringBuilder()
                        errors.append("Unable to run logcat: ")
                        reader.readLines().forEach { if (it.isNotEmpty()) errors.append(it) }
                        throw Exception(errors.toString())
                    }
                }
            } catch (e: Exception) {
                file.delete()
                throw e
            }
            file.absolutePath
        }.whenComplete { filePath: String, throwable: Throwable? ->
            val message: String
            if (throwable != null) {
                val error = ExceptionLoggers.unwrapMessage(throwable)
                message = activity.getString(R.string.log_export_error, error)
                Timber.e(throwable)
            } else {
                message = activity.getString(R.string.log_export_success, filePath)
            }
            activity.findViewById<View>(android.R.id.content)?.let {
                Snackbar.make(it, message, Snackbar.LENGTH_LONG).show()
            }
        }
    }
}
