package ap.andruavmiddlelibrary.webrtc.camera;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.ImageFormat;
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
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.util.Range;
import android.view.Surface;
import android.view.WindowManager;

import org.webrtc.AndruavWebRTCGlobals;
import org.webrtc.CameraEnumerationAndroid;
import org.webrtc.CameraEnumerationAndroid.CaptureFormat;
import org.webrtc.CapturerObserver;
import org.webrtc.Logging;
import org.webrtc.NV21Buffer;
import org.webrtc.Size;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.TextureBufferImpl;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoFrame;

import java.util.ArrayList;
import java.util.Arrays;
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

    /**
     * Frame-path selector (E1j): false delivers texture frames through the
     * SurfaceTextureHelper (default); true captures through an
     * ImageReader(YUV_420_888) -> NV21Buffer path instead, for phones whose
     * hardware encoder rejects texture frames. Set before startCapture.
     */
    public static boolean useYuvCapturePath = false;

    private final Context context;
    private final CameraManager cameraManager;
    private final Listener eventsListener;
    private String cameraId;

    // Set by initialize(). The caller owns surfaceTextureHelper.
    private SurfaceTextureHelper surfaceTextureHelper;
    private Handler cameraThreadHandler;
    private Context applicationContext;
    private CapturerObserver capturerObserver;

    // Requested capture format - kept so a camera switch can reopen with it.
    private int requestedWidth;
    private int requestedHeight;
    private int requestedFps;

    // Camera-session state. Camera thread only.
    private CameraDevice cameraDevice;
    private Surface surface;
    private ImageReader imageReader;
    private byte[] nv21Data;
    private CameraCaptureSession captureSession;
    private CaptureRequest.Builder captureRequestBuilder;
    private CameraCharacteristics cameraCharacteristics;
    private CaptureFormat captureFormat;

    // Camera switch state. Camera thread only.
    private List<String> cameraIds = Collections.emptyList();
    private CameraSwitchHandler pendingSwitchHandler;
    private int fpsUnitFactor = 1;
    private boolean sessionOpening;
    private boolean sessionRunning;
    private boolean firstFrameReported;
    // Camera-open generations. openCamera() bumps cameraOpenGeneration and
    // hands it to the callbacks it registers; stopInternal() raises the stale
    // cutoff so a stop landing while an openCamera -> onConfigured sequence is
    // still in flight marks that open (and every earlier one) stale - the late
    // HAL callbacks then close what they are handed instead of wiring a session
    // after onCapturerStopped() already ran. Same net effect as the fork, where
    // stopCapture() blocks on stateLock until the session open settles.
    private int cameraOpenGeneration;
    private int staleCameraOpenGeneration;

    // Freeze-watchdog state. Camera thread only.
    private int framesSinceLastCheck;
    private int freezePeriodCount;
    private boolean freezeReported;
    private int sensorOrientationDegrees;
    private boolean cameraFrontFacing;
    private float zoomMax = 0.0f;
    private Range<Float> zoomRatioRange;

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
        requestedWidth = width;
        requestedHeight = height;
        requestedFps = framerate;
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
        requestedWidth = width;
        requestedHeight = height;
        requestedFps = framerate;
        cameraThreadHandler.post(() -> {
            if (!sessionRunning && !sessionOpening) {
                // Not capturing: the recorded request applies on the next
                // startCapture - reopening here would start the camera.
                return;
            }
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
        // May be called from any thread - like the fork, real work runs on the
        // camera thread.
        runOnCameraThread(() -> switchCameraInternal(handler));
    }

    private void switchCameraInternal(final CameraSwitchHandler handler) {
        checkIsOnCameraThread();
        cameraIds = orderedCameraIds(context);
        if (cameraIds.size() < 2) {
            reportSwitchError(handler, "No camera to switch to.");
            return;
        }
        if (pendingSwitchHandler != null) {
            reportSwitchError(handler, "Camera switch already in progress.");
            return;
        }
        if (!sessionRunning) {
            // Only a running session may switch (simpler than the fork's
            // switch-while-opening state machine).
            reportSwitchError(handler, "switchCamera: camera is not running.");
            return;
        }

        pendingSwitchHandler = handler;
        final int index = Math.max(0, cameraIds.indexOf(cameraId));
        cameraId = cameraIds.get((index + 1) % cameraIds.size());
        Logging.d(TAG, "switchCamera: switching to camera " + cameraId);
        stopInternal();
        openCameraSession(requestedWidth, requestedHeight, requestedFps);
    }

    private void reportSwitchError(final CameraSwitchHandler handler, final String error) {
        Logging.e(TAG, "switchCamera: " + error);
        if (handler != null) {
            handler.onCameraSwitchError(error);
        }
    }

    private void finishPendingSwitch() {
        if (pendingSwitchHandler != null) {
            pendingSwitchHandler.onCameraSwitchDone(cameraFrontFacing);
            pendingSwitchHandler = null;
        }
    }

    private void failPendingSwitch(final String error) {
        if (pendingSwitchHandler != null) {
            pendingSwitchHandler.onCameraSwitchError(error);
            pendingSwitchHandler = null;
        }
    }

    /**
     * The camera id order this capturer cycles through - the single source of
     * truth for camera order, shared with CameraController's channel table.
     * CameraManager.getCameraIdList() order.
     */
    public static List<String> orderedCameraIds(final Context context) {
        final CameraManager manager =
                (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        if (manager == null) {
            return Collections.emptyList();
        }
        try {
            return Arrays.asList(manager.getCameraIdList());
        } catch (CameraAccessException e) {
            Logging.e(TAG, "orderedCameraIds: " + e);
            return Collections.emptyList();
        }
    }

    /**
     * Largest capture format a camera supports for FPV - biggest preview size
     * from the Camera2 stream map plus its achievable fps (plain fps). Used by
     * PeerConnectionManager's MAX resolution mode on the app capturer path.
     *
     * @return int[]{width, height, fps} or null when it cannot be determined.
     */
    public static int[] maxCaptureFormat(final Context context, final String cameraId) {
        final CameraManager manager =
                (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        if (manager == null) {
            return null;
        }
        try {
            String id = cameraId;
            if (id == null) {
                // Same auto-pick as resolveCameraId: first back-facing, else first.
                final List<String> ids = orderedCameraIds(context);
                for (final String candidate : ids) {
                    if (manager.getCameraCharacteristics(candidate).get(CameraCharacteristics.LENS_FACING)
                            == CameraMetadata.LENS_FACING_BACK) {
                        id = candidate;
                        break;
                    }
                }
                if (id == null) {
                    if (ids.isEmpty()) {
                        return null;
                    }
                    id = ids.get(0);
                }
            }
            final CameraCharacteristics characteristics = manager.getCameraCharacteristics(id);
            final StreamConfigurationMap streamMap =
                    characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (streamMap == null) {
                return null;
            }
            final android.util.Size[] sizes =
                    streamMap.getOutputSizes(android.graphics.SurfaceTexture.class);
            if (sizes == null || sizes.length == 0) {
                return null;
            }
            android.util.Size best = sizes[0];
            for (final android.util.Size size : sizes) {
                if ((long) size.getWidth() * size.getHeight()
                        > (long) best.getWidth() * best.getHeight()) {
                    best = size;
                }
            }
            int maxFps = 0;
            try {
                final long minFrameDurationNs = streamMap.getOutputMinFrameDuration(
                        android.graphics.SurfaceTexture.class, best);
                if (minFrameDurationNs > 0) {
                    maxFps = (int) Math.round(1e9 / minFrameDurationNs);
                }
            } catch (Exception ignored) {
                // getOutputMinFrameDuration() is not supported on all devices.
            }
            if (maxFps <= 0) {
                final Range<Integer>[] ranges = characteristics.get(
                        CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
                if (ranges != null) {
                    for (final Range<Integer> range : ranges) {
                        // Ranges are plain fps, except old Tegra2 devices that report x1000.
                        final int upper = range.getUpper();
                        maxFps = Math.max(maxFps, upper < 1000 ? upper : upper / 1000);
                    }
                }
            }
            return new int[] {best.getWidth(), best.getHeight(), maxFps};
        } catch (CameraAccessException e) {
            Logging.e(TAG, "maxCaptureFormat: " + e);
            return null;
        }
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
        runOnCameraThread(() -> setZoomOnCameraThread(zoom));
    }

    private void setZoomOnCameraThread(float zoom) {
        if (zoom > 1.0f) zoom = 1.0f;
        if (zoom < 0.0f) zoom = 0.0f;
        zoomLevel = zoom;

        if (!isZoomSupported()) {
            // Fork reports this as a camera error, which would kill the
            // stream - a warning is friendlier and loses nothing.
            Logging.w(TAG, "setZoom: zoom not supported on this camera.");
            return;
        }
        if (sessionRunning && captureRequestBuilder != null) {
            applyZoom();
            if (!updateRepeatingRequest()) {
                return;
            }
        }
        if (eventsListener != null) {
            eventsListener.onCameraZoomChanged(zoom);
        }
    }

    private boolean useZoomRatio() {
        return zoomRatioRange != null && zoomRatioRange.getUpper() > 1.0f;
    }

    private void applyZoom() {
        // API 30+: CONTROL_ZOOM_RATIO lets the HAL pick the lens (tele on
        // multi-lens phones) and wins over SCALER_CROP_REGION when both
        // are present, so only one mechanism is ever written.
        if (useZoomRatio()) {
            captureRequestBuilder.set(CaptureRequest.CONTROL_ZOOM_RATIO,
                    ZoomMath.zoomRatio(zoomLevel,
                            zoomRatioRange.getLower(), zoomRatioRange.getUpper()));
            return;
        }
        if (!isZoomSupported()) {
            return;
        }
        final Rect activeArray =
                cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        if (activeArray == null) {
            return;
        }
        final int[] crop = ZoomMath.cropRegion(
                activeArray.left, activeArray.top, activeArray.width(), activeArray.height(),
                zoomLevel, zoomMax);
        captureRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION,
                new Rect(crop[0], crop[1], crop[2], crop[3]));
    }

    @Override
    public float getZoom() {
        return zoomLevel;
    }

    @Override
    public boolean isZoomSupported() {
        return useZoomRatio() || zoomMax > 0.0f;
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
        zoomRatioRange = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                ? cameraCharacteristics.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE)
                : null;
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
            cameraManager.openCamera(cameraId,
                    new CameraStateCallback(++cameraOpenGeneration), cameraThreadHandler);
        } catch (CameraAccessException | IllegalArgumentException | SecurityException e) {
            reportError("Failed to open camera: " + e);
        }
    }

    private final class CameraStateCallback extends CameraDevice.StateCallback {
        private final int openGeneration;

        CameraStateCallback(final int openGeneration) {
            this.openGeneration = openGeneration;
        }

        @Override
        public void onOpened(final CameraDevice camera) {
            checkIsOnCameraThread();
            if (openGeneration <= staleCameraOpenGeneration) {
                // stopInternal() superseded this open while it was in flight -
                // drop the device instead of wiring a session behind a capturer
                // that already reported onCapturerStopped().
                Logging.d(TAG, "onOpened from a stopped camera open - closing it.");
                camera.close();
                return;
            }
            Logging.d(TAG, "Camera opened.");
            cameraDevice = camera;

            final Surface captureSurface;
            if (useYuvCapturePath) {
                imageReader = ImageReader.newInstance(captureFormat.width, captureFormat.height,
                        ImageFormat.YUV_420_888, 2);
                imageReader.setOnImageAvailableListener(yuvFrameListener, cameraThreadHandler);
                captureSurface = imageReader.getSurface();
            } else {
                surfaceTextureHelper.setTextureSize(captureFormat.width, captureFormat.height);
                captureSurface = new Surface(surfaceTextureHelper.getSurfaceTexture());
            }
            surface = captureSurface;
            try {
                camera.createCaptureSession(
                        Collections.singletonList(captureSurface),
                        new CaptureSessionStateCallback(openGeneration), cameraThreadHandler);
            } catch (CameraAccessException e) {
                reportError("Failed to create capture session. " + e);
            }
        }

        @Override
        public void onDisconnected(final CameraDevice camera) {
            checkIsOnCameraThread();
            if (camera != cameraDevice) {
                // Stale callback from the pre-switch device - ignore it.
                Logging.d(TAG, "onDisconnected from a closed camera device.");
                return;
            }
            Logging.w(TAG, "Camera disconnected.");
            failPendingSwitch("Camera disconnected.");
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
            if (camera != cameraDevice) {
                Logging.d(TAG, "onError from a closed camera device.");
                return;
            }
            reportError(cameraErrorDescription(errorCode));
        }

        @Override
        public void onClosed(final CameraDevice camera) {
            checkIsOnCameraThread();
            if (camera != cameraDevice && cameraDevice != null) {
                Logging.d(TAG, "onClosed from a stale camera device.");
                return;
            }
            Logging.d(TAG, "Camera device closed.");
            if (eventsListener != null) {
                eventsListener.onCameraClosed();
            }
        }
    }

    private final class CaptureSessionStateCallback extends CameraCaptureSession.StateCallback {
        private final int openGeneration;

        CaptureSessionStateCallback(final int openGeneration) {
            this.openGeneration = openGeneration;
        }

        @Override
        public void onConfigured(final CameraCaptureSession session) {
            checkIsOnCameraThread();
            Logging.d(TAG, "Camera capture session configured.");
            if (openGeneration <= staleCameraOpenGeneration || cameraDevice == null) {
                // A stop superseded this open before configure finished (the
                // camera device may already be closed and nulled) - drop the
                // session instead of starting capture / reporting started.
                session.close();
                return;
            }
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
                applyZoom();
                chooseStabilizationMode(captureRequestBuilder);
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
            if (!useYuvCapturePath) {
                startFrameListening();
            }
            framesSinceLastCheck = 0;
            freezePeriodCount = 0;
            freezeReported = false;
            cameraThreadHandler.postDelayed(freezeWatchdogRunnable, CAMERA_OBSERVER_PERIOD_MS);
            if (capturerObserver != null) {
                capturerObserver.onCapturerStarted(true);
            }
            finishPendingSwitch();
            Logging.d(TAG, "Camera device successfully started.");
        }

        @Override
        public void onConfigureFailed(final CameraCaptureSession session) {
            checkIsOnCameraThread();
            session.close();
            if (openGeneration <= staleCameraOpenGeneration) {
                // Aborted by a stop while configuring - not an error.
                return;
            }
            reportError("Failed to configure capture session.");
        }
    }

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

    // YUV fallback path (E1j): ImageReader frames -> NV21Buffer -> VideoFrame.
    // Front-camera mirror is not expressible in an NV21 buffer, so the mirror
    // is dropped on this path (rotation still reported on the VideoFrame).
    private final ImageReader.OnImageAvailableListener yuvFrameListener =
            new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(final ImageReader reader) {
        checkIsOnCameraThread();
        if (!sessionRunning) {
            return;
        }
        try (Image image = reader.acquireLatestImage()) {
            if (image == null) {
                return;
            }
            ++framesSinceLastCheck;
            if (!firstFrameReported) {
                firstFrameReported = true;
                if (eventsListener != null) {
                    eventsListener.onFirstFrameAvailable();
                }
            }
            final int width = image.getWidth();
            final int height = image.getHeight();
            if (nv21Data == null || nv21Data.length != width * height * 3 / 2) {
                nv21Data = new byte[width * height * 3 / 2];
            }
            yuv420ToNv21(image, nv21Data);
            final VideoFrame frame = new VideoFrame(
                    new NV21Buffer(nv21Data, width, height, null),
                    getFrameOrientation(), image.getTimestamp());
            capturerObserver.onFrameCaptured(frame);
            frame.release();
        } catch (IllegalStateException e) {
            Logging.e(TAG, "yuvFrameListener: " + e);
        }
        }
    };

    private static void yuv420ToNv21(final Image image, final byte[] nv21) {
        final int width = image.getWidth();
        final int height = image.getHeight();
        final int ySize = width * height;
        int offset = 0;

        final Image.Plane yPlane = image.getPlanes()[0];
        final java.nio.ByteBuffer yBuffer = yPlane.getBuffer();
        final int yRowStride = yPlane.getRowStride();
        if (yPlane.getPixelStride() == 1 && yRowStride == width) {
            yBuffer.get(nv21, 0, ySize);
        } else {
            for (int row = 0; row < height; ++row) {
                yBuffer.position(row * yRowStride);
                yBuffer.get(nv21, offset, width);
                offset += width;
            }
        }
        offset = ySize;

        final Image.Plane uPlane = image.getPlanes()[1];
        final Image.Plane vPlane = image.getPlanes()[2];
        final java.nio.ByteBuffer uBuffer = uPlane.getBuffer();
        final java.nio.ByteBuffer vBuffer = vPlane.getBuffer();
        final int uRowStride = uPlane.getRowStride();
        final int vRowStride = vPlane.getRowStride();
        final int uPixelStride = uPlane.getPixelStride();
        final int vPixelStride = vPlane.getPixelStride();
        final int chromaHeight = height / 2;
        final int chromaWidth = width / 2;
        for (int row = 0; row < chromaHeight; ++row) {
            for (int col = 0; col < chromaWidth; ++col) {
                nv21[offset++] = vBuffer.get(row * vRowStride + col * vPixelStride);
                nv21[offset++] = uBuffer.get(row * uRowStride + col * uPixelStride);
            }
        }
    }

    private void stopInternal() {
        checkIsOnCameraThread();
        // Any openCamera() call still in flight is now stale - its callbacks
        // check this cutoff and drop what the HAL hands them.
        staleCameraOpenGeneration = cameraOpenGeneration;
        sessionOpening = false;
        sessionRunning = false;
        cameraThreadHandler.removeCallbacks(freezeWatchdogRunnable);
        surfaceTextureHelper.stopListening();
        if (captureSession != null) {
            captureSession.close();
            captureSession = null;
        }
        captureRequestBuilder = null;
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
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
        failPendingSwitch(error);
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

    // Prefers optical stabilization over software stabilization if available.
    // Only enables one of the stabilization modes at a time because having both
    // enabled can cause strange results - same as the fork's Camera2Session.
    private void chooseStabilizationMode(final CaptureRequest.Builder requestBuilder) {
        final int[] availableOpticalStabilization = cameraCharacteristics.get(
                CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION);
        if (availableOpticalStabilization != null) {
            for (final int mode : availableOpticalStabilization) {
                if (mode == CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON) {
                    requestBuilder.set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
                            CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON);
                    requestBuilder.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                            CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_OFF);
                    Logging.d(TAG, "Using optical stabilization.");
                    return;
                }
            }
        }
        // If no optical mode is available, try software.
        final int[] availableVideoStabilization = cameraCharacteristics.get(
                CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES);
        if (availableVideoStabilization != null) {
            for (final int mode : availableVideoStabilization) {
                if (mode == CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON) {
                    requestBuilder.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                            CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_ON);
                    requestBuilder.set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
                            CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_OFF);
                    Logging.d(TAG, "Using video stabilization.");
                    return;
                }
            }
        }
        Logging.d(TAG, "Stabilization not available.");
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
        cameraIds = orderedCameraIds(context);
        try {
            for (final String id : cameraIds) {
                if (cameraManager.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING)
                        == CameraMetadata.LENS_FACING_BACK) {
                    return id;
                }
            }
            if (!cameraIds.isEmpty()) {
                return cameraIds.get(0);
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
