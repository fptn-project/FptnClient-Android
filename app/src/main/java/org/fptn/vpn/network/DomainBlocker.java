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

package org.fptn.vpn.network;

import android.content.Context;
import android.content.res.Resources;

import com.elvishew.xlog.XLog;
import org.fptn.vpn.R;
import org.fptn.vpn.utils.SharedPrefUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.net.IDN;
public class DomainBlocker {
    private static final String TAG = DomainBlocker.class.getSimpleName();

    // R.raw.blocklist → src/main/res/raw/blocklist.gz
    private static final int NULL_ROUTE_TTL_SECONDS = 600;
    private static final byte[] LOOPBACK_IPV4 = {127, 0, 0, 1};
    private static final byte[] LOOPBACK_IPV6 = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1};

    private static final int[] SPLIT_TUNNEL_DOMAIN_ARRAYS = {R.array.split_tunnel_domains};

    private final Set<String> blockedDomains;
    private final Set<String> allowedDomains;

    public DomainBlocker(Context context, boolean adBlockEnabled, String domainBlacklist,
            boolean blockDetectorDomains) {
        blockedDomains = new HashSet<>();
        allowedDomains = new HashSet<>();
        for (String domain : context.getResources().getStringArray(R.array.allowed_domains)) {
            String d = domain.trim().toLowerCase();
            if (d.contains(".")) {
                allowedDomains.add(d);
            }
        }
        if (adBlockEnabled) {
            blockedDomains.addAll(loadBlocklist(context));
        }
        blockedDomains.addAll(parseDomainBlacklist(domainBlacklist));
        if (blockDetectorDomains) {
            for (int arrayId : SPLIT_TUNNEL_DOMAIN_ARRAYS) {
                for (String domain : context.getResources().getStringArray(arrayId)) {
                    String d = domain.trim().toLowerCase();
                    if (d.contains(".")) {
                        blockedDomains.add(d);
                    }
                }
            }
        }
        XLog.tag(TAG).i("Blocklist DNS loaded [domains=%d, allowed=%d]",
                blockedDomains.size(), allowedDomains.size());
    }

    // Parses a user-entered domain list: newline/comma separated, optional
    // "domain:" prefix (fptn desktop config format), invalid entries ignored.
    // Supports:
    //   - full domains: "ixbt.com", "суточно.ру"
    //   - whole zones: "ru", "by", "kz", "рф" (or ".ru" with leading dot)
    //   - Cyrillic domains/zones are converted to Punycode (IDN)
    private static Set<String> parseDomainBlacklist(String text) {
    Set<String> domains = new HashSet<>();
    if (text == null) {
        return domains;
    }
    for (String entry : text.split("[\n,]")) {
        String domain = entry.trim().toLowerCase();
        if (domain.startsWith("domain:")) {
        domain = domain.substring("domain:".length()).trim();
        }
        
        // Allow leading dot for zones: ".ru" → "ru"
        if (domain.startsWith(".")) {
        domain = domain.substring(1);
        }
        
        if (domain.isEmpty()) {
        continue;
        }
        
        // Convert Cyrillic to Punycode: "суточно.ру" → "xn--80akhb1ah.xn--p1acf"
        //                                 "рф" → "xn--p1ai"
        try {
        domain = IDN.toASCII(domain, IDN.ALLOW_UNASSIGNED);
        } catch (IllegalArgumentException e) {
        // Invalid domain, skip
        continue;
        }
        
        // Accept both full domains (with dot) AND bare zones (without dot)
        domains.add(domain);
    }
    return domains;
    }

    // Returns 127.0.0.1 (A) or ::1 (AAAA) so SDK gets a "successful" DNS response,
    // attempts TCP connect to loopback, gets connection refused, and does NOT fall back to DoH.
    public byte[] processPacket(IPPacket packet) {
        if (!packet.isDnsQuery()) {
            return null;
        }
        final String domain = packet.getDnsDomain();
        if (domain == null || !isDomainBlocked(domain)) {
            return null;
        }

        XLog.tag(TAG).i("Blocked DNS query [domain=%s]", domain);
        DnsRecordType type = packet.getDnsQueryType();
        return switch (type) {
            case A -> packet.buildDnsAnswer(type, LOOPBACK_IPV4, NULL_ROUTE_TTL_SECONDS);
            case AAAA -> packet.buildDnsAnswer(type, LOOPBACK_IPV6, NULL_ROUTE_TTL_SECONDS);
            default -> packet.buildDnsNxDomain();
        };
    }

    public byte[] blockBySni(IPPacket packet) {
        String sni = packet.getSni();
        if (sni == null || !isDomainBlocked(sni)) {
            return null;
        }
        XLog.tag(TAG).i("Blocked TLS connection [sni=%s]", sni);
        return IPPacket.buildTcp(packet, packet.getAcknowledgment(),
                packet.getSequence() + packet.getPayloadLength(),
                IPPacket.FLAG_RST | IPPacket.FLAG_ACK, 0, null, 0, 0);
    }

    public static boolean matches(Set<String> rules, String domain) {
        if (domain == null) {
            return false;
        }
        String d = domain;
        while (d.contains(".")) {
            if (rules.contains(d)) {
                return true;
            }
            d = d.substring(d.indexOf('.') + 1);
        }
        return false;
    }

    // Checks domain, all parent domains, AND the bare TLD against blocklist.
    // e.g. "sub.ads.example.com" matches if "ads.example.com", "example.com", or "com" is blocked.
    // Allowlist is checked at each level first, so the most specific rule wins:
    // allowing "example.com" still blocks "ads.example.com" if that is on the blocklist.
    private boolean isDomainBlocked(String domain) {
    if (domain == null) return false;
    String d = domain;
    while (d.contains(".")) {
        if (allowedDomains.contains(d)) return false;
        if (blockedDomains.contains(d)) return true;
        int dot = d.indexOf('.');
        d = d.substring(dot + 1);
    }
    // Also check the bare TLD (last label) — enables blocking whole zones
    if (allowedDomains.contains(d)) return false;
    if (blockedDomains.contains(d)) return true;
    return false;
    }

    private Set<String> loadBlocklist(Context context) {
        try (InputStream is = context.getResources().openRawResource(R.raw.blocklist);
             GZIPInputStream gzis = new GZIPInputStream(is);
             BufferedReader reader = new BufferedReader(new InputStreamReader(gzis))) {

            Set<String> domains = new HashSet<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.charAt(0) == '#' || line.charAt(0) == '!') {
                    continue;
                }
                int commentIdx = line.indexOf('#');
                if (commentIdx >= 0) {
                    line = line.substring(0, commentIdx);
                }
                line = line.trim().toLowerCase();
                if (line.isEmpty()) {
                    continue;
                }

                // Hosts format: "0.0.0.0 domain.com" or "127.0.0.1 domain.com"
                // Plain format: "domain.com"
                String domain;
                int space = line.indexOf(' ');
                if (space < 0) {
                    space = line.indexOf('\t');
                }
                if (space >= 0) {
                    domain = line.substring(space + 1).trim();
                } else {
                    domain = line;
                }
                if (!domain.contains(".") || domain.equals("0.0.0.0")) {
                    continue;
                }
                domains.add(domain);
            }
            return domains;
        } catch (OutOfMemoryError e) {
            XLog.tag(TAG).w("Not enough memory to load blocklist — ad blocking inactive");
            return Collections.emptySet();
        } catch (IOException | Resources.NotFoundException e) {
            XLog.tag(TAG).w("Blocklist not found — ad blocking inactive: %s", e.getMessage());
            return Collections.emptySet();
        }
    }
}
