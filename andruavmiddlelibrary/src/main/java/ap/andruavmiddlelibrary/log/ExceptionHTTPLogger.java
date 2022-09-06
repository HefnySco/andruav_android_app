package ap.andruavmiddlelibrary.log;

import android.os.Build;
import android.util.Base64;
import android.util.Log;

import com.andruav.FeatureSwitch;
import ap.andruavmiddlelibrary.factory.communication.NetInfoAdapter;
import ap.andruavmiddlelibrary.preference.Preference;
import ap.andruavmiddlelibrary.database.DaoManager;
import ap.andruavmiddlelibrary.database.GenericDataDao;
import ap.andruavmiddlelibrary.database.GenericDataRow;
import ap.andruavmiddlelibrary.database.LogDao;
import ap.andruavmiddlelibrary.database.LogRow;

import com.andruav.AndruavEngine;
import com.andruav.AndruavSettings;
import com.andruav.interfaces.ILog;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;




/**
 * Created by mhefny on 1/31/16.
 */
public class ExceptionHTTPLogger implements ILog {


    private static final String Default_LOG_PATH ="http://andruav.com:9911";
    private static final String LOCAL_LOG_PATH ="http://192.168.1.144:9911"; //"http://192.168.1.139:9911"; //"http://192.168.2.42:9911";


    // protected  static AndroidLogger mlogentries;
    protected  final static String LINE_SEPARATOR = "\r\n";

    protected final static OkHttpClient mclientHTTP = new OkHttpClient();
    protected final static String  pageName = "www/ws_andruavlogger.php";


    private static class MyRunnable implements Runnable {
        private final String text;
        private final String userName;
        private final String tag;
        private final boolean skipLocal;

        MyRunnable(final String userName,final String tag, final String text, final boolean skipLocal) {
            this.userName = userName;
            this.tag = tag;
            this.text = text;
            this.skipLocal = skipLocal;
        }

        public void run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

            String query = null;
            /*try {
                query = URLEncoder.encode( text, "utf-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }*/

            query = Base64.encodeToString(text.getBytes(), Base64.NO_WRAP);

           // String url =  "http://" + FeatureSwitch.Default_LOG_PATH +  "/" + pageName + "?cmd=s&id=" + userName + "&tag=" +  tag + "&msg=" +  query;
            String url;
            if ((FeatureSwitch.DEBUG_LOG_MODE)  && (FeatureSwitch.DEBUG_MODE))
            {
                url = LOCAL_LOG_PATH; // + "?cmd=s&id=" + userName + "&tag=" +  tag + "&msg=" +  query;
            }
            else
            {
                url = Default_LOG_PATH; //  + "?cmd=s&id=" + userName + "&tag=" +  tag + "&msg=" +  query;
            }




            if (FeatureSwitch.Disable_LOG_In_Local_Server && Preference.isLocalServer(null) && !skipLocal)
            {
                return ;
            }
            url = url.replace("+","%20");
            if (FeatureSwitch.DEBUG_MODE) {
                Log.e("ExceptionHTTPLogger", url);
            }
            if (query.isEmpty()) query="NA";
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("cmd","s")
                    .addHeader("id",userName)
                    .addHeader("tag",tag)
                    .addHeader("msg",query)
                    .build();

            Call call = mclientHTTP.newCall(request);
            try {
                Response response = call.execute();
                ResponseBody responseBody =  response.body();

                if (FeatureSwitch.DEBUG_MODE) {
                    Log.e("logfpv", responseBody.string());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }



    }


    /***
     * Send Logs Data in Generic Table
     */
    public void sendOldErrors2 ()
    {
        if (FeatureSwitch.DEBUG_MODE) {
            Log.e("fpv", "SendOldError Called");
        }
        GenericDataDao genericDataDao=DaoManager.getGenericDataDao();
        if (genericDataDao==null) return;

        try {

            final List genericDataRowList = genericDataDao.queryBuilder().list();
            final int size = genericDataRowList.size();
            for (int i=0;i<size; i=i+1)
            {

                GenericDataRow genericDataRow = (GenericDataRow) genericDataRowList.get(i);
                //LogRow logRow =(LogRow) genericDataRow.getId() + ;
                log(Preference.getLoginUserName(null), "GDR" + " " + genericDataRow.getType().toString() , genericDataRow.getData());
            }
            if (FeatureSwitch.DEBUG_MODE) {
                Log.e("fpv", "SendOldError FOund");
            }
        }
        catch (Exception e)
        {

        }
        finally {
            //if (genericDataDao != null) {
            //    genericDataDao.deleteAll();

        }
    }


    public void sendOldErrors ()
    {
        if (FeatureSwitch.DEBUG_MODE) {
            Log.e("fpv", "SendOldError Called");
        }
        LogDao logDao=DaoManager.getLogDao();
        if (logDao==null) return;

        try {

            final List logs = logDao.queryBuilder().list();
            final int size = logs.size();
            for (int i=0;i<size; i=i+1)
            {
                LogRow logRow =(LogRow) logs.get(i);
                _log2(logRow.getUserName(), logRow.getTag(), logRow.getError());
            }
            if (FeatureSwitch.DEBUG_MODE) {
                Log.e("fpv", "SendOldError FOund");
            }
        }
        catch (Exception e)
        {

        }
        finally {
            if (logDao != null) {
                logDao.deleteAll();
            }
        }
    }

    public void logException (final String tag, final Exception exception) {
        logException(AndruavSettings.AccessCode,tag,exception);
    }

    public void logException (Exception exception) {
        logException(AndruavSettings.AccessCode,"exception",exception);
    }


    public void logException (String userName,String tag, Throwable exception) {
        StringBuilder errorReport;

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
        errorReport.append(AndruavEngine.getPreference());
        log(userName, tag, errorReport.toString());
    }

    public void logException (final String userName,final String tag, final Exception exception)
    {
        StringBuilder errorReport;

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
        log(userName, tag, errorReport.toString());
    }

    public void logException (final String userName,final String tag, final java.lang.VirtualMachineError error)
    {

        StringBuilder errorReport;

        StringWriter stackTrace = new StringWriter();
        error.printStackTrace(new PrintWriter(stackTrace));
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

        if (!AndruavEngine.getPreference().isAndruavLogEnabled())
        {
            return ;
        }


        log(userName, tag, errorReport.toString());
    }


    /***
     * @http http://www.andruav.com/www/ws_andruavlogger.php?cmd=s&id=me@there.com&err=THIS_IS_ERROR
     * @param userName
     * @param text
     */
    public void log(final String userName, final String tag, final String text)
    {

        // if ((NetInfoAdapter.isWifiInternetEnabled()==false) &&  (NetInfoAdapter.isMobileNetworkConnected()==false))
        try {

            Thread t = new Thread(new MyRunnable(
                    userName, /*.replaceAll("'", "%27").replaceAll("\"", "%22"), */
                    tag, //.replaceAll("'", "%27").replaceAll("\"", "%22"),
                    text, //.replaceAll("'", "%27").replaceAll("\"", "%22"),
                    false));
            t.setDaemon(true);
            t.start();

        }
        catch (Exception e)
        {
          //  Log.e("fpv",e.getMessage());
        }

    }


    private void _log2(final String userName, final String tag, final String text)
    {

        try {
            if ((!NetInfoAdapter.isOnline())||(!NetInfoAdapter.isHasValidIPAddress())) {
                // NO INTERNET ACCESS
                //TODO: you may need to log here.
                final LogDao logDao = DaoManager.getLogDao();
                if (logDao!= null)
                {
                    // could be null if crash before DaoManager.init() has been called
                    if (FeatureSwitch.DEBUG_MODE) {
                        Log.e("fpv", "Insert in Database");
                    }
                    logDao.insert(new LogRow(null, userName, tag, text));
                }
                return;
            }

            //  Log.e("logfpv", " Start a Thread");

            Thread t = new Thread(new MyRunnable(
                    userName, /*.replaceAll("'", "%27").replaceAll("\"", "%22"), */
                    tag, //.replaceAll("'", "%27").replaceAll("\"", "%22"),
                    text, //.replaceAll("'", "%27").replaceAll("\"", "%22"),
                    true
            ));
            t.setDaemon(true);
            t.start();

        }
        catch (Exception e)
        {
            //  Log.e("fpv",e.getMessage());
        }

    }

    @Override
    public void log2(String userName, String tag, String text) {
        _log2(userName, tag, text);
    }


    public void LogDeviceInfo (final String userName, final String tag)
    {

        log(userName, tag, "\n************ DEVICE INFORMATION ***********\n" + "Brand: " + Build.BRAND + LINE_SEPARATOR + "Device: " + Build.DEVICE + LINE_SEPARATOR + "Model: " + Build.MODEL + LINE_SEPARATOR + "id: " + Build.ID + LINE_SEPARATOR + "Product: " + Build.PRODUCT + LINE_SEPARATOR + "\n************ FIRMWARE ************\n" + "SDK: " + Build.VERSION.SDK + LINE_SEPARATOR + "Release: " + Build.VERSION.RELEASE + LINE_SEPARATOR + "Incremental: " + Build.VERSION.INCREMENTAL + LINE_SEPARATOR + "App version: " + AndruavEngine.getPreference().getVersionName());
    }

}