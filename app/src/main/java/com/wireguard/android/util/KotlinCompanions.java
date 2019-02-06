/*
 * Copyright © 2018 Harsh Shandilya. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.wireguard.android.util;

import com.wireguard.android.model.Tunnel;
import com.wireguard.android.model.TunnelManager;
import java.util.ArrayList;
import java.util.Set;
import java9.util.concurrent.CompletableFuture;
import java9.util.concurrent.CompletionStage;
import java9.util.stream.StreamSupport;

/**
 * Slightly crufty workaround for Kotlin not supporting the CompletableFuture[]::new expression,
 * don't like it but what can we do :')
 *
 * <p>TODO: Get rid of this as soon as the ability is made available
 */
public final class KotlinCompanions {

    public static CompletionStage<Void> streamForStateChange(
            final ObservableSortedKeyedArrayList<String, Tunnel> tunnels,
            final Set<String> previouslyRunning,
            final TunnelManager tunnelManager) {
        return CompletableFuture.allOf(
                StreamSupport.stream(tunnels)
                        .filter(tunnel -> previouslyRunning.contains(tunnel.getName()))
                        .map(tunnel -> tunnelManager.setTunnelState(tunnel, Tunnel.State.UP))
                        .toArray(CompletableFuture[]::new));
    }

    public static CompletableFuture[] streamForDeletion(final ArrayList<Tunnel> tunnels) {
        return StreamSupport.stream(tunnels).map(Tunnel::delete).toArray(CompletableFuture[]::new);
    }
}
