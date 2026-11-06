package ap.andruav_ap.activities.phone;

import org.greenrobot.eventbus.Subscribe;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.andruav.AndruavEngine;
import com.andruav.AndruavSettings;
import com.andruav.Constants;
import com.andruav.andruavUnit.AndruavUnitBase;
import com.andruav.controlBoard.shared.common.FlightMode;
import com.andruav.event.droneReport_Event.Event_Battery_Ready;
import com.andruav.event.droneReport_Event.Event_FCB_Changed;
import com.andruav.event.droneReport_Event.Event_GCSBlockedChanged;
import com.andruav.event.droneReport_Event.Event_GPS_Ready;
import com.andruav.event.droneReport_Event.Event_IMU_Ready;
import com.andruav.event.droneReport_Event.Event_Vehicle_Mode_Changed;
import com.andruav.sensors.AndruavBattery;
import com.andruav.sensors.AndruavIMU;

import org.greenrobot.eventbus.EventBus;
import ap.andruav_ap.App;
import ap.andruav_ap.R;
import ap.andruav_ap.activities.ThemeColors;
import ap.andruavmiddlelibrary.factory.math.UnitConversion;
import ap.andruavmiddlelibrary.preference.Preference;
import ap.andruavmiddlelibrary.sensors._7asasatEvents.Event_IMU_CMD;
import ap.andruavmiddlelibrary.sensors.Sensor_Accelerometer;
import ap.andruavmiddlelibrary.sensors.Sensor_Gyro;
import ap.andruavmiddlelibrary.sensors.Sensor_Mag;

/***
 * AN-0134 (TASK-113): IMU tab of the PHONE screen - the IMUShasha logic
 * (attitude, Calibrate, direction, Zero Tilt, phone and FCB battery, flight
 * mode, speed/altitude, raw ACC/GYRO/MAG) presented as home-style cards.
 * Phone IMU P/R/Y are radians (Vector3d/getOrientation); FCB IMU is degrees -
 * the values here come from LastEvent_IMU only, so they are converted.
 */
public class PhoneImuFragment extends Fragment {

    private TextView mTxtRoll;
    private TextView mTxtPitch;
    private TextView mTxtHeading;
    private TextView mBtnCalibrate;
    private TextView mBtnDirection;
    private TextView mBtnZeroTilt;
    private TextView mTxtPhoneBattery;
    private TextView mTxtFcbBattery;
    private TextView mTxtFlightMode;
    private TextView mTxtSpeedAlt;
    private TextView mTxtAcc;
    private TextView mTxtGyro;
    private TextView mTxtMag;

    private Handler mhandle;

    ////// Sensor Variables (calibration only)
    private SensorManager mSensorManager;
    private Sensor_Accelerometer mEventAcc;
    private Sensor_Mag mEventMag;
    private Sensor_Gyro mEventGyro;
    ////// EOF Sensor Variables

    private ProgressDialog mprogressDialog;

    //////////BUS EVENT

    @Subscribe
    public void onEvent(final Event_IMU_Ready a7adath_imu_ready) {
        if (!isOwnUnit(a7adath_imu_ready.mAndruavWe7da)) return;
        mhandle.post(() -> {
            updateAttitude();
            updateRawSensors();
        });
    }

    @Subscribe
    public void onEvent(final Event_Battery_Ready a7adath_battery_ready) {
        if (!isOwnUnit(a7adath_battery_ready.mAndruavWe7da)) return;
        mhandle.post(this::updateBattery);
    }

    @Subscribe
    public void onEvent(final Event_GPS_Ready a7adath_gps_ready) {
        if (!isOwnUnit(a7adath_gps_ready.mAndruavWe7da)) return;
        mhandle.post(this::updateSpeedAlt);
    }

    @Subscribe
    public void onEvent(final Event_Vehicle_Mode_Changed a7adath_mode_changed) {
        if (!isOwnUnit(a7adath_mode_changed.mAndruavWe7da)) return;
        mhandle.post(this::updateFlightMode);
    }

    @Subscribe
    public void onEvent(final Event_FCB_Changed a7adath_fcb_changed) {
        if (!isOwnUnit(a7adath_fcb_changed.andruavUnitBase)) return;
        mhandle.post(() -> {
            updateFlightMode();
            updateBattery();
        });
    }

    @Subscribe
    public void onEvent(final Event_GCSBlockedChanged a7adath_gcs_blocked) {
        if (!isOwnUnit(a7adath_gcs_blocked.andruavUnitBase)) return;
        mhandle.post(this::updateFlightMode);
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
        return inflater.inflate(R.layout.fragment_phone_imu, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mhandle = new Handler();

        mTxtRoll = view.findViewById(R.id.phone_imu_roll);
        mTxtPitch = view.findViewById(R.id.phone_imu_pitch);
        mTxtHeading = view.findViewById(R.id.phone_imu_heading);
        mBtnCalibrate = view.findViewById(R.id.phone_imu_btn_calibrate);
        mBtnDirection = view.findViewById(R.id.phone_imu_btn_direction);
        mBtnZeroTilt = view.findViewById(R.id.phone_imu_btn_zerotilt);
        mTxtPhoneBattery = view.findViewById(R.id.phone_status_battery_phone);
        mTxtFcbBattery = view.findViewById(R.id.phone_status_battery_fcb);
        mTxtFlightMode = view.findViewById(R.id.phone_status_flight_mode);
        mTxtSpeedAlt = view.findViewById(R.id.phone_status_speed_alt);
        mTxtAcc = view.findViewById(R.id.phone_raw_acc);
        mTxtGyro = view.findViewById(R.id.phone_raw_gyro);
        mTxtMag = view.findViewById(R.id.phone_raw_mag);

        wireButtons();
        updateDirectionButton(false);
        updateAttitude();
        updateRawSensors();
        updateBattery();
        updateFlightMode();
        updateSpeedAlt();
    }

    private void updateAttitude() {
        if (AndruavSettings.andruavWe7daBase == null) return;
        final AndruavIMU imu = AndruavSettings.andruavWe7daBase.LastEvent_IMU;
        mTxtRoll.setText(String.format("%+.1f°", Math.toDegrees(imu.R)));
        mTxtPitch.setText(String.format("%+.1f°", Math.toDegrees(imu.P)));
        double heading = Math.toDegrees(imu.Y) % 360.0;
        if (heading < 0) heading += 360.0;
        mTxtHeading.setText(String.format("%.0f°", heading));
    }

    private void updateRawSensors() {
        if (AndruavSettings.andruavWe7daBase == null) return;
        final AndruavIMU imu = AndruavSettings.andruavWe7daBase.LastEvent_IMU;
        mTxtAcc.setText((imu.iA != null && imu.iA) ? formatAxes(imu.ACCsmoothedValues) : getString(R.string.phone_raw_na));
        mTxtGyro.setText((imu.iG != null && imu.iG) ? formatAxes(imu.GSV) : getString(R.string.phone_raw_na));
        mTxtMag.setText((imu.iM != null && imu.iM) ? formatAxes(imu.MSV) : getString(R.string.phone_raw_na));
    }

    private static String formatAxes(final double[] values) {
        if (values == null || values.length < 3) return "-";
        return String.format("%+.2f  %+.2f  %+.2f", values[0], values[1], values[2]);
    }

    private void updateBattery() {
        if (AndruavSettings.andruavWe7daBase == null) return;
        final AndruavUnitBase unit = AndruavSettings.andruavWe7daBase;
        final AndruavBattery battery = unit.LastEvent_Battery;
        mTxtPhoneBattery.setText(String.format("%.0f%% · %.2f V", battery.BatteryLevel, battery.Voltage / 1000.0));
        if (unit.useFCBIMU() && (unit.FCBoard != null)) {
            mTxtFcbBattery.setText(String.format("%.0f%% · %.2f V",
                    battery.FCB_BatteryRemaining, battery.FCB_BatteryVoltage / 1000.0));
        } else {
            mTxtFcbBattery.setText(R.string.phone_status_na);
        }
    }

    private void updateFlightMode() {
        if (AndruavSettings.andruavWe7daBase == null) return;
        final AndruavUnitBase unit = AndruavSettings.andruavWe7daBase;
        final int red = ThemeColors.get(requireContext(), R.attr.deBadText);
        final SpannableStringBuilder text = new SpannableStringBuilder(
                FlightMode.getFlightModeText(unit.getFlightModeFromBoard()));
        if (unit.IsArmed()) {
            final int start = text.length();
            text.append("  ").append(getString(R.string.phone_status_armed));
            text.setSpan(new ForegroundColorSpan(red), start, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        if (unit.getisGCSBlockedFromBoard()) {
            final int start = text.length();
            text.append("  ").append(getString(R.string.phone_status_blocked));
            text.setSpan(new ForegroundColorSpan(red), start, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        mTxtFlightMode.setText(text);
    }

    private void updateSpeedAlt() {
        if (AndruavSettings.andruavWe7daBase == null) return;
        final AndruavUnitBase unit = AndruavSettings.andruavWe7daBase;
        final Location loc = unit.getAvailableLocation();
        if (loc == null) {
            mTxtSpeedAlt.setText(R.string.phone_status_na);
            return;
        }
        final double altitude = unit.getActiveIMU().getAltitude();
        if (Preference.getPreferredUnits(null) == Constants.Preferred_UNIT_METRIC_SYSTEM) {
            mTxtSpeedAlt.setText(String.format("%.1f m/s · %.0f m", loc.getSpeed(), altitude));
        } else {
            mTxtSpeedAlt.setText(String.format("%.1f mph · %.0f ft",
                    loc.getSpeed() * UnitConversion.Speed_MetersPerSecondToMilePerHour,
                    altitude * UnitConversion.MetersToFeet));
        }
    }

    private void wireButtons() {
        mBtnCalibrate.setOnClickListener(v -> {
            new AlertDialog.Builder(requireActivity())
                    .setMessage(getString(ap.andruavmiddlelibrary.R.string.imu_calibration))
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            App.stopSensorService();
                            doCalibration();
                        }
                    })
                    .setNegativeButton(android.R.string.no, (dialog, id) -> dialog.cancel())
                    .create().show();
        });

        mBtnDirection.setOnClickListener(v -> {
            final int dir = (Preference.getMobileDirection(null) + 1) % 4;
            Preference.setMobileDirection(null, dir);
            AndruavSettings.mobileDirection = dir;
            updateDirectionButton(true);
        });

        mBtnZeroTilt.setOnClickListener(v -> {
            new AlertDialog.Builder(requireActivity())
                    .setMessage(getString(ap.andruavmiddlelibrary.R.string.imu_tilt))
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            doZeroTilt();
                        }
                    })
                    .setNegativeButton(android.R.string.no, (dialog, id) -> {
                    })
                    .create().show();
        });
    }

    private void doCalibration() {
        AndruavEngine.notification().Speak("Calibration");
        new calibrationTask().execute();
    }

    private void doZeroTilt() {
        // tell the IMU to reread the tilt reference
        EventBus.getDefault().post(new Event_IMU_CMD(Event_IMU_CMD.IMU_CMD_UpdateZeroTilt));
        Toast.makeText(getContext(), R.string.phone_imu_tilt_done, Toast.LENGTH_SHORT).show();
    }

    private void updateDirectionButton(final boolean speak) {
        final int dir = Preference.getMobileDirection(null);
        switch (dir) {
            case Surface.ROTATION_0:
                mBtnDirection.setText(getString(ap.andruavmiddlelibrary.R.string.gen_front));
                if (speak) AndruavEngine.notification().Speak(getString(ap.andruavmiddlelibrary.R.string.gen_front));
                break;
            case Surface.ROTATION_90:
                mBtnDirection.setText(getString(ap.andruavmiddlelibrary.R.string.gen_left));
                if (speak) AndruavEngine.notification().Speak(getString(ap.andruavmiddlelibrary.R.string.gen_left));
                break;
            case Surface.ROTATION_180:
                mBtnDirection.setText(getString(ap.andruavmiddlelibrary.R.string.gen_down));
                if (speak) AndruavEngine.notification().Speak(getString(ap.andruavmiddlelibrary.R.string.gen_down));
                break;
            case Surface.ROTATION_270:
                mBtnDirection.setText(getString(ap.andruavmiddlelibrary.R.string.gen_right));
                if (speak) AndruavEngine.notification().Speak(getString(ap.andruavmiddlelibrary.R.string.gen_right));
                break;
            default:
                break;
        }
    }

    //////////Calibration Task
    public class calibrationTask extends AsyncTask<Void, Integer, Void> {
        private String mstepDescription;
        private int mprogressPercentage;

        final Handler handle = new Handler() {
            @Override
            public void handleMessage(android.os.Message msg) {
                super.handleMessage(msg);
                //http://stackoverflow.com/questions/2224676/android-view-not-attached-to-window-manager
                if (mprogressDialog != null) {
                    mprogressDialog.setMessage(mstepDescription);
                    mprogressDialog.setProgress(mprogressPercentage);
                }
            }
        };

        void Step(String StepDescription, int Progress) {
            mstepDescription = StepDescription;
            mprogressPercentage = Progress;
            handle.sendMessage(handle.obtainMessage());
        }

        @Override
        protected void onPreExecute() {
            if (!isAdded()) return;
            mprogressDialog = new ProgressDialog(requireActivity());
            mprogressDialog.setMax(100);
            mprogressDialog.setMessage(getString(R.string.progress_initialization));
            mprogressDialog.setTitle(getString(R.string.progress_checking_sensors));
            mprogressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mprogressDialog.show();
        }

        @Override
        protected void onPostExecute(Void result) {
            //Update UI here if needed
            mEventAcc.unregisterSensor();
            mEventGyro.unregisterSensor();
            mEventMag.unregisterSensor();
            Step("Finished...", 100);
            exitProgressDialog();
            App.startSensorService();
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {

                mSensorManager = (SensorManager) requireActivity().getSystemService(Context.SENSOR_SERVICE);

                Thread.sleep(100);

                Preference.FactoryReset_Sensors(null);

                Step("Initialize Sensors...Acc", 10);
                mEventAcc = new Sensor_Accelerometer(mSensorManager);
                mEventAcc.calibrationValues = Preference.getAccCalibratedValue(null);
                mEventAcc.registerSensor();

                Thread.sleep(100);

                Step("Initialize Sensors...Mag", 20);
                mEventMag = new Sensor_Mag(mSensorManager);
                mEventMag.calibrationValues = Preference.getMagCalibratedValue(null);
                mEventMag.registerSensor();

                Thread.sleep(100);

                Step("Initialize Sensors...Gyro", 30);
                mEventGyro = new Sensor_Gyro(mSensorManager);
                mEventGyro.calibrationValues = Preference.getGyroCalibratedValue(null);
                mEventGyro.registerSensor();

                Thread.sleep(100);

                Step("Calibrating Gyro...", 40);
                if (mEventGyro.isSupported()) {
                    mEventGyro.calibrate();
                    while (!mEventGyro.isCalibrated()) {
                        Thread.sleep(50);
                    }
                    Preference.setGyroCalibratedValue(null, mEventGyro.calibrationValues);
                }
                Preference.isGyroCalibrated(null, true);   //always calibrated ... check @link PreferenceValidator.isValidIMU()

                Thread.sleep(100);

                Step("Calibrating Acc...", 70);
                if (mEventAcc.isSupported()) {
                    mEventAcc.calibrate();
                    while (!mEventAcc.isCalibrated()) {
                        Thread.sleep(50);
                    }
                    Preference.setAccCalibratedValue(null, mEventAcc.calibrationValues);
                }
                Preference.isAccCalibrated(null, true);    //always calibrated ... check @link PreferenceValidator.isValidIMU()

                Thread.sleep(100);

                // TODO: make compass calibration here
                Step("Calibrating Mag...", 90);
                Preference.isMagCalibrated(null, true);    //always calibrated ... check @link PreferenceValidator.isValidIMU()

                Thread.sleep(100);

            } catch (Exception ex) {
                AndruavEngine.log().logException("exception_imu", ex);
            } finally {
                return null;
            }
        }

    }
    //////////EOF Calibration Task

    private void exitProgressDialog() {
        //Update UI here if needed
        if (mprogressDialog != null) {
            mprogressDialog.dismiss();
            mprogressDialog = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
        App.startSensorService();
    }

    @Override
    public void onPause() {
        super.onPause();
        exitProgressDialog();
        EventBus.getDefault().unregister(this);
        App.stopSensorService();
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
