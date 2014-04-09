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

package com.mediatek.oobe.basic.gemini;

import android.content.Context;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.telephony.TelephonyManager;

import com.mediatek.oobe.utils.OOBEConstants;

public class SubDefaultSimSettings extends SimManagement {
    private static final String TAG = "SubDefaultSimSettings";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        logd("-->onCreate()");
        super.onCreate(savedInstanceState);
        logd("<--onCreate()");
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        logd("-->onActivityCreated");
        super.onActivityCreated(savedInstanceState);

        boolean sIsVoiceCapable = true;
        TelephonyManager telephony = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        sIsVoiceCapable = (telephony != null && telephony.isVoiceCapable());
        logd("VoiceCapable :" + sIsVoiceCapable);

        PreferenceScreen screen = getPreferenceScreen();
        // hide not needed SIM info settings and general settings
        screen.removePreference(findPreference("sim_info"));
        screen.removePreference(findPreference("general_settings"));

        PreferenceGroup defaultGroup = (PreferenceGroup) findPreference("default_sim");
        Preference smsCallPref = findPreference("sms_sim_setting");
        Preference gprsCallPref = findPreference("gprs_sim_setting");
        Preference voiceCallPref = findPreference("voice_call_sim_setting");
        Preference videoCallPref = findPreference("video_call_sim_setting");

        if (sIsVoiceCapable) {
            if (voiceCallPref != null) {
                screen.addPreference(voiceCallPref);
            } else {
                loge("voiceCallPref is null");
            }
            if (videoCallPref != null) {
                screen.addPreference(videoCallPref);
            } else {
                loge("videoCallPref is null");
            }
        } else {
            screen.removePreference(findPreference("voice_call_sim_setting"));
            screen.removePreference(findPreference("video_call_sim_setting"));
            logd("3GDataSMS doesn't need voice and video");
        }

        if (smsCallPref != null) {
            screen.addPreference(smsCallPref);
        } else {
            loge("smsCallPref is null");
        }
        if (gprsCallPref != null) {
            screen.addPreference(gprsCallPref);
        } else {
            loge("gprsCallPref is null");
        }

        if (defaultGroup != null) {
            screen.removePreference(defaultGroup);
        } else {
            loge("defaultGroup is null");
        }
        logd("<--onActivityCreated");
    }

    @Override
    protected void updateSimState(int slotID, int state) {
        logd("updateSimState(), prevent default action for oobe");
        return;
    }

    @Override
    protected void updateSimInfo(long simID, int type) {
        logd("updateSimInfo(), prevent default action for oobe");
        return;
    }

    private void logd(String msg) {
        OOBEConstants.logd(TAG + " - " + msg);
    }

    private void loge(String msg) {
        OOBEConstants.loge(TAG + " - " + msg);
    }
}
