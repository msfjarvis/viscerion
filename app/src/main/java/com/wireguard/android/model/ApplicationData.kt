/*
 * Copyright © 2018 Eric Kuck <eric@bluelinelabs.com>.
 * Copyright © 2018 Jason A. Donenfeld <Jason@zx2c4.com>. All Rights Reserved.
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

    var isExcludedFromTunnel: Boolean
        @Bindable
        get() = if (globallyExcluded) true else excludedFromTunnel
        set(excludedFromTunnel) {
            if (!isGloballyExcluded) {
                this.excludedFromTunnel = excludedFromTunnel
                notifyPropertyChanged(BR.excludedFromTunnel)
            }
        }


    var isGloballyExcluded: Boolean = false
        @Bindable
        get() = globallyExcluded

    override fun getKey(): String {
        return name
    }
}
