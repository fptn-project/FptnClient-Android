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
public enum DnsRecordType {
    A(1, 4),
    AAAA(28, 16),
    OTHER(-1, 0);

    private final int number;
    private final int addressLength;

    DnsRecordType(int number, int addressLength) {
        this.number = number;
        this.addressLength = addressLength;
    }

    public static DnsRecordType of(int number) {
        return switch (number) {
            case 1 -> A;
            case 28 -> AAAA;
            default -> OTHER;
        };
    }

    public boolean isAddress(int rdLength) {
        return this != OTHER && rdLength == addressLength;
    }
}
