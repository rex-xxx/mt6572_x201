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

package com.mediatek.MediatekDM.ext;

import android.net.Uri;
import android.os.IBinder;
import android.os.ServiceManager;
import android.provider.Telephony.Carriers;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.mediatek.common.dm.DMAgent;
import com.mediatek.telephony.TelephonyManagerEx;

public final class MTKPhone {
    public static final int APN_ALREADY_ACTIVE = PhoneConstants.APN_ALREADY_ACTIVE;
    public static final int APN_TYPE_NOT_AVAILABLE = PhoneConstants.APN_TYPE_NOT_AVAILABLE;
    public static final int APN_REQUEST_FAILED = PhoneConstants.APN_REQUEST_FAILED;
    public static final int APN_REQUEST_STARTED = PhoneConstants.APN_REQUEST_STARTED;

    public static final String FEATURE_ENABLE_DM = Phone.FEATURE_ENABLE_DM;

    public static final Uri CONTENT_URI_DM = Carriers.CONTENT_URI_DM;
    public static final Uri CONTENT_URI_DM_GEMINI = Carriers.GeminiCarriers.CONTENT_URI_DM;

    public static String getSubscriberIdGemini(TelephonyManagerEx telMgr, int simId) {
        return telMgr.getSubscriberId(simId);
    }

    public static String getSimOperatorGemini(TelephonyManagerEx telMgr, int slot) {
        return telMgr.getSimOperator(slot);
    }

    public static String getSimOperatorNameGemini(TelephonyManagerEx telMgr, int slot) {
        return telMgr.getSimOperatorName(slot);
    }

    public static DMAgent getDmAgent() {
        IBinder binder = ServiceManager.getService("DMAgent");
        if (binder == null) {
            Log.e("MTKPhone", "ServiceManager.getService(DMAgent) failed.");
            return null;
        }
        DMAgent agent = DMAgent.Stub.asInterface(binder);
        return agent;
    }
}
