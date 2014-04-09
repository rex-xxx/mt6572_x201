/*
 * Copyright (C) 2012 The Android Open Source Project
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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.IWindowManager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternView;
import com.android.internal.R;
import com.mediatek.common.voicecommand.VoiceCommandListener;
import com.mediatek.common.voicecommand.IVoiceCommandManager;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import com.mediatek.common.featureoption.FeatureOption;

public class PowerOffAlarmView extends RelativeLayout implements
        KeyguardSecurityView, MediatekGlowPadView.OnTriggerListener {
    private static final String TAG = "PowerOffAlarm";
    private static final boolean DEBUG = false;
    private final int DELAY_TIME_SECONDS = 7;
    private int mFailedPatternAttemptsSinceLastTimeout = 0;
    private int mTotalFailedPatternAttempts = 0;
    private LockPatternUtils mLockPatternUtils;
    private Button mForgotPatternButton;
    private TextView mVcTips, titleView = null;
    private LinearLayout mVcTipsContainer;
    private KeyguardSecurityCallback mCallback;
    private IVoiceCommandManager voiceCmdManager;
    private VoiceCommandListener voiceCmdListener;
    private boolean mEnableFallback;
    private boolean SUPPORT_VOICE_UI = FeatureOption.MTK_VOICE_UI_SUPPORT;
    private Context mContext;

    // These defaults must match the values in res/xml/settings.xml
    private static final String DEFAULT_SNOOZE = "10";
    private static final String DEFAULT_VOLUME_BEHAVIOR = "2";
    protected static final String SCREEN_OFF = "screen_off";

    protected Alarm mAlarm;
    private int mVolumeBehavior;
    boolean mFullscreenStyle;
    private MediatekGlowPadView mGlowPadView;
    private boolean mIsDocked = false;
    private AlarmManager mAm;
    private static final int UPDATE_LABEL = 99;
    // Parameters for the GlowPadView "ping" animation; see triggerPing().
    private static final int PING_MESSAGE_WHAT = 101;
    private static final boolean ENABLE_PING_AUTO_REPEAT = true;
    private static final long PING_AUTO_REPEAT_DELAY_MSEC = 1200;

    private boolean mPingEnabled = true;
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case PING_MESSAGE_WHAT:
                    triggerPing();
                    break;
                case UPDATE_LABEL:
                    if(titleView != null){
                        titleView.setText(msg.getData().getString("label"));
                    }
                    break;
            }
        }
    };

    public PowerOffAlarmView(Context context) {
        this(context, null);
    }

    public PowerOffAlarmView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        if (SUPPORT_VOICE_UI) {
            registerVoiceCmdService(context);
        }
    }

    public void setKeyguardCallback(KeyguardSecurityCallback callback) {
        mCallback = callback;
    }

    public void setLockPatternUtils(LockPatternUtils utils) {
        mLockPatternUtils = utils;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        Log.w(TAG, "onFinishInflate ... ");
        setKeepScreenOn(true);
        titleView = (TextView) findViewById(com.mediatek.internal.R.id.alertTitle);
        if(SUPPORT_VOICE_UI){
            mVcTips = (TextView) findViewById(com.mediatek.internal.R.id.tips);
            mVcTipsContainer = (LinearLayout) findViewById(com.mediatek.internal.R.id.tips_container);
        }
        mGlowPadView = (MediatekGlowPadView) findViewById(com.mediatek.internal.R.id.glow_pad_view);
        mGlowPadView.setOnTriggerListener(this);
        setFocusableInTouchMode(true);
        triggerPing();

        // Check the docking status , if the device is docked , do not limit rotation
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_DOCK_EVENT);
        Intent dockStatus = mContext.registerReceiver(null, ifilter);
        if (dockStatus != null) {
            mIsDocked = dockStatus.getIntExtra(Intent.EXTRA_DOCK_STATE, -1)
                    != Intent.EXTRA_DOCK_STATE_UNDOCKED;
        }

        // Register to get the alarm killed/snooze/dismiss intent.
        IntentFilter filter = new IntentFilter(Alarms.ALARM_KILLED);
        filter.addAction(Alarms.ALARM_SNOOZE_ACTION);
        filter.addAction(Alarms.ALARM_DISMISS_ACTION);
        filter.addAction(UPDATE_LABEL_ACTION);
        mContext.registerReceiver(mReceiver, filter);

        mLockPatternUtils = mLockPatternUtils == null ? new LockPatternUtils(
                mContext) : mLockPatternUtils;
        mAm = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        enableEventDispatching(true);
        if (SUPPORT_VOICE_UI && voiceCmdManager != null) {
            try {
                voiceCmdManager.registerListener(voiceCmdListener);
                Log.v(TAG, "register and send command ...... ");
                voiceCmdManager.sendCommand(mContext,
                        VoiceCommandListener.ACTION_MAIN_VOICE_UI,
                        VoiceCommandListener.ACTION_VOICE_UI_START, null);
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onTrigger(View v, int target) {
        final int resId = mGlowPadView.getResourceIdForTarget(target);
        switch (resId) {
            case com.mediatek.internal.R.drawable.ic_alarm_alert_snooze:
                snooze();
                break;

            case com.mediatek.internal.R.drawable.ic_alarm_alert_dismiss_pwroff:
                powerOff();
                break;

            case com.mediatek.internal.R.drawable.ic_alarm_alert_dismiss_pwron:
                powerOn();
                break;

            default:
                // Code should never reach here.
                Log.e(TAG, "Trigger detected on unhandled resource. Skipping.");
        }
    }

    private void triggerPing() {
        if (mPingEnabled) {
            mGlowPadView.ping();

            if (ENABLE_PING_AUTO_REPEAT) {
                mHandler.sendEmptyMessageDelayed(PING_MESSAGE_WHAT, PING_AUTO_REPEAT_DELAY_MSEC);
            }
        }
    }

    // Attempt to snooze this alert.
    private void snooze() {
        Intent intent = new Intent(SNOOZE);
        mContext.startService(intent);
        unregisteVoiceCmd();
    }

    // power on the device
    private void powerOn() {
        enableEventDispatching(false);
        stopPlayAlarm();
        // start boot animation
        SystemProperties.set("service.bootanim.exit", "0");
        SystemProperties.set("ctl.start", "bootanim");
        Log.d(TAG, "start boot animation");
        // send broadcast to power on the phone
        sendBR(NORMAL_BOOT_ACTION);
        unregisteVoiceCmd();
    }

    // power off the device
    private void powerOff() {
        unregisteVoiceCmd();
        sendBR("stop_ringtone");
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        boolean result = super.onTouchEvent(ev);
        //TODO: if we need to add some logic here ?
        return result;
    }

    @Override
    public void showUsabilityHint() {
    }

    /** TODO: hook this up */
    public void cleanUp() {
        if (DEBUG)
            Log.v(TAG, "Cleanup() called on " + this);
        mLockPatternUtils = null;
    }

    @Override
    public boolean needsInput() {
        return false;
    }

    @Override
    public void onPause() {
    }

    @Override
    public void onResume(int reason) {
        reset();
        Log.v(TAG, "onResume");
//         If the alarm was deleted at some point, disable snooze.
//        if (Alarms.getAlarm(mContext.getContentResolver(), mAlarm.id) == null) {
//            mGlowPadView.setTargetResources(R.array.dismiss_drawables);
//            mGlowPadView.setTargetDescriptionsResourceId(R.array.dismiss_descriptions);
//            mGlowPadView.setDirectionDescriptionsResourceId(R.array.dismiss_direction_descriptions);
//        }
        // The activity is locked to the default orientation as a default set in the manifest
        // Override this settings if the device is docked or config set it differently
//        if (getResources().getBoolean(R.bool.config_rotateAlarmAlert) || mIsDocked) {
//            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
//        }
    }

    @Override
    public KeyguardSecurityCallback getCallback() {
        return mCallback;
    }

    @Override
    public void onDetachedFromWindow() {
        Log.v(TAG, "onDetachedFromWindow ....");
        mContext.unregisterReceiver(mReceiver);
    }

    @Override
    public void showBouncer(int duration) {
    }

    @Override
    public void hideBouncer(int duration) {
    }

    // Cancel the notification and stop playing the alarm
    private void stopPlayAlarm() {
        mContext.stopService(new Intent(Alarms.ALARM_ALERT_ACTION));
    }

    private void enableEventDispatching(boolean flag) {
        try {
            final IWindowManager wm = IWindowManager.Stub
                    .asInterface(ServiceManager
                            .getService(Context.WINDOW_SERVICE));
            if(wm != null){
                wm.setEventDispatching(flag);
            }
        } catch (RemoteException e) {
            Log.w(TAG, e.toString());
        }
    }

    private void sendBR(String action){
        Log.w(TAG, "send BR: " + action);
        mContext.sendBroadcast(new Intent(action));
    }

    // Receives the ALARM_KILLED action from the AlarmKlaxon,
    // and also ALARM_SNOOZE_ACTION / ALARM_DISMISS_ACTION from other
    // applications
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
       @Override
       public void onReceive(Context context, Intent intent) {
          String action = intent.getAction();
          Log.v(TAG, "receive action : " + action);
          if(UPDATE_LABEL_ACTION.equals(action)){
              Message msg = new Message();
              msg.what = UPDATE_LABEL;
              Bundle data = new Bundle();
              data.putString("label", intent.getStringExtra("label"));
              msg.setData(data);
              mHandler.sendMessage(msg);
              context.removeStickyBroadcast(intent);
          }else if (KeyguardUpdateMonitor.isAlarmBoot()) {
              snooze();
          }
       }
    };

    private void unregisteVoiceCmd(){
        if (SUPPORT_VOICE_UI && voiceCmdManager != null) {
            try{
                voiceCmdManager.unRegisterListener(voiceCmdListener);
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    private void registerVoiceCmdService(Context context){
        voiceCmdManager = (IVoiceCommandManager) context.getSystemService(VoiceCommandListener.VOICE_COMMAND_SERVICE);
        if (voiceCmdManager != null) {
            voiceCmdListener = new VoiceCommandListener("powerOffAlarm") {
                @Override
                public void onVoiceCommandNotified(int mainAction, int subAction, Bundle extraData) {
                    int actionExtraResult = extraData.getInt(ACTION_EXTRA_RESULT);
                    String errorString = extraData.getString(ACTION_EXTRA_RESULT_INFO1);
                    if (mainAction == ACTION_MAIN_VOICE_UI) {
                        switch (subAction) {
                            case ACTION_VOICE_UI_START:
                                if (actionExtraResult == ACTION_EXTRA_RESULT_SUCCESS) {
                                    try {
                                        voiceCmdManager.sendCommand(
                                                        mContext,
                                                        ACTION_MAIN_VOICE_COMMON,
                                                        ACTION_VOICE_COMMON_KEYWORD,
                                                        null);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                                break;
                            case ACTION_VOICE_UI_NOTIFY :
                                int notifyResult = extraData.getInt(ACTION_EXTRA_RESULT);
                                if (notifyResult == ACTION_EXTRA_RESULT_SUCCESS) {
                                    int commandId = extraData.getInt(ACTION_EXTRA_RESULT_INFO);
                                    Log.v(TAG, "voice command id = " + commandId);
                                    if (commandId == 5) {
                                        snooze();
                                    } else if (commandId == 16) {
                                        powerOn();
                                    } else if (commandId == 17) {
                                        powerOff();
                                    }
                                }
                                break;
                            case ACTION_VOICE_UI_STOP :
                                break;
                            default :
                                break;
                        }
                    } else if (mainAction == ACTION_MAIN_VOICE_COMMON) {
                        if (subAction == ACTION_VOICE_COMMON_KEYWORD) {
                            int indicatorResult = extraData.getInt(ACTION_EXTRA_RESULT);
                            if (indicatorResult == ACTION_EXTRA_RESULT_SUCCESS) {
                                String[] stringCommonInfo = extraData.getStringArray(ACTION_EXTRA_RESULT_INFO);
                                String quotaStart = mContext.getString(com.mediatek.internal.R.string.voiceui_quota_start);
                                String quotaEnd = mContext.getString(com.mediatek.internal.R.string.voiceui_quota_end);
                                if(TextUtils.isEmpty(quotaStart)){
                                    quotaStart = "\"";
                                    quotaEnd = "\"";
                                }
                                StringBuilder sb = new StringBuilder();
                                sb.append(mContext.getString(com.mediatek.internal.R.string.voiceui_notify_string));
                                sb.append(quotaStart).append(stringCommonInfo[0]).append(quotaEnd);
                                sb.append(mContext.getString(com.mediatek.internal.R.string.voiceui_comma));
                                sb.append(quotaStart).append(stringCommonInfo[1]).append(quotaEnd);
                                sb.append(mContext.getString(com.mediatek.internal.R.string.voiceui_or));
                                sb.append(quotaStart).append(stringCommonInfo[2]).append(quotaEnd);
                                sb.append(mContext.getString(com.mediatek.internal.R.string.voiceui_control_poweroff_alarm));
                                if (mVcTips != null) {
                                    mVcTips.setText(sb);
                                }
                                if (mVcTipsContainer != null) {
                                    mVcTipsContainer.setVisibility(View.VISIBLE);
                                }
                            }
                        }
                    }
                }
            };
        }
    }

    @Override
    public void onGrabbed(View v, int handle) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onReleased(View v, int handle) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onGrabbedStateChange(View v, int handle) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onFinishFinalAnimation() {
        // TODO Auto-generated method stub
    }

    @Override
    public void reset() {
        // TODO Auto-generated method stub
    }

    private static final String SNOOZE = "android.intent.action.SNOOZE";
    private static final String UPDATE_LABEL_ACTION = "update.power.off.alarm.label";
    private static final String NORMAL_BOOT_ACTION = "android.intent.action.normal.boot";
    private static final String NORMAL_BOOT_DONE_ACTION = "android.intent.action.normal.boot.done";
    private static final String DISABLE_POWER_KEY_ACTION = "android.intent.action.DISABLE_POWER_KEY";

}
