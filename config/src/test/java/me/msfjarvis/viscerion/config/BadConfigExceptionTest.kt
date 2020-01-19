/*
 * Copyright © 2017-2020 WireGuard LLC.
 * Copyright © 2018-2020 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package me.msfjarvis.viscerion.config

import java.io.InputStream
import me.msfjarvis.viscerion.config.BadConfigException.Location
import me.msfjarvis.viscerion.config.BadConfigException.Reason
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Test

class BadConfigExceptionTest {

    @Test
    fun `correctly throws with INVALID_KEY reason`() {
        try {
            Config.parse(configMap["invalid-key"])
            throw Exception("Config parsing must fail in this test")
        } catch (exc: BadConfigException) {
            assertEquals(exc.reason, Reason.INVALID_KEY)
            assertEquals(exc.location, Location.PUBLIC_KEY)
        }
    }

    @Test
    fun `correctly throws with INVALID_NUMBER reason`() {
        try {
            Config.parse(configMap["invalid-number"])
            throw Exception("Config parsing must fail in this test")
        } catch (exc: BadConfigException) {
            assertEquals(exc.reason, Reason.INVALID_NUMBER)
            assertEquals(exc.location, Location.PERSISTENT_KEEPALIVE)
        }
    }

    @Test
    fun `correctly throws with INVALID_VALUE reason`() {
        try {
            Config.parse(configMap["invalid-value"])
            throw Exception("Config parsing must fail in this test")
        } catch (exc: BadConfigException) {
            assertEquals(exc.reason, Reason.INVALID_VALUE)
            assertEquals(exc.location, Location.DNS)
        }
    }

    @Test
    fun `correctly throws with MISSING_ATTRIBUTE reason`() {
        try {
            Config.parse(configMap["missing-attribute"])
            throw Exception("Config parsing must fail in this test")
        } catch (exc: BadConfigException) {
            assertEquals(exc.reason, Reason.MISSING_ATTRIBUTE)
            assertEquals(exc.location, Location.PUBLIC_KEY)
        }
    }

    @Test
    fun `correctly throws with MISSING_SECTION reason`() {
        try {
            Config.parse(configMap["missing-section"])
            throw Exception("Config parsing must fail in this test")
        } catch (exc: BadConfigException) {
            assertEquals(exc.reason, Reason.MISSING_ATTRIBUTE)
            assertEquals(exc.location, Location.PRIVATE_KEY)
        }
    }

    @Test
    @Ignore("The underlying codebase is not very smart about differentiating MISSING_VALUE from SYNTAX_ERROR")
    fun `correctly throws with MISSING_VALUE reason`() {
        try {
            Config.parse(configMap["missing-value"])
            throw Exception("Config parsing must fail in this test")
        } catch (exc: BadConfigException) {
            assertEquals(exc.reason, Reason.MISSING_VALUE)
            assertEquals(exc.location, Location.ENDPOINT)
        }
    }

    @Test
    @Ignore("I need to investigate how to trigger this safely without bringing up other errors")
    fun `correctly throws with SYNTAX_ERROR reason`() {
        try {
            Config.parse(configMap["syntax-error"])
            throw Exception("Config parsing must fail in this test")
        } catch (exc: BadConfigException) {
            assertEquals(exc.reason, Reason.SYNTAX_ERROR)
            assertEquals(exc.location, Location.TOP_LEVEL)
        }
    }

    @Test
    fun `correctly throws with UNKNOWN_ATTRIBUTE reason`() {
        try {
            Config.parse(configMap["unknown-attribute"])
            throw Exception("Config parsing must fail in this test")
        } catch (exc: BadConfigException) {
            assertEquals(exc.reason, Reason.UNKNOWN_ATTRIBUTE)
            assertEquals(exc.location, Location.TOP_LEVEL)
        }
    }

    @Test
    fun `correctly throws with UNKNOWN_SECTION reason`() {
        try {
            Config.parse(configMap["unknown-section"])
            throw Exception("Config parsing must fail in this test")
        } catch (exc: BadConfigException) {
            assertEquals(exc.reason, Reason.UNKNOWN_SECTION)
            assertEquals(exc.location, Location.TOP_LEVEL)
        }
    }

    companion object {
        private var configMap: HashMap<String, InputStream> = HashMap()
        private val configList = listOf(
            "invalid-key",
            "invalid-number",
            "invalid-value",
            "missing-attribute",
            "missing-section",
            "missing-value",
            "syntax-error",
            "unknown-attribute",
            "unknown-section"
        )

        @JvmStatic
        @BeforeClass
        fun `read configs to map for parsing`() {
            configList.forEach {
                configMap[it] = this::class.java.classLoader!!.getResourceAsStream("$it.conf")
            }
        }
    }
}
