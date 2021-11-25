package ap.andruavmiddlelibrary.factory.util;

import android.app.ActivityManager;
import android.content.Context;

import com.andruav.AndruavEngine;


/**
 * Created by M.Hefny on 11-Aug-15.
 */
public class MemoryHelper {


    /***
     * Gets Memory available and used by App.
     * @see  "http://developer.android.com/reference/android/app/ActivityManager.MemoryInfo.html"
     * @return
     */
    public static ActivityManager.MemoryInfo getMemoryAvailable ()
    {

        try {
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            ActivityManager activityManager = (ActivityManager) AndruavEngine.getPreference().getContext().getSystemService(Context.ACTIVITY_SERVICE);
            activityManager.getMemoryInfo(mi);
            return mi;
        }
        catch (Exception e)
        {
            return null;
        }
    }


    /***
     * More like storage memory.
     * @return
     */
    public static long getMemoryUsedByApp ()
    {
        try
        {
        long freeSize = 0L;
        long totalSize = 0L;
        long usedSize = -1L;
            Runtime info = Runtime.getRuntime();
            freeSize = info.freeMemory();
            totalSize = info.totalMemory();
            usedSize = totalSize - freeSize;
            return usedSize;

        } catch (Exception e) {
            e.printStackTrace();
            return -1;

        }

    }

}
