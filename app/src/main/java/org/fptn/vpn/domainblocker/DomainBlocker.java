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

package org.fptn.vpn.domainblocker;

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

public class DomainBlocker {
    private static final String TAG = DomainBlocker.class.getSimpleName();

    // R.raw.blocklist → src/main/res/raw/blocklist.gz
    private static final int DNS_PORT = 53;
    private static final int PROTO_UDP = 17;
    private static final int MIN_IPV4 = 20;
    private static final int MIN_IPV6 = 40;
    private static final int UDP_HDR = 8;
    private static final int DNS_HDR = 12;

    private static final int[] DETECTOR_DOMAIN_ARRAYS = {R.array.blocked_domains_ru};

    private final Set<String> blockedDomains;

    public DomainBlocker(Context context, boolean adBlockEnabled, String domainBlacklist,
            boolean blockDetectorDomains) {
        blockedDomains = new HashSet<>();
        if (adBlockEnabled) {
            blockedDomains.addAll(loadBlocklist(context));
        }
        blockedDomains.addAll(parseDomainBlacklist(domainBlacklist));
        if (blockDetectorDomains) {
            for (int arrayId : DETECTOR_DOMAIN_ARRAYS) {
                for (String domain : context.getResources().getStringArray(arrayId)) {
                    String d = domain.trim().toLowerCase();
                    if (d.contains(".")) {
                        blockedDomains.add(d);
                    }
                }
            }
        }
        XLog.tag(TAG).i("Blocklist DNS loaded [domains=%d]", blockedDomains.size());
    }

    // Parses a user-entered domain list: newline/comma separated, optional
    // "domain:" prefix (fptn desktop config format), invalid entries ignored.
    private static Set<String> parseDomainBlacklist(String text) {
        Set<String> domains = new HashSet<>();
        if (text == null) {
            return domains;
        }
        for (String entry : text.split("[\\n,]")) {
            String domain = entry.trim().toLowerCase();
            if (domain.startsWith("domain:")) {
                domain = domain.substring("domain:".length()).trim();
            }
            if (domain.contains(".")) {
                domains.add(domain);
            }
        }
        return domains;
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
        return buildNullRouteResponse(packet, length, ver, ipHdrLen, udpOff, dnsOff);
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

    // Returns 127.0.0.1 (A) or ::1 (AAAA) so SDK gets a "successful" DNS response,
    // attempts TCP connect to loopback, gets connection refused, and does NOT fall back to DoH.
    private byte[] buildNullRouteResponse(byte[] packet, int length, int ver,
                                           int ipHdrLen, int udpOff, int dnsOff) {
        // Find question section end to know where to append the answer
        int[] cur = {dnsOff + DNS_HDR};
        parseDnsName(packet, length, dnsOff, cur);
        if (cur[0] + 4 > length) {
            return buildNxdomainResponse(packet, length, ver, ipHdrLen, udpOff, dnsOff);
        }
        int qtype = ((packet[cur[0]] & 0xFF) << 8) | (packet[cur[0] + 1] & 0xFF);
        int questionEnd = cur[0] + 4; // past QTYPE + QCLASS

        // Build answer rdata based on query type
        final int QTYPE_A    = 0x0001;
        final int QTYPE_AAAA = 0x001C;
        byte[] rdata;
        int answerType;
        if (qtype == QTYPE_AAAA) {
            answerType = QTYPE_AAAA;
            rdata = new byte[]{0,0, 0,0, 0,0, 0,0, 0,0, 0,0, 0,0, 0,1}; // ::1
        } else if (qtype == QTYPE_A) {
            answerType = QTYPE_A;
            rdata = new byte[]{127, 0, 0, 1}; // 127.0.0.1
        } else {
            return buildNxdomainResponse(packet, length, ver, ipHdrLen, udpOff, dnsOff);
        }

        // answer = name_ptr(2) + type(2) + class(2) + ttl(4) + rdlen(2) + rdata
        int answerLen = 2 + 2 + 2 + 4 + 2 + rdata.length;
        int newLen = questionEnd + answerLen;

        byte[] resp = new byte[newLen];
        System.arraycopy(packet, 0, resp, 0, questionEnd);

        // Append answer record
        int off = questionEnd;
        resp[off++] = (byte) 0xC0; resp[off++] = (byte) DNS_HDR; // ptr to QNAME at dnsOff+12
        resp[off++] = (byte) (answerType >> 8); resp[off++] = (byte) (answerType & 0xFF);
        resp[off++] = 0x00; resp[off++] = 0x01; // class IN
        resp[off++] = 0; resp[off++] = 0; resp[off++] = 2; resp[off++] = 0x58; // TTL 600s
        resp[off++] = (byte) (rdata.length >> 8); resp[off++] = (byte) (rdata.length & 0xFF);
        System.arraycopy(rdata, 0, resp, off, rdata.length);

        // DNS flags: QR=1, RA=1, RCODE=0 (no error), ANCOUNT=1
        resp[dnsOff + 2] = (byte) ((resp[dnsOff + 2] & 0x01) | 0x80);
        resp[dnsOff + 3] = (byte) 0x80;
        resp[dnsOff + 6] = 0; resp[dnsOff + 7] = 1; // ANCOUNT = 1
        resp[dnsOff + 8] = 0; resp[dnsOff + 9] = 0;   // NSCOUNT = 0
        resp[dnsOff + 10] = 0; resp[dnsOff + 11] = 0; // ARCOUNT = 0 (drop EDNS0 OPT)

        // Swap IP src/dst
        if (ver == 4) {
            for (int i = 0; i < 4; i++) {
                byte tmp = resp[12 + i]; resp[12 + i] = resp[16 + i]; resp[16 + i] = tmp;
            }
            resp[8] = 64; // TTL
            resp[2] = (byte) (newLen >> 8); resp[3] = (byte) (newLen & 0xFF); // IP total length
        } else {
            for (int i = 0; i < 16; i++) {
                byte tmp = resp[8 + i]; resp[8 + i] = resp[24 + i]; resp[24 + i] = tmp;
            }
            resp[7] = 64; // hop limit
            int payloadLen = newLen - ipHdrLen;
            resp[4] = (byte) (payloadLen >> 8); resp[5] = (byte) (payloadLen & 0xFF);
        }

        // Swap UDP ports, update UDP length
        byte p0 = resp[udpOff], p1 = resp[udpOff + 1];
        resp[udpOff]     = resp[udpOff + 2];
        resp[udpOff + 1] = resp[udpOff + 3];
        resp[udpOff + 2] = p0;
        resp[udpOff + 3] = p1;
        int udpLen = newLen - udpOff;
        resp[udpOff + 4] = (byte) (udpLen >> 8);
        resp[udpOff + 5] = (byte) (udpLen & 0xFF);

        // Checksums
        if (ver == 4) {
            resp[10] = 0; resp[11] = 0;
            int ck = calcIpv4Checksum(resp, ipHdrLen);
            resp[10] = (byte) (ck >> 8); resp[11] = (byte) (ck & 0xFF);
            resp[udpOff + 6] = 0; resp[udpOff + 7] = 0; // UDP checksum optional in IPv4
        } else {
            resp[udpOff + 6] = 0; resp[udpOff + 7] = 0;
            int ck = calcUdpIpv6Checksum(resp, udpOff, newLen);
            resp[udpOff + 6] = (byte) (ck >> 8); resp[udpOff + 7] = (byte) (ck & 0xFF);
        }

        return resp;
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
