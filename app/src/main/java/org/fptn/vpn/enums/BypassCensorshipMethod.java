package org.fptn.vpn.enums;

public enum BypassCensorshipMethod {
    SNI_SPOOFING,   // deprecated
    TLS_OBFUSCATION,
    SNI_REALITY,  // deprecated
    /* Chrome */
    SNI_REALITY_CHROME_147,
    SNI_REALITY_CHROME_146,
    SNI_REALITY_CHROME_145,
    /* Firefox */
    SNI_REALITY_FIREFOX_149,
    /* Yandex Browser */
    SNI_REALITY_YANDEX_26,
    SNI_REALITY_YANDEX_25,
    SNI_REALITY_YANDEX_24,
    /* Safari */
    SNI_REALITY_SAFARI_26
}
