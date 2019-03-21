/*
 * Copyright © 2017-2018 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.gradle.spotless.SpotlessPlugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

val kotlinLicenseHeader = """/*
 * Copyright © 2017-2018 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
""".trimIndent()

fun Project.configureSpotless() {
    apply<SpotlessPlugin>()

    configure<SpotlessExtension> {
        format("misc") {
            target(
                fileTree(
                    mapOf(
                        "dir" to ".",
                        "include" to listOf("**/*.md", "**/.gitignore", "**/*.yaml", "**/*.yml"),
                        "exclude" to listOf(".gradle/**", ".gradle-cache/**", "**/tools/**", "**/build/**")
                    )
                )
            )
            trimTrailingWhitespace()
            indentWithSpaces()
            endWithNewline()
        }

        format("xml") {
            target("**/res/**/*.xml")
            indentWithSpaces(4)
            trimTrailingWhitespace()
            endWithNewline()
        }

        java {
            target(
                "app/src/main/java/com/wireguard/crypto/Key.java",
                "app/src/main/java/com/wireguard/android/widget/KeyInputFilter.java",
                "app/src/main/java/com/wireguard/android/util/ObservableSortedKeyedArrayList.java",
                "app/src/main/java/com/wireguard/android/util/ObservableKeyedArrayList.java",
                "app/src/main/java/com/wireguard/android/util/KotlinCompanions.java"
            )
            trimTrailingWhitespace()
            @Suppress("INACCESSIBLE_TYPE")
            licenseHeader(kotlinLicenseHeader)
            removeUnusedImports()
            googleJavaFormat().aosp()
            endWithNewline()
        }

        kotlinGradle {
            target("*.gradle.kts", "gradle/*.gradle.kts", "buildSrc/*.gradle.kts")
            ktlint("0.29.0").userData(mapOf("indent_size" to "4", "continuation_indent_size" to "4"))
            @Suppress("INACCESSIBLE_TYPE")
            licenseHeader(kotlinLicenseHeader, "import|tasks|apply|plugins|include")
            trimTrailingWhitespace()
            indentWithSpaces()
            endWithNewline()
        }

        kotlin {
            target("**/src/**/*.kt", "buildSrc/**/*.kt")
            ktlint("0.29.0").userData(mapOf("indent_size" to "4", "continuation_indent_size" to "4"))
            @Suppress("INACCESSIBLE_TYPE")
            licenseHeader(kotlinLicenseHeader, "import|package|class|object|@file")
            trimTrailingWhitespace()
            indentWithSpaces()
            endWithNewline()
        }
    }
}
