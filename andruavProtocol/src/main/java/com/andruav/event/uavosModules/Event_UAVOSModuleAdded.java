package com.andruav.event.uavosModules;


import com.andruav.uavos.modules.UAVOSModuleUnit;

public class Event_UAVOSModuleAdded {

    public final UAVOSModuleUnit uavosModuleUnit;


    public Event_UAVOSModuleAdded (final UAVOSModuleUnit uavosModule)
    {
        uavosModuleUnit = uavosModule;
    }
}
