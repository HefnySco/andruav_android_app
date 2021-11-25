package ap.andruav_ap.activities.map;

import androidx.collection.SimpleArrayMap;

import com.andruav.andruavUnit.AndruavUnitBase;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.Marker;


/**
 * Created by mhefny on 10/27/15.
 */
public class MarkerPlans extends SimpleArrayMap<String,MarkerAndruav>
{


    public MarkerAndruav put (String key, Marker marker)
    {
        MarkerAndruav markerAndruav;
        markerAndruav = get(key);
        if (markerAndruav == null) {
            markerAndruav = new MarkerAndruav();
        }
        markerAndruav.marker = marker;

        return super.put(key, markerAndruav);
    }


    public MarkerAndruav put (String key, Circle c)
    {
        MarkerAndruav markerAndruav;
        markerAndruav = get(key);
        if (markerAndruav == null) {
            markerAndruav = new MarkerAndruav();
        }
        markerAndruav.circle = c;

        return super.put(key, markerAndruav);
    }

    /***
     * returns market stored in {@link AndruavUnitBase}
     * <br> used to avoid testing null return of {@link MarkerAndruav} each time you need a marker.
     * @param key
     * @return marker object ot null.
     */
    public Marker getMarker (String key)
    {
        MarkerAndruav markerAndruav = super.get(key);
        if (markerAndruav ==null) return null;

        return markerAndruav.marker;
    }

}
