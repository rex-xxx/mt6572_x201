package com.mediatek.calendar;

import com.android.calendar.CalendarController;
import com.android.calendar.Utils;
import com.android.calendar.CalendarController.EventType;
import com.android.calendar.CalendarController.ViewType;

import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.text.format.Time;
import android.widget.DatePicker;

/**
 * This fragment is used ONLY in AllInOneActivity for "Go to" functionality.
 */
public class GoToDatePickerDialogFragment extends DialogFragment implements
        OnDateSetListener {
    private static final String TAG = "GoToDatePickerDialogFragment";

    // bundle KEYs for DatePickerDialog parameters
    private static final String KEY_YEAR = "year";
    private static final String KEY_MONTH = "month";
    private static final String KEY_DAY = "day";
    private static final String KEY_FIRST_DAY = "firstDayOfWeek";
    private static final String KEY_SHOW_WEEK = "showWeekNumber";
    private static final String KEY_CANCEL_ON_TOUCH = "cancelOnTouchOutside";
    private static final String KEY_THEME = "theme";

    private String mTimeZone;

    private final Runnable mHomeTimeUpdater = new Runnable() {
        public void run() {
            // nothing to do
        }
    };

    public static GoToDatePickerDialogFragment newInstance(int year, int month,
            int day, int firstDayOfWeek, boolean showWeekNumber,
            boolean cancelOnTouchOutside, int theme) {
        LogUtil.d(TAG, "newInstance()");

        GoToDatePickerDialogFragment f = new GoToDatePickerDialogFragment();

        Bundle date = new Bundle();
        date.putInt(KEY_YEAR, year);
        date.putInt(KEY_MONTH, month);
        date.putInt(KEY_DAY, day);
        date.putInt(KEY_FIRST_DAY, firstDayOfWeek);
        date.putBoolean(KEY_SHOW_WEEK, showWeekNumber);
        date.putBoolean(KEY_CANCEL_ON_TOUCH, cancelOnTouchOutside);
        date.putInt(KEY_THEME, theme);

        f.setArguments(date);

        return f;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LogUtil.d(TAG, "onCreateDialog()");

        Bundle date = getArguments();

        int year = date.getInt(KEY_YEAR);
        int month = date.getInt(KEY_MONTH);
        int day = date.getInt(KEY_DAY);
        int firstDayOfWeek = date.getInt(KEY_FIRST_DAY);
        boolean showWeekNumber = date.getBoolean(KEY_SHOW_WEEK);
        boolean cancelOnTouchOutside = date.getBoolean(KEY_CANCEL_ON_TOUCH);
        int theme = date.getInt(KEY_THEME);

        CalendarDatePickerDialog dpd = new CalendarDatePickerDialog(getActivity(), theme, this,
                year, month, day);
        dpd.getDatePicker().getCalendarView().setShowWeekNumber(showWeekNumber);
        dpd.getDatePicker().getCalendarView().setFirstDayOfWeek(firstDayOfWeek);
        dpd.setCanceledOnTouchOutside(cancelOnTouchOutside);

        return dpd;
    }

    public void onDateSet(DatePicker view, int year, int monthOfYear,
            int dayOfMonth) {
        LogUtil.d(TAG, "date set: " + year + "-" + monthOfYear + "-" + dayOfMonth);

        mTimeZone = Utils.getTimeZone(getActivity(), mHomeTimeUpdater);
        LogUtil.d(TAG, "date set, time zone: " + mTimeZone);

        Time t = new Time(mTimeZone);
        t.year = year;
        t.month = monthOfYear;
        t.monthDay = dayOfMonth;

        long extras = CalendarController.EXTRA_GOTO_TIME
                | CalendarController.EXTRA_GOTO_TODAY;
        int viewType = ViewType.CURRENT;
        CalendarController.getInstance(getActivity()).sendEvent(this,
                EventType.GO_TO, t, null, t, -1, viewType, extras, null, null);
    }
}
