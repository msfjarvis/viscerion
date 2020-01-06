/*
 * Copyright © 2017-2020 WireGuard LLC.
 * Copyright © 2018-2020 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.StrictMode
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import com.wireguard.android.di.AppComponent
import com.wireguard.android.di.DaggerAppComponent
import com.wireguard.android.di.InjectorProvider
import com.wireguard.android.model.TunnelManager
import com.wireguard.android.util.updateAppTheme
import timber.log.Timber

@Suppress("Unused")
class Application : android.app.Application(), InjectorProvider {

    override val component: AppComponent by lazy {
        DaggerAppComponent.factory().create(applicationContext)
    }

    @RequiresApi(26)
    private fun createNotificationChannel() {
        val notificationManager = requireNotNull(getSystemService<NotificationManager>())
        val notificationChannel = NotificationChannel(
                TunnelManager.NOTIFICATION_CHANNEL_ID,
                getString(R.string.notification_channel_wgquick_title),
                NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = getString(R.string.notification_channel_wgquick_desc)
            setShowBadge(false)
            setSound(null, null)
            enableVibration(false)
        }
        notificationManager.createNotificationChannel(notificationChannel)
    }

    override fun onCreate() {
        super.onCreate()
        INSTANCE = this

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )
        }

        updateAppTheme(component.preferences.useDarkTheme)

        if (Build.VERSION.SDK_INT >= 26) {
            createNotificationChannel()
        }
    }

    companion object {
        lateinit var INSTANCE: Context
    }
}
