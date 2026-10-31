package ap.andruavmiddlelibrary.webrtc.camera;

/**
 * Pure zoom math for the app capturer - deliberately free of Android types so
 * it is unit-testable on the JVM. Produces the SCALER_CROP_REGION values as
 * ints; the caller wraps them in android.graphics.Rect.
 *
 * Same idea as the fork's Camera2Session.setZoomPrivate (centered crop inside
 * the sensor active array), one deliberate fix: the fork computes
 * zoomFactor = zoom * zoomMax, which divides by zero at zoom 0. Here the
 * fraction maps onto a real zoom factor of 1..zoomMax, so 0 means "no zoom"
 * (full active array) and 1 means the camera's maximum digital zoom.
 */
public final class ZoomMath {

    private ZoomMath() {}

    /**
     * @param activeLeft/Top/Width/Height the sensor active array
     *        (SENSOR_INFO_ACTIVE_ARRAY_SIZE) - not assumed to start at (0,0).
     * @param zoomFraction 0..1 of the camera's max zoom; out-of-range values
     *        are clamped.
     * @param zoomMax SCALER_AVAILABLE_MAX_DIGITAL_ZOOM (1.0 = no zoom).
     * @return int[]{left, top, right, bottom} crop region, centered on the
     *         active array.
     */
    public static int[] cropRegion(final int activeLeft, final int activeTop,
                                   final int activeWidth, final int activeHeight,
                                   final float zoomFraction, final float zoomMax) {
        float zoom = zoomFraction;
        if (zoom > 1.0f) zoom = 1.0f;
        if (zoom < 0.0f) zoom = 0.0f;

        float zoomFactor = 1.0f + zoom * (zoomMax - 1.0f);
        if (zoomFactor < 1.0f) zoomFactor = 1.0f;

        final int centerX = activeLeft + activeWidth / 2;
        final int centerY = activeTop + activeHeight / 2;
        final int halfWidth = (int) (activeWidth / (2.0f * zoomFactor));
        final int halfHeight = (int) (activeHeight / (2.0f * zoomFactor));

        return new int[] {
                centerX - halfWidth,
                centerY - halfHeight,
                centerX + halfWidth,
                centerY + halfHeight,
        };
    }

    /**
     * Maps the same 0..1 zoom fraction onto a CONTROL_ZOOM_RATIO value
     * (CaptureRequest key, API 30+). Fraction 0 stays at 1x - parity with
     * the crop path even on devices whose range dips below 1 (ultrawide
     * lens) - and fraction 1 reaches the camera's advertised maximum,
     * which on multi-lens devices is where the tele lens takes over.
     * The result is clamped into the camera's CONTROL_ZOOM_RATIO_RANGE.
     *
     * @param zoomFraction 0..1 of the camera's max zoom; out-of-range
     *        values are clamped.
     * @param minRatio lower end of CONTROL_ZOOM_RATIO_RANGE.
     * @param maxRatio upper end of CONTROL_ZOOM_RATIO_RANGE.
     */
    public static float zoomRatio(final float zoomFraction,
                                  final float minRatio, final float maxRatio) {
        float zoom = zoomFraction;
        if (zoom > 1.0f) zoom = 1.0f;
        if (zoom < 0.0f) zoom = 0.0f;

        float ratio = 1.0f + zoom * (maxRatio - 1.0f);
        if (ratio < minRatio) ratio = minRatio;
        if (ratio > maxRatio) ratio = maxRatio;
        return ratio;
    }
}
