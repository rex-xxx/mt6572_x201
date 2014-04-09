/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.format.DateUtils;
import android.text.TextUtils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.widget.Toast;



import com.android.internal.telephony.ITelephony;
import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.telephony.TelephonyManagerEx;

import java.io.File;
import java.io.IOException;

import java.util.Calendar;


/**
 * Manages alarms and vibe. Runs as a service so that it can continue to play
 * if another activity overrides the AlarmAlert dialog.
 */
public class AlarmKlaxon extends Service {
    static final String ALARM_REQUEST_SHUTDOWN_ACTION = "android.intent.action.ACTION_ALARM_REQUEST_SHUTDOWN";
    static final String NORMAL_SHUTDOWN_ACTION = "android.intent.action.normal.shutdown";
    // Default of 10 minutes until alarm is silenced.
    private static final String DEFAULT_ALARM_TIMEOUT = "10";
    /* Retry to play rintone after 1 seconds if power off alarm can not find external resource. */
    private static final int MOUNT_TIMEOUT_SECONDS = 1;

    private static final long[] sVibratePattern = new long[] { 500, 500 };
    private static final int GIMINI_SIM_1 = 0;
    private static final int GIMINI_SIM_2 = 1;

    /** the times to retry play rintone */
    private int mRetryCount = 0;
    private static final int MAX_RETRY_COUNT = 3;
    private boolean mPlaying = false;
    private Vibrator mVibrator;
    private MediaPlayer mMediaPlayer;
    private Alarm mCurrentAlarm;
    private long mStartTime;
    private TelephonyManager mTelephonyManager;
    private TelephonyManagerEx mTelephonyManagerEx;
    private ITelephony mTelephonyService;
    private int mCurrentCallState;

    /* Whether the alarm is using an external alert. */
    private boolean mUsingExternalUri;
    private Context mContext;
    private int mInitialCallState;

    // Internal messages
    private static final int KILLER = 1000;
    private static final int DELAY_TO_PLAY = 1001;
    private static final int STOP_SERVICE = 0;
    /// M: MTK power-off alarm
    private boolean isAlarmBoot = false;
    /// M: add for controlling music playing when alarm come
    private AudioManager mAudioManager;
    protected static int mCurrentPlayingAlarmId = -1;

    private final Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case KILLER:
                    if (Log.LOGV) {
                        Log.v("*********** Alarm killer triggered ***********");
                    }
                    stopPlayAlert((Alarm) msg.obj);
                    break;

                case DELAY_TO_PLAY:
                    Log.v("Alarm play external ringtone failed, retry to play after 1 seconds.");
                    play((Alarm) msg.obj);
                    break;
                /// M: @{
                case STOP_SERVICE:
                    Log.v("stop alarmklaxon service ... ");
                    PowerOffAlarmService.shutDown(mContext);
                    stopSelf();
                    break;
                /// @}
                default:
                    break;
            }
        }
    };

    private final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String ignored) {
            // The user might already be in a call when the alarm fires. When
            // we register onCallStateChanged, we get the initial in-call state
            // which kills the alarm. Check against the initial call state so
            // we don't kill the alarm during a call.
            int newPhoneState = TelephonyManager.CALL_STATE_IDLE;
            if (mTelephonyService != null) {
                try {
                    newPhoneState = mTelephonyService.getPreciseCallState();
                } catch (RemoteException ex) {
                    Log.v("Catch exception when getPreciseCallState: ex = "
                            + ex.getMessage());
                }
            }

            Log.v("onCallStateChanged : current state = " + newPhoneState + ",state = " + state
                    + ",mInitialCallState = " + mCurrentCallState);
            if (newPhoneState != TelephonyManager.CALL_STATE_IDLE
                    && newPhoneState != mCurrentCallState) {
                Log.v("Call state changed: mInitialCallState = " + mCurrentCallState
                        + ",mCurrentAlarm = " + mCurrentAlarm);
                /*
                 * M: when call state has changed from idle to non_idle, there should be no ring and no
                 * vibrate. @{
                 */
                if (mMediaPlayer != null) {
                    mMediaPlayer.setVolume(IN_CALL_VOLUME, IN_CALL_VOLUME);
                }
                Vibrator vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
                vibrator.cancel();
                /*@}*/

                /*
                 * M: when user end the call,the alert dialog should show,but neither ring
                 * nor vibrate happens. @{
                 */
                Alarm alarm = new Alarm();
                alarm = mCurrentAlarm;
                Intent phoneListenerIntent = new Intent(AlarmReceiver.ALARM_PHONE_LISTENER);
                phoneListenerIntent.putExtra(Alarms.ALARM_INTENT_EXTRA, alarm);
                mContext.startService(phoneListenerIntent);
                Log.v("alarm phone listener service starts");
                /* @} */
            }
        }
    };

    @Override
    public void onCreate() {
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        // Listen for incoming calls to kill the alarm.
        mTelephonyManager =
                (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        mTelephonyManagerEx = TelephonyManagerEx.getDefault();
        mTelephonyService = ITelephony.Stub.asInterface(ServiceManager.getService(Context.TELEPHONY_SERVICE));
        /// M : add for controlling music playing when alarm come
        mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);

        // Check if the device is gemini supported
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            mTelephonyManagerEx.listen(mPhoneStateListener,
                    PhoneStateListener.LISTEN_CALL_STATE | PhoneStateListener.LISTEN_SERVICE_STATE,
                    GIMINI_SIM_1);
            mTelephonyManagerEx.listen(mPhoneStateListener,
                    PhoneStateListener.LISTEN_CALL_STATE | PhoneStateListener.LISTEN_SERVICE_STATE,
                    GIMINI_SIM_2);
       } else {

            mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
       }
        /// M: @{
        mContext = AlarmKlaxon.this;
        AlarmAlertWakeLock.acquireCpuWakeLock(this);
        IntentFilter filter = new IntentFilter("stop_ringtone");
        filter.addAction(Alarms.ALARM_SNOOZE_ACTION);
        filter.addAction(Alarms.ALARM_DISMISS_ACTION);
        registerReceiver(stopPlayReceiver, filter);
        /// @}
    }

    @Override
    public void onDestroy() {
        stop();
        /// M: add for controlling music playing when alarm coming @{
        if (mAudioManager != null) {
            mAudioManager.abandonAudioFocus(audioListener);
        }
        /// @}
        Intent alarmDone = new Intent(Alarms.ALARM_DONE_ACTION);
        sendBroadcast(alarmDone);

        // Stop listening for incoming calls.
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            mTelephonyManagerEx.listen(mPhoneStateListener, 0, GIMINI_SIM_1);
            mTelephonyManagerEx.listen(mPhoneStateListener, 0, GIMINI_SIM_2);
        } else {
            mTelephonyManager.listen(mPhoneStateListener, 0);
        }
        if(!Alarms.bootFromPoweroffAlarm()){
           Alarms.backupRingtoneForPoweroffAlarm(getApplicationContext(), null);
        }
        mHandler.removeMessages(DELAY_TO_PLAY);
        Log.v("mHandler.removeMessages DELAY_TO_PLAY");
        AlarmAlertWakeLock.releaseCpuLock();
        /// M: unregister the broadcast receiver for power-off alarm
        unregisterReceiver(stopPlayReceiver);
        mCurrentPlayingAlarmId = -1;
        Alarms.savePlayingAlarmID(AlarmKlaxon.this, -1);
        Alarms.savePlayingAlarmStartTime(AlarmKlaxon.this, 0);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // No intent, tell the system not to restart us.
        if (Alarms.bootFromPoweroffAlarm()) {
            mCurrentAlarm = null;
        }
        if (intent == null) {
            final SharedPreferences prefs = getSharedPreferences(Alarms.PREFERENCES, 0);
            final int playingAlarmId = prefs.getInt(Alarms.PLAYING_ALARM_ID, -1);
            if(playingAlarmId != -1){
                final long playingStartTime = prefs.getLong(Alarms.PLAYING_ALARM_START_TIME, 0);
                final Alarm a = Alarms.getAlarm(this.getContentResolver(), playingAlarmId);
                long millis = System.currentTimeMillis() - playingStartTime;
                int minutes = (int) Math.round(millis / 60000.0) + 1;

                Intent alarmKillOrChangeNotification = new Intent(Alarms.ALARM_CHANGE_NOTIFICATION);
                alarmKillOrChangeNotification.putExtra(Alarms.ALARM_INTENT_EXTRA, a);
                alarmKillOrChangeNotification.putExtra(Alarms.ALARM_KILLED_TIMEOUT, minutes);
                alarmKillOrChangeNotification.putExtra("snoozed", true);
                sendBroadcast(alarmKillOrChangeNotification);
            }
            stopSelf();
            return START_NOT_STICKY;
        }
        /// M : @{
        final Alarm alarm;
        isAlarmBoot = intent.getBooleanExtra("isAlarmBoot", false);
        Log.v("alarm boot: " + isAlarmBoot);
        if (isAlarmBoot) {
            alarm = Alarms.getNearestAlarm(this);
            if (alarm == null) {
                // Make sure we set the next alert if needed.
                Alarms.setNextAlert(this);
                return START_NOT_STICKY;
            }
            // Disable the snooze alert if this alarm is the snooze.
            Alarms.disableSnoozeAlert(this, alarm.id);
            // Disable this alarm if it does not repeat.
            if (alarm.daysOfWeek.isRepeatSet()) {
                // Enable the next alert if there is one. The above call to
                // enableAlarm will call setNextAlert so avoid calling it twice.
                Alarms.setNextAlert(this);
            } else {
                Alarms.enableAlarm(this, alarm.id, false);
            }
        } else {
        /// @}
            alarm = intent.getParcelableExtra(Alarms.ALARM_INTENT_EXTRA);
        }
        if (alarm == null) {
            Log.v("AlarmKlaxon failed to parse the alarm from the intent");
            stopSelf();
            return START_NOT_STICKY;
        }
        if(alarm != null && Alarms.bootFromPoweroffAlarm()){
            updatePoweroffAlarmLabel(alarm.label);
        }

        Log.v("mCurrentAlarm=" + mCurrentAlarm + "  alarm: " + alarm.alert);
        if (mCurrentAlarm != null) {
            if (mCurrentAlarm.id == alarm.id) {
                sendKillOrChangeNotificationBroadcast(Alarms.ALARM_CHANGE_NOTIFICATION, mCurrentAlarm, false);
            } else {
                sendKillOrChangeNotificationBroadcast(Alarms.ALARM_CHANGE_NOTIFICATION, mCurrentAlarm, true);
            }
        }

        Log.v("onStartCommand: intent = " + intent + "alarm id = " + alarm.id + ",alert = "
                + alarm.alert);
        if (alarm.alert != null) {
            mUsingExternalUri = usingExternalUri(alarm.alert);
        }

        play(alarm);
        mCurrentAlarm = alarm;
        initTelephonyService();
        mCurrentPlayingAlarmId = alarm.id;
        Alarms.savePlayingAlarmID(AlarmKlaxon.this, alarm.id);
        Alarms.savePlayingAlarmStartTime(AlarmKlaxon.this, mStartTime);
        return START_STICKY;
    }

    /**
     * @param alarm
     * @param flag
     *            true stands for one alarm snoozed, false stands another alarm
     *            come.
     */
    private void sendKillOrChangeNotificationBroadcast(String action, Alarm alarm, boolean flag) {
        long millis = System.currentTimeMillis() - mStartTime;
        int minutes = (int) Math.round(millis / (double)DateUtils.MINUTE_IN_MILLIS);
        if (TextUtils.isEmpty(action)) {
            action = Alarms.ALARM_KILLED;
        }
        Intent alarmKillOrChangeNotification = new Intent(action);
        alarmKillOrChangeNotification.putExtra(Alarms.ALARM_INTENT_EXTRA, alarm);
        alarmKillOrChangeNotification.putExtra(Alarms.ALARM_KILLED_TIMEOUT, minutes);
        alarmKillOrChangeNotification.putExtra("snoozed", flag);
        Log.v("sendKillOrChangeNotificationBroadcast: mStartTime = " + mStartTime + ",millis = "
                + millis + ",minutes = " + minutes + ",this = " + this);
        sendBroadcast(alarmKillOrChangeNotification);
    }

    // Volume suggested by media team for in-call alarms.
    private static final float IN_CALL_VOLUME = 0.0f;

    /**  M:
     * when the alarm is silent, just do not play the music, but still will
     * initialize the AudioManager and Mediaplayer because we have to disturb
     * playing ringtone by audiomanager.
     *
     * @param alarm
     */
    private void play(Alarm alarm) {
        // stop() checks to see if we are already playing.
        stop();

        if (Log.LOGV) {
            Log.v("AlarmKlaxon.play() " + alarm.id + " alert " + alarm.alert);
        }

        boolean isSilent = alarm.silent;
        Uri alert = alarm.alert;
        // Fall back on the default alarm if the database does not have an
        // alarm stored.
        if (alert == null) {
            alert = RingtoneManager.getActualDefaultRingtoneUri(mContext, RingtoneManager.TYPE_ALARM);
            if (Log.LOGV) {
                Log.v("Using default alarm: " + alert.toString());
            }
        }

        // TODO: Reuse mMediaPlayer instead of creating a new one and/or use
        // RingtoneManager.
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnErrorListener(new OnErrorListener() {
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Log.e("Error occurred while playing audio.");
                mp.stop();
                mp.release();
                mMediaPlayer = null;
                /// M: add for controlling music playing when alarm come @{
                if (mAudioManager != null) {
                    mAudioManager.abandonAudioFocus(audioListener);
                }
                /// @}
                return true;
            }
        });

        try {
            /// M: playing external ringtone for power-off alarm @{
            String ringtonePath = null;
            if (isAlarmBoot && mUsingExternalUri) {
                java.io.File dir = getFilesDir();
                Log.v("base dir: " + dir.getAbsolutePath());
                File[] files = dir.listFiles();
                if (files != null && files.length > 0) {
                    ringtonePath = files[0].getAbsolutePath();
                }
            }
            /// M: @}

            // Check if we are in a call. If we are, use the in-call alarm
            // resource at a low volume to not disrupt the call.
            if (mCurrentCallState != TelephonyManager.CALL_STATE_IDLE) {
                Log.v("Using the in-call alert: mUsingExternalUri = "
                        + mUsingExternalUri);
                mMediaPlayer.setVolume(IN_CALL_VOLUME, IN_CALL_VOLUME);
                setDataSourceFromResource(getResources(), mMediaPlayer,
                        R.raw.in_call_alarm);
                alarm.vibrate = false;
            } else {
                Log.v("ringtone: " + ringtonePath);
                if (!TextUtils.isEmpty(ringtonePath)) {
                    File file = new File(ringtonePath);
                    if (file.exists() && file.getTotalSpace() > 0) {
                        java.io.FileInputStream fis = null;
                        try {
                            fis = new java.io.FileInputStream(file);
                            mMediaPlayer.setDataSource(fis.getFD());
                        } catch (IOException e) {
                            mMediaPlayer.setDataSource(this, alert);
                            Log.e(e.toString());
                        } finally {
                            if (fis != null) {
                                fis.close();
                            }
                        }
                    } else {
                        if(alert == null) {
                            isSilent = true;
                        }else{
                            mMediaPlayer.setDataSource(this, alert);
                        }
                    }
                } else {
                    if(alert == null) {
                        isSilent = true;
                    }else{
                        mMediaPlayer.setDataSource(this, alert);
                    }
                }
            }
            startAlarm(mMediaPlayer, isSilent);
        } catch (IOException ex) {
            Log.v("Exception occured mUsingExternalUri = " + mUsingExternalUri);
            Log.v("Exception occured retryCount = " + mRetryCount);
            if (mUsingExternalUri && mRetryCount < MAX_RETRY_COUNT) {
                delayToPlayAlert(alarm);
                // Reset it to false.
                // mUsingExternalUri = false;
                mRetryCount++;
                mStartTime = System.currentTimeMillis();
                return;
            } else {
                Log.v("Using the fallback ringtone");
                // The alert may be on the sd card which could be busy right
                // now. Use the fallback ringtone.
                try {
                    // Must reset the media player to clear the error state.
                    mMediaPlayer.reset();
                    Uri defaultUri = RingtoneManager.getActualDefaultRingtoneUri(mContext, RingtoneManager.TYPE_ALARM);
                    if(defaultUri == null){
                        isSilent = true;
                    }else{
                        if(!AlarmClock.isRingtoneExisted(mContext, defaultUri)){
                            defaultUri = AlarmPreference.DEFAULT_RINGTONE_URI;
                        }
                        mMediaPlayer.setDataSource(this, defaultUri);
                    }
                    startAlarm(mMediaPlayer, isSilent);
                } catch (IOException ioe2) {
                    // At this point we just don't play anything.
                    Log.e("Failed to play fallback ringtone", ioe2);
                }
            }
        }

        /* Start the vibrator after everything is ok with the media player */
        if (alarm.vibrate) {
            mVibrator.vibrate(sVibratePattern, 0);
        } else {
            mVibrator.cancel();
        }

        enableKiller(alarm);
        mPlaying = true;
        mStartTime = System.currentTimeMillis();
    }

    // Do the common stuff when starting the alarm.
    private void startAlarm(MediaPlayer player, boolean silent)
            throws java.io.IOException, IllegalArgumentException,
                   IllegalStateException {

        /// M: modified for controlling music play when alarm coming @{
        int result = mAudioManager.requestAudioFocus(audioListener, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        // do not play alarms if stream volume is 0
        // (typically because ringer mode is silent).
        boolean isVolumeOk = mAudioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0;
        if (silent) {
            mAudioManager.abandonAudioFocus(audioListener);
            return;
        }
        Log.v("volume is not silent : " + isVolumeOk);
        /// @}

        if (isVolumeOk && result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            player.setAudioStreamType(AudioManager.STREAM_ALARM);
            player.setLooping(true);
            player.prepare();
            player.start();
        }
    }

    /// M:listen to audio focus changing. @{
    private AudioManager.OnAudioFocusChangeListener audioListener = new AudioManager.OnAudioFocusChangeListener() {
        public void onAudioFocusChange(int focusChange) {
            if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                Log.v("audio focus gain ...");
                if (mMediaPlayer != null && !mMediaPlayer.isPlaying()) {
                    mMediaPlayer.start();
                }
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
                    || focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                Log.v("audio focus loss ...");
                if (mAudioManager != null) {
                    mAudioManager.abandonAudioFocus(audioListener);
                }
                mMediaPlayer.stop();
            }
        }
    };
    /// @}

    private void setDataSourceFromResource(Resources resources,
            MediaPlayer player, int res) throws java.io.IOException {
        AssetFileDescriptor afd = resources.openRawResourceFd(res);
        if (afd != null) {
            player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(),
                    afd.getLength());
            afd.close();
        }
    }

    /**
     * Stops alarm audio and disables alarm if it not snoozed and not
     * repeating
     */
    public void stop() {
        if (Log.LOGV) {
            Log.v("AlarmKlaxon.stop().");
        }

        if (mPlaying) {
            mPlaying = false;

            // Stop audio playing
            if (mMediaPlayer != null) {
                mMediaPlayer.stop();
                mMediaPlayer.release();
                mMediaPlayer = null;
            }

            // Stop vibrator
            mVibrator.cancel();
            disableKiller();
        }
    }

    /**
     * Kills alarm audio after ALARM_TIMEOUT_SECONDS, so the alarm
     * won't run all day.
     *
     * This just cancels the audio, but leaves the notification
     * popped, so the user will know that the alarm tripped.
     */
    private void enableKiller(Alarm alarm) {
        Log.v("enableKiller: alarm = " + alarm + ",this = " + this);
        final String autoSnooze =
                PreferenceManager.getDefaultSharedPreferences(this)
                .getString(SettingsActivity.KEY_AUTO_SILENCE,
                        DEFAULT_ALARM_TIMEOUT);
        int autoSnoozeMinutes = Integer.parseInt(autoSnooze);
        if (autoSnoozeMinutes != -1) {
            mHandler.sendMessageDelayed(mHandler.obtainMessage(KILLER, alarm),
                    autoSnoozeMinutes * DateUtils.MINUTE_IN_MILLIS);
        }
    }

    private void disableKiller() {
        mHandler.removeMessages(KILLER);
    }

    private void delayToPlayAlert(Alarm alarm) {
        Log.v("delayToPlayAlert: alarm = " + alarm + ",this = " + this);
        mHandler.sendMessageDelayed(mHandler.obtainMessage(DELAY_TO_PLAY, alarm),
                1000 * MOUNT_TIMEOUT_SECONDS);
    }

    private boolean usingExternalUri(Uri alert) {
        Uri mediaUri = null;
        final String scheme = alert.getScheme();
        if ("content".equals(scheme)) {
            if (Settings.AUTHORITY.equals(alert.getAuthority())) {
                String uriString = android.provider.Settings.System.getString(this.getContentResolver(), "alarm_alert");
                if (uriString != null) {
                    mediaUri = Uri.parse(uriString);
                } else {
                    mediaUri = alert;
                }
            } else {
                mediaUri = alert;
            }
            if (MediaStore.AUTHORITY.equals(mediaUri.getAuthority())) {
                Log.v("AlarmKlaxon onStartCommand mediaUri = " +
                        mediaUri + ",segment 0 = " + mediaUri.getPathSegments().get(0));
                if (mediaUri.getPathSegments().get(0).equalsIgnoreCase("external")) {
                    // Alert is using an external ringtone.
                    return true;
                }
            }
        }
        return false;
    }

    private void stopPlayAlert(Alarm alarm) {
        Log.v("stopPlayAlert: alarm = " + alarm);
        mHandler.removeMessages(DELAY_TO_PLAY);
        sendKillOrChangeNotificationBroadcast(null, alarm, true);
        if(!Alarms.bootFromPoweroffAlarm()){
            stopSelf();
        }
    }

    /// M: receive broadcast to prepare external ringtone file for power-off alarm @{
    private BroadcastReceiver stopPlayReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("stop_ringtone")) {
                if(mMediaPlayer != null){
                    mMediaPlayer.stop();
                }
                Alarms.backupRingtoneForPoweroffAlarm(getApplicationContext(), mHandler);
            } else if (Alarms.ALARM_SNOOZE_ACTION.equals(intent.getAction())) {
                Alarms.snooze(context, mCurrentAlarm);
                mCurrentAlarm = null;
            } else if (Alarms.ALARM_DISMISS_ACTION.equals(intent.getAction())) {
                Alarms.dismiss(context, mCurrentAlarm, false);
                mCurrentAlarm = null;
            }
        }
    };
    /// @}

    /// M: shut down the device @{
    private void shutDown() {
        Log.v("send shutdown broadcast: android.intent.action.normal.shutdown");
        Intent shutdownIntent = new Intent(NORMAL_SHUTDOWN_ACTION);
        sendBroadcast(shutdownIntent);
        Intent intent = new Intent(ALARM_REQUEST_SHUTDOWN_ACTION);
        intent.putExtra("android.intent.extra.KEY_CONFIRM", false);  //Intent.EXTRA_KEY_CONFIRM
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void initTelephonyService() {
        // Record the initial call state here so that the new alarm has the
        // newest state.
        if (mTelephonyService != null) {
            try {
                mCurrentCallState = mTelephonyService.getPreciseCallState();
            } catch (RemoteException ex) {
                Log.v("Catch exception when getPreciseCallState: ex = "
                        + ex.getMessage());
            }
        }
    }

    private void updatePoweroffAlarmLabel(String label){
        Intent intent = new Intent("update.power.off.alarm.label");
        intent.putExtra("label", (label == null ? "" : label));
        sendStickyBroadcast(intent);
    }
    /// @}
}
