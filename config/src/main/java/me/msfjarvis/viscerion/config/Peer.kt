/*
 * Copyright © 2017-2019 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package me.msfjarvis.viscerion.config

import java.util.Collections
import java.util.Locale
import me.msfjarvis.viscerion.config.BadConfigException.Location
import me.msfjarvis.viscerion.config.BadConfigException.Reason
import me.msfjarvis.viscerion.config.BadConfigException.Section
import me.msfjarvis.viscerion.crypto.Key
import me.msfjarvis.viscerion.crypto.KeyFormatException

/**
 * Represents the configuration for a WireGuard peer (a [Peer] block). Peers must have a public key,
 * and may optionally have several other attributes.
 *
 *
 * Instances of this class are immutable.
 */
class Peer private constructor(builder: Builder) {
    /**
     * Returns the peer's set of allowed IPs.
     *
     * @return the set of allowed IPs
     */
    // The collection is already immutable.
    val allowedIps: Set<InetNetwork>
    /**
     * Returns the peer's endpoint.
     *
     * @return the endpoint, or `null` if none is configured
     */
    val endpoint: InetEndpoint?
    /**
     * Returns the peer's persistent keepalive.
     *
     * @return the persistent keepalive, or `null` if none is configured
     */
    val persistentKeepalive: Int?
    /**
     * Returns the peer's pre-shared key.
     *
     * @return the pre-shared key, or `null` if none is configured
     */
    val preSharedKey: Key?
    /**
     * Returns the peer's public key.
     *
     * @return the public key
     */
    val publicKey: Key

    init {
        // Defensively copy to ensure immutability even if the Builder is reused.
        allowedIps = Collections.unmodifiableSet(LinkedHashSet(builder.allowedIps))
        endpoint = builder.endpoint
        persistentKeepalive = builder.persistentKeepalive
        preSharedKey = builder.preSharedKey
        publicKey = requireNotNull(builder.publicKey) { "Peers must have a public key" }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Peer) {
            return false
        }
        return (allowedIps == other.allowedIps &&
                endpoint == other.endpoint &&
                persistentKeepalive == other.persistentKeepalive &&
                preSharedKey == other.preSharedKey &&
                publicKey == other.publicKey)
    }

    override fun hashCode(): Int {
        var hash = 1
        hash = 31 * hash + allowedIps.hashCode()
        hash = 31 * hash + endpoint.hashCode()
        hash = 31 * hash + persistentKeepalive.hashCode()
        hash = 31 * hash + preSharedKey.hashCode()
        hash = 31 * hash + publicKey.hashCode()
        return hash
    }

    /**
     * Converts the `Peer` into a string suitable for debugging purposes. The `Peer` is
     * identified by its public key and (if known) its endpoint.
     *
     * @return a concise single-line identifier for the `Peer`
     */
    override fun toString(): String {
        val sb = StringBuilder("(Peer ")
        sb.append(publicKey.toBase64())
        endpoint?.let { ep -> sb.append(" @").append(ep) }
        sb.append(')')
        return sb.toString()
    }

    /**
     * Converts the `Peer` into a string suitable for inclusion in a `wg-quick`
     * configuration file.
     *
     * @return the `Peer` represented as a series of "Key = Value" lines
     */
    fun toWgQuickString(): String {
        val sb = StringBuilder()
        if (allowedIps.isNotEmpty()) {
            sb.append("AllowedIPs = ").append(Attribute.join(allowedIps)).append('\n')
        }
        endpoint?.let { ep -> sb.append("Endpoint = ").append(ep).append('\n') }
        persistentKeepalive?.let { pk -> sb.append("PersistentKeepalive = ").append(pk).append('\n') }
        preSharedKey?.let { psk -> sb.append("PreSharedKey = ").append(psk.toBase64()).append('\n') }
        sb.append("PublicKey = ").append(publicKey.toBase64()).append('\n')
        return sb.toString()
    }

    /**
     * Serializes the `Peer` for use with the WireGuard cross-platform userspace API. Note
     * that not all attributes are included in this representation.
     *
     * @return the `Peer` represented as a series of "key=value" lines
     */
    fun toWgUserspaceString(): String {
        val sb = StringBuilder()
        // The order here is important: public_key signifies the beginning of a new peer.
        sb.append("public_key=").append(publicKey.toHex()).append('\n')
        for (allowedIp in allowedIps) {
            sb.append("allowed_ip=").append(allowedIp).append('\n')
        }
        endpoint?.getResolved().let { ep -> sb.append("endpoint=").append(ep).append('\n') }
        persistentKeepalive?.let { pk -> sb.append("persistent_keepalive_interval=").append(pk).append('\n') }
        preSharedKey?.let { psk -> sb.append("preshared_key=").append(psk.toHex()).append('\n') }
        return sb.toString()
    }

    class Builder {

        // Defaults to an empty set.
        val allowedIps = LinkedHashSet<InetNetwork>()
        // Defaults to not present.
        var endpoint: InetEndpoint? = null
        // Defaults to not present.
        var persistentKeepalive: Int? = 0
        // Defaults to not present.
        var preSharedKey: Key? = null
        // No default; must be provided before building.
        var publicKey: Key? = null

        fun addAllowedIp(allowedIp: InetNetwork): Builder {
            allowedIps.add(allowedIp)
            return this
        }

        fun addAllowedIps(allowedIps: Collection<InetNetwork>): Builder {
            this.allowedIps.addAll(allowedIps)
            return this
        }

        @Throws(BadConfigException::class)
        fun build(): Peer {
            if (publicKey == null) {
                throw BadConfigException(
                    Section.PEER, Location.PUBLIC_KEY,
                    Reason.MISSING_ATTRIBUTE, null
                )
            }
            return Peer(this)
        }

        @Throws(BadConfigException::class)
        fun parseAllowedIPs(allowedIps: CharSequence): Builder {
            try {
                for (allowedIp in Attribute.split(allowedIps)) {
                    addAllowedIp(InetNetwork.parse(allowedIp))
                }
                return this
            } catch (e: ParseException) {
                throw BadConfigException(Section.PEER, Location.ALLOWED_IPS, e)
            }
        }

        @Throws(BadConfigException::class)
        fun parseEndpoint(endpoint: String): Builder {
            try {
                return setEndpoint(InetEndpoint.parse(endpoint))
            } catch (e: ParseException) {
                throw BadConfigException(Section.PEER, Location.ENDPOINT, e)
            }
        }

        @Throws(BadConfigException::class)
        fun parsePersistentKeepalive(persistentKeepalive: String): Builder {
            try {
                return setPersistentKeepalive(Integer.parseInt(persistentKeepalive))
            } catch (e: NumberFormatException) {
                throw BadConfigException(
                    Section.PEER, Location.PERSISTENT_KEEPALIVE,
                    persistentKeepalive, e
                )
            }
        }

        @Throws(BadConfigException::class)
        fun parsePreSharedKey(preSharedKey: String): Builder {
            try {
                return setPreSharedKey(Key.fromBase64(preSharedKey))
            } catch (e: KeyFormatException) {
                throw BadConfigException(Section.PEER, Location.PRE_SHARED_KEY, e)
            }
        }

        @Throws(BadConfigException::class)
        fun parsePublicKey(publicKey: String): Builder {
            try {
                return setPublicKey(Key.fromBase64(publicKey))
            } catch (e: KeyFormatException) {
                throw BadConfigException(Section.PEER, Location.PUBLIC_KEY, e)
            }
        }

        fun setEndpoint(endpoint: InetEndpoint): Builder {
            this.endpoint = endpoint
            return this
        }

        @Throws(BadConfigException::class)
        fun setPersistentKeepalive(persistentKeepalive: Int): Builder {
            if (persistentKeepalive < 0 || persistentKeepalive > MAX_PERSISTENT_KEEPALIVE) {
                throw BadConfigException(
                    Section.PEER, Location.PERSISTENT_KEEPALIVE,
                    Reason.INVALID_VALUE, persistentKeepalive.toString()
                )
            }
            this.persistentKeepalive = persistentKeepalive
            return this
        }

        fun setPreSharedKey(preSharedKey: Key): Builder {
            this.preSharedKey = preSharedKey
            return this
        }

        fun setPublicKey(publicKey: Key): Builder {
            this.publicKey = publicKey
            return this
        }

        companion object {
            // See wg(8)
            private const val MAX_PERSISTENT_KEEPALIVE = 65535
        }
    }

    companion object {

        /**
         * Parses an series of "KEY = VALUE" lines into a `Peer`. Throws [ParseException] if
         * the input is not well-formed or contains unknown attributes.
         *
         * @param lines an iterable sequence of lines, containing at least a public key attribute
         * @return a `Peer` with all of its attributes set from `lines`
         */
        @Throws(BadConfigException::class)
        fun parse(lines: Iterable<CharSequence>): Peer {
            val builder = Builder()
            for (line in lines) {
                val attribute = Attribute.parse(line) ?: throw BadConfigException(
                    Section.PEER, Location.TOP_LEVEL,
                    Reason.SYNTAX_ERROR, line
                )
                when (attribute.key.toLowerCase(Locale.ENGLISH)) {
                    "allowedips" -> builder.parseAllowedIPs(attribute.value)
                    "endpoint" -> builder.parseEndpoint(attribute.value)
                    "persistentkeepalive" -> builder.parsePersistentKeepalive(attribute.value)
                    "presharedkey" -> builder.parsePreSharedKey(attribute.value)
                    "publickey" -> builder.parsePublicKey(attribute.value)
                    else -> throw BadConfigException(
                        Section.PEER, Location.TOP_LEVEL,
                        Reason.UNKNOWN_ATTRIBUTE, attribute.key
                    )
                }
            }
            return builder.build()
        }
    }
}
