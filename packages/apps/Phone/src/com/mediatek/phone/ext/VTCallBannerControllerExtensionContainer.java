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

import android.content.Context;
import android.util.Log;
import android.view.ViewGroup;

import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallerInfo;
import com.android.internal.telephony.Connection;

import java.util.Iterator;
import java.util.LinkedList;

public class VTCallBannerControllerExtensionContainer extends VTCallBannerControllerExtension {

    private static final String LOG_TAG = "VTCallBannerControllerExtension";

    private LinkedList<VTCallBannerControllerExtension> mSubExtensionList;

    /**
     * 
     * @param extension 
     */
    public void add(VTCallBannerControllerExtension extension) {
        if (null == mSubExtensionList) {
            log("create sub extension list");
            mSubExtensionList = new LinkedList<VTCallBannerControllerExtension>();
        }
        log("add extension, extension is " + extension);
        mSubExtensionList.add(extension);
    }

    /**
     * 
     * @param extension 
     */
    public void remove(VTCallBannerControllerExtension extension) {
        if (null == mSubExtensionList) {
            log("remove extension, sub extension list is null, just return");
            return;
        }
        log("remove extension, extension is " + extension);
        mSubExtensionList.remove(extension);
    }

    /**
     * 
     * @param context 
     * @param vtCallBanner 
     */
    public void initialize(Context context, ViewGroup vtCallBanner) {
        if (null == mSubExtensionList) {
            log("initialize(), sub extension list is null, just return");
            return;
        }
        log("initialize(), context is " + context + " vtCallBanner is " + vtCallBanner);
        Iterator<VTCallBannerControllerExtension> iterator = mSubExtensionList.iterator();
        while (iterator.hasNext()) {
            iterator.next().initialize(context, vtCallBanner);
        }
    }

    /**
     * 
     * @param call 
     * @return 
     */
    public boolean updateCallStateWidgets(Call call) {
        if (null == mSubExtensionList) {
            log("updateCallStateWidgets(), sub extension list is null, just return");
            return false;
        }
        log("updateCallStateWidgets(), call is " + call);
        Iterator<VTCallBannerControllerExtension> iterator = mSubExtensionList.iterator();
        while (iterator.hasNext()) {
            VTCallBannerControllerExtension extension = iterator.next();
            if (extension.updateCallStateWidgets(call)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 
     * @param call 
     * @return 
     */
    public boolean updateState(Call call) {
        if (null == mSubExtensionList) {
            log("updateState(), sub extension list is null, just return");
            return false;
        }
        log("updateState(), call is " + call);
        Iterator<VTCallBannerControllerExtension> iterator = mSubExtensionList.iterator();
        while (iterator.hasNext()) {
            VTCallBannerControllerExtension extension = iterator.next();
            if (extension.updateState(call)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 
     * @param info 
     * @param presentation 
     * @param isTemporary 
     * @param call 
     * @param conn 
     */
    public void updateDisplayForPerson(CallerInfo info, int presentation,
                                        boolean isTemporary, Call call, Connection conn) {
        if (null == mSubExtensionList) {
            log("updateDisplayForPerson(), sub extension list is null, just return");
            return;
        }
        log("updateDisplayForPerson(), info is " + info + ", presentation is " + presentation +
                ", isTemporary is " + isTemporary + ", call is " + call + ", connection is " + conn);
        Iterator<VTCallBannerControllerExtension> iterator = mSubExtensionList.iterator();
        while (iterator.hasNext()) {
            iterator.next().updateDisplayForPerson(info, presentation, isTemporary, call, conn);
        }
    }

    private static void log(String msg) {
        Log.d(LOG_TAG, msg);
    }
}
