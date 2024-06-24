package ap.andruav_ap;

import android.Manifest;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.os.Bundle;
import android.util.Log;

import com.andruav.AndruavEngine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import ap.andruav_ap.activities.main.FirstScreen;
import ap.andruav_ap.activities.main.MainScreen;
import ap.andruav_ap.helpers.CheckAppPermissions;
import ap.andruavmiddlelibrary.preference.Preference;

public class MainActivityBuilder extends AppCompatActivity {

    UsbDevice deviceFound = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main_activity_builder);
        //Preference.gui_ShowAndruavModeDialog(null,true);
        boolean USBIntent = false;
        Intent intent = getIntent();
        if ((intent.getBooleanExtra("autoconnect",false)  || Preference.isAutoStart(null)) && (!Preference.isGCS(null)))
        {

            Intent startIntent = new Intent(this, MainScreen.class);
            startIntent.putExtra("autoconnect", true);
            startActivity(startIntent);
            //finish();
            return;
        }
        final String action = intent.getAction();
        if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
            //deviceFound = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            USBIntent = true;
        }

        if (intent != null)
        {
            if ((action!= null) && (action.equals("LOG")))
            {
                Log.e("logfpv", " Open file " + intent.getStringExtra("text"));
                File file = new File (intent.getStringExtra("text"));
                FileReader reader= null;
                StringBuilder text = new StringBuilder();
                try {
                    BufferedReader br = new BufferedReader(new FileReader(file));
                    String line;

                    while ((line = br.readLine()) != null) {
                        text.append(line);
                        text.append('\n');
                    }
                    br.close();
                    file.delete();

                    Log.e("logfpv", " log file " + text);
                    Log.e("logfpv", intent.getStringExtra("tag"));
                    AndruavEngine.log().log(intent.getStringExtra("userName"), intent.getStringExtra("tag"), text.toString());
                    Thread.sleep(500);
                    this.finish();
                }
                catch (Exception e)
                {

                }
            }
        }


        if ((App.isFirstRun) || (!CheckAppPermissions.isPermissionsOK(this))){
            startActivity(new Intent(this, FirstScreen.class));

        } else {
                    final Intent localIntent = new Intent(this, MainScreen.class);
                    localIntent.putExtra("USBConnect", USBIntent);
                    startActivity(localIntent);
        }
        finish();
    }


    @Override
    protected void onResume() {
        // The activity has become visible (it is now "resumed").
        super.onResume();



    }
}
