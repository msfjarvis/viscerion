/*
 * Copyright © 2017-2018 WireGuard LLC.
 * Copyright © 2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.backend

import com.wireguard.android.model.Tunnel
import com.wireguard.android.model.Tunnel.State
import com.wireguard.android.model.Tunnel.Statistics
import com.wireguard.config.Config

/**
 * Interface for implementations of the WireGuard secure network tunnel.
 */

interface Backend {

    /**
     * Update the volatile configuration of a running tunnel and return the resulting configuration.
     * If the tunnel is not up, return the configuration that would result (if known), or else
     * simply return the given configuration.
     *
     * @param tunnel The tunnel to apply the configuration to.
     * @param config The new configuration for this tunnel.
     * @return The updated configuration of the tunnel.
     */
    @Throws(Exception::class)
    fun applyConfig(tunnel: Tunnel, config: Config): Config

    /**
     * Enumerate the names of currently-running tunnels.
     *
     * @return The set of running tunnel names.
     */
    fun enumerate(): Set<String>

    /**
     * Get the actual state of a tunnel.
     *
     * @param tunnel The tunnel to examine the state of.
     * @return The state of the tunnel.
     */
    @Throws(Exception::class)
    fun getState(tunnel: Tunnel): State

    /**
     * Get statistics about traffic and errors on this tunnel. If the tunnel is not running, the
     * statistics object will be filled with zero values.
     *
     * @param tunnel The tunnel to retrieve statistics for.
     * @return The statistics for the tunnel.
     */
    @Throws(Exception::class)
    fun getStatistics(tunnel: Tunnel): Statistics?

    /**
     * Set the state of a tunnel.
     *
     * @param tunnel The tunnel to control the state of.
     * @param state The new state for this tunnel. Must be `UP`, `DOWN`, or
     * `TOGGLE`.
     * @return The updated state of the tunnel.
     */
    @Throws(Exception::class)
    fun setState(tunnel: Tunnel, state: State): State

    /**
     * Determine version of underlying backend.
     *
     * @return The version of the backend.
     * @throws Exception Any exception that happens during IO from the go library or sysfs
     */
    @Throws(Exception::class)
    fun getVersion(): String

    /**
     * Determine type name of underlying backend.
     *
     * @return Type name
     */
    fun getTypePrettyName(): String
}
