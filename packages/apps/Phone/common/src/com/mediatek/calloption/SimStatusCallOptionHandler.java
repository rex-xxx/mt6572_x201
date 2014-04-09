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

import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.phone.Constants;
import com.mediatek.CellConnService.CellConnMgr;
import com.mediatek.phone.GeminiConstants;
import com.mediatek.telephony.TelephonyManagerEx;

public abstract class SimStatusCallOptionHandler extends CallOptionBaseHandler {

    private static final String TAG = "SimStatusCallOptionHandler";

    // used for runnable
    private Request mRequest;

    private static final int MESSAGE_CHECK_SIM_STATUS = 100;

    private final Handler mHandler = new Handler() {

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_CHECK_SIM_STATUS:
                    final int result = mRequest.getCellConnMgr().handleCellConn(msg.arg1,
                            CellConnMgr.REQUEST_TYPE_ROAMING, mRunnable);
                    log("result = " + result);
                    if (result == mRequest.getCellConnMgr().RESULT_WAIT) {
                        showProgressIndication(mRequest);
                    }
                    break;
                default:
                    break;
            }
        }
    };

    private Runnable mRunnable = new Runnable() {
        public void run() {
            final int result = mRequest.getCellConnMgr().getResult();
            final int slot = mRequest.getCellConnMgr().getPreferSlot();
            log("run, result = " + result + " slot = " + slot);

            dismissProgressIndication();
            if (result != com.mediatek.CellConnService.CellConnMgr.RESULT_STATE_NORMAL) {
                mRequest.getResultHandler().onHandlingFinish();
            } else {
                int oldSolt = mRequest.getIntent().getIntExtra(Constants.EXTRA_SLOT_ID, -1);
                log("afterCheckSIMStatus, oldSolt = " + oldSolt);
                if (oldSolt != -1 && slot != oldSolt) {
                    mRequest.getIntent().putExtra(Constants.EXTRA_SLOT_ID, slot);
                }
                //mRequest.getResultHandler().onContinueCallProcess(mRequest.getIntent());
                if (null != mSuccessor) {
                    mSuccessor.handleRequest(mRequest);
                }
            }
        }
    };

    @Override
    public void handleRequest(final Request request) {
        int slot = request.getIntent().getIntExtra(Constants.EXTRA_SLOT_ID, -1);
        log("handleRequest(), slot = " + slot);
        if (-1 == slot) {
            if (null != mSuccessor) {
                mSuccessor.handleRequest(request);
            }
        } else {
            mRequest = request;
            if (needToCheckSIMStatus(slot)) {
                /*final int result = request.getCellConnMgr().handleCellConn(slot,
                            CellConnMgr.REQUEST_TYPE_ROAMING, mRunnable);
                log("result = " + result);
                if (result == request.getCellConnMgr().RESULT_WAIT) {
                    showProgressIndication(mRequest);
                }*/
                // Should not call function of CellConnMgr, or else OutgoingCallBroadcaster::onResume() may
                // called after CellConnMgr show dialog, which causes CellConnMgr dialog dismiss automatically
                mHandler.sendMessage(mHandler.obtainMessage(MESSAGE_CHECK_SIM_STATUS, slot, 0));
            } else {
                if (null != mSuccessor) {
                    mSuccessor.handleRequest(request);
                }
            }
        }
    }

    private boolean needToCheckSIMStatus(int slot) {
        if (slot < 0 || !CallOptionUtils.isSimInsert(mRequest, slot)) {
            log("the sim not insert, bail out!");
            return false;
        }
        if (!CallOptionUtils.isRadioOn(mRequest, slot)) {
            return true;
        }
        // !!! actually below needs not use TelephonyManager.getDefault().getSimState() because
        // TelephonyManagerEx.getDefault().getSimState(slot) can support single sim card,
        // Change it in future
        int simState = mRequest.isMultipleSim() ? TelephonyManagerEx.getDefault().getSimState(slot) :
                                                  TelephonyManager.getDefault().getSimState();
        return simState != TelephonyManager.SIM_STATE_READY || roamingRequest(slot);
    }

    /**
     * Show an onscreen "progress indication" with the specified title and message.
     */
    protected abstract void showProgressIndication(final Request request);

    /**
     * Dismiss the onscreen "progress indication" (if present).
     */
    protected abstract void dismissProgressIndication();

    private boolean roamingRequest(int slot) {
        log("roamingRequest slot = " + slot);
        boolean bRoaming = false;
        if (mRequest.isMultipleSim()) {
            bRoaming = TelephonyManagerEx.getDefault().isNetworkRoaming(slot);
        } else {
            bRoaming = TelephonyManager.getDefault().isNetworkRoaming();
        }

        log("roamingRequest slot = " + slot + ", isRoam = " + bRoaming);
        if (!bRoaming) {
            return false;
        }

        if (0 == Settings.System.getInt(mRequest.getApplicationContext().getContentResolver(),
                                        Settings.System.ROAMING_REMINDER_MODE_SETTING, -1)
                && isRoamingNeeded(slot)) {
            log("roamingRequest reminder once and need to indicate");
            return true;
        }

        if (1 == Settings.System.getInt(mRequest.getApplicationContext().getContentResolver(),
                                        Settings.System.ROAMING_REMINDER_MODE_SETTING, -1)) {
            log("roamingRequest reminder always");
            return true;
        }

        log("roamingRequest result = false");
        return false;
    }

    private boolean isRoamingNeeded(int slot) {
        boolean result = false;
        for (int i = 0; i < GeminiConstants.SLOTS.length; i++) {
            if (slot == GeminiConstants.SLOTS[i]) {
                result = SystemProperties.getBoolean(
                        GeminiConstants.GSM_ROAMING_INDICATOR_NEEDED_GEMINI[i], false);
            }
        }
        log("isRoamingNeeded slot=" + slot + " result=" + result);
        return result;
    }

    private void log(String msg) {
        Log.d(TAG, msg);
    }
}
