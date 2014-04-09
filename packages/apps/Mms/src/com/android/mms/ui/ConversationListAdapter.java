/*
 * Copyright (C) 2008 Esmertec AG.
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.mms.ui;


import com.android.mms.R;
import com.android.mms.LogTag;
import com.android.mms.data.Conversation;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.CursorAdapter;

/// M:
import java.util.HashSet;
import java.util.Iterator;
import com.mediatek.encapsulation.MmsLog;
/**
 * The back-end data adapter for ConversationList.
 */
//TODO: This should be public class ConversationListAdapter extends ArrayAdapter<Conversation>
public class ConversationListAdapter extends MessageCursorAdapter implements AbsListView.RecyclerListener {
    private static final String TAG = "ConversationListAdapter";
    private static final boolean LOCAL_LOGV = false;

    private final LayoutInflater mFactory;
    private OnContentChangedListener mOnContentChangedListener;

    /// M:
    private boolean mSubjectSingleLine = false;

    public ConversationListAdapter(Context context, Cursor cursor) {
        super(context, cursor, false /* auto-requery */);
        mFactory = LayoutInflater.from(context);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        if (!(view instanceof ConversationListItem)) {
            Log.e(TAG, "Unexpected bound view: " + view);
            return;
        }

        ConversationListItem headerView = (ConversationListItem) view;

        /// M: Code analyze 027, For bug ALPS00331731, set conversation cache . @{
        if (!mIsScrolling) {
            Conversation.setNeedCacheConv(false);
            Conversation conv = Conversation.from(context, cursor);
            Conversation.setNeedCacheConv(true);
            if (mSubjectSingleLine) {
                headerView.setSubjectSingleLineMode(true);
            }
            headerView.bind(context, conv);
        } else {
            headerView.bindDefault();
        }
        /// @}
    }

    public void onMovedToScrapHeap(View view) {
        ConversationListItem headerView = (ConversationListItem)view;
        headerView.unbind();
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        if (LOCAL_LOGV) Log.v(TAG, "inflating new view");
        return mFactory.inflate(R.layout.conversation_list_item, parent, false);
    }

    public interface OnContentChangedListener {
        void onContentChanged(ConversationListAdapter adapter);
    }

    public void setOnContentChangedListener(OnContentChangedListener l) {
        mOnContentChangedListener = l;
    }

    @Override
    protected void onContentChanged() {
        if (mCursor != null && !mCursor.isClosed()) {
            if (mOnContentChangedListener != null) {
                mOnContentChangedListener.onContentChanged(this);
            }
        }
    }

    public void uncheckAll() {
        int count = getCount();
        for (int i = 0; i < count; i++) {
            Cursor cursor = (Cursor)getItem(i);
            Conversation conv = Conversation.from(mContext, cursor);
            conv.setIsChecked(false);
        }
    }
    /// M: Code analyze 025, For new feature ALPS00114403, to solve ANR
    /// happen when multi delete 6 threads . @{
    public void uncheckSelect(HashSet<Integer> idSet) {
        if (idSet != null && idSet.size() > 0) {
            Iterator iterator = idSet.iterator(); 
            while (iterator.hasNext()) {
                int index = (Integer) iterator.next();
                Log.d(TAG, "uncheckSelect index " + index);
                Object obj = getItem(index);
                if (obj != null) {
                    Cursor cursor = (Cursor) obj;
                    Conversation conv = Conversation.getFromCursor(mContext, cursor);
                    conv.setIsChecked(false);
                } else {
                    MmsLog.e(TAG, "getItem in uncheckSelect index " + index + "is null");
                }
            }
        }
    }
    /// @}

    /// M: Code analyze 026, personal use, caculate time . @{
     @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        MmsLog.i(TAG, "[Performance test][Mms] loading data end time ["
            + System.currentTimeMillis() + "]");
    }
     /// @}

    /// M: Code analyze 007, For bug ALPS00242955, If adapter data is valid . @{
    public boolean isDataValid() {
        return mDataValid;
    }
    /// @}

    /// M:
    public void setSubjectSingleLineMode(boolean value) {
        mSubjectSingleLine = value;
    }

    /// M: redesign selection action bar and add shortcut in common version. @{
    public boolean isAllSelected() {
        int count = getCount();
        boolean isAllChecked = true;
        for (int i = 0; i < count; i++) {
            Cursor cursor = (Cursor)getItem(i);
            Conversation conv = Conversation.getFromCursor(mContext, cursor);
            if (!conv.isChecked()) {
                isAllChecked = false;
                break;
            }
        }
        return isAllChecked;
    }
    /// @}
    /// M: For ConversationList to check listener @{
    public OnContentChangedListener getOnContentChangedListener() {
        return mOnContentChangedListener;
    }
    /// @}
}
