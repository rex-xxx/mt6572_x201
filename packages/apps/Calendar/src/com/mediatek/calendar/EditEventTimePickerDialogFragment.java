package com.mediatek.calendar;

import com.android.calendar.event.EditEventActivity;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.os.Bundle;
import android.widget.TimePicker;

public class EditEventTimePickerDialogFragment extends DialogFragment implements
        OnTimeSetListener, OnDismissListener {
    private static final String TAG = "EditEventTimePickerDialogFragment::date_time_debug_tag";

    private static final String KEY_TIME_HOUR = "hour";
    private static final String KEY_TIME_MINUTE = "minute";
    private static final String KEY_24_FORMAT = "24_format";
    private static final String KEY_CANCEL_ON_TOUCH = "cancelOnTouchOutside";
    private static final String KEY_THEME = "theme";

    public static EditEventTimePickerDialogFragment newInstance(int hour,
            int minute, boolean is24Format, boolean cancelOnTouchOutside,
            int theme) {
        LogUtil.d(TAG, "newInstance()");

        EditEventTimePickerDialogFragment f = new EditEventTimePickerDialogFragment();

        Bundle time = new Bundle();
        time.putInt(KEY_TIME_HOUR, hour);
        time.putInt(KEY_TIME_MINUTE, minute);
        time.putBoolean(KEY_24_FORMAT, is24Format);
        time.putBoolean(KEY_CANCEL_ON_TOUCH, cancelOnTouchOutside);
        time.putInt(KEY_THEME, theme);

        f.setArguments(time);

        return f;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LogUtil.d(TAG, "onCreateDialog()");

        Bundle time = getArguments();
        int hour = time.getInt(KEY_TIME_HOUR);
        int minute = time.getInt(KEY_TIME_MINUTE);
        boolean is24Format = time.getBoolean(KEY_24_FORMAT);
        boolean cancel = time.getBoolean(KEY_CANCEL_ON_TOUCH);
        int theme = time.getInt(KEY_THEME);

        CalendarTimePickerDialog tpd = new CalendarTimePickerDialog(
                getActivity(), theme, this, hour, minute, is24Format);
        tpd.setCanceledOnTouchOutside(cancel);

        return tpd;
    }

    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        Activity a = getActivity();
        EditEventActivity eea = null;

        if (a instanceof EditEventActivity) {
            LogUtil.d(TAG, "onTimeSet(), Bingo!");

            eea = (EditEventActivity) a;
            OnTimeSetListener l = eea.getDateTimeOnTimeSetListener();
            if (l != null) {
                l.onTimeSet(view, hourOfDay, minute);
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
