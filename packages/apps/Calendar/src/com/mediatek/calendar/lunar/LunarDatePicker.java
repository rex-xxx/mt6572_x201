package com.mediatek.calendar.lunar;

import android.app.Fragment.SavedState;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View.BaseSavedState;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CalendarView;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.NumberPicker.OnValueChangeListener;
import android.widget.TextView;

import com.android.calendar.R;
import com.mediatek.calendar.LogUtil;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;


///M:#Lunar#
public class LunarDatePicker extends FrameLayout {

    private static final String TAG = LunarDatePicker.class.getSimpleName();

    private static final String DATE_FORMAT = "MM/dd/yyyy";

    private static final boolean DEFAULT_CALENDAR_VIEW_SHOWN = true;

    private static final boolean DEFAULT_SPINNERS_SHOWN = true;

    private static final boolean DEFAULT_ENABLED_STATE = true;

    private final LinearLayout mSpinners;

    private final NumberPicker mDaySpinner;

    private final NumberPicker mMonthSpinner;

    private final NumberPicker mYearSpinner;

    private final EditText mDaySpinnerInput;

    private final EditText mMonthSpinnerInput;

    private final EditText mYearSpinnerInput;

    private final CalendarView mCalendarView;

    private Locale mCurrentLocale;

    private OnDateChangedListener mOnDateChangedListener;

    private final String[] mLunarMonthTextArray;
    private final String[] mLunarMonthNumberTextArray;
    private final String mLunarLeapWordText;

    private final java.text.DateFormat mDateFormat = new SimpleDateFormat(
            DATE_FORMAT);

    private int mNumberOfMonths = 12;

    private Calendar mTempDate;

    // start date
    private static final int DEFAULT_START_YEAR = 1970;
    private static final int DEFAULT_START_MONTH = 0;
    private static final int DEFAULT_START_DAY = 1;
    private static final int DEFAULT_START_HOUR = 0;
    private static final int DEFAULT_START_MINUTE = 0;
    // end date
    private static final int DEFAULT_END_YEAR = 2036;
    private static final int DEFAULT_END_MONTH = 11;
    private static final int DEFAULT_END_DAY = 31;
    private static final int DEFAULT_END_HOUR = 23;
    private static final int DEFAULT_END_MINUTE = 59;

    private static Calendar sMinDate = Calendar.getInstance();
    //Init the min date,just for set sMinDate a Default Value
    static {
        sMinDate.set(DEFAULT_START_YEAR, DEFAULT_START_MONTH, DEFAULT_START_DAY,
                DEFAULT_START_HOUR, DEFAULT_START_MINUTE);
    }
    private static Calendar sMaxDate = Calendar.getInstance();
    //Init the max date,just for set sMaxDate a Default Value
    static {
        sMaxDate.set(DEFAULT_END_YEAR, DEFAULT_END_MONTH, DEFAULT_END_DAY,
                DEFAULT_END_HOUR, DEFAULT_END_MINUTE);
    }

    private Calendar mCurrentDate;

    private boolean mIsEnabled = DEFAULT_ENABLED_STATE;

    private LunarUtil mLunar;
    
    //the datepicker child count
    private static final int PICKER_CHILD_COUNT = 1;
    //the index of EditText in the NumberPicker 
    private static final int PICKER_CHILD_INPUT_TEXT_INDEX = 0;

    /// M: an index refer to word "闰".
    private static final int LUNAR_WORD_RUN = 1;

    /**
     * The callback used to indicate the user changes\d the date.
     */
    public interface OnDateChangedListener {

        /**
         * Called upon a date change.
         * 
         * @param view
         *            The view associated with this listener.
         * @param year
         *            The year that was set.
         * @param monthOfYear
         *            The month that was set (0-11) for compatibility with
         *            {@link java.util.Calendar}.
         * @param dayOfMonth
         *            The day of the month that was set.
         */
        void onDateChanged(LunarDatePicker view, int year, int monthOfYear,
                int dayOfMonth);
    }

    public LunarDatePicker(Context context) {
        this(context, null);
    }

    public LunarDatePicker(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.datePickerStyle);
    }

    public LunarDatePicker(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mLunar = LunarUtil.getInstance(context);
        Resources res = context.getResources();
        mLunarMonthTextArray = res.getStringArray(R.array.lunar_month_array);
        mLunarMonthNumberTextArray = res.getStringArray(R.array.month_number_array);
        /// M: If the leapMonthCode is LEAP_MONTH show special word by getSpecialWord function, because
        //  should show "閏" in TC, "闰" in SC.
        mLunarLeapWordText = mLunar.getSpecialWord(LUNAR_WORD_RUN);

        // initialization based on locale
        setCurrentLocale(Locale.getDefault());

        // /@M:{comment these lines
//         TypedArray attributesArray = context.obtainStyledAttributes(attrs,
//         R.styleable.LunarDatePicker,
//         defStyle, 0);
//         boolean spinnersShown =
//         attributesArray.getBoolean(R.styleable.LunarDatePicker_spinnersShown,
//         DEFAULT_SPINNERS_SHOWN);
//         boolean calendarViewShown = attributesArray.getBoolean(
//         R.styleable.LunarDatePicker_calendarViewShown,
//         DEFAULT_CALENDAR_VIEW_SHOWN);
//         int startYear =
//         attributesArray.getInt(R.styleable.LunarDatePicker_startYear,
//         DEFAULT_START_YEAR);
//         int endYear = attributesArray.getInt(R.styleable.LunarDatePicker_endYear,
//         DEFAULT_END_YEAR);
//         String minDate =
//         attributesArray.getString(R.styleable.LunarDatePicker_minDate);
//         String maxDate =
//         attributesArray.getString(R.styleable.LunarDatePicker_maxDate);
//         int layoutResourceId =R.layout.date_picker;
//         attributesArray.getResourceId(R.styleable.LunarDatePicker_layout,
//         R.layout.date_picker);
//         attributesArray.recycle();
        // /}

        ///M:add @{
        boolean spinnersShown = DEFAULT_SPINNERS_SHOWN;
        boolean calendarViewShown = DEFAULT_CALENDAR_VIEW_SHOWN;
        int startYear = DEFAULT_START_YEAR;
        int endYear = DEFAULT_END_YEAR;
        int layoutResourceId = R.layout.date_picker;
        ///}

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(layoutResourceId, this, true);

        OnValueChangeListener onChangeListener = new OnValueChangeListener() {
            public void onValueChange(NumberPicker picker, int oldVal,
                    int newVal) {
                updateInputState();
                mTempDate.setTimeInMillis(mCurrentDate.getTimeInMillis());
                int gregorianYear = mTempDate.get(Calendar.YEAR);
                int gregorianMonth = mTempDate.get(Calendar.MONTH) + 1;
                int gregorianDay = mTempDate.get(Calendar.DAY_OF_MONTH);
                int lunarDates[] = mLunar.calculateLunarByGregorian(gregorianYear, gregorianMonth, gregorianDay);

                // take care of wrapping of days and months to update greater
                // fields
                if (picker == mDaySpinner) {
                    if (oldVal > 27 && newVal == 1) {
                        mTempDate.add(Calendar.DAY_OF_MONTH, 1);
                    } else if (oldVal == 1 && newVal > 27) {
                        mTempDate.add(Calendar.DAY_OF_MONTH, -1);
                    } else {
                        mTempDate.add(Calendar.DAY_OF_MONTH, newVal - oldVal);
                    }
                } else if (picker == mMonthSpinner) {
                    int leapMonth = 0;
                    int monthCountDays = 0;
                    if (oldVal > 10 && newVal == 0) {
                        leapMonth = mLunar.leapMonth(lunarDates[0]);
                        if (leapMonth == 12) {
                            monthCountDays = mLunar.daysOfLeapMonthInLunarYear(lunarDates[0]);
                        } else {
                            monthCountDays = mLunar.daysOfALunarMonth(lunarDates[0], 12);
                        }
                        mTempDate.add(Calendar.DAY_OF_MONTH, monthCountDays);

                    } else if (oldVal == 0 && newVal > 10) {
                        leapMonth = mLunar.leapMonth(lunarDates[0] - 1);
                        if (leapMonth == 12) {
                            monthCountDays = mLunar.daysOfLeapMonthInLunarYear(lunarDates[0]);
                        } else {
                            monthCountDays = mLunar.daysOfALunarMonth(lunarDates[0] - 1, 12);
                        }

                        mTempDate.add(Calendar.DAY_OF_MONTH, -monthCountDays);
                    } else {
                        leapMonth = mLunar.leapMonth(lunarDates[0]);
                        // move to previous
                        if ((newVal - oldVal) < 0) {
                            if (leapMonth == 0) {
                                monthCountDays = mLunar.daysOfALunarMonth(lunarDates[0],
                                        newVal + 1);
                            } else { // leap year
                                if (newVal < leapMonth) {
                                    monthCountDays = mLunar.daysOfALunarMonth(lunarDates[0],
                                            newVal + 1);
                                } else if (newVal == leapMonth) {
                                    monthCountDays = mLunar.daysOfLeapMonthInLunarYear(lunarDates[0]);
                                } else {
                                    monthCountDays = mLunar.daysOfALunarMonth(lunarDates[0],
                                            newVal);
                                }
                            }
                            monthCountDays = -monthCountDays;
                        } else { // move to next month
                            if (leapMonth == 0) {
                                monthCountDays = mLunar.daysOfALunarMonth(lunarDates[0],
                                        oldVal + 1);
                            } else { // leap year
                                if (oldVal < leapMonth) {
                                    monthCountDays = mLunar.daysOfALunarMonth(lunarDates[0],
                                            oldVal + 1);
                                } else if (oldVal == leapMonth) {
                                    monthCountDays = mLunar.daysOfLeapMonthInLunarYear(lunarDates[0]);
                                } else {
                                    monthCountDays = mLunar.daysOfALunarMonth(lunarDates[0],
                                            oldVal);
                                }
                            }
                        }
                        mTempDate.add(Calendar.DAY_OF_MONTH, monthCountDays);
                    }
                } else if (picker == mYearSpinner) {
                    int orientation = newVal - oldVal > 0 ? LunarUtil.INCREASE_A_LUANR_YEAR
                            : LunarUtil.DECREATE_A_LUANR_YEAR;
                    mTempDate = mLunar.decreaseOrIncreaseALunarYear(mTempDate, lunarDates[1],
                            lunarDates[2], orientation);
                } else {
                    throw new IllegalArgumentException();
                }

                int year = mTempDate.get(Calendar.YEAR);
                int month = mTempDate.get(Calendar.MONTH);
                int day = mTempDate.get(Calendar.DAY_OF_MONTH);
                LogUtil.d(TAG, "onValueChange(), year: " + year + ", month: " + month + ", day: " + day);

                // now set the date to the adjusted one
                setDate(year, month, day);
                updateSpinners();
                updateCalendarView();
                notifyDateChanged();
            }
        };

        mSpinners = (LinearLayout) findViewById(R.id.pickers);

        // calendar view day-picker
        mCalendarView = (CalendarView) findViewById(R.id.calendar_view);
        mCalendarView
                .setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
                    public void onSelectedDayChange(CalendarView view,
                            int year, int month, int monthDay) {
                        setDate(year, month, monthDay);
                        updateSpinners();
                        notifyDateChanged();
                    }
                });

        // day
        mDaySpinner = (NumberPicker) findViewById(R.id.day);
        // /M: comment this line @{
        // mDaySpinner.setFormatter(NumberPicker.TWO_DIGIT_FORMATTER);
        // /}
        mDaySpinner.setOnLongPressUpdateInterval(100);
        mDaySpinner.setOnValueChangedListener(onChangeListener);
        // mDaySpinner has 3 child,topButton,editText,bottomButtom
        if (mDaySpinner.getChildCount() == PICKER_CHILD_COUNT) {
            // set the Middle EditText of Numberpicker read only.
            mDaySpinnerInput = getNumberPickerInputText(mDaySpinner);
            mDaySpinnerInput.setClickable(false);
            mDaySpinnerInput.setFocusable(false);
        } else {
            // Normally,this will always not happen.
            mDaySpinnerInput = new EditText(context);
            Log.e(TAG, "mDaySpinner.getChildCount() != 3,It isn't init ok.");
        }

        // month
        mMonthSpinner = (NumberPicker) findViewById(R.id.month);
        mMonthSpinner.setMinValue(0);
        mMonthSpinner.setMaxValue(mNumberOfMonths - 1);
        mMonthSpinner.setDisplayedValues(mLunarMonthTextArray);
        mMonthSpinner.setOnLongPressUpdateInterval(200);
        mMonthSpinner.setOnValueChangedListener(onChangeListener);
        // mDaySpinner has 3 child,topButton,editText,bottomButtom
        if (mMonthSpinner.getChildCount() == PICKER_CHILD_COUNT) {
            // set the Middle EditText of Numberpicker read only.
            mMonthSpinnerInput = getNumberPickerInputText(mMonthSpinner);
            mMonthSpinnerInput.setClickable(false);
            mMonthSpinnerInput.setFocusable(false);
        } else {
            // Normally,this will always not happen.
            mMonthSpinnerInput = new EditText(context);
            Log.e(TAG, "mMonthSpinner.getChildCount() != 3,It isn't init ok.");
        }
        // year
        mYearSpinner = (NumberPicker) findViewById(R.id.year);
        mYearSpinner.setOnLongPressUpdateInterval(100);
        mYearSpinner.setOnValueChangedListener(onChangeListener);
        // mDaySpinner has 3 child,topButton,editText,bottomButtom
        if (mYearSpinner.getChildCount() == PICKER_CHILD_COUNT) {
            // set the Middle EditText of Numberpicker read only.
            mYearSpinnerInput = getNumberPickerInputText(mYearSpinner);
            mYearSpinnerInput.setClickable(false);
            mYearSpinnerInput.setFocusable(false);
        } else {
            // Normally,this will always not happen.
            mYearSpinnerInput = new EditText(context);
            Log.e(TAG, "mYearSpinner.getChildCount() != 3,It isn't init ok.");
        }

        // show only what the user required but make sure we
        // show something and the spinners have higher priority
        if (!spinnersShown && !calendarViewShown) {
            setSpinnersShown(true);
        } else {
            setSpinnersShown(spinnersShown);
            setCalendarViewShown(calendarViewShown);
        }

        // set the min date giving priority of the minDate over startYear
        mTempDate.clear();
        mTempDate.set(startYear, 0, 1);
        setMinDate(mTempDate.getTimeInMillis());

        // set the max date giving priority of the maxDate over endYear
        mTempDate.clear();
        mTempDate.set(endYear, 11, 31, 23, 59);
        setMaxDate(mTempDate.getTimeInMillis());

        // initialize to current date
        mCurrentDate.setTimeInMillis(System.currentTimeMillis());
        init(mCurrentDate.get(Calendar.YEAR), mCurrentDate.get(Calendar.MONTH),
                mCurrentDate.get(Calendar.DAY_OF_MONTH), null);

        // re-order the number spinners to match the current date format
        reorderSpinners();

        ///M:setContentDescriptions() will not called in JellyBean,comment the code@{
        // set content descriptions
        //AccessibilityManager accessibilityManager = (AccessibilityManager) context
        //        .getSystemService(context.ACCESSIBILITY_SERVICE);
        //if (accessibilityManager.isEnabled()) {
        //    setContentDescriptions();
        //}
        ///@}
    }

    /**
     * Gets the minimal date supported by this {@link DatePicker} in
     * milliseconds since January 1, 1970 00:00:00 in
     * {@link TimeZone#getDefault()} time zone.
     * <p>
     * Note: The default minimal date is 01/01/1900.
     * <p>
     * 
     * @return The minimal supported date.
     */
    public long getMinDate() {
        return mCalendarView.getMinDate();
    }

    /**
     * Sets the minimal date supported by this {@link NumberPicker} in
     * milliseconds since January 1, 1970 00:00:00 in
     * {@link TimeZone#getDefault()} time zone.
     * 
     * @param minDate
     *            The minimal supported date.
     */
    public void setMinDate(long minDate) {
        mTempDate.setTimeInMillis(minDate);
        if (mTempDate.get(Calendar.YEAR) == sMinDate.get(Calendar.YEAR)
                && mTempDate.get(Calendar.DAY_OF_YEAR) != sMinDate.get(Calendar.DAY_OF_YEAR)) {
            Log.w(TAG, "setMinDate failed!:" + mTempDate.get(Calendar.YEAR) + "<->" + sMinDate.get(Calendar.YEAR) + ":"
                            + mTempDate.get(Calendar.DAY_OF_YEAR) + "<->" + sMinDate.get(Calendar.DAY_OF_YEAR));
            return;
        }
        sMinDate.setTimeInMillis(minDate);
        mCalendarView.setMinDate(minDate);
        if (mCurrentDate.before(sMinDate)) {
            mCurrentDate.setTimeInMillis(sMinDate.getTimeInMillis());
            updateCalendarView();
        }
        updateSpinners();
    }

    /**
     * Gets the maximal date supported by this {@link DatePicker} in
     * milliseconds since January 1, 1970 00:00:00 in
     * {@link TimeZone#getDefault()} time zone.
     * <p>
     * Note: The default maximal date is 12/31/2100.
     * <p>
     * 
     * @return The maximal supported date.
     */
    public long getMaxDate() {
        return mCalendarView.getMaxDate();
    }

    /**
     * Sets the maximal date supported by this {@link DatePicker} in
     * milliseconds since January 1, 1970 00:00:00 in
     * {@link TimeZone#getDefault()} time zone.
     * 
     * @param maxDate
     *            The maximal supported date.
     */
    public void setMaxDate(long maxDate) {
        mTempDate.setTimeInMillis(maxDate);
        if (mTempDate.get(Calendar.YEAR) == sMaxDate.get(Calendar.YEAR)
                && mTempDate.get(Calendar.DAY_OF_YEAR) != sMaxDate.get(Calendar.DAY_OF_YEAR)) {
            Log.w(TAG, "setMaxDate failed!:" + mTempDate.get(Calendar.YEAR) + "<->" + sMaxDate.get(Calendar.YEAR) + ":"
                    + mTempDate.get(Calendar.DAY_OF_YEAR) + "<->" + sMaxDate.get(Calendar.DAY_OF_YEAR));
            return;
        }
        sMaxDate.setTimeInMillis(maxDate);
        mCalendarView.setMaxDate(maxDate);
        if (mCurrentDate.after(sMaxDate)) {
            mCurrentDate.setTimeInMillis(sMaxDate.getTimeInMillis());
            updateCalendarView();
        }
        updateSpinners();
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (mIsEnabled == enabled) {
            return;
        }
        super.setEnabled(enabled);
        mDaySpinner.setEnabled(enabled);
        mMonthSpinner.setEnabled(enabled);
        mYearSpinner.setEnabled(enabled);
        mCalendarView.setEnabled(enabled);
        mIsEnabled = enabled;
    }

    @Override
    public boolean isEnabled() {
        return mIsEnabled;
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        onPopulateAccessibilityEvent(event);
        return true;
    }

    @Override
    public void onPopulateAccessibilityEvent(AccessibilityEvent event) {
        super.onPopulateAccessibilityEvent(event);

        final int flags = DateUtils.FORMAT_SHOW_DATE
                | DateUtils.FORMAT_SHOW_YEAR;
        String selectedDateUtterance = DateUtils.formatDateTime(getContext(),
                mCurrentDate.getTimeInMillis(), flags);
        event.getText().add(selectedDateUtterance);
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setClassName(DatePicker.class.getName());
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(DatePicker.class.getName());
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setCurrentLocale(newConfig.locale);
    }

    /**
     * Gets whether the {@link CalendarView} is shown.
     * 
     * @return True if the calendar view is shown.
     * @see #getCalendarView()
     */
    public boolean getCalendarViewShown() {
        return mCalendarView.isShown();
    }

    /**
     * Gets the {@link CalendarView}.
     * 
     * @return The calendar view.
     * @see #getCalendarViewShown()
     */
    public CalendarView getCalendarView() {
        return mCalendarView;
    }

    /**
     * Sets whether the {@link CalendarView} is shown.
     * 
     * @param shown
     *            True if the calendar view is to be shown.
     */
    public void setCalendarViewShown(boolean shown) {
        mCalendarView.setVisibility(shown ? VISIBLE : GONE);
    }

    /**
     * Gets whether the spinners are shown.
     * 
     * @return True if the spinners are shown.
     */
    public boolean getSpinnersShown() {
        return mSpinners.isShown();
    }

    /**
     * Sets whether the spinners are shown.
     * 
     * @param shown
     *            True if the spinners are to be shown.
     */
    public void setSpinnersShown(boolean shown) {
        mSpinners.setVisibility(shown ? VISIBLE : GONE);
    }

    /**
     * Sets the current locale.
     * 
     * @param locale
     *            The current locale.
     */
    private void setCurrentLocale(Locale locale) {
        if (locale.equals(mCurrentLocale)) {
            return;
        }

        mCurrentLocale = locale;

        mTempDate = getCalendarForLocale(mTempDate, locale);
        sMinDate = getCalendarForLocale(sMinDate, locale);
        sMaxDate = getCalendarForLocale(sMaxDate, locale);
        mCurrentDate = getCalendarForLocale(mCurrentDate, locale);

        // /M:comment @{
        // mNumberOfMonths = mTempDate.getActualMaximum(Calendar.MONTH) + 1;
        // mShortMonths = new String[mNumberOfMonths];
        // for (int i = 0; i < mNumberOfMonths; i++) {
        // mShortMonths[i] = DateUtils.getMonthString(Calendar.JANUARY + i,
        // DateUtils.LENGTH_MEDIUM);
        // }
        // /@}
    }

    /**
     * Gets a calendar for locale bootstrapped with the value of a given
     * calendar.
     * 
     * @param oldCalendar
     *            The old calendar.
     * @param locale
     *            The locale.
     */
    private Calendar getCalendarForLocale(Calendar oldCalendar, Locale locale) {
        if (oldCalendar == null) {
            return Calendar.getInstance(locale);
        } else {
            final long currentTimeMillis = oldCalendar.getTimeInMillis();
            Calendar newCalendar = Calendar.getInstance(locale);
            newCalendar.setTimeInMillis(currentTimeMillis);
            return newCalendar;
        }
    }

    /**
     * Reorders the spinners according to the date format that is explicitly set
     * by the user and if no such is set fall back to the current locale's
     * default format.
     */
    private void reorderSpinners() {
        mSpinners.removeAllViews();
        char[] order = DateFormat.getDateFormatOrder(getContext());
        final int spinnerCount = order.length;
        for (int i = 0; i < spinnerCount; i++) {
            switch (order[i]) {
            case DateFormat.DATE:
                mSpinners.addView(mDaySpinner);
                setImeOptions(mDaySpinner, spinnerCount, i);
                break;
            case DateFormat.MONTH:
                mSpinners.addView(mMonthSpinner);
                setImeOptions(mMonthSpinner, spinnerCount, i);
                break;
            case DateFormat.YEAR:
                mSpinners.addView(mYearSpinner);
                setImeOptions(mYearSpinner, spinnerCount, i);
                break;
            default:
                throw new IllegalArgumentException();
            }
        }
    }

    /**
     * Updates the current date.
     * 
     * @param year
     *            The year.
     * @param month
     *            The month which is <strong>starting from zero</strong>.
     * @param dayOfMonth
     *            The day of the month.
     */
    public void updateDate(int year, int month, int dayOfMonth) {
        if (!isNewDate(year, month, dayOfMonth)) {
            return;
        }
        setDate(year, month, dayOfMonth);
        updateSpinners();
        updateCalendarView();
        notifyDateChanged();
    }

    // Override so we are in complete control of save / restore for this widget.
    @Override
    protected void dispatchRestoreInstanceState(
            SparseArray<Parcelable> container) {
        dispatchThawSelfOnly(container);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        return new SavedState(superState, getYear(), getMonth(),
                getDayOfMonth());
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        setDate(ss.mYear, ss.mMonth, ss.mDay);
        updateSpinners();
        updateCalendarView();
    }

    /**
     * Initialize the state. If the provided values designate an inconsistent
     * date the values are normalized before updating the spinners.
     * 
     * @param year
     *            The initial year.
     * @param monthOfYear
     *            The initial month <strong>starting from zero</strong>.
     * @param dayOfMonth
     *            The initial day of the month.
     * @param onDateChangedListener
     *            How user is notified date is changed by user, can be null.
     */
    public void init(int year, int monthOfYear, int dayOfMonth,
            OnDateChangedListener onDateChangedListener) {
        setDate(year, monthOfYear, dayOfMonth);
        updateSpinners();
        updateCalendarView();
        mOnDateChangedListener = onDateChangedListener;
    }

    private boolean isNewDate(int year, int month, int dayOfMonth) {
        return (mCurrentDate.get(Calendar.YEAR) != year
                || mCurrentDate.get(Calendar.MONTH) != dayOfMonth || mCurrentDate
                .get(Calendar.DAY_OF_MONTH) != month);
    }

    private void setDate(int year, int month, int dayOfMonth) {
        mCurrentDate.set(year, month, dayOfMonth);
        if (mCurrentDate.before(sMinDate)) {
            LogUtil.d(TAG, "befor min date, set current to min date.");
            mCurrentDate.setTimeInMillis(sMinDate.getTimeInMillis());
        } else if (mCurrentDate.after(sMaxDate)) {
            LogUtil.d(TAG, "after max date, set current to max date.");
            mCurrentDate.setTimeInMillis(sMaxDate.getTimeInMillis());
        }
    }

    private void updateSpinners() {
        boolean isLeapYear = false;
        int gregorianYear = mCurrentDate.get(Calendar.YEAR);
        int gregorianMonth = mCurrentDate.get(Calendar.MONTH) + 1;
        int gregorianDay = mCurrentDate.get(Calendar.DAY_OF_MONTH);
        int lunarDate[] = mLunar.calculateLunarByGregorian(gregorianYear, gregorianMonth, gregorianDay);
        int leapMonth = mLunar.leapMonth(lunarDate[0]);
        int monthIndexDisplay = lunarDate[1];
        String lunarDateString = mLunar.getLunarDateString(mCurrentDate);

        if (leapMonth == 0) {
            monthIndexDisplay--;
        } else if (monthIndexDisplay < leapMonth && leapMonth != 0) {
            monthIndexDisplay--;
        } else if ((monthIndexDisplay == leapMonth) && (!lunarDateString.contains(mLunarLeapWordText))) {
            monthIndexDisplay--;
        }

        if (leapMonth != 0) {
            mNumberOfMonths = 13;
            isLeapYear = true;
        } else {
            mNumberOfMonths = 12;
        }

        int monthCountDays = mLunar.daysOfALunarMonth(lunarDate[0], lunarDate[1]);
        if ((leapMonth != 0) && (monthIndexDisplay == leapMonth)
                && (!lunarDateString.contains(mLunarLeapWordText))) {
            monthCountDays = mLunar.daysOfLeapMonthInLunarYear(lunarDate[0]);
        }

        // set the spinner ranges respecting the min and max dates
        if (mCurrentDate.equals(sMinDate)) {
            mDaySpinner.setDisplayedValues(null);
            mDaySpinner.setMinValue(lunarDate[2]);
            mDaySpinner.setMaxValue(monthCountDays);
            mDaySpinner.setWrapSelectorWheel(false);
            mMonthSpinner.setDisplayedValues(null);
            mMonthSpinner.setMinValue(monthIndexDisplay);
            mMonthSpinner.setMaxValue(mNumberOfMonths - 1);
            mMonthSpinner.setWrapSelectorWheel(false);

            LogUtil.d(TAG, "set spinners to min date.");
        } else if (mCurrentDate.equals(sMaxDate)) {
            mDaySpinner.setDisplayedValues(null);
            mDaySpinner.setMinValue(1);
            mDaySpinner.setMaxValue(lunarDate[2]);
            mDaySpinner.setWrapSelectorWheel(false);
            mMonthSpinner.setDisplayedValues(null);
            mMonthSpinner.setMinValue(0);
            mMonthSpinner.setMaxValue(monthIndexDisplay);
            mMonthSpinner.setWrapSelectorWheel(false);
            LogUtil.d(TAG, "set spinners to max date.");
        } else {
            mDaySpinner.setDisplayedValues(null);
            mDaySpinner.setMinValue(1);
            mDaySpinner.setMaxValue(monthCountDays);
            mDaySpinner.setWrapSelectorWheel(true);
            mMonthSpinner.setDisplayedValues(null);
            mMonthSpinner.setMinValue(0);
            mMonthSpinner.setMaxValue(mNumberOfMonths - 1);
            mMonthSpinner.setWrapSelectorWheel(true);
        }

        String[] displayedMonths = new String[mNumberOfMonths];
        if (isLeapYear) {
            int i = 0;
            for (; i < leapMonth; i++) {
                displayedMonths[i] = mLunarMonthTextArray[i];
            }
            displayedMonths[leapMonth] = mLunarLeapWordText + mLunarMonthNumberTextArray[leapMonth - 1];
            i++;
            for (; i < 13; i++) {
                displayedMonths[i] = mLunarMonthTextArray[i - 1];
            }
        } else {
            displayedMonths = Arrays.copyOfRange(mLunarMonthTextArray, mMonthSpinner
                    .getMinValue(), mMonthSpinner.getMaxValue() + 1);
        }
        /**
         * M: Do some workaround to avoid NumberPicker.setDisplayedValues to modify the minimum value or maximum value to default  @{
         */
        int monthMin = mMonthSpinner.getMinValue();
        int monthMax = mMonthSpinner.getMaxValue();
        if (monthMin >= displayedMonths.length) {
            mMonthSpinner.setMinValue(0);
        }
        if (monthMax >= displayedMonths.length) {
            mMonthSpinner.setMaxValue(displayedMonths.length - 1);
        }
        /** @} */
        mMonthSpinner.setDisplayedValues(displayedMonths);
        /**
         * M: Do some workaround to avoid NumberPicker.setDisplayedValues to modify the minimum value or maximum value to default  @{
         */
        mMonthSpinner.setMinValue(monthMin);
        mMonthSpinner.setMaxValue(monthMax);
        /** @} */

        int max = mDaySpinner.getMaxValue();
        int min = mDaySpinner.getMinValue();
        String[] displayedDays = new String[max - min + 1];

        for (int i = min; i <= max; i++) {
            displayedDays[i - min] = mLunar.getLunarDayString(i);
        }
        /**
         * M: Do some workaround to avoid NumberPicker.setDisplayedValues to modify the minimum value or maximum value to default  @{
         */
        if (min >= displayedDays.length) {
            mDaySpinner.setMinValue(0);
        }
        if (max >= displayedDays.length) {
            mDaySpinner.setMaxValue(displayedDays.length - 1);
        }
        /** @} */
        mDaySpinner.setDisplayedValues(displayedDays);
        /**
         * M: Do some workaround to avoid NumberPicker.setDisplayedValues to modify the minimum value or maximum value to default  @{
         */
        mDaySpinner.setMinValue(min);
        mDaySpinner.setMaxValue(max);

        if (mCurrentDate.equals(sMinDate)|| mCurrentDate.equals(sMaxDate)) {
            mDaySpinner.setWrapSelectorWheel(false);
            mMonthSpinner.setWrapSelectorWheel(false);
        } else {
            mDaySpinner.setWrapSelectorWheel(true);
            mMonthSpinner.setWrapSelectorWheel(true);
        }
        /** @} */

        // year spinner range does not change based on the current date
        int minGregorianYear = sMinDate.get(Calendar.YEAR);
        int minGregorianMonth = sMinDate.get(Calendar.MONTH) + 1;
        int minGregorianDay = sMinDate.get(Calendar.DAY_OF_MONTH);
        int minLunarDate[] = mLunar.calculateLunarByGregorian(minGregorianYear, minGregorianMonth,
                minGregorianDay);

        int maxGregorianYear = sMaxDate.get(Calendar.YEAR);
        int maxGregorianMonth = sMaxDate.get(Calendar.MONTH) + 1;
        int maxGregorianDay = sMaxDate.get(Calendar.DAY_OF_MONTH);
        int maxLunarDate[] = mLunar.calculateLunarByGregorian(maxGregorianYear, maxGregorianMonth,
                maxGregorianMonth);

        mYearSpinner.setMinValue(minLunarDate[0]);
        mYearSpinner.setMaxValue(maxLunarDate[0]);
        mYearSpinner.setWrapSelectorWheel(false);

        mYearSpinner.setValue(lunarDate[0]);
        mMonthSpinner.setValue(monthIndexDisplay);
        mDaySpinner.setValue(lunarDate[2]);

    }

    /**
     * Updates the calendar view with the current date.
     */
    private void updateCalendarView() {
        mCalendarView.setDate(mCurrentDate.getTimeInMillis(), false, false);
    }

    /**
     * @return The selected year.
     */
    public int getYear() {
        return mCurrentDate.get(Calendar.YEAR);
    }

    /**
     * @return The selected month.
     */
    public int getMonth() {
        return mCurrentDate.get(Calendar.MONTH);
    }

    /**
     * @return The selected day of month.
     */
    public int getDayOfMonth() {
        return mCurrentDate.get(Calendar.DAY_OF_MONTH);
    }

    /**
     * Notifies the listener, if such, for a change in the selected date.
     */
    private void notifyDateChanged() {
        sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);
        if (mOnDateChangedListener != null) {
            mOnDateChangedListener.onDateChanged(this, getYear(), getMonth(),
                    getDayOfMonth());
        }
    }

    /**
     * Sets the IME options for a spinner based on its ordering.
     * 
     * @param spinner
     *            The spinner.
     * @param spinnerCount
     *            The total spinner count.
     * @param spinnerIndex
     *            The index of the given spinner.
     */
    private void setImeOptions(NumberPicker spinner, int spinnerCount,
            int spinnerIndex) {
        final int imeOptions;
        if (spinnerIndex < spinnerCount - 1) {
            imeOptions = EditorInfo.IME_ACTION_NEXT;
        } else {
            imeOptions = EditorInfo.IME_ACTION_DONE;
        }
        
        //check if the spinner is init ok
        if (spinner.getChildCount() != PICKER_CHILD_COUNT) {
            Log.e(TAG, "spinner.getChildCount() != 3,It isn't init ok.return");
            return ;
        }
        //get the middle EditText of  NumberPicker
        TextView input = (TextView)spinner.getChildAt(PICKER_CHILD_INPUT_TEXT_INDEX);
        input.setImeOptions(imeOptions);
    }

    ///M:In JellyBean,there is only can get Edit input text,so this is no use here.
    ///just comment here.@{
    /*private void setContentDescriptions() {
        Context context = getContext();
        //check if the compponent is init ok
        if(mDaySpinner.getChildCount() != PICKER_CHILD_COUNT){
            Log.e(TAG, "mDaySpinner.getChildCount() != 3,It isn't init ok.return");
            return ;
        }else if (mMonthSpinner.getChildCount() != PICKER_CHILD_COUNT) {
            Log.e(TAG, "mMonthSpinner.getChildCount() != 3,It isn't init ok.return");
            return ;
        }else if (mYearSpinner.getChildCount() != PICKER_CHILD_COUNT) {
            Log.e(TAG, "mYearSpinner.getChildCount() != 3,It isn't init ok.return");
            return ;
        }
            
        // Day
        String text = context
                .getString(R.string.date_picker_increment_day_button);
        //set the Top Button of NumberPicker
        ((ImageButton)mDaySpinner.getChildAt(0)).setContentDescription(text);
        text = context.getString(R.string.date_picker_decrement_day_button);
        //set the Bottom Button of NumberPicker
        ((ImageButton)mDaySpinner.getChildAt(2)).setContentDescription(text);
        
        // Month
        //set the Top Button of NumberPicker
        text = context.getString(R.string.date_picker_increment_month_button);
        ((ImageButton)mMonthSpinner.getChildAt(0)).setContentDescription(text);
        text = context.getString(R.string.date_picker_decrement_month_button);
        //set the Bottom Button of NumberPicker
        ((ImageButton)mMonthSpinner.getChildAt(2)).setContentDescription(text);
        
        // Year
        //set the Top Button of NumberPicker
        text = context.getString(R.string.date_picker_increment_year_button);
        ((ImageButton)mYearSpinner.getChildAt(0)).setContentDescription(text);
        //set the Bottom Button of NumberPicker
        text = context.getString(R.string.date_picker_decrement_year_button);
        ((ImageButton)mYearSpinner.getChildAt(2)).setContentDescription(text);
    }*/
    ///@}

    private void updateInputState() {
        // Make sure that if the user changes the value and the IME is active
        // for one of the inputs if this widget, the IME is closed. If the user
        // changed the value via the IME and there is a next input the IME will
        // be shown, otherwise the user chose another means of changing the
        // value and having the IME up makes no sense.
        Context context = getContext();
        InputMethodManager inputMethodManager = (InputMethodManager) context
                .getSystemService(context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            if (inputMethodManager.isActive(mYearSpinnerInput)) {
                mYearSpinnerInput.clearFocus();
                inputMethodManager.hideSoftInputFromWindow(getWindowToken(), 0);
            } else if (inputMethodManager.isActive(mMonthSpinnerInput)) {
                mMonthSpinnerInput.clearFocus();
                inputMethodManager.hideSoftInputFromWindow(getWindowToken(), 0);
            } else if (inputMethodManager.isActive(mDaySpinnerInput)) {
                mDaySpinnerInput.clearFocus();
                inputMethodManager.hideSoftInputFromWindow(getWindowToken(), 0);
            }
        }
    }

    /**
     * Class for managing state storing/restoring.
     */
    private static class SavedState extends BaseSavedState {

        private final int mYear;

        private final int mMonth;

        private final int mDay;

        /**
         * Constructor called from {@link DatePicker#onSaveInstanceState()}
         */
        private SavedState(Parcelable superState, int year, int month, int day) {
            super(superState);
            mYear = year;
            mMonth = month;
            mDay = day;
        }

        /**
         * Constructor called from {@link #CREATOR}
         */
        private SavedState(Parcel in) {
            super(in);
            mYear = in.readInt();
            mMonth = in.readInt();
            mDay = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(mYear);
            dest.writeInt(mMonth);
            dest.writeInt(mDay);
        }

        @SuppressWarnings("all")
        // suppress unused and hiding
        public static final Parcelable.Creator<SavedState> CREATOR = new Creator<SavedState>() {

            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
    
    /**
     * get the Edit input text of NumberPicker.
     * @param numberPicker The NumberPicker to be get
     * @return the EditText get
     */
    public EditText getNumberPickerInputText(NumberPicker numberPicker) {
        return (EditText)numberPicker.getChildAt(PICKER_CHILD_INPUT_TEXT_INDEX);
    }

    public CharSequence getDateString() {
        return mLunar.getLunarDateString(mCurrentDate);
    }
}