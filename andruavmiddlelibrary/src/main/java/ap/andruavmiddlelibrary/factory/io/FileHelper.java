package ap.andruavmiddlelibrary.factory.io;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import com.andruav.AndruavEngine;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

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
                root = new File(Environment.getExternalStorageDirectory() , subFolder);
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
