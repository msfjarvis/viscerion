/*
 * Copyright © 2018 Jason A. Donenfeld <Jason@zx2c4.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.wireguard.android.activity

import android.content.SharedPreferences
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.wireguard.android.Application
import java.lang.reflect.Field

abstract class ThemeChangeAwareActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Application.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onDestroy() {
        Application.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        super.onDestroy()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if ("dark_theme" == key) {
            val darkMode = sharedPreferences.getBoolean(key, false)
            AppCompatDelegate.setDefaultNightMode(
                if (sharedPreferences.getBoolean(key, false))
                    AppCompatDelegate.MODE_NIGHT_YES
                else
                    AppCompatDelegate.MODE_NIGHT_NO
            )
            invalidateDrawableCache(resources, darkMode)
            recreate()
        }
    }

    companion object {
        private val TAG = "WireGuard/" + ThemeChangeAwareActivity::class.java.simpleName

        @Nullable
        private var lastResources: Resources? = null
        private var lastDarkMode: Boolean = false
        @Synchronized
        private fun invalidateDrawableCache(resources: Resources, darkMode: Boolean) {
            if (resources === lastResources && darkMode == lastDarkMode)
                return

            try {
                var f: Field
                var o: Any = resources
                try {
                    f = o.javaClass.getDeclaredField("mResourcesImpl")
                    f.isAccessible = true
                    o = f.get(o)
                } catch (ignored: Exception) {
                }

                f = o.javaClass.getDeclaredField("mDrawableCache")
                f.isAccessible = true
                o = f.get(o)
                try {
                    o.javaClass.getMethod("onConfigurationChange", Int::class.javaPrimitiveType).invoke(o, -1)
                } catch (ignored: Exception) {
                    o.javaClass.getMethod("clear").invoke(o)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to flush drawable cache", e)
            }

            lastResources = resources
            lastDarkMode = darkMode
        }
    }
}
