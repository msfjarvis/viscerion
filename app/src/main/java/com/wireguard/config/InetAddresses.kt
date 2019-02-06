/*
 * Copyright © 2019 Harsh Shandilya. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.config

import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress

/**
 * Utility methods for creating instances of [InetAddress].
 */
object InetAddresses {
    private val PARSER_METHOD: Method

    init {
        try {
            // This method is only present on Android.
            PARSER_METHOD = InetAddress::class.java.getMethod("parseNumericAddress", String::class.java)
        } catch (e: NoSuchMethodException) {
            throw RuntimeException(e)
        }
    }

    /**
     * Parses a numeric IPv4 or IPv6 address without performing any DNS lookups.
     *
     * @param address a string representing the IP address
     * @return an instance of [Inet4Address] or [Inet6Address], as appropriate
     */
    @Throws(ParseException::class)
    fun parse(address: String): InetAddress {
        if (address.isEmpty())
            throw ParseException(InetAddress::class.java, address, "Empty address")
        try {
            return PARSER_METHOD.invoke(null, address) as InetAddress
        } catch (e: IllegalAccessException) {
            val cause = e.cause
            // Re-throw parsing exceptions with the original type, as callers might try to catch
            // them. On the other hand, callers cannot be expected to handle reflection failures.
            if (cause is IllegalArgumentException)
                throw ParseException(InetAddress::class.java, address, cause)
            throw RuntimeException(e)
        } catch (e: InvocationTargetException) {
            val cause = e.cause
            if (cause is IllegalArgumentException)
                throw ParseException(InetAddress::class.java, address, cause)
            throw RuntimeException(e)
        }
    }
} // Prevent instantiation.
