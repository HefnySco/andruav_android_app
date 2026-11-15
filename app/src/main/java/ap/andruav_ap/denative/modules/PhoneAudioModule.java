package ap.andruav_ap.denative.modules;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import de.databus.CapSchema;
import de.databus.caps.DeCapabilityProvider;

/**
 * VC-7 (GL-0247 voice chat): owns the ns=microphone namespace on the
 * andruav_phone CapabilityHost, so a native unit advertises the
 * capability that makes the webclient show its Listen button (VC-6).
 * <ul>
 * <li>the de.cap/1 advert is identical in shape to the one the Linux
 * de_camera publishes under VC-2: state {active, muted} and the
 * active_changed event;</li>
 * <li>the audio itself is not on the bus - it is the audio track
 * PeerConnectionManager puts on the WebRTC local media stream while
 * the app holds RECORD_AUDIO; PnPeer attaches it as a SEND_ONLY
 * transceiver on every dial and a GCS dialing channel=mic receives
 * it;</li>
 * <li>the advert goes out only while RECORD_AUDIO is granted - a GCS
 * must never see a microphone that cannot capture.</li>
 * </ul>
 * No actions and no live state yet: the push-to-listen state machine is
 * a later VC task. The TTS/play-file sound module is untouched - voice
 * chat is a separate namespace.
 */
public final class PhoneAudioModule implements DeCapabilityProvider {

    public static final String NS_MICROPHONE = "microphone";

    /** The mic track id is the wire channel the GCS dials (channel=mic). */
    public static final String CHANNEL_MIC = "mic";

    /** The app's microphone surface. */
    public interface Mic {
        /** True while the app holds RECORD_AUDIO (the capability exists). */
        boolean usable();
    }

    private final Mic m_mic;
    private final String m_version;

    public PhoneAudioModule(final Mic mic, final String version) {
        m_mic = mic;
        m_version = version;
    }

    @Override
    public List<Object> adverts() {
        final List<Object> adverts = new ArrayList<>(1);
        if (m_mic.usable()) {
            try {
                adverts.add(advert(m_version));
            } catch (JSONException e) {
                // the advert is built from constants - unreachable in practice
            }
        }
        return adverts;
    }

    @Override
    public void invoke(final String id, final String ns, final String act,
                       final JSONObject params, final long deadlineMs,
                       final ResultSink sink) {
        sink.err("no actions on " + ns);
    }

    @Override
    public void onEvent(final String name, final JSONObject ed, final String nonce) {
    }

    /** No live state tracking yet - see the class comment. */
    @Override
    public JSONObject state(final String ns) {
        return new JSONObject();
    }

    /** The de.cap/1 advert, identical in shape to the VC-2 Linux one. */
    static JSONObject advert(final String version) throws JSONException {
        return new JSONObject()
                .put("schema", CapSchema.CAP_SCHEMA_ID)
                .put("ns", NS_MICROPHONE)
                .put("module", "andruav_android")
                .put("ver", version)
                .put("desc", "phone microphone over WebRTC, dial channel=" + CHANNEL_MIC)
                .put("state", new JSONObject()
                        .put("active", new JSONObject()
                                .put("type", "bool")
                                .put("desc", "mic audio is streaming to a GCS"))
                        .put("muted", new JSONObject()
                                .put("type", "bool")
                                .put("desc", "mic capture is muted")))
                .put("events", new JSONObject()
                        .put("active_changed", new JSONObject()
                                .put("payload", new JSONObject()
                                        .put("active", new JSONObject()
                                                .put("type", "bool")
                                                .put("desc", "new streaming state")))));
    }
}
