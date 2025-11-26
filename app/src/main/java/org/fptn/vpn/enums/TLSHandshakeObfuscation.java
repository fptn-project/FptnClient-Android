package org.fptn.vpn.enums;

import androidx.annotation.NonNull;

import lombok.Getter;

public enum TLSHandshakeObfuscation {
    TLS_APP_DATA("TLS App Data", 0), // todo: enrich
    TLS_APP_DATA_BASE_64("TLS App Data + base64", 1);

    final String description;

    @Getter
    final int id;

    TLSHandshakeObfuscation(String description, int id) {
        this.description = description;
        this.id = id;
    }

    @NonNull
    @Override
    public String toString() {
        return description;
    }
}
