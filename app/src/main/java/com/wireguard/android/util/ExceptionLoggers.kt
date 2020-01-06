/*
 * Copyright © 2017-2020 WireGuard LLC.
 * Copyright © 2018-2020 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.util

import android.util.Log
import java9.util.concurrent.CompletionException
import java9.util.function.BiConsumer
import timber.log.Timber

/**
 * Helpers for logging exceptions from asynchronous tasks. These can be passed to
 * `CompletionStage.whenComplete()` at the end of an asynchronous future chain.
 */

enum class ExceptionLoggers(private val priority: Int) : BiConsumer<Any, Throwable> {
    D(Log.DEBUG),
    E(Log.ERROR);

    override fun accept(result: Any, throwable: Throwable?) {
        if (throwable != null) {
            Timber.tag(TAG).e(Log.getStackTraceString(throwable))
        } else if (priority <= Log.DEBUG) {
            when (priority) {
                Log.DEBUG -> Timber.tag(TAG).d("Future completed successfully")
                Log.VERBOSE -> Timber.tag(TAG).v("Future completed successfully")
            }
        }
    }

    companion object {

        private val TAG = ExceptionLoggers::class.java.simpleName

        private fun unwrap(throwable: Throwable): Throwable {
            return if (throwable is CompletionException && throwable.cause != null) {
                throwable.cause ?: throwable
            } else {
                throwable
            }
        }

        fun unwrapMessage(throwable: Throwable): String {
            var unwrappableThrowable = throwable
            unwrappableThrowable = unwrap(unwrappableThrowable)
            val message = unwrappableThrowable.message
            return message ?: unwrappableThrowable.javaClass.simpleName
        }
    }
}
