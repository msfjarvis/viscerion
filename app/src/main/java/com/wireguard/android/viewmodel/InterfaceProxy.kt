/*
 * Copyright © 2017-2019 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.android.viewmodel

import android.os.Parcel
import android.os.Parcelable
import androidx.databinding.BaseObservable
import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import me.msfjarvis.viscerion.config.Attribute
import me.msfjarvis.viscerion.config.BadConfigException
import me.msfjarvis.viscerion.config.Interface
import me.msfjarvis.viscerion.crypto.KeyPair

class InterfaceProxy : BaseObservable, Parcelable {

    val excludedApplications = ObservableArrayList<String>()
    val totalExclusionsCount = ObservableInt(0)
    var addresses = ObservableField<String>("")
    var dnsServers = ObservableField<String>("")
    var listenPort = ObservableField<String>("")
    var mtu = ObservableField<String>("")
    var privateKey = ObservableField<String>("")
    var publicKey = ObservableField<String>("")
        private set

    private constructor(parcel: Parcel) {
        addresses.set(parcel.readString())
        dnsServers.set(parcel.readString())
        parcel.readStringList(excludedApplications)
        listenPort.set(parcel.readString())
        mtu.set(parcel.readString())
        privateKey.set(parcel.readString())
        publicKey.set(parcel.readString())
        totalExclusionsCount.set(excludedApplications.size)
    }

    constructor(other: Interface) {
        addresses.set(Attribute.join(other.addresses))
        val dnsServerStrings = other.dnsServers.map { dnsServer -> dnsServer.hostAddress }
        dnsServers.set(Attribute.join(dnsServerStrings))
        excludedApplications.addAll(other.excludedApplications)
        listenPort.set(other.listenPort?.toString() ?: "")
        mtu.set(other.mtu?.toString() ?: "")
        val keyPair = other.keyPair
        privateKey.set(keyPair.privateKey.toBase64())
        publicKey.set(keyPair.publicKey.toBase64())
        totalExclusionsCount.set(excludedApplications.size)
    }

    constructor()

    override fun describeContents(): Int {
        return 0
    }

    fun generateKeyPair() {
        val keyPair = KeyPair()
        privateKey.set(keyPair.privateKey.toBase64())
        publicKey.set(keyPair.publicKey.toBase64())
    }

    @Throws(BadConfigException::class)
    fun resolve(): Interface {
        val builder = Interface.Builder()
        addresses.get()?.let { if (it.isNotEmpty()) builder.parseAddresses(it) }
        dnsServers.get()?.let { if (it.isNotEmpty()) builder.parseDnsServers(it) }
        excludedApplications.let { if (it.isNotEmpty()) builder.excludeApplications(it) }
        listenPort.get()?.let { if (it.isNotEmpty()) builder.parseListenPort(it) }
        mtu.get()?.let { if (it.isNotEmpty()) builder.parseMtu(it) }
        privateKey.get()?.let { if (it.isNotEmpty()) builder.parsePrivateKey(it) }
        return builder.build()
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(addresses.get())
        dest.writeString(dnsServers.get())
        dest.writeStringList(excludedApplications)
        dest.writeString(listenPort.get())
        dest.writeString(mtu.get())
        dest.writeString(privateKey.get())
        dest.writeString(publicKey.get())
    }

    private class InterfaceProxyCreator : Parcelable.Creator<InterfaceProxy> {
        override fun createFromParcel(parcel: Parcel): InterfaceProxy {
            return InterfaceProxy(parcel)
        }

        override fun newArray(size: Int): Array<InterfaceProxy?> {
            return arrayOfNulls(size)
        }
    }

    companion object {
        @JvmField
        @Suppress("Unused")
        val CREATOR: Parcelable.Creator<InterfaceProxy> = InterfaceProxyCreator()
    }
}
