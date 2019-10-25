/*
 * Copyright © 2017-2019 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.util

import android.content.res.Resources
import com.wireguard.android.Application
import com.wireguard.android.R
import com.wireguard.config.BadConfigException
import com.wireguard.config.BadConfigException.Reason
import com.wireguard.config.InetEndpoint
import com.wireguard.config.InetNetwork
import com.wireguard.config.ParseException
import java.net.InetAddress
import me.msfjarvis.viscerion.crypto.Key.Format
import me.msfjarvis.viscerion.crypto.KeyFormatException
import me.msfjarvis.viscerion.crypto.KeyFormatException.Type

object ErrorMessages {
    private val BCE_REASON_MAP = mapOf(
            Reason.INVALID_KEY to R.string.bad_config_reason_invalid_key,
            Reason.INVALID_NUMBER to R.string.bad_config_reason_invalid_number,
            Reason.INVALID_VALUE to R.string.bad_config_reason_invalid_value,
            Reason.MISSING_ATTRIBUTE to R.string.bad_config_reason_missing_attribute,
            Reason.MISSING_SECTION to R.string.bad_config_reason_missing_section,
            Reason.MISSING_VALUE to R.string.bad_config_reason_missing_value,
            Reason.SYNTAX_ERROR to R.string.bad_config_reason_syntax_error,
            Reason.UNKNOWN_ATTRIBUTE to R.string.bad_config_reason_unknown_attribute,
            Reason.UNKNOWN_SECTION to R.string.bad_config_reason_unknown_section
    )

    private val KFE_FORMAT_MAP = mapOf(
            Format.BASE64 to R.string.key_length_explanation_base64,
            Format.BINARY to R.string.key_length_explanation_binary,
            Format.HEX to R.string.key_length_explanation_hex
    )

    private val KFE_TYPE_MAP = mapOf(
            Type.CONTENTS to R.string.key_contents_error,
            Type.LENGTH to R.string.key_length_error
    )

    private val PE_CLASS_MAP = mapOf<Class<*>, Int>(
            InetAddress::class.java to R.string.parse_error_inet_address,
            InetEndpoint::class.java to R.string.parse_error_inet_endpoint,
            InetNetwork::class.java to R.string.parse_error_inet_network,
            Int::class.java to R.string.parse_error_integer
    )

    operator fun get(throwable: Throwable?): String {
        val resources = Application.get().resources
        if (throwable == null)
            return resources.getString(R.string.unknown_error)
        val rootCause = rootCause(throwable)
        val message: String
        when {
            rootCause is BadConfigException -> {
                val reason = getBadConfigExceptionReason(resources, rootCause)
                val context = if (rootCause.location == BadConfigException.Location.TOP_LEVEL)
                    resources.getString(
                            R.string.bad_config_context_top_level,
                            rootCause.section.name
                    )
                else
                    resources.getString(
                            R.string.bad_config_context,
                            rootCause.section.name,
                            rootCause.location.name
                    )
                val explanation = getBadConfigExceptionExplanation(resources, rootCause)
                message = resources.getString(R.string.bad_config_error, reason, context) + explanation
            }
            rootCause.message != null -> message = rootCause.message as String
            else -> {
                val errorType = rootCause.javaClass.simpleName
                message = resources.getString(R.string.generic_error, errorType)
            }
        }
        return message
    }

    private fun getBadConfigExceptionExplanation(
        resources: Resources,
        bce: BadConfigException
    ): String {
        if (bce.cause is KeyFormatException) {
            val kfe = bce.cause
            if (kfe.type == Type.LENGTH)
                return resources.getString(KFE_FORMAT_MAP[kfe.format] as Int)
        } else if (bce.cause is ParseException) {
            val pe = bce.cause
            if (pe.message != null)
                return ": " + pe.message
        } else if (bce.location == BadConfigException.Location.LISTEN_PORT) {
            return resources.getString(R.string.bad_config_explanation_udp_port)
        } else if (bce.location == BadConfigException.Location.MTU) {
            return resources.getString(R.string.bad_config_explanation_positive_number)
        } else if (bce.location == BadConfigException.Location.PERSISTENT_KEEPALIVE) {
            return resources.getString(R.string.bad_config_explanation_pka)
        }
        return ""
    }

    private fun getBadConfigExceptionReason(
        resources: Resources,
        bce: BadConfigException
    ): String {
        if (bce.cause is KeyFormatException) {
            val kfe = bce.cause
            return resources.getString(KFE_TYPE_MAP[kfe.type] as Int)
        } else if (bce.cause is ParseException) {
            val pe = bce.cause
            val type = resources.getString(
                    if (PE_CLASS_MAP.containsKey(pe.parsingClass))
                        PE_CLASS_MAP[pe.parsingClass] as Int
                    else
                        R.string.parse_error_generic
            )
            return resources.getString(R.string.parse_error_reason, type, pe.text)
        }
        return resources.getString(BCE_REASON_MAP[bce.reason] as Int, bce.text)
    }

    private fun rootCause(throwable: Throwable): Throwable {
        var cause = throwable
        while (cause.cause != null) {
            if (cause is BadConfigException)
                break
            cause = cause.cause as Throwable
        }
        return cause
    }
}
