package org.droidplanner.services.android.impl.utils.video;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;

import com.o3dr.android.client.utils.connection.AbstractIpConnection;
import com.o3dr.android.client.utils.connection.IpConnectionListener;
import com.o3dr.android.client.utils.connection.UdpConnection;
import com.o3dr.android.client.utils.video.DecoderListener;
import com.o3dr.android.client.utils.video.MediaCodecManager;
import com.o3dr.services.android.lib.drone.action.CameraActions;
import com.o3dr.services.android.lib.drone.attribute.error.CommandExecutionError;
import com.o3dr.services.android.lib.model.ICommandListener;

import org.droidplanner.services.android.impl.communication.model.DataLink;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import timber.log.Timber;

/**
 * Handles the video stream from artoo.
 */
public class VideoManager implements IpConnectionListener {
    private static final String TAG = VideoManager.class.getSimpleName();

    private static final SimpleDateFormat FILE_DATE_FORMAT =
        new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US);

    private static final String NO_VIDEO_OWNER = "no_video_owner";

    protected static final long RECONNECT_COUNTDOWN = 1000L; //ms

    public static final int ARTOO_UDP_PORT = 5600;
    private static final int UDP_BUFFER_SIZE = 1500;

    private final AtomicBoolean videoStreamObserverUsed = new AtomicBoolean(false);
    private final DataLink.DataLinkProvider linkProvider;

    public interface LinkListener {
        void onLinkConnected();

        void onLinkDisconnected();
    }

    private final Runnable reconnectTask = new Runnable() {
        @Override
        public void run() {
            handler.removeCallbacks(reconnectTask);
            if(linkConn != null)
                linkConn.connect(linkProvider.getConnectionExtras());
        }
    };

    private LinkListener linkListener;

    protected final Handler handler;
    private final AtomicBoolean isStarted = new AtomicBoolean(false);
    private final AtomicBoolean wasConnected = new AtomicBoolean(false);

    protected UdpConnection linkConn;

    private final MediaCodecManager mediaCodecManager;
    private final StreamRecorder streamRecorder;

    private int linkPort = -1;

    public VideoManager(Context context, Handler handler, DataLink.DataLinkProvider linkProvider) {
        this.streamRecorder = new StreamRecorder(context);

        this.handler = handler;
        this.mediaCodecManager = new MediaCodecManager(handler);
        this.mediaCodecManager.setNaluChunkListener(streamRecorder);
        this.linkProvider = linkProvider;
    }

    private void enableLocalRecording(String filename) {
        streamRecorder.enableRecording(filename);
    }

    private void disableLocalRecording() {
        streamRecorder.disableRecording();
    }

    public void startDecoding(final int udpPort, final Surface surface, final DecoderListener listener) {
        start(udpPort, null);

        final Surface currentSurface = mediaCodecManager.getSurface();
        if (surface == currentSurface) {
            if (listener != null)
                listener.onDecodingStarted();
            return;
        }

        // Stop any in progress decoding.
        Log.i(TAG, "Setting up video stream decoding.");
        mediaCodecManager.stopDecoding(new DecoderListener() {
            @Override
            public void onDecodingStarted() {
            }

            @Override
            public void onDecodingError() {
            }

            @Override
            public void onDecodingEnded() {
                try {
                    Log.i(TAG, "Video decoding set up complete. Starting...");
                    mediaCodecManager.startDecoding(surface, listener);
                } catch (IOException | IllegalStateException e) {
                    Log.e(TAG, "Unable to create media codec.", e);
                    if (listener != null)
                        listener.onDecodingError();
                }
            }
        });
    }

    public void reset() {
        Timber.d("Resetting video");
        disableLocalRecording();

        stopDecoding(null);
    }

    public void stopDecoding(DecoderListener listener) {
        Log.i(TAG, "Aborting video decoding process.");
        if (mediaCodecManager == null) return ;
        mediaCodecManager.stopDecoding(listener);

        stop();
    }

    public boolean isLinkConnected() {
        return this.linkConn != null && this.linkConn.getConnectionStatus() == AbstractIpConnection.STATE_CONNECTED;
    }

    private void start(int udpPort, LinkListener listener) {
        if (this.linkConn == null || udpPort != this.linkPort){
            if (isStarted.get()){
                stop();
            }

            this.linkConn = new UdpConnection(handler, udpPort, UDP_BUFFER_SIZE, true, 42);
            this.linkConn.setIpConnectionListener(this);
            this.linkPort = udpPort;
        }

        Log.d(TAG, "Starting video manager");
        handler.removeCallbacks(reconnectTask);

        isStarted.set(true);
        this.streamRecorder.startConverterThread();
        this.linkConn.connect(linkProvider.getConnectionExtras());
        this.linkListener = listener;
    }

    private void stop() {
        Log.d(TAG, "Stopping video manager");

        handler.removeCallbacks(reconnectTask);

        isStarted.set(false);

        if(this.linkConn != null) {
            //Break the link
            this.linkConn.disconnect();
            this.linkConn = null;
        }

        this.linkPort = -1;

        this.streamRecorder.stopConverterThread();
    }

    @Override
    public void onIpConnected() {
        Log.d(TAG, "Connected to video stream");

        handler.removeCallbacks(reconnectTask);
        wasConnected.set(true);

        if (linkListener != null)
            linkListener.onLinkConnected();
    }

    @Override
    public void onIpDisconnected() {
        Log.d(TAG, "Video stream disconnected");

        if (isStarted.get()) {
            if (shouldReconnect()) {
                //Try to reconnect
                handler.postDelayed(reconnectTask, RECONNECT_COUNTDOWN);
            }

            if (linkListener != null && wasConnected.get())
                linkListener.onLinkDisconnected();

            wasConnected.set(false);
        }
    }

    @Override
    public void onPacketReceived(ByteBuffer packetBuffer) {
        if (!videoStreamObserverUsed.get()) {
            // Feed this data stream to the decoder.
            mediaCodecManager.onInputDataReceived(packetBuffer.array(), packetBuffer.limit());
        }
    }

    protected void postSuccessEvent(final ICommandListener listener) {
        if (handler != null && listener != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        listener.onSuccess();
                    } catch (RemoteException e) {
                        Log.e(TAG, e.getMessage(), e);
                    }
                }
            });
        }
    }

    protected void postTimeoutEvent(final ICommandListener listener) {
        if (handler != null && listener != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        listener.onTimeout();
                    } catch (RemoteException e) {
                        Log.e(TAG, e.getMessage(), e);
                    }
                }
            });
        }
    }

    protected void postErrorEvent(final int error, final ICommandListener listener) {
        if (handler != null && listener != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        listener.onError(error);
                    } catch (RemoteException e) {
                        Log.e(TAG, e.getMessage(), e);
                    }
                }
            });
        }
    }

    protected boolean shouldReconnect() {
        return true;
    }

    private void checkForLocalRecording(Bundle videoProps){

        final boolean isLocalRecordingEnabled = videoProps.getBoolean(CameraActions.EXTRA_VIDEO_ENABLE_LOCAL_RECORDING);
        if (isLocalRecordingEnabled) {
            String localRecordingFilename = videoProps.getString(CameraActions.EXTRA_VIDEO_LOCAL_RECORDING_FILENAME);
            if(TextUtils.isEmpty(localRecordingFilename)){
                localRecordingFilename = "appid." + FILE_DATE_FORMAT.format(new Date());
            }

            if(!localRecordingFilename.equalsIgnoreCase(streamRecorder.getRecordingFilename())){
                if(streamRecorder.isRecordingEnabled()){
                    disableLocalRecording();
                }

                enableLocalRecording(localRecordingFilename);
            }
        }
        else {
            disableLocalRecording();
        }
    }

    public void startVideoStream(Bundle videoProps,Surface videoSurface,
                                 final ICommandListener listener) {
        Timber.d("Video stream start request from appID.");

        final int udpPort = videoProps.getInt(CameraActions.EXTRA_VIDEO_PROPS_UDP_PORT, -1);
        if (videoSurface == null || udpPort == -1){
            postErrorEvent(CommandExecutionError.COMMAND_FAILED, listener);
            return;
        }


            checkForLocalRecording(videoProps);

            Timber.i("Starting video decoding.");
            startDecoding(udpPort, videoSurface, new DecoderListener() {

                @Override
                public void onDecodingStarted() {
                    Timber.i("Video decoding started.");
                    postSuccessEvent(listener);
                }

                @Override
                public void onDecodingError() {
                    Timber.i("Video decoding failed.");
                    postErrorEvent(CommandExecutionError.COMMAND_FAILED, listener);
                    reset();
                }

                @Override
                public void onDecodingEnded() {
                    Timber.i("Video decoding ended successfully.");
                    reset();
                }
            });

    }

    public void startVideoStreamForObserver(String newVideoTag,
                                            final ICommandListener listener) {
        Timber.d("Video stream start request from app. Video owner is.");

        if (newVideoTag == null)
            newVideoTag = "";


        if (listener != null) {
            Timber.i("Successful lock obtained for app with id appID." );

            videoStreamObserverUsed.set(true);

            postSuccessEvent(listener);
        } else {
            postErrorEvent(CommandExecutionError.COMMAND_DENIED, listener);
        }
    }

    public void stopVideoStream(final ICommandListener listener) {
        Timber.d("Video stream stop request from appID.");


            disableLocalRecording();

            Timber.d("Stopping video decoding.");

            Timber.i("Stopping video decoding.");
            stopDecoding(new DecoderListener() {
                @Override
                public void onDecodingStarted() {
                }

                @Override
                public void onDecodingError() {
                    postSuccessEvent(listener);
                }

                @Override
                public void onDecodingEnded() {
                    postSuccessEvent(listener);
                }
            });

    }

    public void stopVideoStreamForObserver(final ICommandListener listener) {
        Timber.d("Video stream stop request from appID.");


        videoStreamObserverUsed.set(false);
        postSuccessEvent(listener);
    }

    public void tryStoppingVideoStream() {

        if(videoStreamObserverUsed.get()){
            stopVideoStreamForObserver(null);
        }
        else {
            stopVideoStream(null);
        }
    }

}
