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
import android.content.Context;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.mediatek.MediatekDM.DmConst.TAG;
import com.mediatek.MediatekDM.ext.MTKOptions;
import com.mediatek.MediatekDM.ext.MTKPhone;
import com.mediatek.MediatekDM.xml.DmXMLParser;
import com.mediatek.common.dm.DMAgent;
import com.mediatek.telephony.TelephonyManagerEx;

import java.io.File;

/**
 * Common utilities.
 * 
 * @author mtk81226
 */
public final class DmCommonFunction {
    public static final int GEMINI_SIM_1 = 0;
    public static final int GEMINI_SIM_2 = 1;
    public static int[] GEMSIM = {
            GEMINI_SIM_1, GEMINI_SIM_2
    };

    public static String getOperatorName() {
        String opName = null;
        try {
            File configFileInSystem = new File(DmConst.Path.DmConfigFileInSystem);
            if (configFileInSystem.exists()) {
                DmXMLParser xmlParser = new DmXMLParser(DmConst.Path.DmConfigFileInSystem);
                opName = xmlParser.getValByTagName("op");
                Log.i(TAG.Common, "operator = " + opName);
            }
        } catch (Exception e) {
            Log.e(TAG.Common, "failed reading config.xml", e);
        }
        return opName;

    }

    public static int getRegisteredSimId(Context context) {
        String registerIMSI = null;
        TelephonyManagerEx telMgr = TelephonyManagerEx.getDefault();
        if (telMgr == null) {
            Log.e(TAG.Common, "Get TelephonyManager failed.");
            return -1;
        }

        try {
            DMAgent agent = MTKPhone.getDmAgent();
            if (agent == null) {
                Log.e(TAG.Common, "get dm_agent_binder failed.");
                return -1;
            }
            registerIMSI = new String(agent.readIMSI());
        } catch (Exception e) {
            Log.e(TAG.Common, "get registered IMSI failed", e);
        }

        if (registerIMSI == null) {
            Log.e(TAG.Common, "get registered IMSI failed");
            return -1;
        }

        Log.i(TAG.Common, "[FeatureOption]gemini=" + MTKOptions.MTK_GEMINI_SUPPORT);
        Log.i(TAG.Common, "registered imsi=" + registerIMSI);

        if (MTKOptions.MTK_GEMINI_SUPPORT) {
            for (int i = 0; i < 2; i++) {
                String[] IMSI = new String[2];
                IMSI[i] = MTKPhone.getSubscriberIdGemini(telMgr, GEMSIM[i]);
                if (IMSI[i] != null && IMSI[i].equals(registerIMSI)) {
                    Log.i(TAG.Common, "register SIM card is SIM" + i);
                    return i;
                }
            }
        } else {
            String SigalIMSI = null;
            SigalIMSI = telMgr.getSubscriberId(0);
            Log.i(TAG.Common, "simId = " + SigalIMSI);
            if (SigalIMSI == null) {
                Log.e(TAG.Common, "get sim IMSI error!");
                return -1;
            }
            if (SigalIMSI.equals(registerIMSI)) {
                Log.i(TAG.Common, "It is not gemini and the sim card has registered already");
                return 0;
            }
        }
        return -1;
    }

}
