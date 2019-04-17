/*
 * Copyright © 2017-2018 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.wireguard.android.di.backendAsyncModule
import com.wireguard.android.di.backendModule
import com.wireguard.android.di.earlyInitModules
import com.wireguard.android.di.toolsInstallerModule
import com.wireguard.android.model.TunnelManager
import com.wireguard.android.util.ApplicationPreferences
import com.wireguard.android.util.updateAppTheme
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import timber.log.Timber
import java.lang.ref.WeakReference

class Application : android.app.Application() {

    init {
        weakSelf = WeakReference(this)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
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
            androidLogger(level = Level.DEBUG)
            androidContext(this@Application)
            modules(listOf(earlyInitModules, backendModule, backendAsyncModule, toolsInstallerModule))
        }

        if (BuildConfig.DEBUG)
            Timber.plant(Timber.DebugTree())

        updateAppTheme(inject<ApplicationPreferences>().value)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            createNotificationChannel()
    }

    companion object {
        private lateinit var weakSelf: WeakReference<Application>

        fun get(): Application {
            return weakSelf.get() as Application
        }
    }
}
