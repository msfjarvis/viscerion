/*
 * Copyright © 2017-2018 WireGuard LLC.
 * Copyright © 2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.viewmodel

import android.os.Parcel
import android.os.Parcelable
import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableList

import com.wireguard.config.BadConfigException
import com.wireguard.config.Config
import com.wireguard.config.Peer

import java.util.ArrayList

class ConfigProxy : Parcelable {

    val `interface`: InterfaceProxy
    val peers: ObservableList<PeerProxy> = ObservableArrayList()

    private constructor(`in`: Parcel) {
        `interface` = `in`.readParcelable(InterfaceProxy::class.java.classLoader) as InterfaceProxy
        `in`.readTypedList(peers, PeerProxy.CREATOR)
        for (proxy in peers)
            proxy.bind(this)
    }

    constructor(other: Config) {
        `interface` = InterfaceProxy(other.`interface`)
        for (peer in other.peers) {
            val proxy = PeerProxy(peer)
            peers.add(proxy)
            proxy.bind(this)
        }
    }

    constructor() {
        `interface` = InterfaceProxy()
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
            .setInterface(`interface`.resolve())
            .addPeers(resolvedPeers)
            .build()
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeParcelable(`interface`, flags)
        dest.writeTypedList<Parcelable>(peers as List<Parcelable>?)
    }

    private class ConfigProxyCreator : Parcelable.Creator<ConfigProxy> {
        override fun createFromParcel(`in`: Parcel): ConfigProxy {
            return ConfigProxy(`in`)
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
