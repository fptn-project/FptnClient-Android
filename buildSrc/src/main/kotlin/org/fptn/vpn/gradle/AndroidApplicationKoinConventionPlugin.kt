package org.fptn.vpn.gradle

import com.android.build.api.dsl.ApplicationExtension
import org.fptn.vpn.gradle.extensions.configureAndroidKoin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType

class AndroidApplicationKoinConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.application")
            }
            val extension = extensions.getByType<ApplicationExtension>()
            configureAndroidKoin(extension)
        }
    }
}
