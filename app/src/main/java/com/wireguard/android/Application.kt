/*
 * Copyright © 2019 Harsh Shandilya. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.os.AsyncTask
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.wireguard.android.backend.Backend
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.WgQuickBackend
import com.wireguard.android.configStore.FileConfigStore
import com.wireguard.android.model.TunnelManager
import com.wireguard.android.util.ApplicationPreferences
import com.wireguard.android.util.AsyncWorker
import com.wireguard.android.util.RootShell
import com.wireguard.android.util.ToolsInstaller
import java9.util.concurrent.CompletableFuture
import timber.log.Timber
import java.io.File
import java.lang.ref.WeakReference

class Application : android.app.Application() {
    private lateinit var asyncWorker: AsyncWorker
    private lateinit var rootShell: RootShell
    private val sharedPreferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(this)
    }
    private lateinit var toolsInstaller: ToolsInstaller
    private lateinit var tunnelManager: TunnelManager
    private var backend: Backend? = null
    private val futureBackend = CompletableFuture<Backend>()

    init {
        Application.weakSelf = WeakReference(this)
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

        if (BuildConfig.DEBUG)
            Timber.plant(Timber.DebugTree())

        asyncWorker = AsyncWorker(AsyncTask.SERIAL_EXECUTOR, Handler(Looper.getMainLooper()))
        rootShell = RootShell(applicationContext)
        toolsInstaller = ToolsInstaller(applicationContext)

        AppCompatDelegate.setDefaultNightMode(
            if (ApplicationPreferences.useDarkTheme)
                AppCompatDelegate.MODE_NIGHT_YES
            else
                AppCompatDelegate.MODE_NIGHT_NO
        )

        tunnelManager = TunnelManager(FileConfigStore(applicationContext))
        tunnelManager.onCreate()

        asyncWorker.supplyAsync { backend }.thenAccept { backend ->
            futureBackend.complete(backend)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            createNotificationChannel()
    }

    companion object {

        private lateinit var weakSelf: WeakReference<Application>
        val asyncWorker by lazy { get().asyncWorker }
        val backendAsync by lazy { get().futureBackend }
        val rootShell by lazy { get().rootShell }
        val sharedPreferences by lazy { get().sharedPreferences }
        val toolsInstaller by lazy { get().toolsInstaller }
        val tunnelManager by lazy { get().tunnelManager }
        var supportsKernelModule = false

        fun get(): Application {
            return weakSelf.get() as Application
        }

        val backend: Backend
            get() {
                val app = get()
                synchronized(app.futureBackend) {
                    if (app.backend == null) {
                        var backend: Backend? = null
                        if (File("/sys/module/wireguard").exists()) {
                            supportsKernelModule = true
                            try {
                                if (ApplicationPreferences.forceUserspaceBackend)
                                    throw Exception("Forcing userspace backend on user request.")
                                app.rootShell.start()
                                backend = WgQuickBackend(app.applicationContext)
                            } catch (ignored: Exception) {
                            }
                        }
                        if (backend == null)
                            backend = GoBackend(app.applicationContext)
                        app.backend = backend
                    }
                }
                return app.backend as Backend
            }
    }
}
