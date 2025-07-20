// Top-level build file where you can add configuration options common to all sub-projects/modules.
import org.fptn.vpn.gradle.DetektOptions.applyDetektOptions
import org.fptn.vpn.gradle.FormattingOptions.applyPrecheckOptions

data class VersionInfo(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val build: Int,
)

fun parseVersionTag(versionTag: String?): VersionInfo {
    if (versionTag.isNullOrBlank()) {
        return VersionInfo(0, 0, 0, 1)
    }
    val cleanTag = versionTag.removePrefix("v")
    val parts = cleanTag.split(".").mapNotNull { it.toIntOrNull() }
    return when (parts.size) {
        1 -> VersionInfo(parts[0], 0, 0, 1)
        2 -> VersionInfo(parts[0], parts[1], 0, 1)
        3 -> VersionInfo(parts[0], parts[1], parts[2], 1)
        4 -> VersionInfo(parts[0], parts[1], parts[2], parts[3])
        else -> VersionInfo(0, 0, 0, 1)
    }
}

val versionTag: String? = System.getenv("RELEASE_VERSION") ?: System.getenv("VERSION_TAG")
val versionInfo = parseVersionTag(versionTag)

println(
    """
    Version info:
    - Major: ${versionInfo.major}
    - Minor: ${versionInfo.minor}
    - Patch: ${versionInfo.patch}
    - Build: ${versionInfo.build}
    """.trimIndent(),
)

buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://plugins.gradle.org/m2/")
    }
    dependencies {
        classpath(libs.firebase.crashlytics.gradle)
        classpath(libs.google.services)
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
}

repositories {
    mavenCentral()
}

extra["compileSdkVersion"] = 35
extra["minSdkVersion"] = 28
extra["targetSdkVersion"] = 35
extra["versionMajor"] = versionInfo.major
extra["versionMinor"] = versionInfo.minor
extra["versionPatch"] = versionInfo.patch
extra["versionBuild"] = versionInfo.build

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}

plugins {
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.crashlytics) apply false
    alias(libs.plugins.deps.sorting) apply false
    alias(libs.plugins.deps.unused) apply true
}

applyPrecheckOptions()
applyDetektOptions()

subprojects {
    apply(plugin = "com.squareup.sort-dependencies")
}

dependencyAnalysis {
    val fail = "fail"
    val ignore = "ignore"
    issues {
        all {
            onUnusedDependencies { severity(fail) }
            onUsedTransitiveDependencies { severity(ignore) }
            onIncorrectConfiguration { severity(ignore) }
            onCompileOnly { severity(ignore) }
            onRuntimeOnly { severity(ignore) }
            onUnusedAnnotationProcessors { severity(ignore) }
            onRedundantPlugins { severity(ignore) }
        }
    }
}
