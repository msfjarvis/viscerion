/*
 * Copyright © 2017-2018 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.wireguard.config

import android.os.Parcel
import android.os.Parcelable
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableList
import androidx.databinding.library.baseAdapters.BR
import com.wireguard.android.Application
import com.wireguard.android.R
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.StringReader
import java.nio.charset.StandardCharsets
import java.util.ArrayList

/**
 * Represents a wg-quick configuration file, its name, and its connection state.
 */

class Config {
    val `interface` = Interface()
    private var peers: MutableList<Peer> = ArrayList()

    fun getPeers(): List<Peer> {
        return peers
    }

    override fun toString(): String {
        val sb = StringBuilder().append(`interface`)
        for (peer in peers)
            sb.append('\n').append(peer)
        return sb.toString()
    }

    class Observable : BaseObservable, Parcelable {
        @get:Bindable
        val interfaceSection: Interface.Observable
        @get:Bindable
        val peers: ObservableList<Peer.Observable>
        private var name: String? = null

        constructor(parent: Config?, name: String?) {
            this.name = name

            this.interfaceSection = Interface.Observable(parent?.`interface`)
            this.peers = ObservableArrayList<Peer.Observable>()
            parent?.let {
                for (peer in it.getPeers())
                    this.peers.add(Peer.Observable(peer))
            }
        }

        private constructor(parcel: Parcel) {
            name = parcel.readString()
            interfaceSection = parcel.readParcelable(Interface.Observable::class.java.classLoader) as Interface.Observable
            peers = ObservableArrayList<Peer.Observable>()
            parcel.readTypedList(peers, Peer.Observable.CREATOR)
        }

        fun commitData(parent: Config) {
            this.interfaceSection.commitData(parent.`interface`)
            val newPeers = ArrayList<Peer>(this.peers.size)
            for (observablePeer in this.peers) {
                val peer = Peer()
                observablePeer.commitData(peer)
                newPeers.add(peer)
            }
            parent.peers = newPeers
            notifyChange()
        }

        override fun describeContents(): Int {
            return 0
        }

        @Bindable
        fun getName(): String {
            return if (name == null) "" else name as String
        }

        fun setName(name: String) {
            this.name = name
            notifyPropertyChanged(BR.name)
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeString(name)
            dest.writeParcelable(this.interfaceSection, flags)
            dest.writeTypedList<Peer.Observable>(this.peers)
        }

        companion object {
            @JvmField
            val CREATOR: Parcelable.Creator<Observable> = object : Parcelable.Creator<Observable> {
                override fun createFromParcel(parcel: Parcel): Observable {
                    return Observable(parcel)
                }

                override fun newArray(size: Int): Array<Observable?> {
                    return arrayOfNulls(size)
                }
            }
        }
    }

    companion object {

        @Throws(IOException::class)
        fun from(string: String?): Config {
            return from(BufferedReader(StringReader(string)))
        }

        @Throws(IOException::class)
        fun from(stream: InputStream): Config {
            return from(BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)))
        }

        @Throws(IOException::class)
        fun from(reader: BufferedReader): Config {
            val config = Config()
            val context = Application.get()
            var currentPeer: Peer? = null
            var line: String?
            var inInterfaceSection = false
            while (true) {
                line = reader.readLine()
                if (line == null)
                    break
                val commentIndex = line.indexOf('#')
                if (commentIndex != -1)
                    line = line.substring(0, commentIndex)
                line = line.trim { it <= ' ' }
                if (line.isEmpty())
                    continue
                when {
                    "[Interface]".toLowerCase() == line.toLowerCase() -> {
                        currentPeer = null
                        inInterfaceSection = true
                    }
                    "[Peer]".toLowerCase() == line.toLowerCase() -> {
                        currentPeer = Peer()
                        config.peers.add(currentPeer)
                        inInterfaceSection = false
                    }
                    inInterfaceSection -> config.`interface`.parse(line)
                    currentPeer != null -> currentPeer.parse(line)
                    else -> throw IllegalArgumentException(context.getString(R.string.tunnel_error_invalid_config_line, line))
                }
            }
            if (!inInterfaceSection && currentPeer == null) {
                throw IllegalArgumentException(context.getString(R.string.tunnel_error_no_config_information))
            }
            return config
        }
    }
}
