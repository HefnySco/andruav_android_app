package ap.andruav_ap.broadcastReceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import com.andruav.AndruavEngine;

import ap.andruav_ap.communication.telemetry.AndruavSMSClientParser;
import ap.andruavmiddlelibrary.preference.Preference;

public class SMSReceiver extends BroadcastReceiver {

    private static final String TAG = "SMSReceiver";
    private static final AndruavSMSClientParser andruavSMSClientParser = new AndruavSMSClientParser();
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Preference.isSMSRXEnabled(null))
        {
            return ; // ModuleFeatures is disabled by user.
        }

        if (intent.getAction() != null && intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                Object[] pdus = (Object[]) extras.get("pdus");
                if (pdus != null) {
                    for (Object pdu : pdus) {
                        SmsMessage smsMessage;
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                String format = extras.getString("format");
                                if (format != null) {
                                    smsMessage = SmsMessage.createFromPdu((byte[]) pdu, format);
                                } else {
                                    smsMessage = SmsMessage.createFromPdu((byte[]) pdu);
                                }
                            } else {
                                smsMessage = SmsMessage.createFromPdu((byte[]) pdu);
                            }
                        } catch (Exception e) {
                            AndruavEngine.log().logException("SMSReceiver", e);
                            Log.e(TAG, "createFromPdu failed", e);
                            continue;
                        }
                        if (smsMessage == null) continue;

                        String sender = smsMessage.getDisplayOriginatingAddress();
                        String messageBody = smsMessage.getMessageBody();

                        try {
                            andruavSMSClientParser.executeCommand (sender, messageBody);
                        } catch (Exception e) {
                            AndruavEngine.log().logException("SMSReceiver", e);
                            Log.e(TAG, "executeCommand failed", e);
                        }
                    }
                }
            }
        }
    }
}