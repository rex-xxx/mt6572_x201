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

import android.app.ActivityManagerNative;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDiskIOException;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.net.sip.SipManager;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemVibrator;
import android.os.Vibrator;
import android.provider.CallLog.Calls;
import android.provider.Settings;
import android.provider.Telephony.SIMInfo;
import android.telephony.PhoneNumberUtils;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.EventLog;
import android.util.Log;
import android.widget.Toast;

import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallManager;
import com.android.internal.telephony.CallerInfo;
import com.android.internal.telephony.CallerInfoAsyncQuery;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneBase;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyCapabilities;
import com.android.internal.telephony.cdma.CdmaCallWaitingNotification;
import com.android.internal.telephony.cdma.CdmaInformationRecords.CdmaDisplayInfoRec;
import com.android.internal.telephony.cdma.CdmaInformationRecords.CdmaSignalInfoRec;
import com.android.internal.telephony.cdma.SignalToneUtil;
import com.android.internal.telephony.gemini.*;
import com.android.internal.telephony.gemini.MTKCallManager;
import com.android.internal.telephony.sip.SipPhone;
import com.android.phone.Constants.CallStatusCode;

import com.mediatek.calloption.CallOptionUtils;
import com.mediatek.phone.BlackListManager;
import com.mediatek.phone.DualTalkUtils;
import com.mediatek.phone.PhoneFeatureConstants.FeatureOption;
import com.mediatek.phone.gemini.GeminiUtils;
import com.mediatek.phone.gemini.GeminiRegister;
import com.mediatek.phone.SIMInfoWrapper;
import com.mediatek.phone.provider.CallHistoryAsync;
import com.mediatek.phone.recording.PhoneRecorderHandler;
import com.mediatek.phone.vt.VTCallUtils;
import com.mediatek.phone.vt.VTInCallScreenFlags;
import com.mediatek.settings.VTSettingUtils;
import com.mediatek.telephony.TelephonyManagerEx;
import com.mediatek.vt.VTManager;


/**
 * Phone app module that listens for phone state changes and various other
 * events from the telephony layer, and triggers any resulting UI behavior
 * (like starting the Ringer and Incoming Call UI, playing in-call tones,
 * updating notifications, writing call log entries, etc.)
 */
public class CallNotifier extends Handler
        implements CallerInfoAsyncQuery.OnQueryCompleteListener {
    
    /**
     * Used to store relevant fields for the Missed Call notifications.
     */
    private class CustomInfo {
        public long date;
        public int callVideo;
    }
    
    private static final String LOG_TAG = "CallNotifier";
    private static final boolean DBG = true;
            //(PhoneGlobals.DBG_LEVEL >= 1) && (SystemProperties.getInt("ro.debuggable", 0) == 1);
    private static final boolean VDBG = true; //(PhoneGlobals.DBG_LEVEL >= 2);

    // Maximum time we allow the CallerInfo query to run,
    // before giving up and falling back to the default ringtone.
    private static final int RINGTONE_QUERY_WAIT_TIME = 500;  // msec

    // Timers related to CDMA Call Waiting
    // 1) For displaying Caller Info
    // 2) For disabling "Add Call" menu option once User selects Ignore or CW Timeout occures
    private static final int CALLWAITING_CALLERINFO_DISPLAY_TIME = 20000; // msec
    private static final int CALLWAITING_ADDCALL_DISABLE_TIME = 30000; // msec

    // Time to display the  DisplayInfo Record sent by CDMA network
    private static final int DISPLAYINFO_NOTIFICATION_TIME = 2000; // msec

    /** The singleton instance. */
    private static CallNotifier sInstance;

    // Boolean to keep track of whether or not a CDMA Call Waiting call timed out.
    //
    // This is CDMA-specific, because with CDMA we *don't* get explicit
    // notification from the telephony layer that a call-waiting call has
    // stopped ringing.  Instead, when a call-waiting call first comes in we
    // start a 20-second timer (see CALLWAITING_CALLERINFO_DISPLAY_DONE), and
    // if the timer expires we clean up the call and treat it as a missed call.
    //
    // If this field is true, that means that the current Call Waiting call
    // "timed out" and should be logged in Call Log as a missed call.  If it's
    // false when we reach onCdmaCallWaitingReject(), we can assume the user
    // explicitly rejected this call-waiting call.
    //
    // This field is reset to false any time a call-waiting call first comes
    // in, and after cleaning up a missed call-waiting call.  It's only ever
    // set to true when the CALLWAITING_CALLERINFO_DISPLAY_DONE timer fires.
    //
    // TODO: do we really need a member variable for this?  Don't we always
    // know at the moment we call onCdmaCallWaitingReject() whether this is an
    // explicit rejection or not?
    // (Specifically: when we call onCdmaCallWaitingReject() from
    // PhoneUtils.hangupRingingCall() that means the user deliberately rejected
    // the call, and if we call onCdmaCallWaitingReject() because of a
    // CALLWAITING_CALLERINFO_DISPLAY_DONE event that means that it timed
    // out...)
    private boolean mCallWaitingTimeOut = false;

    // values used to track the query state
    private static final int CALLERINFO_QUERY_READY = 0;
    private static final int CALLERINFO_QUERYING = -1;

    // the state of the CallerInfo Query.
    private int mCallerInfoQueryState;

    // object used to synchronize access to mCallerInfoQueryState
    private Object mCallerInfoQueryStateGuard = new Object();

    // Event used to indicate a query timeout.
    private static final int RINGER_CUSTOM_RINGTONE_QUERY_TIMEOUT = 100;

    public static final int CALL_TYPE_SIP   = -2;
    public static final int CALL_TYPE_NONE  = 0;

    // Events from the Phone object:
    private static final int PHONE_STATE_CHANGED = 1;
    private static final int PHONE_NEW_RINGING_CONNECTION = 2;
    private static final int PHONE_DISCONNECT = 3;
    private static final int PHONE_UNKNOWN_CONNECTION_APPEARED = 4;
    private static final int PHONE_INCOMING_RING = 5;
    private static final int PHONE_STATE_DISPLAYINFO = 6;
    private static final int PHONE_STATE_SIGNALINFO = 7;
    private static final int PHONE_CDMA_CALL_WAITING = 8;
    private static final int PHONE_ENHANCED_VP_ON = 9;
    private static final int PHONE_ENHANCED_VP_OFF = 10;
    private static final int PHONE_RINGBACK_TONE = 11;
    private static final int PHONE_RESEND_MUTE = 12;
    private static final int PHONE_VT_RING_INFO = 13;
    private static final int PHONE_WAITING_DISCONNECT = 15;
    private static final int PHONE_WAITING_DISCONNECT_STOP_TONE_PLAYER = 18;

    // Events generated internally:
    private static final int PHONE_MWI_CHANGED = 21;
    private static final int CALLWAITING_CALLERINFO_DISPLAY_DONE = 22;
    private static final int CALLWAITING_ADDCALL_DISABLE_TIMEOUT = 23;
    private static final int DISPLAYINFO_NOTIFICATION_DONE = 24;
    private static final int EVENT_OTA_PROVISION_CHANGE = 25;
    private static final int CDMA_CALL_WAITING_REJECT = 26;
    private static final int UPDATE_IN_CALL_NOTIFICATION = 27;

    private static final int FAKE_SIP_PHONE_INCOMING_RING = 42;
    private static final int FAKE_SIP_PHONE_INCOMING_RING_DELAY = 2000;

    private static final int DISPLAY_BUSY_MESSAGE = 50;

    // Emergency call related defines:
    private static final int EMERGENCY_TONE_OFF = 0;
    private static final int EMERGENCY_TONE_ALERT = 1;
    private static final int EMERGENCY_TONE_VIBRATE = 2;

    private PhoneGlobals mApplication;
    private Phone mPhone;
    private CallManager mCM;
    private MTKCallManager mCMGemini;
    private Ringer mRinger;
    private BluetoothHeadset mBluetoothHeadset;
    private CallLogAsync mCallLog;
    private boolean mSilentRingerRequested;
    private PhoneConstants.State mLastState = PhoneConstants.State.IDLE;

    // ToneGenerator instance for playing SignalInfo tones
    private ToneGenerator mSignalInfoToneGenerator;

    // The tone volume relative to other sounds in the stream SignalInfo
    private static final int TONE_RELATIVE_VOLUME_SIGNALINFO = 80;

    private Call.State mPreviousCdmaCallState;
    private boolean mVoicePrivacyState = false;
    private boolean mIsCdmaRedialCall = false;

    // Emergency call tone and vibrate:
    private int mIsEmergencyToneOn;
    private int mCurrentEmergencyToneState = EMERGENCY_TONE_OFF;
    private EmergencyTonePlayerVibrator mEmergencyTonePlayerVibrator;

    private Vibrator mVibrator;
    private Call.State mPreviousCallState = Call.State.IDLE;
    // Ringback tone player
    private InCallTonePlayer mInCallRingbackTonePlayer;

    // Call waiting tone player
    private InCallTonePlayer mCallWaitingTonePlayer;
    
    //Video call waiting tone player when voice call,Voice call waiting tone player when video call
    private InCallTonePlayer mVideoOrVoiceCallWaitingTonePlayer = null;

    //This flag used to indicate if play the tone when the contact is blocked
    private boolean ok2Ring = true;
    
    // Cached AudioManager
    private AudioManager mAudioManager;
    public static boolean mIsWaitingQueryComplete = true;
    DualTalkUtils mDualTalk;
    
    InCallTonePlayer mToneThread = null;
    
    /**
     * Initialize the singleton CallNotifier instance.
     * This is only done once, at startup, from PhoneGlobals.onCreate().
     */
    /* package */ static CallNotifier init(PhoneGlobals app, Phone phone, Ringer ringer,
                                           CallLogAsync callLog) {
        synchronized (CallNotifier.class) {
            if (sInstance == null) {
                sInstance = new CallNotifier(app, phone, ringer, callLog);
            } else {
                Log.wtf(LOG_TAG, "init() called multiple times!  sInstance = " + sInstance);
            }
            return sInstance;
        }
    }

    /** Private constructor; @see init() */
    private CallNotifier(PhoneGlobals app, Phone phone, Ringer ringer,
                         CallLogAsync callLog) {
        mApplication = app;
        mPhone = phone;
        mCM = app.mCM;
        mCMGemini = app.mCMGemini;
        mCallLog = callLog;

        if (DualTalkUtils.isSupportDualTalk()) {
            mDualTalk = DualTalkUtils.getInstance();
        }

        mAudioManager = (AudioManager) mApplication.getSystemService(Context.AUDIO_SERVICE);

        registerForNotifications();

        if (mCM.getFgPhone().getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA) {
            // Instantiate the ToneGenerator for SignalInfo and CallWaiting
            // TODO: We probably don't need the mSignalInfoToneGenerator instance
            // around forever. Need to change it so as to create a ToneGenerator instance only
            // when a tone is being played and releases it after its done playing.
            try {
                mSignalInfoToneGenerator = new ToneGenerator(AudioManager.STREAM_VOICE_CALL,
                        TONE_RELATIVE_VOLUME_SIGNALINFO);
            } catch (RuntimeException e) {
                Log.w(LOG_TAG, "CallNotifier: Exception caught while creating " +
                        "mSignalInfoToneGenerator: " + e);
                mSignalInfoToneGenerator = null;
            }
        }

        mRinger = ringer;
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            adapter.getProfileProxy(mApplication.getApplicationContext(),
                                    mBluetoothProfileServiceListener,
                                    BluetoothProfile.HEADSET);
        }

        /// M:Gemini+
        listenPhoneState();

        /**
         * add by mediatek .inc
         */
        mBlackListManager = new BlackListManager(mApplication);
        SIMInfoWrapper.getDefault().registerForSimInfoUpdate(this, EVENT_SIMINFO_CHANGED, null);
        /**
         * add by mediatek .inc
         */
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case PHONE_NEW_RINGING_CONNECTION:
                if (DBG) log("RINGING... (new)");
                onNewRingingConnection((AsyncResult) msg.obj);
                mSilentRingerRequested = false;
                break;

            case PHONE_INCOMING_RING:
                if (DBG) log("PHONE_INCOMING_RING ! ");
                // repeat the ring when requested by the RIL, and when the user has NOT
                // specifically requested silence.
                if (msg.obj != null && ((AsyncResult) msg.obj).result != null) {
                    PhoneBase pb =  (PhoneBase)((AsyncResult)msg.obj).result;
                    boolean bSipRing = pb instanceof SipPhone;
                    boolean bIsRejected = false;
                    Call ringCall = pb.getRingingCall();
                    if (null != ringCall) {
                        bIsRejected = PhoneUtils.getShouldSendToVoiceMailFlag(ringCall.getLatestConnection());
                    }
                    if ((pb.getState() == PhoneConstants.State.RINGING)
                            && (!mSilentRingerRequested)
                            && (!bIsRejected)
                            && ok2Ring) {
                        if (DBG) log("RINGING... (PHONE_INCOMING_RING event)");
                        boolean provisioned = Settings.Global.getInt(mApplication.getContentResolver(),
                        Settings.Global.DEVICE_PROVISIONED, 0) != 0;
                        //For sip call, the ringer will start in onCustomRingQueryComplete
                        if (provisioned && !bSipRing) {
                            mRinger.ring();
                        }
                    } else {
                        if (DBG) log("RING before NEW_RING, skipping");
                    }
                }
                break;

            case PHONE_VT_RING_INFO:
                if (DBG) log(" - handleMessage : RING INFO for video call ! ");
                if (VTCallUtils.isVTRinging() && (!mSilentRingerRequested)) {
                    if (DBG) log("RINGING... (PHONE_VT_RING_INFO event)");
                    boolean provisioned2 = Settings.Global.getInt(mApplication.getContentResolver(),
                    Settings.Global.DEVICE_PROVISIONED, 0) != 0;
                    if (provisioned2) {
                        mRinger.ring();
                    }
                } else {
                    if (DBG) log("RING before NEW_RING, skipping");
                }
                break;
                
            case PHONE_WAITING_DISCONNECT:
                /// M:Gemini+ VT slot id.
                final int simId = GeminiUtils.get3GCapabilitySIM();
                if (DBG) log(" - handleMessage : PHONE_WAITING_DISCONNECT ! simId=" + simId);
                onDisconnectForVTWaiting((AsyncResult) msg.obj, simId);
                break;
            
            case PHONE_WAITING_DISCONNECT_STOP_TONE_PLAYER:
                if (mVideoOrVoiceCallWaitingTonePlayer != null) {
                    mVideoOrVoiceCallWaitingTonePlayer.stopTone();
                    mVideoOrVoiceCallWaitingTonePlayer = null;
                }
                break;

            case PHONE_STATE_CHANGED:
                log("CallNotifier Phone state change");
                onPhoneStateChanged((AsyncResult) msg.obj);
                break;
            case PHONE_DISCONNECT:
            case PHONE_DISCONNECT2:
            case PHONE_DISCONNECT3:
            case PHONE_DISCONNECT4:
                if (DBG) log("DISCONNECT");
                AsyncResult r = (AsyncResult) msg.obj;
                Connection connection = (Connection) r.result;
                if ((!connection.isIncoming() ||
                        !PhoneUtils.getShouldSendToVoiceMailFlag(connection))
                        && ok2Ring) {
                    mApplication.wakeUpScreen();
                }
                final int disconnectSlotId = GeminiRegister.getSlotIdByRegisterEvent(msg.what, PHONE_DISCONNECT_GEMINI);
                onDisconnect((AsyncResult) msg.obj, disconnectSlotId);
                break;

            case PHONE_UNKNOWN_CONNECTION_APPEARED:
                onUnknownConnectionAppeared((AsyncResult) msg.obj);
                break;

            case RINGER_CUSTOM_RINGTONE_QUERY_TIMEOUT:
                onCustomRingtoneQueryTimeout((String) msg.obj);
                break;

            case PHONE_MWI_CHANGED:
            case PHONE_MWI_CHANGED2:
            case PHONE_MWI_CHANGED3:
            case PHONE_MWI_CHANGED4:
                final int mwiSlotId = GeminiRegister.getSlotIdByRegisterEvent(msg.what, PHONE_MWI_CHANGED_GEMINI);
                onMwiChanged(GeminiUtils.getMessageWaitingIndicator(mApplication.phone, mwiSlotId), mwiSlotId);
                break;

            case PHONE_CDMA_CALL_WAITING:
                if (DBG) log("Received PHONE_CDMA_CALL_WAITING event");
                onCdmaCallWaiting((AsyncResult) msg.obj);
                break;

            case CDMA_CALL_WAITING_REJECT:
                Log.i(LOG_TAG, "Received CDMA_CALL_WAITING_REJECT event");
                onCdmaCallWaitingReject();
                break;

            case CALLWAITING_CALLERINFO_DISPLAY_DONE:
                Log.i(LOG_TAG, "Received CALLWAITING_CALLERINFO_DISPLAY_DONE event");
                mCallWaitingTimeOut = true;
                onCdmaCallWaitingReject();
                break;

            case CALLWAITING_ADDCALL_DISABLE_TIMEOUT:
                if (DBG) log("Received CALLWAITING_ADDCALL_DISABLE_TIMEOUT event ...");
                // Set the mAddCallMenuStateAfterCW state to true
                mApplication.cdmaPhoneCallState.setAddCallMenuStateAfterCallWaiting(true);
                mApplication.updateInCallScreen();
                break;

            case PHONE_STATE_DISPLAYINFO:
                if (DBG) log("Received PHONE_STATE_DISPLAYINFO event");
                onDisplayInfo((AsyncResult) msg.obj);
                break;

            case PHONE_STATE_SIGNALINFO:
                if (DBG) log("Received PHONE_STATE_SIGNALINFO event");
                onSignalInfo((AsyncResult) msg.obj);
                break;

            case DISPLAYINFO_NOTIFICATION_DONE:
                if (DBG) log("Received Display Info notification done event ...");
                CdmaDisplayInfo.dismissDisplayInfoRecord();
                break;

            case EVENT_OTA_PROVISION_CHANGE:
                if (DBG) log("EVENT_OTA_PROVISION_CHANGE...");
                mApplication.handleOtaspEvent(msg);
                break;

            case PHONE_ENHANCED_VP_ON:
                if (DBG) log("PHONE_ENHANCED_VP_ON...");
                if (!mVoicePrivacyState) {
                    int toneToPlay = InCallTonePlayer.TONE_VOICE_PRIVACY;
                    new InCallTonePlayer(toneToPlay).start();
                    mVoicePrivacyState = true;
                    // Update the VP icon:
                    if (DBG) log("- updating notification for VP state...");
                    mApplication.notificationMgr.updateInCallNotification();
                }
                break;

            case PHONE_ENHANCED_VP_OFF:
                if (DBG) log("PHONE_ENHANCED_VP_OFF...");
                if (mVoicePrivacyState) {
                    int toneToPlay = InCallTonePlayer.TONE_VOICE_PRIVACY;
                    new InCallTonePlayer(toneToPlay).start();
                    mVoicePrivacyState = false;
                    // Update the VP icon:
                    if (DBG) log("- updating notification for VP state...");
                    mApplication.notificationMgr.updateInCallNotification();
                }
                break;

            case PHONE_RINGBACK_TONE:
                if (DBG) log("- receive the ring back...");
                onRingbackTone((AsyncResult) msg.obj);
                break;

            case PHONE_RESEND_MUTE:
                onResendMute();
                break;

            case DISPLAY_BUSY_MESSAGE:
                //This is request by brazil vivo
                if (FeatureOption.MTK_BRAZIL_CUSTOMIZATION_VIVO) {
                    Toast.makeText(PhoneGlobals.getInstance().getApplicationContext(),
                            R.string.callFailed_userBusy,
                            Toast.LENGTH_SHORT).show();
                }
                break;
            case UPDATE_IN_CALL_NOTIFICATION:
                mApplication.notificationMgr.updateInCallNotification();
                break;

            case FAKE_SIP_PHONE_INCOMING_RING:
                checkAndTriggerRingTone();
                break;

            case EVENT_SIMINFO_CHANGED:
                for (int slot = 0; slot < mCfiStatus.length; slot++) {
                    if (mCfiStatus[slot]) {
                        onCfiChanged(true, slot);
                    }
                }
                break;

            default:
                // super.handleMessage(msg);
        }
    }

    /**
     * Handles a "new ringing connection" event from the telephony layer.
     */
    private void onNewRingingConnection(AsyncResult r) {
        Connection c = (Connection) r.result;
        log("onNewRingingConnection(): state = " + mCM.getState() + ", conn = { " + c + " }");
        Call ringing = c.getCall();
        Phone phone = ringing.getPhone();
        //Assume ringing is ok.
        ok2Ring = true;
        // Check for a few cases where we totally ignore incoming calls.
        if (ignoreAllIncomingCalls(phone)) {
            // Immediately reject the call, without even indicating to the user
            // that an incoming call occurred.  (This will generally send the
            // caller straight to voicemail, just as if we *had* shown the
            // incoming-call UI and the user had declined the call.)
            PhoneUtils.hangupRingingCall(ringing);
            ok2Ring = false;
            return;
        }

        if (c == null) {
            Log.w(LOG_TAG, "CallNotifier.onNewRingingConnection(): null connection!");
            // Should never happen, but if it does just bail out and do nothing.
            return;
        }

        if (!c.isRinging()) {
            Log.i(LOG_TAG, "CallNotifier.onNewRingingConnection(): connection not ringing!");
            // This is a very strange case: an incoming call that stopped
            // ringing almost instantly after the onNewRingingConnection()
            // event.  There's nothing we can do here, so just bail out
            // without doing anything.  (But presumably we'll log it in
            // the call log when the disconnect event comes in...)
            return;
        }

        if (DualTalkUtils.isSupportDualTalk()) {
            if (mDualTalk == null) {
                mDualTalk = DualTalkUtils.getInstance();
            }
            
            //Check if this ringcall is allowed            
            if (ringing != null && mDualTalk.isAllowedIncomingCall(ringing)) {
                mDualTalk.switchPhoneByNeededForRing(ringing.getPhone());
            } else {
                try {
                    ringing.hangup();
                } catch (CallStateException e) {
                    log(e.toString());
                }
                return;
            }
        }
        
        // Stop any signalInfo tone being played on receiving a Call
        stopSignalInfoTone();

        Call.State state = c.getState();
        // State will be either INCOMING or WAITING.
        if (VDBG) log("- connection is ringing!  state = " + state);
        // if (DBG) PhoneUtils.dumpCallState(mPhone);

        // No need to do any service state checks here (like for
        // "emergency mode"), since in those states the SIM won't let
        // us get incoming connections in the first place.

        // TODO: Consider sending out a serialized broadcast Intent here
        // (maybe "ACTION_NEW_INCOMING_CALL"), *before* starting the
        // ringer and going to the in-call UI.  The intent should contain
        // the caller-id info for the current connection, and say whether
        // it would be a "call waiting" call or a regular ringing call.
        // If anybody consumed the broadcast, we'd bail out without
        // ringing or bringing up the in-call UI.
        //
        // This would give 3rd party apps a chance to listen for (and
        // intercept) new ringing connections.  An app could reject the
        // incoming call by consuming the broadcast and doing nothing, or
        // it could "pick up" the call (without any action by the user!)
        // via some future TelephonyManager API.
        //
        // See bug 1312336 for more details.
        // We'd need to protect this with a new "intercept incoming calls"
        // system permission.

        // Obtain a partial wake lock to make sure the CPU doesn't go to
        // sleep before we finish bringing up the InCallScreen.
        // (This will be upgraded soon to a full wake lock; see
        // showIncomingCall().)
        //if (VDBG) log("Holding wake lock on new incoming connection.");
        //mApplication.requestWakeState(PhoneGlobals.WakeState.PARTIAL);

        // - don't ring for call waiting connections
        // - do this before showing the incoming call panel
        if (PhoneUtils.isRealIncomingCall(state)) {
            PhoneUtils.setAudioControlState(PhoneUtils.AUDIO_RINGING);
            startIncomingCallQuery(c);

            /// M: for ALPS00569727 @{
            // require audio focus in advance when there is only ringing call exist (isRealIncomingCall).
            // Note: setAudioMode() is not very safe, need carefully checked before using it.
            // MTK add
            PhoneUtils.setAudioMode();
            /// @}

        } else {
            if (VDBG) log("- starting call waiting tone...");
            mIsWaitingQueryComplete = false;
            if (mCallWaitingTonePlayer == null) {
                mCallWaitingTonePlayer = new InCallTonePlayer(InCallTonePlayer.TONE_CALL_WAITING);
                mCallWaitingTonePlayer.start();
            }
            startIncomingCallQuery(c);
            // in this case, just fall through like before, and call
            // showIncomingCall().
            //if (DBG) log("- showing incoming call (this is a WAITING call)...");
            //showIncomingCall();
        }

        // Note we *don't* post a status bar notification here, since
        // we're not necessarily ready to actually show the incoming call
        // to the user.  (For calls in the INCOMING state, at least, we
        // still need to run a caller-id query, and we may not even ring
        // at all if the "send directly to voicemail" flag is set.)
        //
        // Instead, we update the notification (and potentially launch the
        // InCallScreen) from the showIncomingCall() method, which runs
        // when the caller-id query completes or times out.

        //auto answer voice call when use voice answer VT call and 
        //mVTVoiceAnswer is true
        if (FeatureOption.MTK_PHONE_VT_VOICE_ANSWER
                && FeatureOption.MTK_VT3G324M_SUPPORT
                && VTInCallScreenFlags.getInstance().mVTVoiceAnswer) {
            log("mVTVoiceAnswerPhoneNumber = " + VTInCallScreenFlags.getInstance().mVTVoiceAnswerPhoneNumber);
            log("mVTVoiceAnswer = " + VTInCallScreenFlags.getInstance().mVTVoiceAnswer);
            VTInCallScreenFlags.getInstance().mVTVoiceAnswer = false;
            String strPhoneNumber = null;
            Connection con = ringing.getLatestConnection();
            if (con != null)
                strPhoneNumber = con.getAddress();
            if (null != strPhoneNumber
                    && VTInCallScreenFlags.getInstance().mVTVoiceAnswerPhoneNumber.equals(strPhoneNumber)) {
                SIMInfo simInfo = PhoneUtils.getSimInfoByCall(ringing);
                if (null != simInfo) {
                    VTSettingUtils.getInstance().updateVTSettingState(simInfo.mSlot);
                    if (VTSettingUtils.getInstance().mRingOnlyOnce) {
                        VTInCallScreenFlags.getInstance().mVTVoiceAnswerPhoneNumber = null;
                        autoVTVoiceAnswerCall(ringing);
                    }
                } else {
                    log("simInfo is wrong");
                }
            } else {
                log("strPhoneNumber is wrong");
            }
        }
        if (VDBG) log("- onNewRingingConnection() done.");
    }

    /**
     * auto answer voice call when use voice answer VT call and 
     * mVTVoiceAnswer is true
     */
    public void autoVTVoiceAnswerCall(final Call ringing) {
        log("autointernalAnswerCall()...");
        final boolean hasRingingCall = mCM.hasActiveRingingCall();

        if (hasRingingCall) {
            Phone phone = mCM.getRingingPhone();
            int phoneType = phone.getPhoneType();
            if (phoneType == PhoneConstants.PHONE_TYPE_GSM) {
                // GSM: this is usually just a wrapper around
                // PhoneUtils.answerCall(), *but* we also need to do
                // something special for the "both lines in use" case.

                final boolean hasActiveCall = mCM.hasActiveFgCall();
                final boolean hasHoldingCall = mCM.hasActiveBgCall();

                if (FeatureOption.MTK_VT3G324M_SUPPORT) {
                    if (VTCallUtils.isVTRinging()) {
                        if (DBG)
                            log("autointernalAnswerCall: is VT ringing now,"
                                    + " so call PhoneUtils.answerCall(ringing) anyway !");
                        return;
                    }
                }
                if (hasActiveCall && hasHoldingCall) {
                    if (DBG) log("autointernalAnswerCall: answering (both lines in use!)...");
                    // The relatively rare case where both lines are
                    // already in use.  We "answer incoming, end ongoing"
                    // in this case, according to the current UI spec.
                    PhoneUtils.answerAndEndActive(mCM, mCM.getFirstActiveRingingCall());
//MTK add below one line:
                    mApplication.getInCallScreenInstance().setOnAnswerAndEndFlag(true);

                    // Alternatively, we could use
                    // PhoneUtils.answerAndEndHolding(mPhone);
                    // here to end the on-hold call instead.
                } else {
                    if (DBG) log("autointernalAnswerCall: answering...");
                    PhoneUtils.answerCall(ringing);  // Automatically holds the current active call,
                                                    // if there is one
                }
            } else {
                if (DBG) log("phone type: " + phoneType);
            }
            // Call origin is valid only with outgoing calls. Disable it on incoming calls.
            mApplication.setLatestActiveCallOrigin(null);
        }
    }

    /**
     * Determines whether or not we're allowed to present incoming calls to the
     * user, based on the capabilities and/or current state of the device.
     *
     * If this method returns true, that means we should immediately reject the
     * current incoming call, without even indicating to the user that an
     * incoming call occurred.
     *
     * (We only reject incoming calls in a few cases, like during an OTASP call
     * when we can't interrupt the user, or if the device hasn't completed the
     * SetupWizard yet.  We also don't allow incoming calls on non-voice-capable
     * devices.  But note that we *always* allow incoming calls while in ECM.)
     *
     * @return true if we're *not* allowed to present an incoming call to
     * the user.
     */
    private boolean ignoreAllIncomingCalls(Phone phone) {
        // Incoming calls are totally ignored on non-voice-capable devices.
        if (!PhoneGlobals.sVoiceCapable) {
            // ...but still log a warning, since we shouldn't have gotten this
            // event in the first place!  (Incoming calls *should* be blocked at
            // the telephony layer on non-voice-capable capable devices.)
            Log.w(LOG_TAG, "Got onNewRingingConnection() on non-voice-capable device! Ignoring...");
            return true;
        }

        // In ECM (emergency callback mode), we ALWAYS allow incoming calls
        // to get through to the user.  (Note that ECM is applicable only to
        // voice-capable CDMA devices).
        if (PhoneUtils.isPhoneInEcm(phone)) {
            if (DBG) log("Incoming call while in ECM: always allow...");
            return false;
        }

        // Incoming calls are totally ignored if the device isn't provisioned yet.
        boolean provisioned = Settings.Global.getInt(mApplication.getContentResolver(),
            Settings.Global.DEVICE_PROVISIONED, 0) != 0;
        if (!provisioned) {
            Log.i(LOG_TAG, "Ignoring incoming call: not provisioned");
            return true;
        }

        // Incoming calls are totally ignored if an OTASP call is active.
        if (TelephonyCapabilities.supportsOtasp(phone)) {
            boolean activateState = (mApplication.cdmaOtaScreenState.otaScreenState
                    == OtaUtils.CdmaOtaScreenState.OtaScreenState.OTA_STATUS_ACTIVATION);
            boolean dialogState = (mApplication.cdmaOtaScreenState.otaScreenState
                    == OtaUtils.CdmaOtaScreenState.OtaScreenState.OTA_STATUS_SUCCESS_FAILURE_DLG);
            boolean spcState = mApplication.cdmaOtaProvisionData.inOtaSpcState;

            if (spcState) {
                Log.i(LOG_TAG, "Ignoring incoming call: OTA call is active");
                return true;
            } else if (activateState || dialogState) {
                // We *are* allowed to receive incoming calls at this point.
                // But clear out any residual OTASP UI first.
                // TODO: It's an MVC violation to twiddle the OTA UI state here;
                // we should instead provide a higher-level API via OtaUtils.
                if (dialogState) mApplication.dismissOtaDialogs();
                mApplication.clearOtaState();
                mApplication.clearInCallScreenMode();
                return false;
            }
        }

        /// M: black list and auto reject @{
        // MTK add
        final Call call = mCM.getFirstActiveRingingCall();
        Connection c = call.getLatestConnection();
        if (c != null) {
            String address = c.getAddress();
            if (!TextUtils.isEmpty(address)) {
                // check the call is in black list or not.
                boolean isInBlackList = mBlackListManager.shouldBlock(address,
                        c.isVideo() ? BlackListManager.VIDEO_CALL_REJECT_MODE : BlackListManager.VOICE_CALL_REJECT_MODE);
                // check the call should be auto rejected or not.
                // using CallerInfoCache'sendToVoicemail instead of query from startGetCallerInfo().
                // see ALPS00580998; ALPS00581104
                boolean isAutoReject = false;
                final CallerInfoCache.CacheEntry entry = mApplication.callerInfoCache.getCacheEntry(address);
                if (entry != null) {
                    isAutoReject = entry.sendToVoicemail;
                }
                log("the call should be rejected by black list or auto reject: " + isInBlackList + " / " + isAutoReject);
                if (isInBlackList || isAutoReject) {
                    CallerInfo callerInfo = new CallerInfo();
                    callerInfo.shouldSendToVoicemail = true;
                    c.setUserData((Object)callerInfo);
                    return true;
                }
            }
        }
        /// @}

        // Normal case: allow this call to be presented to the user.
        return false;
    }

    /**
     * Helper method to manage the start of incoming call queries
     */
    private void startIncomingCallQuery(Connection c) {
        // TODO: cache the custom ringer object so that subsequent
        // calls will not need to do this query work.  We can keep
        // the MRU ringtones in memory.  We'll still need to hit
        // the database to get the callerinfo to act as a key,
        // but at least we can save the time required for the
        // Media player setup.  The only issue with this is that
        // we may need to keep an eye on the resources the Media
        // player uses to keep these ringtones around.

        // make sure we're in a state where we can be ready to
        // query a ringtone uri.
        boolean shouldStartQuery = false;
        synchronized (mCallerInfoQueryStateGuard) {
            if (mCallerInfoQueryState == CALLERINFO_QUERY_READY) {
                mCallerInfoQueryState = CALLERINFO_QUERYING;
                shouldStartQuery = true;
            }
        }
        if (shouldStartQuery) {
            // Reset the ringtone to the default first.

            /// M: For VT and ALPS00571388 @{
            //     When message of "PHONE_INCOMING_RING" comes too soon in handleMessage() that
            // query is just in running(neither complete, nor time out(500ms)), there will play the ringtone which is set here;
            // So try to set custom ringtone from CallerInfoCache instead of DEFAULT_RINGTONE_URI as default.
            //
            // Original Code
            // mRinger.setCustomRingtoneUri(Settings.System.DEFAULT_RINGTONE_URI);
            setDefaultRingtoneUri(c);
            /// @}

            // query the callerinfo to try to get the ringer.
            PhoneUtils.CallerInfoToken cit = PhoneUtils.startGetCallerInfo(
                    mApplication, c, this, this);

            // if this has already been queried then just ring, otherwise
            // we wait for the alloted time before ringing.
            if (cit.isFinal) {
                if (VDBG) log("- CallerInfo already up to date, using available data");
                onQueryComplete(0, this, cit.currentInfo);
            } else {
                if (VDBG) log("- Starting query, posting timeout message.");

                // Phone number (via getAddress()) is stored in the message to remember which
                // number is actually used for the look up.
                sendMessageDelayed(
                        Message.obtain(this, RINGER_CUSTOM_RINGTONE_QUERY_TIMEOUT, c.getAddress()),
                        RINGTONE_QUERY_WAIT_TIME);
            }
            // The call to showIncomingCall() will happen after the
            // queries are complete (or time out).
        } else {
            // This should never happen; its the case where an incoming call
            // arrives at the same time that the query is still being run,
            // and before the timeout window has closed.
            EventLog.writeEvent(EventLogTags.PHONE_UI_MULTIPLE_QUERY);

            // In this case, just log the request and ring.
            //For sip call, we have only one chance to ring, so don't miss it
            if (mCM.getRingingPhone().getPhoneType() == PhoneConstants.PHONE_TYPE_SIP) {
                if (VDBG) log("RINGING... (request to ring arrived while query is running)");
                mRinger.ring();
            }

            // in this case, just fall through like before, and call
            // showIncomingCall().
            if (DBG) log("- showing incoming call (couldn't start query)...");

            if (FeatureOption.MTK_VT3G324M_SUPPORT) {
                if (VTCallUtils.isVTRinging()) {
                    VTCallUtils.showVTIncomingCallUi();
                } else {
                    showIncomingCall();
                }
            } else {
                showIncomingCall();
            }
        }
    }

    /**
     * Performs the final steps of the onNewRingingConnection sequence:
     * starts the ringer, and brings up the "incoming call" UI.
     *
     * Normally, this is called when the CallerInfo query completes (see
     * onQueryComplete()).  In this case, onQueryComplete() has already
     * configured the Ringer object to use the custom ringtone (if there
     * is one) for this caller.  So we just tell the Ringer to start, and
     * proceed to the InCallScreen.
     *
     * But this method can *also* be called if the
     * RINGTONE_QUERY_WAIT_TIME timeout expires, which means that the
     * CallerInfo query is taking too long.  In that case, we log a
     * warning but otherwise we behave the same as in the normal case.
     * (We still tell the Ringer to start, but it's going to use the
     * default ringtone.)
     */
    private void onCustomRingQueryComplete() {
        boolean isQueryExecutionTimeExpired = false;
        synchronized (mCallerInfoQueryStateGuard) {
            if (mCallerInfoQueryState == CALLERINFO_QUERYING) {
                mCallerInfoQueryState = CALLERINFO_QUERY_READY;
                isQueryExecutionTimeExpired = true;
            }
        }
        if (isQueryExecutionTimeExpired) {
            // There may be a problem with the query here, since the
            // default ringtone is playing instead of the custom one.
            Log.w(LOG_TAG, "CallerInfo query took too long; falling back to default ringtone");
            EventLog.writeEvent(EventLogTags.PHONE_UI_RINGER_QUERY_ELAPSED);
        }

        // Make sure we still have an incoming call!
        //
        // (It's possible for the incoming call to have been disconnected
        // while we were running the query.  In that case we better not
        // start the ringer here, since there won't be any future
        // DISCONNECT event to stop it!)
        //
        // Note we don't have to worry about the incoming call going away
        // *after* this check but before we call mRinger.ring() below,
        // since in that case we *will* still get a DISCONNECT message sent
        // to our handler.  (And we will correctly stop the ringer when we
        // process that event.)
        if (mCM.getState() != PhoneConstants.State.RINGING) {
            Log.i(LOG_TAG, "onCustomRingQueryComplete: No incoming call! Bailing out...");
            // Don't start the ringer *or* bring up the "incoming call" UI.
            // Just bail out.
            return;
        }

        // Ring, either with the queried ringtone or default one.        
        // It seems that CDMA also no ring message report
        if (mCM.getRingingPhone().getPhoneType() == PhoneConstants.PHONE_TYPE_SIP
                || mCM.getRingingPhone().getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA) {
            if (VDBG) log("RINGING... (onCustomRingQueryComplete)");
            mRinger.ring();
        }

        // ...and display the incoming call to the user:
        if (DBG) log("- showing incoming call (custom ring query complete)...");

        if (FeatureOption.MTK_VT3G324M_SUPPORT) {
            if (VTCallUtils.isVTRinging()) {
                VTCallUtils.showVTIncomingCallUi();
            } else {
                showIncomingCall();
            }
        } else {
            showIncomingCall();
        }
    }

    private void onUnknownConnectionAppeared(AsyncResult r) {
        PhoneConstants.State state = mCM.getState();

        if (state == PhoneConstants.State.OFFHOOK) {
            // basically do onPhoneStateChanged + display the incoming call UI
            onPhoneStateChanged(r);
            if (DBG) log("- showing incoming call (unknown connection appeared)...");

            if (FeatureOption.MTK_VT3G324M_SUPPORT) {
                if (VTCallUtils.isVTRinging()) {
                    VTCallUtils.showVTIncomingCallUi();
                } else {
                    showIncomingCall();
        }
        } else {
                showIncomingCall();
            }
        }
    }

    /**
     * Informs the user about a new incoming call.
     *
     * In most cases this means "bring up the full-screen incoming call
     * UI".  However, if an immersive activity is running, the system
     * NotificationManager will instead pop up a small notification window
     * on top of the activity.
     *
     * Watch out: be sure to call this method only once per incoming call,
     * or otherwise we may end up launching the InCallScreen multiple
     * times (which can lead to slow responsiveness and/or visible
     * glitches.)
     *
     * Note this method handles only the onscreen UI for incoming calls;
     * the ringer and/or vibrator are started separately (see the various
     * calls to Ringer.ring() in this class.)
     *
     * @see NotificationMgr#updateNotificationAndLaunchIncomingCallUi()
     */
    private void showIncomingCall() {
        log("showIncomingCall()...  phone state = " + mCM.getState());

        // Before bringing up the "incoming call" UI, force any system
        // dialogs (like "recent tasks" or the power dialog) to close first.
        try {
            ActivityManagerNative.getDefault().closeSystemDialogs("call");
        } catch (RemoteException e) {
        }

        // Go directly to the in-call screen.
        // (No need to do anything special if we're already on the in-call
        // screen; it'll notice the phone state change and update itself.)
        mApplication.requestWakeState(PhoneGlobals.WakeState.FULL);

        // Post the "incoming call" notification *and* include the
        // fullScreenIntent that'll launch the incoming-call UI.
        // (This will usually take us straight to the incoming call
        // screen, but if an immersive activity is running it'll just
        // appear as a notification.)

        if (DBG) log("- updating notification from showIncomingCall()...");
        mApplication.notificationMgr.updateNotificationAndLaunchIncomingCallUi();
    }

    /**
     * Updates the phone UI in response to phone state changes.
     *
     * Watch out: certain state changes are actually handled by their own
     * specific methods:
     *   - see onNewRingingConnection() for new incoming calls
     *   - see onDisconnect() for calls being hung up or disconnected
     */
    private void onPhoneStateChanged(AsyncResult r) {
        PhoneConstants.State state = mCM.getState();
        if (VDBG) log("onPhoneStateChanged: state = " + state);

        if (DualTalkUtils.isSupportDualTalk()) {
            if (mDualTalk == null) {
                mDualTalk = DualTalkUtils.getInstance();
            }
            mDualTalk.updateState();
        }
        
        if (mVibrator == null) {
            mVibrator = (Vibrator) mApplication.getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
        }

        //This has been supported in 4.1
        //mApplication.requestAudioFocus(state);
        // Turn status bar notifications on or off depending upon the state
        // of the phone.  Notification Alerts (audible or vibrating) should
        // be on if and only if the phone is IDLE.
        mApplication.notificationMgr.statusBarHelper
                .enableNotificationAlerts(state == PhoneConstants.State.IDLE);

        Phone fgPhone = mCM.getFgPhone();
        if (fgPhone.getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA) {
            if ((fgPhone.getForegroundCall().getState() == Call.State.ACTIVE)
                    && ((mPreviousCdmaCallState == Call.State.DIALING)
                    ||  (mPreviousCdmaCallState == Call.State.ALERTING))) {
                if (mIsCdmaRedialCall) {
                    int toneToPlay = InCallTonePlayer.TONE_REDIAL;
                    new InCallTonePlayer(toneToPlay).start();
                }
                // Stop any signal info tone when call moves to ACTIVE state
                stopSignalInfoTone();
            }
            mPreviousCdmaCallState = fgPhone.getForegroundCall().getState();
        }

        /*mApplication.updatePhoneState(state) must be in the front 
         of mApplication.updateBluetoothIndication(false)*/
        // Update the proximity sensor mode (on devices that have a
        // proximity sensor).
        mApplication.updatePhoneState(state);
        
        // Have the PhoneGlobals recompute its mShowBluetoothIndication
        // flag based on the (new) telephony state.
        // There's no need to force a UI update since we update the
        // in-call notification ourselves (below), and the InCallScreen
        // listens for phone state changes itself.
        mApplication.updateBluetoothIndication(false);

        if (state == PhoneConstants.State.OFFHOOK) {
            // stop call waiting tone if needed when answering
            if (mCallWaitingTonePlayer != null) {
                mCallWaitingTonePlayer.stopTone();
                mCallWaitingTonePlayer = null;
            }

            PhoneUtils.setAudioControlState(PhoneUtils.AUDIO_OFFHOOK);
            if (VDBG) log("onPhoneStateChanged: OFF HOOK");
            // make sure audio is in in-call mode now
            // If Audio Mode is not In Call, then set the Audio Mode.  This
            // changes is needed because for one of the carrier specific test case,
            // call is originated from the lower layer without using the UI, and
            // since calling does not go through DIALING state, it skips the steps
            // of setting the Audio Mode (dialing from STK, GSM also need change mode.)
            Call.State callState = mCM.getActiveFgCallState();

            //After long long discuss, rollback to Google's default way:
            //we call setAudioMode as soon as possible
            if (mAudioManager.getMode() != AudioManager.MODE_IN_CALL) {
                PhoneUtils.setAudioMode(mCM);
            } else if (callState == Call.State.ACTIVE && PhoneUtils.isSupportFeature("TTY")) {
                PhoneUtils.openTTY();
            }

            // if the call screen is showing, let it handle the event,
            // otherwise handle it here.
            if (!mApplication.isShowingCallScreen()) {
                mApplication.requestWakeState(PhoneGlobals.WakeState.SLEEP);
            }

            // Since we're now in-call, the Ringer should definitely *not*
            // be ringing any more.  (This is just a sanity-check; we
            // already stopped the ringer explicitly back in
            // PhoneUtils.answerCall(), before the call to phone.acceptCall().)
            // TODO: Confirm that this call really *is* unnecessary, and if so,
            // remove it!
            if (DBG) log("stopRing()... (OFFHOOK state)");
            mRinger.stopRing();

            // Post a request to update the "in-call" status bar icon.
            //
            // We don't call NotificationMgr.updateInCallNotification()
            // directly here, for two reasons:
            // (1) a single phone state change might actually trigger multiple
            //   onPhoneStateChanged() callbacks, so this prevents redundant
            //   updates of the notification.
            // (2) we suppress the status bar icon while the in-call UI is
            //   visible (see updateInCallNotification()).  But when launching
            //   an outgoing call the phone actually goes OFFHOOK slightly
            //   *before* the InCallScreen comes up, so the delay here avoids a
            //   brief flicker of the icon at that point.

            PowerManager pm = (PowerManager)mApplication.getApplicationContext().getSystemService(Context.POWER_SERVICE);
            if (pm.isScreenOn()) {
                if (DBG) log("- posting UPDATE_IN_CALL_NOTIFICATION request...");
                // Remove any previous requests in the queue
                removeMessages(UPDATE_IN_CALL_NOTIFICATION);
                final int IN_CALL_NOTIFICATION_UPDATE_DELAY = 1000;  // msec
                sendEmptyMessageDelayed(UPDATE_IN_CALL_NOTIFICATION,
                                        IN_CALL_NOTIFICATION_UPDATE_DELAY);
            }
        } else if (state == PhoneConstants.State.RINGING) {
            //ALPS00311901: Trigger the call waiting tone after user accept one incoming call.
            if ((DualTalkUtils.isSupportDualTalk()) && (mCM.hasActiveFgCall() || mCM.hasActiveBgCall())) {
                if (mCallWaitingTonePlayer == null) {
                    mCallWaitingTonePlayer = new InCallTonePlayer(InCallTonePlayer.TONE_CALL_WAITING);
                    mCallWaitingTonePlayer.start();
                    log("Start waiting tone.");
                }
            }
        }

        if (fgPhone.getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA) {
            Connection c = fgPhone.getForegroundCall().getLatestConnection();
            if ((c != null) && (PhoneNumberUtils.isLocalEmergencyNumber(c.getAddress(),
                                                                        mApplication))) {
                if (VDBG) log("onPhoneStateChanged: it is an emergency call.");
                Call.State callState = fgPhone.getForegroundCall().getState();
                if (mEmergencyTonePlayerVibrator == null) {
                    mEmergencyTonePlayerVibrator = new EmergencyTonePlayerVibrator();
                }

                if (callState == Call.State.DIALING || callState == Call.State.ALERTING) {
                    mIsEmergencyToneOn = Settings.Global.getInt(
                            mApplication.getContentResolver(),
                            Settings.Global.EMERGENCY_TONE, EMERGENCY_TONE_OFF);
                    if (mIsEmergencyToneOn != EMERGENCY_TONE_OFF &&
                        mCurrentEmergencyToneState == EMERGENCY_TONE_OFF) {
                        if (mEmergencyTonePlayerVibrator != null) {
                            mEmergencyTonePlayerVibrator.start();
                        }
                    }
                } else if (callState == Call.State.ACTIVE) {
                    if (mCurrentEmergencyToneState != EMERGENCY_TONE_OFF) {
                        if (mEmergencyTonePlayerVibrator != null) {
                            mEmergencyTonePlayerVibrator.stop();
                        }
                    }
                }
            }
        }

        if ((fgPhone.getPhoneType() == PhoneConstants.PHONE_TYPE_GSM)
                || (fgPhone.getPhoneType() == PhoneConstants.PHONE_TYPE_SIP)) {
            Call.State callState = mCM.getActiveFgCallState();
            if (!callState.isDialing()) {
                // If call get activated or disconnected before the ringback
                // tone stops, we have to stop it to prevent disturbing.
                if (mInCallRingbackTonePlayer != null) {
                    mInCallRingbackTonePlayer.stopTone();
                    mInCallRingbackTonePlayer = null;
                }
            }
        }
        //new feature MO vibrate start
        //TBD: will replace with mPreviousCallState == Call.State.ALERTING || mPreviousCallState == Call.State.DAILINIG
        if ((mCM.getActiveFgCallState() == Call.State.ACTIVE)
                && (mPreviousCallState != Call.State.IDLE)
                && (mPreviousCallState != Call.State.ACTIVE)
                && (mPreviousCallState != Call.State.HOLDING)
                && (mPreviousCallState != Call.State.DISCONNECTED)
                && (mPreviousCallState != Call.State.DISCONNECTING)) {
            if (DBG) Log.d(LOG_TAG, "onPhoneStateChanged mCM.getActiveFgCallState()= " + mCM.getActiveFgCallState());
            if (DBG) Log.d(LOG_TAG, "onPhoneStateChanged mPreviousCallState= " + mPreviousCallState);
            final int MO_CALL_VIBRATE_TIME = 300;  // msec
            mVibrator.vibrate(MO_CALL_VIBRATE_TIME);
        }
        if (DBG) Log.d(LOG_TAG, "before set value, mPreviousCallState= " + mPreviousCallState);
        mPreviousCallState = mCM.getActiveFgCallState(); 
        if (DBG) Log.d(LOG_TAG, "end after set value, mPreviousCallState= " + mPreviousCallState);
        //new feature add by Jackey, MO vibrate end
    }

    void unregisterCallNotifierRegistrations() {
        if (DBG) Log.d(LOG_TAG, "unregisterCallNotifierRegistrations...");

        removeMessages(PHONE_NEW_RINGING_CONNECTION);
        removeMessages(PHONE_STATE_CHANGED);
        removeMessages(PHONE_DISCONNECT);
        removeMessages(PHONE_INCOMING_RING);

        if (FeatureOption.MTK_VT3G324M_SUPPORT) {
            removeMessages(PHONE_VT_RING_INFO);
            removeMessages(PHONE_WAITING_DISCONNECT);
        }
        Object callManager = GeminiUtils.isGeminiSupport() ? mCMGemini : mCM;

        GeminiRegister.unregisterForDisconnect(callManager, this);
        GeminiRegister.unregisterForIncomingRing(callManager, this);
        GeminiRegister.unregisterForPreciseCallStateChanged(callManager, this);
        GeminiRegister.unregisterForUnknownConnection(callManager, this);
        GeminiRegister.unregisterForNewRingingConnection(callManager, this);
        GeminiRegister.unregisterForRingbackTone(callManager, this);

        // unregister VT
        GeminiRegister.unregisterForVtRingInfo(callManager, this);
        GeminiRegister.unregisterForVtReplaceDisconnect(callManager, this);

        if (GeminiUtils.isGeminiSupport()) {
            final int[] geminiSlots = GeminiUtils.getSlots();
            for (int i = 0; i < geminiSlots.length; i++) {
                mCMGemini.unregisterForCallWaitingGemini(this, geminiSlots[i]);
                mCMGemini.unregisterForDisplayInfoGemini(this, geminiSlots[i]);
                mCMGemini.unregisterForSignalInfoGemini(this, geminiSlots[i]);
                mCMGemini.unregisterForCdmaOtaStatusChangeGemini(this, geminiSlots[i]);
            }

            // need compiler option for C+G project
            mCM.unregisterForCdmaOtaStatusChange2(this);
            mCM.unregisterForCallWaiting2(this);
            mCM.unregisterForDisplayInfo2(this);
            mCM.unregisterForSignalInfo2(this);
            mCM.unregisterForInCallVoicePrivacyOn2(this);
            mCM.unregisterForInCallVoicePrivacyOff2(this);
        } else {
            mCM.unregisterForCallWaiting(this);
            mCM.unregisterForDisplayInfo(this);
            mCM.unregisterForSignalInfo(this);
            mCM.unregisterForCdmaOtaStatusChange(this);

            mCM.unregisterForResendIncallMute(this);
        }
    }

    void updateCallNotifierRegistrationsAfterRadioTechnologyChange() {
        if (DBG) Log.d(LOG_TAG, "updateCallNotifierRegistrationsAfterRadioTechnologyChange...");
        // Unregister all events from the old obsolete phone
        Object callManager = GeminiUtils.isGeminiSupport() ? mCMGemini : mCM;

        GeminiRegister.unregisterForDisconnect(callManager, this);
        GeminiRegister.unregisterForIncomingRing(callManager, this);
        GeminiRegister.unregisterForPreciseCallStateChanged(callManager, this);
        GeminiRegister.unregisterForUnknownConnection(callManager, this);
        GeminiRegister.unregisterForNewRingingConnection(callManager, this);
        GeminiRegister.unregisterForRingbackTone(callManager, this);

        // ungister VT
        GeminiRegister.unregisterForVtRingInfo(callManager, this);
        GeminiRegister.unregisterForVtReplaceDisconnect(callManager, this);


        if (GeminiUtils.isGeminiSupport()) {
            final int[] geminiSlots = GeminiUtils.getSlots();
            for (int slotId : geminiSlots) {
                mCMGemini.unregisterForCallWaitingGemini(this, slotId);
                mCMGemini.unregisterForDisplayInfoGemini(this, slotId);
                mCMGemini.unregisterForSignalInfoGemini(this, slotId);
                mCMGemini.unregisterForCdmaOtaStatusChangeGemini(this, slotId);
            }
            mCM.unregisterForCdmaOtaStatusChange2(this);
            mCM.unregisterForCallWaiting2(this);
            mCM.unregisterForDisplayInfo2(this);
            mCM.unregisterForSignalInfo2(this);
            mCM.unregisterForInCallVoicePrivacyOn2(this);
            mCM.unregisterForInCallVoicePrivacyOff2(this);
        } else {
            mCM.unregisterForCallWaiting(this);
            mCM.unregisterForDisplayInfo(this);
            mCM.unregisterForSignalInfo(this);
            mCM.unregisterForCdmaOtaStatusChange(this);

            mCM.unregisterForResendIncallMute(this);
        }

        // Release the ToneGenerator used for playing SignalInfo and CallWaiting
        if (mSignalInfoToneGenerator != null) {
            mSignalInfoToneGenerator.release();
        }

        // Clear ringback tone player
        mInCallRingbackTonePlayer = null;

        // Clear call waiting tone player
        mCallWaitingTonePlayer = null;

        GeminiRegister.unregisterForInCallVoicePrivacyOn(callManager, this);
        GeminiRegister.unregisterForInCallVoicePrivacyOff(callManager, this);

        // Register all events new to the new active phone
        registerForNotifications();
    }

    private void registerForNotifications() {
        Object callManager = GeminiUtils.isGeminiSupport() ? mCMGemini : mCM;

        GeminiRegister.registerForPreciseCallStateChanged(callManager, this, PHONE_STATE_CHANGED);
        GeminiRegister.registerForNewRingingConnection(callManager, this, PHONE_NEW_RINGING_CONNECTION);
        GeminiRegister.registerForDisconnect(callManager, this, PHONE_DISCONNECT_GEMINI);
        GeminiRegister.registerForUnknownConnection(callManager, this, PHONE_UNKNOWN_CONNECTION_APPEARED);
        GeminiRegister.registerForIncomingRing(callManager, this, PHONE_INCOMING_RING);

        // register VT
        GeminiRegister.registerForVtRingInfo(callManager, this, PHONE_VT_RING_INFO);
        GeminiRegister.registerForVtReplaceDisconnect(callManager, this, PHONE_WAITING_DISCONNECT);

        if (GeminiUtils.isGeminiSupport()) {
            // need compiler option for C+G project
            // we always register these message to avoid use too many feature option
            mCM.registerForCdmaOtaStatusChange2(this, EVENT_OTA_PROVISION_CHANGE, null);
            mCM.registerForCallWaiting2(this, PHONE_CDMA_CALL_WAITING, null);
            mCM.registerForDisplayInfo2(this, PHONE_STATE_DISPLAYINFO, null);
            mCM.registerForSignalInfo2(this, PHONE_STATE_SIGNALINFO, null);
            mCM.registerForInCallVoicePrivacyOn2(this, PHONE_ENHANCED_VP_ON, null);
            mCM.registerForInCallVoicePrivacyOff2(this, PHONE_ENHANCED_VP_OFF, null);
        }

        if (mCM.getFgPhone().getPhoneType() == PhoneConstants.PHONE_TYPE_GSM) {
            GeminiRegister.registerForRingbackTone(callManager, this, PHONE_RINGBACK_TONE);
            if (!GeminiUtils.isGeminiSupport()) {
               mCM.registerForResendIncallMute(this, PHONE_RESEND_MUTE, null);
            }
        } else {
            mCM.registerForCdmaOtaStatusChange(this, EVENT_OTA_PROVISION_CHANGE, null);
            mCM.registerForCallWaiting(this, PHONE_CDMA_CALL_WAITING, null);
            mCM.registerForDisplayInfo(this, PHONE_STATE_DISPLAYINFO, null);
            mCM.registerForSignalInfo(this, PHONE_STATE_SIGNALINFO, null);
            mCM.registerForInCallVoicePrivacyOn(this, PHONE_ENHANCED_VP_ON, null);
            mCM.registerForInCallVoicePrivacyOff(this, PHONE_ENHANCED_VP_OFF, null);
        }
    }

    /**
     * Implemented for CallerInfoAsyncQuery.OnQueryCompleteListener interface.
     * refreshes the CallCard data when it called.  If called with this
     * class itself, it is assumed that we have been waiting for the ringtone
     * and direct to voicemail settings to update.
     */
    @Override
    public void onQueryComplete(int token, Object cookie, CallerInfo ci) {
        if (cookie instanceof CustomInfo) {
            if (VDBG) log("CallerInfo query complete, posting missed call notification");

            mApplication.notificationMgr.notifyMissedCall(ci.name, ci.phoneNumber,
                    ci.phoneLabel, ci.cachedPhoto, ci.cachedPhotoIcon,
                    ((CustomInfo) cookie).date, ((CustomInfo) cookie).callVideo);
        } else if (cookie instanceof CallNotifier) {
            if (VDBG) log("CallerInfo query complete (for CallNotifier), "
                          + "updating state for incoming call..");

            // get rid of the timeout messages
            removeMessages(RINGER_CUSTOM_RINGTONE_QUERY_TIMEOUT);

            boolean isQueryExecutionTimeOK = false;
            synchronized (mCallerInfoQueryStateGuard) {
                if (mCallerInfoQueryState == CALLERINFO_QUERYING) {
                    mCallerInfoQueryState = CALLERINFO_QUERY_READY;
                    isQueryExecutionTimeOK = true;
                }
            }
            mIsWaitingQueryComplete = true;
            //if we're in the right state
            if (isQueryExecutionTimeOK) {

                // add for auto reject
                if (FeatureOption.MTK_VT3G324M_SUPPORT) {

                    boolean isSipIncoming = false;
                    if (SipManager.isVoipSupported(mApplication.getApplicationContext()))
                        isSipIncoming = (mCM.getFirstActiveRingingCall()
                                .getPhone().getPhoneType() == PhoneConstants.PHONE_TYPE_SIP);

                    boolean shouldAutoReject = false;
                    /*if (!isSipIncoming) {
                        shouldAutoReject = ci.shouldSendToVoicemail;
                    } else {
                        shouldAutoReject = ci.shouldSendToVoicemailSip;
                    }*/
                    shouldAutoReject = ci.shouldSendToVoicemail;

                    if (shouldAutoReject) {
                        PhoneUtils.hangupRingingCall(mCM.getFirstActiveRingingCall());
                        ci.shouldSendToVoicemail = true;
                        return;
                    }
                } else if (ci.shouldSendToVoicemail) {
                    if (DBG) log("send to voicemail flag detected. hanging up.");
                    PhoneUtils.hangupRingingCall(mCM.getFirstActiveRingingCall());
                    ci.shouldSendToVoicemail = true;
                    return;
                }

                // set the ringtone uri to prepare for the ring.
                if (ci.contactRingtoneUri != null) {
                    if (DBG) log("custom ringtone found, setting up ringer.");
                    log("contactRingtoneUri = " + ci.contactRingtoneUri + " for " + ci.phoneNumber);
                    Ringer r = ((CallNotifier) cookie).mRinger;
                    r.setCustomRingtoneUri(ci.contactRingtoneUri);
                } else if (DualTalkUtils.isSupportDualTalk()) {
                    //For dual talk solution, the ringtone will be changed for the dual incoming call case.
                    Ringer r = ((CallNotifier) cookie).mRinger;
                    ci.contactRingtoneUri = r.getCustomRingToneUri();
                    log("set call's uri = " + r.getCustomRingToneUri() + " for " + ci);
                }
                
                if (DualTalkUtils.isSupportDualTalk() && mDualTalk.hasMultipleRingingCall()) {
                    //For this case, we need to switch the ringtone to later incoming call
                    Ringer r = ((CallNotifier) cookie).mRinger;
                    Call foregroundRingCall = mDualTalk.getFirstActiveRingingCall();
                    Call backgroundRingCall = mDualTalk.getSecondActiveRingCall();
                    CallerInfo foregroundInfo = getCallerInfoFromConnection(foregroundRingCall.getLatestConnection());
                    CallerInfo backgroundInfo = getCallerInfoFromConnection(backgroundRingCall.getLatestConnection());
                    if (DBG) {
                        log("foregorund calller info = " + foregroundInfo);
                        log("background calller info = " + backgroundInfo);
                    }
                    Uri foregroundUri = null;
                    Uri backgroundUri = null;
                    //This is rare case, but it maybe occur, consider the two incoming call come in the same time and
                    //the first query is ongoing, the query for the new incoming call will not be issued, so the callerinfo
                    //is null
                    if (foregroundInfo != null) {
                        foregroundUri = foregroundInfo.contactRingtoneUri;
                    }
                    if (foregroundUri == null) {
                        if (VTCallUtils.isVideoCall(foregroundRingCall)) {
                            foregroundUri = Settings.System.DEFAULT_VIDEO_CALL_URI;
                        } else {
                            foregroundUri = Settings.System.DEFAULT_RINGTONE_URI;
                        }
                    }
                    if (backgroundInfo != null) {
                        backgroundUri = backgroundInfo.contactRingtoneUri;
                    }
                    if (backgroundUri == null) {
                        if (VTCallUtils.isVideoCall(backgroundRingCall)) {
                            backgroundUri = Settings.System.DEFAULT_VIDEO_CALL_URI;
                        } else {
                            backgroundUri = Settings.System.DEFAULT_RINGTONE_URI;
                        }
                    }

                    if (r.isRinging() && !foregroundUri.equals(backgroundUri)) {
                           r.stopRing();
                           r.setCustomRingtoneUri(foregroundUri);
                           //if (foregroundRingCall.getPhone().getPhoneType() == PhoneConstants.PHONE_TYPE_SIP) {
                               r.ring();
                           //}
                        }
                }
                
//                if (PhoneUtils.isDMLocked()) {
//                    Ringer r2 = ((CallNotifier) cookie).mRinger;
//                    AudioProfileImpl profile
//                        = (AudioProfileImpl) AudioProfileManagerImpl.getInstance(
//                     PhoneGlobals.getInstance().getApplicationContext()).getProfile("mtk_audioprofile_general");
//                    if (VTCallUtils.isVTRinging()) {
//                        r2.setCustomRingtoneUri(profile.getDefaultRingtone(AudioProfile.TYPE_RINGTONE));
//                    } else {
//                        r2.setCustomRingtoneUri(profile.getDefaultRingtone(AudioProfile.TYPE_VIDEO_CALL));
//                    }
//                }
                // ring, and other post-ring actions.
                onCustomRingQueryComplete();
            }
        }
    }

    
    /**
     * Called when asynchronous CallerInfo query is taking too long (more than
     * {@link #RINGTONE_QUERY_WAIT_TIME} msec), but we cannot wait any more.
     *
     * This looks up in-memory fallback cache and use it when available. If not, it just calls
     * {@link #onCustomRingQueryComplete()} with default ringtone ("Send to voicemail" flag will
     * be just ignored).
     *
     * @param number The phone number used for the async query. This method will take care of
     * formatting or normalization of the number.
     */
    private void onCustomRingtoneQueryTimeout(String number) {
        // First of all, this case itself should be rare enough, though we cannot avoid it in
        // some situations (e.g. IPC is slow due to system overload, database is in sync, etc.)
        Log.w(LOG_TAG, "CallerInfo query took too long; look up local fallback cache.");

        // This method is intentionally verbose for now to detect possible bad side-effect for it.
        // TODO: Remove the verbose log when it looks stable and reliable enough.

        final CallerInfoCache.CacheEntry entry =
                mApplication.callerInfoCache.getCacheEntry(number);
        if (entry != null) {
            if (entry.sendToVoicemail) {
                log("send to voicemail flag detected (in fallback cache). hanging up.");
                PhoneUtils.hangupRingingCall(mCM.getFirstActiveRingingCall());
                return;
            }

            if (entry.customRingtone != null) {
                log("custom ringtone found (in fallback cache), setting up ringer: "
                        + entry.customRingtone);
                this.mRinger.setCustomRingtoneUri(Uri.parse(entry.customRingtone));
            }
        } else {
            // In this case we call onCustomRingQueryComplete(), just
            // like if the query had completed normally.  (But we're
            // going to get the default ringtone, since we never got
            // the chance to call Ringer.setCustomRingtoneUri()).
            log("Failed to find fallback cache. Use default ringer tone.");
        }

        onCustomRingQueryComplete();
    }


    public void onTimeToReminder() {
         int toneToPlay = InCallTonePlayer.TONE_CALL_REMINDER;
         if (VDBG) {
             log("- onTimeToReminder ...");
         }
         new InCallTonePlayer(toneToPlay).start();
     }

    private void onDisconnect(AsyncResult r, final int simId) {
        if (VDBG) log("onDisconnect()...  CallManager state: " + mCM.getState() + ", sim id " + simId);

        boolean isSipCall = false;
        PhoneConstants.State state = mCM.getState() ;

        if (state == PhoneConstants.State.IDLE) {
            PhoneUtils.setAudioControlState(PhoneUtils.AUDIO_IDLE);
        } else if (state == PhoneConstants.State.RINGING) {
            log("state == PhoneConstants.State.RINGING");
            removeMessages(FAKE_SIP_PHONE_INCOMING_RING);
            sendEmptyMessageDelayed(FAKE_SIP_PHONE_INCOMING_RING, FAKE_SIP_PHONE_INCOMING_RING_DELAY);
        }

        ok2Ring = true;
        
        mVoicePrivacyState = false;
        Connection c = (Connection) r.result;
        if (c != null) {
            log("onDisconnect: cause = " + c.getDisconnectCause()
                  + ", incoming = " + c.isIncoming()
                  + ", date = " + c.getCreateTime() + ", number = " + c.getAddress());
        } else {
            Log.w(LOG_TAG, "onDisconnect: null connection");
        }

        int autoretrySetting = 0;
        if ((c != null) && (c.getCall().getPhone().getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA)) {
            autoretrySetting = android.provider.Settings.Global.getInt(mApplication.
                    getContentResolver(),android.provider.Settings.Global.CALL_AUTO_RETRY, 0);
        }

        // Stop any signalInfo tone being played when a call gets ended
        stopSignalInfoTone();

        if ((c != null) && (c.getCall().getPhone().getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA)) {
            // Resetting the CdmaPhoneCallState members
            mApplication.cdmaPhoneCallState.resetCdmaPhoneCallState();

            // Remove Call waiting timers
            removeMessages(CALLWAITING_CALLERINFO_DISPLAY_DONE);
            removeMessages(CALLWAITING_ADDCALL_DISABLE_TIMEOUT);
        }

        /*This is ugly and boring for ALPS00111659 (dial out an long invalid number)*/
        if (mApplication.inCallUiState.delayFinished && (state == PhoneConstants.State.IDLE)) {
            log("Meet the case to cache the disconnect call information!!");
            mApplication.inCallUiState.delayFinished = false;
            mApplication.inCallUiState.latestDisconnectCall = new InCallUiState.FakeCall();
            mApplication.inCallUiState.latestDisconnectCall.cause = c.getDisconnectCause();
            if (GeminiUtils.isGeminiSupport()) {
                mApplication.inCallUiState.latestDisconnectCall.slotId = simId;
            } else {
                mApplication.inCallUiState.latestDisconnectCall.slotId = 0;
            }
            mApplication.inCallUiState.latestDisconnectCall.conn = c;
            mApplication.inCallUiState.latestDisconnectCall.number = c.getAddress();
            mApplication.inCallUiState.latestDisconnectCall.phoneType = c.getCall().getPhone().getPhoneType();
        } else {
            log("Don't meet the case clear disconnect call information!!");
            mApplication.inCallUiState.latestDisconnectCall = null;
        }
        
        //Get the phone type for sip
        if ((c != null) && (c.getCall().getPhone().getPhoneType() == PhoneConstants.PHONE_TYPE_SIP)) {
            isSipCall = true;
        }
        
        if (FeatureOption.MTK_VT3G324M_SUPPORT) {
            if (c.isVideo()) {

                if (VTInCallScreenFlags.getInstance().mVTShouldCloseVTManager 
                        // Remember when single sim card, slot (sim) id is -1 !!!!!!!!
                        && (-1 == simId || simId == VTManager.getInstance().getSimId())) {
                    if (!VTCallUtils.isVTActive()) {
                        // When record video, we need firstly stop recording then call close VT manager
                        // So add below code, but the structure may not good
                        // consider adjust in the future
                        if (FeatureOption.MTK_PHONE_VOICE_RECORDING) {
                            if (PhoneRecorderHandler.getInstance().isVTRecording()) {
                                PhoneRecorderHandler.getInstance().stopVideoRecord();
                            }
                        }
                        if (VTManager.State.CLOSE != VTManager.getInstance().getState()) {
                            closeVTManager();
                        }
                    } else {
                        if (DBG)
                            log("onDisconnect: VT is active now, so do nothing for VTManager ...");
                    }
                } else {
                    if (DBG)
                        log("onDisconnect: set VTInCallScreenFlags.getInstance().mVTShouldCloseVTManager = true");
                    VTInCallScreenFlags.getInstance().mVTShouldCloseVTManager = true;
                }
            }
        
        }

        // Stop the ringer if it was ringing (for an incoming call that
        // either disconnected by itself, or was rejected by the user.)
        //
        // TODO: We technically *shouldn't* stop the ringer if the
        // foreground or background call disconnects while an incoming call
        // is still ringing, but that's a really rare corner case.
        // It's safest to just unconditionally stop the ringer here.

        // CDMA: For Call collision cases i.e. when the user makes an out going call
        // and at the same time receives an Incoming Call, the Incoming Call is given
        // higher preference. At this time framework sends a disconnect for the Out going
        // call connection hence we should *not* be stopping the ringer being played for
        // the Incoming Call
        Call ringingCall = mCM.getFirstActiveRingingCall();
        if (ringingCall.getPhone().getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA) {
            if (PhoneUtils.isRealIncomingCall(ringingCall.getState())) {
                // Also we need to take off the "In Call" icon from the Notification
                // area as the Out going Call never got connected
                if (DBG) log("cancelCallInProgressNotifications()... (onDisconnect)");
                mApplication.notificationMgr.cancelCallInProgressNotifications();
            } else {
                if (DBG) log("stopRing()... (onDisconnect)");
                mRinger.stopRing();
            }
        } else { // GSM
            if (DBG) log("stopRing()... (onDisconnect)");
            mRinger.stopRing();
            if (DualTalkUtils.isSupportDualTalk() && state == PhoneConstants.State.RINGING) {
                //This only occurs on dualtalk project
                switchRingToneByNeeded(mCM.getFirstActiveRingingCall());
            }
        }

        // stop call waiting tone if needed when disconnecting
        if (mCallWaitingTonePlayer != null) {
            mCallWaitingTonePlayer.stopTone();
            mCallWaitingTonePlayer = null;
        }

        // If this is the end of an OTASP call, pass it on to the PhoneGlobals.
        if (c != null && TelephonyCapabilities.supportsOtasp(c.getCall().getPhone())) {
            final String number = c.getAddress();
            if (c.getCall().getPhone().isOtaSpNumber(number)) {
                if (DBG) log("onDisconnect: this was an OTASP call!");
                mApplication.handleOtaspDisconnect();
            }
        }

        // Check for the various tones we might need to play (thru the
        // earpiece) after a call disconnects.
        int toneToPlay = InCallTonePlayer.TONE_NONE;

        // The "Busy" or "Congestion" tone is the highest priority:
        if (c != null) {
            Connection.DisconnectCause cause = c.getDisconnectCause();
            if (cause == Connection.DisconnectCause.BUSY) {
                if (DBG) log("- need to play BUSY tone!");
                toneToPlay = InCallTonePlayer.TONE_BUSY;
            } else if (cause == Connection.DisconnectCause.CONGESTION
                       || cause == Connection.DisconnectCause.BEARER_NOT_AVAIL
                       || cause == Connection.DisconnectCause.NO_CIRCUIT_AVAIL) {
                if (DBG) log("- need to play CONGESTION tone!");
                toneToPlay = InCallTonePlayer.TONE_CONGESTION;
            } else if (((cause == Connection.DisconnectCause.NORMAL)
                    || (cause == Connection.DisconnectCause.LOCAL))
                    && (mApplication.isOtaCallInActiveState())) {
                if (DBG) log("- need to play OTA_CALL_END tone!");
                toneToPlay = InCallTonePlayer.TONE_OTA_CALL_END;
            } else if (cause == Connection.DisconnectCause.CDMA_REORDER) {
                if (DBG) log("- need to play CDMA_REORDER tone!");
                toneToPlay = InCallTonePlayer.TONE_REORDER;
            } else if (cause == Connection.DisconnectCause.CDMA_INTERCEPT) {
                if (DBG) log("- need to play CDMA_INTERCEPT tone!");
                toneToPlay = InCallTonePlayer.TONE_INTERCEPT;
            } else if (cause == Connection.DisconnectCause.CDMA_DROP) {
                if (DBG) log("- need to play CDMA_DROP tone!");
                toneToPlay = InCallTonePlayer.TONE_CDMA_DROP;
            } else if (cause == Connection.DisconnectCause.OUT_OF_SERVICE) {
                if (DBG) log("- need to play OUT OF SERVICE tone!");
                toneToPlay = InCallTonePlayer.TONE_OUT_OF_SERVICE;
            } else if (cause == Connection.DisconnectCause.UNOBTAINABLE_NUMBER
                    || cause == Connection.DisconnectCause.INVALID_NUMBER_FORMAT
                    || cause == Connection.DisconnectCause.INVALID_NUMBER) {
                //ALPS00275195
                if (mApplication.getInCallScreenInstance() != null 
                        && mApplication.getInCallScreenInstance().isFinishing()) {
                    //do nothing
                    log("display incallscreen again!");
                    initFakeCall(c, simId);
                    mApplication.displayCallScreen(true);
                }
                
                if (DBG) log("- need to play TONE_UNOBTAINABLE_NUMBER tone!");
                toneToPlay = InCallTonePlayer.TONE_UNOBTAINABLE_NUMBER;
            } else if (cause == Connection.DisconnectCause.ERROR_UNSPECIFIED) {
                if (DBG) log("- DisconnectCause is ERROR_UNSPECIFIED: play TONE_CALL_ENDED!");
                toneToPlay = InCallTonePlayer.TONE_CALL_ENDED;
            } else if (cause == Connection.DisconnectCause.FDN_BLOCKED) {
                /**
                 * add by mediatek .inc
                 * description : while call is blocked by FDN
                 * set a pending status code for CR : ALPS000
                 */
                if (DBG) {
                    log("cause is FDN_BLOCKED");
                }
                mApplication.inCallUiState.setPendingCallStatusCode(CallStatusCode.FDN_BLOCKED);
                
                if (mApplication.getInCallScreenInstance() != null 
                        && mApplication.getInCallScreenInstance().isFinishing()) {
                    //do nothing
                    log("display incallscreen again!");
                    initFakeCall(c, simId);
                    mApplication.displayCallScreen(true);
                } else if (mApplication.getInCallScreenInstance() != null) {
                    if (DBG) {
                        log("incall screen not null, do nothing.!");
                    }
                } else {
                    mApplication.displayCallScreen(true);
                }
            }
        }

        // If we don't need to play BUSY or CONGESTION, then play the
        // "call ended" tone if this was a "regular disconnect" (i.e. a
        // normal call where one end or the other hung up) *and* this
        // disconnect event caused the phone to become idle.  (In other
        // words, we *don't* play the sound if one call hangs up but
        // there's still an active call on the other line.)
        // TODO: We may eventually want to disable this via a preference.
        if ((toneToPlay == InCallTonePlayer.TONE_NONE)
            && (mCM.getState() == PhoneConstants.State.IDLE)
            && (c != null)) {
            Connection.DisconnectCause cause = c.getDisconnectCause();
            if ((cause == Connection.DisconnectCause.NORMAL)// remote hangup
                || (cause == Connection.DisconnectCause.LOCAL)// local hangup
                /// M: for ALPS00531421 @{
                // normal_unspecified is not normal when hangup a call,
                // play a tone for NORMAL_UNSPECIFIED cases.
                || (cause == Connection.DisconnectCause.NORMAL_UNSPECIFIED)) {
                /// @}
                if (VDBG) log("- need to play CALL_ENDED tone!");
                toneToPlay = InCallTonePlayer.TONE_CALL_ENDED;
                mIsCdmaRedialCall = false;
            }
        }

        Call fg , bg, bFg, bBg;
        fg = mCM.getFgPhone().getForegroundCall();
        bg = mCM.getFgPhone().getBackgroundCall();
        bFg = mCM.getBgPhone().getForegroundCall();
        bBg = mCM.getBgPhone().getBackgroundCall();
        if ((state == PhoneConstants.State.RINGING) 
            && fg.isIdle()
            && bg.isIdle() && bFg.isIdle() && bBg.isIdle()) {
            // remove below line for ALPS00567675.
            // mApplication.notificationMgr.cancelCallInProgressNotifications();
            PhoneUtils.setAudioControlState(PhoneUtils.AUDIO_RINGING);
            // we didn't know why add this to here, for dualtalk case, this will
            // cause to play
            // wrong ringtone, so mark this for dualtalk.
            // startIncomingCallQuery(c);
        }
        
        // All phone calls are disconnected.
        if (mCM.getState() == PhoneConstants.State.IDLE) {
            // Don't reset the audio mode or bluetooth/speakerphone state
            // if we still need to let the user hear a tone through the earpiece.
            if (toneToPlay == InCallTonePlayer.TONE_NONE) {
                resetAudioStateAfterDisconnect();
            }

            mApplication.notificationMgr.cancelCallInProgressNotifications();

            // If the InCallScreen is *not* in the foreground, forcibly
            // dismiss it to make sure it won't still be in the activity
            // history.  (But if it *is* in the foreground, don't mess
            // with it; it needs to be visible, displaying the "Call
            // ended" state.)
            /*
             * if (!mApplication.isShowingCallScreen() &&
             * CallStatusCode.FDN_BLOCKED !=
             * mApplication.inCallUiState.getPendingCallStatusCode()) { if
             * (VDBG) log("onDisconnect: force InCallScreen to finish()");
             * mApplication.dismissCallScreen(); }
             */
        }

        if (c != null) {
            final String number = c.getAddress();
            final long date = c.getCreateTime();
            /// M: @{ CR: ALPS00375341
            /// Original code :
            /// final long duration = CallTime.getCallDuration(c.getCall());
            ///
            /// If it is a conference call, one call maybe have more than one connection, when each
            /// connection disconnect the onDisconnect() method will be called, when the second time onDisconnect()
            /// is called, the connection.getCall().getConnections() will return 0 size list, which will result in
            /// the call duration be 0.
            final long duration = c.getDurationMillis();
            if (VDBG) log("onDisconnect: number = " + number + ", duration = " + duration);
            /// @}

        /*
         * New Feature by Mediatek Begin.
         *   CR ID: ALPS00114062
         */
        if (!c.getCall().isMultiparty() && (0 != duration / 1000)) {
            Toast.makeText(PhoneGlobals.getInstance().getApplicationContext(),
                formatDuration((int)(duration / 1000)),
                Toast.LENGTH_SHORT).show();
        }
        /*
         * New Feature by Mediatek End.
         */
            final Connection.DisconnectCause cause = c.getDisconnectCause();
            final Phone phone = c.getCall().getPhone();
            int vtCall = 0;  // add for VT call
            if (FeatureOption.MTK_VT3G324M_SUPPORT) {
                final boolean vtIdle = c.isVideo();
                if (VDBG) log("onDisconnect: VT call, vtIdle= " + vtIdle);
                if (vtIdle) {
                    vtCall = 1;
                }
            }
            final boolean isEmergencyNumber =
                    PhoneNumberUtils.isLocalEmergencyNumber(number, mApplication);

            log("onDisconnect isShouldSendtoVoicemail flag:"
                    + PhoneUtils.getShouldSendToVoiceMailFlag(c));

            // Set the "type" to be displayed in the call log (see constants in CallLog.Calls)
            final int callLogType;
            if (c.isIncoming()) {
                callLogType = (PhoneUtils.getShouldSendToVoiceMailFlag(c)) ? Calls.AUTOREJECTED_TYPE : 
                    ((cause == Connection.DisconnectCause.INCOMING_MISSED ?
                               Calls.MISSED_TYPE : Calls.INCOMING_TYPE));
            } else {
                callLogType = Calls.OUTGOING_TYPE;
            }
            log("onDisconnect callLogType:" + callLogType);

            if (VDBG) log("- callLogType: " + callLogType + ", UserData: " + c.getUserData());


            {
                final CallerInfo ci = getCallerInfoFromConnection(c);  // May be null.
                final String logNumber = getLogNumber(c, ci);

                if (DBG) log("- onDisconnect(): logNumber set to: " + /*logNumber*/ "xxxxxxx");

                // TODO: In getLogNumber we use the presentation from
                // the connection for the CNAP. Should we use the one
                // below instead? (comes from caller info)

                // For international calls, 011 needs to be logged as +
                final int presentation = getPresentation(c, ci);

                if (phone.getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA) {
                    if ((isEmergencyNumber)
                            && (mCurrentEmergencyToneState != EMERGENCY_TONE_OFF)) {
                        if (mEmergencyTonePlayerVibrator != null) {
                            mEmergencyTonePlayerVibrator.stop();
                        }
                    }
                }

                // On some devices, to avoid accidental redialing of
                // emergency numbers, we *never* log emergency calls to
                // the Call Log.  (This behavior is set on a per-product
                // basis, based on carrier requirements.)
                final boolean okToLogEmergencyNumber =
                        mApplication.getResources().getBoolean(
                                R.bool.allow_emergency_numbers_in_call_log);

                // Don't call isOtaSpNumber() on phones that don't support OTASP.
                final boolean isOtaspNumber = TelephonyCapabilities.supportsOtasp(phone)
                        && phone.isOtaSpNumber(number);

                // Don't log emergency numbers if the device doesn't allow it,
                // and never log OTASP calls.
                final boolean okToLogThisCall =
                        (!isEmergencyNumber || okToLogEmergencyNumber)
                        && !isOtaspNumber;

                if (okToLogThisCall) {
                    CallLogAsync.AddCallArgs args;
                    int simIdEx = CALL_TYPE_NONE;

                    if (!GeminiUtils.isGeminiSupport() || isSipCall) { //Single Card
                        if (isSipCall) {
                            simIdEx = CALL_TYPE_SIP;
                        } else {
                            simIdEx = CALL_TYPE_NONE;
                            if (PhoneGlobals.getInstance().phoneMgr.hasIccCard()) {
                                SIMInfo info = SIMInfoWrapper.getDefault().getSimInfoBySlot(0);
                                if (info != null) {
                                    simIdEx = (int)info.mSimId;
                                } else {
                                    //Give an default simId, in most case, this is invalid
                                    simIdEx = 1;
                                }
                            }
                        }
                        if (FeatureOption.MTK_VT3G324M_SUPPORT) {
                            args = new CallLogAsync.AddCallArgs(
                                mApplication, ci, logNumber, presentation,
                                callLogType, date, duration, simIdEx, vtCall);
                        } else {
                            args = new CallLogAsync.AddCallArgs(
                                mApplication, ci, logNumber, presentation,
                                callLogType, date, duration, simIdEx);
                        }
                    } else { //dual SIM
                        // Geminni Enhancement: change call log to sim id;
                        SIMInfo si;
                        if (PhoneGlobals.getInstance().phoneMgr.hasIccCardGemini(simId)) {
                            si = SIMInfo.getSIMInfoBySlot(PhoneGlobals.getInstance(), simId);
                            if (si != null) {
                                simIdEx = (int)si.mSimId;
                            }
                        }

                        if (FeatureOption.MTK_VT3G324M_SUPPORT) {
                            args = new CallLogAsync.AddCallArgs(
                                   mApplication, ci, logNumber, presentation,
                                   callLogType, date, duration, simIdEx, vtCall);
                        } else {
                            args = new CallLogAsync.AddCallArgs(
                                   mApplication, ci, logNumber, presentation,
                                   callLogType, date, duration, simIdEx);
                        }
                    }

                    try {
                        mCallLog.addCall(args);
                    } catch (SQLiteDiskIOException e) {
                        // TODO Auto-generated catch block
                        Log.e(LOG_TAG, "Error!! - onDisconnect() Disk Full!");
                        e.printStackTrace();
                    }

                    // add call number info to call history database for international dialing feature
                    // !!!! need to check okToLogThisCall is suitable for international dialing feature
                    if (!isEmergencyNumber && !c.isIncoming() && !c.isVideo() && !isSipCall
                            && duration >= CALL_DURATION_THRESHOLD_FOR_CALL_HISTORY) {
                        String countryISO = CallOptionUtils.getCurrentCountryISO(PhoneGlobals.getInstance());
                        try {
                            new CallHistoryAsync().addCall(new CallHistoryAsync.AddCallArgs(PhoneGlobals.getInstance(),
                                    logNumber, countryISO, date, simId, GeminiUtils.isGeminiSupport()));
                        } catch (SQLiteDiskIOException e) {
                            // TODO Auto-generated catch block
                            Log.e(LOG_TAG, "Error!! - onDisconnect() Disk Full!");
                            e.printStackTrace();
                        }
                    }
                }
            }

            if (callLogType == Calls.MISSED_TYPE) {
                // Show the "Missed call" notification.
                // (Note we *don't* do this if this was an incoming call that
                // the user deliberately rejected.)
                showMissedCallNotification(c, date);
            }

            // Possibly play a "post-disconnect tone" thru the earpiece.
            // We do this here, rather than from the InCallScreen
            // activity, since we need to do this even if you're not in
            // the Phone UI at the moment the connection ends.
            if (toneToPlay != InCallTonePlayer.TONE_NONE) {
                if (VDBG) log("- starting post-disconnect tone (" + toneToPlay + ")...");
                mToneThread = new InCallTonePlayer(toneToPlay);
                mToneThread.start();

                // TODO: alternatively, we could start an InCallTonePlayer
                // here with an "unlimited" tone length,
                // and manually stop it later when this connection truly goes
                // away.  (The real connection over the network was closed as soon
                // as we got the BUSY message.  But our telephony layer keeps the
                // connection open for a few extra seconds so we can show the
                // "busy" indication to the user.  We could stop the busy tone
                // when *that* connection's "disconnect" event comes in.)
            }

            if (((mPreviousCdmaCallState == Call.State.DIALING)
                    || (mPreviousCdmaCallState == Call.State.ALERTING))
                    && (!isEmergencyNumber)
                    && (cause != Connection.DisconnectCause.INCOMING_MISSED)
                    && (cause != Connection.DisconnectCause.NORMAL)
                    && (cause != Connection.DisconnectCause.LOCAL)
                    && (cause != Connection.DisconnectCause.INCOMING_REJECTED)) {
                if (!mIsCdmaRedialCall) {
                    if (autoretrySetting == InCallScreen.AUTO_RETRY_ON) {
                        // TODO: (Moto): The contact reference data may need to be stored and use
                        // here when redialing a call. For now, pass in NULL as the URI parameter.
                        PhoneUtils.placeCall(mApplication, phone, number, null, false, null);
                        mIsCdmaRedialCall = true;
                    } else {
                        mIsCdmaRedialCall = false;
                    }
                } else {
                    mIsCdmaRedialCall = false;
                }
            }
        }
    }

    /**
     * Resets the audio mode and speaker state when a call ends.
     */
    private void resetAudioStateAfterDisconnect() {
        if (VDBG) log("resetAudioStateAfterDisconnect()...");

        if (mBluetoothHeadset != null) {
            mBluetoothHeadset.disconnectAudio();
        }

        // call turnOnSpeaker() with state=false and store=true even if speaker
        // is already off to reset user requested speaker state.
        PhoneUtils.turnOnSpeaker(mApplication, false, true);

        PhoneUtils.setAudioMode(mCM);
    }

    private void onMwiChanged(boolean visible, int simId) {
        if (VDBG) log("onMwiChanged(): " + visible + "simid:" + simId);

        // "Voicemail" is meaningless on non-voice-capable devices,
        // so ignore MWI events.
        if (!PhoneGlobals.sVoiceCapable) {
            // ...but still log a warning, since we shouldn't have gotten this
            // event in the first place!
            // (PhoneStateListener.LISTEN_MESSAGE_WAITING_INDICATOR events
            // *should* be blocked at the telephony layer on non-voice-capable
            // capable devices.)
            Log.w(LOG_TAG, "Got onMwiChanged() on non-voice-capable device! Ignoring...");
            return;
        }

        mApplication.notificationMgr.updateMwi(visible, simId);
    }

    /**
     * Posts a delayed PHONE_MWI_CHANGED event, to schedule a "retry" for a
     * failed NotificationMgr.updateMwi() call.
     */
    /* package */void sendMwiChangedDelayed(long delayMillis, int slotId) {
        Message message = Message.obtain();

        final int index = GeminiUtils.getIndexInArray(slotId, GeminiUtils.getSlots());
        if (VDBG) log("sendMwiChangedDelayed, error slot(): slotId=" + slotId + " index:" + index);
        if (index != -1) {
            message.what = PHONE_MWI_CHANGED_GEMINI[index];
            sendMessageDelayed(message, delayMillis);
        } else {
            if (VDBG) log("sendMwiChangedDelayed, error slot");
        }
    }

    private void onCfiChanged(boolean visible, int simId) {
        if (VDBG) log("onCfiChanged(): " + visible + "simId:" + simId);
        mApplication.notificationMgr.updateCfi(visible, simId);
    }

    /**
     * Indicates whether or not this ringer is ringing.
     */
    boolean isRinging() {
        return mRinger.isRinging();
    }

    /**
     * Stops the current ring, and tells the notifier that future
     * ring requests should be ignored.
     */
    void silenceRinger() {
        mSilentRingerRequested = true;
        if (DBG) log("stopRing()... (silenceRinger)");
        mRinger.stopRing();
    }

    /**
     * Restarts the ringer after having previously silenced it.
     *
     * (This is a no-op if the ringer is actually still ringing, or if the
     * incoming ringing call no longer exists.)
     */
    /* package */ void restartRinger() {
        if (DBG) log("restartRinger()...");
        if (isRinging()) return;  // Already ringing; no need to restart.

        final Call ringingCall = mCM.getFirstActiveRingingCall();
        // Don't check ringingCall.isRinging() here, since that'll be true
        // for the WAITING state also.  We only allow the ringer for
        // regular INCOMING calls.
        if (DBG) log("- ringingCall state: " + ringingCall.getState());
        if (ringingCall.getState() == Call.State.INCOMING) {
            mRinger.ring();
        }
    }

    /**
     * Helper class to play tones through the earpiece (or speaker / BT)
     * during a call, using the ToneGenerator.
     *
     * To use, just instantiate a new InCallTonePlayer
     * (passing in the TONE_* constant for the tone you want)
     * and start() it.
     *
     * When we're done playing the tone, if the phone is idle at that
     * point, we'll reset the audio routing and speaker state.
     * (That means that for tones that get played *after* a call
     * disconnects, like "busy" or "congestion" or "call ended", you
     * should NOT call resetAudioStateAfterDisconnect() yourself.
     * Instead, just start the InCallTonePlayer, which will automatically
     * defer the resetAudioStateAfterDisconnect() call until the tone
     * finishes playing.)
     */
    private class InCallTonePlayer extends Thread {
        private int mToneId;
        private int mState;
        // The possible tones we can play.
        public static final int TONE_NONE = 0;
        public static final int TONE_CALL_WAITING = 1;
        public static final int TONE_BUSY = 2;
        public static final int TONE_CONGESTION = 3;
        public static final int TONE_CALL_ENDED = 4;
        public static final int TONE_VOICE_PRIVACY = 5;
        public static final int TONE_REORDER = 6;
        public static final int TONE_INTERCEPT = 7;
        public static final int TONE_CDMA_DROP = 8;
        public static final int TONE_OUT_OF_SERVICE = 9;
        public static final int TONE_REDIAL = 10;
        public static final int TONE_OTA_CALL_END = 11;
        public static final int TONE_RING_BACK = 12;
        public static final int TONE_UNOBTAINABLE_NUMBER = 13;
        public static final int TONE_CALL_REMINDER = 15;

        // The tone volume relative to other sounds in the stream

        private static final int TONE_RELATIVE_VOLUME_HIPRIEST = 100;
        private static final int TONE_RELATIVE_VOLUME_HIPRI = 80;
        private static final int TONE_RELATIVE_VOLUME_LOPRI = 50;

        // Buffer time (in msec) to add on to tone timeout value.
        // Needed mainly when the timeout value for a tone is the
        // exact duration of the tone itself.
        static final int TONE_TIMEOUT_BUFFER = 20;

        // The tone state
        static final int TONE_OFF = 0;
        static final int TONE_ON = 1;
        static final int TONE_STOPPED = 2;

        InCallTonePlayer(int toneId) {
            super();
            mToneId = toneId;
            mState = TONE_OFF;
        }

        @Override
        public void run() {
            log("InCallTonePlayer.run(toneId = " + mToneId + ")...");

            int toneType = 0;  // passed to ToneGenerator.startTone()
            int toneVolume;  // passed to the ToneGenerator constructor
            int toneLengthMillis;
            int phoneType = mCM.getFgPhone().getPhoneType();

            switch (mToneId) {
                case TONE_CALL_WAITING:
                    toneType = ToneGenerator.TONE_SUP_CALL_WAITING;
                    toneVolume = TONE_RELATIVE_VOLUME_HIPRI;
                    // Call waiting tone is stopped by stopTone() method
                    toneLengthMillis = Integer.MAX_VALUE - TONE_TIMEOUT_BUFFER;
                    break;
                case TONE_BUSY:
                    //display a "Line busy" message while play the busy tone
                    if (FeatureOption.MTK_BRAZIL_CUSTOMIZATION_VIVO) {
                        CallNotifier me = PhoneGlobals.getInstance().notifier;
                        me.sendMessage(me.obtainMessage(CallNotifier.DISPLAY_BUSY_MESSAGE));
                    }
                    if (phoneType == PhoneConstants.PHONE_TYPE_CDMA) {
                        toneType = ToneGenerator.TONE_CDMA_NETWORK_BUSY_ONE_SHOT;
                        toneVolume = TONE_RELATIVE_VOLUME_LOPRI;
                        toneLengthMillis = 1000;
                    } else if ((phoneType == PhoneConstants.PHONE_TYPE_GSM)
                            || (phoneType == PhoneConstants.PHONE_TYPE_SIP)) {
                        toneType = ToneGenerator.TONE_SUP_BUSY;
                        toneVolume = TONE_RELATIVE_VOLUME_HIPRI;
                        toneLengthMillis = 4000;
                    } else {
                        throw new IllegalStateException("Unexpected phone type: " + phoneType);
                    }
                    break;
                case TONE_CONGESTION:
                    toneType = ToneGenerator.TONE_SUP_CONGESTION;
                    toneVolume = TONE_RELATIVE_VOLUME_HIPRI;
                    toneLengthMillis = 4000;
                    break;

                case TONE_CALL_ENDED:
                    toneType = ToneGenerator.TONE_PROP_PROMPT;
                    toneVolume = TONE_RELATIVE_VOLUME_HIPRI;
                    //According to audio's request, we change this time from 200 to 512
                    //200ms is too short and maybe cause the tone play time less than expected
                    //toneLengthMillis = 200;
                    toneLengthMillis = 512;
                    break;
                 case TONE_OTA_CALL_END:
                    if (mApplication.cdmaOtaConfigData.otaPlaySuccessFailureTone ==
                            OtaUtils.OTA_PLAY_SUCCESS_FAILURE_TONE_ON) {
                        toneType = ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD;
                        toneVolume = TONE_RELATIVE_VOLUME_HIPRI;
                        toneLengthMillis = 750;
                    } else {
                        toneType = ToneGenerator.TONE_PROP_PROMPT;
                        toneVolume = TONE_RELATIVE_VOLUME_HIPRI;
                        toneLengthMillis = 200;
                    }
                    break;
                case TONE_VOICE_PRIVACY:
                    toneType = ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE;
                    toneVolume = TONE_RELATIVE_VOLUME_HIPRI;
                    toneLengthMillis = 5000;
                    break;
                case TONE_REORDER:
                    toneType = ToneGenerator.TONE_CDMA_REORDER;
                    toneVolume = TONE_RELATIVE_VOLUME_HIPRI;
                    toneLengthMillis = 4000;
                    break;
                case TONE_INTERCEPT:
                    toneType = ToneGenerator.TONE_CDMA_ABBR_INTERCEPT;
                    toneVolume = TONE_RELATIVE_VOLUME_LOPRI;
                    toneLengthMillis = 500;
                    break;
                case TONE_CDMA_DROP:
                case TONE_OUT_OF_SERVICE:
                    toneType = ToneGenerator.TONE_CDMA_CALLDROP_LITE;
                    toneVolume = TONE_RELATIVE_VOLUME_LOPRI;
                    toneLengthMillis = 375;
                    break;
                case TONE_REDIAL:
                    toneType = ToneGenerator.TONE_CDMA_ALERT_AUTOREDIAL_LITE;
                    toneVolume = TONE_RELATIVE_VOLUME_LOPRI;
                    toneLengthMillis = 5000;
                    break;
                case TONE_RING_BACK:
                    toneType = ToneGenerator.TONE_SUP_RINGTONE;
                    /** M: Change feature: make video call ring back tone clear to hear
                     * below modify from TONE_RELATIVE_VOLUME_HIPRI to 450 due to
                     * video call ring back tone should be clear to hear with speaker opening
                     * voice call's volume does not be influenced by this value */
                    toneVolume = 450;
                    //toneVolume = TONE_RELATIVE_VOLUME_HIPRI;
                    /** M: Change feature @{ */
                    // Call ring back tone is stopped by stopTone() method
                    toneLengthMillis = Integer.MAX_VALUE - TONE_TIMEOUT_BUFFER;
                    break;
                case TONE_UNOBTAINABLE_NUMBER:
                    toneType = ToneGenerator.TONE_SUP_ERROR;
                    toneVolume = TONE_RELATIVE_VOLUME_HIPRI;
                    toneLengthMillis = 1000;
                    break;
                case TONE_CALL_REMINDER:
                    if (VDBG) log("InCallTonePlayer.TONE_CALL_NOTIFY ");
                    toneType = ToneGenerator.TONE_PROP_PROMPT;
                    toneVolume = TONE_RELATIVE_VOLUME_HIPRIEST;
                    toneLengthMillis = 500;
                break;
                default:
                    throw new IllegalArgumentException("Bad toneId: " + mToneId);
            }

            // If the mToneGenerator creation fails, just continue without it.  It is
            // a local audio signal, and is not as important.
            ToneGenerator toneGenerator;
            try {
                int stream;
                if (mBluetoothHeadset != null) {
                    stream = mBluetoothHeadset.isAudioOn() ? AudioManager.STREAM_BLUETOOTH_SCO :
                        AudioManager.STREAM_VOICE_CALL;
                } else {
                    stream = AudioManager.STREAM_VOICE_CALL;
                }
                if ((stream == AudioManager.STREAM_VOICE_CALL) && (mToneId == TONE_CALL_REMINDER))
                {
                    stream = AudioManager.STREAM_SYSTEM;
                }
                log("toneVolume is " + toneVolume);
                toneGenerator = new ToneGenerator(stream, toneVolume);
                // if (DBG) log("- created toneGenerator: " + toneGenerator);
            } catch (RuntimeException e) {
                Log.w(LOG_TAG,
                      "InCallTonePlayer: Exception caught while creating ToneGenerator: " + e);
                toneGenerator = null;
            }

            // Using the ToneGenerator (with the CALL_WAITING / BUSY /
            // CONGESTION tones at least), the ToneGenerator itself knows
            // the right pattern of tones to play; we do NOT need to
            // manually start/stop each individual tone, or manually
            // insert the correct delay between tones.  (We just start it
            // and let it run for however long we want the tone pattern to
            // continue.)
            //
            // TODO: When we stop the ToneGenerator in the middle of a
            // "tone pattern", it sounds bad if we cut if off while the
            // tone is actually playing.  Consider adding API to the
            // ToneGenerator to say "stop at the next silent part of the
            // pattern", or simply "play the pattern N times and then
            // stop."
            boolean needToStopTone = true;
            boolean okToPlayTone = false;

            if (toneGenerator != null) {
                int ringerMode = mAudioManager.getRingerMode();
                if (phoneType == PhoneConstants.PHONE_TYPE_CDMA) {
                    if (toneType == ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD) {
                        if ((ringerMode != AudioManager.RINGER_MODE_SILENT) &&
                                (ringerMode != AudioManager.RINGER_MODE_VIBRATE)) {
                            if (DBG) log("- InCallTonePlayer: start playing call tone=" + toneType);
                            okToPlayTone = true;
                            needToStopTone = false;
                        }
                    } else if ((toneType == ToneGenerator.TONE_CDMA_NETWORK_BUSY_ONE_SHOT) ||
                            (toneType == ToneGenerator.TONE_CDMA_REORDER) ||
                            (toneType == ToneGenerator.TONE_CDMA_ABBR_REORDER) ||
                            (toneType == ToneGenerator.TONE_CDMA_ABBR_INTERCEPT) ||
                            (toneType == ToneGenerator.TONE_CDMA_CALLDROP_LITE)) {
                        if (ringerMode != AudioManager.RINGER_MODE_SILENT) {
                            if (DBG) log("InCallTonePlayer:playing call fail tone:" + toneType);
                            okToPlayTone = true;
                            needToStopTone = false;
                        }
                    } else if ((toneType == ToneGenerator.TONE_CDMA_ALERT_AUTOREDIAL_LITE) ||
                               (toneType == ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE)) {
                        if ((ringerMode != AudioManager.RINGER_MODE_SILENT) &&
                                (ringerMode != AudioManager.RINGER_MODE_VIBRATE)) {
                            if (DBG) log("InCallTonePlayer:playing tone for toneType=" + toneType);
                            okToPlayTone = true;
                            needToStopTone = false;
                        }
                    } else { // For the rest of the tones, always OK to play.
                        okToPlayTone = true;
                    }
                } else {  // Not "CDMA"
                    okToPlayTone = true;
                }

                synchronized (this) {
                    if (okToPlayTone && mState != TONE_STOPPED) {
                        mState = TONE_ON;
            if (DBG) log("- InCallTonePlayer: startTone");
                        
                      //Tell AudioManager play the waiting tone || TONE_CALL_REMINDER
                        if ((mToneId == TONE_CALL_WAITING || mToneId == TONE_CALL_REMINDER)
                                && DualTalkUtils.isSupportDualTalk()) {
                            mAudioManager.setParameters("SetWarningTone=14");
                        }
                        
                        toneGenerator.startTone(toneType);
                        try {
                            wait(toneLengthMillis + TONE_TIMEOUT_BUFFER);
                        } catch  (InterruptedException e) {
                            Log.w(LOG_TAG,
                                  "InCallTonePlayer stopped: " + e);
                        }
                        if (needToStopTone) {
                            toneGenerator.stopTone();
                        }
                    }
                    // if (DBG) log("- InCallTonePlayer: done playing.");
                    toneGenerator.release();
                    mState = TONE_OFF;
                    
                    if (DBG) log("- InCallTonePlayer: stopTone");
                }
            }

            // Finally, do the same cleanup we otherwise would have done
            // in onDisconnect().
            //
            // (But watch out: do NOT do this if the phone is in use,
            // since some of our tones get played *during* a call (like
            // CALL_WAITING) and we definitely *don't*
            // want to reset the audio mode / speaker / bluetooth after
            // playing those!
            // This call is really here for use with tones that get played
            // *after* a call disconnects, like "busy" or "congestion" or
            // "call ended", where the phone has already become idle but
            // we need to defer the resetAudioStateAfterDisconnect() call
            // till the tone finishes playing.)
            if (mCM.getState() == PhoneConstants.State.IDLE) {
                resetAudioStateAfterDisconnect();
            }
            
            mToneThread = null;
        }

        public void stopTone() {
            synchronized (this) {
                if (mState == TONE_ON) {
                    notify();
                }
                mState = TONE_STOPPED;
            }
        }
    }

    /**
     * Displays a notification when the phone receives a DisplayInfo record.
     */
    private void onDisplayInfo(AsyncResult r) {
        // Extract the DisplayInfo String from the message
        CdmaDisplayInfoRec displayInfoRec = (CdmaDisplayInfoRec)(r.result);

        if (displayInfoRec != null) {
            String displayInfo = displayInfoRec.alpha;
            if (DBG) log("onDisplayInfo: displayInfo=" + displayInfo);
            CdmaDisplayInfo.displayInfoRecord(mApplication, displayInfo);

            // start a 2 second timer
            sendEmptyMessageDelayed(DISPLAYINFO_NOTIFICATION_DONE,
                    DISPLAYINFO_NOTIFICATION_TIME);
        }
    }

    /**
     * Helper class to play SignalInfo tones using the ToneGenerator.
     *
     * To use, just instantiate a new SignalInfoTonePlayer
     * (passing in the ToneID constant for the tone you want)
     * and start() it.
     */
    private class SignalInfoTonePlayer extends Thread {
        private int mToneId;

        SignalInfoTonePlayer(int toneId) {
            super();
            mToneId = toneId;
        }

        @Override
        public void run() {
            log("SignalInfoTonePlayer.run(toneId = " + mToneId + ")...");

            if (mSignalInfoToneGenerator != null) {
                //First stop any ongoing SignalInfo tone
                mSignalInfoToneGenerator.stopTone();

                //Start playing the new tone if its a valid tone
                mSignalInfoToneGenerator.startTone(mToneId);
            }
        }
    }

    /**
     * Plays a tone when the phone receives a SignalInfo record.
     */
    private void onSignalInfo(AsyncResult r) {
        // Signal Info are totally ignored on non-voice-capable devices.
        if (!PhoneGlobals.sVoiceCapable) {
            Log.w(LOG_TAG, "Got onSignalInfo() on non-voice-capable device! Ignoring...");
            return;
        }

        if (PhoneUtils.isRealIncomingCall(mCM.getFirstActiveRingingCall().getState())) {
            // Do not start any new SignalInfo tone when Call state is INCOMING
            // and stop any previous SignalInfo tone which is being played
            stopSignalInfoTone();
        } else {
            // Extract the SignalInfo String from the message
            CdmaSignalInfoRec signalInfoRec = (CdmaSignalInfoRec)(r.result);
            // Only proceed if a Signal info is present.
            if (signalInfoRec != null) {
                boolean isPresent = signalInfoRec.isPresent;
                if (DBG) log("onSignalInfo: isPresent=" + isPresent);
                if (isPresent) { // if tone is valid
                    int uSignalType = signalInfoRec.signalType;
                    int uAlertPitch = signalInfoRec.alertPitch;
                    int uSignal = signalInfoRec.signal;

                    if (DBG) log("onSignalInfo: uSignalType=" + uSignalType + ", uAlertPitch=" +
                            uAlertPitch + ", uSignal=" + uSignal);
                    //Map the Signal to a ToneGenerator ToneID only if Signal info is present
                    int toneID = SignalToneUtil.getAudioToneFromSignalInfo(uSignalType,
                            uAlertPitch, uSignal);

                    //Create the SignalInfo tone player and pass the ToneID
                    new SignalInfoTonePlayer(toneID).start();
                }
            }
        }
    }

    /**
     * Stops a SignalInfo tone in the following condition
     * 1 - On receiving a New Ringing Call
     * 2 - On disconnecting a call
     * 3 - On answering a Call Waiting Call
     */
    /* package */ void stopSignalInfoTone() {
        if (DBG) log("stopSignalInfoTone: Stopping SignalInfo tone player");
        new SignalInfoTonePlayer(ToneGenerator.TONE_CDMA_SIGNAL_OFF).start();
    }

    /**
     * Plays a Call waiting tone if it is present in the second incoming call.
     */
    private void onCdmaCallWaiting(AsyncResult r) {
        // Remove any previous Call waiting timers in the queue
        removeMessages(CALLWAITING_CALLERINFO_DISPLAY_DONE);
        removeMessages(CALLWAITING_ADDCALL_DISABLE_TIMEOUT);

        // Set the Phone Call State to SINGLE_ACTIVE as there is only one connection
        // else we would not have received Call waiting
        mApplication.cdmaPhoneCallState.setCurrentCallState(
                CdmaPhoneCallState.PhoneCallState.SINGLE_ACTIVE);

        // Display the incoming call to the user if the InCallScreen isn't
        // already in the foreground.
        if (!mApplication.isShowingCallScreen()) {
            if (DBG) log("- showing incoming call (CDMA call waiting)...");
            showIncomingCall();
        } else {
            mApplication.getInCallScreenInstance().requestUpdateScreen();
        }

        // Start timer for CW display
        mCallWaitingTimeOut = false;
        sendEmptyMessageDelayed(CALLWAITING_CALLERINFO_DISPLAY_DONE,
                CALLWAITING_CALLERINFO_DISPLAY_TIME);

        // Set the mAddCallMenuStateAfterCW state to false
        mApplication.cdmaPhoneCallState.setAddCallMenuStateAfterCallWaiting(false);

        // Start the timer for disabling "Add Call" menu option
        sendEmptyMessageDelayed(CALLWAITING_ADDCALL_DISABLE_TIMEOUT,
                CALLWAITING_ADDCALL_DISABLE_TIME);

        // Extract the Call waiting information
        CdmaCallWaitingNotification infoCW = (CdmaCallWaitingNotification) r.result;
        int isPresent = infoCW.isPresent;
        if (DBG) log("onCdmaCallWaiting: isPresent=" + isPresent);
        if (isPresent == 1) { //'1' if tone is valid
            int uSignalType = infoCW.signalType;
            int uAlertPitch = infoCW.alertPitch;
            int uSignal = infoCW.signal;
            if (DBG) log("onCdmaCallWaiting: uSignalType=" + uSignalType + ", uAlertPitch="
                    + uAlertPitch + ", uSignal=" + uSignal);
            //Map the Signal to a ToneGenerator ToneID only if Signal info is present
            int toneID =
                SignalToneUtil.getAudioToneFromSignalInfo(uSignalType, uAlertPitch, uSignal);

            //Create the SignalInfo tone player and pass the ToneID
            new SignalInfoTonePlayer(toneID).start();
        }
    }

    /**
     * Posts a event causing us to clean up after rejecting (or timing-out) a
     * CDMA call-waiting call.
     *
     * This method is safe to call from any thread.
     * @see #onCdmaCallWaitingReject()
     */
    /* package */ void sendCdmaCallWaitingReject() {
        sendEmptyMessage(CDMA_CALL_WAITING_REJECT);
    }

    /**
     * Performs Call logging based on Timeout or Ignore Call Waiting Call for CDMA,
     * and finally calls Hangup on the Call Waiting connection.
     *
     * This method should be called only from the UI thread.
     * @see #sendCdmaCallWaitingReject()
     */
    private void onCdmaCallWaitingReject() {
        final Call ringingCall = mCM.getFirstActiveRingingCall();

        // Call waiting timeout scenario
        if (ringingCall.getState() == Call.State.WAITING) {
            // Code for perform Call logging and missed call notification
            Connection c = ringingCall.getLatestConnection();

            if (c != null) {
                String number = c.getAddress();
                int presentation = c.getNumberPresentation();
                final long date = c.getCreateTime();
                final long duration = c.getDurationMillis();
                final int callLogType = mCallWaitingTimeOut ?
                        Calls.MISSED_TYPE : Calls.INCOMING_TYPE;

                // get the callerinfo object and then log the call with it.
                Object o = c.getUserData();
                final CallerInfo ci;
                if ((o == null) || (o instanceof CallerInfo)) {
                    ci = (CallerInfo) o;
                } else {
                    ci = ((PhoneUtils.CallerInfoToken) o).currentInfo;
                }

                // Do final CNAP modifications of logNumber prior to logging [mimicking
                // onDisconnect()]
                final String logNumber = PhoneUtils.modifyForSpecialCnapCases(
                        mApplication, ci, number, presentation);
                final int newPresentation = (ci != null) ? ci.numberPresentation : presentation;
                if (DBG) log("- onCdmaCallWaitingReject(): logNumber set to: " + logNumber
                        + ", newPresentation value is: " + newPresentation);

                CallLogAsync.AddCallArgs args = null;
                if (GeminiUtils.isGeminiSupport()) {
                    int cdmaSimId = 1;
                    final int cdmaSlot = GeminiUtils.getSlotByPhoneType(PhoneConstants.PHONE_TYPE_CDMA);
                    if (mApplication.phoneMgr.hasIccCardGemini(cdmaSlot)) {
                        SIMInfo si = SIMInfo.getSIMInfoBySlot(PhoneGlobals.getInstance(), cdmaSlot);
                        if (si != null) {
                            cdmaSimId = (int) si.mSimId;
                        }
                    }

                    args = new CallLogAsync.AddCallArgs(mApplication, ci, logNumber, presentation, callLogType, date,
                            duration, cdmaSimId);
                } else {
                    args = new CallLogAsync.AddCallArgs(
                                mApplication, ci, logNumber, presentation,
                                callLogType, date, duration);
                }

                try {
                mCallLog.addCall(args);
                } catch (SQLiteDiskIOException e) {
                    // TODO Auto-generated catch block
                    Log.e(LOG_TAG, "Error!! - onDisconnect() Disk Full!");
                    e.printStackTrace();
                }

                if (callLogType == Calls.MISSED_TYPE) {
                    // Add missed call notification
                    showMissedCallNotification(c, date);
                } else {
                    // Remove Call waiting 20 second display timer in the queue
                    removeMessages(CALLWAITING_CALLERINFO_DISPLAY_DONE);
                }

                // Hangup the RingingCall connection for CW
                PhoneUtils.hangup(c);
            }

            //Reset the mCallWaitingTimeOut boolean
            mCallWaitingTimeOut = false;
        }
    }

    /**
     * Return the private variable mPreviousCdmaCallState.
     */
    /* package */ Call.State getPreviousCdmaCallState() {
        return mPreviousCdmaCallState;
    }

    /**
     * Return the private variable mVoicePrivacyState.
     */
    /* package */ boolean getVoicePrivacyState() {
        return mVoicePrivacyState;
    }

    /**
     * Return the private variable mIsCdmaRedialCall.
     */
    public boolean getIsCdmaRedialCall() {
        return mIsCdmaRedialCall;
    }

    /**
     * Helper function used to show a missed call notification.
     */
    void showMissedCallNotification(Connection c, final long date) {
        CustomInfo customInfo = new CustomInfo();
        customInfo.date = date;
        if (null != c) {
            customInfo.callVideo = c.isVideo() ? 1 : 0;
        } else {
            customInfo.callVideo = 0;
        }
        PhoneUtils.CallerInfoToken info =
            PhoneUtils.startGetCallerInfo(mApplication, c, this, customInfo);
        if (info != null) {
            // at this point, we've requested to start a query, but it makes no
            // sense to log this missed call until the query comes back.
            if (VDBG) log("showMissedCallNotification: Querying for CallerInfo on missed call...");
            if (info.isFinal) {
                // it seems that the query we have actually is up to date.
                // send the notification then.
                CallerInfo ci = info.currentInfo;

                // Check number presentation value; if we have a non-allowed presentation,
                // then display an appropriate presentation string instead as the missed
                // call.
                String name = ci.name;
                String number = ci.phoneNumber;

                /** M: comment Modify CRXXX can not callback voicemail @{ */
                if (ci.isVoiceMailNumber()) {
                    if (null == name) {
                        //ci.phoneNumber is string of voicemail.
                        name = ci.phoneNumber;
                    }
                    if (null != c) {
                        number = c.getAddress();
                    }
                }
                /** }@ */

                if (ci.numberPresentation == PhoneConstants.PRESENTATION_RESTRICTED) {
                    name = mApplication.getString(R.string.private_num);
                } else if (ci.numberPresentation != PhoneConstants.PRESENTATION_ALLOWED) {
                    name = mApplication.getString(R.string.unknown);
                } else {
                    number = PhoneUtils.modifyForSpecialCnapCases(mApplication,
                            ci, number, ci.numberPresentation);
                }
                mApplication.notificationMgr.notifyMissedCall(name, number,
                        ci.phoneLabel, ci.cachedPhoto, ci.cachedPhotoIcon, date, customInfo.callVideo);
            }
        } else {
            // getCallerInfo() can return null in rare cases, like if we weren't
            // able to get a valid phone number out of the specified Connection.
            Log.w(LOG_TAG, "showMissedCallNotification: got null CallerInfo for Connection " + c);
        }
    }

    /**
     *  Inner class to handle emergency call tone and vibrator
     */
    private class EmergencyTonePlayerVibrator {
        private final int EMG_VIBRATE_LENGTH = 1000;  // ms.
        private final int EMG_VIBRATE_PAUSE  = 1000;  // ms.
        private final long[] mVibratePattern =
                new long[] { EMG_VIBRATE_LENGTH, EMG_VIBRATE_PAUSE };

        private ToneGenerator mToneGenerator;
        // We don't rely on getSystemService(Context.VIBRATOR_SERVICE) to make sure this vibrator
        // object will be isolated from others.
        private Vibrator mEmgVibrator = new SystemVibrator();
        private int mInCallVolume;

        /**
         * constructor
         */
        public EmergencyTonePlayerVibrator() {
        }

        /**
         * Start the emergency tone or vibrator.
         */
        private void start() {
            if (VDBG) log("call startEmergencyToneOrVibrate.");
            int ringerMode = mAudioManager.getRingerMode();

            if ((mIsEmergencyToneOn == EMERGENCY_TONE_ALERT) &&
                    (ringerMode == AudioManager.RINGER_MODE_NORMAL)) {
                log("EmergencyTonePlayerVibrator.start(): emergency tone...");
                /*
                mToneGenerator = new ToneGenerator (AudioManager.STREAM_VOICE_CALL,
                        InCallTonePlayer.TONE_RELATIVE_VOLUME_EMERGENCY);
                */
                if (mToneGenerator != null) {
                    mInCallVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
                    mAudioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL,
                            mAudioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL),
                            0);
                    mToneGenerator.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK);
                    mCurrentEmergencyToneState = EMERGENCY_TONE_ALERT;
                }
            } else if (mIsEmergencyToneOn == EMERGENCY_TONE_VIBRATE) {
                log("EmergencyTonePlayerVibrator.start(): emergency vibrate...");
                if (mEmgVibrator != null) {
                    mEmgVibrator.vibrate(mVibratePattern, 0);
                    mCurrentEmergencyToneState = EMERGENCY_TONE_VIBRATE;
                }
            }
        }

        /**
         * If the emergency tone is active, stop the tone or vibrator accordingly.
         */
        private void stop() {
            if (VDBG) log("call stopEmergencyToneOrVibrate.");

            if ((mCurrentEmergencyToneState == EMERGENCY_TONE_ALERT)
                    && (mToneGenerator != null)) {
                mToneGenerator.stopTone();
                mToneGenerator.release();
                mAudioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL,
                        mInCallVolume,
                        0);
            } else if ((mCurrentEmergencyToneState == EMERGENCY_TONE_VIBRATE)
                    && (mEmgVibrator != null)) {
                mEmgVibrator.cancel();
            }
            mCurrentEmergencyToneState = EMERGENCY_TONE_OFF;
        }
    }

    private BluetoothProfile.ServiceListener mBluetoothProfileServiceListener =
        new BluetoothProfile.ServiceListener() {
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            mBluetoothHeadset = (BluetoothHeadset) proxy;
            if (VDBG) log("- Got BluetoothHeadset: " + mBluetoothHeadset);
        }

        public void onServiceDisconnected(int profile) {
            mBluetoothHeadset = null;
        }
    };

    private void onRingbackTone(AsyncResult r) {
        boolean playTone = (Boolean)(r.result);

        if (playTone) {
            // Only play when foreground call is in DIALING or ALERTING.
            // to prevent a late coming playtone after ALERTING.
            // Don't play ringback tone if it is in play, otherwise it will cut
            // the current tone and replay it
            if (mCM.getActiveFgCallState().isDialing() &&
                mInCallRingbackTonePlayer == null) {
                mInCallRingbackTonePlayer = new InCallTonePlayer(InCallTonePlayer.TONE_RING_BACK);
                mInCallRingbackTonePlayer.start();
            }
        } else {
            if (mInCallRingbackTonePlayer != null) {
                mInCallRingbackTonePlayer.stopTone();
                mInCallRingbackTonePlayer = null;
            }
        }
    }

    /**
     * Toggle mute and unmute requests while keeping the same mute state
     */
    private void onResendMute() {
        boolean muteState = PhoneUtils.getMute();
        PhoneUtils.setMute(!muteState);
        PhoneUtils.setMute(muteState);
    }

    /**
     * Retrieve the phone number from the caller info or the connection.
     *
     * For incoming call the number is in the Connection object. For
     * outgoing call we use the CallerInfo phoneNumber field if
     * present. All the processing should have been done already (CDMA vs GSM numbers).
     *
     * If CallerInfo is missing the phone number, get it from the connection.
     * Apply the Call Name Presentation (CNAP) transform in the connection on the number.
     *
     * @param conn The phone connection.
     * @param callerInfo The CallerInfo. Maybe null.
     * @return the phone number.
     */
    private String getLogNumber(Connection conn, CallerInfo callerInfo) {
        String number = null;

        if (conn.isIncoming()) {
            number = conn.getAddress();
        } else {
            // For emergency and voicemail calls,
            // CallerInfo.phoneNumber does *not* contain a valid phone
            // number.  Instead it contains an I18N'd string such as
            // "Emergency Number" or "Voice Mail" so we get the number
            // from the connection.
            if (null == callerInfo || TextUtils.isEmpty(callerInfo.phoneNumber) ||
                callerInfo.isEmergencyNumber() || callerInfo.isVoiceMailNumber()) {
                if (conn.getCall().getPhone().getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA) {
                    // In cdma getAddress() is not always equals to getOrigDialString().
                    number = conn.getOrigDialString();
                } else {
                    number = conn.getAddress();
                }
            } else {
                number = callerInfo.phoneNumber;
            }
        }

        if (null == number) {
            return null;
        } else {
            int presentation = conn.getNumberPresentation();

            // Do final CNAP modifications.
            number = PhoneUtils.modifyForSpecialCnapCases(mApplication, callerInfo,
                                                          number, presentation);
            if (!PhoneNumberUtils.isUriNumber(number)) {
                number = PhoneNumberUtils.stripSeparators(number);
            }
            if (VDBG) log("getLogNumber: " + number);
            return number;
        }
    }

    /**
     * Get the caller info.
     *
     * @param conn The phone connection.
     * @return The CallerInfo associated with the connection. Maybe null.
     */
    CallerInfo getCallerInfoFromConnection(Connection conn) {
        CallerInfo ci = null;
        Object o = conn.getUserData();

        if ((o == null) || (o instanceof CallerInfo)) {
            ci = (CallerInfo) o;
        } else if (o instanceof Uri) {
            ci = null;
        } else {
            ci = ((PhoneUtils.CallerInfoToken) o).currentInfo;
        }
        return ci;
    }

    /**
     * Get the presentation from the callerinfo if not null otherwise,
     * get it from the connection.
     *
     * @param conn The phone connection.
     * @param callerInfo The CallerInfo. Maybe null.
     * @return The presentation to use in the logs.
     */
    int getPresentation(Connection conn, CallerInfo callerInfo) {
        int presentation;

        if (null == callerInfo) {
            presentation = conn.getNumberPresentation();
        } else {
            presentation = callerInfo.numberPresentation;
            if (DBG) log("- getPresentation(): ignoring connection's presentation: " +
                         conn.getNumberPresentation());
        }
        if (DBG) log("- getPresentation: presentation: " + presentation);
        return presentation;
    }

    private void log(String msg) {
        Log.d(LOG_TAG, msg);
    }
    
    /**
     * When you want to accept the waiting call , if the Call types (video/voice
     * call) of the active and waiting call are different, we need to disconnect
     * the active call before accept the waiting call (replace) because the
     * voice call and video call cannot exist at the same time.
     * 
     * But now we cannot replace successfully, so Telephony Framework will
     * disconnect the waiting call. And we need to notice user there was a
     * missed call once.So Framework will notice MMI by the Message
     * (PHONE_WAITING_DISCONNECT
     * /PHONE_WAITING_DISCONNECT1/PHONE_WAITING_DISCONNECT2) and MMI will notice
     * user in Notification bar and save a call log (missed call).
     * 
     * @param r
     * @param simId
     */
    private void onDisconnectForVTWaiting(AsyncResult r, final int simId) {

        if (VDBG) {
            log("onDisconnectForVTWaiting()... , sim id : " + simId);
        }
        Connection c = (Connection) r.result;

        if (c != null) {

            final String number = c.getAddress();
            final long date = c.getCreateTime();
            final long duration = c.getDurationMillis();
            final Connection.DisconnectCause cause = c.getDisconnectCause();
            final Phone phone = c.getCall().getPhone();

            boolean isSipCall = (c.getCall().getPhone().getPhoneType() == PhoneConstants.PHONE_TYPE_SIP);

            int vtCall;
            if (c.isVideo()) {
                vtCall = 1;
            } else {
                vtCall = 0;
            }

            final int callLogType = Calls.MISSED_TYPE;

            final CallerInfo ci = getCallerInfoFromConnection(c);
            final String logNumber = getLogNumber(c, ci);
            final int presentation = getPresentation(c, ci);
            CallLogAsync.AddCallArgs args;

            if (!GeminiUtils.isGeminiSupport() || isSipCall) {
                int simIdEx;
                if (isSipCall) {
                    simIdEx = CALL_TYPE_SIP;
                } else {
                    simIdEx = CALL_TYPE_NONE;
                    if (PhoneGlobals.getInstance().phoneMgr.hasIccCard()) {
                        simIdEx = 1;
                    }
                }

                args = new CallLogAsync.AddCallArgs(mApplication, ci,
                        logNumber, presentation, callLogType, date, duration,
                        simIdEx, vtCall);

            } else {
                SIMInfo si;
                int simIdEx = CALL_TYPE_NONE;
                if (PhoneGlobals.getInstance().phoneMgr.hasIccCardGemini(simId)) {
                    si = SIMInfo
                            .getSIMInfoBySlot(PhoneGlobals.getInstance(), simId);
                    if (si != null) {
                        simIdEx = (int) si.mSimId;
                    }
                }
                
                args = new CallLogAsync.AddCallArgs(mApplication, ci,
                        logNumber, presentation, callLogType, date, duration,
                        simIdEx, vtCall);

            }
            if (!isSipCall) {
                long delayMillis = 3000;
                Message message = Message.obtain();
                message.what = PHONE_WAITING_DISCONNECT_STOP_TONE_PLAYER;        
                if (mVideoOrVoiceCallWaitingTonePlayer == null) {
                        mVideoOrVoiceCallWaitingTonePlayer = new InCallTonePlayer(InCallTonePlayer.TONE_CALL_WAITING);
                        mVideoOrVoiceCallWaitingTonePlayer.start();
                }
              // Start call waiting tone if needed when answering
                sendMessageDelayed(message,delayMillis);
                if (c.isVideo()) {
                    Toast.makeText(PhoneGlobals.getInstance().getApplicationContext(),
                            R.string.cannot_answered_Video_Call, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(PhoneGlobals.getInstance().getApplicationContext(),
                            R.string.cannot_answered_Voice_Call, Toast.LENGTH_LONG).show();
                }
            }

            //mCallLog.addCall(args);
            try {
                mCallLog.addCall(args);
            } catch (SQLiteDiskIOException e) {
                // TODO Auto-generated catch block
                Log.e(LOG_TAG, "Error!! - onDisconnect() Disk Full!");
                e.printStackTrace();
            }
            showMissedCallNotification(c, date);

        }
    }
    
    private void closeVTManager() {

        if (DBG) {
            log("closeVTManager()!");
            log("- call VTManager onDisconnected ! ");
        }
        
        VTManager.getInstance().onDisconnected();
        if (VDBG) {
            log("- finish call VTManager onDisconnected ! ");
        }

        if (VDBG) {
            log("- set VTManager close ! ");
        }
        VTManager.getInstance().setVTClose();
        if (VDBG) {
            log("- finish set VTManager close ! ");
        }

        if (VTInCallScreenFlags.getInstance().mVTInControlRes) {
            PhoneGlobals.getInstance().sendBroadcast(new Intent(VTCallUtils.VT_CALL_END));
            VTInCallScreenFlags.getInstance().mVTInControlRes = false;
        }
    }

    /*
     * New Feature by Mediatek Begin.
     *   CR ID: ALPS00114062
     */
    private String formatDuration(long elapsedSeconds) {
        long minutes = 0;
        long seconds = 0;

        if (elapsedSeconds >= 60) {
            minutes = elapsedSeconds / 60;
            elapsedSeconds -= minutes * 60;
        }
        seconds = elapsedSeconds;

        return (mApplication.getString(R.string.card_title_call_ended) + "(" +
            mApplication.getString(R.string.callDurationFormat, minutes, seconds) + ")");
    }

    /* package */ boolean hasPendingCallerInfoQuery() {
        return mCallerInfoQueryState == CALLERINFO_QUERYING;
    }

    /*
     * New Feature by Mediatek End.
     */

    BlackListManager mBlackListManager;
    
    void switchRingToneByNeeded(Call ring) {
        if (PhoneUtils.isRealIncomingCall(ring.getState())) {
            CallerInfo ci = getCallerInfoFromConnection(ring.getLatestConnection());
            if (ci == null) {
                return ;
            }
            
            Uri custUri = ci.contactRingtoneUri;
            
            if (custUri == null) {
                log("switchRingToneByNeeded: custUri == null");
                if (VTCallUtils.isVideoCall(ring)) {
                    custUri = Settings.System.DEFAULT_VIDEO_CALL_URI;
                } else {
                    custUri = Settings.System.DEFAULT_RINGTONE_URI;
                }
            }
            log("switchRingToneByNeeded: ring call = " + ring);
            log("switchRingToneByNeeded: new ringUri = " + custUri);
            log("switchRingToneByNeeded: old ringUri = " + mRinger.getCustomRingToneUri());
            
            if (!custUri.equals(mRinger.getCustomRingToneUri())) {
                mRinger.stopRing();
                mRinger.setCustomRingtoneUri(custUri);
                //if (ring.getPhone().getPhoneType() == PhoneConstants.PHONE_TYPE_SIP) {
                    mRinger.ring();
                //}
                log("switchRingToneByNeeded: stop and start new ring!");
            }
        }
    }
    
    /**
     * Mainly use to reset the audio state when hangup dialing video call when answer the 
     * incoming voice call.
     */
    void resetAudioState() {
        if (VDBG) {
            log("resetAudioState()...");
        }

        if (mBluetoothHeadset != null) {
            mBluetoothHeadset.disconnectAudio();
        }

        // call turnOnSpeaker() with state=false and store=true even if speaker
        // is already off to reset user requested speaker state.
        PhoneUtils.turnOnSpeaker(mApplication, false, true);
    }


    private void initFakeCall(Connection c, int simId) {
        mApplication.inCallUiState.latestDisconnectCall = new InCallUiState.FakeCall();
        mApplication.inCallUiState.latestDisconnectCall.cause = c.getDisconnectCause();
        if (GeminiUtils.isGeminiSupport()) {
            mApplication.inCallUiState.latestDisconnectCall.slotId = simId;
        } else {
            mApplication.inCallUiState.latestDisconnectCall.slotId = 0;
        }
        mApplication.inCallUiState.latestDisconnectCall.number = c.getAddress();
        mApplication.inCallUiState.latestDisconnectCall.phoneType = c.getCall().getPhone().getPhoneType();
    }
    
    public void resetBeforeCall() {
        if (mToneThread != null && mToneThread.isAlive()) {
            mToneThread.stopTone();
            if (DBG) {
                log("resetBeforeCall: notify the tone thread to exit.");
            }
        } else {
            if (DBG) {
                log("resetBeforeCall: do nothing.");
            }
        }
    }
    
    private void checkAndTriggerRingTone() {
        log("checkAndTriggerRingTone");
        if (!DualTalkUtils.isSupportDualTalk() || mRinger.isRinging()) {
            log("checkAndTriggerRingTone:  return directly");
            return ;
        }
        log("trigger the ringtone!");
        Call ringCall = mCM.getFirstActiveRingingCall();
        if (ringCall.getPhone().getPhoneType() == PhoneConstants.PHONE_TYPE_SIP
                && PhoneUtils.isRealIncomingCall(ringCall.getState())) {
            Connection c = ringCall.getLatestConnection();
            if (c == null) {
                return ;
            }
            CallerInfo info = null;
            Object obj = c.getUserData();
            if (obj instanceof PhoneUtils.CallerInfoToken) {
                info = ((PhoneUtils.CallerInfoToken) obj).currentInfo;
            } else if (obj instanceof CallerInfo) {
                info = (CallerInfo) obj;
            }
            
            if (info != null && info.contactRingtoneUri != null) {
                mRinger.setCustomRingtoneUri(info.contactRingtoneUri);
            } else {
                mRinger.setCustomRingtoneUri(Settings.System.DEFAULT_RINGTONE_URI);
            }
            
            mRinger.ring();
        }
    }

    /// M:Gemini+ @{
    private static final int PHONE_DISCONNECT2 = 103;
    private static final int PHONE_DISCONNECT3 = 203;
    private static final int PHONE_DISCONNECT4 = 303;
    private static final int[] PHONE_DISCONNECT_GEMINI = { PHONE_DISCONNECT, PHONE_DISCONNECT2, PHONE_DISCONNECT3,
            PHONE_DISCONNECT4 };

    private static final int PHONE_MWI_CHANGED2 = 121;
    private static final int PHONE_MWI_CHANGED3 = 221;
    private static final int PHONE_MWI_CHANGED4 = 321;
    private static final int[] PHONE_MWI_CHANGED_GEMINI = { PHONE_MWI_CHANGED, PHONE_MWI_CHANGED2, PHONE_MWI_CHANGED3,
            PHONE_MWI_CHANGED4 };

    private static final int PHONE_STATE_LISTENER_EVENT = PhoneStateListener.LISTEN_MESSAGE_WAITING_INDICATOR
            | PhoneStateListener.LISTEN_CALL_FORWARDING_INDICATOR | PhoneStateListener.LISTEN_SERVICE_STATE;
    private PhoneStateListener[] mPhoneStateListeners = null;
    // Last cfi information
    private boolean[] mCfiStatus = null;

    private void listenPhoneState() {
        final int[] geminiSlots = GeminiUtils.getSlots();
        final int count = geminiSlots.length;
        if (mCfiStatus == null) {
            mCfiStatus = new boolean[count];
        }

        TelephonyManager telephonyManager = (TelephonyManager) mApplication.getSystemService(Context.TELEPHONY_SERVICE);
        if (GeminiUtils.isGeminiSupport()) {
            if (mPhoneStateListeners == null) {
                mPhoneStateListeners = new PhoneStateListener[count];
            }
            for (int i = 0; i < count; i++) {
                if (mPhoneStateListeners[i] == null) {
                    mPhoneStateListeners[i] = new GeminiPhoneStateListener(geminiSlots[i]);
                }
                mCfiStatus[i] = false;
                telephonyManager.listenGemini(mPhoneStateListeners[i], PHONE_STATE_LISTENER_EVENT, geminiSlots[i]);
            }
        } else {
            mCfiStatus[0] = false;
            if (mPhoneStateListeners == null) {
                mPhoneStateListeners = new PhoneStateListener[1];
                mPhoneStateListeners[0] = new GeminiPhoneStateListener(geminiSlots[0]);
            }
            telephonyManager.listen(mPhoneStateListeners[0], PHONE_STATE_LISTENER_EVENT);
        }
    }

    private final class GeminiPhoneStateListener extends PhoneStateListener {
        boolean inAirplaneMode = true;
        int mSlotId;

        public GeminiPhoneStateListener(int slotId) {
            mSlotId = slotId;
        }

        @Override
        public void onMessageWaitingIndicatorChanged(boolean mwi) {
            Log.i(LOG_TAG, "PhoneStateListener.onMessageWaitingIndicatorChanged: mwi=" + mwi);
            onMwiChanged(mwi, mSlotId);
        }

        @Override
        public void onCallForwardingIndicatorChanged(boolean cfi) {
            Log.i(LOG_TAG, "PhoneStateListener.onCallForwardingIndicatorChanged: cfi=" + cfi);
            mCfiStatus[mSlotId] = cfi;

            if (!inAirplaneMode) {
                onCfiChanged(cfi, mSlotId);
            }
        }

        @Override
        public void onServiceStateChanged(ServiceState serviceState) {
            Log.i(LOG_TAG, "PhoneStateListener.onServiceStateChanged: serviceState=" + serviceState);
            // final boolean inAirplaneMode;
            inAirplaneMode = serviceState.getState() == ServiceState.STATE_POWER_OFF;
            if (inAirplaneMode == true) {
                onCfiChanged(false, mSlotId);
            } else {
                if (mCfiStatus[mSlotId] && (serviceState.getState() == ServiceState.STATE_IN_SERVICE)) {
                    onCfiChanged(true, mSlotId);
                }
            }
        }
    }
    // Time shreshold to record call history
    private static final int CALL_DURATION_THRESHOLD_FOR_CALL_HISTORY = 10000; // msec

    private static final int EVENT_SIMINFO_CHANGED = 2001;
    /// @}

    /**
     * set DefaultRingtoneUri to mRinger before start CallerInfo query.
     * try to set custom ringtone from CallerInfoCache instead of DEFAULT_RINGTONE_URI as default.
     *
     * @param c
     */
    private void setDefaultRingtoneUri(Connection c) {
        final CallerInfoCache.CacheEntry entry = mApplication.callerInfoCache.getCacheEntry(c.getAddress());
        if (entry != null && entry.customRingtone != null) {
            log("Before query; custom ringtone found in CallerInfoCache for call( " + c.getAddress()
                    + " ), setting up ringer: " + entry.customRingtone);
            mRinger.setCustomRingtoneUri(Uri.parse(entry.customRingtone));
        } else {
            log("Before query; custom ringtone not found in CallerInfoCache. Use default ringer tone.");
            if (FeatureOption.MTK_VT3G324M_SUPPORT && VTCallUtils.isVTRinging()) {
                mRinger.setCustomRingtoneUri(Settings.System.DEFAULT_VIDEO_CALL_URI);
            } else {
                mRinger.setCustomRingtoneUri(Settings.System.DEFAULT_RINGTONE_URI);
            }
        }
    }
}
