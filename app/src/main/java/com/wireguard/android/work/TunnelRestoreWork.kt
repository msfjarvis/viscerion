/*
 * Copyright © 2017-2019 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.work

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.wireguard.android.di.ext.injectRootShell
import com.wireguard.android.di.ext.injectTunnelManager
import com.wireguard.android.model.TunnelManager
import com.wireguard.android.util.ExceptionLoggers
import com.wireguard.android.util.RootShell
import org.koin.core.KoinComponent
import timber.log.Timber

class TunnelRestoreWork(
    appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams), KoinComponent {
    private val tunnelManager: TunnelManager by injectTunnelManager()
    private val rootShell: RootShell by injectRootShell()

    override fun doWork(): Result {
        val result = ArrayList<String>()
        rootShell.run(result, "iptables -L | grep Chain")
        rootShell.run(result, "ip6tables -L | grep Chain")
        Timber.tag("RestoreWork")
        val isDropping = result.any { it.contains("DROP") }
        if (isDropping) {
            // AFWall+ sets all packets to DROP to prevent leaks during boot.
            // Trying to start a WireGuard tunnel in this phase will end badly, so
            // defer the job for later.
            Timber.d("Packets are currently being dropped, defer tunnel state restore")
            return Result.retry()
        } else {
            Timber.d("Restoring tunnel state")
            tunnelManager.restoreState(false).whenComplete(ExceptionLoggers.D)
        }
        return Result.success()
    }
}
