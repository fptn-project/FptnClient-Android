package org.fptn.vpn.utils.token;

import android.util.Log;

import org.fptn.vpn.vpnclient.exception.ErrorCode;
import org.fptn.vpn.vpnclient.exception.PVNClientException;

import lombok.experimental.UtilityClass;

@UtilityClass
public class TokenValidationUtils {
    private static final String TAG = TokenValidationUtils.class.getSimpleName();

    public void validate(Token token) throws PVNClientException {
        if (token == null) {
            Log.e(TAG, "Token cannot be null");
            throw new PVNClientException(ErrorCode.ACCESS_TOKEN_FORMAT_ERROR);
        }

        // Validate username
        if (token.getUsername() == null || token.getUsername().isBlank()) {
            Log.e(TAG, "Username cannot be blank");
            throw new PVNClientException(ErrorCode.ACCESS_TOKEN_FORMAT_ERROR);
        }

        // Validate password
        if (token.getPassword() == null || token.getPassword().isBlank()) {
            Log.e(TAG, "Password cannot be blank");
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
            Log.e(TAG, "Server cannot be null");
            throw new PVNClientException(ErrorCode.ACCESS_TOKEN_FORMAT_ERROR);
        }

        // Validate name
        if (server.getName() == null || server.getName().isBlank()) {
            Log.e(TAG, "Server name cannot be blank");
            throw new PVNClientException(ErrorCode.ACCESS_TOKEN_FORMAT_ERROR);
        }
        if (server.getName().length() > 100) {
            Log.e(TAG, "Server name must be less than 100 characters");
            throw new PVNClientException(ErrorCode.ACCESS_TOKEN_FORMAT_ERROR);
        }

        // Validate host
        if (server.getHost() == null || server.getHost().isBlank()) {
            Log.e(TAG, "Host cannot be blank");
            throw new PVNClientException(ErrorCode.ACCESS_TOKEN_FORMAT_ERROR);
        }

        // Validate md5Fingerprint
//        if (server.getMd5Fingerprint() == null || server.getMd5Fingerprint().isBlank()) {
//            Log.e(TAG, "MD5 fingerprint cannot be blank");
//            throw new PVNClientException(ErrorCode.ACCESS_TOKEN_FORMAT_ERROR);
//        }

        // Validate port
        if (server.getPort() == null) {
            Log.e(TAG, "Port cannot be null");
            throw new PVNClientException(ErrorCode.ACCESS_TOKEN_FORMAT_ERROR);
        }
        if (server.getPort() < 1 || server.getPort() > 65535) {
            Log.e(TAG, "Port must be between 1 and 65535");
            throw new PVNClientException(ErrorCode.ACCESS_TOKEN_FORMAT_ERROR);
        }
    }

}