package ap.andruavmiddlelibrary.webrtc.classes;

import android.media.Image;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.ParcelFileDescriptor;

import androidx.annotation.RequiresApi;

import org.webrtc.EglBase;
import org.webrtc.Logging;
import org.webrtc.ThreadUtils;
import org.webrtc.VideoFrame;
import org.webrtc.VideoSink;
import org.webrtc.YuvHelper;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Records the FPV track to an H.264 MP4: frames are cropped/scaled and
 * rotated to the output size on the render thread, then encoded with a
 * MediaCodec AVC encoder (flexible YUV input) and muxed with MediaMuxer on
 * the file thread. Back-pressure drops frames instead of queueing them.
 */
public class AndruavVideoFileRenderer  implements VideoSink {
    private static final String TAG = "VideoFileRenderer";
    private static final int MAX_PENDING_FRAMES = 2;
    private static final long DROP_LOG_INTERVAL_MS = 1000;
    private static final String MIME_AVC = MediaFormat.MIMETYPE_VIDEO_AVC;
    private static final long CODEC_TIMEOUT_US = 10000;
    /** EOS drain gives up after this many empty polls (x CODEC_TIMEOUT_US). */
    private static final int EOS_MAX_EMPTY_POLLS = 200;
    private final HandlerThread renderThread;
    private final Handler renderThreadHandler;
    private final HandlerThread fileThread;
    private final Handler fileThreadHandler;
    private final String outputFileName;
    private final int outputFileWidth;
    private final int outputFileHeight;
    private final int outputFrameSize;
    private final ByteBuffer outputFrameBuffer;
    private final byte[] rowBuffer;
    private final MediaCodec encoder;
    private final MediaMuxer muxer;
    /** MediaStore entry backing the muxer (API 29+), closed after the muxer. */
    private final ParcelFileDescriptor outputDescriptor;
    private final MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
    private int muxerTrack = -1;
    private boolean muxerStarted;
    private long firstTimestampNs = -1;
    private long lastPresentationUs = -1;
    private int frameCount;
    private final AtomicInteger pendingFrames = new AtomicInteger(0);
    private final AtomicInteger droppedFrames = new AtomicInteger(0);
    private long lastDropLogTimeMs = 0;

    public AndruavVideoFileRenderer(String outputFile, int FPS,  int outputFileWidth, int outputFileHeight, final EglBase.Context sharedContext) throws IOException {
        this(new MediaMuxer(outputFile, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4), null,
                outputFile, FPS, outputFileWidth, outputFileHeight);
    }

    /***
     * Scoped-storage (API 29+) variant: muxes into an already-open MediaStore entry. MP4 needs a
     * seekable target (the muxer rewrites the header on stop), so this takes a "rw" descriptor
     * from ContentResolver.openFileDescriptor() rather than an OutputStream. Takes ownership of
     * the descriptor.
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public AndruavVideoFileRenderer(ParcelFileDescriptor outputDescriptor, String outputFileName, int FPS, int outputFileWidth, int outputFileHeight, final EglBase.Context sharedContext) throws IOException {
        this(new MediaMuxer(outputDescriptor.getFileDescriptor(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4),
                outputDescriptor, outputFileName, FPS, outputFileWidth, outputFileHeight);
    }

    private AndruavVideoFileRenderer(MediaMuxer muxer, ParcelFileDescriptor outputDescriptor, String outputFileName,
                                     int FPS, int outputFileWidth, int outputFileHeight) throws IOException {
        if ((outputFileWidth % 2 == 1) || (outputFileHeight % 2 == 1)) {
            muxer.release();
            throw new IllegalArgumentException("Does not support uneven width or height");
        }
        this.muxer = muxer;
        this.outputDescriptor = outputDescriptor;
        this.outputFileName = outputFileName;
        this.outputFileWidth = outputFileWidth;
        this.outputFileHeight = outputFileHeight;
        this.outputFrameSize = outputFileWidth * outputFileHeight * 3 / 2;
        // YuvHelper.I420Rotate writes through JNI - the destination must be direct
        this.outputFrameBuffer = ByteBuffer.allocateDirect(this.outputFrameSize);
        this.rowBuffer = new byte[outputFileWidth];

        final MediaFormat format = MediaFormat.createVideoFormat(MIME_AVC, outputFileWidth, outputFileHeight);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate(outputFileWidth, outputFileHeight, FPS));
        format.setInteger(MediaFormat.KEY_FRAME_RATE, FPS);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
        MediaCodec codec = null;
        try {
            codec = MediaCodec.createEncoderByType(MIME_AVC);
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            codec.start();
        } catch (IOException | RuntimeException e) {
            if (codec != null) codec.release();
            muxer.release();
            throw new IOException("H.264 encoder unavailable for " + outputFileWidth + "x" + outputFileHeight, e);
        }
        this.encoder = codec;

        this.renderThread = new HandlerThread("VideoFileRendererRenderThread");
        this.renderThread.start();
        this.renderThreadHandler = new Handler(this.renderThread.getLooper());
        this.fileThread = new HandlerThread("VideoFileRendererFileThread");
        this.fileThread.start();
        this.fileThreadHandler = new Handler(this.fileThread.getLooper());
    }

    /** ~0.2 bit per pixel per frame (720p@15 = 2.8 Mbps), 1-8 Mbps. */
    static int bitRate(final int width, final int height, final int fps) {
        final long bps = (long) (width * (long) height * fps * 0.2);
        return (int) Math.max(1_000_000L, Math.min(8_000_000L, bps));
    }

    public void onFrame(VideoFrame frame) {
        if (this.pendingFrames.get() >= MAX_PENDING_FRAMES) {
            this.logDroppedFrame();
            return;
        }
        this.pendingFrames.incrementAndGet();
        frame.retain();
        this.renderThreadHandler.post(() -> {
            this.renderFrameOnRenderThread(frame);
        });
    }

    private void logDroppedFrame() {
        int dropped = this.droppedFrames.incrementAndGet();
        long now = System.currentTimeMillis();
        if (now - this.lastDropLogTimeMs >= DROP_LOG_INTERVAL_MS) {
            this.lastDropLogTimeMs = now;
            Logging.w(TAG, "Recording backpressure: dropping frame, writer cannot keep up (" + dropped + " frames dropped so far)");
        }
    }

    private void renderFrameOnRenderThread(VideoFrame frame) {
        VideoFrame.Buffer buffer = frame.getBuffer();
        final int rotation = frame.getRotation();
        final long timestampNs = frame.getTimestampNs();
        int targetWidth = rotation % 180 == 0 ? this.outputFileWidth : this.outputFileHeight;
        int targetHeight = rotation % 180 == 0 ? this.outputFileHeight : this.outputFileWidth;
        float frameAspectRatio = (float)buffer.getWidth() / (float)buffer.getHeight();
        float fileAspectRatio = (float)targetWidth / (float)targetHeight;
        int cropWidth = buffer.getWidth();
        int cropHeight = buffer.getHeight();
        if (fileAspectRatio > frameAspectRatio) {
            cropHeight = (int)((float)cropHeight * (frameAspectRatio / fileAspectRatio));
        } else {
            cropWidth = (int)((float)cropWidth * (fileAspectRatio / frameAspectRatio));
        }

        int cropX = (buffer.getWidth() - cropWidth) / 2;
        int cropY = (buffer.getHeight() - cropHeight) / 2;
        VideoFrame.Buffer scaledBuffer = buffer.cropAndScale(cropX, cropY, cropWidth, cropHeight, targetWidth, targetHeight);
        frame.release();
        VideoFrame.I420Buffer i420 = scaledBuffer.toI420();
        scaledBuffer.release();
        this.fileThreadHandler.post(() -> {
            try {
                YuvHelper.I420Rotate(i420.getDataY(), i420.getStrideY(), i420.getDataU(), i420.getStrideU(), i420.getDataV(), i420.getStrideV(), this.outputFrameBuffer, i420.getWidth(), i420.getHeight(), rotation);
                i420.release();
                encodeFrame(timestampNs);
            } catch (RuntimeException e) {
                Logging.e(TAG, "Error encoding video frame", e);
            } finally {
                this.pendingFrames.decrementAndGet();
            }
        });
    }

    /** File thread: outputFrameBuffer (packed I420, output size) -> encoder -> muxer. */
    private void encodeFrame(final long timestampNs) {
        final int index = this.encoder.dequeueInputBuffer(CODEC_TIMEOUT_US);
        if (index < 0) {
            this.logDroppedFrame();
            return;
        }
        final Image image = this.encoder.getInputImage(index);
        if (image != null) {
            fillImage(image);
        } else {
            // no Image view: the flexible format falls back to planar I420
            final ByteBuffer input = this.encoder.getInputBuffer(index);
            final ByteBuffer src = this.outputFrameBuffer.duplicate();
            src.clear();
            input.clear();
            input.put(src);
        }

        if (this.firstTimestampNs < 0) this.firstTimestampNs = timestampNs;
        long presentationUs = (timestampNs - this.firstTimestampNs) / 1000;
        // the muxer rejects non-increasing timestamps
        if (presentationUs <= this.lastPresentationUs) presentationUs = this.lastPresentationUs + 1;
        this.lastPresentationUs = presentationUs;

        this.encoder.queueInputBuffer(index, 0, this.outputFrameSize, presentationUs, 0);
        ++this.frameCount;
        drainEncoder(false);
    }

    /** Copies the packed I420 frame into the encoder's planes, honouring their strides. */
    private void fillImage(final Image image) {
        final Image.Plane[] planes = image.getPlanes();
        final int chromaWidth = this.outputFileWidth / 2;
        final int chromaHeight = this.outputFileHeight / 2;
        final int ySize = this.outputFileWidth * this.outputFileHeight;
        final int chromaSize = chromaWidth * chromaHeight;
        copyPlane(0, this.outputFileWidth, this.outputFileHeight, planes[0]);
        copyPlane(ySize, chromaWidth, chromaHeight, planes[1]);
        copyPlane(ySize + chromaSize, chromaWidth, chromaHeight, planes[2]);
    }

    private void copyPlane(final int srcOffset, final int width, final int height, final Image.Plane plane) {
        final ByteBuffer src = this.outputFrameBuffer.duplicate();
        final ByteBuffer dst = plane.getBuffer();
        final int rowStride = plane.getRowStride();
        final int pixelStride = plane.getPixelStride();
        for (int row = 0; row < height; row++) {
            src.position(srcOffset + row * width);
            src.get(this.rowBuffer, 0, width);
            final int rowStart = row * rowStride;
            if (pixelStride == 1) {
                dst.position(rowStart);
                dst.put(this.rowBuffer, 0, width);
            } else {
                for (int col = 0; col < width; col++) {
                    dst.put(rowStart + col * pixelStride, this.rowBuffer[col]);
                }
            }
        }
    }

    /**
     * File thread: moves encoded output into the muxer. With endOfStream it
     * waits for the encoder's EOS buffer (bounded); otherwise it takes only
     * what is ready now.
     */
    private void drainEncoder(final boolean endOfStream) {
        int emptyPolls = 0;
        while (true) {
            final int index = this.encoder.dequeueOutputBuffer(this.bufferInfo, endOfStream ? CODEC_TIMEOUT_US : 0);
            if (index == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (!endOfStream || (++emptyPolls >= EOS_MAX_EMPTY_POLLS)) return;
                continue;
            }
            if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                if (!this.muxerStarted) {
                    this.muxerTrack = this.muxer.addTrack(this.encoder.getOutputFormat());
                    this.muxer.start();
                    this.muxerStarted = true;
                }
                continue;
            }
            if (index < 0) continue;

            final ByteBuffer encoded = this.encoder.getOutputBuffer(index);
            final boolean config = (this.bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0;
            if (!config && (this.bufferInfo.size > 0) && this.muxerStarted && (encoded != null)) {
                encoded.position(this.bufferInfo.offset);
                encoded.limit(this.bufferInfo.offset + this.bufferInfo.size);
                this.muxer.writeSampleData(this.muxerTrack, encoded, this.bufferInfo);
            }
            this.encoder.releaseOutputBuffer(index, false);
            if ((this.bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) return;
        }
    }

    /** File thread: flush the encoder and finalize the MP4. */
    private void finishOnFileThread() {
        try {
            final int index = this.encoder.dequeueInputBuffer(CODEC_TIMEOUT_US * 10);
            if (index >= 0) {
                this.encoder.queueInputBuffer(index, 0, 0, Math.max(0, this.lastPresentationUs + 1),
                        MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                drainEncoder(true);
            }
        } catch (RuntimeException e) {
            Logging.e(TAG, "Error flushing video encoder", e);
        }
        try {
            this.encoder.stop();
        } catch (RuntimeException e) {
            Logging.e(TAG, "Error stopping video encoder", e);
        }
        this.encoder.release();
        try {
            // stop() writes the MP4 header; it throws when no sample was written
            if (this.muxerStarted) this.muxer.stop();
        } catch (RuntimeException e) {
            Logging.e(TAG, "Error finalizing " + this.outputFileName, e);
        }
        this.muxer.release();
        if (this.outputDescriptor != null) {
            try {
                this.outputDescriptor.close();
            } catch (IOException e) {
                Logging.e(TAG, "Error closing " + this.outputFileName, e);
            }
        }
        Logging.d(TAG, "Video written to disk as " + this.outputFileName + ". The number of frames is " + this.frameCount + " (" + this.droppedFrames.get() + " dropped by backpressure) and the dimensions of the frames are " + this.outputFileWidth + "x" + this.outputFileHeight + ".");
    }

    public void release() {
        CountDownLatch cleanupBarrier = new CountDownLatch(1);
        this.renderThreadHandler.post(() -> {
            this.renderThread.quit();
            cleanupBarrier.countDown();
        });
        ThreadUtils.awaitUninterruptibly(cleanupBarrier);
        this.fileThreadHandler.post(() -> {
            finishOnFileThread();
            this.fileThread.quit();
        });

        try {
            this.fileThread.join();
        } catch (InterruptedException var3) {
            Thread.currentThread().interrupt();
            Logging.e("VideoFileRenderer", "Interrupted while waiting for the write to disk to complete.", var3);
        }

    }
}
