/*
 * Copyright © 2018 Eric Kuck <eric@bluelinelabs.com>.
 * Copyright © 2018 Jason A. Donenfeld <Jason@zx2c4.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.wireguard.android.model

import android.graphics.drawable.Drawable

import com.wireguard.android.BR
import com.wireguard.util.Keyed

import androidx.databinding.BaseObservable
import androidx.databinding.Bindable

class ApplicationData(
        val icon: Drawable,
        val name: String,
        val packageName: String,
        private var excludedFromTunnel: Boolean)
    : BaseObservable(), Keyed<String> {

    var isExcludedFromTunnel: Boolean
        @Bindable
        get() = excludedFromTunnel
        set(excludedFromTunnel) {
            this.excludedFromTunnel = excludedFromTunnel
            notifyPropertyChanged(BR.excludedFromTunnel)
        }

    override fun getKey(): String {
        return name
    }
}
