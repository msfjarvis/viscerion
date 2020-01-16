/*
 * Copyright © 2017-2020 WireGuard LLC.
 * Copyright © 2018-2020 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.viewmodel

import android.os.Parcel
import android.os.Parcelable
import androidx.databinding.ObservableArrayList
import me.msfjarvis.viscerion.config.BadConfigException
import me.msfjarvis.viscerion.config.Config
import me.msfjarvis.viscerion.config.Peer

class ConfigProxy : Parcelable {

    val interfaze: InterfaceProxy
    val peers = ObservableArrayList<PeerProxy>()

    private constructor(parcel: Parcel) {
        interfaze = parcel.readParcelable<InterfaceProxy>(InterfaceProxy::class.java.classLoader) as InterfaceProxy
        parcel.readTypedList(peers, PeerProxy.CREATOR)
        for (proxy in peers)
            proxy.bind(this)
    }

    constructor(other: Config) {
        interfaze = InterfaceProxy(other.interfaze)
        for (peer in other.peers) {
            val proxy = PeerProxy(peer)
            peers.add(proxy)
            proxy.bind(this)
        }
    }

    constructor() {
        interfaze = InterfaceProxy()
    }

    fun addPeer(): PeerProxy {
        val proxy = PeerProxy()
        peers.add(proxy)
        proxy.bind(this)
        return proxy
    }

    override fun describeContents(): Int {
        return 0
    }

    @Throws(BadConfigException::class)
    fun resolve(): Config {
        val resolvedPeers = ArrayList<Peer>()
        for (proxy in peers)
            resolvedPeers.add(proxy.resolve())
        return Config.Builder()
            .setInterface(interfaze.resolve())
            .addPeers(resolvedPeers)
            .build()
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeParcelable(interfaze, flags)
        dest.writeTypedList<Parcelable>(peers as List<Parcelable>?)
    }

    private class ConfigProxyCreator : Parcelable.Creator<ConfigProxy> {
        override fun createFromParcel(parcel: Parcel): ConfigProxy {
            return ConfigProxy(parcel)
        }

        override fun newArray(size: Int): Array<ConfigProxy?> {
            return arrayOfNulls(size)
        }
    }

    companion object {
        @JvmField
        @Suppress("Unused")
        val CREATOR: Parcelable.Creator<ConfigProxy> = ConfigProxyCreator()
    }
}
