package org.fptn.vpn.utils

import java.util.Locale
import java.util.MissingResourceException

@Suppress("MagicNumber")
object CountryFlags {
    private val emojiMap =
        mapOf(
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
        // todo: "Russia (Saint Petersburg)" special case!!! Stas, why it's so special?
        if (countryName?.contains("russia", true) == true) return "RU"

        val availableLocales = Locale.getAvailableLocales()
        for (availableLocale in availableLocales) {
            val displayCountry = availableLocale.getDisplayCountry(Locale.US)
            if (displayCountry.equals(countryName, ignoreCase = true) ||
                isThreeLettersCodeEquals(
                    availableLocale,
                    countryName,
                )
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
        try {
            val threeLettersCode = availableLocale.isO3Country
            return threeLettersCode.equals(countryName, ignoreCase = true)
        } catch (ignored: MissingResourceException) {
        }
        return false
    }

    @JvmStatic
    fun getCountryCodeFromHostName(name: String?): String? {
        if (name != null) {
            val splitResult = name.split("-".toRegex(), limit = 2).toTypedArray()
            if (splitResult.isNotEmpty()) {
                val countryName = splitResult[0]
                return getCountryCode(countryName)
            }
        }
        return null
    }
}
