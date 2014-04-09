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

package com.mediatek.MediatekDM;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.mediatek.MediatekDM.DmConst.TAG;
import com.mediatek.MediatekDM.ext.MTKPhone;
import com.mediatek.MediatekDM.util.DmThreadPool;
import com.mediatek.common.dm.DMAgent;

import java.io.File;
import java.util.concurrent.ExecutorService;

public class DmReceiver extends BroadcastReceiver {

    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null || context == null) {
            Log.w(TAG.Receiver, "Invalid arguments. Exit.");
            return;
        }

        Log.i(TAG.Receiver, "Received intent: " + intent);
        String intentAction = intent.getAction();

        if (intentAction.equalsIgnoreCase(TelephonyIntents.ACTION_SIM_INDICATOR_STATE_CHANGED)) {
            /**
             * ACTION_SIM_INDICATOR_STATE_CHANGED is used to launch
             * RebootChecker when network is ready. This intent is MTK internal.
             * 
             * @fixme There are bugs here, obviously.
             */
            int simState = intent.getIntExtra(TelephonyIntents.INTENT_KEY_ICC_STATE,
                    PhoneConstants.SIM_INDICATOR_UNKNOWN);
            Log.d(TAG.Receiver, "Phone state is " + simState);
            if (simState == PhoneConstants.SIM_INDICATOR_NORMAL || simState == PhoneConstants.SIM_INDICATOR_ROAMING) {
                // We ignore SIM_INDICATOR_CONNECTED &
                // SIM_INDICATOR_ROAMINGCONNECTED here.
                Log.i(TAG.Receiver, "Phone state is either normal or roaming. Proceed.");
            } else {
                Log.d(TAG.Receiver, "Invalid phone state. Abort.");
                return;
            }

            /**
             * @fixme This is a mistake! Multiple checker may be launched and
             *        there is no concurrency control!
             */
            Log.d(TAG.Receiver, "Launch reboot checker.");
            Intent checkerIntent = new Intent();
            checkerIntent.setAction(DmConst.IntentAction.ACTION_REBOOT_CHECK);
            RebootChecker checker = new RebootChecker(checkerIntent, context);

            if (sExecutorService == null) {
                sExecutorService = DmThreadPool.getInstance();
            }

            if (sExecutorService != null && checker != null) {
                sExecutorService.execute(checker);
            }
            // Do NOT launch service.
            return;
        } else if (intentAction.equals(DmConst.IntentAction.DM_SWUPDATE)) {
            /**
             * User clicked update in system settings preference.
             */
            Log.i(TAG.Receiver, "Launch system update UI.");
            Intent activityIntent = new Intent(context, DmEntry.class);
            activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(activityIntent);
        } else {
            Log.w(TAG.Receiver, "Normal intent. Forward it to service.");
        }

        Log.i(TAG.Receiver, "Start service");
        Intent serviceIntent = new Intent(intent);
        if (serviceIntent == null || serviceIntent.getAction() == null) {
            return;
        }
        serviceIntent.setClass(context, DmService.class);
        serviceIntent.setAction(intentAction);
        context.startService(serviceIntent);
    }

    public boolean isUpdateReboot() {
        Log.i(TAG.Receiver, "Check the existence of update flag file.");
        boolean ret = false;
        try {
            File updateFile = new File(DmConst.Path.FotaExecFlagFile);
            if (updateFile.exists()) {
                Log.d(TAG.Receiver, "FOTA flag file exists. Delete it.");
                updateFile.delete();
                ret = true;
            }
        } catch (Exception e) {
            Log.e(TAG.Receiver, e.toString());
            e.printStackTrace();
        }
        return ret;
    }

    public class RebootChecker implements Runnable {
        private Intent mDmIntent = null;
        private Context mContext = null;

        public RebootChecker(Intent DmIntent, Context context) {
            mDmIntent = DmIntent;
            mContext = context;
        }

        public void run() {
            DMAgent dmAgent = MTKPhone.getDmAgent();

            boolean shouldStartService = false;
            if (mDmIntent == null || mDmIntent.getAction() == null) {
                Log.w(TAG.Receiver, "mDmIntent is null. Exit.");
                return;
            }

            mDmIntent.setAction(mDmIntent.getAction());
            mDmIntent.setClass(mContext, DmService.class);
            Bundle bundle = new Bundle();

            if (isUpdateReboot()) {
                Log.i(TAG.Receiver, "This is update reboot");
                bundle.putString("update", "true");
                shouldStartService = true;
            }

            if (niaFileExist()) {
                Log.i(TAG.Receiver, "CheckReboot Start dm service really, this is nia exist");
                bundle.putString("nia", "true");
                shouldStartService = true;
            }

            if (wipeFlagExist(dmAgent)) {
                Log.i(TAG.Receiver, "CheckReboot Start dm service really, this is wipe rebbot");
                bundle.putString("wipe", "true");
                shouldStartService = true;

            }
            if (shouldStartService == true) {
                Log.d(TAG.Receiver, "RebootChecker starts service");
                mDmIntent.putExtras(bundle);
                mContext.startService(mDmIntent);
            } else {
                Log.d(TAG.Receiver, "--- no need to start service.");
            }
        }

    }

    public boolean niaFileExist() {
        Log.i(TAG.Receiver, "niaFileExist");
        boolean ret = false;
        try {
            File folder = new File(DmConst.Path.PathNia);
            if (!folder.exists()) {
                Log.w(TAG.Receiver, "CheckNia the nia dir does not exist");
                return ret;
            }

            String[] fileExist = folder.list();
            if (fileExist == null || fileExist.length <= 0) {
                Log.w(TAG.Receiver, "CheckNia there is no unproceed message");
                return ret;
            }
            ret = true;
        } catch (Exception e) {
            Log.e(TAG.Receiver, e.getMessage());
        }

        return ret;
    }

    public boolean wipeFlagExist(DMAgent agent) {
        Log.i(TAG.Receiver, "wipeFlagExist");
        boolean ret = false;
        try {
            if (agent != null) {
                ret = agent.isWipeSet();
            } else {
                Log.w(TAG.Receiver, "mDmAgent is null");
            }
        } catch (Exception e) {
            Log.e(TAG.Receiver, e.getMessage());
        }
        return ret;
    }

    private static ExecutorService sExecutorService = null;
}
