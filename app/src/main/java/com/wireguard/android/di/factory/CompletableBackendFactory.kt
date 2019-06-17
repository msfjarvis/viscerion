/*
 * Copyright © 2017-2019 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.di.factory

import com.wireguard.android.backend.Backend
import com.wireguard.android.util.BackendAsync

object CompletableBackendFactory {
    fun getBackendAsync(backend: Backend): BackendAsync {
        val backendAsync = BackendAsync()
        backendAsync.complete(backend)
        return backendAsync
    }
}
