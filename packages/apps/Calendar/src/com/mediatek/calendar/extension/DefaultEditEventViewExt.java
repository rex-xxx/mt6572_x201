package com.mediatek.calendar.extension;

import android.database.Cursor;
import android.text.format.Time;
import android.view.View;

import com.android.calendar.CalendarEventModel;

public class DefaultEditEventViewExt implements IEditEventViewExt {

    @Override
    public boolean isExtensionEnabled() {
        return false;
    }

    @Override
    public void onDateClicked(View v, Time time) {
    }

    @Override
    public void updateDatePickerSelection() {
    }

    @Override
    public String getDateStringFromMillis(long millis) {
        return null;
    }

    @Override
    public void setExtUI(CalendarEventModel model) {
        
    }

    @Override
    public void onAccountItemSelected(Cursor cursor) {
    }

}
