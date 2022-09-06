package ap.andruav_ap.helpers;

import android.content.Context;
import android.text.Html;
import android.text.Spanned;
import android.view.View;

import com.andruav.AndruavEngine;
import com.andruav.AndruavSettings;
import com.andruav.andruavUnit.AndruavUnitBase;
import com.andruav.andruavUnit.AndruavUnitShadow;
import com.andruav.Constants;
import com.andruav.controlBoard.shared.common.FlightMode;

import ap.andruav_ap.activities.login.drone.MainDroneActiviy;
import ap.andruav_ap.activities.remote.RemoteControlWidget;

import com.andruav.controlBoard.shared.missions.MissionBase;
import com.andruav.controlBoard.shared.missions.WayPointStep;
import com.andruav.sensors.AndruavIMU;

import java.security.InvalidParameterException;

import ap.andruav_ap.activities.login.GCSLoginShasha;
import ap.andruav_ap.App;
import ap.andruav_ap.DeviceManagerFacade;
import ap.andruav_ap.R;
import ap.andruavmiddlelibrary.factory.util.DialogHelper;
import ap.andruavmiddlelibrary.factory.util.HtmlPro;
import ap.andruavmiddlelibrary.factory.math.UnitConversion;
import ap.andruavmiddlelibrary.preference.Preference;

/**
 * Created by M.Hefny on 22-Apr-15.
 */
public class GUI {

    final static String[] StateColors = {"#F75050", "#D375D3", "#3671AB", "#75D375", "#36AB36"};

    public static int scaleDpToPixels(double value) {
        final float scale = App.getAppContext().getResources().getDisplayMetrics().density;
        return (int) Math.round(value * scale);
    }



    public static Class<?> getLoginActivity ()
    {
        if (AndruavSettings.andruavWe7daBase.getIsCGS() || App.isFirstRun)
        {
            return GCSLoginShasha.class;
        }
        else
        {
            return MainDroneActiviy.DroneLoginShasha.class;
        }
    }


    public static  String writeTextAccessCode() {
        String accessCode = Preference.getLoginAccessCode(null);

        if ((accessCode == null) || (accessCode.equals(""))) {
            return "No Access Code is defined";
        } else {
            return accessCode;
        }
    }

    public static  String writeUdpProxy() {
        if (!AndruavSettings.andruavWe7daBase.isUdpProxyEnabled())
        {
            return "<font color=#75A4D3><b>telem: </b></font><font color=#36AB36>not available</font>";
        }

        return String.format("<font color=#75A4D3><b>telem: </b></font><font color=#36AB36>%s</font><font color=#75A4D3><b>:</b></font><font color=#36AB36>%d</font>",AndruavSettings.andruavWe7daBase.getUdp_socket_ip_3rdparty(),AndruavSettings.andruavWe7daBase.getUdp_socket_port_3rdparty());

    }
    public static  String writeTextEmail() {
        String email = Preference.getLoginUserName(null);
        final StringBuffer text = new StringBuffer();
        if ((email == null) || (email.equals(""))) {
            return "No Account is defined";
        } else {
            return email;
        }
    }

    public static Spanned getButtonText (final AndruavUnitBase andruavUnit)
    {
        if (andruavUnit  == null) return Html.fromHtml("");

        final StringBuffer text = new StringBuffer();
        if (andruavUnit.getIsCGS())
        {
            text.append(HtmlPro.AddLine(App.context.getString(R.string.str_TXT_VIOLET),App.getAppContext().getString(R.string.gen_CGS),true,false));
        }
        if (andruavUnit.getIsShutdown())
        {
            text.append(HtmlPro.AddLine("#999999", andruavUnit.UnitID, true, false));

        }
        else
        {

            text.append(HtmlPro.AddLine(StateColors[andruavUnit.getConnectionState()],andruavUnit.UnitID,true,false));
        }

        text.append("<br>").append(HtmlPro.AddLine("#75D375", andruavUnit.Description, false, true));

        return Html.fromHtml(text.toString());
    }

    public static Spanned getAndruavUnitColored(final AndruavUnitBase andruavUnit)
    {
        if (andruavUnit  == null) return Html.fromHtml("");

        final StringBuffer text = new StringBuffer();
        if (andruavUnit.getIsShutdown())
        {
            text.append("<font color=").append("#999999");
        }
        else {
            text.append("<font color=").append(StateColors[andruavUnit.getConnectionState()]);
        }
        text.append(">");
        text.append(andruavUnit.UnitID);
        text.append("<br>").append(getFont(App.context.getString(R.string.str_TXT_GREEN), false, false));
        text.append(andruavUnit.Description);
        text.append("</font>");

        return Html.fromHtml(text.toString());
    }


    public static Spanned getButtonTextWayPoint (final AndruavUnitBase andruavUnit, final MissionBase wayPointStep)
    {
        if (andruavUnit  == null) return Html.fromHtml("");

        final StringBuffer text = new StringBuffer();
        final String sequence   = String.valueOf(wayPointStep.Sequence);

        text.append(getFont(App.context.getString(R.string.str_TXT_BLUE), false, false));
        text.append("Waypoint No.:");
        text.append("</font>");

        text.append(getFont(App.context.getString(R.string.str_TXT_GREEN_DARKER), false, false));
        text.append(sequence);
        text.append("</font><br>");


        if (andruavUnit.getIsShutdown())
        {
            text.append("<font color=").append("#999999");
        }
        else {
            text.append("<font color=").append(StateColors[andruavUnit.getConnectionState()]);
        }
        text.append(">");
        text.append(andruavUnit.UnitID);
        text.append("</font>").append(getFont(App.context.getString(R.string.str_TXT_GREEN), false, false)).append("<br>");
        text.append(andruavUnit.Description);
        text.append("</font>");



        return Html.fromHtml(text.toString());
    }


    public static Spanned getWaypointPopInfo(final AndruavUnitBase andruavUnit , final MissionBase missionBase)
    {
        if (andruavUnit  == null) return Html.fromHtml("");

        final StringBuffer text = new StringBuffer();


        if (missionBase instanceof WayPointStep) {
            WayPointStep wayPointStep = (WayPointStep) missionBase;
            final String altitude = String.format("%4f sec", wayPointStep.Altitude);
            final String timeToStay = String.format("%d sec", wayPointStep.TimeToStay);

            text.append(getFont(App.context.getString(R.string.str_TXT_BLUE), false, false));
            text.append("Altitude:");
            text.append("</font>");

            text.append(getFont(App.context.getString(R.string.str_TXT_GREEN_DARKER), false, false));
            text.append(altitude);
            text.append("</font><br>");



            text.append(getFont(App.context.getString(R.string.str_TXT_BLUE), false, false));
            text.append("Stay time:");
            text.append("</font>");

            text.append(getFont(App.context.getString(R.string.str_TXT_GREEN_DARKER), false, false));
            text.append(timeToStay);
            text.append("</font>");
        }




        return Html.fromHtml(text.toString());
    }

    public static Spanned getSpeedPopInfo(final AndruavUnitBase andruavUnit , final int measureUnit)
    {
        if (andruavUnit  == null) return Html.fromHtml("");

        String GroundSpeed;
        String GroundSpeed_max;
        String GroundAltitude;
        String GroundAltitude_max;
        final AndruavIMU andruavIMU = andruavUnit.getActiveIMU();
        if (measureUnit == Constants.Preferred_UNIT_METRIC_SYSTEM)
        {
            float speed = 0;
            if (andruavIMU.getCurrentLocation()!= null) {
                speed = andruavIMU.getCurrentLocation().getSpeed();
            }

            GroundSpeed         = String.format("%3.1f m/s",speed);
            GroundSpeed_max     = String.format("%3.1f m/s",andruavIMU.GroundSpeed_max);
            GroundAltitude      = String.format("%3.1f m"  ,andruavIMU.getAltitude());
            GroundAltitude_max  = String.format("%3.1f m"  ,andruavIMU.GroundAltitude_max);
        }
        else
        {
            GroundSpeed         =  String.format("%3.1f mph" ,andruavIMU.getCurrentLocation().getSpeed()    * UnitConversion.Speed_MetersPerSecondToMilePerHour);
            GroundSpeed_max     =  String.format("%3.1f mph" ,andruavIMU.GroundSpeed_max                    * UnitConversion.Speed_MetersPerSecondToMilePerHour);
            GroundAltitude      =  String.format("%6.0f feet",andruavIMU.getAltitude()                     * UnitConversion.MetersToFeet);
            GroundAltitude_max  =  String.format("%6.0f feet",andruavIMU.GroundAltitude_max                 * UnitConversion.MetersToFeet);
        }
        final StringBuffer text = new StringBuffer();
        if (andruavUnit.getIsCGS())
        {
            text.append(getFont(App.context.getString(R.string.str_TXT_BLUE),true,false)).append("NO IMU DATA").append("</b></font>");
        }

        text.append(getFont(App.context.getString(R.string.str_TXT_BLUE), false, false));
        text.append("Speed:");
        text.append("</font>");

        text.append(getFont(App.context.getString(R.string.str_TXT_GREEN_DARKER), false, false));
        text.append(GroundSpeed);
        text.append("</font>");

        text.append(getFont(App.context.getString(R.string.str_TXT_BLUE), false, false));
        text.append("    max: ");
        text.append("</font>");

        text.append(getFont(App.context.getString(R.string.str_TXT_GREEN_DARKER), false, false));
        text.append(GroundSpeed_max);
        text.append("</font>");

        text.append("<br>");

        text.append(getFont(App.context.getString(R.string.str_TXT_BLUE), false, false));
        text.append("Altitude cur:");
        text.append("</font>");

        text.append(getFont(App.context.getString(R.string.str_TXT_GREEN_DARKER), false, false));
        text.append(GroundAltitude);
        text.append("</font>");

        text.append(getFont(App.context.getString(R.string.str_TXT_BLUE), false, false));
        text.append("    max: ");
        text.append("</font>");

        text.append(getFont(App.context.getString(R.string.str_TXT_GREEN_DARKER), false, false));
        text.append(GroundAltitude_max);
        text.append("</font>");


        return Html.fromHtml(text.toString());
    }


    public static void turnOffRemote ( RemoteControlWidget remoteControlWidget)
    {
        if (remoteControlWidget ==null) return ;

        remoteControlWidget.stopEngage();
        remoteControlWidget.setVisibility(View.INVISIBLE);
    }

    public static String getFont (final String color, final Boolean isBold, final Boolean isItalic)
    {
        final StringBuffer text = new StringBuffer();
        text.append("<font color=");
        text.append(color);
        text.append(">");
        if (isBold) {
            text.append("<b>");
        }
        if (isItalic) {
            text.append("<t>");
        }

        return text.toString();
    }

    public static boolean isRemoteEngaged (final RemoteControlWidget remoteControlWidget)
    {
        return (remoteControlWidget !=null) && (remoteControlWidget.isEngaged());
    }

    /***
     * Turn Remote on Off
     * @param context
     * @param remoteControlWidget
     * @param andruavWe7da
     * @return true if remote has been turned on else false.
     */
    public static boolean toggleRemote(final Context context, final RemoteControlWidget remoteControlWidget, final AndruavUnitShadow andruavWe7da)
    {

        if (andruavWe7da == null) throw  new InvalidParameterException();

        if (DeviceManagerFacade.hasMultitouch()==false)
        {
            String err = App.getAppContext().getString(R.string.err_feature_multitouch);
            DialogHelper.doModalDialog(context, App.getAppContext().getString(R.string.title_activity_remotecontrol), err, null);
            AndruavEngine.notification().Speak(err);
            return false;
        }

        if (remoteControlWidget ==null) return false;

        final AndruavUnitBase andruavUnitBase = remoteControlWidget.getEngagedPartyID();

        if ((andruavUnitBase != null) && (andruavWe7da.PartyID.equals(andruavUnitBase.PartyID)))
        {
            remoteControlWidget.stopEngage(); // disengage me
            remoteControlWidget.setVisibility(View.INVISIBLE);

            return false; // act as Toggling Button.
        }
        else
        {
            // else Turn ON
            remoteControlWidget.setVisibility(View.VISIBLE);
            remoteControlWidget.startEngage(andruavWe7da);  // internally disengaged the old one.
        }

        return true;
    }

    public static final CharSequence[] MAVMFlightModes = {"RTL", "Auto", "Stabilize", "ALT Hold", "Manual", "Cruise", "FBWA", "TakeOff", "init..." };
    public static final int[] MAVMFlightModesNum = {FlightMode.CONST_FLIGHT_CONTROL_RTL,
            FlightMode.CONST_FLIGHT_CONTROL_AUTO,
            FlightMode.CONST_FLIGHT_CONTROL_STABILIZE,
            FlightMode.CONST_FLIGHT_CONTROL_ALT_HOLD,
            FlightMode.CONST_FLIGHT_CONTROL_MANUAL,
            FlightMode.CONST_FLIGHT_CONTROL_CRUISE,
            FlightMode.CONST_FLIGHT_CONTROL_FBWA,
            FlightMode.CONST_FLIGHT_CONTROL_TAKEOFF};



    public static String elapsed( final double date_future, final double date_now ) {

        String timeString ="";
        boolean startDisplay = false;
        // get total seconds between the times
        Double delta = Math.abs(date_future - date_now) / 1000;

        // calculate (and subtract) whole days
        final Double days = Math.floor(delta / 86400);
        delta -= days * 86400;
        if (days>0)
        {
            timeString += "D" + days.intValue();
            startDisplay = true;
        }
        // calculate (and subtract) whole hours
        final Double hours = Math.floor(delta / 3600) % 24;
        delta -= hours * 3600;
        if ((hours>0) || (startDisplay))
        {
            timeString += "h" + hours.intValue();
            startDisplay = true;
        }
        // calculate (and subtract) whole minutes
        final Double minutes = Math.floor(delta / 60) % 60;
        delta -= minutes * 60;
        if ((minutes>0) || (startDisplay))
        {
            timeString += "m" + minutes.intValue();
            startDisplay = true;
        }
        // what's left is seconds
        final Double seconds = delta % 60;  // in theory the modulus is not required
        if ((seconds>0) || (startDisplay))
        {
            timeString += "s" + seconds.intValue();
            startDisplay = true;
        }

        return timeString;
    }


    public static String elapsed2( final double date_future, final double date_now ) {

        String timeString ="";
        // get total seconds between the times
        Double delta = Math.abs(date_future - date_now) / 1000;

        // calculate (and subtract) whole days

        // calculate (and subtract) whole minutes
        final Double minutes = Math.floor(delta / 60) % 60;
        delta -= minutes * 60;
        if (minutes>0)
        {
            timeString += minutes.intValue();

        }
        else
        {
            timeString +="--";
        }
        // what's left is seconds
        final Double seconds = delta % 60;  // in theory the modulus is not required
        if (seconds>0)
        {
            timeString += ":" + seconds.intValue();

        }
        else
        {
            timeString += ":00";
        }


        return timeString;
    }

}
