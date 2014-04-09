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

package com.mediatek.oobe.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.os.SystemProperties;
import android.provider.Telephony;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.RILConstants;

import com.mediatek.pluginmanager.PluginManager;
import com.mediatek.pluginmanager.Plugin;
import com.mediatek.pluginmanager.Plugin.ObjectCreationException;
import com.mediatek.oobe.ext.DefaultSimManagementExt;
import com.mediatek.oobe.ext.DefaultWifiExt;
import com.mediatek.oobe.ext.DefaultWifiSettingsExt;
import com.mediatek.oobe.ext.ISimManagementExt;
import com.mediatek.oobe.ext.IWifiExt;
import com.mediatek.oobe.ext.IWifiSettingsExt;
import com.mediatek.xlog.Xlog;

import java.net.InetAddress;
import java.util.Iterator;

/**
 * Contains utility functions for getting framework resource
 */
public class Utils {
    private static final int COLORNUM = 7;
    /**


    public static int getStatusResource(int state) {

        switch (state) {
        case PhoneConstants.SIM_INDICATOR_RADIOOFF:
            return com.mediatek.internal.R.drawable.sim_radio_off;
        case PhoneConstants.SIM_INDICATOR_LOCKED:
            return com.mediatek.internal.R.drawable.sim_locked;
        case PhoneConstants.SIM_INDICATOR_INVALID:
            return com.mediatek.internal.R.drawable.sim_invalid;
        case PhoneConstants.SIM_INDICATOR_SEARCHING:
            return com.mediatek.internal.R.drawable.sim_searching;
        case PhoneConstants.SIM_INDICATOR_ROAMING:
            return com.mediatek.internal.R.drawable.sim_roaming;
        case PhoneConstants.SIM_INDICATOR_CONNECTED:
            return com.mediatek.internal.R.drawable.sim_connected;
        case PhoneConstants.SIM_INDICATOR_ROAMINGCONNECTED:
            return com.mediatek.internal.R.drawable.sim_roaming_connected;
        default:
            return -1;
        }
    }

    public static int getSimColorResource(int color) {

        if ((color >= 0) && (color <= COLORNUM)) {
            return Telephony.SIMBackgroundDarkRes[color];
        } else {
            return -1;
        }

    }
     */
    /**
     * judge if is gemini PhoneConstants
     * @return true or false
     */
    public static boolean isGemini() {
        int networkMode = SystemProperties.getInt(Phone.GEMINI_DEFAULT_SIM_MODE, RILConstants.NETWORK_MODE_GEMINI);
        Xlog.d(TAG, " isGemini() networkMode=" + networkMode);
        return (networkMode == RILConstants.NETWORK_MODE_GEMINI);
    }

    private static final String TAG = "OOBE";

    /**
     * if is Wifi Only
     * @param context Context
     * @return boolean
     */
    public static boolean isWifiOnly(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return (!cm.isNetworkSupported(ConnectivityManager.TYPE_MOBILE));
    }

    /**
     * M: create wifi plugin object
     * @param context Context
     * @return IWifiExt
     */
    public static IWifiExt getWifiPlugin(Context context) {
        IWifiExt ext;
        try {
            ext = (IWifiExt)PluginManager.createPluginObject(context,
                IWifiExt.class.getName());
            Xlog.d(TAG , "IWifiExt plugin object created");
        } catch (Plugin.ObjectCreationException e) {
            ext = new DefaultWifiExt(context);    
            Xlog.d(TAG , "DefaultWifiExt plugin object created, e = " + e);
        }
        return ext;
    }
    /**
     * M: create wifi settings plugin object
     * @param context Context context
     * @return IWifiSettingsExt
     */
    public static IWifiSettingsExt getWifiSettingsPlugin(Context context) {
        IWifiSettingsExt ext;
        try {
            ext = (IWifiSettingsExt)PluginManager.createPluginObject(context,
                IWifiSettingsExt.class.getName());
            Xlog.d(TAG , "IWifiSettingsExt Plugin object created");
        } catch (Plugin.ObjectCreationException e) {
            ext = new DefaultWifiSettingsExt(context);    
            Xlog.d(TAG , "DefaultWifiSettingsExt Plugin object created, e = " + e);
        }
        return ext;
    }
    
    /**
     * M: for sim management update preference
     * @param context Context
     * @return ISimManagementExt
     */
    public static ISimManagementExt getSimManagmentExtPlugin(Context context) {
        ISimManagementExt ext;
        try {
            ext = (ISimManagementExt)PluginManager.createPluginObject(context,
                    ISimManagementExt.class.getName());
        } catch (Plugin.ObjectCreationException e) {
            Xlog.d(TAG,"Enter the default ISimManagementExt");
            ext = new DefaultSimManagementExt(); 
        }
        return ext;
    }
}
