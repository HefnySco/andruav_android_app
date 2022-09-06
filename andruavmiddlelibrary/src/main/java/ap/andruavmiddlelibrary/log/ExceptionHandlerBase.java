package ap.andruavmiddlelibrary.log;


import android.content.Context;
import android.os.Build;

import com.andruav.AndruavEngine;

import java.io.PrintWriter;
import java.io.StringWriter;



/**
 * Created by mhefny on 1/31/16.
 */
public class ExceptionHandlerBase implements
        java.lang.Thread.UncaughtExceptionHandler {
    protected final Context myContext;
    protected static final String LINE_SEPARATOR = "\n";
    protected Thread.UncaughtExceptionHandler androidDefaultUEH;
    protected StringBuilder errorReport;


    public ExceptionHandlerBase(Context context) {
        myContext = context;
        androidDefaultUEH = Thread.getDefaultUncaughtExceptionHandler();

    }


    /**
     * Generate String error in errorReport;
     *
     * @param thread
     * @param exception
     */
    protected void CollectError(Thread thread, Throwable exception) {
        StringWriter stackTrace = new StringWriter();
        exception.printStackTrace(new PrintWriter(stackTrace));
        errorReport = new StringBuilder();
        errorReport.append("************ CAUSE OF ERROR ************\n\n");
        errorReport.append(stackTrace);

        errorReport.append("\n************ DEVICE INFORMATION ***********\n");
        errorReport.append("Brand: ");
        errorReport.append(Build.BRAND);
        errorReport.append(LINE_SEPARATOR);
        errorReport.append("Device: ");
        errorReport.append(Build.DEVICE);
        errorReport.append(LINE_SEPARATOR);
        errorReport.append("Model: ");
        errorReport.append(Build.MODEL);
        errorReport.append(LINE_SEPARATOR);
        errorReport.append("id: ");
        errorReport.append(Build.ID);
        errorReport.append(LINE_SEPARATOR);
        errorReport.append("Product: ");
        errorReport.append(Build.PRODUCT);
        errorReport.append(LINE_SEPARATOR);
        errorReport.append("\n************ FIRMWARE ************\n");
        errorReport.append("SDK: ");
        errorReport.append(Build.VERSION.SDK);
        errorReport.append(LINE_SEPARATOR);
        errorReport.append("Release: ");
        errorReport.append(Build.VERSION.RELEASE);
        errorReport.append(LINE_SEPARATOR);
        errorReport.append("Incremental: ");
        errorReport.append(Build.VERSION.INCREMENTAL);
        errorReport.append(LINE_SEPARATOR);
        errorReport.append("App version: ");
        errorReport.append(AndruavEngine.getPreference().getVersionName());

    }


    protected void OnAction(Thread thread, Throwable exception) {

    }

    protected void OnClose(Thread thread, Throwable exception) {

        androidDefaultUEH.uncaughtException(thread, exception);
    }

    public void uncaughtException(Thread thread, Throwable exception) {

        CollectError(thread, exception);

        OnAction(thread, exception);

        OnClose(thread, exception);


        // GMail.sendGMail(myContext, "Please Report Error Via GMAIL", "rcmobilestuff@gmail.com", "Error Report", errorReport.toString());


        //android.os.Process.killProcess(android.os.Process.myPid());
        //System.exit(10);

    }

}

