/*
 * Copyright © 2017-2020 WireGuard LLC.
 * Copyright © 2018-2020 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package me.msfjarvis.viscerion.config

import java.net.Inet4Address
import java.net.InetAddress

/**
 * An Internet network, denoted by its address and netmask
 *
 *
 * Instances of this class are immutable.
 */
class InetNetwork private constructor(val address: InetAddress, val mask: Int) {

    override fun equals(other: Any?): Boolean {
        if (other !is InetNetwork) {
            return false
        }
        return address == other.address && mask == other.mask
    }

    override fun hashCode(): Int {
        return address.hashCode() xor mask
    }

    override fun toString(): String {
        return address.hostAddress + '/'.toString() + mask
    }

    companion object {

        @Throws(ParseException::class)
        fun parse(network: String): InetNetwork {
            val slash = network.lastIndexOf('/')
            val maskString: String
            val rawMask: Int
            val rawAddress: String
            if (slash >= 0) {
                maskString = network.substring(slash + 1)
                try {
                    rawMask = Integer.parseInt(maskString, 10)
                } catch (_: NumberFormatException) {
                    throw ParseException(Int::class.java, maskString)
                }

                rawAddress = network.substring(0, slash)
            } else {
                maskString = ""
                rawMask = -1
                rawAddress = network
            }
            val address = InetAddressUtils.parse(rawAddress)
            val maxMask = if (address is Inet4Address) {
                32
            } else {
                128
            }
            if (rawMask > maxMask) {
                throw ParseException(InetNetwork::class.java, maskString, "Invalid network mask")
            }
            val mask = if (rawMask in 0..maxMask) {
                rawMask
            } else {
                maxMask
            }
            return InetNetwork(address, mask)
        }
    }
}
