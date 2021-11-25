package ap.andruav_ap.helpers;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import ap.andruav_ap.App;

/**
 * Created by M.Hefny on 19-May-15.
 */
public class Voting {


    public static void processVotingCriteria()
    {

    }

    public static void launchMarket() {

        Uri uri = Uri.parse("market://details?id=" + App.getAppContext().getPackageName());
        Intent myAppLinkToMarket = new Intent(Intent.ACTION_VIEW, uri);
        myAppLinkToMarket.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // not recommended : http://stackoverflow.com/questions/3918517/calling-startactivity-from-outside-of-an-activity-context
        try {
            App.getAppContext().startActivity(myAppLinkToMarket);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(App.getAppContext(), " unable to find market app", Toast.LENGTH_LONG).show();
        }
    }
}
