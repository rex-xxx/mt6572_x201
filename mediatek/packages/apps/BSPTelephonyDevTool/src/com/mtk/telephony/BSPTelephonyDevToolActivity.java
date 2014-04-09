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

package com.mtk.telephony;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.RemoteException;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import android.provider.Telephony.SIMInfo;
import com.android.internal.telephony.ITelephony;
import android.os.SystemProperties;
import android.os.ServiceManager;
import com.mediatek.common.featureoption.FeatureOption;

public class BSPTelephonyDevToolActivity extends Activity {
    private static final String TAG = "BSP_Telephony_Dev";

    private Button mDefaultSim1;
    private Button mDefaultSim2;
    private TextView mOperatorSim1;
    private TextView mOperatorSim2;
    private Button mStatusbarNotification;
    private Button mDefaultDataSim1;
    private Button mDefaultDataSim2;
    private Button mDefaultDataOff;

    private TextView mNetworkTypeSim1;
    private TextView mNetworkTypeSim2;
    private ProgressBar mSignalSim1;
    private ProgressBar mSignalSim2;

    private TextView mDataConnectionTypeSim1;
    private TextView mDataConnectionTypeSim2;
    private TextView mDataActivitySim1;
    private TextView mDataActivitySim2;

    private Button m3GSim1;
    private Button m3GSim2;
    private Button m3GSimOff;

    private TelephonyManager mTelephonyManager;
    private ITelephony mTelephony;
    private Switch3GHandler mSwitch3GHandler;

    private int mDefaultSimId;
    private boolean mIsStatusBarNotificationEnabled;
    private int mDefaultDataSimId = -1;

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_DATA_DEFAULT_SIM_CHANGED)) {
                long simInfoId = intent.getLongExtra(PhoneConstants.MULTI_SIM_ID_KEY, Settings.System.DEFAULT_SIM_NOT_SET);
                mDefaultDataSimId = SIMInfo.getSlotById(BSPTelephonyDevToolActivity.this, simInfoId);
                Log.i(TAG, "Receive ACTION_DATA_DEFAULT_SIM_CHANGED, data sim: " + mDefaultDataSimId);
                updateUI();
            }
        }
    };

    private OnClickListener mStatusbarNotificationOnClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            mIsStatusBarNotificationEnabled = !mIsStatusBarNotificationEnabled;
            updateUI();
            if (mIsStatusBarNotificationEnabled)
                startService(new Intent(BSPTelephonyDevToolActivity.this, BSPTelephonyDevToolService.class));
            else
                stopService(new Intent(BSPTelephonyDevToolActivity.this, BSPTelephonyDevToolService.class));
        }
    };

    private OnClickListener mDefaultSimOnClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_default_sim_sim1:
                    mDefaultSimId = PhoneConstants.GEMINI_SIM_1;
                    break;
                case R.id.btn_default_sim_sim2:
                    mDefaultSimId = PhoneConstants.GEMINI_SIM_2;
                    break;
            }

            try {
                mTelephony.setDefaultPhone(mDefaultSimId);
                SystemProperties.set(PhoneConstants.GEMINI_DEFAULT_SIM_PROP, String.valueOf(mDefaultSimId));
            } catch(RemoteException e) {
                e.printStackTrace();
            }

            updateUI();
            rebootAlert();
        }
    };

    private OnClickListener mDefaultDataSimOnClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(Intent.ACTION_DATA_DEFAULT_SIM_CHANGED);
            switch (v.getId()) {
                case R.id.btn_default_data_sim1:
                    mDefaultDataSimId = PhoneConstants.GEMINI_SIM_1;
                    break;
                case R.id.btn_default_data_sim2:
                    mDefaultDataSimId = PhoneConstants.GEMINI_SIM_2;
                    break;
                case R.id.btn_default_data_off:
                    mDefaultDataSimId = -1;
            }

            SIMInfo simInfo = SIMInfo.getSIMInfoBySlot(BSPTelephonyDevToolActivity.this, mDefaultDataSimId);
            if (simInfo != null) {
                intent.putExtra(PhoneConstants.MULTI_SIM_ID_KEY,
                        SIMInfo.getSIMInfoBySlot(BSPTelephonyDevToolActivity.this, mDefaultDataSimId).mSimId);
            }
            sendBroadcast(intent);
            updateUI();
        }
    };

    private OnClickListener m3GSimOnClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_3g_sim1:
                    mSwitch3GHandler.set3GCapabilitySIM(PhoneConstants.GEMINI_SIM_1);
                    break;
                case R.id.btn_3g_sim2:
                    mSwitch3GHandler.set3GCapabilitySIM(PhoneConstants.GEMINI_SIM_2);
                    break;
                case R.id.btn_3g_off:
                    mSwitch3GHandler.set3GCapabilitySIM(-1);
            }

            updateUI();
        }
    };

    private PhoneStateListener mPhone1StateListener = new PhoneStateListener() {
        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            int gsmSignalStrength = signalStrength.getGsmSignalStrength();
            Log.i(TAG, "SIM1 signal strength: " + gsmSignalStrength);
            updateSignalStrengthUI(mSignalSim1, gsmSignalStrength);
        }

        @Override
        public void onServiceStateChanged(ServiceState serviceState) {
            Log.i(TAG, "SIM1 service state changed: " + serviceState);
            if (serviceState.getState() == ServiceState.STATE_IN_SERVICE) {
                mOperatorSim1.setText(serviceState.getOperatorAlphaShort());
                mNetworkTypeSim1.setText(Utility.getNetworkTypeString(mTelephonyManager.getNetworkTypeGemini(PhoneConstants.GEMINI_SIM_1)));
            } else {
                mOperatorSim1.setText(Utility.getServiceStateString(serviceState.getState()));
                mNetworkTypeSim1.setText("");
            }
        }

        @Override
        public void onDataConnectionStateChanged(int state, int networkType) {
            Log.i(TAG, "SIM1 data state: " + state + ", " + networkType);
            if (state > 0) {
                if (state == TelephonyManager.DATA_CONNECTED)
                    mDataConnectionTypeSim1.setText(Utility.getNetworkTypeString(networkType));
                else
                    mDataConnectionTypeSim1.setText(Utility.getDataStateString(state));
            } else {
                mDataConnectionTypeSim1.setText("");
            }

            int peerSimState = mTelephonyManager.getDataStateGemini(PhoneConstants.GEMINI_SIM_2);
            if (peerSimState > 0) {
                if (peerSimState == TelephonyManager.DATA_CONNECTED) {
                    mDataConnectionTypeSim2.setText(Utility.getNetworkTypeString(mTelephonyManager.getNetworkTypeGemini(PhoneConstants.GEMINI_SIM_2)));
                } else {
                    mDataConnectionTypeSim2.setText(Utility.getDataStateString(peerSimState));
                }
            } else {
                mDataConnectionTypeSim2.setText("");
            }
        }

        @Override
        public void onDataActivity(int direction) {
            mDataActivitySim1.setText(Utility.getDataDirectionString(direction));
        }
    };

    private PhoneStateListener mPhone2StateListener = new PhoneStateListener() {
        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            int gsmSignalStrength = signalStrength.getGsmSignalStrength();
            Log.i(TAG, "SIM2 signal strength: " + gsmSignalStrength);
            updateSignalStrengthUI(mSignalSim2, gsmSignalStrength);
        }

        @Override
        public void onServiceStateChanged(ServiceState serviceState) {
            Log.i(TAG, "SIM2 service state changed: " + serviceState);
            if (serviceState.getState() == ServiceState.STATE_IN_SERVICE) {
                mOperatorSim2.setText(serviceState.getOperatorAlphaShort());
                mNetworkTypeSim2.setText(Utility.getNetworkTypeString(mTelephonyManager.getNetworkTypeGemini(PhoneConstants.GEMINI_SIM_2)));
            } else {
                mOperatorSim2.setText(Utility.getServiceStateString(serviceState.getState()));
                mNetworkTypeSim2.setText("");
            }
        }

        @Override
        public void onDataConnectionStateChanged(int state, int networkType) {
            Log.i(TAG, "SIM2 data state: " + state + ", " + networkType);
            if (state > 0) {
                if (state == TelephonyManager.DATA_CONNECTED)
                    mDataConnectionTypeSim2.setText(Utility.getNetworkTypeString(networkType));
                else
                    mDataConnectionTypeSim2.setText(Utility.getDataStateString(state));
            } else {
                mDataConnectionTypeSim2.setText("");
            }

            int peerSimState = mTelephonyManager.getDataStateGemini(PhoneConstants.GEMINI_SIM_1);
            if (peerSimState > 0) {
                if (peerSimState == TelephonyManager.DATA_CONNECTED) {
                    mDataConnectionTypeSim1.setText(Utility.getNetworkTypeString(mTelephonyManager.getNetworkTypeGemini(PhoneConstants.GEMINI_SIM_1)));
                } else {
                    mDataConnectionTypeSim1.setText(Utility.getDataStateString(peerSimState));
                }
            } else {
                mDataConnectionTypeSim1.setText("");
            }
        }

        @Override
        public void onDataActivity(int direction) {
            mDataActivitySim2.setText(Utility.getDataDirectionString(direction));
        }
    };

    private Runnable mUpdateUIRunnable = new Runnable() {
        @Override
        public void run() {
            updateUI();
        }
    };
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        if (!FeatureOption.MTK_GEMINI_SUPPORT) {
            Log.e(TAG, "!!!!!NOT GEMINI LOAD!!!!!");
        }

        mTelephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        mTelephony = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
        mSwitch3GHandler = new Switch3GHandler(this, mTelephony, mUpdateUIRunnable);

        mDefaultSim1 = (Button) findViewById(R.id.btn_default_sim_sim1);
        mDefaultSim2 = (Button) findViewById(R.id.btn_default_sim_sim2);
        mOperatorSim1 = (TextView) findViewById(R.id.operator_sim1);
        mOperatorSim2 = (TextView) findViewById(R.id.operator_sim2);
        mStatusbarNotification = (Button) findViewById(R.id.btn_status_bar_notification);
        mDefaultDataSim1 = (Button) findViewById(R.id.btn_default_data_sim1);
        mDefaultDataSim2 = (Button) findViewById(R.id.btn_default_data_sim2);
        mDefaultDataOff = (Button) findViewById(R.id.btn_default_data_off);
        mNetworkTypeSim1 = (TextView) findViewById(R.id.network_type_sim1);
        mNetworkTypeSim2 = (TextView) findViewById(R.id.network_type_sim2);
        mSignalSim1 = (ProgressBar) findViewById(R.id.progress_signal_sim1);
        mSignalSim2 = (ProgressBar) findViewById(R.id.progress_signal_sim2);
        mDataConnectionTypeSim1 = (TextView) findViewById(R.id.data_connection_type_sim1);
        mDataConnectionTypeSim2 = (TextView) findViewById(R.id.data_connection_type_sim2);
        mDataActivitySim1 = (TextView) findViewById(R.id.data_activity_sim1);
        mDataActivitySim2 = (TextView) findViewById(R.id.data_activity_sim2);
        m3GSim1 = (Button) findViewById(R.id.btn_3g_sim1);
        m3GSim2 = (Button) findViewById(R.id.btn_3g_sim2);
        m3GSimOff = (Button) findViewById(R.id.btn_3g_off);

        mDefaultSim1.setOnClickListener(mDefaultSimOnClickListener);
        mDefaultSim2.setOnClickListener(mDefaultSimOnClickListener);
        mStatusbarNotification.setOnClickListener(mStatusbarNotificationOnClickListener);
        mDefaultDataSim1.setOnClickListener(mDefaultDataSimOnClickListener);
        mDefaultDataSim2.setOnClickListener(mDefaultDataSimOnClickListener);
        mDefaultDataOff.setOnClickListener(mDefaultDataSimOnClickListener);
        m3GSim1.setOnClickListener(m3GSimOnClickListener);
        m3GSim2.setOnClickListener(m3GSimOnClickListener);
        m3GSimOff.setOnClickListener(m3GSimOnClickListener);

        mTelephonyManager.listenGemini(mPhone1StateListener,
                PhoneStateListener.LISTEN_SIGNAL_STRENGTHS |
                PhoneStateListener.LISTEN_SERVICE_STATE |
                PhoneStateListener.LISTEN_DATA_CONNECTION_STATE |
                PhoneStateListener.LISTEN_DATA_ACTIVITY,
                PhoneConstants.GEMINI_SIM_1);
        mTelephonyManager.listenGemini(mPhone2StateListener,
                PhoneStateListener.LISTEN_SIGNAL_STRENGTHS |
                PhoneStateListener.LISTEN_SERVICE_STATE |
                PhoneStateListener.LISTEN_DATA_CONNECTION_STATE |
                PhoneStateListener.LISTEN_DATA_ACTIVITY,
                PhoneConstants.GEMINI_SIM_2);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_DATA_DEFAULT_SIM_CHANGED);
        registerReceiver(mBroadcastReceiver, intentFilter);

        mIsStatusBarNotificationEnabled = BSPTelephonyDevToolService.isRunning();
        mDefaultSimId = SystemProperties.getInt(PhoneConstants.GEMINI_DEFAULT_SIM_PROP, PhoneConstants.GEMINI_SIM_1);

        long dataSiminfoId = Settings.System.getLong(getContentResolver(),
                Settings.System.GPRS_CONNECTION_SIM_SETTING, Settings.System.DEFAULT_SIM_NOT_SET);
        mDefaultDataSimId = SIMInfo.getSlotById(this, dataSiminfoId);

        updateUI();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mTelephonyManager.listenGemini(mPhone1StateListener, PhoneStateListener.LISTEN_NONE, PhoneConstants.GEMINI_SIM_1);
        mTelephonyManager.listenGemini(mPhone2StateListener, PhoneStateListener.LISTEN_NONE, PhoneConstants.GEMINI_SIM_2);
        unregisterReceiver(mBroadcastReceiver);
    }

    private void updateUI() {
        if (mDefaultSimId == PhoneConstants.GEMINI_SIM_1) {
            mDefaultSim1.setEnabled(false);
            mDefaultSim2.setEnabled(true);
        } else {
            mDefaultSim1.setEnabled(true);
            mDefaultSim2.setEnabled(false);
        }

        if (mIsStatusBarNotificationEnabled) {
            mStatusbarNotification.setText(R.string.stop_statusbar_notification);
        } else {
            mStatusbarNotification.setText(R.string.start_status_bar_notification);
        }

        if (mDefaultDataSimId == PhoneConstants.GEMINI_SIM_1) {
            mDefaultDataSim1.setEnabled(false);
            mDefaultDataSim2.setEnabled(true);
            mDefaultDataOff.setEnabled(true);
        } else if (mDefaultDataSimId == PhoneConstants.GEMINI_SIM_2) {
            mDefaultDataSim1.setEnabled(true);
            mDefaultDataSim2.setEnabled(false);
            mDefaultDataOff.setEnabled(true);
        } else {
            mDefaultDataSim1.setEnabled(true);
            mDefaultDataSim2.setEnabled(true);
            mDefaultDataOff.setEnabled(false);
        }

        int sim3G = mSwitch3GHandler.get3GCapabilitySIM();
        if (sim3G == PhoneConstants.GEMINI_SIM_1) {
            m3GSim1.setEnabled(false);
            m3GSim2.setEnabled(true);
            m3GSimOff.setEnabled(true);
        } else if (sim3G == PhoneConstants.GEMINI_SIM_2) {
            m3GSim1.setEnabled(true);
            m3GSim2.setEnabled(false);
            m3GSimOff.setEnabled(true);
        } else {
            m3GSim1.setEnabled(true);
            m3GSim2.setEnabled(true);
            m3GSimOff.setEnabled(false);
        }
    }

    private void updateSignalStrengthUI(ProgressBar progressBar, int signalStrength) {
        progressBar.setMax(31);
        if (signalStrength == 99) {
            progressBar.setProgress(0);
            progressBar.setEnabled(false);
        } else {
            progressBar.setProgress(signalStrength);
            progressBar.setEnabled(true);
        }
    }

    private void rebootAlert() {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
                        pm.reboot("");
                        Toast.makeText(getApplicationContext(), R.string.restarting_device, Toast.LENGTH_LONG).show();
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        break;
                }
            }
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.reboot_confirm_notice).setPositiveButton(R.string.yes, dialogClickListener)
            .setNegativeButton(R.string.no, dialogClickListener).show();
    }
}