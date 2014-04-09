/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
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

package com.mediatek.voicecommand.app;

import java.util.ArrayList;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import com.mediatek.common.voicecommand.IVoiceCommandListener;
import com.mediatek.common.voicecommand.IVoiceCommandManager;
import com.mediatek.common.voicecommand.IVoiceCommandManagerService;
import com.mediatek.common.voicecommand.VoiceCommandListener;

public class VoiceCommandManager implements IVoiceCommandManager {

    private static final String TAG = "VoiceCommandManager";
    private byte[] mLock = new byte[0];
    private Context mAppContext;
    // private Context mComponentContext;
    private IVoiceCommandManagerService mVCmdMgrService;
    private Intent mVoiceServiceIntent;
    /**
     * It Contains all the registration listener from the component or other
     * system service
     */
    private ArrayList<VoiceCommandListener> mRegisterListeners = new ArrayList<VoiceCommandListener>();
    private ArrayList<CacheCommand> mCommandCaches = new ArrayList<CacheCommand>();
    // private VoiceCommandListener mCurListener = null;

    boolean isRegistered = false;
    // boolean isServiceConnected = false;
    boolean isServiceConnecting = false;

    // If Remote Exception , we need to Reconnect the service
    // private int mReconnectIndex = 0;
    // private int mMaxReconnect = 3;

    // Callback used to notify apps
    private IVoiceCommandListener mCallback = new IVoiceCommandListener.Stub() {

        @Override
        public void onVoiceCommandNotified(int mainAction, int subAction,
                Bundle extraData) throws RemoteException {
            // TODO Auto-generated method stub
            // must add handler here to avoid async operation
            Message.obtain(mHandler, mainAction, subAction, 0, extraData)
                    .sendToTarget();
        }
    };

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName classname, IBinder service) {
            // TODO Auto-generated method stub
            synchronized (mLock) {
                Log.d(TAG, "ServiceConnection onServiceConnected");
                mVCmdMgrService = IVoiceCommandManagerService.Stub
                        .asInterface(service);
                isServiceConnecting = false;
                if (!mRegisterListeners.isEmpty()) {
                    try {

                        registerListener(mRegisterListeners
                                .get(mRegisterListeners.size() - 1));
                    } catch (RemoteException e) {
                        // TODO Auto-generated catch block
                        handleServiceDisconnected(true);
                    } catch (IllegalAccessException e) {
                        // TODO Auto-generated catch block
                        Log.e(TAG,
                                "registerListener Error in onServiceConnected");
                        // Do nothing because illegal listener or no listener is
                        // exist
                        // in the arraylist
                    }
                    // Cache command need to be sent right now
                    if (isRegistered) {
                        Log.d(TAG, "Send CacheCommand");
                        for (CacheCommand command : mCommandCaches) {
                            try {
                                sendCommand(mAppContext, command.mMainAction,
                                        command.mSubAction, command.mExtraData);
                            } catch (RemoteException e) {
                                // TODO Auto-generated catch block
                                handleServiceDisconnected(true);
                            } catch (IllegalAccessException e) {
                                // TODO Auto-generated catch block
                                // Do nothing because illegal listener or no
                                // listener is exist
                                Log
                                        .e(TAG,
                                                "sendCommand Error in onServiceConnected");
                            }
                        }
                        mCommandCaches.clear();
                    }
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // TODO Auto-generated method stub
            handleServiceDisconnected(true);
            // Because we need to bind to the service immediately , we should
        }
    };

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            synchronized (mLock) {
                switch (msg.what) {
                case VoiceCommandListener.ACTION_MAIN_VOICE_UNREGISTER:

                    break;
                default:
                    for (VoiceCommandListener listener : mRegisterListeners) {
                        listener.onVoiceCommandNotified(msg.what, msg.arg1,
                            (Bundle) msg.obj);
                    }
                    break;
                }
            }
        }
    };

    public VoiceCommandManager(Context context) {
        // mComponentContext = context;
        if (!context.getPackageName().equals(
                IVoiceCommandManager.sSystemProcess)) {
            context = context.getApplicationContext();
        }

        if (mAppContext == null) {
            mAppContext = context;
        } else if (mAppContext != context) {
            Log.e(TAG, "Constructor called more than once in the process");
            Log.e(TAG, "Original: " + mAppContext.getPackageName() + ", new:  "
                    + context.getPackageName());
        }

        if (mAppContext == null) {
            Log.e(TAG, "sContext is null!!!");
        }

        mVoiceServiceIntent = new Intent();
        mVoiceServiceIntent.setAction("android.mediatek.voicecommand");
        mVoiceServiceIntent.addCategory("android.mediatek.NativeService");
        bindVoiceService();
    }

    /*
     * Bind service and judge whether is connecting
     */
    private void bindVoiceService() {
        isServiceConnecting = true;
        if (!mAppContext.bindService(mVoiceServiceIntent, mServiceConnection,
                Context.BIND_AUTO_CREATE)) {
            isServiceConnecting = false;
        }
    }

    /*
     * Apps or keyguard provide the callback and VoiceCommandManager send the
     * messages to Apps or keyguard when receive messages from
     * VoiceCommandManagerService via IPC
     * 
     * @param listener callback from apps or keyguard
     * 
     * @return true if register successfully
     */
    @Override
    public void registerListener(VoiceCommandListener listener)
            throws IllegalAccessException, RemoteException {
        Log.d(TAG, "registerListener start!");
        if (listener == null) {
            Log.e(TAG, "RegisterListener contains the illeagal listener");
            throw new IllegalAccessException("Illeagal listener");
        }
        synchronized (mLock) {
            if (mVCmdMgrService == null && !isServiceConnecting) {
                bindVoiceService();
            }

            int errorid = VoiceCommandListener.VOICE_NO_ERROR;

            if (mVCmdMgrService != null && !isRegistered) {
                // Send the command directly
                try {
                    String pkgName = listener.getPkgName();
                    if (pkgName == null) {
                        pkgName = mAppContext.getPackageName();
                    }

                    Log.i(TAG, "RegisterListener " + pkgName);
                    errorid = mVCmdMgrService.registerListener(pkgName,
                            mCallback);
                    if (errorid == VoiceCommandListener.VOICE_NO_ERROR) {
                        isRegistered = true;
                        Log.d(TAG, "RegisterListener Success " + pkgName);
                    }
                    addListener(listener);
                } catch (RemoteException e) {
                    handleServiceDisconnected(true);
                    errorid = VoiceCommandListener.VOICE_ERROR_COMMON_SERVICE;
                }
            } else if (isServiceConnecting || isRegistered) {
                // Cache the command until get the service binder
                addListener(listener);
            } else {
                errorid = VoiceCommandListener.VOICE_ERROR_COMMON_SERVICE;
            }

            if (errorid != VoiceCommandListener.VOICE_NO_ERROR) {
                handleCommonError(errorid);
            }
        }
        Log.d(TAG, "registerListener end!");
    }

    /*
     * Apps or keyguard notify VoiceCommandManager remove the callback listener
     * 
     * @param listener callback from apps or keyguard
     */
    @Override
    public void unRegisterListener(VoiceCommandListener listener)
            throws IllegalAccessException, RemoteException {
        Log.d(TAG, "unRegisterListener start!");
        if (listener == null || mRegisterListeners.isEmpty()) {
            Log.e(TAG, "unRegisterListener contains the illeagal listener");
            throw new IllegalAccessException("Illeagal listener");
        }
        synchronized (mLock) {
            deleteListener(listener);
            if (mRegisterListeners.isEmpty()) {
                try {
                    if (mVCmdMgrService != null) {
                        String pkgName = listener.getPkgName();
                        if (pkgName == null) {
                            pkgName = mAppContext.getPackageName();
                        }
                        int errorid = mVCmdMgrService.unregisterListener(
                                pkgName, mCallback);
                        mAppContext.unbindService(mServiceConnection);
                        if (errorid != VoiceCommandListener.VOICE_NO_ERROR) {
                            handleCommonError(errorid);
                        }
                    }
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    mVCmdMgrService = null;
                    Log.e(TAG, "Unregister error in handler RemoteException = "
                            + e.getMessage());
                }
                handleServiceDisconnected(false);
            }
        }
        Log.d(TAG, "unRegisterListener end!");
    }

    /*
     * Send the message to VoiceCommandManagerService
     * 
     * @param context the Component who register the listener
     * 
     * @param mainAction check the column of 'Main Action' in the protocol table
     * 
     * @param subAction check the column of 'Sub Action' in the protocol table
     * 
     * @param extraData check the column of 'Extra Data Key & Value' in the
     * protocol table
     * 
     * @throws IllegalAccessException
     * 
     * @throws RemoteException
     */
    @Override
    public void sendCommand(Context context, int mainAction, int subAction,
            Bundle extraData) throws IllegalAccessException, RemoteException {
        Log.d(TAG, "sendCommand start!");
        synchronized (mLock) {
            Log.d(TAG, "Send Command " + "mainAction=" + mainAction
                    + " subAction=" + subAction + " extraData=" + extraData);
            if (mRegisterListeners.isEmpty()) {
                throw new IllegalAccessException("Register listener first!");
            } else if (isServiceConnecting) {
                // cache command until get binder from service
                mCommandCaches.add(new CacheCommand(mainAction, subAction,
                        extraData));
            } else {
                String pkgName = mRegisterListeners.get(
                        mRegisterListeners.size() - 1).getPkgName();
                if (pkgName == null) {
                    pkgName = mAppContext.getPackageName();
                }
                int errorid = mVCmdMgrService.sendCommand(pkgName, mainAction,
                        subAction, extraData);
                Log
                        .d(TAG, "Sent Command " + "mainAction=" + mainAction
                                + " subAction=" + subAction + " extraData="
                                + extraData);

                if (errorid != VoiceCommandListener.VOICE_NO_ERROR) {
                    handleCommonError(errorid);
                }
            }
        }
        Log.d(TAG, "sendCommand end!");
    }

    private void addListener(VoiceCommandListener listener) {
        if (!mRegisterListeners.contains(listener)) {
            mRegisterListeners.add(listener);
        }
    }

    private void deleteListener(VoiceCommandListener listener) {
        mRegisterListeners.remove(listener);
    }

    /*
     * Deal with the error from Service
     * 
     * @param errorid
     * 
     * @throws IllegalAccessException
     * 
     * @throws RemoteException
     */
    private void handleCommonError(int errorid) throws IllegalAccessException,
            RemoteException {
        switch (errorid) {
        case VoiceCommandListener.VOICE_ERROR_COMMON_REGISTERED:
            throw new IllegalAccessException("Duplicate Registration!");
        case VoiceCommandListener.VOICE_ERROR_COMMON_UNREGISTER:
            throw new IllegalAccessException("Register the process first!");
        case VoiceCommandListener.VOICE_ERROR_COMMON_SERVICE:
            throw new RemoteException("Can't find the service!");
        case VoiceCommandListener.VOICE_ERROR_COMMON_ILLEGALPROCESS:
            throw new IllegalAccessException(
                    "Process has no permission to access!");
        default:
            break;
        }
    }

    /*
     * Sometimes we need to reconnect the service , such as service process has
     * been killed in low memory or other case
     * 
     * @param isReconnect
     */
    private void handleServiceDisconnected(boolean isReconnect) {
        Log.e(TAG, "Service Disconnected");
        mVCmdMgrService = null;
        isRegistered = false;
        isServiceConnecting = false;

        if (isReconnect && !mRegisterListeners.isEmpty()) {
            isServiceConnecting = mAppContext.bindService(mVoiceServiceIntent,
                    mServiceConnection, Context.BIND_AUTO_CREATE);
            if (!isServiceConnecting) {
                // Notify User , we can't bind to service , service is in error;
                if (!mCommandCaches.isEmpty() && !mRegisterListeners.isEmpty()) {
                    CacheCommand command = mCommandCaches.get(0);
                    command.mExtraData.clear();
                    command.mExtraData.putInt(
                            VoiceCommandListener.ACTION_EXTRA_RESULT,
                            VoiceCommandListener.ACTION_EXTRA_RESULT_ERROR);
                    command.mExtraData.putInt(
                            VoiceCommandListener.ACTION_EXTRA_RESULT_INFO,
                            VoiceCommandListener.VOICE_ERROR_COMMON_SERVICE);
                    mRegisterListeners.get(mRegisterListeners.size() - 1)
                            .onVoiceCommandNotified(command.mMainAction,
                                    command.mSubAction, command.mExtraData);
                }

            }
        }

        if (!isServiceConnecting) {
            mCommandCaches.clear();
            mRegisterListeners.clear();
        }
    }

    /*
     * Used to save the command until register successfully
     */
    private class CacheCommand {
        int mMainAction;
        int mSubAction;
        Bundle mExtraData;

        CacheCommand(int mainaction, int subaction, Bundle extradata) {
            mMainAction = mainaction;
            mSubAction = subaction;
            mExtraData = extradata;
        }
    }

}

