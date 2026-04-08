package org.fptn.vpn.enums;

public enum BypassCensorshipMethod {
    SNI_SPOOFING,
    TLS_OBFUSCATION,
    SNI_REALITY,  // deprecated
    SNI_REALITY_CHROME_146,
    SNI_REALITY_FIREFOX_149,
    SNI_REALITY_YANDEX_26,
    SNI_REALITY_YANDEX_25
}
