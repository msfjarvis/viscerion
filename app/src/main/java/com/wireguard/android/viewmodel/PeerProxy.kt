/*
 * Copyright © 2017-2018 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.wireguard.android.viewmodel

import android.os.Parcel
import android.os.Parcelable
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import androidx.databinding.Observable
import androidx.databinding.ObservableList
import com.wireguard.android.BR
import com.wireguard.config.Attribute
import com.wireguard.config.BadConfigException
import com.wireguard.config.Peer
import java.lang.ref.WeakReference
import java.util.ArrayList
import java.util.LinkedHashSet

class PeerProxy : BaseObservable, Parcelable {

    private val dnsRoutes = ArrayList<String>()
    private var allowedIps: String? = null
    private var allowedIpsState = AllowedIpsState.INVALID
    private var endpoint: String? = null
    private var interfaceDnsListener: InterfaceDnsListener? = null
    private var owner: ConfigProxy? = null
    private var peerListListener: PeerListListener? = null
    private var persistentKeepalive: String? = null
    private var preSharedKey: String? = null
    private var publicKey: String? = null
    private var totalPeers: Int = 0

    private val allowedIpsSet: Set<String>
        get() = LinkedHashSet(Attribute.split(allowedIps.toString()).toSet())

    val isAbleToExcludePrivateIps: Boolean
        @Bindable
        get() = allowedIpsState == AllowedIpsState.CONTAINS_IPV4_PUBLIC_NETWORKS || allowedIpsState == AllowedIpsState.CONTAINS_IPV4_WILDCARD

    // Replace the first instance of the wildcard with the public network list, or vice versa.
    // DNS servers only need to handled specially when we're excluding private IPs.
    var isExcludingPrivateIps: Boolean
        @Bindable
        get() = allowedIpsState == AllowedIpsState.CONTAINS_IPV4_PUBLIC_NETWORKS
        set(excludingPrivateIps) {
            if (!isAbleToExcludePrivateIps || isExcludingPrivateIps == excludingPrivateIps)
                return
            val oldNetworks = if (excludingPrivateIps) IPV4_WILDCARD else IPV4_PUBLIC_NETWORKS
            val newNetworks = if (excludingPrivateIps) IPV4_PUBLIC_NETWORKS else IPV4_WILDCARD
            val input = allowedIpsSet
            val outputSize = input.size - oldNetworks.size + newNetworks.size
            val output = LinkedHashSet<String>(outputSize)
            var replaced = false
            for (network in input) {
                if (oldNetworks.contains(network)) {
                    if (!replaced) {
                        for (replacement in newNetworks)
                            if (!output.contains(replacement))
                                output.add(replacement)
                        replaced = true
                    }
                } else if (!output.contains(network)) {
                    output.add(network)
                }
            }
            if (excludingPrivateIps)
                output.addAll(dnsRoutes)
            else
                output.removeAll(dnsRoutes)
            allowedIps = Attribute.join(output)
            allowedIpsState = if (excludingPrivateIps)
                AllowedIpsState.CONTAINS_IPV4_PUBLIC_NETWORKS
            else
                AllowedIpsState.CONTAINS_IPV4_WILDCARD
            notifyPropertyChanged(BR.allowedIps)
            notifyPropertyChanged(BR.excludingPrivateIps)
        }

    fun toggleExcludePrivateIPs() {
        isExcludingPrivateIps = !isExcludingPrivateIps
    }

    private constructor(`in`: Parcel) {
        allowedIps = `in`.readString()
        endpoint = `in`.readString()
        persistentKeepalive = `in`.readString()
        preSharedKey = `in`.readString()
        publicKey = `in`.readString()
    }

    constructor(other: Peer) {
        allowedIps = Attribute.join(other.allowedIps)
        endpoint = other.endpoint?.toString() ?: ""
        persistentKeepalive = other.persistentKeepalive?.toString() ?: ""
        preSharedKey = other.preSharedKey?.toBase64() ?: ""
        publicKey = other.publicKey.toBase64()
    }

    constructor() {
        allowedIps = ""
        endpoint = ""
        persistentKeepalive = ""
        preSharedKey = ""
        publicKey = ""
    }

    fun bind(owner: ConfigProxy) {
        val interfaze = owner.`interface`
        val peers = owner.peers
        if (interfaceDnsListener == null)
            interfaceDnsListener = InterfaceDnsListener(this)
        interfaze.addOnPropertyChangedCallback(interfaceDnsListener!!)
        setInterfaceDns(interfaze.getDnsServers())
        if (peerListListener == null)
            peerListListener = PeerListListener(this)
        peers.addOnListChangedCallback(peerListListener)
        setTotalPeers(peers.size)
        this.owner = owner
    }

    private fun calculateAllowedIpsState() {
        val newState: AllowedIpsState
        newState = if (totalPeers == 1) {
            // String comparison works because we only care if allowedIps is a superset of one of
            // the above sets of (valid) *networks*. We are not checking for a superset based on
            // the individual addresses in each set.
            val networkStrings = allowedIpsSet
            // If allowedIps contains both the wildcard and the public networks, then private
            // networks aren't excluded!
            when {
                networkStrings.containsAll(IPV4_WILDCARD) -> AllowedIpsState.CONTAINS_IPV4_WILDCARD
                networkStrings.containsAll(IPV4_PUBLIC_NETWORKS) -> AllowedIpsState.CONTAINS_IPV4_PUBLIC_NETWORKS
                else -> AllowedIpsState.OTHER
            }
        } else {
            AllowedIpsState.INVALID
        }
        if (newState != allowedIpsState) {
            allowedIpsState = newState
            notifyPropertyChanged(BR.ableToExcludePrivateIps)
            notifyPropertyChanged(BR.excludingPrivateIps)
        }
    }

    override fun describeContents(): Int {
        return 0
    }

    @Bindable
    fun getAllowedIps(): String? {
        return allowedIps
    }

    @Bindable
    fun getEndpoint(): String? {
        return endpoint
    }

    @Bindable
    fun getPersistentKeepalive(): String? {
        return persistentKeepalive
    }

    @Bindable
    fun getPreSharedKey(): String? {
        return preSharedKey
    }

    @Bindable
    fun getPublicKey(): String? {
        return publicKey
    }

    @Throws(BadConfigException::class)
    fun resolve(): Peer {
        val builder = Peer.Builder()
        allowedIps?.let { if (it.isNotEmpty()) builder.parseAllowedIPs(it) }
        endpoint?.let { if (it.isNotEmpty()) builder.parseEndpoint(it) }
        persistentKeepalive?.let { if (it.isNotEmpty()) builder.parsePersistentKeepalive(it) }
        preSharedKey?.let { if (it.isNotEmpty()) builder.parsePreSharedKey(it) }
        publicKey?.let { if (it.isNotEmpty()) builder.parsePublicKey(it) }
        return builder.build()
    }

    fun setAllowedIps(allowedIps: String) {
        this.allowedIps = allowedIps
        notifyPropertyChanged(BR.allowedIps)
        calculateAllowedIpsState()
    }

    fun setEndpoint(endpoint: String) {
        this.endpoint = endpoint
        notifyPropertyChanged(BR.endpoint)
    }

    private fun setInterfaceDns(dnsServers: CharSequence?) {
        val newDnsRoutes: Array<String> = Attribute.split(dnsServers ?: "")
            .filter { server -> !server.contains(":") }
            .map { server -> "$server/32" }
            .toTypedArray()
        if (allowedIpsState == AllowedIpsState.CONTAINS_IPV4_PUBLIC_NETWORKS) {
            val input = allowedIpsSet
            val output = LinkedHashSet<String>(input.size + 1)
            // Yes, this is quadratic in the number of DNS servers, but most users have 1 or 2.
            for (network in input)
                if (!dnsRoutes.contains(network) || newDnsRoutes.contains(network))
                    output.add(network)
            // Since output is a Set, this does the Right Thing™ (it does not duplicate networks).
            output.addAll(newDnsRoutes)
            // None of the public networks are /32s, so this cannot change the AllowedIPs state.
            allowedIps = Attribute.join(output)
            notifyPropertyChanged(BR.allowedIps)
        }
        dnsRoutes.clear()
        dnsRoutes.addAll(newDnsRoutes)
    }

    fun setPersistentKeepalive(persistentKeepalive: String) {
        this.persistentKeepalive = persistentKeepalive
        notifyPropertyChanged(BR.persistentKeepalive)
    }

    fun setPreSharedKey(preSharedKey: String) {
        this.preSharedKey = preSharedKey
        notifyPropertyChanged(BR.preSharedKey)
    }

    fun setPublicKey(publicKey: String) {
        this.publicKey = publicKey
        notifyPropertyChanged(BR.publicKey)
    }

    private fun setTotalPeers(totalPeers: Int) {
        if (this.totalPeers == totalPeers)
            return
        this.totalPeers = totalPeers
        calculateAllowedIpsState()
    }

    fun unbind() {
        if (owner == null)
            return
        owner?.let {
            val interfaze = it.`interface`
            val peers = it.peers
            interfaceDnsListener?.let { interfaceDnsListener ->
                interfaze.removeOnPropertyChangedCallback(
                    interfaceDnsListener
                )
            }
            peerListListener?.let { peerListListener -> peers.removeOnListChangedCallback(peerListListener) }
            peers.remove(this)
            setInterfaceDns("")
            setTotalPeers(0)
        }
        owner = null
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(allowedIps)
        dest.writeString(endpoint)
        dest.writeString(persistentKeepalive)
        dest.writeString(preSharedKey)
        dest.writeString(publicKey)
    }

    private enum class AllowedIpsState {
        CONTAINS_IPV4_PUBLIC_NETWORKS,
        CONTAINS_IPV4_WILDCARD,
        INVALID,
        OTHER
    }

    private class InterfaceDnsListener(peerProxy: PeerProxy) :
        Observable.OnPropertyChangedCallback() {
        private val weakPeerProxy: WeakReference<PeerProxy> = WeakReference(peerProxy)

        override fun onPropertyChanged(sender: Observable, propertyId: Int) {
            val peerProxy: PeerProxy? = weakPeerProxy.get()
            if (peerProxy == null) {
                sender.removeOnPropertyChangedCallback(this)
                return
            }
            // This shouldn't be possible, but try to avoid a ClassCastException anyway.
            if (sender !is InterfaceProxy)
                return
            if (!(propertyId == BR._all || propertyId == BR.dnsServers))
                return
            peerProxy.setInterfaceDns(sender.getDnsServers())
        }
    }

    private class PeerListListener(peerProxy: PeerProxy) :
        ObservableList.OnListChangedCallback<ObservableList<PeerProxy>>() {
        private val weakPeerProxy: WeakReference<PeerProxy> = WeakReference(peerProxy)

        override fun onChanged(sender: ObservableList<PeerProxy>) {
            val peerProxy: PeerProxy? = weakPeerProxy.get()
            if (peerProxy == null) {
                sender.removeOnListChangedCallback(this)
                return
            }
            peerProxy.setTotalPeers(sender.size)
        }

        override fun onItemRangeChanged(
            sender: ObservableList<PeerProxy>,
            positionStart: Int,
            itemCount: Int
        ) {
            // Do nothing.
        }

        override fun onItemRangeInserted(
            sender: ObservableList<PeerProxy>,
            positionStart: Int,
            itemCount: Int
        ) {
            onChanged(sender)
        }

        override fun onItemRangeMoved(
            sender: ObservableList<PeerProxy>,
            fromPosition: Int,
            toPosition: Int,
            itemCount: Int
        ) {
            // Do nothing.
        }

        override fun onItemRangeRemoved(
            sender: ObservableList<PeerProxy>,
            positionStart: Int,
            itemCount: Int
        ) {
            onChanged(sender)
        }
    }

    private class PeerProxyCreator : Parcelable.Creator<PeerProxy> {
        override fun createFromParcel(`in`: Parcel): PeerProxy {
            return PeerProxy(`in`)
        }

        override fun newArray(size: Int): Array<PeerProxy?> {
            return arrayOfNulls(size)
        }
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<PeerProxy> = PeerProxyCreator()
        private val IPV4_PUBLIC_NETWORKS = LinkedHashSet(
            listOf(
                "0.0.0.0/5", "8.0.0.0/7", "11.0.0.0/8", "12.0.0.0/6", "16.0.0.0/4", "32.0.0.0/3",
                "64.0.0.0/2", "128.0.0.0/3", "160.0.0.0/5", "168.0.0.0/6", "172.0.0.0/12",
                "172.32.0.0/11", "172.64.0.0/10", "172.128.0.0/9", "173.0.0.0/8", "174.0.0.0/7",
                "176.0.0.0/4", "192.0.0.0/9", "192.128.0.0/11", "192.160.0.0/13", "192.169.0.0/16",
                "192.170.0.0/15", "192.172.0.0/14", "192.176.0.0/12", "192.192.0.0/10",
                "193.0.0.0/8", "194.0.0.0/7", "196.0.0.0/6", "200.0.0.0/5", "208.0.0.0/4"
            )
        )
        private val IPV4_WILDCARD = setOf("0.0.0.0/0")
    }
}
