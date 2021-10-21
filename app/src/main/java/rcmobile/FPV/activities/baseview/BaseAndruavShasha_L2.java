package rcmobile.FPV.activities.baseview;

import android.media.AudioManager;

import de.greenrobot.event.EventBus;

/**
 * Created by mhefny on 1/22/16.
 */
public class BaseAndruavShasha_L2 extends BaseAndruavShasha {


    @Override
    protected void onResume() {
        super.onResume();
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        EventBus.getDefault().register(this);
    }


    @Override
    protected void onPause() {
        EventBus.getDefault().unregister(this);
        super.onPause();
    }
}
