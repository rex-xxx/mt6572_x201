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

/// M: [SystemUI] Support "SIM Indicator".

public class TelephonyIcons {
    //***** Signal strength icons

    //GSM/UMTS
    static final int[][] TELEPHONY_SIGNAL_STRENGTH = {
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
    
    //white Signal for SIMIndicator
    public static final int[] TELEPHONY_SIGNAL_STRENGTH_WHITE = {
        R.drawable.stat_sys_gemini_signal_0,
        R.drawable.stat_sys_gemini_signal_1_white,
        R.drawable.stat_sys_gemini_signal_2_white,
        R.drawable.stat_sys_gemini_signal_3_white,
        R.drawable.stat_sys_gemini_signal_4_white 
    };

    static final int[][] QS_TELEPHONY_SIGNAL_STRENGTH = {
        { R.drawable.ic_qs_signal_0,
          R.drawable.ic_qs_signal_1,
          R.drawable.ic_qs_signal_2,
          R.drawable.ic_qs_signal_3,
          R.drawable.ic_qs_signal_4 },
        { R.drawable.ic_qs_signal_full_0,
          R.drawable.ic_qs_signal_full_1,
          R.drawable.ic_qs_signal_full_2,
          R.drawable.ic_qs_signal_full_3,
          R.drawable.ic_qs_signal_full_4 }
    };

    static final int[][] TELEPHONY_SIGNAL_STRENGTH_ROAMING = {
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

    static final int[][] DATA_SIGNAL_STRENGTH = TELEPHONY_SIGNAL_STRENGTH;

    //***** Data connection icons

    //GSM/UMTS
    static final int[][] DATA_G = {
          { R.drawable.stat_sys_gemini_data_connected_g_blue },
          { R.drawable.stat_sys_gemini_data_connected_g_orange },
          { R.drawable.stat_sys_gemini_data_connected_g_green },
          { R.drawable.stat_sys_gemini_data_connected_g_purple },
          { R.drawable.stat_sys_gemini_data_connected_g_white }
        };

    static final int[][] DATA_3G = {
          { R.drawable.stat_sys_gemini_data_connected_3g_blue },
          { R.drawable.stat_sys_gemini_data_connected_3g_orange },
          { R.drawable.stat_sys_gemini_data_connected_3g_green },
          { R.drawable.stat_sys_gemini_data_connected_3g_purple },
          { R.drawable.stat_sys_gemini_data_connected_3g_white }
        };

    static final int[][] DATA_E = {
          { R.drawable.stat_sys_gemini_data_connected_e_blue },
          { R.drawable.stat_sys_gemini_data_connected_e_orange },
          { R.drawable.stat_sys_gemini_data_connected_e_green },
          { R.drawable.stat_sys_gemini_data_connected_e_purple },
          { R.drawable.stat_sys_gemini_data_connected_e_white }
        };

    //3.5G
    static final int[][] DATA_H = {
          { R.drawable.stat_sys_gemini_data_connected_h_blue },
          { R.drawable.stat_sys_gemini_data_connected_h_orange },
          { R.drawable.stat_sys_gemini_data_connected_h_green },
          { R.drawable.stat_sys_gemini_data_connected_h_purple },
          { R.drawable.stat_sys_gemini_data_connected_h_white }
    };
    
    //3.5G
    static final int[][] DATA_H_PLUS = {
          { R.drawable.stat_sys_gemini_data_connected_h_plus_blue },
          { R.drawable.stat_sys_gemini_data_connected_h_plus_orange },
          { R.drawable.stat_sys_gemini_data_connected_h_plus_green },
          { R.drawable.stat_sys_gemini_data_connected_h_plus_purple },
          { R.drawable.stat_sys_gemini_data_connected_h_plus_white }
    };

    //CDMA
    // Use 3G icons for EVDO data and 1x icons for 1XRTT data
    static final int[][] DATA_1X = {
          { R.drawable.stat_sys_gemini_data_connected_1x_blue },
          { R.drawable.stat_sys_gemini_data_connected_1x_orange },
          { R.drawable.stat_sys_gemini_data_connected_1x_green },
          { R.drawable.stat_sys_gemini_data_connected_1x_purple },
          { R.drawable.stat_sys_gemini_data_connected_1x_white }
    };

    // LTE and eHRPD
    static final int[][] DATA_4G = {
          { R.drawable.stat_sys_gemini_data_connected_4g_blue },
          { R.drawable.stat_sys_gemini_data_connected_4g_orange },
          { R.drawable.stat_sys_gemini_data_connected_4g_green },
          { R.drawable.stat_sys_gemini_data_connected_4g_purple },
          { R.drawable.stat_sys_gemini_data_connected_4g_white }
        };

    /** Data connection type icons for roaming. @{ */
    /// M: Support Roam Data Icon both show.

    static final int[][] DATA_1X_ROAM = {
        { R.drawable.stat_sys_gemini_data_connected_1x_blue_roam },
        { R.drawable.stat_sys_gemini_data_connected_1x_orange_roam },
        { R.drawable.stat_sys_gemini_data_connected_1x_green_roam },
        { R.drawable.stat_sys_gemini_data_connected_1x_purple_roam },
        { R.drawable.stat_sys_gemini_data_connected_1x_white_roam }
    };
    
    static final int[][] DATA_3G_ROAM = {
        { R.drawable.stat_sys_gemini_data_connected_3g_blue_roam },
        { R.drawable.stat_sys_gemini_data_connected_3g_orange_roam },
        { R.drawable.stat_sys_gemini_data_connected_3g_green_roam },
        { R.drawable.stat_sys_gemini_data_connected_3g_purple_roam },
        { R.drawable.stat_sys_gemini_data_connected_3g_white_roam }
    };
    
    static final int[][] DATA_4G_ROAM = {
        { R.drawable.stat_sys_gemini_data_connected_4g_blue_roam },
        { R.drawable.stat_sys_gemini_data_connected_4g_orange_roam },
        { R.drawable.stat_sys_gemini_data_connected_4g_green_roam },
        { R.drawable.stat_sys_gemini_data_connected_4g_purple_roam },
        { R.drawable.stat_sys_gemini_data_connected_4g_white_roam }
    };
    
    static final int[][] DATA_E_ROAM = {
        { R.drawable.stat_sys_gemini_data_connected_e_blue_roam },
        { R.drawable.stat_sys_gemini_data_connected_e_orange_roam },
        { R.drawable.stat_sys_gemini_data_connected_e_green_roam },
        { R.drawable.stat_sys_gemini_data_connected_e_purple_roam },
        { R.drawable.stat_sys_gemini_data_connected_e_white_roam }
    };
    
    static final int[][] DATA_G_ROAM = {
        { R.drawable.stat_sys_gemini_data_connected_g_blue_roam },
        { R.drawable.stat_sys_gemini_data_connected_g_orange_roam },
        { R.drawable.stat_sys_gemini_data_connected_g_green_roam },
        { R.drawable.stat_sys_gemini_data_connected_g_purple_roam },
        { R.drawable.stat_sys_gemini_data_connected_g_white_roam }
    };
    
    static final int[][] DATA_H_ROAM = {
        { R.drawable.stat_sys_gemini_data_connected_h_blue_roam },
        { R.drawable.stat_sys_gemini_data_connected_h_orange_roam },
        { R.drawable.stat_sys_gemini_data_connected_h_green_roam },
        { R.drawable.stat_sys_gemini_data_connected_h_purple_roam },
        { R.drawable.stat_sys_gemini_data_connected_h_white_roam }
    };
    
    static final int[][] DATA_H_PLUS_ROAM = {
        { R.drawable.stat_sys_gemini_data_connected_h_plus_blue_roam },
        { R.drawable.stat_sys_gemini_data_connected_h_plus_orange_roam },
        { R.drawable.stat_sys_gemini_data_connected_h_plus_green_roam },
        { R.drawable.stat_sys_gemini_data_connected_h_plus_purple_roam },
        { R.drawable.stat_sys_gemini_data_connected_h_plus_white_roam }
    };

    /** Data connection type icons for roaming. }@ */

    /** Roaming icons. @{ */

    static final int[] ROAMING = {
        R.drawable.stat_sys_gemini_data_connected_roam_blue,
        R.drawable.stat_sys_gemini_data_connected_roam_orange,
        R.drawable.stat_sys_gemini_data_connected_roam_green,
        R.drawable.stat_sys_gemini_data_connected_roam_purple,
        R.drawable.stat_sys_gemini_data_connected_roam_white
    };

    /** Roaming icons. @} */
    
    /** Data activity type icons. @{ */

    static final int[][] DATA_ACTIVITY = {
        { 0 },
        { R.drawable.stat_sys_signal_in_blue,
          R.drawable.stat_sys_signal_in_orange,
          R.drawable.stat_sys_signal_in_green,
          R.drawable.stat_sys_signal_in_purple },
        { R.drawable.stat_sys_signal_out_blue,
          R.drawable.stat_sys_signal_out_orange,
          R.drawable.stat_sys_signal_out_green,
          R.drawable.stat_sys_signal_out_purple },
        { R.drawable.stat_sys_signal_inout_blue,
          R.drawable.stat_sys_signal_inout_orange,
          R.drawable.stat_sys_signal_inout_green,
          R.drawable.stat_sys_signal_inout_purple }
    };

    /** Data activity type icons. @} */
    
    /** Sim Background for SimIndicator. @{ */

    public static final int[] SIM_INDICATOR_BACKGROUND = {
        R.drawable.toolbar_light_on_blue,
        R.drawable.toolbar_light_on_orange,
        R.drawable.toolbar_light_on_green,
        R.drawable.toolbar_light_on_purple,
        R.drawable.toolbar_sim_sip_call_select,
        R.drawable.toolbar_sim_always_ask_select,
        R.drawable.toolbar_sim_auto_select
    };

    public static final int[] SIM_INDICATOR_BACKGROUND_GREY = {
        0, 0, 0, 0,
        R.drawable.toolbar_sim_sip_call_not_select,
        R.drawable.toolbar_sim_always_ask_not_select,
        R.drawable.toolbar_sim_auto_not_select
    };
    
    public static final int[] SIM_INDICATOR_BACKGROUND_NOTIFICATION = {
        R.drawable.stat_sys_sim_indicator_background_notification_blue,
        R.drawable.stat_sys_sim_indicator_background_notification_orange,
        R.drawable.stat_sys_sim_indicator_background_notification_green,
        R.drawable.stat_sys_sim_indicator_background_notification_purple
    };
    
    /** Sim Background for SimIndicator. @} */

}

