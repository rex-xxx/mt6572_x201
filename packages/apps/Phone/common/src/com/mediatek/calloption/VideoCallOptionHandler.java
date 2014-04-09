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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.widget.ListAdapter;

import com.android.internal.telephony.PhoneConstants;
import com.android.phone.Constants;
import com.mediatek.phone.GeminiConstants;

import java.util.ArrayList;
import java.util.List;

public abstract class VideoCallOptionHandler extends CallOptionBaseHandler {

    private static final String TAG = "VideoCallOptionHandler";

    private Request mRequest;

    @Override
    public void handleRequest(final Request request) {
        log("handleRequest()");
        mRequest = request;

        if (!request.getIntent().getBooleanExtra(Constants.EXTRA_IS_VIDEO_CALL, false)) {
            if (null != mSuccessor) {
                mSuccessor.handleRequest(request);
            }
            return;
        }

        /// M:Gemini+ @{
        // Dual talk vidoe call case
        List<Integer> slots3G = CallOptionUtils.get3GCapabilitySlots();
        if (mRequest.isMultipleSim() && slots3G.size() > 0) {
            int slotId = -1;
            ArrayList<Integer> inserted3GSims = new ArrayList<Integer>();
            for (int slot : slots3G) {
                if (CallOptionUtils.isSimInsert(request, slot)) {
                    inserted3GSims.add(slot);
                }
            }
            if (inserted3GSims.size() > 1) {
                show3GSIMSelectDialog(request, mThreeGSIMSelectClickListener,
                        mThreeGSIMSelectDismissListener, mThreeGSIMSelectCancelListener);
                return;
            } else if (inserted3GSims.isEmpty()) {
                request.getIntent().putExtra(Constants.EXTRA_SLOT_ID, slotId);
                SimStatusCallOptionHandler simStatusHandler = request.getCallOptionHandlerFactory()
                        .getSimStatusCallOptionHandler();
                simStatusHandler.setSuccessor(request.getCallOptionHandlerFactory()
                        .getFinalCallOptionHandler());
                simStatusHandler.handleRequest(request);
                return;
            }
        }
        /// @}

        // single talk video call case
        final int slot = request.is3GSwitchSupport() ? CallOptionUtils.get3GCapabilitySIM(request) : 0;
        final int defaultSlot3G = slots3G.size() > 0 ? slots3G.get(0) : -1;
        if (-1 == slot) {
            /// M: ALPS00216137 @ {
            // Check the status of the SIM in 3G slot. if the card is inserted
            // but not ready, it may be turned off. Then we need to check the
            // SIM status first.
            if (CallOptionUtils.isSimInsert(request, defaultSlot3G)
                          && !CallOptionUtils.isSimReady(request, defaultSlot3G)) {
                requestCheckSimStatus(request, defaultSlot3G);
                return;
            }
            /// @}
            boolean isInsertSim = false;
            for (int gs : GeminiConstants.SLOTS) {
                if (CallOptionUtils.isSimInsert(request, gs)) {
                    isInsertSim = true;
                    break;
                }
            }
            showOpen3GServiceDialog(request, isInsertSim, mThreeGServiceClickListener,
                    mThreeGServiceDismissListener, mThreeGServiceCancelListener);
        } else {
            requestCheckSimStatus(request, slot);
        }
    }

    /**
     * To check SIM status before dial VT.
     * @param request
     * @param slot
     */
    private void requestCheckSimStatus(final Request request, int slot) {
        // The SIM in 3G slot is not ready. Maybe the SIM card is turned
        // off, check the SIM status first.
        request.getIntent().putExtra(Constants.EXTRA_SLOT_ID, slot);
        SimStatusCallOptionHandler simStatusHandler 
                = request.getCallOptionHandlerFactory().getSimStatusCallOptionHandler();
        simStatusHandler.setSuccessor(request.getCallOptionHandlerFactory().getFinalCallOptionHandler());
        simStatusHandler.handleRequest(request);
    }

    private DialogInterface.OnClickListener mThreeGSIMSelectClickListener
                                            = new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            final AlertDialog alert = (AlertDialog) dialog;
            final ListAdapter listAdapter = alert.getListView().getAdapter();
            final int slot = ((Integer)listAdapter.getItem(which)).intValue();

            // Below is for daily use issue,
            // need to be deleted and does not add internet call item for ip dial
            log("3GSIMSelectClick, onClick() is called, slot = " + slot);
            dialog.dismiss();
            mRequest.getIntent().putExtra(Constants.EXTRA_SLOT_ID, slot);
            SimStatusCallOptionHandler simStatusHandler
                    = mRequest.getCallOptionHandlerFactory().getSimStatusCallOptionHandler();
            simStatusHandler.setSuccessor(mRequest.getCallOptionHandlerFactory().getFinalCallOptionHandler());
            simStatusHandler.handleRequest(mRequest);
        }
    };

    private DialogInterface.OnDismissListener mThreeGSIMSelectDismissListener
                                            = new DialogInterface.OnDismissListener() {
        public void onDismiss(DialogInterface dialog) {
            log("3GSIMSelectDismiss, onDismiss() is called");
        }
    };

    private DialogInterface.OnCancelListener mThreeGSIMSelectCancelListener
                                            = new DialogInterface.OnCancelListener() {
        @Override
        public void onCancel(DialogInterface dialog) {
            log("3GSIMSelectCancel, onCancel() is called");
            mRequest.getResultHandler().onHandlingFinish();
        }
    };

    private DialogInterface.OnClickListener mThreeGServiceClickListener
                                            = new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            log("onClick(), which = " + which);
            if (DialogInterface.BUTTON_POSITIVE == which) {
                Intent intent = new Intent();
                intent.setClassName(Constants.PHONE_PACKAGE, Constants.MODEM_3G_CAPABILITY_SWITCH_SETTING_CLASS_NAME);
                mRequest.getActivityContext().startActivity(intent);
            } else if (DialogInterface.BUTTON_NEGATIVE == which) {
                dialog.cancel();
            }
        }
    };

    private DialogInterface.OnDismissListener mThreeGServiceDismissListener
                                            = new DialogInterface.OnDismissListener() {
        public void onDismiss(DialogInterface dialog) {
            log("onDismiss()");
        }
    };

    private DialogInterface.OnCancelListener mThreeGServiceCancelListener
                                            = new DialogInterface.OnCancelListener() {
        @Override
        public void onCancel(DialogInterface dialog) {
            log("onCancel()");
            mRequest.getResultHandler().onHandlingFinish();
        }
    };

    protected abstract void showOpen3GServiceDialog(final Request request, final boolean isInsertSim,
                                                    DialogInterface.OnClickListener clickListener,
                                                    DialogInterface.OnDismissListener dismissListener,
                                                    DialogInterface.OnCancelListener cancelListener);

    protected abstract void show3GSIMSelectDialog(final Request request,
                                       DialogInterface.OnClickListener clickListener,
                                       DialogInterface.OnDismissListener dismissListener,
                                       DialogInterface.OnCancelListener cancelListener);

    private static void log(String msg) {
        Log.d(TAG, msg);
    }
}
