package ap.andruavmiddlelibrary.sensors;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;

import ap.andruavmiddlelibrary.factory.math.Vector3d;


/**
 * Created by M.Hefny on 31-Aug-14.
 */
public class Sensor_Gyro extends GenericIMUSensor {

    /////// Attributes
    // Roll - Pitch - Z : index 1,0,2
    // IMPORTANT: the axis are 0,1,2 but this index is modified to be compatible with ACC sensor
    // as rotating around Pitch axis is Roll for ACC.
    // also note that Roll direction is negative to Roll in ACC.
    public float alphaAccCorrection = 0.02f;
    public float alpha = 0.8f;
    private static final float NS2S = 10000.0f; //1.0f / 1000.0f;
    private static final float EPSILON = 0.01f;

    private final float[] deltaRotationVector = new float[4];
    public long LastTimestamp=0;
    public Vector3d vGyro = new Vector3d(0.0,0.0,1.0);




    ///////////EOF Attributes

    public Sensor_Gyro(SensorManager sensorManager)
    {
        super(sensorManager);
        mSensor =  msensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);


    }



    @Override
    public void onSensorChanged(SensorEvent event) {

        if (misCalibrated == false)
        {
            calibrateSensor(event.values);

            return ;
        }

        // Axis of the rotation sample, not normalized yet.
        // NOTE: Gyro AXIS is switched with ACC as rotation around PITCH axis is ROLL for ACC
        // also rotation direction is on the negative direction.

        rawValues[0] = event.values[0];
        rawValues[1] = event.values[1];
        rawValues[2] = event.values[2];

        smoothedValues[0] = (1-alpha) * smoothedValues[0] + alpha * (rawValues[0] - calibrationValues[0]);
        smoothedValues[1] = (1-alpha) * smoothedValues[1] + alpha * (rawValues[1] - calibrationValues[1]);
        smoothedValues[2] = (1-alpha) * smoothedValues[2] + alpha * (rawValues[2] - calibrationValues[2]);



        if (LastTimestamp != 0) {
            final long ddT = (event.timestamp - LastTimestamp) / 1000000;
            final float dT = ddT *  0.001f;

            Vector3d deltaGyro = new Vector3d(-smoothedValues[0], smoothedValues[1], smoothedValues[2] );
            //float length = deltaGyro.getLength();
            // if (length > EPSILON ) {
                deltaGyro.multiplyByScalar(dT);


                // Normalize the rotation vector if it's big enough to get the axis
                vGyro.rotateDelta(deltaGyro);
                vGyro.normalize();
            //}

        }
        LastTimestamp = event.timestamp;

    }


    public void updateGyrofromAcc (Vector3d vAcc)
    {
        double vlength = vAcc.getLength();
        double[] gyro;
        double[] acc;
        if ( vlength < 1.5f && vlength > 0.8f)
        {
            gyro = vGyro.getXYZ();
            acc = vAcc.getXYZ();

            gyro[0] = (1- alphaAccCorrection) * gyro[0] + alphaAccCorrection * acc[0];
            gyro[1] = (1- alphaAccCorrection) * gyro[1] + alphaAccCorrection * acc[1];
            gyro[2] = (1- alphaAccCorrection) * gyro[2] + alphaAccCorrection * acc[2];

            vGyro.setXYZ(gyro[0],gyro[1],gyro[2]);

        }
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
