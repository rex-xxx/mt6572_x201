package com.mediatek.calendar.extension;

import android.app.DatePickerDialog.OnDateSetListener;
import android.view.View;

import com.android.calendar.CalendarEventModel;

/**
 * M: the EditEventView must provide some service to the plug-in
 * the ways are defined here.
 * this interface can protect the view's data while service outside
 * world well.
 */
public interface IEditEventView {
    /**
     * reset the start and end button's text
     */
    void resetDateButton();
    
    /**
     * provide the Model to outside world
     * FIXME: this interface is risky, should be corrected
     * @return model
     */
    CalendarEventModel getModel();

    /**
     * The plug-in need the DateListener class, but it's a private class.
     * so provide the OnDateSetListener instead.
     * @param view view clicked
     * @return the new DateListener
     */
    OnDateSetListener getOnDateSetListener(View view);

    /**
     * the attendees group will be set invisible when the account
     * is pc sync. so this function is provided.
     * @param visible View.VISIBLE/GONE
     */
    void setAttendeesGroupVisibility(int visible);
}
