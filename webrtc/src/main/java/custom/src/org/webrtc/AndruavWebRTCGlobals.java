package org.webrtc;

public class AndruavWebRTCGlobals {

    public static final int FlashOn        = 1;
    public static final int FlashOff       = 2;
    public static final int FlashDisabled  = 999;

    /**
     * Overrides CameraSession.getDeviceOrientation()'s live WindowManager rotation query when
     * &gt;= 0. Andruav mounts the capturing phone rigidly (e.g. on a drone) and always forces
     * landscape via setRequestedOrientation() - the phone itself never physically rotates. But
     * Android's Picture-in-Picture shifts "foreground orientation owner" away from the shrunk
     * Activity to whatever's now fullscreen behind it (usually the portrait home launcher), which
     * flips the live-queried rotation and visibly rotates both the local preview and the actual
     * outgoing stream by 90 degrees while minimized. A fixed value sidesteps that entirely.
     * -1 means "use the live query" (stock WebRTC behavior).
     */
    public static int fixedDeviceOrientationDegrees = -1;

}
