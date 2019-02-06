/*
 * Copyright © 2019 Harsh Shandilya. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.util

import androidx.core.content.edit
import com.wireguard.android.Application

class ApplicationPreferences {
    companion object {
        const val appThemeKey = "dark_theme"
        const val globalExclusionsKey = "global_exclusions"
        const val forceUserspaceBackendkey = "force_userspace_backend"
        const val whitelistAppsKey = "whitelist_exclusions"
        var exclusions: String
            get() {
                return Application.sharedPreferences.getString(globalExclusionsKey, "") as String
            }
            set(value) {
                Application.sharedPreferences.edit {
                    putString(globalExclusionsKey, value)
                }
                exclusionsArray = value.toArrayList().toCollection(ArrayList())
            }
        var exclusionsArray: ArrayList<String> = exclusions.toArrayList()
            private set

        val useDarkTheme: Boolean
            get() = Application.sharedPreferences.getBoolean(appThemeKey, false)

        val forceUserspaceBackend: Boolean
            get() = Application.sharedPreferences.getBoolean(forceUserspaceBackendkey, false)

        val whitelistApps: Boolean
            get() = Application.sharedPreferences.getBoolean(whitelistAppsKey, false)
    }
}
