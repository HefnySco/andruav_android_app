package ap.andruavmiddlelibrary.sensors;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * Created by M.Hefny on 31-Aug-14.
 */
public class Sensor_Mag extends GenericIMUSensor implements SensorEventListener{

    /////// Attributes
    /**
     * Smoothing factor for Mag
     */
    public float alpha = 0.5f;
    public long LastTimestamp;



    ///////////EOF Attributes

    public Sensor_Mag(SensorManager sensorManager)
    {
        super(sensorManager);
        mSensor =  msensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

    }



    @Override
    public void onSensorChanged(SensorEvent event) {


        if (misCalibrated == false)
        {
            calibrateSensor(event.values);

            return ;
        }

        rawValues[0] = event.values[0];
        rawValues[1] = event.values[1];
        rawValues[2] = event.values[2];


        smoothedValues[0] = alpha * smoothedValues[0] - (1 - alpha) * (rawValues[0] - calibrationValues[0]);
        smoothedValues[1] = alpha * smoothedValues[1] + (1 - alpha) * (rawValues[1] - calibrationValues[1]);
        smoothedValues[2] = alpha * smoothedValues[2] + (1 - alpha) * (rawValues[2] - calibrationValues[2] + 9.8f);

        LastTimestamp = event.timestamp;
    }

    /**
     * Calculates Zero Gyro values.
     *
     * @values raw values from sensor
     */
    protected void calibrateSensor (float[] values)
    {
        mcounterCalibrated -=1;
        calibrationValues[0] +=  values[0] ;
        calibrationValues[1] +=  values[1] ;
        calibrationValues[2] +=  values[2] ;

        if (mcounterCalibrated ==0)
        {
            misCalibrated = true;
            calibrationValues[0] /= mcalibrationCount;
            calibrationValues[1] /= mcalibrationCount;
            calibrationValues[2] /= mcalibrationCount;
        }
    }
}
