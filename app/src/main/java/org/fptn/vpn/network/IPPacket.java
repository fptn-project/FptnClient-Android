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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class IPPacket {

    private static final int UDP_HEADER = 8;
    private static final int TCP_MIN_HEADER = 20;
    private static final int DNS_HEADER = 12;
    private static final int DNS_PORT = 53;
    private static final int MAX_LABELS = 128;

    private static final int DNS_CLASS_IN = 1;
    private static final int DNS_NAME_POINTER = 0xC0;
    private static final int DNS_FLAG_RESPONSE = 0x80;
    private static final int DNS_RCODE_NXDOMAIN = 0x83;
    private static final int DEFAULT_HOP_LIMIT = 64;
    private static final int DEFAULT_MSS = 1460;

    public static final int FLAG_FIN = 0x01;
    public static final int FLAG_SYN = 0x02;
    public static final int FLAG_RST = 0x04;
    public static final int FLAG_PSH = 0x08;
    public static final int FLAG_ACK = 0x10;

    private static final int TLS_RECORD_HANDSHAKE = 0x16;
    private static final int TLS_CLIENT_HELLO = 0x01;
    private static final int TLS_EXTENSION_SERVER_NAME = 0x0000;
    private static final int TLS_MIN_CLIENT_HELLO = 45;
    private static final int TLS_RANDOM_END = 43;

    private final byte[] data;
    private final int length;
    private IpVersion version;
    private int ipHeaderLength;
    private int protocol;

    public IPPacket(byte[] buffer, int size) {
        data = buffer;
        length = size;
        version = IpVersion.UNKNOWN;
        if (buffer == null || size < IpVersion.V4.getMinHeaderLength() || size > buffer.length) {
            return;
        }
        IpVersion candidate = IpVersion.of((buffer[0] >> 4) & 0x0F);
        if (candidate == IpVersion.V4) {
            int headerLength = (buffer[0] & 0x0F) * 4;
            if (headerLength >= candidate.getMinHeaderLength() && size >= headerLength) {
                version = candidate;
                ipHeaderLength = headerLength;
                protocol = buffer[9] & 0xFF;
            }
        } else if (candidate == IpVersion.V6 && size >= candidate.getMinHeaderLength()) {
            version = candidate;
            ipHeaderLength = candidate.getMinHeaderLength();
            protocol = buffer[6] & 0xFF;
        }
    }

    public boolean isTcp() {
        return protocol == IpProtocol.TCP.getNumber();
    }

    public boolean isUdp() {
        return protocol == IpProtocol.UDP.getNumber();
    }

    public boolean isOk() {
        return version != IpVersion.UNKNOWN;
    }

    public boolean isIpv4() {
        return version == IpVersion.V4;
    }

    public int getDestinationIpv4() {
        return version == IpVersion.V4 ? readInt(version.getDestinationOffset()) : 0;
    }

    private int readInt(int index) {
        return ((data[index] & 0xFF) << 24)
                | ((data[index + 1] & 0xFF) << 16)
                | ((data[index + 2] & 0xFF) << 8)
                | (data[index + 3] & 0xFF);
    }

    public InetAddress getSource() {
        return address(version.getSourceOffset());
    }

    public byte[] getData() {
        return data;
    }

    public int getLength() {
        return length;
    }

    public IPPacket copy() {
        return new IPPacket(Arrays.copyOf(data, length), length);
    }

    public int getFlags() {
        return (isTcp() && length >= ipHeaderLength + TCP_MIN_HEADER) ? u8(ipHeaderLength + 13) : 0;
    }

    public boolean isSyn() {
        return (getFlags() & FLAG_SYN) != 0;
    }

    public boolean isAck() {
        return (getFlags() & FLAG_ACK) != 0;
    }

    public boolean isFin() {
        return (getFlags() & FLAG_FIN) != 0;
    }

    public boolean isRst() {
        return (getFlags() & FLAG_RST) != 0;
    }

    public long getSequence() {
        return isTcp() ? u32(ipHeaderLength + 4) : 0;
    }

    public long getAcknowledgment() {
        return isTcp() ? u32(ipHeaderLength + 8) : 0;
    }

    private long u32(int index) {
        return ((long) u16(index) << 16) | u16(index + 2);
    }

    public InetAddress getDestination() {
        return address(version.getDestinationOffset());
    }

    public int getSourcePort() {
        return hasPorts() ? u16(ipHeaderLength) : -1;
    }

    public int getDestinationPort() {
        return hasPorts() ? u16(ipHeaderLength + 2) : -1;
    }

    public int getPayloadOffset() {
        if (isTcp() && length >= ipHeaderLength + TCP_MIN_HEADER) {
            return ipHeaderLength + ((u8(ipHeaderLength + 12) >> 4) & 0x0F) * 4;
        }
        if (isUdp() && length >= ipHeaderLength + UDP_HEADER) {
            return ipHeaderLength + UDP_HEADER;
        }
        return -1;
    }

    public int getPayloadLength() {
        int offset = getPayloadOffset();
        return offset < 0 ? 0 : Math.max(0, length - offset);
    }

    private boolean isDns() {
        return isUdp()
                && (getSourcePort() == DNS_PORT || getDestinationPort() == DNS_PORT)
                && getPayloadLength() >= DNS_HEADER;
    }

    public boolean isDnsQuery() {
        return isDns()
                && getDestinationPort() == DNS_PORT
                && (u8(getPayloadOffset() + 2) & DNS_FLAG_RESPONSE) == 0;
    }

    public boolean isDnsResponse() {
        return isDns()
                && getSourcePort() == DNS_PORT
                && (u8(getPayloadOffset() + 2) & DNS_FLAG_RESPONSE) != 0;
    }

    public DnsRecordType getDnsQueryType() {
        if (!isDns()) {
            return DnsRecordType.OTHER;
        }
        int base = getPayloadOffset();
        if (u16(base + 4) == 0) {
            return DnsRecordType.OTHER;
        }
        int[] cursor = {base + DNS_HEADER};
        parseName(base, cursor);
        return cursor[0] + 4 > length ? DnsRecordType.OTHER : DnsRecordType.of(u16(cursor[0]));
    }

    public String getDnsDomain() {
        if (!isDns()) {
            return null;
        }
        int base = getPayloadOffset();
        if (u16(base + 4) == 0) {
            return null;
        }
        int[] cursor = {base + DNS_HEADER};
        String name = parseName(base, cursor);
        return name.isEmpty() ? null : name.toLowerCase();
    }

    public List<InetAddress> getDnsAddresses() {
        if (!isDnsResponse()) {
            return Collections.emptyList();
        }
        int base = getPayloadOffset();
        int questions = u16(base + 4);
        int answers = u16(base + 6);
        int[] cursor = {base + DNS_HEADER};

        for (int i = 0; i < questions; i++) {
            parseName(base, cursor);
            cursor[0] += 4;
            if (cursor[0] > length) {
                return Collections.emptyList();
            }
        }

        List<InetAddress> result = new ArrayList<>();
        for (int i = 0; i < answers; i++) {
            parseName(base, cursor);
            if (cursor[0] + 10 > length) {
                break;
            }
            DnsRecordType type = DnsRecordType.of(u16(cursor[0]));
            int rdLength = u16(cursor[0] + 8);
            int rdOffset = cursor[0] + 10;
            cursor[0] = rdOffset + rdLength;
            if (cursor[0] > length) {
                break;
            }
            if (type.isAddress(rdLength)) {
                byte[] raw = new byte[rdLength];
                System.arraycopy(data, rdOffset, raw, 0, rdLength);
                try {
                    result.add(InetAddress.getByAddress(raw));
                } catch (UnknownHostException ignored) {
                }
            }
        }
        return result;
    }

    public String getSni() {
        int offset = getPayloadOffset();
        int payload = getPayloadLength();
        if (!isTcp() || offset < 0 || payload < TLS_MIN_CLIENT_HELLO) {
            return null;
        }
        if (u8(offset) != TLS_RECORD_HANDSHAKE || u8(offset + 5) != TLS_CLIENT_HELLO) {
            return null;
        }
        int end = offset + payload;
        int position = offset + TLS_RANDOM_END;

        position += 1 + safeU8(position, end);
        if (position + 2 > end) {
            return null;
        }
        position += 2 + u16(position);
        if (position + 1 > end) {
            return null;
        }
        position += 1 + safeU8(position, end);
        if (position + 2 > end) {
            return null;
        }
        int extensionsEnd = Math.min(end, position + 2 + u16(position));
        position += 2;

        while (position + 4 <= extensionsEnd) {
            int type = u16(position);
            int size = u16(position + 2);
            int body = position + 4;
            if (body + size > extensionsEnd) {
                return null;
            }
            if (type == TLS_EXTENSION_SERVER_NAME && size >= 5) {
                int nameLength = u16(body + 3);
                if (body + 5 + nameLength > extensionsEnd) {
                    return null;
                }
                return new String(data, body + 5, nameLength).toLowerCase();
            }
            position = body + size;
        }
        return null;
    }

    public static byte[] buildTcp(IPPacket request, long seq, long ack, int flags, int window,
            byte[] payload, int payloadOffset, int payloadLength) {
        boolean withMss = (flags & FLAG_SYN) != 0;
        int tcpLength = TCP_MIN_HEADER + (withMss ? 4 : 0);
        byte[] out = newReply(request, IpProtocol.TCP.getNumber(), tcpLength + payloadLength);

        int tcp = request.ipHeaderLength;
        writeU16(out, tcp, request.getDestinationPort());
        writeU16(out, tcp + 2, request.getSourcePort());
        writeU32(out, tcp + 4, seq);
        writeU32(out, tcp + 8, ack);
        out[tcp + 12] = (byte) ((tcpLength / 4) << 4);
        out[tcp + 13] = (byte) flags;
        writeU16(out, tcp + 14, window);
        if (withMss) {
            out[tcp + TCP_MIN_HEADER] = 0x02;
            out[tcp + TCP_MIN_HEADER + 1] = 0x04;
            writeU16(out, tcp + TCP_MIN_HEADER + 2, DEFAULT_MSS);
        }
        if (payloadLength > 0) {
            System.arraycopy(payload, payloadOffset, out, tcp + tcpLength, payloadLength);
        }

        writeU16(out, tcp + 16, checksum(
                pseudoHeaderSum(out, request.version, IpProtocol.TCP.getNumber(), tcpLength + payloadLength)
                        + sum(out, tcp, tcpLength + payloadLength)));
        return out;
    }

    public static byte[] buildUdp(IPPacket request, byte[] payload, int payloadOffset,
            int payloadLength) {
        int udpLength = UDP_HEADER + payloadLength;
        byte[] out = newReply(request, IpProtocol.UDP.getNumber(), udpLength);

        int udp = request.ipHeaderLength;
        writeU16(out, udp, request.getDestinationPort());
        writeU16(out, udp + 2, request.getSourcePort());
        writeU16(out, udp + 4, udpLength);
        if (payloadLength > 0) {
            System.arraycopy(payload, payloadOffset, out, udp + UDP_HEADER, payloadLength);
        }

        writeU16(out, udp + 6, checksum(
                pseudoHeaderSum(out, request.version, IpProtocol.UDP.getNumber(), udpLength)
                        + sum(out, udp, udpLength)));
        return out;
    }

    private static byte[] newReply(IPPacket request, int protocol, int transportLength) {
        IpVersion version = request.version;
        int ipLength = request.ipHeaderLength;
        byte[] out = new byte[ipLength + transportLength];

        if (version == IpVersion.V4) {
            out[0] = (byte) (0x40 | (ipLength / 4));
            writeU16(out, 2, out.length);
            out[6] = 0x40;
            out[8] = DEFAULT_HOP_LIMIT;
            out[9] = (byte) protocol;
        } else {
            out[0] = 0x60;
            writeU16(out, 4, transportLength);
            out[6] = (byte) protocol;
            out[7] = DEFAULT_HOP_LIMIT;
        }
        System.arraycopy(request.data, version.getDestinationOffset(),
                out, version.getSourceOffset(), version.getAddressLength());
        System.arraycopy(request.data, version.getSourceOffset(),
                out, version.getDestinationOffset(), version.getAddressLength());

        if (version == IpVersion.V4) {
            writeU16(out, 10, checksum(sum(out, 0, ipLength)));
        }
        return out;
    }

    public byte[] buildDnsAnswer(DnsRecordType type, byte[] rdata, int ttlSeconds) {
        if (!isDnsQuery() || type == DnsRecordType.OTHER || rdata == null) {
            return null;
        }
        int base = getPayloadOffset();
        int[] cursor = {base + DNS_HEADER};
        parseName(base, cursor);
        int questionEnd = cursor[0] + 4;
        if (questionEnd > length) {
            return null;
        }
        int answerLength = 12 + rdata.length;
        int total = questionEnd + answerLength;
        byte[] out = new byte[total];
        System.arraycopy(data, 0, out, 0, questionEnd);

        int at = questionEnd;
        out[at++] = (byte) DNS_NAME_POINTER;
        out[at++] = (byte) DNS_HEADER;
        writeU16(out, at, type.getNumber());
        at += 2;
        writeU16(out, at, DNS_CLASS_IN);
        at += 2;
        writeU32(out, at, ttlSeconds);
        at += 4;
        writeU16(out, at, rdata.length);
        at += 2;
        System.arraycopy(rdata, 0, out, at, rdata.length);

        out[base + 2] = (byte) ((out[base + 2] & 0x01) | DNS_FLAG_RESPONSE);
        out[base + 3] = (byte) DNS_FLAG_RESPONSE;
        writeU16(out, base + 6, 1);
        writeU16(out, base + 8, 0);
        writeU16(out, base + 10, 0);

        finishDnsResponse(out, total);
        return out;
    }

    public byte[] buildDnsNxDomain() {
        if (!isDnsQuery()) {
            return null;
        }
        byte[] out = Arrays.copyOf(data, length);
        int base = getPayloadOffset();
        out[base + 2] = (byte) ((out[base + 2] & 0x01) | DNS_FLAG_RESPONSE);
        out[base + 3] = (byte) DNS_RCODE_NXDOMAIN;
        finishDnsResponse(out, length);
        return out;
    }

    private void finishDnsResponse(byte[] out, int total) {
        int udp = ipHeaderLength;
        if (version == IpVersion.V4) {
            swap(out, version.getSourceOffset(), version.getDestinationOffset(),
                    version.getAddressLength());
            out[8] = DEFAULT_HOP_LIMIT;
            writeU16(out, 2, total);
        } else {
            swap(out, version.getSourceOffset(), version.getDestinationOffset(),
                    version.getAddressLength());
            out[7] = DEFAULT_HOP_LIMIT;
            writeU16(out, 4, total - ipHeaderLength);
        }

        int sourcePort = u16Of(out, udp);
        writeU16(out, udp, u16Of(out, udp + 2));
        writeU16(out, udp + 2, sourcePort);
        writeU16(out, udp + 4, total - udp);
        writeU16(out, udp + 6, 0);

        if (version == IpVersion.V4) {
            writeU16(out, 10, 0);
            writeU16(out, 10, checksum(sum(out, 0, ipHeaderLength)));
        } else {
            writeU16(out, udp + 6, checksum(
                    pseudoHeaderSum(out, version, IpProtocol.UDP.getNumber(), total - udp)
                            + sum(out, udp, total - udp)));
        }
    }

    private boolean hasPorts() {
        return (isTcp() || isUdp()) && length >= ipHeaderLength + 4;
    }

    private InetAddress address(int offset) {
        int size = version.getAddressLength();
        if (!isOk() || offset + size > length) {
            return null;
        }
        byte[] raw = new byte[size];
        System.arraycopy(data, offset, raw, 0, size);
        try {
            return InetAddress.getByAddress(raw);
        } catch (UnknownHostException e) {
            return null;
        }
    }

    private String parseName(int base, int[] cursor) {
        StringBuilder name = new StringBuilder();
        boolean jumped = false;
        int position = cursor[0];
        for (int i = 0; i < MAX_LABELS && position < length; i++) {
            int labelLength = u8(position);
            if (labelLength == 0) {
                if (!jumped) {
                    cursor[0] = position + 1;
                }
                break;
            }
            if ((labelLength & 0xC0) == 0xC0) {
                if (position + 2 > length) {
                    break;
                }
                if (!jumped) {
                    cursor[0] = position + 2;
                }
                jumped = true;
                position = base + (((labelLength & 0x3F) << 8) | u8(position + 1));
                continue;
            }
            position++;
            if (position + labelLength > length) {
                break;
            }
            if (name.length() > 0) {
                name.append('.');
            }
            for (int j = 0; j < labelLength; j++) {
                name.append((char) u8(position++));
            }
        }
        return name.toString();
    }

    private int safeU8(int position, int end) {
        return position < end ? u8(position) : 0;
    }

    private int u8(int index) {
        return data[index] & 0xFF;
    }

    private int u16(int index) {
        return ((data[index] & 0xFF) << 8) | (data[index + 1] & 0xFF);
    }

    private static void writeU16(byte[] out, int index, int value) {
        out[index] = (byte) (value >> 8);
        out[index + 1] = (byte) value;
    }

    private static void writeU32(byte[] out, int index, long value) {
        out[index] = (byte) (value >> 24);
        out[index + 1] = (byte) (value >> 16);
        out[index + 2] = (byte) (value >> 8);
        out[index + 3] = (byte) value;
    }

    private static int sum(byte[] p, int offset, int size) {
        int total = 0;
        for (int i = offset; i < offset + size - 1; i += 2) {
            total += ((p[i] & 0xFF) << 8) | (p[i + 1] & 0xFF);
        }
        if ((size & 1) != 0) {
            total += (p[offset + size - 1] & 0xFF) << 8;
        }
        return total;
    }

    private static int pseudoHeaderSum(byte[] out, IpVersion version, int protocol,
            int transportLength) {
        int total = sum(out, version.getSourceOffset(), version.getAddressLength() * 2);
        total += protocol;
        total += transportLength;
        return total;
    }

    private static void swap(byte[] out, int first, int second, int size) {
        for (int i = 0; i < size; i++) {
            byte value = out[first + i];
            out[first + i] = out[second + i];
            out[second + i] = value;
        }
    }

    private static int u16Of(byte[] p, int index) {
        return ((p[index] & 0xFF) << 8) | (p[index + 1] & 0xFF);
    }

    private static int checksum(int total) {
        while ((total >> 16) != 0) {
            total = (total & 0xFFFF) + (total >> 16);
        }
        return ~total & 0xFFFF;
    }
}
