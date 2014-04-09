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

package com.mediatek.CellConnService;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;
import android.view.WindowManager;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Context;

import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;

import com.mediatek.common.featureoption.FeatureOption;

import android.provider.Settings;
import android.provider.Telephony.SIMInfo;
import android.content.SharedPreferences;

public class ConfirmDlgActivity extends Activity implements OnClickListener {

    private static final String LOGTAG = "ConfirmDlgActivity";

    private static final String SYSTEM_DIALOG_REASON_KEY = "reason";
    private static final String SYSTEM_DIALOG_REASON_RECENT_APPS = "recentapps";

    // buttons id
    public static final int OK_BUTTON = R.id.button_ok;
    public static final int CANCEL_BUTTON = R.id.button_cancel;

    private String mTitle;
    private String mText;
    private String mButtonText;
    private String mRButtonText;

    private int mConfirmType;
    private int mSlot;
    private AlertDialog mAlertDlg;
    private boolean mResultSent;
    private boolean mRoamingWithPrefer;
    private int mPreferSlot;
    private boolean mNegativeExit;
    private boolean mBReceiverIsUnregistered = false;

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(LOGTAG, "BroadcastReceiver onReceive");
            if (Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(intent.getAction())) {
                Log.d(LOGTAG, "BroadcastReceiver AIRPLANE_MODE_CHANGED");
                boolean airplaneModeON = intent.getBooleanExtra("state", false);
                Log.d(LOGTAG, "AIRPLANE_MODE_CHANGED ,airplaneModeON = " + airplaneModeON);

                if (airplaneModeON) {
                    // All of radio off
                    mNegativeExit = true;
                    sendConfirmResult(mConfirmType, false);
                } else if (PhoneStatesMgrService.CONFIRM_TYPE_RADIO == mConfirmType) {
                    mNegativeExit = true;
                    sendConfirmResult(mConfirmType, false);
                }
                return;

            } else if (Intent.ACTION_DUAL_SIM_MODE_CHANGED.equals(intent.getAction())) {
                Log.d(LOGTAG, "BroadcastReceiver ACTION_DUAL_SIM_MODE_CHANGED");

                int dualSimMode = intent.getIntExtra(Intent.EXTRA_DUAL_SIM_MODE, 0);
                Log.d(LOGTAG, "BroadcastReceiver duslSimMode = " + dualSimMode);                 

                if( ((dualSimMode >> mSlot) & 1) == 1) {
                    if (PhoneStatesMgrService.CONFIRM_TYPE_RADIO == mConfirmType) {
                        sendConfirmResult(mConfirmType, true);
                    }
                } else {
                    mNegativeExit = true;
                    sendConfirmResult(mConfirmType, false);
                }
            } else if (Intent.ACTION_CLOSE_SYSTEM_DIALOGS.equals(intent.getAction())) {
                Log.d(LOGTAG, "BroadcastReceiver ACTION_CLOSE_SYSTEM_DIALOGS");
                String reason = intent.getStringExtra(SYSTEM_DIALOG_REASON_KEY);
                if (SYSTEM_DIALOG_REASON_RECENT_APPS.equals(reason)) {
                    Log.d(LOGTAG, "Recent App, dismiss dialog");
                    mNegativeExit = true;
                    sendConfirmResult(mConfirmType, false);
                }
            } else if (TelephonyIntents.ACTION_SIM_STATE_CHANGED.equals(intent.getAction())) {
                Log.d(LOGTAG, intent.toString() + intent.getStringExtra(IccCardConstants.INTENT_KEY_ICC_STATE));
                String stateExtra = intent.getStringExtra(IccCardConstants.INTENT_KEY_ICC_STATE);

                int slot = 0;
                if (FeatureOption.MTK_GEMINI_SUPPORT == true) {
                    slot = intent.getIntExtra(PhoneConstants.GEMINI_SIM_ID_KEY,PhoneConstants.GEMINI_SIM_1);
                }

                if (slot >= PhoneConstants.GEMINI_SIM_NUM) {
                    Log.e(LOGTAG, "BroadcastReceiver SIM State changed slot is invalid");
                    return;
                }

                Log.d(LOGTAG, "Slot = " + slot + " ,request slot = " + mSlot);
                if (IccCardConstants.INTENT_VALUE_ICC_READY.equals(stateExtra)) {
                    if(slot == mSlot && PhoneStatesMgrService.CONFIRM_TYPE_RADIO == mConfirmType) {
                        //ALPS00565287: We have to dismiss confirm dialog when the SIM 
                        //state of request slot is ready.
                        mNegativeExit = true;
                        sendConfirmResult(mConfirmType, false);
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        Log.d(LOGTAG, "onCreate");
        IntentFilter itFilter = new IntentFilter();
        if (null == itFilter) {
            Log.e(LOGTAG, "onCreate new intent failed");
            return;
        }
        itFilter.addAction(Intent.ACTION_DUAL_SIM_MODE_CHANGED);
        itFilter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        itFilter.addAction(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        itFilter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        registerReceiver(mIntentReceiver, itFilter);
        mRoamingWithPrefer = false;
        mPreferSlot = 0;
        mNegativeExit = false;
        this.onNewIntent(getIntent());
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        Log.d(LOGTAG, "onDestroy");
        if (!mBReceiverIsUnregistered) {
            mBReceiverIsUnregistered = true;
            unregisterReceiver(mIntentReceiver);
        }
        super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        // TODO Auto-generated method stub
        Log.d(LOGTAG, "onNewIntent");
        initFromIntent(getIntent());
        super.onNewIntent(intent);
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        Log.d(LOGTAG, "onPause");
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        Log.d(LOGTAG, "onResume");
        if (PhoneStatesMgrService.CONFIRM_TYPE_FDN == mConfirmType 
                || PhoneStatesMgrService.CONFIRM_TYPE_SIMLOCKED == mConfirmType
                || PhoneStatesMgrService.CONFIRM_TYPE_SLOTLOCKED == mConfirmType) {
            mAlertDlg = new AlertDialog.Builder(this).setMessage(mText).setIcon(
                    android.R.drawable.ic_dialog_alert).setNegativeButton(
                    android.R.string.ok, this).setCancelable(true)
                    .setOnCancelListener(
                            new DialogInterface.OnCancelListener() {
                                public void onCancel(DialogInterface dialog) {
                                    Log.d(LOGTAG, "onClick is cancel");
                                    mNegativeExit = true;
                                    sendConfirmResult(mConfirmType, false);
                                }
                            }).show();
        } else if (PhoneStatesMgrService.CONFIRM_TYPE_ROAMING == mConfirmType) {
            mAlertDlg = new AlertDialog.Builder(this).setMessage(mText).setTitle(mTitle)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(mButtonText, this).setNegativeButton(
                            mRButtonText, this).setCancelable(true)
                    .setOnCancelListener(
                            new DialogInterface.OnCancelListener() {
                                public void onCancel(DialogInterface dialog) {
                                    Log.d(LOGTAG, "onClick is cancel");
                                    mNegativeExit = true;
                                    sendConfirmResult(mConfirmType, false);
                                }
                            }).show();
        } else {
            mAlertDlg = new AlertDialog.Builder(this).setMessage(mText)
                    .setTitle(mTitle).setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(mButtonText, this).setNegativeButton(
                            android.R.string.cancel, this).setCancelable(true)
                    .setOnCancelListener(
                            new DialogInterface.OnCancelListener() {
                                public void onCancel(DialogInterface dialog) {
                                    Log.d(LOGTAG, "onClick is cancel");
                                    mNegativeExit = true;
                                    sendConfirmResult(mConfirmType, false);
                                }
                            }).show();
        }

        mAlertDlg.getWindow().addFlags(WindowManager.LayoutParams.FLAG_HOMEKEY_DISPATCHED);
        mAlertDlg.setOnKeyListener(new DialogInterface.OnKeyListener(){
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event)
            {
                if (event.isDown() && keyCode == KeyEvent.KEYCODE_HOME)
                {
                    Log.d(LOGTAG,"home key is down, cancel request");
                    mNegativeExit = true;
                    sendConfirmResult(mConfirmType, false);
                    return true;
                }
                return false;
            }
        }
        );

        mResultSent = false;
    }

    private void initFromIntent(Intent intent) {

        Log.d(LOGTAG, "initFromIntent ++ ");
        mConfirmType = 0;
        if (intent != null) {
            mSlot = intent.getIntExtra(PhoneStatesMgrService.CONFIRM_SLOT, 0);
            Log.d(LOGTAG, "initFromIntent mSlot = " + mSlot);
            mPreferSlot = mSlot;

            mConfirmType = intent.getIntExtra(
                    PhoneStatesMgrService.CONFIRM_TYPE, 0);
            Log.d(LOGTAG, "initFromIntent confirmType = "
                    + PhoneStatesMgrService.confirmTypeToString(mConfirmType));
                    
            if (PhoneStatesMgrService.CONFIRM_TYPE_RADIO == mConfirmType) {
                int airplaneMode = Settings.System.getInt(getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 0);    
                mTitle = getResources().getString(R.string.confirm_radio_title);
                if (FeatureOption.MTK_GEMINI_SUPPORT) {
                    mText = getResources().getString(R.string.confirm_radio_msg,
                    intent.getStringExtra(PhoneStatesMgrService.CONFIRM_CARDNAME));
                } else {
                    mText = getResources().getString(R.string.confirm_radio_msg_single);
                }
                if (airplaneMode == 1)  {
                    if (isRadioOffBySimManagement(mSlot)) {
                        mText = getResources().getString(R.string.confirm_radio_flight_mode_on_and_msg) + " " + 
                                getResources().getString(R.string.confirm_radio_flight_mode_on_sim_off_msg,
                                intent.getStringExtra(PhoneStatesMgrService.CONFIRM_CARDNAME));
                    } else {
                        mText = getResources().getString(R.string.confirm_radio_flight_mode_on_sim_on_msg);
                    }
                }
                mButtonText = getResources().getString(android.R.string.ok);
            } else if (PhoneStatesMgrService.CONFIRM_TYPE_PIN == mConfirmType) {
                mTitle = getResources().getString(R.string.confirm_pin_title);
                mText = getResources().getString(R.string.confirm_pin_msg);
                mButtonText = getResources().getString(R.string.confirm_unlock_lbutton);
            } else if (PhoneStatesMgrService.CONFIRM_TYPE_SIMMELOCK == mConfirmType) {
                mTitle = getResources().getString(R.string.confirm_simmelock_title);
                mText = getResources().getString(R.string.confirm_simmelock_msg);
                mButtonText = getResources().getString(R.string.confirm_unlock_lbutton);
            } else if (PhoneStatesMgrService.CONFIRM_TYPE_FDN == mConfirmType) {
                mTitle = getResources().getString(R.string.confirm_fdn_title);
                mText = getResources().getString(R.string.confirm_fdn_msg);
                mButtonText = getResources().getString(android.R.string.ok);
            } else if (PhoneStatesMgrService.CONFIRM_TYPE_ROAMING == mConfirmType) {
                mTitle = getResources().getString(R.string.confirm_roaming_title);
                mRoamingWithPrefer = intent.getBooleanExtra(PhoneStatesMgrService.CONFIRM_ROAMINGWITHPREFER, false);
                if (mRoamingWithPrefer) {
                    mText = getResources().getString(R.string.confirm_roaming_withPrefer_msg);
                    mButtonText = getResources().getString(R.string.confirm_roamingWithPrefer_lbutton);
                    mRButtonText = getResources().getString(R.string.confirm_roamingWithPrefer_rbutton);
                } else {
                    mText = getResources().getString(R.string.confirm_roaming_msg);
                    mButtonText = getResources().getString(R.string.confirm_roaming_lbutton);
                    mRButtonText = getResources().getString(R.string.confirm_roaming_rbutton);
                }
            } else if (PhoneStatesMgrService.CONFIRM_TYPE_SIMLOCKED == mConfirmType) {
                mText = getResources().getString(R.string.confirm_sim_locked_message);
                mButtonText = getResources().getString(android.R.string.ok);
            } else if (PhoneStatesMgrService.CONFIRM_TYPE_SLOTLOCKED == mConfirmType) {
                mText = getResources().getString(R.string.confirm_slot_locked_message);
                mButtonText = getResources().getString(android.R.string.ok);
            }
        } else {
            finish();
        }

        Log.i(LOGTAG, "initFromIntent - [" + mText + "]");
    }

    private boolean isRadioOffBySimManagement(int simId) {
        SharedPreferences mIccidPreference = getSharedPreferences("RADIO_STATUS", 0);
        SIMInfo simInfo = SIMInfo.getSIMInfoBySlot(this, simId);    
        boolean result = true;
        if ((simInfo != null) && (mIccidPreference != null)) {
           result = mIccidPreference.contains(simInfo.mICCId);      
        }
        return result;
    }     

    public void onClick(DialogInterface dialog, int which) {
        // TODO Auto-generated method stub
        if (which == DialogInterface.BUTTON_POSITIVE) {
            Log.d(LOGTAG, "onClick is true");
            sendConfirmResult(mConfirmType, true);
        } else if (which == DialogInterface.BUTTON_NEGATIVE) {
            Log.d(LOGTAG, "onClick is false");
            sendConfirmResult(mConfirmType, false);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // TODO Auto-generated method stub
        switch (keyCode) {
        case KeyEvent.KEYCODE_BACK:
            Log.d(LOGTAG, "onKeyDown back confirm result is false");
            mNegativeExit = true;
            sendConfirmResult(mConfirmType, false);
            return false;

        default:
            break;
        }

        return super.onKeyDown(keyCode, event);
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // TODO Auto-generated method stub
        Log.d(LOGTAG, "onConfigurationChanged");
        super.onConfigurationChanged(newConfig);
    }

    private void dismissAlertDialog(){
        try {
            if ((mAlertDlg != null) && (mAlertDlg.isShowing()) && (!this.isFinishing())) {
                Log.d(LOGTAG, "dismissAlertDialog");
                mAlertDlg.dismiss();
            }
        } catch (IllegalArgumentException e) {
            Log.w(LOGTAG, "Trying to dismiss a dialog not connected to the current UI");
        }
    }
    
    private void sendConfirmResult(int confirmType, int nRet) {
        Log.d(LOGTAG, "sendConfirmResult confirmType = "
                + PhoneStatesMgrService.confirmTypeToString(confirmType)
                + " nRet = " + PhoneStatesMgrService.confirmResultToString(nRet));

        Intent retIntent = new Intent(getBaseContext(),
                PhoneStatesMgrService.class).putExtra(
                PhoneStatesMgrService.START_TYPE,
                PhoneStatesMgrService.START_TYPE_RSP);
        if (null == retIntent) {
            Log.e(LOGTAG, "sendConfirmResult new retIntent failed");
            return;
        }
        retIntent.putExtra(PhoneStatesMgrService.CONFIRM_TYPE, confirmType);
        retIntent.putExtra(PhoneStatesMgrService.CONFIRM_RESULT_PREFERSLOT, mPreferSlot);
        
        if (!mNegativeExit && (PhoneStatesMgrService.CONFIRM_TYPE_ROAMING == mConfirmType) && (mRoamingWithPrefer)) {
            retIntent.putExtra(PhoneStatesMgrService.CONFIRM_RESULT, PhoneStatesMgrService.CONFIRM_RESULT_OK);
        } else {
            retIntent.putExtra(PhoneStatesMgrService.CONFIRM_RESULT, nRet);
        }

        if (!mBReceiverIsUnregistered) {
            mBReceiverIsUnregistered = true;
            unregisterReceiver(mIntentReceiver);
        }

        startService(retIntent);
        dismissAlertDialog();
        finish();
    }

    private void sendConfirmResult(int confirmType, boolean bRet) {
        Log.d(LOGTAG, "sendConfirmResult confirmType = "
                + PhoneStatesMgrService.confirmTypeToString(confirmType)
                + " bRet = " + bRet);

        mResultSent = true;

        if ((PhoneStatesMgrService.CONFIRM_TYPE_ROAMING == mConfirmType) && (mRoamingWithPrefer)) {
            if (bRet) {
                mPreferSlot = (mSlot == 0) ? 1 : 0;
            } else {
                mPreferSlot = mSlot;
            }
        } else if (!bRet) {
            if (PhoneStatesMgrService.CONFIRM_TYPE_RADIO == mConfirmType) {
                Toast.makeText(this, R.string.confirm_turnon_radio_fail, Toast.LENGTH_SHORT).show();
            } else if (PhoneStatesMgrService.CONFIRM_TYPE_PIN == mConfirmType) {
                Toast.makeText(this, R.string.confirm_unlock_fail, Toast.LENGTH_SHORT).show();
            } else if (PhoneStatesMgrService.CONFIRM_TYPE_SIMMELOCK == mConfirmType) {
                Toast.makeText(this, R.string.confirm_unlock_fail,Toast.LENGTH_SHORT).show();
            } else if (PhoneStatesMgrService.CONFIRM_TYPE_ROAMING == mConfirmType) {
                Toast.makeText(this, R.string.confirm_permit_roaming_fail, Toast.LENGTH_SHORT).show();
            }
        }

        if (bRet) {
            sendConfirmResult(confirmType, PhoneStatesMgrService.CONFIRM_RESULT_OK);
        } else {
            sendConfirmResult(confirmType, PhoneStatesMgrService.CONFIRM_RESULT_CANCEL);
        }
    }

}

