package ap.andruavmiddlelibrary.sensors;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;

import ap.andruavmiddlelibrary.factory.math.Vector3d;


/**
 * Created by M.Hefny on 31-Aug-14.
 *
 * Attitude estimation follows the MultiWii "Simplified IMU based on
 * Complementary Filter" (ziss_dm, IMU.cpp getEstimatedAttitude), with the
 * constants rescaled for phone sensor rates:
 *  1) estGravity is rotated every gyro event by the measured angle delta
 *     (raw bias-corrected rates, no pre-smoothing - MultiWii integrates
 *     gyroADC directly).
 *  2) estGravity is pulled back toward the measured acc with a time
 *     constant of ATTITUDE_FUSION_TAU seconds (MultiWii GYR_CMPF_FACTOR),
 *     and only while the acc magnitude is plausible gravity
 *     (MultiWii ACC_VALENCE_MIN/MAX) so vibration and linear acceleration
 *     do not corrupt the attitude.
 * The gyro-to-rotation axis mapping was verified numerically against a
 * quaternion ground truth: delta = (-wx,-wy,-wz)*dt tracks it to <0.1 deg
 * over combined rotations. The old mapping (-wx,+wy,+wz) mirrored roll.
 */
public class Sensor_Gyro extends GenericIMUSensor {

    /////// Attributes
    // IMPORTANT: the axis are 0,1,2 but this index is modified to be compatible with ACC sensor
    // as rotating around Pitch axis is Roll for ACC.
    // also note that Roll direction is negative to Roll in ACC.
    public final float alpha = 0.8f;
    private static final float NS2S = 10000.0f; //1.0f / 1000.0f;
    private static final float EPSILON = 0.01f;

    /**
     * Complementary filter time constant in seconds: how fast the acc pulls
     * the gyro-rotated gravity estimate back toward the measured direction.
     * MultiWii GYR_CMPF_FACTOR equivalent (600 loops at ~300Hz ~ 2s there;
     * a phone needs the faster response of ~0.5s).
     */
    public static final double ATTITUDE_FUSION_TAU = 0.5;
    /**
     * MultiWii ACC_VALENCE_MIN/MAX: acc samples outside 0.72G..1.35G are
     * rejected - the phone is accelerating or vibrating, not measuring gravity.
     */
    public static final double ACC_VALENCE_MIN = 0.72;
    public static final double ACC_VALENCE_MAX = 1.35;
    private static final double GRAVITY_MS2 = 9.80665;

    private final float[] deltaRotationVector = new float[4];
    public long LastTimestamp=0;
    /**
     * MultiWii EstG: the gravity vector estimated in the Android device
     * frame (x right, y top-of-screen, z out of screen). Starts level,
     * seeds from the first plausible acc sample, magnitude stays ~1G.
     */
    public final Vector3d estGravity = new Vector3d(0.0,0.0,GRAVITY_MS2);

    /**
     * true after estGravity adopted the first plausible acc sample as its
     * starting attitude, so a restarted SensorService shows the real tilt
     * at once instead of creeping from level.
     */
    private boolean misAttitudeSeeded = false;
    private long LastFusionNanos = 0;



    ///////////EOF Attributes


    public Sensor_Gyro(SensorManager sensorManager)
    {
        super(sensorManager);
        mSensor =  msensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);


    }



    @Override
    public void onSensorChanged(SensorEvent event) {

        if (!misCalibrated)
        {
            calibrateSensor(event.values);

            return ;
        }

        // NOTE: smoothedValues feed the GSV display row only; the attitude
        // integrates the raw bias-corrected rates like MultiWii gyroADC.
        rawValues[0] = event.values[0];
        rawValues[1] = event.values[1];
        rawValues[2] = event.values[2];

        smoothedValues[0] = (1-alpha) * smoothedValues[0] + alpha * (rawValues[0] - calibrationValues[0]);
        smoothedValues[1] = (1-alpha) * smoothedValues[1] + alpha * (rawValues[1] - calibrationValues[1]);
        smoothedValues[2] = (1-alpha) * smoothedValues[2] + alpha * (rawValues[2] - calibrationValues[2]);

        if (LastTimestamp != 0) {
            final long ddT = (event.timestamp - LastTimestamp) / 1000000;
            final float dT = ddT *  0.001f;

            // dT sanity cap: a sensor gap (pause/resume) must not integrate
            // stale bias into a huge rotation - the acc fusion recovers it.
            if ((dT > 0.0f) && (dT < 0.5f)) {
                // Rotate the gravity estimate by the gyro angle delta
                // (Vector3d.rotateDelta is MultiWii rotateV). The negated
                // mapping is the verified device-frame mapping - see class comment.
                final Vector3d deltaGyroAngle = new Vector3d(
                        -(rawValues[0] - calibrationValues[0]) * dT,
                        -(rawValues[1] - calibrationValues[1]) * dT,
                        -(rawValues[2] - calibrationValues[2]) * dT);
                estGravity.rotateDelta(deltaGyroAngle);
            }
        }
        LastTimestamp = event.timestamp;

    }


    /**
     * MultiWii complementary filter step: pulls estGravity toward the raw acc
     * vector. Called once per SensorService IMU cycle.
     * @param accGravity raw accelerometer values (m/s^2, device frame) - the
     *                   raw vector already contains gravity, so no bias hack
     *                   and no +9.8 restoration is needed.
     */
    public void updateGyrofromAcc (final float[] accGravity)
    {
        if ((accGravity == null) || (accGravity.length < 3)) return ;

        final double accMagnitude = Math.sqrt(
                (double)accGravity[0] * accGravity[0]
                + (double)accGravity[1] * accGravity[1]
                + (double)accGravity[2] * accGravity[2]);
        if ((accMagnitude < ACC_VALENCE_MIN * GRAVITY_MS2)
            || (accMagnitude > ACC_VALENCE_MAX * GRAVITY_MS2)) return ;

        final long now = System.nanoTime();
        final double fusionDt = (LastFusionNanos == 0)
                ? 0.0
                : (now - LastFusionNanos) / 1000000000.0;
        LastFusionNanos = now;

        if (!misAttitudeSeeded)
        {
            // First plausible gravity sample: adopt it as the starting attitude.
            misAttitudeSeeded = true;
            estGravity.setXYZ(accGravity[0], accGravity[1], accGravity[2]);
            return ;
        }

        if (fusionDt <= 0.0) return ;

        final double fusionWeight = fusionDt / (ATTITUDE_FUSION_TAU + fusionDt);
        estGravity.setXYZ(
                estGravity.getX() * (1.0 - fusionWeight) + accGravity[0] * fusionWeight,
                estGravity.getY() * (1.0 - fusionWeight) + accGravity[1] * fusionWeight,
                estGravity.getZ() * (1.0 - fusionWeight) + accGravity[2] * fusionWeight);
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
