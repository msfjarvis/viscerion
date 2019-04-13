/*
 * Copyright © 2017-2018 WireGuard LLC.
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
import com.wireguard.config.Attribute
import com.wireguard.config.BadConfigException
import com.wireguard.config.Interface
import com.wireguard.crypto.KeyPair

class InterfaceProxy : BaseObservable, Parcelable {

    val excludedApplications = ObservableArrayList<String>()
    val totalExclusionsCount = ObservableInt(0)
    var addresses = ObservableField<String>()
    var dnsServers = ObservableField<String>()
    var listenPort = ObservableField<String>()
    var mtu = ObservableField<String>()
    var privateKey = ObservableField<String>()
    var publicKey = ObservableField<String>()
        private set

    private constructor(`in`: Parcel) {
        addresses.set(`in`.readString())
        dnsServers.set(`in`.readString())
        `in`.readStringList(excludedApplications)
        listenPort.set(`in`.readString())
        mtu.set(`in`.readString())
        privateKey.set(`in`.readString())
        publicKey.set(`in`.readString())
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

    constructor() {
        addresses.set("")
        dnsServers.set("")
        listenPort.set("")
        mtu.set("")
        privateKey.set("")
        publicKey.set("")
    }

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
        override fun createFromParcel(`in`: Parcel): InterfaceProxy {
            return InterfaceProxy(`in`)
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
