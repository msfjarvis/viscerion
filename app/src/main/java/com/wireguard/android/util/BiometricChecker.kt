/*
 * Copyright © 2017-2019 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
@file:Suppress("DEPRECATION")
package com.wireguard.android.util

import android.annotation.TargetApi
import android.content.Context
import android.hardware.fingerprint.FingerprintManager
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.core.content.getSystemService

sealed class BiometricChecker {

    abstract val hasBiometrics: Boolean

    @TargetApi(29)
    private class QBiometricChecker(
        private val biometricManager: BiometricManager
    ) : BiometricChecker() {

        private val availableCodes = listOf(
                BiometricManager.BIOMETRIC_SUCCESS,
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED
        )

        override val hasBiometrics: Boolean
            get() = availableCodes.contains(biometricManager.canAuthenticate())

        companion object {

            fun getInstance(context: Context): QBiometricChecker? =
                    context.getSystemService<BiometricManager>()?.let {
                        QBiometricChecker(it)
                    }
        }
    }

    private class LegacyBiometricChecker(
        private val fingerprintManager: FingerprintManager
    ) : BiometricChecker() {

        override val hasBiometrics: Boolean
            get() = if (Build.VERSION.SDK_INT >= 23) fingerprintManager.isHardwareDetected else false

        companion object {
            fun getInstance(context: Context): LegacyBiometricChecker? = if (Build.VERSION.SDK_INT >= 23)
                    context.getSystemService<FingerprintManager>()?.let {
                        LegacyBiometricChecker(it)
                    } else null
        }
    }

    private class DefaultBiometricChecker : BiometricChecker() {
        override val hasBiometrics: Boolean = false
    }

    companion object {

        fun getInstance(context: Context): BiometricChecker {
            return when {
                Build.VERSION.SDK_INT >= 29 -> QBiometricChecker.getInstance(context)
                Build.VERSION.SDK_INT >= 23 -> LegacyBiometricChecker.getInstance(context)
                else -> null
            } ?: DefaultBiometricChecker()
        }
    }
}
