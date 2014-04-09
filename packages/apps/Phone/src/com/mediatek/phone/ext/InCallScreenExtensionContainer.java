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

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.android.internal.telephony.CallManager;
import com.android.internal.telephony.Connection;

import java.util.Iterator;
import java.util.LinkedList;

public class InCallScreenExtensionContainer extends InCallScreenExtension {

    private static final String LOG_TAG = "InCallScreenExtensionContainer";

    private LinkedList<InCallScreenExtension> mSubExtensionList;

    /**
     * 
     * @param extension 
     */
    public void add(InCallScreenExtension extension) {
        if (null == mSubExtensionList) {
            log("create sub extension list");
            mSubExtensionList = new LinkedList<InCallScreenExtension>();
        }
        log("add extension, extension is " + extension);
        mSubExtensionList.add(extension);
    }

    /**
     * 
     * @param extension 
     */
    public void remove(InCallScreenExtension extension) {
        if (null == mSubExtensionList) {
            log("remove extension, sub extension list is null, just return");
            return;
        }
        log("remove extension, extension is " + extension);
        mSubExtensionList.remove(extension);
    }

    /*public int getLayoutResID() {
        return -1;
    }*/

    /**
     * 
     * @param menu 
     * @param inCallControlState 
     */
    public void setupMenuItems(Menu menu, IInCallControlState inCallControlState) {
        if (null == mSubExtensionList) {
            log("setupMenuItems(), sub extension list is null, just return");
            return;
        }
        log("setupMenuItems(), menu is " + menu + ", incallcontrolstate is " + inCallControlState);
        Iterator<InCallScreenExtension> iterator = mSubExtensionList.iterator();
        while (iterator.hasNext()) {
            iterator.next().setupMenuItems(menu, inCallControlState);
        }
    }

    /**
     * 
     * @param menuItem 
     * @return 
     */
    public boolean handleOnScreenMenuItemClick(MenuItem menuItem) {
        if (null == mSubExtensionList) {
            log("handleOnScreenMenuItemClick(), sub extension list is null, just return");
            return false;
        }
        log("handleOnScreenMenuItemClick(), menuItem is " + menuItem);
        Iterator<InCallScreenExtension> iterator = mSubExtensionList.iterator();
        while (iterator.hasNext()) {
            InCallScreenExtension extension = iterator.next();
            if (extension.handleOnScreenMenuItemClick(menuItem)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 
     * @param callManager 
     * @param isForegroundActivity 
     * @return boolean
     */
    public boolean updateScreen(CallManager callManager, boolean isForegroundActivity) {
        if (null == mSubExtensionList) {
            log("updateScreen(), sub extension list is null, just return");
            return false;
        }
        log("updateScreen(), call manage is " + callManager + " isForeground is " + isForegroundActivity);
        Iterator<InCallScreenExtension> iterator = mSubExtensionList.iterator();
        while (iterator.hasNext()) {
            InCallScreenExtension extension = iterator.next();
            if (extension.updateScreen(callManager, isForegroundActivity)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 
     * @param icicle 
     * @param inCallScreenActivity 
     * @param inCallScreenHost 
     * @param cm 
     */
    public void onCreate(Bundle icicle, Activity inCallScreenActivity,
            IInCallScreen inCallScreenHost, CallManager cm) {
        if (null == mSubExtensionList) {
            log("onCreate(), sub extension list is null, just return");
            return;
        }
        log("onCreate(), icicle is " + icicle + " incallscreen activity is " + inCallScreenActivity +
                " incallscreen host is " + inCallScreenHost + " call manager is " + cm);
        Iterator<InCallScreenExtension> iterator = mSubExtensionList.iterator();
        while (iterator.hasNext()) {
            iterator.next().onCreate(icicle, inCallScreenActivity, inCallScreenHost, cm);
        }
    }

    /**
     * 
     * @param inCallScreen 
     */
    public void onDestroy(Activity inCallScreen) {
        if (null == mSubExtensionList) {
            log("onDestroy(), sub extension list is null, just return");
            return;
        }
        log("onDestroy(), incallscreen activity is " + inCallScreen);
        Iterator<InCallScreenExtension> iterator = mSubExtensionList.iterator();
        while (iterator.hasNext()) {
            iterator.next().onDestroy(inCallScreen);
        }
    }

    /**
     * 
     * @param cn 
     * @return boolean
     */
    public boolean onDisconnect(Connection cn) {
        if (null == mSubExtensionList) {
            log("onDisconnect(), sub extension list is null, just return");
            return false;
        }
        log("onDisconnect(), connection is " + cn);
        Iterator<InCallScreenExtension> iterator = mSubExtensionList.iterator();
        while (iterator.hasNext()) {
            InCallScreenExtension extension = iterator.next();
            if (extension.onDisconnect(cn)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 
     * @param cm 
     * @return 
     */
    public boolean onPhoneStateChanged(CallManager cm) {
        if (null == mSubExtensionList) {
            log("onPhoneStateChanged(), sub extension list is null, just return");
            return false;
        }
        log("onPhoneStateChanged(), call manager is " + cm);
        Iterator<InCallScreenExtension> iterator = mSubExtensionList.iterator();
        while (iterator.hasNext()) {
            InCallScreenExtension extension = iterator.next();
            if (extension.onPhoneStateChanged(cm)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 
     * @param id 
     * @return 
     */
    public boolean handleOnscreenButtonClick(int id) {
        if (null == mSubExtensionList) {
            log("handleOnscreenButtonClick(), sub extension list is null, just return");
            return false;
        }
        log("handleOnscreenButtonClick(), id = " + id);
        Iterator<InCallScreenExtension> iterator = mSubExtensionList.iterator();
        while (iterator.hasNext()) {
            InCallScreenExtension extension = iterator.next();
            if (extension.handleOnscreenButtonClick(id)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 
     * @return boolean
     */
    public boolean dismissDialogs() {
        if (null == mSubExtensionList) {
            log("dismissDialogs(), sub extension list is null, just return");
            return false;
        }
        log("dismissDialogs()");
        Iterator<InCallScreenExtension> iterator = mSubExtensionList.iterator();
        while (iterator.hasNext()) {
            InCallScreenExtension extension = iterator.next();
            if (extension.dismissDialogs()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 
     * @param msg
     */
    private static void log(String msg) {
        Log.d(LOG_TAG, msg);
    }
}
