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
import android.view.View;

import com.android.internal.telephony.CallManager;
import com.android.internal.telephony.PhoneConstants;

import java.util.Iterator;
import java.util.LinkedList;

public class CallCardExtensionContainer extends CallCardExtension {

    private static final String LOG_TAG = "CallCardExtensionContainer";

    private LinkedList<CallCardExtension> mSubExtensionList;

    /**
     * @param extension
     */
    public void add(CallCardExtension extension) {
        if (null == mSubExtensionList) {
            log("create sub extension list");
            mSubExtensionList = new LinkedList<CallCardExtension>();
        }
        log("add extension, extension is " + extension);
        mSubExtensionList.add(extension);
    }

    /**
     * 
     * @param extension 
     */
    public void remove(CallCardExtension extension) {
        if (null == mSubExtensionList) {
            log("remove extension, sub extension list is null, just return");
            return;
        }
        log("remove extension, extension is " + extension);
        mSubExtensionList.remove(extension);
    }

    /**
     * 
     * @param callCard 
     */
    public void onFinishInflate(View callCard) {
        if (null == mSubExtensionList) {
            log("onFinishInflate(), sub extension list is null, just return");
            return;
        }
        log("onFinishInflate(), callCard is " + callCard);
        Iterator<CallCardExtension> iterator = mSubExtensionList.iterator();
        while (iterator.hasNext()) {
            iterator.next().onFinishInflate(callCard);
        }
    }

    /**
     * 
     * @param state 
     * @return boolean 
     */
    public boolean updateCallInfoLayout(PhoneConstants.State state) {
        if (null == mSubExtensionList) {
            log("updateCallInfoLayout(), sub extension list is null, just return");
            return false;
        }
        log("updateCallInfoLayout(), phone state is " + state);
        Iterator<CallCardExtension> iterator = mSubExtensionList.iterator();
        while (iterator.hasNext()) {
            CallCardExtension extension = iterator.next();
            if (extension.updateCallInfoLayout(state)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 
     * @param cm 
     */
    public void updateState(CallManager cm) {
        if (null == mSubExtensionList) {
            log("updateState(), sub extension list is null, just return");
            return;
        }
        log("updateState(), call manager is " + cm);
        Iterator<CallCardExtension> iterator = mSubExtensionList.iterator();
        while (iterator.hasNext()) {
            iterator.next().updateState(cm);
        }
    }

    /**
     * 
     * @param msg 
     */
    private static void log(String msg) {
        Log.d(LOG_TAG, msg);
    }
}
