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
import android.provider.Telephony.SIMInfo;
import android.text.TextUtils;
import android.util.Log;

import com.android.phone.Constants;
import com.mediatek.phone.GeminiConstants;
import com.mediatek.phone.SIMInfoWrapper;
import com.mediatek.telephony.TelephonyManagerEx;

public abstract class VoiceMailCallOptionHandler extends CallOptionBaseHandler
                                        implements DialogInterface.OnClickListener,
                                                   DialogInterface.OnDismissListener,
                                                   DialogInterface.OnCancelListener {

    private static final String TAG = "VoiceMailCallOptionHandler";

    private Request mRequest;

    @Override
    public void handleRequest(final Request request) {
        log("handleRequest()");
        mRequest = request;
        if (!Constants.VOICEMAIL_URI.equals(request.getIntent().getData().toString())) {
            if (null != mSuccessor) {
                mSuccessor.handleRequest(request);
            }
            return;
        }
        // for voice mail, dial type is always VOICE
        //intent.putExtra(Constants.EXTRA_IS_VIDEO_CALL, false);
        // !!!!!!!! Set sip call as voice call below
        /*final Uri uri = intent.getData();
        if (uri.getScheme().equals("sip")) {
            
        }*/
        mRequest = request;
        final long defaultSim = Settings.System.getLong(request.getApplicationContext().getContentResolver(),
                Settings.System.VOICE_CALL_SIM_SETTING, Settings.System.DEFAULT_SIM_NOT_SET);
        if (defaultSim > 0) {
            SIMInfo info = SIMInfoWrapper.getDefault().getSimInfoById((int)defaultSim);
            if (info != null) {
               request.getIntent().putExtra(GeminiConstants.SLOT_ID_KEY, info.mSlot);
            }
        }

        final int slot = request.getIntent().getIntExtra(Constants.EXTRA_SLOT_ID, -1);
        if (-1 == slot) {
            if (null != mSuccessor) {
                mSuccessor.handleRequest(request);
            }
            return;
        }

        if (TextUtils.isEmpty(TelephonyManagerEx.getDefault().getVoiceMailNumber(slot))) {
            showMissedingVoiceMailDialog(request, this, this, this);
        } else {
            // put the voicemail number to the intent
            final String voicemailNumber = TelephonyManagerEx.getDefault().getVoiceMailNumber(slot);
            request.getIntent().setData(Uri.fromParts("tel", voicemailNumber, null));
            request.getIntent().putExtra(Constants.EXTRA_ACTUAL_NUMBER_TO_DIAL, voicemailNumber);
            if (null != mSuccessor) {
                mSuccessor.handleRequest(request);
            }
        }
    }

    protected abstract void showMissedingVoiceMailDialog(final Request request,
                                                DialogInterface.OnClickListener clickListener,
                                                DialogInterface.OnDismissListener dismissListener,
                                                DialogInterface.OnCancelListener cancelListener);

    public void onClick(DialogInterface dialog, int which) {
        log("onClick(), which = " + which);
        if (DialogInterface.BUTTON_POSITIVE == which) {
            Intent intentCallSetting = new Intent(Intent.ACTION_MAIN);
            intentCallSetting.setClassName(Constants.PHONE_PACKAGE, Constants.VOICE_MAIL_SETTINGS_CLASS_NAME);
            intentCallSetting.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intentCallSetting.putExtra(GeminiConstants.SLOT_ID_KEY, 
                                       mRequest.getIntent().getIntExtra(Constants.EXTRA_SLOT_ID, -1));
            mRequest.getActivityContext().startActivity(intentCallSetting);
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
