

package ap.andruav_ap.activities.fpv.drone;

import org.greenrobot.eventbus.Subscribe;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.PictureInPictureParams;
import android.app.RemoteAction;
import android.content.DialogInterface;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
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

import com.andruav.AndruavFacade;
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
import com.andruav.interfaces.INotification;
import com.andruav.controlBoard.shared.common.FlightMode;
import com.andruav.notification.PanicFacade;
import com.andruav.protocol.commands.textMessages.AndruavMessage_Error;

import org.webrtc.EglRenderer;
import org.webrtc.SurfaceViewRenderer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.greenrobot.eventbus.EventBus;
import ap.andruav_ap.guiEvent.GUIEvent_EnableFlashing;
import ap.andruav_ap.widgets.AlarmWidget;
import ap.andruav_ap.widgets.flightControlWidgets.AndruavUnitInfoWidget;
import ap.andruav_ap.widgets.flightControlWidgets.AttitudeWidget;
import ap.andruav_ap.App;
import ap.andruav_ap.DeviceManagerFacade;
import ap.andruav_ap.services.fpv.FPVStreamingService;
import ap.andruavmiddlelibrary.eventClasses.fpvEvent.Event_FPV_Image;
import ap.andruavmiddlelibrary.factory.io.FileHelper;
import ap.andruavmiddlelibrary.factory.util.ActivityMosa3ed;
import ap.andruavmiddlelibrary.factory.util.Image_Helper;
import ap.andruavmiddlelibrary.webrtc.events.Event_WebRTC;
import ap.andruavmiddlelibrary.webrtc.classes.PeerConnectionManager;
import ap.andruav_ap.widgets.flightControlWidgets.NEWSWidget;
import ap.andruav_ap.R;
import ap.andruavmiddlelibrary.sensors._7asasatEvents.Event_IMU_CMD;

import ap.andruavmiddlelibrary.preference.Preference;


public class FPVDroneRTCWebCamActivity extends Activity {

    private static final String ACTION_PIP_STOP = "ap.andruav_ap.fpv.ACTION_STOP_STREAM_PIP";

    private final Object stateLock = new Object();

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

    private boolean mTakeImage = false;
    private int mTakeImageCount = 0;
    private boolean mSaveImageLocally = true;
    private long mTimeBetweenShots = 0;
    private double mDistanceBetweenShotes = 0; // ignored
    private boolean mSendBackImages = false;
    private AndruavUnitBase mSendBackTo = null; // name of Image requester

    /**
     * True until the source picker has been shown once for this Activity instance. Prevents
     * re-showing the picker on every onResume() (e.g. returning from PiP or from the permission
     * activity) — the user picks a source once on open and it sticks for the session.
     */
    private boolean mSourcePickerPending = true;


    ////// ActivityMosa3ed Variables
    private TextView txtVideoStatus;
    private Button btnZeroTilt;
    private Button btnCameraSwitch;
    private Button btnStopStream;
    private NEWSWidget newsWidget;
    private AttitudeWidget attitudeWidget;
    private AndruavUnitInfoWidget andruavUnitInfoWidget;

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
            // Apply a source choice that was made in the picker before the service bound.
            if (mPendingSource >= 0) {
                mBoundService.setRequestedSource(mPendingSource);
                mPendingSource = -1;
            }
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
        if (AndruavSettings.andruavWe7daBase.getIsCGS())
        {
            // you cannot switch screen for a user.
            return;
        }


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

                        case Event_FPV_CMD.FPV_CMD_TAKEIMAGE:
                            mTakeImageCount = a7adath_FPV_CMD.NumberOfImages;
                            mSaveImageLocally = a7adath_FPV_CMD.SaveImageLocally;
                            if (a7adath_FPV_CMD.TimeBetweenShotes ==0)
                            {  // in case the interval is Zero then make it one second by default.
                                a7adath_FPV_CMD.TimeBetweenShotes = 1;
                            }
                            mTimeBetweenShots = a7adath_FPV_CMD.TimeBetweenShotes * 1000 ; // convert to milliseconds
                            mDistanceBetweenShotes = a7adath_FPV_CMD.DistanceBetweenShotes;
                            mSendBackImages = a7adath_FPV_CMD.SendBackImages;

                            mSendBackTo = a7adath_FPV_CMD.Requester;

                            mTakeImage = true;
                            takeImage();
                           // PanicFacade.cannotStartCamera(INotification.NOTIFICATION_TYPE_ERROR,INotification.NOTIFICATION_TYPE_ERROR,"Take Image is not supported in this video mode yet",null);
                            break;


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
        attitudeWidget = findViewById((R.id.fpvactivity_widget_attitude));

        newsWidget = findViewById(R.id.NEWSWidget);


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
        // Long-press the camera-swap button to switch the capture source from camera to device
        // screen (MediaProjection). A long-press is used so the existing tap-to-swap behavior is
        // unchanged; screen capture needs an Activity-hosted permission dialog that camera swap
        // does not, so it has its own entry path.
        btnCameraSwitch.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (andruavUnit_selected.UnitID.equals(AndruavSettings.andruavWe7daBase.UnitID)) {
                    requestScreenCapture();
                    return true;
                }
                return false;
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


        andruavUnitInfoWidget = findViewById(R.id.fpvactivity_widget_andruavinfo);
        andruavUnitInfoWidget.setAndruavUnit(andruavUnit_selected);
        andruavUnitInfoWidget.isRecording = false;
        newsWidget.setAndruavUnit(andruavUnit_selected);
        attitudeWidget.setAndruavUnit(andruavUnit_selected);


    }


    /**
     * If a MediaProjection permission was already pre-granted (via
     * {@link ScreenCapturePermissionActivity}), starts screen-capture streaming immediately with
     * no dialog. Otherwise launches {@link ScreenCapturePermissionActivity} to obtain the
     * permission — the result is stored in {@link App#sScreenCaptureIntent} and a subsequent
     * long-press (or a remote stream request) will use it directly.
     * <p>
     * The permission request is delegated to a separate transparent activity because this
     * activity is locked to landscape and the system permission dialog forces a portrait rotation
     * that destroys the WebRTC EGL surfaces.
     */
    private void requestScreenCapture() {
        if (App.hasScreenCaptureIntent()) {
            // Already pre-granted — switch to screen capture now, no dialog.
            //
            // Deliberately NOT posting _7adath_StopAndroidCamera here: FPVStreamingService now
            // swaps the capturer in place on the existing PeerConnectionManager
            // (PeerConnectionManager.switchCaptureSource()) instead of tearing the whole pipeline
            // down. Stopping first would close every already-negotiated peer connection and send
            // connected browsers a hangup they have no way to auto-recover from (the web client
            // only ever (re)joins a stream from an explicit user click) - the in-place swap keeps
            // any already-joined viewer watching with no re-signaling at all.
            App.startFPVStreamingServiceScreenIfGranted();
            if (!mServiceBound) {
                bindService(new Intent(this, FPVStreamingService.class), mServiceConnection, Context.BIND_AUTO_CREATE);
            }
        } else {
            // No pre-grant yet — launch the transparent permission activity. It stores the result
            // in App.sScreenCaptureIntent and finishes. The user long-presses again (or a remote
            // request arrives) to actually start streaming.
            startActivity(new Intent(this, ScreenCapturePermissionActivity.class));
        }
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

    /**
     * Shows the video-source picker dialog (Back Camera / Front Camera / Screen) and starts
     * streaming with the selected source. For Screen, if no MediaProjection permission has been
     * pre-granted, launches {@link ScreenCapturePermissionActivity} instead — the permission
     * result triggers the service start via {@link App#startFPVStreamingServiceScreenIfGranted()}.
     */
    private void showSourcePicker() {
        final String[] items = {"Back Camera", "Front Camera", "Screen"};
        new AlertDialog.Builder(this)
                .setTitle("Select Video Source")
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                startWithSource(PeerConnectionManager.SOURCE_BACK);
                                break;
                            case 1:
                                startWithSource(PeerConnectionManager.SOURCE_FRONT);
                                break;
                            case 2:
                                startWithSource(PeerConnectionManager.SOURCE_SCREEN);
                                break;
                        }
                    }
                })
                .setCancelable(false)
                .show();
    }

    /**
     * Starts the FPV streaming service with the given source. For camera sources, this is a
     * plain {@link App#startFPVStreamingService()} after setting the requested source on the
     * service. For screen, it uses the pre-granted MediaProjection intent if available, or
     * launches the permission activity to obtain one.
     */
    private void startWithSource(final int source) {
        // Now that the user has chosen a source, force landscape — this was deferred from onCreate
        // so the picker dialog could show in portrait without being destroyed by the rotation.
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        // Now bind the service and register EventBus — this was deferred from onResume to avoid
        // the service's "not running" status event from finishing the activity while the picker
        // was still on screen.
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }

        if (source == PeerConnectionManager.SOURCE_SCREEN) {
            if (App.hasScreenCaptureIntent()) {
                App.startFPVStreamingServiceScreenIfGranted();
                if (!mServiceBound) {
                    bindService(new Intent(this, FPVStreamingService.class), mServiceConnection, Context.BIND_AUTO_CREATE);
                }
            } else {
                // Launch the permission activity; it stores the result and starts the service.
                // The service will bind when the permission activity returns and onResume fires.
                startActivity(new Intent(this, ScreenCapturePermissionActivity.class));
            }
        } else {
            // Camera source: start the service, then set the requested source so initRTC() picks
            // the right facing. The service may not be bound yet, so we set it via a start intent
            // extra isn't needed — the service reads mRequestedSource which we set after binding.
            App.startFPVStreamingService();
            if (!mServiceBound) {
                bindService(new Intent(this, FPVStreamingService.class), mServiceConnection, Context.BIND_AUTO_CREATE);
            }
            // Set the source after the service binds via onServiceConnected — but if it's already
            // bound (e.g. re-entry), set it now.
            if (mBoundService != null) {
                mBoundService.setRequestedSource(source);
            } else {
                // Store for onServiceConnected to apply.
                mPendingSource = source;
            }
        }
    }

    /** If non-negative, the source picked in the dialog that hasn't been applied yet (service not bound). */
    private int mPendingSource = -1;

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
     * Take Silent Single Image
     * @param andruavUnitBase
     */
    private void takeSSingleImage(final AndruavUnitBase andruavUnitBase)
    {

        final long now = System.currentTimeMillis();
        if ((now - lastSSImage) < 20000) return; // dont take too close images.
        lastSSImage = now;
        mTakeImageCount = 1;
        mSaveImageLocally = false;
        mTimeBetweenShots = 0; // convert to milliseconds
        mDistanceBetweenShotes = 0;
        mSendBackImages = true;
        mSendBackTo = andruavUnitBase;
        mTakeImage = true;
        takeImage();
    }

    private void takeImage()
    {
        if (mSurfaceViewRenderer ==null) return;
        mSurfaceViewRenderer.addFrameListener(fl ,1);
    }


    private final EglRenderer.FrameListener fl = new EglRenderer.FrameListener() {

        private final int exception_cam_mux_counter = 3;
        private long lastTime = 0;
        private final long mTimeBetweenBoof = 300;

        @Override
        public void onFrame(Bitmap bitmap) {
            synchronized (stateLock) {
                if (mTakeImageCount > 0) {
                    final long now = System.currentTimeMillis();
                    if (mTimeBetweenShots > 0) {  // dont take multiple images if time is equal to zero ... no burst images

                        if ((now - lastTime) >= mTimeBetweenShots) {
                            mTakeImageCount = mTakeImageCount - 1;
                            mTakeImage = true;
                            lastTime = now;
                        } else {
                            mTakeImage = false;
                        }

                        if (mTakeImageCount > 0) {
                            mHandle.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    if (mTakeImageCount == 0) return;
                                    takeImage();
                                }
                            }, mTimeBetweenShots);
                        }

                    } else {
                        // only a single image
                        mTakeImageCount = 0; // reset counter
                        mTakeImage = true;
                    }

                } else {
                    mTakeImage = false;
                }

                if (mTakeImage) {
                    try {
                        final String imageDescription = "Image No#" + mTakeImageCount;

                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                        byte[] pout = stream.toByteArray();
                        stream.flush();


                        AndruavFacade.sendImage(pout, AndruavSettings.andruavWe7daBase.getAvailableLocation(), mSendBackTo);
                        if (mSaveImageLocally) {
                            final Bitmap bitmap2 = Image_Helper.createBMPfromJPG(pout);
                            Bitmap rotatedBmp = Image_Helper.rotateImage(bitmap2, bitmap2.getWidth(), bitmap2.getHeight(), Preference.getFPVActivityRotation(null));

                            File savedImageFile;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                // Scoped storage: save under MediaStore Downloads instead of a File so
                                // the image survives uninstall and is browsable in a File Manager.
                                final String imgName = FileHelper.buildTimestampedJpgName(null);
                                final Uri savedImageUri = FileHelper.savePicToMediaStore(rotatedBmp, imgName, App.KMLFile.getImageRelativePath());
                                if (savedImageUri == null) {
                                    bitmap.recycle();
                                    rotatedBmp.recycle();
                                    return;
                                }
                                try (ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(savedImageUri, "rw")) {
                                    if (pfd != null) {
                                        Image_Helper.AddGPStoJpg(pfd.getFileDescriptor(), AndruavSettings.andruavWe7daBase.getAvailableLocation());
                                    }
                                } catch (IOException ex) {
                                    AndruavEngine.log().logException("exception_img", ex);
                                }
                                // MediaStore items have no real filesystem path - this File only ever
                                // needs to yield its name (KMLFileHandler.addImages() uses getName()).
                                savedImageFile = new File(imgName);
                            } else {
                                savedImageFile = FileHelper.savePic(rotatedBmp, null, App.KMLFile.getImageFolder());
                                if (savedImageFile == null) {
                                    // sendMessageToModule error messages to GCS Please
                                    bitmap.recycle();
                                    rotatedBmp.recycle();
                                    return;
                                }

                                Image_Helper.AddGPStoJpg(savedImageFile.getAbsolutePath(), AndruavSettings.andruavWe7daBase.getAvailableLocation());
                            }


                            Event_FPV_Image event_fpv_image = new Event_FPV_Image();
                            event_fpv_image.isLocalImage = true;
                            event_fpv_image.isVideo = false;
                            event_fpv_image.ImageFile = savedImageFile;
                            event_fpv_image.ImageLocation = AndruavSettings.andruavWe7daBase.getAvailableLocation();
                            event_fpv_image.Description = "Image No#" + mTakeImageCount;
                            EventBus.getDefault().post(event_fpv_image);

                            bitmap.recycle();
                            rotatedBmp.recycle();

                        }
                    } catch (Exception ex) {
                        AndruavEngine.log().logException(AndruavSettings.AccessCode, "exception_fpv2", ex);
                        PanicFacade.cannotStartCamera(INotification.NOTIFICATION_TYPE_ERROR, AndruavMessage_Error.ERROR_CAMERA, App.getAppContext().getString(com.andruav.protocol.R.string.andruav_error_camertakeimage), null);
                        skip = false;
                    }

                    skip = false;
                }

                mSurfaceViewRenderer.clearImage();
            }
        }

    };

}
