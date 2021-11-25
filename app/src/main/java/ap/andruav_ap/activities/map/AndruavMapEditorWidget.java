package ap.andruav_ap.activities.map;

import android.location.Location;
import android.os.Handler;
import android.os.Message;
import androidx.collection.SimpleArrayMap;

import com.andruav.andruavUnit.AndruavUnitBase;
import com.andruav.event.droneReport_7adath._7adath_GPS_Ready;
import com.andruav.event.droneReport_7adath._7adath_GeoFence_Hit;
import com.andruav.event.droneReport_7adath._7adath_GeoFence_Ready;
import com.andruav.event.droneReport_7adath._7adath_HomeLocation_Ready;
import com.andruav.event.droneReport_7adath._7adath_WayPointReached;
import com.andruav.event.droneReport_7adath._7adath_WayPointsRecieved;
import com.andruav.controlBoard.shared.missions.MissionBase;
import com.andruav.controlBoard.shared.missions.MohemmaMapBase;
import com.andruav.controlBoard.shared.missions.WayPointStep;
import com.andruav.util.AndruavLatLng;
import com.andruav.util.GPSHelper;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.greenrobot.event.EventBus;
import ap.andruav_ap.R;
import ap.andruavmiddlelibrary.factory.math.Angles;

/**
 * Created by mhefny on 3/20/16.
 */
public class AndruavMapEditorWidget extends AndruavMapBaseWidget implements IPathHandler{

    private AndruavMapEditorWidget Me;

    private IMarker iMarker;
    private final SimpleArrayMap<String, Marker> markerMissionPoints = new SimpleArrayMap<String, Marker>();
    private final Map<Marker, MarkerWaypoint> markerMissionHash = new HashMap<Marker, MarkerWaypoint>();
    private final SimpleArrayMap<String, Polyline> linesMission = new SimpleArrayMap<String, Polyline>();
    private int wayPointIndex = 1;

    private final double default_alt = 20; //meters

    private Polyline polylineMission;

    private boolean createNewMarkers;


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


    public void onEvent(final _7adath_WayPointReached a7adath_wayPointReached) {

        final Message msg = mhandle.obtainMessage();
        msg.obj = a7adath_wayPointReached;
        if (mhandle != null) mhandle.sendMessageDelayed(msg, 0);


    }

    public void onEvent(final _7adath_WayPointsRecieved a7adath_wayPointsRecieved) {

        final Message msg = mhandle.obtainMessage();
        msg.obj = a7adath_wayPointsRecieved;
        if (mhandle != null) mhandle.sendMessageDelayed(msg, 0);


    }


    public void onEvent(final _7adath_GPS_Ready a7adath_gps_ready) {

        final Message msg = mhandle.obtainMessage();
        msg.obj = a7adath_gps_ready;
        if (mhandle != null) mhandle.sendMessageDelayed(msg, 0);

    }

    public void onEvent(final _7adath_HomeLocation_Ready a7adath_homeLocation_ready) {

        final Message msg = mhandle.obtainMessage();

        msg.obj = a7adath_homeLocation_ready;

        if (mhandle != null) mhandle.sendMessageDelayed(msg, 0);
    }


    @Override
    public void init() {

        Me = this;
        wayPointIndex = 0;

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

                } else if (msg.obj instanceof _7adath_WayPointsRecieved) {
                    _7adath_WayPointsRecieved a7adath_wayPointsRecieved = (_7adath_WayPointsRecieved) msg.obj;
                    handleWayPointReceieved(a7adath_wayPointsRecieved);

                } else if (msg.obj instanceof _7adath_GeoFence_Ready) {
                    // Draw Geo Fence
                    final _7adath_GeoFence_Ready a7adath_geoFence_ready = (_7adath_GeoFence_Ready) msg.obj;
                    showGeoFence (a7adath_geoFence_ready.fenceName,true);

                } else if (msg.obj instanceof _7adath_GeoFence_Hit)
                {

                }
            }


        };
    }


    @Override
    public void setupMap2()
    {
        super.setupMap2();
        setupMapListeners();

    }




    public void setMarkerCallback (IMarker iMarker)
    {
        this.iMarker = iMarker;
    }


    public MohemmaMapBase getMission ()
    {
        final MohemmaMapBase mohemmaMapBase = new MohemmaMapBase();

        int i=0;

        for (MarkerWaypoint item:markerMissionHash.values()
             ) {
            mohemmaMapBase.put(String.valueOf(i),item.wayPointStep);
            i = i +1;
        }

        return mohemmaMapBase;
    }

    public void setCreateNewMarkers (final boolean enable)
    {
        createNewMarkers = enable;
    }

    private void setupMapListeners()
    {
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                if (Me.createNewMarkers) {
                    addSingleWaypoint(latLng);
                }

                iMarker.onMapClick(latLng);

            }
        });


        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            /***
             * true if the listener has consumed the event (i.e., the default behavior should not occur), false otherwise (i.e., the default behavior should occur).
             * The default behavior is for the camera to move to the map and an info window to appear.
             * @param marker
             * @return
             */

            Marker mmarker;
            @Override
            public boolean onMarkerClick(final Marker marker) {
                mmarker=marker;
                mhandle.postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        // test if it is a waypoint
                        if (mmarker!=null) {
                            if (iMarker != null)
                            {
                                iMarker.onClick(mmarker,markerMissionHash.get(marker));
                            }
                        }


                        return ;
                    }
                },100);


                return true;
            }


        });

        mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {

            }

            @Override
            public void onMarkerDrag(Marker marker) {

            }

            @Override
            public void onMarkerDragEnd(Marker marker) {

                showPolyLinesMission();


            }
        });
    }

    private void addSingleWaypoint(LatLng latLng) {
        Marker marker = mMap.addMarker(new MarkerOptions()
                .draggable(true)
                .position(latLng)
                .title(String.valueOf(wayPointIndex))
                .snippet(String.valueOf(wayPointIndex)));

        final WayPointStep wayPointStep = new WayPointStep();
        wayPointStep.Altitude    = default_alt;
        wayPointStep.Longitude   = marker.getPosition().longitude;
        wayPointStep.Latitude    = marker.getPosition().latitude;
        wayPointStep.TimeToStay  = 0;
        wayPointStep.Heading     = 0;
        wayPointStep.Status     = MissionBase.Report_NAV_Unknown;

        final MarkerWaypoint markerWaypoint = new MarkerWaypoint();
        markerWaypoint.order = wayPointIndex;
        markerWaypoint.wayPointStep = wayPointStep;
        markerWaypoint.wayPointStep.Sequence    = wayPointIndex;

        markerMissionPoints.put(String.valueOf(wayPointIndex),marker);
        markerMissionHash.put(marker,markerWaypoint);

        wayPointIndex = wayPointIndex + 1;

        showPolyLinesMission();
    }

    @Override
    public void unInit() {
        super.unInit();

        EventBus.getDefault().unregister(this);
        clearWaypointsMarkers();
    }


    @Override
    public void clearWaypointsMarkers() {
        super.clearWaypointsMarkers();

        cleanMissionMarkers();
    }

    private void cleanMissionMarkers() {

        for (int i =0, limit = markerMissionPoints.size(); i < limit;++i)
        {
            Marker marker = markerMissionPoints.valueAt(i);
            marker.remove();
        }
        markerMissionPoints.clear();
        markerMissionHash.clear();
        linesMission.clear();

        if (polylineMission != null) {
            polylineMission.remove();
            polylineMission = null;
        }

        wayPointIndex = 1;
    }


    private void addPathToMission (List<AndruavLatLng> path)
    {
        if (path==null) return ;
        for (int i=0, s = path.size(); i< s; ++i)
        {
            final AndruavLatLng andruavLatLng = path.get(i);
            addSingleWaypoint (new LatLng(andruavLatLng.getLatitude(),andruavLatLng.getLongitude()));
        }

    }

    private void showPolyLinesMission ()
    {

        if (polylineMission != null) {
            polylineMission.remove();
        }


        final PolylineOptions polylineMissionOptions = new PolylineOptions();

        for (int i =0, limit = markerMissionPoints.size(); i < limit; i=i+1)
        {
            Marker marker = markerMissionPoints.valueAt(i);
            polylineMissionOptions.add(marker.getPosition());
        }

        polylineMission = mMap.addPolyline(polylineMissionOptions.width(3).color(getResources().getColor(R.color.btn_TX_HANDLER)).geodesic(true));

    }

    @Override
    public void addPath(List<AndruavLatLng> path) {

        addPathToMission(path);

    }

    @Override
    public void putPath(List<AndruavLatLng> path) {

        cleanMissionMarkers();
        addPathToMission(path);

    }

    @Override
    public  void clearPath() {
        cleanMissionMarkers();
    }
}