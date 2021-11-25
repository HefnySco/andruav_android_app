package ap.andruav_ap.activities.map;
import android.location.Location;
import android.os.Handler;
import android.os.Message;

import com.andruav.andruavUnit.AndruavUnitBase;
import com.andruav.event.droneReport_7adath._7adath_GPS_Ready;
import com.andruav.event.droneReport_7adath._7adath_GeoFence_Hit;
import com.andruav.event.droneReport_7adath._7adath_GeoFence_Ready;
import com.andruav.event.droneReport_7adath._7adath_HomeLocation_Ready;
import com.andruav.event.droneReport_7adath._7adath_WayPointReached;
import com.andruav.event.droneReport_7adath._7adath_WayPointsRecieved;
import com.andruav.util.GPSHelper;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polyline;

import java.util.List;

import de.greenrobot.event.EventBus;
import ap.andruavmiddlelibrary.factory.math.Angles;

/**
 * Created by mhefny on 3/17/16.
 */
public class AndruavMapWidget extends AndruavMapBaseWidget {


     //////////BUS EVENT


    /***
     * A new GeoFencePoint data has been received either from Me as a Drone or from Another Drone.
     * @param a7adath_geoFence_ready
     */
    public void onEvent (final _7adath_GeoFence_Ready a7adath_geoFence_ready)
    {

        Message msg = new Message();
        msg.obj = a7adath_geoFence_ready;
        mhandle.sendMessage(msg);
    }


    /***
     * A new Geo Fence HIT (either in or out) from a fence has been recieved.
     * @param a7adath_geoFence_hit
     */
    public void onEvent (final _7adath_GeoFence_Hit a7adath_geoFence_hit)
    {
        Message msg = new Message();
        msg.obj = a7adath_geoFence_hit;
        mhandle.sendMessage(msg);
    }


    public void onEvent (final _7adath_WayPointReached a7adath_wayPointReached)
    {

        final Message msg = mhandle.obtainMessage();
        msg.obj = a7adath_wayPointReached;
        if (mhandle != null)  mhandle.sendMessageDelayed(msg,0);


    }


    public void onEvent(final _7adath_WayPointsRecieved a7adath_wayPointsRecieved) {

        final Message msg = mhandle.obtainMessage();
        msg.obj = a7adath_wayPointsRecieved;
        if (mhandle != null) mhandle.sendMessageDelayed(msg, 0);


    }



    public void onEvent (final _7adath_HomeLocation_Ready a7adath_homeLocation_ready)
    {

        final Message msg = mhandle.obtainMessage();
        msg.obj = a7adath_homeLocation_ready;
        if (mhandle != null)  mhandle.sendMessageDelayed(msg,0);


    }

    public void onEvent (final _7adath_GPS_Ready a7adath_gps_ready)
    {

        final Message msg = mhandle.obtainMessage();
        msg.obj = a7adath_gps_ready;
        if (mhandle != null)  mhandle.sendMessageDelayed(msg,0);
    }


    /////////////////////////////////////////


    @Override
    public void init()
    {
        super.init();


        EventBus.getDefault().register(this);
        mhandle = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                if (msg.obj instanceof _7adath_GPS_Ready) {
                    // sent from TYPE_AndruavMessage_GPS Event
                    _7adath_GPS_Ready a7adath_gps_ready = (_7adath_GPS_Ready) (msg.obj);
                    final AndruavUnitBase andruavUnit = a7adath_gps_ready.mAndruavWe7da;

                    MarkerAndruav markerAndruav = markerPlans.get(andruavUnit.PartyID);
                    if ((markerAndruav == null) || (markerAndruav.marker == null)) {
                        // Object is not in the defined markers...
                        // either because :
                        // 1- ScheduledTasks has not run yet
                        // 2- No Message ID has been sent yet.
                        // 3- future reason: permissions and security related issues.
                        return;
                    }

                    final Marker mrkUser = markerAndruav.marker;
                    Location location = andruavUnit.getAvailableLocation();
                    if (location != null) {
                        LatLng lnglat = new LatLng(location.getLatitude(), location.getLongitude());

                        // 1- update mLocation
                        //if (andruavUnit.getAvailableLocation() != null) {
                        mrkUser.setPosition(lnglat);

                        if ((andruavUnit_selected != null) && (andruavUnit.Equals(andruavUnit_selected))) {
                            // this is the selected marker ... follow it.
                            followMarker(mrkUser, false);
                        }

                        //}
                        // 2- update heading
                        if (!andruavUnit.getIsCGS()) {
                            // GCS has no directions ... dont rotate them.
                            mrkUser.setRotation((float) andruavUnit.getActiveIMU().Y * Angles.RADIANS_TO_DEGREES);
                        }

                        Polyline polyline = linesPath.get(andruavUnit.PartyID);
                        if (polyline.getPoints().size() != 0) {
                            LatLng oldlnglat = polyline.getPoints().get(polyline.getPoints().size() - 1);
                            // DEMO
                            // LatLng newlng = new LatLng(oldlnglat.latitude + 0.001, oldlnglat.longitude + 0.001);
                            // END DEMO

                            double distance = GPSHelper.calculateDistance(lnglat.longitude, lnglat.latitude, oldlnglat.longitude, oldlnglat.latitude);
                            if ((location.getAccuracy() < distance)) {
                                //end if demo
                                List<LatLng> lstLatlong = polyline.getPoints();
                                lstLatlong.add(lnglat);
                                polyline.setPoints(lstLatlong);
                            }
                        } else {
                            if ((lnglat.latitude != 0) || (lnglat.longitude != 0)) {
                                List<LatLng> lstLatlong = polyline.getPoints();
                                lstLatlong.add(lnglat);
                                polyline.setPoints(lstLatlong);
                            }
                        }
                    }

                } else if (msg.obj instanceof _7adath_WayPointReached) {
                    _7adath_WayPointReached a7adath_wayPointReached = (_7adath_WayPointReached) msg.obj;
                    handleWayPointReached(a7adath_wayPointReached);

                } else if (msg.obj instanceof _7adath_HomeLocation_Ready) {
                    _7adath_HomeLocation_Ready a7adath_homeLocation_ready = (_7adath_HomeLocation_Ready) msg.obj;
                    handleHomeLocationUpdated(a7adath_homeLocation_ready);

                }
                else if (msg.obj instanceof _7adath_WayPointsRecieved) {
                    _7adath_WayPointsRecieved a7adath_wayPointsRecieved = (_7adath_WayPointsRecieved) msg.obj;
                    handleWayPointReceieved(a7adath_wayPointsRecieved);

                }
                else if (msg.obj instanceof _7adath_GeoFence_Ready) {
                // Draw Geo Fence
                final _7adath_GeoFence_Ready a7adath_geoFence_ready = (_7adath_GeoFence_Ready) msg.obj;
                showGeoFence (a7adath_geoFence_ready.fenceName,true);

                }
                else if (msg.obj instanceof _7adath_GeoFence_Hit)
                {

                }
            }
        };
    }

    @Override
    public void unInit() {
        super.unInit();

        EventBus.getDefault().unregister(this);

    }








}
