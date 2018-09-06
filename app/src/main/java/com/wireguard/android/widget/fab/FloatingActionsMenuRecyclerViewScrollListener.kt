/*
 * Copyright © 2018 Jason A. Donenfeld <Jason@zx2c4.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.wireguard.android.widget.fab

import androidx.recyclerview.widget.RecyclerView

class FloatingActionsMenuRecyclerViewScrollListener(private val menu: FloatingActionsMenu) :
    RecyclerView.OnScrollListener() {

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)
        menu.scrollYTranslation =
            bound(0f, menu.scrollYTranslation + dy * SCALE_FACTOR, menu.measuredHeight - menu.translationY)
    }

    companion object {
        private val SCALE_FACTOR = 1.5f

        private fun bound(min: Float, proposal: Float, max: Float): Float {
            return Math.min(max, Math.max(min, proposal))
        }
    }
}
