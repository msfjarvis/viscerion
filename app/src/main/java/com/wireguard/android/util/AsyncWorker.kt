/*
 * Copyright © 2017-2019 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.util

import android.os.Handler
import dagger.Reusable
import java.util.concurrent.Executor
import java9.util.concurrent.CompletableFuture
import java9.util.concurrent.CompletionStage
import javax.inject.Inject

/**
 * Helper class for running asynchronous tasks and ensuring they are completed on the main thread.
 */

@Reusable
class AsyncWorker @Inject constructor(private val executor: Executor, private val handler: Handler) {

    fun runAsync(run: () -> Unit): CompletionStage<Void> {
        val future = CompletableFuture<Void>()
        executor.execute {
            try {
                run()
                handler.post { future.complete(null) }
            } catch (t: Throwable) {
                handler.post { future.completeExceptionally(t) }
            }
        }
        return future
    }

    fun <T> supplyAsync(get: () -> T?): CompletionStage<T> {
        val future = CompletableFuture<T>()
        executor.execute {
            try {
                val result = get()
                handler.post { future.complete(result) }
            } catch (t: Throwable) {
                handler.post { future.completeExceptionally(t) }
            }
        }
        return future
    }
}
