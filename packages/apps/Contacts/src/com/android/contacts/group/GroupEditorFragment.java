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

package com.android.contacts.group;

import android.accounts.Account;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Intents;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.RawContactsEntity;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.QuickContactBadge;
import android.widget.TextView;
import android.widget.Toast;

import com.android.contacts.ContactPhotoManager;
import com.android.contacts.ContactSaveService;
import com.android.contacts.GroupMemberLoader;
import com.android.contacts.GroupMemberLoader.GroupEditorQuery;
import com.android.contacts.GroupMetaDataLoader;
import com.android.contacts.R;
import com.android.contacts.activities.GroupEditorActivity;
import com.android.contacts.editor.SelectAccountDialogFragment;
import com.android.contacts.group.SuggestedMemberListAdapter.SuggestedMember;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.model.account.AccountType;
import com.android.contacts.model.account.AccountWithDataSet;
import com.android.contacts.util.AccountsListAdapter.AccountListFilter;
import com.android.contacts.util.PhoneCapabilityTester;
import com.android.contacts.util.ViewUtil;
import com.android.internal.util.Objects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

// The following lines are provided and maintained by Mediatek Inc.
import com.android.contacts.activities.PeopleActivity.AccountCategoryInfo;
import com.mediatek.contacts.ContactsFeatureConstants.FeatureOption;
import com.mediatek.contacts.list.ContactListMultiChoiceActivity;
import com.mediatek.contacts.model.AccountWithDataSetEx;
import com.mediatek.contacts.simcontact.AbstractStartSIMService;
import com.mediatek.contacts.simcontact.SimCardUtils;
import com.mediatek.contacts.simcontact.SlotUtils;
import com.mediatek.contacts.util.ContactsGroupUtils;
import com.mediatek.contacts.util.SimContactPhotoUtils;
import com.mediatek.phone.SIMInfoWrapper;

import android.provider.Telephony.SIMInfo;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.Contacts.Data;
import android.content.DialogInterface.OnCancelListener;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.app.ProgressDialog;
import com.android.internal.telephony.gemini.GeminiPhone;
import com.android.internal.telephony.TelephonyIntents;
import com.android.contacts.editor.ContactEditorFragment.SaveMode;
// The previous lines are provided and maintained by Mediatek Inc.

public class GroupEditorFragment extends Fragment implements SelectAccountDialogFragment.Listener {
    private static final String TAG = "GroupEditorFragment";

    private static final String LEGACY_CONTACTS_AUTHORITY = "contacts";

    private static final String KEY_ACTION = "action";
    private static final String KEY_GROUP_URI = "groupUri";
    private static final String KEY_GROUP_ID = "groupId";
    private static final String KEY_STATUS = "status";
    private static final String KEY_ACCOUNT_NAME = "accountName";
    private static final String KEY_ACCOUNT_TYPE = "accountType";
    private static final String KEY_DATA_SET = "dataSet";
    private static final String KEY_GROUP_NAME_IS_READ_ONLY = "groupNameIsReadOnly";
    private static final String KEY_ORIGINAL_GROUP_NAME = "originalGroupName";
    private static final String KEY_MEMBERS_TO_ADD = "membersToAdd";
    private static final String KEY_MEMBERS_TO_REMOVE = "membersToRemove";
    private static final String KEY_MEMBERS_TO_DISPLAY = "membersToDisplay";

    private static final String RESULTINTENTEXTRANAME = "com.mediatek.contacts.list.pickcontactsresult";
    
    // The following lines are provided and maintained by Mediatek Inc.
    private static final String KEY_GROUP_SLOT_ID = "groupSlotId"; 
    private static final String KEY_CATEGORY =  "mCategory";
    // The previous lines are provided and maintained by Mediatek Inc.
    
    private static final String CURRENT_EDITOR_TAG = "currentEditorForAccount";

    public static interface Listener {
        /**
         * Group metadata was not found, close the fragment now.
         */
        public void onGroupNotFound();

        /**
         * User has tapped Revert, close the fragment now.
         */
        void onReverted();

        /**
         * Contact was saved and the Fragment can now be closed safely.
         */
        void onSaveFinished(int resultCode, Intent resultIntent);

        /**
         * Fragment is created but there's no accounts set up.
         */
        void onAccountsNotFound();
    }

    private static final int LOADER_GROUP_METADATA = 1;
    private static final int LOADER_EXISTING_MEMBERS = 2;
    private static final int LOADER_NEW_GROUP_MEMBER = 3;
    private static final int MULTIPLE_ADD_GROUP_MEMBER = 24;

    // The following lines are provided and maintained by Mediatek Inc.
    public static final String SHOW_TOAST_EXTRA_KEY = "showToast";
    // The previous lines are provided and maintained by Mediatek Inc.

    private static final String MEMBER_RAW_CONTACT_ID_KEY = "rawContactId";
    private static final String MEMBER_LOOKUP_URI_KEY = "memberLookupUri";
    private static final String MEMBER_RAW_CONTACT_ID_KEYS = "rawContactIds";
    private static final String MEMBER_LOOKUP_URI_KEYS = "memberLookupUris";

    protected static final String[] PROJECTION_CONTACT = new String[] {
        Contacts._ID,                           // 0
        Contacts.DISPLAY_NAME_PRIMARY,          // 1
        Contacts.DISPLAY_NAME_ALTERNATIVE,      // 2
        Contacts.SORT_KEY_PRIMARY,              // 3
        Contacts.STARRED,                       // 4
        Contacts.CONTACT_PRESENCE,              // 5
        Contacts.CONTACT_CHAT_CAPABILITY,       // 6
        Contacts.PHOTO_ID,                      // 7
        Contacts.PHOTO_THUMBNAIL_URI,           // 8
        Contacts.LOOKUP_KEY,                    // 9
        Contacts.PHONETIC_NAME,                 // 10
        Contacts.HAS_PHONE_NUMBER,              // 11
        Contacts.IS_USER_PROFILE,               // 12
        
        // the following lines are provided and maintained by Mediatek Inc.
        Contacts.INDICATE_PHONE_SIM,            // 13 
        Contacts.INDEX_IN_SIM, // 14
        Contacts.IS_SDN_CONTACT,                // 15
        // the previous lines are provided and maintained by Mediatek Inc.
    };

    protected static final int CONTACT_ID_COLUMN_INDEX = 0;
    protected static final int CONTACT_DISPLAY_NAME_PRIMARY_COLUMN_INDEX = 1;
    protected static final int CONTACT_DISPLAY_NAME_ALTERNATIVE_COLUMN_INDEX = 2;
    protected static final int CONTACT_SORT_KEY_PRIMARY_COLUMN_INDEX = 3;
    protected static final int CONTACT_STARRED_COLUMN_INDEX = 4;
    protected static final int CONTACT_PRESENCE_STATUS_COLUMN_INDEX = 5;
    protected static final int CONTACT_CHAT_CAPABILITY_COLUMN_INDEX = 6;
    protected static final int CONTACT_PHOTO_ID_COLUMN_INDEX = 7;
    protected static final int CONTACT_PHOTO_URI_COLUMN_INDEX = 8;
    protected static final int CONTACT_LOOKUP_KEY_COLUMN_INDEX = 9;
    protected static final int CONTACT_PHONETIC_NAME_COLUMN_INDEX = 10;
    protected static final int CONTACT_HAS_PHONE_COLUMN_INDEX = 11;
    protected static final int CONTACT_IS_USER_PROFILE = 12;

    // the following lines are provided and maintained by Mediatek Inc.
    protected static final int CONTACT_INDICATE_PHONE_SIM_COLUMN_INDEX = 13;
    protected static final int CONTACT_INDEX_IN_SIM_COLUMN_INDEX = 14;
    protected static final int CONTACT_IS_SDN_CONTACT_COLUMN_INDEX = 15;

    private static final String[] PROJECTION_MEMBER_DATA = new String[] {
        RawContacts.CONTACT_ID,                 // 0
        Data.MIMETYPE,                          // 1
        Data.DATA1,                             // 2
        Photo.PHOTO,                            // 3
        RawContacts._ID,                        // 4
    };
    private static final int RAWCONTACT_CONTACT_ID = 0;
    private static final int MIMETYPE_COLUMN_INDEX = 1;
    private static final int DATA_COLUMN_INDEX = 2;
    private static final int PHOTO_COLUMN_INDEX = 3;
    private static final int RAW_CONTACT_ID_COLUMN_INDEX = 4;
    // the previous lines are provided and maintained by Mediatek Inc.

    /**
     * Modes that specify the status of the editor
     */
    public enum Status {
        SELECTING_ACCOUNT, // Account select dialog is showing
        LOADING,    // Loader is fetching the group metadata
        EDITING,    // Not currently busy. We are waiting forthe user to enter data.
        SAVING,     // Data is currently being saved
        CLOSING     // Prevents any more saves
    }

    private Context mContext;
    private String mAction;
    private Bundle mIntentExtras;
    private Uri mGroupUri;
    private long mGroupId;
    private Listener mListener;

    private Status mStatus;

    private ViewGroup mRootView;
    private ListView mListView;
    private LayoutInflater mLayoutInflater;

    private TextView mGroupNameView;
    private AutoCompleteTextView mAutoCompleteTextView;
    private ImageButton mMemberPicker;

    private String mAccountName;
    private String mAccountType;
    private String mDataSet;

    private boolean mGroupNameIsReadOnly;
    private String mOriginalGroupName = "";
    private int mLastGroupEditorId;

    private MemberListAdapter mMemberListAdapter;
    private ContactPhotoManager mPhotoManager;

    private ContentResolver mContentResolver;
    private SuggestedMemberListAdapter mAutoCompleteAdapter;

    private static ArrayList<Member> mListMembersToAdd = new ArrayList<Member>();
    private static ArrayList<Member> mListMembersToRemove = new ArrayList<Member>();
    private static ArrayList<Member> mListToDisplay = new ArrayList<Member>();

    public GroupEditorFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        setHasOptionsMenu(true);
        mLayoutInflater = inflater;
        mRootView = (ViewGroup) inflater.inflate(R.layout.group_editor_fragment, container, false);
        if (sHandler != null) {
            Log.w(TAG, "[onCreateView] there might be some memory leakage");
        }
        sHandler = new AccountChosenHandler(this);
        return mRootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mContext = activity;
        mPhotoManager = ContactPhotoManager.getInstance(mContext);
        mMemberListAdapter = new MemberListAdapter();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            // Just restore from the saved state.  No loading.
            onRestoreInstanceState(savedInstanceState);
            if (mStatus == Status.SELECTING_ACCOUNT) {
                // Account select dialog is showing.  Don't setup the editor yet.
                Log.d(TAG, "onActivityCreated Status.SELECTING_ACCOUNT");
            } else if (mStatus == Status.LOADING) {
                startGroupMetaDataLoader();
            } else {
                setupEditorForAccount();
            }
        } else if (Intent.ACTION_EDIT.equals(mAction)) {
            clearMembersList();
            startGroupMetaDataLoader();
        } else if (Intent.ACTION_INSERT.equals(mAction)) {
            clearMembersList();
            final Account account = mIntentExtras == null ? null :
                    (Account) mIntentExtras.getParcelable(Intents.Insert.ACCOUNT);
            final String dataSet = mIntentExtras == null ? null :
                    mIntentExtras.getString(Intents.Insert.DATA_SET);

            if (account != null) {
                // Account specified in Intent - no data set can be specified in this manner.
                mAccountName = account.name;
                mAccountType = account.type;
                mDataSet = dataSet;
                setupEditorForAccount();
            } else {
                // No Account specified. Let the user choose from a disambiguation dialog.
                selectAccountAndCreateGroup();
            }
        } else {
            throw new IllegalArgumentException("Unknown Action String " + mAction +
                    ". Only support " + Intent.ACTION_EDIT + " or " + Intent.ACTION_INSERT);
        }
        
        // The following lines are provided and maintained by Mediatek Inc.
        if (mReceiver == null) {
            mReceiver = new SimReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
            filter.addAction(TelephonyIntents.ACTION_PHB_STATE_CHANGED);
            filter.addAction(GeminiPhone.EVENT_PRE_3G_SWITCH);
            filter.addAction(Intent.ACTION_DUAL_SIM_MODE_CHANGED);
            mContext.registerReceiver(mReceiver,filter);
            Log.i(TAG, "registerReceiver mReceiver");
        }
        mTask = MemberTask.getExistTask();
        // The previous lines are provided and maintained by Mediatek Inc.
    }

    private void startGroupMetaDataLoader() {
        mStatus = Status.LOADING;
        getLoaderManager().initLoader(LOADER_GROUP_METADATA, null,
                mGroupMetaDataLoaderListener);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_ACTION, mAction);
        outState.putParcelable(KEY_GROUP_URI, mGroupUri);
        outState.putLong(KEY_GROUP_ID, mGroupId);

        outState.putSerializable(KEY_STATUS, mStatus);
        outState.putString(KEY_ACCOUNT_NAME, mAccountName);
        outState.putString(KEY_ACCOUNT_TYPE, mAccountType);
        outState.putString(KEY_DATA_SET, mDataSet);

        outState.putBoolean(KEY_GROUP_NAME_IS_READ_ONLY, mGroupNameIsReadOnly);
        outState.putString(KEY_ORIGINAL_GROUP_NAME, mOriginalGroupName);

        // The following lines are provided and maintained by Mediatek Inc.
        outState.putInt(KEY_GROUP_SLOT_ID, mSlotId);
        outState.putString(KEY_CATEGORY, mCategory);
        outState.putInt("MemberPosition", mMemberPosition);
        // The previous lines are provided and maintained by Mediatek Inc.
    }

    private void onRestoreInstanceState(Bundle state) {
        mAction = state.getString(KEY_ACTION);
        mGroupUri = state.getParcelable(KEY_GROUP_URI);
        mGroupId = state.getLong(KEY_GROUP_ID);

        mStatus = (Status) state.getSerializable(KEY_STATUS);
        mAccountName = state.getString(KEY_ACCOUNT_NAME);
        mAccountType = state.getString(KEY_ACCOUNT_TYPE);
        mDataSet = state.getString(KEY_DATA_SET);

        mGroupNameIsReadOnly = state.getBoolean(KEY_GROUP_NAME_IS_READ_ONLY);
        mOriginalGroupName = state.getString(KEY_ORIGINAL_GROUP_NAME);

        // The following lines are provided and maintained by Mediatek Inc.
        mSlotId = state.getInt(KEY_GROUP_SLOT_ID);
        mCategory = state.getString(KEY_CATEGORY);
        mMemberPosition = state.getInt("MemberPosition");
        // The previous lines are provided and maintained by Mediatek Inc.
    }

    public void setContentResolver(ContentResolver resolver) {
        mContentResolver = resolver;
        if (mAutoCompleteAdapter != null) {
            mAutoCompleteAdapter.setContentResolver(mContentResolver);
        }
    }

    private void selectAccountAndCreateGroup() {
        final List<AccountWithDataSet> accounts =
                AccountTypeManager.getInstance(mContext).getAccounts(true /* writeable */);
        // No Accounts available
        if (accounts.isEmpty()) {
            Log.e(TAG, "No accounts were found.");
            if (mListener != null) {
                mListener.onAccountsNotFound();
            }
            return;
        }

        // In the common case of a single account being writable, auto-select
        // it without showing a dialog.
        if (accounts.size() == 1) {
            mAccountName = accounts.get(0).name;
            mAccountType = accounts.get(0).type;
            mDataSet = accounts.get(0).dataSet;
            setupEditorForAccount();
            return;  // Don't show a dialog.
        }

        mStatus = Status.SELECTING_ACCOUNT;
        SelectAccountDialogFragment.show(getFragmentManager(), this,
                R.string.dialog_new_group_account, AccountListFilter.ACCOUNTS_GROUP_WRITABLE,
                null);
    }

    @Override
    public void onAccountChosen(AccountWithDataSet account, Bundle extraArgs) {
        if (sHandler != null) {
            Message msg = sHandler.obtainMessage(AccountChosenHandler.ACCOUNT_CHOSEN);
            msg.obj = account;
            sHandler.sendMessage(msg);
        } else {
            Log.w(TAG, "[onAccountChosen]the handler is null, the fragment onDestroyView cleared the handler");
        }
    }

    private void onAccountChosenInternal(AccountWithDataSet account) {
        mAccountName = account.name;
        mAccountType = account.type;
        mDataSet = account.dataSet;
        // The following lines are provided and maintained by Mediatek Inc.
        if (account instanceof AccountWithDataSetEx) {
            mSlotId =  ((AccountWithDataSetEx) account).getSlotId();
            //mSimId = SIMInfoWrapper.getDefault().getSimIdBySlotId(mSlotId);
            SIMInfo simInfo = SIMInfo.getSIMInfoBySlot(mContext, mSlotId);
            if (simInfo != null) {
                mSimId = (int) simInfo.mSimId;
            }
            mCategory = account.type;
        }     
        Log.i(TAG, mAccountName + "-------------mAccountName[onAccountChosen]");
        Log.i(TAG, mAccountType + "-------------mAccountType[onAccountChosen]");
        Log.i(TAG, mSlotId + "-------------mSlotId[onAccountChosen]");
        Log.i(TAG, mSimId + "-------------mSimId[onAccountChosen]");
        Log.i(TAG, mCategory + "-------------mCategory[onAccountChosen]");
        // The previous lines are provided and maintained by Mediatek Inc.
        setupEditorForAccount();
    }

    @Override
    public void onAccountSelectorCancelled() {
        if (sHandler != null) {
            sHandler.sendEmptyMessage(AccountChosenHandler.ACCOUNT_CHOOSE_CANCEL);
        } else {
            Log.w(TAG, "[onAccountSelectorCancelled] the handler is null, the fragment onDestroyView cleared the handler");
        }
    }

    private void onAccountSelectorCancelledInternal() {
        if (mListener != null) {
            // Exit the fragment because we cannot continue without selecting an account
            mListener.onGroupNotFound();
        }
    }

    private AccountType getAccountType() {
        return AccountTypeManager.getInstance(mContext).getAccountType(mAccountType, mDataSet);
    }

    /**
     * @return true if the group membership is editable on this account type.  false otherwise,
     *         or account is not set yet.
     */
    private boolean isGroupMembershipEditable() {
        if (mAccountType == null) {
            return false;
        }
        return getAccountType().isGroupMembershipEditable();
    }

    /**
     * Sets up the editor based on the group's account name and type.
     */
    private void setupEditorForAccount() {
        final AccountType accountType = getAccountType();
        final boolean editable = isGroupMembershipEditable();
        boolean isNewEditor = false;
        mMemberListAdapter.setIsGroupMembershipEditable(editable);

        // Since this method can be called multiple time, remove old editor if the editor type
        // is different from the new one and mark the editor with a tag so it can be found for
        // removal if needed
        View editorView;
        int newGroupEditorId =
                editable ? R.layout.group_editor_view : R.layout.external_group_editor_view;
        if (newGroupEditorId != mLastGroupEditorId) {
            View oldEditorView = mRootView.findViewWithTag(CURRENT_EDITOR_TAG);
            if (oldEditorView != null) {
                mRootView.removeView(oldEditorView);
            }
            editorView = mLayoutInflater.inflate(newGroupEditorId, mRootView, false);
            editorView.setTag(CURRENT_EDITOR_TAG);
            mAutoCompleteAdapter = null;
            mLastGroupEditorId = newGroupEditorId;
            isNewEditor = true;
        } else {
            editorView = mRootView.findViewWithTag(CURRENT_EDITOR_TAG);
            if (editorView == null) {
                throw new IllegalStateException("Group editor view not found");
            }
        }

        mGroupNameView = (TextView) editorView.findViewById(R.id.group_name);
        mAutoCompleteTextView = (AutoCompleteTextView) editorView.findViewById(
                R.id.add_member_field);
        // The following lines are provided and maintained by Mediatek Inc.
        mAutoCompleteTextView.setThreshold(1);
        // The previous lines are provided and maintained by Mediatek Inc.
        mListView = (ListView) editorView.findViewById(android.R.id.list);
        mListView.setFocusable(false);
        mListView.setAdapter(mMemberListAdapter);

        // Setup the account header, only when exists.
        if (editorView.findViewById(R.id.account_header) != null) {
            CharSequence accountTypeDisplayLabel = accountType.getDisplayLabel(mContext);
            ImageView accountIcon = (ImageView) editorView.findViewById(R.id.account_icon);
            TextView accountTypeTextView = (TextView) editorView.findViewById(R.id.account_type);
            TextView accountNameTextView = (TextView) editorView.findViewById(R.id.account_name);
            if (!TextUtils.isEmpty(mAccountName)) {
                // The following lines are provided and maintained by Mediatek Inc.
                String accountName = AccountType.getDisplayAccountName(mAccountName);

                // The previous lines are provided and maintained by Mediatek Inc.
                accountNameTextView.setText(
                        // The following lines are provided and maintained by Mediatek Inc.
                        mContext.getString(R.string.from_account_format, accountName));
                        // The previous lines are provided and maintained by Mediatek Inc.
            }
            if (AccountWithDataSetEx.isLocalPhone(accountType.accountType)) {
                accountNameTextView.setVisibility(View.GONE);
            }
            accountTypeTextView.setText(accountTypeDisplayLabel);

            /*
             * New feature by Mediatek Begin Original Android code:
             * accountIcon.setImageDrawable
             * (accountType.getDisplayIcon(mContext)); Descriptions:
             */

            if (mSlotId >= 0) {
                Log.i(TAG, "GroupEditorFragment mSlotId : " + mSlotId);
                accountIcon.setImageDrawable(accountType
                        .getDisplayIconBySlotId(mContext, mSlotId));
            } else {
                accountIcon.setImageDrawable(accountType
                        .getDisplayIcon(mContext));// show the USim icon
            }

            /*
             * New feature by Mediatek End
             */

        }

        // Setup the autocomplete adapter (for contacts to suggest to add to the group) based on the
        // account name and type. For groups that cannot have membership edited, there will be no
        // autocomplete text view.
        if (mAutoCompleteTextView != null) {
            mAutoCompleteAdapter = new SuggestedMemberListAdapter(mContext,
                    android.R.layout.simple_dropdown_item_1line);
            mAutoCompleteAdapter.setContentResolver(mContentResolver);
            mAutoCompleteAdapter.setAccountType(mAccountType);
            mAutoCompleteAdapter.setAccountName(mAccountName);
            mAutoCompleteAdapter.setDataSet(mDataSet);
            mAutoCompleteTextView.setAdapter(mAutoCompleteAdapter);
            mAutoCompleteTextView.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    SuggestedMember member = (SuggestedMember) view.getTag();
                    if (member == null) {
                        return; // just in case
                    }
                    loadMemberToAddToGroup(member.getRawContactId(),
                            String.valueOf(member.getContactId()));

                    // Update the autocomplete adapter so the contact doesn't get suggested again
                    mAutoCompleteAdapter.addNewMember(member.getContactId());

                    // Clear out the text field
                    mAutoCompleteTextView.setText("");
                }
            });

            /*
             * New feature by Mediatek Begin
             * group add member multiple
             */
            mMemberPicker = (ImageButton) editorView.findViewById(R.id.member_picker);
            mMemberPicker.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    addMembers();
                }
            });
            /*
             * New feature by Mediatek End
             */
            // Update the exempt list.  (mListToDisplay might have been restored from the saved
            // state.)
            mAutoCompleteAdapter.updateExistingMembersList(mListToDisplay);
        }

        // If the group name is ready only, don't let the user focus on the field.
        mGroupNameView.setFocusable(!mGroupNameIsReadOnly);
        if (isNewEditor) {
            mRootView.addView(editorView);
        }
        mStatus = Status.EDITING;
    }

    public void load(String action, Uri groupUri, Bundle intentExtras) {
        mAction = action;
        mGroupUri = groupUri;
        mGroupId = (groupUri != null) ? ContentUris.parseId(mGroupUri) : 0;
        mIntentExtras = intentExtras;
    }

    private void bindGroupMetaData(Cursor cursor) {
        if (!cursor.moveToFirst()) {
            Log.i(TAG, "Group not found with URI: " + mGroupUri + " Closing activity now.");
            if (mListener != null) {
                mListener.onGroupNotFound();
            }
            return;
        }
        mOriginalGroupName = cursor.getString(GroupMetaDataLoader.TITLE);
        mAccountName = cursor.getString(GroupMetaDataLoader.ACCOUNT_NAME);
        mAccountType = cursor.getString(GroupMetaDataLoader.ACCOUNT_TYPE);
        mDataSet = cursor.getString(GroupMetaDataLoader.DATA_SET);
        mGroupNameIsReadOnly = (cursor.getInt(GroupMetaDataLoader.IS_READ_ONLY) == 1);
        setupEditorForAccount();

        // Setup the group metadata display
        mGroupNameView.setText(mOriginalGroupName);
    }

    public void loadMemberToAddToGroup(long rawContactId, String contactId) {
        Bundle args = new Bundle();
        args.putLong(MEMBER_RAW_CONTACT_ID_KEY, rawContactId);
        args.putString(MEMBER_LOOKUP_URI_KEY, contactId);
        getLoaderManager().restartLoader(LOADER_NEW_GROUP_MEMBER, args, mContactLoaderListener);
    }

    public void setListener(Listener value) {
        mListener = value;
    }

    public void onDoneClicked() {
        if (isGroupMembershipEditable()) {
            /*
             * Change feature by Mediatek Begin
             * Original Android code:
             * save(SaveMode.CLOSE);
             * CR ID :ALPS00229457
             * Descriptions: 
             */
            save(SaveMode.CLOSE);
            /*
             * Change feature by Mediatek End
             */
        } else {
            // Just revert it.
            doRevertAction();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, final MenuInflater inflater) {
        /** M: Change Feature @{ */
        if (PhoneCapabilityTester.isUsingTwoPanes(mContext)) {
            inflater.inflate(R.menu.edit_group, menu);
        }
        /** @} */
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        /** M: Change Feature @{ */
        if (PhoneCapabilityTester.isUsingTwoPanes(mContext)) {
            switch (item.getItemId()) {
                case R.id.menu_discard:
                    return revert();
                default:
                    break;
            }
        }
        return false;
        /** @} */

        
    }

    private boolean revert() {
        if (!hasNameChange() && !hasMembershipChange()) {
            doRevertAction();
        } else {
            CancelEditDialogFragment.show(this);
        }
        return true;
    }

    private void doRevertAction() {
        // When this Fragment is closed we don't want it to auto-save
        mStatus = Status.CLOSING;
        if (mListener != null) {
            mListener.onReverted();
        }
    }

    public static class CancelEditDialogFragment extends DialogFragment {

        public static void show(GroupEditorFragment fragment) {
            CancelEditDialogFragment dialog = new CancelEditDialogFragment();
            dialog.setTargetFragment(fragment, 0);
            dialog.show(fragment.getFragmentManager(), "cancelEditor");
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog dialog = new AlertDialog.Builder(getActivity())
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .setMessage(R.string.cancel_confirmation_dialog_message)
                    .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int whichButton) {
                                ((GroupEditorFragment) getTargetFragment()).doRevertAction();
                            }
                        }
                    )
                    .setNegativeButton(android.R.string.cancel, null)
                    .create();
            return dialog;
        }
    }

    // The following lines are provided and maintained by Mediatek Inc.
    public boolean save(int saveMode) {
        return save(saveMode, true);
    }
    // The previous lines are provided and maintained by Mediatek Inc.
    
    /**
     * Saves or creates the group based on the mode, and if successful
     * finishes the activity. This actually only handles saving the group name.
     * @return true when successful
     */
    // The following lines are provided and maintained by Mediatek Inc.
    public boolean save(int saveMode, boolean showErrorTips) { 
        //Check background sim service state if it is sim group
        if (!checkSimServiceState(true)) {
            if (saveMode == SaveMode.CLOSE) {
                getLoaderManager().destroyLoader(LOADER_EXISTING_MEMBERS);
            }
            onSaveCompleted(false, null);
            return true;
        }
        
        if (!hasValidGroupName(showErrorTips) || mStatus != Status.EDITING) {
            return false;
        }
        // The previous lines are provided and maintained by Mediatek Inc.
        // If we are about to close the editor - there is no need to refresh the data
        getLoaderManager().destroyLoader(LOADER_EXISTING_MEMBERS);

        // If there are no changes, then go straight to onSaveCompleted()
        if (!hasNameChange() && !hasMembershipChange()) {
            onSaveCompleted(false, mGroupUri);
            return true;
        }

        mStatus = Status.SAVING;

        Activity activity = getActivity();
        // If the activity is not there anymore, then we can't continue with the save process.
        if (activity == null) {
            return false;
        }
        Intent saveIntent = null;
        if (Intent.ACTION_INSERT.equals(mAction)) {
            /*
             * New feature by Mediatek Begin Original Android code: long[]
             * membersToAddArray = convertToArray(mListMembersToAdd); // Create
             * the save intent to create the group and add members at the same
             * time saveIntent =
             * ContactSaveService.createNewGroupIntent(activity, new
             * AccountWithDataSet(mAccountName, mAccountType, mDataSet),
             * mGroupNameView.getText().toString(), membersToAddArray,
             * activity.getClass(), GroupEditorActivity.ACTION_SAVE_COMPLETED);
             */
            long[] membersToAddArray = convertToArray(mListMembersToAdd);
            int [] simIndexArray = convertSimIndexToArray(mListMembersToAdd);
            // Create the save intent to create the group and add members at the same time
            saveIntent = ContactSaveService.createNewGroupIntent(activity,
                    new AccountWithDataSet(mAccountName, mAccountType, mDataSet),
                    mGroupNameView.getText().toString(),
                    membersToAddArray, activity.getClass(),
                   GroupEditorActivity.ACTION_SAVE_COMPLETED, simIndexArray, mSlotId);
            /*
             * New feature by Mediatek End
             */

        } else if (Intent.ACTION_EDIT.equals(mAction)) {
            // Create array of raw contact IDs for contacts to add to the group
            long[] membersToAddArray = convertToArray(mListMembersToAdd);
            // The following lines are provided and maintained by Mediatek Inc.
            int [] simIndexToAddArray = convertSimIndexToArray(mListMembersToAdd);
            // The previous lines are provided and maintained by Mediatek Inc.
            // Create array of raw contact IDs for contacts to add to the group
            long[] membersToRemoveArray = convertToArray(mListMembersToRemove);
            // The following lines are provided and maintained by Mediatek Inc.
            int [] simIndexToRemoveArray = convertSimIndexToArray(mListMembersToRemove);
            // The previous lines are provided and maintained by Mediatek Inc.
            // Create the update intent (which includes the updated group name if necessary)
            
            /*
             * New feature by Mediatek Begin Original Android code: saveIntent =
             * ContactSaveService.createGroupUpdateIntent(activity, mGroupId,
             * getUpdatedName(), membersToAddArray, membersToRemoveArray,
             * activity.getClass(), GroupEditorActivity.ACTION_SAVE_COMPLETED);
             */
            
            Log.i(TAG, "[edit] slotId" + mSlotId);
            Log.i(TAG, "[edit] getUpdatedName" + getUpdatedName());

            if (null != membersToAddArray) {
                Log.i(TAG, "[edit] membersToAddArray.len:" + membersToAddArray.length);
            }
            if (null != simIndexToAddArray) {
                Log.i(TAG, "[edit] simIndexToAddArray.len:" + simIndexToAddArray.length);
            }
            if (null != membersToRemoveArray) {
                Log.i(TAG, "[edit] membersToRemoveArray.len:" + membersToRemoveArray.length);
            }
            if (null != simIndexToRemoveArray) {
                Log.i(TAG, "[edit] simIndexToRemoveArray.len:" + simIndexToRemoveArray.length);
            }

            saveIntent = ContactSaveService.createGroupUpdateIntent(activity,
                    mGroupId, getUpdatedName(), membersToAddArray,
                    membersToRemoveArray, activity.getClass(),
                    GroupEditorActivity.ACTION_SAVE_COMPLETED,
                    mOriginalGroupName, mSlotId, simIndexToAddArray,
                    simIndexToRemoveArray,
                    new AccountWithDataSet(mAccountName, mAccountType, mDataSet));
  
            /*
             * New feature by Mediatek End
             */        
        } else {
            throw new IllegalStateException("Invalid intent action type " + mAction);
        }
        // The following lines are provided and maintained by Mediatek Inc.
        showDialog();
        // The previous lines are provided and maintained by Mediatek Inc.
        activity.startService(saveIntent);
        return true;
    }

    public void onSaveCompleted(boolean hadChanges, Uri groupUri) {
        dismissDialog();
        boolean success = groupUri != null;
        Log.d(TAG, "onSaveCompleted(" + groupUri + ")");
        if (hadChanges) {
            // Toast.makeText(mContext, success ? R.string.groupSavedToast :
            // R.string.groupSavedErrorToast, Toast.LENGTH_SHORT).show();
            Log.d(TAG, "onSaveCompleted hadChanges");
        }
        final Intent resultIntent;
        final int resultCode;
        if (success && groupUri != null) {
            final String requestAuthority = groupUri.getAuthority();

            resultIntent = new Intent();
            if (LEGACY_CONTACTS_AUTHORITY.equals(requestAuthority)) {
                // Build legacy Uri when requested by caller
                final long groupId = ContentUris.parseId(groupUri);
                final Uri legacyContentUri = Uri.parse("content://contacts/groups");
                final Uri legacyUri = ContentUris.withAppendedId(
                        legacyContentUri, groupId);
                resultIntent.setData(legacyUri);
            } else {
                // Otherwise pass back the given Uri
                resultIntent.setData(groupUri);
            }

            resultCode = Activity.RESULT_OK;
        } else {
            resultCode = Activity.RESULT_CANCELED;
            resultIntent = null;
        }
        // It is already saved, so prevent that it is saved again
        mStatus = Status.CLOSING;
        if (mListener != null) {
            mListener.onSaveFinished(resultCode, resultIntent);
        }
        clearMembersList();
        Log.d(TAG, "onSaveCompleted mListMembers.clear()");
    }
    // The following lines are provided and maintained by Mediatek Inc.
    private boolean hasValidGroupName(boolean showTips) {
        if (mGroupNameView != null) {
            if (!checkEmptyGroupName(showTips)) {
                return checkGroupName(mGroupNameView.getText().toString(), showTips);
            }
        }
        return false;
    // The previous lines are provided and maintained by Mediatek Inc.
    }

    private boolean hasNameChange() {
        return mGroupNameView != null &&
                !mGroupNameView.getText().toString().equals(mOriginalGroupName);
    }

    private boolean hasMembershipChange() {
        return mListMembersToAdd.size() > 0 || mListMembersToRemove.size() > 0;
    }

    /**
     * Returns the group's new name or null if there is no change from the
     * original name that was loaded for the group.
     */
    private String getUpdatedName() {
        String groupNameFromTextView = mGroupNameView.getText().toString();
        if (groupNameFromTextView.equals(mOriginalGroupName)) {
            // No name change, so return null
            return null;
        }
        return groupNameFromTextView;
    }

    private static long[] convertToArray(List<Member> listMembers) {
        int size = listMembers.size();
        long[] membersArray = new long[size];
        for (int i = 0; i < size; i++) {
            membersArray[i] = listMembers.get(i).getRawContactId();
        }
        return membersArray;
    }

    private void addExistingMembers(List<Member> members) {

        // Re-create the list to display
        mListToDisplay.clear();
        mListToDisplay.addAll(members);
        mListToDisplay.addAll(mListMembersToAdd);
        mListToDisplay.removeAll(mListMembersToRemove);
        mMemberListAdapter.notifyDataSetChanged();


        // Update the autocomplete adapter (if there is one) so these contacts don't get suggested
        if (mAutoCompleteAdapter != null) {
            // The following lines are provided and maintained by Mediatek Inc.
            mAutoCompleteAdapter.updateExistingMembersList(mListToDisplay);
            // The previous lines are provided and maintained by Mediatek Inc.
        }
    }

    private void addMember(Member member) {
        /** M: Bug Fix for CR ALPS00387554 @{ */
        if (!mListMembersToAdd.contains(member) && !mListToDisplay.contains(member)) {
            mListMembersToAdd.add(member);
        }
        if (!mListToDisplay.contains(member)) {
            mListToDisplay.add(member);
        }
        mMemberListAdapter.notifyDataSetChanged();
        /** @} */
        // Update the autocomplete adapter so the contact doesn't get suggested again
        mAutoCompleteAdapter.addNewMember(member.getContactId());
    }

    private void removeMember(Member member) {
        // If the contact was just added during this session, remove it from the list of
        // members to add
        if (mListMembersToAdd.contains(member)) {
            mListMembersToAdd.remove(member);
        } else {
            // Otherwise this contact was already part of the existing list of contacts,
            // so we need to do a content provider deletion operation
            // The following lines are provided and maintained by Mediatek Inc.
            if (!mListMembersToRemove.contains(member)) {
                mListMembersToRemove.add(member);
            }
            // The previous lines are provided and maintained by Mediatek Inc.
        }
        // In either case, update the UI so the contact is no longer in the list of
        // members
        // The following lines are provided and maintained by Mediatek Inc.
        if (mListToDisplay.contains(member)) {
            mListToDisplay.remove(member);
        }
        // The previous lines are provided and maintained by Mediatek Inc.
        mMemberListAdapter.notifyDataSetChanged();

        // Update the autocomplete adapter so the contact can get suggested again
        mAutoCompleteAdapter.removeMember(member.getContactId());
    }

    /**
     * The listener for the group metadata (i.e. group name, account type, and account name) loader.
     */
    private final LoaderManager.LoaderCallbacks<Cursor> mGroupMetaDataLoaderListener =
            new LoaderCallbacks<Cursor>() {

        @Override
        public CursorLoader onCreateLoader(int id, Bundle args) {
            // The following lines are provided and maintained by Mediatek Inc.
            return new GroupMetaDataLoader(mContext, mGroupUri, false);
            // The previous lines are provided and maintained by Mediatek Inc.
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            bindGroupMetaData(data);

            // Load existing members
            getLoaderManager().initLoader(LOADER_EXISTING_MEMBERS, null,
                    mGroupMemberListLoaderListener);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {}
    };

    /**
     * The loader listener for the list of existing group members.
     */
    private final LoaderManager.LoaderCallbacks<Cursor> mGroupMemberListLoaderListener =
            new LoaderCallbacks<Cursor>() {

        @Override
        public CursorLoader onCreateLoader(int id, Bundle args) {
            return GroupMemberLoader.constructLoaderForGroupEditorQuery(mContext, mGroupId);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            List<Member> listExistingMembers = new ArrayList<Member>();
            data.moveToPosition(-1);
            while (data.moveToNext()) {
                long contactId = data.getLong(GroupEditorQuery.CONTACT_ID);
                long rawContactId = data.getLong(GroupEditorQuery.RAW_CONTACT_ID);
                String lookupKey = data.getString(GroupEditorQuery.CONTACT_LOOKUP_KEY);
                String displayName = data.getString(GroupEditorQuery.CONTACT_DISPLAY_NAME_PRIMARY);
                String photoUri = data.getString(GroupEditorQuery.CONTACT_PHOTO_URI);
                // The following lines are provided and maintained by Mediatek Inc.
                int simIndex = data.getInt(GroupEditorQuery.CONTACT_INDEX_IN_SIM_COLUMN_INDEX);
                
                int i = -1;
                if (mSimInfoWrapper == null) {
                    mSimInfoWrapper = SIMInfoWrapper.getDefault();
                }

                SIMInfo simInfo = mSimInfoWrapper.getSimInfoBySlot(mSlotId);
                if (simInfo != null) {
                    i = simInfo.mColor;
                }
                if (simIndex > 0) {
                    int isSdnContact = data
                            .getInt(GroupEditorQuery.CONTACT_IS_SDN_CONTACT_COLUMN_INDEX);
                    photoUri = new SimContactPhotoUtils().getPhotoUri(isSdnContact, i);

                }
                
                listExistingMembers.add(new Member(rawContactId, lookupKey, contactId,
                        displayName, photoUri, simIndex));
                // The previous lines are provided and maintained by Mediatek Inc.
            }

            // Update the display list
            addExistingMembers(listExistingMembers);

            // No more updates
            // TODO: move to a runnable
            getLoaderManager().destroyLoader(LOADER_EXISTING_MEMBERS);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {}
    };

    /**
     * The listener to load a summary of details for a contact.
     */
    // TODO: Remove this step because showing the aggregate contact can be confusing when the user
    // just selected a raw contact
    private final LoaderManager.LoaderCallbacks<Cursor> mContactLoaderListener =
            new LoaderCallbacks<Cursor>() {

        private long mRawContactId;

        @Override
        public CursorLoader onCreateLoader(int id, Bundle args) {
            String memberId = args.getString(MEMBER_LOOKUP_URI_KEY);
            mRawContactId = args.getLong(MEMBER_RAW_CONTACT_ID_KEY);
            return new CursorLoader(mContext, Uri.withAppendedPath(Contacts.CONTENT_URI, memberId),
                    PROJECTION_CONTACT, null, null, null);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
            if (!cursor.moveToFirst()) {
                return;
            }
            // Retrieve the contact data fields that will be sufficient to update the adapter with
            // a new entry for this contact
            long contactId = cursor.getLong(CONTACT_ID_COLUMN_INDEX);
            String displayName = cursor.getString(CONTACT_DISPLAY_NAME_PRIMARY_COLUMN_INDEX);
            String lookupKey = cursor.getString(CONTACT_LOOKUP_KEY_COLUMN_INDEX);
            String photoUri = cursor.getString(CONTACT_PHOTO_URI_COLUMN_INDEX);

            // the following lines are provided and maintained by Mediatek Inc.
            int simIndex = cursor.getInt(CONTACT_INDEX_IN_SIM_COLUMN_INDEX);
            int indexSimOrPhone = cursor.getInt(CONTACT_INDICATE_PHONE_SIM_COLUMN_INDEX);
            
            int i = -1;
            if (mSimInfoWrapper == null) {
                mSimInfoWrapper = SIMInfoWrapper.getDefault();
            }
            SIMInfo simInfo = mSimInfoWrapper.getSimInfoBySlot(mSlotId);
            if (simInfo != null) {
                i = simInfo.mColor;
            }
            if (simIndex > 0) {
                int isSdnContact = cursor
                        .getInt(CONTACT_IS_SDN_CONTACT_COLUMN_INDEX);
                photoUri = new SimContactPhotoUtils().getPhotoUri(isSdnContact, i);
            }
            
            // the previous lines are provided and maintained by Mediatek Inc.
            
            getLoaderManager().destroyLoader(LOADER_NEW_GROUP_MEMBER);
            // The following lines are provided and maintained by Mediatek Inc.
            Member member = new Member(mRawContactId, lookupKey, contactId, displayName, photoUri, simIndex);
            // The previous lines are provided and maintained by Mediatek Inc.
            addMember(member);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {}
    };

    /**
     * This represents a single member of the current group.
     */
    public static class Member implements Parcelable {

        // TODO: Switch to just dealing with raw contact IDs everywhere if possible
        private final long mRawContactId;
        private final long mContactId;
        private final Uri mLookupUri;
        private final String mDisplayName;
        private final Uri mPhotoUri;
        
        // the following lines are provided and maintained by Mediatek Inc.
        private final int mSimIndex;
//        private final int mSimSlotId;
        // the previous lines are provided and maintained by Mediatek Inc.
        
        /**
         * @param simIndex  the sim index add by mediatek
         */
        public Member(long rawContactId, String lookupKey, long contactId, String displayName,
                String photoUri, int simIndex) { 
            mRawContactId = rawContactId;
            mContactId = contactId;
            mLookupUri = Contacts.getLookupUri(contactId, lookupKey);
            mDisplayName = displayName;
            mPhotoUri = (photoUri != null) ? Uri.parse(photoUri) : null;
            
            // the following lines are provided and maintained by Mediatek Inc.
            mSimIndex = simIndex;
            // the previous lines are provided and maintained by Mediatek Inc.
            
        }

        public long getRawContactId() {
            return mRawContactId;
        }

        public long getContactId() {
            return mContactId;
        }

        public Uri getLookupUri() {
            return mLookupUri;
        }

        public String getDisplayName() {
            return mDisplayName;
        }

        public Uri getPhotoUri() {
            return mPhotoUri;
        }

        @Override
        public boolean equals(Object object) {
            if (object instanceof Member) {
                Member otherMember = (Member) object;
                return Objects.equal(mLookupUri, otherMember.getLookupUri());
            }
            return false;
        }

        @Override
        public int hashCode() {
            return mLookupUri == null ? 0 : mLookupUri.hashCode();
        }

        // Parcelable
        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeLong(mRawContactId);
            dest.writeLong(mContactId);
            dest.writeParcelable(mLookupUri, flags);
            dest.writeString(mDisplayName);
            dest.writeParcelable(mPhotoUri, flags);
            
            // the following lines are provided and maintained by Mediatek Inc.
            dest.writeInt(mSimIndex);
            // the previous lines are provided and maintained by Mediatek Inc.
            
        }

        private Member(Parcel in) {
            mRawContactId = in.readLong();
            mContactId = in.readLong();
            mLookupUri = in.readParcelable(getClass().getClassLoader());
            mDisplayName = in.readString();
            mPhotoUri = in.readParcelable(getClass().getClassLoader());
            
            // the following lines are provided and maintained by Mediatek Inc.
            mSimIndex = in.readInt();
            // the previous lines are provided and maintained by Mediatek Inc.
            
        }

        public static final Parcelable.Creator<Member> CREATOR = new Parcelable.Creator<Member>() {
            @Override
            public Member createFromParcel(Parcel in) {
                return new Member(in);
            }

            @Override
            public Member[] newArray(int size) {
                return new Member[size];
            }
        };
        
        // the following lines are provided and maintained by Mediatek Inc.
        public int getmSimIndex() {
            return mSimIndex;
        }
        // the previous lines are provided and maintained by Mediatek Inc.

    }

    /**
     * This adapter displays a list of members for the current group being edited.
     */
    private final class MemberListAdapter extends BaseAdapter {

        private boolean mIsGroupMembershipEditable = true;

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View result;
            if (convertView == null) {
                result = mLayoutInflater.inflate(mIsGroupMembershipEditable ?
                        R.layout.group_member_item : R.layout.external_group_member_item,
                        parent, false);
            } else {
                result = convertView;
            }
            final Member member = getItem(position);
            final int posi = position;

            QuickContactBadge badge = (QuickContactBadge) result.findViewById(R.id.badge);
            badge.assignContactUri(member.getLookupUri());
            /** M: Change Feature alps00452888 @{ */
            badge.setClickable(false);
            // badge.setOnTouchListener(new OnTouchListener() {
            // public boolean onTouch(View v, MotionEvent event) {
            // if (MotionEvent.ACTION_UP == event.getAction()) {
            // Log.d(TAG, "badge.setOnTouchListener posi = " + posi);
            // mMemberPosition = posi;
            // }
            // return false;
            // }
            // });
            /**
             * @}
             */
            TextView name = (TextView) result.findViewById(R.id.name);
            name.setText(member.getDisplayName());

            View deleteButton = result.findViewById(R.id.delete_button_container);
            if (deleteButton != null) {
                deleteButton.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        removeMember(member);
                    }
                });
            }

            mPhotoManager.loadPhoto(badge, member.getPhotoUri(),
                    ViewUtil.getConstantPreLayoutWidth(badge), false);
            return result;
        }

        @Override
        public int getCount() {
            return mListToDisplay.size();
        }

        @Override
        public Member getItem(int position) {
            return mListToDisplay.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        public void setIsGroupMembershipEditable(boolean editable) {
            mIsGroupMembershipEditable = editable;
        }
    }
    
    /// The following lines are provided and maintained by Mediatek Inc.
    private String mCategory = null;
    private int mSlotId = -1;
    private int mSimId;
    private String mSimName;
    private int mMemberPosition = -1;

    private BroadcastReceiver mReceiver;

    private SIMInfoWrapper mSimInfoWrapper;

    private static MyProgressDialog mProgressDialog;
    private MemberTask mTask = null;

    class SimReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "In onReceive ");
            final String action = intent.getAction();
            Log.i(TAG, "action is " + action);
            if (Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(action)) {
                processAirplaneModeChanged(intent);
            } else if (TelephonyIntents.ACTION_PHB_STATE_CHANGED.equals(action)) {
                processPhbStateChange(intent);
            } else if (GeminiPhone.EVENT_PRE_3G_SWITCH.equals(action)) {
                if (AccountType.isAccountTypeIccCard(mAccountType)) {
                    clearMembersList();
                }
                Log.i(TAG, "Modem switch .....");
                scrubActivity();
            } else if (Intent.ACTION_DUAL_SIM_MODE_CHANGED.equals(action)) {
                int mode = intent.getIntExtra("mode", -1);
                Log.i(TAG, "DUAL_SIM_MODE_CHANGED, new mode is: " + mode);
                ///M: (1 << mSlotId) & type == 0 means current slot was radio turned off
                if (mSlotId >= 0 && !SimCardUtils.isDualSimModeOn(mSlotId, mode)) {
                    Log.i(TAG, "current slot was turned off. slot id: " + mSlotId);
                    scrubActivity();
                }
            }
        }
    };

    public static int[] convertSimIndexToArray(List<Member> listMembers) {
        int size = listMembers.size();
        int[] simIndexArray = new int[size];
        for (int i = 0; i < size; i++) {
            simIndexArray[i] = listMembers.get(i).getmSimIndex();
        }
        return simIndexArray;
    }

    public void load(String action, Uri groupUri, Bundle intentExtras, int slotId, int simId) {
        mAction = action;
        mGroupUri = groupUri;
        mGroupId = (groupUri != null) ? ContentUris.parseId(mGroupUri) : 0;
        mIntentExtras = intentExtras;
        mSlotId = slotId;
        mSimId = simId;
    }

    private boolean checkSimServiceState(boolean showTips) {
        if (mSlotId >= 0 && SlotUtils.isSimServiceRunningOnSlot(mSlotId)) {
            if (showTips) {
                Toast.makeText(this.getActivity(),
                        R.string.msg_loading_sim_contacts_toast,
                        Toast.LENGTH_SHORT).show();
            }
            return false;
        }
        return true;
    }

    private boolean checkEmptyGroupName(boolean showTips) {
        if (mGroupNameView == null
                || TextUtils.isEmpty(mGroupNameView.getText())) {
            if (showTips) {
                Toast.makeText(mContext, R.string.name_needed, Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        return false;
    }

    /*
     * CR ID :ALP00S110925
     * Descriptions: 
     *    It should not save % or / as group name
     */
    private boolean checkGroupName(String groupName, boolean showTips) {
        if (groupName.contains("/") || groupName.contains("%")) {
            if (showTips) {
                Toast.makeText(mContext, R.string.save_group_fail, Toast.LENGTH_SHORT).show();
            }
            return false;
        }
        return true;
    }

    public boolean checkOnBackPressedState() {
        String groupName = mGroupNameView == null ? null : mGroupNameView
                .getText().toString();
        /**
         * M: for ALPS00316371,if there is "%",should pop up a toast
         * "save group failed"
         */
        if (mGroupNameView != null) {
            if (TextUtils.isEmpty(groupName)) {
                if (hasMembershipChange()) {
                    new AlertDialog.Builder(getActivity())
                            .setIconAttribute(android.R.attr.alertDialogIcon)
                            .setTitle(R.string.group_discard_member)
                            .setMessage(R.string.group_discard_member_reason)
                            .setPositiveButton(android.R.string.ok,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            dialog.dismiss();
                                            onSaveCompleted(false, null);
                                        }
                                    })
                            .setNegativeButton(android.R.string.cancel, null)
                            .create()
                            .show();
                    return true;
                }
            } else {
                return !checkGroupName(groupName, true);
            }
            /**
             * M: for ALPS00316371
             */
        }

        return false;
    }

    void processAirplaneModeChanged(Intent intent) {
        boolean isAirplaneModeOn = intent.getBooleanExtra("state", false);
        Log.i(TAG, "[processAirplaneModeChanged]isAirplaneModeOn:" + isAirplaneModeOn);
        if (isAirplaneModeOn && AccountType.isAccountTypeIccCard(mAccountType)) {
            clearMembersList();
        }
        if (isAirplaneModeOn) {
            scrubActivity();
        }
    }

    void processPhbStateChange(Intent intent) {
        boolean phbReady = intent.getBooleanExtra("ready", false);
        int slotId = intent.getIntExtra("simId", -10);
        Log.i(TAG, "[processPhbStateChange]phbReady:" + phbReady + "|slotId:" + slotId);
        if (!phbReady) {
            scrubActivity();
        }
    }

    private void scrubActivity() {
        if (mSlotId >= 0) {
            Log.d(TAG, "sScrubListener = " + sScrubListener);
            if (sScrubListener != null) {
                sScrubListener.scrubAffinity();
            }
            getActivity().finish();
        }
    }

    @Override
    public void onDestroy() {
        if (mReceiver != null) {
            mContext.unregisterReceiver(mReceiver);
            mReceiver = null;
            Log.i(TAG, "unregisterReceiver mReceiver");
        }
        super.onDestroy();
    }

    private void showDialog() {
        mProgressDialog = new MyProgressDialog();
        mProgressDialog.setTargetFragment(GroupEditorFragment.this, 0);
        mProgressDialog.show(GroupEditorFragment.this.getFragmentManager(), "wait");
        mProgressDialog.mIsDismiss = false;
        mProgressDialog.setCancelable(false);
    }

    private void dismissDialog() {
        Log.d(TAG, "this.getActivity()=" + this.getActivity());
        /** M: Bug Fix for ALPS00408272 @{ */
        if (mProgressDialog != null && mProgressDialog.getDialog() != null
                && mProgressDialog.getDialog().isShowing()) {
            if (mProgressDialog.mShouldDismiss) {
                mProgressDialog.mIsDismiss = true;
                return;
            }
            Log.d(TAG, "mProgressDialog.dismiss()");
            mProgressDialog.dismiss();
            mProgressDialog.mIsDismiss = false;
        }
        /** @} */
    }

    /** 
     *M: New Feature Contacts group add members multiple 
     */
    private void addMembers() {
        try {
            Intent intent = new Intent(this.getActivity(), ContactListMultiChoiceActivity.class);
            intent.setAction(com.mediatek.contacts.util.ContactsIntent.LIST.ACTION_GROUP_ADD_MULTICONTACTS);
            intent.setType(Contacts.CONTENT_TYPE);
            intent.putExtra("account_type", mAccountType);
            intent.putExtra("account_name", mAccountName);
            int size = mListToDisplay.size();
            long[] mContactIds = new long[size];
            int i = 0;
            for (GroupEditorFragment.Member member : mListToDisplay) {							
                mContactIds[i++] = member.getContactId();
            }
            intent.putExtra("member_ids", mContactIds);
            startActivityForResult(intent, MULTIPLE_ADD_GROUP_MEMBER);
        } catch (ActivityNotFoundException e) {
            Log.d(TAG, "ActivityNotFoundException for addMembers Intent");
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // if the return data is right.
        if (requestCode == MULTIPLE_ADD_GROUP_MEMBER && data != null) {
            // get the contactIds from contact multiple selection.
            long[] contactIds = data.getLongArrayExtra(RESULTINTENTEXTRANAME);
            showDialog();
            ArrayList<Object> params = new ArrayList<Object>();
            params.add(contactIds);
            params.add(this);
            mTask = MemberTask.getTask();
            mTask.execute(params);
        }
    }

    private static class MemberTask extends AsyncTask<ArrayList, Void, ArrayList> {

        private static MemberTask mInstance = null;
        private GroupEditorFragment mFragment;

        private MemberTask() {
            super();
        }

        public static MemberTask getTask() {
            if (mInstance != null) {
                Log.d(TAG, "MemberTask getTask() mInstance.cancel(true)");
                mInstance.cancel(true);
                mInstance = null;
            }
            mInstance = new MemberTask();
            return mInstance;
        }

        public static MemberTask getExistTask() {
            return mInstance;
        }

        public void setFragment(GroupEditorFragment fragment) {
            mFragment = fragment;
        }

        @Override
        protected ArrayList<Member> doInBackground(ArrayList... params) {
            if (isCancelled()) {
                return null;
            }
            ArrayList<Object> list = params[0];
            long[] contactIds = (long[]) list.get(0);
            mFragment = (GroupEditorFragment) list.get(1);

            String accountFilter = RawContacts.ACCOUNT_NAME + "=? AND " + RawContacts.ACCOUNT_TYPE + "=?";
            if (mFragment.mAccountType != null
                    && mFragment.mAccountType.equals(AccountType.ACCOUNT_TYPE_LOCAL_PHONE)) {
                accountFilter = "((" + accountFilter + ") OR ("
                        + RawContacts.ACCOUNT_NAME + " IS NULL AND "
                        + RawContacts.ACCOUNT_TYPE + " IS NULL ))";
            }
            String[] selectArgs;
            if (mFragment.mDataSet == null) {
                accountFilter += " AND " + RawContacts.DATA_SET + " IS NULL";
                selectArgs = new String[] {
                        mFragment.mAccountName, mFragment.mAccountType
                };
            } else {
                accountFilter += " AND " + RawContacts.DATA_SET + "=?";
                selectArgs = new String[] {
                        mFragment.mAccountName, mFragment.mAccountType, mFragment.mDataSet
                };
            }

            final StringBuilder rawContactIdSelectionBuilder = new StringBuilder();
            rawContactIdSelectionBuilder.append(RawContacts.CONTACT_ID + " IN (");
            if (contactIds.length > 0) {
                rawContactIdSelectionBuilder.append(contactIds[0]);
            }
            for (int i = 1; i < contactIds.length; i++) {
                rawContactIdSelectionBuilder.append("," + contactIds[i]);
            }
            rawContactIdSelectionBuilder.append(")");
            rawContactIdSelectionBuilder.append(" AND ").append(accountFilter);
            Log.d(TAG, "rawContactIdSelectionBuilder==" + rawContactIdSelectionBuilder);
            Cursor cursor = mFragment.mContentResolver.query(RawContactsEntity.CONTENT_URI,
                    // PROJECTION_MEMBER_DATA,
                    // rawContactIdSelectionBuilder.toString(),selectArgs,
                    // null);
                    new String[] {
                            RawContacts.CONTACT_ID, RawContacts._ID
                    }, rawContactIdSelectionBuilder.toString(), selectArgs, null);

            ArrayList<Long> contactIdss = new ArrayList<Long>();
            ArrayList<Long> rawContactIds = new ArrayList<Long>();
            try {
                if (!cursor.moveToFirst()) {
                    return null;
                }
                long contactId = 0;
                long rawContactId = 0;
                do {
                    contactId = cursor.getLong(0);
                    rawContactId = cursor.getLong(1);
                    if (!contactIdss.contains(contactId)) {
                        contactIdss.add(contactId);
                        rawContactIds.add(rawContactId);
                    }
                } while (cursor.moveToNext());
            } finally {
                cursor.close();
            }
            StringBuilder selectionBuilder = new StringBuilder(Contacts._ID + " IN (");
            if (contactIdss.size() > 0) {
                selectionBuilder.append(contactIdss.get(0));
            }
            for (int i = 1; i < contactIdss.size(); i++) {
                selectionBuilder.append(",").append(contactIdss.get(i));
            }
            selectionBuilder.append(")");
            String selection = selectionBuilder.toString();
            Log.i(TAG, "Contacts selection=" + selection);
            Cursor ContactsCursor = mFragment.mContentResolver.query(Contacts.CONTENT_URI,
                    PROJECTION_CONTACT, selection, null, null);
            ArrayList<Member> members = new ArrayList<Member>();
            try {
                if (rawContactIds != null && rawContactIds.size() > 0 && ContactsCursor != null) {
                    if (!ContactsCursor.moveToFirst()) {
                        return null;
                    }
                    HashMap contacts = new HashMap<Long, Long>();
                    int i = -1;
                    long contactId = 0;
                    String displayName = null;
                    String lookupKey = null;
                    String photoUri = null;
                    int simIndex = -1;
                    int indexSimOrPhone = -1;
                    int k;

                    do {
                        contactId = ContactsCursor.getLong(CONTACT_ID_COLUMN_INDEX);
                        displayName = ContactsCursor.getString(CONTACT_DISPLAY_NAME_PRIMARY_COLUMN_INDEX);
                        lookupKey = ContactsCursor.getString(CONTACT_LOOKUP_KEY_COLUMN_INDEX);
                        photoUri = ContactsCursor.getString(CONTACT_PHOTO_URI_COLUMN_INDEX);
                        simIndex = ContactsCursor.getInt(CONTACT_INDEX_IN_SIM_COLUMN_INDEX);
                        indexSimOrPhone = ContactsCursor.getInt(CONTACT_INDICATE_PHONE_SIM_COLUMN_INDEX);

                        i = -1;
                        if (mFragment.mSimInfoWrapper == null) {
                            mFragment.mSimInfoWrapper = SIMInfoWrapper.getDefault();
                        }
                        SIMInfo simInfo = mFragment.mSimInfoWrapper
                                .getSimInfoBySlot(mFragment.mSlotId);
                        if (simInfo != null) {
                            i = simInfo.mColor;
                        }
                        if (simIndex > 0) {
                            int isSdnContact = ContactsCursor
                                    .getInt(CONTACT_IS_SDN_CONTACT_COLUMN_INDEX);
                           photoUri = new SimContactPhotoUtils().getPhotoUri(isSdnContact, i);
                        }
                        
                        for (k = 0; k < rawContactIds.size(); k++) {
                            if (!contacts.containsKey(rawContactIds.get(k)) && contactId == contactIdss.get(k)) {
                                contacts.put(rawContactIds.get(k), contactIdss.get(k));
                                Member member = new Member(rawContactIds.get(k),
                                        lookupKey, contactId, displayName, photoUri, simIndex);
                                boolean isDisplay = mFragment.addMembersToAdd(member);
                                if (!isDisplay) {
                                    members.add(member);
                                    break;
                                }
                            }
                        }
                        contacts.clear();
                        if (isCancelled()) {
                            return null;
                        }
                    } while (ContactsCursor.moveToNext());
                }
            } finally {
                ContactsCursor.close();
            }
            return members;
        }

        @Override
        protected void onPostExecute(ArrayList result) {
            if (result != null) {
                Log.d(TAG, "onPostExecute result.size() == " + result.size());
                mFragment.addMembersToDisplay(result);
            }
            mFragment.dismissDialog();
            mFragment.mTask = null;
            mInstance = null;
            super.onPostExecute(result);
        }
    }

    public static class MyProgressDialog extends DialogFragment {

        private boolean mIsDismiss = false;
        private boolean mShouldDismiss = false;
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            ProgressDialog dialog = new ProgressDialog(getActivity());
            dialog.setMessage(getActivity().getString(R.string.please_wait));
            mProgressDialog = this;
            dialog.setCanceledOnTouchOutside(false);
            return dialog;
        }

        @Override
        public void onPause() {
            super.onPause();
            mShouldDismiss = true;
        }

        @Override
        public void onResume() {
            super.onResume();
            mShouldDismiss = false;
            if (mIsDismiss) {
                if (mProgressDialog != null && mProgressDialog.getDialog() != null
                        && mProgressDialog.getDialog().isShowing()) {
                    mProgressDialog.dismiss();
                    mIsDismiss = false;
                }
            }
        }
    }

    private boolean addMembersToAdd(Member member) {
        boolean isDisplay = true; // If the member in the mListToDisplay; if true,in ,so ignore it.
        if (!mListMembersToAdd.contains(member) && !mListToDisplay.contains(member)) {
            mListMembersToAdd.add(member);
            isDisplay = false;
        }
        // Update the auto-complete adapter so the contact doesn't get suggested again
        mAutoCompleteAdapter.addNewMember(member.getContactId());
        return isDisplay;
    }

    private void addMembersToDisplay(ArrayList result) {
        mListToDisplay.addAll(result);
        mMemberListAdapter.notifyDataSetChanged();
    }

    private void clearMembersList() {
        if (null != mListMembersToAdd) {
            mListMembersToAdd.clear();
        }
        if (null != mListMembersToRemove) {
            mListMembersToRemove.clear();
        }
        if (null != mListToDisplay) {
            mListToDisplay.clear();
        }
    }

    public void doDiscard() {
        revert();
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume mMemberPosition=" + mMemberPosition);
        String contactId;
        if (mMemberPosition != -1 && mListToDisplay != null
                && mListToDisplay.size() > mMemberPosition) {
            Log.d(TAG, "onResume mListToDisplay.size()=" + mListToDisplay.size());
            Member member = mListToDisplay.get(mMemberPosition);
            editMemberFromContact(member.getRawContactId(), String.valueOf(member.getContactId()), mMemberPosition);
            mMemberPosition = -1;
        }
        super.onResume();
    }

    public void editMemberFromContact(long rawContactId, String contactId, int position) {
        Bundle args = new Bundle();
        args.putLong(MEMBER_RAW_CONTACT_ID_KEY, rawContactId);
        args.putString(MEMBER_LOOKUP_URI_KEY, contactId);
        args.putInt("position", position);
        getLoaderManager().restartLoader(LOADER_NEW_GROUP_MEMBER, args, mContactRefreshListener);
    }

    private final LoaderManager.LoaderCallbacks<Cursor> mContactRefreshListener = new LoaderCallbacks<Cursor>() {

        private long mRawContactId;
        private int mPosition;

        @Override
        public CursorLoader onCreateLoader(int id, Bundle args) {
            String memberId = args.getString(MEMBER_LOOKUP_URI_KEY);
            mRawContactId = args.getLong(MEMBER_RAW_CONTACT_ID_KEY);
            mPosition = args.getInt("position");
            return new CursorLoader(mContext, Uri.withAppendedPath(Contacts.CONTENT_URI, memberId),
                            PROJECTION_CONTACT, null, null, null);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
            if (!cursor.moveToFirst()) {
                editMember(null,mPosition);
                return;
            }
            long contactId = cursor.getLong(CONTACT_ID_COLUMN_INDEX);
            String displayName = cursor.getString(CONTACT_DISPLAY_NAME_PRIMARY_COLUMN_INDEX);
            String lookupKey = cursor.getString(CONTACT_LOOKUP_KEY_COLUMN_INDEX);
            String photoUri = cursor.getString(CONTACT_PHOTO_URI_COLUMN_INDEX);
            int simIndex = cursor.getInt(CONTACT_INDEX_IN_SIM_COLUMN_INDEX);
            int indexSimOrPhone = cursor.getInt(CONTACT_INDICATE_PHONE_SIM_COLUMN_INDEX);

            int i = -1;
            if (mSimInfoWrapper == null) {
                mSimInfoWrapper = SIMInfoWrapper.getDefault();
            }
            if (SlotUtils.isGeminiEnabled()) {
                SIMInfo simInfo = mSimInfoWrapper.getSimInfoBySlot(mSlotId);
                if (simInfo != null) {
                    i = simInfo.mColor;
                }
            }
            if (simIndex > 0) {
                int isSdnContact = cursor.getInt(CONTACT_IS_SDN_CONTACT_COLUMN_INDEX);
                photoUri = new SimContactPhotoUtils().getPhotoUri(isSdnContact, i);
            }
            getLoaderManager().destroyLoader(LOADER_NEW_GROUP_MEMBER);
            Member member = new Member(mRawContactId, lookupKey, contactId, displayName, photoUri, simIndex);
            editMember(member, mPosition);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
        }
    };

    private void editMember(Member member, int position) {
        mMemberPosition = -1;
        // delete a contact through quickContact.so delete the member from add list.
        if (null == member) {
            if (mListToDisplay.size() > position) {
                member = mListToDisplay.get(position);
                mListToDisplay.remove(position);
                mMemberListAdapter.notifyDataSetChanged();
            }
            if (null != member && mListMembersToAdd.contains(member)) {
                Log.d(TAG, "editMember mListMembersToAdd.contains(member)");
                int index = mListMembersToAdd.indexOf(member);
                if (index != -1) {
                    mListMembersToAdd.remove(index);
                }
            }
            return;
        }

        if (mListToDisplay.size() > position) {
            Member memberAdd = mListToDisplay.get(position);
            if (null != memberAdd && mListMembersToAdd.contains(memberAdd)) {
                int index = mListMembersToAdd.indexOf(memberAdd);
                if (index != -1) {
                    // if displayname is changed,the lookupUri is changed too, so mListMembersToAdd.contains will be useless
                    mListMembersToAdd.set(index, member);
                }
            }
            mListToDisplay.set(position, member);
            Log.d(TAG, "editMember");
        }

        mMemberListAdapter.notifyDataSetChanged();
        mAutoCompleteAdapter.addNewMember(member.getContactId());
    }

    public static interface ScrubListener {
        public void scrubAffinity();
    }

    private static ScrubListener sScrubListener;

    public static void setScrubListener(ScrubListener listener) {
        if (!(listener instanceof com.mediatek.contacts.list.ContactsGroupAddMultiContactsFragment)) {
            Log.i(TAG, "error.");
            return;
        }
        sScrubListener = listener;
    }

    public static void removeScrubListener(ScrubListener listener) {
        sScrubListener = null;
    }
    /// The previous lines are provided and maintained by Mediatek Inc.

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        sHandler = null;
    }

    /**
     * This reference is to reduce the risk of leakage.
     */
    private static AccountChosenHandler sHandler;
    /**
     * M: update UI should be act in handler, instead of Call back directly.
     */
    private static class AccountChosenHandler extends Handler {

        public static final int ACCOUNT_CHOSEN = 0;
        public static final int ACCOUNT_CHOOSE_CANCEL = 1;
        private GroupEditorFragment mFragment;
        public AccountChosenHandler(GroupEditorFragment fragment) {
            mFragment = fragment;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case ACCOUNT_CHOSEN:
                mFragment.onAccountChosenInternal((AccountWithDataSet) msg.obj);
                break;
            case ACCOUNT_CHOOSE_CANCEL:
                mFragment.onAccountSelectorCancelledInternal();
                break;
            default:
                Log.w(TAG, "[handleMessage] invalid msg: " + msg.what);
                break;
            }
        }
    }

}
