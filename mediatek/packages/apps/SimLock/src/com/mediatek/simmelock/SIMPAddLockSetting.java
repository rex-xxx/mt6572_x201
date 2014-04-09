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
import android.telephony.TelephonyManager;//To invoke the telephony function
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.android.internal.telephony.gemini.GeminiPhone;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneBase;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.IccCard;
import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.telephony.TelephonyManagerEx;

public class SIMPAddLockSetting extends Activity implements DialogInterface.OnKeyListener {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.simpaddlocksetting);

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

        etIMSI = (EditText) findViewById(R.id.idsimpaddlockEditInputIMSI);
        if (etIMSI == null) {
            Log.e("X", "clocwork worked...");
            // not return and let exception happened.
        }
        // Let the user to choose "put in" for just read MCCMNC from the SIM
        // card
        s1 = (Spinner) findViewById(R.id.spinnersimp1);
        if (s1 == null) {
            Log.e("X", "clocwork worked...");
            // not return and let exception happened.
        }
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.Input_mode,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s1.setAdapter(adapter);
        AdapterView.OnItemSelectedListener l = new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                if (s1.getSelectedItem().toString().equals(getString(R.string.strUserInput))) {
                    etIMSI.setVisibility(View.VISIBLE);
                    etIMSI.requestFocus();
                    // set the regulation of EditText
                    SMLCommonProcess.limitEditText(etIMSI, 15);
                    mbIMSIReadSIM1 = false;
                    mbIMSIReadSIM2 = false;
                    mbIMSIReadSIM = false;
                } else {
                    mbIMSIReadSIM1 = true;
                    mbIMSIReadSIM2 = true;
                    mbIMSIReadSIM = true;
                    etIMSI.setVisibility(View.GONE);
                }
            }

            public void onNothingSelected(AdapterView<?> arg0) {

            }
        };
        s1.setOnItemSelectedListener(l);

        etPwd = (EditText) findViewById(R.id.idsimpaddlockEditInputPassword);
        if (etPwd == null) {
            Log.e("X", "clocwork worked...");
            // not return and let exception happened.
        }
        etPwd.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        SMLCommonProcess.limitEditTextPassword(etPwd, 8);
        etPwdConfirm = (EditText) findViewById(R.id.idsimpaddlockEditInputPasswordAgain);
        if (etPwdConfirm == null) {
            Log.e("X", "clocwork worked...");
            // not return and let exception happened.
        }
        etPwdConfirm.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        SMLCommonProcess.limitEditTextPassword(etPwdConfirm, 8);

        // Press the CONFIRM Button
        Button btnConfirm = (Button) findViewById(R.id.idsimpaddlockButtonConfirm);
        if (btnConfirm == null) {
            Log.e("X", "clocwork worked...");
            // not return and let exception happened.
        }
        // Yu for ICS
        // btnConfirm.setHighFocusPriority(true);
        btnConfirm.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View view) {
                Log.i("SIMMELOCK", "clickFlag: " + clickFlag);
                if (clickFlag) {
                    return;
                } else {
                    clickFlag = true;
                }
                // do something after verify all the input things,for example:
                // write a password to the modem
                if (((false == mbIMSIReadSIM) && (false == mbIMSIReadSIM1) && (false == mbIMSIReadSIM1))
                        && ((15 > etIMSI.getText().length()))) {
                    showDialog(DIALOG_IMSILENGTHINCORRECT);
                } else if ((etPwd.getText().length() < 4) || ((etPwd.getText().length() > 8))) {
                    showDialog(DIALOG_PASSWORDLENGTHINCORRECT);
                }
                // else if ((etPwdConfirm.getText().length() < 4) ||
                // ((etPwdConfirm.getText().length() > 8)))
                // {
                // showDialog(DIALOG_PASSWORDLENGTHINCORRECT);
                // }
                else if (etPwd.getText().toString().equals(etPwdConfirm.getText().toString()) == false) {
                    showDialog(DIALOG_PASSWORDWRONG);
                } else {
                    // whether some lock category is disabled?
                    Message callback = Message.obtain(mHandler, ADDLOCK_ICC_SML_COMPLETE);
                    // if it single SIM, just show a lock list
                    if (!FeatureOption.MTK_GEMINI_SUPPORT) {
                        // Single Card:
                        Phone phone = PhoneFactory.getDefaultPhone();
                        if (!mbIMSIReadSIM) {
                            phone.getIccCard().setIccNetworkLockEnabled(4, 2, etPwd.getText().toString(),
                                    etIMSI.getText().toString(), null, null, callback);
                        } else {
                            phone.getIccCard().setIccNetworkLockEnabled(4, 2, etPwd.getText().toString(), msSIMIMSI, null,
                                    null, callback);
                        }

                    } else {
                        GeminiPhone mGeminiPhone = (GeminiPhone) PhoneFactory.getDefaultPhone();
                        intSIMNumber = bundle.getInt("SIMNo");
                        if (intSIMNumber == 0) {
                            if (!mbIMSIReadSIM1) {
                                mGeminiPhone.getIccCardGemini(PhoneConstants.GEMINI_SIM_1).setIccNetworkLockEnabled(4, 2,
                                        etPwd.getText().toString(), etIMSI.getText().toString(), null, null, callback);
                                // editPwdProcess(et, "12345678"); //compare
                                // with the true password "lockPassword"
                            } else {
                                mGeminiPhone.getIccCardGemini(PhoneConstants.GEMINI_SIM_1).setIccNetworkLockEnabled(4, 2,
                                        etPwd.getText().toString(), msSIM1IMSI, null, null, callback);
                                // editPwdProcess(et, "12345678"); //compare
                                // with the true password "lockPassword"
                            }

                        } else {
                            if (!mbIMSIReadSIM2) {
                                mGeminiPhone.getIccCardGemini(PhoneConstants.GEMINI_SIM_2).setIccNetworkLockEnabled(4, 2,
                                        etPwd.getText().toString(), etIMSI.getText().toString(), null, null, callback);
                                // editPwdProcess(et, "12345678"); //compare
                                // with the true password "lockPassword"
                            } else {
                                mGeminiPhone.getIccCardGemini(PhoneConstants.GEMINI_SIM_2).setIccNetworkLockEnabled(4, 2,
                                        etPwd.getText().toString(), msSIM2IMSI, null, null, callback);
                                // editPwdProcess(et, "12345678"); //compare
                                // with the true password "lockPassword"
                            }
                        }
                    }
                }
            }
        });

        // Press the CANCEL Button
        Button btnCancel = (Button) findViewById(R.id.idsimpaddlockButtonCancel);
        if (btnCancel == null) {
            Log.e("X", "clocwork worked...");
            // not return and let exception happened.
        }
        btnCancel.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View view) {
                // do nothing to quit the edit page

                SIMPAddLockSetting.this.finish();
            }
        });

        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        registerReceiver(mReceiver, intentFilter);
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
        
        /*Gemini API refactor*/
        TelephonyManagerEx telephonyManagerEx = TelephonyManagerEx.getDefault();
        
        // To get the IMSI from SIM card
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        // To get the MCC+MNC from SIM card
        if (!FeatureOption.MTK_GEMINI_SUPPORT) {
            miSIMState = telephonyManager.getSimState();
            if (TelephonyManager.SIM_STATE_ABSENT == miSIMState) {
                // SIM1 not ready!
            } else {
                msSIMIMSI = telephonyManager.getSubscriberId();
                if (msSIMIMSI == null) {
                    Log.i("SIMMELOCK", "Fail to read SIM IMSI!");
                    // Toast.makeText(this,"Fail to read SIM IMSI",Toast.LENGTH_LONG).show();
                } else {
                    Log.i("SIMMELOCK", "Succeed to read SIM IMSI!");
                }
            }
        } else {
            msSIM1IMSI = telephonyManagerEx.getSubscriberId(0);// get card 1
            // IMSI code
            msSIM2IMSI = telephonyManagerEx.getSubscriberId(1);// get card 2
            // IMSI code
            // Toast.makeText(this, strIMSI, Toast.LENGTH_LONG);
            if (msSIM1IMSI == null) {
                Log.i("SIMMELOCK", "Fail to read SIM1 IMSI!");
                // Toast.makeText(this,"Fail to read SIM1 IMSI",Toast.LENGTH_LONG).show();
            } else {
                Log.i("SIMMELOCK", "Succeed to read SIM1 IMSI!");
            }
            if (msSIM2IMSI == null) {
                Log.i("SIMMELOCK", "Fail to read SIM2 IMSI!");
                // Toast.makeText(this,"Fail to read SIM2 IMSI",Toast.LENGTH_LONG).show();
            } else {
                Log.i("SIMMELOCK", "Succeed to read SIM2 IMSI!");
            }
        }
    }

    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    protected Dialog onCreateDialog(int id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        switch (id) {
        case DIALOG_ADDLOCKFAIL: // password is wrong
            builder.setCancelable(false).setTitle(R.string.strAttention).setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage(R.string.strAddLockFail).setOnKeyListener(this).setPositiveButton(R.string.strYes,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    etIMSI.setText("");
                                    etPwd.setText("");
                                    etPwdConfirm.setText("");
                                    dialog.cancel();
                                    clickFlag = false;
                                }
                            }).show();
            break;
        case DIALOG_PASSWORDLENGTHINCORRECT:// Length is incorrect
            builder.setCancelable(false).setTitle(R.string.strAttention).setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage(R.string.strPasswordLengthIncorrect).setOnKeyListener(this).setPositiveButton(
                            R.string.strYes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    etPwd.setText("");
                                    etPwdConfirm.setText("");
                                    dialog.cancel();
                                    clickFlag = false;
                                }
                            }).show();
            break;
        case DIALOG_IMSILENGTHINCORRECT:// Length is incorrect
            builder.setCancelable(false).setTitle(R.string.strAttention).setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage(R.string.strIMSILengthIncorrect).setOnKeyListener(this).setPositiveButton(R.string.strYes,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    etIMSI.setText("");
                                    dialog.cancel();
                                    clickFlag = false;
                                }
                            }).show();
            break;
        case DIALOG_ADDLOCKSUCCEED:// Length is incorrect
            builder.setCancelable(false).setTitle(R.string.strAttention).setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage(R.string.strAddLockSucceed).setOnKeyListener(this).setPositiveButton(R.string.strYes,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    etIMSI.setText("");
                                    etPwd.setText("");
                                    etPwdConfirm.setText("");
                                    dialog.cancel();
                                    clickFlag = false;
                                    SIMPAddLockSetting.this.finish();
                                }
                            }).show();

            break;
        case DIALOG_PASSWORDWRONG:// Length is incorrect
            builder.setCancelable(false).setTitle(R.string.strAttention).setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage(R.string.str_simme_passwords_dont_match).setOnKeyListener(this).setPositiveButton(
                            R.string.strYes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    etPwd.setText("");
                                    etPwdConfirm.setText("");
                                    dialog.cancel();
                                    clickFlag = false;
                                }
                            }).show();
            break;
        default:
            break;
        }
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
            case ADDLOCK_ICC_SML_COMPLETE:
                if (ar.exception != null) {
                    showDialog(DIALOG_ADDLOCKFAIL);
                } else {
                    showDialog(DIALOG_ADDLOCKSUCCEED);
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
    Bundle bundle = null;
    EditText etIMSI = null;
    EditText etPwd = null;
    EditText etPwdConfirm = null;
    private String lockName = null;
    private int lockCategory = -1;
    int intSIMNumber = 0;
    Spinner s1;
    String msSIM1IMSI = null;
    String msSIM2IMSI = null;
    String msSIMIMSI = null;
    int miSIMState = 0;
    boolean mbIMSIReadSIM1 = false;// whether to read IMSI from SIM1
    // card,default to set false
    boolean mbIMSIReadSIM2 = false;// whether to read IMSI from SIM2
    // card,default to set false
    boolean mbIMSIReadSIM = false;
    boolean mbIMSICorrect = false;
    private boolean clickFlag = false;

    final int DIALOG_IMSILENGTHINCORRECT = 1;
    final int DIALOG_ADDLOCKFAIL = 2;
    final int DIALOG_ADDLOCKSUCCEED = 3;
    final int DIALOG_PASSWORDLENGTHINCORRECT = 4;
    final int DIALOG_PASSWORDWRONG = 5;

    private static final int ADDLOCK_ICC_SML_COMPLETE = 120;
}
