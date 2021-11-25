package ap.andruavmiddlelibrary.factory.communication;

/**
 * Created by M.Hefny on 11-Nov-14.
 */
public class SMS {

    public  static void sendSMS (final String phoneNo, final String msg)
    {
        /*try {
            // REMOVED AS PER GOOGLE PLAY REQUIREMENT

            *//*if (ActivityCompat.checkSelfPermission(AndruavMo7arek.AppContext, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            SmsManager smsManager = SmsManager.getDefault();
            if (smsManager == null) return;
            ArrayList<String> msgArray=smsManager.divideMessage(msg);
            smsManager.sendMultipartTextMessage(phoneNo, null, msgArray, null, null);*//*
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }*/
    }
}

