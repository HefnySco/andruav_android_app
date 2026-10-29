package ap.andruavmiddlelibrary.webrtc.camera;

import android.content.Context;

import org.webrtc.CapturerObserver;
import org.webrtc.EglBase;
import org.webrtc.Logging;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoFrame;

/**
 * Debug-only hooks for the app camera path. Not part of the shipping FPV
 * flow - invoked from tests/debug entries to exercise AndruavCameraCapturer
 * outside PeerConnectionManager.
 */
public final class AndruavCameraDebug {

    private static final String TAG = "AndruavCamDebug";

    private AndruavCameraDebug() {}

    /**
     * Opens the camera through AndruavCameraCapturer on a throwaway
     * EglBase/SurfaceTextureHelper, counts frames for durationMs, then logs
     * the result under logcat tag 'AndruavCamDebug'. Runs on its own thread;
     * returns immediately. cameraId may be null (auto-picks the back camera).
     */
    public static void runCaptureSelfTest(final Context context, final String cameraId,
                                          final long durationMs) {
        new Thread(() -> {
            Logging.d(TAG, "capture self test starting, cameraId=" + cameraId);
            final EglBase eglBase = EglBase.create();
            final SurfaceTextureHelper helper =
                    SurfaceTextureHelper.create("CamDebugTTX", eglBase.getEglBaseContext());
            final int[] frameCount = {0};
            final int[] lastFrameSize = {0, 0};
            try {
                final AndruavCameraCapturer capturer =
                        new AndruavCameraCapturer(context.getApplicationContext(), cameraId,
                                new AndruavCameraControl.Listener() {
                                    @Override public void onCameraFlashChanged(int onOff) {}
                                    @Override public void onCameraZoomChanged(float zoom) {}
                                    @Override public void onCameraError(String errorDescription) {
                                        Logging.e(TAG, "self test onCameraError: " + errorDescription);
                                    }
                                    @Override public void onCameraFreezed(String errorDescription) {}
                                    @Override public void onCameraOpening(String cameraName) {
                                        Logging.d(TAG, "self test onCameraOpening: " + cameraName);
                                    }
                                    @Override public void onFirstFrameAvailable() {
                                        Logging.d(TAG, "self test onFirstFrameAvailable");
                                    }
                                    @Override public void onCameraClosed() {}
                                    @Override public void onCameraDisconnected() {
                                        Logging.e(TAG, "self test onCameraDisconnected");
                                    }
                                });
                capturer.initialize(helper, context.getApplicationContext(), new CapturerObserver() {
                    @Override public void onCapturerStarted(boolean success) {
                        Logging.d(TAG, "self test onCapturerStarted success=" + success);
                    }
                    @Override public void onCapturerStopped() {}
                    @Override public void onFrameCaptured(VideoFrame frame) {
                        frameCount[0]++;
                        lastFrameSize[0] = frame.getBuffer().getWidth();
                        lastFrameSize[1] = frame.getBuffer().getHeight();
                    }
                });
                capturer.startCapture(1280, 720, 15);
                Thread.sleep(durationMs);
                capturer.stopCapture();
                capturer.dispose();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                helper.dispose();
                eglBase.release();
            }
            Logging.d(TAG, "capture self test done: frames=" + frameCount[0]
                    + " in " + durationMs + "ms"
                    + " last_frame=" + lastFrameSize[0] + "x" + lastFrameSize[1]);
        }, "AndruavCamDebug").start();
    }
}
