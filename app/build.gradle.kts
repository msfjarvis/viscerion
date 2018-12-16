import java.io.FileInputStream
import java.io.IOException
import java.util.Properties
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-android-extensions")
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
        applicationId = "me.msfjarvis.wgandroid"
        minSdkVersion(21)
        targetSdkVersion(28)
        versionCode = 1002
        versionName = "1.0.2"
        buildConfigField("int", "MIN_SDK_VERSION", "21")
        setProperty("archivesBaseName", "wg-android_${gitHash()}")
    }
    compileOptions {
        setSourceCompatibility(JavaVersion.VERSION_1_8)
        setTargetCompatibility(JavaVersion.VERSION_1_8)
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
                storeFile = file(keystoreProperties["storeFile"].toString())
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
                    arguments.add("-DANDROID_PACKAGE_NAME=${android.defaultConfig.applicationId}")
                }
            }
            isMinifyEnabled = false
        }
    }
    externalNativeBuild.cmake {
        path = rootProject.file("$name/tools/CMakeLists.txt")
    }
    lintOptions.isAbortOnError = false
}

dependencies {
    implementation("androidx.annotation:annotation:1.0.1")
    implementation("androidx.appcompat:appcompat:1.0.2")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.core:core-ktx:1.0.1")
    implementation("androidx.databinding:databinding-adapters:3.2.1")
    implementation("androidx.databinding:databinding-runtime:3.2.1")
    implementation("androidx.fragment:fragment-ktx:1.0.0")
    implementation("androidx.preference:preference:1.0.0")
    implementation("com.google.android.material:material:1.0.0")
    implementation("com.jakewharton.timber:timber:4.7.1")
    implementation("com.journeyapps:zxing-android-embedded:3.6.0")
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
