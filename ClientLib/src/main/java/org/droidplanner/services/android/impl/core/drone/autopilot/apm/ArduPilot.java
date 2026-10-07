package org.droidplanner.services.android.impl.core.drone.autopilot.apm;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.MAVLink.Messages.MAVLinkMessage;

import com.MAVLink.ardupilotmega.msg_mount_configure;
import com.MAVLink.ardupilotmega.msg_mount_status;
import com.MAVLink.ardupilotmega.msg_radio;
import com.MAVLink.common.msg_named_value_int;
import com.MAVLink.common.msg_statustext;
import com.MAVLink.common.msg_sys_status;
import com.MAVLink.common.msg_vfr_hud;
import com.MAVLink.enums.MAV_MOUNT_MODE;
import com.MAVLink.enums.MAV_SYS_STATUS_SENSOR;
import com.github.zafarkhaja.semver.Version;

import org.droidplanner.services.android.impl.communication.model.DataLink;
import org.droidplanner.services.android.impl.core.MAVLink.MavLinkParameters;
import org.droidplanner.services.android.impl.core.MAVLink.WaypointManager;
import org.droidplanner.services.android.impl.core.MAVLink.command.doCmd.MavLinkDoCmds;
import org.droidplanner.services.android.impl.core.drone.DroneInterfaces;
import org.droidplanner.services.android.impl.core.drone.LogMessageListener;
import org.droidplanner.services.android.impl.core.drone.autopilot.generic.GenericMavLinkDrone;
import org.droidplanner.services.android.impl.core.drone.variables.ApmModes;
import org.droidplanner.services.android.impl.core.drone.variables.GuidedPoint;
import org.droidplanner.services.android.impl.core.mission.MissionImpl;
import org.droidplanner.services.android.impl.core.model.AutopilotWarningParser;
import com.o3dr.services.android.lib.coordinate.LatLong;
import com.o3dr.services.android.lib.coordinate.LatLongAlt;
import com.o3dr.services.android.lib.drone.attribute.AttributeEvent;
import com.o3dr.services.android.lib.drone.attribute.AttributeEventExtra;
import com.o3dr.services.android.lib.drone.attribute.AttributeType;
import com.o3dr.services.android.lib.drone.attribute.error.CommandExecutionError;
import com.o3dr.services.android.lib.drone.mission.Mission;
import com.o3dr.services.android.lib.drone.property.DroneAttribute;
import com.o3dr.services.android.lib.drone.property.Parameter;
import com.o3dr.services.android.lib.drone.property.Parameters;
import com.o3dr.services.android.lib.drone.property.VehicleMode;
import com.o3dr.services.android.lib.model.AbstractCommandListener;
import com.o3dr.services.android.lib.model.ICommandListener;
import org.droidplanner.services.android.impl.utils.CommonApiUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import timber.log.Timber;

/**
 * Base class for the ArduPilot autopilots
 */
public abstract class ArduPilot extends GenericMavLinkDrone {
    public static final int AUTOPILOT_COMPONENT_ID = 1;
    public static final int ARTOO_COMPONENT_ID = 0;
    public static final int TELEMETRY_RADIO_COMPONENT_ID = 68;

    public static final String FIRMWARE_VERSION_NUMBER_REGEX = "\\d+(\\.\\d{1,2})?";

    private final MissionImpl missionImpl;
    private final GuidedPoint guidedPoint;
    private final WaypointManager waypointManager;

    protected Version firmwareVersionNumber = Version.forIntegers(0, 0, 0);
    
    public ArduPilot(String droneId, Context context, DataLink.DataLinkProvider<MAVLinkMessage> mavClient,
                     Handler handler, AutopilotWarningParser warningParser,
                     LogMessageListener logListener) {

        super(droneId, context, handler, mavClient, warningParser, logListener);

        this.waypointManager = new WaypointManager(this, handler);

        this.missionImpl = new MissionImpl(this);
        this.guidedPoint = new GuidedPoint(this, handler);
    }

    protected void setAltitudeGroundAndAirSpeeds(double altitude, double groundSpeed, double airSpeed, double climb) {
        if (this.altitude.getAltitude() != altitude) {
            this.altitude.setAltitude(altitude);
            notifyDroneEvent(DroneInterfaces.DroneEventsType.ALTITUDE);
        }

        if (speed.getGroundSpeed() != groundSpeed || speed.getAirSpeed() != airSpeed || speed.getVerticalSpeed() != climb) {
            speed.setGroundSpeed(groundSpeed);
            speed.setAirSpeed(airSpeed);
            speed.setVerticalSpeed(climb);

            notifyDroneEvent(DroneInterfaces.DroneEventsType.SPEED);
        }
    }

    @Override
    public WaypointManager getWaypointManager() {
        return waypointManager;
    }

    @Override
    public MissionImpl getMission() {
        return missionImpl;
    }

    @Override
    public GuidedPoint getGuidedPoint() {
        return guidedPoint;
    }

    @Override
    public DroneAttribute getAttribute(String attributeType) {
        if (!TextUtils.isEmpty(attributeType)) {
            switch (attributeType) {

                case AttributeType.MISSION:
                    return CommonApiUtils.getMission(this);

                case AttributeType.GUIDED_STATE:
                    return CommonApiUtils.getGuidedState(this);

            }
        }

        return super.getAttribute(attributeType);
    }

    //************ Commands ************//

    //MISSION COMMANDS
    @Override
    public void loadWaypoints() {
        CommonApiUtils.loadWaypoints(this);
    }

    @Override
    public void setMission(Mission mission, boolean pushToDrone) {
        CommonApiUtils.setMission(this, mission, pushToDrone);
    }

    @Override
    public void startMission(boolean forceModeChange, boolean forceArm, ICommandListener listener) {
        CommonApiUtils.startMission(this, forceModeChange, forceArm, listener);
    }

    //EXPERIMENTAL COMMANDS
    @Override
    public void triggerCamera() {
        CommonApiUtils.triggerCamera(this);
    }

    @Override
    public void resetROI(ICommandListener listener) {
        MavLinkDoCmds.resetROI(this, listener);
    }

    @Override
    public void setServo(int channel, int pwm, ICommandListener listener) {
        MavLinkDoCmds.setServo(this, channel, pwm, listener);
    }

    //CONTROL COMMANDS
    @Override
    public void sendGuidedPoint(LatLong point, boolean force, ICommandListener listener) {
        CommonApiUtils.sendGuidedPoint(this, point, force, listener);
    }

    @Override
    public void sendGuidedVelocityInLocalFrame(float vx, float vy, float vz, float yawRate, float yaw,
                                               short coordinateFrame, short typeMask, ICommandListener listener) {
        CommonApiUtils.setGuidedVelocityInLocalFrame(this, vx, vy, vz, yawRate, yaw, coordinateFrame, typeMask, listener);
    }

    @Override
    public void sendGuidedVelocityInGlobalFrame(float vx, float vy, float vz, float yawRate, float yaw,
                                                short coordinateFrame, short typeMask, ICommandListener listener) {
        CommonApiUtils.setGuidedVelocityInGlobalFrame(this, vx, vy, vz, yawRate, yaw, coordinateFrame, typeMask, listener);
    }

    @Override
    public void setGuidedAltitude(double altitude) {
        CommonApiUtils.setGuidedAltitude(this, altitude);
    }

    //PARAMETER COMMANDS
    @Override
    public void refreshParameters() {
        CommonApiUtils.refreshParameters(this);
    }

    @Override
    public void writeParameters(Parameters parameters) {
        CommonApiUtils.writeParameters(this, parameters);
    }

    //DRONE STATE COMMANDS
    @Override
    public void setVehicleHome(LatLongAlt homeLocation, final ICommandListener listener) {
        if (homeLocation != null) {
            MavLinkDoCmds.setVehicleHome(this, homeLocation, new AbstractCommandListener() {
                @Override
                public void onSuccess() {
                    CommonApiUtils.postSuccessEvent(listener);
                    requestHomeUpdate();
                }

                @Override
                public void onError(int executionError) {
                    CommonApiUtils.postErrorEvent(executionError, listener);
                    requestHomeUpdate();
                }

                @Override
                public void onTimeout() {
                    CommonApiUtils.postTimeoutEvent(listener);
                    requestHomeUpdate();
                }
            });
        } else {
            CommonApiUtils.postErrorEvent(CommandExecutionError.COMMAND_FAILED, listener);
        }
    }

    //************ Gimbal COMMANDS *************//
    @Override
    public void setGimbalOrientation(float pitch, float roll, float yaw, ICommandListener listener) {
        MavLinkDoCmds.setGimbalOrientation(this, pitch, roll, yaw, listener);
    }

    @Override
    public void setGimbalMountMode(int mountMode, ICommandListener listener) {
        Timber.i("Setting gimbal mount mode: %d", mountMode);

        Parameter mountParam = getParameterManager().getParameter("MNT_MODE");
        if (mountParam == null) {
            msg_mount_configure msg = new msg_mount_configure();
            msg.target_system = getSysid();
            msg.target_component = getCompid();
            msg.mount_mode = (byte) mountMode;
            msg.stab_pitch = 0;
            msg.stab_roll = 0;
            msg.stab_yaw = 0;
            getMavClient().sendMessage(msg, listener);
        } else {
            MavLinkParameters.sendParameter(this, "MNT_MODE", 1, mountMode);
        }
    }

    @Override
    public void resetGimbalMountMode(ICommandListener listener) {
        setGimbalMountMode(MAV_MOUNT_MODE.MAV_MOUNT_MODE_RC_TARGETING, listener);
    }

    @Override
    public void enableManualControl(boolean enable, ICommandListener listener) {
        CommonApiUtils.postErrorEvent(CommandExecutionError.COMMAND_UNSUPPORTED, listener);
    }

    @Override
    public void arm(boolean doArm, boolean emergencyDisarm, ICommandListener listener) {
        CommonApiUtils.arm(this, doArm, emergencyDisarm, listener);
    }

    @Override
    public void setVehicleMode(VehicleMode newMode, ICommandListener listener) {
        CommonApiUtils.changeVehicleMode(this, newMode, listener);
    }

    @Override
    public void takeoff(double altitude, ICommandListener listener) {
        CommonApiUtils.doGuidedTakeoff(this, altitude, listener);
    }

    @Override
    public void onMavLinkMessageReceived(MAVLinkMessage message) {

        // ANDRUAV SPECIFIC CHANGE .. MHEFNY:

//        if ((message.sysid != this.getSysid()) && !isMavLinkMessageException(message)) {
//            // Reject Messages that are not for the system id
//            return;
//        }

        int compId = message.compid;
        if (compId != AUTOPILOT_COMPONENT_ID
                && compId != ARTOO_COMPONENT_ID
                && compId != TELEMETRY_RADIO_COMPONENT_ID) {
            return;
        }

        if (!getParameterManager().processMessage(message)) {

            getWaypointManager().processMessage(message);

            switch (message.msgid) {

                case msg_statustext.MAVLINK_MSG_ID_STATUSTEXT:
                    // These are any warnings sent from APM:Copter with
                    // gcs_send_text_P()
                    // This includes important thing like arm fails, prearm fails, low
                    // battery, etc.
                    // also less important things like "erasing logs" and
                    // "calibrating barometer"
                    msg_statustext msg_statustext = (msg_statustext) message;
                    processStatusText(msg_statustext);
                    break;

                case msg_vfr_hud.MAVLINK_MSG_ID_VFR_HUD:
                    processVfrHud((msg_vfr_hud) message);
                    break;

                case msg_radio.MAVLINK_MSG_ID_RADIO:
                    msg_radio m_radio = (msg_radio) message;
                    processSignalUpdate(m_radio.rxerrors, m_radio.fixed, m_radio.rssi,
                            m_radio.remrssi, m_radio.txbuf, m_radio.noise, m_radio.remnoise);
                    break;

                case msg_mount_status.MAVLINK_MSG_ID_MOUNT_STATUS:
                    processMountStatus((msg_mount_status) message);
                    break;

                case msg_named_value_int.MAVLINK_MSG_ID_NAMED_VALUE_INT:
                    processNamedValueInt((msg_named_value_int) message);
                    break;

                default:
                    break;
            }
        }

        super.onMavLinkMessageReceived(message);
    }

    @Override
    protected void processSysStatus(msg_sys_status m_sys) {
        super.processSysStatus(m_sys);
        checkControlSensorsHealth(m_sys);
    }

    @Override
    protected final void setFirmwareVersion(String message) {
        super.setFirmwareVersion(message);
        setFirmwareVersionNumber(message);
    }

    protected Version getFirmwareVersionNumber() {
        return firmwareVersionNumber;
    }

    private void setFirmwareVersionNumber(String message) {
        firmwareVersionNumber = extractVersionNumber(message);
    }

    protected static Version extractVersionNumber(String firmwareVersion) {
        Version version = Version.forIntegers(0, 0, 0);

        Pattern pattern = Pattern.compile(FIRMWARE_VERSION_NUMBER_REGEX);
        Matcher matcher = pattern.matcher(firmwareVersion);
        if (matcher.find()) {
            String versionNumber = matcher.group(0) + ".0"; // Adding a default patch version number for successful parsing.

            try {
                version = Version.valueOf(versionNumber);
            } catch (Exception e){
                Timber.e(e, "Firmware version invalid");
            }
        }

        return version;
    }

    private void checkControlSensorsHealth(msg_sys_status sysStatus) {
        boolean isRCFailsafe = (sysStatus.onboard_control_sensors_health & MAV_SYS_STATUS_SENSOR
                .MAV_SYS_STATUS_SENSOR_RC_RECEIVER) == 0;
        if (isRCFailsafe) {
            getState().parseAutopilotError("RC FAILSAFE");
        }
    }

    protected void processVfrHud(msg_vfr_hud vfrHud) {
        if (vfrHud == null)
            return;

        setAltitudeGroundAndAirSpeeds(vfrHud.alt, vfrHud.groundspeed, vfrHud.airspeed, vfrHud.climb);
    }

    protected void processMountStatus(msg_mount_status mountStatus) {
        Bundle eventInfo = new Bundle(3);
        eventInfo.putFloat(AttributeEventExtra.EXTRA_GIMBAL_ORIENTATION_PITCH, mountStatus.pointing_a / 100f);
        eventInfo.putFloat(AttributeEventExtra.EXTRA_GIMBAL_ORIENTATION_ROLL, mountStatus.pointing_b / 100f);
        eventInfo.putFloat(AttributeEventExtra.EXTRA_GIMBAL_ORIENTATION_YAW, mountStatus.pointing_c / 100f);
        notifyAttributeListener(AttributeEvent.GIMBAL_ORIENTATION_UPDATED, eventInfo);
    }

    private void processNamedValueInt(msg_named_value_int message) {
        if (message == null)
            return;

        if ("ARMMASK".equals(message.getName())) {//Give information about the vehicle's ability to arm successfully.
            ApmModes vehicleMode = getState().getMode();
            if (ApmModes.isCopter(vehicleMode.getType())) {
                int value = message.value;
                boolean isReadyToArm = (value & (1 << vehicleMode.getNumber())) != 0;
                String armReadinessMsg = isReadyToArm ? "READY TO ARM" : "UNREADY FOR ARMING";
                logMessage(Log.INFO, armReadinessMsg);
            }
        }
    }

    protected void processStatusText(msg_statustext statusText) {
        String message = statusText.getText();
        if (TextUtils.isEmpty(message))
            return;

        if (message.startsWith("ArduCopter") || message.startsWith("ArduPlane")
                || message.startsWith("ArduRover")
                || message.startsWith("APM:Copter") || message.startsWith("APM:Plane")
                || message.startsWith("APM:Rover")) {
            setFirmwareVersion(message);
        } else {

            //Try parsing as an error.
            if (!getState().parseAutopilotError(message)) {

                //Relay to the connected client.
                int logLevel;
                switch (statusText.severity) {
                    case APMConstants.Severity.SEVERITY_CRITICAL:
                        logLevel = Log.ERROR;
                        break;

                    case APMConstants.Severity.SEVERITY_HIGH:
                        logLevel = Log.WARN;
                        break;

                    case APMConstants.Severity.SEVERITY_MEDIUM:
                        logLevel = Log.INFO;
                        break;

                    default:
                    case APMConstants.Severity.SEVERITY_LOW:
                        logLevel = Log.VERBOSE;
                        break;

                    case APMConstants.Severity.SEVERITY_USER_RESPONSE:
                        logLevel = Log.DEBUG;
                        break;
                }

                logMessage(logLevel, message);
            }
        }
    }

    public Double getBattDischarge(double battRemain) {
        Parameter battCap = getParameterManager().getParameter("BATT_CAPACITY");
        if (battCap == null || battRemain == -1) {
            return null;
        }
        return (1 - battRemain / 100.0) * battCap.getValue();
    }

    @Override
    protected void processBatteryUpdate(double voltage, double remain, double current) {
        if (battery.getBatteryRemain() != remain) {
            battery.setBatteryDischarge(getBattDischarge(remain));
        }
        super.processBatteryUpdate(voltage, remain, current);
    }
}
