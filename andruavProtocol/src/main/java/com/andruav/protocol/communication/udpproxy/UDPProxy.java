package com.andruav.protocol.communication.udpproxy;

import static com.andruav.protocol.communication.websocket.AndruavWSClientBase.SOCKETSTATE_REGISTERED;

import com.andruav.AndruavEngine;
import com.andruav.AndruavFacade;
import com.andruav.AndruavSettings;
import com.andruav.event.fcb_event.Event_SocketData;
import com.andruav.protocol.communication.udpserver.UDPServerBase;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class UDPProxy extends UDPServerBase {


    /*** The relay's per-pair unit token is always 16 raw bytes (see de_mavlink MV-0017). */
    static final int TOKEN_BYTES = 16;

    /*** While a token is set the hello datagram is also the idle keepalive. */
    private static final int UDP_HELLO_MS = 5000;

    private static final byte[] HELLO_MAGIC = {'D', 'E', 'U', '1'};

    private byte[] mToken;
    private ScheduledExecutorService mHelloScheduler;

    public UDPProxy ()
    {
        super(0);
    }

    @Override
    public void broadCast() throws IllegalAccessException {

    }

    public void sendMessage(final byte[] msg, final int length) {
        if (!AndruavSettings.andruavWe7daBase.isUdpProxyEnabled()) return ;
        {
            InetAddress destAddress = AndruavSettings.andruavWe7daBase.getUdp_iaddress_socket_unit();
            if (destAddress==null) return ;

            send (destAddress, AndruavSettings.andruavWe7daBase.getUdp_socket_port_unit(), msg, length);
        }
    }


    /***
     * Applies the relay-issued unit token carried by an inbound 9008 (base64url).
     * <br>{@code null}/empty clears it - i.e. a built-in proxy or old relay restores
     * today's behaviour. A reply with a new token replaces it.
     */
    public synchronized void setToken(final String tokenBase64Url)
    {
        if ((tokenBase64Url == null) || tokenBase64Url.isEmpty())
        {
            mToken = null;
            stopHelloLoop();
            return;
        }

        final byte[] token = decodeBase64Url(tokenBase64Url);
        if (token.length != TOKEN_BYTES)
        {
            mToken = null;
            stopHelloLoop();
            AndruavEngine.log().log(AndruavSettings.Account_SID, "udp_proxy",
                    "invalid relay token (" + token.length + " bytes)");
            return;
        }

        mToken = token;
        // never log the token in full
        AndruavEngine.log().log(AndruavSettings.Account_SID, "udp_proxy",
                "relay token " + tokenBase64Url.substring(0, Math.min(4, tokenBase64Url.length())) + "...");
        startHelloLoop();
    }


    /***
     * The relay drops data datagrams until a hello ("DEU1"+token) pins socket1,
     * so the hello runs right away and then every {@link #UDP_HELLO_MS} ms.
     */
    private void sendHello()
    {
        final byte[] token = mToken;
        if (token == null) return ;
        if (!AndruavSettings.andruavWe7daBase.isUdpProxyEnabled()) return ;

        final InetAddress destAddress = AndruavSettings.andruavWe7daBase.getUdp_iaddress_socket_unit();
        if (destAddress == null) return ;

        final byte[] hello = buildHello(token);
        send(destAddress, AndruavSettings.andruavWe7daBase.getUdp_socket_port_unit(), hello, hello.length);
    }

    private synchronized void startHelloLoop()
    {
        if ((mHelloScheduler != null) && !mHelloScheduler.isShutdown()) return ;

        mHelloScheduler = Executors.newSingleThreadScheduledExecutor();
        mHelloScheduler.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                sendHello();
            }
        }, 0, UDP_HELLO_MS, TimeUnit.MILLISECONDS);
    }

    private synchronized void stopHelloLoop()
    {
        if (mHelloScheduler != null)
        {
            mHelloScheduler.shutdownNow();
            mHelloScheduler = null;
        }
    }

    @Override
    protected void shutDown()
    {
        stopHelloLoop();
        super.shutDown();
    }


    /***
     * Decodes a base64url token. Returns an empty array on bad input.
     * <br>Pure Java so it is verifiable in a JVM unit test (android.util.Base64 is not).
     */
    static byte[] decodeBase64Url(final String b64url)
    {
        int v = 0, bits = -8;
        byte[] out = new byte[(b64url.length() * 6) / 8 + 1];
        int count = 0;
        for (int i = 0; i < b64url.length(); i++)
        {
            final char c = b64url.charAt(i);
            int d;
            if ((c >= 'A') && (c <= 'Z')) d = c - 'A';
            else if ((c >= 'a') && (c <= 'z')) d = c - 'a' + 26;
            else if ((c >= '0') && (c <= '9')) d = c - '0' + 52;
            else if (c == '-') d = 62;
            else if (c == '_') d = 63;
            else if (c == '=') break;
            else return new byte[0];
            v = (v << 6) | d;
            bits += 6;
            if (bits >= 0)
            {
                out[count++] = (byte) ((v >> bits) & 0xFF);
                bits -= 8;
            }
        }
        final byte[] result = new byte[count];
        System.arraycopy(out, 0, result, 0, count);
        return result;
    }


    /*** Builds the hello datagram: 'D','E','U','1' + the raw token. */
    static byte[] buildHello(final byte[] token)
    {
        final byte[] hello = new byte[HELLO_MAGIC.length + token.length];
        System.arraycopy(HELLO_MAGIC, 0, hello, 0, HELLO_MAGIC.length);
        System.arraycopy(token, 0, hello, HELLO_MAGIC.length, token.length);
        return hello;
    }


    @Override
    protected void onSendResult(final long durationUs) {
        if (AndruavEngine.getPreference().recordUdpSendFeedback(durationUs)) {
            // AUTO changed the effective level: push it to GCS so it reflects it without waiting for a poll.
            AndruavFacade.sendUdpProxyStatus(null);
        }
    }

    @Override
    protected void onData(final DatagramPacket packet, final byte[] buffer, final int len) {
        final int status = AndruavEngine.getAndruavWSStatus();
        if (status != SOCKETSTATE_REGISTERED) return ;

        Event_SocketData event_socketData = new Event_SocketData();
        event_socketData.IsLocal = Event_SocketData.SOURCE_REMOTE;
        event_socketData.Data = buffer;
        event_socketData.DataLength = len;
        AndruavSettings.andruavWe7daBase.updateUdpProxyLastReceiveTime();
        AndruavEngine.getEventBus().post(event_socketData);
    }
}
