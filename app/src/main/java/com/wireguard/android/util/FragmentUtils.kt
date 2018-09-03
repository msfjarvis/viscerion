/*
 * Copyright © 2018 Harsh Shandilya <msfjarvis@gmail.com>
 * Copyright © 2018 Jason A. Donenfeld <Jason@zx2c4.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.util

import android.view.ContextThemeWrapper
import androidx.preference.Preference
import com.wireguard.android.activity.SettingsActivity

object FragmentUtils {

    fun getPrefActivity(preference: Preference): SettingsActivity? {
        val context = preference.context
        if (context is ContextThemeWrapper) {
            if (context.baseContext is SettingsActivity) {
                return context.baseContext as SettingsActivity
            }
        }
        return null
    }
}
