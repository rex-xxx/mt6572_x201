/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.systemui.statusbar.policy;

import com.android.systemui.R;
import com.mediatek.systemui.ext.DataType;

/**
 * M: This class define the Gemini constants of telephony icons.
 */
public class TelephonyIconsGemini {

    public static int[] getTelephonySignalStrengthIconList(int simColorId, boolean showSimIndicator) {
        if (showSimIndicator) {
            return TELEPHONY_SIGNAL_STRENGTH_WHITE;
        } else {
            return TELEPHONY_SIGNAL_STRENGTH[simColorId];
        }
    }

    private static final int[] TELEPHONY_SIGNAL_STRENGTH_WHITE = {
        R.drawable.stat_sys_gemini_signal_0,
        R.drawable.stat_sys_gemini_signal_1_white,
        R.drawable.stat_sys_gemini_signal_2_white,
        R.drawable.stat_sys_gemini_signal_3_white,
        R.drawable.stat_sys_gemini_signal_4_white };
    /** Signal level icons for normal. @{ */

    private static final int[][] TELEPHONY_SIGNAL_STRENGTH = {
        { R.drawable.stat_sys_gemini_signal_0,
          R.drawable.stat_sys_gemini_signal_1_blue,
          R.drawable.stat_sys_gemini_signal_2_blue,
          R.drawable.stat_sys_gemini_signal_3_blue,
          R.drawable.stat_sys_gemini_signal_4_blue },
        { R.drawable.stat_sys_gemini_signal_0,
          R.drawable.stat_sys_gemini_signal_1_orange,
          R.drawable.stat_sys_gemini_signal_2_orange,
          R.drawable.stat_sys_gemini_signal_3_orange,
          R.drawable.stat_sys_gemini_signal_4_orange },
        { R.drawable.stat_sys_gemini_signal_0,
          R.drawable.stat_sys_gemini_signal_1_green,
          R.drawable.stat_sys_gemini_signal_2_green,
          R.drawable.stat_sys_gemini_signal_3_green,
          R.drawable.stat_sys_gemini_signal_4_green },
        { R.drawable.stat_sys_gemini_signal_0,
          R.drawable.stat_sys_gemini_signal_1_purple,
          R.drawable.stat_sys_gemini_signal_2_purple,
          R.drawable.stat_sys_gemini_signal_3_purple,
          R.drawable.stat_sys_gemini_signal_4_purple }
    };

    /** Signal level icons for normal. @} */

    /** Data connection type icons for normal. @{ */

    static final int[] DATA_1X = {
        R.drawable.stat_sys_gemini_data_connected_1x_blue,
        R.drawable.stat_sys_gemini_data_connected_1x_orange,
        R.drawable.stat_sys_gemini_data_connected_1x_green,
        R.drawable.stat_sys_gemini_data_connected_1x_purple,
        R.drawable.stat_sys_gemini_data_connected_1x_white
    };
    
    static final int[] DATA_3G = {
        R.drawable.stat_sys_gemini_data_connected_3g_blue,
        R.drawable.stat_sys_gemini_data_connected_3g_orange,
        R.drawable.stat_sys_gemini_data_connected_3g_green,
        R.drawable.stat_sys_gemini_data_connected_3g_purple,
        R.drawable.stat_sys_gemini_data_connected_3g_white
    };
    
    static final int[] DATA_4G = {
        R.drawable.stat_sys_gemini_data_connected_4g_blue,
        R.drawable.stat_sys_gemini_data_connected_4g_orange,
        R.drawable.stat_sys_gemini_data_connected_4g_green,
        R.drawable.stat_sys_gemini_data_connected_4g_purple,
        R.drawable.stat_sys_gemini_data_connected_4g_white
    };
    
    static final int[] DATA_E = {
        R.drawable.stat_sys_gemini_data_connected_e_blue,
        R.drawable.stat_sys_gemini_data_connected_e_orange,
        R.drawable.stat_sys_gemini_data_connected_e_green,
        R.drawable.stat_sys_gemini_data_connected_e_purple,
        R.drawable.stat_sys_gemini_data_connected_e_white
    };
    
    static final int[] DATA_G = {
        R.drawable.stat_sys_gemini_data_connected_g_blue,
        R.drawable.stat_sys_gemini_data_connected_g_orange,
        R.drawable.stat_sys_gemini_data_connected_g_green,
        R.drawable.stat_sys_gemini_data_connected_g_purple,
        R.drawable.stat_sys_gemini_data_connected_g_white
    };
    
    static final int[] DATA_H = {
        R.drawable.stat_sys_gemini_data_connected_h_blue,
        R.drawable.stat_sys_gemini_data_connected_h_orange,
        R.drawable.stat_sys_gemini_data_connected_h_green,
        R.drawable.stat_sys_gemini_data_connected_h_purple,
        R.drawable.stat_sys_gemini_data_connected_h_white
    };

    static final int[] DATA_H_PLUS = {
        R.drawable.stat_sys_gemini_data_connected_h_plus_blue,
        R.drawable.stat_sys_gemini_data_connected_h_plus_orange,
        R.drawable.stat_sys_gemini_data_connected_h_plus_green,
        R.drawable.stat_sys_gemini_data_connected_h_plus_purple,
        R.drawable.stat_sys_gemini_data_connected_h_plus_white
    };
    
    static final int[][] DATA = {
        DATA_1X,
        DATA_3G,
        DATA_4G,
        DATA_E,
        DATA_G,
        DATA_H,
        DATA_H_PLUS
    };

    /** Data connection type icons for normal. @} */

    /** Data connection type icons for roaming. @{ */

    static final int[] DATA_1X_ROAM = {
        R.drawable.stat_sys_gemini_data_connected_1x_blue_roam,
        R.drawable.stat_sys_gemini_data_connected_1x_orange_roam,
        R.drawable.stat_sys_gemini_data_connected_1x_green_roam,
        R.drawable.stat_sys_gemini_data_connected_1x_purple_roam,
        R.drawable.stat_sys_gemini_data_connected_1x_white_roam
    };
    
    static final int[] DATA_3G_ROAM = {
        R.drawable.stat_sys_gemini_data_connected_3g_blue_roam,
        R.drawable.stat_sys_gemini_data_connected_3g_orange_roam,
        R.drawable.stat_sys_gemini_data_connected_3g_green_roam,
        R.drawable.stat_sys_gemini_data_connected_3g_purple_roam,
        R.drawable.stat_sys_gemini_data_connected_3g_white_roam
    };
    
    static final int[] DATA_4G_ROAM = {
        R.drawable.stat_sys_gemini_data_connected_4g_blue_roam,
        R.drawable.stat_sys_gemini_data_connected_4g_orange_roam,
        R.drawable.stat_sys_gemini_data_connected_4g_green_roam,
        R.drawable.stat_sys_gemini_data_connected_4g_purple_roam,
        R.drawable.stat_sys_gemini_data_connected_4g_white_roam
    };
    
    static final int[] DATA_E_ROAM = {
        R.drawable.stat_sys_gemini_data_connected_e_blue_roam,
        R.drawable.stat_sys_gemini_data_connected_e_orange_roam,
        R.drawable.stat_sys_gemini_data_connected_e_green_roam,
        R.drawable.stat_sys_gemini_data_connected_e_purple_roam,
        R.drawable.stat_sys_gemini_data_connected_e_white_roam
    };
    
    static final int[] DATA_G_ROAM = {
        R.drawable.stat_sys_gemini_data_connected_g_blue_roam,
        R.drawable.stat_sys_gemini_data_connected_g_orange_roam,
        R.drawable.stat_sys_gemini_data_connected_g_green_roam,
        R.drawable.stat_sys_gemini_data_connected_g_purple_roam,
        R.drawable.stat_sys_gemini_data_connected_g_white_roam
    };
    
    static final int[] DATA_H_ROAM = {
        R.drawable.stat_sys_gemini_data_connected_h_blue_roam,
        R.drawable.stat_sys_gemini_data_connected_h_orange_roam,
        R.drawable.stat_sys_gemini_data_connected_h_green_roam,
        R.drawable.stat_sys_gemini_data_connected_h_purple_roam,
        R.drawable.stat_sys_gemini_data_connected_h_white_roam
    };
    
    static final int[] DATA_H_PLUS_ROAM = {
        R.drawable.stat_sys_gemini_data_connected_h_plus_blue_roam,
        R.drawable.stat_sys_gemini_data_connected_h_plus_orange_roam,
        R.drawable.stat_sys_gemini_data_connected_h_plus_green_roam,
        R.drawable.stat_sys_gemini_data_connected_h_plus_purple_roam,
        R.drawable.stat_sys_gemini_data_connected_h_plus_white_roam
    };

    static final int[][] DATA_ROAM = {
        DATA_1X_ROAM,
        DATA_3G_ROAM,
        DATA_4G_ROAM,
        DATA_E_ROAM,
        DATA_G_ROAM,
        DATA_H_ROAM,
        DATA_H_PLUS_ROAM
    };

    /** Data connection type icons for roaming. @} */

    /** Roaming icons. @{ */

    static final int[] ROAMING = {
        R.drawable.stat_sys_gemini_data_connected_roam_blue,
        R.drawable.stat_sys_gemini_data_connected_roam_orange,
        R.drawable.stat_sys_gemini_data_connected_roam_green,
        R.drawable.stat_sys_gemini_data_connected_roam_purple,
        R.drawable.stat_sys_gemini_data_connected_roam_white
    };

    /** Roaming icons. @} */

    public static int[] getDataTypeIconListGemini(boolean roaming, DataType dataType) {
        int[] iconList = null;
        if (roaming) {
            iconList = DATA_ROAM[dataType.getTypeId()];
        } else {
            iconList = DATA[dataType.getTypeId()];
        }
        return iconList;
    }

}
