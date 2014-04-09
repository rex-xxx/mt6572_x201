package com.mediatek.mock.hardware;

import java.util.HashMap;

public class MiniFeatureTable {

    public static final int SETTING_NUM = 21;
    private static final HashMap<String, Integer> SCENE_INDEX = new HashMap<String, Integer>();
    static {
        SCENE_INDEX.put("normal", 0);
        SCENE_INDEX.put("portrait", 1);
        SCENE_INDEX.put("landscape", 2);
        SCENE_INDEX.put("night", 3);
        SCENE_INDEX.put("night-portrait", 4);
        SCENE_INDEX.put("theatre", 5);
        SCENE_INDEX.put("beach", 6);
        SCENE_INDEX.put("snow", 7);
        SCENE_INDEX.put("sunset", 8);
        SCENE_INDEX.put("steadyphoto", 9);
        SCENE_INDEX.put("fireworks", 10);
        SCENE_INDEX.put("sports", 11);
        SCENE_INDEX.put("party", 12);
        SCENE_INDEX.put("candlelight", 13);
        SCENE_INDEX.put("auto", 14);
    }
    private static final HashMap<String, Integer> KEY_INDEX = new HashMap<String, Integer>();
    static {
        KEY_INDEX.put("exposure-compensation", 0);
        KEY_INDEX.put("whitebalance", 1);
        KEY_INDEX.put("iso-speed", 2);
        KEY_INDEX.put("exposure-meter", 3);
        KEY_INDEX.put("edge", 4);
        KEY_INDEX.put("hue", 5);
        KEY_INDEX.put("saturation", 6);
        KEY_INDEX.put("brightness", 7);
        KEY_INDEX.put("contrast", 8);
    }
    private static final String[][] MATRIX_SCENE = {
        /*SETTING_EXPOSURE*/     {"0", "0", "0", "0", "0", "0", "1", "1", "0", "0", "0", "0", "0", "0", "0"},
        /*SETTING_WHITE_BALANCE*/{"auto", "auto", "daylight", "auto", "auto", "auto", "auto", "auto", "daylight", "auto", "auto", "auto", "auto", "incandescent", "auto"},
        /*SETTING_ISO*/          {"auto", "auto", "auto", "auto", "auto", "auto", "auto", "auto", "auto", "auto", "auto", "auto", "auto", "auto", "auto"},
        /*SETTING_AE_METER*/     {"center", "center", "center", "center", "center", "center", "center", "center", "center", "center", "center", "center", "center", "center", "center"},
        /*SETTING_EDGE*/         {"middle", "low", "high", "low", "low", "high", "high", "high", "high", "middle", "middle", "middle", "middle", "middle", "middle"},
        /*SETTING_HUE*/          {"middle", "middle", "middle", "middle", "middle", "middle", "middle", "middle", "middle", "middle", "middle", "middle", "middle", "middle", "middle"},
        /*SETTING_SATURATION*/   {"middle", "middle", "middle", "middle", "low", "middle", "middle", "middle", "middle", "middle", "middle", "middle", "middle", "middle", "middle"},
        /*SETTING_BRIGHTNESS*/   {"middle", "middle", "middle", "middle", "middle", "middle", "middle", "middle", "middle", "middle", "middle", "middle", "middle", "middle", "middle"},
        /*SETTING_CONTRAST*/     {"middle", "middle", "middle", "middle", "middle", "middle", "middle", "middle", "middle", "middle", "middle", "middle", "middle", "middle", "middle"}
    };
    public static final int SETTING_EXPOSURE = 0;
    public static final int SETTING_WHITE_BALANCE = 1;
    public static final int SETTING_ISO = 2;
    public static final int SETTING_AE_METER = 3;
    public static final int SETTING_EDGE = 4;
    public static final int SETTING_HUE = 5;
    public static final int SETTING_SATURATION = 6;
    public static final int SETTING_BRIGHTNESS = 7;
    public static final int SETTING_CONTRAST = 8;

    public static final int SCENE_NORMAL = 0;
    public static final int SCENE_PORTRAIT = 1;
    public static final int SCENE_LAND = 2;
    public static final int SCENE_NIGHT = 3;
    public static final int SCENE_NIGHTPORTRAIT = 4;
    public static final int SCENE_THEATRE = 4;
    public static final int SCENE_BEACH = 5;
    public static final int SCENE_SNOW = 6;
    public static final int SCENE_SUNSET = 7;
    public static final int SCENE_STEADY = 8;
    public static final int SCENE_FIREWORKS = 9;
    public static final int SCENE_SPORT = 10;
    public static final int SCENE_PARTT = 11;
    public static final int SCENE_CANDLE = 12;
    public static final int SCENE_AUTO = 13;
    /*
    {
                                                        //normal    portrait    land    night     nightport  theatre    beach     snow     sunset    steady   fireworks    spot    party    candle    auto
        MATRIX_SCENE[SETTING_EXPOSURE]      = new String[] {"0", "0", "0", "0", "0", "0", "1", "1", "0", "0", "0", "0", "0", "0", "0"};//2: ev
        MATRIX_SCENE[SETTING_WHITE_BALANCE] = new String[] {"auto", "auto", "daylight", "auto", "auto", "auto", "auto", "auto", "daylight", "auto", "auto", "auto", "auto", "incandescent"};//4: white balance
        MATRIX_SCENE[SETTING_ISO]           = new String[] {"auto", "auto", "auto", "auto", "auto", "auto", "auto", "auto", "auto", "auto", "auto", "auto", "auto", "auto", "auto"};//12: ISO
        MATRIX_SCENE[SETTING_AE_METER]      = new String[] {"center", "center", "center", "center", "center", "center", "center", "center", "center", "center", "center", "center", "center", "center", "center"};//13: AE meter
        MATRIX_SCENE[SETTING_EDGE]          = new String[] {"middle", "low", "high", "low", "low", "high", "high", "high", "high", "middle", "middle", "middle", "middle", "middle", "middle"};
        MATRIX_SCENE[SETTING_HUE]           = new String[] {"middle", "middle", "middle", "middle", "middle", "middle", "middle", "middle", "middle", "middle", "middle", "middle", "middle", "middle", "middle"};
        MATRIX_SCENE[SETTING_SATURATION]    = new String[] {"middle", "middle", "middle", "middle", "low", "middle", "middle", "middle", "middle", "middle", "middle", "middle", "middle", "middle", "middle"};
        MATRIX_SCENE[SETTING_BRIGHTNESS]    = new String[] {"middle", "middle", "middle", "middle", "middle", "middle", "middle", "middle", "middle", "middle", "middle", "middle", "middle", "middle", "middle"};
        MATRIX_SCENE[SETTING_CONTRAST]      = new String[] {"middle", "middle", "middle", "middle", "middle", "middle", "middle", "middle", "middle", "middle", "middle", "middle", "middle", "middle", "middle"};

    }
    */

    public MiniFeatureTable() {
        
    }

    public String getFixedParameters(int keyIndex, int sceneIndex) {
        if (keyIndex > SETTING_CONTRAST || keyIndex < SETTING_EXPOSURE) {
            throw new IllegalArgumentException("Please check keyIndex!");
        }
        if (sceneIndex > SCENE_AUTO || sceneIndex < SCENE_NORMAL) {
            throw new IllegalArgumentException("Please check sceneIndex!");
        }
        return MATRIX_SCENE[keyIndex][sceneIndex];
    }

    public static String pickItem(String key, String scene) {
        if (KEY_INDEX.get(key) == null
                || SCENE_INDEX.get(scene) == null) {
            throw new IllegalArgumentException("key or scene not found");
        }
        return MATRIX_SCENE[KEY_INDEX.get(key)][SCENE_INDEX.get(scene)];
    }
}
