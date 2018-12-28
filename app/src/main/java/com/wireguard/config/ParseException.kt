/*
 * Copyright © 2018 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.wireguard.config

import androidx.annotation.Nullable

/**
 */
class ParseException @JvmOverloads constructor(
    val parsingClass: Class<*>,
    val text: CharSequence,
    @Nullable message: String? = null,
    @Nullable cause: Throwable? = null
) : Exception(message, cause) {

    constructor(
        parsingClass: Class<*>,
        text: CharSequence,
        @Nullable cause: Throwable
    ) : this(parsingClass, text, null, cause)
}