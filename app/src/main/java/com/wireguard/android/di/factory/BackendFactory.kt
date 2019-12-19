/*
 * Copyright © 2017-2019 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.di.factory

import android.content.Context
import com.wireguard.android.backend.Backend
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.WgQuickBackend
import com.wireguard.android.util.ApplicationPreferences
import com.wireguard.android.util.RootShell
import com.wireguard.android.util.ToolsInstaller
import java.io.File

object BackendFactory {
    fun getBackend(
        context: Context,
        prefs: ApplicationPreferences,
        rootShell: RootShell,
        toolsInstaller: ToolsInstaller
    ): Backend {
        var ret: Backend? = null
        if (File("/sys/module/wireguard").exists()) {
            try {
                if (prefs.forceUserspaceBackend) {
                    throw Exception("Forcing userspace backend on user request.")
                }
                rootShell.start()
                ret = WgQuickBackend(context, toolsInstaller, rootShell)
            } catch (_: Exception) {
            }
        }
        if (ret == null) {
            ret = GoBackend(context, prefs)
        }
        return ret
    }
}
