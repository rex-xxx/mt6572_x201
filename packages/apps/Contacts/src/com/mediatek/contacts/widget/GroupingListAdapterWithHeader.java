package com.mediatek.contacts.widget;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.android.common.widget.GroupingListAdapter;

import java.util.HashMap;

public abstract class GroupingListAdapterWithHeader extends GroupingListAdapter {

    private static final String LOG_TAG = "GroupingListAdapterWithHeader";

    private Cursor mCursor;
    HashMap<Integer, Boolean> mHeaderPositionList = new HashMap<Integer, Boolean>();

    public GroupingListAdapterWithHeader(Context context) {
        super(context);
        mHeaderPositionList.clear();
    }

    public void changeCursor(Cursor cursor) {
        log("changeCursor(), cursor = " + cursor);
        if (null != cursor) {
            log("cursor count = " + cursor.getCount());
        }
        if (mCursor != cursor) {
            mCursor = cursor;
            mHeaderPositionList.clear();
        }
        super.changeCursor(cursor);
    }

    public boolean isDateGroupHeader(int cursorPosition) {
        Boolean isDateGroupHeader = mHeaderPositionList.get(Integer.valueOf(cursorPosition));
        return (null == isDateGroupHeader) ? false : isDateGroupHeader.booleanValue();
    }

    public void setGroupHeaderPosition(int cursorPosition) {
        mHeaderPositionList.put(Integer.valueOf(cursorPosition), Boolean.valueOf(true));
    }
    
    private void log(final String log) {
        Log.i(LOG_TAG, log);
    }
}
