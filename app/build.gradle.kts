/*
 * Copyright © 2017-2019 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.FileInputStream
import java.io.IOException
import java.util.Properties

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
    buildToolsVersion = "29.0.2"
    defaultConfig {
        applicationId = "me.msfjarvis.viscerion"
        minSdkVersion(21)
        targetSdkVersion(29)
        versionCode = VersionConfiguration.versionCode
        versionName = VersionConfiguration.versionName
        if (System.getenv("DRONE") != "true") setProperty("archivesBaseName", "viscerion_${gitHash()}")
        resConfigs("de", "en", "fr", "pt-rBR", "ru")
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
    implementation(project(":crypto"))
    implementation(project(":native"))
    implementation(Libs.android_retrofuture)
    implementation(Libs.annotation)
    implementation(Libs.appcompat)
    implementation(Libs.biometric)
    implementation(Libs.constraintlayout)
    implementation(Libs.core_ktx)
    implementation(Libs.databinding_adapters)
    implementation(Libs.databinding_runtime)
    implementation(Libs.fragment_ktx)
    implementation(Libs.kotlin_stdlib_jdk8)
    implementation(Libs.material)
    implementation(Libs.barcode_kaiteki)
    implementation(Libs.koin_android)
    implementation(Libs.koin_core)
    implementation(Libs.preference)
    implementation(Libs.recyclerview)
    implementation(Libs.recyclical)
    implementation(Libs.slice_builders)
    implementation(Libs.slice_core)
    implementation(Libs.slice_builders_ktx)
    implementation(Libs.threetenabp)
    implementation(Libs.timber)
    implementation(Libs.work_runtime_ktx)
    debugImplementation(Libs.leakcanary_android)
}

kapt {
    useBuildCache = true
    // https://github.com/google/dagger/issues/1449#issuecomment-495404186
    javacOptions {
        option("-source", "8")
        option("-target", "8")
    }
}

tasks {
    withType<JavaCompile> {
        options.compilerArgs.add("-Xlint:unchecked")
        options.isDeprecation = true
    }
    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "1.8"
        }
    }
}
