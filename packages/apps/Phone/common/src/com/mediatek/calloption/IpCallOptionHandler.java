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

import android.content.Intent;
import android.provider.Telephony.SIMInfo;
import android.text.TextUtils;
import android.util.Log;

import com.android.phone.Constants;
import com.mediatek.phone.GeminiConstants;
import com.mediatek.phone.SIMInfoWrapper;

public abstract class IpCallOptionHandler extends CallOptionBaseHandler {

    private static final String TAG = "IpCallOptionHandler";

    @Override
    public void handleRequest(final Request request) {
        log("handleRequest()");
        int slot = request.getIntent().getIntExtra(Constants.EXTRA_SLOT_ID, -1);
        if (-1 == slot) {
            log("handleRequest(), slot is -1");
            if (null != mSuccessor) {
                mSuccessor.handleRequest(request);
            }
            return;
        }

        // ip dial only support voice call
        if (request.getIntent().getBooleanExtra(Constants.EXTRA_IS_IP_DIAL, false)
                && SIMInfoWrapper.getDefault().getInsertedSimCount() > 0) {
            final String ipPrefix = CallOptionUtils.queryIPPrefix(request.getApplicationContext(), slot,
                                                                  request.isMultipleSim());
            if (TextUtils.isEmpty(ipPrefix)) {
                showToast(request);
                final Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.setClassName(Constants.PHONE_PACKAGE, Constants.IP_PREFIX_SETTING_CLASS_NAME);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                final SIMInfo simInfo = SIMInfoWrapper.getDefault().getSimInfoBySlot(slot);
                if (request.isMultipleSim()) {
                    intent.putExtra(GeminiConstants.SLOT_ID_KEY, slot);
                }
                intent.putExtra(Constants.SETTING_SUB_TITLE_NAME,
                        SIMInfo.getSIMInfoBySlot(request.getActivityContext(), simInfo.mSlot).mDisplayName);
                request.getActivityContext().startActivity(intent);
                request.getResultHandler().onHandlingFinish();
                return;
            } else {
                String number = CallOptionUtils.getInitialNumber(request.getApplicationContext(),
                                                                 request.getIntent());
                if (number.indexOf(ipPrefix) != 0) {
                    request.getIntent().putExtra(Constants.EXTRA_ACTUAL_NUMBER_TO_DIAL, ipPrefix + number);
                }
            }
        }
        if (null != mSuccessor) {
            mSuccessor.handleRequest(request);
        }
    }

    protected abstract void showToast(final Request request);

    /*private String queryIPPrefix(Context context, int slot) {
        final SIMInfo simInfo = SIMInfoWrapper.getDefault().getSimInfoBySlot(slot);
        StringBuilder builder = new StringBuilder();
        builder.append("ipprefix");
        builder.append(simInfo.mSimId);
        final String key = builder.toString();
        final String ipPrefix = Settings.Global.getString(context.getContentResolver(), key);
        log("queryIPPrefix, ipPrefix = " + ipPrefix);
        return ipPrefix;
    }*/

    private static void log(String msg) {
        Log.d(TAG, msg);
    }
}
