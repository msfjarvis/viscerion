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
    id("io.gitlab.arturbosch.detekt") version "1.0.1"
}

buildSrcVersions {
    indent = "    "
    rejectedVersionKeywords()
}

subprojects {
    apply(file("$rootDir/detekt.gradle"))
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
