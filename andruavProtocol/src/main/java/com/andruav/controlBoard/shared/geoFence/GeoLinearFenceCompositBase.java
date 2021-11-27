package com.andruav.controlBoard.shared.geoFence;

import com.andruav.andruavUnit.AndruavUnitBase;
import com.andruav.event.droneReport_Event.Event_GeoFence_Hit;
import com.andruav.util.GPSHelper;

/**
 * Created by mhefny on 6/17/16.
 */
public class GeoLinearFenceCompositBase extends GeoFenceCompositBase {




    private PointD closest;



    public GeoLinearFenceCompositBase()
    {
        shouldKeepOutside = false;
    }

    /***
     *
     * @return point on Fence where distance to measured point was within range. Please note that this could be not the nearest point. {@link #testPoint}
     */
    public PointD getPointonFence ()
    {
        return closest;
    }

    /***
     * sendMessageToModule Announcement {@link Event_GeoFence_Hit} when:
     * <br>1- a toggle in state happpened.
     * <br>2- if old state was {@link #NOT_TESTED}.
     * <br>3- if any toggle.
     * @param inside
     */
    @Override
    protected void set_isInside(final AndruavUnitBase andruavUnitBase, final boolean inside, final double distance)
    {
        final Event_GeoFence_Hit a7adath_geoFence_hit = this.mAndruavUnits.get(andruavUnitBase.PartyID);
        if (a7adath_geoFence_hit == null) return ; //Should never happen

        a7adath_geoFence_hit.distance           = -1;
        a7adath_geoFence_hit.shouldKeepOutside  = this.shouldKeepOutside;



        if ((!a7adath_geoFence_hit.hasValue) || (a7adath_geoFence_hit.inZone != inside)) // as inZone is by default false
        {
            // Value Toggle

            a7adath_geoFence_hit.hasValue       = true;

            a7adath_geoFence_hit.inZone         = inside;

            fireEvent (a7adath_geoFence_hit);
        }
    }





    public GeoFencePoint getWayPointByHash (final double hash)
    {
        final int len = this.size();

        GeoFencePoint geoFencePoint;
        for (int i=0;i<len;i=i+1)
        {

            geoFencePoint = this.valueAt(i);
            if (geoFencePoint.getHash() == hash)
            {
                return geoFencePoint;
            }
        }

        return null;
    }




    /***
     *
     * @param lat
     * @param lng
     * @param fireEvent if <b>true</b> then fire event {@link Event_GeoFence_Hit} if there is change in state.
     * @return a distance that is either Double.NaN or less than {@link #maxDistance}
     */
    @Override
    public double testPoint(final AndruavUnitBase andruavUnitBase, final double lat, final double lng, boolean fireEvent)  {

        if (mAndruavUnits.containsKey(andruavUnitBase.PartyID)==false) return Double.NaN;

        final int size = this.size();

        if (size==0) return  Double.NaN;

        double[] X = new double[size];
        double[] Y = new double[size];

        for (int i=0;i<size;++i)
        {
            final GeoFencePoint geoFencePoint = this.valueAt(i);

            X[i] = geoFencePoint.Latitude;
            Y[i] = geoFencePoint.Longitude;
        }

        final double distance = testPoint(lat,lng,X,Y);

        if (andruavUnitBase.IsMe() && fireEvent)
        {
            set_isInside(andruavUnitBase,!Double.isNaN(distance),distance);
        }

        return distance;
    }

    /****
     * Test each line segment against a given location.
     * <br><b>Successfull Result</b> means the point is near to a line segment less than {@link #maxDistance}
     * <br>function returns with te first success <b>NOT</b> the real closest point to all lines.
     * <br>see http://csharphelper.com/blog/2014/08/find-the-shortest-distance-between-a-point-and-a-line-segment-in-c/
     *
     * @param XP
     * @param YP
     * @param X
     * @param Y
     * @return
     */
    private double testPoint(final double XP, final double YP, final double[] X, final double[] Y)
    {

        if (X.length != Y.length) throw  new IllegalArgumentException();

        for (int i = 0; i < X.length -1 ; ++ i)
        {
            closest = FindDistanceToSegment (XP,YP,X[i],Y[i],X[i+1],Y[i+1]);
            if ((closest.t <= 1) && (closest.t >= 0))
            {
                if (distance <= maxDistance) {
                    return distance;
                }
            }
        }
        return Double.NaN;
    }




    // Calculate the distance between
// point pt and the segment p1 --> p2.
    private  PointD FindDistanceToSegment(
            double ptx, double pty, double p1x, double p1y, double p2x, double p2y)
    {
        PointD  closest = new PointD();
        double dx = p2x - p1x;
        double dy = p2y - p1y;
        if ((dx == 0) && (dy == 0))
        {
            // It's a point not a line segment.
            closest.setPoint(p1x,p1y);
            //dx = ptx - p1x;
            //dy = pty - p1y;
          //  closest.distance = Math.sqrt(dx * dx + dy * dy);
            distance = GPSHelper.calculateDistance(pty,ptx,closest.y,closest.x);
            return closest;
        }

        // Calculate the t that minimizes the distance.
        double t = ((ptx - p1x) * dx + (pty - p1y) * dy) /
                (dx * dx + dy * dy);

        // See if this represents one of the segment's
        // end points or a point in the middle.
        if (t < 0)
        {
            closest = new PointD(p1x, p1y);
            //dx = ptx - p1x;
            //dy = pty - p1y;
        }
        else if (t > 1)
        {
            closest = new PointD(p2x, p2y);
            //dx = ptx - p2x;
            //dy = pty - p2y;
        }
        else
        {
            closest = new PointD(p1x + t * dx, p1y + t * dy);
            //dx = ptx - closest.x;
            //dy = pty - closest.y;
        }

        closest.t = t;
        //closest.distance = Math.sqrt(dx * dx + dy * dyl);
        distance = GPSHelper.calculateDistance(pty,ptx,closest.y,closest.x);
        return closest;
    }
}
