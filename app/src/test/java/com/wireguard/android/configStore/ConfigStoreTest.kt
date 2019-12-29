/*
 * Copyright © 2017-2019 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.configStore

import android.content.Context
import com.wireguard.config.Config
import com.wireguard.config.InetAddressUtils
import com.wireguard.config.InetNetwork
import java.io.IOException
import java.nio.file.Files
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock

class ConfigStoreTest {
    private val testConfig = javaClass.classLoader!!.getResourceAsStream("working.conf")
    private val tempDir = Files.createTempDirectory("viscerion").toFile()
    private val config: Config by lazy { Config.parse(testConfig) }
    private val configStore = FileConfigStore(mock(Context::class.java), tempDir)

    @Test
    fun `config creation succeeds`() {
        checkStoreIsEmpty()
        configStore.create("test-1", config)
        assertTrue("config store must have one config", configStore.enumerate().isNotEmpty())
        emptyStore()
    }

    @Test
    fun `config rename succeeds`() {
        checkStoreIsEmpty()
        configStore.create("test-1", config)
        configStore.rename("test-1", "test-2")
        validateConfig(configStore.load("test-2"))
        emptyStore()
    }

    @Test
    fun `config cannot be duplicated`() {
        checkStoreIsEmpty()
        configStore.create("test-1", config)
        assertThrows(IOException::class.java) { configStore.create("test-1", config) }
        emptyStore()
    }

    @Test
    fun `config can be updated`() {
        checkStoreIsEmpty()
        val configCopy = config
        configStore.create("test-1", configCopy)
        assertFalse("test config has no excluded applications", configStore.load("test-1").interfaze.excludedApplications.contains("me.msfjarvis.viscerion"))
        configCopy.interfaze.excludedApplications.add("me.msfjarvis.viscerion")
        configStore.save("test-1", configCopy)
        assertTrue("updated config must have 'me.msfjarvis.viscerion' excluded", configStore.load("test-1").interfaze.excludedApplications.contains("me.msfjarvis.viscerion"))
        emptyStore()
    }

    @Test
    fun `config does not mutate on save`() {
        checkStoreIsEmpty()
        validateConfig(config)
        configStore.create("test-1", config)
        val loadedConfig = configStore.load("test-1")
        validateConfig(loadedConfig)
        emptyStore()
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
            configuration.peers.getOrNull(0)?.allowedIps ==
                    setOf(InetNetwork.parse("0.0.0.0/0"), InetNetwork.parse("::0/0"))
        )
        assertTrue("Test config has one DNS server", configuration.interfaze.dnsServers.size == 1)
        assertTrue(
            "193.138.218.74 must be present as a DNS server",
            configuration.interfaze.dnsServers == setOf(InetAddressUtils.parse("193.138.218.74"))
        )
    }

    private fun checkStoreIsEmpty() {
        assertTrue("config store must be empty at this stage", configStore.enumerate().isEmpty())
    }

    private fun emptyStore() {
        configStore.enumerate().asSequence().forEach(configStore::delete)
        checkStoreIsEmpty()
    }

    @After
    fun cleanup() {
        tempDir.deleteOnExit()
    }
}
