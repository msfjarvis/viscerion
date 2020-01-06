/*
 * Copyright © 2017-2020 WireGuard LLC.
 * Copyright © 2018-2020 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.util

import com.wireguard.util.Keyed
import com.wireguard.util.SortedKeyedList

/**
 * A list that is both sorted/keyed and observable.
 */

interface ObservableSortedKeyedList<K, E : Keyed<out K>> : ObservableKeyedList<K, E>, SortedKeyedList<K, E>
