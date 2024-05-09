package ap.andruav_ap.activities.login;

import android.content.Context;
import android.content.Intent;

import com.andruav.AndruavSettings;

import ap.andruav_ap.activities.login.drone.MainDroneActiviy;

/**
 * Created by mhefny on 4/21/16.
 */
public class LoginScreenFactory {

    public static Intent getIntentLoginActivity(Context context) {

        Intent intent;

        // you can add a select statement here to display different activities.
        intent = new Intent(context, MainDroneActiviy.DroneLoginShasha.class);

        return intent;
    }


    public static void startLoginActivity(Context context) {


        final Intent intent = getIntentLoginActivity(context);

        context.startActivity(intent);
    }
}
