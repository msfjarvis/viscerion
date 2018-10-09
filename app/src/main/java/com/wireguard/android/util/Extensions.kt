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
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.View
import androidx.preference.Preference
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.wireguard.android.activity.SettingsActivity
import com.wireguard.config.Attribute

fun <T> ArrayList<T>.addExclusive(otherArray: ArrayList<T>): ArrayList<T> {
    otherArray.forEach {
        if (it !in this)
            this.add(it)
    }
    return this
}

fun <T> ArrayList<T>.addExclusive(otherArray: Array<T>): ArrayList<T> {
    otherArray.forEach {
        if (it !in this)
            this.add(it)
    }
    return this
}

fun String?.addExclusive(otherArray: ArrayList<String>): String {
    val stringCopy = Attribute.stringToList(this).toCollection(ArrayList())
    otherArray.forEach {
        if (it !in stringCopy)
            stringCopy.add(it)
    }
    return Attribute.iterableToString(stringCopy)
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

fun Preference.getPrefActivity(): SettingsActivity? {
    val context = this.context
    if (context is ContextThemeWrapper) {
        if (context.baseContext is SettingsActivity) {
            return context.baseContext as SettingsActivity
        }
    }
    return null
}

fun Context.resolveAttribute(attr: Int): Int {
    val typedValue = TypedValue()
    this.theme.resolveAttribute(attr, typedValue, true)
    return typedValue.data
}

fun copyTextView(view: View) {
    if (view !is TextInputEditText)
        return
    val text = view.editableText
    if (text == null || text.isEmpty())
        return
    val service = view.getContext().getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    val description = view.hint
    service.primaryClip = ClipData.newPlainText(description, text)
    Snackbar.make(view, description.toString() + " copied to clipboard", Snackbar.LENGTH_LONG).show()
}
