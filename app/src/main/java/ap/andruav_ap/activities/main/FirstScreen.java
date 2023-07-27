package ap.andruav_ap.activities.main;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;

import com.andruav.AndruavEngine;
import com.andruav.AndruavSettings;
import com.andruav.controlBoard.shared.common.VehicleTypes;

import de.greenrobot.event.EventBus;
import ap.andruav_ap.activities.baseview.BaseAndruavShasha;
import ap.andruav_ap.App;
import ap.andruav_ap.activities.login.drone.MainDroneActiviy;
import ap.andruav_ap.activities.login.GCSLoginShasha;
import ap.andruav_ap.helpers.CheckAppPermissions;
import com.andruav.event.systemEvent.Event_ShutDown_Signalling;
import ap.andruavmiddlelibrary.factory.util.DialogHelper;
import ap.andruavmiddlelibrary.factory.util.GMail;
import ap.andruavmiddlelibrary.preference.Preference;
import ap.andruavmiddlelibrary.preference.PreferenceValidator;
import ap.andruav_ap.R;

import static com.andruav.protocol.communication.websocket.AndruavWSClientBase.SOCKETSTATE_REGISTERED;

public class FirstScreen extends BaseAndruavShasha {


    private FirstScreen Me;

    private Button btnDroneMode;
    private Button btnGCSMode;
    private CheckBox chkNoAgain;

    private void initGUI ()
    {
        chkNoAgain = findViewById((R.id.firstactivity_chkNoAgain));
        btnDroneMode = findViewById(R.id.firstactivity_btnDroneMode);
        btnGCSMode = findViewById(R.id.firstactivity_btnGCSMode);


        chkNoAgain.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Preference.gui_ShowAndruavModeDialog(null, !isChecked);
            }
        });

        btnDroneMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Preference.isGCS(null, false);
                //AndruavSettings.andruavWe7daBase.IsCGS = false;
                App.defineAndruavUnit(false);
                AndruavSettings.andruavWe7daBase.setVehicleType(Preference.getVehicleType(null));
                if (App.isFirstRun) {
                    if (PreferenceValidator.isInvalidLoginCode()) {
                        startActivity(new Intent(Me, MainDroneActiviy.DroneLoginShasha.class));
                        return;
                    }
                }

                startActivity(new Intent(Me, MainScreen.class));
            }
        });



        btnGCSMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Preference.isGCS(null, true);
                //AndruavSettings.andruavWe7daBase.IsCGS = true;
                App.defineAndruavUnit(true);
                Preference.setVehicleType(null, VehicleTypes.VEHICLE_GCS);
                AndruavSettings.andruavWe7daBase.setVehicleType(VehicleTypes.VEHICLE_GCS);


                if (App.isFirstRun) {
                    if (PreferenceValidator.isInvalidLoginCode()) {
                        startActivity(new Intent(Me, GCSLoginShasha.class));
                        return;
                    }
                }

                startActivity(new Intent(Me, MainScreen.class));

            }
        });

        if (AndruavEngine.isAndruavWSStatus(SOCKETSTATE_REGISTERED))
        {
            startActivity(new Intent(Me, MainScreen.class));
        }

        new Handler().post(new Runnable() {
            @Override
            public void run() {


                if (!CheckAppPermissions.isPermissionsOK(Me))
                {
                    DialogHelper.doModalDialog(Me, getString(R.string.gen_security), getString(R.string.err_security), "yes",
                            (dialogInterface, i) -> CheckAppPermissions.goToSettings(),
                            "No",
                            (dialogInterface, i) -> GMail.sendGMail(Me, getString(R.string.email_title), getString(R.string.email_to), getString(R.string.email_subject2), getString(R.string.email_body2), null));
                    return;
                }

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) //R == API 30
                {
                    CheckAppPermissions.checkPermissionAndRequest(Me, Manifest.permission.WRITE_EXTERNAL_STORAGE, "Storage access is needed for log kml files, images and videos.");
                }
                else
                {
                    CheckAppPermissions.checkPermissionAndRequest(Me,
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            "Storage access is needed for log kml files, images and videos.");
                }
                CheckAppPermissions.checkPermissionAndRequest(Me, Manifest.permission.CAMERA,"Please Open Camera Permission");
                CheckAppPermissions.checkPermissionAndRequest(Me, Manifest.permission.ACCESS_FINE_LOCATION,"Mobile GPS is needed for GCS for maps & Drones as a backup.");
                CheckAppPermissions.checkPermissionAndRequest(Me, Manifest.permission.READ_PHONE_STATE,"Phone state is used to determine network quality & connection status.");
                //CheckAppPermissions.checkPermissionAndRequest(Me, Manifest.permission.SEND_SMS,"If you want your drone to sendMessageToModule SOS SMS you need to enable this. It is not an option.");

            }
        });
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first);
        Me = this;

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        disableButtonBar();


        ////// Set Content View Here
        AndruavEngine.notification().Speak(getString(R.string.hello_world));

        initGUI();

    }

    @Override
    protected void onResume () {
        super.onResume();

        setVolumeControlStream(AudioManager.STREAM_MUSIC);


    }


    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    protected void disableButtonBar ()
    {
        // For the main activity, make sure the app icon in the action bar
        // does not behave as a button
        androidx.appcompat.app.ActionBar actionBar = getSupportActionBar();
        if (actionBar == null) return;
        actionBar.setHomeButtonEnabled(false);
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




    private  void shutDown ()
    {
        try {

            AndruavSettings.andruavWe7daBase.setShutdown(true);

            Thread.sleep(1000); // wait time to sendMessageToModule shut down message.


            EventBus.getDefault().post(new Event_ShutDown_Signalling(1));

            EventBus.getDefault().post(new Event_ShutDown_Signalling(2));

            EventBus.getDefault().post(new Event_ShutDown_Signalling(3));

            EventBus.getDefault().post(new Event_ShutDown_Signalling(4));

            App.stopUDPServer();


        }
        catch (Exception ex)
        {
            AndruavEngine.log().logException("exception",ex);
        }
    }

    // Jai handle screen rotate - start
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        setContentView(R.layout.activity_first);
        initGUI();
    }
    // Jai handle screen rotate - end
}
