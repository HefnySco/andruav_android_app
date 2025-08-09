package ap.andruav_ap.communication.telemetry;

import android.content.Context;
import android.content.ContextWrapper;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.widget.Toast;

import com.andruav.AndruavEngine;
import com.andruav.AndruavFacade;
import com.andruav.AndruavSettings;
import com.andruav.notification.PanicFacade;
import com.andruav.TelemetryProtocol;
import com.andruav.interfaces.INotification;
import com.andruav.protocol.commands.textMessages.AndruavMessage_Error;

import de.greenrobot.event.EventBus;
import ap.andruav_ap.guiEvent.GUIEvent_UpdateConnection;
import ap.andruav_ap.activities.fcb.drone.FCB_AndruavShashaL2;
import ap.andruav_ap.App;
import ap.andruav_ap.communication.telemetry.DroneKit.DroneKitServer;
import ap.andruav_ap.DeviceManagerFacade;
import ap.andruavmiddlelibrary.eventClasses.remoteControl.Event_ProtocolChanged;
import ap.andruavmiddlelibrary.preference.Preference;
import ap.andruav_ap.R;
import ap.andruavmiddlelibrary.factory.util.DialogHelper;
import ap.andruavmiddlelibrary.factory.util.ProgressDialogHelper;

/**
 * Created by mhefny on 1/22/16.
 */
public class TelemetryModeer {


    public static boolean calledOnce = false;
    public static final int CURRENTCONNECTION_NON   =0;
    public static final int CURRENTCONNECTION_BT    =1;
    public static final int CURRENTCONNECTION_USB   =2;
    public static final int CURRENTCONNECTION_3DR   =3;
    public static final int CURRENTCONNECTION_UDP   =5;  // native
    public static final int CURRENTCONNECTION_TCP   =6;  // native
    //public final int CURRENTCONNECTION_BT=5;

    protected static boolean mIsConnecting = false;
    protected static int mCurrentConnection = CURRENTCONNECTION_NON;

    protected static  Context mContext;
    static ContextWrapper mContextWrapper;

   private static int lastConnected = CURRENTCONNECTION_NON;

    public static boolean isConnecting ()
    {
        return mIsConnecting;
    }
    public static synchronized void setConnected (final int connected)
    {

        switch (connected)
        {
            case CURRENTCONNECTION_USB:
                PanicFacade.telemetryPanic(INotification.NOTIFICATION_TYPE_NORMAL, AndruavMessage_Error.ERROR_USBERROR, App.getAppContext().getString(ap.andruavmiddlelibrary.R.string.gen_usb_connected), null);

                break;

            case CURRENTCONNECTION_BT:
                PanicFacade.telemetryPanic(INotification.NOTIFICATION_TYPE_NORMAL, AndruavMessage_Error.ERROR_BLUETOOTH, App.getAppContext().getString(ap.andruavmiddlelibrary.R.string.gen_bluetooth_connected), null);

                break;

            case CURRENTCONNECTION_NON:


                switch (lastConnected)
                {

                    case CURRENTCONNECTION_UDP:
                        PanicFacade.telemetryPanic(INotification.NOTIFICATION_TYPE_ERROR, AndruavMessage_Error.ERROR_UDP, App.getAppContext().getString(ap.andruavmiddlelibrary.R.string.gen_udp_disabled), null);

                        break;

                    case CURRENTCONNECTION_BT:
                        PanicFacade.telemetryPanic(INotification.NOTIFICATION_TYPE_ERROR, AndruavMessage_Error.ERROR_BLUETOOTH, App.getAppContext().getString(ap.andruavmiddlelibrary.R.string.gen_bluetooth_disabled), null);

                        break;

                    case CURRENTCONNECTION_USB:
                        PanicFacade.telemetryPanic(INotification.NOTIFICATION_TYPE_ERROR, AndruavMessage_Error.ERROR_USBERROR, App.getAppContext().getString(ap.andruavmiddlelibrary.R.string.gen_usb_disconnected), null);

                        break;

                    case CURRENTCONNECTION_3DR:
                        PanicFacade.telemetryPanic(INotification.NOTIFICATION_TYPE_ERROR, AndruavMessage_Error.ERROR_3DR, "3DR Service Disconnected ", null);

                        break;

                }

                break;

            case CURRENTCONNECTION_3DR:
                PanicFacade.telemetryPanic(INotification.NOTIFICATION_TYPE_NORMAL, AndruavMessage_Error.ERROR_3DR, "3DR Service Connected", null);

                break;

        }

        lastConnected = connected;
        TelemetryModeer.mCurrentConnection =connected;
        if (connected == CURRENTCONNECTION_NON) {
            if (App.telemetryProtocolParser!= null)
            {
                App.telemetryProtocolParser.shutDown();
                App.telemetryProtocolParser = null;
            }
        }
        else
        {
            App.telemetryProtocolParser = new TelemetryDroneProtocolParser();
        }

        AndruavEngine.getEventBus().post(new GUIEvent_UpdateConnection(AndruavSettings.andruavWe7daBase));

    }


    public static  synchronized void startAutoConnection (final boolean enforceConnect)
    {
        if ((!AndruavSettings.andruavWe7daBase.getIsCGS())
                && (enforceConnect || Preference.isAutoFCBConnect(null))  // not GCS
                && (TelemetryModeer.getConnectionInfo() == TelemetryModeer.CURRENTCONNECTION_NON)  // No current FCB connection
                )
        {
            Toast.makeText(App.context, "Trying to Auto Connect to FCB", Toast.LENGTH_SHORT).show();
            calledOnce = true;
            TelemetryModeer.connectToPreferredConnection(App.context,true);
        }
        else
        {
            String str = "IGNORE For " + TelemetryModeer.getConnectionInfo();
            Toast.makeText(App.context, str, Toast.LENGTH_LONG).show();
        }
    }

    public static synchronized int getConnectionInfo()
    {
        return TelemetryModeer.mCurrentConnection;
    }


    /***
     * Connect to the selected connection based on preference settings defined in {@link FCB_AndruavShashaL2}
     * @param context
     * @param autoconnect if true then connect silently without dialog boxes.
     */
    public static void connectToPreferredConnection (final Context context, final boolean autoconnect)
    {
        mContext = context;
        mContextWrapper = new ContextWrapper(mContext);
        closeAllConnections();


        switch (Preference.getFCBTargetLib(null))
       {
           case Preference.FCB_LIB_NATIVE:
               switch (Preference.getFCBTargetComm(null))
               {
                   case Preference.FCB_COM_USB:
                       /*if (!DeviceManagerFacade.hasUSBHost()) return; // should not happen
                       connectUSB(autoconnect);*/
                       break;
                   case Preference.FCB_COM_BT:
                       final String mac = Preference.getFCBBlueToothMAC(null);
                       if ((mac!= null) && (!mac.isEmpty())) {
                           connectBlueTooth();
                       }
                       else
                       {
                           AndruavEngine.notification().Speak("No Bluetooth device has been selected");
                           try {
                               DialogHelper.doModalDialog(context, context.getString(ap.andruavmiddlelibrary.R.string.gen_connection), "No Bluetooth device has been selected", null);
                           }
                           catch (RuntimeException er)
                           {
                                // Bluetooth Auto Connect
                           }
                           catch (Exception e)
                           {
                                // Bluetooth Auto Connect

                               /*
                               android.view.WindowManager$BadTokenException:
                                            Unable to add window token null is not for an application at android.view.ViewRootImpl.setView(ViewRootImpl.java:882)
                                            at android.view.WindowManagerGlobal.addView(WindowManagerGlobal.java:342)
                                            at android.view.WindowManagerImpl.addView(WindowManagerImpl.java:97)
                                            at android.app.Dialog.show(Dialog.java:556) at android.app.AlertDialog$Builder.show(AlertDialog.java:1131)
                                            at rcmobile.andruavmiddlelibrary.Mosa3ed.util.DialogHelper.doModalDialog(SourceFile:30)
                                            at rcmobile.FPV.etesalat.Telemetry.TelemetryModeer.connectToPreferredConnection(SourceFile:200)
                                            at rcmobile.FPV.etesalat.Telemetry.TelemetryModeer.startAutoConnection(SourceFile:156)
                                             
                                */
                           }
                       }
                       break;

                   case Preference.FCB_COM_UDP:
//                            final int port = Integer.parseInt(Preference.getFCBDroneUDPServerPort(null));
//                            final String IP = Preference.getFCBDroneTCPServerIP(null);
//
//                       try {
//                           App.telemetryUDPServer = new TelemetryUDPServer(InetAddress.getByName(IP),port);
//                           App.telemetryUDPServer.init(null);
//                        } catch (Exception e) {
//                           e.printStackTrace();
//                       }
                       break;
                   case Preference.FCB_COM_TCP:
                   default:
                       DialogHelper.doModalDialog(context, context.getString(ap.andruavmiddlelibrary.R.string.gen_connection), context.getString(ap.andruavmiddlelibrary.R.string.andruav_error_connection_notsupported), null);
                       break;
               }
               break;

           case Preference.FCB_LIB_3DR:
               if (App.droneKitServer== null) {
                   App.droneKitServer = new DroneKitServer(context);
                   App.droneKitServer.init(); /////// <<<I moved if from after IF
               }
               else
               {
                   App.droneKitServer.shutDown();
                   App.droneKitServer = null;
               }



              break;

       }
       // should be set after closeAllConnections()
       mIsConnecting = true;
    }

    public static void closeAllConnections ()
    {
        closeBlueTooth();
        closeUSB();
        closeUDPServer();
        closeDJI();
        closeDroneKit();
        mIsConnecting = false;
    }


    public static void closeBlueTooth ()
    {
        if (TelemetryModeer.getConnectionInfo()!= CURRENTCONNECTION_BT) return ;

        if ((!DeviceManagerFacade.hasBlueTooth()) || (App.BT.Bluetooth==null)) return ;

        App.BT.StopPersistentConnection();
        App.BT.Bluetooth.cancelDiscovery();
        App.BT.Bluetooth.disable();

        TelemetryModeer.setConnected(CURRENTCONNECTION_NON);
        AndruavSettings.andruavWe7daBase.setTelemetry_protocol(TelemetryProtocol.TelemetryProtocol_No_Telemetry);

    }


    public static void closeUSB ()
    {
        /*if (TelemetryModeer.getConnectionInfo()!= CURRENTCONNECTION_USB) return ;

        if (!DeviceManagerFacade.hasUSBHost()) return ;*/

        /*if ((App.usbConn != null) && (App.usbConn.isRunning()))
        {
            App.stopUSBClient();
            TelemetryModeer.setConnected(CURRENTCONNECTION_NON);
        }

        if ((App.ftdiusbConn != null) && (App.ftdiusbConn.isRunning()))
        {
            App.stopFTDIUSBClient();
            TelemetryModeer.setConnected(CURRENTCONNECTION_NON);
        }*/
    }

    public static void closeUDPServer ()
    {
        /*if (App.telemetryUDPServer != null)
        {
            App.telemetryUDPServer.shutDownServer();
            TelemetryModeer.setConnected(CURRENTCONNECTION_NON);
            App.telemetryUDPServer = null;

        }*/
    }

    public static void closeDJI ()
    {
       /* if (TelemetryModeer.getConnectionInfo()!= CURRENTCONNECTION_DJI) return ;

        if (!DJIManager.isValidAndroidVersion()) return ;
       if (App.djiManager != null)
       {
           App.djiManager.UnInitSDK();
       }
        TelemetryModeer.setConnected(CURRENTCONNECTION_NON);
        */
    }


    public static void closeDroneKit ()
    {
        //if (TelemetryModeer.getConnectionInfo()!= CURRENTCONNECTION_3DR) return ;

        if ((DroneKitServer.isValidAndroidVersion()) && (App.droneKitServer!=null))
        {
            App.droneKitServer.shutDown();
            App.droneKitServer = null;
        }
        TelemetryModeer.setConnected(CURRENTCONNECTION_NON);

    }


    ////////////////////////////////////////// BLUETOOTH CONNECTION
    protected static void connectBlueTooth ()
    {

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                SystemClock.sleep(1000);

                try {
                    final String macAddress = Preference.getFCBBlueToothMAC(null);
                    App.BT.MakePersistentConnection(macAddress);
                    ProgressDialogHelper.exitProgressDialog();


                    return null;
                }
                catch (Exception ex)
                {
                    AndruavEngine.log().logException("BT",ex);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void params ) {
                try {
                    if (App.BT.Bluetooth.isConnected()) {
                        Preference.setFCBBlueToothMAC(null, Preference.getFCBBlueToothMAC(null));
                        AndruavSettings.andruavWe7daBase.setTelemetry_protocol(TelemetryProtocol.TelemetryProtocol_Unknown_Telemetry);  // reset telemetry
                        TelemetryModeer.setConnected(CURRENTCONNECTION_BT);
                        EventBus.getDefault().post(new Event_ProtocolChanged(true));
                        AndruavFacade.broadcastID();

                    } else {
                        AndruavSettings.andruavWe7daBase.setTelemetry_protocol(TelemetryProtocol.TelemetryProtocol_No_Telemetry);
                        TelemetryModeer.setConnected(CURRENTCONNECTION_NON);
                        EventBus.getDefault().post(new Event_ProtocolChanged(false));
                        AndruavFacade.broadcastID();


                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }

                ProgressDialogHelper.exitProgressDialog();

            }

            @Override
            protected void onPreExecute ()
            {
                try
                {
                    final boolean res = App.BT.Bluetooth.GetAdapter();
                    App.BT.Bluetooth.Enable();
                    if (!res) {
                        this.cancel(true);
                        DialogHelper.doModalDialog(mContext, mContext.getString(ap.andruavmiddlelibrary.R.string.gen_connection), "cannot start Bluetooth", null);
                        return;
                    }
                    ProgressDialogHelper.doProgressDialog(mContext,"Bluetooth");
                }
                catch (Exception ex)
                {
                    ex.printStackTrace();
                }
            }

        }.execute((Void) null);
    }

}
