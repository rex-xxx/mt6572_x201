/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
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

package com.android.phone;

import android.app.ActionBar;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.Telephony.SIMInfo;
import android.view.MenuItem;
import android.widget.Toast;

import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.gemini.GeminiPhone;
import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.settings.CallSettings;
import com.mediatek.settings.MultipleSimActivity;
import com.mediatek.xlog.Xlog;

import java.util.ArrayList;
import java.util.List;

public class GsmUmtsCallForwardOptions extends TimeConsumingPreferenceActivity {
    private static final String LOG_TAG = "Settings/GsmUmtsCallForwardOptions";
    private static final boolean DBG = true;//(PhoneApp.DBG_LEVEL >= 2);

    private static final String NUM_PROJECTION[] = {Phone.NUMBER};

    private static final String BUTTON_CFU_KEY   = "button_cfu_key";
    private static final String BUTTON_CFB_KEY   = "button_cfb_key";
    private static final String BUTTON_CFNRY_KEY = "button_cfnry_key";
    private static final String BUTTON_CFNRC_KEY = "button_cfnrc_key";

    private static final String KEY_TOGGLE = "toggle";
    private static final String KEY_STATUS = "status";
    private static final String KEY_NUMBER = "number";

    private CallForwardEditPreference mButtonCFU;
    private CallForwardEditPreference mButtonCFB;
    private CallForwardEditPreference mButtonCFNRy;
    private CallForwardEditPreference mButtonCFNRc;

    private final ArrayList<CallForwardEditPreference> mPreferences =
            new ArrayList<CallForwardEditPreference>();
    private int mInitIndex = 0;

    private boolean mFirstResume;
    private Bundle mIcicle;
    /// M: support gemini & broadcast @{
    private static final String KEY_ITEM_STATUS = "item_status";
    private boolean mIsFinished = false;
    private boolean mIsVtSetting = false;
    public static final int DEFAULT_SIM = 2; /* 0: SIM1, 1: SIM2 */
    private int mSimId = DEFAULT_SIM;
    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(action)) {
                if (intent.getBooleanExtra("state", false)) {
                    finish();
                }
            } else if (Intent.ACTION_DUAL_SIM_MODE_CHANGED.equals(action)) {
                if (intent.getIntExtra(Intent.EXTRA_DUAL_SIM_MODE, -1) == 0) {
                    finish();
                }
            } else if (action.equals(TelephonyIntents.ACTION_SIM_INFO_UPDATE)) {
                ///M: add for hot swap {
                List<SIMInfo> temp = SIMInfo.getInsertedSIMList(context);
                // all sim card is pluged out
                if (temp.size() == 0) {
                    Xlog.d(LOG_TAG, "Activity finished");
                    CallSettings
                            .goUpToTopLevelSetting(GsmUmtsCallForwardOptions.this);
                } else if (CallSettings.isMultipleSim() && temp.size() == 1) {
                    if (temp.get(0).mSlot != mSimId) {
                        Xlog.d(LOG_TAG, "temp.size()=" + temp.size()
                                + "Activity finished");
                        finish();
                    }
                }
                ///@}
            }
        }
    };
    /// @} 

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.callforward_options);

        PreferenceScreen prefSet = getPreferenceScreen();
        mButtonCFU   = (CallForwardEditPreference) prefSet.findPreference(BUTTON_CFU_KEY);
        mButtonCFB   = (CallForwardEditPreference) prefSet.findPreference(BUTTON_CFB_KEY);
        mButtonCFNRy = (CallForwardEditPreference) prefSet.findPreference(BUTTON_CFNRY_KEY);
        mButtonCFNRc = (CallForwardEditPreference) prefSet.findPreference(BUTTON_CFNRC_KEY);

        mButtonCFU.setParentActivity(this, mButtonCFU.mReason);
        mButtonCFB.setParentActivity(this, mButtonCFB.mReason);
        mButtonCFNRy.setParentActivity(this, mButtonCFNRy.mReason);
        mButtonCFNRc.setParentActivity(this, mButtonCFNRc.mReason);
        mPreferences.add(mButtonCFU);
        mPreferences.add(mButtonCFB);
        mPreferences.add(mButtonCFNRy);
        mPreferences.add(mButtonCFNRc);

        // we wait to do the initialization until onResume so that the
        // TimeConsumingPreferenceActivity dialog can display as it
        // relies on onResume / onPause to maintain its foreground state.

        mFirstResume = true;
        mIcicle = icicle;
        
        /// M: for vt CF, gemini phone receiver @{
        //Set the toggle in order to meet the recover dialog in correct status
        if (CallSettings.isMultipleSim()) {
            mSimId = getIntent().getIntExtra(PhoneConstants.GEMINI_SIM_ID_KEY, -1);
        }
        mIsVtSetting = getIntent().getBooleanExtra("ISVT", false);
        Xlog.d("GsmUmtsCallForwardoptions", "Sim Id : " + mSimId + "  for VT setting = " + mIsVtSetting);        
        isReady();
        if (mIsVtSetting) {
            mButtonCFU.setServiceClass(CommandsInterface.SERVICE_CLASS_VIDEO);
            mButtonCFB.setServiceClass(CommandsInterface.SERVICE_CLASS_VIDEO);
            mButtonCFNRy.setServiceClass(CommandsInterface.SERVICE_CLASS_VIDEO);
            mButtonCFNRc.setServiceClass(CommandsInterface.SERVICE_CLASS_VIDEO);
        }
        if (null != icicle) {
            for (CallForwardEditPreference pref : mPreferences) {
                if (null != pref) {
                    Bundle bundle = icicle.getParcelable(pref.getKey());
                    if (null != bundle) {
                        pref.setToggled(bundle.getBoolean(KEY_TOGGLE));
                    }
                }
            }
        }
        PhoneUtils.setMmiFinished(false);
        if (null != getIntent().getStringExtra(MultipleSimActivity.SUB_TITLE_NAME)) {
            setTitle(getIntent().getStringExtra(MultipleSimActivity.SUB_TITLE_NAME));
        }
        
        IntentFilter intentFilter = new IntentFilter(
                Intent.ACTION_AIRPLANE_MODE_CHANGED);
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            intentFilter.addAction(Intent.ACTION_DUAL_SIM_MODE_CHANGED);
        }
        ///M: add for hot swap {
        intentFilter.addAction(TelephonyIntents.ACTION_SIM_INFO_UPDATE);
        ///@}
        registerReceiver(mIntentReceiver, intentFilter);
        /// @}
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            // android.R.id.home will be triggered in onOptionsItemSelected()
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        ///M: when press home key & return back refresh the setting
        if (mFirstResume) {
            mInitIndex = 0;
            Xlog.d(LOG_TAG, "START INIT(onResume1): mInitIndex is  " + mInitIndex);    
            mPreferences.get(mInitIndex).init(this, false, mSimId);          
            mFirstResume = false;
        } else if (PhoneUtils.getMmiFinished()) {
            mInitIndex = 0;
            Xlog.d(LOG_TAG, "START INIT(onResume2): mInitIndex is  " + mInitIndex);    
            mPreferences.get(mInitIndex).init(this, false, mSimId);
            PhoneUtils.setMmiFinished(false);
        } else {
            Xlog.d(LOG_TAG, "No change, so don't query!");
        }
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

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        for (CallForwardEditPreference pref : mPreferences) {
            Bundle bundle = new Bundle();
            bundle.putBoolean(KEY_TOGGLE, pref.isToggled());
            bundle.putBoolean(KEY_ITEM_STATUS, pref.isEnabled());
            if (pref.mCallForwardInfo != null) {
                bundle.putString(KEY_NUMBER, pref.mCallForwardInfo.number);
                bundle.putInt(KEY_STATUS, pref.mCallForwardInfo.status);
            }
            outState.putParcelable(pref.getKey(), bundle);
        }
    }

    @Override
    public void onFinished(Preference preference, boolean reading) {
        ///M: when the CF get fail, disable them
        if (mInitIndex < mPreferences.size() - 1 && !isFinishing()) {
            if (mPreferences.get(mInitIndex++).isSuccess()) {
                Xlog.d(LOG_TAG, "START INIT(onFinished): mInitIndex is  " + mInitIndex);    
                mPreferences.get(mInitIndex).init(this, false, mSimId);
            } else {
                for (int i = mInitIndex; i < mPreferences.size(); ++i) {
                    mPreferences.get(i).setEnabled(false);
                }
                mInitIndex = mPreferences.size();
            }
        }

        super.onFinished(preference, reading);
        removeDialog();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (DBG) {
            Xlog.d(LOG_TAG, "onActivityResult: done");
        }
        if (resultCode != RESULT_OK) {
            if (DBG) {
                Xlog.d(LOG_TAG, "onActivityResult: contact picker result not OK.");
            }
            return;
        }

        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(data.getData(),
                    NUM_PROJECTION, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                switch (requestCode) {
                case CommandsInterface.CF_REASON_UNCONDITIONAL:
                    if (mButtonCFU != null) {
                        mButtonCFU.onPickActivityResult(cursor.getString(0));
                    }
                       break;
                case CommandsInterface.CF_REASON_BUSY:
                    if (mButtonCFB != null) {
                        mButtonCFB.onPickActivityResult(cursor.getString(0));
                    }
                    break;
                case CommandsInterface.CF_REASON_NO_REPLY:
                    if (mButtonCFNRy != null) {
                        mButtonCFNRy.onPickActivityResult(cursor.getString(0));
                    }
                    break;
                case CommandsInterface.CF_REASON_NOT_REACHABLE:
                    if (mButtonCFNRc != null) {
                        mButtonCFNRc.onPickActivityResult(cursor.getString(0));
                    }
                    break;
                default:
                    break;
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
    /// M: to confirm CF preference the activity is destroy   
    public void onDestroy() {
        if (mButtonCFU != null) {
            mButtonCFU.setStatus(true);
        }
        if (mButtonCFB != null) {
            mButtonCFB.setStatus(true);
        }
        if (mButtonCFNRy != null) {
            mButtonCFNRy.setStatus(true);
        }
        if (mButtonCFNRc != null) {
            mButtonCFNRc.setStatus(true);
        }
        unregisterReceiver(mIntentReceiver);
        super.onDestroy();
    }
    /// M: is sim card ready or not    
    private void isReady() {
        com.android.internal.telephony.Phone  phone = PhoneGlobals.getPhone();
        int state;
        if (CallSettings.isMultipleSim()) {
            state = ((GeminiPhone)phone).getServiceStateGemini(mSimId).getState();
        } else {
            state = phone.getServiceState().getState();
        }
        if (state != android.telephony.ServiceState.STATE_IN_SERVICE) {
            finish();
            Toast.makeText(this,getString(R.string.net_or_simcard_busy),Toast.LENGTH_SHORT).show();
        }
    }
    
    /// M: Refresh the settings when disable CFU
    public void refreshSettings(boolean bNeed) {
        if (bNeed) {
            mInitIndex = 1;
            Xlog.d(LOG_TAG, "START INIT(refreshSettings): mInitIndex is  " + mInitIndex);    
            mPreferences.get(mInitIndex).init(this, false, mSimId);
        }
    }
}
