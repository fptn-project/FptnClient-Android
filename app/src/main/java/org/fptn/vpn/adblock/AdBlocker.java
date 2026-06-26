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

package org.fptn.vpn.adblock;

import android.content.Context;
import android.content.res.Resources;

import com.elvishew.xlog.XLog;
import org.fptn.vpn.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.GZIPInputStream;

public class AdBlocker {
    private static final String TAG = AdBlocker.class.getSimpleName();

    // R.raw.blocklist → src/main/res/raw/blocklist.gz
    private static final int DNS_PORT = 53;
    private static final int PROTO_UDP = 17;
    private static final int MIN_IPV4 = 20;
    private static final int MIN_IPV6 = 40;
    private static final int UDP_HDR = 8;
    private static final int DNS_HDR = 12;

    private final Set<String> blockedDomains;

    public AdBlocker(Context context) {
        blockedDomains = loadBlocklist(context);
        XLog.tag(TAG).i("Blocklist DNS loaded [domains=%d]", blockedDomains.size());
    }

    // Port of DnsPtr() + DnsPayloadPtr() from ip_packet.h.
    // Returns offset of DNS payload in packet[], or -1 if not a DNS packet.
    // Handles IPv4 and IPv6; checks src OR dst port == 53.
    public static int dnsPayloadOffset(byte[] p, int len) {
        if (len < MIN_IPV4) {
            return -1;
        }
        int ver = (p[0] >> 4) & 0xF;
        int ipHdrLen;
        if (ver == 4) {
            if ((p[9] & 0xFF) != PROTO_UDP) {
                return -1;
            }
            ipHdrLen = (p[0] & 0xF) * 4;
        } else if (ver == 6) {
            if (len < MIN_IPV6) {
                return -1;
            }
            if ((p[6] & 0xFF) != PROTO_UDP) {
                return -1;
            }
            ipHdrLen = MIN_IPV6;
        } else {
            return -1;
        }
        int udpOff = ipHdrLen;
        if (len < udpOff + UDP_HDR) {
            return -1;
        }
        int srcPort = ((p[udpOff] & 0xFF) << 8) | (p[udpOff + 1] & 0xFF);
        int dstPort = ((p[udpOff + 2] & 0xFF) << 8) | (p[udpOff + 3] & 0xFF);
        if (srcPort != DNS_PORT && dstPort != DNS_PORT) {
            return -1;
        }
        int dnsOff = udpOff + UDP_HDR;
        return (len >= dnsOff + DNS_HDR) ? dnsOff : -1;
    }

    public static boolean isDnsPacket(byte[] packet, int length) {
        return dnsPayloadOffset(packet, length) >= 0;
    }

    public byte[] processPacket(byte[] packet, int length) {
        int dnsOff = dnsPayloadOffset(packet, length);
        if (dnsOff < 0) {
            return null;
        }

        int ver = (packet[0] >> 4) & 0xF;
        int ipHdrLen = (ver == 4) ? (packet[0] & 0xF) * 4 : MIN_IPV6;
        int udpOff = ipHdrLen;

        // Only intercept outgoing queries (dst port == 53, QR=0)
        int dstPort = ((packet[udpOff + 2] & 0xFF) << 8) | (packet[udpOff + 3] & 0xFF);
        if (dstPort != DNS_PORT) {
            return null;
        }
        if ((packet[dnsOff + 2] & 0x80) != 0) {
            return null; // QR=1, it's a response
        }

        String domain = getDnsDomain(packet, length, dnsOff);
        if (domain == null || !isDomainBlocked(domain)) {
            return null;
        }

        XLog.tag(TAG).i("Blocked DNS query [domain=%s]", domain);
        return buildNxdomainResponse(packet, length, ver, ipHdrLen, udpOff, dnsOff);
    }

    // Port of ParseDnsName() from ip_packet.h — supports RFC 1035 pointer compression.
    // base: offset of start of DNS payload; cur[0]: current parse position (in/out).
    private static String parseDnsName(byte[] p, int len, int base, int[] cur) {
        StringBuilder name = new StringBuilder();
        boolean jumped = false;
        int ptr = cur[0];
        for (int i = 0; i < 128 && ptr < len; i++) {
            int labelLen = p[ptr] & 0xFF;
            if (labelLen == 0) {
                if (!jumped) {
                    cur[0] = ptr + 1;
                }
                break;
            }
            if ((labelLen & 0xC0) == 0xC0) {
                if (ptr + 2 > len) {
                    break;
                }
                if (!jumped) {
                    cur[0] = ptr + 2;
                }
                jumped = true;
                ptr = base + ((labelLen & 0x3F) << 8 | (p[ptr + 1] & 0xFF));
                continue;
            }
            ptr++;
            if (ptr + labelLen > len) {
                break;
            }
            if (name.length() > 0) {
                name.append('.');
            }
            for (int j = 0; j < labelLen; j++) {
                name.append((char) (p[ptr++] & 0xFF));
            }
        }
        return name.toString();
    }

    // Checks domain and all parent domains against blocklist.
    // e.g. "sub.ads.example.com" matches if "ads.example.com" or "example.com" is blocked.
    private boolean isDomainBlocked(String domain) {
        if (domain == null) return false;
        String d = domain;
        while (d.contains(".")) {
            if (blockedDomains.contains(d)) return true;
            int dot = d.indexOf('.');
            d = d.substring(dot + 1);
        }
        return false;
    }

    // Port of GetDnsDomain() from ip_packet.h.
    private static String getDnsDomain(byte[] p, int len, int dnsOff) {
        int qdcount = ((p[dnsOff + 4] & 0xFF) << 8) | (p[dnsOff + 5] & 0xFF);
        if (qdcount == 0) {
            return null;
        }
        int[] cur = {dnsOff + DNS_HDR};
        String name = parseDnsName(p, len, dnsOff, cur);
        return name.isEmpty() ? null : name.toLowerCase();
    }

    private byte[] buildNxdomainResponse(byte[] packet, int length, int ver,
                                          int ipHdrLen, int udpOff, int dnsOff) {
        byte[] resp = Arrays.copyOf(packet, length);

        if (ver == 4) {
            for (int i = 0; i < 4; i++) {
                byte tmp = resp[12 + i]; resp[12 + i] = resp[16 + i]; resp[16 + i] = tmp;
            }
            resp[8] = 64; // TTL
        } else {
            for (int i = 0; i < 16; i++) {
                byte tmp = resp[8 + i]; resp[8 + i] = resp[24 + i]; resp[24 + i] = tmp;
            }
            resp[7] = 64; // hop limit
        }

        byte p0 = resp[udpOff], p1 = resp[udpOff + 1];
        resp[udpOff]     = resp[udpOff + 2];
        resp[udpOff + 1] = resp[udpOff + 3];
        resp[udpOff + 2] = p0;
        resp[udpOff + 3] = p1;

        // QR=1, keep RD; RA=1, RCODE=3 (NXDOMAIN)
        resp[dnsOff + 2] = (byte) ((resp[dnsOff + 2] & 0x01) | 0x80);
        resp[dnsOff + 3] = (byte) 0x83;

        if (ver == 4) {
            resp[10] = 0;
            resp[11] = 0;
            int ck = calcIpv4Checksum(resp, ipHdrLen);
            resp[10] = (byte) (ck >> 8);
            resp[11] = (byte) (ck & 0xFF);
            resp[udpOff + 6] = 0; // UDP checksum optional in IPv4
            resp[udpOff + 7] = 0;
        } else {
            resp[udpOff + 6] = 0;
            resp[udpOff + 7] = 0;
            int ck = calcUdpIpv6Checksum(resp, udpOff, length);
            resp[udpOff + 6] = (byte) (ck >> 8);
            resp[udpOff + 7] = (byte) (ck & 0xFF);
        }

        return resp;
    }

    private int calcIpv4Checksum(byte[] data, int ipHdrLen) {
        int sum = 0;
        for (int i = 0; i < ipHdrLen; i += 2) {
            sum += ((data[i] & 0xFF) << 8) | (data[i + 1] & 0xFF);
        }
        while ((sum >> 16) != 0) sum = (sum & 0xFFFF) + (sum >> 16);
        return ~sum & 0xFFFF;
    }

    // IPv6 UDP checksum — RFC 2460 pseudo-header: src(16)+dst(16)+udpLen(32)+zeros(3)+next(1).
    private int calcUdpIpv6Checksum(byte[] p, int udpOff, int len) {
        int udpLen = len - udpOff;
        int sum = 0;
        for (int i = 8; i < 40; i += 2) {
            sum += ((p[i] & 0xFF) << 8) | (p[i + 1] & 0xFF);
        }
        sum += udpLen & 0xFFFF;
        sum += PROTO_UDP;
        for (int i = udpOff; i < len; i += 2) {
            int hi = p[i] & 0xFF;
            int lo = (i + 1 < len) ? (p[i + 1] & 0xFF) : 0;
            sum += (hi << 8) | lo;
        }
        while ((sum >> 16) != 0) sum = (sum & 0xFFFF) + (sum >> 16);
        return ~sum & 0xFFFF;
    }

    private Set<String> loadBlocklist(Context context) {
        try (InputStream is = context.getResources().openRawResource(R.raw.blocklist);
             GZIPInputStream gzis = new GZIPInputStream(is);
             BufferedReader reader = new BufferedReader(new InputStreamReader(gzis))) {

            Set<String> domains = new HashSet<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.charAt(0) == '#' || line.charAt(0) == '!') continue;
                int commentIdx = line.indexOf('#');
                if (commentIdx >= 0) line = line.substring(0, commentIdx);
                line = line.trim().toLowerCase();
                if (line.isEmpty()) continue;

                // Hosts format: "0.0.0.0 domain.com" or "127.0.0.1 domain.com"
                // Plain format: "domain.com"
                String domain;
                int space = line.indexOf(' ');
                if (space < 0) space = line.indexOf('\t');
                if (space >= 0) {
                    domain = line.substring(space + 1).trim();
                } else {
                    domain = line;
                }

                if (!domain.contains(".") || domain.equals("0.0.0.0")) continue;
                domains.add(domain);
            }
            return domains;
        } catch (IOException | Resources.NotFoundException e) {
            XLog.tag(TAG).w("Blocklist not found — ad blocking inactive: %s", e.getMessage());
            return Collections.emptySet();
        }
    }
}
