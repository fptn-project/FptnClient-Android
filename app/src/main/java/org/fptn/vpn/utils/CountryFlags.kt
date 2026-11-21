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

package org.fptn.vpn.utils

import java.util.Locale
import java.util.MissingResourceException

@Suppress("MagicNumber")
object CountryFlags {
    private val emojiMap = mapOf(
        'A' to getEmojiByUnicode(0x1F1E6),
        'B' to getEmojiByUnicode(0x1F1E7),
        'C' to getEmojiByUnicode(0x1F1E8),
        'D' to getEmojiByUnicode(0x1F1E9),
        'E' to getEmojiByUnicode(0x1F1EA),
        'F' to getEmojiByUnicode(0x1F1EB),
        'G' to getEmojiByUnicode(0x1F1EC),
        'H' to getEmojiByUnicode(0x1F1ED),
        'I' to getEmojiByUnicode(0x1F1EE),
        'J' to getEmojiByUnicode(0x1F1EF),
        'K' to getEmojiByUnicode(0x1F1F0),
        'L' to getEmojiByUnicode(0x1F1F1),
        'M' to getEmojiByUnicode(0x1F1F2),
        'N' to getEmojiByUnicode(0x1F1F3),
        'O' to getEmojiByUnicode(0x1F1F4),
        'P' to getEmojiByUnicode(0x1F1F5),
        'Q' to getEmojiByUnicode(0x1F1F6),
        'R' to getEmojiByUnicode(0x1F1F7),
        'S' to getEmojiByUnicode(0x1F1F8),
        'T' to getEmojiByUnicode(0x1F1F9),
        'U' to getEmojiByUnicode(0x1F1FA),
        'V' to getEmojiByUnicode(0x1F1FB),
        'W' to getEmojiByUnicode(0x1F1FC),
        'X' to getEmojiByUnicode(0x1F1FD),
        'Y' to getEmojiByUnicode(0x1F1FE),
        'Z' to getEmojiByUnicode(0x1F1FF),
    )

    /**
     * Словарь альтернативных названий стран для надёжного разрешения в ISO-коды.
     *
     * Зачем: на современных Android (API 33+) Locale.getDisplayCountry() возвращает
     * новые официальные названия (Türkiye вместо Turkey, Türkiye вместо Turkey, и т.п.),
     * а в token'е FPTN сервера обычно называются по-старому. Также покрывает популярные
     * альтернативы (USA, UK, UAE) и кириллицу для русскоязычных серверов.
     */
    private val COUNTRY_ALIASES: Map<String, String> = mapOf(
        // Официальные переименования (современный Android возвращает новые названия)
        "Turkey" to "TR", "Türkiye" to "TR",
        "Czech Republic" to "CZ", "Czechia" to "CZ",
        "Cape Verde" to "CV", "Cabo Verde" to "CV",
        "Swaziland" to "SZ", "Eswatini" to "SZ",
        "Macedonia" to "MK", "North Macedonia" to "MK",
        "Burma" to "MM", "Myanmar" to "MM",
        // Популярные аббревиатуры
        "USA" to "US", "UAE" to "AE", "UK" to "GB",
        "Russia" to "RU", "Russian Federation" to "RU",
        // Русские названия (кириллица) — частые для русскоязычных серверов
        "Россия" to "RU", "РФ" to "RU",
        "Турция" to "TR", "Туркия" to "TR",
        "США" to "US", "Америка" to "US",
        "Германия" to "DE", "Франция" to "FR",
        "Нидерланды" to "NL", "Голландия" to "NL",
        "Англия" to "GB", "Британия" to "GB",
        "Япония" to "JP", "Китай" to "CN",
        "Корея" to "KR", "Индия" to "IN",
        "Украина" to "UA", "Беларусь" to "BY",
        "Польша" to "PL", "Италия" to "IT",
        "Испания" to "ES", "Финляндия" to "FI",
        "Швеция" to "SE", "Норвегия" to "NO",
    )

    private fun getCodeByCharacter(character: Char): String {
        return emojiMap[character.uppercaseChar()] ?: ""
    }

    private fun getEmojiByUnicode(unicode: Int): String {
        return String(Character.toChars(unicode))
    }

    @JvmStatic
    fun getCountryFlagByCountryCode(countryCode: String?): String? {
        return countryCode?.let { code ->
            code
                .uppercase(Locale.getDefault())
                .takeIf { it.length == 2 }
                ?.let { string ->
                    getCodeByCharacter(string[0]) + getCodeByCharacter(string[1])
                }
        }
    }

    @JvmStatic
    fun getCountryCode(countryName: String?): String? {
        if (countryName.isNullOrBlank()) return null

        val normalized = countryName.trim().replace("\\s+".toRegex(), " ")
        if (normalized.isEmpty()) return null

        // Спец-кейс Russia: "Russia (Saint Petersburg)" → "RU"
        if (normalized.contains("russia", true)) return "RU"

        // 0) Проверка хардкод-словаря алиасов (быстро, надёжно, не зависит от системы).
        // Покрывает переименования (Türkiye→TR, Czechia→CZ) и альтернативные названия.
        for ((alias, code) in COUNTRY_ALIASES) {
            if (alias.equals(normalized, ignoreCase = true)) return code
        }

        val upper = normalized.uppercase(Locale.US)

        // 1) Если уже 2-буквенный ISO-код (TR, US, GB, RU, ...) — валидируем через Locale.
        if (upper.length == 2 && upper[0].isLetter() && upper[1].isLetter()) {
            val locale = Locale("", upper)
            if (locale.displayCountry.isNotEmpty()) {
                return upper
            }
            // Даже если Locale не нашёл страну, вернём код — возможно, он валидный,
            // просто эмулятор не знает этой локали.
            return upper
        }

        // 2) Если 3-буквенный ISO-код (TUR, USA, RUS, ...) — ищем через isO3Country.
        if (upper.length == 3 && upper.all { it.isLetter() }) {
            for (availableLocale in Locale.getAvailableLocales()) {
                try {
                    if (availableLocale.isO3Country.equals(upper, ignoreCase = true)) {
                        return availableLocale.country
                    }
                } catch (ignored: MissingResourceException) {
                }
            }
        }

        // 3) Линейный поиск по всем локалям: полное название (displayCountry) или ISO-3.
        for (availableLocale in Locale.getAvailableLocales()) {
            val displayCountry = availableLocale.getDisplayCountry(Locale.US)
            if (displayCountry.isNotEmpty() && displayCountry.equals(normalized, ignoreCase = true)) {
                return availableLocale.country
            }
            if (isThreeLettersCodeEquals(availableLocale, normalized)) {
                return availableLocale.country
            }
        }

        // 4) Поиск по displayCountry на локальном языке (Türkiye вместо Turkey).
        for (availableLocale in Locale.getAvailableLocales()) {
            val displayCountryNative = availableLocale.getDisplayCountry(availableLocale)
            if (displayCountryNative.isNotEmpty() &&
                displayCountryNative.equals(normalized, ignoreCase = true)
            ) {
                return availableLocale.country
            }
        }

        // 5) Last resort: startsWith для частичных совпадений.
        // ⚠️ ВАЖНО: проверяем, что displayCountry не пустой, иначе "Turkey".startsWith("")
        // вернёт true и мы получим пустой код (баг, который был раньше).
        for (availableLocale in Locale.getAvailableLocales()) {
            val displayCountry = availableLocale.getDisplayCountry(Locale.US)
            if (displayCountry.isEmpty()) continue
            if (displayCountry.startsWith(normalized, ignoreCase = true) ||
                normalized.startsWith(displayCountry, ignoreCase = true)
            ) {
                return availableLocale.country
            }
        }

        return null
    }

    private fun isThreeLettersCodeEquals(
        availableLocale: Locale,
        countryName: String?,
    ): Boolean {
        if (countryName == null) return false
        try {
            val threeLettersCode = availableLocale.isO3Country
            return threeLettersCode.equals(countryName, ignoreCase = true)
        } catch (ignored: MissingResourceException) {
        }
        return false
    }

    @JvmStatic
    fun getCountryCodeFromHostName(name: String?): String? {
        if (name.isNullOrBlank()) return null

        val splitResult = name.split("-".toRegex(), limit = 2).toTypedArray()
        if (splitResult.isEmpty()) return null

        val countryName = splitResult[0].trim()
        if (countryName.isEmpty()) return null

        return getCountryCode(countryName)
    }
}