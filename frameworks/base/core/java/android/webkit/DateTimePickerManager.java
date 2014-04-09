/// M: HTML5 date/time input type support @{
package android.webkit;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.text.format.DateFormat;
import android.webkit.WebViewClassic;
import android.widget.DatePicker;
import android.widget.TimePicker;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * @hide
 */
public final class DateTimePickerManager {
    static final String LOGTAG = "webview";

    public static final int DATE_TIME_PICKER  = 1;
    public static final int DATE_PICKER       = 2;
    public static final int MONTH_PICKER      = 3;
    public static final int WEEK_PICKER       = 4;
    public static final int TIME_PICKER       = 5;

    public static int getPickerId(String type) {
        if (type.contains("datetime") ||
            type.contains("datetime-local")) {
            return DATE_TIME_PICKER;
        }

        if (type.contains("date")) {
            return DATE_PICKER;
        }

        if (type.contains("month")) {
            return MONTH_PICKER;
        }

        if (type.contains("week")) {
            return WEEK_PICKER;
        }

        if (type.contains("time")) {
            return TIME_PICKER;
        }

        return DATE_TIME_PICKER;
    }

    private static Date convertToDate(int type, String value) {
        if (value == null || value.isEmpty()) {
            return new Date();
        }
        try {
            switch (type) {
                case DATE_TIME_PICKER :
                case DATE_PICKER :
                    return new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(value);
                case MONTH_PICKER :
                case WEEK_PICKER :
                case TIME_PICKER :
                    return new SimpleDateFormat("HH:mm", Locale.US).parse(value);
            }
        } catch (java.text.ParseException e) {
            Log.v(LOGTAG, "WebViewClassic convertToDate : " + value + ", exception" + e);
        }
        return new Date();
    }

    public static AlertDialog createPicker(final WebViewClassic view, Context ctx, int type, String value) {
        switch(type) {
            case DATE_TIME_PICKER :
            case DATE_PICKER : {
                final Date date = convertToDate(type, value);
                return new DatePickerDialog(ctx,
                    new DatePickerDialog.OnDateSetListener() {
                        public void onDateSet(DatePicker picker, int year, int monthOfYear, int dayOfMonth) {
                            date.setYear(year - 1900);
                            date.setMonth(monthOfYear);
                            date.setDate(dayOfMonth);
                            String output = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date);
                            view.replaceTextfieldText(0, 255, output, 0, 255);
                        }
                    }, date.getYear() + 1900, date.getMonth(), date.getDate());
            }
            case MONTH_PICKER :
            case WEEK_PICKER :
            case TIME_PICKER : {
                final Date date = convertToDate(type, value);
                return new TimePickerDialog(ctx,
                    new TimePickerDialog.OnTimeSetListener() {
                        public void onTimeSet(TimePicker picker, int hourOfDay, int minute) {
                            date.setHours(hourOfDay);
                            date.setMinutes(minute);
                            String output = new SimpleDateFormat("HH:mm", Locale.US).format(date);
                            view.replaceTextfieldText(0, 255, output, 0, 255);
                        }
                    }, date.getHours(), date.getMinutes(), DateFormat.is24HourFormat(ctx));
            }
        }
        return null;
    }
}
/// @}
