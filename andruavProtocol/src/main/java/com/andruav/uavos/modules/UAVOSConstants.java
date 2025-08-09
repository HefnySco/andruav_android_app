package com.andruav.uavos.modules;

/**
 *
 *
 * Author: Mohammad S. Hefny
 * Date Jan 2020
 */

final public class UAVOSConstants {

    public final static String UAVOS_MODULE_TYPE_CAMERA ="camera";


    public final static String UAVOS_MODULE_TYPE_FCB ="FCB_CTRL";
    public final static String UAVOS_MODULE_TYPE_DNN_TRK ="DNN_TRK";


    public final static String CAMERA_LOCAL_NAME        = "ln";
    public final static String CAMERA_UNIQUE_NAME       = "id";
    public final static String CAMERA_SUPPORT_VIDEO     = "v";
    public final static String CAMERA_SUPPORT_ZOOM      = "z";
    public final static String CAMERA_SUPPORT_FLASH     = "f";
    public final static String CAMERA_ACTIVE            = "active";

    public final static int CAMERA_SPECIFICATION_SUPPORT_ZOOMING    = 0x1;
    public final static int CAMERA_SPECIFICATION_SUPPORT_ROTATION   = 0x2;
    public final static int CAMERA_SPECIFICATION_SUPPORT_RECORDING  = 0x4;
    public final static int CAMERA_SPECIFICATION_SUPPORT_PHOTO      = 0x8;
    public final static int CAMERA_SPECIFICATION_DUAL_CAM           = 0x10;
    public final static int CAMERA_SPECIFICATION_SUPPORT_FLASHING   = 0x20;
    public final static String CAMERA_TYPE              = "p";

    //public final static String CAMERA_ANDROID_DUAL_CAM  = "f";
    public final static String CAMERA_RECORDING_NOW     = "r";

    public final static String CAMERA_SPECIFICATION     = "s";
}
