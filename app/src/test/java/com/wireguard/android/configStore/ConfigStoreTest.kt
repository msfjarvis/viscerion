/*
 * Copyright © 2017-2020 WireGuard LLC.
 * Copyright © 2018-2020 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.configStore

import java.io.File
import java.io.IOException
import java.nio.file.Files
import me.msfjarvis.viscerion.config.Config
import me.msfjarvis.viscerion.config.InetAddressUtils
import me.msfjarvis.viscerion.config.InetNetwork
import org.junit.After
import org.junit.AfterClass
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

class ConfigStoreTest {
    private val testConfig = javaClass.classLoader!!.getResourceAsStream("working.conf")
    private val config: Config by lazy { Config.parse(testConfig) }

    @After
    fun `clear config store`() {
        tempDir.listFiles()?.forEach { it.delete() }
    }

    @Test
    fun `config creation succeeds`() {
        configStore.create("test-1", config)
        assertTrue("config store must have one config", configStore.enumerate().isNotEmpty())
    }

    @Test
    fun `config rename succeeds`() {
        configStore.create("test-1", config)
        configStore.rename("test-1", "test-2")
        validateConfig(configStore.load("test-2"))
    }

    @Test
    fun `config cannot be duplicated`() {
        configStore.create("test-1", config)
        assertThrows(IOException::class.java) { configStore.create("test-1", config) }
    }

    @Test
    fun `config can be updated`() {
        val configCopy = config
        configStore.create("test-1", configCopy)
        assertFalse(
            "test config has no excluded applications",
            configStore.load("test-1").interfaze.excludedApplications.contains("me.msfjarvis.viscerion")
        )
        configCopy.interfaze.excludedApplications.add("me.msfjarvis.viscerion")
        configStore.save("test-1", configCopy)
        assertTrue(
            "updated config must have 'me.msfjarvis.viscerion' excluded",
            configStore.load("test-1").interfaze.excludedApplications.contains("me.msfjarvis.viscerion")
        )
    }

    @Test
    fun `config does not mutate on save`() {
        validateConfig(config)
        configStore.create("test-1", config)
        val loadedConfig = configStore.load("test-1")
        validateConfig(loadedConfig)
    }

    private fun validateConfig(configuration: Config) {
        assertNotNull("Valid configs cannot not be null after parsing", configuration)
        assertTrue(
            "No applications should be excluded by default",
            configuration.interfaze.excludedApplications.isEmpty()
        )
        assertTrue("Test config has exactly one peer", configuration.peers.size == 1)
        assertTrue(
            "Test config's allowed IPs are 0.0.0.0/0 and ::0/0",
            configuration.peers[0].allowedIps ==
                setOf(InetNetwork.parse("0.0.0.0/0"), InetNetwork.parse("::0/0"))
        )
        assertTrue("Test config has one DNS server", configuration.interfaze.dnsServers.size == 1)
        assertTrue(
            "193.138.218.74 must be present as a DNS server",
            configuration.interfaze.dnsServers == setOf(InetAddressUtils.parse("193.138.218.74"))
        )
    }

    companion object {
        private lateinit var tempDir: File
        private lateinit var configStore: FakeConfigStore

        @BeforeClass @JvmStatic
        fun createTempFolder() {
            tempDir = Files.createTempDirectory("viscerion").toFile()
            configStore = FakeConfigStore(tempDir)
        }

        @AfterClass @JvmStatic
        fun removeTempFolder() {
            tempDir.deleteRecursively()
        }
    }
}
