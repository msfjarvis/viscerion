/*
 * Copyright © 2017-2018 WireGuard LLC.
 * Copyright © 2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
@file:Suppress("Unused")

package com.wireguard.android.widget.fab

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar

class FloatingActionButtonBehavior(
    context: Context,
    attrs: AttributeSet
) : CoordinatorLayout.Behavior<FloatingActionButton>(context, attrs) {

    override fun layoutDependsOn(
        parent: CoordinatorLayout,
        child: FloatingActionButton,
        dependency: View
    ): Boolean {
        return dependency is Snackbar.SnackbarLayout
    }

    override fun onDependentViewChanged(
        parent: CoordinatorLayout,
        child: FloatingActionButton,
        dependency: View
    ): Boolean {
        child.translationY = Math.min(0f, dependency.translationY - dependency.measuredHeight)
        return true
    }

    override fun onDependentViewRemoved(
        parent: CoordinatorLayout,
        child: FloatingActionButton,
        dependency: View
    ) {
        child.translationY = 0f
    }
}
