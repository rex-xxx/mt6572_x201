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

package com.mediatek.calloption;

import android.content.Context;
import android.content.Intent;

import com.android.internal.telephony.ITelephony;
import com.mediatek.CellConnService.CellConnMgr;

public class Request {

    private Context mActivityContext;
    private Context mApplicationContext;
    private Intent mIntent;
    private CallOptionBaseHandler.ICallOptionResultHandle mResultHandler;
    private CellConnMgr mCellConnMgr;
    private ITelephony mTelephonyInterface;
    private boolean mIsMultipleSim;
    private boolean mIs3GSwitchSupport;
    private CallOptionHandlerFactory mCallOptionHandlerFactory;

    public Request(final Context activityContext, final Context applicationContext, final Intent intent,
                   final CallOptionBaseHandler.ICallOptionResultHandle resultHandler, final CellConnMgr cellConnMgr,
                   final ITelephony telephonyInterface, final boolean isMultipleSim, final boolean is3GSwitchSupport,
                   final CallOptionHandlerFactory callOptionHandlerFactory) {
        mActivityContext = activityContext;
        mApplicationContext = applicationContext;
        mIntent = intent;
        mResultHandler = resultHandler;
        mCellConnMgr = cellConnMgr;
        mTelephonyInterface = telephonyInterface;
        mIsMultipleSim = isMultipleSim;
        mIs3GSwitchSupport = is3GSwitchSupport;
        mCallOptionHandlerFactory = callOptionHandlerFactory;
    }

    public Context getActivityContext() {
        return mActivityContext;
    }

    public Context getApplicationContext() {
        return mApplicationContext;
    }

    public Intent getIntent() {
        return mIntent;
    }

    public CallOptionBaseHandler.ICallOptionResultHandle getResultHandler() {
        return mResultHandler;
    }

    public CellConnMgr getCellConnMgr() {
        return mCellConnMgr;
    }

    public ITelephony getTelephonyInterface() {
        return mTelephonyInterface;
    }

    public boolean isMultipleSim() {
        return mIsMultipleSim;
    }

    public boolean is3GSwitchSupport() {
        return mIs3GSwitchSupport;
    }

    public CallOptionHandlerFactory getCallOptionHandlerFactory() {
        return mCallOptionHandlerFactory;
    }
}
