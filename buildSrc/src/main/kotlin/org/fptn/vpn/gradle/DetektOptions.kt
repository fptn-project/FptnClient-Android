/*
 * FPTN Android Client
 * Copyright (C) 2026  Skokov Stanislav, Enin Sergey
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Website: https://fptn.org
 */

package org.fptn.vpn.gradle

import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask
import org.fptn.vpn.gradle.task.TomlFileValidationTask
import org.gradle.api.Project
import org.gradle.api.file.ProjectLayout
import org.gradle.api.tasks.SourceTask
import org.gradle.kotlin.dsl.register

object DetektOptions {
    private const val CONFIG_FILE = "detekt.yml"
    private const val BASELINE_FILE = "baseline.xml"

    fun Project.applyDetektOptions() {
        plugins.apply("io.gitlab.arturbosch.detekt")

        val configFolder = file("${layout.projectDirectory}/config/detekt/")

        tasks.register<Detekt>("detektCheck") {
            configureCommon()
            config.setFrom(configFolder.resolve(CONFIG_FILE))
            baseline.set(configFolder.resolve(BASELINE_FILE))
        }

        tasks.register<Detekt>("detektCheckStrict") {
            configureCommon()
            buildUponDefaultConfig = true
        }

        tasks.register<DetektCreateBaselineTask>("detektCreateBaseline") {
            config.setFrom(configFolder.resolve(CONFIG_FILE))
            baseline.set(configFolder.resolve(BASELINE_FILE))

            configureSources(layout)

            parallel.set(true)
        }

        tasks.register<TomlFileValidationTask>("tomlCheck")
    }

    private fun SourceTask.configureSources(layout: ProjectLayout) {
        setSource(layout.projectDirectory)

        include("**/*.kt")
        include("**/*.kts")

        exclude("**/analytics/impl/*")
        exclude("**/analytics/pojo/*")

        exclude("**/build/**")
        exclude("**/resources/**")

        // Exclude gradle cache directory in CI
        exclude("**/.gradle/**")
    }

    private fun Detekt.configureCommon() {
        val outputFolder = project.file("${project.layout.buildDirectory.get()}/reports/detekt/")

        basePath = project.layout.projectDirectory.asFile.absolutePath // required for CI reporting

        configureSources(project.layout)

        parallel = true

        reports {
            html {
                required.set(true)
                outputLocation.set(outputFolder.resolve("detekt-report.html"))
            }

            // disable unwanted report formats
            sarif { required.set(false) }
            txt { required.set(false) }
            md { required.set(false) }
        }
    }
}
