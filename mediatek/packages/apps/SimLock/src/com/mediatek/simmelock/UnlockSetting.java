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
import android.content.DialogInterface.OnDismissListener;
import android.content.res.Configuration;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.telephony.gemini.GeminiPhone;
import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneBase;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.mediatek.common.featureoption.FeatureOption;

public class UnlockSetting extends Activity implements DialogInterface.OnKeyListener {

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.unlocksetting);

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
        Log.i("SIMMELOCK", "lockName: " + lockName + "    || lockCategory: " + lockCategory);

        String localName = bundle.getString("LOCALNAME");
        Configuration conf = getResources().getConfiguration();
        String locale = conf.locale.getDisplayName(conf.locale);
        Log.i("SIMMELOCK", "localName: " + localName + "    || getLocalClassName: " + locale);
        if (localName != null && !localName.equals(locale)) {
            finish();
            return;
        }
        // initial left password input chances
        etPwdLeftChances = (TextView) findViewById(R.id.idunlockEditInputChancesleft);
        if (etPwdLeftChances == null) {
            Log.e("X", "clocwork worked...");
            // not return and let exception happened.
        }

        // set the regulation of EditText
        etPwd = (EditText) findViewById(R.id.idunlockEditInputPassword);
        if (etPwd == null) {
            Log.e("X", "clocwork worked...");
            // not return and let exception happened.
        }
        etPwd.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        SMLCommonProcess.limitEditTextPassword(etPwd, 8);

        Button btnConfirm = (Button) findViewById(R.id.idunlockButtonConfirm);
        if (btnConfirm == null) {
            Log.e("X", "clocwork worked...");
            // not return and let exception happened.
        }
        // Yu for ICS
        // btnConfirm.setHighFocusPriority(true);
        btnConfirm.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View view) {
                // editPwdProcess(et, lockPassword);
                // whether some lock category is disabled?
                Log.i("SIMMELOCK", "clickFlag: " + clickFlag);
                if (clickFlag) {
                    return;
                } else {
                    clickFlag = true;
                }

                if ((etPwd.getText().length() < 4) || ((etPwd.getText().length() > 8))) {
                    showAlertDialog(DIALOG_PASSWORDLENGTHINCORRECT);
                } else {
                    // get the left chances to unlock(less than 5)
                    // if it single SIM, just show a lock list
                    if (!FeatureOption.MTK_GEMINI_SUPPORT) {
                        Message callback = Message.obtain(mHandler, UNLOCK_ICC_SML_COMPLETE);
                        // Single Card:
                        Phone phone = PhoneFactory.getDefaultPhone();
                        phone.getIccCard().setIccNetworkLockEnabled(lockCategory, 0, etPwd.getText().toString(), null, null,
                                null, callback);
                    } else {
                        intSIMNumber = bundle.getInt("SIMNo");
                        Log.i("SIMMELOCK", "[btnconfirm]intSIMNumber: " + intSIMNumber);
                        GeminiPhone mGeminiPhone = (GeminiPhone) PhoneFactory.getDefaultPhone();
                        if (intSIMNumber == 0) {
                            Message callback = Message.obtain(mHandler, UNLOCK_ICC_SML_COMPLETE);
                            // editPwdProcess(et, "12345678"); //compare with
                            // the true password "lockPassword"
                            mGeminiPhone.getIccCardGemini(PhoneConstants.GEMINI_SIM_1).setIccNetworkLockEnabled(lockCategory, 0,
                                    etPwd.getText().toString(), null, null, null, callback);
                        } else {
                            Message callback = Message.obtain(mHandler, UNLOCK_ICC_SML_COMPLETE);

                            // editPwdProcess(et, "12345678"); //compare with
                            // the true password "lockPassword"
                            mGeminiPhone.getIccCardGemini(PhoneConstants.GEMINI_SIM_2).setIccNetworkLockEnabled(lockCategory, 0,
                                    etPwd.getText().toString(), null, null, null, callback);
                        }
                    }
                }
            }
        });

        Button btnCancel = (Button) findViewById(R.id.idunlockButtonCancel);
        if (btnCancel == null) {
            Log.e("X", "clocwork worked...");
            // not return and let exception happened.
        }
        btnCancel.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View view) {
                UnlockSetting.this.finish();
            }
        });

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

    protected void onResume() {
        super.onResume();

        Message callback3 = Message.obtain(mHandler, UNLOCK_ICC_SML_QUERYLEFTTIMES);
        if (!FeatureOption.MTK_GEMINI_SUPPORT) {
            // Single Card:
            Log.i("SIMMELOCK", "single: ");
            Phone phone = PhoneFactory.getDefaultPhone();
            phone.getIccCard().QueryIccNetworkLock(lockCategory, 0, null, null, null, null, callback3);

        } else {
            intSIMNumber = bundle.getInt("SIMNo");
            Log.i("SIMMELOCK", "intSIMNumber: " + intSIMNumber);
            GeminiPhone mGeminiPhone = (GeminiPhone) PhoneFactory.getDefaultPhone();
            if (intSIMNumber == 0) {
                mGeminiPhone.getIccCardGemini(PhoneConstants.GEMINI_SIM_1).QueryIccNetworkLock(lockCategory, 0, null, null, null,
                        null, callback3);

            } else {
                mGeminiPhone.getIccCardGemini(PhoneConstants.GEMINI_SIM_2).QueryIccNetworkLock(lockCategory, 0, null, null, null,
                        null, callback3);

            }
        }
    }

    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    protected void showAlertDialog(int id) {

        if (id == DIALOG_UNLOCKFAILED || id == DIALOG_PASSWORDLENGTHINCORRECT || id == DIALOG_UNLOCKSUCCEED
                || id == DIALOG_QUERYFAIL) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            switch (id) {
            case DIALOG_UNLOCKFAILED: // password is wrong
                Log.i("SIMMELOCK", "show DIALOG_UNLOCKFAILED");
                builder.setCancelable(false).setTitle(R.string.strAttention).setIcon(android.R.drawable.ic_dialog_alert)
                        .setMessage(R.string.strUnlockFail).setOnKeyListener(this).setNegativeButton(R.string.strYes,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        // etPwdLeftChances.setText(String.valueOf(mPwdLeftChances));
                                        Log.i("SIMMELOCK", "query mPwdLeftChances: " + mPwdLeftChances);
                                        etPwd.setText("");
                                        dialog.cancel();
                                        clickFlag = false;
                                    }
                                }).show();
                break;
            case DIALOG_PASSWORDLENGTHINCORRECT:// Length is incorrect
                Log.i("SIMMELOCK", "show DIALOG_PASSWORDLENGTHINCORRECT");
                builder.setCancelable(false).setTitle(R.string.strAttention).setIcon(android.R.drawable.ic_dialog_alert)
                        .setMessage(R.string.strPasswordLengthIncorrect).setOnKeyListener(this).setNegativeButton(
                                R.string.strYes, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        etPwd.setText("");
                                        dialog.cancel();
                                        clickFlag = false;
                                    }
                                }).show();
                break;
            case DIALOG_UNLOCKSUCCEED:// Length is incorrect
                Log.i("SIMMELOCK", "show DIALOG_UNLOCKSUCCEED");
                AlertDialog succeedDialog = builder.setCancelable(false).setTitle(R.string.strAttention).setIcon(
                        android.R.drawable.ic_dialog_alert).setMessage(R.string.strUnlockSucceed).setOnKeyListener(this)
                        .setPositiveButton(R.string.strYes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                if (id == AlertDialog.BUTTON_POSITIVE) {
                                    // etPwd.setText("");
                                    Log.i("SIMMELOCK", "Success dialog UnlockSetting dismissed.");
                                    clickFlag = false;
                                    try {
                                        if (null != dialog) {
                                            dialog.cancel();
                                        }
                                    } catch (IllegalArgumentException e) {
                                        Log.e("SIMMELOCK", "Catch IllegalArgumentException");
                                    }
                                    finish();
                                    Log.i("SIMMELOCK", "Success dialog dismissed.");
                                }
                            }
                        }).create();
                succeedDialog.setOnDismissListener(new OnDismissListener() {

                    public void onDismiss(DialogInterface dialog) {
                        Log.i("SIMMELOCK", "setOnDismissListenerX");
                        // UnlockSetting.this.finish();
                        // clickFlag = false;
                    }
                });
                succeedDialog.show();
                break;
            case DIALOG_QUERYFAIL:// Length is incorrect
                Log.i("SIMMELOCK", "show DIALOG_QUERYFAIL");
                builder.setCancelable(false).setTitle(R.string.strAttention).setIcon(android.R.drawable.ic_dialog_alert)
                        .setMessage(R.string.strQueryFailed).setOnKeyListener(this).setNegativeButton(R.string.strYes,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        etPwd.setText("");
                                        dialog.cancel();
                                        clickFlag = false;
                                    }
                                }).show();
                break;
            }
        }
        Log.i("SIMMELOCK", "show null");
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
            case UNLOCK_ICC_SML_COMPLETE: {
                if (clickFlag == true) {
                    Log.i("SIMMELOCK", "set ar: " + ar.exception);
                    if (ar.exception != null)// fail to unlock
                    {
                        // get the left chances to unlock(less than 5)
                        // if it single SIM, just show a lock list
                        if (!FeatureOption.MTK_GEMINI_SUPPORT) {
                            // Single Card:
                            Message callback_query = Message.obtain(mHandler, UNLOCK_ICC_SML_QUERYLEFTTIMES);
                            Phone phone = PhoneFactory.getDefaultPhone();
                            phone.getIccCard().QueryIccNetworkLock(lockCategory, 0, null, null, null, null, callback_query);
                            // phone.getIccCard().setIccNetworkLockEnabled(lockCategory,0,etPwd.getText().toString(),null,null,null,callback);
                        } else {
                            intSIMNumber = bundle.getInt("SIMNo");
                            Log.i("SIMMELOCK", "[btnconfirm]intSIMNumber: " + intSIMNumber);
                            GeminiPhone mGeminiPhone = (GeminiPhone) PhoneFactory.getDefaultPhone();
                            if (intSIMNumber == 0) {
                                Message callback_query = Message.obtain(mHandler, UNLOCK_ICC_SML_QUERYLEFTTIMES);
                                mGeminiPhone.getIccCardGemini(PhoneConstants.GEMINI_SIM_1).QueryIccNetworkLock(lockCategory, 0, null,
                                        null, null, null, callback_query);
                                // editPwdProcess(et, "12345678"); //compare
                                // with the true password "lockPassword"
                                // mGeminiPhone.getIccCardGemini(PhoneConstants.GEMINI_SIM_1).setIccNetworkLockEnabled(lockCategory,0,etPwd.getText().toString(),null,null,null,callback);
                            } else {
                                Message callback_query = Message.obtain(mHandler, UNLOCK_ICC_SML_QUERYLEFTTIMES);
                                mGeminiPhone.getIccCardGemini(PhoneConstants.GEMINI_SIM_2).QueryIccNetworkLock(lockCategory, 0, null,
                                        null, null, null, callback_query);
                                // editPwdProcess(et, "12345678"); //compare
                                // with the true password "lockPassword"
                                // mGeminiPhone.getIccCardGemini(PhoneConstants.GEMINI_SIM_2).setIccNetworkLockEnabled(lockCategory,0,etPwd.getText().toString(),null,null,null,callback);
                            }

                        }
                        showAlertDialog(DIALOG_UNLOCKFAILED);
                    } else {
                        showAlertDialog(DIALOG_UNLOCKSUCCEED);
                    }
                    Log.i("SIMMELOCK", "handler UNLOCK_ICC_SML_COMPLETE mPwdLeftChances: " + mPwdLeftChances);
                }
                break;
            }

            case UNLOCK_ICC_SML_QUERYLEFTTIMES: {
                Log.i("SIMMELOCK", "handler query");
                Log.i("SIMMELOCK", "query ar: " + ar.exception);
                if (ar.exception != null) {
                    showAlertDialog(DIALOG_QUERYFAIL);// Query fail!
                } else {
                    AsyncResult ar1 = (AsyncResult) msg.obj;
                    int[] LockState = (int[]) ar1.result;
                    if (LockState[2] > 0) {
                        // still have chances to unlock
                        mPwdLeftChances = LockState[2];
                        etPwdLeftChances.setText(String.valueOf(mPwdLeftChances));
                        Log.i("SIMMELOCK", "query mPwdLeftChances: " + mPwdLeftChances);
                    } else {
                        UnlockSetting.this.finish();
                    }
                }
                break;
            }

            }
        }
    };

    EditText etPwd = null;
    TextView etPwdLeftChances = null;
    private String lockName = null;
    private boolean unlockPwdCorrect = false;
    private int mPwdLeftChances = 5;

    static final int DIALOG_PASSWORDLENGTHINCORRECT = 1;
    static final int DIALOG_UNLOCKSUCCEED = 2;
    static final int DIALOG_UNLOCKFAILED = 3;
    static final int DIALOG_QUERYFAIL = 4;

    int lockCategory = 0;
    int intSIMNumber = 0;
    Bundle bundle = null;
    PhoneBase phone = null;
    private boolean clickFlag = false;

    private static final int UNLOCK_ICC_SML_COMPLETE = 120;
    private static final int UNLOCK_ICC_SML_QUERYLEFTTIMES = 110;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
                finish();
            }
        }
    };

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.i("SIMMELOCK", "[UnlckSetting]onConfigurationChanged ");
        super.onConfigurationChanged(newConfig);
    }
}
