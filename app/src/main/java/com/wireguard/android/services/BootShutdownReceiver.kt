/*
 * Copyright © 2017-2019 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.BackoffPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.wireguard.android.backend.WgQuickBackend
import com.wireguard.android.di.ext.getBackendAsync
import com.wireguard.android.di.ext.getTunnelManager
import com.wireguard.android.work.TunnelRestoreWork
import org.koin.core.KoinComponent
import timber.log.Timber
import java.util.concurrent.TimeUnit

class BootShutdownReceiver : BroadcastReceiver(), KoinComponent {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == null) return
        getBackendAsync().thenAccept { backend ->
            if (backend !is WgQuickBackend)
                return@thenAccept
            val action = intent.action
            val tunnelManager = getTunnelManager()
            if (Intent.ACTION_BOOT_COMPLETED == action) {
                Timber.i("Broadcast receiver attempting to restore state (boot)")
                val restoreWork = OneTimeWorkRequestBuilder<TunnelRestoreWork>()
                        .setBackoffCriteria(
                                BackoffPolicy.LINEAR,
                                OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
                                TimeUnit.MILLISECONDS
                        )
                        .addTag("restore_work")
                        .build()
                WorkManager.getInstance(context).enqueue(restoreWork)
            } else if (Intent.ACTION_SHUTDOWN == action) {
                Timber.i("Broadcast receiver saving state (shutdown)")
                tunnelManager.saveState()
            }
        }
    }
}
