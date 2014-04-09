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

package com.android.simmelock;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncResult; //import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;//To find the SIM card Ready State
import android.view.KeyEvent;
import android.widget.Toast;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneBase;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.gemini.GeminiPhone;
import com.android.internal.telephony.IccCard;
import com.mediatek.common.featureoption.FeatureOption;

public class PermanUnlockSetting extends Activity implements DialogInterface.OnKeyListener {
    /** Called when the activity is first created. */
    private Handler mHandlerFinish = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case 666:
                showDialog(DIALOG_PERMANUNLOCK);
                break;
            case 777:
                PermanUnlockSetting.this.finish();
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.permanunlocksetting);

        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        registerReceiver(mReceiver, intentFilter);

        // set the title
        bundle = this.getIntent().getExtras();
        if (bundle != null) {
            lockCategory = bundle.getInt(ActionList.LOCKCATEGORY, -1);
        }
        if (lockCategory == -1) {
            finish();
            return;
        }
        lockName = getLockName(lockCategory);
        this.setTitle(lockName);

        // show a alert dialog
        // mHandlerFinish.sendEmptyMessage(666);
        // showDialog(DIALOG_PERMANUNLOCK);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mHandlerFinish.sendEmptyMessage(666);
    }

    private String getLockName(final int locktype) {
        switch (locktype) {
        case 0:
            return getString(R.string.strLockNameNetwork);
        case 1:
            return getString(R.string.strLockNameNetworkSub);
        case 2:
            return getString(R.string.strLockNameService);
        case 3:
            return getString(R.string.strLockNameCorporate);
        case 4:
            return getString(R.string.strLockNameSIM);
        default:
            return getString(R.string.simmelock_name);

        }
    }

    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    protected Dialog onCreateDialog(int id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false).setTitle(R.string.strAttention).setIcon(android.R.drawable.ic_dialog_alert).setMessage(
                R.string.srtConfirmPermanUnlock).setOnKeyListener(this).setPositiveButton(R.string.strConfirm,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface arg0, int arg1) {
                        // whether some lock category is disabled?
                        Message callback = Message.obtain(mHandler, PERREMOVELOCK_ICC_SML_COMPLETE);

                        // PermanUnlock a lock
                        if (!FeatureOption.MTK_GEMINI_SUPPORT) {
                            // Single SIM: directly permanently remove a lock
                            // Single Card:
                            Phone phone = PhoneFactory.getDefaultPhone();
                            phone.getIccCard().setIccNetworkLockEnabled(lockCategory, 4, null, null, null, null, callback);
                        } else {
                            // Gemini SIM:permanently remove the given
                            // SIM 's lock

                            // Framework to do permanently remove action
                            GeminiPhone mGeminiPhone = (GeminiPhone) PhoneFactory.getDefaultPhone();
                            intSIMNumber = bundle.getInt("SIMNo");
                            if (intSIMNumber == 0) {
                                mGeminiPhone.getIccCardGemini(PhoneConstants.GEMINI_SIM_1).setIccNetworkLockEnabled(lockCategory, 4,
                                        null, null, null, null, callback);
                                // editPwdProcess(et, "12345678");
                                // //compare with the true password
                                // "lockPassword"

                            } else {
                                mGeminiPhone.getIccCardGemini(PhoneConstants.GEMINI_SIM_2).setIccNetworkLockEnabled(lockCategory, 4,
                                        null, null, null, null, callback);
                                // editPwdProcess(et, "12345678");
                                // //compare with the true password
                                // "lockPassword"

                            }
                        }
                        finish();
                    }
                }).setNegativeButton(R.string.strCancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                // PermanUnlockSetting.this.finish();
                mHandlerFinish.sendEmptyMessage(777);
                // finish();
            }
        }).show();

        return super.onCreateDialog(id);
    }

    public boolean onKey(DialogInterface arg0, int arg1, KeyEvent arg2) {
        if (arg2.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            arg0.dismiss();
            finish();
            return true;
        }
        return false;
    }

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            AsyncResult ar = (AsyncResult) msg.obj;
            switch (msg.what) {
            case PERREMOVELOCK_ICC_SML_COMPLETE:
                if (ar.exception != null) {
                    // Toast.makeText(PermanUnlockSetting.this,
                    // "PermanUnlock lock fail", Toast.LENGTH_LONG).show();
                } else {
                    // Toast.makeText(PermanUnlockSetting.this,
                    // "PermanUnlock lock succeed!", Toast.LENGTH_LONG).show();
                    PermanUnlockSetting.this.finish();
                }
                break;
            default:
                break;
            }
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
                finish();
            }
        }
    };
    /******************************************/
    /*** values list ***/
    /******************************************/
    static int DIALOG_PERMANUNLOCK = 2;
    private String lockName = null;
    int intSIMNumber = 0;
    Bundle bundle = null;
    int lockCategory = -1;

    private static final int PERREMOVELOCK_ICC_SML_COMPLETE = 120;

}
