package ap.andruavmiddlelibrary.webrtc.classes;

import android.os.Handler;
import android.os.HandlerThread;

import org.webrtc.EglBase;
import org.webrtc.Logging;
import org.webrtc.ThreadUtils;
import org.webrtc.VideoFrame;
import org.webrtc.VideoSink;
import org.webrtc.YuvHelper;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;

public class AndruavVideoFileRenderer  implements VideoSink {
    private static final String TAG = "VideoFileRenderer";
    private final HandlerThread renderThread;
    private final Handler renderThreadHandler;
    private final HandlerThread fileThread;
    private final Handler fileThreadHandler;
    private final FileOutputStream videoOutFile;
    private final String outputFileName;
    private final int outputFileWidth;
    private final int outputFileHeight;
    private final int outputFrameSize;
    private final ByteBuffer outputFrameBuffer;
    private EglBase eglBase;
   // private YuvConverter yuvConverter;
    private int frameCount;

    public AndruavVideoFileRenderer(String outputFile, int FPS,  int outputFileWidth, int outputFileHeight, final EglBase.Context sharedContext) throws IOException {
        if ((outputFileWidth % 2 != 1) && (outputFileHeight % 2 != 1)) {
            this.outputFileName = outputFile;
            this.outputFileWidth = outputFileWidth;
            this.outputFileHeight = outputFileHeight;
            this.outputFrameSize = outputFileWidth * outputFileHeight * 3 / 2;
            this.outputFrameBuffer = ByteBuffer.allocateDirect(this.outputFrameSize);
            this.videoOutFile = new FileOutputStream(outputFile);
            this.videoOutFile.write(("YUV4MPEG2 C420 W" + outputFileWidth + " H" + outputFileHeight + " Ip F"+ FPS +":1 A1:1\n").getBytes(StandardCharsets.US_ASCII));
            this.renderThread = new HandlerThread("VideoFileRendererRenderThread");
            this.renderThread.start();
            this.renderThreadHandler = new Handler(this.renderThread.getLooper());
            this.fileThread = new HandlerThread("VideoFileRendererFileThread");
            this.fileThread.start();
            this.fileThreadHandler = new Handler(this.fileThread.getLooper());
//            ThreadUtils.invokeAtFrontUninterruptibly(this.renderThreadHandler, new Runnable() {
//                public void run() {
//                    AndruavVideoFileRenderer.this.eglBase = EglBase.create(sharedContext, EglBase.CONFIG_PIXEL_BUFFER);
//                    AndruavVideoFileRenderer.this.eglBase.createDummyPbufferSurface();
//                    AndruavVideoFileRenderer.this.eglBase.makeCurrent();
//                    AndruavVideoFileRenderer.this.yuvConverter = new YuvConverter();
//                }
//            });
        } else {
            throw new IllegalArgumentException("Does not support uneven width or height");
        }
    }

    public void onFrame(VideoFrame frame) {
        frame.retain();
        this.renderThreadHandler.post(() -> {
            this.renderFrameOnRenderThread(frame);
        });
    }

    private void renderFrameOnRenderThread(VideoFrame frame) {
        VideoFrame.Buffer buffer = frame.getBuffer();
        int targetWidth = frame.getRotation() % 180 == 0 ? this.outputFileWidth : this.outputFileHeight;
        int targetHeight = frame.getRotation() % 180 == 0 ? this.outputFileHeight : this.outputFileWidth;
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
            YuvHelper.I420Rotate(i420.getDataY(), i420.getStrideY(), i420.getDataU(), i420.getStrideU(), i420.getDataV(), i420.getStrideV(), this.outputFrameBuffer, i420.getWidth(), i420.getHeight(), frame.getRotation());
            i420.release();

            try {
                this.videoOutFile.write("FRAME\n".getBytes(StandardCharsets.US_ASCII));
                this.videoOutFile.write(this.outputFrameBuffer.array(), this.outputFrameBuffer.arrayOffset(), this.outputFrameSize);
            } catch (IOException var4) {
                throw new RuntimeException("Error writing video to disk", var4);
            }

            ++this.frameCount;
        });
    }

    public void release() {
        CountDownLatch cleanupBarrier = new CountDownLatch(1);
        this.renderThreadHandler.post(() -> {
            //this.yuvConverter.release();
            //this.eglBase.release();
            this.renderThread.quit();
            cleanupBarrier.countDown();
        });
        ThreadUtils.awaitUninterruptibly(cleanupBarrier);
        this.fileThreadHandler.post(() -> {
            try {
                this.videoOutFile.close();
                Logging.d("VideoFileRenderer", "Video written to disk as " + this.outputFileName + ". The number of frames is " + this.frameCount + " and the dimensions of the frames are " + this.outputFileWidth + "x" + this.outputFileHeight + ".");
            } catch (IOException var2) {
                throw new RuntimeException("Error closing output file", var2);
            }

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
