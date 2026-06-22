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

import org.fptn.vpn.vpnclient.exception.ErrorCode;
import org.fptn.vpn.vpnclient.exception.PVNClientException;

import lombok.experimental.UtilityClass;

@UtilityClass
public class TokenValidationUtils {
    private static final String TAG = TokenValidationUtils.class.getSimpleName();

    public void validate(Token token) throws PVNClientException {
        if (token == null) {
            XLog.tag(TAG).e("Token validation failed: token is null");
            throw new PVNClientException(ErrorCode.ACCESS_TOKEN_FORMAT_ERROR);
        }

        // Validate username
        if (token.getUsername() == null || token.getUsername().isBlank()) {
            XLog.tag(TAG).e("Token validation failed: username is missing");
            throw new PVNClientException(ErrorCode.ACCESS_TOKEN_FORMAT_ERROR);
        }

        // Validate password
        if (token.getPassword() == null || token.getPassword().isBlank()) {
            XLog.tag(TAG).e("Token validation failed: password is missing");
            throw new PVNClientException(ErrorCode.ACCESS_TOKEN_FORMAT_ERROR);
        }

        // Validate servers
        if (token.getServers() != null) {
            for (ServerFromToken server : token.getServers()) {
                validate(server);
            }
        }

        // Validate censoredServers
        if (token.getCensoredServers() != null) {
            for (ServerFromToken server : token.getCensoredServers()) {
                validate(server);
            }
        }
    }

    public void validate(ServerFromToken server) throws PVNClientException {
        if (server == null) {
            XLog.tag(TAG).e("Token validation failed: server entry is null");
            throw new PVNClientException(ErrorCode.ACCESS_TOKEN_FORMAT_ERROR);
        }

        // Validate name
        if (server.getName() == null || server.getName().isBlank()) {
            XLog.tag(TAG).e("Token validation failed: server name is empty");
            throw new PVNClientException(ErrorCode.ACCESS_TOKEN_FORMAT_ERROR);
        }
        if (server.getName().length() > 100) {
            XLog.tag(TAG).e("Token validation failed: server name exceeds 100 chars [name=%s]", server.getName());
            throw new PVNClientException(ErrorCode.ACCESS_TOKEN_FORMAT_ERROR);
        }

        // Validate host
        if (server.getHost() == null || server.getHost().isBlank()) {
            XLog.tag(TAG).e("Token validation failed: server host is empty [server=%s]", server.getName());
            throw new PVNClientException(ErrorCode.ACCESS_TOKEN_FORMAT_ERROR);
        }

        // Validate md5Fingerprint
//        if (server.getMd5Fingerprint() == null || server.getMd5Fingerprint().isBlank()) {
//            XLog.tag(TAG).e("MD5 fingerprint cannot be blank");
//            throw new PVNClientException(ErrorCode.ACCESS_TOKEN_FORMAT_ERROR);
//        }

        // Validate port
        if (server.getPort() == null) {
            XLog.tag(TAG).e("Token validation failed: server port is null [server=%s]", server.getName());
            throw new PVNClientException(ErrorCode.ACCESS_TOKEN_FORMAT_ERROR);
        }
        if (server.getPort() < 1 || server.getPort() > 65535) {
            XLog.tag(TAG).e("Token validation failed: invalid port [server=%s, port=%d]", server.getName(), server.getPort());
            throw new PVNClientException(ErrorCode.ACCESS_TOKEN_FORMAT_ERROR);
        }
    }

}
