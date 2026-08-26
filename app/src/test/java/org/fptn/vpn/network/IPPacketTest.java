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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.util.List;

public class IPPacketTest {

    private static final int PROTO_TCP = 6;
    private static final int PROTO_UDP = 17;

    private static final byte[] CLIENT_IP = {10, 10, 0, 1};
    private static final byte[] SERVER_IP = {77, (byte) 88, 44, (byte) 242};

    private static final String REAL_IP_DNS_QUERY =
              "4500003929e1000040117997c0a8068408080808d6dd0035002512746f7301000001000000000000076578616d706c650363"
            + "6f6d0000010001";

    private static final String REAL_IP_DNS_RESPONSE =
              "45000059c54600007111ad1108080808c0a806840035d6dd0045c36f6f7381800001000200000000076578616d706c650363"
            + "6f6d0000010001c00c00010001000000e200046814179ac00c00010001000000e20004ac4293f3";

    private static final String REAL_IP_TCP_SYN =
              "450000400000400040063356c0a80684ac4293f3defa01bb900b198500000000b0c2ffffc03e0000020405b4010303060101"
            + "080a99d84a7c0000000004020000";

    private static final String REAL_IP_TCP_SYN_ACK =
              "4500003c0000400039063a5aac4293f3c0a8068401bbdefad8924c79900b1986a052ffff84e90000020405780402080a5644"
            + "d1ad99d84a7c0103030d";

    private static final String REAL_IP_CLIENT_HELLO =
              "450205a00000400040062df4c0a80684ac4293f3defa01bb900b1986d8924c7a8010080d1b6800000101080a99d84a855644"
            + "d1ad16030105e7010005e30303a03d5f01739b03829df3d70d5574b96c424f6103a3d684c93bca0df790f7fee4207d19463b"
            + "f207e8c0c41bd2ee1cc115333ef1510ecb2c5a520189ff2ebed805350022130213031301c02cc030c02bc02fcca9cca8c024"
            + "c028c023c027009f009e006b006701000578ff0100010000000010000e00000b6578616d706c652e636f6d000b0002010000"
            + "0a0012001011ec001d0017001e0018001901000101002300000016000000170000000d003600340905090609040403050306"
            + "0308070808081a081b081c0809080a080b080408050806040105010601030303010302040205020602002b00050403040303"
            + "002d00020101003304ea04e811ec04c0ca590184a3a066417c145b672938ce233aa5197583c9c67cce1ab55e651f47aa579c"
            + "cc3f042c5682cb958a30aee183239e1123d06447ca7b399d35bcf1689577b4ba3ebc3c93b61fee567425d7320839b7c59cc6"
            + "264c56871a3e362991b280556ed48f79846c392b4abe025cd7da3ebc643db151c941e220579023933cb9f7f2cc160a9470f2"
            + "4741195c8087b2515b99ffc8cff16072ee358ecd0a5129b255050a5d4cf02b3ad3b901ea34bf513b9fd54f36f406fbd1397c"
            + "2c1f92b5c991333f3171ac82c7a915f2a5cda39a08dc9995a8a68c3c69c7c7a8cd8973a44a87c32891b3459b9684abca0c1e"
            + "1ba9cafe8bc7aa694bbf7a54e6997fb3083a84c9a066bb015d562330fba726b85dd16919b3e2ce33b44cd0975d3c48a8f97a"
            + "c65c8b724dea029e97ae3486ba3d98a05b19a07de83709ecc4641b1da16648d6840ffe5c681d45ce9c6b8c65ab2f986b4fa0"
            + "411caf84737d235ffc853fa5f54675036a711cb5c666027a1b06c8a53c45f4c92345a82dc62643d610ba79434426300bd6a5"
            + "1d012e1be7589387855295cf40b4c74668b4ad4b2c01f57d75087b39781db85886389c86c3ca66bf251ded65a2a254460803"
            + "531ab9673fb39f2b93c8a70a820975ccb0a5ca164ba72a049bd7fa9af7d047b7099895b46ff006ad7a5963b380bad56a2a03"
            + "2311576333d34011c9e023c249a81e2b5f760899428966a6b1324250ac75839ddf4c07801c7fb272254f77bd93307ec29317"
            + "b78874b8f72633105b6b191028731fa2e6781b1a5a0dca3d1707ad21249f470565b2464b40321d6246827cd6816022451521"
            + "8a99e93e8ea9bb9f82202eeb9783ec4c22902d5bac3e4a5223cbe4ab0fd7627a5baae1f8066040940fba0fb575c3de0ab087"
            + "39cfd33b05eb7a2de814cdbd76c1f5775662605a38b99f2ef366f01c9112f986e94a2740da65d012529cfb1462fc51d279c7"
            + "da3b8056fc7c6a796daad7ac8ef8b25962b2cb4a568173bca1a44aba8952a0ac5b7162c80960cec2ec642815602a402c6bc5"
            + "96b1e00dd621425fe39edd872b1dacc9b9b28e91691d78da573b6b69bfc88513f0bd36d5a08ab629ab0cb047391b2574212f"
            + "f9acac163a85fa84accc1e7bec9ae41782a6097f0f937e517ac53dd4b025ea735d66aae1ec2f21098220e059870381b415ac"
            + "2196710eb801c0a5329f7acee19a973eb291bbe2ceec0b2117cc87f054c7bab02a3a2734e9248e5e494aeffaa8a02674a2e9"
            + "cdd371602b69be8bd4cd7e0750b6cb3ba6a592b0b847cdb48f450236dbdc52ab27a3e52a8f541081f36c400a7acad53591da"
            + "443571621e6828bf5313079df56638f1b2e0343471ec25ee028941a6567638ca6c65870913a898224155f311e0333076e8ad"
            + "a2eb44497b4903379fadc16932da14795036c1a68c458c98b8f8721ad3206ce941cc967a42084745dc4f6c0b2ce7a7557b12"
            + "8d06e9bab4aa0def9110d95387b991b987e98c4c906d6302b220193338653c4066a9c61a547f2a252e6c9dd2ba419059992d"
            + "037f39a2ae99da0a87cb0607e06da25ccb100a91aad55e1970818eba27f27c58edd0c220933fb89a";

    private static final String REAL_DNS_QUERY_YA_RU =
              "1d60010000010000000000000279610272750000010001";

    private static final String REAL_DNS_RESPONSE_YA_RU =
              "1d60818000010003000000000279610272750000010001c00c000100010000002c00044d582cf2c00c000100010000002c00"
            + "044d5837f2c00c000100010000002c000405fffff2";

    private static final String REAL_DNS_RESPONSE_GITHUB =
              "c49381800001000200000000037777770667697468756203636f6d0000010001c00c0005000100000ced0002c010c0100001"
            + "000100000024000404ed1626";

    private static final String REAL_CLIENT_HELLO =
              "16030105ec010005e80303ca09887dcb62dfc89f321300c40a78732e44419d839b5795f87d9f415e57cf3520191f5690799d"
            + "9ec1a80d7268fd540498530a280a148e813ca5568360fd6a3b250022130213031301c02cc030c02bc02fcca9cca8c024c028"
            + "c023c027009f009e006b00670100057dff010001000000001500130000106465746563746f722e6578616d706c65000b0002"
            + "0100000a0012001011ec001d0017001e0018001901000101002300000016000000170000000d003600340905090609040403"
            + "0503060308070808081a081b081c0809080a080b080408050806040105010601030303010302040205020602002b00050403"
            + "040303002d00020101003304ea04e811ec04c0f1681ae730278c530351174f2e400d5f156f650588045a890b7319b1c165f0"
            + "5a814e9b72253cc0d0d21d8f494648cac723a7c1beb44b962bb6c7998c2698850754116f95018b557307aa09d2e0717b957f"
            + "1b120112f42f5cf84cfaa3c1712835c57436a1f7910bdc115fa46e3b5bc598e0b02a8109649713843c0672487c0bb4c23a45"
            + "4c89017da5266da6d89daff7a843a181ac567565941906817ed6c9c731e30799b9346031cca6c38390d376110612997749c9"
            + "f75d579c65d09165ce11c27b006d3c9572e92c69deb4286ab68fe1b984c464a98636119b291884211de278173fec812028c5"
            + "76640bf940b478102a6762173bb4b8a868c5e4405dffd250b9b204b6a2a1466aca24d041051541e96bbb3fb5192b4a4191f1"
            + "601183c3f2fbaa5a25c02b68925fd9452aa5cce395821530787a146cf253721388b0c57a63e8ea822b42714ea018c2ba1cb1"
            + "7510987b3dc51730e283a107a7bdedaa837ee13ec0a8c0fcc7316f2300a0b99e13a29f92315f5fbbbd680005db072725118b"
            + "b3fb1136448a2e442a516290f43ba55ce286c5c12948499b88e061f3f038d400a359e42aaccb353012b03a48483be6526c10"
            + "5aa5944c0e45a829f179c9b31fd4f528fc01b7941455e56ab577462b6f247ff03834f19592d1d94f50d459b9d4a1882b52a5"
            + "60c5268964d7f5c66039c839bc4ca5e34c918c5b7524af97ac11bed4b35c287a5e58b4614c69f1baa8a076ac7f60a41bf669"
            + "986633dae06f1a66c70ccc2919294e43675da841b8ea772510820e896a35c9751a29180a773a3d508780066b7ec150b5fafa"
            + "68f7599cb49448865ccef4d5c729c72bc906669ad39af99958703b65ed48b27b8c6655e8381b0b7297ca0f539b38845b28c3"
            + "735a5dca95cbe813f0d6a58d665fd28701a2879c37ba75b561548fd5a42ef52dd0ca92eb311ff6716b4da2556ea048231c0c"
            + "e886a4c284521f0743785c75a0a971a5c34091e48fe8c3b231611ecdd6190d4352a158c3666c55e0b06af9b9a369a6b57182"
            + "6be0d24399703eb726a7e46aa377007e3ab91470e76d4c56792188400c7a2a7bc6c26a66380984cb73f4714ef2b64b35566e"
            + "c249d3524d69086e73cc79692916ec0a23052b35e39577cd60367abcc575077d497388a58a07efb3160f290ff96185fdf90f"
            + "1cf01ee79ca1d7c75d3146be87d53f1dfb7bd47c6675046148873832721dda23388c686ed5693e912b42a6821fa4d3751270"
            + "57cd2c944fe95f25d7c4c3737c8ecbc12da04258b934a7a07ffd69a92c071c9c10a12fda3162f3a2fd746a554c6e47fa679e"
            + "c743ae035cb21b40a467aaa9a275fc6181cb69ad9798bbd7d1600fd12239a07f6d214e50b27c11030fe86a0b22fa5881f2a5"
            + "0c40968418ae5f191689bcbeb5786f5049b6a1b929bb011c398b3cf30869ecdca68fe5932c791ffe690623a34f3e9697ed3b"
            + "53ec157ee59a05f45c865201c138e062c615a38b025c1d05c9fdd946db938c8e2b2bb851376f38689e5608ba7c84a2bb86df"
            + "240a7b7093c9b1a1e8c089c7049cfde8bf1d8b1e478943bc414523ca05764c4f4ad80aa6821fb79779d780abd1a1c3f64337"
            + "fef626c6a14549b209cf476a85823431e741b2a58708f1c1f3422a5daa14ece118af04660fdd699e2def1c21f1c75668e559"
            + "efeca9918076ac1e0f66d44a8fa1671f93487e318be1605c90875f3e7a826ce1d7635f001d0020110281f2135e1c75b54834"
            + "3420f559323f61a331608b5c3c23e3046f325ed362";

    @Test
    public void rejectsGarbage() {
        byte[] raw = {0x00, 0x01, 0x02, 0x03};
        assertFalse(new IPPacket(raw, raw.length).isOk());
    }

    @Test
    public void rejectsTruncatedHeader() {
        byte[] raw = new byte[20];
        raw[0] = 0x45;
        assertTrue(new IPPacket(raw, raw.length).isOk());
        assertFalse(new IPPacket(raw, 10).isOk());
    }

    @Test
    public void parsesDnsQuery() {
        byte[] dns = dnsQuery(0x1234, "ya.ru", 1);
        IPPacket packet = packet(ipv4(PROTO_UDP, udp(51234, 53, dns)));

        assertTrue(packet.isOk());
        assertTrue(packet.isUdp());
        assertTrue(packet.isDnsQuery());
        assertFalse(packet.isDnsResponse());
        assertEquals("ya.ru", packet.getDnsDomain());
        assertEquals(DnsRecordType.A, packet.getDnsQueryType());
    }

    @Test
    public void parsesDnsResponseAddresses() {
        byte[] dns = dnsResponse(0x1234, "ya.ru", SERVER_IP);
        IPPacket packet = packet(ipv4(PROTO_UDP, udp(53, 51234, dns)));

        assertTrue(packet.isDnsResponse());
        assertEquals("ya.ru", packet.getDnsDomain());

        List<InetAddress> addresses = packet.getDnsAddresses();
        assertEquals(1, addresses.size());
        assertEquals("77.88.44.242", addresses.get(0).getHostAddress());
    }

    @Test
    public void ignoresDnsResponseWithoutAnswers() {
        byte[] dns = dnsQuery(0x1234, "ya.ru", 1);
        dns[2] = (byte) 0x81;
        dns[3] = (byte) 0x80;
        IPPacket packet = packet(ipv4(PROTO_UDP, udp(53, 51234, dns)));

        assertTrue(packet.isDnsResponse());
        assertTrue(packet.getDnsAddresses().isEmpty());
    }

    @Test
    public void parsesSni() {
        byte[] hello = clientHello("detector.example");
        IPPacket packet = packet(ipv4(PROTO_TCP, tcp(51234, 443, hello)));

        assertTrue(packet.isTcp());
        assertEquals("detector.example", packet.getSni());
    }

    @Test
    public void returnsNoSniForPlainPayload() {
        byte[] payload = "SSH-2.0-OpenSSH_9.6\r\n".getBytes();
        IPPacket packet = packet(ipv4(PROTO_TCP, tcp(51234, 22, payload)));

        assertTrue(packet.isTcp());
        assertNull(packet.getSni());
    }

    @Test
    public void buildsSynAckWithSwappedEndpoints() {
        IPPacket syn = packet(ipv4(PROTO_TCP, tcp(51234, 443, new byte[0])));
        byte[] raw = IPPacket.buildTcp(syn, 900000000L, 3001000001L,
                IPPacket.FLAG_SYN | IPPacket.FLAG_ACK, 32768, null, 0, 0);
        IPPacket reply = new IPPacket(raw, raw.length);

        assertTrue(reply.isOk());
        assertEquals(443, reply.getSourcePort());
        assertEquals(51234, reply.getDestinationPort());
        assertEquals(900000000L, reply.getSequence());
        assertEquals(3001000001L, reply.getAcknowledgment());
        assertTrue(reply.isSyn());
        assertTrue(reply.isAck());
        assertEquals("10.10.0.1", reply.getDestination().getHostAddress());
        assertEquals("77.88.44.242", reply.getSource().getHostAddress());
        assertTrue(checksumValid(raw, 0, 20));
    }

    @Test
    public void buildsTcpSegmentWithPayload() {
        IPPacket request = packet(ipv4(PROTO_TCP, tcp(51234, 443, new byte[0])));
        byte[] payload = "hello".getBytes();
        byte[] raw = IPPacket.buildTcp(request, 5L, 7L, IPPacket.FLAG_PSH | IPPacket.FLAG_ACK,
                32768, payload, 0, payload.length);
        IPPacket segment = new IPPacket(raw, raw.length);

        assertEquals(payload.length, segment.getPayloadLength());
        int offset = segment.getPayloadOffset();
        assertEquals('h', raw[offset]);
        assertEquals('o', raw[offset + 4]);
        assertTrue(checksumValid(raw, 0, 20));
    }

    @Test
    public void buildsUdpDatagram() {
        IPPacket request = packet(ipv4(PROTO_UDP, udp(51234, 443, new byte[]{1, 2, 3})));
        byte[] payload = {9, 8, 7, 6};
        byte[] raw = IPPacket.buildUdp(request, payload, 0, payload.length);
        IPPacket reply = new IPPacket(raw, raw.length);

        assertTrue(reply.isUdp());
        assertEquals(443, reply.getSourcePort());
        assertEquals(51234, reply.getDestinationPort());
        assertEquals(payload.length, reply.getPayloadLength());
        assertTrue(checksumValid(raw, 0, 20));
    }

    @Test
    public void buildsNullRouteAnswer() {
        byte[] dns = dnsQuery(0x1234, "ads.example.com", 1);
        IPPacket query = packet(ipv4(PROTO_UDP, udp(51234, 53, dns)));

        byte[] raw = query.buildDnsAnswer(DnsRecordType.A, new byte[]{127, 0, 0, 1}, 600);
        assertNotNull(raw);

        IPPacket answer = new IPPacket(raw, raw.length);
        assertTrue(answer.isDnsResponse());
        assertEquals("ads.example.com", answer.getDnsDomain());
        assertEquals(1, answer.getDnsAddresses().size());
        assertEquals("127.0.0.1", answer.getDnsAddresses().get(0).getHostAddress());
        assertEquals("10.10.0.1", answer.getDestination().getHostAddress());
        assertTrue(checksumValid(raw, 0, 20));
    }

    @Test
    public void parsesRealDnsQuery() {
        IPPacket packet = packet(ipv4(PROTO_UDP, udp(51234, 53, hex(REAL_DNS_QUERY_YA_RU))));

        assertTrue(packet.isDnsQuery());
        assertEquals("ya.ru", packet.getDnsDomain());
        assertEquals(DnsRecordType.A, packet.getDnsQueryType());
    }

    @Test
    public void parsesRealDnsResponse() {
        IPPacket packet = packet(ipv4(PROTO_UDP, udp(53, 51234, hex(REAL_DNS_RESPONSE_YA_RU))));

        assertTrue(packet.isDnsResponse());
        assertEquals("ya.ru", packet.getDnsDomain());

        List<InetAddress> addresses = packet.getDnsAddresses();
        assertEquals(3, addresses.size());
        assertEquals("77.88.44.242", addresses.get(0).getHostAddress());
        assertEquals("77.88.55.242", addresses.get(1).getHostAddress());
        assertEquals("5.255.255.242", addresses.get(2).getHostAddress());
    }

    @Test
    public void parsesRealDnsResponseBehindCname() {
        IPPacket packet = packet(ipv4(PROTO_UDP, udp(53, 51234, hex(REAL_DNS_RESPONSE_GITHUB))));

        assertTrue(packet.isDnsResponse());
        assertEquals("www.github.com", packet.getDnsDomain());

        List<InetAddress> addresses = packet.getDnsAddresses();
        assertEquals(1, addresses.size());
        assertEquals("4.237.22.38", addresses.get(0).getHostAddress());
    }

    @Test
    public void parsesRealClientHello() {
        IPPacket packet = packet(ipv4(PROTO_TCP, tcp(51234, 443, hex(REAL_CLIENT_HELLO))));

        assertTrue(packet.isTcp());
        assertEquals("detector.example", packet.getSni());
    }

    @Test
    public void parsesCapturedDnsQuery() {
        IPPacket packet = packet(hex(REAL_IP_DNS_QUERY));

        assertTrue(packet.isOk());
        assertTrue(packet.isDnsQuery());
        assertEquals("example.com", packet.getDnsDomain());
        assertEquals(DnsRecordType.A, packet.getDnsQueryType());
    }

    @Test
    public void parsesCapturedDnsResponse() {
        IPPacket packet = packet(hex(REAL_IP_DNS_RESPONSE));

        assertTrue(packet.isDnsResponse());
        assertEquals("example.com", packet.getDnsDomain());
        assertFalse(packet.getDnsAddresses().isEmpty());
    }

    @Test
    public void parsesCapturedSynWithOptions() {
        IPPacket packet = packet(hex(REAL_IP_TCP_SYN));

        assertTrue(packet.isTcp());
        assertTrue(packet.isSyn());
        assertFalse(packet.isAck());
        assertEquals(443, packet.getDestinationPort());
        assertEquals(0, packet.getPayloadLength());
        assertEquals(64, packet.getPayloadOffset());
    }

    @Test
    public void parsesCapturedSynAck() {
        IPPacket packet = packet(hex(REAL_IP_TCP_SYN_ACK));

        assertTrue(packet.isSyn());
        assertTrue(packet.isAck());
        assertEquals(443, packet.getSourcePort());
    }

    @Test
    public void parsesCapturedClientHello() {
        IPPacket packet = packet(hex(REAL_IP_CLIENT_HELLO));

        assertTrue(packet.isTcp());
        assertEquals(52, packet.getPayloadOffset());
        assertEquals("example.com", packet.getSni());
    }

    @Test
    public void buildsReplyFromCapturedSyn() {
        IPPacket syn = packet(hex(REAL_IP_TCP_SYN));
        byte[] raw = IPPacket.buildTcp(syn, 1000L, syn.getSequence() + 1,
                IPPacket.FLAG_SYN | IPPacket.FLAG_ACK, 32768, null, 0, 0);
        IPPacket reply = packet(raw);

        assertEquals(syn.getDestinationPort(), reply.getSourcePort());
        assertEquals(syn.getSourcePort(), reply.getDestinationPort());
        assertEquals(syn.getSource().getHostAddress(), reply.getDestination().getHostAddress());
        assertEquals(syn.getDestination().getHostAddress(), reply.getSource().getHostAddress());
        assertTrue(checksumValid(raw, 0, 20));
    }

    private static byte[] hex(String value) {
        byte[] out = new byte[value.length() / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) Integer.parseInt(value.substring(i * 2, i * 2 + 2), 16);
        }
        return out;
    }

    private static IPPacket packet(byte[] raw) {
        return new IPPacket(raw, raw.length);
    }

    private static boolean checksumValid(byte[] packet, int offset, int size) {
        int total = 0;
        for (int i = offset; i < offset + size; i += 2) {
            total += ((packet[i] & 0xFF) << 8) | (packet[i + 1] & 0xFF);
        }
        while ((total >> 16) != 0) {
            total = (total & 0xFFFF) + (total >> 16);
        }
        return total == 0xFFFF;
    }

    private static byte[] ipv4(int protocol, byte[] transport) {
        byte[] out = new byte[20 + transport.length];
        out[0] = 0x45;
        out[2] = (byte) (out.length >> 8);
        out[3] = (byte) out.length;
        out[6] = 0x40;
        out[8] = 64;
        out[9] = (byte) protocol;
        System.arraycopy(CLIENT_IP, 0, out, 12, 4);
        System.arraycopy(SERVER_IP, 0, out, 16, 4);
        System.arraycopy(transport, 0, out, 20, transport.length);
        return out;
    }

    private static byte[] udp(int sourcePort, int destinationPort, byte[] payload) {
        byte[] out = new byte[8 + payload.length];
        writeU16(out, 0, sourcePort);
        writeU16(out, 2, destinationPort);
        writeU16(out, 4, out.length);
        System.arraycopy(payload, 0, out, 8, payload.length);
        return out;
    }

    private static byte[] tcp(int sourcePort, int destinationPort, byte[] payload) {
        byte[] out = new byte[20 + payload.length];
        writeU16(out, 0, sourcePort);
        writeU16(out, 2, destinationPort);
        out[12] = (byte) (5 << 4);
        out[13] = (byte) IPPacket.FLAG_ACK;
        writeU16(out, 14, 65535);
        System.arraycopy(payload, 0, out, 20, payload.length);
        return out;
    }

    private static byte[] dnsQuery(int id, String domain, int type) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeU16(out, id);
        writeU16(out, 0x0100);
        writeU16(out, 1);
        writeU16(out, 0);
        writeU16(out, 0);
        writeU16(out, 0);
        writeName(out, domain);
        writeU16(out, type);
        writeU16(out, 1);
        return out.toByteArray();
    }

    private static byte[] dnsResponse(int id, String domain, byte[] address) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeU16(out, id);
        writeU16(out, 0x8180);
        writeU16(out, 1);
        writeU16(out, 1);
        writeU16(out, 0);
        writeU16(out, 0);
        writeName(out, domain);
        writeU16(out, 1);
        writeU16(out, 1);

        out.write(0xC0);
        out.write(12);
        writeU16(out, 1);
        writeU16(out, 1);
        writeU16(out, 0);
        writeU16(out, 300);
        writeU16(out, address.length);
        out.write(address, 0, address.length);
        return out.toByteArray();
    }

    private static byte[] clientHello(String serverName) {
        byte[] name = serverName.getBytes();

        ByteArrayOutputStream extension = new ByteArrayOutputStream();
        writeU16(extension, name.length + 3);
        extension.write(0);
        writeU16(extension, name.length);
        extension.write(name, 0, name.length);
        byte[] serverNameExtension = extension.toByteArray();

        ByteArrayOutputStream extensions = new ByteArrayOutputStream();
        writeU16(extensions, 0x0000);
        writeU16(extensions, serverNameExtension.length);
        extensions.write(serverNameExtension, 0, serverNameExtension.length);
        byte[] allExtensions = extensions.toByteArray();

        ByteArrayOutputStream body = new ByteArrayOutputStream();
        writeU16(body, 0x0303);
        body.write(new byte[32], 0, 32);
        body.write(0);
        writeU16(body, 2);
        writeU16(body, 0x1301);
        body.write(1);
        body.write(0);
        writeU16(body, allExtensions.length);
        body.write(allExtensions, 0, allExtensions.length);
        byte[] helloBody = body.toByteArray();

        ByteArrayOutputStream record = new ByteArrayOutputStream();
        record.write(0x16);
        writeU16(record, 0x0301);
        writeU16(record, helloBody.length + 4);
        record.write(0x01);
        record.write(0);
        writeU16(record, helloBody.length);
        record.write(helloBody, 0, helloBody.length);
        return record.toByteArray();
    }

    private static void writeName(ByteArrayOutputStream out, String domain) {
        for (String label : domain.split("\\.")) {
            out.write(label.length());
            byte[] raw = label.getBytes();
            out.write(raw, 0, raw.length);
        }
        out.write(0);
    }

    private static void writeU16(ByteArrayOutputStream out, int value) {
        out.write((value >> 8) & 0xFF);
        out.write(value & 0xFF);
    }

    private static void writeU16(byte[] out, int index, int value) {
        out[index] = (byte) (value >> 8);
        out[index + 1] = (byte) value;
    }
}
