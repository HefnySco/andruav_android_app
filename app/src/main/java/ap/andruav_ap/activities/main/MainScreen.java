package ap.andruav_ap.activities.main;

import org.greenrobot.eventbus.Subscribe;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.hardware.usb.UsbManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.text.Html;
import android.util.Log;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorRes;
import androidx.core.content.ContextCompat;

import java.io.UnsupportedEncodingException;

import org.greenrobot.eventbus.EventBus;
import ap.andruav_ap.activities.HUBCommunication;
import ap.andruav_ap.activities.remote.RemoteControlSettingGCSActivityTab;
import ap.andruav_ap.activities.settings.SettingsDrone;
import ap.andruav_ap.activities.baseview.BaseAndruavShasha;
import ap.andruav_ap.activities.data.DataShashaTab;
import ap.andruav_ap.activities.drone.IMUShasha;
import ap.andruav_ap.activities.fcb.drone.FCB_AndruavShashaL2;
import ap.andruav_ap.activities.fpv.FPVActivityFactory;

import ap.andruav_ap.activities.remote.RemoteControlSettingActivityTab;
import ap.andruav_ap.App;


import com.andruav.AndruavInternalCommands;
import com.andruav.AndruavFacade;
import com.andruav.controlBoard.shared.common.VehicleTypes;

import ap.andruav_ap.helpers.CheckAppPermissions;
import ap.andruavmiddlelibrary.eventClasses.remoteControl.Event_ProtocolChanged;
import ap.andruavmiddlelibrary.LoginClient;
import ap.andruav_ap.communication.telemetry.TelemetryDroneProtocolParser;
import ap.andruav_ap.communication.telemetry.TelemetryGCSProtocolParser;
import ap.andruav_ap.communication.telemetry.TelemetryModeer;
import ap.andruav_ap.DeviceManagerFacade;

import com.andruav.event.fcb_event.Event_UDP_Proxy;
import com.andruav.event.fpv7adath._7adath_FPVStreamingStatusChanged;
import com.andruav.event.networkEvent.EventLoginClient;
import com.andruav.event.networkEvent.EventSocketState;

import ap.andruav_ap.guiEvent.GUIEvent_UpdateConnection;
import ap.andruav_ap.helpers.GUI;
import ap.andruav_ap.helpers.RemoteControl;
import com.andruav.FeatureSwitch;

import ap.andruavmiddlelibrary.preference.Preference;
import ap.andruavmiddlelibrary.preference.PreferenceValidator;
import ap.andruav_ap.R;
import ap.andruavmiddlelibrary.factory.communication.NetInfoAdapter;
import ap.andruavmiddlelibrary.factory.tts.TTS;
import ap.andruavmiddlelibrary.factory.util.GMail;
import ap.andruavmiddlelibrary.factory.tts.SoundManager;
import ap.andruavmiddlelibrary.factory.util.DialogHelper;
import ap.andruavmiddlelibrary.log.ExceptionHTTPLogger;

import com.andruav.AndruavEngine;
import com.andruav.andruavUnit.AndruavUnitBase;
import com.andruav.event.droneReport_Event.Event_GCSBlockedChanged;
import com.andruav.protocol.commands.textMessages.Andruav_2MR;

import com.andruav.AndruavSettings;
import com.andruav.interfaces.INotification;
import com.andruav.protocol.commands.textMessages.systemCommands.AndruavSystem_ConnectedCommServer;
import com.andruav.protocol.commands.textMessages.systemCommands.AndruavSystem_EnteredChatRoom;
import com.andruav.protocol.commands.textMessages.systemCommands.AndruavSystem_Ping;
import com.andruav.protocol.communication.websocket.AndruavWSClientBase;

import static com.andruav.protocol.communication.websocket.AndruavWSClientBase.SOCKETACTION_CONNECTING;
import static com.andruav.protocol.communication.websocket.AndruavWSClientBase.SOCKETSTATE_CONNECTED;
import static com.andruav.protocol.communication.websocket.AndruavWSClientBase.SOCKETSTATE_DISCONNECTED;
import static com.andruav.protocol.communication.websocket.AndruavWSClientBase.SOCKETSTATE_ERROR;
import static com.andruav.protocol.communication.websocket.AndruavWSClientBase.SOCKETSTATE_FREASH;
import static com.andruav.protocol.communication.websocket.AndruavWSClientBase.SOCKETSTATE_REGISTERED;


public class MainScreen extends BaseAndruavShasha {

    //////// Attributes

    //////  Variables
    private MainScreen Me;
    // https://developers.google.com/+/mobile/android/recommend
    // The request code must be 0 or greater.
    private final boolean pauseToExit = false;

    private ImageButton mBtnMenu;

    private View mCardServer;
    private View mDotServer;
    private TextView mTxtServerStatus;
    private TextView mTxtServerSubtitle;
    private TextView mBtnServerSignIn;
    private TextView mBtnServerConnect;
    private TextView mBtnServerDisconnect;

    private TextView mTxtTelemetryStatus;
    private View mGroupTelemetryLinked;
    private TextView mTxtDroneName;
    private TextView mTxtDroneSubtitle;
    private TextView mTxtGps;
    private TextView mTxtBattery;
    private TextView mTxtSignal;
    private TextView mTxtTelemetryHelper;

    // module grid tiles: container (tap target), icon (runtime-tinted), state caption
    private View mTileImu;
    private View mTileFpv;
    private View mTileCom;
    private View mTileFcb;
    private ImageView mIconImu;
    private ImageView mIconFpv;
    private ImageView mIconCom;
    private ImageView mIconFcb;
    private TextView mStateImu;
    private TextView mStateFpv;
    private TextView mStateCom;
    private TextView mStateFcb;

    // mirrors the enabled-state the old ActionBar menu items carried, applied to the new kebab popup rows
    private boolean mMenuActionsEnabled = true;
    private boolean mSettingsEnabled = true;

    private Handler mhandle;

    private Menu mMenu;
    private MenuItem miConnect;

    private String text;
    private final int MSG_TEXT = 1;
    private final int MSG_ICON = 2;
    private final int MSG_BOTH = 3;
    private final int MSG_DONE = 4;

    private AlertDialog mprogressDialog;
    private TextView mprogressDialogMessage;

    private boolean onFinalConnectionSucceededCalled = false;

    /***
     * Enabled when press disconnect button
     * to prevent a connection lost error message
     */
    private boolean isDisconnect = false;
    private boolean autoConnect = false;
    private int mRetryCount = 0;

    /***
     * true when the app is called from a USB device.
     */
    private boolean isUSBIntent = false;
    ////// EOF ActivityMosa3ed Variables


    /////////// EOF Attributes


    //////////BUS EVENT



    @Subscribe
    public void onEvent(Event_UDP_Proxy event_protocolChanged) {

        if (!event_protocolChanged.mAndruavWe7da.IsMe()) return ;

        Message msg = new Message();
        msg.obj = event_protocolChanged;
        mhandle.sendMessageDelayed(msg, 0);
    }

    @Subscribe
    public void onEvent (final Event_GCSBlockedChanged a7adath_gcsBlockedChanged)
    {

        Message msg = new Message();
        msg.obj = a7adath_gcsBlockedChanged;
        mhandle.sendMessageDelayed(msg, 0);
    }

    @Subscribe
    public void onEvent(Event_ProtocolChanged event_protocolChanged) {

        Message msg = new Message();
        msg.obj = event_protocolChanged;
        mhandle.sendMessageDelayed(msg, 0);
    }

    @Subscribe
    public void onEvent(GUIEvent_UpdateConnection guiEvent_updateConnection) {

        Message msg = new Message();
        msg.obj = guiEvent_updateConnection;
        mhandle.sendMessageDelayed(msg, 0);
    }

    @Subscribe
    public void onEvent(final _7adath_FPVStreamingStatusChanged a7adath_fpvStreamingStatusChanged) {

        Message msg = new Message();
        msg.obj = a7adath_fpvStreamingStatusChanged;
        mhandle.sendMessageDelayed(msg, 0);
    }

    @Subscribe
    public void onEvent(EventLoginClient event_LoginClient) {

        Message msg = new Message();
        msg.obj = event_LoginClient;
        mhandle.sendMessageDelayed(msg, 0);
    }


    @Subscribe
    public void onEvent(Andruav_2MR Andruav_2MR) {

        // sendMessageToModule all System messages and some of Communication messages
        Message msg = new Message();

        if (!Andruav_2MR.MessageRouting.equals(AndruavWSClientBase.MESSAGE_TYPE_SYSTEM)) {
            // only system message passes.
            return;
        }
        msg.obj = Andruav_2MR;
        mhandle.sendMessageDelayed(msg, 0);
    }


    @Subscribe
    public void onEvent(EventSocketState event) {
        Message msg = new Message();
        msg.obj = event;
        mhandle.sendMessageDelayed(msg, 100);
    }

    ///////////////////






    private void doProgressDialog() {

        if ((mprogressDialog != null) && (mprogressDialog.isShowing())) {
            // already showing while retrying: keep it up instead of flashing it off & on.
            return;
        }

        mRetryCount = 0;

        final View view = LayoutInflater.from(this).inflate(R.layout.dialog_connect_progress, null);
        mprogressDialogMessage = view.findViewById(R.id.dialogconnect_txtMessage);
        mprogressDialogMessage.setText(getString(ap.andruavmiddlelibrary.R.string.gen_step1) + getString(ap.andruavmiddlelibrary.R.string.action_init));

        final Button btnStop = view.findViewById(R.id.dialogconnect_btnStop);
        btnStop.setOnClickListener(v -> stopConnectionRetry());

        mprogressDialog = new AlertDialog.Builder(Me).setView(view).setCancelable(false).create();
        if (mprogressDialog.getWindow() != null) {
            mprogressDialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
        mprogressDialog.show();
    }

    /***
     * Stops any pending connect/reconnect attempt so the user can change login settings
     * instead of waiting for a retry cycle to end.
     */
    private void stopConnectionRetry() {
        mRetryCount = 0;
        exitProgressDialog();
        doLogout();
        EventBus.getDefault().post(new EventSocketState(EventSocketState.ENUM_SOCKETSTATE.onDisconnect, "manual closing"));
    }

    protected void progressDialogSetMessage(CharSequence message) {
        if (mprogressDialogMessage != null) {   // as we can reach this point with other messages when  the dialog is off
            // http://localhost:8080/mantis/view.php?id=30

            mprogressDialogMessage.setText(message);
        }
    }


    private void exitProgressDialog() {
        //Update UI here if needed
        if (mprogressDialog != null) {
            mprogressDialog.dismiss();
        }
    }


    private void UIHandler() {

        mhandle = new Handler() {
            @Override
            public void handleMessage(Message msg) {

                if (msg.obj instanceof Event_ProtocolChanged)
                {
                    updateFCBButton();
                }
                else if (msg.obj instanceof GUIEvent_UpdateConnection)
                {
                    // FCB connect/disconnect (e.g. FcbConnectionSheet's Disconnect button)
                    // posts this - refresh the FCB/IMU/FPV tiles and Flight Telemetry card.
                    updateFCBButton();
                }
                else if (msg.obj instanceof _7adath_FPVStreamingStatusChanged)
                {
                    updateFPVButton();
                }
                else if (msg.obj instanceof Event_UDP_Proxy)
                {
                    Event_UDP_Proxy event_udp_proxy = (Event_UDP_Proxy) msg.obj;
                    if (AndruavSettings.andruavWe7daBase.isUdpProxyEnabled()) {
                        AndruavEngine.notification().Speak(getString(com.andruav.protocol.R.string.udp_proxy_en));
                    }
                    else
                    {
                        AndruavEngine.notification().Speak(getString(com.andruav.protocol.R.string.udp_proxy_dis));
                    }

                    AndruavFacade.sendUdpProxyStatus(null);

                }
                else if (msg.obj instanceof Event_GCSBlockedChanged)
                {
                    updateFCBButton();
                }
                else if (msg.obj instanceof EventLoginClient) {   //Event Login Client Handling

                    EventLoginClient eventLoginClient = (EventLoginClient) msg.obj;
                    switch (eventLoginClient.LastError) {
                        case LoginClient.ERR_SUCCESS:
                            exitProgressDialog();
                            AndruavSettings.Account_SID = eventLoginClient.Parameters.get(LoginClient.CONST_SENDER_ID);
                            AndruavSettings.andruavWe7daBase.setPermissions(eventLoginClient.Parameters.get(LoginClient.CONST_PERMISSION));
                            if ((!AndruavSettings.andruavWe7daBase.canBeGCS() && AndruavSettings.andruavWe7daBase.getIsCGS())
                                || (!AndruavSettings.andruavWe7daBase.canBeDrone() && !AndruavSettings.andruavWe7daBase.getIsCGS()))
                            {
                                // cannot be in this Mode
                                progressDialogSetMessage(getString(ap.andruavmiddlelibrary.R.string.gen_step2) + getString(ap.andruavmiddlelibrary.R.string.err_per_bad_mode));
                                AndruavEngine.notification().Speak(getString(ap.andruavmiddlelibrary.R.string.err_per_bad_mode));
                                return ;
                            }

                            // used when logging using authentication server.
                            // till now the only authentication server is Andruav.com
                            AndruavSettings.WebServerURL = eventLoginClient.Parameters.get(LoginClient.CONST_COMM_SERVER_PUBLIC_HOST);
                            AndruavSettings.WebServerPort = eventLoginClient.Parameters.get(LoginClient.CONST_COMM_SERVER_PORT);
                            AndruavSettings.WEBMOFTA7 = eventLoginClient.Parameters.get(LoginClient.CONST_COMM_SERVER_LOGIN_TEMP_KEY);

                            progressDialogSetMessage(getString(ap.andruavmiddlelibrary.R.string.gen_step2) + getString(ap.andruavmiddlelibrary.R.string.gen_accesscodevalid));
                            AndruavEngine.notification().Speak(getString(ap.andruavmiddlelibrary.R.string.gen_accesscodevalid));
                            startAndruavConnection();
                            break;
                        case LoginClient.ERR_OLD_APP_VERSION:
                            exitProgressDialog();
                            AndruavEngine.notification().Speak(getString(ap.andruavmiddlelibrary.R.string.err_old_app_version));
                            final String appPackageName = getPackageName(); // getPackageName() from Context or Activity object
                            try {
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                            } catch (android.content.ActivityNotFoundException anfe) {
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + appPackageName)));
                            }
                            break;
                        case LoginClient.ERR_SUCCESS_DISPLAY_MESSAGE:
                            exitProgressDialog();
                            AndruavEngine.notification().Speak(eventLoginClient.LastMessage);
                            DialogHelper.doModalDialog(Me, "Login", eventLoginClient.LastMessage, null);
                            break;
                        case LoginClient.ERR_SERVER_UNREACHABLE:
                            mRetryCount++;
                            progressDialogSetMessage(getString(ap.andruavmiddlelibrary.R.string.gen_retrying) + " (" + mRetryCount + ")");
                            mhandle.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    if (pauseToExit || isDisconnect)
                                    {
                                        // user pressed Stop while we were waiting to retry.
                                        exitProgressDialog();
                                        return;
                                    }
                                    doLogin(true);

                                }
                            },1000);
                            break;
                        default:
                            Intent intent = new Intent(Me, GUI.getLoginActivity());
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            intent.putExtra(LoginClient.CONST_ERROR, (byte) LoginClient.ERR_ACCOUNT_NOT_FOUND);
                            startActivity(intent);
                            break;
                    }

                }   // EOF EventLoginClient
                else if (msg.obj instanceof EventSocketState) {
                    EventSocketState eventSocketState = (EventSocketState) msg.obj;
                    if (eventSocketState.SocketState == EventSocketState.ENUM_SOCKETSTATE.onConnect) {
                        updateConnectionIconsStatus(AndruavEngine.getAndruavWSStatus(), AndruavEngine.getAndruavWSAction());
                        progressDialogSetMessage(getString(ap.andruavmiddlelibrary.R.string.gen_step3) + getString(ap.andruavmiddlelibrary.R.string.gen_hostfound));
                    } else if (eventSocketState.SocketState == EventSocketState.ENUM_SOCKETSTATE.onDisconnect) {
                        if (isDisconnect || (mprogressDialog == null) || (!mprogressDialog.isShowing())) {
                            // user-initiated stop, or we were not in the middle of a connect attempt.
                            exitProgressDialog();
                        } else {
                            // mid-connect failure: keep the dialog (with its Stop button) up while it retries in the background.
                            mRetryCount++;
                            progressDialogSetMessage(getString(ap.andruavmiddlelibrary.R.string.gen_retrying) + " (" + mRetryCount + ")");
                        }
                        updateConnectionIconsStatus(AndruavEngine.getAndruavWSStatus(), AndruavEngine.getAndruavWSAction());


                    } else if (eventSocketState.SocketState == EventSocketState.ENUM_SOCKETSTATE.onError) {
                        if (isDisconnect || (mprogressDialog == null) || (!mprogressDialog.isShowing())) {
                            exitProgressDialog();
                        } else {
                            mRetryCount++;
                            progressDialogSetMessage(getString(ap.andruavmiddlelibrary.R.string.gen_retrying) + " (" + mRetryCount + ")");
                        }
                        updateConnectionIconsStatus(AndruavEngine.getAndruavWSStatus(), AndruavEngine.getAndruavWSAction());


                    } else if (eventSocketState.SocketState == EventSocketState.ENUM_SOCKETSTATE.onMessage) {

                    }
                } else if (msg.obj instanceof Andruav_2MR) {
                    //onFinalConnectionSucceededCalled = false;
                    Andruav_2MR eventWSComm = (Andruav_2MR) msg.obj;

                    if ((eventWSComm.IsReceived) && (eventWSComm.MessageRouting.equals(AndruavWSClientBase.MESSAGE_TYPE_SYSTEM))) { // Pass system messages only as this part is used for login.
                        if (eventWSComm.andruavMessageBase instanceof AndruavSystem_ConnectedCommServer) {
                            onFinalConnectionSucceededCalled = false;
                            if (!eventWSComm.IsErr) {
                                text = getString(ap.andruavmiddlelibrary.R.string.gen_step4) + getString(ap.andruavmiddlelibrary.R.string.gen_hostfound);
                            } else {
                                text = "<br><font color=#F75050>" + getString(ap.andruavmiddlelibrary.R.string.err_hostnotfound) + "</font>";
                                AndruavEngine.notification().Speak(getString(ap.andruavmiddlelibrary.R.string.gen_connectionlost));
                            }
                            progressDialogSetMessage(Html.fromHtml(text));
                            if (!eventWSComm.IsErr) {
                                text += "<br><font color=#75A4D3>" + getString(ap.andruavmiddlelibrary.R.string.gen_ws_registered) + "</font'>";
                                updateConnectionIconsStatus(AndruavEngine.getAndruavWSStatus(),AndruavEngine.getAndruavWSAction());
                                AndruavEngine.getAndruavWS().sendPing();

                            } else {
                                text += "<br><font color=#F75050>" + getString(ap.andruavmiddlelibrary.R.string.err_ws_replicatedid) + "</font'>";
                                AndruavEngine.notification().Speak(getString(ap.andruavmiddlelibrary.R.string.err_ws_replicatedid));
                                AndruavEngine.getAndruavWS().disconnect();
                            }

                            progressDialogSetMessage(Html.fromHtml(text));
                            return;

                        }

                        //text = "<br><font color=#36AB36>" + eventWSComm.GroupName + "</font>.<font color=#75A4D3>" + eventWSComm.PartyID + "</font>.";
                        text = "<br><font color=#36AB36>" + eventWSComm.groupName + "</font>.<font color=#75A4D3>" + AndruavSettings.andruavWe7daBase.UnitID + "</font>.";

                        if ((eventWSComm.andruavMessageBase instanceof AndruavSystem_EnteredChatRoom)) {
                            if (!eventWSComm.IsErr) {
                                text += "<br><font color=#75A4D3>" + getString(ap.andruavmiddlelibrary.R.string.gen_ws_registered) + "</font'>";
                                updateConnectionIconsStatus(AndruavEngine.getAndruavWSStatus(),AndruavEngine.getAndruavWSAction());
                                AndruavEngine.getAndruavWS().sendPing();

                            } else {
                                text += "<br><font color=#F75050>" + getString(ap.andruavmiddlelibrary.R.string.err_ws_replicatedid) + "</font'>";
                                AndruavEngine.notification().Speak(getString(ap.andruavmiddlelibrary.R.string.err_ws_replicatedid));
                                AndruavEngine.getAndruavWS().disconnect();
                            }

                            progressDialogSetMessage(Html.fromHtml(text));

                        } else if (eventWSComm.andruavMessageBase instanceof AndruavSystem_Ping) {
                            if (!eventWSComm.IsErr) {


                                text += "<br><font color=#75A4D3>" + "msg duration: " + eventWSComm.timeStamp + "ms" + "</font'>";
                                Toast.makeText(Me, "Ping time " + eventWSComm.timeStamp + "ms", Toast.LENGTH_LONG).show();
                                if (!onFinalConnectionSucceededCalled) onFinalConnectionSucceeded();
                                /* always start it as u may want to track GCS in follow me forexample
                                if (App.isCGS == false) {
                                    App.startSensorService();
                                }*/
                            } else {
                                text += "<br><font color=F75050>" + getString(ap.andruavmiddlelibrary.R.string.err_ws_cmd_ping) + "</font'>";
                            }

                            progressDialogSetMessage(Html.fromHtml(text));

                            exitProgressDialog();

                        }
                    }  // end IsRecieved = true
                }
            }
        };
    }

    /***
     * Called after Register and successful ping
     */
    private void onFinalConnectionSucceeded() {
        App.nextTimeEvent =0; //reset
        App.isDone =false; //reset
        onFinalConnectionSucceededCalled = true;
        AndruavSettings.andruavWe7daBase.setShutdown(false);
        AndruavFacade.broadcastID(); // tell them I am online
        AndruavFacade.requestID();  // guys !! who are there ?
        AndruavFacade.sendID((AndruavUnitBase)null); // guys I am here
        App.startSensorService();



        if (AndruavSettings.andruavWe7daBase.getIsCGS())
        {
            ap.andruavmiddlelibrary.Voting.onConnectToServer();


        }
        else
        {
            AndruavSettings.loadGenericPermanentTasks();
            AndruavSettings.loadMyPermanentTasksByPartyID();
        }

        if (Preference.isAutoFCBConnect(null))
        {
            TelemetryModeer.connectToPreferredConnection(Me,false);
        }
    }


    private void init() {
    }

    private void initGUI() {


        if (isInEditMode) return;

        mBtnMenu = findViewById(R.id.home_btn_menu);
        mBtnMenu.setOnClickListener(this::showOverflowMenu);

        mCardServer = findViewById(R.id.home_card_server);
        mDotServer = findViewById(R.id.home_dot_server);
        mTxtServerStatus = findViewById(R.id.home_txt_server_status);
        mTxtServerSubtitle = findViewById(R.id.home_txt_server_subtitle);
        mBtnServerSignIn = findViewById(R.id.home_btn_server_signin);
        mBtnServerConnect = findViewById(R.id.home_btn_server_connect);
        mBtnServerDisconnect = findViewById(R.id.home_btn_server_disconnect);
        mBtnServerSignIn.setOnClickListener(v -> doSignOut());
        mBtnServerConnect.setOnClickListener(v -> onServerConnectToggle());
        mBtnServerDisconnect.setOnClickListener(v -> onServerConnectToggle());
        mTxtServerSubtitle.setText(Preference.getAuthServerURL(null));

        mTxtTelemetryStatus = findViewById(R.id.home_txt_telemetry_status);
        mGroupTelemetryLinked = findViewById(R.id.home_group_telemetry_linked);
        mTxtDroneName = findViewById(R.id.home_txt_drone_name);
        mTxtDroneSubtitle = findViewById(R.id.home_txt_drone_subtitle);
        mTxtGps = findViewById(R.id.home_txt_gps);
        mTxtBattery = findViewById(R.id.home_txt_battery);
        mTxtSignal = findViewById(R.id.home_txt_signal);
        mTxtTelemetryHelper = findViewById(R.id.home_txt_telemetry_helper);

        mTileImu = findViewById(R.id.home_tile_imu);
        mTileFpv = findViewById(R.id.home_tile_fpv);
        mTileCom = findViewById(R.id.home_tile_com);
        mTileFcb = findViewById(R.id.home_tile_fcb);
        mIconImu = findViewById(R.id.home_icon_imu);
        mIconFpv = findViewById(R.id.home_icon_fpv);
        mIconCom = findViewById(R.id.home_icon_com);
        mIconFcb = findViewById(R.id.home_icon_fcb);
        mStateImu = findViewById(R.id.home_state_imu);
        mStateFpv = findViewById(R.id.home_state_fpv);
        mStateCom = findViewById(R.id.home_state_com);
        mStateFcb = findViewById(R.id.home_state_fcb);

        mTileImu.setOnClickListener(v -> startActivity(new Intent(MainScreen.this, IMUShasha.class)));
        mTileFpv.setOnClickListener(v -> FPVActivityFactory.startFPVActivity(MainScreen.this));
        mTileCom.setOnClickListener(v -> startActivity(new Intent(MainScreen.this, HUBCommunication.class)));
        mTileFcb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (!AndruavSettings.andruavWe7daBase.getIsCGS()) {
                    if ((App.telemetryProtocolParser instanceof TelemetryGCSProtocolParser))
                    {
                        App.telemetryProtocolParser.shutDown();
                        App.telemetryProtocolParser = null;
                    }

                    FcbConnectionSheet.newInstance().show(getSupportFragmentManager(), FcbConnectionSheet.TAG);
                }

            }
        });

        // Define UI Handler
        UIHandler();

        App.notification.showSnack(INotification.NOTIFICATION_TYPE_NORMAL, "Info", "Andruav version: " + App.versionName);

        updateServerCard();
        updateModuleTiles();
    }




    /**
     * Login to AndruavAuthenticator
     * @param Connect
     */
    protected void doLogin(boolean Connect) {
        Log.d("ws","doLogin");

        onFinalConnectionSucceededCalled = false;

        /* GARBAGE http://www.scriptscoop.net/t/9fc9e0b24f55/how-can-i-turn-off-on-4g-data-programmatically-on-android.html
        try {


            final ConnectivityManager conman = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        final Class conmanClass = Class.forName(conman.getClass().getName());
        final Field iConnectivityManagerField = conmanClass.getDeclaredField("mService");
        iConnectivityManagerField.setAccessible(true);
        final Object iConnectivityManager = iConnectivityManagerField.get(conman);
        final Class iConnectivityManagerClass = Class.forName(iConnectivityManager.getClass().getName());
        final Method setMobileDataEnabledMethod = iConnectivityManagerClass.getDeclaredMethod("isNetworkTypeValid", Integer.TYPE);
        setMobileDataEnabledMethod.setAccessible(true);

            setMobileDataEnabledMethod.invoke(iConnectivityManager, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        */

        //final JSch jsch = new JSch();
        //SshConnection ssh = new SshConnection("192.168.1.2","ss","ss",null);
        if (!AndruavSettings.andruavWe7daBase.getIsCGS()) {
            RemoteControl.loadDualRates();
        }



//        if (DeviceManagerFacade.isRunningOnEmulator()) {
//            AndruavEngine.log().logException(AndruavSettings.Account_SID, "emulator", new Exception("I am on a simulator"));
//
//        }

        if (AndruavEngine.isAndruavWSStatus(SOCKETSTATE_REGISTERED)) {
            return; // I am already connected.
        }


        if (!PreferenceValidator.isValidWebRTC())
        {
            DialogHelper.doModalDialog(this, getString(ap.andruavmiddlelibrary.R.string.pref_gr_fpv_stunserver), getString(ap.andruavmiddlelibrary.R.string.err_rtc_nostunserver), null,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {

                            doSettings();
                        }
                    });
            return;
        }



        // Test IP connection Including Tethering
        NetInfoAdapter.Update();

        if (!NetInfoAdapter.isConnected()) {   // No Internet Access
            Log.d("ws","isConnected is false");
            DialogHelper.doModalDialog(this, getString(ap.andruavmiddlelibrary.R.string.gen_connection), getString(ap.andruavmiddlelibrary.R.string.err_no_internet), null);
            return;
        }

        if (Connect) {
            Log.d("ws","isConnected Connect");

            AndruavInternalCommands.init();
            isDisconnect = false;

            if (PreferenceValidator.isInvalidLoginCode()) {
                // you need internt connection here to register for the first time
                if ((!FeatureSwitch.IGNORE_NO_INTERNET_CONNECTION) && (!NetInfoAdapter.isHasValidIPAddress())) {   // No Internet Access
                    DialogHelper.doModalDialog(this, getString(ap.andruavmiddlelibrary.R.string.gen_connection), getString(ap.andruavmiddlelibrary.R.string.err_no_internet), null);
                    //   AndruavMo7arek.log().log(AndruavSettings.AccessCode, "No-Net", NetInfoAdapter.DebugStr);

                    return;
                }

                startActivity(new Intent(MainScreen.this, GUI.getLoginActivity()));

                return;
            }


            // Connect Globally
            try {
                AndruavSettings.AuthIp = Preference.getAuthServerURL(null);
                AndruavSettings.AuthPort =Preference.getAuthServerPort(null);

                LoginClient.ValidateAccount(Preference.getLoginUserName(null), Preference.getLoginAccessCode(null),Preference.getWebServerGroupName(null), null);

                doProgressDialog();
            } catch (UnsupportedEncodingException e) {
                AndruavEngine.log().logException("exception_log", e);
                DialogHelper.doModalDialog(this, getString(ap.andruavmiddlelibrary.R.string.action_login), getString(ap.andruavmiddlelibrary.R.string.err_loginfailed), null);
            }

        }
    }

    /***
     * logout from AndruavAuthenticator & AndruavServer
     */
    protected void doLogout() {

        isDisconnect = true;
        AndruavSettings.andruavWe7daBase.setShutdown(true);
        App.stopAndruavWS(false);

        if (AndruavSettings.andruavWe7daBase.getIsCGS())
        {
            ap.andruavmiddlelibrary.Voting.onDisconnecFromServerSafely();
        }

        AndruavSettings.andruavWe7daBase.getMohemmaMapBase().clear();
        AndruavEngine.getAndruavWe7daMapBase().clear();
        App.stopSensorService();
    }

    protected void updateConnectionIconsStatus(final int status, final int  action) {
        if (miConnect == null) return; // issue 42
        updateButtonsStatus(status, action);
        updateServerCard();
        updateModuleTiles(); // Com tile (and dependents) track server link too - keep in sync.
        switch (status) {
            case SOCKETSTATE_CONNECTED:
                miConnect.setIcon(R.drawable.connected_w_32x32);
              break;
            case SOCKETSTATE_DISCONNECTED:
                miConnect.setIcon(R.drawable.connect_w_32x32);
                AndruavEngine.notification().Speak(getString(ap.andruavmiddlelibrary.R.string.gen_connectiondisconnected));
                break;
            case SOCKETSTATE_ERROR:
                if (!isDisconnect) {
                    miConnect.setIcon(R.drawable.connected_error_32x32);
               } else {
                    miConnect.setIcon(R.drawable.connect_w_32x32);
                    AndruavEngine.notification().Speak(getString(ap.andruavmiddlelibrary.R.string.gen_connectiondisconnected));
                }
                break;
            case SOCKETSTATE_FREASH:
                miConnect.setIcon(R.drawable.connect_w_32x32);
                App.soundManager.playSound(SoundManager.SND_ERR);
                break;
            case SOCKETSTATE_REGISTERED:
                miConnect.setIcon(R.drawable.connected_color_32x32);
                break;
        }
    }

    protected void updateButtonsStatus(final int status, final int action) {

        boolean enabled = true;
        boolean BigButtonsenabled = true;

        if (action == SOCKETACTION_CONNECTING)
        {
            BigButtonsenabled = false;
            enabled = false;
            if (AndruavSettings.andruavWe7daBase.getIsCGS()) {
                mTileFcb.setEnabled(false);
            }

        }
        else {

            switch (status) {
                case SOCKETSTATE_CONNECTED:
                    BigButtonsenabled = false;
                    enabled = false;
                    if (AndruavSettings.andruavWe7daBase.getIsCGS()) {
                        mTileFcb.setEnabled(false);
                    }
                    break;
                case SOCKETSTATE_REGISTERED:
                    enabled = false;
                    BigButtonsenabled = true;
                    if (AndruavSettings.andruavWe7daBase.getIsCGS()) {
                        mTileFcb.setEnabled(AndruavSettings.andruavWe7daBase.canTelemetry());
                    }

                    break;

                case SOCKETSTATE_ERROR:
                    enabled = true;
                    BigButtonsenabled = false;
                    if (AndruavSettings.andruavWe7daBase.getIsCGS()) {
                        mTileFcb.setEnabled(false);
                    }
                    break;
                case SOCKETSTATE_FREASH:
                    if (AndruavSettings.andruavWe7daBase.getIsCGS()) {
                        mTileFcb.setEnabled(false);
                    }
                    enabled = true;
                    BigButtonsenabled = true;
                    break;
                case SOCKETSTATE_DISCONNECTED:
                    enabled = true;
                    BigButtonsenabled = true;
                    if (AndruavSettings.andruavWe7daBase.getIsCGS()) {
                        mTileFcb.setEnabled(false);
                    }
                    break;
            }

        }
        mMenu.findItem(R.id.mi_main_ResetFactory).setEnabled(enabled);
        mMenu.findItem(R.id.mi_main_signout).setEnabled(enabled);
        mMenuActionsEnabled = enabled;

        mTileCom.setEnabled(BigButtonsenabled && enabled);

        if (!AndruavSettings.andruavWe7daBase.getIsCGS()) {
            mTileFcb.setEnabled(BigButtonsenabled);
            mTileImu.setEnabled(BigButtonsenabled && enabled);
            mTileFpv.setEnabled(BigButtonsenabled);

            mMenu.findItem(R.id.mi_main_Settings_drone).setEnabled(enabled);
            mSettingsEnabled = enabled;
        }
        else
        {

            mTileImu.setEnabled(false);
            mTileFpv.setEnabled(false);

            mMenu.findItem(R.id.mi_main_Settings_drone).setEnabled(false);
            mSettingsEnabled = false;
        }
    }

    protected void updateFCBButton() {
        updateModuleTiles();
    }

    protected void updateFPVButton() {
        updateModuleTiles();
    }

    /***
     * Recomputes the IMU/FPV/Com/FCB module tile styling and the Flight Telemetry
     * card from current connection state, per the Home screen redesign state rules:
     * IMU/FPV follow the FCB link, Com follows the Mission Server link, FCB reflects
     * both (green when linked, blue when server-connected but not yet linked, gray
     * otherwise).
     */
    private void updateModuleTiles() {
        final boolean serverConnected = AndruavEngine.isAndruavWSStatus(SOCKETSTATE_REGISTERED);
        final boolean fcbConnected = TelemetryModeer.getConnectionInfo() != TelemetryModeer.CURRENTCONNECTION_NON;

        setTileState(mTileImu, mIconImu, mStateImu,
                fcbConnected ? TileState.BLUE : TileState.GRAY,
                fcbConnected ? R.string.home_tile_state_ready : R.string.home_tile_state_offline);
        setTileState(mTileFpv, mIconFpv, mStateFpv,
                fcbConnected ? TileState.BLUE : TileState.GRAY,
                fcbConnected ? R.string.home_tile_state_ready : R.string.home_tile_state_offline);
        setTileState(mTileCom, mIconCom, mStateCom,
                serverConnected ? TileState.GREEN : TileState.GRAY,
                serverConnected ? R.string.home_tile_state_live : R.string.home_tile_state_offline);

        final TileState fcbState = fcbConnected ? TileState.GREEN : (serverConnected ? TileState.BLUE : TileState.GRAY);
        final float fcbOpacity = fcbConnected ? 1f : (serverConnected ? 0.85f : 0.55f);
        final int fcbCaption = fcbConnected ? R.string.home_tile_state_linked : R.string.home_tile_state_tap_to_link;
        setTileState(mTileFcb, mIconFcb, mStateFcb, fcbState, fcbCaption, fcbOpacity);

        updateTelemetryCard(fcbConnected);
    }

    private enum TileState {
        GRAY(R.drawable.bg_home_tile_gray, R.color.home_icon_gray, 0.55f),
        BLUE(R.drawable.bg_home_tile_blue, R.color.home_blue_icon, 1f),
        GREEN(R.drawable.bg_home_tile_green, R.color.home_green_icon, 1f);

        final int background;
        final int colorRes;
        final float opacity;

        TileState(int background, int colorRes, float opacity) {
            this.background = background;
            this.colorRes = colorRes;
            this.opacity = opacity;
        }
    }

    private void setTileState(View tile, ImageView icon, TextView caption, TileState state, int captionRes) {
        setTileState(tile, icon, caption, state, captionRes, state.opacity);
    }

    private void setTileState(View tile, ImageView icon, TextView caption, TileState state, int captionRes, float opacity) {
        tile.setBackgroundResource(state.background);
        final int color = ContextCompat.getColor(this, state.colorRes);
        icon.setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN);
        icon.setAlpha(opacity);
        caption.setText(captionRes);
        caption.setTextColor(color);
        caption.setAlpha(opacity);
    }

    private void updateTelemetryCard(boolean fcbConnected) {
        mTxtTelemetryStatus.setText(fcbConnected ? R.string.home_telemetry_linked : R.string.home_telemetry_not_linked);
        mTxtTelemetryStatus.setTextColor(ContextCompat.getColor(this,
                fcbConnected ? R.color.home_green_icon : R.color.home_text_dim));

        mGroupTelemetryLinked.setVisibility(fcbConnected ? View.VISIBLE : View.GONE);
        mTxtTelemetryHelper.setVisibility(fcbConnected ? View.GONE : View.VISIBLE);

        if (!fcbConnected) return;

        final com.andruav.andruavUnit.AndruavUnitBase unit = AndruavSettings.andruavWe7daBase;

        mTxtDroneName.setText(unit.UnitID);

        final int vehicleType = unit.getVehicleType();
        final String vehicleLabel = (vehicleType >= 0 && vehicleType < VehicleTypes.vechicleTypes.length)
                ? VehicleTypes.vechicleTypes[vehicleType] : "";
        mTxtDroneSubtitle.setText(getString(R.string.home_telemetry_subtitle_format, vehicleLabel, linkTypeLabel(Preference.getFCBTargetComm(null))));

        mTxtGps.setText(getString(R.string.home_sats_format, unit.getActiveIMU().SATC));
        mTxtBattery.setText(getString(R.string.home_battery_format, (int) unit.LastEvent_Battery.FCB_BatteryRemaining));
        mTxtSignal.setText(getString(R.string.home_signal_format, unit.getSignalLevel()));
    }

    private String linkTypeLabel(int fcbTargetComm) {
        if (fcbTargetComm == Preference.FCB_COM_BT) return "Bluetooth";
        if (fcbTargetComm == Preference.FCB_COM_USB) return "USB";
        if (fcbTargetComm == Preference.FCB_COM_TCP) return "Wi-Fi";
        if (fcbTargetComm == Preference.FCB_COM_UDP) return "UDP";
        return "";
    }

    private void updateServerCard() {
        final boolean connected = AndruavEngine.isAndruavWSStatus(SOCKETSTATE_REGISTERED);

        mCardServer.setBackgroundResource(connected ? R.drawable.bg_home_card : R.drawable.bg_home_card_warning);
        setViewColor(mDotServer, connected ? R.color.home_green_icon : R.color.home_red_dot);

        mTxtServerStatus.setText(connected ? R.string.home_server_status_connected : R.string.home_server_status_disconnected);
        mTxtServerStatus.setTextColor(ContextCompat.getColor(this,
                connected ? R.color.home_green_icon : R.color.home_red_text));

        mBtnServerSignIn.setVisibility(connected ? View.GONE : View.VISIBLE);
        mBtnServerConnect.setVisibility(connected ? View.GONE : View.VISIBLE);
        mBtnServerDisconnect.setVisibility(connected ? View.VISIBLE : View.GONE);
    }

    private void setViewColor(View view, @ColorRes int colorRes) {
        final Drawable bg = view.getBackground();
        if (bg instanceof GradientDrawable) {
            ((GradientDrawable) bg.mutate()).setColor(ContextCompat.getColor(this, colorRes));
        }
    }

    /***
     * Mirrors the previous ActionBar "connect" icon behavior (now the Mission Server
     * card's CONNECT/DISCONNECT control): toggles the server link and briefly disables
     * itself to guard against double taps while the tap is being processed.
     */
    private void onServerConnectToggle() {
        mBtnServerConnect.setEnabled(false);
        mBtnServerDisconnect.setEnabled(false);
        mhandle.postDelayed(() -> {
            mBtnServerConnect.setEnabled(true);
            mBtnServerDisconnect.setEnabled(true);
        }, 1500);

        final int status = AndruavEngine.getAndruavWSStatus();
        final int action = AndruavEngine.getAndruavWSAction();

        if ((status == SOCKETSTATE_CONNECTED) || (status == SOCKETSTATE_REGISTERED)) {
            doLogout();
        } else if ((status == SOCKETSTATE_DISCONNECTED) || (status == SOCKETSTATE_FREASH)) {
            doLogin(true);
        } else if ((status == SOCKETSTATE_ERROR) || (action == SOCKETACTION_CONNECTING)) {
            doLogout();
            EventBus.getDefault().post(new EventSocketState(EventSocketState.ENUM_SOCKETSTATE.onDisconnect, "manual closing"));
        }
    }

    private void showOverflowMenu(View anchor) {
        final View content = LayoutInflater.from(this).inflate(R.layout.popup_home_overflow, null);

        final PopupWindow popup = new PopupWindow(content, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popup.setOutsideTouchable(true);
        popup.setElevation(12f);

        final boolean isGCS = AndruavSettings.andruavWe7daBase.getIsCGS();

        bindMenuRow(content, R.id.home_menu_row_help, popup, this::doHelp, true);
        bindMenuRow(content, R.id.home_menu_row_settings, popup, this::doSettings_Drone, mSettingsEnabled && !isGCS);
        bindMenuRow(content, R.id.home_menu_row_reset, popup, this::doFactoryReset, mMenuActionsEnabled);
        bindMenuRow(content, R.id.home_menu_row_remote, popup, this::doRemoteSettings, true);
        bindMenuRow(content, R.id.home_menu_row_export_logs, popup, this::doExportErrorLogs, true);
        bindMenuRow(content, R.id.home_menu_row_clear_logs, popup, this::doClearErrorLogs, true);
        bindMenuRow(content, R.id.home_menu_row_about, popup, this::showAboutDialog, true);
        bindMenuRow(content, R.id.home_menu_row_exit, popup, this::doExit, true);

        popup.showAsDropDown(anchor, -150, 4, Gravity.END);
    }

    private void bindMenuRow(View content, int rowId, PopupWindow popup, Runnable action, boolean enabled) {
        final View row = content.findViewById(rowId);
        row.setEnabled(enabled);
        row.setAlpha(enabled ? 1f : 0.4f);
        row.setOnClickListener(v -> {
            popup.dismiss();
            if (enabled) action.run();
        });
    }

    private void doHelp() {
        GMail.sendGMail(this, getString(ap.andruavmiddlelibrary.R.string.email_title), getString(ap.andruavmiddlelibrary.R.string.email_to), getString(ap.andruavmiddlelibrary.R.string.email_subject), getString(ap.andruavmiddlelibrary.R.string.email_body), null);
    }

    private void doFactoryReset() {
        AlertDialog.Builder builder = new AlertDialog.Builder(Me);
        builder.setMessage(getString(ap.andruavmiddlelibrary.R.string.conf_factoryReset))
                .setCancelable(false)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Preference.FactoryReset(null);
                        finish();
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

    private void doRemoteSettings() {
        if (AndruavSettings.andruavWe7daBase.getIsCGS()) {
            startActivity(new Intent(MainScreen.this, RemoteControlSettingGCSActivityTab.class));
        }
        else
        {
            startActivity(new Intent(MainScreen.this, RemoteControlSettingActivityTab.class));
        }
    }

    protected void doSignOut() {


        App.stopAndruavWS(false);
        startActivity(new Intent(MainScreen.this, GUI.getLoginActivity()));
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //IntentFilter filter = new IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        //registerReceiver(mUsbAttachReceiver , filter);
        //filter = new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED);
        //registerReceiver(mUsbDetachReceiver , filter);

        Me = this;


        //http://stackoverflow.com/questions/23703778/exit-android-application-programmatically
        final Intent localIntent = getIntent();
        if (localIntent.getBooleanExtra("EXIT", false)) {
            finish();
            return;
        }


        ((App)(getApplication())).initSignalMonitor();
        isUSBIntent = localIntent.getBooleanExtra("USBConnect", false);


       /* Bundle b= getIntent().getExtras();
        if (b!= null) {
            String Error = b.getString("Error");
            if (Error != null) {
                Log.e(App.TAG, "Report:" + Error);
                GMail.sendGMail(this, "Please Report Error Via GMAIL", "rcmobilestuff@gmail.com", "Error Report", Error.toString());
            }
        }
        */
        if (App.MainIntent == null) {
            App.MainIntent = new Intent(this, MainScreen.class);
            App.MainPendingIntent = PendingIntent.getActivity(this, 0, App.MainIntent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        }


        setContentView(R.layout.activity_main);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);


        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        disableButtonBar();
        // Make sure we're running on Honeycomb or higher to use ActionBar APIs


        ////// Set Content View Here
        AndruavEngine.notification().Speak(getString(ap.andruavmiddlelibrary.R.string.hello_world));
        init();
        initGUI();





        if ((getIntent().getBooleanExtra("autoconnect",false)  || Preference.isAutoStart(null)) && (!Preference.isGCS(null)))
        {

            autoConnect = true;
            Log.d("ac","autoconnect onCreate  true");
        }
        else {
            // login to test if there is an account.
            doLogin(false);  // true makes a bg
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    protected void disableButtonBar() {
        try {
            // For the main activity, make sure the app icon in the action bar
            // does not behave as a button
            androidx.appcompat.app.ActionBar actionBar = getSupportActionBar();
            if (actionBar == null) return;
            actionBar.setHomeButtonEnabled(false);
            // the Home screen redesign draws its own in-layout header; hide the system one.
            actionBar.hide();
        } catch (Throwable I) {
            // https://github.com/google/iosched/issues/79 SAMSUNG ISSUE
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        mMenu = menu;
        miConnect = mMenu.findItem(R.id.action_main_wsconnect);
        miConnect.setIcon(App.gui_ConnectionIconID);

        if (DeviceManagerFacade.canBeDroneAndruav()) {
            activateDroneMode();
        } else {
            final String msg = getString(ap.andruavmiddlelibrary.R.string.err_config_drone);
            AndruavEngine.notification().Speak(msg);
            DialogHelper.doModalDialog(this, "Limitation", msg, null);
        }

        return true;
    }


    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {

        TTS.getInstance().muteTTS = true; // we dont want to speak of update status of buttons
        updateConnectionIconsStatus(AndruavEngine.getAndruavWSStatus(), AndruavEngine.getAndruavWSAction());
        TTS.getInstance().muteTTS = false;

        return  super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {


        final  MenuItem mi_wsconnect = item;


        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_main_wsconnect) {

            mi_wsconnect.setEnabled(false);
            this.mhandle.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mi_wsconnect.setEnabled(true);
                }
            },1500);


            final int status = AndruavEngine.getAndruavWSStatus();
            final int action = AndruavEngine.getAndruavWSAction();

            if ((status == SOCKETSTATE_CONNECTED) || (status == SOCKETSTATE_REGISTERED))
            {
                doLogout();
            }
            else  if ((status == SOCKETSTATE_DISCONNECTED) || (status == SOCKETSTATE_FREASH))
            {

                doLogin(true);

            }
            else  if ((status == SOCKETSTATE_ERROR) || (action == SOCKETACTION_CONNECTING))
            {
                doLogout();
                EventBus.getDefault().post(new EventSocketState(EventSocketState.ENUM_SOCKETSTATE.onDisconnect, "manual closing"));
            }

        } else if (id == R.id.mi_main_signout) {
            doSignOut();
        } else if (id == R.id.mi_main_Exit) {
            doExit();
        } else if (id == R.id.mi_main_Settings_drone) {
            doSettings_Drone();
        } else if (id == R.id.mi_main_ResetFactory) {
            doFactoryReset();
        } else if (id == R.id.mi_main_Help) {
            doHelp();
        } else if (id == R.id.mi_remotesettings) {
            doRemoteSettings();
            return true;
        } else if (id == R.id.mi_main_About) {

            showAboutDialog();

        }
        return super.onOptionsItemSelected(item);
    }

    private void startAndruavConnection() {

        if (!PreferenceValidator.isValidWebSocket()) {
            startActivity(new Intent(MainScreen.this, HUBCommunication.class));
        }
        if (!App.isAndruavWSConnected()) {
            doProgressDialog();
            App.startAndruavWS();
            App.startAndruavSMS();
        } else {
            App.stopAndruavWS(false);
        }

    }

    protected void activateDroneMode() {


        Preference.isGCS(null, false);
        //AndruavSettings.andruavWe7daBase.IsCGS = false;
        if ((AndruavSettings.andruavWe7daBase== null) || (AndruavSettings.andruavWe7daBase.getIsCGS())) {
            // define unit if available unit is GCS or null
            App.defineAndruavUnit(false);
        }
        mTileImu.setEnabled(true);
        mTileFpv.setEnabled(true);
        mTileFcb.setEnabled(true);
        mMenu.findItem(R.id.mi_remotesettings).setVisible(true);
        // Dont Reset Vehicle Type if Connected to FCB.
        if (!AndruavSettings.andruavWe7daBase.useFCBIMU()) {
            AndruavSettings.andruavWe7daBase.setVehicleType(Preference.getVehicleType(null));
        }

        AndruavEngine.notification().Speak(getString(ap.andruavmiddlelibrary.R.string.gen_speak_droneactivated));
    }



    protected void ToggleGCS_Drone() {

        TelemetryModeer.closeAllConnections();

        if (AndruavSettings.andruavWe7daBase.getIsCGS()) {
            // GCS & Switch to Drone
            if (DeviceManagerFacade.canBeDroneAndruav()) {
                activateDroneMode();
            } else {
                final String msg = getString(ap.andruavmiddlelibrary.R.string.err_config_drone);
                AndruavEngine.notification().Speak(msg);
                DialogHelper.doModalDialog(this, "Device Limitation", msg, null);
            }
        }

    }


    private void doSettings_Drone() {
        startActivity(new Intent(MainScreen.this, SettingsDrone.class));
    }

    private void showAboutDialog() {

        final View view = LayoutInflater.from(this).inflate(R.layout.dialog_about, null);

        TextView txtVersion = view.findViewById(R.id.dialogabout_txtVersion);
        TextView txtInfo = view.findViewById(R.id.dialogabout_txtInfo);
        Button btnOk = view.findViewById(R.id.dialogabout_btnOk);

        txtVersion.setText(String.format("version %s", App.versionName));
        txtInfo.setText(Html.fromHtml(String.format("<font color=#75A4D3><b>email:</b></font><font color=#36AB36>%s</font><br><font color=#75A4D3><b>access code:</b></font><font color=#36AB36>%s</font><br><font color=#75A4D3><b>pin code:</b></font><font color=#36AB36>%s</font><br>%s",
                GUI.writeTextEmail(),
                GUI.writeTextAccessCode(),
                AndruavSettings.andruavWe7daBase.PartyID,
                GUI.writeUdpProxy()
        )));

        final AlertDialog dialog = new AlertDialog.Builder(Me).setView(view).create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
        btnOk.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void doExportErrorLogs() {
        final ExceptionHTTPLogger logger = App.exceptionHTTPLogger;
        if (logger == null) {
            Toast.makeText(Me, getString(R.string.home_menu_export_logs) + ": logger unavailable", Toast.LENGTH_SHORT).show();
            return;
        }
        new Thread(() -> {
            final Uri uri = logger.exportExceptionLogAsCSV();
            runOnUiThread(() -> {
                if (uri != null) {
                    Toast.makeText(Me, "Error logs exported to " + uri, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(Me, "No error logs to export", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private void doClearErrorLogs() {
        final ExceptionHTTPLogger logger = App.exceptionHTTPLogger;
        if (logger == null) {
            Toast.makeText(Me, getString(R.string.home_menu_clear_logs) + ": logger unavailable", Toast.LENGTH_SHORT).show();
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(Me);
        builder.setMessage("Delete all error logs?")
                .setCancelable(false)
                .setPositiveButton(android.R.string.yes, (dialog, id) -> {
                    new Thread(() -> {
                        final boolean ok = logger.clearExceptionLog();
                        runOnUiThread(() -> Toast.makeText(Me,
                                ok ? "Error logs deleted" : "Failed to delete error logs",
                                Toast.LENGTH_SHORT).show());
                    }).start();
                })
                .setNegativeButton(android.R.string.no, (dialog, id) -> dialog.cancel());
        builder.create().show();
    }

    private void doSettings() {
       doSettings_Drone();
    }


    /***
     * The final call you receive before your activity is destroyed.
     * This can happen either because the activity is finishing (someone called finish() on it,
     * or because the system is temporarily destroying this instance of the activity to save space.
     * You can distinguish between these two scenarios with the isFinishing() method.
     *
     * @html http://developer.android.com/reference/android/app/Activity.html
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        App.shutDown();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // The activity is no longer visible (it is now "stopped")

        //http://stackoverflow.com/questions/3611457/android-temporarily-disable-orientation-changes-in-an-activity


    }

    @Override
    protected void onResume() {
        // The activity has become visible (it is now "resumed").
        super.onResume();
        //http://stackoverflow.com/questions/3611457/android-temporarily-disable-orientation-changes-in-an-activity
        // setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        invalidateOptionsMenu();
        updateFCBButton();
        updateFPVButton();
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        // onPause calls mhandle.removeCallbacksAndMessages(null) which cancels
        // the postDelayed re-enable from onServerConnectToggle. Re-enable here
        // so the buttons are not stuck disabled after returning from a sub-activity
        // (e.g. login screen opened by doLogin).
        if (mBtnServerConnect != null) mBtnServerConnect.setEnabled(true);
        if (mBtnServerDisconnect != null) mBtnServerDisconnect.setEnabled(true);

        EventBus.getDefault().register(this);
        //SMS.sendSMS("01029000028","HI that is me ANdroid");

        if (AndruavSettings.andruavWe7daBase.mIsModule)
        {
            doExit(true, getString(ap.andruavmiddlelibrary.R.string.gen_must_exit));
        }

       if (autoConnect)
        {

           // Toast.makeText(Me,"SHOULD  COnnect",Toast.LENGTH_LONG).show();
            Log.d("ac","autoconnect resume true");
            mhandle.postDelayed(new Runnable() {
                @Override
                public void run() {

                    if (!NetInfoAdapter.isConnected())
                    {
                        Toast.makeText(getApplicationContext(), "no connection", Toast.LENGTH_LONG).show();
                        mhandle.postDelayed(this, 1000);
                        return ;
                    }
                    autoConnect = false; /// toggle it back
                    Log.d("ac","autoconnect doLogin Resume  true");
                    doLogin(true);

                }
            },100);
        }
        else
       {
           Log.d("ac","autoconnect resume false");
       }

        if (autoConnect)
        {

            Log.d("ac","autoconnect resume true");
            mhandle.postDelayed(new Runnable() {
                @Override
                public void run() {
                    TelemetryModeer.startAutoConnection(isUSBIntent);
                    isUSBIntent = false;

                }
            },300);
        }
        else
        {
            Log.d("ac","autoconnect resume false");
        }
    }

    @Override
    protected void onPause() {
        // Another activity is taking focus (this activity is about to be "paused").
        EventBus.getDefault().unregister(this);
        mhandle.removeCallbacksAndMessages(null);
        super.onPause();


    }

    @Override
    public void onBackPressed() {
        /*
        ************ IMPORTANT
        http://stackoverflow.com/questions/23703778/exit-android-application-programmatically
        For Other forms please use this
            Intent intent = new Intent(getApplicationContext(), FirstShasha.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra("EXIT", true);
            startActivity(intent);
         */
        doExit();
    }


    BroadcastReceiver mUsbAttachReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                Toast.makeText(Me,"Usb Attached",Toast.LENGTH_LONG).show();
                TelemetryModeer.connectToPreferredConnection(Me,false);
            }
        }
    };

    BroadcastReceiver mUsbDetachReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                Toast.makeText(Me,"Usb Detached",Toast.LENGTH_LONG).show();

            }
        }
    };
}