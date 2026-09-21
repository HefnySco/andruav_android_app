package org.droidplanner.services.android.impl.utils.file;

import android.content.Context;
import android.os.Environment;

public class DirectoryPath {

    /**
     * Main path used to store public data files related to the app.
     * @param context application context
     * @return Path to DroneKit-Android public data directory.
     */
    public static String getPublicDataPath(Context context){
        final String root = Environment.getExternalStorageDirectory().getPath();
        return root + "/3DRServices/";
    }

}
