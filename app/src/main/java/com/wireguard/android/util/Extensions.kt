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
import android.os.Handler
import android.os.SystemClock
import android.text.TextUtils
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.Preference
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.wireguard.android.Application
import com.wireguard.config.Attribute.Companion.LIST_SEPARATOR

fun String.toArrayList(): ArrayList<String> {
    return if (TextUtils.isEmpty(this))
        ArrayList()
    else
        LIST_SEPARATOR.split(trim()).toCollection(ArrayList())
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
    (getSystemService(Context.ALARM_SERVICE) as AlarmManager)
        .setExact(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 500, pi)
    Handler().postDelayed({ android.os.Process.killProcess(android.os.Process.myPid()) }, 500L)
}

inline fun <reified T : AppCompatActivity> Preference.getParentActivity(): T? {
    return try {
        context as T
    } catch (ignored: ClassCastException) {
        null
    }
}

fun Context.updateAppTheme() {
    AppCompatDelegate.setDefaultNightMode(
            if (Application.appPrefs.useDarkTheme)
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
    val service = view.context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    val description = if (isTextInput) (view as TextInputEditText).hint else view.contentDescription
    service.primaryClip = ClipData.newPlainText(description, text)
    Snackbar.make(view, "$description copied to clipboard", Snackbar.LENGTH_LONG).show()
}
