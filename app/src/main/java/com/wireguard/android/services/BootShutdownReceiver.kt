/*
 * Copyright © 2017-2020 WireGuard LLC.
 * Copyright © 2018-2020 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
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
import com.wireguard.android.di.getInjector
import com.wireguard.android.model.TunnelManager
import com.wireguard.android.util.BackendAsync
import com.wireguard.android.work.TunnelRestoreWorker
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import timber.log.Timber

class BootShutdownReceiver : BroadcastReceiver() {

    @Inject lateinit var backendAsync: BackendAsync
    @Inject lateinit var tunnelManager: TunnelManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == null) return
        getInjector(context).inject(this)
        backendAsync.thenAccept { backend ->
            if (backend !is WgQuickBackend) {
                return@thenAccept
            }
            val action = intent.action
            if (Intent.ACTION_BOOT_COMPLETED == action) {
                Timber.i("Broadcast receiver attempting to restore state (boot)")
                val restoreWork = OneTimeWorkRequestBuilder<TunnelRestoreWorker>()
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
