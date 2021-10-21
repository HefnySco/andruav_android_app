package rcmobilestuff.security;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import de.greenrobot.event.EventBus;

// to test it:
// adb shell pm grant  andro.jf.mypermission
// adb shell  am broadcast -a rcmobilestuff.andruav.ANDRUAV_FEATURES --es action "REG"

public class AndruavRegistration extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, Intent intent) {
        final long ts       = intent.getLongExtra("ts",0l);
        final String app    = intent.getStringExtra("app");

        if ((ts == 0l) || (app == null)) return;
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                final Intent intnet = new Intent("rcmobilestuff.andruav.ANDRUAV_FEATURES"); // adding S
                intnet.putExtra("action","REG");
                intnet.putExtra("app",app);
                intnet.putExtra("ts",ts);
                context.sendBroadcast(intnet);
                //context.startActivity(new Intent(context,MainActivity.class));
                EventBus.getDefault().post(new _7adath_App(app,ts));

            }
        });

        Log.d("AndruavRegistration","rcmobilestuff.security.ANDRUAV_REGISTRATION");
        Toast.makeText(context, intent.getAction() ,
                Toast.LENGTH_LONG).show();
    }
}
