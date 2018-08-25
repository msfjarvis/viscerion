/*
 * Copyright © 2018 Samuel Holland <samuel@sholland.org>
 * Copyright © 2018 Jason A. Donenfeld <Jason@zx2c4.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.wireguard.config;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.Nullable;
import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;
import com.wireguard.android.Application;
import com.wireguard.android.BR;
import com.wireguard.android.R;
import com.wireguard.android.model.GlobalExclusions;
import com.wireguard.crypto.Keypair;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents the configuration for a WireGuard interface (an [Interface] block).
 */

public class Interface {
    private final List<InetNetwork> addressList;
    private final List<InetAddress> dnsList;
    private final List<String> excludedApplications;
    private final Context context = Application.Companion.get();
    @Nullable
    private Keypair keypair;
    private int listenPort;
    private int mtu;

    public Interface() {
        addressList = new ArrayList<>();
        dnsList = new ArrayList<>();
        excludedApplications = new ArrayList<>(Arrays.asList(Attribute.stringToList(GlobalExclusions.Companion.getExclusions())));
    }

    private void addAddresses(@Nullable final String[] addresses) {
        if (addresses != null && addresses.length > 0) {
            for (final String addr : addresses) {
                if (addr.isEmpty())
                    throw new IllegalArgumentException(context.getString(R.string.tunnel_error_empty_interface_address));
                addressList.add(new InetNetwork(addr));
            }
        }
    }

    private void addDnses(@Nullable final String[] dnses) {
        if (dnses != null && dnses.length > 0) {
            for (final String dns : dnses) {
                dnsList.add(InetAddresses.parse(dns));
            }
        }
    }

    private void addExcludedApplications(@Nullable final String[] applications) {
        if (applications != null && applications.length > 0) {
            excludedApplications.addAll(Arrays.asList(applications));
        }
    }

    @Nullable
    private String getAddressString() {
        if (addressList.isEmpty())
            return null;
        return Attribute.iterableToString(addressList);
    }

    private void setAddressString(@Nullable final String addressString) {
        addressList.clear();
        addAddresses(Attribute.stringToList(addressString));
    }

    public InetNetwork[] getAddresses() {
        return addressList.toArray(new InetNetwork[addressList.size()]);
    }

    @Nullable
    private String getDnsString() {
        if (dnsList.isEmpty())
            return null;
        return Attribute.iterableToString(getDnsStrings());
    }

    private void setDnsString(@Nullable final String dnsString) {
        dnsList.clear();
        addDnses(Attribute.stringToList(dnsString));
    }

    private List<String> getDnsStrings() {
        final List<String> strings = new ArrayList<>();
        for (final InetAddress addr : dnsList)
            strings.add(addr.getHostAddress());
        return strings;
    }

    public InetAddress[] getDnses() {
        return dnsList.toArray(new InetAddress[dnsList.size()]);
    }

    @Nullable
    private String getExcludedApplicationsString() {
        if (excludedApplications.isEmpty())
            return null;
        return Attribute.iterableToString(excludedApplications);
    }

    private void setExcludedApplicationsString(@Nullable final String applicationsString) {
        excludedApplications.clear();
        addExcludedApplications(Attribute.stringToList(applicationsString));
    }

    public String[] getExcludedApplications() {
        return excludedApplications.toArray(new String[excludedApplications.size()]);
    }

    public int getListenPort() {
        return listenPort;
    }

    private void setListenPort(final int listenPort) {
        this.listenPort = listenPort;
    }

    @Nullable
    private String getListenPortString() {
        if (listenPort == 0)
            return null;
        return Integer.valueOf(listenPort).toString();
    }

    private void setListenPortString(@Nullable final String port) {
        if (port != null && !port.isEmpty())
            setListenPort(Integer.parseInt(port, 10));
        else
            setListenPort(0);
    }

    public int getMtu() {
        return mtu;
    }

    private void setMtu(final int mtu) {
        this.mtu = mtu;
    }

    @Nullable
    private String getMtuString() {
        if (mtu == 0)
            return null;
        return Integer.toString(mtu);
    }

    private void setMtuString(@Nullable final String mtu) {
        if (mtu != null && !mtu.isEmpty())
            setMtu(Integer.parseInt(mtu, 10));
        else
            setMtu(0);
    }

    @Nullable
    public String getPrivateKey() {
        if (keypair == null)
            return null;
        return keypair.getPrivateKey();
    }

    private void setPrivateKey(@Nullable String privateKey) {
        if (privateKey != null && privateKey.isEmpty())
            privateKey = null;
        keypair = privateKey == null ? null : new Keypair(privateKey);
    }

    @Nullable
    public String getPublicKey() {
        if (keypair == null)
            return null;
        return keypair.getPublicKey();
    }

    public void parse(final String line) {
        final Attribute key = Attribute.match(line);
        if (key == null)
            throw new IllegalArgumentException(String.format(context.getString(R.string.tunnel_error_interface_parse_failed), line));
        switch (key) {
            case ADDRESS:
                addAddresses(key.parseList(line));
                break;
            case DNS:
                addDnses(key.parseList(line));
                break;
            case EXCLUDED_APPLICATIONS:
                addExcludedApplications(key.parseList(line));
                break;
            case LISTEN_PORT:
                setListenPortString(key.parse(line));
                break;
            case MTU:
                setMtuString(key.parse(line));
                break;
            case PRIVATE_KEY:
                setPrivateKey(key.parse(line));
                break;
            default:
                throw new IllegalArgumentException(line);
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder().append("[Interface]\n");
        if (!addressList.isEmpty())
            sb.append(Attribute.ADDRESS.composeWith(addressList));
        if (!dnsList.isEmpty())
            sb.append(Attribute.DNS.composeWith(getDnsStrings()));
        if (!excludedApplications.isEmpty())
            sb.append(Attribute.EXCLUDED_APPLICATIONS.composeWith(excludedApplications));
        if (listenPort != 0)
            sb.append(Attribute.LISTEN_PORT.composeWith(listenPort));
        if (mtu != 0)
            sb.append(Attribute.MTU.composeWith(mtu));
        if (keypair != null)
            sb.append(Attribute.PRIVATE_KEY.composeWith(keypair.getPrivateKey()));
        return sb.toString();
    }

    public static class Observable extends BaseObservable implements Parcelable {
        public static final Creator<Observable> CREATOR = new Creator<Observable>() {
            @Override
            public Observable createFromParcel(final Parcel in) {
                return new Observable(in);
            }

            @Override
            public Observable[] newArray(final int size) {
                return new Observable[size];
            }
        };
        @Nullable
        private String addresses;
        @Nullable
        private String dnses;
        @Nullable
        private String excludedApplications;
        @Nullable
        private String listenPort;
        @Nullable
        private String mtu;
        @Nullable
        private String privateKey;
        @Nullable
        private String publicKey;

        public Observable(@Nullable final Interface parent) {
            if (parent != null)
                loadData(parent);
        }

        private Observable(final Parcel in) {
            addresses = in.readString();
            dnses = in.readString();
            publicKey = in.readString();
            privateKey = in.readString();
            listenPort = in.readString();
            mtu = in.readString();
            excludedApplications = in.readString();
        }

        public void commitData(final Interface parent) {
            parent.setAddressString(addresses);
            parent.setDnsString(dnses);
            parent.setExcludedApplicationsString(excludedApplications);
            parent.setPrivateKey(privateKey);
            parent.setListenPortString(listenPort);
            parent.setMtuString(mtu);
            loadData(parent);
            notifyChange();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public void generateKeypair() {
            final Keypair keypair = new Keypair();
            privateKey = keypair.getPrivateKey();
            publicKey = keypair.getPublicKey();
            notifyPropertyChanged(BR.privateKey);
            notifyPropertyChanged(BR.publicKey);
        }

        @Nullable
        @Bindable
        public String getAddresses() {
            return addresses;
        }

        public void setAddresses(final String addresses) {
            this.addresses = addresses;
            notifyPropertyChanged(BR.addresses);
        }

        @Nullable
        @Bindable
        public String getDnses() {
            return dnses;
        }

        public void setDnses(final String dnses) {
            this.dnses = dnses;
            notifyPropertyChanged(BR.dnses);
        }

        @Nullable
        @Bindable
        public String getExcludedApplications() {
            return excludedApplications;
        }

        public void setExcludedApplications(final String excludedApplications) {
            this.excludedApplications = excludedApplications;
            notifyPropertyChanged(BR.excludedApplications);
            notifyPropertyChanged(BR.excludedApplicationsCount);
        }

        @Bindable
        public int getExcludedApplicationsCount() {
            return Attribute.stringToList(excludedApplications).length;
        }

        @Nullable
        @Bindable
        public String getListenPort() {
            return listenPort;
        }

        public void setListenPort(final String listenPort) {
            this.listenPort = listenPort;
            notifyPropertyChanged(BR.listenPort);
        }

        @Nullable
        @Bindable
        public String getMtu() {
            return mtu;
        }

        public void setMtu(final String mtu) {
            this.mtu = mtu;
            notifyPropertyChanged(BR.mtu);
        }

        @Nullable
        @Bindable
        public String getPrivateKey() {
            return privateKey;
        }

        public void setPrivateKey(final String privateKey) {
            this.privateKey = privateKey;

            try {
                publicKey = new Keypair(privateKey).getPublicKey();
            } catch (final IllegalArgumentException ignored) {
                publicKey = "";
            }

            notifyPropertyChanged(BR.privateKey);
            notifyPropertyChanged(BR.publicKey);
        }

        @Nullable
        @Bindable
        public String getPublicKey() {
            return publicKey;
        }

        private void loadData(final Interface parent) {
            addresses = parent.getAddressString();
            dnses = parent.getDnsString();
            excludedApplications = parent.getExcludedApplicationsString();
            publicKey = parent.getPublicKey();
            privateKey = parent.getPrivateKey();
            listenPort = parent.getListenPortString();
            mtu = parent.getMtuString();
        }

        @Override
        public void writeToParcel(final Parcel dest, final int flags) {
            dest.writeString(addresses);
            dest.writeString(dnses);
            dest.writeString(publicKey);
            dest.writeString(privateKey);
            dest.writeString(listenPort);
            dest.writeString(mtu);
            dest.writeString(excludedApplications);
        }
    }
}
