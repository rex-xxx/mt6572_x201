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

package com.mediatek.MediatekDM.session;

import android.util.Log;

import com.mediatek.MediatekDM.DmConst.TAG;
import com.mediatek.MediatekDM.mdm.MdmException.MdmError;
import com.mediatek.MediatekDM.option.Options;

import java.util.ArrayList;

public class SessionEventQueue {
    /**
     * DL session event types
     */
    public static final int EVENT_DL_SESSION_UNKNOWN = 0;
    public static final int EVENT_DL_SESSION_STARTED = 1;
    public static final int EVENT_DL_SESSION_ABORTED = 2;
    public static final int EVENT_DL_SESSION_COMPLETED = 3;
    /**
     * Connection event types
     */
    public static final int EVENT_CONN_UNKNOWN = 10;
    public static final int EVENT_CONN_CONNECTED = 11;
    public static final int EVENT_CONN_DISCONNECTED = 12;

    private static final int QUEUE_CAPACITY = 20;

    private ArrayList<Event> mEventQueue;
    private int mLastConnEvent;
    private Event mLastSessionEvent;
    private Object mLock;

    public SessionEventQueue() {
        mEventQueue = new ArrayList<Event>(QUEUE_CAPACITY);
        mLock = new Object();
        mLastConnEvent = EVENT_CONN_UNKNOWN;
        mLastSessionEvent = null;
    }

    public void queueEvent(int eventType, Object extra) {
        Event event = new Event();
        event.type = eventType;
        event.timestamp = System.currentTimeMillis();
        event.extra = extra;

        synchronized (mLock) {
            if (mEventQueue.size() == QUEUE_CAPACITY) {
                mEventQueue.remove(0);
            }

            if (eventType == EVENT_DL_SESSION_STARTED) {
                Log.d(TAG.Debug, "[event-queue]->new session started, reset Q.");
                mEventQueue.clear();
            }

            mEventQueue.add(event);

            if (isConnEvent(eventType)) {
                // cache last connection state.
                mLastConnEvent = eventType;
            } else {
                // cache last session event here.
                mLastSessionEvent = event;
            }
        }
    }

    public DLAbortState analyzeAbortState(long nowMillis) {
        DLAbortState state = new DLAbortState();

        synchronized (mLock) {
            if (mEventQueue.size() == 0) {
                return state;
            }

            if (mLastSessionEvent == null) {
                Log.w(TAG.Debug, "[DLAbortState]->no cached last session event.");
                return state;
            }

            boolean isConnIssue = false;

            if (mLastSessionEvent.type == EVENT_DL_SESSION_ABORTED) {
                Integer lastErrorObj = (Integer) mLastSessionEvent.extra;
                int errCode = lastErrorObj.intValue();

                if (errCode == MdmError.COMMS_FATAL.val || errCode == MdmError.COMMS_NON_FATAL.val
                        || errCode == MdmError.COMMS_SOCKET_ERROR.val
                        || errCode == MdmError.COMMS_HTTP_ERROR.val) {
                    // we only analyze abort issues caused by network issue.
                    // except SOCKET_TIMEOUT
                    isConnIssue = true;
                }
            }

            if (!isConnIssue) {
                Log.w(TAG.Debug, "[DLAbortState]->no need to analyze, not ABORT via network issue.");
                return state;
            }

            boolean hasDisconnectEvent = false;
            int indexOfReconnectEvent = -1;
            for (int index = mEventQueue.size() - 1; index >= 0; index--) {
                Event evt = mEventQueue.get(index);
                // TODO
                if (evt.type == EVENT_CONN_DISCONNECTED) {
                    hasDisconnectEvent = true;
                    if (nowMillis - evt.timestamp > Options.DLTimeoutWait.WAIT_INTERVAL) {
                        state.value = DLAbortState.STATE_ALREADY_TIMEOUT;
                        break;
                    } else {

                        if (indexOfReconnectEvent > index) {
                            // network already recovered
                            state.value = DLAbortState.STATE_NEED_RESUME_NOW;
                        } else {
                            state.value = DLAbortState.STATE_HAVENOT_TIMEOUT;
                            state.leftTime = Options.DLTimeoutWait.WAIT_INTERVAL
                                    - (nowMillis - evt.timestamp);
                        }
                        break;
                    }
                } else if (evt.type == EVENT_CONN_CONNECTED) {
                    if (indexOfReconnectEvent == -1) {
                        indexOfReconnectEvent = index;
                    }
                }
            }

            // if disconnect event haven't been received
            if (!hasDisconnectEvent) {
                state.value = DLAbortState.STATE_HAVENOT_TIMEOUT;
                state.leftTime = Options.DLTimeoutWait.WAIT_INTERVAL
                        - (nowMillis - mLastSessionEvent.timestamp);
            }

        }

        Log.d(TAG.Debug, "[DLAbortState]analyze result=>" + state.value + "," + state.leftTime);
        return state;
    }

    public boolean isNetworkConnected() {
        boolean isConnected = false;
        synchronized (mLock) {
            if (mLastConnEvent == EVENT_CONN_CONNECTED) {
                isConnected = true;
            }
        }
        Log.d(TAG.Debug, "[event-queue]->isNetworkConnected:" + isConnected);
        return isConnected;
    }

    private boolean isConnEvent(int eventType) {
        return (eventType == EVENT_CONN_UNKNOWN || eventType == EVENT_CONN_CONNECTED || eventType == EVENT_CONN_DISCONNECTED);
    }

    public void dump() {
        Log.d(TAG.Debug, "------ dumping event queue ------");
        synchronized (mLock) {
            for (Event evt : mEventQueue) {
                Log.d(TAG.Debug, "" + evt);
            }
        }
        Log.d(TAG.Debug, "------ dumping finished ------");
    }

    public static class Event {
        public int type;
        public Object extra;
        public long timestamp;

        public Event() {
            type = EVENT_DL_SESSION_UNKNOWN;
            extra = null;
            timestamp = 0L;
        }

        @Override
        public String toString() {
            return String.format("[session event]type=%d, extra=%s, time=%d", type, extra,
                    timestamp);
        }
    }

    public static class DLAbortState {
        public static final int STATE_UNDEFINE = 100;
        // already passed 5 min
        public static final int STATE_ALREADY_TIMEOUT = 101;
        // still within 5 min
        public static final int STATE_HAVENOT_TIMEOUT = 102;
        // still within 5 min, and connection has been re-setup.
        public static final int STATE_NEED_RESUME_NOW = 103;

        public int value = STATE_UNDEFINE;
        public long leftTime = 0L;
    }
};
