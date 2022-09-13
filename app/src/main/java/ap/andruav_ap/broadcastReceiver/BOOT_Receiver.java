package ap.andruav_ap.broadcastReceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;

import ap.andruav_ap.App;
import ap.andruavmiddlelibrary.preference.Preference;

/**
 * Created by mhefny on 6/8/16.
 */
public class BOOT_Receiver  extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        Log.d("AS","autoconnect BROADCAST");
        if (Preference.isAutoStart(null)) {
            Log.d("AS","autoconnect BROADCAST ON 1");
            if (!Preference.isGCS(null)) {
                Log.d("AS","autoconnect BROADCAST ON 2");
                App.restartApp(10,true);

                 //Intent startIntent = new Intent(App.getAppContext(), MainShasha.class);
                 //startIntent.putExtra("autoconnect", true);
                 //startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                 //App.getAppContext().startActivity(startIntent);
                 return;

            }

        }
    }
}
