package com.android.internal.policy.impl.keyguard;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.media.AudioSystem;
import android.os.PowerManager;
import android.telephony.TelephonyManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.android.internal.R;
import com.android.internal.widget.LockPatternUtils;

import com.mediatek.xlog.Xlog;

public class KeyguardVoiceUnlockView extends LinearLayout implements KeyguardSecurityView {

    private static final String TAG = "VoiceUnlock";
    private static final boolean DEBUG = true;
    
    private KeyguardSecurityCallback mKeyguardSecurityCallback;
    private LockPatternUtils mLockPatternUtils;
    private BiometricSensorUnlock mBiometricUnlock;
    private View mVoiceUnlockAreaView;
    private SecurityMessageDisplay mSecurityMessageDisplay;
    private View mEcaView;
    private Drawable mBouncerFrame;

    private boolean mIsShowing = false;
    private final Object mIsShowingLock = new Object();

    public KeyguardVoiceUnlockView(Context context) {
        this(context, null);
    }

    public KeyguardVoiceUnlockView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        initializeBiometricUnlockView();

        mSecurityMessageDisplay = new KeyguardMessageArea.Helper(this);
        mEcaView = findViewById(R.id.keyguard_selector_fade_container);
        View bouncerFrameView = findViewById(R.id.keyguard_bouncer_frame);
        if (bouncerFrameView != null) {
            mBouncerFrame = bouncerFrameView.getBackground();
        }
    }

    @Override
    public void setKeyguardCallback(KeyguardSecurityCallback callback) {
        mKeyguardSecurityCallback = callback;
        // TODO: formalize this in the intervoice or factor it out
        ((VoiceUnlock)mBiometricUnlock).setKeyguardCallback(callback);
    }

    @Override
    public void setLockPatternUtils(LockPatternUtils utils) {
        mLockPatternUtils = utils;
    }

    @Override
    public void reset() {

    }

    @Override
    public void onDetachedFromWindow() {
        log("onDetachedFromWindow()");
        if (mBiometricUnlock != null) {
            mBiometricUnlock.stop();
        }
        KeyguardUpdateMonitor.getInstance(mContext).removeCallback(mUpdateCallback);
    }

    @Override
    public void onPause() {
        log("onPause()");
        if (mBiometricUnlock != null) {
            mBiometricUnlock.stop();
        }
        KeyguardUpdateMonitor.getInstance(mContext).removeCallback(mUpdateCallback);
    }

    @Override
    public void onResume(int reason) {
        log("onResume()");
        mIsShowing = KeyguardUpdateMonitor.getInstance(mContext).isKeyguardVisible();
        maybeStartBiometricUnlock();
        KeyguardUpdateMonitor.getInstance(mContext).registerCallback(mUpdateCallback);
    }

    @Override
    public boolean needsInput() {
        return false;
    }

    @Override
    public KeyguardSecurityCallback getCallback() {
        return mKeyguardSecurityCallback;
    }

    private void initializeBiometricUnlockView() {
        log("initializeBiometricUnlockView()");
        mVoiceUnlockAreaView = findViewById(R.id.zz_voice_unlock_area_view);
        if (mVoiceUnlockAreaView != null) {
            mBiometricUnlock = new VoiceUnlock(mContext, this);
            mBiometricUnlock.initializeView(mVoiceUnlockAreaView);
        } else {
            log("Couldn't find biometric unlock view");
        }
    }

    /**
     * Starts the biometric unlock if it should be started based on a number of factors.  If it
     * should not be started, it either goes to the back up, or remains showing to prepare for
     * it being started later.
     */
    private void maybeStartBiometricUnlock() {
        log("maybeStartBiometricUnlock()");
        if (mBiometricUnlock != null) {
            KeyguardUpdateMonitor monitor = KeyguardUpdateMonitor.getInstance(mContext);
            final boolean backupIsTimedOut = (
                    monitor.getFailedUnlockAttempts() >=
                    LockPatternUtils.FAILED_ATTEMPTS_BEFORE_TIMEOUT);
            final boolean mediaPlaying = AudioSystem.isStreamActive(AudioSystem.STREAM_MUSIC, 0) 
                    || AudioSystem.isStreamActive(AudioSystem.STREAM_FM, 0);
            PowerManager powerManager = (PowerManager) mContext.getSystemService(
                    Context.POWER_SERVICE);

            boolean isShowing;
            synchronized(mIsShowingLock) {
                isShowing = mIsShowing;
            }

            // Don't start it if the screen is off or if it's not showing, but keep this view up
            // because we want it here and ready for when the screen turns on or when it does start
            // showing.
            if (!powerManager.isScreenOn() || !isShowing) {
                mBiometricUnlock.stop(); // It shouldn't be running but calling this can't hurt.
                return;
            }

            // TODO: Some of these conditions are handled in KeyguardSecurityModel and may not be
            // necessary here.
            if (monitor.getPhoneState() == TelephonyManager.CALL_STATE_IDLE
                    && !monitor.getMaxBiometricUnlockAttemptsReached()
                    && !backupIsTimedOut
                    && !mediaPlaying) {
                mBiometricUnlock.start();
            } else {
                mBiometricUnlock.stopAndShowBackup();
            }
        }
    }

    KeyguardUpdateMonitorCallback mUpdateCallback = new KeyguardUpdateMonitorCallback() {
        // We need to stop the biometric unlock when a phone call comes in
        @Override
        public void onPhoneStateChanged(int phoneState) {
            log("onPhoneStateChanged(" + phoneState + ")");
            if (phoneState == TelephonyManager.CALL_STATE_RINGING) {
                if (mBiometricUnlock != null) {
                    mBiometricUnlock.stopAndShowBackup();
                }
            }
        }

        @Override
        public void onUserSwitched(int userId) {
            log("onUserSwitched(" + userId + ")");
            if (mBiometricUnlock != null) {
                mBiometricUnlock.stop();
            }
            // No longer required; static value set by KeyguardViewMediator
            // mLockPatternUtils.setCurrentUser(userId);
        }

        @Override
        public void onKeyguardVisibilityChanged(boolean showing) {
            log("onKeyguardVisibilityChanged(" + showing + ")");
            boolean wasShowing = false;
            synchronized(mIsShowingLock) {
                wasShowing = mIsShowing;
                mIsShowing = showing;
            }
            PowerManager powerManager = (PowerManager) mContext.getSystemService(
                    Context.POWER_SERVICE);
            if (mBiometricUnlock != null) {
                if (!showing && wasShowing) {
                    mBiometricUnlock.stop();
                } else if (showing && powerManager.isScreenOn() && !wasShowing) {
                    maybeStartBiometricUnlock();
                }
            }
        }
    };

    @Override
    public void showUsabilityHint() {
    }

    @Override
    public void showBouncer(int duration) {
        KeyguardSecurityViewHelper.
                showBouncer(mSecurityMessageDisplay, mEcaView, mBouncerFrame, duration);
    }

    @Override
    public void hideBouncer(int duration) {
        KeyguardSecurityViewHelper.
                hideBouncer(mSecurityMessageDisplay, mEcaView, mBouncerFrame, duration);
    }
    
    private void log(String msg) {
        if (DEBUG) {
            Xlog.d(TAG, "KeyguardVoiceUnlockView: " + msg);
        }
    }

}
