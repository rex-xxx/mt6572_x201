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

/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.phone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.IPowerManager;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.SystemVibrator;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;

import com.android.internal.telephony.Phone;
import com.mediatek.phone.PhoneFeatureConstants;

/**
 * Ringer manager for the Phone app.
 */
public class Ringer {
    private static final String LOG_TAG = "Ringer";
/*    private static final boolean DBG =
            (PhoneGlobals.DBG_LEVEL >= 1) && (SystemProperties.getInt("ro.debuggable", 0) == 1); */
    private static final boolean DBG = true;

    private static final int PLAY_RING_ONCE = 1;
    private static final int STOP_RING = 3;

    private static final int VIBRATE_LENGTH = 1000; // ms
    private static final int PAUSE_LENGTH = 1000; // ms

    /** The singleton instance. */
    private static Ringer sInstance;

//MTK add below line:
    public static final String VBIRT_MODE_CHANGE_ACTION = "vbirtchange";

    private static boolean sIsRingingAndVolumnZero = false;

    // Uri for the ringtone.
    Uri mCustomRingtoneUri = Settings.System.DEFAULT_RINGTONE_URI;

    Ringtone mRingtone;
    Vibrator mVibrator;
    IPowerManager mPowerManager;
    volatile boolean mContinueVibrating;
    VibratorThread mVibratorThread;
    Context mContext;
    private Worker mRingThread;
    private Handler mRingHandler;
    private long mFirstRingEventTime = -1;
    private long mFirstRingStartTime = -1;

    private boolean mMute = false;

    /**
     * Initialize the singleton Ringer instance.
     * This is only done once, at startup, from PhoneGlobals.onCreate().
     */
    /* package */ static Ringer init(Context context) {
        synchronized (Ringer.class) {
            if (sInstance == null) {
                sInstance = new Ringer(context);
            } else {
                Log.wtf(LOG_TAG, "init() called multiple times!  sInstance = " + sInstance);
            }
            return sInstance;
        }
    }

    /** Private constructor; @see init() */
    private Ringer(Context context) {
        mContext = context;
        mPowerManager = IPowerManager.Stub.asInterface(ServiceManager.getService(Context.POWER_SERVICE));
//MTK begin:
        IntentFilter filter = new IntentFilter();
        filter.addAction(VBIRT_MODE_CHANGE_ACTION);
        // We don't rely on getSystemService(Context.VIBRATOR_SERVICE) to make sure this
        // vibrator object will be isolated from others.
        mVibrator = new SystemVibrator();
        mContext.registerReceiver(mVbirtStateChangeReceiver, filter);
//MTK end
    }
//MTK begin:
    protected void finalize() {
        try {
            mContext.unregisterReceiver(mVbirtStateChangeReceiver);
        } catch (IllegalArgumentException e) {
            log("IllegalArgumentException happened in finalized().");
        }
    }

    private class VbirtStateChangeReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(VBIRT_MODE_CHANGE_ACTION)) {
                Log.i("VbirtStateChangeReceiver", "onReceive");
                    if (mVibratorThread != null) {
                        mContinueVibrating = false;
                        mVibratorThread = null;
                    }
            }
        }

    }

    private VbirtStateChangeReceiver mVbirtStateChangeReceiver = new VbirtStateChangeReceiver();
//MTK end
    /**
     * After a radio technology change, e.g. from CDMA to GSM or vice versa,
     * the Context of the Ringer has to be updated. This is done by that function.
     *
     * @parameter Phone, the new active phone for the appropriate radio
     * technology
     */
    void updateRingerContextAfterRadioTechnologyChange(Phone phone) {
        if(DBG) Log.d(LOG_TAG, "updateRingerContextAfterRadioTechnologyChange...");
        mContext = phone.getContext();
    }

    /**
     * @return true if we're playing a ringtone and/or vibrating
     *     to indicate that there's an incoming call.
     *     ("Ringing" here is used in the general sense.  If you literally
     *     need to know if we're playing a ringtone or vibrating, use
     *     isRingtonePlaying() or isVibrating() instead.)
     *
     * @see isVibrating
     * @see isRingtonePlaying
     */
    public boolean isRinging() {
        synchronized (this) {
            return (isRingtonePlaying() || isVibrating());
        }
    }

    /**
     * @return true if the ringtone is playing
     * @see isVibrating
     * @see isRinging
     */
    private boolean isRingtonePlaying() {
        synchronized (this) {
            return (mRingtone != null && mRingtone.isPlaying()) ||
                    (mRingHandler != null && mRingHandler.hasMessages(PLAY_RING_ONCE));
        }
    }

    /**
     * @return true if we're vibrating in response to an incoming call
     * @see isVibrating
     * @see isRinging
     */
    private boolean isVibrating() {
        synchronized (this) {
            return (mVibratorThread != null);
        }
    }

    void setMute(boolean mute) {
        mMute = mute;
    }
    
    /**
     * Starts the ringtone and/or vibrator
     */
    void ring() {
        if (DBG) log("ring()...");

        if (this.mCustomRingtoneUri == null) {
            //return ;
            log("ring()... with null uri");
        }

        if (mMute) {
            log("mute, bail out...");
            return;
        }

        synchronized (this) {
            try {
                if (PhoneGlobals.getInstance().showBluetoothIndication()) {
                    mPowerManager.setAttentionLight(true, 0x000000ff);
                } else {
                    mPowerManager.setAttentionLight(true, 0x00ffffff);
                }
            } catch (RemoteException ex) {
                // the other end of this binder call is in the system process.
            }
//MTK begin:
            if (PhoneUtils.getAudioControlState() == PhoneUtils.AUDIO_OFFHOOK) {
                // if Audio Control State is OFFHOOK, do not ring.
                return;
            }
//MTK end

            if (shouldVibrate() && mVibratorThread == null) {
                mContinueVibrating = true;
                mVibratorThread = new VibratorThread();
                if (DBG) log("- starting vibrator...");
                mVibratorThread.start();
            }
            AudioManager audioManager =
                    (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);

            if (audioManager.getStreamVolume(AudioManager.STREAM_RING) == 0) {
                if (DBG) log("skipping ring because volume is zero");
//MTK add below one line:
                PhoneUtils.setAudioMode();
                sIsRingingAndVolumnZero = true;
                //PhoneUtils.setAudioMode(mContext, AudioManager.MODE_RINGTONE);
                return;
            }

            makeLooper();
            if (mFirstRingEventTime < 0) {
                mFirstRingEventTime = SystemClock.elapsedRealtime();
                if (mRingHandler != null) {
                    mRingHandler.sendEmptyMessage(PLAY_RING_ONCE);
                }
            } else {
                // For repeat rings, figure out by how much to delay
                // the ring so that it happens the correct amount of
                // time after the previous ring
                if (mFirstRingStartTime > 0) {
                    // Delay subsequent rings by the delta between event
                    // and play time of the first ring
                    if (DBG) {
                        log("delaying ring by " + (mFirstRingStartTime - mFirstRingEventTime));
                    }
                    
                    if (mRingHandler != null) {
                        mRingHandler.sendEmptyMessageDelayed(PLAY_RING_ONCE,
                            mFirstRingStartTime - mFirstRingEventTime);
                    }
                } else {
                    // We've gotten two ring events so far, but the ring
                    // still hasn't started. Reset the event time to the
                    // time of this event to maintain correct spacing.
                    mFirstRingEventTime = SystemClock.elapsedRealtime();
                }
            }
        }
    }

    boolean shouldVibrate() {
        final AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);

        if (PhoneFeatureConstants.FeatureOption.MTK_AUDIO_PROFILES) {
            return audioManager.shouldVibrate(AudioManager.VIBRATE_TYPE_RINGER);
        } else {
            final int ringerMode = audioManager.getRingerMode();
            if (CallFeaturesSetting.getVibrateWhenRinging(mContext)) {
                return ringerMode != AudioManager.RINGER_MODE_SILENT;
            } else {
                return ringerMode == AudioManager.RINGER_MODE_VIBRATE;
            }
        }
    }

    /**
     * Stops the ringtone and/or vibrator if any of these are actually
     * ringing/vibrating.
     */
    void stopRing() {
        synchronized (this) {
            if (DBG) log("stopRing()...");

            try {
                mPowerManager.setAttentionLight(false, 0x00000000);
            } catch (RemoteException ex) {
                // the other end of this binder call is in the system process.
            }

            if (mRingHandler != null) {
                mRingHandler.removeCallbacksAndMessages(null);
                Message msg = mRingHandler.obtainMessage(STOP_RING);
                msg.obj = mRingtone;
                mRingHandler.sendMessage(msg);
                PhoneUtils.setAudioMode();
                mRingThread = null;
                mRingHandler = null;
                mRingtone = null;
                mFirstRingEventTime = -1;
                mFirstRingStartTime = -1;
            } else {
                if (mRingThread != null) {
                    mRingThread = null;
                }
                if (DBG) log("- stopRing: null mRingHandler!");
            }

            if (mVibratorThread != null) {
                if (DBG) log("- stopRing: cleaning up vibrator thread...");
                mContinueVibrating = false;
                mVibratorThread.mStop = true;
                mVibratorThread = null;
//MTK begin:
            } else if (sIsRingingAndVolumnZero) {
                //PhoneUtils.setAudioMode(mContext, AudioManager.MODE_NORMAL);
                sIsRingingAndVolumnZero = false;
                PhoneUtils.setAudioMode();
//MTK end
            }
            // Also immediately cancel any vibration in progress.
            mVibrator.cancel();

            //this.mCustomRingtoneUri = null;
        }
    }

    private class VibratorThread extends Thread {
        boolean mStop = false;
        public void run() {
            while (mContinueVibrating && !mStop) {
                mVibrator.vibrate(VIBRATE_LENGTH);
                SystemClock.sleep(VIBRATE_LENGTH + PAUSE_LENGTH);
            }
        }
    }
    private class Worker implements Runnable {
        private final Object mLock = new Object();
        private Looper mLooper;

        Worker(String name) {
            Thread t = new Thread(null, this, name);
            t.start();
            synchronized (mLock) {
                if (DBG) {
                    Log.d(LOG_TAG, "Worker's constructor enter:");
                }
                while (mLooper == null) {
                    if (DBG) {
                        Log.d(LOG_TAG, "Worker waiting for looper ready!");
                    }
                    try {
                        mLock.wait(5000);
                    } catch (InterruptedException ex) {
                    }
                }
                if (DBG) {
                    Log.d(LOG_TAG, "Worker's run exit with looper = " + mLooper);
                }
            }
        }

        public Looper getLooper() {
            return mLooper;
        }

        public void run() {
            synchronized (mLock) {
                if (DBG) {
                    Log.d(LOG_TAG, "Worker's run enter:");
                }
                Looper.prepare();
                mLooper = Looper.myLooper();
                if (DBG) {
                    Log.d(LOG_TAG, "Worker has got the looper, notify");
                }
                mLock.notifyAll();
            }
            Looper.loop();
        }

        public void quit() {
            mLooper.quit();
        }
    }

    /**
     * Sets the ringtone uri in preparation for ringtone creation
     * in makeLooper().  This uri is defaulted to the phone-wide
     * default ringtone.
     */
    void setCustomRingtoneUri (Uri uri) {
        /// M: if the uri is not aviable(ex, ALPS00414350), use default Ringtone @{
        // if (uri != null) {
        // log("setCustomRingtoneUri = " + uri);
        // mCustomRingtoneUri = uri;
        // }

        if (RingtoneManager.isRingtoneExist(mContext, uri)) {
            mCustomRingtoneUri = uri;
            log("setCustomRingtoneUri = " + uri);
        } else {
            mCustomRingtoneUri = Settings.System.DEFAULT_RINGTONE_URI;
            log("CustomRingtoneUri = " + uri + ", not available, use default ringtone!");
        }
        /// @}
    }

    Uri getCustomRingToneUri() {
        return mCustomRingtoneUri;
    }

    private void makeLooper() {
        if (mRingThread == null) {
            mRingThread = new Worker("ringer");
            if (mRingThread.getLooper() == null) {
                return ;
            }
            mRingHandler = new Handler(mRingThread.getLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    Ringtone r = null;
                    switch (msg.what) {
                        case PLAY_RING_ONCE:
                            if (DBG) log("mRingHandler: PLAY_RING_ONCE...");
                            if (mRingtone == null && !hasMessages(STOP_RING)) {
                                // create the ringtone with the uri
                                if (DBG) log("creating ringtone: " + mCustomRingtoneUri);
                                r = RingtoneManager.getRingtone(mContext, mCustomRingtoneUri);
                                synchronized (Ringer.this) {
                                    if (!hasMessages(STOP_RING)) {
                                        mRingtone = r;
                                    }
                                }
                            }
                            r = mRingtone;
                            if (r != null && !hasMessages(STOP_RING) && !r.isPlaying()) {
                                if (DBG) {
                                    log("play ringtone... ");
                                }
                                PhoneUtils.setAudioMode();
                                r.play();
                                synchronized (Ringer.this) {
                                    if (mFirstRingStartTime < 0) {
                                        mFirstRingStartTime = SystemClock.elapsedRealtime();
                                    }
                                }
                            }
                            break;
                        case STOP_RING:
                            if (DBG) log("mRingHandler: STOP_RING...");
                            r = (Ringtone) msg.obj;
                            if (r != null) {
                                r.stop();
                            } else {
                                if (DBG) log("- STOP_RING with null ringtone!  msg = " + msg);
                            }
                            getLooper().quit();
                            break;
                    }
                }
            };
        }
    }

    private static void log(String msg) {
        Log.d(LOG_TAG, msg);
    }
}
