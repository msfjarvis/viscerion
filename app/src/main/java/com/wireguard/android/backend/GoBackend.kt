/*
 * Copyright © 2017-2019 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.backend

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.collection.ArraySet
import com.wireguard.android.R
import com.wireguard.android.activity.MainActivity
import com.wireguard.android.di.getInjector
import com.wireguard.android.model.Tunnel
import com.wireguard.android.model.Tunnel.Statistics
import com.wireguard.android.model.TunnelManager
import com.wireguard.android.util.ApplicationPreferences
import com.wireguard.android.util.ExceptionLoggers
import com.wireguard.android.util.SharedLibraryLoader
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java9.util.concurrent.CompletableFuture
import javax.inject.Inject
import me.msfjarvis.viscerion.config.Config
import me.msfjarvis.viscerion.crypto.Key
import me.msfjarvis.viscerion.crypto.KeyFormatException
import timber.log.Timber

class GoBackend @Inject constructor(
    private val context: Context,
    private val prefs: ApplicationPreferences
) : Backend {

    private var currentTunnel: Tunnel? = null
    private var currentTunnelHandle = -1

    private external fun wgGetConfig(handle: Int): String

    private external fun wgGetSocketV4(handle: Int): Int

    private external fun wgGetSocketV6(handle: Int): Int

    private external fun wgTurnOff(handle: Int)

    private external fun wgTurnOn(ifName: String, tunFd: Int, settings: String): Int

    private external fun wgVersion(): String

    init {
        SharedLibraryLoader.loadSharedLibrary(context, "wg-go")
    }

    override fun applyConfig(tunnel: Tunnel, config: Config): Config {
        if (tunnel.state == Tunnel.State.UP) {
            // Restart the tunnel to apply the new config.
            setStateInternal(tunnel, tunnel.getConfig(), Tunnel.State.DOWN)
            try {
                setStateInternal(tunnel, config, Tunnel.State.UP)
            } catch (e: Exception) {
                // The new configuration didn't work, so try to go back to the old one.
                setStateInternal(tunnel, tunnel.getConfig(), Tunnel.State.UP)
                throw e
            }
        }
        return config
    }

    override fun enumerate(): Set<String> {
        currentTunnel?.let {
            val runningTunnels = ArraySet<String>()
            runningTunnels.add(it.name)
            return runningTunnels
        }
        return emptySet()
    }

    override fun getState(tunnel: Tunnel): Tunnel.State {
        return if (currentTunnel == tunnel) Tunnel.State.UP else Tunnel.State.DOWN
    }

    override fun getStatistics(tunnel: Tunnel): Statistics? {
        val stats = Statistics()
        if (tunnel != currentTunnel) {
            return stats
        }
        val config = wgGetConfig(currentTunnelHandle)
        var key: Key? = null
        var rx: Long = 0
        var tx: Long = 0
        for (line in config.split("\\n").toTypedArray()) {
            if (line.startsWith("public_key=")) {
                if (key != null) {
                    stats.add(key, rx, tx)
                }
                rx = 0
                tx = 0
                key = try {
                    Key.fromHex(line.substring(11))
                } catch (_: KeyFormatException) {
                    null
                }
            } else if (line.startsWith("rx_bytes=")) {
                if (key == null) {
                    continue
                }
                rx = try {
                    line.substring(9).toLong()
                } catch (_: NumberFormatException) {
                    0
                }
            } else if (line.startsWith("tx_bytes=")) {
                if (key == null) {
                    continue
                }
                tx = try {
                    line.substring(9).toLong()
                } catch (_: NumberFormatException) {
                    0
                }
            }
        }
        if (key != null) stats.add(key, rx, tx)
        return stats
    }

    override fun setState(tunnel: Tunnel, state: Tunnel.State): Tunnel.State {
        val originalState = getState(tunnel)
        var finalState = state
        if (state == Tunnel.State.TOGGLE) {
            finalState = if (originalState == Tunnel.State.UP) Tunnel.State.DOWN else Tunnel.State.UP
        }
        if (state == originalState) {
            return originalState
        }
        check(!(state == Tunnel.State.UP && currentTunnel != null)) { context.getString(R.string.multiple_tunnels_error) }
        Timber.d("Changing tunnel ${tunnel.name} to state $finalState ")
        setStateInternal(tunnel, tunnel.getConfig(), finalState)
        return getState(tunnel)
    }

    override fun getVersion(): String {
        return wgVersion()
    }

    override fun getTypePrettyName(): String {
        return context.getString(R.string.type_name_go_userspace)
    }

    override fun postNotification(state: Tunnel.State, tunnel: Tunnel) {
        // Android handles user-facing context for VpnBuilder based interfaces
    }

    @Throws(Exception::class)
    private fun setStateInternal(tunnel: Tunnel?, config: Config?, state: Tunnel.State?) {
        if (state == Tunnel.State.UP) {
            Timber.i("Bringing tunnel up")

            if (config == null) {
                throw NullPointerException(context.getString(R.string.no_config_error))
            }

            if (VpnService.prepare(this.context) != null) {
                throw Exception(context.getString(R.string.vpn_not_authorized_error))
            }

            val service: VpnService
            if (!vpnService.isDone) {
                startVpnService()
            }

            try {
                service = vpnService.get(2, TimeUnit.SECONDS)
            } catch (e: TimeoutException) {
                throw Exception(context.getString(R.string.vpn_start_error), e)
            }

            if (currentTunnelHandle != -1) {
                Timber.w("Tunnel already up")
                return
            }

            // Build config
            val goConfig = config.toWgUserspaceString()

            // Create the vpn tunnel with android API
            val builder = service.getBuilder()
            builder.setSession(tunnel!!.name)

            val configureIntent = Intent(context, MainActivity::class.java)
            configureIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            builder.setConfigureIntent(PendingIntent.getActivity(context, 0, configureIntent, 0))

            // Merge config's excluded applications with global exclusions, then blacklist/whitelist
            // depending on the user's preference.
            val applications = config.interfaze.excludedApplications + prefs.exclusions
            if (prefs.whitelistApps) {
                applications.forEach { builder.addAllowedApplication(it) }
            } else {
                applications.forEach { builder.addDisallowedApplication(it) }
            }

            config.interfaze.addresses.forEach { addr ->
                builder.addAddress(addr.address, addr.mask)
            }

            config.interfaze.dnsServers.forEach { dns ->
                builder.addDnsServer(dns.hostAddress)
            }

            config.peers.forEach { peer ->
                peer.allowedIps.forEach { addr ->
                    builder.addRoute(addr.address, addr.mask)
                }
            }

            if (Build.VERSION.SDK_INT >= 29) {
                builder.setMetered(false)
            }

            var mtu = config.interfaze.mtu
            if (mtu == null || mtu == 0) {
                mtu = 1280
            }
            builder.setMtu(mtu)

            builder.setBlocking(true)
            builder.establish().use { tun ->
                if (tun == null) {
                    throw Exception(context.getString(R.string.tun_create_error))
                }
                Timber.d("Go backend v%s", wgVersion())
                currentTunnelHandle = wgTurnOn(tunnel.name, tun.detachFd(), goConfig)
            }
            if (currentTunnelHandle < 0) {
                throw Exception(context.getString(R.string.tunnel_on_error, currentTunnelHandle))
            }

            currentTunnel = tunnel

            service.protect(wgGetSocketV4(currentTunnelHandle))
            service.protect(wgGetSocketV6(currentTunnelHandle))
        } else {
            Timber.i("Bringing tunnel down")

            if (currentTunnelHandle == -1) {
                Timber.w("Tunnel already down")
                return
            }

            wgTurnOff(currentTunnelHandle)
            currentTunnel = null
            currentTunnelHandle = -1
        }
    }

    private fun startVpnService() {
        Timber.d("Requesting to start VpnService")
        context.startService(Intent(context, VpnService::class.java))
    }

    class VpnService : android.net.VpnService() {

        @Inject lateinit var tunnelManager: TunnelManager

        fun getBuilder(): Builder {
            return Builder()
        }

        override fun onCreate() {
            getInjector(applicationContext).inject(this)
            vpnService.complete(this)
            super.onCreate()
        }

        override fun onDestroy() {
            tunnelManager.getTunnels().thenAccept { tunnels ->
                tunnels.forEach { tunnel ->
                    if (tunnel.state != Tunnel.State.DOWN) {
                        tunnel.setState(Tunnel.State.DOWN)
                    }
                }
            }

            vpnService = vpnService.newIncompleteFuture()
            super.onDestroy()
        }

        override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
            vpnService.complete(this)
            if (intent == null || intent.component == null || intent.component?.packageName != packageName) {
                Timber.d("Service started by Always-on VPN feature")
                tunnelManager.restoreState(true).whenComplete(ExceptionLoggers.D)
            }
            return super.onStartCommand(intent, flags, startId)
        }

        companion object {
            fun prepare(context: Context): Intent? {
                return android.net.VpnService.prepare(context)
            }
        }
    }

    companion object {
        private var vpnService = CompletableFuture<VpnService>()
    }
}
