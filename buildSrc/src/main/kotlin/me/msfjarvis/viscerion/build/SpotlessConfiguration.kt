/*
 * Copyright © 2017-2018 WireGuard LLC.
 * Copyright © 2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package me.msfjarvis.viscerion.build

import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.gradle.spotless.SpotlessPlugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

val kotlinLicenseHeader = """/*
 * Copyright © 2017-2018 WireGuard LLC.
 * Copyright © 2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
""".trimIndent()

fun Project.configureSpotless() {
    apply<SpotlessPlugin>()

    configure<SpotlessExtension>() {
        format("misc") {
            target(
                fileTree(
                    mapOf(
                        "dir" to ".",
                        "include" to listOf("**/*.md", "**/.gitignore", "**/*.yaml", "**/*.yml"),
                        "exclude" to listOf(".gradle/**", ".gradle-cache/**", "**/tools/**")
                    )
                )
            )
            trimTrailingWhitespace()
            indentWithSpaces()
            endWithNewline()
        }

        java {
            target("**/*.java")
            trimTrailingWhitespace()
            removeUnusedImports()
            googleJavaFormat().aosp()
            endWithNewline()
        }

        kotlinGradle {
            target("*.gradle.kts", "gradle/*.gradle.kts", "buildSrc/*.gradle.kts")
            ktlint("0.29.0")
            licenseHeader(kotlinLicenseHeader, "import|tasks|apply|plugins|include")
            trimTrailingWhitespace()
            indentWithSpaces()
            endWithNewline()
        }

        kotlin {
            target("src/**/*.kt", "buildSrc/**/*.kt")
            ktlint("0.29.0")
            licenseHeader(kotlinLicenseHeader)
            trimTrailingWhitespace()
            indentWithSpaces()
            endWithNewline()
        }
    }
}
