package com.andruav.controlBoard.shared.geoFence;

/**
 * Created by mhefny on 6/23/16.
 */
public class PointD
{

    /***
     * ratio 0 - 1  means the point is near to P1 or P2 of the segment.
     * <br> @see http://csharphelper.com/blog/2014/08/find-the-shortest-distance-between-a-point-and-a-line-segment-in-c/
     */
    public double t;



    public double x;

    public double y;

    public PointD()
    {

    }
    public PointD(double x, double y)
    {
        this.x = x;
        this.y = y;
    }

    public void setPoint (double x, double y)
    {
        this.x = x;
        this.y = y;
    }
    public void setPoint (final PointD f)
    {
        this.x = f.x;
        this.y = f.y;
    }
}
