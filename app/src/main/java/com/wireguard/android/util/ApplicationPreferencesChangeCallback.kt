/*
 * Copyright © 2017-2019 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.util

import android.content.Context
import com.wireguard.android.model.TunnelManager

class ApplicationPreferencesChangeCallback(
    val context: Context,
    val tunnelManager: TunnelManager
) {

    fun restart() {
        context.restartApplication()
    }

    fun restartActiveTunnels() {
        tunnelManager.restartActiveTunnels()
    }
}
