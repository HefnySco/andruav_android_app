package rcmobile.andruavmiddlelibrary.factory.util;

import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.Locale;

public abstract class Time_Helper {


    private static final SimpleDateFormat mDateTimeFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.UK);


    public static String getFormatedDateTime () {
        return android.text.format.DateFormat.format("yyyy-MM-dd hh:mm:ss a", new java.util.Date()).toString();
    }


    public static String getDateTimeString() {
        final GregorianCalendar now = new GregorianCalendar();

        return mDateTimeFormat.format(now.getTime());
    }
}
