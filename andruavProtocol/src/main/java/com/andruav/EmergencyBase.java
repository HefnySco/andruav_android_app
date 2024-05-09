package com.andruav;

/**
 * Created by mhefny on 2/27/17.
 */

public abstract class EmergencyBase {



    protected  long mlatestSMSTime;
    protected static final long mSMSSeparationPeriod1 = 45000; // each 45 sec
    protected static final long mSMSSeparationPeriod2 = 180000; // each 3 min
    protected static long mSMSSeparationPeriod = mSMSSeparationPeriod1;
    protected static final long mRTLTrigger = 45000; // each 30 min
    protected static final long mFlashTrigger = 30000; // each 30 sec
    protected static final long mSirenTrigger = 30000; // each 30 sec


    protected  long mFirstCall_triggerEmergencyChangeFlightMode =0;
    protected  long mFirstCall_triggerEmergencyFlash =0;
    protected  long mFirstCall_triggerEmergencySiren =0;
    protected  boolean mBatteryEmergency = false;
    protected  boolean mConnectionEmergency = false;


    protected  boolean mTriggerSerienFromGCS = false;
    protected  boolean mTriggerFlashFromGCS = false;


    /***
     * Called when websocket fails.
     * @param bOnOff
     */
    public void triggerConnectionEmergency(final boolean bOnOff)
    {
        mConnectionEmergency = bOnOff;
        if (bOnOff)
        {
            playSiren(true,false);
            doFlash(true,false);
        }
        else
        {
            playSiren(false,false);
            doFlash(false,false);
        }
    }

    /***
     * Called when battery fails
     * @param bOnOff
     */
    public void triggerBatteryEmergency(final boolean bOnOff)
    {
        mBatteryEmergency = bOnOff;
        if (bOnOff)
        {

            playSiren(true,false);
            doFlash(true,false);
        }
        else
        {
            playSiren(false,false);
            doFlash(false,false);
        }
    }

    /***
     * Called by another GCS.
     * @param onOff
     */
    public void triggerFlashByGCS(final boolean onOff)
    {
        mTriggerFlashFromGCS = onOff;

        if (!onOff && mBatteryEmergency)
        {
            // assuming GCS want to shut down alarms.
            mBatteryEmergency = false;
        }
        doFlash(onOff,true);
    }

    /***
     * Called by another GCS
     * @param onOff
     */
    public void triggerSirenByGCS(final boolean onOff)
    {
        mTriggerSerienFromGCS = onOff;
        if (!onOff && mBatteryEmergency)
        {
            // assuming GCS want to shut down alarms.
            mBatteryEmergency = false;
        }
        playSiren(onOff,true);
    }

    public  void resetTimers ()
    {
        resetFlashTimer();
        resetRTLTimer();
        resetSirenTimer();
        resetSMSTimer();
    }


    public  void resetEmergency()
    {
        doFlash(false,true);
        playSiren(false,true);

        mBatteryEmergency = false;
        mTriggerSerienFromGCS = false;
        mTriggerFlashFromGCS = false;
        mConnectionEmergency = false;

    }

    public  void resetSMSTimer ()
    {
        mlatestSMSTime = 0;

    }


    public  void resetRTLTimer ()
    {
        mFirstCall_triggerEmergencyChangeFlightMode = 0;

    }

    public  void resetFlashTimer ()
    {
        mFirstCall_triggerEmergencyFlash = 0;

    }

    public  void resetSirenTimer ()
    {
        mFirstCall_triggerEmergencySiren = 0;

    }

    public  abstract void triggerEmergencyFlightModeFaileSafe(final boolean ignoreTiming);
    public  abstract boolean getIsSirenActive();
    public  abstract boolean getIsFlashing ();
    protected  abstract void playSiren (final boolean active,final boolean ignorePermission);
    protected  abstract void doFlash (final boolean enable,final boolean ignorePermission);
    public  abstract void sendSMS (final boolean ignoreTiming);

    public  abstract void sendSMSLocation (final String receiver_num);

}
