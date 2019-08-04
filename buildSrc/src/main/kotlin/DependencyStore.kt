/*
 * Copyright © 2017-2019 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
class DependencyStore {

    object AndroidX {
        private const val annotationVersion = "1.1.0"
        private const val appcompatVersion = "1.1.0-rc01"
        private const val constraintlayoutVersion = "2.0.0-beta2"
        private const val coreKtxVersion = "1.2.0-alpha02"
        private const val databindingVersion = "3.5.0-beta05"
        private const val fragmentKtxVersion = "1.2.0-alpha01"
        private const val preferenceVersion = "1.1.0-rc01"
        private const val slicesVersion = "1.1.0-alpha01"
        private const val slicesKtxVersion = "1.0.0-alpha07"
        private const val workVersion = "2.2.0-rc01"

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
        const val workmanager = "androidx.work:work-runtime-ktx:$workVersion"
    }

    object Debugging {
        private const val leakcanaryVersion = "2.0-beta-1"

        const val leakcanary = "com.squareup.leakcanary:leakcanary-android:$leakcanaryVersion"
    }

    object Material {
        private const val materialVersion = "1.1.0-alpha09"

        const val material = "com.google.android.material:material:$materialVersion"
    }

    object ThirdParty {
        private const val koinVersion = "2.0.1"
        private const val recyclicalVersion = "1.0.1"
        private const val retroFutureVersion = "1.7.1"
        private const val threetenabpVersion = "1.2.1"
        private const val timberVersion = "4.7.1"
        private const val zxingVersion = "3.4.0"

        const val koinCore = "org.koin:koin-core:$koinVersion"
        const val koinAndroid = "org.koin:koin-android:$koinVersion"
        const val recyclical = "com.afollestad:recyclical:$recyclicalVersion"
        const val retrofuture = "net.sourceforge.streamsupport:android-retrofuture:$retroFutureVersion"
        const val threetenabp = "com.jakewharton.threetenabp:threetenabp:$threetenabpVersion"
        const val timber = "com.jakewharton.timber:timber:$timberVersion"
        const val zxing = "com.google.zxing:core:$zxingVersion"
    }
}
