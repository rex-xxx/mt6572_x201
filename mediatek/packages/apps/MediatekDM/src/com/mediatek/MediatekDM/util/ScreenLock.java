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

package com.mediatek.MediatekDM.util;

import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.content.Context;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

public class ScreenLock {
    private static WakeLock mFullWakelock = null;
    private static WakeLock mPartialWakelock = null;
    private static KeyguardLock mKeyguardLock = null;
    private static String TAG = "DM/ScreenLock";

    public static void acquirePartialWakelock(Context context) {
        // get a PowerManager instance
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (mPartialWakelock == null) {
            // get WakeLock
            mPartialWakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "dm_PartialLock");
            if (!mPartialWakelock.isHeld()) {
                Log.d(TAG, "need to aquire partial wake up");
                // wake lock
                mPartialWakelock.acquire();
            } else {
                mPartialWakelock = null;
                Log.d(TAG, "not need to aquire partial wake up");
            }
        }
    }

    public static void acquireFullWakelock(Context context) {
        // get a PowerManager instance
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

        if (mFullWakelock == null) {
            // get WakeLock
            mFullWakelock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK
                    | PowerManager.ACQUIRE_CAUSES_WAKEUP
                    | PowerManager.ON_AFTER_RELEASE, "dm_FullLock");
            if (!mFullWakelock.isHeld()) {
                Log.d(TAG, "need to aquire full wake up");
                // wake lock
                mFullWakelock.acquire();
            } else {
                mFullWakelock = null;
                Log.d(TAG, "not need to aquire full wake up");
            }
        }
    }

    public static void disableKeyguard(Context context) {
        // get a KeyguardManager instance
        KeyguardManager km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);

        if (mKeyguardLock == null) {
            // get KeyguardLock
            mKeyguardLock = km.newKeyguardLock("dm_KL");
            if (km.inKeyguardRestrictedInputMode()) {
                Log.d(TAG, "need to disableKeyguard");
                // release key guard lock
                mKeyguardLock.disableKeyguard();
            } else {
                mKeyguardLock = null;
                Log.d(TAG, "not need to disableKeyguard");
            }
        }
    }

    public static void releaseFullWakeLock(Context context) {
        if (mFullWakelock != null) {
            if (mFullWakelock.isHeld()) {
                mFullWakelock.release();
                mFullWakelock = null;
                Log.d(TAG, "releaseFullWakeLock release");
            } else {
                Log.d(TAG, "releaseFullWakeLock mWakelock.isHeld() == false");
            }
        } else {
            Log.d(TAG, "releaseFullWakeLock mWakelock == null");
        }
    }

    public static void releasePartialWakeLock(Context context) {
        if (mPartialWakelock != null) {
            if (mPartialWakelock.isHeld()) {
                mPartialWakelock.release();
                mPartialWakelock = null;
                Log.d(TAG, "releasePartialWakeLock release");
            } else {
                Log.d(TAG, "releasePartialWakeLock mWakelock.isHeld() == false");
            }
        } else {
            Log.d(TAG, "releasePartialWakeLock mWakelock == null");
        }
    }

    public static void enableKeyguard(Context context) {
        if (mKeyguardLock != null) {
            KeyguardManager km = (KeyguardManager) context
                    .getSystemService(Context.KEYGUARD_SERVICE);

            // release screen
            // if (!km.inKeyguardRestrictedInputMode()) {
            // lock keyguard
            mKeyguardLock.reenableKeyguard();
            mKeyguardLock = null;
            Log.d(TAG, "enableKeyguard reenableKeyguard");
            // } else {
            // Log.d(TAG,
            // "enableKeyguard km.inKeyguardRestrictedInputMode() == true");
            // }
        } else {
            Log.d(TAG, "enableKeyguard mKeyguardLock == null");
        }
    }

    public static void releaseWakeLock(Context context) {
        releasePartialWakeLock(context);
        releaseFullWakeLock(context);
    }

}
