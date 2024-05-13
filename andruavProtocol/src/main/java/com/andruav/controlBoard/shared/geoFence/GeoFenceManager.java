package com.andruav.controlBoard.shared.geoFence;

import android.location.Location;

import androidx.collection.SimpleArrayMap;

import com.andruav.AndruavFacade;
import com.andruav.AndruavEngine;
import com.andruav.AndruavSettings;
import com.andruav.andruavUnit.AndruavUnitBase;
import com.andruav.event.droneReport_Event.Event_GeoFence_Hit;
import com.andruav.event.droneReport_Event.Event_GeoFence_Ready;
import com.andruav.controlBoard.shared.common.FlightMode;
import com.andruav.notification.PanicFacade;


/**
 * Created by mhefny on 6/25/16.
 */
public class GeoFenceManager {

    /***
     * GeoFenceNames are Unique ... it is still not Clear _HOW TO INFORCE IT :( -
     * so same {@link GeoFenceCompositBase} with same {@link GeoFenceCompositBase#fenceName} is the same and should not be replicated.
     */
    protected final static SimpleArrayMap<String,GeoFenceBase> mGeoFenceBaseArray = new SimpleArrayMap<>();



    public static void clear ()
    {
        mGeoFenceBaseArray.clear();
    }


    public static int size()
    {
        return  mGeoFenceBaseArray.size();
    }

    public static GeoFenceBase valueAt (final int i)
    {
        return mGeoFenceBaseArray.valueAt(i);
    }

    public static GeoFenceBase get (final String key)
    {
        return mGeoFenceBaseArray.get(key);
    }

    public static boolean containsKey (final String key)
    {
        return mGeoFenceBaseArray.containsKey(key);
    }

    public static void addGeoFence (final AndruavUnitBase andruavUnitBase_new, final GeoFenceBase geoFenceMapBase_new)
    {
        GeoFenceBase geoFenceBase = getGeoFence (geoFenceMapBase_new.fenceName);
        if (geoFenceBase == null)
        {
            // fence is not defined add fence to array
            mGeoFenceBaseArray.put(geoFenceMapBase_new.fenceName,geoFenceMapBase_new);
            geoFenceBase = geoFenceMapBase_new;
        }

        final Event_GeoFence_Hit a7adath_geoFence_hit = geoFenceBase.mAndruavUnits.get(andruavUnitBase_new);
        if (a7adath_geoFence_hit ==null) {
            // add unit to Fence
            geoFenceBase.mAndruavUnits.put(andruavUnitBase_new.PartyID, new Event_GeoFence_Hit(andruavUnitBase_new,geoFenceMapBase_new.fenceName));
        }

        // Event that a Fence is Ready or a Drone is Attached to a Fence
        AndruavEngine.getEventBus().post(new Event_GeoFence_Ready(andruavUnitBase_new, geoFenceMapBase_new.fenceName)); // inform all that a data is ready

    }

    /***
     * Remove fence objects ad sendMessageToModule deattach.
     */
    public static void clearFenceData()
    {
        final int size = GeoFenceManager.size();
        for (int i = 0 ; i < size; ++i)
        {
            final GeoFenceBase geoFenceBase   = GeoFenceManager.valueAt(i);

            removeUnitFromGeoFence (geoFenceBase,AndruavSettings.andruavWe7daBase);
        }

        mGeoFenceBaseArray.clear();
        // TODO: Send Event Here to update MAP
    }

    /***
     * Remove fence object and sendMessageToModule deattach
     * @param geoFenceName
     */
    public static void removeGeoFence (final String geoFenceName)
    {
        if ((geoFenceName == null) || (geoFenceName.isEmpty()))
        {
            clearFenceData();
            return ;
        }

        final GeoFenceBase geoFenceBase = GeoFenceManager.get(geoFenceName);

        removeUnitFromGeoFence (geoFenceBase,AndruavSettings.andruavWe7daBase);

        mGeoFenceBaseArray.remove(geoFenceBase.fenceName);
        // TODO: Send Event Here to update MAP
    }

    /***
     * Remove a unit from a {@link GeoFenceBase} and sends deattach message {@link  AndruavFacade#sendGeoFenceAttach} with isAttach = false
     * @param geoFenceName
     * @param andruavUnitBase
     */
    public static void removeUnitFromGeoFence(final String geoFenceName, final AndruavUnitBase andruavUnitBase)
    {

        if ((geoFenceName != null) && (!geoFenceName.isEmpty())){
            final GeoFenceBase geoFenceBase = GeoFenceManager.get(geoFenceName);
            removeUnitFromGeoFence (geoFenceBase, andruavUnitBase);
        }
        else
        {
            final int size = GeoFenceManager.size();
            for (int i = 0 ; i < size; ++i)
            {
                final GeoFenceBase geoFenceBase   =GeoFenceManager.valueAt(i);

                removeUnitFromGeoFence (geoFenceBase, andruavUnitBase);

            }
        }
    }

    /**
     * Remove a unit from a {@link GeoFenceBase} and sends deattach message {@link  AndruavFacade#sendGeoFenceAttach} with isAttach = false
     * @param geoFenceBase
     * @param andruavUnitBase
     */
    public static void removeUnitFromGeoFence (final GeoFenceBase geoFenceBase, final AndruavUnitBase andruavUnitBase)
    {
        if (geoFenceBase == null) return ;

        geoFenceBase.mAndruavUnits.remove(andruavUnitBase.PartyID);
        if (andruavUnitBase.IsMe()) {
            AndruavFacade.sendGeoFenceAttach(geoFenceBase.fenceName, false, null);
        }
    }

    public static GeoFenceBase getGeoFence (final String geoFenceName)
    {
        return mGeoFenceBaseArray.get(geoFenceName);
    }

    public static void attachToGeoFence (final GeoFenceBase geoFenceBase, final AndruavUnitBase andruavUnitBase)
    {
        geoFenceBase.setisInsideRemote(andruavUnitBase);

        // Event that a Fence is Ready or a Drone is Attached to a Fence
        AndruavEngine.getEventBus().post(new Event_GeoFence_Ready(andruavUnitBase, geoFenceBase.fenceName)); // inform all that a data is ready

    }

    private static final int FenceViolation_NO =0b000;
    private static final int FenceViolation_IS_GOOD =0b100;
    private static final int FenceViolation_IS_BAD =0b010;
    private static final int FenceViolation_IS_OUT_OF_GOOD =0b001;

    /***
     * Testing all geo fences and create a final deceision about vilation.
     * This is not needed for Drone as drone takes its decision action per fence.
     * i.e. if it is violating a fence it will check its action and take it.
     * This function is important gor GCS so that it can judge if the overall status is ok.
     * Currently web uses a similar function. maybe later I will call this from drone only and
     * send result as final message status to web and other GCS.
     * I am violating iff:
     * <br>1- I am in a red zone.
     * <br>2- I am out of ALL Green zone(s) -when exist-
     * <br>I am Good iff:
     * <br>1- I am out of all red zones and no green zones.
     * <br>2- I am out of all red zones and in one of green zone(s) when exist.
     * <br>3- no fences to violate
     * <b>if I am out of both red and green then I consider this <i>VIOLATION</i></b>
     * @param andruavUnitBase
     * @return
     */
    public static boolean isViolatingGeoFence(final AndruavUnitBase andruavUnitBase)
    {
        int violation = 0;
        final int size = mGeoFenceBaseArray.size();

        for (int i = 0; i < size; ++i) {
            final GeoFenceBase geoFenceMapBase =  mGeoFenceBaseArray.valueAt(i);

            final boolean res = geoFenceMapBase.isViolatingGeoFence(andruavUnitBase);
            if (res && geoFenceMapBase.shouldKeepOutside) violation = violation | 0b010;      // fence violation & I am in a red zone. THIS IS BAD
            if (res && !geoFenceMapBase.shouldKeepOutside) violation = violation | 0b001;     // fence violation as I should be in or in another green one.
            if (!res && !geoFenceMapBase.shouldKeepOutside) violation = violation | 0b100;    // no fence violation & I should be in. This is GOOD
        }

        // I am in a red zone or red in green does not one bad is all bad.
        if ((violation & FenceViolation_IS_BAD) == FenceViolation_IS_BAD) return true; // violating

        // I am in green zoon only
        if (((violation & FenceViolation_IS_BAD) == FenceViolation_NO)
                &&  (
                //&& in one of green zone
                ((violation & FenceViolation_IS_GOOD) == FenceViolation_IS_GOOD)
                        ||  // or no green zones
                        (   (violation & FenceViolation_IS_OUT_OF_GOOD) == FenceViolation_NO)
        )
                )
            return false;

        // I am out of both red and green
        return ((violation & FenceViolation_IS_BAD) == FenceViolation_NO)
                && ((violation & FenceViolation_IS_OUT_OF_GOOD) == FenceViolation_IS_OUT_OF_GOOD);

    }


    /***
     * Sends my own attachement status to target
     * @param fenceName   can be NUll
     * @param target
     */
    public static void sendAttachedStatusToTarget (final String fenceName, final AndruavUnitBase target) {
        final int size = mGeoFenceBaseArray.size();

        for (int i = 0; i < size; ++i) {
            final GeoFenceBase geoFenceMapBase = mGeoFenceBaseArray.valueAt(i);

            if ((fenceName == null) || (fenceName.isEmpty()) || (fenceName.equals(geoFenceMapBase.fenceName))) {

                if (mGeoFenceBaseArray.containsKey(AndruavSettings.andruavWe7daBase.PartyID)) {
                    AndruavFacade.sendGeoFenceAttach(geoFenceMapBase.fenceName, true, target);
                }
            }

        }
    }


    /***
     *  Updates Status of fences for this drone.
     *  @return Event {@link Event_GeoFence_Hit} will be triggered in case of change status.
     */
    public static void updateGeoFenceHit (final AndruavUnitBase andruavUnitBase) {
        try {

            final Location loc  = andruavUnitBase.getAvailableLocation();

            if (loc==null) return ;
            final int size = mGeoFenceBaseArray.size();

            for (int i = 0; i < size; ++i) {

                final GeoFenceBase geoFenceMapBase = mGeoFenceBaseArray.valueAt(i);
                final double dis = geoFenceMapBase.testPoint(andruavUnitBase,loc.getLatitude(), loc.getLongitude(), true);
            }

        } catch (Exception e) {
            // just in case the array changed
        }
    }


    /***
     * Check status with a single fence. Result is not a final result for drone.
     * The rule is if the fence is "must not leave" and drone is outside then it is bad.
     * If drone is "must be out" and drone is inside then it is bad.
     *
     * @param inZone
     * @param shouldKeepOut
     * @return
     */
    private static  boolean isBadFencing (final boolean inZone, final boolean shouldKeepOut)
    {
        final boolean res = (inZone && shouldKeepOut) ||  (!inZone  && !shouldKeepOut);
        return res;
    }


//    /**
//     * Check all fences and take actions.
//     * @param andruavWe7daBase
//     */
//    public static void determineFenceValidationAction(final AndruavWe7daBase andruavWe7daBase)
//    {
//        final int size = GeoFenceManager.size();
//
//        for (int i = 0; i < size; ++i) {
//            final GeoFenceBase geoLinearFenceMapBase = GeoFenceManager.valueAt(i);
//
//
//            final _7adath_GeoFence_Hit geoFence_hit = geoLinearFenceMapBase.mAndruavUnits.get(andruavWe7daBase.PartyID);
//
//            if (geoFence_hit.hasValue)
//            {
//                // potential bug here... what if I violated multiple hard fences what action should I talk.
//                // but this should not geo fences are designed bad.
//                determineFenceValidationAction(geoFence_hit.inZone, geoFence_hit.shouldKeepOutside, geoLinearFenceMapBase);
//            }
//        }
//    }

    /***
     * Handle fence violtion actions by Me.
     * @param inZone
     * @param shouldKeepOutside
     * @param geoFenceBase
     */
    public static void determineFenceValidationAction(final boolean inZone, final boolean shouldKeepOutside, final GeoFenceBase geoFenceBase)
    {
        final boolean badFencing = GeoFenceManager.isBadFencing(inZone, shouldKeepOutside);

        boolean ignorePanic = false;
        if (badFencing) {
            // actions are taken when violating rules.
            switch (geoFenceBase.hardFenceAction) {
                case GeoFenceBase.ACTION_SOFT_FENCE:
                    // Soft Fence  DO NOTHING
                    break;
                case FlightMode.CONST_FLIGHT_CONTROL_RTL:
                case FlightMode.CONST_FLIGHT_CONTROL_SMART_RTL:
                case FlightMode.CONST_FLIGHT_CONTROL_BRAKE:
                case FlightMode.CONST_FLIGHT_CONTROL_LOITER:
                    if (badFencing & AndruavSettings.andruavWe7daBase.useFCBIMU()) {
                        AndruavSettings.andruavWe7daBase.FCBoard.do_Mode(geoFenceBase.hardFenceAction, null);
                    }
                    break;
                case FlightMode.CONST_FLIGHT_CONTROL_LAND:
                    if (badFencing & AndruavSettings.andruavWe7daBase.useFCBIMU()) {
                        AndruavSettings.andruavWe7daBase.FCBoard.do_Land(null);
                    }
                    break;
                case GeoFenceBase.ACTION_SHUT_DOWN:
                    ignorePanic = true;

                    break;
            }
        }

        if (!ignorePanic) {

            PanicFacade.hitGEOFence(geoFenceBase.fenceName, badFencing);
        }
    }
}