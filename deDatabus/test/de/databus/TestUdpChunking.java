package de.databus;

import static org.junit.jupiter.api.Assertions.*;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Golden chunk test: a 20000-byte message with chunk size 8192 must
 * produce 3 datagrams numbered 0, 1, 0xFFFF on the wire and reassemble
 * to the same bytes. Uses a real loopback DatagramSocket pair.
 */
class TestUdpChunking {

    @Test
    void twenty_k_splits_into_three_chunks() throws Exception {
        byte[] message = new byte[20000];
        for (int i = 0; i < message.length; i++) message[i] = (byte) (i & 0xFF);

        try (DatagramSocket receiver = new DatagramSocket(new InetSocketAddress("127.0.0.1", 0))) {
            receiver.setSoTimeout(5000);
            int recvPort = receiver.getLocalPort();

            CUDPClient sender = new CUDPClient();
            sender.init("127.0.0.1", recvPort, "127.0.0.1", 0, 8192, null);
            // sendMSG directly - no start() so no receiver/sender-id threads
            sender.sendMSG(message, message.length);

            List<Integer> chunkNumbers = new ArrayList<>();
            List<byte[]> payloads = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                byte[] buf = new byte[65507];
                DatagramPacket p = new DatagramPacket(buf, buf.length);
                receiver.receive(p);
                int cn = ((p.getData()[1] & 0xFF) << 8) | (p.getData()[0] & 0xFF);
                chunkNumbers.add(cn);
                byte[] payload = new byte[p.getLength() - 2];
                System.arraycopy(p.getData(), 2, payload, 0, payload.length);
                payloads.add(payload);
            }

            assertEquals(List.of(0, 1, 0xFFFF), chunkNumbers);

            byte[] reassembled = new byte[20000];
            int pos = 0;
            for (byte[] c : payloads) {
                System.arraycopy(c, 0, reassembled, pos, c.length);
                pos += c.length;
            }
            assertArrayEquals(message, reassembled);

            sender.stop();
        }
    }
}
