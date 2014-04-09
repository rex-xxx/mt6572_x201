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

public class PhonePluginExtensionContainer {

    private static final String LOG_TAG = "PhonePluginExtensionContainer";

    private InCallScreenExtensionContainer mInCallScreenExtensionContainer;
    private CallCardExtensionContainer mCallCardExtensionContainer;
    private InCallTouchUiExtensionContainer mInCallTouchUiExtensionContainer;
    private VTCallBannerControllerExtensionContainer mVTCallBannerControllerExtensionContainer;
    private VTInCallScreenExtensionContainer mVTInCallScreenExtensionContainer;
    private VTInCallScreenFlagsExtensionContainer mVTInCallScreenFlagsExtensionContainer;
    private PhoneGlobalsBroadcastReceiverExtensionContainer mPhoneGlobalsBroadcastReceiverExtensionContainer;

    private OthersSettingsExtension mOthersSettingsExtension;
    private SettingsExtension mSettingsExtension;

    public PhonePluginExtensionContainer() {
        mInCallScreenExtensionContainer = new InCallScreenExtensionContainer();
        mCallCardExtensionContainer = new CallCardExtensionContainer();
        mInCallTouchUiExtensionContainer = new InCallTouchUiExtensionContainer();
        mVTCallBannerControllerExtensionContainer = new VTCallBannerControllerExtensionContainer();
        mVTInCallScreenExtensionContainer = new VTInCallScreenExtensionContainer();
        mVTInCallScreenFlagsExtensionContainer = new VTInCallScreenFlagsExtensionContainer();
        mPhoneGlobalsBroadcastReceiverExtensionContainer = new PhoneGlobalsBroadcastReceiverExtensionContainer();
    }

    /**
     * 
     * @return InCallScreenExtension
     */
    public InCallScreenExtension getInCallScreenExtension() {
        log("getInCallScreenExtension()");
        return mInCallScreenExtensionContainer;
    }

    /**
     * 
     * @return CallCardExtension
     */
    public CallCardExtension getCallCardExtension() {
        log("getCallCardExtension()");
        return mCallCardExtensionContainer;
    }

    /**
     * 
     * @return InCallTouchUiExtension
     */
    public InCallTouchUiExtension getInCallTouchUiExtension() {
        log("getInCallTouchUiExtension()");
        return mInCallTouchUiExtensionContainer;
    }

    /**
     * 
     * @return VTCallBannerControllerExtension
     */
    public VTCallBannerControllerExtension getVTCallBannerControllerExtension() {
        log("getVTCallBannerControllerExtension()");
        return mVTCallBannerControllerExtensionContainer;
    }

    /**
     * 
     * @return VTInCallScreenExtension
     */
    public VTInCallScreenExtension getVTInCallScreenExtension() {
        log("getVTInCallScreenExtension()");
        return mVTInCallScreenExtensionContainer;
    }

    /**
     * 
     * @return VTInCallScreenFlagsExtension
     */
    public VTInCallScreenFlagsExtension getVTInCallScreenFlagsExtension() {
        log("getVTInCallScreenFlagsExtension()");
        return mVTInCallScreenFlagsExtensionContainer;
    }

    /**
     * Get PhoneGlobalsBroadcastReceiverExtension object
     * @return PhoneGlobalsBroadcastReceiverExtension object
     */
    public PhoneGlobalsBroadcastReceiverExtension getPhoneGlobalsBroadcastReceiverExtension() {
        log("getPhoneGlobalsBroadcastReceiverExtension()");
        return mPhoneGlobalsBroadcastReceiverExtensionContainer;
    }

    /**
     * 
     * @return OthersSettingsExtension
     */
    public OthersSettingsExtension getOthersSettingsExtension() {
        if (null == mOthersSettingsExtension) {
            mOthersSettingsExtension = new OthersSettingsExtension();
        }
        return mOthersSettingsExtension;
    }

    /**
     * 
     * @return SettingsExtension
     */
    public SettingsExtension getSettingsExtension() {
        if (null == mSettingsExtension) {
            mSettingsExtension = new SettingsExtension();
        }
        return mSettingsExtension;
    }

    /**
     * 
     * @param phonePlugin 
     */
    public void addExtensions(IPhonePlugin phonePlugin) {
        log("addExtensions, phone plugin object is " + phonePlugin);

        mInCallScreenExtensionContainer.add(phonePlugin.createInCallScreenExtension());
        mCallCardExtensionContainer.add(phonePlugin.createCallCardExtension());
        mInCallTouchUiExtensionContainer.add(phonePlugin.createInCallTouchUiExtension());
        mVTCallBannerControllerExtensionContainer.add(phonePlugin.createVTCallBannerControllerExtension());
        mVTInCallScreenExtensionContainer.add(phonePlugin.createVTInCallScreenExtension());
        mVTInCallScreenFlagsExtensionContainer.add(phonePlugin.createVTInCallScreenFlagsExtension());
        mPhoneGlobalsBroadcastReceiverExtensionContainer.add(phonePlugin.createPhoneGlobalsBroadcastReceiverExtension());

        if (null == mOthersSettingsExtension) {
            mOthersSettingsExtension = phonePlugin.createOthersSettingsExtension();
        }
        if (null == mSettingsExtension) {
            mSettingsExtension = phonePlugin.createSettingsExtension();
        }
    }

    private static void log(String msg) {
        Log.d(LOG_TAG, msg);
    }
}
