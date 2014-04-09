package com.android.camera;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.graphics.Point;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.media.CamcorderProfile;
import android.media.CameraProfile;
import android.provider.MediaStore;
import android.util.DisplayMetrics;

import com.android.camera.FeatureSwitcher;
import com.android.camera.Restriction.MappingFinder;
import com.android.camera.actor.EffectsRecorder;
import com.android.camera.manager.ModePicker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class SettingChecker {
    private static final String TAG = "SettingChecker";
    private static final boolean LOG = Log.LOGV;
    
    public static final int ROW_SETTING_FLASH = 0;//common
    public static final int ROW_SETTING_DUAL_CAMERA = 1;//common
    public static final int ROW_SETTING_EXPOSURE = 2;//common
    public static final int ROW_SETTING_SCENCE_MODE = 3;//common
    public static final int ROW_SETTING_WHITE_BALANCE = 4;//common
    public static final int ROW_SETTING_IMAGE_PROPERTIES = 5;//common
    public static final int ROW_SETTING_COLOR_EFFECT = 6;//common
    public static final int ROW_SETTING_SELF_TIMER = 7;//camera
    public static final int ROW_SETTING_ZSD = 8;//camera
    public static final int ROW_SETTING_CONTINUOUS = 9;//camera
    public static final int ROW_SETTING_GEO_TAG = 10;//common
    public static final int ROW_SETTING_PICTURE_SIZE = 11;//camera
    public static final int ROW_SETTING_ISO = 12;//camera
    //public static final int ROW_SETTING_AE_METER = 13;
    public static final int ROW_SETTING_ANTI_FLICKER = 14;//common
    public static final int ROW_SETTING_VIDEO_STABLE = 15;//video
    public static final int ROW_SETTING_MICROPHONE = 16;//video
    public static final int ROW_SETTING_AUDIO_MODE = 17;//video
    public static final int ROW_SETTING_TIME_LAPSE = 18;//video
    public static final int ROW_SETTING_LIVE_EFFECT = 19;//video
    public static final int ROW_SETTING_VIDEO_QUALITY = 20;//video
    
    public static final int ROW_SETTING_PICTURE_RATIO = 21;//camera
    public static final int ROW_SETTING_VOICE = 22;//camera
    
    //image adjustment
    public static final int ROW_SETTING_SHARPNESS = 30;//common
    public static final int ROW_SETTING_HUE = 31;//common
    public static final int ROW_SETTING_SATURATION = 32;//common
    public static final int ROW_SETTING_BRIGHTNESS = 33;//common
    public static final int ROW_SETTING_CONTRAST = 34;//common
    
    //not in preference or not same as preference, but should be set in parameter
    public static final int ROW_SETTING_CAMERA_MODE = 40;//camera mode
    public static final int ROW_SETTING_CAPTURE_MODE = 41;//not in preference
    public static final int ROW_SETTING_CONTINUOUS_NUM = 42;//different from preference
    public static final int ROW_SETTING_RECORDING_HINT = 43;//not in preference
    public static final int ROW_SETTING_JPEG_QUALITY = 44;//not in preference
    //not in preference and not in parameter
    public static final int ROW_SETTING_STEREO_MODE = 45;

    public static final int ROW_SETTING_FACEBEAUTY_PROPERTIES = 46;//camera
    // facebeauty adjustment
    public static final int ROW_SETTING_FACEBEAUTY_SMOOTH = 47;//camera
    public static final int ROW_SETTING_FACEBEAUTY_SKIN_COLOR = 48;//camera
    public static final int ROW_SETTING_FACEBEAUTY_SHARP = 49;//camera
    public static final int ROW_SETTING_CAMERA_FACE_DETECT = 50;//camera

//    public static final int MODE_PHOTO = 0;
//    public static final int MODE_HDR = 1;
//    public static final int MODE_FACE_BEAUTY = 2;
//    public static final int MODE_PANORAMA = 3;
//    public static final int MODE_MAV = 4;
//    public static final int MODE_ASD = 5;
//    public static final int MODE_SMILE_SHOT = 6;
//    public static final int MODE_BEST = 7;
//    public static final int MODE_EV = 8;
//    public static final int MODE_VIDEO = 9;
//    public static final int COLUM_MODE_NORMAL_3D = 10;
//    public static final int COLUM_MODE_PANORAMA_3D = 11;
    public static final int MODE_VIDEO_LIVE_EFFECT = 12;
    
    private static final int COLUM_SCENE_MODE_NORMAL = 0;
    private static final int COLUM_SCENE_MODE_PORTRAIT = 1;
    private static final int COLUM_SCENE_MODE_LANDSCAPE = 2;
    private static final int COLUM_SCENE_MODE_NIGHT = 3;
    private static final int COLUM_SCENE_MODE_NIGHT_PORTRAIT = 4;
    private static final int COLUM_SCENE_MODE_THEATRE = 5;
    private static final int COLUM_SCENE_MODE_BEACH = 6;
    private static final int COLUM_SCENE_MODE_SNOW = 7;
    private static final int COLUM_SCENE_MODE_SUNSET = 8;
    private static final int COLUM_SCENE_MODE_STEADYPHOTO = 9;
    private static final int COLUM_SCENE_MODE_FIREWORKS = 10;
    private static final int COLUM_SCENE_MODE_SPORT = 11;
    private static final int COLUM_SCENE_MODE_PARTY = 12;
    private static final int COLUM_SCENE_MODE_CANDLELIGHT = 13;
    private static final int COLUM_SCENE_MODE_AUTO = 14;
        
    private static final int STATE_D0 = 100;//disable
    private static final int STATE_E0 = 200;//enable
    private static final int STATE_R0 = 300;//reset value 0
    private static final int STATE_R1 = 301;//reset value 1
    private static final int STATE_R2 = 302;//reset value 2
    private static final int STATE_R3 = 303;//reset value 3
    private static final int STATE_R4 = 304;//reset value 4
    private static final int STATE_R5 = 305;//reset value 5
    private static final int STATE_R6 = 306;//reset value 6
    private static final int STATE_R7 = 307;//reset value 7
    
    private static final int STATE_OFFSET = 100;

    public static final int CAMERA_BACK_ID = 0;
    public static final int CAMERA_FRONT_ID = 1;

    public static final int CAMERA_COUNT = 2;
    public static final int SETTING_ROW_COUNT = 51;
    
    public static final int UNKNOWN = -1;
    
    private static final int[] RESET_SETTING_ITEMS = new int[]{
        ROW_SETTING_DUAL_CAMERA,
        ROW_SETTING_EXPOSURE,
        ROW_SETTING_SCENCE_MODE,
        ROW_SETTING_WHITE_BALANCE,
        ROW_SETTING_COLOR_EFFECT,
        ROW_SETTING_SELF_TIMER,
        ROW_SETTING_SHARPNESS,//common
        ROW_SETTING_HUE,//common
        ROW_SETTING_SATURATION,//common
        ROW_SETTING_BRIGHTNESS,//common
        ROW_SETTING_CONTRAST,//common
        ROW_SETTING_ISO,
        //ROW_SETTING_AE_METER,
        ROW_SETTING_TIME_LAPSE,
        ROW_SETTING_LIVE_EFFECT,
        ROW_SETTING_AUDIO_MODE,
    };
    //Common setting items for final user, order by UX spec
    public static final int[] SETTING_GROUP_COMMON_FOR_TAB = new int[]{
        ROW_SETTING_GEO_TAG,//common
        //ROW_SETTING_FLASH,//common
        //ROW_SETTING_DUAL_CAMERA,//common
        ROW_SETTING_EXPOSURE,//common
        ROW_SETTING_COLOR_EFFECT,//common
        ROW_SETTING_SCENCE_MODE,//common
        ROW_SETTING_WHITE_BALANCE,//common
        ROW_SETTING_IMAGE_PROPERTIES,
        //ROW_SETTING_AE_METER,
        ROW_SETTING_ANTI_FLICKER,//common
        
        //ROW_SETTING_SHARPNESS,//common
        //ROW_SETTING_HUE,//common
        //ROW_SETTING_SATURATION,//common
        //ROW_SETTING_BRIGHTNESS,//common
        //ROW_SETTING_CONTRAST,//common
    };
    //Camera setting items for final user, order by UX spec
    public static final int[] SETTING_GROUP_CAMERA_FOR_TAB = new int[]{
        ROW_SETTING_ZSD,//camera
        ROW_SETTING_VOICE,//camera
        ROW_SETTING_CAMERA_FACE_DETECT,//camera
        ROW_SETTING_SELF_TIMER,//camera
        ROW_SETTING_CONTINUOUS,//camera
        ROW_SETTING_PICTURE_SIZE,//camera
        ROW_SETTING_PICTURE_RATIO,//camera
        ROW_SETTING_ISO,//camera
        ROW_SETTING_FACEBEAUTY_PROPERTIES,//camera
    };
    //Video setting items for final user, order by UX spec
    public static final int[] SETTING_GROUP_VIDEO_FOR_TAB = new int[]{
        ROW_SETTING_VIDEO_STABLE,//video
        ROW_SETTING_MICROPHONE,//video
        ROW_SETTING_AUDIO_MODE,//video
        ROW_SETTING_TIME_LAPSE,//video
        ROW_SETTING_LIVE_EFFECT,//video
        ROW_SETTING_VIDEO_QUALITY,//video
    };
    
    //Preview setting items for final user(brief case), order by UX spec
    public static final int[] SETTING_GROUP_COMMON_FOR_TAB_PREVIEW = new int[]{
        ROW_SETTING_PICTURE_SIZE,//camera
        ROW_SETTING_PICTURE_RATIO,//camera
        ROW_SETTING_VIDEO_QUALITY//video
    };
    //Camera setting items for final user, order by UX spec
    public static final int[] SETTING_GROUP_CAMERA_FOR_TAB_NO_PREVIEW = new int[]{
        ROW_SETTING_ZSD,//camera
        ROW_SETTING_VOICE,//camera
        ROW_SETTING_CAMERA_FACE_DETECT,//camera
        ROW_SETTING_SELF_TIMER,//camera
        ROW_SETTING_CONTINUOUS,//camera
        ROW_SETTING_ISO,//camera
    };
    //Video setting items for final user, order by UX spec
    public static final int[] SETTING_GROUP_VIDEO_FOR_TAB_NO_PREVIEW = new int[]{
        ROW_SETTING_VIDEO_STABLE,//video
        ROW_SETTING_MICROPHONE,//video
        ROW_SETTING_AUDIO_MODE,//video
        ROW_SETTING_TIME_LAPSE,//video
        ROW_SETTING_LIVE_EFFECT,//video
    };
    
    //Image adjustments for final user, order by UX spec
    public static final int[] SETTING_GROUP_IMAGE_FOR_TAB = new int[]{
        ROW_SETTING_SHARPNESS,//common
        ROW_SETTING_HUE,//common
        ROW_SETTING_SATURATION,//common
        ROW_SETTING_BRIGHTNESS,//common
        ROW_SETTING_CONTRAST,//common
    };
    
    //Camera setting items can be set in Parameters. 
    public static final int[] SETTING_GROUP_CAMERA_FOR_PARAMETERS = new int[]{
        ROW_SETTING_SCENCE_MODE,//common should be checked firstly
        ROW_SETTING_PICTURE_SIZE,//camera should be checked firstly
        
        //ROW_SETTING_VIDEO_QUALITY,//video should be checked firstly
        ROW_SETTING_FLASH,//common
        //ROW_SETTING_DUAL_CAMERA,//common not in parameter
        ROW_SETTING_EXPOSURE,//common
        //ROW_SETTING_SCENCE_MODE,//common //moved to head.
        ROW_SETTING_WHITE_BALANCE,//common
        //ROW_SETTING_IMAGE_ADJUSTMENT,//common not in parameter
        ROW_SETTING_COLOR_EFFECT,//common
        //ROW_SETTING_GEO_TAG,//common not in parameter
        //ROW_SETTING_AE_METER,
        ROW_SETTING_ANTI_FLICKER,//common
        
        //ROW_SETTING_SELF_TIMER,//camera not in parameter
        ROW_SETTING_ZSD,//camera
        //ROW_SETTING_CONTINUOUS,//camera in parameter, but should be set by actor
        //ROW_SETTING_PICTURE_SIZE,//camera //moved to head.
        ROW_SETTING_ISO,//camera
        ROW_SETTING_FACEBEAUTY_SMOOTH,//camera
        ROW_SETTING_FACEBEAUTY_SKIN_COLOR,//camera
        ROW_SETTING_FACEBEAUTY_SHARP,//camera
        
        //ROW_SETTING_VIDEO_STABLE,//video
        //ROW_SETTING_MICROPHONE,//video not in parameter
        //ROW_SETTING_AUDIO_MODE,//video not in parameter
        //ROW_SETTING_TIME_LAPSE,//video not in parameter
        //ROW_SETTING_LIVE_EFFECT,//video not in parameter
        //ROW_SETTING_VIDEO_QUALITY,//video //moved to head.
        
        ROW_SETTING_SHARPNESS,//common
        ROW_SETTING_HUE,//common
        ROW_SETTING_SATURATION,//common
        ROW_SETTING_BRIGHTNESS,//common
        ROW_SETTING_CONTRAST,//common
        
        ROW_SETTING_CAMERA_MODE,//camera mode
        ROW_SETTING_CAPTURE_MODE,//not in preference
        ROW_SETTING_CONTINUOUS_NUM,//different from preference
        ROW_SETTING_RECORDING_HINT,//not in preference
        ROW_SETTING_JPEG_QUALITY,//not in preference
    };
    
    //Video setting items can be set in parameters
    public static final int[] SETTING_GROUP_VIDEO_FOR_PARAMETERS = new int[]{
        ROW_SETTING_SCENCE_MODE,//common should be checked firstly
        ROW_SETTING_VIDEO_QUALITY,//video should be checked firstly
        
        ROW_SETTING_FLASH,//common
        //ROW_SETTING_DUAL_CAMERA,//common not in parameter
        ROW_SETTING_EXPOSURE,//common
        //ROW_SETTING_SCENCE_MODE,//common //moved to head.
        ROW_SETTING_WHITE_BALANCE,//common
        //ROW_SETTING_IMAGE_ADJUSTMENT,//common not in parameter
        ROW_SETTING_COLOR_EFFECT,//common
        //ROW_SETTING_GEO_TAG,//common not in parameter
        //ROW_SETTING_AE_METER,
        ROW_SETTING_ANTI_FLICKER,//common
        
        //ROW_SETTING_SELF_TIMER,//camera not in parameter
        //ROW_SETTING_ZSD,//camera
        //ROW_SETTING_CONTINUOUS,//camera in parameter, but should be set by actor
        //ROW_SETTING_PICTURE_SIZE,//camera //moved to head.
        //ROW_SETTING_ISO,//camera
        
        ROW_SETTING_VIDEO_STABLE,//video
        //ROW_SETTING_MICROPHONE,//video not in parameter
        //ROW_SETTING_AUDIO_MODE,//video not in parameter
        //ROW_SETTING_TIME_LAPSE,//video not in parameter
        //ROW_SETTING_LIVE_EFFECT,//video not in parameter
        //ROW_SETTING_VIDEO_QUALITY,//video //moved to head.
        
        ROW_SETTING_SHARPNESS,//common
        ROW_SETTING_HUE,//common
        ROW_SETTING_SATURATION,//common
        ROW_SETTING_BRIGHTNESS,//common
        ROW_SETTING_CONTRAST,//common
        
        ROW_SETTING_CAMERA_MODE,//camera mode
        ROW_SETTING_CAPTURE_MODE,//not in preference
        ROW_SETTING_CONTINUOUS_NUM,//different from preference
        ROW_SETTING_RECORDING_HINT,//not in preference
        ROW_SETTING_JPEG_QUALITY,//not in preference
        
    };
    
    //Camera all setting items for final user.
    public static final int[] SETTING_GROUP_CAMERA_FOR_UI = new int[]{
        ROW_SETTING_FLASH,//common
        //ROW_SETTING_DUAL_CAMERA,//common
        ROW_SETTING_EXPOSURE,//common
        ROW_SETTING_SCENCE_MODE,//common
        ROW_SETTING_WHITE_BALANCE,//common
        //ROW_SETTING_IMAGE_PROPERTIES,//common
        ROW_SETTING_COLOR_EFFECT,//common
        ROW_SETTING_GEO_TAG,//common
        //ROW_SETTING_AE_METER,
        ROW_SETTING_ANTI_FLICKER,//common
        
        ROW_SETTING_SELF_TIMER,//camera
        ROW_SETTING_ZSD,//camera
        ROW_SETTING_CONTINUOUS,//camera
        ROW_SETTING_PICTURE_SIZE,//camera
        ROW_SETTING_ISO,//camera
        //ROW_SETTING_FACEBEAUTY_PROPERTIES,//camera
        ROW_SETTING_FACEBEAUTY_SMOOTH,
        ROW_SETTING_FACEBEAUTY_SKIN_COLOR,
        ROW_SETTING_FACEBEAUTY_SHARP,
        
        ROW_SETTING_SHARPNESS,//common
        ROW_SETTING_HUE,//common
        ROW_SETTING_SATURATION,//common
        ROW_SETTING_BRIGHTNESS,//common
        ROW_SETTING_CONTRAST,//common

        ROW_SETTING_PICTURE_RATIO,//camera
        ROW_SETTING_VOICE,//camera
        ROW_SETTING_CAMERA_FACE_DETECT,//camera
    };
    
    //Video all setting items for final user.
    public static final int[] SETTING_GROUP_VIDEO_FOR_UI = new int[]{
        ROW_SETTING_FLASH,//common
        //ROW_SETTING_DUAL_CAMERA,//common
        ROW_SETTING_EXPOSURE,//common
        ROW_SETTING_SCENCE_MODE,//common moved to head
        ROW_SETTING_WHITE_BALANCE,//common
        //ROW_SETTING_IMAGE_PROPERTIES,//common
        ROW_SETTING_COLOR_EFFECT,//common
        ROW_SETTING_GEO_TAG,//common
        //ROW_SETTING_AE_METER,
        ROW_SETTING_ANTI_FLICKER,//common
        
        ROW_SETTING_VIDEO_STABLE,//video
        ROW_SETTING_MICROPHONE,//video
        ROW_SETTING_AUDIO_MODE,//video
        ROW_SETTING_TIME_LAPSE,//video
        ROW_SETTING_LIVE_EFFECT,//video
        //ROW_SETTING_VIDEO_QUALITY,//video
        
        ROW_SETTING_SHARPNESS,//common
        ROW_SETTING_HUE,//common
        ROW_SETTING_SATURATION,//common
        ROW_SETTING_BRIGHTNESS,//common
        ROW_SETTING_CONTRAST,//common
        
    };
    
    public static final int[] SETTING_GROUP_ALL_IN_SCREEN = new int[]{
        ROW_SETTING_FLASH,//common
        ROW_SETTING_DUAL_CAMERA,//common not in parameter
//        ROW_SETTING_EXPOSURE,//common
//        ROW_SETTING_SCENCE_MODE,//common //moved to head.
//        ROW_SETTING_WHITE_BALANCE,//common
//        //ROW_SETTING_IMAGE_ADJUSTMENT,//common not in preference
//        ROW_SETTING_COLOR_EFFECT,//common
//        ROW_SETTING_GEO_TAG,//common not in parameter
//        //ROW_SETTING_AE_METER,
//        ROW_SETTING_ANTI_FLICKER,//common
//        
        ROW_SETTING_SELF_TIMER,//camera not in parameter
        ROW_SETTING_CAMERA_FACE_DETECT,//camera not in parameter
        ROW_SETTING_ZSD,//camera
//        ROW_SETTING_CONTINUOUS,//camera in parameter, but should be set by actor
//        ROW_SETTING_PICTURE_SIZE,//camera //moved to head.
//        ROW_SETTING_ISO,//camera
//        
//        ROW_SETTING_VIDEO_STABLE,//video
//        ROW_SETTING_MICROPHONE,//video not in parameter
//        ROW_SETTING_AUDIO_MODE,//video not in parameter
//        ROW_SETTING_TIME_LAPSE,//video not in parameter
//        ROW_SETTING_LIVE_EFFECT,//video not in parameter
//        ROW_SETTING_VIDEO_QUALITY,//video //moved to head.
//        
//        ROW_SETTING_SHARPNESS,//common
//        ROW_SETTING_HUE,//common
//        ROW_SETTING_SATURATION,//common
//        ROW_SETTING_BRIGHTNESS,//common
//        ROW_SETTING_CONTRAST,//common
    };
    
    public static final int[] SETTING_GROUP_ALL_IN_SETTING = new int[]{
        ROW_SETTING_FLASH,//common
        //ROW_SETTING_DUAL_CAMERA,//common not in parameter
        ROW_SETTING_EXPOSURE,//common
        ROW_SETTING_SCENCE_MODE,//common //moved to head.
        ROW_SETTING_WHITE_BALANCE,//common
        //ROW_SETTING_IMAGE_ADJUSTMENT,//common not in preference
        ROW_SETTING_COLOR_EFFECT,//common
        ROW_SETTING_GEO_TAG,//common not in parameter
        //ROW_SETTING_AE_METER,
        ROW_SETTING_ANTI_FLICKER,//common
        
        ROW_SETTING_SELF_TIMER,//camera not in parameter
        ROW_SETTING_ZSD,//camera
        ROW_SETTING_CONTINUOUS,//camera in parameter, but should be set by actor
        ROW_SETTING_PICTURE_SIZE,//camera //moved to head.
        ROW_SETTING_ISO,//camera
        ROW_SETTING_FACEBEAUTY_SMOOTH,//camera
        ROW_SETTING_FACEBEAUTY_SKIN_COLOR,//camera
        ROW_SETTING_FACEBEAUTY_SHARP,//camera
        ROW_SETTING_CAMERA_FACE_DETECT,//camera
        
        ROW_SETTING_VIDEO_STABLE,//video
        ROW_SETTING_MICROPHONE,//video not in parameter
        ROW_SETTING_AUDIO_MODE,//video not in parameter
        ROW_SETTING_TIME_LAPSE,//video not in parameter
        ROW_SETTING_LIVE_EFFECT,//video not in parameter
        ROW_SETTING_VIDEO_QUALITY,//video //moved to head.
        
        ROW_SETTING_SHARPNESS,//common
        ROW_SETTING_HUE,//common
        ROW_SETTING_SATURATION,//common
        ROW_SETTING_BRIGHTNESS,//common
        ROW_SETTING_CONTRAST,//common
        
        ROW_SETTING_PICTURE_RATIO,//camera
        ROW_SETTING_VOICE,//camera
    };
    
    public static final int[] SETTING_GROUP_NOT_IN_PREFERENCE = new int[]{
        ROW_SETTING_CAMERA_MODE,//camera mode
        ROW_SETTING_CAPTURE_MODE,//not in preference
        ROW_SETTING_CONTINUOUS_NUM,//different from preference
        ROW_SETTING_RECORDING_HINT,//not in preference
        ROW_SETTING_JPEG_QUALITY,//not in preference
        ROW_SETTING_IMAGE_PROPERTIES,//not in preference
        ROW_SETTING_FACEBEAUTY_PROPERTIES,//not in preference
        ROW_SETTING_VOICE,//not in preference
    };
    
    private static final String[] KEYS_FOR_SCENE = new String[]{
        Parameters.SCENE_MODE_ACTION,
        Parameters.SCENE_MODE_PORTRAIT,
        Parameters.SCENE_MODE_LANDSCAPE,
        Parameters.SCENE_MODE_NIGHT,
        Parameters.SCENE_MODE_NIGHT_PORTRAIT,
        Parameters.SCENE_MODE_THEATRE,
        Parameters.SCENE_MODE_BEACH,
        Parameters.SCENE_MODE_SNOW,
        Parameters.SCENE_MODE_SUNSET,
        Parameters.SCENE_MODE_STEADYPHOTO,
        Parameters.SCENE_MODE_FIREWORKS,
        Parameters.SCENE_MODE_SPORTS,
        Parameters.SCENE_MODE_PARTY,
        Parameters.SCENE_MODE_CANDLELIGHT,
        Parameters.SCENE_MODE_AUTO,
        ParametersHelper.KEY_SCENE_MODE_NORMAL,
        ParametersHelper.KEY_SCENE_MODE_HDR,
    };
    
    private static final Restriction[] RESTRICTIOINS = new Restriction[]{
        new Restriction(ROW_SETTING_LIVE_EFFECT) //only limit UI
            .setValues(Integer.toString(EffectsRecorder.EFFECT_NONE))
            .setRestrictions(new Restriction(ROW_SETTING_VIDEO_QUALITY)
                .setEnable(false)
                .setValues("fine")),
      new Restriction(ROW_SETTING_TIME_LAPSE) //should be checked.
            .setValues("1000", "1500", "2000", "2500", "3000", "5000", "10000")
            .setRestrictions(new Restriction(ROW_SETTING_MICROPHONE)
                .setEnable(false)
                .setValues("off"),new Restriction(ROW_SETTING_AUDIO_MODE)
                    .setEnable(false)
                    .setValues("normal")),
        new Restriction(ROW_SETTING_MICROPHONE) //should be checked.
            .setValues("off")
            .setRestrictions(new Restriction(ROW_SETTING_AUDIO_MODE)
                .setEnable(false)
                .setValues("normal")),
    };
    
    public static final MappingFinder MAPPING_FINDER_PICTURE_SIZE = new PictureSizeMappingFinder();
    public static final MappingFinder MAPPING_FINDER_FLASH = new FlashMappingFinder();
    
    private static final Restriction[] CAPABILITIES = new Restriction[] {
        new Restriction(ModePicker.MODE_SMILE_SHOT)
            .setType(Restriction.TYPE_MODE)
            .setRestrictions(new Restriction(ROW_SETTING_SCENCE_MODE)
                .setEnable(true)
                .setValues(Parameters.SCENE_MODE_AUTO, Parameters.SCENE_MODE_PORTRAIT)),
        new Restriction(ModePicker.MODE_VIDEO)
            .setType(Restriction.TYPE_MODE)
            .setRestrictions(new Restriction(ROW_SETTING_SCENCE_MODE)
                .setEnable(true)
                .setValues(Parameters.SCENE_MODE_AUTO, Parameters.SCENE_MODE_NIGHT)),
        new Restriction(ROW_SETTING_RECORDING_HINT)
            .setType(Restriction.TYPE_SETTING)
            .setValues(Boolean.toString(false))
            .setRestrictions(new Restriction(ROW_SETTING_FLASH)
                .setEnable(true)
                .setMappingFinder(MAPPING_FINDER_FLASH)
                .setValues(Parameters.FLASH_MODE_AUTO, Parameters.FLASH_MODE_ON, Parameters.FLASH_MODE_OFF)),
        new Restriction(ROW_SETTING_RECORDING_HINT)
            .setType(Restriction.TYPE_SETTING)
            .setValues(Boolean.toString(true))
            .setRestrictions(new Restriction(ROW_SETTING_FLASH)
                .setEnable(true)
                .setMappingFinder(MAPPING_FINDER_FLASH)
                .setValues(Parameters.FLASH_MODE_TORCH, Parameters.FLASH_MODE_OFF)),
        new Restriction(ROW_SETTING_PICTURE_RATIO)
            .setType(Restriction.TYPE_SETTING)
            .setValues(CameraSettings.PICTURE_RATIO_4_3)
            .setRestrictions(new Restriction(ROW_SETTING_PICTURE_SIZE)
                .setEnable(true)
                .setMappingFinder(MAPPING_FINDER_PICTURE_SIZE)
                .setValues(CameraSettings.PICTURE_SIZE_4_3)),
        new Restriction(ROW_SETTING_PICTURE_RATIO)
            .setType(Restriction.TYPE_SETTING)
            .setValues(CameraSettings.PICTURE_RATIO_16_9)
            .setRestrictions(new Restriction(ROW_SETTING_PICTURE_SIZE)
                .setEnable(true)
                .setMappingFinder(MAPPING_FINDER_PICTURE_SIZE)
                .setValues(CameraSettings.PICTURE_SIZE_16_9)),
        new Restriction(ROW_SETTING_PICTURE_RATIO)
            .setType(Restriction.TYPE_SETTING)
            .setValues(CameraSettings.PICTURE_RATIO_5_3)
            .setRestrictions(new Restriction(ROW_SETTING_PICTURE_SIZE)
                .setEnable(true)
                .setMappingFinder(MAPPING_FINDER_PICTURE_SIZE)
                .setValues(CameraSettings.PICTURE_SIZE_5_3)),
        new Restriction(ROW_SETTING_PICTURE_RATIO)
        .setType(Restriction.TYPE_SETTING)
        .setValues(CameraSettings.PICTURE_RATIO_3_2)
        .setRestrictions(new Restriction(ROW_SETTING_PICTURE_SIZE)
            .setEnable(true)
            .setMappingFinder(MAPPING_FINDER_PICTURE_SIZE)
            .setValues(CameraSettings.PICTURE_SIZE_3_2)),
    };

    private final String[] mOverrideSettingValues = new String[SETTING_ROW_COUNT];
    private static final int[][] MATRIX_MODE_STATE = new int[SETTING_ROW_COUNT][];
    private static final int[][] MATRIX_SCENE_STATE = new int[SETTING_ROW_COUNT][];
    private static final int[] DEFAULT_VALUE_FOR_SETTING_ID = new int[SETTING_ROW_COUNT];
    private static final String[] DEFAULT_VALUE_FOR_SETTING = new String[SETTING_ROW_COUNT];
    private static final boolean[][] MATRIX_SETTING_VISIBLE = new boolean[CAMERA_COUNT][];
    private static final String[][] RESET_STATE_VALUE = new String[SETTING_ROW_COUNT][];
    public static final String[] KEYS_FOR_SETTING = new String[SETTING_ROW_COUNT];
    private static final boolean[] MATRIX_ZOOM_ENABLE = new boolean[]{
        true, //photo
        true, //hdr
        true, //facebeauty
        true, //panorama
        true, //mav
        true, //asd
        true, //smile
        true, //best
        true, //ev
        true, //video
        true, //normal3d
        true, //panorama3d
    };
    
    private static final int[] MATRIX_FOCUS_MODE_DEFAULT_ARRAY = new int[]{
        R.array.pref_camera_focusmode_default_array, //photo
        R.array.pref_camera_focusmode_default_array, //hdr
        R.array.pref_camera_focusmode_default_array, //facebeauty
        R.array.pref_camera_focusmode_default_array, //panorama
        R.array.pref_camera_focusmode_default_array, //mav
        R.array.pref_camera_focusmode_default_array, //asd
        R.array.pref_camera_focusmode_default_array, //smile
        R.array.pref_camera_focusmode_default_array, //best
        R.array.pref_camera_focusmode_default_array, //ev
        R.array.pref_video_focusmode_default_array, //video
        R.array.pref_camera_focusmode_default_array, //normal3d
        R.array.pref_camera_focusmode_default_array, //panorama3d
    };
    
    private static final String[] MATRIX_FOCUS_MODE_CONTINUOUS = new String[]{
        Parameters.FOCUS_MODE_CONTINUOUS_PICTURE, //photo
        Parameters.FOCUS_MODE_CONTINUOUS_PICTURE, //hdr
        Parameters.FOCUS_MODE_CONTINUOUS_PICTURE, //facebeauty
        Parameters.FOCUS_MODE_CONTINUOUS_PICTURE, //panorama
        Parameters.FOCUS_MODE_CONTINUOUS_PICTURE, //mav
        Parameters.FOCUS_MODE_CONTINUOUS_PICTURE, //asd
        Parameters.FOCUS_MODE_CONTINUOUS_PICTURE, //smile
        Parameters.FOCUS_MODE_CONTINUOUS_PICTURE, //best
        Parameters.FOCUS_MODE_CONTINUOUS_PICTURE, //ev
        Parameters.FOCUS_MODE_CONTINUOUS_VIDEO, //video
        Parameters.FOCUS_MODE_CONTINUOUS_PICTURE, //normal3d
        Parameters.FOCUS_MODE_CONTINUOUS_PICTURE, //panorama3d
    };
    
    static {
                                                                  //photo    hdr    facebeauty    panorama    mav    asd    smile     best     ev    video    single3d    panorama3d    live effect
        MATRIX_MODE_STATE[ROW_SETTING_FLASH]            = new int[]{STATE_E0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_E0, STATE_E0, STATE_E0, STATE_R0, STATE_E0, STATE_R0, STATE_R0, STATE_E0};//0: flash
        MATRIX_MODE_STATE[ROW_SETTING_DUAL_CAMERA]      = new int[]{STATE_E0, STATE_D0, STATE_E0, STATE_D0, STATE_D0, STATE_D0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_D0, STATE_D0, STATE_E0};//1: dual camera
        MATRIX_MODE_STATE[ROW_SETTING_EXPOSURE]         = new int[]{STATE_E0, STATE_R0, STATE_R0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_R0};//2: ev
        MATRIX_MODE_STATE[ROW_SETTING_SCENCE_MODE]      = new int[]{STATE_E0, STATE_R1, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_E0, STATE_R0, STATE_R0, STATE_E0, STATE_E0, STATE_R0, STATE_D0};//3: scence mode
        MATRIX_MODE_STATE[ROW_SETTING_WHITE_BALANCE]    = new int[]{STATE_E0, STATE_R0, STATE_R0, STATE_E0, STATE_E0, STATE_R0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_D0};//4: white balance
        //image properties
        MATRIX_MODE_STATE[ROW_SETTING_COLOR_EFFECT]     = new int[]{STATE_E0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_R0, STATE_D0};//6: color effect
        MATRIX_MODE_STATE[ROW_SETTING_SELF_TIMER]       = new int[]{STATE_E0, STATE_E0, STATE_E0, STATE_R0, STATE_R0, STATE_E0, STATE_R0, STATE_R0, STATE_R0, STATE_E0, STATE_R0, STATE_R0, STATE_D0};//7: self timer
        MATRIX_MODE_STATE[ROW_SETTING_ZSD]              = new int[]{STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_D0};//8: zsd
        MATRIX_MODE_STATE[ROW_SETTING_CONTINUOUS]       = new int[]{STATE_E0, STATE_D0, STATE_D0, STATE_D0, STATE_D0, STATE_D0, STATE_D0, STATE_D0, STATE_D0, STATE_E0, STATE_D0, STATE_D0, STATE_D0};//9: continuous number
        MATRIX_MODE_STATE[ROW_SETTING_GEO_TAG]          = new int[]{STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0};//10: geo tag
        MATRIX_MODE_STATE[ROW_SETTING_PICTURE_SIZE]     = new int[]{STATE_E0, STATE_E0, STATE_E0, STATE_D0, STATE_D0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_D0, STATE_D0};//11: picture size
        MATRIX_MODE_STATE[ROW_SETTING_ISO]              = new int[]{STATE_E0, STATE_R0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_D0};//12: ISO
        //MATRIX_MODE_STATE[ROW_SETTING_AE_METER]         = new int[]{STATE_E0, STATE_D0, STATE_D0, STATE_D0, STATE_D0, STATE_E0, STATE_E0, STATE_E0, STATE_D0, STATE_D0};//13: AE meter
        MATRIX_MODE_STATE[ROW_SETTING_ANTI_FLICKER]     = new int[]{STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_D0};//14: Anti flicker
        MATRIX_MODE_STATE[ROW_SETTING_VIDEO_STABLE]     = new int[]{STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_R0, STATE_R0};//15: video stable
        MATRIX_MODE_STATE[ROW_SETTING_MICROPHONE]       = new int[]{STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0};//16 microphone
        MATRIX_MODE_STATE[ROW_SETTING_AUDIO_MODE]       = new int[]{STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_R0};//17 audio mode
        MATRIX_MODE_STATE[ROW_SETTING_TIME_LAPSE]       = new int[]{STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0};//18 timelapse
        MATRIX_MODE_STATE[ROW_SETTING_LIVE_EFFECT]      = new int[]{STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0};//19 live effect
        MATRIX_MODE_STATE[ROW_SETTING_VIDEO_QUALITY]    = new int[]{STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_R0};//20 video quality
        
        MATRIX_MODE_STATE[ROW_SETTING_PICTURE_RATIO]    = new int[]{STATE_E0, STATE_E0, STATE_E0, STATE_D0, STATE_D0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_D0, STATE_D0};//21: picture ratio
        MATRIX_MODE_STATE[ROW_SETTING_VOICE]            = new int[]{STATE_E0, STATE_E0, STATE_E0, STATE_R0, STATE_R0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_R0, STATE_D0};//22 voice
        
        MATRIX_MODE_STATE[ROW_SETTING_SHARPNESS]        = new int[]{STATE_E0, STATE_R0, STATE_D0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_D0};//30: image adjustment sharpness
        MATRIX_MODE_STATE[ROW_SETTING_HUE]              = new int[]{STATE_E0, STATE_R0, STATE_D0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_D0};//31: image adjustment hue
        MATRIX_MODE_STATE[ROW_SETTING_SATURATION]       = new int[]{STATE_E0, STATE_R0, STATE_D0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_D0};//32: image adjustment saturation
        MATRIX_MODE_STATE[ROW_SETTING_BRIGHTNESS]       = new int[]{STATE_E0, STATE_R0, STATE_D0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_D0};//33: image adjustment brightness
        MATRIX_MODE_STATE[ROW_SETTING_CONTRAST]         = new int[]{STATE_E0, STATE_R0, STATE_D0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_D0};//34: image adjustment contrast
        
        MATRIX_MODE_STATE[ROW_SETTING_CAMERA_MODE]      = new int[]{STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R1, STATE_R0, STATE_R0, STATE_R1};//21 camera mode
        MATRIX_MODE_STATE[ROW_SETTING_CAPTURE_MODE]     = new int[]{STATE_R0, STATE_R0, STATE_R2, STATE_R7, STATE_R0, STATE_R3, STATE_R4, STATE_R5, STATE_R6, STATE_R0, STATE_R0, STATE_R0, STATE_R0};//22: capture mode
        MATRIX_MODE_STATE[ROW_SETTING_CONTINUOUS_NUM]   = new int[]{STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0};//23: initial continuous number, it is different from 6 for here is real number, 6 is enable/disable state.
        MATRIX_MODE_STATE[ROW_SETTING_RECORDING_HINT]   = new int[]{STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R1, STATE_R0, STATE_R0, STATE_R1};//24 recording hint
        MATRIX_MODE_STATE[ROW_SETTING_JPEG_QUALITY]     = new int[]{STATE_R0, STATE_R0, STATE_R0, STATE_R1, STATE_R1, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0};//25 jpeg quality

        // face beauty properties 46
        MATRIX_MODE_STATE[ROW_SETTING_FACEBEAUTY_SMOOTH]     = new int[]{STATE_D0, STATE_D0, STATE_E0, STATE_D0, STATE_D0, STATE_D0, STATE_D0, STATE_D0, STATE_D0, STATE_D0, STATE_D0, STATE_D0, STATE_D0};//47: face beauty smooth
        MATRIX_MODE_STATE[ROW_SETTING_FACEBEAUTY_SKIN_COLOR] = new int[]{STATE_D0, STATE_D0, STATE_E0, STATE_D0, STATE_D0, STATE_D0, STATE_D0, STATE_D0, STATE_D0, STATE_D0, STATE_D0, STATE_D0, STATE_D0};//48: face beauty skin-color
        MATRIX_MODE_STATE[ROW_SETTING_FACEBEAUTY_SHARP]      = new int[]{STATE_D0, STATE_D0, STATE_E0, STATE_D0, STATE_D0, STATE_D0, STATE_D0, STATE_D0, STATE_D0, STATE_D0, STATE_D0, STATE_D0, STATE_D0};//49: face beauty sharp
        MATRIX_MODE_STATE[ROW_SETTING_CAMERA_FACE_DETECT]    = new int[]{STATE_E0, STATE_E0, STATE_R1, STATE_R0, STATE_R0, STATE_R1, STATE_R1, STATE_E0, STATE_E0, STATE_R0, STATE_E0, STATE_E0, STATE_D0};//50: face detection

                                                                    //action    portrait    land    night    nightport    theatre    beach    snow    sunset    steady    fireworks    spot    party    candle    auto    normal
        MATRIX_SCENE_STATE[ROW_SETTING_EXPOSURE]        = new int[]{STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R1, STATE_R1, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_E0, STATE_E0, STATE_E0};//2: ev
        MATRIX_SCENE_STATE[ROW_SETTING_WHITE_BALANCE]   = new int[]{STATE_R0, STATE_R0, STATE_R1, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R1, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R2, STATE_E0, STATE_E0, STATE_E0};//4: white balance
        //image properties
        MATRIX_SCENE_STATE[ROW_SETTING_ISO]             = new int[]{STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_E0, STATE_E0, STATE_E0};//12: ISO
        //MATRIX_SCENE_STATE[ROW_SETTING_AE_METER]        = new int[]{STATE_R0, STATE_R0, STATE_R0, STATE_E0, STATE_E0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_E0, STATE_E0, STATE_E0};//13: AE meter
        
        MATRIX_SCENE_STATE[ROW_SETTING_SHARPNESS]       = new int[]{STATE_R0, STATE_R1, STATE_R2, STATE_R1, STATE_R1, STATE_R2, STATE_R2, STATE_R2, STATE_R2, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_E0, STATE_E0, STATE_E0};//32: image adjustment sharpness
        MATRIX_SCENE_STATE[ROW_SETTING_HUE]             = new int[]{STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_E0, STATE_E0, STATE_E0};//30: image adjustment hue
        MATRIX_SCENE_STATE[ROW_SETTING_SATURATION]      = new int[]{STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R1, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_E0, STATE_E0, STATE_E0};//33: image adjustment saturation
        MATRIX_SCENE_STATE[ROW_SETTING_BRIGHTNESS]      = new int[]{STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_E0, STATE_E0, STATE_E0};//34: image adjustment brightness
        MATRIX_SCENE_STATE[ROW_SETTING_CONTRAST]        = new int[]{STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_R0, STATE_E0, STATE_E0, STATE_E0};//31: image adjustment contrast
        MATRIX_SCENE_STATE[ROW_SETTING_FLASH]           = new int[]{STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_R0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0, STATE_E0};//0: flash
        
        RESET_STATE_VALUE[ROW_SETTING_FLASH]            = new String[]{Parameters.FLASH_MODE_OFF};//off
        RESET_STATE_VALUE[ROW_SETTING_DUAL_CAMERA]      = new String[]{"0"};//none need check
        RESET_STATE_VALUE[ROW_SETTING_EXPOSURE]         = new String[]{"0", "1"};//auto need check
        RESET_STATE_VALUE[ROW_SETTING_SCENCE_MODE]      = new String[]{
                Parameters.SCENE_MODE_AUTO,
                ParametersHelper.KEY_SCENE_MODE_HDR,
        };//normal
        RESET_STATE_VALUE[ROW_SETTING_WHITE_BALANCE]    = new String[]{
                Parameters.WHITE_BALANCE_AUTO,
                Parameters.WHITE_BALANCE_DAYLIGHT,
                Parameters.WHITE_BALANCE_INCANDESCENT
        };//auto
        //image properties
        RESET_STATE_VALUE[ROW_SETTING_COLOR_EFFECT]     = new String[]{Parameters.EFFECT_NONE};//none
        RESET_STATE_VALUE[ROW_SETTING_SELF_TIMER]       = new String[]{"0"};//none
        RESET_STATE_VALUE[ROW_SETTING_ZSD]              = new String[]{ParametersHelper.ZSD_MODE_OFF};//none
        RESET_STATE_VALUE[ROW_SETTING_CONTINUOUS]       = null;//none
        RESET_STATE_VALUE[ROW_SETTING_GEO_TAG]          = null;//none
        RESET_STATE_VALUE[ROW_SETTING_PICTURE_SIZE]     = null;//none need check
        RESET_STATE_VALUE[ROW_SETTING_ISO]              = new String[]{CameraSettings.ISO_AUTO};//auto
        //RESET_STATE_VALUE[ROW_SETTING_AE_METER]         = new String[]{"center"};//should be rechecked
        RESET_STATE_VALUE[ROW_SETTING_ANTI_FLICKER]     = null;//none
        RESET_STATE_VALUE[ROW_SETTING_VIDEO_STABLE]     = new String[]{"off"};
        RESET_STATE_VALUE[ROW_SETTING_MICROPHONE]       = new String[]{"on"};
        RESET_STATE_VALUE[ROW_SETTING_AUDIO_MODE]       = new String[]{"normal"};
        RESET_STATE_VALUE[ROW_SETTING_TIME_LAPSE]       = new String[]{"off"};
        RESET_STATE_VALUE[ROW_SETTING_LIVE_EFFECT]      = new String[]{"off"};
        RESET_STATE_VALUE[ROW_SETTING_VIDEO_QUALITY]    = new String[]{"9"};
        
        RESET_STATE_VALUE[ROW_SETTING_SHARPNESS]        = new String[]{
                CameraSettings.DIP_MEDIUM,
                CameraSettings.DIP_LOW,
                CameraSettings.DIP_HIGH
        };//middle
        RESET_STATE_VALUE[ROW_SETTING_HUE]              = new String[]{CameraSettings.DIP_MEDIUM};//middle
        RESET_STATE_VALUE[ROW_SETTING_SATURATION]       = new String[]{
                CameraSettings.DIP_MEDIUM,
                CameraSettings.DIP_LOW
        };//middle
        RESET_STATE_VALUE[ROW_SETTING_BRIGHTNESS]       = new String[]{CameraSettings.DIP_MEDIUM};//middle
        RESET_STATE_VALUE[ROW_SETTING_CONTRAST]         = new String[]{CameraSettings.DIP_MEDIUM};//middle
        RESET_STATE_VALUE[ROW_SETTING_FACEBEAUTY_SMOOTH]     = new String[]{"0"};//middle
        RESET_STATE_VALUE[ROW_SETTING_FACEBEAUTY_SKIN_COLOR] = new String[]{"0"};//middle
        RESET_STATE_VALUE[ROW_SETTING_FACEBEAUTY_SHARP]      = new String[]{"0"};//middle
        RESET_STATE_VALUE[ROW_SETTING_CAMERA_FACE_DETECT]    = new String[]{"off", "on"};
        
        RESET_STATE_VALUE[ROW_SETTING_RECORDING_HINT]   = new String[]{Boolean.toString(false), Boolean.toString(true)};
        RESET_STATE_VALUE[ROW_SETTING_CAPTURE_MODE]     = new String[] {
                Parameters.CAPTURE_MODE_NORMAL,
                Parameters.CAPTURE_MODE_HDR,
                Parameters.CAPTURE_MODE_FB,
                Parameters.CAPTURE_MODE_ASD,
                Parameters.CAPTURE_MODE_SMILE_SHOT,
                Parameters.CAPTURE_MODE_BEST_SHOT,
                Parameters.CAPTURE_MODE_EV_BRACKET_SHOT,
                "autorama"
        };
        RESET_STATE_VALUE[ROW_SETTING_CONTINUOUS_NUM]   = new String[]{"1"};
        RESET_STATE_VALUE[ROW_SETTING_JPEG_QUALITY]     = new String[]{
                Integer.toString(CameraProfile.QUALITY_HIGH),
                Integer.toString(CameraProfile.QUALITY_HIGH),//should be rechecked
        };
        RESET_STATE_VALUE[ROW_SETTING_CAMERA_MODE]      = new String[]{
                Integer.toString(Parameters.CAMERA_MODE_MTK_PRV),
                Integer.toString(Parameters.CAMERA_MODE_MTK_VDO),
        };
        RESET_STATE_VALUE[ROW_SETTING_VOICE]            = new String[]{VoiceManager.VOICE_OFF};//off
                          
        KEYS_FOR_SETTING[ROW_SETTING_FLASH]             = CameraSettings.KEY_FLASH_MODE;
        KEYS_FOR_SETTING[ROW_SETTING_DUAL_CAMERA]       = CameraSettings.KEY_CAMERA_ID;//need recheck
        KEYS_FOR_SETTING[ROW_SETTING_EXPOSURE]          = CameraSettings.KEY_EXPOSURE;
        KEYS_FOR_SETTING[ROW_SETTING_SCENCE_MODE]       = CameraSettings.KEY_SCENE_MODE;
        KEYS_FOR_SETTING[ROW_SETTING_WHITE_BALANCE]     = CameraSettings.KEY_WHITE_BALANCE;
        KEYS_FOR_SETTING[ROW_SETTING_IMAGE_PROPERTIES]  = CameraSettings.KEY_IMAGE_PROPERTIES;
        KEYS_FOR_SETTING[ROW_SETTING_COLOR_EFFECT]      = CameraSettings.KEY_COLOR_EFFECT;
        KEYS_FOR_SETTING[ROW_SETTING_SELF_TIMER]        = CameraSettings.KEY_SELF_TIMER;
        KEYS_FOR_SETTING[ROW_SETTING_ZSD]               = CameraSettings.KEY_CAMERA_ZSD;
        KEYS_FOR_SETTING[ROW_SETTING_CONTINUOUS]        = CameraSettings.KEY_CONTINUOUS_NUMBER;
        KEYS_FOR_SETTING[ROW_SETTING_GEO_TAG]           = CameraSettings.KEY_RECORD_LOCATION;//need recheck
        KEYS_FOR_SETTING[ROW_SETTING_PICTURE_SIZE]      = CameraSettings.KEY_PICTURE_SIZE;
        KEYS_FOR_SETTING[ROW_SETTING_ISO]               = CameraSettings.KEY_ISO;
        
        //KEYS_FOR_SETTING[ROW_SETTING_AE_METER]          = CameraSettings.KEY_EXPOSURE_METER;
        KEYS_FOR_SETTING[ROW_SETTING_ANTI_FLICKER]      = CameraSettings.KEY_ANTI_BANDING;
        KEYS_FOR_SETTING[ROW_SETTING_VIDEO_STABLE]      = CameraSettings.KEY_VIDEO_EIS;
        KEYS_FOR_SETTING[ROW_SETTING_MICROPHONE]        = CameraSettings.KEY_VIDEO_RECORD_AUDIO;
        KEYS_FOR_SETTING[ROW_SETTING_AUDIO_MODE]        = CameraSettings.KEY_VIDEO_HD_AUDIO_RECORDING;
        KEYS_FOR_SETTING[ROW_SETTING_TIME_LAPSE]        = CameraSettings.KEY_VIDEO_TIME_LAPSE_FRAME_INTERVAL;
        KEYS_FOR_SETTING[ROW_SETTING_LIVE_EFFECT]       = CameraSettings.KEY_VIDEO_EFFECT;
        KEYS_FOR_SETTING[ROW_SETTING_VIDEO_QUALITY]     = CameraSettings.KEY_VIDEO_QUALITY;
        KEYS_FOR_SETTING[ROW_SETTING_STEREO_MODE]       = CameraSettings.KEY_STEREO3D_MODE;
        
        KEYS_FOR_SETTING[ROW_SETTING_SHARPNESS]         = CameraSettings.KEY_EDGE;
        KEYS_FOR_SETTING[ROW_SETTING_HUE]               = CameraSettings.KEY_HUE;
        KEYS_FOR_SETTING[ROW_SETTING_SATURATION]        = CameraSettings.KEY_SATURATION;
        KEYS_FOR_SETTING[ROW_SETTING_BRIGHTNESS]        = CameraSettings.KEY_BRIGHTNESS;
        KEYS_FOR_SETTING[ROW_SETTING_CONTRAST]          = CameraSettings.KEY_CONTRAST;

        KEYS_FOR_SETTING[ROW_SETTING_PICTURE_RATIO]     = CameraSettings.KEY_PICTURE_RATIO;
        KEYS_FOR_SETTING[ROW_SETTING_VOICE]             = CameraSettings.KEY_VOICE;
        KEYS_FOR_SETTING[ROW_SETTING_FACEBEAUTY_PROPERTIES] = CameraSettings.KEY_FACE_BEAUTY_PROPERTIES;
        KEYS_FOR_SETTING[ROW_SETTING_FACEBEAUTY_SMOOTH]     = CameraSettings.KEY_FACE_BEAUTY_SMOOTH;
        KEYS_FOR_SETTING[ROW_SETTING_FACEBEAUTY_SKIN_COLOR] = CameraSettings.KEY_FACE_BEAUTY_SKIN_COLOR;
        KEYS_FOR_SETTING[ROW_SETTING_FACEBEAUTY_SHARP]      = CameraSettings.KEY_FACE_BEAUTY_SHARP;
        KEYS_FOR_SETTING[ROW_SETTING_CAMERA_FACE_DETECT]    = CameraSettings.KEY_CAMERA_FACE_DETECT;
        
        
        MATRIX_SETTING_VISIBLE[CAMERA_BACK_ID] = new boolean[SETTING_ROW_COUNT];
        MATRIX_SETTING_VISIBLE[CAMERA_BACK_ID][ROW_SETTING_FLASH]           = true;
        MATRIX_SETTING_VISIBLE[CAMERA_BACK_ID][ROW_SETTING_DUAL_CAMERA]     = true;
        MATRIX_SETTING_VISIBLE[CAMERA_BACK_ID][ROW_SETTING_EXPOSURE]        = true;
        MATRIX_SETTING_VISIBLE[CAMERA_BACK_ID][ROW_SETTING_SCENCE_MODE]     = true;
        MATRIX_SETTING_VISIBLE[CAMERA_BACK_ID][ROW_SETTING_WHITE_BALANCE]   = true;
        MATRIX_SETTING_VISIBLE[CAMERA_BACK_ID][ROW_SETTING_IMAGE_PROPERTIES] = true;
        MATRIX_SETTING_VISIBLE[CAMERA_BACK_ID][ROW_SETTING_COLOR_EFFECT]    = true;
        MATRIX_SETTING_VISIBLE[CAMERA_BACK_ID][ROW_SETTING_SELF_TIMER]      = true;
        MATRIX_SETTING_VISIBLE[CAMERA_BACK_ID][ROW_SETTING_ZSD]             = true;
        MATRIX_SETTING_VISIBLE[CAMERA_BACK_ID][ROW_SETTING_CONTINUOUS]      = true;
        MATRIX_SETTING_VISIBLE[CAMERA_BACK_ID][ROW_SETTING_GEO_TAG]         = true;
        MATRIX_SETTING_VISIBLE[CAMERA_BACK_ID][ROW_SETTING_PICTURE_SIZE]    = true;
        MATRIX_SETTING_VISIBLE[CAMERA_BACK_ID][ROW_SETTING_ISO]             = true;
        //MATRIX_SETTING_VISIBLE[CAMERA_BACK_ID][ROW_SETTING_AE_METER]        = true;
        MATRIX_SETTING_VISIBLE[CAMERA_BACK_ID][ROW_SETTING_ANTI_FLICKER]    = true;
        MATRIX_SETTING_VISIBLE[CAMERA_BACK_ID][ROW_SETTING_VIDEO_STABLE]    = true;
        MATRIX_SETTING_VISIBLE[CAMERA_BACK_ID][ROW_SETTING_MICROPHONE]      = true;
        MATRIX_SETTING_VISIBLE[CAMERA_BACK_ID][ROW_SETTING_AUDIO_MODE]      = true;
        MATRIX_SETTING_VISIBLE[CAMERA_BACK_ID][ROW_SETTING_TIME_LAPSE]      = true;
        MATRIX_SETTING_VISIBLE[CAMERA_BACK_ID][ROW_SETTING_LIVE_EFFECT]     = true;
        MATRIX_SETTING_VISIBLE[CAMERA_BACK_ID][ROW_SETTING_VIDEO_QUALITY]   = true;
        MATRIX_SETTING_VISIBLE[CAMERA_BACK_ID][ROW_SETTING_STEREO_MODE]     = true;
        
        MATRIX_SETTING_VISIBLE[CAMERA_BACK_ID][ROW_SETTING_SHARPNESS]       = true;
        MATRIX_SETTING_VISIBLE[CAMERA_BACK_ID][ROW_SETTING_HUE]             = true;
        MATRIX_SETTING_VISIBLE[CAMERA_BACK_ID][ROW_SETTING_SATURATION]      = true;
        MATRIX_SETTING_VISIBLE[CAMERA_BACK_ID][ROW_SETTING_BRIGHTNESS]      = true;
        MATRIX_SETTING_VISIBLE[CAMERA_BACK_ID][ROW_SETTING_CONTRAST]        = true;
        
        MATRIX_SETTING_VISIBLE[CAMERA_BACK_ID][ROW_SETTING_PICTURE_RATIO]   = true;
        MATRIX_SETTING_VISIBLE[CAMERA_BACK_ID][ROW_SETTING_VOICE]           = true;
        MATRIX_SETTING_VISIBLE[CAMERA_BACK_ID][ROW_SETTING_FACEBEAUTY_PROPERTIES] = true;
        MATRIX_SETTING_VISIBLE[CAMERA_BACK_ID][ROW_SETTING_FACEBEAUTY_SMOOTH]     = true;
        MATRIX_SETTING_VISIBLE[CAMERA_BACK_ID][ROW_SETTING_FACEBEAUTY_SKIN_COLOR] = true;
        MATRIX_SETTING_VISIBLE[CAMERA_BACK_ID][ROW_SETTING_FACEBEAUTY_SHARP]      = true;
        MATRIX_SETTING_VISIBLE[CAMERA_BACK_ID][ROW_SETTING_CAMERA_FACE_DETECT]    = true;
        
        
        MATRIX_SETTING_VISIBLE[CAMERA_FRONT_ID] = new boolean[SETTING_ROW_COUNT];
        MATRIX_SETTING_VISIBLE[CAMERA_FRONT_ID][ROW_SETTING_FLASH]          = true;
        MATRIX_SETTING_VISIBLE[CAMERA_FRONT_ID][ROW_SETTING_DUAL_CAMERA]    = true;
        MATRIX_SETTING_VISIBLE[CAMERA_FRONT_ID][ROW_SETTING_EXPOSURE]       = true;
        MATRIX_SETTING_VISIBLE[CAMERA_FRONT_ID][ROW_SETTING_SCENCE_MODE]    = true;
        MATRIX_SETTING_VISIBLE[CAMERA_FRONT_ID][ROW_SETTING_WHITE_BALANCE]  = true;
        MATRIX_SETTING_VISIBLE[CAMERA_FRONT_ID][ROW_SETTING_IMAGE_PROPERTIES] = true;
        MATRIX_SETTING_VISIBLE[CAMERA_FRONT_ID][ROW_SETTING_COLOR_EFFECT]   = true;
        MATRIX_SETTING_VISIBLE[CAMERA_FRONT_ID][ROW_SETTING_SELF_TIMER]     = true;
        MATRIX_SETTING_VISIBLE[CAMERA_FRONT_ID][ROW_SETTING_ZSD]            = true;
        MATRIX_SETTING_VISIBLE[CAMERA_FRONT_ID][ROW_SETTING_CONTINUOUS]     = false;
        MATRIX_SETTING_VISIBLE[CAMERA_FRONT_ID][ROW_SETTING_GEO_TAG]        = true;
        MATRIX_SETTING_VISIBLE[CAMERA_FRONT_ID][ROW_SETTING_PICTURE_SIZE]   = true;
        MATRIX_SETTING_VISIBLE[CAMERA_FRONT_ID][ROW_SETTING_ISO]            = true;
        //MATRIX_SETTING_VISIBLE[CAMERA_FRONT_ID][ROW_SETTING_AE_METER]       = true;
        MATRIX_SETTING_VISIBLE[CAMERA_FRONT_ID][ROW_SETTING_ANTI_FLICKER]   = true;
        MATRIX_SETTING_VISIBLE[CAMERA_FRONT_ID][ROW_SETTING_VIDEO_STABLE]   = true;
        MATRIX_SETTING_VISIBLE[CAMERA_FRONT_ID][ROW_SETTING_MICROPHONE]     = true;
        MATRIX_SETTING_VISIBLE[CAMERA_FRONT_ID][ROW_SETTING_AUDIO_MODE]     = true;
        MATRIX_SETTING_VISIBLE[CAMERA_FRONT_ID][ROW_SETTING_TIME_LAPSE]     = true;
        MATRIX_SETTING_VISIBLE[CAMERA_FRONT_ID][ROW_SETTING_LIVE_EFFECT]    = true;
        MATRIX_SETTING_VISIBLE[CAMERA_FRONT_ID][ROW_SETTING_VIDEO_QUALITY]  = true;
        MATRIX_SETTING_VISIBLE[CAMERA_FRONT_ID][ROW_SETTING_STEREO_MODE]    = false;
        
        MATRIX_SETTING_VISIBLE[CAMERA_FRONT_ID][ROW_SETTING_SHARPNESS]       = true;
        MATRIX_SETTING_VISIBLE[CAMERA_FRONT_ID][ROW_SETTING_HUE]             = true;
        MATRIX_SETTING_VISIBLE[CAMERA_FRONT_ID][ROW_SETTING_SATURATION]      = true;
        MATRIX_SETTING_VISIBLE[CAMERA_FRONT_ID][ROW_SETTING_BRIGHTNESS]      = true;
        MATRIX_SETTING_VISIBLE[CAMERA_FRONT_ID][ROW_SETTING_CONTRAST]        = true;
        
        MATRIX_SETTING_VISIBLE[CAMERA_FRONT_ID][ROW_SETTING_PICTURE_RATIO]   = true;
        MATRIX_SETTING_VISIBLE[CAMERA_FRONT_ID][ROW_SETTING_VOICE]           = true;
        MATRIX_SETTING_VISIBLE[CAMERA_FRONT_ID][ROW_SETTING_FACEBEAUTY_PROPERTIES] = true;
        MATRIX_SETTING_VISIBLE[CAMERA_FRONT_ID][ROW_SETTING_FACEBEAUTY_SMOOTH]     = true;
        MATRIX_SETTING_VISIBLE[CAMERA_FRONT_ID][ROW_SETTING_FACEBEAUTY_SKIN_COLOR] = true;
        MATRIX_SETTING_VISIBLE[CAMERA_FRONT_ID][ROW_SETTING_FACEBEAUTY_SHARP]      = true;
        MATRIX_SETTING_VISIBLE[CAMERA_FRONT_ID][ROW_SETTING_CAMERA_FACE_DETECT]    = true;
        
        DEFAULT_VALUE_FOR_SETTING_ID[ROW_SETTING_FLASH]          = R.string.pref_camera_flashmode_default;
        DEFAULT_VALUE_FOR_SETTING_ID[ROW_SETTING_DUAL_CAMERA]    = R.string.pref_camera_id_default;//need recheck
        DEFAULT_VALUE_FOR_SETTING_ID[ROW_SETTING_EXPOSURE]       = R.string.pref_camera_exposure_default;
        DEFAULT_VALUE_FOR_SETTING_ID[ROW_SETTING_SCENCE_MODE]    = R.string.pref_camera_scenemode_default;
        DEFAULT_VALUE_FOR_SETTING_ID[ROW_SETTING_WHITE_BALANCE]  = R.string.pref_camera_whitebalance_default;
        //DEFAULT_VALUE_FOR_SETTING_ID[ROW_SETTING_HUE]            = R.string.pref_camera_hue_default;
        DEFAULT_VALUE_FOR_SETTING_ID[ROW_SETTING_COLOR_EFFECT]   = R.string.pref_camera_coloreffect_default;
        DEFAULT_VALUE_FOR_SETTING_ID[ROW_SETTING_SELF_TIMER]     = R.string.pref_camera_selftimer_default;
        DEFAULT_VALUE_FOR_SETTING_ID[ROW_SETTING_ZSD]            = R.string.pref_camera_zsd_default;
        DEFAULT_VALUE_FOR_SETTING_ID[ROW_SETTING_CONTINUOUS]     = R.string.pref_camera_continuous_number_default;
        DEFAULT_VALUE_FOR_SETTING_ID[ROW_SETTING_GEO_TAG]        = R.string.pref_camera_recordlocation_default;//need recheck
        DEFAULT_VALUE_FOR_SETTING_ID[ROW_SETTING_PICTURE_SIZE]   = UNKNOWN;
        DEFAULT_VALUE_FOR_SETTING_ID[ROW_SETTING_ISO]            = R.string.pref_camera_iso_default;
        
        //DEFAULT_VALUE_FOR_SETTING_ID[ROW_SETTING_AE_METER]       = R.string.pref_camera_exposuremeter_default;
        DEFAULT_VALUE_FOR_SETTING_ID[ROW_SETTING_ANTI_FLICKER]   = R.string.pref_camera_antibanding_default;
        DEFAULT_VALUE_FOR_SETTING_ID[ROW_SETTING_VIDEO_STABLE]   = R.string.pref_camera_eis_default;
        DEFAULT_VALUE_FOR_SETTING_ID[ROW_SETTING_MICROPHONE]     = R.string.pref_camera_recordaudio_default;
        DEFAULT_VALUE_FOR_SETTING_ID[ROW_SETTING_AUDIO_MODE]     = R.string.pref_video_hd_recording_default;
        DEFAULT_VALUE_FOR_SETTING_ID[ROW_SETTING_TIME_LAPSE]     = R.string.pref_video_time_lapse_frame_interval_default;
        DEFAULT_VALUE_FOR_SETTING_ID[ROW_SETTING_LIVE_EFFECT]    = R.string.pref_video_effect_default;
        DEFAULT_VALUE_FOR_SETTING_ID[ROW_SETTING_VIDEO_QUALITY]  = R.string.pref_video_quality_default;
        DEFAULT_VALUE_FOR_SETTING_ID[ROW_SETTING_STEREO_MODE]    = R.string.pref_stereo3d_mode_default;
        
        DEFAULT_VALUE_FOR_SETTING_ID[ROW_SETTING_SHARPNESS]      = R.string.pref_camera_edge_default;
        DEFAULT_VALUE_FOR_SETTING_ID[ROW_SETTING_HUE]            = R.string.pref_camera_hue_default;
        DEFAULT_VALUE_FOR_SETTING_ID[ROW_SETTING_SATURATION]     = R.string.pref_camera_saturation_default;
        DEFAULT_VALUE_FOR_SETTING_ID[ROW_SETTING_BRIGHTNESS]     = R.string.pref_camera_brightness_default;
        DEFAULT_VALUE_FOR_SETTING_ID[ROW_SETTING_CONTRAST]       = R.string.pref_camera_contrast_default;
        
        DEFAULT_VALUE_FOR_SETTING_ID[ROW_SETTING_PICTURE_RATIO]  = UNKNOWN;
        DEFAULT_VALUE_FOR_SETTING_ID[ROW_SETTING_VOICE]          = R.string.pref_voice_default;
        DEFAULT_VALUE_FOR_SETTING_ID[ROW_SETTING_FACEBEAUTY_SMOOTH]     = R.string.pref_facebeauty_smooth_default;
        DEFAULT_VALUE_FOR_SETTING_ID[ROW_SETTING_FACEBEAUTY_SKIN_COLOR] = R.string.pref_facebeauty_skin_color_default;
        DEFAULT_VALUE_FOR_SETTING_ID[ROW_SETTING_FACEBEAUTY_SHARP]      = R.string.pref_facebeauty_sharp_default;
        DEFAULT_VALUE_FOR_SETTING_ID[ROW_SETTING_CAMERA_FACE_DETECT]    = R.string.pref_camera_face_detect_default;
    }
    
    public static String[] getSettingKeys(int[] indexes) {
        if (indexes != null) {
            int len = indexes.length;
            String[] keys = new String[len];
            for (int i = 0; i < len; i++) {
                keys[i] = KEYS_FOR_SETTING[indexes[i]];
            }
            return keys;
        }
        return null;
    }
    
    public static boolean isZoomEnable(int mode) {
        return MATRIX_ZOOM_ENABLE[mode];
    }
    
    private static int[] getModeSettingGroupForUI(int mode) {
        return isVideoMode(mode) ? SETTING_GROUP_VIDEO_FOR_UI : SETTING_GROUP_CAMERA_FOR_UI;
    }
    
    private static int[] getModeSettingGroupForParameters(int mode) {
        return isVideoMode(mode) ? SETTING_GROUP_VIDEO_FOR_PARAMETERS : SETTING_GROUP_CAMERA_FOR_PARAMETERS;
    }
    
    public static boolean isVideoMode(int mode) {
        int state = MATRIX_MODE_STATE[ROW_SETTING_RECORDING_HINT][mode];
        int column = state % STATE_OFFSET;
        String value = RESET_STATE_VALUE[ROW_SETTING_RECORDING_HINT][column];
        boolean video = Boolean.parseBoolean(value);
        if (LOG) {
            Log.v(TAG, "isVideoMode(" + mode + ") return " + video);
        }
        return video;
    }
    
    public static int getCameraMode(int mode) {
        int cameraMode = MATRIX_MODE_STATE[ROW_SETTING_CAMERA_MODE][mode];
        if (LOG) {
            Log.v(TAG, "getCameraMode(" + mode + ") return " + cameraMode);
        }
        return cameraMode;
    }
    
    public static String[] getModeDefaultFocusModes(Context context, int mode) {
        return context.getResources().getStringArray(MATRIX_FOCUS_MODE_DEFAULT_ARRAY[mode]);
    }
    
    public static String getModeContinousFocusMode(int mode) {
        //In video mode, should set parameter focus mode FOCUS_MODE_AUTO
        if (mode == ModePicker.MODE_VIDEO && FeatureSwitcher.isContinuousFocusEnabledWhenTouch()) {
            return null;
        } else {
            return MATRIX_FOCUS_MODE_CONTINUOUS[mode];
        }
    }
    
    private static String[] getSettingKeyValues(int[][] matrix, int column, int[] appliedGroup) {
        if (LOG) {
            Log.v(TAG, "getSettingKeyValues(" + column + ")");
        }
        List<String> keyvalues = new ArrayList<String>();
        for (int i = 0, len = appliedGroup.length; i < len; i++) {
            int row = appliedGroup[i];
            int[] settings = matrix[row];
            //just apply given group
            if (settings == null) {
                continue;
            }
            int state = matrix[row][column];
            switch(state) {
            case STATE_E0:
                //We don't override enable state for we have enable it
                //before applyParametersToUI().
                //keyvalues.add(KEYS_FOR_SETTING[row]);
                //keyvalues.add(null);
                break;
            case STATE_D0:
                keyvalues.add(KEYS_FOR_SETTING[row]);
                keyvalues.add(SettingUtils.RESET_STATE_VALUE_DISABLE);
                break;
            case STATE_R0:
                keyvalues.add(KEYS_FOR_SETTING[row]);
                keyvalues.add(RESET_STATE_VALUE[row][0]);
                break;
            case STATE_R1:
                keyvalues.add(KEYS_FOR_SETTING[row]);
                keyvalues.add(RESET_STATE_VALUE[row][1]);
                break;
            case STATE_R2:
                keyvalues.add(KEYS_FOR_SETTING[row]);
                keyvalues.add(RESET_STATE_VALUE[row][2]);
                break;
            case STATE_R3:
                keyvalues.add(KEYS_FOR_SETTING[row]);
                keyvalues.add(RESET_STATE_VALUE[row][3]);
                break;
            case STATE_R4:
                keyvalues.add(KEYS_FOR_SETTING[row]);
                keyvalues.add(RESET_STATE_VALUE[row][4]);
                break;
            case STATE_R5:
                keyvalues.add(KEYS_FOR_SETTING[row]);
                keyvalues.add(RESET_STATE_VALUE[row][5]);
                break;
            case STATE_R6:
                keyvalues.add(KEYS_FOR_SETTING[row]);
                keyvalues.add(RESET_STATE_VALUE[row][6]);
                break;
            case STATE_R7:
                keyvalues.add(KEYS_FOR_SETTING[row]);
                keyvalues.add(RESET_STATE_VALUE[row][7]);
                break;
            default:
                break;
            }
        }
        return keyvalues.toArray(new String[keyvalues.size()]);
    }
    
    /// M: return mode table key-values
    private static String[] getModeTableKeyValues(int mode, int[] appliedGroup) {
        return getSettingKeyValues(MATRIX_MODE_STATE, mode, appliedGroup);
    }
    
    /// M: return scene mode table key-values
    private static String[] getSceneTableKeyValues(Parameters parameters, int[] appliedGroup) {
        int sceneMode = SettingUtils.index(KEYS_FOR_SCENE, getParameterValue(parameters, ROW_SETTING_SCENCE_MODE));
        return getSettingKeyValues(MATRIX_SCENE_STATE, sceneMode, appliedGroup);
    }
    
    private static String[] getRestrictionsKeyValues(Context context, ComboPreferences preferences,
            Parameters parameters, int[] appliedGroup) {
        List<String> keyvalues = new ArrayList<String>();
        for (int i = 0, len = RESTRICTIOINS.length; i < len; i++) {
            Restriction curRestriction = RESTRICTIOINS[i];
            if (curRestriction == null || !SettingUtils.contains(appliedGroup, curRestriction.getIndex())) {
                continue;
            }
            int curRow = curRestriction.getIndex();
            String curValue = null;
            if (SettingUtils.contains(SETTING_GROUP_CAMERA_FOR_PARAMETERS, curRow)) {
                curValue = getParameterValue(parameters, curRow);
            } else if (!SettingUtils.contains(SETTING_GROUP_NOT_IN_PREFERENCE, curRow)) {
                curValue = getPreferenceValue(context, preferences, curRow);
            }
            List<String> limitedValues = curRestriction.getValues();
            if (limitedValues != null && limitedValues.contains(curValue)) {
                for (Restriction restriction : curRestriction.getRestrictioins()) {
                    if (!SettingUtils.contains(appliedGroup, restriction.getIndex())) {
                        continue;
                    }
                    //limit UI to defined value
                    keyvalues.add(KEYS_FOR_SETTING[restriction.getIndex()]);
                    keyvalues.add(restriction.getValues().get(0));
                }
            }
        }
        int size = keyvalues.size();
        if (size > 0) {
            return keyvalues.toArray(new String[size]);
        }
        return null;
    }
    
    private static String[] getCapabilityKeyValues(Context context, ComboPreferences preferences,
            Parameters parameters, int[] appliedGroup, int mode) {
        List<String> keyvalues = new ArrayList<String>();
        for (int i = 0, len = CAPABILITIES.length; i < len; i++) {
            Restriction curRestriction = CAPABILITIES[i];
            if (curRestriction == null) {
                continue;
            }
            if (curRestriction.getType() == Restriction.TYPE_MODE) { //mode type
                int limitedMode = curRestriction.getIndex();
                //find valid mode restriction
                if ((limitedMode == UNKNOWN || limitedMode == mode) && curRestriction.getRestrictioins() != null) {
                    fillCapabilityKeyValues(context, preferences, parameters, appliedGroup, keyvalues, curRestriction);
                }
            } else {
                int curRow = curRestriction.getIndex();
                String curValue = null;
                if (ROW_SETTING_RECORDING_HINT == curRow) {
                    curValue = Boolean.toString(isVideoMode(mode));
                } else if (SettingUtils.contains(SETTING_GROUP_CAMERA_FOR_PARAMETERS, curRow)) {
                    curValue = getParameterValue(parameters, curRow);
                } else if (!SettingUtils.contains(SETTING_GROUP_NOT_IN_PREFERENCE, curRow)) {
                    curValue = getPreferenceValue(context, preferences, curRow);
                }
                List<String> limitedValues = curRestriction.getValues();
                if (limitedValues != null && limitedValues.contains(curValue)) {
                    fillCapabilityKeyValues(context, preferences, parameters, appliedGroup, keyvalues, curRestriction);
                }
            }
        }
        // add for capture intent
        if (((Camera)context).isImageCaptureIntent()) {
            keyvalues.add(KEYS_FOR_SETTING[ROW_SETTING_ZSD]);
            keyvalues.add(ParametersHelper.ZSD_MODE_OFF);
        }
        int size = keyvalues.size();
        if (size > 0) {
            return keyvalues.toArray(new String[size]);
        }
        return null;
    }

    private static void fillCapabilityKeyValues(Context context, ComboPreferences preferences, Parameters parameters,
            int[] appliedGroup, List<String> keyvalues, Restriction curRestriction) {
        for (Restriction restriction : curRestriction.getRestrictioins()) {
            int limitedRow = restriction.getIndex();
            if (!SettingUtils.contains(appliedGroup, limitedRow)) {
                continue;
            }
            String limitedValue = null;
            if (SettingUtils.contains(SETTING_GROUP_CAMERA_FOR_PARAMETERS, limitedRow)) {
                limitedValue = getParameterValue(parameters, limitedRow);
            } else if (!SettingUtils.contains(SETTING_GROUP_NOT_IN_PREFERENCE, limitedRow)) {
                limitedValue = getPreferenceValue(context, preferences, limitedRow);
            }
            //limit UI to defined values
            keyvalues.add(KEYS_FOR_SETTING[limitedRow]);
            keyvalues.add(SettingUtils.buildEnableList(
                    restriction.getValues().toArray(new String[]{}), limitedValue));
        }
    }

    private static void applyTableToParameters(Context context, ComboPreferences preferences,
            Parameters parameters, int[][] matrix, int column, int[] appliedGroup) {
        List<String> keyvalues = new ArrayList<String>();
        for (int i = 0, len = appliedGroup.length; i < len; i++) {
            int row = appliedGroup[i];
            int[] settings = matrix[row];
            //just apply given group
            //if user override it, don't set it again.
            if (settings == null || ((Camera)context).getSettingChecker().getOverrideSettingValue(row) != null) {
                continue;
            }
            String key = KEYS_FOR_SETTING[row];
            int state = settings[column];
            String value = null;
            switch(state) {
            case STATE_E0:
                //value has been reset by preference, so here do nothing
                break;
            case STATE_D0:
                //use last value as disable value, so here do nothing.
                break;
            case STATE_R0:
                value = RESET_STATE_VALUE[row][0];
                break;
            case STATE_R1:
                value = RESET_STATE_VALUE[row][1];
                break;
            case STATE_R2:
                value = RESET_STATE_VALUE[row][2];
                break;
            case STATE_R3:
                value = RESET_STATE_VALUE[row][3];
                break;
            case STATE_R4:
                value = RESET_STATE_VALUE[row][4];
                break;
            case STATE_R5:
                value = RESET_STATE_VALUE[row][5];
                break;
            case STATE_R6:
                value = RESET_STATE_VALUE[row][6];
                break;
            case STATE_R7:
                value = RESET_STATE_VALUE[row][7];
                break;
            default:
                break;
            }
            if (value != null) {
                if (isParametersSupportedValue(parameters, row, value)) {
                    setParameterValue(context, parameters, row, value);
                } else {
                    Log.w(TAG, "applyTableToParameters() not support row=" + row + ", value=" + value);
                }
            }
        }
    }
    
    /// M: mode table --> parameters @{
    private static void applyModeTableToParameters(Context context, ComboPreferences preferences, Parameters parameters,
            int[] appliedGroup, int mode) {
        //Just apply parameters not in preference for that:
        //parameters in preference has been applied in applyPreferenceToParameters().
        List<Integer> modeList = new ArrayList<Integer>();
        for (int i = 0, len = SETTING_GROUP_NOT_IN_PREFERENCE.length; i < len; i++) {
            int row = SETTING_GROUP_NOT_IN_PREFERENCE[i];
            if (SettingUtils.contains(appliedGroup, row)) {
                modeList.add(row);
            }
        }
        int size = modeList.size();
        int[] appliedGroup2 = new int[size];
        for (int i = 0; i < size; i++) {
            appliedGroup2[i] = modeList.get(i);
        }
        applyTableToParameters(context, preferences, parameters, MATRIX_MODE_STATE, mode, appliedGroup2);
    }
    
    /// M: scene table --> parameters @{
    private static void applySceneTableToParameters(Context context, ComboPreferences preferences, Parameters parameters,
            int[] appliedGroup) {
        int sceneMode = SettingUtils.index(KEYS_FOR_SCENE, getParameterValue(parameters, ROW_SETTING_SCENCE_MODE));
        applyTableToParameters(context, preferences, parameters, MATRIX_SCENE_STATE, sceneMode, appliedGroup);
    }
    /// @}
    
    /// M: restrictions --> parameters @{
    private static void applyRestrictionsToParameters(Context context, Parameters parameters, int[] appliedGroup) {
        for (int i = 0, len = RESTRICTIOINS.length; i < len; i++) {
            Restriction curRestriction = RESTRICTIOINS[i];
            if (curRestriction == null || !SettingUtils.contains(appliedGroup, curRestriction.getIndex())) {
                continue;
            }
            //just apply given group
            int curRow = curRestriction.getIndex();
            String curValue = getParameterValue(parameters, curRow);
            List<String> limitedValues = curRestriction.getValues();
            if (limitedValues != null && limitedValues.contains(curValue)) {
                for (Restriction restriction : curRestriction.getRestrictioins()) {
                    int limitedRow = restriction.getIndex();
                    if (!SettingUtils.contains(appliedGroup, limitedRow)) {
                        continue;
                    }
                    //limit parameter to defined value
                    String value = null;
                    if (restriction.getValues().size() > 1 && context instanceof Camera) {
                        /* For preview ratio, picture size, ISO case:
                         * We should set different picture size for different preview ratio.
                         */
                        ListPreference pref = ((Camera)context).getListPreference(limitedRow);
                        if (pref != null) {
                            for (String temp : restriction.getValues()) {
                                if (SettingUtils.contains(pref.getEntryValues(), temp)) {
                                    value = temp;
                                    break;
                                }
                            }
                        }
                    }
                    if (value == null) {
                        value = restriction.getValues().get(0);
                    }
                    if (isParametersSupportedValue(parameters, limitedRow, value)) {
                        setParameterValue(context, parameters, limitedRow, value);
                    } else {
                        Log.w(TAG, "applyRestrictionsToParameters() not support limitedRow=" + limitedRow
                                + ", value=" + value);
                    }
                }
            }
        }
    }
    /// @}
    
    /// M: preference --> parameters @{
    //scenemode != auto, ignore MATRIX_SCENE != null && MATRIX_SCENE[row] != E0
    private static void applyPreferenceToParameters(Context context, ComboPreferences preferences, Parameters parameters,
            int[] appliedGroup, int mode) {
        for (int i = 0, len = appliedGroup.length; i < len; i++) {
            int row = appliedGroup[i];
            parameters = applyPreferenceToParameters(context, preferences, parameters, mode, row);
        }
    }

    private static Parameters applyPreferenceToParameters(Context context, ComboPreferences preferences,
            Parameters parameters, int mode, int row) {
        if (!SettingUtils.contains(SETTING_GROUP_NOT_IN_PREFERENCE, row)) {
            //check override value not shown in setting, for example: asd mode
            String preferenceValue = ((Camera)context).getSettingChecker().getOverrideSettingValue(row);
            //check override value in mode matrix
            if (preferenceValue == null) {
                preferenceValue = getMatrixValue(MATRIX_MODE_STATE, mode, row);
            }
            if (preferenceValue == null) {
                preferenceValue = getPreferenceValue(context, preferences, row);
            }
            parameters = applyValueToParameters(context, preferences, parameters, mode, row, preferenceValue);
        }
        return parameters;
    }
    
    private static String getMatrixValue(int[][] matrix, int column, int row) {
        int[] settings = matrix[row];
        int state = settings[column];
        String value = null;
        switch(state) {
        case STATE_E0:
            //value has been reset by preference, so here do nothing
            break;
        case STATE_D0:
            //use last value as disable value, so here do nothing.
            break;
        case STATE_R0:
            value = RESET_STATE_VALUE[row][0];
            break;
        case STATE_R1:
            value = RESET_STATE_VALUE[row][1];
            break;
        case STATE_R2:
            value = RESET_STATE_VALUE[row][2];
            break;
        case STATE_R3:
            value = RESET_STATE_VALUE[row][3];
            break;
        case STATE_R4:
            value = RESET_STATE_VALUE[row][4];
            break;
        case STATE_R5:
            value = RESET_STATE_VALUE[row][5];
            break;
        case STATE_R6:
            value = RESET_STATE_VALUE[row][6];
            break;
        case STATE_R7:
            value = RESET_STATE_VALUE[row][7];
            break;
        default:
            break;
        }
        if (LOG) {
            Log.v(TAG, "getMatrixValue(" + column + ", " + row + ") return " + value);
        }
        return value;
    }
    /// @}
    
    private static Parameters applyValueToParameters(Context context, ComboPreferences preferences,
            Parameters parameters, int mode, int row, String value) {
        if (isParametersSupportedValue(parameters, row, value)) {
            value = getCapabilitySupportedValue(context, preferences, parameters, mode, row, value);
            if (setParameterValue(context, parameters, row, value)) {
                ((Camera)context).applyParametersToServer();
                parameters = ((Camera)context).fetchParametersFromServer();
            }
        } else {
            Log.w(TAG, "applyValueToParameters() not support mode=" + mode + ", row=" + row + ", value=" + value);
        }
        return parameters;
    }
    
    private static String getDefaultValueFromXml(Context context, int row) {
        String value = DEFAULT_VALUE_FOR_SETTING[row];
        PreferenceGroup group = null;
        if (value == null) {
            ListPreference pref = null;
            if (context instanceof Camera) {
                pref = ((Camera)context).getListPreference(row);
            }
            if (!CameraSettings.SUPPORTED_SHOW_CONINUOUS_SHOT_NUMBER && 
                    row == ROW_SETTING_CONTINUOUS) {
                value = CameraSettings.DEFAULT_CONINUOUS_CAPTURE_NUM;
            } else {
                if (pref != null) {
                    value = pref.findSupportedDefaultValue();
                    if (value == null && pref.getEntryValues() != null && pref.getEntryValues().length > 0) {
                        value = String.valueOf(pref.getEntryValues()[0]);
                    }
                }
                
                //last defense for some preference may be removed for supported values less than 2.
                if (value == null) {
                    int resId = DEFAULT_VALUE_FOR_SETTING_ID[row];
                    if (resId != UNKNOWN) {
                        value = context.getString(resId);
                    }
                }
            }
            DEFAULT_VALUE_FOR_SETTING[row] = value;
            if (LOG) {
                Log.v(TAG, "getDefaultValueFromXml(" + row + ") " + pref);
            }
        }
        if (LOG) {
            Log.v(TAG, "getDefaultValueFromXml(" + group + ", " + row + ") return " + value);
        }
        return value;
    }
    
    public static String getPreferenceValue(Context context, ComboPreferences preferences, int row) {
        String key = KEYS_FOR_SETTING[row];
        String value = preferences.getString(key, getDefaultValueFromXml(context, row));
        if ((row == ROW_SETTING_PICTURE_RATIO) && (value == null)){
            value = CameraSettings.PICTURE_RATIO_4_3;
        }
        if (LOG) {
            Log.v(TAG, "getPreferenceValue(" + row + ") return " + value);
        }
        return value;
    }
    
    private static String getParameterValue(Parameters parameters, int row) {
        if (parameters == null) {
            Log.w(TAG, "getParameterValue(" + row + ") parameters=null!!!", new Throwable());
            return null;
        }
        String value = null;
        switch(row) {
        case ROW_SETTING_FLASH://common
            value = parameters.getFlashMode();
            break;
        case ROW_SETTING_DUAL_CAMERA://common special case
            throw new CameraSettingException("Cannot get dual camera from parameters.");
        case ROW_SETTING_EXPOSURE://common
            value = Integer.toString(parameters.getExposureCompensation());
            break;
        case ROW_SETTING_SCENCE_MODE://common
            value = parameters.getSceneMode();
            break;
        case ROW_SETTING_WHITE_BALANCE://common
            value = parameters.getWhiteBalance();
            break;
        case ROW_SETTING_IMAGE_PROPERTIES:
            throw new CameraSettingException("Cannot get image adjustment from parameters.");
        case ROW_SETTING_HUE://common
            value = parameters.getHueMode();
            break;
        case ROW_SETTING_CONTRAST://common
            parameters.getContrastMode();
            break;
        case ROW_SETTING_SHARPNESS://common
            parameters.getEdgeMode();
            break;
        case ROW_SETTING_SATURATION://common
            parameters.getSaturationMode();
            break;
        case ROW_SETTING_BRIGHTNESS://common
            parameters.getBrightnessMode();
            break;
        case ROW_SETTING_COLOR_EFFECT://common
            value = parameters.getColorEffect();
            break;
        case ROW_SETTING_GEO_TAG://common app layer
            throw new CameraSettingException("Cannot get geo tag from parameters.");
        //case ROW_SETTING_AE_METER:
        //    value = parameters.getExposureMeter();
        //    break;
        case ROW_SETTING_ANTI_FLICKER://common
            value = parameters.getAntibanding();
            break;
        case ROW_SETTING_SELF_TIMER://camera app layer
            throw new CameraSettingException("Cannot get self timer from parameters.");
        case ROW_SETTING_ZSD://camera
            value = parameters.getZSDMode();
            break;
        case ROW_SETTING_CONTINUOUS://camera
            throw new CameraSettingException("Cannot get continuous number from parameters.");
        case ROW_SETTING_PICTURE_SIZE://camera special case
            Size size = parameters.getPictureSize();
            value = "" + size.width + 'x' + size.height;
            break;
        case ROW_SETTING_ISO://camera
            value = parameters.getISOSpeed();
            break;
        case ROW_SETTING_FACEBEAUTY_PROPERTIES://camera
            throw new CameraSettingException("Cannot get facebeauty adjustment from parameters.");
        case ROW_SETTING_FACEBEAUTY_SMOOTH://camera
            value = parameters.get(ParametersHelper.KEY_FACEBEAUTY_SMOOTH);
            break;
        case ROW_SETTING_FACEBEAUTY_SKIN_COLOR://camera
            value = parameters.get(ParametersHelper.KEY_FACEBEAUTY_SKIN_COLOR);
            break;
        case ROW_SETTING_FACEBEAUTY_SHARP://camera
            value = parameters.get(ParametersHelper.KEY_FACEBEAUTY_SHARP);
            break;
        case ROW_SETTING_VIDEO_STABLE://video
            value = Boolean.toString(parameters.getVideoStabilization());
            break;
        case ROW_SETTING_MICROPHONE://video for media recorder
            throw new CameraSettingException("Cannot get microphone from parameters.");
        case ROW_SETTING_AUDIO_MODE://video for media recorder
            throw new CameraSettingException("Cannot get audio mode from parameters.");
        case ROW_SETTING_TIME_LAPSE://video
            throw new CameraSettingException("Cannot get time lapse from parameters.");
        case ROW_SETTING_LIVE_EFFECT://video for effect recorder 
            throw new CameraSettingException("Cannot get live effect from parameters.");
        case ROW_SETTING_VIDEO_QUALITY://video
            //Here doesn't throw exception for that:
            //set parameter will deal with quality.
            break;
        case ROW_SETTING_RECORDING_HINT://plus for recording hint
            value = parameters.get(ParametersHelper.KEY_RECORDING_HINT);
            break;
        case ROW_SETTING_PICTURE_RATIO:
            throw new CameraSettingException("Cannot get picture ratio from parameters.");
        case ROW_SETTING_VOICE:
            throw new CameraSettingException("Cannot get voice from parameters.");
        case ROW_SETTING_CAMERA_FACE_DETECT:
            throw new CameraSettingException("Cannot get face detection from parameters.");
        default:
            break;
        }
        if (LOG) {
            Log.v(TAG, "getParameterValue(" + row + ") return " + value);
        }
        return value;
    }
    
    private static boolean setParameterValue(Context context, Parameters parameters, int row, String value) {
        if (parameters == null) {
            Log.w(TAG, "setParameterValue(" + row + ", " + value + ") parameters=null!!!", new Throwable());
            return false;
        }
        boolean needreload = false;
        String key = KEYS_FOR_SETTING[row];
        switch(row) {
        case ROW_SETTING_FLASH://common
            parameters.setFlashMode(value);
            break;
        case ROW_SETTING_DUAL_CAMERA://common special case
            throw new CameraSettingException("Cannot set dual camera to parameters.");
        case ROW_SETTING_EXPOSURE://common
            int exposure = Integer.parseInt(value);
            parameters.setExposureCompensation(exposure);
            break;
        case ROW_SETTING_SCENCE_MODE://common
            if (!parameters.getSceneMode().equals(value)) {
                parameters.setSceneMode(value);
                // Setting scene mode will change the settings of flash mode,
                // white balance, and focus mode. Here we read back the
                // parameters, so we can know those settings.
                needreload = true;
            }
            break;
        case ROW_SETTING_WHITE_BALANCE://common
            parameters.setWhiteBalance(value);
            break;
        case ROW_SETTING_IMAGE_PROPERTIES:
            throw new CameraSettingException("Cannot set total image adjustement, " +
                    "Please use Hue, Contrast, Edge, staturation and Brightness.");
        case ROW_SETTING_HUE://common
            parameters.setHueMode(value);
            break;
        case ROW_SETTING_CONTRAST://common
            parameters.setContrastMode(value);
            break;
        case ROW_SETTING_SHARPNESS://common
            parameters.setEdgeMode(value);
            break;
        case ROW_SETTING_SATURATION://common
            parameters.setSaturationMode(value);
            break;
        case ROW_SETTING_BRIGHTNESS://common
            parameters.setBrightnessMode(value);
            break;
        case ROW_SETTING_COLOR_EFFECT://common
            parameters.setColorEffect(value);
            break;
        case ROW_SETTING_GEO_TAG://common app layer
            throw new CameraSettingException("Cannot set geo tag to parameters.");
        //case ROW_SETTING_AE_METER:
        //    parameters.setExposureMeter(value);
        //    break;
        case ROW_SETTING_ANTI_FLICKER://common
            parameters.setAntibanding(value);
            break;
        case ROW_SETTING_SELF_TIMER://camera app layer
            throw new CameraSettingException("Cannot set self timer to parameters.");
        case ROW_SETTING_ZSD://camera
            parameters.setZSDMode(value);
            break;
        case ROW_SETTING_CONTINUOUS://camera
            throw new CameraSettingException("Please use ROW_SETTING_CONTINUOUS_NUM to set shot number.");
        case ROW_SETTING_PICTURE_SIZE://camera special case
            setPicturePreview(context, parameters, value);
            break;
        case ROW_SETTING_ISO://camera
            parameters.setISOSpeed(value);
            break;
        case ROW_SETTING_FACEBEAUTY_PROPERTIES://camera
            throw new CameraSettingException("Cannot set total facebeauty adjustement, " +
                    "Please use Smooth, SkinColor and Sharp.");
        case ROW_SETTING_FACEBEAUTY_SMOOTH://camera
            parameters.set(ParametersHelper.KEY_FACEBEAUTY_SMOOTH, value);
            break;
        case ROW_SETTING_FACEBEAUTY_SKIN_COLOR://camera
            parameters.set(ParametersHelper.KEY_FACEBEAUTY_SKIN_COLOR, value);
            break;
        case ROW_SETTING_FACEBEAUTY_SHARP://camera
            parameters.set(ParametersHelper.KEY_FACEBEAUTY_SHARP, value);
            break;
        case ROW_SETTING_VIDEO_STABLE://video
            boolean toggle = "on".equals(value) ? true : false;
            parameters.setVideoStabilization(toggle);
            break;
        case ROW_SETTING_MICROPHONE://video for media recorder
            throw new CameraSettingException("Cannot set microphone to parameters.");
        case ROW_SETTING_AUDIO_MODE://video for media recorder
            throw new CameraSettingException("Cannot set audio mode to parameters.");
        case ROW_SETTING_TIME_LAPSE://video should be rechecked
            throw new CameraSettingException("Cannot set timelapse to parameters.");
        case ROW_SETTING_LIVE_EFFECT://video for effect recorder
            throw new CameraSettingException("Cannot set live effect to parameters.");
        case ROW_SETTING_VIDEO_QUALITY://video
            setVideoPreview(context, parameters, value);
            break;
        case ROW_SETTING_RECORDING_HINT://plus for recroding hint
            parameters.setRecordingHint(Boolean.parseBoolean(value));
            break;
        case ROW_SETTING_CAPTURE_MODE:
            parameters.setCaptureMode(value);
            break;
        case ROW_SETTING_CONTINUOUS_NUM:
            int number = Integer.parseInt(value);
            parameters.setBurstShotNum(number);
            break;
        case ROW_SETTING_JPEG_QUALITY:
            int jpegQuality = getJpegQuality(context, Integer.parseInt(value));
            parameters.setJpegQuality(jpegQuality);
            break;
        case ROW_SETTING_CAMERA_MODE:
            parameters.setCameraMode(Integer.parseInt(value));
            break;
        case ROW_SETTING_PICTURE_RATIO:
            throw new CameraSettingException("Cannot set picture ratiot to parameters.");
        case ROW_SETTING_VOICE:
            throw new CameraSettingException("Cannot set voice to parameters.");
        case ROW_SETTING_CAMERA_FACE_DETECT:
            throw new CameraSettingException("Cannot set face detection to parameters.");
        default:
            break;
        }
        if (LOG) {
            Log.v(TAG, "setParameterValue(" + row + ", " + value + ") return " + needreload);
        }
        return needreload;
    }

    private static int getJpegQuality(Context context, int quality) {
        int cameraId = ((Camera)context).getCameraId();
        int jpegQuality = CameraProfile.getJpegEncodingQualityParameter(
                cameraId, quality);
        return jpegQuality;
    }

    private static void setPicturePreview(Context context, Parameters parameters, String value) {
        if (LOG) {
            Log.v(TAG, "setPicturePreview(" + value + ")");
        }
        Camera camera = (Camera)context;
        if (value == null) {
            CameraSettings.initialCameraPictureSize(context, parameters);
        } else {
            List<Size> supported = parameters.getSupportedPictureSizes();
            String targetRatio = getPreferenceValue(context,
                    camera.getPreferences(), ROW_SETTING_PICTURE_RATIO);
            CameraSettings.setCameraPictureSize(value, supported, parameters, targetRatio, context);
        }
        
        double previewRatio = Double.parseDouble(CameraSettings.PICTURE_RATIO_4_3);
        if (!getPreferenceValue(context, camera.getPreferences(), ROW_SETTING_PICTURE_RATIO)
                .equals(CameraSettings.PICTURE_RATIO_4_3)) {
            DisplayMetrics metrics = context.getResources().getDisplayMetrics();
            if (metrics.widthPixels > metrics.heightPixels) {
                previewRatio = (double) metrics.widthPixels / metrics.heightPixels;
            } else {
                previewRatio = (double) metrics.heightPixels / metrics.widthPixels;
            }
        }
        
        // Set a preview size that is closest to the viewfinder height and has
        // the right aspect ratio.
        List<Size> sizes = parameters.getSupportedPreviewSizes();
        Size optimalSize = Util.getOptimalPreviewSize(camera, sizes,
                previewRatio, false, true);
        Size original = parameters.getPreviewSize();
        if (!original.equals(optimalSize)) {
            parameters.setPreviewSize(optimalSize.width, optimalSize.height);

            // Zoom related settings will be changed for different preview
            // sizes, so set and read the parameters to get latest values
            /* M: we use zoom manager to get zoom rate after apply parameters to server,
             * so here don't reload parameters.
             */
        }
        
        // Reset preview frame rate to the maximum because it may be lowered by
        // video camera application.
        List<Integer> frameRates = parameters.getSupportedPreviewFrameRates();
        if (frameRates != null) {
            Integer max = Collections.max(frameRates);
            parameters.setPreviewFrameRate(max);
        }
    }
    
    private static void setVideoPreview(Context context, Parameters parameters, String value) {
        Camera camera = (Camera)context;
        int cameraId = camera.getCameraId();
        ComboPreferences mPreferences = camera.getPreferences();
        Intent intent = camera.getIntent();
        
        // The preference stores values from ListPreference and is thus string type for all values.
        // We need to convert it to int manually.
        String defaultQuality = CameraSettings.getDefaultVideoQuality(cameraId,
                context.getResources().getString(R.string.pref_video_quality_default));
        String videoQuality = mPreferences.getString(CameraSettings.KEY_VIDEO_QUALITY, defaultQuality);
        int quality = Integer.valueOf(videoQuality);

        // Set video quality according client app's requirement.
        boolean userLimitQuality = intent.hasExtra(MediaStore.EXTRA_VIDEO_QUALITY);
        if (userLimitQuality) {
            int extraVideoQuality = intent.getIntExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0);
            if (extraVideoQuality > 0) {
                if (CamcorderProfile.hasProfile(cameraId, extraVideoQuality)) {
                    quality = extraVideoQuality;
                } else {
                    if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_MTK_MEDIUM)) {
                        quality = CamcorderProfile.QUALITY_MTK_MEDIUM;
                    } else {
                        quality = CamcorderProfile.QUALITY_MTK_HIGH;
                    }
                }
            } else { // 0 is mms.
                quality = CamcorderProfile.QUALITY_LOW;
            }
        }

        // effect type will affect video quality
        if (camera.effectsActive()) {
            // Set quality to HIGH for effects, unless intent is overriding it
            if (!userLimitQuality) {
                if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_MTK_LIVE_EFFECT)) {
                    quality = CamcorderProfile.QUALITY_MTK_LIVE_EFFECT;
                } else {
                    if (cameraId == 0) {
                        quality = CamcorderProfile.QUALITY_MTK_MEDIUM;
                    } else {
                        quality = CamcorderProfile.QUALITY_MTK_HIGH;
                    }
                }
            }
        }
        // Read time lapse recording interval.
        String frameIntervalStr = getPreferenceValue(context, mPreferences, ROW_SETTING_TIME_LAPSE);
        CamcorderProfile profile = camera.fetchProfile(quality, Integer.parseInt(frameIntervalStr));

        Point previewSize = computeDesiredPreviewSize(camera, profile, parameters);
        Point originalPreviewSize = new Point(parameters.getPreviewSize().width, parameters.getPreviewSize().height);
        if (!originalPreviewSize.equals(previewSize)) {
            parameters.setPreviewSize(previewSize.x, previewSize.y);
            
            // Zoom related settings will be changed for different preview
            // sizes, so set and read the parameters to get latest values
            /* M: we use zoom manager to get zoom rate after apply parameters to server,
             * so here don't reload parameters.
             */
        }
        
        reviseVideoCapability(context, mPreferences, parameters, profile);
        parameters.setPreviewFrameRate(profile.videoFrameRate);
       
        // Set picture size.
        // The logic here is different from the logic in still-mode camera.
        // There we determine the preview size based on the picture size, but
        // here we determine the picture size based on the preview size.
        if (parameters.isVideoSnapshotSupported()) {
               List<Size> supported = parameters.getSupportedPictureSizes();
               Size optimalSize = Util.getOptimalVideoSnapshotPictureSize(supported,
                   (double) previewSize.x / previewSize.y);
               Size original = parameters.getPictureSize();
               if (!original.equals(optimalSize)) {
                   parameters.setPictureSize(optimalSize.width, optimalSize.height);
               }
        } else {
            parameters.setPictureSize(previewSize.x, previewSize.y);
        }
    }

    private static Point computeDesiredPreviewSize(Camera context, CamcorderProfile profile, Parameters parameters) {
        int previewWidth = -1;
        int previewHeight = -1;
        if (parameters.getSupportedVideoSizes() == null || context.effectsActive()) { //should be rechecked usedefault
            previewWidth = profile.videoFrameWidth;
            previewHeight = profile.videoFrameHeight;
            
        } else {  // Driver supports separates outputs for preview and video.
            List<Size> sizes = parameters.getSupportedPreviewSizes();
            Size preferred = parameters.getPreferredPreviewSizeForVideo();
            int product = preferred.width * preferred.height;
            Iterator<Size> it = sizes.iterator();
            // Remove the preview sizes that are not preferred.
            while (it.hasNext()) {
                Size size = it.next();
                if (size.width * size.height > product) {
                    it.remove();
                }
            }
            Size optimalSize = Util.getOptimalPreviewSize(context, sizes,
                    (double) profile.videoFrameWidth / profile.videoFrameHeight,
                    !context.isVideoWallPaperIntent(), false); //don't change target ratio
            if (optimalSize != null) {
                previewWidth = optimalSize.width;
                previewHeight = optimalSize.height;            
            } else {
                previewWidth = profile.videoFrameWidth;
                previewHeight = profile.videoFrameHeight;
            }
        }
        Point desired = new Point(previewWidth, previewHeight);
        if (LOG) {
            Log.v(TAG, "computeDesiredPreviewSize(" + context.effectsActive() + ") return " + desired);
        }
        return desired;
    }
    
    private static void reviseVideoCapability(Context context, ComboPreferences preferences,
            Parameters parameters, CamcorderProfile profile) {
        if (LOG) {
            Log.v(TAG, "reviseVideoCapability() begin profile.videoFrameRate=" + profile.videoFrameRate);
        }
        //Mediatek modify to meet lower frame sensor
        List<Integer> supportedFrameRates = parameters.getSupportedPreviewFrameRates();
        if (!isSupported(profile.videoFrameRate, supportedFrameRates)) {
            int maxFrame = getMaxSupportedPreviewFrameRate(supportedFrameRates);
            profile.videoBitRate = (profile.videoBitRate * maxFrame) / profile.videoFrameRate;
            profile.videoFrameRate = maxFrame;
        }
        String sceneMode = getPreferenceValue(context, preferences, ROW_SETTING_SCENCE_MODE);
        if (isParametersSupportedValue(parameters, ROW_SETTING_SCENCE_MODE, sceneMode)) {
            if (Parameters.SCENE_MODE_NIGHT.equals(sceneMode)) {
                profile.videoFrameRate /= 2;
                profile.videoBitRate /= 2;
            }
        }
        if (LOG) {
            Log.v(TAG, "reviseVideoCapability() end profile.videoFrameRate=" + profile.videoFrameRate);
        }
    }
    
    private static int getMaxSupportedPreviewFrameRate(List<Integer> supportedPreviewRate) {
        int maxFrameRate = 0;
        for (int rate : supportedPreviewRate) {
            if (rate > maxFrameRate) {
                maxFrameRate = rate;
            }
        }
        if (LOG) {
            Log.v(TAG, "getMaxSupportedPreviewFrameRate() return " + maxFrameRate);
        }
        return maxFrameRate;
    }
    
    public static boolean isSupported(Object value, List<?> supported) {
        return supported == null ? false : supported.indexOf(value) >= 0;
    }
    
    private static String getCapabilitySupportedValue(Context context, ComboPreferences preferences,
            Parameters parameters, int mode, int row, String value) {
        String support = value;
        for (int i = 0, len = CAPABILITIES.length; i < len; i++) {
            Restriction cur = CAPABILITIES[i];
            if (cur.getType() == Restriction.TYPE_MODE) { //mode type
                int limitedMode = cur.getIndex();
                if ((limitedMode == UNKNOWN || limitedMode == mode) && cur.getRestrictioins() != null) {
                    for (Restriction restriction : cur.getRestrictioins()) {
                        //and current row's restriction and limit parameter to defined value
                        if (restriction.getIndex() == row) {
                            support = restriction.findSupported(value);
                            break;
                        }
                    }
                }
                if (LOG) {
                    Log.v(TAG, "getCapabilitySupportedValue() limitedMode=" + limitedMode + ", mode=" + mode
                            + ", support=" + support);
                }
            } else {
                int curRow = cur.getIndex();
                String curValue = null;
                if (ROW_SETTING_RECORDING_HINT == curRow) {
                    curValue = Boolean.toString(isVideoMode(mode));
                } else if (SettingUtils.contains(SETTING_GROUP_CAMERA_FOR_PARAMETERS, curRow)) {
                    curValue = getParameterValue(parameters, curRow);
                } else if (!SettingUtils.contains(SETTING_GROUP_NOT_IN_PREFERENCE, curRow)) {
                    curValue = getPreferenceValue(context, preferences, curRow);
                }
                List<String> limitedValues = cur.getValues();
                if (limitedValues != null && limitedValues.contains(curValue)) {
                    for (Restriction restriction : cur.getRestrictioins()) {
                        //and current row's restriction and limit parameter to defined value
                        if (restriction.getIndex() == row) {
                            support = restriction.findSupported(value);
                            break;
                        }
                    }
                }
                if (LOG) {
                    Log.v(TAG, "getCapabilitySupportedValue() curRow=" + curRow + ", curValue=" + curValue
                            + ", support=" + support);
                }
            }
        }
        if (LOG) {
            Log.v(TAG, "getCapabilitySupportedValue(" + mode + ", " + row + ", " + value + ") return " + support);
        }
        return support;
    }
    
    private static boolean isParametersSupportedValue(Parameters parameters, int row, String value) {
        if (parameters == null) {
            Log.w(TAG, "isParametersSupportedValue(" + row + ", " + value + ") parameters=null!!!", new Throwable());
            return false;
        }
        boolean support = false;
        List<String> supportedList = null;
        List<Size> supportedListSize = null;
        switch(row) {
            case ROW_SETTING_FLASH://common
                supportedList = parameters.getSupportedFlashModes();
                break;
            case ROW_SETTING_DUAL_CAMERA://common
                throw new CameraSettingException("Cannot get dual camera capability.");
            case ROW_SETTING_EXPOSURE://common
                int max = parameters.getMaxExposureCompensation();
                int min = parameters.getMinExposureCompensation();
                int exposure = Integer.parseInt(value);
                if (exposure >= min && exposure <= max) {
                    support = true;
                }
                break;
            case ROW_SETTING_SCENCE_MODE://common
                supportedList = parameters.getSupportedSceneModes();
                break;
            case ROW_SETTING_WHITE_BALANCE://common
                supportedList = parameters.getSupportedWhiteBalance();
                break;
            case ROW_SETTING_IMAGE_PROPERTIES:
                throw new CameraSettingException("Cannot get image adjustment capability.");
            case ROW_SETTING_HUE://common
                supportedList = parameters.getSupportedHueMode();
                break;
            case ROW_SETTING_CONTRAST://common
                supportedList = parameters.getSupportedContrastMode();
                break;
            case ROW_SETTING_SHARPNESS://common
                supportedList = parameters.getSupportedEdgeMode();
                break;
            case ROW_SETTING_SATURATION://common
                supportedList = parameters.getSupportedSaturationMode();
                break;
            case ROW_SETTING_BRIGHTNESS://common
                supportedList = parameters.getSupportedBrightnessMode();
                break;
            case ROW_SETTING_COLOR_EFFECT://common
                supportedList = parameters.getSupportedColorEffects();
                break;
            case ROW_SETTING_GEO_TAG://common
                throw new CameraSettingException("Cannot get geo tag capability.");
            //case ROW_SETTING_AE_METER:
            //    supportedList = parameters.getSupportedExposureMeter();
            //    break;
            case ROW_SETTING_ANTI_FLICKER://common
                supportedList = parameters.getSupportedAntibanding();
                break;
            case ROW_SETTING_SELF_TIMER://camera
                throw new CameraSettingException("Cannot get self timer capability.");
            case ROW_SETTING_ZSD://camera
                supportedList = parameters.getSupportedZSDMode();
                break;
            case ROW_SETTING_CONTINUOUS://camera
                support = true;
                break;
            case ROW_SETTING_PICTURE_SIZE://camera special case
                //mParameters.getSupportedPictureSizes();
                support = true;//we always return true for we should set picture size.
                break;
            case ROW_SETTING_ISO://camera
                supportedList = parameters.getSupportedISOSpeed();
                break;
            case ROW_SETTING_FACEBEAUTY_PROPERTIES://camera
                throw new CameraSettingException("Cannot get facebeauty adjustment capability.");
            case ROW_SETTING_FACEBEAUTY_SMOOTH:
                int maxSmooth = ParametersHelper.getMaxLevel(parameters, ParametersHelper.FACEBEAUTY_SMOOTH);
                int minSmooth = ParametersHelper.getMinLevel(parameters, ParametersHelper.FACEBEAUTY_SMOOTH);
                int smooth = Integer.parseInt(value);
                if (smooth >= minSmooth && smooth <= maxSmooth) {
                    support = true;
                }
                break;
            case ROW_SETTING_FACEBEAUTY_SKIN_COLOR:
                int maxSkinColor = ParametersHelper.getMaxLevel(parameters, ParametersHelper.FACEBEAUTY_SKIN_COLOR);
                int minSkinColor = ParametersHelper.getMinLevel(parameters, ParametersHelper.FACEBEAUTY_SKIN_COLOR);
                int skinColor = Integer.parseInt(value);
                if (skinColor >= minSkinColor && skinColor <= maxSkinColor) {
                    support = true;
                }
                break;
            case ROW_SETTING_FACEBEAUTY_SHARP:
                int maxSharp = ParametersHelper.getMaxLevel(parameters, ParametersHelper.FACEBEAUTY_SHARP);
                int minSharp = ParametersHelper.getMinLevel(parameters, ParametersHelper.FACEBEAUTY_SHARP);
                int sharp = Integer.parseInt(value);
                if (sharp >= minSharp && sharp <= maxSharp) {
                    support = true;
                }
                break;
            case ROW_SETTING_VIDEO_STABLE://video
                support = parameters.isVideoStabilizationSupported();
                break;
            case ROW_SETTING_MICROPHONE://video recorder capability
                throw new CameraSettingException("Cannot get microphone capability.");
            case ROW_SETTING_AUDIO_MODE://video recorder capability
                throw new CameraSettingException("Cannot get audio mode capability.");
            case ROW_SETTING_TIME_LAPSE://video
                throw new CameraSettingException("Cannot time lapse capability.");
            case ROW_SETTING_LIVE_EFFECT://video effect recorder capability
                throw new CameraSettingException("Cannot get live effect capability.");
            case ROW_SETTING_VIDEO_QUALITY://video special case
                support = true;//here we always return true for that we should set it.
                break;
            case ROW_SETTING_RECORDING_HINT://plus for recording hint
            case ROW_SETTING_CAPTURE_MODE:
            case ROW_SETTING_CONTINUOUS_NUM:
            case ROW_SETTING_JPEG_QUALITY:
            case ROW_SETTING_CAMERA_MODE:
                support = true;
                break;
            case ROW_SETTING_PICTURE_RATIO:
                support = true;
                break;
            case ROW_SETTING_VOICE:
                throw new CameraSettingException("Cannot get voice capability.");
            case ROW_SETTING_CAMERA_FACE_DETECT:
                throw new CameraSettingException("Cannot get fd capability.");
            default:
                break;
        }
        support = supportedList == null ? support : supportedList.indexOf(value) >= 0;
        if (LOG) {
            Log.v(TAG, "isParametersSupportedValue(" + row + ", " + value + ") supportedList=" + supportedList
                    + " return " + support);
        }
        return support;
    }
    /// @}
    
    public void setOverrideValues(int row, String value) {
        if (LOG) {
            Log.v(TAG, "setOverrideValues(" + row + ", " + value + ")");
        }
        if (row >= 0 && row <= mOverrideSettingValues.length) {
            mOverrideSettingValues[row] = value;
        }
    }
    
    public void clearOverrideValues() {
        for (int i = 0, len = mOverrideSettingValues.length; i < len; i++) {
            mOverrideSettingValues[i] = null;
        }
    }
    
    public String getOverrideValues(int row) {
        String value = null;
        if (row >= 0 && row <= mOverrideSettingValues.length) {
            value = mOverrideSettingValues[row];
        }
        if (LOG) {
            Log.v(TAG, "getOverrideValues(" + row + ") return " + value);
        }
        return value;
    }
    
    private Camera mContext;
    private ListPreference[] mListPrefs = new ListPreference[SETTING_ROW_COUNT];
        
    public SettingChecker(Camera context) {
        mContext = context;
    }
    
    public synchronized void setListPreference(int row, ListPreference pref) {
        if (LOG) {
            Log.v(TAG, "setListPreference(" + row + ", " + pref + ")");
        }
        mListPrefs[row] = pref;
    }

    public synchronized ListPreference getListPreference(int row) {
        return mListPrefs[row];
    }
    
    public synchronized void clearListPreference() {
        if (LOG) {
            Log.v(TAG, "clearListPreference()");
        }
        for (int i = 0, len = mListPrefs.length; i < len; i++) {
            mListPrefs[i] = null;
        }
    }
    
    //apply focus capability and mode 
    public void applyFocusCapabilities(boolean setArea) {
        FocusManager focusManager = mContext.getFocusManager();
        if (focusManager.getAeLockSupported()) {
            mContext.getParameters().setAutoExposureLock(focusManager.getAeLock());
        }
        if (focusManager.getAwbLockSupported()) {
            mContext.getParameters().setAutoWhiteBalanceLock(focusManager.getAwbLock());
        }
        if (focusManager.getFocusAreaSupported() && setArea) {
            mContext.getParameters().setFocusAreas(focusManager.getFocusAreas());
        }
        if (focusManager.getMeteringAreaSupported() && setArea) {
            // Use the same area for focus and metering.
            mContext.getParameters().setMeteringAreas(focusManager.getMeteringAreas());
        }

        mContext.getCameraActor().handleFocus();
        mContext.getParameters().setFocusMode(focusManager.getFocusMode());
        //Here set continuous callback
        mContext.applyContinousCallback();
    }
    
    public void applyPreferenceToParameters() {
        ComboPreferences preferences = mContext.getPreferences();
        int mode = mContext.getCurrentMode();
        int[] appliedGroup = getModeSettingGroupForParameters(mode);
        //we should save all preference values to mParameters
        applyPreferenceToParameters(mContext, preferences, mContext.getParameters(), appliedGroup, getVirtualMode(mode));
        //apply mode table
        applyModeTableToParameters(mContext, preferences, mContext.getParameters(), appliedGroup, getVirtualMode(mode));
        //apply scene mode table
        applySceneTableToParameters(mContext, preferences, mContext.getParameters(), appliedGroup);
        //apply restrictions
        applyRestrictionsToParameters(mContext, mContext.getParameters(), appliedGroup);
        //apply other limitation
        applyLimitToParameters();
        //for capability parameters
        applyFocusCapabilities(false);//should be checked
    }
    
    private int getVirtualMode(int mode) {
        int virtual = mode;
        if (mode == ModePicker.MODE_VIDEO && mContext.effectsActive()) {
            virtual = MODE_VIDEO_LIVE_EFFECT;
        }
        if (LOG) {
            Log.v(TAG, "getVirtualMode(" + mode + ") return " + virtual);
        }
        return virtual;
    }
    
    //Some preference is on screen, we should update immediately before applyParameterToUI().
    public void applyParametersToUIImmediately() {
        if (LOG) {
            Log.v(TAG, "applyParametersToUIImmediately()");
        }
        int[] appliedGroup = SETTING_GROUP_ALL_IN_SCREEN;
        ComboPreferences preferences = mContext.getPreferences();
        int mode = mContext.getCurrentMode();
        clearOverrideSettings(appliedGroup);
        String[] keyvalues = getModeTableKeyValues(getVirtualMode(mode), appliedGroup);
        overrideSettings(keyvalues);
        keyvalues = getSceneTableKeyValues(mContext.getParameters(), appliedGroup);
        overrideSettings(keyvalues);
        keyvalues = getRestrictionsKeyValues(mContext, preferences, mContext.getParameters(), appliedGroup);
        overrideSettings(keyvalues);
        keyvalues = getCapabilityKeyValues(mContext, preferences, mContext.getParameters(), appliedGroup, mode);
        overrideSettings(keyvalues);
        mContext.runOnUiThread(new Runnable() {
            public void run() {
                //Force update picker button state.
                if (mContext.getPickerManager() != null) {
                    mContext.getPickerManager().onCameraParameterReady();
                    mContext.getPickerManager().refresh();
                }
            };
        });
    }
    
    public void applyParametersToUI() {
        if (LOG) {
            Log.v(TAG, "applyParametersToUI()");
        }
        ComboPreferences preferences = mContext.getPreferences();
        int mode = mContext.getCurrentMode();
        int[] appliedGroup = SETTING_GROUP_ALL_IN_SETTING;
        clearOverrideSettings(appliedGroup);
        String[] keyvalues = getModeTableKeyValues(getVirtualMode(mode), appliedGroup);
        overrideSettings(keyvalues);
        keyvalues = getSceneTableKeyValues(mContext.getParameters(), appliedGroup);
        overrideSettings(keyvalues);
        keyvalues = getRestrictionsKeyValues(mContext, preferences, mContext.getParameters(), appliedGroup);
        overrideSettings(keyvalues);
        keyvalues = getCapabilityKeyValues(mContext, preferences, mContext.getParameters(), appliedGroup, mode);
        overrideSettings(keyvalues);
        //may be we should check key-values according current parameters
    }
    
    private void clearOverrideSettings(int[] appliedGroup) {
        PreferenceGroup group = mContext.getPreferenceGroup();
        for (int i = 0, size = appliedGroup.length; i < size; i++) {
            ListPreference pref = getListPreference(appliedGroup[i]);
            if (pref != null) {
                pref.setOverrideValue(null);
            }
        }
    }
    
    // Scene mode can override other camera settings (ex: flash mode).
    private void overrideSettings(final String ... keyvalues) {
        if (keyvalues == null || keyvalues.length < 2) {
            return;
        }
        for (int i = 0; i < keyvalues.length; i += 2) {
            String key = keyvalues[i];
            String value = keyvalues[i + 1];
            ListPreference pref = mContext.getListPreference(key);
            if (pref != null) { // && key.equals(pref.getKey())
                pref.setOverrideValue(value);
                if (LOG) {
                    Log.v(TAG, "overrideSettings() key=" + key + ", value=" + value);
                }
            }
        }
    }
    
    public void resetSettings() {
        long start = System.currentTimeMillis();
        int curId = mContext.getCameraId();
        ComboPreferences preferences = mContext.getPreferences();
        if (preferences != null) {
            resetSettings(preferences); //reset current
            List<Integer> cameraIds = new ArrayList<Integer>();
            int count = mContext.getCameraCount();
            for (int i = 0; i < count; i++) {
                cameraIds.add(i);
            }
            if (cameraIds.size() > 0) {
                cameraIds.remove(curId); //remove current and reset others
            }
            count = cameraIds.size();
            for (int i = 0; i < count; i++) {
                int cameraId = cameraIds.get(i);
                preferences.setLocalId(mContext, cameraId);
                resetSettings(preferences); //reset current
                if (LOG) {
                    Log.v(TAG, "resetSettings() reset cameraId=" + cameraId);
                }
            }
        }
        long stop = System.currentTimeMillis();
        if (LOG) {
            Log.v(TAG, "resetSettings() consume:" + (stop - start));
        }
    }
    
    private void resetSettings(ComboPreferences preferences) {
        Editor editor = preferences.edit();
        for (int i = 0, len = RESET_SETTING_ITEMS.length; i < len; i++) {
            int row = RESET_SETTING_ITEMS[i];
            String key = KEYS_FOR_SETTING[row];
            editor.remove(key);//remove it will use default.
            if (LOG) {
                Log.v(TAG, "resetSettings() remove key[" + row + "]");
            }
        }
        editor.apply();
    }
    
    public void enableContinuousShot() {
        //Don't apply ROW_SETTING_CONTINUOUS immediately for that:
        //continuous shot is not immediate action, it just work after long press shutter.
        //Here apply continuous preference value to continuous_num.
        applyValueToParameters(ROW_SETTING_CONTINUOUS_NUM, getPreferenceValue(ROW_SETTING_CONTINUOUS));
        applyValueToParameters(ROW_SETTING_CAPTURE_MODE, Parameters.CAPTURE_MODE_CONTINUOUS_SHOT);
    }
    
    public void disableContinuousShot() {
        //reset continuous value to 1
        applyValueToParameters(ROW_SETTING_CONTINUOUS_NUM, RESET_STATE_VALUE[ROW_SETTING_CONTINUOUS_NUM][0]);
        applyValueToParameters(ROW_SETTING_CAPTURE_MODE, Parameters.CAPTURE_MODE_NORMAL);
    }
    
    public void turnOffWhenHide() {
        Parameters parameters = mContext.getParameters();
        if (LOG) { //may open camera slow, so parameters may be null.
            Log.v(TAG, "turnOffWhenHide() mContext.getParameters()=" + parameters);
        }
        setOverrideValues(ROW_SETTING_FLASH, Parameters.FLASH_MODE_OFF);
        if (parameters != null) {
            // when slip from Camera to Gallery, stop continous auto focus.
            FocusManager focusManager = mContext.getFocusManager();
            if (focusManager != null) {
                focusManager.overrideFocusMode(Parameters.FOCUS_MODE_INFINITY);
                parameters.setFocusMode(focusManager.getFocusMode());
            }
            setParameterValue(mContext, parameters, ROW_SETTING_FLASH, Parameters.FLASH_MODE_OFF);
            mContext.applyParametersToServer();
        }
    }
    
    public void applyLimitToParameters() {
        if (LOG) { //may open camera slow, so parameters may be null.
            Log.v(TAG, "applyLimitToParameters() mContext.getParameters()=" + mContext.getParameters());
        }
        // should do when capture intent
        if (mContext.isImageCaptureIntent()) {
            setParameterValue(mContext, mContext.getParameters(), ROW_SETTING_ZSD, ParametersHelper.ZSD_MODE_OFF);
        }
    }
    
    public void turnOnWhenShown() {
        Parameters parameters = mContext.getParameters();
        ComboPreferences preference = mContext.getPreferences();
        if (LOG) { //may open camera slow, so parameters may be null.
            Log.v(TAG, "turnOnWhenShown() mContext.getParameters()=" + parameters
                    + ", mContext.getPreferences()=" + preference);
        }
        setOverrideValues(ROW_SETTING_FLASH, null);
        if (parameters != null && preference != null) {
            // when slip back to Camera from Gallery, start continous auto focus.
            FocusManager focusManager = mContext.getFocusManager();
            if (focusManager != null) {
                focusManager.overrideFocusMode(null);
                parameters.setFocusMode(focusManager.getFocusMode());
            }
            applyPreferenceToParameters(mContext, preference, parameters,
                    getVirtualMode(mContext.getCurrentMode()), ROW_SETTING_FLASH);
            mContext.applyParametersToServer();
            applyParametersToUIImmediately();
        }
    }
    
    public boolean isParametersSupportedValue(int row, String value) {
        return isParametersSupportedValue(mContext.getParameters(), row, value);
    }
    
    public String getParameterValue(int row) {
        return getParameterValue(mContext.getParameters(), row);
    }
    
    public String getPreferenceValue(int row) {
        String value = null;
        //Voice is special for it will not keep any value in camera.
        if (row == SettingChecker.ROW_SETTING_VOICE) {
            value = mContext.getVoiceManager().getVoiceValue();
        } else {
            value = getPreferenceValue(mContext, mContext.getPreferences(), row);
        }
        if (LOG) {
            Log.v(TAG, "getPreferenceValue(" + row + ") return " + value);
        }
        return value;
    }
    
    public String getSettingCurrentValue(int row) {
        String value = null;
        //Voice is special for it will not keep any value in camera.
        if (row == SettingChecker.ROW_SETTING_VOICE) {
            value = getMatrixValue(MATRIX_MODE_STATE, mContext.getCurrentMode(), row);
            if (value == null) {
                value = mContext.getVoiceManager().getVoiceValue();
            }
        } else {
            ListPreference pref = getListPreference(row);
            if (pref == null) {
                value = getDefaultValueFromXml(mContext, row);
            } else if (!pref.isEnabled()) {
                value = pref.getOverrideValue();
            }
            if (value == null) {
                value = getPreferenceValue(mContext, mContext.getPreferences(), row);
            }
        }
        if (LOG) {
            Log.v(TAG, "getSettingCurrentValue(" + row + ") return " + value);
        }
        return value;
    }
        
    public String getDefaultValue(int row) {
        String value = getDefaultValueFromXml(mContext, row);
        if (LOG) {
            Log.v(TAG, "getDefaultValue(" + row + ") return " + value);
        }
        return value;
    }
    
    public void applyValueToParameters(int row, String value) {
        if (LOG) {
            Log.v(TAG, "applyValueToParameters(" + row + ", " + value + ")");
        }
        applyValueToParameters(mContext, mContext.getPreferences(), mContext.getParameters(),
                mContext.getCurrentMode(), row, value);
    }

    public String getOverrideSettingValue(int row) {
        if (LOG) {
            Log.v(TAG, "getOverrideSettingValue(" + row + ")");
        }
        if (0 <= row && row < mOverrideSettingValues.length) {
            return mOverrideSettingValues[row];
        } else {
            return null;
        }
    }

    //for back/front camera setting visible
    public static PreferenceGroup filterUnsuportedPreference(CameraSettings cameraSettings,
            PreferenceGroup preferenceGroup, int cameraId) {
        //CameraSetting.initPreference() will filter some preference according to native Parameter
        //Unluckily, not all item are set by camera server, here we correct these lost items.
        for (int i = 0, len = MATRIX_SETTING_VISIBLE[cameraId].length; i < len; i++) {
            if (!MATRIX_SETTING_VISIBLE[cameraId][i]) {
                cameraSettings.removePreferenceFromScreen(preferenceGroup, KEYS_FOR_SETTING[i], i);
            }
        }
        //clear cache for front/back camera
        int len = DEFAULT_VALUE_FOR_SETTING.length;
        for (int i = 0; i < len; i++) {
            DEFAULT_VALUE_FOR_SETTING[i] = null;
        }
        return preferenceGroup;
    }
    /// @}
    
    public static class FlashMappingFinder implements MappingFinder {
        @Override
        public String find(String current, List<String> supportedList) {
            String supported = current;
            if (supportedList != null && !supportedList.contains(current)) {
                if (Parameters.FLASH_MODE_AUTO.equals(current)) { //if cannot find auto, it video mode, match off
                    supported = Parameters.FLASH_MODE_OFF;
                } else if (Parameters.FLASH_MODE_ON.equals(current)) { //if cannot find on, it video mode, match torch
                    supported = Parameters.FLASH_MODE_TORCH;
                } else if (Parameters.FLASH_MODE_TORCH.equals(current)) { //if cannot find torch, it video mode, match on
                    supported = Parameters.FLASH_MODE_ON;
                }
            }
            if (!supportedList.contains(supported)) {
                supported = supportedList.get(0);
            }
            if (LOG) {
                Log.v(TAG, "find(" + current + ") return " + supported);
            }
            return supported;
        }
        @Override
        public int findIndex(String current, List<String> supportedList) {
            int index = UNKNOWN;
            if (supportedList != null) {
                String supported = find(current, supportedList);
                index = supportedList.indexOf(supported);
            }
            if (LOG) {
                Log.v(TAG, "findIndex(" + current + ", " + supportedList + ") return " + index);
            }
            return index;
        }
        
    }
    
    public static class PictureSizeMappingFinder implements MappingFinder {
        @Override
        public String find(String current, List<String> supportedList) {
            String supported = current;
            int index = current.indexOf('x');
            if (index != UNKNOWN && supportedList != null && !supportedList.contains(current)) {
                //find other appropriate size
                int size = supportedList.size();
                Point findPs = SettingUtils.getSize(supportedList.get(size - 1));
                Point candidatePs = SettingUtils.getSize(current);
                for (int i = size - 2; i >= 0; i--) {
                    Point ps = SettingUtils.getSize(supportedList.get(i));
                    if (ps != null && Math.abs(ps.x - candidatePs.x) <
                            Math.abs(findPs.x - candidatePs.x)) {
                        findPs = ps;
                    }
                }
                supported = SettingUtils.buildSize(findPs.x, findPs.y);
            }
            if (!supportedList.contains(supported)) {
                supported = supportedList.get(0);
            }
            if (LOG) {
                Log.v(TAG, "find(" + current + ") return " + supported);
            }
            return supported;
        }
        @Override
        public int findIndex(String current, List<String> supportedList) {
            int index = UNKNOWN;
            if (supportedList != null) {
                String supported = find(current, supportedList);
                index = supportedList.indexOf(supported);
            }
            if (LOG) {
                Log.v(TAG, "findIndex(" + current + ", " + supportedList + ") return " + index);
            }
            return index;
        }
    }
}