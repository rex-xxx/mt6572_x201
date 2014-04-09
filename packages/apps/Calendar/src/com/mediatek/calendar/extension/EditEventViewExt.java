package com.mediatek.calendar.extension;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.database.Cursor;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Calendars;
import android.text.format.Time;
import android.view.View;
import android.widget.CalendarView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;

import com.android.calendar.CalendarEventModel;
import com.android.calendar.R;
import com.android.calendar.Utils;
import com.android.calendar.event.EditEventActivity;
import com.mediatek.calendar.EditEventDatePickerDialogFragment;
import com.mediatek.calendar.EditEventLunarDatePickerDialogFragment;
import com.mediatek.calendar.LogUtil;
import com.mediatek.calendar.lunar.LunarDatePickerDialog;
import com.mediatek.calendar.lunar.LunarUtil;

import java.util.Calendar;

/**
 * M: this class extends the function of EditEventView.
 * typically, the Lunar function added, the PC Sync specific functions changed.
 */
public class EditEventViewExt implements IEditEventViewExt {

    private static final String TAG = "EditEventViewExt";
    private Activity mActivity;
    private LunarUtil mLunarUtil;
    private boolean mIsUseLunarDatePicker;
    private IEditEventView mEditEventView;
    public EditEventViewExt(Activity activity, IEditEventView editEventView) {
        mActivity = activity;
        mLunarUtil = LunarUtil.getInstance(activity);
        mEditEventView = editEventView;
    }
    @Override
    public String getDateStringFromMillis(long millis) {
        ///M:#Lunar# modify for lunar calendar. @{
        mIsUseLunarDatePicker = isLunarDataPickerClicked();
        String dateString = null;
        if (mIsUseLunarDatePicker) {
            Time time = new Time();
            time.set(millis);
            dateString = mLunarUtil.getLunarDateString(time.year, time.month + 1, time.monthDay);
        }
        ///@}
        return dateString;
    }

    @Override
    public boolean isExtensionEnabled() {
        return true;
    }

    @Override
    public void onAccountItemSelected(Cursor cursor) {
        ///M:#PC Sync# filter the local account for "Guest".@{
        int accountTypeColumn = cursor.getColumnIndexOrThrow(Calendars.ACCOUNT_TYPE);
        String accountType = cursor.getString(accountTypeColumn);
        setAttendeesGroupVisibility(accountType);
        mEditEventView.getModel().mAccountType = accountType;
        ///@}
    }

    ///M: keep DatePickerDialog and TimePickerDialog when rotate device @{
    private static final String DATE_TIME_TAG = TAG + "::date_time_debug_tag";

    private static final String EDIT_EVENT_DATE_DIALOG_FRAG = "edit_event_date_dialog_frag";
    private static final String EDIT_EVENT_LUNAR_DATE_DIALOG_FRAG = "edit_event_lunar_date_dialog_frag";
    ///@}

    @Override
    public void onDateClicked(View v, Time time) {
        ///M: keep DatePickerDialog and TimePickerDialog when rotate device @{
        EditEventActivity eea = null;
        if (mActivity instanceof EditEventActivity) {
            eea = (EditEventActivity) mActivity;
        } else {
            LogUtil.d(DATE_TIME_TAG, "onDateClicked(), only used by EditEventActivity");

            return;
        }

        // prevent multi-dialogs showing
        if (eea.isAnyDialogShown()) {
            return;
        }
        eea.setDialogShown();

        int startOfWeek = Utils.getFirstDayOfWeek(mActivity);
        // Utils returns Time days while CalendarView wants Calendar days
        if (startOfWeek == Time.SATURDAY) {
            startOfWeek = Calendar.SATURDAY;
        } else if (startOfWeek == Time.SUNDAY) {
            startOfWeek = Calendar.SUNDAY;
        } else {
            startOfWeek = Calendar.MONDAY;
        }

        mIsUseLunarDatePicker = isLunarDataPickerClicked();

        if (mIsUseLunarDatePicker) {
            EditEventLunarDatePickerDialogFragment lf = EditEventLunarDatePickerDialogFragment
                    .newInstance(time.year, time.month, time.monthDay,
                            startOfWeek, Utils.getShowWeekNumber(mActivity),
                            true, 0);
            lf.show(mActivity.getFragmentManager(), EDIT_EVENT_LUNAR_DATE_DIALOG_FRAG);
        } else {
            EditEventDatePickerDialogFragment f = EditEventDatePickerDialogFragment
                    .newInstance(time.year, time.month, time.monthDay,
                            startOfWeek, Utils.getShowWeekNumber(mActivity),
                            true, 0);
            f.show(mActivity.getFragmentManager(), EDIT_EVENT_DATE_DIALOG_FRAG);
        }
        ///@}
    }

    @Override
    public void setExtUI(CalendarEventModel model) {
        ///M:#Lunar# @{
        if (mLunarUtil.canShowLunarCalendar()) {
            mIsUseLunarDatePicker = model.mIsLunar;
            RadioButton radioBtn = (RadioButton)mActivity.findViewById(
                    model.mIsLunar ? R.id.switch_lunar : R.id.switch_gregorian);
            if (radioBtn == null) {
                LogUtil.d(TAG, "radio button is null, do nothing here.");
            } else {
                radioBtn.setChecked(true);
            }
        }
        ///@}
        ///M: #PC Sync# remove the "Guest" when it is local account. @{
        setAttendeesGroupVisibility(model.mAccountType);
        ///@}
    }

    @Override
    public void updateDatePickerSelection() {
        RadioGroup radioGroup = (RadioGroup) mActivity.findViewById(R.id.switch_date_picker);
        if (radioGroup != null) {
            if (mLunarUtil.canShowLunarCalendar()) {
                radioGroup.setVisibility(View.VISIBLE);
                mEditEventView.resetDateButton();

                //set the listener.
                radioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                    public void onCheckedChanged(RadioGroup group, int checkedId) {
                        switch(checkedId) {
                        case R.id.switch_lunar:
                            mIsUseLunarDatePicker = true;
                            mEditEventView.resetDateButton();
                            mEditEventView.getModel().mIsLunar = true;
                            break;
                        case R.id.switch_gregorian:
                        default:
                            mIsUseLunarDatePicker = false;
                            mEditEventView.resetDateButton();
                            mEditEventView.getModel().mIsLunar = false;
                            break;
                        }
                    }
         
                });
            } else {
                radioGroup.setVisibility(View.GONE);
            }
        }

    }

    /**
     * M: if lunar data picker is ckecked return true ,else return false.
     */
    private boolean isLunarDataPickerClicked() {
        RadioGroup radioGroup = (RadioGroup) mActivity.findViewById(R.id.switch_date_picker);
        if ((radioGroup == null) || (radioGroup.getVisibility() != View.VISIBLE)) {
            LogUtil.d(TAG, "RadioGroup is null, or is invisible, means not clicked");
            return false;
        }
        return radioGroup.getCheckedRadioButtonId() == R.id.switch_lunar ? true : false;
    }

    ///M:#PC Sync#@{
    /**
     * M: if the account is pc-sync, ACCOUNT_TYPE_LOCAL, the Attendee field should GONE
     */
    private void setAttendeesGroupVisibility(String accountType) {
        mEditEventView.setAttendeesGroupVisibility(
                CalendarContract.ACCOUNT_TYPE_LOCAL.equals(accountType) ?
                        View.GONE : View.VISIBLE);
    }
    ///@}

}
