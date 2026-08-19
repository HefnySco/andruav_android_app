package com.andruav.uavos.modules;

/*

  Author: Mohammad S. Hefny
  Date Jan 2020
 */

import androidx.collection.SimpleArrayMap;

/**
 * Created By M.Hefny on 19-Jan-2020
 */
public class UAVOSMapBase extends SimpleArrayMap<String, UAVOSModuleUnit> {


    @Override
    public UAVOSModuleUnit put(final String key, final UAVOSModuleUnit value) {

        synchronized (this) {
            super.put(key, value);
        }

        return value;
    }


    public UAVOSModuleUnit get (final String key)
    {
        return super.get(key);
    }


    public void remove(final String moduleID) {

        super.remove(moduleID);
    }
}
