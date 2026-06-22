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

package org.fptn.vpn.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.TaskAction
import java.io.File

@CacheableTask
abstract class TomlFileValidationTask : DefaultTask() {
    @TaskAction
    fun action() {
        val directory = File(DIR_PATH)
        val files = directory.listFiles()?.filter { it.extension == FILE_EXTENSION } ?: emptyList()
        for (file in files) {
            val fileName = file.name
            val content =
                file
                    .readText(Charsets.UTF_8)
                    .split("\n")
                    .filter { it.isNotEmpty() }
            checkBlockOrder(fileName, content)
            val anchors = getAllAnchors(content)
            for (i in 1 until anchors.size) {
                val firstAnchor: String = anchors[i - 1]
                val lastAnchor = anchors[i]
                val firstAnchorIndex = content.indexOf(firstAnchor)
                val lastAnchorIndex = content.indexOf(lastAnchor)
                val contentBetween = content.subList(firstAnchorIndex + 1, lastAnchorIndex - 1)
                when (firstAnchor) {
                    ANCHOR_VERSIONS -> checkVersions(fileName, contentBetween)
                    ANCHOR_LIBRARIES, ANCHOR_PLUGINS -> checkContent(fileName, contentBetween)
                    ANCHOR_BUNDLES -> {}
                }
            }
            val firstAnchor = anchors[anchors.size - 1]
            val firstAnchorIndex = content.indexOf(firstAnchor)
            val contentBetween = content.subList(firstAnchorIndex + 1, content.size)
            when (firstAnchor) {
                ANCHOR_VERSIONS -> checkVersions(fileName, contentBetween)
                ANCHOR_LIBRARIES, ANCHOR_PLUGINS -> checkContent(fileName, contentBetween)
                ANCHOR_BUNDLES -> {}
            }
        }
    }

    private fun getAllAnchors(content: List<String>): List<String> = content.filter { a: String -> anchors.contains(a) }

    private fun checkBlockOrder(
        fileName: String,
        content: List<String>,
    ) {
        val versionsIndex = content.indexOf(ANCHOR_VERSIONS)
        val librariesIndex = content.indexOf(ANCHOR_LIBRARIES)
        val bundlesIndex = content.indexOf(ANCHOR_BUNDLES)
        val pluginsIndex = content.indexOf(ANCHOR_PLUGINS)

        val curAnchorList = mutableListOf<Pair<String, Int>>()
        if (versionsIndex != -1) curAnchorList.add(ANCHOR_VERSIONS to versionsIndex)
        if (librariesIndex != -1) curAnchorList.add(ANCHOR_LIBRARIES to librariesIndex)
        if (bundlesIndex != -1) curAnchorList.add(ANCHOR_BUNDLES to bundlesIndex)
        if (pluginsIndex != -1) curAnchorList.add(ANCHOR_PLUGINS to pluginsIndex)
        val sortedCurAnchorsListOfPair = curAnchorList.sortedBy { it.second }
        if (sortedCurAnchorsListOfPair[0].second !=
            0
        ) {
            showError(
                fileName,
                "Please, remove any content" +
                    " before block ${curAnchorList[0].first}",
            )
        }
        val sortedCurAnchorsList = sortedCurAnchorsListOfPair.map { it.first }
        val allAnchors = anchors.toMutableList()
        allAnchors.removeAll { !sortedCurAnchorsList.contains(it) }
        val newLine = "\n   ...\n"
        for (i in allAnchors.indices) {
            if (allAnchors[i] != sortedCurAnchorsList[i]) {
                showError(
                    fileName,
                    "Please put blocks in current order: \n" +
                        "   [versions]" + newLine +
                        "   [libraries]" + newLine +
                        "   [bundles]" + newLine +
                        "   [plugins]\n",
                )
            }
        }
    }

    private fun checkVersions(
        fileName: String,
        content: List<String>,
    ) {
        for (version in content) checkVersionName(fileName, version)
        checkAlphabeticalOrder(fileName, content)
    }

    private fun checkVersionName(
        fileName: String,
        name: String,
    ) {
        val versionName = name.substringBefore('=').trim()
        if (versionName.contains("-") or versionName.contains("_")) {
            showError(
                fileName,
                "Please, use camelCase for version name $versionName",
            )
        }
        if (versionName.contains(
                "version",
                true,
            )
        ) {
            showError(
                fileName,
                "Please, don't use `version` " +
                    "suffix in version name $versionName",
            )
        }
    }

    private fun checkAlphabeticalOrder(
        fileName: String,
        strings: List<String>,
    ) {
        strings
            .filter {
                it.startsWith("#").not()
            }.let {
                for (i in 1 until it.size) {
                    val prevString = it[i - 1].lowercase()
                    val curString = it[i].lowercase()
                    if (prevString >= curString) {
                        showError(
                            fileName,
                            "Name $curString is out of order. Please, " +
                                "keep it in alphabetical order.",
                        )
                    }
                }
            }
    }

    private fun checkContent(
        fileName: String,
        content: List<String>,
    ) {
        for (item in content) checkLibraryName(fileName, item)
        checkLibraryContent(fileName, content)
        checkAlphabeticalOrder(fileName, content)
    }

    private fun checkLibraryName(
        fileName: String,
        name: String,
    ) {
        val libName = name.substringBefore('=').trim()
        if (libName.any { it.isUpperCase() }) {
            showError(
                fileName,
                "Please, don't use camelCase in library name: $name. Use dashes instead",
            )
        }
    }

    private fun checkLibraryContent(
        fileName: String,
        array: List<String>,
    ) {
        val arSet = mutableSetOf<String>()
        array.forEach { item ->
            val content = item.substringAfter("{").substringBefore("}").trim()
            if (content.contains("version = ")) {
                showError(
                    fileName,
                    "Please, use version.ref for library with content: " +
                        "$content",
                )
            }
            if (content in arSet) {
                showError(
                    fileName,
                    "You have duplicated library with content: $content. " +
                        "Please, remove it.",
                )
            }
            arSet.add(content)
        }
    }

    private fun showError(
        fileName: String,
        message: String,
    ): Nothing = throw IllegalArgumentException("[$fileName] parse error. $message")

    companion object {
        private const val ANCHOR_VERSIONS = "[versions]"
        private const val ANCHOR_LIBRARIES = "[libraries]"
        private const val ANCHOR_BUNDLES = "[bundles]"
        private const val ANCHOR_PLUGINS = "[plugins]"
        private val anchors =
            listOf(ANCHOR_VERSIONS, ANCHOR_LIBRARIES, ANCHOR_BUNDLES, ANCHOR_PLUGINS)
        private const val DIR_PATH = "gradle"
        private const val FILE_EXTENSION = "toml"
    }
}
