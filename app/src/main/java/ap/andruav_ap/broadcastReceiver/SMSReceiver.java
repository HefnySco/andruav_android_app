package ap.andruav_ap.broadcastReceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;

public class SMSReceiver  extends BroadcastReceiver {
    private static final String SMS_RECEIVED_ACTION = "android.provider.Telephony.SMS_RECEIVED";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals(SMS_RECEIVED_ACTION)) {
            // Retrieve the SMS message
            SmsMessage[] messages = getMessagesFromIntent(intent);
            if (messages != null && messages.length > 0) {
                StringBuilder sb = new StringBuilder();
                for (SmsMessage smsMessage : messages) {
                    String sender = smsMessage.getDisplayOriginatingAddress();
                    String messageBody = smsMessage.getMessageBody();
                    sb.append("SMS from: ").append(sender).append("\n");
                    sb.append("Message: ").append(messageBody).append("\n\n");
                }

                // Do something with the received SMS
                String smsContent = sb.toString();
                // Here, you can process or display the received SMS as per your app's requirements
            }
        }
    }

    private SmsMessage[] getMessagesFromIntent(Intent intent) {
        SmsMessage[] messages = null;
        try {
            Object[] pdus = (Object[]) intent.getSerializableExtra("pdus");
            if (pdus != null) {
                messages = new SmsMessage[pdus.length];
                for (int i = 0; i < pdus.length; i++) {
                    messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return messages;
    }
}