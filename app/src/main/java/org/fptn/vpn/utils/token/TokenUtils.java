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

package org.fptn.vpn.utils.token;

import com.elvishew.xlog.XLog;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.fptn.vpn.database.entity.ServerEntity;
import org.fptn.vpn.utils.BrotliUtils;
import org.fptn.vpn.utils.CountryFlags;
import org.fptn.vpn.vpnclient.exception.ErrorCode;
import org.fptn.vpn.vpnclient.exception.PVNClientException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import lombok.experimental.UtilityClass;

@UtilityClass
public class TokenUtils {
    private static final String TAG = TokenUtils.class.getSimpleName();

    private static final ObjectMapper OBJECT_MAPPER;

    static {
        OBJECT_MAPPER = new ObjectMapper();
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public List<ServerEntity> parseToken(String token) throws PVNClientException {
        List<ServerEntity> serverDtoList = new ArrayList<>();

        // removes all whitespaces and non-visible characters (e.g., tab, \n) and prefixes fptn://  fptn: fptnb:
        final String clearedToken = token.replaceAll("\\s+", "")
                .replace("fptn://", "")
                .replace("fptn:", "")
                .replace("fptnb:", "");
        try {
            String decodedToken;
            if (token.contains("fptnb:")) {
                decodedToken = BrotliUtils.decodeBrotliString(clearedToken);
            } else {
                decodedToken = new String(Base64.getDecoder().decode(clearedToken));
            }

            Token fptnToken = OBJECT_MAPPER.readValue(decodedToken, Token.class);
            TokenValidationUtils.validate(fptnToken);

            String username = fptnToken.getUsername();
            String password = fptnToken.getPassword();
            // add normal servers
            List<ServerFromToken> servers = fptnToken.getServers();
            if (servers != null && !servers.isEmpty()) {
                for (ServerFromToken server : servers) {
                    ServerEntity serverEntity = createServerEntity(server, username, password, false);
                    serverDtoList.add(serverEntity);
                    XLog.tag(TAG).d("Server parsed from token [server=%s]", serverEntity.getServerInfo());
                }
            }
            // add censured servers
            List<ServerFromToken> censoredServers = fptnToken.getCensoredServers();
            if (censoredServers != null && !censoredServers.isEmpty()) {
                for (ServerFromToken server : censoredServers) {
                    ServerEntity serverEntity = createServerEntity(server, username, password, true);
                    serverDtoList.add(serverEntity);
                    XLog.tag(TAG).d("Censored server parsed from token [server=%s]", serverEntity.getServerInfo());
                }
            }
        } catch (IOException e) {
            XLog.tag(TAG).e("Failed to decode/parse FPTN token: %s", e.getMessage());
            throw new PVNClientException(ErrorCode.ACCESS_TOKEN_FORMAT_ERROR);
        }

        if (serverDtoList.isEmpty()) {
            XLog.tag(TAG).e("Token parsed but no servers found — token may be invalid or expired");
//            throw new PVNClientException(ErrorCode.SERVER_LIST_NULL_OR_EMPTY);
        }

        return serverDtoList;
    }

    private ServerEntity createServerEntity(ServerFromToken server, String username, String password, boolean censured) {
        String countryCode = Optional.ofNullable(server.getCountryCode())
                .orElse(CountryFlags.getCountryCodeFromHostName(server.getName()));

        return ServerEntity.builder()
                .id(0)
                .name(server.getName())
                .host(server.getHost())
                .port(server.getPort())
                .countryCode(countryCode)
                .username(username)
                .password(password)
                .md5ServerFingerprint(server.getMd5Fingerprint())
                .selected(false)
                .censured(censured)
                .build();
    }

}
