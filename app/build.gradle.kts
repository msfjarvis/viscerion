import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.FileInputStream
import java.io.IOException
import java.util.Properties

apply {
    from("../spotless.gradle")
}

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-kapt")
}

// Create a variable called keystorePropertiesFile, and initialize it to your
// keystore.properties file, in the rootProject folder.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val buildTypeRelease = "release"

fun gitHash(): String {
    try {
        return Runtime.getRuntime().exec("git describe --tags").inputStream.reader().use { it.readText() }.trim()
    } catch (ignored: IOException) {
    }
    return ""
}

android {
    compileSdkVersion(28)
    dataBinding.isEnabled = true
    defaultConfig {
        applicationId = "me.msfjarvis.viscerion"
        minSdkVersion(21)
        targetSdkVersion(28)
        versionCode = 3000
        versionName = "3.0.0"
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
            create(buildTypeRelease) {
                keyAlias = keystoreProperties["keyAlias"].toString()
                keyPassword = keystoreProperties["keyPassword"].toString()
                storeFile = rootProject.file(keystoreProperties["storeFile"].toString())
                storePassword = keystoreProperties["storePassword"].toString()
            }
        }
    }
    buildTypes {
        getByName(buildTypeRelease) {
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
    implementation("androidx.annotation:annotation:1.0.1")
    implementation("androidx.appcompat:appcompat:1.0.2")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.constraintlayout:constraintlayout:1.1.3")
    implementation("androidx.core:core-ktx:1.0.1")
    implementation("androidx.databinding:databinding-adapters:3.3.0")
    implementation("androidx.databinding:databinding-runtime:3.3.0")
    implementation("androidx.fragment:fragment-ktx:1.0.0")
    implementation("androidx.preference:preference:1.0.0")
    implementation("com.google.android.material:material:1.0.0")
    implementation("com.google.zxing:core:3.3.3")
    implementation("com.jakewharton.threetenabp:threetenabp:1.1.1")
    implementation("com.jakewharton.timber:timber:4.7.1")
    implementation("net.sourceforge.streamsupport:android-retrofuture:1.7.0")
    implementation("net.sourceforge.streamsupport:android-retrostreams:1.7.0")
    implementation(kotlin("stdlib-jdk8", KotlinCompilerVersion.VERSION))
}

repositories {
    mavenCentral()
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-Xlint:unchecked")
    options.isDeprecation = true
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

kapt {
    useBuildCache = true
}
