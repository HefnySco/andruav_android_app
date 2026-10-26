package de.databus;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

/**
 * Java port of de_databus/python/udpClient.py — the UDP chunk protocol:
 * datagram = 2-byte little-endian chunk number + payload; chunk 0 starts
 * a message; 0xFFFF marks the last chunk; reassemble in order; the
 * sender-ID thread re-sends the module id JSON once a second.
 */
public class CUDPClient {

    public static final int LAST_CHUNK_NUMBER = 0xFFFF;

    /** Receives a reassembled message (null-terminated JSON header + payload). */
    public interface OnReceiveCallback {
        void onReceive(byte[] message, int length);
    }

    private DatagramSocket m_SocketFD;
    private InetAddress m_targetIP;
    private int m_broadcastPort;
    private int m_chunkSize;
    private volatile boolean m_stopped_called;
    private volatile boolean m_starrted;
    private Thread m_threadCreateUDPSocket;
    private Thread m_threadSenderID;
    private OnReceiveCallback m_callback;
    private String m_JsonID = "";
    private final Object m_lock = new Object();
    private final Object m_lock2 = new Object();
    private static final int MAXLINE = 65507;

    public void init(String targetIP, int broadcastPort, String host, int listeningPort,
                     int chunkSize, OnReceiveCallback onReceiveCallback) throws IOException {
        this.m_chunkSize = chunkSize;
        this.m_callback = onReceiveCallback;
        this.m_SocketFD = new DatagramSocket(null);
        this.m_SocketFD.setReuseAddress(true);
        this.m_SocketFD.bind(new java.net.InetSocketAddress(host, listeningPort));
        this.m_targetIP = InetAddress.getByName(targetIP);
        this.m_broadcastPort = broadcastPort;
        System.out.println("UDP Listener at " + host + ":" + listeningPort);
        System.out.println("Expected Comm Server at " + targetIP + ":" + broadcastPort);
        System.out.println("UDP Max Packet Size " + chunkSize);
    }

    public void start() {
        if (m_starrted) {
            throw new IllegalStateException("Starrted called twice");
        }
        startReceiver();
        startSenderID();
        m_starrted = true;
    }

    private void startReceiver() {
        m_threadCreateUDPSocket = new Thread(this::InternalReceiverEntry, "de_databus-rx");
        m_threadCreateUDPSocket.setDaemon(true);
        m_threadCreateUDPSocket.start();
    }

    private void startSenderID() {
        m_threadSenderID = new Thread(this::InternelSenderIDEntry, "de_databus-id");
        m_threadSenderID.setDaemon(true);
        m_threadSenderID.start();
    }

    public void stop() {
        m_stopped_called = true;
        if (m_SocketFD != null) {
            m_SocketFD.close();
        }
        if (m_starrted) {
            try {
                m_threadCreateUDPSocket.join(2000);
                m_threadSenderID.join(2000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void InternalReceiverEntry() {
        List<byte[]> receivedChunks = new ArrayList<>();
        byte[] buf = new byte[MAXLINE];

        while (!m_stopped_called) {
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            try {
                m_SocketFD.receive(packet);
            } catch (SocketException e) {
                if (m_stopped_called) break;
                continue;
            } catch (IOException e) {
                if (m_stopped_called) break;
                continue;
            }

            int len = packet.getLength();
            if (len > 0) {
                if (len < 2) {
                    System.out.println("ERROR: Received packet too small: " + len + " bytes");
                    continue;
                }

                byte[] data = packet.getData();
                int off = packet.getOffset();
                int chunkNumber = ((data[off + 1] & 0xFF) << 8) | (data[off] & 0xFF);

                if (chunkNumber == 0) {
                    receivedChunks = new ArrayList<>();
                }

                byte[] payload = new byte[len - 2];
                System.arraycopy(data, off + 2, payload, 0, payload.length);
                receivedChunks.add(payload);

                if (chunkNumber == LAST_CHUNK_NUMBER) {
                    int total = 0;
                    for (byte[] c : receivedChunks) total += c.length;
                    // NOTICE: we don't know if this is a text or a text+binary
                    // message so a null terminator is appended; it should be
                    // stripped later if binary.
                    byte[] concatenatedData = new byte[total + 1];
                    int pos = 0;
                    for (byte[] c : receivedChunks) {
                        System.arraycopy(c, 0, concatenatedData, pos, c.length);
                        pos += c.length;
                    }

                    if (m_callback != null) {
                        try {
                            m_callback.onReceive(concatenatedData, concatenatedData.length);
                        } catch (Exception e) {
                            System.out.println("ERROR: onReceive callback failed: " + e);
                        }
                    }

                    receivedChunks = new ArrayList<>();
                }
            }
        }
    }

    public void setJsonId(String jsonID) {
        this.m_JsonID = jsonID;
    }

    private void InternelSenderIDEntry() {
        while (!m_stopped_called) {
            synchronized (m_lock2) {
                if (!m_JsonID.isEmpty()) {
                    byte[] msg = m_JsonID.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                    sendMSG(msg, msg.length);
                }
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /** Splits the message into chunks and sends each datagram. */
    public void sendMSG(byte[] msg, int length) {
        synchronized (m_lock) {
            try {
                int remaining_length = length;
                int offset = 0;
                int chunk_number = 0;

                while (remaining_length > 0) {
                    int chunk_length = Math.min(m_chunkSize, remaining_length);
                    remaining_length -= chunk_length;

                    byte[] chunk_msg = new byte[chunk_length + 2];

                    if (remaining_length == 0) {
                        // Last packet is always 0xFFFF regardless of its number.
                        chunk_msg[0] = (byte) 0xFF;
                        chunk_msg[1] = (byte) 0xFF;
                    } else {
                        chunk_msg[0] = (byte) (chunk_number & 0xFF);
                        chunk_msg[1] = (byte) ((chunk_number >> 8) & 0xFF);
                    }

                    System.arraycopy(msg, offset, chunk_msg, 2, chunk_length);

                    m_SocketFD.send(new DatagramPacket(chunk_msg, chunk_msg.length,
                            m_targetIP, m_broadcastPort));

                    if (remaining_length != 0) {
                        // Fast sending causes packet loss.
                        Thread.sleep(10);
                    }

                    offset += chunk_length;
                    chunk_number += 1;
                }
            } catch (Exception e) {
                System.out.println("DEBUG: InternelSenderIDEntry EXIT\n" + e);
            }
        }
    }
}
