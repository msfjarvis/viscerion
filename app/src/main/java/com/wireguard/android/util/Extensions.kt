/*
 * Copyright © 2017-2020 WireGuard LLC.
 * Copyright © 2018-2020 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.SystemClock
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
import androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode
import androidx.core.app.AlarmManagerCompat.setExact
import androidx.core.content.getSystemService
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.wireguard.android.backend.Backend
import java.io.BufferedReader
import java.io.InputStreamReader
import java9.util.concurrent.CompletableFuture

typealias BackendAsync = CompletableFuture<Backend>

fun String.runShellCommand(): ArrayList<String> {
    val ret = ArrayList<String>()
    try {
        val shell = Runtime.getRuntime().exec(this)
        val reader = BufferedReader(InputStreamReader(shell.inputStream))
        var line: String?
        while (true) {
            line = reader.readLine() ?: break
            ret.add(line)
        }
    } catch (_: Exception) {
    }
    return ret
}

fun Context.restartApplication() {
    val homeIntent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_HOME)
            .setPackage(packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val pi = PendingIntent.getActivity(
            this, 42, // The answer to everything
            homeIntent, PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_ONE_SHOT
    )
    getSystemService<AlarmManager>()?.let {
        setExact(it, AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 500, pi)
        Handler().postDelayed({ android.os.Process.killProcess(android.os.Process.myPid()) }, 500L)
    }
}

fun Context.isSystemDarkThemeEnabled(): Boolean {
    return resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
}

fun Context.resolveAttribute(@AttrRes attrRes: Int): Int {
    val typedValue = TypedValue()
    theme.resolveAttribute(attrRes, typedValue, true)
    return typedValue.data
}

val Uri.humanReadablePath: String
    get() {
        path?.apply {
            if (startsWith("/document/primary")) {
                return replace("/document/primary:", "/sdcard/")
            } else if (startsWith("/document/")) {
                return replace("/document/", "/storage/").replace(":", "/")
            }
        }
        return requireNotNull(path)
    }

fun updateAppTheme(dark: Boolean) {
    setDefaultNightMode(
            if (dark) {
                MODE_NIGHT_YES
            } else {
                if (Build.VERSION.SDK_INT >= 29) {
                    MODE_NIGHT_FOLLOW_SYSTEM
                } else {
                    MODE_NIGHT_AUTO_BATTERY
                }
            }
    )
}

fun copyTextView(view: View) {
    val data = when (view) {
        is TextInputEditText -> Pair(view.editableText, view.hint)
        is TextView -> Pair(view.text, view.contentDescription)
        else -> return
    }
    if (data.first == null || data.first.isEmpty()) {
        return
    }
    val service = view.context.getSystemService<ClipboardManager>() ?: return
    service.setPrimaryClip(ClipData.newPlainText(data.second, data.first))
    Snackbar.make(view, "${data.second} copied to clipboard", Snackbar.LENGTH_LONG).show()
}
