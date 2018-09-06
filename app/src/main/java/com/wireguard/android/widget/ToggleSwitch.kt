/*
 * Copyright © 2013 The Android Open Source Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.wireguard.android.widget

import android.content.Context
import android.os.Parcelable
import android.util.AttributeSet
import android.widget.Switch

class ToggleSwitch(context: Context, attrs: AttributeSet?) : Switch(context, attrs) {
    private var isRestoringState: Boolean = false
    private var listener: OnBeforeCheckedChangeListener? = null

    constructor(context: Context) : this(context, null) {}

    override fun onRestoreInstanceState(state: Parcelable) {
        isRestoringState = true
        super.onRestoreInstanceState(state)
        isRestoringState = false
    }

    override fun setChecked(checked: Boolean) {
        if (checked == isChecked)
            return
        if (isRestoringState || listener == null) {
            super.setChecked(checked)
            return
        }
        isEnabled = false
        listener!!.onBeforeCheckedChanged(this, checked)
    }

    fun setCheckedInternal(checked: Boolean) {
        super.setChecked(checked)
        isEnabled = true
    }

    fun setOnBeforeCheckedChangeListener(listener: OnBeforeCheckedChangeListener?) {
        this.listener = listener
    }

    interface OnBeforeCheckedChangeListener {
        fun onBeforeCheckedChanged(toggleSwitch: ToggleSwitch, checked: Boolean)
    }
}
