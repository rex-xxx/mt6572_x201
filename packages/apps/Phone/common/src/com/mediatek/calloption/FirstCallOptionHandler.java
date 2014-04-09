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
import android.provider.Settings;
import android.util.Log;

import com.android.phone.Constants;
import com.mediatek.phone.SIMInfoWrapper;

public class FirstCallOptionHandler extends CallOptionBaseHandler {

    private static final String TAG = "FirstCallOptionHandler";

    @Override
    public void handleRequest(final Request request) {
        // below should be deleted because conflict with international call
        log("handleRequest()");
        //if (-1 != request.getIntent().getIntExtra(Constants.EXTRA_SLOT_ID, -1)) {
        //    request.getResultHandler().onContinueCallProcess(request.getIntent());
        //} else {
        
        //If the call is an voicemail, we put an exact slot here if the voice call
        //setting isn't always ask. This will help to get the correct voice mail number.
        Intent intent = request.getIntent();
        Context ctx = request.getApplicationContext();
        if (Constants.VOICEMAIL_URI.equals(intent.getData().toString())) {
            final long defaultSim = Settings.System.getLong(ctx.getContentResolver(),
                    Settings.System.VOICE_CALL_SIM_SETTING, Settings.System.DEFAULT_SIM_NOT_SET);
            final SIMInfoWrapper simInfoWrapper = com.mediatek.phone.SIMInfoWrapper.getDefault();
            
            if (defaultSim > 0 && simInfoWrapper.getSlotIdBySimId((int)defaultSim) >= 0) {
                intent.putExtra("simId", simInfoWrapper.getSlotIdBySimId((int)defaultSim));
            }
        }
        
        if (null != mSuccessor) {
            mSuccessor.handleRequest(request);
        }
    }

    private static void log(String msg) {
        Log.d(TAG, msg);
    }
}
