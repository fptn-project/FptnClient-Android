package org.fptn.vpn.gradle

import org.fptn.vpn.gradle.extensions.buildLibs
import org.fptn.vpn.gradle.extensions.implementation
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class KoinConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.google.devtools.ksp")
            }
            dependencies {
                implementation(platform(buildLibs.koin.bom))
                implementation(buildLibs.koin.core)
            }
        }
    }
}
