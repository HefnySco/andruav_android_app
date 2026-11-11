package ap.andruav_ap.activities.main;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** AN-0174: the shared "connect FCB" action (sheet button + remote CONNECT_FCB). */
public class FcbConnectLinkTest {

    private static final class FakeSwitch implements FcbConnectionSheet.FcbLinkSwitch {
        boolean enabled;
        int set_calls;

        @Override
        public boolean isEnabled() {
            return enabled;
        }

        @Override
        public void setEnabled(boolean e) {
            set_calls++;
            enabled = e;
        }
    }

    @Test
    public void connectEnablesAnOffLink() {
        final FakeSwitch link = new FakeSwitch();
        assertTrue(FcbConnectionSheet.connectFcbLink(link));
        assertTrue(link.enabled);
        assertEquals(1, link.set_calls);
    }

    @Test
    public void connectOnAnEnabledLinkChangesNothing() {
        // a repeated remote CONNECT_FCB must not touch the preference -
        // a write would make DeConfigSync restart de_mavlink
        final FakeSwitch link = new FakeSwitch();
        link.enabled = true;
        assertFalse(FcbConnectionSheet.connectFcbLink(link));
        assertTrue(link.enabled);
        assertEquals(0, link.set_calls);
    }
}
