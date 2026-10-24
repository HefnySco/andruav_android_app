package com.andruav.protocol.communication.udpproxy;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class UdpProxyTokenTest {

    /***
     * base64url token from the 9008 reply must decode to the 16 raw bytes.
     */
    @Test
    public void base64UrlDecodesTo16Bytes() {
        // {0,1,...,15}
        final byte[] token = UDPProxy.decodeBase64Url("AAECAwQFBgcICQoLDA0ODw");
        assertEquals(UDPProxy.TOKEN_BYTES, token.length);
        for (int i = 0; i < token.length; i++) {
            assertEquals((byte) i, token[i]);
        }
    }

    /***
     * base64url uses '-' and '_' instead of '+' and '/'; they must decode.
     */
    @Test
    public void base64UrlAlphabet() {
        final byte[] token = UDPProxy.decodeBase64Url("____");
        assertArrayEquals(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF}, token);

        final byte[] token2 = UDPProxy.decodeBase64Url("-A");
        assertArrayEquals(new byte[]{(byte) 0xF8}, token2);
    }

    @Test
    public void base64UrlRejectsBadInput() {
        assertEquals(0, UDPProxy.decodeBase64Url("abc!def").length);
        assertEquals(0, UDPProxy.decodeBase64Url("").length);
    }

    /***
     * The hello that pins socket1 on the relay is exactly "DEU1" + 16 token bytes.
     */
    @Test
    public void helloDatagramIsDEU1PlusToken() {
        final byte[] token = UDPProxy.decodeBase64Url("AAECAwQFBgcICQoLDA0ODw");
        final byte[] hello = UDPProxy.buildHello(token);

        assertEquals(4 + UDPProxy.TOKEN_BYTES, hello.length);
        assertEquals('D', hello[0]);
        assertEquals('E', hello[1]);
        assertEquals('U', hello[2]);
        assertEquals('1', hello[3]);
        for (int i = 0; i < token.length; i++) {
            assertEquals(token[i], hello[4 + i]);
        }
    }
}
