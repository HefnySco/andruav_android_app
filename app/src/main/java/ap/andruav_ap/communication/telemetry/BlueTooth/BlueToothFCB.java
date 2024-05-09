package ap.andruav_ap.communication.telemetry.BlueTooth;

import de.greenrobot.event.EventBus;
import ap.andruav_ap.App;

import com.andruav.AndruavEngine;
import com.andruav.event.fcb_event.Event_FCBData;
import com.andruav.interfaces.INotification;
import com.andruav.protocol.commands.textMessages.AndruavMessage_Error;

import ap.andruav_ap.communication.telemetry.IEvent_SocketData;
import com.andruav.event.fcb_event.Event_SocketData;
import com.andruav.event.systemEvent.Event_ShutDown_Signalling;
import com.andruav.notification.PanicFacade;

import ap.andruav_ap.R;

/**
 * Created by M.Hefny on 27-Nov-14.
 */
public class BlueToothFCB  implements IEvent_SocketData {


    protected final BlueToothFCB Me;
    protected Boolean mKillMe = true;
    public final Bluetooth Bluetooth;
    protected Thread threadBT;
    protected int log =0;
    protected final Event_FCBData mevent_FCBData = new Event_FCBData();

    protected long exception_BT_time_counter = 15000;
    protected long exception_BT_counter = 5;

    //////////BUS EVENT

    public void onEvent (final Event_ShutDown_Signalling event)
    {
        if (event.CloseOrder != 1) return ;


        this.shutDown();
        App.droneKitServer = null;
    }


    @Override
    public void onSocketData(final Event_SocketData event) {
        if ((!Bluetooth.isConnected()) ||(event.IsLocal == Event_SocketData.SOURCE_LOCAL)) return ;

        Bluetooth.Send(event.Data,event.DataLength);


    }

    /***
     * Data is received from Serial Socket This data could be from the server on the same andruav
     * or can be received from AndruavCommand {@link //Commands.BinaryMessages.AndruavResalaBinaryBase}
     *
     * Data from Remote GCS should be delivered directly to board, as in initialization Drone protocol
     * is set from the board reply only to ensure accuracy
     * @param event
     */
    public void onEvent (final Event_SocketData event)
    {
       // if ((!Bluetooth.isConnected()) ||(event.IsLocal == Event_SocketData.SOURCE_LOCAL)) return ;

       //  Bluetooth.Send(event.Data,event.DataLength);

        /*  Uncomment this for loop back Test
        mevent_FCBData.Data = event.Data;
        mevent_FCBData.DataLength = event.Data.length;
        mevent_FCBData.IsLocal = true;
        if (mKillMe)
            return; // if due to roundrobin the next line is executed it will give exception
        EventBus.getDefault().post(mevent_FCBData);
        */

    }


    ///////////////////

    protected void shutDown()
    {
        App.iEvent_socketData = null;
        EventBus.getDefault().unregister(this);

        StopPersistentConnection();
    }

    public Boolean isConnected ()
    {
        if (Bluetooth==null) return false;
        return Bluetooth.isConnected();
    }

    public Boolean isRunning()
    {
        return (!mKillMe);
    }

    public BlueToothFCB ()
    {
        Me = this;
        Bluetooth = new Bluetooth(App.context);

    }



    public void StopPersistentConnection ()
    {

        mKillMe = true;
        if (threadBT != null) {
            try {

                threadBT.join();
            } catch (InterruptedException ex) {
                AndruavEngine.log().logException("exception_BT", ex);
            }
            finally {
                threadBT = null;
            }
        }

    }
    public void MakePersistentConnection (final String address)
    {

        mKillMe = false;
        log = 5;
        threadBT = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mKillMe = false;

                    App.iEvent_socketData = Me;
                    if (!EventBus.getDefault().isRegistered(Me)) {
                        // because this function [MakePersistentConnection] is called twice
                        EventBus.getDefault().register(Me);
                    }
                    //EventBus.getDefault().register(Me, "onEventSerialSocket", Event_SocketData.class);
                    //Preference.setFCBBlueToothMAC(null, address);
                    //String serr =App.getAppContext().getString(R.string.gen_bluetooth_connected);
                    //App.notification.displayNotification(INotification.NOTIFICATION_TYPE_NORMAL, "Status", serr, true, INotification.INFO_TYPE_TELEMETRY, false);
                    //AndruavFacade.sendErrorMessage(INotification.INFO_TYPE_TELEMETRY, INotification.NOTIFICATION_TYPE_NORMAL, AndruavResala_Error.ERROR_BLUETOOTH, serr, null);
                    //TTS.getInstance().Speak(App.getAppContext().getString(R.string.gen_bluetooth_connected));
                    PanicFacade.telemetryPanic(INotification.NOTIFICATION_TYPE_NORMAL, AndruavMessage_Error.ERROR_BLUETOOTH, App.getAppContext().getString(R.string.gen_bluetooth_connected), null);
                    while (!mKillMe) {
                        Thread.sleep(1, 0);
                        int i = Bluetooth.available();
                        if (i > 0) {
                            byte[] b = Bluetooth.ReadFrame(i);
                            mevent_FCBData.Data = b;
                            mevent_FCBData.DataLength = i;
                            mevent_FCBData.IsLocal = Event_SocketData.SOURCE_LOCAL;
                            if (mKillMe)
                                return; // if due to roundrobin the next line is executed it will give exception
                            EventBus.getDefault().post(mevent_FCBData);

                        }
                        else
                        {
                            if (i==-1)
                            {
                                if (log > 0) { // log once
                                    //serr =App.getAppContext().getString(R.string.andruav_error_bluetootherror);
                                    //App.notification.displayNotification(INotification.NOTIFICATION_TYPE_ERROR, "Error", serr, true, 3, false);
                                    //AndruavFacade.sendErrorMessage(INotification.INFO_TYPE_TELEMETRY, INotification.NOTIFICATION_TYPE_ERROR, AndruavResala_Error.ERROR_BLUETOOTH, serr, null);
                                    //TTS.getInstance().Speak(serr);
                                    PanicFacade.telemetryPanic(INotification.NOTIFICATION_TYPE_ERROR, AndruavMessage_Error.ERROR_BLUETOOTH, App.getAppContext().getString(R.string.andruav_error_bluetootherror), null);

                                    log -=1;
                                }
                            }
                        }
                    }

                }catch (Exception ex)
                {
                    long now = System.currentTimeMillis();
                    if ((exception_BT_counter>0) && ((now - exception_BT_time_counter) > 15000)) {
                        exception_BT_time_counter = now;
                        exception_BT_counter = exception_BT_counter -1;
                        //String  serr =App.getAppContext().getString(R.string.andruav_error_bluetootherror);

                        //App.notification.displayNotification(INotification.NOTIFICATION_TYPE_ERROR, "Status", serr, true, INotification.INFO_TYPE_TELEMETRY, false);
                        //AndruavFacade.sendErrorMessage(INotification.INFO_TYPE_TELEMETRY, INotification.NOTIFICATION_TYPE_ERROR, AndruavResala_Error.ERROR_BLUETOOTH, serr, null);
                        //TTS.getInstance().Speak(App.getAppContext().getString(R.string.andruav_error_bluetootherror));
                        PanicFacade.telemetryPanic(INotification.NOTIFICATION_TYPE_ERROR, AndruavMessage_Error.ERROR_BLUETOOTH, App.getAppContext().getString(R.string.andruav_error_bluetootherror), null);



                        AndruavEngine.log().logException("exception_BT", ex);
                    }

                }
                finally {
                    App.iEvent_socketData = null;
                    Bluetooth.CloseSocket();
                    //ToDo enh - sendMessageToModule Bluetooth disconnected here.

                    EventBus.getDefault().unregister(Me);
                    //String serr =App.getAppContext().getString(R.string.andruav_error_bluetootherror);
                    //App.notification.displayNotification(INotification.NOTIFICATION_TYPE_ERROR, "Error", serr, true, INotification.INFO_TYPE_TELEMETRY, false);
                    //AndruavFacade.sendErrorMessage(INotification.INFO_TYPE_TELEMETRY, INotification.NOTIFICATION_TYPE_ERROR, AndruavResala_Error.ERROR_BLUETOOTH, serr, null);
                    //TTS.getInstance().Speak(serr);
                    PanicFacade.telemetryPanic(INotification.NOTIFICATION_TYPE_ERROR, AndruavMessage_Error.ERROR_BLUETOOTH, App.getAppContext().getString(R.string.andruav_error_bluetootherror), null);

                }
            }
        }
        );
        Bluetooth.Connect(address);

        if (!Bluetooth.isConnected()) {
            AndruavEngine.notification().Speak(App.getAppContext().getString(R.string.err_bluetooth_cannotconnect));
            return;
        }
        threadBT.start();
    }
}
