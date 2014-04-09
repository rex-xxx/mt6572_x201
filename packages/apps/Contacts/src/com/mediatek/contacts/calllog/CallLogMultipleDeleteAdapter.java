/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mediatek.contacts.calllog;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.View;

import com.android.contacts.PhoneCallDetails;
import com.android.contacts.calllog.CallLogAdapter;
import com.android.contacts.calllog.CallLogQuery;
import com.android.contacts.calllog.ContactInfoHelper;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class CallLogMultipleDeleteAdapter extends CallLogAdapter {

    private static final String LOG_TAG = "CallLogMultipleDeleteAdapter";
    private Cursor mCursor;

    private final Map<Integer, Integer> mSelectedCursorItemStatusMap =
                                                    new HashMap<Integer, Integer>();

    /**
     * Construct function
     * 
     * @param context context
     * @param callFetcher Callfetcher
     * @param contactInfoHelper contactinfohelper
     * @param voicemailNumber voicemailNumber
     */
    public CallLogMultipleDeleteAdapter(Context context, CallFetcher callFetcher,
            ContactInfoHelper contactInfoHelper, String voicemailNumber) {
        super(context, callFetcher, contactInfoHelper);
    }

    /**
     * @param cursor cursor
     */
    public void changeCursor(Cursor cursor) {
        log("changeCursor(), cursor = " + cursor);
        if (null != cursor) {
            log("cursor count = " + cursor.getCount());
        }
        if (mCursor != cursor) {
            mCursor = cursor;
        }
        super.changeCursor(cursor);
    }

    /**
     * Binds the views in the entry to the data in the call log.
     *
     * @param view the view corresponding to this entry
     * @param c the cursor pointing to the entry in the call log
     * @param count the number of entries in the current item, greater than 1 if it is a group
     */
    protected void bindView(View view, Cursor c, int count) {
        log("bindView(), cursor = " + c + " count = " + count);

        super.bindView(view, c, count);
        CallLogListItemView itemView = (CallLogListItemView) view;
        // set check box state
        Integer cursorPosition = mSelectedCursorItemStatusMap.get(c.getPosition());
        itemView.setCheckBoxMultiSel(false, false);
        final Boolean checkState = (null == cursorPosition) ? false : true;
        itemView.getCheckBoxMultiSel().setChecked(checkState);
        if (null != cursorPosition) {
            mSelectedCursorItemStatusMap.put(c.getPosition(), Integer.valueOf(count));
        }
        
    }

    /**
     * select all items
     * 
     * @return the selected items numbers
     */
    public int selectAllItems() {
        log("selectAllItems()");
        for (int i = 0; i < getCount(); ++i) {
            int count = 0;
            // Below code also need check child type,
            // but current do not use child type view
            // so need consider it in the future
            if (isGroupHeader(i)) {
                count = getGroupSize(i);
            } else {
                count = 1;
            }
            Cursor cursor = (Cursor)getItem(i);
            mSelectedCursorItemStatusMap.put(Integer.valueOf(cursor.getPosition()), Integer
                    .valueOf(count));
        }
        return mSelectedCursorItemStatusMap.size();
    }

    /**
     * unselect all items
     */
    public void unSelectAllItems() {
        log("unSelectAllItems()");
        mSelectedCursorItemStatusMap.clear();
    }

    /**
     * get delete filter
     * 
     * @return the delete selection
     */
    public String getDeleteFilter() {
        log("getDeleteFilter()");
        StringBuilder where = new StringBuilder("_id in ");
        where.append("(");
        if (mSelectedCursorItemStatusMap.size() > 0) {
            Iterator iterator = mSelectedCursorItemStatusMap.entrySet().iterator();
            Map.Entry entry = (Map.Entry) iterator.next();
            Integer key = (Integer) entry.getKey();
            Integer value = (Integer) entry.getValue();
            if (null == mCursor || !mCursor.moveToPosition(key)) {
                return "";
            }
            where.append("\'");
            where.append(mCursor.getInt(CallLogQuery.ID));
            where.append("\'");
            while (mCursor.moveToNext() && value > 1) {
                mCursor.getInt(CallLogQuery.ID);
                where.append(",");
                where.append("\'");
                where.append(mCursor.getInt(CallLogQuery.ID));
                where.append("\'");
                value--;
            }
            while (iterator.hasNext()) {
                entry = (Map.Entry) iterator.next();
                key = (Integer) entry.getKey();
                value = (Integer) entry.getValue();
                mCursor.moveToPosition(key);
                where.append(",");
                where.append("\'");
                where.append(mCursor.getInt(CallLogQuery.ID));
                where.append("\'");

                while (mCursor.moveToNext() && value > 1) {
                    mCursor.getInt(CallLogQuery.ID);
                    where.append(",");
                    where.append("\'");
                    where.append(mCursor.getInt(CallLogQuery.ID));
                    where.append("\'");
                    value--;
                }
            }
        } else {
            where.append(-1);
        }

        where.append(")");
        log("getDeleteFilter() where ==  " + where.toString());
        return where.toString();
    }

    /**
     * change selected status to map
     * 
     * @param listPosition position to change
     * @return int
     */
    public int changeSelectedStatusToMap(final int listPosition) {
        log("changeSelectedStatusToMap()");
        int count = 0;
        if (isGroupHeader(listPosition)) {
            count = getGroupSize(listPosition);
        } else {
            count = 1;
        }
        Cursor cursor = (Cursor)getItem(listPosition);
        if (null != cursor) {
            if (null == mSelectedCursorItemStatusMap.get(cursor.getPosition())) {
                mSelectedCursorItemStatusMap.put(Integer.valueOf(cursor.getPosition()), Integer
                        .valueOf(count));
            } else {
                mSelectedCursorItemStatusMap.remove(new Integer(cursor.getPosition()));
            }
        }
        return mSelectedCursorItemStatusMap.size();
    }
    
    protected void bindCallButtonView(CallLogListItemView itemView, PhoneCallDetails details) {
        // override to prevent calling it in this class
    }
    
    /**
     * get selected items count
     * 
     * @return the count of selected
     */
    public int getSelectedItemCount() {
        log("getSelectedItemCount()");
        return mSelectedCursorItemStatusMap.size();
    }

    private void log(final String log) {
        Log.i(LOG_TAG, log);
    }
}
