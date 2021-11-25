package ap.andruavmiddlelibrary.sensors;


/**
 * Created by M.Hefny on 31-Aug-14.
 */
public class AHRS {

    /// Gets or sets the sample period.
    public float mSamplePeriod;
    public float mLastReadingTime;
    /// Gets or sets the algorithm proportional gain.
    public float mKp;
    /// Gets or sets the algorithm integral gain.
    public float mKi;
    /// Gets or sets the Quaternion output.
    public float[] mQuaternion;
    /// Gets or sets the integral error.
    private float[] meInt;


    public float Roll,Pitch,Yaw,vRoll,vPitch;



    public void init( float SamplePeriod, float kp, float ki)
    {
        mSamplePeriod = SamplePeriod;
        mKp = kp;
        mKi = ki;
        mQuaternion = new float[] { 1f, 0f, 0f, 0f };
        meInt = new float[] { 0f, 0f, 0f };
    }


    public void update(float gx, float gy, float gz, float ax, float ay, float az, float mx, float my, float mz)
    {
        float q1 = mQuaternion[0], q2 = mQuaternion[1], q3 = mQuaternion[2], q4 = mQuaternion[3];   // short name local variable for readability
        float norm;
        float hx, hy, bx, bz;
        float vx, vy, vz, wx, wy, wz;
        float ex, ey, ez;
        float pa, pb, pc;


        // Auxiliary variables to avoid repeated arithmetic
        float q1q1 = q1 * q1;
        float q1q2 = q1 * q2;
        float q1q3 = q1 * q3;
        float q1q4 = q1 * q4;
        float q2q2 = q2 * q2;
        float q2q3 = q2 * q3;
        float q2q4 = q2 * q4;
        float q3q3 = q3 * q3;
        float q3q4 = q3 * q4;
        float q4q4 = q4 * q4;

        // Normalise accelerometer measurement
        norm = (float)Math.sqrt(ax * ax + ay * ay + az * az);
        if (norm == 0f) return; // handle NaN
        norm = 1 / norm;        // use reciprocal for division
        ax *= norm;
        ay *= norm;
        az *= norm;

        // Normalise magnetometer measurement
        norm = (float)Math.sqrt(mx * mx + my * my + mz * mz);
        if (norm == 0f) return; // handle NaN
        norm = 1 / norm;        // use reciprocal for division
        mx *= norm;
        my *= norm;
        mz *= norm;

        // Reference direction of Earth's magnetic field
        hx = 2f * mx * (0.5f - q3q3 - q4q4) + 2f * my * (q2q3 - q1q4) + 2f * mz * (q2q4 + q1q3);
        hy = 2f * mx * (q2q3 + q1q4) + 2f * my * (0.5f - q2q2 - q4q4) + 2f * mz * (q3q4 - q1q2);
        bx = (float)Math.sqrt((hx * hx) + (hy * hy));
        bz = 2f * mx * (q2q4 - q1q3) + 2f * my * (q3q4 + q1q2) + 2f * mz * (0.5f - q2q2 - q3q3);

        // Estimated direction of gravity and magnetic field
        vx = 2f * (q2q4 - q1q3);
        vy = 2f * (q1q2 + q3q4);
        vz = q1q1 - q2q2 - q3q3 + q4q4;
        wx = 2f * bx * (0.5f - q3q3 - q4q4) + 2f * bz * (q2q4 - q1q3);
        wy = 2f * bx * (q2q3 - q1q4) + 2f * bz * (q1q2 + q3q4);
        wz = 2f * bx * (q1q3 + q2q4) + 2f * bz * (0.5f - q2q2 - q3q3);

        // Error is cross product between estimated direction and measured direction of gravity
        ex = (ay * vz - az * vy) + (my * wz - mz * wy);
        ey = (az * vx - ax * vz) + (mz * wx - mx * wz);
        ez = (ax * vy - ay * vx) + (mx * wy - my * wx);
        if (mKi > 0f)
        {
            meInt[0] += ex;      // accumulate integral error
            meInt[1] += ey;
            meInt[2] += ez;
        }
        else
        {
            meInt[0] = 0.0f;     // prevent integral wind up
            meInt[1] = 0.0f;
            meInt[2] = 0.0f;
        }

        // Apply feedback terms
        gx = gx + mKp * ex + mKi * meInt[0];
        gy = gy + mKp * ey + mKi * meInt[1];
        gz = gz + mKp * ez + mKi * meInt[2];

        // Integrate rate of change of quaternion
        pa = q2;
        pb = q3;
        pc = q4;
        q1 = q1 + (-q2 * gx - q3 * gy - q4 * gz) * (0.5f * mSamplePeriod);
        q2 = pa + ( q1 * gx + pb * gz - pc * gy) * (0.5f * mSamplePeriod);
        q3 = pb + ( q1 * gy - pa * gz + pc * gx) * (0.5f * mSamplePeriod);
        q4 = pc + ( q1 * gz + pa * gy - pb * gx) * (0.5f * mSamplePeriod);

        // Normalise quaternion
        norm = (float)Math.sqrt(q1 * q1 + q2 * q2 + q3 * q3 + q4 * q4);
        norm = 1.0f / norm;
        mQuaternion[0] = q1 * norm;
        mQuaternion[1] = q2 * norm;
        mQuaternion[2] = q3 * norm;
        mQuaternion[3] = q4 * norm;

        //http://answers.unity3d.com/questions/416169/finding-pitchrollyaw-from-quaternions.html
    //    Roll  = (float)Math.atan2(2 * mQuaternion[1] * mQuaternion[3] - 2 * mQuaternion[0] * mQuaternion[2], 1- 2 * mQuaternion[1] * mQuaternion[1] - 2 * mQuaternion[2] * mQuaternion[2]);
  //      Pitch = (float)Math.atan2(2 * mQuaternion[0] * mQuaternion[3] - 2 * mQuaternion[1] * mQuaternion[2], 1- 2 * mQuaternion[0] * mQuaternion[0] - 2 * mQuaternion[2] * mQuaternion[2]);
//        Yaw   = (float)Math.asin (2 * mQuaternion[0] * mQuaternion[1] + 2 * mQuaternion[2] * mQuaternion[3]);


        Pitch  = (float)Math.atan2(2 * (mQuaternion[1] * mQuaternion[2] + mQuaternion[3] * mQuaternion[0]) , mQuaternion[3]* mQuaternion[3] - mQuaternion[0]* mQuaternion[0] - mQuaternion[1] * mQuaternion[1] + mQuaternion[2] * mQuaternion[2]);
        Roll =   (float)Math.atan2(2 * (mQuaternion[0] * mQuaternion[1] + mQuaternion[3] * mQuaternion[2]) , mQuaternion[3]* mQuaternion[3] + mQuaternion[0]* mQuaternion[0] - mQuaternion[1] * mQuaternion[1] - mQuaternion[2] * mQuaternion[2]);
        Yaw   =  (float)Math.asin (2 * mQuaternion[3] * mQuaternion[1] - 2 * mQuaternion[0] * mQuaternion[2]);

        // att.angle[ROLL]  = _atan2(EstG.V16.X , EstG.V16.Z);
        // att.angle[PITCH] = _atan2(EstG.V16.Y , InvSqrt(sqGX_sqGZ)*sqGX_sqGZ);

        vRoll =   (float)Math.atan2(Roll, Yaw);
    }

}
