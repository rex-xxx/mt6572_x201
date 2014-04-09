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

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneBase;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.gemini.GeminiPhone;
import com.android.internal.telephony.IccCard;
import com.mediatek.common.featureoption.FeatureOption;

public class LockSetting extends Activity implements DialogInterface.OnKeyListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.locksetting);

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
        // set the regulation of EditText
        et = (EditText) findViewById(R.id.idEditInputPassword);
        if (et == null) {
            Log.e("X", "clocwork worked...");
            // not return and let exception happened.
        }
        et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        SMLCommonProcess.limitEditTextPassword(et, 8);
        if (lockCategory == 0 || lockCategory == 1 || lockCategory == 2 || lockCategory == 3 || lockCategory == 4) {
            TextView t = (TextView) findViewById(R.id.idInputPasswordAgain);
            if (t == null) {
                Log.e("X", "clocwork worked...");
                // not return and let exception happened.
            }
            t.setVisibility(View.VISIBLE);
            re_et = (EditText) findViewById(R.id.idEditInputPasswordAgain);
            if (re_et == null) {
                Log.e("X", "clocwork worked...");
                // not return and let exception happened.
            }
            re_et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            SMLCommonProcess.limitEditTextPassword(re_et, 8);
            re_et.setVisibility(View.VISIBLE);
        }

        Button btnConfirm = (Button) findViewById(R.id.idButtonConfirm);
        if (btnConfirm == null) {
            Log.e("X", "clocwork worked...");
            // not return and let exception happened.
        }
        // Yu for ICS
        // btnConfirm.setHighFocusPriority(true);
        btnConfirm.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View view) {
                // whether some lock category is disabled?
                // make sure the password's length is correct
                // make sure the length of inputed password is 4 to 8
                Log.i("SIMMELOCK", "clickFlag: " + clickFlag);
                if (clickFlag) {
                    return;
                } else {
                    clickFlag = true;
                }
                if ((et.getText().length() < 4) || ((et.getText().length() > 8))) {
                    showAlertDialog(DIALOG_PASSWORDLENGTHINCORRECT);
                    return;
                }
                if (lockCategory == 0 || lockCategory == 1 || lockCategory == 2 || lockCategory == 3 || lockCategory == 4) {
                    if ((et.getText().toString().equals(re_et.getText().toString())) == false) {
                        // Log.i("SIMMELOCK","[LockSetting]origin is"+et.getText().toString());
                        // Log.i("SIMMELOCK","[LockSetting]confirm is"+re_et.getText().toString());
                        // Log.i("SIMMELOCK","[LockSetting]Equal ? "+(et.getText().toString().equals(re_et.getText().toString())));
                        showAlertDialog(DIALOG_PASSWORDWRONG);
                        return;
                    }
                }
                Message callback = Message.obtain(mHandler, LOCK_ICC_SML_COMPLETE);
                // if it single SIM, just show a lock list
                if (!FeatureOption.MTK_GEMINI_SUPPORT) {
                    // Single Card:
                    Phone phone = PhoneFactory.getDefaultPhone();
                    phone.getIccCard().setIccNetworkLockEnabled(lockCategory, 1, et.getText().toString(), null, null, null,
                            callback);
                } else {
                    intSIMNumber = bundle.getInt("SIMNo");
                    Log.i("SIMMELOCK", "[LockSetting]intSIMNumber is" + intSIMNumber);
                    GeminiPhone mGeminiPhone = (GeminiPhone) PhoneFactory.getDefaultPhone();
                    if (intSIMNumber == 0) {
                        mGeminiPhone.getIccCardGemini(PhoneConstants.GEMINI_SIM_1).setIccNetworkLockEnabled(lockCategory, 1,
                                et.getText().toString(), null, null, null, callback);
                        // editPwdProcess(et, "12345678"); //compare with the
                        // true password "lockPassword"
                    } else {
                        mGeminiPhone.getIccCardGemini(PhoneConstants.GEMINI_SIM_2).setIccNetworkLockEnabled(lockCategory, 1,
                                et.getText().toString(), null, null, null, callback);
                        // editPwdProcess(et, "12345678"); //compare with the
                        // true password "lockPassword"

                    }
                }
            }
        });

        Button btnCancel = (Button) findViewById(R.id.idButtonCancel);
        btnCancel.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View view) {
                LockSetting.this.finish();
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

    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    protected void showAlertDialog(int id) {
        if (id == DIALOG_LOCKFAIL || id == DIALOG_PASSWORDLENGTHINCORRECT || id == DIALOG_LOCKSUCCEED
                || id == DIALOG_PASSWORDWRONG) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            switch (id) {
            case DIALOG_LOCKFAIL: // password is wrong
                builder.setCancelable(false).setTitle(R.string.strAttention).setIcon(android.R.drawable.ic_dialog_alert)
                        .setMessage(R.string.strLockFail).setOnKeyListener(this).setNegativeButton(R.string.strYes,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        et.setText("");
                                        if (re_et != null){
                                            re_et.setText("");
                                        }                                      
                                        clickFlag = false;
                                        dialog.cancel();
                                    }
                                }).show();
                break;
            case DIALOG_PASSWORDLENGTHINCORRECT:// Length is incorrect
                builder.setCancelable(false).setTitle(R.string.strAttention).setIcon(android.R.drawable.ic_dialog_alert)
                        .setMessage(R.string.strPasswordLengthIncorrect).setOnKeyListener(this).setNegativeButton(
                                R.string.strYes, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        et.setText("");
                                        if (re_et != null) {
                                            re_et.setText("");
                                        }
                                        clickFlag = false;
                                        dialog.cancel();
                                    }
                                }).show();
                break;
            case DIALOG_LOCKSUCCEED:// Length is incorrect
                builder.setCancelable(false).setTitle(R.string.strAttention).setIcon(android.R.drawable.ic_dialog_alert)
                        .setMessage(R.string.strLockSucceed).setOnKeyListener(this).setPositiveButton(R.string.strYes,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        et.setText("");
                                        if (re_et != null) {
                                            re_et.setText("");
                                        }                                       
                                        dialog.cancel();
                                        clickFlag = false;
                                        LockSetting.this.finish();
                                    }
                                }).show();
                break;
            case DIALOG_PASSWORDWRONG:// passwords donot match
                builder.setCancelable(false).setTitle(R.string.strAttention).setIcon(android.R.drawable.ic_dialog_alert)
                        .setMessage(R.string.str_simme_passwords_dont_match).setOnKeyListener(this).setNegativeButton(
                                R.string.strYes, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        et.setText("");
                                        if (re_et != null) {
                                            re_et.setText("");
                                        }                                           
                                        dialog.cancel();
                                        et.requestFocus();
                                        clickFlag = false;
                                    }
                                }).show();
                break;
            }
        }
    }

    @Override
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
            case LOCK_ICC_SML_COMPLETE:
                if (ar.exception != null) {
                    // Toast.makeText(LockSetting.this, "lock fail",
                    // Toast.LENGTH_LONG).show();
                    showAlertDialog(DIALOG_LOCKFAIL);
                } else {
                    // Toast.makeText(LockSetting.this, "lock Succeed!",
                    // Toast.LENGTH_LONG).show();
                    showAlertDialog(DIALOG_LOCKSUCCEED);
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

    EditText et = null;
    EditText re_et = null;
    private String lockName = null;
    private String lockPassword = null;// the true password string which need to
    // be compared with the input string
    private boolean lockPwdCorrect = false;
    final int DIALOG_LOCKFAIL = 3;
    final int DIALOG_PASSWORDLENGTHINCORRECT = 1;
    final int DIALOG_LOCKSUCCEED = 2;
    final int DIALOG_PASSWORDWRONG = 4;
    int lockCategory = 0;
    int intSIMNumber = 0;
    Bundle bundle = null;
    PhoneBase phone = null;
    private boolean clickFlag = false;

    private static final int LOCK_ICC_SML_COMPLETE = 120;
}
