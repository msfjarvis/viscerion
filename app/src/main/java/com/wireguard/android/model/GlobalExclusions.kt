package com.wireguard.android.model

import com.wireguard.android.Application
import com.wireguard.config.Attribute

class GlobalExclusions {
    companion object {
        var exclusions: String = Application.sharedPreferences.getString("global_exclusions", "") as String
            set(value) {
                Application.sharedPreferences.edit().putString("global_exclusions", value).apply()
                exclusionsArray = Attribute.stringToList(value).toCollection(ArrayList())
                field = value
            }
        var exclusionsArray: ArrayList<String> = Attribute.stringToList(exclusions).toCollection(ArrayList())
    }
}