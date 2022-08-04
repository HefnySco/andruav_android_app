package ap.andruav_ap.activities.settings;

import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.widget.Toast;

import com.andruav.Constants;

import ap.andruav_ap.R;

/**
 * Created by mhefny on 2/27/17.
 */

public class SettingsDrone extends PreferenceActivity {

    PreferenceActivity Me;
    protected EditTextPreference txtMobileNum;
    protected EditTextPreference txtCommModuleIP;
    protected EditTextPreference txtGCSBlockChannelNumber;
    protected EditTextPreference txtGCSBlockPMWMinValue;
    protected EditTextPreference txtRCCamChannelNumber;
    protected EditTextPreference txtRCCamPMWMinValue;
    protected EditTextPreference txtBatteryMinPercentage;
    protected CheckBoxPreference chkCommModuleIPAuto;
    protected CheckBoxPreference chkGPSInjection;


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        setupSimplePreferencesScreen();
    }

    @Override
    public boolean onIsMultiPane() {
        return false;
    }


    private PreferenceCategory addTitle(int title) {

        PreferenceCategory preferenceCategory = new PreferenceCategory(this);
        preferenceCategory.setTitle(title);
        getPreferenceScreen().addPreference(preferenceCategory);

        return preferenceCategory;
    }

    private void addSection(int sectionID) {
        addPreferencesFromResource(sectionID);

    }


    private void setupSimplePreferencesScreen() {


        Me = this;

        // In the simplified UI, fragments are not used at all and we instead
        // use the older PreferenceActivity APIs.

        // Add 'general' preferences.
        PreferenceCategory preferenceCategory;
        addPreferencesFromResource(R.xml.pref_general);

        // Add 'notifications' preferences, and a corresponding header.
        // preferenceCategory = addTitle(R.string.pref_gr_general_settings);
        //addSection(R.xml.pref_general);

        preferenceCategory = addTitle(R.string.pref_gr_fcb);
        addSection(R.xml.pref_drone_fcbsettings);


        preferenceCategory = addTitle(R.string.pref_gr_fpv_settings);
        addSection(R.xml.pref_drone_fpvsettings_noexternalcam);


        // Add 'notifications' preferences, and a corresponding header.
        preferenceCategory = addTitle(R.string.pref_gr_recovery);
        addSection(R.xml.pref_drone_systemrecovery);

        // Add 'data and sync' preferences, and a corresponding header.
        preferenceCategory = addTitle(R.string.pref_feedback_support);
        addSection(R.xml.feedback_support);

        //preferenceCategory = addTitle(R.string.pref_gr_advanced_settings);
        //addSection(R.xml.pref_advancedsettings);

        txtRCCamChannelNumber = (EditTextPreference) findPreference("sw_cam_rc_num");
        txtRCCamPMWMinValue = (EditTextPreference) findPreference("sw_cam_rc_pwm");
        txtMobileNum = (EditTextPreference) findPreference("ZB8vM05KAdg");
        txtGCSBlockChannelNumber = (EditTextPreference) findPreference("p7wCvhb2Akm");
        txtGCSBlockPMWMinValue = (EditTextPreference) findPreference("xokpINK9PECd");
        txtBatteryMinPercentage = (EditTextPreference) findPreference("WiDVQ");
        txtCommModuleIP = (EditTextPreference) findPreference("WXXG2IUCUUzrG1");
        chkCommModuleIPAuto = (CheckBoxPreference) findPreference("WSXG2IUCUUzrG1");
        chkGPSInjection = (CheckBoxPreference) findPreference("LNScs17Oks");

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            chkGPSInjection.setEnabled(false);
            ap.andruavmiddlelibrary.preference.Preference.isGPSInjecttionEnabled(null, false);
        }

        txtRCCamChannelNumber.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                int val = Integer.parseInt(newValue.toString());
                if ((val >= 1) && (val <= 18)) {

                    return true;
                } else {
                    // invalid you can show invalid message
                    Toast.makeText(getApplicationContext(), "bad channel number. choose from 1 to 16", Toast.LENGTH_LONG).show();
                    return false;
                }
            }
        });
        txtGCSBlockChannelNumber.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                int val = Integer.parseInt(newValue.toString());
                if ((val >= 1) && (val <= 16)) {

                    return true;
                } else {
                    // invalid you can show invalid message
                    Toast.makeText(getApplicationContext(), "bad channel number. choose from 1 to 16", Toast.LENGTH_LONG).show();
                    return false;
                }
            }
        });

        txtRCCamPMWMinValue.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                int val = Integer.parseInt(newValue.toString());
                if ((val >= Constants.Default_RC_MIN_VALUE) && (val <= Constants.Default_RC_MAX_VALUE)) {

                    return true;
                } else {
                    // invalid you can show invalid message
                    Toast.makeText(getApplicationContext(), "error range number in PWM", Toast.LENGTH_LONG).show();
                    return false;
                }
            }
        });

        txtGCSBlockPMWMinValue.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                int val = Integer.parseInt(newValue.toString());
                if ((val >= Constants.Default_RC_MIN_VALUE) && (val <= Constants.Default_RC_MAX_VALUE)) {

                    return true;
                } else {
                    // invalid you can show invalid message
                    Toast.makeText(getApplicationContext(), "error range number in PWM", Toast.LENGTH_LONG).show();
                    return false;
                }
            }
        });

        txtBatteryMinPercentage.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                int val = Integer.parseInt(newValue.toString());
                if ((val >= 0) && (val <= 100)) {

                    return true;
                } else {
                    // invalid you can show invalid message
                    Toast.makeText(getApplicationContext(), "BAttery percentage from 0% to 100%", Toast.LENGTH_LONG).show();
                    return false;
                }
            }
        });
    }


    @Override
    protected void onPause() {
        super.onPause();
    }
}