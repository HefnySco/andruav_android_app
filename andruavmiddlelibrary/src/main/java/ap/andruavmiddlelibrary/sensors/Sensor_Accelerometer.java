package ap.andruavmiddlelibrary.sensors;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import ap.andruavmiddlelibrary.factory.math.Vector3d;


/**
 * Created by M.Hefny on 31-Aug-14.
 */
public class Sensor_Accelerometer extends GenericIMUSensor implements SensorEventListener{

    /////// Attributes
    // Roll - Pitch - Z : index 0,1,2
    // Roll is negavtive to gyro
    /**
     * Smoothing factor for Acc...represents gravity
     */
    public final double alpha = 0.8f;
    public final double[] linear_acceleration = new double[3];
    public final double[] linear_velocity = new double[3];
    public double[] tiltValues = new double[3];


    public final Vector3d vAcc = new Vector3d(0.0,0.0,1.0);
    public Boolean misTilted;
    public long LastTimestamp;


    ///////////EOF Attributes

    public Sensor_Accelerometer(SensorManager sensorManager)
    {
        super(sensorManager);
        mSensor =  msensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        misTilted = true;
    }


    public void doZeroTilt()
    {
        mcounterCalibrated = mcalibrationCount;
        tiltValues[0] = 0.0f;
        tiltValues[1] = 0.0f;
        tiltValues[2] = 0.0f;
        misTilted = false;

        return ;
    }

    public Boolean isZeroTilt()
    {
        return misTilted;
    }


    /*
        It seems that values get from android is always for 2G and from -19.61 to + 19.61
        This is based on S5, SPlus & Prestigio
     */
    @Override
    public void onSensorChanged(SensorEvent event)
    {

        // alpha is calculated as t / (t + dT)
        // with t, the low-pass filter's time-constant
        // and dT, the event delivery rate

        if (misCalibrated == false)
        {

            calibrateSensor(event.values);

            return ;
        }



        rawValues[0]= event.values[0];
        rawValues[1]= event.values[1];
        rawValues[2]= event.values[2];


        smoothedValues[0] = alpha * smoothedValues[0] + (1 - alpha) * (rawValues[0] - calibrationValues[0]);
        smoothedValues[1] = alpha * smoothedValues[1] + (1 - alpha) * (rawValues[1] - calibrationValues[1]);
        smoothedValues[2] = alpha * smoothedValues[2] + (1 - alpha) * (rawValues[2] - calibrationValues[2]);

        if (misTilted == false)
        {
            tiltSensor(smoothedValues);

            return ;
        }

        // Negative Roll is for Gyro compatible
        vAcc.setXYZ(-smoothedValues[0],smoothedValues[1],smoothedValues[2]+ 9.8f);
        vAcc.normalize();

        linear_acceleration[0] = rawValues[0] + smoothedValues[0] + calibrationValues[0];
        linear_acceleration[1] = rawValues[1] + smoothedValues[1] + calibrationValues[1];
        linear_acceleration[2] = rawValues[2] + smoothedValues[2] + calibrationValues[2];

        final long ddT = (event.timestamp - LastTimestamp) / 1000000;
        final float dT = ddT *  0.001f;
        linear_velocity[0] += linear_acceleration[0] * dT;
        linear_velocity[1] += linear_acceleration[1] * dT;
        linear_velocity[2] += linear_acceleration[2] * dT;

        LastTimestamp = event.timestamp;
    }


    /**
     * Calculates Zero Acc values.
     * You need to add + 9.8f to Acc-Z to reserve gravity effect
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

        return ;
    }

    protected void tiltSensor (double[] values)
    {
        mcounterCalibrated -=1;
        tiltValues[0] +=  values[0] ;
        tiltValues[1] +=  values[1] ;
        tiltValues[2] +=  values[2] ;

        if (mcounterCalibrated ==0)
        {
            misTilted = true;
            tiltValues[0] /= mcalibrationCount;
            tiltValues[1] /= mcalibrationCount;
            tiltValues[2] /= mcalibrationCount;
        }

        return ;
    }
}
