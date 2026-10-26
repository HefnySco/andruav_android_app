package ap.andruav_ap.services.fpv;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.andruav.AndruavEngine;

/**
 * Headless single-shot still capture: opens a Camera2 device with an
 * ImageReader(JPEG) surface only - no preview Surface, no Activity, works
 * with the screen off as long as the process holds camera eligibility
 * (foreground, or a running foreground service of type camera). One call =
 * open -> one TEMPLATE_STILL_CAPTURE -> JPEG bytes -> close, so the device
 * is never held between bursts.
 *
 * Everything runs on a private HandlerThread; {@link #captureStill} blocks
 * the caller (the CameraController burst worker) until the JPEG arrives,
 * the camera fails, or the timeout hits.
 */
final class Camera2StillCapture {

    /** Result of one headless still attempt: exactly one of jpeg/err is set. */
    static final class Outcome {
        byte[] jpeg;
        String err;
    }

    private Camera2StillCapture() {}

    /**
     * Take one JPEG still from {@code cameraId}. Blocks up to {@code timeoutMs}.
     *
     * @param zoomLevel linear zoom factor >= 1.0 mapped onto
     *                  SCALER_CROP_REGION (clamped to the device's max
     *                  digital zoom); <= 1.0 means full sensor area.
     */
    @SuppressLint("MissingPermission") // permission is checked inside
    static Outcome captureStill(final Context context, final String cameraId,
                                final double zoomLevel, final long timeoutMs) {
        final Outcome outcome = new Outcome();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            outcome.err = "camera2 requires API 21";
            return outcome;
        }
        if (context == null) {
            outcome.err = "no context";
            return outcome;
        }
        if (context.checkSelfPermission(android.Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            outcome.err = "CAMERA permission not granted";
            return outcome;
        }

        final CameraManager cameraManager =
                (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        if (cameraManager == null) {
            outcome.err = "no CameraManager";
            return outcome;
        }

        final CameraCharacteristics characteristics;
        try {
            characteristics = cameraManager.getCameraCharacteristics(cameraId);
        } catch (Exception e) {
            outcome.err = "cameraId '" + cameraId + "': " + e.getMessage();
            return outcome;
        }

        final Size jpegSize = pickJpegSize(characteristics);
        if (jpegSize == null) {
            outcome.err = "camera '" + cameraId + "' exposes no JPEG sizes";
            return outcome;
        }

        final CountDownLatch done = new CountDownLatch(1);
        final AtomicBoolean fired = new AtomicBoolean(false);
        final HandlerThread thread = new HandlerThread("camera2-still");
        thread.start();
        final Handler handler = new Handler(thread.getLooper());

        // Everything below may be torn down from any of the callbacks; keep
        // references in finals and funnel all ends through finish().
        final ImageReader[] reader = new ImageReader[1];
        final CameraDevice[] device = new CameraDevice[1];
        final CameraCaptureSession[] session = new CameraCaptureSession[1];

        final Runnable finish = () -> {
            try { if (session[0] != null) session[0].close(); } catch (Exception ignored) {}
            try { if (device[0] != null) device[0].close(); } catch (Exception ignored) {}
            try { if (reader[0] != null) reader[0].close(); } catch (Exception ignored) {}
            thread.quitSafely();
            done.countDown();
        };

        final Runnable succeed = () -> finish.run();
        final Runnable fail = () -> finish.run();

        final Runnable watchdog = () -> {
            if (fired.compareAndSet(false, true)) {
                outcome.err = "still capture timed out on camera '" + cameraId + "'";
                fail.run();
            }
        };
        handler.postDelayed(watchdog, timeoutMs);

        try {
            cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice cameraDevice) {
                    device[0] = cameraDevice;
                    reader[0] = ImageReader.newInstance(
                            jpegSize.getWidth(), jpegSize.getHeight(), ImageFormat.JPEG, 2);
                    reader[0].setOnImageAvailableListener(imageReader -> {
                        final Image image = imageReader.acquireLatestImage();
                        if (image == null) return;
                        try {
                            final Image.Plane plane = image.getPlanes()[0];
                            final ByteBuffer buffer = plane.getBuffer();
                            final byte[] jpeg = new byte[buffer.remaining()];
                            buffer.get(jpeg);
                            outcome.jpeg = jpeg;
                        } finally {
                            image.close();
                        }
                        if (fired.compareAndSet(false, true)) {
                            handler.removeCallbacks(watchdog);
                            handler.post(succeed);
                        }
                    }, handler);

                    try {
                        cameraDevice.createCaptureSession(
                                Collections.singletonList(reader[0].getSurface()),
                                new CameraCaptureSession.StateCallback() {
                                    @Override
                                    public void onConfigured(@NonNull CameraCaptureSession s) {
                                        session[0] = s;
                                        try {
                                            final CaptureRequest.Builder request =
                                                    cameraDevice.createCaptureRequest(
                                                            CameraDevice.TEMPLATE_STILL_CAPTURE);
                                            request.addTarget(reader[0].getSurface());
                                            request.set(CaptureRequest.CONTROL_MODE,
                                                    CaptureRequest.CONTROL_MODE_AUTO);
                                            if (hasAf(characteristics)) {
                                                request.set(CaptureRequest.CONTROL_AF_MODE,
                                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                            }
                                            final Integer orientation = characteristics.get(
                                                    CameraCharacteristics.SENSOR_ORIENTATION);
                                            request.set(CaptureRequest.JPEG_ORIENTATION,
                                                    orientation != null ? orientation : 0);
                                            final Rect crop = zoomCropRegion(characteristics, zoomLevel);
                                            if (crop != null) {
                                                request.set(CaptureRequest.SCALER_CROP_REGION, crop);
                                            }
                                            s.capture(request.build(), null, handler);
                                        } catch (Exception e) {
                                            if (fired.compareAndSet(false, true)) {
                                                outcome.err = "capture request failed: " + e.getMessage();
                                                fail.run();
                                            }
                                        }
                                    }

                                    @Override
                                    public void onConfigureFailed(@NonNull CameraCaptureSession s) {
                                        if (fired.compareAndSet(false, true)) {
                                            outcome.err = "capture session configure failed";
                                            fail.run();
                                        }
                                    }
                                }, handler);
                    } catch (Exception e) {
                        if (fired.compareAndSet(false, true)) {
                            outcome.err = "createCaptureSession failed: " + e.getMessage();
                            fail.run();
                        }
                    }
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice cameraDevice) {
                    if (fired.compareAndSet(false, true)) {
                        outcome.err = "camera '" + cameraId + "' disconnected";
                        fail.run();
                    }
                }

                @Override
                public void onError(@NonNull CameraDevice cameraDevice, int error) {
                    if (fired.compareAndSet(false, true)) {
                        outcome.err = "camera '" + cameraId + "' error " + error;
                        fail.run();
                    }
                }
            }, handler);
        } catch (SecurityException e) {
            // App is in the background without camera eligibility (API 34):
            // the caller surfaces this as a failed shot.
            handler.removeCallbacks(watchdog);
            thread.quitSafely();
            outcome.err = "camera access denied in background: " + e.getMessage();
            return outcome;
        } catch (CameraAccessException e) {
            handler.removeCallbacks(watchdog);
            thread.quitSafely();
            outcome.err = "camera '" + cameraId + "' open failed: " + e.getMessage();
            return outcome;
        }

        try {
            // The watchdog also counts down the latch, so this await is the
            // only timeout gate we need; keep a small grace on top.
            done.await(timeoutMs + 1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (outcome.err != null) {
            AndruavEngine.log().log2("camera2", "still-capture", outcome.err);
        }
        return outcome;
    }

    private static Size pickJpegSize(final CameraCharacteristics characteristics) {
        final android.hardware.camera2.params.StreamConfigurationMap map =
                characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if (map == null) return null;
        final Size[] sizes = map.getOutputSizes(ImageFormat.JPEG);
        if (sizes == null || sizes.length == 0) return null;
        Size best = sizes[0];
        for (final Size size : sizes) {
            if ((long) size.getWidth() * size.getHeight()
                    > (long) best.getWidth() * best.getHeight()) {
                best = size;
            }
        }
        return best;
    }

    private static boolean hasAf(final CameraCharacteristics characteristics) {
        final int[] modes = characteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
        if (modes == null) return false;
        for (final int mode : modes) {
            if (mode == CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE) return true;
        }
        return false;
    }

    /** Center-crop region for a linear zoom level, or null when level <= 1. */
    private static Rect zoomCropRegion(final CameraCharacteristics characteristics,
                                       final double zoomLevel) {
        if (zoomLevel <= 1.0) return null;
        final Rect sensor = characteristics.get(
                CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        if (sensor == null) return null;
        final Float maxZoom = characteristics.get(
                CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
        final double effective = Math.min(zoomLevel, maxZoom != null ? maxZoom : zoomLevel);
        if (effective <= 1.0) return null;
        final int halfW = (int) (sensor.width() / (2.0 * effective));
        final int halfH = (int) (sensor.height() / (2.0 * effective));
        final int cx = sensor.centerX();
        final int cy = sensor.centerY();
        return new Rect(cx - halfW, cy - halfH, cx + halfW, cy + halfH);
    }
}
