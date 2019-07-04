/*
 * Copyright © 2017-2019 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android

import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import com.wireguard.android.di.backendAsyncModule
import com.wireguard.android.di.backendModule
import com.wireguard.android.di.configStoreModule
import com.wireguard.android.di.earlyInitModules
import com.wireguard.android.di.ext.injectPrefs
import com.wireguard.android.di.toolsInstallerModule
import com.wireguard.android.model.TunnelManager
import com.wireguard.android.services.TaskerIntegrationService
import com.wireguard.android.util.updateAppTheme
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import timber.log.Timber
import java.lang.ref.WeakReference

class Application : android.app.Application() {

    val prefs by injectPrefs()

    init {
        weakSelf = WeakReference(this)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val notificationManager = requireNotNull(getSystemService<NotificationManager>())
        val notificationChannel = NotificationChannel(
                TunnelManager.NOTIFICATION_CHANNEL_ID,
                getString(R.string.notification_channel_wgquick_title),
                NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationChannel.description = getString(R.string.notification_channel_wgquick_desc)
        notificationChannel.setShowBadge(false)
        notificationChannel.setSound(null, null)
        notificationChannel.enableVibration(false)
        notificationManager.createNotificationChannel(notificationChannel)
    }

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@Application)
            modules(listOf(
                    configStoreModule,
                    earlyInitModules,
                    backendModule,
                    backendAsyncModule,
                    toolsInstallerModule
            ))
        }

        if (BuildConfig.DEBUG)
            Timber.plant(Timber.DebugTree())

        updateAppTheme(prefs.useDarkTheme)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            createNotificationChannel()

        if (prefs.allowTaskerIntegration && canStartService()) {
            startService(Intent(this, TaskerIntegrationService::class.java))
        }
    }

    private fun canStartService(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
        val appProcessInfo = ActivityManager.RunningAppProcessInfo()
        ActivityManager.getMyMemoryState(appProcessInfo)
        return (appProcessInfo.importance == IMPORTANCE_FOREGROUND || appProcessInfo.importance == IMPORTANCE_VISIBLE)
    }

    companion object {
        private lateinit var weakSelf: WeakReference<Application>

        fun get(): Application {
            return requireNotNull(weakSelf.get())
        }
    }
}
