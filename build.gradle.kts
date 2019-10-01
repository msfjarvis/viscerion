/*
 * Copyright © 2017-2019 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
buildscript {
    repositories {
        google()
        jcenter()
        maven(url = "https://storage.googleapis.com/r8-releases/raw")
    }
    dependencies {
        classpath(Libs.com_android_tools_build_gradle)
        classpath(Libs.kotlin_gradle_plugin)
        classpath(Libs.r8)
    }
}

plugins {
    buildSrcVersions
    id("com.gradle.build-scan") version Versions.com_gradle_build_scan_gradle_plugin
}

buildScan {
    termsOfServiceAgree = "yes"
    termsOfServiceUrl = "https://gradle.com/terms-of-service"
}

buildSrcVersions {
    indent = "    "
}

subprojects {
    repositories {
        google()
        jcenter()
        maven(url = "https://jitpack.io")
    }
}

tasks {
    named<Wrapper>("wrapper") {
        gradleVersion = Versions.gradleLatestVersion
        distributionType = Wrapper.DistributionType.ALL
    }
}

configureSpotless()
