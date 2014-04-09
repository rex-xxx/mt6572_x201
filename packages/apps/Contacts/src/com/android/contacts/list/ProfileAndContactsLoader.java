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
package com.android.contacts.list;

import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.Profile;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;

import com.google.android.collect.Lists;

import java.util.List;

/**
 * A loader for use in the default contact list, which will also query for the user's profile
 * if configured to do so.
 */
public class ProfileAndContactsLoader extends CursorLoader {

    private boolean mLoadProfile;
    private String[] mProjection;

    public ProfileAndContactsLoader(Context context) {
        super(context);
    }

    public void setLoadProfile(boolean flag) {
        mLoadProfile = flag;
    }

    public void setProjection(String[] projection) {
        super.setProjection(projection);
        mProjection = projection;
    }

    @Override
    public Cursor loadInBackground() {
        // First load the profile, if enabled.
        List<Cursor> cursors = Lists.newArrayList();
        if (mLoadProfile) {
            cursors.add(loadProfile());
        }

        /** M: New Feature SDN @{ */
        mSdnContactCount = 0;
        String oldSelection = getSelection();
        Cursor sdnCursor = loadSDN(this);
        if (null != sdnCursor){
            cursors.add(sdnCursor);
        }

        setSelection(oldSelection);
        /** @} */

        final Cursor contactsCursor = super.loadInBackground();
        cursors.add(contactsCursor);
        return new MergeCursor(cursors.toArray(new Cursor[cursors.size()])) {
            @Override
            public Bundle getExtras() {
                // Need to get the extras from the contacts cursor.
                return contactsCursor.getExtras();
            }
        };
    }

    /**
     * Loads the profile into a MatrixCursor.
     */
    private MatrixCursor loadProfile() {
        Cursor cursor = getContext().getContentResolver().query(Profile.CONTENT_URI, mProjection,
                null, null, null);
        try {
            MatrixCursor matrix = new MatrixCursor(mProjection);
            Object[] row = new Object[mProjection.length];
            while (cursor.moveToNext()) {
                for (int i = 0; i < row.length; i++) {
                    row[i] = cursor.getString(i);
                }
                matrix.addRow(row);
            }
            return matrix;
        } finally {
            cursor.close();
        }
    }

    /** M: The following lines are provided and maintained by Mediatek Inc. @{ */
    private static final String TAG = "ProfileAndContactsLoader";
    private int mSdnContactCount = 0;

    public boolean hasSdnContact() {
        return this.mSdnContactCount > 0;
    }

    public int getSdnContactCount() {
        return this.mSdnContactCount;
    }

    @Override
    protected void onStartLoading() {
        forceLoad();
    }

    private Cursor loadSDN(ProfileAndContactsLoader profileAndContactsLoader) {
        Cursor sdnCursor = null;
        if (null != profileAndContactsLoader.getSelection()
                && profileAndContactsLoader.getSelection().indexOf(
                        RawContacts.IS_SDN_CONTACT + " = 0") >= 0) {
            Uri uri = profileAndContactsLoader.getUri();
            String[] projection = profileAndContactsLoader.getProjection();
            String newSelection = profileAndContactsLoader.getSelection().replace(
                    RawContacts.IS_SDN_CONTACT + " = 0", RawContacts.IS_SDN_CONTACT + " = 1");
            String[] selectionArgs = profileAndContactsLoader.getSelectionArgs();
            String sortOrder = profileAndContactsLoader.getSortOrder();
            sdnCursor = getContext().getContentResolver().query(uri, projection, newSelection,
                    selectionArgs, sortOrder);
            mSdnContactCount = sdnCursor == null ? 0 : sdnCursor.getCount();
            MatrixCursor matrix = new MatrixCursor(projection);
            try {
                Object[] row = new Object[projection.length];
                while (sdnCursor.moveToNext()) {
                    for (int i = 0; i < row.length; i++) {
                        row[i] = sdnCursor.getString(i);
                    }
                    matrix.addRow(row);
                }
                Log.i(TAG, "loadSDN sdnCursor : " + sdnCursor);
                return matrix;
            } finally {
                if (null != sdnCursor) {
                    sdnCursor.close();
                }
            }
        }
        Log.i(TAG,"loadSDN return null");
        return null;
    }
    /** @} */
    /** M: The previous lines are provided and maintained by Mediatek Inc. @} */
}
