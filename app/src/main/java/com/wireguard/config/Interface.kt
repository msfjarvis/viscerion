package com.wireguard.config

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import com.wireguard.android.Application
import com.wireguard.android.BR
import com.wireguard.android.R
import com.wireguard.android.model.GlobalExclusions
import com.wireguard.crypto.Keypair
import java.net.InetAddress

class Interface {
    private val addressList: ArrayList<InetNetwork> = ArrayList()
    private val dnsList: ArrayList<InetAddress> = ArrayList()
    private val excludedApplications: ArrayList<String> = Attribute.stringToList(GlobalExclusions.exclusions).toCollection(ArrayList())
    private var keypair: Keypair? = null
    private var listenPort: Int = 0
    private var mtu: Int = 0
    private val context: Context = Application.get()

    private fun addAddresses(addresses: Array<String>?) {
        if (addresses != null && addresses.isNotEmpty()) {
            addresses.iterator().forEach {
                if (it.isEmpty())
                    throw IllegalArgumentException(
                            context.getString(R.string.tunnel_error_empty_interface_address)
                    )
                addressList.add(InetNetwork(it))
            }
        }
    }

    private fun addDnses(dnses: Array<String>?) {
        if (dnses != null && dnses.isNotEmpty()) {
            dnses.iterator().forEach {
                dnsList.add(InetAddresses.parse(it))
            }
        }
    }

    private fun addExcludedApplications(applications: Array<String>?) {
        if (applications != null && applications.isNotEmpty()) {
            excludedApplications.addAll(applications)
        }
    }

    private fun getAddressString(): String? {
        return if (addressList.isEmpty()) null else Attribute.iterableToString(addressList)
    }

    fun getAddresses(): Array<InetNetwork> {
        return addressList.toTypedArray()
    }

    private fun getDnsString(): String? {
        return if (dnsList.isEmpty()) null else Attribute.iterableToString(getDnsStrings())
    }

    private fun getDnsStrings(): List<String> {
        val strings = ArrayList<String>()
        dnsList.iterator().forEach { strings.add(it.hostAddress) }
        return strings
    }

    fun getDnses(): Array<InetAddress> {
        return dnsList.toTypedArray()
    }

    private fun getExcludedApplicationsString(): String? {
        return if (excludedApplications.isEmpty()) null else Attribute.iterableToString(excludedApplications)
    }

    fun getExcludedApplications(): Array<String> {
        return excludedApplications.toTypedArray()
    }

    fun getListenPort(): Int {
        return listenPort
    }

    private fun getListenPortString(): String? {
        return if (listenPort == 0) null else Integer.valueOf(listenPort).toString()
    }

    fun getMtu(): Int {
        return mtu
    }

    private fun getMtuString(): String? {
        return if (mtu == 0) null else Integer.toString(mtu)
    }

    fun getPrivateKey(): String? {
        return if (keypair == null) null else keypair!!.privateKey
    }

    fun getPublicKey(): String? {
        return if (keypair == null) null else keypair!!.publicKey
    }

    fun parse(line: String) {
        val key = Attribute.match(line)
                ?: throw IllegalArgumentException(String.format(context.getString(R.string.tunnel_error_interface_parse_failed), line))
        when (key) {
            Attribute.ADDRESS -> addAddresses(key.parseList(line))
            Attribute.DNS -> addDnses(key.parseList(line))
            Attribute.EXCLUDED_APPLICATIONS -> addExcludedApplications(key.parseList(line))
            Attribute.LISTEN_PORT -> setListenPortString(key.parse(line))
            Attribute.MTU -> setMtuString(key.parse(line))
            Attribute.PRIVATE_KEY -> setPrivateKey(key.parse(line))
            else -> throw IllegalArgumentException(line)
        }
    }

    private fun setAddressString(addressString: String?) {
        addressList.clear()
        addAddresses(Attribute.stringToList(addressString))
    }

    private fun setDnsString(dnsString: String?) {
        dnsList.clear()
        addDnses(Attribute.stringToList(dnsString))
    }

    private fun setExcludedApplicationsString(applicationsString: String?) {
        excludedApplications.clear()
        addExcludedApplications(Attribute.stringToList(applicationsString))
    }

    private fun setListenPort(listenPort: Int) {
        this.listenPort = listenPort
    }

    private fun setListenPortString(port: String?) {
        if (port != null && !port.isEmpty())
            setListenPort(Integer.parseInt(port, 10))
        else
            setListenPort(0)
    }

    private fun setMtu(mtu: Int) {
        this.mtu = mtu
    }

    private fun setMtuString(mtu: String?) {
        if (mtu != null && !mtu.isEmpty())
            setMtu(Integer.parseInt(mtu, 10))
        else
            setMtu(0)
    }

    private fun setPrivateKey(privateKey: String?) {
        var key = privateKey
        if (key != null && key.isEmpty())
            key = null
        keypair = if (key == null) null else Keypair(key)
    }

    override fun toString(): String {
        val sb = StringBuilder().append("[Interface]\n")
        if (!addressList.isEmpty())
            sb.append(Attribute.ADDRESS.composeWith(addressList))
        if (!dnsList.isEmpty())
            sb.append(Attribute.DNS.composeWith(getDnsStrings()))
        if (!excludedApplications.isEmpty())
            sb.append(Attribute.EXCLUDED_APPLICATIONS.composeWith(excludedApplications))
        if (listenPort != 0)
            sb.append(Attribute.LISTEN_PORT.composeWith(listenPort))
        if (mtu != 0)
            sb.append(Attribute.MTU.composeWith(mtu))
        if (keypair != null)
            sb.append(Attribute.PRIVATE_KEY.composeWith(keypair!!.privateKey))
        return sb.toString()
    }

    class Observable : BaseObservable, Parcelable {
        private var addresses: String? = null
        private var dnses: String? = null
        private var excludedApplications: String? = null
        private var listenPort: String? = null
        private var mtu: String? = null
        private var privateKey: String? = null
        @get:Bindable
        var publicKey: String? = null
            private set

        val excludedApplicationsCount: Int
            @Bindable
            get() = Attribute.stringToList(excludedApplications).size

        constructor(parent: Interface?) {
            if (parent != null)
                loadData(parent)
        }

        private constructor(`in`: Parcel) {
            addresses = `in`.readString()
            dnses = `in`.readString()
            publicKey = `in`.readString()
            privateKey = `in`.readString()
            listenPort = `in`.readString()
            mtu = `in`.readString()
            excludedApplications = `in`.readString()
        }

        fun commitData(parent: Interface) {
            parent.setAddressString(addresses)
            parent.setDnsString(dnses)
            parent.setExcludedApplicationsString(excludedApplications)
            parent.setPrivateKey(privateKey)
            parent.setListenPortString(listenPort)
            parent.setMtuString(mtu)
            loadData(parent)
            notifyChange()
        }

        override fun describeContents(): Int {
            return 0
        }

        fun generateKeypair() {
            val keypair = Keypair()
            privateKey = keypair.privateKey
            publicKey = keypair.publicKey
            notifyPropertyChanged(BR.privateKey)
            notifyPropertyChanged(BR.publicKey)
        }

        @Bindable
        fun getAddresses(): String? {
            return addresses
        }

        @Bindable
        fun getDnses(): String? {
            return dnses
        }

        @Bindable
        fun getExcludedApplications(): String? {
            return excludedApplications
        }

        @Bindable
        fun getListenPort(): String? {
            return listenPort
        }

        @Bindable
        fun getMtu(): String? {
            return mtu
        }

        @Bindable
        fun getPrivateKey(): String? {
            return privateKey
        }

        private fun loadData(parent: Interface) {
            addresses = parent.getAddressString()
            dnses = parent.getDnsString()
            excludedApplications = parent.getExcludedApplicationsString()
            publicKey = parent.getPublicKey()
            privateKey = parent.getPrivateKey()
            listenPort = parent.getListenPortString()
            mtu = parent.getMtuString()
        }

        fun setAddresses(addresses: String) {
            this.addresses = addresses
            notifyPropertyChanged(BR.addresses)
        }

        fun setDnses(dnses: String) {
            this.dnses = dnses
            notifyPropertyChanged(BR.dnses)
        }

        fun setExcludedApplications(excludedApplications: String) {
            this.excludedApplications = excludedApplications
            notifyPropertyChanged(BR.excludedApplications)
            notifyPropertyChanged(BR.excludedApplicationsCount)
        }

        fun setListenPort(listenPort: String) {
            this.listenPort = listenPort
            notifyPropertyChanged(BR.listenPort)
        }

        fun setMtu(mtu: String) {
            this.mtu = mtu
            notifyPropertyChanged(BR.mtu)
        }

        fun setPrivateKey(privateKey: String) {
            this.privateKey = privateKey

            publicKey = try {
                Keypair(privateKey).publicKey
            } catch (ignored: IllegalArgumentException) {
                ""
            }

            notifyPropertyChanged(BR.privateKey)
            notifyPropertyChanged(BR.publicKey)
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeString(addresses)
            dest.writeString(dnses)
            dest.writeString(publicKey)
            dest.writeString(privateKey)
            dest.writeString(listenPort)
            dest.writeString(mtu)
            dest.writeString(excludedApplications)
        }

        companion object {
            val CREATOR: Parcelable.Creator<Observable> = object : Parcelable.Creator<Observable> {
                override fun createFromParcel(`in`: Parcel): Observable {
                    return Observable(`in`)
                }

                override fun newArray(size: Int): Array<Observable?> {
                    return arrayOfNulls(size)
                }
            }
        }
    }
}