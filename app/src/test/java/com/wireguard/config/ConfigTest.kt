/*
 * Copyright © 2017-2019 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.config

import android.content.Context
import android.content.SharedPreferences
import com.wireguard.android.util.ApplicationPreferences
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.mockito.Mockito

class ConfigTest : KoinTest {

    @Before
    fun setupKoin() {
        val sharedPrefs = Mockito.mock(SharedPreferences::class.java)
        val context: Context = Mockito.mock(Context::class.java)
        startKoin {
            androidContext(context)
            modules(module {
                single { ApplicationPreferences(sharedPrefs) }
            })
        }
    }

    @After
    fun cleanup() {
        stopKoin()
    }

    @Test
    fun `test config parses correctly`() {
        val testConfig = javaClass.classLoader!!.getResourceAsStream("working.conf")
        val config = Config.parse(testConfig)
        assertNotNull("Valid configs cannot not be null after parsing", config)
        assertTrue(
            "No applications should be excluded by default",
            config.interfaze.excludedApplications.isEmpty()
        )
        assertTrue("Test config has exactly one peer", config.peers.size == 1)
        assertTrue(
            "Test config's allowed IPs are 0.0.0.0/0 and ::0/0",
            config.peers.getOrNull(0)!!.allowedIps ==
                setOf(InetNetwork.parse("0.0.0.0/0"), InetNetwork.parse("::0/0"))
        )
        assertTrue("Test config has one DNS server", config.interfaze.dnsServers.size == 1)
        assertTrue(
            "",
            config.interfaze.dnsServers == setOf(InetAddressUtils.parse("193.138.218.74"))
        )
    }

    @Test(expected = BadConfigException::class)
    fun `broken config throws BadConfigException`() {
        Config.parse(javaClass.classLoader!!.getResourceAsStream("broken.conf"))
    }
}
