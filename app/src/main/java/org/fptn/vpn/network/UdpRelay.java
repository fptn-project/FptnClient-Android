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

import com.elvishew.xlog.XLog;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;

final class UdpRelay {
    private static final String TAG = UdpRelay.class.getSimpleName();
    private static final int MAX_DATAGRAM = 65535;

    private final Splitter splitter;
    private final Splitter.Bridge bridge;
    private final long flowKey;
    private final IPPacket template;
    private final DatagramChannel channel;
    private final ByteBuffer readBuffer = ByteBuffer.allocate(MAX_DATAGRAM);

    private volatile long lastActivity = System.currentTimeMillis();

    private UdpRelay(Splitter splitter, Splitter.Bridge bridge, long flowKey, IPPacket template,
            DatagramChannel channel) {
        this.splitter = splitter;
        this.bridge = bridge;
        this.flowKey = flowKey;
        this.template = template;
        this.channel = channel;
    }

    static UdpRelay open(Splitter splitter, Splitter.Bridge bridge, long flowKey, IPPacket first) {
        DatagramChannel channel = null;
        try {
            channel = DatagramChannel.open();
            channel.configureBlocking(false);
            if (!bridge.protect(channel.socket())) {
                throw new IOException("protect failed");
            }
            channel.connect(new InetSocketAddress(first.getDestination(), first.getDestinationPort()));

            UdpRelay relay = new UdpRelay(splitter, bridge, flowKey, first.copy(), channel);
            splitter.register(channel, SelectionKey.OP_READ, key -> relay.onReadable());
            XLog.tag(TAG).i("Relay opened [dst=%s:%d]",
                    first.getDestination().getHostAddress(), first.getDestinationPort());
            return relay;
        } catch (Exception e) {
            XLog.tag(TAG).w("UDP relay failed [%s:%d]: %s",
                    first.getDestination(), first.getDestinationPort(), e.getMessage());
            closeQuietly(channel);
            return null;
        }
    }

    void onPacketFromApp(IPPacket packet) {
        lastActivity = System.currentTimeMillis();
        int offset = packet.getPayloadOffset();
        int payloadLength = packet.getPayloadLength();
        if (offset < 0 || payloadLength <= 0) {
            return;
        }
        try {
            channel.write(ByteBuffer.wrap(packet.getData(), offset, payloadLength));
        } catch (IOException e) {
            XLog.tag(TAG).w("UDP write failed [%s]: %s", e.getClass().getSimpleName(), e.getMessage());
            close();
        }
    }

    private void onReadable() {
        try {
            while (true) {
                readBuffer.clear();
                int read = channel.read(readBuffer);
                if (read <= 0) {
                    return;
                }
                lastActivity = System.currentTimeMillis();
                bridge.toTun(IPPacket.buildUdp(template, readBuffer.array(), 0, read));
            }
        } catch (IOException e) {
            XLog.tag(TAG).w("UDP read failed [%s]: %s", e.getClass().getSimpleName(), e.getMessage());
            close();
        }
    }

    long getLastActivity() {
        return lastActivity;
    }

    void close() {
        closeQuietly(channel);
        splitter.removeUdp(flowKey);
    }

    private static void closeQuietly(DatagramChannel channel) {
        if (channel != null) {
            try {
                channel.close();
            } catch (IOException ignored) {
            }
        }
    }
}
