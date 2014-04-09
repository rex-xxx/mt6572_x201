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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;

import com.android.deskclock.stopwatch.StopwatchFragment;
import com.android.deskclock.timer.TimerObj;

import java.io.File;

public class AlarmInitReceiver extends BroadcastReceiver {
    private static final String IPO_BOOT_ACTION = "android.intent.action.ACTION_BOOT_IPO";

    /// fix a known issue for power-off alarm ALPS00356932 @{
    private static final String PRE_SHUTDOWN = "android.intent.action.ACTION_PRE_SHUTDOWN";
    private final String mUriStr = "content://media/external/audio/media/";
    /// @}

    private static boolean mBlockTimeChange = false;

    // A flag that indicates that switching the volume button default was done
    private static final String PREF_VOLUME_DEF_DONE = "vol_def_done";
    private static boolean mBootIPO = false;

    /**
     * Sets alarm on ACTION_BOOT_COMPLETED.  Resets alarm on
     * TIME_SET, TIMEZONE_CHANGED
     */
    @Override
    public void onReceive(final Context context, Intent intent) {
        final String action = intent.getAction();
        Log.v("AlarmInitReceiver: action = " + action + ",mBlockTimeChange = " + mBlockTimeChange);

        /*
         * Note: Never call setNextAlert when the device is boot from power off
         * alarm, since it would make the power off alarm dismiss the wrong
         * alarm.
         */
        if (IPO_BOOT_ACTION.equals(action)) {
            Log.v("Receive android.intent.action.ACTION_BOOT_IPO intent.");
            mBlockTimeChange = true;
            mBootIPO = true;
            return;
        }
        /// fix a known issue for power-off alarm ALPS00356932 @{
        if (PRE_SHUTDOWN.equals(action)) {
            AsyncHandler.post(new Runnable() {
                @Override
                public void run() {
                    File dir = context.getFilesDir();
                    Log.v("base dir: " + dir.getAbsolutePath());
                    File[] files = dir.listFiles();
                    if (files != null && files.length > 0) {
                        File file = new File(files[0].getAbsolutePath());
                        if (file.exists() && file.getTotalSpace() > 0) {
                            Log.v("file name: " + file.getName() + "   "
                                    + mUriStr + file.getName());
                            Cursor c = null;
                            try {
                                c = context.getContentResolver().query(
                                        Uri.parse(mUriStr + file.getName()),
                                        null, null, null, null);
                                boolean fileRemoved = false;
                                if (c != null && c.getCount() > 0) {
                                    c.moveToFirst();
                                    File sdFile = new File(c.getString(c.getColumnIndex("_data")));
                                    if (!sdFile.exists()) {
                                        fileRemoved = true;
                                    }
                                }
                                if (fileRemoved || c != null
                                        && c.getCount() == 0) {
                                    file.delete();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            } finally {
                                if (c != null) {
                                    c.close();
                                }
                            }
                        }
                    }
                }
            });
        /// @}
            // Clear stopwatch and timers data
            SharedPreferences prefs =
                    PreferenceManager.getDefaultSharedPreferences(context);
            Log.v("AlarmInitReceiver PRE_SHUTDOWN - Cleaning old timer and stopwatch data");
            TimerObj.cleanTimersFromSharedPrefs(prefs);
            Utils.clearSwSharedPref(prefs);
            if (StopwatchFragment.mLapsAdapter != null) {
                StopwatchFragment.mLapsAdapter.clearLaps();
            }
        }

        if (mBlockTimeChange && Intent.ACTION_TIME_CHANGED.equals(action)) {
            Log.v("Ignore time change broadcast because it is sent from ipo.");
            return;
        }

        final PendingResult result = goAsync();
        final WakeLock wl = AlarmAlertWakeLock.createPartialWakeLock(context);
        wl.acquire();
        Log.v("AlarmInitReceiver AsyncHandler before.");
        AsyncHandler.post(new Runnable() {
            @Override
            public void run() {
                // Remove the snooze alarm after a boot.
                if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
                    // Clear stopwatch and timers data
                    if(!mBootIPO) {
                        SharedPreferences prefs =
                                PreferenceManager.getDefaultSharedPreferences(context);
                        Log.v("AlarmInitReceiver - Cleaning old timer and stopwatch data");
                        TimerObj.cleanTimersFromSharedPrefs(prefs);
                        Utils.clearSwSharedPref(prefs);
                    }
                    mBlockTimeChange = false;
                    Alarms.saveSnoozeAlert(context, Alarms.INVALID_ALARM_ID, -1);
                } else {
                    Alarms.disableExpiredAlarms(context);
                    SharedPreferences prefs =
                            PreferenceManager.getDefaultSharedPreferences(context);
                    Log.v("AlarmInitReceiver - Reset timers and clear stopwatch data");
                    TimerObj.resetTimersInSharedPrefs(prefs);
                    Utils.clearSwSharedPref(prefs);

                    if (!prefs.getBoolean(PREF_VOLUME_DEF_DONE, false)) {
                        // Fix the default
                        Log.v("AlarmInitReceiver - resetting volume button default");
                        switchVolumeButtonDefault(prefs);
                    }

                    /* If time changes, we need to reset the time column of alarms in database. */
                    if (Intent.ACTION_TIME_CHANGED.equals(action)
                            || Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
                        Alarms.resetAlarmTimes(context);
                    }
                    Alarms.setNextAlert(context);
                }
                result.finish();
                Log.v("AlarmInitReceiver finished.");
                wl.release();
            }
        });
    }

    private void switchVolumeButtonDefault(SharedPreferences prefs) {
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString(SettingsActivity.KEY_VOLUME_BEHAVIOR,
            SettingsActivity.DEFAULT_VOLUME_BEHAVIOR);

        // Make sure we do it only once
        editor.putBoolean(PREF_VOLUME_DEF_DONE, true);
        editor.apply();
    }
}
