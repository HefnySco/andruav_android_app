package ap.andruav_ap.communication.telemetry;

import android.content.Context;
import android.util.Log;

import com.andruav.AndruavEngine;
import com.andruav.AndruavSettings;
import com.andruav.notification.PanicFacade;
import com.andruav.interfaces.INotification;
import com.andruav.protocol.commands.textMessages.AndruavMessage_Error;

import ap.andruav_ap.guiEvent.GUIEvent_UpdateConnection;
import ap.andruav_ap.activities.fcb.drone.FCB_AndruavShashaL2;
import ap.andruav_ap.App;
import ap.andruav_ap.R;

/**
 * Created by mhefny on 1/22/16.
 * <br>
 * C6 (AN-0054): the Java FC link (DroneKitServer) is hidden in
 * {@code src/legacy/java} - this class keeps only the connection-state
 * surface the UI still reads. The FC link belongs to de_mavlink; the FCB
 * sheet changes it by saving preferences, which {@code DeConfigSync} turns
 * into a de_mavlink restart with new {@code --fcb_*} args.
 */
public class TelemetryModeer {


    public static boolean calledOnce = false;
    public static final int CURRENTCONNECTION_NON   =0;
    public static final int CURRENTCONNECTION_3DR   =3;

    protected static boolean mIsConnecting = false;
    protected static int mCurrentConnection = CURRENTCONNECTION_NON;

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
                        PanicFacade.telemetryPanic(INotification.NOTIFICATION_TYPE_ERROR, AndruavMessage_Error.ERROR_3DR, App.getAppContext().getString(R.string.panic_3dr_disconnected), null);

                        break;

                }

                break;

            case CURRENTCONNECTION_3DR:
                PanicFacade.telemetryPanic(INotification.NOTIFICATION_TYPE_NORMAL, AndruavMessage_Error.ERROR_3DR, App.getAppContext().getString(R.string.panic_3dr_connected), null);

                break;

        }

        lastConnected = connected;
        TelemetryModeer.mCurrentConnection =connected;

        AndruavEngine.getEventBus().post(new GUIEvent_UpdateConnection(AndruavSettings.andruavWe7daBase));

    }


    public static  synchronized void startAutoConnection (final boolean enforceConnect)
    {
        // C6 (AN-0054): no Java FC link to auto-connect. The native stack
        // opens the FC link from the saved FCB preferences on module start.
        calledOnce = true;
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
        // C6 (AN-0054): the FC belongs to de_mavlink (N4/N8) - the Java
        // DroneKitServer is gone. The caller's savePreferences() is what
        // matters: DeConfigSync regenerates the module config and restarts
        // de_mavlink with the new --fcb_* args.
        Log.d("TelemetryModeer", "connectToPreferredConnection ignored - FC link is owned by de_mavlink");
    }

    public static void closeAllConnections ()
    {
        // The native FC link is not torn down from here - it follows the
        // de_mavlink module lifecycle. Reset the legacy UI state only.
        mIsConnecting = false;
        TelemetryModeer.setConnected(CURRENTCONNECTION_NON);
    }


    public static void closeDroneKit ()
    {
        TelemetryModeer.setConnected(CURRENTCONNECTION_NON);
    }


}
