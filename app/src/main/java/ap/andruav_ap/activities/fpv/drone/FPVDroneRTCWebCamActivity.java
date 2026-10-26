

package ap.andruav_ap.activities.fpv.drone;

import org.greenrobot.eventbus.Subscribe;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.PictureInPictureParams;
import android.app.RemoteAction;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.drawable.Icon;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Bundle;
import android.util.Rational;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.andruav.AndruavEngine;
import com.andruav.AndruavSettings;
import com.andruav.event.droneReport_Event.Event_CameraZoom;
import com.andruav.event.droneReport_Event.Event_Vehicle_Flying_Changed;
import com.andruav.event.droneReport_Event.Event_Vehicle_Mode_Changed;
import com.andruav.event.fpv7adath._7adath_FPVStreamingStatusChanged;
import com.andruav.event.fpv7adath._7adath_InitAndroidCamera;
import com.andruav.event.fpv7adath._7adath_StopAndroidCamera;
import com.andruav.andruavUnit.AndruavUnitBase;
import com.andruav.event.fpv7adath.Event_FPV_CMD;
import com.andruav.FeatureSwitch;
import com.andruav.andruavUnit.AndruavUnitSystem;
import com.andruav.controlBoard.shared.common.FlightMode;

import org.webrtc.SurfaceViewRenderer;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.greenrobot.eventbus.EventBus;
import ap.andruav_ap.guiEvent.GUIEvent_EnableFlashing;
import ap.andruav_ap.widgets.AlarmWidget;
import ap.andruav_ap.App;
import ap.andruav_ap.DeviceManagerFacade;
import ap.andruav_ap.services.fpv.CameraController;
import ap.andruav_ap.services.fpv.FPVStreamingService;
import ap.andruavmiddlelibrary.factory.util.ActivityMosa3ed;
import ap.andruavmiddlelibrary.webrtc.events.Event_WebRTC;
import ap.andruav_ap.R;
import ap.andruavmiddlelibrary.sensors._7asasatEvents.Event_IMU_CMD;

import ap.andruavmiddlelibrary.preference.Preference;


public class FPVDroneRTCWebCamActivity extends Activity {

    private static final String ACTION_PIP_STOP = "ap.andruav_ap.fpv.ACTION_STOP_STREAM_PIP";

    private FPVDroneRTCWebCamActivity Me;
    private static Handler mHandle;
    private SurfaceViewRenderer mSurfaceViewRenderer;
    private FPVStreamingService mBoundService;
    private boolean mServiceBound = false;
    private AndruavUnitBase andruavUnit_selected;
    private final Object synObj = new Object();

    // Remembers each overlay control's visibility from just before entering PiP, so it can be
    // restored exactly (e.g. txtVideoStatus, which is independently toggled by connection status)
    // instead of blindly forcing everything back to VISIBLE.
    private final Map<View, Integer> mPrePipVisibility = new HashMap<>();

    private final BroadcastReceiver mPipStopReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_PIP_STOP.equals(intent.getAction())) {
                // Same single stop funnel as the on-screen stop button.
                EventBus.getDefault().post(new _7adath_StopAndroidCamera());
                finish();
            }
        }
    };

    ////// ActivityMosa3ed Variables
    private TextView txtVideoStatus;
    private Button btnZeroTilt;
    private Button btnCameraSwitch;
    private Button btnStopStream;

    int mImgWidth =0;
    int mImgHeight =0;


    private boolean skip = false;  // handle conncurrency in taking images


    private AlarmWidget alarmWidget;

    /**
     * ScheduledExecutorService used to periodically schedule the rcRepeater.
     */
    private ScheduledExecutorService rcRepeater;

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBoundService = ((FPVStreamingService.LocalBinder) service).getService();
            mServiceBound = true;
            mBoundService.attachRenderer(mSurfaceViewRenderer);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBoundService = null;
            mServiceBound = false;
        }
    };


    @Subscribe
    public void onEvent (final Event_CameraZoom adath_cameraZoom)
    {
        final Message msg = mHandle.obtainMessage();
        msg.obj = adath_cameraZoom;
        mHandle.sendMessageDelayed(msg,0);
    }


    @Subscribe
    public void onEvent (final GUIEvent_EnableFlashing guiEvent_enableFlashing)
    {
        if (guiEvent_enableFlashing.enableFlashing) {
            mHandle.post(new Runnable() {
                @Override
                public void run() {

                    if ((alarmWidget == null)) {

                        alarmWidget = new AlarmWidget(App.activeActivity);
                    }
                    alarmWidget.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT));
                    App.activeActivity.addContentView(alarmWidget, alarmWidget.getLayoutParams());


                }
            });
        }
        else
        {
            mHandle.post(new Runnable() {
                @Override
                public void run() {
                    if (alarmWidget != null) {
                        ActivityMosa3ed.removeMeFromParentView(alarmWidget);
                        alarmWidget = null;
                    }
                }
            });
        }


    }


    @Subscribe
    public void onEvent (final Event_Vehicle_Mode_Changed adath_vehicle_mode_changed)
    {

        if (!adath_vehicle_mode_changed.mAndruavWe7da.IsMe()) return ;

        final Message msg = mHandle.obtainMessage();
        msg.obj = adath_vehicle_mode_changed;
        mHandle.sendMessageDelayed(msg,0);
    }


    @Subscribe
    public void onEvent (final Event_Vehicle_Flying_Changed adath_vehicle_flying_changed)
    {

        if (!adath_vehicle_flying_changed.mAndruavWe7da.IsMe()) return ;

        final Message msg = mHandle.obtainMessage();
        msg.obj = adath_vehicle_flying_changed;
        mHandle.sendMessageDelayed(msg,0);
    }


    @Subscribe
    public void onEvent(final Event_FPV_CMD a7adath_fpv_CMD) {

        if(!DeviceManagerFacade.hasCamera()) return ;
        final Message msg = mHandle.obtainMessage();
        msg.obj = a7adath_fpv_CMD;
        mHandle.sendMessageDelayed(msg,0);
    }

    @Subscribe
    public void onEvent (final Event_WebRTC event_webRTC) {
        final Message msg = mHandle.obtainMessage();
        msg.obj = event_webRTC;
        mHandle.sendMessageDelayed(msg,0);
    }

    @Subscribe
    public void onEvent (final _7adath_FPVStreamingStatusChanged a7adath_fpvStreamingStatusChanged) {
        // FPVStreamingService stopped itself (remote STREAMVIDEO Act:false once no viewer remains,
        // or any other stop path) without going through our own stop button/finish(). Because this
        // Activity is launchMode="singleTask", leaving it open here means the next remote start
        // just re-fronts this stale instance via onNewIntent() (not overridden) instead of creating
        // a fresh one - streaming could never actually restart until the user manually pressed the
        // on-screen exit button. Close in step with the service so the next start works.
        if (!App.isFPVStreamingServiceRunning()) {
            finish();
        }
    }

    @Subscribe
    public void onEvent (final _7adath_InitAndroidCamera adath_initAndroidCamera) {
        // A remote start (camera-list request / STREAMVIDEO Act:true) that lands while this screen
        // is still up: BaseAndruavShasha is the only other subscriber and MainScreen unregisters
        // from the bus in onPause(), so with us in the foreground nobody else can act on it - and
        // singleTask re-fronting this instance runs no lifecycle callback that would restart
        // capture. Bring the service back ourselves instead.
        mHandle.post(() -> {
            if (isFinishing()) return; // closing in step with a stop that just happened - let it close.
            if (App.isFPVStreamingServiceRunning()) return;

            App.startFPVStreamingService();
            if (!mServiceBound) {
                bindService(new Intent(this, FPVStreamingService.class), mServiceConnection, Context.BIND_AUTO_CREATE);
            }
        });
    }


    private final Runnable defaultSchedular = new Runnable() {
        @Override
        public void run() {
            // issue server and connected to online

        }
    };


    /***
     * Event to UI gate to enable access UI safely.
     */
    private void UIHandler () {


        mHandle = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                if (msg.obj instanceof Event_CameraZoom)
                {
                    if (mBoundService == null || mBoundService.getPeerConnectionManager() == null) return;
                    Event_CameraZoom adath_cameraZoom = (Event_CameraZoom) msg.obj;
                    if (adath_cameraZoom.ZoomValue == Double.MAX_VALUE)
                    {
                        float zoom = mBoundService.getPeerConnectionManager().getZoom();
                        if (adath_cameraZoom.ZoomIn)
                        {
                            zoom +=adath_cameraZoom.ZoomValueStep;
                        }
                        else
                        {
                            zoom -=adath_cameraZoom.ZoomValueStep;
                        }
                        mBoundService.getPeerConnectionManager().setZoom(zoom);
                    }
                }

                else if (msg.obj instanceof Event_WebRTC) {
                    Event_WebRTC event_webRTC = (Event_WebRTC) msg.obj;
                    switch (event_webRTC.getEventType())
                    {
                        case Event_WebRTC.EVENT_CLOSED_CONNECTION:
                            txtVideoStatus.setVisibility(View.INVISIBLE);
                        break;
                        case Event_WebRTC.EVENT_CONNECT_SUCCEEDED:
                            txtVideoStatus.setText(App.context.getString(ap.andruavmiddlelibrary.R.string.action_video_on));
                            txtVideoStatus.setVisibility(View.VISIBLE);
                        break;
                        case Event_WebRTC.EVENT_CONNECTION_ERROR:
                            //txtVideoStatus.setText(App.context.getString(R.string.action_video_err));
                            //txtVideoStatus.setVisibility(View.VISIBLE);
                        break;
                    }
                }

                else if (msg.obj instanceof Event_Vehicle_Mode_Changed)
                {
                    final Event_Vehicle_Mode_Changed mode = (Event_Vehicle_Mode_Changed)msg.obj;
                    switch (mode.mAndruavWe7da.getFlightModeFromBoard())
                    {
                        case FlightMode.CONST_FLIGHT_CONTROL_RTL:
                        case FlightMode.CONST_FLIGHT_CONTROL_AUTO:
                        case FlightMode.CONST_FLIGHT_CONTROL_CRUISE:
                        case FlightMode.CONST_FLIGHT_CONTROL_GUIDED:
                            takeSSingleImage(new AndruavUnitSystem());
                            break;
                    }


                } else if (msg.obj instanceof Event_FPV_CMD) {
                    // Only UI-visible-only commands (need the live renderer) are handled here.
                    // FPV_CMD_FLASHCAM / FPV_CMD_SWITCHCAM / FPV_CMD_RECORDVIDEO are handled by
                    // FPVStreamingService directly so they aren't executed twice.
                    Event_FPV_CMD a7adath_FPV_CMD = (Event_FPV_CMD) msg.obj;
                    switch (a7adath_FPV_CMD.CMD_ID) {

                        // FPV_CMD_TAKEIMAGE is not handled here anymore:
                        // CameraController owns still capture end to end
                        // (headless - works with the screen off and no FPV
                        // screen open). This Activity only adds UI.

                        case Event_FPV_CMD.FPV_CMD_ROTATECAM:

                            break;
                    }

                }

            }
        };


        if (rcRepeater == null || rcRepeater.isShutdown()) {
            rcRepeater = Executors.newSingleThreadScheduledExecutor();
            rcRepeater.scheduleWithFixedDelay(defaultSchedular, 10, 30, TimeUnit.SECONDS);
        }
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);


    }

    private void initGUI ()
    {
        txtVideoStatus = findViewById(R.id.fpvactivity_txtVideoStatus);
        txtVideoStatus.setVisibility(View.INVISIBLE);

        mSurfaceViewRenderer = findViewById(R.id.fpvactivity_rtc_glviewsurface);
        mSurfaceViewRenderer.setEnableHardwareScaler(true);

        btnZeroTilt = findViewById(R.id.fpvactivity_btn_ZeroTilt);
        btnZeroTilt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (FeatureSwitch.DEBUG_MODE)
                {
                    if ((mBoundService != null) && (mBoundService.getPeerConnectionManager() != null)) {
                        mBoundService.getPeerConnectionManager().setFlash(mBoundService.getPeerConnectionManager().getFlash());
                    }
                }
                else {
                    EventBus.getDefault().post(new Event_IMU_CMD(Event_IMU_CMD.IMU_CMD_UpdateZeroTilt)); // tel IMU to reread Tilt
                }
            }
        });



        btnCameraSwitch = findViewById(R.id.fpvactivity_btn_CameraSwitch);
        btnCameraSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (andruavUnit_selected.UnitID.equals(AndruavSettings.andruavWe7daBase.UnitID)) {
                    if ((mBoundService != null) && (mBoundService.getPeerConnectionManager() != null)) {
                        mBoundService.getPeerConnectionManager().switchCamera();
                    }
                }
            }
        });

        btnStopStream = findViewById(R.id.fpvactivity_btn_StopStream);
        btnStopStream.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Single funnel for real stop - FPVStreamingService tears down capture/recording
                // and stops itself; the remote RemoteCommand_STREAMVIDEO Act:false path posts the
                // same event.
                EventBus.getDefault().post(new _7adath_StopAndroidCamera());
                finish();
            }
        });


    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Me = this;
        App.activeActivity = this;
        App.ForceLanguage();
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        Window win = getWindow();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_fpvdrone_rtcweb_cam);
        win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        andruavUnit_selected =  AndruavSettings.andruavWe7daBase;

        initGUI();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(mPipStopReceiver, new IntentFilter(ACTION_PIP_STOP), Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(mPipStopReceiver, new IntentFilter(ACTION_PIP_STOP));
        }

    }

    @Override
    public void onBackPressed() {
        // Don't close the FPV screen on back - shrink it into a floating Picture-in-Picture
        // window instead, like a video call, so the live feed stays visible while the pilot uses
        // the rest of the app. The stream only really stops via the explicit stop button (or the
        // system PiP window's own close button, see onDestroy()).
        if (!tryEnterPictureInPicture()) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        // Covers Home button / switching to recents the same way as back press.
        tryEnterPictureInPicture();
    }

    private boolean tryEnterPictureInPicture() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false;
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) return false;
        try {
            return enterPictureInPictureMode(buildPipParams());
        } catch (Exception e) {
            return false;
        }
    }

    private PictureInPictureParams buildPipParams() {
        final Rational aspect = Preference.isActiveCameraHD(null) ? new Rational(16, 9) : new Rational(4, 3);
        final PictureInPictureParams.Builder builder = new PictureInPictureParams.Builder().setAspectRatio(aspect);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setActions(Collections.singletonList(buildStopRemoteAction()));
        }
        return builder.build();
    }

    private RemoteAction buildStopRemoteAction() {
        final PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, 0, new Intent(ACTION_PIP_STOP).setPackage(getPackageName()),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        final Icon icon = Icon.createWithResource(this, R.drawable.signout_w_32x32);
        final CharSequence label = getString(ap.andruavmiddlelibrary.R.string.action_exit);
        return new RemoteAction(icon, label, label, pendingIntent);
    }

    /**
     * While floating in PiP, only the system-drawn stop action (see buildStopRemoteAction())
     * should be visible on the video - our own on-screen buttons/telemetry widgets are far too
     * small to use there and just clutter the tiny window. Hide them all on entering PiP, restore
     * each one's exact prior state (not just VISIBLE - e.g. txtVideoStatus toggles independently
     * based on connection status) on returning to full screen.
     */
    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);

        final ViewGroup root = (ViewGroup) mSurfaceViewRenderer.getParent();
        if (isInPictureInPictureMode) {
            for (int i = 0; i < root.getChildCount(); i++) {
                final View child = root.getChildAt(i);
                if (child == mSurfaceViewRenderer) continue;
                mPrePipVisibility.put(child, child.getVisibility());
                child.setVisibility(View.GONE);
            }
        } else {
            for (Map.Entry<View, Integer> entry : mPrePipVisibility.entrySet()) {
                entry.getKey().setVisibility(entry.getValue());
            }
            mPrePipVisibility.clear();
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        // Streaming is owned by FPVStreamingService now and keeps running regardless of this
        // Activity's lifecycle, so a restart simply resumes/rebinds - no forced re-init needed.
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        App.startFPVStreamingService();
        // Resuming from PiP (rather than a fresh onStart) leaves us still bound/registered from
        // before, since onPause() deliberately skips unbinding/unregistering while floating -
        // re-doing either here would double-bind, or throw (EventBus rejects a second register()
        // on an already-registered subscriber).
        if (!mServiceBound) {
            bindService(new Intent(this, FPVStreamingService.class), mServiceConnection, Context.BIND_AUTO_CREATE);
        }
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }

        App.startSensorService();

    }

    @Override
    protected void onPause() {

        try
        {
            if (!isFinishing() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode()) {
                // Still floating and visible in the PiP window - keep the renderer attached and
                // stay bound, otherwise the floating preview would go blank.
                super.onPause();
                return;
            }

            mHandle.removeCallbacksAndMessages(null);

            // Only detach the local-preview renderer - capture/publish/recording keep running in
            // FPVStreamingService regardless of this Activity's visibility, so screen-off/backgrounding
            // never interrupts the stream.
            if (mBoundService != null) {
                mBoundService.detachRenderer();
            }
            if (mServiceBound) {
                unbindService(mServiceConnection);
                mServiceBound = false;
                mBoundService = null;
            }

            EventBus.getDefault().unregister(this);

            if (rcRepeater != null ) {
                rcRepeater.shutdownNow();
                rcRepeater = null;
            }

            super.onPause();
        }
        catch (Exception e)
        {
            AndruavEngine.log().logException("rtc", e);
        }

    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mPipStopReceiver);
        if (isFinishing()) {
            // A real close - either the explicit stop button (which already posted this event
            // itself; posting again is harmless) or the system PiP window's own close (X) button,
            // which calls finish() directly with no other hook to intercept it.
            EventBus.getDefault().post(new _7adath_StopAndroidCamera());
        }
        super.onDestroy();

    }

    @Override
    public void onStart() {
        super.onStart();

        App.activeActivity = this;

        UIHandler();


    }


    private long lastSSImage =0;
    /***
     *
     * Take Silent Single Image - forwarded to CameraController (headless
     * still capture); this screen only throttles how often one is requested.
     * @param andruavUnitBase
     */
    private void takeSSingleImage(final AndruavUnitBase andruavUnitBase)
    {

        final long now = System.currentTimeMillis();
        if ((now - lastSSImage) < 20000) return; // dont take too close images.
        lastSSImage = now;
        CameraController.getInstance().captureSilent(andruavUnitBase);
    }

}
