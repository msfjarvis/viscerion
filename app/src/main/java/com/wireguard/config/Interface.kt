/*
 * Copyright © 2017-2018 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.wireguard.config

import com.wireguard.android.util.ApplicationPreferences
import com.wireguard.android.util.requireNonNull
import com.wireguard.config.BadConfigException.Location
import com.wireguard.config.BadConfigException.Reason
import com.wireguard.config.BadConfigException.Section
import com.wireguard.crypto.Key
import com.wireguard.crypto.KeyFormatException
import com.wireguard.crypto.KeyPair
import java9.util.stream.Collectors
import java9.util.stream.StreamSupport
import java.net.InetAddress
import java.util.Collections
import java.util.LinkedHashSet
import java.util.Locale

/**
 * Represents the configuration for a WireGuard interface (an [Interface] block). Interfaces must
 * have a private key (used to initialize a `KeyPair`), and may optionally have several other
 * attributes.
 *
 *
 * Instances of this class are immutable.
 */
class Interface private constructor(builder: Builder) {

    /**
     * Returns the set of IP addresses assigned to the interface.
     *
     * @return a set of [InetNetwork]s
     */
    // The collection is already immutable.
    val addresses: Set<InetNetwork>
    /**
     * Returns the set of DNS servers associated with the interface.
     *
     * @return a set of [InetAddress]es
     */
    // The collection is already immutable.
    val dnsServers: Set<InetAddress>
    /**
     * Returns the set of applications excluded from using the interface.
     *
     * @return a set of package names
     */
    // This should ideally be immutable but I require it to not be
    // for my global exclusions implementation. Suggestions welcome
    // on alternate ways to do this.
    var excludedApplications: ArrayList<String>
    /**
     * Returns the public/private key pair used by the interface.
     *
     * @return a key pair
     */
    val keyPair: KeyPair
    /**
     * Returns the UDP port number that the WireGuard interface will listen on.
     *
     * @return a UDP port number, or `Optional.empty()` if none is configured
     */
    val listenPort: Int?
    /**
     * Returns the MTU used for the WireGuard interface.
     *
     * @return the MTU, or `Optional.empty()` if none is configured
     */
    val mtu: Int?

    init {
        // Defensively copy to ensure immutability even if the Builder is reused.
        addresses = Collections.unmodifiableSet(LinkedHashSet(builder.addresses))
        dnsServers = Collections.unmodifiableSet(LinkedHashSet(builder.dnsServers))
        excludedApplications = ArrayList(builder.excludedApplications)
        keyPair = builder.keyPair.requireNonNull("Interfaces must have a private key")
        listenPort = builder.listenPort
        mtu = builder.mtu
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Interface)
            return false
        val obj = other as Interface?
        return (addresses == obj!!.addresses &&
            dnsServers == obj.dnsServers &&
            excludedApplications == obj.excludedApplications &&
            keyPair == obj.keyPair &&
            listenPort == obj.listenPort &&
            mtu == obj.mtu)
    }

    override fun hashCode(): Int {
        var hash = 1
        hash = 31 * hash + addresses.hashCode()
        hash = 31 * hash + dnsServers.hashCode()
        hash = 31 * hash + excludedApplications.hashCode()
        hash = 31 * hash + keyPair.hashCode()
        hash = 31 * hash + listenPort.hashCode()
        hash = 31 * hash + mtu.hashCode()
        return hash
    }

    /**
     * Converts the `Interface` into a string suitable for debugging purposes. The `Interface` is identified by its public key and (if set) the port used for its UDP socket.
     *
     * @return A concise single-line identifier for the `Interface`
     */
    override fun toString(): String {
        val sb = StringBuilder("(Interface ")
        sb.append(keyPair.publicKey.toBase64())
        listenPort?.let { lp -> sb.append(" @").append(lp) }
        sb.append(')')
        return sb.toString()
    }

    /**
     * Converts the `Interface` into a string suitable for inclusion in a `wg-quick`
     * configuration file.
     *
     * @return The `Interface` represented as a series of "Key = Value" lines
     */
    fun toWgQuickString(): String {
        val sb = StringBuilder()
        if (!addresses.isEmpty())
            sb.append("Address = ").append(Attribute.join(addresses)).append('\n')
        if (!dnsServers.isEmpty()) {
            val dnsServerStrings = StreamSupport.stream(dnsServers)
                .map<String>(InetAddress::getHostAddress)
                .collect(Collectors.toUnmodifiableList())
            sb.append("DNS = ").append(Attribute.join(dnsServerStrings)).append('\n')
        }
        if (!excludedApplications.isEmpty())
            sb.append("ExcludedApplications = ").append(
                Attribute.join(
                    excludedApplications + ApplicationPreferences.exclusionsArray
                )
            ).append('\n')
        listenPort?.let { lp -> sb.append("ListenPort = ").append(lp).append('\n') }
        mtu?.let { m -> sb.append("MTU = ").append(m).append('\n') }
        sb.append("PrivateKey = ").append(keyPair.privateKey.toBase64()).append('\n')
        return sb.toString()
    }

    /**
     * Serializes the `Interface` for use with the WireGuard cross-platform userspace API.
     * Note that not all attributes are included in this representation.
     *
     * @return the `Interface` represented as a series of "KEY=VALUE" lines
     */
    fun toWgUserspaceString(): String {
        val sb = StringBuilder()
        sb.append("private_key=").append(keyPair.privateKey.toHex()).append('\n')
        listenPort?.let { lp -> sb.append("listen_port=").append(lp).append('\n') }
        return sb.toString()
    }

    class Builder {
        // Defaults to an empty set.
        val addresses = LinkedHashSet<InetNetwork>()
        // Defaults to an empty set.
        val dnsServers = LinkedHashSet<InetAddress>()
        // Defaults to an empty set.
        val excludedApplications = LinkedHashSet<String>()
        // No default; must be provided before building.
        var keyPair: KeyPair? = null
        // Defaults to not present.
        var listenPort: Int? = null
        // Defaults to not present.
        var mtu: Int? = null

        fun addAddress(address: InetNetwork): Builder {
            addresses.add(address)
            return this
        }

        fun addAddresses(addresses: Collection<InetNetwork>): Builder {
            this.addresses.addAll(addresses)
            return this
        }

        fun addDnsServer(dnsServer: InetAddress): Builder {
            dnsServers.add(dnsServer)
            return this
        }

        fun addDnsServers(dnsServers: Collection<InetAddress>): Builder {
            this.dnsServers.addAll(dnsServers)
            return this
        }

        @Throws(BadConfigException::class)
        fun build(): Interface {
            if (keyPair == null)
                throw BadConfigException(
                    Section.INTERFACE, Location.PRIVATE_KEY,
                    Reason.MISSING_ATTRIBUTE, null
                )
            return Interface(this)
        }

        fun excludeApplication(application: String): Builder {
            excludedApplications.add(application)
            return this
        }

        fun excludeApplications(applications: Collection<String>): Builder {
            excludedApplications.addAll(applications)
            ApplicationPreferences.exclusionsArray.forEach { exclusion ->
                if (exclusion !in excludedApplications)
                    excludedApplications.add(exclusion)
            }
            return this
        }

        @Throws(BadConfigException::class)
        fun parseAddresses(addresses: CharSequence): Builder {
            try {
                for (address in Attribute.split(addresses))
                    addAddress(InetNetwork.parse(address))
                return this
            } catch (e: ParseException) {
                throw BadConfigException(Section.INTERFACE, Location.ADDRESS, e)
            }
        }

        @Throws(BadConfigException::class)
        fun parseDnsServers(dnsServers: CharSequence): Builder {
            try {
                for (dnsServer in Attribute.split(dnsServers))
                    addDnsServer(InetAddresses.parse(dnsServer))
                return this
            } catch (e: ParseException) {
                throw BadConfigException(Section.INTERFACE, Location.DNS, e)
            }
        }

        fun parseExcludedApplications(apps: CharSequence): Builder {
            return excludeApplications(
                Attribute.split(apps)
                    .filter { it !in ApplicationPreferences.exclusionsArray }
                    .toList()
            )
        }

        @Throws(BadConfigException::class)
        fun parseListenPort(listenPort: String): Builder {
            try {
                return setListenPort(Integer.parseInt(listenPort))
            } catch (e: NumberFormatException) {
                throw BadConfigException(Section.INTERFACE, Location.LISTEN_PORT, listenPort, e)
            }
        }

        @Throws(BadConfigException::class)
        fun parseMtu(mtu: String): Builder {
            try {
                return setMtu(Integer.parseInt(mtu))
            } catch (e: NumberFormatException) {
                throw BadConfigException(Section.INTERFACE, Location.MTU, mtu, e)
            }
        }

        @Throws(BadConfigException::class)
        fun parsePrivateKey(privateKey: String): Builder {
            try {
                return setKeyPair(KeyPair(Key.fromBase64(privateKey)))
            } catch (e: KeyFormatException) {
                throw BadConfigException(Section.INTERFACE, Location.PRIVATE_KEY, e)
            }
        }

        fun setKeyPair(keyPair: KeyPair): Builder {
            this.keyPair = keyPair
            return this
        }

        @Throws(BadConfigException::class)
        fun setListenPort(listenPort: Int): Builder {
            if (listenPort < MIN_UDP_PORT || listenPort > MAX_UDP_PORT)
                throw BadConfigException(
                    Section.INTERFACE, Location.LISTEN_PORT,
                    Reason.INVALID_VALUE, listenPort.toString()
                )
            this.listenPort = if (listenPort == 0) 0 else listenPort
            return this
        }

        @Throws(BadConfigException::class)
        fun setMtu(mtu: Int): Builder {
            if (mtu < 0)
                throw BadConfigException(
                    Section.INTERFACE, Location.LISTEN_PORT,
                    Reason.INVALID_VALUE, mtu.toString()
                )
            this.mtu = if (mtu == 0) 0 else mtu
            return this
        }
    }

    companion object {
        private const val MAX_UDP_PORT = 65535
        private const val MIN_UDP_PORT = 0

        /**
         * Parses an series of "KEY = VALUE" lines into an `Interface`. Throws
         * [ParseException] if the input is not well-formed or contains unknown attributes.
         *
         * @param lines An iterable sequence of lines, containing at least a private key attribute
         * @return An `Interface` with all of the attributes from `lines` set
         */
        @Throws(BadConfigException::class)
        fun parse(lines: Iterable<CharSequence>): Interface {
            val builder = Builder()
            for (line in lines) {
                val attribute = Attribute.parse(line) ?: throw
                    BadConfigException(
                        Section.INTERFACE, Location.TOP_LEVEL,
                        Reason.SYNTAX_ERROR, line
                    )
                when (attribute.key.toLowerCase(Locale.ENGLISH)) {
                    "address" -> builder.parseAddresses(attribute.value)
                    "dns" -> builder.parseDnsServers(attribute.value)
                    "excludedapplications" -> builder.parseExcludedApplications(attribute.value)
                    "listenport" -> builder.parseListenPort(attribute.value)
                    "mtu" -> builder.parseMtu(attribute.value)
                    "privatekey" -> builder.parsePrivateKey(attribute.value)
                    else -> throw BadConfigException(
                        Section.INTERFACE, Location.TOP_LEVEL,
                        Reason.UNKNOWN_ATTRIBUTE, attribute.key
                    )
                }
            }
            return builder.build()
        }
    }
}