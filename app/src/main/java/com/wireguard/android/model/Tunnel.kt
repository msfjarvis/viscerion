/*
 * Copyright © 2017-2018 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
// For statistics
@file:Suppress("Unused")

package com.wireguard.android.model

import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import com.wireguard.android.BR
import com.wireguard.android.util.ExceptionLoggers
import com.wireguard.config.Config
import com.wireguard.util.Keyed
import java9.util.concurrent.CompletableFuture
import java9.util.concurrent.CompletionStage
import java.util.Locale
import java.util.regex.Pattern

/**
 * Encapsulates the volatile and nonvolatile state of a WireGuard tunnel.
 */

class Tunnel internal constructor(
    private val manager: TunnelManager,
    @Bindable
    var name: String,
    private var config: Config?,
    @Bindable
    var state: State?
) : BaseObservable(), Keyed<String> {
    override val key: String
        get() = name

    private var statistics: Statistics? = null

    val configAsync: CompletionStage<Config>
        get() = if (config == null) manager.getTunnelConfig(this) else CompletableFuture.completedFuture(config)

    val stateAsync: CompletionStage<State>
        get() = TunnelManager.getTunnelState(this)

    // FIXME: Check age of statistics.
    val statisticsAsync: CompletionStage<Statistics>
        get() = if (statistics == null) TunnelManager.getTunnelStatistics(this) else CompletableFuture.completedFuture(
            statistics
        )

    fun delete(): CompletionStage<Void> {
        return manager.delete(this)
    }

    @Bindable
    fun getConfig(): Config? {
        if (config == null)
            manager.getTunnelConfig(this).whenComplete(ExceptionLoggers.E)
        return config
    }

    @Bindable
    fun getStatistics(): Statistics? {
        // FIXME: Check age of statistics.
        if (statistics == null)
            TunnelManager.getTunnelStatistics(this).whenComplete(ExceptionLoggers.E)
        return statistics
    }

    fun onConfigChanged(config: Config): Config {
        this.config = config
        notifyPropertyChanged(BR.config)
        return config
    }

    fun onNameChanged(name: String): String {
        this.name = name
        notifyPropertyChanged(BR.name)
        return name
    }

    fun onStateChanged(state: State?): State? {
        if (state != State.UP)
            onStatisticsChanged(null)
        this.state = state
        notifyPropertyChanged(BR.state)
        return state
    }

    fun onStatisticsChanged(statistics: Statistics?): Statistics? {
        this.statistics = statistics
        notifyPropertyChanged(BR.statistics)
        return statistics
    }

    fun setConfig(config: Config): CompletionStage<Config> {
        return if (config != this.config) manager.setTunnelConfig(this, config) else CompletableFuture.completedFuture(
            this.config
        )
    }

    fun setName(name: String): CompletionStage<String> {
        return if (name != this.name) manager.setTunnelName(
            this,
            name
        ) else CompletableFuture.completedFuture(this.name)
    }

    fun setState(state: State): CompletionStage<State> {
        return if (state != this.state) manager.setTunnelState(
            this,
            state
        ) else CompletableFuture.completedFuture(this.state)
    }

    enum class State {
        DOWN,
        TOGGLE,
        UP;

        override fun toString(): String {
            return super.toString().toLowerCase(Locale.ENGLISH)
        }

        companion object {
            fun of(running: Boolean): State {
                return if (running) UP else DOWN
            }
        }
    }

    class Statistics : BaseObservable()

    companion object {
        const val NAME_MAX_LENGTH = 15
        private val NAME_PATTERN = Pattern.compile("[a-zA-Z0-9_=+.-]{1,15}")

        fun isNameInvalid(name: CharSequence): Boolean {
            return !NAME_PATTERN.matcher(name).matches()
        }
    }
}
