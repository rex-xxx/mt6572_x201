package com.android.camera;

import com.mediatek.common.featureoption.FeatureOption;

public class FeatureSwitcher {
    private static final String TAG = "FeatureSwitcher";
    private static final boolean LOG = Log.LOGV;
    
    public static boolean isVssEnabled() {
        boolean enabled = FeatureOption.MTK_VSS_SUPPORT;
        if (LOG) {
            Log.v(TAG, "isVssEnabled() return " + enabled);
        }
        return enabled;
    }
    
    public static boolean isHdRecordingEnabled() {
        boolean enabled = FeatureOption.MTK_AUDIO_HD_REC_SUPPORT;
        if (LOG) {
            Log.v(TAG, "isHdRecordingEnabled() return " + enabled);
        }
        return enabled;
    }
    
    public static boolean isStereo3dEnable() {
        return false;
    }
    
    public static boolean isStereoSingle3d() {
        return false;
    }
    
    // M: used as a flag to decide enable video live effect or not.
    public static boolean isVideoLiveEffectEnabled() {
        boolean enabled = false;
        if (LOG) {
            Log.v(TAG, "isVideoLiveEffectEnabled() return " + enabled);
        }
        return enabled;
    }
    
    public static boolean isLcaROM() {
        boolean enabled = FeatureOption.MTK_LCA_ROM_OPTIMIZE;
        if (LOG) {
            Log.v(TAG, "isLcaEffects() return " + enabled);
        }
        return enabled ;
    }
    
    //used as a flag to decide whether can switch camera in live effect and default is false only when both 
    //camera support live effect can set it to true
    public static boolean isBothCameraSupportLiveEffect() {
        boolean enabled = false;
        if (LOG) {
            Log.v(TAG, "isBothCameraSupportLiveEffect() return " + enabled);
        }
        return enabled;
    }
    
    // M: used as a flag to decide can slide to gallery or not
    public static boolean isSlideEnabled() {
        boolean enabled = true;
        if (LOG) {
            Log.v(TAG, "isSlideEnabled() return " + enabled);
        }
        return enabled;
    }

    // M: used as a flag to save origin or not in HDR mode
    public static boolean isHdrOriginalPictureSaved() {
        boolean enabled = true;
        if (LOG) {
            Log.v(TAG, "isHdrOriginalPictureSaved() return " + enabled);
        }
        return enabled;
    }

    // M: used as a flag to save origin or not in FaceBeauty mode
    public static boolean isFaceBeautyOriginalPictureSaved() {
        boolean enabled = true;
        if (LOG) {
            Log.v(TAG, "isFaceBeautyOriginalPictureSaved() return " + enabled);
        }
        return enabled;
    }

    public static boolean isContinuousFocusEnabledWhenTouch() {
        boolean enabled = true;
        if (LOG) {
            Log.v(TAG, "isContinuousFocusEnabledWhenTouch() return " + enabled);
        }
        return enabled;
    }
    
    public static boolean isThemeEnabled() {
        boolean enabled = FeatureOption.MTK_THEMEMANAGER_APP;
        if (LOG) {
            Log.v(TAG, "isThemeEnabled() return " + enabled);
        }
        return enabled;
    }

    public static boolean isVoiceEnabled() {
        boolean enabled = FeatureOption.MTK_VOICE_UI_SUPPORT;
        if (LOG) {
            Log.v(TAG, "isVoiceEnabled() return " + enabled);
        }
        return enabled;
    }

    //M: is LCA Enable
    public static boolean isLcaRAM() {
        boolean enabled = FeatureOption.MTK_LCA_RAM_OPTIMIZE;
        if (LOG) {
            Log.v(TAG, "isLcaEnabled() return " + enabled);
        }
        return enabled;
    }
    public static boolean isOnlyCheckBackCamera() {
        //false will check all camera
        //true will only check back camera
        return false;
    }
    
    public static boolean isMtkFatOnNand() {
        boolean enabled = FeatureOption.MTK_FAT_ON_NAND;
        if (LOG) {
            Log.v(TAG, "isMtkFatOnNand() return " + enabled);
        }
        return enabled;
    }
}
