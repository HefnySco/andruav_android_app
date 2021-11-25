package ap.andruav_ap.activities.map;

import com.andruav.util.AndruavLatLng;

import java.util.List;

/**
 * Created by mhefny on 4/3/16.
 */
public interface IPathHandler {

    /***
     * Add path to current path without replace.
     * @param path
     */
    void addPath(List<AndruavLatLng> path);

    /***
     * Replace current path with this new path
     * @param path
     */
    void putPath(List<AndruavLatLng> path);


    void clearPath();
}
