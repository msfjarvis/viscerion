/*
 * Copyright © 2017-2020 WireGuard LLC.
 * Copyright © 2018-2020 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.model

import android.graphics.drawable.Drawable
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import com.wireguard.android.BR
import com.wireguard.util.Keyed

class ApplicationData(
    val icon: Drawable,
    val name: String,
    val packageName: String,
    private var excludedFromTunnel: Boolean,
    private var globallyExcluded: Boolean = false
) : BaseObservable(), Keyed<String> {
    override val key: String
        get() = name

    var isExcludedFromTunnel: Boolean
        @Bindable
        get() = if (globallyExcluded) true else excludedFromTunnel
        set(excludedFromTunnel) {
            if (!isGloballyExcluded) {
                this.excludedFromTunnel = excludedFromTunnel
                notifyPropertyChanged(BR.excludedFromTunnel)
            }
        }

    val isGloballyExcluded: Boolean
        @Bindable
        get() = globallyExcluded
}
