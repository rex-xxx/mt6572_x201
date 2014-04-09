/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.camera;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Point;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.media.CamcorderProfile;
import android.util.DisplayMetrics;
import android.util.FloatMath;

import com.android.camera.ListPreference;
import com.android.camera.actor.EffectsRecorder;
import com.mediatek.camera.ext.ExtensionHelper;
import com.mediatek.camera.ext.IFeatureExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 *  Provides utilities and keys for Camera settings.
 */
public class CameraSettings {
    private static final int NOT_FOUND = -1;

    public static final String KEY_SUN_SET = "backdropper/file:///system/media/video/Sunset.480p.mp4";
    public static final String KEY_ANDROID_INSPACE = "backdropper/file:///system/media/video/AndroidInSpace.480p.mp4";
    public static final String KEY_VERSION = "pref_version_key";
    public static final String KEY_LOCAL_VERSION = "pref_local_version_key";
    public static final String KEY_RECORD_LOCATION = RecordLocationPreference.KEY;
    public static final String KEY_VIDEO_QUALITY = "pref_video_quality_key";
    public static final String KEY_VIDEO_TIME_LAPSE_FRAME_INTERVAL = "pref_video_time_lapse_frame_interval_key";
    public static final String KEY_PICTURE_SIZE = "pref_camera_picturesize_key";
    public static final String KEY_JPEG_QUALITY = "pref_camera_jpegquality_key";
    public static final String KEY_FLASH_MODE = "pref_camera_flashmode_key";
    public static final String KEY_WHITE_BALANCE = "pref_camera_whitebalance_key";
    public static final String KEY_SCENE_MODE = "pref_camera_scenemode_key";
    public static final String KEY_EXPOSURE = "pref_camera_exposure_key";
    public static final String KEY_VIDEO_EFFECT = "pref_video_effect_key";
    public static final String KEY_CAMERA_ID = "pref_camera_id_key";
    // M:
    public static final String KEY_NORMAL_CAPTURE_KEY = "pref_camera_normal_capture_key";
    public static final String KEY_FD_MODE = "pref_camera_fd_key";
    public static final String KEY_ISO = "pref_camera_iso_key";
    public static final String KEY_COLOR_EFFECT = "pref_camera_coloreffect_key";
    public static final String KEY_CAMERA_ZSD = "pref_camera_zsd_key";
    public static final String KEY_STEREO3D_PICTURE_SIZE = "pref_camera_picturesize_stereo3d_key";
    public static final String KEY_STEREO3D_MODE = "pref_stereo3d_mode_key";
    public static final String KEY_STEREO3D_PICTURE_FORMAT = "pref_camera_pictureformat_key";
    public static final String KEY_VIDEO_RECORD_AUDIO = "pref_camera_recordaudio_key";
    public static final String KEY_VIDEO_HD_AUDIO_RECORDING = "pref_camera_video_hd_recording_key";

    public static final String KEY_EDGE = "pref_camera_edge_key";
    public static final String KEY_HUE = "pref_camera_hue_key";
    public static final String KEY_SATURATION = "pref_camera_saturation_key";
    public static final String KEY_BRIGHTNESS = "pref_camera_brightness_key";
    public static final String KEY_CONTRAST = "pref_camera_contrast_key";
    public static final String KEY_SELF_TIMER = "pref_camera_self_timer_key";
    public static final String KEY_ANTI_BANDING = "pref_camera_antibanding_key";
    public static final String KEY_VIDEO_EIS = "pref_video_eis_key";
    public static final String KEY_CONTINUOUS_NUMBER = "pref_camera_shot_number";
    public static final String KEY_IMAGE_PROPERTIES = "pref_camera_image_properties_key";//virtual item
    public static final String KEY_FACE_BEAUTY_PROPERTIES = "pref_camera_facebeauty_properties_key";//virtual item
    public static final String KEY_PICTURE_RATIO = "pref_camera_picturesize_ratio_key";
    public static final String KEY_VOICE = "pref_voice_key";
    public static final String KEY_VIDEO_HDR = "pref_video_hdr_key";
    public static final String KEY_FACE_BEAUTY_SMOOTH = "pref_facebeauty_smooth_key";
    public static final String KEY_FACE_BEAUTY_SKIN_COLOR = "pref_facebeauty_skin_color_key";
    public static final String KEY_FACE_BEAUTY_SHARP = "pref_facebeauty_sharp_key";
    public static final String KEY_CAMERA_FACE_DETECT = "pref_face_detect_key";
    
    public static final String FOCUS_METER_SPOT = "spot";
    public static final String WHITE_BALANCE_AUTO = "auto";
    public static final String COLOR_EFFECT_NONE = "none";
    public static final String MAX_ISO_SPEED = "1600";
    public static final String ISO_SPEED_1600 = "1600";
    public static final String ISO_SPEED_800 = "800";
    public static final String ISO_AUTO = "auto";
    public static final String IMG_SIZE_FOR_HIGH_ISO = "1280x960";      // Limit pic size to 1M
    public static final String IMG_SIZE_FOR_PANORAMA = "1600x1200";     // Limit pic size to 2M
    public static final String FACE_DETECTION_DEFAULT = "on";
    public static final String SELF_TIMER_OFF = "0";
    public static final String DIP_MEDIUM = "middle";
    public static final String DIP_LOW = "low";
    public static final String DIP_HIGH = "high";
    public static final String STEREO3D_ENABLE = "1";
    public static final String STEREO3D_DISABLE = "0";
    // Continuous shot number
    public static final String DEFAULT_CAPTURE_NUM = "40";
    public static final String VIDEO_MICRIPHONE_ON = "on";
    //Mediatek feature end
    
    // false means the continuous shot number will not show in the settings UI
    public static final boolean SUPPORTED_SHOW_CONINUOUS_SHOT_NUMBER = 
	!FeatureSwitcher.isLcaRAM();
    public static final String DEFAULT_CONINUOUS_CAPTURE_NUM = "20";
    public static final String DEFAULT_CAPTURE_NUM_MAX = "99";
    public static final String[] IMG_SIZE_FOR_HIGH_ISO_ARRAYS = new String[] {
        IMG_SIZE_FOR_HIGH_ISO, //4:3
        "1280x720", //16:9
        "1280x768", //5:3
    };
    
    public static final String[] PICTURE_SIZE_4_3 = new String[] {
        "320x240",
        "640x480",
        "1024x768",
        "1280x960",
        "1600x1200",
        "2048x1536",
        "2560x1920",
        "3264x2448",
        "4096x3072",
    };
    public static final String[] PICTURE_SIZE_16_9 = new String[] {
        "1280x720",
        "2048x1152",
        "2560x1440",
        "3328x1872",
    };
    public static final String[] PICTURE_SIZE_5_3 = new String[] {
        "1280x768",
        "1600x960",
        "2880x1728",
        "3600x2160",
    };
    
    public static final String[] PICTURE_SIZE_3_2 = new String[] {
        "1440x960",
        "2048x1360",
        "2560x1712"
    };
    
    public static final String PICTURE_RATIO_16_9 = "1.7778";
    public static final String PICTURE_RATIO_5_3 = "1.6667";
    public static final String PICTURE_RATIO_3_2 = "1.5";
    public static final String PICTURE_RATIO_4_3 = "1.3333";
    
    public static final double [] RATIOS = new double[]{1.3333,1.5,1.6667,1.7778};
    
    public static final String EXPOSURE_DEFAULT_VALUE = "0";

    public static final int CURRENT_VERSION = 5;
    public static final int CURRENT_LOCAL_VERSION = 2;

    public static final int DEFAULT_VIDEO_DURATION = 0; // no limit
    //private static final double ASPECT_TOLERANCE = 0.02;

    private static final String TAG = "CameraSettings";
    private static final boolean LOG = Log.LOGV;

    private final Camera mContext;
    private final Parameters mParameters;
    private final CameraInfo[] mCameraInfo;
    private final int mCameraId;

    public CameraSettings(Camera camera, Parameters parameters,
                          int cameraId, CameraInfo[] cameraInfo) {
        mContext = camera;
        mParameters = parameters;
        mCameraId = cameraId;
        mCameraInfo = cameraInfo;
    }

    public PreferenceGroup getPreferenceGroup(int preferenceRes) {
        PreferenceInflater inflater = new PreferenceInflater(mContext);
        PreferenceGroup group =
                (PreferenceGroup) inflater.inflate(preferenceRes);
        initPreference(group);
        return group;
    }

    public static String getDefaultVideoQuality(int cameraId,
            String defaultQuality) {
        int quality = Integer.valueOf(defaultQuality);
        if (CamcorderProfile.hasProfile(cameraId, quality)) {
            return defaultQuality;
        }
        return Integer.toString(CamcorderProfile.QUALITY_MTK_HIGH);
    }

    public static void initialCameraPictureSize(Context context, Parameters parameters) {
        /// M: here we find the full screen picture size for default, not first one in arrays.xml
        List<String> supportedRatios = buildPreviewRatios(context, parameters);
        String ratioString = null;
        if (supportedRatios != null && supportedRatios.size() > 0) {
            SharedPreferences.Editor editor = ComboPreferences.get(context).edit();
            ratioString = supportedRatios.get(supportedRatios.size() - 1);
            editor.putString(KEY_PICTURE_RATIO, ratioString);
            editor.apply();
        }
        List<String> supportedSizes = buildSupportedPictureSize(context, parameters, ratioString);
        if (supportedSizes != null && supportedSizes.size() > 0) {
            String findPictureSize = supportedSizes.get(supportedSizes.size() - 1);
            SharedPreferences.Editor editor = ComboPreferences.get(context).edit();
            editor.putString(KEY_PICTURE_SIZE, findPictureSize);
            editor.apply();
            Point ps = SettingUtils.getSize(findPictureSize);
            parameters.setPictureSize(ps.x, ps.y);
        }
    }

    public void removePreferenceFromScreen(PreferenceGroup group, String key, int row) {
        removePreference(group, key, row);
    }

    public static boolean setCameraPictureSize(String candidate, List<Size> supported,
            Parameters parameters, String targetRatio, Context context) {
        if (LOG) {
            Log.v(TAG, "setCameraPictureSize(" + candidate + ")");
        }
        int index = candidate.indexOf('x');
        if (index == NOT_FOUND) {
            return false;
        }
        List<String> supportedRatioSizes = buildSupportedPictureSize(context, parameters, targetRatio);
        candidate = SettingChecker.MAPPING_FINDER_PICTURE_SIZE.find(candidate, supportedRatioSizes);
        index = candidate == null ? NOT_FOUND : candidate.indexOf('x');
        int width = Integer.parseInt(candidate.substring(0, index));
        int height = Integer.parseInt(candidate.substring(index + 1));
        parameters.setPictureSize(width, height);
        return true;
    }
    
    private static boolean toleranceRatio(double target, double candidate) {
        boolean tolerance = true;
        if (candidate > 0) {
            tolerance = Math.abs(target - candidate) <= Util.ASPECT_TOLERANCE;
        }
        if (LOG) {
            Log.v(TAG, "toleranceRatio(" + target + ", " + candidate + ") return " + tolerance);
        }
        return tolerance;
    }

    private void initPreference(PreferenceGroup group) {
        ListPreference videoQuality = group.findPreference(KEY_VIDEO_QUALITY);
        ListPreference timeLapseInterval = group.findPreference(KEY_VIDEO_TIME_LAPSE_FRAME_INTERVAL);
        ListPreference pictureSize = group.findPreference(KEY_PICTURE_SIZE);
        ListPreference whiteBalance =  group.findPreference(KEY_WHITE_BALANCE);
        ListPreference sceneMode = group.findPreference(KEY_SCENE_MODE);
        ListPreference flashMode = group.findPreference(KEY_FLASH_MODE);
        ListPreference exposure = group.findPreference(KEY_EXPOSURE);
        IconListPreference cameraIdPref = (IconListPreference) group.findPreference(KEY_CAMERA_ID);
        ListPreference videoEffect = group.findPreference(KEY_VIDEO_EFFECT);

        ListPreference iso = group.findPreference(KEY_ISO);
        ListPreference antiBanding = group.findPreference(KEY_ANTI_BANDING);
        ListPreference colorEffect = group.findPreference(KEY_COLOR_EFFECT);
        ListPreference edge = group.findPreference(KEY_EDGE);
        ListPreference hue = group.findPreference(KEY_HUE);
        ListPreference sat = group.findPreference(KEY_SATURATION);
        ListPreference brightness = group.findPreference(KEY_BRIGHTNESS);
        ListPreference contrast = group.findPreference(KEY_CONTRAST);
        ListPreference eis = group.findPreference(KEY_VIDEO_EIS);
        ListPreference stereo3dPictureSize = group.findPreference(KEY_STEREO3D_PICTURE_SIZE);
        ListPreference continuousNumber = group.findPreference(KEY_CONTINUOUS_NUMBER);
        ListPreference imageproperties = group.findPreference(KEY_IMAGE_PROPERTIES);
        ListPreference pictureRatio = group.findPreference(KEY_PICTURE_RATIO);
        ListPreference facebeautyproperties = group.findPreference(KEY_FACE_BEAUTY_PROPERTIES);
        ListPreference smooth = group.findPreference(KEY_FACE_BEAUTY_SMOOTH);
        ListPreference skinColor = group.findPreference(KEY_FACE_BEAUTY_SKIN_COLOR);
        ListPreference sharp = group.findPreference(KEY_FACE_BEAUTY_SHARP);
        ListPreference voice = group.findPreference(KEY_VOICE);
        ListPreference zsd = group.findPreference(KEY_CAMERA_ZSD);
        ListPreference selftimer = group.findPreference(KEY_SELF_TIMER);
        ListPreference microphone = group.findPreference(KEY_VIDEO_RECORD_AUDIO);
        ListPreference storeLocation = group.findPreference(KEY_RECORD_LOCATION);
        ListPreference faceDetect = group.findPreference(KEY_CAMERA_FACE_DETECT);
        
        SettingChecker settingChecker = mContext.getSettingChecker();
        synchronized (settingChecker) {
            settingChecker.clearListPreference();
            settingChecker.setListPreference(SettingChecker.ROW_SETTING_SELF_TIMER, selftimer);
            // when LCA not supported ,we don't want the numbe show in the settings
            if (SUPPORTED_SHOW_CONINUOUS_SHOT_NUMBER) {
                // SetListPreference is put the continuous shot number a the position of ROW_SETTING_CONTINUOUS
                // after will get from the ListPreference[] mListPrefs array
                // so if we not put the continuous shot in the array ,other space will don't get the values 
                settingChecker.setListPreference(SettingChecker.ROW_SETTING_CONTINUOUS, continuousNumber);
            }
            settingChecker.setListPreference(SettingChecker.ROW_SETTING_GEO_TAG, storeLocation);
            settingChecker.setListPreference(SettingChecker.ROW_SETTING_MICROPHONE, microphone);
            settingChecker.setListPreference(SettingChecker.ROW_SETTING_TIME_LAPSE, timeLapseInterval);
            settingChecker.setListPreference(SettingChecker.ROW_SETTING_CAMERA_FACE_DETECT, faceDetect);
            
//            if (imageproperties != null) { //remove image properties if hue not exists.
//                filterUnsupportedOptions(group, imageproperties, mParameters.getSupportedHueMode(),
//                        SettingChecker.ROW_SETTING_IMAGE_PROPERTIES);
//            }
//
            //remove facebeauty properties if fb capture mode not exist
            if (facebeautyproperties != null && !ParametersHelper.isFaceBeautySupported(mParameters)) {
                removePreference(group, facebeautyproperties.getKey(),
                        SettingChecker.ROW_SETTING_FACEBEAUTY_PROPERTIES);
            } else {
                setListPreference(SettingChecker.ROW_SETTING_FACEBEAUTY_PROPERTIES, facebeautyproperties);
            }
    
            if (iso != null) {
                filterUnsupportedOptions(group, iso, mParameters.getSupportedISOSpeed(),
                        SettingChecker.ROW_SETTING_ISO);
            }
            // facebeauty smooth
            if (smooth != null) {
                buildFaceBeautyPreference(group, ParametersHelper.FACEBEAUTY_SMOOTH, smooth,
                        SettingChecker.ROW_SETTING_FACEBEAUTY_SMOOTH);
            }
            // facebeauty skin-color
            if (skinColor != null) {
                buildFaceBeautyPreference(group, ParametersHelper.FACEBEAUTY_SKIN_COLOR, skinColor,
                        SettingChecker.ROW_SETTING_FACEBEAUTY_SKIN_COLOR);
            }
            // facebeauty sharp
            if (sharp != null) {
                buildFaceBeautyPreference(group, ParametersHelper.FACEBEAUTY_SHARP, sharp,
                        SettingChecker.ROW_SETTING_FACEBEAUTY_SHARP);
            }
            if (colorEffect != null) {
                filterUnsupportedOptions(group, colorEffect, mParameters.getSupportedColorEffects(),
                        SettingChecker.ROW_SETTING_COLOR_EFFECT);
            }
            if (antiBanding != null) {
                filterUnsupportedOptions(group, antiBanding, mParameters.getSupportedAntibanding(),
                        SettingChecker.ROW_SETTING_ANTI_FLICKER);
            }
            /// image properties begin
            List<String> supportedImageProperties = new ArrayList<String>();
            if (edge != null) {
                filterUnsupportedOptions(group, edge, mParameters.getSupportedEdgeMode(),
                        SettingChecker.ROW_SETTING_SHARPNESS);
                buildSupportedListperference(supportedImageProperties,SettingChecker.ROW_SETTING_SHARPNESS);
            }
            if (hue != null) {
                filterUnsupportedOptions(group, hue, mParameters.getSupportedHueMode(),
                        SettingChecker.ROW_SETTING_HUE);
                buildSupportedListperference(supportedImageProperties,SettingChecker.ROW_SETTING_HUE);
            }
            if (sat != null) {
                filterUnsupportedOptions(group, sat, mParameters.getSupportedSaturationMode(),
                        SettingChecker.ROW_SETTING_SATURATION);
                buildSupportedListperference(supportedImageProperties,SettingChecker.ROW_SETTING_SATURATION);
            }
            if (brightness != null) {
                filterUnsupportedOptions(group, brightness, mParameters.getSupportedBrightnessMode(),
                        SettingChecker.ROW_SETTING_BRIGHTNESS);
                buildSupportedListperference(supportedImageProperties,SettingChecker.ROW_SETTING_BRIGHTNESS);
            }
            if (contrast != null) {
                filterUnsupportedOptions(group, contrast, mParameters.getSupportedContrastMode(),
                        SettingChecker.ROW_SETTING_CONTRAST);
                buildSupportedListperference(supportedImageProperties,SettingChecker.ROW_SETTING_CONTRAST);
            }
            
            if (imageproperties != null) {
                filterUnsupportedEntries(group, imageproperties, supportedImageProperties,true,
                        SettingChecker.ROW_SETTING_IMAGE_PROPERTIES);
            }
          /// image properties end

            if (eis != null && !"true".equals(mParameters.get("video-stabilization-supported"))) {
                filterUnsupportedOptions(group, eis, null, SettingChecker.ROW_SETTING_VIDEO_STABLE);
            } else if (eis != null && "true".equals(mParameters.get("video-stabilization-supported"))) {
                setListPreference(SettingChecker.ROW_SETTING_VIDEO_STABLE, eis);
            }
    
            if (!FeatureSwitcher.isHdRecordingEnabled()) {
                removePreference(group, KEY_VIDEO_HD_AUDIO_RECORDING, SettingChecker.ROW_SETTING_AUDIO_MODE);
            } else {
                setListPreference(SettingChecker.ROW_SETTING_AUDIO_MODE, group.findPreference(KEY_VIDEO_HD_AUDIO_RECORDING));
            }
    
            if (FeatureSwitcher.isLcaRAM()) {
                removePreference(group, KEY_CAMERA_ZSD, SettingChecker.ROW_SETTING_ZSD);
            } else if (zsd != null) {
                filterUnsupportedOptions(group, zsd, mParameters.getSupportedZSDMode(),
                        SettingChecker.ROW_SETTING_ZSD);
            }
            //Mediatek feature end
    
            // Since the screen could be loaded from different resources, we need
            // to check if the preference is available here
            if (videoQuality != null) {
                filterUnsupportedOptions(group, videoQuality, getMTKSupportedVideoQuality(),
                        SettingChecker.ROW_SETTING_VIDEO_QUALITY);
            }
            
            if (pictureRatio != null) {
                List<String> supportedRatios = buildPreviewRatios(mContext, mParameters);
                filterUnsupportedOptions(group, pictureRatio, supportedRatios,
                        SettingChecker.ROW_SETTING_PICTURE_RATIO);
            }
            if (pictureSize != null && pictureRatio != null) {
                /// M: filter supported values.
                filterUnsupportedOptionsForPictureSize(group, pictureSize, sizeListToStringList(
                        mParameters.getSupportedPictureSizes()), false,
                        SettingChecker.ROW_SETTING_PICTURE_SIZE);
                /// M: for picture size was ordered, here we don't set it to index 0.
                List<String> supportedForRatio = buildSupportedPictureSize(mContext, mParameters, pictureRatio.getValue());
                filterDisabledOptions(group, pictureSize, supportedForRatio, false,
                        SettingChecker.ROW_SETTING_PICTURE_SIZE);
            }
            
            IFeatureExtension updateString = ExtensionHelper.getFeatureExtension();
            if (whiteBalance != null) { /// M: for cmcc string feature
                updateString.updateWBStrings(whiteBalance.getEntries());
                filterUnsupportedOptions(group, whiteBalance, mParameters.getSupportedWhiteBalance(),
                        SettingChecker.ROW_SETTING_WHITE_BALANCE);
            }
            if (sceneMode != null) {
                CharSequence[] entries = sceneMode.getEntries();
                CharSequence[] entryValues = sceneMode.getEntryValues();
                int length = entries.length;
                ArrayList<CharSequence> newEntries = new ArrayList<CharSequence>();
                ArrayList<CharSequence> newEntryValues = new ArrayList<CharSequence>();
                for (int i = 0; i < length; i++) {
                    newEntries.add(entries[i]);
                    newEntryValues.add(entryValues[i]);
                }
                updateString.updateSceneStrings(newEntries, newEntryValues);
                length = newEntryValues.size();
                sceneMode.setEntries(newEntries.toArray(new CharSequence[length]));
                sceneMode.setEntryValues(newEntryValues.toArray(new CharSequence[length]));
                filterUnsupportedOptions(group, sceneMode, mParameters.getSupportedSceneModes(),
                        SettingChecker.ROW_SETTING_SCENCE_MODE);
            }
            if (flashMode != null) {
                filterUnsupportedOptions(group, flashMode, mParameters.getSupportedFlashModes(),
                        SettingChecker.ROW_SETTING_FLASH);
            }
            if (exposure != null) {
                buildExposureCompensation(group, exposure, SettingChecker.ROW_SETTING_EXPOSURE);
            }
            if (cameraIdPref != null) {
                buildCameraId(group, cameraIdPref, SettingChecker.ROW_SETTING_DUAL_CAMERA);
            }
    
            if (timeLapseInterval != null) {
                resetIfInvalid(timeLapseInterval);
            }
            if (videoEffect != null) {
                if (!FeatureSwitcher.isVideoLiveEffectEnabled()) {
                    removePreference(group, videoEffect.getKey(), SettingChecker.ROW_SETTING_LIVE_EFFECT);
                } else {
                    initVideoEffect(group, videoEffect, SettingChecker.ROW_SETTING_LIVE_EFFECT);
                    resetIfInvalid(videoEffect);
                }
            }
            
            if (!FeatureSwitcher.isVoiceEnabled()) {
                removePreference(group, KEY_VOICE, SettingChecker.ROW_SETTING_VOICE);
            } else {
                ((VoiceListPreference)voice).setVoiceManager(((Camera)mContext).getVoiceManager());
                setListPreference(SettingChecker.ROW_SETTING_VOICE, voice);
            }
        }
    }
    
    public static List<String> buildSupportedPictureSize(Context context, Parameters parameters, String targetRatio) {
        ArrayList<String> list = new ArrayList<String>();
        double ratio = 0;
        if (targetRatio == null) {
            ratio = findFullscreenRatio(context, parameters);
        } else {
            try {
                ratio = Double.parseDouble(targetRatio);
            } catch (NumberFormatException e) {
                Log.w(TAG, "buildSupportedPictureSize() bad ratio: " + targetRatio, e);
                ratio = findFullscreenRatio(context, parameters);
            }
        }
        List<Size> sizes = parameters.getSupportedPictureSizes();
        if (sizes != null) {
            for (Size size : sizes) {
                if (toleranceRatio(ratio, (double)size.width / size.height)) {
                    list.add(SettingUtils.buildSize(size.width, size.height));
                }
            }
        }
        if (LOG) {
            Log.v(TAG, "buildSupportedPictureSize(" + parameters + ", " + targetRatio + ")" + list.size());
            for (String added : list) {
                Log.v(TAG, "buildSupportedPictureSize() add " + added);
            }
        }
        return list;
    }

    private static List<String> buildPreviewRatios(Context context, Parameters parameters) {
        List<String> supportedRatios = new ArrayList<String>();
        String findString = null;
        if (context != null && parameters != null) {
            double find = findFullscreenRatio(context, parameters);
            supportedRatios.add(SettingUtils.getRatioString(4d / 3)); //add standard ratio
            findString = SettingUtils.getRatioString(find);
            if (!supportedRatios.contains(findString)) { //add full screen ratio
                supportedRatios.add(findString);
            }
        }
        if (LOG) {
            Log.v(TAG, "buildPreviewRatios(" + parameters + ") add supportedRatio " + findString);
        }
        return supportedRatios;
    }

    public static double findFullscreenRatio(Context context, Parameters parameters) {
        double find = 4d / 3;
        if (context != null && parameters != null) {
            DisplayMetrics metrics = context.getResources().getDisplayMetrics();
            double fullscreen;
            if (metrics.widthPixels > metrics.heightPixels) {
                fullscreen = (double)metrics.widthPixels / metrics.heightPixels;
            } else {
                fullscreen = (double)metrics.heightPixels / metrics.widthPixels;
            }
            
            for (int i=0; i < RATIOS.length ;i++) {
                if (Math.abs(RATIOS[i] - fullscreen) < Math.abs(fullscreen - find)) {
                    find = RATIOS[i];
                }
            }
        }
        
        List<Size> sizes = parameters.getSupportedPictureSizes();
        if (sizes != null) {
            for (Size size : sizes) {
                if (toleranceRatio(find, (double)size.width / size.height)) {
                    Log.v(TAG, "findFullscreenRatio(" + parameters + ") return " + find);
                    return find;
                } 
            }
            find = 4d / 3;
        }
        if (LOG) {
            Log.v(TAG, "findFullscreenRatio(" + parameters + ") return " + find);
        }
        
        return find;
    }

    //should be refactored for icons
    private void buildExposureCompensation(PreferenceGroup group, ListPreference exposure, int row) {
        int max = mParameters.getMaxExposureCompensation();
        int min = mParameters.getMinExposureCompensation();
        if (max == 0 && min == 0) {
            removePreference(group, exposure.getKey(), row);
            return;
        }
        float step = mParameters.getExposureCompensationStep();

        // show only integer values for exposure compensation
        int maxValue = (int) FloatMath.floor(max * step);
        int minValue = (int) FloatMath.ceil(min * step);
        //CharSequence entries[] = new CharSequence[maxValue - minValue + 1];
        //CharSequence entryValues[] = new CharSequence[maxValue - minValue + 1];
        ArrayList<String> entryValuesList = new ArrayList<String>();
        for (int i = minValue; i <= maxValue; ++i) {
            String value = Integer.toString(Math.round(i / step));
            //StringBuilder builder = new StringBuilder();
            //if (i > 0) builder.append('+');
            //entries[maxValue - i] = builder.append(i).toString();
            entryValuesList.add(String.valueOf(value));
        }
        //exposure.setEntries(entries);
        //exposure.setEntryValues(entryValues);
        exposure.filterUnsupported(entryValuesList);
        setListPreference(row, exposure);
    }

    //should be refactored for icons
    private void buildFaceBeautyPreference(PreferenceGroup group, int fbKey, ListPreference fbPreference, int row) {
        int max = ParametersHelper.getMaxLevel(mParameters, fbKey);
        int min = ParametersHelper.getMinLevel(mParameters, fbKey);
        if (max == 0 && min == 0) {
            removePreference(group, fbPreference.getKey(), row);
            return;
        }

        ArrayList<String> entryValuesList = new ArrayList<String>();
        for (int i = min; i <= max; ++i) {
            entryValuesList.add(String.valueOf(i));
        }
        fbPreference.filterUnsupported(entryValuesList);
        setListPreference(row, fbPreference);
    }

    private void buildCameraId(PreferenceGroup group, IconListPreference preference, int row) {
        int numOfCameras = mCameraInfo.length;
        if (numOfCameras < 2) {
            removePreference(group, preference.getKey(), row);
            return;
        }

        CharSequence[] entryValues = new CharSequence[2];
        for (int i = 0; i < mCameraInfo.length; ++i) {
            int index =
                    (mCameraInfo[i].facing == CameraInfo.CAMERA_FACING_FRONT)
                    ? CameraInfo.CAMERA_FACING_FRONT
                    : CameraInfo.CAMERA_FACING_BACK;
            if (entryValues[index] == null) {
                entryValues[index] = "" + i;
                if (entryValues[((index == 1) ? 0 : 1)] != null) {
                    break;
                }
            }
        }
        preference.setEntryValues(entryValues);
        setListPreference(row, preference);
    }

    private boolean removePreference(PreferenceGroup group, String key, int row) {
        for (int i = 0, n = group.size(); i < n; i++) {
            CameraPreference child = group.get(i);
            if (child instanceof PreferenceGroup) {
                if (removePreference((PreferenceGroup) child, key, row)) {
                    return true;
                }
            }
            if (child instanceof ListPreference &&
                    ((ListPreference) child).getKey().equals(key)) {
                group.removePreference(i);
                removePreference(row);
                return true;
            }
        }
        return false;
    }
    
    private void removePreference(int row) {
        mContext.getSettingChecker().setListPreference(row, null);
    }
    
    private void setListPreference(int row, ListPreference pref) {
        mContext.getSettingChecker().setListPreference(row, pref);
    }
    
    private void filterUnsupportedOptions(PreferenceGroup group,
            ListPreference pref, List<String> supported, int row) {
        filterUnsupportedOptions(group, pref, supported, true, row);
    }
    
    private void filterUnsupportedOptions(PreferenceGroup group,
            ListPreference pref, List<String> supported, boolean resetFirst, int row) {

        // Remove the preference if the parameter is not supported or there is
        // only one options for the settings.
        if (supported == null || supported.size() <= 1) {
            removePreference(group, pref.getKey(), row);
            return;
        }

        pref.filterUnsupported(supported);
        if (pref.getEntries().length <= 1) {
            removePreference(group, pref.getKey(), row);
            return;
        }

        resetIfInvalid(pref, resetFirst);
        setListPreference(row, pref);
    }
    
    //add for filter unsupported image properties
    //image properties just can be filtered by entries
    private void filterUnsupportedEntries(PreferenceGroup group,
            ListPreference pref, List<String> supported, boolean resetFirst, int row) {
        if (supported == null || supported.size() <= 0) {
            removePreference(group, pref.getKey(), row);
            return;
        }
        pref.filterUnsupportedEntries(supported);
        if (pref.getEntries().length <= 0) {
            removePreference(group, pref.getKey(), row);
            return;
        }
        resetIfInvalid(pref, resetFirst);
        setListPreference(row, pref);
    }

    private void buildSupportedListperference(List<String> supportedList , int key ) {
        ListPreference list = mContext.getSettingChecker().getListPreference(key);
        if(list != null && supportedList!= null) {
            supportedList.add(list.getKey());
        }
    }

    private void filterUnsupportedOptionsForPictureSize(PreferenceGroup group,
            ListPreference pref, List<String> supported, boolean resetFirst, int row) {

        // Remove the preference if the parameter is not supported or there is
        // only one options for the settings.
        if (supported == null || supported.size() < 1) {
            removePreference(group, pref.getKey(), row);
            return;
        }

        pref.filterUnsupported(supported);
        if (pref.getEntries().length < 1) {
            removePreference(group, pref.getKey(), row);
            return;
        }

        resetIfInvalid(pref, resetFirst);
        setListPreference(row, pref);
    }
    
    private void filterDisabledOptions(PreferenceGroup group,
            ListPreference pref, List<String> supported, boolean resetFirst, int row) {

        // Remove the preference if the parameter is not supported or there is
        // only one options for the settings.
        if (supported == null || supported.size() < 1) {
            removePreference(group, pref.getKey(), row);
            return;
        }

        pref.filterDisabled(supported);
        if (pref.getEntries().length < 1) {
            removePreference(group, pref.getKey(), row);
            return;
        }

        resetIfInvalid(pref, resetFirst);
        setListPreference(row, pref);
    }

    private void resetIfInvalid(ListPreference pref) {
        resetIfInvalid(pref, true);
    }
    
    private void resetIfInvalid(ListPreference pref, boolean first) {
        // Set the value to the first entry if it is invalid.
        String value = pref.getValue();
        if (pref.findIndexOfValue(value) == NOT_FOUND) {
            if (first) {
                pref.setValueIndex(0);
            } else if (pref.getEntryValues() != null && pref.getEntryValues().length > 0) {
                pref.setValueIndex(pref.getEntryValues().length - 1);
            }
        }
    }

    private static List<String> sizeListToStringList(List<Size> sizes) {
        ArrayList<String> list = new ArrayList<String>();
        for (Size size : sizes) {
            list.add(String.format(Locale.ENGLISH, "%dx%d", size.width, size.height));
        }
        return list;
    }

    public static void upgradeLocalPreferences(SharedPreferences pref) {
        int version;
        try {
            version = pref.getInt(KEY_LOCAL_VERSION, 0);
        } catch (Exception ex) {
            version = 0;
        }
        if (version == CURRENT_LOCAL_VERSION) return;

        SharedPreferences.Editor editor = pref.edit();
        if (version == 1) {
            // We use numbers to represent the quality now. The quality definition is identical to
            // that of CamcorderProfile.java.
            editor.remove("pref_video_quality_key");
        }
        editor.putInt(KEY_LOCAL_VERSION, CURRENT_LOCAL_VERSION);
        editor.apply();
    }

    public static void upgradeGlobalPreferences(SharedPreferences pref) {
        upgradeOldVersion(pref);
        upgradeCameraId(pref);
    }

    private static void upgradeOldVersion(SharedPreferences pref) {
        int version;
        try {
            version = pref.getInt(KEY_VERSION, 0);
        } catch (Exception ex) {
            version = 0;
        }
        if (version == CURRENT_VERSION) return;

        SharedPreferences.Editor editor = pref.edit();
        if (version == 0) {
            // We won't use the preference which change in version 1.
            // So, just upgrade to version 1 directly
            version = 1;
        }
        if (version == 1) {
            // Change jpeg quality {65,75,85} to {normal,fine,superfine}
            String quality = pref.getString(KEY_JPEG_QUALITY, "85");
            if (quality.equals("65")) {
                quality = "normal";
            } else if (quality.equals("75")) {
                quality = "fine";
            } else {
                quality = "superfine";
            }
            editor.putString(KEY_JPEG_QUALITY, quality);
            version = 2;
        }
        if (version == 2) {
            editor.putString(KEY_RECORD_LOCATION,
                    pref.getBoolean(KEY_RECORD_LOCATION, false)
                    ? RecordLocationPreference.VALUE_ON
                    : RecordLocationPreference.VALUE_NONE);
            version = 3;
        }
        if (version == 3) {
            // Just use video quality to replace it and
            // ignore the current settings.
            editor.remove("pref_camera_videoquality_key");
            editor.remove("pref_camera_video_duration_key");
        }

        editor.putInt(KEY_VERSION, CURRENT_VERSION);
        editor.apply();
    }

    private static void upgradeCameraId(SharedPreferences pref) {
        // The id stored in the preference may be out of range if we are running
        // inside the emulator and a webcam is removed.
        // Note: This method accesses the global preferences directly, not the
        // combo preferences.
        int cameraId = readPreferredCameraId(pref);
        if (cameraId == 0) return;  // fast path

        int n = CameraHolder.instance().getNumberOfCameras();
        if (cameraId < 0 || cameraId >= n) {
            writePreferredCameraId(pref, 0);
        }
    }

    public static int readPreferredCameraId(SharedPreferences pref) {
        return Integer.parseInt(pref.getString(KEY_CAMERA_ID, "0"));
    }

    public static void writePreferredCameraId(SharedPreferences pref,
            int cameraId) {
        Editor editor = pref.edit();
        editor.putString(KEY_CAMERA_ID, Integer.toString(cameraId));
        editor.apply();
    }

    public static int readExposure(ComboPreferences preferences) {
        String exposure = preferences.getString(
                CameraSettings.KEY_EXPOSURE,
                EXPOSURE_DEFAULT_VALUE);
        try {
            return Integer.parseInt(exposure);
        } catch (Exception ex) {
            Log.e(TAG, "Invalid exposure: " + exposure);
        }
        return 0;
    }

    public static int readEffectType(SharedPreferences pref) {
        String effectSelection = pref.getString(KEY_VIDEO_EFFECT, "none");
        if (effectSelection.equals("none")) {
            return EffectsRecorder.EFFECT_NONE;
        } else if (effectSelection.startsWith("goofy_face")) {
            return EffectsRecorder.EFFECT_GOOFY_FACE;
        } else if (effectSelection.startsWith("backdropper")) {
            return EffectsRecorder.EFFECT_BACKDROPPER;
        }
        Log.e(TAG, "Invalid effect selection: " + effectSelection);
        return EffectsRecorder.EFFECT_NONE;
    }

    public static void clearEffects(SharedPreferences pref, String mStereo3DMode) {
        if (CameraSettings.STEREO3D_ENABLE.equals(mStereo3DMode)) {
            ComboPreferences.Editor editor = pref.edit();
            editor.putString(CameraSettings.KEY_VIDEO_EFFECT, "none");
            editor.apply();
        }
    }

    public static Object readEffectParameter(SharedPreferences pref) {
        String effectSelection = pref.getString(KEY_VIDEO_EFFECT, "none");
        if (effectSelection.equals("none")) {
            return null;
        }
        int separatorIndex = effectSelection.indexOf('/');
        String effectParameter =
                effectSelection.substring(separatorIndex + 1);
        if (effectSelection.startsWith("goofy_face")) {
            if (effectParameter.equals("squeeze")) {
                return EffectsRecorder.EFFECT_GF_SQUEEZE;
            } else if (effectParameter.equals("big_eyes")) {
                return EffectsRecorder.EFFECT_GF_BIG_EYES;
            } else if (effectParameter.equals("big_mouth")) {
                return EffectsRecorder.EFFECT_GF_BIG_MOUTH;
            } else if (effectParameter.equals("small_mouth")) {
                return EffectsRecorder.EFFECT_GF_SMALL_MOUTH;
            } else if (effectParameter.equals("big_nose")) {
                return EffectsRecorder.EFFECT_GF_BIG_NOSE;
            } else if (effectParameter.equals("small_eyes")) {
                return EffectsRecorder.EFFECT_GF_SMALL_EYES;
            }
        } else if (effectSelection.startsWith("backdropper")) {
            // Parameter is a string that either encodes the URI to use,
            // or specifies 'gallery'.
            return effectParameter;
        }

        Log.e(TAG, "Invalid effect selection: " + effectSelection);
        return null;
    }

    public static void writePreferredVideoEffect(SharedPreferences pref, int effectType, Object effectParameter) {
        String effectTypeValue = "none";
        String effectParameterValue = null;
        switch (effectType) {
        case EffectsRecorder.EFFECT_NONE:
            effectTypeValue = "none";
            break;
        case EffectsRecorder.EFFECT_GOOFY_FACE:
            effectTypeValue = "goofy_face";
            effectParameterValue = "squeeze";
            int googyValue = EffectsRecorder.EFFECT_GF_SQUEEZE;
            try {
                googyValue = Integer.parseInt(String.valueOf(effectParameter));
            } catch (NumberFormatException ex) {
                ex.printStackTrace();
            }
            switch (googyValue) {
            case EffectsRecorder.EFFECT_GF_SQUEEZE:
                effectParameterValue = "squeeze";
                break;
            case EffectsRecorder.EFFECT_GF_BIG_EYES:
                effectParameterValue = "big_eyes";
                break;
            case EffectsRecorder.EFFECT_GF_BIG_MOUTH:
                effectParameterValue = "big_mouth";
                break;
            case EffectsRecorder.EFFECT_GF_SMALL_MOUTH:
                effectParameterValue = "small_mouth";
                break;
            case EffectsRecorder.EFFECT_GF_BIG_NOSE:
                effectParameterValue = "big_nose";
                break;
            case EffectsRecorder.EFFECT_GF_SMALL_EYES:
                effectParameterValue = "small_eyes";
                break;
            default:
                break;
            }
            break;
        case EffectsRecorder.EFFECT_BACKDROPPER:
            effectTypeValue = "backdropper";
            effectParameterValue = String.valueOf(effectParameter);
            break;
        default:
            break;
        }
        String finalValue = effectTypeValue + (effectParameterValue == null ? "" : "/" + effectParameterValue);
        ComboPreferences.Editor editor = pref.edit();
        editor.putString(CameraSettings.KEY_VIDEO_EFFECT, finalValue);
        editor.apply();
        if (LOG) {
            Log.v(TAG, "writePreferredVideoEffect(" + effectType + ", " + effectParameter + ") write " + finalValue);
        }
    }

    public static void restorePreferences(Context context,
            ComboPreferences preferences, Parameters parameters) {
        int currentCameraId = readPreferredCameraId(preferences);

        // Clear the preferences of both cameras.
        int backCameraId = CameraHolder.instance().getBackCameraId();
        if (backCameraId != -1) {
            preferences.setLocalId(context, backCameraId);
            Editor editor = preferences.edit();
            editor.clear();
            editor.apply();
        }
        int frontCameraId = CameraHolder.instance().getFrontCameraId();
        if (frontCameraId != -1) {
            preferences.setLocalId(context, frontCameraId);
            Editor editor = preferences.edit();
            editor.clear();
            editor.apply();
        }

        // Switch back to the preferences of the current camera. Otherwise,
        // we may write the preference to wrong camera later.
        preferences.setLocalId(context, currentCameraId);

        upgradeGlobalPreferences(preferences.getGlobal());
        upgradeLocalPreferences(preferences.getLocal());

        // Write back the current camera id because parameters are related to
        // the camera. Otherwise, we may switch to the front camera but the
        // initial picture size is that of the back camera.
        initialCameraPictureSize(context, parameters);
        writePreferredCameraId(preferences, currentCameraId);
    }

    private ArrayList<String> getSupportedVideoQuality() {
        ArrayList<String> supported = new ArrayList<String>();
        // Check for supported quality
        if (CamcorderProfile.hasProfile(mCameraId, CamcorderProfile.QUALITY_1080P)) {
            supported.add(Integer.toString(CamcorderProfile.QUALITY_1080P));
        }
        if (CamcorderProfile.hasProfile(mCameraId, CamcorderProfile.QUALITY_720P)) {
            supported.add(Integer.toString(CamcorderProfile.QUALITY_720P));
        }
        if (CamcorderProfile.hasProfile(mCameraId, CamcorderProfile.QUALITY_480P)) {
            supported.add(Integer.toString(CamcorderProfile.QUALITY_480P));
        }

        return supported;
    }

    private void initVideoEffect(PreferenceGroup group, ListPreference videoEffect, int row) {
        CharSequence[] values = videoEffect.getEntryValues();

        boolean goofyFaceSupported =
                EffectsRecorder.isEffectSupported(EffectsRecorder.EFFECT_GOOFY_FACE);
        boolean backdropperSupported =
                EffectsRecorder.isEffectSupported(EffectsRecorder.EFFECT_BACKDROPPER) &&
                mParameters.isAutoExposureLockSupported() &&
                mParameters.isAutoWhiteBalanceLockSupported();

        ArrayList<String> supported = new ArrayList<String>();
        for (CharSequence value : values) {
            String effectSelection = value.toString();
            if (!goofyFaceSupported && effectSelection.startsWith("goofy_face")) {
                continue;
            }
            if (!backdropperSupported && effectSelection.startsWith("backdropper")) {
                continue;
            }
            if (FeatureSwitcher.isLcaROM()&&(effectSelection.equals(KEY_SUN_SET)||effectSelection.equals(KEY_ANDROID_INSPACE))) {
                continue;
            }
            supported.add(effectSelection);
        }

        filterUnsupportedOptions(group, videoEffect, supported, row);
    }

    private ArrayList<String> getMTKSupportedVideoQuality() {
        ArrayList<String> supported = new ArrayList<String>();
        // Check for supported quality
        if (CamcorderProfile.hasProfile(mCameraId, CamcorderProfile.QUALITY_MTK_LOW)) {
            supported.add(Integer.toString(CamcorderProfile.QUALITY_MTK_LOW));
        }
        if (CamcorderProfile.hasProfile(mCameraId, CamcorderProfile.QUALITY_MTK_MEDIUM)) {
            supported.add(Integer.toString(CamcorderProfile.QUALITY_MTK_MEDIUM));
        }
        if (CamcorderProfile.hasProfile(mCameraId, CamcorderProfile.QUALITY_MTK_HIGH)) {
            supported.add(Integer.toString(CamcorderProfile.QUALITY_MTK_HIGH));
        }
        if (CamcorderProfile.hasProfile(mCameraId, CamcorderProfile.QUALITY_MTK_FINE)) {
            supported.add(Integer.toString(CamcorderProfile.QUALITY_MTK_FINE));
        }
        if (CamcorderProfile.hasProfile(mCameraId, CamcorderProfile.QUALITY_MTK_1080P)) {
            supported.add(Integer.toString(CamcorderProfile.QUALITY_MTK_1080P));
        }
        return supported;
    }

    public static String readPreferredCamera3DMode(SharedPreferences pref) {
        return pref.getString(KEY_STEREO3D_MODE, STEREO3D_DISABLE);
    }

    public static void writePreferredCamera3DMode(SharedPreferences pref,
            String camera3DMode) {
        Editor editor = pref.edit();
        editor.putString(KEY_STEREO3D_MODE, camera3DMode);
        editor.apply();
    }
}

//settingChecker.clearListPreference();
//settingChecker.setListPreference(SettingChecker.ROW_SETTING_FLASH, flashMode);
//settingChecker.setListPreference(SettingChecker.ROW_SETTING_DUAL_CAMERA, cameraIdPref);
//settingChecker.setListPreference(SettingChecker.ROW_SETTING_EXPOSURE, exposure);
//settingChecker.setListPreference(SettingChecker.ROW_SETTING_SCENCE_MODE, sceneMode);
//settingChecker.setListPreference(SettingChecker.ROW_SETTING_WHITE_BALANCE, whiteBalance);
//settingChecker.setListPreference(SettingChecker.ROW_SETTING_IMAGE_PROPERTIES, imageproperties);
//settingChecker.setListPreference(SettingChecker.ROW_SETTING_COLOR_EFFECT, colorEffect);
//settingChecker.setListPreference(SettingChecker.ROW_SETTING_ZSD, zsd);

//settingChecker.setListPreference(SettingChecker.ROW_SETTING_PICTURE_SIZE, pictureSize);
//settingChecker.setListPreference(SettingChecker.ROW_SETTING_ISO, iso);
//settingChecker.setListPreference(SettingChecker.ROW_SETTING_AE_METER = 13;
//settingChecker.setListPreference(SettingChecker.ROW_SETTING_ANTI_FLICKER, antiBanding);
//settingChecker.setListPreference(SettingChecker.ROW_SETTING_VIDEO_STABLE, eis);
/*if (!FeatureSwitcher.isHdRecordingEnabled()) {
settingChecker.setListPreference(SettingChecker.ROW_SETTING_AUDIO_MODE,
        group.findPreference(KEY_VIDEO_HD_AUDIO_RECORDING));
}*/
//settingChecker.setListPreference(SettingChecker.ROW_SETTING_LIVE_EFFECT, videoEffect);
//settingChecker.setListPreference(SettingChecker.ROW_SETTING_VIDEO_QUALITY, videoQuality);

//settingChecker.setListPreference(SettingChecker.ROW_SETTING_PICTURE_RATIO, pictureRatio);
//settingChecker.setListPreference(SettingChecker.ROW_SETTING_VOICE, voice);

//image adjustment
//settingChecker.setListPreference(SettingChecker.ROW_SETTING_SHARPNESS, edge);
//settingChecker.setListPreference(SettingChecker.ROW_SETTING_HUE, hue);
//settingChecker.setListPreference(SettingChecker.ROW_SETTING_SATURATION, sat);
//settingChecker.setListPreference(SettingChecker.ROW_SETTING_BRIGHTNESS, brightness);
//settingChecker.setListPreference(SettingChecker.ROW_SETTING_CONTRAST, contrast);

////not in preference or not same as preference, but should be set in parameter
//settingChecker.setListPreference(SettingChecker.ROW_SETTING_CAMERA_MODE = 40;//camera mode
//settingChecker.setListPreference(SettingChecker.ROW_SETTING_CAPTURE_MODE = 41;//not in preference
//settingChecker.setListPreference(SettingChecker.ROW_SETTING_CONTINUOUS_NUM = 42;//different from preference
//settingChecker.setListPreference(SettingChecker.ROW_SETTING_RECORDING_HINT = 43;//not in preference
//settingChecker.setListPreference(SettingChecker.ROW_SETTING_JPEG_QUALITY = 44;//not in preference
////not in preference and not in parameter
//settingChecker.setListPreference(SettingChecker.ROW_SETTING_STEREO_MODE = 45;

//settingChecker.setListPreference(SettingChecker.ROW_SETTING_FACEBEAUTY_PROPERTIES, facebeautyproperties);
// facebeauty adjustment
//settingChecker.setListPreference(SettingChecker.ROW_SETTING_FACEBEAUTY_SMOOTH, smooth);
//settingChecker.setListPreference(SettingChecker.ROW_SETTING_FACEBEAUTY_SKIN_COLOR, skinColor);
//settingChecker.setListPreference(SettingChecker.ROW_SETTING_FACEBEAUTY_SHARP, sharp);

/*List<String> supportCaptureModes = mParameters.getSupportedCaptureMode();
if (supportCaptureModes == null) {
    normalCapture = null;
    hdrCapMode = null;
}
if (normalCapture != null && 
        supportCaptureModes.indexOf(Parameters.CAPTURE_MODE_BURST_SHOT) != NOT_FOUND) {
    //Burst shot supported
    CharSequence values[] = normalCapture.getEntryValues();
    for (CharSequence value : values) {
        if (value.toString().startsWith(Parameters.CAPTURE_MODE_BURST_SHOT)) {
            supportCaptureModes.add(value.toString());
        }
    }
}
if (hdrCapMode != null) {
    if (supportCaptureModes.indexOf(Parameters.CAPTURE_MODE_HDR) == NOT_FOUND) {
        filterUnsupportedOptions(group, hdrCapMode, null);
    }
}
if (normalCapture != null) {
    filterUnsupportedOptions(group, normalCapture, supportCaptureModes);
}
if (continuousNumber != null && supportCaptureModes != null &&
        supportCaptureModes.indexOf(Parameters.CAPTURE_MODE_CONTINUOUS_SHOT) == NOT_FOUND) {
    filterUnsupportedOptions(group, continuousNumber, null);
}*/
/*if (videoSceneMode != null) {
    filterUnsupportedOptions(group, videoSceneMode, mParameters.getSupportedSceneModes());
}
if (videoDuration != null) {
    CamcorderProfile profile = CamcorderProfile.getMtk(CamcorderProfile.QUALITY_MTK_LOW);
    CharSequence[] entries = videoDuration.getEntries();
    int mmsIndex = entries.length > 0 ? entries.length - 1 : entries.length;
    if (profile != null) {
        // Modify video duration settings.
        // The first entry is for MMS video duration, and we need to fill
        // in the device-dependent value (in seconds).
        entries[mmsIndex] = String.format(Locale.ENGLISH, 
                entries[mmsIndex].toString(), profile.duration);
    } else {
        entries[mmsIndex] = String.format(Locale.ENGLISH, 
                entries[mmsIndex].toString(), 30);
    }
}*/
/*if (focusMode != null) {
if (mParameters.getMaxNumFocusAreas() == 0) {
    filterUnsupportedOptions(group,
            focusMode, mParameters.getSupportedFocusModes());
} else {
    // Remove the focus mode if we can use tap-to-focus.
    removePreference(group, focusMode.getKey());
}
}
if (videoFocusMode != null) {
filterUnsupportedOptions(group, videoFocusMode, mParameters.getSupportedFocusModes());
}
if (videoFlashMode != null) {
filterUnsupportedOptions(group,
        videoFlashMode, mParameters.getSupportedFlashModes());
}*/
