package rcmobile.andruavmiddlelibrary.factory.util;

import android.app.ProgressDialog;
import android.content.Context;

import rcmobile.andruavmiddlelibrary.R;

/**
 * Created by mhefny on 1/22/16.
 */
public class ProgressDialogHelper {


    private  static ProgressDialog mprogressDialog;

    public static synchronized void  doProgressDialog(final Context context, final String title)
    {

        mprogressDialog = new ProgressDialog(context);
        mprogressDialog.setMessage(context.getString(R.string.action_scan));
        mprogressDialog.setTitle(title);
        mprogressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mprogressDialog.show();
    }

    public static synchronized void exitProgressDialog()
    {
        //Update UI here if needed
        if (mprogressDialog!=null) {
            mprogressDialog.dismiss();
            mprogressDialog = null;
        }

    }

}
