/*
 * Copyright © 2017-2018 WireGuard LLC.
 * Copyright © 2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.config

import com.wireguard.android.util.requireNonNull
import com.wireguard.config.BadConfigException.Location
import com.wireguard.config.BadConfigException.Reason
import com.wireguard.config.BadConfigException.Section
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.ArrayList
import java.util.Collections
import java.util.LinkedHashSet

/**
 * Represents the contents of a wg-quick configuration file, made up of one or more "Interface"
 * sections (combined together), and zero or more "Peer" sections (treated individually).
 *
 *
 * Instances of this class are immutable.
 */
class Config private constructor(builder: Builder) {
    /**
     * Returns the interface section of the configuration.
     *
     * @return the interface configuration
     */
    val `interface`: Interface
    /**
     * Returns a list of the configuration's peer sections.
     *
     * @return a list of [Peer]s
     */
    val peers: List<Peer>

    init {
        `interface` = builder.interfaze.requireNonNull("An [Interface] section is required")
        // Defensively copy to ensure immutability even if the Builder is reused.
        peers = Collections.unmodifiableList(ArrayList(builder.peers))
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Config)
            return false
        return `interface` == other.`interface` && peers == other.peers
    }

    override fun hashCode(): Int {
        return 31 * `interface`.hashCode() + peers.hashCode()
    }

    /**
     * Converts the `Config` into a string suitable for debugging purposes. The `Config`
     * is identified by its interface's public key and the number of peers it has.
     *
     * @return a concise single-line identifier for the `Config`
     */
    override fun toString(): String {
        return "(Config $`interface` (${peers.size}))"
    }

    /**
     * Converts the `Config` into a string suitable for use as a `wg-quick`
     * configuration file.
     *
     * @return the `Config` represented as one [Interface] and zero or more [Peer] sections
     */
    fun toWgQuickString(): String {
        val sb = StringBuilder()
        sb.append("[Interface]\n").append(`interface`.toWgQuickString())
        for (peer in peers)
            sb.append("\n[Peer]\n").append(peer.toWgQuickString())
        return sb.toString()
    }

    /**
     * Serializes the `Config` for use with the WireGuard cross-platform userspace API.
     *
     * @return the `Config` represented as a series of "key=value" lines
     */
    fun toWgUserspaceString(): String {
        val sb = StringBuilder()
        sb.append(`interface`.toWgUserspaceString())
        sb.append("replace_peers=true\n")
        for (peer in peers)
            sb.append(peer.toWgUserspaceString())
        return sb.toString()
    }

    class Builder {
        // Defaults to an empty set.
        val peers = LinkedHashSet<Peer>()
        // No default; must be provided before building.
        var interfaze: Interface? = null

        private fun addPeer(peer: Peer): Builder {
            peers.add(peer)
            return this
        }

        fun addPeers(peers: Collection<Peer>): Builder {
            this.peers.addAll(peers)
            return this
        }

        fun build(): Config {
            if (interfaze == null)
                throw IllegalArgumentException("An [Interface] section is required")
            return Config(this)
        }

        @Throws(BadConfigException::class)
        fun parseInterface(lines: Iterable<CharSequence>): Builder {
            return setInterface(Interface.parse(lines))
        }

        @Throws(BadConfigException::class)
        fun parsePeer(lines: Iterable<CharSequence>): Builder {
            return addPeer(Peer.parse(lines))
        }

        fun setInterface(interfaze: Interface): Builder {
            this.interfaze = interfaze
            return this
        }
    }

    companion object {

        /**
         * Parses an series of "Interface" and "Peer" sections into a `Config`. Throws
         * [BadConfigException] if the input is not well-formed or contains data that cannot
         * be parsed.
         *
         * @param stream a stream of UTF-8 text that is interpreted as a WireGuard configuration
         * @return a `Config` instance representing the supplied configuration
         */
        @Throws(IOException::class, BadConfigException::class)
        fun parse(stream: InputStream?): Config {
            return parse(BufferedReader(InputStreamReader(stream)))
        }

        /**
         * Parses an series of "Interface" and "Peer" sections into a `Config`. Throws
         * [BadConfigException] if the input is not well-formed or contains data that cannot
         * be parsed.
         *
         * @param reader a BufferedReader of UTF-8 text that is interpreted as a WireGuard configuration
         * @return a `Config` instance representing the supplied configuration
         */
        @Throws(IOException::class, BadConfigException::class)
        fun parse(reader: BufferedReader): Config {
            val builder = Builder()
            val interfaceLines = ArrayList<String>()
            val peerLines = ArrayList<String>()
            var inInterfaceSection = false
            var inPeerSection = false
            var line: String?
            while (true) {
                line = reader.readLine()
                if (line == null)
                    break
                val commentIndex = line.indexOf('#')
                if (commentIndex != -1)
                    line = line.substring(0, commentIndex)
                line = line.trim { it <= ' ' }
                if (line.isEmpty())
                    continue
                when {
                    line.startsWith("[") -> {
                        // Consume all [Peer] lines read so far.
                        if (inPeerSection) {
                            builder.parsePeer(peerLines)
                            peerLines.clear()
                        }
                        when {
                            "[Interface]".equals(line, ignoreCase = true) -> {
                                inInterfaceSection = true
                                inPeerSection = false
                            }
                            "[Peer]".equals(line, ignoreCase = true) -> {
                                inInterfaceSection = false
                                inPeerSection = true
                            }
                            else -> throw BadConfigException(
                                Section.CONFIG, Location.TOP_LEVEL,
                                Reason.UNKNOWN_SECTION, line
                            )
                        }
                    }
                    inInterfaceSection -> interfaceLines.add(line)
                    inPeerSection -> peerLines.add(line)
                    else -> throw BadConfigException(
                        Section.CONFIG, Location.TOP_LEVEL,
                        Reason.UNKNOWN_SECTION, line
                    )
                }
            }
            if (inPeerSection)
                builder.parsePeer(peerLines)
            else if (!inInterfaceSection)
                throw BadConfigException(
                    Section.CONFIG, Location.TOP_LEVEL,
                    Reason.MISSING_SECTION, null
                )
            // Combine all [Interface] sections in the file.
            builder.parseInterface(interfaceLines)
            return builder.build()
        }
    }
}
