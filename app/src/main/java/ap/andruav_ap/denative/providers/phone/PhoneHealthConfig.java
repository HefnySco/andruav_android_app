package ap.andruav_ap.denative.providers.phone;

import org.json.JSONObject;

/**
 * Thresholds for the phone-health tracker (Mission-Planner-Capabilities-TOBE
 * section 5.6: "Thresholds in config; edge-only with hysteresis").
 * Defaults match the spec: battery_low at 20 % clearing at 25 %, overheat
 * at 45 C clearing at 42 C. fromJson() accepts a config overlay:
 * {"battery_low":20,"battery_low_clear":25,"overheat_c":45,
 *  "overheat_clear_c":42,"rssi_deadband_dbm":2}
 */
public class PhoneHealthConfig {

    /** battery_low fires when the level drops to this percent or below. */
    public int battery_low_on_percent = 20;
    /** the battery_low latch clears when the level climbs back to this. */
    public int battery_low_off_percent = 25;
    /** overheat fires when the battery temperature reaches this (C). */
    public double overheat_on_c = 45.0;
    /** the overheat latch clears when the temperature drops to this (C). */
    public double overheat_off_c = 42.0;
    /** rssi_dbm republishes only after drifting at least this much (dBm),
     *  so signal jitter does not churn the state feed. */
    public double rssi_deadband_dbm = 2.0;

    public static PhoneHealthConfig defaults() {
        return new PhoneHealthConfig();
    }

    /** Defaults overlaid with a "phone_health" config object (may be null). */
    public static PhoneHealthConfig fromJson(JSONObject config) {
        PhoneHealthConfig result = defaults();
        if (config == null) return result;
        result.battery_low_on_percent = config.optInt("battery_low", result.battery_low_on_percent);
        result.battery_low_off_percent = config.optInt("battery_low_clear", result.battery_low_off_percent);
        result.overheat_on_c = config.optDouble("overheat_c", result.overheat_on_c);
        result.overheat_off_c = config.optDouble("overheat_clear_c", result.overheat_off_c);
        result.rssi_deadband_dbm = config.optDouble("rssi_deadband_dbm", result.rssi_deadband_dbm);
        return result;
    }
}
