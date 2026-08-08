package ap.andruavmiddlelibrary.factory.io;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.andruav.AndruavEngine;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import ap.andruavmiddlelibrary.factory.os.OS;


/**
 * Created by M.Hefny on 22-Sep-14.
 */
public class FileHelper {

    /**
     * @see  "sample: copyResourceFile(R.raw.index, resourceDir + "index.html"  );"
     * @param context
     * @param rid
     * @param resourceDirectory "final String resourceDir = "/sdcard/webcamera/""
     * @param targetFile
     * @throws IOException
     */
    public static void copyResourceFile(final Context context, final int rid, final String resourceDirectory, final String targetFile) throws IOException
    {
        File target = new File (resourceDirectory);
        if (!target.exists())
        {
            target.mkdir();
        }
        InputStream fileInputStream = context.getResources().openRawResource(rid);
        FileOutputStream fileOutputStream = new FileOutputStream(targetFile);

        int     length;
        //TODO: bug expected issue here for fixed size allocation.
        byte[] buffer = new byte[1024*32];
        while( (length = fileInputStream.read(buffer)) != -1){
            fileOutputStream.write(buffer,0,length);
        }
        fileInputStream.close();
        fileOutputStream.close();
    }

    /**
     * @see "final String resourceDir = "/sdcard/webcamera/";"
     * @param directoryPath
     */
    public static void createDirectory (final String directoryPath) {

        try {
            OS.executeCMD("mkdir " + directoryPath, false);
        } catch (Exception e) {
            Log.e(AndruavEngine.getPreference().TAG(), String.format("Failed in %s", e));
        }
    }







    /***
     * Returns a path of existing folder or creates one if not found.
     * @param subFolder
     * @param rootPath parent folder absolutePath "parentFolder.getAbsolutePath()"
     * @return
     */
    public static File GetFolder(final String subFolder,final String rootPath) {
        Boolean externalStorageWriteable = false;
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            // TODO: handle if storage is not available
            if (Environment.MEDIA_MOUNTED.equals(state)) {
                externalStorageWriteable = true;
            } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
                externalStorageWriteable = false;
            } else {
                externalStorageWriteable = false;
            }
        }
     //   externalStorageWriteable = false;
        File root;
        if (rootPath==null)
        {  // get Folder relative to ExternalStorageDirectory root.
            if (externalStorageWriteable)
            {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Scoped storage (API 29+): requestLegacyExternalStorage is ignored once
                    // targetSdkVersion >= 30, so the shared public Downloads directory below is not
                    // reliably writable without MANAGE_EXTERNAL_STORAGE. Use app-specific external
                    // storage instead - no permission required on any API level, always writable.
                    root = AndruavEngine.getPreference().getContext().getExternalFilesDir(subFolder);
                }else {
                    root = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), subFolder);
                }
            }
            else {
                File docsFolder = null;
                if (Build.VERSION.SDK_INT != Build.VERSION_CODES.JELLY_BEAN) {
                    try {
                        if (Build.VERSION.SDK_INT >= 19) {

                            docsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
                        }else{
                            docsFolder = new File(Environment.getExternalStorageDirectory() + "/Documents");
                        }

                    } catch (Exception ex) {
                        AndruavEngine.log().logException("IOException", ex);
                    }
                }
               if ((docsFolder != null) && (docsFolder.exists())) {
                          root = new File(docsFolder, subFolder);
                }
                else {

                   root = new File(Environment.getExternalStorageDirectory().toString(), subFolder);
                }


            }
        }
        else
        {   // get path relative to rootPath.
            root = new File(rootPath, subFolder);
        }

        if ((root!=null) && (!root.exists())) {
            // now create if not existed.
            root.mkdirs();
        }
        return root;
    }


    /***
     * Scoped storage (API 29+) equivalent of a "public, survives-uninstall" folder: creates a new
     * entry under the shared Downloads collection via MediaStore, so it's visible in a normal File
     * Manager and is not deleted when the app is uninstalled - unlike app-specific external storage.
     * Multiple entries sharing the same relativeFolderPath end up as real filesystem siblings, which
     * matters for e.g. a KML file that references image files next to it via a relative href.
     *
     * @param relativeFolderPath e.g. "Download/AndruavKML/Trip_20260808_120000/files"
     * @param displayName file name, e.g. "IMG_1.jpg"
     * @param mimeType e.g. "image/jpeg"
     * @return the new item's content Uri, or null on failure
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    public static Uri createDownloadsEntry(final String relativeFolderPath, final String displayName, final String mimeType) {
        try {
            final ContentResolver resolver = AndruavEngine.getPreference().getContext().getContentResolver();
            final ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, displayName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, relativeFolderPath);
            values.put(MediaStore.MediaColumns.IS_PENDING, 1);
            return resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        } catch (Exception ex) {
            AndruavEngine.log().logException("exception_mediastore", ex);
            return null;
        }
    }

    /***
     * Clears IS_PENDING on a MediaStore entry created via {@link #createDownloadsEntry}, making it
     * visible to other apps (File Manager, gallery scanners, etc). Call once writing is finished.
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    public static void markMediaStoreEntryComplete(final Uri uri) {
        if (uri == null) return;
        try {
            final ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.IS_PENDING, 0);
            AndruavEngine.getPreference().getContext().getContentResolver().update(uri, values, null, null);
        } catch (Exception ex) {
            AndruavEngine.log().logException("exception_mediastore", ex);
        }
    }

    /***
     * Builds the same timestamped ".jpg" name {@link #savePic(Bitmap, String, File)} generates
     * internally, exposed so MediaStore callers (which need the final name for their own bookkeeping,
     * e.g. a KML href) can compute it once and reuse it for both the write and their own records.
     */
    public static String buildTimestampedJpgName(String strFileName) {
        if ((strFileName == null) || (strFileName.length()==0)) {
            strFileName = "FPV_IMG";
        }
        return strFileName + "_" + System.currentTimeMillis() + ".jpg";
    }

    /***
     * Scoped-storage (API 29+) equivalent of {@link #savePic(Bitmap, String, File)} - saves under the
     * shared Downloads collection instead of a File, so the image survives uninstall and is browsable.
     * @param displayName final file name (see {@link #buildTimestampedJpgName})
     * @return the new item's content Uri, or null on failure
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    public static Uri savePicToMediaStore(final Bitmap b, final String displayName, final String relativeFolderPath) {
        final Uri uri = createDownloadsEntry(relativeFolderPath, displayName, "image/jpeg");
        if (uri == null) return null;

        try (OutputStream os = AndruavEngine.getPreference().getContext().getContentResolver().openOutputStream(uri)) {
            if (os == null) return null;
            b.compress(Bitmap.CompressFormat.JPEG, 90, os);
        } catch (IOException ex) {
            AndruavEngine.log().logException("exception_img", ex);
            return null;
        }
        markMediaStoreEntryComplete(uri);
        return uri;
    }


    /***
     * Reads String from InputStream
     * @param inputStream
     * @return
     */
    public static String readTextfromStream(final InputStream inputStream) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        byte[] buf = new byte[1024];
        int len;
        try {
            while ((len = inputStream.read(buf)) != -1) {
                outputStream.write(buf, 0, len);
            }
            outputStream.close();
            inputStream.close();
        } catch (IOException ex) {
            AndruavEngine.log().logException("IOException", ex);
            return "<br><br>Corrupted Resource- Please refer to rcmobilesutff@gmail.com";
        }
        return outputStream.toString();
    }


    public static File savePic(final Bitmap b, String strFileName, File root)
    {
        FileOutputStream fos;
        try
        {
            // http://developer.android.com/training/basics/data-storage/files.html
            // Remember that getExternalFilesDir() creates a directory inside a directory that is deleted
            // when the user uninstalls your app. If the files you're saving should remain available after
            // the user uninstalls your app—such as when your app is a camera and the user will want to keep the photos
            // —you should instead use getExternalStoragePublicDirectory().

            if (root == null) {
                root = GetFolder("AndruavImgs", null);
            }

            if ((strFileName == null) || (strFileName.length()==0))
            {
                strFileName = "FPV_IMG";
            }
            strFileName +="_" + System.currentTimeMillis()+".jpg";
            //File img = new File(Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DOWNLOADS , strFileName);
            File img = new File(root, strFileName);
            if (!img.exists())
            {
                img.createNewFile();
            }

            fos = new FileOutputStream(img.getAbsolutePath());
            b.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.flush();
            fos.close();
            return img;
        }
        catch (FileNotFoundException ex)
        {
            AndruavEngine.log().logException("exception_img", ex);
        }
        catch (IOException ex)
        {
            AndruavEngine.log().logException("exception_img", ex);
        }
        return null;
    }



    public static File savePic(final Rect rect, final YuvImage jpgimg, String strFileName, File root)
    {

        FileOutputStream fos;
        try
        {
            // http://developer.android.com/training/basics/data-storage/files.html
            // Remember that getExternalFilesDir() creates a directory inside a directory that is deleted
            // when the user uninstalls your app. If the files you're saving should remain available after
            // the user uninstalls your app—such as when your app is a camera and the user will want to keep the photos
            // —you should instead use getExternalStoragePublicDirectory().

            //File root = new File(App.getAppContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES), ".RCMobile");

            if (root == null) {
                root = GetFolder("AndruavImgs", null);
            }

            if ((strFileName == null) || (strFileName.length()==0))
            {
                strFileName = "FPV_IMG";
            }
            strFileName +="_" + System.currentTimeMillis()+".jpg";

            File img = new File(root, strFileName);
            if (!img.exists())
            {
                img.createNewFile();
            }

            fos = new FileOutputStream(img.getAbsolutePath());
            jpgimg.compressToJpeg(rect,90,fos);
            fos.flush();
            fos.close();
            return img;
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return null;
    }


    public static File savePic( final byte[] dataImg, String strFileName, File root)
    {

        FileOutputStream fos;
        try
        {
            // http://developer.android.com/training/basics/data-storage/files.html
            // Remember that getExternalFilesDir() creates a directory inside a directory that is deleted
            // when the user uninstalls your app. If the files you're saving should remain available after
            // the user uninstalls your app—such as when your app is a camera and the user will want to keep the photos
            // —you should instead use getExternalStoragePublicDirectory().

            //File root = new File(App.getAppContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES), ".RCMobile");

            if (root == null) {
                root = GetFolder("AndruavImgs", null);
            }

            if ((strFileName == null) || (strFileName.length()==0))
            {
                strFileName = "FPV_IMG";
            }
            strFileName +="_" + System.currentTimeMillis()+".jpg";

            File img = new File(root, strFileName);
            if (!img.exists())
            {
                img.createNewFile();
            }

            fos = new FileOutputStream(img.getAbsolutePath());
            fos.write(dataImg);
            fos.flush();
            fos.close();
            return img;
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return null;
    }

}
