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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.RemoteViews;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import android.provider.Telephony.SIMInfo;
import com.android.internal.telephony.ITelephony;
import android.os.SystemProperties;
import android.os.ServiceManager;

public class BSPTelephonyDevToolService extends Service {
    private static final String TAG = "BSP_Telephony_Dev_Service";
    private static final int NOTIFICATION_ID_SIM_1 = 0x500;
    private static final int NOTIFICATION_ID_SIM_2 = 0x520;

    private static boolean mIsRunning;

    private TelephonyManager mTelephonyManager;
    private ITelephony mTelephony;
    private NotificationManager mNotificationManager;
    private Notification mSim1Notification = new Notification();
    private Notification mSim2Notification = new Notification();

    private SignalStrength mSim1SignalStrength;
    private SignalStrength mSim2SignalStrength;
    private int mSim1DataNetworkType = TelephonyManager.NETWORK_TYPE_UNKNOWN;
    private int mSim2DataNetworkType = TelephonyManager.NETWORK_TYPE_UNKNOWN;
    private int mSim1DataDirection = TelephonyManager.DATA_ACTIVITY_NONE;
    private int mSim2DataDirection = TelephonyManager.DATA_ACTIVITY_NONE;

    private PhoneStateListener mPhone1StateListener = new PhoneStateListener() {
        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            mSim1SignalStrength = signalStrength;
            updateNotifications(PhoneConstants.GEMINI_SIM_1);
        }

        @Override
        public void onServiceStateChanged(ServiceState serviceState) {
            updateNotifications(PhoneConstants.GEMINI_SIM_1);
        }

        @Override
        public void onDataConnectionStateChanged(int state, int networkType) {
            mSim1DataNetworkType = networkType;
            updateNotifications(PhoneConstants.GEMINI_SIM_1);
        }

        @Override
        public void onDataActivity(int direction) {
            mSim1DataDirection = direction;
            updateNotifications(PhoneConstants.GEMINI_SIM_1);
        }
    };

    private PhoneStateListener mPhone2StateListener = new PhoneStateListener() {
        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            mSim2SignalStrength = signalStrength;
            updateNotifications(PhoneConstants.GEMINI_SIM_2);
        }

        @Override
        public void onServiceStateChanged(ServiceState serviceState) {
            updateNotifications(PhoneConstants.GEMINI_SIM_2);
        }

        @Override
        public void onDataConnectionStateChanged(int state, int networkType) {
            mSim2DataNetworkType = networkType;
            updateNotifications(PhoneConstants.GEMINI_SIM_2);
        }

        @Override
        public void onDataActivity(int direction) {
            mSim2DataDirection = direction;
            updateNotifications(PhoneConstants.GEMINI_SIM_2);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "BSP package telephony dev service started");
        mIsRunning = true;
        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        mTelephony = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

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

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, BSPTelephonyDevToolActivity.class), 0);

        mSim1Notification.flags = Notification.FLAG_NO_CLEAR;
        mSim1Notification.contentIntent = contentIntent;
        mSim1Notification.icon = R.drawable.ic_launcher;

        mSim2Notification.flags = Notification.FLAG_NO_CLEAR;
        mSim2Notification.contentIntent = contentIntent;
        mSim2Notification.icon = R.drawable.ic_launcher;

        updateNotifications(PhoneConstants.GEMINI_SIM_2);
        updateNotifications(PhoneConstants.GEMINI_SIM_1);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mTelephonyManager.listenGemini(mPhone1StateListener, PhoneStateListener.LISTEN_NONE, PhoneConstants.GEMINI_SIM_1);
        mTelephonyManager.listenGemini(mPhone2StateListener, PhoneStateListener.LISTEN_NONE, PhoneConstants.GEMINI_SIM_2);
        mIsRunning = false;
        mNotificationManager.cancel(NOTIFICATION_ID_SIM_1);
        mNotificationManager.cancel(NOTIFICATION_ID_SIM_2);
        Log.i(TAG, "BSP package telephony dev service stopped");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    static public boolean isRunning() {
        return mIsRunning;
    }

    private void updateNotifications(int simId) {
        if (simId == PhoneConstants.GEMINI_SIM_1) {
            mSim1Notification.contentView = getNotificationRemoteViews(simId);
            mNotificationManager.notify(NOTIFICATION_ID_SIM_1, mSim1Notification);
        } else {
            mSim2Notification.contentView = getNotificationRemoteViews(simId);
            mNotificationManager.notify(NOTIFICATION_ID_SIM_2, mSim2Notification);
        }
    }

    private RemoteViews getNotificationRemoteViews(int simId) {
        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.notification);
        String simIdText = getString(R.string.sim) + (simId+1);
        remoteViews.setTextViewText(R.id.notification_sim_id, simIdText);
        remoteViews.setTextViewText(R.id.notification_network_type,
                Utility.getNetworkTypeString(mTelephonyManager.getNetworkTypeGemini(simId)));

        int signalStrength = 0;

        if (simId == PhoneConstants.GEMINI_SIM_1) {
            remoteViews.setTextViewText(R.id.notification_data_activity,
                    Utility.getDataDirectionString(mSim1DataDirection));

            if (mTelephonyManager.getDataStateGemini(simId) == TelephonyManager.DATA_CONNECTED) {
                remoteViews.setTextViewText(R.id.notification_data_connection_type,
                        Utility.getNetworkTypeString(mSim1DataNetworkType));
            } else {
                remoteViews.setTextViewText(R.id.notification_data_connection_type, "");
            }

            if (mSim1SignalStrength != null)
                signalStrength = mSim1SignalStrength.getGsmSignalStrength();
        } else {
            remoteViews.setTextViewText(R.id.notification_data_activity,
                    Utility.getDataDirectionString(mSim2DataDirection));

            if (mTelephonyManager.getDataStateGemini(simId) == TelephonyManager.DATA_CONNECTED) {
                remoteViews.setTextViewText(R.id.notification_data_connection_type,
                        Utility.getNetworkTypeString(mSim2DataNetworkType));
            } else {
                remoteViews.setTextViewText(R.id.notification_data_connection_type, "");
            }

            if (mSim2SignalStrength != null)
                signalStrength = mSim2SignalStrength.getGsmSignalStrength();
        }

        if (signalStrength == 99)
            signalStrength = 0;
        remoteViews.setProgressBar(R.id.notification_progress_signal, 31, signalStrength, false);

        return remoteViews;
    }
}
