package com.andruav.uavos.modules;

/*


  Author: Mohammad S. Hefny
  Date Jan 2020
 */

import static com.andruav.uavos.modules.UAVOSConstants.UAVOS_MODULE_TYPE_CAMERA;

public abstract class UAVOSModuleBuilderBase {

    public static UAVOSModuleUnit getModule (final String moduleClass)
    {
        if (UAVOS_MODULE_TYPE_CAMERA.equals(moduleClass)) {
            return new UAVOSModuleCamera();
        }
        return new UAVOSModuleUnit(moduleClass);
    }

}
