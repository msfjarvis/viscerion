/*
 * Copyright © 2017-2018 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
@file:Suppress("Unused")

package com.wireguard.android.widget.fab

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.snackbar.Snackbar

class FloatingActionButtonBehavior(
    context: Context,
    attrs: AttributeSet
) : CoordinatorLayout.Behavior<FloatingActionsMenu>(context, attrs) {

    override fun layoutDependsOn(
        parent: CoordinatorLayout,
        child: FloatingActionsMenu,
        dependency: View
    ): Boolean {
        return dependency is Snackbar.SnackbarLayout
    }

    override fun onDependentViewChanged(
        parent: CoordinatorLayout,
        child: FloatingActionsMenu,
        dependency: View
    ): Boolean {
        child.setBehaviorYTranslation(Math.min(0f, dependency.translationY - dependency.measuredHeight))
        return true
    }

    override fun onDependentViewRemoved(
        parent: CoordinatorLayout,
        child: FloatingActionsMenu,
        dependency: View
    ) {
        child.setBehaviorYTranslation(0f)
    }
}
