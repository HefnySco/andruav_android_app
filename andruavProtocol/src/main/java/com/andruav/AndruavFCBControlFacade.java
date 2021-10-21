package com.andruav;

import com.andruav.andruavUnit.AndruavUnitBase;
import com.andruav.protocol.commands.textMessages.FlightControl.AndruavMessage_Arm;
import com.andruav.protocol.commands.textMessages.FlightControl.AndruavMessage_ChangeSpeed;
import com.andruav.protocol.commands.textMessages.FlightControl.AndruavMessage_DoYAW;
import com.andruav.protocol.commands.textMessages.FlightControl.AndruavMessage_FlightControl;
import com.andruav.protocol.commands.textMessages.FlightControl.AndruavMessage_GuidedPoint;
import com.andruav.protocol.commands.textMessages.FlightControl.AndruavMessage_Land;
import com.andruav.protocol.commands.textMessages.FlightControl.AndruavMessage_ChangeAltitude;

/**
 * Created by mhefny on 12/25/16.
 */

public class AndruavFCBControlFacade extends AndruavFacadeBase {


    private static boolean isValid (final AndruavUnitBase andruavUnitBase)
    {
        return !((andruavUnitBase.getIsCGS())
                || (andruavUnitBase.IsMe())
                || (!andruavUnitBase.useFCBIMU()));
    }

    /***
     *
     * @param andruavUnitBase
     * @param isArm
     * @param emergencyDisarm valid when <b>isArm</b> equal false
     */
    public static void do_Arm (final boolean isArm, final boolean emergencyDisarm, final AndruavUnitBase andruavUnitBase)
    {
        if (!AndruavSettings.andruavWe7daBase.canControl())
        {
            return;
        }

        if (!isValid(andruavUnitBase)) return ;

        final AndruavMessage_Arm andruavResala_arm = new AndruavMessage_Arm();
        andruavResala_arm.arm = isArm;
        andruavResala_arm.emergencyDisarm = emergencyDisarm;

        sendMessage(andruavResala_arm, andruavUnitBase, Boolean.FALSE);
    }

    public static void do_Land (final AndruavUnitBase andruavUnitBase)
    {
        if (!AndruavSettings.andruavWe7daBase.canControl())
        {
            return;
        }

        if (!isValid(andruavUnitBase)) return ;

        final AndruavMessage_Land andruavResala_land= new AndruavMessage_Land();

        sendMessage(andruavResala_land, andruavUnitBase, Boolean.FALSE);
    }


    /***
     * This function does take off & change altitude. as take off is sort of change altitude.
     * @param andruavUnitBase
     * @param altitude
     */
    public static void do_ChangeAltitude(final double altitude, final AndruavUnitBase andruavUnitBase)
    {
        if (!AndruavSettings.andruavWe7daBase.canControl())
        {
            return;
        }

        if (!isValid(andruavUnitBase)) return ;

        final AndruavMessage_ChangeAltitude andruavResala_changeAltitude = new AndruavMessage_ChangeAltitude();
        andruavResala_changeAltitude.altitude = altitude;

        sendMessage(andruavResala_changeAltitude, andruavUnitBase, Boolean.FALSE);
    }


    /***
     *
     * @param andruavUnitBase
     * @param targetAngle
     * @param turnAngle
     * @param isClockwise  this is not effective unless <b>isAbsolute</b> is FALSE.
     * @param isRelative
     */
    public static void do_Yaw (double targetAngle, double turnAngle, boolean isClockwise, boolean isRelative, final AndruavUnitBase andruavUnitBase)
    {
        if (!AndruavSettings.andruavWe7daBase.canControl())
        {
            return;
        }

        if (!isValid(andruavUnitBase)) return ;

        final AndruavMessage_DoYAW andruavResala_doYAW = new AndruavMessage_DoYAW();
        andruavResala_doYAW.targetAngle = targetAngle;
        andruavResala_doYAW.turnRate = turnAngle;
        andruavResala_doYAW.isClockwise = isClockwise;
        andruavResala_doYAW.isRelative = isRelative;

        sendMessage(andruavResala_doYAW, andruavUnitBase, Boolean.FALSE);
    }


    /***
     *
     * @param target
     * @param flightMode values from {@link com.andruav.controlBoard.shared.common.FlightMode}
     */
    public static void do_FlightMode (final int flightMode, final int radius, final AndruavUnitBase target)
    {
        if (!AndruavSettings.andruavWe7daBase.canControl())
        {
            return;
        }

        if (!isValid(target)) return ;

        final AndruavMessage_FlightControl andruavResala_flightControl = new AndruavMessage_FlightControl();
        andruavResala_flightControl.FlightMode = flightMode;
        if (radius!= 0) andruavResala_flightControl.radius = radius;

        sendMessage(andruavResala_flightControl, target, Boolean.FALSE);
    }

    public static void do_FlightMode (final int flightMode,  final AndruavUnitBase target)
    {
        if (!AndruavSettings.andruavWe7daBase.canControl())
        {
            return;
        }

        do_FlightMode(flightMode,0,target);
    }

    public static void do_ChangeGroundSpeed (final double speed, final AndruavUnitBase target)
    {
        if (!AndruavSettings.andruavWe7daBase.canControl())
        {
            return;
        }

        if (!isValid(target)) return ;

        final AndruavMessage_ChangeSpeed andruavResala_changeSpeed = new AndruavMessage_ChangeSpeed();
        andruavResala_changeSpeed.speed         = speed ;
        andruavResala_changeSpeed.isGroundSpeed = true;
        andruavResala_changeSpeed.isRelative    = false;
        andruavResala_changeSpeed.throttle      = -1;

        sendMessage(andruavResala_changeSpeed, target, Boolean.FALSE);
    }


    public static void do_FlyToPoint (final double latitude, final double longitude, final double altitude, final AndruavUnitBase target)
    {
        if (!AndruavSettings.andruavWe7daBase.canControl())
        {
            return;
        }

        if ((target.getIsCGS()) || (!target.useFCBIMU())) return ;

        final AndruavMessage_GuidedPoint andruavResala_guidedPoint = new AndruavMessage_GuidedPoint();
        andruavResala_guidedPoint.Altitude=altitude;
        andruavResala_guidedPoint.Latitude=latitude;
        andruavResala_guidedPoint.Longitude=longitude;

        sendMessage(andruavResala_guidedPoint, target, Boolean.FALSE);
    }

}
