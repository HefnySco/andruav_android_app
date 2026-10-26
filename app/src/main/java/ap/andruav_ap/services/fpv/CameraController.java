package ap.andruav_ap.services.fpv;

import android.content.Context;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;

import org.greenrobot.eventbus.EventBus;
import org.webrtc.VideoFrame;
import org.webrtc.YuvHelper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.andruav.AndruavEngine;
import com.andruav.AndruavFacade;
import com.andruav.AndruavSettings;
import com.andruav.andruavUnit.AndruavLocation;
import com.andruav.andruavUnit.AndruavUnitBase;
import com.andruav.andruavUnit.AndruavUnitAllGCS;
import com.andruav.event.fpv7adath.Event_FPV_CMD;

import ap.andruav_ap.App;
import ap.andruav_ap.denative.providers.camera.CameraBackend;
import ap.andruavmiddlelibrary.eventClasses.fpvEvent.Event_FPV_Image;
import ap.andruavmiddlelibrary.factory.io.FileHelper;
import ap.andruavmiddlelibrary.factory.util.Image_Helper;

/**
 * Single owner of the phone-camera still/record/zoom surface, reachable
 * without any FPV Activity or preview Surface (headless).
 *
 * Two frame sources are arbitrated per shot:
 *  - when FPVStreamingService owns the camera (its WebRTC capturer is up),
 *    the next VideoFrame off the live stream is latched and converted to
 *    JPEG — exactly what the FPV activities used to do via the renderer;
 *  - otherwise a Camera2 + ImageReader(JPEG) still is taken directly
 *    ({@link Camera2StillCapture}), so a capture works with the screen off
 *    and no FPV screen open.
 *
 * Recording goes through {@link #setRecording(String, boolean, String)} -
 * the single choke point every path (GCS RECORDVIDEO, capability invoke,
 * Sync_EventFire command, fault, shutdown) ends in. It is edge-only and
 * fires camera.<ch>.went_recording / went_idle facts through the
 * registered {@link Listener} (the de.cap/1 camera provider).
 *
 * Wire-facing "src" strings mirror the Linux de_camera module
 * (RECORD_CAUSE): "gcs", "invoke", "event", "fault", "shutdown".
 */
public class CameraController implements CameraBackend {

    public static final String SRC_GCS      = "gcs";
    public static final String SRC_INVOKE   = "invoke";
    public static final String SRC_EVENT    = "event";
    public static final String SRC_FAULT    = "fault";
    public static final String SRC_SHUTDOWN = "shutdown";

    private static final String TAG = "CameraController";

    private static final long FRAME_LATCH_TIMEOUT_MS      = 5000;
    private static final long STREAM_START_TIMEOUT_MS     = 12000;
    private static final long HEADLESS_STILL_TIMEOUT_MS   = 8000;
    private static final long RECORD_STREAM_TIMEOUT_MS    = 10000;

    private static volatile CameraController sInstance;

    public static CameraController getInstance() {
        if (sInstance == null) {
            synchronized (CameraController.class) {
                if (sInstance == null) sInstance = new CameraController();
            }
        }
        return sInstance;
    }

    /**
     * In-process view of FPVStreamingService's capture pipeline, registered
     * while its PeerConnectionManager owns the camera. Implemented by the
     * service so this controller never reaches into WebRTC internals.
     */
    public interface StreamingLink {
        /** true once the capturer is producing frames. */
        boolean isCapturing();
        /** enumerator device name of the camera currently captured. */
        String captureDeviceName();
        /** facing of the camera currently captured (fallback id when the
         *  device name is not a Camera2 id, e.g. Camera1 enumerator). */
        boolean isCaptureFrontFacing();
        /** starts the MP4 recorder; returns the file label or null on failure. */
        String startVideoRecording();
        /** stops the MP4 recorder; returns the finalized file label or null. */
        String stopVideoRecording();
        boolean isVideoRecording();
        /** zoom as a 0..1 fraction of the sensor's max digital zoom. */
        void setZoomFraction(float fraction);
        float getZoomFraction();
    }

    private static final class Channel extends CameraBackend.CameraChannel {
        private final int    mFacing;   // CameraCharacteristics.LENS_FACING_*
        private final double mMaxZoom;  // SCALER_AVAILABLE_MAX_DIGITAL_ZOOM, 1 when unknown

        Channel(String key, String cameraId, String name, int facing, double maxZoom) {
            super(key, cameraId, name);
            this.mFacing = facing;
            this.mMaxZoom = maxZoom;
        }
    }

    // ---- channels ------------------------------------------------------------

    private volatile List<Channel> mChannels;      // null = not enumerated yet
    private final Object mChannelsLock = new Object();

    private void ensureChannels() {
        if (mChannels != null) return;
        synchronized (mChannelsLock) {
            if (mChannels != null) return;
            mChannels = enumerateChannels();
        }
    }

    private static List<Channel> enumerateChannels() {
        final List<Channel> channels = new ArrayList<>();
        try {
            final Context context = App.getAppContext();
            if (context == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                return channels;
            }
            final CameraManager manager =
                    (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            if (manager == null) return channels;

            int backIndex = 0, frontIndex = 0, usbIndex = 0;
            for (final String cameraId : manager.getCameraIdList()) {
                final CameraCharacteristics characteristics;
                try {
                    characteristics = manager.getCameraCharacteristics(cameraId);
                } catch (Exception e) {
                    continue;
                }
                final Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing == null) continue;
                final Float maxZoom = characteristics.get(
                        CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
                final double max = maxZoom != null ? maxZoom : 1.0;
                final String key;
                switch (facing) {
                    case CameraCharacteristics.LENS_FACING_BACK:
                        key = backIndex++ == 0 ? "back" : "cam_" + cameraId;
                        break;
                    case CameraCharacteristics.LENS_FACING_FRONT:
                        key = frontIndex++ == 0 ? "front" : "cam_" + cameraId;
                        break;
                    case CameraCharacteristics.LENS_FACING_EXTERNAL:
                        key = "usb_" + usbIndex++;
                        break;
                    default:
                        continue;
                }
                channels.add(new Channel(key, cameraId,
                        "camera " + cameraId, facing, max));
            }
        } catch (Exception e) {
            AndruavEngine.log().logException("cam_channels", e);
        }
        return channels;
    }

    private Channel channelByKey(final String key) {
        ensureChannels();
        for (final Channel channel : mChannels) {
            if (channel.key.equals(key)) return channel;
        }
        return null;
    }

    @Override
    public List<CameraBackend.CameraChannel> channels() {
        ensureChannels();
        return new ArrayList<CameraBackend.CameraChannel>(mChannels);
    }

    @Override
    public String defaultChannelKey() {
        ensureChannels();
        for (final Channel channel : mChannels) {
            if ("back".equals(channel.key)) return channel.key;
        }
        return mChannels.isEmpty() ? null : mChannels.get(0).key;
    }

    /**
     * Resolve a user/mission channel string to a channel key:
     * "" -> default (first surfaced, "back" preferred), then resource key,
     * then the Camera2 device id, then the display name.
     */
    @Override
    public String resolveChannelKey(final String channel) {
        if (channel == null || channel.isEmpty()) return defaultChannelKey();
        ensureChannels();
        for (final Channel c : mChannels) {
            if (c.key.equals(channel)) return c.key;
        }
        for (final Channel c : mChannels) {
            if (c.id.equals(channel)) return c.key;
        }
        for (final Channel c : mChannels) {
            if (c.name.equals(channel)) return c.key;
        }
        return null;
    }

    /** Key of the channel the WebRTC capturer is on, or null when idle. */
    @Override
    public String activeChannelKey() {
        final StreamingLink link = mStreamingLink;
        if (link == null || !link.isCapturing()) return null;
        ensureChannels();
        final String deviceName = link.captureDeviceName();
        if (deviceName != null) {
            for (final Channel c : mChannels) {
                if (c.id.equals(deviceName)) return c.key;
            }
        }
        final boolean front = link.isCaptureFrontFacing();
        for (final Channel c : mChannels) {
            if (front == (c.mFacing == CameraCharacteristics.LENS_FACING_FRONT)) {
                return c.key;
            }
        }
        return defaultChannelKey();
    }

    // ---- fact listener (registered by the camera capability provider) ---------

    private volatile Listener mListener;

    @Override
    public void setListener(final Listener listener) {
        this.mListener = listener;
    }

    private void fireCaptured(final String ch, final String file, final double[] pos,
                              final long tMs, final int n, final String invokeId) {
        final Listener listener = mListener;
        if (listener != null) {
            try {
                listener.onCaptured(ch, file, pos, tMs, n, invokeId);
            } catch (Exception e) {
                AndruavEngine.log().logException("cam_fact", e);
            }
        }
    }

    private void fireRecordEdge(final String ch, final boolean on, final String src,
                                final String reason, final String file) {
        final Listener listener = mListener;
        if (listener == null) return;
        try {
            if (on) {
                listener.onWentRecording(ch, src, file);
            } else {
                listener.onWentIdle(ch, src, reason, file);
            }
        } catch (Exception e) {
            AndruavEngine.log().logException("cam_fact", e);
        }
    }

    // ---- streaming link --------------------------------------------------------

    private volatile StreamingLink mStreamingLink;
    private final Object mLinkLock = new Object();

    /** Called by FPVStreamingService once its PeerConnectionManager exists. */
    public void attachStreamingLink(final StreamingLink link) {
        synchronized (mLinkLock) {
            mStreamingLink = link;
            mLinkLock.notifyAll();
        }
    }

    /** Called by FPVStreamingService when its capture pipeline goes away. */
    public void detachStreamingLink(final StreamingLink link) {
        synchronized (mLinkLock) {
            if (mStreamingLink == link) {
                mStreamingLink = null;
                mLinkLock.notifyAll();
            }
        }
    }

    /**
     * The service is tearing down: a recording still on goes through the
     * choke point as a shutdown so went_idle {reason:shutdown} fires and
     * the recorder finalizes its MP4.
     */
    public void onStreamingStopping(final StreamingLink link) {
        final String ch = activeChannelKey();
        if (ch != null) {
            final Channel channel = channelByKey(ch);
            if (channel != null && recordingFlag(ch)) {
                setRecording(ch, false, SRC_SHUTDOWN, new String[1]);
            }
        }
        detachStreamingLink(link);
    }

    // ---- still capture --------------------------------------------------------

    /** One burst: serialized on mBurstExecutor; per-channel busy rejection. */
    private final ExecutorService mBurstExecutor = Executors.newSingleThreadExecutor();
    private final java.util.Set<String> mBusyChannels = new java.util.HashSet<>();
    private final Object mBusyLock = new Object();

    /** Next-frame latch fed by FPVStreamingService.onFrame(VideoFrame). */
    private final AtomicReference<CountDownLatch> mFrameLatch = new AtomicReference<>();
    private final AtomicReference<VideoFrame> mLatchedFrame = new AtomicReference<>();

    /**
     * Fed by FPVStreamingService.onFrame - the service is the WebRTC local
     * track's external sink, so every captured frame passes through here.
     * Consumed only while a burst is waiting on a frame; retained so it
     * stays valid until the burst worker converts it.
     */
    public void onVideoFrame(final VideoFrame frame) {
        final CountDownLatch latch = mFrameLatch.get();
        if (latch == null) return;
        if (mLatchedFrame.compareAndSet(null, frame)) {
            frame.retain();
            latch.countDown();
        }
    }

    /**
     * Legacy GCS path: FPV_CMD_TAKEIMAGE (1050 Ctrl_Camera /
     * MAV_CMD_DO_DIGICAM_CONTROL). Preserves the old activity behaviour:
     * TimeBetweenShotes 0 -> 1 s floor, images saved locally and sent back
     * to the requester per the message flags.
     */
    public void onGcsTakeImage(final Event_FPV_CMD cmd) {
        if (cmd == null) return;
        long intervalMs = cmd.TimeBetweenShotes;
        if (intervalMs <= 0) intervalMs = 1;   // legacy: 0 means a 1-second floor
        startCapture(null, cmd.NumberOfImages, intervalMs * 1000,
                cmd.SaveImageLocally, cmd.SendBackImages, cmd.Requester,
                false, null, SRC_GCS, new String[1]);
    }

    /** Mode-change single shot forwarded by the FPV activities. */
    public void captureSilent(final AndruavUnitBase sendBackTo) {
        startCapture(null, 1, 0, false, true, sendBackTo, false, null, SRC_GCS, new String[1]);
    }

    // CameraBackend.startCapture - the de.cap/1 surface.
    @Override
    public boolean startCapture(final String channelKey, final int count, final int intervalMs,
                                final boolean gcsPreview, final String invokeId,
                                final String src, final String[] err) {
        return startCapture(channelKey, count, intervalMs, true, gcsPreview,
                new AndruavUnitAllGCS(), gcsPreview, invokeId, src, err);
    }

    /**
     * Queue a burst. Returns false + err[0] when the channel is unknown or a
     * burst is already running on it. The burst itself runs on
     * mBurstExecutor; every saved/sent shot fires a "camera.captured" fact.
     */
    public boolean startCapture(final String channelKey, final int count, final long intervalMs,
                                final boolean saveLocal, final boolean sendBack,
                                final AndruavUnitBase sendBackTo,
                                final boolean gcsPreview, final String invokeId,
                                final String src, final String[] err) {
        final String ch = resolveChannelKey(channelKey);
        if (ch == null) {
            if (err != null && err.length > 0) err[0] = "unknown camera channel '" + channelKey + "'";
            return false;
        }
        if (count < 1) {
            if (err != null && err.length > 0) err[0] = "count must be >= 1";
            return false;
        }
        synchronized (mBusyLock) {
            if (!mBusyChannels.add(ch)) {
                if (err != null && err.length > 0) {
                    err[0] = "camera '" + ch + "' is busy capturing";
                }
                return false;
            }
        }
        final String channelKeyFinal = ch;
        mBurstExecutor.execute(() -> {
            try {
                runBurst(channelKeyFinal, count, intervalMs, saveLocal, sendBack,
                        sendBackTo, gcsPreview, invokeId);
            } finally {
                synchronized (mBusyLock) {
                    mBusyChannels.remove(channelKeyFinal);
                }
            }
        });
        return true;
    }

    private void runBurst(final String ch, final int count, final long intervalMs,
                          final boolean saveLocal, final boolean sendBack,
                          final AndruavUnitBase sendBackTo,
                          final boolean gcsPreview, final String invokeId) {
        for (int n = 1; n <= count; n++) {
            final byte[] jpeg = acquireJpeg(ch);
            if (jpeg == null) {
                AndruavEngine.log().log2(TAG, "still", "capture failed on channel " + ch);
                break;
            }
            processJpeg(ch, jpeg, n, saveLocal, sendBack, sendBackTo, gcsPreview, invokeId);
            if (n < count && intervalMs > 0) {
                try {
                    Thread.sleep(intervalMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    /**
     * One shot from the best available source: the live WebRTC capturer
     * frame when it owns the requested channel, a bounded wait for a
     * just-starting stream, or a headless Camera2 still.
     */
    private byte[] acquireJpeg(final String ch) {
        final StreamingLink link = mStreamingLink;
        if (link != null && link.isCapturing()) {
            final String active = activeChannelKey();
            if (ch.equals(active)) {
                final byte[] jpeg = latchStreamFrame();
                if (jpeg != null) return jpeg;
                // stream went away mid-wait - fall through to Camera2
            }
            // requested channel is not the one being streamed: try the
            // device directly below (multi-camera devices may allow it).
        } else if (App.isFPVStreamingServiceRunning() || App.pendingFPVStart) {
            // A stream start is in flight (_7adath_InitAndroidCamera raced
            // us): wait for the capturer rather than competing for the
            // camera device with Camera2.
            if (awaitStreamingCapture(STREAM_START_TIMEOUT_MS)) {
                final String active = activeChannelKey();
                if (ch.equals(active)) {
                    final byte[] jpeg = latchStreamFrame();
                    if (jpeg != null) return jpeg;
                }
            }
        }

        final Channel channel = channelByKey(ch);
        if (channel == null) return null;
        final Camera2StillCapture.Outcome outcome = Camera2StillCapture.captureStill(
                App.getAppContext(), channel.id, pendingZoom(ch),
                HEADLESS_STILL_TIMEOUT_MS);
        return outcome.jpeg;
    }

    /** Wait until FPVStreamingService's capturer is producing frames. */
    private boolean awaitStreamingCapture(final long timeoutMs) {
        final long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            final StreamingLink link = mStreamingLink;
            if (link != null && link.isCapturing()) return true;
            if (!App.isFPVStreamingServiceRunning() && !App.pendingFPVStart) return false;
            synchronized (mLinkLock) {
                try {
                    mLinkLock.wait(250);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        return false;
    }

    /** Grab the next frame off the live stream and convert it to JPEG. */
    private byte[] latchStreamFrame() {
        final CountDownLatch latch = new CountDownLatch(1);
        mLatchedFrame.set(null);
        mFrameLatch.set(latch);
        try {
            latch.await(FRAME_LATCH_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            mFrameLatch.set(null);
        }
        final VideoFrame frame = mLatchedFrame.getAndSet(null);
        if (frame == null) return null;
        try {
            return videoFrameToJpeg(frame);
        } finally {
            frame.release();
        }
    }

    /** VideoFrame (I420) -> JPEG bytes, honouring the capture rotation. */
    private static byte[] videoFrameToJpeg(final VideoFrame frame) {
        VideoFrame.I420Buffer i420 = frame.getBuffer().toI420();
        try {
            final int rotation = frame.getRotation();
            final int width = rotation % 180 == 0 ? i420.getWidth() : i420.getHeight();
            final int height = rotation % 180 == 0 ? i420.getHeight() : i420.getWidth();

            final ByteBuffer yPlane;
            final ByteBuffer uPlane;
            final ByteBuffer vPlane;
            final int strideY;
            final int strideUV;
            if (rotation != 0) {
                // rotate into a packed I420 buffer, then slice the planes out
                final ByteBuffer packed = ByteBuffer.allocateDirect(
                        width * height + 2 * ((width + 1) / 2) * ((height + 1) / 2));
                YuvHelper.I420Rotate(i420.getDataY(), i420.getStrideY(),
                        i420.getDataU(), i420.getStrideU(),
                        i420.getDataV(), i420.getStrideV(),
                        packed, i420.getWidth(), i420.getHeight(), rotation);
                final int chromaSize = ((width + 1) / 2) * ((height + 1) / 2);
                packed.position(0);
                yPlane = packed.slice(); yPlane.limit(width * height);
                packed.position(width * height);
                uPlane = packed.slice(); uPlane.limit(chromaSize);
                packed.position(width * height + chromaSize);
                vPlane = packed.slice(); vPlane.limit(chromaSize);
                strideY = width;
                strideUV = (width + 1) / 2;
            } else {
                yPlane = i420.getDataY();
                uPlane = i420.getDataU();
                vPlane = i420.getDataV();
                strideY = i420.getStrideY();
                strideUV = i420.getStrideU();
            }

            final byte[] nv21 = i420ToNv21(yPlane, strideY, uPlane, strideUV,
                    vPlane, rotation != 0 ? strideUV : i420.getStrideV(),
                    width, height);
            final YuvImage image = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            image.compressToJpeg(new Rect(0, 0, width, height), 100, out);
            return out.toByteArray();
        } finally {
            i420.release();
        }
    }

    /**
     * Tightly packed NV21 (Y + interleaved VU, what {@link YuvImage} needs)
     * out of stride-aware I420 planes. Pure Java - unit-testable without a
     * device.
     */
    static byte[] i420ToNv21(final ByteBuffer y, final int strideY,
                             final ByteBuffer u, final int strideU,
                             final ByteBuffer v, final int strideV,
                             final int width, final int height) {
        final int chromaW = (width + 1) / 2;
        final int chromaH = (height + 1) / 2;
        final byte[] out = new byte[width * height + 2 * chromaW * chromaH];
        for (int row = 0; row < height; row++) {
            y.position(row * strideY);
            y.get(out, row * width, width);
        }
        int offset = width * height;
        for (int row = 0; row < chromaH; row++) {
            v.position(row * strideV);
            u.position(row * strideU);
            for (int col = 0; col < chromaW; col++) {
                out[offset++] = v.get();
                out[offset++] = u.get();
            }
        }
        return out;
    }

    /**
     * One JPEG through the legacy pipeline: send-back to the requesting GCS
     * (same as SendBackImages today), optional save next to the trip images
     * (MediaStore Downloads on API 29+, files dir below) with a GPS EXIF tag,
     * then the camera.captured fact for the capability provider.
     */
    private void processJpeg(final String ch, final byte[] jpeg, final int index,
                             final boolean saveLocal, final boolean sendBack,
                             final AndruavUnitBase sendBackTo,
                             final boolean gcsPreview, final String invokeId) {
        final Location location = currentLocation();

        if (sendBack) {
            try {
                AndruavFacade.sendImage(jpeg, location, sendBackTo);
            } catch (Exception e) {
                AndruavEngine.log().logException("cam_send", e);
            }
        }

        String savedName = null;
        if (saveLocal) {
            savedName = saveJpeg(jpeg, location);
        }

        if (savedName != null) {
            // Same bookkeeping event the FPV activity used to post, so the
            // KML handler still picks headless shots up.
            try {
                final Event_FPV_Image event = new Event_FPV_Image();
                event.isLocalImage = true;
                event.isVideo = false;
                event.ImageFile = new File(savedName);
                event.ImageLocation = location;
                event.Description = "Image No#" + index;
                EventBus.getDefault().post(event);
            } catch (Exception e) {
                AndruavEngine.log().logException("cam_evt", e);
            }
        }

        final double[] pos = location != null
                ? new double[]{location.getLatitude(), location.getLongitude(),
                               location.getAltitude()}
                : null;
        fireCaptured(ch, savedName != null ? savedName : "", pos,
                System.currentTimeMillis(), index, invokeId);
    }

    /** The unit's position for geotagging; null when no fix is available. */
    private static Location currentLocation() {
        try {
            final AndruavLocation location =
                    AndruavSettings.andruavWe7daBase.getAvailableLocation();
            if (location != null && (location.getLatitude() != 0 || location.getLongitude() != 0)) {
                return location;
            }
        } catch (Exception e) {
            AndruavEngine.log().logException("cam_pos", e);
        }
        return null;
    }

    /**
     * Persist a JPEG next to today's captures (the KML trip image folder on
     * both storage models), falling back to a plain "AndruavImgs" folder
     * when no trip file is open. Returns the display/file name saved.
     */
    private static String saveJpeg(final byte[] jpeg, final Location location) {
        final String name = FileHelper.buildTimestampedJpgName(null);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                String relative = null;
                if (App.KMLFile != null) relative = App.KMLFile.getImageRelativePath();
                if (relative == null) relative = "Download/AndruavImgs";
                final Uri uri = FileHelper.createDownloadsEntry(relative, name, "image/jpeg");
                if (uri == null) return null;
                final Context context = App.getAppContext();
                try (OutputStream os = context.getContentResolver().openOutputStream(uri)) {
                    if (os == null) return null;
                    os.write(jpeg);
                }
                FileHelper.markMediaStoreEntryComplete(uri);
                if (location != null) {
                    try (ParcelFileDescriptor pfd =
                                 context.getContentResolver().openFileDescriptor(uri, "rw")) {
                        if (pfd != null) {
                            Image_Helper.AddGPStoJpg(pfd.getFileDescriptor(), location);
                        }
                    }
                }
                return name;
            }

            File folder = null;
            if (App.KMLFile != null) folder = App.KMLFile.getImageFolder();
            if (folder == null) folder = FileHelper.GetFolder("AndruavImgs", null);
            final File image = new File(folder, name);
            try (FileOutputStream fos = new FileOutputStream(image)) {
                fos.write(jpeg);
            }
            if (location != null) {
                Image_Helper.AddGPStoJpg(image.getAbsolutePath(), location);
            }
            return image.getAbsolutePath();
        } catch (IOException e) {
            AndruavEngine.log().logException("exception_img", e);
            return null;
        }
    }

    // ---- recording (the setRecording choke point) ------------------------------

    private final Object mRecordLock = new Object();
    private final java.util.Map<String, Boolean> mRecording = new java.util.HashMap<>();
    private final java.util.Map<String, String> mRecordingFile = new java.util.HashMap<>();
    private final ExecutorService mControlExecutor = Executors.newSingleThreadExecutor();

    private boolean recordingFlag(final String ch) {
        synchronized (mRecordLock) {
            final Boolean recording = mRecording.get(ch);
            return recording != null && recording;
        }
    }

    @Override
    public boolean isRecording(final String ch) {
        return recordingFlag(ch);
    }

    @Override
    public boolean anyRecording() {
        synchronized (mRecordLock) {
            for (final Boolean recording : mRecording.values()) {
                if (recording != null && recording) return true;
            }
            return false;
        }
    }

    /** Async entry for bus/UI callers that must not block (record over the bus). */
    public void queueSetRecording(final String ch, final boolean on, final String src) {
        mControlExecutor.execute(() -> setRecording(ch, on, src, new String[1]));
    }

    /**
     * THE recording choke point - every start/stop path ends here:
     * GCS RECORDVIDEO, capability invoke, Sync_EventFire command, recorder
     * fault, module shutdown. Edge-only: a no-change call is a silent no-op
     * (idempotent, matching the Linux record_on/record_off semantics).
     */
    @Override
    public boolean setRecording(final String channelKey, final boolean on,
                                final String src, final String[] err) {
        final String ch = resolveChannelKey(channelKey);
        if (ch == null) {
            if (err != null && err.length > 0) err[0] = "unknown camera channel '" + channelKey + "'";
            return false;
        }

        synchronized (mRecordLock) {
            final boolean wasRecording = recordingFlag(ch);
            if (wasRecording == on) {
                // no edge - report current state without emitting facts
                return true;
            }
        }

        StreamingLink link = mStreamingLink;
        if (on && (link == null || !link.isCapturing())) {
            // Recording encodes the capturer's frames: bring the streaming
            // service up first, the same way a remote video request does.
            try {
                App.startFPVStreamingService();
            } catch (Exception e) {
                if (err != null && err.length > 0) err[0] = "cannot start camera service: " + e;
                return false;
            }
            awaitStreamingCapture(RECORD_STREAM_TIMEOUT_MS);
            link = mStreamingLink;
        }

        if (link == null || !link.isCapturing()) {
            if (err != null && err.length > 0) {
                err[0] = "no camera capture running - recording needs the video pipeline";
            }
            return false;
        }

        // The WebRTC capturer owns exactly one camera: record only the
        // channel it is on.
        final String active = activeChannelKey();
        if (!ch.equals(active)) {
            if (err != null && err.length > 0) {
                err[0] = "channel '" + ch + "' is not the active stream ('" + active
                        + "'); switch the camera first";
            }
            return false;
        }

        final String file;
        if (on) {
            file = link.startVideoRecording();
            if (file == null) {
                if (err != null && err.length > 0) err[0] = "recorder failed to start";
                return false;
            }
        } else {
            file = link.stopVideoRecording();
        }

        synchronized (mRecordLock) {
            mRecording.put(ch, on);
            if (on) {
                mRecordingFile.put(ch, file);
            }
        }

        final String reason = SRC_FAULT.equals(src) ? "fault"
                : SRC_SHUTDOWN.equals(src) ? "shutdown" : "command";
        fireRecordEdge(ch, on, src, reason, file);
        return true;
    }

    /** Recorder fault hook: goes through the choke point as cause=fault. */
    public void notifyRecorderFault(final String ch) {
        setRecording(ch != null ? ch : activeChannelKey(), false, SRC_FAULT, new String[1]);
    }

    // ---- zoom ------------------------------------------------------------------

    /** Stored per-channel level applied to the next headless Camera2 still. */
    private final java.util.Map<String, Double> mPendingZoom = new java.util.HashMap<>();

    private double pendingZoom(final String ch) {
        synchronized (mPendingZoom) {
            final Double level = mPendingZoom.get(ch);
            return level != null ? level : 1.0;
        }
    }

    /**
     * zoom {level 1..10} - a linear zoom factor. On the live stream it maps
     * onto the capturer's 0..1 fraction of SCALER_AVAILABLE_MAX_DIGITAL_ZOOM;
     * headless it is remembered and applied as the still's crop region.
     */
    @Override
    public boolean setZoom(final String channelKey, final double level, final String[] err) {
        final String ch = resolveChannelKey(channelKey);
        if (ch == null) {
            if (err != null && err.length > 0) err[0] = "unknown camera channel '" + channelKey + "'";
            return false;
        }
        final Channel channel = channelByKey(ch);
        if (channel == null) {
            if (err != null && err.length > 0) err[0] = "channel '" + ch + "' has no camera";
            return false;
        }

        final StreamingLink link = mStreamingLink;
        if (link != null && link.isCapturing() && ch.equals(activeChannelKey())) {
            if (channel.mMaxZoom > 1.0) {
                final float fraction = (float) Math.min(1.0,
                        Math.max(0.0, (level - 1.0) / (channel.mMaxZoom - 1.0)));
                link.setZoomFraction(fraction);
            } else {
                link.setZoomFraction(0f);
            }
            return true;
        }

        synchronized (mPendingZoom) {
            mPendingZoom.put(ch, level);
        }
        return true;
    }

    // ---- lifecycle --------------------------------------------------------------

    /** Module/service shutdown: finalize any recording with reason=shutdown. */
    public void shutdown() {
        ensureChannels();
        for (final Channel channel : new ArrayList<>(mChannels)) {
            if (recordingFlag(channel.key)) {
                setRecording(channel.key, false, SRC_SHUTDOWN, new String[1]);
            }
        }
    }
}
