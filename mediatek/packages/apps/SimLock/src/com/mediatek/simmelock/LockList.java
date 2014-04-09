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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneBase;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.gemini.GeminiPhone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.IccCard;
import com.mediatek.common.featureoption.FeatureOption;

public class LockList extends PreferenceActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // setContentView(R.layout.main);
        this.addPreferencesFromResource(R.xml.locklist);
        // get a gemini phone instance

        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            GeminiPhone geminiPhone = (GeminiPhone) PhoneFactory.getDefaultPhone();
            // get the sim card states
            isSim1Insert = geminiPhone.isSimInsert(PhoneConstants.GEMINI_SIM_1);
            isSim2Insert = geminiPhone.isSimInsert(PhoneConstants.GEMINI_SIM_2);
        } else {
            isSimInsert = TelephonyManager.getDefault().hasIccCard();
        }
        // boolean isRadio1On = geminiPhone.isRadioOnGemini(PhoneConstants.GEMINI_SIM_1);
        // boolean isRadio2On = geminiPhone.isRadioOnGemini(PhoneConstants.GEMINI_SIM_2);

        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        registerReceiver(mReceiver, intentFilter);

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            GeminiPhone geminiPhone = (GeminiPhone) PhoneFactory.getDefaultPhone();
            int simId = getIntent().getIntExtra("Setting SIM Number", -1);
            Log.i("SIMMELOCK", "GeminiPhone SimId: " + simId);
            if (simId == 0) {
                boolean isRadio1On = geminiPhone.isRadioOnGemini(PhoneConstants.GEMINI_SIM_1);
                Log.i("SIMMELOCK", "(isSim1Insert == " + isSim1Insert + "isRadio1On == " + isRadio1On);
                if ((!isSim1Insert) || (!isRadio1On)) {
                    getPreferenceScreen().setEnabled(false);
                    return;
                } else {
                    getPreferenceScreen().setEnabled(true);
                }
            } else if (simId == 1) {
                boolean isRadio2On = geminiPhone.isRadioOnGemini(PhoneConstants.GEMINI_SIM_2);
                Log.i("SIMMELOCK", "(isSim2Insert == " + isSim2Insert + "isRadio2On == " + isRadio2On);
                if ((!isSim2Insert) || (!isRadio2On)) {
                    getPreferenceScreen().setEnabled(false);
                    return;
                } else {
                    getPreferenceScreen().setEnabled(true);
                }
            } else {
                getPreferenceScreen().setEnabled(false);
                return;
            }
        } else {
            final ITelephony iTel = ITelephony.Stub.asInterface(ServiceManager.getService(Context.TELEPHONY_SERVICE));
            if (iTel == null) {
                Log.e("SIMMELOCK", "clocwork worked...");
                // not return and let exception happened.
            }
            try {
                if (!isSimInsert || !iTel.isRadioOn()) {
                    Log.w("SIMMELOCK", "(isSimInsert == " + isSimInsert);
                    getPreferenceScreen().setEnabled(false);
                    return;
                } else {
                    getPreferenceScreen().setEnabled(true);
                }
            } catch (RemoteException ex) {
                ex.printStackTrace();
            }
        }

        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);

        if (!FeatureOption.MTK_GEMINI_SUPPORT) {
            // if it single SIM, just show a lock list
            Message callback = Message.obtain(mHandler, QUERY_ICC_SML_COMPLETE);
            Message callback2 = Message.obtain(mHandler, QUERY_ICC_SML_COMPLETE);
            Message callback3 = Message.obtain(mHandler, QUERY_ICC_SML_COMPLETE);
            Message callback4 = Message.obtain(mHandler, QUERY_ICC_SML_COMPLETE);
            Message callback5 = Message.obtain(mHandler, QUERY_ICC_SML_COMPLETE);
            Phone phone = PhoneFactory.getDefaultPhone();
            if (phone != null) {
                IccCard iccCard = phone.getIccCard();
                if (iccCard != null) {
                    iccCard.QueryIccNetworkLock(0, 4, null, null, null, null, callback);
                    iccCard.QueryIccNetworkLock(1, 4, null, null, null, null, callback2);
                    iccCard.QueryIccNetworkLock(2, 4, null, null, null, null, callback3);
                    iccCard.QueryIccNetworkLock(3, 4, null, null, null, null, callback4);
                    iccCard.QueryIccNetworkLock(4, 4, null, null, null, null, callback5);
                }
            }
        } else {
            // Gemini SIM project
            // Toast.makeText(LockList.this,"Gemini Card version!",Toast.LENGTH_LONG).show();
            Log.i("SIMMELOCK", "Gemini Card version!");
            GeminiPhone mGeminiPhone = (GeminiPhone) PhoneFactory.getDefaultPhone();
            // find which SIM card is be setting now
            Bundle bundleReceive = this.getIntent().getExtras();
            if (bundleReceive == null) {
                Log.e("X", "clocwork worked...");
                // not return and let exception happened.
            }
            intSIMNumber = bundleReceive.getInt("Setting SIM Number");
            if (mGeminiPhone != null) {
                if (0 == intSIMNumber) {
                    // choose the Tab1:SIM1 lock setting
                    // //whether some lock category is disabled?
                    Message callback = Message.obtain(mHandler, QUERY_ICC_SML_COMPLETE);
                    Message callback2 = Message.obtain(mHandler, QUERY_ICC_SML_COMPLETE);
                    Message callback3 = Message.obtain(mHandler, QUERY_ICC_SML_COMPLETE);
                    Message callback4 = Message.obtain(mHandler, QUERY_ICC_SML_COMPLETE);
                    Message callback5 = Message.obtain(mHandler, QUERY_ICC_SML_COMPLETE);
                    IccCard iccCard = mGeminiPhone.getIccCardGemini(PhoneConstants.GEMINI_SIM_1);
                    if (iccCard != null) {
                        iccCard.QueryIccNetworkLock(0, 4, null, null, null, null, callback);
                        iccCard.QueryIccNetworkLock(1, 4, null, null, null, null, callback2);
                        iccCard.QueryIccNetworkLock(2, 4, null, null, null, null, callback3);
                        iccCard.QueryIccNetworkLock(3, 4, null, null, null, null, callback4);
                        iccCard.QueryIccNetworkLock(4, 4, null, null, null, null, callback5);
                    }
                } else if (1 == intSIMNumber) {
                    // whether some lock category is disabled?
                    Message callback = Message.obtain(mHandler, QUERY_ICC_SML_COMPLETE);
                    Message callback2 = Message.obtain(mHandler, QUERY_ICC_SML_COMPLETE);
                    Message callback3 = Message.obtain(mHandler, QUERY_ICC_SML_COMPLETE);
                    Message callback4 = Message.obtain(mHandler, QUERY_ICC_SML_COMPLETE);
                    Message callback5 = Message.obtain(mHandler, QUERY_ICC_SML_COMPLETE);
                    IccCard iccCard = mGeminiPhone.getIccCardGemini(PhoneConstants.GEMINI_SIM_2);
                    if (iccCard != null) {
                        iccCard.QueryIccNetworkLock(0, 4, null, null, null, null, callback);
                        iccCard.QueryIccNetworkLock(1, 4, null, null, null, null, callback2);
                        iccCard.QueryIccNetworkLock(2, 4, null, null, null, null, callback3);
                        iccCard.QueryIccNetworkLock(3, 4, null, null, null, null, callback4);
                        iccCard.QueryIccNetworkLock(4, 4, null, null, null, null, callback5);
                    }
                }
            }
        }
    }

    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferencescreen, Preference preference) {
        if (this.getPreferenceScreen().findPreference("nplock") == preference) {
            // Toast.makeText(this, "nplock", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(LockList.this, ActionList.class);
            Bundle bundle = new Bundle();
            bundle.putInt(LOCKCATEGORY, NPLOCKTYPE);
            if (!FeatureOption.MTK_GEMINI_SUPPORT) {
                // if it single SIM, no need to pass the card number
            } else {
                // Gemini SIM: put the SIM number to the bundle
                bundle.putInt("SIMNo", intSIMNumber);
            }
            intent.putExtras(bundle);
            startActivity(intent);

            return true;
        } else if (this.getPreferenceScreen().findPreference("nsplock") == preference) {
            // Toast.makeText(this, "nsplock", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(LockList.this, ActionList.class);
            Bundle bundle = new Bundle();
            bundle.putInt(LOCKCATEGORY, NSPLOCKTYPE);
            if (!FeatureOption.MTK_GEMINI_SUPPORT) {
                // if it single SIM, no need to pass the card number
            } else {
                // Gemini SIM: put the SIM number to the bundle
                bundle.putInt("SIMNo", intSIMNumber);
            }
            intent.putExtras(bundle);
            startActivity(intent);

            return true;
        } else if (this.getPreferenceScreen().findPreference("splock") == preference) {
            // Toast.makeText(this, "splock", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(LockList.this, ActionList.class);

            Bundle bundle = new Bundle();
            bundle.putInt(LOCKCATEGORY, SPLOCKTYPE);
            if (!FeatureOption.MTK_GEMINI_SUPPORT) {
                // if it single SIM, no need to pass the card number
            } else {
                // Gemini SIM: put the SIM number to the bundle
                bundle.putInt("SIMNo", intSIMNumber);
            }
            intent.putExtras(bundle);
            startActivity(intent);

            return true;
        } else if (this.getPreferenceScreen().findPreference("cplock") == preference) {
            // Toast.makeText(this, "cplock", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(LockList.this, ActionList.class);

            Bundle bundle = new Bundle();
            bundle.putInt(LOCKCATEGORY, CPLOCKTYPE);
            if (!FeatureOption.MTK_GEMINI_SUPPORT) {
                // if it single SIM, no need to pass the card number
            } else {
                // Gemini SIM: put the SIM number to the bundle
                bundle.putInt("SIMNo", intSIMNumber);
            }
            intent.putExtras(bundle);
            startActivity(intent);

            return true;
        } else if (this.getPreferenceScreen().findPreference("simplock") == preference) {
            // Toast.makeText(this, "simplock", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(LockList.this, ActionList.class);

            Bundle bundle = new Bundle();
            bundle.putInt(LOCKCATEGORY, SIMPLOCKTYPE);
            if (!FeatureOption.MTK_GEMINI_SUPPORT) {
                // if it single SIM, no need to pass the card number
            } else {
                // Gemini SIM: put the SIM number to the bundle
                bundle.putInt("SIMNo", intSIMNumber);
            }
            intent.putExtras(bundle);
            startActivity(intent);

            return true;
        }
        return false;
    }

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            AsyncResult ar = (AsyncResult) msg.obj;
            switch (msg.what) {
            case QUERY_ICC_SML_COMPLETE:
                Log.i("SIMMELOCK", "QUERY_ICC_SML_COMPLETE");
                AsyncResult ar1 = (AsyncResult) msg.obj;
                int[] LockState = (int[]) ar1.result;

                // Toast.makeText(LockList.this,"LockState: " +
                // String.valueOf(LockState[1]),Toast.LENGTH_LONG).show();
                if (LockState == null) {
                    ((PreferenceActivity) (LockList.this)).getPreferenceScreen().setEnabled(false);
                } else if (LockState[2] == 0) {
                    ((PreferenceActivity) (LockList.this)).getPreferenceScreen().getPreference(0).setEnabled(false);
                    ((PreferenceActivity) (LockList.this)).getPreferenceScreen().getPreference(1).setEnabled(false);
                    ((PreferenceActivity) (LockList.this)).getPreferenceScreen().getPreference(2).setEnabled(false);
                    ((PreferenceActivity) (LockList.this)).getPreferenceScreen().getPreference(3).setEnabled(false);
                    ((PreferenceActivity) (LockList.this)).getPreferenceScreen().getPreference(4).setEnabled(false);
                } else if (LockState[1] == 4) {
                    // be disabled!
                    ((PreferenceActivity) (LockList.this)).getPreferenceScreen().getPreference(LockState[0]).setEnabled(
                            false);
                }
                break;
            default:
                break;
            }
        }
    };

    /******************************************/
    /*** values list ***/
    /******************************************/
    public static final String LOCKCATEGORY = "LockCategory";
    private static final int NPLOCKTYPE = 0;
    private static final int NSPLOCKTYPE = 1;
    private static final int SPLOCKTYPE = 2;
    private static final int CPLOCKTYPE = 3;
    private static final int SIMPLOCKTYPE = 4;
    boolean[] boolLockStatus = { false, false, false, false, false };
    int intSIMNumber = 0;
    int miSIM1State = 0;// the current SIM1 card state
    int miSIM2State = 0;// the current SIM2 card state
    int miSIMState = 0;// the current SIM card state
    // Preference pref;
    boolean isSim1Insert = false;
    boolean isSim2Insert = false;
    boolean isSimInsert = false;

    private static final int QUERY_ICC_SML_COMPLETE = 120;
    protected PhoneBase phone;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
                finish();
            }
        }
    };

}
