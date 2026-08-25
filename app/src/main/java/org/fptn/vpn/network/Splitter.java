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
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public class Splitter {
    private static final String TAG = Splitter.class.getSimpleName();

    private static final int MAX_ADDRESSES = 1024 * 16;
    private static final long ADDRESS_TTL_MILLIS = 24 * 60 * 60 * 1000L; // 24 hours
    private static final long UDP_IDLE_MILLIS = 60 * 1000L;
    private static final long SELECT_TIMEOUT_MILLIS = 1000L;

    public interface Bridge {
        void toTun(byte[] packet);

        boolean protect(Socket socket);

        boolean protect(DatagramSocket socket);
    }

    private final Set<String> domains;
    private final Bridge bridge;

    private final Map<Integer, Long> addresses;
    private final Map<Long, TcpRelay> tcpFlows = new ConcurrentHashMap<>();
    private final Map<Long, UdpRelay> udpFlows = new ConcurrentHashMap<>();

    private final Queue<Runnable> tasks = new ConcurrentLinkedQueue<>();
    private final Selector selector;
    private final Thread ioThread;
    private volatile boolean running = true;

    public Splitter(Set<String> domains, Map<Integer, Long> addresses, Bridge bridge)
            throws IOException {
        this.domains = domains;
        this.addresses = addresses;
        this.bridge = bridge;
        this.selector = Selector.open();
        this.ioThread = new Thread(this::runIoLoop, "splitter-io");
        this.ioThread.setDaemon(true);
        this.ioThread.start();
        XLog.tag(TAG).i("Split tunneling enabled [domains=%d, knownAddresses=%d]",
                domains.size(), addresses.size());
    }

    public boolean handleOutbound(IPPacket packet) {
        if (!packet.isIpv4()) {
            return false;
        }
        if (packet.isTcp()) {
            return handleTcp(packet);
        }
        if (packet.isUdp()) {
            return handleUdp(packet);
        }
        return false;
    }

    public void handleInbound(IPPacket packet) {
        if (!packet.isDnsResponse()) {
            return;
        }
        String domain = packet.getDnsDomain();
        if (!DomainBlocker.matches(domains, domain)) {
            return;
        }
        for (InetAddress address : packet.getDnsAddresses()) {
            remember(address, domain, "dns");
        }
    }

    private boolean resetIfBypass(IPPacket packet) {
        String sni = packet.getSni();
        if (!DomainBlocker.matches(domains, sni)) {
            return false;
        }
        remember(packet.getDestination(), sni, "sni");
        bridge.toTun(IPPacket.buildTcp(packet, packet.getAcknowledgment(),
                packet.getSequence() + packet.getPayloadLength(),
                IPPacket.FLAG_RST | IPPacket.FLAG_ACK, 0, null, 0, 0));
        XLog.tag(TAG).i("Tunneled flow reset before ClientHello [domain=%s, address=%s]",
                sni, packet.getDestination().getHostAddress());
        return true;
    }

    public void close() {
        running = false;
        selector.wakeup();
        for (TcpRelay relay : tcpFlows.values()) {
            relay.close();
        }
        for (UdpRelay relay : udpFlows.values()) {
            relay.close();
        }
        try {
            selector.close();
        } catch (IOException ignored) {
        }
    }

    void register(SelectableChannel channel, int ops, Consumer<SelectionKey> handler) {
        tasks.add(() -> {
            try {
                channel.register(selector, ops, handler);
            } catch (Exception e) {
                XLog.tag(TAG).w("Register failed: %s", e.getMessage());
            }
        });
        selector.wakeup();
    }

    void updateInterest(SelectableChannel channel, int ops) {
        tasks.add(() -> {
            SelectionKey key = channel.keyFor(selector);
            if (key != null && key.isValid()) {
                key.interestOps(ops);
            }
        });
        selector.wakeup();
    }

    void removeTcp(long flowKey) {
        tcpFlows.remove(flowKey);
    }

    void removeUdp(long flowKey) {
        udpFlows.remove(flowKey);
    }

    private boolean handleTcp(IPPacket packet) {
        long flowKey = flowKey(packet);
        TcpRelay relay = tcpFlows.get(flowKey);
        if (relay != null) {
            relay.onPacketFromApp(packet);
            return true;
        }
        if (!packet.isSyn() || packet.isAck()) {
            return resetIfBypass(packet);
        }
        if (!isBypass(packet.getDestinationIpv4())) {
            return false;
        }
        relay = TcpRelay.open(this, bridge, flowKey, packet);
        if (relay == null) {
            return false;
        }
        tcpFlows.put(flowKey, relay);
        return true;
    }

    private boolean handleUdp(IPPacket packet) {
        long flowKey = flowKey(packet);
        UdpRelay relay = udpFlows.get(flowKey);
        if (relay == null) {
            if (!isBypass(packet.getDestinationIpv4())) {
                return false;
            }
            relay = UdpRelay.open(this, bridge, flowKey, packet);
            if (relay == null) {
                return false;
            }
            udpFlows.put(flowKey, relay);
        }
        relay.onPacketFromApp(packet);
        return true;
    }

    private static long flowKey(IPPacket packet) {
        return ((long) packet.getDestinationIpv4() << 32)
                | ((long) packet.getSourcePort() << 16)
                | packet.getDestinationPort();
    }

    private boolean isBypass(int address) {
        Long expiry = addresses.get(address);
        if (expiry == null) {
            return false;
        }
        if (expiry < System.currentTimeMillis()) {
            addresses.remove(address);
            return false;
        }
        return true;
    }

    private void remember(InetAddress address, String domain, String source) {
        if (!(address instanceof Inet4Address)) {
            return;
        }
        int key = toInt(address);
        long expiry = System.currentTimeMillis() + ADDRESS_TTL_MILLIS;
        if (addresses.replace(key, expiry) != null) {
            return;
        }
        if (addresses.size() >= MAX_ADDRESSES) {
            XLog.tag(TAG).w("Bypass address limit reached [max=%d], ignoring %s",
                    MAX_ADDRESSES, address.getHostAddress());
            return;
        }
        addresses.put(key, expiry);
        XLog.tag(TAG).i("Bypass address learned [source=%s, domain=%s, address=%s, total=%d]",
                source, domain, address.getHostAddress(), addresses.size());
    }

    private static int toInt(InetAddress address) {
        byte[] raw = address.getAddress();
        return ((raw[0] & 0xFF) << 24) | ((raw[1] & 0xFF) << 16)
                | ((raw[2] & 0xFF) << 8) | (raw[3] & 0xFF);
    }

    private void runIoLoop() {
        while (running) {
            try {
                Runnable task;
                while ((task = tasks.poll()) != null) {
                    task.run();
                }
                selector.select(SELECT_TIMEOUT_MILLIS);
                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
                while (keys.hasNext()) {
                    SelectionKey key = keys.next();
                    keys.remove();
                    @SuppressWarnings("unchecked")
                    Consumer<SelectionKey> handler = (Consumer<SelectionKey>) key.attachment();
                    handler.accept(key);
                }
                evictIdleUdp();
            } catch (Exception e) {
                if (running) {
                    XLog.tag(TAG).w("Splitter io loop error [%s]: %s",
                            e.getClass().getSimpleName(), e.getMessage());
                }
            }
        }
    }

    private void evictIdleUdp() {
        long deadline = System.currentTimeMillis() - UDP_IDLE_MILLIS;
        for (UdpRelay relay : udpFlows.values()) {
            if (relay.getLastActivity() < deadline) {
                relay.close();
            }
        }
    }
}
