package com.android.deskclock;

import java.io.File;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;

public class CommonBroadcastReceiver extends BroadcastReceiver {
    private final String RESET_PWR_OFF_ALARM = "notify.deskclock.reset.alarms";
    public static final String PRE_SHUTDOWN = "android.intent.action.ACTION_PRE_SHUTDOWN";
    private static boolean mShutdownInProcess = false;

    @Override
    public void onReceive(final Context ctx, Intent intent) {
        // TODO Auto-generated method stub
        String action = intent.getAction();
        if(RESET_PWR_OFF_ALARM.equals(action)){
            AlarmManager am = (AlarmManager)ctx.getSystemService(Context.ALARM_SERVICE);
            am.cancelPoweroffAlarm(ctx.getPackageName());
            Alarms.setNextAlert(ctx, true);//set next alarm using RTC_WAKEUP
        }
        Log.v("is sdswap: " + Alarms.SUPPORT_SDSWAP);
        if (Alarms.SUPPORT_SDSWAP) {
            if (PRE_SHUTDOWN.equals(action)) {
                mShutdownInProcess = true;
            } else if (!mShutdownInProcess
                    && Intent.ACTION_MEDIA_SCANNER_FINISHED.equals(action)) {
                new Thread() {
                    public void run() {
                        Log.v("start run checking and update .... ");
                        // only update the uri when when user mount/umount Sd
                        // card manually or when system boot.
                        updateAlarmUri(ctx);
                        // the alarms.db has been alerted,call setNextAlert to
                        // update status.
                        Alarms.setNextAlert(ctx);
                    }
                }.start();
            }
        }
    }

    /**
     * /storage/sdcard0/Alarms/Treason.mp3
     * @param uri
     */
    private String checkFileExisting(ContentResolver cr, String oldPath){
        String alarmAlert = null;
        String fileName = oldPath.substring(16);
        Log.v("ringtone path = " + fileName);
        Cursor c = null;
        try{
            c = cr.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    new String[]{MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DATA },
                    MediaStore.Audio.Media.DATA + " like '%" + fileName + "%'", null, null);
            Log.v("ringtone size with same name " + (c == null ? 0 : c.getCount()));
            if (c != null && c.moveToFirst()) {
                while (!c.isAfterLast()) {
                    String fullPath = c.getString(1);
                    Log.v("fullpath = " + fullPath + "  _id =" + c.getInt(0));
                    if (!TextUtils.isEmpty(fullPath)) {
                        Uri ringtoneUri = ContentUris.withAppendedId(
                                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                c.getLong(0));
                        if (setRingtoneAsAlarm(cr, ringtoneUri) > 0) {
                            alarmAlert = ringtoneUri.toString();
                        }
                    }else{
                        alarmAlert = AlarmPreference.DEFAULT_RINGTONE_URI.toString();
                    }
                    c.moveToNext();
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }finally{
            if(c != null){
                c.close();
            }
        }
        return alarmAlert;
    }

    private void updateAlarmUri(Context ctx) {
        Cursor c = null;
        ContentResolver cr = ctx.getContentResolver();
        try {
            c = Alarms.getAlarmsCursor(cr);
            Log.v("cursor is with size = " + (c == null ? 0 : c.getCount()));
            if (c != null && c.moveToFirst()) {
                while (!c.isAfterLast()) {
                    Alarm alarm = new Alarm(c);
                    Log.v("update alarm alert and path ...  " + alarm.alert
                            + " path = " + alarm.path);
                    if (!TextUtils.isEmpty(alarm.path)) {
                        ContentValues values = new ContentValues();
                        values.put(Alarm.Columns.ALERT, checkFileExisting(cr, alarm.path));
                        values.put(Alarm.Columns.PATH, alarm.path);
                        cr.update(Uri.withAppendedPath(Alarm.Columns.CONTENT_URI, String.valueOf(alarm.id)), values, "", null);
                    }
                    c.moveToNext();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    protected static int setRingtoneAsAlarm(ContentResolver cr, Uri ringtoneUri){
        if(cr != null && ringtoneUri != null){
            ContentValues values = new ContentValues();
            values.put(MediaStore.Audio.Media.IS_ALARM, 1);
            return cr.update(ringtoneUri, values, null, null);
        }else{
            return -1;
        }
    }
}