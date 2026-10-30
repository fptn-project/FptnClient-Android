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

public final class TokenUtils {
    private static final String TAG = TokenUtils.class.getSimpleName();

    private static final ObjectMapper OBJECT_MAPPER;

    static {
        OBJECT_MAPPER = new ObjectMapper();
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private TokenUtils() {
    }

    /**
     * Token parsing result: metadata + list of servers.
     */
    public static class ParsedToken {
        public final String serviceName;
        public final String username;
        public final String password;
        public final List<ServerEntity> servers;

        public ParsedToken(String serviceName, String username, String password, List<ServerEntity> servers) {
            this.serviceName = serviceName;
            this.username = username;
            this.password = password;
            this.servers = servers;
        }
    }

    /**
     * Parses the token and returns its metadata + list of servers.
     *
     * @return ParsedToken containing serviceName, username, password, and servers.
     *         If the token does not contain serviceName, it will be null (the calling code can use username).
     */
    public static ParsedToken parseToken(String token) throws PVNClientException {
        List<ServerEntity> serverDtoList = new ArrayList<>();

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
            String serviceName = fptnToken.getServiceName();  // may be null

            List<ServerFromToken> servers = fptnToken.getServers();
            if (servers != null && !servers.isEmpty()) {
                for (int i = 0; i < servers.size(); i++) {
                    ServerEntity serverEntity = createServerEntity(servers.get(i), username, password, false, i);
                    serverDtoList.add(serverEntity);
                    XLog.tag(TAG).d("Server parsed from token [server=%s]", serverEntity.getServerInfo());
                }
            }
            List<ServerFromToken> censoredServers = fptnToken.getCensoredServers();
            if (censoredServers != null && !censoredServers.isEmpty()) {
                for (int i = 0; i < censoredServers.size(); i++) {
                    ServerEntity serverEntity = createServerEntity(censoredServers.get(i), username, password, true, i);
                    serverDtoList.add(serverEntity);
                    XLog.tag(TAG).d("Censored server parsed from token [server=%s]", serverEntity.getServerInfo());
                }
            }

            return new ParsedToken(serviceName, username, password, serverDtoList);
        } catch (Exception e) {
            XLog.tag(TAG).e("Failed to decode/parse FPTN token: %s", e.getMessage());
            throw new PVNClientException(ErrorCode.ACCESS_TOKEN_FORMAT_ERROR);
        }
    }

    private static ServerEntity createServerEntity(ServerFromToken server, String username, String password, boolean censured, int sortIndex) {
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
                .sortIndex(sortIndex)
                .build();
    }
}