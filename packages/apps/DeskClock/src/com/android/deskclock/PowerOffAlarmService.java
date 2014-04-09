package com.android.deskclock;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.preference.PreferenceManager;

/**
 * This broadcast receiver intents to receive power off alarm alert broadcast
 * sent by AlarmManagerService.
 */
public class PowerOffAlarmService extends Service {
    private static final String TAG = "PowerOffAlarmService";
    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v("start service , intent = " + intent.getAction());
        if (intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }
        snooze(PowerOffAlarmService.this);
        return START_STICKY;
    }

    /**
     * save next alarm time and show snooze time by toast
     */
    private void snooze(Context context) {
        final String snooze = PreferenceManager.getDefaultSharedPreferences(
                context).getString(SettingsActivity.KEY_ALARM_SNOOZE, DEFAULT_SNOOZE);
        Log.v("duration of alarm snooze: " + snooze);
        int snoozeMinutes = Integer.parseInt(snooze);
        final long snoozeTime = System.currentTimeMillis()
                + ((long) 1000 * 60 * snoozeMinutes);
        if(AlarmKlaxon.mCurrentPlayingAlarmId != -1){
            Alarms.saveSnoozeAlert(context, AlarmKlaxon.mCurrentPlayingAlarmId, snoozeTime);
        }
        String displayTime = context.getResources().getString(R.string.alarm_alert_snooze_set,
                snoozeMinutes);
        Log.v("display time: " + displayTime + " snoozeTime: " + snoozeTime);
        context.sendBroadcast(new Intent("stop_ringtone"));
    }

    // shut down the device
    protected static void shutDown(Context mContext) {
        // send normal shutdown broadcast
        Intent shutdownIntent = new Intent(NORMAL_SHUTDOWN_ACTION);
        mContext.sendBroadcast(shutdownIntent);

        // shutdown the device
        Intent intent = new Intent(ALARM_REQUEST_SHUTDOWN_ACTION);
        //intent.putExtra(Intent.EXTRA_KEY_CONFIRM, false);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
    }

    private static final String DEFAULT_SNOOZE = "10";
    static final String SNOOZE = "android.intent.action.SNOOZE";
    private static final String NORMAL_SHUTDOWN_ACTION = "android.intent.action.normal.shutdown";
    private static final String NORMAL_BOOT_ACTION = "android.intent.action.normal.boot";
    private static final String ALARM_REQUEST_SHUTDOWN_ACTION = "android.intent.action.ACTION_ALARM_REQUEST_SHUTDOWN";
}
