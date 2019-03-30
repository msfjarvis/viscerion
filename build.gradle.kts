/*
 * Copyright © 2017-2018 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.gradle.api.tasks.wrapper.Wrapper.DistributionType

buildscript {
    repositories {
        google()
        jcenter()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:3.3.2")
        classpath(kotlin("gradle-plugin", "1.3.21"))
        classpath("com.diffplug.spotless:spotless-plugin-gradle:3.18.0")
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
    wrapper {
        gradleVersion = "5.3.1"
        distributionType = DistributionType.ALL
    }
}

configureSpotless()
