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

package com.mediatek.phone.calloption;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.android.phone.OutgoingCallBroadcaster;
import com.android.phone.R;
import com.android.phone.SipCallOptionHandler;
import com.mediatek.calloption.InternetCallOptionHandler;
import com.mediatek.calloption.Request;

public class PhoneInternetCallOptionHandler extends InternetCallOptionHandler {

    private static final String TAG = "PhoneInternetCallOptionHandler";

    protected void doSipCallOptionHandle(final Request request) {
        Intent selectPhoneIntent = newSipCallOptionHandlerIntent(request.getApplicationContext(),
                                                                 request.getIntent());
        log("startSipCallOptionHandler(): " + "calling startActivity: " + selectPhoneIntent);
        request.getActivityContext().startActivity(selectPhoneIntent);
    }

    private Intent newSipCallOptionHandlerIntent(Context context, Intent original) {
        Intent selectPhoneIntent = new Intent(OutgoingCallBroadcaster.ACTION_SIP_SELECT_PHONE, original.getData());
        selectPhoneIntent.setClass(context, SipCallOptionHandler.class);
        selectPhoneIntent.putExtra(OutgoingCallBroadcaster.EXTRA_NEW_CALL_INTENT, original);
        selectPhoneIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return selectPhoneIntent;
    }

    protected void showIPDialToast(final Request request) {
        Toast.makeText(request.getApplicationContext(), 
                R.string.ip_dial_error_toast_for_sip_call_selected, Toast.LENGTH_SHORT).show();
    }

    protected void showSipDisableDialog(final Request request,
                                        DialogInterface.OnClickListener clickListener,
                                        DialogInterface.OnDismissListener dismissListener,
                                        DialogInterface.OnCancelListener cancelListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(request.getActivityContext());
        builder.setTitle(R.string.reminder)
               .setMessage(R.string.enable_sip_dialog_message)
               .setNegativeButton(android.R.string.no, clickListener)
               .setPositiveButton(android.R.string.yes, clickListener);
        mDialog = builder.create();
        mDialog.setOnDismissListener(dismissListener);
        mDialog.setOnCancelListener(cancelListener);
        mDialog.show();
    }

    private static void log(String msg) {
        Log.d(TAG, msg);
    }
}
