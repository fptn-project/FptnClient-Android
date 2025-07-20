import java.io.FileInputStream
import java.io.InputStream
import java.util.Properties
import kotlin.concurrent.thread

plugins {
    id("org.fptn.vpn.application")
    id("org.fptn.vpn.application.compose")
    id("org.fptn.vpn.application.koin")
    id("com.google.gms.google-services")
    alias(libs.plugins.crashlytics)
}

val keystorePropertiesFile: File = rootProject.file("keystore.properties")

android {
    namespace = "org.fptn.vpn"
    compileSdk = rootProject.extra.get("compileSdkVersion") as Int
    ndkVersion = "28.1.13356709"

    var isCI = System.getenv("KEY_ALIAS") != null

    signingConfigs {
        create("release") {
            if (isCI) {
                keyAlias = System.getenv("KEY_ALIAS") ?: ""
                keyPassword = System.getenv("KEY_PASSWORD") ?: ""
                storeFile = file(System.getenv("KEYSTORE_PATH") ?: "android-keystore.jks")
                storePassword = System.getenv("STORE_PASSWORD") ?: ""
            } else {
                if (keystorePropertiesFile.exists()) {
                    val keystoreProperties = Properties()
                    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
                    keyAlias = keystoreProperties["keyAlias"] as String
                    keyPassword = keystoreProperties["keyPassword"] as String
                    storeFile = file(keystoreProperties["storeFile"]!!)
                    storePassword = keystoreProperties["storePassword"] as String
                } else {
                    println(
                        "Warning: keystore.properties file not found. " +
                            "Release signing configuration will not be applied.",
                    )
                }
            }
        }
    }

    defaultConfig {
        applicationId = "org.fptn.vpn"
        val versionMajor: Int by rootProject.extra
        val versionMinor: Int by rootProject.extra
        val versionPatch: Int by rootProject.extra
        val versionBuild: Int by rootProject.extra
        versionCode =
            1000 * (1000 * versionMajor + 100 * versionMinor + versionPatch) + versionBuild
        versionName = "$versionMajor.$versionMinor.$versionPatch.$versionBuild"

        minSdk = rootProject.extra.get("minSdkVersion") as Int
        targetSdk = rootProject.extra.get("targetSdkVersion") as Int

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables.useSupportLibrary = true

        externalNativeBuild {
            cmake {
                cppFlags("-v")
                arguments("-DCMAKE_TOOLCHAIN_FILE=conan_android_toolchain.cmake")
            }
        }

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            isDebuggable = false
            if (isCI || keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isMinifyEnabled = false
            isDebuggable = true
            manifestPlaceholders["appName"] = "FPTN VPN debug"
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
    }

    externalNativeBuild {
        cmake {
            // version = "3.31.6"
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }
}

dependencies {
    implementation(platform(libs.firebase.bom))
    implementation(project(":auth:domain"))
    implementation(project(":auth:ui"))
    implementation(project(":core:common"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:model"))
    implementation(project(":core:network"))
    implementation(project(":core:persistent"))
    implementation(project(":home:ui"))
    implementation(project(":settings:ui"))
    implementation(project(":vpnclient"))
    implementation(libs.androidx.activity)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.monitor)
    implementation(libs.androidx.room.guava)
    implementation(libs.androidx.room.runtime)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics.ndk)
    implementation(libs.guava)
    implementation(libs.ipaddress)
    implementation(libs.jackson.databind)
    implementation(libs.koin.android)
    implementation(libs.material)
    implementation(libs.zxing)

    compileOnly(libs.lombock)

    annotationProcessor(libs.androidx.room.compiler)
    annotationProcessor(libs.lombock)

    testImplementation(libs.junit)
    testImplementation(libs.koin.test)

    androidTestImplementation(libs.androidx.junit)
}
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

fun readStreamAsync(
    stream: InputStream,
    label: String,
) = thread {
    stream.bufferedReader().useLines { lines ->
        lines.forEach { println("[$label] $it") }
    }
}

tasks.register("conanInstall") {
    group = "c++"
    doLast {
        val buildDir = file("$buildDir/conan").apply { mkdirs() }
        listOf("Debug", "Release", "RelWithDebInfo").forEach { buildType ->
            listOf("armv8", "armv7").forEach { arch ->
                exec {
                    workingDir = buildDir
                    commandLine(
                        "conan",
                        "install",
                        "$projectDir/src/main/cpp",
                        "--profile",
                        "$rootDir/conan/profiles/android-studio",
                        "-s",
                        "build_type=$buildType",
                        "-s",
                        "arch=$arch",
                        "--build",
                        "missing",
                        "-c",
                        "tools.cmake.cmake_layout:build_folder_vars=['settings.arch']",
                    )
                }
            }
        }
    }
}
tasks.named("preBuild") { dependsOn("conanInstall") }
