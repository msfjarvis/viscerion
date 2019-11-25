/*
 * Copyright © 2017-2019 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.config

import android.content.Context
import androidx.preference.PreferenceManager
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.wireguard.android.util.ApplicationPreferences
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest

@LargeTest
class ConfigTest : AutoCloseKoinTest() {
    private lateinit var targetContext: Context
    private lateinit var testContext: Context
    private val testModule = module {
        single { PreferenceManager.getDefaultSharedPreferences(androidContext()) }
        single { ApplicationPreferences(get()) }
    }

    @Before
    fun init() {
        targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        testContext = InstrumentationRegistry.getInstrumentation().context
        stopKoin()
        startKoin {
            androidContext(targetContext)
            modules(testModule)
        }
    }

    @Test
    fun validConfigReadSuccessfully() {
        val testConfig = testContext.assets.open("working.conf")
        val config = Config.parse(testConfig)
        assertNotNull("Valid configs cannot not be null after parsing", config)
        assertTrue("No applications should be excluded by default", config.`interface`.excludedApplications.isEmpty())
        assertTrue("Test config has exactly one peer", config.peers.size == 1)
        assertTrue(
            "Test config's allowed IPs are 0.0.0.0/0 and ::0/0",
            config.peers.getOrNull(0)!!.allowedIps ==
                setOf(InetNetwork.parse("0.0.0.0/0"), InetNetwork.parse("::0/0"))
        )
        assertTrue("Test config has one DNS server", config.`interface`.dnsServers.size == 1)
        assertTrue("", config.`interface`.dnsServers == setOf(InetAddressUtils.parse("193.138.218.74")))
    }

    @Test(expected = BadConfigException::class)
    fun invalidConfigThrows() {
        Config.parse(testContext.assets.open("broken.conf"))
    }
}
