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

package org.fptn.vpn.enums;

public enum ConnectionStrategy {
    PERSISTENT_TUNNEL("persistent-tunnel"),
    ROLLING_TUNNEL("rolling-tunnel"),
    DUAL_TUNNEL("dual-rolling-tunnel"),
    TRIPLE_TUNNEL("triple-rolling-tunnel"),
    BROWSER_MIMICRY("browser-mimicry");

    private final String nativeName;

    ConnectionStrategy(String nativeName) {
        this.nativeName = nativeName;
    }

    public String getNativeName() {
        return nativeName;
    }
}
