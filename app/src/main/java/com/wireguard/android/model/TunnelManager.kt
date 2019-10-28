/*
 * Copyright © 2017-2019 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.model

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import com.wireguard.android.BR
import com.wireguard.android.BuildConfig
import com.wireguard.android.R
import com.wireguard.android.di.ext.getConfigStore
import com.wireguard.android.di.ext.getPrefs
import com.wireguard.android.di.ext.getTunnelManager
import com.wireguard.android.di.ext.injectAsyncWorker
import com.wireguard.android.di.ext.injectBackend
import com.wireguard.android.model.Tunnel.Statistics
import com.wireguard.android.util.ExceptionLoggers
import com.wireguard.android.util.KotlinCompanions
import com.wireguard.android.util.ObservableSortedKeyedArrayList
import com.wireguard.android.util.ObservableSortedKeyedList
import com.wireguard.config.Config
import java9.util.Comparators
import java9.util.concurrent.CompletableFuture
import java9.util.concurrent.CompletionStage
import org.koin.core.KoinComponent
import timber.log.Timber

class TunnelManager(private val context: Context) : BaseObservable(), KoinComponent {

    private val completableTunnels = CompletableFuture<ObservableSortedKeyedList<String, Tunnel>>()
    private val tunnels = ObservableSortedKeyedArrayList<String, Tunnel>(COMPARATOR)
    private val delayedLoadRestoreTunnels = ArrayList<CompletableFuture<Void>>()
    private val configStore = getConfigStore()
    private val prefs = getPrefs()
    private var haveLoaded: Boolean = false

    init {
        asyncWorker.supplyAsync {
            configStore.enumerate()
        }.thenAcceptBoth(asyncWorker.supplyAsync {
            backend.enumerate()
        }) { present, running ->
            this.onTunnelsLoaded(present, running)
        }.whenComplete(ExceptionLoggers.E)
    }

    private fun addToList(name: String, config: Config?, state: Tunnel.State): Tunnel {
        val tunnel = Tunnel(this, name, config, state)
        tunnels.add(tunnel)
        return tunnel
    }

    fun create(name: String, config: Config?): CompletionStage<Tunnel> {
        if (Tunnel.isNameInvalid(name))
            return CompletableFuture.failedFuture(IllegalArgumentException(context.getString(R.string.tunnel_error_invalid_name)))
        if (tunnels.containsKey(name)) {
            val message = context.getString(R.string.tunnel_error_already_exists, name)
            return CompletableFuture.failedFuture(IllegalArgumentException(message))
        }
        return asyncWorker.supplyAsync { config?.let { configStore.create(name, it) } }
                .thenApply { savedConfig -> addToList(name, savedConfig, Tunnel.State.DOWN) }
    }

    internal fun delete(tunnel: Tunnel): CompletionStage<Void> {
        val originalState = tunnel.state
        val wasLastUsed = tunnel == lastUsedTunnel
        // Make sure nothing touches the tunnel.
        if (wasLastUsed)
            setLastUsedTunnel(null)
        tunnels.remove(tunnel)
        return asyncWorker.runAsync {
            if (originalState == Tunnel.State.UP)
                backend.setState(tunnel, Tunnel.State.DOWN)
            try {
                configStore.delete(tunnel.name)
            } catch (e: Exception) {
                if (originalState == Tunnel.State.UP)
                    backend.setState(tunnel, Tunnel.State.UP)
                // Re-throw the exception to fail the completion.
                throw e
            }
        }.whenComplete { _, e ->
            if (e == null)
                return@whenComplete
            // Failure, put the tunnel back.
            tunnels.add(tunnel)
            if (wasLastUsed)
                setLastUsedTunnel(tunnel)
        }
    }

    @Bindable
    fun getLastUsedTunnel(): Tunnel? {
        return lastUsedTunnel
    }

    private fun setLastUsedTunnel(tunnel: Tunnel?) {
        if (tunnel == lastUsedTunnel)
            return
        lastUsedTunnel = tunnel
        notifyPropertyChanged(BR.lastUsedTunnel)
        prefs.lastUsedTunnel = tunnel?.name ?: ""
    }

    internal fun getTunnelConfig(tunnel: Tunnel): CompletionStage<Config> {
        return asyncWorker.supplyAsync { configStore.load(tunnel.name) }
                .thenApply(tunnel::onConfigChanged)
    }

    fun getTunnels(): CompletableFuture<ObservableSortedKeyedList<String, Tunnel>> {
        return completableTunnels
    }

    private fun onTunnelsLoaded(present: Iterable<String>, running: Collection<String>) {
        for (name in present)
            addToList(name, null, if (running.contains(name)) Tunnel.State.UP else Tunnel.State.DOWN)
        val lastUsedName = prefs.lastUsedTunnel
        if (lastUsedName.isNotEmpty())
            setLastUsedTunnel(tunnels[lastUsedName])
        var toComplete: Array<CompletableFuture<Void>>?
        synchronized(delayedLoadRestoreTunnels) {
            haveLoaded = true
            toComplete = delayedLoadRestoreTunnels.toTypedArray()
            delayedLoadRestoreTunnels.clear()
        }
        restoreState(true).whenComplete { v, t ->
            toComplete?.let {
                it.forEach { future ->
                    if (t == null)
                        future.complete(v)
                    else
                        future.completeExceptionally(t)
                }
            }
        }
        completableTunnels.complete(tunnels)
    }

    fun refreshTunnelStates() {
        asyncWorker.supplyAsync {
            backend.enumerate()
        }.thenAccept { running ->
            tunnels.forEach { tunnel ->
                val state = if (running?.contains(tunnel.name) == true) Tunnel.State.UP else Tunnel.State.DOWN
                tunnel.onStateChanged(state)
                backend.postNotification(state, tunnel)
            }
        }.whenComplete(ExceptionLoggers.E)
    }

    fun restoreState(force: Boolean): CompletionStage<Void> {
        if (!force && !prefs.restoreOnBoot)
            return CompletableFuture.completedFuture(null)
        synchronized(delayedLoadRestoreTunnels) {
            if (!haveLoaded) {
                val f = CompletableFuture<Void>()
                delayedLoadRestoreTunnels.add(f)
                return f
            }
        }
        val previouslyRunning = prefs.runningTunnels
        return KotlinCompanions.streamForStateChange(tunnels, previouslyRunning, this)
    }

    fun saveState() {
        context.contentResolver.notifyChange(
                Uri.parse("content://${BuildConfig.APPLICATION_ID}/vpn"),
                null
        )
        prefs.runningTunnels =
                tunnels.asSequence().filter { it.state == Tunnel.State.UP }.map { it.name }.toSet()
    }

    fun restartActiveTunnels() {
        completableTunnels.thenAccept { tunnels ->
            tunnels.forEach { tunnel ->
                if (tunnel.state == Tunnel.State.UP)
                    tunnel.setState(Tunnel.State.DOWN).whenComplete { _, _ -> tunnel.setState(Tunnel.State.UP) }
            }
        }
    }

    internal fun setTunnelConfig(tunnel: Tunnel, config: Config): CompletionStage<Config> {
        return asyncWorker.supplyAsync {
            val appliedConfig = backend.applyConfig(tunnel, config)
            configStore.save(tunnel.name, appliedConfig)
        }.thenApply(tunnel::onConfigChanged)
    }

    internal fun setTunnelName(tunnel: Tunnel, name: String): CompletionStage<String> {
        if (Tunnel.isNameInvalid(name))
            return CompletableFuture.failedFuture(IllegalArgumentException(context.getString(R.string.tunnel_error_invalid_name)))
        if (tunnels.containsKey(name)) {
            val message = context.getString(R.string.tunnel_error_already_exists, name)
            return CompletableFuture.failedFuture(IllegalArgumentException(message))
        }
        val originalState = tunnel.state
        val wasLastUsed = tunnel == lastUsedTunnel
        // Make sure nothing touches the tunnel.
        if (wasLastUsed)
            setLastUsedTunnel(null)
        tunnels.remove(tunnel)
        return asyncWorker.supplyAsync {
            if (originalState == Tunnel.State.UP)
                backend.setState(tunnel, Tunnel.State.DOWN)
            configStore.rename(tunnel.name, name)
            val newName = tunnel.onNameChanged(name)
            if (originalState == Tunnel.State.UP)
                backend.setState(tunnel, Tunnel.State.UP)
            newName
        }.whenComplete { _, e ->
            // On failure, we don't know what state the tunnel might be in. Fix that.
            if (e != null)
                getTunnelState(tunnel)
            // Add the tunnel back to the manager, under whatever name it thinks it has.
            tunnels.add(tunnel)
            if (wasLastUsed)
                setLastUsedTunnel(tunnel)
        }
    }

    fun setTunnelState(tunnel: Tunnel, state: Tunnel.State): CompletionStage<Tunnel.State> {
        // Ensure the configuration is loaded before trying to use it.
        return tunnel.configAsync.thenCompose {
            asyncWorker.supplyAsync {
                backend.setState(
                        tunnel,
                        state
                )
            }
        }.whenComplete { newState, e ->
            // Ensure onStateChanged is always called (failure or not), and with the correct state.
            tunnel.onStateChanged(if (e == null) newState else tunnel.state)
            if (e == null && newState == Tunnel.State.UP)
                setLastUsedTunnel(tunnel)
            saveState()
        }
    }

    class IntentReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null || intent.action == null)
                return
            when (intent.action) {
                "com.wireguard.android.action.REFRESH_TUNNEL_STATES" -> {
                    getTunnelManager().refreshTunnelStates()
                    return
                }
                else -> Timber.tag("TunnelManager").d("Invalid intent action: ${intent.action}")
            }
        }
    }

    companion object : KoinComponent {
        const val NOTIFICATION_CHANNEL_ID = "wg-quick_tunnels"
        const val TUNNEL_NAME_INTENT_EXTRA = "tunnel_name"
        const val INTENT_INTEGRATION_SECRET_EXTRA = "integration_secret"
        const val TUNNEL_STATE_INTENT_EXTRA = "tunnel_state"
        private val asyncWorker by injectAsyncWorker()
        private val backend by injectBackend()

        private val COMPARATOR = Comparators.thenComparing(
                String.CASE_INSENSITIVE_ORDER, Comparators.naturalOrder()
        )
        private var lastUsedTunnel: Tunnel? = null

        internal fun getTunnelState(tunnel: Tunnel): CompletionStage<Tunnel.State> {
            return asyncWorker.supplyAsync { backend.getState(tunnel) }
                    .thenApply(tunnel::onStateChanged)
        }

        fun getTunnelStatistics(tunnel: Tunnel): CompletionStage<Statistics> {
            return asyncWorker.supplyAsync { backend.getStatistics(tunnel) }
                    .thenApply(tunnel::onStatisticsChanged)
        }
    }
}
