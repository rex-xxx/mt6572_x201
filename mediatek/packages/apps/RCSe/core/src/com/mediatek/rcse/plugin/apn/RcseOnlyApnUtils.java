/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2012. All rights reserved.
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

package com.mediatek.rcse.plugin.apn;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.android.internal.telephony.Phone;

import com.mediatek.rcse.api.Logger;

import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.utils.PhoneUtils;

/**
 * This class provide the mainly functions to start and stop RCS-e only APN
 */
public class RcseOnlyApnUtils {
    
    /**
     * Logger tag
     */
    private static final String TAG = "RcseOnlyApnUtils";
    
    /**
     * The RcseOnlyApnUtils instance
     */
    private static final RcseOnlyApnUtils INSTANCE = new RcseOnlyApnUtils();
    
    private Boolean mRcsOnlyApnStarted = Boolean.TRUE;
    
    private Context mContext = null;
    private ConnectivityManager mConnManager = null;
    private static boolean sIsInit = false;
    
    private static final int APN_ALREADY_ACTIVE = 0;
    private static final int APN_REQUEST_STARTED = 1;
    private static final int APN_TYPE_NOT_AVAILABLE = 2;
    private static final int APN_REQUEST_FAILED = 3;
    private static final int APN_ALREADY_INACTIVE = 4;
    
    private static final int STARTED_USING_RCSE_APN = 1;
    private static final int STOPPED_USING_RCSE_APN = 0;
    private static final int INVILIDE_OPERATION = -1;
    
    private Boolean mApnDebugMode = Boolean.FALSE;
    
    /**
     * Used for RCS-e only APN mock tool
     */
    private Boolean mMockedRomingState = Boolean.TRUE;
    public static final String ROAMING_MOCKED_ACTION = "com.mediatek.ROAMING_MOCKED_ACTION";
    
    /**
     * Get the RcseOnlyApnUtils instance
     * 
     * @return RcseOnlyApnUtils instance
     */
    public static RcseOnlyApnUtils getInstance(Context context) {
        INSTANCE.initialize(context);
        return INSTANCE;
    }
    
    /**
     * Initial the RcseOnlyApnUtils instance
     * 
     * @param context The context instance used by the RcseOnlyApnUtils instance
     */
    public void initialize(Context context) {
        synchronized (this) {
            if (!sIsInit) {
                sIsInit = true;
                mContext = context;
                mConnManager = (ConnectivityManager) context
                        .getSystemService(Context.CONNECTIVITY_SERVICE);
                mApnDebugMode = PhoneUtils.sIsApnDebug;
            }
        }
    }

    /**
     * Destroy the RcseOnlyApnUtils instance
     */
    public void destroy() {
        stopUsingRcseOnlyApn();
    }

    /**
     * Set the state for RCSe only APN
     * 
     * @param state The RCSe only APN
     */
    private void setRcsOnlyApnStarted(Boolean state) {
        mRcsOnlyApnStarted = state;
    }

    /**
     * Get the RCSe only APN state
     * @return
     */
    public boolean isRcsOnlyApnStarted() {
        return mRcsOnlyApnStarted.booleanValue();
    }
    
    /**
     * Launch the RCS-e only APN
     * 
     * @return 1 if the RCSe only APN is started, 0 if the RCSe only APN is
     *         stopped, -1 if operation error
     */
    public int switchRcseOnlyApn() {
        NetworkInfo info = mConnManager.getActiveNetworkInfo();
        boolean apnEnabledByServer = RcsSettings.getInstance().isRcseOnlyApnEnabled();
        Logger.v(TAG, "RCS-e only APN is enabled by server = " + apnEnabledByServer);
        int retVal = -1;
        if (!mApnDebugMode.booleanValue()) {
            if (info == null) {
                Logger.d(TAG, "The NetworkInfo instance is null");
                retVal = INVILIDE_OPERATION;
            } else {
                boolean mobileType = isMobileNetwork(info);
                boolean isRoaming = isMobileRoaming(info);
                if (mobileType && isRoaming && apnEnabledByServer) {
                    Logger.v(TAG, "It's mobile network = " + mobileType);
                    Logger.v(TAG, "It's mobile roaming = " + isRoaming);
                    startUsingRcseOnlyApn();
                    retVal = STARTED_USING_RCSE_APN;
                } else {
                    stopUsingRcseOnlyApn();
                    retVal = STOPPED_USING_RCSE_APN;
                }
            }
        } else {
            Logger.v(TAG, "The mocked roaming state = " + mMockedRomingState.booleanValue());
            if (mMockedRomingState.booleanValue() && apnEnabledByServer) {
                startUsingRcseOnlyApn();
                retVal = STARTED_USING_RCSE_APN;
            } else {
                stopUsingRcseOnlyApn();
                retVal = STOPPED_USING_RCSE_APN;
            }
        }
        return retVal;
    }
    
    private Boolean startUsingRcseOnlyApn() {
        int retVal = mConnManager.startUsingNetworkFeature(ConnectivityManager.TYPE_MOBILE,
                Phone.FEATURE_ENABLE_RCSE);
        boolean isSuccess = false;
        switch (retVal) {
            case APN_ALREADY_ACTIVE:
                isSuccess = true;
                setRcsOnlyApnStarted(Boolean.TRUE);
                Logger.d(TAG, "The network feature is already active");
                break;
            case APN_ALREADY_INACTIVE:
                Logger.d(TAG, "The network feature is already in-active");
                break;
            case APN_TYPE_NOT_AVAILABLE:
                Logger.d(TAG, "The network feature is not available");
                break;
            case APN_REQUEST_STARTED:
                isSuccess = true;
                setRcsOnlyApnStarted(Boolean.TRUE);
                Logger.d(TAG, "Start using network feature successed");
                break;
            case APN_REQUEST_FAILED:
                Logger.d(TAG, "Start using network feature failed");
                break;
            default:
                Logger.d(TAG, "Start using network feature failed by unknown reason");
                break;
        }
        return Boolean.valueOf(isSuccess);
    }

    private Boolean stopUsingRcseOnlyApn() {
        boolean isSuccess = false;
        if (isRcsOnlyApnStarted()) {
            int retVal = mConnManager.stopUsingNetworkFeature(ConnectivityManager.TYPE_MOBILE,
                    Phone.FEATURE_ENABLE_RCSE);
            if (retVal == -1) {
                Logger.d(TAG, "Stop using network feature failed");
            } else {
                Logger.d(TAG, "Stop using network feature successed");
                setRcsOnlyApnStarted(Boolean.FALSE);
                isSuccess = true;
            }
        } else {
            Logger.d(TAG, "The network feature is stoped yet");
            isSuccess = true;
        }
        return Boolean.valueOf(isSuccess);
    }

    private Boolean isMobileNetwork(NetworkInfo info) {
        if (info == null) {
            Logger.w(TAG, "The NetworkInfo instance is null");
            return Boolean.FALSE;
        } else {
            boolean retVal = (info.getType() == ConnectivityManager.TYPE_MOBILE ? true : false);
            return Boolean.valueOf(retVal);
        }
    }
    
    private Boolean isMobileRoaming(NetworkInfo info) {
        if (info == null) {
            Logger.w(TAG, "The NetworkInfo instance is null");
            return Boolean.FALSE;
        } else {
            boolean isRoaming = info.isRoaming();
            return Boolean.valueOf(isRoaming);
        }
    }
    
    /**
     * Set the mocked roaming state, used to test the RCSe only APN feature
     * 
     * @param state The roaming state to set
     * @param context Context instance
     */
    public void setMockedRoamingState(boolean state) {
        mMockedRomingState = Boolean.valueOf(state);
        Intent intent = new Intent();
        intent.setAction(ROAMING_MOCKED_ACTION);
        mContext.sendBroadcast(intent);
    }
}
