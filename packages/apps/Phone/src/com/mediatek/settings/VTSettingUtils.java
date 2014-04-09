/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.mediatek.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

import com.android.phone.PhoneGlobals;
import com.mediatek.vt.VTManager;

public class VTSettingUtils {
    
    private static final String LOG_TAG = "VTSettingUtils";
    private static final boolean DBG = true;// (PhoneApp.DBG_LEVEL >= 2);
    private static final boolean DBGEM = true;
    
    private static void log(String msg) {
        Log.d(LOG_TAG, msg);
    }

    public String mPicToReplaceLocal;
    public boolean mEnableBackCamera;
    public boolean mPeerBigger;
    public boolean mShowLocalMO;
    public boolean mRingOnlyOnce;
    public String mShowLocalMT;
    public VTEngineerModeValues mVTEngineerModeValues;
    
    public boolean mAutoDropBack;
    public boolean mToReplacePeer;
    public String mPicToReplacePeer;
    
    
    private static final VTSettingUtils VT_SETTINGS_UTILS = new VTSettingUtils();
    
    public static VTSettingUtils getInstance() {
        return VT_SETTINGS_UTILS;
    }
    
    private VTSettingUtils() {
        mVTEngineerModeValues = new VTEngineerModeValues();
        resetVTSettingToDefaultValue();
    }
    
    public void resetVTSettingToDefaultValue() {
        mPicToReplaceLocal = "0";
        mEnableBackCamera = true;
        mPeerBigger = true;
        mShowLocalMO = true;
        mShowLocalMT = "0";
        mRingOnlyOnce = false;
        mAutoDropBack = false;
        mToReplacePeer = true;
        mPicToReplacePeer = "0";
    }
    
    public void updateVTSettingState(int slotId) {
        
        if (DBG) {
            log("updateVTSettingState()...");
        }
        
        SharedPreferences sp = PhoneGlobals.getInstance().getApplicationContext()
            .getSharedPreferences("com.android.phone_preferences" , Context.MODE_PRIVATE);
        
        if (null == sp) {
            if (DBG) { 
                log("updateVTEngineerModeValues() : can not find 'com.android.phone_preferences'...");
            }
            return;
        }
        
        mPicToReplaceLocal = sp.getString("button_vt_replace_expand_key_" + slotId, "0");        
        mEnableBackCamera = sp.getBoolean("button_vt_enable_back_camera_key_" + slotId, true);
        mPeerBigger = sp.getBoolean("button_vt_peer_bigger_key_" + slotId, true);
        mShowLocalMO = sp.getBoolean("button_vt_mo_local_video_display_key_" + slotId, true);
        mShowLocalMT = sp.getString("button_vt_mt_local_video_display_key_" + slotId, "0");
        mRingOnlyOnce = sp.getBoolean("ring_only_once", true);
        mAutoDropBack = sp.getBoolean("button_vt_auto_dropback_key_" + slotId, false);
        mToReplacePeer = sp.getBoolean("button_vt_enable_peer_replace_key_" + slotId, true);
        mPicToReplacePeer = sp.getString("button_vt_replace_peer_expand_key_" + slotId, "0");
        
        if (DBG) {
            log(" - mPicToReplaceLocal = " + mPicToReplaceLocal);
        }
        if (DBG) {
            log(" - mEnableBackCamera = " + mEnableBackCamera);
        }
        if (DBG) {
            log(" - mPeerBigger = " + mPeerBigger);
        }
        if (DBG) {
            log(" - mShowLocalMO = " + mShowLocalMO);
        }
        if (DBG) {
            log(" - mShowLocalMT = " + mShowLocalMT);
        }
        if (DBG) {
            log(" - mAutoDropBack = " + mAutoDropBack);
        }
        if (DBG) {
            log(" - mRingOnlyOnce = " + mRingOnlyOnce);
        }
        if (DBG) {
            log(" - mToReplacePeer = " + mToReplacePeer);
        }
        if (DBG) {
            log(" - mPicToReplacePeer = " + mPicToReplacePeer);
        }
        if (DBG) {
            log("updateVTSettingState() : call VTManager.replacePeerVideoSettings() start !");
        }
        if (mPicToReplacePeer.equals(VTAdvancedSetting.SELECT_DEFAULT_PICTURE2)) {
            if (mToReplacePeer) {
                VTManager.getInstance().replacePeerVideoSettings(1, VTAdvancedSetting.getPicPathDefault2());
            } else {
                VTManager.getInstance().replacePeerVideoSettings(0, VTAdvancedSetting.getPicPathDefault2());
            }
        } else {
            if (mToReplacePeer) {
                VTManager.getInstance().replacePeerVideoSettings(1, VTAdvancedSetting.getPicPathUserselect2(slotId));
            } else {
                VTManager.getInstance().replacePeerVideoSettings(0, VTAdvancedSetting.getPicPathUserselect2(slotId));
            }
        }
        if (DBG) {
            log("updateVTSettingState() : call VTManager.replacePeerVideoSettings() end !");
        }
    }
    
    public class VTEngineerModeValues {
        public String working_mode;
        public String working_mode_detail;
        public String config_audio_channel_adapt;
        public String config_video_channel_adapt;
        public String config_video_channel_reverse;
        public String config_multiplex_level;
        public String config_video_codec_preference;
        public String config_use_wnsrp;
        public String config_terminal_type;
        public boolean auto_answer;
        public String auto_answer_time;
        public boolean peer_audio_recoder_service;
        public String peer_audio_recoder_format;
        public boolean peer_video_recoder_service;
        public String peer_video_recoder_format;
        public boolean debug_message;
        public boolean h223_raw_data;
        public boolean log_to_file;
        public boolean h263_only;
                
        public int log_filter_tag_0_value;
        public int log_filter_tag_1_value;
        public int log_filter_tag_2_value;
        public int log_filter_tag_3_value;
        public int log_filter_tag_4_value;
        public int log_filter_tag_5_value;
        public int log_filter_tag_6_value;
 
        
        public VTEngineerModeValues() {
            resetValuesToDefault();
        }
        
        public void resetValuesToDefault() {
            working_mode = "0";
            working_mode_detail = "0";
            config_audio_channel_adapt = "0";
            config_video_channel_adapt = "0";
            config_video_channel_reverse = "0";
            config_multiplex_level = "0";
            config_video_codec_preference = "0";
            config_use_wnsrp = "0";
            config_terminal_type = "0";
            auto_answer = false;
            auto_answer_time = "0";
            peer_audio_recoder_service = false;
            peer_audio_recoder_format = "0";
            peer_video_recoder_service = false;
            peer_video_recoder_format = "0";
            debug_message = false;
            h223_raw_data = false;
            log_to_file = false;
            h263_only = false;
            
            log_filter_tag_0_value = 24;
            log_filter_tag_1_value = 24;
            log_filter_tag_2_value = 24;
            log_filter_tag_3_value = 24;
            log_filter_tag_4_value = 24;
            log_filter_tag_5_value = 24;
            log_filter_tag_6_value = 24;
        }
    } 
    
    //update the VT Engineer Mode values and set them to VTManager
    public void updateVTEngineerModeValues() {
        if (DBGEM) {
            log("updateVTEngineerModeValues()...");
        }
        
        Context emContext = null;
        try {
            emContext = PhoneGlobals.getInstance().createPackageContext("com.mediatek.engineermode",
                                                                    Context.CONTEXT_INCLUDE_CODE);
        } catch (NameNotFoundException e) {
            if (DBGEM) {
                log("updateVTEngineerModeValues() : can not find 'com.mediatek.engineermode'...");
            }
            return;
        }
        
        SharedPreferences sp = emContext.getSharedPreferences("engineermode_vt_preferences",
                                                              Context.MODE_WORLD_READABLE);
        
        if (null == sp) {
            if (DBGEM) {
                log("updateVTEngineerModeValues() : can not find 'engineermode_vt_preferences'...");
            }
            return;
        }
        
        mVTEngineerModeValues.working_mode = sp.getString("working_mode", "0");
        mVTEngineerModeValues.working_mode_detail = sp.getString("working_mode_detail", "0");
        mVTEngineerModeValues.config_audio_channel_adapt = sp.getString("config_audio_channel_adapt", "0");
        mVTEngineerModeValues.config_video_channel_adapt = sp.getString("config_video_channel_adapt", "0");
        mVTEngineerModeValues.config_video_channel_reverse = sp.getString("config_video_channel_reverse", "0");
        mVTEngineerModeValues.config_multiplex_level = sp.getString("config_multiplex_level", "0");
        mVTEngineerModeValues.config_video_codec_preference = sp.getString("config_video_codec_preference", "0");
        mVTEngineerModeValues.config_use_wnsrp = sp.getString("config_use_wnsrp", "0");
        mVTEngineerModeValues.config_terminal_type = sp.getString("config_terminal_type", "0");
        mVTEngineerModeValues.auto_answer = sp.getBoolean("auto_answer", false);
        mVTEngineerModeValues.auto_answer_time = sp.getString("auto_answer_time", "0");        
        mVTEngineerModeValues.peer_audio_recoder_service = sp.getBoolean("peer_audio_recoder_service", false);
        mVTEngineerModeValues.peer_audio_recoder_format = sp.getString("peer_audio_recoder_format", "0");
        mVTEngineerModeValues.peer_video_recoder_service = sp.getBoolean("peer_video_recoder_service", false);
        mVTEngineerModeValues.peer_video_recoder_format = sp.getString("peer_video_recoder_format", "0");
        mVTEngineerModeValues.debug_message = sp.getBoolean("debug_message", false);
        mVTEngineerModeValues.h223_raw_data = sp.getBoolean("h223_raw_data", false);    
        mVTEngineerModeValues.log_to_file = sp.getBoolean("log_to_file", false); 
        mVTEngineerModeValues.h263_only = sp.getBoolean("h263_only", false);
            
        mVTEngineerModeValues.log_filter_tag_0_value = sp.getInt("log_filter_tag_0_value", 24);
        mVTEngineerModeValues.log_filter_tag_1_value = sp.getInt("log_filter_tag_1_value", 24);
        mVTEngineerModeValues.log_filter_tag_2_value = sp.getInt("log_filter_tag_2_value", 24);
        mVTEngineerModeValues.log_filter_tag_3_value = sp.getInt("log_filter_tag_3_value", 24);
        mVTEngineerModeValues.log_filter_tag_4_value = sp.getInt("log_filter_tag_4_value", 24);
        mVTEngineerModeValues.log_filter_tag_5_value = sp.getInt("log_filter_tag_5_value", 24);
        mVTEngineerModeValues.log_filter_tag_6_value = sp.getInt("log_filter_tag_6_value", 24);
        
        if (DBGEM) {
            log(" - mVTEngineerModeValues.working_mode = " + mVTEngineerModeValues.working_mode);
        }
        if (DBGEM) {
            log(" - mVTEngineerModeValues.working_mode_detail = " + mVTEngineerModeValues.working_mode_detail);
        }
        if (DBGEM) {
            log(" - mVTEngineerModeValues.config_audio_channel_adapt = " + mVTEngineerModeValues.config_audio_channel_adapt);
        }
        if (DBGEM) {
            log(" - mVTEngineerModeValues.config_video_channel_adapt = " + mVTEngineerModeValues.config_video_channel_adapt);
        }
        if (DBGEM) {
            log(" - mVTEngineerModeValues.config_video_channel_reverse = " 
                    + mVTEngineerModeValues.config_video_channel_reverse);
        }
        if (DBGEM) {
            log(" - mVTEngineerModeValues.config_multiplex_level = " + mVTEngineerModeValues.config_multiplex_level);
        }
        if (DBGEM) {
            log(" - mVTEngineerModeValues.config_video_codec_preference = " 
                    + mVTEngineerModeValues.config_video_codec_preference);
        }
        if (DBGEM) {
            log(" - mVTEngineerModeValues.config_use_wnsrp = " + mVTEngineerModeValues.config_use_wnsrp);
        }
        if (DBGEM) {
            log(" - mVTEngineerModeValues.config_terminal_type = " + mVTEngineerModeValues.config_terminal_type);
        }
        if (DBGEM) {
            log(" - mVTEngineerModeValues.auto_answer = " + mVTEngineerModeValues.auto_answer);
        }
        if (DBGEM) {
            log(" - mVTEngineerModeValues.auto_answer_time = " + mVTEngineerModeValues.auto_answer_time);
        }
        if (DBGEM) {
            log(" - mVTEngineerModeValues.peer_audio_recoder_service = " + mVTEngineerModeValues.peer_audio_recoder_service);
        }
        if (DBGEM) {
            log(" - mVTEngineerModeValues.peer_audio_recoder_format = " + mVTEngineerModeValues.peer_audio_recoder_format);
        }
        if (DBGEM) {
            log(" - mVTEngineerModeValues.peer_video_recoder_service = " + mVTEngineerModeValues.peer_video_recoder_service);
        }
        if (DBGEM) {
            log(" - mVTEngineerModeValues.peer_video_recoder_format = " + mVTEngineerModeValues.peer_video_recoder_format);
        }
        if (DBGEM) {
            log(" - mVTEngineerModeValues.debug_message = " + mVTEngineerModeValues.debug_message);
        }
        if (DBGEM) {
            log(" - mVTEngineerModeValues.h223_raw_data = " + mVTEngineerModeValues.h223_raw_data);
        }
        if (DBGEM) {
            log(" - mVTEngineerModeValues.log_to_file = " + mVTEngineerModeValues.log_to_file);
        }
        if (DBGEM) {
            log(" - mVTEngineerModeValues.h263_only = " + mVTEngineerModeValues.h263_only);
        }
        if (DBGEM) {
            log(" - mVTEngineerModeValues.log_filter_tag_0_value = " + mVTEngineerModeValues.log_filter_tag_0_value);
        }
        if (DBGEM) {
            log(" - mVTEngineerModeValues.log_filter_tag_1_value = " + mVTEngineerModeValues.log_filter_tag_1_value);
        }
        if (DBGEM) {
            log(" - mVTEngineerModeValues.log_filter_tag_2_value = " + mVTEngineerModeValues.log_filter_tag_2_value);
        }
        if (DBGEM) {
            log(" - mVTEngineerModeValues.log_filter_tag_3_value = " + mVTEngineerModeValues.log_filter_tag_3_value);
        }
        if (DBGEM) {
            log(" - mVTEngineerModeValues.log_filter_tag_4_value = " + mVTEngineerModeValues.log_filter_tag_4_value);
        }
        if (DBGEM) {
            log(" - mVTEngineerModeValues.log_filter_tag_5_value = " + mVTEngineerModeValues.log_filter_tag_5_value);
        }
        if (DBGEM) {
            log(" - mVTEngineerModeValues.log_filter_tag_6_value = " + mVTEngineerModeValues.log_filter_tag_6_value);
        }
        
        VTManager.setEM(0, new Integer(mVTEngineerModeValues.working_mode).intValue(), 
                        new Integer(mVTEngineerModeValues.working_mode_detail).intValue());
        VTManager.setEM(1, 0, new Integer(mVTEngineerModeValues.config_audio_channel_adapt).intValue());
        VTManager.setEM(1, 1, new Integer(mVTEngineerModeValues.config_video_channel_adapt).intValue());
        VTManager.setEM(1, 2, new Integer(mVTEngineerModeValues.config_video_channel_reverse).intValue());
        VTManager.setEM(1, 3, new Integer(mVTEngineerModeValues.config_multiplex_level).intValue());
        VTManager.setEM(1, 4, new Integer(mVTEngineerModeValues.config_video_codec_preference).intValue());
        VTManager.setEM(1, 5, new Integer(mVTEngineerModeValues.config_use_wnsrp).intValue());
        VTManager.setEM(1, 6, new Integer(mVTEngineerModeValues.config_terminal_type).intValue());

        if (mVTEngineerModeValues.peer_audio_recoder_service) {
            VTManager.setEM(3, 0, 1);
        } else {
            VTManager.setEM(3, 0, 0);
        }
        VTManager.setEM(3, 1, new Integer(mVTEngineerModeValues.peer_audio_recoder_format).intValue());

        if (mVTEngineerModeValues.peer_video_recoder_service) {
            VTManager.setEM(4, 0, 1);
        } else {
            VTManager.setEM(4, 0, 0);
        }
        VTManager.setEM(4, 1, new Integer(mVTEngineerModeValues.peer_video_recoder_format).intValue());

        if (mVTEngineerModeValues.debug_message) {
            VTManager.setEM(5, 1, 0);
        } else {
            VTManager.setEM(5, 0, 0);
        }

        if (mVTEngineerModeValues.h223_raw_data) {
            VTManager.setEM(6, 1, 0);
        } else {
            VTManager.setEM(6, 0, 0);
        }

        if (mVTEngineerModeValues.log_to_file) {
            VTManager.setEM(7, 1, 0);
        } else {
            VTManager.setEM(7, 0, 0);
        }
        
        VTManager.setEM(8, 0, mVTEngineerModeValues.log_filter_tag_0_value);
        VTManager.setEM(8, 1, mVTEngineerModeValues.log_filter_tag_1_value);
        VTManager.setEM(8, 2, mVTEngineerModeValues.log_filter_tag_2_value);
        VTManager.setEM(8, 3, mVTEngineerModeValues.log_filter_tag_3_value);
        VTManager.setEM(8, 4, mVTEngineerModeValues.log_filter_tag_4_value);
        VTManager.setEM(8, 5, mVTEngineerModeValues.log_filter_tag_5_value);
        VTManager.setEM(8, 6, mVTEngineerModeValues.log_filter_tag_6_value);
        
        if (mVTEngineerModeValues.h263_only) {
            VTManager.setEM(9, 1, 0);
        } else {
            VTManager.setEM(9, 0, 0);
        }
    }
}
