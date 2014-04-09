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
import android.telephony.TelephonyManager;
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
import com.android.internal.telephony.PhoneBase;
import com.android.internal.telephony.IccFileHandler;
import com.android.internal.telephony.IccConstants;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.IccUtils;
import com.android.internal.telephony.PhoneProxy;
import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.telephony.TelephonyManagerEx;

public class CPAddLockSetting extends Activity implements DialogInterface.OnKeyListener {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cpaddlocksetting);

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
        etMCCMNC = (EditText) findViewById(R.id.idcpaddlockEditInputMCCMNC);
        etGID1 = (EditText) findViewById(R.id.idcpaddlockEditInputGID1);
        etGID2 = (EditText) findViewById(R.id.idcpaddlockEditInputGID2);
        etPwd = (EditText) findViewById(R.id.idcpaddlockEditInputPassword);
        // Let the user to choose "put in" for just read MCCMNC from the SIM
        // card
        s1 = (Spinner) findViewById(R.id.spinnercp1);
        s2 = (Spinner) findViewById(R.id.spinnercp2);
        s3 = (Spinner) findViewById(R.id.spinnercp3);
        etPwdConfirm = (EditText) findViewById(R.id.idcpaddlockEditInputPasswordAgain);
        // Press the CONFIRM Button
        Button btnConfirm = (Button) findViewById(R.id.idcpaddlockButtonConfirm);
        Button btnCancel = (Button) findViewById(R.id.idcpaddlockButtonCancel);

        if (etMCCMNC == null || etGID1 == null || etGID2 == null || etPwd == null || s1 == null || s2 == null || s3 == null
                || etPwdConfirm == null || btnConfirm == null || btnCancel == null) {
            Log.e("SIMMELOCK", "clocwork worked...");
            // not return and let exception happened.
        }

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.Input_mode,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s1.setAdapter(adapter);
        AdapterView.OnItemSelectedListener l = new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                if (s1.getSelectedItem().toString().equals(getString(R.string.strUserInput))) {
                    etMCCMNC.setVisibility(View.VISIBLE);
                    // etMCCMNC.requestFocus();
                    // set the regulation of EditText
                    SMLCommonProcess.limitEditText(etMCCMNC, 6);
                    mbMCCMNCReadSIM1 = false;
                    mbMCCMNCReadSIM2 = false;
                    mbMCCMNCReadSIM = false;
                } else {
                    mbMCCMNCReadSIM1 = true;
                    mbMCCMNCReadSIM2 = true;
                    mbMCCMNCReadSIM = true;
                    etMCCMNC.setVisibility(View.GONE);
                }
            }

            public void onNothingSelected(AdapterView<?> arg0) {

            }
        };
        s1.setOnItemSelectedListener(l);

        ArrayAdapter<CharSequence> adapter2 = ArrayAdapter.createFromResource(this, R.array.Input_mode,
                android.R.layout.simple_spinner_item);
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s2.setAdapter(adapter2);
        AdapterView.OnItemSelectedListener l2 = new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                if (s2.getSelectedItem().toString().equals(getString(R.string.strUserInput))) {
                    etGID1.setVisibility(View.VISIBLE);
                    // etGID1.requestFocus();
                    // set the regulation of EditText
                    SMLCommonProcess.limitEditText(etGID1, 3);
                    mbGID1ReadSIM1 = false;
                    mbGID1ReadSIM2 = false;
                    mbGID1ReadSIM = false;
                    intSIMGID1 = 1;
                } else {
                    mbGID1ReadSIM1 = true;
                    mbGID1ReadSIM2 = true;
                    mbGID1ReadSIM = true;
                    intSIMGID1 = 255;
                    etGID1.setVisibility(View.GONE);
                }
            }

            public void onNothingSelected(AdapterView<?> arg0) {

            }
        };
        s2.setOnItemSelectedListener(l2);

        ArrayAdapter<CharSequence> adapter3 = ArrayAdapter.createFromResource(this, R.array.Input_mode,
                android.R.layout.simple_spinner_item);
        adapter3.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s3.setAdapter(adapter3);
        AdapterView.OnItemSelectedListener l3 = new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                if (s3.getSelectedItem().toString().equals(getString(R.string.strUserInput))) {
                    etGID2.setVisibility(View.VISIBLE);
                    // etGID2.requestFocus();
                    // set the regulation of EditText
                    SMLCommonProcess.limitEditText(etGID2, 3);
                    mbGID2ReadSIM1 = false;
                    mbGID2ReadSIM2 = false;
                    mbGID2ReadSIM = false;
                    intSIMGID2 = 1;// ///////////////////////////////
                } else {
                    mbGID2ReadSIM = true;
                    mbGID2ReadSIM1 = true;
                    mbGID2ReadSIM2 = true;
                    intSIMGID2 = 255;
                    etGID2.setVisibility(View.GONE);
                }
            }

            public void onNothingSelected(AdapterView<?> arg0) {

            }
        };
        s3.setOnItemSelectedListener(l3);

        etPwd.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        SMLCommonProcess.limitEditTextPassword(etPwd, 8);

        etPwdConfirm.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        SMLCommonProcess.limitEditTextPassword(etPwdConfirm, 8);

        // Yu for ICS
        // btnConfirm.setHighFocusPriority(true);
        btnConfirm.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i("SIMMELOCK", "clickFlag: " + clickFlag);
                if (clickFlag) {
                    return;
                } else {
                    clickFlag = true;
                }
                if (((!mbMCCMNCReadSIM) && (!mbMCCMNCReadSIM1) && (!mbMCCMNCReadSIM2))
                        && ((5 > etMCCMNC.getText().length()) || (6 < etMCCMNC.getText().length()))) {
                    showDialog(DIALOG_MCCMNCLENGTHINCORRECT);
                } else if (etGID1.getText().length() < 1) {
                    showDialog(DIALOG_GID1WRONG);
                } else if (etGID2.getText().length() < 1) {
                    showDialog(DIALOG_GID2WRONG);
                } else if ((Integer.parseInt(etGID1.getText().toString()) < 0)
                        || (Integer.parseInt(etGID1.getText().toString()) > 254) || ((intSIMGID1 > 254) && mbGID1ReadSIM)) {
                    showDialog(DIALOG_GID1WRONG);
                } else if ((Integer.parseInt(etGID2.getText().toString()) < 0)
                        || (Integer.parseInt(etGID2.getText().toString()) > 254) || ((intSIMGID2 > 254) && mbGID2ReadSIM)) {
                    showDialog(DIALOG_GID2WRONG);
                } else if ((etPwd.getText().length() < 4) || ((etPwd.getText().length() > 8))) {
                    showDialog(DIALOG_PASSWORDLENGTHINCORRECT);
                }
                // else if ((etPwdConfirm.getText().length() < 4) ||
                // ((etPwdConfirm.getText().length() > 8)))
                // {
                // showDialog(DIALOG_PASSWORDLENGTHINCORRECT);
                // }
                else if (!etPwd.getText().toString().equals(etPwdConfirm.getText().toString())) {
                    showDialog(DIALOG_PASSWORDWRONG);
                } else {
                    // whether some lock category is disabled?
                    Message callback = Message.obtain(mHandler, ADDLOCK_ICC_SML_COMPLETE);
                    // if it single SIM, just show a lock list
                    if (!FeatureOption.MTK_GEMINI_SUPPORT) {
                        // Single Card:
                        Phone phone = PhoneFactory.getDefaultPhone();
                        if (!mbMCCMNCReadSIM) {
                            // user input
                            if (!mbGID1ReadSIM) {
                                if (!mbGID2ReadSIM) {
                                    phone.getIccCard().setIccNetworkLockEnabled(3, 2, etPwd.getText().toString(),
                                            etMCCMNC.getText().toString(), etGID1.getText().toString(),
                                            etGID2.getText().toString(), callback);
                                } else {
                                    phone.getIccCard().setIccNetworkLockEnabled(3, 2, etPwd.getText().toString(),
                                            etMCCMNC.getText().toString(), etGID1.getText().toString(), msSIMGID2, callback);
                                }

                            } else {
                                if (!mbGID2ReadSIM) {
                                    phone.getIccCard().setIccNetworkLockEnabled(3, 2, etPwd.getText().toString(),
                                            etMCCMNC.getText().toString(), msSIMGID2, etGID2.getText().toString(), callback);
                                } else {
                                    phone.getIccCard().setIccNetworkLockEnabled(3, 2, etPwd.getText().toString(),
                                            etMCCMNC.getText().toString(), msSIMGID2, msSIMGID2, callback);
                                }
                            }
                        } else {
                            if (!mbGID1ReadSIM) {
                                if (!mbGID2ReadSIM) {
                                    phone.getIccCard().setIccNetworkLockEnabled(3, 2, etPwd.getText().toString(),
                                            msSIMMCCMNC, etGID1.getText().toString(), etGID2.getText().toString(), callback);
                                } else {
                                    phone.getIccCard().setIccNetworkLockEnabled(3, 2, etPwd.getText().toString(),
                                            msSIMMCCMNC, etGID1.getText().toString(), msSIMGID2, callback);
                                }

                            } else {
                                if (!mbGID2ReadSIM) {
                                    phone.getIccCard().setIccNetworkLockEnabled(3, 2, etPwd.getText().toString(),
                                            msSIMMCCMNC, msSIMGID2, etGID2.getText().toString(), callback);
                                } else {
                                    phone.getIccCard().setIccNetworkLockEnabled(3, 2, etPwd.getText().toString(),
                                            msSIMMCCMNC, msSIMGID2, msSIMGID2, callback);
                                }
                            }
                        }
                    } else {
                        GeminiPhone mGeminiPhone = (GeminiPhone) PhoneFactory.getDefaultPhone();
                        // intSIMNumber = bundle.getInt("SIMNo");
                        if (intSIMNumber == 0) {
                            // mGeminiPhone.getIccCardGemini(PhoneConstants.GEMINI_SIM_1).setIccNetworkLockEnabled(0,3,etPwd.getText().toString(),etMCCMNC.getText().toString(),etGID1.getText().toString(),etGID2.getText().toString(),callback);
                            // editPwdProcess(et, "12345678"); //compare with
                            // the true password "lockPassword"
                            if (!mbMCCMNCReadSIM1) {
                                // user input
                                if (!mbGID1ReadSIM1) {
                                    if (!mbGID2ReadSIM1) {
                                        mGeminiPhone.getIccCardGemini(PhoneConstants.GEMINI_SIM_1).setIccNetworkLockEnabled(3, 2,
                                                etPwd.getText().toString(), etMCCMNC.getText().toString(),
                                                etGID1.getText().toString(), etGID2.getText().toString(), callback);
                                    } else {
                                        mGeminiPhone.getIccCardGemini(PhoneConstants.GEMINI_SIM_1).setIccNetworkLockEnabled(3, 2,
                                                etPwd.getText().toString(), etMCCMNC.getText().toString(),
                                                etGID1.getText().toString(), msSIM1GID2, callback);
                                    }

                                } else {
                                    if (!mbGID2ReadSIM1) {
                                        mGeminiPhone.getIccCardGemini(PhoneConstants.GEMINI_SIM_1).setIccNetworkLockEnabled(3, 2,
                                                etPwd.getText().toString(), etMCCMNC.getText().toString(), msSIM1GID2,
                                                etGID2.getText().toString(), callback);
                                    } else {
                                        mGeminiPhone.getIccCardGemini(PhoneConstants.GEMINI_SIM_1).setIccNetworkLockEnabled(3, 2,
                                                etPwd.getText().toString(), etMCCMNC.getText().toString(), msSIM1GID2,
                                                msSIM1GID2, callback);
                                    }
                                }

                            } else {
                                if (!mbGID1ReadSIM1) {
                                    if (!mbGID2ReadSIM1) {
                                        mGeminiPhone.getIccCardGemini(PhoneConstants.GEMINI_SIM_1).setIccNetworkLockEnabled(3, 2,
                                                etPwd.getText().toString(), msSIM1MCCMNC, etGID1.getText().toString(),
                                                etGID2.getText().toString(), callback);
                                    } else {
                                        mGeminiPhone.getIccCardGemini(PhoneConstants.GEMINI_SIM_1).setIccNetworkLockEnabled(3, 2,
                                                etPwd.getText().toString(), msSIM1MCCMNC, etGID1.getText().toString(),
                                                msSIM1GID2, callback);
                                    }

                                } else {
                                    if (!mbGID2ReadSIM1) {
                                        mGeminiPhone.getIccCardGemini(PhoneConstants.GEMINI_SIM_1).setIccNetworkLockEnabled(3, 2,
                                                etPwd.getText().toString(), msSIM1MCCMNC, msSIM1GID1,
                                                etGID2.getText().toString(), callback);
                                    } else {
                                        mGeminiPhone.getIccCardGemini(PhoneConstants.GEMINI_SIM_1).setIccNetworkLockEnabled(3, 2,
                                                etPwd.getText().toString(), msSIM1MCCMNC, msSIM1GID1, msSIM1GID2, callback);
                                    }
                                }
                            }
                        } else {
                            if (!mbMCCMNCReadSIM2) {
                                // user input
                                if (!mbGID1ReadSIM2) {
                                    if (!mbGID2ReadSIM2) {
                                        mGeminiPhone.getIccCardGemini(PhoneConstants.GEMINI_SIM_2).setIccNetworkLockEnabled(3, 2,
                                                etPwd.getText().toString(), etMCCMNC.getText().toString(),
                                                etGID1.getText().toString(), etGID2.getText().toString(), callback);
                                    } else {
                                        mGeminiPhone.getIccCardGemini(PhoneConstants.GEMINI_SIM_2).setIccNetworkLockEnabled(3, 2,
                                                etPwd.getText().toString(), etMCCMNC.getText().toString(),
                                                etGID1.getText().toString(), msSIM2GID2, callback);
                                    }

                                } else {
                                    if (!mbGID2ReadSIM1) {
                                        mGeminiPhone.getIccCardGemini(PhoneConstants.GEMINI_SIM_2).setIccNetworkLockEnabled(3, 2,
                                                etPwd.getText().toString(), etMCCMNC.getText().toString(), msSIM2GID2,
                                                etGID2.getText().toString(), callback);
                                    } else {
                                        mGeminiPhone.getIccCardGemini(PhoneConstants.GEMINI_SIM_2).setIccNetworkLockEnabled(3, 2,
                                                etPwd.getText().toString(), etMCCMNC.getText().toString(), msSIM2GID2,
                                                msSIM2GID2, callback);
                                    }
                                }

                            } else {
                                if (!mbGID1ReadSIM1) {
                                    if (!mbGID2ReadSIM1) {
                                        mGeminiPhone.getIccCardGemini(PhoneConstants.GEMINI_SIM_2).setIccNetworkLockEnabled(3, 2,
                                                etPwd.getText().toString(), msSIM2MCCMNC, etGID1.getText().toString(),
                                                etGID2.getText().toString(), callback);
                                    } else {
                                        mGeminiPhone.getIccCardGemini(PhoneConstants.GEMINI_SIM_2).setIccNetworkLockEnabled(3, 2,
                                                etPwd.getText().toString(), msSIM2MCCMNC, etGID1.getText().toString(),
                                                msSIM2GID2, callback);
                                    }

                                } else {
                                    if (!mbGID2ReadSIM1) {
                                        mGeminiPhone.getIccCardGemini(PhoneConstants.GEMINI_SIM_2).setIccNetworkLockEnabled(3, 2,
                                                etPwd.getText().toString(), msSIM2MCCMNC, msSIM2GID1,
                                                etGID2.getText().toString(), callback);
                                    } else {
                                        mGeminiPhone.getIccCardGemini(PhoneConstants.GEMINI_SIM_2).setIccNetworkLockEnabled(3, 2,
                                                etPwd.getText().toString(), msSIM2MCCMNC, msSIM2GID1, msSIM1GID2, callback);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        });

        // Press the CANCEL Button

        btnCancel.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View view) {
                // do nothing to quit the edit page
                CPAddLockSetting.this.finish();
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
        
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        // To get the MCC+MNC+GID1 from SIM card
        if (!FeatureOption.MTK_GEMINI_SUPPORT) {
            miSIMState = telephonyManager.getSimState();
            if (TelephonyManager.SIM_STATE_ABSENT == miSIMState) {
                // SIM1 not ready!
            } else {
                msSIMMCCMNC = telephonyManager.getSimOperator();
                if (msSIMMCCMNC == null) {
                    Log.i("SIMMELOCK", "Fail to read SIM MCC+MNC!");
                    // Toast.makeText(this,"Fail to read SIM MCC+MNC",Toast.LENGTH_LONG).show();
                } else {
                    Log.i("SIMMELOCK", "Succeed to read SIM MCC+MNC. MCC+MNC is " + msSIMMCCMNC);
                }
                // To get the GID1 from SIM card //TEMP
                // msSIMGID1 = telephonyManager.getSimOperatorName();
                // if (msSIMGID1 == null)
                // {
                // Log.i("SIMMELOCK","Fail to read SIM GID1!");
                // //Toast.makeText(this,"Fail to read SIM1 GID1",//Toast.LENGTH_LONG).show();
                // }
                // else
                // {
                // Log.i("SIMMELOCK","Succeed to read SIM GID1. GID1 is "+msSIMGID1);
                // }
                // msSIMGID2 = telephonyManager.getSimOperatorName();
                // if (msSIMGID2 == null)
                // {
                // Log.i("SIMMELOCK","Fail to read SIM GID2!");
                // //Toast.makeText(this,"Fail to read SIM1 GID1",//Toast.LENGTH_LONG).show();
                // }
                // else
                // {
                // Log.i("SIMMELOCK","Succeed to read SIM GID2. GID2 is "+msSIMGID2);
                // }
                // To get the GID1 from SIM card
                phone_ori = PhoneFactory.getDefaultPhone();
                IccFileHandler iccFh = ((PhoneProxy) phone_ori).getIccFileHandler();
                iccFh.loadEFTransparent(IccConstants.EF_GID1, mHandler.obtainMessage(EVENT_GET_SIM_GID1));
                iccFh.loadEFTransparent(IccConstants.EF_GID2, mHandler.obtainMessage(EVENT_GET_SIM_GID2));

            }
        } else {
            miSIM1State = telephonyManagerEx.getSimState(0);  /*Gemini API refactor*/
            miSIM2State = telephonyManagerEx.getSimState(1);
            if (TelephonyManager.SIM_STATE_ABSENT == miSIM1State) {
                // SIM1 not ready!
                // Toast.makeText(this,"Add CP lock fail : SIM1 not ready!",Toast.LENGTH_LONG).show();
                Log.i("SIMMELOCK", "Add CP lock fail : SIM1 not ready!");
            }
            if (TelephonyManager.SIM_STATE_ABSENT == miSIM2State) {
                // SIM2 not ready!
                // Toast.makeText(this,"Add CP lock fail : SIM2 not ready!",Toast.LENGTH_LONG).show();
                Log.i("SIMMELOCK", "Add CP lock fail : SIM2 not ready!");
            }

            msSIM1MCCMNC = telephonyManagerEx.getSimOperator(0);
            if (msSIM1MCCMNC == null) {
                Log.i("SIMMELOCK", "Fail to read SIM1 MCC+MNC!");
                // Toast.makeText(this,"Fail to read SIM1 MCC+MNC",Toast.LENGTH_LONG).show();
            } else {
                Log.i("SIMMELOCK", "[Gemini]Succeed to read SIM1 MCC+MNC. MCC+MNC is " + msSIM1MCCMNC);
            }
            msSIM2MCCMNC = telephonyManagerEx.getSimOperator(1);
            if (msSIM2MCCMNC == null) {
                Log.i("SIMMELOCK", "Fail to read SIM2 MCC+MNC!");
                // Toast.makeText(this,"Fail to read SIM2 MCC+MNC",Toast.LENGTH_LONG).show();
            } else {
                Log.i("SIMMELOCK", "[Gemini]Succeed to read SIM2 MCC+MNC. MCC+MNC is " + msSIM2MCCMNC);
            }

            // To get the GID1 from SIM card //TEMP
            // msSIM1GID1 = telephonyManager.getSimOperatorNameGemini(0);
            // if (msSIM1GID1 == null)
            // {
            // Log.i("SIMMELOCK","Fail to read SIM1 GID1!");
            // //Toast.makeText(this,"Fail to read SIM1 GID1",Toast.LENGTH_LONG).show();
            // }
            // else
            // {
            // Log.i("SIMMELOCK","[Gemini]Succeed to read SIM1 SIM1GID1. SIM1GID1 is "+msSIM1GID1);
            // }
            // msSIM2GID1 = telephonyManager.getSimOperatorNameGemini(1);
            // if (msSIM2GID1 == null)
            // {
            // Log.i("SIMMELOCK","Fail to read SIM2 GID1!");
            // //Toast.makeText(this,"Fail to read SIM2 GID1",Toast.LENGTH_LONG).show();
            // }
            // else
            // {
            // Log.i("SIMMELOCK","[Gemini]Succeed to read SIM2 SIM2GID1. SIM2GID1 is "+msSIM2GID1);
            // }
            //            
            // //To get the GID2 from SIM card //TEMP
            // msSIM1GID2 = telephonyManager.getSimOperatorNameGemini(0);
            // if (msSIM1GID2 == null)
            // {
            // Log.i("SIMMELOCK","Fail to read SIM1 GID2!");
            // //Toast.makeText(this,"Fail to read SIM1 GID2",Toast.LENGTH_LONG).show();
            // }
            // else
            // {
            // Log.i("SIMMELOCK","[Gemini]Succeed to read SIM1 SIM1GID2. SIM1GID2 is "+msSIM1GID2);
            // }
            // msSIM2GID2 = telephonyManager.getSimOperatorNameGemini(1);
            // if (msSIM2GID2 == null)
            // {
            // Log.i("SIMMELOCK","Fail to read SIM2 GID2!");
            // //Toast.makeText(this,"Fail to read SIM2 GID2",Toast.LENGTH_LONG).show();
            // }
            // else
            // {
            // Log.i("SIMMELOCK","[Gemini]Succeed to read SIM2 SIM2GID2. SIM2GID2 is "+msSIM2GID2);
            // }
            intSIMNumber = bundle.getInt("SIMNo");
            GeminiPhone mGeminiPhone = (GeminiPhone) PhoneFactory.getDefaultPhone();
            if (intSIMNumber == 0) {
                IccFileHandler iccFh = mGeminiPhone.getIccFileHandlerGemini(PhoneConstants.GEMINI_SIM_1);
                iccFh.loadEFTransparent(IccConstants.EF_GID1, mHandler.obtainMessage(EVENT_GET_SIM1_GID1));
                iccFh.loadEFTransparent(IccConstants.EF_GID2, mHandler.obtainMessage(EVENT_GET_SIM1_GID2));
            } else {
                IccFileHandler iccFh = mGeminiPhone.getIccFileHandlerGemini(PhoneConstants.GEMINI_SIM_2);
                iccFh.loadEFTransparent(IccConstants.EF_GID1, mHandler.obtainMessage(EVENT_GET_SIM2_GID1));
                iccFh.loadEFTransparent(IccConstants.EF_GID2, mHandler.obtainMessage(EVENT_GET_SIM2_GID2));
            }

            // To get the GID2 from SIM card //TEMP
            msSIM1GID2 = telephonyManager.getSimOperatorNameGemini(0);
            if (msSIM1GID2 == null) {
                Log.i("SIMMELOCK", "Fail to read SIM1 GID2!");
                // Toast.makeText(this,"Fail to read SIM1 GID2",Toast.LENGTH_LONG).show();
            } else {
                Log.i("SIMMELOCK", "[Gemini]Succeed to read SIM1 SIM1GID2. SIM1GID2 is " + msSIM1GID2);
            }
            msSIM2GID2 = telephonyManager.getSimOperatorNameGemini(1);
            if (msSIM2GID2 == null) {
                Log.i("SIMMELOCK", "Fail to read SIM2 GID2!");
                // Toast.makeText(this,"Fail to read SIM2 GID2",Toast.LENGTH_LONG).show();
            } else {
                Log.i("SIMMELOCK", "[Gemini]Succeed to read SIM2 SIM2GID2. SIM2GID2 is " + msSIM2GID2);
            }
        }
    }

    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        switch (id) {
        case DIALOG_ADDLOCKFAIL: // password is wrong
            builder.setCancelable(false).setTitle(R.string.strAttention).setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage(R.string.strAddLockFail).setOnKeyListener(this).setPositiveButton(R.string.strYes,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    etGID1.setText("");
                                    etGID2.setText("");
                                    etMCCMNC.setText("");
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
                                    // etMCCMNC.setText("");
                                    etPwd.setText("");
                                    etPwdConfirm.setText("");
                                    dialog.cancel();
                                    clickFlag = false;
                                }
                            }).show();
            break;
        case DIALOG_MCCMNCLENGTHINCORRECT:// Length is incorrect
            builder.setCancelable(false).setTitle(R.string.strAttention).setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage(R.string.strMCCMNCLengthIncorrect).setOnKeyListener(this).setPositiveButton(R.string.strYes,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    etMCCMNC.setText("");
                                    // etPwd.setText("");
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
                                    dialog.cancel();
                                    clickFlag = false;
                                    CPAddLockSetting.this.finish();
                                }
                            }).show();

            break;
        case DIALOG_GID1WRONG:// Length is incorrect
            builder.setCancelable(false).setTitle(R.string.strAttention).setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage(R.string.strGID1WRONG).setOnKeyListener(this).setPositiveButton(R.string.strYes,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    etGID1.setText("");
                                    dialog.cancel();
                                    clickFlag = false;
                                }
                            }).show();

            break;
        case DIALOG_GID2WRONG:// Length is incorrect
            builder.setCancelable(false).setTitle(R.string.strAttention).setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage(R.string.strGID2WRONG).setOnKeyListener(this).setPositiveButton(R.string.strYes,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    etGID2.setText("");
                                    dialog.cancel();
                                    clickFlag = false;
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
        @Override
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
            case EVENT_GET_SIM_GID1:
                if (ar.exception != null) {
                    Log.i("SIMME Lock", "fail to get SIM GID1");//
                    intSIMGID1 = 255;// ///////////////////////////////
                } else {
                    byte[] data = (byte[]) (ar.result);
                    String msHexSIMGID1 = IccUtils.bytesToHexString(data);
                    if ((data[0] & 0xff) == 0xff) {
                        Log.i("SIMME Lock", "SIM GID1 not initialized");//
                        intSIMGID1 = 255;// ///////////////////////////////
                    } else {
                        // intSIMGID1 = Integer.parseInt(msHexSIMGID1);
                        intSIMGID1 = 1;// ///////////////////////////////
                        msSIMGID1 = String.valueOf(Integer.parseInt(IccUtils.bytesToHexString(data).substring(0, 2)));
                    }
                }
                break;
            case EVENT_GET_SIM1_GID1:
                if (ar.exception != null) {
                    Log.i("SIMME Lock", "fail to get SIM1 GID1");//
                    intSIMGID1 = 255;// ///////////////////////////////
                } else {
                    Log.i("SIMME Lock", "succeed to get SIM1 GID1");
                    byte[] data = (byte[]) (ar.result);
                    Log.i("SIMME Lock", "SIM1 GID1 :" + data);
                    String msHexSIM1GID1 = IccUtils.bytesToHexString(data);
                    if ((data[0] & 0xff) == 0xff) {
                        Log.i("SIMME Lock", "SIM1 GID1 not initialized");//
                        intSIMGID1 = 255;// ///////////////////////////////
                    } else {
                        // intSIM1GID1 = Integer.parseInt(msHexSIM1GID1);
                        intSIMGID1 = 1;// ///////////////////////////////
                        if (msHexSIM1GID1.length() >= 2) {
                            msSIM1GID1 = String.valueOf(Integer.parseInt(IccUtils.bytesToHexString(data).substring(0, 2)));
                        } else {
                            msSIM1GID1 = String.valueOf(Integer.parseInt(IccUtils.bytesToHexString(data)));
                        }
                        Log.i("SIMME Lock", "Normal SIM1 GID1 :" + msSIM1GID1);
                    }

                }
                break;
            case EVENT_GET_SIM2_GID1:
                if (ar.exception != null) {
                    Log.i("SIMME Lock", "fail to get SIM2 GID1");//
                    intSIMGID1 = 255;// ///////////////////////////////

                } else {
                    byte[] data = (byte[]) (ar.result);
                    String msHexSIM2GID1 = IccUtils.bytesToHexString(data);
                    if ((data[0] & 0xff) == 0xff) {
                        Log.i("SIMME Lock", "SIM2 GID1 not initialized");//
                        intSIMGID1 = 255;// ///////////////////////////////
                    } else {
                        // intSIM2GID1 = Integer.parseInt(msHexSIM2GID1);
                        intSIMGID1 = 1;// ///////////////////////////////
                        if (msHexSIM2GID1.length() >= 2) {
                            msSIM2GID1 = String.valueOf(Integer.parseInt(IccUtils.bytesToHexString(data).substring(0, 2)));
                        } else {
                            msSIM2GID1 = String.valueOf(Integer.parseInt(IccUtils.bytesToHexString(data)));
                            Log.i("SIMME Lock", "Normal SIM2 GID1 :" + msSIM2GID1);
                        }
                    }
                }
                break;

            case EVENT_GET_SIM_GID2:
                if (ar.exception != null) {
                    Log.i("SIMME Lock", "fail to get SIM GID2");//
                    intSIMGID2 = 255;// ///////////////////////////////
                } else {
                    byte[] data = (byte[]) (ar.result);
                    String msHexSIMGID2 = IccUtils.bytesToHexString(data);
                    if ((data[0] & 0xff) == 0xff) {
                        Log.i("SIMME Lock", "SIM GID2 not initialized");//
                        intSIMGID2 = 255;// ///////////////////////////////
                    } else {
                        // intSIMGID1 = Integer.parseInt(msHexSIMGID1);
                        intSIMGID2 = 1;// ///////////////////////////////
                        msSIMGID2 = String.valueOf(Integer.parseInt(IccUtils.bytesToHexString(data).substring(0, 2)));
                    }
                }
                break;
            case EVENT_GET_SIM1_GID2:
                if (ar.exception != null) {
                    Log.i("SIMME Lock", "fail to get SIM1 GID2");//
                    intSIMGID2 = 255;// ///////////////////////////////
                } else {
                    Log.i("SIMME Lock", "succeed to get SIM1 GID2");
                    byte[] data = (byte[]) (ar.result);
                    Log.i("SIMME Lock", "SIM1 GID2 :" + data);
                    String msHexSIM1GID2 = IccUtils.bytesToHexString(data);
                    if ((data[0] & 0xff) == 0xff) {
                        Log.i("SIMME Lock", "SIM1 GID2 not initialized");//
                        intSIMGID2 = 255;// ///////////////////////////////
                    } else {
                        // intSIM1GID1 = Integer.parseInt(msHexSIM1GID1);
                        intSIMGID2 = 1;// ///////////////////////////////
                        if (msHexSIM1GID2.length() >= 2) {
                            msSIM1GID2 = String.valueOf(Integer.parseInt(IccUtils.bytesToHexString(data).substring(0, 2)));
                        } else {
                            msSIM1GID2 = String.valueOf(Integer.parseInt(IccUtils.bytesToHexString(data)));
                        }
                        Log.i("SIMME Lock", "Normal SIM1 GID2 :" + msSIM1GID2);
                    }
                }
                break;
            case EVENT_GET_SIM2_GID2:
                if (ar.exception != null) {
                    Log.i("SIMME Lock", "fail to get SIM2 GID2");//
                    intSIMGID2 = 255;// ///////////////////////////////
                } else {
                    byte[] data = (byte[]) (ar.result);
                    String msHexSIM2GID2 = IccUtils.bytesToHexString(data);
                    if ((data[0] & 0xff) == 0xff) {
                        Log.i("SIMME Lock", "SIM2 GID2 not initialized");//
                        intSIMGID2 = 255;// ///////////////////////////////
                    } else {
                        // intSIM2GID1 = Integer.parseInt(msHexSIM2GID1);
                        intSIMGID2 = 1;// ///////////////////////////////
                        if (msHexSIM2GID2.length() >= 2) {
                            msSIM2GID2 = String.valueOf(Integer.parseInt(IccUtils.bytesToHexString(data).substring(0, 2)));
                        } else {
                            msSIM2GID2 = String.valueOf(Integer.parseInt(IccUtils.bytesToHexString(data)));
                            Log.i("SIMME Lock", "Normal SIM2 GID2 :" + msSIM2GID2);
                        }
                    }
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
    EditText etMCCMNC = null;
    EditText etGID1 = null;
    EditText etGID2 = null;
    EditText etPwd = null;
    EditText etPwdConfirm = null;
    private String lockName = null;
    private int lockCategory = -1;
    int miSIM1State = 0;// the current SIM1 card state
    int miSIM2State = 0;// the current SIM2 card state
    int miSIMState = 0;
    Spinner s1;
    Spinner s2;
    Spinner s3;
    String msSIM1MCCMNC = null;
    String msSIM2MCCMNC = null;
    String msSIMMCCMNC = null;
    String msSIM1GID1 = null;
    String msSIM2GID1 = null;
    String msSIMGID1 = null;
    String msSIMGID2 = null;
    String msSIM1GID2 = null;
    String msSIM2GID2 = null;
    boolean mbMCCMNCReadSIM1 = false;// whether to read MCC+MNC from SIM1
    // card,default to set false
    boolean mbMCCMNCReadSIM2 = false;// whether to read MCC+MNC from SIM2
    // card,default to set false
    boolean mbMCCMNCReadSIM = false;
    boolean mbGID1ReadSIM = false;
    boolean mbGID1ReadSIM1 = false;// whether to read GID1 from SIM1
    // card,default to set false
    boolean mbGID1ReadSIM2 = false;// whether to read GID1 from SIM2
    // card,default to set false
    boolean mbGID2ReadSIM = false;
    boolean mbGID2ReadSIM1 = false;// whether to read GID2 from SIM1
    // card,default to set false
    boolean mbGID2ReadSIM2 = false;// whether to read GID2 from SIM2
    // card,default to set false
    boolean mbMCCMNCCorrect = false;
    boolean mbGID1Correct = false;
    boolean mbGID2Correct = false;

    int intSIMNumber = 0;
    Bundle bundle = null;
    protected PhoneBase phone;
    protected Phone phone_ori;
    int intSIMGID1 = 0;
    int intSIMGID2 = 0;
    private boolean clickFlag = false;

    final int DIALOG_MCCMNCLENGTHINCORRECT = 1;
    final int DIALOG_ADDLOCKFAIL = 2;
    final int DIALOG_ADDLOCKSUCCEED = 3;
    final int DIALOG_PASSWORDLENGTHINCORRECT = 4;
    final int DIALOG_PASSWORDWRONG = 5;
    final int DIALOG_GID1WRONG = 6;
    final int DIALOG_GID2WRONG = 7;
    private static final int ADDLOCK_ICC_SML_COMPLETE = 120;
    private static final int EVENT_GET_SIM_GID1 = 36;
    private static final int EVENT_GET_SIM1_GID1 = 38;
    private static final int EVENT_GET_SIM2_GID1 = 40;
    private static final int EVENT_GET_SIM_GID2 = 37;
    private static final int EVENT_GET_SIM1_GID2 = 39;
    private static final int EVENT_GET_SIM2_GID2 = 41;
}
