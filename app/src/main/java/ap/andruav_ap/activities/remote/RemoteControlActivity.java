package ap.andruav_ap.activities.remote;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import de.greenrobot.event.EventBus;
import ap.andruav_ap.DeviceManagerFacade;
import ap.andruavmiddlelibrary.eventClasses.remoteControl.Event_RemoteEngaged_CMD;
import ap.andruav_ap.R;
import ap.andruavmiddlelibrary.factory.util.GMail;
import ap.andruavmiddlelibrary.factory.util.DialogHelper;

public class RemoteControlActivity extends AppCompatActivity {

    //////// Attributes
    Activity Me;
    private Handler mhandle;

    protected RemoteControlWidget mRemoteControlWidget;

    /////////// EOF Attributes


    //////////BUS EVENT

    public void onEvent(Event_RemoteEngaged_CMD event_remoteEngaged_cmd) {

        final Message msg = mhandle.obtainMessage();
        msg.obj = event_remoteEngaged_cmd;

        if (mhandle != null)  mhandle.sendMessageDelayed(msg,0);
    }

//    public void onEvent(EventRemote_ChannelsCMD eventRemote_channelsCMD) {
//        final Message msg = mhandle.obtainMessage();
//        msg.obj = eventRemote_channelsCMD;
//
//        if (mhandle != null)  mhandle.sendMessageDelayed(msg,0);
//    }

    ///EOF Event BUS


    /***
     * Event to UI gate to enable access UI safely.
     */
    private void UIHandler () {
        mhandle = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

//                if (msg.obj instanceof EventRemote_ChannelsCMD)
//                {
//                    OnChannelChanged((EventRemote_ChannelsCMD) msg.obj);
//                }
//                else
                if (msg.obj instanceof Event_RemoteEngaged_CMD)
                {
                    OnRemoteEngaged((Event_RemoteEngaged_CMD)msg.obj);
                }
            }
        };


    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Me = this;
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        setContentView(R.layout.activity_remote_control);

        mRemoteControlWidget = findViewById(R.id.remotecontrolactivity_remotecontrolwidget);
        if (DeviceManagerFacade.hasMultitouch())
        {
            mRemoteControlWidget.updateSettings();
        }


        UIHandler();
    }

    @Override
    protected void onResume ()
    {
        super.onResume();
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        EventBus.getDefault().register(this);

        if (!DeviceManagerFacade.hasMultitouch())
        {
            DialogHelper.doModalDialog(Me,getString(R.string.title_activity_remotecontrol),getString(R.string.err_feature_multitouch),null,new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    Me.finish();
                }
            });
        }
        mRemoteControlWidget.updateSettings();

    }

    @Override
    protected void onPause()
    {
        EventBus.getDefault().unregister(this);


        super.onPause();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_remote_control, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.mi_Help) {
            GMail.sendGMail(this, getString(R.string.email_title), getString(R.string.email_to), getString(R.string.email_subject), getString(R.string.email_body), null);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    public void OnRemoteEngaged(Event_RemoteEngaged_CMD event_remoteEngaged_cmd) {

    }

    public void OnChannelChanged() {
       //String status = String.format("<font color=#7584D3>channel:<b>%d</b> value:<b>%d</b>",eventRemote_channelsCMD.singleChannelChange,eventRemote_channelsCMD.singleChannelValue);
       //txtStickLeft.setText("channel:" + eventRemote_channelsCMD.singleChannelChange +" value:"+ eventRemote_channelsCMD.singleChannelValue);

    }
}
