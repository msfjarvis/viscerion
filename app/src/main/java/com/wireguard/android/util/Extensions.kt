/*
 * Copyright © 2017-2018 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.SystemClock
import android.text.TextUtils
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.AlarmManagerCompat.setExact
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.preference.Preference
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.wireguard.android.backend.Backend
import com.wireguard.config.Attribute.Companion.LIST_SEPARATOR
import java9.util.concurrent.CompletableFuture
import java.io.BufferedReader
import java.io.InputStreamReader

typealias BackendAsync = CompletableFuture<Backend>

fun String.toArrayList(): ArrayList<String> {
    return if (TextUtils.isEmpty(this))
        ArrayList()
    else
        LIST_SEPARATOR.split(trim()).toCollection(ArrayList())
}

fun String.runShellCommand(): ArrayList<String> {
    val ret = ArrayList<String>()
    try {
        val shell = Runtime.getRuntime().exec(this)
        val reader = BufferedReader(InputStreamReader(shell.inputStream))
        var line: String?
        while (true) {
            line = reader.readLine()
            if (line == null)
                break
            ret.add(line)
        }
    } catch (ignored: Exception) {
    }
    return ret
}

fun <T> List<T>.asString(): String {
    return TextUtils.join(", ", this)
}

inline fun <reified T : Any> Any?.requireNonNull(message: String): T {
    if (this == null)
        throw NullPointerException(message)
    if (this !is T)
        throw IllegalArgumentException(message)
    return this
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

fun Context.isPermissionGranted(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}

inline fun <reified T : AppCompatActivity> Preference.getParentActivity(): T? {
    return try {
        context as T
    } catch (ignored: ClassCastException) {
        null
    }
}

fun updateAppTheme(prefs: ApplicationPreferences) {
    AppCompatDelegate.setDefaultNightMode(
            if (prefs.useDarkTheme)
                AppCompatDelegate.MODE_NIGHT_YES
            else
                AppCompatDelegate.MODE_NIGHT_NO
    )
}

fun copyTextView(view: View) {
    var isTextInput = false
    if (view is TextInputEditText)
        isTextInput = true
    else if (view !is TextView)
        return
    val text = if (isTextInput) (view as TextInputEditText).editableText else (view as TextView).text
    if (text == null || text.isEmpty())
        return
    val service = view.context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return
    val description = if (isTextInput) (view as TextInputEditText).hint else view.contentDescription
    service.setPrimaryClip(ClipData.newPlainText(description, text))
    Snackbar.make(view, "$description copied to clipboard", Snackbar.LENGTH_LONG).show()
}
