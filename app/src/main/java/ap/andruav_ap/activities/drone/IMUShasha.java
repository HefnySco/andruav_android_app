package ap.andruav_ap.activities.drone;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.andruav.AndruavEngine;
import com.andruav.AndruavSettings;
import com.andruav.sensors.AndruavIMU;

import de.greenrobot.event.EventBus;
import ap.andruav_ap.activities.baseview.BaseAndruavShasha;
import ap.andruav_ap.App;
import ap.andruavmiddlelibrary.preference.Preference;
import ap.andruav_ap.R;
import ap.andruavmiddlelibrary.factory.util.GMail;
import ap.andruavmiddlelibrary.sensors._7asasatEvents.Event_IMU_CMD;
import com.andruav.event.droneReport_Event.Event_IMU_Ready;
import ap.andruavmiddlelibrary.sensors.Sensor_Accelerometer;
import ap.andruavmiddlelibrary.sensors.Sensor_Gyro;
import ap.andruavmiddlelibrary.sensors.Sensor_Mag;

public class IMUShasha extends BaseAndruavShasha {


    protected IMUShasha Me;
    private Button btnCalibrate;
    private Button btnZeroTilt;
    private Button btnMobileDirection;
    private EditText txtLog;
    private TextView txtAcc;
    private Handler mhandle;
    private String htmlText;
    private String htmlLog;
    ////// Sensor Variables
    private SensorManager mSensorManager;
    private Sensor_Accelerometer mEventAcc;
    private Sensor_Mag mEventMag;
    private Sensor_Gyro mEventGyro;

    ////// EOF Sensor Variables

    private ProgressDialog mprogressDialog;

    //////////BUS EVENT

    public void onEvent (final Event_IMU_Ready a7adath_imu_ready) {
        AndruavIMU event_IMU = AndruavSettings.andruavWe7daBase.LastEvent_IMU;
        htmlText = "<font color=#1D5E1D>Raw Sensor Data:</font><br>";
        if (event_IMU.iA) {
            htmlText += "<font color=#1D5E1D>Acc:  </font>";
            htmlText += "<font color=#75A4D3>x:" + String.format("%+2.2f ", event_IMU.ACCsmoothedValues[0]) + "</font>";
            htmlText += "<font color=#75A4D3>y:" + String.format("%+2.2f ", event_IMU.ACCsmoothedValues[1]) + "</font>";
            htmlText += "<font color=#75A4D3>z:" + String.format("%+2.2f ", event_IMU.ACCsmoothedValues[2]) + "</font>";
        }
        else
        {
            htmlText = "<font color=#1D5E1D>Acc </font> <font color=#F75050> Not Available </font>";
        }

        if (event_IMU.iG) {
            htmlText += "<br><font color=#1D5E1D>Gyro: </font>";
            htmlText += "<font color=#75A4D3>x:" + String.format("%+2.2f ", event_IMU.GSV[0]) + "</font>";
            htmlText += "<font color=#75A4D3>y:" + String.format("%+2.2f ", event_IMU.GSV[1]) + "</font>";
            htmlText += "<font color=#75A4D3>z:" + String.format("%+2.2f ", event_IMU.GSV[2]) + "</font>";
        }
        else
        {
            htmlText += "<br><font color=#1D5E1D>Gyro </font> <font color=#F75050> Not Available </font>";
        }

        if (event_IMU.iM) {
            htmlText += "<br><font color=#1D5E1D>Mag:  </font>";
            htmlText += "<font color=#75A4D3>x:" + String.format("%+2.2f ", event_IMU.MSV[0]) + "</font>";
            htmlText += "<font color=#75A4D3>y:" + String.format("%+2.2f ", event_IMU.MSV[1]) + "</font>";
            htmlText += "<font color=#75A4D3>z:" + String.format("%+2.2f ", event_IMU.MSV[2]) + "</font>";
        }
        else
        {
            htmlText += "<br><font color=#1D5E1D>Mag </font> <font color=#F75050> Not Available </font>";
        }
        mhandle.sendEmptyMessage(0);
    }



    private void UIHandler ()
    {

        mhandle = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 0:
                        txtAcc.setText(Html.fromHtml(htmlText));
                        break;
                    case 1:
                        txtLog.append(Html.fromHtml(htmlLog));
                        break;
                }
            }
        };
    }

    //////////EOF BUS EVENT


    //////////Calibration Task
    public class calibrationTask extends AsyncTask<Void, Integer, Void>
    {
        private String mstepDescription;
        private int mprogressPercentage;

        Handler handle = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                //http://stackoverflow.com/questions/2224676/android-view-not-attached-to-window-manager
                if (mprogressDialog!= null) {
                    mprogressDialog.setMessage(mstepDescription);
                    mprogressDialog.setProgress(mprogressPercentage);
                }

            }
        };

        void Step (String StepDescription, int Progress)
        {
            mstepDescription = StepDescription;
            mprogressPercentage = Progress;
            handle.sendMessage(handle.obtainMessage());
        }

        // http://stackoverflow.com/questions/15215126/progress-dialog-freeze-at-the-middle-of-the-process
        @Override
        protected void onPreExecute() {
            //Show progress Dialog here
            mprogressDialog = new ProgressDialog(IMUShasha.this);
            mprogressDialog.setMax(100);
            mprogressDialog.setMessage("Initialization....");
            mprogressDialog.setTitle("Checking Sensors");
            mprogressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mprogressDialog.show();
        }


        @Override
        protected void onPostExecute(Void result)
        {
            //Update UI here if needed
            mEventAcc.unregisterSensor();
            mEventGyro.unregisterSensor();
            mEventMag.unregisterSensor();
            Step("Finished...", 100);
            exitProgressDialog();
            App.startSensorService();
        }




        @Override
        protected Void doInBackground(Void... params)
        {
            try {

                mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

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
                    Preference.setGyroCalibratedValue(null,mEventGyro.calibrationValues);
                }
                Preference.isGyroCalibrated(null,true);   //always calibrated ... check @link PreferenceValidator.isValidIMU()


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
            }
            finally
            {
                return null;
            }
        }

    }

    //////////EOF Calibration Task

    private void exitProgressDialog()
    {
        //Update UI here if needed
        if (mprogressDialog!=null) {
            mprogressDialog.dismiss();
            mprogressDialog = null;
        }

    }

    private void initGUI()
    {
        txtLog = findViewById(R.id.edtimuTextLog);
        txtAcc = findViewById(R.id.txtimuAcc);
        btnCalibrate = findViewById(R.id.imuactivity_btnCalibrate);
        btnCalibrate.setOnClickListener(new View.OnClickListener() {

            public void doCalibration ()
            {
                AndruavEngine.notification().Speak("Calibration");
                new calibrationTask().execute();

            }
            public void onClick(View v) {

                AlertDialog.Builder builder = new AlertDialog.Builder(Me);
                builder.setMessage(getString(R.string.imu_calibration))
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                App.stopSensorService();
                                doCalibration();
                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
                AlertDialog alert = builder.create();
                alert.show();
            }
        });


        btnZeroTilt = findViewById(R.id.imuactivity_btnZeroTilt);
        btnZeroTilt.setOnClickListener(new View.OnClickListener()
        {

            public void doZeroTilt()
            {
                //Preference.setAccZeroTilt(null,new float[]{mlatestEvent_IMU.P,mlatestEvent_IMU.R,0.0f});
                EventBus.getDefault().post(new Event_IMU_CMD(Event_IMU_CMD.IMU_CMD_UpdateZeroTilt)); // tel IMU to reread Tilt
                htmlLog = "<font color=#36AB36>" + "Sensor Titled Successfully" + "</font><br>";
                mhandle.sendEmptyMessage(1);


            }

            public void onClick(View v) {

                AlertDialog.Builder builder = new AlertDialog.Builder(Me);
                builder.setMessage(getString(R.string.imu_tilt))
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                               doZeroTilt();
                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {

                            }
                        });
                AlertDialog alert = builder.create();
                alert.show();
            }
        });

        btnMobileDirection = findViewById(R.id.imuactivity_btnMobileDirection);
        btnMobileDirection.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v) {


                int dir = Preference.getMobileDirection(null);
                dir = (dir + 1) %4;
                Preference.setMobileDirection(null, dir);
                AndruavSettings.mobileDirection = dir;
                updateArrowButton(true);



            }
        });

        updateArrowButton(false);
        UIHandler();
    }


    private void updateArrowButton (boolean speak)
    {
        int dir = Preference.getMobileDirection(null);

        switch (dir)
        {
            case Surface.ROTATION_0:
                btnMobileDirection.setCompoundDrawablesWithIntrinsicBounds(R.drawable.arrow_up_w_32x32,0, 0, 0);
                btnMobileDirection.setText(getString(R.string.gen_front));
                if (speak) AndruavEngine.notification().Speak(getString(R.string.gen_front));
                break;
            case Surface.ROTATION_90:
                btnMobileDirection.setCompoundDrawablesWithIntrinsicBounds(R.drawable.arrow_left_w_32x32,0, 0, 0);
                btnMobileDirection.setText(getString(R.string.gen_left));
                if (speak) AndruavEngine.notification().Speak(getString(R.string.gen_left));
                break;
            case Surface.ROTATION_180:
                btnMobileDirection.setText(getString(R.string.gen_down));
                btnMobileDirection.setCompoundDrawablesWithIntrinsicBounds(R.drawable.arrow_down_w_32x32,0, 0, 0);
                if (speak) AndruavEngine.notification().Speak(getString(R.string.gen_down));
                break;
            case Surface.ROTATION_270:
                btnMobileDirection.setCompoundDrawablesWithIntrinsicBounds(R.drawable.arrow_right_w_32x32,0, 0, 0);
                btnMobileDirection.setText(getString(R.string.gen_right));
                if (speak) AndruavEngine.notification().Speak(getString(R.string.gen_right));
                break;
            default:
                break;

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Me = this;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_imu);

        initGUI();

    }





    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_imu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.mi_imu_Help)
        {
            GMail.sendGMail(this, getString(R.string.email_title),getString(R.string.email_to), getString(R.string.email_subject), getString(R.string.email_body),null);

        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onResume() {
        super.onResume();
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        EventBus.getDefault().register(this);
        //EventBus.getDefault().register(this,"onIMUEvent",Event_IMU.class);

        App.startSensorService();

    }

    @Override
    public void onPause() {
        super.onPause();
        exitProgressDialog();
        EventBus.getDefault().unregister(this);
        App.stopSensorService();
    }
}
