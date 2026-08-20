import java.io.FileInputStream
import java.io.InputStream
import java.net.URL
import java.util.Properties
import java.util.zip.GZIPOutputStream
import kotlin.concurrent.thread

plugins {
    id("pvnclient.android.application")
    // Google/Firebase plugins are applied conditionally below — only when a `gms`
    // variant is built. This keeps the `foss` APK free of Firebase/Crashlytics and
    // means `foss` builds don't require google-services.json.
    id("com.google.gms.google-services") apply false
    alias(libs.plugins.crashlytics) apply false
}

// Apply the Google/Firebase plugins only when a `gms` variant is being built
// (e.g. `bundleGmsRelease` -> AAB for Google Play). `foss` builds
// (e.g. `assembleFossRelease` -> APK for sideloading) skip them entirely.
val isGmsBuild = gradle.startParameter.taskRequests.any { request ->
    request.args.any { it.contains("Gms", ignoreCase = true) }
}
if (isGmsBuild) {
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
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
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
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
    flavorDimensions += "distribution"
    productFlavors {
        // With Google/Firebase (Analytics + Crashlytics). Build the AAB from this
        // flavor for Google Play: `bundleGmsRelease`.
        create("gms") {
            dimension = "distribution"
        }
        // No Google/Firebase code at all. Build the APK from this flavor for
        // sideloading / non-GMS devices: `assembleFossRelease`.
        create("foss") {
            dimension = "distribution"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = true
        }
    }

    externalNativeBuild {
        cmake {
            // version = "3.31.6"
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }
}

dependencies {
    // Firebase only in the `gms` flavor (AAB). The `foss` flavor (APK) has none of it.
    "gmsImplementation"(platform(libs.firebase.bom))
    "gmsImplementation"(libs.firebase.analytics)
    "gmsImplementation"(libs.firebase.crashlytics.ndk)
    implementation(project(":core:common"))
    implementation(project(":vpnclient"))
    implementation(libs.androidx.activity)
    implementation(libs.androidx.appcompat)
    // To use CallbackToFutureAdapter
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.monitor)
    implementation(libs.androidx.room.guava)
    implementation(libs.androidx.room.runtime)
    implementation(libs.decoding)
    implementation(libs.guava)
    implementation(libs.ipaddress)
    implementation(libs.jackson.databind)
    implementation(libs.material)
    implementation(libs.zxing)
    implementation(libs.gson)
    implementation(libs.xlog)

    compileOnly(libs.androidlombock)

    annotationProcessor(libs.androidlombock)
    annotationProcessor(libs.androidx.room.compiler)

    testImplementation(libs.assertj.core)
    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.rules)
    androidTestImplementation(libs.androidx.runner)
    androidTestImplementation(libs.androidx.testing)
    androidTestImplementation(libs.assertj.core)
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
        val conanBuildDir = layout.buildDirectory.dir("conan").get().asFile.apply { mkdirs() }
        listOf("Debug", "Release", "RelWithDebInfo").forEach { buildType ->
            listOf("armv8", "armv7", "x86_64").forEach { arch ->
                val process = ProcessBuilder(
                    "conan", "install", "$projectDir/src/main/cpp",
                    "--profile", "$rootDir/conan/profiles/android-studio",
                    "-s", "build_type=$buildType",
                    "-s", "arch=$arch",
                    "--build", "missing",
                    "-c", "tools.cmake.cmake_layout:build_folder_vars=['settings.arch']",
                ).directory(conanBuildDir).inheritIO().start()
                val exitCode = process.waitFor()
                if (exitCode != 0) throw GradleException("conan install failed [buildType=$buildType, arch=$arch]")
            }
        }
    }
}
tasks.register("downloadBlocklist") {
    group = "build"
    description = "Downloads StevenBlack unified hosts list and bundles it as a gzipped asset"

    val outputFile = file("src/main/res/raw/blocklist.gz")
    outputs.file(outputFile)
    // Re-download only if the cached file is older than 7 days
    outputs.upToDateWhen {
        val maxAgeMs = 7L * 24 * 60 * 60 * 1000
        outputFile.exists() && (System.currentTimeMillis() - outputFile.lastModified()) < maxAgeMs
    }

    doLast {
        outputFile.parentFile.mkdirs()
        val sources = listOf(
            "https://raw.githubusercontent.com/hagezi/dns-blocklists/main/domains/ultimate.txt",
            "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts"
        )
        var anySuccess = false
        val tmpFile = File(outputFile.path + ".tmp")
        GZIPOutputStream(tmpFile.outputStream()).bufferedWriter().use { writer ->
            for (src in sources) {
                try {
                    println("[blocklist] Downloading $src ...")
                    URL(src).openStream().use { input: InputStream ->
                        input.bufferedReader().forEachLine { line ->
                            writer.write(line)
                            writer.newLine()
                        }
                    }
                    anySuccess = true
                    println("[blocklist] OK: $src")
                } catch (e: Exception) {
                    println("[blocklist] FAILED $src: ${e.message}")
                }
            }
        }
        if (!anySuccess) {
            tmpFile.delete()
            throw GradleException("[blocklist] All sources failed and no cached file found")
        }
        tmpFile.renameTo(outputFile)
        if (!anySuccess) throw GradleException("[blocklist] All sources failed and no cached file found")
        println("[blocklist] Saved to ${outputFile.path} (${outputFile.length() / 1024} KB)")
    }
}

tasks.named("preBuild") { dependsOn("conanInstall", "downloadBlocklist") }
afterEvaluate {
    tasks.matching { t ->
        t.name.endsWith("Resources") || t.name.endsWith("SourceSetPaths") || t.name.endsWith("NavigationResources")
    }.configureEach {
        dependsOn("downloadBlocklist")
    }
}
