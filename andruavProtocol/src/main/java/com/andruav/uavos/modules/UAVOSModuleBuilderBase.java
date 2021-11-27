package com.andruav.uavos.modules;

/*


  Author: Mohammad S. Hefny
  Date Jan 2020
 */

import static com.andruav.uavos.modules.UAVOSConstants.UAVOS_MODULE_TYPE_CAMERA;

public abstract class UAVOSModuleBuilderBase {

    public static UAVOSModuleUnit getModule (final String moduleClass)
    {
        switch (moduleClass)
        {
            case UAVOS_MODULE_TYPE_CAMERA:
                return new UAVOSModuleCamera();

            default:
                return new UAVOSModuleUnit(moduleClass);
        }
    }

}
