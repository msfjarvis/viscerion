package com.wireguard.android.util;

import com.wireguard.android.Application;
import com.wireguard.android.model.Tunnel;
import com.wireguard.android.model.TunnelManager;
import java9.util.concurrent.CompletableFuture;
import java9.util.concurrent.CompletionStage;
import java9.util.stream.StreamSupport;

import java.util.Set;

/**
 * Slightly crufty workaround for Kotlin not supporting the CompletableFuture[]::new
 * expression, don't like it but what can we do :')
 * <p>
 * TODO: Get rid of this as soon as the ability is made available
 */
public final class KotlinCompanions {

    public static CompletionStage<Void> stream(final ObservableSortedKeyedArrayList<String, Tunnel> tunnels,
                                               final Set<String> previouslyRunning,
                                               final TunnelManager tunnelManager) {
        return CompletableFuture.allOf(StreamSupport.stream(tunnels)
                .filter(tunnel -> previouslyRunning.contains(tunnel.getName()))
                .map(tunnel -> setTunnelState(tunnel, tunnelManager))
                .toArray(CompletableFuture[]::new));
    }

    private static CompletionStage<Tunnel.State> setTunnelState(final Tunnel tunnel, final TunnelManager tunnelManager) {
        // Ensure the configuration is loaded before trying to use it.
        return tunnel.getConfigAsync().thenCompose(x ->
                Application.Companion.getAsyncWorker().supplyAsync(() -> Application.Companion.getBackend().setState(tunnel, Tunnel.State.UP))
        ).whenComplete((newState, e) -> {
            // Ensure onStateChanged is always called (failure or not), and with the correct state.
            tunnel.onStateChanged(e == null ? newState : tunnel.getState());
            if (e == null && newState == Tunnel.State.UP)
                tunnelManager.setLastUsedTunnel(tunnel);
            tunnelManager.saveState();
        });
    }
}
