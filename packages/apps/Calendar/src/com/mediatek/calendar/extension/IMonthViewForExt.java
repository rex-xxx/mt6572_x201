package com.mediatek.calendar.extension;

import android.text.format.Time;

/**
 * M: There are some information and service MonthView should provide to
 * the plug-ins. this interface describes the ways MonthView serves.
 */
public interface IMonthViewForExt {
    /**
     * M: the plug-in provides a location (x, y), and the host should
     * tell which cell this location belongs to. tell the plug-in the
     * Time of it.
     * @param x x
     * @param y y
     * @return the cell's Time
     */
    Time getTimeFromLocation(int x, int y);
}
