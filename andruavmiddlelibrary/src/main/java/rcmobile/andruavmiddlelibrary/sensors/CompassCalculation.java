package rcmobile.andruavmiddlelibrary.sensors;

import android.hardware.SensorManager;

/**
 * Created by M.Hefny on 04-Sep-14.
 * Also refer to : http://stackoverflow.com/questions/10192057/android-getorientation-method-returns-bad-results
 */
public class CompassCalculation {


    /////// Attributes

    public float alpha = 0.2f;

    public float azimuthCompass=0;
    public float pitchCompass=0;
    public float rollCompass=0;
    public float declination = 0;

    protected float[] mrotationMatrix = new float[9];
    protected float[] Imat = new float[9];

    public float[] vOrientation = new float[3];
    ///////////EOF Attributes






    public void processSensorData(float[] mGravity, float[] mMagneticField){
        boolean success = SensorManager.getRotationMatrix(mrotationMatrix, Imat, mGravity, mMagneticField);
        if (success) {
            SensorManager.getOrientation(mrotationMatrix, vOrientation);
            float yaw = vOrientation[0] + declination;
            float pitch = vOrientation[1];
            float roll = vOrientation[2];

            if (Math.abs(yaw-azimuthCompass) < 3.12) {
                azimuthCompass = (1 - alpha) * azimuthCompass + alpha * yaw;
            }
            else
            {   // what happens here is that value switched from -180 to 0 which make low pass filter to change
                // slowly from -180 to 0 .... switching from right to left and left to right at -180 makes the value
                // swings and corrupts UI.
                azimuthCompass = yaw;
            }
            if (Math.abs(pitch-pitchCompass) < 3.12) {
                pitchCompass = (1 - alpha) * pitchCompass + alpha * pitch;
           }
            else
            {
                pitchCompass=  pitch;
            }
            if (Math.abs(roll-rollCompass) < 3.12) {
                rollCompass = (1 - alpha) * rollCompass + alpha * roll;
            }
            else
            {
                rollCompass = roll;
            }
         }
   }




}
