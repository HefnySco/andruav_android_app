package ap.andruavmiddlelibrary.factory.communication;

import android.Manifest;
import android.content.pm.PackageManager;
import android.telephony.SmsManager;

import androidx.core.app.ActivityCompat;

import com.andruav.AndruavEngine;
import com.andruav.AndruavFacade;
import com.andruav.interfaces.INotification;

import static com.andruav.protocol.communication.websocket.AndruavWSClientBase.SOCKETSTATE_REGISTERED;

import java.net.URLEncoder;
import java.util.ArrayList;

/**
 * Created by M.Hefny on 11-Nov-14.
 */
public class SMS {

    public  static void sendSMS (final String phoneNo, final String msg)
    {
        try {
            if (ActivityCompat.checkSelfPermission(AndruavEngine.AppContext, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                if (AndruavEngine.isAndruavWSStatus(SOCKETSTATE_REGISTERED)) {
                    AndruavFacade.sendErrorMessage(INotification.INFO_TYPE_TELEMETRY, INotification.NOTIFICATION_TYPE_ERROR,
                            0, "SMS permission denied (SEND_SMS)", null);
                }
                return;
            }
            SmsManager smsManager = SmsManager.getDefault();
            if (smsManager == null) return;
            ArrayList<String> msgArray=smsManager.divideMessage(msg);
            smsManager.sendMultipartTextMessage(phoneNo, null, msgArray, null, null);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }



}

