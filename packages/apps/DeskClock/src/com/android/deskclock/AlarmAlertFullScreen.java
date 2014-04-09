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

package com.android.deskclock;

import static com.mediatek.common.voicecommand.VoiceCommandListener.ACTION_EXTRA_RESULT;
import static com.mediatek.common.voicecommand.VoiceCommandListener.ACTION_EXTRA_RESULT_INFO;
import static com.mediatek.common.voicecommand.VoiceCommandListener.ACTION_EXTRA_RESULT_INFO1;
import static com.mediatek.common.voicecommand.VoiceCommandListener.ACTION_EXTRA_RESULT_SUCCESS;
import static com.mediatek.common.voicecommand.VoiceCommandListener.ACTION_MAIN_VOICE_COMMON;
import static com.mediatek.common.voicecommand.VoiceCommandListener.ACTION_MAIN_VOICE_UI;
import static com.mediatek.common.voicecommand.VoiceCommandListener.ACTION_VOICE_COMMON_KEYWORD;
import static com.mediatek.common.voicecommand.VoiceCommandListener.ACTION_VOICE_UI_NOTIFY;
import static com.mediatek.common.voicecommand.VoiceCommandListener.ACTION_VOICE_UI_START;
import static com.mediatek.common.voicecommand.VoiceCommandListener.ACTION_VOICE_UI_STOP;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.deskclock.widget.multiwaveview.GlowPadView;
import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.common.voicecommand.IVoiceCommandManager;
import com.mediatek.common.voicecommand.VoiceCommandListener;

import java.util.Calendar;

/**
 * Alarm Clock alarm alert: pops visible indicator and plays alarm
 * tone. This activity is the full screen version which shows over the lock
 * screen with the wallpaper as the background.
 */
public class AlarmAlertFullScreen extends Activity implements GlowPadView.OnTriggerListener {
    private final String ALARM_PHONE_LISTENER = "com.android.deskclock.ALARM_PHONE_LISTENER";
    private final boolean LOG = true;
    // These defaults must match the values in res/xml/settings.xml
    private static final String DEFAULT_SNOOZE = "10";
    // the priority for receiver to receive the kill alarm broadcast first
    private static final int PRIORITY = 100;
    private static final String DEFAULT_VOLUME_BEHAVIOR = "1";
    private static final String KEY_VOLUME_BEHAVIOR = "power_on_volume_behavior";
    protected static final String SCREEN_OFF = "screen_off";
    protected Alarm mAlarm;
    private int mVolumeBehavior;
    boolean mFullscreenStyle;
    private GlowPadView mGlowPadView;
    private boolean mIsDocked = false;

    // Parameters for the GlowPadView "ping" animation; see triggerPing().
    private static final int PING_MESSAGE_WHAT = 101;
    private static final boolean ENABLE_PING_AUTO_REPEAT = true;
    private static final long PING_AUTO_REPEAT_DELAY_MSEC = 1200;

    private boolean mPingEnabled = true;

    // Receives the ALARM_KILLED action from the AlarmKlaxon,
    // and also ALARM_SNOOZE_ACTION / ALARM_DISMISS_ACTION from other applications
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (LOG) {
                Log.v("AlarmAlertFullScreen - onReceive " + action);
            }
            if (action.equals(Alarms.ALARM_SNOOZE_ACTION)) {
                Alarms.snooze(context, mAlarm);
                finish();
            } else if (action.equals(Alarms.ALARM_DISMISS_ACTION)) {
                if (IS_SUPPORT_VOICE_COMMAND_UI) {
                    getNotificationManager().cancel("voiceui",100);
                }
                Alarms.dismiss(context, mAlarm, false);
                finish();
            } else {
                Alarm alarm = intent.getParcelableExtra(Alarms.ALARM_INTENT_EXTRA);
                boolean replaced = intent.getBooleanExtra(Alarms.ALARM_REPLACED, false);
                if (alarm != null && mAlarm.id == alarm.id) {
                    if (IS_SUPPORT_VOICE_COMMAND_UI) {
                        getNotificationManager().cancel("voiceui",100);
                    }
                    Alarms.dismiss(context, mAlarm, true);
                    if (!replaced) {
                        finish();
                    }
                }
            }
        }
    };

    private final Handler mPingHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case PING_MESSAGE_WHAT:
                    triggerPing();
                    break;
            }
        }
    };

    public static final String TAG = "VoiceCommand";
    public static final boolean IS_SUPPORT_VOICE_COMMAND_UI = FeatureOption.MTK_VOICE_UI_SUPPORT;
    private IVoiceCommandManager  mVoiceCmdManager;
    private VoiceCommandListener mVoiceCmdListener;
    private static final int VOICE_COMMAND_ID_SNOOZE = 5;
    private static final int VOICE_COMMAND_ID_STOP = 6;
    private String[] mKeywordArray;
    private Context mContext;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Log.v("onCreate");
        ///M: init voice command manager and listener @{
        if (IS_SUPPORT_VOICE_COMMAND_UI) {
            mContext = this;
            mVoiceCmdManager = (IVoiceCommandManager)getSystemService(
                    VoiceCommandListener.VOICE_COMMAND_SERVICE);
            if (mVoiceCmdManager != null) {
                mVoiceCmdListener = new VoiceCommandListener(this) {
                    @Override
                        public void onVoiceCommandNotified (int mainAction,int subAction,
                                Bundle extraData) {
                            handleVoiceCommandNotified(mainAction,subAction,extraData);
                        }
                };
            }
        }
        /// @}

       // Register to get the alarm killed/snooze/dismiss intent.
        IntentFilter filter = new IntentFilter(Alarms.ALARM_KILLED);
        filter.addAction(Alarms.ALARM_SNOOZE_ACTION);
        filter.addAction(Alarms.ALARM_DISMISS_ACTION);
        filter.setPriority(PRIORITY);
        registerReceiver(mReceiver, filter);

        if (Alarms.bootFromPoweroffAlarm()) {
            finish();
            return ;
        }
        mAlarm = getIntent().getParcelableExtra(Alarms.ALARM_INTENT_EXTRA);
        if (LOG) {
            Log.v("AlarmAlertFullScreen - onCreate");
            if (mAlarm != null) {
                Log.v("AlarmAlertFullScreen - Alarm Id " + mAlarm.toString());
            }
        }

        // Get the volume/camera button behavior setting
        final String vol =
                PreferenceManager.getDefaultSharedPreferences(this)
                .getString(SettingsActivity.KEY_VOLUME_BEHAVIOR,
                        SettingsActivity.DEFAULT_VOLUME_BEHAVIOR);
        mVolumeBehavior = Integer.parseInt(vol);

        final Window win = getWindow();
        win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        // Turn on the screen unless we are being launched from the AlarmAlert
        // subclass as a result of the screen turning off.
        if (!getIntent().getBooleanExtra(SCREEN_OFF, false)) {
            win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
        }

        updateLayout();

        // Check the docking status , if the device is docked , do not limit rotation
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_DOCK_EVENT);
        Intent dockStatus = registerReceiver(null, ifilter);
        if (dockStatus != null) {
            mIsDocked = dockStatus.getIntExtra(Intent.EXTRA_DOCK_STATE, -1)
                    != Intent.EXTRA_DOCK_STATE_UNDOCKED;
        }
    }

    private void setTitle() {
        final String titleText = mAlarm.getLabelOrDefault(this);

        TextView tv = (TextView) findViewById(R.id.alertTitle);
        if (tv != null) {
            tv.setText(titleText);
        }
        setTitle(titleText);
    }

    protected int getLayoutResId() {
        return R.layout.alarm_alert;
    }

    private void updateLayout() {
        if (LOG) {
            Log.v("AlarmAlertFullScreen - updateLayout");
        }

        final LayoutInflater inflater = LayoutInflater.from(this);
        final View view = inflater.inflate(getLayoutResId(), null);
        view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
        setContentView(view);

        /* Set the title from the passed in alarm */
        setTitle();

        mGlowPadView = (GlowPadView) findViewById(R.id.glow_pad_view);
        mGlowPadView.setOnTriggerListener(this);
        triggerPing();
    }

    private void triggerPing() {
        if (mPingEnabled) {
            mGlowPadView.ping();

            if (ENABLE_PING_AUTO_REPEAT) {
                mPingHandler.sendEmptyMessageDelayed(PING_MESSAGE_WHAT, PING_AUTO_REPEAT_DELAY_MSEC);
            }
        }
    }

    private NotificationManager getNotificationManager() {
        return (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    /**
     * this is called when a second alarm is triggered while a
     * previous alert window is still active.
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (Log.LOGV) {
             Log.v("AlarmAlert.OnNewIntent()");
        }
        mAlarm = intent.getParcelableExtra(Alarms.ALARM_INTENT_EXTRA);
        setTitle();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (LOG) {
            Log.v("AlarmAlertFullScreen - onConfigChanged");
        }
        updateLayout();
        if (mVoiceUiStartSuccessful) {
            displayIndicator();
        }
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (LOG) {
            Log.v("AlarmAlertFullScreen - onResume");
        }
        // If the alarm was deleted at some point, disable snooze.
        if (Alarms.getAlarm(getContentResolver(), mAlarm.id) == null) {
            mGlowPadView.setTargetResources(R.array.dismiss_drawables);
            mGlowPadView.setTargetDescriptionsResourceId(R.array.dismiss_descriptions);
            mGlowPadView.setDirectionDescriptionsResourceId(R.array.dismiss_direction_descriptions);
        }
        // The activity is locked to the default orientation as a default set in the manifest
        // Override this settings if the device is docked or config set it differently
        if (getResources().getBoolean(R.bool.config_rotateAlarmAlert) || mIsDocked) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }

        ///M: register and start voice UI. @{
        if (IS_SUPPORT_VOICE_COMMAND_UI) {
            if (mVoiceCmdManager != null) {
                try {
                    mVoiceCmdManager.registerListener(mVoiceCmdListener);
                    mVoiceCmdManager.sendCommand(this,
                            ACTION_MAIN_VOICE_UI,
                            ACTION_VOICE_UI_START,null);
                } catch (RemoteException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        /// @}
    }

    @Override
    public void onStop() {
        super.onStop();
        ///M: stop voice ui and unregister corresponding listener.@{
        if (IS_SUPPORT_VOICE_COMMAND_UI) {
            if (mVoiceCmdManager != null) {
                try {
                    mVoiceCmdManager.sendCommand(this,
                            ACTION_MAIN_VOICE_UI,
                            ACTION_VOICE_UI_STOP,null);
                            mVoiceCmdManager.unRegisterListener(mVoiceCmdListener);
                } catch (RemoteException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (Log.LOGV) {
            Log.v("AlarmAlert.onDestroy()");
        }
        // No longer care about the alarm being killed.
        unregisterReceiver(mReceiver);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // Do this on key down to handle a few of the system keys.
        boolean up = event.getAction() == KeyEvent.ACTION_UP;
        if (LOG) {
            Log.v("AlarmAlertFullScreen - dispatchKeyEvent " + event.getKeyCode());
        }
        switch (event.getKeyCode()) {
            // Volume keys and camera keys dismiss the alarm
            case KeyEvent.KEYCODE_POWER:
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_MUTE:
            case KeyEvent.KEYCODE_CAMERA:
            case KeyEvent.KEYCODE_FOCUS:
                if (up) {
                    switch (mVolumeBehavior) {
                        case 1:
                            Alarms.snooze(this, mAlarm);
                            finish();
                            break;

                        case 2:
                            if (IS_SUPPORT_VOICE_COMMAND_UI) {
                                getNotificationManager().cancel("voiceui",100);
                            }
                            Alarms.dismiss(this, mAlarm, false);
                            finish();
                            break;

                        default:
                            break;
                    }
                }
                return true;
            default:
                break;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onBackPressed() {
        // Don't allow back to dismiss. This method is overriden by AlarmAlert
        // so that the dialog is dismissed.
        if (LOG) {
            Log.v("AlarmAlertFullScreen - onBackPressed");
        }
        return;
    }


    @Override
    public void onGrabbed(View v, int handle) {
        mPingEnabled = false;
    }

    @Override
    public void onReleased(View v, int handle) {
        mPingEnabled = true;
        triggerPing();
    }

    @Override
    public void onTrigger(View v, int target) {
        final int resId = mGlowPadView.getResourceIdForTarget(target);
        switch (resId) {
            case R.drawable.ic_alarm_alert_snooze:
                Alarms.snooze(this, mAlarm);
                finish();
                break;

            case R.drawable.ic_alarm_alert_dismiss:
                if (IS_SUPPORT_VOICE_COMMAND_UI) {
                    getNotificationManager().cancel("voiceui",100);
                }
                Alarms.dismiss(this, mAlarm, false);
                finish();
                break;
            default:
                // Code should never reach here.
                Log.e("Trigger detected on unhandled resource. Skipping.");
        }
    }

    @Override
    public void onGrabbedStateChange(View v, int handle) {
    }

    @Override
    public void onFinishFinalAnimation() {
    }
    private boolean mVoiceUiStartSuccessful = false;

    private void handleVoiceCommandNotified(int mainAction,int subAction,Bundle extraData) {
        switch(mainAction) {
        case ACTION_MAIN_VOICE_COMMON:
            if (subAction == ACTION_VOICE_COMMON_KEYWORD) {
                int keywordResult = extraData.getInt(ACTION_EXTRA_RESULT);
                if (keywordResult == ACTION_EXTRA_RESULT_SUCCESS) {
                    mKeywordArray =  extraData.getStringArray(
                            VoiceCommandListener.ACTION_EXTRA_RESULT_INFO);
                    if (mKeywordArray != null && mKeywordArray.length >= 2) {
                        displayIndicator();
                        Notification indicatorNotify = new Notification.Builder(mContext)
                            .setContentTitle(mContext.getString(R.string.voice_ui_title))
                            .setContentText(mContext.getString(R.string.voice_ui_content_text))
                            .setSmallIcon(com.mediatek.internal.R.drawable.stat_voice)
                            .build();
                        getNotificationManager().notify("voiceui",100,indicatorNotify);
                    }
                }
            }
            break;
        case ACTION_MAIN_VOICE_UI:
            if (subAction == ACTION_VOICE_UI_START ) {
                int startResult = extraData.getInt(ACTION_EXTRA_RESULT);
                if (startResult == ACTION_EXTRA_RESULT_SUCCESS) {
                    //success.
                    mVoiceUiStartSuccessful = true;
                    try {
                        mVoiceCmdManager.sendCommand(this,
                                ACTION_MAIN_VOICE_COMMON,
                                ACTION_VOICE_COMMON_KEYWORD,null);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                    android.util.Log.v(TAG,"voice ui start success.");
                } else {
                //error
                    int errorID = extraData.getInt(ACTION_EXTRA_RESULT_INFO);
                    String errorString = extraData.getString(ACTION_EXTRA_RESULT_INFO1);
                    android.util.Log.v(TAG," voice ui starts abnormally,with errorID: " +
                                    errorID + ",errorString: " + errorString);
                }
            } else if (subAction == ACTION_VOICE_UI_STOP) {
                int stopResult = extraData.getInt(ACTION_EXTRA_RESULT);
                if (stopResult == ACTION_EXTRA_RESULT_SUCCESS) {
                    //sucess
                    android.util.Log.v(TAG,"voice ui stop success");
                } else {
                    //ERROR
                    int stopErrorID = extraData.getInt(ACTION_EXTRA_RESULT_INFO);
                    String stopErrorString = extraData.getString(ACTION_EXTRA_RESULT_INFO1);
                    android.util.Log.v(TAG,"voice ui stop error with errorID:" +
                            stopErrorID + ",errorString:" + stopErrorString);
                }
            } else if (subAction == ACTION_VOICE_UI_NOTIFY) {
                int notifyResult = extraData.getInt(ACTION_EXTRA_RESULT);
                int id = extraData.getInt(ACTION_EXTRA_RESULT_INFO);
                if (notifyResult == ACTION_EXTRA_RESULT_SUCCESS) {
                    //success
                    if (id == VOICE_COMMAND_ID_SNOOZE) {
                        //ringtone disappears gradually.
                        android.util.Log.v(TAG,"snooze is triggered");
                        Alarms.snooze(this, mAlarm);
                        finish();
                    } else if (id == VOICE_COMMAND_ID_STOP) {
                        android.util.Log.v(TAG,"dismissed is triggered");
                        if (IS_SUPPORT_VOICE_COMMAND_UI) {
                            getNotificationManager().cancel("voiceui",100);
                        }
                        Alarms.dismiss(this, mAlarm, false);
                        finish();
                    }
                } else {
                    //error
                    String notifyErrorString = extraData.getString(ACTION_EXTRA_RESULT_INFO1);
                    android.util.Log.v(TAG,"something is wrong when notify,with notifyError id:" +
                            id + ",notifyErrorString:" + notifyErrorString);
                }
            }
            break;
        default:
            break;
        }
    }

    private void displayIndicator() {
        ImageView icon = (ImageView) findViewById(R.id.indicator_icon);
        TextView ticker = (TextView) findViewById(R.id.indicator_text);
        icon.setImageResource(com.mediatek.internal.R.drawable.stat_voice);
        icon.setVisibility(View.VISIBLE);
        ticker.setVisibility(View.VISIBLE);
        Configuration conf = getResources().getConfiguration();
        if (conf.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            ticker.setText(mContext.getString(
                    R.string.alarm_command_summary_format_land,
                    mKeywordArray[0],mKeywordArray[1]));
        } else {
            ticker.setText(mContext.getString(
                    R.string.alarm_command_summary_format,
                    mKeywordArray[0],mKeywordArray[1]));
        }
    }
}
