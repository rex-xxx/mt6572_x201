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
 * limitations under the License
 */

package com.android.contacts.activities;

import android.app.ActionBar;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

/** M: New Feature xxx @{ */
import com.android.contacts.ContactSaveService;
/** @} */
import com.android.contacts.ContactsActivity;
import com.android.contacts.R;
/** M: New Feature xxx @{ */
import com.android.contacts.activities.PeopleActivity.AccountCategoryInfo;
import com.android.contacts.editor.ContactEditorFragment;
import com.android.contacts.editor.ContactEditorFragment.SaveMode;
/** @} */
import com.android.contacts.group.GroupEditorFragment;
import com.android.contacts.util.DialogManager;
import com.android.contacts.util.PhoneCapabilityTester;
import com.android.contacts.vcard.VCardService;
import com.mediatek.contacts.list.ContactsGroupMultiPickerFragment;
import com.mediatek.contacts.list.service.MultiChoiceService;

public class GroupEditorActivity extends ContactsActivity
        implements DialogManager.DialogShowingViewActivity {

    private static final String TAG = "GroupEditorActivity";

    public static final String ACTION_SAVE_COMPLETED = "saveCompleted";
    public static final String ACTION_ADD_MEMBER_COMPLETED = "addMemberCompleted";
    public static final String ACTION_REMOVE_MEMBER_COMPLETED = "removeMemberCompleted";

    private static final int SUBACTIVITY_DETAIL_GROUP = 1;
    private GroupEditorFragment mFragment;

    private DialogManager mDialogManager = new DialogManager(this);

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
       
        /*
         * New feature by Mediatek Begin
         * Original Android code:
         *  String action = getIntent().getAction();
         */
        Intent intent = getIntent();
        String action = intent.getAction();
        mSlotId = -1;
        int simId = -1;
        mIntentExtras = intent.getExtras();
        Log.i(TAG, " mIntentExtras : " + mIntentExtras);
        final AccountCategoryInfo accountCategoryInfo = mIntentExtras == null ? null
                : (AccountCategoryInfo) mIntentExtras.getParcelable("AccountCategory");
        if (accountCategoryInfo != null) {
            Log.i(TAG, "onCrete " + accountCategoryInfo);
            mSlotId = accountCategoryInfo.mSlotId;
            simId = accountCategoryInfo.mSimId;
        } else {
            mSlotId = intent.getIntExtra("SLOT_ID", mSlotId);
            simId = intent.getIntExtra("SIM_ID", simId);
        }
        Log.i(TAG, mSlotId + "-------mSlotId[oncreate]");
        Log.i(TAG, simId + "-------simId[oncreate]");
        /*
         * New feature by Mediatek End
         */
 
        if (ACTION_SAVE_COMPLETED.equals(action)) {
            finish();
            return;
        }
        
        /**
         * M: fixed CR ALPS00542175 @ {
         */
        if (MultiChoiceService.isProcessing(MultiChoiceService.TYPE_DELETE)
                || MultiChoiceService.isProcessing(MultiChoiceService.TYPE_COPY)
                || VCardService.isProcessing(VCardService.TYPE_IMPORT)
                || ContactsGroupMultiPickerFragment.isMoveContactsInProcessing() /// fixed cr ALPS00567939
                || ContactSaveService.isGroupTransactionProcessing()) {
            Log.i(TAG, "delete or copy is processing ");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), R.string.phone_book_busy, Toast.LENGTH_SHORT).show();
                }
            });
            finish();
            return;
        }
        /**
         * @}
         */

        setContentView(R.layout.group_editor_activity);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            // Inflate a custom action bar that contains the "done" button for saving changes
            // to the group
            /** M: Change Feature @{ */
            boolean isTablet = PhoneCapabilityTester.isUsingTwoPanes(this);
            /** @} */
            LayoutInflater inflater = (LayoutInflater) getSystemService
                    (Context.LAYOUT_INFLATER_SERVICE);
            /** M: Change Feature @{ */
            View customActionBarView = null;
            if (isTablet) {
                customActionBarView = inflater.inflate(R.layout.editor_custom_action_bar, null);
            } else {
                customActionBarView = inflater.inflate(R.layout.editor_custom_action_bar_ext, null);
            }
            /** @} */
            View saveMenuItem = customActionBarView.findViewById(R.id.save_menu_item);
            saveMenuItem.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mFragment.onDoneClicked();
                }
            });
            /** M: Change Feature @{ */
            if (!isTablet) {
                View cancelMenuItem = customActionBarView.findViewById(R.id.cancel_menu_item);
                cancelMenuItem.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mFragment.doDiscard();
                    }
                });
            }
            /** @} */
            // Show the custom action bar but hide the home icon and title
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                    ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME |
                    ActionBar.DISPLAY_SHOW_TITLE);
            actionBar.setCustomView(customActionBarView);
        }

        mFragment = (GroupEditorFragment) getFragmentManager().findFragmentById(
                R.id.group_editor_fragment);
        mFragment.setListener(mFragmentListener);
        mFragment.setContentResolver(getContentResolver());

        // NOTE The fragment will restore its state by itself after orientation changes, so
        // we need to do this only for a new instance.
        if (savedState == null) {
            Uri uri = Intent.ACTION_EDIT.equals(action) ? getIntent().getData() : null;
            /*
             * New feature by Mediatek Begin
             * Original Android code:
             *  mFragment.load(action, uri, getIntent().getExtras());
             */
            Log.i(TAG, " savedState == null mSlotId : " + mSlotId);
            mFragment.load(action, uri, getIntent().getExtras(), mSlotId, simId);
            /*
             * New feature by Mediatek End
             */

        }
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        if (DialogManager.isManagedId(id)) {
            return mDialogManager.onCreateDialog(id, args);
        } else {
            // Nobody knows about the Dialog
            Log.w(TAG, "Unknown dialog requested, id: " + id + ", args: " + args);
            return null;
        }
    }

    @Override
    public void onBackPressed() {
        // If the change could not be saved, then revert to the default "back" button behavior.

        /*
         * New feature by Mediatek Begin
         * Original Android code:
         * if (!mFragment.save(SaveMode.CLOSE)) {
            super.onBackPressed();
           }
         * CR ID :ALPS00228918
         * Descriptions: 
         */
        if (!mFragment.save(SaveMode.CLOSE, false)) {
            if (!mFragment.checkOnBackPressedState()) {
                super.onBackPressed();
            }
        }
        /*
         * New feature by Mediatek End
         */
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (mFragment == null) {
            return;
        }
        // The following lines are provided and maintained by Mediatek Inc.
        mSlotId = intent.getIntExtra(ContactSaveService.EXTRA_SLOT_ID, -1);
        Log.i(TAG, mSlotId + "----mSlotId");
        // The previous  lines are provided and maintained by Mediatek Inc.
        String action = intent.getAction();
        boolean showToast = intent.getBooleanExtra(
                GroupEditorFragment.SHOW_TOAST_EXTRA_KEY, true);
        int saveMode = intent.getIntExtra(
                ContactEditorFragment.SAVE_MODE_EXTRA_KEY, SaveMode.CLOSE);
        Log.i(TAG, action + "----action");
        if (ACTION_SAVE_COMPLETED.equals(action)) {
            mFragment.onSaveCompleted(true, intent.getData());
            if (showToast && saveMode != SaveMode.RELOAD) {
                Toast.makeText(
                        getApplicationContext(),
                        intent.getData() != null ? R.string.groupSavedToast : R.string.groupSavedErrorToast,
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private final GroupEditorFragment.Listener mFragmentListener =
            new GroupEditorFragment.Listener() {
        @Override
        public void onGroupNotFound() {
            finish();
        }

        @Override
        public void onReverted() {
            finish();
        }

        @Override
        public void onAccountsNotFound() {
            finish();
        }

        @Override
        public void onSaveFinished(int resultCode, Intent resultIntent) {
            // TODO: Collapse these 2 cases into 1 that will just launch an intent with the VIEW
            // action to see the group URI (when group URIs are supported)
            // For a 2-pane screen, set the activity result, so the original activity (that launched
            // the editor) can display the group detail page
            if (PhoneCapabilityTester.isUsingTwoPanes(GroupEditorActivity.this)) {
                setResult(resultCode, resultIntent);
            } else if (resultIntent != null) {
                // For a 1-pane screen, launch the group detail page
                /*
                 * Bug Fix by Mediatek Begin
                 * Original Android's code:
                 * 
                Intent intent = new Intent(GroupEditorActivity.this, GroupDetailActivity.class);
                intent.setData(resultIntent.getData());
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                 * CR ID :ALPS000113859
                 * Descriptions:add the arg slotId 
                 */
                Intent intent = new Intent(GroupEditorActivity.this, GroupDetailActivity.class);
                intent.setData(resultIntent.getData());
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra("mSlotId", mSlotId);
                intent.putExtra("callBackIntent", "callBackIntent");
                startActivityForResult(intent, SUBACTIVITY_DETAIL_GROUP);
                /*
                 * Bug Fix by Mediatek End
                 */
            }
            finish();
        }
    };

    @Override
    public DialogManager getDialogManager() {
        return mDialogManager;
    }

    /** M: New Feature xxx @{ */
    private int mSlotId;
    private Bundle mIntentExtras;
    /** @} */
}
