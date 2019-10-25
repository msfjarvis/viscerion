/*
 * Copyright © 2017-2019 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.StrictMode
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import com.wireguard.android.di.backendAsyncModule
import com.wireguard.android.di.backendModule
import com.wireguard.android.di.configStoreModule
import com.wireguard.android.di.earlyInitModules
import com.wireguard.android.di.ext.getPrefs
import com.wireguard.android.di.toolsInstallerModule
import com.wireguard.android.model.TunnelManager
import com.wireguard.android.util.updateAppTheme
import java.lang.ref.WeakReference
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import timber.log.Timber

class Application : android.app.Application() {

    init {
        weakSelf = WeakReference(this)
    }

    @RequiresApi(26)
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

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )
        }

        updateAppTheme(getPrefs().useDarkTheme)

        if (Build.VERSION.SDK_INT >= 26)
            createNotificationChannel()
    }

    companion object {
        private lateinit var weakSelf: WeakReference<Application>

        fun get(): Application {
            return requireNotNull(weakSelf.get())
        }
    }
}
