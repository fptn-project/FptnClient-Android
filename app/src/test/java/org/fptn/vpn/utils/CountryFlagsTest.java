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

package org.fptn.vpn.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class CountryFlagsTest {

    @Test
    public void testGetCountryFlagByCountryCode() {
        // Test valid country codes
        assertEquals("🇺🇸", CountryFlags.getCountryFlagByCountryCode("US")); // United States
        assertEquals("🇬🇧", CountryFlags.getCountryFlagByCountryCode("GB")); // United Kingdom
        assertEquals("🇯🇵", CountryFlags.getCountryFlagByCountryCode("JP")); // Japan

        // Test invalid country codes
        assertNull(CountryFlags.getCountryFlagByCountryCode("U"));  // Too short
        assertNull(CountryFlags.getCountryFlagByCountryCode(null)); // Null input
    }

    @Test
    public void testGetCountryCode() {
        // Test valid country names
        assertEquals("US", CountryFlags.getCountryCode("United States"));
        assertEquals("GB", CountryFlags.getCountryCode("United Kingdom"));
        assertEquals("JP", CountryFlags.getCountryCode("Japan"));

        // Test invalid country names
        assertNull(CountryFlags.getCountryCode("Nonexistent Country"));
        assertNull(CountryFlags.getCountryCode(null)); // Null input

        // Test three letters code
        assertEquals("US", CountryFlags.getCountryCode("USA"));
        assertEquals("RU", CountryFlags.getCountryCode("RUS"));
    }

    @Test
    public void testGetCountryCodeFromHostName() {
        // Test valid country names
        assertEquals("US", CountryFlags.getCountryCodeFromHostName("USA-NewYork"));
        assertEquals("LV", CountryFlags.getCountryCodeFromHostName("Latvia-200"));
        assertEquals("NL", CountryFlags.getCountryCodeFromHostName("Netherlands-1"));
        assertEquals("EE", CountryFlags.getCountryCodeFromHostName("Estonia"));
    }

}
