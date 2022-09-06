package ap.andruav_ap.communication.telemetry.SerialSocketServer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Arrays;

import de.greenrobot.event.EventBus;
import ap.andruav_ap.App;

import com.andruav.AndruavFacade;
import com.andruav.AndruavEngine;
import com.andruav.AndruavSettings;
import com.andruav.Constants;
import com.andruav.TelemetryProtocol;

import com.andruav.event.fcb_event.Event_FCBData;

import com.andruav.event.fcb_event.Event_SocketAction;
import com.andruav.event.fcb_event.Event_SocketData;
import com.andruav.event.systemEvent.Event_ShutDown_Signalling;
import ap.andruav_ap.R;

/**
 * Created by M.Hefny on 25-Nov-14.
 */
public class AndruavGCSSerialSocketServer {

    protected String mip;
    protected int mport;
    protected ServerSocket serverSocket;

    protected Boolean mkillMe = true;
    protected CommunicationThread mClientsocketRunnable;
    protected Event_SocketData mevent_socketData;
    protected AndruavGCSSerialSocketServer Me;

    private long last_sent_time = 0;
    private final long last_sent_time_duration = 1000;

    //////////BUS EVENT


    public void onEvent (final Event_ShutDown_Signalling event)
    {
        if (event.CloseOrder != 1) return ;


        this.shutDown();
        //App.stop = null;
    }


    /*
    * Data is comming from Drone-FCB to GCS and we need to forard it to GCS App from here.
    */
    public void onEvent (final Event_FCBData event_FCBData)
    {

        // You can add here
        //   if ((!SocketServer.isConnected()) ||(event.IsLocal == Event_SocketData.SOURCE_LOCAL)) return ;
        // in case Drone has built in TCPserver at the same time.


        if ((event_FCBData.senderWe7da==null) || (!event_FCBData.senderWe7da.Equals(AndruavSettings.remoteTelemetryAndruavWe7da)))
        {
            // a packet that is not sent to me !!!
            return ;
        }

        //final Message msg = mHandler.obtainMessage();
        //msg.obj = event_FCBData;
        //mHandler.sendMessageDelayed(msg,0);

        Me.handleIncommingMessages(event_FCBData);

    }


    ///////////////////

    protected void shutDown ()
    {
        this.stopListening();
    }

    public Boolean isRunning()
    {
        return (!mkillMe);
    }


    /***
     * Client Socket Thread
     * Created per connection .... in our case it should be one only.
     */
    class CommunicationThread implements Runnable {

        private final Socket mclientSocket;
        final byte[] buffer = new byte[8192];

        private BufferedInputStream input;
        private BufferedOutputStream output;
        private Boolean mCommKillMe = false;


        private long last_sent_time = 0;
        private final long last_sent_time_duration = 1000;




        public CommunicationThread(Socket clientSocket) {
            mclientSocket = clientSocket;
            mClientsocketRunnable = this;
            try {

                this.input = new BufferedInputStream(clientSocket.getInputStream());
                this.output = new BufferedOutputStream(clientSocket.getOutputStream());

            } catch (IOException e) {
                final long now = System.currentTimeMillis();
                if ((now - last_sent_time) < last_sent_time_duration)
                {
                    return ;
                }
                last_sent_time = now;

                AndruavEngine.log().logException("fcb-exception2", e);
            }
        }


        private int exception_SerialSocket_counter = 5;
        public void run() {

            while (!mCommKillMe && (!Thread.currentThread().isInterrupted())) {

                try {

                    final int len = input.read(buffer);
                    if (len == -1) {
                        break;
                    }
                    // Happens when u disable telemetry before closing sockets..
                    if (AndruavSettings.remoteTelemetryAndruavWe7da!=null) {
                        mevent_socketData.Data = Arrays.copyOf(buffer, len);
                        mevent_socketData.IsLocal = Event_SocketData.SOURCE_LOCAL;  // the event is not a translation of AndruavEvent.
                        mevent_socketData.DataLength = len;
                        mevent_socketData.targetName = AndruavSettings.remoteTelemetryAndruavWe7da.PartyID;
                        mevent_socketData.targetWe7da = AndruavSettings.remoteTelemetryAndruavWe7da;
                        EventBus.getDefault().post(mevent_socketData);
                    }

                }
                catch (Exception error)
                {
                    if (!mCommKillMe) {
                        //java.net.SocketException: Socket closed is OK Exception in case  mCommKillMe = true
                        // soome times the cause is [ java.net.SocketException: recvfrom failed: ECONNRESET (Connection reset by peer)]
                        if (exception_SerialSocket_counter > 0) {
                            exception_SerialSocket_counter = exception_SerialSocket_counter - 1;
                            AndruavEngine.log().logException("fcb-exception3", error);
                        }
                        return;
                    }
                }
            }

            if (!mCommKillMe) Close();
            Event_SocketAction event_socketAction = new Event_SocketAction(Event_SocketAction.SOCKETACTION_CLIENT_DISCONNECTED);
            SocketAddress socketAddress =mclientSocket.getRemoteSocketAddress();
            if (socketAddress!= null) {
                event_socketAction.clientSocketIP = socketAddress.toString();
                EventBus.getDefault().post(event_socketAction);
            }
            // we disconnected anyway
            AndruavEngine.notification().Speak(App.getAppContext().getString(R.string.gen_serialsocket_client_disconnected));

        }

        public void Close ()
        {
            try {
                mCommKillMe = true;

                if (this.input != null)  this.input.close();
                if (this.output != null) this.output.close();
                if (mclientSocket != null) mclientSocket.close();
            } catch (IOException e) {
                final long now = System.currentTimeMillis();
                if ((now - last_sent_time) < last_sent_time_duration)
                {
                    return ;
                }
                last_sent_time = now;

                AndruavEngine.log().logException("fcb-exception2", e);
            }
        }

    }

    public AndruavGCSSerialSocketServer()
    {
        Me = this;

    }

    protected void handleIncommingMessages (Event_FCBData event_FCBData)
    {
        if (mkillMe) return;


        if (mClientsocketRunnable == null) return ;
        if ((!mClientsocketRunnable.mclientSocket.isConnected()) ||(event_FCBData.IsLocal==Event_SocketData.SOURCE_LOCAL)) return ;

        try {
            mClientsocketRunnable.output.write(event_FCBData.Data, 0, event_FCBData.DataLength);
            mClientsocketRunnable.output.flush();
        } catch (IOException e) {
            // Client socket such as Mission Planner Shutdown
            final long now = System.currentTimeMillis();
            if ((now - last_sent_time) < last_sent_time_duration)
            {
                return ;
            }
            last_sent_time = now;

            AndruavEngine.log().logException("fcb-exception", e);
            mClientsocketRunnable.Close();
            mClientsocketRunnable = null;
        }
        catch (NullPointerException ex)
        {
            // socket has been closed externaly
            //************ CAUSE OF ERROR ************ java.lang.NullPointerException at rcmobile.FPV.Communication.Telemetry.SerialSocketServer.AndruavGCSSerialSocketServer$1.handleMessage(AndruavGCSSerialSocketServer.java:256)
            return ;
        }
    }

    public void Listen (String ip, int port)
    {
        if (isRunning())
        {
            // Stop need to be callsed
            stopListening();
        }
        mport=port;
        mip=ip;
        mkillMe = false;

    }

    public void stopListening ()
    {
        EventBus.getDefault().unregister(this);
        mkillMe = true;
        try {
           /* if (mHandler != null) {
                mHandler.removeCallbacksAndMessages(null);
                mHandler = null;
            }
            */

           EventBus.getDefault().post(new Event_SocketAction(Event_SocketAction.SOCKETACTION_CLOSED));

        } catch (Exception e) {
            final long now = System.currentTimeMillis();
            if ((now - last_sent_time) < last_sent_time_duration)
            {
                return ;
            }
            last_sent_time = now;

            AndruavEngine.log().logException("fcb-exception", e);
        }
    }


}
