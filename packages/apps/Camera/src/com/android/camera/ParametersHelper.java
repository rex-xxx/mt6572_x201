package com.android.camera;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.hardware.Camera.Parameters;
import android.text.TextUtils;

public class ParametersHelper {
    private static final String TAG = "ParametersHelper";
    private static final boolean LOG = Log.LOGV;

    public static final int FACEBEAUTY_SMOOTH = 0;
    public static final int FACEBEAUTY_SKIN_COLOR = 1;
    public static final int FACEBEAUTY_SHARP = 2;
    public static final String KEY_RECORDING_HINT = "recording-hint";
    public static final String KEY_FACEBEAUTY_SMOOTH_MAX = "fb-smooth-level-max";
    public static final String KEY_FACEBEAUTY_SMOOTH_MIN = "fb-smooth-level-min";
    public static final String KEY_FACEBEAUTY_SMOOTH = "fb-smooth-level";
    public static final String KEY_FACEBEAUTY_SKIN_COLOR_MAX = "fb-skin-color-max";
    public static final String KEY_FACEBEAUTY_SKIN_COLOR_MIN = "fb-skin-color-min";
    public static final String KEY_FACEBEAUTY_SKIN_COLOR = "fb-skin-color";
    public static final String KEY_FACEBEAUTY_SHARP_MAX = "fb-sharp-max";
    public static final String KEY_FACEBEAUTY_SHARP_MIN = "fb-sharp-min";
    public static final String KEY_FACEBEAUTY_SHARP = "fb-sharp";
    private static final String SUPPORTED_VALUES_SUFFIX = "-values";
    
    
    public static boolean isFaceBeautySupported(Parameters parameters) {
        if (parameters != null) {
            List<String> supported = parameters.getSupportedCaptureMode();
            return (supported.indexOf(Parameters.CAPTURE_MODE_FB) >= 0);
        } else {
            throw new RuntimeException("(ParametersHelper)why parameters is null?");
        }
    }

    public static boolean isSupportedSmooth(Parameters parameters) {
        int max = getMaxLevel(parameters, FACEBEAUTY_SMOOTH);
        int min = getMinLevel(parameters, FACEBEAUTY_SMOOTH);
        return max != 0 || min != 0;
    }

    public static int getMaxLevel(Parameters parameters, int key) {
        switch (key) {
            case FACEBEAUTY_SMOOTH:
                return getInt(parameters, KEY_FACEBEAUTY_SMOOTH_MAX, 0);
            case FACEBEAUTY_SKIN_COLOR:
                return getInt(parameters, KEY_FACEBEAUTY_SKIN_COLOR_MAX, 0);
            case FACEBEAUTY_SHARP:
                return getInt(parameters, KEY_FACEBEAUTY_SHARP_MAX, 0);
            default:
                return 0;
        }
    }

    public static int getMinLevel(Parameters parameters, int key) {
        switch (key) {
            case FACEBEAUTY_SMOOTH:
                return getInt(parameters, KEY_FACEBEAUTY_SMOOTH_MIN, 0);
            case FACEBEAUTY_SKIN_COLOR:
                return getInt(parameters, KEY_FACEBEAUTY_SKIN_COLOR_MIN, 0);
            case FACEBEAUTY_SHARP:
                return getInt(parameters, KEY_FACEBEAUTY_SHARP_MIN, 0);
            default:
                return 0;
        }
    }
//
//    public static int getMaxSmoothLevel(Parameters parameters) {
//        return getInt(parameters, KEY_FACEBEAUTY_SMOOTH_MAX, 0);
//    }
//
//    public static int getMinSmoothLevel(Parameters parameters) {
//        return getInt(parameters, KEY_FACEBEAUTY_SMOOTH_MIN, 0);
//    }
//
//    public static int getMaxSkinColor(Parameters parameters) {
//        return getInt(parameters, KEY_FACEBEAUTY_SKIN_COLOR_MAX, 0);
//    }
//
//    public static int getMinSkinColor(Parameters parameters) {
//        return getInt(parameters, KEY_FACEBEAUTY_SKIN_COLOR_MIN, 0);
//    }
//
//    public static int getMaxSharp(Parameters parameters) {
//        return getInt(parameters, KEY_FACEBEAUTY_SHARP_MAX, 0);
//    }
//
//    public static int getMinSharp(Parameters parameters) {
//        return getInt(parameters, KEY_FACEBEAUTY_SHARP_MIN, 0);
//    }

    /* MR1 put HDR in scene mode. So, here we don't put it into user list.
     * In apply logic, HDR will set in scene mode and show auto scene to final user.
     * If scene mode not find in ListPreference, first one(auto) will be choose.
     * I don't think this is a good design.
     */
    public static final String KEY_SCENE_MODE_HDR = "hdr";
    //special scene mode for operator, like auto.
    public static final String KEY_SCENE_MODE_NORMAL = "normal";
    public static final String ZSD_MODE_OFF = "off";
    
    //Copied from android.hardware.Camera
    // Splits a comma delimited string to an ArrayList of String.
    // Return null if the passing string is null or the size is 0.
    public static ArrayList<String> split(String str) {
        ArrayList<String> substrings = null;
        if (str != null) {
            TextUtils.StringSplitter splitter = new TextUtils.SimpleStringSplitter(',');
            splitter.setString(str);
            substrings = new ArrayList<String>();
            for (String s : splitter) {
                substrings.add(s);
            }
        }
        if (LOG) {
            Log.v(TAG, "split(" + str + ") return " + substrings);
        }
        return substrings;
    }
    
    public static List<String> getSupportedValues(Parameters parameters, String key) {
        List<String> supportedList = null;
        if (parameters != null) {
            String str = parameters.get(key + SUPPORTED_VALUES_SUFFIX);
            supportedList = split(str);
        }
        return supportedList;
    }

    // Returns the value of a integer parameter.
    public static int getInt(Parameters parameters, String key, int defaultValue) {
        if (parameters != null) {
            try {
                return Integer.parseInt(parameters.get(key));
            } catch (NumberFormatException ex) {
                return defaultValue;
            }
        } else {
            return defaultValue;
        }
    }
}
