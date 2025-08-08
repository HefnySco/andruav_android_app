package ap.andruav_ap.activities.map;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Handler;
import androidx.core.app.ActivityCompat;
import androidx.collection.SimpleArrayMap;

import com.andruav.AndruavFacade;
import com.andruav.AndruavEngine;
import com.andruav.AndruavSettings;
import com.andruav.andruavUnit.AndruavUnitBase;
import com.andruav.andruavUnit.AndruavUnitShadow;
import com.andruav.event.droneReport_Event.Event_GeoFence_Ready;
import com.andruav.event.droneReport_Event.Event_HomeLocation_Ready;
import com.andruav.event.droneReport_Event.Event_WayPointReached;
import com.andruav.event.droneReport_Event.Event_WayPointsRecieved;
import com.andruav.sensors.AndruavIMU;
import com.andruav.controlBoard.shared.common.VehicleTypes;
import com.andruav.controlBoard.shared.geoFence.GeoFencePointNodeCylinder;
import com.andruav.controlBoard.shared.missions.MissionBase;
import com.andruav.controlBoard.shared.missions.MohemmaMapBase;
import com.andruav.controlBoard.shared.missions.WayPointStep;
import com.andruav.controlBoard.shared.geoFence.GeoCylinderFenceMapBase;
import com.andruav.controlBoard.shared.geoFence.GeoFencePoint;
import com.andruav.controlBoard.shared.geoFence.GeoFenceBase;
import com.andruav.controlBoard.shared.geoFence.GeoFenceManager;
import com.andruav.controlBoard.shared.geoFence.GeoLinearFenceCompositBase;
import com.andruav.controlBoard.shared.geoFence.GeoPolygonFenceCompositBase;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.HashMap;
import java.util.Map;

import ap.andruav_ap.App;
import ap.andruav_ap.R;
import ap.andruavmiddlelibrary.Voting;

/**
 * This is the MAP Control
 * Created by mhefny on 3/21/16.
 */
public class AndruavMapBaseWidget extends SupportMapFragment {

    public final MarkerPlans markerPlans = new MarkerPlans();

    public final SimpleArrayMap<String, Marker> markerWayPoints = new SimpleArrayMap<String, Marker>();
    public final Map<Marker, MarkerWaypoint> markerWayPointsHash = new HashMap<Marker, MarkerWaypoint>();
    public final SimpleArrayMap<String, Polyline> linesWayPoints = new SimpleArrayMap<String, Polyline>();
    public final SimpleArrayMap<String, Polyline> linesPath = new SimpleArrayMap<String, Polyline>();
    public final SimpleArrayMap<String, Polyline> geoLinearFence = new SimpleArrayMap<String, Polyline>();
    public final SimpleArrayMap<String, Polygon> geoPolygonFence = new SimpleArrayMap<String, Polygon>();
    public final SimpleArrayMap<String, Circle> geoCylinderFence = new SimpleArrayMap<String, Circle>();

    public AndruavUnitShadow andruavUnit_selected;
    public Marker targetMarker;
    protected Handler mhandle;
    protected GoogleMap mMap;

    /***
     * Map Zoom when selecting a marker.
     */
    private static final float defaultCloseZoom = 12.0f;
    private static final float defaultCloserZoom = 13.0f;
    //////////////////////////////////////// Vehicle Icons

    private IGoogleMapFeedback mIGoogleMapFeedback;

    private final int[] numDroneHomeMarkers =
            {
                    R.drawable.home_gr_24x24,
                    R.drawable.home_b_24x24,
                    R.drawable.home_pr_24x24,
                    R.drawable.home_y_24x24
            };


    private final int[] numCarDroneMarkers =
            {
                    R.drawable.car_1_32x32,
                    R.drawable.car_2_32x32,
                    R.drawable.car_3_32x32,
                    R.drawable.car_4_32x32
            };

    private final int[] numHeliDroneMarkers =
            {
                    R.drawable.heli_1_32x32,
                    R.drawable.heli_2_32x32,
                    R.drawable.heli_3_32x32,
                    R.drawable.heli_4_32x32
            };

    private final int[] numQuadDroneMarkers =
            {
                    R.drawable.drone_q1_32x32,
                    R.drawable.drone_q2_32x32,
                    R.drawable.drone_q3_32x32,
                    R.drawable.drone_q4_32x32
            };

    private final int[] numDroneMarkers =
            {
                    R.drawable.drone_1_32x32,
                    R.drawable.drone_2_32x32,
                    R.drawable.drone_3_32x32,
                    R.drawable.drone_4_32x32
            };

    private final int[] numGCSMarkers =
            {
                    R.drawable.map_gcs_1_32x32,
                    R.drawable.map_gcs_2_32x32,
                    R.drawable.map_gcs_3_32x32,
                    R.drawable.map_gcs_4_32x32
            };

    int i = 0, p = 0, q = 0, h = 0, c = 0;

    private void resetIconCounters() {
        i = 0;
        p = 0;
        q = 0;
        h = 0;
        c = 0;
    }

    public int getVehicleIcon(final AndruavUnitBase andruavUnit) {
        int iconResourceID;
        if (andruavUnit.getIsCGS()) {
            iconResourceID = numGCSMarkers[i % 4];
            i = i + 1;
        } else {
            switch (andruavUnit.getVehicleType()) {
                case VehicleTypes.VEHICLE_HELI:
                    iconResourceID = numHeliDroneMarkers[h % 4];
                    h = h + 1;
                    break;
                case VehicleTypes.VEHICLE_TRI:
                case VehicleTypes.VEHICLE_QUAD:
                    iconResourceID = numQuadDroneMarkers[q % 4];
                    q = q + 1;
                    break;
                case VehicleTypes.VEHICLE_ROVER:

                    iconResourceID = numCarDroneMarkers[c % 4];
                    c = c + 1;
                    break;
                case VehicleTypes.VEHICLE_PLANE:
                case VehicleTypes.VEHICLE_UNKNOWN:
                default:
                    iconResourceID = numDroneMarkers[p % 4];
                    p = p + 1;
                    break;

            }

        }
        return iconResourceID;
    }

    int homeIndex = 0;

    public int getHomeIcon() {

        final int iconResourceID = numDroneHomeMarkers[homeIndex % 4];
        homeIndex = homeIndex + 1;

        return iconResourceID;

    }
    /////////////////////////////////////////////////////////////

    public void setGoogleMapFeedback(IGoogleMapFeedback iGoogleMapFeedback)
    {
        this.mIGoogleMapFeedback = iGoogleMapFeedback;
    }
    public void setUp1() {
        MapsInitializer.initialize(App.getAppContext());
    }

    public void setupMap2() {
        // Make sure the map is initialized
        this.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                mMap = googleMap;
                // TODO it should wait for the map layout
                // before setting it up, instead of just
                // skipping the setup
                setupMapUI();
                setupMapOverlay();

                showGeoFence(true);

                if (mIGoogleMapFeedback != null)
                {
                    mIGoogleMapFeedback.onMapReady(mMap);
                }

            }

        });
    }


    private void setupMapOverlay() {

    }

    private void setupMapUI() {
        if (mMap == null) return;

        if (ActivityCompat.checkSelfPermission(App.context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(App.context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mMap.setMyLocationEnabled(true);
        final UiSettings mUiSettings = mMap.getUiSettings();
        mUiSettings.setMyLocationButtonEnabled(true);
        mUiSettings.setCompassEnabled(true);
        mUiSettings.setTiltGesturesEnabled(true);
        mUiSettings.setZoomControlsEnabled(true);
        mUiSettings.setRotateGesturesEnabled(true);
        mUiSettings.setMapToolbarEnabled(false);


    }

    public boolean isMapLayoutFinished() {
        return mMap != null;
    }

    public void init()
    {

    }

    public void unInit()
    {
        cleanMarkers();
        mhandle.removeCallbacksAndMessages(null);
        mhandle= null;
    }


    public void cleanMarkers()
    {
        if (mMap != null) {
            // fix a bug
            mMap.clear();
        }
        resetIconCounters();
        clearPlaneMarkers();
        clearWaypointsMarkers();
        clearGeoFence();
    }

    public void clearWaypointsMarkers()
    {

        // remove markers
        for (int i =0, limit = markerWayPoints.size(); i < limit;++i)
        {
            final Marker marker = markerWayPoints.valueAt(i);
            marker.remove();
        }
        markerWayPoints.clear();
        markerWayPointsHash.clear();

        // remove polylines
        for (int i =0, limit = linesWayPoints.size(); i < limit;++i)
        {
            final Polyline polyline= linesWayPoints.valueAt(i);
            polyline.remove();
        }

        linesWayPoints.clear();

    }

    public void clearPlaneMarkers()
    {

        // remove markers
        for (int i =0, limit = markerPlans.size(); i < limit;++i)
        {
            final MarkerAndruav marker = markerPlans.valueAt(i);
            marker.marker.remove();
            final  Circle c = marker.circle;
            if (c!= null) c.remove();
        }

        markerPlans.clear();



        for (int i =0, limit = linesPath.size(); i < limit;++i)
        {
            final  Polyline polyline = linesPath.valueAt(i);
            polyline.remove();
        }
        linesPath.clear();
    }

    public void clearGeoFence()
    {
        for (int i =0, limit = geoLinearFence.size(); i < limit;++i)
        {
            final  Polyline polyline = geoLinearFence.valueAt(i);
            polyline.remove();
        }
        geoLinearFence.clear();

        for (int i =0, limit = geoPolygonFence.size(); i < limit;++i)
        {
            final com.google.android.gms.maps.model.Polygon polygon = geoPolygonFence.valueAt(i);
            polygon.remove();
        }
        geoPolygonFence.clear();


        for (int i =0, limit = geoCylinderFence.size(); i < limit;++i)
        {
            final  Circle circle= geoCylinderFence.valueAt(i);
            circle.remove();
        }
        geoCylinderFence.clear();
    }


    public String getKeybyMarker (Marker marker)
    {
        int s = markerPlans.size();
        for (int i = 0; i < s; ++i) {

            if (markerPlans.valueAt(i).marker.equals(marker)) {
                return  markerPlans.keyAt(i);

            }
        }
        return null;
    }

    /***
     * Zoom map to display only a given waypoint
     * @param markerWaypoint
     */
    public void pantoMission (final SimpleArrayMap<String,Marker>  markerWaypoint)
    {
        if ((mMap == null) || (markerWayPoints.size()==0))  return ;

        final LatLngBounds.Builder b = new LatLngBounds.Builder();
        for (int i =0, limit = markerWaypoint.size(); i < limit;++i)
        {
            final Marker marker = markerWaypoint.valueAt(i);
            b.include(marker.getPosition());
        }

        LatLngBounds bounds = b.build();
        //Change the padding as per needed
        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds,100);
        mMap.animateCamera(cu);
    }


    public void pantoHome (final AndruavUnitShadow andruavWe7da, final boolean zoom)
    {
        if ((mMap == null) || (andruavWe7da == null))return ;

        if (andruavWe7da.hasHomeLocation())
            return;

        //marker.showInfoWindow();
        float camzoon = mMap.getCameraPosition().zoom;

        if (zoom) {
            camzoon = camzoon > defaultCloserZoom ? camzoon : defaultCloserZoom ;
        }

        final MohemmaMapBase mohemmaMapBase = andruavWe7da.getMohemmaMapBase();

        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(andruavWe7da.getGpsHomeLocation().getLatitude(), andruavWe7da.getGpsHomeLocation().getLongitude()), camzoon));

    }


    public void addNewUnitMarker(final Location loc, final AndruavUnitShadow andruavWe7da) {

        if (mMap == null) return ;

        Marker mrkUser;
        final int iconResourceID = getVehicleIcon(andruavWe7da);

        LatLng latLng =new LatLng(loc.getLatitude(), loc.getLongitude());
        mrkUser = mMap.addMarker(new MarkerOptions()
                .draggable(false)
                .rotation((float)andruavWe7da.getActiveIMU().Y)
                .position(latLng)
                .icon(BitmapDescriptorFactory.fromResource(iconResourceID))
                .title(andruavWe7da.UnitID + " (" + andruavWe7da.Description + ")"));


        mrkUser.setAnchor(0.5f,0.5f);
        markerPlans.put(andruavWe7da.PartyID, mrkUser);
        AndruavEngine.notification().Speak(andruavWe7da.UnitID + " " + getString(ap.andruavmiddlelibrary.R.string.action_drone_added));
        if (AndruavSettings.andruavWe7daBase.getIsCGS())
        {
            Voting.onMapHasObjectsRecieved();
        }

        // Add Polyline object used for Path
        PolylineOptions polylineOptions =new PolylineOptions();
        // polylineOptions.add(latLng);
        Polyline polyline = mMap.addPolyline(polylineOptions.width(3).color(getResources().getColor(R.color.btn_TX_HANDLER)).geodesic(true));
        linesPath.put(andruavWe7da.PartyID, polyline);


        if (!andruavWe7da.getIsCGS()  && andruavWe7da.IsMe())
        {
            showWayPoints(andruavWe7da);
            showHomeLocation(andruavWe7da);
        }


    }


    /***
     * Move camera to marker , and zoom to {@link #defaultCloseZoom} only if the current zoom is less than it. i.e. never zoom out.
     * @param marker
     */
    public void followMarker (final Marker marker, final boolean zoom) {

        if (marker == null) return ;

        //marker.showInfoWindow();
        float camzoon = mMap.getCameraPosition().zoom;

        if (zoom) {
            camzoon = camzoon > defaultCloseZoom ? camzoon : defaultCloseZoom;
        }
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(marker.getPosition().latitude, marker.getPosition().longitude), camzoon));

    }


    public void showHomeLocation(AndruavUnitBase andruavWe7da)
    {
        final AndruavIMU andruavIMU = andruavWe7da.getActiveIMU();
        if ((andruavIMU==null) || (!andruavWe7da.hasHomeLocation()))
        return;

        LatLng lnglat = new LatLng(andruavWe7da.getGpsHomeLocation().getLatitude(), andruavWe7da.getGpsHomeLocation().getLongitude());

        Marker marker = mMap.addMarker(new MarkerOptions()
                .draggable(false)
                .rotation((float) andruavWe7da.getActiveIMU().Y)
                .position(lnglat)
                //   .icon(BitmapDescriptorFactory.fromResource(R.drawable.location_bb_32x32))
                .title(String.valueOf(i))
                .snippet(String.valueOf(i)));

        int homeIndex;
        if (andruavWe7da.homeIconIndex==-1)
        {
            andruavWe7da.homeIconIndex = getHomeIcon();
        }

        homeIndex = andruavWe7da.homeIconIndex;

        marker.setIcon(BitmapDescriptorFactory.fromResource(homeIndex));

        marker.setAnchor(0.5f, 0.5f);

    }


    public void showWayPoints(AndruavUnitBase andruavUnit)
    {
        pv_showWayPoints(andruavUnit);

    }


    private void pv_showWayPoints(AndruavUnitBase andruavWe7da)
    {
        try {

            final MohemmaMapBase mohemmaMapBase = andruavWe7da.getMohemmaMapBase();
            if (mohemmaMapBase == null) return;

            int size = mohemmaMapBase.size();

            clearWaypointsMarkers();
            PolylineOptions polylineOptions = new PolylineOptions();
            for (int i = 0; i < size; i = i + 1) {

                MissionBase missionBase = mohemmaMapBase.valueAt(i);
                if (missionBase instanceof WayPointStep)
                {

                    WayPointStep wayPointStep = (WayPointStep) missionBase;
                    LatLng lnglat = new LatLng(wayPointStep.Latitude, wayPointStep.Longitude);
                    MarkerWaypoint markerWaypoint = new MarkerWaypoint();
                    markerWaypoint.order = i;
                    markerWaypoint.andruavWe7da = andruavWe7da;
                    markerWaypoint.wayPointStep = wayPointStep;
                    Marker marker = mMap.addMarker(new MarkerOptions()
                            .draggable(false)
                            .rotation((float) andruavWe7da.getActiveIMU().Y)
                            .position(lnglat)
                            //   .icon(BitmapDescriptorFactory.fromResource(R.drawable.location_bb_32x32))
                            .title(String.valueOf(i))
                            .snippet(String.valueOf(i)));

                    int icon ;
                    int speak=0;
                    switch (wayPointStep.Status)
                    {
                        case MissionBase.Report_NAV_ItemExecuting:
                            icon = R.drawable.location_bg_32x32;

                            break;
                        case MissionBase.Report_NAV_ItemReached:
                            icon = R.drawable.location_gy_32x32;
                            break;
                        case MissionBase.Report_NAV_Unknown:
                        default:
                            icon = R.drawable.location_bb_32x32;
                            break;
                    }

                    marker.setIcon(BitmapDescriptorFactory.fromResource(icon));
                    marker.setAnchor(0.5f, 0.5f);

                    markerWayPoints.put(String.valueOf(i), marker);
                    markerWayPointsHash.put(marker, markerWaypoint);



           /* PolygonOptions pathOptions = new PolygonOptions()
                    .strokeColor(PATH_DEFAULT_COLOR)
                    .strokeWidth(PATH_DEFAULT_WIDTH)
                    .fillColor(PATH_FILL_COLOR);
                    .fillColor(PATH_FILL_COLOR);
            pathOptions.add(lnglat);
            */
                    polylineOptions.add(lnglat);

                    // mMap.addPolygon(pathOptions);
                }

             }
            pantoMission(markerWayPoints);

            Polyline polyline = mMap.addPolyline(polylineOptions
                    .width(3).color(getResources().getColor(R.color.btn_TXT_BLUE_DARKEST)).geodesic(true));
            linesWayPoints.put(andruavWe7da.PartyID, polyline);
        }
        catch (Exception e)
        {
            AndruavEngine.log().logException("maps_waypoints", e);
        }
    }




    protected void handleWayPointReached (final Event_WayPointReached a7adath_wayPointReached)
    {
        if ((andruavUnit_selected != null) && (andruavUnit_selected.Equals(a7adath_wayPointReached.mAndruavWe7da))) {
            // this is the current selected one.


            final MohemmaMapBase mohemmaMapBase = a7adath_wayPointReached.mAndruavWe7da.getMohemmaMapBase();
            if ((mohemmaMapBase == null)
            || (mohemmaMapBase.size() <= a7adath_wayPointReached.mWaypointIndex)) {
                AndruavFacade.requestWayPoints(a7adath_wayPointReached.mAndruavWe7da);
                return;
            }

            final MissionBase missionBase = mohemmaMapBase.valueAt(a7adath_wayPointReached.mWaypointIndex);
            if ( missionBase == null) return;
            int speak=0;
            if (missionBase instanceof WayPointStep)
            {

                WayPointStep wayPointStep = (WayPointStep) missionBase;
                switch (wayPointStep.Status) {
                    case MissionBase.Report_NAV_ItemExecuting:
                        speak = ap.andruavmiddlelibrary.R.string.action_waypoint_reached_going;

                        break;
                    case MissionBase.Report_NAV_ItemReached:
                        speak = ap.andruavmiddlelibrary.R.string.action_waypoint_reached_charging;
                        break;
                    case MissionBase.Report_NAV_Unknown:
                    default:
                        break;
                }

            }


            if (speak !=0) {
                AndruavEngine.notification().Speak(getString(speak) + a7adath_wayPointReached.mWaypointIndex);
            }

            showWayPoints(a7adath_wayPointReached.mAndruavWe7da);


        }
    }


    protected void handleWayPointReceieved (final Event_WayPointsRecieved a7adath_wayPointsRecieved)
    {
        if ((andruavUnit_selected != null) && (andruavUnit_selected.Equals(a7adath_wayPointsRecieved.mAndruavWe7da))) {
            // this is the current selected one.
            showWayPoints(a7adath_wayPointsRecieved.mAndruavWe7da);
            AndruavEngine.notification().Speak(getString(ap.andruavmiddlelibrary.R.string.action_waypoint_waypoints_received));
        }
    }



    protected void handleHomeLocationUpdated (final Event_HomeLocation_Ready a7adath_homeLocation_ready)
    {
        if ((andruavUnit_selected != null) && (andruavUnit_selected.Equals(a7adath_homeLocation_ready.mAndruavWe7da))) {
            // this is the current selected one.
            //showWayPoints((AndruavWe7da) a7adath_homeLocation_ready.mAndruavWe7da);

            showHomeLocation(a7adath_homeLocation_ready.mAndruavWe7da);
        }
    }

    /***
     * same fence can be attached to different drones, so by default @param forceUpdate is false.
     * <br> it is true when calling this function from a a {@link Event_GeoFence_Ready}
     * @param forceUpdate
     */
    protected void showGeoFence (final boolean forceUpdate)
    {
        final int size = GeoFenceManager.size();

        for (int i = 0; i < size; ++i) {
            final GeoFenceBase geoFenceBase = GeoFenceManager.valueAt(i);
            showGeoFence(geoFenceBase, forceUpdate);
        }
    }

    protected void showGeoFence(final String fenceName, final boolean forceUpdate) {

        final GeoFenceBase geoFenceBase = GeoFenceManager.get(fenceName);

        showGeoFence (geoFenceBase,forceUpdate);
    }


    /***
     *
     * @param geoFenceBase
     * @param forceUpdate  delete old fence even if exists... as geo fences are common between andruavUnits
     */
    protected void showGeoFence(final GeoFenceBase geoFenceBase, final boolean forceUpdate)
    {

        if (geoFenceBase.hardFenceAction == GeoFenceBase.ACTION_SHUT_DOWN)
        {
            // dont display these fences
            return ;
        }

        if (geoFenceBase instanceof GeoLinearFenceCompositBase) {

            final Polyline polyline = geoLinearFence.get(geoFenceBase.fenceName);
            if (polyline!= null)
            {
                if (forceUpdate) {
                    polyline.remove();
                }
                else
                {
                    return;
                }
            }
            showGeoLinearFence ((GeoLinearFenceCompositBase)geoFenceBase);
        }
        else
        if (geoFenceBase instanceof GeoPolygonFenceCompositBase) {

            final com.google.android.gms.maps.model.Polygon polygon = geoPolygonFence.get(geoFenceBase.fenceName);
            if (polygon!= null)
            {
                if (forceUpdate) {
                    polygon.remove();
                }
                else
                {
                    return;
                }
            }

            showGeoPolyFence ((GeoPolygonFenceCompositBase)geoFenceBase);
        }
        else
        if (geoFenceBase instanceof GeoCylinderFenceMapBase) {
            final Circle  circle = geoCylinderFence.get(geoFenceBase.fenceName);

            if (circle!= null)
            {
                if (forceUpdate) {
                    circle.remove();
                }
                else
                {
                    return;
                }
            }
            showGeoCylinderFence ((GeoCylinderFenceMapBase)geoFenceBase);
        }
    }

    private void showGeoCylinderFence (final GeoCylinderFenceMapBase geoCylinderFenceMapBase)
    {
        final GeoFencePointNodeCylinder geoFenceNodeCylinder = geoCylinderFenceMapBase.getGeoFence();
        LatLng latLng = new LatLng(geoFenceNodeCylinder.Latitude, geoFenceNodeCylinder.Longitude);

        int fillColor; //, strockColor;

        if (geoCylinderFenceMapBase.shouldKeepOutside)
        {
            fillColor = getResources().getColor(R.color.COLOR_UNSAFE_FENCE);
            //strockColor = getResources().getColor(R.color.btn_TXT_WARNING);
        }
        else
        {
            fillColor = getResources().getColor(R.color.COLOR_SAFE_FENCE);
            //strockColor = getResources().getColor(R.color.btn_COLOR_WHITE);
        }

         final Circle circle = mMap.addCircle(new CircleOptions()
                .center(new LatLng(geoFenceNodeCylinder.Latitude, geoFenceNodeCylinder.Longitude))
                .radius(geoCylinderFenceMapBase.maxDistance)
                .strokeColor(Color.TRANSPARENT)
                .fillColor(fillColor)
                .strokeWidth(1.0f));

        geoCylinderFence.put(geoCylinderFenceMapBase.fenceName, circle);
    }

    private void showGeoLinearFence(final GeoLinearFenceCompositBase geoLinearFenceMapBase)
    {
        final PolylineOptions polylineOptions = new PolylineOptions();

        final int size2 = geoLinearFenceMapBase.size();

        int  strockColor;

        if (geoLinearFenceMapBase.shouldKeepOutside)
        {
            strockColor = getResources().getColor(R.color.COLOR_UNSAFE_FENCE);
        }
        else
        {
            strockColor = getResources().getColor(R.color.COLOR_SAFE_FENCE);
        }


        for (int j = 0; j < size2; ++j) {

            final GeoFencePoint geoFencePoint = geoLinearFenceMapBase.valueAt(j);
            //  final Geo geoLinearFenceMapBase.valueAt(j);

            final LatLng lnglat = new LatLng(geoFencePoint.Latitude, geoFencePoint.Longitude);
            polylineOptions.add(lnglat);
        }

        final Polyline polyline2 = mMap.addPolyline(polylineOptions.width(4).color(strockColor).geodesic(true));


        geoLinearFence.put(geoLinearFenceMapBase.fenceName, polyline2);
    }

    private void showGeoPolyFence(final GeoPolygonFenceCompositBase geoPolygonFenceMapBase)
    {
        final PolygonOptions polygonOptions = new PolygonOptions();
        int fillColor; //, strockColor;

        if (geoPolygonFenceMapBase.shouldKeepOutside)
        {
            fillColor = getResources().getColor(R.color.COLOR_UNSAFE_FENCE);
           // strockColor = getResources().getColor(R.color.btn_TXT_WARNING);
        }
        else
        {
            fillColor = getResources().getColor(R.color.COLOR_SAFE_FENCE);
          //  strockColor = getResources().getColor(R.color.btn_COLOR_WHITE);
        }
     //   polygonOptions.clickable(false).strokeColor(strockColor).fillColor(fillColor).strokeWidth(4);
        polygonOptions.clickable(false).strokeColor(Color.TRANSPARENT).fillColor(fillColor).strokeWidth(4);

        final int size2 = geoPolygonFenceMapBase.size();

        if (size2 < 3) return ; // bad Poly

        for (int j = 0; j < size2; ++j) {

            final GeoFencePoint geoFencePoint = geoPolygonFenceMapBase.valueAt(j);
            //  final Geo geoLinearFenceMapBase.valueAt(j);

            final LatLng lnglat = new LatLng(geoFencePoint.Latitude, geoFencePoint.Longitude);
            polygonOptions.add(lnglat);
        }

        final com.google.android.gms.maps.model.Polygon polygon = mMap.addPolygon(polygonOptions);


        geoPolygonFence.put(geoPolygonFenceMapBase.fenceName, polygon);
    }

    public interface IGoogleMapFeedback
    {
        void onMapReady ( GoogleMap map);
    }
}
