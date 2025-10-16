package org.fptn.vpn.enums;

import androidx.annotation.NonNull;

import lombok.Getter;

public enum ObfuscationMethodEnum {
    NONE("None", 0),
    TLS_APP_DATA("TLS App Data", 1),
    TLS_APP_DATA_BASE_64("TLS App Data + base64", 2);

    final String description;

    @Getter
    final int id;

    ObfuscationMethodEnum(String description, int id) {
        this.description = description;
        this.id = id;
    }

    @NonNull
    @Override
    public String toString() {
        return description;
    }
}
