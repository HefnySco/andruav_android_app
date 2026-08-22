package ap.andruavmiddlelibrary.log;

import android.net.Uri;
import android.os.Build;
import android.util.Log;

import com.andruav.FeatureSwitch;
import ap.andruavmiddlelibrary.factory.io.FileHelper;
import ap.andruavmiddlelibrary.database.DaoManager;
import ap.andruavmiddlelibrary.database.LogDao;
import ap.andruavmiddlelibrary.database.LogRow;

import com.andruav.AndruavEngine;
import com.andruav.AndruavSettings;
import com.andruav.interfaces.ILog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;




/**
 * Created by mhefny on 1/31/16.
 *
 * Web/HTTP logging has been removed. This class now only performs DAO logging:
 * exceptions and waypoints (the latter via GenericDataDao from KMLFileHandler) are persisted
 * locally in the greenDAO database. Use {@link #exportExceptionLogAsCSV()} to dump the exception
 * log to a CSV file under Download/AndruavLogs, and {@link #clearExceptionLog()} to wipe it.
 */
public class ExceptionDaoLogger implements ILog {


    protected  final static String LINE_SEPARATOR = "\r\n";


    private static class InsertRunnable implements Runnable {
        private final String userName;
        private final String tag;
        private final String text;

        InsertRunnable(final String userName, final String tag, final String text) {
            this.userName = userName;
            this.tag = tag;
            this.text = text;
        }

        @Override
        public void run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
            try {
                final LogDao logDao = DaoManager.getLogDao();
                if (logDao == null) {
                    // could be null if crash before DaoManager.init() has been called
                    return;
                }
                if (FeatureSwitch.DEBUG_MODE) {
                    Log.e("fpv", "Insert in Database");
                }
                logDao.insert(new LogRow(null, userName, tag, text));
            } catch (Exception e) {
                if (FeatureSwitch.DEBUG_MODE) {
                    Log.e("ExceptionDaoLogger", "insert failed", e);
                }
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
     * Persists a log entry into the local DAO database (no network).
     * @param userName
     * @param tag
     * @param text
     */
    public void log(final String userName, final String tag, final String text)
    {
        try {
            Thread t = new Thread(new InsertRunnable(userName, tag, text));
            t.setDaemon(true);
            t.start();
        }
        catch (Exception e)
        {
            if (FeatureSwitch.DEBUG_MODE) {
                Log.e("ExceptionDaoLogger", "log failed", e);
            }
        }
    }


    @Override
    public void log2(String userName, String tag, String text) {
        log(userName, tag, text);
    }


    public void LogDeviceInfo (final String userName, final String tag)
    {

        log(userName, tag, "\n************ DEVICE INFORMATION ***********\n" + "Brand: " + Build.BRAND + LINE_SEPARATOR + "Device: " + Build.DEVICE + LINE_SEPARATOR + "Model: " + Build.MODEL + LINE_SEPARATOR + "id: " + Build.ID + LINE_SEPARATOR + "Product: " + Build.PRODUCT + LINE_SEPARATOR + "\n************ FIRMWARE ************\n" + "SDK: " + Build.VERSION.SDK + LINE_SEPARATOR + "Release: " + Build.VERSION.RELEASE + LINE_SEPARATOR + "Incremental: " + Build.VERSION.INCREMENTAL + LINE_SEPARATOR + "App version: " + AndruavEngine.getPreference().getVersionName());
    }


    /***
     * Exports all rows currently stored in the exception log (the {@link LogDao} table) to a CSV
     * file under the shared Downloads collection: {@code Download/AndruavLogs/exceptions_<timestamp>.csv}
     * on API 29+ (via MediaStore, so it survives app uninstall and is visible in File Manager), or
     * {@code AndruavLogs/exceptions_<timestamp>.csv} under the public Downloads directory on older
     * APIs.
     *
     * Columns: {@code id,tag,error}. The {@code error} field is RFC 4180 quoted and escapes embedded
     * double quotes by doubling them, so embedded newlines/commas/quotes are preserved.
     *
     * @return the content Uri of the written file on API 29+, or the absolute path as a
     *         {@code file://}-style Uri on older APIs, or {@code null} on failure / empty log.
     */
    public Uri exportExceptionLogAsCSV ()
    {
        final LogDao logDao = DaoManager.getLogDao();
        if (logDao == null) {
            return null;
        }

        final List<LogRow> rows;
        try {
            rows = logDao.queryBuilder().list();
        } catch (Exception e) {
            if (FeatureSwitch.DEBUG_MODE) {
                Log.e("ExceptionDaoLogger", "exportExceptionLogAsCSV query failed", e);
            }
            return null;
        }

        if ((rows == null) || rows.isEmpty()) {
            return null;
        }

        final String timeStamp =
                new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.UK).format(new Date());
        final String fileName = "exceptions_" + timeStamp + ".csv";

        final StringBuilder csv = new StringBuilder();
        csv.append("id,tag,error\n");
        for (LogRow row : rows) {
            csv.append(csvId(row.getId()));
            csv.append(',');
            csv.append(csvField(row.getTag()));
            csv.append(',');
            csv.append(csvField(row.getError()));
            csv.append('\n');
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            final String relativeFolderPath = "Download/AndruavLogs";
            final Uri uri = FileHelper.createDownloadsEntry(relativeFolderPath, fileName, "text/csv");
            if (uri == null) {
                return null;
            }
            try (OutputStream os = AndruavEngine.getPreference().getContext().getContentResolver().openOutputStream(uri)) {
                if (os == null) {
                    return null;
                }
                os.write(csv.toString().getBytes());
                os.flush();
            } catch (IOException ex) {
                AndruavEngine.log().logException("exception_log_export", ex);
                return null;
            }
            FileHelper.markMediaStoreEntryComplete(uri);
            return uri;
        } else {
            final File root = FileHelper.GetFolder("AndruavLogs", null);
            if (root == null) {
                return null;
            }
            final File file = new File(root, fileName);
            try {
                if (!file.exists() && !file.createNewFile()) {
                    return null;
                }
                try (FileOutputStream fos = new FileOutputStream(file.getAbsolutePath())) {
                    fos.write(csv.toString().getBytes());
                    fos.flush();
                }
                return Uri.fromFile(file);
            } catch (IOException ex) {
                AndruavEngine.log().logException("exception_log_export", ex);
                return null;
            }
        }
    }


    /***
     * Deletes every row from the exception log (the {@link LogDao} table). After this call
     * {@link #exportExceptionLogAsCSV()} will return {@code null} until new exceptions are logged.
     *
     * @return {@code true} if the table was cleared; {@code false} if the DAO was unavailable or
     *         the delete failed.
     */
    public boolean clearExceptionLog ()
    {
        final LogDao logDao = DaoManager.getLogDao();
        if (logDao == null) {
            return false;
        }
        try {
            logDao.deleteAll();
            return true;
        } catch (Exception e) {
            if (FeatureSwitch.DEBUG_MODE) {
                Log.e("ExceptionDaoLogger", "clearExceptionLog failed", e);
            }
            return false;
        }
    }


    private static String csvId(final Long id) {
        // id is a primary key (integer); no quoting needed. null becomes empty.
        return (id == null) ? "" : id.toString();
    }

    /***
     * Quotes a CSV field per RFC 4180: wrap in double quotes and double any embedded double quotes.
     * Embedded newlines/commas are preserved inside the quotes.
     */
    private static String csvField(final String value) {
        if (value == null) {
            return "\"\"";
        }
        // Escape backslashes first? RFC 4180 has no backslash escaping - only double-quote doubling.
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

}
