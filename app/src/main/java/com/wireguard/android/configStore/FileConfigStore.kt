/*
 * Copyright © 2017-2018 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.wireguard.android.configStore

import android.content.Context
import com.wireguard.config.Config
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets

/**
 * Configuration store that uses a `wg-quick`-style file for each configured tunnel.
 */

class FileConfigStore(private val context: Context) : ConfigStore {

    @Throws(IOException::class)
    override fun create(name: String, config: Config): Config {
        Timber.tag(TAG).d("Creating configuration for tunnel $name")
        val file = fileFor(name)
        if (!file.createNewFile())
            throw IOException("Configuration file ${file.name} already exists")
        FileOutputStream(
            file,
            false
        ).use { stream -> stream.write(config.toString().toByteArray(StandardCharsets.UTF_8)) }
        return config
    }

    @Throws(IOException::class)
    override fun delete(name: String) {
        Timber.tag(TAG).d("Deleting configuration for tunnel $name")
        val file = fileFor(name)
        if (!file.delete())
            throw IOException("Cannot delete configuration file ${file.name}")
    }

    override fun enumerate(): Set<String> {
        return context.fileList()
            .filter { it -> it.endsWith(CONFIGURATION_FILE_SUFFIX) }
            .map { it -> it.substring(0, it.length - CONFIGURATION_FILE_SUFFIX.length) }
            .toSet()
    }

    private fun fileFor(name: String): File {
        return File(context.filesDir, "$name.conf")
    }

    @Throws(IOException::class)
    override fun load(name: String): Config {
        FileInputStream(fileFor(name)).use { stream -> return Config.from(stream) }
    }

    @Throws(IOException::class)
    override fun rename(name: String, replacement: String) {
        Timber.tag(TAG).d("Renaming configuration for tunnel $name to $replacement")
        val file = fileFor(name)
        val replacementFile = fileFor(replacement)
        if (!replacementFile.createNewFile())
            throw IOException("Configuration for $replacement already exists")
        if (!file.renameTo(replacementFile)) {
            if (!replacementFile.delete())
                Timber.tag(TAG).w("Couldn't delete marker file for new name $replacement")
            throw IOException("Cannot rename configuration file ${file.name}")
        }
    }

    @Throws(IOException::class)
    override fun save(name: String, config: Config): Config {
        Timber.tag(TAG).d("Saving configuration for tunnel $name")
        val file = fileFor(name)
        if (!file.isFile)
            throw FileNotFoundException("Configuration file ${file.name} not found")
        FileOutputStream(
            file,
            false
        ).use { stream -> stream.write(config.toString().toByteArray(StandardCharsets.UTF_8)) }
        return config
    }

    companion object {
        private val TAG = "WireGuard/" + FileConfigStore::class.java.simpleName
        const val CONFIGURATION_FILE_SUFFIX = ".conf"
    }
}
