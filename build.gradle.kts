/*
 * Copyright © 2017-2018 WireGuard LLC.
 * Copyright © 2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import me.msfjarvis.viscerion.build.configureSpotless

buildscript {
    repositories {
        google()
        jcenter()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:3.3.1")
        classpath(kotlin("gradle-plugin", "1.3.21"))
        classpath("com.diffplug.spotless:spotless-plugin-gradle:3.18.0")
    }
}

plugins {
    id("com.github.ben-manes.versions") version "0.20.0"
}

allprojects {
    repositories {
        google()
        jcenter()
    }
}

tasks.named<DependencyUpdatesTask>("dependencyUpdates") {
    resolutionStrategy {
        componentSelection {
            all {
                val rejected = listOf("alpha", "beta", "rc", "cr", "m", "preview")
                    .map { qualifier -> Regex("(?i).*[.-]$qualifier[.\\d-]*") }
                    .any { it.matches(candidate.version) }
                if (rejected && !(candidate.group == "androidx.preference")) {
                    reject("Release candidate")
                }
            }
        }
    }
    checkForGradleUpdate = true
    outputFormatter = "json"
    outputDir = "build/dependencyUpdates"
    reportfileName = "report"
}

configureSpotless()
