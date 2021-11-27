package ap.andruav_ap.activities.main;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.RequiresApi;

import com.andruav.AndruavEngine;
import com.andruav.AndruavSettings;
import com.andruav.protocol.commands.ProtocolHeaders;

import de.greenrobot.event.EventBus;
import ap.andruav_ap.App;
import ap.andruav_ap.R;
import ap.andruav_ap.activities.baseview.BaseAndruavShasha;
import ap.andruav_ap.activities.fpv.FPVActivityFactory;
import ap.andruav_ap.activities.remote.RemoteControlSettingActivityTab;
import ap.andruav_ap.activities.remote.RemoteControlSettingGCSActivityTab;
import ap.andruav_ap.activities.settings.SettingsDrone;
import ap.andruav_ap.helpers.GUI;
import ap.andruavmiddlelibrary.factory.tts.TTS;
import ap.andruavmiddlelibrary.factory.util.DialogHelper;
import ap.andruavmiddlelibrary.factory.util.GMail;
import ap.andruavmiddlelibrary.preference.Preference;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class ModuleScreen extends BaseAndruavShasha {

    private ModuleScreen Me;
    private Menu mMenu;
    private MenuItem miConnect;
    private Handler mhandle;
    private TextView mtxtAccessCode;
    private Button mbtnFPV;
    private Button mbtnFCB;



    private void UIHandler() {

        mhandle = new Handler() {
            @Override
            public void handleMessage(Message msg) {

            }
        };
    }
    private CameraManager mCameraManager;

    private void initGUI() {
        if (isInEditMode) return;

        mbtnFCB = findViewById(R.id.btnFCB);
        AndruavEngine.getPreference().getModuleType();
        mbtnFPV = findViewById(R.id.btnFPV);
        mbtnFPV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {


                FPVActivityFactory.startFPVActivity(ModuleScreen.this);

            }
        });

        mbtnFCB.setEnabled(false);
        mbtnFPV.setEnabled(false);


        if (AndruavEngine.getPreference().getModuleType().contains(ProtocolHeaders.UAVOS_FCB_MODULE_CLASS)) {
            mbtnFCB.setEnabled(true);
        }
        if (AndruavEngine.getPreference().getModuleType().contains(ProtocolHeaders.UAVOS_CAMERA_MODULE_CLASS)) {
            mbtnFPV.setEnabled(true);
        }


        boolean b = getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);

        mtxtAccessCode  = findViewById(R.id.modulleactivity_txtAccessCode);
        writeInfoLabel();

        UIHandler();
    }


    private void writeInfoLabel() {
        mtxtAccessCode.setText(Html.fromHtml(String.format("<font color=#75A4D3><b>access code: </b></font><font color=#36AB36>%s</font><br><font color=#75A4D3><b>pin code:</b></font><font color=#36AB36>%s</font>", GUI.writeTextAccessCode(),AndruavSettings.andruavWe7daBase.PartyID)));
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        Me = this;

        final Intent localIntent = getIntent();
        if (localIntent.getBooleanExtra("EXIT", false)) {
            finish();
            return;
        }

        setContentView(R.layout.activity_module_shasha);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);


        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        disableButtonBar();

        initGUI();

        // I must be a drone
        activateDroneMode();
    }


    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    protected void disableButtonBar() {
        try {
            // For the main activity, make sure the app icon in the action bar
            // does not behave as a button
            androidx.appcompat.app.ActionBar actionBar = getSupportActionBar();
            if (actionBar == null) return;
            actionBar.setHomeButtonEnabled(false);
        } catch (Throwable I) {
            // https://github.com/google/iosched/issues/79 SAMSUNG ISSUE
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);

        if (!AndruavSettings.andruavWe7daBase.mIsModule)
        {
            doExit(true, getString(R.string.gen_must_exit));
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
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_module_main, menu);
        mMenu = menu;
        miConnect = mMenu.findItem(R.id.action_main_wsconnect);
        miConnect.setIcon(App.gui_ConnectionIconID);



        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {

        TTS.getInstance().muteTTS = true; // we dont want to speak of update status of buttons
        updateConnectionIconsStatus();
        TTS.getInstance().muteTTS = false;

        return  super.onPrepareOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {


        final  MenuItem item2 = item;


        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_main_wsconnect) {

            if (!AndruavSettings.andruavWe7daBase.mIsModule)
            {
                doExit(true, getString(R.string.gen_must_exit));
            }

            if (App.isAndruavUDPOn())
            {
                App.stopUDPServer();
            }
            else
            {
                App.initializeAndruavUDP();

                App.startUDPServer();
            }

        } else if (id == R.id.mi_main_signout) {
        } else if (id == R.id.mi_main_GCS) {
        } else if (id == R.id.mi_main_Exit) {
            doExit();
        } else if (id == R.id.mi_main_Settings_drone) {
            doSettings_Drone();
        } else if (id == R.id.mi_main_ResetFactory) {
            AlertDialog.Builder builder = new AlertDialog.Builder(Me);
            builder.setMessage(getString(R.string.conf_factoryReset))
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


        } else if (id == R.id.mi_main_Help) {
            GMail.sendGMail(this, getString(R.string.email_title), getString(R.string.email_to), getString(R.string.email_subject), getString(R.string.email_body), null);

        } else if (id == R.id.mi_remotesettings) {
            if (AndruavSettings.andruavWe7daBase.getIsCGS()) {
                startActivity(new Intent(ModuleScreen.this, RemoteControlSettingGCSActivityTab.class));
            }
            else
            {
                startActivity(new Intent(ModuleScreen.this, RemoteControlSettingActivityTab.class));
            }
            return true;
        } else if (id == R.id.mi_main_About) {

            DialogHelper.doModalDialog(Me, getString(R.string.gen_about), Html.fromHtml(String.format("<font color=#75A4D3><b>version:</b></font><font color=#36AB36>%s</font><br><font color=#75A4D3><b>email:</b></font><font color=#36AB36>%s</font><br><font color=#75A4D3><b>access code:</b></font><font color=#36AB36>%s</font><br><font color=#75A4D3><b>pin code:</b></font><font color=#36AB36>%s</font>", App.versionName, GUI.writeTextEmail(),GUI.writeTextAccessCode(),AndruavSettings.andruavWe7daBase.PartyID)), null);
        }

        updateConnectionIconsStatus();
        return super.onOptionsItemSelected(item);
    }


    protected void updateConnectionIconsStatus() {

        if (App.isAndruavUDPOn())
        {
            miConnect.setIcon(R.drawable.connected_color_32x32);
        }
        else
        {
            miConnect.setIcon(R.drawable.connect_w_32x32);
        }
    }

    private void doSettings_Drone() {
        startActivity(new Intent(ModuleScreen.this, SettingsDrone.class));
    }


    @Override
    public void onBackPressed() {
        doExit(false,null);
    }

    protected void doExit(final boolean mandatoryExit, final String exitMessage) {
        pauseToExit = true;
        AlertDialog.Builder builder = new AlertDialog.Builder(Me);
        String exitString = getString(R.string.gen_exit);
        if ((exitMessage != null) && (!exitMessage.isEmpty()))
        {
            exitString = exitMessage;
        }

        builder.setMessage(exitString)
                .setCancelable(false)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                        App.shutDown();

                        android.os.Process.killProcess(android.os.Process.myPid());
                    }
                });

        if (!mandatoryExit) {
            builder.setNeutralButton(R.string.main_action_hide, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    moveTaskToBack(true);
                    pauseToExit = true;
                }
            });

            builder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                            pauseToExit = false;
                            //AndruavReceiver.checkRegistration(Me);
                        }
                    });
        }

        AlertDialog alert = builder.create();
        alert.show();
    }



    protected void activateDroneMode() {


        Preference.isGCS(null, false);
        //AndruavSettings.andruavWe7daBase.IsCGS = false;
        if ((AndruavSettings.andruavWe7daBase== null) || (AndruavSettings.andruavWe7daBase.getIsCGS())) {
            // define unit if available unit is GCS or null
            App.defineAndruavUnit(false);
        }
//        mbtnIMU.setEnabled(true);
//        mbtnFCB.setEnabled(true);
        //mMenu.findItem(R.id.mi_remotesettings).setVisible(true);
        // Dont Reset Vehicle Type if Connected to FCB.
        if (!AndruavSettings.andruavWe7daBase.useFCBIMU()) {
            AndruavSettings.andruavWe7daBase.setVehicleType(Preference.getVehicleType(null));
        }

        AndruavEngine.notification().Speak(getString(R.string.gen_speak_droneactivated));
    }
}
