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

package com.mediatek.rcse.plugin.phone;


import android.util.Log;

import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallManager;
import com.android.internal.telephony.Connection;

/**
 * "Call card" UI element: the in-call screen contains a tiled layout of call
 * cards, each representing the state of a current "call" (ie. an active call, a
 * call on hold, or an incoming call.)
 */
public class RCSeUtils {

    private static final String LOG_TAG = "RCSeUtils";
    private static final boolean DBG = true;

    public static boolean canShare(CallManager cm) {
        if (!canShareFromCallState(cm)) {
            return false;
        }
        String number = getRCSePhoneNumber(cm);
        if (null != number) {
            return RCSeInCallScreenExtension.isCapabilityToShare(number);
        } else {
            if (DBG) {
                log("get rcse phone number failed, return false");
            }
            return false;
        }
    }

    public static boolean canShareFromCallState(CallManager cm) {
        if (cm.hasActiveRingingCall()) {
            if (DBG) {
                log("can not share for ring call is active");
            }
            return false;
        }
        if (cm.hasActiveBgCall()) {
            if (DBG) {
                log("can not share for background call is active");
            }
            return false;
        }
        /*
         * if (cm.getForegroundCalls().size() > 1) { if (DBG)
         * log("can not share for foreground call count > 1"); return false; }
         */
        if (cm.getFgCallConnections().size() > 1) {
            if (DBG) {
                log("can not share for foreground call connection > 1");
            }
            return false;
        }
        Call fgCall = cm.getActiveFgCall();
        if (fgCall.getState() != Call.State.ACTIVE) {
            if (DBG) {
                log("can not share for foreground call state is not ACTIVE");
            }
            return false;
        }
        Connection cn = fgCall.getLatestConnection();
        if (null == cn) {
            if (DBG) {
                log("can not share for latest connection is null");
            }
            return false;
        }
        if (cn.isVideo()) {
            if (DBG) {
                log("can not share for latest connection is video type");
            }
            return false;
        }
        return true;
    }

    public static String getRCSePhoneNumber(CallManager cm) {
        Call fgCall = cm.getActiveFgCall();
        Connection cn = fgCall.getLatestConnection();
        if (null == cn) {
            if (DBG) {
                log("can not get RCSe phone number for latest connection is null");
            }
            return null;
        }
        if (DBG) {
            log("getRCSePhoneNumber(), number is " + cn.getAddress());
        }
        return cn.getAddress();
    }

    public static boolean shouldStop(CallManager cm) {
        if (cm.hasActiveBgCall()) {
            if (DBG) {
                log("should stop sharing for background call is active");
            }
            return true;
        }
        /*
         * if (cm.getForegroundCalls().size() > 1) { if (DBG)
         * log("can not share for foreground call count > 1"); return false; }
         */
        if (cm.getFgCallConnections().size() > 1) {
            if (DBG) {
                log("should stop sharing for foreground call connection > 1");
            }
            return true;
        }
        Call fgCall = cm.getActiveFgCall();
        if (fgCall.getState() != Call.State.ACTIVE) {
            if (DBG) {
                log("should stop sharing for foreground call state is not ACTIVE");
            }
            return true;
        }
        Connection cn = fgCall.getLatestConnection();
        if (null == cn) {
            if (DBG) {
                log("should stop for latest connection is null");
            }
            return true;
        }
        if (cn.isVideo()) {
            if (DBG) {
                log("should stop for latest connection is video type");
            }
            return true;
        }
        return false;
    }

    private static void log(String msg) {
        Log.d(LOG_TAG, msg);
    }

}
