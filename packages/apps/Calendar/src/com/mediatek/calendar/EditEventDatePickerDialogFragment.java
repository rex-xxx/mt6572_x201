package com.mediatek.calendar;

import com.android.calendar.event.EditEventActivity;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.DatePicker;

public class EditEventDatePickerDialogFragment extends DialogFragment implements OnDateSetListener{
    private static final String TAG = "EditEventDatePickerDialogFragment::date_time_debug_tag";

    // bundle KEYs for DatePickerDialog parameters
    private static final String KEY_YEAR = "year";
    private static final String KEY_MONTH = "month";
    private static final String KEY_DAY = "day";
    private static final String KEY_FIRST_DAY = "firstDayOfWeek";
    private static final String KEY_SHOW_WEEK = "showWeekNumber";
    private static final String KEY_CANCEL_ON_TOUCH = "cancelOnTouchOutside";
    private static final String KEY_THEME = "theme";

    public static EditEventDatePickerDialogFragment newInstance(int year, int month,
            int day, int firstDayOfWeek, boolean showWeekNumber,
            boolean cancelOnTouchOutside, int theme) {
        LogUtil.d(TAG, "newInstance()");

        EditEventDatePickerDialogFragment f = new EditEventDatePickerDialogFragment();

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

        CalendarDatePickerDialog dpd = new CalendarDatePickerDialog(getActivity(), theme, this, year, month, day);
        dpd.getDatePicker().getCalendarView().setShowWeekNumber(showWeekNumber);
        dpd.getDatePicker().getCalendarView().setFirstDayOfWeek(firstDayOfWeek);
        dpd.setCanceledOnTouchOutside(cancelOnTouchOutside);

        return dpd;
    }

    public void onDateSet(DatePicker view, int year, int monthOfYear,
            int dayOfMonth) {
        Activity a = getActivity();
        EditEventActivity eea = null;

        if (a instanceof EditEventActivity) {
            LogUtil.d(TAG, "onDateSet(), Bingo!");

            eea = (EditEventActivity) a;
            OnDateSetListener l = eea.getDateTimeOnDateSetListener();
            if (l != null) {
                l.onDateSet(view, year, monthOfYear, dayOfMonth);
            }
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);

        Activity a = getActivity();
        EditEventActivity eea = null;

        if (a instanceof EditEventActivity) {
            LogUtil.d(TAG, "onDismiss(), Bingo!");

            eea = (EditEventActivity) a;
            eea.getDateTimeOnDismissListener().onDismiss(dialog);
        }
    }
}
