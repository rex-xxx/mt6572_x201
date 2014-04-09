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

package com.mediatek.phone.ext;

import android.content.Context;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.Telephony.SIMInfo;
import android.util.Log;

import com.android.internal.telephony.Phone;

import java.util.List;

public class SettingsExtension {

    private static final String LOG_TAG = "NetworkSettings";

    /**
     * 
     * @param prefSet 
     * @param mPreferredNetworkMode 
     * @param mButtonPreferencedNetworkMode 
     * @param mPreference3GSwitch 
     * @param mButtonPreferredGSMOnly 
     */
    public void customizeFeatureForOperator(PreferenceScreen prefSet,
            Preference mPreferredNetworkMode, Preference mButtonPreferencedNetworkMode,
            Preference mPreference3GSwitch, Preference mButtonPreferredGSMOnly) {
        prefSet.removePreference(mButtonPreferredGSMOnly);
    }

    /**
     * 
     * @param prefSet 
     * @param mPLMNPreference 
     */
    public void customizePLMNFeature(PreferenceScreen prefSet, Preference mPLMNPreference) {
    };

    /**
     * 
     * @param prefsc 
     * @param buttonPreferredNetworkMode 
     * @param buttonPreferredGSMOnly 
     * @param buttonPreferredNetworkModeEx 
     */
    public void removeNMMode(PreferenceScreen prefsc, Preference buttonPreferredNetworkMode,
            Preference buttonPreferredGSMOnly, Preference buttonPreferredNetworkModeEx) {
    }

    /**
     * 
     * @param prefsc 
     * @param carrierSelPref 
     * @param isShowPlmn 
     */
    public void removeNMOp(PreferenceScreen prefsc, Preference carrierSelPref, boolean isShowPlmn) {
    }

    /**
     * 
     * @param prefsc 
     * @param networkMode 
     */
    public void removeNMOpFor3GSwitch(PreferenceScreen prefsc, Preference networkMode) {
    }

    /**
     * 
     * @param phone 
     * @param simList 
     * @param targetClass 
     */
    public void removeNMOpForMultiSim(Phone phone, List<SIMInfo> simList, String targetClass) {
    }

    /**
     * 
     * @param dataEnable 
     * @param activity 
     * @return 
     */
    public boolean dataEnableReminder(boolean isCheckedBefore, PreferenceActivity activity) {
        return false;
    }

    public void log(String msg) {
        Log.d(LOG_TAG, msg);
    }

    /**
     * 
     * @param context 
     * @param res 
     */
    public void showWarningDlg(Context context, int res) {
        log("default to do nothing");
    }
    /**
     * 
     * @param buttonDataRoam data roaming checkbox pref
     * @param isEnabled true for enable 
     * Default not doing anything
     */
    public void disableDataRoaming(CheckBoxPreference buttonDataRoam,boolean isEnabled) {
        
    }
    /**
     * 
     * @param context Context
     * @param res string id
     * @return the summary
     */
    public String getRoamingSummary(Context context,int res) {
        String summary;
        summary = context.getString(res);
        Log.d(LOG_TAG,"Default setRoamingSummary with summary=" + summary);
        return summary;
    } 
}
