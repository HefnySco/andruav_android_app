package com.andruav.uavos.modules;

/*


  Author: Mohammad S. Hefny
  Date Jan 2020
 */

import org.json.JSONArray;

import java.net.InetAddress;

/**
 * Created By M.Hefny on 19-Jan-2020
 */
public class UAVOSModuleUnit {

    /**
     * For Modules in Andruav it is named as ModuleType + PartyID.
     * For linux version it is defined in the config file.
     */
    public String ModuleId;
    public String ModuleKey;
    public String ModuleClass;
    public JSONArray ModuleCapturedMessages;
    public JSONArray ModuleFeatures;

    public int Port;
    public InetAddress ModuleAddress;
    public boolean BuiltInModule = false;
    public long lastActiveTime;


    private  Object ModuleMessages; // other modules should have its own specific types.


    public UAVOSModuleUnit ()
    {
    }

    public UAVOSModuleUnit (final String moduleClass)
    {
        ModuleClass = moduleClass;
    }

    public void setModuleMessages(final Object moduleMessage)
    {
        ModuleMessages = moduleMessage;
    }

    public Object getModuleMessages()
    {
        return ModuleMessages;
    }


}
