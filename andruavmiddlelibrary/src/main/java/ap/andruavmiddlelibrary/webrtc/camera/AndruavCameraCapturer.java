package ap.andruavmiddlelibrary.webrtc.camera;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Handler;
import android.util.Range;
import android.view.Surface;
import android.view.WindowManager;

import org.webrtc.AndruavWebRTCGlobals;
import org.webrtc.CameraEnumerationAndroid;
import org.webrtc.CameraEnumerationAndroid.CaptureFormat;
import org.webrtc.CapturerObserver;
import org.webrtc.Logging;
import org.webrtc.Size;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.TextureBufferImpl;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoFrame;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * App-owned camera capturer: a VideoCapturer fed by the Camera2 API, and the
 * AndruavCameraControl surface for it. All camera work runs on the
 * SurfaceTextureHelper thread (the "camera thread").
 *
 * Capture path (CameraManager -> CameraCaptureSession -> repeating request ->
 * SurfaceTexture -> CapturerObserver) plus the freeze watchdog and error
 * event mapping equivalent to the fork's CameraStatistics/StateCallback. The
 * AndruavCameraControl camera controls (flash, zoom, switch) are stubs for
 * now; setFixedOrientationDegrees already stores the value that replaces the
 * AndruavWebRTCGlobals.fixedDeviceOrientationDegrees global on this path.
 */
public class AndruavCameraCapturer implements VideoCapturer, AndruavCameraControl {

    private static final String TAG = "AndruavCameraCapturer";

    // Freeze watchdog - same cadence as the fork's CameraStatistics: a 2 s
    // observer that fires onCameraFreezed once two consecutive periods pass
    // with zero frames (i.e. no frames for >= 4 s).
    private static final int CAMERA_OBSERVER_PERIOD_MS = 2000;
    private static final int CAMERA_FREEZE_REPORT_TIMOUT_MS = 4000;

    private final Context context;
    private final CameraManager cameraManager;
    private final Listener eventsListener;
    private String cameraId;

    // Set by initialize(). The caller owns surfaceTextureHelper.
    private SurfaceTextureHelper surfaceTextureHelper;
    private Handler cameraThreadHandler;
    private Context applicationContext;
    private CapturerObserver capturerObserver;

    // Camera-session state. Camera thread only.
    private CameraDevice cameraDevice;
    private Surface surface;
    private CameraCaptureSession captureSession;
    private CaptureRequest.Builder captureRequestBuilder;
    private CameraCharacteristics cameraCharacteristics;
    private CaptureFormat captureFormat;
    private int fpsUnitFactor = 1;
    private boolean sessionOpening;
    private boolean sessionRunning;
    private boolean firstFrameReported;

    // Freeze-watchdog state. Camera thread only.
    private int framesSinceLastCheck;
    private int freezePeriodCount;
    private boolean freezeReported;
    private int sensorOrientationDegrees;
    private boolean cameraFrontFacing;
    private float zoomMax = 0.0f;

    // Control state (stubs for E1d-E1g).
    private int flashMode = AndruavWebRTCGlobals.FlashOff;
    private float zoomLevel = 0.0f;
    private int fixedOrientationDegrees = -1;

    /**
     * @param cameraId camera2 id to capture from, or null to auto-pick the
     *                 first back-facing camera (first id if none).
     * @param listener camera events, same shapes as the fork's
     *                 CameraEventsHandler. May be null.
     */
    public AndruavCameraCapturer(final Context context, final String cameraId,
                                 final Listener listener) {
        this.context = context;
        this.cameraId = cameraId;
        this.eventsListener = listener;
        cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
    }

    /////////////////////////////////////////////////////////////////// VideoCapturer

    @Override
    public void initialize(final SurfaceTextureHelper surfaceTextureHelper,
                           final Context applicationContext,
                           final CapturerObserver capturerObserver) {
        this.surfaceTextureHelper = surfaceTextureHelper;
        this.applicationContext = applicationContext;
        this.capturerObserver = capturerObserver;
        cameraThreadHandler = surfaceTextureHelper == null ? null : surfaceTextureHelper.getHandler();
    }

    @Override
    public void startCapture(final int width, final int height, final int framerate) {
        Logging.d(TAG, "startCapture: " + width + "x" + height + "@" + framerate);
        if (surfaceTextureHelper == null) {
            throw new IllegalStateException("AndruavCameraCapturer must be initialized before startCapture.");
        }
        cameraThreadHandler.post(() -> openCameraSession(width, height, framerate));
    }

    @Override
    public void stopCapture() {
        Logging.d(TAG, "stopCapture");
        runOnCameraThread(() -> {
            stopInternal();
            if (capturerObserver != null) {
                capturerObserver.onCapturerStopped();
            }
        });
    }

    @Override
    public void changeCaptureFormat(final int width, final int height, final int framerate) {
        Logging.d(TAG, "changeCaptureFormat: " + width + "x" + height + "@" + framerate);
        cameraThreadHandler.post(() -> {
            stopInternal();
            openCameraSession(width, height, framerate);
        });
    }

    @Override
    public void dispose() {
        Logging.d(TAG, "dispose");
        stopCapture();
    }

    @Override
    public boolean isScreencast() {
        return false;
    }

    //////////////////////////////////////////////////////////// AndruavCameraControl

    @Override
    public void switchCamera(final CameraSwitchHandler handler) {
        // E1f: not implemented yet on this path.
        runOnCameraThread(() -> {
            if (handler != null) {
                handler.onCameraSwitchError("AndruavCameraCapturer.switchCamera not implemented");
            }
        });
    }

    @Override
    public void setFlash(final int onOff) {
        runOnCameraThread(() -> setFlashOnCameraThread(onOff));
    }

    private void setFlashOnCameraThread(final int onOff) {
        if (cameraFrontFacing) {
            // Front camera has no torch - same FlashDisabled reply as the fork.
            if (eventsListener != null) {
                eventsListener.onCameraFlashChanged(AndruavWebRTCGlobals.FlashDisabled);
            }
            return;
        }
        if (onOff != AndruavWebRTCGlobals.FlashOn && onOff != AndruavWebRTCGlobals.FlashOff) {
            return;
        }

        flashMode = onOff;
        if (sessionRunning && captureRequestBuilder != null) {
            applyFlashMode();
            if (!updateRepeatingRequest()) {
                return;
            }
        }
        if (eventsListener != null) {
            eventsListener.onCameraFlashChanged(onOff);
        }
    }

    private void applyFlashMode() {
        captureRequestBuilder.set(CaptureRequest.FLASH_MODE,
                flashMode == AndruavWebRTCGlobals.FlashOn
                        ? CameraMetadata.FLASH_MODE_TORCH : CameraMetadata.FLASH_MODE_OFF);
    }

    private boolean updateRepeatingRequest() {
        if (captureSession == null) {
            return false;
        }
        try {
            captureSession.setRepeatingRequest(
                    captureRequestBuilder.build(), captureCallback, cameraThreadHandler);
        } catch (CameraAccessException | IllegalStateException e) {
            reportError("Failed to update capture request. " + e);
            return false;
        }
        return true;
    }

    @Override
    public int getFlash() {
        if (cameraFrontFacing) {
            return AndruavWebRTCGlobals.FlashDisabled;
        }
        return flashMode;
    }

    @Override
    public void setZoom(final float zoom) {
        // E1e stub.
        zoomLevel = zoom;
    }

    @Override
    public float getZoom() {
        return zoomLevel;
    }

    @Override
    public boolean isZoomSupported() {
        return zoomMax > 0.0f;
    }

    @Override
    public void setFixedOrientationDegrees(final int degrees) {
        fixedOrientationDegrees = degrees;
    }

    /////////////////////////////////////////////////////////////////// camera session

    private void openCameraSession(final int width, final int height, final int framerate) {
        checkIsOnCameraThread();
        if (sessionRunning || sessionOpening) {
            Logging.w(TAG, "openCameraSession: session already running or opening.");
            return;
        }
        sessionOpening = true;

        cameraId = resolveCameraId();
        if (cameraId == null) {
            reportError("No usable camera found.");
            return;
        }

        try {
            cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
        } catch (CameraAccessException e) {
            reportError("getCameraCharacteristics(): " + e.getMessage());
            return;
        }

        sensorOrientationDegrees = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        cameraFrontFacing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING)
                == CameraMetadata.LENS_FACING_FRONT;
        final Float maxZoom = cameraCharacteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
        zoomMax = maxZoom == null ? 0.0f : maxZoom;
        if (cameraFrontFacing) {
            flashMode = AndruavWebRTCGlobals.FlashDisabled;
        }
        if (eventsListener != null) {
            eventsListener.onCameraFlashChanged(flashMode);
        }

        if (!findCaptureFormat(width, height, framerate)) {
            reportError("No supported capture formats.");
            return;
        }

        openCamera();
    }

    private boolean findCaptureFormat(final int width, final int height, final int framerate) {
        final Range<Integer>[] fpsRanges =
                cameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
        if (fpsRanges == null || fpsRanges.length == 0) {
            return false;
        }
        // Camera2 fps ranges are in fps, except old Tegra2-based devices which
        // report them x1000 - same normalization as the fork's Camera2Enumerator.
        fpsUnitFactor = fpsRanges[0].getUpper() < 1000 ? 1000 : 1;
        final List<CaptureFormat.FramerateRange> framerateRanges = new ArrayList<>();
        for (final Range<Integer> range : fpsRanges) {
            framerateRanges.add(new CaptureFormat.FramerateRange(
                    range.getLower() * fpsUnitFactor, range.getUpper() * fpsUnitFactor));
        }

        final StreamConfigurationMap streamMap =
                cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if (streamMap == null) {
            return false;
        }
        final android.util.Size[] nativeSizes = streamMap.getOutputSizes(android.graphics.SurfaceTexture.class);
        if (nativeSizes == null || nativeSizes.length == 0) {
            return false;
        }
        final List<Size> sizes = new ArrayList<>(nativeSizes.length);
        for (final android.util.Size size : nativeSizes) {
            sizes.add(new Size(size.getWidth(), size.getHeight()));
        }
        Logging.d(TAG, "Available preview sizes: " + sizes);
        Logging.d(TAG, "Available fps ranges: " + framerateRanges);

        final CaptureFormat.FramerateRange bestFpsRange =
                CameraEnumerationAndroid.getClosestSupportedFramerateRange(framerateRanges, framerate);
        final Size bestSize =
                CameraEnumerationAndroid.getClosestSupportedSize(sizes, width, height);
        captureFormat = new CaptureFormat(bestSize.width, bestSize.height, bestFpsRange);
        Logging.d(TAG, "Using capture format: " + captureFormat);
        return true;
    }

    @SuppressLint("MissingPermission")
    private void openCamera() {
        checkIsOnCameraThread();
        Logging.d(TAG, "Opening camera " + cameraId);
        if (eventsListener != null) {
            eventsListener.onCameraOpening(cameraId);
        }
        try {
            cameraManager.openCamera(cameraId, cameraStateCallback, cameraThreadHandler);
        } catch (CameraAccessException | IllegalArgumentException | SecurityException e) {
            reportError("Failed to open camera: " + e);
        }
    }

    private final CameraDevice.StateCallback cameraStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(final CameraDevice camera) {
            checkIsOnCameraThread();
            Logging.d(TAG, "Camera opened.");
            cameraDevice = camera;

            surfaceTextureHelper.setTextureSize(captureFormat.width, captureFormat.height);
            surface = new Surface(surfaceTextureHelper.getSurfaceTexture());
            try {
                camera.createCaptureSession(
                        Collections.singletonList(surface), captureSessionCallback, cameraThreadHandler);
            } catch (CameraAccessException e) {
                reportError("Failed to create capture session. " + e);
            }
        }

        @Override
        public void onDisconnected(final CameraDevice camera) {
            checkIsOnCameraThread();
            Logging.w(TAG, "Camera disconnected.");
            final boolean wasRunning = sessionRunning;
            final boolean wasOpening = sessionOpening;
            stopInternal();
            if (capturerObserver != null) {
                if (wasRunning) {
                    capturerObserver.onCapturerStopped();
                } else if (wasOpening) {
                    capturerObserver.onCapturerStarted(false);
                }
            }
            if (eventsListener != null) {
                eventsListener.onCameraDisconnected();
            }
        }

        @Override
        public void onError(final CameraDevice camera, final int errorCode) {
            checkIsOnCameraThread();
            reportError(cameraErrorDescription(errorCode));
        }

        @Override
        public void onClosed(final CameraDevice camera) {
            checkIsOnCameraThread();
            Logging.d(TAG, "Camera device closed.");
            if (eventsListener != null) {
                eventsListener.onCameraClosed();
            }
        }
    };

    private final CameraCaptureSession.StateCallback captureSessionCallback =
            new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(final CameraCaptureSession session) {
            checkIsOnCameraThread();
            Logging.d(TAG, "Camera capture session configured.");
            captureSession = session;
            try {
                captureRequestBuilder =
                        cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
                captureRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                        new Range<>(captureFormat.framerate.min / fpsUnitFactor,
                                captureFormat.framerate.max / fpsUnitFactor));
                captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                captureRequestBuilder.set(CaptureRequest.CONTROL_AE_LOCK, false);
                applyFlashMode();
                chooseFocusMode(captureRequestBuilder);
                captureRequestBuilder.addTarget(surface);
                session.setRepeatingRequest(captureRequestBuilder.build(), captureCallback, cameraThreadHandler);
            } catch (CameraAccessException | IllegalStateException e) {
                reportError("Failed to start capture request. " + e);
                return;
            }

            sessionOpening = false;
            sessionRunning = true;
            firstFrameReported = false;
            startFrameListening();
            framesSinceLastCheck = 0;
            freezePeriodCount = 0;
            freezeReported = false;
            cameraThreadHandler.postDelayed(freezeWatchdogRunnable, CAMERA_OBSERVER_PERIOD_MS);
            if (capturerObserver != null) {
                capturerObserver.onCapturerStarted(true);
            }
            Logging.d(TAG, "Camera device successfully started.");
        }

        @Override
        public void onConfigureFailed(final CameraCaptureSession session) {
            checkIsOnCameraThread();
            session.close();
            reportError("Failed to configure capture session.");
        }
    };

    private final CameraCaptureSession.CaptureCallback captureCallback =
            new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureFailed(final CameraCaptureSession session, final CaptureRequest request,
                                    final CaptureFailure failure) {
            Logging.d(TAG, "Capture failed: " + failure);
        }
    };

    // Camera observer - monitors the framerate like the fork's
    // CameraStatistics. Fires onCameraFreezed once, then stops.
    private final Runnable freezeWatchdogRunnable = new Runnable() {
        @Override
        public void run() {
            checkIsOnCameraThread();
            if (!sessionRunning) {
                return;
            }
            if (framesSinceLastCheck == 0) {
                ++freezePeriodCount;
                if (CAMERA_OBSERVER_PERIOD_MS * freezePeriodCount >= CAMERA_FREEZE_REPORT_TIMOUT_MS
                        && !freezeReported) {
                    freezeReported = true;
                    Logging.e(TAG, "Camera freezed.");
                    if (eventsListener != null) {
                        eventsListener.onCameraFreezed(
                                surfaceTextureHelper.isTextureInUse()
                                        ? "Camera failure. Client must return video buffers."
                                        : "Camera failure.");
                    }
                    return;
                }
            } else {
                freezePeriodCount = 0;
            }
            framesSinceLastCheck = 0;
            cameraThreadHandler.postDelayed(this, CAMERA_OBSERVER_PERIOD_MS);
        }
    };

    private void startFrameListening() {
        surfaceTextureHelper.startListening(frame -> {
            checkIsOnCameraThread();
            if (!sessionRunning) {
                return;
            }
            ++framesSinceLastCheck;
            if (!firstFrameReported) {
                firstFrameReported = true;
                if (eventsListener != null) {
                    eventsListener.onFirstFrameAvailable();
                }
            }
            final VideoFrame modifiedFrame = new VideoFrame(
                    createTextureBufferWithModifiedTransformMatrix(
                            (TextureBufferImpl) frame.getBuffer(),
                            cameraFrontFacing, -sensorOrientationDegrees),
                    getFrameOrientation(), frame.getTimestampNs());
            capturerObserver.onFrameCaptured(modifiedFrame);
            modifiedFrame.release();
        });
    }

    private void stopInternal() {
        checkIsOnCameraThread();
        sessionOpening = false;
        sessionRunning = false;
        cameraThreadHandler.removeCallbacks(freezeWatchdogRunnable);
        surfaceTextureHelper.stopListening();
        if (captureSession != null) {
            captureSession.close();
            captureSession = null;
        }
        captureRequestBuilder = null;
        if (surface != null) {
            surface.release();
            surface = null;
        }
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }

    private void reportError(final String error) {
        Logging.e(TAG, "Error: " + error);
        // A failure before the session ran is a start failure - tell the
        // observer like the fork's CreateSessionCallback.onFailure does.
        final boolean startFailed = !sessionRunning;
        stopInternal();
        if (startFailed && capturerObserver != null) {
            capturerObserver.onCapturerStarted(false);
        }
        if (eventsListener != null) {
            eventsListener.onCameraError(error);
        }
    }

    private void chooseFocusMode(final CaptureRequest.Builder requestBuilder) {
        final int[] availableFocusModes =
                cameraCharacteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
        if (availableFocusModes == null) {
            return;
        }
        for (final int mode : availableFocusModes) {
            if (mode == CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO) {
                requestBuilder.set(
                        CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);
                Logging.d(TAG, "Using continuous video auto-focus.");
                return;
            }
        }
        Logging.d(TAG, "Auto-focus is not available.");
    }

    private String resolveCameraId() {
        if (cameraId != null) {
            return cameraId;
        }
        try {
            final String[] ids = cameraManager.getCameraIdList();
            for (final String id : ids) {
                if (cameraManager.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING)
                        == CameraMetadata.LENS_FACING_BACK) {
                    return id;
                }
            }
            if (ids.length > 0) {
                return ids[0];
            }
        } catch (CameraAccessException e) {
            Logging.e(TAG, "resolveCameraId: " + e);
        }
        return null;
    }

    /////////////////////////////////////////////////////////////////// frame helpers

    // Same math as the fork's CameraSession: undo the front-camera mirror and
    // fold the sensor orientation into the texture transform, then report the
    // residual rotation in the VideoFrame.
    private static VideoFrame.TextureBuffer createTextureBufferWithModifiedTransformMatrix(
            final TextureBufferImpl buffer, final boolean mirror, final int rotation) {
        final Matrix transformMatrix = new Matrix();
        transformMatrix.preTranslate(0.5f, 0.5f);
        if (mirror) {
            transformMatrix.preScale(-1f, 1f);
        }
        transformMatrix.preRotate(rotation);
        transformMatrix.preTranslate(-0.5f, -0.5f);
        return buffer.applyTransformMatrix(transformMatrix, buffer.getWidth(), buffer.getHeight());
    }

    private int getDeviceOrientation() {
        if (fixedOrientationDegrees >= 0) {
            return fixedOrientationDegrees;
        }
        final WindowManager wm = (WindowManager) applicationContext.getSystemService(Context.WINDOW_SERVICE);
        switch (wm.getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_90:
                return 90;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_270:
                return 270;
            case Surface.ROTATION_0:
            default:
                return 0;
        }
    }

    private int getFrameOrientation() {
        int rotation = getDeviceOrientation();
        if (!cameraFrontFacing) {
            rotation = 360 - rotation;
        }
        return (sensorOrientationDegrees + rotation) % 360;
    }

    private void runOnCameraThread(final Runnable runnable) {
        if (cameraThreadHandler == null) {
            Logging.w(TAG, "runOnCameraThread: not initialized.");
            return;
        }
        if (Thread.currentThread() == cameraThreadHandler.getLooper().getThread()) {
            runnable.run();
        } else {
            cameraThreadHandler.post(runnable);
        }
    }

    private void checkIsOnCameraThread() {
        if (Thread.currentThread() != cameraThreadHandler.getLooper().getThread()) {
            throw new IllegalStateException("Not on camera thread.");
        }
    }

    private static String cameraErrorDescription(final int errorCode) {
        switch (errorCode) {
            case CameraDevice.StateCallback.ERROR_CAMERA_DEVICE:
                return "Camera device has encountered a fatal error.";
            case CameraDevice.StateCallback.ERROR_CAMERA_DISABLED:
                return "Camera device could not be opened due to a device policy.";
            case CameraDevice.StateCallback.ERROR_CAMERA_IN_USE:
                return "Camera device is in use already.";
            case CameraDevice.StateCallback.ERROR_CAMERA_SERVICE:
                return "Camera service has encountered a fatal error.";
            case CameraDevice.StateCallback.ERROR_MAX_CAMERAS_IN_USE:
                return "Camera device could not be opened because there are too many other open camera devices.";
            default:
                return "Unknown camera error: " + errorCode;
        }
    }
}
