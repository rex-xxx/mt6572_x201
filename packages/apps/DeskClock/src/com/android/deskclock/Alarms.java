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

package com.android.deskclock;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Parcel;
import android.os.SystemProperties;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.Time;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import android.preference.PreferenceManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.widget.Toast;
import java.util.Calendar;

import com.mediatek.common.featureoption.FeatureOption;
/**
 * The Alarms provider supplies info about Alarm Clock settings
 */
public class Alarms {

    static final String PREFERENCES = "AlarmClock";
    static final String NEAREST_ALARM_PREFERENCES = "NearestAlarm";
    // This action triggers the AlarmReceiver as well as the AlarmKlaxon. It
    // is a public action used in the manifest for receiving Alarm broadcasts
    // from the alarm manager.
    public static final String ALARM_ALERT_ACTION = "com.android.deskclock.ALARM_ALERT";

    // A public action sent by AlarmKlaxon when the alarm has stopped sounding
    // for any reason (e.g. because it has been dismissed from AlarmAlertFullScreen,
    // or killed due to an incoming phone call, etc).
    public static final String ALARM_DONE_ACTION = "com.android.deskclock.ALARM_DONE";

    // AlarmAlertFullScreen listens for this broadcast intent, so that other applications
    // can snooze the alarm (after ALARM_ALERT_ACTION and before ALARM_DONE_ACTION).
    public static final String ALARM_SNOOZE_ACTION = "com.android.deskclock.ALARM_SNOOZE";

    // AlarmAlertFullScreen listens for this broadcast intent, so that other applications
    // can dismiss the alarm (after ALARM_ALERT_ACTION and before ALARM_DONE_ACTION).
    public static final String ALARM_DISMISS_ACTION = "com.android.deskclock.ALARM_DISMISS";

    // A public action sent by AlarmAlertFullScreen when a snoozed alarm was dismissed due
    // to it handling ALARM_DISMISS_ACTION cancelled
    public static final String ALARM_SNOOZE_CANCELLED = "com.android.deskclock.ALARM_SNOOZE_CANCELLED";

    // A broadcast sent every time the next alarm time is set in the system
    public static final String NEXT_ALARM_TIME_SET = "com.android.deskclock.NEXT_ALARM_TIME_SET";

    // This is a private action used by the AlarmKlaxon to update the UI to
    // show the alarm has been killed.
    public static final String ALARM_KILLED = "alarm_killed";

    // This is a private action used by the AlarmReceiver to update the Notification
    public static final String ALARM_CHANGE_NOTIFICATION = "alarm_change_notification";

    // This is a public action used by the AlarmReceiver to update the enable or disable snooze Notification
    public static final String DISABLE_OR_ENABLE_SNOOZE_NOTIFICATION = "disable_or_enable_snooze_notification";

    // Extra in the ALARM_KILLED intent to indicate to the user how long the
    // alarm played before being killed.
    public static final String ALARM_KILLED_TIMEOUT = "alarm_killed_timeout";

    // Extra in the ALARM_KILLED intent to indicate when alarm was replaced
    public static final String ALARM_REPLACED = "alarm_replaced";

    // This string is used to indicate a silent alarm in the db.
    public static final String ALARM_ALERT_SILENT = "silent";

    // This intent is sent from the notification when the user cancels the
    // snooze alert.
    public static final String CANCEL_SNOOZE = "cancel_snooze";

    // This string is used when passing an Alarm object through an intent.
    public static final String ALARM_INTENT_EXTRA = "intent.extra.alarm";

    // This extra is the raw Alarm object data. It is used in the
    // AlarmManagerService to avoid a ClassNotFoundException when filling in
    // the Intent extras.
    public static final String ALARM_RAW_DATA = "intent.extra.alarm_raw";
    public static final String PLAYING_ALARM_ID = "playing_alarm_id";
    public static final String PLAYING_ALARM_START_TIME = "playing_alarm_start_time";
    public static final boolean SUPPORT_SDSWAP = FeatureOption.MTK_2SDCARD_SWAP;
    private static final String PREF_SNOOZE_IDS = "snooze_ids";
    private static final String PREF_SNOOZE_TIME = "snooze_time";
    private static final String DEFAULT_SNOOZE = "10";
    private final static String DM12 = "E h:mm aa";
    private final static String DM24 = "E kk:mm";

    private final static String M12 = "h:mm aa";
    // Shared with DigitalClock
    final static String M24 = "kk:mm";

    final static int INVALID_ALARM_ID = -1;

    public static final int POWER_OFF_WAKE_UP = 8;

    static final String PREF_NEAREST_ALARM_ID = "nearest_id";
    static final String PREF_NEAREST_ALARM_TIME = "nearest_time";

    /**
     * Creates a new Alarm and fills in the given alarm's id.
     */
    public static long addAlarm(Context context, Alarm alarm) {
        ContentValues values = createContentValues(context, alarm);
        Uri uri = context.getContentResolver().insert(
                Alarm.Columns.CONTENT_URI, values);
        alarm.id = (int) ContentUris.parseId(uri);

        long timeInMillis = calculateAlarm(alarm);
        if (alarm.enabled) {
            clearSnoozeIfNeeded(context, timeInMillis);
        }
        setNextAlert(context);
        return timeInMillis;
    }

    /**
     * Removes an existing Alarm.  If this alarm is snoozing, disables
     * snooze.  Sets next alert.
     */
    public static void deleteAlarm(Context context, int alarmId) {
        if (alarmId == INVALID_ALARM_ID) return;
        if (AlarmKlaxon.mCurrentPlayingAlarmId != -1
                && alarmId == AlarmKlaxon.mCurrentPlayingAlarmId) {
            final Alarm a = getAlarm(context.getContentResolver(), alarmId);
            Intent disableSnoozeNotification = new Intent(DISABLE_OR_ENABLE_SNOOZE_NOTIFICATION);
            disableSnoozeNotification.putExtra(Alarms.ALARM_INTENT_EXTRA, a);
            disableSnoozeNotification.putExtra("enable", false);
            disableSnoozeNotification.putExtra("alarmID", alarmId);
            disableSnoozeNotification.putExtra("alarm", a);
            context.sendBroadcast(disableSnoozeNotification);
        }



        ContentResolver contentResolver = context.getContentResolver();
        /* If alarm is snoozing, lose it */
        disableSnoozeAlert(context, alarmId);

        Uri uri = ContentUris.withAppendedId(Alarm.Columns.CONTENT_URI, alarmId);
        contentResolver.delete(uri, "", null);

        setNextAlert(context);
    }

    public static CursorLoader getAlarmsCursorLoader(Context context) {
        return new CursorLoader(context, Alarm.Columns.CONTENT_URI,
                Alarm.Columns.ALARM_QUERY_COLUMNS, null, null, Alarm.Columns.DEFAULT_SORT_ORDER);
    }

    /**
     * Queries all alarms
     * @return cursor over all alarms
     */
    public static Cursor getAlarmsCursor(ContentResolver contentResolver) {
        return contentResolver.query(
                Alarm.Columns.CONTENT_URI, Alarm.Columns.ALARM_QUERY_COLUMNS,
                null, null, Alarm.Columns.DEFAULT_SORT_ORDER);
    }

    // Private method to get a more limited set of alarms from the database.
    private static Cursor getFilteredAlarmsCursor(
            ContentResolver contentResolver) {
        return contentResolver.query(Alarm.Columns.CONTENT_URI,
                Alarm.Columns.ALARM_QUERY_COLUMNS, Alarm.Columns.WHERE_ENABLED,
                null, null);
    }

    private static ContentValues createContentValues(Context ctx, Alarm alarm) {
        ContentValues values = new ContentValues(8);
        // Set the alarm_time value if this alarm does not repeat. This will be
        // used later to disable expire alarms.
        long time = 0;
        if (!alarm.daysOfWeek.isRepeatSet()) {
            time = calculateAlarm(alarm);
        }

        // -1 means generate new id.
        if (alarm.id != -1) {
            values.put(Alarm.Columns._ID, alarm.id);
        }

        values.put(Alarm.Columns.ENABLED, alarm.enabled ? 1 : 0);
        values.put(Alarm.Columns.HOUR, alarm.hour);
        values.put(Alarm.Columns.MINUTES, alarm.minutes);
        values.put(Alarm.Columns.ALARM_TIME, time);
        values.put(Alarm.Columns.DAYS_OF_WEEK, alarm.daysOfWeek.getCoded());
        values.put(Alarm.Columns.VIBRATE, alarm.vibrate);
        values.put(Alarm.Columns.MESSAGE, alarm.label);

        // A null alert Uri indicates a silent alarm.
        values.put(Alarm.Columns.ALERT, alarm.alert == null ? ALARM_ALERT_SILENT
                : alarm.alert.toString());
        if(alarm.alert != null && alarm.alert.toString().contains("external")){
            Log.v(" external uri = " + alarm.alert.toString());
            values.put(Alarm.Columns.PATH, getExternalRingtonePath(ctx, alarm.alert));
        }
        /// M: add for playing external ringtone for power-off alarm
        Alarms.backupRingtoneForPoweroffAlarm(ctx, null);
        return values;
    }

    private static String getExternalRingtonePath(Context ctx, Uri uri) {
        Cursor c = null;
        String path = null;
        Log.v("ringtone uri = " + uri == null ? "" : uri.toString());
        try {
            c = ctx.getContentResolver().query(uri, new String[]{MediaStore.Audio.Media.DATA}, null, null, null);
            if(c != null && c.moveToFirst()){
                path = c.getString(0);
                Log.v("external path = " + path == null ? "" : path);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return path;
    }

    private static void clearSnoozeIfNeeded(Context context, long alarmTime) {
        // If this alarm fires before the next snooze, clear the snooze to
        // enable this alarm.
        SharedPreferences prefs = context.getSharedPreferences(PREFERENCES, 0);

        // Get the list of snoozed alarms
        final Set<String> snoozedIds = prefs.getStringSet(PREF_SNOOZE_IDS, new HashSet<String>());
        final Set<String> snoozedIdsForCopy = new HashSet<String>();
        snoozedIdsForCopy.addAll(snoozedIds);
        for (String snoozedAlarm : snoozedIdsForCopy) {
            final long snoozeTime = prefs.getLong(getAlarmPrefSnoozeTimeKey(snoozedAlarm), 0);
            if (alarmTime < snoozeTime) {
                final int alarmId = Integer.parseInt(snoozedAlarm);
                clearSnoozePreference(context, prefs, alarmId);
            }
        }
    }

    /**
     * Return an Alarm object representing the alarm id in the database.
     * Returns null if no alarm exists.
     */
    public static Alarm getAlarm(ContentResolver contentResolver, int alarmId) {
        Cursor cursor = contentResolver.query(
                ContentUris.withAppendedId(Alarm.Columns.CONTENT_URI, alarmId),
                Alarm.Columns.ALARM_QUERY_COLUMNS,
                null, null, null);
        Alarm alarm = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                alarm = new Alarm(cursor);
            }
            cursor.close();
        }
        return alarm;
    }


    /**
     * A convenience method to set an alarm in the Alarms
     * content provider.
     * @return Time when the alarm will fire. Or < 1 if update failed.
     */
    public static long setAlarm(Context context, Alarm alarm) {
        ContentValues values = createContentValues(context, alarm);
        ContentResolver resolver = context.getContentResolver();
        long rowsUpdated = resolver.update(
                ContentUris.withAppendedId(Alarm.Columns.CONTENT_URI, alarm.id),
                values, null, null);
        if (rowsUpdated < 1) {
            Log.e("Error updating alarm " + alarm);
            return rowsUpdated;
        }

        long timeInMillis = calculateAlarm(alarm);

        if (alarm.enabled) {
            // Disable the snooze if we just changed the snoozed alarm. This
            // only does work if the snoozed alarm is the same as the given
            // alarm.
            // TODO: disableSnoozeAlert should have a better name.
            disableSnoozeAlert(context, alarm.id);

            // Disable the snooze if this alarm fires before the snoozed alarm.
            // This works on every alarm since the user most likely intends to
            // have the modified alarm fire next.
            clearSnoozeIfNeeded(context, timeInMillis);
        }

        setNextAlert(context);

        return timeInMillis;
    }

    /**
     * A convenience method to enable or disable an alarm.
     *
     * @param id             corresponds to the _id column
     * @param enabled        corresponds to the ENABLED column
     */

    public static void enableAlarm(
            final Context context, final int id, boolean enabled) {
        enableAlarmInternal(context, id, enabled);
        setNextAlert(context);
    }

    private static void enableAlarmInternal(final Context context,
            final int id, boolean enabled) {
        enableAlarmInternal(context, getAlarm(context.getContentResolver(), id),
                enabled);
    }

    private static void enableAlarmInternal(final Context context,
            final Alarm alarm, boolean enabled) {
        if (alarm == null) {
            return;
        }
        ContentResolver resolver = context.getContentResolver();

        ContentValues values = new ContentValues(2);
        values.put(Alarm.Columns.ENABLED, enabled ? 1 : 0);

        // If we are enabling the alarm, calculate alarm time since the time
        // value in Alarm may be old.
        if (enabled) {
            long time = 0;
            if (!alarm.daysOfWeek.isRepeatSet()) {
                time = calculateAlarm(alarm);
            }
            values.put(Alarm.Columns.ALARM_TIME, time);
        } else {
            // Clear the snooze if the id matches.
            disableSnoozeAlert(context, alarm.id);
        }

        resolver.update(ContentUris.withAppendedId(
                Alarm.Columns.CONTENT_URI, alarm.id), values, null, null);
    }

    /**
     * A  method to save the alarm.time to DB
     *
     * @param context      the context
     * @param alarm        the target alarm
     * @param time         the alarm.time to save in DB
     */

    private static void saveAlarmTime(final Context context,
            final Alarm alarm, long time) {

        ContentResolver resolver = context.getContentResolver();

        ContentValues values = new ContentValues(2);
        values.put(Alarm.Columns.ALARM_TIME, time);

        resolver.update(ContentUris.withAppendedId(
                Alarm.Columns.CONTENT_URI, alarm.id), values, null, null);
    }

    private static Alarm calculateNextAlert(final Context context) {
        long minTime = Long.MAX_VALUE;
        long now = System.currentTimeMillis();
        final SharedPreferences prefs = context.getSharedPreferences(PREFERENCES, 0);

        Set<Alarm> alarms = new HashSet<Alarm>();

        // We need to to build the list of alarms from both the snoozed list and the scheduled
        // list.  For a non-repeating alarm, when it goes of, it becomes disabled.  A snoozed
        // non-repeating alarm is not in the active list in the database.

        // first go through the snoozed alarms
        final Set<String> snoozedIds = prefs.getStringSet(PREF_SNOOZE_IDS, new HashSet<String>());
        for (String snoozedAlarm : snoozedIds) {
            final int alarmId = Integer.parseInt(snoozedAlarm);
            final Alarm a = getAlarm(context.getContentResolver(), alarmId);
            alarms.add(a);
        }

        // Now add the scheduled alarms
        final Cursor cursor = getFilteredAlarmsCursor(context.getContentResolver());
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    do {
                        final Alarm a = new Alarm(cursor);
                        alarms.add(a);
                    } while (cursor.moveToNext());
                }
            } finally {
                cursor.close();
            }
        }

        Alarm alarm = null;

        for (Alarm a : alarms) {
            // A time of 0 indicates this is a repeating alarm, so
            // calculate the time to get the next alert.
            if (a.time == 0) {
                a.time = calculateAlarm(a);
                if (!a.daysOfWeek.isRepeatSet()) {
                    saveAlarmTime(context, a, a.time);
                }
            }

            // Update the alarm if it has been snoozed
            updateAlarmTimeForSnooze(prefs, a);

            if (a.time < now) {
                Log.v("Disabling expired alarm set for " + Log.formatTime(a.time));
                // Expired alarm, disable it and move along.
                enableAlarmInternal(context, a, false);
                continue;
            }
            if (a.time < minTime) {
                minTime = a.time;
                alarm = a;
            }
        }

        return alarm;
    }

    /**
     * Disables non-repeating alarms that have passed.  Called at
     * boot.
     */
    public static void disableExpiredAlarms(final Context context) {
        Cursor cur = getFilteredAlarmsCursor(context.getContentResolver());
        long now = System.currentTimeMillis();

        try {
            if (cur.moveToFirst()) {
                do {
                    Alarm alarm = new Alarm(cur);
                    // A time of 0 means this alarm repeats. If the time is
                    // non-zero, check if the time is before now.
                    if (alarm.time != 0 && alarm.time < now) {
                        Log.v("Disabling expired alarm set for " +
                              Log.formatTime(alarm.time));
                        enableAlarmInternal(context, alarm, false);
                    }
                } while (cur.moveToNext());
            }
        } finally {
            cur.close();
        }
    }

    /**
     * Called at system startup, on time/timezone change, and whenever
     * the user changes alarm settings.  Activates snooze if set,
     * otherwise loads all alarms, activates next alert.
     */
    public static void setNextAlert(final Context context) {
        final Alarm alarm = calculateNextAlert(context);
        if (alarm != null) {
            enableAlert(context, alarm, alarm.time, false);
        } else {
            disableAlert(context);
        }
        Intent i = new Intent(NEXT_ALARM_TIME_SET);
        context.sendBroadcast(i);
    }

    /**
     * @param context
     * @param disablePoweroffAlarm
     */
    public static void setNextAlert(final Context context, boolean disablePoweroffAlarm){
        final Alarm alarm = calculateNextAlert(context);
        if (alarm != null) {
            enableAlert(context, alarm, alarm.time, disablePoweroffAlarm);
        } else {
            disableAlert(context);
        }
        Intent i = new Intent(NEXT_ALARM_TIME_SET);
        context.sendBroadcast(i);
    }

    /**
     * Sets alert in AlarmManger and StatusBar.  This is what will
     * actually launch the alert when the alarm triggers.
     *
     * @param alarm Alarm.
     * @param atTimeInMillis milliseconds since epoch
     */
    private static void enableAlert(Context context, final Alarm alarm,
            final long atTimeInMillis, boolean disablePoweroffAlarm) {
        AlarmManager am = (AlarmManager)
                context.getSystemService(Context.ALARM_SERVICE);

        // Intentionally verbose: always log the alarm time to provide useful
        // information in bug reports.
        Log.v("Alarm set for id=" + alarm.id + " " + Log.formatTime(atTimeInMillis));

        Intent intent = new Intent(ALARM_ALERT_ACTION);

        // XXX: This is a slight hack to avoid an exception in the remote
        // AlarmManagerService process. The AlarmManager adds extra data to
        // this Intent which causes it to inflate. Since the remote process
        // does not know about the Alarm class, it throws a
        // ClassNotFoundException.
        //
        // To avoid this, we marshall the data ourselves and then parcel a plain
        // byte[] array. The AlarmReceiver class knows to build the Alarm
        // object from the byte[] array.
        Parcel out = Parcel.obtain();
        alarm.writeToParcel(out, 0);
        out.setDataPosition(0);
        intent.putExtra(ALARM_RAW_DATA, out.marshall());

        PendingIntent sender = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        //changed by MTK start
        // disable power-off alarm when device encrypted.
        if (!disablePoweroffAlarm && "unencrypted".equals(SystemProperties.get("ro.crypto.state"))) {
            // not enabled device encrypt, enable power-off alarm
            am.set(POWER_OFF_WAKE_UP, atTimeInMillis, sender);
        } else {
            // enabled device encrypt, use google default design
            am.set(AlarmManager.RTC_WAKEUP, atTimeInMillis, sender);
        }
        //changed by MTK end
        storeNearestAlarm(context, alarm);
        //am.set(AlarmManager.RTC_WAKEUP, atTimeInMillis, sender);

        setStatusBarIcon(context, true);

        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(atTimeInMillis);
        String timeString = formatDayAndTime(context, c);
        saveNextAlarm(context, timeString);
    }

    /**
     * Disables alert in AlarmManager and StatusBar.
     *
     * @param context The context
     */
    static void disableAlert(Context context) {
        AlarmManager am = (AlarmManager)
                context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent sender = PendingIntent.getBroadcast(
                context, 0, new Intent(ALARM_ALERT_ACTION),
                PendingIntent.FLAG_CANCEL_CURRENT);
        am.cancel(sender);
        am.cancelPoweroffAlarm(context.getPackageName());
        setStatusBarIcon(context, false);
        // Intentionally verbose: always log the lack of a next alarm to provide useful
        // information in bug reports.
        Log.v("No next alarm");
        saveNextAlarm(context, "");
    }

    static void savePlayingAlarmID(final Context context, final int id) {
        SharedPreferences prefs = context.getSharedPreferences(PREFERENCES, 0);
        final SharedPreferences.Editor ed = prefs.edit();
        ed.putInt(PLAYING_ALARM_ID, id);
        ed.apply();
    }

    static void savePlayingAlarmStartTime(final Context context, final long startTime) {
        SharedPreferences prefs = context.getSharedPreferences(PREFERENCES, 0);
        final SharedPreferences.Editor ed = prefs.edit();
        ed.putLong(PLAYING_ALARM_START_TIME, startTime);
        ed.apply();
    }

    static void saveSnoozeAlert(final Context context, final int id,
            final long time) {
        SharedPreferences prefs = context.getSharedPreferences(PREFERENCES, 0);
        if (id == INVALID_ALARM_ID) {
            clearAllSnoozePreferences(context, prefs);
        } else {
            final Set<String> snoozedIds =
                    prefs.getStringSet(PREF_SNOOZE_IDS, new HashSet<String>());
            snoozedIds.add(Integer.toString(id));
            final SharedPreferences.Editor ed = prefs.edit();
            ed.putStringSet(PREF_SNOOZE_IDS, snoozedIds);
            ed.putLong(getAlarmPrefSnoozeTimeKey(id), time);
            ed.apply();
        }
        // Set the next alert after updating the snooze.
        setNextAlert(context);
    }

    private static String getAlarmPrefSnoozeTimeKey(int id) {
        return getAlarmPrefSnoozeTimeKey(Integer.toString(id));
    }

    private static String getAlarmPrefSnoozeTimeKey(String id) {
        return PREF_SNOOZE_TIME + id;
    }

    /**
     * Disable the snooze alert if the given id matches the snooze id.
     */
    static void disableSnoozeAlert(final Context context, final int id) {
        SharedPreferences prefs = context.getSharedPreferences(PREFERENCES, 0);
        if (hasAlarmBeenSnoozed(prefs, id)) {
            // This is the same id so clear the shared prefs.
            clearSnoozePreference(context, prefs, id);
        }
    }

    // Helper to remove the snooze preference. Do not use clear because that
    // will erase the clock preferences. Also clear the snooze notification in
    // the window shade.
    private static void clearSnoozePreference(final Context context,
            final SharedPreferences prefs, final int id) {
        final String alarmStr = Integer.toString(id);
        final Set<String> snoozedIds =
                prefs.getStringSet(PREF_SNOOZE_IDS, new HashSet<String>());
        if (snoozedIds.contains(alarmStr)) {
            NotificationManager nm = (NotificationManager)
                    context.getSystemService(Context.NOTIFICATION_SERVICE);
            nm.cancel(id);
        }

        final SharedPreferences.Editor ed = prefs.edit();
        snoozedIds.remove(alarmStr);
        ed.putStringSet(PREF_SNOOZE_IDS, snoozedIds);
        ed.remove(getAlarmPrefSnoozeTimeKey(alarmStr));
        ed.apply();
    }

    private static void clearAllSnoozePreferences(final Context context,
            final SharedPreferences prefs) {
        NotificationManager nm = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        final Set<String> snoozedIds =
                prefs.getStringSet(PREF_SNOOZE_IDS, new HashSet<String>());
        final SharedPreferences.Editor ed = prefs.edit();
        for (String snoozeId : snoozedIds) {
            nm.cancel(Integer.parseInt(snoozeId));
            ed.remove(getAlarmPrefSnoozeTimeKey(snoozeId));
        }

        ed.remove(PREF_SNOOZE_IDS);
        ed.apply();
    }

    private static boolean hasAlarmBeenSnoozed(final SharedPreferences prefs, final int alarmId) {
        final Set<String> snoozedIds = prefs.getStringSet(PREF_SNOOZE_IDS, null);

        // Return true if there a valid snoozed alarmId was saved
        return snoozedIds != null && snoozedIds.contains(Integer.toString(alarmId));
    }

    /**
     * Updates the specified Alarm with the additional snooze time.
     * Returns a boolean indicating whether the alarm was updated.
     */
    private static boolean updateAlarmTimeForSnooze(
            final SharedPreferences prefs, final Alarm alarm) {
        if (!hasAlarmBeenSnoozed(prefs, alarm.id)) {
            // No need to modify the alarm
            return false;
        }

        final long time = prefs.getLong(getAlarmPrefSnoozeTimeKey(alarm.id), -1);
        // The time in the database is either 0 (repeating) or a specific time
        // for a non-repeating alarm. Update this value so the AlarmReceiver
        // has the right time to compare.
        alarm.time = time;

        return true;
    }

    /**
     * Tells the StatusBar whether the alarm is enabled or disabled
     */
    static void setStatusBarIcon(Context context, boolean enabled) {
        Intent alarmChanged = new Intent("android.intent.action.ALARM_CHANGED");
        alarmChanged.putExtra("alarmSet", enabled);
        context.sendBroadcast(alarmChanged);
    }

    private static long calculateAlarm(Alarm alarm) {
        return calculateAlarm(alarm.hour, alarm.minutes, alarm.daysOfWeek)
                .getTimeInMillis();
    }

    /**
     * Given an alarm in hours and minutes, return a time suitable for
     * setting in AlarmManager.
     */
    static Calendar calculateAlarm(int hour, int minute,
            Alarm.DaysOfWeek daysOfWeek) {

        // start with now
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis());

        int nowHour = c.get(Calendar.HOUR_OF_DAY);
        int nowMinute = c.get(Calendar.MINUTE);

        // if alarm is behind current time, advance one day
        if (hour < nowHour  ||
            hour == nowHour && minute <= nowMinute) {
            c.add(Calendar.DAY_OF_YEAR, 1);
        }
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        int addDays = daysOfWeek.getNextAlarm(c);
        if (addDays > 0) c.add(Calendar.DAY_OF_WEEK, addDays);
        return c;
    }

    static String formatTime(final Context context, int hour, int minute,
                             Alarm.DaysOfWeek daysOfWeek) {
        Calendar c = calculateAlarm(hour, minute, daysOfWeek);
        return formatTime(context, c);
    }

    /* used by AlarmAlert */
    static String formatTime(final Context context, Calendar c) {
        String format = get24HourMode(context) ? M24 : M12;
        return (c == null) ? "" : (String)DateFormat.format(format, c);
    }

    /**
     * Shows day and time -- used for lock screen
     */
    private static String formatDayAndTime(final Context context, Calendar c) {
        String format = get24HourMode(context) ? DM24 : DM12;
        return (c == null) ? "" : (String)DateFormat.format(format, c);
    }

    /**
     * Save time of the next alarm, as a formatted string, into the system
     * settings so those who care can make use of it.
     */
    static void saveNextAlarm(final Context context, String timeString) {
        Settings.System.putString(context.getContentResolver(),
                                  Settings.System.NEXT_ALARM_FORMATTED,
                                  timeString);
    }

    /**
     * @return true if clock is set to 24-hour mode
     */
    public static boolean get24HourMode(final Context context) {
        return android.text.format.DateFormat.is24HourFormat(context);
    }

    /**
     * Store the nearest alarm into preference.
     *
     * @param alarm Alarm to stored.
     * @param context context.
     */
    private static void storeNearestAlarm(final Context context, final Alarm alarm) {
        Log.v("alarm id = " + alarm.id);
        if (alarm.id == -1) {
            return;
        } else {
            SharedPreferences prefs = context.getSharedPreferences(
                    NEAREST_ALARM_PREFERENCES, 0);
            int alarmId = prefs.getInt(PREF_NEAREST_ALARM_ID, 0);
            long time = prefs.getLong(PREF_NEAREST_ALARM_TIME, 0);
            if (alarm.id == alarmId && time == alarm.time) {
                return;
            }
            SharedPreferences.Editor ed = prefs.edit();
            ed.clear();
            ed.putInt(PREF_NEAREST_ALARM_ID, alarm.id);
            ed.putLong(PREF_NEAREST_ALARM_TIME, alarm.time);
            ed.apply();
        }
    }

    /**
     * Get the nearest alarm from preference file.
     *
     * @param context
     * @return the nearest alarm object, if not set, return null.
     */
    public static Alarm getNearestAlarm(final Context context) {
        SharedPreferences prefs = context.getSharedPreferences(
                NEAREST_ALARM_PREFERENCES, 0);
        int alarmId = prefs.getInt(PREF_NEAREST_ALARM_ID, -1);

        if (alarmId == -1) {
            return null;
        }

        ContentResolver cr = context.getContentResolver();
        return Alarms.getAlarm(cr, alarmId);
    }

    /**
     * Get the alert time of the nearest alarm.
     *
     * @param context
     * @return the nearest alarm alert time, if not set, return -1.
     */
    public static long getNearestAlarmTime(final Context context) {
        SharedPreferences prefs = context.getSharedPreferences(
                NEAREST_ALARM_PREFERENCES, 0);
        return prefs.getLong(PREF_NEAREST_ALARM_TIME, -1);
    }

    /**
     * Get a formatted string for the given time.
     *
     * @param now
     * @return
     */
    private static String getTimeString(long now) {
        Time time = new Time();
        time.set(now);
        return (time.format("%b %d %I:%M:%S %p"));
    }

    /**
     * Whether this boot is from power off alarm or schedule power on or normal boot.
     *
     * @param context
     * @return
     */
    static boolean bootFromPoweroffAlarm() {
        String bootReason = SystemProperties.get("sys.boot.reason");
        boolean ret = (bootReason != null && bootReason.equals("1")) ? true : false;
        Log.v("bootFromPoweroffAlarm ret is " + ret);
        return ret;
    }

    /**
     * Clear all snoozed alarms but do not setNextAlert.
     *
     * @param context
     */
    static void disableAllSnoozedAlarms(final Context context) {
        SharedPreferences prefs = context.getSharedPreferences(
                PREFERENCES, 0);
        clearAllSnoozePreferences(context, prefs);
    }

    /**
     * Reset the alarm time to 0 when TIME_SET or TIMEZONE_CHANGED.
     *
     * @param context
     */
    static void resetAlarmTimes(Context context) {
        final Cursor cursor = getFilteredAlarmsCursor(context.getContentResolver());
        if (cursor != null) {
            ContentResolver resolver = context.getContentResolver();

            ContentValues values = new ContentValues();
            values.put(Alarm.Columns.ALARM_TIME, 0);

            try {
                if (cursor.moveToFirst()) {
                    do {
                        final Alarm alarm = new Alarm(cursor);
                        if (alarm.time != 0) {
                            resolver.update(ContentUris.withAppendedId(Alarm.Columns.CONTENT_URI,
                                    alarm.id), values, null, null);
                        }
                    } while (cursor.moveToNext());
                }
            } finally {
                cursor.close();
            }
        }
    }

    /** M:
     * copy ringtone music file to local from sd-card, to avoid power-off alarm
     * could not load the user set ringtone music. if have existed not the same
     * ringtone based on the file name then delete and copy the new one
     * there.
     *
     * @author mtk54296
     * @param uri
     *            ringtone uri
     *            @{ */
    static void backupRingtoneForPoweroffAlarm(final Context ctx, final Handler handler) {
        final ContentResolver cr = ctx.getContentResolver();
        Log.v("backupRingtoneForPoweroffalarm ...... ");
        new Thread() {
            public void run() {
                String filepath = null;
                File existedRingtone = null;
                File files = ctx.getFilesDir();
                String nextRingtone = null;
                nextRingtone = getNearestAlarmWithExternalRingtone(ctx);
                Log.v("nextRingtone: " + nextRingtone);
                if (nextRingtone != null) {
                    if (files.isDirectory() && files.list().length == 1) {
                        for (File item : files.listFiles()) {
                            existedRingtone = item;
                        }
                    }
                    String existedRingtoneName = existedRingtone == null ? null
                            : existedRingtone.getName();
                    Log.v("existedRingtoneName: " + existedRingtoneName);
                    if (!TextUtils.isEmpty(nextRingtone)
                            && (existedRingtoneName == null || !TextUtils
                                    .isEmpty(existedRingtoneName)
                                    && !nextRingtone
                                            .contains(existedRingtoneName))) {
                        if (existedRingtone != null) {
                            existedRingtone.delete();
                        }
                        try {
                            Cursor c = cr.query(Uri.parse(nextRingtone), null,
                                    null, null, null);
                            if (c != null && c.moveToFirst()) {
                                filepath = c.getString(1);
                                c.close();
                            }
                        } catch (Exception e) {
                            Log.v("database operation error: " + e.getMessage());
                        }
                        if (filepath != null) {
                            // copy from sd-card to local files directory.
                            String target = files.getAbsolutePath()
                                    + File.separator
                                    + nextRingtone.substring(nextRingtone
                                            .lastIndexOf(File.separator) + 1);
                            copyFile(filepath, target);
                        }
                    }
                }
                Log.v("handler =" + handler);
                if (handler != null) {
                    handler.sendEmptyMessage(0);
                }
            }
        }.start();
    }
    /** @}   */
    /** M:
     * get the next will play alarm, whose ringtone is from external storage
     *
     * @param cr
     * @return
     *       @{  */
    private static String getNearestAlarmWithExternalRingtone(Context ctx) {
        Cursor c = null;
        String alert = null;
        try {
            Alarm nextAlarm = Alarms.getNearestAlarm(ctx);
            if (nextAlarm != null && nextAlarm.alert != null
                    && nextAlarm.alert.toString().contains("external")) {
                alert = nextAlarm.alert.toString();
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return alert;
    }
    /**  @} */
    /** M:
     * copy one file from source to target
     *
     * @author mtk54296
     * @param from
     *            source
     * @param to
     *            target
     * @return
     *              @{ */
    private static int copyFile(String from, String to) {
        Log.v("source: " + from + "  target: " + to);
        int result = 0;
        if (TextUtils.isEmpty(from) || TextUtils.isEmpty(to)) {
            result = -1;
        }
        Log.v("media mounted: " + Environment.getExternalStorageState());
        if (Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            java.io.InputStream fis = null;
            java.io.OutputStream fos = null;
            try {
                fis = new java.io.FileInputStream(from);
                fos = new java.io.FileOutputStream(to);
                byte bt[] = new byte[1024];
                int c;
                while ((c = fis.read(bt)) > 0) {
                    fos.write(bt, 0, c);
                }
                fos.flush();
                fos.close();
                fis.close();
            } catch (IOException e) {
                e.printStackTrace();
                Log.v("copy ringtone file error: " + e.toString());
                result = -1;
            }
        }
        return result;
    }
    /** @}   */
   static void snooze(Context context, Alarm alarm) {
        final String snooze =
                PreferenceManager.getDefaultSharedPreferences(context)
                .getString(SettingsActivity.KEY_ALARM_SNOOZE, DEFAULT_SNOOZE);
        int snoozeMinutes = Integer.parseInt(snooze);

        final long snoozeTime = System.currentTimeMillis()
                + ((long)1000 * 60 * snoozeMinutes);
        saveSnoozeAlert(context, alarm.id, snoozeTime);

        // Get the display time for the snooze and update the notification.
        final Calendar c = Calendar.getInstance();
        c.setTimeInMillis(snoozeTime);
        String snoozeTimeStr = Alarms.formatTime(context, c);
        String label = alarm.getLabelOrDefault(context);

        // Notify the user that the alarm has been snoozed.
        Intent dismissIntent = new Intent(context, AlarmReceiver.class);
        dismissIntent.setAction(Alarms.CANCEL_SNOOZE);
        dismissIntent.putExtra(Alarms.ALARM_INTENT_EXTRA, alarm);

        Intent openAlarm = new Intent(context, DeskClock.class);
        openAlarm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        openAlarm.putExtra(Alarms.ALARM_INTENT_EXTRA, alarm);
        openAlarm.putExtra(DeskClock.SELECT_TAB_INTENT_EXTRA, DeskClock.CLOCK_TAB_INDEX);

        NotificationManager nm = getNotificationManager(context);
        Notification notif = new Notification.Builder(context.getApplicationContext())
        .setContentTitle(label)
        .setContentText(context.getResources().getString(R.string.alarm_alert_snooze_until, snoozeTimeStr))
        .setSmallIcon(R.drawable.stat_notify_alarm)
        .setOngoing(true)
        .setAutoCancel(false)
        .setPriority(Notification.PRIORITY_MAX)
        .setWhen(0)
        .addAction(android.R.drawable.ic_menu_close_clear_cancel,
                context.getResources().getString(R.string.alarm_alert_dismiss_text),
                PendingIntent.getBroadcast(context, alarm.id, dismissIntent, 0))
        .build();
        notif.contentIntent = PendingIntent.getActivity(context, alarm.id, openAlarm, 0);
        nm.notify(alarm.id, notif);
        ///M: @}
        String displayTime = context.getString(R.string.alarm_alert_snooze_set,
                snoozeMinutes);
        // Intentionally log the snooze time for debugging.
        Log.v(displayTime);

        // Display the snooze minutes in a toast.
        Toast.makeText(context, displayTime,
                Toast.LENGTH_LONG).show();
        context.stopService(new Intent(Alarms.ALARM_ALERT_ACTION));
        context.stopService(new Intent("com.android.deskclock.ALARM_PHONE_LISTENER"));
    }

    static private NotificationManager getNotificationManager(Context context) {
        return (NotificationManager) context.getSystemService("notification");
    }

    // Dismiss the alarm.
    static void dismiss(Context context, Alarm alarm, boolean killed) {
        if (!killed) {
           stopPlayAlarm(context, alarm);
        }
        context.stopService(new Intent("com.android.deskclock.ALARM_PHONE_LISTENER"));
    }

    ///M: Cancel the notification and stop playing the alarm @{
    static void stopPlayAlarm(Context context,Alarm alarm) {
        NotificationManager nm = getNotificationManager(context);
        nm.cancel(alarm.id);
        context.stopService(new Intent(ALARM_ALERT_ACTION));
    }
    /// @}
}
