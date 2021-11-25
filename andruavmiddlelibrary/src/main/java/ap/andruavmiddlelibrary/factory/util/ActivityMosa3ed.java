package ap.andruavmiddlelibrary.factory.util;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by mhefny on 2/8/17.
 */

public class ActivityMosa3ed {


    public static View getRootViewOfActivity(final Activity ac)
    {
        return ac.findViewById(android.R.id.content).getRootView();
    }


    public static void removeMeFromParentView(final View me)
    {
        ((ViewGroup)(me.getParent())).removeView(me);
    }


}
