package rcmobile.andruavmiddlelibrary.sensors;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * Created by M.Hefny on 06-Sep-14.
 */
class GenericIMUSensor extends GenericSensor  implements SensorEventListener {

    /////// Attributes

    protected Sensor mSensor;
    protected SensorManager msensorManager;

    // Calibration Variables
    public double[] calibrationValues = new double[3];
    protected static final int mcalibrationCount = 100;
    protected boolean misCalibrated = true;
    protected int mcounterCalibrated = mcalibrationCount;

    ///////////EOF Attributes

    private GenericIMUSensor()
    {

    }

    public GenericIMUSensor (SensorManager sensorManager)
    {
        msensorManager = sensorManager;
    }

    @Override
    public boolean isSupported ()
    {
        return (mSensor != null);
    }

    public void calibrate ()
    {
        mcounterCalibrated = mcalibrationCount;
        calibrationValues[0] = 0.0f;
        calibrationValues[1] = 0.0f;
        calibrationValues[2] = 0.0f;
        misCalibrated = false;

        return ;
    }


    public boolean isCalibrated ()
    {
        return misCalibrated;
    }


    @Override
    public void registerSensor()
    {
        if ((mregisteredSensor == true) || (mSensor == null)) return ;
        msensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_GAME);
        mregisteredSensor = true;
    }

    @Override
    public void unregisterSensor()
    {
        if (mregisteredSensor == false)  return ;
        msensorManager.unregisterListener(this, mSensor);
        mregisteredSensor = false;
        mSensor = null;
    }

    @Override
    public void onAccuracyChanged(Sensor arg0, int arg1) {
        // ToDo: Override and implement in child class
    }

    @Override
    public void onSensorChanged(SensorEvent event) {



    }

}
