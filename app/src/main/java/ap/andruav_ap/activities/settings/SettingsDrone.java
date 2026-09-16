package ap.andruav_ap.activities.settings;

import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.CheckBoxPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.andruav.AndruavSettings;
import com.andruav.Constants;
import com.andruav.controlBoard.ControlBoardBase;

import ap.andruav_ap.R;
import ap.andruav_ap.communication.controlBoard.ControlBoard_DroneKit;

/**
 * Created by mhefny on 2/27/17.
 * <p>
 * Migrated from the deprecated {@code android.preference.PreferenceActivity} to
 * {@link AppCompatActivity} + {@link PreferenceFragmentCompat} (androidx.preference).
 */
public class SettingsDrone extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(android.R.id.content, new SettingsDroneFragment())
                    .commit();
        }
    }


    /**
     * Preference fragment that hosts all drone-related settings.
     * Loads {@code R.xml.pref_drone_root} which consolidates the previously
     * separate preference XMLs (general, fcb, fpv, recovery, feedback) into
     * a single resource with {@code PreferenceCategory} sections.
     */
    public static class SettingsDroneFragment extends PreferenceFragmentCompat {

        private EditTextPreference txtMobileNum;
        private EditTextPreference txtGCSBlockChannelNumber;
        private EditTextPreference txtGCSBlockPMWMinValue;
        private EditTextPreference txtRCCamChannelNumber;
        private EditTextPreference txtRCCamPMWMinValue;
        private EditTextPreference txtBatteryMinPercentage;
        private CheckBoxPreference chkGPSInjection;
        private CheckBoxPreference chkIgnoreMobileSensors;
        private CheckBoxPreference chkNtripEnable;
        private EditTextPreference txtNtripPort;

        // The static XML summary ("Inject mobile GPS into FCB.") - kept so the live FC-state line
        // added in refreshGPSInjectionStatus() prepends to it instead of replacing it outright.
        private CharSequence baseGPSInjectionSummary;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.pref_drone_root, rootKey);

            txtRCCamChannelNumber = findPreference("sw_cam_rc_num");
            txtRCCamPMWMinValue = findPreference("sw_cam_rc_pwm");
            txtMobileNum = findPreference("key_mobile_recovery");
            txtGCSBlockChannelNumber = findPreference("key_block_channel");
            txtGCSBlockPMWMinValue = findPreference("key_block_pwm_min");
            txtBatteryMinPercentage = findPreference("WiDVQ");
            chkGPSInjection = findPreference("gps_inject");
            chkIgnoreMobileSensors = findPreference("mePMWRUHZFwA");
            chkNtripEnable = findPreference("ntrip_enable");
            txtNtripPort = findPreference("ntrip_port");
            baseGPSInjectionSummary = chkGPSInjection.getSummary();

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                chkGPSInjection.setEnabled(false);
                ap.andruavmiddlelibrary.preference.Preference.isGPSInjecttionEnabled(null, false);
            }

            // GPS Injection relies on the phone's own GPS sensor to feed the FC, so it cannot be
            // combined with "Ignore Mobile Sensors" (which keeps the phone GPS/IMU switched off).
            chkGPSInjection.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (Boolean.TRUE.equals(newValue) && chkIgnoreMobileSensors.isChecked()) {
                        Toast.makeText(getContext(), "Disable 'Ignore Mobile Sensors' first to enable GPS Injection.", Toast.LENGTH_LONG).show();
                        return false;
                    }
                    return true;
                }
            });

            chkIgnoreMobileSensors.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (Boolean.TRUE.equals(newValue) && chkGPSInjection.isChecked()) {
                        Toast.makeText(getContext(), "Disable 'GPS Injection' first to ignore mobile sensors.", Toast.LENGTH_LONG).show();
                        return false;
                    }
                    return true;
                }
            });

            // NTRIP needs at least a host and mountpoint before it can do anything - refuse the
            // toggle otherwise, exactly like the GPS-injection conflict guard above.
            chkNtripEnable.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (Boolean.TRUE.equals(newValue)) {
                        final String host = ap.andruavmiddlelibrary.preference.Preference.getNtripHost(null);
                        final String mountPoint = ap.andruavmiddlelibrary.preference.Preference.getNtripMountPoint(null);
                        if (host.isEmpty() || mountPoint.isEmpty()) {
                            Toast.makeText(getContext(), "Set NTRIP caster host and mountpoint first.", Toast.LENGTH_LONG).show();
                            return false;
                        }
                    }
                    return true;
                }
            });

            txtNtripPort.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    int val;
                    try {
                        val = Integer.parseInt(newValue.toString());
                    } catch (NumberFormatException e) {
                        Toast.makeText(getContext(), "NTRIP port must be a number from 1 to 65535", Toast.LENGTH_LONG).show();
                        return false;
                    }
                    if ((val >= 1) && (val <= 65535)) {
                        return true;
                    } else {
                        Toast.makeText(getContext(), "NTRIP port must be a number from 1 to 65535", Toast.LENGTH_LONG).show();
                        return false;
                    }
                }
            });

            txtRCCamChannelNumber.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    int val;
                    try {
                        val = Integer.parseInt(newValue.toString());
                    } catch (NumberFormatException e) {
                        Toast.makeText(getContext(), "bad channel number. choose from 1 to 18", Toast.LENGTH_LONG).show();
                        return false;
                    }
                    if ((val >= 1) && (val <= 18)) {
                        return true;
                    } else {
                        Toast.makeText(getContext(), "bad channel number. choose from 1 to 18", Toast.LENGTH_LONG).show();
                        return false;
                    }
                }
            });
            txtGCSBlockChannelNumber.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    int val;
                    try {
                        val = Integer.parseInt(newValue.toString());
                    } catch (NumberFormatException e) {
                        Toast.makeText(getContext(), "bad channel number. choose from 1 to 16", Toast.LENGTH_LONG).show();
                        return false;
                    }
                    if ((val >= 1) && (val <= 16)) {
                        return true;
                    } else {
                        Toast.makeText(getContext(), "bad channel number. choose from 1 to 16", Toast.LENGTH_LONG).show();
                        return false;
                    }
                }
            });

            txtRCCamPMWMinValue.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    int val;
                    try {
                        val = Integer.parseInt(newValue.toString());
                    } catch (NumberFormatException e) {
                        Toast.makeText(getContext(), "error range number in PWM", Toast.LENGTH_LONG).show();
                        return false;
                    }
                    if ((val >= Constants.Default_RC_MIN_VALUE) && (val <= Constants.Default_RC_MAX_VALUE)) {
                        return true;
                    } else {
                        Toast.makeText(getContext(), "error range number in PWM", Toast.LENGTH_LONG).show();
                        return false;
                    }
                }
            });
            txtGCSBlockPMWMinValue.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    int val;
                    try {
                        val = Integer.parseInt(newValue.toString());
                    } catch (NumberFormatException e) {
                        Toast.makeText(getContext(), "error range number in PWM", Toast.LENGTH_LONG).show();
                        return false;
                    }
                    if ((val >= Constants.Default_RC_MIN_VALUE) && (val <= Constants.Default_RC_MAX_VALUE)) {
                        return true;
                    } else {
                        Toast.makeText(getContext(), "error range number in PWM", Toast.LENGTH_LONG).show();
                        return false;
                    }
                }
            });

            txtBatteryMinPercentage.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    int val;
                    try {
                        val = Integer.parseInt(newValue.toString());
                    } catch (NumberFormatException e) {
                        Toast.makeText(getContext(), "Battery percentage from 0% to 100%", Toast.LENGTH_LONG).show();
                        return false;
                    }
                    if ((val >= 0) && (val <= 100)) {
                        return true;
                    } else {
                        Toast.makeText(getContext(), "Battery percentage from 0% to 100%", Toast.LENGTH_LONG).show();
                        return false;
                    }
                }
            });
        }

        @Override
        public void onResume() {
            super.onResume();
            refreshGPSInjectionStatus();
        }

        /***
         * Appends the flight controller's actual GPS_TYPE state to the GPS-injection preference's
         * summary, so enabling the checkbox is never a blind toggle: a user should not be able to
         * turn this on, see nothing happen once connected, and have no way to tell why.
         * Re-evaluated in onResume() - the common flow is connect first, then open Settings, and
         * a fragment already on screen when the connection completes will show the next time it
         * regains focus (e.g. the user backs out of a connect dialog).
         */
        private void refreshGPSInjectionStatus() {
            if (chkGPSInjection == null) return;

            final ControlBoardBase fcBoard = AndruavSettings.andruavWe7daBase.FCBoard;
            if (!(fcBoard instanceof ControlBoard_DroneKit)) {
                chkGPSInjection.setSummary(getString(
                        ap.andruavmiddlelibrary.R.string.pref_gr_fcb_gps_injection_status_not_connected, baseGPSInjectionSummary));
                return;
            }

            final ControlBoard_DroneKit droneKitBoard = (ControlBoard_DroneKit) fcBoard;
            if (!droneKitBoard.hasReceivedGPSTypeParams()) {
                chkGPSInjection.setSummary(getString(
                        ap.andruavmiddlelibrary.R.string.pref_gr_fcb_gps_injection_status_checking, baseGPSInjectionSummary));
                return;
            }

            if (droneKitBoard.isFCConfiguredForGPSInjection()) {
                final int mavSlot = (droneKitBoard.getGPS1_Type() == ControlBoard_DroneKit.GPS_TYPE_MAV) ? 1 : 2;
                chkGPSInjection.setSummary(getString(
                        ap.andruavmiddlelibrary.R.string.pref_gr_fcb_gps_injection_status_ready, baseGPSInjectionSummary, mavSlot));
            } else {
                chkGPSInjection.setSummary(getString(
                        ap.andruavmiddlelibrary.R.string.pref_gr_fcb_gps_injection_status_not_ready, baseGPSInjectionSummary,
                        droneKitBoard.getGPS1_Type(), droneKitBoard.getGPS2_Type()));
            }
        }
    }
}
