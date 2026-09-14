package ap.andruavmiddlelibrary.factory.communication;

import android.Manifest;
import android.content.pm.PackageManager;
import android.telephony.SmsManager;

import androidx.core.app.ActivityCompat;

import com.andruav.AndruavEngine;
import com.andruav.AndruavFacade;
import com.andruav.interfaces.INotification;
import com.andruav.protocol.commands.textMessages.Control.AndruavMessage_RemoteExecuteResult;

import static com.andruav.protocol.communication.websocket.AndruavWSClientBase.SOCKETSTATE_REGISTERED;

import java.net.URLEncoder;
import java.util.ArrayList;

import ap.andruavmiddlelibrary.factory.DeviceFeatures;

/**
 * Created by M.Hefny on 11-Nov-14.
 */
public class SMS {

    /***
     * @return one of {@link AndruavMessage_RemoteExecuteResult} RESULT_* codes.
     */
    public  static int sendSMS (final String phoneNo, final String msg)
    {
        try {
            // SMS is only required when the device actually has telephony/SMS
            // capabilities. On devices without it (tablets, WiFi-only units),
            // skip silently instead of nagging for a permission that could
            // never be backed by a real SMS channel.
            if (!DeviceFeatures.hasSMSCapabilities) {
                AndruavEngine.log().log2("SMS", "sms_skip", "sendSMS skipped: no SMS capabilities (to=" + phoneNo + ")");
                return AndruavMessage_RemoteExecuteResult.RESULT_NO_CAPABILITY;
            }
            if (ActivityCompat.checkSelfPermission(AndruavEngine.AppContext, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                AndruavEngine.log().log2("SMS", "sms_skip", "sendSMS skipped: SEND_SMS permission denied (to=" + phoneNo + ")");
                if (AndruavEngine.isAndruavWSStatus(SOCKETSTATE_REGISTERED)) {
                    AndruavFacade.sendErrorMessage(INotification.INFO_TYPE_TELEMETRY, INotification.NOTIFICATION_TYPE_ERROR,
                            0, "SMS permission denied (SEND_SMS)", null);
                }
                return AndruavMessage_RemoteExecuteResult.RESULT_PERMISSION_DENIED;
            }
            SmsManager smsManager = SmsManager.getDefault();
            if (smsManager == null) {
                AndruavEngine.log().log2("SMS", "sms_skip", "sendSMS skipped: SmsManager.getDefault() returned null (to=" + phoneNo + ")");
                return AndruavMessage_RemoteExecuteResult.RESULT_NO_CAPABILITY;
            }
            ArrayList<String> msgArray=smsManager.divideMessage(msg);
            smsManager.sendMultipartTextMessage(phoneNo, null, msgArray, null, null);
            AndruavEngine.log().log2("SMS", "sms_tx", "sendSMS sent to=" + phoneNo + " parts=" + msgArray.size());
            return AndruavMessage_RemoteExecuteResult.RESULT_OK;
        }
        catch (Exception e)
        {
            AndruavEngine.log().logException("SMS", "sendSMS_failed", e);
            e.printStackTrace();
            return AndruavMessage_RemoteExecuteResult.RESULT_ERROR;
        }
    }



}

