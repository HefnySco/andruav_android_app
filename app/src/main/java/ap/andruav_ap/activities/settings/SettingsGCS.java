package ap.andruav_ap.activities.settings;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;

import ap.andruav_ap.R;

/**
 * Created by mhefny on 2/27/17.
 */

public class SettingsGCS extends PreferenceActivity {

    PreferenceActivity Me;


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        setupSimplePreferencesScreen();
    }

    @Override
    public boolean onIsMultiPane() {
        return false;
    }



    private PreferenceCategory addTitle (int title )
    {

        PreferenceCategory preferenceCategory = new PreferenceCategory(this);
        preferenceCategory.setTitle(title);
        getPreferenceScreen().addPreference(preferenceCategory);

        return preferenceCategory;
    }

    private void addSection (int sectionID )
    {
        addPreferencesFromResource(sectionID);

    }


    private void setupSimplePreferencesScreen() {


        Me = this;

        // In the simplified UI, fragments are not used at all and we instead
        // use the older PreferenceActivity APIs.

        // Add 'general' preferences.
        PreferenceCategory preferenceCategory;
        addPreferencesFromResource(R.xml.pref_general);


        preferenceCategory = addTitle(R.string.pref_gr_fcb);
        addSection(R.xml.pref_gcs_fcbsettings);


        preferenceCategory = addTitle(R.string.pref_gr_fpv_settings);
        addSection(R.xml.pref_gcs_fpvsettings_noexternalcam);



       // Add 'data and sync' preferences, and a corresponding header.
        preferenceCategory = addTitle(R.string.pref_feedback_support);
        addSection(R.xml.feedback_support);

        preferenceCategory = addTitle(R.string.pref_gr_advanced_settings);
        //addSection(R.xml.pref_advancedsettings);




    }
}
