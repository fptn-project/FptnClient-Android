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

import org.gradle.api.Project
import org.gradle.api.attributes.Bundling
import org.gradle.api.tasks.JavaExec
import org.gradle.kotlin.dsl.creating
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.language.base.plugins.LifecycleBasePlugin

object FormattingOptions {
    fun Project.applyPrecheckOptions() {
        val ktlint by configurations.creating

        dependencies {
            ktlint("com.pinterest.ktlint:ktlint-cli:1.5.0") {
                attributes {
                    attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
                }
            }
            // ktlint(project(":custom-ktlint-ruleset")) // in case of custom ruleset
        }

        tasks.register<JavaExec>("ktlintCheck") {
            group = LifecycleBasePlugin.VERIFICATION_GROUP
            description = "Check Kotlin code style"
            classpath = ktlint
            mainClass.set("com.pinterest.ktlint.Main")
            // see https://pinterest.github.io/ktlint/install/cli/#command-line-usage for more information
            args(
                "**/src/**/*.kt",
                "**.kts",
                "!**/build/**",
            )
        }

        tasks.register<JavaExec>("ktlintFormat") {
            group = LifecycleBasePlugin.VERIFICATION_GROUP
            description = "Check Kotlin code style and format"
            classpath = ktlint
            mainClass.set("com.pinterest.ktlint.Main")
            jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
            // see https://pinterest.github.io/ktlint/install/cli/#command-line-usage for more information
            args(
                "-F",
                "**/src/**/*.kt",
                "**.kts",
                "!**/build/**",
            )
        }
    }
}
