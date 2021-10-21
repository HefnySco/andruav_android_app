package com.andruav.event.uavosModules;

import com.andruav.uavos.modules.UAVOSModuleUnit;

public class Event_UAVOSModuleRemoved {

    public UAVOSModuleUnit uavosModuleUnit;


    public Event_UAVOSModuleRemoved (final UAVOSModuleUnit uavosModule)
    {
        uavosModuleUnit = uavosModule;
    }
}
