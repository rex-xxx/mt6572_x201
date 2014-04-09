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

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.IBluetoothHeadsetPhone;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.IPowerManager;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemService;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UpdateLock;
import android.os.UserHandle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.provider.Telephony.SIMInfo;
import android.provider.CallLog.Calls;
import android.provider.Settings.System;
import android.provider.Settings.SettingNotFoundException;
import android.telephony.ServiceState;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Slog;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.view.WindowManagerGlobal;
import android.app.ActivityManagerNative;
import android.app.IActivityManager;

import com.android.i18n.phonenumbers.PhoneNumberUtil;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallManager;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.CallerInfo;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.MmiCode;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.TelephonyCapabilities;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.cdma.TtyIntent;
import com.android.internal.telephony.gemini.*;
import com.android.phone.OtaUtils.CdmaOtaScreenState;
import com.android.server.sip.SipService;
import com.mediatek.CellConnService.CellConnMgr;
import com.mediatek.calloption.SimAssociateHandler;
import com.mediatek.common.telephony.ITelephonyEx;
import com.mediatek.phone.DualTalkUtils;
import com.mediatek.phone.GeminiConstants;
import com.mediatek.phone.HyphonManager;
import com.mediatek.phone.PhoneLog;
import com.mediatek.phone.PhoneFeatureConstants.FeatureOption;
import com.mediatek.phone.SIMInfoWrapper;
import com.mediatek.phone.ext.ExtensionManager;
import com.mediatek.phone.gemini.GeminiUtils;
import com.mediatek.phone.gemini.GeminiRegister;
import com.mediatek.phone.provider.CallHistoryDatabaseHelper;
import com.mediatek.phone.vt.VTCallUtils;
import com.mediatek.phone.vt.VTInCallScreenFlags;
import com.mediatek.vt.VTManager;

import java.util.List;

// ECC button should be hidden when there is no service.
import static android.provider.Telephony.Intents.SPN_STRINGS_UPDATED_ACTION;
import static android.provider.Telephony.Intents.EXTRA_PLMN;
import static android.provider.Telephony.Intents.EXTRA_SHOW_PLMN;

/**
 * Global state for the telephony subsystem when running in the primary
 * phone process.
 */
public class PhoneGlobals extends ContextWrapper implements AccelerometerListener.OrientationListener {
    /* package */ static final String LOG_TAG = "PhoneGlobals";

    /**
     * Phone app-wide debug level:
     *   0 - no debug logging
     *   1 - normal debug logging if ro.debuggable is set (which is true in
     *       "eng" and "userdebug" builds but not "user" builds)
     *   2 - ultra-verbose debug logging
     *
     * Most individual classes in the phone app have a local DBG constant,
     * typically set to
     *   (PhoneGlobals.DBG_LEVEL >= 1) && (SystemProperties.getInt("ro.debuggable", 0) == 1)
     * or else
     *   (PhoneGlobals.DBG_LEVEL >= 2)
     * depending on the desired verbosity.
     *
     * ***** DO NOT SUBMIT WITH DBG_LEVEL > 0 *************
     */
    /* package */ static final int DBG_LEVEL = 1;

    //private static final boolean DBG =
    //         (PhoneGlobals.DBG_LEVEL >= 1) && (SystemProperties.getInt("ro.debuggable", 0) == 1);
    // private static final boolean VDBG = (PhoneGlobals.DBG_LEVEL >= 2);
    
    private static final int USE_CM = 1;  // use call manager or not

    private static final boolean DBG = true ;
    private static final boolean VDBG = true;

    // Message codes; see mHandler below.
    private static final int EVENT_WIRED_HEADSET_PLUG = 7;
    private static final int EVENT_SIM_STATE_CHANGED = 8;
    private static final int EVENT_UPDATE_INCALL_NOTIFICATION = 9;
    private static final int EVENT_DATA_ROAMING_DISCONNECTED = 10;
    private static final int EVENT_DATA_ROAMING_OK = 11;
    private static final int EVENT_UNSOL_CDMA_INFO_RECORD = 12;
    private static final int EVENT_DOCK_STATE_CHANGED = 13;
    private static final int EVENT_TTY_PREFERRED_MODE_CHANGED = 14;
    private static final int EVENT_TTY_MODE_GET = 15;
    private static final int EVENT_TTY_MODE_SET = 16;
    private static final int EVENT_START_SIP_SERVICE = 17;
    private static final int EVENT_TIMEOUT = 18;
    private static final int EVENT_TOUCH_ANSWER_VT = 30;
    
    /// M: To trigger main thread looper for showing confirm dialog.
    private static final int EVENT_TRIGGER_MAINTHREAD_LOOPER = 31; 

    // The MMI codes are also used by the InCallScreen.
    public static final int MMI_INITIATE = 51;
    public static final int MMI_COMPLETE = 52;
    public static final int MMI_CANCEL = 53;

    public static final int EVENT_SHOW_INCALL_SCREEN_FOR_STK_SETUP_CALL = 57;
    public static final int DELAY_SHOW_INCALL_SCREEN_FOR_STK_SETUP_CALL = 160;

    private static final String PERMISSION = android.Manifest.permission.PROCESS_OUTGOING_CALLS;
    private static final String STKCALL_REGISTER_SPEECH_INFO = "com.android.stk.STKCALL_REGISTER_SPEECH_INFO";
    public static final String MISSEDCALL_DELETE_INTENT = "com.android.phone.MISSEDCALL_DELETE_INTENT";
    // Don't use message codes larger than 99 here; those are reserved for
    // the individual Activities of the Phone UI.
    public static final String OLD_NETWORK_MODE = "com.android.phone.OLD_NETWORK_MODE";
    public static final String NETWORK_MODE_CHANGE = "com.android.phone.NETWORK_MODE_CHANGE";
    public static final String NETWORK_MODE_CHANGE_RESPONSE = "com.android.phone.NETWORK_MODE_CHANGE_RESPONSE";
    public static final int MESSAGE_SET_PREFERRED_NETWORK_TYPE = 10011;

    public static final boolean IS_VIDEO_CALL_SUPPORT = true;

    private static final String ACTION_MODEM_STATE = "com.mtk.ACTION_MODEM_STATE";
    private static final int CCCI_MD_BROADCAST_EXCEPTION = 1;
    private static final int CCCI_MD_BROADCAST_RESET = 2;
    private static final int CCCI_MD_BROADCAST_READY = 3;

    /**
     * Allowable values for the wake lock code.
     *   SLEEP means the device can be put to sleep.
     *   PARTIAL means wake the processor, but we display can be kept off.
     *   FULL means wake both the processor and the display.
     */
    public enum WakeState {
        SLEEP,
        PARTIAL,
        FULL
    }

    /**
     * Intent Action used for hanging up the current call from Notification bar. This will
     * choose first ringing call, first active call, or first background call (typically in
     * HOLDING state).
     */
    public static final String ACTION_HANG_UP_ONGOING_CALL =
            "com.android.phone.ACTION_HANG_UP_ONGOING_CALL";

    /**
     * Intent Action used for making a phone call from Notification bar.
     * This is for missed call notifications.
     */
    public static final String ACTION_CALL_BACK_FROM_NOTIFICATION =
            "com.android.phone.ACTION_CALL_BACK_FROM_NOTIFICATION";

    /**
     * Intent Action used for sending a SMS from notification bar.
     * This is for missed call notifications.
     */
    public static final String ACTION_SEND_SMS_FROM_NOTIFICATION =
            "com.android.phone.ACTION_SEND_SMS_FROM_NOTIFICATION";

    private static PhoneGlobals sMe;

    // A few important fields we expose to the rest of the package
    // directly (rather than thru set/get methods) for efficiency.
    public Phone phone;
    public CallController callController;
    public InCallUiState inCallUiState;
    public CallerInfoCache callerInfoCache;
    public CallNotifier notifier;
    public NotificationMgr notificationMgr;
    public Ringer ringer;
    public IBluetoothHeadsetPhone mBluetoothPhone;
    public PhoneInterfaceManager phoneMgr;
    public PhoneInterfaceManagerEx phoneMgrEx;
    public CallManager mCM;
    int mBluetoothHeadsetState = BluetoothProfile.STATE_DISCONNECTED;
    int mBluetoothHeadsetAudioState = BluetoothHeadset.STATE_AUDIO_DISCONNECTED;
    boolean mShowBluetoothIndication = false;
    static int mDockState = Intent.EXTRA_DOCK_STATE_UNDOCKED;
    static boolean sVoiceCapable = true;

    // Internal PhoneGlobals Call state tracker
    public CdmaPhoneCallState cdmaPhoneCallState;

    // The InCallScreen instance (or null if the InCallScreen hasn't been
    // created yet.)
    private InCallScreen mInCallScreen;

    // The currently-active PUK entry activity and progress dialog.
    // Normally, these are the Emergency Dialer and the subsequent
    // progress dialog.  null if there is are no such objects in
    // the foreground.
    private Activity mPUKEntryActivity;
    private ProgressDialog mPUKEntryProgressDialog;

    private boolean mIsSimPinEnabled;
    private String mCachedSimPin;

    // True if a wired headset is currently plugged in, based on the state
    // from the latest Intent.ACTION_HEADSET_PLUG broadcast we received in
    // mReceiver.onReceive().
    private boolean mIsHeadsetPlugged;

    // True if the keyboard is currently *not* hidden
    // Gets updated whenever there is a Configuration change
    private boolean mIsHardKeyboardOpen;

    // True if we are beginning a call, but the phone state has not changed yet
    private boolean mBeginningCall;

    // Last phone state seen by updatePhoneState()
    private PhoneConstants.State mLastPhoneState = PhoneConstants.State.IDLE;

    private WakeState mWakeState = WakeState.SLEEP;

    private PowerManager mPowerManager;
    private IPowerManager mPowerManagerService;
    private PowerManager.WakeLock mWakeLock;
    private PowerManager.WakeLock mWakeLockForDisconnect;
    private int mWakelockSequence = 0; 
    private PowerManager.WakeLock mPartialWakeLock;
    private PowerManager.WakeLock mProximityWakeLock;
    private KeyguardManager mKeyguardManager;
    private AccelerometerListener mAccelerometerListener;
    private int mOrientation = AccelerometerListener.ORIENTATION_UNKNOWN;

    private UpdateLock mUpdateLock;

    // Broadcast receiver for various intent broadcasts (see onCreate())
    private final BroadcastReceiver mReceiver = new PhoneGlobalsBroadcastReceiver();

    // Broadcast receiver purely for ACTION_MEDIA_BUTTON broadcasts
    private final BroadcastReceiver mMediaButtonReceiver = new MediaButtonBroadcastReceiver();

    /** boolean indicating restoring mute state on InCallScreen.onResume() */
    private boolean mShouldRestoreMuteOnInCallResume;

    /**
     * The singleton OtaUtils instance used for OTASP calls.
     *
     * The OtaUtils instance is created lazily the first time we need to
     * make an OTASP call, regardless of whether it's an interactive or
     * non-interactive OTASP call.
     */
    public OtaUtils otaUtils;

    // Following are the CDMA OTA information Objects used during OTA Call.
    // cdmaOtaProvisionData object store static OTA information that needs
    // to be maintained even during Slider open/close scenarios.
    // cdmaOtaConfigData object stores configuration info to control visiblity
    // of each OTA Screens.
    // cdmaOtaScreenState object store OTA Screen State information.
    public OtaUtils.CdmaOtaProvisionData cdmaOtaProvisionData;
    public OtaUtils.CdmaOtaConfigData cdmaOtaConfigData;
    public OtaUtils.CdmaOtaScreenState cdmaOtaScreenState;
    public OtaUtils.CdmaOtaInCallScreenUiState cdmaOtaInCallScreenUiState;

    // TTY feature enabled on this platform
    private boolean mTtyEnabled;
    
    // ECC button should be hidden when there is no service.
    private boolean mIsNoService[] = {true, true};
    
    AudioManager mAudioManager = null;
    
    public boolean isEnableTTY() {
        return mTtyEnabled;
    }
    // Current TTY operating mode selected by user
    private int mPreferredTtyMode = Phone.TTY_MODE_OFF;

    
    /**
     * Set the restore mute state flag. Used when we are setting the mute state
     * OUTSIDE of user interaction {@link PhoneUtils#startNewCall(Phone)}
     */
    /*package*/void setRestoreMuteOnInCallResume (boolean mode) {
        PhoneLog.d(LOG_TAG, "setRestoreMuteOnInCallResume, mode = " + mode);
        mShouldRestoreMuteOnInCallResume = mode;
    }

    /**
     * Get the restore mute state flag.
     * This is used by the InCallScreen {@link InCallScreen#onResume()} to figure
     * out if we need to restore the mute state for the current active call.
     */
    /*package*/boolean getRestoreMuteOnInCallResume () {
        return mShouldRestoreMuteOnInCallResume;
    }

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            PhoneConstants.State phoneState;
            switch (msg.what) {
                // Starts the SIP service. It's a no-op if SIP API is not supported
                // on the deivce.
                // TODO: Having the phone process host the SIP service is only
                // temporary. Will move it to a persistent communication process
                // later.
                case EVENT_START_SIP_SERVICE:
                    SipService.start(getApplicationContext());
                    break;

                case EVENT_UPDATE_INCALL_NOTIFICATION:
                    // Tell the NotificationMgr to update the "ongoing
                    // call" icon in the status bar, if necessary.
                    // Currently, this is triggered by a bluetooth headset
                    // state change (since the status bar icon needs to
                    // turn blue when bluetooth is active.)
                    if (DBG) Log.d(LOG_TAG, "- updating in-call notification from handler...");
                    notificationMgr.updateInCallNotification();
                    break;

                case EVENT_DATA_ROAMING_DISCONNECTED:
                    notificationMgr.showDataDisconnectedRoaming(msg.arg1);
                    break;

                case EVENT_DATA_ROAMING_OK:
                    notificationMgr.hideDataDisconnectedRoaming();
                    break;

                case MMI_INITIATE:
                case MMI_INITIATE2:
                case MMI_INITIATE3:
                case MMI_INITIATE4:
                    int mmiInitiateSlot = GeminiRegister.getSlotIdByRegisterEvent(msg.what, MMI_INITIATE_GEMINI);
                    if (mInCallScreen == null) {
                        inCallUiState.setPendingUssdMessage(
                                Message.obtain(mHandler, mmiInitiateSlot, (AsyncResult) msg.obj));
                    }
                    break;

                case MMI_COMPLETE:
                case MMI_COMPLETE2:
                case MMI_COMPLETE3:
                case MMI_COMPLETE4:
                    inCallUiState.setPendingUssdMessage(null);
                    int mmiCompleteSlot = GeminiRegister.getSlotIdByRegisterEvent(msg.what, MMI_COMPLETE_GEMINI);
                    onMMIComplete((AsyncResult) msg.obj, mmiCompleteSlot);
                    break;

                case MMI_CANCEL:
                case MMI_CANCEL2:
                case MMI_CANCEL3:
                case MMI_CANCEL4:
                    int mmiCancelSlot = GeminiRegister.getSlotIdByRegisterEvent(msg.what, MMI_CANCEL_GEMINI);
                    PhoneUtils.cancelMmiCodeExt(phone, mmiCancelSlot);
                    break;

                case EVENT_WIRED_HEADSET_PLUG:
                    // Since the presence of a wired headset or bluetooth affects the
                    // speakerphone, update the "speaker" state.  We ONLY want to do
                    // this on the wired headset connect / disconnect events for now
                    // though, so we're only triggering on EVENT_WIRED_HEADSET_PLUG.

                    phoneState = mCM.getState();
                    // Do not change speaker state if phone is not off hook
                    if (phoneState == PhoneConstants.State.OFFHOOK) {
                        if (!isShowingCallScreen() && !isBluetoothHeadsetAudioOn()) {
                            if (!isHeadsetPlugged()) {
                                /// M: for Video Call && ALPS00456873 @{
                                // change feature: if VT call is dial out with
                                // speaker off(headset/bluetooth connected)
                                // when plug out, should force to turn on speaker,
                                // otherwise restore to previous mode
                                //
                                // MTK add
                                if(VTCallUtils.isVTActive() && VTCallUtils.isVTDialWithSpeakerOff()) {
                                    PhoneUtils.turnOnSpeaker(getApplicationContext(), true, true);
                                    VTCallUtils.setVTDialWithSpeakerOff(false);
                                } else {
                                /// @}
                                     PhoneUtils.restoreSpeakerMode(getApplicationContext());
                                }
                            } else {
                                // if the state is "connected", force the speaker off without
                                // storing the state.
                                PhoneUtils.turnOnSpeaker(getApplicationContext(), false, false);
                            }
                        }
                    }
                    // Update the Proximity sensor based on headset state
                    updateProximitySensorMode(phoneState);

                    // Force TTY state update according to new headset state
                    if (mTtyEnabled) {
                        sendMessage(obtainMessage(EVENT_TTY_PREFERRED_MODE_CHANGED, 0));
                    }
                    break;

                case EVENT_SIM_STATE_CHANGED:
                    // Marks the event where the SIM goes into ready state.
                    // Right now, this is only used for the PUK-unlocking
                    // process.
                    if (msg.obj.equals(IccCardConstants.INTENT_VALUE_ICC_READY)) {
                        // when the right event is triggered and there
                        // are UI objects in the foreground, we close
                        // them to display the lock panel.
                        if (mPUKEntryActivity != null) {
                            mPUKEntryActivity.finish();
                            mPUKEntryActivity = null;
                        }
                        if (mPUKEntryProgressDialog != null) {
                            mPUKEntryProgressDialog.dismiss();
                            mPUKEntryProgressDialog = null;
                        }
                    }
                    break;

                case EVENT_UNSOL_CDMA_INFO_RECORD:
                    //TODO: handle message here;
                    break;

                case EVENT_DOCK_STATE_CHANGED:
                    // If the phone is docked/undocked during a call, and no wired or BT headset
                    // is connected: turn on/off the speaker accordingly.
                    boolean inDockMode = false;
                    if (mDockState != Intent.EXTRA_DOCK_STATE_UNDOCKED) {
                        inDockMode = true;
                    }
                    if (VDBG) Log.d(LOG_TAG, "received EVENT_DOCK_STATE_CHANGED. Phone inDock = "
                            + inDockMode);

                    phoneState = mCM.getState();
                    if (phoneState == PhoneConstants.State.OFFHOOK &&
                        !isHeadsetPlugged() && !isBluetoothHeadsetAudioOn()) {
                        PhoneUtils.turnOnSpeaker(getApplicationContext(), inDockMode, true);
                        updateInCallScreen();  // Has no effect if the InCallScreen isn't visible
                    }
                    break;

                case EVENT_TTY_PREFERRED_MODE_CHANGED:
                    // TTY mode is only applied if a headset is connected
                    int ttyMode;
                    if (isHeadsetPlugged()) {
                        ttyMode = mPreferredTtyMode;
                    } else {
                        ttyMode = Phone.TTY_MODE_OFF;
                    }
                    /// Gemini+ @{ google code:
                    // phone.setTTYMode(ttyMode, mHandler.obtainMessage(EVENT_TTY_MODE_SET));
                    setTTYMode(ttyMode);
                    /// @}
                    break;

                case EVENT_TTY_MODE_GET:
                    handleQueryTTYModeResponse(msg);
                    break;

                case EVENT_TTY_MODE_SET:
                    handleSetTTYModeResponse(msg);
                    break;

                case EVENT_TIMEOUT:
                    handleTimeout(msg.arg1);
                    break;

                case MESSAGE_SET_PREFERRED_NETWORK_TYPE:
                    AsyncResult ar = (AsyncResult) msg.obj;
                    Intent it = new Intent(NETWORK_MODE_CHANGE_RESPONSE);
                    if (ar.exception == null) {
                        it.putExtra(NETWORK_MODE_CHANGE_RESPONSE, true);
                        it.putExtra("NEW_NETWORK_MODE", msg.arg2);
                    } else {
                        it.putExtra(NETWORK_MODE_CHANGE_RESPONSE, false);
                        it.putExtra(OLD_NETWORK_MODE, msg.arg1);
                    }
                    sendBroadcast(it);
                    break;

                case EVENT_TOUCH_ANSWER_VT:
                    if (DBG) {
                        Log.d(LOG_TAG, "mHandler.handleMessage() : EVENT_TOUCH_ANSWER_VT");
                    }
                    try {
                        getInCallScreenInstance().internalAnswerCall();
                    } catch (Exception e) {
                        if (DBG) {
                            Log.d(LOG_TAG, "mHandler.handleMessage() : the InCallScreen Instance is null ," +
                                    " so cannot answer incoming VT call");
                        }
                    }
                    break;

                case EVENT_SHOW_INCALL_SCREEN_FOR_STK_SETUP_CALL:
                    PhoneUtils.showIncomingCallUi();
                    break;

                /// M: To trigger main thread looper for showing confirm dialog.
                case EVENT_TRIGGER_MAINTHREAD_LOOPER:
                  Log.d(LOG_TAG, "handle EVENT_TRIGGER_MAINTHREAD_LOOPER");
                  break;
            }
        }
    };

    public PhoneGlobals(Context context) {
        super(context);
        sMe = this;
    }

    public void onCreate() {
        if (VDBG) Log.v(LOG_TAG, "onCreate()...");

        String state = SystemProperties.get("vold.decrypt");

        if (!SystemProperties.getBoolean("gsm.phone.created", false)
                && ("".equals(state) || "trigger_restart_framework".equals(state))) {
            Log.d(LOG_TAG, "set System Property gsm.phone.created = true");
            SystemProperties.set("gsm.phone.created", "true");
            Settings.System.putLong(getApplicationContext().getContentResolver(),
                    Settings.System.SIM_LOCK_STATE_SETTING, 0x0L);
        }

        ContentResolver resolver = getContentResolver();

        // Cache the "voice capable" flag.
        // This flag currently comes from a resource (which is
        // overrideable on a per-product basis):
        sVoiceCapable =
                getResources().getBoolean(com.android.internal.R.bool.config_voice_capable);
        // ...but this might eventually become a PackageManager "system
        // feature" instead, in which case we'd do something like:
        // sVoiceCapable =
        //   getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY_VOICE_CALLS);

        if (phone == null) {
            Log.v(LOG_TAG, "onCreate(), start to make default phone");    
            // Initialize the telephony framework
            PhoneFactory.makeDefaultPhones(this);
            Log.v(LOG_TAG, "onCreate(), make default phone complete");
            // Get the default phone
            phone = PhoneFactory.getDefaultPhone();

            // Start TelephonyDebugService After the default phone is created.
            Intent intent = new Intent(this, TelephonyDebugService.class);
            startService(intent);

            /// M: @ { google code:
            // mCM = CallManager.getInstance();
            // mCM.registerPhone(phone);
            registerPhone();
            /// @}

            // Create the NotificationMgr singleton, which is used to display
            // status bar icons and control other status bar behavior.
            notificationMgr = NotificationMgr.init(this);

            Log.v(LOG_TAG, "onCreate(), start to new phone interface");

            phoneMgr = PhoneInterfaceManager.init(this, phone);

            phoneMgrEx = PhoneInterfaceManagerEx.init(this, phone);

            mHandler.sendEmptyMessage(EVENT_START_SIP_SERVICE);

            int phoneType = phone.getPhoneType();

            if (phoneType == PhoneConstants.PHONE_TYPE_CDMA) {
                // Create an instance of CdmaPhoneCallState and initialize it to IDLE
                cdmaPhoneCallState = new CdmaPhoneCallState();
                cdmaPhoneCallState.CdmaPhoneCallStateInit();
            }

            Log.v(LOG_TAG, "onCreate(), start to get BT default adapter");            

            if (BluetoothAdapter.getDefaultAdapter() != null) {
                // Start BluetoothPhoneService even if device is not voice capable.
                // The device can still support VOIP.
                startService(new Intent(this, BluetoothPhoneService.class));
                bindService(new Intent(this, BluetoothPhoneService.class),
                            mBluetoothPhoneConnection, 0);
            } else {
                // Device is not bluetooth capable
                mBluetoothPhone = null;
            }

            ringer = Ringer.init(this);

            // before registering for phone state changes
            mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            mWakeLock = mPowerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK
                        | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, LOG_TAG);

            mWakeLockForDisconnect = mPowerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK
                    | PowerManager.ON_AFTER_RELEASE, LOG_TAG);
   
            Log.v(LOG_TAG, "onCreate(), new partial wakelock");

            // lock used to keep the processor awake, when we don't care for the display.
            mPartialWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK
                    | PowerManager.ON_AFTER_RELEASE, LOG_TAG);
            // Wake lock used to control proximity sensor behavior.
            if (mPowerManager.isWakeLockLevelSupported(
                    PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) {
                mProximityWakeLock = mPowerManager.newWakeLock(
                        PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, LOG_TAG);
            }

            if (DBG) Log.d(LOG_TAG, "onCreate: mProximityWakeLock: " + mProximityWakeLock);

            // create mAccelerometerListener only if we are using the proximity sensor
            if (proximitySensorModeEnabled()) {
                mAccelerometerListener = new AccelerometerListener(this, this);
            }

            mKeyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);

            // get a handle to the service so that we can use it later when we
            // want to set the poke lock.
            mPowerManagerService = IPowerManager.Stub.asInterface(
                    ServiceManager.getService("power"));

            // Get UpdateLock to suppress system-update related events (e.g. dialog show-up)
            // during phone calls.
            mUpdateLock = new UpdateLock("phone");

            if (DBG) Log.d(LOG_TAG, "onCreate: mUpdateLock: " + mUpdateLock);

            // Create the CallController singleton, which is the interface
            // to the telephony layer for user-initiated telephony functionality
            // (like making outgoing calls.)
            callController = CallController.init(this);
            // ...and also the InCallUiState instance, used by the CallController to
            // keep track of some "persistent state" of the in-call UI.
            inCallUiState = InCallUiState.init(this);

            // Create the CallerInfoCache singleton, which remembers custom ring tone and
            // send-to-voicemail settings.
            //
            // The asynchronous caching will start just after this call.
            callerInfoCache = CallerInfoCache.init(this);

            // Create the CallNotifer singleton, which handles
            // asynchronous events from the telephony layer (like
            // launching the incoming-call UI when an incoming call comes
            // in.)
            Log.v(LOG_TAG, "onCreate(), new callnotifier");
            notifier = CallNotifier.init(this, phone, ringer, new CallLogAsync());

            // register for MMI/USSD
            /// M:Gemini+ @{ google code:
            // mCM.registerForMmiComplete(mHandler, MMI_COMPLETE, null);
            if (phoneType == PhoneConstants.PHONE_TYPE_GSM) {
                Object callManager = getCallManager();
                GeminiRegister.registerForMmiComplete(callManager, mHandler, MMI_COMPLETE_GEMINI);
                GeminiRegister.registerForMmiInitiate(callManager, mHandler, MMI_INITIATE_GEMINI);
            }

            if (FeatureOption.EVDO_DT_SUPPORT) {
                mCM.registerForMmiComplete(mHandler, MMI_INITIATE, null);
                mCM.registerForMmiComplete(mHandler, MMI_COMPLETE, null);
            }
            /// @}

            Log.v(LOG_TAG, "onCreate(), initialize connection handler");

            // register connection tracking to PhoneUtils
            PhoneUtils.initializeConnectionHandler(mCM);

            // Read platform settings for TTY feature
            if (PhoneUtils.isSupportFeature("TTY")) {
                mTtyEnabled = getResources().getBoolean(R.bool.tty_enabled);
            } else {
                mTtyEnabled = false;
            }

            Log.v(LOG_TAG, "onCreate(), new intentfilter");

            // Register for misc other intent broadcasts.
            IntentFilter intentFilter =
                    new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);
            intentFilter.addAction(Intent.ACTION_DUAL_SIM_MODE_CHANGED);
            intentFilter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
            intentFilter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
//            intentFilter.addAction(BluetoothHeadset.ACTION_STATE_CHANGED);
            intentFilter.addAction(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED);
            intentFilter.addAction(TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED);
            intentFilter.addAction(Intent.ACTION_HEADSET_PLUG);
            intentFilter.addAction(Intent.ACTION_DOCK_EVENT);
            intentFilter.addAction(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
            intentFilter.addAction(TelephonyIntents.ACTION_RADIO_TECHNOLOGY_CHANGED);
            intentFilter.addAction(TelephonyIntents.ACTION_SERVICE_STATE_CHANGED);
            intentFilter.addAction(TelephonyIntents.ACTION_EMERGENCY_CALLBACK_MODE_CHANGED);
            if (mTtyEnabled) {
                intentFilter.addAction(TtyIntent.TTY_PREFERRED_MODE_CHANGE_ACTION);
            }
            intentFilter.addAction(AudioManager.RINGER_MODE_CHANGED_ACTION);
            intentFilter.addAction(Intent.ACTION_SHUTDOWN);
            intentFilter.addAction(STKCALL_REGISTER_SPEECH_INFO);
            intentFilter.addAction(MISSEDCALL_DELETE_INTENT);
            intentFilter.addAction("out_going_call_to_phone_app");
            //Handle the network mode change for enhancement
            intentFilter.addAction(NETWORK_MODE_CHANGE);
            intentFilter.addAction("android.intent.action.ACTION_SHUTDOWN_IPO");
            intentFilter.addAction("android.intent.action.ACTION_PREBOOT_IPO");
            intentFilter.addAction(GeminiPhone.EVENT_3G_SWITCH_START_MD_RESET);
            intentFilter.addAction(TelephonyIntents.ACTION_RADIO_OFF);
            intentFilter.addAction(ACTION_MODEM_STATE);
            /// M: To trigger main thread looper for showing confirm dialog.
            intentFilter.addAction("TRIGGER_MAINTHREAD_LOOPER"); 

            // ECC button should be hidden when there is no service.
            intentFilter.addAction(SPN_STRINGS_UPDATED_ACTION);
            intentFilter.addAction("android.intent.action.normal.boot");
            registerReceiver(mReceiver, intentFilter);

            // Use a separate receiver for ACTION_MEDIA_BUTTON broadcasts,
            // since we need to manually adjust its priority (to make sure
            // we get these intents *before* the media player.)
            IntentFilter mediaButtonIntentFilter =
                    new IntentFilter(Intent.ACTION_MEDIA_BUTTON);
            // TODO verify the independent priority doesn't need to be handled thanks to the
            //  private intent handler registration
            // Make sure we're higher priority than the media player's
            // MediaButtonIntentReceiver (which currently has the default
            // priority of zero; see apps/Music/AndroidManifest.xml.)
            mediaButtonIntentFilter.setPriority(1);
            //
            registerReceiver(mMediaButtonReceiver, mediaButtonIntentFilter);
            // register the component so it gets priority for calls
            AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            am.registerMediaButtonEventReceiverForCalls(new ComponentName(this.getPackageName(),
                    MediaButtonBroadcastReceiver.class.getName()));

            //set the default values for the preferences in the phone.
            PreferenceManager.setDefaultValues(this, R.xml.network_setting, false);

            PreferenceManager.setDefaultValues(this, R.xml.call_feature_setting, false);

            // Make sure the audio mode (along with some
            // audio-mode-related state of our own) is initialized
            // correctly, given the current state of the phone.
            PhoneUtils.setAudioMode(mCM);
        }

        if (TelephonyCapabilities.supportsOtasp(phone)) {
            cdmaOtaProvisionData = new OtaUtils.CdmaOtaProvisionData();
            cdmaOtaConfigData = new OtaUtils.CdmaOtaConfigData();
            cdmaOtaScreenState = new OtaUtils.CdmaOtaScreenState();
            cdmaOtaInCallScreenUiState = new OtaUtils.CdmaOtaInCallScreenUiState();
        }

        // XXX pre-load the SimProvider so that it's ready
        resolver.getType(Uri.parse("content://icc/adn"));

        // start with the default value to set the mute state.
        mShouldRestoreMuteOnInCallResume = false;

        // TODO: Register for Cdma Information Records
        // phone.registerCdmaInformationRecord(mHandler, EVENT_UNSOL_CDMA_INFO_RECORD, null);

        // Read TTY settings and store it into BP NV.
        // AP owns (i.e. stores) the TTY setting in AP settings database and pushes the setting
        // to BP at power up (BP does not need to make the TTY setting persistent storage).
        // This way, there is a single owner (i.e AP) for the TTY setting in the phone.
        if (mTtyEnabled) {
            mPreferredTtyMode = android.provider.Settings.Secure.getInt(
                    phone.getContext().getContentResolver(),
                    android.provider.Settings.Secure.PREFERRED_TTY_MODE,
                    Phone.TTY_MODE_OFF);
            mHandler.sendMessage(mHandler.obtainMessage(EVENT_TTY_PREFERRED_MODE_CHANGED, 0));
        }
        // Read HAC settings and configure audio hardware
        if (getResources().getBoolean(R.bool.hac_enabled)) {
            int hac = android.provider.Settings.System.getInt(phone.getContext().getContentResolver(),
                                                              android.provider.Settings.System.HEARING_AID,
                                                              0);
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            audioManager.setParameter(CallFeaturesSetting.HAC_KEY, hac != 0 ?
                                      CallFeaturesSetting.HAC_VAL_ON :
                                      CallFeaturesSetting.HAC_VAL_OFF);
        }

        /**
         * Change Feature by mediatek .inc
         * description : initilize SimAssociateHandler
         */
        SimAssociateHandler.getInstance(this).prepair();
        SimAssociateHandler.getInstance(this).load();
        cellConnMgr = new CellConnMgr();
        cellConnMgr.register(getApplicationContext());
        /**
         * Change Feature by mediatek .inc end
         */

        /**
         * Change Feature by mediatek .inc
         * description : set the global flag that support dualtalk
         */
        if (FeatureOption.MTK_DT_SUPPORT) {
            DualTalkUtils.init();
        }
        /**
         * Change Feature by mediatek .inc end
         */

        // init SimInfoWrapper
        SIMInfoWrapper.getDefault().init(this);

        // init CallHistory
        CallHistoryDatabaseHelper.getInstance(this).initDatabase();

        // init PhoneNumberUtil
        PhoneNumberUtil.getInstance();

        // init HyphonManager for future use
        HyphonManager.getInstance().init(this);

        /// M: Check if the screen has soft navigation bar or not. @{
        // MTK add
        try {
            sHasNavigationBar = WindowManagerGlobal.getWindowManagerService().hasNavigationBar();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        /// @}

        Log.v(LOG_TAG, "onCreate(), exit.");
   }

    public void onConfigurationChanged(Configuration newConfig) {
        if (newConfig.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO) {
            mIsHardKeyboardOpen = true;
        } else {
            mIsHardKeyboardOpen = false;
        }

        // Update the Proximity sensor based on keyboard state
        updateProximitySensorMode(mCM.getState());
    }

    /**
     * Returns the singleton instance of the PhoneGlobals.
     */
    public static PhoneGlobals getInstance() {
        if (sMe == null) {
            throw new IllegalStateException("No PhoneGlobals here!");
        }
        return sMe;
    }

    /**
     * Returns the singleton instance of the PhoneGlobals if running as the
     * primary user, otherwise null.
     */
    static PhoneGlobals getInstanceIfPrimary() {
        return sMe;
    }

    /**
     * Returns the Phone associated with this instance
     */
    public static Phone getPhone() {
        return getInstance().phone;
    }

    Ringer getRinger() {
        return ringer;
    }

    IBluetoothHeadsetPhone getBluetoothPhoneService() {
        return mBluetoothPhone;
    }

    boolean isBluetoothHeadsetAudioOn() {
        return (mBluetoothHeadsetAudioState != BluetoothHeadset.STATE_AUDIO_DISCONNECTED);
    }

    /**
     * Returns an Intent that can be used to go to the "Call log"
     * UI (aka CallLogActivity) in the Contacts app.
     *
     * Watch out: there's no guarantee that the system has any activity to
     * handle this intent.  (In particular there may be no "Call log" at
     * all on on non-voice-capable devices.)
     */
    /* package */ static Intent createCallLogIntent() {
        Intent intent = new Intent(Intent.ACTION_VIEW, null);
        intent.setType("vnd.android.cursor.dir/calls");
        return intent;
    }

    /**
     * Return an Intent that can be used to bring up the in-call screen.
     *
     * This intent can only be used from within the Phone app, since the
     * InCallScreen is not exported from our AndroidManifest.
     */
    /* package */ static Intent createInCallIntent() {
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                | Intent.FLAG_ACTIVITY_NO_USER_ACTION
                | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        intent.setClassName("com.android.phone", getCallScreenClassName());

        // EXTRA_FORCE_SPEAKER_ON is only appended only at tablet 
        // or while the MTK_TB_APP_CALL_FORCE_SPEAKER_ON is true
        // Since there's no ear piece in tablet, speaker should be ON defaultly while call is placed
        if (FeatureOption.MTK_TB_APP_CALL_FORCE_SPEAKER_ON) {
          intent.putExtra(InCallScreen.EXTRA_FORCE_SPEAKER_ON, true);
        }

        return intent;
    }

    /**
     * Variation of createInCallIntent() that also specifies whether the
     * DTMF dialpad should be initially visible when the InCallScreen
     * comes up.
     */
    /* package */ static Intent createInCallIntent(boolean showDialpad) {
        Intent intent = createInCallIntent();
        intent.putExtra(InCallScreen.SHOW_DIALPAD_EXTRA, showDialpad);
        return intent;
    }

    /**
     * Returns PendingIntent for hanging up ongoing phone call. This will typically be used from
     * Notification context.
     */
    /* package */ static PendingIntent createHangUpOngoingCallPendingIntent(Context context) {
        Intent intent = new Intent(PhoneGlobals.ACTION_HANG_UP_ONGOING_CALL, null,
                context, NotificationBroadcastReceiver.class);
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    /* package */ static PendingIntent getCallBackPendingIntent(Context context, String number) {
        Intent intent = new Intent(ACTION_CALL_BACK_FROM_NOTIFICATION,
                Uri.fromParts(Constants.SCHEME_TEL, number, null),
                context, NotificationBroadcastReceiver.class);
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    /* package */ static PendingIntent getSendSmsFromNotificationPendingIntent(
            Context context, String number) {
        Intent intent = new Intent(ACTION_SEND_SMS_FROM_NOTIFICATION,
                Uri.fromParts(Constants.SCHEME_SMSTO, number, null),
                context, NotificationBroadcastReceiver.class);
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    /*package*/ static String getCallScreenClassName() {
        return InCallScreen.class.getName();
    }

    public void displayCallScreen(final boolean isVoiceOrVTCall) {
        displayCallScreen(isVoiceOrVTCall,false);
    }

    /**
     * Starts the InCallScreen Activity.
     */
    public void displayCallScreen(final boolean isVoiceOrVTCall, final boolean isForPlaceCall) {
        if (VDBG) Log.d(LOG_TAG, "displayCallScreen()...");

        // On non-voice-capable devices we shouldn't ever be trying to
        // bring up the InCallScreen in the first place.
        if (!sVoiceCapable) {
            Log.w(LOG_TAG, "displayCallScreen() not allowed: non-voice-capable device",
                  new Throwable("stack dump"));  // Include a stack trace since this warning
                                                 // indicates a bug in our caller
            return;
        }

        try {
            Intent intent = isVoiceOrVTCall ? createInCallIntent() : createVTInCallIntent();
        if (isForPlaceCall) {
         intent.putExtra("isForPlaceCall", true);
        }
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // It's possible that the in-call UI might not exist (like on
            // non-voice-capable devices), so don't crash if someone
            // accidentally tries to bring it up...
            Log.w(LOG_TAG, "displayCallScreen: transition to InCallScreen failed: " + e);
        }
        Profiler.callScreenRequested();
    }

    boolean isSimPinEnabled() {
        return mIsSimPinEnabled;
    }

    boolean authenticateAgainstCachedSimPin(String pin) {
        return (mCachedSimPin != null && mCachedSimPin.equals(pin));
    }

    void setCachedSimPin(String pin) {
        mCachedSimPin = pin;
    }

    void setInCallScreenInstance(InCallScreen inCallScreen) {
        mInCallScreen = inCallScreen;
    }

    void clearInCallScreenInstance(InCallScreen inCallScreen) {
        if (DBG) Log.d(LOG_TAG, "clearInCallScreenInstance(), inCallScreen = " + inCallScreen);
        // Here we need judge whether mInCallScreen is same as
        // inCallScreen because there may be 2 InCallScreen instance
        // exiting in some case even if InCallScreen activity is single instance.
        // if mInCallScreen != inCallScreen, that means another InCallScreen
        // is active, no need set mInCallScreen as null
        if (mInCallScreen == inCallScreen) {
            if (DBG) Log.d(LOG_TAG, "same InCallScreen instance");
            mInCallScreen = null;
        }
    }

    public InCallScreen getInCallScreenInstance() {
        return mInCallScreen;
    }

    /**
     * @return true if the in-call UI is running as the foreground
     * activity.  (In other words, from the perspective of the
     * InCallScreen activity, return true between onResume() and
     * onPause().)
     *
     * Note this method will return false if the screen is currently off,
     * even if the InCallScreen *was* in the foreground just before the
     * screen turned off.  (This is because the foreground activity is
     * always "paused" while the screen is off.)
     */
    boolean isShowingCallScreen() {
        if (mInCallScreen == null) return false;
        return mInCallScreen.isForegroundActivity();
    }

    /**
     * @return true if the in-call UI is running as the foreground activity, or,
     * it went to background due to screen being turned off. This might be useful
     * to determine if the in-call screen went to background because of other
     * activities, or its proximity sensor state or manual power-button press.
     *
     * Here are some examples.
     *
     * - If you want to know if the activity is in foreground or screen is turned off
     *   from the in-call UI (i.e. though it is not "foreground" anymore it will become
     *   so after screen being turned on), check
     *   {@link #isShowingCallScreenForProximity()} is true or not.
     *   {@link #updateProximitySensorMode(com.android.internal.telephony.PhoneConstants.State)} is
     *   doing this.
     *
     * - If you want to know if the activity is not in foreground just because screen
     *   is turned off (not due to other activity's interference), check
     *   {@link #isShowingCallScreen()} is false *and* {@link #isShowingCallScreenForProximity()}
     *   is true. InCallScreen#onDisconnect() is doing this check.
     *
     * @see #isShowingCallScreen()
     *
     * TODO: come up with better naming..
     */
    boolean isShowingCallScreenForProximity() {
        if (mInCallScreen == null) return false;
        return mInCallScreen.isForegroundActivityForProximity();
    }

    /**
     * Dismisses the in-call UI.
     *
     * This also ensures that you won't be able to get back to the in-call
     * UI via the BACK button (since this call removes the InCallScreen
     * from the activity history.)
     * For OTA Call, it call InCallScreen api to handle OTA Call End scenario
     * to display OTA Call End screen.
     */
    /* package */ void dismissCallScreen() {
        if (mInCallScreen != null) {
            if ((TelephonyCapabilities.supportsOtasp(phone)) &&
                    (mInCallScreen.isOtaCallInActiveState()
                    || mInCallScreen.isOtaCallInEndState()
                    || ((cdmaOtaScreenState != null)
                    && (cdmaOtaScreenState.otaScreenState
                            != CdmaOtaScreenState.OtaScreenState.OTA_STATUS_UNDEFINED)))) {
                // TODO: During OTA Call, display should not become dark to
                // allow user to see OTA UI update. Phone app needs to hold
                // a SCREEN_DIM_WAKE_LOCK wake lock during the entire OTA call.
                wakeUpScreen();
                // If InCallScreen is not in foreground we resume it to show the OTA call end screen
                // Fire off the InCallScreen intent
                displayCallScreen(true);

                mInCallScreen.handleOtaCallEnd();
                return;
            } else {
                mInCallScreen.finish();
            }
        } else {
            //Tells to finish incallscreen when it be resumed with Phone IDLE
            InCallUiState.sLastInCallScreenStatus = InCallUiState.INCALLSCREEN_NOT_EXIT_NOT_INIT;
        }
    }

    /**
     * Handles OTASP-related events from the telephony layer.
     *
     * While an OTASP call is active, the CallNotifier forwards
     * OTASP-related telephony events to this method.
     */
    void handleOtaspEvent(Message msg) {
        if (DBG) Log.d(LOG_TAG, "handleOtaspEvent(message " + msg + ")...");

        if (otaUtils == null) {
            // We shouldn't be getting OTASP events without ever
            // having started the OTASP call in the first place!
            Log.w(LOG_TAG, "handleOtaEvents: got an event but otaUtils is null! "
                  + "message = " + msg);
            return;
        }

        otaUtils.onOtaProvisionStatusChanged((AsyncResult) msg.obj);
    }

    /**
     * Similarly, handle the disconnect event of an OTASP call
     * by forwarding it to the OtaUtils instance.
     */
    /* package */ void handleOtaspDisconnect() {
        if (DBG) Log.d(LOG_TAG, "handleOtaspDisconnect()...");

        if (otaUtils == null) {
            // We shouldn't be getting OTASP events without ever
            // having started the OTASP call in the first place!
            Log.w(LOG_TAG, "handleOtaspDisconnect: otaUtils is null!");
            return;
        }

        otaUtils.onOtaspDisconnect();
    }

    /**
     * Sets the activity responsible for un-PUK-blocking the device
     * so that we may close it when we receive a positive result.
     * mPUKEntryActivity is also used to indicate to the device that
     * we are trying to un-PUK-lock the phone. In other words, iff
     * it is NOT null, then we are trying to unlock and waiting for
     * the SIM to move to READY state.
     *
     * @param activity is the activity to close when PUK has
     * finished unlocking. Can be set to null to indicate the unlock
     * or SIM READYing process is over.
     */
    void setPukEntryActivity(Activity activity) {
        mPUKEntryActivity = activity;
    }

    Activity getPUKEntryActivity() {
        return mPUKEntryActivity;
    }

    /**
     * Sets the dialog responsible for notifying the user of un-PUK-
     * blocking - SIM READYing progress, so that we may dismiss it
     * when we receive a positive result.
     *
     * @param dialog indicates the progress dialog informing the user
     * of the state of the device.  Dismissed upon completion of
     * READYing process
     */
    void setPukEntryProgressDialog(ProgressDialog dialog) {
        mPUKEntryProgressDialog = dialog;
    }

    ProgressDialog getPUKEntryProgressDialog() {
        return mPUKEntryProgressDialog;
    }

    /**
     * Controls whether or not the screen is allowed to sleep.
     *
     * Once sleep is allowed (WakeState is SLEEP), it will rely on the
     * settings for the poke lock to determine when to timeout and let
     * the device sleep {@link PhoneGlobals#setScreenTimeout}.
     *
     * @param ws tells the device to how to wake.
     */
    public void requestWakeState(WakeState ws) {
        if (VDBG) Log.d(LOG_TAG, "requestWakeState(" + ws + ")...");
        synchronized (this) {
            if (mWakeState != ws) {
                switch (ws) {
                    case PARTIAL:
                        // acquire the processor wake lock, and release the FULL
                        // lock if it is being held.
                        mPartialWakeLock.acquire();
                        if (mWakeLock.isHeld()) {
                            mWakeLock.release();
                        }
                        break;
                    case FULL:
                        // acquire the full wake lock, and release the PARTIAL
                        // lock if it is being held.
                        mWakeLock.acquire();
                        if (mPartialWakeLock.isHeld()) {
                            mPartialWakeLock.release();
                        }
                        break;
                    case SLEEP:
                    default:
                        // release both the PARTIAL and FULL locks.
                        if (mWakeLock.isHeld()) {
                            mWakeLock.release();
                        }
                        if (mPartialWakeLock.isHeld()) {
                            mPartialWakeLock.release();
                        }
                        break;
                }
                mWakeState = ws;
            }
        }
    }

    /**
     * If we are not currently keeping the screen on, then poke the power
     * manager to wake up the screen for the user activity timeout duration.
     */
    /* package */ void wakeUpScreen() {
        synchronized (this) {
            if (mWakeState == WakeState.SLEEP) {
                if (DBG) Log.d(LOG_TAG, "pulse screen lock");
                mPowerManager.wakeUp(SystemClock.uptimeMillis());
            }
        }
    }

    void wakeUpScreenForDisconnect(int holdMs) {
        synchronized (this) {
            if (mWakeState == WakeState.SLEEP && !mPowerManager.isScreenOn()) { 
                if (DBG) Log.d(LOG_TAG, "wakeUpScreenForDisconnect(" + holdMs + ")");
                mWakeLockForDisconnect.acquire();
                mHandler.removeMessages(EVENT_TIMEOUT);
                mWakelockSequence++;
                Message msg = mHandler.obtainMessage(EVENT_TIMEOUT, mWakelockSequence, 0);
                mHandler.sendMessageDelayed(msg, holdMs);
            }
        }
    }
    
    void handleTimeout(int seq) {
        synchronized (this) {
            if (DBG) Log.d(LOG_TAG, "handleTimeout");
            if (seq == mWakelockSequence) {
                mWakeLockForDisconnect.release();
            }
        }
    }
    /**
     * Sets the wake state and screen timeout based on the current state
     * of the phone, and the current state of the in-call UI.
     *
     * This method is a "UI Policy" wrapper around
     * {@link PhoneGlobals#requestWakeState} and {@link PhoneGlobals#setScreenTimeout}.
     *
     * It's safe to call this method regardless of the state of the Phone
     * (e.g. whether or not it's idle), and regardless of the state of the
     * Phone UI (e.g. whether or not the InCallScreen is active.)
     */
    /* package */ void updateWakeState() {
        PhoneConstants.State state = mCM.getState();

        // True if the in-call UI is the foreground activity.
        // (Note this will be false if the screen is currently off,
        // since in that case *no* activity is in the foreground.)
        boolean isShowingCallScreen = isShowingCallScreen();

        // True if the InCallScreen's DTMF dialer is currently opened.
        // (Note this does NOT imply whether or not the InCallScreen
        // itself is visible.)
        boolean isDialerOpened = (mInCallScreen != null) && mInCallScreen.isDialerOpened();

        // True if the speakerphone is in use.  (If so, we *always* use
        // the default timeout.  Since the user is obviously not holding
        // the phone up to his/her face, we don't need to worry about
        // false touches, and thus don't need to turn the screen off so
        // aggressively.)
        // Note that we need to make a fresh call to this method any
        // time the speaker state changes.  (That happens in
        // PhoneUtils.turnOnSpeaker().)
        boolean isSpeakerInUse = (state == PhoneConstants.State.OFFHOOK) && PhoneUtils.isSpeakerOn(this);

        // TODO (bug 1440854): The screen timeout *might* also need to
        // depend on the bluetooth state, but this isn't as clear-cut as
        // the speaker state (since while using BT it's common for the
        // user to put the phone straight into a pocket, in which case the
        // timeout should probably still be short.)

        if (DBG) Log.d(LOG_TAG, "updateWakeState: callscreen " + isShowingCallScreen
                       + ", dialer " + isDialerOpened
                       + ", speaker " + isSpeakerInUse + "...");

        //
        // Decide whether to force the screen on or not.
        //
        // Force the screen to be on if the phone is ringing or dialing,
        // or if we're displaying the "Call ended" UI for a connection in
        // the "disconnected" state.
        // However, if the phone is disconnected while the user is in the
        // middle of selecting a quick response message, we should not force
        // the screen to be on.
        //
        boolean isRinging = (state == PhoneConstants.State.RINGING);
        boolean isDialing = GeminiUtils.isDialing(phone);
        boolean showingQuickResponseDialog = (mInCallScreen != null) &&
                mInCallScreen.isQuickResponseDialogShowing();
        boolean showingDisconnectedConnection =
                PhoneUtils.hasDisconnectedConnections(mCM) && isShowingCallScreen;
        boolean keepScreenOn = isRinging || isDialing ||
                (showingDisconnectedConnection && !showingQuickResponseDialog);
        if (DBG) Log.d(LOG_TAG, "updateWakeState: keepScreenOn = " + keepScreenOn
                       + " (isRinging " + isRinging
                       + ", isDialing " + isDialing
                       + ", showingQuickResponse " + showingQuickResponseDialog
                       + ", showingDisc " + showingDisconnectedConnection + ")");
        // keepScreenOn == true means we'll hold a full wake lock:
        requestWakeState(keepScreenOn ? WakeState.FULL : WakeState.SLEEP);
    }

    /**
     * Manually pokes the PowerManager's userActivity method.  Since we
     * set the {@link WindowManager.LayoutParams#INPUT_FEATURE_DISABLE_USER_ACTIVITY}
     * flag while the InCallScreen is active when there is no proximity sensor,
     * we need to do this for touch events that really do count as user activity
     * (like pressing any onscreen UI elements.)
     */
    /* package */ void pokeUserActivity() {
        if (VDBG) Log.d(LOG_TAG, "pokeUserActivity()...");
        mPowerManager.userActivity(SystemClock.uptimeMillis(), false);
    }

    /**
     * Set when a new outgoing call is beginning, so we can update
     * the proximity sensor state.
     * Cleared when the InCallScreen is no longer in the foreground,
     * in case the call fails without changing the telephony state.
     */
    /* package */ void setBeginningCall(boolean beginning) {
        // Note that we are beginning a new call, for proximity sensor support
        mBeginningCall = beginning;
        // Update the Proximity sensor based on mBeginningCall state
        updateProximitySensorMode(mCM.getState());
    }

    /**
     * Updates the wake lock used to control proximity sensor behavior,
     * based on the current state of the phone.  This method is called
     * from the CallNotifier on any phone state change.
     *
     * On devices that have a proximity sensor, to avoid false touches
     * during a call, we hold a PROXIMITY_SCREEN_OFF_WAKE_LOCK wake lock
     * whenever the phone is off hook.  (When held, that wake lock causes
     * the screen to turn off automatically when the sensor detects an
     * object close to the screen.)
     *
     * This method is a no-op for devices that don't have a proximity
     * sensor.
     *
     * Note this method doesn't care if the InCallScreen is the foreground
     * activity or not.  That's because we want the proximity sensor to be
     * enabled any time the phone is in use, to avoid false cheek events
     * for whatever app you happen to be running.
     *
     * Proximity wake lock will *not* be held if any one of the
     * conditions is true while on a call:
     * 1) If the audio is routed via Bluetooth
     * 2) If a wired headset is connected
     * 3) if the speaker is ON
     * 4) If the slider is open(i.e. the hardkeyboard is *not* hidden)
     *
     * @param state current state of the phone (see {@link Phone#State})
     */
    /* package */ void updateProximitySensorMode(PhoneConstants.State state) {
    
        boolean isRingingWhenActive = false;//MTK81281 add isRingingWhenActive for Cr:ALPS00117091
        
        if (VDBG) Log.d(LOG_TAG, "updateProximitySensorMode: state = " + state);

        if (proximitySensorModeEnabled()) {
            synchronized (mProximityWakeLock) {
                // turn proximity sensor off and turn screen on immediately if
                // we are using a headset, the keyboard is open, or the device
                // is being held in a horizontal position.
                boolean screenOnImmediately = (isHeadsetPlugged()
                                               || PhoneUtils.isSpeakerOn(this)
                                               || isBluetoothHeadsetAudioOn()
                                               || mIsHardKeyboardOpen);

                if (FeatureOption.MTK_VT3G324M_SUPPORT) {
                    screenOnImmediately = screenOnImmediately ||
                            ((!VTCallUtils.isVTIdle()) && (!VTCallUtils.isVTRinging()));
                }

                // We do not keep the screen off when the user is outside in-call screen and we are
                // horizontal, but we do not force it on when we become horizontal until the
                // proximity sensor goes negative.
                
                // this horizontal is not the same portrait.
                 boolean horizontal =
                        (mOrientation == AccelerometerListener.ORIENTATION_HORIZONTAL);
                 screenOnImmediately |= !isShowingCallScreenForProximity() && horizontal;
                if (VDBG) Log.d(LOG_TAG, "updateProximitySensorMode: mBeginningCall = " + mBeginningCall);
                if (VDBG) Log.d(LOG_TAG, "updateProximitySensorMode: screenOnImmediately = " + screenOnImmediately);
           //MTK81281 add isRingingWhenActive for Cr:ALPS00117091 start    
           //when a call is activeand p-sensor turn off the screen,  
           //another call or vtcall in we don't release the lock and acquire again
           //(the prowermanagerservice will turn on and off the screen and it's a problem)
           //instead ,we don't release the lock(prowermanagerservice will not turn on and off the screen)
                isRingingWhenActive = (state == PhoneConstants.State.RINGING)
                    && (mCM.getActiveFgCallState() == Call.State.ACTIVE)
                    && (mCM.getFirstActiveRingingCall().getState() == Call.State.WAITING);

                if (VDBG) Log.d(LOG_TAG, "updateProximitySensorMode: isRingingWhenActive = " + isRingingWhenActive);
           //MTK81281 add  isRingingWhenActive for Cr:ALPS00117091 end

                //MTK81281 add isRingingWhenActive for Cr:ALPS00117091
                if (((state == PhoneConstants.State.OFFHOOK) || mBeginningCall || isRingingWhenActive)
                        && !screenOnImmediately) {
                    // Phone is in use!  Arrange for the screen to turn off
                    // automatically when the sensor detects a close object.
                    if (!mProximityWakeLock.isHeld()) {
                        if (DBG) Log.d(LOG_TAG, "updateProximitySensorMode: acquiring...");
                        mProximityWakeLock.acquire();
                    } else {
                        if (VDBG) Log.d(LOG_TAG, "updateProximitySensorMode: lock already held.");
                    }
                } else {
                    // Phone is either idle, or ringing.  We don't want any
                    // special proximity sensor behavior in either case.
                    if (mProximityWakeLock.isHeld()) {
                        if (DBG) Log.d(LOG_TAG, "updateProximitySensorMode: releasing...");
                        // Wait until user has moved the phone away from his head if we are
                        // releasing due to the phone call ending.
                        // Qtherwise, turn screen on immediately
                        int flags =
                            (screenOnImmediately ? 0 : PowerManager.WAIT_FOR_PROXIMITY_NEGATIVE);
                        mProximityWakeLock.release(flags);
                    } else {
                        if (VDBG) {
                            Log.d(LOG_TAG, "updateProximitySensorMode: lock already released.");
                        }
                    }
                }
            }
        }
    }

    @Override
    public void orientationChanged(int orientation) {
        mOrientation = orientation;
        updateProximitySensorMode(mCM.getState());
    }

    /**
     * Notifies the phone app when the phone state changes.
     *
     * This method will updates various states inside Phone app (e.g. proximity sensor mode,
     * accelerometer listener state, update-lock state, etc.)
     */
    /* package */ void updatePhoneState(PhoneConstants.State state) {
        if (state != mLastPhoneState) {
            mLastPhoneState = state;
            if (state == PhoneConstants.State.IDLE)
                PhoneGlobals.getInstance().pokeUserActivity();
            updateProximitySensorMode(state);

            // Try to acquire or release UpdateLock.
            //
            // Watch out: we don't release the lock here when the screen is still in foreground.
            // At that time InCallScreen will release it on onPause().
            if (state != PhoneConstants.State.IDLE) {
                // UpdateLock is a recursive lock, while we may get "acquire" request twice and
                // "release" request once for a single call (RINGING + OFFHOOK and IDLE).
                // We need to manually ensure the lock is just acquired once for each (and this
                // will prevent other possible buggy situations too).
                if (!mUpdateLock.isHeld()) {
                    mUpdateLock.acquire();
                }
            } else {
                if (!isShowingCallScreen()) {
                    if (!mUpdateLock.isHeld()) {
                        mUpdateLock.release();
                    }
                } else {
                    // For this case InCallScreen will take care of the release() call.
                }
            }

            if (mAccelerometerListener != null) {
                // use accelerometer to augment proximity sensor when in call
                mOrientation = AccelerometerListener.ORIENTATION_UNKNOWN;
                mAccelerometerListener.enable(state == PhoneConstants.State.OFFHOOK);
            }
            // clear our beginning call flag
            mBeginningCall = false;
            // While we are in call, the in-call screen should dismiss the keyguard.
            // This allows the user to press Home to go directly home without going through
            // an insecure lock screen.
            // But we do not want to do this if there is no active call so we do not
            // bypass the keyguard if the call is not answered or declined.
            if (mInCallScreen != null) {
        if (VDBG) Log.d(LOG_TAG, "updatePhoneState: state = " + state);
        if (!PhoneUtils.isDMLocked())
                    mInCallScreen.updateKeyguardPolicy(state == PhoneConstants.State.OFFHOOK);
            }
        }
    }

    /* package */ PhoneConstants.State getPhoneState() {
        return mLastPhoneState;
    }

    /**
     * Returns UpdateLock object.
     */
    /* package */ UpdateLock getUpdateLock() {
        return mUpdateLock;
    }

    /**
     * @return true if this device supports the "proximity sensor
     * auto-lock" feature while in-call (see updateProximitySensorMode()).
     */
    /* package */ boolean proximitySensorModeEnabled() {
        return (mProximityWakeLock != null);
    }

    KeyguardManager getKeyguardManager() {
        return mKeyguardManager;
    }

    private void onMMIComplete(AsyncResult r, int slotId) {
        if (VDBG) Log.d(LOG_TAG, "onMMIComplete()...");
        MmiCode mmiCode = (MmiCode) r.result;
        if (null == mmiCode) {
            return;
        }
        MmiCode.State state = mmiCode.getState();
        if (GeminiUtils.isGeminiSupport()) {
            if (state != MmiCode.State.PENDING) {
                Intent intent = new Intent();
                intent.setAction("com.android.phone.mmi");
                sendBroadcast(intent);
            }
        }
        PhoneUtils.displayMMICompleteExt(phone, getApplicationContext(), mmiCode, null, null, slotId);
    }

    private void initForNewRadioTechnology() {
        if (DBG) Log.d(LOG_TAG, "initForNewRadioTechnology...");

         if (phone.getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA) {
            // Create an instance of CdmaPhoneCallState and initialize it to IDLE
            cdmaPhoneCallState = new CdmaPhoneCallState();
            cdmaPhoneCallState.CdmaPhoneCallStateInit();
        }
        if (TelephonyCapabilities.supportsOtasp(phone)) {
            //create instances of CDMA OTA data classes
            if (cdmaOtaProvisionData == null) {
                cdmaOtaProvisionData = new OtaUtils.CdmaOtaProvisionData();
            }
            if (cdmaOtaConfigData == null) {
                cdmaOtaConfigData = new OtaUtils.CdmaOtaConfigData();
            }
            if (cdmaOtaScreenState == null) {
                cdmaOtaScreenState = new OtaUtils.CdmaOtaScreenState();
            }
            if (cdmaOtaInCallScreenUiState == null) {
                cdmaOtaInCallScreenUiState = new OtaUtils.CdmaOtaInCallScreenUiState();
            }
        } else {
            //Clean up OTA data in GSM/UMTS. It is valid only for CDMA
            clearOtaState();
        }

        ringer.updateRingerContextAfterRadioTechnologyChange(this.phone);
        notifier.updateCallNotifierRegistrationsAfterRadioTechnologyChange();
        if (mBluetoothPhone != null) {
            try {
                mBluetoothPhone.updateBtHandsfreeAfterRadioTechnologyChange();
            } catch (RemoteException e) {
                Log.e(LOG_TAG, Log.getStackTraceString(new Throwable()));
            }
        }
        if (mInCallScreen != null) {
            mInCallScreen.updateAfterRadioTechnologyChange();
        }
    }


    /**
     * @return true if a wired headset is currently plugged in.
     *
     * @see Intent.ACTION_HEADSET_PLUG (which we listen for in mReceiver.onReceive())
     */
    public boolean isHeadsetPlugged() {
        return mIsHeadsetPlugged;
    }

    /**
     * @return true if the onscreen UI should currently be showing the
     * special "bluetooth is active" indication in a couple of places (in
     * which UI elements turn blue and/or show the bluetooth logo.)
     *
     * This depends on the BluetoothHeadset state *and* the current
     * telephony state; see shouldShowBluetoothIndication().
     *
     * @see CallCard
     * @see NotificationMgr.updateInCallNotification
     */
    /* package */ boolean showBluetoothIndication() {
        return mShowBluetoothIndication;
    }

    /**
     * Recomputes the mShowBluetoothIndication flag based on the current
     * bluetooth state and current telephony state.
     *
     * This needs to be called any time the bluetooth headset state or the
     * telephony state changes.
     *
     * @param forceUiUpdate if true, force the UI elements that care
     *                      about this flag to update themselves.
     */
    /* package */ void updateBluetoothIndication(boolean forceUiUpdate) {
        mShowBluetoothIndication = shouldShowBluetoothIndication(mBluetoothHeadsetState,
                                                                 mBluetoothHeadsetAudioState,
                                                                 mCM);
        if (forceUiUpdate) {
            // Post Handler messages to the various components that might
            // need to be refreshed based on the new state.
            if (isShowingCallScreen()) {
                /// M: Call the new requestUpdateBluetoothIndication(),
                /// and pass the BluetoothHeadsetAudioState @{
                mInCallScreen.requestUpdateBluetoothIndication(mBluetoothHeadsetAudioState);
                /// @}
            }
            if (DBG) Log.d(LOG_TAG, "- updating in-call notification for BT state change...");
            mHandler.sendEmptyMessage(EVENT_UPDATE_INCALL_NOTIFICATION);
        }

        // Update the Proximity sensor based on Bluetooth audio state
        updateProximitySensorMode(mCM.getState());
    }

    /**
     * UI policy helper function for the couple of places in the UI that
     * have some way of indicating that "bluetooth is in use."
     *
     * @return true if the onscreen UI should indicate that "bluetooth is in use",
     *         based on the specified bluetooth headset state, and the
     *         current state of the phone.
     * @see showBluetoothIndication()
     */
    private static boolean shouldShowBluetoothIndication(int bluetoothState,
                                                         int bluetoothAudioState,
                                                         CallManager cm) {
        // We want the UI to indicate that "bluetooth is in use" in two
        // slightly different cases:
        //
        // (a) The obvious case: if a bluetooth headset is currently in
        //     use for an ongoing call.
        //
        // (b) The not-so-obvious case: if an incoming call is ringing,
        //     and we expect that audio *will* be routed to a bluetooth
        //     headset once the call is answered.

        switch (cm.getState()) {
            case OFFHOOK:
                // This covers normal active calls, and also the case if
                // the foreground call is DIALING or ALERTING.  In this
                // case, bluetooth is considered "active" if a headset
                // is connected *and* audio is being routed to it.
                return ((bluetoothState == BluetoothHeadset.STATE_CONNECTED)
                        && (bluetoothAudioState == BluetoothHeadset.STATE_AUDIO_CONNECTED));

            case RINGING:
                // If an incoming call is ringing, we're *not* yet routing
                // audio to the headset (since there's no in-call audio
                // yet!)  In this case, if a bluetooth headset is
                // connected at all, we assume that it'll become active
                // once the user answers the phone.
                return (bluetoothState == BluetoothHeadset.STATE_CONNECTED);

            default:  // Presumably IDLE
                return false;
        }
    }


    /**
     * Receiver for misc intent broadcasts the Phone app cares about.
     */
    private class PhoneGlobalsBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ExtensionManager.getInstance().
                    getPhoneGlobalsBroadcastReceiverExtension().onReceive(context, intent)) {
                return;
            }
            String action = intent.getAction();
            if (VDBG) Log.d(LOG_TAG, "PhoneGlobalsBroadcastReceiver -----action=" + action);
            if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
                boolean enabled = intent.getBooleanExtra("state", false);
                if (VDBG) Log.d(LOG_TAG, "PhoneGlobalsBroadcastReceiver ------ AIRPLANEMODE enabled=" + enabled);
                if (enabled) {
                    PhoneUtils.dismissMMIDialog();
                }

                ///M: Solve [ALPS00409547][Rose][Free Test][Call]The ECC call cannot hang up after turn on airplane mode. @{
                try {
                   if (enabled && (mCM.getState() != PhoneConstants.State.IDLE)) {
                      Log.d(LOG_TAG, "Hangup all calls before turning on airplane mode");
                      mCM.hangupAllEx();
                   }
                } catch (CallStateException e) {
                    Log.e(LOG_TAG,
                            "CallStateException when mCM.hangupAllEx() in PhoneGlobalsBroadcastReceiver.onReceive(...).");
                }
                /// @}

                if (FeatureOption.MTK_FLIGHT_MODE_POWER_OFF_MD) {
                    if (GeminiUtils.isGeminiSupport()) {
                        if (enabled) {
                            ((GeminiPhone) phone).setRadioMode(GeminiNetworkSubUtil.MODE_POWER_OFF);
                        } else {
                            ((GeminiPhone) phone).setRadioPowerOn();
                        }
                    } else {
                        if (enabled) {
                            phone.setRadioPower(false, true);
                        } else {
                            phone.setRadioPowerOn();
                        }
                    }
                } else {
                    if (GeminiUtils.isGeminiSupport()) {
                        if (enabled) {
                            ((GeminiPhone)phone).setRadioMode(GeminiNetworkSubUtil.MODE_FLIGHT_MODE);
                        } else {
                            int dualSimModeSetting = System.getInt(getContentResolver(),
                                    System.DUAL_SIM_MODE_SETTING, GeminiNetworkSubUtil.MODE_DUAL_SIM);
                            ((GeminiPhone)phone).setRadioMode(dualSimModeSetting);
                        }
                    } else {
                        /* for consistent UI ,SIM Management for single sim project START */
                        if (!enabled) {
                            int simModeSetting = System.getInt(getContentResolver(),
                                    System.DUAL_SIM_MODE_SETTING, GeminiNetworkSubUtil.MODE_SIM1_ONLY);
                            if (simModeSetting == GeminiNetworkSubUtil.MODE_FLIGHT_MODE) {
                                if (VDBG) {
                                    Log.d(LOG_TAG,
                                            "Turn off airplane mode, but Radio still off due to sim mode setting is off");
                                }
                                enabled = true;
                            }
                        }
                        /* for consistent UI ,SIM Management for single sim project END */
                        phone.setRadioPower(!enabled);
                    }
                }
            } else if (action.equals(Intent.ACTION_DUAL_SIM_MODE_CHANGED)) {
                int mode = intent.getIntExtra(Intent.EXTRA_DUAL_SIM_MODE, GeminiNetworkSubUtil.MODE_DUAL_SIM);
                if (GeminiUtils.isGeminiSupport()) {
                    ((GeminiPhone) phone).setRadioMode(mode);
                } else {
                    boolean radioStatus = (0 == mode) ? false : true;
                    phone.setRadioPower(radioStatus);
                }
            } else if (action.equals(Intent.ACTION_CONFIGURATION_CHANGED)) {
                if (GeminiUtils.isGeminiSupport()) {
                    ((GeminiPhone) phone).refreshSpnDisplay();
                } else {
                    phone.refreshSpnDisplay();
                }
            } else if (action.equals(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)) {
                mBluetoothHeadsetState = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE,
                                                          BluetoothHeadset.STATE_DISCONNECTED);
                if (VDBG) Log.d(LOG_TAG, "mReceiver: HEADSET_STATE_CHANGED_ACTION");
                if (VDBG) Log.d(LOG_TAG, "==> new state: " + mBluetoothHeadsetState);
                updateBluetoothIndication(true);  // Also update any visible UI if necessary
            } else if (action.equals(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED)) {
                mBluetoothHeadsetAudioState =
                        intent.getIntExtra(BluetoothHeadset.EXTRA_STATE,
                                           BluetoothHeadset.STATE_AUDIO_DISCONNECTED);
                if (VDBG) Log.d(LOG_TAG, "mReceiver: HEADSET_AUDIO_STATE_CHANGED_ACTION");
                if (VDBG) Log.d(LOG_TAG, "==> new state: " + mBluetoothHeadsetAudioState);
                updateBluetoothIndication(true);  // Also update any visible UI if necessary
            } else if (action.equals(TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED)) {
                String state = intent.getStringExtra(PhoneConstants.STATE_KEY);
                String reason = intent.getStringExtra(PhoneConstants.STATE_CHANGE_REASON_KEY);
                if (VDBG) Log.d(LOG_TAG, "mReceiver: ACTION_ANY_DATA_CONNECTION_STATE_CHANGED state:" + state
                     + " reason:" + reason);

                /// M: Add for ALPS00555664 @{
                //     Skip if the apn type is not APN_TYPE_DEFAULT
                // MTK add
                String apnType = intent.getStringExtra(PhoneConstants.DATA_APN_TYPE_KEY);
                if (PhoneConstants.APN_TYPE_DEFAULT.equals(apnType)) {
                    Log.d(LOG_TAG, "APN Type default.");
                    // The "data disconnected due to roaming" notification is shown
                    // if (a) you have the "data roaming" feature turned off, and
                    // (b) you just lost data connectivity because you're roaming.
                    boolean disconnectedDueToRoaming = "DISCONNECTED".equals(state) &&
                            Phone.REASON_ROAMING_ON.equals(reason) && !phone.getDataRoamingEnabled();
                    //since getDataRoamingEnabled will access database, put it at last.

                    mHandler.sendEmptyMessage(disconnectedDueToRoaming
                                          ? EVENT_DATA_ROAMING_DISCONNECTED
                                          : EVENT_DATA_ROAMING_OK);
                }
                /// @}
            } else if (action.equals(Intent.ACTION_HEADSET_PLUG)) {
                if (VDBG) Log.d(LOG_TAG, "mReceiver: ACTION_HEADSET_PLUG");
                if (VDBG) Log.d(LOG_TAG, "    state: " + intent.getIntExtra("state", 0));
                if (VDBG) Log.d(LOG_TAG, "    name: " + intent.getStringExtra("name"));
                mIsHeadsetPlugged = (intent.getIntExtra("state", 0) == 1);
                mHandler.sendMessage(mHandler.obtainMessage(EVENT_WIRED_HEADSET_PLUG, 0));
            } else if (action.equals(TelephonyIntents.ACTION_SIM_STATE_CHANGED)) {
                // if an attempt to un-PUK-lock the device was made, while we're
                // receiving this state change notification, notify the handler.
                // NOTE: This is ONLY triggered if an attempt to un-PUK-lock has
                // been attempted.
                // below is MTK version
                int unlockSIMID = intent.getIntExtra(PhoneConstants.GEMINI_SIM_ID_KEY,-1);
                String unlockSIMStatus = intent.getStringExtra(IccCardConstants.INTENT_KEY_ICC_STATE);

                Log.d(LOG_TAG, "[unlock SIM card NO switched. Now] " + unlockSIMID + " is active.");
                Log.d(LOG_TAG, "[unlockSIMStatus] : "  + unlockSIMStatus);

                if (unlockSIMStatus.equals(IccCardConstants.INTENT_VALUE_ICC_READY)) {
                    int delaySendMessage = 2000;
                    mHandler.sendMessageDelayed(mHandler.obtainMessage(EVENT_SIM_STATE_CHANGED,
                            IccCardConstants.INTENT_VALUE_ICC_READY), delaySendMessage);
                } else {
                    Log.d(LOG_TAG, "[unlockSIMID : Other information]: " + unlockSIMStatus);
                }
            } else if (action.equals(TelephonyIntents.ACTION_RADIO_TECHNOLOGY_CHANGED)) {
                String newPhone = intent.getStringExtra(PhoneConstants.PHONE_NAME_KEY);
                Log.d(LOG_TAG, "Radio technology switched. Now " + newPhone + " is active.");
                initForNewRadioTechnology();
            } else if (action.equals(TelephonyIntents.ACTION_SERVICE_STATE_CHANGED)) {
                handleServiceStateChanged(intent);
            } else if (action.equals(TelephonyIntents.ACTION_EMERGENCY_CALLBACK_MODE_CHANGED)) {
                if (TelephonyCapabilities.supportsEcm(phone)) {
                    Log.d(LOG_TAG, "Emergency Callback Mode arrived in PhoneGlobals.");
                    // Start Emergency Callback Mode service
                    if (intent.getBooleanExtra("phoneinECMState", false)) {
                        context.startService(new Intent(context,
                                EmergencyCallbackModeService.class));
                    }
                } else {
                    // It doesn't make sense to get ACTION_EMERGENCY_CALLBACK_MODE_CHANGED
                    // on a device that doesn't support ECM in the first place.
                    Log.e(LOG_TAG, "Got ACTION_EMERGENCY_CALLBACK_MODE_CHANGED, "
                          + "but ECM isn't supported for phone: " + phone.getPhoneName());
                }
            } else if (action.equals(Intent.ACTION_DOCK_EVENT)) {
                mDockState = intent.getIntExtra(Intent.EXTRA_DOCK_STATE,
                        Intent.EXTRA_DOCK_STATE_UNDOCKED);
                if (VDBG) Log.d(LOG_TAG, "ACTION_DOCK_EVENT -> mDockState = " + mDockState);
                mHandler.sendMessage(mHandler.obtainMessage(EVENT_DOCK_STATE_CHANGED, 0));
            } else if (action.equals(TtyIntent.TTY_PREFERRED_MODE_CHANGE_ACTION)) {
                mPreferredTtyMode = intent.getIntExtra(TtyIntent.TTY_PREFFERED_MODE,
                                                       Phone.TTY_MODE_OFF);
                if (VDBG) Log.d(LOG_TAG, "mReceiver: TTY_PREFERRED_MODE_CHANGE_ACTION");
                if (VDBG) Log.d(LOG_TAG, "    mode: " + mPreferredTtyMode);
                mHandler.sendMessage(mHandler.obtainMessage(EVENT_TTY_PREFERRED_MODE_CHANGED, 0));
            } else if (action.equals(AudioManager.RINGER_MODE_CHANGED_ACTION)) {
                int ringerMode = intent.getIntExtra(AudioManager.EXTRA_RINGER_MODE,
                        AudioManager.RINGER_MODE_NORMAL);
                if (ringerMode == AudioManager.RINGER_MODE_SILENT) {
                    notifier.silenceRinger();
                }
            } else if (action.equals(Intent.ACTION_SHUTDOWN)) {
                Log.d(LOG_TAG, "ACTION_SHUTDOWN received");
                addCallSync();
                notifier.unregisterCallNotifierRegistrations();
            } else if (action.equals(STKCALL_REGISTER_SPEECH_INFO)) {
                PhoneUtils.placeCallRegister(phone);
                mHandler.sendEmptyMessageDelayed(EVENT_SHOW_INCALL_SCREEN_FOR_STK_SETUP_CALL,
                        DELAY_SHOW_INCALL_SCREEN_FOR_STK_SETUP_CALL);
            } else if (action.equals(MISSEDCALL_DELETE_INTENT)) {
                Log.d(LOG_TAG, "MISSEDCALL_DELETE_INTENT");
                notificationMgr.resetMissedCallNumber();
            } else if (action.equals(NETWORK_MODE_CHANGE)) {
                int modemNetworkMode = intent.getIntExtra(NETWORK_MODE_CHANGE, 0);
                int slotId = intent.getIntExtra(GeminiConstants.SLOT_ID_KEY, 0);
                int oldmode = intent.getIntExtra(OLD_NETWORK_MODE, -1);
                if (GeminiUtils.isGeminiSupport()) {
                    GeminiPhone dualPhone = (GeminiPhone)phone;
                    dualPhone.setPreferredNetworkTypeGemini(modemNetworkMode, mHandler
                            .obtainMessage(MESSAGE_SET_PREFERRED_NETWORK_TYPE, oldmode, modemNetworkMode), slotId);
                } else {
                    phone.setPreferredNetworkType(modemNetworkMode, mHandler
                            .obtainMessage(MESSAGE_SET_PREFERRED_NETWORK_TYPE, oldmode, modemNetworkMode));
                }
            } else if (action.equals("android.intent.action.ACTION_SHUTDOWN_IPO")) {
                Log.d(LOG_TAG, "ACTION_SHUTDOWN_IPO received");
                SystemProperties.set("gsm.ril.uicctype", "");
                SystemProperties.set("gsm.ril.uicctype.2", "");
                SystemProperties.set("ril.iccid.sim1", null);
                SystemProperties.set("ril.iccid.sim2", null);

                String bootReason = SystemProperties.get("sys.boot.reason");
                if ("1".equals(bootReason)) {
                    Log.d(LOG_TAG, "Alarm boot shutdown and not turn off radio again");
                } else {
                    phone.setRadioPower(false, true);
                }

                if (FeatureOption.MTK_VT3G324M_SUPPORT) {
                    if (VTManager.State.CLOSE != VTManager.getInstance().getState()) {
                        if (VDBG) Log.d(LOG_TAG,"- call VTManager onDisconnected ! ");
                        VTManager.getInstance().onDisconnected();
                        if (VDBG) Log.d(LOG_TAG,"- finish call VTManager onDisconnected ! ");
                        if (VDBG) Log.d(LOG_TAG,"- set VTManager close ! ");
                        VTManager.getInstance().setVTClose();
                        if (VDBG) Log.d(LOG_TAG,"- finish set VTManager close ! ");
                        if (VTInCallScreenFlags.getInstance().mVTInControlRes) {
                            sendBroadcast(new Intent(VTCallUtils.VT_CALL_END));
                            VTInCallScreenFlags.getInstance().mVTInControlRes = false;
                        }
                    }
                }
                if (null != inCallUiState) {
                    inCallUiState.clearState();
                }
                if (PhoneGlobals.this.mInCallScreen != null) {
                    PhoneGlobals.this.mInCallScreen.internalHangupAllCalls(mCM);
                }

                /// M: @ {
                // IPO shut down, cancel missed call notificaiton.
                if (notificationMgr != null) {
                    Log.d(LOG_TAG, "IPO Shutdown: call cancelMissedCallNotification()");
                    notificationMgr.cancelMissedCallNotification();
                }
                /// @}
            } else if (action.equals("android.intent.action.ACTION_PREBOOT_IPO")) {
                Log.d(LOG_TAG, "ACTION_PREBOOT_IPO received");
                Settings.System.putLong(getApplicationContext().getContentResolver(),
                        Settings.System.SIM_LOCK_STATE_SETTING, 0x0L);

                String bootReason = SystemProperties.get("sys.boot.reason");
                if ("1".equals(bootReason)) {
                    Log.d(LOG_TAG, "Alarm boot detected");
                } else {
                    Log.d(LOG_TAG, "IPO boot up detected");
                }
                phone.setRadioPowerOn();

                if (null != inCallUiState) {
                    inCallUiState.clearState();
                }

                /// M: @ {
                // Query missed call and show notification.
                // (LED will be shutdown when IPO shut down, re-send missed call
                // notification to notify LED useage.)
                if (notifier != null) {
                    Log.d(LOG_TAG, "IPO Reboot: call showMissedCallNotification()");
                    notifier.showMissedCallNotification(null, 0);
                }
                /// @}
            } else if (action.equals(GeminiPhone.EVENT_3G_SWITCH_START_MD_RESET)) {
                Log.d(LOG_TAG, "EVENT_3G_SWITCH_START_MD_RESET");
                Settings.System.putLong(getApplicationContext().getContentResolver(), Settings.System.SIM_LOCK_STATE_SETTING, 0x0L);
            } else if (action.equals(TelephonyIntents.ACTION_RADIO_OFF)) {
                int slot = intent.getIntExtra(TelephonyIntents.INTENT_KEY_ICC_SLOT, 0);
                Log.d(LOG_TAG, "ACTION_RADIO_OFF slot = " + slot);
                clearSimSettingFlag(slot);
                Log.i(LOG_TAG,"[xp Test][MODEM RESET]");
            } else if (action.equals(ACTION_MODEM_STATE)) {
                SystemService.start("md_minilog_util");
                /*int mdState = intent.getIntExtra("state", -1);
                Log.i(LOG_TAG, "Get MODEM STATE [" + mdState + "]");
                switch (mdState) {
                    case CCCI_MD_BROADCAST_EXCEPTION:
                        SystemService.start("md_minilog_util");
                        break;
                    case CCCI_MD_BROADCAST_RESET:
                        SystemService.start("md_minilog_util");
                        break;
                    case CCCI_MD_BROADCAST_READY:
                        SystemService.start("md_minilog_util");
                        break;
                    defaut:
                        SystemService.start("md_minilog_util");
                }*/
            } else if (action.equals("TRIGGER_MAINTHREAD_LOOPER")) {
                /// M: To trigger main thread looper for showing confirm dialog.
                Log.d(LOG_TAG, "TRIGGER_MAINTHREAD_LOOPER received");
                mHandler.sendEmptyMessage(EVENT_TRIGGER_MAINTHREAD_LOOPER);
            } else if (SPN_STRINGS_UPDATED_ACTION.equals(action)) {
                // ECC button should be hidden when there is no service.
                if (intent.getBooleanExtra(EXTRA_SHOW_PLMN, false)) {
                       String plmn = intent.getStringExtra(EXTRA_PLMN);
                       int simId = intent.getIntExtra(PhoneConstants.GEMINI_SIM_ID_KEY, PhoneConstants.GEMINI_SIM_1);
                       int index = simId - PhoneConstants.GEMINI_SIM_1;
                       Log.d(LOG_TAG, "[SPN_STRINGS_UPDATED_ACTION]index = " + index);
                       Log.d(LOG_TAG, "[SPN_STRINGS_UPDATED_ACTION]plmn = " + plmn);
                       if (index < 2) {
                        String noServiceStr = getResources().getText(
                                com.android.internal.R.string.lockscreen_carrier_default).toString();
                          mIsNoService[index] = ((plmn == null) || plmn.equals(noServiceStr));
                       }
                }
                /* End of SPN_STRINGS_UPDATED_ACTION */
            } else if ("android.intent.action.normal.boot".equals(action)) {
                Log.d(LOG_TAG, "receive alarm normal boot");
            }
        }
    }

    /**
     * Broadcast receiver for the ACTION_MEDIA_BUTTON broadcast intent.
     *
     * This functionality isn't lumped in with the other intents in
     * PhoneGlobalsBroadcastReceiver because we instantiate this as a totally
     * separate BroadcastReceiver instance, since we need to manually
     * adjust its IntentFilter's priority (to make sure we get these
     * intents *before* the media player.)
     */
    private class MediaButtonBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            KeyEvent event = (KeyEvent) intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (VDBG) Log.d(LOG_TAG,
                           "MediaButtonBroadcastReceiver.onReceive()...  event = " + event);
            //Not sure why add the ACTION_DOWN condition, but this will not answer the incomig call
            //so change the ACTION_DOWN to ACTION_UP (ALPS00287837)
            if ((event != null)
                && (event.getKeyCode() == KeyEvent.KEYCODE_HEADSETHOOK)
                && (event.getAction() == KeyEvent.ACTION_UP)) {

                if (event.getRepeatCount() == 0) {
                    // Mute ONLY on the initial keypress.
                    if (VDBG) Log.d(LOG_TAG, "MediaButtonBroadcastReceiver: HEADSETHOOK down!");
                    boolean consumed = PhoneUtils.handleHeadsetHook(phone, event);
                    if (VDBG) Log.d(LOG_TAG, "==> handleHeadsetHook(): consumed = " + consumed);
                    if (consumed) {
                        // If a headset is attached and the press is consumed, also update
                        // any UI items (such as an InCallScreen mute button) that may need to
                        // be updated if their state changed.
                        updateInCallScreen();  // Has no effect if the InCallScreen isn't visible
                        abortBroadcast();
                    }
                } else {
                    if (mCM.getState() != PhoneConstants.State.IDLE) {
                        // If the phone is anything other than completely idle,
                        // then we consume and ignore any media key events,
                        // Otherwise it is too easy to accidentally start
                        // playing music while a phone call is in progress.
                        if (VDBG) Log.d(LOG_TAG, "MediaButtonBroadcastReceiver: consumed");
                        abortBroadcast();
                    }
                }
            }
        }
    }

     /**
     * Accepts broadcast Intents which will be prepared by {@link NotificationMgr} and thus
     * sent from framework's notification mechanism (which is outside Phone context).
     * This should be visible from outside, but shouldn't be in "exported" state.
     *
     * TODO: If possible merge this into PhoneGlobalsBroadcastReceiver.
     */
    public static class NotificationBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // TODO: use "if (VDBG)" here.
            Log.d(LOG_TAG, "Broadcast from Notification: " + action);

            if (action.equals(ACTION_HANG_UP_ONGOING_CALL)) {
                PhoneUtils.hangup(PhoneGlobals.getInstance().mCM);
            } else if (action.equals(ACTION_CALL_BACK_FROM_NOTIFICATION)) {
                // Collapse the expanded notification and the notification item itself.
                closeSystemDialogs(context);
                clearMissedCallNotification(context);
                KeyguardManager km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
                if (km.isKeyguardLocked()) {
                    Log.d(LOG_TAG, "Disable keyguard!");
                    try {
                        IActivityManager am = ActivityManagerNative.asInterface(ServiceManager.getService("activity"));
                        am.dismissKeyguardOnNextActivity();
                    } catch (RemoteException e) {
                        Log.e(LOG_TAG, "RemoteException happened in NotificationBroadcastReceiver.onReceive().");
                    }
                } else {
                    Log.d(LOG_TAG, "Keyguard not enable!");
                }
                Intent callIntent = new Intent(Intent.ACTION_CALL_PRIVILEGED, intent.getData());
                callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                context.startActivity(callIntent);
            } else if (action.equals(ACTION_SEND_SMS_FROM_NOTIFICATION)) {
                // Collapse the expanded notification and the notification item itself.
                closeSystemDialogs(context);
                clearMissedCallNotification(context);

                Intent smsIntent = new Intent(Intent.ACTION_SENDTO, intent.getData());
                smsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ///M: add exception catch, for the case when SMS is disabled by
                // user, see ALPS00383231 @{
                //context.startActivity(smsIntent);
                try {
                    context.startActivity(smsIntent);
                } catch (ActivityNotFoundException e) {
                    Log.w(LOG_TAG, "start sms activity fail, sms is not available");
                }
                ///M: @}
            } else {
                Log.w(LOG_TAG, "Received hang-up request from notification,"
                        + " but there's no call the system can hang up.");
            }
        }

        private void closeSystemDialogs(Context context) {
            Intent intent = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            context.sendBroadcastAsUser(intent, UserHandle.ALL);
        }

        private void clearMissedCallNotification(Context context) {
            Intent clearIntent = new Intent(context, ClearMissedCallsService.class);
            clearIntent.setAction(ClearMissedCallsService.ACTION_CLEAR_MISSED_CALLS);
            context.startService(clearIntent);
        }
    }

    private void handleServiceStateChanged(Intent intent) {
        /**
         * This used to handle updating EriTextWidgetProvider this routine
         * and and listening for ACTION_SERVICE_STATE_CHANGED intents could
         * be removed. But leaving just in case it might be needed in the near
         * future.
         */

        // If service just returned, start sending out the queued messages
        ServiceState ss = ServiceState.newFromBundle(intent.getExtras());

        if (ss != null) {
            int state = ss.getState();
            notificationMgr.updateNetworkSelection(state, ss.getMySimId());
        }
    }

    public boolean isOtaCallInActiveState() {
        boolean otaCallActive = false;
        if (mInCallScreen != null) {
            otaCallActive = mInCallScreen.isOtaCallInActiveState();
        }
        if (VDBG) Log.d(LOG_TAG, "- isOtaCallInActiveState " + otaCallActive);
        return otaCallActive;
    }

    public boolean isOtaCallInEndState() {
        boolean otaCallEnded = false;
        if (mInCallScreen != null) {
            otaCallEnded = mInCallScreen.isOtaCallInEndState();
        }
        if (VDBG) Log.d(LOG_TAG, "- isOtaCallInEndState " + otaCallEnded);
        return otaCallEnded;
    }

    // it is safe to call clearOtaState() even if the InCallScreen isn't active
    public void clearOtaState() {
        if (DBG) Log.d(LOG_TAG, "- clearOtaState ...");
        if ((mInCallScreen != null)
                && (otaUtils != null)) {
            otaUtils.cleanOtaScreen(true);
            if (DBG) Log.d(LOG_TAG, "  - clearOtaState clears OTA screen");
        }
    }

    // it is safe to call dismissOtaDialogs() even if the InCallScreen isn't active
    public void dismissOtaDialogs() {
        if (DBG) Log.d(LOG_TAG, "- dismissOtaDialogs ...");
        if ((mInCallScreen != null)
                && (otaUtils != null)) {
            otaUtils.dismissAllOtaDialogs();
            if (DBG) Log.d(LOG_TAG, "  - dismissOtaDialogs clears OTA dialogs");
        }
    }

    // it is safe to call clearInCallScreenMode() even if the InCallScreen isn't active
    public void clearInCallScreenMode() {
        if (DBG) Log.d(LOG_TAG, "- clearInCallScreenMode ...");
        if (mInCallScreen != null) {
            mInCallScreen.resetInCallScreenMode();
        }
    }

    /**
     * Force the in-call UI to refresh itself, if it's currently visible.
     *
     * This method can be used any time there's a state change anywhere in
     * the phone app that needs to be reflected in the onscreen UI.
     *
     * Note that it's *not* necessary to manually refresh the in-call UI
     * (via this method) for regular telephony state changes like
     * DIALING -> ALERTING -> ACTIVE, since the InCallScreen already
     * listens for those state changes itself.
     *
     * This method does *not* force the in-call UI to come up if it's not
     * already visible.  To do that, use displayCallScreen().
     */
    /* package */ void updateInCallScreen() {
        if (DBG) Log.d(LOG_TAG, "- updateInCallScreen()...");
        if (mInCallScreen != null) {
            // Post an updateScreen() request.  Note that the
            // updateScreen() call will end up being a no-op if the
            // InCallScreen isn't the foreground activity.
            mInCallScreen.requestUpdateScreen();
        }
    }

    private void handleQueryTTYModeResponse(Message msg) {
        AsyncResult ar = (AsyncResult) msg.obj;
        if (ar.exception != null) {
            if (DBG) Log.d(LOG_TAG, "handleQueryTTYModeResponse: Error getting TTY state.");
        } else {
            if (DBG) Log.d(LOG_TAG,
                           "handleQueryTTYModeResponse: TTY enable state successfully queried.");
            //We will get the tty mode from the settings directly
            //int ttymode = ((int[]) ar.result)[0];
            int ttymode = Phone.TTY_MODE_OFF;
            if (isHeadsetPlugged()) {
                ttymode = mPreferredTtyMode;
            }
            if (DBG) Log.d(LOG_TAG, "handleQueryTTYModeResponse:ttymode=" + ttymode);

            Intent ttyModeChanged = new Intent(TtyIntent.TTY_ENABLED_CHANGE_ACTION);
            ttyModeChanged.putExtra("ttyEnabled", ttymode != Phone.TTY_MODE_OFF);
            sendBroadcastAsUser(ttyModeChanged, UserHandle.ALL);

            String audioTtyMode;
            switch (ttymode) {
            case Phone.TTY_MODE_FULL:
                audioTtyMode = "tty_full";
                break;
            case Phone.TTY_MODE_VCO:
                audioTtyMode = "tty_vco";
                break;
            case Phone.TTY_MODE_HCO:
                audioTtyMode = "tty_hco";
                break;
            case Phone.TTY_MODE_OFF:
            default:
                audioTtyMode = "tty_off";
                break;
            }
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            audioManager.setParameters("tty_mode="+audioTtyMode);
            PhoneUtils.setTtyMode(audioTtyMode);
        }
    }

    private void handleSetTTYModeResponse(Message msg) {
        AsyncResult ar = (AsyncResult) msg.obj;

        if (ar.exception != null) {
            if (DBG) Log.d (LOG_TAG,
                    "handleSetTTYModeResponse: Error setting TTY mode, ar.exception"
                    + ar.exception);
        }

       //Now Phone doesn't support ttymode query, so we make a fake response to trigger the set to audio
        //phone.queryTTYMode(mHandler.obtainMessage(EVENT_TTY_MODE_GET));
        Message m = mHandler.obtainMessage(EVENT_TTY_MODE_GET);
        m.obj = new AsyncResult(null, null, null);
        m.sendToTarget();
    }


    /**
     * "Call origin" may be used by Contacts app to specify where the phone call comes from.
     * Currently, the only permitted value for this extra is {@link #ALLOWED_EXTRA_CALL_ORIGIN}.
     * Any other value will be ignored, to make sure that malicious apps can't trick the in-call
     * UI into launching some random other app after a call ends.
     *
     * TODO: make this more generic. Note that we should let the "origin" specify its package
     * while we are now assuming it is "com.android.contacts"
     */
    public static final String EXTRA_CALL_ORIGIN = "com.android.phone.CALL_ORIGIN";
    private static final String DEFAULT_CALL_ORIGIN_PACKAGE = "com.android.contacts";
    private static final String ALLOWED_EXTRA_CALL_ORIGIN =
            "com.android.contacts.activities.DialtactsActivity";
    /**
     * Used to determine if the preserved call origin is fresh enough.
     */
    private static final long CALL_ORIGIN_EXPIRATION_MILLIS = 30 * 1000;

    public void setLatestActiveCallOrigin(String callOrigin) {
        inCallUiState.latestActiveCallOrigin = callOrigin;
        if (callOrigin != null) {
            inCallUiState.latestActiveCallOriginTimeStamp = SystemClock.elapsedRealtime();
        } else {
            inCallUiState.latestActiveCallOriginTimeStamp = 0;
        }
    }

    /**
     * Reset call origin depending on its timestamp.
     *
     * See if the current call origin preserved by the app is fresh enough or not. If it is,
     * previous call origin will be used as is. If not, call origin will be reset.
     *
     * This will be effective especially for 3rd party apps which want to bypass phone calls with
     * their own telephone lines. In that case Phone app may finish the phone call once and make
     * another for the external apps, which will drop call origin information in Intent.
     * Even in that case we are sure the second phone call should be initiated just after the first
     * phone call, so here we restore it from the previous information iff the second call is done
     * fairly soon.
     */
    public void resetLatestActiveCallOrigin() {
        final long callOriginTimestamp = inCallUiState.latestActiveCallOriginTimeStamp;
        final long currentTimestamp = SystemClock.elapsedRealtime();
        if (VDBG) {
            Log.d(LOG_TAG, "currentTimeMillis: " + currentTimestamp
                    + ", saved timestamp for call origin: " + callOriginTimestamp);
        }
        if (inCallUiState.latestActiveCallOriginTimeStamp > 0
                && (currentTimestamp - callOriginTimestamp < CALL_ORIGIN_EXPIRATION_MILLIS)) {
            if (VDBG) {
                Log.d(LOG_TAG, "Resume previous call origin (" +
                        inCallUiState.latestActiveCallOrigin + ")");
            }
            // Do nothing toward call origin itself but update the timestamp just in case.
            inCallUiState.latestActiveCallOriginTimeStamp = currentTimestamp;
        } else {
            if (VDBG) Log.d(LOG_TAG, "Drop previous call origin and set the current one to null");
            setLatestActiveCallOrigin(null);
        }
    }

    /**
     * @return Intent which will be used when in-call UI is shown and the phone call is hang up.
     * By default CallLog screen will be introduced, but the destination may change depending on
     * its latest call origin state.
     */
    public Intent createPhoneEndIntentUsingCallOrigin() {
        if (TextUtils.equals(inCallUiState.latestActiveCallOrigin, ALLOWED_EXTRA_CALL_ORIGIN)) {
            if (VDBG) Log.d(LOG_TAG, "Valid latestActiveCallOrigin("
                    + inCallUiState.latestActiveCallOrigin + ") was found. "
                    + "Go back to the previous screen.");
            // Right now we just launch the Activity which launched in-call UI. Note that we're
            // assuming the origin is from "com.android.contacts", which may be incorrect in the
            // future.
            final Intent intent = new Intent();
            intent.setClassName(DEFAULT_CALL_ORIGIN_PACKAGE, inCallUiState.latestActiveCallOrigin);
            return intent;
        } else {
            if (VDBG) Log.d(LOG_TAG, "Current latestActiveCallOrigin ("
                    + inCallUiState.latestActiveCallOrigin + ") is not valid. "
                    + "Just use CallLog as a default destination.");
            return PhoneGlobals.createCallLogIntent();
        }
    }

    /** Service connection */
    private final ServiceConnection mBluetoothPhoneConnection = new ServiceConnection() {

        /** Handle the task of binding the local object to the service */
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.i(LOG_TAG, "Headset phone created, binding local service.");
            mBluetoothPhone = IBluetoothHeadsetPhone.Stub.asInterface(service);
        }

        /** Handle the task of cleaning up the local binding */
        public void onServiceDisconnected(ComponentName className) {
            Log.i(LOG_TAG, "Headset phone disconnected, cleaning local binding.");
            mBluetoothPhone = null;
        }
    };

    public boolean isQVGA() {
        boolean retval = false;
        DisplayMetrics dm = new DisplayMetrics();
        WindowManager wm = (WindowManager)getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(dm);
        if ((dm.widthPixels == 320 && dm.heightPixels == 240) 
                || (dm.widthPixels == 240 && dm.heightPixels == 320)) {
            retval = true;
        }
        return retval;
    }

    /*void displayVTCallScreen() {
        if (VDBG) Log.d(LOG_TAG, "displayVTCallScreen()...");
        startActivity(createVTInCallIntent());
        Profiler.callScreenRequested();
    }*/

    static Intent createVTInCallIntent() {
        Intent intent = createInCallIntent();
        intent.putExtra(Constants.EXTRA_IS_VIDEO_CALL, true);
        return intent;
    }
    
    /*public boolean isVTIdle() {
        if (!FeatureOption.MTK_VT3G324M_SUPPORT) {
            return true;
        }
        
        if(PhoneConstants.State.IDLE == mCM.getState()) {
            return true;
        }
        
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            if (PhoneConstants.State.IDLE == ((GeminiPhone)phone).getState()) {
                return true;
            } else if (((GeminiPhone)phone).getForegroundCall().getState().isAlive()) {
                if(((GeminiPhone)phone).getForegroundCall().getLatestConnection().isVideo()) {
                    return false;
                }
            } else if (((GeminiPhone)phone).getRingingCall().getState().isAlive()) {
                if(((GeminiPhone)phone).getRingingCall().getLatestConnection().isVideo()) {
                    return false;
                }
            }
            return true;
        } else {
            if (PhoneConstants.State.IDLE == phone.getState()) {
                return true;
            } else if (phone.getForegroundCall().getState().isAlive()) {
                if (phone.getForegroundCall().getLatestConnection().isVideo()) {
                    return false;
                }
            } else if (phone.getRingingCall().getState().isAlive()) {
                if (phone.getRingingCall().getLatestConnection().isVideo()) {
                    return false;
                }
            }
            return true;
        }
    }
    
    public boolean isVTActive() {
        if (!FeatureOption.MTK_VT3G324M_SUPPORT) {
            return false;
        }
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            if (Call.State.ACTIVE == ((GeminiPhone)phone).getForegroundCall().getState()) {
                if (((GeminiPhone)phone).getForegroundCall().getLatestConnection().isVideo()) {
                    return true;
                }
            }
        } else {
            if (Call.State.ACTIVE == phone.getForegroundCall().getState()) {
                if (phone.getForegroundCall().getLatestConnection().isVideo()) {
                    return true;
                }
            }
        }
        return false;
    }
    
    public boolean isVTRinging() {
        if (true != FeatureOption.MTK_VT3G324M_SUPPORT) {
            return false;
        }
        if (PhoneConstants.State.RINGING != mCM.getState()) {
            return false;
        }
        if (true == FeatureOption.MTK_GEMINI_SUPPORT) {
            if (((GeminiPhone)phone).getRingingCall().getState().isRinging()) {
                if (((GeminiPhone)phone).getRingingCall().getLatestConnection().isVideo()) {
                    return true;
                }
            }
        } else {
            if (phone.getRingingCall().getState().isRinging()) {
                if (phone.getRingingCall().getLatestConnection().isVideo()) {
                return true;
                }
            }
        }
        
        return false;
    }*/
    
    public void touchAnswerVTCall() {

        if (DBG) {
            Log.d(LOG_TAG, "touchAnswerVTCall()");
        }

        if (getInCallScreenInstance() == null) {
            if (DBG) {
                Log.d(LOG_TAG,
                        "touchAnswerVTCall() : the InCallScreen Instance is null , so cannot answer incoming VT call");
            }
            return;
        }

        if (!VTCallUtils.isVTRinging()) {
            if (DBG) {
                Log.d(LOG_TAG, "touchAnswerVTCall() : there is no Ringing VT call , so return");
            }
            return;
        }

        mHandler.sendMessage(Message.obtain(mHandler, EVENT_TOUCH_ANSWER_VT));
    }

    //To judge whether current sim card need to unlock sim lock:default false
    public static boolean bNeedUnlockSIMLock(int iSIMNum) {
            GeminiPhone mGeminiPhone = (GeminiPhone)PhoneFactory.getDefaultPhone();
            if ((mGeminiPhone.getIccCardGemini(iSIMNum).getState() == IccCardConstants.State.PIN_REQUIRED) ||
                (mGeminiPhone.getIccCardGemini(iSIMNum).getState() == IccCardConstants.State.PUK_REQUIRED) ||
                (mGeminiPhone.getIccCardGemini(iSIMNum).getState() == IccCardConstants.State.NOT_READY)) {

                Log.d(LOG_TAG, "[bNeedUnlockSIMLock][NO Card/PIN/PUK]: " +  iSIMNum);
                return false;
            } else {
                return true;
            }
        
    }

    void addCallSync() {
        Call fgCall = mCM.getActiveFgCall();
        Call bgCall = mCM.getFirstActiveBgCall();
        
        List<Connection> connections = null;
        CallerInfo ci = null;
        int callType = Calls.OUTGOING_TYPE;
        int simId = GeminiUtils.getDefaultSlot();
        int isVideo = 0;
        /// M:Gemini+ @{
        // Slot Id and SIM Id are different
        if (GeminiUtils.isGeminiSupport()) {
            simId = GeminiUtils.getSlotNotIdle(phone);
            if (mInCallScreen != null) {
                SIMInfo simInfo = SIMInfo.getSIMInfoBySlot(mInCallScreen, simId);
                if (simInfo != null) {
                    simId = (int) simInfo.mSimId;
                }
            } else {
                simId = GeminiUtils.getDefaultSlot();
            }
        }
        /// @}

        if (fgCall.getState() != Call.State.IDLE) {
            connections = fgCall.getConnections();
            for (Connection c : connections) {
                if (c.isAlive()) {
                    ci = notifier.getCallerInfoFromConnection(c);
                    if (c.isIncoming())
                        callType = Calls.INCOMING_TYPE;
                    if (c.isVideo()) {
                        isVideo = 1;
                    } else {
                        isVideo = 0;
                    }
                    Calls.addCall(ci, mInCallScreen, c.getAddress(), notifier.getPresentation(c, ci),
                            callType, c.getCreateTime(), (int) (c.getDurationMillis() / 1000), simId, isVideo);// , false);
                }
            }
        }
        
        if (bgCall.getState() != Call.State.IDLE) {
            connections = bgCall.getConnections();
            for (Connection c : connections) {
                if (c.isAlive()) {
                    ci = notifier.getCallerInfoFromConnection(c);
                    if (c.isIncoming()) {
                        callType = Calls.INCOMING_TYPE;
                    }
                    if (c.isVideo()) {
                        isVideo = 1;
                    } else {
                        isVideo = 0;
                    }
                    Calls.addCall(ci, mInCallScreen, c.getAddress(), notifier.getPresentation(c, ci),
                            callType, c.getCreateTime(), (int) (c.getDurationMillis() / 1000), simId, isVideo);// ,false);
                }
            }
        }
    }

    private void clearSimSettingFlag(int slot) {

        Long bitSetMask = (0x3L << (2 * slot));

        Long simLockState = 0x0L;

        try {
            simLockState = Settings.System.getLong(getApplicationContext()
                    .getContentResolver(), Settings.System.SIM_LOCK_STATE_SETTING);

            simLockState = simLockState & (~bitSetMask);

            Settings.System.putLong(getApplicationContext().getContentResolver(),
                    Settings.System.SIM_LOCK_STATE_SETTING, simLockState);
        } catch (SettingNotFoundException e) {
            Log.e(LOG_TAG, "clearSimSettingFlag exception");
            e.printStackTrace();
        }
    }

    /* below are added by mediatek .inc */
    public CellConnMgr cellConnMgr;

    public Intent createPhoneEndIntent() {
        Intent intent = null;
        if (FeatureOption.MTK_BRAZIL_CUSTOMIZATION_VIVO) {
            intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            return intent;
        }

        if (TextUtils.equals(inCallUiState.latestActiveCallOrigin, ALLOWED_EXTRA_CALL_ORIGIN)) {
            if (VDBG) Log.d(LOG_TAG, "Valid latestActiveCallOrigin("
                    + inCallUiState.latestActiveCallOrigin + ") was found. "
                    + "Go back to the previous screen.");
            // Right now we just launch the Activity which launched in-call UI. Note that we're
            // assuming the origin is from "com.android.contacts", which may be incorrect in the
            // future.
            intent = new Intent();
            intent.setClassName(DEFAULT_CALL_ORIGIN_PACKAGE, inCallUiState.latestActiveCallOrigin);
            return intent;
        }

        return intent;
    }

    /**
     * Use to check if there is no service right now.
     * If the plmn in SPN_STRINGS_UPDATED_ACTION intent is null, it means that there is no service.
     *
     * @return true if there is no service, else return false.
     */
    public boolean isCurrentlyNoService() {
       // ECC button should be hidden when there is no service.
       Log.d(LOG_TAG, "[isCurrentlyNoService]mIsNoService[0] = " + mIsNoService[0]);
       Log.d(LOG_TAG, "[isCurrentlyNoService]mIsNoService[1] = " + mIsNoService[1]);
       return (mIsNoService[0] && mIsNoService[1]);
    }

    /** --------------------------------MTK------------------------------------------- */
    /// M:Gemini+ for support multi-sims @{
    public MTKCallManager mCMGemini = null;

    private static final int EVENT_SIM_NETWORK_LOCKED2 = 103;
    private static final int EVENT_SIM_NETWORK_LOCKED3 = 203;
    private static final int EVENT_SIM_NETWORK_LOCKED4 = 303;

    public static final int MMI_INITIATE2 = 151;
    public static final int MMI_COMPLETE2 = 152;
    public static final int MMI_CANCEL2 = 153;
    public static final int MMI_INITIATE3 = 251;
    public static final int MMI_COMPLETE3 = 252;
    public static final int MMI_CANCEL3 = 253;
    public static final int MMI_INITIATE4 = 351;
    public static final int MMI_COMPLETE4 = 352;
    public static final int MMI_CANCEL4 = 353;

    public static final int[] MMI_INITIATE_GEMINI = { MMI_INITIATE, MMI_INITIATE2, MMI_INITIATE3,
            MMI_INITIATE4 };
    public static final int[] MMI_COMPLETE_GEMINI = { MMI_COMPLETE, MMI_COMPLETE2, MMI_COMPLETE3,
            MMI_COMPLETE4 };
    public static final int[] MMI_CANCEL_GEMINI = { MMI_CANCEL, MMI_CANCEL2, MMI_CANCEL3,
            MMI_CANCEL4 };

    private void registerPhone() {
        mCM = CallManager.getInstance();
        if (GeminiUtils.isGeminiSupport()) {
            mCMGemini = MTKCallManager.getInstance();
            mCMGemini.registerPhoneGemini(phone);
        } else {
            mCM.registerPhone(phone);
        }
    }

    /**
     * If GEMINI, return MTKCallManager, else google default CallManager
     * 
     * @return
     */
    public Object getCallManager() {
        if (GeminiUtils.isGeminiSupport()) {
            return mCMGemini;
        }
        return mCM;
    }

    /**
     * set TTY mode for phone. {@link Phone#setTTYMode(int, Message)},
     * {@link GeminiPhone#setTTYModeGemini(int, Message, int)}
     * 
     * @param phone
     * @param ttyMode
     * @param msg
     */
    private void setTTYMode(int ttyMode) {
        if (GeminiUtils.isGeminiSupport()) {
            GeminiPhone gPhone = (GeminiPhone) phone;
            final int[] geminiSlots = GeminiUtils.getSlots();
            for (int gs : geminiSlots) {
                gPhone.setTTYModeGemini(convertTTYmodeToRadio(ttyMode), mHandler
                        .obtainMessage(EVENT_TTY_MODE_SET), gs);
            }
        } else {
            phone.setTTYMode(convertTTYmodeToRadio(ttyMode), mHandler
                    .obtainMessage(EVENT_TTY_MODE_SET));
        }
    }

    private int convertTTYmodeToRadio(int ttyMode) {
        int radioMode = 0;

        switch (ttyMode) {
        case Phone.TTY_MODE_FULL:
        case Phone.TTY_MODE_HCO:
        case Phone.TTY_MODE_VCO:
            radioMode = Phone.TTY_MODE_FULL;
            break;
        default:
            radioMode = Phone.TTY_MODE_OFF;
        }
        return radioMode;
    }
    /// @}

    /// M: Indicate if the screen has soft navigation bar or not. @{
    // MTK add
    public static boolean sHasNavigationBar;
    /// @}

}
