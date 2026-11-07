package ap.andruav_ap.activities.phone;

import org.greenrobot.eventbus.Subscribe;

import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.andruav.AndruavSettings;
import com.andruav.andruavUnit.AndruavUnitBase;
import com.andruav.event.droneReport_Event.Event_GPS_Ready;
import com.andruav.event.droneReport_Event.Event_IMU_Ready;
import com.andruav.sensors.AndruavIMU;

import org.greenrobot.eventbus.EventBus;
import ap.andruav_ap.R;
import ap.andruav_ap.activities.AppThemes;
import ap.andruav_ap.activities.ThemeColors;
import ap.andruavmiddlelibrary.factory.util.DialogHelper;
import ap.andruavmiddlelibrary.preference.Preference;

/***
 * AN-0136 (TASK-115): GPS tab of the PHONE screen - the phone fix status
 * (fix pill, satellites, accuracy, fix age, lat/lon, and which source the
 * unit uses) plus the two Settings > FCB & Telemetry toggles: GPS Injection
 * (gps_inject) and Ignore Mobile Sensors (mePMWRUHZFwA). Phone GPS only
 * reaches GPS_INPUT while GPS Injection is on - the same gate Settings sets.
 */
public class PhoneGpsFragment extends Fragment {

    private TextView mTxtFix;
    private TextView mTxtSats;
    private TextView mTxtAcc;
    private TextView mTxtAge;
    private TextView mTxtLatLon;
    private TextView mTxtSource;
    private TextView mTglInject;
    private TextView mTglIgnore;

    private Handler mhandle;

    //////////BUS EVENT

    @Subscribe
    public void onEvent(final Event_GPS_Ready a7adath_gps_ready) {
        if (!isOwnUnit(a7adath_gps_ready.mAndruavWe7da)) return;
        mhandle.post(this::updateGps);
    }

    @Subscribe
    public void onEvent(final Event_IMU_Ready a7adath_imu_ready) {
        if (!isOwnUnit(a7adath_imu_ready.mAndruavWe7da)) return;
        mhandle.post(this::updateGps);
    }

    //////////EOF BUS EVENT

    private static boolean isOwnUnit(final AndruavUnitBase unit) {
        return (unit != null)
            && (AndruavSettings.andruavWe7daBase != null)
            && AndruavSettings.andruavWe7daBase.Equals(unit);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_phone_gps, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mhandle = new Handler();

        mTxtFix = view.findViewById(R.id.phone_gps_fix);
        mTxtSats = view.findViewById(R.id.phone_gps_sats);
        mTxtAcc = view.findViewById(R.id.phone_gps_acc);
        mTxtAge = view.findViewById(R.id.phone_gps_age);
        mTxtLatLon = view.findViewById(R.id.phone_gps_latlon);
        mTxtSource = view.findViewById(R.id.phone_gps_source);
        mTglInject = view.findViewById(R.id.phone_gps_inject_toggle);
        mTglIgnore = view.findViewById(R.id.phone_gps_ignore_toggle);

        mTglInject.setOnClickListener(v -> {
            Preference.isGPSInjecttionEnabled(null, !Preference.isGPSInjecttionEnabled(null));
            updateToggles();
        });
        mTglIgnore.setOnClickListener(v -> {
            Preference.isMobileSensorsDisabled(null, !Preference.isMobileSensorsDisabled(null));
            updateToggles();
            /* AN-0143: this flag is read when SensorService initializes -
             * a running app keeps the old state, so tell the user. GPS
             * Injection above applies live and needs no notice. */
            DialogHelper.doModalDialog(requireContext(),
                    getString(R.string.phone_gps_restart_title),
                    getString(R.string.phone_gps_restart_msg), null);
        });

        updateToggles();
        updateGps();
    }

    private void updateGps() {
        if (AndruavSettings.andruavWe7daBase == null) return;
        final AndruavUnitBase unit = AndruavSettings.andruavWe7daBase;
        final AndruavIMU phone = unit.getMobileGPS();
        final Location loc = phone.getCurrentLocation();

        // fix pill: 1 = no fix, 2 = 2D, >=3 = 3D (same readout the old widget used)
        if (phone.GPS3DFix >= 3) {
            mTxtFix.setText(R.string.phone_gps_fix_3d);
            mTxtFix.setBackgroundResource(R.drawable.bg_home_tile_green);
            mTxtFix.setTextColor(ThemeColors.get(requireContext(), R.attr.deGood));
        } else if (phone.GPS3DFix == 2) {
            mTxtFix.setText(R.string.phone_gps_fix_2d);
            mTxtFix.setBackgroundResource(R.drawable.bg_home_tile_blue);
            mTxtFix.setTextColor(ThemeColors.get(requireContext(), R.attr.deBlueIcon));
        } else {
            mTxtFix.setText(R.string.phone_gps_fix_nofix);
            mTxtFix.setBackgroundResource(R.drawable.bg_home_tile_gray);
            mTxtFix.setTextColor(ThemeColors.get(requireContext(), R.attr.deIconGray));
        }

        mTxtSats.setText(String.valueOf(phone.SATC));
        mTxtAcc.setText((loc != null && loc.hasAccuracy())
                ? String.format("%.1f", loc.getAccuracy()) : "-");
        mTxtAge.setText(loc != null
                ? String.valueOf(Math.max(0L, (System.currentTimeMillis() - loc.getTime()) / 1000L))
                : "-");
        mTxtLatLon.setText(loc != null
                ? String.format("%.4f · %.4f", loc.getLatitude(), loc.getLongitude())
                : getString(R.string.phone_status_na));

        // which source the unit reports: board gps when FCB IMU feeds it
        final boolean board = unit.getActiveGPS().getUseFCBIMU();
        mTxtSource.setText(board ? R.string.phone_gps_source_board : R.string.phone_gps_source_phone);
        mTxtSource.setTextColor(ThemeColors.get(requireContext(),
                board ? R.attr.deBadText : R.attr.deGood));
    }

    private void updateToggles() {
        applyToggle(mTglInject, Preference.isGPSInjecttionEnabled(null));
        applyToggle(mTglIgnore, Preference.isMobileSensorsDisabled(null));
    }

    private void applyToggle(final TextView toggle, final boolean on) {
        toggle.setText(AppThemes.switchLabel(requireContext(), on));
        toggle.setBackgroundResource(on
                ? R.drawable.bg_home_bt_toggle_on : R.drawable.bg_home_bt_toggle_off);
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
        updateToggles();
        updateGps();
    }

    @Override
    public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mhandle != null) {
            mhandle.removeCallbacksAndMessages(null);
            mhandle = null;
        }
    }
}
