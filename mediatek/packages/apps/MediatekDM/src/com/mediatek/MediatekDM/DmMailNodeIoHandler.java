/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 * 
 * MediaTek Inc. (C) 2010. All rights reserved.
 * 
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package com.mediatek.MediatekDM;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.Log;

import com.mediatek.MediatekDM.DmConst.TAG;
import com.mediatek.MediatekDM.mdm.MdmException;
import com.mediatek.MediatekDM.mdm.NodeIoHandler;

public class DmMailNodeIoHandler implements NodeIoHandler {

    protected Context mContext = null;
    protected Uri uri = null;
    protected String mccmnc = null;
    protected String recordToWrite = null;
    private boolean isDataReady = true;
    private String pushMailStr;
    private String itemToRead;
    private final Object mLock = new Object();
    private final IntentFilter filter = new IntentFilter("android.intent.action.PUSHMAIL_PROFILE");
    private pushMailReceiver mReceiver = new pushMailReceiver();

    private String[] item = {
            "apn", "smtp_server", "smtp_port", "smtp_ssl", "pop3_server", "pop3_port", "pop3_ssl",
            "recv_protocol"
    };

    static String[] mContent = new String[8];
    static String[] mSetArr = new String[8];

    public DmMailNodeIoHandler(Context ctx, Uri treeUri, String mccMnc) {
        Log.i(TAG.NodeIOHandler, "Mail constructed");

        mContext = ctx;
        uri = treeUri;
        mccmnc = mccMnc;
    }

    public int read(int arg0, byte[] arg1) throws MdmException {
        String recordToRead = null;
        String uriPath = uri.getPath();
        Log.i(TAG.NodeIOHandler, "uri: " + uriPath);
        Log.i(TAG.NodeIOHandler, "arg0: " + arg0);

        if (DmService.mCCStoredParams.containsKey(uriPath)) {
            recordToRead = DmService.mCCStoredParams.get(uriPath);
            Log.d(TAG.NodeIOHandler, "get valueToRead from mCCStoredParams, the value is "
                    + recordToRead);
        } else {
            recordToRead = new String();
            for (int i = 0; i < getItem().length; i++) {
                if (uri.getPath().contains(getItem()[i])) {
                    if (mContent[i] != null) {
                        recordToRead = mContent[i];
                    } else {
                        itemToRead = getItem()[i];
                        HandlerThread thread = new HandlerThread("pushmail");
                        thread.start();
                        mContext.registerReceiver(mReceiver, filter, null,
                                new Handler(thread.getLooper()));
                        Intent intent = new Intent();
                        intent.setAction("android.intent.action.PUSHMAIL_GET_PROFILE");
                        mContext.sendBroadcast(intent);
                        Log.i(TAG.NodeIOHandler,
                                "[MailNode] send broadcast intent 'PUSHMAIL_GET_PROFILE' ====>");

                        Log.i(TAG.NodeIOHandler,
                                "[MailNode] blocking here to wait for intent 'PUSHMAIL_PROFILE'...");
                        synchronized (mLock) {
                            while (isDataReady) {
                                try {
                                    // FIXME!!!
                                    // CC procedure will hang if the intent has
                                    // no response.
                                    // Do we need set a timeout here?
                                    mLock.wait();
                                    Log.i(TAG.NodeIOHandler,
                                            "[MailNode] skip waiting when got intent back");
                                    break;
                                } catch (InterruptedException e) {
                                    Log.e(TAG.NodeIOHandler, "[MailNode] waiting interrupted.");
                                }
                            }
                        }
                        isDataReady = true;
                        recordToRead = mContent[i];
                        mContext.unregisterReceiver(mReceiver);
                    }
                }
            }
            DmService.mCCStoredParams.put(uriPath, recordToRead);
            Log.d(TAG.NodeIOHandler, "put valueToRead to mCCStoredParams, the value is "
                    + recordToRead);
        }
        if (TextUtils.isEmpty(recordToRead)) {
            return 0;
        } else {
            byte[] temp = recordToRead.getBytes();
            if (arg1 == null) {
                return temp.length;
            }
            int numberRead = 0;
            for (; numberRead < arg1.length - arg0; numberRead++) {
                if (numberRead < temp.length) {
                    arg1[numberRead] = temp[arg0 + numberRead];
                } else {
                    break;
                }
            }
            if (numberRead < arg1.length - arg0) {
                recordToRead = null;
            } else if (numberRead < temp.length) {
                recordToRead = recordToRead.substring(arg1.length - arg0);
            }
            return numberRead;
        }
    }

    public void write(int arg0, byte[] arg1, int arg2) throws MdmException {

        Log.i(TAG.NodeIOHandler, "uri: " + uri.getPath());
        Log.i(TAG.NodeIOHandler, "arg1: " + new String(arg1));
        Log.i(TAG.NodeIOHandler, "arg0: " + arg0);
        Log.i(TAG.NodeIOHandler, "arg2: " + arg2);

        if (recordToWrite == null) {
            recordToWrite = new String();
        }
        recordToWrite += new String(arg1);
        if (recordToWrite.length() == arg2) {
            for (int i = 0; i < getItem().length; i++) {
                if (uri.getPath().contains(getItem()[i])) {
                    mSetArr[i] = recordToWrite;

                    boolean needToBroadcast = true;
                    for (String s : mSetArr) {
                        if (s == null) {
                            needToBroadcast = false;
                        }
                    }

                    if (needToBroadcast) {

                        Intent intent = new Intent();
                        intent.setAction("android.intent.action.PUSHMAIL_SET_PROFILE");
                        for (int k = 0; k < mSetArr.length; k++) {
                            intent.putExtra(getItem()[k], mSetArr[k]);
                            Log.i(TAG.NodeIOHandler, getItem()[k] + ": " + mSetArr[k]);
                        }

                        mContext.sendBroadcast(intent);
                        recordToWrite = null;
                        for (int j = 0; j < mContent.length; j++) {
                            mContent[j] = null;
                            mSetArr[j] = null;
                        }
                    }
                    break;
                }
            }
        }
    }

    protected String[] getItem() {
        return item;
    };

    class pushMailReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String intentAction = intent.getAction();
            if (intentAction.equals("android.intent.action.PUSHMAIL_PROFILE")) {
                Log.i(TAG.NodeIOHandler,
                        "[MailNode] received broadcast intent 'PUSHMAIL_PROFILE' <====");
                for (int i = 0; i < mContent.length; i++) {
                    mContent[i] = intent.getStringExtra(item[i]);
                    Log.i(TAG.NodeIOHandler, item[i] + ":" + mContent[i]);
                }
                isDataReady = true;
                synchronized (mLock) {
                    mLock.notify();
                    Log.i(TAG.NodeIOHandler, "[MailNode] notifying the wait lock.");
                }
            }
        }

    }
}
