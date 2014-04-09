package com.mediatek.calendar.lunar;

import android.app.AlertDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import com.android.calendar.R;

/**
 * #Lunar# A simple dialog containing an {@link android.widget.DatePicker}.
 * 
 * <p>
 * See the <a href="{@docRoot}
 * resources/tutorials/views/hello-datepicker.html">Date Picker tutorial</a>.
 * </p>
 */
public class LunarDatePickerDialog extends AlertDialog implements OnClickListener,
        LunarDatePicker.OnDateChangedListener {

    private static final String YEAR = "year";
    private static final String MONTH = "month";
    private static final String DAY = "day";

    private final LunarDatePicker mDatePicker;
    private final OnDateSetListener mCallBack;
    private boolean mTitleNeedsUpdate = true;

    /**
     * @param context
     *            The context the dialog is to run in.
     * @param callBack
     *            How the parent is notified that the date is set.
     * @param year
     *            The initial year of the dialog.
     * @param monthOfYear
     *            The initial month of the dialog.
     * @param dayOfMonth
     *            The initial day of the dialog.
     */
    public LunarDatePickerDialog(Context context, OnDateSetListener callBack, int year,
            int monthOfYear, int dayOfMonth) {
        this(context, 0, callBack, year, monthOfYear, dayOfMonth);
    }

    /**
     * @param context
     *            The context the dialog is to run in.
     * @param theme
     *            the theme to apply to this dialog
     * @param callBack
     *            How the parent is notified that the date is set.
     * @param year
     *            The initial year of the dialog.
     * @param monthOfYear
     *            The initial month of the dialog.
     * @param dayOfMonth
     *            The initial day of the dialog.
     */
    public LunarDatePickerDialog(Context context, int theme, OnDateSetListener callBack, int year,
            int monthOfYear, int dayOfMonth) {
        super(context, theme);
        mCallBack = callBack;

        Context themeContext = getContext();
        setButton(BUTTON_POSITIVE, themeContext.getText(R.string.date_time_done), this);
        setIcon(0);

        LayoutInflater inflater = (LayoutInflater) themeContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        LinearLayout linearLayout = (LinearLayout) inflater.inflate(R.layout.date_picker_dialog,
                null);
        mDatePicker = new LunarDatePicker(getContext());
        mDatePicker.setCalendarViewShown(false);
        linearLayout.addView(mDatePicker);
        setView(linearLayout);

        mDatePicker.init(year, monthOfYear, dayOfMonth, this);
        updateTitle();
    }

    public void onClick(DialogInterface dialog, int which) {
        tryNotifyDateSet();
    }

    public void onDateChanged(LunarDatePicker view, int year, int month, int day) {
        mDatePicker.init(year, month, day, this);
        updateTitle();
    }

    /**
     * Gets the {@link DatePicker} contained in this dialog.
     * 
     * @return The calendar view.
     */
    public LunarDatePicker getDatePicker() {
        return mDatePicker;
    }

    /**
     * Sets the current date.
     * 
     * @param year
     *            The date year.
     * @param monthOfYear
     *            The date month.
     * @param dayOfMonth
     *            The date day of month.
     */
    public void updateDate(int year, int monthOfYear, int dayOfMonth) {
        mDatePicker.updateDate(year, monthOfYear, dayOfMonth);
    }

    private void tryNotifyDateSet() {
        if (mCallBack != null) {
            mDatePicker.clearFocus();
            mCallBack.onDateSet(null, mDatePicker.getYear(), mDatePicker.getMonth(),
                    mDatePicker.getDayOfMonth());
        }
    }

    @Override
    protected void onStop() {
        // it is commented because we do NOT want it notify call back
//        tryNotifyDateSet();
        super.onStop();
    }

    private void updateTitle() {
        if (!mDatePicker.getCalendarViewShown()) {
            setTitle(mDatePicker.getDateString());
            mTitleNeedsUpdate = true;
        } else {
            if (mTitleNeedsUpdate) {
                mTitleNeedsUpdate = false;
                setTitle(R.string.date_picker_dialog_title);
            }
        }
    }

    @Override
    public Bundle onSaveInstanceState() {
        Bundle state = super.onSaveInstanceState();
        state.putInt(YEAR, mDatePicker.getYear());
        state.putInt(MONTH, mDatePicker.getMonth());
        state.putInt(DAY, mDatePicker.getDayOfMonth());
        return state;
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        int year = savedInstanceState.getInt(YEAR);
        int month = savedInstanceState.getInt(MONTH);
        int day = savedInstanceState.getInt(DAY);
        mDatePicker.init(year, month, day, this);
    }
}
