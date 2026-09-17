package ap.andruav_ap.broadcastReceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.content.ContextCompat;

import ap.andruav_ap.App;
import ap.andruav_ap.services.link.AndruavLinkService;
import ap.andruavmiddlelibrary.preference.Preference;

/**
 * Created by mhefny on 6/8/16.
 */
public class BOOT_Receiver  extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        Log.d("AS","autoconnect BROADCAST");
        if (Preference.isAutoStart(null)) {
            if (Preference.isLinkServiceDesired(null)) {
                // The link was up when the phone went down: resume it headless instead of
                // launching the full app (the old activity launch from a receiver is blocked by
                // background-activity-start rules on API 29+, so boot autostart was broken).
                // AndruavLinkService re-logins, re-registers and restores IDs/tasks only - the
                // FCB is deliberately not auto-connected.
                // Legal because targetSdk == 34: Android 15 restricts which FGS types may
                // start from BOOT_COMPLETED for apps targeting 35+ - re-verify on a bump.
                try {
                    ContextCompat.startForegroundService(context,
                            new Intent(context, AndruavLinkService.class));
                    return;
                } catch (Exception e) {
                    // e.g. ForegroundServiceStartNotAllowedException without the
                    // battery-optimization exemption. Fall through to the old full-app restart.
                    Log.e("AS","link FGS boot start failed", e);
                }
            }

            Log.d("AS","autoconnect BROADCAST ON 1");
            Log.d("AS","autoconnect BROADCAST ON 2");
            App.restartApp(10,true);

             //Intent startIntent = new Intent(App.getAppContext(), MainShasha.class);
             //startIntent.putExtra("autoconnect", true);
             //startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
             //App.getAppContext().startActivity(startIntent);

        }
    }
}
