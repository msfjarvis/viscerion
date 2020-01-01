/*
 * Copyright © 2017-2019 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.di

import android.content.Context
import android.content.SharedPreferences
import android.os.AsyncTask
import android.os.Handler
import android.os.Looper
import androidx.preference.PreferenceManager
import com.wireguard.android.activity.BaseActivity
import com.wireguard.android.activity.LaunchActivity
import com.wireguard.android.activity.MainActivity
import com.wireguard.android.activity.SettingsActivity.SettingsFragment
import com.wireguard.android.activity.TunnelToggleActivity
import com.wireguard.android.backend.Backend
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.configStore.ConfigStore
import com.wireguard.android.configStore.FileConfigStore
import com.wireguard.android.di.factory.BackendFactory
import com.wireguard.android.fragment.AppListDialogFragment
import com.wireguard.android.fragment.BaseFragment
import com.wireguard.android.fragment.ConfigNamingDialogFragment
import com.wireguard.android.fragment.TunnelDetailFragment
import com.wireguard.android.fragment.TunnelEditorFragment
import com.wireguard.android.fragment.TunnelListFragment
import com.wireguard.android.model.TunnelManager
import com.wireguard.android.preference.ToolsInstallerPreference
import com.wireguard.android.preference.VersionPreference
import com.wireguard.android.providers.ViscerionSliceProvider
import com.wireguard.android.services.BootShutdownReceiver
import com.wireguard.android.services.QuickTileService
import com.wireguard.android.services.TaskerIntegrationReceiver
import com.wireguard.android.util.ApplicationPreferences
import com.wireguard.android.util.AsyncWorker
import com.wireguard.android.util.BackendAsync
import com.wireguard.android.util.RootShell
import com.wireguard.android.util.ToolsInstaller
import com.wireguard.android.work.TunnelRestoreWorker
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import dagger.Reusable
import java.util.concurrent.Executor
import javax.inject.Singleton

@Singleton
@Component(modules = [ApplicationModule::class])
interface AppComponent {

    @Component.Factory
    interface Factory {
        fun create(@BindsInstance applicationContext: Context): AppComponent
    }

    val backend: Backend
    val backendAsync: BackendAsync
    val asyncWorker: AsyncWorker
    val backendType: Class<Backend>
    val toolsInstaller: ToolsInstaller
    val tunnelManager: TunnelManager
    val rootShell: RootShell
    val preferences: ApplicationPreferences

    // Activities
    fun inject(activity: BaseActivity)
    fun inject(activity: LaunchActivity)
    fun inject(activity: MainActivity)
    fun inject(activity: TunnelToggleActivity)

    // Fragments
    fun inject(fragment: BaseFragment)
    fun inject(fragment: AppListDialogFragment)
    fun inject(fragment: ConfigNamingDialogFragment)
    fun inject(fragment: SettingsFragment)
    fun inject(fragment: TunnelDetailFragment)
    fun inject(fragment: TunnelEditorFragment)
    fun inject(fragment: TunnelListFragment)

    // Preferences
    fun inject(preference: ToolsInstallerPreference)
    fun inject(preference: VersionPreference)

    // ContentProviders
    fun inject(provider: ViscerionSliceProvider)

    // BroadcastReceivers
    fun inject(receiver: BootShutdownReceiver)
    fun inject(receiver: TaskerIntegrationReceiver)
    fun inject(receiver: TunnelManager.IntentReceiver)

    // Services
    fun inject(service: QuickTileService)
    fun inject(service: GoBackend.VpnService)

    // And some other thingies
    fun inject(worker: TunnelRestoreWorker)
}

@Module
object ApplicationModule {
    @get:Reusable
    @get:Provides
    val executor: Executor = AsyncTask.SERIAL_EXECUTOR

    @get:Reusable
    @get:Provides
    val handler: Handler = Handler(Looper.getMainLooper())

    @Reusable
    @Provides
    fun getSharedPrefs(context: Context): SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    @Reusable
    @Provides
    fun getConfigStore(context: Context): ConfigStore = FileConfigStore(context)

    @Reusable
    @Provides
    fun getBackend(
        context: Context,
        preferences: ApplicationPreferences,
        rootShell: RootShell,
        toolsInstaller: ToolsInstaller
    ): Backend {
        return BackendFactory.getBackend(context, preferences, rootShell, toolsInstaller)
    }

    @Reusable
    @Provides
    fun getBackendType(backend: Backend): Class<Backend> = backend.javaClass

    @Singleton
    @Provides
    fun getBackendAsync(backend: Backend): BackendAsync {
        val backendAsync = BackendAsync()
        backendAsync.complete(backend)
        return backendAsync
    }
}
