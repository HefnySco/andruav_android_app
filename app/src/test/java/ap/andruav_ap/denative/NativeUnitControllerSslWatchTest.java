package ap.andruav_ap.denative;

import org.junit.Test;

import ap.andruav_ap.denative.config.DeConfigSettings;
import ap.andruav_ap.denative.config.DeConfigWriter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * DE-0103: the 30 s SSL-verify watch must not post Event_SSLVerifyFailed
 * for an IP-literal auth host. These drive the real chain the watch
 * uses: commArgs() builds de_comm's argv, verifySsl() reads back the
 * --auth_verify_ssl flag de_comm was started with (sCommVerifySsl), and
 * sslVerifyWatchPosts() is the post decision.
 */
public class NativeUnitControllerSslWatchTest {

    private static DeConfigSettings settings(final String host, final boolean reqSsl) {
        final DeConfigSettings s = new DeConfigSettings();
        s.authHost = host;
        s.authPort = 19408;
        s.requireSsl = reqSsl;
        return s;
    }

    /** The --auth_verify_ssl value the running de_comm would carry. */
    private static boolean runningVerifySsl(final String host, final boolean reqSsl) {
        return DeConfigWriter.verifySsl(
                DeConfigWriter.commArgs(settings(host, reqSsl)));
    }

    @Test
    public void ipLiteralAuthHostDoesNotPost() {
        // an IPv4 literal cannot carry a verifiable SAN, so de_comm runs
        // with verify off even when req_ssl is on - a dead comm line then
        // cannot be a cert failure and no event may post
        final boolean verify = runningVerifySsl("192.169.9.1", true);
        assertFalse(verify);
        assertFalse(NativeUnitController.sslVerifyWatchPosts(verify, false));
    }

    @Test
    public void ipv6LiteralAuthHostDoesNotPost() {
        final boolean verify = runningVerifySsl("fe80::1", true);
        assertFalse(verify);
        assertFalse(NativeUnitController.sslVerifyWatchPosts(verify, false));
    }

    @Test
    public void dnsAuthHostWithReqSslPostsWhileCommIsDown() {
        final boolean verify = runningVerifySsl("auth.example.com", true);
        assertTrue(verify);
        assertTrue(NativeUnitController.sslVerifyWatchPosts(verify, false));
        // once the comm line is up the watch stays quiet
        assertFalse(NativeUnitController.sslVerifyWatchPosts(verify, true));
    }

    @Test
    public void dnsAuthHostWithoutReqSslDoesNotPost() {
        final boolean verify = runningVerifySsl("auth.example.com", false);
        assertFalse(verify);
        assertFalse(NativeUnitController.sslVerifyWatchPosts(verify, false));
    }
}
