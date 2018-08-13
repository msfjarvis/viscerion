/*
 * Copyright © 2018 Jason A. Donenfeld <Jason@zx2c4.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.wireguard.android

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import com.wireguard.android.backend.Backend
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.WgQuickBackend
import com.wireguard.android.configStore.FileConfigStore
import com.wireguard.android.model.TunnelManager
import com.wireguard.android.util.AsyncWorker
import com.wireguard.android.util.RootShell
import com.wireguard.android.util.ToolsInstaller
import java9.util.concurrent.CompletableFuture
import org.acra.ACRA
import org.acra.config.CoreConfigurationBuilder
import org.acra.config.HttpSenderConfigurationBuilder
import org.acra.data.StringFormat
import org.acra.sender.HttpSender
import java.io.ByteArrayInputStream
import java.io.File
import java.lang.ref.WeakReference
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

class Application : android.app.Application() {
    private var asyncWorker: AsyncWorker? = null
    private var rootShell: RootShell? = null
    private val sharedPreferences: SharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(this) }
    private var toolsInstaller: ToolsInstaller? = null
    private lateinit var tunnelManager: TunnelManager
    private var backend: Backend? = null
    private val futureBackend = CompletableFuture<Backend>()

    init {
        Application.weakSelf = WeakReference(this)
    }

    override fun attachBaseContext(context: Context) {
        super.attachBaseContext(context)

        if (BuildConfig.MIN_SDK_VERSION > Build.VERSION.SDK_INT) {
            val intent = Intent(Intent.ACTION_MAIN)
            intent.addCategory(Intent.CATEGORY_HOME)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            System.exit(0)
        }

        val installSource = getInstallSource(context)
        if (installSource != null) {
            ACRA.init(this)
            ACRA.getErrorReporter().putCustomData("installSource", installSource)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationChannel = NotificationChannel(TunnelManager.NOTIFICATION_CHANNEL_ID,
                getString(R.string.notification_channel_wgquick_title),
                NotificationManager.IMPORTANCE_DEFAULT)
        notificationChannel.description = getString(R.string.notification_channel_wgquick_desc)
        notificationManager.createNotificationChannel(notificationChannel)
    }

    override fun onCreate() {
        super.onCreate()

        asyncWorker = AsyncWorker(AsyncTask.SERIAL_EXECUTOR, Handler(Looper.getMainLooper()))
        rootShell = RootShell(applicationContext)
        toolsInstaller = ToolsInstaller(applicationContext)

        AppCompatDelegate.setDefaultNightMode(
                if (sharedPreferences.getBoolean("dark_theme", true))
                    AppCompatDelegate.MODE_NIGHT_YES
                else
                    AppCompatDelegate.MODE_NIGHT_NO)

        tunnelManager = TunnelManager(FileConfigStore(applicationContext))
        tunnelManager.onCreate()

        if (sharedPreferences.getBoolean("enable_logging", true)) {
            val configurationBuilder = CoreConfigurationBuilder(this)

            // Core configuration
            configurationBuilder.setReportFormat(StringFormat.JSON)
                    .setBuildConfigClass(BuildConfig::class.java)
                    .setLogcatArguments("-b", "all", "-d", "-v", "threadtime", "*:V")
                    .setExcludeMatchingSettingsKeys("last_used_tunnel", "enabled_configs")

            // HTTP Sender configuration
            configurationBuilder.getPluginConfigurationBuilder(HttpSenderConfigurationBuilder::class.java)
                    .setUri("https://crashreport.zx2c4.com/android/report")
                    .setBasicAuthLogin("6RCovLxEVCTXGiW5")
                    .setBasicAuthPassword("O7I3sVa5ULVdiC51")
                    .setHttpMethod(HttpSender.Method.POST)
                    .setCompress(true)
            ACRA.init(this, configurationBuilder)

            asyncWorker!!.supplyAsync<Backend> { getBackend() }.thenAccept { backend ->
                futureBackend.complete(backend)
                if (ACRA.isInitialised()) {
                    ACRA.getErrorReporter().putCustomData("backend", backend.javaClass.simpleName)
                    asyncWorker!!.supplyAsync<String> { backend.version }
                            .thenAccept { version -> ACRA.getErrorReporter().putCustomData("backendVersion", version) }
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            createNotificationChannel()

    }

    companion object {

        private lateinit var weakSelf: WeakReference<Application>

        /* The ACRA password can be trivially reverse engineered and is open source anyway,
         * so there's no point in trying to protect it. However, we do want to at least
         * prevent innocent self-builders from uploading stuff to our crash reporter. So, we
         * check the DN of the certs that signed the apk, without even bothering to try
         * validating that they're authentic. It's a good enough heuristic.
         */
        private fun getInstallSource(context: Context): String? {
            if (BuildConfig.DEBUG)
                return null
            try {
                val cf = CertificateFactory.getInstance("X509")
                for (sig in context.packageManager
                        .getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES).signatures) {
                    try {
                        for (category in (cf.generateCertificate(ByteArrayInputStream(sig.toByteArray())) as X509Certificate)
                                .subjectDN.name.split(", *".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
                            val parts = category.split("=".toRegex(), 2).toTypedArray()
                            if ("O" != parts[0])
                                continue
                            when (parts[1]) {
                                "Google Inc." -> return "Play Store"
                                "fdroid.org" -> return "F-Droid"
                            }
                        }
                    } catch (ignored: Exception) {
                    }

                }
            } catch (ignored: Exception) {
            }
            return null
        }

        fun get(): Application {
            return weakSelf.get() as Application
        }

        fun getAsyncWorker(): AsyncWorker? {
            return get().asyncWorker
        }

        fun getBackend(): Backend {
            val app = get()
            synchronized(app.futureBackend) {
                if (app.backend == null) {
                    var backend: Backend? = null
                    if (File("/sys/module/wireguard").exists()) {
                        try {
                            app.rootShell!!.start()
                            backend = WgQuickBackend(app.applicationContext)
                        } catch (ignored: Exception) {
                        }

                    }
                    if (backend == null)
                        backend = GoBackend(app.applicationContext)
                    app.backend = backend
                }
                return app.backend as Backend
            }
        }

        val backendAsync: CompletableFuture<Backend>
            get() = get().futureBackend

        fun getRootShell(): RootShell? {
            return get().rootShell
        }

        fun getSharedPreferences(): SharedPreferences {
            return get().sharedPreferences
        }

        fun getToolsInstaller(): ToolsInstaller? {
            return get().toolsInstaller
        }

        fun getTunnelManager(): TunnelManager {
            return get().tunnelManager
        }
    }
}
