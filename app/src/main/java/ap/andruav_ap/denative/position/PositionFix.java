package ap.andruav_ap.denative.position;

/**
 * Immutable snapshot of one phone location fix, Android-free so
 * {@link PositionSourceGate} and the 9102 builder stay unit-testable.
 * Accuracy/bearing fields use {@link Double#NaN} for "the phone did not
 * report this" - the same convention ControlBoard_DroneKit uses before
 * mapping NaN onto the protocol's unknown sentinels.
 */
public final class PositionFix {

    public final double latitude_deg;
    public final double longitude_deg;
    /** Best altitude estimate in metres (MSL where the API provides it,
     *  WGS84 ellipsoid otherwise). */
    public final double altitude_m;
    /** Horizontal accuracy radius in metres; NaN when unreported. */
    public final double horizontal_accuracy_m;
    /** Vertical accuracy in metres; NaN when unreported (pre-API 26). */
    public final double vertical_accuracy_m;
    /** Speed accuracy in metres/second; NaN when unreported (pre-API 26). */
    public final double speed_accuracy_mps;
    /** Bearing in degrees from true north; NaN when unreported. */
    public final double bearing_deg;
    /** Fix timestamp on the elapsed-realtime (monotonic) clock, ms. */
    public final long elapsed_realtime_ms;

    public PositionFix(double latitude_deg, double longitude_deg, double altitude_m,
                       double horizontal_accuracy_m, double vertical_accuracy_m,
                       double speed_accuracy_mps, double bearing_deg,
                       long elapsed_realtime_ms) {
        this.latitude_deg = latitude_deg;
        this.longitude_deg = longitude_deg;
        this.altitude_m = altitude_m;
        this.horizontal_accuracy_m = horizontal_accuracy_m;
        this.vertical_accuracy_m = vertical_accuracy_m;
        this.speed_accuracy_mps = speed_accuracy_mps;
        this.bearing_deg = bearing_deg;
        this.elapsed_realtime_ms = elapsed_realtime_ms;
    }

    public boolean hasHorizontalAccuracy() {
        return !Double.isNaN(horizontal_accuracy_m);
    }

    public boolean hasVerticalAccuracy() {
        return !Double.isNaN(vertical_accuracy_m);
    }

    public boolean hasSpeedAccuracy() {
        return !Double.isNaN(speed_accuracy_mps);
    }

    public boolean hasBearing() {
        return !Double.isNaN(bearing_deg);
    }
}
