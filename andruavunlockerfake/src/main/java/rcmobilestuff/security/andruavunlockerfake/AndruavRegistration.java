package rcmobilestuff.security.andruavunlockerfake;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

// to test it:
// adb shell pm grant  andro.jf.mypermission
// adb shell am broadcast -a rcmobilestuff.andruav.ANDRUAV_FEATURES

public class AndruavRegistration extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, Intent intent) {
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                Intent intnet = new Intent("rcmobilestuff.andruav.ANDRUAV_FEATURES"); // adding S
                intnet.setAction("OK");
                context.startActivity(new Intent(context,MainActivity.class));
            }
        });

        Log.d("AndruavRegistration","rcmobilestuff.andruav.ANDRUAV_FEATURES");
        Toast.makeText(context, intent.getAction() ,
                Toast.LENGTH_LONG).show();
    }
}
