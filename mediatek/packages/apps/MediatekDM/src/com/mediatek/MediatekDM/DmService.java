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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import com.android.internal.os.storage.ExternalStorageFormatter;
import com.mediatek.MediatekDM.DmConst.TAG;
import com.mediatek.MediatekDM.conn.DmDataConnection;
import com.mediatek.MediatekDM.data.DownloadInfo;
import com.mediatek.MediatekDM.data.IDmPersistentValues;
import com.mediatek.MediatekDM.data.PersistentContext;
import com.mediatek.MediatekDM.ext.MTKFileUtil;
import com.mediatek.MediatekDM.ext.MTKOptions;
import com.mediatek.MediatekDM.ext.MTKPhone;
import com.mediatek.MediatekDM.fumo.FotaDeltaFiles;
import com.mediatek.MediatekDM.mdm.BootProfile;
import com.mediatek.MediatekDM.mdm.CpSecurity;
import com.mediatek.MediatekDM.mdm.DownloadDescriptor;
import com.mediatek.MediatekDM.mdm.MdmException;
import com.mediatek.MediatekDM.mdm.MdmException.MdmError;
import com.mediatek.MediatekDM.mdm.MdmTree;
import com.mediatek.MediatekDM.mdm.MmiConfirmation.ConfirmCommand;
import com.mediatek.MediatekDM.mdm.PLStorage.ItemType;
import com.mediatek.MediatekDM.mdm.SessionInitiator;
import com.mediatek.MediatekDM.mdm.fumo.FumoAction;
import com.mediatek.MediatekDM.mdm.fumo.FumoState;
import com.mediatek.MediatekDM.mdm.fumo.MdmFumoUpdateResult;
import com.mediatek.MediatekDM.mdm.fumo.MdmFumoUpdateResult.ResultCode;
import com.mediatek.MediatekDM.mdm.lawmo.LawmoAction;
import com.mediatek.MediatekDM.mdm.lawmo.LawmoOperationType;
import com.mediatek.MediatekDM.mdm.lawmo.LawmoResultCode;
import com.mediatek.MediatekDM.mdm.scomo.MdmScomo;
import com.mediatek.MediatekDM.mdm.scomo.MdmScomoDc;
import com.mediatek.MediatekDM.mdm.scomo.MdmScomoDp;
import com.mediatek.MediatekDM.mdm.scomo.MdmScomoResult;
import com.mediatek.MediatekDM.mdm.scomo.ScomoAction;
import com.mediatek.MediatekDM.option.Options;
import com.mediatek.MediatekDM.session.DmSessionStateObserver.DmAction;
import com.mediatek.MediatekDM.util.DmThreadPool;
import com.mediatek.MediatekDM.xml.DmXMLParser;
import com.mediatek.common.dm.DMAgent;
import com.mediatek.common.featureoption.FeatureOption;

import org.w3c.dom.Node;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;

public class DmService extends Service {
    public static final int SESSION_TYPE_NONE = -1;
    public static final int SESSION_TYPE_FUMO = 0;
    public static final int SESSION_TYPE_SCOMO = 1;
    public static final int SESSION_TYPE_LAWMO = 2;

    public static int sessionType = SESSION_TYPE_NONE;

    /**
     * stop service
     */
    private static final int MSG_STOP_SERVICE = 600;

    // scomo
    private int downloadingNotificationCount = 0;
    private List<DmScomoUpdateListener> scomoUpdateListeners = new ArrayList<DmScomoUpdateListener>();
    private static DmScomoState mScomoState;
    public static boolean isScomoSession = false;

    // end scomo

    /**
     * Get the reference of dm service instance.
     * 
     * @return The reference of dm service instance
     */
    static DmService getMdmServiceInstance() {
        return mdmServiceInstance;
    }

    public static DmService getInstance() {
        return mdmServiceInstance;
    }

    /**
     * Override function of android.app.Service, initiate mdm controls.
     */
    public void onCreate() {
        Log.i(TAG.Service, "On create service");
        super.onCreate();
        mDmAgent = MTKPhone.getDmAgent();
        if (mdmServiceInstance == null) {
            mdmServiceInstance = this;
        }

        try {
            isDmTreeReady();
        } catch (Exception e) {
            Log.e(TAG.Service, e.getMessage());
        }

        if (mDmDownloadNotification == null) {
            mDmDownloadNotification = new DmDownloadNotification(this);
        }

        if (mDmDownloadNotification.getListener() != null) {
            Log.i(TAG.Service, ">>>>>>register notification listener");
            registListener(mDmDownloadNotification.getListener());
        }

		if (NiaQueue == null) {
			NiaQueue = new ArrayBlockingQueue<NiaInfo>(MAXNIAQUEUESIZE);
        }

        if (mServiceInstance == null) {
            mServiceInstance = this;
        }
        Log.i(TAG.Service, "On create service done");
    }

    /**
     * Override function of android.app.Service, handle three types of intents:
     * 1. DM wap push 2. boot complete if system upgrades 3. download foreground
     */
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if (intent == null) {
            Log.w(TAG.Service, "onStartCommand intent is null");
            return START_NOT_STICKY;
        }

        String action = intent.getAction();
        if (action == null) {
            Log.w(TAG.Service, "onStartCommand action is null");
            return START_NOT_STICKY;
        }

        if (Options.UseSmsRegister) {
            int registeredSimId = DmCommonFunction.getRegisteredSimId(this);
            if (registeredSimId == -1) {
                Log.w(TAG.Service, "The sim card is not register to DM server");
                return START_NOT_STICKY;
            } else if (MTKOptions.MTK_GEMINI_SUPPORT
                    && action.equals(DmConst.IntentAction.DM_WAP_PUSH)) {
                int receivedSimId = intent.getIntExtra("simId", -1);
                if (receivedSimId != registeredSimId) {
                    Log.w(TAG.Service, "The sim card is not register to DM server,the sim card = "
                            + receivedSimId
                            + ",register sim card = " + registeredSimId);
                    return START_NOT_STICKY;
                }
            }
        }

        Log.i(TAG.Service, "Received start id " + startId + " : " + intent + " action is " + action);

        writeNiaMessage(intent);

        /* cancel the stop service operation */
        mHandler.removeMessages(MSG_STOP_SERVICE);

        if (!Options.UseDirectInternet) {
            try {
                int result = DmDataConnection.getInstance(this).startDmDataConnectivity();
                Log.i(TAG.Service, "starting DM WAP conn...ret=" + result);

                if (result == MTKPhone.APN_ALREADY_ACTIVE) {
                    Log.i(TAG.Service, "handling intent as WAP is ready.");
                    handleStartEvent(intent);
                } else {
                    Log.i(TAG.Service, "saving intent as WAP is not ready yet.");
                    Bundle data = intent.getExtras();
                    if (action.equalsIgnoreCase(DmConst.IntentAction.ACTION_REBOOT_CHECK)) {
                        Log.v(TAG.Service, "saving ACTION_REBOOT_CHECK intent ...");
                        receivedIntent = intent;
                    } else if (action.equalsIgnoreCase(DmConst.IntentAction.DM_WAP_PUSH)) {
                        Log.v(TAG.Service, "[NIA]saving WAP_PUSH intent...");
                        receivedIntent = intent;
                    } else if (action.equals(DmConst.IntentAction.DM_REMINDER)) {
                        Log.v(TAG.Service, "[fota]saving reminder intent...");
                        receivedIntent = intent;
                    } else if (action.equals(DmConst.IntentAction.DM_NIA_START)) {
                        Log.v(TAG.Service, "saving DM_NIA_START intent, it is an alarm...");
                        cancleNiaAlarm();
                        sendBroadcast(new Intent(DmConst.IntentAction.DM_CLOSE_DIALOG));
                        if (mDmDownloadNotification != null) {
                            mDmDownloadNotification.clearDownloadNotification();
                        }
                        receivedIntent = intent;
                    }
                }
            } catch (IOException ex) {
                Log.e(TAG.Service, "startDmDataConnectivity failed.", ex);
            }
        } else {
            Log.i(TAG.Service, "starting intent handling when using internet.");
            handleStartEvent(intent);
        }

        return START_STICKY;
    }

    /**
     * Handle intent.
     * 
     * @param intent The intent to handle. If intent is null, this method will
     *            handle the intent saved previously which is left unhandled
     *            yet.
     */
    private void handleStartEvent(Intent intent) {
        Log.i(TAG.Service, "handleStartEvent");
        if (mDmController == null) {
            initDmController();
        }
        if (mScomoState == null) {
            initAndNotifyScomo();
        }
        if (intent == null) {
            intent = receivedIntent;
            receivedIntent = null;
        }
        if (intent == null || intent.getAction() == null) {
            Log.w(TAG.Service, "handleStartEvent receivedIntent is null");
            scheduleProcessNia();
            return;
        }
        String action = intent.getAction();
        if (action.equals(DmConst.IntentAction.DM_WAP_PUSH)) {
            // NIA WAP push received.
            Log.i(TAG.Service, "Receive NIA wap push intent");
            String type = "";
            type += intent.getType();
            byte[] message = intent.getByteArrayExtra("data");

            if (type.equals(DmConst.IntentType.DM_NIA)) {
                Log.w(TAG.Service, "receive DM_NIA message");
                if (message != null) {
                    receivedNiaMessage(message);
                }

            } else if (type.equals(DmConst.IntentType.BOOTSTRAP_NIA)) {
                HashMap<String, String> contentTypeParameter = (HashMap<String, String>) intent
                        .getParcelableExtra("contentTypeParameter");
                String mac = null;
                if (contentTypeParameter != null) {
                    mac = contentTypeParameter.get("mac");
                }
                triggerBootstrapSession(BootProfile.WAP, CpSecurity.NONE, mac, message);
            }
        } else if (action.equals(DmConst.IntentAction.DM_DL_FOREGROUND)
                || action.equals(DmConst.IntentAction.DM_REMINDER)) {
            Log.i(TAG.Service, "Receive show dm client intent");
            int state = PersistentContext.getInstance(this).getDLSessionStatus();
            if (action.equals(DmConst.IntentAction.DM_REMINDER)
                    && state != IDmPersistentValues.STATE_DLPKGCOMPLETE) {
                Log.w(TAG.Service, "[DM_REMINDER]the dl state is not STATE_DLPKGCOMPLETE, it = "
                        + state);
                return;
            }
            Intent activityIntent = new Intent(this, DmClient.class);
            if (activityIntent != null) {
                activityIntent.setAction("com.mediatek.MediatekDM.DMCLIENT");
                activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(activityIntent);
            }
        } else if (action.equalsIgnoreCase(DmConst.IntentAction.ACTION_REBOOT_CHECK)) {
            Log.i(TAG.Service, "received DM reboot check intent.");
            if (!mDmController.isIdle()) {
                return;
            }
            if (intent.getExtras() == null) {
                return;
            }
            if ("true".equals(intent.getExtras().getString("update"))) {
                Log.i(TAG.Service, "[reboot-state]=>reboot from update.");

                PersistentContext.getInstance(this).deleteDeltaPackage();
                PersistentContext.getInstance(this).setDLSessionStatus(
                        IDmPersistentValues.STATE_UPDATECOMPLETE);

                boolean isUpdateSuccessfull = reportState();

                Intent activityIntent = new Intent(this, DmReport.class);
                activityIntent.setAction("com.mediatek.MediatekDM.UPDATECOMPLETE");
                activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activityIntent.putExtra("isUpdateSuccessfull", isUpdateSuccessfull);
                startActivity(activityIntent);
            }

            if (NiaQueue != null && NiaQueue.size() <= 0
                    && "true".equals(intent.getExtras().getString("nia"))) {
                Log.i(TAG.Service, "[reboot-state]=>has pending NIA.");
                ReadNiaMsg rnia = new ReadNiaMsg();
                if (rnia != null) {
                    if (cExec == null) {
                        cExec = DmThreadPool.getInstance();
                    }
                    if (cExec != null) {
                        cExec.execute(rnia);
                    }
                }
            }
        } else if (action.equals(DmConst.IntentAction.DM_SWUPDATE)) {
            Log.i(TAG.Service, "Receive software update intent");
        } else if (action.equals(DmConst.IntentAction.DM_NIA_START)) {
            Log.w(TAG.Service, "action is DM_NIA_START");
            cancleNiaAlarm();
            sendBroadcast(new Intent(DmConst.IntentAction.DM_CLOSE_DIALOG));
            if (mDmDownloadNotification != null) {
                mDmDownloadNotification.clearDownloadNotification();
            }
            if ("true".equals(intent.getCharSequenceExtra("nia"))) {
                cancleDmSession();
            } else if (!"true".equals(intent.getCharSequenceExtra("interact"))) {
                Log.w(TAG.Service, "trigger nia message beigin");
                if (NiaMessage != null) {
                    triggerNiaMessage(NiaMessage);
                }
            }
        } else if (action.equals("com.mediatek.MediatekDM.FUMO_CI")) {
            // background querying for firmware update.
            Log.i(TAG.Service, "------- fumo ci request start ------");
            setSessionInitor(IDmPersistentValues.CLIENT_POLLING);
            queryNewVersion();
            Log.i(TAG.Service, "------- fumo ci session triggered ------");

        } else {
            Log.i(TAG.Service, "Receive other intent!");
        }
    }

    /**
     * Override function of android.app.Binder
     */
    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG.Service, "On bind service");
        return mBinder;
    }

    /**
     * Override function of android.app.Binder
     */
    public void onRebind(Intent intent) {
        Log.i(TAG.Service, "On rebind service");
        super.onRebind(intent);
    }

    /**
     * Override function of android.app.Binder
     */
    public boolean onUnbind(Intent intent) {
        Log.i(TAG.Service, "On unbind service");
        return super.onUnbind(intent);
    }

    /**
     * Override function of android.app.Service
     */
    public void onDestroy() {
        Log.i(TAG.Service, "On destroy service");
        if (mDmController != null) {
            mDmController.stop();
            mDmController = null;
        }

        if (!Options.UseDirectInternet) {
            DmDataConnection.getInstance(this).stopDmDataConnectivity();
            DmDataConnection.destroyInstance();
        }

        removeListener();
        mServiceInstance = null;
        mdmServiceInstance = null;
        mAlarmMgr = null;
        receivedIntent = null;
        if (mDmDownloadNotification != null) {
            mDmDownloadNotification.clearDownloadNotification();
            mDmDownloadNotification = null;
        }

        if (NiaQueue != null) {
            NiaQueue.clear();
            NiaQueue = null;
        }
        super.onDestroy();
    }

    /**
     * schedule a process nia queue after this call stack
     */
    private void scheduleProcessNia() {
        Log.d(TAG.Service, "scheduleProcessNia()");
        mHandler.sendMessage(mHandler.obtainMessage(
                IDmPersistentValues.MSG_CHECK_NIA, null));
    }

    /**
     * Trigger fumo session to query new version
     */
    public void queryNewVersion() {
        Log.i(TAG.Service, "queryNewVersion Trigger fumo session.");
        if (mDmController != null) {
            mDmController.triggerFumoSession(null);
        }
        Log.i(TAG.Service, "queryNewVersion Fumo session triggered.");
    }

    /**
     * Proceed download session to start download
     */
    public void startDlPkg() {
        Log.i(TAG.Service, "startDlPkg Proceed the download session.");
        mDmController.proceedDLSession();
        Log.i(TAG.Service, "startDlPkg Download session proceeded.");
    }

    void initAndNotifyScomo() {
        mScomoState = DmScomoState.load(this);
        try {
            ArrayList<MdmScomoDp> dps = MdmScomo
                    .getInstance(DmConst.NodeUri.ScomoRootUri, DmScomoHandler.getInstance())
                    .getDps();
            if (dps != null && dps.size() != 0) {
                mScomoState.currentDp = dps.get(0);
            }
            Log.w(TAG.Service, "initAndNotifyScomo currentDp " + mScomoState.currentDp.getName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        notifyScomoListener();

        this.registerScomoListener(new DmScomoNotification(this));
        if (DmFeatureSwitch.DM_SCOMO) {
            DmScomoPackageManager.getInstance().scanPackage();
        }
    }

    void deleteDeltaFile() {
        PersistentContext.getInstance(this).deleteDeltaPackage();
    }

    public void deleteScomoFile() {
        deleteFile(IDmPersistentValues.scomoFileName);
        deleteFile(IDmPersistentValues.resumeScomoFileName);
    }

    public Handler getScomoHandler() {
        return mScomoHandler;
    }

    Handler mScomoHandler = new Handler() {
        public void handleMessage(Message msg) {
            MdmScomoDp dp = null;
            int lastError = 0;
            if (msg == null) {
                return;
            }
            switch (msg.what) {
                case IDmPersistentValues.MSG_SCOMO_DL_PKG_UPGRADE:
                    onScomoDownloading(msg.arg1, msg.arg2);
                    break;
                case IDmPersistentValues.MSG_SCOMO_CONFIRM_DOWNLOAD:
                    dp = (MdmScomoDp) ((Object[]) msg.obj)[0];
                    DownloadDescriptor dd = (DownloadDescriptor) ((Object[]) msg.obj)[1];
                    onScomoConfirmDownload(dp, dd);
                    break;
                case IDmPersistentValues.MSG_SCOMO_CONFIRM_INSTALL:
                    dp = (MdmScomoDp) msg.obj;
                    onScomoConfirmInstall(dp);
                    break;
                case IDmPersistentValues.MSG_SCOMO_EXEC_INSTALL:
                    dp = (MdmScomoDp) msg.obj;
                    onScomoExecuteInstall(dp);
                    break;
                case IDmPersistentValues.MSG_SCOMO_DL_SESSION_COMPLETED:

                    break;
                case IDmPersistentValues.MSG_DM_SESSION_ABORTED:
                    // through
                    lastError = msg.arg1;
                    onScomoError(lastError);
                    processNextNiaMessage();
                    break;
                case IDmPersistentValues.MSG_SCOMO_DL_SESSION_ABORTED:
                    lastError = msg.arg1;
                    onScomoError(lastError);
                    if (mScomoState.state == DmScomoState.IDLE) {
                        processNextNiaMessage();
                    }
                    break;
                case IDmPersistentValues.MSG_DM_SESSION_COMPLETED:
                    deleteScomoFile();
                    processNextNiaMessage();
                    break;
                case IDmPersistentValues.MSG_SCOMO_DL_SESSION_START:
                    mScomoState.state = DmScomoState.DOWNLOADING_STARTED;
                    DmScomoState.store(DmService.getInstance(), mScomoState);
                    notifyScomoListener();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    };

    private void onScomoError(int lastError) {
        Log.e(TAG.Service,
                "last error is " + lastError + " str is "
                        + MdmException.MdmError.fromInt(lastError));
        if ((lastError == MdmException.MdmError.COMMS_FATAL.val)
                || (lastError == MdmException.MdmError.COMMS_NON_FATAL.val)
                || (lastError == MdmException.MdmError.COMMS_SOCKET_ERROR.val)) {
            mScomoState.state = DmScomoState.DOWNLOAD_FAILED;
            mScomoState.errorMsg = "connection failed";
        } else if (lastError == MdmException.MdmError.COMMS_SOCKET_TIMEOUT.val) {
            mScomoState.state = DmScomoState.DOWNLOAD_FAILED;
            mScomoState.errorMsg = "timeout";
        } else if (lastError == MdmException.MdmError.CANCEL.val) {

        } else {
            mScomoState.state = DmScomoState.GENERIC_ERROR;
            mScomoState.errorMsg = "Error: " + MdmException.MdmError.fromInt(lastError);
        }

        DmScomoState.store(this, mScomoState);
        notifyScomoListener();
        // mScomoState.mVerbose=false;
        // Log.e(TAG.Service,"mVerbose set to false onScomoError");
    }

    void onScomoConfirmInstall(MdmScomoDp dp) {
        // Log.i(TAG.Service,"onScomoConfirmInstall: mVerbose is "+mScomoState.mVerbose);
        String archiveFilePath = "";
        try {
            archiveFilePath = DmService.this.getFilesDir().getAbsolutePath() + "/"
                    + dp.getDeliveryPkgPath();
            Log.v(TAG.Service, "scomo archive file path " + archiveFilePath);
        } catch (Exception e) {
            e.printStackTrace();
        }

        mScomoState.currentDp = dp; // currentDp is set again, because
                                    // confirm_download
        // may not be executed, if we are using direct http download instead of
        // OMA download
        mScomoState.setArchivePath(archiveFilePath); // side effect: will set
                                                     // pkgInfo
        Log.w(TAG.Service, "onScomoConfirmInstall currentDp " + dp.getName());

        mScomoState.state = DmScomoState.CONFIRM_INSTALL;
        DmScomoState.store(DmService.this, mScomoState);

        /*
         * if (mScomoState.mVerbose) { Log.i(TAG.Service,
         * "scomoConfirmInstall:mVerbose is true, notifyScomoListener");
         * DmService.this.notifyScomoListener(); } else {
         * Log.e(TAG.Service,"scomoConfirmInstall:mVerbose is nil, confirm directly"
         * );
         */
        try {
            dp.executeInstall();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // }
    }

    void onScomoExecuteInstall(final MdmScomoDp dp) {
        mScomoState.state = DmScomoState.INSTALLING;
        DmScomoState.store(DmService.this, mScomoState);
        DmService.this.notifyScomoListener();
        try {
            String archiveFilePath = DmService.this.getFilesDir().getAbsolutePath() + "/"
                    + dp.getDeliveryPkgPath();

            DmScomoPackageManager.getInstance().install(archiveFilePath,
                    new DmScomoPackageManager.ScomoPackageInstallObserver() {
                        public void packageInstalled(String pkgName, int status) {
                            Log.e(TAG.Service, "dmservice: package installed, status: " + status);
                            if (status == DmScomoPackageManager.STATUS_OK) {
                                try {
                                    String dpId = dp.getId();
                                    MdmScomo scomo = MdmScomo.getInstance(
                                            DmConst.NodeUri.ScomoRootUri,
                                            DmScomoHandler.getInstance());
                                    MdmScomoDc dc = scomo.createDC(pkgName,
                                            DmScomoDcHandler.getInstance(),
                                            DmPLInventory.getInstance());
                                    dc.deleteFromInventory();
                                    dc.destroy();
                                    dc = scomo.createDC(pkgName, DmScomoDcHandler.getInstance(),
                                            DmPLInventory.getInstance());
                                    dc.addToInventory(pkgName, pkgName, dpId, null, null, null,
                                            true);
                                    new MdmTree().writeToPersistentStorage();
                                    dp.triggerReportSession(new MdmScomoResult(1200));
                                    mScomoState.state = DmScomoState.INSTALL_OK;
                                    DmScomoState.store(DmService.this, mScomoState);
                                    DmService.this.notifyScomoListener();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    onScomoInstallFailed(dp);
                                }
                            } else {
                                onScomoInstallFailed(dp);
                            }
                        }
                    }, true);
        } catch (MdmException e) {
            e.printStackTrace();
            onScomoInstallFailed(dp);
        }
        // mVerbose will set to false on every session's begin.
        // mScomoState.mVerbose=false;
        // Log.e(TAG.Service,"mVerbose set to false after install");
    }

    private void onScomoInstallFailed(MdmScomoDp dp) {
        try {
            dp.triggerReportSession(new MdmScomoResult(500));
        } catch (Exception e) {
            e.printStackTrace();
        }
        mScomoState.state = DmScomoState.INSTALL_FAILED;
        DmScomoState.store(DmService.this, mScomoState);
        DmService.this.notifyScomoListener();
    }

    void onScomoConfirmDownload(MdmScomoDp dp, DownloadDescriptor dd) {
        Log.e(TAG.Service, "confirm download");
        mScomoState.currentDd = dd;
        mScomoState.currentDp = dp;

        Log.w(TAG.Service, "onScomoConfirmDownload currentDp " + dp.getName());

        mScomoState.state = DmScomoState.CONFIRM_DOWNLOAD;
        DmScomoState.store(DmService.this, mScomoState);
        try {
            mScomoState.totalSize = Integer.parseInt(dd.getField(DownloadDescriptor.Field.SIZE));
        } catch (Exception e) {
            mScomoState.totalSize = 0;
        }
        if (mScomoState.mVerbose) {
            Log.e(TAG.Service, "scomoConfirmDownload:mVerbose is true, notifyScomoListener");
            DmService.this.notifyScomoListener();
        } else {
            Log.e(TAG.Service, "scomoConfirmDownload:mVerbose is nil, confirm directly");
            this.startDlScomoPkg();
        }
    }

    void onScomoDownloading(int currentSize, int totalSize) {
        Log.i(TAG.Service, "onScomoDownloading, state is " + mScomoState.state);
        if (mScomoState.state == DmScomoState.DOWNLOADING
                || mScomoState.state == DmScomoState.RESUMED
                || mScomoState.state == DmScomoState.IDLE
                || mScomoState.state == DmScomoState.DOWNLOADING_STARTED) {

            mScomoState.state = DmScomoState.DOWNLOADING;
            DmScomoState.store(DmService.this, mScomoState);

            mScomoState.currentSize = currentSize;
            mScomoState.totalSize = totalSize;
            // downloading notification is too much...reshape the frequency
            if (currentSize > (downloadingNotificationCount) * totalSize / 20) {
                downloadingNotificationCount++;
                DmService.this.notifyScomoListener();
            }
            if (currentSize >= totalSize) {
                downloadingNotificationCount = 0;
            }
        }
    }

    private void notifyScomoListener() {
        if (!mScomoState.mVerbose) {
            Log.v(TAG.Service, "---notifyScomoListener, mScomoState.mVerbose is false---");
            return;
        }
        synchronized (scomoUpdateListeners) {
            for (DmScomoUpdateListener listener : scomoUpdateListeners) {
                listener.onScomoUpdated();
            }
        }
    }

    public void registerScomoListener(DmScomoUpdateListener listener) {
        synchronized (scomoUpdateListeners) {
            scomoUpdateListeners.add(listener);
        }
    }

    public void removeScomoListener(DmScomoUpdateListener listener) {
        synchronized (scomoUpdateListeners) {
            scomoUpdateListeners.remove(listener);
        }
    }

    public DmScomoState getScomoState() {
        return mScomoState;
    }

    public void startScomoSession(String dpName) {
        mScomoState.state = DmScomoState.DOWNLOADING_STARTED;

        try {
            MdmScomoDp currentDp = MdmScomo.getInstance(DmConst.NodeUri.ScomoRootUri,
                    DmScomoHandler.getInstance())
                    .createDP(dpName, DmScomoDpHandler.getInstance());
            mScomoState.currentDp = currentDp;
        } catch (Exception e) {
            e.printStackTrace();
        }

        DmScomoState.store(this, mScomoState);
    }

    public void startDlScomoPkg() {
        Log.i(TAG.Service, "startDLScomoPkg");

        mScomoState.state = DmScomoState.DOWNLOADING_STARTED;
        mScomoState.currentSize = 0;
        mScomoState.totalSize = 0;
        DmScomoState.store(this, mScomoState);

        notifyScomoListener();

        HandlerThread thread = DmScomoPackageManager.getInstance().getThread();
        new Handler(thread.getLooper()).post(new Runnable() {
            public void run() {
                deleteScomoFile();
                mDmController.proceedDLSession();
            }
        });
    }

    public void cancelDlScomoPkg() {
        Log.i(TAG.Service, "cancelDlScomoPkg");

        mScomoState.state = DmScomoState.ABORTED;
        DmScomoState.store(this, mScomoState);

        notifyScomoListener();

        HandlerThread thread = DmScomoPackageManager.getInstance().getThread();
        new Handler(thread.getLooper()).post(new Runnable() {
            public void run() {
                MdmScomoDp dp = mScomoState.currentDp;
                try {
                    if (dp != null) {
                        Log.i(TAG.Service, "cancelDlScomoPkg: triggerReportSession 1401");
                        dp.triggerReportSession(new MdmScomoResult(1401));
                    } else {
                        Log.e(TAG.Service, "dp is null");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                deleteScomoFile();
                Log.d(TAG.Service, "scomo dl canceled, delete delta package");
            }
        });
    }

    // public void cancelScomoSession() {
    // Log.e(TAG.Service, "cancelScomoSession");
    // mDmController.cancelDLSession();
    // }

    public void pauseDlScomoPkg() {
        Log.i(TAG.Service, "pauseDlScomoPkg");

        if (mScomoState.state == DmScomoState.RESUMED) {
            Log.w(TAG.Service, "-- The ScomoState is RESUMED, cannot be paused --");
            return;
        }
        if (mScomoState.state == DmScomoState.PAUSED) {
            Log.w(TAG.Service, "-- The ScomoState is PAUSED, no need pause again --");
            return;
        }
        mScomoState.state = DmScomoState.PAUSED;
        DmScomoState.store(this, mScomoState);

        notifyScomoListener();

        HandlerThread thread = DmScomoPackageManager.getInstance().getThread();
        new Handler(thread.getLooper()).post(new Runnable() {
            public void run() {
                mDmController.cancelDLSession();
                Log.i(TAG.Service, "pauseDlScomoPkg end");
            }
        });

    }

    public void resumeDlScomoPkg() {
        Log.d(TAG.Service, "resumeDlScomoPkg");

        if (mScomoState.state != DmScomoState.PAUSED) {
            Log.e(TAG.Service, "-- mScomoState is not PAUSED!! --");
            return;
        }
        if (!isScomoSession) {
            // used for resume scomo when DM died abnormally.
            if (!mDmController.isIdle()) {
                // --it's other DM Session, but User want to resume scomo--
                Log.e(TAG.Service, "--cannot resume! mDmController is not idle!! --");
                notifyScomoListener();
                return;
            }
            isScomoSession = true;
            DmPLDlPkg.setDeltaFileName(IDmPersistentValues.scomoFileName);
        }

        mScomoState.state = DmScomoState.RESUMED;
        DmScomoState.store(this, mScomoState);

        notifyScomoListener();

        HandlerThread thread = DmScomoPackageManager.getInstance().getThread();
        new Handler(thread.getLooper()).post(new Runnable() {
            public void run() {
                try {
                    mScomoState.currentDp.resumeDLSession();
                    Log.d(TAG.Service, "resumDlScomoPkg end");
                } catch (Exception e) {
                    Log.e(TAG.Service, "resumDlScomoPkg exception " + e);
                    e.printStackTrace();
                }
            }
        });
    }

    public void resumeDlScomoPkgNoUI() {
        Log.d(TAG.Service, "resumeDlScomoPkgNoUI");
        if (mScomoState.state == DmScomoState.RESUMED) {
            Log.e(TAG.Service, "-- duplicated resume!! --");
        }
        mScomoState.state = DmScomoState.RESUMED;
        DmScomoState.store(this, mScomoState);

        HandlerThread thread = DmScomoPackageManager.getInstance().getThread();
        new Handler(thread.getLooper()).post(new Runnable() {
            public void run() {
                try {
                    mScomoState.currentDp.resumeDLSession();
                    Log.d(TAG.Service, "resumDlScomoPkgNoUI end");
                } catch (Exception e) {
                    Log.e(TAG.Service, "resumDlScomoPkgNoUI exception " + e);
                    e.printStackTrace();
                }
            }
        });

    }

    // / end scomo
    /**
     * Cancel download session and delete delta packaeg to cancel download
     * 
     * @param boolean userConfirmed - if user confirm to cancel download, delete
     *        the delta package
     */
    public void cancelDlPkg() {

        if (mDmDownloadNotification != null) {
            mDmDownloadNotification.clearDownloadNotification();
        }
        Log.i(TAG.Service, "User cancel the download process.");
        try {
            mDmController.cancelDLSession();
        } catch (Exception e) {
            Log.e(TAG.Service, e.getMessage());
        }
        Log.i(TAG.Service, "[cancelDlPkg]Delete the downloaded file. status is " + mDlStatus);
        PersistentContext.getInstance(DmService.this).deleteDeltaPackage();
        if (mDmController != null) {
            Log.i(TAG.Service, "cancelDlPkg report state to server");
            mDmController.triggerReportSession(MdmFumoUpdateResult.ResultCode.USER_CANCELED);
        }

    }

    /**
     * Cancel download session to pause download
     */
    public void pauseDlPkg() {
        Log.i(TAG.Service, "pauseDlPkg Pause the download session.");
        if (mDmDownloadNotification != null) {
            mDmDownloadNotification.clearDownloadNotification();
        }
        if (mDmController != null) {
            mDmController.cancelDLSession();
        }
        Log.i(TAG.Service, "pauseDlPkg Download session Paused.");
    }

    /**
     * Resume download session to resume download
     */
    public void resumeDlPkg() {
        Log.i(TAG.Service, "resumeDlPkg Resume the download session.");
        if (mDmController != null) {
            Log.w(TAG.Service, "resumeDlPkg engine is idle");
            mDmController.resumeDLSession();
        } else {
            Log.w(TAG.Service, "resumeDlPkg engine is not idle");
        }
        Log.i(TAG.Service, "resumeDlPkg Download session resumed.");
    }

    public void reportresult(MdmFumoUpdateResult.ResultCode resultCode) {
        if (mDmController != null) {
            mDmController.triggerReportSession(resultCode);
        }
    }

    /**
     * Execute firmware update
     */
    public void executeFwUpdate() {
        Log.i(TAG.Service, "executeFwUpdate Execute firmware update.");
        if (mDmController != null) {
            mDmController.executeFwUpdate();
        }
    }

    /**
     * Set reminder alarm due to the item id user selected
     * 
     * @param long checkedItem - item user selected on list view
     */
    public void setAlarm(long checkedItem) {
        Log.i(TAG.Service, "setAlarm Set reminder alarm");
        Intent intent = new Intent();
        intent.setAction(DmConst.IntentAction.DM_REMINDER);
        if (mAlarmMgr == null) {
            Log.w(TAG.Service, "setAlarm alarmMgr is null, get alarmMgr.");
            mAlarmMgr = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        }
        mReminderOperation = PendingIntent.getBroadcast(this, 0, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        mAlarmMgr.cancel(mReminderOperation);
        mAlarmMgr.set(AlarmManager.RTC_WAKEUP,
                (System.currentTimeMillis() + mTimingArray[(int) checkedItem]
                        * ONEMINUTE), mReminderOperation);
    }

    /**
     * Cancel reminder alarm
     */
    public void cancelAlarm() {
        Log.i(TAG.Service, "cancelAlarm, cancel reminder alarm");
        if (mAlarmMgr != null && mReminderOperation != null) {
            Log.w(TAG.Service, "cancle reminder Alarm");
            mAlarmMgr.cancel(mReminderOperation);
            mReminderOperation = null;
        }
    }

    private void setNiaAlarm(long seconds, boolean interact, boolean nia) {
        Log.i(TAG.Service, "setNiaAlarm reminder alarm: nia " + nia + ", interact " + interact);
        Intent intent = new Intent(this, DmService.class);
        if (interact) {
            intent.putExtra("interact", "true");
        }
        if (nia) {
            intent.putExtra("nia", "true");
        }
        intent.setAction(DmConst.IntentAction.DM_NIA_START);
        if (mAlarmMgr == null) {
            mAlarmMgr = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        }
        mNiaOperation = PendingIntent
                .getService(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        mAlarmMgr.cancel(mNiaOperation);
        mAlarmMgr.set(AlarmManager.RTC_WAKEUP, (System.currentTimeMillis() + seconds * ONESECOND),
                mNiaOperation);
    }

    public void cancleNiaAlarm() {
        Log.i(TAG.Service, "cancleNiaAlarm enter");
        if (mAlarmMgr != null && mNiaOperation != null) {
            Log.w(TAG.Service, "cancleNiaAlarm");
            mAlarmMgr.cancel(mNiaOperation);
            mNiaOperation = null;
        }
    }

    /**
     * Get texts of update types
     * 
     * @return String array contains texts of update types
     */
    public String[] getUpdateTypes() {
        // if(mTextArray == null){//remove for MUI issue
        getReminderAndTiming();
        // }
        return mTextArray;
    }

    /**
     * Get timing of update types
     * 
     * @return int array contains texts of update types
     */
    public int[] getTimingType() {
        if (mTimingArray == null) {
            getReminderAndTiming();
        }
        return mTimingArray;
    }

    /**
     * Set update type user selected
     * 
     * @param long type - the item id user selected
     */
    public void setUpdateType(long type) {

        Log.i(TAG.Service, "setUpdateType type is " + type);
        if (type > mTimingArray.length) {
            return;
        }
        Log.i(TAG.Service, "setUpdateType type is, select is  " + mTimingArray[(int) type]);
        if (mTimingArray[(int) type] == UPDATENOW) {
            cancelAlarm();
            executeFwUpdate();
        } else if (mTimingArray[(int) type] == NEVERASK) {
            cancelAlarm();
            processNextNiaMessage();
        } else {
            setAlarm(type);
            processNextNiaMessage();
        }
    }

    /**
     * Trigger NIA session to start a network initiated dm session
     * 
     * @param byte[] message - message body of wap push
     */
    public void triggerNiaMessage(byte[] message) {
        if (message == null) {
            Log.i(TAG.Service, "Do not trigger Nia session : message is null");
            return;
        }
        if (mDmController == null) {
            Log.i(TAG.Service, "Do not trigger Nia session : mDmController is null");
            return;
        }
        Log.i(TAG.Service, "Trigger Nia session.");

        setSessionInitor(IDmPersistentValues.SERVER);

        notifyFumoListener(null, -1, false, IDmPersistentValues.STATE_NIASESSION_START);

        mScomoState.mVerbose = false;
        DmScomoState.store(this, mScomoState);
        mDmController.triggerNiaDmSession(message);

        Log.i(TAG.Service, "Nia session triggered.");
    }

    /**
     * Proceed NIA session
     */
    private void proceedNiaMessage() {
        Log.i(TAG.Service, "Proceed Nia session.");
        if (cExec == null) {
            cExec = DmThreadPool.getInstance();
        }
        if (cExec != null) {
            cExec.execute(new NiaProcessor());
        }
        Log.i(TAG.Service, "Nia session proceeded.");
    }

    /**
     * Trigger report session to report the update result
     */
    public boolean reportState() {
        boolean isUpdateSuccessfull = false;
        if (FeatureOption.MTK_EMMC_SUPPORT) {
            Log.d(TAG.Service, "----- reading OTA result for eMMC -----");
            int otaResult = 0;

            try {
                otaResult = MTKPhone.getDmAgent().readOtaResult();
                Log.d(TAG.Service, "OTA result = " + otaResult);
            } catch (RemoteException ex) {
                Log.e(TAG.Service, "DMAgent->readOtaResult failed:" + ex);
            }
            isUpdateSuccessfull = otaResult == 1;
            if (isUpdateSuccessfull) {
                reportresult(ResultCode.SUCCESSFUL);
            } else {
                reportresult(ResultCode.UPDATE_FAILED);
            }

            // remove the OTA file
            notifyFumoListener(null, IDmPersistentValues.STATE_UPDATECOMPLETE, true, -1);
            PersistentContext.getInstance(DmService.this).deleteDeltaPackage();
            return isUpdateSuccessfull;
        }

        Log.d(TAG.Service, "----- reading OTA result for NAND -----");

        // Check report result.
        File updateFile = new File(DmConst.Path.PathUpdateFile);
        if (!updateFile.exists()) {
            Log.w(TAG.Service, "RebootChecker the update file is not exist");
            return false;
        }
        try {
            Log.i(TAG.Service, "RebootChecker the update file is  exist");
            byte[] ret = new byte[0];
            FileInputStream in = new FileInputStream(DmConst.Path.PathUpdateFile);
            ByteArrayOutputStream swapStream = new ByteArrayOutputStream();
            byte[] buff = new byte[512];
            int rc = 0;
            while ((rc = in.read(buff, 0, 512)) > 0) {
                swapStream.write(buff, 0, rc);
            }
            ret = swapStream.toByteArray();
            in.close();
            swapStream.close();
            String result = new String(ret);
            Log.i(TAG.Service, "RebootChecker update result is " + result);
            if (result != null) {
                isUpdateSuccessfull = result.equalsIgnoreCase("1");
                if (isUpdateSuccessfull) {
                    Log.i(TAG.Service, "RebootChecker update result is 1, report code is "
                            + ResultCode.SUCCESSFUL);
                    reportresult(ResultCode.SUCCESSFUL);
                } else {
                    Log.i(TAG.Service, "RebootChecker update result is 0, report code is "
                            + ResultCode.UPDATE_FAILED);
                    reportresult(ResultCode.UPDATE_FAILED);
                }
            }
        } catch (Exception e) {
            Log.e(TAG.Service, "RebootChecker " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (updateFile != null) {
                    Log.w(TAG.Service, "RebootChecker deltet the update files");
                    updateFile.delete();
                }
                Log.w(TAG.Service, "reportState sync the status, delete the file begin");
                notifyFumoListener(null, IDmPersistentValues.STATE_UPDATECOMPLETE, true, -1);
                PersistentContext.getInstance(DmService.this).deleteDeltaPackage();
                Log.w(TAG.Service, "reportState sync the status, delete the file end");
            } catch (Exception e) {
                Log.e(TAG.Service, e.getMessage());
            }
        }

        return isUpdateSuccessfull;
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
        Log.i(TAG.Service, "Trigger bootstrap session.");
        if (mDmController != null) {
            mDmController.triggerBootstrapSession(profile, security, mac, message);
        }
        Log.i(TAG.Service, "Bootstrap session triggered.");
    }

    /**
     * Called when receive new version detected message
     * 
     * @param DownloadDescriptor dd - download descriptor of current download
     */
    private int onNewVersionDetected(DownloadDescriptor dd) {
        Log.i(TAG.Service, "onNewVersionDetected Get new version founded message.");
        int ret = IDmPersistentValues.MSG_NIASESSION_INVALID;
        if (dd == null) {
            Log.i(TAG.Service, "dd is null, return MSG_NIASESSION_INVALID");
            return ret;
        }

        String ddurl = dd.getField(DownloadDescriptor.Field.OBJECT_URI);
        String ddversion = dd.getField(DownloadDescriptor.Field.VERSION);
        Log.i(TAG.Service, "onNewVersionDetected url is " + ddurl + " version is " + ddversion);
        if (ddurl == null) {
            Log.i(TAG.Service, "ddurl is null, return MSG_NIASESSION_INVALID");
            return ret;
        }
        if (ddversion == null) {
            Log.i(TAG.Service, "ddversion is null, return MSG_NIASESSION_INVALID");
            return ret;
        }
        if (mDlStatus == -1) {
            mDlStatus = PersistentContext.getInstance(this).getDLSessionStatus();
        }
        Log.i(TAG.Service, "mDlStatus:" + mDlStatus + " " + IDmPersistentValues.STATE_PAUSEDOWNLOAD
                + "=pause download"
                + " " + IDmPersistentValues.STATE_DLPKGCOMPLETE + "=DL pkg complete");
        if (mDlStatus == IDmPersistentValues.STATE_DOWNLOADING
                || mDlStatus == IDmPersistentValues.STATE_PAUSEDOWNLOAD
                || mDlStatus == IDmPersistentValues.STATE_DLPKGCOMPLETE) {
            DownloadInfo info = PersistentContext.getInstance(DmService.this).getDownloadInfo();
            if (info != null && info.url.equalsIgnoreCase(ddurl)
                    && info.version.equalsIgnoreCase(ddversion)) {
                Log.w(TAG.Service, "same version was already downloaded. skip!(" + ddurl + ", "
                        + ddversion + ")");
                return ret;
            }

        }
        ret = IDmPersistentValues.MSG_NEWVERSIONDETECTED;
        return ret;

    }

    /**
     * Called when receive download complete message
     */
    private void onDlPkgComplete() {
        Log.i(TAG.Service, "onDlPkgComplete service received the download finish message");
        mDlStatus = IDmPersistentValues.STATE_DLPKGCOMPLETE;

        // unzip & verify FOTA package.
        int result = FotaDeltaFiles.unpackAndVerify(DmConst.Path.pathDelta);
        if (result != FotaDeltaFiles.DELTA_VERIFY_OK) {
            // TODO report error to UI.
        }

    }

    /**
     * Called when receive dm session complete message
     */
    private void onDmSessionComplete(DmAction action) {
        Log.w(TAG.Service, "onDmSessionComplete clear DmConfirmInfo.lastResult");
        DmConfirmInfo.lastResult = ConfirmCommand.UNDEFINED;
        isScomoSession = false;
        // if(mSessionInitor == IDmPersistentValues.CLIENT){
        // if(sessionActions == FumoAction.NONE){
        // onNoNewVersionDetected();
        // }else{
        // checkFumoAction(sessionActions);
        // }
        // }else{
        // Log.i(TAG.Service, "NIA DM session complete.");
        // //int lawmoAction = mDmController.queryLawmoSessionActions();
        // //checkLawmoAction(lawmoAction);
        // }
        if (mDmController != null) {
            if (mDmDownloadNotification != null) {
                mDmDownloadNotification.clearDownloadNotification();
            }
            int fumoAction = 0;
            int lawmoAction = 0;
            int scomoAction = 0;
            if (action != null) {
                Log.w(TAG.Service, "onDmSessionComplete  action is not null");
                fumoAction = action.fumoAction;
                lawmoAction = action.lawmoAction;
                scomoAction = action.scomoAction;
            }
            Log.i(TAG.Service, "onDmSessionComplete fumo action is " + fumoAction);
            Log.i(TAG.Service, "onDmSessionComplete lawmo action is " + lawmoAction);
            Log.i(TAG.Service, "onDmSessionComplete scomo action is " + scomoAction);
            if (fumoAction != FumoAction.NONE) {
                DmPLDlPkg.setDeltaFileName(IDmPersistentValues.deltaFileName);
            } else if (scomoAction != ScomoAction.NONE) {
                isScomoSession = true;
                DmPLDlPkg.setDeltaFileName(IDmPersistentValues.scomoFileName);
                Log.d(TAG.Service,
                        "scomo begin, mState = " + mScomoState.state + ", currentDp = "
                                + String.valueOf(mScomoState.currentDp));
                if (mScomoState.state != DmScomoState.IDLE) {
                    // if state not idle, restore state.
                    mScomoState.state = DmScomoState.IDLE;
                    DmScomoState.store(DmService.this, mScomoState);
                }
                if (mScomoState.currentDp == null) {
                    try {
                        ArrayList<MdmScomoDp> dps = MdmScomo.getInstance(
                                DmConst.NodeUri.ScomoRootUri,
                                DmScomoHandler.getInstance()).getDps();
                        if (dps != null && dps.size() != 0) {
                            mScomoState.currentDp = dps.get(0);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    Log.w(TAG.Service,
                            "onDmSessionComplete currentDp " + mScomoState.currentDp.getName());
                }
                downloadingNotificationCount = 0;
            } else if (fakeLawmoAction == LawmoAction.FACTORY_RESET_EXECUTED) {
                fakeLawmoAction = 0;
                // Erase SD card & Factory reset
                Intent intent = new Intent(ExternalStorageFormatter.FORMAT_AND_FACTORY_RESET);
                intent.setComponent(ExternalStorageFormatter.COMPONENT_NAME);
                this.startService(intent);
                Log.i(TAG.Service,
                        "onDmSessionComplete start Service ExternalStorageFormatter.FORMAT_AND_FACTORY_RESET");
            } else {
                Log.i(TAG.Service, "onDmSessionComplete no action");
                notifyFumoListener(null, IDmPersistentValues.STATE_NOTDOWNLOAD, false,
                        IDmPersistentValues.STATE_NIASESSION_CANCLE);
                scheduleProcessNia();
            }
        }
    }

    /**
     * Called when receive session abort message
     */
    private void onSessionAbort(SessionInitiator initiator, int lastError, int what) {
        String errorCode = MdmException.MdmError.fromInt(lastError).toString();
        Log.i(TAG.Service, "Get error message. LastError is " + errorCode);

        if (initiator.getId().startsWith("Network Inited")
                && lastError == MdmError.COMMS_SOCKET_ERROR.val
                && what == IDmPersistentValues.MSG_DMSESSIONABORTED) {
            if (NiaMessage != null) {
                Log.i(TAG.Service, "Save Nia message for resume. ");
                messageToResume = NiaMessage;
            } else {
                Log.i(TAG.Service, "NiaMessage is null, clear DmConfirmInfo.lastResult.");
                DmConfirmInfo.lastResult = ConfirmCommand.UNDEFINED;
            }
        } else {
            DmConfirmInfo.lastResult = ConfirmCommand.UNDEFINED;
        }

        if (lastError == MdmException.MdmError.CANCEL.val) {
            Log.i(TAG.Service, "last error is user cancle");
            if (mDlStatus == IDmPersistentValues.STATE_PAUSEDOWNLOAD
                    || mDlStatus == IDmPersistentValues.STATE_CANCELDOWNLOAD) {
                Log.i(TAG.Service, "User paused the downloading.");
            }
            Log.i(TAG.Service, "status is " + mDlStatus);
            if (what == IDmPersistentValues.MSG_DLSESSIONABORTED
                    && mDlStatus == IDmPersistentValues.STATE_CANCELDOWNLOAD) {
                Log.i(TAG.Service, "Delete the paritially downloaded file." + "status is "
                        + mDlStatus);
                PersistentContext.getInstance(DmService.this).deleteDeltaPackage();
                DmPLStorage storage = new DmPLStorage(this);
                if (storage != null) {
                    storage.delete(ItemType.DLRESUME);
                }
                Log.i(TAG.Service, "Delete the paritially downloaded file." + "status is "
                        + mDlStatus);
            }

            scheduleProcessNia();
        }
    }

    /**
     * Get update select items from configuration file
     */
    private void getReminderAndTiming() {
        Log.i(TAG.Service, "Execute getReminderParser");
        DmXMLParser xmlParser = new DmXMLParser(DmConst.Path.ReminderConfigFile);
        List<Node> nodeList = new ArrayList<Node>();
        xmlParser.getChildNode(nodeList, "operator");

        if (nodeList != null && nodeList.size() > 0) {
            Node node = nodeList.get(0);
            List<Node> timeNodeList = new ArrayList<Node>();
            xmlParser.getChildNode(node, timeNodeList, DmConst.TagName.Timing);
            Node timingNode = timeNodeList.get(0);
            List<Node> timingNodeList = new ArrayList<Node>();
            xmlParser.getLeafNode(timingNode, timingNodeList, "item");
            if (timingNodeList != null && timingNodeList.size() > 0) {
                int size = timingNodeList.size();
                mTimingArray = new int[size];
                for (int i = 0; i < size; i++) {
                    String nodeStr = timingNodeList.get(i).getFirstChild().getNodeValue();
                    // Here do not catch exception for test of the reminder file
                    mTimingArray[i] = Integer.parseInt(nodeStr);
                }
            }

            List<Node> textList = new ArrayList<Node>();
            xmlParser.getChildNode(node, textList, DmConst.TagName.Text);
            Node textNode = textList.get(0);
            List<Node> textNodeList = new ArrayList<Node>();
            xmlParser.getLeafNode(textNode, textNodeList, "item");
            if (textNodeList != null && textNodeList.size() > 0) {
                int size = textNodeList.size();
                mTextArray = new String[size];
                for (int i = 0; i < size; i++) {
                    String nodeStr = textNodeList.get(i).getFirstChild().getNodeValue();
                    Field filedname = null;
                    int strOffset = 0;
                    try {
                        filedname = R.string.class.getField(nodeStr);
                        strOffset = filedname.getInt(null);
                    } catch (SecurityException e) {
                        e.printStackTrace();
                    } catch (NoSuchFieldException e) {
                        e.printStackTrace();
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                    mTextArray[i] = getString(strOffset);
                }
            }
        }
    }

	private void stopService() {
		Log.d(TAG.Service, "stopService");
		if (mDmController.isIdle() && NiaQueue.size() <= 0) {
			if (!Options.UseDirectInternet) {
				if (DmApplication.getInstance().isDmWapConnected()) {
					Log.d(TAG.Service,
							"****** stopping DM connection when idle ******");
					DmDataConnection.getInstance(DmService.this)
							.stopDmDataConnectivity();
				}
			}
			Log.d(TAG.Service, "****** stopping DM service: " + DmService.this
					+ "******");
			stopSelf();
		}

	}

    public Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg == null) {
                return;
            }
            int status = -1;
            int dmStatus = -1;
            switch (msg.what) {
            case MSG_STOP_SERVICE:
                stopService();
                return;

                case IDmPersistentValues.MSG_NEWVERSIONDETECTED:
                    msg.what = onNewVersionDetected((DownloadDescriptor) msg.obj);
                    if (msg.what == IDmPersistentValues.MSG_NIASESSION_INVALID) {
                        Log.w(TAG.Service, "new version is invalid");
                        if (mDlStatus == IDmPersistentValues.STATE_DOWNLOADING) {
                            cancleDmSession();
                        }
                    } else {
                        Log.w(TAG.Service, "new version is valid");
                        DownloadDescriptor dd = (DownloadDescriptor) msg.obj;

                        Log.d(TAG.Service, "++saving dd..." + dd);
                        Log.d(TAG.Service, "dd size=" + dd.size);
                        Log.d(TAG.Service, "--------------------");
                        for (String value : dd.field) {
                            Log.d(TAG.Service, "\t" + value);
                        }
                        Log.d(TAG.Service, "--------------------");
                        PersistentContext.getInstance(DmService.this).deleteDeltaPackage();
                        PersistentContext.getInstance(DmService.this).setDownloadDescriptor(dd);
                        PersistentContext.getInstance(DmService.this).setDLSessionStatus(
                                IDmPersistentValues.STATE_NEWVERSIONDETECTED);
                        status = IDmPersistentValues.STATE_NEWVERSIONDETECTED;
                    }
                    int fumoaction = mDmController.queryFumoSessionActions();
                    break;
                case IDmPersistentValues.MSG_DLPKGSTARTED:
                    if (mDlStatus == IDmPersistentValues.STATE_PAUSEDOWNLOAD) {
                        Log.v(TAG.Service,
                                "----mHandler, receive MSG_DLPKGSTARTED msg, set status to STATE_DOWNLOADING----");
                        status = IDmPersistentValues.STATE_DOWNLOADING;
                    }
                    break;
                case IDmPersistentValues.MSG_DLPKGUPGRADE:
                    try {
                        Log.v(TAG.Service, "DL_pkg_upgrade,size is " + msg.arg1);
                        long downloadsize = Long.valueOf(msg.arg1);
                        PersistentContext.getInstance(DmService.this).setDownloadedSize(
                                downloadsize);
                    } catch (Exception e) {
                        Log.e(TAG.Service, e.toString());
                    }
                    status = -1;
                    break;
                case IDmPersistentValues.MSG_DLPKGCOMPLETE:
                    if (mDlStatus == IDmPersistentValues.STATE_CANCELDOWNLOAD) {
                        Log.d(TAG.Service, "has already canceled download, ignore complete message");
                        return;
                    }
                    onDlPkgComplete();
                    status = IDmPersistentValues.STATE_DLPKGCOMPLETE;
                    break;
                case IDmPersistentValues.MSG_DMSESSIONABORTED:
                case IDmPersistentValues.MSG_DLSESSIONABORTED:
                    onSessionAbort((SessionInitiator) msg.obj, msg.arg1, msg.what);
                    status = -1;
                    dmStatus = IDmPersistentValues.STATE_NIASESSION_CANCLE;
                    break;
                case IDmPersistentValues.MSG_NIACONFIRMED:
                    proceedNiaMessage();
                    status = -1;
                    break;
                case IDmPersistentValues.MSG_DMSESSIONCOMPLETED:
                    // cancleAlarm();
                    deleteNia();
                    onDmSessionComplete((DmAction) msg.obj);
                    dmStatus = IDmPersistentValues.STATE_NIASESSION_COMPLETE;
                    break;
                case IDmPersistentValues.MSG_WAP_CONNECTION_SUCCESS:
                    Log.w(TAG.Service, "mHandler message is MSG_WAP_CONNECTION_SUCCESS");
                    Log.i(TAG.Service, "wap connect success");
                    if (mDmController == null) {
                        initDmController();
                    }

                    if (mScomoState == null) {
                        initAndNotifyScomo();
                    }
                    if (receivedIntent != null) {
                        handleStartEvent(null);
                    } else {
                        if (messageToResume != null) {
                            Log.i(TAG.Service, "there is messageToResume, triggerNiaDmSession");
                            mDmController.triggerNiaDmSession(messageToResume);
                            messageToResume = null;
                        }
                    }
                    break;
                // added: end
            case IDmPersistentValues.MSG_CHECK_NIA:
                processNextNiaMessage();
                break;

                default:
                    super.handleMessage(msg);
            }
            if (msg.what == IDmPersistentValues.MSG_NIASESSION_INVALID) {
                notifyFumoListener(msg, status, false, dmStatus);
            } else {
                notifyFumoListener(msg, status, true, dmStatus);
            }
        }
    };

    public void initDmController() {
        mDmController = new DmController(this);
    }

    public boolean IsInitDmController() {
        if (mDmController == null) {
            return false;
        } else {
            return true;
        }
    }

    public class DmBinder extends Binder {
        DmService getService() {
            return DmService.this;
        }
    }

    public static DmService getServiceInstance() {
        return mServiceInstance;
    }

    public synchronized void syncStateWithClient(int status) {
        if (status < 0) {
            return;
        }
        mDlStatus = status;
        notifyFumoListener(null, mDlStatus, false, -1);
    }

    public boolean registListener(FumoUpdateListener listener) {
        if (fumoListener == null) {
            return false;
        }
        fumoListener.add(listener);
        return true;
    }

    public void removeListener() {
        if (fumoListener != null) {
            fumoListener.clear();
        }
    }

    public void unregisterListener(FumoUpdateListener listener) {
        if (fumoListener == null || fumoListener.isEmpty()) {
            return;
        }
        try {
            fumoListener.remove(listener);
        } catch (Exception e) {
            Log.e(TAG.Service, e.getMessage());
        }

    }

    private void notifyFumoListener(Message msg, int status, boolean writeBack, int dmSessionStatus) {
        if (fumoListener == null || fumoListener.size() <= 0) {
            return;
        }
        for (int i = 0; i < fumoListener.size(); i++) {
            if (msg != null) {
                fumoListener.get(i).onUpdate(msg);
            }
            if (status >= 0) {
                fumoListener.get(i).syncStatus(status);
            }
            if (dmSessionStatus > 0) {
                fumoListener.get(i).syncDmSession(dmSessionStatus);
            }
        }
        if (writeBack && status >= 0) {
            PersistentContext.getInstance(DmService.this).setDLSessionStatus(status);
        }

    }

    public int getSessionInitor() {
        return mSessionInitor;
    }

    public void setSessionInitor(int initor) {
        Log.d(TAG.Service, "set session initor=" + initor);
        mSessionInitor = initor;
    }

    public void showNiaNotification(int msgState) {
        Log.i(TAG.Service, "showNiaNotification msgState is " + msgState);
        if (niaNotificationShown) {
            Log.i(TAG.Service, "there is messageToResume, check last the notification response");
            if (DmConfirmInfo.lastResult == ConfirmCommand.YES) {
                DmConfirmInfo.observer.notifyConfirmationResult(true);
            } else {
                DmConfirmInfo.observer.notifyConfirmationResult(false);
            }
            return;
        }
        mScomoState.mVerbose = true;
        DmScomoState.store(this, mScomoState);
        Log.i(TAG.Service, "mVerbose set to true");
        if (NiaMessage == null) {
            return;
        }
        Message message = new Message();
        message.what = msgState;
        notifyFumoListener(message, -1, false, -1);
        setNiaAlarm(TIMEOUT_ALERT_1101, false, true);
        niaNotificationShown = true;
    }

    static int decode(byte[] msg) {
        Log.i(TAG.Service, "decode begin");
        int uiMode = -1;
        if (msg == null || msg.length <= 0) {
            return uiMode;
        }
        ByteBuffer buffer = ByteBuffer.wrap(msg);
        if (buffer == null) {
            return uiMode;
        }
        buffer.getDouble();
        buffer.getDouble();
        byte b1 = buffer.get();
        byte b2 = buffer.get();
        uiMode = ((b2 << 2) >>> 6) & 3;
        Log.i(TAG.Service, "uiMode: " + uiMode);
        return uiMode;
    }

    public void cancleDmSession() {
        Log.i(TAG.Service, "cancleDmSession");
        if (mDmController != null) {
            mDmController.cancelDLSession();

        }
    }

    public void userCancled() {
        Log.i(TAG.Service, "userCancled");

        // also cancel NIA alarm when user cancelled.
        cancleNiaAlarm();

        processNextNiaMessage();
    }

    public void receivedNiaMessage(byte[] message) {
        if (mDmController == null) {
            Log.w(TAG.Service, "receivedNiaMessage mDmController is null");
            return;
        }
        boolean idle = mDmController.isIdle();
        Log.i(TAG.Service, "receivedNiaMessage the idle state of the engine is " + idle);
        if (idle) {
            processNextNiaMessage();
        }
    }

    /**
     * Add wap push message to queue and write it to file system.
     * 
     * @param intent The wap push message intent
     */
    public void writeNiaMessage(Intent intent) {
        if (intent == null) {
            Log.w(TAG.Service, "writeNiaMessage receivedIntent is null");
            return;
        }
        if (intent.getAction() == null) {
            Log.w(TAG.Service, "writeNiaMessage receivedIntent asction is null");
            return;
        }
        if (intent.getAction().equals(DmConst.IntentAction.DM_WAP_PUSH)) {
            Log.i(TAG.Service, "Receive NIA wap push intent");
            String type = "";
            type += intent.getType();
            byte[] message = intent.getByteArrayExtra("data");

            if (type.equals(DmConst.IntentType.DM_NIA)) {
                String filename = String.valueOf(System.currentTimeMillis());
                if (NiaQueue == null) {
                    Log.w(TAG.Service, "receivedNiaMessage NiaQueue is null");
                    return;
                }
                if (NiaQueue.size() == MAXNIAQUEUESIZE) {
                    Log.w(TAG.Service, "receivedNiaMessage exceeds the max number messages");
                    return;
                }
                NiaInfo info = new NiaInfo();
                info.filename = filename;
                info.msg = message;
                NiaQueue.add(info);

                if (cExec == null) {
                    cExec = DmThreadPool.getInstance();
                }
                if (cExec != null) {
                    cExec.execute(new writeMsgInfo(message, filename));
                }
            }
        }

    }

    public void processNiaMessage(byte[] message, String filename) {
        Log.i(TAG.Service, "processNiaMessage Enter");
        if (message == null || filename == null || filename.length() <= 0) {
            Log.w(TAG.Service, "Invalid parameters, filename is " + filename);
            return;
        }

        /* dump message */
        hexdump(message);

        currentNiaMsgName = filename;
        NiaMessage = message;
        int uiMode = decode(message);
        Log.i(TAG.Service, "ui mode is " + uiMode);
        if (uiMode >= 0) {
            Message msg = new Message();
            msg.obj = message;
            int status = -1;
            switch (uiMode) {
                case 0:
                case 1:
                    msg.what = IDmPersistentValues.MSG_USERMODE_INVISIBLE;
                    status = IDmPersistentValues.STATE_USERMODE_INVISIBLE;
                    triggerNiaMessage(message);
                    break;
                case 2:
                    setNiaAlarm(TIMEOUT_UI_VISIBLE, false, false);
                    status = IDmPersistentValues.STATE_USERMODE_VISIBLE;
                    msg.what = IDmPersistentValues.MSG_USERMODE_VISIBLE;
                    break;
                case 3:
                    setNiaAlarm(TIMEOUT_UI_INTERACT, true, false);
                    status = IDmPersistentValues.STATE_USERMODE_INTERACT;
                    msg.what = IDmPersistentValues.MSG_USERMODE_INTERACT;
                    break;
                default:
                    break;
            }
            notifyFumoListener(msg, status, false, -1);
        }
    }

    public void processNextNiaMessage() {
        // Reset parameters for Resume DM Session when account abnormal APN
        // Error
        niaNotificationShown = false;
        messageToResume = null;
        mCCStoredParams.clear();
        Log.v(TAG.Service, "reset resume DM Session params, the CCStoredParams number is "
                + mCCStoredParams.size());
        // End reset parameters Resume DM Session when account abnormal APN
        // Error
        mDlStatus = -1;
        Log.v(TAG.Service, "reset mDlStatus for FOTA");
        deleteNia();

        if (!mDmController.isIdle()) {
            Log.d(TAG.Service, "processNextNiaMessage: engine is busy!");
            return;
        }

        NiaInfo currentMsg = null;
        if (NiaQueue == null || NiaQueue.size() <= 0) {
            Log.d(TAG.Service, "processNextNiaMessage there is no nia message to proceed");
            if (!mHandler.hasMessages(MSG_STOP_SERVICE)) {
            	Log.d(TAG.Service, "processNextNiaMessage schedule delay stop service");
                mHandler.sendEmptyMessageDelayed(MSG_STOP_SERVICE, 2 * 60 * 1000);
            }
            return;
        }
        if (NiaQueue != null && NiaQueue.size() > 0) {
            currentMsg = NiaQueue.poll();
        }
        if (currentMsg != null) {
            processNiaMessage(currentMsg.msg, currentMsg.filename);
        }
    }

    class writeMsgInfo implements Runnable {
        private byte[] msg = null;
        private String filename = null;

        public writeMsgInfo(byte[] message, String name) {
            msg = message;
            filename = name;
        }

        public void run() {
            // TODO Auto-generated method stub
            Log.d(TAG.Service, "~~ writeMsgInfo run ~~");
            String dirpath = DmConst.Path.PathInData;
            File dir = new File(dirpath);
            if (!dir.exists()) {
                Log.w(TAG.Service, "writeMsgInfo the data dir is not exist");
                return;
            }
            String niaPath = DmConst.Path.PathNia;
            File nia = new File(niaPath);
            if (!nia.exists()) {
                boolean ret = nia.mkdirs();
                if (ret == false) {
                    Log.w(TAG.Service, "writeMsgInfo make the nia dir fail");
                    return;
                }
            }
            FileOutputStream msgFile = null;
            try {
                String filepath = niaPath + "/" + filename;
                msgFile = new FileOutputStream(filepath);
                msgFile.write(msg);
            } catch (Exception e) {
                Log.e(TAG.Service, e.getMessage());
            } finally {
                try {
                    if (msgFile != null) {
                        msgFile.close();
                        msgFile = null;
                    }
                } catch (Exception e) {
                    Log.e(TAG.Service, e.getMessage());
                }

            }

        }

    }

    public void deleteNia() {
        Log.i(TAG.Service, "deleteNia Enter");
        if (currentNiaMsgName == null || currentNiaMsgName.length() <= 0) {
            Log.w(TAG.Service, "deleteNia the current msg name is null");
            return;
        }
        try {
            String filePath = DmConst.Path.PathNia + "/" + currentNiaMsgName;
            File file = new File(filePath);
            if (file.exists()) {
                boolean ret = file.delete();
                if (ret) {
                    Log.w(TAG.Service, "deleteNia delete file sucess, file name is "
                            + currentNiaMsgName);
                    currentNiaMsgName = null;
                }
            }

        } catch (Exception e) {
            Log.e(TAG.Service, e.getMessage());
        }

    }

    class ReadNiaMsg implements Runnable {

        public void run() {
            // TODO Auto-generated method stub
            try {
                String nia_Folder = DmConst.Path.PathNia;
                File folder = new File(nia_Folder);
                if (!folder.exists()) {
                    Log.w(TAG.Service, "ReadNiaMsg the nia dir is noet exist");
                    return;
                }

                String[] fileExist = folder.list();
                if (fileExist == null || fileExist.length <= 0) {
                    Log.w(TAG.Service, "ReadNiaMsg there is no unproceed message");
                    return;
                }
                if (NiaQueue == null) {
                    Log.w(TAG.Service, "ReadNiaMsg the niaqueue is null");
                    return;
                }
                // long[] files=new long[fileExist.length];
                // for(int i=0;i<fileExist.length;i++)
                // {
                // files[i]=Long.valueOf(fileExist[i]);
                // }
                Arrays.sort(fileExist);
                for (int i = 0; i < fileExist.length && i < MAXNIAQUEUESIZE; i++) {
                    String name = fileExist[i];
                    if (name == null || name.length() <= 0) {
                        continue;
                    }
                    FileInputStream in = null;
                    try {
                        in = new FileInputStream(nia_Folder + "/" + name);
                        ByteArrayOutputStream swapStream = new ByteArrayOutputStream();
                        byte[] buff = new byte[512];
                        int rc = 0;
                        while ((rc = in.read(buff, 0, 512)) > 0) {
                            swapStream.write(buff, 0, rc);
                        }
                        NiaInfo info = new NiaInfo();
                        info.filename = name;
                        info.msg = swapStream.toByteArray();
                        NiaQueue.add(info);
                        swapStream.close();

                    } catch (Exception e) {
                        Log.e(TAG.Service, e.getMessage());
                    } finally {
                        try {
                            if (in != null) {
                                in.close();
                                in = null;
                            }
                        } catch (Exception e) {
                            Log.e(TAG.Service, e.getMessage());
                        }

                    }
                }

                if (NiaQueue != null && NiaQueue.size() > 0) {
                    processNextNiaMessage();
                }

            } catch (Exception e) {
                Log.e(TAG.Service, e.getMessage());
            }
        }

    }

    class NiaInfo {
        private String filename = null;
        private byte[] msg = null;
    }

    public static boolean isDmTreeReady() throws Exception {
        boolean ret = false;
        FileInputStream in = null;
        FileOutputStream out = null;
        try {
            File systemTree = new File(DmConst.Path.DmTreeFileInRom);
            File dataTree = new File(DmConst.Path.DmTreeFile);
            File dataFilesDir = new File(DmConst.Path.PathInData);
            if (!dataTree.exists()) {
                if (!systemTree.exists()) {
                    Log.e(TAG.Service, "The tree in system is not exist");
                    return false;
                }
                if (!dataFilesDir.exists()) {
                    Log.e(TAG.Service, "there is no /files dir in dm folder");
                    if (dataFilesDir.mkdir()) {
                        // chmod for recovery access?
                        MTKFileUtil.openPermission(DmConst.Path.PathInData);
                    } else {
                        Log.e(TAG.Service, "Create files dir in dm folder error");
                        return false;
                    }
                }
                int length = 1024 * 50;
                in = new FileInputStream(systemTree);
                out = new FileOutputStream(dataTree);
                byte[] buffer = new byte[length];
                while (true) {
                    Log.i(TAG.Service, "in while");
                    int ins = in.read(buffer);
                    if (ins == -1) {
                        in.close();
                        out.flush();
                        out.close();
                        Log.i(TAG.Service, "there is no more data");
                        break;
                    } else {
                        out.write(buffer, 0, ins);
                    }
                }// while
                ret = true;
            }
        } catch (Exception e) {
            Log.e(TAG.Service, e.getMessage());
        } finally {
            try {
                if (in != null) {
                    in.close();
                    in = null;
                }
                if (out != null) {
                    out.close();
                    out = null;
                }
            } catch (Exception e) {
                Log.e(TAG.Service, e.getMessage());
            }
        }
        return ret;

    }

    public FumoState getFumoState() {
        if (mDmController != null) {
            return mDmController.getFumoState();
        } else {
            return FumoState.IDLE;
        }
    }

    class WipeReport implements Runnable {

        public void run() {
            // TODO Auto-generated method stub
            if (mDmController == null) {
                Log.w(TAG.Service, "WipeReport mDmController is null");
                return;
            }
            boolean ret = mDmController.triggerLawmoReportSession(LawmoOperationType.FACTORY_RESET,
                    new LawmoResultCode(LawmoResultCode.OPERATION_SUCCESSSFUL));
            if (ret == true) {
                Log.w(TAG.Service, "WipeReport report wipe result to server, delete the file");
                try {
                    if (mDmAgent != null) {
                        mDmAgent.clearWipeFlag();
                    }
                } catch (Exception e) {
                    Log.e(TAG.Service, e.getMessage());
                }
            }
        }

    }

    class NiaProcessor implements Runnable {

        public void run() {
            // TODO Auto-generated method stub
            if (mDmController != null) {
                mDmController.proceedNiaSession();
            }
        }

    }

    public static void hexdump(byte[] data) {
        final int ROW_BYTES = 16;
        final int ROW_QTR1 = 3;
        final int ROW_HALF = 7;
        final int ROW_QTR2 = 11;
        int rows, residue, i, j;
        byte[] save_buf = new byte[ROW_BYTES + 2];
        char[] hex_buf = new char[4];
        char[] idx_buf = new char[8];
        char[] hex_chars = new char[20];

        hex_chars[0] = '0';
        hex_chars[1] = '1';
        hex_chars[2] = '2';
        hex_chars[3] = '3';
        hex_chars[4] = '4';
        hex_chars[5] = '5';
        hex_chars[6] = '6';
        hex_chars[7] = '7';
        hex_chars[8] = '8';
        hex_chars[9] = '9';
        hex_chars[10] = 'A';
        hex_chars[11] = 'B';
        hex_chars[12] = 'C';
        hex_chars[13] = 'D';
        hex_chars[14] = 'E';
        hex_chars[15] = 'F';

        rows = data.length >> 4;
        residue = data.length & 0x0000000F;
        for (i = 0; i < rows; i++) {
            StringBuilder sb = new StringBuilder();
            int hexVal = (i * ROW_BYTES);
            idx_buf[0] = hex_chars[((hexVal >> 12) & 15)];
            idx_buf[1] = hex_chars[((hexVal >> 8) & 15)];
            idx_buf[2] = hex_chars[((hexVal >> 4) & 15)];
            idx_buf[3] = hex_chars[(hexVal & 15)];

            String idxStr = new String(idx_buf, 0, 4);
            sb.append(idxStr + ": ");

            for (j = 0; j < ROW_BYTES; j++) {
                save_buf[j] = data[(i * ROW_BYTES) + j];

                hex_buf[0] = hex_chars[(save_buf[j] >> 4) & 0x0F];
                hex_buf[1] = hex_chars[save_buf[j] & 0x0F];

                sb.append(hex_buf[0]);
                sb.append(hex_buf[1]);
                sb.append(' ');

                if (j == ROW_QTR1 || j == ROW_HALF || j == ROW_QTR2) {
                    sb.append(" ");
                }

                if (save_buf[j] < 0x20 || save_buf[j] > 0x7E) {
                    save_buf[j] = (byte) '.';
                }
            }

            String saveStr = new String(save_buf, 0, j);
            sb.append(" | " + saveStr + " |");
            Log.d(TAG.Service, sb.toString());
        }

        if (residue > 0) {
            StringBuilder sb = new StringBuilder();
            int hexVal = (i * ROW_BYTES);
            idx_buf[0] = hex_chars[((hexVal >> 12) & 15)];
            idx_buf[1] = hex_chars[((hexVal >> 8) & 15)];
            idx_buf[2] = hex_chars[((hexVal >> 4) & 15)];
            idx_buf[3] = hex_chars[(hexVal & 15)];

            String idxStr = new String(idx_buf, 0, 4);
            sb.append(idxStr + ": ");

            for (j = 0; j < residue; j++) {
                save_buf[j] = data[(i * ROW_BYTES) + j];

                hex_buf[0] = hex_chars[(save_buf[j] >> 4) & 0x0F];
                hex_buf[1] = hex_chars[save_buf[j] & 0x0F];

                sb.append((char) hex_buf[0]);
                sb.append((char) hex_buf[1]);
                sb.append(' ');

                if (j == ROW_QTR1 || j == ROW_HALF || j == ROW_QTR2) {
                    sb.append(" ");
                }

                if (save_buf[j] < 0x20 || save_buf[j] > 0x7E) {
                    save_buf[j] = (byte) '.';
                }
            }

            for ( /* j INHERITED */; j < ROW_BYTES; j++) {
                save_buf[j] = (byte) ' ';
                sb.append("   ");
                if (j == ROW_QTR1 || j == ROW_HALF || j == ROW_QTR2) {
                    sb.append(" ");
                }
            }

            String saveStr = new String(save_buf, 0, j);
            sb.append(" | " + saveStr + " |");
            Log.d(TAG.Service, sb.toString());
        }
    }

    private static DmService mdmServiceInstance = null;
    private final IBinder mBinder = new DmBinder();
    private static final int UPDATENOW = 0;
    private static final int NEVERASK = 0x1ffff;
    private static DmController mDmController = null;
    private static int mSessionInitor = IDmPersistentValues.SERVER;
    private static DmDownloadNotification mDmDownloadNotification = null;
    private static boolean mDownloadingBkg = false;
    private static int mDlStatus = -1;
    private static final long ONESECOND = 1000;
    private static final long ONEMINUTE = ONESECOND * 60;

    private int[] mTimingArray = null;
    private String[] mTextArray = null;

    private static ExecutorService cExec = null;
    private static DmService mServiceInstance = null;
    private static ArrayList<FumoUpdateListener> fumoListener = new ArrayList<FumoUpdateListener>();

    private static AlarmManager mAlarmMgr = null;
    private static PendingIntent mNiaOperation = null;
    private static PendingIntent mReminderOperation = null;
    private static byte[] NiaMessage = null;
    // added for resume NIA session when session aborted due to
    // HTTP_SOCKET_ERROR
    private static byte[] messageToResume = null;
    public static Map<String, String> mCCStoredParams = new HashMap<String, String>();;
    private static boolean niaNotificationShown = false;
    // end added

    private static final int MAXNIAQUEUESIZE = 3;
    private static ArrayBlockingQueue<NiaInfo> NiaQueue = new ArrayBlockingQueue<NiaInfo>(
            MAXNIAQUEUESIZE);
    private static String currentNiaMsgName = null;
    // All time out are defined by CMCC spec. in dm session, MAXDT=30
    private static final int TIMEOUT_ALERT_1101 = 30;
    private static final int TIMEOUT_UI_VISIBLE = 10;
    private static final int TIMEOUT_UI_INTERACT = 10 * 60;
    private static Intent receivedIntent = null;
    private static DMAgent mDmAgent = null;
    public static int fakeLawmoAction = 0;
}
