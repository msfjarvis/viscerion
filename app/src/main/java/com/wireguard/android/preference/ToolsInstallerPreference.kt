/*
 * Copyright © 2017-2018 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.preference

import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference
import com.wireguard.android.R
import com.wireguard.android.util.AsyncWorker
import com.wireguard.android.util.ToolsInstaller
import org.koin.core.KoinComponent
import org.koin.core.inject

/**
 * Preference implementing a button that asynchronously runs `ToolsInstaller` and displays the
 * result as the preference summary.
 */

class ToolsInstallerPreference(context: Context, attrs: AttributeSet) : Preference(context, attrs), KoinComponent {
    private var state = State.INITIAL
    private val asyncWorker by inject<AsyncWorker>()
    private val toolsInstaller by inject<ToolsInstaller>()

    override fun getSummary(): CharSequence {
        return context.getString(state.messageResourceId)
    }

    override fun getTitle(): CharSequence {
        return context.getString(R.string.tools_installer_title)
    }

    override fun onAttached() {
        super.onAttached()
        asyncWorker.supplyAsync<Int> {
            toolsInstaller.areInstalled()
        }.whenComplete(this::onCheckResult)
    }

    private fun onCheckResult(state: Int, throwable: Throwable?) {
        if (throwable != null || state == ToolsInstaller.ERROR)
            setState(State.INITIAL)
        else if (state and ToolsInstaller.YES == ToolsInstaller.YES)
            setState(State.ALREADY)
        else if ((state and (ToolsInstaller.MAGISK or ToolsInstaller.NO)) == (ToolsInstaller.MAGISK or ToolsInstaller.NO))
            setState(State.INITIAL_MAGISK)
        else if ((state and (ToolsInstaller.SYSTEM or ToolsInstaller.NO)) == (ToolsInstaller.SYSTEM or ToolsInstaller.NO))
            setState(State.INITIAL_SYSTEM)
        else
            setState(State.INITIAL)
    }

    override fun onClick() {
        setState(State.WORKING)
        asyncWorker.supplyAsync<Int> {
            toolsInstaller.install()
        }.whenComplete(this::onInstallResult)
    }

    private fun onInstallResult(result: Int, throwable: Throwable?) {
        when {
            throwable != null -> setState(State.FAILURE)
            result and ((ToolsInstaller.YES or ToolsInstaller.MAGISK)) == (ToolsInstaller.YES or ToolsInstaller.MAGISK) -> setState(
                    State.SUCCESS_MAGISK
            )
            result and (ToolsInstaller.YES or ToolsInstaller.SYSTEM) == (ToolsInstaller.YES or ToolsInstaller.SYSTEM) -> setState(
                    State.SUCCESS_SYSTEM
            )
            else -> setState(State.FAILURE)
        }
    }

    private fun setState(state: State) {
        if (this.state == state)
            return
        this.state = state
        if (isEnabled != state.shouldEnableView)
            isEnabled = state.shouldEnableView
        notifyChanged()
    }

    private enum class State(val messageResourceId: Int, val shouldEnableView: Boolean) {
        INITIAL(R.string.tools_installer_initial, true),
        ALREADY(R.string.tools_installer_already, false),
        FAILURE(R.string.tools_installer_failure, true),
        WORKING(R.string.tools_installer_working, false),
        INITIAL_SYSTEM(R.string.tools_installer_initial_system, true),
        SUCCESS_SYSTEM(R.string.tools_installer_success_system, false),
        INITIAL_MAGISK(R.string.tools_installer_initial_magisk, true),
        SUCCESS_MAGISK(R.string.tools_installer_success_magisk, false)
    }
}
