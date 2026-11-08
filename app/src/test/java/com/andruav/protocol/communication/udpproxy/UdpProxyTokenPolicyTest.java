package com.andruav.protocol.communication.udpproxy;

import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class UdpProxyTokenPolicyTest {

    private static final String NEZAM = "_SYS_";
    private static final String OWN_PARTY = "wss_" + NEZAM + "andruav_unit_x";
    private static final String OTHER_PARTY = "wss_gcs_someone";
    private static final String TOKEN = "AAECAwQFBgcICQoLDA0ODw";

    private static byte[] tokenOf(final UDPProxy proxy) throws Exception {
        final Field field = UDPProxy.class.getDeclaredField("mToken");
        field.setAccessible(true);
        return (byte[]) field.get(proxy);
    }

    /*** A 9008 addressed to this unit carries "_SYS_" in its partyID. */
    @Test
    public void ownUnitIsDetected() {
        assertTrue(UdpProxyTokenPolicy.isForThisUnit(OWN_PARTY, NEZAM));
        assertFalse(UdpProxyTokenPolicy.isForThisUnit(OTHER_PARTY, NEZAM));
        assertFalse(UdpProxyTokenPolicy.isForThisUnit(null, NEZAM));
    }

    /***
     * Own-unit 9008 with tk -> setToken stores the 16-byte token;
     * a 9008 for another party must never reach setToken, so the token stays.
     */
    @Test
    public void tokenAppliesOnlyForThisUnit() throws Exception {
        final UDPProxy proxy = new UDPProxy();
        try {
            if (UdpProxyTokenPolicy.isForThisUnit(OWN_PARTY, NEZAM)) {
                proxy.setToken(TOKEN);
            }
            assertNotNull(tokenOf(proxy));
            assertEquals(UDPProxy.TOKEN_BYTES, tokenOf(proxy).length);

            // 9008 for another party: policy refuses, setToken is not called.
            if (UdpProxyTokenPolicy.isForThisUnit(OTHER_PARTY, NEZAM)) {
                proxy.setToken(null);
            }
            assertNotNull(tokenOf(proxy));
            assertEquals(UDPProxy.TOKEN_BYTES, tokenOf(proxy).length);
        } finally {
            proxy.setToken(null);
        }
    }

    /*** Own-unit 9008 with en:false -> setToken(null) clears the stored token. */
    @Test
    public void disabledProxyClearsToken() throws Exception {
        final UDPProxy proxy = new UDPProxy();
        proxy.setToken(TOKEN);
        assertNotNull(tokenOf(proxy));

        // en:false -> the dispatcher calls setToken(null)
        proxy.setToken(null);
        assertNull(tokenOf(proxy));
    }

    /*** A malformed token is refused and clears state rather than half-applying. */
    @Test
    public void invalidTokenIsRefused() throws Exception {
        final UDPProxy proxy = new UDPProxy();
        proxy.setToken("AAEC"); // decodes to 3 bytes - not 16
        assertNull(tokenOf(proxy));
    }
}
