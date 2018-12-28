/*
 * Copyright © 2017-2018 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.wireguard.android.util

import androidx.databinding.ObservableList
import com.wireguard.util.Keyed
import com.wireguard.util.KeyedList

/**
 * A list that is both keyed and observable.
 */

interface ObservableKeyedList<K, E : Keyed<out K>> : KeyedList<K, E>, ObservableList<E>
