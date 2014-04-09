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
package com.android.contacts.group;

import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.Contacts.Data;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.RawContactsEntity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.contacts.R;
import com.android.contacts.group.SuggestedMemberListAdapter.SuggestedMember;
import com.android.contacts.model.account.AccountType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

// The following lines are provided and maintained by Mediatek Inc.
import android.net.Uri;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Contacts;
import android.util.Log;
import com.android.contacts.ContactPhotoManager;
import com.mediatek.contacts.dialpad.DialerSearchUtils;
import com.mediatek.contacts.simcontact.SimCardUtils;
import com.mediatek.phone.SIMInfoWrapper;
// The previous lines are provided and maintained by Mediatek Inc.

/**
 * This adapter provides suggested contacts that can be added to a group for an
 * {@link AutoCompleteTextView} within the group editor.
 */
public class SuggestedMemberListAdapter extends ArrayAdapter<SuggestedMember> {

    private static final String TAG = "SuggestedMemberListAdapter";

    private final Context mContext;

    /*
     * Bug Fix by Mediatek Begin.
     * Android original code:
     * 
     *   private static final String[] PROJECTION_FILTERED_MEMBERS = new String[] {
     *       RawContacts._ID,                        // 0
     *       RawContacts.CONTACT_ID,                 // 1
     *       RawContacts.DISPLAY_NAME_PRIMARY,        // 2
     *   };
     *
     *   private static final int RAW_CONTACT_ID_COLUMN_INDEX = 0;
     *   private static final int CONTACT_ID_COLUMN_INDEX = 1;
     *   private static final int DISPLAY_NAME_PRIMARY_COLUMN_INDEX = 2;
     *   
     *   private static final String[] PROJECTION_MEMBER_DATA = new String[] {
     *       RawContacts._ID,                        // 0
     *       RawContacts.CONTACT_ID,                 // 1
     *       Data.MIMETYPE,                          // 2
     *       Data.DATA1,                             // 3
     *       Photo.PHOTO,                            // 4
     *   };
     *
     *   private static final int MIMETYPE_COLUMN_INDEX = 2;
     *   private static final int DATA_COLUMN_INDEX = 3;
     *   private static final int PHOTO_COLUMN_INDEX = 4;
     *   
     *   CR ID: ALPS00280807
     *   Descriptions:
     *    Support Pinyin search in Chinese.
     *    Support Phone number, email account search.
     */
    
    private static final String[] PROJECTION_FILTERED_MEMBERS = new String[] {
        Contacts._ID,                        // 0
        Contacts.DISPLAY_NAME_PRIMARY,       // 1
        Contacts.INDICATE_PHONE_SIM,         // 2
        Contacts.IS_SDN_CONTACT,             //3
    };

    private static final int CONTACT_ID_COLUMN_INDEX = 0;
    private static final int DISPLAY_NAME_PRIMARY_COLUMN_INDEX = 1;
    private static final int RAW_CONTACT_SIM_ID = 2;
    private static final int IS_SDN_CONTACT = 3;

    private static final String[] PROJECTION_MEMBER_DATA = new String[] {
        RawContacts.CONTACT_ID,                 // 0
        Data.MIMETYPE,                          // 1
        Data.DATA1,                             // 2
        Photo.PHOTO,                            // 3
        RawContacts._ID,                        // 4
    };

    private static final int MIMETYPE_COLUMN_INDEX = 1;
    private static final int DATA_COLUMN_INDEX = 2;
    private static final int PHOTO_COLUMN_INDEX = 3;
    private static final int RAW_CONTACT_ID_COLUMN_INDEX = 4;
    /*
     * Bug Fix by Mediatek End.
     */

    private Filter mFilter;
    private ContentResolver mContentResolver;
    private LayoutInflater mInflater;

    private String mAccountType;
    private String mAccountName;
    private String mDataSet;

    // TODO: Make this a Map for better performance when we check if a new contact is in the list
    // or not
    private final List<Long> mExistingMemberContactIds = new ArrayList<Long>();

    private static final int SUGGESTIONS_LIMIT = 5;

    public SuggestedMemberListAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        // The previous lines are provided and maintained by Mediatek Inc.
        mContext = context;
        // The previous lines are provided and maintained by Mediatek Inc.

    }

    public void setAccountType(String accountType) {
        mAccountType = accountType;
    }

    public void setAccountName(String accountName) {
        mAccountName = accountName;
    }

    public void setDataSet(String dataSet) {
        mDataSet = dataSet;
    }

    public void setContentResolver(ContentResolver resolver) {
        mContentResolver = resolver;
    }

    public void updateExistingMembersList(List<GroupEditorFragment.Member> list) {
        mExistingMemberContactIds.clear();
        for (GroupEditorFragment.Member member : list) {
            mExistingMemberContactIds.add(member.getContactId());
        }
    }

    public void addNewMember(long contactId) {
        // The following lines are provided and maintained by Mediatek Inc.
        if (!mExistingMemberContactIds.contains(contactId)) {
            mExistingMemberContactIds.add(contactId);
        }
        // The previous lines are provided and maintained by Mediatek Inc.
    }

    public void removeMember(long contactId) {
        if (mExistingMemberContactIds.contains(contactId)) {
            mExistingMemberContactIds.remove(contactId);
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View result = convertView;
        if (result == null) {
            result = mInflater.inflate(R.layout.group_member_suggestion, parent, false);
        }
        // TODO: Use a viewholder
        SuggestedMember member = getItem(position);
        TextView text1 = (TextView) result.findViewById(R.id.text1);
        TextView text2 = (TextView) result.findViewById(R.id.text2);
        ImageView icon = (ImageView) result.findViewById(R.id.icon);
        text1.setText(member.getDisplayName());
        if (member.hasExtraInfo()) {
            // The following lines are provided and maintained by Mediatek Inc.
            text2.setVisibility(View.VISIBLE);
            // The previous lines are provided and maintained by Mediatek Inc.
            text2.setText(member.getExtraInfo());
        } else {
            text2.setVisibility(View.GONE);
        }
        byte[] byteArray = member.getPhotoByteArray();
        if (byteArray == null) {
            // The following lines are provided and maintained by Mediatek Inc.
            ContactPhotoManager photoMgr = ContactPhotoManager.getInstance(this.getContext());
            long photoId = 0;
            if (member.getSimId() > 0) {
                photoId = DialerSearchUtils.getSimType(member.getSimId(), member.getIsSdnContact());
            }
            photoMgr.loadThumbnail(icon, photoId, false);

        } else {
            Bitmap bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
            icon.setImageBitmap(bitmap);
        }
        result.setTag(member);
        return result;
    }

    @Override
    public Filter getFilter() {
        if (mFilter == null) {
            mFilter = new SuggestedMemberFilter();
        }
        return mFilter;
    }

    /**
     * This filter queries for raw contacts that match the given account name and account type,
     * as well as the search query.
     */
    public class SuggestedMemberFilter extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence prefix) {
            FilterResults results = new FilterResults();
            if (mContentResolver == null || TextUtils.isEmpty(prefix)) {
                return results;
            }

            // Create a list to store the suggested contacts (which will be alphabetically ordered),
            // but also keep a map of raw contact IDs to {@link SuggestedMember}s to make it easier
            // to add supplementary data to the contact (photo, phone, email) to the members based
            // on raw contact IDs after the second query is completed.
            List<SuggestedMember> suggestionsList = new ArrayList<SuggestedMember>();
            HashMap<Long, SuggestedMember> suggestionsMap = new HashMap<Long, SuggestedMember>();

            // First query for all the raw contacts that match the given search query
            // and have the same account name and type as specified in this adapter
            /*
             * Bug Fix by Mediatek Begin.
             *   Original Android code:
             *   String searchQuery = prefix.toString() + "%";
             *   String accountClause = RawContacts.ACCOUNT_NAME + "=? AND " +
             *           RawContacts.ACCOUNT_TYPE + "=?";
             *   String[] args;
             *   if (mDataSet == null) {
             *       accountClause += " AND " + RawContacts.DATA_SET + " IS NULL";
             *       args = new String[] {mAccountName, mAccountType, searchQuery, searchQuery};
             *   } else {
             *       accountClause += " AND " + RawContacts.DATA_SET + "=?";
             *       args = new String[] {
             *               mAccountName, mAccountType, mDataSet, searchQuery, searchQuery
             *       };
             *   }
             *   
             *   Cursor cursor = mContentResolver.query(
             *           RawContacts.CONTENT_URI, PROJECTION_FILTERED_MEMBERS,
             *           accountClause + " AND (" +
             *           RawContacts.DISPLAY_NAME_PRIMARY + " LIKE ? OR " +
             *           RawContacts.DISPLAY_NAME_ALTERNATIVE + " LIKE ? )",
             *           args, RawContacts.DISPLAY_NAME_PRIMARY + " COLLATE LOCALIZED ASC");
             *   CR ID: ALPS00113782,ALPS00280807
             *   Descriptions:
             *    The deleted contacts can be search out in group editor screen.
             *    Actually, they should not be search out.
             *    Support Pinyin search in Chinese.
             *    Support Phone number, email account search.
             */
            String searchFilter = prefix.toString();
            String accountFilter = RawContacts.ACCOUNT_NAME + "=? AND "
            + RawContacts.ACCOUNT_TYPE + "=?"; 
            if (mAccountType != null
                    && mAccountType.equals(AccountType.ACCOUNT_TYPE_LOCAL_PHONE)) {
                accountFilter = "((" + accountFilter + ") OR ("
                        + RawContacts.ACCOUNT_NAME + " IS NULL AND "
                        + RawContacts.ACCOUNT_TYPE + " IS NULL ))";
            }
            String[] selectArgs;
            if (mDataSet == null) {
                accountFilter += " AND " + RawContacts.DATA_SET + " IS NULL";
                selectArgs = new String[] { mAccountName, mAccountType};
            } else {
                accountFilter += " AND " + RawContacts.DATA_SET + "=?";
                selectArgs = new String[] { mAccountName, mAccountType, mDataSet};
            }
            Uri.Builder uriBuilder = Contacts.CONTENT_FILTER_URI.buildUpon()
                    .appendPath(searchFilter);
            final String selection = " EXISTS ("
                    + " SELECT "
                    + RawContacts._ID
                    + " FROM view_raw_contacts WHERE view_raw_contacts.contact_id=view_contacts._id AND "
                    + accountFilter + ")";
            Log.i(TAG, "begin the first query");
            Cursor cursor = mContentResolver.query(uriBuilder.build(), PROJECTION_FILTERED_MEMBERS,
                            selection, selectArgs,
                            RawContacts.DISPLAY_NAME_PRIMARY + " COLLATE LOCALIZED ASC");
            Log.i(TAG, "End the first query");
            /*
             * Bug Fix by Mediatek End.
             */

            if (cursor == null) {
                return results;
            }

            // Read back the results from the cursor and filter out existing group members.
            // For valid suggestions, add them to the hash map of suggested members.
            try {
                cursor.moveToPosition(-1);
                while (cursor.moveToNext() && suggestionsMap.keySet().size() < SUGGESTIONS_LIMIT) {
                    /*
                     * Bug Fix by Mediatek Begin.
                     *   long rawContactId = cursor.getLong(RAW_CONTACT_ID_COLUMN_INDEX);
                     *   
                     *   CR ID: ALPS00280807
                     *   Descriptions:
                     *    Support Pinyin search in Chinese.
                     *    Support Phone number, email account search.
                     */
                    long contactId = cursor.getLong(CONTACT_ID_COLUMN_INDEX);
                    // Filter out contacts that have already been added to this group
                    if (mExistingMemberContactIds.contains(contactId)) {
                        continue;
                    }
                    // Otherwise, add the contact as a suggested new group member
                    String displayName = cursor.getString(DISPLAY_NAME_PRIMARY_COLUMN_INDEX);
                    /*
                     * Bug Fix by Mediatek Begin.
                     *   SuggestedMember member = new SuggestedMember(rawContactId, displayName,
                     *      contactId);
                     *   
                     *   CR ID: ALPS00280807
                     *   Descriptions:
                     *    Support Pinyin search in Chinese.
                     *    Support Phone number, email account search.
                     */
                    SuggestedMember member = new SuggestedMember(-1, displayName,
                            contactId);
                    int simId = cursor.getInt(RAW_CONTACT_SIM_ID);
                    member.setSimId(simId);
                    if (simId > 0) { 
                        int slotId = SIMInfoWrapper.getDefault().getSlotIdBySimId(simId);
                        member.setSimType(SimCardUtils.getSimTypeBySlot(slotId));
                        member.setSlotId(slotId);
                    } else {
                        member.setSimType(-1);
                    }
                    
                    member.setIsSdnContact(cursor.getInt(IS_SDN_CONTACT));
                    /*
                     * Bug Fix by Mediatek End.
                     */
                    
                    // Store the member in the list of suggestions and add it to the hash map too.
                    suggestionsList.add(member);
                    // The following lines are provided and maintained by Mediatek Inc.
                    suggestionsMap.put(contactId, member);
                    // The previous lines are provided and maintained by Mediatek Inc.
                }
            } finally {
                cursor.close();
            }

            int numSuggestions = suggestionsMap.keySet().size();
            if (numSuggestions == 0) {
                return results;
            }

            // Create a part of the selection string for the next query with the pattern (?, ?, ?)
            // where the number of comma-separated question marks represent the number of raw
            // contact IDs found in the previous query (while respective the SUGGESTION_LIMIT)
            final StringBuilder rawContactIdSelectionBuilder = new StringBuilder();
            final String[] questionMarks = new String[numSuggestions];
            Arrays.fill(questionMarks, "?");
            // The following lines are provided and maintained by Mediatek Inc.
            rawContactIdSelectionBuilder.append(RawContacts.CONTACT_ID + " IN (")
                    // The previous lines are provided and maintained by Mediatek Inc.
                    .append(TextUtils.join(",", questionMarks))
                    .append(")");
            // The following lines are provided and maintained by Mediatek Inc.
            rawContactIdSelectionBuilder.append(" AND ").append(accountFilter);
            // The previous lines are provided and maintained by Mediatek Inc.

            // Construct the selection args based on the raw contact IDs we're interested in
            // (as well as the photo, email, and phone mimetypes)
            List<String> selectionArgs = new ArrayList<String>();
            selectionArgs.add(Photo.CONTENT_ITEM_TYPE);
            selectionArgs.add(Email.CONTENT_ITEM_TYPE);
            selectionArgs.add(Phone.CONTENT_ITEM_TYPE);
            // The following lines are provided and maintained by Mediatek Inc.
            selectionArgs.add(StructuredName.CONTENT_ITEM_TYPE);
            for (Long contactId : suggestionsMap.keySet()) {
                selectionArgs.add(String.valueOf(contactId));
            }
            for (String str: selectArgs) {
                selectionArgs.add(str);
            }

            Log.i(TAG, "Begin the second query");
            // The previous lines are provided and maintained by Mediatek Inc.
            // Perform a second query to retrieve a photo and possibly a phone number or email
            // address for the suggested contact
            Cursor memberDataCursor = mContentResolver.query(
                    RawContactsEntity.CONTENT_URI, PROJECTION_MEMBER_DATA,
                    "(" + Data.MIMETYPE + "=? OR " + Data.MIMETYPE + "=? OR " + Data.MIMETYPE +
                    // The following lines are provided and maintained by Mediatek Inc.
                    "=? OR " + Data.MIMETYPE + "=? ) AND " + rawContactIdSelectionBuilder.toString(),
                    // The previous lines are provided and maintained by Mediatek Inc.
                    selectionArgs.toArray(new String[0]), null);
            // The following lines are provided and maintained by Mediatek Inc.
            Log.i(TAG, "End the second query");
            HashMap<Long, SuggestedMember> jointContactsMap = new HashMap<Long, SuggestedMember>();
            // The previous lines are provided and maintained by Mediatek Inc.
            try {
                memberDataCursor.moveToPosition(-1);
                while (memberDataCursor.moveToNext()) {
                    long rawContactId = memberDataCursor.getLong(RAW_CONTACT_ID_COLUMN_INDEX);
                    // The following lines are provided and maintained by Mediatek Inc.
                    long contactId = memberDataCursor.getLong(CONTACT_ID_COLUMN_INDEX);
                    SuggestedMember member = suggestionsMap.get(contactId);
                    // The previous lines are provided and maintained by Mediatek Inc.
                    if (member == null) {
                        continue;
                    }
                    // The following lines are provided and maintained by Mediatek Inc.
                    if (member.getRawContactId() < 0) {
                        member.setRawContactId(rawContactId);
                    } else {
                        if (member.getRawContactId() != rawContactId) {
                            SuggestedMember tempMember = jointContactsMap.get(rawContactId);
                            if (tempMember == null) {
                                tempMember = new SuggestedMember(member);
                                tempMember.setRawContactId(rawContactId);
                                int index = suggestionsList.indexOf(member);
                                if (index >= 0 && index <= suggestionsList.size()) {
                                    suggestionsList.add(index, tempMember);
                                } else {
                                    suggestionsList.add(tempMember);
                                }
                                jointContactsMap.put(rawContactId, tempMember);
                            }
                            member = tempMember;
                        }
                    }
                    // The previous lines are provided and maintained by Mediatek Inc.
                    String mimetype = memberDataCursor.getString(MIMETYPE_COLUMN_INDEX);
                    if (Photo.CONTENT_ITEM_TYPE.equals(mimetype)) {
                        // Set photo
                        byte[] bitmapArray = memberDataCursor.getBlob(PHOTO_COLUMN_INDEX);
                        member.setPhotoByteArray(bitmapArray);
                    } else if (Email.CONTENT_ITEM_TYPE.equals(mimetype) ||
                            Phone.CONTENT_ITEM_TYPE.equals(mimetype)) {
                        // Set at most 1 extra piece of contact info that can be a phone number or
                        // email
                        if (!member.hasExtraInfo()) {
                            String info = memberDataCursor.getString(DATA_COLUMN_INDEX);
                            member.setExtraInfo(info);
                            // The following lines are provided and maintained by Mediatek Inc.
                            if (info.indexOf(searchFilter) > -1) {
                                member.setFixExtrasInfo(true);
                            }
                        } else {
                            if (!member.hasFixedExtrasInfo()) {
                                String info = memberDataCursor.getString(DATA_COLUMN_INDEX);
                                if (info.indexOf(searchFilter) > -1) {
                                    member.setExtraInfo(info);
                                    member.setFixExtrasInfo(true);
                                }
                            }
                            // The previous lines are provided and maintained by Mediatek Inc.
                        }
                    }
                }
            } finally {
                memberDataCursor.close();
            }
            
            // The following lines are provided and maintained by Mediatek Inc.
            results.values = suggestionsList.size() > SUGGESTIONS_LIMIT ? suggestionsList
                    .subList(0, SUGGESTIONS_LIMIT)
                    : suggestionsList;
            // The previous lines are provided and maintained by Mediatek Inc.
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            @SuppressWarnings("unchecked")
            List<SuggestedMember> suggestionsList = (List<SuggestedMember>) results.values;
            if (suggestionsList == null) {
                return;
            }

            // Clear out the existing suggestions in this adapter
            clear();

            // Add all the suggested members to this adapter
            for (SuggestedMember member : suggestionsList) {
                add(member);
            }

            notifyDataSetChanged();
        }
    }

    /**
     * This represents a single contact that is a suggestion for the user to add to a group.
     */
    // TODO: Merge this with the {@link GroupEditorFragment} Member class once we can find the
    // lookup URI for this contact using the autocomplete filter queries
    public class SuggestedMember {

        private long mRawContactId;
        private long mContactId;
        private String mDisplayName;
        private String mExtraInfo;
        private byte[] mPhoto;
        // The following lines are provided and maintained by Mediatek Inc.
        private int mSimId = -1;
        private int mSimType = -1;
        private int mSlotId = -1;
        private int mIsSdnContact = 0;
        private boolean mFixExtraInfo = false;

        // The previous lines are provided and maintained by Mediatek Inc.
        public SuggestedMember(long rawContactId, String displayName, long contactId) {
            mRawContactId = rawContactId;
            mDisplayName = displayName;
            mContactId = contactId;
        }

        // The following lines are provided and maintained by Mediatek Inc.
        public SuggestedMember(SuggestedMember member) {
            mRawContactId = member.getRawContactId();
            mDisplayName = member.getDisplayName();
            mContactId = member.getContactId();
            mPhoto = member.getPhotoByteArray();
            mSimId = member.getSimId();
            mSimType = member.getSimType();
            mSlotId = member.getSlotId();
            mIsSdnContact = member.getIsSdnContact();
        }

        // The previous lines are provided and maintained by Mediatek Inc.

        public String getDisplayName() {
            return mDisplayName;
        }

        public String getExtraInfo() {
            return mExtraInfo;
        }

        public long getRawContactId() {
            return mRawContactId;
        }

        public long getContactId() {
            return mContactId;
        }

        public byte[] getPhotoByteArray() {
            return mPhoto;
        }

        public boolean hasExtraInfo() {
            return mExtraInfo != null;
        }

        // The following lines are provided and maintained by Mediatek Inc.
        public void setRawContactId(long rawContactId) {
            mRawContactId = rawContactId;
        }
        // The previous lines are provided and maintained by Mediatek Inc.
        
        /**
         * Set a phone number or email to distinguish this contact
         */
        public void setExtraInfo(String info) {
            mExtraInfo = info;
        }

        public void setPhotoByteArray(byte[] photo) {
            mPhoto = photo;
        }

        @Override
        public String toString() {
            return getDisplayName();
        }
        
        // The following lines are provided and maintained by Mediatek Inc.
        public void setSimId(int simId) {
            mSimId = simId;
        }
        
        public void setSimType(int simType) {
            mSimType = simType;
        }
        
        public int getSimId() {
            return mSimId;
        }
        
        public int getSimType() {
            return mSimType;
        }

        public int getSlotId() {
            return mSlotId;
        }

        public void setSlotId(int slotId) {
            this.mSlotId = slotId;
        }

        public void setIsSdnContact(int isSdnContact) {
            this.mIsSdnContact = isSdnContact;
        }

        public int getIsSdnContact() {
            return this.mIsSdnContact;
        }

        public void setFixExtrasInfo(boolean flag) {
            mFixExtraInfo = flag;
        }

        public boolean hasFixedExtrasInfo() {
            return mFixExtraInfo;
        }
        // The previous lines are provided and maintained by Mediatek Inc.
    }

    // The following lines are provided and maintained by Mediatek Inc.
    private SIMInfoWrapper mSimInfoWrapper;
    // The previous lines are provided and maintained by Mediatek Inc.
}
