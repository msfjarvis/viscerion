/*
 * Copyright © 2017-2018 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

buildscript {
    repositories {
        google()
        jcenter()
        maven(url = "https://storage.googleapis.com/r8-releases/raw")
    }
    dependencies {
        classpath("com.android.tools.build:gradle:3.3.2")
        classpath(kotlin("gradle-plugin", "1.3.30"))
        classpath("com.diffplug.spotless:spotless-plugin-gradle:3.21.1")
        classpath("com.android.tools:r8:1.4.79")
    }
}

plugins {
    id("com.github.ben-manes.versions") version "0.21.0"
}

allprojects {
    repositories {
        google()
        jcenter()
    }
}

tasks {
    named<DependencyUpdatesTask>("dependencyUpdates") {
        checkForGradleUpdate = true
        outputFormatter = "json"
        outputDir = "build/dependencyUpdates"
        reportfileName = "report"
    }
    named<Wrapper>("wrapper") {
        gradleVersion = "5.4-rc-1"
        distributionType = Wrapper.DistributionType.ALL
    }
}

configureSpotless()
