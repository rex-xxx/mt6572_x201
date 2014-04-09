/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.internal.policy.impl.keyguard;

import static android.provider.Settings.System.SCREEN_OFF_TIMEOUT;

import android.app.Activity;
import android.app.ActivityManagerNative;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.app.StatusBarManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.EventLog;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.view.WindowManagerPolicy;

import com.android.internal.R;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.IccCardConstants.State;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.widget.LockPatternUtils;

import com.mediatek.common.featureoption.FeatureOption;


/**
 * Mediates requests related to the keyguard.  This includes queries about the
 * state of the keyguard, power management events that effect whether the keyguard
 * should be shown or reset, callbacks to the phone window manager to notify
 * it of when the keyguard is showing, and events from the keyguard view itself
 * stating that the keyguard was succesfully unlocked.
 *
 * Note that the keyguard view is shown when the screen is off (as appropriate)
 * so that once the screen comes on, it will be ready immediately.
 *
 * Example queries about the keyguard:
 * - is {movement, key} one that should wake the keygaurd?
 * - is the keyguard showing?
 * - are input events restricted due to the state of the keyguard?
 *
 * Callbacks to the phone window manager:
 * - the keyguard is showing
 *
 * Example external events that translate to keyguard view changes:
 * - screen turned off -> reset the keyguard, and show it so it will be ready
 *   next time the screen turns on
 * - keyboard is slid open -> if the keyguard is not secure, hide it
 *
 * Events from the keyguard view:
 * - user succesfully unlocked keyguard -> hide keyguard view, and no longer
 *   restrict input events.
 *
 * Note: in addition to normal power managment events that effect the state of
 * whether the keyguard should be showing, external apps and services may request
 * that the keyguard be disabled via {@link #setKeyguardEnabled(boolean)}.  When
 * false, this will override all other conditions for turning on the keyguard.
 *
 * Threading and synchronization:
 * This class is created by the initialization routine of the {@link WindowManagerPolicy},
 * and runs on its thread.  The keyguard UI is created from that thread in the
 * constructor of this class.  The apis may be called from other threads, including the
 * {@link com.android.server.input.InputManagerService}'s and {@link android.view.WindowManager}'s.
 * Therefore, methods on this class are synchronized, and any action that is pointed
 * directly to the keyguard UI is posted to a {@link Handler} to ensure it is taken on the UI
 * thread of the keyguard.
 */
public class KeyguardViewMediator {
    private static final int KEYGUARD_DISPLAY_TIMEOUT_DELAY_DEFAULT = 30000;
    final static boolean DEBUG = true;
    private final static boolean DBG_WAKE = true;
    private final static boolean DBG_MESSAGE = true;

    private final static String TAG = "KeyguardViewMediator";

    private static final String DELAYED_KEYGUARD_ACTION =
        "com.android.internal.policy.impl.PhoneWindowManager.DELAYED_KEYGUARD";

    private boolean mIsIPOBoot = false;
    // used for handler messages
    private static final int SHOW = 2;
    private static final int HIDE = 3;
    private static final int RESET = 4;
    private static final int VERIFY_UNLOCK = 5;
    private static final int NOTIFY_SCREEN_OFF = 6;
    private static final int NOTIFY_SCREEN_ON = 7;
    private static final int WAKE_WHEN_READY = 8;
    private static final int KEYGUARD_DONE = 9;
    private static final int KEYGUARD_DONE_DRAWING = 10;
    private static final int KEYGUARD_DONE_AUTHENTICATING = 11;
    private static final int SET_HIDDEN = 12;
    private static final int KEYGUARD_TIMEOUT = 13;
    private static final int SHOW_ASSISTANT = 14;

    /**
     * The default amount of time we stay awake (used for all key input)
     */
    protected static final int AWAKE_INTERVAL_DEFAULT_MS = 10000;

    /**
     * How long to wait after the screen turns off due to timeout before
     * turning on the keyguard (i.e, the user has this much time to turn
     * the screen back on without having to face the keyguard).
     */
    private static final int KEYGUARD_LOCK_AFTER_DELAY_DEFAULT = 5000;

    /**
     * How long we'll wait for the {@link ViewMediatorCallback#keyguardDoneDrawing()}
     * callback before unblocking a call to {@link #setKeyguardEnabled(boolean)}
     * that is reenabling the keyguard.
     */
    private static final int KEYGUARD_DONE_DRAWING_TIMEOUT_MS = 2000;

    /**
     * Allow the user to expand the status bar when the keyguard is engaged
     * (without a pattern or password).
     */
    private static final boolean ENABLE_INSECURE_STATUS_BAR_EXPAND = true;

    /** The stream type that the lock sounds are tied to. */
    private int mMasterStreamType;

    private Context mContext;
    private AlarmManager mAlarmManager;
    private AudioManager mAudioManager;
    private StatusBarManager mStatusBarManager;
    private boolean mShowLockIcon;
    private boolean mShowingLockIcon;

    private boolean mSystemReady;

    // Whether the next call to playSounds() should be skipped.  Defaults to
    // true because the first lock (on boot) should be silent.
    private boolean mSuppressNextLockSound = true;


    /** High level access to the power manager for WakeLocks */
    private PowerManager mPM;

    /** UserManager for querying number of users */
    private UserManager mUserManager;

    /** SearchManager for determining whether or not search assistant is available */
    private SearchManager mSearchManager;

    /**
     * Used to keep the device awake while to ensure the keyguard finishes opening before
     * we sleep.
     */
    private PowerManager.WakeLock mShowKeyguardWakeLock;

    /**
     * Does not turn on screen, held while a call to {@link KeyguardViewManager#wakeWhenReadyTq(int)}
     * is called to make sure the device doesn't sleep before it has a chance to poke
     * the wake lock.
     * @see #wakeWhenReady(int)
     */
    private PowerManager.WakeLock mWakeAndHandOff;

    private KeyguardViewManager mKeyguardViewManager;

    // these are protected by synchronized (this)

    /**
     * External apps (like the phone app) can tell us to disable the keygaurd.
     */
    private static boolean mExternallyEnabled = true;

    /**
     * Remember if an external call to {@link #setKeyguardEnabled} with value
     * false caused us to hide the keyguard, so that we need to reshow it once
     * the keygaurd is reenabled with another call with value true.
     */
    private boolean mNeedToReshowWhenReenabled = false;

    // cached value of whether we are showing (need to know this to quickly
    // answer whether the input should be restricted)
    private static boolean mShowing = false;

    // true if the keyguard is hidden by another window
    private static boolean mHidden = false;

    /**
     * Helps remember whether the screen has turned on since the last time
     * it turned off due to timeout. see {@link #onScreenTurnedOff(int)}
     */
    private int mDelayedShowingSequence;

    /**
     * If the user has disabled the keyguard, then requests to exit, this is
     * how we'll ultimately let them know whether it was successful.  We use this
     * var being non-null as an indicator that there is an in progress request.
     */
    private WindowManagerPolicy.OnKeyguardExitResult mExitSecureCallback;

    // the properties of the keyguard

    private KeyguardUpdateMonitor mUpdateMonitor;

    private boolean mScreenOn;

    // last known state of the cellular connection
    private String mPhoneState = TelephonyManager.EXTRA_STATE_IDLE;

    /**
     * we send this intent when the keyguard is dismissed.
     */
    private Intent mUserPresentIntent;

    /**
     * {@link #setKeyguardEnabled} waits on this condition when it reenables
     * the keyguard.
     */
    private boolean mWaitingUntilKeyguardVisible = false;
    private LockPatternUtils mLockPatternUtils;
    private boolean mKeyguardDonePending = false;

    private SoundPool mLockSounds;
    private int mLockSoundId;
    private int mUnlockSoundId;
    private int mLockSoundStreamId;

    /**
     * The volume applied to the lock/unlock sounds.
     */
    private final float mLockSoundVolume;

    /// M: for count the message ID
    private static int sWakeMSGId = 0;

    /**
     * The callback used by the keyguard view to tell the {@link KeyguardViewMediator}
     * various things.
     */
    public interface ViewMediatorCallback {

        /**
         * Wake the device immediately.
         */
        void wakeUp();

        /**
         * Reports user activity and requests that the screen stay on.
         */
        void userActivity();

        /**
         * Reports user activity and requests that the screen stay on for at least
         * the specified amount of time.
         * @param millis The amount of time in millis.  This value is currently ignored.
         */
        void userActivity(long millis);

        /**
         * Report that the keyguard is done.
         * @param authenticated Whether the user securely got past the keyguard.
         *   the only reason for this to be false is if the keyguard was instructed
         *   to appear temporarily to verify the user is supposed to get past the
         *   keyguard, and the user fails to do so.
         */
        void keyguardDone(boolean authenticated);

        /**
         * Report that the keyguard is done drawing.
         */
        void keyguardDoneDrawing();

        /**
         * Tell ViewMediator that the current view needs IME input
         * @param needsInput
         */
        void setNeedsInput(boolean needsInput);

        /**
         * Tell view mediator that the keyguard view's desired user activity timeout
         * has changed and needs to be reapplied to the window.
         */
        void onUserActivityTimeoutChanged();

        /**
         * Report that the keyguard is dismissable, pending the next keyguardDone call.
         */
        void keyguardDonePending();
    }

    KeyguardUpdateMonitorCallback mUpdateCallback = new KeyguardUpdateMonitorCallback() {

        @Override
        public void onUserSwitched(int userId) {
            // Note that the mLockPatternUtils user has already been updated from setCurrentUser.
            // We need to force a reset of the views, since lockNow (called by
            // ActivityManagerService) will not reconstruct the keyguard if it is already showing.
            synchronized (KeyguardViewMediator.this) {
                Bundle options = new Bundle();
                options.putBoolean(LockPatternUtils.KEYGUARD_SHOW_USER_SWITCHER, true);
                resetStateLocked(options);
                adjustStatusBarLocked();
                // Disable face unlock when the user switches.
                KeyguardUpdateMonitor.getInstance(mContext).setAlternateUnlockEnabled(false);
            }
        }

        @Override
        public void onUserRemoved(int userId) {
            mLockPatternUtils.removeUser(userId);
        }

        @Override
        void onPhoneStateChanged(int phoneState) {
            synchronized (KeyguardViewMediator.this) {
                if (TelephonyManager.CALL_STATE_IDLE == phoneState  // call ending
                        && !mScreenOn                           // screen off
                        && mExternallyEnabled) {                // not disabled by any app

                    // note: this is a way to gracefully reenable the keyguard when the call
                    // ends and the screen is off without always reenabling the keyguard
                    // each time the screen turns off while in call (and having an occasional ugly
                    // flicker while turning back on the screen and disabling the keyguard again).
                    if (DEBUG) Log.d(TAG, "screen is off and call ended, let's make sure the "
                            + "keyguard is showing");
                    doKeyguardLocked();
                }
            }
        };

        @Override
        public void onClockVisibilityChanged() {
            adjustStatusBarLocked();
        }

        @Override
        public void onDeviceProvisioned() {
            sendUserPresentBroadcast();
        }

        @Override
        public void onSimStateChanged(IccCardConstants.State simState) {
            if (DEBUG) Log.d(TAG, "onSimStateChanged: " + simState);
            /// M: on simUNlockscreen, 400185 
            IccCardConstants.State lastSimState = mUpdateMonitor.getLastSimState(PhoneConstants.GEMINI_SIM_1);
            boolean lastWasLocked = (lastSimState == IccCardConstants.State.PIN_REQUIRED 
                                    || lastSimState == IccCardConstants.State.PUK_REQUIRED
                                    || lastSimState == IccCardConstants.State.NETWORK_LOCKED);
            switch (simState) {
                case ABSENT:
                    // only force lock screen in case of missing sim if user hasn't
                    // gone through setup wizard
                    /// M: on simUNlockscreen, 400185 
                    synchronized (this) {
                        if (!mUpdateMonitor.isDeviceProvisioned() || (lastWasLocked && simState == IccCardConstants.State.ABSENT)) {
                            if (!isShowing()) {
                                if (DEBUG) KeyguardUtils.xlogD(TAG, "ICC_ABSENT isn't showing,"
                                        + " we need to show the keyguard since the "
                                        + "device isn't provisioned yet.");
                                doKeyguardLocked();
                            } else {
                                resetStateLocked(null);
                            }
                        }
                    }
                    break;
                case PIN_REQUIRED:
                case PUK_REQUIRED:
                case NETWORK_LOCKED:
                    synchronized (this) {
                        if (!isShowing()) {
                            if (DEBUG) KeyguardUtils.xlogD(TAG, "INTENT_VALUE_ICC_LOCKED and keygaurd isn't "
                                    + "showing; need to show keyguard so user can enter sim pin");
                            doKeyguardLocked();
                        } else {
                            resetStateLocked(null);
                        }
                    }
                    break;
                case PERM_DISABLED:
                    synchronized (this) {
                        /// M: detected whether the sim card invlid ,if it is, show the invalid dialog. @{
                        if (mUpdateMonitor.getRetryPukCount(PhoneConstants.GEMINI_SIM_1) == 0) {
                            mUpdateMonitor.requestShowDialog(new InvalidDialogCallback());
                            break;
                        }
                        /// @}
                        if (!isShowing()) {
                            if (DEBUG) KeyguardUtils.xlogD(TAG, "PERM_DISABLED and "
                                  + "keygaurd isn't showing.");
                            doKeyguardLocked();
                        } else {
                            if (DEBUG) KeyguardUtils.xlogD(TAG, "PERM_DISABLED, resetStateLocked to"
                                  + "show permanently disabled message in lockscreen.");
                            resetStateLocked(null);
                        }
                    }
                    break;
                case READY:
                    synchronized (this) {
                        // if last view is PinPukView, it process ready in SimPinPukView onSimStateChange callbak
                        if (isShowing() && !lastWasLocked) {
                            resetStateLocked(null);
                        }
                    }
                    break;
                case NOT_READY:
                    synchronized (this) {
                        if (isShowing() && lastWasLocked) {
                            resetStateLocked(null);
                        }
                    }
                    break;
            }
        }
        
        @Override
        public void onSimStateChangedGemini(IccCardConstants.State simState, int simId) {
            if (DEBUG) {
                KeyguardUtils.xlogD(TAG, "onSimStateChangedGemini: " + simState + ",simId = " + simId);
            }
            IccCardConstants.State lastSimState = mUpdateMonitor.getLastSimState(simId);
            boolean lastWasLocked = (lastSimState == IccCardConstants.State.PIN_REQUIRED 
                                    || lastSimState == IccCardConstants.State.PUK_REQUIRED
                                    || lastSimState == IccCardConstants.State.NETWORK_LOCKED);

            switch (simState) {
                case ABSENT:
                    // only force lock screen in case of missing sim if user hasn't
                    // gone through setup wizard
                    // M: on simUNlockscreen, 400185  @{
                    if (!mUpdateMonitor.isDeviceProvisioned() || (lastWasLocked && simState == IccCardConstants.State.ABSENT)) {
                        if (!isShowing()) {
                            if (DEBUG) {
                                KeyguardUtils.xlogD(TAG, "INTENT_VALUE_ICC_ABSENT and keygaurd isn't showing, we need "
                                        + "to show the keyguard since the device isn't provisioned yet.");
                            }
                            doKeyguardLocked();
                        } else {
                            resetStateLocked(null);
                        }
                    }
                    /// @}
                    break;
                case PIN_REQUIRED:
                case PUK_REQUIRED:
                case NETWORK_LOCKED:
                    /// M: detected whether the sim card invlid ,if it is, show the invalid dialog.  @{
                    if (mUpdateMonitor.getRetryPukCount(simId) == 0) {
                        mUpdateMonitor.requestShowDialog(new InvalidDialogCallback());
                        break;
                    }
                    /// M: detected whether the SimME permanently locked, show the permanently locked dialog.
                    if (IccCardConstants.State.NETWORK_LOCKED == simState
                        && 0 == mUpdateMonitor.getSimMeLeftRetryCount(simId)) {
                        KeyguardUtils.xlogD(TAG, "SIM ME lock retrycount is 0 so SimME is not showing, only to show dialog");
                        mUpdateMonitor.requestShowDialog(new MeLockedDialogCallback());
                        break;
                    }
                    /// @}
                    if ((IccCardConstants.State.PIN_REQUIRED == simState && mUpdateMonitor.getPINDismissFlag(simId, KeyguardUpdateMonitor.SimLockType.SIM_LOCK_PIN))
                        || (IccCardConstants.State.PUK_REQUIRED == simState && mUpdateMonitor.getPINDismissFlag(simId, KeyguardUpdateMonitor.SimLockType.SIM_LOCK_PUK))
                        || (IccCardConstants.State.NETWORK_LOCKED == simState && mUpdateMonitor.getPINDismissFlag(simId, KeyguardUpdateMonitor.SimLockType.SIM_LOCK_ME))) {
                        KeyguardUtils.xlogD(TAG, "We have dismissed locks, so, we should reset the dismiss flags");
                        mUpdateMonitor.setPINDismiss(simId, KeyguardUpdateMonitor.SimLockType.SIM_LOCK_PIN, false);
                        mUpdateMonitor.setPINDismiss(simId, KeyguardUpdateMonitor.SimLockType.SIM_LOCK_PUK, false);
                        mUpdateMonitor.setPINDismiss(simId, KeyguardUpdateMonitor.SimLockType.SIM_LOCK_ME, false);
                    }
                   
                    if (!isShowing()) {
                        if (DEBUG) {
                            KeyguardUtils.xlogD(TAG, "INTENT_VALUE_ICC_LOCKED and keygaurd isn't showing, we need "
                                    + "to show the keyguard so the user can enter their sim pin");
                        }
                        doKeyguardLocked();
                    } else {
                        resetStateLocked(null);
                    }

                    break;

                case READY:
                    // if last view is PinPukView, it process ready in SimPinPukView onSimStateChange callbak
                    if (isShowing() && !lastWasLocked) {
                        resetStateLocked(null);
                    }
                    break;

                case NOT_READY:
                    if (isShowing() && lastWasLocked) {
                        resetStateLocked(null);
                    }
                    break;
                    
                default:
                    break;
            }
        }
    };

    ViewMediatorCallback mViewMediatorCallback = new ViewMediatorCallback() {
        public void wakeUp() {
            KeyguardViewMediator.this.wakeUp();
        }

        public void userActivity() {
            KeyguardViewMediator.this.userActivity();
        }

        public void userActivity(long holdMs) {
            KeyguardViewMediator.this.userActivity(holdMs);
        }

        public void keyguardDone(boolean authenticated) {
            KeyguardViewMediator.this.keyguardDone(authenticated, true);
        }

        public void keyguardDoneDrawing() {
            mHandler.sendEmptyMessage(KEYGUARD_DONE_DRAWING);
        }

        @Override
        public void setNeedsInput(boolean needsInput) {
            mKeyguardViewManager.setNeedsInput(needsInput);
        }

        @Override
        public void onUserActivityTimeoutChanged() {
            mKeyguardViewManager.updateUserActivityTimeout();
        }

        @Override
        public void keyguardDonePending() {
            mKeyguardDonePending = true;
        }
    };

    public void wakeUp() {
        mPM.wakeUp(SystemClock.uptimeMillis());
    }

    public void userActivity() {
        userActivity(AWAKE_INTERVAL_DEFAULT_MS);
    }

    public void userActivity(long holdMs) {
        // We ignore the hold time.  Eventually we should remove it.
        // Instead, the keyguard window has an explicit user activity timeout set on it.
        mPM.userActivity(SystemClock.uptimeMillis(), false);
    }

    /**
     * Construct a KeyguardViewMediator
     * @param context
     * @param lockPatternUtils optional mock interface for LockPatternUtils
     */
    public KeyguardViewMediator(Context context, LockPatternUtils lockPatternUtils) {
        mContext = context;
        mPM = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        mUserManager = (UserManager) mContext.getSystemService(Context.USER_SERVICE);
        mShowKeyguardWakeLock = mPM.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "show keyguard");
        mShowKeyguardWakeLock.setReferenceCounted(false);

        mWakeAndHandOff = mPM.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "keyguardWakeAndHandOff");
        mWakeAndHandOff.setReferenceCounted(false);

        /// M: Add DM lock intent, when DM intent comes, we reset keyguard so that all unlock widget can be updated
        IntentFilter filter = new IntentFilter();
        filter.addAction(DELAYED_KEYGUARD_ACTION);
        /// M: For DM lock @{
        filter.addAction(OMADM_LAWMO_LOCK);
        filter.addAction(OMADM_LAWMO_UNLOCK);
        /// @}

        /// M: power-off alarm @{
        filter.addAction(IPO_DISABLE);
        filter.addAction(NORMAL_SHUTDOWN_ACTION);
        filter.addAction(LAUNCH_PWROFF_ALARM);
        filter.addAction(NORMAL_BOOT_ACTION);
        filter.addAction(PRE_SHUTDOWN);
        /// @}
        mContext.registerReceiver(mBroadcastReceiver, filter);

        mAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        mUpdateMonitor = KeyguardUpdateMonitor.getInstance(context);

        mLockPatternUtils = lockPatternUtils != null
                ? lockPatternUtils : new LockPatternUtils(mContext);
        mLockPatternUtils.setCurrentUser(UserHandle.USER_OWNER);

        WindowManager wm = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);

        mKeyguardViewManager = new KeyguardViewManager(context, wm, mViewMediatorCallback,
                mLockPatternUtils);

        mUserPresentIntent = new Intent(Intent.ACTION_USER_PRESENT);
        mUserPresentIntent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING
                | Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT);

        final ContentResolver cr = mContext.getContentResolver();
        mShowLockIcon = (Settings.System.getInt(cr, "show_status_bar_lock", 0) == 1);

        mScreenOn = mPM.isScreenOn();

        mLockSounds = new SoundPool(1, AudioManager.STREAM_SYSTEM, 0);
        String soundPath = Settings.Global.getString(cr, Settings.Global.LOCK_SOUND);
        if (soundPath != null) {
            mLockSoundId = mLockSounds.load(soundPath, 1);
        }
        if (soundPath == null || mLockSoundId == 0) {
            Log.w(TAG, "failed to load lock sound from " + soundPath);
        }
        soundPath = Settings.Global.getString(cr, Settings.Global.UNLOCK_SOUND);
        if (soundPath != null) {
            mUnlockSoundId = mLockSounds.load(soundPath, 1);
        }
        if (soundPath == null || mUnlockSoundId == 0) {
            Log.w(TAG, "failed to load unlock sound from " + soundPath);
        }
        int lockSoundDefaultAttenuation = context.getResources().getInteger(
                com.android.internal.R.integer.config_lockSoundVolumeDb);
        mLockSoundVolume = (float)Math.pow(10, (float)lockSoundDefaultAttenuation/20);
    }

    /**
     * Let us know that the system is ready after startup.
     */
    public void onSystemReady() {
        mSearchManager = (SearchManager) mContext.getSystemService(Context.SEARCH_SERVICE);
        synchronized (this) {
            if (DEBUG) Log.d(TAG, "onSystemReady");
            mSystemReady = true;
            mUpdateMonitor.registerCallback(mUpdateCallback);

            // Suppress biometric unlock right after boot until things have settled if it is the
            // selected security method, otherwise unsuppress it.  It must be unsuppressed if it is
            // not the selected security method for the following reason:  if the user starts
            // without a screen lock selected, the biometric unlock would be suppressed the first
            // time they try to use it.
            //
            // Note that the biometric unlock will still not show if it is not the selected method.
            // Calling setAlternateUnlockEnabled(true) simply says don't suppress it if it is the
            // selected method.
            if (mLockPatternUtils.usingBiometricWeak()
                    && mLockPatternUtils.isBiometricWeakInstalled()
                    || mLockPatternUtils.usingVoiceWeak()
                    && FeatureOption.MTK_VOICE_UNLOCK_SUPPORT) {
                if (DEBUG) Log.d(TAG, "suppressing biometric unlock during boot");
                mUpdateMonitor.setAlternateUnlockEnabled(false);
            } else {
                mUpdateMonitor.setAlternateUnlockEnabled(true);
            }
            /// M: power-off alarm @{
            if (!KeyguardUpdateMonitor.isAlarmBoot()) {
                doKeyguardLocked();
            }
            /// @}
        }
        // Most services aren't available until the system reaches the ready state, so we
        // send it here when the device first boots.
        maybeSendUserPresentBroadcast();
    }

    /**
     * Called to let us know the screen was turned off.
     * @param why either {@link WindowManagerPolicy#OFF_BECAUSE_OF_USER},
     *   {@link WindowManagerPolicy#OFF_BECAUSE_OF_TIMEOUT} or
     *   {@link WindowManagerPolicy#OFF_BECAUSE_OF_PROX_SENSOR}.
     */
    public void onScreenTurnedOff(int why) {
        if (DBG_WAKE) KeyguardUtils.xlogD(TAG, ">>>onScreenTurnedOff(" + why + ") ---ScreenOff Before--synchronized (this)");
        synchronized (this) {
            mScreenOn = false;
            if (DBG_WAKE) KeyguardUtils.xlogD(TAG, "onScreenTurnedOff(" + why + ") ---ScreenOff mScreenOn = false; Before--boolean lockImmediately");

            mKeyguardDonePending = false;

            // Lock immediately based on setting if secure (user has a pin/pattern/password).
            // This also "locks" the device when not secure to provide easy access to the
            // camera while preventing unwanted input.
            final boolean lockImmediately =
                mLockPatternUtils.getPowerButtonInstantlyLocks() || !mLockPatternUtils.isSecure();
            
            if (DBG_WAKE) KeyguardUtils.xlogD(TAG, "onScreenTurnedOff(" + why + ") ---ScreenOff mScreenOn = false; After--boolean lockImmediately=" 
                    + lockImmediately + ", mExitSecureCallback=" + mExitSecureCallback + ", mShowing=" + mShowing);
            
            /// M: Mediatek add to make sure donw drawing is send before screen off
            if (mExitSecureCallback != null) {
                if (DBG_WAKE) KeyguardUtils.xlogD(TAG, "onScreenTurnedOff(" + why + ") ---ScreenOff pending exit secure callback cancelled ---ScreenOff");
                mExitSecureCallback.onKeyguardExitResult(false);
                mExitSecureCallback = null;
                if (!mExternallyEnabled) {
                    hideLocked();
                }
            } else if (mShowing) {
                boolean isReady = ActivityManagerNative.isSystemReady();
                KeyguardUtils.xlogD(TAG, "on screen turned off, we should show keyguard immediately, " +
                        "because it doesn't destroyed, isReady=" + isReady);
                if (isReady) {
                    mContext.sendBroadcast(new Intent(Intent.ACTION_SEND),
                            "com.android.internal.policy.impl.KeyguardViewMediator.DONE_DRAW");
                }
                notifyScreenOffLocked();
                resetStateLocked(null);
            } else if (why == WindowManagerPolicy.OFF_BECAUSE_OF_TIMEOUT
                   || (why == WindowManagerPolicy.OFF_BECAUSE_OF_USER && !lockImmediately)) {
                doKeyguardLaterLocked();
            } else if (why == WindowManagerPolicy.OFF_BECAUSE_OF_PROX_SENSOR) {
                // Do not enable the keyguard if the prox sensor forced the screen off.
            } else {
                doKeyguardLocked();
            }
        }
        if (DBG_WAKE) KeyguardUtils.xlogD(TAG, "<<<onScreenTurnedOff(" + why + ") ---ScreenOff After--synchronized (this)");
    }

    private void doKeyguardLaterLocked() {
        // if the screen turned off because of timeout or the user hit the power button
        // and we don't need to lock immediately, set an alarm
        // to enable it a little bit later (i.e, give the user a chance
        // to turn the screen back on within a certain window without
        // having to unlock the screen)
        final ContentResolver cr = mContext.getContentResolver();

        // From DisplaySettings
        long displayTimeout = Settings.System.getInt(cr, SCREEN_OFF_TIMEOUT,
                KEYGUARD_DISPLAY_TIMEOUT_DELAY_DEFAULT);

        // From SecuritySettings
        final long lockAfterTimeout = Settings.Secure.getInt(cr,
                Settings.Secure.LOCK_SCREEN_LOCK_AFTER_TIMEOUT,
                KEYGUARD_LOCK_AFTER_DELAY_DEFAULT);

        // From DevicePolicyAdmin
        final long policyTimeout = mLockPatternUtils.getDevicePolicyManager()
                .getMaximumTimeToLock(null, mLockPatternUtils.getCurrentUser());

        long timeout;
        if (policyTimeout > 0) {
            // policy in effect. Make sure we don't go beyond policy limit.
            displayTimeout = Math.max(displayTimeout, 0); // ignore negative values
            timeout = Math.min(policyTimeout - displayTimeout, lockAfterTimeout);
        } else {
            timeout = lockAfterTimeout;
        }
        
        if (DBG_WAKE) KeyguardUtils.xlogD(TAG, "doKeyguardLaterLocked enter displayTimeout=" + displayTimeout 
                + ", lockAfterTimeout=" + lockAfterTimeout + ", policyTimeout=" + policyTimeout + ", timeout=" + timeout);

        if (timeout <= 0) {
            // Lock now
            mSuppressNextLockSound = true;
            doKeyguardLocked();
        } else {
            // Lock in the future
            long when = SystemClock.elapsedRealtime() + timeout;
            Intent intent = new Intent(DELAYED_KEYGUARD_ACTION);
            intent.putExtra("seq", mDelayedShowingSequence);
            PendingIntent sender = PendingIntent.getBroadcast(mContext,
                    0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
            mAlarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, when, sender);
            if (DEBUG) Log.d(TAG, "setting alarm to turn off keyguard, seq = "
                             + mDelayedShowingSequence);
        }
    }

    private void cancelDoKeyguardLaterLocked() {
        mDelayedShowingSequence++;
    }

    /**
     * Let's us know the screen was turned on.
     */
    public void onScreenTurnedOn(KeyguardViewManager.ShowListener showListener) {
        if (DBG_WAKE) KeyguardUtils.xlogD(TAG, ">>>onScreenTurnedOn, ---ScreenOn seq = " + mDelayedShowingSequence
                + "seq will be change in synchronized Before--synchronized (this)");
        synchronized (this) {
            mScreenOn = true;
            cancelDoKeyguardLaterLocked();
            if (DEBUG) Log.d(TAG, "onScreenTurnedOn, seq = " + mDelayedShowingSequence);
            if (showListener != null) {
                notifyScreenOnLocked(showListener);
            }
        }
        maybeSendUserPresentBroadcast();
        if (DBG_WAKE) KeyguardUtils.xlogD(TAG, "<<<onScreenTurnedOn, ---ScreenOn seq = " + mDelayedShowingSequence);
    }

    private void maybeSendUserPresentBroadcast() {
        if (mSystemReady && mLockPatternUtils.isLockScreenDisabled()
                && mUserManager.getUsers(true).size() == 1) {
            // Lock screen is disabled because the user has set the preference to "None".
            // In this case, send out ACTION_USER_PRESENT here instead of in
            // handleKeyguardDone()
            sendUserPresentBroadcast();
        }
    }

    /**
     * A dream started.  We should lock after the usual screen-off lock timeout but only
     * if there is a secure lock pattern.
     */
    public void onDreamingStarted() {
        synchronized (this) {
            if (mScreenOn && mLockPatternUtils.isSecure()) {
                doKeyguardLaterLocked();
            }
        }
    }

    /**
     * A dream stopped.
     */
    public void onDreamingStopped() {
        synchronized (this) {
            if (mScreenOn) {
                cancelDoKeyguardLaterLocked();
            }
        }
    }

    /**
     * Same semantics as {@link WindowManagerPolicy#enableKeyguard}; provide
     * a way for external stuff to override normal keyguard behavior.  For instance
     * the phone app disables the keyguard when it receives incoming calls.
     */
    public void setKeyguardEnabled(boolean enabled) {
        synchronized (this) {
            if (DEBUG) KeyguardUtils.xlogD(TAG, "setKeyguardEnabled(" + enabled + ")");

            mExternallyEnabled = enabled;

            if (!enabled && mShowing) {
                if (mExitSecureCallback != null) {
                    if (DEBUG) Log.d(TAG, "in process of verifyUnlock request, ignoring");
                    // we're in the process of handling a request to verify the user
                    // can get past the keyguard. ignore extraneous requests to disable / reenable
                    return;
                }

                // hiding keyguard that is showing, remember to reshow later
                if (DEBUG) Log.d(TAG, "remembering to reshow, hiding keyguard, "
                        + "disabling status bar expansion");
                mNeedToReshowWhenReenabled = true;
                hideLocked();
            } else if (enabled && mNeedToReshowWhenReenabled) {
                // reenabled after previously hidden, reshow
                if (DEBUG) Log.d(TAG, "previously hidden, reshowing, reenabling "
                        + "status bar expansion");
                mNeedToReshowWhenReenabled = false;

                if (mExitSecureCallback != null) {
                    if (DEBUG) Log.d(TAG, "onKeyguardExitResult(false), resetting");
                    mExitSecureCallback.onKeyguardExitResult(false);
                    mExitSecureCallback = null;
                    resetStateLocked(null);
                } else {
                    showLocked(null);

                    // block until we know the keygaurd is done drawing (and post a message
                    // to unblock us after a timeout so we don't risk blocking too long
                    // and causing an ANR).
                    mWaitingUntilKeyguardVisible = true;
                    mHandler.sendEmptyMessageDelayed(KEYGUARD_DONE_DRAWING, KEYGUARD_DONE_DRAWING_TIMEOUT_MS);
                    if (DEBUG) Log.d(TAG, "waiting until mWaitingUntilKeyguardVisible is false");
                    while (mWaitingUntilKeyguardVisible) {
                        try {
                            wait();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                    if (DEBUG) Log.d(TAG, "done waiting for mWaitingUntilKeyguardVisible");
                }
            }
        }
    }

    /**
     * @see android.app.KeyguardManager#exitKeyguardSecurely
     */
    public void verifyUnlock(WindowManagerPolicy.OnKeyguardExitResult callback) {
        synchronized (this) {
            if (DEBUG) Log.d(TAG, "verifyUnlock");
            if (!mUpdateMonitor.isDeviceProvisioned()) {
                // don't allow this api when the device isn't provisioned
                if (DEBUG) Log.d(TAG, "ignoring because device isn't provisioned");
                callback.onKeyguardExitResult(false);
            } else if (mExternallyEnabled) {
                // this only applies when the user has externally disabled the
                // keyguard.  this is unexpected and means the user is not
                // using the api properly.
                Log.w(TAG, "verifyUnlock called when not externally disabled");
                callback.onKeyguardExitResult(false);
            } else if (mExitSecureCallback != null) {
                // already in progress with someone else
                callback.onKeyguardExitResult(false);
            } else {
                mExitSecureCallback = callback;
                verifyUnlockLocked();
            }
        }
    }

    /**
     * Is the keyguard currently showing?
     */
    public boolean isShowing() {
        return mShowing;
    }

    /**
     * Is the keyguard currently showing and not being force hidden?
     */
    public boolean isShowingAndNotHidden() {
        return mShowing && !mHidden;
    }

    /**
     * Notify us when the keyguard is hidden by another window
     */
    public void setHidden(boolean isHidden) {
        if (DEBUG) Log.d(TAG, "setHidden " + isHidden);
        mUpdateMonitor.sendKeyguardVisibilityChanged(!isHidden);
        mHandler.removeMessages(SET_HIDDEN);
        Message msg = mHandler.obtainMessage(SET_HIDDEN, (isHidden ? 1 : 0), 0);
        mHandler.sendMessage(msg);
    }

    /**
     * Handles SET_HIDDEN message sent by setHidden()
     */
    private void handleSetHidden(boolean isHidden) {
        synchronized (KeyguardViewMediator.this) {
            if (mHidden != isHidden) {
                mHidden = isHidden;
                updateActivityLockScreenState();
                adjustStatusBarLocked();
            }
        }
    }

    /**
     * Used by PhoneWindowManager to enable the keyguard due to a user activity timeout.
     * This must be safe to call from any thread and with any window manager locks held.
     */
    public void doKeyguardTimeout(Bundle options) {
        mHandler.removeMessages(KEYGUARD_TIMEOUT);
        Message msg = mHandler.obtainMessage(KEYGUARD_TIMEOUT, options);
        mHandler.sendMessage(msg);
    }

    /**
     * Given the state of the keyguard, is the input restricted?
     * Input is restricted when the keyguard is showing, or when the keyguard
     * was suppressed by an app that disabled the keyguard or we haven't been provisioned yet.
     */
    public boolean isInputRestricted() {
        return mShowing || mNeedToReshowWhenReenabled || !mUpdateMonitor.isDeviceProvisioned();
    }

    private void doKeyguardLocked() {
        doKeyguardLocked(null);
    }

    /**
     * Enable the keyguard if the settings are appropriate.
     */
    private void doKeyguardLocked(Bundle options) {
        // if another app is disabling us, don't show
        if (!mExternallyEnabled || KeyguardUpdateMonitor.isAlarmBoot()) {
            if (DEBUG) KeyguardUtils.xlogD(TAG, "doKeyguard: not showing because externally disabled");

            // note: we *should* set mNeedToReshowWhenReenabled=true here, but that makes
            // for an occasional ugly flicker in this situation:
            // 1) receive a call with the screen on (no keyguard) or make a call
            // 2) screen times out
            // 3) user hits key to turn screen back on
            // instead, we reenable the keyguard when we know the screen is off and the call
            // ends (see the broadcast receiver below)
            // TODO: clean this up when we have better support at the window manager level
            // for apps that wish to be on top of the keyguard
            return;
        }

        // if the keyguard is already showing, don't bother
        if (mKeyguardViewManager.isShowing()) {
            if (DEBUG) KeyguardUtils.xlogD(TAG, "doKeyguard: not showing because it is already showing");
            return;
        }

        // if the setup wizard hasn't run yet, don't show
        if (DEBUG) KeyguardUtils.xlogD(TAG, "doKeyguard: get keyguard.no_require_sim property before");
        final boolean requireSim = !SystemProperties.getBoolean("keyguard.no_require_sim",
                false);
        if (DEBUG) KeyguardUtils.xlogD(TAG, "doKeyguard: get keyguard.no_require_sim property after");
        final boolean provisioned = mUpdateMonitor.isDeviceProvisioned();
        final IccCardConstants.State state = mUpdateMonitor.getSimState();
        boolean lockedOrMissing = false;
        /// M: Support GeminiPlus
        for (int i = PhoneConstants.GEMINI_SIM_1; i <= KeyguardUtils.getMaxSimId(); i++) {
            lockedOrMissing = (lockedOrMissing || isLockedOrMissingGemini(i, requireSim));
            if (lockedOrMissing) {
                break;
            }
        }
        
        if (DEBUG) KeyguardUtils.xlogD(TAG, "doKeyguard: get sim state after");

        /// M: MTK MOTA UPDATE when on ics2 keygaurd set none,update to JB,the keyguard will show LockScreen.
        /// MTK MOTA UPDATE when the phone first boot,check the settingDB mirged or not ,because mota update,
        /// the settingdb migrate slow than keygaurd(timing sequence problem) @{
        boolean keyguardDisable = false;
        
        /////*************************************TODO
        boolean motaUpdateFirst = true;//mLockPatternUtils.isDbMigrated();
        if (motaUpdateFirst) {
            /// DB mogi done
            keyguardDisable = mLockPatternUtils.isLockScreenDisabled();
        } else {
            /// DB not mogi
            final ContentResolver cr = mContext.getContentResolver();
            String value = Settings.Secure.getString(cr, "lockscreen.disabled");
            boolean booleanValue = false;
            if( null!=value ){
                booleanValue = value.equals("1") ? true :false;
            }
            keyguardDisable = (!mLockPatternUtils.isSecure()) && booleanValue;
        }
        /// @}
        
        if (DEBUG) KeyguardUtils.xlogD(TAG, "doKeyguard: keyguardDisable query end");
        
        /// M: Add new condition DM lock is not true
        boolean dmLocked = KeyguardUpdateMonitor.getInstance(mContext).dmIsLocked();
        KeyguardUtils.xlogD(TAG, "lockedOrMissing is " + lockedOrMissing + ", requireSim=" + requireSim
            + ", provisioned=" + provisioned + ", keyguardisable=" + keyguardDisable + ", dmLocked=" + dmLocked);
        
        if (!lockedOrMissing && !provisioned && !dmLocked) {
            if (DEBUG) Log.d(TAG, "doKeyguard: not showing because device isn't provisioned"
                    + " and the sim is not locked or missing");
            return;
        }

        /// M: Add a new condition DM lock is not on, or user can still bypass dm lock when Keygaurd is disabled
        if (mUserManager.getUsers(true).size() < 2
                && keyguardDisable && !lockedOrMissing && !KeyguardUpdateMonitor.getInstance(mContext).dmIsLocked()) {
            if (DEBUG) Log.d(TAG, "doKeyguard: not showing because lockscreen is off");
            return;
        }

        if (DEBUG) Log.d(TAG, "doKeyguard: showing the lock screen");
        showLocked(options);
    }

    /// M: Support GeminiPlus
    private boolean isLockedOrMissingGemini(int SimId, boolean requireSim) {
        if (mUpdateMonitor != null && KeyguardUtils.isValidSimId(SimId)) {
            final IccCardConstants.State stateGemini = mUpdateMonitor.getSimState(SimId);
            /// M: Remove absent sim state for issue ALPS00234952
            return (stateGemini == IccCardConstants.State.PIN_REQUIRED && !mUpdateMonitor.getPINDismissFlag(SimId, KeyguardUpdateMonitor.SimLockType.SIM_LOCK_PIN))
                    || (stateGemini == IccCardConstants.State.PUK_REQUIRED && !mUpdateMonitor.getPINDismissFlag(SimId, KeyguardUpdateMonitor.SimLockType.SIM_LOCK_PUK))
                    || (stateGemini == IccCardConstants.State.NETWORK_LOCKED && !mUpdateMonitor.getPINDismissFlag(SimId, KeyguardUpdateMonitor.SimLockType.SIM_LOCK_ME))
                    || (stateGemini == IccCardConstants.State.PERM_DISABLED && requireSim);
        } else {
            return false;
        }            
    }

    /**
     * Dismiss the keyguard through the security layers.
     */
    public void dismiss() {
        if (mShowing && !mHidden) {
            mKeyguardViewManager.dismiss();
        }
    }

    /**
     * Send message to keyguard telling it to reset its state.
     * @param options options about how to show the keyguard
     * @see #handleReset()
     */
    private void resetStateLocked(Bundle options) {
        if (DEBUG) KeyguardUtils.xlogD(TAG, "resetStateLocked");
        Message msg = mHandler.obtainMessage(RESET, options);
        mHandler.sendMessage(msg);
    }

    /**
     * Send message to keyguard telling it to verify unlock
     * @see #handleVerifyUnlock()
     */
    private void verifyUnlockLocked() {
        if (DEBUG) KeyguardUtils.xlogD(TAG, "verifyUnlockLocked");
        mHandler.sendEmptyMessage(VERIFY_UNLOCK);
    }


    /**
     * Send a message to keyguard telling it the screen just turned on.
     * @see #onScreenTurnedOff(int)
     * @see #handleNotifyScreenOff
     */
    private void notifyScreenOffLocked() {
        if (DEBUG) Log.d(TAG, "notifyScreenOffLocked");
        mHandler.sendEmptyMessage(NOTIFY_SCREEN_OFF);
    }

    /**
     * Send a message to keyguard telling it the screen just turned on.
     * @see #onScreenTurnedOn()
     * @see #handleNotifyScreenOn
     */
    private void notifyScreenOnLocked(KeyguardViewManager.ShowListener showListener) {
        if (DEBUG) KeyguardUtils.xlogD(TAG, "notifyScreenOnLocked");
        Message msg = mHandler.obtainMessage(NOTIFY_SCREEN_ON, showListener);
        mHandler.sendMessage(msg);
    }

    /**
     * Send message to keyguard telling it about a wake key so it can adjust
     * its state accordingly and then poke the wake lock when it is ready.
     * @param keyCode The wake key.
     * @see #handleWakeWhenReady
     * @see #onWakeKeyWhenKeyguardShowingTq(int)
     */
    private void wakeWhenReady(int keyCode) {
        if (DBG_WAKE) KeyguardUtils.xlogD(TAG, "wakeWhenReady(" + keyCode + ")");

        /**
         * acquire the handoff lock that will keep the cpu running.  this will
         * be released once the keyguard has set itself up and poked the other wakelock
         * in {@link #handleWakeWhenReady(int)}
         */
        mWakeAndHandOff.acquire();
        sWakeMSGId++;
        if (DBG_WAKE) KeyguardUtils.xlogD(TAG, "<<<wakeWhenReadyLocked(" + keyCode + ") Before--Send Message WAKE_WHEN_READY sWakeMSGId = " + sWakeMSGId);
        Message msg = mHandler.obtainMessage(WAKE_WHEN_READY, keyCode, sWakeMSGId);///M:
        mHandler.sendMessage(msg);
    }

    /**
     * Send message to keyguard telling it to show itself
     * @see #handleShow()
     */
    private void showLocked(Bundle options) {
        if (DEBUG) KeyguardUtils.xlogD(TAG, "showLocked");
        // ensure we stay awake until we are finished displaying the keyguard
        mShowKeyguardWakeLock.acquire();
        Message msg = mHandler.obtainMessage(SHOW, options);
        mHandler.sendMessage(msg);
    }

    /**
     * Send message to keyguard telling it to hide itself
     * @see #handleHide()
     */
    private void hideLocked() {
        if (DEBUG) KeyguardUtils.xlogD(TAG, "hideLocked");
        Message msg = mHandler.obtainMessage(HIDE);
        mHandler.sendMessage(msg);
    }

    public boolean isSecure() {
        return mLockPatternUtils.isSecure()
            || KeyguardUpdateMonitor.getInstance(mContext).isSimPinSecure();
    }

    /**
     * Update the newUserId. Call while holding WindowManagerService lock.
     * NOTE: Should only be called by KeyguardViewMediator in response to the user id changing.
     *
     * @param newUserId The id of the incoming user.
     */
    public void setCurrentUser(int newUserId) {
        mLockPatternUtils.setCurrentUser(newUserId);
    }

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (DELAYED_KEYGUARD_ACTION.equals(action)) {
                final int sequence = intent.getIntExtra("seq", 0);
                if (DEBUG) KeyguardUtils.xlogD(TAG, "received DELAYED_KEYGUARD_ACTION with seq = "
                        + sequence + ", mDelayedShowingSequence = " + mDelayedShowingSequence);
                synchronized (KeyguardViewMediator.this) {
                    if (mDelayedShowingSequence == sequence) {
                        // Don't play lockscreen SFX if the screen went off due to timeout.
                        mSuppressNextLockSound = true;
                        doKeyguardLocked();
                    }
                }
            }
            /// M: DM Begin @{
            else if (OMADM_LAWMO_LOCK.equals(action)) {
                KeyguardUpdateMonitor.getInstance(mContext).setDmLocked(true);
                KeyguardUtils.xlogD(TAG, "OMADM_LAWMO_LOCK received, KEYGUARD_DM_LOCKED");
                Message msg = mHandler.obtainMessage(MSG_DM_KEYGUARD_UPDATE);
                msg.arg1 = 1;
                msg.sendToTarget();
            } else if(OMADM_LAWMO_UNLOCK.equals(action)) {
                KeyguardUpdateMonitor.getInstance(mContext).setDmLocked(false);
                KeyguardUtils.xlogD(TAG, "OMADM_LAWMO_UNLOCK received, KEYGUARD_DM_LOCKED");
                Message msg = mHandler.obtainMessage(MSG_DM_KEYGUARD_UPDATE);
                msg.arg1 = 0;
                msg.sendToTarget();
              /// @}

              ///M: add for power-off alarm @{
            } else if (IPO_DISABLE.equals(action)) {
                mHideNavBar = false;
                mIsIPOBoot = true;
                Log.w(TAG, "IPO_DISABLE: " + action + "  alarmBoot: " + KeyguardUpdateMonitor.isAlarmBoot());
                if (KeyguardUpdateMonitor.isAlarmBoot()) {
                    hideLocked();
                }
            } else if (LAUNCH_PWROFF_ALARM.equals(action)) {
                Log.w(TAG, "LAUNCH_PWROFF_ALARM: " + action);
                mHandler.sendEmptyMessageDelayed(ALARM_BOOT, 1500);
            } else if (NORMAL_BOOT_ACTION.equals(action)) {
                Log.w(TAG, "NORMAL_BOOT_ACTION: " + action);
                mHandler.sendEmptyMessageDelayed(RESHOW_ANYWHERE, 1500);
            } else if (NORMAL_SHUTDOWN_ACTION.equals(action)) {
                //add to reset environment variables for power-off alarm
                //is running when schedule power off coming and shutdown device.
                Log.w(TAG, "ACTION_SHUTDOWN: " + action);
                if (KeyguardUpdateMonitor.isAlarmBoot()) {
                    SystemProperties.set("sys.boot.reason", "0");
                    mHandler.sendEmptyMessageDelayed(HIDE, 1500);
                }
                mHideNavBar = true;
            /// M: fix 441605, play sound after power off
            } else if (PRE_SHUTDOWN.equals(action)) {
                Log.w(TAG, "PRE_SHUTDOWN: " + action);
                mSuppressNextLockSound = true;
            }
            /// @}
        }
    };

    /**
     * When a key is received when the screen is off and the keyguard is showing,
     * we need to decide whether to actually turn on the screen, and if so, tell
     * the keyguard to prepare itself and poke the wake lock when it is ready.
     *
     * The 'Tq' suffix is per the documentation in {@link WindowManagerPolicy}.
     * Be sure not to take any action that takes a long time; any significant
     * action should be posted to a handler.
     *
     * @param keyCode The keycode of the key that woke the device
     */
    public void onWakeKeyWhenKeyguardShowingTq(int keyCode) {
        if (DEBUG) Log.d(TAG, "onWakeKeyWhenKeyguardShowing(" + keyCode + ")");

        // give the keyguard view manager a chance to adjust the state of the
        // keyguard based on the key that woke the device before poking
        // the wake lock
        wakeWhenReady(keyCode);
    }

    /**
     * When a wake motion such as an external mouse movement is received when the screen
     * is off and the keyguard is showing, we need to decide whether to actually turn
     * on the screen, and if so, tell the keyguard to prepare itself and poke the wake
     * lock when it is ready.
     *
     * The 'Tq' suffix is per the documentation in {@link WindowManagerPolicy}.
     * Be sure not to take any action that takes a long time; any significant
     * action should be posted to a handler.
     */
    public void onWakeMotionWhenKeyguardShowingTq() {
        if (DEBUG) Log.d(TAG, "onWakeMotionWhenKeyguardShowing()");

        // give the keyguard view manager a chance to adjust the state of the
        // keyguard based on the key that woke the device before poking
        // the wake lock
        wakeWhenReady(KeyEvent.KEYCODE_UNKNOWN);
    }

    public void keyguardDone(boolean authenticated, boolean wakeup) {
        mKeyguardDonePending = false;
        synchronized (this) {
            EventLog.writeEvent(70000, 2);
            if (DEBUG) KeyguardUtils.xlogD(TAG, "keyguardDone(" + authenticated + ")");
            Message msg = mHandler.obtainMessage(KEYGUARD_DONE);
            msg.arg1 = wakeup ? 1 : 0;
            mHandler.sendMessage(msg);

            if (authenticated) {
                mUpdateMonitor.clearFailedUnlockAttempts();
            }

            if (mExitSecureCallback != null) {
                mExitSecureCallback.onKeyguardExitResult(authenticated);
                mExitSecureCallback = null;

                if (authenticated) {
                    // after succesfully exiting securely, no need to reshow
                    // the keyguard when they've released the lock
                    mExternallyEnabled = true;
                    mNeedToReshowWhenReenabled = false;
                }
            }
        }
    }

    /**
     * This handler will be associated with the policy thread, which will also
     * be the UI thread of the keyguard.  Since the apis of the policy, and therefore
     * this class, can be called by other threads, any action that directly
     * interacts with the keyguard ui should be posted to this handler, rather
     * than called directly.
     */
    private Handler mHandler = new Handler(Looper.myLooper(), null, true /*async*/) {
        
        /// M: Add for log message string
        private String getMessageString(Message message) {
            switch (message.what) {
                case SHOW:
                    return "SHOW";
                case HIDE:
                    return "HIDE";
                case RESET:
                    return "RESET";
                case VERIFY_UNLOCK:
                    return "VERIFY_UNLOCK";
                case NOTIFY_SCREEN_OFF:
                    return "NOTIFY_SCREEN_OFF";
                case NOTIFY_SCREEN_ON:
                    return "NOTIFY_SCREEN_ON";
                case WAKE_WHEN_READY:
                    return "WAKE_WHEN_READY";
                case KEYGUARD_DONE:
                    return "KEYGUARD_DONE";
                case KEYGUARD_DONE_DRAWING:
                    return "KEYGUARD_DONE_DRAWING";
                case KEYGUARD_DONE_AUTHENTICATING:
                    return "KEYGUARD_DONE_AUTHENTICATING";
                case SET_HIDDEN:
                    return "SET_HIDDEN";
                case KEYGUARD_TIMEOUT:
                    return "KEYGUARD_TIMEOUT";
                case SHOW_ASSISTANT:
                    return "SHOW_ASSISTANT";
                case KEYGUARD_RELAYOUT:
                    return "KEYGUARD_RELAYOUT";
                case ALARM_BOOT:
                    return "ALARM_BOOT";
                case RESHOW_ANYWHERE:
                    return "RESHOW_ANYWHERE";
                case MSG_DM_KEYGUARD_UPDATE:
                    return "MSG_DM_KEYGUARD_UPDATE";
            }
            return null;
        }
        
        @Override
        public void handleMessage(Message msg) {
            if (DBG_MESSAGE) KeyguardUtils.xlogD(TAG, "handleMessage enter msg name=" + getMessageString(msg));
            switch (msg.what) {
                case SHOW:
                    handleShow((Bundle) msg.obj);
                    break;
                case HIDE:
                    handleHide();
                    break;
                case RESET:
                    handleReset((Bundle) msg.obj);
                    break;
                case VERIFY_UNLOCK:
                    handleVerifyUnlock();
                    break;
                case NOTIFY_SCREEN_OFF:
                    handleNotifyScreenOff();
                    break;
                case NOTIFY_SCREEN_ON:
                    handleNotifyScreenOn((KeyguardViewManager.ShowListener)msg.obj);
                    break;
                case WAKE_WHEN_READY:
                    /// M: add another pramater to identify the message ID
                    handleWakeWhenReady(msg.arg1, msg.arg2);
                    break;
                case KEYGUARD_DONE:
                    handleKeyguardDone(msg.arg1 != 0);
                    break;
                case KEYGUARD_DONE_DRAWING:
                    handleKeyguardDoneDrawing();
                    break;
                case KEYGUARD_DONE_AUTHENTICATING:
                    keyguardDone(true, true);
                    break;
                case SET_HIDDEN:
                    handleSetHidden(msg.arg1 != 0);
                    break;
                case KEYGUARD_TIMEOUT:
                    synchronized (KeyguardViewMediator.this) {
                        KeyguardUtils.xlogD(TAG, "doKeyguardLocked, because:KEYGUARD_TIMEOUT");
                        doKeyguardLocked((Bundle) msg.obj);
                    }
                    break;
                case SHOW_ASSISTANT:
                    handleShowAssistant();
                    break;
                /// M: Mediatek added message begin @{
                /// M: For DM lock to show/hide statusbar
                case KEYGUARD_RELAYOUT:
                    handleKeyguardReLayout(msg.arg1 == 1);
                    break;
                case ALARM_BOOT:
                    handleAlarmBoot();
                    break;
                case RESHOW_ANYWHERE:
                    SystemProperties.set("sys.boot.reason", "0");

                    boolean dmLocked = mUpdateMonitor.dmIsLocked();
                    mKeyguardViewManager.reLayoutScreen(dmLocked);

                    handleHide();
                    if(!mLockPatternUtils.isLockScreenDisabled()){
                        handleShow(null);
                    }
                    postDelayed(new Runnable(){
                        public void run(){
                            mContext.sendBroadcast(new Intent(NORMAL_BOOT_DONE_ACTION));
                        }
                    }, 4000);
                    break;
                case MSG_DM_KEYGUARD_UPDATE:
                    handleDMKeyguardUpdate(msg.arg1 == 1);
                    break;
                /// M: Mediatek added message end @}
            }
            if (DBG_MESSAGE) KeyguardUtils.xlogD(TAG, "handleMessage exit msg name=" + getMessageString(msg));
        }
    };

    /**
     * @see #keyguardDone
     * @see #KEYGUARD_DONE
     */
    private void handleKeyguardDone(boolean wakeup) {
        KeyguardUtils.xlogD(TAG, "handleKeyguardDone, wakeup=" + wakeup);
        handleHide();
        if (wakeup) {
            wakeUp();
        }

        sendUserPresentBroadcast();
    }

    private void sendUserPresentBroadcast() {
        /// M: If calling sendBroadcast() when ActivityManagerService is not yet "system ready",
        /// a Java Exception will pops up.
        if (!(mContext instanceof Activity) && ActivityManagerNative.isSystemReady()) {
            final UserHandle currentUser = new UserHandle(mLockPatternUtils.getCurrentUser());
            mContext.sendBroadcastAsUser(mUserPresentIntent, currentUser);
        }
    }

    /**
     * @see #keyguardDoneDrawing
     * @see #KEYGUARD_DONE_DRAWING
     */
    private void handleKeyguardDoneDrawing() {
        synchronized(this) {
            if (DEBUG) KeyguardUtils.xlogD(TAG, "handleKeyguardDoneDrawing");
            if (mWaitingUntilKeyguardVisible) {
                if (DEBUG) KeyguardUtils.xlogD(TAG, "handleKeyguardDoneDrawing: notifying mWaitingUntilKeyguardVisible");
                mWaitingUntilKeyguardVisible = false;
                notifyAll();

                // there will usually be two of these sent, one as a timeout, and one
                // as a result of the callback, so remove any remaining messages from
                // the queue
                mHandler.removeMessages(KEYGUARD_DONE_DRAWING);
            } else if (mContext != null && ActivityManagerNative.isSystemReady()) {
                //If calling sendBroadcast() when ActivityManagerService is not yet "system ready",
                //a Java Exception will pops up.
                mContext.sendBroadcast(new Intent(Intent.ACTION_SEND), "com.android.internal.policy.impl.KeyguardViewMediator.DONE_DRAW");
            }
        }
    }

    private void playSounds(boolean locked) {
        // User feedback for keyguard.
        Log.d(TAG, "playSounds mSuppressNextLockSound ="+ mSuppressNextLockSound);

        if (mSuppressNextLockSound) {
            mSuppressNextLockSound = false;
            return;
        }

        final ContentResolver cr = mContext.getContentResolver();
        if (Settings.System.getInt(cr, Settings.System.LOCKSCREEN_SOUNDS_ENABLED, 1) == 1) {
            final int whichSound = locked
                ? mLockSoundId
                : mUnlockSoundId;
            mLockSounds.stop(mLockSoundStreamId);
            // Init mAudioManager
            if (mAudioManager == null) {
                mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
                if (mAudioManager == null) return;
                mMasterStreamType = mAudioManager.getMasterStreamType();
            }
            // If the stream is muted, don't play the sound
            if (mAudioManager.isStreamMute(mMasterStreamType)) return;

            mLockSoundStreamId = mLockSounds.play(whichSound,
                    mLockSoundVolume, mLockSoundVolume, 1/*priortiy*/, 0/*loop*/, 1.0f/*rate*/);
        }
    }

    private void updateActivityLockScreenState() {
        try {
            ActivityManagerNative.getDefault().setLockScreenShown(
                    mShowing && !mHidden);
        } catch (RemoteException e) {
        }
    }

    /**
     * Handle message sent by {@link #showLocked}.
     * @see #SHOW
     */
    private void handleShow(Bundle options) {
        synchronized (KeyguardViewMediator.this) {
            if (DEBUG) KeyguardUtils.xlogD(TAG, "handleShow enter");
            if (!mSystemReady) return;
            /// M: if already showing, just return
            if (mShowing) return;

            mKeyguardViewManager.show(options);
            
            if (DEBUG) KeyguardUtils.xlogD(TAG, "handleShow mKeyguardViewManager Show exit");
            
            mShowing = true;
            mKeyguardDonePending = false;
            updateActivityLockScreenState();
            adjustStatusBarLocked();
            userActivity();
            try {
                ActivityManagerNative.getDefault().closeSystemDialogs("lock");
            } catch (RemoteException e) {
            }

            if (DEBUG) KeyguardUtils.xlogD(TAG, "handleShow query AlarmBoot before");
            // Do this at the end to not slow down display of the keyguard.
            if (!KeyguardUpdateMonitor.isAlarmBoot()) {
                playSounds(true);
            } else {
                new Handler().postDelayed(new Runnable() {
                    public void run() {
                        sendRemoveIPOWinBroadcast();
                        startAlarm();
                    }
                }, 250);
            }
            mShowKeyguardWakeLock.release();
            if (DEBUG) KeyguardUtils.xlogD(TAG, "handleShow exit");
        }
    }

    /**
     * Handle message sent by {@link #hideLocked()}
     * @see #HIDE
     */
    private void handleHide() {
        synchronized (KeyguardViewMediator.this) {
            if (DEBUG) KeyguardUtils.xlogD(TAG, "handleHide");
            if (mWakeAndHandOff.isHeld()) {
                KeyguardUtils.xlogD(TAG, "attempt to hide the keyguard while waking, ignored");
                return;
            }
            if (!mShowing) return;

            // only play "unlock" noises if not on a call (since the incall UI
            // disables the keyguard)
            if (TelephonyManager.EXTRA_STATE_IDLE.equals(mPhoneState)
                    && !KeyguardUpdateMonitor.isAlarmBoot() && mScreenOn) {
                playSounds(false);
            }

            mKeyguardViewManager.hide();
            mShowing = false;
            mKeyguardDonePending = false;
            updateActivityLockScreenState();
            adjustStatusBarLocked();
        }
    }

    /// M: Optimization, Avoid frequently call LockPatternUtils.isSecure
    /// whihc is very time consuming
    private void adjustStatusBarLocked() {
        if (mStatusBarManager == null) {
            mStatusBarManager = (StatusBarManager)
                    mContext.getSystemService(Context.STATUS_BAR_SERVICE);
        }
        if (mStatusBarManager == null) {
            KeyguardUtils.xlogD(TAG, "Could not get status bar manager");
        } else {
            /// M: Optimization, save isSecure()'s result instead of calling it 3 times in a single function, 
            /// Do not worry the case we save isSecure() result while sim pin/puk state change, that change will
            /// cause KeyguardViewMediator reset@{
            boolean isSecure = isSecure();
            /// @}
            if (mShowLockIcon) {
                // Give feedback to user when secure keyguard is active and engaged
                if (mShowing && isSecure) {
                    if (!mShowingLockIcon) {
                        String contentDescription = mContext.getString(
                                com.android.internal.R.string.status_bar_device_locked);
                        mStatusBarManager.setIcon("secure",
                                com.android.internal.R.drawable.stat_sys_secure, 0,
                                contentDescription);
                        mShowingLockIcon = true;
                    }
                } else {
                    if (mShowingLockIcon) {
                        mStatusBarManager.removeIcon("secure");
                        mShowingLockIcon = false;
                    }
                }
            }

            // Disable aspects of the system/status/navigation bars that must not be re-enabled by
            // windows that appear on top, ever
            int flags = StatusBarManager.DISABLE_NONE;
            /// M: [ALPS00581575]  Adjust bars when keyguard is showing and not force hidden.
            if (isShowingAndNotHidden()) {
                // Permanently disable components not available when keyguard is enabled
                // (like recents). Temporary enable/disable (e.g. the "back" button) are
                // done in KeyguardHostView.
                flags |= StatusBarManager.DISABLE_RECENT;
                if (isSecure || !ENABLE_INSECURE_STATUS_BAR_EXPAND) {
                    // showing secure lockscreen; disable expanding.
                    flags |= StatusBarManager.DISABLE_EXPAND;
                }
                if (isSecure) {
                    // showing secure lockscreen; disable ticker.
                    flags |= StatusBarManager.DISABLE_NOTIFICATION_TICKER;
                }
                /// M: [ALPS00604438] Disable search view for alarm boot 
                if (!isAssistantAvailable() || KeyguardUpdateMonitor.isAlarmBoot()) {
                    flags |= StatusBarManager.DISABLE_SEARCH;
                }
            }

            if (DEBUG) {
                Log.d(TAG, "adjustStatusBarLocked: mShowing=" + mShowing + " mHidden=" + mHidden
                        + " isSecure=" + isSecure + " --> flags=0x" + Integer.toHexString(flags));
            }

            if (!(mContext instanceof Activity)) {
                mStatusBarManager.disable(flags);
            }
        }
    }

    /**
     * Handle message sent by {@link #wakeWhenReady(int)}
     * M: add parameter to identify the message ID
     * @param keyCode The key that woke the device.
     * @see #WAKE_WHEN_READY
     */
    private void handleWakeWhenReady(int keyCode, int wakeMSGId) {
        if (DBG_WAKE) KeyguardUtils.xlogD(TAG, ">>>handleWakeWhenReady(" + keyCode + ") Before--synchronized (KeyguardViewMediator.this) wakeMSGId = " + wakeMSGId); /// M:
        synchronized (KeyguardViewMediator.this) {
            if (DBG_WAKE) KeyguardUtils.xlogD(TAG, "handleWakeWhenReady(" + keyCode + ") wakeMSGId = " + wakeMSGId);

            // this should result in a call to 'poke wakelock' which will set a timeout
            // on releasing the wakelock
            if (!mKeyguardViewManager.wakeWhenReadyTq(keyCode)) {
                // poke wakelock ourselves if keyguard is no longer active
                KeyguardUtils.xlogD(TAG, "mKeyguardViewManager.wakeWhenReadyTq did not poke wake lock, so poke it ourselves");
                userActivity();
            }

            /**
             * Now that the keyguard is ready and has poked the wake lock, we can
             * release the handoff wakelock
             */
            mWakeAndHandOff.release();
            if (DBG_WAKE) KeyguardUtils.xlogD(TAG, "<<<handleWakeWhenReady(" + keyCode + ") wakeMSGId = " + wakeMSGId);
        }
    }

    /**
     * Handle message sent by {@link #resetStateLocked(Bundle)}
     * @see #RESET
     */
    private void handleReset(Bundle options) {
        synchronized (KeyguardViewMediator.this) {
            if (DEBUG) KeyguardUtils.xlogD(TAG, "handleReset");
            mKeyguardViewManager.reset(options);
            /// M: also adjust statusbar
            adjustStatusBarLocked();
        }
    }

    /**
     * Handle message sent by {@link #verifyUnlock}
     * @see #VERIFY_UNLOCK
     */
    private void handleVerifyUnlock() {
        synchronized (KeyguardViewMediator.this) {
            if (DEBUG) KeyguardUtils.xlogD(TAG, "handleVerifyUnlock");
            mKeyguardViewManager.verifyUnlock();
            mShowing = true;
            updateActivityLockScreenState();
        }
    }

    /**
     * Handle message sent by {@link #notifyScreenOffLocked()}
     * @see #NOTIFY_SCREEN_OFF
     */
    private void handleNotifyScreenOff() {
        synchronized (KeyguardViewMediator.this) {
            if (DEBUG) KeyguardUtils.xlogD(TAG, "handleNotifyScreenOff");
            mKeyguardViewManager.onScreenTurnedOff();
        }
    }

    /**
     * Handle message sent by {@link #notifyScreenOnLocked()}
     * @see #NOTIFY_SCREEN_ON
     */
    private void handleNotifyScreenOn(KeyguardViewManager.ShowListener showListener) {
        synchronized (KeyguardViewMediator.this) {
            if (DEBUG) KeyguardUtils.xlogD(TAG, "handleNotifyScreenOn");
            mKeyguardViewManager.onScreenTurnedOn(showListener);
        }
    }

    public boolean isDismissable() {
        return mKeyguardDonePending || !isSecure();
    }

    /**
     * M: power-off alarm @{
     */
    private void sendRemoveIPOWinBroadcast() {
        Log.w(TAG, "sendRemoveIPOWinBroadcast ... ");
        Intent in = new Intent(REMOVE_IPOWIN);
        mContext.sendBroadcast(in);
    }
    /** @} */

    /**
     * M: power-off alarm @{
     */
    private void handleAlarmBoot() {
        if (mShowing) {
            handleHide();
        }
        if (mIsIPOBoot) {
            mIsIPOBoot = false;
        }
        showLocked(null);
    }

    private void startAlarm() {
        Intent in = new Intent("com.android.deskclock.ALARM_ALERT");
        in.putExtra("isAlarmBoot", true);
        mContext.startService(in);
    }
    /**  @}*/
    public void showAssistant() {
        Message msg = mHandler.obtainMessage(SHOW_ASSISTANT);
        mHandler.sendMessage(msg);
    }

    public void handleShowAssistant() {
        mKeyguardViewManager.showAssistant();
    }

    private boolean isAssistantAvailable() {
        return mSearchManager != null
                && mSearchManager.getAssistIntent(mContext, UserHandle.USER_CURRENT) != null;
    }

    /********************************************************
     ** Mediatek add begin
     ********************************************************/


    private static final int KEYGUARD_RELAYOUT = 1000;

  /// M: For DM lock feature to update keyguard
    private static final int MSG_DM_KEYGUARD_UPDATE = 1001;
    
    /// M: DM lock update flag, used in reset
    public static final String RESET_FOR_DM_LOCK = "dmlock_reset";

    /// M: OMADM INTENT to lock and unlock device @{
    public static final String OMADM_LAWMO_LOCK = "com.mediatek.dm.LAWMO_LOCK";
    public static final String OMADM_LAWMO_UNLOCK = "com.mediatek.dm.LAWMO_UNLOCK";
    /// @}
    
    /**
     * Handle message sent by {@link #onDMKeyguardUpdate()}
     * @see #KEYGUARD_RELAYOUT
     */
    private void handleKeyguardReLayout(boolean dmLock) {
        synchronized (KeyguardViewMediator.this) {
            if (DEBUG) {
                KeyguardUtils.xlogD(TAG, "handleKeyguardReLayout dmLock=" + dmLock);
            }
            mKeyguardViewManager.reLayoutScreen(dmLock);
        }
    }

    public void setDebugFilterStatus(boolean debugflag){
        //DEBUG = debugflag;
        //mKeyguardViewManager.setDebugFilterStatus(debugflag);
        //mUpdateMonitor.setDebugFilterStatus(debugflag);
    }

    public boolean dmCheckLocked() {
        return mUpdateMonitor.dmIsLocked();
    }

    // Used for PowerManagerService.java start
    public static boolean isKeyguardNotShowing() {
        if (DEBUG) {
            KeyguardUtils.xlogD(TAG, " mExternallyEnabled=" + mExternallyEnabled + " mShowing=" + mShowing);
        }
        return (mExternallyEnabled && !mShowing);
    }

    public static boolean isKeyguardEnabled() {
        return mExternallyEnabled;
    }

    //added by Cui Zhang, used for PowerManagerService.java
    public static boolean isKeyguardShowingAndNotHidden() {
        return mShowing && !mHidden;
    }
    // Used for PowerManagerService.java end
    /**
     * M: used for power-off alarm
     * @return
     */
    public boolean isAlarmUnlockScreen() {
        return mKeyguardViewManager.isAlarmUnlockScreen();
    }

    ///M: The real show dialog callback @{
    private class InvalidDialogCallback implements
            KeyguardUpdateMonitor.DialogShowCallBack {
        public void show() {
            String title = mContext
                    .getString(com.mediatek.internal.R.string.invalid_sim_title);
            String message = mContext
                    .getString(com.mediatek.internal.R.string.invalid_sim_message);
            AlertDialog dialog = createDialog(title, message);
            dialog.show();
        }
    }

    private class MeLockedDialogCallback implements
            KeyguardUpdateMonitor.DialogShowCallBack {
        public void show() {
            String title = null;
            String message = mContext.getString(com.mediatek.internal.R.string.simlock_slot_locked_message);
            AlertDialog dialog = createDialog(title, message);
            dialog.show();
        }
    }
    ///@}

    /**
     *  M: For showing invalid sim card dilaog if user insert a perm_disabled sim card @{
     * @param title The invalid sim card alert dialog title. 
     * @param message THe invalid sim catd alert dialog content.
     */
    private AlertDialog createDialog(String title, String message) {
        final AlertDialog dialog  =  new AlertDialog.Builder(mContext)
            .setTitle(title)
            .setIcon(R.drawable.ic_dialog_alert)
            .setCancelable(false)
            .setMessage(message)
            .setNegativeButton(R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which){
                    mUpdateMonitor.reportDialogClose();
                    KeyguardUtils.xlogD(TAG, "invalid sim card ,reportCloseDialog");
                    
                }
            } )
            .create();
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG);
        return dialog;
    }
    /**  @}*/
    
    /**
     * Handle {@link #MSG_DM_KEYGUARD_UPDATE}
     */
    private void handleDMKeyguardUpdate(boolean lock) {
        Log.d(TAG, "handleDMKeyguardUpdate lock=" + lock);
        if (!isShowing()) {
            if (DEBUG) KeyguardUtils.xlogD(TAG, "INTENT_VALUE_ICC_LOCKED and keygaurd isn't "
                    + "showing; need to show keyguard so user can enter sim pin");
            doKeyguardLocked();
        } else {
            /// M: If dm lock cause reset, tell KeyguardViewManager to force reset
            Bundle option = new Bundle();
            option.putBoolean(RESET_FOR_DM_LOCK, true);
            resetStateLocked(option);
        }
        handleDisableSystemUIForDM(lock);
    }
    
    /// M: Hide systemui when dm lock is on
    private void handleDisableSystemUIForDM(boolean lock) {
        Message msg = mHandler.obtainMessage(KEYGUARD_RELAYOUT);
        msg.arg1 = lock ? 1 : 0;
        mHandler.sendMessage(msg);
    }
    /// M: add for power off alarm, IPO INTENT @{
    public static final String IPO_DISABLE = "android.intent.action.ACTION_BOOT_IPO";
    public static boolean mHideNavBar = false;
    private static final int ALARM_BOOT = 115;
    private static final int RESHOW_ANYWHERE = 116;
    private static final String NORMAL_BOOT_ACTION = "android.intent.action.normal.boot";
    private static final String NORMAL_BOOT_DONE_ACTION = "android.intent.action.normal.boot.done";
    private static final String LAUNCH_PWROFF_ALARM = "android.intent.action.LAUNCH_POWEROFF_ALARM";
    private static final String NORMAL_SHUTDOWN_ACTION = "android.intent.action.normal.shutdown";
    static final String REMOVE_IPOWIN = "alarm.boot.remove.ipowin";
    /// M: fix 441605, play sound after power off
    private static final String PRE_SHUTDOWN = "android.intent.action.ACTION_PRE_SHUTDOWN";
    /// @}
}
