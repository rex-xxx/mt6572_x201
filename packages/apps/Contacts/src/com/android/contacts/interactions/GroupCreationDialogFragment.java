/*
 * Copyright (C) 2010 The Android Open Source Project
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
package com.android.contacts.interactions;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;

import com.android.contacts.ContactSaveService;
import com.android.contacts.R;
import com.android.contacts.model.account.AccountWithDataSet;

// The following lines are provided and maintained by Mediatek Inc.
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract.Groups;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
// The previous lines are provided and maintained by Mediatek Inc.

/**
 * A dialog for creating a new group.
 */
public class GroupCreationDialogFragment extends GroupNameDialogFragment {
    private static final String ARG_ACCOUNT_TYPE = "accountType";
    private static final String ARG_ACCOUNT_NAME = "accountName";
    private static final String ARG_DATA_SET = "dataSet";

    public static final String FRAGMENT_TAG = "createGroupDialog";

    private final OnGroupCreatedListener mListener;

    public interface OnGroupCreatedListener {
        public void onGroupCreated();
    }

    public static void show(
            FragmentManager fragmentManager, String accountType, String accountName,
            String dataSet, OnGroupCreatedListener listener) {
        GroupCreationDialogFragment dialog = new GroupCreationDialogFragment(listener);
        Bundle args = new Bundle();
        args.putString(ARG_ACCOUNT_TYPE, accountType);
        args.putString(ARG_ACCOUNT_NAME, accountName);
        args.putString(ARG_DATA_SET, dataSet);
        dialog.setArguments(args);
        dialog.show(fragmentManager, FRAGMENT_TAG);
    }

    public GroupCreationDialogFragment() {
        super();
        mListener = null;
    }

    private GroupCreationDialogFragment(OnGroupCreatedListener listener) {
        super();
        mListener = listener;
    }

    public OnGroupCreatedListener getOnGroupCreatedListener() {
        return mListener;
    }

    @Override
    protected void initializeGroupLabelEditText(EditText editText) {
    }

    @Override
    protected int getTitleResourceId() {
        return R.string.create_group_dialog_title;
    }

    @Override
    protected void onCompleted(String groupLabel) {
        Bundle arguments = getArguments();
        String accountType = arguments.getString(ARG_ACCOUNT_TYPE);
        String accountName = arguments.getString(ARG_ACCOUNT_NAME);
        String dataSet = arguments.getString(ARG_DATA_SET);

        // Indicate to the listener that a new group will be created.
        // If the device is rotated, mListener will become null, so that the
        // popup from GroupMembershipView will not be shown.
        if (mListener != null) {
            mListener.onGroupCreated();
        }

        /*
         * Change feature by Mediatek Begin
         * Original Android code:
         *
         * CR ID :ALPS000118978
         * Descriptions: 
         */
        if(!checkName(groupLabel, accountType, accountName)){
            return; 
        }
        
        /*
         * Change feature by Mediatek End
         */
        Activity activity = getActivity();
        activity.startService(ContactSaveService.createNewGroupIntent(activity,
                new AccountWithDataSet(accountName, accountType, dataSet), groupLabel,
                null /* no new members to add */,
                activity.getClass(), Intent.ACTION_EDIT));
    }
    
    // The following lines are provided and maintained by Mediatek Inc.
    private static String TAG = "GroupNameDialogFragment";
    private Context mContext; 
    public boolean checkName(CharSequence name, String accountType, String accountName) {
        mContext = this.getActivity();
        Log.i(TAG, "checkName begiin"+name);
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(mContext, R.string.name_needed, Toast.LENGTH_SHORT).show();
            return false;
        }
        if(name.toString().contains("/") || name.toString().contains("%"))
        {
             Toast.makeText(mContext, R.string.save_group_fail, Toast.LENGTH_SHORT).show();
             return false;
        }
        boolean nameExists = false;
        //check group name in DB
        Log.i(TAG, accountName+"--accountName");
        Log.i(TAG, accountType+"--accountType");
        if (!nameExists) {
            Cursor cursor = mContext.getContentResolver().query(
                    Groups.CONTENT_SUMMARY_URI,
                    new String[] { Groups._ID },
                    Groups.TITLE + "=? AND " + Groups.ACCOUNT_NAME + " =? AND " +
                    Groups.ACCOUNT_TYPE + "=? AND " + Groups.DELETED + "=0",
                    new String[] { name.toString(), accountName, accountType}, null);     
            Log.i(TAG, cursor.getCount()+"--cursor.getCount()");
            if (cursor == null || cursor.getCount() == 0) {
                if (cursor != null) {
                    cursor.close();
                }
            } else {
                cursor.close();
                nameExists = true;
            }
        }
        //If group name exists, make a toast and return false.
        if (nameExists) {
            Toast.makeText(mContext,
                    R.string.group_name_exists, Toast.LENGTH_SHORT).show();
            return false;
        } else {
            return true;
        }
    }
    
    // The previous  lines are provided and maintained by Mediatek Inc.
}
