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

import android.app.ActionBar;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.Telephony.SIMInfo;
import android.view.MenuItem;

import com.android.internal.telephony.TelephonyIntents;
import com.android.phone.PhoneGlobals;
import com.android.phone.R;
import com.mediatek.phone.PhoneFeatureConstants.FeatureOption;
import com.mediatek.xlog.Xlog;

import java.util.List;

public class VTAdvancedSettingEx extends PreferenceActivity {

    private static final String BUTTON_VT_REPLACE_KEY     = "button_vt_replace_expand_key";
    private static final String BUTTON_VT_ENABLE_BACK_CAMERA_KEY     = "button_vt_enable_back_camera_key";
    private static final String BUTTON_VT_PEER_BIGGER_KEY     = "button_vt_peer_bigger_key";
    private static final String BUTTON_VT_MO_LOCAL_VIDEO_DISPLAY_KEY     = "button_vt_mo_local_video_display_key";
    private static final String BUTTON_VT_MT_LOCAL_VIDEO_DISPLAY_KEY     = "button_vt_mt_local_video_display_key";
    
    private static final String BUTTON_CALL_FWD_KEY    = "button_cf_expand_key";
    private static final String BUTTON_CALL_BAR_KEY    = "button_cb_expand_key";
    private static final String BUTTON_CALL_ADDITIONAL_KEY    = "button_more_expand_key";
    
    private static final String BUTTON_VT_PEER_REPLACE_KEY = "button_vt_replace_peer_expand_key";
    private static final String BUTTON_VT_ENABLE_PEER_REPLACE_KEY = "button_vt_enable_peer_replace_key";
    private static final String BUTTON_VT_AUTO_DROPBACK_KEY = "button_vt_auto_dropback_key";
    private static final String CHECKBOX_RING_ONLY_ONCE = "ring_only_once";
    private static final String BUTTON_VT_RINGTONE_KEY    = "button_vt_ringtone_key";
    private static final String SELECT_MY_PICTURE         = "2";
    
    private static final String SELECT_DEFAULT_PICTURE    = "0";
    
    private static final String SELECT_DEFAULT_PICTURE2    = "0";
    private static final String SELECT_MY_PICTURE2         = "1";

    /** The launch code when picking a photo and the raw data is returned */
    public static final int REQUESTCODE_PICTRUE_PICKED_WITH_DATA = 3021;
    
    private Preference mButtonVTEnablebackCamer;
    private Preference mButtonVTReplace;
    private Preference mButtonVTPeerBigger;
    private Preference mButtonVTMoVideo;
    private Preference mButtonVTMtVideo;
    private Preference mButtonCallFwd;
    private Preference mButtonCallBar;
    private Preference mButtonCallAdditional;    
    private CheckBoxPreference mCheckBoxRingOnlyOnce;
    
    private Preference mButtonVTPeerReplace;
    private Preference mButtonVTEnablePeerReplace;
    private Preference mButtonVTAutoDropBack;
    
    private long mSimIds[] = null;

    // debug data
    private static final String LOG_TAG = "Settings/VTAdvancedSetting";
    private static final boolean DBG = true; // (PhoneApp.DBG_LEVEL >= 2);
    
    private PreCheckForRunning mPreCfr = null;
    private boolean mIsOnlyOneSim = false;
    
    private static void log(String msg) {
        Xlog.d(LOG_TAG, msg);
    }
    //M: add for hot swap {
    private IntentFilter mIntentFilter;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(TelephonyIntents.ACTION_SIM_INFO_UPDATE)) {
                List<SIMInfo> temp = SIMInfo.getInsertedSIMList(context);
                if (temp.size() == 0) {
                    Xlog.d(LOG_TAG,"Activity finished");
                    CallSettings.goUpToTopLevelSetting(VTAdvancedSettingEx.this);
                } else if (temp.size() == 1) {
                    Xlog.d(LOG_TAG,"temp.size()=" + temp.size() + "Activity finished");
                    CallSettings.goUpToTopLevelSetting(VTAdvancedSettingEx.this);
                } else {
                    mSimIds = CallSettings.get3GSimCards(VTAdvancedSettingEx.this);
                    setScreenEnabled();
                }    
            }    
        }
    };
    ///@}
     
    protected void onCreate(Bundle icicle) {
        
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.vt_advanced_setting_ex);

        mPreCfr = new PreCheckForRunning(this);
        List<SIMInfo> list = SIMInfo.getInsertedSIMList(this);

        mSimIds = CallSettings.get3GSimCards(this);

        mButtonVTReplace = findPreference(BUTTON_VT_REPLACE_KEY);
        
        mButtonVTEnablebackCamer = findPreference(BUTTON_VT_ENABLE_BACK_CAMERA_KEY);
        mButtonVTPeerBigger = findPreference(BUTTON_VT_PEER_BIGGER_KEY);
        mButtonVTMoVideo = findPreference(BUTTON_VT_MO_LOCAL_VIDEO_DISPLAY_KEY);
        mButtonVTMtVideo = findPreference(BUTTON_VT_MT_LOCAL_VIDEO_DISPLAY_KEY);
        
        mButtonCallAdditional = findPreference(BUTTON_CALL_ADDITIONAL_KEY);
        mButtonCallFwd =  findPreference(BUTTON_CALL_FWD_KEY);
        mButtonCallBar = findPreference(BUTTON_CALL_BAR_KEY);
        
        mButtonVTPeerReplace = findPreference(BUTTON_VT_PEER_REPLACE_KEY);
        mButtonVTEnablePeerReplace = findPreference(BUTTON_VT_ENABLE_PEER_REPLACE_KEY);
        mButtonVTAutoDropBack = findPreference(BUTTON_VT_AUTO_DROPBACK_KEY);
        mCheckBoxRingOnlyOnce = (CheckBoxPreference)findPreference(CHECKBOX_RING_ONLY_ONCE);
        Xlog.d("MyLog","FeatureOption.MTK_VT3G324M_SUPPORT=" + FeatureOption.MTK_VT3G324M_SUPPORT + "" 
                + "FeatureOption.MTK_PHONE_VT_VOICE_ANSWER=" + FeatureOption.MTK_PHONE_VT_VOICE_ANSWER);
        if (!(FeatureOption.MTK_VT3G324M_SUPPORT && FeatureOption.MTK_PHONE_VT_VOICE_ANSWER)) {
            getPreferenceScreen().removePreference(mCheckBoxRingOnlyOnce);
        } 
        ///M: add for hot swap {
        mIntentFilter = new IntentFilter(TelephonyIntents.ACTION_SIM_INFO_UPDATE);
        this.registerReceiver(mReceiver, mIntentFilter);
        ///@}
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            // android.R.id.home will be triggered in onOptionsItemSelected()
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        setScreenEnabled();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        switch (itemId) {
        case android.R.id.home:
            finish();
            return true;
        default:
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setScreenEnabled() {
        boolean isEnable = mSimIds.length > 0;
        if ((mButtonVTReplace.isEnabled() && !isEnable) ||
                (!mButtonVTReplace.isEnabled() && isEnable)) {
            mButtonVTReplace.setEnabled(isEnable);
            mButtonVTEnablebackCamer.setEnabled(isEnable);
            mButtonVTPeerBigger.setEnabled(isEnable);
            mButtonVTMoVideo.setEnabled(isEnable);
            mButtonVTMtVideo.setEnabled(isEnable);
            mButtonCallAdditional.setEnabled(isEnable);
            mButtonCallFwd.setEnabled(isEnable);
            mButtonCallBar.setEnabled(isEnable);
            mButtonVTPeerReplace.setEnabled(isEnable);
            mButtonVTEnablePeerReplace.setEnabled(isEnable);
            mButtonVTAutoDropBack.setEnabled(isEnable); 
        }
                
    }


    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        
        if (preference == mButtonCallFwd) {
            Intent intent = new Intent(this, MultipleSimActivity.class);
            intent.putExtra(MultipleSimActivity.INIT_TITLE_NAME, R.string.labelCF);
            intent.putExtra(MultipleSimActivity.INIT_FEATURE_NAME, "VT");
            intent.putExtra(MultipleSimActivity.INTENT_KEY, "PreferenceScreen");
            intent.putExtra(MultipleSimActivity.INIT_SIM_ID, mSimIds);
            intent.putExtra(MultipleSimActivity.TARGET_CALSS, "com.android.phone.GsmUmtsCallForwardOptions");
            startActivity(intent);
            return true;
        } else if (preference == mButtonCallBar) {
            Intent intent = new Intent(this, MultipleSimActivity.class);
            intent.putExtra("ISVT", true);
            intent.putExtra(MultipleSimActivity.INIT_TITLE_NAME, R.string.lable_call_barring);
            intent.putExtra(MultipleSimActivity.INIT_FEATURE_NAME, "VT");
            intent.putExtra(MultipleSimActivity.INTENT_KEY, "PreferenceScreen");
            intent.putExtra(MultipleSimActivity.INIT_SIM_ID, mSimIds);
            intent.putExtra(MultipleSimActivity.TARGET_CALSS, "com.mediatek.settings.CallBarring");
            startActivity(intent);
            return true;
        } else if (preference == mButtonCallAdditional) {
            Intent intent = new Intent(this, MultipleSimActivity.class);
            intent.putExtra(MultipleSimActivity.INIT_TITLE_NAME, R.string.additional_gsm_call_settings);
            intent.putExtra(MultipleSimActivity.INIT_FEATURE_NAME, "VT");
            intent.putExtra(MultipleSimActivity.INTENT_KEY, "PreferenceScreen");
            intent.putExtra(MultipleSimActivity.INIT_SIM_ID, mSimIds);
            intent.putExtra(MultipleSimActivity.TARGET_CALSS, "com.android.phone.GsmUmtsAdditionalCallOptions");
            startActivity(intent);
            return true;
        } else if (preference == mButtonVTEnablebackCamer) {
            Intent intent = new Intent(this, MultipleSimActivity.class);
            intent.putExtra(MultipleSimActivity.INIT_FEATURE_NAME, "VT");
            intent.putExtra(MultipleSimActivity.INIT_SIM_ID, mSimIds);
            intent.putExtra(MultipleSimActivity.INIT_TITLE_NAME, R.string.vt_enable_back_camera);
            intent.putExtra(MultipleSimActivity.INIT_BASE_KEY, "button_vt_enable_back_camera_key@");
            intent.putExtra(MultipleSimActivity.INTENT_KEY, "CheckBoxPreference");
            startActivity(intent);
            return true;
        } else if (preference == this.mButtonVTReplace) {
            Intent intent = new Intent(this, MultipleSimActivity.class);
            intent.putExtra(MultipleSimActivity.INIT_TITLE_NAME, R.string.vt_local_video_rep);
            intent.putExtra(MultipleSimActivity.INIT_FEATURE_NAME, "VT");
            intent.putExtra(MultipleSimActivity.INTENT_KEY, "ListPreference");
            intent.putExtra(MultipleSimActivity.LIST_TITLE, R.string.vt_local_video_rep);
            intent.putExtra(MultipleSimActivity.INIT_ARRAY, R.array.vt_replace_local_video_entries);
            intent.putExtra(MultipleSimActivity.INIT_SIM_ID, mSimIds);
            if (getKeyValue("button_vt_replace_expand_key") == null) {
                setKeyValue("button_vt_replace_expand_key", "0");
            }
            intent.putExtra(MultipleSimActivity.INIT_BASE_KEY, "button_vt_replace_expand_key@");
            intent.putExtra(MultipleSimActivity.INIT_ARRAY_VALUE, R.array.vt_replace_local_video_values);
            startActivity(intent);
            return true;
        } else if (preference == mButtonVTPeerBigger) {
            Intent intent = new Intent(this, MultipleSimActivity.class);
            intent.putExtra(MultipleSimActivity.INIT_FEATURE_NAME, "VT");
            intent.putExtra(MultipleSimActivity.INIT_SIM_ID, mSimIds);
            intent.putExtra(MultipleSimActivity.INIT_TITLE_NAME, R.string.vt_peer_video_bigger);
            intent.putExtra(MultipleSimActivity.INTENT_KEY, "CheckBoxPreference");
            intent.putExtra(MultipleSimActivity.INIT_BASE_KEY, "button_vt_peer_bigger_key@");
            startActivity(intent);
        } else if (preference == mButtonVTMoVideo) {
            Intent intent = new Intent(this, MultipleSimActivity.class);
            intent.putExtra(MultipleSimActivity.INIT_FEATURE_NAME, "VT");
            intent.putExtra(MultipleSimActivity.INIT_SIM_ID, mSimIds);
            intent.putExtra(MultipleSimActivity.INIT_TITLE_NAME, R.string.vt_outgoing_call);
            intent.putExtra(MultipleSimActivity.INTENT_KEY, "CheckBoxPreference");
            intent.putExtra(MultipleSimActivity.INIT_BASE_KEY, "button_vt_mo_local_video_display_key@");
            startActivity(intent);
        } else if (preference == mButtonVTMtVideo) {
            Intent intent = new Intent(this, MultipleSimActivity.class);
            intent.putExtra(MultipleSimActivity.INIT_TITLE_NAME, R.string.vt_incoming_call);
            intent.putExtra(MultipleSimActivity.INIT_FEATURE_NAME, "VT");
            intent.putExtra(MultipleSimActivity.LIST_TITLE, R.string.vt_incoming_call);
            intent.putExtra(MultipleSimActivity.INTENT_KEY, "ListPreference");
            intent.putExtra(MultipleSimActivity.INIT_ARRAY, R.array.vt_mt_local_video_display_entries);
            intent.putExtra(MultipleSimActivity.INIT_SIM_ID, mSimIds);
            if (getKeyValue("button_vt_mt_local_video_display_key") == null) {
                setKeyValue("button_vt_mt_local_video_display_key", "0");
            }
            intent.putExtra(MultipleSimActivity.INIT_BASE_KEY, "button_vt_mt_local_video_display_key@");
            intent.putExtra(MultipleSimActivity.INIT_ARRAY_VALUE, R.array.vt_mt_local_video_display_values);
            startActivity(intent);
            return true;
        } else if (preference == mButtonVTPeerReplace) {
            Intent intent = new Intent(this, MultipleSimActivity.class);
            intent.putExtra(MultipleSimActivity.INIT_TITLE_NAME, R.string.vt_peer_video_rep);
            intent.putExtra(MultipleSimActivity.INIT_FEATURE_NAME, "VT");
            intent.putExtra(MultipleSimActivity.INTENT_KEY, "ListPreference");
            intent.putExtra(MultipleSimActivity.LIST_TITLE, R.string.vt_peer_video_rep);
            intent.putExtra(MultipleSimActivity.INIT_ARRAY, R.array.vt_replace_local_video_entries2);
            intent.putExtra(MultipleSimActivity.INIT_SIM_ID, mSimIds);
            if (getKeyValue("button_vt_replace_peer_expand_key") == null) {
                setKeyValue("button_vt_replace_peer_expand_key", "0");
            }
            intent.putExtra(MultipleSimActivity.INIT_BASE_KEY, "button_vt_replace_peer_expand_key@");
            intent.putExtra(MultipleSimActivity.INIT_ARRAY_VALUE, R.array.vt_replace_local_video_values2);
            startActivity(intent);
            return true;
        } else if (preference == mButtonVTEnablePeerReplace) {
            Intent intent = new Intent(this, MultipleSimActivity.class);
            intent.putExtra(MultipleSimActivity.INIT_FEATURE_NAME, "VT");
            intent.putExtra(MultipleSimActivity.INIT_SIM_ID, mSimIds);
            intent.putExtra(MultipleSimActivity.INIT_TITLE_NAME, R.string.vt_dis_peer_rep);
            intent.putExtra(MultipleSimActivity.INTENT_KEY, "CheckBoxPreference");
            intent.putExtra(MultipleSimActivity.INIT_BASE_KEY, "button_vt_enable_peer_replace_key@");
            startActivity(intent);
        } else if (preference == mButtonVTAutoDropBack) {
            Intent intent = new Intent(this, MultipleSimActivity.class);
            intent.putExtra(MultipleSimActivity.INIT_FEATURE_NAME, "VT");
            intent.putExtra(MultipleSimActivity.INIT_SIM_ID, mSimIds);
            intent.putExtra(MultipleSimActivity.INIT_TITLE_NAME, R.string.vt_auto_dropback);
            intent.putExtra(MultipleSimActivity.INTENT_KEY, "CheckBoxPreference");
            intent.putExtra(MultipleSimActivity.INIT_BASE_KEY, "button_vt_auto_dropback_key@");
            startActivity(intent);
        }
        
        return false;
    }
    
    private String getKeyValue(String key) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
        return sp.getString(key, null);
    }
    
    private void setKeyValue(String key, String value) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(key, value);
        editor.commit();
    }

    protected void onDestroy() {
        super.onDestroy();
        if (mPreCfr != null) {
            mPreCfr.deRegister();
        }
        ///M: add for hot swap{
        this.unregisterReceiver(mReceiver);
        ///@}
    }
}
