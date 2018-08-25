/*
 * Copyright © 2018 Samuel Holland <samuel@sholland.org>
 * Copyright © 2018 Jason A. Donenfeld <Jason@zx2c4.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.wireguard.android.activity

import android.os.Bundle
import androidx.annotation.Nullable
import com.wireguard.android.fragment.TunnelEditorFragment
import com.wireguard.android.model.Tunnel

/**
 * Standalone activity for creating tunnels.
 */

class TunnelCreatorActivity : BaseActivity() {
    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (supportFragmentManager.findFragmentById(android.R.id.content) == null) {
            supportFragmentManager.beginTransaction()
                .add(android.R.id.content, TunnelEditorFragment())
                .commit()
        }
    }

    override fun onSelectedTunnelChanged(@Nullable oldTunnel: Tunnel?, @Nullable newTunnel: Tunnel?) {
        finish()
    }
}
