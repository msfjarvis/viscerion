/*
 * Copyright © 2018 Samuel Holland <samuel@sholland.org>
 * Copyright © 2018 Jason A. Donenfeld <Jason@zx2c4.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.wireguard.config

import java.net.Inet4Address
import java.net.InetAddress

class InetNetwork internal constructor(input: String) {
    val address: InetAddress
    val mask: Int

    init {
        val slash = input.lastIndexOf('/')
        val rawMask: Int
        val rawAddress: String
        if (slash >= 0) {
            rawMask = Integer.parseInt(input.substring(slash + 1), 10)
            rawAddress = input.substring(0, slash)
        } else {
            rawMask = -1
            rawAddress = input
        }
        address = InetAddresses.parse(rawAddress)
        val maxMask = if (address is Inet4Address) 32 else 128
        mask = if (rawMask in 0..maxMask) rawMask else maxMask
    }

    override fun equals(other: Any?): Boolean {
        if (other !is InetNetwork)
            return false
        val network = other as InetNetwork?
        return address == network?.address && mask == network.mask
    }

    override fun hashCode(): Int {
        return address.hashCode() xor mask
    }

    override fun toString(): String {
        return address.hostAddress + '/'.toString() + mask
    }
}
