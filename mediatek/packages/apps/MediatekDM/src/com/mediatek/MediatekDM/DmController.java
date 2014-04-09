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

import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.mediatek.MediatekDM.DmConst.TAG;
import com.mediatek.MediatekDM.conn.DmDatabase;
import com.mediatek.MediatekDM.ext.MTKPhone;
import com.mediatek.MediatekDM.lawmo.DmLawmoHandler;
import com.mediatek.MediatekDM.mdm.BootProfile;
import com.mediatek.MediatekDM.mdm.CpSecurity;
import com.mediatek.MediatekDM.mdm.MdmComponent;
import com.mediatek.MediatekDM.mdm.MdmEngine;
import com.mediatek.MediatekDM.mdm.MdmException;
import com.mediatek.MediatekDM.mdm.MdmLogLevel;
import com.mediatek.MediatekDM.mdm.MdmTree;
import com.mediatek.MediatekDM.mdm.fumo.FumoAction;
import com.mediatek.MediatekDM.mdm.fumo.FumoState;
import com.mediatek.MediatekDM.mdm.fumo.MdmFumo;
import com.mediatek.MediatekDM.mdm.fumo.MdmFumoUpdateResult;
import com.mediatek.MediatekDM.mdm.lawmo.LawmoAction;
import com.mediatek.MediatekDM.mdm.lawmo.LawmoOperationType;
import com.mediatek.MediatekDM.mdm.lawmo.LawmoResultCode;
import com.mediatek.MediatekDM.mdm.lawmo.MdmLawmo;
import com.mediatek.MediatekDM.mdm.scomo.MdmScomo;
import com.mediatek.MediatekDM.mdm.scomo.MdmScomoDc;
import com.mediatek.MediatekDM.mdm.scomo.MdmScomoDp;
import com.mediatek.MediatekDM.option.Options;
import com.mediatek.MediatekDM.session.DmSessionStateObserver;
import com.mediatek.MediatekDM.session.DmSessionStateObserver.DmAction;
import com.mediatek.common.dm.DMAgent;
import com.mediatek.telephony.TelephonyManagerEx;

import junit.framework.Assert;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * This class wraps mdm classes, including mdm engine, mdm fumo and mdm tree.
 */
public class DmController {

    /**
     * Constructor of DmController. Initiate mdm controls.
     */
    public DmController(Context context) {
        Log.i(TAG.Controller, "Enter DmController constructor");
        mLogger = new DmPLLogger(DmService.getMdmServiceInstance());
        Log.i(TAG.Controller, "Logger created");
        createEngine(DmService.getMdmServiceInstance());
        mDmNiaMsgHandler = new DmNiaMsgHandler();
        mSessionStateObserver = new DmSessionStateObserver();
        mDmBootstrapHandler = new DmBootstrapHandler();
        mDmConfig = new DmConfig(DmService.getMdmServiceInstance());
        mDmConfig.configure();
        setTimeout(DmConst.TimeValue.TIMEOUT);
        startEngine();

        if (DmFeatureSwitch.DM_FUMO) {
            createFumo(DmConst.NodeUri.FumoRootUri);
        }

        if (DmFeatureSwitch.DM_LAWMO) {
            createLawmo(DmConst.NodeUri.LawmoRootUri, context);
        }

        if (DmFeatureSwitch.DM_SCOMO) {
            createScomo();
        }

        mDmTree = new MdmTree();

        if (!Options.UseDirectInternet) {
            Log.i(TAG.Controller, "Start to sync dm address");
            if (syncDmServerAddr() == false) {
                Log.e(TAG.Controller, "Sync Dm tree with database failed!!");
            }
        }

        syncLawmoStatus();

        mSwVIOHandler = new DmDevSwVNodeIOHandler();
        mDevIdIOHandler = new DmDevIdNodeIOHandler(context);
        registerSwVNodeIOHandler();
        registerDevIdNodeIOHandler();

        if (Options.UseSmsRegister) {
            int simSlot = DmCommonFunction.getRegisteredSimId(context);
            if (simSlot != -1) {
                TelephonyManagerEx teleMgr = TelephonyManagerEx.getDefault();
                String mccmnc = MTKPhone.getSimOperatorGemini(teleMgr, simSlot);
                Log.w(TAG.Controller, "reg sim mccmnc: " + mccmnc);
                DmRegister dr = new DmRegister(mContext, mccmnc);
                dr.registerCCNodeIoHandler(DmConst.Path.DmTreeFile);
            } else {
                Log.e(TAG.Controller, "SIM reg not finished or wrong");
            }
        } else {
            DmRegister dr = new DmRegister(mContext, null);
            dr.registerCCNodeIoHandler(DmConst.Path.DmTreeFile);
        }

        registerSessionStateObserver();
        Log.i(TAG.Controller, "Exit DmController constructor");
    }

    /**
     * Release mdm resources
     */
    public void stop() {
        if (mEngine != null) {
            mEngine.stop();
        }

        if (mFumo != null) {
            Log.i(TAG.Controller, "Destroy mdm fumo.");
            terminateFumo();
        }

        if (mLawmo != null) {
            Log.i(TAG.Controller, "Destroy mdm lawmo.");
            terminateLawmo();
        }

        if (mScomo != null) {
            Log.i(TAG.Controller, "Destroy scomolawmo.");
            terminateScomo();
        }

        if (mEngine != null) {
            Log.i(TAG.Controller, "Unregister session state observer.");
            mEngine.unregisterSessionStateObserver(mSessionStateObserver);
            Log.i(TAG.Controller, "Destroy mdm engine.");
            mEngine.destroy();
            mEngine = null;
        }
    }

    public void stopEngine() {
        if (mEngine != null) {
            Log.w(TAG.Controller, "stopEngine stop engine");
            mEngine.stop();
        }
    }

    /**
     * Create mdm engine
     * 
     * @param Context context - context that use mdm engine.
     */
    public void createEngine(Context context) {
        try {
            mContext = context;
            Log.i(TAG.Controller, "Create mdm engine.");
            mEngine = new MdmEngine(mContext, new DmMmiFactory(), new DmPLFactory(mContext));
            mLogger.logMsg(Log.INFO, "MdmEngine created ");
            Log.i(TAG.Controller, "Mdm engine created.");
        } catch (MdmException e) {
            Log.e(TAG.Controller, "Create mdm engine error.");
            mLogger.logMsg(Log.ERROR, "MdmEngine ctor exception " + e.getError().name());
            e.printStackTrace();
        }
    }

    /**
     * Create mdm fumo and fumo handler
     * 
     * @param String fumoRootUri - root node uri of fumo in dm tree
     */
    public void createFumo(String fumoRootUri) {
        mLogger.logMsg(Log.VERBOSE, "Creating FUMO object");
        Log.i(TAG.Controller, "Create fumo handler.");
        mFumoHandler = new DmFumoHandler();
        Log.i(TAG.Controller, "Create mdm fumo.");
        try {
            mFumo = new MdmFumo(fumoRootUri, mFumoHandler);
        } catch (MdmException e) {
            e.printStackTrace();
        }
        Log.i(TAG.Controller, "Mdm fumo created.");
    }

    /**
     * Destroy fumo
     */
    public void terminateFumo() {
        if (mFumo != null) {
            mFumo.destroy();
            mFumo = null;
        }

    }

    /**
     * Create mdm lawmo and lawmo handler
     * 
     * @param String fumoRootUri - root node uri of fumo in dm tree
     */
    public void createLawmo(String lawmoRootUri, Context context) {
        mLogger.logMsg(Log.VERBOSE, "Creating LAWMO object");
        Log.i(TAG.Controller, "Create lawmo handler.");
        mLawmoHandler = new DmLawmoHandler(context);
        Log.i(TAG.Controller, "Create mdm lawmo.");
        mLawmo = new MdmLawmo(lawmoRootUri, mLawmoHandler);
        Log.i(TAG.Controller, "Mdm lawmo created.");
    }

    /**
     * Destroy lawmo
     */
    public void terminateLawmo() {
        if (mLawmo != null) {
            mLawmo.destroy();
            mLawmo = null;
        }
    }

    public void terminateScomo() {
        mScomo.destroy();
        mScomo = null;
    }

    /**
     * Register node IO handler of getting software version
     */
    public void registerSwVNodeIOHandler() {
        try {
            mDmTree.registerNodeIoHandler(DmConst.NodeUri.DevDetailSwVUri, mSwVIOHandler);
            Log.i(TAG.Controller, "Node : " + DmConst.NodeUri.DevDetailSwVUri
                    + "IO handler registered");
        } catch (MdmException e) {
            Log.e(TAG.Controller, "MdmException in registerSwVNodeIOHandler()", e);
        }
    }

    /**
     * Register node IO handler of getting software version
     */
    public void registerDevIdNodeIOHandler() {
        try {
            mDmTree.registerNodeIoHandler(DmConst.NodeUri.DevInfoDevId, mDevIdIOHandler);
            Log.i(TAG.Controller, "Node : " + DmConst.NodeUri.DevInfoDevId
                    + "IO handler registered");
        } catch (MdmException e) {
            Log.e(TAG.Controller, "MdmException in registerDevIdNodeIOHandler()", e);
        }
    }

    /**
     * Register session state observer
     */
    public void registerSessionStateObserver() {
        Log.i(TAG.Controller, "Register session state observer.");
        if (mEngine != null) {
            mEngine.registerSessionStateObserver(mSessionStateObserver);
            Log.i(TAG.Controller, "Session state observer registered.");
        }
    }

    /**
     * Set time out value
     * 
     * @param int seconds - time out value
     */
    public void setTimeout(int seconds) {
        Log.i(TAG.Controller, "Set connection time out : " + seconds + "s");
        if (mEngine != null) {
            mEngine.setConnectionTimeout(seconds);
        }
        Log.i(TAG.Controller, "Connection time out set.");
    }

    /**
     * Start mdm engine
     */
    public void startEngine() {
        try {
            Log.i(TAG.Controller, "Start mdm engine.");
            mEngine.setDefaultLogLevel(MdmLogLevel.DEBUG);
            mEngine.setComponentLogLevel(MdmComponent.TREE, MdmLogLevel.WARNING);
            mEngine.start();
            Log.i(TAG.Controller, "Mdm engine started.");
        } catch (MdmException e) {
            Log.e(TAG.Controller, "MdmException in startEngine()", e);
            mLogger.logMsg(Log.ERROR, "MdmEngine start exception " + e);
        } catch (Exception e) {
            Log.e(TAG.Controller, e.getMessage());
        }
    }

    /**
     * Trigger fumo session
     * 
     * @param byte[] message - message transfered to mdm fumo. Could be null.
     */
    public void triggerFumoSession(byte[] message) {
        MdmFumo.ClientType clientType = MdmFumo.ClientType.USER;
        try {
            Log.d(TAG.Controller, "[fumo]trigger session start--->");
            mFumo.triggerSession(null, clientType);
            Log.d(TAG.Controller, "[fumo]session triggerred<---");
        } catch (MdmException e) {
            Log.e(TAG.Controller, "MdmException in triggerFumoSession()", e);
        }
    }

    /**
     * Confirm to start download.
     */
    public void proceedDLSession() {
        try {
            mEngine.notifyDLSessionProceed();
        } catch (MdmException e) {
            Log.e(TAG.Controller, "MdmException in proceedDLSession()", e);
        }
    }

    /**
     * Cancel session.
     */
    public void cancelSession() {
        try {
            mEngine.cancelSession();
        } catch (MdmException e) {
            Log.e(TAG.Controller, "MdmException in cancelSession()", e);
        }
    }

    /**
     * Cancel download session
     */
    public void cancelDLSession() {
        try {
            mEngine.cancelSession();
        } catch (MdmException e) {
            Log.e(TAG.Controller, "MdmException in cancelDLSession()", e);
        }
    }

    /**
     * Resume download session
     */
    public void resumeDLSession() {
        Log.i(TAG.Controller, "resuming DL session.");
        try {
            mFumo.resumeDLSession();
        } catch (MdmException e) {
            Log.e(TAG.Controller, "MdmException in resumeDLSession()", e);
        }
    }

    /**
     * Execute firmware update
     */
    public void executeFwUpdate() {
        // Set a flag to tell recovery do the update.
        Log.d(TAG.Controller, "Execute firmware update");
        setUpdateFlag();

        // Create a flag file to indicate this is FUMO update operation. We will
        // check the existence of this file to
        // tell whether the reboot is triggered by FOTA.
        Log.d(TAG.Controller, "Touch flag file: " + DmConst.Path.FotaExecFlagFile);
        File ff = new File(DmConst.Path.FotaExecFlagFile);
        try {
            ff.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG.Controller, "Failed to touch flag file: " + e);
        }

        // Reboot to update firmware. Recovery will do the heavy work.
        try {
            Intent rebootIntent = new Intent(Intent.ACTION_REBOOT);
            rebootIntent.putExtra("nowait", 1);
            rebootIntent.putExtra("interval", 1);
            rebootIntent.putExtra("window", 0);
            mContext.sendBroadcast(rebootIntent);
        } catch (Exception e) {
            Log.e(TAG.Controller, "Exception occurs when rebooting.");
            e.printStackTrace();
        }
    }

    /**
     * Set update flag
     * 
     * @return If set flag successfully, return true, else return false
     */
    private boolean setUpdateFlag() {
        Log.i(TAG.Controller, "setUpdateFlag");
        boolean ret = true;
        try {
            DMAgent agent = MTKPhone.getDmAgent();
            if (agent != null) {
                ret = agent.setRebootFlag();
            }
        } catch (Exception e) {
            Log.e(TAG.Controller, "Exception accur when set flag to reboot into recovery mode.");
            e.printStackTrace();
        }
        return ret;
    }

    /**
     * Trigger report session
     * 
     * @param MdmFumoUpdateResult .ResultCode inResultCode - result of update
     */
    public void triggerReportSession(MdmFumoUpdateResult.ResultCode inResultCode) {
        try {
            Log.i(TAG.Controller,
                    "triggerReportSession fumo report result to server, result code is "
                            + inResultCode);
            mFumo.triggerReportSession(inResultCode);
        } catch (MdmException e) {
            Log.e(TAG.Controller, "MdmException in triggerReportSession(" + inResultCode + ")", e);
        }
    }

    /**
     * Trigger NIA dm session
     * 
     * @param byte[] message - message body of wap push
     */
    public void triggerNiaDmSession(byte[] message) {
        if (mEngine == null) {
            Log.w(TAG.Controller, "triggerNiaDmSession mEngine is null");
            return;
        }
        try {
            mEngine.triggerNIADmSession(message, mDmNiaMsgHandler, mDmNiaMsgHandler);
        } catch (MdmException e) {
            Log.e(TAG.Controller, "MdmException in triggerNiaDmSession()", e);
        } catch (Exception e) {
            Log.e(TAG.Controller, e.getMessage());
        }
    }

    /**
     * Proceed NIA dm session
     */
    public void proceedNiaSession() {
        try {
            mEngine.notifyNIASessionProceed();
        } catch (MdmException e) {
            Log.e(TAG.Controller, "MdmException in proceedNiaSession()", e);
        }
    }

    /**
     * Trigger boot strap session
     * 
     * @param BootProfile profile - boot profile
     * @param CpSecurity security - security level
     * @param String mac - mac region of wap push
     * @param byte[] message - message body of wap push
     */
    public void triggerBootstrapSession(BootProfile profile, CpSecurity security, String mac,
            byte[] message) {
        try {
            mEngine.triggerBootstrapSession(null, profile, security, mac, message,
                    mDmBootstrapHandler,
                    mDmBootstrapHandler);
        } catch (MdmException e) {
            Log.e(TAG.Controller, "MdmException in triggerBootstrapSession()", e);
        }
    }

    /**
     * Query fumo session actions
     */
    public int queryFumoSessionActions() {
        if (mFumo != null) {
            return mFumo.querySessionActions();
        } else {
            Log.w(TAG.Controller, "queryFumoSessionActions mFumo is null");
            return FumoAction.NONE;
        }
    }

    /**
     * Query lawmo session actions
     */
    public int queryLawmoSessionActions() {
        if (mLawmo != null) {
            return mLawmo.querySessionActions();
        } else {
            return LawmoAction.NONE;
        }
    }

    public int queryScomoSessionActions() {
        return mScomo.querySessionActions();
    }

    /**
     * The DM tree file is ok or not
     */

    public boolean isIdle() {
        if (mEngine != null) {
            return mEngine.isIdle();
        } else {
            return false;
        }
    }

    /**
     * Create SCOMO manager and register DP & DC handlers.
     */
    public void createScomo() {
        try {
            Log.d(TAG.Controller, "createScomo()");
            mScomo = MdmScomo.getInstance(DmConst.NodeUri.ScomoRootUri,
                    DmScomoHandler.getInstance());
            mScomo.setAutoAddDPChildNodes(true);
            ArrayList<MdmScomoDp> dps = mScomo.getDps();
            if (dps != null) {
                for (MdmScomoDp dp : dps) {
                    dp.setHandler(DmScomoDpHandler.getInstance());
                }
            }
            ArrayList<MdmScomoDc> dcs = mScomo.getDcs();
            if (dcs != null) {
                Log.d(TAG.Controller, "createScomo: dcs size is " + dcs.size());
                for (MdmScomoDc dc : dcs) {
                    dc.setHandler(DmScomoDcHandler.getInstance());
                    dc.setPLInventory(DmPLInventory.getInstance());
                }
            }
        } catch (MdmException e) {
            e.printStackTrace();
        }
    }

    private void syncLawmoStatus() {
        if (mLawmo == null) {
            return;
        }

        boolean isFullyLock = false;
        int lockStatus = -1;
        String lawmoUri = "./LAWMO/State";
        try {
            DMAgent agent = MTKPhone.getDmAgent();
            if (agent != null) {
                Log.i(TAG.Controller, "The device lock status is " + agent.isLockFlagSet());
                if (agent.isLockFlagSet() == true) {
                    // the status is locked, if it is full lock
                    // isPartillyLock = agent.getLockType();
                    isFullyLock = (agent.getLockType() == 1);
                    Log.i(TAG.Controller, "is fully lock is " + isFullyLock);
                    if (isFullyLock == false) {
                        lockStatus = DmConst.LawmoStatus.PARTIALY_LOCK;
                    } else {
                        lockStatus = DmConst.LawmoStatus.FULLY_LOCK;
                    }
                    Log.i(TAG.Controller, "Lock status is " + lockStatus);
                    if (lockStatus == DmConst.LawmoStatus.FULLY_LOCK
                            || lockStatus == DmConst.LawmoStatus.PARTIALY_LOCK) {
                        int treeLawmoStatus = mDmTree.getIntValue(lawmoUri);
                        Log.i(TAG.Controller, "Lawmo status in tree is " + treeLawmoStatus);
                        if (lockStatus != treeLawmoStatus) {
                            // need to write dm tree to sync lawmo status
                            mDmTree.replaceIntValue(lawmoUri, lockStatus);
                            mDmTree.writeToPersistentStorage();
                            Log.i(TAG.Controller, "After write status, the lawmo staus is "
                                    + mDmTree.getIntValue(lawmoUri));
                        }

                    }
                }

            } else {
                Log.e(TAG.Controller, "DmAgent is null");
                return;
            }

        } catch (Exception e) {
            Log.e(TAG.Controller, "get lock status error.");
            e.printStackTrace();
        }

    }

    public FumoState getFumoState() {
        FumoState state = FumoState.IDLE;
        try {
            if (mFumo != null) {
                state = mFumo.getState();
            }
        } catch (Exception e) {
            Log.w(TAG.Controller, e.getMessage());
        }
        return state;
    }

    public boolean triggerLawmoReportSession(LawmoOperationType type, LawmoResultCode code) {
        Log.w(TAG.Controller, "triggerLawmoReportSession begin");
        boolean ret = true;
        if (mLawmo != null) {
            try {
                Log.w(TAG.Controller, "triggerLawmoReportSession trigger report session type is "
                        + type);
                mLawmo.triggerReportSession(type, new LawmoResultCode(
                        LawmoResultCode.OPERATION_SUCCESSSFUL));
            } catch (Exception e) {
                ret = false;
                Log.e(TAG.Controller, "triggerLawmoReportSession" + e.getMessage());
            }
        } else {
            ret = false;
            Log.e(TAG.Controller, "triggerLawmoReportSession mLawmo is null");
        }
        return ret;
    }

    public static DmAction getDmAction() {
        DmAction action = new DmAction();
        if (mFumo == null) {
            action.fumoAction = 0;
        } else {
            action.fumoAction = mFumo.querySessionActions();
        }

        if (mLawmo == null) {
            action.lawmoAction = 0;
        } else {
            action.lawmoAction = mLawmo.querySessionActions();
        }

        if (mScomo == null) {
            action.scomoAction = 0;
        } else {
            action.scomoAction = mScomo.querySessionActions();
        }

        Log.w(TAG.Controller, "getDmAction left, fumo action is " + action.fumoAction
                + ",lawmo action is "
                + action.lawmoAction + ",scomo action is " + action.scomoAction);
        return action;
    }

    // *************************** DM WAP connection begins
    // *****************************//
    /**
     * Update DM server address in DM tree with values from DmDatabase.
     */
    private boolean syncDmServerAddr() {
        Assert.assertFalse("syncDmServerAddr MUST NOT called in direct internet",
                Options.UseDirectInternet);

        Log.i(TAG.Controller, "Start to sync dm server address with dm tree");
        String nodeUri = null;
        DmDatabase dmDatabase = new DmDatabase(mContext);
        String serverAddrInDb = dmDatabase.getDmAddressFromSettings();
        Log.i(TAG.Controller, "Get dm server address in database is " + serverAddrInDb);
        if (serverAddrInDb == null || serverAddrInDb.equals("")) {
            Log.e(TAG.Controller, "Get dm server address from database error!");
            return false;
        }
        try {
            String opName = DmCommonFunction.getOperatorName();
            if (opName == null) {
                Log.e(TAG.Controller, "Get operator name from config file is null");
                return false;
            }
            Log.i(TAG.Controller, "operator name is " + opName);
            if (opName.equals("cmcc")) {
                nodeUri = "./DMAcc/OMSAcc/AppAddr/SrvAddr/Addr";
            } else if (opName.equals("cu")) {
                nodeUri = "./DMAcc/CUDMAcc/AppAddr/CUDMAcc/Addr";
            } else {
                Log.e(TAG.Controller, "There is not the right operator");
                return false;
            }

            Log.i(TAG.Controller, "The urinode is " + nodeUri);

            String serverAddrInTree = mDmTree.getStringValue(nodeUri);
            if (serverAddrInDb != null && !(serverAddrInDb.equals(serverAddrInTree))) {
                Log.i(TAG.Controller, "Start to write serverAddrInTree = " + serverAddrInTree);
                mDmTree.replaceStringValue(nodeUri, serverAddrInDb);
            }
            String serverAddrInTree1 = mDmTree.getStringValue(nodeUri);
            Log.i(TAG.Controller, "After write serverAddr in Dm Tree  = " + serverAddrInTree1);

        } catch (MdmException e) {
            e.printStackTrace();
        }
        return true;
    }

    // *************************** DM WAP connection ends
    // *****************************//

    private MdmEngine mEngine;
    private MdmTree mDmTree;
    private static MdmFumo mFumo = null;
    private DmFumoHandler mFumoHandler;
    private static MdmLawmo mLawmo;
    private static MdmScomo mScomo;
    private DmLawmoHandler mLawmoHandler;
    private DmNiaMsgHandler mDmNiaMsgHandler;
    private DmSessionStateObserver mSessionStateObserver;
    private DmBootstrapHandler mDmBootstrapHandler;
    private DmDevSwVNodeIOHandler mSwVIOHandler;
    private DmDevIdNodeIOHandler mDevIdIOHandler;
    private DmPLLogger mLogger;
    private DmConfig mDmConfig;
    private Context mContext;
}
