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

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.mediatek.deskclock.ext.IRepeatPreferenceExtension;
import com.mediatek.pluginmanager.Plugin;
import com.mediatek.pluginmanager.PluginManager;
import com.mediatek.xlog.Xlog;

/**
 * Manages each alarm
 */
public class SetAlarm extends PreferenceActivity implements Preference.OnPreferenceChangeListener,
        AlarmTimePickerDialogFragment.AlarmTimePickerDialogHandler {
    public static final String TAG = "SetAlarm";
    private static final String KEY_CURRENT_ALARM = "currentAlarm";
    private static final String KEY_ORIGINAL_ALARM = "originalAlarm";
    private static final String KEY_TIME_PICKER_BUNDLE = "timePickerBundle";
    private static final String KEY_NEW_REPEAT_TIME = "newRepeatTime";

    private EditText mLabel;
    private CheckBoxPreference mEnabledPref;
    private Preference mTimePref;
    private AlarmPreference mAlarmPref;
    private CheckBoxPreference mVibratePref;
    private Preference mRepeatPref;

    private int     mId;
    private int     mHour;
    private int     mMinute;
    private Alarm   mOriginalAlarm;
    private IRepeatPreferenceExtension mRepeatPreferenceExtension;
    private ContentResolver mContentResolver;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // Override the default content view.
        setContentView(R.layout.set_alarm);

        EditText label = (EditText) getLayoutInflater().inflate(R.layout.alarm_label, null);
        ListView list = (ListView) findViewById(android.R.id.list);
        list.addFooterView(label);

        // TODO Stop using preferences for this view. Save on done, not after
        // each change.
        ///M: easy porting @{
        if (mRepeatPreferenceExtension == null) {
            PluginManager<IRepeatPreferenceExtension> pm 
                    = PluginManager.<IRepeatPreferenceExtension>create(getBaseContext(),
                            IRepeatPreferenceExtension.class.getName());
            for (int i = 0,count = pm.getPluginCount();i < count;i++) {
                Plugin<IRepeatPreferenceExtension> plugin = pm.getPlugin(i);
                try {
                    IRepeatPreferenceExtension ext = plugin.createObject();
                    if (ext != null) {
                        mRepeatPreferenceExtension = ext;
                        break;
                    }
                } catch (Plugin.ObjectCreationException ex) {
                    Log.e("can not create plugin object!");
                    ex.printStackTrace();
                }
            }
        }
        if (mRepeatPreferenceExtension != null &&
                mRepeatPreferenceExtension.shouldUseMTKRepeatPref()) {
            addPreferencesFromResource(R.xml.mtk_alarm_prefs);
        } else {
            addPreferencesFromResource(R.xml.alarm_prefs);
        }
        ///M: @}
        mContentResolver = getContentResolver();
        mContentResolver.registerContentObserver(
                Alarm.Columns.CONTENT_URI, true, mSetAlarmObserver);

        // Get each preference so we can retrieve the value later.
        mLabel = label;
        mEnabledPref = (CheckBoxPreference) findPreference("enabled");
        mEnabledPref.setOnPreferenceChangeListener(this);
        mTimePref = findPreference("time");
        mAlarmPref = (AlarmPreference) findPreference("alarm");
        mAlarmPref.setOnPreferenceChangeListener(this);
        mVibratePref = (CheckBoxPreference) findPreference("vibrate");
        mVibratePref.setOnPreferenceChangeListener(this);
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (!v.hasVibrator()) {
            getPreferenceScreen().removePreference(mVibratePref);
        }
        mRepeatPref = (RepeatPreference) findPreference("setRepeat");
        mRepeatPref.setOnPreferenceChangeListener(this);

        Intent i = getIntent();
        Alarm alarm = i.getParcelableExtra(Alarms.ALARM_INTENT_EXTRA);

        if (alarm == null) {
            // No alarm means create a new alarm.
            alarm = new Alarm();
        }
        mOriginalAlarm = alarm;

        // Populate the prefs with the original alarm data.  updatePrefs also
        // sets mId so it must be called before checking mId below.
        updatePrefs(mOriginalAlarm);
        Xlog.d(TAG, "onCreate: icicle = " + icicle + ",this = " + this);
        // We have to do this to get the save/cancel buttons to highlight on
        // their own.
        getListView().setItemsCanFocus(true);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayOptions(
                    0, ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
            LayoutInflater inflater 
                    = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View customActionBarView = inflater.inflate(R.layout.set_alarm_action_bar, null);
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                    ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME |
                    ActionBar.DISPLAY_SHOW_TITLE);
            actionBar.setCustomView(customActionBarView);
            View saveMenuItem = customActionBarView.findViewById(R.id.save_menu_item);
            saveMenuItem.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    saveAndExit();
                }
            });
        } else {     //tablet
            // Attach actions to each button.
            Button b = (Button) findViewById(R.id.alarm_save);
            b.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        long time = saveAlarm(null);
                        if(mEnabledPref.isChecked()) {
                            AlarmUtils.popAlarmSetToast(SetAlarm.this, time);
                        }
                        finish();
                    }
            });
            Button revert = (Button) findViewById(R.id.alarm_revert);
            revert.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    revert();
                    finish();
                }
            });
            b = (Button) findViewById(R.id.alarm_delete);
            if (mId == -1) {
                b.setEnabled(false);
                b.setVisibility(View.GONE);
            } else {
                b.setVisibility(View.VISIBLE);
                b.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        deleteAlarm();
                    }
                });
            }
        }
}

    private final ContentObserver mSetAlarmObserver = new ContentObserver(
            new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            if (mId == -1) {
                return;
            }
            Cursor cursor = mContentResolver.query(
                    ContentUris.withAppendedId(Alarm.Columns.CONTENT_URI, mId),
                    new String[] { "enabled" }, null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    boolean enabled = cursor.getInt(0) == 1;
                    if (enabled != mEnabledPref.isChecked()) {
                        mEnabledPref.setChecked(enabled);
                    }
                }
                cursor.close();
            }
        }
    };
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_delete) {
            deleteAlarm();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.set_alarm_context, menu);
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_ORIGINAL_ALARM, mOriginalAlarm);
        outState.putParcelable(KEY_CURRENT_ALARM, buildAlarmFromUi());
        if (mRepeatPreferenceExtension != null
                &&  mRepeatPreferenceExtension.shouldUseMTKRepeatPref()) {
            outState.putInt(KEY_NEW_REPEAT_TIME, ((MTKRepeatPreference) mRepeatPref)
                   .getNewDaysOfWeek().getCoded());
        } else {
            outState.putInt(KEY_NEW_REPEAT_TIME, ((RepeatPreference) mRepeatPref)
                    .getNewDaysOfWeek().getCoded());
        }
        Xlog.d(TAG, "onSaveInstanceState:mRepeatPref = " + mRepeatPref + ",outState = " + outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);

        Alarm alarmFromBundle = state.getParcelable(KEY_ORIGINAL_ALARM);
        if (alarmFromBundle != null) {
            mOriginalAlarm = alarmFromBundle;
        }

        alarmFromBundle = state.getParcelable(KEY_CURRENT_ALARM);
        if (alarmFromBundle != null) {
            updatePrefs(alarmFromBundle);
        }
        final int repeatCode = state.getInt(KEY_NEW_REPEAT_TIME);
        if (mRepeatPreferenceExtension != null
                 && mRepeatPreferenceExtension.shouldUseMTKRepeatPref()) {
            ((MTKRepeatPreference) mRepeatPref).setNewDaysOfWeek(new Alarm.DaysOfWeek(repeatCode));
        } else {
            ((RepeatPreference) mRepeatPref).setNewDaysOfWeek(new Alarm.DaysOfWeek(repeatCode));
        }

        Bundle b = state.getParcelable(KEY_TIME_PICKER_BUNDLE);
        if (b != null) {
            showTimePicker();
        }
    }

    // Used to post runnables asynchronously.
    private static final Handler sHandler = new Handler();

    public boolean onPreferenceChange(final Preference p, Object newValue) {
        // Asynchronously save the alarm since this method is called _before_
        // the value of the preference has changed.
        sHandler.post(new Runnable() {
            public void run() {
                // Editing any preference (except enable) enables the alarm.
                if (p != mEnabledPref) {
                    mEnabledPref.setChecked(true);
                }
            }
        });
        return true;
    }

    private void updatePrefs(Alarm alarm) {
        mId = alarm.id;
        mEnabledPref.setChecked(alarm.enabled);
        mLabel.setText(alarm.label);
        mHour = alarm.hour;
        mMinute = alarm.minutes;
        if (mRepeatPreferenceExtension != null
                && mRepeatPreferenceExtension.shouldUseMTKRepeatPref()) {
            ((MTKRepeatPreference)mRepeatPref).setDaysOfWeek(alarm.daysOfWeek);
        } else {
            ((RepeatPreference)mRepeatPref).setDaysOfWeek(alarm.daysOfWeek);
        }

        mVibratePref.setChecked(alarm.vibrate);
        // Give the alert uri to the preference.
        mAlarmPref.setAlert(alarm.alert);
        updateTime();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        if (preference == mTimePref) {
            showTimePicker();
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    private void showTimePicker() {
        AlarmUtils.showTimeEditDialog(getFragmentManager(), null);
    }

    @Override
    public void onDialogTimeSet(Alarm alarm, int hourOfDay, int minute) {
        // onTimeSet is called when the user clicks "Set"
        mHour = hourOfDay;
        mMinute = minute;
        updateTime();
        // If the time has been changed, enable the alarm.
        mEnabledPref.setChecked(true);
    }

    private void updateTime() {
        if (mRepeatPreferenceExtension != null
                &&  mRepeatPreferenceExtension.shouldUseMTKRepeatPref()) {
            mTimePref.setSummary(Alarms.formatTime(this, mHour, mMinute,
                ((MTKRepeatPreference)mRepeatPref).getDaysOfWeek()));
        } else {
            mTimePref.setSummary(Alarms.formatTime(this, mHour, mMinute,
                ((RepeatPreference)mRepeatPref).getDaysOfWeek()));
        }

    }
    private void revert() {
         int newId = mId;
         // "Revert" on a newly created alarm should delete it.
         if (mOriginalAlarm.id == -1) {
             Alarms.deleteAlarm(SetAlarm.this, newId);
         } else {
             saveAlarm(mOriginalAlarm);
         }
     }

    private long saveAlarm(Alarm alarm) {
        if (alarm == null) {
            alarm = buildAlarmFromUi();
        }

        long time;
        if (alarm.id == -1) {
            time = Alarms.addAlarm(this, alarm);
            // addAlarm populates the alarm with the new id. Update mId so that
            // changes to other preferences update the new alarm.
            mId = alarm.id;
        } else {
            time = Alarms.setAlarm(this, alarm);
        }
        return time;
    }

    private Alarm buildAlarmFromUi() {
        Alarm alarm = new Alarm();
        alarm.id = mId;
        alarm.enabled = mEnabledPref.isChecked();
        alarm.hour = mHour;
        alarm.minutes = mMinute;
        if (mRepeatPreferenceExtension != null
                &&  mRepeatPreferenceExtension.shouldUseMTKRepeatPref()) {
           alarm.daysOfWeek = ((MTKRepeatPreference)mRepeatPref).getDaysOfWeek();
        } else {
           alarm.daysOfWeek = ((RepeatPreference)mRepeatPref).getDaysOfWeek();
        }
        alarm.vibrate = mVibratePref.isChecked();
        alarm.label = mLabel.getText().toString();
        alarm.alert = mAlarmPref.getAlert();
        Log.v("buildAlarmFromUi alarm alert = " + alarm.alert);
        return alarm;
    }

    private void deleteAlarm() {
        if (mId == -1) {
            // Unedited, newly created alarms don't require confirmation
            finish();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.delete_alarm))
                    .setMessage(getString(R.string.delete_alarm_confirm))
                    .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface d, int w) {
                                    Alarms.deleteAlarm(SetAlarm.this, mId);
                                    finish();
                                }
                            })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        }
    }


    /**
     * Store any changes to the alarm and exit the activity.
     * Show a toast if the alarm is enabled with the time remaining until alarm
     */
    private void saveAndExit() {
        long time = saveAlarm(null);
        if(mEnabledPref.isChecked()) {
            AlarmUtils.popAlarmSetToast(SetAlarm.this, time);
        }
        finish();
    }

    /**
     * format "Alarm set for 2 days 7 hours and 53 minutes from
     * now"
     */
    static String formatToast(Context context, long timeInMillis) {
        long delta = timeInMillis - System.currentTimeMillis();
        long hours = delta / (1000 * 60 * 60);
        long minutes = delta / (1000 * 60) % 60;
        long days = hours / 24;
        hours = hours % 24;

        String daySeq = (days == 0) ? "" :
                (days == 1) ? context.getString(R.string.day) :
                context.getString(R.string.days, Long.toString(days));

        String minSeq = (minutes == 0) ? "" :
                (minutes == 1) ? context.getString(R.string.minute) :
                context.getString(R.string.minutes, Long.toString(minutes));

        String hourSeq = (hours == 0) ? "" :
                (hours == 1) ? context.getString(R.string.hour) :
                context.getString(R.string.hours, Long.toString(hours));

        boolean dispDays = days > 0;
        boolean dispHour = hours > 0;
        boolean dispMinute = minutes > 0;

        int index = (dispDays ? 1 : 0) |
                    (dispHour ? 2 : 0) |
                    (dispMinute ? 4 : 0);

        String[] formats = context.getResources().getStringArray(R.array.alarm_set);
        return String.format(formats[index], daySeq, hourSeq, minSeq);
    }

    @Override
    public void onPause() {
        super.onPause();
        mContentResolver.unregisterContentObserver(mSetAlarmObserver);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            final AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            audioManager.adjustStreamVolume(AudioManager.STREAM_ALARM,
                    AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI | AudioManager.FLAG_PLAY_SOUND);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            final AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            audioManager.adjustStreamVolume(AudioManager.STREAM_ALARM,
                    AudioManager.ADJUST_RAISE,
                    AudioManager.FLAG_SHOW_UI | AudioManager.FLAG_PLAY_SOUND);
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }
}
