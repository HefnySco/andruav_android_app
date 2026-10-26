package ap.andruav_ap.denative.providers.camera;

import java.util.List;

/**
 * Android-facing half of the camera capability: the provider (pure de.cap/1
 * logic, unit-tested on the JVM) talks to the real camera through this
 * interface only. {@code CameraController} is the production
 * implementation; tests inject a fake.
 *
 * Channel keys follow the Linux de_camera surface: "back", "front" and
 * "usb_<n>" are the resource keys missions target; the same keys name the
 * per-channel commands/facts (camera.<ch>.capture|record_on|record_off|
 * went_recording|went_idle).
 */
public interface CameraBackend {

    /** One camera as seen on the wire. */
    class CameraChannel {
        /** resource key: "back", "front", "usb_<n>", ... */
        public final String key;
        /** stable device identity (Camera2 cameraId). */
        public final String id;
        /** human label, never used for routing. */
        public final String name;

        public CameraChannel(String key, String id, String name) {
            this.key = key;
            this.id = id;
            this.name = name;
        }
    }

    /**
     * Facts producer -> provider. Called on the backend's own threads;
     * implementations of the listener must not block.
     */
    interface Listener {
        /** A still was saved: camera.captured {file, pos?, t, n, inv?, ch}. */
        void onCaptured(String channelKey, String file, double[] pos,
                        long tMs, int index, String invokeId);
        /** recording off -> on: camera.<ch>.went_recording {src, file?}. */
        void onWentRecording(String channelKey, String src, String file);
        /** recording on -> off: camera.<ch>.went_idle {src, reason, file?}. */
        void onWentIdle(String channelKey, String src, String reason, String file);
    }

    /** Surfaced channels in stable order (advert order). */
    List<CameraChannel> channels();

    /** Key used when an action/command carries channel "" or none. */
    String defaultChannelKey();

    /**
     * Resolve a channel param to a resource key: "" -> default, then key,
     * then device id, then name. null when nothing matches.
     */
    String resolveChannelKey(String channel);

    /** Key of the channel the streaming capturer is on, null when idle. */
    String activeChannelKey();

    /** Facts listener - at most one (the provider). */
    void setListener(Listener listener);

    /**
     * Queue a still burst on a channel. count >= 1, intervalMs >= 0. Returns
     * once the burst started (photos arrive afterwards as captured facts);
     * false + err[0] when the channel is unknown or busy.
     */
    boolean startCapture(String channelKey, int count, int intervalMs,
                         boolean gcsPreview, String invokeId, String src, String[] err);

    /**
     * THE record choke point (single writer of the recording flag):
     * edge-only, idempotent. src is the wire cause: "gcs" | "invoke" |
     * "event" | "fault" | "shutdown".
     */
    boolean setRecording(String channelKey, boolean on, String src, String[] err);

    boolean isRecording(String channelKey);

    boolean anyRecording();

    /** Linear zoom level (advert range 1..10). */
    boolean setZoom(String channelKey, double level, String[] err);
}
