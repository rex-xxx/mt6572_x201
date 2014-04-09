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

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.mediatek.MediatekDM.DmConst.TAG;
import com.mediatek.MediatekDM.conn.DmDatabase;
import com.mediatek.MediatekDM.ext.MTKOptions;
import com.mediatek.MediatekDM.option.Options;

import java.util.ArrayList;

public class OmaCpReceiver extends BroadcastReceiver {

    // private static final String TAG = "OmaCpReceiver";
    public static final String APP_ID_KEY = "appId";
    public static final String DM_ID = "w7";
    private static final String CP_DM_SETTING_ACTION = "com.mediatek.omacp.settings";
    private static final String CP_DM_CAPABILITY_ACTION = "com.mediatek.omacp.capability";
    private static final String CP_DM_APP_SETTING_RESULT_ACTION = "com.mediatek.omacp.settings.result";
    private static final String CP_DM_CAPABILITY_RESULT_ACTION = "com.mediatek.omacp.capability.result";
    private Context mContext;
    private TelephonyManager mTelephonyManager;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG.CP, "Receiver intent: " + intent);
        mContext = context;
        mTelephonyManager = (TelephonyManager) mContext.getSystemService(Service.TELEPHONY_SERVICE);
        String intentAction = intent.getAction();
        Intent DmIntent = new Intent(intent);
        String dmServerAddr = null;
        if (intentAction.equals(CP_DM_CAPABILITY_ACTION)) {
            Log.i(TAG.CP, "Receive cp config dm capability intent");
            handleCpCapabilityMessage(intent);
        } else if (intentAction.equals(CP_DM_SETTING_ACTION)) {
            Log.i(TAG.CP, "Receive cp config dm setting intent");
            if (MTKOptions.MTK_OMACP_SUPPORT == false) {
                Log.i(TAG.CP, "OMA CP in not supported by feature option");
            }

            int simId = intent.getIntExtra("simId", 0);
            Log.i(TAG.CP, "99999999999999999999Get server address array list from intent");
            ArrayList<String> dmAddr = intent.getStringArrayListExtra("ADDR");
            if (dmAddr != null) {
                Log.i(TAG.CP, "Get server address array list from intent");
                dmServerAddr = dmAddr.get(0);
            }
            try {
                if (dmServerAddr == null || dmServerAddr.equals("")) {
                    Log.w(TAG.CP, "Get invalid form cp intent");
                    return;
                }

            } catch (Exception e) {
                Log.e(TAG.CP, "Exception happen when parse OMA CP message");
                e.printStackTrace();
                return;
            }

            // String dmServerAddr = intent.getStringExtra("ADDR");
            Log.i(TAG.CP, "In receiver: server address = " + dmServerAddr + " simId = " + simId);
            boolean result = handleCpConfigMessage(simId, dmServerAddr);
            Intent resultIntent = new Intent();
            resultIntent.setAction(CP_DM_APP_SETTING_RESULT_ACTION);
            resultIntent.putExtra(APP_ID_KEY, DM_ID);
            resultIntent.putExtra("result", result);
            mContext.sendBroadcast(resultIntent);
            Log.i(TAG.CP, "send OMA CP config DM result intent: " + resultIntent);
        } else {
            Log.i(TAG.CP, "Normal intent.");
        }
    }

    private void handleCpCapabilityMessage(Intent intent) {
        Intent cpResultIntent = new Intent();
        cpResultIntent.setAction(CP_DM_CAPABILITY_RESULT_ACTION);
        cpResultIntent.putExtra(APP_ID_KEY, DM_ID);
        cpResultIntent.putExtra("dm", true);
        cpResultIntent.putExtra("dm_provider_id", false);
        cpResultIntent.putExtra("dm_server_name", false);
        cpResultIntent.putExtra("dm_to_proxy", false);
        cpResultIntent.putExtra("dm_to_napid", false);
        cpResultIntent.putExtra("dm_server_address", true);
        cpResultIntent.putExtra("dm_addr_type", false);
        cpResultIntent.putExtra("dm_port_number", false);
        cpResultIntent.putExtra("dm_auth_level", false);
        cpResultIntent.putExtra("dm_auth_type", false);
        cpResultIntent.putExtra("dm_auth_name", false);
        cpResultIntent.putExtra("dm_auth_secret", false);
        cpResultIntent.putExtra("dm_auth_data", false);
        cpResultIntent.putExtra("dm_init", false);
        mContext.sendBroadcast(cpResultIntent);
        return;
    }

    private boolean handleCpConfigMessage(int simId, String serverAddr) {
        // TODO: need to config the dm server address in dm tree
        Log.i(TAG.CP, "Enter config DmServer addr : server addr = " + serverAddr + "sim id is "
                + simId);
        if (serverAddr == null) {
            Log.e(TAG.CP, "server address is null");
            return false;
        }

        if (Options.UseSmsRegister) {
            int sim = DmCommonFunction.getRegisteredSimId(mContext);
            if (sim == -1 || simId != sim) {
                Log.e(TAG.CP,
                        "sim card is not register OR cp sim card is not the register sim card.");
                return false;
            }
        }

        boolean ret = false;
        if (!Options.UseDirectInternet) {
            Log.w(TAG.CP, "---- handling CP config msg ----");
            DmDatabase mDb = new DmDatabase(mContext);
            if (mDb == null) {
                Log.e(TAG.CP, "Init DmDatabase error!");
                return false;
            }
            if (mDb.DmApnReady(simId) == false) {
                Log.e(TAG.CP, "Initialize dm database error and can not insert data to dm table");
                return false;
            }

            ret = mDb.updateDmServer(simId, serverAddr);
        } else {
            Log.w(TAG.CP, "----skipped handling CP config msg----");
        }

        Log.i(TAG.CP, "Update dm tree in database [" + ret + "]");

        return ret;

    }
}
