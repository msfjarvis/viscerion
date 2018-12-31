/*
 * Copyright © 2017-2018 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.wireguard.android.util

import android.util.Log
import java9.util.concurrent.CompletionException
import java9.util.function.BiConsumer

/**
 * Helpers for logging exceptions from asynchronous tasks. These can be passed to
 * `CompletionStage.whenComplete()` at the end of an asynchronous future chain.
 */

enum class ExceptionLoggers(private val priority: Int) : BiConsumer<Any, Throwable> {
    D(Log.DEBUG),
    E(Log.ERROR);

    override fun accept(result: Any, throwable: Throwable?) {
        if (throwable != null)
            Log.println(Log.ERROR, TAG, Log.getStackTraceString(throwable))
        else if (priority <= Log.DEBUG)
            Log.println(priority, TAG, "Future completed successfully")
    }

    companion object {

        private val TAG = ExceptionLoggers::class.java.simpleName

        private fun unwrap(throwable: Throwable): Throwable {
            return if (throwable is CompletionException && throwable.cause != null) (throwable.cause
                ?: throwable) else throwable
        }

        fun unwrapMessage(throwable: Throwable): String {
            var unwrappableThrowable = throwable
            unwrappableThrowable = unwrap(unwrappableThrowable)
            val message = unwrappableThrowable.message
            return message ?: unwrappableThrowable.javaClass.simpleName
        }
    }
}
