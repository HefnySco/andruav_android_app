package rcmobile.andruavmiddlelibrary.sensors;

/**
 * Created by M.Hefny on 04-Sep-14.
 */
public class GenericSensor {

    /////// Attributes
    public float[] rawValues = new float[3];
    public double[] smoothedValues = new double[3];
    protected  boolean mregisteredSensor = false;


    ///////////EOF Attributes

    public GenericSensor ()
    {

    }

    public boolean isSupported () throws NoSuchMethodException
    {
        throw new NoSuchMethodException();

    }


    public void registerSensor() throws NoSuchMethodException
    {
        throw new NoSuchMethodException();
    }

    public void unregisterSensor() throws NoSuchMethodException
    {
        throw new NoSuchMethodException();
    }
}
