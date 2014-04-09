package com.mediatek.mock.hardware;

public class MessageEnum {
    public static final int MTK_CAMERA_MSG_EXT_DATA = 0x80000000;

    public static final int CAMERA_MSG_COMPRESSED_IMAGE    = 0x100;

    public static final int MTK_CAMERA_MSG_EXT_DATA_AUTORAMA           = 0x00000001;
    //
    // AF Window Results
    public static final int MTK_CAMERA_MSG_EXT_DATA_AF                 = 0x00000002;
    //
    // Burst Shot (EV Shot)
    //  int[0]: the total shut count.
    //  int[1]: count-down shut number; 0: the last one shut.
    public static final int MTK_CAMERA_MSG_EXT_DATA_BURST_SHOT         = 0x00000003;


    public static final int MTK_CAMERA_MSG_EXT_NOTIFY                  = 0x40000000;


    public static final int MTK_CAMERA_MSG_EXT_NOTIFY_SMILE_DETECT     = 0x00000001;
    //
    // Auto Scene Detection
    public static final int MTK_CAMERA_MSG_EXT_NOTIFY_ASD              = 0x00000002;
    // Multi Angle View
    public static final int MTK_CAMERA_MSG_EXT_NOTIFY_MAV              = 0x00000003;
    public static final int MTK_CAMERA_MSG_EXT_NOTIFY_CONTINUOUS_END   = 0x00000006;
}
