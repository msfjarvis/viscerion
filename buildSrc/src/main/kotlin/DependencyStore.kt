/*
 * Copyright © 2017-2018 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
@file:Suppress("Unused")

class DependencyStore {

    object AndroidX {
        private const val annotationVersion = "1.1.0-beta01"
        private const val appcompatVersion = "1.1.0-alpha04"
        private const val constraintlayoutVersion = "2.0.0-alpha4"
        private const val coreKtxVersion = "1.1.0-alpha05"
        private const val databindingVersion = "3.3.2"
        private const val fragmentKtxVersion = "1.1.0-alpha06"
        private const val preferenceVersion = "1.1.0-alpha04"
        private const val slicesVersion = "1.0.0"
        private const val slicesKtxVersion = "1.0.0-alpha6"

        const val annotations = "androidx.annotation:annotation:$annotationVersion"
        const val appcompat = "androidx.appcompat:appcompat:$appcompatVersion"
        const val constraintlayout = "androidx.constraintlayout:constraintlayout:$constraintlayoutVersion"
        const val coreKtx = "androidx.core:core-ktx:$coreKtxVersion"
        const val databindingAdapters = "androidx.databinding:databinding-adapters:$databindingVersion"
        const val databindingRuntime = "androidx.databinding:databinding-runtime:$databindingVersion"
        const val fragmentKtx = "androidx.fragment:fragment-ktx:$fragmentKtxVersion"
        const val preference = "androidx.preference:preference:$preferenceVersion"
        const val sliceBuilders = "androidx.slice:slice-builders:$slicesVersion"
        const val sliceCore = "androidx.slice:slice-core:$slicesVersion"
        const val sliceKtx = "androidx.slice:slice-builders-ktx:$slicesKtxVersion"
    }

    object Material {
        private const val materialVersion = "1.1.0-alpha05"

        const val material = "com.google.android.material:material:$materialVersion"
    }

    object ThirdParty {
        private const val koinVersion = "2.0.0-rc-2"
        private const val streamsupportVersion = "1.7.0"
        private const val threetenabpVersion = "1.2.0"
        private const val timberVersion = "4.7.1"
        private const val zxingVersion = "3.3.3"

        const val koinCore = "org.koin:koin-core:$koinVersion"
        const val koinAndroid = "org.koin:koin-android:$koinVersion"
        const val retrofuture = "net.sourceforge.streamsupport:android-retrofuture:$streamsupportVersion"
        const val retrostreams = "net.sourceforge.streamsupport:android-retrostreams:$streamsupportVersion"
        const val threetenabp = "com.jakewharton.threetenabp:threetenabp:$threetenabpVersion"
        const val timber = "com.jakewharton.timber:timber:$timberVersion"
        const val zxing = "com.google.zxing:core:$zxingVersion"
    }
}
