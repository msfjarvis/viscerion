/*
 * Copyright © 2017-2020 WireGuard LLC.
 * Copyright © 2018-2020 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.util

import androidx.biometric.BiometricPrompt

internal sealed class AuthenticationResult {
    internal data class Success(val cryptoObject: BiometricPrompt.CryptoObject?) :
            AuthenticationResult()

    internal data class RecoverableError(val code: Int, val message: CharSequence) :
            AuthenticationResult()

    internal data class UnrecoverableError(val code: Int, val message: CharSequence) :
            AuthenticationResult()

    internal object Failure : AuthenticationResult()
    internal object Cancelled : AuthenticationResult()
}
