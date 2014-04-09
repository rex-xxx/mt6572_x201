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

package com.mediatek.MediatekDM.lawmo;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.mediatek.MediatekDM.DmConst;
import com.mediatek.MediatekDM.DmConst.TAG;
import com.mediatek.MediatekDM.DmService;
import com.mediatek.MediatekDM.ext.MTKPhone;
import com.mediatek.MediatekDM.mdm.MdmTree;
import com.mediatek.MediatekDM.mdm.lawmo.LawmoHandler;
import com.mediatek.MediatekDM.mdm.lawmo.LawmoOperationResult;
import com.mediatek.MediatekDM.mdm.lawmo.LawmoResultCode;
import com.mediatek.MediatekDM.xml.DmXMLParser;
import com.mediatek.common.dm.DMAgent;

import java.io.File;

public class DmLawmoHandler implements LawmoHandler {

    public DmLawmoHandler(Context context) {
        mContext = context;
        mDmAgent = MTKPhone.getDmAgent();
        mDmTree = new MdmTree();
    }

    public LawmoOperationResult executeFactoryReset() {
        // TODO: Execute factory reset
        Log.i(TAG.Lawmo, "executeFactoryReset Execute factory reset");
        try {
            DmService.fakeLawmoAction = 8;
            Log.i(TAG.Lawmo, "executeFactoryReset fakeLawmoAction has been set 8");
            mDmAgent.setWipeFlag();
            Log.i(TAG.Lawmo, "executeFactoryReset FactoryReset has been set");
            File wipeFile = new File(DmConst.Path.PathWipe);
            wipeFile.createNewFile();
            Log.d(TAG.Lawmo, "create new file data/data/com.meidatek.dm/wipe");
        } catch (Exception e) {
            Log.e(TAG.Lawmo, e.getMessage());
            return new LawmoOperationResult(new LawmoResultCode(LawmoResultCode.WIPE_DATA_FAILED));
        }

        return new LawmoOperationResult();
    }

    public LawmoOperationResult executeFullyLock() {
        // TODO Execute fully lock
        Log.i(TAG.Lawmo, "Execute fully lock");
        return new LawmoOperationResult(
                new LawmoResultCode(DmConst.LawmoResult.OPERATION_SUCCESSSFUL));
    }

    public LawmoOperationResult executePartiallyLock() {
        Log.i(TAG.Lawmo, "Execute partially lock");
        String lawmoUri = "./LAWMO/State";

        try {
            if (mDmAgent == null) {
                Log.w(TAG.Lawmo, "executePartiallyLock mDmAgent is null");
                return new LawmoOperationResult(
                        new LawmoResultCode(LawmoResultCode.PARTIALLY_LOCK_FAILED));
            }
            mDmAgent.setLockFlag("partially".getBytes());
            Log.i(TAG.Lawmo, "executePartiallyLock partially flag has been set");
            mContext.sendBroadcast(new Intent("com.mediatek.MediatekDM.LAWMO_LOCK"));
            Log.i(TAG.Lawmo,
                    "executePartiallyLock Intent : com.mediatek.MediatekDM.LAWMO_LOCK broadcasted.");

            Log.i(TAG.Lawmo, "isRestartAndroid = " + isRestartAndroid());
            if (isRestartAndroid()) {
                Thread.sleep(500);
                mDmAgent.restartAndroid();
                Log.i(TAG.Lawmo, "executePartiallyLock restart android");
            }
            // return new LawmoOperationResult(
            // new LawmoResultCode(LawmoResultCode.OPERATION_SUCCESSSFUL));
            Log.i(TAG.Lawmo, "Lock 200");

            mDmTree.replaceIntValue(lawmoUri, 20);
            mDmTree.writeToPersistentStorage();
            Log.i(TAG.Lawmo,
                    "After write status, the lawmo staus is " + mDmTree.getIntValue(lawmoUri));

            return new LawmoOperationResult(new LawmoResultCode(200));
        } catch (Exception e) {
            Log.e(TAG.Lawmo, e.getMessage());
            return new LawmoOperationResult(
                    new LawmoResultCode(LawmoResultCode.PARTIALLY_LOCK_FAILED));
        }
    }

    public LawmoOperationResult executeUnLock() {
        Log.i(TAG.Lawmo, "Execute unlock command");
        String lawmoUri = "./LAWMO/State";

        try {
            if (mDmAgent == null) {
                Log.w(TAG.Lawmo, "executeUnLock mDmAgent is null");
                return new LawmoOperationResult(new LawmoResultCode(LawmoResultCode.UNLOCK_FAILED));
            }
            mDmAgent.clearLockFlag();
            Log.i(TAG.Lawmo, "executeUnLock flag has been cleared");
            mContext.sendBroadcast(new Intent("com.mediatek.MediatekDM.LAWMO_UNLOCK"));
            Log.i(TAG.Lawmo,
                    "executeUnLock Intent : com.mediatek.MediatekDM.LAWMO_UNLOCK broadcasted.");

            if (isRestartAndroid()) {
                Thread.sleep(500);
                mDmAgent.restartAndroid();
                Log.i(TAG.Lawmo, "executeUnLock restart android");
            }
            // return new LawmoOperationResult(
            // new LawmoResultCode(LawmoResultCode.OPERATION_SUCCESSSFUL));
            Log.i(TAG.Lawmo, "UnLock 200");
            mDmTree.replaceIntValue(lawmoUri, 30);
            mDmTree.writeToPersistentStorage();
            Log.i(TAG.Lawmo,
                    "After write status, the lawmo staus is " + mDmTree.getIntValue(lawmoUri));

            return new LawmoOperationResult(new LawmoResultCode(200));

        } catch (Exception e) {
            Log.e(TAG.Lawmo, e.getMessage());
            return new LawmoOperationResult(new LawmoResultCode(LawmoResultCode.UNLOCK_FAILED));
        }
    }

    public LawmoOperationResult executeWipe(String[] dataToWipe) {
        // TODO Execute wipe command
        Log.i(TAG.Lawmo, "executeWipe Execute wipe command");

        if (dataToWipe.length == 0) {
            return new LawmoOperationResult(
                    new LawmoResultCode(LawmoResultCode.OPERATION_SUCCESSSFUL));
        }
        mContext.sendBroadcast(new Intent("com.mediatek.MediatekDM.LAWMO_WIPE"));
        return new LawmoOperationResult(new LawmoResultCode(LawmoResultCode.OPERATION_SUCCESSSFUL));
    }

    public boolean isRestartAndroid() {
        Log.i(TAG.Lawmo, "if restart android when lock and unlock");
        boolean ret = false;
        try {
            File configFileInSystem = new File(DmConst.Path.DmConfigFileInSystem);
            if (configFileInSystem != null && configFileInSystem.exists()) {
                DmXMLParser xmlParser = new DmXMLParser(DmConst.Path.DmConfigFileInSystem);
                if (xmlParser != null) {
                    String ifRestartAndroid = xmlParser.getValByTagName("LockRestart");
                    Log.i(TAG.Lawmo, "the restart flag is " + ifRestartAndroid);
                    if (ifRestartAndroid != null && ifRestartAndroid.equalsIgnoreCase("yes")) {
                        ret = true;
                    }
                }

            }
        } catch (Exception e) {
            Log.e(TAG.Lawmo, e.getMessage());
        }
        return ret;
    }

    private Context mContext;
    private DMAgent mDmAgent;
    private MdmTree mDmTree;
}
