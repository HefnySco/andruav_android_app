package ap.andruavmiddlelibrary.sensors;


import android.location.Location;
import android.location.LocationManager;

/**
 * Created by M.Hefny on 06-Sep-14.
 */
public class GenericLocationSensor extends GenericSensor {

    /////// Attributes
    public Location currentLocation;
    protected LocationManager mLocationManager;

    ///////////EOF Attributes

    private GenericLocationSensor ()
    {

    }

    public GenericLocationSensor (LocationManager locationManager)
    {
        mLocationManager = locationManager;
    }

    @Override
    public boolean isSupported () throws NoSuchMethodException
    {
        throw new NoSuchMethodException();

    }


}
