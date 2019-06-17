/*
 * Copyright © 2017-2019 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.util

import android.content.Context
import com.wireguard.android.di.ext.getTunnelManager
import org.koin.core.KoinComponent

class ApplicationPreferencesChangeCallback(val context: Context) : KoinComponent {

    fun restart() {
        context.restartApplication()
    }

    fun restartActiveTunnels() {
        getTunnelManager().restartActiveTunnels()
    }
}
