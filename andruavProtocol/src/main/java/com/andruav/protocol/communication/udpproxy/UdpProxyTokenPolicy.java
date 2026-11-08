package com.andruav.protocol.communication.udpproxy;

/***
 * Decides whether an inbound 9008 UDP-proxy config applies to THIS unit, so its
 * relay token (tk) may only ever be set or cleared for our own pair - a 9008
 * about another unit must not touch it (AN-0027).
 */
public final class UdpProxyTokenPolicy {

    private UdpProxyTokenPolicy() {
    }

    /*** Same own-unit test {@code AndruavWSClientBase} uses for the 9008 itself. */
    public static boolean isForThisUnit(final String partyID, final String nezam) {
        return (partyID != null) && partyID.contains(nezam.toUpperCase());
    }
}
