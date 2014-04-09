/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 * 
 * MediaTek Inc. (C) 2010. All rights reserved.
 * 
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package com.mediatek.oobe.utils;

import com.mediatek.xlog.Xlog;

public class OOBEConstants {
    public static final String TAG = "OOBE";
    public static final boolean DEBUG = false;
    // status of activity returning
    public static final int RESULT_CODE_NEXT = 20;
    public static final int RESULT_CODE_BACK = 21;
    public static final int RESULT_CODE_FINISH = 22;
    // public static final int RESULT_CODE_CLOSE = 23;
    public static final int RESULT_CODE_SKIP = 24;
    public static final int RESULT_QUICK_START_GUIDE_FINISH = 25;
    public static final int ID_LANGUAGE_SETTING = 1;
    public static final int ID_KEYBOARD_SETTING = 2;
    public static final int ID_SIM_MANAGEMENT_SETTING = 3;
    public static final int ID_DEFAULT_SIM_SETTING = 4;
    public static final int ID_IMPORT_CONTACTS = 5;
    public static final int ID_INTERNET_CONNECTION = 6;
    public static final int ID_WIFI_SETTING = 7;
    public static final int ID_DATE_TIME_SETTING = 8;
    public static final int ID_BASIC_CONGRATULATION = 9;

    public static final String OOBE_HAS_RUN_KEY = "oobe_has_run";
    public static final String ACTION_LANGUAGE_SETTING = "com.mediatek.oobe.basic.OOBE_LANGUAGE_SETTING";
    public static final String ACTION_KEYBOARD_SETTING = "com.mediatek.oobe.basic.OOBE_KEYBOARD_SETTING";
    // public static final String ACTION_SIM_MANAGEMENT = "com.android.settings.SIM_MANAGEMENT_SETTINGS_WIZARD";
    // public static final String ACTION_DEFAULT_SIM = "com.android.settings.DEFAULT_SIM_SETTINGS_WIZARD";
    public static final String ACTION_SIM_MANAGEMENT = "com.mediatek.oobe.basic.SIM_MANAGEMENT_SETTINGS_WIZARD";
    public static final String ACTION_DEFAULT_SIM = "com.mediatek.oobe.basic.DEFAULT_SIM_SETTINGS_WIZARD";
    public static final String ACTION_IMPORT_CONTACTS = "com.mediatek.oobe.basic.OOBE_IMPORT_CONTACTS";
    public static final String ACTION_INTERNET_CONNECTION = "com.mediatek.oobe.basic.OOBE_INTERNET_CONNECTION";
    // public static final String ACTION_WIFI_SETTING = "com.android.settings.WIFI_SETTINGS_WIZARD";
    public static final String ACTION_DATE_TIME_SETTING = "com.android.settings.DATE_TIME_SETTINGS_WIZARD";
    public static final String ACTION_WIFI_SETTING = "com.mediatek.oobe.basic.WIFI_SETTINGS_WIZARD";
    // public static final String ACTION_DATE_TIME_SETTING = "com.mediatek.oobe.basic.DATE_TIME_SETTINGS_WIZARD";
    public static final String ACTION_TIMEZONE_PICKER = "com.mediatek.oobe.basic.TIMEZONE_PICKER_WIZARD";
    public static final String ACTION_SNS_SETTING = "com.mediatek.oobe.advanced.SNSSettings";

    public static final String ACTION_ADVANCED_SETTINGS = "com.mediatek.oobe.advanced.AdvanceSettings";
    public static final String ACTION_QUICK_START_GUIDE = "com.mediatek.oobe.QUICK_START_GUIDE";

    // parameter for activities
    public static final String OOBE_BASIC_STEP_TOTAL = "oobe_step_total";
    public static final String OOBE_BASIC_STEP_INDEX = "oobe_step_index";
    public static final String OOBE_BASIC_RESULT = "oobe_result";

    // whether left and right fling gesture will take effect
    public static final boolean WITH_GESTURE = true;

    /** 
     * log for information
     * @param message a message String
     */
    public static void logi(String message) {
        if (DEBUG) {
            Xlog.i(TAG, message);
        }
    }

    /**
     * log for debug
     * @param message debug message string
     */
    public static void logd(String message) {
        if (DEBUG) {
            Xlog.d(TAG, message);
        }
    }

    /**
     * log for error 
     * @param message String
     */
    public static void loge(String message) {
        Xlog.e(TAG, message);
    }

    /**
     * log for warning
     * @param message String
     */
    public static void logw(String message) {
        if (DEBUG) {
            Xlog.w(TAG, message);
        }
    }
}