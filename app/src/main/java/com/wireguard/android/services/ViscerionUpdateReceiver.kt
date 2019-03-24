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
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.wireguard.android.Application
import com.wireguard.android.BuildConfig
import com.wireguard.android.util.ZipExporter
import timber.log.Timber

class ViscerionUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null || intent.action == null || BuildConfig.DEBUG) return
        if (context != null &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        )
            return
        Application.asyncWorker.runAsync {
            Application.tunnelManager.getTunnels().thenAccept {
                ZipExporter.exportZip(it) { filePath, throwable ->
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
