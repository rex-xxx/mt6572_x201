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

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.KeyguardManager;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.IBluetoothHeadsetPhone;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.ComponentName;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Telephony.SIMInfo;
import android.telephony.ServiceState;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.method.DialerKeyListener;
import android.telephony.PhoneNumberUtils;
import android.util.EventLog;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.view.MenuInflater;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;
import com.android.internal.telephony.sip.SipPhone;
import android.widget.ListAdapter;

import android.telephony.TelephonyManager;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallerInfo;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.CallManager;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.MmiCode;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyCapabilities;
import com.android.phone.Constants.CallStatusCode;
import com.android.phone.InCallUiState.InCallScreenMode;
import com.android.phone.OtaUtils.CdmaOtaScreenState;
import com.android.phone.sip.SipSharedPreferences;
import com.android.internal.telephony.gsm.SuppServiceNotification;
import com.android.internal.telephony.gemini.MTKCallManager;
import com.android.internal.telephony.gemini.GeminiPhone;
import android.preference.PreferenceManager;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import com.android.internal.telephony.gsm.SuppCrssNotification;
import android.widget.Button;
import android.view.MenuItem;
import com.mediatek.common.MediatekClassFactory;
import com.mediatek.common.telephony.IServiceStateExt;
import com.mediatek.vt.VTManager;
import android.view.SurfaceView;
import java.io.File;
import java.io.IOException;
import android.os.StatFs;
import android.content.ContextWrapper;
import java.util.Timer; 
import java.util.TimerTask; 
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.view.SurfaceHolder;
import android.os.PowerManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.app.ProgressDialog;
import java.util.Timer; 
import java.util.TimerTask; 
import android.text.format.DateFormat;
import java.util.Date;
import android.graphics.drawable.AnimationDrawable;
import android.os.RemoteException;
import android.provider.Telephony.Intents;
import static android.provider.Telephony.Intents.ACTION_UNLOCK_KEYGUARD;
import java.util.List;

import com.android.internal.telephony.cdma.CdmaMmiCode;
import com.android.internal.telephony.gsm.GsmMmiCode;

import com.mediatek.phone.PhoneFeatureConstants.FeatureOption;
import android.os.Process;
import com.mediatek.phone.ext.ExtensionManager;
import com.mediatek.phone.ext.InCallScreenExtension;
import com.mediatek.phone.ext.IInCallScreen;
import com.mediatek.phone.gemini.GeminiUtils;
import com.mediatek.phone.gemini.GeminiRegister;

import com.mediatek.phone.CallPickerAdapter;
import com.mediatek.phone.recording.PhoneRecorder;
import com.mediatek.phone.recording.PhoneRecorderHandler;
import com.mediatek.phone.vt.IVTInCallScreen;
import com.mediatek.phone.vt.VTCallUtils;
import com.mediatek.phone.vt.VTInCallScreenFlags;
import com.mediatek.phone.vt.VTInCallScreenProxy;
import com.mediatek.phone.DualTalkUtils;
import com.mediatek.phone.InCallMenuState;
import com.mediatek.phone.VoiceCommandHandler;
import com.mediatek.settings.VTSettingUtils;

/**
 * Phone app "in call" screen.
 */
public class InCallScreen extends Activity
        implements View.OnClickListener, CallTime.OnTickListener, PopupMenu.OnMenuItemClickListener,
                   PhoneRecorderHandler.Listener, DialogInterface.OnShowListener, IInCallScreen,
                   VoiceCommandHandler.Listener {
    private static final String LOG_TAG = "InCallScreen";

    //private static final boolean DBG = (PhoneGlobals.DBG_LEVEL >= 1) && (SystemProperties.getInt("ro.debuggable", 0) == 1);
    //private static final boolean VDBG = (PhoneGlobals.DBG_LEVEL >= 2);
    private static final boolean DBG = true;
    private static final boolean VDBG = true;
    /**
     * Intent extra used to specify whether the DTMF dialpad should be
     * initially visible when bringing up the InCallScreen.  (If this
     * extra is present, the dialpad will be initially shown if the extra
     * has the boolean value true, and initially hidden otherwise.)
     */
    // TODO: Should be EXTRA_SHOW_DIALPAD for consistency.
    static final String SHOW_DIALPAD_EXTRA = "com.android.phone.ShowDialpad";

    static final String KEY_EMERGENCY_DIALER = "com.android.phone.EmergencyDialer";

    /**
     * Intent extra to specify the package name of the gateway
     * provider.  Used to get the name displayed in the in-call screen
     * during the call setup. The value is a string.
     */
    // TODO: This extra is currently set by the gateway application as
    // a temporary measure. Ultimately, the framework will securely
    // set it.
    /* package */ static final String EXTRA_GATEWAY_PROVIDER_PACKAGE =
            "com.android.phone.extra.GATEWAY_PROVIDER_PACKAGE";

    /**
     * Intent extra to specify the URI of the provider to place the
     * call. The value is a string. It holds the gateway address
     * (phone gateway URL should start with the 'tel:' scheme) that
     * will actually be contacted to call the number passed in the
     * intent URL or in the EXTRA_PHONE_NUMBER extra.
     */
    // TODO: Should the value be a Uri (Parcelable)? Need to make sure
    // MMI code '#' don't get confused as URI fragments.
    /* package */ static final String EXTRA_GATEWAY_URI =
            "com.android.phone.extra.GATEWAY_URI";
   /**
     * Intent extra to force the speaker on at devices without ear piece, such as tablet
     * The intent extra will only be appended while MTK_TB_APP_CALL_FORCE_SPEAKER_ON is true
     */
    /* package */ static final String EXTRA_FORCE_SPEAKER_ON =
             "com.android.phone.extra.FORCE_SPEAKER_ON";

    // Amount of time (in msec) that we display the "Call ended" state.
    // The "short" value is for calls ended by the local user, and the
    // "long" value is for calls ended by the remote caller.
    private static final int CALL_ENDED_SHORT_DELAY =  0;  // msec
    private static final int CALL_ENDED_LONG_DELAY = 2000;  // msec
    private static final int CALL_ENDED_EXTRA_LONG_DELAY = 5000;  // msec

    // Amount of time that we display the PAUSE alert Dialog showing the
    // post dial string yet to be send out to the n/w
    private static final int PAUSE_PROMPT_DIALOG_TIMEOUT = 2000;  //msec

    // Amount of time that we display the provider info if applicable.
    private static final int PROVIDER_INFO_TIMEOUT = 5000;  // msec

    // These are values for the settings of the auto retry mode:
    // 0 = disabled
    // 1 = enabled
    // TODO (Moto):These constants don't really belong here,
    // they should be moved to Settings where the value is being looked up in the first place
    static final int AUTO_RETRY_OFF = 0;
    static final int AUTO_RETRY_ON = 1;

    // Message codes; see mHandler below.
    // Note message codes < 100 are reserved for the PhoneGlobals.
    private static final int PHONE_STATE_CHANGED = 101;
    private static final int PHONE_DISCONNECT = 102;
    private static final int EVENT_HEADSET_PLUG_STATE_CHANGED = 103;
    private static final int POST_ON_DIAL_CHARS = 104;
    private static final int WILD_PROMPT_CHAR_ENTERED = 105;
    private static final int ADD_VOICEMAIL_NUMBER = 106;
    private static final int DONT_ADD_VOICEMAIL_NUMBER = 107;
    public static final int DELAYED_CLEANUP_AFTER_DISCONNECT = 108;
    private static final int SUPP_SERVICE_FAILED = 110;
    private static final int REQUEST_UPDATE_BLUETOOTH_INDICATION = 114;
    private static final int PHONE_CDMA_CALL_WAITING = 115;
    private static final int REQUEST_CLOSE_SPC_ERROR_NOTICE = 118;
    private static final int REQUEST_CLOSE_OTA_FAILURE_NOTICE = 119;
    private static final int EVENT_PAUSE_DIALOG_COMPLETE = 120;
    private static final int EVENT_HIDE_PROVIDER_INFO = 121;  // Time to remove the info.
    private static final int REQUEST_UPDATE_SCREEN = 122;
    private static final int PHONE_INCOMING_RING = 123;
    private static final int PHONE_NEW_RINGING_CONNECTION = 124;

    private static final int DELAY_AUTO_ANSWER = 125;
    private static final int SET_IGNORE_USER_ACTIVITY = 126;
    private static final int PHONE_RECORD_STATE_UPDATE = 130;
    private static final int SUPP_SERVICE_NOTIFICATION = 140;
    private static final int CRSS_SUPP_SERVICE = 141;

    private static final int FAKE_INCOMING_CALL_WIDGET = 160;
    private static final int VT_VOICE_ANSWER_OVER = 161;
    //private static final int DELAY_TO_SHOW_MMI_INIT = 201;
    private static final int VT_EM_AUTO_ANSWER = 202;
    private static final int DELAY_TO_FINISH_INCALLSCREEN = 203;
    
    private static final int REQUEST_UPDATE_SCREEN_EXT = 204;

    // When InCallScreenMode is UNDEFINED set the default action
    // to ACTION_UNDEFINED so if we are resumed the activity will
    // know its undefined. In particular checkIsOtaCall will return
    // false.
    public static final String ACTION_UNDEFINED = "com.android.phone.InCallScreen.UNDEFINED";

    public static final String EXTRA_SEND_EMPTY_FLASH = "com.android.phone.extra.SEND_EMPTY_FLASH";
    
    private static int sPreHeadsetPlugState = -1;
    
    private boolean mSpeechEnabled = false;
    private boolean mShowStatusIndication = false;
    
    private long mLastClickActionTime = 0;

    /** Status codes returned from syncWithPhoneState(). */
    private enum SyncWithPhoneStateStatus {
        /**
         * Successfully updated our internal state based on the telephony state.
         */
        SUCCESS,

        /**
         * There was no phone state to sync with (i.e. the phone was
         * completely idle).  In most cases this means that the
         * in-call UI shouldn't be visible in the first place, unless
         * we need to remain in the foreground while displaying an
         * error message.
         */
        PHONE_NOT_IN_USE
    }

    private boolean mRegisteredForPhoneStates;

    private PhoneGlobals mApp;
    private CallManager mCM;

    // TODO: need to clean up all remaining uses of mPhone.
    // (There may be more than one Phone instance on the device, so it's wrong
    // to just keep a single mPhone field.  Instead, any time we need a Phone
    // reference we should get it dynamically from the CallManager, probably
    // based on the current foreground Call.)
    private Phone mPhone;

    private Call mForegroundCall;
    private Call mBackgroundCall;
    private Call mRingingCall;
    private Call.State mForegroundLastState = Call.State.IDLE;
    private Call.State mBackgroundLastState = Call.State.IDLE;
    private Call.State mRingingLastState = Call.State.IDLE;

    private BluetoothHeadset mBluetoothHeadset;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothAdapter mAdapter;
    private boolean mBluetoothConnectionPending;
    private long mBluetoothConnectionRequestTime;

    /** Main in-call UI elements. */
    private CallCard mCallCard;

    /** M: New Feature Phone Landscape UI @{ */
    /* secondary call info */
    private ViewStub mSecCallInfo;
    /** @ }*/

    // Both voice call and VT call use mCallTime,
    // so move mCallTime from CallCard to InCallScreen
    private CallTime mCallTime;

    // Use proxy design pattern to improve performance of launching InCallScreen
    // VTInCallScreen will be inflated when first VT call is incoming or making
    private IVTInCallScreen mVTInCallScreen;

    // UI controls:
    private InCallMenuState mInCallMenuState;
    private InCallControlState mInCallControlState;
    private InCallTouchUi mInCallTouchUi;
    private RespondViaSmsManager mRespondViaSmsManager;  // see internalRespondViaSms()
    private ManageConferenceUtils mManageConferenceUtils;

    // DTMF Dialer controller and its view:
    // Below just use google default
    private DTMFTwelveKeyDialer mDialer;

    private EditText mWildPromptText;

    // Various dialogs we bring up (see dismissAllDialogs()).
    // TODO: convert these all to use the "managed dialogs" framework.
    //
    // The MMI started dialog can actually be one of 2 items:
    //   1. An alert dialog if the MMI code is a normal MMI
    //   2. A progress dialog if the user requested a USSD
    private Dialog mMmiStartedDialog;
    private AlertDialog mMissingVoicemailDialog;
    private AlertDialog mGenericErrorDialog;
    private AlertDialog mSuppServiceFailureDialog;
    private AlertDialog mWaitPromptDialog;
    private AlertDialog mWildPromptDialog;
    private AlertDialog mCallLostDialog;
    private AlertDialog mPausePromptDialog;
    private AlertDialog mExitingECMDialog;
    private AlertDialog mCanDismissDialog;
    private AlertDialog mSimCollisionDialog;
    private AlertDialog mStorageSpaceDialog;
    // add for dual talk
    private AlertDialog mCallSelectDialog;

    private ImageView mVoiceRecorderIcon;

    // NOTE: if you add a new dialog here, be sure to add it to dismissAllDialogs() also.

    // ProgressDialog created by showProgressIndication()
    private ProgressDialog mProgressDialog;

    // TODO: If the Activity class ever provides an easy way to get the
    // current "activity lifecycle" state, we can remove these flags.
    private boolean mIsDestroyed = false;
    private boolean mIsForegroundActivity = false;
    private boolean mIsForegroundActivityForProximity = false;
    private PowerManager mPowerManager;

    // For use with Pause/Wait dialogs
    private String mPostDialStrAfterPause;
    private boolean mPauseInProgress = false;

    private static final String PIN_REQUIRED = "PIN_REQUIRED";
    private static final String PUK_REQUIRED = "PUK_REQUIRED";
    private boolean mSwappingCalls = false;//if the fc and bc is swaping
    private boolean mOnAnswerandEndCall = false;//if on Answer and End call state
    
    // For VT voice answer
    private boolean mInVoiceAnswerVideoCall = false;
    private ProgressDialog mVTVoiceAnswerDialog = null;
    private Timer mVTVoiceAnswerTimer = null;

    // Info about the most-recently-disconnected Connection, which is used
    // to determine what should happen when exiting the InCallScreen after a
    // call.  (This info is set by onDisconnect(), and used by
    // delayedCleanupAfterDisconnect().)
    /* Decline call and send sms start*/
    private static final int SEND_MESSAGE = 1;

    private PopupMenu mPopupMenu;
    
    /**
     * Change Feature by mediatek .inc
     * description : support for dualtalk
     */
    DualTalkUtils mDualTalk;
    /**
     * change by mediatek .inc end
     */

    InCallScreenExtension mExtension;
    private VoiceCommandHandler mVoiceCommandHandler;

    /*private Runnable runnableForOnPause = new Runnable() {
        public void run() {
            PhoneGlobals.getInstance().setIgnoreTouchUserActivity(false);
        }
    };*/

    /** In-call audio routing options; see switchInCallAudio(). */
    public enum InCallAudioMode {
        SPEAKER,    // Speakerphone
        BLUETOOTH,  // Bluetooth headset (if available)
        EARPIECE,   // Handset earpiece (or wired headset, if connected)
    }


    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (mIsDestroyed) {
                if (DBG) log("Handler: ignoring message " + msg + "; we're destroyed!");
                //if (msg.what == SET_IGNORE_USER_ACTIVITY) {
                //    mApp.setIgnoreTouchUserActivity(false);
                //}
                return;
            }
            if (!mIsForegroundActivity) {
                if (DBG) log("Handler: handling message " + msg + " while not in foreground");
                // Continue anyway; some of the messages below *want* to
                // be handled even if we're not the foreground activity
                // (like DELAYED_CLEANUP_AFTER_DISCONNECT), and they all
                // should at least be safe to handle if we're not in the
                // foreground...
            }

            switch (msg.what) {
                case SUPP_SERVICE_FAILED:
                    onSuppServiceFailed((AsyncResult) msg.obj);
                    break;

                case SUPP_SERVICE_NOTIFICATION:
                    onSuppServiceNotification((AsyncResult) msg.obj);
                    break;
                    
                case CRSS_SUPP_SERVICE:
                    onSuppCrssSuppServiceNotification((AsyncResult) msg.obj);
                    break;

                case PHONE_STATE_CHANGED:
                    if (DBG) {
                        log("----------------------------------------InCallScreen Phone state change----------------------------------");
                    }
                    onPhoneStateChanged((AsyncResult) msg.obj);
                    break;

                case PHONE_DISCONNECT:
                case PHONE_DISCONNECT2:
                case PHONE_DISCONNECT3:
                case PHONE_DISCONNECT4:
                    onDisconnect((AsyncResult) msg.obj, msg.what);
                    break;

                case EVENT_HEADSET_PLUG_STATE_CHANGED:
                    if (sPreHeadsetPlugState != msg.arg1 || sPreHeadsetPlugState == -1) {
                        // Update the in-call UI, since some UI elements (in
                        // particular the "Speaker" menu button) change state
                        // depending on whether a headset is plugged in.
                        // TODO: A full updateScreen() is overkill here, since
                        // the value of PhoneGlobals.isHeadsetPlugged() only affects
                        // a single menu item. (But even a full updateScreen()
                        // is still pretty cheap, so let's keep this simple
                        // for now.)
                        if (!isBluetoothAudioConnected()) {
                            if (msg.arg1 != 1) {
                                // If the state is "not connected", restore the speaker state.
                                // We ONLY want to do this on the wired headset connect /
                                // disconnect events for now though, so we're only triggering
                                // on EVENT_HEADSET_PLUG_STATE_CHANGED.

                                /// M: for Video Call && ALPS00456873 @{
                                // change feature: if VT call is dial out with
                                // speaker off(headset/bluetooth connected)
                                // when plug out, should force to turn on speaker,
                                // otherwise restore to previous mode
                                //
                                // MTK add
                                if(VTCallUtils.isVTActive() && VTCallUtils.isVTDialWithSpeakerOff()) {
                                    PhoneUtils.turnOnSpeaker(InCallScreen.this, true, true);
                                    VTCallUtils.setVTDialWithSpeakerOff(false);
                                } else {
                                /// @}
                                    PhoneUtils.restoreSpeakerMode(InCallScreen.this);
                                }
                            } else {
                                // If the state is "connected", force the speaker off without
                                // storing the state.
                                PhoneUtils.turnOnSpeaker(InCallScreen.this, false, false);
                                // If the dialpad is open, we need to start the timer that will
                                // eventually bring up the "touch lock" overlay.
                                /*
                                if (mDialer.isOpened() && !isTouchLocked()) {
                                    resetTouchLockTimer();
                                }*/
                            }
                        } 
                        sPreHeadsetPlugState = msg.arg1;
                    }
                    updateScreen();

                    // Also, force the "audio mode" popup to refresh itself if
                    // it's visible, since one of its items is either "Wired
                    // headset" or "Handset earpiece" depending on whether the
                    // headset is plugged in or not.
                    mInCallTouchUi.refreshAudioModePopup();  // safe even if the popup's not active
                    mVTInCallScreen.refreshAudioModePopup();
                    break;

                //case DELAY_TO_SHOW_MMI_INIT:
                    //onMMIInitiate((AsyncResult) msg.obj, msg.arg1);
                //    break;

                // TODO: sort out MMI code (probably we should remove this method entirely).
                // See also MMI handling code in onResume()
                // case PhoneGlobals.MMI_INITIATE:
                // onMMIInitiate((AsyncResult) msg.obj, PhoneConstants.GEMINI_SIM_1);
                //    break;

                //case PhoneGlobals.MMI_INITIATE2:
                //    onMMIInitiate((AsyncResult) msg.obj, PhoneConstants.GEMINI_SIM_2);
                //    break;

                case PhoneGlobals.MMI_CANCEL:
                case PhoneGlobals.MMI_CANCEL2:
                case PhoneGlobals.MMI_CANCEL3:
                case PhoneGlobals.MMI_CANCEL4:
                    final int slotId = GeminiRegister.getSlotIdByRegisterEvent(msg.what, PhoneGlobals.MMI_CANCEL_GEMINI);
                    onMMICancel(slotId);
                    break;

                // handle the mmi complete message.
                // since the message display class has been replaced with
                // a system dialog in PhoneUtils.displayMMIComplete(), we
                // should finish the activity here to close the window.

                case PhoneGlobals.MMI_COMPLETE:
                case PhoneGlobals.MMI_COMPLETE2:
                case PhoneGlobals.MMI_COMPLETE3:
                case PhoneGlobals.MMI_COMPLETE4:
                    onMMIComplete((MmiCode) ((AsyncResult) msg.obj).result);
                    break;

                case POST_ON_DIAL_CHARS:
                    handlePostOnDialChars((AsyncResult) msg.obj, (char) msg.arg1);
                    break;

                case ADD_VOICEMAIL_NUMBER:
                    addVoiceMailNumberPanel();
                    break;

                case DONT_ADD_VOICEMAIL_NUMBER:
                    dontAddVoiceMailNumber();
                    break;

                case DELAYED_CLEANUP_AFTER_DISCONNECT:
                case DELAYED_CLEANUP_AFTER_DISCONNECT2:
                case DELAYED_CLEANUP_AFTER_DISCONNECT3:
                case DELAYED_CLEANUP_AFTER_DISCONNECT4:
                    log("mHandler() DELAYED_CLEANUP_AFTER_DISCONNECT  : " + msg.what);
                    delayedCleanupAfterDisconnect(msg.what);
                    break;

                case REQUEST_UPDATE_BLUETOOTH_INDICATION:
                    if (VDBG) log("REQUEST_UPDATE_BLUETOOTH_INDICATION...");
                    // The bluetooth headset state changed, so some UI
                    // elements may need to update.  (There's no need to
                    // look up the current state here, since any UI
                    // elements that care about the bluetooth state get it
                    // directly from PhoneGlobals.showBluetoothIndication().)
                    /// M: for ALPS00515118 && ALPS00598135@{
                    // avoid update screen when InCallScreen is about to move to backgroud
                    if (mApp.inCallUiState.inCallScreenMode != InCallScreenMode.UNDEFINED) {
                    /// @}
                        updateScreen();
                    }
                    break;

                case PHONE_CDMA_CALL_WAITING:
                    if (DBG) log("Received PHONE_CDMA_CALL_WAITING event ...");
                    Connection cn = mCM.getFirstActiveRingingCall().getLatestConnection();

                    // Only proceed if we get a valid connection object
                    if (cn != null) {
                        // Finally update screen with Call waiting info and request
                        // screen to wake up
                        updateScreen();
                        mApp.updateWakeState();
                    }
                    break;

                case REQUEST_CLOSE_SPC_ERROR_NOTICE:
                    if (mApp.otaUtils != null) {
                        mApp.otaUtils.onOtaCloseSpcNotice();
                    }
                    break;

                case REQUEST_CLOSE_OTA_FAILURE_NOTICE:
                    if (mApp.otaUtils != null) {
                        mApp.otaUtils.onOtaCloseFailureNotice();
                    }
                    break;

                case EVENT_PAUSE_DIALOG_COMPLETE:
                    if (mPausePromptDialog != null) {
                        if (DBG) log("- DISMISSING mPausePromptDialog.");
                        mPausePromptDialog.dismiss();  // safe even if already dismissed
                        mPausePromptDialog = null;
                    }
                    break;

                case EVENT_HIDE_PROVIDER_INFO:
                    mApp.inCallUiState.providerInfoVisible = false;
                    if (mCallCard != null) {
                        mCallCard.updateState(mCM);
                    }
                    break;

               case FAKE_INCOMING_CALL_WIDGET:
                        log("handleMessage FAKE_INCOMING_CALL_WIDGET");
                        mInCallTouchUi.updateState(mCM);
                        if (mSuppServiceFailureDialog != null && mSuppServiceFailureDialog.isShowing()) {
                            mSuppServiceFailureDialog.dismiss();
                            mSuppServiceFailureDialog = null;
                        }
                        mSuppServiceFailureDialog = new AlertDialog.Builder(InCallScreen.this)
                                                                   .setMessage(R.string.incall_error_supp_service_switch)
                                                                   .setPositiveButton(R.string.ok, null)
                                                                   .setCancelable(true)
                                                                   .create();
                        mSuppServiceFailureDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
                        mSuppServiceFailureDialog.show();
                    break;

                case DELAY_AUTO_ANSWER:
                    if (FeatureOption.MTK_VT3G324M_SUPPORT) {
                        if (VTCallUtils.isVTRinging()) {
                            break;
                        }
                    }
                    try {
                        Context friendContext = createPackageContext("com.mediatek.engineermode",
                                CONTEXT_IGNORE_SECURITY);
                        SharedPreferences sh = friendContext.getSharedPreferences("AutoAnswer",
                                MODE_WORLD_READABLE);

                        if (sh.getBoolean("flag", false)) {
                            if (null != mCM) {
                                PhoneUtils.answerCall(mCM.getFirstActiveRingingCall());
                            }
                        }
                    } catch (NameNotFoundException e) {
                        e.printStackTrace();
                    }
                    break;

                case PHONE_RECORD_STATE_UPDATE:
                    requestUpdateRecordState(PhoneRecorderHandler.getInstance().getPhoneRecorderState(),
                                             PhoneRecorderHandler.getInstance().getCustomValue());
                    break;

                case REQUEST_UPDATE_SCREEN:
                case REQUEST_UPDATE_SCREEN_EXT:
                    updateScreen();
                    break;

                case PHONE_INCOMING_RING:
                    onIncomingRing();
                    break;

                case PHONE_NEW_RINGING_CONNECTION:
                    onNewRingingConnection();
                    break;

                case VT_VOICE_ANSWER_OVER:
                    if (DBG) {
                        log("- handler : VT_VOICE_ANSWER_OVER ! ");
                    }
                    if (getInVoiceAnswerVideoCall()) {
                        setInVoiceAnswerVideoCall(false);
                        delayedCleanupAfterDisconnect();
                    }
                    break;

                case PhoneUtils.PHONE_SPEECH_INFO:
                case PhoneUtils.PHONE_SPEECH_INFO2:
                case PhoneUtils.PHONE_SPEECH_INFO3:
                case PhoneUtils.PHONE_SPEECH_INFO4:
                    AsyncResult ar = (AsyncResult) msg.obj;
                    if (((int[])ar.result)[0] == 1) {
                        log("- handler : PHONE_SPEECH_INFO enabled!");
                        mSpeechEnabled = true;
                        //CR:ALPS00251944 start
                        if (!mInCallControlState.dialpadEnabled) {
                            log("- handler : PHONE_SPEECH_INFO updateInCallTouchUi!");
                            updateInCallTouchUi();
                        }
                        //CR:ALPS00251944 end
                    } else {
                        log("- handler : PHONE_SPEECH_INFO disabled!");
                        mSpeechEnabled = false;
                    }
                    break;

                case VT_EM_AUTO_ANSWER:
                    if (DBG) {
                        log("- handler : VT_EM_AUTO_ANSWER ! ");
                    }
                    /*if (FeatureOption.MTK_VT3G324M_SUPPORT == true) {
                        if (VTCallUtils.isVTRinging()) {
                            getInCallTouchUi().touchAnswerCall();
                        }
                    }*/
                    internalAnswerCall();
                    break;

                case DELAY_TO_FINISH_INCALLSCREEN:
                    final InCallUiState inCallUiState = mApp.inCallUiState;
                    boolean handledStartupError = false;
                    if (inCallUiState.hasPendingCallStatusCode()) {
                        if (DBG) {
                            log("- DELAY_TO_FINISH_INCALLSCREEN: need to show status indication!");
                        }
                        showStatusIndication(inCallUiState.getPendingCallStatusCode());

                        // Set handledStartupError to ensure that we won't bail out below.
                        // (We need to stay here in the InCallScreen so that the user
                        // is able to see the error dialog!)
                        handledStartupError = true;
                        
                        // Clear pending code for not showing same dialog in next onResume()
                        inCallUiState.setPendingCallStatusCode(CallStatusCode.SUCCESS);
                    } else {
                        if (mCM.getState() == PhoneConstants.State.IDLE) {
                            finish();
                        }
                    }
                    break;

                default:
                    Log.wtf(LOG_TAG, "mHandler: unexpected message: " + msg);
                    break;
            }
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals(Intent.ACTION_HEADSET_PLUG)) {
                    // Listen for ACTION_HEADSET_PLUG broadcasts so that we
                    // can update the onscreen UI when the headset state changes.
                    // if (DBG) log("mReceiver: ACTION_HEADSET_PLUG");
                    // if (DBG) log("==> intent: " + intent);
                    // if (DBG) log("    state: " + intent.getIntExtra("state", 0));
                    // if (DBG) log("    name: " + intent.getStringExtra("name"));
                    // send the event and add the state as an argument.
                    Message message = Message.obtain(mHandler, EVENT_HEADSET_PLUG_STATE_CHANGED,
                            intent.getIntExtra("state", 0), 0);
                    mHandler.sendMessage(message);
                }
            }
        };

    // InCallScreen should be destroyed, so set mLocaleChanged to static.
    static private boolean mLocaleChanged = false; 
    private final BroadcastReceiver mLocaleChangeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals(Intent.ACTION_LOCALE_CHANGED)) {
                    mLocaleChanged = true;
                    mApp.notificationMgr.updateInCallNotification();
                }
            }
        };
        private static String ACTION_LOCKED = "com.mediatek.dm.LAWMO_LOCK";
        private static String ACTION_UNLOCK = "com.mediatek.dm.LAWMO_UNLOCK";

        private final BroadcastReceiver mDMLockReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals(ACTION_LOCKED)) {
                    int msg = R.string.dm_lock;
                    PhoneConstants.State state = mCM.getState() ;
                    if (state == PhoneConstants.State.IDLE) {
                        return;
                    } else {
                        Toast.makeText(InCallScreen.this, msg, Toast.LENGTH_LONG).show();
                    }
                } else if (action.equals(ACTION_UNLOCK)) {
                    int msg = R.string.dm_unlock;
                    PhoneConstants.State state = mCM.getState() ;
                    if (state == PhoneConstants.State.IDLE) {
                        return;
                    } else {
                        Toast.makeText(InCallScreen.this, msg, Toast.LENGTH_LONG).show();
                    }
                }

                mCallCard.updateState(mCM);
                updateInCallTouchUi();   

                if (Constants.VTScreenMode.VT_SCREEN_OPEN == mVTInCallScreen.getVTScreenMode()) {
                    mVTInCallScreen.updateVTScreen(mVTInCallScreen.getVTScreenMode());
                }
            }
        };

        ///M: #Only emergency# text replace in case IMEI locked @{
        private IServiceStateExt mServiceStateExt;
        ///@}

    @Override
    protected void onCreate(Bundle icicle) {
        Log.i(LOG_TAG, "onCreate()...  this = " + this);
        ///M: #Only emergency# text replace in case IMEI locked @{
        mServiceStateExt = MediatekClassFactory.createInstance(IServiceStateExt.class, this);
        ///@}
        Profiler.callScreenOnCreate();
        super.onCreate(icicle);

        // Make sure this is a voice-capable device.
        if (!PhoneGlobals.sVoiceCapable) {
            // There should be no way to ever reach the InCallScreen on a
            // non-voice-capable device, since this activity is not exported by
            // our manifest, and we explicitly disable any other external APIs
            // like the CALL intent and ITelephony.showCallScreen().
            // So the fact that we got here indicates a phone app bug.
            Log.wtf(LOG_TAG, "onCreate() reached on non-voice-capable device");
            finish();
            return;
        }

        mApp = PhoneGlobals.getInstance();
        mApp.setInCallScreenInstance(this);

        // set this flag so this activity will stay in front of the keyguard
        int flags = WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON;
        if (mApp.getPhoneState() == PhoneConstants.State.OFFHOOK) {
            // While we are in call, the in-call screen should dismiss the keyguard.
            // This allows the user to press Home to go directly home without going through
            // an insecure lock screen.
            // But we do not want to do this if there is no active call so we do not
            // bypass the keyguard if the call is not answered or declined.
            if (VDBG) {
                log("onCreate: set window FLAG_DISMISS_KEYGUARD flag ");
            }
            if (!PhoneUtils.isDMLocked()) {
                flags |= WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD;
            }
        }

        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.flags |= flags;
        if (!mApp.proximitySensorModeEnabled()) {
            // If we don't have a proximity sensor, then the in-call screen explicitly
            // controls user activity.  This is to prevent spurious touches from waking
            // the display.
            lp.inputFeatures |= WindowManager.LayoutParams.INPUT_FEATURE_DISABLE_USER_ACTIVITY;
        }
        getWindow().setAttributes(lp);

        mCM = mApp.mCM;
        mCMGemini = mApp.mCMGemini;
        log("- onCreate: phone state = " + mCM.getState());

        setPhone(mApp.phone);  // Sets mPhone

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter != null) {
            // Blow should be use this activity context
            // should not use application context to avoid memory leak
            mBluetoothAdapter.getProfileProxy(this, mBluetoothProfileServiceListener,
                                    BluetoothProfile.HEADSET);
        }

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        mExtension = ExtensionManager.getInstance().getInCallScreenExtension();
        mExtension.onCreate(icicle, this, this, mCM);

        setContentView(R.layout.incall_screen);

        /** M: New Feature Phone Landscape UI @{ */
        // If in landscape, then one of the ViewStubs (instead of <include>) is used for the
        // incall_touch_ui, because CDMA and GSM button layouts are noticeably different.
        //final ViewStub touchUiStub = (ViewStub) findViewById(
                //mPhone.getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA
                //? R.id.inCallTouchUiCdmaStub : R.id.inCallTouchUiStub);
        //if (touchUiStub != null) touchUiStub.inflate();
        /** @ }*/

        initInCallScreen();

        registerForPhoneStates();

        // No need to change wake state here; that happens in onResume() when we
        // are actually displayed.

        // Handle the Intent we were launched with, but only if this is the
        // the very first time we're being launched (ie. NOT if we're being
        // re-initialized after previously being shut down.)
        // Once we're up and running, any future Intents we need
        // to handle will come in via the onNewIntent() method.
        if (icicle == null) {
            if (DBG) log("onCreate(): this is our very first launch, checking intent...");
            internalResolveIntent(getIntent());
        }

        if (VTInCallScreenFlags.getInstance().mVTIsInflate
                && null != mVTInCallScreen) {
            if (DBG) {
                log("onCreate(): VTInCallScreen already inflated before destroy, inflate again");
            }
            mVTInCallScreen.initVTInCallScreen();
        }

        Profiler.callScreenCreated();
        registerReceiver(mLocaleChangeReceiver, new IntentFilter(Intent.ACTION_LOCALE_CHANGED));

        IntentFilter dmLockFilter = new IntentFilter(ACTION_LOCKED);
        dmLockFilter.addAction(ACTION_UNLOCK);

        registerReceiver(mDMLockReceiver, dmLockFilter);

        mHandler.sendEmptyMessageDelayed(DELAY_AUTO_ANSWER, 2000);
        registerReceiver(mReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));

        PhoneRecorderHandler.getInstance().setListener(this);

        if (FeatureOption.MTK_VOICE_UI_SUPPORT) {
            mVoiceCommandHandler = new VoiceCommandHandler(this, this);
        }

        /// M: To adjust the layout for some special screen,
        // such as no physical key screen, which has a soft navigation bar. @{
        //
        // MTK add
        amendLayoutForDifferentScreen();
        /// @}

        if (DBG) log("onCreate(): exit");
    }

    private BluetoothProfile.ServiceListener mBluetoothProfileServiceListener =
             new BluetoothProfile.ServiceListener() {
         @Override
         public void onServiceConnected(int profile, BluetoothProfile proxy) {
             mBluetoothHeadset = (BluetoothHeadset) proxy;
             if (VDBG) log("- Got BluetoothHeadset: " + mBluetoothHeadset);
         }

         @Override
         public void onServiceDisconnected(int profile) {
             mBluetoothHeadset = null;
         }
    };

    /**
     * Sets the Phone object used internally by the InCallScreen.
     *
     * In normal operation this is called from onCreate(), and the
     * passed-in Phone object comes from the PhoneGlobals.
     * For testing, test classes can use this method to
     * inject a test Phone instance.
     */
    /* package */ void setPhone(Phone phone) {
        mPhone = phone;
        // Hang onto the three Call objects too; they're singletons that
        // are constant (and never null) for the life of the Phone.        
        mForegroundCall = mCM.getActiveFgCall();
        mBackgroundCall = mCM.getFirstActiveBgCall();
        mRingingCall = mCM.getFirstActiveRingingCall();
    }

    @Override
    protected void onResume() {
        // !!!!! Need to check below line code
        //Profiler.trace(Profiler.InCallScreenEnterOnResume);
        if (DBG) log("onResume()...");
        super.onResume();
        dismissDialogs();

        mIsForegroundActivity = true;
        mIsForegroundActivityForProximity = true;

        if (DualTalkUtils.isSupportDualTalk() && mDualTalk == null) {
            mDualTalk = DualTalkUtils.getInstance();
        }

        // The flag shouldn't be turned on when there are actual phone calls.
        if (mCM.hasActiveFgCall() || mCM.hasActiveBgCall() || mCM.hasActiveRingingCall()) {
            mApp.inCallUiState.showAlreadyDisconnectedState = false;
        }

        final InCallUiState inCallUiState = mApp.inCallUiState;
        //if (VDBG) inCallUiState.dumpState();

        if (mLocaleChanged) {
            mLocaleChanged = false;
            mCallCard.updateForLanguageChange();
            mVTInCallScreen.notifyLocaleChange();
            ManageConferenceUtils.sLocalChanged = true;
        }
        
        if (FeatureOption.MTK_VT3G324M_SUPPORT) {
            mVTInCallScreen.updateVTScreen(mVTInCallScreen.getVTScreenMode());
            mVTInCallScreen.dismissVTDialogs();
        }

        updateExpandedViewState();

        // ...and update the in-call notification too, since the status bar
        // icon needs to be hidden while we're the foreground activity:
        mApp.notificationMgr.updateInCallNotification();

        // Listen for broadcast intents that might affect the onscreen UI.
        //registerReceiver(mReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));

        // Keep a "dialer session" active when we're in the foreground.
        // (This is needed to play DTMF tones.)
        mDialer.startDialerSession();

        // Restore various other state from the InCallUiState object:

        // Update the onscreen dialpad state to match the InCallUiState.
        if (inCallUiState.showDialpad) {
            openDialpadInternal(false);  // no "opening" animation
        } else {
            closeDialpadInternal(false);  // no "closing" animation
        }

        // Reset the dialpad context
        // TODO: Dialpad digits should be set here as well (once they are saved)
        mDialer.setDialpadContext(inCallUiState.dialpadContextText);
        //Use to fix Google default behavior(Google introduced this from android 4.1.2):
        //In order to resolve ALPS00422424, this will add about 10+(ms) effort, but we have no
        //choice about this...
        if (mCM.getState() == PhoneConstants.State.OFFHOOK) {
            Call fgCall = null;
            if (DualTalkUtils.isSupportDualTalk()) {
                fgCall = mDualTalk.getActiveFgCall();
            } else {
                fgCall = mCM.getActiveFgCall();
            }
            
            SIMInfo info = PhoneUtils.getSimInfoByCall(fgCall);
            Connection connection = fgCall.getEarliestConnection();
            if (info != null && connection != null 
                    && !PhoneUtils.isVoicemailNumber(connection.getAddress(), info.mSlot, mCM.getFgPhone())) {
                mDialer.setDialpadContext("");
            }
        }

        // If there's a "Respond via SMS" popup still around since the
        // last time we were the foreground activity, make sure it's not
        // still active!
        // (The popup should *never* be visible initially when we first
        // come to the foreground; it only ever comes up in response to
        // the user selecting the "SMS" option from the incoming call
        // widget.)
        mRespondViaSmsManager.dismissPopup();  // safe even if already dismissed

        // Display an error / diagnostic indication if necessary.
        //
        // When the InCallScreen comes to the foreground, we normally we
        // display the in-call UI in whatever state is appropriate based on
        // the state of the telephony framework (e.g. an outgoing call in
        // DIALING state, an incoming call, etc.)
        //
        // But if the InCallUiState has a "pending call status code" set,
        // that means we need to display some kind of status or error
        // indication to the user instead of the regular in-call UI.  (The
        // most common example of this is when there's some kind of
        // failure while initiating an outgoing call; see
        // CallController.placeCall().)
        //
        boolean handledStartupError = false;
        mShowStatusIndication = false;
        if (inCallUiState.hasPendingCallStatusCode()) {
            if (DBG) log("- onResume: need to show status indication!");
            mShowStatusIndication = true;
            showStatusIndication(inCallUiState.getPendingCallStatusCode());

            // Set handledStartupError to ensure that we won't bail out below.
            // (We need to stay here in the InCallScreen so that the user
            // is able to see the error dialog!)
            handledStartupError = true;
            
            // Clear pending code for not showing same dialog in next onResume()
            inCallUiState.setPendingCallStatusCode(CallStatusCode.SUCCESS);
        }

        // Set the volume control handler while we are in the foreground.
        final boolean bluetoothConnected = isBluetoothAudioConnected();

        if (bluetoothConnected) {
            setVolumeControlStream(AudioManager.STREAM_BLUETOOTH_SCO);
        } else {
            setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
        }

        takeKeyEvents(true);

        // If an OTASP call is in progress, use the special OTASP-specific UI.
        boolean inOtaCall = false;
        if (TelephonyCapabilities.supportsOtasp(mPhone)) {
            inOtaCall = checkOtaspStateOnResume();
        }
        if (!inOtaCall) {
            // Always start off in NORMAL mode
            setInCallScreenMode(InCallScreenMode.NORMAL);
        }

        // Before checking the state of the CallManager, clean up any
        // connections in the DISCONNECTED state.
        // (The DISCONNECTED state is used only to drive the "call ended"
        // UI; it's totally useless when *entering* the InCallScreen.)
        mCM.clearDisconnected();

        /// M: For ALPS00613349
        setOnAnswerAndEndFlag(false);

        // Update the onscreen UI to reflect the current telephony state.
        SyncWithPhoneStateStatus status = syncWithPhoneState();

        // Note there's no need to call updateScreen() here;
        // syncWithPhoneState() already did that if necessary.

        if (status != SyncWithPhoneStateStatus.SUCCESS) {
            if (DBG) log("- onResume: syncWithPhoneState failed! status = " + status);
            // Couldn't update the UI, presumably because the phone is totally
            // idle.

            // Even though the phone is idle, though, we do still need to
            // stay here on the InCallScreen if we're displaying an
            // error dialog (see "showStatusIndication()" above).

            if (handledStartupError) {
                // Stay here for now.  We'll eventually leave the
                // InCallScreen when the user presses the dialog's OK
                // button (see bailOutAfterErrorDialog()), or when the
                // progress indicator goes away.
                Log.i(LOG_TAG, "  ==> syncWithPhoneState failed, but staying here anyway.");
            } else {
                // The phone is idle, and we did NOT handle a
                // startup error during this pass thru onResume.
                //
                // This basically means that we're being resumed because of
                // some action *other* than a new intent.  (For example,
                // the user pressing POWER to wake up the device, causing
                // the InCallScreen to come back to the foreground.)
                //
                // In this scenario we do NOT want to stay here on the
                // InCallScreen: we're not showing any useful info to the
                // user (like a dialog), and the in-call UI itself is
                // useless if there's no active call.  So bail out.

                Log.i(LOG_TAG, "  ==> syncWithPhoneState failed; bailing out!");
                dismissAllDialogs();

                // Force the InCallScreen to truly finish(), rather than just
                // moving it to the back of the activity stack (which is what
                // our finish() method usually does.)
                // This is necessary to avoid an obscure scenario where the
                // InCallScreen can get stuck in an inconsistent state, somehow
                // causing a *subsequent* outgoing call to fail (bug 4172599).
                
                /*This is ugly and boring for ALPS00111659 (dial out an long invalid number)
                 * Modify Google default behavior that finish self to set an special flag.
                 * There is an exception if the incallscreen not in foreground, so check status
                 * to determine if need to finish ourself.
                 */
                //endInCallScreenSession(true /* force a real finish() call */);
                if (!InCallUiState.isNormalInCallScreenState()) {
                    Log.d(LOG_TAG, "  ==> syncWithPhoneState failed; not in normal status!");
                    endInCallScreenSession(true);
                    InCallUiState.sLastInCallScreenStatus = InCallUiState.INCALLSCREEN_NOT_EXIT_NORMAL;
                } else {
                    mApp.inCallUiState.delayFinished = true;
                    mHandler.sendEmptyMessageDelayed(DELAY_TO_FINISH_INCALLSCREEN, PAUSE_PROMPT_DIALOG_TIMEOUT);
                }
                adjustProcessPriority();
                return;
            }
        } else if (TelephonyCapabilities.supportsOtasp(mPhone)) {
            if (inCallUiState.inCallScreenMode == InCallScreenMode.OTA_NORMAL ||
                    inCallUiState.inCallScreenMode == InCallScreenMode.OTA_ENDED) {
                if (mCallCard != null) mCallCard.setVisibility(View.GONE);
                updateScreen();
                adjustProcessPriority();
                return;
            }
        }

        /*This is ugly and boring for ALPS00111659 (dial out an long invalid number)*/
        mApp.inCallUiState.delayFinished = false;
        
        //This means the incallscreen is in normal status
        InCallUiState.sLastInCallScreenStatus = InCallUiState.INCALLSCREEN_NOT_EXIT_NORMAL;
        
        // InCallScreen is now active.
        EventLog.writeEvent(EventLogTags.PHONE_UI_ENTER);

        // Update the poke lock and wake lock when we move to the foreground.
        // This will be no-op when prox sensor is effective.
        mApp.updateWakeState();

        // Restore the mute state if the last mute state change was NOT
        // done by the user.
        if (mApp.getRestoreMuteOnInCallResume()) {
            // Mute state is based on the foreground call
            PhoneUtils.restoreMuteState();
            mApp.setRestoreMuteOnInCallResume(false);
        }

        Profiler.profileViewCreate(getWindow(), InCallScreen.class.getName());

        // If there's a pending MMI code, we'll show a dialog here.
        //
        // Note: previously we had shown the dialog when MMI_INITIATE event's coming
        // from telephony layer, while right now we don't because the event comes
        // too early (before in-call screen is prepared).
        // Now we instead check pending MMI code and show the dialog here.
        //
        // This *may* cause some problem, e.g. when the user really quickly starts
        // MMI sequence and calls an actual phone number before the MMI request
        // being completed, which is rather rare.
        //
        // TODO: streamline this logic and have a UX in a better manner.
        // Right now syncWithPhoneState() above will return SUCCESS based on
        // mPhone.getPendingMmiCodes().isEmpty(), while we check it again here.
        // Also we show pre-populated in-call UI under the dialog, which looks
        // not great. (issue 5210375, 5545506)
        // After cleaning them, remove commented-out MMI handling code elsewhere.
        if (GeminiUtils.isGeminiSupport()) {
            int simid = -1;
            int messageid = -1;
            /// M:Gemini+ @{
            final int[] geminiSlots = GeminiUtils.getSlots();
            for (int i = 0; i < geminiSlots.length; i++) {
                if (!((GeminiPhone) mPhone).getPendingMmiCodesGemini(geminiSlots[i]).isEmpty()) {
                    simid = geminiSlots[i];
                    messageid = PhoneGlobals.MMI_CANCEL_GEMINI[i];
                    break;
                }
            }
            /// @}
            if (-1 != simid) {
                if (mMmiStartedDialog == null) {
                    MmiCode mmiCode = ((GeminiPhone)mPhone).getPendingMmiCodesGemini(simid).get(0);
                    Message message = Message.obtain(mHandler, messageid);
                    mMmiStartedDialog = PhoneUtils.displayMMIInitiate(this, mmiCode,
                            message, mMmiStartedDialog);
                    // mInCallScreen needs to receive MMI_COMPLETE/MMI_CANCEL event from telephony,
                    // which will dismiss the entire screen.
                }
            }
        } else {
            if (!mPhone.getPendingMmiCodes().isEmpty()) {
                if (mMmiStartedDialog == null) {
                    MmiCode mmiCode = mPhone.getPendingMmiCodes().get(0);
                    Message message = Message.obtain(mHandler, PhoneGlobals.MMI_CANCEL);
                    mMmiStartedDialog = PhoneUtils.displayMMIInitiate(this, mmiCode,
                            message, mMmiStartedDialog);
                    // mInCallScreen needs to receive MMI_COMPLETE/MMI_CANCEL event from telephony,
                    // which will dismiss the entire screen.
                }
            }
        }

        // This means the screen is shown even though there's no connection, which only happens
        // when the phone call has hung up while the screen is turned off at that moment.
        // We want to show "disconnected" state with photos with appropriate elapsed time for
        // the finished phone call.
        if (mApp.inCallUiState.showAlreadyDisconnectedState) {
            if (DBG) {
                log("onResume(): detected \"show already disconnected state\" situation."
                    + " set up DELAYED_CLEANUP_AFTER_DISCONNECT message with "
                    + CALL_ENDED_LONG_DELAY + " msec delay.");
            }
            mHandler.removeMessages(DELAYED_CLEANUP_AFTER_DISCONNECT);
            mHandler.sendEmptyMessageDelayed(DELAYED_CLEANUP_AFTER_DISCONNECT,
                    CALL_ENDED_LONG_DELAY);
        }

        mHandler.sendEmptyMessageDelayed(DELAY_AUTO_ANSWER, 2000);
        initVTAutoAnswer();
        
        mIsForegroundActivity = true;
        //handlePendingStatus();
        if (null != mHandler) {
            mHandler.sendEmptyMessageDelayed(PHONE_RECORD_STATE_UPDATE, 500);
        }

        Log.i(LOG_TAG, "[mtk performance result]:" + System.currentTimeMillis());
        if (VDBG) log("onResume() done.");
        //Profiler.trace(Profiler.InCallScreenLeaveOnResume);
        adjustProcessPriority();
    }

    // onPause is guaranteed to be called when the InCallScreen goes
    // in the background.
    @Override
    protected void onPause() {
        if (DBG) log("onPause()...");
        super.onPause();

        if (mPowerManager.isScreenOn()) {
            // Set to false when the screen went background *not* by screen turned off. Probably
            // the user bailed out of the in-call screen (by pressing BACK, HOME, etc.)
            mIsForegroundActivityForProximity = false;
        }
        mIsForegroundActivity = false;
        
        
        mApp.notificationMgr.updateInCallNotification();

        if (DBG) {
            log("- remove DELAY_TO_FINISH_INCALLSCREEN:");
        }
        mHandler.removeMessages(DELAY_TO_FINISH_INCALLSCREEN);
        // Force a clear of the provider info frame. Since the
        // frame is removed using a timed message, it is
        // possible we missed it if the prev call was interrupted.
        mApp.inCallUiState.providerInfoVisible = false;

        // "show-already-disconnected-state" should be effective just during the first wake-up.
        // We should never allow it to stay true after that.
        mApp.inCallUiState.showAlreadyDisconnectedState = false;

        // A safety measure to disable proximity sensor in case call failed
        // and the telephony state did not change.
        mApp.setBeginningCall(false);

        // Make sure the "Manage conference" chronometer is stopped when
        // we move away from the foreground.
        mManageConferenceUtils.stopConferenceTime();

        // as a catch-all, make sure that any dtmf tones are stopped
        // when the UI is no longer in the foreground.
        mDialer.onDialerKeyUp(null);

        // Release any "dialer session" resources, now that we're no
        // longer in the foreground.
        mDialer.stopDialerSession();

        // If the device is put to sleep as the phone call is ending,
        // we may see cases where the DELAYED_CLEANUP_AFTER_DISCONNECT
        // event gets handled AFTER the device goes to sleep and wakes
        // up again.

        // This is because it is possible for a sleep command
        // (executed with the End Call key) to come during the 2
        // seconds that the "Call Ended" screen is up.  Sleep then
        // pauses the device (including the cleanup event) and
        // resumes the event when it wakes up.

        // To fix this, we introduce a bit of code that pushes the UI
        // to the background if we pause and see a request to
        // DELAYED_CLEANUP_AFTER_DISCONNECT.

        // Note: We can try to finish directly, by:
        //  1. Removing the DELAYED_CLEANUP_AFTER_DISCONNECT messages
        //  2. Calling delayedCleanupAfterDisconnect directly

        // However, doing so can cause problems between the phone
        // app and the keyguard - the keyguard is trying to sleep at
        // the same time that the phone state is changing.  This can
        // end up causing the sleep request to be ignored.
        //Fix issue(ALPS00406953)
        if ((mHandler.hasMessages(DELAYED_CLEANUP_AFTER_DISCONNECT) 
                || mHandler.hasMessages(DELAYED_CLEANUP_AFTER_DISCONNECT2))
                && (mCM.getState() /*!= Phone.State.RINGING*/ == PhoneConstants.State.IDLE)) {
            if (DBG) log("DELAYED_CLEANUP_AFTER_DISCONNECT detected, moving UI to background.");
            endInCallScreenSession();
        }

        EventLog.writeEvent(EventLogTags.PHONE_UI_EXIT);

        // Dismiss any dialogs we may have brought up, just to be 100%
        // sure they won't still be around when we get back here.
        dismissAllDialogs();

        if (FeatureOption.MTK_VT3G324M_SUPPORT) {
            mVTInCallScreen.dismissVTDialogs();
            if (FeatureOption.MTK_PHONE_VT_VOICE_ANSWER) {
                if (getInVoiceAnswerVideoCall()) {
                    setInVoiceAnswerVideoCall(false);
                    delayedCleanupAfterDisconnect();
                }
            }
        }

        updateExpandedViewState();

        // ...and the in-call notification too:
        //mApp.notificationMgr.updateInCallNotification();
        // ...and *always* reset the system bar back to its normal state
        // when leaving the in-call UI.
        // (While we're the foreground activity, we disable navigation in
        // some call states; see InCallTouchUi.updateState().)
        mApp.notificationMgr.statusBarHelper.enableSystemBarNavigation(true);

        // Unregister for broadcast intents.  (These affect the visible UI
        // of the InCallScreen, so we only care about them while we're in the
        // foreground.)
        //unregisterReceiver(mReceiver);

        // Make sure we revert the poke lock and wake lock when we move to
        // the background.
        mApp.updateWakeState();

        // clear the dismiss keyguard flag so we are back to the default state
        // when we next resume
        updateKeyguardPolicy(false);

        // See also PhoneGlobals#updatePhoneState(), which takes care of all the other release() calls.
        if (mApp.getUpdateLock().isHeld() && mApp.getPhoneState() == PhoneConstants.State.IDLE) {
            if (DBG) {
                log("Release UpdateLock on onPause() because there's no active phone call.");
            }
            mApp.getUpdateLock().release();
        }

        /**
         * add by mediatek .inc end
         */
        if (null != mPopupMenu) {
            mPopupMenu.dismiss();
        }

        //ALPS00448181
        if (mInCallTouchUi != null) {
            mInCallTouchUi.dismissAudioModePopup();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (FeatureOption.MTK_VOICE_UI_SUPPORT) {
            if (null != mVoiceCommandHandler) {
                if (VoiceCommandHandler.isValidCondition()) {
                    mVoiceCommandHandler.startVoiceCommand();
                }
            }
        }
    }

    @Override
    protected void onStop() {
        if (DBG) log("onStop()...");
        super.onStop();

        PhoneConstants.State state = mCM.getState();
        if (DBG) log("onStop: state = " + state);

        if (state == PhoneConstants.State.IDLE) {
            if (mRespondViaSmsManager.isShowingPopup()) {
                // This means that the user has been opening the "Respond via SMS" dialog even
                // after the incoming call hanging up, and the screen finally went background.
                // In that case we just close the dialog and exit the whole in-call screen.
                mRespondViaSmsManager.dismissPopup();
            }

            // when OTA Activation, OTA Success/Failure dialog or OTA SPC
            // failure dialog is running, do not destroy inCallScreen. Because call
            // is already ended and dialog will not get redrawn on slider event.
            if ((mApp.cdmaOtaProvisionData != null)
                    && (mApp.cdmaOtaScreenState != null)
                    && ((mApp.cdmaOtaScreenState.otaScreenState != CdmaOtaScreenState.OtaScreenState.OTA_STATUS_ACTIVATION)
                            && (mApp.cdmaOtaScreenState.otaScreenState != CdmaOtaScreenState.OtaScreenState.OTA_STATUS_SUCCESS_FAILURE_DLG)
                            && (!mApp.cdmaOtaProvisionData.inOtaSpcState))) {
                // we don't want the call screen to remain in the activity
                // history
                // if there are not active or ringing calls.
                if (DBG) {
                    log("- onStop: calling finish() to clear activity history...");
                }
                moveTaskToBack(true);
                if (mApp.otaUtils != null) {
                    mApp.otaUtils.cleanOtaScreen(true);
                }
            }
            ///M: ALPS00383496 Add for the situation that on the process of disconnect a call,
            ///  user change to bluetooth mode when bluetooth is avilable, but bluetooth need
            ///  a disconnect order to resume the A2DP, so we need make sure the phone has
            ///  disconnected the bluetooth when it exits. @{
            if (isBluetoothAvailable() && isBluetoothAudioConnected()) {
                log("Call disconnectBluetoothAudio from onStop()");
                disconnectBluetoothAudio();
            }
            ///M: @}
        }
        mVTInCallScreen.onStop();

        if (FeatureOption.MTK_VOICE_UI_SUPPORT) {
            if (null != mVoiceCommandHandler) {
                mVoiceCommandHandler.stopVoiceCommand();
            }
        }
    }

    @Override
    protected void onDestroy() {
        Log.i(LOG_TAG, "onDestroy()...  this = " + this);
        super.onDestroy();

        // Set the magic flag that tells us NOT to handle any handler
        // messages that come in asynchronously after we get destroyed.
        mIsDestroyed = true;

        unregisterReceiver(mLocaleChangeReceiver);
        unregisterReceiver(mDMLockReceiver);

        mApp.clearInCallScreenInstance(this);

        PhoneRecorderHandler.getInstance().clearListener(this);

        if (FeatureOption.MTK_VOICE_UI_SUPPORT) {
            if (null != mVoiceCommandHandler) {
                mVoiceCommandHandler.clear();
            }
        }

        // Clear out the InCallScreen references in various helper objects
        // (to let them know we've been destroyed).
        if (mCallCard != null) {
            mCallCard.setInCallScreenInstance(null);
        }
        if (mInCallTouchUi != null) {
            mInCallTouchUi.setInCallScreenInstance(null);
        }
        mRespondViaSmsManager.setInCallScreenInstance(null);

        if (mCallTime != null) {
            mCallTime.cancelTimer();
            mCallTime.setCallTimeListener(null);
        }

        mDialer.clearInCallScreenReference();
        mDialer = null;

        unregisterForPhoneStates();
        // No need to change wake state here; that happens in onPause() when we
        // are moving out of the foreground.

        if (mBluetoothHeadset != null) {
            mBluetoothAdapter.closeProfileProxy(BluetoothProfile.HEADSET, mBluetoothHeadset);
            mBluetoothHeadset = null;
        }

        // Dismiss all dialogs, to be absolutely sure we won't leak any of
        // them while changing orientation.
        unregisterReceiver(mReceiver);
        dismissAllDialogs();
        if (FeatureOption.MTK_VT3G324M_SUPPORT) {
            mVTInCallScreen.dismissVTDialogs();
            mVTInCallScreen.onDestroy();
        }
        // If there's an OtaUtils instance around, clear out its
        // references to our internal widgets.
        if (mApp.otaUtils != null) {
            mApp.otaUtils.clearUiWidgets();
        }

        mExtension.onDestroy(this);
    }

    /**
     * Dismisses the in-call screen.
     *
     * We never *really* finish() the InCallScreen, since we don't want to
     * get destroyed and then have to be re-created from scratch for the
     * next call.  Instead, we just move ourselves to the back of the
     * activity stack.
     *
     * This also means that we'll no longer be reachable via the BACK
     * button (since moveTaskToBack() puts us behind the Home app, but the
     * home app doesn't allow the BACK key to move you any farther down in
     * the history stack.)
     *
     * (Since the Phone app itself is never killed, this basically means
     * that we'll keep a single InCallScreen instance around for the
     * entire uptime of the device.  This noticeably improves the UI
     * responsiveness for incoming calls.)
     */
    @Override
    public void finish() {
        if (DBG) log("finish()...");
        dismissDialogs();
        moveTaskToBack(true);
    }

    /**
     * End the current in call screen session.
     *
     * This must be called when an InCallScreen session has
     * complete so that the next invocation via an onResume will
     * not be in an old state.
     */
    public void endInCallScreenSession() {
        if (DBG) log("endInCallScreenSession()... phone state = " + mCM.getState());
        endInCallScreenSession(false);
    }

    /**
     * Internal version of endInCallScreenSession().
     *
     * @param forceFinish If true, force the InCallScreen to
     *        truly finish() rather than just calling moveTaskToBack().
     *        @see finish()
     */
    private void endInCallScreenSession(boolean forceFinish) {
        if (DBG) {
            log("endInCallScreenSession(" + forceFinish + ")...  phone state = " + mCM.getState());
        }

        /// M: For ALPS00567667 @{
        // click "start record" while call end, record will run in background; so stop here.
        //
        // MTK add
        if (FeatureOption.MTK_PHONE_VOICE_RECORDING) {
            handleRecordProc();
        }
        /// @}

        if (forceFinish) {
            Log.i(LOG_TAG, "endInCallScreenSession(): FORCING a call to super.finish()!");
            super.finish();  // Call super.finish() rather than our own finish() method,
                             // which actually just calls moveTaskToBack().
        } else {
            moveTaskToBack(true);
        }
        setInCallScreenMode(InCallScreenMode.UNDEFINED);
    }

    /**
     * True when this Activity is in foreground (between onResume() and onPause()).
     */
    /* package */ boolean isForegroundActivity() {
        return mIsForegroundActivity;
    }

    /**
     * Returns true when the Activity is in foreground (between onResume() and onPause()),
     * or, is in background due to user's bailing out of the screen, not by screen turning off.
     *
     * @see #isForegroundActivity()
     */
    /* package */ boolean isForegroundActivityForProximity() {
        return mIsForegroundActivityForProximity;
    }

    /* package */ void updateKeyguardPolicy(boolean dismissKeyguard) {
        if (DBG) {
            log("incallscreen: updateKeyguardPolicy() ,dismissKeyguard: " + dismissKeyguard);
        }
        if (dismissKeyguard) {
            if (DBG) {
                log("updateKeyguardPolicy: set dismiss keyguard window flag ");
            }
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        } else {
            if (DBG) {
                log("updateKeyguardPolicy: clear dismiss keyguard window flag ");
            }
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        }
    }

    private void registerForPhoneStates() {
        if (!mRegisteredForPhoneStates) {

            /// M: for Gemini @{
            // modify register methods for Gemini/ Gemini+
            //
            // mCM.registerForPreciseCallStateChanged(mHandler, PHONE_STATE_CHANGED, null);
            // mCM.registerForDisconnect(mHandler, PHONE_DISCONNECT, null);
            // // TODO: sort out MMI code (probably we should remove this method entirely).
            // // See also MMI handling code in onResume()
            // // mCM.registerForMmiInitiate(mHandler, PhoneApp.MMI_INITIATE, null);

            // // register for the MMI complete message.  Upon completion,
            // // PhoneUtils will bring up a system dialog instead of the
            // // message display class in PhoneUtils.displayMMIComplete().
            // // We'll listen for that message too, so that we can finish
            // // the activity at the same time.
            // mCM.registerForMmiComplete(mHandler, PhoneGlobals.MMI_COMPLETE, null);
            // mCM.registerForCallWaiting(mHandler, PHONE_CDMA_CALL_WAITING, null);
            // mCM.registerForPostDialCharacter(mHandler, POST_ON_DIAL_CHARS, null);
            // mCM.registerForSuppServiceFailed(mHandler, SUPP_SERVICE_FAILED, null);
            // mCM.registerForIncomingRing(mHandler, PHONE_INCOMING_RING, null);
            // mCM.registerForNewRingingConnection(mHandler, PHONE_NEW_RINGING_CONNECTION, null);

            Object callManager = GeminiUtils.isGeminiSupport() ? mCMGemini : mCM;

            GeminiRegister.registerForPreciseCallStateChanged(callManager, mHandler, PHONE_STATE_CHANGED);
            GeminiRegister.registerForDisconnect(callManager, mHandler, PHONE_DISCONNECT_GEMINI);
            // TODO: sort out MMI code (probably we should remove this method entirely).
            // See also MMI handling code in onResume()
            // mCM.registerForMmiInitiate(mHandler, PhoneGlobals.MMI_INITIATE, null);

            // For C+G dualtalk, the mPhone's type is CDMA
            if (FeatureOption.EVDO_DT_SUPPORT) {
                mCM.registerForMmiComplete(mHandler, PhoneGlobals.MMI_COMPLETE, null);
            }

            final int phoneType = mPhone.getPhoneType();
            if (phoneType == PhoneConstants.PHONE_TYPE_GSM) {
                GeminiRegister.registerForMmiComplete(callManager, mHandler, PhoneGlobals.MMI_COMPLETE_GEMINI);
            } else if (phoneType == PhoneConstants.PHONE_TYPE_CDMA) {
                if (DBG) {
                    log("Registering for Call Waiting.");
                }
                mCM.registerForCallWaiting(mHandler, PHONE_CDMA_CALL_WAITING, null);
            } else {
                throw new IllegalStateException("Unexpected phone type: " + phoneType);
            }
            GeminiRegister.registerForPostDialCharacter(callManager, mHandler, POST_ON_DIAL_CHARS);
            GeminiRegister.registerForSuppServiceFailed(callManager, mHandler, SUPP_SERVICE_FAILED);
            GeminiRegister.registerForIncomingRing(callManager, mHandler, PHONE_INCOMING_RING);
            GeminiRegister.registerForNewRingingConnection(callManager, mHandler, PHONE_NEW_RINGING_CONNECTION);
            /// @}

            /// M:  for Gemini @{
            // register MTK customized messages
            //
            // MTK add
            registerForPhoneStatesMTK(callManager);
            /// @}
            mRegisteredForPhoneStates = true;
        }
    }

    private void unregisterForPhoneStates() {
        /// M: for Gemini && Gemini+ @{
        //
        // mCM.unregisterForPreciseCallStateChanged(mHandler);
        // mCM.unregisterForDisconnect(mHandler);
        // mCM.unregisterForMmiInitiate(mHandler);
        // mCM.unregisterForMmiComplete(mHandler);
        // mCM.unregisterForCallWaiting(mHandler);
        // mCM.unregisterForPostDialCharacter(mHandler);
        // mCM.unregisterForSuppServiceFailed(mHandler);
        // mCM.unregisterForIncomingRing(mHandler);
        // mCM.unregisterForNewRingingConnection(mHandler);

        Object callManager = GeminiUtils.isGeminiSupport() ? mCMGemini : mCM;

        GeminiRegister.unregisterForPreciseCallStateChanged(callManager, mHandler);
        GeminiRegister.unregisterForDisconnect(callManager, mHandler);


        int phoneType = mPhone.getPhoneType();
        if (phoneType == PhoneConstants.PHONE_TYPE_GSM) {
            GeminiRegister.unregisterForMmiComplete(callManager, mHandler);
            GeminiRegister.unregisterForMmiInitiate(callManager, mHandler);
        } else if (phoneType == PhoneConstants.PHONE_TYPE_CDMA) {
            if (DBG) {
                log("Registering for Call Waiting.");
            }
            mCM.unregisterForCallWaiting(mHandler);
        }
        // For C+G dualtalk, the mPhone's type is CDMA
        if (FeatureOption.EVDO_DT_SUPPORT) {
            mCM.unregisterForMmiComplete(mHandler);
        }

        GeminiRegister.unregisterForPostDialCharacter(callManager, mHandler);
        GeminiRegister.unregisterForSuppServiceFailed(callManager, mHandler);
        GeminiRegister.unregisterForIncomingRing(callManager, mHandler);
        GeminiRegister.unregisterForNewRingingConnection(callManager, mHandler);
        /// @}

        /// M: for Gemini @{
        // unregister MTK customized messages
        //
        // MTK add
        unregisterForPhoneStatesMTK(callManager);
        /// @}

        mRegisteredForPhoneStates = false;
    }

    /* package */ void updateAfterRadioTechnologyChange() {
        if (DBG) Log.d(LOG_TAG, "updateAfterRadioTechnologyChange()...");

        // Reset the call screen since the calls cannot be transferred
        // across radio technologies.
        resetInCallScreenMode();

        // Unregister for all events from the old obsolete phone
        unregisterForPhoneStates();

        // (Re)register for all events relevant to the new active phone
        registerForPhoneStates();

        // And finally, refresh the onscreen UI.  (Note that it's safe
        // to call requestUpdateScreen() even if the radio change ended up
        // causing us to exit the InCallScreen.)
        requestUpdateScreen();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        log("onNewIntent: intent = " + intent + ", phone state = " + mCM.getState());

        // We're being re-launched with a new Intent.  Since it's possible for a
        // single InCallScreen instance to persist indefinitely (even if we
        // finish() ourselves), this sequence can potentially happen any time
        // the InCallScreen needs to be displayed.

        // Stash away the new intent so that we can get it in the future
        // by calling getIntent().  (Otherwise getIntent() will return the
        // original Intent from when we first got created!)
        setIntent(intent);

        // Activities are always paused before receiving a new intent, so
        // we can count on our onResume() method being called next.

        // Just like in onCreate(), handle the intent.
        internalResolveIntent(intent);
    }

    private void internalResolveIntent(Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }
        String action = intent.getAction();
        if (DBG) log("internalResolveIntent: action=" + action);

        // In gingerbread and earlier releases, the InCallScreen used to
        // directly handle certain intent actions that could initiate phone
        // calls, namely ACTION_CALL and ACTION_CALL_EMERGENCY, and also
        // OtaUtils.ACTION_PERFORM_CDMA_PROVISIONING.
        //
        // But it doesn't make sense to tie those actions to the InCallScreen
        // (or especially to the *activity lifecycle* of the InCallScreen).
        // Instead, the InCallScreen should only be concerned with running the
        // onscreen UI while in a call.  So we've now offloaded the call-control
        // functionality to a new module called CallController, and OTASP calls
        // are now launched from the OtaUtils startInteractiveOtasp() or
        // startNonInteractiveOtasp() methods.
        //
        // So now, the InCallScreen is only ever launched using the ACTION_MAIN
        // action, and (upon launch) performs no functionality other than
        // displaying the UI in a state that matches the current telephony
        // state.

        if (action.equals(intent.ACTION_MAIN)) {
            // This action is the normal way to bring up the in-call UI.
            //
            // Most of the interesting work of updating the onscreen UI (to
            // match the current telephony state) happens in the
            // syncWithPhoneState() => updateScreen() sequence that happens in
            // onResume().
            //
            // But we do check here for one extra that can come along with the
            // ACTION_MAIN intent:

            if (intent.hasExtra(SHOW_DIALPAD_EXTRA)) {
                // SHOW_DIALPAD_EXTRA can be used here to specify whether the DTMF
                // dialpad should be initially visible.  If the extra isn't
                // present at all, we just leave the dialpad in its previous state.

                boolean showDialpad = intent.getBooleanExtra(SHOW_DIALPAD_EXTRA, false);
                if (VDBG) log("- internalResolveIntent: SHOW_DIALPAD_EXTRA: " + showDialpad);

                // If SHOW_DIALPAD_EXTRA is specified, that overrides whatever
                // the previous state of inCallUiState.showDialpad was.
                mApp.inCallUiState.showDialpad = showDialpad;

                final boolean hasActiveCall = mCM.hasActiveFgCall();
                final boolean hasHoldingCall = mCM.hasActiveBgCall();

                // There's only one line in use, AND it's on hold, at which we're sure the user
                // wants to use the dialpad toward the exact line, so un-hold the holding line.
                if (showDialpad && !hasActiveCall && hasHoldingCall) {
                    PhoneUtils.switchHoldingAndActive(mCM.getFirstActiveBgCall());
                }
            }

            if (intent.getBooleanExtra("isForPlaceCall", false)) {
                mCallCard.hideCallStates(mCM);
            }
            // EXTRA_FORCE_SPEAKER_ON is handled only at tablet 
            // or while the MTK_TB_APP_CALL_FORCE_SPEAKER_ON is true
            // Since there's no ear piece in tablet, speaker should be ON defaultly while call is placed
            if (FeatureOption.MTK_TB_APP_CALL_FORCE_SPEAKER_ON) {
              if (intent.hasExtra(EXTRA_FORCE_SPEAKER_ON)) {
                  boolean forceSpeakerOn = intent.getBooleanExtra(EXTRA_FORCE_SPEAKER_ON, false);
                  if (forceSpeakerOn)
                  {
                    Log.e("MTK_TB_APP_CALL_FORCE_SPEAKER_ON", "forceSpeakerOn is true");
                    if (!PhoneGlobals.getInstance().isHeadsetPlugged() 
                            && !(mApp.isBluetoothHeadsetAudioOn())) {
                      //Only force the speaker ON while not video call and speaker is not ON
                      if (!intent.getBooleanExtra(Constants.EXTRA_IS_VIDEO_CALL, false)
                              && !PhoneUtils.isSpeakerOn(mApp)) {
                        Log.e("MTK_TB_APP_CALL_FORCE_SPEAKER_ON", "PhoneUtils.turnOnSpeaker");
                        PhoneUtils.turnOnSpeaker(mApp, true, true, true);
                      }
                    }
                  }
              }
            }
            
            // ...and in onResume() we'll update the onscreen dialpad state to
            // match the InCallUiState.
            if (FeatureOption.MTK_VT3G324M_SUPPORT) {
                if (getInVoiceAnswerVideoCall()) {
                    setInVoiceAnswerVideoCall(false);
                }
                if (mCM.getState() == PhoneConstants.State.RINGING) {
                    if (DBG) {
                        log("call manager state is ringing");
                    }
                    // When VT call incoming, use voice call incoming call GUI
                    mVTInCallScreen.setVTVisible(false);
                    mVTInCallScreen.setVTScreenMode(Constants.VTScreenMode.VT_SCREEN_CLOSE);
                } else if (intent.getBooleanExtra(Constants.EXTRA_IS_VIDEO_CALL, false)) {
                    if (DBG) {
                        log("vt extra is true");
                    }
                    // When dialing VT call, inflate VTInCallScreen
                    mVTInCallScreen.initVTInCallScreen();
                    // When dialed a VT call, but dialed failed, needs not init state for dialing
                    if (CallStatusCode.SUCCESS == mApp.inCallUiState.getPendingCallStatusCode()) {
                        mVTInCallScreen.initDialingSuccessVTState();
                    }
                    mVTInCallScreen.initDialingVTState();
                    mVTInCallScreen.initCommonVTState();
                    if (PhoneConstants.State.IDLE != PhoneGlobals.getInstance().mCM.getState() &&
                                !VTCallUtils.isVideoCall(mCM.getActiveFgCall())) {
                        // When voice is connected and place a VT call, need close VT GUI
                        mVTInCallScreen.setVTScreenMode(Constants.VTScreenMode.VT_SCREEN_CLOSE);
                    } else {
                        mVTInCallScreen.setVTScreenMode(Constants.VTScreenMode.VT_SCREEN_OPEN);
                    }
                } else {
                    // set VT open or close according the active foreground call
                    if (mCM.getState() != PhoneConstants.State.IDLE && VTCallUtils.isVideoCall(mCM.getActiveFgCall())) {
                        if (DBG) {
                            log("receive ACTION_MAIN, but active foreground call is video call");
                        }
                        mVTInCallScreen.initVTInCallScreen();
                        mVTInCallScreen.initCommonVTState();
                        mVTInCallScreen.setVTScreenMode(Constants.VTScreenMode.VT_SCREEN_OPEN);
                    } else if (!intent.getBooleanExtra(Constants.EXTRA_IS_NOTIFICATION, false)) {
                        mVTInCallScreen.setVTScreenMode(Constants.VTScreenMode.VT_SCREEN_CLOSE);
                    }
                } 
                mVTInCallScreen.updateVTScreen(mVTInCallScreen.getVTScreenMode());
            }
            return;
        }
        if (action.equals(Intent.ACTION_ANSWER)) {
            /*if (FeatureOption.MTK_VT3G324M_SUPPORT == true) {
                if (VTCallUtils.isVTRinging()) {
                    mVTInCallScreen.internalAnswerVTCallPre();
                }
            }*/
            internalAnswerCall();

            mApp.setRestoreMuteOnInCallResume(false);
            return;
        }
        if (action.equals(OtaUtils.ACTION_DISPLAY_ACTIVATION_SCREEN)) {
            // Bring up the in-call UI in the OTASP-specific "activate" state;
            // see OtaUtils.startInteractiveOtasp().  Note that at this point
            // the OTASP call has not been started yet; we won't actually make
            // the call until the user presses the "Activate" button.

            if (!TelephonyCapabilities.supportsOtasp(mPhone)) {
                throw new IllegalStateException(
                    "Received ACTION_DISPLAY_ACTIVATION_SCREEN intent on non-OTASP-capable device: "
                    + intent);
            }

            setInCallScreenMode(InCallScreenMode.OTA_NORMAL);
            if ((mApp.cdmaOtaProvisionData != null)
                && (!mApp.cdmaOtaProvisionData.isOtaCallIntentProcessed)) {
                mApp.cdmaOtaProvisionData.isOtaCallIntentProcessed = true;
                mApp.cdmaOtaScreenState.otaScreenState =
                        CdmaOtaScreenState.OtaScreenState.OTA_STATUS_ACTIVATION;
            }
            return;
        }

        // Various intent actions that should no longer come here directly:
        if (action.equals(OtaUtils.ACTION_PERFORM_CDMA_PROVISIONING)) {
            // This intent is now handled by the InCallScreenShowActivation
            // activity, which translates it into a call to
            // OtaUtils.startInteractiveOtasp().
            throw new IllegalStateException(
                "Unexpected ACTION_PERFORM_CDMA_PROVISIONING received by InCallScreen: "
                + intent);
        } else if (action.equals(Intent.ACTION_CALL)
                   || action.equals(Intent.ACTION_CALL_EMERGENCY)) {
            // ACTION_CALL* intents go to the OutgoingCallBroadcaster, which now
            // translates them into CallController.placeCall() calls rather than
            // launching the InCallScreen directly.
            throw new IllegalStateException("Unexpected CALL action received by InCallScreen: "
                                            + intent);
        } else if (action.equals(ACTION_UNDEFINED)) {
            // This action is only used for internal bookkeeping; we should
            // never actually get launched with it.
            Log.wtf(LOG_TAG, "internalResolveIntent: got launched with ACTION_UNDEFINED");
            return;
        } else {
            Log.wtf(LOG_TAG, "internalResolveIntent: unexpected intent action: " + action);
            // But continue the best we can (basically treating this case
            // like ACTION_MAIN...)
            return;
        }
    }

    /*private void stopTimer() {
        if (mCallCard != null) mCallCard.stopTimer();
    }*/

    private void initInCallScreen() {
        if (VDBG) log("initInCallScreen()...");

        // Have the WindowManager filter out touch events that are "too fat".
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_IGNORE_CHEEK_PRESSES);

        // Initialize CallTime
        mCallTime = new CallTime(this);

        // Initialize the CallCard.
        mCallCard = (CallCard) findViewById(R.id.callCard);
        if (VDBG) log("  - mCallCard = " + mCallCard);
        
        /** M: New Feature Phone Landscape UI @{ */
        mSecCallInfo = (ViewStub) findViewById(R.id.secondary_call_info);
        /** @ }*/

        mCallCard.setInCallScreenInstance(this);

        mVoiceRecorderIcon = (ImageView) findViewById(R.id.voiceRecorderIcon);
        mVoiceRecorderIcon.setBackgroundResource(R.drawable.voice_record);
        mVoiceRecorderIcon.setVisibility(View.INVISIBLE);

        // Initialize the onscreen UI elements.
        initInCallTouchUi();

        // Helper class to keep track of enabledness/state of UI controls
        mInCallControlState = new InCallControlState(this, mCM);
        mInCallMenuState = new InCallMenuState(this, mCM);

        // Helper class to run the "Manage conference" UI
        mManageConferenceUtils = new ManageConferenceUtils(this, mCM);

        // The DTMF Dialpad.
        ViewStub stub = (ViewStub) findViewById(R.id.dtmf_twelve_key_dialer_stub);
        mDialer = new DTMFTwelveKeyDialer(this, stub);
        mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);

        // Initialize VTInCallScreen
        mVTInCallScreen = new VTInCallScreenProxy(this, mDialer);
    }

    /**
     * Returns true if the phone is "in use", meaning that at least one line
     * is active (ie. off hook or ringing or dialing).  Conversely, a return
     * value of false means there's currently no phone activity at all.
     */
    private boolean phoneIsInUse() {
        return mCM.getState() != PhoneConstants.State.IDLE;
    }

    private boolean handleDialerKeyDown(int keyCode, KeyEvent event) {
        if (VDBG) log("handleDialerKeyDown: keyCode " + keyCode + ", event " + event + "...");

        // As soon as the user starts typing valid dialable keys on the
        // keyboard (presumably to type DTMF tones) we start passing the
        // key events to the DTMFDialer's onDialerKeyDown.  We do so
        // only if the okToDialDTMFTones() conditions pass.
        if (okToDialDTMFTones()) {
            return mDialer.onDialerKeyDown(event);

            // TODO: If the dialpad isn't currently visible, maybe
            // consider automatically bringing it up right now?
            // (Just to make sure the user sees the digits widget...)
            // But this probably isn't too critical since it's awkward to
            // use the hard keyboard while in-call in the first place,
            // especially now that the in-call UI is portrait-only...
        }

        return false;
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        // For FTA: Run CRTUG on 26.8.1.3.5.3, MS should response with end key.
        // related CR 1233. 
        // solution: When long click the back Key, if it is in ringing state, and without
        // Foreground call and Background call, should hangup the call.
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mRingingCall != null && !mRingingCall.isIdle()) {
                if (mForegroundCall == null || mForegroundCall.isIdle()) {
                    if (mBackgroundCall == null || mBackgroundCall.isIdle()) {
                        // TODO Hangup call interface need replace.
                        log("onKeyLongPress(), hangupRingingCall");
                        if (FeatureOption.MTK_VT3G324M_SUPPORT) {
                            if (!VTCallUtils.isVTActive()) {
                                mVTInCallScreen.setVTScreenMode(Constants.VTScreenMode.VT_SCREEN_CLOSE);
                            } else {
                                mVTInCallScreen.setVTScreenMode(Constants.VTScreenMode.VT_SCREEN_OPEN);
                            }
                            mVTInCallScreen.updateVTScreen(mVTInCallScreen.getVTScreenMode());
                        }
                        hangupRingingCall();
                        //internalHangupAll();
                    }
                }
            }
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        if (DBG) log("onBackPressed()...");

        // To consume this BACK press, the code here should just do
        // something and return.  Otherwise, call super.onBackPressed() to
        // get the default implementation (which simply finishes the
        // current activity.)

        if (mCM.hasActiveRingingCall()) {
            // The Back key, just like the Home key, is always disabled
            // while an incoming call is ringing.  (The user *must* either
            // answer or reject the call before leaving the incoming-call
            // screen.)
            if (DBG) log("BACK key while ringing: ignored");

            // And consume this event; *don't* call super.onBackPressed().
            return;
        }

        // BACK is also used to exit out of any "special modes" of the
        // in-call UI:

        if (mDialer.isOpened()) {
            closeDialpadInternal(true);  // do the "closing" animation
            return;
        }

        if (mApp.inCallUiState.inCallScreenMode == InCallScreenMode.MANAGE_CONFERENCE) {
            // Hide the Manage Conference panel, return to NORMAL mode.
            setInCallScreenMode(InCallScreenMode.NORMAL);
            requestUpdateScreen();
            return;
        }

        // Nothing special to do.  Fall back to the default behavior.
        super.onBackPressed();
    }

    /**
     * Handles the green CALL key while in-call.
     * @return true if we consumed the event.
     */
    private boolean handleCallKey() {
        // The green CALL button means either "Answer", "Unhold", or
        // "Swap calls", or can be a no-op, depending on the current state
        // of the Phone.

        final boolean hasRingingCall = mCM.hasActiveRingingCall();
        final boolean hasActiveCall = mCM.hasActiveFgCall();
        final boolean hasHoldingCall = mCM.hasActiveBgCall();

        if (DualTalkUtils.isSupportDualTalk()) {
            if (mDualTalk != null && mDualTalk.isCdmaAndGsmActive()) {
                return handleCallKeyForDualTalk();
            }
        }

        int phoneType = PhoneConstants.PHONE_TYPE_GSM;
        if (DualTalkUtils.isSupportDualTalk() && mDualTalk != null) {
            Phone phone = mDualTalk.getFirstPhone();
            if (phone != null) {
                phoneType = phone.getPhoneType();
            }
        } else {
            phoneType = mPhone.getPhoneType();
        }
        
        if (phoneType == PhoneConstants.PHONE_TYPE_CDMA) {
            // The green CALL button means either "Answer", "Swap calls/On Hold", or
            // "Add to 3WC", depending on the current state of the Phone.

            CdmaPhoneCallState.PhoneCallState currCallState =
                mApp.cdmaPhoneCallState.getCurrentCallState();
            if (hasRingingCall) {
                //Scenario 1: Accepting the First Incoming and Call Waiting call
                if (DBG) {
                    log("answerCall: First Incoming and Call Waiting scenario");
                }
                internalAnswerCall();  // Automatically holds the current active call,
                                       // if there is one
            } else if ((currCallState == CdmaPhoneCallState.PhoneCallState.THRWAY_ACTIVE)
                    && (hasActiveCall)) {
                //Scenario 2: Merging 3Way calls
                if (DBG) {
                    log("answerCall: Merge 3-way call scenario");
                }
                // Merge calls
                PhoneUtils.mergeCalls(mCM);
            } else if (currCallState == CdmaPhoneCallState.PhoneCallState.CONF_CALL) {
                //Scenario 3: Switching between two Call waiting calls or drop the latest
                // connection if in a 3Way merge scenario
                if (DBG) {
                    log("answerCall: Switch btwn 2 calls scenario");
                }
                internalSwapCalls();
            } else if (currCallState == CdmaPhoneCallState.PhoneCallState.SINGLE_ACTIVE
                    && hasActiveCall) {
                if (DBG) {
                    log("handleCallKey: hold/unhold cdma case.");
                }
                internalSwapCalls();
            }
        } else if ((phoneType == PhoneConstants.PHONE_TYPE_GSM)
                || (phoneType == PhoneConstants.PHONE_TYPE_SIP)) {
            if (hasRingingCall) {
                // If an incoming call is ringing, the CALL button is actually
                // handled by the PhoneWindowManager.  (We do this to make
                // sure that we'll respond to the key even if the InCallScreen
                // hasn't come to the foreground yet.)
                //
                // We'd only ever get here in the extremely rare case that the
                // incoming call started ringing *after*
                // PhoneWindowManager.interceptKeyTq() but before the event
                // got here, or else if the PhoneWindowManager had some
                // problem connecting to the ITelephony service.
                Log.w(LOG_TAG, "handleCallKey: incoming call is ringing!"
                      + " (PhoneWindowManager should have handled this key.)");
                // But go ahead and handle the key as normal, since the
                // PhoneWindowManager presumably did NOT handle it:

                // There's an incoming ringing call: CALL means "Answer".
                /*if (FeatureOption.MTK_VT3G324M_SUPPORT) {
                    if (VTCallUtils.isVTRinging()) {
                        mVTInCallScreen.internalAnswerVTCallPre();
                    }
                }*/
                internalAnswerCall();
            } else if (hasActiveCall && hasHoldingCall) {
                // Two lines are in use: CALL means "Swap calls".
                if (DBG) log("handleCallKey: both lines in use ");
                if (mCM.hasActiveFgCall()) {                
                    if (DBG) {
                        log("handleCallKey: ==> swap calls.");
                    }
                internalSwapCalls();
                }
            } else if (hasHoldingCall) {
                // There's only one line in use, AND it's on hold.
                // In this case CALL is a shortcut for "unhold".
                if (DBG) log("handleCallKey: call on hold ==> unhold.");
                PhoneUtils.switchHoldingAndActive(mCM.getFirstActiveBgCall());  // Really means "unhold" in this state
            } else {
                // The most common case: there's only one line in use, and
                // it's an active call (i.e. it's not on hold.)
                // In this case CALL is a no-op.
                // (This used to be a shortcut for "add call", but that was a
                // bad idea because "Add call" is so infrequently-used, and
                // because the user experience is pretty confusing if you
                // inadvertently trigger it.)
                if (VDBG) log("handleCallKey: call in foregound ==> ignoring.");
                // If the foreground call is video call, ignore this call key
                // And if in VT, it is impossible that hasHoldingCall is true
                boolean ignoreThisCallKey = false;
                if (FeatureOption.MTK_VT3G324M_SUPPORT) {
                    if (phoneType == PhoneConstants.PHONE_TYPE_GSM) {
                        if (VTCallUtils.isVideoCall(mCM.getActiveFgCall())) {
                            ignoreThisCallKey = true;
                        }
                    }
                }
                if (VDBG) {
                    log("handleCallKey: ignoreThisCallKey = " + ignoreThisCallKey);
                }
                if (!ignoreThisCallKey)
                    // Really means "hold" in this state
                    PhoneUtils.switchHoldingAndActive(mCM.getFirstActiveBgCall());
                // But note we still consume this key event; see below.
            }
        } else {
            throw new IllegalStateException("Unexpected phone type: " + phoneType);
        }

        // We *always* consume the CALL key, since the system-wide default
        // action ("go to the in-call screen") is useless here.
        return true;
    }

    boolean isKeyEventAcceptableDTMF (KeyEvent event) {
        return (mDialer != null && mDialer.isKeyEventAcceptable(event));
    }

    /**
     * Overriden to track relevant focus changes.
     *
     * If a key is down and some time later the focus changes, we may
     * NOT recieve the keyup event; logically the keyup event has not
     * occured in this window.  This issue is fixed by treating a focus
     * changed event as an interruption to the keydown, making sure
     * that any code that needs to be run in onKeyUp is ALSO run here.
     */
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        // the dtmf tones should no longer be played
        if (VDBG) log("onWindowFocusChanged(" + hasFocus + ")...");
        if (!hasFocus && mDialer != null) {
            if (VDBG) log("- onWindowFocusChanged: faking onDialerKeyUp()...");
            mDialer.onDialerKeyUp(null);
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        // if (DBG) log("onKeyUp(keycode " + keyCode + ")...");

        // push input to the dialer.
        if ((mDialer != null) && (mDialer.onDialerKeyUp(event))) {
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_CALL) {
            // Always consume CALL to be sure the PhoneWindow won't do anything with it
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (DBG) log("onKeyDown(keycode " + keyCode + ")...");


        if (Constants.VTScreenMode.VT_SCREEN_CLOSE == mVTInCallScreen.getVTScreenMode()) {
            if (mVTInCallScreen.onKeyDown(keyCode, event)) {
                return true;
            }
        }

        switch (keyCode) {
            case KeyEvent.KEYCODE_HOME:
                if (DBG) {
                    log("ignore KEYCODE_HOME");
                }
                return true;
            case KeyEvent.KEYCODE_CALL:
                if (event.getRepeatCount() == 0) {
                    if (DBG) {
                        log(" onKeyDown() KEYCODE_CALL RepeatCount is 0 ...");
                    }
                    boolean handled = handleCallKey();
                    if (!handled) {
                        Log.w(LOG_TAG, "InCallScreen should always handle KEYCODE_CALL in onKeyDown");
                    }
                } else {
                    if (DBG) {
                        log(" onKeyDown() KEYCODE_CALL long press");
                    }
                }
                // Always consume CALL to be sure the PhoneWindow won't do anything with it
                return true;

            // Note there's no KeyEvent.KEYCODE_ENDCALL case here.
            // The standard system-wide handling of the ENDCALL key
            // (see PhoneWindowManager's handling of KEYCODE_ENDCALL)
            // already implements exactly what the UI spec wants,
            // namely (1) "hang up" if there's a current active call,
            // or (2) "don't answer" if there's a current ringing call.

            case KeyEvent.KEYCODE_CAMERA:
                // Disable the CAMERA button while in-call since it's too
                // easy to press accidentally.
                return true;

            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_MUTE:
                if (mCM.getState() == PhoneConstants.State.RINGING) {
                    // If an incoming call is ringing, the VOLUME buttons are
                    // actually handled by the PhoneWindowManager.  (We do
                    // this to make sure that we'll respond to them even if
                    // the InCallScreen hasn't come to the foreground yet.)
                    //
                    // We'd only ever get here in the extremely rare case that the
                    // incoming call started ringing *after*
                    // PhoneWindowManager.interceptKeyTq() but before the event
                    // got here, or else if the PhoneWindowManager had some
                    // problem connecting to the ITelephony service.
                    Log.w(LOG_TAG, "VOLUME key: incoming call is ringing!"
                          + " (PhoneWindowManager should have handled this key.)");
                    // But go ahead and handle the key as normal, since the
                    // PhoneWindowManager presumably did NOT handle it:
                    internalSilenceRinger();

                    // As long as an incoming call is ringing, we always
                    // consume the VOLUME keys.
                    return true;
                }
                break;

            case KeyEvent.KEYCODE_BACK:
                if (mDialer.isOpened()) {
                    if (DBG) {
                        log("mDialer.isOpened(): DTMFTwelveKeyDialer is opened");
                    }
                    closeDialpadInternal(true);
                    return true;
                }
                break;

            case KeyEvent.KEYCODE_MUTE:
                onMuteClick();
                return true;

            // Various testing/debugging features, enabled ONLY when VDBG == true.
            case KeyEvent.KEYCODE_SLASH:
                if (VDBG) {
                    log("----------- InCallScreen View dump --------------");
                    // Dump starting from the top-level view of the entire activity:
                    Window w = this.getWindow();
                    View decorView = w.getDecorView();
                    decorView.debug();
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_EQUALS:
                if (VDBG) {
                    log("----------- InCallScreen call state dump --------------");
                    PhoneUtils.dumpCallState(mPhone);
                    PhoneUtils.dumpCallManager();
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_GRAVE:
                if (VDBG) {
                    // Placeholder for other misc temp testing
                    log("------------ Temp testing -----------------");
                    return true;
                }
                break;
        }

        if (event.getRepeatCount() == 0 && handleDialerKeyDown(keyCode, event)) {
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

//MTK begin:
    static final int SUP_TYPE = 0x91;
    void onSuppServiceNotification(AsyncResult r) {
        SuppServiceNotification notification = (SuppServiceNotification) r.result;
        if (DBG) {
            log("onSuppServiceNotification: " + notification);
        }

        String msg = null;
        // MO
        if (notification.notificationType == 0) {
            msg = getSuppServiceMOStringId(notification);   
        } else if (notification.notificationType == 1) {
            // MT 
            String str = "";
            msg = getSuppServiceMTStringId(notification);
            // not 0x91 should add + .
            if (notification.type == SUP_TYPE) {
                if (notification.number != null && notification.number.length() != 0) {
                    str = " +" + notification.number;
                }
            }
            msg = msg + str;
        }
        // Display Message
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }
    
    private String getSuppServiceMOStringId(SuppServiceNotification notification) {
        // TODO Replace the strings.
        String retStr = "";
        switch (notification.code) {
        case SuppServiceNotification.MO_CODE_UNCONDITIONAL_CF_ACTIVE:
            retStr = getResources().getString(R.string.mo_code_unconditional_cf_active);
            break;
        case SuppServiceNotification.MO_CODE_SOME_CF_ACTIVE:
            retStr = getResources().getString(R.string.mo_code_some_cf_active);
            break;
        case SuppServiceNotification.MO_CODE_CALL_FORWARDED :
            retStr = getResources().getString(R.string.mo_code_call_forwarded);
            break;
        case SuppServiceNotification.MO_CODE_CALL_IS_WAITING:
            retStr = getResources().getString(R.string.mo_code_call_is_waiting);
            break;
        case SuppServiceNotification.MO_CODE_CUG_CALL:
            retStr = getResources().getString(R.string.mo_code_cug_call);
            retStr = retStr + " " + notification.index ;
            break;
        case SuppServiceNotification.MO_CODE_OUTGOING_CALLS_BARRED:
            retStr = getResources().getString(R.string.mo_code_outgoing_calls_barred);
            break;
        case SuppServiceNotification.MO_CODE_INCOMING_CALLS_BARRED:
            retStr = getResources().getString(R.string.mo_code_incoming_calls_barred);
            break;
        case SuppServiceNotification.MO_CODE_CLIR_SUPPRESSION_REJECTED:
            retStr = getResources().getString(R.string.mo_code_clir_suppression_rejected);
            break;
        case SuppServiceNotification.MO_CODE_CALL_DEFLECTED:
            retStr = getResources().getString(R.string.mo_code_call_deflected);
            break;
        default:
            // Attempt to use a service we don't recognize or support
            // ("Unsupported service" or "Selected service failed")
            retStr = getResources().getString(R.string.incall_error_supp_service_unknown);
            break;
        }
        return retStr;
    }

    private String getSuppServiceMTStringId(SuppServiceNotification notification) {
        // TODO Replace the strings.
        String retStr = "";
        switch (notification.code) {
        case SuppServiceNotification.MT_CODE_FORWARDED_CALL:
            retStr = getResources().getString(R.string.mt_code_forwarded_call);
            break;
        case SuppServiceNotification.MT_CODE_CUG_CALL:
            retStr = getResources().getString(R.string.mt_code_cug_call);
            retStr = retStr + " " + notification.index ;
            break;
        case SuppServiceNotification.MT_CODE_CALL_ON_HOLD :
            retStr = getResources().getString(R.string.mt_code_call_on_hold);
            break;
        case SuppServiceNotification.MT_CODE_CALL_RETRIEVED:
            retStr = getResources().getString(R.string.mt_code_call_retrieved);
            break;
        case SuppServiceNotification.MT_CODE_MULTI_PARTY_CALL:
            retStr = getResources().getString(R.string.mt_code_multi_party_call);
            break;
        case SuppServiceNotification.MT_CODE_ON_HOLD_CALL_RELEASED:
            retStr = getResources().getString(R.string.mt_code_on_hold_call_released);
            break;
        case SuppServiceNotification.MT_CODE_FORWARD_CHECK_RECEIVED:
            retStr = getResources().getString(R.string.mt_code_forward_check_received);
            break;
        case SuppServiceNotification.MT_CODE_CALL_CONNECTING_ECT:
            retStr = getResources().getString(R.string.mt_code_call_connecting_ect);
            break;
        case SuppServiceNotification.MT_CODE_CALL_CONNECTED_ECT:
            retStr = getResources().getString(R.string.mt_code_call_connected_ect);
            break;
        case SuppServiceNotification.MT_CODE_DEFLECTED_CALL:
            retStr = getResources().getString(R.string.mt_code_deflected_call);
            break;
        case SuppServiceNotification.MT_CODE_ADDITIONAL_CALL_FORWARDED:
            retStr = getResources().getString(R.string.mt_code_additional_call_forwarded);
            break;
        case SuppServiceNotification.MT_CODE_FORWARDED_CF:
            retStr = getResources().getString(R.string.mt_code_forwarded_call)
                    + "(" + getResources().getString(R.string.mt_code_forwarded_cf) + ")";
            break;
        case SuppServiceNotification.MT_CODE_FORWARDED_CF_UNCOND:
            retStr = getResources().getString(R.string.mt_code_forwarded_call)
                    + "(" + getResources().getString(R.string.mt_code_forwarded_cf_uncond) + ")";
            break;
        case SuppServiceNotification.MT_CODE_FORWARDED_CF_COND:
            retStr = getResources().getString(R.string.mt_code_forwarded_call)
                    + "(" + getResources().getString(R.string.mt_code_forwarded_cf_cond) + ")";
            break;
        case SuppServiceNotification.MT_CODE_FORWARDED_CF_BUSY:
            retStr = getResources().getString(R.string.mt_code_forwarded_call)
                    + "(" + getResources().getString(R.string.mt_code_forwarded_cf_busy) + ")";
            break;
        case SuppServiceNotification.MT_CODE_FORWARDED_CF_NO_REPLY:
            retStr = getResources().getString(R.string.mt_code_forwarded_call)
                    + "(" + getResources().getString(R.string.mt_code_forwarded_cf_no_reply) + ")";
            break;
        case SuppServiceNotification.MT_CODE_FORWARDED_CF_NOT_REACHABLE:
            retStr = getResources().getString(R.string.mt_code_forwarded_call)
                    + "(" + getResources().getString(R.string.mt_code_forwarded_cf_not_reachable) + ")";
            break;
        default:
            // Attempt to use a service we don't recognize or support
            // ("Unsupported service" or "Selected service failed")
            retStr = getResources().getString(R.string.incall_error_supp_service_unknown);
            break;
        }
        return retStr;
    }

    void doSuppCrssSuppServiceNotification(String number) {
        Connection conn = null;
        if (mForegroundCall != null) {
            int phoneType = mPhone.getPhoneType();
            if (phoneType == PhoneConstants.PHONE_TYPE_CDMA) {
                conn = mForegroundCall.getLatestConnection();
            } else if (phoneType == PhoneConstants.PHONE_TYPE_GSM) {
                conn = mForegroundCall.getEarliestConnection();
            } else {
                throw new IllegalStateException("Unexpected phone type: "
                + phoneType);
            }
        }
        if (conn == null) {
            // TODO
            if (DBG) {
                log(" Connnection is null");
            }
            return;
        } else {
            Object o = conn.getUserData();
            if (o instanceof CallerInfo) {
                CallerInfo ci = (CallerInfo) o;
                // Update CNAP information if Phone state change occurred
                if (DBG) {
                    log("SuppCrssSuppService ci.phoneNumber:" + ci.phoneNumber);
                }
                if (!ci.isVoiceMailNumber() && !ci.isEmergencyNumber()) {
                    ci.phoneNumber = number;
                }
            } else if (o instanceof PhoneUtils.CallerInfoToken) {
                CallerInfo ci = ((PhoneUtils.CallerInfoToken) o).currentInfo;
                if (!ci.isVoiceMailNumber()) {
                    ci.phoneNumber = number;
                }
            }
            conn.setUserData(o);
            updateScreen();
        }
    }

    ///M: for ALPS00543022 @{
    // when CLIP message received, the incoming call info(like CNAP name, number presentaion)
    // may changed, so need to update to user
    void doSuppCrssSuppServiceNotificationforInComing() {
        if (DBG) {
            log("doSuppCrssSuppServiceNotificationforInComing...");
        }

        Connection conn = null;
        Call ringingCall = mCM.getFirstActiveRingingCall();
        if (ringingCall != null) {
            conn = ringingCall.getLatestConnection();
        }

        if (conn != null) {
            Object o = conn.getUserData();
            if (o instanceof CallerInfo) {
                CallerInfo ci = (CallerInfo) o;
                if (ci.shouldSendToVoicemail) {
                    log("should not update Screen and Notification.");
                    return;
                }
            }
            updateScreen();
            mApp.notificationMgr.updateInCallNotification();
        }

    }
    /// @}

    void onSuppCrssSuppServiceNotification(AsyncResult r) {
        SuppCrssNotification notification = (SuppCrssNotification) r.result;
        if (DBG) {
            log("SuppCrssNotification: " + notification);
        }
        switch (notification.code) {
        case SuppCrssNotification.CRSS_CALL_WAITING:
            break;
        case SuppCrssNotification.CRSS_CALLED_LINE_ID_PREST:
            doSuppCrssSuppServiceNotification(notification.number);
            break;
        case SuppCrssNotification.CRSS_CALLING_LINE_ID_PREST:
            //ALPS00543022 need to update the incoming call info
            doSuppCrssSuppServiceNotificationforInComing();
            break;
        case SuppCrssNotification.CRSS_CONNECTED_LINE_ID_PREST:
            doSuppCrssSuppServiceNotification(PhoneNumberUtils.stringFromStringAndTOA(
                    notification.number, notification.type));
            break;
        default:
            break;
        }
        return;
    }
//MTK end

    void tryToRestoreSlidingTab() {
        if (DBG) {
            log("tryToRestoreSlidingTab");
        }
        Call fgCall = mCM.getActiveFgCall();
        Call bgCall = mCM.getFirstActiveBgCall();
        Call ringingCall = mCM.getFirstActiveRingingCall();
        if (null != fgCall) {
            log("fgCall : " + fgCall.getState());
        }
        if (null != bgCall) {
            log("bgCall : " + bgCall.getState());
        }
        if (null != ringingCall) {
            log("ringingCall : " + ringingCall.getState());
        }
        if (DBG) {
            log("mInCallTouchUi visibility = " + mInCallTouchUi.getVisibility());
        }
        if (null != fgCall && null != bgCall && null != ringingCall &&
            fgCall.getState() != Call.State.IDLE &&
            bgCall.getState() == Call.State.IDLE &&
            ringingCall.getState() == Call.State.WAITING) {
            if (DBG) {
                log("send FAKE_INCOMING_CALL_WIDGET message");
            }
            Message message = mHandler.obtainMessage(FAKE_INCOMING_CALL_WIDGET);
            mHandler.sendMessageDelayed(message, 600);
        }
    }

    /**
     * Handle a failure notification for a supplementary service
     * (i.e. conference, switch, separate, transfer, etc.).
     */
    void onSuppServiceFailed(AsyncResult r) {
        Phone.SuppService service = (Phone.SuppService) r.result;
        if (DBG) log("onSuppServiceFailed: " + service);

        int errorMessageResId;
        switch (service) {
            case SWITCH:
                // Attempt to switch foreground and background/incoming calls failed
                // ("Failed to switch calls")
                /// M: For ALPS00513091 @{
                if (PhoneUtils.getPhoneSwapStatus()) {
                    PhoneUtils.setPhoneSwapStatus(false);
                }
                /// @}
                errorMessageResId = R.string.incall_error_supp_service_switch;
                if (DualTalkUtils.isSupportDualTalk() && mDualTalk != null) {
                    if (mDualTalk.isCdmaAndGsmActive()) {
                        log("onSuppServiceFailed: can't hold, so hangup!");
                        PhoneUtils.hangup(mCM.getActiveFgCall());
                        Toast.makeText(InCallScreen.this, R.string.end_call_because_can_not_hold, Toast.LENGTH_LONG).show();
                        return;
                    }
                }
                handleSwitchFailed();
                break;

            case SEPARATE:
                // Attempt to separate a call from a conference call
                // failed ("Failed to separate out call")
                errorMessageResId = R.string.incall_error_supp_service_separate;
                break;

            case TRANSFER:
                // Attempt to connect foreground and background calls to
                // each other (and hanging up user's line) failed ("Call
                // transfer failed")
                errorMessageResId = R.string.incall_error_supp_service_transfer;
                break;

            case CONFERENCE:
                // Attempt to add a call to conference call failed
                // ("Conference call failed")
                errorMessageResId = R.string.incall_error_supp_service_conference;
                break;

            case REJECT:
                // Attempt to reject an incoming call failed
                // ("Call rejection failed")
                errorMessageResId = R.string.incall_error_supp_service_reject;
                break;

            case HANGUP:
                // Attempt to release a call failed ("Failed to release call(s)")
                errorMessageResId = R.string.incall_error_supp_service_hangup;
                break;

            case UNKNOWN:
            default:
                // Attempt to use a service we don't recognize or support
                // ("Unsupported service" or "Selected service failed")
                errorMessageResId = R.string.incall_error_supp_service_unknown;
                break;
        }

        // mSuppServiceFailureDialog is a generic dialog used for any
        // supp service failure, and there's only ever have one
        // instance at a time.  So just in case a previous dialog is
        // still around, dismiss it.
        if (mSuppServiceFailureDialog != null) {
            if (DBG) log("- DISMISSING mSuppServiceFailureDialog.");
            mSuppServiceFailureDialog.dismiss();  // It's safe to dismiss() a dialog
                                                  // that's already dismissed.
            mSuppServiceFailureDialog = null;
        }

        mSuppServiceFailureDialog = new AlertDialog.Builder(this)
                .setMessage(errorMessageResId)
                .setPositiveButton(R.string.ok, null)
                .create();
        mSuppServiceFailureDialog.getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
        mSuppServiceFailureDialog.show();

        String ProductCharacteristic = SystemProperties.get("ro.build.characteristics");

        if ((ProductCharacteristic != null) && (ProductCharacteristic.equals("tablet"))) {
          switch (service) {
              case SWITCH:
                if (mInCallTouchUi != null) {
                  if (DBG) {
                      log("onSuppServiceFailed: " + service + " cancelIncomingCallActionTime");
                  }
                  mInCallTouchUi.cancelIncomingCallActionTime();
                }
                break;
              default:
                break;
          }
        }
    }

    private void updateLocalCache() {
        mForegroundCall = mCM.getActiveFgCall();
        mBackgroundCall = mCM.getFirstActiveBgCall();
        mRingingCall = mCM.getFirstActiveRingingCall();
    }

    /**
     * Something has changed in the phone's state.  Update the UI.
     */
    private void onPhoneStateChanged(AsyncResult r) {
        PhoneConstants.State state = mCM.getState();
        if (DBG) log("onPhoneStateChanged: current state = " + state);

        if (mExtension.onPhoneStateChanged(mCM)) {
            return;
        }

        if (state != PhoneConstants.State.RINGING) {
            // if now is not Ringing, reset incoming call mute
            muteIncomingCall(false);
        }

        if (FeatureOption.MTK_VOICE_UI_SUPPORT) {
            if (null != mVoiceCommandHandler) {
                if (VoiceCommandHandler.isValidCondition()) {
                    mVoiceCommandHandler.startVoiceCommand();
                } else {
                    mVoiceCommandHandler.stopVoiceCommand();
                }
            }
        }

        enableHomeKeyDispatched(mCM.hasActiveRingingCall());

        // There's nothing to do here if we're not the foreground activity.
        // (When we *do* eventually come to the foreground, we'll do a
        // full update then.)
        if (!mIsForegroundActivity) {
            if (DBG) log("onPhoneStateChanged: Activity not in foreground! Bailing out...");
            return;
        }
        updateLocalCache();

        if (DBG) {
            log("fgCall state : " + mForegroundCall.getState());
            log("bgCall state : " + mBackgroundCall.getState());
            log("ringingCall state : " + mRingingCall.getState());
        }
        /// M: For ALPS00513091 @{
        if (PhoneUtils.getPhoneSwapStatus()) {
            if ((mForegroundCall.getState() == Call.State.ACTIVE) &&
                (mBackgroundCall.getState() == Call.State.HOLDING)) {
                PhoneUtils.setPhoneSwapStatus(false);
            }
        }
        /// @}
        updateExpandedViewState();

        // Update the onscreen UI.
        // We use requestUpdateScreen() here (which posts a handler message)
        // instead of calling updateScreen() directly, which allows us to avoid
        // unnecessary work if multiple onPhoneStateChanged() events come in all
        // at the same time.

        requestUpdateScreen();

        // Make sure we update the poke lock and wake lock when certain
        // phone state changes occur.
        mApp.updateWakeState();

        if (DBG) {
            log("onPhoneStateChanged() end");
        }
    }

    /**
     * Updates the UI after a phone connection is disconnected, as follows:
     *
     * - If this was a missed or rejected incoming call, and no other
     *   calls are active, dismiss the in-call UI immediately.  (The
     *   CallNotifier will still create a "missed call" notification if
     *   necessary.)
     *
     * - With any other disconnect cause, if the phone is now totally
     *   idle, display the "Call ended" state for a couple of seconds.
     *
     * - Or, if the phone is still in use, stay on the in-call screen
     *   (and update the UI to reflect the current state of the Phone.)
     *
     * @param r r.result contains the connection that just ended
     */
    private void onDisconnect(AsyncResult r , int msg) {
        if (DBG) {
            log("onDisconnect: start...");
        }

        mSwappingCalls = false;

        PhoneConstants.State state;
        state = mCM.getState();

        Connection c = (Connection) r.result;
        Connection.DisconnectCause cause = c.getDisconnectCause();
        if (DBG) log("onDisconnect: connection '" + c + "', cause = " + cause
                + ", showing screen: " + mApp.isShowingCallScreen());

        if (null != mCallSelectDialog && mCallSelectDialog.isShowing()) {
            mCallSelectDialog.dismiss();
            mCallSelectDialog = null;
        }

        if (mExtension.onDisconnect(c)) {
            return;
        }

        if (FeatureOption.MTK_VT3G324M_SUPPORT) {
            if (c.isVideo()) {
                mVTInCallScreen.updateVTScreen(mVTInCallScreen.getVTScreenMode());
                mHandler.removeMessages(VTManager.VT_ERROR_CALL_DISCONNECT);
            }
        }

        boolean currentlyIdle = !phoneIsInUse();
        int autoretrySetting = AUTO_RETRY_OFF;
        boolean phoneIsCdma = (mPhone.getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA);
        if (phoneIsCdma) {
            // Get the Auto-retry setting only if Phone State is IDLE,
            // else let it stay as AUTO_RETRY_OFF
            if (currentlyIdle) {
                autoretrySetting = android.provider.Settings.Global.getInt(mPhone.getContext().
                        getContentResolver(), android.provider.Settings.Global.CALL_AUTO_RETRY, 0);
            }
        }

        // for OTA Call, only if in OTA NORMAL mode, handle OTA END scenario
        if ((mApp.inCallUiState.inCallScreenMode == InCallScreenMode.OTA_NORMAL)
                && ((mApp.cdmaOtaProvisionData != null)
                && (!mApp.cdmaOtaProvisionData.inOtaSpcState))) {
            setInCallScreenMode(InCallScreenMode.OTA_ENDED);
            updateScreen();
            return;
        } else if ((mApp.inCallUiState.inCallScreenMode == InCallScreenMode.OTA_ENDED)
                   || ((mApp.cdmaOtaProvisionData != null)
                       && mApp.cdmaOtaProvisionData.inOtaSpcState)) {
           if (DBG) log("onDisconnect: OTA Call end already handled");
           return;
        }

        // Any time a call disconnects, clear out the "history" of DTMF
        // digits you typed (to make sure it doesn't persist from one call
        // to the next.)
        mDialer.clearDigits();

        if (FeatureOption.MTK_PHONE_VT_VOICE_ANSWER
                && FeatureOption.MTK_VT3G324M_SUPPORT) {
            if (getInVoiceAnswerVideoCall()) {
                return;
            }
        }

        // Under certain call disconnected states, we want to alert the user
        // with a dialog instead of going through the normal disconnect
        // routine.
        if (cause == Connection.DisconnectCause.CALL_BARRED) {
            showGenericErrorDialog(R.string.callFailed_cb_enabled, false);
            return;
        } else if (cause == Connection.DisconnectCause.FDN_BLOCKED
                && CallStatusCode.FDN_BLOCKED == mApp.inCallUiState.getPendingCallStatusCode()) {
            showGenericErrorDialog(R.string.callFailed_fdn_only, false);
            return;
        } else if (cause == Connection.DisconnectCause.CS_RESTRICTED) {
            showGenericErrorDialog(R.string.callFailed_dsac_restricted, false);
            return;
        } else if (cause == Connection.DisconnectCause.CS_RESTRICTED_EMERGENCY) {
            showGenericErrorDialog(R.string.callFailed_dsac_restricted_emergency, false);
            return;
        } else if (cause == Connection.DisconnectCause.CS_RESTRICTED_NORMAL) {
            showGenericErrorDialog(R.string.callFailed_dsac_restricted_normal, false);
            return;
        } else if ((FeatureOption.MTK_VT3G324M_SUPPORT) && (c.isVideo())) {
            if (DBG) {
                Log.d("InCallScreen", "Check VT call dropback and IOT call disconnect UI");
            }
            boolean isReturnImmediately = false;
            
            //ALPS00270737
            //for dualtalk case, the incoming call maybe come when there is an dialing video call
            boolean isNeededCheck = DualTalkUtils.isSupportDualTalk() && (mCM.getState() == PhoneConstants.State.RINGING);
            
            if (!isNeededCheck){
                final int slotId = GeminiRegister.getSlotIdByRegisterEvent(msg, PHONE_DISCONNECT_GEMINI);
                if (slotId != -1) {
                    isReturnImmediately = mVTInCallScreen.onDisconnectVT(c, slotId, mIsForegroundActivity);
                }
            }
            if (isReturnImmediately) {
                return;
            }
        }

        if (phoneIsCdma) {
            Call.State callState = mApp.notifier.getPreviousCdmaCallState();
            if ((callState == Call.State.ACTIVE)
                    && (cause != Connection.DisconnectCause.INCOMING_MISSED)
                    && (cause != Connection.DisconnectCause.NORMAL)
                    && (cause != Connection.DisconnectCause.LOCAL)
                    && (cause != Connection.DisconnectCause.INCOMING_REJECTED)) {
                showCallLostDialog();
            } else if ((callState == Call.State.DIALING || callState == Call.State.ALERTING)
                        && (cause != Connection.DisconnectCause.INCOMING_MISSED)
                        && (cause != Connection.DisconnectCause.NORMAL)
                        && (cause != Connection.DisconnectCause.LOCAL)
                        && (cause != Connection.DisconnectCause.INCOMING_REJECTED)) {

                if (mApp.inCallUiState.needToShowCallLostDialog) {
                    // Show the dialog now since the call that just failed was a retry.
                    showCallLostDialog();
                    mApp.inCallUiState.needToShowCallLostDialog = false;
                } else {
                    if (autoretrySetting == AUTO_RETRY_OFF) {
                        // Show the dialog for failed call if Auto Retry is OFF in Settings.
                        showCallLostDialog();
                        mApp.inCallUiState.needToShowCallLostDialog = false;
                    } else {
                        // Set the needToShowCallLostDialog flag now, so we'll know to show
                        // the dialog if *this* call fails.
                        mApp.inCallUiState.needToShowCallLostDialog = true;
                    }
                }
            }
        }

        // Explicitly clean up up any DISCONNECTED connections
        // in a conference call.
        // [Background: Even after a connection gets disconnected, its
        // Connection object still stays around for a few seconds, in the
        // DISCONNECTED state.  With regular calls, this state drives the
        // "call ended" UI.  But when a single person disconnects from a
        // conference call there's no "call ended" state at all; in that
        // case we blow away any DISCONNECTED connections right now to make sure
        // the UI updates instantly to reflect the current state.]
        final Call call = c.getCall();
        if (call != null) {
            // We only care about situation of a single caller
            // disconnecting from a conference call.  In that case, the
            // call will have more than one Connection (including the one
            // that just disconnected, which will be in the DISCONNECTED
            // state) *and* at least one ACTIVE connection.  (If the Call
            // has *no* ACTIVE connections, that means that the entire
            // conference call just ended, so we *do* want to show the
            // "Call ended" state.)
            List<Connection> connections = call.getConnections();
            if (connections != null && connections.size() > 1) {
                for (Connection conn : connections) {
                    if (conn.getState() == Call.State.ACTIVE) {
                        // This call still has at least one ACTIVE connection!
                        // So blow away any DISCONNECTED connections
                        // (including, presumably, the one that just
                        // disconnected from this conference call.)

                        // We also force the wake state to refresh, just in
                        // case the disconnected connections are removed
                        // before the phone state change.
                        if (VDBG) log("- Still-active conf call; clearing DISCONNECTED...");
                        mApp.updateWakeState();
                        mCM.clearDisconnected();  // This happens synchronously.
                        break;
                    }
                }
            }
        }

        // Note: see CallNotifier.onDisconnect() for some other behavior
        // that might be triggered by a disconnect event, like playing the
        // busy/congestion tone.

        // Stash away some info about the call that just disconnected.
        // (This might affect what happens after we exit the InCallScreen; see
        // delayedCleanupAfterDisconnect().)
        // TODO: rather than stashing this away now and then reading it in
        // delayedCleanupAfterDisconnect(), it would be cleaner to just pass
        // this as an argument to delayedCleanupAfterDisconnect() (if we call
        // it directly) or else pass it as a Message argument when we post the
        // DELAYED_CLEANUP_AFTER_DISCONNECT message.
        //mLastDisconnectCause = cause;

        // We bail out immediately (and *don't* display the "call ended"
        // state at all) if this was an incoming call.
        boolean bailOutImmediately =
                ((cause == Connection.DisconnectCause.INCOMING_MISSED)
                 || (cause == Connection.DisconnectCause.INCOMING_REJECTED))
                && currentlyIdle;

        //Add for cr: alps00044084 
        //because the internal reject call disconnet message still coming, don't show it.
        if (cause == Connection.DisconnectCause.INCOMING_REJECTED 
                && PhoneUtils.getShouldSendToVoiceMailFlag(c)
                && (mRingingCall.getState() ==  Call.State.DISCONNECTING
                        || mRingingCall.getState() ==  Call.State.DISCONNECTED
                        || mRingingCall.getState() == Call.State.IDLE)) {
            //also end incallscreen if there is no call exist, this can avoid the incallscreen not exit
            final int index = GeminiUtils.getIndexInArray(msg, PHONE_DISCONNECT_GEMINI);
            delayedCleanupAfterDisconnect(DELAYED_CLEANUP_AFTER_DISCONNECT_GEMINI[index]);

          //clear the disconnected connection.
            mRingingCall.getPhone().clearDisconnected();
            return ;
        }

        boolean showingQuickResponseDialog =
                mRespondViaSmsManager != null && mRespondViaSmsManager.isShowingPopup();

        // Note: we also do some special handling for the case when a call
        // disconnects with cause==OUT_OF_SERVICE while making an
        // emergency call from airplane mode.  That's handled by
        // EmergencyCallHelper.onDisconnect().

        if (bailOutImmediately && showingQuickResponseDialog) {
            if (DBG) log("- onDisconnect: Respond-via-SMS dialog is still being displayed...");
            //ALPS00418549
            InCallUiState.sLastInCallScreenStatus = InCallUiState.INCALLSCREEN_NOT_EXIT_NOT_FORGROUND;

            // Do *not* exit the in-call UI yet!
            // If the call was an incoming call that was missed *and* the user is using
            // quick response screen, we keep showing the screen for a moment, assuming the
            // user wants to reply the call anyway.
            //
            // For this case, we will exit the screen when:
            // - the message is sent (RespondViaSmsManager)
            // - the message is canceled (RespondViaSmsManager), or
            // - when the whole in-call UI becomes background (onPause())
        } else if (bailOutImmediately) {
            if (DBG) log("- onDisconnect: bailOutImmediately...");

            // Exit the in-call UI!
            // (This is basically the same "delayed cleanup" we do below,
            // just with zero delay.  Since the Phone is currently idle,
            // this call is guaranteed to immediately finish this activity.)

            updateInCallTouchUi();
            final int index = GeminiUtils.getIndexInArray(msg, PHONE_DISCONNECT_GEMINI);
            if (DBG) log("- onDisconnect: bailOutImmediately...index=" + index + " msg=" + msg);
            if (index != -1) {
                delayedCleanupAfterDisconnect(DELAYED_CLEANUP_AFTER_DISCONNECT_GEMINI[index]);
            }
        } else {
            if (DBG) log("- onDisconnect: delayed bailout...");
            // Stay on the in-call screen for now.  (Either the phone is
            // still in use, or the phone is idle but we want to display
            // the "call ended" state for a couple of seconds.)

            // Switch to the special "Call ended" state when the phone is idle
            // but there's still a call in the DISCONNECTED state:
            if (currentlyIdle
                && (mCM.hasDisconnectedFgCall() || mCM.hasDisconnectedBgCall())) {
                if (DBG) log("- onDisconnect: switching to 'Call ended' state...");
                setInCallScreenMode(InCallScreenMode.CALL_ENDED);
            }

            // Force a UI update in case we need to display anything
            // special based on this connection's DisconnectCause
            // (see CallCard.getCallFailedString()).
            updateScreen();

            // Some other misc cleanup that we do if the call that just
            // disconnected was the foreground call.
            final boolean hasActiveCall = mCM.hasActiveFgCall();
            if (!hasActiveCall) {
                if (DBG) log("- onDisconnect: cleaning up after FG call disconnect...");

                // Dismiss any dialogs which are only meaningful for an
                // active call *and* which become moot if the call ends.
                if (mWaitPromptDialog != null) {
                    if (VDBG) log("- DISMISSING mWaitPromptDialog.");
                    mWaitPromptDialog.dismiss();  // safe even if already dismissed
                    mWaitPromptDialog = null;
                }
                if (mWildPromptDialog != null) {
                    if (VDBG) log("- DISMISSING mWildPromptDialog.");
                    mWildPromptDialog.dismiss();  // safe even if already dismissed
                    mWildPromptDialog = null;
                }
                if (mPausePromptDialog != null) {
                    if (DBG) log("- DISMISSING mPausePromptDialog.");
                    mPausePromptDialog.dismiss();  // safe even if already dismissed
                    mPausePromptDialog = null;
                }
            }

            // Updating the screen wake state is done in onPhoneStateChanged().


            // CDMA: We only clean up if the Phone state is IDLE as we might receive an
            // onDisconnect for a Call Collision case (rare but possible).
            // For Call collision cases i.e. when the user makes an out going call
            // and at the same time receives an Incoming Call, the Incoming Call is given
            // higher preference. At this time framework sends a disconnect for the Out going
            // call connection hence we should *not* bring down the InCallScreen as the Phone
            // State would be RINGING
            if (mPhone.getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA) {
                if (!currentlyIdle) {
                    // Clean up any connections in the DISCONNECTED state.
                    // This is necessary cause in CallCollision the foreground call might have
                    // connections in DISCONNECTED state which needs to be cleared.
                    mCM.clearDisconnected();

                    // The phone is still in use.  Stay here in this activity.
                    // But we don't need to keep the screen on.
                    if (DBG) log("onDisconnect: Call Collision case - staying on InCallScreen.");
                    if (DBG) PhoneUtils.dumpCallState(mPhone);
                    return;
                }
            }

            // This is onDisconnect() request from the last phone call; no available call anymore.
            //
            // When the in-call UI is in background *because* the screen is turned off (unlike the
            // other case where the other activity is being shown), we wake up the screen and
            // show "DISCONNECTED" state once, with appropriate elapsed time. After showing that
            // we *must* bail out of the screen again, showing screen lock if needed.
            //
            // See also comments for isForegroundActivityForProximity()
            //
            // TODO: Consider moving this to CallNotifier. This code assumes the InCallScreen
            // never gets destroyed. For this exact case, it works (since InCallScreen won't be
            // destroyed), while technically this isn't right; Activity may be destroyed when
            // in background.
            if (currentlyIdle && !isForegroundActivity() && isForegroundActivityForProximity()) {
                log("Force waking up the screen to let users see \"disconnected\" state");
                if (call != null) {
                    mCallCard.updateElapsedTimeWidget(call);
                }
                // This variable will be kept true until the next InCallScreen#onPause(), which
                // forcibly turns it off regardless of the situation (for avoiding unnecessary
                // confusion around this special case).
                mApp.inCallUiState.showAlreadyDisconnectedState = true;

                // Finally request wake-up..
                mApp.wakeUpScreen();

                // InCallScreen#onResume() will set DELAYED_CLEANUP_AFTER_DISCONNECT message,
                // so skip the following section.
                return;
            }

            // Finally, arrange for delayedCleanupAfterDisconnect() to get
            // called after a short interval (during which we display the
            // "call ended" state.)  At that point, if the
            // Phone is idle, we'll finish out of this activity.
            final int callEndedDisplayDelay;
            switch (cause) {
                // When the local user hanged up the ongoing call, it is ok to dismiss the screen
                // soon. In other cases, we show the "hung up" screen longer.
                //
                // - For expected reasons we will use CALL_ENDED_LONG_DELAY.
                // -- when the peer hanged up the call
                // -- when the local user rejects the incoming call during the other ongoing call
                // (TODO: there may be other cases which should be in this category)
                //
                // - For other unexpected reasons, we will use CALL_ENDED_EXTRA_LONG_DELAY,
                //   assuming the local user wants to confirm the disconnect reason.
                case LOCAL:
                    callEndedDisplayDelay = CALL_ENDED_SHORT_DELAY;
                    break;
                case NORMAL:
                case INCOMING_REJECTED:
                    callEndedDisplayDelay = CALL_ENDED_LONG_DELAY;
                    break;
                default:
                    callEndedDisplayDelay = CALL_ENDED_EXTRA_LONG_DELAY;
                    break;
            }

            final int index = GeminiUtils.getIndexInArray(msg, PHONE_DISCONNECT_GEMINI);
            log("onDisconnect() PHONE_DISCONNECT : index=" + index);
            if (index != -1) {
                mHandler.removeMessages(DELAYED_CLEANUP_AFTER_DISCONNECT_GEMINI[index]);
                mHandler.sendEmptyMessageDelayed(DELAYED_CLEANUP_AFTER_DISCONNECT_GEMINI[index], callEndedDisplayDelay);
            }
        }

        // Remove 3way timer (only meaningful for CDMA)
        // TODO: this call needs to happen in the CallController, not here.
        // (It should probably be triggered by the CallNotifier's onDisconnect method.)
        // mHandler.removeMessages(THREEWAY_CALLERINFO_DISPLAY_DONE);
    }

    /**
     * Brings up the "MMI Started" dialog.
     */
    /* TODO: sort out MMI code (probably we should remove this method entirely). See also
       MMI handling code in onResume()
    private void onMMIInitiate(AsyncResult r, int simId) {
        if (VDBG) log("onMMIInitiate()...  AsyncResult r = " + r);

        // Watch out: don't do this if we're not the foreground activity,
        // mainly since in the Dialog.show() might fail if we don't have a
        // valid window token any more...
        // (Note that this exact sequence can happen if you try to start
        // an MMI code while the radio is off or out of service.)
        if (!mIsForegroundActivity) {
            if (VDBG) log("Activity not in foreground! Bailing out...");
            mHandler.sendMessageDelayed(Message.obtain(mHandler, DELAY_TO_SHOW_MMI_INIT,
                    simId, 0, r), 200);
            return;
        }

        // Also, if any other dialog is up right now (presumably the
        // generic error dialog displaying the "Starting MMI..."  message)
        // take it down before bringing up the real "MMI Started" dialog
        // in its place.
        dismissAllDialogs();

        MmiCode mmiCode = (MmiCode) r.result;
        if (VDBG) log("  - MmiCode: " + mmiCode);
//MTK begin:
        Message message = null;
        if (simId == PhoneConstants.GEMINI_SIM_1) {
            message = Message.obtain(mHandler, PhoneGlobals.MMI_CANCEL);
        } else if (simId == PhoneConstants.GEMINI_SIM_2) {
            message = Message.obtain(mHandler, PhoneGlobals.MMI_CANCEL2);
        } else {
            log("onMMIInitiate()... no such simId");
        }
//MTK end
        mMmiStartedDialog = PhoneUtils.displayMMIInitiate(this, mmiCode,
                                                          message, mMmiStartedDialog);
    }*/

    /**
     * Handles an MMI_CANCEL event, which is triggered by the button
     * (labeled either "OK" or "Cancel") on the "MMI Started" dialog.
     * @see PhoneUtils#cancelMmiCode(Phone)
     */
    private void onMMICancel(int simId) {
        if (VDBG) log("onMMICancel()...");

        // First of all, cancel the outstanding MMI code (if possible.)
        PhoneUtils.cancelMmiCodeExt(mPhone, simId);

        // Regardless of whether the current MMI code was cancelable, the
        // PhoneGlobals will get an MMI_COMPLETE event very soon, which will
        // take us to the MMI Complete dialog (see
        // PhoneUtils.displayMMIComplete().)
        //
        // But until that event comes in, we *don't* want to stay here on
        // the in-call screen, since we'll be visible in a
        // partially-constructed state as soon as the "MMI Started" dialog
        // gets dismissed.  So let's forcibly bail out right now.
        if (DBG) log("onMMICancel: finishing InCallScreen...");
        dismissAllDialogs();
        if (mCM.getState() == PhoneConstants.State.IDLE) {
            endInCallScreenSession();
        } else {
            log("Got MMI_COMPLETE, Phone isn't in idle, don't finishing InCallScreen...");
        }
        if (null != mMmiStartedDialog) {
            mMmiStartedDialog.dismiss();
            mMmiStartedDialog = null;
            log("Got MMI_COMPLETE, Phone isn't in idle, dismiss the start progress dialog...");
        }
    }

    /**
     * Handles an MMI_COMPLETE event, which is triggered by telephony,
     * implying MMI
     */
    private void onMMIComplete(MmiCode mmiCode) {
        //if (hasMessages(DELAY_TO_SHOW_MMI_INIT)) {
        //    removeMessages(DELAY_TO_SHOW_MMI_INIT);
        //}
        // Check the code to see if the request is ready to
        // finish, this includes any MMI state that is not
        // PENDING.
        // if phone is a CDMA phone display feature code completed message
        int phoneType = PhoneConstants.PHONE_TYPE_GSM;
        if (mmiCode instanceof GsmMmiCode) {
            phoneType = PhoneConstants.PHONE_TYPE_GSM;
        } else if (mmiCode instanceof CdmaMmiCode) {
            phoneType = PhoneConstants.PHONE_TYPE_CDMA;
        } else {
            phoneType = mPhone.getPhoneType();
        }
        if (phoneType == PhoneConstants.PHONE_TYPE_CDMA) {
            PhoneUtils.displayMMIComplete(mPhone, mApp, mmiCode, null, null);
        } else if (phoneType == PhoneConstants.PHONE_TYPE_GSM) {
            if (mmiCode.getState() != MmiCode.State.PENDING) {
                if (DBG) log("Got MMI_COMPLETE, finishing InCallScreen...");
                if (mCM.getState() == PhoneConstants.State.IDLE) {
                    endInCallScreenSession();
                } else {
                    log("Got MMI_COMPLETE, Phone isn't in idle, don't finishing InCallScreen...");
                }
                
                if (null != mMmiStartedDialog) {
                    mMmiStartedDialog.dismiss();
                    mMmiStartedDialog = null;
                    log("Got MMI_COMPLETE, Phone isn't in idle, dismiss the start progress dialog...");
                }
            }
        }
    }

    /**
     * Handles the POST_ON_DIAL_CHARS message from the Phone
     * (see our call to mPhone.setOnPostDialCharacter() above.)
     *
     * TODO: NEED TO TEST THIS SEQUENCE now that we no longer handle
     * "dialable" key events here in the InCallScreen: we do directly to the
     * Dialer UI instead.  Similarly, we may now need to go directly to the
     * Dialer to handle POST_ON_DIAL_CHARS too.
     */
    private void handlePostOnDialChars(AsyncResult r, char ch) {
        Connection c = (Connection) r.result;

        if (c != null) {
            Connection.PostDialState state =
                    (Connection.PostDialState) r.userObj;

            if (VDBG) log("handlePostOnDialChar: state = " +
                    state + ", ch = " + ch);

            switch (state) {
                case STARTED:
                    mDialer.stopLocalToneIfNeeded();
                    if (mPauseInProgress) {
                        /**
                         * Note that on some devices, this will never happen,
                         * because we will not ever enter the PAUSE state.
                         */
                        showPausePromptDialog(c, mPostDialStrAfterPause);
                    }
                    mPauseInProgress = false;
                    mDialer.startLocalToneIfNeeded(ch);

                    // TODO: is this needed, now that you can't actually
                    // type DTMF chars or dial directly from here?
                    // If so, we'd need to yank you out of the in-call screen
                    // here too (and take you to the 12-key dialer in "in-call" mode.)
                    // displayPostDialedChar(ch);
                    break;

                case WAIT:
                    // wait shows a prompt.
                    if (DBG) log("handlePostOnDialChars: show WAIT prompt...");
                    mDialer.stopLocalToneIfNeeded();
                    String postDialStr = c.getRemainingPostDialString();
                    showWaitPromptDialog(c, postDialStr);
                    break;

                case WILD:
                    if (DBG) log("handlePostOnDialChars: show WILD prompt");
                    mDialer.stopLocalToneIfNeeded();
                    showWildPromptDialog(c);
                    break;

                case COMPLETE:
                    mDialer.stopLocalToneIfNeeded();
                    break;

                case PAUSE:
                    // pauses for a brief period of time then continue dialing.
                    mDialer.stopLocalToneIfNeeded();
                    mPostDialStrAfterPause = c.getRemainingPostDialString();
                    mPauseInProgress = true;
                    break;

                default:
                    break;
            }
        }
    }

    /**
     * Pop up an alert dialog with OK and Cancel buttons to allow user to
     * Accept or Reject the WAIT inserted as part of the Dial string.
     */
    private void showWaitPromptDialog(final Connection c, String postDialStr) {
        if (DBG) log("showWaitPromptDialogChoice: '" + postDialStr + "'...");

        Resources r = getResources();
        StringBuilder buf = new StringBuilder();
        buf.append(r.getText(R.string.wait_prompt_str));
        buf.append(postDialStr);

        // if (DBG) log("- mWaitPromptDialog = " + mWaitPromptDialog);
        if (mWaitPromptDialog != null) {
            if (mWaitPromptDialog.isShowing()) {
                if (DBG) {
                    log("mWaitPromptDialog already show");
                }
                return;
            }
        }

        mWaitPromptDialog = new AlertDialog.Builder(this)
                .setMessage(buf.toString())
                .setPositiveButton(R.string.pause_prompt_yes,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int whichButton) {
                                if (DBG) log("handle WAIT_PROMPT_CONFIRMED, proceed...");
                                c.proceedAfterWaitChar();
                            }
                        })
                .setNegativeButton(R.string.pause_prompt_no,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int whichButton) {
                                if (DBG) log("handle POST_DIAL_CANCELED!");
                                c.cancelPostDial();
                            }
                        })
                .create();
        mWaitPromptDialog.getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_BLUR_BEHIND);

        mWaitPromptDialog.show();
    }

    /**
     * Pop up an alert dialog which waits for 2 seconds for each P (Pause) Character entered
     * as part of the Dial String.
     */
    private void showPausePromptDialog(final Connection c, String postDialStrAfterPause) {
        Resources r = getResources();
        StringBuilder buf = new StringBuilder();
        buf.append(r.getText(R.string.pause_prompt_str));
        buf.append(postDialStrAfterPause);

        if (mPausePromptDialog != null) {
            if (DBG) log("- DISMISSING mPausePromptDialog.");
            mPausePromptDialog.dismiss();  // safe even if already dismissed
            mPausePromptDialog = null;
        }

        mPausePromptDialog = new AlertDialog.Builder(this)
                .setMessage(buf.toString())
                .create();
        mPausePromptDialog.show();
        // 2 second timer
        Message msg = Message.obtain(mHandler, EVENT_PAUSE_DIALOG_COMPLETE);
        mHandler.sendMessageDelayed(msg, PAUSE_PROMPT_DIALOG_TIMEOUT);
    }

    private View createWildPromptView() {
        LinearLayout result = new LinearLayout(this);
        result.setOrientation(LinearLayout.VERTICAL);
        result.setPadding(5, 5, 5, 5);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);

        TextView promptMsg = new TextView(this);
        promptMsg.setTextSize(14);
        promptMsg.setTypeface(Typeface.DEFAULT_BOLD);
        promptMsg.setText(getResources().getText(R.string.wild_prompt_str));

        result.addView(promptMsg, lp);

        mWildPromptText = new EditText(this);
        mWildPromptText.setKeyListener(DialerKeyListener.getInstance());
        mWildPromptText.setMovementMethod(null);
        mWildPromptText.setTextSize(14);
        mWildPromptText.setMaxLines(1);
        mWildPromptText.setHorizontallyScrolling(true);
        mWildPromptText.setBackgroundResource(android.R.drawable.editbox_background);

        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
        lp2.setMargins(0, 3, 0, 0);

        result.addView(mWildPromptText, lp2);

        return result;
    }

    private void showWildPromptDialog(final Connection c) {
        View v = createWildPromptView();

        if (mWildPromptDialog != null) {
            if (VDBG) log("- DISMISSING mWildPromptDialog.");
            mWildPromptDialog.dismiss();  // safe even if already dismissed
            mWildPromptDialog = null;
        }

        mWildPromptDialog = new AlertDialog.Builder(this)
                .setView(v)
                .setPositiveButton(
                        R.string.send_button,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int whichButton) {
                                if (VDBG) log("handle WILD_PROMPT_CHAR_ENTERED, proceed...");
                                String replacement = null;
                                if (mWildPromptText != null) {
                                    replacement = mWildPromptText.getText().toString();
                                    mWildPromptText = null;
                                }
                                c.proceedAfterWildChar(replacement);
                                mApp.pokeUserActivity();
                            }
                        })
                .setOnCancelListener(
                        new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                if (VDBG) log("handle POST_DIAL_CANCELED!");
                                c.cancelPostDial();
                                mApp.pokeUserActivity();
                            }
                        })
                .create();
        mWildPromptDialog.getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
        mWildPromptDialog.show();

        mWildPromptText.requestFocus();
    }

    /**
     * Updates the state of the in-call UI based on the current state of
     * the Phone.  This call has no effect if we're not currently the
     * foreground activity.
     *
     * This method is only allowed to be called from the UI thread (since it
     * manipulates our View hierarchy).  If you need to update the screen from
     * some other thread, or if you just want to "post a request" for the screen
     * to be updated (rather than doing it synchronously), call
     * requestUpdateScreen() instead.
     *
     * Right now this method will update UI visibility immediately, with no animation.
     * TODO: have animate flag here and use it anywhere possible.
     */
    private void updateScreen() {
        if (DBG) log("updateScreen()...");
        final InCallScreenMode inCallScreenMode = mApp.inCallUiState.inCallScreenMode;
        if (VDBG) {
            PhoneConstants.State state = mCM.getState();
            log("  - phone state = " + state);
            log("  - inCallScreenMode = " + inCallScreenMode);
        }

        if (mExtension.updateScreen(mCM, mIsForegroundActivity)) {
            return;
        }

        // if popup RespondViaSmsManager dialog but accept with bluetooth,
        // RespondViaSmsManager dialog will not hide even if call is connected,
        // so hide here to prevent such problem
        if (!mRingingCall.getState().isRinging()) {
            if (mRespondViaSmsManager != null) {
                mRespondViaSmsManager.dismissPopup();  // safe even if already dismissed
            }
        }

        // please do not return before calling updateCallTime()
        // or else, call time thread may not stop after call end
        updateCallTime();

        if (FeatureOption.MTK_VT3G324M_SUPPORT && !VTCallUtils.isVTIdle()) {
            if (mForegroundCall.getState().isAlive() && !mRingingCall.getState().isRinging()) {
                if (mForegroundCall.getLatestConnection().isVideo()) {
                    mVTInCallScreen.setVTScreenMode(Constants.VTScreenMode.VT_SCREEN_OPEN);
                    mVTInCallScreen.updateVTScreen(mVTInCallScreen.getVTScreenMode());
                    mVTInCallScreen.setVTVisible(true);
                    //need set enableSystemBarNavigation when answer vt call asap, because vt init is slow.
                    mApp.notificationMgr.statusBarHelper.enableSystemBarNavigation(true);
                    return;
                } else {
                    mVTInCallScreen.updateVTScreen(mVTInCallScreen.getVTScreenMode());
                }
            } else {
                mVTInCallScreen.updateVTScreen(mVTInCallScreen.getVTScreenMode());
            }
        }
        // Don't update anything if we're not in the foreground (there's
        // no point updating our UI widgets since we're not visible!)
        // Also note this check also ensures we won't update while we're
        // in the middle of pausing, which could cause a visible glitch in
        // the "activity ending" transition.
        if (!mIsForegroundActivity) {
            if (DBG) log("- updateScreen: not the foreground Activity! Bailing out...");
            return;
        }

        if (inCallScreenMode == InCallScreenMode.OTA_NORMAL) {
            if (DBG) log("- updateScreen: OTA call state NORMAL (NOT updating in-call UI)...");
            mCallCard.setVisibility(View.GONE);
            if (mApp.otaUtils != null) {
                mApp.otaUtils.otaShowProperScreen();
            } else {
                Log.w(LOG_TAG, "OtaUtils object is null, not showing any screen for that.");
            }
            return;  // Return without updating in-call UI.
        } else if (inCallScreenMode == InCallScreenMode.OTA_ENDED) {
            if (DBG) log("- updateScreen: OTA call ended state (NOT updating in-call UI)...");
            mCallCard.setVisibility(View.GONE);
            // Wake up the screen when we get notification, good or bad.
            mApp.wakeUpScreen();
            if (mApp.cdmaOtaScreenState.otaScreenState
                    == CdmaOtaScreenState.OtaScreenState.OTA_STATUS_ACTIVATION) {
                if (DBG) log("- updateScreen: OTA_STATUS_ACTIVATION");
                if (mApp.otaUtils != null) {
                    if (DBG) log("- updateScreen: mApp.otaUtils is not null, "
                                  + "call otaShowActivationScreen");
                    mApp.otaUtils.otaShowActivateScreen();
                }
            } else {
                if (DBG) log("- updateScreen: OTA Call end state for Dialogs");
                if (mApp.otaUtils != null) {
                    if (DBG) log("- updateScreen: Show OTA Success Failure dialog");
                    mApp.otaUtils.otaShowSuccessFailure();
                }
            }
            return;  // Return without updating in-call UI.
        } else if (inCallScreenMode == InCallScreenMode.MANAGE_CONFERENCE) {
            if (DBG) log("- updateScreen: manage conference mode (NOT updating in-call UI)...");
 
            /** M: New Feature Phone Landscape UI @{ */
            if (PhoneUtils.isLandscape(InCallScreen.this)) {
                // CallCard will be hidded because of conference call management panel
                mSecCallInfo.setVisibility(View.GONE);
            } else {
                // CallCard won't be hidded in landscape view
            /** @ }*/          
                mCallCard.setVisibility(View.GONE);
            /** M: New Feature Phone Landscape UI @{ */
            }
            /** @ }*/

            updateManageConferencePanelIfNecessary();
            return;  // Return without updating in-call UI.
        } else if (inCallScreenMode == InCallScreenMode.CALL_ENDED) {
            if (DBG) log("- updateScreen: call ended state...");
            // Continue with the rest of updateScreen() as usual, since we do
            // need to update the background (to the special "call ended" color)
            // and the CallCard (to show the "Call ended" label.)
        }

        if (DBG) log("- updateScreen: updating the in-call UI...");
        // Note we update the InCallTouchUi widget before the CallCard,
        // since the CallCard adjusts its size based on how much vertical
        // space the InCallTouchUi widget needs.
        updateInCallTouchUi();
        /** M: New Feature Phone Landscape UI @{ */
        //if (PhoneUtils.isLandscape(InCallScreen.this)) {
        /** @ }*/
        mCallCard.updateState(mCM);
        //}

        /// M: For ALPS00558420 @{
        // update mVoiceRecorderIcon
        //
        // MTK add
        if (FeatureOption.MTK_PHONE_VOICE_RECORDING) {
            handleRecordProc();
        }
        /// @}

        // If an incoming call is ringing, make sure the dialpad is
        // closed.  (We do this to make sure we're not covering up the
        // "incoming call" UI.)
        if (mCM.getState() == PhoneConstants.State.RINGING) {
            if (mDialer.isOpened()) {
              Log.i(LOG_TAG, "During RINGING state we force hiding dialpad.");
              closeDialpadInternal(false);  // don't do the "closing" animation
            }

            // At this point, we are guranteed that the dialer is closed.
            // This means that it is safe to clear out the "history" of DTMF digits
            // you may have typed into the previous call (so you don't see the
            // previous call's digits if you answer this call and then bring up the
            // dialpad.)
            //
            // TODO: it would be more precise to do this when you *answer* the
            // incoming call, rather than as soon as it starts ringing, but
            // the InCallScreen doesn't keep enough state right now to notice
            // that specific transition in onPhoneStateChanged().
            // TODO: This clears out the dialpad context as well so when a second
            // call comes in while a voicemail call is happening, the voicemail
            // dialpad will no longer have the "Voice Mail" context. It's a small
            // case so not terribly bad, but we need to maintain a better
            // call-to-callstate mapping before we can fix this.
            mDialer.clearDigits();
        }


        // Now that we're sure DTMF dialpad is in an appropriate state, reflect
        // the dialpad state into CallCard
        updateCallCardVisibilityPerDialerState(false);

        updateProgressIndication();

        // Forcibly take down all dialog if an incoming call is ringing.
        if (mCM.hasActiveRingingCall()) {
            dismissAllDialogs();
            if (FeatureOption.MTK_VT3G324M_SUPPORT) {
                mVTInCallScreen.dismissVTDialogs();
                // Sometimes, InCallScreen with VT GUI is not disappear last time,
                // but new incoming call is coming, so needs close VT call here
                // By the way, current design for VT has some point to improve in the future:
                // 1. set VT mode as one mode of InCallScreen, such as NORMAL
                // 2. seperate updateScreen() and updateVTScreen(), that is, 
                //    updateScreen() should not include VT related code
                if (Constants.VTScreenMode.VT_SCREEN_OPEN == mVTInCallScreen.getVTScreenMode()) {
                    mVTInCallScreen.setVTVisible(false);
                    mVTInCallScreen.setVTScreenMode(Constants.VTScreenMode.VT_SCREEN_CLOSE);
                }
            }
        } else {
            // Wait prompt dialog is not currently up.  But it *should* be
            // up if the FG call has a connection in the WAIT state and
            // the phone isn't ringing.
            String postDialStr = null;
            List<Connection> fgConnections = mCM.getFgCallConnections();
            int phoneType = mCM.getFgPhone().getPhoneType();
            if (phoneType == PhoneConstants.PHONE_TYPE_CDMA) {
                Connection fgLatestConnection = mCM.getFgCallLatestConnection();
                if (mApp.cdmaPhoneCallState.getCurrentCallState() ==
                        CdmaPhoneCallState.PhoneCallState.CONF_CALL) {
                    for (Connection cn : fgConnections) {
                        if ((cn != null) && (cn.getPostDialState() ==
                                Connection.PostDialState.WAIT)) {
                            cn.cancelPostDial();
                        }
                    }
                } else if ((fgLatestConnection != null)
                     && (fgLatestConnection.getPostDialState() == Connection.PostDialState.WAIT)) {
                    if (DBG) log("show the Wait dialog for CDMA");
                    postDialStr = fgLatestConnection.getRemainingPostDialString();
                    showWaitPromptDialog(fgLatestConnection, postDialStr);
                }
            } else if ((phoneType == PhoneConstants.PHONE_TYPE_GSM)
                    || (phoneType == PhoneConstants.PHONE_TYPE_SIP)) {
                for (Connection cn : fgConnections) {
                    if ((cn != null) && (cn.getPostDialState() == Connection.PostDialState.WAIT)) {
                        postDialStr = cn.getRemainingPostDialString();
                        showWaitPromptDialog(cn, postDialStr);
                    }
                }
            } else {
                throw new IllegalStateException("Unexpected phone type: " + phoneType);
            }
        }
    }

    /**
     * (Re)synchronizes the onscreen UI with the current state of the
     * telephony framework.
     *
     * @return SyncWithPhoneStateStatus.SUCCESS if we successfully updated the UI, or
     *    SyncWithPhoneStateStatus.PHONE_NOT_IN_USE if there was no phone state to sync
     *    with (ie. the phone was completely idle).  In the latter case, we
     *    shouldn't even be in the in-call UI in the first place, and it's
     *    the caller's responsibility to bail out of this activity by
     *    calling endInCallScreenSession if appropriate.
     *
     * This method directly calls updateScreen() in the normal "phone is
     * in use" case, so there's no need for the caller to do so.
     */
    private SyncWithPhoneStateStatus syncWithPhoneState() {
        boolean updateSuccessful = false;
        if (DBG) log("syncWithPhoneState()...");
        if (DBG) PhoneUtils.dumpCallState(mPhone);
        if (VDBG) dumpBluetoothState();

        // Make sure the Phone is "in use".  (If not, we shouldn't be on
        // this screen in the first place.)

        // An active or just-ended OTA call counts as "in use".
        if (TelephonyCapabilities.supportsOtasp(mCM.getFgPhone())
                && ((mApp.inCallUiState.inCallScreenMode == InCallScreenMode.OTA_NORMAL)
                    || (mApp.inCallUiState.inCallScreenMode == InCallScreenMode.OTA_ENDED))) {
            // Even when OTA Call ends, need to show OTA End UI,
            // so return Success to allow UI update.
            return SyncWithPhoneStateStatus.SUCCESS;
        }

        // If an MMI code is running that also counts as "in use".
        //
        // TODO: We currently only call getPendingMmiCodes() for GSM
        //   phones.  (The code's been that way all along.)  But CDMAPhone
        //   does in fact implement getPendingMmiCodes(), so should we
        //   check that here regardless of the phone type?
        final boolean hasPendingMmi = GeminiUtils.hasPendingMmi(mPhone); /// M:Gemini+

        // Finally, it's also OK to stay here on the InCallScreen if we
        // need to display a progress indicator while something's
        // happening in the background.
        boolean showProgressIndication = mApp.inCallUiState.isProgressIndicationActive();

        boolean showScreenEvenAfterDisconnect = mApp.inCallUiState.showAlreadyDisconnectedState;

        if (mCM.hasActiveFgCall() || mCM.hasActiveBgCall() || mCM.hasActiveRingingCall()
                || hasPendingMmi || showProgressIndication || showScreenEvenAfterDisconnect) {
            if (VDBG) log("syncWithPhoneState: it's ok to be here; update the screen...");
            updateScreen();
            return SyncWithPhoneStateStatus.SUCCESS;
        }

        Log.i(LOG_TAG, "syncWithPhoneState: phone is idle (shouldn't be here)");
        return SyncWithPhoneStateStatus.PHONE_NOT_IN_USE;
    }

    private void handleMissingVoiceMailNumber() {
        if (DBG) log("handleMissingVoiceMailNumber");

        final Message msg = Message.obtain(mHandler);
        msg.what = DONT_ADD_VOICEMAIL_NUMBER;

        final Message msg2 = Message.obtain(mHandler);
        msg2.what = ADD_VOICEMAIL_NUMBER;

        mMissingVoicemailDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.no_vm_number)
                .setMessage(R.string.no_vm_number_msg)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            if (VDBG) log("Missing voicemail AlertDialog: POSITIVE click...");
                            msg.sendToTarget();  // see dontAddVoiceMailNumber()
                            mApp.pokeUserActivity();
                        }})
                .setNegativeButton(R.string.add_vm_number_str,
                                   new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            if (VDBG) log("Missing voicemail AlertDialog: NEGATIVE click...");
                            msg2.sendToTarget();  // see addVoiceMailNumber()
                            mApp.pokeUserActivity();
                        }})
                .setOnCancelListener(new OnCancelListener() {
                        public void onCancel(DialogInterface dialog) {
                            if (VDBG) log("Missing voicemail AlertDialog: CANCEL handler...");
                            msg.sendToTarget();  // see dontAddVoiceMailNumber()
                            mApp.pokeUserActivity();
                        }})
                .create();

        // When the dialog is up, completely hide the in-call UI
        // underneath (which is in a partially-constructed state).
        mMissingVoicemailDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        mMissingVoicemailDialog.setOnShowListener(this);
        mMissingVoicemailDialog.show();
    }

    private void addVoiceMailNumberPanel() {
        if (mMissingVoicemailDialog != null) {
            mMissingVoicemailDialog.dismiss();
            mMissingVoicemailDialog = null;
        }
        if (DBG) log("addVoiceMailNumberPanel: finishing InCallScreen...");
        endInCallScreenSession();

        if (DBG) log("show vm setting");

        // navigate to the Voicemail setting in the Call Settings activity.
        Intent intent = new Intent(CallFeaturesSetting.ACTION_ADD_VOICEMAIL);
        intent.setClass(this, CallFeaturesSetting.class);
        startActivity(intent);
    }

    private void dontAddVoiceMailNumber() {
        if (mMissingVoicemailDialog != null) {
            mMissingVoicemailDialog.dismiss();
            mMissingVoicemailDialog = null;
        }
        if (DBG) log("dontAddVoiceMailNumber: finishing InCallScreen...");
        endInCallScreenSession();
    }

    private void cleanupAfterDisconnect(int msg) {
        mCM.clearDisconnected();
    }
    
    /**
     * Do some delayed cleanup after a Phone call gets disconnected.
     *
     * This method gets called a couple of seconds after any DISCONNECT
     * event from the Phone; it's triggered by the
     * DELAYED_CLEANUP_AFTER_DISCONNECT message we send in onDisconnect().
     *
     * If the Phone is totally idle right now, that means we've already
     * shown the "call ended" state for a couple of seconds, and it's now
     * time to endInCallScreenSession this activity.
     *
     * If the Phone is *not* idle right now, that probably means that one
     * call ended but the other line is still in use.  In that case, do
     * nothing, and instead stay here on the InCallScreen.
     */
    public void delayedCleanupAfterDisconnect(int msg) {
        int slot = PhoneConstants.GEMINI_SIM_1;
        if (GeminiUtils.isGeminiSupport()) {
            if (VDBG) {
                log("delayedCleanupAfterDisconnect()...  GeminiPhone state = " + ((GeminiPhone)mPhone).getState());
            }
        } else {
            if (VDBG) {
                log("delayedCleanupAfterDisconnect()...  Phone state = " + mPhone.getState());
            }
        }    

        // Clean up any connections in the DISCONNECTED state.
        //
        // [Background: Even after a connection gets disconnected, its
        // Connection object still stays around, in the special
        // DISCONNECTED state.  This is necessary because we we need the
        // caller-id information from that Connection to properly draw the
        // "Call ended" state of the CallCard.
        //   But at this point we truly don't need that connection any
        // more, so tell the Phone that it's now OK to to clean up any
        // connections still in that state.]
        mCM.clearDisconnected();

        // There are two cases where we should *not* exit the InCallScreen:
        //   (1) Phone is still in use
        // or
        //   (2) There's an active progress indication (i.e. the "Retrying..."
        //       progress dialog) that we need to continue to display.

        boolean stayHere = phoneIsInUse() || mApp.inCallUiState.isProgressIndicationActive();

        if (stayHere) {
            if (DBG) log("- delayedCleanupAfterDisconnect: staying on the InCallScreen...");
        } else {
            // Phone is idle!  We should exit the in-call UI now.
            if (DBG) log("- delayedCleanupAfterDisconnect: phone is idle...");

            // And (finally!) exit from the in-call screen
            // (but not if we're already in the process of pausing...)
            if (mIsForegroundActivity) {
                if (DBG) log("- delayedCleanupAfterDisconnect: finishing InCallScreen...");
                InCallUiState.sLastInCallScreenStatus = InCallUiState.INCALLSCREEN_NOT_EXIT_NORMAL;

                // In some cases we finish the call by taking the user to the
                // Call Log.  Otherwise, we simply call endInCallScreenSession,
                // which will take us back to wherever we came from.
                //
                // UI note: In eclair and earlier, we went to the Call Log
                // after outgoing calls initiated on the device, but never for
                // incoming calls.  Now we do it for incoming calls too, as
                // long as the call was answered by the user.  (We always go
                // back where you came from after a rejected or missed incoming
                // call.)
                //
                // And in any case, *never* go to the call log if we're in
                // emergency mode (i.e. if the screen is locked and a lock
                // pattern or PIN/password is set), or if we somehow got here
                // on a non-voice-capable device.

                if (VDBG) log("- Post-call behavior:");
                //if (VDBG) log("  - isPhoneStateRestricted() = " + isPhoneStateRestricted());
                
                boolean isPhoneStateRestricted = false;
                slot = GeminiRegister.getSlotIdByRegisterEvent(msg, DELAYED_CLEANUP_AFTER_DISCONNECT_GEMINI);
                isPhoneStateRestricted = isPhoneStateRestricted(slot);
                if (VDBG) {
                    log(" - isPhoneStateRestricted slot=" + slot + ", isPhoneStateRestricted = " + isPhoneStateRestricted);
                }

                // DisconnectCause values in the most common scenarios:
                // - INCOMING_MISSED: incoming ringing call times out, or the
                //                    other end hangs up while still ringing
                // - INCOMING_REJECTED: user rejects the call while ringing
                // - LOCAL: user hung up while a call was active (after
                //          answering an incoming call, or after making an
                //          outgoing call)
                // - NORMAL: the other end hung up (after answering an incoming
                //           call, or after making an outgoing call)

                if (isPhoneStateRestricted() && PhoneGlobals.sVoiceCapable) {
                    final Intent intent = mApp.createPhoneEndIntent();
                    ActivityOptions opts = ActivityOptions.makeCustomAnimation(this,
                            R.anim.activity_close_enter, R.anim.activity_close_exit);
                    if (VDBG) {
                        log("- Show Call Log (or Dialtacts) after disconnect. Current intent: "
                                + intent);
                    }
                    if (intent != null) {
                        try {
                            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                            startActivity(intent, opts.toBundle());
                        } catch (ActivityNotFoundException e) {
                            // Don't crash if there's somehow no "Call log" at
                            // all on this device.
                            // (This should never happen, though, since we already
                            // checked PhoneGlobals.sVoiceCapable above, and any
                            // voice-capable device surely *should* have a call
                            // log activity....)
                            Log.w(LOG_TAG, "delayedCleanupAfterDisconnect: "
                                 + "transition to call log failed; intent = " + intent);
                            // ...so just return back where we came from....
                        }
                    }
                    // Even if we did go to the call log, note that we still
                    // call endInCallScreenSession (below) to make sure we don't
                    // stay in the activity history.
                }

                endInCallScreenSession();
                finishIfNecessory();
            } else {
                //set this flag to tell us when no call exist force ourself
                if (phoneIsInUse()) {
                    InCallUiState.sLastInCallScreenStatus = InCallUiState.INCALLSCREEN_NOT_EXIT_NORMAL;
                } else {
                    InCallUiState.sLastInCallScreenStatus = InCallUiState.INCALLSCREEN_NOT_EXIT_NOT_FORGROUND;
                }
            }

            endInCallScreenSession();
            finishIfNecessory();
            /// M: @{
            // We've already shown the "call ended" state for a couple of
            // seconds. Generally, we don't need to update screen now.
            requestUpdateScreenDelay(200);
            /// @}

            // Reset the call origin when the session ends and this in-call UI is being finished.
            mApp.setLatestActiveCallOrigin(null);
        }
    }

    private void finishIfNecessory() {
        String simState = SystemProperties.get("gsm.sim.state");
        if (simState.equals(PIN_REQUIRED) || simState.equals(PUK_REQUIRED)) {
            if (DBG) {
                log("PIN or PUK Locked, need finish InCallScreen.");
            }
            super.finish();
            return;
        }

        ActivityManager am = (ActivityManager)this.getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningTaskInfo> list = am.getRunningTasks(5);
        for (RunningTaskInfo info : list) {
            if (DBG) {
                log("info.baseActivity.getPackageName() " + info.baseActivity.getPackageName());
            }
            if (info != null && info.baseActivity.getPackageName().equals("com.mediatek.oobe")) {
                super.finish();
                break;
            }
        }

        return;
    }

    /**
     * Start recording service.
     */
    private void startRecord() {
        PhoneRecorderHandler.getInstance().startVoiceRecord(Constants.PHONE_RECORDING_VOICE_CALL_CUSTOM_VALUE);
        //mHandler.postDelayed(mRecordDiskCheck, 500);
    }

    /**
     * Stop recording service.
     */
    private void stopRecord() {
        PhoneRecorderHandler.getInstance().stopVoiceRecord();
    }

    public void requestUpdateRecordState(final int state, final int customValue) {
        if (FeatureOption.MTK_PHONE_VOICE_RECORDING) {
            log("phone record custom value is" + customValue);
            if (Constants.PHONE_RECORDING_VOICE_CALL_CUSTOM_VALUE == customValue) {
                updateVoiceCallRecordState(state);
            } else if (Constants.PHONE_RECORDING_VIDEO_CALL_CUSTOM_VALUE == customValue) {
                mVTInCallScreen.updateVideoCallRecordState(state);
            }
        }
    }

    public void onStorageFull() {
        log("onStorageFull");
        handleStorageFull(false); // false for recording case
    }

    private void updateVoiceCallRecordState(int state) {
        log("updateVoiceCallRecordState... state = " + state);
        Call ringCall = null;
        Call.State ringCallState = null;
        if (null != mCM) {
            ringCall = mCM.getFirstActiveRingingCall();
            if (null != ringCall) {
                ringCallState = ringCall.getState();
            }
        }
        //AnimationDrawable animDrawable = (AnimationDrawable) mVoiceRecorderIcon.getBackground();
        if (PhoneRecorder.RECORDING_STATE == state && /*null != animDrawable &&*/ 
                       (null != ringCallState && !ringCallState.isRinging())) {
            mVoiceRecorderIcon.setVisibility(View.VISIBLE);
            //animDrawable.start();
        } else if ((PhoneRecorder.IDLE_STATE == state /*&& null != animDrawable*/) || 
                       (null != ringCallState && ringCallState.isRinging())) {
            //animDrawable.stop();
            mVoiceRecorderIcon.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * View.OnClickListener implementation.
     *
     * This method handles clicks from UI elements that use the
     * InCallScreen itself as their OnClickListener.
     *
     * Note: Currently this method is used only for a few special buttons:
     * - the mButtonManageConferenceDone "Back to call" button
     * - the "dim" effect for the secondary call photo in CallCard as the second "swap" button
     * - other OTASP-specific buttons managed by OtaUtils.java.
     *
     * *Most* in-call controls are handled by the handleOnscreenButtonClick() method, via the
     * InCallTouchUi widget.
     */
    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (VDBG) log("onClick(View " + view + ", id " + id + ")...");

        switch (id) {
            case R.id.manage_done:  // mButtonManageConferenceDone
                if (VDBG) log("onClick: mButtonManageConferenceDone...");
                // Hide the Manage Conference panel, return to NORMAL mode.
                setInCallScreenMode(InCallScreenMode.NORMAL);
                requestUpdateScreen();
                break;

            case R.id.dim_effect_for_secondary_photo:
                long currentTime = SystemClock.uptimeMillis();
                if (mInCallControlState.canSwap && (currentTime - mLastClickActionTime > 1000)) {
                    internalSwapCalls();
                    mLastClickActionTime = currentTime;
                    if (VDBG) {
                        log("Respond the swap click action.");
                    }
                } else {
                    if (VDBG) {
                        log("Ignore the swap click action.");
                    }
                }
                break;

            default:
                // Presumably one of the OTASP-specific buttons managed by
                // OtaUtils.java.
                // (TODO: It would be cleaner for the OtaUtils instance itself to
                // be the OnClickListener for its own buttons.)

                if ((mApp.inCallUiState.inCallScreenMode == InCallScreenMode.OTA_NORMAL
                     || mApp.inCallUiState.inCallScreenMode == InCallScreenMode.OTA_ENDED)
                    && mApp.otaUtils != null) {
                    mApp.otaUtils.onClickHandler(id);
                } else {
                    // Uh oh: we *should* only receive clicks here from the
                    // buttons managed by OtaUtils.java, but if we're not in one
                    // of the special OTASP modes, those buttons shouldn't have
                    // been visible in the first place.
                    Log.w(LOG_TAG,
                          "onClick: unexpected click from ID " + id + " (View = " + view + ")");
                }
                break;
        }

        EventLog.writeEvent(EventLogTags.PHONE_UI_BUTTON_CLICK,
                (view instanceof TextView) ? ((TextView) view).getText() : "");

        // Clicking any onscreen UI element counts as explicit "user activity".
        mApp.pokeUserActivity();
    }

    private void onHoldClick() {
        final boolean hasActiveCall = mCM.hasActiveFgCall();
        final boolean hasHoldingCall = mCM.hasActiveBgCall();
        final boolean haveMultipleHoldingCall = (mDualTalk != null)
                && mDualTalk.hasDualHoldCallsOnly();
        log("onHoldClick: hasActiveCall = " + hasActiveCall
            + ", hasHoldingCall = " + hasHoldingCall);
        boolean newHoldState;
        boolean holdButtonEnabled;
        if (hasActiveCall && !hasHoldingCall) {
            // There's only one line in use, and that line is active.
            PhoneUtils.switchHoldingAndActive(
                mCM.getFirstActiveBgCall());  // Really means "hold" in this state
            newHoldState = true;
            holdButtonEnabled = true;
        } else if (!hasActiveCall && hasHoldingCall && !haveMultipleHoldingCall) {
            // There's only one line in use, and that line is on hold.
            PhoneUtils.switchHoldingAndActive(
                mCM.getFirstActiveBgCall());  // Really means "unhold" in this state
            newHoldState = false;
            holdButtonEnabled = true;
        } else {
            // Either zero or 2 lines are in use; "hold/unhold" is meaningless.
            // For c+g project, if allow hold gsm and cdma, the call status will
            // be error!!!
            if (DualTalkUtils.isSupportDualTalk() 
                    && mDualTalk != null && mDualTalk.isMultiplePhoneActive()
                    && !mDualTalk.hasActiveCdmaPhone()) {
                if (hasActiveCall) {
                    log("onHoldClick: has active call.");
                    Call fgCall = mDualTalk.getActiveFgCall();
                    try {
                        fgCall.getPhone().switchHoldingAndActive();
                    } catch (CallStateException e) {
                        log("onHoldClick: CallStateException.");
                    }
                } else {
                    log("onHoldClick: has two background calls");
                    Call bgCall = mDualTalk.getFirstActiveBgCall();
                    try {
                        bgCall.getPhone().switchHoldingAndActive();
                    } catch (CallStateException e) {
                        log("onHoldClick: CallStateException.");
                    }
                }
            }
            newHoldState = false;
            holdButtonEnabled = false;
        }
        // No need to forcibly update the onscreen UI; just wait for the
        // onPhoneStateChanged() callback.  (This seems to be responsive
        // enough.)

        // Also, any time we hold or unhold, force the DTMF dialpad to close.
        closeDialpadInternal(true);  // do the "closing" animation
    }

    /**
     * Toggles in-call audio between speaker and the built-in earpiece (or
     * wired headset.)
     */
    public void toggleSpeaker() {
        // TODO: Turning on the speaker seems to enable the mic
        //   whether or not the "mute" feature is active!
        // Not sure if this is an feature of the telephony API
        //   that I need to handle specially, or just a bug.
        boolean newSpeakerState = !PhoneUtils.isSpeakerOn(this);
        log("toggleSpeaker(): newSpeakerState = " + newSpeakerState);

        if (newSpeakerState && isBluetoothAvailable() && isBluetoothAudioConnected()) {
            disconnectBluetoothAudio();
        }
        PhoneUtils.turnOnSpeaker(this, newSpeakerState, true);

        // And update the InCallTouchUi widget (since the "audio mode"
        // button might need to change its appearance based on the new
        // audio state.)
        updateInCallTouchUi();
        if (FeatureOption.MTK_VT3G324M_SUPPORT) {
            if (Constants.VTScreenMode.VT_SCREEN_OPEN == mVTInCallScreen.getVTScreenMode()) {
                mVTInCallScreen.updateVTScreen(mVTInCallScreen.getVTScreenMode());
            }
        }
    }

    /*
     * onMuteClick is called only when there is a foreground call
     */
    public void onMuteClick() {
        boolean newMuteState = !PhoneUtils.getMute();
        log("onMuteClick(): newMuteState = " + newMuteState);
        PhoneUtils.setMute(newMuteState);
        if (FeatureOption.MTK_VT3G324M_SUPPORT) {
            if (Constants.VTScreenMode.VT_SCREEN_OPEN == mVTInCallScreen.getVTScreenMode()) {
                mVTInCallScreen.updateVTScreen(mVTInCallScreen.getVTScreenMode());
            }
        }
    }

    private DialogInterface.OnClickListener mDialogClickListener = new DialogInterface.OnClickListener() {

        public void onClick(DialogInterface dialog, int which) {
            if (dialog == mSimCollisionDialog && which == Dialog.BUTTON_POSITIVE) {
                PhoneUtils.startNewCall(mCM);
            }
        }
    };

    /*
     * do not show the 'accpt waiting and hang up active' menu item
     *  when it's not a fta test card'
     */
    boolean okToShowFTAMenu() {
        log("okToAcceptWaitingAndHangupActive");
        boolean retval = false;
        if (GeminiUtils.isGeminiSupport()) {
            /// M:Gemini+
            int slot = GeminiUtils.getSlotNotIdle(PhoneGlobals.getInstance().phone);
            log("slot = " + slot);
            if (slot != -1) {
                retval = PhoneGlobals.getInstance().phoneMgr.isTestIccCardGemini(slot);
            }
        } else {
            retval = PhoneGlobals.getInstance().phoneMgr.isTestIccCard();
        }
        log("retval = " + retval);
        return retval;
    }

    private boolean canHangupAll() {
        Call fgCall = mCM.getActiveFgCall();
        Call bgCall = mCM.getFirstActiveBgCall();
        boolean retval = false;
        if (null != bgCall && bgCall.getState() == Call.State.HOLDING) {
            if (null != fgCall && fgCall.getState() == Call.State.ACTIVE) {
                retval = true;
            } else if (PhoneUtils.hasActivefgEccCall(mCM)) {
                retval = true;
            }
        }
        log("canHangupAll = " + retval);
        return retval;
    }

    private void onAddCallClick() {
        PhoneUtils.startNewCall(mCM);
    }

    private void muteIncomingCall(boolean mute) {
        Ringer ringer = PhoneGlobals.getInstance().ringer;
        if (mute && ringer.isRinging()) {
            ringer.stopRing();
        }
        ringer.setMute(mute);
    }

    /**
     * Toggles whether or not to route in-call audio to the bluetooth
     * headset, or do nothing (but log a warning) if no bluetooth device
     * is actually connected.
     *
     * TODO: this method is currently unused, but the "audio mode" UI
     * design is still in flux so let's keep it around for now.
     * (But if we ultimately end up *not* providing any way for the UI to
     * simply "toggle bluetooth", we can get rid of this method.)
     */
    public void toggleBluetooth() {
        if (VDBG) log("toggleBluetooth()...");

        if (isBluetoothAvailable()) {
            // Toggle the bluetooth audio connection state:
            if (isBluetoothAudioConnected()) {
                disconnectBluetoothAudio();
                if (FeatureOption.MTK_VT3G324M_SUPPORT) {
                    if (VTCallUtils.isVideoCall(mCM.getActiveFgCall())) {
                        PhoneUtils.turnOnSpeaker(this, true, true);
                    }
                }
            } else {
                // Manually turn the speaker phone off, instead of allowing the
                // Bluetooth audio routing to handle it, since there's other
                // important state-updating that needs to happen in the
                // PhoneUtils.turnOnSpeaker() method.
                // (Similarly, whenever the user turns *on* the speaker, we
                // manually disconnect the active bluetooth headset;
                // see toggleSpeaker() and/or switchInCallAudio().)
                if (PhoneUtils.isSpeakerOn(this)) {
                    PhoneUtils.turnOnSpeaker(this, false, true);
                }

                connectBluetoothAudio();
            }
        } else {
            // Bluetooth isn't available; the onscreen UI shouldn't have
            // allowed this request in the first place!
            Log.w(LOG_TAG, "toggleBluetooth(): bluetooth is unavailable");
        }

        // And update the InCallTouchUi widget (since the "audio mode"
        // button might need to change its appearance based on the new
        // audio state.)
        updateInCallTouchUi();
    }

    /**
     * Switches the current routing of in-call audio between speaker,
     * bluetooth, and the built-in earpiece (or wired headset.)
     *
     * This method is used on devices that provide a single 3-way switch
     * for audio routing.  For devices that provide separate toggles for
     * Speaker and Bluetooth, see toggleBluetooth() and toggleSpeaker().
     *
     * TODO: UI design is still in flux.  If we end up totally
     * eliminating the concept of Speaker and Bluetooth toggle buttons,
     * we can get rid of toggleBluetooth() and toggleSpeaker().
     */
    public void switchInCallAudio(InCallAudioMode newMode) {
        log("switchInCallAudio: new mode = " + newMode);
        switch (newMode) {
            case SPEAKER:
                if (!PhoneUtils.isSpeakerOn(this)) {
                    // Switch away from Bluetooth, if it was active.
                    if (isBluetoothAvailable() && isBluetoothAudioConnected()) {
                        disconnectBluetoothAudio();
                    }
                    PhoneUtils.turnOnSpeaker(this, true, true);
                }
                break;

            case BLUETOOTH:
                // If already connected to BT, there's nothing to do here.
                if (isBluetoothAvailable() && !isBluetoothAudioConnected()) {
                    // Manually turn the speaker phone off, instead of allowing the
                    // Bluetooth audio routing to handle it, since there's other
                    // important state-updating that needs to happen in the
                    // PhoneUtils.turnOnSpeaker() method.
                    // (Similarly, whenever the user turns *on* the speaker, we
                    // manually disconnect the active bluetooth headset;
                    // see toggleSpeaker() and/or switchInCallAudio().)
                    if (PhoneUtils.isSpeakerOn(this)) {
                        PhoneUtils.turnOnSpeaker(this, false, true);
                    }
                    connectBluetoothAudio();
                }
                break;

            case EARPIECE:
                // Switch to either the handset earpiece, or the wired headset (if connected.)
                // (Do this by simply making sure both speaker and bluetooth are off.)
                if (isBluetoothAvailable() && isBluetoothAudioConnected()) {
                    disconnectBluetoothAudio();
                }
                if (PhoneUtils.isSpeakerOn(this)) {
                    PhoneUtils.turnOnSpeaker(this, false, true);
                }
                break;

            default:
                Log.wtf(LOG_TAG, "switchInCallAudio: unexpected mode " + newMode);
                break;
        }

        // And finally, update the InCallTouchUi widget (since the "audio
        // mode" button might need to change its appearance based on the
        // new audio state.)
        if (mVTInCallScreen.getVTScreenMode() != Constants.VTScreenMode.VT_SCREEN_OPEN) {
            updateInCallTouchUi();
        } else {
            mVTInCallScreen.updateVTScreen(mVTInCallScreen.getVTScreenMode());
        }
    }

    /**
     * Handle a click on the "Open/Close dialpad" button.
     *
     * @see DTMFTwelveKeyDialer#openDialer(boolean)
     * @see DTMFTwelveKeyDialer#closeDialer(boolean)
     */
    public void onOpenCloseDialpad() {
        if (VDBG) log("onOpenCloseDialpad()...");
        if (mDialer.isOpened()) {
            closeDialpadInternal(true);  // do the "closing" animation
        } else {
            openDialpadInternal(true);  // do the "opening" animation
        }
        mApp.updateProximitySensorMode(mCM.getState());
    }

    /** Internal wrapper around {@link DTMFTwelveKeyDialer#openDialer(boolean)} */
    private void openDialpadInternal(boolean animate) {
        mDialer.openDialer(animate);
        // And update the InCallUiState (so that we'll restore the dialpad
        // to the correct state if we get paused/resumed).
        mApp.inCallUiState.showDialpad = true;
    }

    // Internal wrapper around DTMFTwelveKeyDialer.closeDialer()
    private void closeDialpadInternal(boolean animate) {
        mDialer.closeDialer(animate);
        // And update the InCallUiState (so that we'll restore the dialpad
        // to the correct state if we get paused/resumed).
        mApp.inCallUiState.showDialpad = false;
    }

    /**
     * Handles button clicks from the InCallTouchUi widget.
     */
     public void handleOnscreenButtonClick(int id) {
        if (DBG) log("handleOnscreenButtonClick(id " + id + ")...");

        // This counts as explicit "user activity".
        PhoneGlobals.getInstance().pokeUserActivity();

        if (mExtension.handleOnscreenButtonClick(id)) {
            mApp.pokeUserActivity();
            return;
        }

        switch (id) {
            // Actions while an incoming call is ringing:
            case R.id.incomingCallAnswer:
                internalAnswerCall();
                break;
            case R.id.incomingCallReject:
                if (FeatureOption.MTK_VT3G324M_SUPPORT) {
                    if (!VTCallUtils.isVTActive()) {
                        mVTInCallScreen.setVTScreenMode(Constants.VTScreenMode.VT_SCREEN_CLOSE);
                    } else {
                        mVTInCallScreen.setVTScreenMode(Constants.VTScreenMode.VT_SCREEN_OPEN);
                    }
                    mVTInCallScreen.updateVTScreen(mVTInCallScreen.getVTScreenMode());
                }
                hangupRingingCall();
                break;
            case R.id.incomingCallRespondViaSms:
                internalRespondViaSms();
                break;

            // The other regular (single-tap) buttons used while in-call:
            case R.id.holdButton:
                onHoldClick();
                break;
            case R.id.swapButton:
                long currentTime = SystemClock.uptimeMillis();
                if (currentTime - mLastClickActionTime > 1000) {
                    if (DualTalkUtils.isSupportDualTalk() && mDualTalk.isDualTalkMultipleHoldCase()) {
                        List<Call> list = mDualTalk.getAllNoIdleCalls();
                        selectWhichCallActive(list);
                    } else {
                        internalSwapCalls();
                    }
                    mLastClickActionTime = currentTime;
                }
                break;
            case R.id.endButton:
                internalHangup();
                break;
            case R.id.dialpadButton:
                onOpenCloseDialpad();
                break;
            case R.id.muteButton:
                onMuteClick();
                break;
            case R.id.addButton:
                /**
                 * add by mediatek .inc
                 * description : ignore the add button click
                 * event when the error dialog is showing
                 */
                if (mShowStatusIndication) {
                    if (DBG) {
                        log("ignore addButton click event");
                    }
                    break;
                }
                onAddCallClick();
                break;
            case R.id.mergeButton:
            case R.id.cdmaMergeButton:
                mSwappingCalls = false;
                PhoneUtils.mergeCalls(mCM);
                break;

            case R.id.incomingOverflowMenu:
            case R.id.overflowMenu:
                if (mPopupMenu != null) {
                    mPopupMenu.dismiss();
                }
                mPopupMenu = constructPopupMenu(findViewById(id));
                if (mPopupMenu != null && mPopupMenu.getMenu().hasVisibleItems()) {
                    mPopupMenu.show();
                }
                break;

            case R.id.manageConferenceButton:
                // Show the Manage Conference panel.
                setInCallScreenMode(InCallScreenMode.MANAGE_CONFERENCE);
                requestUpdateScreen();
                break;

            default:
                Log.w(LOG_TAG, "handleOnscreenButtonClick: unexpected ID " + id);
                break;
        }

        // Clicking any onscreen UI element counts as explicit "user activity".
        mApp.pokeUserActivity();

        // Just in case the user clicked a "stateful" UI element (like one
        // of the toggle buttons), we force the in-call buttons to update,
        // to make sure the user sees the *new* current state.
        //
        // Note that some in-call buttons will *not* immediately change the
        // state of the UI, namely those that send a request to the telephony
        // layer (like "Hold" or "End call".)  For those buttons, the
        // updateInCallTouchUi() call here won't have any visible effect.
        // Instead, the UI will be updated eventually when the next
        // onPhoneStateChanged() event comes in and triggers an updateScreen()
        // call.
        //
        // TODO: updateInCallTouchUi() is overkill here; it would be
        // more efficient to update *only* the affected button(s).
        // (But this isn't a big deal since updateInCallTouchUi() is pretty
        // cheap anyway...)
        if (id == R.id.swapButton) {
            Log.w(LOG_TAG, "handleOnscreenButtonClick: id == R.id.swapButton " + id);
            return;
        }
        //CR:ALPS00259161 start
        if (id == R.id.mergeButton) {
            Log.w(LOG_TAG, "handleOnscreenButtonClick: id == R.id.mergeButton " + id);
            return;
        }
        //CR:ALPS00259161 end
        updateInCallTouchUi();
    }

    /**
     * Display a status or error indication to the user according to the
     * specified InCallUiState.CallStatusCode value.
     */
    private void showStatusIndication(CallStatusCode status) {
        switch (status) {
            case SUCCESS:
                // The InCallScreen does not need to display any kind of error indication,
                // so we shouldn't have gotten here in the first place.
                Log.wtf(LOG_TAG, "showStatusIndication: nothing to display");
                break;

            case POWER_OFF:
                // Radio is explictly powered off, presumably because the
                // device is in airplane mode.
                //
                // TODO: For now this UI is ultra-simple: we simply display
                // a message telling the user to turn off airplane mode.
                // But it might be nicer for the dialog to offer the option
                // to turn the radio on right there (and automatically retry
                // the call once network registration is complete.)
                Log.w(LOG_TAG, "handleStartupError: POWER_OFF");
                if (null != PhoneGlobals.getInstance().phoneMgr) {
                    final int[] geminiSlots = GeminiUtils.getSlots();
                    boolean hasInsertSim = false;
                    StringBuffer sb = null;
                    for (int i = 0; i < geminiSlots.length; i++) {
                        if (!PhoneGlobals.getInstance().phoneMgr.isSimInsert(geminiSlots[i])) {
                            if (sb == null) {
                                sb = new StringBuffer(geminiSlots[i] + 1);
                            } else {
                                sb.append(", ").append(geminiSlots[i] + 1);
                            }
                        } else {
                            hasInsertSim = true;
                        }
                    }
    
                    if (!hasInsertSim) {
                        // /M: #Only emergency# text replace in case IMEI locked @{
                        showCanDismissDialog(
                                mServiceStateExt.isImeiLocked() ? R.string.callFailed_cdma_notEmergency
                                        : R.string.callFailed_simError, true);
                        // /@}
                        break;
                    } else if (sb != null) {
                        CharSequence text = getResources().getString(
                                R.string.callFailed_simError_slotNumber, sb.toString());
                        showCanDismissDialog(text, true);
                        break;
                    }
                }
                showCanDismissDialog(R.string.incall_error_power_off, true);
                //showGenericErrorDialog(R.string.incall_error_power_off,
                                       //true /* isStartupError */);
                break;

            case EMERGENCY_ONLY:
                // Only emergency numbers are allowed, but we tried to dial
                // a non-emergency number.
                // (This state is currently unused; see comments above.)
                showGenericErrorDialog(R.string.incall_error_emergency_only,
                                       true /* isStartupError */);
                break;

            case OUT_OF_SERVICE:
                // No network connection.
                //showGenericErrorDialog(R.string.incall_error_out_of_service,
                //                       true /* isStartupError */);
                boolean hasInsertSim = true;
                if (null != PhoneGlobals.getInstance().phoneMgr) {
                    final int[] geminiSlots = GeminiUtils.getSlots();
                    hasInsertSim = false;
                    for (int gs : geminiSlots) {
                        if (PhoneGlobals.getInstance().phoneMgr.isSimInsert(gs)) {
                            hasInsertSim = true;
                            break;
                        }
                    }
                }
                if (!hasInsertSim) {
                    // /M: #Only emergency# text replace in case IMEI locked @{
                    showCanDismissDialog(mServiceStateExt.isImeiLocked() ? R.string.callFailed_cdma_notEmergency
                            : R.string.callFailed_simError, true);
                    // /@}
                } else if (mRingingCall.isIdle()) {
                    showCanDismissDialog(R.string.incall_error_out_of_service, true);
                }
                break;

            case NO_PHONE_NUMBER_SUPPLIED:
                // The supplied Intent didn't contain a valid phone number.
                // (This is rare and should only ever happen with broken
                // 3rd-party apps.)  For now just show a generic error.
                if (mRingingCall.isIdle()) {
                    showGenericErrorDialog(R.string.incall_error_no_phone_number_supplied,
                                           true /* isStartupError */);
                }
                break;

            case DIALED_MMI:
                // Our initial phone number was actually an MMI sequence.
                // There's no real "error" here, but we do bring up the
                // a Toast (as requested of the New UI paradigm).
                //
                // In-call MMIs do not trigger the normal MMI Initiate
                // Notifications, so we should notify the user here.
                // Otherwise, the code in PhoneUtils.java should handle
                // user notifications in the form of Toasts or Dialogs.
                mSwappingCalls = false;
                PhoneConstants.State state;
                if (GeminiUtils.isGeminiSupport()) {
                    state = ((GeminiPhone)mPhone).getState();
                } else {
                    state = mCM.getState();
                }

                if (state == PhoneConstants.State.OFFHOOK) {
                    Toast.makeText(mApp, R.string.incall_status_dialed_mmi, Toast.LENGTH_SHORT).show();
                }
                mShowStatusIndication = false;
                break;

            case CALL_FAILED:
                // We couldn't successfully place the call; there was some
                // failure in the telephony layer.
                // TODO: Need UI spec for this failure case; for now just
                // show a generic error.
                //showGenericErrorDialog(R.string.incall_error_call_failed,
                //                       true /* isStartupError */);
                if (mRingingCall.isIdle()) {
                    showCanDismissDialog(R.string.incall_error_call_failed, true);
                } else {
                    mShowStatusIndication = false;
                }
                break;

            case VOICEMAIL_NUMBER_MISSING:
                // We tried to call a voicemail: URI but the device has no
                // voicemail number configured.
                if (mRingingCall.isIdle()) {
                    handleMissingVoiceMailNumber();
                } else {
                    mShowStatusIndication = false;
                }
                break;

            case CDMA_CALL_LOST:
                // This status indicates that InCallScreen should display the
                // CDMA-specific "call lost" dialog.  (If an outgoing call fails,
                // and the CDMA "auto-retry" feature is enabled, *and* the retried
                // call fails too, we display this specific dialog.)
                //
                // TODO: currently unused; see InCallUiState.needToShowCallLostDialog
                mShowStatusIndication = false;
                break;

            case EXITED_ECM:
                // This status indicates that InCallScreen needs to display a
                // warning that we're exiting ECM (emergency callback mode).
                showExitingECMDialog();
                break;

            case OUT_OF_3G_FAILED:
                // Here never happens because out of 3G case is handled by the logic
                // before calling CallController.placeCall(), so comment here
                mShowStatusIndication = false;
                break;

            /**
             * add by mediatek .inc
             * description : show the FDN block dialog
             */
            case FDN_BLOCKED:
                log("showGenericErrorDialog, fdn_only");
                showGenericErrorDialog(R.string.callFailed_fdn_only, false);
                break;

            /**
            * sim no 3G signal for drop voice call
            */
            case DROP_VOICECALL:
                final int slot = mApp.inCallUiState.getSlot();
                String number = mApp.inCallUiState.getNumber();
                log("DROP_VOICECALL number:" + number);
                mVTInCallScreen.showReCallDialog(R.string.callFailed_dsac_vt_out_of_3G_yourphone,number,slot);
                break;

            default:
                showCanDismissDialog(R.string.incall_error_call_failed, true);
                throw new IllegalStateException(
                    "showStatusIndication: unexpected status code: " + status);
        }

        // TODO: still need to make sure that pressing OK or BACK from
        // *any* of the dialogs we launch here ends up calling
        // inCallUiState.clearPendingCallStatusCode()
        //  *and*
        // make sure the Dialog handles both OK *and* cancel by calling
        // endInCallScreenSession.  (See showGenericErrorDialog() for an
        // example.)
        //
        // (showGenericErrorDialog() currently does this correctly,
        // but handleMissingVoiceMailNumber() probably needs to be fixed too.)
        //
        // Also need to make sure that bailing out of any of these dialogs by
        // pressing Home clears out the pending status code too.  (If you do
        // that, neither the dialog's clickListener *or* cancelListener seems
        // to run...)
    }

    /**
     * Utility function to bring up a generic "error" dialog, and then bail
     * out of the in-call UI when the user hits OK (or the BACK button.)
     */
    public void showGenericErrorDialog(int resid, boolean isStartupError) {
        CharSequence msg = getResources().getText(resid);
        if (DBG) log("showGenericErrorDialog('" + msg + "')...");

        /// M: for ALPS00552109 @{
        // if the dilaog is showing aready, DO NOT show it again
        //
        if (mGenericErrorDialog != null && mGenericErrorDialog.isShowing()) {
            if (DBG) {
                log("already showing, skip...");
            }
            return;
        }
        /// @}

        // create the clicklistener and cancel listener as needed.
        DialogInterface.OnClickListener clickListener;
        OnCancelListener cancelListener;
        OnDismissListener dismissListener;
        if (isStartupError) {
            clickListener = new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    bailOutAfterErrorDialog();
                }};
            cancelListener = new OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    bailOutAfterErrorDialog();
                }};
            dismissListener = new DialogInterface.OnDismissListener() {
                public void onDismiss(DialogInterface dialog) {
                    bailOutAfterErrorDialog();
                }
            };
        } else {
            clickListener = new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    if (DBG) log("showGenericErrorDialog: receive click!");
                    dialog.dismiss();
                    delayedCleanupAfterDisconnect();
                }};
            cancelListener = new OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    if (DBG) log("showGenericErrorDialog: receive cancel!");
                    delayedCleanupAfterDisconnect();
                }};
            dismissListener = new DialogInterface.OnDismissListener() {
                public void onDismiss(DialogInterface dialog) {
                    if (DBG) log("showGenericErrorDialog: dismiss!");
                    bailOutAfterErrorDialog();
                }
            };
        }

        // TODO: Consider adding a setTitle() call here (with some generic
        // "failure" title?)
        mGenericErrorDialog = new AlertDialog.Builder(this)
                .setMessage(msg)
                .setPositiveButton(R.string.ok, clickListener)
                .setOnCancelListener(cancelListener)
                .create();

        mGenericErrorDialog.setOnDismissListener(dismissListener);
        mGenericErrorDialog.setOnShowListener(this);
        // When the dialog is up, completely hide the in-call UI
        // underneath (which is in a partially-constructed state).
        mGenericErrorDialog.getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        mGenericErrorDialog.show();
    }

    private void showCallLostDialog() {
        if (DBG) log("showCallLostDialog()...");

        // Don't need to show the dialog if InCallScreen isn't in the forgeround
        if (!mIsForegroundActivity) {
            if (DBG) log("showCallLostDialog: not the foreground Activity! Bailing out...");
            return;
        }

        // Don't need to show the dialog again, if there is one already.
        if (mCallLostDialog != null) {
            if (DBG) log("showCallLostDialog: There is a mCallLostDialog already.");
            return;
        }

        mCallLostDialog = new AlertDialog.Builder(this)
                .setMessage(R.string.call_lost)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .create();
        mCallLostDialog.show();
    }

    /**
     * Displays the "Exiting ECM" warning dialog.
     *
     * Background: If the phone is currently in ECM (Emergency callback
     * mode) and we dial a non-emergency number, that automatically
     * *cancels* ECM.  (That behavior comes from CdmaCallTracker.dial().)
     * When that happens, we need to warn the user that they're no longer
     * in ECM (bug 4207607.)
     *
     * So bring up a dialog explaining what's happening.  There's nothing
     * for the user to do, by the way; we're simply providing an
     * indication that they're exiting ECM.  We *could* use a Toast for
     * this, but toasts are pretty easy to miss, so instead use a dialog
     * with a single "OK" button.
     *
     * TODO: it's ugly that the code here has to make assumptions about
     *   the behavior of the telephony layer (namely that dialing a
     *   non-emergency number while in ECM causes us to exit ECM.)
     *
     *   Instead, this warning dialog should really be triggered by our
     *   handler for the
     *   TelephonyIntents.ACTION_EMERGENCY_CALLBACK_MODE_CHANGED intent in
     *   PhoneGlobals.java.  But that won't work until that intent also
     *   includes a *reason* why we're exiting ECM, since we need to
     *   display this dialog when exiting ECM because of an outgoing call,
     *   but NOT if we're exiting ECM because the user manually turned it
     *   off via the EmergencyCallbackModeExitDialog.
     *
     *   Or, it might be simpler to just have outgoing non-emergency calls
     *   *not* cancel ECM.  That way the UI wouldn't have to do anything
     *   special here.
     */
    private void showExitingECMDialog() {
        Log.i(LOG_TAG, "showExitingECMDialog()...");

        if (mExitingECMDialog != null) {
            if (DBG) log("- DISMISSING mExitingECMDialog.");
            mExitingECMDialog.dismiss();  // safe even if already dismissed
            mExitingECMDialog = null;
        }

        // When the user dismisses the "Exiting ECM" dialog, we clear out
        // the pending call status code field (since we're done with this
        // dialog), but do *not* bail out of the InCallScreen.

        final InCallUiState inCallUiState = mApp.inCallUiState;
        DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    inCallUiState.clearPendingCallStatusCode();
                }};
        OnCancelListener cancelListener = new OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    inCallUiState.clearPendingCallStatusCode();
                }};

        // Ultra-simple AlertDialog with only an OK button:
        mExitingECMDialog = new AlertDialog.Builder(this)
                .setMessage(R.string.progress_dialog_exiting_ecm)
                .setPositiveButton(R.string.ok, clickListener)
                .setOnCancelListener(cancelListener)
                .create();
        mExitingECMDialog.getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
        mExitingECMDialog.setOnShowListener(this);
        mExitingECMDialog.show();
    }

//MTK begin:
    private void showCanDismissDialog(int resid, boolean isStartupError) {
        if (DBG) {
            log("showCanDismissDialog...isStartupError = " + isStartupError);
        }
        CharSequence msg = getResources().getText(resid);
        showCanDismissDialog(msg, isStartupError);
    }

    private void showCanDismissDialog(CharSequence message, boolean isStartupError) {

        if (DBG) {
            log("showCanDismissDialog...message = " + message);
        }

        /// M: for ALPS00552109 @{
        // if the dilaog is showing aready, DO NOT show it again.
        //
        if (mCanDismissDialog != null && mCanDismissDialog.isShowing()) {
            if (DBG) {
                log("already showing, skip...");
            }
            return;
        }
        /// @}

        // create the clicklistener and cancel listener as needed.
        DialogInterface.OnClickListener clickListener;
        OnCancelListener cancelListener;
        if (isStartupError) {
            clickListener = new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    bailOutAfterCanDismissDialog();
                }
            };
            cancelListener = new OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    bailOutAfterCanDismissDialog();
                }
            };
        } else {
            clickListener = new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    delayedCleanupAfterDisconnect();
                }
            };
            cancelListener = new OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    delayedCleanupAfterDisconnect();
                }
            };
        }

        mCanDismissDialog = new AlertDialog.Builder(this).setMessage(message).setPositiveButton(R.string.ok, clickListener).setOnCancelListener(cancelListener).create();
        mCanDismissDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        mCanDismissDialog.setOnShowListener(this);
        mCanDismissDialog.show();
    }

    private void bailOutAfterErrorDialog() {
        if (mGenericErrorDialog != null) {
            if (DBG) log("bailOutAfterErrorDialog: DISMISSING mGenericErrorDialog.");
            mGenericErrorDialog.dismiss();
            mGenericErrorDialog = null;
        }
        if (DBG) log("bailOutAfterErrorDialog(): end InCallScreen session...");

        // Now that the user has dismissed the error dialog (presumably by
        // either hitting the OK button or pressing Back, we can now reset
        // the pending call status code field.
        //
        // (Note that the pending call status is NOT cleared simply
        // by the InCallScreen being paused or finished, since the resulting
        // dialog is supposed to persist across orientation changes or if the
        // screen turns off.)
        //
        // See the "Error / diagnostic indications" section of
        // InCallUiState.java for more detailed info about the
        // pending call status code field.
        final InCallUiState inCallUiState = mApp.inCallUiState;
        inCallUiState.clearPendingCallStatusCode();

        // Force the InCallScreen to truly finish(), rather than just
        // moving it to the back of the activity stack (which is what
        // our finish() method usually does.)
        // This is necessary to avoid an obscure scenario where the
        // InCallScreen can get stuck in an inconsistent state, somehow
        // causing a *subsequent* outgoing call to fail (bug 4172599).
        if (!mCM.hasActiveFgCall() && !mCM.hasActiveBgCall() && !mCM.hasActiveRingingCall()) {
            endInCallScreenSession(true /* force a real finish() call */);
        }
    }
    
    private void bailOutAfterCanDismissDialog() {
        if (mCanDismissDialog != null) {
            if (DBG) log("bailOutAfterCanDismissDialog: DISMISSING mCanDismissDialog.");
            mCanDismissDialog.dismiss();
            mCanDismissDialog = null;
        }
        if (DBG) log("bailOutAfterCanDismissDialog(): end InCallScreen session...");

        // Now that the user has dismissed the error dialog (presumably by
        // either hitting the OK button or pressing Back, we can now reset
        // the pending call status code field.
        //
        // (Note that the pending call status is NOT cleared simply
        // by the InCallScreen being paused or finished, since the resulting
        // dialog is supposed to persist across orientation changes or if the
        // screen turns off.)
        //
        // See the "Error / diagnostic indications" section of
        // InCallUiState.java for more detailed info about the
        // pending call status code field.
        final InCallUiState inCallUiState = mApp.inCallUiState;
        inCallUiState.clearPendingCallStatusCode();

        // Force the InCallScreen to truly finish(), rather than just
        // moving it to the back of the activity stack (which is what
        // our finish() method usually does.)
        // This is necessary to avoid an obscure scenario where the
        // InCallScreen can get stuck in an inconsistent state, somehow
        // causing a *subsequent* outgoing call to fail (bug 4172599).
        if (!mCM.hasActiveFgCall() && !mCM.hasActiveBgCall() && !mCM.hasActiveRingingCall()) {
            endInCallScreenSession(true /* force a real finish() call */);
        }
    }

    /**
     * Dismisses (and nulls out) all persistent Dialogs managed
     * by the InCallScreen.  Useful if (a) we're about to bring up
     * a dialog and want to pre-empt any currently visible dialogs,
     * or (b) as a cleanup step when the Activity is going away.
     */
    private void dismissAllDialogs() {
        if (DBG) log("dismissAllDialogs()...");

        // Note it's safe to dismiss() a dialog that's already dismissed.
        // (Even if the AlertDialog object(s) below are still around, it's
        // possible that the actual dialog(s) may have already been
        // dismissed by the user.)

       if (mGenericErrorDialog != null) {
            if (VDBG) log("- DISMISSING mGenericErrorDialog.");
            mGenericErrorDialog.dismiss();
            mGenericErrorDialog = null;
        }

       if (mStorageSpaceDialog != null) {
           if (DBG) log("- DISMISSING mStorageSpaceDialog.");
           mStorageSpaceDialog.dismiss();
           mStorageSpaceDialog = null;
       }

        if (null != mCallSelectDialog) {
            mCallSelectDialog.dismiss();
            mCallSelectDialog = null;
        }
        
        if (mSuppServiceFailureDialog != null) {
            if (VDBG) log("- DISMISSING mSuppServiceFailureDialog.");
            mSuppServiceFailureDialog.dismiss();
            mSuppServiceFailureDialog = null;
        }

        dismissDialogs();
    }

    private void dismissDialogs() {
        if (DBG) log("dismissDialogs()...");

        // Note it's safe to dismiss() a dialog that's already dismissed.
        // (Even if the AlertDialog object(s) below are still around, it's
        // possible that the actual dialog(s) may have already been
        // dismissed by the user.)

        if (mExtension.dismissDialogs()) {
            return;
        }

        if (mMissingVoicemailDialog != null) {
            if (VDBG) log("- DISMISSING mMissingVoicemailDialog.");
            mMissingVoicemailDialog.dismiss();
            mMissingVoicemailDialog = null;
        }
        if (mMmiStartedDialog != null) {
            if (VDBG) log("- DISMISSING mMmiStartedDialog.");
            mMmiStartedDialog.dismiss();
            mMmiStartedDialog = null;
        }
        if (mGenericErrorDialog != null) {
            if (VDBG) log("- DISMISSING mGenericErrorDialog.");
            mGenericErrorDialog.dismiss();
            mGenericErrorDialog = null;
        }

        if (mWaitPromptDialog != null) {
            if (VDBG) log("- DISMISSING mWaitPromptDialog.");
            mWaitPromptDialog.dismiss();
            mWaitPromptDialog = null;
        }

        if (mWildPromptDialog != null) {
            if (VDBG) log("- DISMISSING mWildPromptDialog.");
            mWildPromptDialog.dismiss();
            mWildPromptDialog = null;
        }
        if (mCallLostDialog != null) {
            if (VDBG) log("- DISMISSING mCallLostDialog.");
            mCallLostDialog.dismiss();
            mCallLostDialog = null;
        }
        if (mCanDismissDialog != null) {
            PhoneGlobals app = PhoneGlobals.getInstance();
            setPhone(app.phone);
            if (mBackgroundCall.isIdle() && mForegroundCall.isIdle() && mRingingCall.isIdle()) {
                endInCallScreenSession();
            }
            if (VDBG) log("- DISMISSING mCanDismissDialog.");
            mCanDismissDialog.dismiss();
            mCanDismissDialog = null;
        }
        if ((mApp.inCallUiState.inCallScreenMode == InCallScreenMode.OTA_NORMAL
                || mApp.inCallUiState.inCallScreenMode == InCallScreenMode.OTA_ENDED)
                && mApp.otaUtils != null) {
            mApp.otaUtils.dismissAllOtaDialogs();
        }
        if (mPausePromptDialog != null) {
            if (DBG) log("- DISMISSING mPausePromptDialog.");
            mPausePromptDialog.dismiss();
            mPausePromptDialog = null;
        }
        //Add for cr: alps00039286
        if (mSimCollisionDialog != null) {
            mSimCollisionDialog.dismiss();
            mSimCollisionDialog = null;
        }
        if (mExitingECMDialog != null) {
            if (DBG) log("- DISMISSING mExitingECMDialog.");
            mExitingECMDialog.dismiss();
            mExitingECMDialog = null;
        }
        if (DBG) log("dismissDialogs() done");
    }

    public boolean getOnAnswerAndEndFlag() {
        return mOnAnswerandEndCall;
    }

    public void setOnAnswerAndEndFlag(boolean flag) {
        mOnAnswerandEndCall = flag;
    }

    /**
     * Updates the state of the onscreen "progress indication" used in
     * some (relatively rare) scenarios where we need to wait for
     * something to happen before enabling the in-call UI.
     *
     * If necessary, this method will cause a ProgressDialog (i.e. a
     * spinning wait cursor) to be drawn *on top of* whatever the current
     * state of the in-call UI is.
     *
     * @see InCallUiState.ProgressIndicationType
     */
    private void updateProgressIndication() {
        // If an incoming call is ringing, that takes priority over any
        // possible value of inCallUiState.progressIndication.
        if (mCM.hasActiveRingingCall()) {
            dismissProgressIndication();
            return;
        }

        // Otherwise, put up a progress indication if indicated by the
        // inCallUiState.progressIndication field.
        final InCallUiState inCallUiState = mApp.inCallUiState;
        switch (inCallUiState.getProgressIndication()) {
            case NONE:
                // No progress indication necessary, so make sure it's dismissed.
                dismissProgressIndication();
                break;

            case TURNING_ON_RADIO:
                showProgressIndication(
                    R.string.emergency_enable_radio_dialog_title,
                    R.string.emergency_enable_radio_dialog_message);
                break;

            case RETRYING:
                showProgressIndication(
                    R.string.emergency_enable_radio_dialog_title,
                    R.string.emergency_enable_radio_dialog_retry);
                break;

            default:
                Log.wtf(LOG_TAG, "updateProgressIndication: unexpected value: "
                        + inCallUiState.getProgressIndication());
                dismissProgressIndication();
                break;
        }
    }

    /**
     * Show an onscreen "progress indication" with the specified title and message.
     */
    private void showProgressIndication(int titleResId, int messageResId) {
        if (DBG) log("showProgressIndication(message " + messageResId + ")...");

        // TODO: make this be a no-op if the progress indication is
        // already visible with the exact same title and message.

        dismissProgressIndication();  // Clean up any prior progress indication
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle(getText(titleResId));
        mProgressDialog.setMessage(getText(messageResId));
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setCancelable(false);
        //mProgressDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG);
        mProgressDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
        mProgressDialog.show();
    }

    /**
     * Dismiss the onscreen "progress indication" (if present).
     */
    private void dismissProgressIndication() {
        if (DBG) log("dismissProgressIndication()...");
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();  // safe even if already dismissed
            mProgressDialog = null;
        }
    }


    //
    // Helper functions for answering incoming calls.
    //

    /**
     * Answer a ringing call.  This method does nothing if there's no
     * ringing or waiting call.
     */
    public void internalAnswerCall() {
        if (DBG) log("internalAnswerCall()...");

        final boolean hasRingingCall = mCM.hasActiveRingingCall();

        if (FeatureOption.MTK_VT3G324M_SUPPORT && VTCallUtils.isVTRinging()) {
            mVTInCallScreen.internalAnswerVTCallPre();
        } else if (DualTalkUtils.isSupportDualTalk()) {
            if (mDualTalk.hasMultipleRingingCall()
                    || mDualTalk.isDualTalkAnswerCase()
                    || mDualTalk.isRingingWhenOutgoing()) {
                internalAnswerCallForDualTalk();
                return;
            }
        }

        if (hasRingingCall) {
            Phone phone = mCM.getRingingPhone();
            Call ringing = mCM.getFirstActiveRingingCall();
            int phoneType = phone.getPhoneType();
            if (phoneType == PhoneConstants.PHONE_TYPE_CDMA) {
                if (DBG) log("internalAnswerCall: answering (CDMA)...");
                if (mCM.hasActiveFgCall()
                        && mCM.getFgPhone().getPhoneType() == PhoneConstants.PHONE_TYPE_SIP) {
                    // The incoming call is CDMA call and the ongoing
                    // call is a SIP call. The CDMA network does not
                    // support holding an active call, so there's no
                    // way to swap between a CDMA call and a SIP call.
                    // So for now, we just don't allow a CDMA call and
                    // a SIP call to be active at the same time.We'll
                    // "answer incoming, end ongoing" in this case.
                    if (DBG) log("internalAnswerCall: answer "
                            + "CDMA incoming and end SIP ongoing");
                    PhoneUtils.answerAndEndActive(mCM, ringing);
                } else {
                    PhoneUtils.answerCall(ringing);
                }
            } else if (phoneType == PhoneConstants.PHONE_TYPE_SIP) {
                if (DBG) log("internalAnswerCall: answering (SIP)...");
                if (mCM.hasActiveFgCall()
                        && mCM.getFgPhone().getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA) {
                    // Similar to the PHONE_TYPE_CDMA handling.
                    // The incoming call is SIP call and the ongoing
                    // call is a CDMA call. The CDMA network does not
                    // support holding an active call, so there's no
                    // way to swap between a CDMA call and a SIP call.
                    // So for now, we just don't allow a CDMA call and
                    // a SIP call to be active at the same time.We'll
                    // "answer incoming, end ongoing" in this case.
                    if (DBG) log("internalAnswerCall: answer "
                            + "SIP incoming and end CDMA ongoing");
                    PhoneUtils.answerAndEndActive(mCM, ringing);
                } else if (mCM.hasActiveFgCall()
                        && mCM.getFgPhone().getPhoneType() != PhoneConstants.PHONE_TYPE_CDMA 
                        && mCM.hasActiveBgCall()) {
                    PhoneUtils.answerAndEndActive(mCM, ringing);
                } else {
                    if (mCM.hasActiveFgCall() && VTCallUtils.isVideoCall(mCM.getActiveFgCall())) {
                        try {
                            mCM.getFgPhone().hangupActiveCall();
                        } catch (Exception e) {
                            log(e.toString());
                        }
                    }
                    PhoneUtils.answerCall(ringing);
                }
            } else if (phoneType == PhoneConstants.PHONE_TYPE_GSM) {
                if (DBG) log("internalAnswerCall: answering (GSM)...");
                // GSM: this is usually just a wrapper around
                // PhoneUtils.answerCall(), *but* we also need to do
                // something special for the "both lines in use" case.

                final boolean hasActiveCall = mCM.hasActiveFgCall();
                final boolean hasHoldingCall = mCM.hasActiveBgCall();

                if (FeatureOption.MTK_VT3G324M_SUPPORT) {
                    Call fg = mCM.getActiveFgCall();
                    //On dualtalk solution, when video call and voice call maybe co-exist(1A+1R)
                    if (VTCallUtils.isVTRinging()
                            || (hasActiveCall && VTCallUtils.isVideoCall(fg))) {
                        if (DBG) {
                            log("internalAnswerCall: is VT ringing now, so call PhoneUtils.answerCall(ringing) anyway !");
                        }
                        if (hasActiveCall) {
                            if (DBG) {
                                log("internalAnswerCall: is VT ringing now, first disconnect active call!");
                            }
                            Phone p = mCM.getFgPhone();
                            if (p != mCM.getRingingPhone()) {
                                try {
                                    if (p instanceof SipPhone) {
                                        mCM.getActiveFgCall().hangup();
                                    } else {
                                        if (VTCallUtils.isVideoCall(fg)) {
                                            mApp.notifier.resetAudioState();
                                        }
                                        p.hangupActiveCall();
                                    }
                                } catch (Exception e) {
                                    log(e.toString());
                                }
                            }
                        } else if (hasHoldingCall && VTCallUtils.isVideoCall(ringing)) {
                            //the holdingCall must be voice call, hangup it
                            if (DBG) {
                                log("internalAnswerCall: is VT ringing now, first disconnect holding call!");
                            }
                            try {
                                mCM.getFirstActiveBgCall().hangup();
                            } catch (Exception e) {
                                log(e.toString());
                            }
                        }
                        PhoneUtils.answerCall(ringing); 
                        return;
                    }
                }

                if (hasActiveCall && hasHoldingCall) {
                    if (DBG) log("internalAnswerCall: answering (both lines in use!)...");
                    // The relatively rare case where both lines are
                    // already in use.  We "answer incoming, end ongoing"
                    // in this case, according to the current UI spec.
                    PhoneUtils.answerAndEndActive(mCM, mCM.getFirstActiveRingingCall());
//MTK add below one line:
                    setOnAnswerAndEndFlag(true);

                    // Alternatively, we could use
                    // PhoneUtils.answerAndEndHolding(mPhone);
                    // here to end the on-hold call instead.
                } else {
                    if (DBG) log("internalAnswerCall: answering...");
                    PhoneUtils.answerCall(ringing);  // Automatically holds the current active call,
                                                    // if there is one
                }
            } else {
                throw new IllegalStateException("Unexpected phone type: " + phoneType);
            }

            // Call origin is valid only with outgoing calls. Disable it on incoming calls.
            mApp.setLatestActiveCallOrigin(null);
        }
    }

    /**
     * Answer the ringing call *and* hang up the ongoing call.
     */
    private void internalAnswerAndEnd() {
        if (DBG) log("internalAnswerAndEnd()...");
        if (VDBG) PhoneUtils.dumpCallManager();
        // In the rare case when multiple calls are ringing, the UI policy
        // it to always act on the first ringing call.
        PhoneUtils.answerAndEndActive(mCM, mCM.getFirstActiveRingingCall());
    }

    /**
     * Hang up the ringing call (aka "Don't answer").
     */
    /* package */ void hangupRingingCall() {
        if (DBG) log("hangupRingingCall()...");
        if (VDBG) PhoneUtils.dumpCallManager();
        // In the rare case when multiple calls are ringing, the UI policy
        // it to always act on the first ringing call.
        if (DualTalkUtils.isSupportDualTalk() && mDualTalk.hasMultipleRingingCall()) {
            PhoneUtils.hangupForDualTalk(mDualTalk.getFirstActiveRingingCall());
            PhoneGlobals.getInstance().notifier.switchRingToneByNeeded(mDualTalk.getSecondActiveRingCall());
        } else {
            PhoneUtils.hangupRingingCall(mCM.getFirstActiveRingingCall());
            /// M: For ALPS00602096
            //  Silence the Ringer when user reject the incoming call,
            //  avoid the DISCONNECT message delayed for busy system.
            internalSilenceRinger();
        }
    }

    /**
     * Silence the ringer (if an incoming call is ringing.)
     */
    private void internalSilenceRinger() {
        mSwappingCalls = false;
        if (DBG) log("internalSilenceRinger()...");
        final CallNotifier notifier = mApp.notifier;
        if (notifier.isRinging()) {
            // ringer is actually playing, so silence it.
            notifier.silenceRinger();
        }
    }

    /**
     * Hang up the all calls.
     */
    public void internalHangupAll() {
        if (DBG) {
            log("internalHangupAll()...");
        }
        GeminiUtils.hangupAll(mPhone);
    }

    /**
     * Respond via SMS to the ringing call.
     * @see RespondViaSmsManager
     */
    private void internalRespondViaSms() {
        log("internalRespondViaSms()...");
        if (VDBG) PhoneUtils.dumpCallManager();

        // In the rare case when multiple calls are ringing, the UI policy
        // it to always act on the first ringing call.
        Call ringingCall = null;
        if (DualTalkUtils.isSupportDualTalk() && mDualTalk.hasMultipleRingingCall()) {
            ringingCall = mDualTalk.getFirstActiveRingingCall();
        } else {
            ringingCall = mCM.getFirstActiveRingingCall();
        }

        mRespondViaSmsManager.showRespondViaSmsPopup(ringingCall);

        // Silence the ringer, since it would be distracting while you're trying
        // to pick a response.  (Note that we'll restart the ringer if you bail
        // out of the popup, though; see RespondViaSmsCancelListener.)
        internalSilenceRinger();
    }

    /**
     * Hang up the current active call.
     */
    private void internalHangup() {
        PhoneConstants.State state = mCM.getState();
        log("internalHangup()...  phone state = " + state);

        // Regardless of the phone state, issue a hangup request.
        // (If the phone is already idle, this call will presumably have no
        // effect (but also see the note below.))
        PhoneUtils.hangup(mCM);

        // If the user just hung up the only active call, we'll eventually exit
        // the in-call UI after the following sequence:
        // - When the hangup() succeeds, we'll get a DISCONNECT event from
        //   the telephony layer (see onDisconnect()).
        // - We immediately switch to the "Call ended" state (see the "delayed
        //   bailout" code path in onDisconnect()) and also post a delayed
        //   DELAYED_CLEANUP_AFTER_DISCONNECT message.
        // - When the DELAYED_CLEANUP_AFTER_DISCONNECT message comes in (see
        //   delayedCleanupAfterDisconnect()) we do some final cleanup, and exit
        //   this activity unless the phone is still in use (i.e. if there's
        //   another call, or something else going on like an active MMI
        //   sequence.)

        if (state == PhoneConstants.State.IDLE) {
            // The user asked us to hang up, but the phone was (already) idle!
            Log.w(LOG_TAG, "internalHangup(): phone is already IDLE!");

            // This is rare, but can happen in a few cases:
            // (a) If the user quickly double-taps the "End" button.  In this case
            //   we'll see that 2nd press event during the brief "Call ended"
            //   state (where the phone is IDLE), or possibly even before the
            //   radio has been able to respond to the initial hangup request.
            // (b) More rarely, this can happen if the user presses "End" at the
            //   exact moment that the call ends on its own (like because of the
            //   other person hanging up.)
            // (c) Finally, this could also happen if we somehow get stuck here on
            //   the InCallScreen with the phone truly idle, perhaps due to a
            //   bug where we somehow *didn't* exit when the phone became idle
            //   in the first place.

            // TODO: as a "safety valve" for case (c), consider immediately
            // bailing out of the in-call UI right here.  (The user can always
            // bail out by pressing Home, of course, but they'll probably try
            // pressing End first.)
            //
            //    Log.i(LOG_TAG, "internalHangup(): phone is already IDLE!  Bailing out...");
            //    endInCallScreenSession();
            
            if (mCallCard.getVisibility() == View.INVISIBLE || mCallCard.getVisibility() == View.GONE) {
                endInCallScreenSession();
                Log.w(LOG_TAG, "internalHangup(): phone is already IDLE, end ourself!");
            }
        }
    }

    /**
     * InCallScreen-specific wrapper around PhoneUtils.switchHoldingAndActive().
     */
    private void internalSwapCalls() {
        if (DBG) log("internalSwapCalls()...");
        mSwappingCalls = true;//the fc and bc is swapping
        // Any time we swap calls, force the DTMF dialpad to close.
        // (We want the regular in-call UI to be visible right now, so the
        // user can clearly see which call is now in the foreground.)
        closeDialpadInternal(true);  // do the "closing" animation

        // Also, clear out the "history" of DTMF digits you typed, to make
        // sure you don't see digits from call #1 while call #2 is active.
        // (Yes, this does mean that swapping calls twice will cause you
        // to lose any previous digits from the current call; see the TODO
        // comment on DTMFTwelvKeyDialer.clearDigits() for more info.)
        mDialer.clearDigits();

        // Swap the fg and bg calls.
        // In the future we may provides some way for user to choose among
        // multiple background calls, for now, always act on the first background calll.
        if (DualTalkUtils.isSupportDualTalk() && mDualTalk.isCdmaAndGsmActive()) {
            handleSwapCdmaAndGsm();
        } else if (DualTalkUtils.isSupportDualTalk() && mDualTalk.hasDualHoldCallsOnly()) {
            //According to planner's define:
            //If there are two calls both in hold status, when tap the
            //swap call button, it will unhold the "background hold call"
            Call bgHoldCall = mDualTalk.getSecondActiveBgCall();
            try {
                bgHoldCall.getPhone().switchHoldingAndActive();
            } catch (CallStateException e) {
                log("internalSwapCalls exception = " + e);
            }
            //Before solution: only switch the two hold call but not change the status
            //mDualTalk.switchCalls();
            //updateScreen();
        } else if (DualTalkUtils.isSupportDualTalk() && mDualTalk.isDualTalkMultipleHoldCase()) {
            Call fgCall = mDualTalk.getActiveFgCall();
            Phone fgPhone = fgCall.getPhone();
            if (fgPhone.getBackgroundCall().getState().isAlive()) {
                if (DBG) {
                    log("Cal foreground phone's switchHoldingAndActive");
                }
                try {
                    fgPhone.switchHoldingAndActive();
                } catch (CallStateException e) {
                    log(e.toString());
                }
            } else {
                if (DBG) {
                    log("PhoneUtils.switchHoldingAndActive");
                }
                PhoneUtils.switchHoldingAndActive(mDualTalk.getFirstActiveBgCall());
            }
            //PhoneUtils.switchHoldingAndActive(mCM.getFirstActiveBgCall());
        } else {
            PhoneUtils.switchHoldingAndActive(mCM.getFirstActiveBgCall());
        }

        // If we have a valid BluetoothPhoneService then since CDMA network or
        // Telephony FW does not send us information on which caller got swapped
        // we need to update the second call active state in BluetoothPhoneService internally
        if (mCM.getBgPhone().getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA) {
            IBluetoothHeadsetPhone btPhone = mApp.getBluetoothPhoneService();
            if (btPhone != null) {
                try {
                    btPhone.cdmaSwapSecondCallState();
                } catch (RemoteException e) {
                    Log.e(LOG_TAG, Log.getStackTraceString(new Throwable()));
                }
            }
        }

    }

    /**
     * Sets the current high-level "mode" of the in-call UI.
     *
     * NOTE: if newMode is CALL_ENDED, the caller is responsible for
     * posting a delayed DELAYED_CLEANUP_AFTER_DISCONNECT message, to make
     * sure the "call ended" state goes away after a couple of seconds.
     *
     * Note this method does NOT refresh of the onscreen UI; the caller is
     * responsible for calling updateScreen() or requestUpdateScreen() if
     * necessary.
     */
    private void setInCallScreenMode(InCallScreenMode newMode) {
        if (DBG) log("setInCallScreenMode: " + newMode);
        mApp.inCallUiState.inCallScreenMode = newMode;

        switch (newMode) {
            case MANAGE_CONFERENCE:
                if (!PhoneUtils.isConferenceCall(mCM.getActiveFgCall())) {
                    Log.w(LOG_TAG, "MANAGE_CONFERENCE: no active conference call!");
                    // Hide the Manage Conference panel, return to NORMAL mode.
                    setInCallScreenMode(InCallScreenMode.NORMAL);
                    return;
                }
                List<Connection> connections = mCM.getFgCallConnections();
                // There almost certainly will be > 1 connection,
                // since isConferenceCall() just returned true.
                if ((connections == null) || (connections.size() <= 1)) {
                    Log.w(LOG_TAG,
                          "MANAGE_CONFERENCE: Bogus TRUE from isConferenceCall(); connections = "
                          + connections);
                    // Hide the Manage Conference panel, return to NORMAL mode.
                    setInCallScreenMode(InCallScreenMode.NORMAL);
                    return;
                }

                // TODO: Don't do this here. The call to
                // initManageConferencePanel() should instead happen
                // automagically in ManageConferenceUtils the very first
                // time you call updateManageConferencePanel() or
                // setPanelVisible(true).
                mManageConferenceUtils.initManageConferencePanel();  // if necessary

                mManageConferenceUtils.updateManageConferencePanel(connections);

                // The "Manage conference" UI takes up the full main frame,
                // replacing the CallCard PopupWindow.
                mManageConferenceUtils.setPanelVisible(true);

                // Start the chronometer.
                // TODO: Similarly, we shouldn't expose startConferenceTime()
                // and stopConferenceTime(); the ManageConferenceUtils
                // class ought to manage the conferenceTime widget itself
                // based on setPanelVisible() calls.

                // Note: there is active Fg call since we are in conference call
                long callDuration =
                        mCM.getActiveFgCall().getEarliestConnection().getDurationMillis();
                mManageConferenceUtils.startConferenceTime(
                        SystemClock.elapsedRealtime() - callDuration);

                // No need to close the dialer here, since the Manage
                // Conference UI will just cover it up anyway.

                break;

            case CALL_ENDED:
                mManageConferenceUtils.setPanelVisible(false);
                mManageConferenceUtils.stopConferenceTime();
                // Make sure the CallCard is visible.
                mCallCard.setVisibility(View.VISIBLE);
                log("setInCallScreenMode(CALL_ENDED): Set mCallCard VISIBLE");
                break;

            case NORMAL:
                mManageConferenceUtils.setPanelVisible(false);
                mManageConferenceUtils.stopConferenceTime();
                if (mCM.getState() == PhoneConstants.State.IDLE) {
                    mCallCard.setVisibility(View.GONE);
                } else {
                    mCallCard.setVisibility(View.VISIBLE);
                }
                log("setInCallScreenMode: (NORMAL) Set mCallCard VISIBLE +"
                        + mCallCard.getVisibility());
                break;

            case OTA_NORMAL:
                mApp.otaUtils.setCdmaOtaInCallScreenUiState(
                        OtaUtils.CdmaOtaInCallScreenUiState.State.NORMAL);
                mCallCard.setVisibility(View.GONE);
                break;

            case OTA_ENDED:
                mApp.otaUtils.setCdmaOtaInCallScreenUiState(
                        OtaUtils.CdmaOtaInCallScreenUiState.State.ENDED);
                mCallCard.setVisibility(View.GONE);
                break;

            case UNDEFINED:
                // Set our Activities intent to ACTION_UNDEFINED so
                // that if we get resumed after we've completed a call
                // the next call will not cause checkIsOtaCall to
                // return true.
                //
                // TODO(OTASP): update these comments
                //
                // With the framework as of October 2009 the sequence below
                // causes the framework to call onResume, onPause, onNewIntent,
                // onResume. If we don't call setIntent below then when the
                // first onResume calls checkIsOtaCall via checkOtaspStateOnResume it will
                // return true and the Activity will be confused.
                //
                //  1) Power up Phone A
                //  2) Place *22899 call and activate Phone A
                //  3) Press the power key on Phone A to turn off the display
                //  4) Call Phone A from Phone B answering Phone A
                //  5) The screen will be blank (Should be normal InCallScreen)
                //  6) Hang up the Phone B
                //  7) Phone A displays the activation screen.
                //
                // Step 3 is the critical step to cause the onResume, onPause
                // onNewIntent, onResume sequence. If step 3 is skipped the
                // sequence will be onNewIntent, onResume and all will be well.
                setIntent(new Intent(ACTION_UNDEFINED));

                // Cleanup Ota Screen if necessary and set the panel
                // to VISIBLE.
                if (mCM.getState() != PhoneConstants.State.OFFHOOK) {
                    if (mApp.otaUtils != null) {
                        mApp.otaUtils.cleanOtaScreen(true);
                    }
                } else {
                    log("WARNING: Setting mode to UNDEFINED but phone is OFFHOOK,"
                            + " skip cleanOtaScreen.");
                }
                mCallCard.setVisibility(View.VISIBLE);
                log("setInCallScreenMode: (UNDEFINED): Set mCallCard VISIBLE");
                break;
        }
    }

    /**
     * @return true if the "Manage conference" UI is currently visible.
     */
    /* package */ boolean isManageConferenceMode() {
        return (mApp.inCallUiState.inCallScreenMode == InCallScreenMode.MANAGE_CONFERENCE);
    }

    /**
     * Checks if the "Manage conference" UI needs to be updated.
     * If the state of the current conference call has changed
     * since our previous call to updateManageConferencePanel()),
     * do a fresh update.  Also, if the current call is no longer a
     * conference call at all, bail out of the "Manage conference" UI and
     * return to InCallScreenMode.NORMAL mode.
     */
    private void updateManageConferencePanelIfNecessary() {
        if (VDBG) log("updateManageConferencePanelIfNecessary: " + mCM.getActiveFgCall() + "...");

        List<Connection> connections = mCM.getFgCallConnections();
        if (connections == null) {
            if (VDBG) log("==> no connections on foreground call!");
            // Hide the Manage Conference panel, return to NORMAL mode.
            setInCallScreenMode(InCallScreenMode.NORMAL);
            SyncWithPhoneStateStatus status = syncWithPhoneState();
            if (status != SyncWithPhoneStateStatus.SUCCESS) {
                Log.w(LOG_TAG, "- syncWithPhoneState failed! status = " + status);
                // We shouldn't even be in the in-call UI in the first
                // place, so bail out:
                if (DBG) log("updateManageConferencePanelIfNecessary: endInCallScreenSession... 1");
                endInCallScreenSession();
                return;
            }
            return;
        }

        int numConnections = connections.size();
        if (numConnections <= 1) {
            if (VDBG) log("==> foreground call no longer a conference!");
            // Hide the Manage Conference panel, return to NORMAL mode.
            setInCallScreenMode(InCallScreenMode.NORMAL);
            SyncWithPhoneStateStatus status = syncWithPhoneState();
            if (status != SyncWithPhoneStateStatus.SUCCESS) {
                Log.w(LOG_TAG, "- syncWithPhoneState failed! status = " + status);
                // We shouldn't even be in the in-call UI in the first
                // place, so bail out:
                if (DBG) log("updateManageConferencePanelIfNecessary: endInCallScreenSession... 2");
                endInCallScreenSession();
                return;
            }
            return;
        }

        // TODO: the test to see if numConnections has changed can go in
        // updateManageConferencePanel(), rather than here.
        if (numConnections != mManageConferenceUtils.getNumCallersInConference()) {
            if (VDBG) log("==> Conference size has changed; need to rebuild UI!");
            mManageConferenceUtils.updateManageConferencePanel(connections);
        }
    }

    /**
     * Updates {@link #mCallCard}'s visibility state per DTMF dialpad visibility. They
     * cannot be shown simultaneously and thus we should reflect DTMF dialpad visibility into
     * another.
     *
     * Note: During OTA calls or users' managing conference calls, we should *not* call this method
     * but manually manage both visibility.
     *
     * @see #updateScreen()
     */
    private void updateCallCardVisibilityPerDialerState(boolean animate) {
        // We need to hide the CallCard while the dialpad is visible.
        /** M: New Feature Phone Landscape UI @{ */
        // CallCard is always visible in landscape view
        if (isDialerOpened() && !PhoneUtils.isLandscape(InCallScreen.this)) {
        /** @ }*/
            if (VDBG) {
                log("- updateCallCardVisibilityPerDialerState(animate="
                        + animate + "): dialpad open, hide mCallCard...");
            }
            if (animate) {
                AnimationUtils.Fade.hide(mCallCard, View.GONE);
            } else {
                mCallCard.setVisibility(View.GONE);
            }
        } else {
            // Dialpad is dismissed; bring back the CallCard if it's supposed to be visible.
            if ((mApp.inCallUiState.inCallScreenMode == InCallScreenMode.NORMAL)
                || (mApp.inCallUiState.inCallScreenMode == InCallScreenMode.CALL_ENDED)) {
                if (VDBG) {
                    log("- updateCallCardVisibilityPerDialerState(animate="
                            + animate + "): dialpad dismissed, show mCallCard...");
                }
                if (animate) {
                    AnimationUtils.Fade.show(mCallCard);
                } else {
                    mCallCard.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    /**
     * @see DTMFTwelveKeyDialer#isOpened()
     */
    /* package */ boolean isDialerOpened() {
        return (mDialer != null && mDialer.isOpened());
    }

    /**
     * Called any time the DTMF dialpad is opened.
     * @see DTMFTwelveKeyDialer#openDialer(boolean)
     */
    /* package */ void onDialerOpen(boolean animate) {
        if (DBG) log("onDialerOpen()...");

        // Update the in-call touch UI.
        updateInCallTouchUi();

        // Update CallCard UI, which depends on the dialpad.
        updateCallCardVisibilityPerDialerState(animate);

        // This counts as explicit "user activity".
        mApp.pokeUserActivity();

        //If on OTA Call, hide OTA Screen
        // TODO: This may not be necessary, now that the dialpad is
        // always visible in OTA mode.
        if  ((mApp.inCallUiState.inCallScreenMode == InCallScreenMode.OTA_NORMAL
                || mApp.inCallUiState.inCallScreenMode == InCallScreenMode.OTA_ENDED)
                && mApp.otaUtils != null) {
            mApp.otaUtils.hideOtaScreen();
        }
        if (FeatureOption.MTK_VT3G324M_SUPPORT) {
            if (null != mVTInCallScreen) {
                mVTInCallScreen.updateVTScreen(mVTInCallScreen.getVTScreenMode());
            }
        }
    }

    /**
     * Called any time the DTMF dialpad is closed.
     * @see DTMFTwelveKeyDialer#closeDialer(boolean)
     */
    /* package */ void onDialerClose(boolean animate) {
        if (DBG) log("onDialerClose()...");

        // OTA-specific cleanup upon closing the dialpad.
        if (FeatureOption.MTK_VT3G324M_SUPPORT) {
            if (null != mVTInCallScreen) {
                mVTInCallScreen.updateVTScreen(mVTInCallScreen.getVTScreenMode());
            }
        }

        if ((mApp.inCallUiState.inCallScreenMode == InCallScreenMode.OTA_NORMAL)
            || (mApp.inCallUiState.inCallScreenMode == InCallScreenMode.OTA_ENDED)
            || ((mApp.cdmaOtaScreenState != null)
                && (mApp.cdmaOtaScreenState.otaScreenState ==
                    CdmaOtaScreenState.OtaScreenState.OTA_STATUS_ACTIVATION))) {
            if (mApp.otaUtils != null) {
                mApp.otaUtils.otaShowProperScreen();
            }
        }

        // Update the in-call touch UI.
        updateInCallTouchUi();

        // Update CallCard UI, which depends on the dialpad.
        updateCallCardVisibilityPerDialerState(animate);

        // This counts as explicit "user activity".
        mApp.pokeUserActivity();
        requestUpdateScreen();
    }

    /**
     * Determines when we can dial DTMF tones.
     */
    /* package */ boolean okToDialDTMFTones() {
        final boolean hasRingingCall = mCM.hasActiveRingingCall();
        final Call.State fgCallState = mCM.getActiveFgCall().getState();
        final Call.State bgCallState = mCM.getFirstActiveBgCall().getState();

        // We're allowed to send DTMF tones when there's an ACTIVE
        // foreground call, and not when an incoming call is ringing
        // (since DTMF tones are useless in that state), or if the
        // Manage Conference UI is visible (since the tab interferes
        // with the "Back to call" button.)

        // We can also dial while in ALERTING state because there are
        // some connections that never update to an ACTIVE state (no
        // indication from the network).
        boolean noConference = mApp.inCallUiState.inCallScreenMode != InCallScreenMode.MANAGE_CONFERENCE;
        boolean canDial =
            (fgCallState == Call.State.ACTIVE || fgCallState == Call.State.ALERTING || bgCallState == Call.State.HOLDING)
            && !hasRingingCall
            && noConference;

        if (VDBG) log ("[okToDialDTMFTones] foreground state: " + fgCallState +
                       ", background state: " + bgCallState +
                ", ringing state: " + hasRingingCall +
                ", call screen mode: " + mApp.inCallUiState.inCallScreenMode +
                ", result: " + canDial);

        return canDial ? true : (mSpeechEnabled && noConference && !hasRingingCall);
       
        //return mSpeechEnabled;
    }

    /**
     * @return true if the in-call DTMF dialpad should be available to the
     *      user, given the current state of the phone and the in-call UI.
     *      (This is used to control the enabledness of the "Show
     *      dialpad" onscreen button; see InCallControlState.dialpadEnabled.)
     */
    /* package */ boolean okToShowDialpad() {
        // Very similar to okToDialDTMFTones(), but allow DIALING here.
        final Call.State fgCallState = mCM.getActiveFgCallState();
        return okToDialDTMFTones() || (fgCallState == Call.State.DIALING);
    }

    /**
     * Initializes the in-call touch UI on devices that need it.
     */
    private void initInCallTouchUi() {
        if (DBG) log("initInCallTouchUi()...");
        // TODO: we currently use the InCallTouchUi widget in at least
        // some states on ALL platforms.  But if some devices ultimately
        // end up not using *any* onscreen touch UI, we should make sure
        // to not even inflate the InCallTouchUi widget on those devices.
        mInCallTouchUi = (InCallTouchUi) findViewById(R.id.inCallTouchUi);
        mInCallTouchUi.setInCallScreenInstance(this);

        // RespondViaSmsManager implements the "Respond via SMS"
        // feature that's triggered from the incoming call widget.
        mRespondViaSmsManager = new RespondViaSmsManager();
        mRespondViaSmsManager.setInCallScreenInstance(this);
    }

    /**
     * Updates the state of the in-call touch UI.
     */
    private void updateInCallTouchUi() {
        if (mInCallTouchUi != null) {
            mInCallTouchUi.updateState(mCM);
        }
    }

    /**
     * @return the InCallTouchUi widget
     */
    /* package */ InCallTouchUi getInCallTouchUi() {
        return mInCallTouchUi;
    }

    /**
     * @return the mCallCard widget
     */
    /* package */ CallCard getCallCardOnlyForTest() {
        return mCallCard;
    }

    /**
     * @return the mCallTime widget
     */
    /* package */ CallTime getCallTimeOnlyForTest() {
        return mCallTime; 
    }

    /**
     * @return the mVoiceRecorderIcon widget
     */
    /* package */ ImageView getVoiceRecorderIconOnlyForTest() {
        return mVoiceRecorderIcon;
    }

    /**
     * @return the mRespondViaSmsManager widget
     */
    /* package */ RespondViaSmsManager getRespondViaSmsManagerOnlyForTest() {
        return mRespondViaSmsManager;
    }

    /**
     * @return the mInCallControlState widget
     */
    /* package */ InCallControlState getInCallControlStateOnlyForTest() {
        return mInCallControlState;
    }

    /**
     * @return the mInCallMenuState widget
     */
    /* package */ InCallMenuState getInCallMenuStateOnlyForTest() {
        return mInCallMenuState;
    }

    /**
     * @return the mManageConferenceUtils widget
     */
    /* package */ ManageConferenceUtils getManageConferenceUtilsOnlyForTest() {
        return mManageConferenceUtils;
    }

    /**
     * @return the mReceiver widget
     */
    /* package */ BroadcastReceiver getmReceiverOnlyForTest() {
        return mReceiver;
    }

    /**
     * @return the mLocaleChangeReceiver widget
     */
    /* package */ BroadcastReceiver mLocaleChangeReceiverOnlyForTest() {
        return mLocaleChangeReceiver;
    }

    /**
     * @return the mDMLockReceiver widget
     */
    /* package */ BroadcastReceiver getmDMLockReceiverOnlyForTest() {
        return mDMLockReceiver;
    }

    /**
     * @return the mLocaleChanged widget
     */
    public static boolean getmLocaleChangedOnlyForTest() {
        return mLocaleChanged;
    }

    /**
     * @return the mApp widget
     */
    /* package */ PhoneGlobals getmAppOnlyForTest() {
        return mApp;
    }

    /**
     * @return mIsForegroundActivity
     */
    /* package */ boolean getmIsForegroundActivityOnlyForTest() {
        return mIsForegroundActivity;
    }

     /**
     * @return mIsForegroundActivityForProximity
     */
    /* package */ boolean getmIsForegroundActivityForProximityOnlyForTest() {
        return mIsForegroundActivityForProximity;
    }

    /**
     * @return mPowerManager
     */
    /* package */ PowerManager getmPowerManagerOnlyForTest() {
        return mPowerManager;
    }

    /**
     * @return mDialer
     */
    /* package */ DTMFTwelveKeyDialer getmDialerOnlyForTest() {
        return mDialer;
    }   

    /**
     * @return mPopupMenu
     */
    /* package */ PopupMenu getmPopupMenuOnlyForTest() {
        return mPopupMenu;
    }

    /**
     * @return mCM
     */
    /* package */ CallManager getmCMOnlyForTest() {
        return mCM;
    }

    /**
     * Posts a handler message telling the InCallScreen to refresh the
     * onscreen in-call UI.
     *
     * This is just a wrapper around updateScreen(), for use by the
     * rest of the phone app or from a thread other than the UI thread.
     *
     * updateScreen() is a no-op if the InCallScreen is not the foreground
     * activity, so it's safe to call this whether or not the InCallScreen
     * is currently visible.
     */
    public void requestUpdateScreen() {
        if (DBG) log("requestUpdateScreen()...");
        mHandler.removeMessages(REQUEST_UPDATE_SCREEN);
        /**
         * add by mediatek .inc
         * description : a little tricky here, do not
         * update screen when : 1A1W and the waiting call query
         * is not completed ( ALPS000231023 )
         */
        if (ignoreUpdateScreen()) {
            if (DBG) {
                log("ignoreUpdateScreen");
            }
            return;
        }
        /*
         * change by mediatek .inc end
         */
        mHandler.sendEmptyMessage(REQUEST_UPDATE_SCREEN);
    }

    /**
     * @return true if we're in restricted / emergency dialing only mode.
     */
    public boolean isPhoneStateRestricted() {
        // TODO:  This needs to work IN TANDEM with the KeyGuardViewMediator Code.
        // Right now, it looks like the mInputRestricted flag is INTERNAL to the
        // KeyGuardViewMediator and SPECIFICALLY set to be FALSE while the emergency
        // phone call is being made, to allow for input into the InCallScreen.
        // Having the InCallScreen judge the state of the device from this flag
        // becomes meaningless since it is always false for us.  The mediator should
        // have an additional API to let this app know that it should be restricted.
        /* need to review if should check phone 2 or not */
        if (GeminiUtils.isGeminiSupport()) {
            boolean is_ecc_only = false;
            boolean is_out_of_service = false;
            /// M:Gemini+ @{
            final int[] geminiSlots = GeminiUtils.getSlots();
            for (int gs : geminiSlots) {
                final int serviceState = ((GeminiPhone) mPhone).getServiceStateGemini(gs).getState();
                if (serviceState == ServiceState.STATE_EMERGENCY_ONLY) {
                    is_ecc_only = true;
                }
                if (serviceState == ServiceState.STATE_OUT_OF_SERVICE) {
                    is_out_of_service = true;
                }
                if (is_ecc_only && is_out_of_service) {
                    break;
                }
            }
            /// @}
            return (is_ecc_only || is_out_of_service || (PhoneGlobals.getInstance().getKeyguardManager()
                    .inKeyguardRestrictedInputMode()));
        }

        int serviceState = mCM.getServiceState();
        return ((serviceState == ServiceState.STATE_EMERGENCY_ONLY) ||
                (serviceState == ServiceState.STATE_OUT_OF_SERVICE) ||
                (mApp.getKeyguardManager().inKeyguardRestrictedInputMode()));
    }

    public boolean isPhoneStateRestricted(int simId) {
        if (GeminiUtils.isGeminiSupport()) {
            int serviceStateGemini = ((GeminiPhone)mPhone).getServiceStateGemini(simId).getState();
            if (DBG) {
                log("isPhoneStateRestricted - sim : " + simId + " state: " + serviceStateGemini);
            }
            return ((serviceStateGemini == ServiceState.STATE_EMERGENCY_ONLY) ||
                    (serviceStateGemini == ServiceState.STATE_OUT_OF_SERVICE) ||
                    (PhoneGlobals.getInstance().getKeyguardManager().inKeyguardRestrictedInputMode()));
        } else {
            int serviceState = mCM.getServiceState();
            return ((serviceState == ServiceState.STATE_EMERGENCY_ONLY) ||
                (serviceState == ServiceState.STATE_OUT_OF_SERVICE) ||
                (PhoneGlobals.getInstance().getKeyguardManager().inKeyguardRestrictedInputMode()));
        }
    }

    //
    // Bluetooth helper methods.
    //
    // - BluetoothAdapter is the Bluetooth system service.  If
    //   getDefaultAdapter() returns null
    //   then the device is not BT capable.  Use BluetoothDevice.isEnabled()
    //   to see if BT is enabled on the device.
    //
    // - BluetoothHeadset is the API for the control connection to a
    //   Bluetooth Headset.  This lets you completely connect/disconnect a
    //   headset (which we don't do from the Phone UI!) but also lets you
    //   get the address of the currently active headset and see whether
    //   it's currently connected.

    /**
     * @return true if the Bluetooth on/off switch in the UI should be
     *         available to the user (i.e. if the device is BT-capable
     *         and a headset is connected.)
     */
    public boolean isBluetoothAvailable() {
        if (VDBG) log("isBluetoothAvailable()...");

        // There's no need to ask the Bluetooth system service if BT is enabled:
        //
        //    BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        //    if ((adapter == null) || !adapter.isEnabled()) {
        //        if (DBG) log("  ==> FALSE (BT not enabled)");
        //        return false;
        //    }
        //    if (DBG) log("  - BT enabled!  device name " + adapter.getName()
        //                 + ", address " + adapter.getAddress());
        //
        // ...since we already have a BluetoothHeadset instance.  We can just
        // call isConnected() on that, and assume it'll be false if BT isn't
        // enabled at all.

        // Check if there's a connected headset, using the BluetoothHeadset API.
        boolean isConnected = false;
        if (mBluetoothHeadset != null) {
            List<BluetoothDevice> deviceList = mBluetoothHeadset.getConnectedDevices();

            if (deviceList.size() > 0) {
                BluetoothDevice device = deviceList.get(0);
                isConnected = true;

                if (VDBG) log("  - headset state = " +
                              mBluetoothHeadset.getConnectionState(device));
                if (VDBG) log("  - headset address: " + device);
                if (VDBG) log("  - isConnected: " + isConnected);
            }
        }

        if (VDBG) log("  ==> " + isConnected);
        return isConnected;
    }

    /**
     * @return true if a BT Headset is available, and its audio is currently connected.
     */
    /* package */ boolean isBluetoothAudioConnected() {
        if (mBluetoothHeadset == null) {
            if (VDBG) log("isBluetoothAudioConnected: ==> FALSE (null mBluetoothHeadset)");
            return false;
        }
        List<BluetoothDevice> deviceList = mBluetoothHeadset.getConnectedDevices();

        if (deviceList.isEmpty()) {
            return false;
        }
        BluetoothDevice device = deviceList.get(0);
        boolean isAudioOn = mBluetoothHeadset.isAudioConnected(device);
        if (VDBG) log("isBluetoothAudioConnected: ==> isAudioOn = " + isAudioOn);
        return isAudioOn;
    }

    /**
     * Helper method used to control the onscreen "Bluetooth" indication;
     * see InCallControlState.bluetoothIndicatorOn.
     *
     * @return true if a BT device is available and its audio is currently connected,
     *              <b>or</b> if we issued a  BluetoothHandsfree.userWantsAudioOn()
     *              call within the last 5 seconds (which presumably means
     *              that the BT audio connection is currently being set
     *              up, and will be connected soon.)
     */
    /* package */ boolean isBluetoothAudioConnectedOrPending() {
        if (isBluetoothAudioConnected()) {
            if (VDBG) log("isBluetoothAudioConnectedOrPending: ==> TRUE (really connected)");
            return true;
        }

        // If we issued a connectAudio() call "recently enough", even
        // if BT isn't actually connected yet, let's still pretend BT is
        // on.  This makes the onscreen indication more responsive.
        if (mBluetoothConnectionPending) {
            long timeSinceRequest =
                    SystemClock.elapsedRealtime() - mBluetoothConnectionRequestTime;
            if (timeSinceRequest < 5000 /* 5 seconds */) {
                if (VDBG) log("isBluetoothAudioConnectedOrPending: ==> TRUE (requested "
                             + timeSinceRequest + " msec ago)");
                return true;
            } else {
                if (VDBG) log("isBluetoothAudioConnectedOrPending: ==> FALSE (request too old: "
                             + timeSinceRequest + " msec ago)");
                mBluetoothConnectionPending = false;
                return false;
            }
        }

        if (VDBG) log("isBluetoothAudioConnectedOrPending: ==> FALSE");
        return false;
    }

    /**
     * Posts a message to our handler saying to update the onscreen UI
     * based on a bluetooth headset state change.
     */
    /* package */ void requestUpdateBluetoothIndication() {
        if (VDBG) log("requestUpdateBluetoothIndication()...");
        // No need to look at the current state here; any UI elements that
        // care about the bluetooth state (i.e. the CallCard) get
        // the necessary state directly from PhoneGlobals.showBluetoothIndication().
        mHandler.removeMessages(REQUEST_UPDATE_BLUETOOTH_INDICATION);
        mHandler.sendEmptyMessage(REQUEST_UPDATE_BLUETOOTH_INDICATION);
    }

    private void dumpBluetoothState() {
        log("============== dumpBluetoothState() =============");
        log("= isBluetoothAvailable: " + isBluetoothAvailable());
        log("= isBluetoothAudioConnected: " + isBluetoothAudioConnected());
        log("= isBluetoothAudioConnectedOrPending: " + isBluetoothAudioConnectedOrPending());
        log("= PhoneGlobals.showBluetoothIndication: "
            + mApp.showBluetoothIndication());
        log("=");
        if (mBluetoothAdapter != null) {
            if (mBluetoothHeadset != null) {
                List<BluetoothDevice> deviceList = mBluetoothHeadset.getConnectedDevices();

                if (deviceList.size() > 0) {
                    BluetoothDevice device = deviceList.get(0);
                    log("= BluetoothHeadset.getCurrentDevice: " + device);
                    log("= BluetoothHeadset.State: "
                        + mBluetoothHeadset.getConnectionState(device));
                    log("= BluetoothHeadset audio connected: " +
                        mBluetoothHeadset.isAudioConnected(device));
                }
            } else {
                log("= mBluetoothHeadset is null");
            }
        } else {
            log("= mBluetoothAdapter is null; device is not BT capable");
        }
    }

    /* package */ void connectBluetoothAudio() {
        if (VDBG) log("connectBluetoothAudio()...");
        if (mBluetoothHeadset != null) {
            // TODO(BT) check return
            mBluetoothHeadset.connectAudio();
        }

        // Watch out: The bluetooth connection doesn't happen instantly;
        // the connectAudio() call returns instantly but does its real
        // work in another thread.  The mBluetoothConnectionPending flag
        // is just a little trickery to ensure that the onscreen UI updates
        // instantly. (See isBluetoothAudioConnectedOrPending() above.)
        mBluetoothConnectionPending = true;
        mBluetoothConnectionRequestTime = SystemClock.elapsedRealtime();
    }

    /* package */ void disconnectBluetoothAudio() {
        if (VDBG) log("disconnectBluetoothAudio()...");
        if (mBluetoothHeadset != null) {
            mBluetoothHeadset.disconnectAudio();
        }
        mBluetoothConnectionPending = false;
    }

    /**
     * Posts a handler message telling the InCallScreen to close
     * the OTA failure notice after the specified delay.
     * @see OtaUtils.otaShowProgramFailureNotice
     */
    /* package */ void requestCloseOtaFailureNotice(long timeout) {
        if (DBG) log("requestCloseOtaFailureNotice() with timeout: " + timeout);
        mHandler.sendEmptyMessageDelayed(REQUEST_CLOSE_OTA_FAILURE_NOTICE, timeout);

        // TODO: we probably ought to call removeMessages() for this
        // message code in either onPause or onResume, just to be 100%
        // sure that the message we just posted has no way to affect a
        // *different* call if the user quickly backs out and restarts.
        // (This is also true for requestCloseSpcErrorNotice() below, and
        // probably anywhere else we use mHandler.sendEmptyMessageDelayed().)
    }

    /**
     * Posts a handler message telling the InCallScreen to close
     * the SPC error notice after the specified delay.
     * @see OtaUtils.otaShowSpcErrorNotice
     */
    /* package */ void requestCloseSpcErrorNotice(long timeout) {
        if (DBG) log("requestCloseSpcErrorNotice() with timeout: " + timeout);
        mHandler.sendEmptyMessageDelayed(REQUEST_CLOSE_SPC_ERROR_NOTICE, timeout);
    }

    public boolean isOtaCallInActiveState() {
        if ((mApp.inCallUiState.inCallScreenMode == InCallScreenMode.OTA_NORMAL)
                || ((mApp.cdmaOtaScreenState != null)
                    && (mApp.cdmaOtaScreenState.otaScreenState ==
                        CdmaOtaScreenState.OtaScreenState.OTA_STATUS_ACTIVATION))) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Handle OTA Call End scenario when display becomes dark during OTA Call
     * and InCallScreen is in pause mode.  CallNotifier will listen for call
     * end indication and call this api to handle OTA Call end scenario
     */
    public void handleOtaCallEnd() {
        if (DBG) log("handleOtaCallEnd entering");
        if (((mApp.inCallUiState.inCallScreenMode == InCallScreenMode.OTA_NORMAL)
                || ((mApp.cdmaOtaScreenState != null)
                && (mApp.cdmaOtaScreenState.otaScreenState !=
                    CdmaOtaScreenState.OtaScreenState.OTA_STATUS_UNDEFINED)))
                && ((mApp.cdmaOtaProvisionData != null)
                && (!mApp.cdmaOtaProvisionData.inOtaSpcState))) {
            if (DBG) log("handleOtaCallEnd - Set OTA Call End stater");
            setInCallScreenMode(InCallScreenMode.OTA_ENDED);
            updateScreen();
        }
    }

    public boolean isOtaCallInEndState() {
        return (mApp.inCallUiState.inCallScreenMode == InCallScreenMode.OTA_ENDED);
    }


    /**
     * Upon resuming the in-call UI, check to see if an OTASP call is in
     * progress, and if so enable the special OTASP-specific UI.
     *
     * TODO: have a simple single flag in InCallUiState for this rather than
     * needing to know about all those mApp.cdma*State objects.
     *
     * @return true if any OTASP-related UI is active
     */
    private boolean checkOtaspStateOnResume() {
        // If there's no OtaUtils instance, that means we haven't even tried
        // to start an OTASP call (yet), so there's definitely nothing to do here.
        if (mApp.otaUtils == null) {
            if (DBG) log("checkOtaspStateOnResume: no OtaUtils instance; nothing to do.");
            return false;
        }

        if ((mApp.cdmaOtaScreenState == null) || (mApp.cdmaOtaProvisionData == null)) {
            // Uh oh -- something wrong with our internal OTASP state.
            // (Since this is an OTASP-capable device, these objects
            // *should* have already been created by PhoneGlobals.onCreate().)
            throw new IllegalStateException("checkOtaspStateOnResume: "
                                            + "app.cdmaOta* objects(s) not initialized");
        }

        // The PhoneGlobals.cdmaOtaInCallScreenUiState instance is the
        // authoritative source saying whether or not the in-call UI should
        // show its OTASP-related UI.

        OtaUtils.CdmaOtaInCallScreenUiState.State cdmaOtaInCallScreenState =
                mApp.otaUtils.getCdmaOtaInCallScreenUiState();
        // These states are:
        // - UNDEFINED: no OTASP-related UI is visible
        // - NORMAL: OTASP call in progress, so show in-progress OTASP UI
        // - ENDED: OTASP call just ended, so show success/failure indication

        boolean otaspUiActive =
                (cdmaOtaInCallScreenState == OtaUtils.CdmaOtaInCallScreenUiState.State.NORMAL)
                || (cdmaOtaInCallScreenState == OtaUtils.CdmaOtaInCallScreenUiState.State.ENDED);

        if (otaspUiActive) {
            // Make sure the OtaUtils instance knows about the InCallScreen's
            // OTASP-related UI widgets.
            //
            // (This call has no effect if the UI widgets have already been set up.
            // It only really matters  the very first time that the InCallScreen instance
            // is onResume()d after starting an OTASP call.)
            mApp.otaUtils.updateUiWidgets(this, mInCallTouchUi, mCallCard);

            // Also update the InCallScreenMode based on the cdmaOtaInCallScreenState.

            if (cdmaOtaInCallScreenState == OtaUtils.CdmaOtaInCallScreenUiState.State.NORMAL) {
                if (DBG) log("checkOtaspStateOnResume - in OTA Normal mode");
                setInCallScreenMode(InCallScreenMode.OTA_NORMAL);
            } else if (cdmaOtaInCallScreenState ==
                       OtaUtils.CdmaOtaInCallScreenUiState.State.ENDED) {
                if (DBG) log("checkOtaspStateOnResume - in OTA END mode");
                setInCallScreenMode(InCallScreenMode.OTA_ENDED);
            }

            // TODO(OTASP): we might also need to go into OTA_ENDED mode
            // in one extra case:
            //
            // else if (mApp.cdmaOtaScreenState.otaScreenState ==
            //            CdmaOtaScreenState.OtaScreenState.OTA_STATUS_SUCCESS_FAILURE_DLG) {
            //     if (DBG) log("checkOtaspStateOnResume - set OTA END Mode");
            //     setInCallScreenMode(InCallScreenMode.OTA_ENDED);
            // }

        } else {
            // OTASP is not active; reset to regular in-call UI.

            if (DBG) log("checkOtaspStateOnResume - Set OTA NORMAL Mode");
            setInCallScreenMode(InCallScreenMode.OTA_NORMAL);

            if (mApp.otaUtils != null) {
                mApp.otaUtils.cleanOtaScreen(false);
            }
        }

        // TODO(OTASP):
        // The original check from checkIsOtaCall() when handling ACTION_MAIN was this:
        //
        //        [ . . . ]
        //        else if (action.equals(intent.ACTION_MAIN)) {
        //            if (DBG) log("checkIsOtaCall action ACTION_MAIN");
        //            boolean isRingingCall = mCM.hasActiveRingingCall();
        //            if (isRingingCall) {
        //                if (DBG) log("checkIsOtaCall isRingingCall: " + isRingingCall);
        //                return false;
        //            } else if ((mApp.cdmaOtaInCallScreenUiState.state
        //                            == CdmaOtaInCallScreenUiState.State.NORMAL)
        //                    || (mApp.cdmaOtaInCallScreenUiState.state
        //                            == CdmaOtaInCallScreenUiState.State.ENDED)) {
        //                if (DBG) log("action ACTION_MAIN, OTA call already in progress");
        //                isOtaCall = true;
        //            } else {
        //                if (mApp.cdmaOtaScreenState.otaScreenState !=
        //                        CdmaOtaScreenState.OtaScreenState.OTA_STATUS_UNDEFINED) {
        //                    if (DBG) log("checkIsOtaCall action ACTION_MAIN, "
        //                                 + "OTA call in progress with UNDEFINED");
        //                    isOtaCall = true;
        //                }
        //            }
        //        }
        //
        // Also, in internalResolveIntent() we used to do this:
        //
        //        if ((mApp.inCallUiState.inCallScreenMode == InCallScreenMode.OTA_NORMAL)
        //                || (mApp.inCallUiState.inCallScreenMode == InCallScreenMode.OTA_ENDED)) {
        //            // If in OTA Call, update the OTA UI
        //            updateScreen();
        //            return;
        //        }
        //
        // We still need more cleanup to simplify the mApp.cdma*State objects.

        return otaspUiActive;
    }

    /**
     * Updates and returns the InCallControlState instance.
     */
    public InCallControlState getUpdatedInCallControlState() {
        if (VDBG) log("getUpdatedInCallControlState()...");
        mInCallControlState.update();
        return mInCallControlState;
    }

    public void resetInCallScreenMode() {
        if (DBG) log("resetInCallScreenMode: setting mode to UNDEFINED...");
        setInCallScreenMode(InCallScreenMode.UNDEFINED);
    }

    /**
     * Updates the onscreen hint displayed while the user is dragging one
     * of the handles of the RotarySelector widget used for incoming
     * calls.
     *
     * @param hintTextResId resource ID of the hint text to display,
     *        or 0 if no hint should be visible.
     * @param hintColorResId resource ID for the color of the hint text
     */
    /* package */ void updateIncomingCallWidgetHint(int hintTextResId, int hintColorResId) {
        if (VDBG) log("updateIncomingCallWidgetHint(" + hintTextResId + ")...");
        if (mCallCard != null) {
            mCallCard.setIncomingCallWidgetHint(hintTextResId, hintColorResId);
            mCallCard.updateState(mCM);
            // TODO: if hintTextResId == 0, consider NOT clearing the onscreen
            // hint right away, but instead post a delayed handler message to
            // keep it onscreen for an extra second or two.  (This might make
            // the hint more helpful if the user quickly taps one of the
            // handles without dragging at all...)
            // (Or, maybe this should happen completely within the RotarySelector
            // widget, since the widget itself probably wants to keep the colored
            // arrow visible for some extra time also...)
        }
    }


    /**
     * Used when we need to update buttons outside InCallTouchUi's updateInCallControls() along
     * with that method being called. CallCard may call this too because it doesn't have
     * enough information to update buttons inside itself (more specifically, the class cannot
     * obtain mInCallControllState without some side effect. See also
     * {@link #getUpdatedInCallControlState()}. We probably don't want a method like
     * getRawCallControlState() which returns raw intance with no side effect just for this
     * corner case scenario)
     *
     * TODO: need better design for buttons outside InCallTouchUi.
     */
    /* package */ void updateButtonStateOutsideInCallTouchUi() {
        if (mCallCard != null) {
            mCallCard.setSecondaryCallClickable(mInCallControlState.canSwap);
        }
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        super.dispatchPopulateAccessibilityEvent(event);
        mCallCard.dispatchPopulateAccessibilityEvent(event);
        return true;
    }

    /**
     * Manually handle configuration changes.
     *
     * Originally android:configChanges was set to "orientation|keyboardHidden|uiMode"
     * in order "to make sure the system doesn't destroy and re-create us due to the
     * above config changes". However it is currently set to "keyboardHidden" since
     * the system needs to handle rotation when inserted into a compatible cardock.
     * Even without explicitly handling orientation and uiMode, the app still runs
     * and does not drop the call when rotated.
     *
     */
    /*public void onConfigurationChanged(Configuration newConfig) {
        if (DBG) log("onConfigurationChanged: newConfig = " + newConfig);

        // Note: At the time this function is called, our Resources object
        // will have already been updated to return resource values matching
        // the new configuration.

        // Watch out: we *can* still get destroyed and recreated if a
        // configuration change occurs that is *not* listed in the
        // android:configChanges attribute.  TODO: Any others we need to list?

        super.onConfigurationChanged(newConfig);

        // Nothing else to do here, since (currently) the InCallScreen looks
        // exactly the same regardless of configuration.
        // (Specifically, we'll never be in landscape mode because we set
        // android:screenOrientation="portrait" in our manifest, and we don't
        // change our UI at all based on newConfig.keyboardHidden or
        // newConfig.uiMode.)

        // TODO: we do eventually want to handle at least some config changes, such as:
        boolean isKeyboardOpen = (newConfig.keyboardHidden == Configuration.KEYBOARDHIDDEN_NO);
        if (DBG) log("  - isKeyboardOpen = " + isKeyboardOpen);
        boolean isLandscape = (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE);
        if (DBG) log("  - isLandscape = " + isLandscape);
        if (DBG) log("  - uiMode = " + newConfig.uiMode);
        // See bug 2089513.
    }*/

    /**
     * The last foregrounding phone numbers.
     */
    private static List<Connection> prevPhonenums = new ArrayList<Connection>();

    /**
     * Compare the current foreground phone numbers with the last one.
     * @return false if different, otherwise true.
     */
    private boolean comparePhoneNumbers() {
        if (prevPhonenums == null || prevPhonenums.size() == 0) {
            return true;
        }

        List<Connection> fgCalls ;
        
        fgCalls = mCM.getActiveFgCall().getConnections();    

        if (prevPhonenums.size() != fgCalls.size()) {
            return false;
        }

        for (int i = 0; i < fgCalls.size(); i++) {
            if (! prevPhonenums.contains(fgCalls.get(i))) {
                return false;
            } else if (prevPhonenums.size() == 1 && !fgCalls.get(i).isAlive()) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * If the previous phone numbers is different from the current one.
     * Set it the current one. 
     */
    private void updatePrevPhonenums() {
        if (DBG) {
            log("-updatePrevPhonenums:update the previous phone number list.");
        }
     
        List<Connection> fgCalls;
        
        fgCalls = mCM.getActiveFgCall().getConnections();

        prevPhonenums.clear();
        for (int i = 0; i < fgCalls.size(); i++) {
            prevPhonenums.add(fgCalls.get(i));
        }
    }
    
    /**
     * Change the recording status according to the phone status changing.
     */
    private void handleRecordProc() {
        PhoneRecorder phoneRecorder = PhoneRecorder.getInstance(this.getApplicationContext());
        if (prevPhonenums == null || prevPhonenums.size() == 0) {
            log("the record custom value is " + PhoneRecorderHandler.getInstance().getCustomValue());
            if (Constants.PHONE_RECORDING_VOICE_CALL_CUSTOM_VALUE == 
                    PhoneRecorderHandler.getInstance().getCustomValue()) {
                if (PhoneRecorder.isRecording()) {
                    stopRecord();
                }
            } else if (Constants.PHONE_RECORDING_VIDEO_CALL_CUSTOM_VALUE == 
                           PhoneRecorderHandler.getInstance().getCustomValue()) {
                mVTInCallScreen.stopRecord();
            }
            updatePrevPhonenums();
            return;
        }
        boolean recordFlag = false;
        recordFlag = phoneRecorder.ismFlagRecord();
        boolean isDifferent = false;
        if (recordFlag) {
            isDifferent = !comparePhoneNumbers();
            if (isDifferent) {
                if (Constants.PHONE_RECORDING_VOICE_CALL_CUSTOM_VALUE == 
                        PhoneRecorderHandler.getInstance().getCustomValue()) {
                    if (PhoneRecorder.isRecording()) {
                        stopRecord();
                    }
                } else if (Constants.PHONE_RECORDING_VIDEO_CALL_CUSTOM_VALUE == 
                               PhoneRecorderHandler.getInstance().getCustomValue()) {
                    mVTInCallScreen.stopRecord();
                }
            }
        }
        updatePrevPhonenums();
        // This code is added for CALLWAITING state.
        // When in CALLWAITING state, a new call is incoming, even if recording now,
        // need hide recording flash icon, so here needs update
        requestUpdateRecordState(PhoneRecorderHandler.getInstance().getPhoneRecorderState(),
                                 PhoneRecorderHandler.getInstance().getCustomValue());
    }

    boolean getSwappingCalls() {
        return this.mSwappingCalls;
    }

    void setSwappingCalls(boolean b) {
        this.mSwappingCalls = b;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (ViewConfiguration.get(this).hasPermanentMenuKey()) {
            MenuInflater inflate = new MenuInflater(this);
            inflate.inflate(R.menu.incall_menu, menu);
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mVTInCallScreen.getVTScreenMode() == Constants.VTScreenMode.VT_SCREEN_OPEN 
                && mVTInCallScreen.onPrepareOptionsMenu(menu)) {
            return false;
        }
        if (ViewConfiguration.get(this).hasPermanentMenuKey()) {
            for (int i = 0; i < menu.size(); ++i) {
                menu.getItem(i).setVisible(false);
            }
            if (mVTInCallScreen.getVTScreenMode() != Constants.VTScreenMode.VT_SCREEN_OPEN) {
                setupMenuItems(menu);
                mExtension.setupMenuItems(menu, getUpdatedInCallControlState());
            } else {
                mVTInCallScreen.setupMenuItems(menu);
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mVTInCallScreen.getVTScreenMode() != Constants.VTScreenMode.VT_SCREEN_OPEN) {
            if (mExtension.handleOnScreenMenuItemClick(item)) {
                return true;
            } else {
                return handleOnScreenMenuItemClick(item);
            }
        } else {
            return mVTInCallScreen.handleOnScreenMenuItemClick(item);
        }
    }

    //This is ugly, but we have no choice because of SipPhone doesn't implement the method
    void internalHangupAllCalls(CallManager cm) {
        log("internalHangupAllCalls");
        List<Phone> phones = cm.getAllPhones();
        try {
            for (Phone phone : phones) {
                Call fg = phone.getForegroundCall();
                Call bg = phone.getBackgroundCall();
                if (phone.getState() != PhoneConstants.State.IDLE) {
                    if (!(phone instanceof SipPhone)) {
                        Log.d(LOG_TAG, phone.toString() + "   " + phone.getClass().toString());
                        if ((fg != null && fg.getState().isAlive()) && (bg != null && bg.getState().isAlive())) {
                            phone.hangupAll();
                        } else if (fg != null && fg.getState().isAlive()) {
                            fg.hangup();
                        } else if (bg != null && bg.getState().isAlive()) {
                            bg.hangup();
                        }
                    } else {
                        Log.d(LOG_TAG, phone.toString() + "   " + phone.getClass().toString());
                        if (fg != null && fg.getState().isAlive()) {
                            fg.hangup();
                        }
                        if (bg != null && bg.getState().isAlive()) {
                            bg.hangup();
                        }
                    }
                } else {
                    Log.d(LOG_TAG, "Phone is idle  " + phone.toString() + "   " + phone.getClass().toString());
                }
            }
        } catch (Exception e) {
            Log.d(LOG_TAG, e.toString());
        }
    }

    /**
     * Handles an incoming RING event from the telephony layer.
     */
    private void onIncomingRing() {
        if (DBG) log("onIncomingRing()...");
        // IFF we're visible, forward this event to the InCallTouchUi
        // instance (which uses this event to drive the animation of the
        // incoming-call UI.)
        if (mIsForegroundActivity && (mInCallTouchUi != null)) {
            mInCallTouchUi.onIncomingRing();
        }
    }

    /**
     * Handles a "new ringing connection" event from the telephony layer.
     *
     * This event comes in right at the start of the incoming-call sequence,
     * exactly once per incoming call.
     *
     * Watch out: this won't be called if InCallScreen isn't ready yet,
     * which typically happens for the first incoming phone call (even before
     * the possible first outgoing call).
     */
    private void onNewRingingConnection() {
        if (DBG) log("onNewRingingConnection()...");

        // We use this event to reset any incoming-call-related UI elements
        // that might have been left in an inconsistent state after a prior
        // incoming call.
        // (Note we do this whether or not we're the foreground activity,
        // since this event comes in *before* we actually get launched to
        // display the incoming-call UI.)

        // If there's a "Respond via SMS" popup still around since the
        // last time we were the foreground activity, make sure it's not
        // still active(!) since that would interfere with *this* incoming
        // call.
        // (Note that we also do this same check in onResume().  But we
        // need it here too, to make sure the popup gets reset in the case
        // where a call-waiting call comes in while the InCallScreen is
        // already in the foreground.)
        mRespondViaSmsManager.dismissPopup();  // safe even if already dismissed
    }

    public void onTickForCallTimeElapsed(long timeElapsed) {
        // While a call is in progress, update the elapsed time shown
        // onscreen.
        if (!VTCallUtils.isVideoCall(mCM.getActiveFgCall())) {
            mCallCard.updateElapsedTimeWidget(timeElapsed);
        } else {
            mVTInCallScreen.updateElapsedTime(timeElapsed);
        }
    }

    public void updateCallTime() {
        if (DBG) {
            log("updateCallTime()...");
        }

        Call fgCall = mCM.getActiveFgCall();
        if (mCM.hasActiveRingingCall()) {
            fgCall = mCM.getFirstActiveRingingCall();
        }

        final Call.State state = fgCall.getState();
        switch (state) {

            case ACTIVE:
                // for VT, trigger timer start count does not use ACTIVE state,
                // but use VTManager.VT_MSG_START_COUNTER message to trigger
                if (VTCallUtils.isVideoCall(fgCall) 
                        && VTInCallScreenFlags.getInstance().mVTConnectionStarttime.mStarttime < 0) {
                    if (null != fgCall.getLatestConnection() && fgCall.getLatestConnection().isIncoming()) {
                        mVTInCallScreen.onReceiveVTManagerStartCounter();
                    }
                    break;
                }
            case DISCONNECTING:
                // update timer field
                if (DBG) {
                    log("updateCallTime: start periodicUpdateTimer");
                }
                mCallTime.setActiveCallMode(fgCall);
                mCallTime.reset();
                mCallTime.periodicUpdateTimer();
                break;

            case IDLE:
            case HOLDING:
            case DIALING:
            case ALERTING:
            case INCOMING:
            case WAITING:
            case DISCONNECTED:
                // Stop getting timer ticks from a previous call
                mCallTime.cancelTimer();
                break;

            default:
                Log.wtf(LOG_TAG, "updateCallTime: unexpected call state: " + state);
                break;
        }
    }

    private void log(String msg) {
        Log.d(LOG_TAG, msg);
    }

    private PopupMenu constructPopupMenu(View anchorView) {
        final PopupMenu popupMenu = new PopupMenu(this, anchorView);
        final Menu menu = popupMenu.getMenu();
        popupMenu.inflate(R.menu.voice_incall_menu);
        popupMenu.setOnMenuItemClickListener(this);
        setupMenuItems(menu);
        return popupMenu;
    }

    private void setupMenuItems(Menu menu) {
        final Call ringingCall = mCM.getFirstActiveRingingCall();
        final Call bgCall = mCM.getFirstActiveBgCall();
        final InCallControlState inCallControlState = getUpdatedInCallControlState();

        //final MenuItem muteRingerMenu = menu.findItem(R.id.menu_mute_ringer);
        final MenuItem addMenu = menu.findItem(R.id.menu_add_call);
        final MenuItem mergeMenu = menu.findItem(R.id.menu_merge_call);
        final MenuItem recordMenu = menu.findItem(R.id.menu_voice_record);
        final MenuItem voiceAnswerMenu = menu.findItem(R.id.menu_vt_voice_answer);

        final MenuItem hangupAllMenu = menu.findItem(R.id.menu_hangup_all);
        final MenuItem hangupHoldingMenu = menu.findItem(R.id.menu_hangup_holding);
        final MenuItem hangupActiveAndAnswerWaitingMenu = menu.findItem(R.id.menu_hangup_active_and_answer_waiting);
        final MenuItem ectMenu = menu.findItem(R.id.menu_ect);
        
        final MenuItem holdMenu = menu.findItem(R.id.menu_hold_voice);
        
        if (DualTalkUtils.isSupportDualTalk()) {
            if (mDualTalk.isSupportHoldAndUnhold()) {
                holdMenu.setVisible(true);
                if (mDualTalk.getActiveFgCall().getState() == Call.State.ACTIVE) {
                    holdMenu.setTitle(R.string.onscreenHoldText);
                } else if (mDualTalk.getFirstActiveBgCall().getState() == Call.State.HOLDING) {
                    holdMenu.setTitle(R.string.incall_toast_unhold);
                } else {
                    //
                    holdMenu.setVisible(false);
                    log("some thing is wrong!!");
                }
            } else {
                holdMenu.setVisible(false);
            }
        } else {
            holdMenu.setVisible(false);
        }

        final int phoneType = mCM.getActiveFgCall().getPhone().getPhoneType();

        //muteRingerMenu.setVisible(false);
        if (addMenu != null) {
            addMenu.setVisible(false);
        }
        if (mergeMenu != null) {
            mergeMenu.setVisible(false);
        }
        recordMenu.setVisible(false);
        voiceAnswerMenu.setVisible(false);

        hangupAllMenu.setVisible(false);
        hangupHoldingMenu.setVisible(false);
        hangupActiveAndAnswerWaitingMenu.setVisible(false);
        ectMenu.setVisible(false);

        if (PhoneUtils.isDMLocked()) {
            return;
        }

        mInCallMenuState.update();

        // Show menu items when there is no ringing call or
        // ringing call is disconected and there is an active bgCall.
        if (ringingCall.getState() == Call.State.IDLE ||
                (ringingCall.getState() == Call.State.DISCONNECTED &&
                        bgCall != null && bgCall.getState() == Call.State.IDLE)) {
            if (!ViewConfiguration.get(this).hasPermanentMenuKey()) {
                if (inCallControlState.canAddCall) {
                    if (addMenu != null) {
                        addMenu.setVisible(true);
                    }
                } else if (inCallControlState.canMerge) {
                    if ((phoneType == PhoneConstants.PHONE_TYPE_GSM)
                            || (phoneType == PhoneConstants.PHONE_TYPE_SIP)) {
                        if (mergeMenu != null) {
                            mergeMenu.setVisible(true);
                        }
                    } else {
                        throw new IllegalStateException("Unexpected phone type: " + phoneType);
                    }
                } else {
                    // Neither "Add" nor "Merge" is available.  (This happens in
                    // some transient states, like while dialing an outgoing call,
                    // and in other rare cases like if you have both lines in use
                    // *and* there are already 5 people on the conference call.)
                    // Since the common case here is "while dialing", we show the
                    // "Add" button in a disabled state so that there won't be any
                    // jarring change in the UI when the call finally connects.
                    if (addMenu != null) {
                        addMenu.setVisible(true);
                        addMenu.setEnabled(false);
                    }
                }
            }

            if (okToRecordVoice()) {
                PhoneRecorder phoneRecorder = PhoneRecorder.getInstance(getApplicationContext());
                recordMenu.setVisible(true);
                //We think the visible equal enable.
                recordMenu.setEnabled(true);
                if (!phoneRecorder.ismFlagRecord()) {
                    recordMenu.setTitle(R.string.start_record);
                } else {
                    recordMenu.setTitle(R.string.stop_record);
                }
            }

            hangupAllMenu.setVisible(mInCallMenuState.canHangupAll);
            hangupHoldingMenu.setVisible(mInCallMenuState.canHangupHolding);
        } else {
            // This is for VT voice answer feature
            voiceAnswerMenu.setVisible(mInCallMenuState.canVTVoiceAnswer);
        }

        //muteRingerMenu.setVisible(mInCallMenuState.canMuteRinger);
        hangupActiveAndAnswerWaitingMenu.setVisible(mInCallMenuState.canHangupActiveAndAnswerWaiting);
        ectMenu.setVisible(mInCallMenuState.canECT);
    }

    boolean handleOnScreenMenuItemClick(MenuItem menuItem) {
        switch(menuItem.getItemId()) {
            /*case R.id.menu_mute_ringer:
                muteIncomingCall(true);
                return true;*/
            case R.id.menu_hold_voice:
                //handleHoldAndUnhold();
                onHoldClick();
                break;
            case R.id.menu_add_call:
                onAddCallClick();
                updateInCallTouchUi();
                return true;
            case R.id.menu_merge_call:
                mSwappingCalls = false;
                PhoneUtils.mergeCalls(mCM);
                return true;
            case R.id.menu_voice_record:
                onVoiceRecordClick(menuItem);
                return true;
            case R.id.menu_vt_voice_answer:
                onVTVoiceAnswer();
                return true;
            case R.id.menu_hangup_all:
                PhoneUtils.hangupAllCalls();
                return true;
            case R.id.menu_hangup_holding:
                PhoneUtils.hangupHoldingCall(mCM.getFirstActiveBgCall());
                return true;
            case R.id.menu_hangup_active_and_answer_waiting:
                PhoneUtils.hangup(mCM.getActiveFgCall());
                return true;
            case R.id.menu_ect:
                try {
                    /// M:Gemini+
                    if (GeminiUtils.isGeminiSupport()) {
                        int slotId = GeminiUtils.getSlotNotIdle(mPhone);
                        if (slotId!=-1) {
                            ((GeminiPhone) mPhone).explicitCallTransferGemini(slotId);
                        }
                    } else {
                        mPhone.explicitCallTransfer();
                    }
                } catch (CallStateException e) {
                    e.printStackTrace();
                }
                return true;
            default:
                break;
        }
        return false;
    }

    public boolean onMenuItemClick(MenuItem arg0) {
        return handleOnScreenMenuItemClick(arg0);
    }

    private boolean okToRecordVoice() {
        boolean retval = false;
        if (!FeatureOption.MTK_PHONE_VOICE_RECORDING) {
            //For dualtalk solution, because of audio's limitation, don't support voice record
            return retval;
        }

        final Call ringingCall = mCM.getFirstActiveRingingCall();

        if (ringingCall.getState() == Call.State.IDLE) {
            log("fgCall state:" + mCM.getActiveFgCall().getState());
            log("phoneType" + mCM.getFgPhone().getPhoneType());
            Call fgCall = mCM.getActiveFgCall();
            if (fgCall.getState() == Call.State.ACTIVE && mCM.getFgPhone().getPhoneType() != PhoneConstants.PHONE_TYPE_SIP) {
                retval = true;
            }
        }

        return retval;
    }

    /*package*/ void onVoiceRecordClick(MenuItem menuItem) {
        log("onVoiceRecordClick");
        String title = menuItem.getTitle().toString();
        if (title == null || !okToRecordVoice()) {
            return;
        }
        if (!PhoneUtils.isExternalStorageMounted()) {
            Toast.makeText(this, getResources().getString(R.string.error_sdcard_access), Toast.LENGTH_LONG).show();
            return;
        }
        if (!PhoneUtils.diskSpaceAvailable(Constants.PHONE_RECORD_LOW_STORAGE_THRESHOLD)) {
            handleStorageFull(true); // true for checking case
            return;
        }


        PhoneRecorder phoneRecorder = PhoneRecorder.getInstance(getApplicationContext());
        if (title.equals(getString(R.string.start_record))) {
            log("want to startRecord");
            if (!phoneRecorder.ismFlagRecord()) {
                log("startRecord");
                //menuItem.setTitle(R.string.stop_record);
                startRecord();
            }
        } else if (title.equals(getString(R.string.stop_record))) {
            log("want to stopRecord");
            if (phoneRecorder.ismFlagRecord()) {
                log("stopRecord");
                //menuItem.setTitle(R.string.start_record);
                stopRecord();
            }
        }
    }

    void enableHomeKeyDispatched(boolean enable) {
        if (DBG) {
            log("enableHomeKeyDispatched, enable = " + enable);
        }
        final Window window = getWindow();
        final WindowManager.LayoutParams lp = window.getAttributes();
        if (enable) {
            lp.flags |= WindowManager.LayoutParams.FLAG_HOMEKEY_DISPATCHED;
        } else {
            lp.flags &= ~WindowManager.LayoutParams.FLAG_HOMEKEY_DISPATCHED;
        }
        window.setAttributes(lp);
    }

    void finishForTest() {
        super.finish();
    }
    
    public void setInVoiceAnswerVideoCall(boolean value) {
        if (DBG) {
            log("setInVoiceAnswerVideoCall() : " + value);
        }
        if (value) {
            VTInCallScreenFlags.getInstance().mVTVoiceAnswer = true;
            Connection c = mRingingCall.getLatestConnection();
            VTInCallScreenFlags.getInstance().mVTVoiceAnswerPhoneNumber = c.getAddress();
            mInVoiceAnswerVideoCall = true;
            mVTVoiceAnswerDialog = new ProgressDialog(this);
            mVTVoiceAnswerDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            mVTVoiceAnswerDialog.setMessage(getResources().getString(R.string.vt_wait_voice));
            mVTVoiceAnswerDialog.setOnCancelListener(new OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    delayedCleanupAfterDisconnect();
                }
            });

            mVTVoiceAnswerDialog.show();

            mVTVoiceAnswerTimer = new Timer();
            mVTVoiceAnswerTimer.schedule(new TimerTask() {
                public void run() {
                    mHandler.sendMessage(Message.obtain(mHandler, VT_VOICE_ANSWER_OVER));
                }
            }, 15 * 1000);
        } else {
            mInVoiceAnswerVideoCall = false;
            if (mVTVoiceAnswerDialog != null) {
                mVTVoiceAnswerDialog.dismiss();
                mVTVoiceAnswerDialog = null;
            }
            if (mVTVoiceAnswerTimer != null) {
                mVTVoiceAnswerTimer.cancel();
                mVTVoiceAnswerTimer = null;
            }
        }
    }

    public boolean getInVoiceAnswerVideoCall() {
        return mInVoiceAnswerVideoCall;
    }
    
    private void onVTVoiceAnswer() {
        if (DBG) {
            log("onVTVoiceAnswer() ! ");
        }
        setInVoiceAnswerVideoCall(true);

        try {
            if (DBG) {
                log("onVTVoiceAnswer() : call CallManager.voiceAccept() start ");
            }
            mCM.voiceAccept(mRingingCall);
            if (DBG) {
                log("onVTVoiceAnswer() : call CallManager.voiceAccept() end ");
            }
        } catch (CallStateException e) {
            e.printStackTrace();
        }
    }

    public IVTInCallScreen getVTInCallScreenInstance() {
        return mVTInCallScreen;
    }
    
    /*public void answerVTCall() {
        if (FeatureOption.MTK_VT3G324M_SUPPORT) {
            if (VTCallUtils.isVTRinging()) {
                mVTInCallScreen.internalAnswerVTCallPre();
            }
        }
        internalAnswerCall();
    }*/
    
    public void triggerTimerStartCount(Call call) {
        mCallTime.setActiveCallMode(call);
        mCallTime.reset();
        mCallTime.periodicUpdateTimer();
    }

    /* package */ boolean ignoreUpdateScreen() {
        final boolean hasActiveFgCall = mCM.hasActiveFgCall();
        final boolean hasActiveBgCall = mCM.hasActiveBgCall();
        return (hasActiveFgCall || hasActiveBgCall) && mApp.notifier.hasPendingCallerInfoQuery();
    }
    
    private void initVTAutoAnswer() {
        if (FeatureOption.MTK_VT3G324M_SUPPORT) {
            if (VTSettingUtils.getInstance().mVTEngineerModeValues.auto_answer) {
                int autoAnswer;
                try {
                    autoAnswer = new Integer(
                            VTSettingUtils.getInstance().mVTEngineerModeValues.auto_answer_time);
                } catch (Exception e) {
                    autoAnswer = 0;
                }
                mHandler.sendEmptyMessageDelayed(VT_EM_AUTO_ANSWER, autoAnswer);
            }
        }
    }

    public void onShow(DialogInterface dialog) {
        if (DBG) {
            log("clear mShowStatusIndication remove DELAY_TO_FINISH_INCALLSCREEN");
        }
        mHandler.removeMessages(DELAY_TO_FINISH_INCALLSCREEN);
        mShowStatusIndication = false;
    }
    
    void adjustProcessPriority() {
        final int myId = Process.myPid();
        if (Process.getThreadPriority(myId) != Process.THREAD_PRIORITY_DEFAULT) {
            Process.setThreadPriority(myId, Process.THREAD_PRIORITY_DEFAULT);
        }
    }

    /**
     * Change Feature by mediatek .inc
     * description : support for dualtalk
     */
    void internalAnswerCallForDualTalk() {
        Call ringing = mDualTalk.getFirstActiveRingingCall();
        //In order to make the answer process simply, firtly, check there is outgoingcall, if exist, disconnect it;
        
        if (mDualTalk.isRingingWhenOutgoing()) {
            if (DBG) {
                log("internalAnswerCallForDualTalk: " + "ringing when dialing");
            }
            Call call = mDualTalk.getSecondActivieFgCall();
            if (call.getState().isDialing()) {
                try {
                    Phone phone = call.getPhone();
                    if (phone instanceof SipPhone) {
                        call.hangup();
                    } else {
                        if (FeatureOption.MTK_VT3G324M_SUPPORT) {
                            if (VTCallUtils.isVideoCall(call) && call.getState().isAlive()) {
                                mApp.notifier.resetAudioState();
                            }
                        }
                        phone.hangupActiveCall();
                    }
                } catch (Exception e) {
                    log("internalAnswerCallForDualTalk:Exception ");
                }
            }
        }
        
        List<Call> list = mDualTalk.getAllNoIdleCalls();
        int callCount = list.size();
        
        try {
            if (callCount > 2) {
                if (DBG) {
                    log("internalAnswerCallForDualTalk: " + "has more than two calls exist.");
                }
                //This offen occurs in W+G platform.
                //On C+G platform, the only case is: CDMA has an active(real be hold in network) call,
                //and the GSM has active + hold call, then GSM has a ringing call
                if (mDualTalk.hasActiveCdmaPhone()) {
                    //In this case, the ringing call must be exist in the same phone with active call
                    handleAnswerAndEnd(ringing.getPhone().getForegroundCall());
                    if (DBG) {
                        log("internalAnswerCallForDualTalk (C+G): hangup the gsm active call!");
                    }
                } else {
                    handleAnswerAndEnd(mCM.getActiveFgCall());
                }
                return ;
            } else if (callCount == 2) {
                if (DBG) {
                    log("internalAnswerCallForDualTalk: " + "has two calls exist.");
                }
                if (list.get(0).getPhone() == list.get(1).getPhone()) {
                    if (DBG) {
                        log("internalAnswerCallForDualTalk: " + "two calls exist in the same phone.");
                    }
                    handleAnswerAndEnd(mCM.getActiveFgCall());
                    return ;
                } else {
                    if (DBG) {
                        log("internalAnswerCallForDualTalk: " + "two calls exist in diffrent phone.");
                    }
                    if (mDualTalk.hasActiveOrHoldBothCdmaAndGsm()) {
                        //because gsm has the exact status, so we deduce the cdma call status and then
                        //decide if hold operation is needed by cdma call.
                        Phone gsmPhone = mDualTalk.getActiveGsmPhone();
                        Phone cdmaPhone = mDualTalk.getActiveCdmaPhone();
                        
                        Call cCall = cdmaPhone.getForegroundCall();
                        if (PhoneUtils.hasMultipleConnections(cCall)) {
                            log("internalAnswerCallForDualTalk: cdma has multiple connections, disconneted it!");
                            cCall.hangup();
                            PhoneUtils.answerCall(ringing);
                            return ;
                        }
                        if (gsmPhone.getForegroundCall().getState().isAlive()) {
                            //cdma call is hold, and the ringing call must be gsm call
                            ringing.getPhone().acceptCall();
                            if (DBG) {
                                log("internalAnswerCallForDualTalk: " + "cdma hold + gsm active + gsm ringing");
                            }
                        } else {
                            //gsm has hold call
                            if (DBG) {
                                log("internalAnswerCallForDualTalk: " + "cdma active + gsm holding + cdma ringing/gsm ringing");
                            }
                            PhoneUtils.answerCall(ringing);
                        }
                    } else {
                        //This is for W+G handler
                        for (Call call : list) {
                            Call.State state = call.getState();
                            if (state == Call.State.ACTIVE) {
                                if (ringing.getPhone() != call.getPhone()) {
                                    call.getPhone().switchHoldingAndActive();
                                }
                                PhoneUtils.answerCall(ringing);
                                break;
                            } else if (state == Call.State.HOLDING) {
                                //this maybe confuse, need further check: this happend when the dialing is disconnected
                                PhoneUtils.answerCall(ringing);
                            }
                        }
                    }
                }
            } else if (callCount == 1) {
                if (DBG) {
                    log("internalAnswerCallForDualTalk: " + "there is one call exist.");
                }
                Call call = list.get(0);
                //First check if the only ACTIVE call is CDMA (three-way or call-waitting) call
                if (call.getPhone().getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA
                        && PhoneUtils.hasMultipleConnections(call)) {
                    //The ring call must be GSM call
                    log("internalAnswerCallForDualTalk: hangup the cdma multiple call and answer the gsm call!");
                    call.hangup();
                    PhoneUtils.answerCall(ringing);
                    
                } else if (call.getPhone() == ringing.getPhone()) {
                    PhoneUtils.answerCall(ringing);
                } else if (call.getState() == Call.State.ACTIVE) {
                    if (VTCallUtils.isVideoCall(call) || VTCallUtils.isVideoCall(ringing)) {
                        if (DBG) {
                            log("internalAnswerCallForDualTalk: " + "there is one video call, hangup current call!");
                        }
                        Phone phone = call.getPhone();
                        if (phone instanceof SipPhone) {
                            call.hangup();
                        } else {
                            phone.hangupActiveCall();
                        }
                    } /*else {
                        //not needed, CallManager will switch ACTIVE & HOLDING when answer the call
                        mCM.switchHoldingAndActive(call);
                    }*/
                    PhoneUtils.answerCall(ringing);
                } else {
                    PhoneUtils.answerCall(ringing);
                }
            } else if (callCount == 0) {
                if (DBG) {
                    log("internalAnswerCallForDualTalk: " + "there is no call exist.");
                }
                handleVideoAndVoiceIncoming(mDualTalk);
                PhoneUtils.answerCall(ringing);
            }
        } catch (Exception e) {
            log(e.toString());
        }
    }
    
    
    void handleAnswerAndEnd(Call call) {
        log("+handleAnswerAndEnd");
        List<Call> list = mDualTalk.getAllNoIdleCalls();
        int size = list.size();
        try {
            if (call.getState().isAlive()) {
                Phone phone = call.getPhone();
                //call.hangup();
                
                if (call.getState() == Call.State.ACTIVE) {
                    log("+handleAnswerAndEnd: " + "hangup Call.State.ACTIVE");
                    if (phone instanceof SipPhone) {
                        call.hangup();
                    } else {
                        phone.hangupActiveCall();
                    }
                } else if (call.getState() == Call.State.HOLDING) {
                    log("+handleAnswerAndEnd: " + "hangup Call.State.HOLDING and switch H&A");
                    call.hangup();
                    phone.switchHoldingAndActive();
                }
            }
        } catch (Exception e) {
            log(e.toString());
        }
        
        Call ringCall = mDualTalk.getFirstActiveRingingCall();
        if (mDualTalk.hasActiveCdmaPhone() && (ringCall.getPhone().getPhoneType() != PhoneConstants.PHONE_TYPE_CDMA)) {
            if (DBG) {
                log("handleAnswerAndEnd: cdma phone has acttive call, don't switch it and answer the ringing only");
            }
            try {
                ringCall.getPhone().acceptCall();
            } catch (Exception e) {
                log(e.toString());
            }
        } else {
            PhoneUtils.answerCall(mDualTalk.getFirstActiveRingingCall());
        }
        
        log("-handleAnswerAndEnd");
    }
    
    void handleUnholdAndEnd(Call call) {
        log("+handleUnholdAndEnd");
        List<Call> list = mDualTalk.getAllNoIdleCalls();
        int size = list.size();
        try {
            if (call.getState().isAlive()) {
                Phone phone = call.getPhone();
                //call.hangup();
                
                if (call.getState() == Call.State.ACTIVE) {
                    log("+handleUnholdAndEnd: " + "hangup Call.State.ACTIVE");
                    if (phone instanceof SipPhone) {
                        call.hangup();
                    } else {
                        phone.hangupActiveCall();
                    }
                } else if (call.getState() == Call.State.HOLDING) {
                    log("+handleUnholdAndEnd: " + "hangup Call.State.HOLDING and switch H&A");
                    call.hangup();
                    phone.switchHoldingAndActive();
                }
            }
            
            mDualTalk.getSecondActiveBgCall().getPhone().switchHoldingAndActive();
            
        } catch (Exception e) {
            log(e.toString());
        }
        
        log("-handleUnholdAndEnd");
    }
    
    void handleHoldAndUnhold() {
        if (!DualTalkUtils.isSupportDualTalk()) {
            return ;
        }
        Call fgCall = mDualTalk.getActiveFgCall();
        Call bgCall = mDualTalk.getFirstActiveBgCall();
        try {
            if (fgCall.getState().isAlive()) {
                fgCall.getPhone().switchHoldingAndActive();
            } else if (bgCall.getState().isAlive()) {
                bgCall.getPhone().switchHoldingAndActive();
            }
        } catch (Exception e) {
            log("handleHoldAndUnhold: " + e.toString());
        }
    }
    
    void requestUpdateScreenDelay(long ms) {
        if (DBG) {
            log("requestUpdateScreenDelay()...");
        }
        mHandler.removeMessages(REQUEST_UPDATE_SCREEN);
        mHandler.removeMessages(REQUEST_UPDATE_SCREEN_EXT);
        /**
         * add by mediatek .inc
         * description : a little tricky here, do not
         * update screen when : 1A1W and the waiting call query
         * is not completed ( ALPS000231023 )
         */
        if (ignoreUpdateScreen()) {
            if (DBG) {
                log("ignoreUpdateScreen");
            }
            return;
        }
        /*
         * change by mediatek .inc end
         */
        mHandler.sendMessageDelayed(mHandler.obtainMessage(REQUEST_UPDATE_SCREEN_EXT), ms);
    }

    void handleVideoAndVoiceIncoming(DualTalkUtils dt) {
        if (dt == null || !DualTalkUtils.isSupportDualTalk()) {
            return ;
        }
        
        if (!dt.hasMultipleRingingCall()) {
            return ;
        }
        
        Call firstRinging = dt.getFirstActiveRingingCall();
        Call secondRinging = dt.getSecondActiveRingCall();
        
        //there is no video call exist, return
        if (!VTCallUtils.isVideoCall(firstRinging) && !VTCallUtils.isVideoCall(secondRinging)) {
            return ;
        }
        
        if (secondRinging.getState().isAlive()) {
            try {
                secondRinging.hangup();
            } catch (Exception e) {
                log(e.toString());
            }
        }
    }

    /**
     *  we can go here, means both CDMA and GSM are active:
     *  1.cdma has one call, gsm has one call: switch between gsm and cdma phone
     *  2.cdma has one call, gsm has two call: switch between gsm's active and hold call
     *  3.cdma has two call, gsm has one call: switch gsm, and don't switch cdma phone, but switch the audio path
     */
    private void handleSwapCdmaAndGsm() {
       
        Call fgCall = mDualTalk.getActiveFgCall();
        Call bgCall = mDualTalk.getFirstActiveBgCall();
        
        int fgCallPhoneType = fgCall.getPhone().getPhoneType();
        int bgCallPhoneType = bgCall.getPhone().getPhoneType();
        
        if (DBG) {
            log("handleSwapCdmaAndGsm fgCall = " + fgCall.getConnections());
            log("handleSwapCdmaAndGsm bgCall = " + bgCall.getConnections());
        }
        
        //cdma has one call, gsm has two call: switch between gsm's active and hold call
        if (fgCallPhoneType == PhoneConstants.PHONE_TYPE_GSM
                && bgCallPhoneType == PhoneConstants.PHONE_TYPE_GSM) {
            log("handleSwapCdmaAndGsm: switch between two GSM calls.");
            try {
                fgCall.getPhone().switchHoldingAndActive();
            } catch (Exception e) {
                log(e.toString());
            }
            //Call CallManager's special api
        } else if (fgCallPhoneType == PhoneConstants.PHONE_TYPE_CDMA) {
            if (PhoneUtils.hasMultipleConnections(fgCall)) {
                log("handleSwapCdmaAndGsm: cdma has multiple calls and in foreground, only switch the audio.");
                //off cdma audio
                try {
                    bgCall.getPhone().switchHoldingAndActive();
                } catch (Exception e) {
                    log(e.toString());
                }
            } else {
                log("handleSwapCdmaAndGsm: cdma has single call and in foreground, switch by phone");
                try {
                    fgCall.getPhone().switchHoldingAndActive();
                    bgCall.getPhone().switchHoldingAndActive();
                } catch (Exception e) {
                    log(e.toString());
                }
            }
            
        } else if (fgCallPhoneType == PhoneConstants.PHONE_TYPE_GSM) {
            if (PhoneUtils.hasMultipleConnections(bgCall)) {
                log("handleSwapCdmaAndGsm: cdma has multiple calls and in background, only switch the audio");
                //on cdma audio
                try {
                    fgCall.getPhone().switchHoldingAndActive();
                } catch (Exception e) {
                    log(e.toString());
                }
            } else {
                log("handleSwapCdmaAndGsm: cdma has single call and in background, switch by phone");
                try {
                    fgCall.getPhone().switchHoldingAndActive();
                    bgCall.getPhone().switchHoldingAndActive();
                } catch (Exception e) {
                    log(e.toString());
                }
            }
        }
    }
    
    
    private boolean handleCallKeyForDualTalk() {
        if (mCM.getState() == PhoneConstants.State.RINGING) {
            //we assume that the callkey shouldn't be here when there is ringing call
            if (DBG) {
                log("handleCallKeyForDualTalk: rev call-key when ringing!");
            }
            return false;
        }
        
        return false;
    }

    // true for checking, false for recording
    public void handleStorageFull(final boolean isForCheckingOrRecording) {
        if (PhoneUtils.getMountedStorageCount() > 1) {
            // SD card case
            log("handleStorageFull(), mounted storage count > 1");
            if (Constants.STORAGE_TYPE_SD_CARD == PhoneUtils.getDefaultStorageType()) {
                log("handleStorageFull(), SD card is using");
                showStorageFullDialog(com.mediatek.internal.R.string.storage_sd, true);
            } else if (Constants.STORAGE_TYPE_PHONE_STORAGE == PhoneUtils.getDefaultStorageType()) {
                log("handleStorageFull(), phone storage is using");
                showStorageFullDialog(com.mediatek.internal.R.string.storage_withsd, true);
            } else {
                // never happen here
                log("handleStorageFull(), never happen here");
            }
        } else if (1 == PhoneUtils.getMountedStorageCount()) {
            log("handleStorageFull(), mounted storage count == 1");
            if (Constants.STORAGE_TYPE_SD_CARD == PhoneUtils.getDefaultStorageType()) {
                log("handleStorageFull(), SD card is using, " + (isForCheckingOrRecording ? "checking case" : "recording case"));
                String toast = isForCheckingOrRecording ? getResources().getString(R.string.vt_sd_not_enough) :
                                                          getResources().getString(R.string.vt_recording_saved_sd_full);
                Toast.makeText(PhoneGlobals.getInstance(), toast, Toast.LENGTH_LONG).show();
            } else if (Constants.STORAGE_TYPE_PHONE_STORAGE == PhoneUtils.getDefaultStorageType()) {
                // only Phone storage case
                log("handleStorageFull(), phone storage is using");
                showStorageFullDialog(com.mediatek.internal.R.string.storage_withoutsd, false);
            } else {
                // never happen here
                log("handleStorageFull(), never happen here");
            }
        }
    }

    public void showStorageFullDialog(final int resid, final boolean isSDCardExist) {
        if (DBG) {
            log("showStorageDialog... ");
        }

        if (null != mStorageSpaceDialog) {
            if (mStorageSpaceDialog.isShowing()) {
                return;
            }
        }
        CharSequence msg = getResources().getText(resid);

        // create the clicklistener and cancel listener as needed.
        DialogInterface.OnClickListener oKClickListener = null;
        DialogInterface.OnClickListener cancelClickListener = null;
        OnCancelListener cancelListener = new OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
            }
        };

        if (isSDCardExist) {
            oKClickListener = new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    if (DBG) {
                        log("showStorageDialog... , on click, which=" + which);
                    }
                    if (null != mStorageSpaceDialog) {
                        mStorageSpaceDialog.dismiss();
                    }
                    //To Setting Storage
                    Intent intent = new Intent(Constants.STORAGE_SETTING_INTENT_NAME);
                    startActivity(intent);
                }
            };
        }

        cancelClickListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (DBG) {
                    log("showStorageDialog... , on click, which=" + which);
                }
                if (null != mStorageSpaceDialog) {
                    mStorageSpaceDialog.dismiss();
                }
            }
        };

        CharSequence cancelButtonText = isSDCardExist ? getResources().getText(R.string.alert_dialog_dismiss) :
                                                        getResources().getText(R.string.ok);
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this).setMessage(msg)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle(getResources().getText(R.string.reminder))
            .setNegativeButton(cancelButtonText, cancelClickListener)
            .setOnCancelListener(cancelListener);
        if (isSDCardExist) {
            dialogBuilder.setPositiveButton(getResources().getText(R.string.vt_change_my_pic),
                                            oKClickListener);
        }

        mStorageSpaceDialog = dialogBuilder.create();
        mStorageSpaceDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        mStorageSpaceDialog.setOnShowListener(this);
        mStorageSpaceDialog.show();
    }

    /**
     * change by mediatek .inc end
     */


    /**
     * Enables or disables the status bar "window shade" based on the current situation.
     */
    private void updateExpandedViewState() {
        if (mIsForegroundActivity) {
            if (mApp.proximitySensorModeEnabled()) {
                // We should not enable notification's expanded view on RINGING state.
                mApp.notificationMgr.statusBarHelper.enableExpandedView(
                        mCM.getState() != PhoneConstants.State.RINGING);
            } else {
                // If proximity sensor is unavailable on the device, disable it to avoid false
                // touches toward notifications.
                mApp.notificationMgr.statusBarHelper.enableExpandedView(false);
            }
        } else {
            mApp.notificationMgr.statusBarHelper.enableExpandedView(true);
        }
    }

    /**
     * Requests to remove provider info frame after having
     * {@link #PROVIDER_INFO_TIMEOUT}) msec delay.
     */
    /* package */ void requestRemoveProviderInfoWithDelay() {
        // Remove any zombie messages and then send a message to
        // self to remove the provider info after some time.
        mHandler.removeMessages(EVENT_HIDE_PROVIDER_INFO);
        Message msg = Message.obtain(mHandler, EVENT_HIDE_PROVIDER_INFO);
        mHandler.sendMessageDelayed(msg, PROVIDER_INFO_TIMEOUT);
        if (DBG) {
            log("Requested to remove provider info after " + PROVIDER_INFO_TIMEOUT + " msec.");
        }
    }

    /**
     * Indicates whether or not the QuickResponseDialog is currently showing in the call screen
     */
    public boolean isQuickResponseDialogShowing() {
        return mRespondViaSmsManager != null && mRespondViaSmsManager.isShowingPopup();
    }

    //This is ugly but we have no choice(ALPS00333673).
    boolean isNeedToUpdateInCallNotification() {
        boolean result = false;
        ActivityManager am = (ActivityManager)this.getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningTaskInfo> list = am.getRunningTasks(1);
        for (RunningTaskInfo info : list) {
            String name = info.topActivity.getClassName();
            if (DBG) {
                log("start dump");
                log("info.baseActivity.getPackageName() " + info.baseActivity.getPackageName());
                log("info.topActivity.getClassName() " + name);
                log("stop dump");
            }
            
            if ("com.android.phone.InCallScreen".equals(name)) {
                result = true;
                break;
            }
        }
        /// M: Even if the screen is off, we still need update the notification.
        return !result;//&& mPowerManager.isScreenOn();
    }

    private void selectWhichCallActive(final List<Call> list) {
        List<Call> holdList = new ArrayList<Call>();
        if (DBG) {
            log("+selectWhichCallActive");
        }
        for (Call call : list) {
            if (call.getState() == Call.State.HOLDING) {
                holdList.add(call);
            }
        }

        if (null == mCallSelectDialog || !mCallSelectDialog.isShowing()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this).setNegativeButton(
                    getResources().getString(android.R.string.cancel),
                    (DialogInterface.OnClickListener) null);

            CallPickerAdapter callPickerAdapter = new CallPickerAdapter(this, holdList);
            if (2 == holdList.size()) {
                callPickerAdapter.setOperatorName(mCallCard.getOperatorNameByCall(holdList.get(0)), mCallCard
                        .getOperatorNameByCall(holdList.get(1)));
                callPickerAdapter.setOperatorColor(mCallCard.getOperatorColorByCall(holdList.get(0)), mCallCard
                        .getOperatorColorByCall(holdList.get(1)));
                callPickerAdapter.setCallerInfoName(mCallCard.getCallInfoName(1), mCallCard
                        .getCallInfoName(2));

                builder.setSingleChoiceItems(callPickerAdapter, -1,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                final AlertDialog alert = (AlertDialog) dialog;
                                final ListAdapter listAdapter = alert.getListView().getAdapter();
                                Call call = (Call) listAdapter.getItem(which);
                                Call firstBgCall = mDualTalk.getFirstActiveBgCall();
                                Call secondBgCall = mDualTalk.getSecondActiveBgCall();
                                if (null != call && null != firstBgCall && null != secondBgCall) {
                                    Phone firstPhone = firstBgCall.getPhone();
                                    Phone secondPhone = secondBgCall.getPhone();
                                    if (DBG) {
                                        log("select call at phone :" + call.getPhone() + " firstPhone " + firstPhone
                                                + " secondPhone " + secondPhone);
                                    }

                                    if (call.getPhone() == firstPhone) {
                                        //This is ugly, only for the craze clicking and clicking......
                                        //This is only for tester's operation[ALPS00446633]
                                        mLastClickActionTime = SystemClock.uptimeMillis();
                                        PhoneUtils.switchHoldingAndActive(call);
                                    } else {
                                        handleUnholdAndEnd(mDualTalk.getActiveFgCall());
                                    }
                                }
                                dialog.dismiss();
                            }
                        }).setTitle(getResources().getString(R.string.which_call_to_activate));
                mCallSelectDialog = builder.create();
                mCallSelectDialog.show();
            }
        }
        if (DBG) {
            log("-selectWhichCallActive");
        }
    }

    /** --------------------------------MTK------------------------------------------- */
    /// M:Gemini+ @{
    private MTKCallManager mCMGemini;

    private static final int PHONE_DISCONNECT2 = 144;
    private static final int PHONE_DISCONNECT3 = 244;
    private static final int PHONE_DISCONNECT4 = 344;

    private static final int[] PHONE_DISCONNECT_GEMINI = new int[] { PHONE_DISCONNECT, PHONE_DISCONNECT2,
            PHONE_DISCONNECT3, PHONE_DISCONNECT4 };

    public static final int DELAYED_CLEANUP_AFTER_DISCONNECT2 = 147;
    public static final int DELAYED_CLEANUP_AFTER_DISCONNECT3 = 247;
    public static final int DELAYED_CLEANUP_AFTER_DISCONNECT4 = 347;

    public static final int[] DELAYED_CLEANUP_AFTER_DISCONNECT_GEMINI = new int[] { DELAYED_CLEANUP_AFTER_DISCONNECT,
            DELAYED_CLEANUP_AFTER_DISCONNECT2, DELAYED_CLEANUP_AFTER_DISCONNECT3, DELAYED_CLEANUP_AFTER_DISCONNECT4 };

    /**
     * check the slot and call delayedCleanupAfterDisconnect()
     */
    public void delayedCleanupAfterDisconnect() {
        int[] geminiSlots = GeminiUtils.getSlots();
        for (int i = 0; i < geminiSlots.length; i++) {
            delayedCleanupAfterDisconnect(DELAYED_CLEANUP_AFTER_DISCONNECT_GEMINI[i]);
        }
    }
    /// @}

    /**
     * Posts a message to our handler saying to update the onscreen UI
     * based on a bluetooth headset state change.
     *
     * M: Add param bluetoothHeadsetAudioState in order to get the audio state. @{
     * For ALPS00389674
     */
    /* package */ void requestUpdateBluetoothIndication(int bluetoothHeadsetAudioState) {
        if (VDBG) {
            log("requestUpdateBluetoothIndication()... bluetoothHeadsetAudioState= "
                    + bluetoothHeadsetAudioState);
        }
        // When get state that BT connect fail, reset mBluetoothConnectionPending to update icon.
        if (BluetoothHeadset.STATE_AUDIO_DISCONNECTED == bluetoothHeadsetAudioState) {
            mBluetoothConnectionPending = false;
        }

        requestUpdateBluetoothIndication();
    }
    /// @}

    public void acceptIncomingCallByVoiceCommand() {
        log("acceptIncomingCallByVoiceCommand");
        handleOnscreenButtonClick(R.id.incomingCallAnswer);
        if (!mApp.isHeadsetPlugged() && !mApp.isBluetoothHeadsetAudioOn()) {
            log("acceptIncomingCallByVoiceCommand(), turn on speaker");
            PhoneUtils.turnOnSpeaker(mApp, true, true, true);
        }
    }

    public void rejectIncomingCallByVoiceCommand() {
        log("rejectIncomingCallByVoiceCommand");
        handleOnscreenButtonClick(R.id.incomingCallReject);
    }
    
    void handleSwitchFailed() {
        if (mCM.getState() == PhoneConstants.State.RINGING) {
            //This hold must be triggered by answer the ringing call
            //pop a toast and update the incallscreen.
            log("send message to update screen after 500ms");
            requestUpdateScreenDelay(500);
            Toast.makeText(InCallScreen.this, R.string.incall_error_supp_service_switch, Toast.LENGTH_LONG).show();
            return;
        }
        List<Call> activeCalls = null;
        if (DualTalkUtils.isSupportDualTalk() && mDualTalk != null) {
            activeCalls = mDualTalk.getAllActiveCalls();
        } else {
            activeCalls = new ArrayList<Call>();
            List<Phone> phoneList = mCM.getAllPhones();
            for (Phone phone : phoneList) {
                if (DBG) {
                    log("active phone = " + phone + " phone state = " + phone.getState());
                }
                if (phone.getState() == PhoneConstants.State.OFFHOOK) {
                    Call fgCall = phone.getForegroundCall();
                    if (DBG) {
                        log("active call = " + fgCall.getConnections() + " state = " + fgCall.getState());
                    }
                    if (fgCall != null && fgCall.getState().isAlive()) {
                        activeCalls.add(fgCall);
                    }
                }
            }
        }
        if (activeCalls == null || activeCalls.size() < 2) {
            log("This is only one ACTIVE call, so do nothing.");
        } else {
            long firstDuration = CallTime.getCallDuration(activeCalls.get(0));
            long secondDuration = CallTime.getCallDuration(activeCalls.get(1));
            log("More than one ACTIVE calls, hangup the latest.");
            try {
                if (firstDuration > secondDuration) {
                    activeCalls.get(1).hangup();
                } else {
                    activeCalls.get(0).hangup();
                }
            } catch (CallStateException e) {
            }
        }
    }
    /// @}

    private void registerForPhoneStatesMTK(Object callManager) {
        GeminiRegister.registerForCrssSuppServiceNotification(callManager, mHandler, CRSS_SUPP_SERVICE);
        GeminiRegister.registerForSpeechInfo(callManager, mHandler, PhoneUtils.PHONE_SPEECH_INFO_GEMINI);
        GeminiRegister.registerForSuppServiceNotification(callManager, mHandler,  SUPP_SERVICE_NOTIFICATION);
    }

    private void unregisterForPhoneStatesMTK(Object callManager) {
        GeminiRegister.unregisterForSpeechInfo(callManager, mHandler);
        GeminiRegister.unregisterForCrssSuppServiceNotification(callManager, mHandler);
        GeminiRegister.unregisterForSuppServiceNotification(callManager,mHandler);
    }

    public boolean isVTInCallScreenOn() {
        if (FeatureOption.MTK_VT3G324M_SUPPORT) {
            return mVTInCallScreen.getVTScreenMode() == Constants.VTScreenMode.VT_SCREEN_OPEN;
        }
        return false;
    }

    /// M: The fuction for getting the screen size. @{
    private Point sScreenSize;

    public Point getScreenSize() {
        if (sScreenSize == null) {
            final Point outSize = new Point();
            getWindowManager().getDefaultDisplay().getSize(outSize);
            sScreenSize = outSize;
        }

        return sScreenSize;
    }
    /// @}


    /// M: To adjust the layout for some special screen,
    // such as no physical key screen, which has a soft navigation bar. @{
    //
    // MTK add
    private void amendLayoutForDifferentScreen() {
        if (PhoneGlobals.sHasNavigationBar) {
            // in the on physical key screen, the multi-call hold icon height is too tall,
            // we need adjust to the small one.
            ViewStub view = (ViewStub) findViewById(R.id.secondary_call_info);
            view.getLayoutParams().height = getResources().getDimensionPixelSize(
                    R.dimen.nopk_incoming_call_2_total_height);
        }
    }
    /// @}
}
