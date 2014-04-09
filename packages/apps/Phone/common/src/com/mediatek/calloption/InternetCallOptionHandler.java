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

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;

import com.android.phone.Constants;

public abstract class InternetCallOptionHandler extends CallOptionBaseHandler
                                       implements DialogInterface.OnClickListener,
                                                  DialogInterface.OnDismissListener,
                                                  DialogInterface.OnCancelListener {

    private static final String TAG = "InternetCallOptionHandler";

    private Request mRequest;

    @Override
    public void handleRequest(final Request request) {
        log("handleRequest()");
        final Uri uri = request.getIntent().getData();
        final String scheme = uri.getScheme();
        //with sip scheme and tell us not follow the sim management, then it can be out by sip phone
        if (!scheme.equals("sip")
                || request.getIntent().getBooleanExtra(Constants.EXTRA_FOLLOW_SIM_MANAGEMENT, false)) {
            if (null != mSuccessor) {
                mSuccessor.handleRequest(request);
            }
            return;
        }

        /*String number = "";
        try {
            number = PhoneUtils.getInitialNumber(intent);
        } catch (VoiceMailNumberMissingException e) {
            log(e.getMessage());
        }
        final long defaultSim = Settings.Global.getLong(PhoneGlobals.getInstance().getContentResolver(),
                Settings.Global.VOICE_CALL_SIM_SETTING, Settings.Global.DEFAULT_SIM_NOT_SET);
        final ArrayList associateSims = SimAssociateHandler.getInstance().query(number);
        final boolean hasAssociateSims = associateSims != null && associateSims.size() > 0;
        final long originalSim =
            intent.getLongExtra(Constants.EXTRA_ORIGINAL_SIM_ID, Settings.Global.DEFAULT_SIM_NOT_SET);
        if (hasAssociateSims || (originalSim != Settings.Global.DEFAULT_SIM_NOT_SET &&
                                 originalSim != Settings.Global.VOICE_CALL_SIM_SETTING_INTERNET)) {
            if (null != mSuccessor) {
                mSuccessor.handleRequest(context, intent, resultHandler);
            }
            return;
        }*/

        log("data schema is sip and follow sim management extra is false");
        mRequest = request;

        final int enabled = Settings.System.getInt(request.getApplicationContext().getContentResolver(), 
                                                   Settings.System.ENABLE_INTERNET_CALL, 0);
        if (1 == enabled) {
            //don't allowed ip dial for sip call
            log("internet call setting is enabled");
            if (request.getIntent().getBooleanExtra(Constants.EXTRA_IS_IP_DIAL, false)) {
                log("extra ip dial is true, show toast");
                showIPDialToast(request);
            } else {
                // start sip call option handler
                log("doSipCallOptionHandle()");
                doSipCallOptionHandle(request);
            }
            request.getResultHandler().onHandlingFinish();
        } else {
            log("internet call setting not enabled, show sip disable dialog");
            showSipDisableDialog(request, this, this, this);
        }
    }

    // below function is different between Phone version and Contacts version,
    // so implement it in subclass
    protected abstract void doSipCallOptionHandle(final Request request);

    protected abstract void showIPDialToast(final Request request);

    protected void showSipDisableDialog(final Request request, 
                                        DialogInterface.OnClickListener clickListener,
                                        DialogInterface.OnDismissListener dismissListener,
                                        DialogInterface.OnCancelListener cancelListener) {
    }

    public void onClick(DialogInterface dialog, int which) {
        log("onClick(), which = " + which);
        if (DialogInterface.BUTTON_POSITIVE == which) {
            Intent intent = new Intent();
            intent.setClassName(Constants.PHONE_PACKAGE, Constants.SIP_CALL_SETTING_CLASS_NAME);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            mRequest.getActivityContext().startActivity(intent);
        } else if (DialogInterface.BUTTON_NEGATIVE == which) {
            dialog.cancel();
        }
    }

    public void onDismiss(DialogInterface dialog) {
        log("onDismiss()");
    }

    public void onCancel(DialogInterface dialog) {
        log("onCancel()");
        mRequest.getResultHandler().onHandlingFinish();
    }

    private static void log(String msg) {
        Log.d(TAG, msg);
    }
}
