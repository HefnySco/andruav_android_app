package rcmobile.FPV.broadcastReceiver;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.andruav.AndruavSettings;
import com.andruav.event.Event_Registering;
import com.andruav.interfaces.INotification;

import de.greenrobot.event.EventBus;
import rcmobile.FPV.App;

public class AndruavReceiver extends BroadcastReceiver {

    private static AndruavReceiver mAndruavReceiver;
    private static IntentFilter mIntentFilter;
    private static long timestamp;
    // adb shell  am broadcast -a rcmobilestuff.andruav.ANDRUAV_FEATURES --es action "REG"

    private static final String APP = "ANDRUAV";
    public static long lastUpdate;

    @Override
    public void onReceive(final Context context, final Intent intent) {
        final String action = intent.getStringExtra("action");
        if ((action!= null) && (action.equals("REG")))
        {
            final long ts       = intent.getLongExtra("ts",0L);
            final String app    = intent.getStringExtra("app");

            if ((ts == 0L) || (ts != timestamp))
            {
                return;
            }

            if ((app == null) || (!app.equals(APP) ))
            {
                return;
            }

            if (!AndruavSettings.UnLockerExists) {
                AndruavSettings.UnLockerExists = true;
                App.notification.displayNotification(INotification.NOTIFICATION_TYPE_NORMAL, "Andruav", "Registered Version", true, INotification.INFO_TYPE_REGISTRATION, true);
            }
            EventBus.getDefault().post(new Event_Registering(true));
        }
    }

    public static void checkRegistration (final Context context)
    {
        timestamp = System.currentTimeMillis();

        if  (((System.currentTimeMillis() - AndruavReceiver.lastUpdate) > 5000) &&  (AndruavSettings.UnLockerExists))
        {
            AndruavSettings.UnLockerExists = false;
            App.notification.displayNotification(INotification.NOTIFICATION_TYPE_WARNING, "Andruav", "Free Version", true, INotification.INFO_TYPE_REGISTRATION, true);

        }

        final Intent intnet = new Intent();
        intnet.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        intnet.setAction("rcmobilestuff.security.ANDRUAV_REGISTRATION");
        intnet.setComponent(new ComponentName("rcmobilestuff.security","rcmobilestuff.security.AndruavRegistration"));

        intnet.putExtra("app",APP);
        intnet.putExtra("ts",timestamp);

        context.sendBroadcast(intnet); //,"andro2.jf.mypermission" );
    }


    public static void registerMe(final Context context)
    {
        mAndruavReceiver = new AndruavReceiver();
        mIntentFilter = new IntentFilter("rcmobilestuff.andruav.ANDRUAV_FEATURES");

        context.registerReceiver(mAndruavReceiver, mIntentFilter);


    }

    public static void unregisterMe(final Context context)
    {
        context.unregisterReceiver(mAndruavReceiver);

    }
}
