package ap.andruavmiddlelibrary.webrtc.camera;

/**
 * App-owned camera control surface. Implemented by the Andruav camera
 * capturer so that camera controls (flash, zoom, switch, orientation) and
 * camera events are app code instead of org.webrtc fork APIs — the capturer
 * behind it can change without callers noticing.
 * Event shapes mirror the fork's CameraVideoCapturer callbacks.
 */
public interface AndruavCameraControl {

    /**
     * Flash states — same values the removed fork's AndruavWebRTCGlobals used,
     * so the wire meaning sent to the GCS is unchanged.
     */
    int FLASH_ON = 1;
    int FLASH_OFF = 2;
    int FLASH_DISABLED = 999;

    /**
     * Camera events — same shapes as the fork's
     * CameraVideoCapturer.CameraEventsHandler. Callbacks are executed from an
     * arbitrary thread.
     */
    interface Listener {

        // Callback invoked when Flash turned off or on
        void onCameraFlashChanged(int onOff);

        // Callback invoked when zoom changed
        void onCameraZoomChanged(final float zoom);

        // Camera error handler - invoked when camera can not be opened
        // or any camera exception happens on camera thread.
        void onCameraError(String errorDescription);

        // Invoked when camera stops receiving frames.
        void onCameraFreezed(String errorDescription);

        // Callback invoked when camera is opening.
        void onCameraOpening(String cameraName);

        // Callback invoked when first camera frame is available after camera is started.
        void onFirstFrameAvailable();

        // Callback invoked when camera is closed.
        void onCameraClosed();

        // Called when camera is disconnected.
        void onCameraDisconnected();
    }

    /**
     * Camera switch result — same shape as the fork's
     * CameraVideoCapturer.CameraSwitchHandler. The callback may be called on
     * an arbitrary thread.
     */
    interface CameraSwitchHandler {

        // Invoked on success. |isFrontCamera| is true if the new camera is front facing.
        void onCameraSwitchDone(boolean isFrontCamera);

        // Invoked on failure, e.g. camera is stopped or only one camera available.
        void onCameraSwitchError(String errorDescription);
    }

    /**
     * Switch camera to the next valid camera id. This can only be called while
     * the camera is running. This function can be called from any thread.
     */
    void switchCamera(CameraSwitchHandler handler);

    /**
     * Turns on/off flash.
     */
    void setFlash(final int onOff);

    int getFlash();

    /**
     * Changes camera zoom.
     */
    void setZoom(final float zoom);

    float getZoom();

    /**
     * Check if camera supports zoom.
     */
    boolean isZoomSupported();

    /**
     * Fixes the device orientation the capturer assumes when rotating frames,
     * for a phone rigidly mounted to the airframe. Replaces the
     * AndruavWebRTCGlobals.fixedDeviceOrientationDegrees global on the app
     * capturer path. Degrees follow Surface.Rotation semantics (0/90/180/270).
     */
    void setFixedOrientationDegrees(final int degrees);
}
