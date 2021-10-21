package rcmobile.andruavmiddlelibrary.log;

import android.content.Context;
import android.util.Log;

import com.andruav.AndruavEngine;
import com.andruav.AndruavSettings;
import com.andruav.FeatureSwitch;

import rcmobile.andruavmiddlelibrary.database.DaoManager;
import rcmobile.andruavmiddlelibrary.database.LogDao;
import rcmobile.andruavmiddlelibrary.database.LogRow;

/**
 * Created by M.Hefny on 01-Nov-14.
 */
public class ExceptionHandler  extends ExceptionHandlerBase {


    private final Context mcontext;
    public ExceptionHandler(Context context) {
        super(context);

        mcontext = context;


    }





    /***
     * Collecting Error info in errorReport variable.
     * @param thread
     * @param err
     */
    @Override
    protected void CollectError (Thread thread, Throwable err) {

        super.CollectError(thread, err);

        LogDao logDao = DaoManager.getLogDao();
        if (logDao!= null)
        {
            // could be null if crash before DaoManager.init() has been called
            if (FeatureSwitch.DEBUG_MODE) {
                Log.e("fpv", "Insert in Database");
            }
            logDao.insert(new LogRow(null, AndruavSettings.Account_SID, "FATAL", errorReport.toString()));
            DaoManager.closeAll();

        }

        AndruavEngine.log().log(AndruavSettings.Account_SID, "FATAL", errorReport.toString());

    }
    /***
     * Determin the way to close APP
     * you can give control to default handler or shut the app or restart it
     * @param textAppearanceAppCompatActivityButtonActive
     * @param text
     */
    @Override
    protected void OnClose(Thread textAppearanceAppCompatActivityButtonActive, Throwable text) {
       // AlarmManager i = (AlarmManager) myContext.getSystemService(Context.ALARM_SERVICE);
       // i.set(AlarmManager.RTC, System.currentTimeMillis() + 2000, App.MainPendingIntent);
        System.exit(2);

    }





}
