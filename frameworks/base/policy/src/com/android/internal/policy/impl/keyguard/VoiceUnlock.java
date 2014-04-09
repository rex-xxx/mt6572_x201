package com.android.internal.policy.impl.keyguard;

import com.mediatek.internal.R;

import android.app.ActivityManagerNative;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.mediatek.common.voicecommand.VoiceCommandListener;
import com.mediatek.common.voicecommand.IVoiceCommandManager;

import com.mediatek.xlog.Xlog;

import com.android.internal.widget.LockPatternUtils;

public class VoiceUnlock implements BiometricSensorUnlock, Handler.Callback, KeyguardHostView.OnDismissAction {
    private static final boolean DEBUG = true;
    private static final String TAG = "VoiceUnlock";

    private final Context mContext;
    private final LockPatternUtils mLockPatternUtils;

    KeyguardSecurityCallback mKeyguardScreenCallback;
    
    private Handler mHandler;
    private Handler mIntensityHandler;
    private Runnable mIntensityRunnable;
    private Handler mHideHandler;
    private Runnable mHideRunnable;
    private static final int MSG_SHOW_RECOGNIZE_READY = 0;
    private static final int MSG_RECOGNIZE_PASS = 1;
    private static final int MSG_RECOGNIZE_FAIL = 2;
    private static final int MSG_SERVICE_ERROR = 3;
    private static final int MSG_UPDATE_INTENSITY = 4;
    private static final int MSG_CANCEL = 5;

    private static final int FAIL_TYPE_SERVICE_ERROR = 0;
    private static final int FAIL_TYPE_PASSWORD_WRONG = 1;
    private static final int FAIL_TYPE_NOISY = 2;
    private static final int FAIL_TYPE_WEAK = 3;
    
    private static final String KEY_VOICE_COMMAND1 = "voice_command1";
    private static final String KEY_VOICE_COMMAND2 = "voice_command2";
    private static final String KEY_VOICE_COMMAND3 = "voice_command3";
    
    private static final String SOUND_PATH = "system/media/audio/notifications/Tejat.ogg";
    private static final int SOUND_ATTENUATION = -6;
    
    // So the user has a consistent amount of time when brought to the backup method from Voice
    // Unlock
    private final int BACKUP_LOCK_TIMEOUT = 5000;
    
    private final long TIMEOUT_AFTER_UNLOCK_FAIL = 3000;
    
    private static final int INTENSITY_ANIMATION_INTERVAL = 90;
    
    // TODO: This was added for the purpose of adhering to what the biometric interface expects
    // the isRunning() function to return.  However, it is probably not necessary to have both
    // mRunning and mServiceRunning.  I'd just rather wait to change that logic.
    private volatile boolean mIsRunning = false;
    
    private boolean mBoundToService = false;

    
    private View mVoiceUnlockView;
    private ImageView mWave;
    private ImageView mCancel;
    private AnimationDrawable mWaveAnim;
    
    private IVoiceCommandManager mVoiceCommandManager;
    private VoiceCommandListener mVoiceCommandListener;
    
    private View mUnlockView;
    private SecurityMessageDisplay mSecurityMessageDisplay;
    
    private String mLaunchApp;
    /**
     * Stores some of the structures that Voice Unlock will need to access and creates the handler
     * will be used to execute messages on the UI thread.
     */
    public VoiceUnlock(Context context) {
        this(context, null);
    }
    
    public VoiceUnlock(Context context, View unlockView) {
        
        mContext = context;
        mLockPatternUtils = new LockPatternUtils(context);
        mUnlockView = unlockView;
        if (unlockView != null) {
            mSecurityMessageDisplay = new KeyguardMessageArea.Helper(unlockView);
        }
        mHandler = new Handler(this);

        mVoiceCommandManager = (IVoiceCommandManager)context.getSystemService("voicecommand");
        mVoiceCommandListener = new VoiceCommandListener(mContext) {
            @Override
            public void onVoiceCommandNotified(int mainAction, int subAction, Bundle extraData) {
                int result = extraData.getInt(VoiceCommandListener.ACTION_EXTRA_RESULT);
                log("onNotified result=" + result + " mainAction = " + mainAction + " subAction = " + subAction);
                if (result == VoiceCommandListener.ACTION_EXTRA_RESULT_SUCCESS) {
                    switch (subAction) {
                        case VoiceCommandListener.ACTION_VOICE_RECOGNIZE_START:
                            log("onNotified RECOGNIZE_START");
                            mHandler.obtainMessage(MSG_SHOW_RECOGNIZE_READY).sendToTarget();
                            break;
                        case VoiceCommandListener.ACTION_VOICE_RECOGNIZE_NOTIFY:
                            int verifyResult = extraData.getInt(VoiceCommandListener.ACTION_EXTRA_RESULT_INFO);
                            log("onNotified RECOGNIZE_NOTIFY verifyResult = " + verifyResult);
                            if (verifyResult == 1) {
                                int commandId = extraData.getInt(VoiceCommandListener.ACTION_EXTRA_RESULT_INFO1);
                                log("onNotified RECOGNIZE_NOTIFY commandId = " + commandId);
                                mHandler.obtainMessage(MSG_RECOGNIZE_PASS, commandId, 0).sendToTarget();
                            } else {
                                mHandler.obtainMessage(MSG_RECOGNIZE_FAIL, verifyResult, 0).sendToTarget();
                            }
                            break;
                        case VoiceCommandListener.ACTION_VOICE_RECOGNIZE_INTENSITY:
                            int intensity = extraData.getInt(ACTION_EXTRA_RESULT_INFO);
                            log("onNotified RECOGNIZE_INTENSITY intensity = " + intensity);
                            mHandler.removeMessages(MSG_UPDATE_INTENSITY);
                            mHandler.obtainMessage(MSG_UPDATE_INTENSITY, intensity, 0).sendToTarget();
                            break;
                        default:
                            break;
                    }
                } else if (result == VoiceCommandListener.ACTION_EXTRA_RESULT_ERROR) {
                    mHandler.obtainMessage(MSG_SERVICE_ERROR).sendToTarget();
                }
            }
        };
        
        mIntensityHandler = new Handler();
        mIntensityRunnable = new Runnable() {
            @Override
            public void run() {
                if (mVoiceCommandManager != null) {
                    try {
                        log("sendCommand RECOGNIZE_INTENSITY");
                        mVoiceCommandManager.sendCommand(mContext,
                                VoiceCommandListener.ACTION_MAIN_VOICE_RECOGNIZE,
                                VoiceCommandListener.ACTION_VOICE_RECOGNIZE_INTENSITY, null);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
                mIntensityHandler.postDelayed(this, INTENSITY_ANIMATION_INTERVAL);
            }
        };

        mHideHandler = new Handler();
        mHideRunnable = new Runnable() {
            public void run() {
                stop();
                pokeWakelock(BACKUP_LOCK_TIMEOUT);
            }
        };
    }
    
    @Override
    public void initializeView(View voiceUnlockView) {
        log("initializeView()");
        mVoiceUnlockView = voiceUnlockView;
        mWave = (ImageView) voiceUnlockView.findViewById(com.android.internal.R.id.zz_voiceLockWave);
        mWave.setBackgroundResource(R.drawable.voice_wave_anim);
        mWave.setImageResource(R.drawable.voice_wave);
        mWave.setImageLevel(0);
        mCancel = (ImageView) voiceUnlockView.findViewById(com.android.internal.R.id.zz_voiceLockCancel);
        mCancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mHandler.obtainMessage(MSG_CANCEL).sendToTarget();
            }
        });
    }

    @Override
    public boolean isRunning() {
        return mIsRunning;
    }

    
    public void startUi() {
        log("startUi()");
        if (mHandler.getLooper() != Looper.myLooper()) {
            log("startUi() called off of the UI thread");
        }

        if (mVoiceUnlockView != null) {
            mVoiceUnlockView.setVisibility(View.VISIBLE);
        }
    }
    
    public void setKeyguardCallback(KeyguardSecurityCallback keyguardScreenCallback) {
        mKeyguardScreenCallback = keyguardScreenCallback;
    }
    
    private void handleVoiceCommandFail(int type) {
        switch (type) {
        case FAIL_TYPE_SERVICE_ERROR:
            mSecurityMessageDisplay.setMessage(R.string.voice_unlock_service_error, true);
            break;
        case FAIL_TYPE_PASSWORD_WRONG:
            mSecurityMessageDisplay.setMessage(R.string.voice_unlock_password_wrong, true);
            reportFailedBiometricUnlockAttempt();
            break;
        case FAIL_TYPE_NOISY:
            mSecurityMessageDisplay.setMessage(R.string.voice_unlock_noisy, true);
            reportFailedBiometricUnlockAttempt();
            break;
        case FAIL_TYPE_WEAK:
            mSecurityMessageDisplay.setMessage(R.string.voice_unlock_weak, true);
            reportFailedBiometricUnlockAttempt();
            break;
        default:
            break;
        }
        mHandler.sendEmptyMessageDelayed(MSG_CANCEL, TIMEOUT_AFTER_UNLOCK_FAIL);
    }
    
    private void handleUpdateIntensity(int intensity) {
        log("updateIntensity intensity = " + intensity);
        intensity -= 200;  //we don't want voice wave too sensitive
        if (intensity < 128) {
            mWave.setImageLevel(0);
        } else if (intensity < 256) {
            mWave.setImageLevel(1);
        } else if (intensity < 512) {
            mWave.setImageLevel(2);
        } else if (intensity < 1024) {
            mWave.setImageLevel(3);
        } else if (intensity < 2048) {
            mWave.setImageLevel(4);
        }
    }
    
    private void reportFailedBiometricUnlockAttempt() {
        if (DEBUG) Log.d(TAG, "handleReportFailedAttempt()");
        // We are going to the backup method, so we don't want to see Voice Unlock again until the
        // next time the user visits keyguard.
        KeyguardUpdateMonitor.getInstance(mContext).setAlternateUnlockEnabled(false);

        mKeyguardScreenCallback.reportFailedUnlockAttempt();
        
        mHideHandler.postDelayed(mHideRunnable, KeyguardMessageArea.SECURITY_MESSAGE_DURATION);
    }
    
    private void handleVoiceServiceReady() {
        pokeWakelock(10000);
//        startUi();
        mWaveAnim = (AnimationDrawable) mWave.getBackground();
        if (mWaveAnim.isRunning()) {
            mWaveAnim.stop();
        }
        mWaveAnim.start();
        int durationTime = 0;
        for (int i = 0; i < mWaveAnim.getNumberOfFrames(); i++) {
            durationTime += mWaveAnim.getDuration(i);
        }
        mIntensityHandler.postDelayed(mIntensityRunnable, durationTime);
    }
    
    private void handleVoiceCommandPass(int commandId) {
        stop();
        switch (commandId) {
        case 1:
            mLaunchApp = Settings.System.getString(mContext.getContentResolver(), Settings.System.VOICE_UNLOCK_AND_LAUNCH1);
            break;
        case 2:
            mLaunchApp = Settings.System.getString(mContext.getContentResolver(), Settings.System.VOICE_UNLOCK_AND_LAUNCH2);
            break;
        case 3:
            mLaunchApp = Settings.System.getString(mContext.getContentResolver(), Settings.System.VOICE_UNLOCK_AND_LAUNCH3);
            break;
        default:
            mLaunchApp = null;
            break;
        }
        log("handleVoiceCommandPass commandId = " + commandId + " appName = " + mLaunchApp);

        if (!mKeyguardScreenCallback.hasOnDismissAction()) {
            //If already have dismiss action that set by add widget,
            //don't add voice unlock dismiss action.
            log("onDismissAction is null, set voice unlock dismiss action");
            mKeyguardScreenCallback.setOnDismissAction(this);
        }
        mKeyguardScreenCallback.reportSuccessfulUnlockAttempt();
        mKeyguardScreenCallback.dismiss(true);
        pokeWakelock(10000);
    }
    
    @Override
    public boolean onDismiss() {
        if (mLaunchApp == null) {
            return false;
        } else {
            ComponentName cn = ComponentName.unflattenFromString(mLaunchApp);
            log("onDismiss cn = " + cn.toString());
            Intent intent = new Intent();
            intent.setComponent(cn);
            intent.setAction(Intent.ACTION_MAIN);
            intent.setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            try {
                ActivityManagerNative.getDefault().dismissKeyguardOnNextActivity();
            } catch (RemoteException e) {
                log("can't dismiss keyguard on launch");
            }
            try {
                mContext.startActivityAsUser(intent, new UserHandle(mLockPatternUtils.getCurrentUser()));
                log("startActivity intent = " + intent.toString());
            } catch (ActivityNotFoundException e) {
                log("Activity not found for intent + " + intent.getAction());
                return false;
            }
        }
        return true;
    }

    public void pokeWakelock(int millis) {
        PowerManager powerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        if (powerManager.isScreenOn()) {
          mKeyguardScreenCallback.userActivity(millis);
        }
    }
    
    public void stopUi() {
        log("stopUi()");
        if (mVoiceUnlockView != null) {
            mVoiceUnlockView.setVisibility(View.INVISIBLE);
        } else {
            log("mVoiceUnlockView is null in stopUi()");
        }
    }

    @Override
    public boolean start() {
        log("start() mBoundToService = " + mBoundToService);
        if (mHandler.getLooper() != Looper.myLooper()) {
            log("start() called off of the UI thread");
        }

        if (mIsRunning) {
            log("start() called when already running");
        }
        
        mHideHandler.removeCallbacks(mHideRunnable);
        
        startUi();
        if (!mBoundToService) {
            if (mVoiceCommandManager != null) {
                try {
                    log("register to service");
                    mVoiceCommandManager.registerListener(mVoiceCommandListener);
                    log("sendCommand RECOGNIZE_START");
                    mVoiceCommandManager.sendCommand(mContext, VoiceCommandListener.ACTION_MAIN_VOICE_RECOGNIZE,
                            VoiceCommandListener.ACTION_VOICE_RECOGNIZE_START, null);
                    mBoundToService = true;
                } catch (RemoteException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        
        log("start() mIsRunning = true");
        mIsRunning = true;
        return true;
    }

    @Override
    public boolean stop() {
        log("stop() mBoundToService = " + mBoundToService);
        if (mHandler.getLooper() != Looper.myLooper()) {
            log("stop() called off of the UI thread");
        }
        
        mIntensityHandler.removeCallbacks(mIntensityRunnable);

        boolean mWasRunning = mIsRunning;
        stopUi();

        if (mBoundToService) {
            if (mVoiceCommandManager != null) {
                try {
                    log("unregister to service");
                    mVoiceCommandManager.unRegisterListener(mVoiceCommandListener);
                } catch (RemoteException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            mBoundToService = false;
        } else {
            log("Attempt to unRegisterListener from Voice Unlock when not bound");
        }
          
        log("stop() mIsRunning  = false");
        mIsRunning = false;
        return mWasRunning;
    }

    @Override
    public void cleanUp() {
        log("cleanUp()");
        mVoiceCommandManager = null;
        mVoiceCommandListener = null;
    }

    @Override
    public int getQuality() {
        return DevicePolicyManager.PASSWORD_QUALITY_VOICE_WEAK;
    }
    
    public void stopAndShowBackup() {
        if (DEBUG) Log.d(TAG, "stopAndShowBackup()");
        mHandler.obtainMessage(MSG_CANCEL).sendToTarget();
    }
    
    /**
     * Stops the Voice Unlock service and goes to the backup lock.
     */
    private void handleCancel() {
        log("handleCancel()");
        // We are going to the backup method, so we don't want to see Voice Unlock again until the
        // next time the user visits keyguard.
        KeyguardUpdateMonitor.getInstance(mContext).setAlternateUnlockEnabled(false);

        mKeyguardScreenCallback.showBackupSecurity();
        stop();
        mKeyguardScreenCallback.userActivity(BACKUP_LOCK_TIMEOUT);
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
        case MSG_SHOW_RECOGNIZE_READY:
            handleVoiceServiceReady();
            break;
        case MSG_RECOGNIZE_PASS:
            handleVoiceCommandPass(msg.arg1);
            break;
        case MSG_RECOGNIZE_FAIL:
           switch (msg.arg1) {
            case 0:
            case 100:
                handleVoiceCommandFail(FAIL_TYPE_PASSWORD_WRONG);
                break;
            case 2:
                handleVoiceCommandFail(FAIL_TYPE_NOISY);
                break;
            case 3:
                handleVoiceCommandFail(FAIL_TYPE_WEAK);
                break;
            default:
                break;
            }
            break;
        case MSG_SERVICE_ERROR:
            handleVoiceCommandFail(FAIL_TYPE_SERVICE_ERROR);
            break;
        case MSG_UPDATE_INTENSITY:
            handleUpdateIntensity(msg.arg1);
            break;
        case MSG_CANCEL:
            handleCancel();
            break;
        default:
            log("Unhandled message");
            return false;
        }
        return true;
    }
    
    private void log(String msg) {
        if (DEBUG) {
            Xlog.d(TAG, msg);
        }
    }
}
