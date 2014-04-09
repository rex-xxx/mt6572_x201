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

package com.mediatek.phone.recording;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class PhoneRecorderServices extends Service {

    private static final String LOG_TAG = "RecorderServices";
    private static final String PHONE_VOICE_RECORD_STATE_CHANGE_MESSAGE = "com.android.phone.VoiceRecorder.STATE";
    private PhoneRecorder mPhoneRecorder;
    private boolean mMount = true;
    IPhoneRecordStateListener mStateListener;

    public IBinder onBind(Intent intent) {
        Log.d(LOG_TAG, "onBind");
        return mBinder;
    }

    public boolean onUnbind(Intent intent) {
        Log.d(LOG_TAG, "onUnbind");
        return super.onUnbind(intent);
    }

    public void onCreate() {
        super.onCreate();
        log("onCreate");
        mPhoneRecorder = PhoneRecorder.getInstance(this);

        if (null != mPhoneRecorder) {
            mPhoneRecorder.setOnStateChangedListener(mPhoneRecorderStateListener);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        log("onDestroy");
        if (null != mPhoneRecorder) {
            mPhoneRecorder.stopRecord(mMount);
            mPhoneRecorder = null;
        }
        /*if (null != mBroadcastReceiver) {
            unregisterReceiver(mBroadcastReceiver);
            mBroadcastReceiver = null;
        }*/
    }

    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        log("onStart");
        if (null != mPhoneRecorder) {
            mPhoneRecorder.startRecord();
        }
    }

    public void log(String msg) {
        Log.d(LOG_TAG, msg);
    }

    private int mPhoneRecorderStatus = PhoneRecorder.IDLE_STATE;
    private PhoneRecorder.OnStateChangedListener mPhoneRecorderStateListener = new PhoneRecorder.OnStateChangedListener() {
        public void onStateChanged(int state) {
            int iPreviousStatus = PhoneRecorderServices.this.mPhoneRecorderStatus;
            PhoneRecorderServices.this.mPhoneRecorderStatus = state;
            if ((iPreviousStatus != state)) {
                Intent broadcastIntent = new Intent(PHONE_VOICE_RECORD_STATE_CHANGE_MESSAGE);
                broadcastIntent.putExtra("state", state);
                sendBroadcast(broadcastIntent);
                if (null != mStateListener) {
                    try {
                        log("onStateChanged");
                        mStateListener.onStateChange(state);
                    } catch (RemoteException e) {
                        Log.e(LOG_TAG, "PhoneRecordService: call listener onStateChange failed",
                                new IllegalStateException());
                    }
                }
            }
        }

        public void onError(int error) {
            if (null != mStateListener) {
                try {
                    log("onError");
                    mStateListener.onError(error);
                } catch (RemoteException e) {
                    Log.e(LOG_TAG, "PhoneRecordService: call listener onError() failed",
                            new IllegalStateException());
                }
            }
        }
    };

    private final IPhoneRecorder.Stub mBinder = new IPhoneRecorder.Stub() {
        public void listen(IPhoneRecordStateListener callback) {
            log("listen");
            if (null != callback) {
                mStateListener = callback;
            }
        }

        public void remove() {
            log("remove");
            mStateListener = null;
        }

        public void startRecord() {
            log("startRecord");
            if (null != mPhoneRecorder) {
                mPhoneRecorder.startRecord();
            }
        }

        public void stopRecord(boolean isMounted) {
            log("stopRecord");
            if (null != mPhoneRecorder) {
                mPhoneRecorder.stopRecord(isMounted);
            }
            mPhoneRecorder = null;
        }
    };
}
