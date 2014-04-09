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

package com.mediatek.MediatekDM.session;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.mediatek.MediatekDM.DmApplication;
import com.mediatek.MediatekDM.DmConst.TAG;
import com.mediatek.MediatekDM.DmController;
import com.mediatek.MediatekDM.DmService;
import com.mediatek.MediatekDM.data.IDmPersistentValues;
import com.mediatek.MediatekDM.mdm.MdmException.MdmError;
import com.mediatek.MediatekDM.mdm.SessionInitiator;
import com.mediatek.MediatekDM.mdm.SessionStateObserver;
import com.mediatek.MediatekDM.session.SessionEventQueue.DLAbortState;
import com.mediatek.MediatekDM.util.DmThreadPool;

public class DmSessionStateObserver implements SessionStateObserver {
    public DmSessionStateObserver() {

    }

    // Interface method of sessionstateobserver.
    // Called by engine when state of session changes.
    public void notify(SessionType type, SessionState state, int lastError,
            SessionInitiator initiator) {
        Log.i(TAG.Session, "---- session state notify ----");
        Log.i(TAG.Session, "[session-type] = " + type);
        Log.i(TAG.Session, "[session-stat] = " + state);
        Log.i(TAG.Session, "[last-err-msg] = " + MdmError.fromInt(lastError) + "(" + lastError
                + ")");
        Log.i(TAG.Session, "[ses-initiator]= " + initiator.getId());
        Log.i(TAG.Session, "---- session state dumped ----");

        String initiatorName = initiator.getId();

        if (initiatorName.startsWith("VDM_SCOMO")) {
            DmService.sessionType = DmService.SESSION_TYPE_SCOMO;
            final Handler h = DmService.getInstance().getScomoHandler();
            if (type == SessionType.DM) {
                if (state == SessionState.COMPLETE) {
                    h.sendMessage(h.obtainMessage(IDmPersistentValues.MSG_DM_SESSION_COMPLETED));
                } else if (state == SessionState.ABORTED) {
                    Message msg = h.obtainMessage(IDmPersistentValues.MSG_DM_SESSION_ABORTED);
                    msg.arg1 = lastError;
                    msg.sendToTarget();
                }
            } else if (type == SessionType.DL) {
                if (state == SessionState.COMPLETE) {
                    Log.d(TAG.Session, "--- DL session COMPLETED ---");
                    DmApplication.getInstance().queueEvent(
                            SessionEventQueue.EVENT_DL_SESSION_COMPLETED);
                    h.sendMessage(h
                            .obtainMessage(IDmPersistentValues.MSG_SCOMO_DL_SESSION_COMPLETED));
                } else if (state == SessionState.STARTED) {
                    Log.d(TAG.Session, "--- DL session STARTED ---");
                    h.sendEmptyMessage(IDmPersistentValues.MSG_SCOMO_DL_SESSION_START);
                    DmApplication.getInstance().queueEvent(
                            SessionEventQueue.EVENT_DL_SESSION_STARTED);
                } else if (state == SessionState.ABORTED) {
                    Log.d(TAG.Session, "+++ DL session ABORTED +++");
                    DmApplication.getInstance().queueEvent(
                            SessionEventQueue.EVENT_DL_SESSION_ABORTED,
                            new Integer(lastError));
                    executeSessionAbort(initiator, lastError, h);
                }
            }
        } else if (initiatorName.startsWith("VDM_FUMO") || initiatorName.startsWith("VDM_LAWMO")) {
            if (initiatorName.startsWith("VDM_FUMO")) {
                DmService.sessionType = DmService.SESSION_TYPE_FUMO;
            } else {
                DmService.sessionType = DmService.SESSION_TYPE_LAWMO;
            }
            final Handler h = DmService.getInstance().mHandler;
            if (type == SessionType.DM) {
                if (state == SessionState.COMPLETE) {
                    Log.i(TAG.Session, "DM session complete, send message to service.");
                    DmAction action = DmController.getDmAction();
                    h.sendMessage(h.obtainMessage(IDmPersistentValues.MSG_DMSESSIONCOMPLETED,
                            action));
                } else if (state == SessionState.ABORTED) {
                    Log.i(TAG.Session, "DM session aborted, send message to service.");
                    h.sendMessage(h.obtainMessage(IDmPersistentValues.MSG_DMSESSIONABORTED,
                            lastError, 0, initiator));
                }
            } else if (type == SessionType.DL) {
                if (state == SessionState.ABORTED) {
                    Log.d(TAG.Session, "+++ DL session ABORTED +++");
                    DmApplication.getInstance().queueEvent(
                            SessionEventQueue.EVENT_DL_SESSION_ABORTED,
                            new Integer(lastError));
                    executeSessionAbort(initiator, lastError, h);
                } else if (state == SessionState.STARTED) {
                    h.sendEmptyMessage(IDmPersistentValues.MSG_DLPKGSTARTED);
                    Log.d(TAG.Session, "--- DL session STARTED ---");
                    DmApplication.getInstance().queueEvent(
                            SessionEventQueue.EVENT_DL_SESSION_STARTED);
                } else if (state == SessionState.COMPLETE) {
                    Log.d(TAG.Session, "--- DL session COMPLETED ---");
                    DmApplication.getInstance().queueEvent(
                            SessionEventQueue.EVENT_DL_SESSION_COMPLETED);
                }

            }
        } else if (initiatorName.startsWith("Network Inited")) {
            Handler h = DmService.getInstance().mHandler;
            if (type == SessionType.DM) {

                if (state == SessionState.COMPLETE) {
                    Log.i(TAG.Session, "DM session complete, send message to service.");
                    DmAction action = DmController.getDmAction();
                    h.sendMessage(h.obtainMessage(IDmPersistentValues.MSG_DMSESSIONCOMPLETED,
                            action));
                } else if (state == SessionState.ABORTED) {
                    Log.i(TAG.Session, "DM session aborted, send message to service.");
                    h.sendMessage(h.obtainMessage(IDmPersistentValues.MSG_DMSESSIONABORTED,
                            lastError, 0, initiator));
                }
            } else if (type == SessionType.DL) {
                if (state == SessionState.ABORTED) {
                    Log.i(TAG.Session, "DL session aborted, send message to service.");
                    h.sendMessage(h.obtainMessage(IDmPersistentValues.MSG_DLSESSIONABORTED,
                            lastError, 0, initiator));

                }
            }
        } else {
            Log.e(TAG.Session, "unknown initiator: " + initiator.getId());
            return;
        }
    }

    private void executeSessionAbort(final SessionInitiator initiator, final int lastError,
            final Handler handler) {

        final boolean isFumo = initiator.getId().startsWith("VDM_FUMO");
        final boolean isScomo = initiator.getId().startsWith("VDM_SCOMO");
        SessionEventQueue.DLAbortState abState = DmApplication.getInstance().analyzeDLAbortState();
        switch (abState.value) {
            case DLAbortState.STATE_NEED_RESUME_NOW:
                Log.d(TAG.Client, "+++ connection re-setup +++, continue DL session...");
                Runnable resumeJob = new Runnable() {
                    @Override
                    public void run() {
                        if (isFumo) {
                            DmService.getInstance().resumeDlPkg();
                        } else if (isScomo) {
                            DmService.getInstance().resumeDlScomoPkgNoUI();
                        }
                    }
                };
                DmThreadPool.getInstance().execute(resumeJob);
                break;

            case DLAbortState.STATE_HAVENOT_TIMEOUT:
                Log.d(TAG.Client, "+++ connection not timeout(5min) +++, waiting...");
                Runnable batchJob = new Runnable() {
                    @Override
                    public void run() {
                        if (DmApplication.getInstance().isDmWapConnected()) {
                            Log.d(TAG.Debug, "[batch-task]->cancel all other pending jobs.");
                            DmApplication.getInstance().cancelAllPendingJobs();

                            Log.d(TAG.Debug, "[batch-task]->netowrk re-setup, resume DL.");
                            if (isFumo) {
                                DmService.getInstance().resumeDlPkg();
                            } else if (isScomo) {
                                DmService.getInstance().resumeDlScomoPkgNoUI();
                            }
                        } else {
                            Log.d(TAG.Debug, "[batch-task]->network un-ready, bypass.");
                        }
                    }
                };
                Runnable pendingJob = new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG.Debug, "[pending-task]->cancel all other pending jobs.");
                        DmApplication.getInstance().cancelAllPendingJobs();

                        if (DmApplication.getInstance().isDmWapConnected()) {
                            Log.d(TAG.Debug, "[pending-task]->netowrk re-setup, resume DL.");
                            if (isFumo) {
                                DmService.getInstance().resumeDlPkg();
                            } else if (isScomo) {
                                DmService.getInstance().resumeDlScomoPkgNoUI();
                            }
                        } else {
                            Log.d(TAG.Debug, "[pending-task]->send DL ABORT msg after timeout.");
                            if (isFumo) {
                                handler.sendMessage(handler.obtainMessage(
                                        IDmPersistentValues.MSG_DLSESSIONABORTED,
                                        lastError, 0, initiator));
                            } else if (isScomo) {
                                handler.sendMessage(handler.obtainMessage(
                                        IDmPersistentValues.MSG_SCOMO_DL_SESSION_ABORTED,
                                        lastError, 0, initiator));
                            }
                        }
                    }
                };
                DmApplication.getInstance().cancelAllPendingJobs();
                DmApplication.getInstance()
                        .scheduleBatchJobs(batchJob, 10 * 1000, abState.leftTime);
                DmApplication.getInstance().scheduleJob(pendingJob, abState.leftTime);
                break;

            case DLAbortState.STATE_ALREADY_TIMEOUT:
            default:
                Log.i(TAG.Session, "DL session aborted/timeout, send message to service.");
                if (isFumo) {
                    handler.sendMessage(handler.obtainMessage(
                            IDmPersistentValues.MSG_DLSESSIONABORTED, lastError, 0,
                            initiator));
                } else if (isScomo) {
                    handler.sendMessage(handler.obtainMessage(
                            IDmPersistentValues.MSG_SCOMO_DL_SESSION_ABORTED,
                            lastError, 0, initiator));
                }
        }
    }

    public static class DmAction {
        public int fumoAction = 0;
        public int lawmoAction = 0;
        public int scomoAction = 0;
    }
}
