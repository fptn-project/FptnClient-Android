package org.fptn.vpn.enums;

import java.util.Set;

public enum ConnectionState {
    SEARCH_SNI,
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    WAITING_FOR_NETWORK;

    private final static Set<ConnectionState> ACTIVE_STATES = Set.of(
            CONNECTING,
            CONNECTED,
            RECONNECTING,
            SEARCH_SNI,
            WAITING_FOR_NETWORK
    );

    public boolean isActiveState() {
        return ACTIVE_STATES.contains(this);
    }

}
