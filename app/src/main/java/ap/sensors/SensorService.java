package ap.sensors;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.Surface;

import com.andruav.AndruavSettings;

import java.util.Timer;

import de.greenrobot.event.EventBus;
import ap.andruav_ap.App;

import com.andruav.event.systemEvent.Event_ShutDown_Signalling;
import ap.andruavmiddlelibrary.preference.Preference;
import ap.andruavmiddlelibrary.sensors.CompassCalculation;
import ap.andruavmiddlelibrary.sensors.power.BatterySensor;
import ap.andruavmiddlelibrary.sensors.Sensor_Accelerometer;
import ap.andruavmiddlelibrary.sensors.Sensor_GPS;
import ap.andruavmiddlelibrary.sensors.Sensor_Gyro;
import ap.andruavmiddlelibrary.sensors.Sensor_Mag;

import com.andruav.event.droneReport_Event.Event_Battery_Ready;
import com.andruav.event.droneReport_Event.Event_GPS_Ready;
import ap.andruavmiddlelibrary.sensors._7asasatEvents.Event_IMU_CMD;
import com.andruav.event.droneReport_Event.Event_IMU_Ready;

public class SensorService extends Service {

    //////// Attributes
    protected boolean               mcreated = false;
    protected Timer                 mTimer;
    private boolean                 mkillme = false;
    private Handler                 mhandler;
    private HandlerThread           mhandlerThread;


    ////// Sensor Variables
    private LocationManager         mLocationManager;
    private SensorManager           mSensorManager;


    private Sensor_Accelerometer mEventAcc;
    private Sensor_Mag mEventMag;
    private Sensor_Gyro mEventGyro;
    private Sensor_GPS mEventGPS;

    protected BatterySensor         mEventBattery;

    private CompassCalculation mCompassCalculation;
   // Event_IMU Latestevent_IMU = new Event_IMU();

    final Event_GPS_Ready a7adath_gps_ready = new Event_GPS_Ready(AndruavSettings.andruavWe7daBase);
    final Event_IMU_Ready a7adath_imu_ready = new Event_IMU_Ready(AndruavSettings.andruavWe7daBase);
    final Event_Battery_Ready a7adath_battery_ready = new Event_Battery_Ready(AndruavSettings.andruavWe7daBase);

    /////////// EOF Attributes

    //////////BUS EVENT


    public void onEvent (final Event_ShutDown_Signalling event)
    {
        if (event.CloseOrder != 1) return ;


        this.shutDown();

    }


    public void onEvent (final Event_IMU_CMD event)
    {
        switch (event.cmdID)
        {
            case Event_IMU_CMD.IMU_CMD_UpdateZeroTilt:
                Preference.setAccZeroTilt(null,new double[]{AndruavSettings.andruavWe7daBase.LastEvent_IMU.P, AndruavSettings.andruavWe7daBase.LastEvent_IMU.R,0.0});
                mEventAcc.tiltValues = Preference.getAccZeroTilt(null);
                break;

            case Event_IMU_CMD.IMU_CMD_ReadGPS:
                // GPS Sensor requests from service to read it.
                Event_IMU_CMD event_imu_cmd = event;
                Location loc = (Location) (event_imu_cmd.tag);
                if (loc != null)
                {

                    AndruavSettings.andruavWe7daBase.LastEvent_IMU.setCurrentLocation(loc);
                   // ((AndruavWe7da)AndruavSettings.andruavWe7daBase).LastEvent_IMU.GroundAltitude = Sensor_GPS.groundaltitude;
                    AndruavSettings.andruavWe7daBase.LastEvent_IMU.GPS3DFix = mEventGPS.mFixLevel;
                    AndruavSettings.andruavWe7daBase.LastEvent_IMU.GPSFixQuality = mEventGPS.mFixQuality;
                    AndruavSettings.andruavWe7daBase.LastEvent_IMU.SATC = mEventGPS.intSatCount;
                    AndruavSettings.andruavWe7daBase.LastEvent_IMU.Hdop = Sensor_GPS.Hdop;
                    AndruavSettings.andruavWe7daBase.LastEvent_IMU.Vdop = Sensor_GPS.Vdop;

                    //EventBus.getDefault().post(AndruavSettings.andruavWe7daBase.LastEvent_IMU);
                    EventBus.getDefault().post(a7adath_gps_ready);
                }
                break;
            default:
                // non interested commands for this service ..
                // however this may be invalid values as this event is just for this service ... anyway ignore...maybe someone "service" else can understand it
                break;
        }

    }
    ///////////////////

    protected void shutDown()
    {

        mkillme = true;

        if (mhandler != null) {
            mhandler.removeCallbacksAndMessages(null);
            mhandler = null;
        }
        if (mhandlerThread != null)
        {
            mhandlerThread.quit();
            try {
                mhandlerThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }


        UnRegisterListeners();

        stopSelf();

    }

    /**
     * @link "http://www.techotopia.com/index.php/A_Basic_Overview_of_Android_Threads_and_Thread_handlers"
     */
    private void initHandler () {
        mhandlerThread = new HandlerThread("Sensors");
        mhandlerThread.start(); //NOTE: mhandlerThread.getLooper() will return null if not started.
        mhandler = new Handler(mhandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
            }

        };

        if (!Preference.isMobileSensorsDisabled(null)) {

            mhandler.postDelayed(runnableIMU, 50);
        }

        mhandler.postDelayed(runnableBattery, 50);  // first time run fast


    }

    /***
     * http://www.mopri.de/2010/timertask-bad-do-it-the-android-way-use-a-handler/
     * .......While coding for a project I noticed that updating the gui out of a TimerTask didn’t work everytime and specially didn’t work good.
     * Actually it basically never worked and at first I just couldn’t figure what was going on.
     * I put some debugging stuff in the timers and everything seemed to be fine,
     * as the debug messages appeared in the Log. Still: the gui wasn’t affected at all :oogle: .....
     * Hefny: I had the same problem although I was using eventBus to sendMessageToModule data to update a view, but calling invalidate was not calling onDraw.
     *
     */
    private final Runnable runnableIMU = new Runnable() {
        @Override
        public void run() {

           if (!mEventGyro.isSupported()) {

            }
            else
            {
            }

            if (mEventMag.isSupported()) {
                mCompassCalculation.processSensorData(mEventAcc.rawValues, mEventMag.rawValues);
            }

            if (mEventGyro.isSupported())
            {
                mEventGyro.updateGyrofromAcc(mEventAcc.vAcc);

                AndruavSettings.andruavWe7daBase.LastEvent_IMU.P = -mEventGyro.vGyro.getPitch();
                AndruavSettings.andruavWe7daBase.LastEvent_IMU.R = mEventGyro.vGyro.getRoll();
                AndruavSettings.andruavWe7daBase.LastEvent_IMU.Y = mCompassCalculation.azimuthCompass;
            }
            else
            {
                AndruavSettings.andruavWe7daBase.LastEvent_IMU.P = -mCompassCalculation.pitchCompass;
                AndruavSettings.andruavWe7daBase.LastEvent_IMU.R = mCompassCalculation.rollCompass;
                AndruavSettings.andruavWe7daBase.LastEvent_IMU.Y = mCompassCalculation.azimuthCompass;
            }


            switch (AndruavSettings.mobileDirection)
            {
                case  Surface.ROTATION_0:
                    break;
                case Surface.ROTATION_90: {
                    double temp = AndruavSettings.andruavWe7daBase.LastEvent_IMU.P;
                    AndruavSettings.andruavWe7daBase.LastEvent_IMU.P = AndruavSettings.andruavWe7daBase.LastEvent_IMU.R;
                    AndruavSettings.andruavWe7daBase.LastEvent_IMU.R = temp;
                }   break;
                case Surface.ROTATION_180:
                    AndruavSettings.andruavWe7daBase.LastEvent_IMU.P = AndruavSettings.andruavWe7daBase.LastEvent_IMU.P;
                    AndruavSettings.andruavWe7daBase.LastEvent_IMU.R = -AndruavSettings.andruavWe7daBase.LastEvent_IMU.R;
                    break;
                case Surface.ROTATION_270:
                    double temp = AndruavSettings.andruavWe7daBase.LastEvent_IMU.P;
                    AndruavSettings.andruavWe7daBase.LastEvent_IMU.P = -AndruavSettings.andruavWe7daBase.LastEvent_IMU.R;
                    AndruavSettings.andruavWe7daBase.LastEvent_IMU.R = -temp;
                    break;
            }

            AndruavSettings.andruavWe7daBase.LastEvent_IMU.PT = mEventAcc.tiltValues[0];
            AndruavSettings.andruavWe7daBase.LastEvent_IMU.RT = mEventAcc.tiltValues [1];

            AndruavSettings.andruavWe7daBase.LastEvent_IMU.iA = mEventAcc.isSupported();
            AndruavSettings.andruavWe7daBase.LastEvent_IMU.iG = mEventGyro.isSupported();
            AndruavSettings.andruavWe7daBase.LastEvent_IMU.iM = mEventMag.isSupported();

            // Copy Smoothed values
            AndruavSettings.andruavWe7daBase.LastEvent_IMU.ACCsmoothedValues   = mEventAcc.smoothedValues;
            AndruavSettings.andruavWe7daBase.LastEvent_IMU.GSV                 = mEventGyro.smoothedValues;
            AndruavSettings.andruavWe7daBase.LastEvent_IMU.MSV                 = mEventMag.smoothedValues;


            //EventBus.getDefault().post(AndruavSettings.andruavWe7daBase.LastEvent_IMU);
            EventBus.getDefault().post(a7adath_imu_ready);

            if (!mkillme) {
                mhandler.postDelayed(this, 100);
            }
            else
            {
                Log.d(App.TAG,"SensorService Runnable Terminated");
            }

        }
    };



    private final Runnable runnableBattery = new Runnable() {
        @Override
        public void run() {
      /* do what you need to do */

            AndruavSettings.andruavWe7daBase.LastEvent_Battery.BatteryTechnology = mEventBattery.batteryTechnology;
            AndruavSettings.andruavWe7daBase.LastEvent_Battery.BatteryTemperature = mEventBattery.batteryTemperature/10.0;
            AndruavSettings.andruavWe7daBase.LastEvent_Battery.Voltage = mEventBattery.voltage;
            AndruavSettings.andruavWe7daBase.LastEvent_Battery.BatteryLevel = mEventBattery.batteryLevel;
            AndruavSettings.andruavWe7daBase.LastEvent_Battery.Charging = mEventBattery.isCharging;
            AndruavSettings.andruavWe7daBase.LastEvent_Battery.Health = mEventBattery.getHealthString();
            AndruavSettings.andruavWe7daBase.LastEvent_Battery.PlugStatus = mEventBattery.getStatusString();



            //EventBus.getDefault().post(AndruavSettings.andruavWe7daBase.LastEvent_Battery);
            EventBus.getDefault().post(a7adath_battery_ready);

            if (!mkillme) {
                mhandler.postDelayed(this, 5000);
            }


        }
    };

     private void InitSensors()
    {
        if (!Preference.isMobileSensorsDisabled(null)) {
            mCompassCalculation = new CompassCalculation();

            mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);


            mEventAcc = new Sensor_Accelerometer(mSensorManager);
            mEventMag = new Sensor_Mag(mSensorManager);
            mEventGyro = new Sensor_Gyro(mSensorManager);
            mEventGPS = new Sensor_GPS(mLocationManager);

            readCalibrationValues();
        }

        mEventBattery = new BatterySensor();

    }

    private void readCalibrationValues()
    {
        if (!Preference.isMobileSensorsDisabled(null)) {
            mEventAcc.tiltValues = Preference.getAccZeroTilt(null);
            mEventAcc.calibrationValues = Preference.getAccCalibratedValue(null);
            mEventMag.calibrationValues = Preference.getMagCalibratedValue(null);
            mEventGyro.calibrationValues = Preference.getGyroCalibratedValue(null);
        }
    }

    boolean UnRegisterListenersCalled = true;
    private void RegisterListeners()
    {
        if (UnRegisterListenersCalled == false) return ;
        UnRegisterListenersCalled = false;

        if (!Preference.isMobileSensorsDisabled(null)) {
            mEventAcc.registerSensor();
            mEventGyro.registerSensor();
            mEventMag.registerSensor();
            mEventGPS.registerSensor();
        }

        this.registerReceiver(this.mEventBattery,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }


    private void UnRegisterListeners()
    {
        if (UnRegisterListenersCalled == true) return ;
        UnRegisterListenersCalled = true;

        if (!Preference.isMobileSensorsDisabled(null))
        {
            mEventAcc.unregisterSensor();
            mEventGyro.unregisterSensor();
            mEventMag.unregisterSensor();
            mEventGPS.unregisterSensor();
        }

        this.unregisterReceiver(this.mEventBattery);
    }

    public void doZeroTilt ()
    {
        mEventAcc.doZeroTilt();
    }

    public SensorService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(android.content.Intent intent, int flags, int startId)
    {
        if (mcreated)
        {   // already running
            readCalibrationValues();
            return START_STICKY;
        }

        mcreated = true;
        EventBus.getDefault().register(this);

        AndruavSettings.mobileDirection = Preference.getMobileDirection(null);

        InitSensors();
        RegisterListeners();


        /*
        Commented this violated the saved preference.
        mEventAcc.calibrate();
        mEventGyro.calibrate();
        mEventMag.calibrate();
        */

        initHandler();

        return START_STICKY;
    }

    @Override
    public void onDestroy(){

        EventBus.getDefault().unregister(this);


        mkillme = true;
        UnRegisterListeners();
        if (mhandler != null) {
            mhandler.removeCallbacksAndMessages(null);
        }


        if (mhandlerThread != null)
        {


            mhandlerThread.quit();
            try {
                mhandlerThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


        super.onDestroy();
    }
}
