package com.andruav.event.droneReport_7adath;

import com.andruav.andruavUnit.AndruavUnitBase;
import com.andruav.util.AndruavPoint2Dd;

/**
 * Created by mhefny on 2/22/17.
 * Track can be using DNN or CV so {@link #ClassName} can be used or can be null. same for {@link #Percentage}
 */

public class _7adath_TRK_Target_TrackedAt {

    public AndruavUnitBase mAndruavWe7da;

    public AndruavPoint2Dd Corner1;
    public AndruavPoint2Dd Corner2;
    public String ClassName;
    public double Percentage;


    /***
     * Normalized values
     * @param andruavUnit
     * @param corner1_x
     * @param corner1_y
     * @param corner2_x
     * @param corner2_y
     */
    public _7adath_TRK_Target_TrackedAt(final AndruavUnitBase andruavUnit, final double corner1_x, final double corner1_y, final double corner2_x, final double corner2_y, final String className, final double percetnage)
    {
        mAndruavWe7da = andruavUnit;
        Corner1 = new AndruavPoint2Dd(corner1_x,corner1_y);
        Corner2 = new AndruavPoint2Dd(corner2_x,corner2_y);
        ClassName = className;
        Percentage = percetnage;
    }


    /***
     * Non Normalized values.
     * @param andruavUnit
     * @param topleft_x
     * @param topleft_y
     * @param bottomright_x
     * @param bottomright_y
     * @param width
     * @param height
     */
    public _7adath_TRK_Target_TrackedAt(final AndruavUnitBase andruavUnit, final double topleft_x, final double topleft_y , final double bottomright_x, final double bottomright_y, final int width, final int height)
    {

        mAndruavWe7da = andruavUnit;
        Corner1 = new AndruavPoint2Dd(
                (topleft_x - (width  / 2.0)) / (width /2.0),
                (topleft_y - (height / 2.0)) / (height /2.0)
        );
        Corner2 = new AndruavPoint2Dd(
                (bottomright_x - (width  / 2.0)) / (width /2.0),
                (bottomright_y - (height / 2.0)) / (height /2.0)
        );
    }
}


