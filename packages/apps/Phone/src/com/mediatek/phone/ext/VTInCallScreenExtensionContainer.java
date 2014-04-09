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
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.android.internal.telephony.CallManager;

import java.util.Iterator;
import java.util.LinkedList;

public class VTInCallScreenExtensionContainer extends VTInCallScreenExtension {

    private static final String LOG_TAG = "VTInCallScreenExtensionContainer";

    private LinkedList<VTInCallScreenExtension> mSubExtensionList;

    /**
     * 
     * @param extension 
     */
    public void add(VTInCallScreenExtension extension) {
        if (null == mSubExtensionList) {
            log("create sub extension list");
            mSubExtensionList = new LinkedList<VTInCallScreenExtension>();
        }
        log("add extension, extension is " + extension);
        mSubExtensionList.add(extension);
    }

    /**
     * 
     * @param extension 
     */
    public void remove(VTInCallScreenExtension extension) {
        if (null == mSubExtensionList) {
            log("remove extension, sub extension list is null, just return");
            return;
        }
        log("remove extension, extension is " + extension);
        mSubExtensionList.remove(extension);
    }

    //public int getLayoutResID() {
    //    return -1;
    //}

    /**
     * 
     * @param vtInCallScreen 
     * @param touchListener 
     * @param inCallScreen 
     * @return initVTInCallScreen
     */
    public boolean initVTInCallScreen(ViewGroup vtInCallScreen, View.OnTouchListener touchListener,
                                      Activity inCallScreen) {
        if (null == mSubExtensionList) {
            log("initVTInCallScreen(), sub extension list is null, just return");
            return false;
        }
        log("initVTInCallScreen(), vtInCallScreen is " + vtInCallScreen + ", touchListener is " + touchListener +
                ", inCallScreen is " + inCallScreen);
        Iterator<VTInCallScreenExtension> iterator = mSubExtensionList.iterator();
        while (iterator.hasNext()) {
            VTInCallScreenExtension extension = iterator.next();
            if (extension.initVTInCallScreen(vtInCallScreen, touchListener, inCallScreen)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 
     * @param v 
     * @param event 
     * @return 
     */
    public boolean onTouch(View v, MotionEvent event) {
        if (null == mSubExtensionList) {
            log("onTouch(), sub extension list is null, just return");
            return false;
        }
        log("onTouch(), view is " + v + ", event is " + event);
        Iterator<VTInCallScreenExtension> iterator = mSubExtensionList.iterator();
        while (iterator.hasNext()) {
            VTInCallScreenExtension extension = iterator.next();
            if (extension.onTouch(v, event)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 
     * @return boolean
     */
    public boolean internalAnswerVTCallPre() {
        if (null == mSubExtensionList) {
            log("internalAnswerVTCallPre(), sub extension list is null, just return");
            return false;
        }
        log("internalAnswerVTCallPre()");
        Iterator<VTInCallScreenExtension> iterator = mSubExtensionList.iterator();
        while (iterator.hasNext()) {
            VTInCallScreenExtension extension = iterator.next();
            if (extension.internalAnswerVTCallPre()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 
     * @return boolean
     */
    public boolean initDialingSuccessVTState() {
        if (null == mSubExtensionList) {
            log("initDialingSuccessVTState(), sub extension list is null, just return");
            return false;
        }
        log("initDialingSuccessVTState()");
        Iterator<VTInCallScreenExtension> iterator = mSubExtensionList.iterator();
        while (iterator.hasNext()) {
            VTInCallScreenExtension extension = iterator.next();
            if (extension.initDialingSuccessVTState()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 
     * @param keyCode 
     * @param event 
     * @return boolean
     */
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (null == mSubExtensionList) {
            log("onKeyDown(), sub extension list is null, just return");
            return false;
        }
        log("onKeyDown(), keyCode is " + keyCode + ", event is " + event);
        Iterator<VTInCallScreenExtension> iterator = mSubExtensionList.iterator();
        while (iterator.hasNext()) {
            VTInCallScreenExtension extension = iterator.next();
            if (extension.onKeyDown(keyCode, event)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 
     * @param menu 
     * @return boolean
     */
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (null == mSubExtensionList) {
            log("onPrepareOptionMenu(), sub extension list is null, just return");
            return false;
        }
        log("onPrepareOptionMenu(), menu is " + menu);
        Iterator<VTInCallScreenExtension> iterator = mSubExtensionList.iterator();
        while (iterator.hasNext()) {
            VTInCallScreenExtension extension = iterator.next();
            if (extension.onPrepareOptionsMenu(menu)) {
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
    public boolean onReceiveVTManagerStartCounter(CallManager cm) {
        if (null == mSubExtensionList) {
            log("onReceiveVTManagerStartCounter(), sub extension list is null, just return");
            return false;
        }
        log("onReceiveVTManagerStartCounter(), call manager is " + cm);
        Iterator<VTInCallScreenExtension> iterator = mSubExtensionList.iterator();
        while (iterator.hasNext()) {
            VTInCallScreenExtension extension = iterator.next();
            if (extension.onReceiveVTManagerStartCounter(cm)) {
                return true;
            }
        }
        return false;
    }

    private static void log(String msg) {
        Log.d(LOG_TAG, msg);
    }
}
