/*
 * Copyright © 2017-2018 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.services

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.wireguard.android.BuildConfig
import com.wireguard.android.di.ext.getAsyncWorker
import com.wireguard.android.di.ext.getTunnelManager
import com.wireguard.android.util.ZipExporter
import com.wireguard.android.util.isPermissionGranted
import org.koin.core.KoinComponent
import timber.log.Timber

class ViscerionUpdateReceiver : BroadcastReceiver(), KoinComponent {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null || intent.action == null || BuildConfig.DEBUG) return
        if (context != null && context.isPermissionGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)) return
        getAsyncWorker().runAsync {
            getTunnelManager().getTunnels().thenAccept { tunnels ->
                ZipExporter.exportZip(tunnels) { filePath, throwable ->
                    Timber.tag("ViscerionUpdate")
                    if (throwable == null) {
                        Timber.d("Successfully exported ZIPs to $filePath")
                    } else {
                        Timber.d(throwable)
                    }
                }
            }
        }
    }
}
