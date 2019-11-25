/*
 * Copyright © 2017-2019 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
@file:Suppress("Unused")

package com.wireguard.android.di.ext

import android.content.ComponentCallbacks
import android.content.Context
import com.wireguard.android.backend.Backend
import com.wireguard.android.configStore.ConfigStore
import com.wireguard.android.model.TunnelManager
import com.wireguard.android.util.ApplicationPreferences
import com.wireguard.android.util.AsyncWorker
import com.wireguard.android.util.BackendAsync
import com.wireguard.android.util.RootShell
import com.wireguard.android.util.ToolsInstaller
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import org.koin.core.KoinComponent
import org.koin.core.get
import org.koin.core.inject

fun ComponentCallbacks.injectAsyncWorker() = inject<AsyncWorker>()
fun ComponentCallbacks.injectBackend() = inject<Backend>()
fun ComponentCallbacks.injectBackendAsync() = inject<BackendAsync>()
fun ComponentCallbacks.injectContext() = inject<Context>()
fun ComponentCallbacks.injectConfigStore() = inject<ConfigStore>()
fun ComponentCallbacks.injectPrefs() = inject<ApplicationPreferences>()
fun ComponentCallbacks.injectRootShell() = inject<RootShell>()
fun ComponentCallbacks.injectToolsInstaller() = inject<ToolsInstaller>()
fun ComponentCallbacks.injectTunnelManager() = inject<TunnelManager>()

fun KoinComponent.injectAsyncWorker() = inject<AsyncWorker>()
fun KoinComponent.injectBackend() = inject<Backend>()
fun KoinComponent.injectBackendAsync() = inject<BackendAsync>()
fun KoinComponent.injectConfigStore() = inject<ConfigStore>()
fun KoinComponent.injectContext() = inject<Context>()
fun KoinComponent.injectPrefs() = inject<ApplicationPreferences>()
fun KoinComponent.injectRootShell() = inject<RootShell>()
fun KoinComponent.injectToolsInstaller() = inject<ToolsInstaller>()
fun KoinComponent.injectTunnelManager() = inject<TunnelManager>()

fun ComponentCallbacks.getAsyncWorker() = get<AsyncWorker>()
fun ComponentCallbacks.getBackend() = get<Backend>()
fun ComponentCallbacks.getBackendAsync() = get<BackendAsync>()
fun ComponentCallbacks.getConfigStore() = get<ConfigStore>()
fun ComponentCallbacks.getContext() = get<Context>()
fun ComponentCallbacks.getPrefs() = get<ApplicationPreferences>()
fun ComponentCallbacks.getRootShell() = get<RootShell>()
fun ComponentCallbacks.getToolsInstaller() = get<ToolsInstaller>()
fun ComponentCallbacks.getTunnelManager() = get<TunnelManager>()

fun KoinComponent.getAsyncWorker() = get<AsyncWorker>()
fun KoinComponent.getBackend() = get<Backend>()
fun KoinComponent.getBackendAsync() = get<BackendAsync>()
fun KoinComponent.getConfigStore() = get<ConfigStore>()
fun KoinComponent.getContext() = get<Context>()
fun KoinComponent.getRootShell() = get<RootShell>()
fun KoinComponent.getPrefs() = get<ApplicationPreferences>()
fun KoinComponent.getToolsInstaller() = get<ToolsInstaller>()
fun KoinComponent.getTunnelManager() = get<TunnelManager>()
