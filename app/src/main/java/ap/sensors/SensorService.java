package ap.sensors;

import org.greenrobot.eventbus.Subscribe;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.Surface;

import androidx.core.app.NotificationCompat;

import ap.andruav_ap.R;

import com.andruav.AndruavSettings;
import com.andruav.andruavUnit.AndruavUnitBase;
import com.andruav.controlBoard.ControlBoardBase;

import java.util.Timer;

import org.greenrobot.eventbus.EventBus;
import ap.andruav_ap.App;

import com.andruav.andruavUnit.AndruavLocation;
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

    /**
     * Notification id for this service's foreground-service notification. Distinct from
     * {@code DroidPlannerService}'s own (101) and every id in {@link com.andruav.interfaces.INotification},
     * since this service and the FCB-link service can both be in the foreground at once.
     */
    private static final int FOREGROUND_ID = 120;

    //////// Attributes
    protected boolean               mcreated = false;
    protected Timer                 mTimer;
    private boolean                 mkillme = false;
    private Handler                 mhandler;
    private HandlerThread           mhandlerThread;
    /**
     * Runs the FC-state sensor gate on the main thread, since that's the thread
     * {@link #RegisterListeners()}/{@link #UnRegisterListeners()} originally registered
     * hardware listeners from (LocationManager delivers callbacks on the calling thread's
     * Looper, so toggling registration from a different thread would silently move them).
     */
    private final Handler           mGateHandler = new Handler(android.os.Looper.getMainLooper());
    private static final long       SENSOR_GATE_INTERVAL_MS = 3000;


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


    @Subscribe
    public void onEvent (final Event_ShutDown_Signalling event)
    {
        if (event.CloseOrder != 1) return ;


        this.shutDown();

    }


    @Subscribe
    public void onEvent (final Event_IMU_CMD event)
    {
        switch (event.cmdID)
        {
            case Event_IMU_CMD.IMU_CMD_UpdateZeroTilt:
                if (Preference.isMobileSensorsDisabled(null)) break; // mobile sensors overridden off: mEventAcc was never created
                Preference.setAccZeroTilt(null,new double[]{AndruavSettings.andruavWe7daBase.LastEvent_IMU.P, AndruavSettings.andruavWe7daBase.LastEvent_IMU.R,0.0});
                mEventAcc.tiltValues = Preference.getAccZeroTilt(null);
                break;

            case Event_IMU_CMD.IMU_CMD_ReadGPS:
                // GPS Sensor requests from service to read it.
                Event_IMU_CMD event_imu_cmd = event;
                AndruavLocation loc =  new AndruavLocation ((Location)event_imu_cmd.tag);
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

        mGateHandler.removeCallbacksAndMessages(null);

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

        stopForeground(true);
        stopSelf();

    }

    /**
     * Keeps GPS/IMU/battery collection running at full rate while the app is backgrounded
     * mid-flight - without this, API 26+ throttles or kills sensor/location updates for
     * background services after a short grace period.
     */
    private void promoteToForeground()
    {
        android.app.Notification notification = new NotificationCompat.Builder(this, ap.andruav_ap.Notification.CHANNEL_ID)
                .setContentTitle("Andruav")
                .setContentText("Collecting sensor and location data")
                .setSmallIcon(R.drawable.ic_logo2)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setOngoing(true)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // On Android 14+ the typed startForeground throws ForegroundServiceTypeNotAllowed
            // if the permission backing the type isn't granted. Gate on ACCESS_FINE/COARSE_LOCATION
            // so a START_STICKY restart after a permission revocation doesn't crash; fall back to
            // the untyped overload (uses manifest-declared type) when location permission is held
            // but the check raced, or when no type permission is held at all.
            int type = 0;
            if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                type = android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION;
            }
            try {
                if (type != 0) {
                    startForeground(FOREGROUND_ID, notification, type);
                } else {
                    startForeground(FOREGROUND_ID, notification);
                }
            } catch (SecurityException e) {
                startForeground(FOREGROUND_ID, notification);
            }
        } else {
            startForeground(FOREGROUND_ID, notification);
        }
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
            mGateHandler.postDelayed(runnableSensorGate, SENSOR_GATE_INTERVAL_MS);
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



    /**
     * Re-evaluates whether the phone's own GPS/IMU should be on, so a flight controller
     * connecting/disconnecting or arming/disarming mid-session takes effect without
     * restarting this service.
     */
    private final Runnable runnableSensorGate = new Runnable() {
        @Override
        public void run() {

            setMobileGpsImuActive(shouldMobileGpsImuRun());

            if (!mkillme) {
                mGateHandler.postDelayed(this, SENSOR_GATE_INTERVAL_MS);
            }
        }
    };

    /**
     * Decides whether the phone's own GPS/IMU should be actively polling, vs. relying on the
     * flight controller's MAVLink telemetry. Goal: stop heating the phone with redundant
     * GPS/IMU sampling once the FC is armed and already has its own GPS fix, while keeping the
     * phone active as a fallback location source (and as the source for MAVLink GPS injection)
     * whenever the FC can't supply one itself. Respects explicit GCS commands via GPS_MODE.
     */
    private boolean shouldMobileGpsImuRun()
    {
        boolean fcConnected = (App.droneKitServer != null) && App.droneKitServer.isConnected();
        if (!fcConnected) return true; // no FC telemetry at all - phone is the only location source

        if (Preference.isGPSInjecttionEnabled(null)) return true; // FC is relying on the phone feeding it GPS

        AndruavUnitBase unit = AndruavSettings.andruavWe7daBase;
        int gpsMode = unit.getGPSMode();
        if (gpsMode == AndruavUnitBase.GPS_MODE_MOBILE) return true; // GCS commanded explicit use of phone GPS
        if (gpsMode == AndruavUnitBase.GPS_MODE_FCB) {
            // GCS commanded exclusive FC GPS - phone sensors not needed (but still keep low rate if FC dies mid-flight)
            ControlBoardBase fcBoard = unit.FCBoard;
            return fcBoard == null || !fcBoard.isArmed();
        }

        // GPS_MODE_AUTO: smart switching based on FC state
        ControlBoardBase fcBoard = unit.FCBoard;
        if (fcBoard == null || !fcBoard.isArmed()) return true; // idle/preflight - keep a heartbeat so the unit is still locatable

        return fcBoard.getGPSfixType() < 2; // armed with FC GPS fix -> FC is self-sufficient, else phone backs it up
    }

    /**
     * Registers/unregisters just the phone's own GPS+IMU hardware listeners, independent of the
     * battery receiver. Idempotent - safe to call repeatedly with the same value.
     */
    private boolean mMobileSensorsActive = false;
    private void setMobileGpsImuActive(boolean active)
    {
        if (Preference.isMobileSensorsDisabled(null)) return; // manual override: sensor objects were never created

        if (active == mMobileSensorsActive) return;
        mMobileSensorsActive = active;

        if (active) {
            mEventAcc.registerSensor();
            mEventGyro.registerSensor();
            mEventMag.registerSensor();
            mEventGPS.registerSensor();
        } else {
            mEventAcc.unregisterSensor();
            mEventGyro.unregisterSensor();
            mEventMag.unregisterSensor();
            mEventGPS.unregisterSensor();
        }
    }


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
        if (!UnRegisterListenersCalled) return ;
        UnRegisterListenersCalled = false;

        setMobileGpsImuActive(shouldMobileGpsImuRun());

        this.registerReceiver(this.mEventBattery,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }


    private void UnRegisterListeners()
    {
        if (UnRegisterListenersCalled) return ;
        UnRegisterListenersCalled = true;

        setMobileGpsImuActive(false);

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
        promoteToForeground();

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
        mGateHandler.removeCallbacksAndMessages(null);
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

        stopForeground(true);

        super.onDestroy();
    }
}
