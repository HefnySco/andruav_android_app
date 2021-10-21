package rcmobile.andruavmiddlelibrary.factory.math;

/**
 * Created by M.Hefny on 18-Oct-14.
 */
public class UnitConversion {


    public static final float MetersToFeet = 3.28084f;
    public static final float FeetToMeters = 0.3048f;
    public static final float MetersToMile = 0.000621371f;
    public static final float Speed_MetersPerSecondToMilePerHour = 2.2369356f;
    public static final float Speed_MetersPerSecondToKMeterPerHour = 3.6f;


    public static double convertfromCelsiustoFahrenheit (double celsius)
    {
        return celsius * ( 9/5) + 32;
    }

    public static double convertfromFahrenheittoCelsius (double fahrenheit)
    {
        return (fahrenheit - 32) * 5/9;
    }


    public static double convertfromMetertoFeet (double aux3)
    {
        return aux3 * 3.2808399;
    }


    public static double convertfromFeettoMeter (double feet)
    {
        return feet * 0.304799999536704;
    }

    public static double convertfromKMtoMile (double now)
    {
        return now * 0.621371192;
    }


    public static double convertfromMiletoKM (double scaledBmp)
    {
        return scaledBmp * 1.609344000614692;
    }

}
