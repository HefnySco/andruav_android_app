package org.droidplanner.services.android.impl.utils;



import com.o3dr.services.android.lib.coordinate.LatLongAlt;
import com.o3dr.services.android.lib.drone.mission.item.MissionItem;
import com.o3dr.services.android.lib.drone.mission.item.command.CameraControl;
import com.o3dr.services.android.lib.drone.mission.item.command.CameraTrigger;
import com.o3dr.services.android.lib.drone.mission.item.command.ChangeSpeed;
import com.o3dr.services.android.lib.drone.mission.item.command.DoJump;
import com.o3dr.services.android.lib.drone.mission.item.command.EpmGripper;
import com.o3dr.services.android.lib.drone.mission.item.command.ResetROI;
import com.o3dr.services.android.lib.drone.mission.item.command.ReturnToLaunch;
import com.o3dr.services.android.lib.drone.mission.item.command.SetRelay;
import com.o3dr.services.android.lib.drone.mission.item.command.SetServo;
import com.o3dr.services.android.lib.drone.mission.item.command.Takeoff;
import com.o3dr.services.android.lib.drone.mission.item.command.YawCondition;
import com.o3dr.services.android.lib.drone.mission.item.spatial.Circle;
import com.o3dr.services.android.lib.drone.mission.item.spatial.DoLandStart;
import com.o3dr.services.android.lib.drone.mission.item.spatial.Land;
import com.o3dr.services.android.lib.drone.mission.item.spatial.RegionOfInterest;
import com.o3dr.services.android.lib.drone.mission.item.spatial.SplineWaypoint;
import com.o3dr.services.android.lib.drone.mission.item.spatial.Waypoint;

import org.droidplanner.services.android.impl.core.mission.MissionImpl;
import org.droidplanner.services.android.impl.core.mission.MissionItemImpl;
import org.droidplanner.services.android.impl.core.mission.commands.CameraControlImpl;
import org.droidplanner.services.android.impl.core.mission.commands.CameraTriggerImpl;
import org.droidplanner.services.android.impl.core.mission.commands.ChangeSpeedImpl;
import org.droidplanner.services.android.impl.core.mission.commands.ConditionYawImpl;
import org.droidplanner.services.android.impl.core.mission.commands.DoJumpImpl;
import org.droidplanner.services.android.impl.core.mission.commands.EpmGripperImpl;
import org.droidplanner.services.android.impl.core.mission.commands.ReturnToHomeImpl;
import org.droidplanner.services.android.impl.core.mission.commands.SetRelayImpl;
import org.droidplanner.services.android.impl.core.mission.commands.SetServoImpl;
import org.droidplanner.services.android.impl.core.mission.commands.TakeoffImpl;
import org.droidplanner.services.android.impl.core.mission.waypoints.CircleImpl;
import org.droidplanner.services.android.impl.core.mission.waypoints.DoLandStartImpl;
import org.droidplanner.services.android.impl.core.mission.waypoints.LandImpl;
import org.droidplanner.services.android.impl.core.mission.waypoints.RegionOfInterestImpl;
import org.droidplanner.services.android.impl.core.mission.waypoints.SplineWaypointImpl;
import org.droidplanner.services.android.impl.core.mission.waypoints.WaypointImpl;

/**
 * Created by fhuya on 11/10/14.
 */
public class ProxyUtils {

    public static MissionItemImpl getMissionItemImpl(MissionImpl missionImpl, MissionItem proxyItem) {
        if (proxyItem == null)
            return null;

        MissionItemImpl missionItemImpl;
        switch (proxyItem.getType()) {

            case CAMERA_TRIGGER: {
                CameraTrigger proxy = (CameraTrigger) proxyItem;

                CameraTriggerImpl temp = new CameraTriggerImpl(missionImpl, proxy.getTriggerDistance(), proxy.getShutter(), proxy.getTrigger());

                missionItemImpl = temp;
                break;
            }
            case CAMERA_CONTROL: {
                CameraControl proxy = (CameraControl) proxyItem;

                CameraControlImpl temp = new CameraControlImpl(missionImpl, proxy.getSessionControl(), proxy.getZoomAbsolute(),
                        proxy.getZoomRelative(), proxy.getFocus(),
                        proxy.getShootCommand(), proxy.getCommandIdentity(),
                        proxy.getShotID()
                        );

                missionItemImpl = temp;
                break;
            }
            case CHANGE_SPEED: {
                ChangeSpeed proxy = (ChangeSpeed) proxyItem;

                ChangeSpeedImpl temp = new ChangeSpeedImpl(missionImpl, proxy.getSpeed());

                missionItemImpl = temp;
                break;
            }
            case EPM_GRIPPER: {
                EpmGripper proxy = (EpmGripper) proxyItem;

                EpmGripperImpl temp = new EpmGripperImpl(missionImpl, proxy.isRelease());

                missionItemImpl = temp;
                break;
            }
            case RETURN_TO_LAUNCH: {
                ReturnToLaunch proxy = (ReturnToLaunch) proxyItem;

                ReturnToHomeImpl temp = new ReturnToHomeImpl(missionImpl);
                temp.setHeight((proxy.getReturnAltitude()));

                missionItemImpl = temp;
                break;
            }
            case SET_SERVO: {
                SetServo proxy = (SetServo) proxyItem;

                SetServoImpl temp = new SetServoImpl(missionImpl, proxy.getChannel(), proxy.getPwm());

                missionItemImpl = temp;
                break;
            }
            case TAKEOFF: {
                Takeoff proxy = (Takeoff) proxyItem;

                TakeoffImpl temp = new TakeoffImpl(missionImpl, proxy.getTakeoffAltitude(),
                    proxy.getTakeoffPitch());

                missionItemImpl = temp;
                break;
            }
            case CIRCLE: {
                Circle proxy = (Circle) proxyItem;

                CircleImpl temp = new CircleImpl(missionImpl, (proxy.getCoordinate()));
                temp.setRadius(proxy.getRadius());
                temp.setTurns(proxy.getTurns());

                missionItemImpl = temp;
                break;
            }
            case LAND: {
                Land proxy = (Land) proxyItem;

                LandImpl temp = new LandImpl(missionImpl, (proxy.getCoordinate()));

                missionItemImpl = temp;
                break;
            }
            case DO_LAND_START: {
                DoLandStart proxy = (DoLandStart) proxyItem;

                DoLandStartImpl temp = new DoLandStartImpl(missionImpl, (proxy.getCoordinate()));

                missionItemImpl = temp;
                break;
            }

            case REGION_OF_INTEREST: {
                RegionOfInterest proxy = (RegionOfInterest) proxyItem;

                RegionOfInterestImpl temp = new RegionOfInterestImpl(missionImpl, (proxy.getCoordinate()));

                missionItemImpl = temp;
                break;
            }

            case RESET_ROI: {
                //Sending a roi with all coordinates set to 0 will reset the current roi.
                RegionOfInterestImpl temp = new RegionOfInterestImpl(missionImpl, new LatLongAlt(0, 0, 0));
                missionItemImpl = temp;
                break;
            }

            case SPLINE_WAYPOINT: {
                SplineWaypoint proxy = (SplineWaypoint) proxyItem;

                SplineWaypointImpl temp = new SplineWaypointImpl(missionImpl, (proxy.getCoordinate()));
                temp.setDelay(proxy.getDelay());

                missionItemImpl = temp;
                break;
            }
            case WAYPOINT: {
                Waypoint proxy = (Waypoint) proxyItem;

                WaypointImpl temp = new WaypointImpl(missionImpl, (proxy
                        .getCoordinate()));
                temp.setAcceptanceRadius(proxy.getAcceptanceRadius());
                temp.setDelay(proxy.getDelay());
                temp.setOrbitCCW(proxy.isOrbitCCW());
                temp.setOrbitalRadius(proxy.getOrbitalRadius());
                temp.setYawAngle(proxy.getYawAngle());

                missionItemImpl = temp;
                break;
            }
            case YAW_CONDITION: {
                YawCondition proxy = (YawCondition) proxyItem;

                ConditionYawImpl temp = new ConditionYawImpl(missionImpl, proxy.getAngle(), proxy.isRelative());
                temp.setAngularSpeed(proxy.getAngularSpeed());

                missionItemImpl = temp;
                break;
            }

            case SET_RELAY: {
                SetRelay proxy = (SetRelay) proxyItem;
                missionItemImpl = new SetRelayImpl(missionImpl, proxy.getRelayNumber(), proxy.isEnabled());
                break;
            }

            case DO_JUMP: {
                DoJump proxy = (DoJump) proxyItem;
                missionItemImpl = new DoJumpImpl(missionImpl, proxy.getWaypoint(), proxy.getRepeatCount());
                break;
            }

            default:
                missionItemImpl = null;
                break;
        }

        return missionItemImpl;
    }

    public static MissionItem getProxyMissionItem(MissionItemImpl itemImpl) {
        if (itemImpl == null)
            return null;

        MissionItem proxyMissionItem;
        switch (itemImpl.getType()) {
            case WAYPOINT: {
                WaypointImpl source = (WaypointImpl) itemImpl;

                Waypoint temp = new Waypoint();
                temp.setCoordinate((source.getCoordinate()));
                temp.setAcceptanceRadius(source.getAcceptanceRadius());
                temp.setDelay(source.getDelay());
                temp.setOrbitalRadius(source.getOrbitalRadius());
                temp.setOrbitCCW(source.isOrbitCCW());
                temp.setYawAngle(source.getYawAngle());

                proxyMissionItem = temp;
                break;
            }

            case SPLINE_WAYPOINT: {
                SplineWaypointImpl source = (SplineWaypointImpl) itemImpl;

                SplineWaypoint temp = new SplineWaypoint();
                temp.setCoordinate((source.getCoordinate()));
                temp.setDelay(source.getDelay());

                proxyMissionItem = temp;
                break;
            }

            case TAKEOFF: {
                TakeoffImpl source = (TakeoffImpl) itemImpl;

                Takeoff temp = new Takeoff();
                temp.setTakeoffAltitude(source.getFinishedAlt());
                temp.setTakeoffPitch(source.getPitch());

                proxyMissionItem = temp;
                break;
            }
            case RTL: {
                ReturnToHomeImpl source = (ReturnToHomeImpl) itemImpl;

                ReturnToLaunch temp = new ReturnToLaunch();
                temp.setReturnAltitude(source.getHeight());

                proxyMissionItem = temp;
                break;
            }
            case LAND: {
                LandImpl source = (LandImpl) itemImpl;

                Land temp = new Land();
                temp.setCoordinate((source.getCoordinate()));

                proxyMissionItem = temp;
                break;
            }
            case DO_LAND_START: {
                DoLandStartImpl source = (DoLandStartImpl) itemImpl;

                DoLandStart temp = new DoLandStart();
                temp.setCoordinate((source.getCoordinate()));

                proxyMissionItem = temp;
                break;
            }
            case CIRCLE: {
                CircleImpl source = (CircleImpl) itemImpl;

                Circle temp = new Circle();
                temp.setCoordinate((source.getCoordinate()));
                temp.setRadius(source.getRadius());
                temp.setTurns(source.getNumberOfTurns());

                proxyMissionItem = temp;
                break;
            }

            case ROI: {
                RegionOfInterestImpl source = (RegionOfInterestImpl) itemImpl;

                if(source.isReset()){
                    ResetROI temp = new ResetROI();
                    proxyMissionItem = temp;
                }
                else {
                    RegionOfInterest temp = new RegionOfInterest();
                    temp.setCoordinate((source.getCoordinate()));

                    proxyMissionItem = temp;
                }
                break;
            }

            case CHANGE_SPEED: {
                ChangeSpeedImpl source = (ChangeSpeedImpl) itemImpl;

                ChangeSpeed temp = new ChangeSpeed();
                temp.setSpeed(source.getSpeed());

                proxyMissionItem = temp;
                break;
            }

            case CAMERA_TRIGGER: {
                CameraTriggerImpl source = (CameraTriggerImpl) itemImpl;

                CameraTrigger temp = new CameraTrigger();
                temp.setTriggerDistance(source.getTriggerDistance());
                temp.setShutter(source.getShutter());
                temp.setTrigger(source.getTrigger());

                proxyMissionItem = temp;
                break;
            }
            case CAMERA_CONTROL: {
                CameraControlImpl source = (CameraControlImpl) itemImpl;

                CameraControl temp = new CameraControl();
                temp.setSessionControl(source.getSessionControl());
                temp.setZoomAbsolute(source.getZoomAbsolute());
                temp.setZoomRelative(source.getZoomRelative());
                temp.setFocus(source.getFocus());
                temp.setShootCommand(source.getShootCommand());
                temp.setCommandIdentity(source.getCommandIdentity());
                temp.setShotID(source.getShotID());

                proxyMissionItem = temp;
                break;
            }

            case EPM_GRIPPER: {
                EpmGripperImpl source = (EpmGripperImpl) itemImpl;

                EpmGripper temp = new EpmGripper();
                temp.setRelease(source.isRelease());

                proxyMissionItem = temp;
                break;
            }

            case SET_SERVO: {
                SetServoImpl source = (SetServoImpl) itemImpl;

                SetServo temp = new SetServo();
                temp.setChannel(source.getChannel());
                temp.setPwm(source.getPwm());

                proxyMissionItem = temp;
                break;
            }
            case CONDITION_YAW: {
                ConditionYawImpl source = (ConditionYawImpl) itemImpl;

                YawCondition temp = new YawCondition();
                temp.setAngle(source.getAngle());
                temp.setAngularSpeed(source.getAngularSpeed());
                temp.setRelative(source.isRelative());

                proxyMissionItem = temp;
                break;
            }

            case SET_RELAY: {
                SetRelayImpl impl = (SetRelayImpl) itemImpl;

                SetRelay proxy = new SetRelay();
                proxy.setRelayNumber(impl.getRelayNumber());
                proxy.setEnabled(impl.isEnabled());

                proxyMissionItem = proxy;
                break;
            }

            case DO_JUMP: {
                DoJumpImpl source = (DoJumpImpl) itemImpl;

                DoJump proxy = new DoJump();
                proxy.setWaypoint(source.getWaypoint());
                proxy.setRepeatCount(source.getRepeatCount());

                proxyMissionItem = proxy;
                break;
            }

            default:
                proxyMissionItem = null;
                break;
        }

        return proxyMissionItem;
    }

}
