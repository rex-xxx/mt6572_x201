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

import android.content.Context;
import android.os.Parcelable; //added by MTK
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.mediatek.xlog.Xlog;

import java.text.DateFormatSymbols;
import java.util.Calendar;

public class MTKRepeatPreference extends DialogPreference implements OnCheckedChangeListener {
    private static final String TAG = "MTKRepeatPreference";
    private static final boolean DBG = true;

    private static final int WEEK_DAYS_COUNT = 5;
    private static final int ALL_DAYS_COUNT = 7;

    static final int WEEK_DAYS_CODE = 31;
    static final int ALL_DAYS_CODE = 127;

    private RadioButton mWeekdaysButton;
    private RadioButton mEverydayButton;
    private RadioButton mNoRepeatsButton;

    private ListView mListView;
    private ArrayAdapter<CharSequence> mRepeatAdapter;

    // Initial value that can be set with the values saved in the database.
    private final Alarm.DaysOfWeek mDaysOfWeek = new Alarm.DaysOfWeek(0);

    // New value that will be set if a positive result comes back from the dialog.
    private final Alarm.DaysOfWeek mNewDaysOfWeek = new Alarm.DaysOfWeek(0);

    public MTKRepeatPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogLayoutResource(R.layout.mtk_repeat_layout);

        String[] weekdays = new DateFormatSymbols().getWeekdays();
        String[] values = new String[] {
                weekdays[Calendar.MONDAY], weekdays[Calendar.TUESDAY],
                weekdays[Calendar.WEDNESDAY], weekdays[Calendar.THURSDAY],
                weekdays[Calendar.FRIDAY], weekdays[Calendar.SATURDAY],
                weekdays[Calendar.SUNDAY],
        };
        mRepeatAdapter = new ArrayAdapter<CharSequence>(context,
                com.android.internal.R.layout.select_dialog_multichoice,
                com.android.internal.R.id.text1, values) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                if ((mNewDaysOfWeek.getCoded() & (1 << position)) > 0) {
                    mListView.setItemChecked(position, true);
//                    Xlog.d(TAG, "getView checked: position = " + position);
                } else {
                    mListView.setItemChecked(position, false);
//                    Xlog.d(TAG, "......getView unchecked: position = " + position);
                }
                return view;
            }
        };

        if (DBG) {
            Xlog.d(TAG, "MTKRepeatPreference constructor: mNewDaysOfWeek = "
                    + mNewDaysOfWeek.getCoded() + ",mDaysOfWeek = " + mDaysOfWeek.getCoded()
                    + ",this = " + this);
        }
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (DBG) {
            Xlog.d(TAG, "onDialogClosed: positiveResult = " + positiveResult + ",mNewDaysOfWeek = "
                    + mNewDaysOfWeek.getCoded() + ",mDaysOfWeek = " + mDaysOfWeek.getCoded()
                    + ",this = " + this);
        }
        if (positiveResult) {
            mDaysOfWeek.set(mNewDaysOfWeek);
            setSummary(mDaysOfWeek.toString(getContext(), true));
            callChangeListener(mDaysOfWeek);
        } else {
            // Reset repeat days if user cancel the set.
            mNewDaysOfWeek.set(mDaysOfWeek);
            setRadioRepeatState(mNewDaysOfWeek.getCoded());
        }
    }
   /**
    *     added by MTK
    */
    protected void onRestoreInstanceState(Parcelable state) {
        super.onRestoreInstanceState(state);
        mWeekdaysButton.setText(getContext().getResources().getString(R.string.repeat_weekdays));
        mEverydayButton.setText(getContext().getResources().getString(R.string.every_day));
        mNoRepeatsButton.setText(getContext().getResources().getString(R.string.never));
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        setupViews(view);
        setRadioRepeatState(mNewDaysOfWeek.getCoded());
        if (DBG) {
            Xlog.d(TAG, "onBindDialogView: mListView = " + mListView + ",view = " + view
                    + ",mNewDaysOfWeek = " + mNewDaysOfWeek.getCoded() + ",mDaysOfWeek = "
                    + mDaysOfWeek.getCoded());
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (DBG) {
            Xlog.d(TAG, "onCheckedChanged: buttonView = " + buttonView + ",isChecked = "
                    + isChecked + ",text = " + buttonView.getText() + ",mNewDaysOfWeek = "
                    + mNewDaysOfWeek.getCoded() + ",mDaysOfWeek = " + mDaysOfWeek.getCoded()
                    + ",this = " + this);
        }
        if (!isChecked) {
            return;
        }

        if (buttonView.getId() == mWeekdaysButton.getId()) {
            mEverydayButton.setChecked(false);
            mNoRepeatsButton.setChecked(false);
            for (int i = 0; i < WEEK_DAYS_COUNT; i++) {
                mListView.setItemChecked(i, true);
                mNewDaysOfWeek.set(i, true);
            }
            for (int i = WEEK_DAYS_COUNT; i < ALL_DAYS_COUNT; i++) {
                mListView.setItemChecked(i, false);
                mNewDaysOfWeek.set(i, false);
            }
        } else if (buttonView.getId() == mEverydayButton.getId()) {
            mWeekdaysButton.setChecked(false);
            mNoRepeatsButton.setChecked(false);
            for (int i = 0; i < ALL_DAYS_COUNT; i++) {
                mListView.setItemChecked(i, true);
                mNewDaysOfWeek.set(i, true);
            }
        } else if (buttonView.getId() == mNoRepeatsButton.getId()) {
            mEverydayButton.setChecked(false);
            mWeekdaysButton.setChecked(false);
            for (int i = 0; i < ALL_DAYS_COUNT; i++) {
                mListView.setItemChecked(i, false);
                mNewDaysOfWeek.set(i, false);
            }
        }
    }

    private void setupViews(View view) {
        mWeekdaysButton = (RadioButton) view.findViewById(R.id.weekdays);
        mWeekdaysButton.setOnCheckedChangeListener(this);
        mEverydayButton = (RadioButton) view.findViewById(R.id.everyday);
        mEverydayButton.setOnCheckedChangeListener(this);
        mNoRepeatsButton = (RadioButton) view.findViewById(R.id.noRepeats);
        mNoRepeatsButton.setOnCheckedChangeListener(this);

        mListView = (ListView) view.findViewById(R.id.repeatList);
        mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        mListView.setAdapter(mRepeatAdapter);

        mListView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView parent, View v, int position, long id) {
                mNewDaysOfWeek.set(position, mListView.isItemChecked(position));
                if (DBG) {
                    Xlog.d(TAG, "onItemClick: position = " + position + ",mNewDaysOfWeek = "
                            + mNewDaysOfWeek.getCoded() + ",mDaysOfWeek = "
                            + mDaysOfWeek.getCoded());
                }
                setRadioRepeatState(mNewDaysOfWeek.getCoded());
            }
        });
    }

    private void setRadioRepeatState(final int daysOfWeek) {
        if (DBG) {
            Xlog.d(TAG, "setRadioRepeatState: daysOfWeek = " + daysOfWeek + ",this = " + this);
        }
        if (daysOfWeek == ALL_DAYS_CODE) {
            mEverydayButton.setChecked(true);
            mWeekdaysButton.setChecked(false);
            mNoRepeatsButton.setChecked(false);
        } else if (daysOfWeek == WEEK_DAYS_CODE) {
            mEverydayButton.setChecked(false);
            mWeekdaysButton.setChecked(true);
            mNoRepeatsButton.setChecked(false);
        } else if (daysOfWeek == 0) {
            mEverydayButton.setChecked(false);
            mWeekdaysButton.setChecked(false);
            mNoRepeatsButton.setChecked(true);
        } else {
            mEverydayButton.setChecked(false);
            mWeekdaysButton.setChecked(false);
            mNoRepeatsButton.setChecked(false);
        }
    }

    /**
     * Set days of week.
     *
     * @param dow
     */
    public void setDaysOfWeek(Alarm.DaysOfWeek dow) {
        if (DBG) {
            Xlog.d(TAG, "setDaysOfWeek: daysOfWeek = " + dow.getCoded());
        }
        mDaysOfWeek.set(dow);
        mNewDaysOfWeek.set(dow);
        setSummary(dow.toString(getContext(), true));
    }

    /**
     * Get days of week.
     *
     * @return
     */
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
