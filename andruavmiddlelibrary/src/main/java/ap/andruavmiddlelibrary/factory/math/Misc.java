package ap.andruavmiddlelibrary.factory.math;


/**
 * Created by M.Hefny on 23-Sep-14.
 */
public class Misc {

    /**
     * Return the integer part of a number
     */
    public static double abs_double(double mavlinkMsgLength) {
        double dFix;
        if (mavlinkMsgLength >= 0.0)
            dFix = Math.floor(mavlinkMsgLength);
        else
            dFix = Math.ceil(mavlinkMsgLength);
        return dFix;
    }

}
