/*
 * Copyright © 2019 Harsh Shandilya. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.crypto

/**
 * Represents a Curve25519 key pair as used by WireGuard.
 *
 *
 * Instances of this class are immutable.
 */
class KeyPair
/**
 * Creates a key pair using an existing private key.
 *
 * @param privateKey a private key, used to derive the public key
 */
@JvmOverloads constructor(
    /**
     * Returns the private key from the key pair.
     *
     * @return the private key
     */
    val privateKey: Key = Key.generatePrivateKey()
) {
    /**
     * Returns the public key from the key pair.
     *
     * @return the public key
     */
    val publicKey: Key = Key.generatePublicKey(privateKey)
}
/**
 * Creates a key pair using a newly-generated private key.
 */
