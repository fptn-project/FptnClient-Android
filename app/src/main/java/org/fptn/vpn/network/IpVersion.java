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

package org.fptn.vpn.network;

import lombok.Getter;

@Getter
public enum IpVersion {
    UNKNOWN(0, 0, 0, 0, 0),
    V4(4, 20, 12, 16, 4),
    V6(6, 40, 8, 24, 16);

    private final int number;
    private final int minHeaderLength;
    private final int sourceOffset;
    private final int destinationOffset;
    private final int addressLength;

    IpVersion(int number, int minHeaderLength, int sourceOffset, int destinationOffset,
            int addressLength) {
        this.number = number;
        this.minHeaderLength = minHeaderLength;
        this.sourceOffset = sourceOffset;
        this.destinationOffset = destinationOffset;
        this.addressLength = addressLength;
    }

    public static IpVersion of(int number) {
        return switch (number) {
            case 4 -> V4;
            case 6 -> V6;
            default -> UNKNOWN;
        };
    }
}
