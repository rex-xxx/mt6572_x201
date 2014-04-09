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

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;

import com.mediatek.oobe.utils.OOBEConstants;
import com.mediatek.xlog.Xlog;

public class SubSimManagementSettings extends SimManagement {
    private static final String TAG = "SubSimManagementSettings";

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

        PreferenceScreen screen = getPreferenceScreen();
        // hide not needed default SIM settings and general settings
        screen.removePreference(findPreference("default_sim"));
        screen.removePreference(findPreference("general_settings"));

        PreferenceGroup simInfoCategory = (PreferenceGroup) findPreference("sim_info");
        SimInfoPreference infoPref = null;
        if (simInfoCategory != null) {
            int simCount = simInfoCategory.getPreferenceCount();
            Xlog.d(TAG, " SimInfoListCategory children count=" + simInfoCategory.getPreferenceCount());

            for (int i = simCount - 1; i >= 0; i--) {
                Xlog.d(TAG, "i=" + i);
                Preference pref = simInfoCategory.getPreference(i);
                if (pref instanceof SimInfoPreference) {
                    // hide check box
                    infoPref = (SimInfoPreference) pref;
                    infoPref.setNeedCheckBox(false);
                    screen.addPreference(pref);
                }

            }
            screen.removePreference(simInfoCategory);
        }
        logd("<--onActivityCreated");
    }

    // prevent default action of operating default SIM, not needed here
    @Override
    protected void initDefaultSimPreference() {
        logd("initDefaultSimPreference(), prevent default action for oobe");
        return;
    }

    @Override
    protected void updateDefaultSimState(int slotID, int state) {
        logd("updateDefaultSimState(), prevent default action for oobe");
        return;
    }

    @Override
    protected void updateDefaultSimInfo(long simID) {
        logd("updateDefaultSimInfo(), prevent default action for oobe");
        return;
    }

    protected void updateDefaultSimItemList(int type) {
        logd("updateDefaultSimItemList(1), prevent default action for oobe");
        return;
    }

    @Override
    protected void updateGprsSettings() {
        logd("updateGprsSettings(), prevent default action for oobe");
        return;
    }

    private void logd(String msg) {
        OOBEConstants.logd(TAG + "  " + msg);
    }

    private void loge(String msg) {
        OOBEConstants.loge(TAG + "  " + msg);
    }
}
