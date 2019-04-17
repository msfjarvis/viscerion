/*
 * Copyright © 2017-2018 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.di

import android.os.AsyncTask
import android.os.Handler
import android.os.Looper
import com.wireguard.android.configStore.ConfigStore
import com.wireguard.android.configStore.FileConfigStore
import com.wireguard.android.di.factory.BackendFactory
import com.wireguard.android.di.factory.CompletableBackendFactory
import com.wireguard.android.model.TunnelManager
import com.wireguard.android.util.ApplicationPreferences
import com.wireguard.android.util.AsyncWorker
import com.wireguard.android.util.RootShell
import com.wireguard.android.util.ToolsInstaller
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val earlyInitModules = module {
    single { AsyncWorker(AsyncTask.SERIAL_EXECUTOR, Handler(Looper.getMainLooper())) }
    single { RootShell(androidContext()) }
    single { ApplicationPreferences(androidContext()) }
    single<ConfigStore> { FileConfigStore(androidContext()) }
    single { TunnelManager(androidContext()) }
}

val backendModule = module {
    single { BackendFactory(androidContext()).backend }
}

val backendAsyncModule = module {
    single { CompletableBackendFactory(androidContext()).backendAsync }
}

val toolsInstallerModule = module {
    single { ToolsInstaller(androidContext()) }
}
