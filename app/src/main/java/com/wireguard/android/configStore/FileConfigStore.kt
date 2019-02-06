/*
 * Copyright © 2019 Harsh Shandilya. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.configStore

import android.content.Context
import com.wireguard.android.R
import com.wireguard.config.BadConfigException
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
        Timber.d("Creating configuration for tunnel $name")
        val file = fileFor(name)
        if (!file.createNewFile())
            throw IOException(context.getString(R.string.config_file_exists_error, file.name))
        FileOutputStream(
            file,
            false
        ).use { stream -> stream.write(config.toWgQuickString().toByteArray(StandardCharsets.UTF_8)) }
        return config
    }

    @Throws(IOException::class)
    override fun delete(name: String) {
        Timber.d("Deleting configuration for tunnel $name")
        val file = fileFor(name)
        if (!file.delete())
            throw IOException(context.getString(R.string.config_delete_error, file.name))
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

    @Throws(IOException::class, BadConfigException::class)
    override fun load(name: String): Config {
        FileInputStream(fileFor(name)).use { stream -> return Config.parse(stream) }
    }

    @Throws(IOException::class)
    override fun rename(name: String, replacement: String) {
        Timber.d("Renaming configuration for tunnel $name to $replacement")
        val file = fileFor(name)
        val replacementFile = fileFor(replacement)
        if (!replacementFile.createNewFile())
            throw IOException(context.getString(R.string.config_exists_error, replacement))
        if (!file.renameTo(replacementFile)) {
            if (!replacementFile.delete())
                Timber.w("Couldn't delete marker file for new name $replacement")
            throw IOException(context.getString(R.string.config_rename_error, file.name))
        }
    }

    @Throws(IOException::class)
    override fun save(name: String, config: Config): Config {
        Timber.d("Saving configuration for tunnel $name")
        val file = fileFor(name)
        if (!file.isFile)
            throw FileNotFoundException(context.getString(R.string.config_not_found_error, file.name))
        FileOutputStream(
            file,
            false
        ).use { stream -> stream.write(config.toWgQuickString().toByteArray(StandardCharsets.UTF_8)) }
        return config
    }

    companion object {
        const val CONFIGURATION_FILE_SUFFIX = ".conf"
    }
}
