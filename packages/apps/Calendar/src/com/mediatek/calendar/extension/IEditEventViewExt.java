package com.mediatek.calendar.extension;

import android.database.Cursor;
import android.text.format.Time;
import android.view.View;

import com.android.calendar.CalendarEventModel;

/**
 * M: interface defines the way to extend the EditEventView's function
 */
public interface IEditEventViewExt {

    /**
     * we have to update the DatePicker selection through the fragment or activity's
     * life cycle.
     */
    void updateDatePickerSelection();

    /**
     * additional UI Elements are set in this method
     * @param model model can provide some info
     */
    void setExtUI(CalendarEventModel model);

    /**
     * get the extended string such as lunar string to tell the Date
     * @param millis the millis time
     * @return null means the extension won't handle the translation, 
     * other means the extension had changed the millis to string.
     */
    String getDateStringFromMillis(long millis);

    /**
     * When the date button clicked, this function will judge which
     * datepicker will displayed
     * @param v the button clicked
     * @param time the mTime
     */
    void onDateClicked(View v, Time time);

    /**
     * if the Extension enabled some flow won't perform google way
     * @return true means the extension is enabled, false means not.
     */
    boolean isExtensionEnabled();

    /**
     * When some specific account was selected, some additional operation
     * should perform. such as PC Sync, will hide the Attendee field.
     * @param cursor to retrive account type infos
     */
    void onAccountItemSelected(Cursor cursor);
}
