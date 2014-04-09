/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
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

package com.mediatek.phone.ext;

import android.util.Log;

import com.android.phone.PhoneGlobals;

import com.mediatek.pluginmanager.Plugin;
import com.mediatek.pluginmanager.Plugin.ObjectCreationException;
import com.mediatek.pluginmanager.PluginManager;

import java.util.LinkedList;

public final class ExtensionManager {

    private static final String LOG_TAG = "ExtensionManager";

    private static ExtensionManager sInstance;
    private PhonePluginExtensionContainer mPhonePluginExtContainer;
    private LinkedList<IPhonePlugin> mIPhonePluginList;

    private ExtensionManager() {
        mPhonePluginExtContainer = new PhonePluginExtensionContainer();
        initContainerByPlugin();
    }

    /**
     * 
     * @return ExtensionManager
     */
    public static ExtensionManager getInstance() {
        if (null == sInstance) {
            sInstance = new ExtensionManager();
        }
        return sInstance;
    }

    /**
     * 
     * @return InCallScreenExtension
     */
    public InCallScreenExtension getInCallScreenExtension() {
        log("getInCallScreenExtension()");
        return mPhonePluginExtContainer.getInCallScreenExtension();
    }

    /**
     * @return CallCardExtension
     */
    public CallCardExtension getCallCardExtension() {
        log("getCallCardExtension()");
        return mPhonePluginExtContainer.getCallCardExtension();
    }

    /**
     * 
     * @return InCallTouchUiExtension
     */
    public InCallTouchUiExtension getInCallTouchUiExtension() {
        log("getInCallTouchUiExtension()");
        return mPhonePluginExtContainer.getInCallTouchUiExtension();
    }

    /**
     * 
     * @return VTInCallScreenExtension
     */
    public VTInCallScreenExtension getVTInCallScreenExtension() {
        log("getVTInCallScreenExtension()");
        return mPhonePluginExtContainer.getVTInCallScreenExtension();
    }

    /**
     * @return VTCallBannerControllerExtension
     */
    public VTCallBannerControllerExtension getVTCallBannerControllerExtension() {
        log("getVTCallBannerControllerExtension()");
        return mPhonePluginExtContainer.getVTCallBannerControllerExtension();
    }

    /**
     * 
     * @return VTInCallScreenFlagsExtension
     */
    public VTInCallScreenFlagsExtension getVTInCallScreenFlagsExtension() {
        log("getVTInCallScreenFlagsExtension()");
        return mPhonePluginExtContainer.getVTInCallScreenFlagsExtension();
    }

    /**
     * Get PhoneGlobalsBroadcastReceiver extension object
     * @return PhoneGlobalsBroadcastReceiver extension object
     */
    public PhoneGlobalsBroadcastReceiverExtension getPhoneGlobalsBroadcastReceiverExtension() {
        log("PhoneGlobalsBroadcastReceiverExtension()");
        return mPhonePluginExtContainer.getPhoneGlobalsBroadcastReceiverExtension();
    }

    /**
     * 
     * @return OthersSettingsExtension
     */
    public OthersSettingsExtension getOthersSettingsExtension() {
        log("getOthersSettingsExtension()");
        return mPhonePluginExtContainer.getOthersSettingsExtension();
    }

    /**
     * 
     * @return SettingsExtension
     */
    public SettingsExtension getSettingsExtension() {
        log("getSettingsExtension()");
        return mPhonePluginExtContainer.getSettingsExtension();
    }

    private void initContainerByPlugin() {
        PluginManager<IPhonePlugin> pm = PluginManager.<IPhonePlugin>create(
                PhoneGlobals.getInstance(), IPhonePlugin.class.getName());
        try {
            for (int i = 0; i < pm.getPluginCount(); ++i) {
                Plugin<IPhonePlugin> plugIn = pm.getPlugin(i);
                if (null != plugIn) {
                    log("create plugin object, number = " + (i + 1));
                    IPhonePlugin phonePluginObject = plugIn.createObject();
                    mPhonePluginExtContainer.addExtensions(phonePluginObject);
                    if (null == mIPhonePluginList) {
                        log("create mIPhonePluglist");
                        mIPhonePluginList = new LinkedList<IPhonePlugin>();
                    }
                    mIPhonePluginList.add(phonePluginObject);
                }
            }
        } catch (ObjectCreationException e) {
            log("create plugin object failed");
            e.printStackTrace();
        }
    }

    private static void log(String msg) {
        Log.d(LOG_TAG, msg);
    }
}
