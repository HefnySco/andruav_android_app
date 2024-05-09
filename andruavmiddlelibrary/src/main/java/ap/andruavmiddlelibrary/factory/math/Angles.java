package ap.andruavmiddlelibrary.factory.math;

/**
 * Created by M.Hefny on 17-Sep-14.
 */
public class Angles {

    public static final float PI = 3.1415926535897932384626433832795f;
    public static float PI_2 = 1.5707963267948966192313216916398f;
    // Convert Degrees to Radians
    public static final float DEGREES_TO_RADIANS = PI / 180.0f;

    // Convert Radians to Degrees
    public static final float RADIANS_TO_DEGREES = 180.0f / PI;



    /***
     *
     * @param tmpAngle Angle in Degree
     * @return Wrapped angle between -180 & +180
     */
    public static float restrictAngle(float tmpAngle){

         while(tmpAngle>=360) tmpAngle-=360;
         while(tmpAngle<-360) tmpAngle+=360;

        return tmpAngle;
    }


    public static float to_Degrees (float radianAngle){
        return RADIANS_TO_DEGREES * radianAngle ;
    }


}
