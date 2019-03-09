/*
 * Copyright © 2017-2018 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package me.msfjarvis.viscerion.build

class DependencyStore {

    object AndroidX {
        private const val annotationVersion = "1.0.2"
        private const val appcompatVersion = "1.0.2"
        private const val cardviewVersion = "1.0.0"
        private const val constraintlayoutVersion = "1.1.3"
        private const val coreKtxVersion = "1.0.1"
        private const val databindingVersion = "3.3.1"
        private const val fragmentKtxVersion = "1.0.0"
        private const val preferenceVersion = "1.1.0-alpha03"

        const val annotations = "androidx.annotation:annotation:$annotationVersion"
        const val appcompat = "androidx.appcompat:appcompat:$appcompatVersion"
        const val cardview = "androidx.cardview:cardview:$cardviewVersion"
        const val constraintlayout = "androidx.constraintlayout:constraintlayout:$constraintlayoutVersion"
        const val coreKtx = "androidx.core:core-ktx:$coreKtxVersion"
        const val databindingAdapters = "androidx.databinding:databinding-adapters:$databindingVersion"
        const val databindingRuntime = "androidx.databinding:databinding-runtime:$databindingVersion"
        const val fragmentKtx = "androidx.fragment:fragment-ktx:$fragmentKtxVersion"
        const val preference = "androidx.preference:preference:$preferenceVersion"
    }

    object Material {
        private const val materialVersion = "1.1.0-alpha04"

        const val material = "com.google.android.material:material:$materialVersion"
    }

    object ThirdParty {
        private const val streamsupportVersion = "1.7.0"
        private const val threetenabpVersion = "1.2.0"
        private const val timberVersion = "4.7.1"
        private const val zxingVersion = "3.3.3"

        const val retrofuture = "net.sourceforge.streamsupport:android-retrofuture:$streamsupportVersion"
        const val retrostreams = "net.sourceforge.streamsupport:android-retrostreams:$streamsupportVersion"
        const val threetenabp = "com.jakewharton.threetenabp:threetenabp:$threetenabpVersion"
        const val timber = "com.jakewharton.timber:timber:$timberVersion"
        const val zxing = "com.google.zxing:core:$zxingVersion"
    }
}
