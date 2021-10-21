package rcmobile.andruavmiddlelibrary.factory.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.location.Location;
import android.media.ExifInterface;
import android.net.Uri;
import android.view.View;

import com.andruav.util.StringSplit;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by M.Hefny on 29-Oct-14.
 * for all tags:
 * @link http://www.exiv2.org/metadata.html
 */
public class Image_Helper {

    public static final java.lang.String TAG_ImageDescription = "ImageDescription";


    public static Bitmap getScreenShot(View view) {
        View screenView = view.getRootView();
        screenView.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(screenView.getDrawingCache());
        screenView.setDrawingCacheEnabled(false);


        return bitmap;
    }

    /***
     * takes screen shot of an activity
     * @see <a>http://www.androidsnippets.com/rotete-the-bitmap-image-programatically-in-android</a>
     * @param activity
     * @return
     */
    public  static Bitmap takeScreenShot(final Activity activity)
    {
        return takeScreenShot_p (activity);
    }

    private  static Bitmap takeScreenShot_p2(final Activity activity)
    {
        View view = activity.getWindow().getDecorView();
        view.setDrawingCacheEnabled(true);
        // view.buildDrawingCache();
        Bitmap b1 = view.getDrawingCache();
        Rect frame = new Rect();
        view.getWindowVisibleDisplayFrame(frame);

        int statusBarHeight = frame.top;
        int width = activity.getWindowManager().getDefaultDisplay().getWidth();
        int height = activity.getWindowManager().getDefaultDisplay().getHeight();

        Bitmap b = Bitmap.createBitmap(b1, 0, statusBarHeight, width, height  - statusBarHeight);

        view.destroyDrawingCache();
        return b;
    }

    private  static Bitmap takeScreenShot_p(final Activity activity) {
        View rootView = ActivityMosa3ed.getRootViewOfActivity(activity);
        rootView.setDrawingCacheEnabled(true);
        Bitmap b1= rootView.getDrawingCache();
        //rootView.destroyDrawingCache();
        return b1;
    }


    /***
     * Runs image scanner on your image file folder so that you can find it as a folder in the gallery.
     * @param context
     * @param photoPath
     */
    public static void galleryAddPic(final Context context, final String photoPath) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(photoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        context.sendBroadcast(mediaScanIntent);
    }



    /***
     * Download an image from a URL
     * @param imgURL
     * @return
     */
    public static InputStream getInputStreamFromURL(final String imgURL) {
        try {
            URL url = new URL(imgURL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            return connection.getInputStream();


        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }



    /***
     * Download an image from a URL
     * @param imgURL
     * @return
     */
    public static Bitmap getBitmapFromURL(final String imgURL) {
        try {
            InputStream input = getInputStreamFromURL(imgURL);
            Bitmap myBitmap = BitmapFactory.decodeStream(input);
            return myBitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    static byte[] chunk = new byte[8192];
    public static  byte[] downloadUrl(final String imgURL) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {

            int bytesRead;
            URL toDownload = new URL(imgURL);
            InputStream stream = toDownload.openStream();

            while ((bytesRead = stream.read(chunk)) > 0) {
                outputStream.write(chunk, 0, bytesRead);
            }

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return outputStream.toByteArray();
    }

    public static Bitmap createBMPfromJPG (final byte[] imgData)
    {
        Bitmap bitmap = BitmapFactory.decodeByteArray(imgData, 0, imgData.length);

        return bitmap;
    }

   static  ByteArrayOutputStream output_stream = new ByteArrayOutputStream();

    public static Bitmap createBMPfromYUV (final YuvImage image, final Rect rectangle)
    {

        output_stream.reset();
        image.compressToJpeg(rectangle ,50,output_stream);

        Bitmap bitmap = BitmapFactory.decodeByteArray(output_stream.toByteArray(), 0, output_stream.size());

        return bitmap;
    }


    public static byte[] createJpgStream  (final YuvImage image, final Rect rectangle)
    {
        output_stream.reset();
        image.compressToJpeg(rectangle ,50,output_stream);

        return output_stream.toByteArray();
    }


    public static int returnRotationAngle (int rotation)
    {
        rotation =  (rotation +1 )   % 3;
        rotation = rotation * 90;
        return rotation;

    }

    public static Bitmap rotateImage (final Bitmap srcBitmap, final int width, final int height, final int rotationAngle)
    {
        // create rotated image
        Matrix matrix = new Matrix();
        matrix.postRotate( rotationAngle,
                width,
                height );
        Bitmap rotatedBmp = Bitmap.createBitmap( srcBitmap,
                0,
                0,
                srcBitmap.getWidth(),
                srcBitmap.getHeight(),
                matrix,
                false );

        return rotatedBmp;
    }

    //@see <a>http://www.androidsnippets.com/rotete-the-bitmap-image-programatically-in-android</a>
    public static byte[] resizeImage(final Bitmap jpgimg, final int width, final int height) {

        // create scaled image
        Bitmap scaledBMP = Bitmap.createScaledBitmap(jpgimg, width, height, false);


        //create JPEG compress & stream
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        scaledBMP.compress(Bitmap.CompressFormat.JPEG, 50, baos);
        scaledBMP.recycle();
        return baos.toByteArray();
    }

    //@see <a>http://www.androidsnippets.com/rotete-the-bitmap-image-programatically-in-android</a>
    public static Bitmap resizeImage2(final Bitmap jpgimg,int width, int height) {

        // create scaled image
        return  Bitmap.createScaledBitmap(jpgimg, width, height, false);
    }

    /***
     * Adds location GPS info to jpg file.
     * @linnk <a href='http://stackoverflow.com/questions/11644873/android-write-exif-gps-latitude-and-longitude-onto-jpeg-failed'></a></a>
     * @link http://www.exiv2.org/metadata.html
     * @param filePath
     * @param locationInfo
     ***/
    public static void AddGPStoJpg (final String filePath, final Location locationInfo)
    {
        AddGPStoJpg_p(filePath,locationInfo);
    }

    private static void AddGPStoJpg_p (final String filePath, final Location locationInfo)
    {

        if (locationInfo == null) return ; // http://localhost:8080/mantis/view.php?id=20

        ExifInterface exif = null;
        try {
            exif = new ExifInterface(filePath);
        } catch (IOException e) {
            e.printStackTrace();
            return ;
        }
        //String latitudeStr = "90/1,12/1,30/1";
        double lat = locationInfo.getLatitude();
        double alat = Math.abs(lat);
        String dms = Location.convert(alat, Location.FORMAT_SECONDS);
        String[] splits = dms.split(":");
        String[] secnds = (splits[2]).split("\\.");
        String seconds;
        if(secnds.length==0)
        {
            seconds = splits[2];
        }
        else
        {
            seconds = secnds[0];
        }

        String latitudeStr = splits[0] + "/1," + splits[1] + "/1," + seconds + "/1";
        exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, latitudeStr);

        exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, lat>0?"N":"S");

        double lon = locationInfo.getLongitude();
        double alon = Math.abs(lon);


        dms = Location.convert(alon, Location.FORMAT_SECONDS);
        splits = StringSplit.fastSplit(dms,':');
        secnds = (splits[2]).split("\\.");

        if(secnds.length==0)
        {
            seconds = splits[2];
        }
        else
        {
            seconds = secnds[0];
        }
        String longitudeStr = splits[0] + "/1," + splits[1] + "/1," + seconds + "/1";


        exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, longitudeStr);
        exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, lon>0?"E":"W");

      /*
        if (locationInfo.hasAltitude()) {

            double maxAltitude = locationInfo.getAltitude();
            exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE, String.valueOf(maxAltitude));
        }

        // @see http://www.exiv2.org/metadata.html
        exif.setAttribute(TAG_ImageDescription, "Andruav FPV");
*/
        try {
            exif.saveAttributes();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


}
