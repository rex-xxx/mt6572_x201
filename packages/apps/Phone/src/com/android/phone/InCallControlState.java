/*
 * Copyright (C) 2009 The Android Open Source Project
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

import android.telephony.PhoneNumberUtils;
import android.util.Log;

import com.android.internal.telephony.Call;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.CallManager;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyCapabilities;

//MTK begin:
import com.mediatek.phone.DualTalkUtils;
import com.mediatek.phone.ext.IInCallControlState;
//MTK end

/**
 * Helper class to keep track of enabledness, visibility, and "on/off"
 * or "checked" state of the various controls available in the in-call
 * UI, based on the current telephony state.
 *
 * This class is independent of the exact UI controls used on any given
 * device.  To avoid cluttering up the "view" code (i.e. InCallTouchUi)
 * with logic about which functions are available right now, we instead
 * have that logic here, and provide simple boolean flags to indicate the
 * state and/or enabledness of all possible in-call user operations.
 *
 * (In other words, this is the "model" that corresponds to the "view"
 * implemented by InCallTouchUi.)
 */
public class InCallControlState implements IInCallControlState {
    private static final String LOG_TAG = "InCallControlState";
    //private static final boolean DBG = (PhoneGlobals.DBG_LEVEL >= 2);
    private static final boolean DBG = true;

    private InCallScreen mInCallScreen;
    private CallManager mCM;

    //
    // Our "public API": Boolean flags to indicate the state and/or
    // enabledness of all possible in-call user operations:
    //

    public boolean manageConferenceVisible;
    public boolean manageConferenceEnabled;
    //
    public boolean canAddCall;
    //
    public boolean canEndCall;
    //
    public boolean canShowSwap;
    public boolean canSwap;
    public boolean canMerge;
    //
    public boolean bluetoothEnabled;
    public boolean bluetoothIndicatorOn;
    //
    public boolean speakerEnabled;
    public boolean speakerOn;
    //
    public boolean canMute;
    public boolean muteIndicatorOn;
    //
    public boolean dialpadEnabled;
    public boolean dialpadVisible;
    //
    /** True if the "Hold" function is *ever* available on this device */
    public boolean supportsHold;
    /** True if the call is currently on hold */
    public boolean onHold;
    /** True if the "Hold" or "Unhold" function should be available right now */
    // TODO: this name is misleading.  Let's break this apart into
    // separate canHold and canUnhold flags, and have the caller look at
    // "canHold || canUnhold" to decide whether the hold/unhold UI element
    // should be visible.
    public boolean canHold;

    public boolean contactsEnabled;

    public InCallControlState(InCallScreen inCallScreen, CallManager cm) {
        if (DBG) log("InCallControlState constructor...");
        mInCallScreen = inCallScreen;
        mCM = cm;
    }

    /**
     * Updates all our public boolean flags based on the current state of
     * the Phone.
     */
    public void update() {
        final PhoneConstants.State state = mCM.getState();  // coarse-grained voice call state
/*Google: final Call fgCall = mCM.getActiveFgCall();
        final Call.State fgCallState = fgCall.getState();
        final boolean hasActiveForegroundCall = (fgCallState == Call.State.ACTIVE);
        final boolean hasHoldingCall = mCM.hasActiveBgCall();*/
//MTK begin:
        final Call fgCall;
        final Call.State fgCallState;
        final boolean hasActiveForegroundCall;
        final boolean hasHoldingCall;
        
        fgCall = mCM.getActiveFgCall();
        hasHoldingCall = mCM.hasActiveBgCall();
        if (null != fgCall) {
            fgCallState = fgCall.getState();
            hasActiveForegroundCall = (fgCallState == Call.State.ACTIVE);
        } else {
            fgCallState = Call.State.IDLE;
            hasActiveForegroundCall = false;
        }
//MTK end
        
        /* Added by xingping.zheng start */
        if ((fgCallState == Call.State.DIALING)
                || (fgCallState == Call.State.ALERTING)) {
            contactsEnabled = false;
        } else {
            contactsEnabled = true;
        }
        /* Added by xingping.zheng end   */

        // Manage conference:
        if (Call.State.IDLE != fgCall.getState() && TelephonyCapabilities.supportsConferenceCallManagement(fgCall.getPhone())) {
            // This item is visible only if the foreground call is a
            // conference call, and it's enabled unless the "Manage
            // conference" UI is already up.
            manageConferenceVisible = PhoneUtils.isConferenceCall(fgCall);
            manageConferenceEnabled =
                    manageConferenceVisible && !mInCallScreen.isManageConferenceMode();
        } else if (hasHoldingCall && TelephonyCapabilities.supportsConferenceCallManagement(mCM.getBgPhone())) {
            //manageConferenceVisible = PhoneUtils.isConferenceCall(mCM.getBgPhone().getBackgroundCall());
            //ALPS00118272
            manageConferenceVisible = false;
            manageConferenceEnabled =
                manageConferenceVisible && !mInCallScreen.isManageConferenceMode();
        } else {
            // This device has no concept of managing a conference call.
            manageConferenceVisible = false;
            manageConferenceEnabled = false;
        }

        // "Add call":
        canAddCall = PhoneUtils.okToAddCall(mCM);

        // "End call": always enabled unless the phone is totally idle.
        // Note that while the phone is ringing, the InCallTouchUi widget isn't
        // visible at all, so the state of the End button doesn't matter.  However
        // we *do* still set canEndCall to true in this case, purely to prevent a
        // UI glitch when the InCallTouchUi widget first appears, immediately after
        // answering an incoming call.
        canEndCall = (mCM.hasActiveFgCall() || mCM.hasActiveRingingCall() || mCM.hasActiveBgCall());

        // Swap / merge calls
        canShowSwap = PhoneUtils.okToShowSwapButton(mCM);
        canSwap = PhoneUtils.okToSwapCalls(mCM);
        if (DualTalkUtils.isSupportDualTalk() && DualTalkUtils.getInstance().hasDualHoldCallsOnly()) {
            canSwap = true;
        }
        canMerge = PhoneUtils.okToMergeCalls(mCM);

        // "Bluetooth":
        if (mInCallScreen.isBluetoothAvailable()) {
            bluetoothEnabled = true;
            bluetoothIndicatorOn = mInCallScreen.isBluetoothAudioConnectedOrPending();
        } else {
            bluetoothEnabled = false;
            bluetoothIndicatorOn = false;
        }

        // "Speaker": always enabled unless the phone is totally idle.
        // The current speaker state comes from the AudioManager.
        speakerEnabled = (state != PhoneConstants.State.IDLE);
        speakerOn = PhoneUtils.isSpeakerOn(mInCallScreen);

        // "Mute": only enabled when the foreground call is ACTIVE.
        // (It's meaningless while on hold, or while DIALING/ALERTING.)
        // It's also explicitly disabled during emergency calls or if
        // emergency callback mode (ECM) is active.
        Connection c;
        if (null != fgCall) {
            c = fgCall.getLatestConnection();
        } else {
            c = null;
        }
        boolean isEmergencyCall = false;

        /**
         * change feature by mediatek .inc
         * description : use isEmergencyNumber instead to avoid performance issues
         * original android code:
         isEmergencyCall = PhoneNumberUtils.isLocalEmergencyNumber(c.getAddress(), fgCall
                    .getPhone().getContext());
         */
        if (c != null) {
            isEmergencyCall = PhoneNumberUtils.isEmergencyNumber(c.getAddress());
        }

        boolean isECM = PhoneUtils.isPhoneInEcm(fgCall.getPhone());
        if (isEmergencyCall || isECM) {  // disable "Mute" item
            canMute = false;
            muteIndicatorOn = false;
        } else {
            canMute = hasActiveForegroundCall;
            muteIndicatorOn = PhoneUtils.getMute();
        }

        // "Dialpad": Enabled only when it's OK to use the dialpad in the
        // first place.
        dialpadEnabled = mInCallScreen.okToShowDialpad();

        // Also keep track of whether the dialpad is currently "opened"
        // (i.e. visible).
        dialpadVisible = mInCallScreen.isDialerOpened();

        // "Hold:
        if (DualTalkUtils.isSupportDualTalk() && DualTalkUtils.getInstance().isCdmaAndGsmActive()) {
            canHold = false;
        } else if (null != fgCall && TelephonyCapabilities.supportsHoldAndUnhold(fgCall.getPhone())) {
            // This phone has the concept of explicit "Hold" and "Unhold" actions.
            supportsHold = true;
            // "On hold" means that there's a holding call and
            // *no* foreground call.  (If there *is* a foreground call,
            // that's "two lines in use".)
            onHold = hasHoldingCall && (fgCallState == Call.State.IDLE) && !PhoneUtils.holdAndActiveFromDifPhone(mCM);
            // The "Hold" control is disabled entirely if there's
            // no way to either hold or unhold in the current state.
            boolean okToHold = hasActiveForegroundCall && !hasHoldingCall;
            boolean okToUnhold = onHold;
            canHold = okToHold || okToUnhold;
        } else if (hasHoldingCall && (fgCallState == Call.State.IDLE)) {
            // Even when foreground phone device doesn't support hold/unhold, phone devices
            // for background holding calls may do.
            //
            // If the foreground call is ACTIVE,  we should turn on "swap" button instead.
            final Call bgCall = mCM.getFirstActiveBgCall();
            if (bgCall != null &&
                    TelephonyCapabilities.supportsHoldAndUnhold(bgCall.getPhone())) {
                supportsHold = true;
                onHold = true;
                canHold = true;
            }
        } else {
            // This device has no concept of "putting a call on hold."
            supportsHold = false;
            onHold = false;
            canHold = false;
        }

        if (DBG) dumpState();
    }

    public void dumpState() {
        log("InCallControlState:");
        log("  manageConferenceVisible: " + manageConferenceVisible);
        log("  manageConferenceEnabled: " + manageConferenceEnabled);
        log("  canAddCall: " + canAddCall);
        log("  canEndCall: " + canEndCall);
        log("  canSwap: " + canSwap);
        log("  canShowSwap: " + canShowSwap);
        log("  canMerge: " + canMerge);
        log("  bluetoothEnabled: " + bluetoothEnabled);
        log("  bluetoothIndicatorOn: " + bluetoothIndicatorOn);
        log("  speakerEnabled: " + speakerEnabled);
        log("  speakerOn: " + speakerOn);
        log("  canMute: " + canMute);
        log("  muteIndicatorOn: " + muteIndicatorOn);
        log("  dialpadEnabled: " + dialpadEnabled);
        log("  dialpadVisible: " + dialpadVisible);
        log("  onHold: " + onHold);
        log("  canHold: " + canHold);
    }

    private void log(String msg) {
        Log.d(LOG_TAG, msg);
    }

    // Add by MTK for plugin
    public boolean isManageConferenceVisible() {
        return manageConferenceVisible;
    }

    public boolean isManageConferenceEnabled() {
        return manageConferenceEnabled;
    }

    public boolean canAddCall() {
        return canAddCall;
    }
    
    public boolean canEndCall() {
        return canEndCall;
    }
    
    public boolean canShowSwap() {
        return canShowSwap;
    }
    
    public boolean canSwap() {
        return canSwap;
    }
    
    public boolean canMerge() {
        return canMerge;
    }
    
    public boolean isBluetoothEnabled() {
        return bluetoothEnabled;
    }
    
    public boolean isBluetoothIndicatorOn() {
        return bluetoothIndicatorOn;
    }
    
    public boolean isSpeakerEnabled() {
        return speakerEnabled;
    }
    
    public boolean isSpeakerOn() {
        return speakerOn;
    }
    
    public boolean canMute() {
        return canMute;
    }
    
    public boolean isMuteIndicatorOn() {
        return muteIndicatorOn;
    }
    
    public boolean isDialpadEnabled() {
        return dialpadEnabled;
    }
    
    public boolean isDialpadVisible() {
        return dialpadVisible;
    }
    
    public boolean supportsHold() {
        return supportsHold;
    }
    
    public boolean onHold() {
        return onHold;
    }
    
    public boolean canHold() {
        return canHold;
    }
    
    public boolean isContactsEnabled() {
        return contactsEnabled;
    }

}
