package rcmobile.FPV.communication.controlBoard.mavlink;

import com.mavlink.MAVLinkPacket;
import com.mavlink.ardupilotmega.msg_ahrs;
import com.mavlink.ardupilotmega.msg_ahrs2;
import com.mavlink.ardupilotmega.msg_ahrs3;
import com.mavlink.ardupilotmega.msg_ekf_status_report;
import com.mavlink.ardupilotmega.msg_hwstatus;
import com.mavlink.ardupilotmega.msg_meminfo;
import com.mavlink.ardupilotmega.msg_mount_status;
import com.mavlink.ardupilotmega.msg_simstate;
import com.mavlink.ardupilotmega.msg_wind;
import com.mavlink.common.msg_attitude;
import com.mavlink.common.msg_battery_status;
import com.mavlink.common.msg_power_status;
import com.mavlink.common.msg_raw_imu;
import com.mavlink.common.msg_scaled_imu;
import com.mavlink.common.msg_scaled_imu2;
import com.mavlink.common.msg_scaled_imu3;
import com.mavlink.common.msg_gps_raw_int;
import com.mavlink.common.msg_gps_rtk;
import com.mavlink.common.msg_global_position_int;
import com.mavlink.common.msg_local_position_ned;
import com.mavlink.common.msg_heartbeat;
import com.mavlink.common.msg_mission_current;
import com.mavlink.common.msg_servo_output_raw;
import com.mavlink.common.msg_rc_channels;
import com.mavlink.common.msg_rc_channels_raw;
import com.mavlink.common.msg_rc_channels_scaled;
import com.mavlink.common.msg_nav_controller_output;
import com.mavlink.common.msg_scaled_pressure;
import com.mavlink.common.msg_scaled_pressure2;
import com.mavlink.common.msg_scaled_pressure3;
import com.mavlink.common.msg_sys_status;
import com.mavlink.common.msg_system_time;
import com.mavlink.common.msg_terrain_report;
import com.mavlink.common.msg_vfr_hud;
import com.mavlink.common.msg_vibration;

import com.andruav.Constants;

/**
 * Created by mhefny on 12/20/16.
 */

public class TrafficOptimizer {




    static long  smarttelemetry_msg_servo_output_raw_sent_time =0;
    final static long[]  smarttelemetry_time_msg_servo_output_raw_duration = new long[] {0,800,1000,4000};

    static long  smarttelemetry_msg_scaled_imu_sent_time =0;
    static long  smarttelemetry_msg_scaled_imu2_sent_time =0;
    static long  smarttelemetry_msg_scaled_imu3_sent_time =0;
    static long  smarttelemetry_msg_raw_imu_sent_time =0;
    final static long[] smarttelemetry_time_msg_imu_duration = new long[] {0,600,1000,2000};

    static long  smarttelemetry_msg_gps_rtk__sent_time =0;
    static long  smarttelemetry_msg_gps_raw_int_sent_time =0;
    static long  smarttelemetry_msg_global_position_int_sent_time =0;
    static long  smarttelemetry_msg_local_position_int_sent_time =0;
    final static long[]  smarttelemetry_time_msg_gps_duration = new long[] {0,400,800,1600};

    static long smarttelemetry_msg_rc_channels_raw_last_sent_time =0;
    final static long[] smarttelemetry_time_msg_rc_channels_raw_duration = new long[] {0,1200,2000,4000};

    static long smarttelemetry_msg_rc_channels_scaled_last_sent_time =0;
    final static long[] smarttelemetry_time_msg_rc_channels_scaled_duration = new long[] {0,1000,1500,3000};

    static long smarttelemetry_msg_rc_channels_last_sent_time =0;
    final static long[] smarttelemetry_time_msg_rc_channels_duration = new long[] {0,1000,1500,3000};

    static long smarttelemetry_msg_terrain_report_last_sent_time =0;
    final static long[] smarttelemetry_time_msg_terrain_report_duration = new long[] {0,1000,3000,5000};

    static long smarttelemetry_msg_heartbeat_last_sent_time =0;
    final static long[] smarttelemetry_time_msg_heartbeat_duration = new long[] {700,700,700,800};

    static long smarttelemetry_msg_sys_status_last_sent_time =0;
    final static long[] smarttelemetry_time_msg_sys_status_duration = new long[] {0,1000,3000,5000};


    static long smarttelemetry_msg_mission_current_last_sent_time = 0;
    final static long[] smarttelemetry_time_msg_mission_current_duration = new long[] {0,1000,3000,5000};


    static long smarttelemetry_msg_hwstatus_last_sent_time =0;
    final static long[] smarttelemetry_time_msg_hwstatus_duration = new long[] {0,1500,3000,5000};

    static long smarttelemetry_msg_system_time_last_sent_time =0;
    final static long[] smarttelemetry_time_msg_system_time_duration = new long[] {0,1500,2000,5000};

    static long smarttelemetry_msg_nav_controller_output_last_sent_time =0;
    final static long[] smarttelemetry_time_msg_nav_controller_output_duration = new long[] {0,200,500,800};

    static long smarttelemetry_msg_attitude_last_sent_time =0;
    final static long[] smarttelemetry_time_msg_attitude_duration = new long[] {0,300,500,1000};


    static long smarttelemetry_msg_ahrs_last_sent_time =0;
    static long smarttelemetry_msg_ahrs2_last_sent_time =0;
    static long smarttelemetry_msg_ahrs3_last_sent_time =0;
    final static long[] smarttelemetry_time_msg_ahrs_duration = new long[] {0,500,2000,5000}; //430;




    static long smarttelemetry_msg_vfr_hud_last_sent_time =0;
    final static long[] smarttelemetry_time_msg_vfr_hud_duration = new long[] {0,430,1000,1500};

    static long smarttelemetry_msg_vibration_last_sent_time =0;
    final static long[] smarttelemetry_time_msg_vibration_duration = new long[] {0,1000,2000,3000};

    static long smarttelemetry_msg_wind_last_sent_time =0;
    final static long[] smarttelemetry_time_msg_wind_duration = new long[] {0,1500,1500,3000};

    static long smarttelemetry_msg_ekf_status_report_last_sent_time =0;
    final static long[] smarttelemetry_time_msg_ekf_status_report_duration = new long[] {0,800,1200,3000}; //800;

    static long smarttelemetry_msg_meminfo_last_sent_time =0;
    final static long[] smarttelemetry_time_msg_meminfo_duration = new long[] {0,800,1500,3000};

    static long[] smarttelemetry_msg_scaled_pressure_last_sent_time = {0, 0, 0};
    final static long[] smarttelemetry_time_msg_scaled_pressure_duration = new long[] {0,800,1500,3000};

    static long  smarttelemetry_msg_simstatelast_sent_time =0;
    final static long[]  smarttelemetry_time_msg_simstate_duration = new long[] {0,1000,1000,1000};

    static long  smarttelemetry_msg_battery_status_sent_time =0;
    final static long[]  smarttelemetry_msg_battery_status_sent_duration = new long[] {0,500,1500,3000};

    static long now;

    static long smarttelemetry_msg_mount_status_last_sent_time = 0;
    final static long[] smarttelemetry_msg_mount_status_duration = new long[] {0,500,1500,2000};

    static long smarttelemetry_msg_NON_heartbeat_last_sent_time = 0;
    final static long[] smarttelemetry_msg_NON_heartbeat_duration = new long[] {1000,1500,1500,1500};

    /***
     * Indicates that only heart beat is recieved.
     * @param msgid
     * @return
     */
    static boolean noTraffic (final int msgid, final int optimizationLevel)
    {



        if (msgid == msg_heartbeat.MAVLINK_MSG_ID_HEARTBEAT)
        {
            // heart beat
            if ((now - smarttelemetry_msg_NON_heartbeat_last_sent_time) > (smarttelemetry_msg_NON_heartbeat_duration[optimizationLevel] * 2))
            {
                // heart beat only since a while
                smarttelemetry_msg_NON_heartbeat_last_sent_time = now;
                return true;
            }
        }
        else
        {
            smarttelemetry_msg_NON_heartbeat_last_sent_time = now;
        }


        // any non heat beat then OK
        return  false;
    }

    /***
     *
     * @param mavLinkPacket
     * @param optimizationLevel  check {@link Constants#SMART_TELEMETRY_LEVEL_0}
     * @return
     */
    public static  boolean shouldSend (final MAVLinkPacket mavLinkPacket, final int optimizationLevel)
    {
        now = System.currentTimeMillis();

        switch (mavLinkPacket.msgid)
        {

            case msg_heartbeat.MAVLINK_MSG_ID_HEARTBEAT:
                final msg_heartbeat msg_heartbeat = (msg_heartbeat) mavLinkPacket.unpack();
                if ((now - smarttelemetry_msg_heartbeat_last_sent_time) > smarttelemetry_time_msg_heartbeat_duration[optimizationLevel])
                {
                    smarttelemetry_msg_heartbeat_last_sent_time = now;
                    return true;
                }
                if ( hasHeartBeatChanged(msg_heartbeat, optimizationLevel)) return true;

                break;

            case msg_mount_status.MAVLINK_MSG_ID_MOUNT_STATUS:

                if ((now - smarttelemetry_msg_mount_status_last_sent_time) > smarttelemetry_msg_mount_status_duration[optimizationLevel])
                {
                    smarttelemetry_msg_mount_status_last_sent_time = now;
                    return true;
                }
                break;

            case msg_sys_status.MAVLINK_MSG_ID_SYS_STATUS:
                final msg_sys_status msg_sys_status = (msg_sys_status) mavLinkPacket.unpack();

                // 2nd level - we can ignore changes or sendMessageToModule changes only after timeout.
                if (hasSystemStatusChanged(msg_sys_status, optimizationLevel)) return true;

                if ((now - smarttelemetry_msg_sys_status_last_sent_time) > smarttelemetry_time_msg_sys_status_duration[optimizationLevel])
                {
                    smarttelemetry_msg_sys_status_last_sent_time = now;
                    return true;
                }
                break;

            case msg_scaled_imu.MAVLINK_MSG_ID_SCALED_IMU:

                if ((now - smarttelemetry_msg_scaled_imu_sent_time) > smarttelemetry_time_msg_imu_duration[optimizationLevel])
                {
                    smarttelemetry_msg_scaled_imu_sent_time = now;
                    return true;
                }
                break;
            case msg_scaled_imu2.MAVLINK_MSG_ID_SCALED_IMU2:

                if ((now - smarttelemetry_msg_scaled_imu2_sent_time) > smarttelemetry_time_msg_imu_duration[optimizationLevel])
                {
                    smarttelemetry_msg_scaled_imu2_sent_time = now;
                    return true;
                }
                break;
            case msg_scaled_imu3.MAVLINK_MSG_ID_SCALED_IMU3:

                if ((now - smarttelemetry_msg_scaled_imu3_sent_time) > smarttelemetry_time_msg_imu_duration[optimizationLevel])
                {
                    smarttelemetry_msg_scaled_imu3_sent_time = now;
                    return true;
                }
                break;
            case msg_raw_imu.MAVLINK_MSG_ID_RAW_IMU:

                if ((now - smarttelemetry_msg_raw_imu_sent_time) > smarttelemetry_time_msg_imu_duration[optimizationLevel])
                {
                    smarttelemetry_msg_raw_imu_sent_time = now;
                    return true;
                }
                break;


            case msg_servo_output_raw.MAVLINK_MSG_ID_SERVO_OUTPUT_RAW:

                if ((now - smarttelemetry_msg_servo_output_raw_sent_time) > smarttelemetry_time_msg_servo_output_raw_duration[optimizationLevel])
                {
                    smarttelemetry_msg_servo_output_raw_sent_time = now;
                    return true;
                }
                break;

            case msg_gps_raw_int.MAVLINK_MSG_ID_GPS_RAW_INT:
                if ((now - smarttelemetry_msg_gps_raw_int_sent_time) > smarttelemetry_time_msg_gps_duration[optimizationLevel])
                {
                    smarttelemetry_msg_gps_raw_int_sent_time = now;
                    return true;
                }
                break;
            case msg_gps_rtk.MAVLINK_MSG_ID_GPS_RTK:
                if ((now - smarttelemetry_msg_gps_rtk__sent_time) > smarttelemetry_time_msg_gps_duration[optimizationLevel])
                {
                    smarttelemetry_msg_gps_rtk__sent_time = now;
                    return true;
                }
                break;
            case msg_global_position_int.MAVLINK_MSG_ID_GLOBAL_POSITION_INT:
                if ((now - smarttelemetry_msg_global_position_int_sent_time) > smarttelemetry_time_msg_gps_duration[optimizationLevel])
                {
                    smarttelemetry_msg_global_position_int_sent_time = now;
                    return true;
                }
                break;
            case msg_local_position_ned.MAVLINK_MSG_ID_LOCAL_POSITION_NED:

                if ((now - smarttelemetry_msg_local_position_int_sent_time) > smarttelemetry_time_msg_gps_duration[optimizationLevel])
                {
                    smarttelemetry_msg_local_position_int_sent_time = now;
                    return true;
                }
                break;


            case msg_mission_current.MAVLINK_MSG_ID_MISSION_CURRENT:
                final msg_mission_current msg_mission_current = (msg_mission_current) mavLinkPacket.unpack();

                if (hasMission_CurrentChanged(msg_mission_current)) return  true;

                if ((now - smarttelemetry_msg_mission_current_last_sent_time) > smarttelemetry_time_msg_mission_current_duration[optimizationLevel])
                {
                    smarttelemetry_msg_mission_current_last_sent_time = now;
                    return true;
                }
                break;

            case msg_rc_channels_raw.MAVLINK_MSG_ID_RC_CHANNELS_RAW:
                if ((now - smarttelemetry_msg_rc_channels_raw_last_sent_time) > smarttelemetry_time_msg_rc_channels_raw_duration[optimizationLevel])
                {
                    smarttelemetry_msg_rc_channels_raw_last_sent_time = now;
                    return true;
                }
                break;
            case msg_rc_channels_scaled.MAVLINK_MSG_ID_RC_CHANNELS_SCALED:
                if ((now - smarttelemetry_msg_rc_channels_scaled_last_sent_time) > smarttelemetry_time_msg_rc_channels_scaled_duration[optimizationLevel])
                {
                    smarttelemetry_msg_rc_channels_scaled_last_sent_time = now;
                    return true;
                }
                break;
            case msg_rc_channels.MAVLINK_MSG_ID_RC_CHANNELS:
                if ((now - smarttelemetry_msg_rc_channels_last_sent_time) > smarttelemetry_time_msg_rc_channels_duration[optimizationLevel])
                {
                    smarttelemetry_msg_rc_channels_last_sent_time = now;
                    return true;
                }
                break;

            case msg_scaled_pressure.MAVLINK_MSG_ID_SCALED_PRESSURE:
                if ((now - smarttelemetry_msg_scaled_pressure_last_sent_time[0]) > smarttelemetry_time_msg_scaled_pressure_duration[optimizationLevel])
                {
                    smarttelemetry_msg_scaled_pressure_last_sent_time[0] = now;
                    return true;
                }
                break;
            case msg_scaled_pressure2.MAVLINK_MSG_ID_SCALED_PRESSURE2:
                if ((now - smarttelemetry_msg_scaled_pressure_last_sent_time[1]) > smarttelemetry_time_msg_scaled_pressure_duration[optimizationLevel])
                {
                    smarttelemetry_msg_scaled_pressure_last_sent_time[1] = now;
                    return true;
                }
                break;
            case msg_scaled_pressure3.MAVLINK_MSG_ID_SCALED_PRESSURE3:
                if ((now - smarttelemetry_msg_scaled_pressure_last_sent_time[2]) > smarttelemetry_time_msg_scaled_pressure_duration[optimizationLevel])
                {
                    smarttelemetry_msg_scaled_pressure_last_sent_time[2] = now;
                    return true;
                }
                break;
            case msg_meminfo.MAVLINK_MSG_ID_MEMINFO:
                if ((now - smarttelemetry_msg_meminfo_last_sent_time) > smarttelemetry_time_msg_meminfo_duration[optimizationLevel])
                {
                    smarttelemetry_msg_meminfo_last_sent_time = now;
                    return true;
                }
                break;
            case msg_ekf_status_report.MAVLINK_MSG_ID_EKF_STATUS_REPORT:
                if ((now - smarttelemetry_msg_ekf_status_report_last_sent_time) > smarttelemetry_time_msg_ekf_status_report_duration[optimizationLevel])
                {
                    smarttelemetry_msg_ekf_status_report_last_sent_time = now;
                    return true;
                }
                break;

            case msg_terrain_report.MAVLINK_MSG_ID_TERRAIN_REPORT:
                if ((now - smarttelemetry_msg_terrain_report_last_sent_time) > smarttelemetry_time_msg_terrain_report_duration[optimizationLevel])
                {
                    smarttelemetry_msg_terrain_report_last_sent_time = now;
                    return true;
                }
                break;
            case msg_wind.MAVLINK_MSG_ID_WIND:
                if ((now - smarttelemetry_msg_wind_last_sent_time) > smarttelemetry_time_msg_wind_duration[optimizationLevel])
                {
                    smarttelemetry_msg_wind_last_sent_time = now;
                    return true;
                }
                break;
            case msg_vibration.MAVLINK_MSG_ID_VIBRATION:
                if ((now - smarttelemetry_msg_vibration_last_sent_time) > smarttelemetry_time_msg_vibration_duration[optimizationLevel])
                {
                    smarttelemetry_msg_vibration_last_sent_time = now;
                    return true;
                }
                break;
            case msg_hwstatus.MAVLINK_MSG_ID_HWSTATUS:
                if ((now - smarttelemetry_msg_hwstatus_last_sent_time) > smarttelemetry_time_msg_hwstatus_duration[optimizationLevel])
                {
                    smarttelemetry_msg_hwstatus_last_sent_time = now;
                    return true;
                }
                break;
            case msg_system_time.MAVLINK_MSG_ID_SYSTEM_TIME:
                if ((now - smarttelemetry_msg_system_time_last_sent_time) > smarttelemetry_time_msg_system_time_duration[optimizationLevel])
                {
                    smarttelemetry_msg_system_time_last_sent_time = now;
                    return true;
                }
                break;
            case msg_nav_controller_output.MAVLINK_MSG_ID_NAV_CONTROLLER_OUTPUT:
                if ((now - smarttelemetry_msg_nav_controller_output_last_sent_time) > smarttelemetry_time_msg_nav_controller_output_duration[optimizationLevel])
                {
                    smarttelemetry_msg_nav_controller_output_last_sent_time = now;
                    return true;
                }
                break;
            case msg_attitude.MAVLINK_MSG_ID_ATTITUDE:
                if ((now - smarttelemetry_msg_attitude_last_sent_time) > smarttelemetry_time_msg_attitude_duration[optimizationLevel])
                {
                    smarttelemetry_msg_attitude_last_sent_time = now;
                    return true;
                }
                break;
            case msg_ahrs.MAVLINK_MSG_ID_AHRS:
                if ((now - smarttelemetry_msg_ahrs_last_sent_time) > smarttelemetry_time_msg_ahrs_duration[optimizationLevel])
                {
                    smarttelemetry_msg_ahrs_last_sent_time = now;
                    return true;
                }
                break;
            case msg_ahrs2.MAVLINK_MSG_ID_AHRS2:
                if ((now - smarttelemetry_msg_ahrs2_last_sent_time) > smarttelemetry_time_msg_ahrs_duration[optimizationLevel])
                {
                    smarttelemetry_msg_ahrs2_last_sent_time = now;
                    return true;
                }
                break;
            case msg_ahrs3.MAVLINK_MSG_ID_AHRS3:
                if ((now - smarttelemetry_msg_ahrs3_last_sent_time) > smarttelemetry_time_msg_ahrs_duration[optimizationLevel])
                {
                    smarttelemetry_msg_ahrs3_last_sent_time = now;
                    return true;
                }
                break;
            case msg_vfr_hud.MAVLINK_MSG_ID_VFR_HUD:
                if ((now - smarttelemetry_msg_vfr_hud_last_sent_time) > smarttelemetry_time_msg_vfr_hud_duration[optimizationLevel])
                {
                    smarttelemetry_msg_vfr_hud_last_sent_time = now;
                    return true;
                }
                break;
            case msg_simstate.MAVLINK_MSG_ID_SIMSTATE:
                if ((now - smarttelemetry_msg_simstatelast_sent_time) > smarttelemetry_time_msg_simstate_duration[optimizationLevel])
                {
                    smarttelemetry_msg_simstatelast_sent_time = now;
                    return true;
                }
                break;
            case msg_power_status.MAVLINK_MSG_ID_POWER_STATUS:
            case msg_battery_status.MAVLINK_MSG_ID_BATTERY_STATUS:
                if ((now - smarttelemetry_msg_battery_status_sent_time) > smarttelemetry_msg_battery_status_sent_duration[optimizationLevel])
                {
                    smarttelemetry_msg_battery_status_sent_time = now;
                    return true;
                }
                break;

            default:
                // sent true to all unhandled messages.
                return true;
        }

        return false;
    }



    /***
     * Caches heartbeat message from FCB to Drone.
     * <br>It caches it in Andruav Drone & Andruav GCS.
     * <br>It Andruav Drone is uses it to compare and detect changes, in Andruav GCS it uses it to resend internally.
     */
    static int mHearbeatCounter = 0;
    static msg_heartbeat mCahed_msg_heartbeat = null;
    static boolean hasHeartBeatChanged (final msg_heartbeat msg_heartbeat, final int OptimizationLevel)
    {

        // if feature is not active ... return always true so it is always sent.
        if(OptimizationLevel == Constants.SMART_TELEMETRY_LEVEL_0) return true;


        if (mCahed_msg_heartbeat==null)
        {
            mCahed_msg_heartbeat = msg_heartbeat;
            return true;
        }

        mHearbeatCounter = mHearbeatCounter + 1;
        if (mHearbeatCounter < 10)
        {
            // sendMessageToModule first 10 times and every 10 messages -assuming all messages are the same-.
             mCahed_msg_heartbeat = msg_heartbeat;
            return true;
        }


        boolean changed = false;
        changed = (mCahed_msg_heartbeat.autopilot != msg_heartbeat.autopilot);
        changed = (mCahed_msg_heartbeat.base_mode != msg_heartbeat.base_mode) || changed ;
        changed = (mCahed_msg_heartbeat.custom_mode != msg_heartbeat.custom_mode) || changed ;
        changed = (mCahed_msg_heartbeat.mavlink_version != msg_heartbeat.mavlink_version) || changed ;
        changed = (mCahed_msg_heartbeat.system_status != msg_heartbeat.system_status) || changed ;
        changed = (mCahed_msg_heartbeat.type!= msg_heartbeat.type) || changed ;
        changed = (mCahed_msg_heartbeat.compid!= msg_heartbeat.compid) || changed ;
        changed = (mCahed_msg_heartbeat.sysid!= msg_heartbeat.sysid) || changed ;


        mCahed_msg_heartbeat = msg_heartbeat;


        return changed;
    }


    static int mission_current_seq =-999;
    static private boolean hasMission_CurrentChanged (final msg_mission_current msg_mission_current) {
        if (msg_mission_current.seq != mission_current_seq)
        {
            mission_current_seq = msg_mission_current.seq;

            return true;
        }

        return  false;
    }
    /***
     * The idea here is that some parameters has priority over others. So you can ignore some changes, as long as they are sent each {@link #mSystemStatusCounter} times.
     * @param msg_sys_status
     * @return
     */
    static  int mSystemStatusCounter = 0;
    static protected msg_sys_status mCahed_msg_sys_status = null;

    static private boolean hasSystemStatusChanged (final msg_sys_status msg_sys_status,final int OptimizationLevel)
    {
        // if feature is not active ... return always true so it is always sent.
        if(OptimizationLevel == Constants.SMART_TELEMETRY_LEVEL_0) return true;

        if (mCahed_msg_sys_status==null)
        {
            mCahed_msg_sys_status = msg_sys_status;
            return true;
        }

        mSystemStatusCounter = mSystemStatusCounter + 1;
        if (mSystemStatusCounter < 10)
        {
            // sendMessageToModule first 10 times and every 10 messages -assuming all messages are the same-.
            mCahed_msg_sys_status = msg_sys_status;
            return true;
        }

        boolean changed = false;
        // ignored  changed = (mCahed_msg_sys_status.errors_comm != msg_sys_status.errors_comm);                            //Communication errors (UART, I2C, SPI, CAN), dropped packets on all links (packets that were corrupted on reception on the MAV)
        changed = (mCahed_msg_sys_status.errors_count1 != msg_sys_status.errors_count1) || changed ;
        changed = (mCahed_msg_sys_status.errors_count2 != msg_sys_status.errors_count2) || changed ;
        changed = (mCahed_msg_sys_status.errors_count3 != msg_sys_status.errors_count3) || changed ;
        changed = (mCahed_msg_sys_status.errors_count4 != msg_sys_status.errors_count4) || changed ;
        // ignored  changed = (mCahed_msg_sys_status.voltage_battery != msg_sys_status.voltage_battery) || changed ;        // Battery voltage, in millivolts (1 = 1 millivolt)
        // ignored  changed = (mCahed_msg_sys_status.battery_remaining != msg_sys_status.battery_remaining) || changed ;    // Remaining battery energy: (0%: 0, 100%: 100), -1: autopilot estimate the remaining battery
        // ignored  changed = (mCahed_msg_sys_status.current_battery != msg_sys_status.current_battery) || changed ;        //Battery current, in 10*milliamperes (1 = 10 milliampere), -1: autopilot does not measure the current
        // ignored  changed = (mCahed_msg_sys_status.drop_rate_comm != msg_sys_status.drop_rate_comm) || changed ;          // Communication drops in percent, (0%: 0, 100%: 10'000), (UART, I2C, SPI, CAN), dropped packets on all links (packets that were corrupted on reception on the MAV)
        // ignored  changed = (mCahed_msg_sys_status.load != msg_sys_status.load) || changed ;
        changed = (mCahed_msg_sys_status.onboard_control_sensors_enabled != msg_sys_status.onboard_control_sensors_enabled) || changed ;
        changed = (mCahed_msg_sys_status.onboard_control_sensors_health != msg_sys_status.onboard_control_sensors_health) || changed ;
        changed = (mCahed_msg_sys_status.onboard_control_sensors_present != msg_sys_status.onboard_control_sensors_present) || changed ;


        mCahed_msg_sys_status = msg_sys_status;


        return changed;
    }

}
