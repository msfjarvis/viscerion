/*
 * Copyright © 2017-2018 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("DefaultLocale")
package com.wireguard.config

import com.wireguard.android.Application
import com.wireguard.android.R
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.URI
import java.net.URISyntaxException
import java.net.UnknownHostException

class InetEndpoint internal constructor(endpoint: String?) {
    val host: String
    val port: Int
    private var resolvedHost: InetAddress? = null

    val resolvedEndpoint: String
        @Throws(UnknownHostException::class)
        get() {
            if (resolvedHost == null) {
                val candidates = InetAddress.getAllByName(host)
                if (candidates.isEmpty())
                    throw UnknownHostException(host)
                for (addr in candidates) {
                    if (addr is Inet4Address) {
                        resolvedHost = addr
                        break
                    }
                }
                if (resolvedHost == null)
                    resolvedHost = candidates[0]
            }
            return String.format(
                if (resolvedHost is Inet6Address)
                    "[%s]:%d"
                else
                    "%s:%d", resolvedHost?.hostAddress, port
            )
        }

    val endpoint: String
        get() = String.format(
            if (host.contains(":") && !host.contains("["))
                "[%s]:%d"
            else
                "%s:%d", host, port
        )

    init {
        endpoint?.let {
            if (it.indexOf('/') != -1 || it.indexOf('?') != -1 || it.indexOf('#') != -1)
                throw IllegalArgumentException(Application.get().getString(R.string.tunnel_error_forbidden_endpoint_chars))
        }
        val uri: URI
        try {
            uri = URI("wg://$endpoint")
        } catch (e: URISyntaxException) {
            throw IllegalArgumentException(e)
        }

        host = uri.host
        port = uri.port
    }
}
