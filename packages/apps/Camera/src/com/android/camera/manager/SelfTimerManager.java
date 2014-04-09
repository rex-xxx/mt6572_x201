/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.camera.manager;

import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.mediatek.xlog.Xlog;

public class SelfTimerManager {
    private static final String TAG = "SelfTimerManager";

    private static final int SELF_TIMER_VOLUME = 100;
    private static final int SELF_TIMER_INTERVAL = 250;
    private static final int SELF_TIMER_SHORT_BOUND = 2000;
    private static final int MAX_DELEY_TIME = 10000; // sec

    // self timer related fields
    private static final int STATE_SELF_TIMER_IDLE = 0;
    private static final int STATE_SELF_TIMER_COUNTING = 1;
    private static final int STATE_SELF_TIMER_SNAP = 2;
    private static final int MSG_SELFTIMER_TIMEOUT = 9;

    private final Handler mHandler;
    private static Looper sLooper;
    private int mSelfTimerDuration;
    private int mSelfTimerState;
    private long mTimeSelfTimerStart;
    private boolean mLowStorageTag = false;
    private ToneGenerator mSelfTimerTone;

    private static SelfTimerManager sSelfTimerManager;
    private SelfTimerListener mSelfTimerListener;

    public interface SelfTimerListener {
        void onTimerStart();

        void onTimerTimeout();

        void onTimerStop();
    }

    private SelfTimerManager(Looper looper) {
        sLooper = looper;
        mHandler = new Handler(looper) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                case MSG_SELFTIMER_TIMEOUT:
                    selfTimerTimeout();
                    break;
                default:
                    break;
                }
            }
        };
        initSelfTimerTone();
    }

    public static synchronized SelfTimerManager getInstance(Looper looper) {
        if (sSelfTimerManager == null || sLooper != looper) {
            sSelfTimerManager = new SelfTimerManager(looper);
        }
        return sSelfTimerManager;
    }

    public static void release() {
        sLooper = null;
        if (sSelfTimerManager != null) {
            sSelfTimerManager.mHandler.removeCallbacksAndMessages(null);
            sSelfTimerManager = null;
        }
    }

    public void setTimerListener(SelfTimerListener listener) {
        mSelfTimerListener = listener;
    }

    public boolean clerSelfTimerState() {
        if (mSelfTimerDuration != 0) {
            if (mSelfTimerState == STATE_SELF_TIMER_COUNTING) {
                selfTimerStop();
            }
            return true;
        }
        return false;
    }

    private synchronized void selfTimerStop() {
        if (mSelfTimerState == STATE_SELF_TIMER_IDLE) {
            return;
        }
        Xlog.i(TAG, "selfTimerStop");
        mSelfTimerState = STATE_SELF_TIMER_IDLE;
        if (mSelfTimerListener != null) {
            mSelfTimerListener.onTimerStop();
            // cancelAutoFocus(); // move
        }
        mHandler.removeMessages(MSG_SELFTIMER_TIMEOUT);
    }

    private void initSelfTimerTone() {
        try {
            mSelfTimerTone = new ToneGenerator(AudioManager.STREAM_SYSTEM, SELF_TIMER_VOLUME);
        } catch (OutOfMemoryError e) {
            Xlog.w(TAG, "Exception caught while creating tone generator: ", e);
            mSelfTimerTone = null;
        } /*catch (RuntimeException ex) {
            Xlog.w(TAG, "Exception caught while creating tone generator: ", ex);
            mSelfTimerTone = null;
        }*/
    }

    private synchronized void selfTimerStart() {
        if (mSelfTimerState != STATE_SELF_TIMER_IDLE || mHandler.hasMessages(MSG_SELFTIMER_TIMEOUT) || mLowStorageTag) {
            return;
        }
        mTimeSelfTimerStart = System.currentTimeMillis();
        mSelfTimerState = STATE_SELF_TIMER_COUNTING;
        selfTimerTimeout();
    }

    private synchronized void selfTimerTimeout() {
        long msDelay;
        long msDelta = System.currentTimeMillis() - mTimeSelfTimerStart;
        long msTimeLeft;

        msTimeLeft = (mSelfTimerDuration > msDelta) ? mSelfTimerDuration - msDelta : 0;
        // Xlog.d(TAG, "[SelfTimer]selfTimerTimeout(), " + msTimeLeft + "ms left");

        if (msTimeLeft >= SELF_TIMER_SHORT_BOUND) {
            msDelay = msTimeLeft - SELF_TIMER_SHORT_BOUND;
        } else if (msTimeLeft != 0) {
            msDelay = SELF_TIMER_INTERVAL;
        } else { /* timeout */
            mSelfTimerState = STATE_SELF_TIMER_SNAP;
            if (mSelfTimerListener != null) {
                mSelfTimerListener.onTimerTimeout();
            }
            mSelfTimerState = STATE_SELF_TIMER_IDLE;
            return;
        }
        mHandler.sendEmptyMessageDelayed(MSG_SELFTIMER_TIMEOUT, msDelay);
        if (mSelfTimerTone != null) {
            mSelfTimerTone.startTone(ToneGenerator.TONE_DTMF_9, 100);
        }
    }

    public synchronized void breakTimer() {
        if (mSelfTimerState != STATE_SELF_TIMER_IDLE) {
            mHandler.removeMessages(MSG_SELFTIMER_TIMEOUT);
            mSelfTimerState = STATE_SELF_TIMER_IDLE;
            if (mSelfTimerListener != null) {
                mSelfTimerListener.onTimerStop();
            }
        }
    }

    public void setLowStorage(boolean storage) {
        mLowStorageTag = storage;
        breakTimer();
    }

    public void setSelfTimerDuration(String timeDelay) {
        int delay = Integer.valueOf(timeDelay);
        if (delay < 0 || delay > MAX_DELEY_TIME) {
            throw new RuntimeException("invalid self timer delay");
        }
        mSelfTimerDuration = delay;
    }

    public boolean checkSelfTimerMode() {
        if (mSelfTimerDuration > 0 && mSelfTimerState == STATE_SELF_TIMER_IDLE) {
            selfTimerStart();
            return true;
        } else if (mSelfTimerState == STATE_SELF_TIMER_COUNTING) {
            return true;
        }
        return false;
    }

    public boolean isSelfTimerEnabled() {
        return mSelfTimerDuration > 0;
    }

    public boolean isSelfTimerCounting() {
        return mSelfTimerState == STATE_SELF_TIMER_COUNTING;
    }

    public void releaseTone() {
        if (mSelfTimerTone != null) {
            mSelfTimerTone.release();
        }
    }
}
