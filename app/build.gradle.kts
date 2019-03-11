import me.msfjarvis.viscerion.build.VersionConfiguration
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.FileInputStream
import java.io.IOException
import java.util.Properties
import me.msfjarvis.viscerion.build.DependencyStore as deps

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
}

// Create a variable called keystorePropertiesFile, and initialize it to your
// keystore.properties file, in the rootProject folder.
val keystorePropertiesFile = rootProject.file("keystore.properties")

fun gitHash(): String {
    return try {
        Runtime.getRuntime().exec("git describe --tags").inputStream.reader().use { it.readText() }.trim()
    } catch (ignored: IOException) {
        ""
    }
}

android {
    compileSdkVersion(28)
    dataBinding.isEnabled = true
    defaultConfig {
        applicationId = "me.msfjarvis.viscerion"
        minSdkVersion(21)
        targetSdkVersion(28)
        versionCode = VersionConfiguration.versionCode
        versionName = VersionConfiguration.versionName
        buildConfigField("String", "GIT_HASH", "\"${gitHash()}\"")
        setProperty("archivesBaseName", "viscerion_${gitHash()}")
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
    }
    buildTypes {
        getByName("release") {
            if (keystorePropertiesFile.exists()) signingConfig = signingConfigs.getByName("release")
            externalNativeBuild {
                cmake {
                    arguments.add("-DANDROID_PACKAGE_NAME=${android.defaultConfig.applicationId}")
                }
            }
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        getByName("debug") {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            externalNativeBuild {
                cmake {
                    arguments.add("-DANDROID_PACKAGE_NAME=${android.defaultConfig.applicationId}$applicationIdSuffix")
                }
            }
            isMinifyEnabled = false
        }
    }
    externalNativeBuild.cmake {
        setPath(file("tools/CMakeLists.txt"))
    }
    lintOptions {
        isAbortOnError = true
        disable("UnusedResources")
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar"))))
    implementation(deps.AndroidX.annotations)
    implementation(deps.AndroidX.appcompat)
    implementation(deps.AndroidX.cardview)
    implementation(deps.AndroidX.constraintlayout)
    implementation(deps.AndroidX.coreKtx)
    implementation(deps.AndroidX.databindingAdapters)
    implementation(deps.AndroidX.databindingRuntime)
    implementation(deps.AndroidX.fragmentKtx)
    implementation(deps.AndroidX.preference)
    implementation(deps.Material.material)
    implementation(deps.ThirdParty.zxing)
    implementation(deps.ThirdParty.threetenabp)
    implementation(deps.ThirdParty.timber)
    implementation(deps.ThirdParty.retrofuture)
    implementation(deps.ThirdParty.retrostreams)
    implementation(embeddedKotlin("stdlib-jdk8"))
}

repositories {
    mavenCentral()
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
        kotlinOptions.jvmTarget = "1.8"
    }
}

afterEvaluate {
    val kotlinCompileTask = tasks.findByName("compileDebugKotlin") ?: tasks.findByName("compileReleaseKotlin")
    val dependencies = mutableListOf()
    dependencies += rootProject.tasks.getByName("spotlessCheck")
    if (dependencies.isNotEmpty()) {
        kotlinCompileTask?.let { task ->
            task.dependsOn.forEach { dependencies += it }
            task.setDependsOn(dependencies)
            task.doFirst {
                println("Removing: ${buildDir.absolutePath + "/outputs/apk/debug/"}")
                delete(buildDir.absolutePath + "/outputs/apk/debug")
            }
        }
    }
}
