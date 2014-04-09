/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.phone;

import static android.view.Window.PROGRESS_VISIBILITY_OFF;
import static android.view.Window.PROGRESS_VISIBILITY_ON;

import android.app.ListActivity;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.provider.Telephony.SIMInfo;
import android.util.Log;
import android.view.Window;
import android.widget.CursorAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.mediatek.settings.CallSettings;

/**
 * ADN List activity for the Phone app.
 */
public class ADNList extends ListActivity {
    protected static final String TAG = "ADNList";
    protected static final boolean DBG = false;

    private static final int[] VIEW_NAMES = new int[] {
        android.R.id.text1,
        android.R.id.text2
    };

    protected static final int QUERY_TOKEN = 0;
    protected static final int INSERT_TOKEN = 1;
    protected static final int UPDATE_TOKEN = 2;
    protected static final int DELETE_TOKEN = 3;


    protected QueryHandler mQueryHandler;
    protected CursorAdapter mCursorAdapter;
    protected Cursor mCursor = null;

    private TextView mEmptyText;

    protected int mInitialSelection = -1;
    /// M: add broadcast receiver & some columns @{
    protected boolean mAirplaneMode = false;    // used for SimContacts only for now. mtk80909, 2010-10-28

    private static final String[] COLUMN_NAMES = new String[] {
        "index",
        "name",
        "number",
        "emails",
        "additionalNumber",            
        "groupIds"
    };

    protected static final int INDEX_COLUMN = 0;
    protected static final int NAME_COLUMN = 1;
    protected static final int NUMBER_COLUMN = 2;
    protected static final int EMAIL_COLUMN = 3;
    protected static final int ADDITIONAL_NUMBER_COLUMN = 4;
    protected static final int GROUP_COLUMN = 5;

    protected int mSimId;
    protected int mIndicate;

    private final BroadcastReceiver mReceiver = new ADNListBroadcastReceiver();
    /// @}
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        getWindow().requestFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.adn_list);
        mEmptyText = (TextView) findViewById(android.R.id.empty);
        mQueryHandler = new QueryHandler(getContentResolver());

        /// M: add broadcast receiver & some columns @{
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        registerReceiver(mReceiver, intentFilter);
        /// @}
    }

    @Override
    protected void onResume() {
        super.onResume();
        query();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mCursor != null) {
            mCursor.deactivate();
        }
    }

    protected Uri resolveIntent() {
        Intent intent = getIntent();
        if (intent.getData() == null) {
            intent.setData(Uri.parse("content://icc/adn"));
        }

        return intent.getData();
    }

    private void query() {
        Uri uri = resolveIntent();
        if (DBG) {
            log("query: starting an async query");
        }
        mQueryHandler.startQuery(QUERY_TOKEN, null, uri, COLUMN_NAMES,
                null, null, null);
        displayProgress(true);
    }

    private void reQuery() {
        query();
    }

    private void setAdapter() {
        // NOTE:
        // As it it written, the positioning code below is NOT working.
        // However, this current non-working state is in compliance with
        // the UI paradigm, so we can't really do much to change it.

        // In the future, if we wish to get this "positioning" correct,
        // we'll need to do the following:
        //   1. Change the layout to in the cursor adapter to:
        //     android.R.layout.simple_list_item_checked
        //   2. replace the selection / focus code with:
        //     getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        //     getListView().setItemChecked(mInitialSelection, true);

        // Since the positioning is really only useful for the dialer's
        // SpecialCharSequence case (dialing '2#' to get to the 2nd
        // contact for instance), it doesn't make sense to mess with
        // the usability of the activity just for this case.

        // These artifacts include:
        //  1. UI artifacts (checkbox and highlight at the same time)
        //  2. Allowing the user to edit / create new SIM contacts when
        //    the user is simply trying to retrieve a number into the d
        //    dialer.

        if (mCursorAdapter == null) {
            mCursorAdapter = newAdapter();

            setListAdapter(mCursorAdapter);
        } else {
            mCursorAdapter.changeCursor(mCursor);
        }

        if (mInitialSelection >= 0 && mInitialSelection < mCursorAdapter.getCount()) {
            setSelection(mInitialSelection);
            getListView().setFocusableInTouchMode(true);
            boolean gotfocus = getListView().requestFocus();
        }
    }

    protected CursorAdapter newAdapter() {
        return new SimpleCursorAdapter(this,
                    R.layout.adn_list_item,
                    mCursor, new String[]{COLUMN_NAMES[1], COLUMN_NAMES[2]}, VIEW_NAMES);
    }

    private void displayProgress(boolean loading) {
        if (DBG) {
            log("displayProgress: " + loading);
        }
        /// M: support for gemini phone
        if (CallSettings.isMultipleSim()) {
            SIMInfo info = SIMInfo.getSIMInfoBySlot(this, mSimId);
            if (info != null && loading) {
                String text = getResources().getString(R.string.simContacts_emptyLoading_ex, info.mDisplayName);
                mEmptyText.setText(text);
            } else {
                mEmptyText.setText(R.string.simContacts_empty);
            }
        } else {
            mEmptyText.setText(loading ? R.string.simContacts_emptyLoading :
                (isAirplaneModeOn(this) ? R.string.simContacts_airplaneMode :
                R.string.simContacts_empty));
        }
        getWindow().setFeatureInt(
                Window.FEATURE_INDETERMINATE_PROGRESS,
                loading ? PROGRESS_VISIBILITY_ON : PROGRESS_VISIBILITY_OFF);
    }

    private static boolean isAirplaneModeOn(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.AIRPLANE_MODE_ON, 0) != 0;
    }

    private class QueryHandler extends AsyncQueryHandler {
        public QueryHandler(ContentResolver cr) {
            super(cr);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor c) {
            if (DBG) {
                log("onQueryComplete: cursor.count=" + c.getCount());
            }
            mCursor = c;
            if (mAirplaneMode) {
                mCursor = null;
            }
            setAdapter();
            displayProgress(false);

            // Cursor is refreshed and inherited classes may have menu items depending on it.
            invalidateOptionsMenu();
        }

        @Override
        protected void onInsertComplete(int token, Object cookie, Uri uri) {
            if (DBG) {
                log("onInsertComplete: requery");
            }
            reQuery();
        }

        @Override
        protected void onUpdateComplete(int token, Object cookie, int result) {
            if (DBG) {
                log("onUpdateComplete: requery");
            }
            reQuery();
        }

        @Override
        protected void onDeleteComplete(int token, Object cookie, int result) {
            if (DBG) {
                log("onDeleteComplete: requery");
            }
            reQuery();
        }
    }

    protected void log(String msg) {
        Log.d(TAG, "[ADNList] " + msg);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    private class ADNListBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
                finish();
            }
        }
    }
}
