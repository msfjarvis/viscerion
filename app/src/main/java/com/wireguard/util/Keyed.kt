/*
 * Copyright © 2017-2018 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.wireguard.util

/**
 * Interface for objects that have a identifying key of the given type.
 */

interface Keyed<K> {
    val key: K
}
