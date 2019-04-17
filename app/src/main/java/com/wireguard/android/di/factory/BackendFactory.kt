/*
 * Copyright © 2017-2018 WireGuard LLC.
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
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.io.File

class BackendFactory(context: Context) : KoinComponent {
    val backend: Backend

    init {
        var ret: Backend? = null
        if (File("/sys/module/wireguard").exists()) {
            try {
                if (inject<ApplicationPreferences>().value.forceUserspaceBackend)
                    throw Exception("Forcing userspace backend on user request.")
                inject<RootShell>().value.start()
                ret = WgQuickBackend(context)
            } catch (ignored: Exception) {
            }
        }
        if (ret == null)
            ret = GoBackend(context)
        backend = ret
    }
}
