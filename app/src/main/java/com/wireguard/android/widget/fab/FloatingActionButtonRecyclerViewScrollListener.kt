/*
 * Copyright © 2018 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.wireguard.android.widget.fab

import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class FloatingActionButtonRecyclerViewScrollListener(private val fab: FloatingActionButton) : RecyclerView.OnScrollListener() {

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)
        if (dy > 0 && fab.isOrWillBeShown) {
            fab.hide()
        } else if (dy < 0 && fab.isOrWillBeHidden) {
            fab.show()
        }
    }
}
