package com.andruav.protocol.communication.websocket;

import android.content.ContentValues;

import com.andruav.AndruavEngine;
import com.andruav.AndruavSettings;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ServerHandshake;

import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;

import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

import com.andruav.protocol.communication.tls.TlsTrustPolicy;

public abstract class AndruavWSClientBase_TooTallNate extends AndruavWSClientBase {
    public int getSocketAction  ()
    {

        return mSocketAction ;
    }

    public void setSocketAction (int value)
    {
        mSocketAction = value;
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
            // Seconds in Java-WebSocket 1.5.7. Makes half-dead-socket detection explicit - a
            // Wi-Fi<->LTE handoff fires onError within 45s instead of waiting for a TCP timeout.
            mWebSocketClient.setConnectionLostTimeout(45);


            /////// SSL SECURE SERVICE LAYER
            // AN-0018: verification policy lives in TlsTrustPolicy - strict for
            // public endpoints, pinned self-signed for private ones, trust-all
            // only when the "Require SSL" preference is OFF.
            final String tlsHost = mURI.getHost();
            final X509TrustManager trustManager = TlsTrustPolicy.newTrustManager();
            final SSLSocketFactory factory = TlsTrustPolicy.createSocketFactory(trustManager);
            if (factory == null) {
                // never fall back to a plaintext socket for a wss:// endpoint
                throw new IllegalStateException("cannot create TLS socket factory");
            }
            final Socket socket = factory.createSocket();
            if ((socket instanceof SSLSocket) && TlsTrustPolicy.requiresHostnameVerification(tlsHost)) {
                final SSLParameters sslParameters = ((SSLSocket) socket).getSSLParameters();
                sslParameters.setEndpointIdentificationAlgorithm("HTTPS");
                ((SSLSocket) socket).setSSLParameters(sslParameters);
            }
            mWebSocketClient.setSocket(socket);

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
        return mWebSocketClient != null && mWebSocketClient.isOpen();
    }


}
