package com.o3dr.android.client.interfaces;

/**
 * Used to monitor the state of manual control for the vehicle.
 *
 * @since 2.6.9
 */
public interface ManualControlStateListener {
    /**
     * Manual control is toggled on the vehicle.
     * @param isEnabled True if manual control is enabled, false if disabled.
     */
    void onManualControlToggled(boolean isEnabled);
}
