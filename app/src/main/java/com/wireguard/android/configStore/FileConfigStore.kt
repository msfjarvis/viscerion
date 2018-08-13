/*
 * Copyright © 2018 Samuel Holland <samuel@sholland.org>
 * Copyright © 2018 Jason A. Donenfeld <Jason@zx2c4.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.wireguard.android.configStore

import android.content.Context
import android.util.Log
import com.wireguard.config.Config
import java.io.*
import java.nio.charset.StandardCharsets

/**
 * Configuration store that uses a `wg-quick`-style file for each configured tunnel.
 */

class FileConfigStore(private val context: Context) : ConfigStore {

    @Throws(IOException::class)
    override fun create(name: String, config: Config): Config {
        Log.d(TAG, "Creating configuration for tunnel $name")
        val file = fileFor(name)
        if (!file.createNewFile())
            throw IOException("Configuration file " + file.name + " already exists")
        FileOutputStream(file, false).use { stream -> stream.write(config.toString().toByteArray(StandardCharsets.UTF_8)) }
        return config
    }

    @Throws(IOException::class)
    override fun delete(name: String) {
        Log.d(TAG, "Deleting configuration for tunnel $name")
        val file = fileFor(name)
        if (!file.delete())
            throw IOException("Cannot delete configuration file " + file.name)
    }

    override fun enumerate(): Set<String> {
        return context.fileList()
                .filter { it -> it.endsWith(".conf") }
                .map { it -> it.substring(0, it.length - ".conf".length) }
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
        Log.d(TAG, "Renaming configuration for tunnel $name to $replacement")
        val file = fileFor(name)
        val replacementFile = fileFor(replacement)
        if (!replacementFile.createNewFile())
            throw IOException("Configuration for $replacement already exists")
        if (!file.renameTo(replacementFile)) {
            if (!replacementFile.delete())
                Log.w(TAG, "Couldn't delete marker file for new name $replacement")
            throw IOException("Cannot rename configuration file " + file.name)
        }
    }

    @Throws(IOException::class)
    override fun save(name: String, config: Config): Config {
        Log.d(TAG, "Saving configuration for tunnel $name")
        val file = fileFor(name)
        if (!file.isFile)
            throw FileNotFoundException("Configuration file " + file.name + " not found")
        FileOutputStream(file, false).use { stream -> stream.write(config.toString().toByteArray(StandardCharsets.UTF_8)) }
        return config
    }

    companion object {
        private val TAG = "WireGuard/" + FileConfigStore::class.java.simpleName
    }
}
