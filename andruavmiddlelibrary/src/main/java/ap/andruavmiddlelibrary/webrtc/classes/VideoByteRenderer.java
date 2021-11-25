package ap.andruavmiddlelibrary.webrtc.classes;

import android.os.Handler;
import android.os.HandlerThread;

import org.webrtc.EglBase;
import org.webrtc.ThreadUtils;
import org.webrtc.VideoFrame;
import org.webrtc.VideoSink;
import org.webrtc.YuvHelper;

import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

import ap.andruavmiddlelibrary.com.serenegiant.encoder.MediaVideoEncoder;

public class VideoByteRenderer implements VideoSink {
    private static final String TAG = "VideoFileRenderer";
    private final HandlerThread renderThread;
    private final Handler renderThreadHandler;
    //private final FileOutputStream videoOutFile;
    //private final String outputFileName;
    private final int outputFileWidth;
    private final int outputFileHeight;
    private final int outputFrameSize;
    //private final int outputFrameSize2;
    private final ByteBuffer outputFrameBuffer;
    private final ByteBuffer outputFrameBuffer2;
    private final VSink iVideoSink;

    public VideoByteRenderer(VSink videoSink, int outputFileWidth, int outputFileHeight, final EglBase.Context sharedContext) throws IllegalArgumentException {
        if (outputFileWidth % 2 != 1 && outputFileHeight % 2 != 1) {
            //this.outputFileName = outputFile;
            iVideoSink = videoSink;
            this.outputFileWidth = outputFileWidth;
            this.outputFileHeight = outputFileHeight;
            this.outputFrameSize = outputFileWidth * outputFileHeight * 3 / 2;
            //this.outputFrameSize2 = outputFileWidth * outputFileHeight * 3 / 2;
            //final int chromaHeight = (outputFileHeight + 1) / 2;

            this.outputFrameBuffer = ByteBuffer.allocateDirect(this.outputFrameSize);
            this.outputFrameBuffer2 = ByteBuffer.allocateDirect(this.outputFrameSize);
            //this.outputFrameBuffer2 = ByteBuffer.allocateDirect(this.outputFrameSize2);

            //this.videoOutFile = new FileOutputStream(outputFile);
            //this.videoOutFile.write(("YUV4MPEG2 C420 W" + outputFileWidth + " H" + outputFileHeight + " Ip F30:1 A1:1\n").getBytes(Charset.forName("US-ASCII")));
            this.renderThread = new HandlerThread("VideoFileRendererRenderThread");
            this.renderThread.start();
            this.renderThreadHandler = new Handler(this.renderThread.getLooper());

        } else {
            throw new IllegalArgumentException("Does not support uneven width or height");
        }


    }

    public void onFrame(VideoFrame frame) {
        frame.retain();
        this.renderThreadHandler.post(() -> this.renderFrameOnRenderThread(frame));
    }



    private void renderFrameOnRenderThread(VideoFrame frame) {
        VideoFrame.Buffer buffer = frame.getBuffer();
        int targetWidth = frame.getRotation() % 180 == 0 ? this.outputFileWidth : this.outputFileHeight;
        int targetHeight = frame.getRotation() % 180 == 0 ? this.outputFileHeight : this.outputFileWidth;

        //int targetWidth = this.outputFileHeight;
        //int targetHeight = this.outputFileWidth;
        float frameAspectRatio = (float)buffer.getWidth() / (float)buffer.getHeight();
        float fileAspectRatio = (float)targetWidth / (float)targetHeight;
        int cropWidth = buffer.getWidth();
        int cropHeight = buffer.getHeight();
        if (fileAspectRatio > frameAspectRatio) {
            cropHeight = (int)((float)cropHeight * (frameAspectRatio / fileAspectRatio));
        } else {
            cropWidth = (int)((float)cropWidth * (fileAspectRatio / frameAspectRatio));
        }
        int cropX = (buffer.getWidth() - cropWidth) >> 1;
        int cropY = (buffer.getHeight() - cropHeight) >> 1;
        VideoFrame.Buffer scaledBuffer = buffer.cropAndScale(cropX, cropY, cropWidth, cropHeight, targetWidth, targetHeight);
        frame.release();
        VideoFrame.I420Buffer i420 = scaledBuffer.toI420();
        scaledBuffer.release();

        if (MediaVideoEncoder.VIDEO_FORMAT == MediaVideoEncoder.MOBILE_MATE10_STYLE)
        {
            YuvHelper.I420ToNV12(i420.getDataY(), i420.getStrideY(), i420.getDataU(), i420.getStrideU(), i420.getDataV(), i420.getStrideV(),this.outputFrameBuffer2,i420.getWidth(), i420.getHeight());
            i420.release();
            iVideoSink.onFrame(this.outputFrameBuffer2.array(),this.outputFrameBuffer2.arrayOffset(), this.outputFrameSize);
        }
        else
        if (MediaVideoEncoder.VIDEO_FORMAT == MediaVideoEncoder.MOBILE_S5_STYLE)
        {
            YuvHelper.I420ToNV12(i420.getDataY(), i420.getStrideY(), i420.getDataU(), i420.getStrideU(), i420.getDataV(), i420.getStrideV(),this.outputFrameBuffer2,i420.getWidth(), i420.getHeight());
            i420.release();
            iVideoSink.onFrame(this.outputFrameBuffer2.array(),this.outputFrameBuffer2.arrayOffset(), this.outputFrameSize);
        }
        else
        if (MediaVideoEncoder.VIDEO_FORMAT == MediaVideoEncoder.MOBILE_MATE8_STYLE)
        {
            YuvHelper.I420ToNV12(i420.getDataY(), i420.getStrideY(), i420.getDataV(), i420.getStrideV(),i420.getDataU(), i420.getStrideU(), this.outputFrameBuffer2,i420.getWidth(), i420.getHeight());
            i420.release();
            iVideoSink.onFrame(this.outputFrameBuffer2.array(),this.outputFrameBuffer2.arrayOffset(), this.outputFrameSize);
        }
        else
        if (MediaVideoEncoder.VIDEO_FORMAT == MediaVideoEncoder.MOBILE_OPPO_F11_STYLE)
        {
            YuvHelper.I420Rotate(i420.getDataY(), i420.getStrideY(),i420.getDataU(), i420.getStrideU(), i420.getDataV(), i420.getStrideV(), this.outputFrameBuffer, i420.getWidth(), i420.getHeight(), frame.getRotation());
            i420.release();
            iVideoSink.onFrame(this.outputFrameBuffer.array(),this.outputFrameBuffer.arrayOffset(), this.outputFrameSize);
        }
        else
        {
            YuvHelper.I420Rotate(i420.getDataY(), i420.getStrideY(),i420.getDataU(), i420.getStrideU(), i420.getDataV(), i420.getStrideV(), this.outputFrameBuffer, i420.getWidth(), i420.getHeight(), frame.getRotation());
            i420.release();
            iVideoSink.onFrame(this.outputFrameBuffer.array(),this.outputFrameBuffer.arrayOffset(), this.outputFrameSize);
        }

    }




    public void release() {
        CountDownLatch cleanupBarrier = new CountDownLatch(1);
        this.renderThreadHandler.post(() -> {
            //this.yuvConverter.release();
            this.renderThread.quit();
            cleanupBarrier.countDown();
        });
        ThreadUtils.awaitUninterruptibly(cleanupBarrier);

    }
}