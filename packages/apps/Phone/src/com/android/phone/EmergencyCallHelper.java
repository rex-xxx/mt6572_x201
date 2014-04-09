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

package com.android.phone;

import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallManager;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.phone.Constants.CallStatusCode;
import com.android.phone.InCallUiState.ProgressIndicationType;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.provider.Telephony.SIMInfo;
import android.telephony.ServiceState;
import android.util.Log;

import com.android.internal.telephony.gemini.GeminiPhone;
import com.android.internal.telephony.gemini.MTKCallManager;
import com.mediatek.phone.gemini.GeminiRegister;
import com.mediatek.phone.gemini.GeminiUtils;
import com.mediatek.telephony.PhoneNumberUtilsEx;

import java.util.List;

/**
 * Helper class for the {@link CallController} that implements special
 * behavior related to emergency calls.  Specifically, this class handles
 * the case of the user trying to dial an emergency number while the radio
 * is off (i.e. the device is in airplane mode), by forcibly turning the
 * radio back on, waiting for it to come up, and then retrying the
 * emergency call.
 *
 * This class is instantiated lazily (the first time the user attempts to
 * make an emergency call from airplane mode) by the the
 * {@link CallController} singleton.
 */
public class EmergencyCallHelper extends Handler {
    private static final String TAG = "EmergencyCallHelper";
    private static final boolean DBG = true;

    // Number of times to retry the call, and time between retry attempts.
    // MTK old code set MAX_NUM_RETRIES as 0, but does not know reason,
    // Here just keep google default count 6
    public static final int MAX_NUM_RETRIES = 6;
    //Sync with FW's owner, modem will power on/off, this may take more time
    public static final long TIME_BETWEEN_RETRIES = 15000;  // msec

    // Timeout used with our wake lock (just as a safety valve to make
    // sure we don't hold it forever).
    public static final long WAKE_LOCK_TIMEOUT = 5 * 60 * 1000;  // 5 minutes in msec

    // !!! Below is from MTK code, need check whether needed
    //private static boolean dialing_ecc = false;

    // Handler message codes; see handleMessage()
    private static final int START_SEQUENCE = 1;
    private static final int SERVICE_STATE_CHANGED = 2;
    private static final int DISCONNECT = 3;
    private static final int RETRY_TIMEOUT = 4;

    private static final int PHONE_STATE_CHANGED = 5;
    private static final int SEND_ECC_CALL = 6;

    private CallController mCallController;
    private PhoneGlobals mApp;
    private CallManager mCM;
    private Phone mPhone;
    private String mNumber;  // The emergency number we're trying to dial
    private int mNumRetriesSoFar;

    private int mServiceOnSlot;

    // Wake lock we hold while running the whole sequence
    private PowerManager.WakeLock mPartialWakeLock;

    public EmergencyCallHelper(CallController callController) {
        if (DBG) log("EmergencyCallHelper constructor...");
        mCallController = callController;
        mApp = PhoneGlobals.getInstance();
        mCM =  mApp.mCM;
        mCMGemini = mApp.mCMGemini;
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case START_SEQUENCE:
                startSequenceInternal(msg);
                break;
            case SERVICE_STATE_CHANGED:
            case SERVICE_STATE_CHANGED2:
            case SERVICE_STATE_CHANGED3:
            case SERVICE_STATE_CHANGED4:
                onServiceStateChanged(msg);
                break;
            case DISCONNECT:
                onDisconnect(msg);
                break;
            case RETRY_TIMEOUT:
                onRetryTimeout(msg);
                break;
            /// M: for ALPS00612522 @{
            // if any calls established during power on radio to dail ECC, need to hang up them before
            // send the ECC call
            case PHONE_STATE_CHANGED:
                PhoneConstants.State state = mCM.getState();
                log("handleMessage: PHONE_STATE_CHANGED with state = " + state);
                if (state == PhoneConstants.State.IDLE) {

                    /// M:Gemini+
                    Object callManager = GeminiUtils.isGeminiSupport() ? PhoneGlobals.getInstance().mCMGemini : mCM;
                    GeminiRegister.unregisterForDisconnect(callManager, this);

                    // for ALPS00565730; leave 200ms to make sure InCallScreen.onDisconnect() finish.
                    // or moveTaskToBack() may cause InCallScreen's disappear, see ALPS00565730's case.
                    Message message = obtainMessage(SEND_ECC_CALL);
                    sendMessageDelayed(message, 200);
                } else {
                    log("handleMessage: PHONE_STATE_CHANGED continue waiting...");
                }
                break;

            case SEND_ECC_CALL:
                log("send the ecc call!");
                // Take down the "Turning on radio..." indication.
                mApp.inCallUiState.clearProgressIndication();
                cancelRetryTimer();
                placeEmergencyCall(mServiceOnSlot);
                break;
             /// @}

            default:
                Log.wtf(TAG, "handleMessage: unexpected message: " + msg);
                break;
        }
    }

    /**
     * Starts the "emergency call from airplane mode" sequence.
     *
     * This is the (single) external API of the EmergencyCallHelper class.
     * This method is called from the CallController placeCall() sequence
     * if the user dials a valid emergency number, but the radio is
     * powered-off (presumably due to airplane mode.)
     *
     * This method kicks off the following sequence:
     * - Power on the radio
     * - Listen for the service state change event telling us the radio has come up
     * - Then launch the emergency call
     * - Retry if the call fails with an OUT_OF_SERVICE error
     * - Retry if we've gone 5 seconds without any response from the radio
     * - Finally, clean up any leftover state (progress UI, wake locks, etc.)
     *
     * This method is safe to call from any thread, since it simply posts
     * a message to the EmergencyCallHelper's handler (thus ensuring that
     * the rest of the sequence is entirely serialized, and runs only on
     * the handler thread.)
     *
     * This method does *not* force the in-call UI to come up; our caller
     * is responsible for doing that (presumably by calling
     * PhoneGlobals.displayCallScreen().)
     */
    public void startEmergencyCallFromAirplaneModeSequence(String number) {
        if (DBG) log("startEmergencyCallFromAirplaneModeSequence('" + number + "')...");
        Message msg = obtainMessage(START_SEQUENCE, number);
        sendMessage(msg);
    }

    /**
     * Actual implementation of startEmergencyCallFromAirplaneModeSequence(),
     * guaranteed to run on the handler thread.
     * @see startEmergencyCallFromAirplaneModeSequence()
     */
    private void startSequenceInternal(Message msg) {
        if (DBG) log("startSequenceInternal(): msg = " + msg);

        // First of all, clean up any state (including mPartialWakeLock!)
        // left over from a prior emergency call sequence.
        // This ensures that we'll behave sanely if another
        // startEmergencyCallFromAirplaneModeSequence() comes in while
        // we're already in the middle of the sequence.
        cleanup();

        mNumber = (String) msg.obj;
        if (DBG) log("- startSequenceInternal: Got mNumber: '" + mNumber + "'");

        mNumRetriesSoFar = 0;

        // Reset mPhone to whatever the current default phone is right now.
        /**
         * change feature by mediatek .inc
         * original android code :
         * mPhone = mApp.mCM.getDefaultPhone();
         * description : use mApp.phone for gemini support
         */
        mPhone = mApp.phone;

        // Wake lock to make sure the processor doesn't go to sleep midway
        // through the emergency call sequence.
        PowerManager pm = (PowerManager) mApp.getSystemService(Context.POWER_SERVICE);
        mPartialWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        // Acquire with a timeout, just to be sure we won't hold the wake
        // lock forever even if a logic bug (in this class) causes us to
        // somehow never call cleanup().
        if (DBG) log("- startSequenceInternal: acquiring wake lock");
        mPartialWakeLock.acquire(WAKE_LOCK_TIMEOUT);

        // No need to check the current service state here, since the only
        // reason the CallController would call this method in the first
        // place is if the radio is powered-off.
        //
        // So just go ahead and turn the radio on.

        powerOnRadio();  // We'll get an onServiceStateChanged() callback
                         // when the radio successfully comes up.

        // Next step: when the SERVICE_STATE_CHANGED event comes in,
        // we'll retry the call; see placeEmergencyCall();
        // But also, just in case, start a timer to make sure we'll retry
        // the call even if the SERVICE_STATE_CHANGED event never comes in
        // for some reason.
        startRetryTimer();

        // And finally, let the in-call UI know that we need to
        // display the "Turning on radio..." progress indication.
        mApp.inCallUiState.setProgressIndication(ProgressIndicationType.TURNING_ON_RADIO);

        // (Our caller is responsible for calling mApp.displayCallScreen().)
    }

    /**
     * Handles the SERVICE_STATE_CHANGED event.
     *
     * (Normally this event tells us that the radio has finally come
     * up.  In that case, it's now safe to actually place the
     * emergency call.)
     */
    private void onServiceStateChanged(Message msg) {
        ServiceState state = (ServiceState) ((AsyncResult) msg.obj).result;
        if (DBG) log("onServiceStateChanged()...  new state = " + state);

        // Possible service states:
        // - STATE_IN_SERVICE        // Normal operation
        // - STATE_OUT_OF_SERVICE    // Still searching for an operator to register to,
        //                           // or no radio signal
        // - STATE_EMERGENCY_ONLY    // Phone is locked; only emergency numbers are allowed
        // - STATE_POWER_OFF         // Radio is explicitly powered off (airplane mode)

        // Once we reach either STATE_IN_SERVICE or STATE_EMERGENCY_ONLY,
        // it's finally OK to place the emergency call.
        /**
         * change feature by mediatek .inc
         * original android code:
         * boolean okToCall = (state.getState() == ServiceState.STATE_IN_SERVICE)
                || (state.getState() == ServiceState.STATE_EMERGENCY_ONLY);
         * description : when there are no sim cards inserted, service state is out of service
         * so change the condition
         */
        boolean okToCall = state.getState() != ServiceState.STATE_POWER_OFF;

        if (okToCall) {
            // Woo hoo!  It's OK to actually place the call.
            if (DBG) log("onServiceStateChanged: ok to call!");

            // Deregister for the service state change events.
            GeminiRegister.unregisterForServiceStateChanged(mPhone, this, SERVICE_STATE_CHANGED_GEMINI);

            /// M:Gemini+
            mServiceOnSlot = GeminiRegister.getSlotIdByRegisterEvent(msg.what, SERVICE_STATE_CHANGED_GEMINI);
            log("onServiceStateChanged slotId=" + mServiceOnSlot);

            /// M: for alps00571489 @{
            // hangup all calls that is not ecc call
            if (hangupAllCallsWhenEcc()) {
                return;
            }
            /// @}

            // Take down the "Turning on radio..." indication.
            mApp.inCallUiState.clearProgressIndication();

            /// M: For ALPS00396774
            /// Need cancel retry timer here, since the emergency phone call will place. @{
            cancelRetryTimer();
            /// @}

            placeEmergencyCall(mServiceOnSlot);

            // The in-call UI is probably still up at this point,
            // but make sure of that:
            mApp.displayCallScreen(true);
        } else {
            // The service state changed, but we're still not ready to call yet.
            // (This probably was the transition from STATE_POWER_OFF to
            // STATE_OUT_OF_SERVICE, which happens immediately after powering-on
            // the radio.)
            //
            // So just keep waiting; we'll probably get to either
            // STATE_IN_SERVICE or STATE_EMERGENCY_ONLY very shortly.
            // (Or even if that doesn't happen, we'll at least do another retry
            // when the RETRY_TIMEOUT event fires.)
            if (DBG) log("onServiceStateChanged: not ready to call yet, keep waiting...");
        }
    }


    /**
     * Handles a DISCONNECT event from the telephony layer.
     *
     * Even after we successfully place an emergency call (after powering
     * on the radio), it's still possible for the call to fail with the
     * disconnect cause OUT_OF_SERVICE.  If so, schedule a retry.
     */
    private void onDisconnect(Message msg) {
        Connection conn = (Connection) ((AsyncResult) msg.obj).result;
        Connection.DisconnectCause cause = conn.getDisconnectCause();
        if (DBG) log("onDisconnect: connection '" + conn
                     + "', addr '" + conn.getAddress() + "', cause = " + cause);

        if (cause == Connection.DisconnectCause.OUT_OF_SERVICE) {
            // Wait a bit more and try again (or just bail out totally if
            // we've had too many failures.)
            if (DBG) log("- onDisconnect: OUT_OF_SERVICE, need to retry...");
            //scheduleRetryOrBailOut();
            cleanup();
        } else {
            // Any other disconnect cause means we're done.
            // Either the emergency call succeeded *and* ended normally,
            // or else there was some error that we can't retry.  In either
            // case, just clean up our internal state.)

            if (DBG) log("==> Disconnect event; clean up...");
            cleanup();

            // Nothing else to do here.  If the InCallScreen was visible,
            // it would have received this disconnect event too (so it'll
            // show the "Call ended" state and finish itself without any
            // help from us.)
        }
    }

    /**
     * Handles the retry timer expiring.
     */
    private void onRetryTimeout(Message msg) {
        PhoneConstants.State phoneState = PhoneConstants.State.IDLE;
        int slot = msg.arg1;
        boolean slotPowerOff = isSlotPowerOff(msg.arg1);
        if (DBG) {
            log("onRetryTimeout():  phone state " + phoneState
                     + ", service state(PowerOff)=" + slotPowerOff
                     + ", mNumRetriesSoFar = " + mNumRetriesSoFar);
        }

        // - If we're actually in a call, we've succeeded.
        //
        // - Otherwise, if the radio is now on, that means we successfully got
        //   out of airplane mode but somehow didn't get the service state
        //   change event.  In that case, try to place the call.
        //
        // - If the radio is still powered off, try powering it on again.

        if (phoneState == PhoneConstants.State.OFFHOOK) {
            if (DBG) log("- onRetryTimeout: Call is active!  Cleaning up...");
            cleanup();
            return;
        }

        if (!slotPowerOff) {
            // Woo hoo -- we successfully got out of airplane mode.

            // Deregister for the service state change events; we don't need
            // these any more now that the radio is powered-on.
            GeminiRegister.unregisterForServiceStateChanged(mPhone, this, SERVICE_STATE_CHANGED_GEMINI);

            // Take down the "Turning on radio..." indication.
            mApp.inCallUiState.clearProgressIndication();

            placeEmergencyCall(slot);
        } else {
            // Uh oh; we've waited the full TIME_BETWEEN_RETRIES and the
            // radio is still not powered-on.  Try again...

            if (DBG) log("- Trying (again) to turn on the radio...");
            powerOnRadio();  // Again, we'll (hopefully) get an onServiceStateChanged()
                             // callback when the radio successfully comes up.

            // ...and also set a fresh retry timer (or just bail out
            // totally if we've had too many failures.)
            scheduleRetryOrBailOut();
        }

        // Finally, the in-call UI is probably still up at this point,
        // but make sure of that:
        mApp.displayCallScreen(true);
    }

    /**
     * Attempt to power on the radio (i.e. take the device out
     * of airplane mode.)
     *
     * Additionally, start listening for service state changes;
     * we'll eventually get an onServiceStateChanged() callback
     * when the radio successfully comes up.
     */
    private void powerOnRadio() {
        if (DBG)
            log("- powerOnRadio()...");

        // We're about to turn on the radio, so arrange to be notified
        // when the sequence is complete.
        GeminiRegister.registerForServiceStateChanged(mPhone, this, SERVICE_STATE_CHANGED_GEMINI);

        // If airplane mode is on, we turn it off the same way that the
        // Settings activity turns it off.
        int dualSimMode = getSysDualSimMode();
        boolean bOffAirplaneMode = false;

        if (DBG) Log.d(TAG, "dualSimMode = " + dualSimMode);
        if (Settings.Global.getInt(mApp.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) > 0) {
            if (DBG) log("==> Turning off airplane mode...");

            // Change the system setting
            Settings.Global.putInt(mApp.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0);

            // Post the intent
            Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
            intent.putExtra("state", false);
            mApp.sendBroadcastAsUser(intent, UserHandle.ALL);
            bOffAirplaneMode = true;
        } else if (!GeminiUtils.isGeminiSupport()) {
            // Otherwise, for some strange reason the radio is off
            // (even though the Settings database doesn't think we're
            // in airplane mode.) In this case just turn the radio
            // back on.
            if (DBG) log("==> (Apparently) not in airplane mode; manually powering radio on...");
            mPhone.setRadioPower(true);
        }
        int expMode = getProperDualSimMode(dualSimMode);
        if (GeminiUtils.isGeminiSupport()
                && (!bOffAirplaneMode || (bOffAirplaneMode && (expMode != dualSimMode)))) {
            log("Send broadcast to turn on radio with mode = " + expMode);
            Settings.Global.putInt(mApp.getContentResolver(),
                    Settings.System.DUAL_SIM_MODE_SETTING, expMode);
            final Intent intent = new Intent(Intent.ACTION_DUAL_SIM_MODE_CHANGED);
            intent.putExtra(Intent.EXTRA_DUAL_SIM_MODE, expMode);
            mApp.sendBroadcastAsUser(intent, UserHandle.ALL);
        } else {
            log("do nothing.");
        }
    }

    /**
     * Actually initiate the outgoing emergency call.
     * (We do this once the radio has successfully been powered-up.)
     *
     * If the call succeeds, we're done.
     * If the call fails, schedule a retry of the whole sequence.
     */
    private void placeEmergencyCall(int simId) {
        if (DBG) log("placeEmergencyCall()...");

        // Place an outgoing call to mNumber.
        // Note we call PhoneUtils.placeCall() directly; we don't want any
        // of the behavior from CallController.placeCallInternal() here.
        // (Specifically, we don't want to start the "emergency call from
        // airplane mode" sequence from the beginning again!)

        /// M:Gemini+
        Object callManager = GeminiUtils.isGeminiSupport() ? mCMGemini : mCM;
        GeminiRegister.registerForDisconnect(callManager, this, DISCONNECT);

        if (DBG) log("- placing call to '" + mNumber + "'..." + " simId = " + simId);
        int callStatus;
        if (GeminiUtils.isGeminiSupport()) {
            callStatus = PhoneUtils.placeCallGemini(mApp,
                                                    mPhone,
                                                    mNumber,
                                                    null,  // contactUri
                                                    true,  // isEmergencyCall
                                                    null,
                                                    simId);  // gatewayUri
            if (DBG) log("- PhoneUtils.placeCallGemini() returned status = " + callStatus);
        } else {
            callStatus = PhoneUtils.placeCall(mApp,
                                              mPhone,
                                              mNumber,
                                              null,  // contactUri
                                              true,  // isEmergencyCall
                                              null);  // gatewayUri
            if (DBG) log("- PhoneUtils.placeCall() returned status = " + callStatus);
        }

        boolean success;
        // Note PhoneUtils.placeCall() returns one of the CALL_STATUS_*
        // constants, not a CallStatusCode enum value.
        switch (callStatus) {
            case PhoneUtils.CALL_STATUS_DIALED:
                success = true;
                break;

            case PhoneUtils.CALL_STATUS_DIALED_MMI:
            case Constants.CALL_STATUS_FAILED:
            default:
                // Anything else is a failure, and we'll need to retry.
                Log.w(TAG, "placeEmergencyCall(): placeCall() failed: callStatus = " + callStatus);
                success = false;
                break;
        }

        if (success) {
            if (DBG) log("==> Success from PhoneUtils.placeCall()!");
            // Ok, the emergency call is (hopefully) under way.

            // We're not done yet, though, so don't call cleanup() here.
            // (It's still possible that this call will fail, and disconnect
            // with cause==OUT_OF_SERVICE.  If so, that will trigger a retry
            // from the onDisconnect() method.)
        } else {
            if (DBG) log("==> Failure.");
            // Wait a bit more and try again (or just bail out totally if
            // we've had too many failures.)
            scheduleRetryOrBailOut();
        }
    }

    /**
     * Schedules a retry in response to some failure (either the radio
     * failing to power on, or a failure when trying to place the call.)
     * Or, if we've hit the retry limit, bail out of this whole sequence
     * and display a failure message to the user.
     */
    private void scheduleRetryOrBailOut() {
        mNumRetriesSoFar++;
        if (DBG) log("scheduleRetryOrBailOut()...  mNumRetriesSoFar is now " + mNumRetriesSoFar);

        if (mNumRetriesSoFar > MAX_NUM_RETRIES) {
            Log.w(TAG, "scheduleRetryOrBailOut: hit MAX_NUM_RETRIES; giving up...");
            cleanup();
            // ...and have the InCallScreen display a generic failure
            // message.
            mApp.inCallUiState.setPendingCallStatusCode(CallStatusCode.CALL_FAILED);
        } else {
            if (DBG) log("- Scheduling another retry...");
            startRetryTimer();
            mApp.inCallUiState.setProgressIndication(ProgressIndicationType.RETRYING);
        }
    }

    /**
     * Clean up when done with the whole sequence: either after
     * successfully placing *and* ending the emergency call, or after
     * bailing out because of too many failures.
     *
     * The exact cleanup steps are:
     * - Take down any progress UI (and also ask the in-call UI to refresh itself,
     *   if it's still visible)
     * - Double-check that we're not still registered for any telephony events
     * - Clean up any extraneous handler messages (like retry timeouts) still in the queue
     * - Make sure we're not still holding any wake locks
     *
     * Basically this method guarantees that there will be no more
     * activity from the EmergencyCallHelper until the CallController
     * kicks off the whole sequence again with another call to
     * startEmergencyCallFromAirplaneModeSequence().
     *
     * Note we don't call this method simply after a successful call to
     * placeCall(), since it's still possible the call will disconnect
     * very quickly with an OUT_OF_SERVICE error.
     */
    private void cleanup() {
        if (DBG) log("cleanup()...");

        // Take down the "Turning on radio..." indication.
        mApp.inCallUiState.clearProgressIndication();
        /// M:Gemini+
        GeminiRegister.unregisterForServiceStateChanged(mPhone, this, SERVICE_STATE_CHANGED_GEMINI);
        Object callManager = GeminiUtils.isGeminiSupport() ? mCMGemini : mCM;
        GeminiRegister.unregisterForDisconnect(callManager, this);
        cancelRetryTimer();

        // Release / clean up the wake lock
        if (mPartialWakeLock != null) {
            if (mPartialWakeLock.isHeld()) {
                if (DBG) log("- releasing wake lock");
                mPartialWakeLock.release();
            }
            mPartialWakeLock = null;
        }

        // And finally, ask the in-call UI to refresh itself (to clean up the
        // progress indication if necessary), if it's currently visible.
        mApp.updateInCallScreen();
    }

    private void startRetryTimer() {
        removeMessages(RETRY_TIMEOUT);
        /**
         * change feature by mediatek .inc
         * description : add slot id to RETRY_TIMEOUT message for gemini
         * original android code:
         sendEmptyMessageDelayed(RETRY_TIMEOUT, TIME_BETWEEN_RETRIES);
         */

        if (GeminiUtils.isGeminiSupport()) {
            int dualSimMode = getSysDualSimMode();
            int slot = getBestSlotForDualSimMode(dualSimMode);

            Message msg = obtainMessage(RETRY_TIMEOUT, slot, 0);
            sendMessageDelayed(msg, TIME_BETWEEN_RETRIES);
            // if(DBG) log("startRetryTimer, slot = " + slot);
        } else {
            sendEmptyMessageDelayed(RETRY_TIMEOUT, TIME_BETWEEN_RETRIES);
        }
        /**
         * change feature by mediatek .inc
         */
    }

    private void cancelRetryTimer() {
        removeMessages(RETRY_TIMEOUT);
    }

    /** --------------------------------MTK------------------------------------------- */
    /// M:Gemini+ @{
    public MTKCallManager mCMGemini = null;

    private static final int SERVICE_STATE_CHANGED2 = 102;
    private static final int SERVICE_STATE_CHANGED3 = 202;
    private static final int SERVICE_STATE_CHANGED4 = 302;
    private static final int[] SERVICE_STATE_CHANGED_GEMINI = { SERVICE_STATE_CHANGED, SERVICE_STATE_CHANGED2,
            SERVICE_STATE_CHANGED3, SERVICE_STATE_CHANGED4 };

    /**
     * The value of all SIMs on. 
     * slot size , value 
     *     1     ,   1
     *     2     ,   1+2
     *     3     ,   1+2+4
     * ...
     */
    private static final int DUAL_SIMS_MODE_ON = (1 << GeminiUtils.getSlotCount()) - 1;

    private boolean needSetDualSimMode(int lastMode) {
        // dual radio off, off airplane mode not open radios

        List<SIMInfo> list = SIMInfo.getInsertedSIMList(mApp);
        if (list == null || list.size() == 0) {
            boolean hasSimInserted = false;
            final int[] geminiSlots = GeminiUtils.getSlots();
            for (int gs : geminiSlots) {
                hasSimInserted = hasSimInserted || PhoneGlobals.getInstance().phoneMgr.isSimInsert(gs);
            }
            if (!hasSimInserted) {
                // no SIM inserted
                if (DBG) log("needSetDualSimMode, false, no sim inserted");
                return false; // change value to false.
            }
        }

        for (SIMInfo info : list) {
            int dualSim = lastMode & (1 << info.mSlot);
            if (dualSim > 0) {
                // follow the default set
                if (DBG) log("needSetDualSimMode, false, follow settings mode");
                return false;
            }
        }
        if (DBG) log("needSetDualSimMode, true");
        return true;
    }

    /**
     * get dual sim mode value from Settings
     * 
     * @return
     */
    private int getSysDualSimMode() {
        int dualSimMode = 0;
        if (GeminiUtils.isGeminiSupport()) {
            dualSimMode = Settings.Global.getInt(mApp.getContentResolver(),
                    Settings.System.DUAL_SIM_MODE_SETTING,
                    Settings.System.DUAL_SIM_MODE_SETTING_DEFAULT);
        }
        return dualSimMode;
    }

    /**
     * for supporting multi-sims: No SIM (mode is 0); One SIM x (mode is 1<<SIM
     * Slot); Multi-sims (mode is |=(1<<different slots)).
     *
     * @param originMode The origin dual sim mode saved in Settings.
     * @return
     */
    private int getProperDualSimMode(int originMode) {
        int mode = 0;
        List<SIMInfo> list = SIMInfo.getInsertedSIMList(mApp);
        if (list == null || list.isEmpty()) {
            log("getProperDualSimMode, No sim inserted, return " + DUAL_SIMS_MODE_ON);
            return DUAL_SIMS_MODE_ON; // MODE SIMs ON
        }
        if (list != null) {
            final int[] geminiSlots = GeminiUtils.getSlots();
            for (SIMInfo info : list) {
                for (int gs : geminiSlots) {
                    if (info.mSlot == gs) {
                        mode |= (1 << gs);
                        break;
                    }
                }
            }
        }
        if (DBG) {
            log("getProperDualSimMode, mode=" + mode);
        }

        /// M: ALPS567908 @{
        // if the originMode can work, just keep it.
        if ((mode & originMode) != 0) {
            mode = originMode;
        }
        /// @}
        return mode;
    }

    private int getBestSlotForDualSimMode(int mode) {
        // mode value (1 for SIM1, 1<<1 for SIM2, 1<<2 for SIM3, 1<<3 for SIM4)
        int bestSlot = GeminiUtils.getDefaultSlot();
        final int[] geminiSlots = GeminiUtils.getSlots();
        for (int gs : geminiSlots) {
            int dualSim = mode & (1 << gs);
            if (dualSim > 0) {
                bestSlot = gs;
                break;
            }
        }
        if (DBG) log("getBestSlotForDualSimMode, mode=" + mode + ", bestSlot=" + bestSlot);
        return bestSlot;
    }

    /**
     * 
     * if slotId is valid, check the slot's service state, else check all slots' service sate. 
     * @return true if the slot is power off
     */
    private boolean isSlotPowerOff(int slotId) {
        if (GeminiUtils.isGeminiSupport()) {
            GeminiPhone gPhone = (GeminiPhone) mPhone;
            if (GeminiUtils.isValidSlot(slotId)) {
                return gPhone.getServiceStateGemini(slotId).getState() == ServiceState.STATE_POWER_OFF;
            }
            int[] geminiSlots = GeminiUtils.getSlots();
            for (int gs : geminiSlots) {
                if (gPhone.getServiceStateGemini(gs).getState() != ServiceState.STATE_POWER_OFF) {
                    return false;
                }
            }
            return true;
        }
        return mPhone.getServiceState().getState() == ServiceState.STATE_POWER_OFF;
    }

    private void log(String msg) {
        Log.d(TAG, msg);
    }
    /// @}

    /**
     * when dial Emergency call, hangup all call.
     * @return
     */
    private boolean hangupAllCallsWhenEcc() {
        log("hangupAllCallsWhenEcc()...");
        PhoneConstants.State phoneState = mCM.getState();
        if (phoneState != PhoneConstants.State.IDLE) {
            Call fgCall = mCM.getActiveFgCall();
            Connection connection = fgCall.getEarliestConnection();
            String activeCallAddress = connection != null ? connection.getAddress() : null;

            Call bgCall = mCM.getFirstActiveBgCall();
            Connection bgconnection = bgCall.getEarliestConnection();
            String bgCallAddress = bgconnection != null ? bgconnection.getAddress() : null;
            /// M: for ALPS00612522 @{
            // also need to hang up ringing call during turning on radio to dial ECC
            Call ringingCall = mCM.getFirstActiveRingingCall();
            /// @}

            //The active forground call isn't ecc, disconnect
            if (fgCall != null
                    && fgCall.getState().isAlive()
                    && (!PhoneUtils.isEccCall(fgCall) || PhoneNumberUtilsEx
                            .isSpecialEmergencyNumber(activeCallAddress))
                    || bgCall != null
                    && bgCall.getState().isAlive()
                    && (!PhoneUtils.isEccCall(bgCall) || PhoneNumberUtilsEx
                            .isSpecialEmergencyNumber(bgCallAddress))
                    || ringingCall != null && ringingCall.getState().isAlive()) {
                /// M:Gemini+
                Object callManager = GeminiUtils.isGeminiSupport() ? PhoneGlobals.getInstance().mCMGemini : mCM;
                GeminiRegister.registerForDisconnect(callManager, this, PHONE_STATE_CHANGED);
                try {
                    mCM.hangupAllEx();
                    log("Waiting for disconnect exist calls.");
                    return true;
                } catch (CallStateException e) {
                    log("catch exception = " + e);
                }
            }
        }
        return false;
    }

}
