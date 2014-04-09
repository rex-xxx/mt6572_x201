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

package com.android.internal.telephony.gsm;

import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.SystemProperties;
import android.telephony.PhoneNumberUtils;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.util.EventLog;
import android.util.Log;

import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.CallTracker;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.DriverCall;
import com.android.internal.telephony.EventLogTags;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyProperties;
import com.android.internal.telephony.UUSInfo;
import com.android.internal.telephony.gsm.CallFailCause;
import com.android.internal.telephony.gsm.GSMPhone;
import com.android.internal.telephony.gsm.GsmCall;
import com.android.internal.telephony.gsm.GsmConnection;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.List;
import java.util.ArrayList;

/// M: [mtk04070][111118][ALPS00093395]MTK added. @{
import com.android.internal.telephony.Call;
import com.android.internal.telephony.CommandException;
/* AGPS start */
import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.os.Bundle;
import com.mediatek.common.featureoption.FeatureOption;
/* AGPS end */

// MTK_OPTR_PROTECT_START
/* DM start */
import android.os.IBinder;
import android.os.ServiceManager;
import com.mediatek.common.dm.DMAgent;
import android.os.RemoteException;
/* DM end */
// MTK_OPTR_PROTECT_END

/* For adjust PhoneAPP priority, mtk04070, 20120307 */
import android.os.Process;

import com.mediatek.common.MediatekClassFactory;
import com.mediatek.common.telephony.IPhoneNumberExt;
/// @}

/**
 * {@hide}
 */
public final class GsmCallTracker extends CallTracker {
    static final String LOG_TAG = "GSM";
    private static final boolean REPEAT_POLLING = false;

    /// [mtk04070][111118][ALPS00093395]Enable debug poll.
    private static final boolean DBG_POLL = true;

    //***** Constants

    static final int MAX_CONNECTIONS = 7;   // only 7 connections allowed in GSM
    static final int MAX_CONNECTIONS_PER_CALL = 5; // only 5 connections allowed per call

    /// M: [mtk04070][111118][ALPS00093395]MTK added. @{
    public enum CrssAction {
        NONE,
        CONFERENCE,
        SEPERATE,
        SWAP,
        ECT;
    }
    /// @}

    //***** Instance Variables
    GsmConnection connections[] = new GsmConnection[MAX_CONNECTIONS];
    RegistrantList voiceCallEndedRegistrants = new RegistrantList();
    RegistrantList voiceCallStartedRegistrants = new RegistrantList();
    RegistrantList voiceCallIncomingIndicationRegistrants = new RegistrantList();

    // connections dropped during last poll
    ArrayList<GsmConnection> droppedDuringPoll
        = new ArrayList<GsmConnection>(MAX_CONNECTIONS);

    //Incoming or waiting call
    GsmCall ringingCall = new GsmCall(this);
    // Active, dialing or alerting call
    GsmCall foregroundCall = new GsmCall(this);
    //Holding call
    GsmCall backgroundCall = new GsmCall(this);

    GsmConnection pendingMO;
    boolean hangupPendingMO;

    GSMPhone phone;

    boolean desiredMute = false;    // false = mute off

    PhoneConstants.State state = PhoneConstants.State.IDLE;

    /// M: [mtk04070][111118][ALPS00093395]MTK added. @{
    boolean isSwitchBeforeDial = false;
    String delayedDialString = null;
    int delayedClirMode = 0;
    UUSInfo delayedUUSInfo = null;
    CrssAction crssAction = CrssAction.NONE;

    boolean isPendingSwap = false;

    int causeCode = 0;

    /* force release */
    boolean hasPendingHangupRequest = false;
    int pendingHangupRequest = 0;

    /* voice&video waiting */
    boolean hasPendingReplaceRequest = false;

    /* AGPS start */
    protected Context mContext;
    private boolean hasSendAgpsStartIntent = false;
    /* AGPS end */

    // MTK_OPTR_PROTECT_START
    /* DM start */
    private boolean isInLock = false;
    private boolean isFullLock = false;
    private boolean needHangupMOCall = false;
    private boolean needHangupMTCall = false;
    private DMAgent dmAgent;
    private BroadcastReceiver mReceiver;
    /* DM end */
    // MTK_OPTR_PROTECT_END

    private int pendingMTCallId = 0;
    private int pendingMTSeqNum = 0;

    //***** MTK class factory
    private static IPhoneNumberExt mPhoneNumberExt;
    static {
        try{
            mPhoneNumberExt = MediatekClassFactory.createInstance(IPhoneNumberExt.class);
        } catch (Exception e){
            e.printStackTrace();
        }
    }
    /// @}


    //***** Events


    //***** Constructors

    GsmCallTracker (GSMPhone phone) {
        this.phone = phone;
        cm = phone.mCM;

        cm.registerForCallStateChanged(this, EVENT_CALL_STATE_CHANGE, null);

        cm.registerForOn(this, EVENT_RADIO_AVAILABLE, null);
        cm.registerForNotAvailable(this, EVENT_RADIO_NOT_AVAILABLE, null);

        /// M: [mtk04070][111118][ALPS00093395]MTK added. @{
        /* AGPS start */
        mContext = phone.getContext();
        /* AGPS end */

        cm.registerForCallProgressInfo(this, EVENT_CALL_PROGRESS_INFO, null);

        cm.setOnIncomingCallIndication(this, EVENT_INCOMING_CALL_INDICATION, null);

        // MTK_OPTR_PROTECT_START
        /* DM start */
        IBinder binder = ServiceManager.getService("DMAgent");
        dmAgent = DMAgent.Stub.asInterface (binder);

        IntentFilter filter = new IntentFilter("com.mediatek.dm.LAWMO_LOCK");
        filter.addAction("com.mediatek.dm.LAWMO_UNLOCK");

        mReceiver = new GsmCallTrackerReceiver();
        Intent intent = mContext.registerReceiver(mReceiver, filter);
        try {
            if (dmAgent != null) {
                isInLock = dmAgent.isLockFlagSet();
                isFullLock = (dmAgent.getLockType() == 1);
                log("isInLock = " + isInLock + ", isFullLock = " + isFullLock);
                needHangupMOCall = dmAgent.isHangMoCallLocking();
                needHangupMTCall = dmAgent.isHangMtCallLocking();
                log("needHangupMOCall = " + needHangupMOCall + ", needHangupMTCall = " + needHangupMTCall);
            }
        } catch (RemoteException ex) {
        }
        /* DM end */
        // MTK_OPTR_PROTECT_END
        /// @}
    }

    public void dispose() {
        //Unregister for all events
        cm.unregisterForCallStateChanged(this);
        cm.unregisterForOn(this);
        cm.unregisterForNotAvailable(this);

        /// M: [mtk04070][111118][ALPS00093395]MTK added. @{
        cm.unregisterForCallProgressInfo(this);
        cm.unsetOnIncomingCallIndication(this);
        /// @}

        for(GsmConnection c : connections) {
            try {
                if(c != null) hangup(c);
            } catch (CallStateException ex) {
                Log.e(LOG_TAG, "unexpected error on hangup during dispose");
            }
        }

        try {
            if(pendingMO != null) hangup(pendingMO);
        } catch (CallStateException ex) {
            Log.e(LOG_TAG, "unexpected error on hangup during dispose");
        }

        clearDisconnected();
    }

    protected void finalize() {
        Log.d(LOG_TAG, "GsmCallTracker finalized");
    }

    //***** Instance Methods

    //***** Public Methods
    public void registerForVoiceCallStarted(Handler h, int what, Object obj) {
		/* 
		  Called in GsmDataConnectionTracker and CdmaDataConnectionTracker,
		  then onVoiceCallStarted is called to process this event.
		*/
        Registrant r = new Registrant(h, what, obj);
        voiceCallStartedRegistrants.add(r);
    }

    public void unregisterForVoiceCallStarted(Handler h) {
		/* Called in GsmDataConnectionTracker and CdmaDataConnectionTracker */
        voiceCallStartedRegistrants.remove(h);
    }

    public void registerForVoiceCallIncomingIndication(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        voiceCallIncomingIndicationRegistrants.add(r);
    }

    public void unregisterForVoiceCallIncomingIndication(Handler h) {
		/* Called in GsmDataConnectionTracker and CdmaDataConnectionTracker */
        voiceCallIncomingIndicationRegistrants.remove(h);
    }
    
    public void registerForVoiceCallEnded(Handler h, int what, Object obj) {
		/* 
		  Called in GsmDataConnectionTracker and CdmaDataConnectionTracker,
		  then onVoiceCallEnded is called to process this event.
		*/
        Registrant r = new Registrant(h, what, obj);
        voiceCallEndedRegistrants.add(r);
    }

    public void unregisterForVoiceCallEnded(Handler h) {
		/* Called in GsmDataConnectionTracker and CdmaDataConnectionTracker */
        voiceCallEndedRegistrants.remove(h);
    }

    private void
    fakeHoldForegroundBeforeDial() {
        List<Connection> connCopy;

        // We need to make a copy here, since fakeHoldBeforeDial()
        // modifies the lists, and we don't want to reverse the order
        connCopy = (List<Connection>) foregroundCall.connections.clone();

        for (int i = 0, s = connCopy.size() ; i < s ; i++) {
            GsmConnection conn = (GsmConnection)connCopy.get(i);

            conn.fakeHoldBeforeDial();
        }
    }

    /**
     * clirMode is one of the CLIR_ constants
     */
    synchronized Connection
    dial (String dialString, int clirMode, UUSInfo uusInfo) throws CallStateException {
        // note that this triggers call state changed notif
        clearDisconnected();

        if (!canDial()) {
            throw new CallStateException("cannot dial in current state");
        }

        // The new call must be assigned to the foreground call.
        // That call must be idle, so place anything that's
        // there on hold
        if (foregroundCall.getState() == GsmCall.State.ACTIVE) {
            // this will probably be done by the radio anyway
            // but the dial might fail before this happens
            // and we need to make sure the foreground call is clear
            // for the newly dialed connection
            /// M: [mtk04070][111118][ALPS00093395]MTK added. 
            isSwitchBeforeDial = true;
            switchWaitingOrHoldingAndActive();

            // Fake local state so that
            // a) foregroundCall is empty for the newly dialed connection
            // b) hasNonHangupStateChanged remains false in the
            // next poll, so that we don't clear a failed dialing call
            fakeHoldForegroundBeforeDial();
        }

        if (foregroundCall.getState() != GsmCall.State.IDLE) {
            //we should have failed in !canDial() above before we get here
            throw new CallStateException("cannot dial in current state");
        }

        pendingMO = new GsmConnection(phone.getContext(), checkForTestEmergencyNumber(dialString),
                this, foregroundCall);
        hangupPendingMO = false;

        /// M: [mtk04070][111118][ALPS00093395]MTK modified. @{
        if (pendingMO.address == null || pendingMO.address.length() == 0
            || pendingMO.address.indexOf(PhoneNumberUtils.WILD) >= 0
        ) {
            // Phone number is invalid
            pendingMO.cause = Connection.DisconnectCause.INVALID_NUMBER;

            if (isSwitchBeforeDial) {
                isSwitchBeforeDial = false;
            }
            Message msg = obtainMessage();
            msg.what = EVENT_DIAL_CALL_RESULT;
            msg.obj = new AsyncResult(null, null, new CommandException(CommandException.Error.GENERIC_FAILURE));
            sendMessageDelayed(msg, 100);

            // handlePollCalls() will notice this call not present
            // and will mark it as dropped.
            // pollCallsWhenSafe();
        } else {
            int len = dialString.length();
            int i = 0;
            StringBuilder ret = new StringBuilder(len);
            boolean firstCharAdded = false;

            for (i = 0; i < len; i++) {
                char c = dialString.charAt(i);

                if (c == PhoneNumberUtils.PAUSE) {
                    ret.append('p');
                } else if (c == PhoneNumberUtils.WAIT) {
                    ret.append('w');
        } else {
                    if (c != '+' || !firstCharAdded) {
                        firstCharAdded = true;
                        ret.append(c);
                    }
                }
            }

            // Always unmute when initiating a new call
            setMute(false);

            /* AGPS start */
            if (FeatureOption.MTK_AGPS_APP) {
                String string = ret.toString();
                if (PhoneNumberUtils.isEmergencyNumber(string) && !hasSendAgpsStartIntent) {
                    Bundle mbundle = new Bundle();
                    mbundle.putInt("EM_Call_State", 1);
                    mbundle.putString("Call_Number", string);
                    Intent intent = new Intent("android.location.agps.EMERGENCY_CALL");
                    intent.putExtras(mbundle);
                    mContext.sendBroadcast(intent);
                    hasSendAgpsStartIntent = true;
                    log("Broadcast ecc start intent for AGPS");
        }
            }
            /* AGPS end */

            if (!isSwitchBeforeDial) {
                if (PhoneNumberUtils.isEmergencyNumber(dialString)
                    && !mPhoneNumberExt.isSpecialEmergencyNumber(dialString)) {
                    cm.emergencyDial(ret.toString(), clirMode, uusInfo, obtainCompleteMessage(EVENT_DIAL_CALL_RESULT));
                } else {
                    cm.dial(ret.toString(), clirMode, uusInfo, obtainCompleteMessage(EVENT_DIAL_CALL_RESULT));
                }
            } else {
                delayedDialString = ret.toString();
                delayedClirMode = clirMode;
                delayedUUSInfo = uusInfo;
            }
        }
        /// @}

        updatePhoneState();
        phone.notifyPreciseCallStateChanged();

        return pendingMO;
    }

    Connection
    dial(String dialString) throws CallStateException {
        return dial(dialString, CommandsInterface.CLIR_DEFAULT, null);
    }

    Connection
    dial(String dialString, UUSInfo uusInfo) throws CallStateException {
        return dial(dialString, CommandsInterface.CLIR_DEFAULT, uusInfo);
    }

    Connection
    dial(String dialString, int clirMode) throws CallStateException {
        return dial(dialString, clirMode, null);
    }

    void
    acceptCall () throws CallStateException {
        // FIXME if SWITCH fails, should retry with ANSWER
        // in case the active/holding call disappeared and this
        // is no longer call waiting

        /// M: [mtk04070][111118][ALPS00093395]MTK modified. @{
        if (ringingCall.getState() == GsmCall.State.INCOMING) {
            log("acceptCall: incoming...");
            // Always unmute when answering a new call
            setMute(false);
            cm.acceptCall(obtainCompleteMessage());
        } else if (ringingCall.getState() == GsmCall.State.WAITING) {
            log("acceptCall: waiting...");
            setMute(false);
            /* vt start */
            if (FeatureOption.MTK_VT3G324M_SUPPORT) {
                GsmConnection cn = (GsmConnection)ringingCall.connections.get(0);
                if (cn.isVideo()) {
                    GsmConnection fgCn = (GsmConnection)foregroundCall.connections.get(0);
                    if (fgCn != null && fgCn.isVideo()) {
                        hasPendingReplaceRequest = true;
                        cm.replaceVtCall(fgCn.index + 1, obtainCompleteMessage());
                        fgCn.onHangupLocal();
                        return;
                    }
                }
            }
            /* vt end */
            switchWaitingOrHoldingAndActive();
        } else {
            throw new CallStateException("phone not ringing");
        }
        /// @}
    }

    void
    rejectCall () throws CallStateException {
        // AT+CHLD=0 means "release held or UDUB"
        // so if the phone isn't ringing, this could hang up held
        if (ringingCall.getState().isRinging()) {
            cm.rejectCall(obtainCompleteMessage());
        } else {
            throw new CallStateException("phone not ringing");
        }
    }

    void
    switchWaitingOrHoldingAndActive() throws CallStateException {
        // Should we bother with this check?
        if (ringingCall.getState() == GsmCall.State.INCOMING) {
            throw new CallStateException("cannot be in the incoming state");
        } else {
            /// M: [mtk04070][111118][ALPS00093395]MTK modified. @{
            if (!isPendingSwap)
            {
            cm.switchWaitingOrHoldingAndActive(
                    obtainCompleteMessage(EVENT_SWITCH_RESULT));

                crssAction = CrssAction.SWAP;
                isPendingSwap = true;
                foregroundCall.lastMptyState = foregroundCall.isMptyCall;
                backgroundCall.lastMptyState = backgroundCall.isMptyCall;
            }
            /// @}
        }
    }

    void
    conference() throws CallStateException {
        cm.conference(obtainCompleteMessage(EVENT_CONFERENCE_RESULT));
    }

    void
    explicitCallTransfer() throws CallStateException {
        cm.explicitCallTransfer(obtainCompleteMessage(EVENT_ECT_RESULT));
    }

    void
    clearDisconnected() {
        internalClearDisconnected();

        /// M: [mtk04070][111118][ALPS00093395]Update the state of conference call.
        updateMptyState();
        updatePhoneState();
        phone.notifyPreciseCallStateChanged();
    }

    boolean
    canConference() {
        return foregroundCall.getState() == GsmCall.State.ACTIVE
                && backgroundCall.getState() == GsmCall.State.HOLDING
                && !backgroundCall.isFull()
                && !foregroundCall.isFull();
    }

    boolean
    canDial() {
        boolean ret;
        int serviceState = phone.getServiceState().getState();
        String disableCall = SystemProperties.get(
                TelephonyProperties.PROPERTY_DISABLE_CALL, "false");

        ret = (serviceState != ServiceState.STATE_POWER_OFF)
                && pendingMO == null
                && !ringingCall.isRinging()
                && !disableCall.equals("true")
                && (!foregroundCall.getState().isAlive()
                    || !backgroundCall.getState().isAlive());

        return ret;
    }

    boolean
    canTransfer() {
        return foregroundCall.getState() == GsmCall.State.ACTIVE
                && backgroundCall.getState() == GsmCall.State.HOLDING;
    }

    //***** Private Instance Methods

    private void
    internalClearDisconnected() {
        ringingCall.clearDisconnected();
        foregroundCall.clearDisconnected();
        backgroundCall.clearDisconnected();
    }

    /**
     * Obtain a message to use for signalling "invoke getCurrentCalls() when
     * this operation and all other pending operations are complete
     */
    private Message
    obtainCompleteMessage() {
        return obtainCompleteMessage(EVENT_OPERATION_COMPLETE);
    }

    /**
     * Obtain a message to use for signalling "invoke getCurrentCalls() when
     * this operation and all other pending operations are complete
     */
    private Message
    obtainCompleteMessage(int what) {
        pendingOperations++;
        lastRelevantPoll = null;
        needsPoll = true;

        if (DBG_POLL) log("obtainCompleteMessage: pendingOperations=" +
                pendingOperations + ", needsPoll=" + needsPoll);

        return obtainMessage(what);
    }

    private void
    operationComplete() {
        pendingOperations--;

        if (DBG_POLL) log("operationComplete: pendingOperations=" +
                pendingOperations + ", needsPoll=" + needsPoll + ", hasPendingReplaceRequest=" + hasPendingReplaceRequest);

        if (pendingOperations == 0 && needsPoll) {
            /// M: [mtk04070][111118][ALPS00093395]MTK marked. @{
            //lastRelevantPoll = obtainMessage(EVENT_POLL_CALLS_RESULT);
            //cm.getCurrentCalls(lastRelevantPoll);
            /// @}
        } else if (pendingOperations < 0) {
            // this should never happen
            Log.e(LOG_TAG,"GsmCallTracker.pendingOperations < 0");
            pendingOperations = 0;
        }
    }

    private void
    updatePhoneState() {
        PhoneConstants.State oldState = state;

        if (ringingCall.isRinging()) {
            state = PhoneConstants.State.RINGING;
        } else if (pendingMO != null ||
                !(foregroundCall.isIdle() && backgroundCall.isIdle())) {
            state = PhoneConstants.State.OFFHOOK;
        } else {
            state = PhoneConstants.State.IDLE;
        }

        if (state == PhoneConstants.State.IDLE && oldState != state) {
            voiceCallEndedRegistrants.notifyRegistrants(
                new AsyncResult(null, null, null));
        } else if (oldState == PhoneConstants.State.IDLE && oldState != state) {
            voiceCallStartedRegistrants.notifyRegistrants (
                    new AsyncResult(null, null, null));
        }

        if (state != oldState) {
            phone.notifyPhoneStateChanged();
        }
    }

    protected synchronized void
    handlePollCalls(AsyncResult ar) {
        List polledCalls;

        if (ar.exception == null) {
            polledCalls = (List)ar.result;
        } else if (isCommandExceptionRadioNotAvailable(ar.exception)) {
            // just a dummy empty ArrayList to cause the loop
            // to hang up all the calls
            polledCalls = new ArrayList();
        } else {
            // Radio probably wasn't ready--try again in a bit
            // But don't keep polling if the channel is closed
            pollCallsAfterDelay();
            return;
        }

        Connection newRinging = null; //or waiting
        boolean hasNonHangupStateChanged = false;   // Any change besides
                                                    // a dropped connection
        boolean needsPollDelay = false;
        boolean unknownConnectionAppeared = false;

        for (int i = 0, curDC = 0, dcSize = polledCalls.size()
                ; i < connections.length; i++) {
            GsmConnection conn = connections[i];
            DriverCall dc = null;

            // polledCall list is sparse
            if (curDC < dcSize) {
                dc = (DriverCall) polledCalls.get(curDC);

                if (dc.index == i+1) {
                    curDC++;
                } else {
                    dc = null;
                }
            }

            if (DBG_POLL) log("poll: conn[i=" + i + "]=" +
                    conn+", dc=" + dc);

            if (conn == null && dc != null) {
                // Connection appeared in CLCC response that we don't know about
                if (pendingMO != null && pendingMO.compareTo(dc)) {

                    if (DBG_POLL) log("poll: pendingMO=" + pendingMO);

                    // It's our pending mobile originating call
                    connections[i] = pendingMO;
                    pendingMO.index = i;
                    pendingMO.update(dc);
                    pendingMO = null;

                    // Someone has already asked to hangup this call
                    if (hangupPendingMO) {
                        hangupPendingMO = false;
                        try {
                            if (Phone.DEBUG_PHONE) log(
                                    "poll: hangupPendingMO, hangup conn " + i);
                            hangup(connections[i]);
                        } catch (CallStateException ex) {
                            Log.e(LOG_TAG, "unexpected error on hangup");
                        }

                        // Do not continue processing this poll
                        // Wait for hangup and repoll
                        return;
                    }
                } else {
                    connections[i] = new GsmConnection(phone.getContext(), dc, this, i);

                    // it's a ringing call
                    if (connections[i].getCall() == ringingCall) {
                        newRinging = connections[i];
                    } else {
                        // Something strange happened: a call appeared
                        // which is neither a ringing call or one we created.
                        // Either we've crashed and re-attached to an existing
                        // call, or something else (eg, SIM) initiated the call.

                        Log.i(LOG_TAG,"Phantom call appeared " + dc);

                        // If it's a connected call, set the connect time so that
                        // it's non-zero.  It may not be accurate, but at least
                        // it won't appear as a Missed Call.
                        if (dc.state != DriverCall.State.ALERTING
                                && dc.state != DriverCall.State.DIALING) {
                            connections[i].connectTime = System.currentTimeMillis();
                        }

                        unknownConnectionAppeared = true;
                    }
                }
                hasNonHangupStateChanged = true;
            } else if (conn != null && dc == null) {
                // Connection missing in CLCC response that we were
                // tracking.
                droppedDuringPoll.add(conn);
                // Dropped connections are removed from the CallTracker
                // list but kept in the GsmCall list
                connections[i] = null;
            } else if (conn != null && dc != null && !conn.compareTo(dc)) {
                // Connection in CLCC response does not match what
                // we were tracking. Assume dropped call and new call

                droppedDuringPoll.add(conn);
                connections[i] = new GsmConnection (phone.getContext(), dc, this, i);

                if (connections[i].getCall() == ringingCall) {
                    newRinging = connections[i];
                } // else something strange happened
                hasNonHangupStateChanged = true;
            } else if (conn != null && dc != null) { /* implicit conn.compareTo(dc) */
                boolean changed;
                changed = conn.update(dc);
                hasNonHangupStateChanged = hasNonHangupStateChanged || changed;
            }

            if (REPEAT_POLLING) {
                if (dc != null) {
                    // FIXME with RIL, we should not need this anymore
                    if ((dc.state == DriverCall.State.DIALING
                            /*&& cm.getOption(cm.OPTION_POLL_DIALING)*/)
                        || (dc.state == DriverCall.State.ALERTING
                            /*&& cm.getOption(cm.OPTION_POLL_ALERTING)*/)
                        || (dc.state == DriverCall.State.INCOMING
                            /*&& cm.getOption(cm.OPTION_POLL_INCOMING)*/)
                        || (dc.state == DriverCall.State.WAITING
                            /*&& cm.getOption(cm.OPTION_POLL_WAITING)*/)
                    ) {
                        // Sometimes there's no unsolicited notification
                        // for state transitions
                        needsPollDelay = true;
                    }
                }
            }
        }

        // This is the first poll after an ATD.
        // We expect the pending call to appear in the list
        // If it does not, we land here
        if (pendingMO != null) {
            Log.d(LOG_TAG,"Pending MO dropped before poll fg state:"
                            + foregroundCall.getState());

            droppedDuringPoll.add(pendingMO);
            pendingMO = null;
            hangupPendingMO = false;
        }

        if (newRinging != null) {
            phone.notifyNewRingingConnection(newRinging);
        }

        // clear the "local hangup" and "missed/rejected call"
        // cases from the "dropped during poll" list
        // These cases need no "last call fail" reason
        for (int i = droppedDuringPoll.size() - 1; i >= 0 ; i--) {
            GsmConnection conn = droppedDuringPoll.get(i);

            if (conn.isIncoming() && conn.getConnectTime() == 0) {
                // Missed or rejected call
                Connection.DisconnectCause cause;
                if (conn.cause == Connection.DisconnectCause.LOCAL) {
                    cause = Connection.DisconnectCause.INCOMING_REJECTED;
                } else {
                    cause = Connection.DisconnectCause.INCOMING_MISSED;
                }

                if (Phone.DEBUG_PHONE) {
                    log("missed/rejected call, conn.cause=" + conn.cause);
                    log("setting cause to " + cause);
                }
                droppedDuringPoll.remove(i);
                conn.onDisconnect(cause);
            } else if (conn.cause == Connection.DisconnectCause.LOCAL) {
                // Local hangup
                droppedDuringPoll.remove(i);
                conn.onDisconnect(Connection.DisconnectCause.LOCAL);
            } else if (conn.cause ==
                Connection.DisconnectCause.INVALID_NUMBER) {
                droppedDuringPoll.remove(i);
                conn.onDisconnect(Connection.DisconnectCause.INVALID_NUMBER);
            }
        }

        // Any non-local disconnects: determine cause
        if (droppedDuringPoll.size() > 0) {
            cm.getLastCallFailCause(
                obtainNoPollCompleteMessage(EVENT_GET_LAST_CALL_FAIL_CAUSE));
        }

        if (needsPollDelay) {
            pollCallsAfterDelay();
        }

        // Cases when we can no longer keep disconnected Connection's
        // with their previous calls
        // 1) the phone has started to ring
        // 2) A Call/Connection object has changed state...
        //    we may have switched or held or answered (but not hung up)
        if (newRinging != null || hasNonHangupStateChanged) {
            internalClearDisconnected();
        }

        updatePhoneState();

        if (unknownConnectionAppeared) {
            phone.notifyUnknownConnection();
        }

        if (hasNonHangupStateChanged || newRinging != null) {
            phone.notifyPreciseCallStateChanged();
        }

        //dumpState();
    }

    private void
    handleRadioNotAvailable() {
        /// M: [mtk04070][111118][ALPS00093395]MTK modified. @{
        // handlePollCalls will clear out its
        // call list when it gets the CommandException
        // error result from this
        // pollCallsWhenSafe();
        lastRelevantPoll = obtainMessage(EVENT_POLL_CALLS_RESULT);
        cm.getCurrentCalls(lastRelevantPoll);
        /// @}
    }

    private void
    dumpState() {
        /// M: [mtk04070][111118][ALPS00093395]MTK modified. @{
        List l;
        int callId = 0;
        int count = 0;

        log("* Phone State: " + state);

        log("* Ringing call: " + ringingCall.toString());

        l = ringingCall.getConnections();
        for (int i = 0, s = l.size(); i < s; i++) {
            log(l.get(i).toString());
        }

        log("* Foreground call: " + foregroundCall.toString() + ", MPTY: " + foregroundCall.isMptyCall);

        l = foregroundCall.getConnections();
        for (int i = 0, s = l.size(); i < s; i++) {
            log(l.get(i).toString());
        }

        log("* Background call: " + backgroundCall.toString() + ", MPTY: " + backgroundCall.isMptyCall);

        l = backgroundCall.getConnections();
        for (int i = 0, s = l.size(); i < s; i++) {
            log(l.get(i).toString());
        }

        for (int i = 0, s = MAX_CONNECTIONS; i < s; i++) {
            if (connections[i] != null) {
                callId = connections[i].index + 1;	
                count ++;	
                log ("* conn id " + callId + " existed");
            }
        }
        log ("* GsmCT has " + count + " connection");
        /// @}
    }

    //***** Called from GsmConnection

    /*package*/ void
    hangup (GsmConnection conn) throws CallStateException {
        if (conn.owner != this) {
            throw new CallStateException ("GsmConnection " + conn
                                    + "does not belong to GsmCallTracker " + this);
        }

        if (conn == pendingMO) {
            // We're hanging up an outgoing call that doesn't have it's
            // GSM index assigned yet

            if (Phone.DEBUG_PHONE) log("hangup: set hangupPendingMO to true");
            hangupPendingMO = true;
	    hasPendingHangupRequest = false;
	    pendingHangupRequest = 0;
        } else {
            try {
                /// M: [mtk04070][111118][ALPS00093395]Obtain EVENT_HANG_UP_RESULT message.
                cm.hangupConnection (conn.getGSMIndex(), obtainCompleteMessage(EVENT_HANG_UP_RESULT));
            } catch (CallStateException ex) {
                // Ignore "connection not found"
                // Call may have hung up already
                /// M: [mtk04070][120103][ALPS00109412]Solve "Can't disconnect the VT call". @{
                //Merge from ALPS00093274
                hasPendingHangupRequest = false;
                pendingHangupRequest = 0;
                /// @}
                Log.w(LOG_TAG,"GsmCallTracker WARN: hangup() on absent connection "
                                + conn);
            }
        }

        conn.onHangupLocal();
    }

    /*package*/ void
    separate (GsmConnection conn) throws CallStateException {
        if (conn.owner != this) {
            throw new CallStateException ("GsmConnection " + conn
                                    + "does not belong to GsmCallTracker " + this);
        }
        try {
            cm.separateConnection (conn.getGSMIndex(),
                obtainCompleteMessage(EVENT_SEPARATE_RESULT));
            /// M: [mtk04070][111118][ALPS00093395]MTK added.
            crssAction = CrssAction.SEPERATE;
        } catch (CallStateException ex) {
            // Ignore "connection not found"
            // Call may have hung up already
            Log.w(LOG_TAG,"GsmCallTracker WARN: separate() on absent connection "
                          + conn);
        }
    }

    //***** Called from GSMPhone

    /*package*/ void
    setMute(boolean mute) {
        desiredMute = mute;
        /// M: [mtk04070][111118][ALPS00093395]Mute should not be handled here.
        /*cm.setMute(desiredMute, null);*/
    }

    /*package*/ boolean
    getMute() {
        return desiredMute;
    }


    //***** Called from GsmCall

    /* package */ void
    hangup (GsmCall call) throws CallStateException {
        /// M: [mtk04070][111118][ALPS00093395]MTK modified. @{
        if (call.getConnections().size() == 0) {
            throw new CallStateException("no connections in call");
        }

        if (hasPendingHangupRequest) {
            Log.d(LOG_TAG, "hangup(GsmCall) hasPendingHangupRequest = true");
            GsmConnection cn;
            if (foregroundCall.state == Call.State.DISCONNECTING) {
                for (int i = 0; i < foregroundCall.connections.size(); i++) {
                    cn = (GsmConnection)foregroundCall.connections.get(i);
                    cm.forceReleaseCall(cn.index + 1, obtainCompleteMessage());
                }
            }
            if (backgroundCall.state == Call.State.DISCONNECTING) {
                for (int i = 0; i < backgroundCall.connections.size(); i++) {
                    cn = (GsmConnection)backgroundCall.connections.get(i);
                    cm.forceReleaseCall(cn.index + 1, obtainCompleteMessage());
                }
            }
            return;
        }

        if (call == ringingCall) {
            if (Phone.DEBUG_PHONE) log("(ringing) hangup waiting or background");
            /* Solve [ALPS00303482][SIMCOM][MT6575][Acer_C8][GCF][51.010-1][26.8.1.3.5.3], mtk04070, 20120628 */
            log("Hang up waiting or background call by connection index.");
            GsmConnection conn = (GsmConnection)(call.getConnections().get(0));
            cm.hangupConnection (conn.getGSMIndex(), obtainCompleteMessage());
            //cm.hangupWaitingOrBackground(obtainCompleteMessage());
        } else if (call == foregroundCall) {
            hasPendingHangupRequest = true;
            pendingHangupRequest++;
            if (call.isDialingOrAlerting()) {
                if (Phone.DEBUG_PHONE) log("(foregnd) hangup dialing/alerting");
                hangup((GsmConnection)(call.getConnections().get(0)));
            } else {
                if (Phone.DEBUG_PHONE) log("(foregnd) hangup active");
                hangupForegroundResumeBackground();
            }
        } else if (call == backgroundCall) {
            if (ringingCall.isRinging()) {
                if (Phone.DEBUG_PHONE) log("(backgnd) hangup all connections");
                hangupAllConnections(call);
            } else {
                hasPendingHangupRequest = true;
                pendingHangupRequest++;
                if (Phone.DEBUG_PHONE) log("(backgnd) hangup waiting/background");
                hangupWaitingOrBackground();
            }
        } else {
            throw new RuntimeException ("GsmCall " + call +
                    "does not belong to GsmCallTracker " + this);
        }

        call.onHangupLocal();
        phone.notifyPreciseCallStateChanged();
        /// @}
    }

    /* package */
    void hangupWaitingOrBackground() {
        if (Phone.DEBUG_PHONE) log("hangupWaitingOrBackground");
        /// M: [mtk04070][111118][ALPS00093395]Obtain EVENT_HANG_UP_RESULT message.
        cm.hangupWaitingOrBackground(obtainCompleteMessage(EVENT_HANG_UP_RESULT));
    }

    /* package */
    void hangupForegroundResumeBackground() {
        if (Phone.DEBUG_PHONE) log("hangupForegroundResumeBackground");
        /// M: [mtk04070][111118][ALPS00093395]Obtain EVENT_HANG_UP_RESULT message.
        cm.hangupForegroundResumeBackground(obtainCompleteMessage(EVENT_HANG_UP_RESULT));
    }

    void hangupConnectionByIndex(GsmCall call, int index)
            throws CallStateException {
        int count = call.connections.size();
        for (int i = 0; i < count; i++) {
            GsmConnection cn = (GsmConnection)call.connections.get(i);
            if (cn.getGSMIndex() == index) {
                cm.hangupConnection(index, obtainCompleteMessage());
                return;
            }
        }

        /// M: [mtk04070][111118][ALPS00093395]MTK added. @{
        count = backgroundCall.connections.size();
        for (int i = 0; i < count; i++) {
            GsmConnection cn = (GsmConnection)backgroundCall.connections.get(i);
            if (cn.getGSMIndex() == index) {
                cm.hangupConnection(index, obtainCompleteMessage());
                return;
            }
        }

        count = ringingCall.connections.size();
        for (int i = 0; i < count; i++) {
            GsmConnection cn = (GsmConnection)ringingCall.connections.get(i);
            if (cn.getGSMIndex() == index) {
                cm.hangupConnection(index, obtainCompleteMessage());
                return;
            }
        }
        /// @}

        throw new CallStateException("no gsm index found");
    }

    void hangupAllConnections(GsmCall call) throws CallStateException{
        try {
            int count = call.connections.size();
            for (int i = 0; i < count; i++) {
                GsmConnection cn = (GsmConnection)call.connections.get(i);
                cm.hangupConnection(cn.getGSMIndex(), obtainCompleteMessage());
            }
        } catch (CallStateException ex) {
            Log.e(LOG_TAG, "hangupConnectionByIndex caught " + ex);
        }
    }

    /* package */
    GsmConnection getConnectionByIndex(GsmCall call, int index)
            throws CallStateException {
        int count = call.connections.size();
        for (int i = 0; i < count; i++) {
            GsmConnection cn = (GsmConnection)call.connections.get(i);
            if (cn.getGSMIndex() == index) {
                return cn;
            }
        }

        return null;
    }

    private Phone.SuppService getFailedService(int what) {
        switch (what) {
            case EVENT_SWITCH_RESULT:
                return Phone.SuppService.SWITCH;
            case EVENT_CONFERENCE_RESULT:
                return Phone.SuppService.CONFERENCE;
            case EVENT_SEPARATE_RESULT:
                return Phone.SuppService.SEPARATE;
            case EVENT_ECT_RESULT:
                return Phone.SuppService.TRANSFER;
        }
        return Phone.SuppService.UNKNOWN;
    }

    //****** Overridden from Handler

    public void
    handleMessage (Message msg) {
        AsyncResult ar;

        switch (msg.what) {
            case EVENT_POLL_CALLS_RESULT:
                ar = (AsyncResult)msg.obj;

                if (msg == lastRelevantPoll) {
                    if (DBG_POLL) log(
                            "handle EVENT_POLL_CALL_RESULT: set needsPoll=F");
                    needsPoll = false;
                    lastRelevantPoll = null;
                    handlePollCalls((AsyncResult)msg.obj);
                }
            break;

            /// M: [mtk04070][111118][ALPS00093395]MTK added and modified. @{
            case EVENT_CALL_PROGRESS_INFO:
                log("handle EVENT_CALL_PROGRESS_INFO");
                handleCallProgressInfo((AsyncResult)msg.obj);
            break;

            case EVENT_OPERATION_COMPLETE:
                log("handle EVENT_OPERATION_COMPLETE");
                ar = (AsyncResult)msg.obj;
                updateMptyState();
                operationComplete();
                if (hasPendingReplaceRequest) {
                    hasPendingReplaceRequest = false;
                }
                /// M: Solve [ALPS00331442][Rose][MT6577][Free Test][phone]There is no response after you end the VT call.(Once). @{
                /* Root cause: When there is an active call and modem is reset(maybe by adb command), EVENT_RADIO_NOT_AVAILABLE event
                   is received and cm.getCurrentCalls() will be called. And then GsmCallTracker will handle polls calls when received 
                   EVENT_POLL_CALLS_RESULT. But occasionally, VT manager will hang up the active call due to modem reset, and
                   lastRelevantPoll variable is also set to null in obtainCompleteMessage(), so EVENT_POLL_CALLS_RESULT will be
                   ignored. 
                */
                if (isCommandExceptionRadioNotAvailable(ar.exception)) {
                   handleRadioNotAvailable();
                }
                /// @}
            break;

            case EVENT_SWITCH_RESULT:
                log("handle EVENT_SWITCH_RESULT");
                ar = (AsyncResult)msg.obj;
                if (ar.exception != null) {
                    if (isSwitchBeforeDial) {
                        /* Solve GB2 CR - ALPS00236842,
                           When get response of hold request, if hasPendingMO,
                           set hasPendingHangupRequest as false.
                           mtk04070, 2012.02.22 							 
                        */									   
                        updatePendingHangupRequest();
                        pendingMO.cause = Connection.DisconnectCause.LOCAL;
                        handleDialCallFailed();
                        internalClearDisconnected();
                        resumeBackgroundAfterDialFailed();
                        isSwitchBeforeDial = false;
                    }
                    phone.notifySuppServiceFailed(getFailedService(msg.what));
                }else {
                    if (isSwitchBeforeDial) {
                        if (PhoneNumberUtils.isEmergencyNumber(delayedDialString)
                            && !mPhoneNumberExt.isSpecialEmergencyNumber(delayedDialString)) {
                            cm.emergencyDial(delayedDialString, delayedClirMode, delayedUUSInfo, obtainCompleteMessage(EVENT_DIAL_CALL_RESULT));
                        } else {
                            cm.dial(delayedDialString, delayedClirMode, delayedUUSInfo, obtainCompleteMessage(EVENT_DIAL_CALL_RESULT));
                        }
                        isSwitchBeforeDial = false;
                        delayedDialString = null;
                        delayedClirMode = 0;
                        delayedUUSInfo = null;
                    }
                }

                updateMptyState();
                crssAction = CrssAction.NONE;
                isPendingSwap = false;
                log("notify precise call state changed");
                phone.notifyPreciseCallStateChanged();
                operationComplete();
            break;

            case EVENT_CONFERENCE_RESULT:
                log("handle EVENT_CONFERENCE_RESULT");
                updateMptyState();
                ar = (AsyncResult)msg.obj;
                if (ar.exception != null) {
                    phone.notifySuppServiceFailed(getFailedService(msg.what));
                }
                /*let application to update conference screen when operation completed*/
                phone.notifyPreciseCallStateChanged();
                operationComplete();
            break;

            case EVENT_SEPARATE_RESULT:
                log("handle EVENT_SEPARATE_RESULT");
                updateMptyState();
                ar = (AsyncResult)msg.obj;
                if (ar.exception != null) {
                    phone.notifySuppServiceFailed(getFailedService(msg.what));
                }
                crssAction = CrssAction.NONE;
                operationComplete();
            break;

            case EVENT_ECT_RESULT:
                ar = (AsyncResult)msg.obj;
                if (ar.exception != null) {
                    phone.notifySuppServiceFailed(getFailedService(msg.what));
                }
                operationComplete();
            break;

            case EVENT_DIAL_CALL_RESULT:
                log("handle EVENT_DIAL_CALL_RESULT");
                ar = (AsyncResult)msg.obj;
                if (ar.exception != null) {
                    log("dial call failed!!");
                    handleDialCallFailed();
                }
                operationComplete();
            break;

            case EVENT_GET_LAST_CALL_FAIL_CAUSE:
                log("handle EVENT_GET_LAST_CALL_FAIL_CAUSE");
                int FgDiscConn = 0;
                int BgDiscConn = 0;

                ar = (AsyncResult)msg.obj;

                operationComplete();

                if (ar.exception != null) {
                    // An exception occurred...just treat the disconnect
                    // cause as "normal"
                    causeCode = CallFailCause.NORMAL_CLEARING;
                    Log.i(LOG_TAG,
                            "Exception during getLastCallFailCause, assuming normal disconnect");
                } else {
                    causeCode = ((int[])ar.result)[0];
                }
                // Log the causeCode if its not normal
                if (causeCode == CallFailCause.NO_CIRCUIT_AVAIL ||
                    causeCode == CallFailCause.TEMPORARY_FAILURE ||
                    causeCode == CallFailCause.SWITCHING_CONGESTION ||
                    causeCode == CallFailCause.CHANNEL_NOT_AVAIL ||
                    causeCode == CallFailCause.QOS_NOT_AVAIL ||
                    causeCode == CallFailCause.BEARER_NOT_AVAIL ||
                    causeCode == CallFailCause.ERROR_UNSPECIFIED) {
                    GsmCellLocation loc = ((GsmCellLocation)phone.getCellLocation());
                    EventLog.writeEvent(EventLogTags.CALL_DROP,
                            causeCode, loc != null ? loc.getCid() : -1,
                            TelephonyManager.getDefault().getNetworkType());
                }

                for (int i = 0, s =  droppedDuringPoll.size(); i < s ; i++) {
                    GsmConnection conn = droppedDuringPoll.get(i);
                    conn.onRemoteDisconnect(causeCode);
                }

                updatePhoneState();

                for (int j = foregroundCall.connections.size() - 1 ; j >= 0 ; j--) {
                    GsmConnection cn = (GsmConnection)(foregroundCall.connections.get(j));

                    if (cn.getState() == GsmCall.State.DISCONNECTED) {
                        FgDiscConn++;
                    }
                }

                if(foregroundCall.connections.size() <= 1 ||
                   (foregroundCall.connections.size() > 1 && (foregroundCall.connections.size() - FgDiscConn) <= 1))
                {
                    foregroundCall.isMptyCall = false;
                }

                for (int k = backgroundCall.connections.size() - 1 ; k >= 0 ; k--) {
                    GsmConnection cn = (GsmConnection)(backgroundCall.connections.get(k));

                    if (cn.getState() == GsmCall.State.DISCONNECTED) {
                        BgDiscConn++;
                    }
                }

                if (backgroundCall.connections.size() <= 1 ||
                    (backgroundCall.connections.size() > 1 && (backgroundCall.connections.size() - BgDiscConn) <= 1))
                {
                    backgroundCall.isMptyCall = false;
                }

                phone.notifyPreciseCallStateChanged();
                droppedDuringPoll.clear();
            break;

            case EVENT_HANG_UP_RESULT:
                log("handle EVENT_HANG_UP_RESULT");
                if (hasPendingHangupRequest) {
                    pendingHangupRequest--;
                    if (pendingHangupRequest ==0) {
                        hasPendingHangupRequest = false;
                    }
                }
                updateMptyState();
                operationComplete();
            break;

            case EVENT_INCOMING_CALL_INDICATION:
                log("handle EVENT_INCOMING_CALL_INDICATION");
                handleIncomingCallIndication((AsyncResult)msg.obj);
            break;

            case EVENT_RADIO_NOT_AVAILABLE:
                handleRadioNotAvailable();
            break;

            default:
                break;
            /// @}
        }
    }

    protected void log(String msg) {
        /// M: [mtk04070][111118][ALPS00093395]MTK modified.
        Log.d(LOG_TAG, "[CC][GsmCT][SIM" + (phone.getMySimId()+ 1) +"] " + msg);
    }

    @Override
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("GsmCallTracker extends:");
        super.dump(fd, pw, args);
        pw.println("connections: length=" + connections.length);
        for(int i=0; i < connections.length; i++) {
            pw.printf("  connections[%d]=%s\n", i, connections[i]);
        }
        pw.println(" voiceCallEndedRegistrants=" + voiceCallEndedRegistrants);
        pw.println(" voiceCallStartedRegistrants=" + voiceCallStartedRegistrants);
        pw.println(" droppedDuringPoll: size=" + droppedDuringPoll.size());
        for(int i = 0; i < droppedDuringPoll.size(); i++) {
            pw.printf( "  droppedDuringPoll[%d]=%s\n", i, droppedDuringPoll.get(i));
        }
        pw.println(" ringingCall=" + ringingCall);
        pw.println(" foregroundCall=" + foregroundCall);
        pw.println(" backgroundCall=" + backgroundCall);
        pw.println(" pendingMO=" + pendingMO);
        pw.println(" hangupPendingMO=" + hangupPendingMO);
        pw.println(" phone=" + phone);
        pw.println(" desiredMute=" + desiredMute);
        pw.println(" state=" + state);
    }

    /// M: [mtk04070][111118][ALPS00093395]MTK proprietary methods. @{
    private void
    resumeBackgroundAfterDialFailed() {
        List<Connection> connCopy;

        connCopy = (List<Connection>) backgroundCall.connections.clone();

        for (int i = 0, s = connCopy.size() ; i < s ; i++) {
            GsmConnection conn = (GsmConnection)connCopy.get(i);

            conn.resumeHoldAfterDialFailed();
        }
    }

    /* vt start */
    /**
     * clirMode is one of the CLIR_ constants
     */
    Connection
    vtDial (String dialString, int clirMode, UUSInfo uusInfo) throws CallStateException {
        // note that this triggers call state changed notif
        clearDisconnected();

        if (!canDial()) {
            throw new CallStateException("cannot dial VT call in current state");
        }

        if (state != PhoneConstants.State.IDLE) {
            throw new CallStateException("can only dial VT call in idle state");
        }

        pendingMO = new GsmConnection(phone.getContext(), dialString, this, foregroundCall);
        pendingMO.isVideo = true;
        hangupPendingMO = false;

        if (pendingMO.address == null || pendingMO.address.length() == 0
            || pendingMO.address.indexOf(PhoneNumberUtils.WILD) >= 0
        ) {
            // Phone number is invalid
            pendingMO.cause = Connection.DisconnectCause.INVALID_NUMBER;

            // handlePollCalls() will notice this call not present
            // and will mark it as dropped.
            // pollCallsWhenSafe();
        } else {
            int len = dialString.length();
            int i = 0;
            StringBuilder ret = new StringBuilder(len);
            boolean firstCharAdded = false;

            for (i = 0; i < len; i++) {
                char c = dialString.charAt(i);

                if (c == PhoneNumberUtils.PAUSE) {
                    ret.append('p');
                } else if (c == PhoneNumberUtils.WAIT) {
                    ret.append('w');
                } else {
                    if (c != '+' || !firstCharAdded) {
                        firstCharAdded = true;
                        ret.append(c);
                    }
                }                   
            }

            if (PhoneNumberUtils.isEmergencyNumber(dialString)
                && !mPhoneNumberExt.isSpecialEmergencyNumber(dialString)) {
                cm.emergencyDial(ret.toString(), clirMode, uusInfo, obtainCompleteMessage(EVENT_DIAL_CALL_RESULT));
            } else {
                cm.vtDial(ret.toString(), clirMode, uusInfo, obtainCompleteMessage(EVENT_DIAL_CALL_RESULT));
            }
        }

        updatePhoneState();
        phone.notifyPreciseCallStateChanged();

        return pendingMO;
    }

    Connection
    vtDial (String dialString) throws CallStateException {
        return vtDial(dialString, CommandsInterface.CLIR_DEFAULT, null);
    }

    Connection
    vtDial (String dialString, UUSInfo uusInfo) throws CallStateException {
        return vtDial(dialString, CommandsInterface.CLIR_DEFAULT, uusInfo);
    }

    Connection
    vtDial(String dialString, int clirMode) throws CallStateException {
        return vtDial(dialString, clirMode, null);
    }

    void voiceAccept() throws CallStateException {
        if (Phone.DEBUG_PHONE) log("voiceAccept");
        if (!ringingCall.isIdle()) {
            GsmConnection cn = (GsmConnection)ringingCall.connections.get(0);
            cm.voiceAccept(cn.getGSMIndex(),obtainCompleteMessage());
        }
    }
    /* vt end */

    private void
    updateMptyState() {
        if(foregroundCall.connections.size() > 1){
            if(foregroundCall.getState() != GsmCall.State.DIALING)
                foregroundCall.isMptyCall = true;
        }
        else
            foregroundCall.isMptyCall = false;

        if(backgroundCall.connections.size() > 1)
            backgroundCall.isMptyCall = true;
        else
            backgroundCall.isMptyCall = false;

        log("FG call is MPTY? " + foregroundCall.isMptyCall +
            ", BG call is MPTY? " + backgroundCall.isMptyCall);
    }

    protected void
    handleCallProgressInfo(AsyncResult ar) {

        String[] CallProgressInfo = (String[]) ar.result;
        int callId = Integer.parseInt(CallProgressInfo[0]);
        int msgType = Integer.parseInt(CallProgressInfo[1]);
        int dir = 0xff;
        int callMode = 0xff;
        String number = null;
        int toa = 0xff; 
        GsmCall oldParent = null;

        Connection newRinging = null; //or waiting
        boolean hasNonHangupStateChanged = false;   // Any change besides
        boolean unknownConnectionAppeared = false;
        boolean localHangup = false;

        /* +ECPI:<call_id>, <msg_type>, <is_ibt>, <is_tch>, <dir>, <call_mode>[, <number>, <toa>], "",<cause>
          *
          * if msg_type = DISCONNECT_MSG or ALL_CALLS_DISC_MSG,
          * +ECPI:<call_id>, <msg_type>, <is_ibt>, <is_tch>,,,"",,"",<cause> 
          * 
          * if msg_type = STATE_CHANGE_HELD or STATE_CHANGE_ACTIVE or STATE_CHANGE_DISCONNECTED,
          * +ECPI:<call_id>, <msg_type>,,,,,"",,""
          *
          * if others, 
          * +ECPI:<call_id>, <msg_type>, <is_ibt>, <is_tch>, <dir>, <call_mode>[, <number>, <toa>], ""
          *
          *     0  O  CSMCC_SETUP_MSG
          *     1  X  CSMCC_DISCONNECT_MSG
          *     2  O  CSMCC_ALERT_MSG
          *     3  X  CSMCC_CALL_PROCESS_MSG
          *     4  X  CSMCC_SYNC_MSG
          *     5  X  CSMCC_PROGRESS_MSG
          *     6  O  CSMCC_CALL_CONNECTED_MSG
          *   129  X  CSMCC_ALL_CALLS_DISC_MSG
          *   130  O  CSMCC_MO_CALL_ID_ASSIGN_MSG
          *   131  O  CSMCC_STATE_CHANGE_HELD
          *   132  O  CSMCC_STATE_CHANGE_ACTIVE
          *   133  O  CSMCC_STATE_CHANGE_DISCONNECTED
          *   134  X  CSMCC_STATE_CHANGE_MO_DISCONNECTING
          */

        //callId = 254  --> id assigned failed
        if(msgType == 0 || msgType == 2 || msgType == 6 || (msgType == 130 && callId != 254) || 
           msgType == 131 || msgType == 132 ||msgType == 133)
        {
            if (isSwitchBeforeDial && (msgType == 131 || msgType == 132)) {
                return;
            }

            int i = callId-1;
            if (i < 0 || i > 6) {
                log("Error caller id. i = " + i);
                return;
            }
            GsmConnection conn = connections[i];
            DriverCall dc = new DriverCall();
            int count = 0;
            int j = 0;

            if (msgType == 0 || msgType == 2 || msgType == 6 || msgType == 130)
            {
                dir = Integer.parseInt(CallProgressInfo[4]);
                callMode = Integer.parseInt(CallProgressInfo[5]);

                if (CallProgressInfo[6] != null && CallProgressInfo[6].length() != 0)
                {
                    number = CallProgressInfo[6];

                    if (CallProgressInfo[7] != null && CallProgressInfo[7].length() != 0)
                        toa = Integer.parseInt(CallProgressInfo[7]);
                }
            }

            log("id=" + callId + ", msg=" + msgType + ", dir=" + dir + 
                ", mode=" + callMode + ", number=\"" + number + "\", toa=" + toa);

            log("========dump start========");
            dumpState();
            log("========dump start========");

            /*compose DriverCall contents: start*/
            dc.index = callId;

            dc.isMT = (dir != 0);

            if (msgType == 132 || msgType == 6)
                dc.state = DriverCall.State.ACTIVE;
            else if (msgType == 131)
                dc.state = DriverCall.State.HOLDING;
            else if (msgType == 130 && callId != 254)
                dc.state = DriverCall.State.DIALING;
            else if (msgType == 2)
                dc.state = DriverCall.State.ALERTING;
            else if (msgType == 0)
            {
                for (j = 0; j < MAX_CONNECTIONS; j++) {
                    if (connections[j] != null) {
                        count ++;
                    }
                }

                if (state == PhoneConstants.State.IDLE || 
                    (count == 0 &&  foregroundCall.getState() == GsmCall.State.DIALING))
                {
                    /* if the 2nd condition is true, that means we make a MO call, receiving +ECPI: 130, 
                    * then receiving +ECPI: 133 immediately due to MT call (+ECPI: 0) is receiving*/
                    if (count == 0 &&  foregroundCall.getState() == GsmCall.State.DIALING)
                        log("MO/MT conflict!!");

                    dc.state = DriverCall.State.INCOMING;
                }
                else
                    dc.state = DriverCall.State.WAITING;
            }

            /* vt start */
            if (msgType != 131 && msgType != 132 && msgType != 133) {
                if (callMode == 0) {
                    dc.isVoice = true;
                    dc.isVideo = false;
                }
                else if (callMode == 10) {
                    dc.isVoice = false;
                    dc.isVideo = true;
                }
                else {
                    dc.isVoice = false;
                    dc.isVideo = false;
                }
            }
            /* vt end */

            dc.number = number;
            dc.numberPresentation = PhoneConstants.PRESENTATION_ALLOWED;
            dc.TOA = toa;
            dc.number = PhoneNumberUtils.stringFromStringAndTOA(dc.number, dc.TOA);
            /*compose DriverCall contents: end*/

            if (conn == null) 
            {
                log("1. new connection appeared!!");

                if (pendingMO != null)
                {
                    if (msgType == 130)
                    {
                        log("1.1. it is a MO call");

                        connections[i] = pendingMO;
                        pendingMO.index = i;
                        pendingMO.update(dc);
                        pendingMO = null;

                        // Someone has already asked to hangup this call
                        if (hangupPendingMO) {
                            hangupPendingMO = false;
                            try {
                                log("poll: hangupPendingMO, hangup conn " + i);
                                hangup(connections[i]);
                            } catch (CallStateException ex) {
                                log("unexpected error on hangup");
                            }

                            // Do not continue processing this poll
                            // Wait for hangup and repoll
                            return;
                        }
                    }
                    else if (msgType == 133)
                    {
                        log("Pending MO dropped before call id assigned!! fg state:" + foregroundCall.getState());

                        droppedDuringPoll.add(pendingMO);
                        pendingMO = null;
                        hangupPendingMO = false;
                    }
                    else if (msgType == 0)
                    {
                        log("MO/MT conflict! Hang up MT call to prevent abnormal behavior.");

                        /// M: Solve [ALPS00329045]MO call failed due to MT call incoming. @{
                        /* MO call failed due to MT call is incoming, also hang up MT call to prevent abnormal behavior. */
                        connections[i] = new GsmConnection(phone.getContext(), dc, this, i);
                        try {
                            hangup(connections[i]);
                        } catch (CallStateException ex) {
                            Log.e(LOG_TAG, "unexpected error on hangup");
                        }
                        /// @}
                        /*TBD: MO call failed due to MT call is incoming*/
                        /*
                        if (connections[i].getCall() == ringingCall) {
                            newRinging = connections[i];
                        }
                        */
                    }
                }
                else if (msgType == 0) 
                {
                    log("1.2 it is a MT call");

                    connections[i] = new GsmConnection(phone.getContext(), dc, this, i);

                    // MTK_OPTR_PROTECT_START
                    /* DM start */
                    log("isInLock = " + isInLock + ", isFullLock = " + isFullLock);
                    if (isInLock && isFullLock) {
                        log("hang up MT call because of in DM lock state");
                        try {
                            hangup(connections[i]);
                        } catch (CallStateException ex) {
                            Log.e(LOG_TAG, "unexpected error on hangup");
                        }
                    } else {
                    /* DM end */
                    // MTK_OPTR_PROTECT_END
                        if (connections[i].getCall() == ringingCall) {
                            newRinging = connections[i];
                        } else {
                            // Something strange happened: a call appeared
                            // which is neither a ringing call or one we created.
                            // Either we've crashed and re-attached to an existing
                            // call, or something else (eg, SIM) initiated the call.
                            log("Phantom call appeared!!");

                            // If it's a connected call, set the connect time so that
                            // it's non-zero.  It may not be accurate, but at least
                            // it won't appear as a Missed Call.
                            if (dc.state != DriverCall.State.ALERTING
                                    && dc.state != DriverCall.State.DIALING) {
                                connections[i].connectTime = System.currentTimeMillis();
                            }

                            unknownConnectionAppeared = true;
                        }
                    // MTK_OPTR_PROTECT_START
                    /* DM start */
                    }
                    /* DM end */
                    // MTK_OPTR_PROTECT_END
                }
                else
                {
                    if (msgType == 130)
                    {
                        log("1.3 Phantom call appeared!!");

                        connections[i] = new GsmConnection(phone.getContext(), dc, this, i);
                        unknownConnectionAppeared = true;
                    }
                }
                hasNonHangupStateChanged = true;
            }
            else /*conn != null*/
            {
                oldParent = conn.getCall();
                if (msgType == 133)
                {
                    log("2. connection disconnected");

                    if (((conn.getCall() == foregroundCall && foregroundCall.connections.size() == 1 && backgroundCall.isIdle()) ||
                         (conn.getCall() == backgroundCall && backgroundCall.connections.size() == 1 && foregroundCall.isIdle())) &&
                         ringingCall.getState() == GsmCall.State.WAITING)
                        ringingCall.state = GsmCall.State.INCOMING;

                    droppedDuringPoll.add(conn);
                    // Dropped connections are removed from the CallTracker
                    // list but kept in the GsmCall list
                    connections[i] = null;

                    /* AGPS start */
                    if (FeatureOption.MTK_AGPS_APP) {
                        String string = conn.address;
                        if (PhoneNumberUtils.isEmergencyNumber(string) && hasSendAgpsStartIntent) {
                            Bundle mbundle = new Bundle();
                            mbundle.putInt("EM_Call_State", 0);
                            mbundle.putString("Call_Number", string);
                            Intent intent = new Intent("android.location.agps.EMERGENCY_CALL");
                            intent.putExtras(mbundle);
                            mContext.sendBroadcast(intent);
                            hasSendAgpsStartIntent = false;
                            log("Broadcast ecc end intent for AGPS");
                        }
                    }
                    /* AGPS end */
                }
                else 
                {
                    boolean changed;

                    log("3. update current connection[i="+ i +"]");
                    if (dc.number == null) {
                        dc.number = conn.address;
                    }

                    //MTK-START [mtk04070][120104][ALPS00109412]Solve "[VT]Sound can't be heard during VT call"
                    //Merge from ALPS00100904
                    if (callMode == 0xff) {
                        dc.isVideo = conn.isVideo();
                    }
                    //MTK-END [mtk04070][120104][ALPS00109412]Solve "[VT]Sound can't be heard during VT call"
					
                    changed = conn.update(dc);
                    hasNonHangupStateChanged = hasNonHangupStateChanged || changed;
                }
            }

            if (newRinging != null) {
                log("notify CallNotifier for New Ringing connection");
                phone.notifyNewRingingConnection(newRinging);
            }

            // clear the "local hangup" and "missed/rejected call"
            // cases from the "dropped during poll" list
            // These cases need no "last call fail" reason
            log("dropped during poll size = " + droppedDuringPoll.size());
            for (int n = droppedDuringPoll.size() - 1; n >= 0 ; n--) {
                GsmConnection dropConn = droppedDuringPoll.get(n);

                if (dropConn.isIncoming() && dropConn.getConnectTime() == 0) {
                    // Missed or rejected call
                    Connection.DisconnectCause cause;
                    if (dropConn.cause == Connection.DisconnectCause.LOCAL) {
                        cause = Connection.DisconnectCause.INCOMING_REJECTED;
                    } else {
                        cause = Connection.DisconnectCause.INCOMING_MISSED;
                    }

                    log("1. missed/rejected call, conn.cause=" + dropConn.cause);
                    log("1. setting cause to " + cause);

                    droppedDuringPoll.remove(n);
                    dropConn.onDisconnect(cause);
                    localHangup = true;
                } else if (dropConn.cause == Connection.DisconnectCause.LOCAL) {
                    // Local hangup
                    if (!hasPendingReplaceRequest || (hasPendingReplaceRequest && msgType != 133)) {
                        log("2. local hangup");
                        droppedDuringPoll.remove(n);
                        dropConn.onDisconnect(Connection.DisconnectCause.LOCAL);
                        localHangup = true;
                    }
                } else if (dropConn.cause == Connection.DisconnectCause.INVALID_NUMBER) {
                    log("3. invalid number");
                    droppedDuringPoll.remove(n);
                    dropConn.onDisconnect(Connection.DisconnectCause.INVALID_NUMBER);
                    localHangup = true;
                }
                 causeCode = 16;
            }

            // Any non-local disconnects: determine cause
            if (droppedDuringPoll.size() > 0 && !hasPendingReplaceRequest) {
                log("get last call failed cause");
                cm.getLastCallFailCause(
                    obtainNoPollCompleteMessage(EVENT_GET_LAST_CALL_FAIL_CAUSE));
            } else if (localHangup == false && msgType == 133) {
                // [ALPS00426486] Add by mtk01411: If msgType is 133 but not localHangup or get Last Call Fail Cause:
                // This gsmConnection & its corresponding call object will not have chance to clear their states correctly
                log("msgType=133:Other case:CallId=" + callId + " ,but not ivoke onDisconnect() of GsmConnection");
            }
            
            // [ALPS00426486] Add by mtk01411: For final check - If msgType is 133 but not localHangup or get Last Call Fail Cause:
            // This gsmConnection & its corresponding call object will not have chance to clear their states correctly
            // If the following codes are not added, the connection stored in droppedDuringPoll will be cleaned untill receiving next +ECPI
            // This scenario may be dangerous: It means that CallManager will maintain wrong state and call list for a while
            if ((msgType == 133) && (droppedDuringPoll.size() > 0) && hasPendingReplaceRequest && (localHangup == false)) {
                for (int m = droppedDuringPoll.size() - 1; m >= 0 ; m--) {
                    GsmConnection dropConnX = droppedDuringPoll.get(m);
                    log("4. Force to invoke onDisconnect() of GsmConnection with callID=" + callId + ", with cause=" + dropConnX.cause);					
                    droppedDuringPoll.remove(m);
                    dropConnX.onDisconnect(dropConnX.cause);
                }
            }

            //if(msgType == 133 && localHangup == false) {
            //    conn.index = -1;
            //    conn.disconnectTime = System.currentTimeMillis();
            //    conn.duration = SystemClock.elapsedRealtime() - conn.connectTimeReal;
            //    conn.disconnected = true;
            //    if (conn.parent != null) {
            //        conn.parent.connectionDisconnected(conn);
            //    }
            //}

            // Cases when we can no longer keep disconnected Connection's
            // with their previous calls
            // 1) the phone has started to ring
            // 2) A Call/Connection object has changed state...
            //    we may have switched or held or answered (but not hung up)
            if (newRinging != null || hasNonHangupStateChanged) {
                log("internal clear disconnected");
                internalClearDisconnected();
            }

            updatePhoneState();

            if (msgType == 133)
            {
                //updateMptyState();
                if(oldParent == foregroundCall && foregroundCall.connections.size() <= 1)
                {
                    foregroundCall.isMptyCall = false;
                }
                else if (oldParent == backgroundCall && backgroundCall.connections.size() <= 1)
                {
                    backgroundCall.isMptyCall = false;
                }

                /* To adjust PhoneAPP priority to normal, mtk04070, 20120307 */
                int pid = Process.myPid();
                if (Process.getThreadPriority(pid) != Process.THREAD_PRIORITY_DEFAULT) {
                   Process.setThreadPriority(pid, Process.THREAD_PRIORITY_DEFAULT);
                   log("Current priority = " + Process.getThreadPriority(pid));
                }
            }

            if (msgType == 131 && crssAction == CrssAction.SEPERATE && foregroundCall.connections.size() <= 1)
            {
                foregroundCall.isMptyCall = false;
            }

            if (unknownConnectionAppeared) {
                log("notity unknown connection");
                phone.notifyUnknownConnection();
            }

            if ((hasNonHangupStateChanged || newRinging != null) && crssAction != CrssAction.SWAP && !hasPendingReplaceRequest) {
                log("notify precise call state changed");
                phone.notifyPreciseCallStateChanged();
            }

            /* Solve ALPS00401290 */
            if ((getCurrentTotalConnections() == 1) &&
                (ringingCall.getState() == GsmCall.State.WAITING)) {
               ringingCall.state = GsmCall.State.INCOMING;
            }

            log("=========dump end=========");
            dumpState();
            log("=========dump end=========");

        } else if (msgType == 130 && callId == 254) {
            if (pendingMO != null) {
                log("id=" + callId + ", msg=" + msgType);
                pendingMO.onDisconnect(Connection.DisconnectCause.ERROR_UNSPECIFIED);
                updatePhoneState();
                updateMptyState();

                log("notify precise call state changed");
                phone.notifyPreciseCallStateChanged();

                log("========dump start========");
                dumpState();
                log("========dump stop========");

                pendingMO = null;
            }
        }
    }

    protected void
    handleDialCallFailed() {

        dumpState();

        /* AGPS start */
        if (FeatureOption.MTK_AGPS_APP) {
            if (pendingMO != null) {
                String string = pendingMO.address;
                if (PhoneNumberUtils.isEmergencyNumber(string) && hasSendAgpsStartIntent) {
                    Bundle mbundle = new Bundle();
                    mbundle.putInt("EM_Call_State", 0);
                    mbundle.putString("Call_Number", string);
                    Intent intent = new Intent("android.location.agps.EMERGENCY_CALL");
                    intent.putExtras(mbundle);
                    mContext.sendBroadcast(intent);
                    hasSendAgpsStartIntent = false;
                    log("Broadcast ecc end intent for AGPS");
                }
            }
            else {
               hasSendAgpsStartIntent = false;
            }        }
        /* AGPS end */

        /* Solve [ALPS00252152]You cannot end the call by pressing call icon.
           The root cause is that hasPendingHangupRequest is not reset to false.
        */
        log("handleDialCallFailed - updatePendingHangupRequest");        
        updatePendingHangupRequest();
        
        if (pendingMO != null) {
            droppedDuringPoll.add(pendingMO);
            pendingMO = null;
        }
        hangupPendingMO = false;

        // clear the "local hangup" and "missed/rejected call"
        // cases from the "dropped during poll" list
        // These cases need no "last call fail" reason
        log("dropped during poll size = " + droppedDuringPoll.size());
        for (int n = droppedDuringPoll.size() - 1; n >= 0 ; n--) {
            GsmConnection dropConn = droppedDuringPoll.get(n);

            if (dropConn.isIncoming() && dropConn.getConnectTime() == 0) {
                // Missed or rejected call
                Connection.DisconnectCause cause;
                if (dropConn.cause == Connection.DisconnectCause.LOCAL) {
                    cause = Connection.DisconnectCause.INCOMING_REJECTED;
                } else {
                    cause = Connection.DisconnectCause.INCOMING_MISSED;
                }

                log("1. missed/rejected call, conn.cause=" + dropConn.cause);
                log("1. setting cause to " + cause);

                droppedDuringPoll.remove(n);
                dropConn.onDisconnect(cause);
            } else if (dropConn.cause == Connection.DisconnectCause.LOCAL) {
                // Local hangup
                log("2. local hangup");
                droppedDuringPoll.remove(n);
                dropConn.onDisconnect(Connection.DisconnectCause.LOCAL);
            } else if (dropConn.cause ==
                Connection.DisconnectCause.INVALID_NUMBER) {
                log("3. invalid number");
                droppedDuringPoll.remove(n);
                dropConn.onDisconnect(Connection.DisconnectCause.INVALID_NUMBER);
            }
        }

        // Any non-local disconnects: determine cause
        if (droppedDuringPoll.size() > 0) {
            log("non-local disconnect");
            cm.getLastCallFailCause(
                obtainNoPollCompleteMessage(EVENT_GET_LAST_CALL_FAIL_CAUSE));
        }

        updatePhoneState();

        // phone.notifyPreciseCallStateChanged();

        dumpState();

    }

    private void
    handleIncomingCallIndication(AsyncResult ar) {
        int mode = 0;
        String[] incomingCallInfo = (String[]) ar.result;
        int callId = Integer.parseInt(incomingCallInfo[0]);
        int callMode = Integer.parseInt(incomingCallInfo[3]);
        int seqNumber = Integer.parseInt(incomingCallInfo[4]);

        if (state == PhoneConstants.State.RINGING) {
            mode = 1;
        } else if (state == PhoneConstants.State.OFFHOOK) {
            if (callMode == 10) {
                // incoming VT call
                for (int i = 0; i < MAX_CONNECTIONS; i++) {
                    Connection cn = connections[i];
                    if (cn != null && !cn.isVideo()) {
                        mode = 1;
                        break;
                    }
                }
            } else if (callMode == 0) {
                // incoming voice call
                for (int i = 0; i < MAX_CONNECTIONS; i++) {
                    Connection cn = connections[i];
                    if (cn != null && cn.isVideo()) {
                        mode = 1;
                        break;
                    }
                }
            } else {
                mode = 1;
            }
        }

        /* To raise PhoneAPP priority to avoid delaying incoming call screen to be showed, mtk04070, 20120307 */
        if (mode == 0) {
            
            pendingMTCallId = callId;
            pendingMTSeqNum = seqNumber;
            voiceCallIncomingIndicationRegistrants.notifyRegistrants();
            log("notify voiceCallIncomingIndicationRegistrants " + pendingMTCallId + " " + pendingMTSeqNum);          
        }

        if (mode == 1) {
            DriverCall dc = new DriverCall();
            dc.isMT = true;
            dc.index = callId;
            dc.state = DriverCall.State.WAITING;

            cm.setCallIndication(mode, callId, seqNumber, obtainCompleteMessage());
            
            if (callMode == 0) {
                dc.isVoice = true;
                dc.isVideo = false;
            }
            else if (callMode == 10) {
                dc.isVoice = false;
                dc.isVideo = true;
            }
            else {
                dc.isVoice = false;
                dc.isVideo = false;
            }
            dc.number = incomingCallInfo[1];
            dc.numberPresentation = PhoneConstants.PRESENTATION_ALLOWED;
            dc.TOA = Integer.parseInt(incomingCallInfo[2]);
            dc.number = PhoneNumberUtils.stringFromStringAndTOA(dc.number, dc.TOA);

            GsmConnection cn = new GsmConnection(phone.getContext(), dc, this, callId);
            cn.onReplaceDisconnect(Connection.DisconnectCause.INCOMING_MISSED);
        }
    }

    void hangupAll() {
        if (Phone.DEBUG_PHONE) log("hangupAll");
        cm.hangupAll(obtainCompleteMessage());
    }

    void hangupAllEx() {
        if (Phone.DEBUG_PHONE) log("hangupAllEx");
        cm.hangupAllEx(obtainCompleteMessage());
    }

    void hangupActiveCall() throws CallStateException {
        if (Phone.DEBUG_PHONE) log("hangupActiveCall");

        if (foregroundCall.getConnections().size() == 0) {
            throw new CallStateException("no connections in call");
        }

        if (pendingMO != null) {
            hangupPendingMO = true;
        } else {
            hangupAllConnections(foregroundCall);
        }
        /* ALPS00023537 */
        foregroundCall.onHangupLocal();
    }

    void getCurrentCallMeter(Message result) {
        if (Phone.DEBUG_PHONE) log("getCurrentCallMeter");
        cm.getCurrentCallMeter(result);
    }

    void getAccumulatedCallMeter(Message result) {
        if (Phone.DEBUG_PHONE) log("getAccumulatedCallMeter");
        cm.getAccumulatedCallMeter(result);
    }

    void getAccumulatedCallMeterMaximum(Message result) {
        if (Phone.DEBUG_PHONE) log("getAccumulatedCallMeterMaximum");
        cm.getAccumulatedCallMeterMaximum(result);
    }

    void getPpuAndCurrency(Message result) {
        if (Phone.DEBUG_PHONE) log("getPpuAndCurrency");
        cm.getPpuAndCurrency(result);
    }

    void setAccumulatedCallMeterMaximum(String acmmax, String pin2, Message result) {
        if (Phone.DEBUG_PHONE) log("setAccumulatedCallMeterMaximum");
        cm.setAccumulatedCallMeterMaximum(acmmax, pin2, result);
    }

    void resetAccumulatedCallMeter(String pin2, Message result) {
        if (Phone.DEBUG_PHONE) log("resetAccumulatedCallMeter");
        cm.resetAccumulatedCallMeter(pin2, result);
    }

    void setPpuAndCurrency(String currency, String ppu, String pin2, Message result) {
        if (Phone.DEBUG_PHONE) log("setPpuAndCurrency");
        String cur = "";
        for (int i = 0; i < currency.length(); i++) {
            cur += "00" + Integer.toHexString(currency.codePointAt(i));
        }
        cm.setPpuAndCurrency(cur, ppu, pin2, result);
    }

    public void setIncomingCallIndicationResponse(boolean accept) {
        int mode = 0;

        log("setIncomingCallIndicationResponse " + mode + " pendingMTCallId " + pendingMTCallId + " pendingMTSeqNum "+pendingMTSeqNum);
        
        if(accept) {
            int pid = Process.myPid();

            mode = 0;
            Process.setThreadPriority(pid, Process.THREAD_PRIORITY_DEFAULT - 10);
            log("Adjust the priority of process - " + pid + " to " + Process.getThreadPriority(pid));
        } else {
            mode = 1;
        } 
        cm.setCallIndication(mode, pendingMTCallId, pendingMTSeqNum, obtainCompleteMessage());
        pendingMTCallId = 0;
        pendingMTSeqNum = 0;
    }
    
    /**
      Update pendingHangupRequest state if needed.
      mtk04070, 20120314
    */
    void updatePendingHangupRequest() {
       log("updatePendingHangupRequest - " + hangupPendingMO + hasPendingHangupRequest + pendingHangupRequest);
       if (hangupPendingMO) {
           if (hasPendingHangupRequest) {
               pendingHangupRequest--;
               if (pendingHangupRequest == 0) {
                  hasPendingHangupRequest = false;
               }
           }
       }						
    }

    /**
     * Get the count of current existed connections.
     * @return Return the count of current connections.
     */
    private int getCurrentTotalConnections() {
        int count = 0;
        for (int i = 0, s = MAX_CONNECTIONS; i < s; i++) {
            if (connections[i] != null) {
                count ++;	
            }	
		    }
        return count;
    }

    // MTK_OPTR_PROTECT_START
    /* DM start */
    private class GsmCallTrackerReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            // update variables when receive intent
            try {
                if (dmAgent != null) {
                    isInLock = dmAgent.isLockFlagSet();
                    isFullLock = (dmAgent.getLockType() == 1);
                    log("isInLock = " + isInLock + ", isFullLock = " + isFullLock);
                    needHangupMOCall = dmAgent.isHangMoCallLocking();
                    needHangupMTCall = dmAgent.isHangMtCallLocking();
                    log("needHangupMOCall = " + needHangupMOCall + ", needHangupMTCall = " + needHangupMTCall);
                }
            } catch (RemoteException ex) {
            }

            if (intent.getAction().equals("com.mediatek.dm.LAWMO_LOCK")) {
                // when phone is not idle, hang up call
                // when phone is idle, do nothing
                if (PhoneConstants.State.IDLE != state) {
                    if (needHangupMOCall && needHangupMTCall) {
                        hangupAll();
                    } else {
                        int count = connections.length;
                        log("The count of connections is" + count);
                        for (int i = 0; i < count; i++) {
                            GsmConnection cn = connections[i];
                            if ((cn.isIncoming() && needHangupMTCall) ||
                               (!cn.isIncoming() && needHangupMOCall))
                                try {
                                    hangup(connections[i]);
                                } catch (CallStateException ex) {
                                    Log.e(LOG_TAG, "unexpected error on hangup");
                                }
                        }
                    }
                }
            }
        }    
    }
    /* DM end */
    // MTK_OPTR_PROTECT_END
    /// @}
}
