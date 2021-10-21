package com.andruav.event.droneReport_7adath;

import com.andruav.andruavUnit.AndruavUnitBase;
import com.andruav.util.AndruavPoint2D;
import com.andruav.util.AndruavPoint2Dd;

/**
 * Created by mhefny on 2/22/17.
 */

public class _7adath_TRK_Target_Ready {

    /***
     * This is the name of the REQUESTER.
     */
    public AndruavUnitBase mAndruavWe7da;

    /***
     * Normalized Point from [-1,1]
     */
    public AndruavPoint2Dd Corner1;
    public AndruavPoint2Dd Corner2;


    /***
     * if you use this then you should Normalize input from [-1 to 1]
     * @param andruavUnit
     * @param corner1_x
     * @param corner1_y
     * @param corner2_x
     * @param corner2_y
     */
    public _7adath_TRK_Target_Ready(final AndruavUnitBase andruavUnit, final double corner1_x, final double corner1_y, final double corner2_x, final double corner2_y)
    {
        mAndruavWe7da = andruavUnit;
        Corner1 = new AndruavPoint2Dd(corner1_x,corner1_y);
        Corner2 = new AndruavPoint2Dd(corner2_x,corner2_y);
    }

    public _7adath_TRK_Target_Ready(final AndruavUnitBase andruavUnit, final AndruavPoint2D corner1, final AndruavPoint2D corner2, final int width, final int height)
    {

        mAndruavWe7da = andruavUnit;
        Corner1 = new AndruavPoint2Dd(
               (corner1.X - (width  / 2.0)) / (width /2.0),
               (corner1.Y - (height / 2.0)) / (height /2.0)
        );
        Corner2 = new AndruavPoint2Dd(
                (corner2.X - (width  / 2.0)) / (width /2.0),
                (corner2.Y - (height / 2.0)) / (height /2.0)
        );
    }
}

