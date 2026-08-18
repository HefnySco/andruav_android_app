package ap.andruav_ap.activities.settings;

import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.CheckBoxPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.andruav.Constants;

import ap.andruav_ap.R;

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
    }
}
