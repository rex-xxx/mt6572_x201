/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.contacts.editor;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.CommonDataKinds.Organization;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.RawContacts;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.android.contacts.GroupMetaDataLoader;
import com.android.contacts.R;
import com.android.contacts.model.RawContactDelta;
import com.android.contacts.model.RawContactDelta.ValuesDelta;
import com.android.contacts.model.RawContactModifier;
import com.android.contacts.model.account.AccountType;
import com.android.contacts.model.account.AccountType.EditType;
import com.android.contacts.model.dataitem.DataKind;
import com.android.internal.util.Objects;

import java.util.ArrayList;

// The following lines are provided and maintained by Mediatek Inc.
// Description: for SIM name display
import android.util.Log;
import java.util.List;

import com.android.contacts.ext.ContactDetailExtension;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.model.account.AccountWithDataSet;

import com.mediatek.contacts.ExtensionManager;
import com.mediatek.contacts.model.AccountWithDataSetEx;
import com.mediatek.contacts.simcontact.SimCardUtils;
import com.mediatek.phone.SIMInfoWrapper;

// The previous lines are provided and maintained by Mediatek Inc.

/**
 * Custom view that provides all the editor interaction for a specific
 * {@link Contacts} represented through an {@link RawContactDelta}. Callers can
 * reuse this view and quickly rebuild its contents through
 * {@link #setState(RawContactDelta, AccountType, ViewIdGenerator)}.
 * <p>
 * Internal updates are performed against {@link ValuesDelta} so that the
 * source {@link RawContact} can be swapped out. Any state-based changes, such as
 * adding {@link Data} rows or changing {@link EditType}, are performed through
 * {@link RawContactModifier} to ensure that {@link AccountType} are enforced.
 */
public class RawContactEditorView extends BaseRawContactEditorView {
    private LayoutInflater mInflater;

    private StructuredNameEditorView mName;
    private PhoneticNameEditorView mPhoneticName;
    private GroupMembershipView mGroupMembershipView;

    private ViewGroup mFields;

    private ImageView mAccountIcon;
    private TextView mAccountTypeTextView;
    private TextView mAccountNameTextView;

    private Button mAddFieldButton;

    private long mRawContactId = -1;
    private boolean mAutoAddToDefaultGroup = true;
    private Cursor mGroupMetaData;
    private DataKind mGroupMembershipKind;
    private RawContactDelta mState;

    private boolean mPhoneticNameAdded;

    // The following lines are provided and maintained by Mediatek Inc.
    // Description: for SIM name display
    private List<AccountWithDataSet> mAccounts = null;
    // The previous lines are provided and maintained by Mediatek Inc.

    public RawContactEditorView(Context context) {
        super(context);
        // The following lines are provided and maintained by Mediatek Inc.
        // Description: for SIM name display
        initMembers(context);
        // The previous lines are provided and maintained by Mediatek Inc.
    }

    public RawContactEditorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // The following lines are provided and maintained by Mediatek Inc.
        // Description: for SIM name display
        initMembers(context);
        // The previous lines are provided and maintained by Mediatek Inc.
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        View view = getPhotoEditor();
        if (view != null) {
            view.setEnabled(enabled);
        }

        if (mName != null) {
            mName.setEnabled(enabled);
        }

        if (mPhoneticName != null) {
            mPhoneticName.setEnabled(enabled);
        }

        if (mFields != null) {
            int count = mFields.getChildCount();
            for (int i = 0; i < count; i++) {
                mFields.getChildAt(i).setEnabled(enabled);
            }
        }

        if (mGroupMembershipView != null) {
            mGroupMembershipView.setEnabled(enabled);
        }
        /*
         * New Feature by Mediatek Begin. 
         * Original Android's code:
         * mAddFieldButton.setEnabled(enabled);
         * Descriptions: crete sim/usim contact
         */
        if (mAddFieldButton != null) {
            mAddFieldButton.setEnabled(enabled);
        } 
        /*
         * Change Feature by Mediatek End.
         */
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mInflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mName = (StructuredNameEditorView)findViewById(R.id.edit_name);
        mName.setDeletable(false);

        mPhoneticName = (PhoneticNameEditorView)findViewById(R.id.edit_phonetic_name);
        mPhoneticName.setDeletable(false);

        mFields = (ViewGroup)findViewById(R.id.sect_fields);

        mAccountIcon = (ImageView) findViewById(R.id.account_icon);
        mAccountTypeTextView = (TextView) findViewById(R.id.account_type);
        mAccountNameTextView = (TextView) findViewById(R.id.account_name);

        mAddFieldButton = (Button) findViewById(R.id.button_add_field);
        /*
         * New Feature by Mediatek Begin. 
         * Original Android's code:
         * mAddFieldButton.setOnClickListener(new OnClickListener() {
         * @Override public void onClick(View v) {
         * showAddInformationPopupWindow(); } }); 
         * Descriptions: crete sim/usim contact
         */
        if (mAddFieldButton != null) {
            mAddFieldButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    /** M: New Feature */
                    //Extension the popupmenu
//                    showAddInformationPopupWindow();
                    showAddInformationPopupWindowEx();
                    /** @} */
                }
            });
        } 
        /*
         * Change Feature by Mediatek End.
         */
    }

    /**
     * Set the internal state for this view, given a current
     * {@link RawContactDelta} state and the {@link AccountType} that
     * apply to that state.
     */
    @Override
    public void setState(RawContactDelta state, AccountType type, ViewIdGenerator vig,
            boolean isProfile) {

        Log.d(TAG, "[setState]state: " + state + ", AccountType: " + type + ", isProfile: " + isProfile);
        mState = state;

        // Remove any existing sections
        mFields.removeAllViews();

        // Bail if invalid state or account type
        if (state == null || type == null) return;

        setId(vig.getId(state, null, null, ViewIdGenerator.NO_VIEW_INDEX));

        // Make sure we have a StructuredName and Organization
        RawContactModifier.ensureKindExists(state, type, StructuredName.CONTENT_ITEM_TYPE);
        RawContactModifier.ensureKindExists(state, type, Organization.CONTENT_ITEM_TYPE);

        mRawContactId = state.getRawContactId();

        // The following lines are provided and maintained by Mediatek Inc.
        // Description: for SIM name display
        String accountDisplayName = null;
        ValuesDelta values = state.getValues();
        String accountName = values.getAsString(RawContacts.ACCOUNT_NAME);
        if (type.isIccCardAccount()) {
            for (AccountWithDataSet ads : mAccounts) {
                if (ads instanceof AccountWithDataSetEx) {
                    //accountDisplayName = ((AccountWithDataSetEx) ads).getDisplayName();
                    
                    if (ads.name.equals(accountName)) {
                        int slotId = ((AccountWithDataSetEx) ads).mSlotId;
                        mSlotId = slotId;
                        Log.d(TAG, "[setState]got slotId " + mSlotId + " from AccountName: " + accountName);
                        accountDisplayName = SIMInfoWrapper.getDefault().getSimDisplayNameBySlotId(
                                slotId);
                    }
                }
            }
        }

        if (null != accountDisplayName) {
            accountName = accountDisplayName;
        }
        // The previous lines are provided and maintained by Mediatek Inc.

        // Fill in the account info
        if (isProfile) {
            // The following lines are provided and maintained by Mediatek Inc.
            // Description: for SIM name display, and accountName defined above
            // Keep previous code here.
            // String accountName = state.getAccountName();
            // The previous lines are provided and maintained by Mediatek inc.
            if (TextUtils.isEmpty(accountName)) {
                mAccountNameTextView.setVisibility(View.GONE);
                mAccountTypeTextView.setText(R.string.local_profile_title);
            } else {
                CharSequence accountType = type.getDisplayLabel(mContext);
                mAccountTypeTextView.setText(mContext.getString(R.string.external_profile_title,
                        accountType));
                mAccountNameTextView.setText(accountName);
            }
        } else {
            // The following lines are provided and maintained by Mediatek Inc.
            // Description: for SIM name display, and accountName defined above
            // Keep previous code here.
            // String accountName = state.getAccountName();
            // The previous lines are provided and maintained by Mediatek inc.
            CharSequence accountType = type.getDisplayLabel(mContext);
            if (TextUtils.isEmpty(accountType)) {
                accountType = mContext.getString(R.string.account_phone_only);
            }
            if (!TextUtils.isEmpty(accountName) && !AccountWithDataSetEx.isLocalPhone(type.accountType)) {
                mAccountNameTextView.setVisibility(View.VISIBLE);
                mAccountNameTextView.setText(
                        mContext.getString(R.string.from_account_format, accountName));
            } else {
                // Hide this view so the other text view will be centered vertically
                mAccountNameTextView.setVisibility(View.GONE);
            }
            /**
             * M:Change feature by Mediatek Begin.
             *   Original Android's code:
             *                 mAccountTypeTextView.setText(
             *          mContext.getString(R.string.account_type_format, accountType));
             *   CR ID: ALPS00453091
             *   Descriptions: if local phone account, set string to "Phone contact" @{
             */
            if (AccountWithDataSetEx.isLocalPhone(type.accountType)) {
                mAccountTypeTextView.setText(accountType);
            } else {
                mAccountTypeTextView.setText(mContext.getString(R.string.account_type_format, accountType));
            }
            /**
             * @} Change Feature by Mediatek End.
             */

        }
        Log.i("geticon","[Rawcontacteditorview] setstate");
        /*
         * Change feature by Mediatek Begin.
         *   Original Android's code:
         *     mAccountIcon.setImageDrawable(type.getDisplayIcon(mContext));
         *   CR ID: ALPS00233786
         *   Descriptions: cu feature change photo by slot id 
         */
        if (type != null && type.isIccCardAccount()) {
            Log.i("checkphoto", "RawContactEditorview slotId : " + mSlotId);
            mAccountIcon.setImageDrawable(type.getDisplayIconBySlotId(mContext, mSlotId));
        } else {
            mAccountIcon.setImageDrawable(type.getDisplayIcon(mContext));
        }
        /*
         * Change Feature by Mediatek End.
         */
        // Show photo editor when supported
        RawContactModifier.ensureKindExists(state, type, Photo.CONTENT_ITEM_TYPE);
        setHasPhotoEditor((type.getKindForMimetype(Photo.CONTENT_ITEM_TYPE) != null));
        getPhotoEditor().setEnabled(isEnabled());
        mName.setEnabled(isEnabled());

        mPhoneticName.setEnabled(isEnabled());

        // Show and hide the appropriate views
        mFields.setVisibility(View.VISIBLE);
        mName.setVisibility(View.VISIBLE);
        mPhoneticName.setVisibility(View.VISIBLE);

        /** M:AAS @ { mPhoneticName -> GONE */
        ExtensionManager.getInstance().getContactDetailExtension().updateView(mPhoneticName, 0,
                ContactDetailExtension.VIEW_UPDATE_VISIBILITY, ExtensionManager.COMMD_FOR_AAS);
        /*** M: @ } */

        mGroupMembershipKind = type.getKindForMimetype(GroupMembership.CONTENT_ITEM_TYPE);
        if (mGroupMembershipKind != null) {
            mGroupMembershipView = (GroupMembershipView)mInflater.inflate(
                    R.layout.item_group_membership, mFields, false);
            mGroupMembershipView.setKind(mGroupMembershipKind);
            mGroupMembershipView.setEnabled(isEnabled());
        }

        // Create editor sections for each possible data kind
        for (DataKind kind : type.getSortedDataKinds()) {
            // Skip kind of not editable
            if (!kind.editable) continue;

            final String mimeType = kind.mimeType;
            if (StructuredName.CONTENT_ITEM_TYPE.equals(mimeType)) {
                // Handle special case editor for structured name
                final ValuesDelta primary = state.getPrimaryEntry(mimeType);
                mName.setValues(
                        type.getKindForMimetype(DataKind.PSEUDO_MIME_TYPE_DISPLAY_NAME),
                        primary, state, false, vig);
                mPhoneticName.setValues(
                        type.getKindForMimetype(DataKind.PSEUDO_MIME_TYPE_PHONETIC_NAME),
                        primary, state, false, vig);
            } else if (Photo.CONTENT_ITEM_TYPE.equals(mimeType)) {
                // Handle special case editor for photos
                final ValuesDelta primary = state.getPrimaryEntry(mimeType);
                getPhotoEditor().setValues(kind, primary, state, false, vig);
            } else if (GroupMembership.CONTENT_ITEM_TYPE.equals(mimeType)) {
                if (mGroupMembershipView != null) {
                    mGroupMembershipView.setState(state);
                }
            } else if (Organization.CONTENT_ITEM_TYPE.equals(mimeType)) {
                // Create the organization section
                final KindSectionView section = (KindSectionView) mInflater.inflate(
                        R.layout.item_kind_section, mFields, false);
                section.setTitleVisible(false);
                section.setEnabled(isEnabled());
                section.setState(kind, state, false, vig);

                // If there is organization info for the contact already, display it
                if (!section.isEmpty()) {
                    mFields.addView(section);
                } else {
                    // Otherwise provide the user with an "add organization" button that shows the
                    // EditText fields only when clicked
                    final View organizationView = mInflater.inflate(
                            R.layout.organization_editor_view_switcher, mFields, false);
                    final View addOrganizationButton = organizationView.findViewById(
                            R.id.add_organization_button);
                    final ViewGroup organizationSectionViewContainer =
                            (ViewGroup) organizationView.findViewById(R.id.container);

                    organizationSectionViewContainer.addView(section);

                    // Setup the click listener for the "add organization" button
                    addOrganizationButton.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // Once the user expands the organization field, the user cannot
                            // collapse them again.
                            EditorAnimator.getInstance().expandOrganization(addOrganizationButton,
                                    organizationSectionViewContainer);
                        }
                    });

                    mFields.addView(organizationView);
                    }
            ///M:Bug Fix for ALPS00566570,some USIM card do not support storing Email address.
            } else if (Email.CONTENT_ITEM_TYPE.equals(mimeType) && type.isUSIMAccountType()
                    && SimCardUtils.getIccCardEmailCount(mSlotId) <= 0) {
                Log.i(TAG, "[setState]account is a USIM account and contains no Email field in slot " + mSlotId);
                continue;
            } else {
                // Otherwise use generic section-based editors
                if (kind.fieldList == null) continue;
                final KindSectionView section = (KindSectionView)mInflater.inflate(
                        R.layout.item_kind_section, mFields, false);
                section.setEnabled(isEnabled());
                section.setState(kind, state, false, vig);
                mFields.addView(section);
            }
        }
        /** M: Bug Fix for ALPS00440157 @{ */
        if (mGroupMembershipView != null && !isProfile) {
            mFields.addView(mGroupMembershipView);
        }
        /** @} */

        /*
         * New Feature by Mediatek Begin. 
         * Original Android's code:
         * updatePhoneticNameVisibility(); 
         * CR ID: ALPS00101852 
         * Descriptions: new SIM & USIM UI when create new account.
         */
        Log.i(TAG, "type : " + type.accountType);
        if (type.getKindForMimetype(DataKind.PSEUDO_MIME_TYPE_PHONETIC_NAME) != null) {
            updatePhoneticNameVisibility();
        }
        // UIM
        // else if (type.accountType.equals(mUsimAccountType) ||
        // type.accountType.equals(mSimAccountType)) {
        else if (type.accountType.equals(mUsimAccountType)
                || type.accountType.equals(mSimAccountType)
                || type.accountType.equals(mUimAccountType)) {
            // UIM
            Log.i(TAG, "***************");
            simTypeSetState(mFields);

        }
        /*
         * Change Feature by Mediatek End.
         */
        addToDefaultGroupIfNeeded();

        final int sectionCount = getSectionViewsWithoutFields().size();
        /** M: New Feature JB Porting for Sanity Test, JE 
         * Original Code:
         * mAddFieldButton.setVisibility(sectionCount > 0 ? View.VISIBLE : View.GONE);
         * @{ 
         */
        if (null != mAddFieldButton) {
            mAddFieldButton.setVisibility(sectionCount > 0 ? View.VISIBLE : View.GONE);
        }
        /** @} */

        /*
         * New Feature by Mediatek Begin. 
         * Original Android's code:
         * mAddFieldButton.setEnabled(enabled);
         * Descriptions: crete sim/usim contact
         */
        if (mAddFieldButton != null) {
            mAddFieldButton.setEnabled(isEnabled());
        }
        /*
         * Change Feature by Mediatek End.
         */
    }

    @Override
    public void setGroupMetaData(Cursor groupMetaData) {
        mGroupMetaData = groupMetaData;
        addToDefaultGroupIfNeeded();
        if (mGroupMembershipView != null) {
            mGroupMembershipView.setGroupMetaData(groupMetaData);
        }
    }

    public void setAutoAddToDefaultGroup(boolean flag) {
        this.mAutoAddToDefaultGroup = flag;
    }

    /**
     * If automatic addition to the default group was requested (see
     * {@link #setAutoAddToDefaultGroup}, checks if the raw contact is in any
     * group and if it is not adds it to the default group (in case of Google
     * contacts that's "My Contacts").
     */
    private void addToDefaultGroupIfNeeded() {
        if (!mAutoAddToDefaultGroup || mGroupMetaData == null || mGroupMetaData.isClosed()
                || mState == null) {
            return;
        }

        boolean hasGroupMembership = false;
        ArrayList<ValuesDelta> entries = mState.getMimeEntries(GroupMembership.CONTENT_ITEM_TYPE);
        if (entries != null) {
            for (ValuesDelta values : entries) {
                Long id = values.getGroupRowId();
                if (id != null && id.longValue() != 0) {
                    hasGroupMembership = true;
                    break;
                }
            }
        }

        if (!hasGroupMembership) {
            long defaultGroupId = getDefaultGroupId();
            if (defaultGroupId != -1) {
                ValuesDelta entry = RawContactModifier.insertChild(mState, mGroupMembershipKind);
                entry.setGroupRowId(defaultGroupId);
            }
        }
    }

    /**
     * Returns the default group (e.g. "My Contacts") for the current raw contact's
     * account.  Returns -1 if there is no such group.
     */
    private long getDefaultGroupId() {
        String accountType = mState.getAccountType();
        String accountName = mState.getAccountName();
        String accountDataSet = mState.getDataSet();
        mGroupMetaData.moveToPosition(-1);
        while (mGroupMetaData.moveToNext()) {
            String name = mGroupMetaData.getString(GroupMetaDataLoader.ACCOUNT_NAME);
            String type = mGroupMetaData.getString(GroupMetaDataLoader.ACCOUNT_TYPE);
            String dataSet = mGroupMetaData.getString(GroupMetaDataLoader.DATA_SET);
            if (name.equals(accountName) && type.equals(accountType)
                    && Objects.equal(dataSet, accountDataSet)) {
                long groupId = mGroupMetaData.getLong(GroupMetaDataLoader.GROUP_ID);
                if (!mGroupMetaData.isNull(GroupMetaDataLoader.AUTO_ADD)
                            && mGroupMetaData.getInt(GroupMetaDataLoader.AUTO_ADD) != 0) {
                    return groupId;
                }
            }
        }
        return -1;
    }

    public TextFieldsEditorView getNameEditor() {
        return mName;
    }

    public TextFieldsEditorView getPhoneticNameEditor() {
        return mPhoneticName;
    }

    private void updatePhoneticNameVisibility() {
        boolean showByDefault =
                getContext().getResources().getBoolean(R.bool.config_editor_include_phonetic_name);

        if (showByDefault || mPhoneticName.hasData() || mPhoneticNameAdded) {
            mPhoneticName.setVisibility(View.VISIBLE);
        } else {
            mPhoneticName.setVisibility(View.GONE);
        }
    }

    @Override
    public long getRawContactId() {
        return mRawContactId;
    }

    /**
     * Return a list of KindSectionViews that have no fields yet...
     * these are candidates to have fields added in
     * {@link #showAddInformationPopupWindow()}
     */
    private ArrayList<KindSectionView> getSectionViewsWithoutFields() {
        final ArrayList<KindSectionView> fields =
                new ArrayList<KindSectionView>(mFields.getChildCount());
        for (int i = 0; i < mFields.getChildCount(); i++) {
            View child = mFields.getChildAt(i);
            if (child instanceof KindSectionView) {
                final KindSectionView sectionView = (KindSectionView) child;
                // If the section is already visible (has 1 or more editors), then don't offer the
                // option to add this type of field in the popup menu
                if (sectionView.getEditorCount() > 0) {
                    continue;
                }
                DataKind kind = sectionView.getKind();
                // not a list and already exists? ignore
                if ((kind.typeOverallMax == 1) && sectionView.getEditorCount() != 0) {
                    continue;
                }
                if (DataKind.PSEUDO_MIME_TYPE_DISPLAY_NAME.equals(kind.mimeType)) {
                    continue;
                }

                if (DataKind.PSEUDO_MIME_TYPE_PHONETIC_NAME.equals(kind.mimeType)
                        && mPhoneticName.getVisibility() == View.VISIBLE) {
                    continue;
                }

                fields.add(sectionView);
            }
        }
        return fields;
    }

    private void showAddInformationPopupWindow() {
        final ArrayList<KindSectionView> fields = getSectionViewsWithoutFields();
        final PopupMenu popupMenu = new PopupMenu(getContext(), mAddFieldButton);
        final Menu menu = popupMenu.getMenu();
        for (int i = 0; i < fields.size(); i++) {
            menu.add(Menu.NONE, i, Menu.NONE, fields.get(i).getTitle());
        }

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                final KindSectionView view = fields.get(item.getItemId());
                if (DataKind.PSEUDO_MIME_TYPE_PHONETIC_NAME.equals(view.getKind().mimeType)) {
                    mPhoneticNameAdded = true;
                    updatePhoneticNameVisibility();
                } else {
                    view.addItem();
                }

                // If this was the last section without an entry, we just added one, and therefore
                // there's no reason to show the button.
                if (fields.size() == 1) {
                    mAddFieldButton.setVisibility(View.GONE);
                }

                return true;
            }
        });

        popupMenu.show();
    }

    // The following lines are provided and maintained by Mediatek Inc.
    // Description: for SIM name display
    private void initMembers(Context context) {
        mAccounts = AccountTypeManager.getInstance(context).getAccounts(true);
    }

    // The previous lines are provided and maintained by Mediatek inc.
    /*
     * New Feature by Mediatek Begin. Original Android¡¯s code: CR ID:
     * Descriptions: ¡­
     */
    private String mUsimAccountType = AccountType.ACCOUNT_TYPE_USIM;
    private String mSimAccountType = AccountType.ACCOUNT_TYPE_SIM;
    // UIM
    private String mUimAccountType = AccountType.ACCOUNT_TYPE_UIM;
    // UIM
    private static final String TAG = "RawContactEditorView";
    private static int mSlotId = -1;

    private void simTypeSetState(ViewGroup fields) {
        Log.i(TAG, "simTypeSetState count = " + fields.getChildCount());
        for (int i = 0; i < fields.getChildCount(); i++) {
            View child = fields.getChildAt(i);
            if (child instanceof KindSectionView) {
                final KindSectionView sectionView = (KindSectionView) child;
                if (sectionView.getEditorCount() > 0) {
                    continue;
                }
                DataKind kind = sectionView.getKind();
                if ((kind.typeOverallMax == 1) && sectionView.getEditorCount() != 0) {
                    continue;
                }
                if (DataKind.PSEUDO_MIME_TYPE_DISPLAY_NAME.equals(kind.mimeType)) {
                    continue;
                }

                if (DataKind.PSEUDO_MIME_TYPE_PHONETIC_NAME.equals(kind.mimeType)
                        && mPhoneticName.getVisibility() == View.VISIBLE) {
                    continue;
                }
                Log.i(TAG, "the child :" + child);

                /** M: Bug Fix for CR ALPS00328644 @{ */
                /*
                 * Original Code: ((KindSectionView) child).addItem();
                 */
                ((KindSectionView) child).addSimItem();
                /** @} */
            }
        }

    }

    private void showAddInformationPopupWindowEx() {
        final ArrayList<KindSectionView> fields = getSectionViewsWithoutFields();
        if (null == mPopupMenu) {
            mPopupMenu = new PopupMenu(getContext(), mAddFieldButton);
        }
        Log.i(TAG, "create mPopupMenu hashCode : " + mPopupMenu.hashCode());
        final Menu menu = mPopupMenu.getMenu();
        menu.clear(); // remove all existing item
        for (int i = 0; i < fields.size(); i++) {
            menu.add(Menu.NONE, i, Menu.NONE, fields.get(i).getTitle());
        }

        mPopupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                final KindSectionView view = fields.get(item.getItemId());
                if (DataKind.PSEUDO_MIME_TYPE_PHONETIC_NAME.equals(view.getKind().mimeType)) {
                    mPhoneticNameAdded = true;
                    updatePhoneticNameVisibility();
                } else {
                    view.addItem();
                }
                if (fields.size() == 1) {
                    mAddFieldButton.setVisibility(View.GONE);
                }

                return true;
            }
        });

        mPopupMenu.show();
    }

    public boolean dismissAddFieldPopupMenu() {
        if (null != mPopupMenu) {
            Log.i(TAG, "dismiss mPopupMenu hashCode : " + mPopupMenu.hashCode());
            mPopupMenu.dismiss();
            return true;
        }
        return false;
    }

    private PopupMenu mPopupMenu;
    /*
     * New Feature by Mediatek End.
     */
}
