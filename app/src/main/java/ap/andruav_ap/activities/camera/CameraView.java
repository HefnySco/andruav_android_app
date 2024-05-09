package ap.andruav_ap.activities.camera;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.andruav.AndruavEngine;

import java.util.List;

import ap.andruav_ap.DeviceManagerFacade;
import com.andruav.notification.PanicFacade;
import ap.andruavmiddlelibrary.preference.Preference;
import com.andruav.FeatureSwitch;
import ap.andruavmiddlelibrary.factory.util.MemoryHelper;

public class CameraView extends SurfaceView implements SurfaceHolder.Callback{

    public interface CameraReadyCallback {
        void onCameraReady();
    }

    public boolean isReleased = true;
    public byte[] buffer;
    private int bufferSize;

    public Camera mcameraHW = null;
    private SurfaceHolder surfaceHolder_ = null;
    CameraReadyCallback cameraReadyCallback_ = null;

    private List<Camera.Size> supportedSizes;
    private Camera.Size mpreviewSize;
    private final boolean inProcessing_ = false;
    private PreviewCallback mpreviewCallback;


    /***
     * Android Camera Preview Stretched
     * {@link "http://stackoverflow.com/questions/19577299/android-camera-preview-stretched"}
     * @param widthMeasureSpec
     * @param heightMeasureSpec
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if ((mcameraHW == null) || (isInEditMode())) {super.onMeasure(widthMeasureSpec,heightMeasureSpec); return; }

         int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
         int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
         setMeasuredDimension(width, height);
        if (supportedSizes != null) {
            // camera has been created and queried for available cam preview sizes

            mpreviewSize = getOptimalPreviewSize(supportedSizes, FeatureSwitch.cameraWidths[Preference.getVideoImageSizeQuality(null)], FeatureSwitch.cameraHeights[Preference.getVideoImageSizeQuality(null)]);
            // Hefny HACK ; 1.0.56
            StopPreview();
            Camera.Parameters p = mcameraHW.getParameters();
            p.setPreviewSize(mpreviewSize.width, mpreviewSize.height);
            mcameraHW.setParameters(p);
            //{@link:http://stackoverflow.com/questions/14520803/onpreviewframe-only-called-once}
            /////bitsPerPixel= ImageFormat.getBitsPerPixel(mImageFormat);
            bufferSize= (mpreviewSize.width * mpreviewSize.height + (mpreviewSize.width * mpreviewSize.height )/2);

            // create one by default
            buffer = new byte[bufferSize];
            mcameraHW.addCallbackBuffer(buffer);

            // https://github.com/nico/hack/blob/master/android/ArAttemptTwo/src/de/amnoid/SensorTestActivity.java
            int buffercount=3;
            ActivityManager.MemoryInfo mi;
            mi=MemoryHelper.getMemoryAvailable();
            while ((mi!= null)&& (buffercount > 0))
            {
                if (mi.threshold > (long)(2L * bufferSize)) {
                    // multiple buffers to speed up.
                    buffer = new byte[bufferSize];
                    mcameraHW.addCallbackBuffer(buffer);
                }

                buffercount = buffercount -1;
            }
            mcameraHW.setPreviewCallbackWithBuffer(mpreviewCallback);
            StartPreview();
        }
    }


    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int targetHeight) {
        final double ASPECT_TOLERANCE = 0.1;
        final double targetRatio=(double)targetHeight / w;

        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;


        // get size considering Ratio
        for (Camera.Size size : sizes) {
           final  double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // get size disregarding Ratio.
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    public CameraView(Context context, AttributeSet attributeSet) {

        super(context, attributeSet);
        if (this.isInEditMode()) return ;
        surfaceHolder_ = this.getHolder();
        surfaceHolder_.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surfaceHolder_.addCallback(this);
    }

    public List<Camera.Size> getSupportedPreviewSize() {
        return supportedSizes;
    }

    public int Width() {
        //TODO: not happy with the fix as I dont know the root cause
        // http://localhost:8080/mantis/view.php?id=23
        if (mpreviewSize == null) return 0;
        return mpreviewSize.width;
    }

    public int Height() {
        //TODO: not happy with the fix as I dont know the root cause
        // http://localhost:8080/mantis/view.php?id=23
        if (mpreviewSize == null) return 0;
        return mpreviewSize.height;
    }

    public void setCameraReadyCallback(CameraReadyCallback cb) {
        cameraReadyCallback_ = cb;
    }

    public void StartPreview(){
        if ( mcameraHW == null)
            return;
        mcameraHW.startPreview();

    }

    public void StopPreview() {
        if (mcameraHW == null)
            return;
        try {

            mcameraHW.stopPreview();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void AutoFocus() {
        mcameraHW.autoFocus(afcb);
    }

    public void Release() {
        isReleased = true;
        if ( mcameraHW != null) {
            try {
                mcameraHW.stopPreview();
                mcameraHW.setPreviewCallbackWithBuffer(null);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            surfaceHolder_.removeCallback(this);
            mcameraHW.unlock();
            mcameraHW.release();
            mcameraHW = null;
        }
    }



    @Override
    protected void onDraw(Canvas canvas) {
        if (!isInEditMode())
        {
            super.onDraw(canvas);
            return ;
        }

        canvas.drawColor(Color.TRANSPARENT);
    }




    public void setupCamera(int width, int height, PreviewCallback previewCallback,int rotation) {
        if (mpreviewSize == null) return ; /// need better handling here
        mpreviewSize.width = width;
        mpreviewSize.height = height;
        mpreviewCallback = previewCallback;
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(0, info);

        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {

            degrees = (info.orientation + degrees) % 360;
            degrees = (360 - degrees) % 360;  // compensate the mirror
        } else {  // back-facing
            degrees = (info.orientation - degrees + 360) % 360;
        }


        mcameraHW.setDisplayOrientation(degrees);

       // Camera.Parameters p = mcameraHW.getParameters();
       // mpreviewSize = getOptimalPreviewSize(supportedSizes, mpreviewSize.width, mpreviewSize.height);
       // p.setPreviewSize(mpreviewSize.width, mpreviewSize.height);
       // mcameraHW.setParameters(p);


        //{@link:http://stackoverflow.com/questions/14520803/onpreviewframe-only-called-once}
        /////bitsPerPixel= ImageFormat.getBitsPerPixel(mImageFormat);
        bufferSize= (mpreviewSize.width * mpreviewSize.height + (mpreviewSize.width * mpreviewSize.height )/2);

        buffer=new byte[bufferSize];
        mcameraHW.addCallbackBuffer(buffer);
        mcameraHW.setPreviewCallbackWithBuffer(mpreviewCallback);


    }

    public void setupCamera() {
       if (!DeviceManagerFacade.hasCamera()) return ;
        try {
            mcameraHW = Camera.open();
            mcameraHW.lock();
        }
        catch (Exception ex)
        {
            PanicFacade.cannotStartCamera();
            return ;

        }
        try {

            Camera.Parameters p = mcameraHW.getParameters();

        supportedSizes = p.getSupportedPreviewSizes();
        if (supportedSizes==null) return ;
        mpreviewSize = supportedSizes.get(supportedSizes.size() / 2);

        /*
        p.setPreviewSize(mpreviewSize.width, mpreviewSize.height);
        mcameraHW.setParameters(p);
        */

            mcameraHW.setPreviewDisplay(surfaceHolder_);
            isReleased = false;
        } catch ( Exception ex) {
            AndruavEngine.log().logException("exception_cam", ex);
            PanicFacade.cannotStartCamera();

        }

       // mcameraHW.startPreview();
    }

    public void test ()
    {

    }

    private final Camera.AutoFocusCallback afcb = this.isInEditMode()?null:new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
        }
    };

    @Override
    public void surfaceChanged(SurfaceHolder sh, int format, int w, int h){
        if (isInEditMode()) return ;
        if (surfaceHolder_.getSurface() == null)
        {
            return ;
        }
        Release();
        setupCamera();
        if ( cameraReadyCallback_ != null) {
            cameraReadyCallback_.onCameraReady();
        }


    }

    public void takePhoto (Camera.PictureCallback jpeg)
    {

        takePhoto(null, null, jpeg);

    }

    public void takePhoto (Camera.ShutterCallback shutter, Camera.PictureCallback raw,Camera.PictureCallback jpeg)
    {
        if ((!isReleased) && (mcameraHW != null))
        {
            mcameraHW.takePicture(shutter, raw, jpeg);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder sh){
       // if (isInEditMode()) return ;
        //setupCamera();
        //super.surfaceCreated(sh);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder sh){
        if (isInEditMode()) return ;
        Release();
    }

    @Override
    protected void onAttachedToWindow() {

        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {

        StopPreview();
        Release();

        super.onDetachedFromWindow();
    }
}
