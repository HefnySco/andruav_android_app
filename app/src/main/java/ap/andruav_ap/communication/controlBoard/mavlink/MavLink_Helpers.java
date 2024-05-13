package ap.andruav_ap.communication.controlBoard.mavlink;

import com.MAVLink.enums.MAV_RESULT;
import com.MAVLink.enums.MAV_TYPE;
import com.andruav.controlBoard.shared.common.FlightMode;
import com.andruav.controlBoard.shared.common.VehicleTypes;
import com.o3dr.services.android.lib.drone.property.VehicleMode;

/**
 * Created by mhefny on 8/29/16.
 */
public class MavLink_Helpers {



    /***
     * Called by the protocol to map APM vehicle type to Andruav vehicle type.
     * @param apm_vehicleType
     */
    public static int setCommonVehicleType (final int  apm_vehicleType)
    {

        switch (apm_vehicleType) {
            case MAV_TYPE.MAV_TYPE_SUBMARINE:
                return VehicleTypes.VEHICLE_SUBMARINE;

            case MAV_TYPE.MAV_TYPE_GROUND_ROVER:
                return VehicleTypes.VEHICLE_ROVER;

            case MAV_TYPE.MAV_TYPE_TRICOPTER:
                return VehicleTypes.VEHICLE_TRI;

            case MAV_TYPE.MAV_TYPE_OCTOROTOR:
            case MAV_TYPE.MAV_TYPE_QUADROTOR:
            case MAV_TYPE.MAV_TYPE_HEXAROTOR:
                return VehicleTypes.VEHICLE_QUAD;

            case MAV_TYPE.MAV_TYPE_GENERIC:
            case MAV_TYPE.MAV_TYPE_FREE_BALLOON:

                return VehicleTypes.VEHICLE_UNKNOWN;

            case MAV_TYPE.MAV_TYPE_COAXIAL:
            case MAV_TYPE.MAV_TYPE_HELICOPTER:
                return VehicleTypes.VEHICLE_HELI;

            case MAV_TYPE.MAV_TYPE_FIXED_WING:
            case MAV_TYPE.MAV_TYPE_FLAPPING_WING:
            case MAV_TYPE.MAV_TYPE_AIRSHIP:
                return VehicleTypes.VEHICLE_PLANE;

            case MAV_TYPE.MAV_TYPE_GCS:  // sometimes returns this !!
                break;

            default:
                return VehicleTypes.VEHICLE_UNKNOWN;
        }

        return  VehicleTypes.VEHICLE_UNKNOWN;
    }


    public static boolean isCanFly (final int  apm_vehicleType)
    {
        switch (apm_vehicleType) {
            case MAV_TYPE.MAV_TYPE_FIXED_WING:
            case MAV_TYPE.MAV_TYPE_FLAPPING_WING:
            case MAV_TYPE.MAV_TYPE_AIRSHIP:
            case MAV_TYPE.MAV_TYPE_COAXIAL:
            case MAV_TYPE.MAV_TYPE_HELICOPTER:
            case MAV_TYPE.MAV_TYPE_GENERIC:
            case MAV_TYPE.MAV_TYPE_FREE_BALLOON:
            case MAV_TYPE.MAV_TYPE_OCTOROTOR:
            case MAV_TYPE.MAV_TYPE_QUADROTOR:
            case MAV_TYPE.MAV_TYPE_HEXAROTOR:
            case MAV_TYPE.MAV_TYPE_TRICOPTER:
            case MAV_TYPE.MAV_TYPE_SUBMARINE:
                return true;

            case MAV_TYPE.MAV_TYPE_GROUND_ROVER:
                return false;






            default:
                return false;
        }
    }


    /***
     * Translates Generic Flying Modes to specific APM Modes.
     * @param flighControl
     * @return
     */
    @Deprecated
    public  static long getAPMFlightControl(final int  APM_VehicleType, final int flighControl)
    {
        long mode=0;

        switch (flighControl)
        {
            case FlightMode.CONST_FLIGHT_CONTROL_RTL:
                switch (APM_VehicleType)
                {
                    case MAV_TYPE.MAV_TYPE_FIXED_WING:
                        mode = ApmModes.FIXED_WING_RTL.getNumber();
                        break;
                    case MAV_TYPE.MAV_TYPE_GROUND_ROVER:
                        mode = ApmModes.ROVER_RTL.getNumber();
                        break;
                    default:
                        mode = ApmModes.ROTOR_RTL.getNumber();
                        break;
                }

                break;

            case FlightMode.CONST_FLIGHT_CONTROL_AUTO:
                switch (APM_VehicleType)
                {
                    case MAV_TYPE.MAV_TYPE_FIXED_WING:
                        mode = ApmModes.FIXED_WING_AUTO.getNumber();
                        break;
                    case MAV_TYPE.MAV_TYPE_GROUND_ROVER:
                        mode = ApmModes.ROVER_AUTO.getNumber();
                        break;
                    default:
                        mode = ApmModes.ROTOR_AUTO.getNumber();
                }
                break;

            case FlightMode.CONST_FLIGHT_CONTROL_STABILIZE:
                switch (APM_VehicleType)
                {
                    case MAV_TYPE.MAV_TYPE_FIXED_WING:
                        mode = ApmModes.FIXED_WING_STABILIZE.getNumber();
                        break;
                    case MAV_TYPE.MAV_TYPE_GROUND_ROVER:
                        mode = ApmModes.ROVER_STEERING.getNumber();
                        break;
                    default:
                        mode = ApmModes.ROTOR_STABILIZE.getNumber();
                }
                break;

            case FlightMode.CONST_FLIGHT_CONTROL_GUIDED:
                switch  (APM_VehicleType)
                {
                    case MAV_TYPE.MAV_TYPE_FIXED_WING:
                        mode = ApmModes.FIXED_WING_GUIDED.getNumber();
                        break;
                    case MAV_TYPE.MAV_TYPE_GROUND_ROVER:
                        mode = ApmModes.ROVER_GUIDED.getNumber();
                        break;
                    default:
                        mode = ApmModes.ROTOR_GUIDED.getNumber();
                }
                break;

            case FlightMode.CONST_FLIGHT_CONTROL_LOITER:
                switch  (APM_VehicleType)
                {
                    case MAV_TYPE.MAV_TYPE_FIXED_WING:
                        mode = ApmModes.FIXED_WING_LOITER.getNumber();
                        break;
                    case MAV_TYPE.MAV_TYPE_GROUND_ROVER:
                        mode = ApmModes.ROVER_LOITER.getNumber(); // I assume that
                        break;
                    default:
                        mode = ApmModes.ROTOR_LOITER.getNumber();
                }
                break;

            case FlightMode.CONST_FLIGHT_CONTROL_ALT_HOLD:
                switch  (APM_VehicleType)
                {
                    case MAV_TYPE.MAV_TYPE_FIXED_WING:
                        mode = ApmModes.FIXED_WING_LOITER.getNumber();
                        break;
                    case MAV_TYPE.MAV_TYPE_GROUND_ROVER:
                        mode = ApmModes.ROVER_MANUAL.getNumber();  // I assume that
                        break;
                    default:
                        mode = ApmModes.ROTOR_ALT_HOLD.getNumber();
                }
                break;

            case FlightMode.CONST_FLIGHT_CONTROL_TAKEOFF:
                switch  (APM_VehicleType)
                {
                    case MAV_TYPE.MAV_TYPE_FIXED_WING:
                        mode = ApmModes.FIXED_WING_LOITER.getNumber();
                        break;
                    case MAV_TYPE.MAV_TYPE_GROUND_ROVER:
                        mode = ApmModes.ROVER_HOLD.getNumber();
                        break;
                    default:
                        mode = ApmModes.ROTOR_BRAKE.getNumber();
                }
                break;

            case FlightMode.CONST_FLIGHT_CONTROL_BRAKE:
                switch  (APM_VehicleType)
                {
                    case MAV_TYPE.MAV_TYPE_FIXED_WING:
                        mode = ApmModes.FIXED_WING_LOITER.getNumber();
                        break;
                    case MAV_TYPE.MAV_TYPE_GROUND_ROVER:
                        mode = ApmModes.ROVER_HOLD.getNumber();
                        break;
                    default:
                        mode = ApmModes.ROTOR_BRAKE.getNumber();
                }
                break;

            case FlightMode.CONST_FLIGHT_CONTROL_INITIALIZING:
                switch  (APM_VehicleType)
                {
                    case MAV_TYPE.MAV_TYPE_FIXED_WING:
                        mode = ApmModes.FIXED_WING_TRAINING.getNumber();
                        break;
                    case MAV_TYPE.MAV_TYPE_GROUND_ROVER:
                        mode = ApmModes.ROVER_INITIALIZING.getNumber();
                        break;
                    default:
                        mode = ApmModes.ROTOR_POSHOLD.getNumber();
                }
                break;


            default:
                break;
        }

        return mode;
    }

    /***
     * Mapping between requested mode and actual mode that is compatible with ArduPilot and Vehicle Type.
     * This enable using extra flying modes and uniform flying modes for different vehicles.
     * @param APM_VehicleType
     * @param flighControl
     * @return
     */
    public  static VehicleMode get3DRFlightControl(final int  APM_VehicleType, final int flighControl)
    {
        VehicleMode mode = VehicleMode.UNKNOWN;

        switch (flighControl)
        {

            case FlightMode.CONST_FLIGHT_CONTROL_TAKEOFF:
                switch (APM_VehicleType)
                {
                    case MAV_TYPE.MAV_TYPE_FIXED_WING:
                        mode = VehicleMode.PLANE_TAKEOFF;
                        break;
                    case MAV_TYPE.MAV_TYPE_GROUND_ROVER:
                        mode = VehicleMode.UNKNOWN;
                        break;
                    case MAV_TYPE.MAV_TYPE_SUBMARINE:
                        mode = VehicleMode.UNKNOWN;
                        break;
                    default:
                        mode = VehicleMode.UNKNOWN;
                        break;
                }
                break;

            case FlightMode.CONST_FLIGHT_CONTROL_BRAKE:
                switch (APM_VehicleType)
                {
                    case MAV_TYPE.MAV_TYPE_FIXED_WING:
                        mode = VehicleMode.PLANE_CIRCLE;
                        break;
                    case MAV_TYPE.MAV_TYPE_GROUND_ROVER:
                        mode = VehicleMode.ROVER_HOLD;
                        break;
                    case MAV_TYPE.MAV_TYPE_SUBMARINE:
                        mode = VehicleMode.SUBMARINE_ALT_HOLD;
                        break;
                    default:
                        mode = VehicleMode.COPTER_BRAKE;
                        break;
                }

                break;

            case FlightMode.CONST_FLIGHT_CONTROL_RTL:
                switch (APM_VehicleType)
                {
                    case MAV_TYPE.MAV_TYPE_FIXED_WING:
                        mode = VehicleMode.PLANE_RTL;
                        break;
                    case MAV_TYPE.MAV_TYPE_GROUND_ROVER:
                        mode = VehicleMode.ROVER_RTL;
                        break;
                    default:
                        mode = VehicleMode.COPTER_RTL;
                        break;
                }

                break;

            case FlightMode.CONST_FLIGHT_CONTROL_SMART_RTL:
                switch (APM_VehicleType)
                {
                    case MAV_TYPE.MAV_TYPE_FIXED_WING:
                        mode = VehicleMode.PLANE_RTL;
                        break;
                    case MAV_TYPE.MAV_TYPE_GROUND_ROVER:
                        mode = VehicleMode.ROVER_SMART_RTL;
                        break;
                    default:
                        mode = VehicleMode.COPTER_SMART_RTL;
                        break;
                }

                break;

            case FlightMode.CONST_FLIGHT_CONTROL_AUTO:
                switch (APM_VehicleType)
                {
                    case MAV_TYPE.MAV_TYPE_FIXED_WING:
                        mode = VehicleMode.PLANE_AUTO;
                        break;
                    case MAV_TYPE.MAV_TYPE_GROUND_ROVER:
                        mode = VehicleMode.ROVER_AUTO;
                        break;
                    case MAV_TYPE.MAV_TYPE_SUBMARINE:
                        mode = VehicleMode.SUBMARINE_AUTO;
                        break;
                    default:
                        mode = VehicleMode.COPTER_AUTO;
                }
                break;

            case FlightMode.CONST_FLIGHT_CONTROL_STABILIZE:
                switch (APM_VehicleType)
                {
                    case MAV_TYPE.MAV_TYPE_FIXED_WING:
                        mode = VehicleMode.PLANE_STABILIZE;
                        break;
                    case MAV_TYPE.MAV_TYPE_GROUND_ROVER:
                        mode = VehicleMode.ROVER_STEERING;
                        break;
                    case MAV_TYPE.MAV_TYPE_SUBMARINE:
                        mode = VehicleMode.SUBMARINE_STABILIZE;
                        break;
                    default:
                        mode = VehicleMode.COPTER_STABILIZE;
                }
                break;

            case FlightMode.CONST_FLIGHT_CONTROL_GUIDED:
                switch  (APM_VehicleType)
                {
                    case MAV_TYPE.MAV_TYPE_FIXED_WING:
                        mode = VehicleMode.PLANE_GUIDED;
                        break;
                    case MAV_TYPE.MAV_TYPE_GROUND_ROVER:
                        mode = VehicleMode.ROVER_GUIDED;
                        break;
                    case MAV_TYPE.MAV_TYPE_SUBMARINE:
                        mode = VehicleMode.SUBMARINE_GUIDED;
                        break;
                    default:
                        mode = VehicleMode.COPTER_GUIDED;
                }
                break;

            case FlightMode.CONST_FLIGHT_CONTROL_CIRCLE:
                switch  (APM_VehicleType)
                {
                    case MAV_TYPE.MAV_TYPE_FIXED_WING:
                        mode = VehicleMode.PLANE_CIRCLE;
                        break;
                    case MAV_TYPE.MAV_TYPE_GROUND_ROVER:
                        mode = VehicleMode.ROVER_GUIDED;
                        break;
                    case MAV_TYPE.MAV_TYPE_SUBMARINE:
                        mode = VehicleMode.SUBMARINE_CIRCLE;
                        break;
                    default:
                        mode = VehicleMode.COPTER_CIRCLE;
                }
                break;

            case FlightMode.CONST_FLIGHT_CONTROL_LOITER:
                switch  (APM_VehicleType)
                {
                    case MAV_TYPE.MAV_TYPE_FIXED_WING:
                        mode = VehicleMode.PLANE_LOITER;
                        break;
                    case MAV_TYPE.MAV_TYPE_GROUND_ROVER:
                        mode = VehicleMode.ROVER_LOITER; // I assume that
                        break;
                    default:
                        mode = VehicleMode.COPTER_LOITER;
                }
                break;

            case FlightMode.CONST_FLIGHT_CONTROL_LAND:
                switch  (APM_VehicleType)
                {
                    case MAV_TYPE.MAV_TYPE_FIXED_WING:
                        mode = VehicleMode.PLANE_LOITER;    // I assume
                        break;
                    case MAV_TYPE.MAV_TYPE_GROUND_ROVER:
                        mode = VehicleMode.ROVER_HOLD; // I assume that
                        break;
                    case MAV_TYPE.MAV_TYPE_SUBMARINE:
                        mode = VehicleMode.SUBMARINE_SURFACE;
                        break;
                    default:
                        mode = VehicleMode.COPTER_LAND;
                }
                break;

            case FlightMode.CONST_FLIGHT_CONTROL_POSTION_HOLD:
                switch  (APM_VehicleType)
                {
                    case MAV_TYPE.MAV_TYPE_FIXED_WING:
                        mode = VehicleMode.PLANE_CRUISE;
                        break;
                    case MAV_TYPE.MAV_TYPE_GROUND_ROVER:
                        mode = VehicleMode.ROVER_MANUAL;  // I assume that
                        break;
                    case MAV_TYPE.MAV_TYPE_SUBMARINE:
                        mode = VehicleMode.SUBMARINE_ALT_HOLD;
                        break;
                    default:
                        mode = VehicleMode.COPTER_POSHOLD;
                }
                break;

            case FlightMode.CONST_FLIGHT_CONTROL_ALT_HOLD:
                switch  (APM_VehicleType)
                {
                    case MAV_TYPE.MAV_TYPE_FIXED_WING:
                        mode = VehicleMode.PLANE_LOITER;
                        break;
                    case MAV_TYPE.MAV_TYPE_GROUND_ROVER:
                        mode = VehicleMode.ROVER_MANUAL;  // I assume that
                        break;
                    case MAV_TYPE.MAV_TYPE_SUBMARINE:
                        mode = VehicleMode.SUBMARINE_ALT_HOLD;
                        break;
                    default:
                        mode = VehicleMode.COPTER_ALT_HOLD;
                }
                break;

            case FlightMode.CONST_FLIGHT_CONTROL_MANUAL:
                switch  (APM_VehicleType)
                {
                    case MAV_TYPE.MAV_TYPE_FIXED_WING:
                        mode = VehicleMode.PLANE_MANUAL;
                        break;
                    case MAV_TYPE.MAV_TYPE_GROUND_ROVER:
                        mode = VehicleMode.ROVER_MANUAL;
                        break;
                    case MAV_TYPE.MAV_TYPE_SUBMARINE:
                        mode = VehicleMode.SUBMARINE_MANUAL;
                        break;
                    default:
                        mode = VehicleMode.UNKNOWN;
                }
                break;


            case FlightMode.CONST_FLIGHT_CONTROL_ACRO:
                switch  (APM_VehicleType)
                {
                    case MAV_TYPE.MAV_TYPE_FIXED_WING:
                        mode = VehicleMode.PLANE_ACRO;
                        break;
                    case MAV_TYPE.MAV_TYPE_GROUND_ROVER:
                        mode = VehicleMode.ROVER_ACRO;
                        break;
                    case MAV_TYPE.MAV_TYPE_SUBMARINE:
                        mode = VehicleMode.SUBMARINE_ACRO;
                        break;
                    default:
                        mode = VehicleMode.COPTER_ACRO;
                }
                break;

            case FlightMode.CONST_FLIGHT_CONTROL_FBWA:
                switch  (APM_VehicleType)
                {
                    case MAV_TYPE.MAV_TYPE_FIXED_WING:
                        mode = VehicleMode.PLANE_FLY_BY_WIRE_A;
                        break;
                    case MAV_TYPE.MAV_TYPE_GROUND_ROVER:
                        mode = VehicleMode.ROVER_STEERING;
                        break;
                    default:
                        mode = VehicleMode.UNKNOWN;
                }
                break;


            case FlightMode.CONST_FLIGHT_CONTROL_FBWB:
                switch  (APM_VehicleType)
                {
                    case MAV_TYPE.MAV_TYPE_FIXED_WING:
                        mode = VehicleMode.PLANE_FLY_BY_WIRE_B;
                        break;
                    case MAV_TYPE.MAV_TYPE_GROUND_ROVER:
                        mode = VehicleMode.ROVER_STEERING;
                        break;
                    default:
                        mode = VehicleMode.UNKNOWN;
                }
                break;


            case FlightMode.CONST_FLIGHT_CONTROL_CRUISE:
                switch  (APM_VehicleType)
                {
                    case MAV_TYPE.MAV_TYPE_FIXED_WING:
                        mode = VehicleMode.PLANE_CRUISE;
                        break;
                    case MAV_TYPE.MAV_TYPE_GROUND_ROVER:
                        mode = VehicleMode.ROVER_STEERING;
                        break;
                    default:
                        mode = VehicleMode.UNKNOWN;
                }
                break;

            case FlightMode.CONST_FLIGHT_CONTROL_SURFACE:
                if (APM_VehicleType == MAV_TYPE.MAV_TYPE_SUBMARINE) {
                    mode = VehicleMode.SUBMARINE_SURFACE;
                } else {
                    mode = VehicleMode.UNKNOWN;
                }
                break;

             case FlightMode.CONST_FLIGHT_CONTROL_INITIALIZING:
                switch  (APM_VehicleType)
                {
                    case MAV_TYPE.MAV_TYPE_FIXED_WING:
                        mode = VehicleMode.ROVER_INITIALIZING;
                        break;
                    case MAV_TYPE.MAV_TYPE_GROUND_ROVER:
                        mode = VehicleMode.ROVER_INITIALIZING;
                        break;
                    default:
                        mode = VehicleMode.UNKNOWN;
                }
                break;

            default:
                break;
        }

        return mode;
    }


    /**
     * Translate Ardupilot flight mode to Andruav flight mode which is almost 1 to 1
     * @param apm_vehicleType
     * @param apm_flightMode
     * @return Andruav equivelant mode.
     */
    public static int getAndruavStandardFlightMode (final int apm_vehicleType, final short apm_flightMode)
    {
        if (apm_flightMode==-1) return FlightMode.CONST_FLIGHT_CONTROL_UNKNOWN;
        final int andruavVehicleType = setCommonVehicleType (apm_vehicleType);
        switch (andruavVehicleType)
        {
            case  VehicleTypes.VEHICLE_TRI:
            case  VehicleTypes.VEHICLE_QUAD:
                switch (apm_flightMode) {

                    case 0: //Stabilize
                        return FlightMode.CONST_FLIGHT_CONTROL_STABILIZE;
                    case 1:     //Acro
                        return FlightMode.CONST_FLIGHT_CONTROL_ACRO;
                    case 13:    //Sport
                        return FlightMode.CONST_FLIGHT_CONTROL_MANUAL;
                    case 2:     //Alt Hold
                        return FlightMode.CONST_FLIGHT_CONTROL_ALT_HOLD;
                    case 3:     //Auto
                        return FlightMode.CONST_FLIGHT_CONTROL_AUTO;
                    case 4:     //Guided
                        return FlightMode.CONST_FLIGHT_CONTROL_GUIDED;
                    case 5:     //Loiter
                        return FlightMode.CONST_FLIGHT_CONTROL_LOITER;
                    case 6:     //RTL
                        return FlightMode.CONST_FLIGHT_CONTROL_RTL;
                    case 7:     //CIRCLE
                        return FlightMode.CONST_FLIGHT_CONTROL_CIRCLE;
                    case 9:     //Land
                        return FlightMode.CONST_FLIGHT_CONTROL_LAND;
                    case 16:     //PosHold
                        return FlightMode.CONST_FLIGHT_CONTROL_POSTION_HOLD;
                    case 17:     //PosHold
                        return FlightMode.CONST_FLIGHT_CONTROL_BRAKE;
                    case 21:     //PosHold
                        return FlightMode.CONST_FLIGHT_CONTROL_SMART_RTL;
                    default:
                        return FlightMode.CONST_FLIGHT_CONTROL_UNKNOWN;

                }


            case VehicleTypes.VEHICLE_ROVER:
                switch (apm_flightMode)
                {
                    case 0:     //MANUAL
                        return FlightMode.CONST_FLIGHT_CONTROL_MANUAL;
                    case 1:     //ACRO
                        return FlightMode.CONST_FLIGHT_CONTROL_ACRO;
                    case 2:     //LEARNING obsolete.
                        return FlightMode.CONST_FLIGHT_CONTROL_MANUAL;
                    case 3: //STEERING
                        return FlightMode.CONST_FLIGHT_CONTROL_STABILIZE;
                    case 10:     //Auto
                        return FlightMode.CONST_FLIGHT_CONTROL_AUTO;
                    case 15:     //Guided
                        return FlightMode.CONST_FLIGHT_CONTROL_GUIDED;
                    case 11:     //RTL
                        return FlightMode.CONST_FLIGHT_CONTROL_RTL;
                    case 5:
                        return FlightMode.CONST_FLIGHT_CONTROL_LOITER;
                    case 4:     //HOLD similar to brake mode
                        return FlightMode.CONST_FLIGHT_CONTROL_HOLD;
                    case 12:     //HOLD similar to brake mode
                        return FlightMode.CONST_FLIGHT_CONTROL_SMART_RTL;
                    case 16: //INITIALIZING
                        return FlightMode.CONST_FLIGHT_CONTROL_INITIALIZING;
                    default:
                        return FlightMode.CONST_FLIGHT_CONTROL_UNKNOWN;

                }

            case  VehicleTypes.VEHICLE_PLANE:
                switch (apm_flightMode)
                {
                    case 0:     //MANUAL
                    case 2:     //LEARNING
                        return FlightMode.CONST_FLIGHT_CONTROL_MANUAL;
                    case 4:     //Acro
                        return FlightMode.CONST_FLIGHT_CONTROL_ACRO;
                    case 3: //Training
                        return FlightMode.CONST_FLIGHT_CONTROL_STABILIZE;
                    case 5: //FBW A
                        return FlightMode.CONST_FLIGHT_CONTROL_FBWA;
                    case 6: //FBW A
                        return FlightMode.CONST_FLIGHT_CONTROL_FBWB;
                    case 7: //Cruise
                        return FlightMode.CONST_FLIGHT_CONTROL_CRUISE;
                    case 10:     //Auto
                        return FlightMode.CONST_FLIGHT_CONTROL_AUTO;
                    case 15:     //Guided
                        return FlightMode.CONST_FLIGHT_CONTROL_GUIDED;
                    case 11:     //RTL
                        return FlightMode.CONST_FLIGHT_CONTROL_RTL;
                    case 1: //Circle
                        return FlightMode.CONST_FLIGHT_CONTROL_CIRCLE;
                    case 12:
                        return FlightMode.CONST_FLIGHT_CONTROL_LOITER;
                    case 13:
                        return FlightMode.CONST_FLIGHT_CONTROL_TAKEOFF;
                    case 16: //INITIALIZING
                        return FlightMode.CONST_FLIGHT_CONTROL_INITIALIZING;
                    default:
                        return FlightMode.CONST_FLIGHT_CONTROL_UNKNOWN;

                }

            case  VehicleTypes.VEHICLE_SUBMARINE:
                /*
                STABILIZE =     0,  // manual angle with manual depth/throttle
                ACRO =          1,  // manual body-frame angular rate with manual depth/throttle
                ALT_HOLD =      2,  // manual angle with automatic depth/throttle
                AUTO =          3,  // fully automatic waypoint control using mission commands
                GUIDED =        4,  // fully automatic fly to coordinate or fly at velocity/direction using GCS immediate commands
                CIRCLE =        7,  // automatic circular flight with automatic throttle
                SURFACE =       9,  // automatically return to surface, pilot maintains horizontal control
                POSHOLD =      16,  // automatic position hold with manual override, with automatic throttle
                MANUAL =       19,  // Pass-through input with no stabilization
                MOTOR_DETECT = 20   // Automatically detect motors orientation
                */
                switch (apm_flightMode)
                {
                    case 0:     //STABILIZE
                        return FlightMode.CONST_FLIGHT_CONTROL_STABILIZE;
                    case 1:     //ACRO
                        return FlightMode.CONST_FLIGHT_CONTROL_ACRO;
                    case 19:     //ACRO
                        return FlightMode.CONST_FLIGHT_CONTROL_MANUAL;
                    case 2: //ALT_HOLD
                        return FlightMode.CONST_FLIGHT_CONTROL_ALT_HOLD;
                    case 3: //Auto
                        return FlightMode.CONST_FLIGHT_CONTROL_AUTO;
                    case 4: //GUIDED
                        return FlightMode.CONST_FLIGHT_CONTROL_GUIDED;
                    case 7: //CIRCLE
                        return FlightMode.CONST_FLIGHT_CONTROL_CIRCLE;
                    case 9: //SURFACE
                        return FlightMode.CONST_FLIGHT_CONTROL_SURFACE;
                    case 16: //POSHOLD
                        return FlightMode.CONST_FLIGHT_CONTROL_POSTION_HOLD;
                    case 20: //POSHOLD
                        return FlightMode.CONST_FLIGHT_MOTOR_DETECT;
                    default:
                        return FlightMode.CONST_FLIGHT_CONTROL_UNKNOWN;

                }

            case VehicleTypes.VEHICLE_HELI:
            default:
                return FlightMode.CONST_FLIGHT_CONTROL_UNKNOWN;
        }
    }


    public static String getACKError (int  result)
    {
        String err;
        switch (result) {
            case MAV_RESULT.MAV_RESULT_ACCEPTED:
                err = "succeeded";
                break;

            case MAV_RESULT.MAV_RESULT_TEMPORARILY_REJECTED:
                err = "Command is valid, but cannot be executed at this time. This is used to indicate a problem that should be fixed just by waiting (e.g. a state machine is busy, can't arm because have not got GPS lock, etc.). Retrying later should work.";
                break;

            case MAV_RESULT.MAV_RESULT_DENIED:
                err = "Command is invalid (is supported but has invalid parameters). Retrying same command and parameters will not work.";
                break;

            case MAV_RESULT.MAV_RESULT_UNSUPPORTED:
                err = "Command is not supported (unknown).";
                break;

            case MAV_RESULT.MAV_RESULT_FAILED:
                err = "Command is valid, but execution has failed. This is used to indicate any non-temporary or unexpected problem, i.e. any problem that must be fixed before the command can succeed/be retried. For example, attempting to write a file when out of memory, attempting to arm when sensors are not calibrated, etc.";
                break;

            case MAV_RESULT.MAV_RESULT_IN_PROGRESS:
                err = "Command is valid and is being executed. This will be followed by further progress updates, i.e. the component may send further COMMAND_ACK messages with result MAV_RESULT_IN_PROGRESS (at a rate decided by the implementation), and must terminate by sending a COMMAND_ACK message with final result of the operation. The COMMAND_ACK.progress field can be used to indicate the progress of the operation. There is no need for the sender to retry the command, but if done during execution, the component will return MAV_RESULT_IN_PROGRESS with an updated progress.";
                break;

            case MAV_RESULT.MAV_RESULT_COMMAND_LONG_ONLY:
                err = "Command is only accepted when sent as a COMMAND_LONG.";
                break;

            case MAV_RESULT.MAV_RESULT_COMMAND_INT_ONLY:
                err = "Command is only accepted when sent as a COMMAND_INT.";
                break;
            default:
                err = "Unknown";
                break;
        }

        return err;
    }
}
