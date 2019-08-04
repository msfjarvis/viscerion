/*
 * Copyright © 2017-2019 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.FileInputStream
import java.io.IOException
import java.util.Properties
import DependencyStore as deps

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
}

// Create a variable called keystorePropertiesFile, and initialize it to your
// keystore.properties file, in the rootProject folder.
val keystorePropertiesFile: File = rootProject.file("keystore.properties")

fun gitHash(): String {
    return try {
        Runtime.getRuntime().exec("git describe --tags").inputStream.reader().use { it.readText() }.trim()
    } catch (ignored: IOException) {
        ""
    }
}

android {
    compileSdkVersion(29)
    dataBinding.isEnabled = true
    defaultConfig {
        applicationId = "me.msfjarvis.viscerion"
        minSdkVersion(21)
        targetSdkVersion(29)
        versionCode = VersionConfiguration.versionCode
        versionName = VersionConfiguration.versionName
        if (System.getenv("DRONE") != "true") setProperty("archivesBaseName", "viscerion_${gitHash()}")
        resConfigs("de", "en", "fr", "ko", "pt-rBR", "ru")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    // If the keystore file exists
    if (keystorePropertiesFile.exists()) {
        // Initialize a new Properties() object called keystoreProperties.
        val keystoreProperties = Properties()

        // Load your keystore.properties file into the keystoreProperties object.
        keystoreProperties.load(FileInputStream(keystorePropertiesFile))

        signingConfigs {
            create("release") {
                keyAlias = keystoreProperties["keyAlias"].toString()
                keyPassword = keystoreProperties["keyPassword"].toString()
                storeFile = rootProject.file(keystoreProperties["storeFile"].toString())
                storePassword = keystoreProperties["storePassword"].toString()
            }
        }
        buildTypes.getByName("release").signingConfig = signingConfigs.getByName("release")
        buildTypes.getByName("debug").signingConfig = signingConfigs.getByName("release")
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            buildConfigField("String", "GIT_HASH", "\"\"")
        }
        getByName("debug") {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isMinifyEnabled = false
            buildConfigField("String", "GIT_HASH", "\"${gitHash()}\"")
        }
    }
    lintOptions {
        isAbortOnError = true
        disable(
            "UnusedResources", // Databinding-only layouts are misinterpreted by Android lint as unused
            "MissingTranslation", // I personally resolve these issues before releases
            "ImpliedQuantity" // Some languages differ between 0 and 1 quantities but I don't use %d in the confirm_tunnel_deletion plural so lint trips
        )
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar"))))
    implementation(project(":crypto"))
    implementation(project(":native"))
    implementation(deps.AndroidX.annotations)
    implementation(deps.AndroidX.appcompat)
    implementation(deps.AndroidX.constraintlayout)
    implementation(deps.AndroidX.coreKtx)
    implementation(deps.AndroidX.databindingAdapters)
    implementation(deps.AndroidX.databindingRuntime)
    implementation(deps.AndroidX.fragmentKtx)
    implementation(deps.AndroidX.preference)
    implementation(deps.AndroidX.sliceBuilders)
    implementation(deps.AndroidX.sliceCore)
    implementation(deps.AndroidX.sliceKtx)
    implementation(deps.AndroidX.workmanager)
    implementation(deps.Material.material)
    implementation(deps.ThirdParty.koinAndroid)
    implementation(deps.ThirdParty.koinCore)
    implementation(deps.ThirdParty.recyclical)
    implementation(deps.ThirdParty.retrofuture)
    implementation(deps.ThirdParty.threetenabp)
    implementation(deps.ThirdParty.timber)
    implementation(deps.ThirdParty.zxing)
    implementation(embeddedKotlin("stdlib-jdk8"))
    debugImplementation(deps.Debugging.leakcanary)
}

kapt {
    useBuildCache = true
}

tasks {
    withType<JavaCompile> {
        options.compilerArgs.add("-Xlint:unchecked")
        options.isDeprecation = true
    }
    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "1.8"
            freeCompilerArgs += "-Xnew-inference"
        }
    }
}
