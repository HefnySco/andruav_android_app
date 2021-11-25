package ap.andruavmiddlelibrary.factory.os;

import android.util.Log;

import com.andruav.AndruavEngine;


/**
 * Created by M.Hefny on 22-Sep-14.
 */
public class OS {

    /**
     * Execute a command in command prompt.
     * @param cmd
     * @param isRootCmd true if you want to processInterModuleMessages su before the command
     * @throws Exception
     */
    public static void executeCMD (String cmd, Boolean isRootCmd) throws Exception
    {
        Process ps=null;
        try {
            java.lang.Runtime rt = Runtime.getRuntime();
            if (isRootCmd == true) {
                ps = rt.exec("su");
                //Causes the calling thread to wait for the native process associated with this object to finish executing.
                ps.waitFor();
                ps.destroy();
                ps = null;
            }

            ps = rt.exec(cmd);
            ps.waitFor();
            ps.destroy();
            ps = null;
        }
        catch (Exception e)
        {
            Log.e(AndruavEngine.getPreference().TAG(),String.format("Failed in %s",e.toString()));
        }
        finally {

            if (ps !=null)  ps.destroy();

        }
    }
}
