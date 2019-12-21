/*
 * Copyright © 2017-2019 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.config

import java.util.regex.Pattern

class Attribute private constructor(val key: String, val value: String) {
    companion object {
        private val LINE_PATTERN = Pattern.compile("(\\w+)\\s*=\\s*([^\\s#][^#]*)")
        private val LIST_SEPARATOR: Pattern = Pattern.compile("\\s*,\\s*")

        fun join(values: Iterable<*>): String {
            val it: Iterator<*> = values.iterator()
            if (!it.hasNext()) {
                return ""
            }
            val sb = StringBuilder()
            sb.append(it.next())
            while (it.hasNext()) {
                sb.append(", ")
                sb.append(it.next())
            }
            return sb.toString()
        }

        fun parse(line: CharSequence): Attribute? {
            val matcher = LINE_PATTERN.matcher(line)
            return if (matcher.matches()) {
                Attribute(
                    requireNotNull(matcher.group(1)),
                    requireNotNull(matcher.group(2))
                )
            } else {
                null
            }
        }

        fun split(value: CharSequence): Array<String> {
            return LIST_SEPARATOR.split(value)
        }
    }
}
