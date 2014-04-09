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

package com.mediatek.contacts.calloption;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.provider.Telephony.SIMInfo;
import android.util.Log;

import com.android.contacts.R;
import com.mediatek.calloption.Request;
import com.mediatek.calloption.SimPickerDialog;
import com.mediatek.calloption.SimSelectionCallOptionHandler;

public class ContactsSimSelectionCallOptionHandler extends SimSelectionCallOptionHandler {

    private static final String TAG = "PhoneSimSelectionCallOptionHandler";

    protected void showReasonAskDialog(final Request request, final long suggestedSim, final boolean addInternet,
                                       DialogInterface.OnClickListener clickListener,
                                       DialogInterface.OnDismissListener dismissListener,
                                       DialogInterface.OnCancelListener cancelListener) {
        mDialog = SimPickerDialog.create(request.getActivityContext(),
                request.getActivityContext().getResources().getString(R.string.sim_manage_call_via), clickListener,
                new ContactsSimPickerAdapter(request.getActivityContext(), suggestedSim, request.isMultipleSim()),
                addInternet, false);
        mDialog.setOnDismissListener(dismissListener);
        mDialog.setOnCancelListener(cancelListener);
        mDialog.show();
    }

    protected void showAssociateMissingDialog(final Request request, SIMInfo associateSimInfo, String number,
                                              AssociateSimMissingArgs associateSimMissingArgs,
                                              DialogInterface.OnClickListener clickListener,
                                              DialogInterface.OnDismissListener dismissListener,
                                              DialogInterface.OnCancelListener cancelListener) {
        Resources resources = request.getActivityContext().getResources();

        String associateSimName = (null != associateSimInfo) ? associateSimInfo.mDisplayName : "";
        String viaSimName = (null != associateSimMissingArgs.viaSimInfo) ?
                    associateSimMissingArgs.viaSimInfo.mDisplayName
                    : request.getActivityContext().getResources().getString(R.string.label_sip_address);
        String message = request.getActivityContext().getResources().getString(
                    R.string.associate_sim_missing_message, associateSimName, viaSimName);

        AlertDialog.Builder builder = new AlertDialog.Builder(request.getActivityContext());
        builder.setTitle(number).setMessage(message).setPositiveButton(android.R.string.yes, clickListener);
        if (associateSimMissingArgs.type == AssociateSimMissingArgs.ASSOCIATE_SIM_MISSING_YES_NO) {
            builder.setNegativeButton(resources.getString(android.R.string.cancel), clickListener);
        } else if (associateSimMissingArgs.type == AssociateSimMissingArgs.ASSOCIATE_SIM_MISSING_YES_OTHER) {
            builder.setNegativeButton(resources.getString(R.string.associate_sim_missing_other), clickListener);
        }

        mDialog = builder.create();
        mDialog.setOnDismissListener(dismissListener);
        mDialog.setOnCancelListener(cancelListener);
        mDialog.show();
    }

    private static void log(String msg) {
        Log.d(TAG, msg);
    }
}
