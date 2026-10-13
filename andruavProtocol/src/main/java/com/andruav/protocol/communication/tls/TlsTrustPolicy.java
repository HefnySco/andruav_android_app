package com.andruav.protocol.communication.tls;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.andruav.AndruavEngine;

import java.net.InetAddress;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.X509TrustManager;

/***
 * Single point of truth for TLS server-certificate verification, shared by
 * LoginClient (OkHttp) and the WSS client (AndruavWSClientBase_TooTallNate).
 *
 * Governed by the "Require SSL" preference (default ON):
 *  - OFF: trust-all (previous behaviour), connections report non secure.
 *  - ON + publicly-certifiable endpoint (public domain / public IP): strict
 *    platform verification. An untrusted certificate is recorded as
 *    pending-untrusted so the UI can run a TOFU prompt; accepting pins
 *    host -> SHA-256 fingerprint. A changed certificate on a pinned host is
 *    treated as a fresh untrusted cert (re-prompt).
 *  - ON + private endpoint (private/loopback/link-local IP, .local/.lan/
 *    .home.arpa-style name, single-label name, or a domain resolving to a
 *    private IP): self-signed allowed; the cert is pinned on first sight and
 *    must not change afterwards.
 */
public class TlsTrustPolicy {

    /** SharedPreferences key for the "Require SSL" checkbox (pref_drone_root.xml). */
    public static final String PREF_REQUIRE_SSL = "req_ssl";

    /** Pin store key prefix inside the default SharedPreferences. */
    private static final String PREF_PIN_PREFIX = "tls_pin_";

    ////// Verification state surfaced to the UI (secure / non secure suffix)
    public static final int TLS_STATE_UNKNOWN        = 0;
    public static final int TLS_STATE_SECURE         = 1;   // strict platform verification passed
    public static final int TLS_STATE_SECURE_PINNED  = 2;   // matched a stored cert pin
    public static final int TLS_STATE_INSECURE       = 3;   // Require SSL OFF - nothing verified

    private static volatile int mLastState = TLS_STATE_UNKNOWN;

    /***
     * Fingerprints this policy explicitly accepted during a real handshake
     * (pinned or private-endpoint certs). OkHttp's certificate-chain cleaner
     * re-invokes the two-arg checkServerTrusted() without socket/host context
     * after the handshake - consulting this set keeps that second check from
     * contradicting the decision the socket-aware call already made.
     */
    private static final Set<String> mAcceptedFingerprints = new HashSet<String>();

    /** Certificates the user refused this session (host|fingerprint) - no re-prompt. */
    private static final Set<String> mDeclined = new HashSet<String>();

    private static PendingUntrusted mPendingUntrusted;

    /***
     * A certificate the strict path could not verify on a public endpoint.
     * Held for the UI's trust-on-first-use prompt.
     */
    public static class PendingUntrusted {
        public final String host;
        public final String fingerprint;
        public final String subject;
        /** True when the host was previously pinned and the cert changed - MITM signal. */
        public final boolean certChanged;

        PendingUntrusted(final String host, final String fingerprint, final String subject, final boolean certChanged) {
            this.host = host;
            this.fingerprint = fingerprint;
            this.subject = subject;
            this.certChanged = certChanged;
        }
    }


    private static Context getAppContext() {
        try {
            if (AndruavEngine.getPreference() == null) return null;
            return AndruavEngine.getPreference().getContext();
        } catch (Exception e) {
            return null;
        }
    }

    private static SharedPreferences getPrefs() {
        final Context context = getAppContext();
        if (context == null) return null;
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static boolean isRequireSSL() {
        final SharedPreferences prefs = getPrefs();
        if (prefs == null) return true; // fail-safe: verify when prefs unavailable
        return prefs.getBoolean(PREF_REQUIRE_SSL, true);
    }

    ////// Pin store

    public static String getPinnedFingerprint(final String host) {
        final SharedPreferences prefs = getPrefs();
        if ((prefs == null) || (host == null)) return null;
        return prefs.getString(PREF_PIN_PREFIX + host.toLowerCase(Locale.US), null);
    }

    public static boolean isPinned(final String host) {
        return getPinnedFingerprint(host) != null;
    }

    public static void pinCertificate(final String host, final String fingerprint) {
        final SharedPreferences prefs = getPrefs();
        if ((prefs == null) || (host == null) || (fingerprint == null)) return;
        prefs.edit().putString(PREF_PIN_PREFIX + host.toLowerCase(Locale.US), fingerprint).apply();
    }

    ////// Pending untrusted certificate (TOFU prompt)

    public static synchronized boolean hasPendingUntrusted() {
        return mPendingUntrusted != null;
    }

    public static synchronized PendingUntrusted getPendingUntrusted() {
        return mPendingUntrusted;
    }

    /** User accepted the pending certificate: pin host -> fingerprint. */
    public static synchronized void trustPending() {
        if (mPendingUntrusted == null) return;
        pinCertificate(mPendingUntrusted.host, mPendingUntrusted.fingerprint);
        synchronized (mAcceptedFingerprints) {
            mAcceptedFingerprints.add(mPendingUntrusted.fingerprint);
        }
        mPendingUntrusted = null;
    }

    /** User rejected the pending certificate: remember so we do not re-prompt this session. */
    public static synchronized void declinePending() {
        if (mPendingUntrusted == null) return;
        synchronized (mDeclined) {
            mDeclined.add(mPendingUntrusted.host + "|" + mPendingUntrusted.fingerprint);
        }
        mPendingUntrusted = null;
    }

    private static synchronized void recordPending(final PendingUntrusted pending) {
        synchronized (mDeclined) {
            if (mDeclined.contains(pending.host + "|" + pending.fingerprint)) {
                // user already refused this exact cert this session - stay quiet
                mPendingUntrusted = null;
                return;
            }
        }
        mPendingUntrusted = pending;
    }


    ////// Verification state for the UI

    public static int getLastState() {
        return mLastState;
    }

    /** True when the last connection was verified (strict or pinned). */
    public static boolean isLastConnectionSecure() {
        return (mLastState == TLS_STATE_SECURE) || (mLastState == TLS_STATE_SECURE_PINNED);
    }


    ////// Endpoint classification

    /***
     * True when the endpoint can never hold a public CA certificate:
     * private/loopback/link-local/reserved IP literal, single-label or
     * .local/.lan/.home.arpa-style name, or a domain resolving to private IPs.
     * An unresolvable or public-looking name is treated as public (fail-safe).
     */
    public static boolean isPrivateEndpoint(final String host) {
        if ((host == null) || host.isEmpty()) return false;

        String normalized = host.toLowerCase(Locale.US).trim();
        if (normalized.endsWith(".")) normalized = normalized.substring(0, normalized.length() - 1);
        if (normalized.startsWith("[") && normalized.endsWith("]")) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }

        if (isIpLiteral(normalized)) {
            try {
                return isPrivateAddress(InetAddress.getByName(normalized));
            } catch (Exception e) {
                return false;
            }
        }

        // single-label names (raspberrypi, droneengage, localhost) and mDNS /
        // LAN-only suffixes cannot hold a public CA certificate.
        if (!normalized.contains(".")) return true;
        if (normalized.endsWith(".local") || normalized.endsWith(".lan")
                || normalized.endsWith(".home.arpa") || normalized.endsWith(".internal")
                || normalized.endsWith(".intranet") || normalized.endsWith(".corp")
                || normalized.endsWith(".private") || normalized.endsWith(".localdomain")) {
            return true;
        }

        try {
            final InetAddress[] addresses = InetAddress.getAllByName(normalized);
            if (addresses.length == 0) return false;
            for (InetAddress address : addresses) {
                if (!isPrivateAddress(address)) return false;
            }
            return true; // every resolved address is private
        } catch (Exception e) {
            return false; // cannot resolve -> public (fail-safe: strict)
        }
    }

    private static boolean isIpLiteral(final String host) {
        if (host.indexOf(':') >= 0) return true; // IPv6
        for (int i = 0; i < host.length(); ++i) {
            final char c = host.charAt(i);
            if ((c != '.') && (c < '0' || c > '9')) return false;
        }
        return host.length() > 0;
    }

    private static boolean isPrivateAddress(final InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress()
                || address.isLinkLocalAddress() || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return true;
        }
        final byte[] bytes = address.getAddress();
        if (bytes.length == 4) {
            final int first = bytes[0] & 0xff;
            final int second = bytes[1] & 0xff;
            if (first == 100 && second >= 64 && second <= 127) return true; // CGNAT 100.64.0.0/10
            if (first == 198 && (second == 18 || second == 19)) return true; // benchmarking 198.18.0.0/15
        }
        return false;
    }


    /***
     * Whether the raw socket (Java-WebSocket) should run TLS endpoint
     * identification (hostname verification). Skipped for private endpoints
     * and pinned hosts - their self-signed/pinned certs rarely carry a
     * matching CN, and the pin already binds identity.
     */
    public static boolean requiresHostnameVerification(final String host) {
        if (!isRequireSSL()) return false;
        if (isPrivateEndpoint(host)) return false;
        return !isPinned(host);
    }

    /***
     * HostnameVerifier for OkHttp: accept-all when Require SSL is OFF; strict
     * default for public unpinned hosts; relaxed for private/pinned endpoints
     * (the pin check in the trust manager already bound identity).
     */
    public static HostnameVerifier hostnameVerifier() {
        final HostnameVerifier defaultVerifier = HttpsURLConnection.getDefaultHostnameVerifier();
        return new HostnameVerifier() {
            @Override
            public boolean verify(final String hostname, final SSLSession session) {
                if (!isRequireSSL()) return true;
                if (isPrivateEndpoint(hostname)) return true;
                if (isPinned(hostname)) return true;
                return defaultVerifier.verify(hostname, session);
            }
        };
    }


    /***
     * A TrustManager implementing the policy. Must be handed to the same
     * SSLContext that produces the socket factory (see createSocketFactory).
     * The "Require SSL" preference is read live on every verification, so a
     * singleton OkHttpClient built once still honours a later toggle.
     */
    public static X509TrustManager newTrustManager() {

        final X509ExtendedTrustManager defaultTrustManager = platformTrustManager();
        return new X509ExtendedTrustManager() {

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return defaultTrustManager.getAcceptedIssuers();
            }

            @Override
            public void checkClientTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
                defaultTrustManager.checkClientTrusted(chain, authType);
            }

            @Override
            public void checkClientTrusted(final X509Certificate[] chain, final String authType, final Socket socket) throws CertificateException {
                defaultTrustManager.checkClientTrusted(chain, authType, socket);
            }

            @Override
            public void checkClientTrusted(final X509Certificate[] chain, final String authType, final SSLEngine engine) throws CertificateException {
                defaultTrustManager.checkClientTrusted(chain, authType, engine);
            }

            @Override
            public void checkServerTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
                // No socket/host context (e.g. OkHttp's certificate-chain cleaner
                // re-checking after the handshake). Trust the platform result; if it
                // fails, honour a decision the socket-aware call already made.
                if (!isRequireSSL()) {
                    mLastState = TLS_STATE_INSECURE;
                    return;
                }
                try {
                    defaultTrustManager.checkServerTrusted(chain, authType);
                    return;
                } catch (CertificateException e) {
                    final String fingerprint = fingerprint(chain);
                    synchronized (mAcceptedFingerprints) {
                        if ((fingerprint != null) && mAcceptedFingerprints.contains(fingerprint)) {
                            return;
                        }
                    }
                    throw e;
                }
            }

            @Override
            public void checkServerTrusted(final X509Certificate[] chain, final String authType, final Socket socket) throws CertificateException {
                evaluate(resolveHost(socket), chain, authType, socket, null);
            }

            @Override
            public void checkServerTrusted(final X509Certificate[] chain, final String authType, final SSLEngine engine) throws CertificateException {
                evaluate(engine.getPeerHost(), chain, authType, null, engine);
            }

            private String resolveHost(final Socket socket) {
                if (socket == null) return null;
                final InetAddress address = socket.getInetAddress();
                if (address == null) return null;
                return address.getHostName();
            }

            private void evaluate(final String host, final X509Certificate[] chain, final String authType, final Socket socket, final SSLEngine engine) throws CertificateException {
                if (!isRequireSSL()) {
                    mLastState = TLS_STATE_INSECURE;
                    return;
                }
                try {
                    if (socket != null) {
                        defaultTrustManager.checkServerTrusted(chain, authType, socket);
                    } else if (engine != null) {
                        defaultTrustManager.checkServerTrusted(chain, authType, engine);
                    } else {
                        defaultTrustManager.checkServerTrusted(chain, authType);
                    }
                    mLastState = TLS_STATE_SECURE;
                    return;
                } catch (CertificateException strictFailure) {
                    // fall through to pin / private-endpoint policy
                }

                final String fingerprint = fingerprint(chain);
                if (fingerprint == null) throw new CertificateException("unusable certificate chain");

                final String normalizedHost = (host == null) ? null : host.toLowerCase(Locale.US);
                if (normalizedHost == null) {
                    // cannot classify the endpoint - strict already failed, fail safe
                    throw new CertificateException("untrusted certificate (unknown host)");
                }
                final String pinned = getPinnedFingerprint(normalizedHost);

                if (pinned != null) {
                    if (pinned.equalsIgnoreCase(fingerprint)) {
                        accept(normalizedHost, fingerprint);
                        return;
                    }
                    // pinned host presenting a different certificate - classic MITM shape
                    recordPending(new PendingUntrusted(normalizedHost, fingerprint, subjectOf(chain), true));
                    throw new CertificateException("certificate changed for pinned host " + normalizedHost);
                }

                if (isPrivateEndpoint(normalizedHost)) {
                    // first-seen self-signed cert on a private endpoint: allow and pin it
                    pinCertificate(normalizedHost, fingerprint);
                    accept(normalizedHost, fingerprint);
                    return;
                }

                recordPending(new PendingUntrusted(normalizedHost, fingerprint, subjectOf(chain), false));
                throw new CertificateException("untrusted certificate for " + normalizedHost);
            }

            private void accept(final String host, final String fingerprint) {
                synchronized (mAcceptedFingerprints) {
                    mAcceptedFingerprints.add(fingerprint);
                }
                mLastState = TLS_STATE_SECURE_PINNED;
            }
        };
    }

    private static X509ExtendedTrustManager platformTrustManager() {
        try {
            final TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            factory.init((java.security.KeyStore) null);
            for (TrustManager trustManager : factory.getTrustManagers()) {
                if (trustManager instanceof X509ExtendedTrustManager) {
                    return (X509ExtendedTrustManager) trustManager;
                }
                if (trustManager instanceof X509TrustManager) {
                    // wrap non-extended manager (should not happen on Android)
                    final X509TrustManager delegate = (X509TrustManager) trustManager;
                    return new X509ExtendedTrustManager() {
                        @Override public X509Certificate[] getAcceptedIssuers() { return delegate.getAcceptedIssuers(); }
                        @Override public void checkClientTrusted(X509Certificate[] c, String a) throws CertificateException { delegate.checkClientTrusted(c, a); }
                        @Override public void checkServerTrusted(X509Certificate[] c, String a) throws CertificateException { delegate.checkServerTrusted(c, a); }
                        @Override public void checkClientTrusted(X509Certificate[] c, String a, Socket s) throws CertificateException { delegate.checkClientTrusted(c, a); }
                        @Override public void checkServerTrusted(X509Certificate[] c, String a, Socket s) throws CertificateException { delegate.checkServerTrusted(c, a); }
                        @Override public void checkClientTrusted(X509Certificate[] c, String a, SSLEngine e) throws CertificateException { delegate.checkClientTrusted(c, a); }
                        @Override public void checkServerTrusted(X509Certificate[] c, String a, SSLEngine e) throws CertificateException { delegate.checkServerTrusted(c, a); }
                    };
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // absolute fallback: strict = reject everything rather than trust-all
        return new X509ExtendedTrustManager() {
            @Override public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
            @Override public void checkClientTrusted(X509Certificate[] c, String a) throws CertificateException { throw new CertificateException("no trust manager"); }
            @Override public void checkServerTrusted(X509Certificate[] c, String a) throws CertificateException { throw new CertificateException("no trust manager"); }
            @Override public void checkClientTrusted(X509Certificate[] c, String a, Socket s) throws CertificateException { throw new CertificateException("no trust manager"); }
            @Override public void checkServerTrusted(X509Certificate[] c, String a, Socket s) throws CertificateException { throw new CertificateException("no trust manager"); }
            @Override public void checkClientTrusted(X509Certificate[] c, String a, SSLEngine e) throws CertificateException { throw new CertificateException("no trust manager"); }
            @Override public void checkServerTrusted(X509Certificate[] c, String a, SSLEngine e) throws CertificateException { throw new CertificateException("no trust manager"); }
        };
    }

    /***
     * Socket factory bound to the given trust manager - pass the SAME
     * X509TrustManager instance to OkHttp's sslSocketFactory(factory, tm).
     */
    public static SSLSocketFactory createSocketFactory(final X509TrustManager trustManager) {
        try {
            final SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{trustManager}, new java.security.SecureRandom());
            return sslContext.getSocketFactory();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    ////// Certificate helpers

    private static String fingerprint(final X509Certificate[] chain) {
        try {
            if ((chain == null) || (chain.length == 0)) return null;
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            final byte[] hash = digest.digest(chain[0].getEncoded());
            final StringBuilder builder = new StringBuilder(hash.length * 3);
            for (int i = 0; i < hash.length; ++i) {
                if (i > 0) builder.append(':');
                builder.append(String.format(Locale.US, "%02X", hash[i]));
            }
            return builder.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private static String subjectOf(final X509Certificate[] chain) {
        try {
            if ((chain == null) || (chain.length == 0)) return "";
            return chain[0].getSubjectX500Principal().getName();
        } catch (Exception e) {
            return "";
        }
    }
}
