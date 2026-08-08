package ap.andruav_ap.activities.main;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.andruav.AndruavEngine;
import com.andruav.AndruavSettings;

import ap.andruavmiddlelibrary.factory.communication.SMS;
import org.greenrobot.eventbus.EventBus;
import ap.andruav_ap.activities.baseview.BaseAndruavShasha;
import ap.andruav_ap.App;
import ap.andruav_ap.helpers.CheckAppPermissions;
import com.andruav.event.systemEvent.Event_ShutDown_Signalling;
import ap.andruavmiddlelibrary.factory.util.DialogHelper;
import ap.andruavmiddlelibrary.factory.util.GMail;
import ap.andruav_ap.R;

import static com.andruav.protocol.communication.websocket.AndruavWSClientBase.SOCKETSTATE_REGISTERED;

public class FirstScreen extends BaseAndruavShasha {


    private FirstScreen Me;


    private boolean proceededToMainScreen = false;
    private final Handler mPermissionHandler = new Handler();

    /***
     * Always continues to MainScreen exactly once, regardless of how the user
     * responds to the permission dialog (or if they don't respond at all).
     * This guarantees the app never gets stuck with "nowhere to go".
     */
    private void proceedToMainScreen ()
    {
        if (proceededToMainScreen) return;
        proceededToMainScreen = true;
        mPermissionHandler.removeCallbacksAndMessages(null);
        startActivity(new Intent(Me, MainScreen.class));
        finish();
    }

    private static final int PERMISSIONS_REQUEST_CODE = 1;

    private void initGUI ()
    {

        if (AndruavEngine.isAndruavWSStatus(SOCKETSTATE_REGISTERED))
        {
            proceedToMainScreen();
            return;
        }

        new Handler().post(new Runnable() {
            @Override
            public void run() {

                if (!CheckAppPermissions.isPermissionsOK(Me))
                {
                    // Highlight and ask for permissions -- clearly visible, not a toast
                    // that can be missed. "Grant" triggers the real OS permission dialogs
                    // and we wait for onRequestPermissionsResult() before proceeding --
                    // finishing the Activity right away would kill the dialogs before
                    // they can even show. "Skip" proceeds immediately. A safety timer
                    // guarantees we never get stuck here either way.
                    DialogHelper.doModalDialog(Me,
                            getString(ap.andruavmiddlelibrary.R.string.gen_security),
                            getString(ap.andruavmiddlelibrary.R.string.err_security),
                            getString(android.R.string.ok),
                            (dialogInterface, i) -> CheckAppPermissions.requestAllPermissions(Me, PERMISSIONS_REQUEST_CODE),
                            getString(android.R.string.cancel),
                            (dialogInterface, i) -> proceedToMainScreen());

                    // Safety net: never get stuck on this screen even if the user
                    // ignores the dialog, dismisses it by tapping outside, or the OS
                    // permission flow never calls back for some reason.
                    mPermissionHandler.postDelayed(FirstScreen.this::proceedToMainScreen, 20000);
                }
                else
                {
                    proceedToMainScreen();
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            proceedToMainScreen();
        }
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first);
        Me = this;

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        disableButtonBar();


        ////// Set Content View Here
        AndruavEngine.notification().Speak(getString(ap.andruavmiddlelibrary.R.string.hello_world));

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
