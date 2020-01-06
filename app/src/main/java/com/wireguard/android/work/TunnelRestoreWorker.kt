/*
 * Copyright © 2017-2020 WireGuard LLC.
 * Copyright © 2018-2020 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.work

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.wireguard.android.di.getInjector
import com.wireguard.android.model.TunnelManager
import com.wireguard.android.util.ExceptionLoggers
import com.wireguard.android.util.RootShell
import javax.inject.Inject
import timber.log.Timber

class TunnelRestoreWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams) {
    @Inject lateinit var tunnelManager: TunnelManager
    @Inject lateinit var rootShell: RootShell

    override fun doWork(): Result {
        getInjector(applicationContext).inject(this)
        val result = ArrayList<String>()
        rootShell.run(result, "iptables -L | grep Chain")
        rootShell.run(result, "ip6tables -L | grep Chain")
        Timber.tag("RestoreWork")
        val isDropping = result.any { it.contains("DROP") }
        return if (isDropping) {
            // AFWall+ sets all packets to DROP to prevent leaks during boot.
            // Trying to start a WireGuard tunnel in this phase will end badly, so
            // defer the job for later.
            Timber.d("Packets are currently being dropped, defer tunnel state restore")
            Result.retry()
        } else {
            Timber.d("Restoring tunnel state")
            tunnelManager.restoreState(false).whenComplete(ExceptionLoggers.D)
            Result.success()
        }
    }
}
