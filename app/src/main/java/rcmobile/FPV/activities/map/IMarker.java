package rcmobile.FPV.activities.map;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

/**
 * Created by mhefny on 4/5/16.
 */
public interface IMarker {

    void onClick (Marker marker, MarkerWaypoint markerWaypoint);
    void onMapClick (LatLng latLng);
}
