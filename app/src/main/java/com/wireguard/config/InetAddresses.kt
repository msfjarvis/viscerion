/*
 * Copyright © 2018 Samuel Holland <samuel@sholland.org>
 * Copyright © 2018 Jason A. Donenfeld <Jason@zx2c4.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.wireguard.config

import com.wireguard.android.Application
import com.wireguard.android.R

import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.net.InetAddress

object InetAddresses {
    private val PARSER_METHOD: Method

    init {
        try {
            // This method is only present on Android.
            PARSER_METHOD = InetAddress::class.java
                .getMethod("parseNumericAddress", String::class.java)
        } catch (e: NoSuchMethodException) {
            throw RuntimeException(e)
        }
    }

    fun parse(address: String?): InetAddress {
        if (address == null || address.isEmpty())
            throw IllegalArgumentException(
                Application.get().getString(R.string.tunnel_error_empty_inetaddress)
            )
        try {
            return PARSER_METHOD.invoke(null, address) as InetAddress
        } catch (e: IllegalAccessException) {
            throw RuntimeException(if (e.cause == null) e else e.cause)
        } catch (e: InvocationTargetException) {
            throw RuntimeException(if (e.cause == null) e else e.cause)
        }
    }
}
