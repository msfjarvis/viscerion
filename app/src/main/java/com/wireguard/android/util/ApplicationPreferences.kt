/*
 * Copyright © 2018 Harsh Shandilya. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.wireguard.android.util

import androidx.core.content.edit
import com.wireguard.android.Application
import com.wireguard.config.Attribute

class ApplicationPreferences {
    companion object {
        const val appThemeKey = "app_theme"
        const val globalExclusionsKey = "global_exclusions"
        const val forceUserspaceBackendkey = "force_userspace_backend"
        const val appThemeDarkValue = "dark"
        const val appThemeBlackValue = "amoled"
        val isDarkTheme = theme in arrayOf(appThemeBlackValue, appThemeDarkValue)
        var exclusions: String
            get() {
                return Application.sharedPreferences.getString(globalExclusionsKey, "") as String
            }
            set(value) {
                Application.sharedPreferences.edit {
                    putString(globalExclusionsKey, value)
                }
                exclusionsArray = Attribute.stringToList(value).toCollection(ArrayList())
            }
        var exclusionsArray: ArrayList<String> = Attribute.stringToList(exclusions).toCollection(ArrayList())

        var theme: String
            get() {
                return Application.sharedPreferences.getString(appThemeKey, appThemeDarkValue) as String
            }
            set(value) {
                Application.sharedPreferences.edit {
                    putString(appThemeKey, value)
                }
            }
        var forceUserspaceBackend: Boolean
            get() {
                return Application.sharedPreferences.getBoolean(forceUserspaceBackendkey, false)
            }
            set(value) {
                Application.sharedPreferences.edit {
                    putBoolean(forceUserspaceBackendkey, value)
                }
            }
    }
}
