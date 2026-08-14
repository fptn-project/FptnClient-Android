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

import java.util.Set;

public enum ConnectionState {
    SEARCH_SNI,
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    WAITING_FOR_NETWORK,
    BLOCKED;

    private final static Set<ConnectionState> ACTIVE_STATES = Set.of(
            CONNECTING,
            CONNECTED,
            RECONNECTING,
            SEARCH_SNI,
            WAITING_FOR_NETWORK,
            BLOCKED
    );

    public boolean isActiveState() {
        return ACTIVE_STATES.contains(this);
    }

}
