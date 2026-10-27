package ap.andruav_ap;

import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.andruav.AndruavDroneFacade;
import com.andruav.AndruavEngine;
import com.andruav.AndruavSettings;
import com.andruav.EmergencyBase;
import com.andruav.interfaces.INotification;
import com.andruav.controlBoard.IControlBoard_Callback;
import com.andruav.notification.PanicFacade;
import com.andruav.protocol.commands.textMessages.AndruavMessage_Error;
import com.andruav.protocol.commands.textMessages.Control.AndruavMessage_RemoteExecuteResult;

import ap.andruav_ap.guiEvent.GUIEvent_EnableFlashing;
import ap.andruav_ap.denative.NativeUnitController;
import ap.andruav_ap.denative.providers.phone.PhoneDevices;
import ap.andruav_ap.denative.providers.phone.PhoneOutput;
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


    /**
     * Which surface the in-flight flash request came from — reported as the
     * {@code src} of the {@code phone.screen.went_on|went_off} facts:
     * a GCS MAKEFLASH is {@code "legacy"}, battery/connection emergencies
     * and the emergency reset are {@code "emergency"}.
     */
    private String mFlashSrc = PhoneOutput.SRC_LEGACY;

    @Override
    public void triggerConnectionEmergency(final boolean bOnOff) {
        mFlashSrc = PhoneOutput.SRC_EMERGENCY;
        super.triggerConnectionEmergency(bOnOff);
    }

    @Override
    public void triggerFlashByGCS(final boolean onOff) {
        mFlashSrc = PhoneOutput.SRC_LEGACY;
        super.triggerFlashByGCS(onOff);
    }

    @Override
    public void resetEmergency() {
        mFlashSrc = PhoneOutput.SRC_EMERGENCY;
        super.resetEmergency();
    }

    public void triggerBatteryEmergency(final boolean bOnOff) {
        mFlashSrc = PhoneOutput.SRC_EMERGENCY;
       if ((mBatteryEmergency != bOnOff )&& (bOnOff))
        {
            PanicFacade.telemetryPanic(INotification.NOTIFICATION_TYPE_ERROR, AndruavMessage_Error.ERROR_POWER, App.getAppContext().getString(com.andruav.protocol.R.string.andruav_error_lowbattery), null);
        }
        super.triggerBatteryEmergency(bOnOff);

    }

    /***
     * Change Flight mode to predefined when emergency.
     * <p>
     * C4e/F5 (AN-0072): in native mode the comm-loss flight-mode failsafe
     * moved into de_mavlink (config {@code failsafe_on_comm_lost}, C4g/F6)
     * so it still fires with the app process dead - Java no longer owns
     * the trigger and sends no mode change for comm-loss. The legacy
     * {@code FCBoard.do_Mode} path below stays for the Java stack.
     * @param ignoreTiming
     */
    public  void triggerEmergencyFlightModeFaileSafe(final boolean ignoreTiming)
    {
        if (NativeUnitController.isNativeMode())
        {
            // de_mavlink fires the configured failsafe on comm-off itself.
            return ;
        }

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
        final PhoneOutput screen = PhoneDevices.output("screen");
        if (screen != null)
        {
            // the phone.screen choke point owns the truth: GCS MAKEFLASH,
            // missions and emergencies share it
            return screen.isOn();
        }
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

        // vibrate follows the siren (B4): the phone.vibrate choke point is
        // shared with the bus provider; src "emergency" marks the surface.
        final PhoneOutput vibrate = PhoneDevices.output("vibrate");
        if (vibrate != null)
        {
            vibrate.write(active, PhoneOutput.SRC_EMERGENCY);
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

        final PhoneOutput screen = PhoneDevices.output("screen");
        final boolean flashing = (screen != null)
                ? screen.isOn()
                : AndruavSettings.andruavWe7daBase.getIsFlashing();
        if (flashing == enable)
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


        if (screen != null)
        {
            // the phone.screen choke point (strobe Activity) — shared with
            // missions and bus events, emitting went_on/went_off {src}
            screen.write(enable, mFlashSrc);
        }
        else
        {
            // fallback for the window before the phone outputs exist
            AndruavEngine.getEventBus().post(new GUIEvent_EnableFlashing (enable));
        }

    }
    /***
     * Handles sending SMS to destination for recovery
     * it keeps track of available locations and sendMessageToModule SMS whenever reading is available or better accuracy.
     */
    public  int sendSMS (final boolean ignoreTiming)
    {
        try {

            if (!ignoreTiming && (AndruavEngine.getAndruavWSStatus() == AndruavWSClientBase.SOCKETSTATE_REGISTERED))
            {
                AndruavEngine.log().log2(AndruavSettings.andruavWe7daBase.UnitID, "sms_skip", "sendSMS skipped: WS registered");
                return AndruavMessage_RemoteExecuteResult.RESULT_REJECTED;
            }

            if (!ignoreTiming && !Preference.isSMSTXEnabled(null))
            {
                AndruavEngine.log().log2(AndruavSettings.andruavWe7daBase.UnitID, "sms_skip", "sendSMS skipped: SMS TX disabled");
                return AndruavMessage_RemoteExecuteResult.RESULT_REJECTED; // ModuleFeatures is disabled by user.
            }

            if (!ignoreTiming)
            {
                final long now = System.currentTimeMillis();
                if (mlatestSMSTime == 0)
                {
                    mlatestSMSTime = now;
                    AndruavEngine.log().log2(AndruavSettings.andruavWe7daBase.UnitID, "sms_skip", "sendSMS skipped: first-call timing gate");
                    return AndruavMessage_RemoteExecuteResult.RESULT_REJECTED;
                }
                if ((now - mlatestSMSTime) >=mSMSSeparationPeriod)
                {
                    // sendMessageToModule multiple SMS with separation mSMSSeparationPeriod
                    mlatestSMSTime = now;
                    mSMSSeparationPeriod = mSMSSeparationPeriod2;
                }
                else
                {
                    AndruavEngine.log().log2(AndruavSettings.andruavWe7daBase.UnitID, "sms_skip", "sendSMS skipped: too-soon timing gate (now-last=" + (now - mlatestSMSTime) + "ms < " + mSMSSeparationPeriod + "ms)");
                    return AndruavMessage_RemoteExecuteResult.RESULT_REJECTED;
                }

            }
            String sms_target = Preference.getRecoveryPhoneNo(null);
            if (sms_target.isEmpty()) {
                AndruavEngine.log().log2(AndruavSettings.andruavWe7daBase.UnitID, "sms_skip", "sendSMS skipped: recovery phone number is empty");
                return AndruavMessage_RemoteExecuteResult.RESULT_NO_RECIPIENT;
            } else {
                return sendSMSLocation(sms_target, ignoreTiming);
            }


        }
        catch (Exception e)
        {
            AndruavEngine.log().logException(AndruavSettings.andruavWe7daBase.UnitID,"exception",e);
            return AndruavMessage_RemoteExecuteResult.RESULT_ERROR;
        }
    }


    public  int sendSMSLocation (final String receiver_num, final boolean b_forced) {
        try
        {
            if (!b_forced && !Preference.isSMSTXEnabled(null))
            {
                AndruavEngine.log().log2(AndruavSettings.andruavWe7daBase.UnitID, "sms_skip", "sendSMSLocation skipped: SMS TX disabled (to=" + receiver_num + ")");
                return AndruavMessage_RemoteExecuteResult.RESULT_REJECTED; // ModuleFeatures is disabled by user.
            }

            Location loc = AndruavDroneFacade.getLastKnownLocation();

            if (loc == null) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(App.getAppContext(),
                                "No Location to sendMessageToModule in SMS", Toast.LENGTH_LONG).show();
                    }
                });
                AndruavEngine.log().log2(AndruavSettings.andruavWe7daBase.UnitID, "sms_skip", "sendSMSLocation skipped: no location (to=" + receiver_num + ")");
                return AndruavMessage_RemoteExecuteResult.RESULT_NO_LOCATION;
            }

            String msg = "lat:" + loc.getLatitude() + ",lng:" + loc.getLongitude()
                    + " \r\n " + "alt:" + loc.getAltitude() + " m"
                    + " \r\n " + "spd:" + loc.getSpeed() + " m/s"
                    + " \r\n " + "acc:" + loc.getAccuracy() + " m";

            if (DeviceFeatures.hasSMSCapabilities) {

                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(App.getAppContext(),
                                "Sending Location SMS", Toast.LENGTH_LONG).show();
                    }
                });


                    return SMS.sendSMS(receiver_num, msg);
            }
            else
            {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(App.getAppContext(),
                                "Phone does not have SMS Capabilities", Toast.LENGTH_LONG).show();
                    }
                });
                AndruavEngine.log().log2(AndruavSettings.andruavWe7daBase.UnitID, "sms_skip", "sendSMSLocation skipped: no SMS capabilities (to=" + receiver_num + ")");
                return AndruavMessage_RemoteExecuteResult.RESULT_NO_CAPABILITY;
            }
        }
            catch (Exception e)
        {
            AndruavEngine.log().logException(AndruavSettings.andruavWe7daBase.UnitID,"exception",e);
            return AndruavMessage_RemoteExecuteResult.RESULT_ERROR;
        }
    }

}