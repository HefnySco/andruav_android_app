package ap.andruav_ap.broadcastReceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbManager;
import android.util.Log;
import android.widget.Toast;

import ap.andruav_ap.App;
import ap.andruav_ap.communication.telemetry.TelemetryModeer;
import ap.andruavmiddlelibrary.preference.Preference;

public class USB_Receiver  extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        Log.d("AS","autoconnect BROADCAST");
        if (Preference.isAutoFCBConnect(null)) {
            Log.d("AS","autoconnect BROADCAST ON 1");
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {

                //Intent startIntent = new Intent(App.getAppContext(), MainShasha.class);
                //startIntent.putExtra("autoconnect", true);
                //startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                //App.getAppContext().startActivity(startIntent);
                TelemetryModeer.connectToPreferredConnection(App.getAppContext(),false);
                Toast.makeText(App.getAppContext(), "USB Attached",Toast.LENGTH_LONG).show();
            }
            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                Toast.makeText(App.getAppContext(),"Usb Detached",Toast.LENGTH_LONG).show();

            }

        }
    }
}
