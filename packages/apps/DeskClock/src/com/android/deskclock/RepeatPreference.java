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

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.ListPreference;
import android.util.AttributeSet;

import com.mediatek.xlog.Xlog;

import java.text.DateFormatSymbols;
import java.util.Calendar;

public class RepeatPreference extends ListPreference {
    private static final String TAG = "RepeatPreference";
    private static final boolean DBG = false;
    // Initial value that can be set with the values saved in the database.
    private final Alarm.DaysOfWeek mDaysOfWeek = new Alarm.DaysOfWeek(0);
    // New value that will be set if a positive result comes back from the
    // dialog.
    private final Alarm.DaysOfWeek mNewDaysOfWeek = new Alarm.DaysOfWeek(0);

    public RepeatPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (DBG) {
            Xlog.d(TAG, "RepeatPreference constructor mDaysOfWeek = "
                    + mDaysOfWeek.toString(context, true) + ",mNewDaysOfWeek = "
                    + mNewDaysOfWeek.toString(context, true) + ",mNewDaysOfWeek = "
                    + mNewDaysOfWeek + ",mDaysOfWeek = " + mDaysOfWeek + ",this = " + this);
        }
        String[] weekdays = new DateFormatSymbols().getWeekdays();
        String[] values = new String[] {
            weekdays[Calendar.MONDAY],
            weekdays[Calendar.TUESDAY],
            weekdays[Calendar.WEDNESDAY],
            weekdays[Calendar.THURSDAY],
            weekdays[Calendar.FRIDAY],
            weekdays[Calendar.SATURDAY],
            weekdays[Calendar.SUNDAY],
        };
        setEntries(values);
        setEntryValues(values);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (DBG) {
            Xlog.d(TAG, "onDialogClosed: positiveResult = " + positiveResult + ",mDaysOfWeek = "
                    + mDaysOfWeek.toString(getContext(), true) + ",mNewDaysOfWeek = "
                    + mNewDaysOfWeek.toString(getContext(), true) + ",mNewDaysOfWeek = "
                    + mNewDaysOfWeek + ",mDaysOfWeek = " + mDaysOfWeek + ",this = " + this);
        }
        if (positiveResult) {
            mDaysOfWeek.set(mNewDaysOfWeek);
            setSummary(mDaysOfWeek.toString(getContext(), true));
            callChangeListener(mDaysOfWeek);
        } else {
            // Reset repeat days if user cancel the set.
            mNewDaysOfWeek.set(mDaysOfWeek);
        }
    }

    @Override
    protected void onPrepareDialogBuilder(Builder builder) {
        CharSequence[] entries = getEntries();
        if (DBG) {
            Xlog.d(TAG, "onPrepareDialogBuilder: mDaysOfWeek = "
                    + mDaysOfWeek.toString(getContext(), true) + ",mNewDaysOfWeek = "
                    + mNewDaysOfWeek.toString(getContext(), true) + ",mNewDaysOfWeek = "
                    + mNewDaysOfWeek + ",mDaysOfWeek = " + mDaysOfWeek + ",this = " + this);
        }
        builder.setMultiChoiceItems(
                entries, mNewDaysOfWeek.getBooleanArray(),
                new DialogInterface.OnMultiChoiceClickListener() {
                    public void onClick(DialogInterface dialog, int which,
                            boolean isChecked) {
                        mNewDaysOfWeek.set(which, isChecked);
                    }
                });
    }

    public void setDaysOfWeek(Alarm.DaysOfWeek dow) {
        if (DBG) {
            Xlog.d(TAG, "setDaysOfWeek: dow = " + dow.toString(getContext(), true)
                    + ",mDaysOfWeek = " + mDaysOfWeek.toString(getContext(), true)
                    + ",mNewDaysOfWeek = " + mNewDaysOfWeek.toString(getContext(), true)
                    + ",mNewDaysOfWeek = " + mNewDaysOfWeek + ",mDaysOfWeek = " + mDaysOfWeek
                    + ",this = " + this);
        }

        mDaysOfWeek.set(dow);
        mNewDaysOfWeek.set(dow);
        setSummary(dow.toString(getContext(), true));
    }

    public Alarm.DaysOfWeek getDaysOfWeek() {
        return mDaysOfWeek;
    }

    public void setNewDaysOfWeek(Alarm.DaysOfWeek dow) {
        if (DBG) {
            Xlog.d(TAG, "setNewDaysOfWeek: dow = " + dow.toString(getContext(), true)
                    + ",mNewDaysOfWeek = " + mNewDaysOfWeek + ",mDaysOfWeek = " + mDaysOfWeek
                    + ",this = " + this);
        }
        mNewDaysOfWeek.set(dow);
    }

    public Alarm.DaysOfWeek getNewDaysOfWeek() {
        return mNewDaysOfWeek;
    }
}
