package com.wireguard.android.model

import com.wireguard.android.Application

class GlobalExclusions {
    companion object {
        var exclusions: String = Application.getSharedPreferences().getString("global_exclusions", "") as String
        set(value) {
            Application.getSharedPreferences().edit().putString("global_exclusions", value).apply()
            field = value
        }
    }
}