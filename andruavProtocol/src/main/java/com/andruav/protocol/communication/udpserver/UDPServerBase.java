package com.andruav.protocol.communication.udpserver;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import com.andruav.AndruavEngine;
import com.andruav.AndruavSettings;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

/**
 * Created by mhefny on 7/10/16.
 */
public abstract class UDPServerBase {
    private boolean mIsPaused = false;

    private static final int BUFFSIZE = 4096;


    private final int port;

    private Handler mHandler;
    private HandlerThread mhandlerThread;
    private java.lang.Thread mthreadListener;

    private DatagramSocket socketUDP;
    private boolean mKillMe = false;

    WifiManager mWifi;
    private boolean mRunning = false;


    /***
     * Socket used to sendMessageToModule UDP packets.
     * either broadcast ID or sendMessageToModule Data to specific target.
     */
    DatagramSocket dataSocketSender = null;



    public UDPServerBase(int port)  {
        this.port = port;

    }


    public UDPServerBase()  {
        this.port = AndruavSettings.DEFAULT_ANDRUAV_LAN_UDP_PORT;

    }

    public abstract void broadCast() throws IllegalAccessException;



    public void init () throws SocketException {
        mKillMe = false;
        mWifi = (WifiManager) AndruavEngine.AppContext.getSystemService(Context.WIFI_SERVICE);
        if (mthreadListener != null) {
            mthreadListener.interrupt();
        }


        if (mhandlerThread != null) {
            mhandlerThread.quit();
        }

        if (socketUDP == null || socketUDP.isClosed()) {
            socketUDP = new DatagramSocket(port);
            //socketUDP.setReuseAddress(true);
            //socketUDP.bind(new InetSocketAddress(port));
            socketUDP.setBroadcast(true);
        }

        mhandlerThread = new HandlerThread("UDP_Thread");
        mhandlerThread.start();
        mHandler = new Handler(mhandlerThread.getLooper()) {

            private int exception_udp_counter = 3;
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                try {
                    if ((mKillMe) || (socketUDP.isClosed()))
                    {
                        return ;
                    }

                    socketUDP.send((DatagramPacket) msg.obj);
                } catch (IOException e) {
                    if (exception_udp_counter >0) {
                        AndruavEngine.log().logException("exception-init", e);
                        exception_udp_counter = exception_udp_counter -1;
                    }

                }

            }
        };

        mthreadListener = new Thread(mrunnableListener);
        mthreadListener.start();


    }


    protected abstract void onData(final DatagramPacket packet,final byte[] Buffer, final int len);

    private final Runnable mrunnableListener = new Runnable() {
        @Override
        public void run() {
            try
            {
                        if (mIsPaused) return ;

                        final byte[] buffer = new byte[BUFFSIZE];
                        final DatagramPacket packet = new DatagramPacket(buffer, BUFFSIZE);

                        int len; // bytes received
                        mRunning = true;
                        while (isRunning()) {
                            try {
                                if (socketUDP.isClosed())
                                {
                                    len=0;
                                }
                                else
                                {
                                    socketUDP.receive(packet);
                                    len = packet.getLength();
                                }

                                if (len > 0) {
                                    onData (packet,buffer,len);
                                }
                                else {
                                    // could happen mostly to the servers thread
                                    // connHandler.obtainMessage(CONNECTOR_STATE.MSG_CONN_SERVER_CLIENT_DISCONNECTED.ordinal()).sendToTarget();
                                    mRunning = false;
                                }

                                if (mKillMe) return ;
                            }
                            catch (Exception e) {
                                // could happen mostly to the client thread
                                // Log.d(TAG, "** UDP packet receive exception**" + e.getMessage());
                                if (e.getMessage().equals("Socket closed")) return ; // ignore
                                AndruavEngine.log().logException("udp",e);

                            }
                        }
                        if (socketUDP!= null)
                        {
                            socketUDP.close();
                        }
            } catch (Exception ex) {
                mRunning = false;
                AndruavEngine.log().logException("exception-udp", ex);
            }
        }
    };




    protected void send(final InetAddress destAddress, int destPort,final byte[] buffer, final int length)
    {
        try
        {
            if (mIsPaused) return ;
            if (mKillMe) return ;
            if (mHandler==null) return;

            DatagramPacket packet = new DatagramPacket(buffer, length, destAddress, destPort);

            final Message msg =  mHandler.obtainMessage();
            msg.obj = packet;
            mHandler.sendMessageDelayed(msg, 0);
        } catch (Exception ex) {
            AndruavEngine.log().logException("exception-udp", ex);
        }
    }



    protected void shutDown()
    {
        // EventBus.getDefault().unregister(this);

        StopPersistentConnection();
    }


    public final boolean isRunning()
    {
        return (!mKillMe);
    }


    public void StopPersistentConnection ()
    {
        try
        {
            mKillMe = true;

            // should be after thread to avoid using invalid socket
            if (socketUDP!= null) {
                if (!socketUDP.isClosed()) {
                    socketUDP.close();
                }
            }
            if (mthreadListener != null)
            {

                mthreadListener.interrupt();

                mthreadListener = null;
            }

            if (mHandler != null) {
                mHandler.removeCallbacksAndMessages(null);
            }

            if (mhandlerThread != null) {
                mhandlerThread.quit();
                mhandlerThread = null;
            }


            if (dataSocketSender != null) {

                //dataSocketSender.disconnect();
                dataSocketSender.close();
                dataSocketSender = null;
            }

            socketUDP = null;

        } catch (Exception ex) {
            AndruavEngine.log().logException("exception-udp", ex);
        }
    }


    public void setPause(boolean paused)
    {
        mIsPaused = paused;
    }

    public boolean isPaused()
    {
        return mIsPaused;
    }

}
