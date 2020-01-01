/*
 * Copyright © 2017-2019 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.configStore

import com.wireguard.config.Config
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets

class FakeConfigStore(private val filesDir: File) : ConfigStore {

    override fun create(name: String, config: Config): Config {
        val file = fileFor(name)
        if (!file.createNewFile()) {
            throw IOException("${file.name} already exists!")
        }
        FileOutputStream(
            file,
            false
        ).use { stream -> stream.write(config.toWgQuickString().toByteArray(StandardCharsets.UTF_8)) }
        return config
    }

    override fun delete(name: String) {
        val file = fileFor(name)
        if (!file.delete()) {
            throw IOException("Failed to delete $name")
        }
    }

    override fun enumerate(): Set<String> {
        return (filesDir.list() ?: emptyArray())
            .filter { it.endsWith("conf") }
            .map { it.substring(0, it.length - "conf".length) }
            .toSet()
    }

    override fun load(name: String): Config {
        FileInputStream(fileFor(name)).use { stream -> return Config.parse(stream) }
    }

    override fun rename(name: String, replacement: String) {
        val file = fileFor(name)
        val replacementFile = fileFor(replacement)
        if (!replacementFile.createNewFile()) {
            throw IOException("$replacement already exists!")
        }
        if (!file.renameTo(replacementFile)) {
            if (!replacementFile.delete()) {
                println("Couldn't delete marker file for new name $replacement")
            }
            throw IOException("Failed to rename ${file.name}")
        }
    }

    override fun save(name: String, config: Config): Config {
        val file = fileFor(name)
        if (!file.isFile) {
            throw FileNotFoundException("Configuration file \"${file.name}\" not found ")
        }
        FileOutputStream(
            file,
            false
        ).use { stream -> stream.write(config.toWgQuickString().toByteArray(StandardCharsets.UTF_8)) }
        return config
    }

    private fun fileFor(name: String): File {
        return File(filesDir, "$name.conf")
    }
}
