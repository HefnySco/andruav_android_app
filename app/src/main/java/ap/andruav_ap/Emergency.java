package ap.andruav_ap;

import android.location.Location;
import android.widget.Toast;

import com.andruav.AndruavDroneFacade;
import com.andruav.AndruavEngine;
import com.andruav.AndruavSettings;
import com.andruav.EmergencyBase;
import com.andruav.interfaces.INotification;
import com.andruav.controlBoard.IControlBoard_Callback;
import com.andruav.notification.PanicFacade;
import com.andruav.protocol.commands.textMessages.AndruavMessage_Error;

import ap.andruav_ap.guiEvent.GUIEvent_EnableFlashing;
import com.andruav.FeatureSwitch;
import com.andruav.protocol.communication.websocket.AndruavWSClientBase;

import ap.andruavmiddlelibrary.preference.Preference;
import ap.andruavmiddlelibrary.factory.DeviceFeatures;
import ap.andruavmiddlelibrary.factory.communication.SMS;

/**
 * Created by M.Hefny on 11-Nov-14.
 *
 */
public class Emergency extends EmergencyBase {



    public void triggerBatteryEmergency(final boolean bOnOff) {
       if ((mBatteryEmergency != bOnOff )&& (bOnOff==true))
        {
            PanicFacade.telemetryPanic(INotification.NOTIFICATION_TYPE_ERROR, AndruavMessage_Error.ERROR_POWER, App.getAppContext().getString(com.andruav.protocol.R.string.andruav_error_lowbattery), null);
        }
        super.triggerBatteryEmergency(bOnOff);

    }

    /***
     * Change Flight mode to predefined when emergency
     * @param ignoreTiming
     */
    public  void triggerEmergencyFlightModeFaileSafe(final boolean ignoreTiming)
    {

        final int flightMode =  Preference.isEmergencyFlightModeFailSafeEnabled(null);

        if (flightMode ==0 )
        {
            return ;
        }



        if (!ignoreTiming) {
            if (mFirstCall_triggerEmergencyChangeFlightMode ==0)
            {
                mFirstCall_triggerEmergencyChangeFlightMode = System.currentTimeMillis();

                return ;
            }
            else
            {
                final long now = System.currentTimeMillis();
                if ((now - mFirstCall_triggerEmergencyChangeFlightMode) >= mRTLTrigger)
                {
                    mFirstCall_triggerEmergencyChangeFlightMode = now;
                }
                else
                {
                    return ;
                }
            }
        }

        if (AndruavSettings.andruavWe7daBase.isControllable())
        {

            if (AndruavSettings.andruavWe7daBase.getFlightModeFromBoard() == flightMode)
            {
                AndruavSettings.andruavWe7daBase.setIsEmergencyChangeFlightModeFailSafe(true);
            }
            else {

                final IControlBoard_Callback iControlBoard_callback = new IControlBoard_Callback() {
                    @Override
                    public void OnSuccess() {
                        AndruavSettings.andruavWe7daBase.setIsEmergencyChangeFlightModeFailSafe(true);

                    }

                    @Override
                    public void OnFailue(int code) {
                        App.mScheduleHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                triggerEmergencyFlightModeFaileSafe(true); // ignore timing
                            }
                        }, 12000);
                    }

                    @Override
                    public void OnTimeout() {
                        App.mScheduleHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                triggerEmergencyFlightModeFaileSafe(true); // ignore timing
                            }
                        }, 10000);
                    }
                };


                AndruavSettings.andruavWe7daBase.FCBoard.do_Mode(Preference.isEmergencyFlightModeFailSafeEnabled(null), iControlBoard_callback);

            }
        }

    }




    public  boolean getIsSirenActive()
    {
        return App.soundManager.isSirenOn();
    }


    public  boolean getIsFlashing ()
    {
        return AndruavSettings.andruavWe7daBase.getIsFlashing();
    }


    public  void playSiren (final boolean active,final boolean ignorePermission)
    {
        if (!active &&
                (mTriggerSerienFromGCS || mBatteryEmergency || mConnectionEmergency))
        {
            // someone trying to shutdown while there is an alarm
            return ;
        }

        if (!ignorePermission) {
            if (!Preference.isEmergencySirenEnabled(null)) {  // only obey when emergency
                return;
            }
        }

//        if (!ignoreTiming) {
//
//            if (mFirstCall_triggerEmergencySiren ==0)
//            {
//                mFirstCall_triggerEmergencySiren = System.currentTimeMillis();
//
//                return ;
//            }
//            else
//            {
//                final long now = System.currentTimeMillis();
//                if ((now - mFirstCall_triggerEmergencySiren) >= mSirenTrigger)
//                {
//                    mFirstCall_triggerEmergencySiren = now;
//                }
//                else
//                {
//                    return ;
//                }
//            }
//        }

        if (active)
        {
            App.soundManager.playSiren();
        }
        else
        {
            App.soundManager.stopSiren();
        }



    }


    protected  void doFlash (final boolean enable,final boolean ignorePermission)
    {

        if (!enable &&
                (mTriggerFlashFromGCS || mBatteryEmergency || mConnectionEmergency))
        {
            // someone trying to shutdown while there is an alarm
            return ;
        }

        if (AndruavSettings.andruavWe7daBase.getIsFlashing() == enable)
        {
            return; // do nothing
        }

        if (!ignorePermission) {
            if (!Preference.isEmergencyFlashEnabled(null)) {  // only obey when emergency
                return;
            }
        }

//        if (!ignoreTiming) {
//
//
//
//            if (mFirstCall_triggerEmergencySiren ==0)
//            {
//                mFirstCall_triggerEmergencyFlash = System.currentTimeMillis();
//
//                return ;
//            }
//            else
//            {
//                final long now = System.currentTimeMillis();
//                if ((now - mFirstCall_triggerEmergencyFlash) >= mFlashTrigger)
//                {
//                    mFirstCall_triggerEmergencyFlash = now;
//                }
//                else
//                {
//                    return ;
//                }
//            }
//        }


        AndruavEngine.getEventBus().post(new GUIEvent_EnableFlashing (enable));

    }
    /***
     * Handles sending SMS to destination for recovery
     * it keeps track of available locations and sendMessageToModule SMS whenever reading is available or better accuracy.
     */
    public  void sendSMS (final boolean ignoreTiming)
    {
        try {

            if (!ignoreTiming && (AndruavEngine.getAndruavWSStatus() == AndruavWSClientBase.SOCKETSTATE_REGISTERED))
            {
                return;
            }

            if (Preference.isEmergencySMSGPSEnabled(null)==false)
            {
                return ; // ModuleFeatures is disabled by user.
            }

            if (ignoreTiming == false)
            {
                final long now = System.currentTimeMillis();
                if (mlatestSMSTime == 0)
                {
                    mlatestSMSTime = now;
                    return ;
                }
                if ((now - mlatestSMSTime) >=mSMSSeparationPeriod)
                {
                    // sendMessageToModule multiple SMS with separation mSMSSeparationPeriod
                    mlatestSMSTime = now;
                    mSMSSeparationPeriod = mSMSSeparationPeriod2;
                }
                else
                {
                    return ;
                }

            }
            sendActualSMS();


        }
        catch (Exception e)
        {
            AndruavEngine.log().logException(AndruavSettings.andruavWe7daBase.UnitID,"exception",e);
        }
    }

    private  void sendActualSMS () {

        Location loc = AndruavDroneFacade.getLastKnownLocation();

        if (loc == null) {
            Toast.makeText(App.getAppContext(),
                    "No Location to sendMessageToModule in SMS", Toast.LENGTH_LONG).show();
            return;
        }

        String msg = "http://maps.google.com/?q=" + loc.getLatitude() + "," + loc.getLongitude() + " \r\n " + " acc:" + loc.getAccuracy() + "m";
        // String msgold = "lng:" + loc.getLongitude() + " lat:" + loc.getLatitude() + " acc:" + loc.getAccuracy() + "m";

        String phoneNo = Preference.getRecoveryPhoneNo(null);
        if (phoneNo.length() == 0) {
            // this can be used to support ppl to find their lost planes.
            AndruavEngine.log().log(AndruavSettings.andruavWe7daBase.UnitID + Preference.getLoginAccessCode(null), "Lost Vehicle", msg);
            return; // no valid phone
        }


        if (DeviceFeatures.hasSMSCapabilities) {

            Toast.makeText(App.getAppContext(),
                    "Sending Location SMS", Toast.LENGTH_LONG).show();


            if (FeatureSwitch.DEBUG_MODE == false) {
                // dont cost me money
                //SMS.sendSMS(Preference.getRecoveryPhoneNo(null), msgold);
                SMS.sendSMS(Preference.getRecoveryPhoneNo(null), msg);
            }

        }
        else
        {
            Toast.makeText(App.getAppContext(),
                    "Phone does not have SMS Capabilities", Toast.LENGTH_LONG).show();

        }

    }
}