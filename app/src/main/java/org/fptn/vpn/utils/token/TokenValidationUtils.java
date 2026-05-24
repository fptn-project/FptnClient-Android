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
            XLog.tag(TAG).e("Token cannot be null");
            throw new PVNClientException(ErrorCode.ACCESS_TOKEN_FORMAT_ERROR);
        }

        // Validate username
        if (token.getUsername() == null || token.getUsername().isBlank()) {
            XLog.tag(TAG).e("Username cannot be blank");
            throw new PVNClientException(ErrorCode.ACCESS_TOKEN_FORMAT_ERROR);
        }

        // Validate password
        if (token.getPassword() == null || token.getPassword().isBlank()) {
            XLog.tag(TAG).e("Password cannot be blank");
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
            XLog.tag(TAG).e("Server cannot be null");
            throw new PVNClientException(ErrorCode.ACCESS_TOKEN_FORMAT_ERROR);
        }

        // Validate name
        if (server.getName() == null || server.getName().isBlank()) {
            XLog.tag(TAG).e("Server name cannot be blank");
            throw new PVNClientException(ErrorCode.ACCESS_TOKEN_FORMAT_ERROR);
        }
        if (server.getName().length() > 100) {
            XLog.tag(TAG).e("Server name must be less than 100 characters");
            throw new PVNClientException(ErrorCode.ACCESS_TOKEN_FORMAT_ERROR);
        }

        // Validate host
        if (server.getHost() == null || server.getHost().isBlank()) {
            XLog.tag(TAG).e("Host cannot be blank");
            throw new PVNClientException(ErrorCode.ACCESS_TOKEN_FORMAT_ERROR);
        }

        // Validate md5Fingerprint
//        if (server.getMd5Fingerprint() == null || server.getMd5Fingerprint().isBlank()) {
//            XLog.tag(TAG).e("MD5 fingerprint cannot be blank");
//            throw new PVNClientException(ErrorCode.ACCESS_TOKEN_FORMAT_ERROR);
//        }

        // Validate port
        if (server.getPort() == null) {
            XLog.tag(TAG).e("Port cannot be null");
            throw new PVNClientException(ErrorCode.ACCESS_TOKEN_FORMAT_ERROR);
        }
        if (server.getPort() < 1 || server.getPort() > 65535) {
            XLog.tag(TAG).e("Port must be between 1 and 65535");
            throw new PVNClientException(ErrorCode.ACCESS_TOKEN_FORMAT_ERROR);
        }
    }

}