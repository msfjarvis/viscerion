package com.wireguard.android.util

import com.wireguard.android.Application
import com.wireguard.config.Attribute

class ApplicationPreferences {
    companion object {
        const val appThemeKey = "app_theme"
        private const val globalExclusionsKey = "global_exclusions"
        var exclusions: String
            get() {
                return Application.sharedPreferences.getString(globalExclusionsKey, "") as String
            }
            set(value) {
                Application.sharedPreferences.edit().putString(globalExclusionsKey, value).apply()
                exclusionsArray = Attribute.stringToList(value).toCollection(ArrayList())
            }
        var exclusionsArray: ArrayList<String> = Attribute.stringToList(exclusions).toCollection(ArrayList())

        var theme: String
            get() {
                return Application.sharedPreferences.getString(appThemeKey, "dark") as String
            }
            set(value) {
                Application.sharedPreferences.edit().putString(appThemeKey, value).apply()
            }
    }
}