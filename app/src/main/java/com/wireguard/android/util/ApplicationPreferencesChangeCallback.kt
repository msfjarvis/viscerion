/*
 * Copyright © 2017-2018 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.util

import android.content.Context
import com.wireguard.android.model.TunnelManager
import org.koin.core.KoinComponent
import org.koin.core.inject

class ApplicationPreferencesChangeCallback(val context: Context) : KoinComponent {

    fun restart() {
        context.restartApplication()
    }

    fun restartActiveTunnels() {
        inject<TunnelManager>().value.restartActiveTunnels()
    }
}
