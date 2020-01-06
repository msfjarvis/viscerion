/*
 * Copyright © 2017-2020 WireGuard LLC.
 * Copyright © 2018-2020 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.widget

import android.content.Context
import android.os.Parcelable
import android.util.AttributeSet
import androidx.appcompat.widget.SwitchCompat

class ToggleSwitch(context: Context, attrs: AttributeSet? = null) : SwitchCompat(context, attrs) {
    private var isRestoringState: Boolean = false
    private var listener: OnBeforeCheckedChangeListener? = null

    override fun onRestoreInstanceState(state: Parcelable) {
        isRestoringState = true
        super.onRestoreInstanceState(state)
        isRestoringState = false
    }

    override fun setChecked(checked: Boolean) {
        if (checked == isChecked) {
            return
        }
        if (isRestoringState || listener == null) {
            super.setChecked(checked)
            return
        }
        isEnabled = false
        listener?.onBeforeCheckedChanged(this, checked)
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
