/*  RCMobileStuff
    Copyright (C) <2014>  RCMobileStuff rcmobilestuff@gmail.com

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package ap.andruav_ap;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import androidx.multidex.MultiDex;
import androidx.multidex.MultiDexApplication;

import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.text.format.Time;
import android.view.Surface;

import com.andruav.AndruavDroneFacade;
import com.andruav.AndruavEngine;
import com.andruav.AndruavFacade;
import com.andruav.AndruavSettings;
import com.andruav.event.fcb_7adath._7adath_FCB_2AMR;
import com.andruav.event.networkEvent._7adath_ConnectionQuality;
import com.andruav.event.systemEvent.Event_ShutDown_Signalling;
import com.andruav.andruavUnit.AndruavUnitMe;
import com.andruav.andruavUnit.AndruavUnitBase;
import com.andruav.Constants;
import com.andruav.TelemetryProtocol;
import com.andruav.event.droneReport_Event.Event_FCB_Changed;
import com.andruav.interfaces.IEventBus;
import com.andruav.interfaces.INotification;
import com.andruav.interfaces.IPreference;

import de.greenrobot.event.EventBus;
import ap.andruav_ap.communication.AndruavWSClient_TooTallNate;
import ap.andruav_ap.communication.AndruavUnitMap;
import ap.andruav_ap.communication.ControlBoardFactory;
import ap.andruav_ap.communication.telemetry.IEvent_SocketData;
import ap.andruav_ap.communication.telemetry.SerialSocketServer.AndruavGCSSerialSocketServer;
import ap.andruav_ap.communication.telemetry.TelemetryModeer;

import com.andruav.event.networkEvent.EventSocketState;
import ap.andruav_ap.communication.telemetry.TelemetryProtocolParser;
import ap.andruav_ap.communication.AndruavUnitFactory;

import ap.andruav_ap.communication.telemetry.DroneKit.DroneKitServer;
import com.andruav.FeatureSwitch;

import ap.andruavmiddlelibrary.LoginClient;
import ap.andruav_ap.communication.telemetry.BlueTooth.BlueToothFCB;
import ap.andruavmiddlelibrary.com.serenegiant.encoder.MediaVideoEncoder;
import ap.andruavmiddlelibrary.eventClasses.remoteControl.Event_ProtocolChanged;
import ap.andruavmiddlelibrary.preference.Preference;
import ap.andruavmiddlelibrary.preference.PreferenceValidator;
import ap.andruavmiddlelibrary.factory.DeviceFeatures;

import ap.andruavmiddlelibrary.factory.tts.SoundManager;
import ap.andruavmiddlelibrary.factory.tts.TTS;

import com.andruav.controlBoard.shared.common.VehicleTypes;
import com.andruav.protocol.commands.ProtocolHeaders;
import com.andruav.protocol.commands.textMessages.AndruavMessage_CameraList;
import com.andruav.protocol.communication.websocket.AndruavWSClientBase;
import com.andruav.uavos.modules.UAVOSConstants;
import com.andruav.uavos.modules.UAVOSException;
import com.andruav.uavos.modules.UAVOSModuleCamera;
import com.andruav.uavos.modules.UAVOSModuleFCB;
import com.andruav.util.RandomString;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import ap.andruavmiddlelibrary.log.ExceptionHTTPLogger;
import ap.andruavmiddlelibrary.log.ExceptionHandler;
import ap.andruavmiddlelibrary.factory.communication.NetInfoAdapter;
import ap.sensors.SensorService;
import ap.andruavmiddlelibrary.database.DaoManager;

import static com.andruav.uavos.modules.UAVOSConstants.UAVOS_MODULE_TYPE_CAMERA;
import static com.andruav.uavos.modules.UAVOSConstants.UAVOS_MODULE_TYPE_FCB;

/**
 * Created by M.Hefny on 11-Sep-14.
 */
public class App  extends MultiDexApplication implements IEventBus, IPreference {




    /***
     * used to restore screen orientation of main screen after entering MAP or FPV screen
     * <br> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED means unknown
     * <br> other values from {@link ActivityInfo}
     */
    public static Activity activeActivity = null;
    public static int gui_ConnectionIconID;
    public static Time timeApp;
    public boolean D = false; // debug
    public final static String TAG = "RCMOB";
    public static Context context;
    public static IEvent_SocketData iEvent_socketData;
   // public static AndruavWSClient andruavWSClient;
    public static SoundManager soundManager;
    public static Intent iSensorService;
    /***
     * unit-ID of the telemetry drone that this CGS connected with.
     */



    public static Intent MainIntent;
    public static PendingIntent MainPendingIntent;
    public static KMLFileHandler KMLFile;

    protected static AndruavGCSSerialSocketServer andruavGCSSerialSocketServer;
    public static DroneKitServer droneKitServer;
    public static BlueToothFCB BT;
    /*public static USBFCB usbConn;
    public static FTDIFCB ftdiusbConn;*/
    public static String versionName;
    private static ExceptionHTTPLogger exceptionHTTPLogger;
    public static Notification notification;

    public static TelemetryProtocolParser telemetryProtocolParser;

    public static boolean isFirstRun;
    public static boolean isNewVersion;

    public static HandlerThread mScheduleThread;
    public static Handler       mScheduleHandler;

    private TelephonyManager mManager;
    static private boolean shutdown = false;

    private static ScheduledExecutorService rcRepeater;

    private static boolean mAndruavUDP = false;


    private android.os.Handler mhandle = null;
    public static boolean isDone = false;
    public static long nextTimeEvent =0;

    public static boolean m_Kill = false;



    public void onEvent (final _7adath_ConnectionQuality a7adath_ConnectionQuality)
    {
        Message msg = new Message();
        msg.obj = a7adath_ConnectionQuality;
        mhandle.sendMessageDelayed(msg, 0);
    }

    public void onEvent (final EventSocketState event) {
        final Message msg = mhandle.obtainMessage();
        msg.obj = event;
        if (mhandle != null)  mhandle.sendMessageDelayed(msg,0);
    }

    public void onEvent(Event_ProtocolChanged event_protocolChanged) {

        Message msg = new Message();
        msg.obj = event_protocolChanged;
        mhandle.sendMessageDelayed(msg, 0);
    }

    public void onEvent (final Event_FCB_Changed a7adath_fcb_changed)
    {
        final Message msg = mhandle.obtainMessage();
        msg.obj = a7adath_fcb_changed;
        if (mhandle != null) mhandle.sendMessageDelayed(msg, 0);
    }


    public void onEvent (final _7adath_FCB_2AMR adath_fcb_2AMR)
    {
        final Message msg = mhandle.obtainMessage();
        msg.obj = adath_fcb_2AMR;
        if (mhandle != null) mhandle.sendMessageDelayed(msg, 0);
    }



    private final Runnable defaultSchedular = new Runnable() {
        @Override
        public void run() {


        }
    };



    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    private void UIHandler () {
        mhandle = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.obj instanceof EventSocketState) {
                    //Socket Status
                    EventSocketState eventSocketState = (EventSocketState) msg.obj;
                    if (eventSocketState.SocketState == EventSocketState.ENUM_SOCKETSTATE.onConnect) {
                        String connection = getString(R.string.gen_connected);
                        connection += " to Internet Server";
                        App.notification.displayNotification(INotification.NOTIFICATION_TYPE_NORMAL, "Andruav", connection, true, INotification.INFO_TYPE_PROTOCOL, true);

                        App.gui_ConnectionIconID = R.drawable.connected_w_32x32;
                    } else if (eventSocketState.SocketState == EventSocketState.ENUM_SOCKETSTATE.onDisconnect) {
                        App.gui_ConnectionIconID = R.drawable.connect_w_32x32;
                        App.soundManager.playSound(SoundManager.SND_ERR);
                        TTS.getInstance().Speak(getString(R.string.gen_connectionlost));
                    } else if (eventSocketState.SocketState == EventSocketState.ENUM_SOCKETSTATE.onError) {
                        App.gui_ConnectionIconID = R.drawable.connected_error_32x32;
                        App.soundManager.playSound(SoundManager.SND_ERR);
                        TTS.getInstance().Speak(getString(R.string.gen_connectionlost));
                    } else if (eventSocketState.SocketState == EventSocketState.ENUM_SOCKETSTATE.onMessage) {
                        // MenuItem mi = mMenu.findItem(R.id.action_main_wsconnect);
                        // mi.setIcon(R.drawable.connected_color_32x32);
                    } else if (eventSocketState.SocketState == EventSocketState.ENUM_SOCKETSTATE.onRegistered) {
                        // MenuItem mi = mMenu.findItem(R.id.action_main_wsconnect);
                        // mi.setIcon(R.drawable.connected_color_32x32);
                        TTS.getInstance().Speak(getString(R.string.gen_connected));
                    }
                }
                else if (msg.obj instanceof  _7adath_ConnectionQuality) {
                    AndruavDroneFacade.sendCommSignalStatus(null, true);
                }
                else if (msg.obj instanceof Event_FCB_Changed) {
                    // Restore Old Telemetry
                    final Event_FCB_Changed adath_fcb_changed = (Event_FCB_Changed) msg.obj;
                    final AndruavUnitBase andruavWe7da = adath_fcb_changed.andruavUnitBase;
                    if ((andruavWe7da.FCBoard!= null)  && (AndruavSettings.remoteTelemetryAndruavWe7da != null) && andruavWe7da.IsMe()) {   // it is already connected to me

                        AndruavFacade.ResumeTelemetry(Constants.SMART_TELEMETRY_LEVEL_NEGLECT);
                        TTS.getInstance().Speak(getString(R.string.action_res_tel));
                    }
                }
                else if (msg.obj instanceof _7adath_FCB_2AMR) {
                    final _7adath_FCB_2AMR adath_fcb_2AMR = (_7adath_FCB_2AMR)msg.obj;

                    TelemetryModeer.startAutoConnection(adath_fcb_2AMR.enForceConnection);

                }
                else if (msg.obj instanceof Event_ProtocolChanged) {
                    try {

                        if (AndruavSettings.andruavWe7daBase.getIsCGS()) {

                        } else if ((TelemetryModeer.getConnectionInfo() != TelemetryModeer.CURRENTCONNECTION_NON) && !AndruavSettings.andruavWe7daBase.getIsCGS()) {
                            UAVOSModuleFCB uavosModuleFCB = new UAVOSModuleFCB();
                            uavosModuleFCB.ModuleId = UAVOS_MODULE_TYPE_FCB;
                            uavosModuleFCB.BuiltInModule = true;

                            AndruavEngine.getUAVOSMapBase().put(uavosModuleFCB.ModuleId, uavosModuleFCB);
                        } else {
                            AndruavEngine.getUAVOSMapBase().remove(UAVOS_MODULE_TYPE_FCB);
                        }
                    }
                    catch (final UAVOSException e)
                    {
                        // yavos is not available.
                        // speak UAVOSException error.
                    }
                }
                }
        };
    }


    public void initSignalMonitor ()
    {
        mManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (mManager != null) {
            mManager.listen(mListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS |
                    PhoneStateListener.LISTEN_CELL_LOCATION);
        }
    }

    final PhoneStateListener mListener = new PhoneStateListener()
    {
        @Override
        public void onCellLocationChanged(CellLocation mLocation)
        {
            if (shutdown) return;
        }

        @Override
        public void onSignalStrengthsChanged(SignalStrength sStrength) {
//            if (shutdown || AndruavSettings.andruavWe7daBase.getIsCGS()) return;
//
//            int dbm = 0;
//            final String[] parts = sStrength.toString().split(" ");
//
//            if (sStrength.isGsm())
//            {
//                dbm = sStrength.getGsmSignalStrength();
//                if (dbm ==0) dbm = Integer.parseInt(parts[3]);
//            }else
//            if (sStrength.getCdmaDbm()>0)
//            {
//                dbm = sStrength.getCdmaDbm();
//            }else if(sStrength.getEvdoDbm()>0)
//            {
//                dbm = sStrength.getEvdoDbm();
//            }
//
//            if (dbm >=0) {
//                if (mManager.getNetworkType() == TelephonyManager.NETWORK_TYPE_LTE) {
//                    dbm = Integer.parseInt(parts[11]);
//                } else {
//                    dbm = -113 + 2 * dbm;
//                }
//            }
//
//            //TTS.getInstance().Speak(String.valueOf(dbm));
//
//
//            AndruavSettings.andruavWe7daBase.setSignal(mManager.getNetworkType(), dbm);
//            EventBus.getDefault().post(new _7adath_ConnectionQuality());
        }
    };


    /***
     * http://stackoverflow.com/questions/2002288/static-way-to-get-context-on-android
     * @return
     */
    public static Context getAppContext() {
        return App.context;
    }








    public static void updateWe7daInfo()
    {

        AndruavSettings.andruavWe7daBase.UnitID = Preference.getWebServerUserName(null).toLowerCase();
        AndruavSettings.andruavWe7daBase.PartyID = Preference.getLoginPartyID(null);
        AndruavSettings.andruavWe7daBase.Description = Preference.getWebServerUserDescription(null);
        AndruavSettings.andruavWe7daBase.GroupName = Preference.getWebServerGroupName(null).toLowerCase();
        AndruavSettings.andruavWe7daBase.setRTC(Preference.isChannelReturnToCenter(null));


        AndruavSettings.encryptionkey = Preference.getEncryptedWSKey(null).getBytes();
        AndruavSettings.encryptionEnabled = Preference.isEncryptedWS(null);
        AndruavSettings.andruavWe7daBase.setVehicleType(Preference.getVehicleType(null));

        if (AndruavEngine.getPreference().getModuleType().contains(ProtocolHeaders.UAVOS_CAMERA_MODULE_CLASS)) {
            AndruavSettings.andruavWe7daBase.mIsModule = true;
        }
    }

    public static void startSensorService()
    {
        if (iSensorService == null) {

                iSensorService = new Intent(App.getAppContext(), SensorService.class);
                App.getAppContext().startService(iSensorService);
            }

        if (KMLFile == null) {
            KMLFile = new KMLFileHandler();
            KMLFile.openKMZ("Trip" + Preference.getWebServerGroupName(null) + " " + Preference.getWebServerUserName(null));
        }
        shutdown = false;
    }


    /**
     * Stop Sensor Service
     */
    public static void stopSensorService()
    {
        shutdown= true;
        if (iSensorService != null)
        {

            App.getAppContext().stopService(iSensorService);
            iSensorService = null;
        }

        if (KMLFile != null)
        {
            KMLFile.shutDown();
            KMLFile = null;
        }
    }

    public static void sendTelemetryfromGCS (final byte[] Data, AndruavUnitBase telemetryTarget)
    {
        if (AndruavEngine.getAndruavWS() != null) ((AndruavWSClient_TooTallNate) AndruavEngine.getAndruavWS()).sendTelemetryfromGCS(Data, telemetryTarget.PartyID);
    }

    public static void sendTelemetryfromDrone(final byte[] Data)
    {
        if (AndruavEngine.getAndruavWS() != null) ((AndruavWSClient_TooTallNate) AndruavEngine.getAndruavWS()).sendTelemetryfromDrone(Data);

    }

    public static boolean isSocketListenerRunning()
    {
        if (andruavGCSSerialSocketServer == null) return false;
        return  andruavGCSSerialSocketServer.isRunning();
    }

    public static void startsocketListener ()
    {

        TTS.getInstance().Speak(App.getAppContext().getString(R.string.gen_serialsocket_started));
        if (andruavGCSSerialSocketServer ==null)
        {
            andruavGCSSerialSocketServer = new AndruavGCSSerialSocketServer();
        }
        andruavGCSSerialSocketServer.Listen("0.0.0.0", Preference.getSerialServerPort(null));
    }



    public static void stopsocketListener () {
        TTS.getInstance().Speak(App.getAppContext().getString(R.string.gen_serialsocket_stopped));
        if (andruavGCSSerialSocketServer == null) return ;
            andruavGCSSerialSocketServer.stopListening();
        }

    /***
     * This is not UAVOS. So even if UAVOS is disabled this function should be called.
     * It is used to make Andruav & UAVOS compatible.
     */
    protected static void defineBuiltIntModules ()
    {
        try {

            if (DeviceFeatures.hasCamera) {
                AndruavSettings.andruavLocalCameraModuleID = UAVOS_MODULE_TYPE_CAMERA + AndruavSettings.andruavWe7daBase.PartyID;
                UAVOSModuleCamera uavosModuleCamera = new UAVOSModuleCamera();
                uavosModuleCamera.ModuleId = AndruavSettings.andruavLocalCameraModuleID;
                uavosModuleCamera.BuiltInModule = true;

                boolean supportFlash = getAppContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);

                // Define Camera
                JSONObject camera = new JSONObject();
                camera.put(UAVOSConstants.CAMERA_SUPPORT_VIDEO, true);
                camera.put(UAVOSConstants.CAMERA_SUPPORT_ZOOM, true);
                camera.put(UAVOSConstants.CAMERA_SUPPORT_FLASH, supportFlash);
                camera.put(UAVOSConstants.CAMERA_UNIQUE_NAME, AndruavSettings.andruavWe7daBase.PartyID);
                camera.put(UAVOSConstants.CAMERA_TYPE, AndruavMessage_CameraList.EXTERNAL_CAMERA_TYPE_RTCWEBCAM);
                camera.put(UAVOSConstants.CAMERA_ANDROID_DUAL_CAM, true);
                camera.put(UAVOSConstants.CAMERA_RECORDING_NOW, false);
                camera.put(UAVOSConstants.CAMERA_ACTIVE, 1);
                if (AndruavEngine.getPreference().getModuleType().contains(ProtocolHeaders.UAVOS_CAMERA_MODULE_CLASS)) {
                    camera.put(UAVOSConstants.CAMERA_LOCAL_NAME, AndruavSettings.andruavWe7daBase.UnitID);
                }
                else
                {
                    camera.put(UAVOSConstants.CAMERA_LOCAL_NAME, UAVOS_MODULE_TYPE_CAMERA);
                }
                // End of Define Camera

                JSONArray json_tracks = new JSONArray();
                json_tracks.put(camera);
                uavosModuleCamera.setModuleMessages(json_tracks);

                AndruavEngine.getUAVOSMapBase().put(uavosModuleCamera.ModuleId, uavosModuleCamera);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static void initializeAndruavUDP()
    {
        try {
            stopUDPServer();


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isAndruavUDPOn()
    {
        return mAndruavUDP;
    }

    public static void startUDPServer ()
    {
        stopUDPServer();



        if (!BuildConfig.DEBUG)
        {
            return;
        }

    }

    public static void stopUDPServer ()
    {

        mAndruavUDP = false;

        if (!BuildConfig.DEBUG)
        {
            return;
        }



    }

    /**
     * Connect to server.
     * You need to handle registration and other issues.
     */
    public static void startAndruavWS ()
    {

        if ((AndruavSettings.andruavWe7daBase.getTelemetry_protocol()==TelemetryProtocol.TelemetryProtocol_No_Telemetry) && (AndruavSettings.andruavWe7daBase.getIsCGS()))
        {   // a GCS is always a telemetry
            AndruavSettings.andruavWe7daBase.setTelemetry_protocol(TelemetryProtocol.TelemetryProtocol_Unknown_Telemetry);
        }
        String websocketURL = "wss://" + LoginClient.getWSURL();

        if (AndruavEngine.getAndruavWS() == null)
        {
            AndruavEngine.setAndruavWS(new AndruavWSClient_TooTallNate(websocketURL, null));
         }
        else {


            if (AndruavEngine.getAndruavWS().isConnected())
                AndruavEngine.getAndruavWS().disconnect();


        }
        AndruavEngine.getAndruavWS().connect(websocketURL);
        TTS.getInstance().Speak(App.getAppContext().getString(R.string.gen_speak_connecting));
    }


    /**
     * Stops Andruav WebSocket Listener
     */
    public static void stopAndruavWS (boolean kill)
    {

        if (AndruavEngine.getAndruavWS()== null)
        {
            return ;
        }

        if(AndruavEngine.getAndruavWS().isConnected()) {
            AndruavSettings.andruavWe7daBase.setShutdown(true);
        }

        AndruavEngine.getAndruavWS().disconnect();
        AndruavEngine.getAndruavWe7daMapBase().clear();

        // kill is used to remove the socket server, this is necessary when you want to initialize a new one
        // with new IP or PORT number
        if (kill)
        {

            AndruavEngine.getAndruavWS().shutDown();

        }

    }


    /**
     * return TRUE if Andruav is connected.
     * That does not mean it is registered or anything. it is just the socket is connected to server.
     * @return
     */
    public static Boolean isAndruavWSConnected()
    {
        return !((AndruavEngine.getAndruavWS() == null)  || (!AndruavEngine.getAndruavWS().isConnected()));

    }



    /***
     * Returns current socket conneciton status
     * @return
     */
    public static int getAndruavWSStatus () {
        if (AndruavEngine.getAndruavWS()==null) return AndruavWSClientBase.SOCKETSTATE_FREASH;

        return AndruavEngine.getAndruavWS().getSocketState();
    }


    /***
     * Returns current socket conneciton status
     * @return
     */
    public static int getAndruavWSAction () {
        if (AndruavEngine.getAndruavWS()==null) return AndruavWSClientBase.SOCKETACTION_NONE;

        return AndruavEngine.getAndruavWS().getSocketAction();
    }


    /**
     * Loads sound resources.
     */
    private void prepareSounds() {
        soundManager = new SoundManager(App.getAppContext());
        soundManager.addSound(0, R.raw.sputnik1);
        soundManager.addSound(SoundManager.SND_ERR, R.raw.taan);
        soundManager.addSound(SoundManager.SND_EMERGENCY, R.raw.lostalarm);
    }


    @Override
    public void onCreate()
    {
        super.onCreate();


//        try {
//            // Google Play will install latest OpenSSL
//            ProviderInstaller.installIfNeeded(getApplicationContext());
//            SSLContext sslContext;
//            sslContext = SSLContext.getInstance("TLSv1.2");
//            sslContext.init(null, null, null);
//            sslContext.createSSLEngine();
//        } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException
//                | NoSuchAlgorithmException | KeyManagementException e) {
//            e.printStackTrace();
//        }


        App.context = getApplicationContext();
        AndruavEngine.AppContext = App.context;

        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(App.context));

        if (FeatureSwitch.DEBUG_MODE == true)
        {
            // turn off the log 'No subscribers registered for event class xxx.xxx'
            EventBus.builder().logNoSubscriberMessages(false).installDefaultEventBus();
        }
        DaoManager.init();






        MediaVideoEncoder.SetVideoEncoder();


        AndruavEngine.setPreference(this);

        DeviceFeatures.scanDevice(App.context);



        notification = new Notification();
        notification.init(App.context);
        AndruavEngine.setNotificationHandler(notification);

        exceptionHTTPLogger = new ExceptionHTTPLogger();
        AndruavEngine.setLogHandler(exceptionHTTPLogger);


        exceptionHTTPLogger.sendOldErrors();

        // should be early so that settings in Preferences will be reflected below
        getAppVersion();


        if (Preference.getLoginPartyID(null).equals("")){
            RandomString randomString = new RandomString(5);
            Preference.setLoginPartyID(null,randomString.nextString());
        }




        AndruavEngine.setLo7etTa7akomMasna(new ControlBoardFactory());
        AndruavEngine.setAndruavWe7daMasna3(new AndruavUnitFactory());
        AndruavEngine.setAndruavWe7daMapBase(new AndruavUnitMap());



        defineAndruavUnit(Preference.isGCS(null));
        //exceptionHTTPLogger.Logentris(AndruavSettings.AccessCode,"INFO","Started");



        timeApp = new Time();

        gui_ConnectionIconID = R.drawable.connect_w_32x32;

        prepareSounds();

        if (DeviceManagerFacade.hasBlueTooth()) {
            BT = new BlueToothFCB();
        }

        if (DeviceManagerFacade.hasUSBHost()) {
           /* usbConn = new USBFCB();
            ftdiusbConn = new FTDIFCB();*/
        }



        mScheduleThread = new HandlerThread("APP_SCHEDULE");
        mScheduleThread.start();

        mScheduleHandler = new Handler(mScheduleThread.getLooper());
        mScheduleHandler.postDelayed(mSchedulRunnable,1000);

        EventBus.getDefault().register(this);
        AndruavEngine.setEventBus(this);
        AndruavEngine.setEmergency(new Emergency());

        UIHandler();


        if (rcRepeater == null || rcRepeater.isShutdown()) {
            rcRepeater = Executors.newSingleThreadScheduledExecutor();
            rcRepeater.scheduleWithFixedDelay(defaultSchedular, 0, 30, TimeUnit.SECONDS);
        }

        defineBuiltIntModules();

    }

    public static void defineAndruavUnit(final boolean isGCS)
    {
        AndruavSettings.andruavWe7daBase = new AndruavUnitMe(isGCS);
        AndruavSettings.andruavWe7daBase.setTelemetry_protocol(TelemetryProtocol.TelemetryProtocol_No_Telemetry);  // reset telemetry



        updateWe7daInfo();

        if (PreferenceValidator.isInvalidLoginCode())
        {
            AndruavSettings.AccessCode = "New Me";
        }
        else
        {
            AndruavSettings.AccessCode = Preference.getLoginAccessCode(null);
            AndruavSettings.AccountName = Preference.getLoginUserName(null);
        }
    }

    /***
     * A Scheduler Task
     */
    private final Runnable mSchedulRunnable = new Runnable()
    {
        long mlastTimeUDPBroadCast =0;
        @Override
        public void run() {

            try {

                if (m_Kill)
                {
                    mScheduleHandler.removeCallbacksAndMessages(null);
                    return ;
                }

                long now = System.currentTimeMillis();
                long diff = now - mlastTimeUDPBroadCast;

                if (diff > 2000) {
                       mlastTimeUDPBroadCast = now;
                    }

            }
            catch (Exception ex)
            {

            }
            finally {
                mScheduleHandler.postDelayed(this,1000);
            }
        }
    };

    private void getAppVersion() {

        PackageManager manager = this.getPackageManager();
        PackageInfo info;
        try {
            info = manager.getPackageInfo(this.getPackageName(), 0);
            App.versionName = info.versionName;

            // There was no Andruav installed here before.



           isFirstRun = Preference.getAppVersion(null).equals("");

           if (isFirstRun)
           {
               onFirstAndruavRun();
           }
            // This isthe first run of an updated version.
            isNewVersion = (Preference.getAppVersion(null).equals(App.versionName) == false);

            // HEFNY DEBUG
            //isNewVersion = true;
            if (isNewVersion)
            {
                onFirstUpdatedVersionRun(Preference.getAppVersion(null));
            }

            Preference.setAppVersion(null,App.versionName);




        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            App.versionName = "0.0.0";
        }
    }

    /***
     * This is the first run of Andruav ever on this machine.
     * or maybe after an Unintstall. It runs once.
     * <br>{@link #onFirstUpdatedVersionRun(String currentlyInstalledVersion)} will be called after this function.
     */
    public void onFirstAndruavRun ()
    {
        Preference.useStreamVideoHD(null,true);
        Preference.setFCBTargetLib(null, Preference.FCB_LIB_3DR);
        Preference.isEmergencyFlightModeFailSafeEnabled(null, 0); //FlightMode.CONST_FLIGHT_CONTROL_RTL);

        Preference.useUDPCamera(null,false);
        Preference.isRCBlockEnabled(null,false);
        Preference.setFPVActivityRotation(null,Surface.ROTATION_0);
        Preference.setRTCCamMirrored(null,false);
        Preference.setRTCCamRotateCAM(null,0);
        Preference.setVehicleType(null,VehicleTypes.VEHICLE_UNKNOWN);

        // GCS Data Block
        Preference.setChannelRCBlock(null,8);
        Preference.setChannelRCBlock_min_value(null,1500);
        Preference.setSerialServerPort(null,5760);

    }

    /***
     * isNewVersion is true.
     * This is the first run of an updated version. It runs once.
     */
    public void onFirstUpdatedVersionRun(String currentlyInstalledVersion)
    {

        Preference.setLoginAccessCode(null,"");
        Preference.isLocalServer(null,false);
        Preference.setFirstServer(null,0);
        Preference.useStreamVideoHD(null,true);
        Preference.FactoryReset_Tracker(null);
        Preference.setBattery_min_value(null, 0);
        Preference.setDefaultCircleRadius(null, 30);
        Preference.setDefaultClimbAlt(null, 30);
        Preference.isCommModuleIPAutoDetect(null,false);
        Preference.isGPSInjecttionEnabled(null,true);

        Preference.isMobileSensorsDisabled(null,false);

        Preference.setSmartMavlinkTelemetry(null, Constants.SMART_TELEMETRY_LEVEL_2);

        Preference.setSTUNServer(null,"");

        Preference.setModuleType(null, ProtocolHeaders.UAVOS_COMM_MODULE_CLASS);

        // this should be kept always
        Preference.FactoryReset_GUIWizard(null); // reset all ActivityMosa3ed Warnings
    }

    public static void shutDown()
    {
        try {

            m_Kill = true;
            AndruavFacade.sendShutDown(null);

            Thread.sleep(1000); // wait time to sendMessageToModule shut down message.


            EventBus.getDefault().post(new Event_ShutDown_Signalling(1));

            EventBus.getDefault().post(new Event_ShutDown_Signalling(2));

            EventBus.getDefault().post(new Event_ShutDown_Signalling(3));

            EventBus.getDefault().post(new Event_ShutDown_Signalling(4));

            App.stopUDPServer();
            App.stopSensorService();


        } catch (Exception ex) {
            AndruavEngine.log().logException("exception", ex);
        }
    }

    @Override
    public void onTerminate()
    {

        //AndruavReceiver.unregisterMe(getContext());

        mScheduleHandler.removeCallbacksAndMessages(null);
        mScheduleThread.interrupt();

        super.onTerminate();

    }

    /***
     * This is called when the overall system is running low on memory,
     * and actively running processes should trim their memory usage.
     * While the exact point at which this will be called is not defined,
     * generally it will happen when all background process have been killed.
     * That is, before reaching the point of killing processes hosting service
     * and foreground UI that we would like to avoid killing.
     * @see "http://developer.android.com/reference/android/app/Application.html"
     * You should implement this method to release any caches or other unnecessary resources you may be holding on to.
     * The system will perform a garbage collection for you after returning from this method.
     */
    @Override
    public void onLowMemory()
    {
        super.onLowMemory();
    }

    /***
     * Called when the operating system has determined that it is a good time for a process to trim unneeded memory from its process.
     * This will happen for example when it goes in the background and there is not enough memory to keep as many background processes running as desired.
     * You should never compare to exact values of the level, since new intermediate values may be added
     * -- you will typically want to compare if the value is greater or equal to a level you are interested in.
     * @param level
     */
    @Override
    public void onTrimMemory(int level)
    {
        super.onTrimMemory(level);
    }

    /***
     * restart Andruav  App
     * @param milliSeconds: ignored ... not used
     */
    public static void restartApp(int milliSeconds, boolean autoConnect)
    {
        restartApp_p(milliSeconds, autoConnect);
    }

    private static void restartApp_p(int milliSeconds, boolean autoConnect)
    {
        //AlarmManager mgr = (AlarmManager) App.context.getSystemService(Context.ALARM_SERVICE);
        //mgr.set(AlarmManager.RTC, System.currentTimeMillis() + milliSeconds, App.MainPendingIntent);

        Intent i = App.context.getPackageManager()
                .getLaunchIntentForPackage(  App.context.getPackageName() );
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.putExtra("autoconnect",autoConnect);
        App.context.startActivity(i);

        //System.exit(2);
    }



   public static void ForceLanguage()
    {

        Locale localLocale = new Locale(Preference.getPreferredLanguage(null));
        Locale.setDefault(localLocale);
        Configuration localConfiguration = new Configuration();
        localConfiguration.locale = localLocale;
        App.getAppContext().getResources().updateConfiguration(localConfiguration, null);

    }


    ///////////////////////////////////////////////// IPreference Interface
    @Override
    public boolean isAndruavLogEnabled() {
        return Preference.isAndruavLogEnabled(null);
    }

    @Override
    public String getVersionName() {
        return App.versionName;
    }

    @Override
    public Context getContext() {
        return App.context;
    }

    @Override
    public int getBattery_min_value() {
        return Preference.getBattery_min_value(null);
    }

    @Override
    public boolean isChannelReversed(int channelNumber) {
        return Preference.isChannelReversed(null,channelNumber);
    }

    @Override
    public boolean isChannelReturnToCenter(int channelNumber) {
        return Preference.isChannelReturnToCenter(null,channelNumber);
    }

    @Override
    public int getChannelmaxValue(int channelNumber) {
        return Preference.getChannelmaxValue(null,channelNumber);
    }

    @Override
    public int getChannelminValue(int channelNumber) {
        return Preference.getChannelminValue(null,channelNumber);
    }

    @Override
    public String TAG() {
        return "rcmobile.FPV";
    }

    @Override
    public String getLoginUserName()
    {
        return Preference.getLoginUserName(null);
    }

    @Override
    public String getModuleType () {return Preference.getModuleType(null);}


    @Override
    public String getCommModuleIP () {
        if (Preference.isCommModuleIPAutoDetect(null))
        {
            return "";
        }
        return Preference.getCommModuleIP(null);
    }

    @Override
    public void setCommModuleIP (final String commModuleIP) {Preference.setCommModuleIP(null, commModuleIP);}

    @Override
    public void post(Object object) {
        if (object == null) return ;
        EventBus.getDefault().post(object);
    }

    ///////////////////////////////////////////////// END OF Interface

}
