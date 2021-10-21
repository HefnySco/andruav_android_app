package rcmobilestuff.security;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();


        checkPermissionAndRequest (this.getApplicationContext(),"andro.jf.mypermission");


        return;
    }

    public  boolean checkPermissionAndRequest(final Context context, final String permission)
    {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;

        final int res = context.checkSelfPermission("andro.jf.mypermission");
        if (res == PackageManager.PERMISSION_DENIED)
        {
        }

        return true;

    }
}
