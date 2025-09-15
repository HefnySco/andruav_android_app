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



}
