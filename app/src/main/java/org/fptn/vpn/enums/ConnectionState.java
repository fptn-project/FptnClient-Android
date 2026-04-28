package org.fptn.vpn.enums;

import java.util.Set;

public enum ConnectionState {
    SEARCH_SNI,
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING;

    private final static Set<ConnectionState> ACTIVE_STATES = Set.of(
            CONNECTING,
            CONNECTED,
            RECONNECTING,
            SEARCH_SNI
    );

    public boolean isActiveState() {
        return ACTIVE_STATES.contains(this);
    }

}
