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
public enum IpProtocol {
    ICMP(1),
    TCP(6),
    UDP(17),
    ICMPV6(58),
    OTHER(-1);

    private final int number;

    IpProtocol(int number) {
        this.number = number;
    }

    public static IpProtocol of(int number) {
        return switch (number) {
            case 1 -> ICMP;
            case 6 -> TCP;
            case 17 -> UDP;
            case 58 -> ICMPV6;
            default -> OTHER;
        };
    }
}
