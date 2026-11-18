package ap.andruav_ap.denative.modules;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import de.databus.CapSchema;
import de.databus.caps.CapabilityHost;
import de.databus.caps.DeCapabilityProvider;

/**
 * VC-7/VC-8 (GL-0247 voice chat): owns the voice namespaces on the
 * andruav_phone CapabilityHost, so a native unit advertises the
 * capabilities that make the webclient show its Listen and Talk
 * buttons (VC-6). The two are independent - a phone can advertise a
 * speaker with no mic.
 * <ul>
 * <li>ns=microphone (VC-7): the audio is the mic track
 * PeerConnectionManager puts on the WebRTC local media stream; a GCS
 * dialing channel=mic receives it. DE-0199: advertised only once the
 * track exists - the permission alone advertises a mic that cannot
 * capture.</li>
 * <li>ns=speaker (VC-8): a GCS dialing channel=spk sends its mic audio;
 * the remote track plays on the phone speaker once enabled in
 * PeerConnectionManager.onAddRemoteStream. mute/unmute invokes reach
 * the stream through {@link Speaker}.</li>
 * </ul>
 * The adverts match the VC-2/VC-3 Linux contracts. No live mic state
 * yet: the push-to-listen state machine is a later VC task. The
 * TTS/play-file sound module is untouched - voice chat is a separate
 * namespace. Android-free on purpose so invokes and advert gating run
 * under plain JUnit.
 */
public final class PhoneAudioModule implements DeCapabilityProvider {

    public static final String NS_MICROPHONE = "microphone";
    public static final String NS_SPEAKER = "speaker";

    /** The mic track id is the wire channel the GCS dials (channel=mic). */
    public static final String CHANNEL_MIC = com.andruav.uavos.modules.UAVOSConstants.VOICE_CHANNEL_MIC;
    /** The GCS-to-unit voice direction dials channel=spk. */
    public static final String CHANNEL_SPK = com.andruav.uavos.modules.UAVOSConstants.VOICE_CHANNEL_SPK;

    /** The app's microphone surface. */
    public interface Mic {
        /** DE-0199: true once the WebRTC mic track exists, not just on grant. */
        boolean usable();
    }

    /** The speaker side of the WebRTC stream, narrowed to mute control. */
    public interface Speaker {
        void setMuted(boolean muted);

        boolean muted();
    }

    private final Mic m_mic;
    private final Speaker m_speaker;
    private final String m_version;
    private CapabilityHost m_host;

    public PhoneAudioModule(final Mic mic, final Speaker speaker, final String version) {
        m_mic = mic;
        m_speaker = speaker;
        m_version = version;
    }

    public void attach(final CapabilityHost host) {
        m_host = host;
    }

    @Override
    public List<Object> adverts() {
        final List<Object> adverts = new ArrayList<>(2);
        try {
            if (m_mic.usable()) adverts.add(microphoneAdvert(m_version));
            adverts.add(speakerAdvert(m_version));
        } catch (JSONException e) {
            // the adverts are built from constants - unreachable in practice
        }
        return adverts;
    }

    @Override
    public void invoke(final String id, final String ns, final String act,
                       final JSONObject params, final long deadlineMs,
                       final ResultSink sink) {
        if (NS_SPEAKER.equals(ns)) {
            if ("mute".equals(act) || "unmute".equals(act)) {
                m_speaker.setMuted("mute".equals(act));
                final boolean muted = m_speaker.muted();
                if (m_host != null) {
                    try {
                        m_host.fireEvent(NS_SPEAKER, "muted_changed",
                                new JSONObject().put("muted", muted));
                    } catch (JSONException e) {
                        // payload is a constant shape - unreachable
                    }
                }
                try {
                    sink.ok(new JSONObject().put("muted", muted));
                } catch (JSONException e) {
                    sink.ok(null);
                }
                return;
            }
            sink.err("unknown action speaker." + act);
            return;
        }
        sink.err("no actions on " + ns);
    }

    @Override
    public void onEvent(final String name, final JSONObject ed, final String nonce) {
    }

    /** Speaker mute is live; mic state tracking is a later VC task. */
    @Override
    public JSONObject state(final String ns) {
        final JSONObject state = new JSONObject();
        if (NS_SPEAKER.equals(ns)) {
            try {
                state.put("muted", m_speaker.muted());
            } catch (JSONException e) {
                // constant shape - unreachable
            }
        }
        return state;
    }

    /** The de.cap/1 mic advert, identical in shape to the VC-2 Linux one. */
    static JSONObject microphoneAdvert(final String version) throws JSONException {
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

    /** The de.cap/1 speaker advert, identical in shape to the VC-3 Linux one. */
    static JSONObject speakerAdvert(final String version) throws JSONException {
        return new JSONObject()
                .put("schema", CapSchema.CAP_SCHEMA_ID)
                .put("ns", NS_SPEAKER)
                .put("module", "andruav_android")
                .put("ver", version)
                .put("desc", "phone speaker over WebRTC, dial channel=" + CHANNEL_SPK)
                .put("actions", new JSONObject()
                        .put("mute", new JSONObject()
                                .put("desc", "mute the speaker voice playout"))
                        .put("unmute", new JSONObject()
                                .put("desc", "resume the speaker voice playout")))
                .put("state", new JSONObject()
                        .put("muted", new JSONObject()
                                .put("type", "bool")
                                .put("desc", "speaker voice playout is muted")))
                .put("events", new JSONObject()
                        .put("muted_changed", new JSONObject()
                                .put("payload", new JSONObject()
                                        .put("muted", new JSONObject()
                                                .put("type", "bool")
                                                .put("desc", "new muted state")))));
    }
}
