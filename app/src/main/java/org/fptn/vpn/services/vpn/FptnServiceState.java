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

package org.fptn.vpn.services.vpn;

import org.fptn.vpn.enums.ConnectionState;
import org.fptn.vpn.vpnclient.exception.PVNClientException;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class FptnServiceState {
    private final ConnectionState connectionState;
    private final PVNClientException exception;
    private final String serverInfo;

    public static final FptnServiceState INITIAL = FptnServiceState.builder()
            .connectionState(ConnectionState.DISCONNECTED)
            .exception(null)
            .build();

    public static final FptnServiceState FAKE_CONNECTING = FptnServiceState.builder()
            .connectionState(ConnectionState.CONNECTING)
            .exception(null)
            .build();
}
