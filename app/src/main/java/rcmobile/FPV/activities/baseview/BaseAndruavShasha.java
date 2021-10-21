package rcmobile.FPV.activities.baseview;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import androidx.fragment.app.FragmentManager;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;

import com.andruav.AndruavSettings;
import com.andruav.event.fpv7adath._7adath_InitAndroidCamera;

import rcmobile.FPV.R;
import rcmobile.FPV.guiEvent.GUIEvent_EnableFlashing;
import rcmobile.FPV.activities.fpv.FPVActivityFactory;
import rcmobile.FPV.App;
import rcmobile.FPV.widgets.AlarmWidget;
import rcmobile.andruavmiddlelibrary.factory.util.ActivityMosa3ed;

/**
 * Created by mhefny on 1/27/16.
 */
public class BaseAndruavShasha extends AppCompatActivity {

    // mode to base view
    protected boolean isInEditMode;

    protected FragmentManager fragmentManager;

    protected AlarmWidget alarmWidget;

    protected boolean pauseToExit = false;

    //////////BUS EVENT

    public void onEvent(final _7adath_InitAndroidCamera adath_initAndroidCamera) {

        if (AndruavSettings.andruavWe7daBase.getIsCGS()) {
            // you cannot switch screen for a user.
            return;
        }

        mbaseAndruavActivityHandler.post(new Runnable() {
            @Override
            public void run() {

                 FPVActivityFactory.startFPVActivity(App.activeActivity, adath_initAndroidCamera);

            }
        });
    }


    public void onEvent (GUIEvent_EnableFlashing guiEvent_enableFlashing)
    {
        if (AndruavSettings.andruavWe7daBase.getIsCGS())
        {
            // you cannot switch screen for a user.
            return;
        }


        if (guiEvent_enableFlashing.enableFlashing) {
            mbaseAndruavActivityHandler.post(new Runnable() {
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
            if (alarmWidget != null)
            {
                mbaseAndruavActivityHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        // must be called from Handler
                        ActivityMosa3ed.removeMeFromParentView(alarmWidget);
                        alarmWidget = null;
                    }
                });

            }
        }


    }


    protected Handler mbaseAndruavActivityHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {


        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fragmentManager = getSupportFragmentManager();


        App.activeActivity = this;

        View v = this.findViewById(android.R.id.content);
        isInEditMode = v.isInEditMode();

    }


    @Override
    protected void onResume() {
        super.onResume();
        App.activeActivity = this;
    }


    @Override
    protected void onPause() {
        App.activeActivity = null;
        super.onPause();
    }

    @Override
    protected void onStop() {

        super.onStop();
    }

    protected void doExit()
    {
        doExit(false,getString(R.string.gen_exit));
    }

    protected void doExit(final boolean mandatoryExit, final String exitMessage) {
        pauseToExit = true;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(exitMessage)
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

}