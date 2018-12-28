/*
 * Copyright © 2018 Harsh Shandilya. All Rights Reserved.
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
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.TextView
import androidx.preference.Preference
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.wireguard.android.activity.SettingsActivity
import com.wireguard.config.Attribute.Companion.LIST_SEPARATOR

fun String.toList(): List<String> {
    if (TextUtils.isEmpty(this))
        return emptyList()
    return LIST_SEPARATOR.split(this.trim()).toList()
}

fun <T> List<T>.asString(): String {
    return TextUtils.join(", ", this)
}

fun Context.restartApplication() {
    val homeIntent = Intent(Intent.ACTION_MAIN)
        .addCategory(Intent.CATEGORY_HOME)
        .setPackage(this.packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val pi = PendingIntent.getActivity(
        this, 42, // The answer to everything
        homeIntent, PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_ONE_SHOT
    )
    (this.getSystemService(Context.ALARM_SERVICE) as AlarmManager)
        .setExact(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 500, pi)
    Handler().postDelayed({ android.os.Process.killProcess(android.os.Process.myPid()) }, 500L)
}

val Preference.parentActivity: SettingsActivity?
    get() {
        return try {
            ((context as ContextThemeWrapper).baseContext as SettingsActivity)
        } catch (ignored: ClassCastException) {
            null
        }
    }

fun Context.resolveAttribute(attr: Int): Int {
    val typedValue = TypedValue()
    this.theme.resolveAttribute(attr, typedValue, true)
    return typedValue.data
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
    Snackbar.make(view, description.toString() + " copied to clipboard", Snackbar.LENGTH_LONG).show()
}
