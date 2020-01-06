/*
 * Copyright © 2017-2020 WireGuard LLC.
 * Copyright © 2018-2020 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package me.msfjarvis.viscerion.config

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfigTest {

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
