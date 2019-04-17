/*
 * Copyright © 2017-2018 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.di.factory

import android.content.Context
import com.wireguard.android.util.BackendAsync

class CompletableBackendFactory(context: Context) {
    val backendAsync = BackendAsync()

    init {
        backendAsync.complete(BackendFactory(context).backend)
    }
}
