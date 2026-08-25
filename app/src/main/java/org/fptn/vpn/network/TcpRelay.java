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
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Random;

final class TcpRelay {
    private static final String TAG = TcpRelay.class.getSimpleName();

    private static final int WINDOW = 32 * 1024;
    private static final int SEND_BUFFER = 64 * 1024;
    private static final int MAX_SEGMENT = 1400;
    private static final long SEQ_MASK = 0xFFFFFFFFL;

    private static final Random RANDOM = new Random();

    private final Splitter splitter;
    private final Splitter.Bridge bridge;
    private final long flowKey;
    private final IPPacket template;
    private final SocketChannel channel;

    private final ByteBuffer toServer = ByteBuffer.allocate(SEND_BUFFER);
    private final ByteBuffer fromServer = ByteBuffer.allocate(MAX_SEGMENT);

    private final long openedAt = System.currentTimeMillis();

    private long sendNext;
    private long receiveNext;
    private boolean connected;
    private boolean closed;

    private TcpRelay(Splitter splitter, Splitter.Bridge bridge, long flowKey, IPPacket syn,
            SocketChannel channel) {
        this.splitter = splitter;
        this.bridge = bridge;
        this.flowKey = flowKey;
        this.template = syn.copy();
        this.channel = channel;
        this.sendNext = RANDOM.nextInt() & SEQ_MASK;
        this.receiveNext = (syn.getSequence() + 1) & SEQ_MASK;
    }

    static TcpRelay open(Splitter splitter, Splitter.Bridge bridge, long flowKey, IPPacket syn) {
        SocketChannel channel = null;
        try {
            channel = SocketChannel.open();
            channel.configureBlocking(false);
            if (!bridge.protect(channel.socket())) {
                throw new IOException("protect failed");
            }
            TcpRelay relay = new TcpRelay(splitter, bridge, flowKey, syn, channel);
            channel.connect(new InetSocketAddress(syn.getDestination(), syn.getDestinationPort()));
            splitter.register(channel, SelectionKey.OP_CONNECT, relay::onSelected);
            XLog.tag(TAG).i("Relay opened [dst=%s:%d]",
                    syn.getDestination().getHostAddress(), syn.getDestinationPort());
            return relay;
        } catch (Exception e) {
            XLog.tag(TAG).w("TCP relay failed [%s:%d]: %s",
                    syn.getDestination(), syn.getDestinationPort(), e.getMessage());
            closeQuietly(channel);
            return null;
        }
    }

    synchronized void onPacketFromApp(IPPacket packet) {
        if (closed) {
            return;
        }
        if (packet.isRst()) {
            close();
            return;
        }
        if (packet.isSyn()) {
            if (connected) {
                sendToApp(sendNext - 1, IPPacket.FLAG_SYN | IPPacket.FLAG_ACK, null, 0, 0);
            }
            return;
        }

        int offset = packet.getPayloadOffset();
        int payloadLength = packet.getPayloadLength();
        if (payloadLength > 0 && packet.getSequence() == receiveNext) {
            if (toServer.remaining() < payloadLength) {
                return;
            }
            toServer.put(packet.getData(), offset, payloadLength);
            receiveNext = (receiveNext + payloadLength) & SEQ_MASK;
            splitter.updateInterest(channel, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        }

        if (packet.isFin()) {
            receiveNext = (receiveNext + 1) & SEQ_MASK;
            splitter.updateInterest(channel, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        }

        if (payloadLength > 0 || packet.isFin()) {
            sendToApp(sendNext, IPPacket.FLAG_ACK, null, 0, 0);
        }
    }

    private void onSelected(SelectionKey key) {
        if (closed) {
            return;
        }
        try {
            if (key.isConnectable() && channel.finishConnect()) {
                onConnected();
            }
            if (key.isValid() && key.isWritable()) {
                flushToServer();
            }
            if (key.isValid() && key.isReadable()) {
                readFromServer();
            }
        } catch (IOException | RuntimeException e) {
            XLog.tag(TAG).w("TCP relay error [%s]: %s",
                    e.getClass().getSimpleName(), e.getMessage());
            synchronized (this) {
                sendToApp(sendNext, IPPacket.FLAG_RST, null, 0, 0);
                close();
            }
        }
    }

    private synchronized void onConnected() {
        connected = true;
        XLog.tag(TAG).i("Relay connected [dst=%s:%d, tookMs=%d]",
                template.getDestination().getHostAddress(), template.getDestinationPort(),
                System.currentTimeMillis() - openedAt);
        sendToApp(sendNext, IPPacket.FLAG_SYN | IPPacket.FLAG_ACK, null, 0, 0);
        sendNext = (sendNext + 1) & SEQ_MASK;
        splitter.updateInterest(channel, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
    }

    private synchronized void flushToServer() throws IOException {
        toServer.flip();
        if (toServer.hasRemaining()) {
            channel.write(toServer);
        }
        toServer.compact();
        splitter.updateInterest(channel, SelectionKey.OP_READ);
    }

    private void readFromServer() throws IOException {
        while (true) {
            fromServer.clear();
            int read = channel.read(fromServer);
            if (read == 0) {
                return;
            }
            synchronized (this) {
                if (read < 0) {
                    sendToApp(sendNext, IPPacket.FLAG_FIN | IPPacket.FLAG_ACK, null, 0, 0);
                    sendNext = (sendNext + 1) & SEQ_MASK;
                    close();
                    return;
                }
                sendToApp(sendNext, IPPacket.FLAG_PSH | IPPacket.FLAG_ACK,
                        fromServer.array(), 0, read);
                sendNext = (sendNext + read) & SEQ_MASK;
            }
        }
    }

    private void sendToApp(long seq, int flags, byte[] payload, int offset, int length) {
        bridge.toTun(IPPacket.buildTcp(template, seq & SEQ_MASK, receiveNext,
                flags, WINDOW, payload, offset, length));
    }

    synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        closeQuietly(channel);
        splitter.removeTcp(flowKey);
    }

    private static void closeQuietly(SocketChannel channel) {
        if (channel != null) {
            try {
                channel.close();
            } catch (IOException ignored) {
            }
        }
    }
}
