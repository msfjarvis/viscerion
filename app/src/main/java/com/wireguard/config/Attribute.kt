/*
 * Copyright © 2019 Harsh Shandilya. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.config

import android.text.TextUtils
import java.util.regex.Pattern

class Attribute private constructor(val key: String, val value: String) {
    companion object {
        private val LINE_PATTERN = Pattern.compile("(\\w+)\\s*=\\s*([^\\s#][^#]*)")
        val LIST_SEPARATOR = Pattern.compile("\\s*,\\s*")

        fun join(values: Iterable<*>): String {
            return TextUtils.join(", ", values)
        }

        fun parse(line: CharSequence): Attribute? {
            val matcher = LINE_PATTERN.matcher(line)
            return if (!matcher.matches()) null else
                Attribute(
                    matcher.group(1),
                    matcher.group(2)
                )
        }

        fun split(value: CharSequence): Array<String> {
            return LIST_SEPARATOR.split(value)
        }
    }
}
