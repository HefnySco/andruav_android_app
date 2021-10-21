package com.andruav.protocol.communication.websocket;

import android.content.ContentValues;

import com.andruav.AndruavEngine;
import com.andruav.AndruavSettings;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public abstract class AndruavWSClientBase_TooTallNate extends AndruavWSClientBase {
    public int getSocketAction  ()
    {

        return mSocketAction ;
    }

    public void setSocketAction (int value)
    {
        mSocketAction = value;
        //Log.d("ac","mSocketAction   = " + value);
    }



    protected final AndruavWSClientBase_TooTallNate Me;


    /***
     * connection port as seen from server.
     */
    protected String mport;
    /***
     * client IP as seend from server...not the LAN IP.
     */
    protected String mIP;
    /***
     * ws://IP:port?SID=Account_SID
     * <br>IP is the server IP.
     * <br>Port is the server listening port.
     * <br>SID: can be ANYTHIG Unique for the Account.  Acount-has multiple -> groups -has multiple -> units
     */
    protected URI mURI;

    protected ContentValues mextraHeaders;





    protected WebSocketClient mWebSocketClient;


    /////////// EOF Attributes




    @Override
    protected void socketSendTextMessage(String payload)
    {
        if (mWebSocketClient == null) return;
        mWebSocketClient.send(payload);
    }

    @Override
    protected void socketSendBinaryMessage(byte[] payload)
    {
        if (mWebSocketClient == null) return;
        mWebSocketClient.send(payload);
    }



    @Override
    protected void socketDisconnect ()
    {
        if (mWebSocketClient == null) return;
        mWebSocketClient.close();
    }



    protected AndruavWSClientBase_TooTallNate (final String uri, final ContentValues extraHeaders)
    {
        super ();
        Me = this;
        try {
            mURI = new URI(uri);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }
        mextraHeaders = extraHeaders;


        synchronized (mSocketStateSync) {
            setSocketState  (SOCKETSTATE_FREASH);
            setSocketAction (SOCKETACTION_NONE);
        }
        for (int i=0;i<PING_HISTORY_SIZE; i=i+1)
        {
            HistoryPing.add(300,true); // initial delay for ping
        }
        initHandler();

    }




    @Override
    public void connect (final URI url)
    {
        try {
            synchronized (mSocketStateSync) {
                merrorRecovery = true;
                mIgnoreConnect = true;
                mkillMe = false;
                setSocketAction(SOCKETACTION_CONNECTING);
            }

            if (mWebSocketClient != null)
            {
                mWebSocketClient.close();
            }

            mWebSocketClient = new WebSocketClient(mURI) {

                private long mlastTime=0;


                @Override
                public void onOpen(final ServerHandshake handshakedata) {
                    Me.onOpen();
                }

                @Override
                public void onMessage(final String message) {
                    Me.TotalPacketsRecieved+=1;
                    Me.TotalBytesRecieved = TotalBytesRecieved + message.length();
                    Me.onTextMessage(message);
                }


                @Override
                public void onMessage(final ByteBuffer bytes ) {
                    Me.TotalBinaryBytesRecieved+=1;
                    final byte[] b = bytes.array();
                    Me.TotalBytesRecieved = TotalBytesRecieved + b.length;
                    Me.onBinaryMessage(b);
                }

                @Override
                public void onClose(final int code, final String reason, boolean remote) {
                    if ((code == CloseFrame.ABNORMAL_CLOSE) && (merrorRecovery)) {
                        // it is an error not a close
                        this.onError(code, reason);
                        return;
                    }
                    synchronized (mSocketStateSync) {
                        //socketState = enum_socketState.DISCONNECTED;

                        setSocketState  (SOCKETSTATE_DISCONNECTED);
                        setSocketAction (SOCKETACTION_NONE);

                    }
                    Me.onClose(code, reason);

                    doErrorRecovery();
                }

                @Override
                public void onError(final Exception ex) {
                    Me.onError(1,ex.getMessage());
                    return ;
                }


                private void onError (final int code, final String reason)
                {
                    // moved to setSocketState()
                    //mIgnoreConnect = false; // last connect has failed.
                    //merrorRecovery = true;


                    // avoid repeated exceptions flooding the database.
                    long now = System.currentTimeMillis();
                    if ((now - mlastTime) > 15000) {
                        mlastTime = now;
                        AndruavEngine.log().log(AndruavSettings.Account_SID, "exception_ws", "code:" + code + " error: " + reason);
                    }
                    synchronized (mSocketStateSync) {
                        if (mkillMe)
                        {
                            merrorRecovery = false;
                            return;
                        }
                        //socketState = enum_socketState.ERROR;
                        setSocketState  (SOCKETSTATE_ERROR);
                        setSocketAction (SOCKETACTION_NONE);

                    }

                    doErrorRecovery();

                    Me.onError(code, reason);
                }
            };

            mWebSocketClient.setTcpNoDelay(true);
            mWebSocketClient.setReuseAddr(true); /// CHECK THIS


            /////// SSL SECURE SERVICE LAYER
            final TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    X509Certificate[] cArrr = new X509Certificate[0];
                    return cArrr;
                }

                @Override
                public void checkServerTrusted(final X509Certificate[] chain,
                                               final String authType) {
                }

                @Override
                public void checkClientTrusted(final X509Certificate[] chain,
                                               final String authType) {
                }
            }};

            final SSLContext sslContext;
            sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            SSLSocketFactory factory = sslContext.getSocketFactory();
            mWebSocketClient.setSocket( factory.createSocket() );

            //////////////////////////////////////////////////

            mWebSocketClient.connect();

        } catch (Exception e) {
            e.printStackTrace();
            mIgnoreConnect = false;
            setSocketState  (SOCKETSTATE_ERROR);
        }
    }

    @Override
    public void connect(final String url ) {
        if (mIgnoreConnect) return ;

        if ((url != null) && (!url.isEmpty()))
        {
            try {
                mURI = new URI(url); //"wss://echo.websocket.org");
            } catch (URISyntaxException e) {
                e.printStackTrace();
                return;
            }
        }

        connect (mURI);


    }


    @Override
    public boolean isConnected()
    {
        return mWebSocketClient.isOpen();
    }


}
