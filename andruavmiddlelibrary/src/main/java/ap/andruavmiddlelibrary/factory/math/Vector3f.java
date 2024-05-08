package ap.andruavmiddlelibrary.factory.math;

/**
 * Created by M.Hefny on 01-Sep-14.
 * code is  originally from https://bitbucket.org/apacha/sensor-fusion-demo/downloads
 */
public class Vector3f {
    /**
     * A float array was chosen instead of individual variables due to performance concerns. Converting the points into
     * an array at run time can cause slowness so instead we use one array and extract the individual variables with get
     * methods.
     */
    protected final float[] points = new float[3];


    /**
    * Instantiates a new vector3f.
    */
    public Vector3f ()
    {
        zeroVector();
    }

    /**
     * Initialises the vector with the given values
     *
     * @param x the x-component
     * @param y the y-component
     * @param z the z-component
     */
    public Vector3f(float x, float y, float z) {
        this.points[0] = x;
        this.points[1] = y;
        this.points[2] = z;
    }

    /**
     * Initialises all components of this vector with the given same value.
     *
     * @param value Initialisation value for all components
     */
    public Vector3f(float value) {
        this.points[0] = value;
        this.points[1] = value;
        this.points[2] = value;
    }




    /**
     * Copy constructor
     */
    public Vector3f(Vector3f vector) {
        this.points[0] = vector.points[0];
        this.points[1] = vector.points[1];
        this.points[2] = vector.points[2];
    }

    /**
     * Returns this vector as float-array.
     *
     * @return the float[]
     */
    public float[] toArray() {
        return this.points;
    }

    /**
     * Adds a vector to this vector
     *
     * @param summand the vector that should be added component-wise
     */
    public void add(Vector3f summand) {
        this.points[0] += summand.points[0];
        this.points[1] += summand.points[1];
        this.points[2] += summand.points[2];
    }

    /**
     * Adds the value to all components of this vector
     *
     * @param summand The value that should be added to all components
     */
    public void add(float summand) {
        this.points[0] += summand;
        this.points[1] += summand;
        this.points[2] += summand;
    }

    /**
     *
     * @param subtrahend
     */
    public void subtract(Vector3f subtrahend) {
        this.points[0] -= subtrahend.points[0];
        this.points[1] -= subtrahend.points[1];
        this.points[2] -= subtrahend.points[2];
    }

    /**
     * Multiply by scalar.
     *
     * @param scalar the scalar
     */
    public void multiplyByScalar(float scalar) {
        this.points[0] *= scalar;
        this.points[1] *= scalar;
        this.points[2] *= scalar;
    }


    /**
     * Normalize.
     */
    public void normalize() {

        float len = getLength();
        // @<https://code.google.com/p/stardroid/source/browse/trunk/app/src/com/google/android/stardroid/util/VectorUtil.java></https://code.google.com/p/stardroid/source/browse/trunk/app/src/com/google/android/stardroid/util/VectorUtil.java>
        if (len < 0.000001f) {
            zeroVector();
        }
        scale(1.0f/len);
    }


    public void scale(float factor )
    {
        this.points[0] = this.points[0] * factor;
        this.points[1] = this.points[1] * factor;
        this.points[2] = this.points[2] * factor;

    }

    /**
     * Rotate using Multiwii code
     */
    public void rotateDelta (Vector3f Delta)
    {
        float[][] mat= new float [3][3];                                      // This does a  "proper" matrix rotation using gyro deltas without small-angle approximation
        float cosx, sinx, cosy, siny, cosz, sinz;
        float coszcosx, coszcosy, sinzcosx, coszsinx, sinzsinx;
        float[] snap_points = points.clone();
        cosx = (float)Math.cos(Delta.getX());
        sinx = (float)Math.sin(Delta.getX());
        cosy = (float)Math.cos(Delta.getY());
        siny = (float)Math.sin(Delta.getY());
        cosz = (float)Math.cos(Delta.getZ());
        sinz = (float)Math.sin(Delta.getZ());

        coszcosx = cosz * cosx;
        coszcosy = cosz * cosy;
        sinzcosx = sinz * cosx;
        coszsinx = sinx * cosz;
        sinzsinx = sinx * sinz;

        mat[0][0] = coszcosy;
        mat[0][1] = sinz * cosy;
        mat[0][2] = -siny;
        mat[1][0] = (coszsinx * siny) - sinzcosx;
        mat[1][1] = (sinzsinx * siny) + (coszcosx);
        mat[1][2] = cosy * sinx;
        mat[2][0] = (coszcosx * siny) + (sinzsinx);
        mat[2][1] = (sinzcosx * siny) - (coszsinx);
        mat[2][2] = cosy * cosx;

        points[0] = snap_points[0] * mat[0][0] + snap_points[1] * mat[1][0] + snap_points[2] * mat[2][0];
        points[1] = snap_points[0] * mat[0][1] + snap_points[1] * mat[1][1] + snap_points[2] * mat[2][1];
        points[2] = snap_points[0] * mat[0][2] + snap_points[1] * mat[1][2] + snap_points[2] * mat[2][2];

    }

    /**
     * Gets the x.
     *
     * @return the x
     */
    public float getX() {
        return points[0];
    }

    /**
     * Gets the y.
     *
     * @return the y
     */
    public float getY() {
        return points[1];
    }

    /**
     * Gets the z.
     *
     * @return the z
     */
    public float getZ() {
        return points[2];
    }

    public float[] getXYZ()
    {
        return points.clone();
    }
    /**
     * Sets the x.
     *
     * @param x the new x
     */
    public void setX(float x) {
        this.points[0] = x;
    }

    /**
     * Sets the y.
     *
     * @param y the new y
     */
    public void setY(float y) {
        this.points[1] = y;
    }

    /**
     * Sets the z.
     *
     * @param z the new z
     */
    public void setZ(float z) {
        this.points[2] = z;
    }

    /**
     * Functions for convenience
     */

    public void setXYZ(float x, float y, float z) {
        this.points[0] = x;
        this.points[1] = y;
        this.points[2] = z;
    }

    /**
     * Return the dot product of this vector with the input vector
     *
     * @param inputVec The vector you want to do the dot product with against this vector.
     * @return Float value representing the scalar of the dot product operation
     */
    public float dotProduct(Vector3f inputVec) {
        return points[0] * inputVec.points[0] + points[1] * inputVec.points[1] + points[2] * inputVec.points[2];

    }

    /**
     * Get the cross product of this vector and another vector. The result will be stored in the output vector.
     *
     * @param inputVec The vector you want to get the dot product of against this vector.
     * @param outputVec The vector to store the result in.
     */
    public void crossProduct(Vector3f inputVec, Vector3f outputVec) {
        outputVec.setX(points[1] * inputVec.points[2] - points[2] * inputVec.points[1]);
        outputVec.setY(points[2] * inputVec.points[0] - points[0] * inputVec.points[2]);
        outputVec.setZ(points[0] * inputVec.points[1] - points[1] * inputVec.points[0]);
    }

    public Vector3f crossProduct(Vector3f in) {
        Vector3f out = new Vector3f();
        crossProduct(in, out);
        return out;
    }

    /**
     * If you need to get the length of a vector then use this function.
     *
     * @return The length of the vector
     */
    public float getLength() {
        return (float) Math.sqrt(points[0] * points[0] + points[1] * points[1] + points[2] * points[2]);
    }


    public float getRoll () {
        return (float)(Math.atan2(points[0],points[2]));
    }

    public float getPitch() {
        return (float)(Math.asin(points[1] / (- Math.sqrt(getLength())))) ;
    }

    public void zeroVector ()
    {
        this.points[0] = 0.0f;
        this.points[1] = 0.0f;
        this.points[2] = 0.0f;
    }
}
