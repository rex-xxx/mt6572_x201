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

package com.mediatek.engineermode;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.SystemProperties;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.telephony.TelephonyManager;

import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.xlog.Xlog;
import com.mediatek.nfc.dynamicload.NativeDynamicLoad;
import java.io.File;

public class PrefsFragment extends PreferenceFragment {

    private static final String TAG = "EM/PrefsFragment";
    private static final int FRAGMENT_RES[] = { R.xml.telephony,
            R.xml.connectivity, R.xml.hardware_testing, R.xml.location,
            R.xml.log_and_debugging, R.xml.others, };
    private static final String INNER_LOAD_INDICATOR_FILE = 
    	"/system/etc/system_update/address.xml";

    private int mXmlResId;
    private static final int MTK_NFC_CHIP_TYPE_MSR3110 = 0x01;
    private static final int MTK_NFC_CHIP_TYPE_MT6605 = 0x02;
    
    /**
     * Default empty constructor
     */
    public PrefsFragment() {

    }

    /**
     * Set this fragment resource
     * 
     * @param resIndex
     *            Resource ID
     */
    public void setResource(int resIndex) {
        mXmlResId = resIndex;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load preferences from xml.
        addPreferencesFromResource(FRAGMENT_RES[mXmlResId]);
        PreferenceScreen screen = getPreferenceScreen();

        if (!FeatureOption.MTK_DT_SUPPORT) {
            removePreference(screen, "dualtalk_network_info");
            removePreference(screen, "dualtalk_bandmode");
        }

        // Duplicate with Network Selecting, remove them
        removePreference(screen, "digital_standard");
        if (ModemCategory.getModemType() == ModemCategory.MODEM_TD) {
            removePreference(screen, "rat_mode");
        }

        if (!FeatureOption.MTK_DT_SUPPORT) {
            removePreference(screen, "dualtalk_network_select");
        } else {
            removePreference(screen, "network_select");
        }

        // if
        // (NfcAdapter.getDefaultAdapter(getActivity().getApplicationContext())
        // == null) {
        // removePreference(screen, "nfc");
        // }
        // it's ok

        switch(NativeDynamicLoad.queryVersion()) {
            case MTK_NFC_CHIP_TYPE_MSR3110 :
                Xlog.i(TAG, "MSR3110 nfc chip, call nfc");
                removePreference(screen, "hqanfc");
                break;
            case MTK_NFC_CHIP_TYPE_MT6605 :
                Xlog.i(TAG, "MT6606 nfc chip, call hqanfc");
                removePreference(screen, "nfc");
                break;
            default:
                Xlog.i(TAG, "no nfc chip support");
                removePreference(screen, "hqanfc");
                removePreference(screen, "nfc");
                break;
        }

        /*if (!FeatureOption.MTK_LOG2SERVER_APP) {
            removePreference(screen, "log2server");
        }*/

        if (FeatureOption.EVDO_DT_SUPPORT || !FeatureOption.MTK_DT_SUPPORT
                || SystemProperties.getInt("ril.external.md", 0) == 0) {
            removePreference(screen, "ext_md_logger");
        }

        if (!FeatureOption.MTK_SMSREG_APP) {
            removePreference(screen, "device_manager");
        }

        if (FeatureOption.MTK_BSP_PACKAGE) {
            removePreference(screen, "auto_answer");
        }

        if (!FeatureOption.HAVE_CMMB_FEATURE) {
            removePreference(screen, "cmmb");
        }

        if (ChipSupport.isFeatureSupported(ChipSupport.MTK_FM_SUPPORT)) {
            if (!ChipSupport.isFeatureSupported(ChipSupport.MTK_FM_TX_SUPPORT)) {
                removePreference(screen, "fm_transmitter");
            }
        } else {
            removePreference(screen, "fm_receiver");
            removePreference(screen, "fm_transmitter");
        }

        // AGPS is not ready if MTK_AGPS_APP isn't defined
        if (!ChipSupport.isFeatureSupported(ChipSupport.MTK_AGPS_APP)
                || !ChipSupport.isFeatureSupported(ChipSupport.MTK_GPS_SUPPORT)) {
            removePreference(screen, "location_basedservice");
        }

        if (!ChipSupport.isFeatureSupported(ChipSupport.MTK_GPS_SUPPORT)) {
            removePreference(screen, "ygps");
        }

        // MATV is not ready if HAVE_MATV_FEATURE isn't defined
        if (!ChipSupport.isFeatureSupported(ChipSupport.HAVE_MATV_FEATURE)) {
            removePreference(screen, "matv");
        }

        // BT is not ready if MTK_BT_SUPPORT isn't defined
        if (!ChipSupport.isFeatureSupported(ChipSupport.MTK_BT_SUPPORT)) {
            removePreference(screen, "bluetooth");
        }

        // wifi is not ready if MTK_WLAN_SUPPORT isn't defined
        if (!ChipSupport.isFeatureSupported(ChipSupport.MTK_WLAN_SUPPORT)) {
            removePreference(screen, "wifi");
        }

        if (!FeatureOption.MTK_TVOUT_SUPPORT) {
            removePreference(screen, "tv_out");
        }
        if (!isVoiceCapable() || isWifiOnly()) {
            removePreference(screen, "auto_answer");
            removePreference(screen, "repeat_call_test");
            removePreference(screen, "video_telephony");
        }

        if (!FeatureOption.MTK_VT3G324M_SUPPORT) {
            removePreference(screen, "video_telephony");
        }

        if (isWifiOnly()) {
            removePreference(screen, "GPRS");
            removePreference(screen, "Modem");
            removePreference(screen, "NetworkInfo");
            removePreference(screen, "Baseband");
            removePreference(screen, "SIMMeLock");
            removePreference(screen, "BandMode");
            removePreference(screen, "RAT Mode");
            removePreference(screen, "SWLA");
            removePreference(screen, "ModemTest");
        }

        // if it single sim, then the flow is the same as before
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            /**
             * if it is Gemini, then the flow is : it start a TabActivity, then
             * the TabActivity will start sim1 or sim2 simLock activity Intent
             * to launch SIM lock TabActivity
             */
            // intent.setComponent(new
            // ComponentName("com.android.simmelock","com.android.simmelock.TabLockList"));
            removePreference(screen, "simme_lock1");
        } else {
            // Intent to launch SIM lock settings
            // intent.setComponent(new
            // ComponentName("com.android.simmelock","com.android.simmelock.LockList"));
            removePreference(screen, "simme_lock2");
        }
        Xlog.i(TAG, "ChipSupport.getChip(): " + ChipSupport.getChip());
        if (ChipSupport.MTK_6589_SUPPORT > ChipSupport.getChip()) {
            removePreference(screen, "de_sense");
            removePreference(screen, "camera89");
        } else {
            removePreference(screen, "camera");
        }

        if (!FeatureOption.MTK_FD_SUPPORT) {
            removePreference(screen, "fast_dormancy");
        }
        
        File innerLoadIndicator = new File(INNER_LOAD_INDICATOR_FILE);
        if (!innerLoadIndicator.exists()) {
        	removePreference(screen, "system_update");
        }
        if (ChipSupport.isCurrentChipEquals(ChipSupport.MTK_6572_SUPPORT)) {
        	//removePreference(screen, "non_sleep_mode");
        } else {
        	removePreference(screen, "deep_idle");
        	removePreference(screen, "sleep_mode");
        	removePreference(screen, "dcm");
        	removePreference(screen, "pll_cg");
        	removePreference(screen, "cpu_dvfs");
        	removePreference(screen, "mcdi_setting");
        }

        if (!"OP01".equals(SystemProperties.get("ro.operator.optr"))) {
        	removePreference(screen, "ConfigureCheck");
        }
        
        if(!FeatureOption.MTK_CDS_EM_SUPPORT){
            removePreference(screen, "cds_information");
        }

        Preference pref = (Preference) findPreference("cmas");
        if (pref != null && !isActivityAvailable(pref.getIntent())) {
            removePreference(screen, "cmas");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        PreferenceScreen screen = getPreferenceScreen();
        int count = screen.getPreferenceCount();
        for (int i = 0; i < count; i++) {
            Preference pre = screen.getPreference(i);
            if (null != pre) {
                Intent intent = pre.getIntent();
                pre.setEnabled(isActivityAvailable(intent));
            }
        }
    }

    private void removePreference(PreferenceScreen prefScreen, String prefId) {
        Preference pref = (Preference) findPreference(prefId);
        if (pref != null) {
            prefScreen.removePreference(pref);
        }
    }

    private boolean isVoiceCapable() {
        TelephonyManager telephony = (TelephonyManager) getActivity()
                .getSystemService(Context.TELEPHONY_SERVICE);
        boolean bVoiceCapable = (telephony != null && telephony
                .isVoiceCapable());
        Xlog.i(TAG, "sIsVoiceCapable : " + bVoiceCapable);
        return bVoiceCapable;
    }

    private boolean isWifiOnly() {
        ConnectivityManager connManager = (ConnectivityManager) getActivity()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean bWifiOnly = false;
        if (null != connManager) {
            bWifiOnly = !connManager
                    .isNetworkSupported(ConnectivityManager.TYPE_MOBILE);
        }
        return bWifiOnly;
    }

    private boolean isActivityAvailable(Intent intent) {
        return (null != getActivity().getPackageManager().resolveActivity(
                intent, 0));
    }
}
