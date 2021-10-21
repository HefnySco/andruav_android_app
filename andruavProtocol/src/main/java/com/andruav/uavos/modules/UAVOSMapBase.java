package com.andruav.uavos.modules;

/**
 *
 *
 * Author: Mohammad S. Hefny
 * Date Jan 2020
 */

import androidx.collection.SimpleArrayMap;

import com.andruav.AndruavEngine;
import com.andruav.event.uavosModules.Event_UAVOSModuleAdded;
import com.andruav.event.uavosModules.Event_UAVOSModuleRemoved;

/**
 * Created By M.Hefny on 19-Jan-2020
 */
public class UAVOSMapBase extends SimpleArrayMap<String, UAVOSModuleUnit> {


    @Override
    public UAVOSModuleUnit put(final String key, final UAVOSModuleUnit value) {


        synchronized (this) {
            super.put(key, value);
        }

        AndruavEngine.getEventBus().post(new Event_UAVOSModuleAdded(value));

        return value;
    }


    public UAVOSModuleUnit get (final String key)
    {
        return super.get(key);
    }


    public void remove(final String moduleID) {

        final UAVOSModuleUnit uavosModuleUnit = super.remove(moduleID);
        if (uavosModuleUnit == null) return ;
        AndruavEngine.getEventBus().post(new Event_UAVOSModuleRemoved(uavosModuleUnit));
    }

    public void updateLastActiveTime(final String moduleID) {

        UAVOSModuleUnit uavosModuleUnit = get(moduleID);
        if (uavosModuleUnit == null) {
            // Object is not in the defined markers...
            // either because :
            // 1- No Message ID has been sent yet.
            // 2- future reason: permissions and security related issues.
            // We can add it as update list to increase response time but I prefer to leave it as is
            // for future security and permissions updates.
            // The safest way here is to sendMessageToModule individual message asking the unit to sendMessageToModule its ID
            return;
        }
        uavosModuleUnit.lastActiveTime = System.currentTimeMillis();
    }
}
