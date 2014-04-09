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

/*
 * Copyright (C) 2007-2008 Esmertec AG.
 * Copyright (C) 2007-2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.mms.transaction;

import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.provider.Telephony;
import android.util.Log;
import android.widget.Toast;
import android.telephony.SmsCbMessage;

import com.android.mms.LogTag;
import com.android.mms.MmsApp;
import com.android.mms.R;
import com.android.internal.telephony.PhoneConstants;
import com.mediatek.encapsulation.com.mediatek.common.featureoption.EncapsulatedFeatureOption;
import com.mediatek.encapsulation.MmsLog;
import com.mediatek.encapsulation.android.telephony.EncapsulatedTelephony;
import com.mediatek.encapsulation.android.telephony.EncapsulatedTelephony.SIMInfo;


/**
 * M:
 * This service essentially plays the role of a "worker thread", allowing us to store
 * incoming messages to the database, update notifications, etc. without blocking the
 * main thread that SmsReceiver runs on.
 */
public class CBMessageReceiverService extends Service {
    private static final String TAG = "CBMessageReceiverService";

    private ServiceHandler mServiceHandler;
    private Looper mServiceLooper;

    private static final Uri MESSAGE_URI = EncapsulatedTelephony.SmsCb.CONTENT_URI;
    private static final int DEFAULT_SIM_ID = 1;
    
    public Handler mToastHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Toast.makeText(CBMessageReceiverService.this, getString(R.string.message_queued),
                    Toast.LENGTH_SHORT).show();
        }
    };

    // This must match SEND_PROJECTION.
    private int mResultCode;

    @Override
    public void onCreate() {
        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.
        HandlerThread thread = new HandlerThread(TAG, Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        mServiceLooper = thread.getLooper();
        if (null != mServiceLooper) {
            mServiceHandler = new ServiceHandler(mServiceLooper);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mResultCode = intent != null ? intent.getIntExtra("result", 0) : 0;

        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        msg.obj = intent;
        mServiceHandler.sendMessage(msg);
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        mServiceLooper.quit();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        /**
         * Handle incoming transaction requests.
         * The incoming requests are initiated by the MMSC Server or by the MMS Client itself.
         */
        @Override
        public void handleMessage(Message msg) {
            int serviceId = msg.arg1;
            Intent intent = (Intent)msg.obj;
            if (intent != null) {
                String action = intent.getAction();
                // NEED Replace with CB ACTION
                if (Telephony.Sms.Intents.SMS_CB_RECEIVED_ACTION.equals(action)) {
                    handleCBMessageReceived(intent);
                }
            }
            // NOTE: We MUST not call stopSelf() directly, since we need to
            // make sure the wake lock acquired by AlertReceiver is released.
            CBMessageReceiver.finishStartingService(CBMessageReceiverService.this, serviceId);
        }
    }

    private void handleCBMessageReceived(Intent intent) {
        // TODO need replace with cb message.
        Bundle extras = intent.getExtras();
        if (null == extras) {
            MmsLog.e(MmsApp.TXN_TAG, "Intents.getMessagesFromIntent return null !!");
            return;
        }
        SmsCbMessage message = (SmsCbMessage) extras.get("message");
        if (null == message) {
            MmsLog.e(MmsApp.TXN_TAG, "received SMS_CB_RECEIVED_ACTION with no extras!");
            return;
        }

        int simId = -1;
        int slotId = intent.getIntExtra(PhoneConstants.GEMINI_SIM_ID_KEY, -1);
        SIMInfo si = SIMInfo.getSIMInfoBySlot(getApplicationContext(), slotId);
        if (null != si) {
            simId = (int)si.getSimId();
        } else {
            MmsLog.w(MmsApp.TXN_TAG, "handleCBMessageReceived:SIMInfo is null for slot " + slotId);
        }

        Uri messageUri = insertMessage(this, message, simId);
        if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE)) {
            Log.v(TAG, "handleSmsReceived" +
                    " messageUri: " + messageUri +
                    ", body: " + message.getMessageBody());
        }

        if (messageUri != null) {
            if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                CBMessagingNotification.updateNewMessageIndicatorGemini(this, true, simId);
            } else {
                CBMessagingNotification.updateNewMessageIndicator(this, true);
            }
        }
    }

    public static final String CLASS_ZERO_BODY_KEY = "CLASS_ZERO_BODY";

    private static final int REPLACE_COLUMN_ID = 0;

    /**
     * If the message is a class-zero message, display it immediately
     * and return null.  Otherwise, store it using the
     * <code>ContentResolver</code> and return the
     * <code>Uri</code> of the thread containing this message
     * so that we can use it for notification.
     */
    // TODO Need replace with CBMessage
    private Uri insertMessage(Context context, SmsCbMessage msg, int simId) {
        return storeCBMessage(context, msg, simId);
    }

    // TODO Need replace with CB message
    private Uri storeCBMessage(Context context, SmsCbMessage msg, int simId) {
        // Store the message in the content provider.
        String body = msg.getMessageBody();
        ContentResolver resolver = context.getContentResolver();
        ContentValues values = getCBContentValue(msg, body, simId);
        MmsLog.d(MmsApp.TXN_TAG, "CB message body: " + body);
        return resolver.insert(MESSAGE_URI, values);
    }

    // TODO  Need replace with CB Message
    private ContentValues getCBContentValue(SmsCbMessage msg, String body, int simId) {
        ContentValues values = new ContentValues();
        // TODO just use default SIM ID, need improve when two sim cards.
        if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
            values.put(EncapsulatedTelephony.SmsCb.SIM_ID, simId);
        }
        values.put(EncapsulatedTelephony.SmsCb.DATE, Long.valueOf(System.currentTimeMillis()));
        // Channel ID is getting from getMessageID
        values.put(EncapsulatedTelephony.SmsCb.CHANNEL_ID, msg.getServiceCategory());
        values.put(EncapsulatedTelephony.SmsCb.READ, Integer.valueOf(0));
        values.put(EncapsulatedTelephony.SmsCb.BODY, body);
        return values;  
    }
}
