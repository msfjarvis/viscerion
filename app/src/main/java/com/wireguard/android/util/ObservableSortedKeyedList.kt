/*
 * Copyright © 2019 Harsh Shandilya. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.util

import com.wireguard.util.Keyed
import com.wireguard.util.SortedKeyedList

/**
 * A list that is both sorted/keyed and observable.
 */

interface ObservableSortedKeyedList<K, E : Keyed<out K>> : ObservableKeyedList<K, E>, SortedKeyedList<K, E>
