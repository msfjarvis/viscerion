/*
 * Copyright © 2018 Samuel Holland <samuel@sholland.org>
 * Copyright © 2018 Jason A. Donenfeld <Jason@zx2c4.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.wireguard.config

import android.annotation.SuppressLint
import android.text.TextUtils
import java.util.regex.Pattern

/**
 * The set of valid attributes for an interface or peer in a WireGuard configuration file.
 */

enum class Attribute(private val token: String) {
    ADDRESS("Address"),
    ALLOWED_IPS("AllowedIPs"),
    DNS("DNS"),
    EXCLUDED_APPLICATIONS("ExcludedApplications"),
    ENDPOINT("Endpoint"),
    LISTEN_PORT("ListenPort"),
    MTU("MTU"),
    PERSISTENT_KEEPALIVE("PersistentKeepalive"),
    PRESHARED_KEY("PresharedKey"),
    PRIVATE_KEY("PrivateKey"),
    PUBLIC_KEY("PublicKey");

    private val pattern: Pattern = Pattern.compile("$token\\s*=\\s*(\\S.*)")

    @SuppressLint("DefaultLocale")
    fun composeWith(value: Any?): String {
        return String.format("%s = %s%n", token, value)
    }

    @SuppressLint("DefaultLocale")
    fun composeWith(value: Int): String {
        return String.format("%s = %d%n", token, value)
    }

    fun <T> composeWith(value: Iterable<T>): String {
        return String.format("%s = %s%n", token, iterableToString(value))
    }

    fun parse(line: CharSequence): String? {
        val matcher = pattern.matcher(line)
        return if (matcher.matches()) matcher.group(1) else null
    }

    fun parseList(line: CharSequence): Array<String>? {
        val matcher = pattern.matcher(line)
        return if (matcher.matches()) stringToList(matcher.group(1)) else null
    }

    companion object {

        private val KEY_MAP: MutableMap<String, Attribute>
        private val LIST_SEPARATOR_PATTERN = Pattern.compile("\\s*,\\s*")
        private val SEPARATOR_PATTERN = Pattern.compile("\\s|=")

        init {
            KEY_MAP = HashMap(Attribute.values().size)
            for (key in Attribute.values()) {
                KEY_MAP[key.token.toLowerCase()] = key
            }
        }

        fun <T> iterableToString(iterable: Iterable<T>): String {
            return TextUtils.join(", ", iterable)
        }

        fun match(line: CharSequence): Attribute? {
            return KEY_MAP[SEPARATOR_PATTERN.split(line)[0].toLowerCase()]
        }

        fun stringToList(string: String?): Array<String> {
            return if (TextUtils.isEmpty(string)) emptyArray() else LIST_SEPARATOR_PATTERN.split(string!!.trim { it <= ' ' })
        }
    }
}
