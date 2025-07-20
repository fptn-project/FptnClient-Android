package org.fptn.vpn.gradle

import com.android.build.api.dsl.ApplicationExtension
import org.fptn.vpn.gradle.extensions.buildLibs
import org.fptn.vpn.gradle.extensions.configureAndroidFirebase
import org.fptn.vpn.gradle.extensions.configureKotlinAndroid
import org.fptn.vpn.gradle.extensions.implementation
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.getByType

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.application")
                apply("org.jetbrains.kotlin.android")
                apply("com.autonomousapps.dependency-analysis")
            }

            extensions.configure<ApplicationExtension> {
                configureKotlinAndroid(this)
                defaultConfig.targetSdk = rootProject.extra.get("targetSdkVersion") as Int
            }
            val extension = extensions.getByType<ApplicationExtension>()
            configureAndroidFirebase(extension)
            dependencies {
                implementation(buildLibs.timber)
            }
        }
    }
}
