import java.io.FileInputStream
import java.io.InputStream
import java.net.URL
import java.util.ArrayDeque
import java.util.Properties
import java.util.zip.GZIPOutputStream
import kotlin.concurrent.thread

plugins {
    id("pvnclient.android.application")
    // Jetpack Compose compiler (Kotlin 2.x). Applied here so the module can build
    // Compose UI; the Kotlin Android plugin itself comes from the convention plugin.
    alias(libs.plugins.kotlin.compose)
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
        compose = true
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
    implementation(libs.androidx.activity)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    // Jetpack Compose (single-activity UI). Shared by both `gms` and `foss` flavors.
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.runtime.livedata)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.navigation.compose)
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

    debugImplementation(libs.androidx.compose.ui.tooling)
}
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

fun resolveConanExecutable(): String {
    if (project.hasProperty("conan.path")) {
        val customPath = project.property("conan.path") as String
        val customFile = File(customPath)
        if (customFile.exists() && customFile.canExecute()) {
            return customFile.absolutePath
        }
    }
    System.getenv("CONAN_BIN")?.let { envPath ->
        val envFile = File(envPath)
        if (envFile.exists() && envFile.canExecute()) {
            return envFile.absolutePath
        }
    }
    val userHome = System.getProperty("user.home")
    val candidateDirs = (System.getenv("PATH") ?: "")
        .split(File.pathSeparator)
        .toMutableList()
    candidateDirs.add("$userHome/.local/bin")
    candidateDirs.add("/usr/local/bin")
    candidateDirs.add("/opt/homebrew/bin")
    candidateDirs.add("/tmp/conan-env/bin")

    for (dir in candidateDirs) {
        val candidate = File(dir, "conan")
        if (candidate.isFile && candidate.canExecute()) {
            return candidate.absolutePath
        }
    }
    return "conan"
}

fun resolveNdkDirectory(): File? {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        val props = Properties()
        localPropertiesFile.inputStream().use { props.load(it) }
        val ndkDirProp = props.getProperty("ndk.dir")
        if (!ndkDirProp.isNullOrBlank()) {
            val file = File(ndkDirProp)
            if (file.isDirectory) return file
        }
    }
    val envNdk = System.getenv("ANDROID_NDK_HOME") ?: System.getenv("ANDROID_NDK_ROOT")
    if (!envNdk.isNullOrBlank()) {
        val file = File(envNdk)
        if (file.isDirectory) return file
    }
    val sdkHome = System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")
    if (!sdkHome.isNullOrBlank()) {
        val candidate = File(sdkHome, "ndk/28.1.13356709")
        if (candidate.isDirectory) return candidate
        val ndkParent = File(sdkHome, "ndk")
        if (ndkParent.isDirectory) {
            val installed = ndkParent.listFiles { f -> f.isDirectory }
            if (!installed.isNullOrEmpty()) {
                installed.sortByDescending { it.name }
                return installed.first()
            }
        }
    }
    return null
}

tasks.register("conanInstall") {
    group = "c++"
    description = "Installs Conan C++ dependencies for the native FPTN library"
    doLast {
        val conanBuildDir = layout.buildDirectory.dir("conan").get().asFile.apply { mkdirs() }
        val conanExe = resolveConanExecutable()

        val defaultProfileFile = File(System.getProperty("user.home"), ".conan2/profiles/default")
        if (!defaultProfileFile.exists()) {
            println("[conan] 'default' profile not found, generating with 'conan profile detect --force'...")
            val detectBuilder = ProcessBuilder(conanExe, "profile", "detect", "--force")
                .redirectErrorStream(true)
            val p = detectBuilder.start()
            p.inputStream.bufferedReader().useLines { lines ->
                lines.forEach { println("[conan-profile] $it") }
            }
            p.waitFor()
        }

        val requestedBuildTypes = if (project.hasProperty("conanBuildTypes")) {
            (project.property("conanBuildTypes") as String).split(",").map { it.trim() }
        } else {
            val taskArgs = gradle.startParameter.taskRequests.flatMap { it.args }
            val hasDebug = taskArgs.any { it.contains("Debug", ignoreCase = true) }
            val hasRelease = taskArgs.any { it.contains("Release", ignoreCase = true) }
            when {
                hasDebug && !hasRelease -> listOf("Debug")
                hasRelease && !hasDebug -> listOf("Release")
                else -> listOf("Debug", "Release")
            }
        }

        val requestedArchs = if (project.hasProperty("conanArchs")) {
            (project.property("conanArchs") as String).split(",").map { it.trim() }
        } else {
            listOf("armv8", "armv7", "x86_64")
        }

        val ndkDir = resolveNdkDirectory()
        if (ndkDir != null) {
            println("[conan] Local Android NDK detected: ${ndkDir.absolutePath}")
        } else {
            println("[conan] Local Android NDK not detected; Conan will use profile tool_requires")
        }

        val conanDir = File(conanExe).parentFile
        val extraPath = if (conanDir != null && conanDir.isDirectory) {
            conanDir.absolutePath + File.pathSeparator + (System.getProperty("user.home") + "/.local/bin")
        } else {
            System.getProperty("user.home") + "/.local/bin"
        }

        requestedBuildTypes.forEach { buildType ->
            requestedArchs.forEach { arch ->
                println("[conan] Installing dependencies [buildType=$buildType, arch=$arch]...")
                val cmd = mutableListOf(
                    conanExe, "install", "$projectDir/src/main/cpp",
                    "--profile", "$rootDir/conan/profiles/android-studio",
                    "-s", "build_type=$buildType",
                    "-s", "arch=$arch",
                    "--build", "missing",
                    "-c", "tools.cmake.cmake_layout:build_folder_vars=['settings.arch']",
                )
                if (ndkDir != null && ndkDir.isDirectory) {
                    cmd.add("-c")
                    cmd.add("tools.android:ndk_path=${ndkDir.absolutePath}")
                }

                val builder = ProcessBuilder(cmd)
                    .directory(conanBuildDir)
                    .redirectErrorStream(true)

                val env = builder.environment()
                val currentPath = env["PATH"] ?: ""
                env["PATH"] = extraPath + File.pathSeparator + currentPath
                if (ndkDir != null) {
                    env["ANDROID_NDK_HOME"] = ndkDir.absolutePath
                    env["ANDROID_NDK_ROOT"] = ndkDir.absolutePath
                }

                val process = builder.start()
                val recentLines = ArrayDeque<String>(40)
                process.inputStream.bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        println("[$buildType-$arch] $line")
                        if (recentLines.size >= 40) recentLines.removeFirst()
                        recentLines.addLast(line)
                    }
                }
                val exitCode = process.waitFor()
                if (exitCode != 0) {
                    val lastOutput = recentLines.joinToString("\n")
                    throw GradleException(
                        """
                        conan install failed [buildType=$buildType, arch=$arch] (exit code $exitCode).
                        Command: ${cmd.joinToString(" ")}
                        Recent Conan output:
                        $lastOutput
                        """.trimIndent(),
                    )
                }
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
            // Disabled: hagezi ultimate adds ~257k domains on top! It causes memory issues
            // "https://raw.githubusercontent.com/hagezi/dns-blocklists/main/wildcard/ultimate-onlydomains.txt",
            "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts"
        )
        val maxAttempts = 10
        val retryDelayMs = 5_000L

        val contents = sources.map { src ->
            var content: String? = null
            for (attempt in 1..maxAttempts) {
                println("[blocklist] downloading $src (attempt $attempt/$maxAttempts)")
                try {
                    content = URL(src).readText()
                    break
                } catch (e: Exception) {
                    println("[blocklist] attempt $attempt failed: ${e.message}")
                    if (attempt != maxAttempts) Thread.sleep(retryDelayMs)
                }
            }
            content ?: throw GradleException("[blocklist] failed $src after $maxAttempts attempts")
        }

        val tmpFile = File(outputFile.path + ".tmp")
        GZIPOutputStream(tmpFile.outputStream()).bufferedWriter().use { writer ->
            for (content in contents) {
                writer.write(content)
                if (!content.endsWith("\n")) writer.newLine()
            }
        }
        tmpFile.renameTo(outputFile)
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
