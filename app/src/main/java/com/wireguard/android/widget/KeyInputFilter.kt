/*
 * Copyright © 2017-2020 WireGuard LLC.
 * Copyright © 2018-2020 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.widget

import android.text.InputFilter
import android.text.SpannableStringBuilder
import android.text.Spanned
import me.msfjarvis.viscerion.crypto.Key

class KeyInputFilter : InputFilter {
    override fun filter(
        source: CharSequence,
        start: Int,
        end: Int,
        dest: Spanned,
        dstart: Int,
        dend: Int
    ): CharSequence? {
        var replacement: SpannableStringBuilder? = null
        var rIndex = 0
        val dLength = dest.length
        for (sIndex in start until end) {
            val c = source[sIndex]
            val dIndex = dstart + (sIndex - start)
            // Restrict characters to the base64 character set.
            // Ensure adding this character does not push the length over the limit.
            if ((dIndex + 1 < Key.Format.BASE64.length &&
                    isAllowed(c) ||
                    dIndex + 1 == Key.Format.BASE64.length && c == '=') &&
                dLength + (sIndex - start) < Key.Format.BASE64.length
            ) {
                ++rIndex
            } else {
                if (replacement == null) {
                    replacement = SpannableStringBuilder(source, start, end)
                }
                replacement.delete(rIndex, rIndex + 1)
            }
        }
        return replacement
    }

    companion object {
        private fun isAllowed(c: Char): Boolean {
            return Character.isLetterOrDigit(c) || c == '+' || c == '/'
        }

        @JvmStatic
        fun newInstance(): InputFilter {
            return KeyInputFilter()
        }
    }
}
