package ap.andruav_ap.communication.telemetry;

import android.content.Context;
import android.content.ContextWrapper;
import android.widget.Toast;

import com.andruav.AndruavEngine;
import com.andruav.AndruavSettings;
import com.andruav.notification.PanicFacade;
import com.andruav.interfaces.INotification;
import com.andruav.protocol.commands.textMessages.AndruavMessage_Error;

import ap.andruav_ap.guiEvent.GUIEvent_UpdateConnection;
import ap.andruav_ap.activities.fcb.drone.FCB_AndruavShashaL2;
import ap.andruav_ap.App;
import ap.andruav_ap.communication.telemetry.DroneKit.DroneKitServer;
import ap.andruavmiddlelibrary.preference.Preference;

/**
 * Created by mhefny on 1/22/16.
 */
public class TelemetryModeer {


    public static boolean calledOnce = false;
    public static final int CURRENTCONNECTION_NON   =0;
    public static final int CURRENTCONNECTION_3DR   =3;

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
            case CURRENTCONNECTION_NON:


                switch (lastConnected)
                {

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

        if (App.droneKitServer== null) {
            App.droneKitServer = new DroneKitServer(context);
            App.droneKitServer.init(); /////// <<<I moved if from after IF
        }
        else
        {
            App.droneKitServer.shutDown();
            App.droneKitServer = null;
        }

       // should be set after closeAllConnections()
       mIsConnecting = true;
    }

    public static void closeAllConnections ()
    {
        closeDroneKit();
        mIsConnecting = false;
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


}
