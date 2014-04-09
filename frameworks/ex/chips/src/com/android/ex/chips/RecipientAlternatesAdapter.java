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

package com.android.ex.chips;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.text.util.Rfc822Token;
import android.text.util.Rfc822Tokenizer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.ex.chips.Queries.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/// M:
import android.database.MergeCursor;
import android.util.Patterns;
import android.provider.ContactsContract.PhoneLookup;
import android.net.Uri;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.telephony.PhoneNumberUtils;


/**
 * RecipientAlternatesAdapter backs the RecipientEditTextView for managing contacts
 * queried by email or by phone number.
 */
public class RecipientAlternatesAdapter extends CursorAdapter {
    static final int MAX_LOOKUPS = 100; /// M: Let chips to be parsed can be up to 100
    private final LayoutInflater mLayoutInflater;

    private final long mCurrentId;

    private int mCheckedItemPosition = -1;

    private OnCheckedItemChangedListener mCheckedItemChangedListener;

    private static final String TAG = "RecipAlternates";

    public static final int QUERY_TYPE_EMAIL = 0;
    public static final int QUERY_TYPE_PHONE = 1;
    
    /// M: Type to distinguish email and phone address.@{
    private static final int TYPE_EMAIL = 1;
    private static final int TYPE_PHONE = 2;
    /// M: }@
    
    private Query mQuery;

    public static HashMap<String, RecipientEntry> getMatchingRecipients(Context context,
            ArrayList<String> inAddresses) {
        return getMatchingRecipients(context, inAddresses, QUERY_TYPE_EMAIL);
    }
    
    /*
     * M: This method used for judge whether given address is an Email address.
     */
    private static boolean isEmailType(String address) {
        if (address != null && address.contains("@")) {
            return true;
        }
        return false;
    }

    /*
     * M: This method used for split the addresses to Email and Phone addresses.
     */
    private static void splitAddressToEmailAndPhone(ArrayList<String> inAddresses, ArrayList<String> emailAddresses,
            ArrayList<String> phoneAddresses, int[] index) {
        final int addressSize = Math.min(MAX_LOOKUPS, inAddresses.size());
        for (int i = 0; i < addressSize; i++) {
            if (isEmailType(inAddresses.get(i))) {
                emailAddresses.add(inAddresses.get(i));
                index[i] = TYPE_EMAIL;
            } else {
                phoneAddresses.add(inAddresses.get(i));
                index[i] = TYPE_PHONE;
            }
        }
    }
    
    /**
     * Get a HashMap of address to RecipientEntry that contains all contact
     * information for a contact with the provided address, if one exists. This
     * may block the UI, so run it in an async task.
     *
     * @param context Context.
     * @param inAddresses Array of addresses on which to perform the lookup.
     * @return HashMap<String,RecipientEntry>
     */
    public static HashMap<String, RecipientEntry> getMatchingRecipients(Context context,
            ArrayList<String> inAddresses, int addressType) {
        /// M: We have splitted some function to special method for reuse.@ {
        final int addressesSize = Math.min(MAX_LOOKUPS, inAddresses.size());
        ArrayList<String> emailAddressesList = new ArrayList<String>();
        ArrayList<String> phoneAddressesList = new ArrayList<String>();
        int[] addressTypeIndex = new int[addressesSize];
        
        splitAddressToEmailAndPhone(inAddresses, emailAddressesList, phoneAddressesList, addressTypeIndex);

        HashMap<String, RecipientEntry> recipientEntries = new HashMap<String, RecipientEntry>();

        Cursor cEmail = queryAddressData(context, emailAddressesList, QUERY_TYPE_EMAIL);
        Cursor cPhone = queryAddressData(context, phoneAddressesList, QUERY_TYPE_PHONE);
        
        if (cEmail != null && cPhone == null) {
            fillRecipientEntries(cEmail, recipientEntries);
        } else if (cEmail == null && cPhone != null) {
            fillRecipientEntries(cPhone, recipientEntries);
        } else if (cEmail != null && cPhone != null) {
            fillRecipientEntriesCompound(cEmail, cPhone, recipientEntries, addressesSize, addressTypeIndex);
        }
        /// M: }@
        return recipientEntries;
    }
    
    /*
     * M: This method used for queryData from database of email or phone addresses.
     */
    private static Cursor queryAddressData(Context context, ArrayList<String> addressesList,  int addressType) {
        final int addressesSize = Math.min(MAX_LOOKUPS, addressesList.size());
        
        StringBuilder bindString = new StringBuilder();
        String[] addresses = new String[addressesSize];

        Queries.Query query;
        if (addressType == QUERY_TYPE_EMAIL) {
            query = Queries.EMAIL;
        } else {
            query = Queries.PHONE;
        }
        
        // Create the "?" string and set up arguments.
        String queryStr = ""; /// M: For query phone number with (,),-.
        if (addressType == QUERY_TYPE_EMAIL) {
            for (int i = 0; i < addressesSize; i++) {
                Rfc822Token[] tokens = Rfc822Tokenizer.tokenize(addressesList.get(i));
                addresses[i] = (tokens.length > 0 ? tokens[0].getAddress() : addressesList.get(i));
                bindString.append("?");
                if (i < addressesSize - 1) {
                    bindString.append(",");
                }
            }
        } else {
            /// M: For query phone number with (,),-. @{
            String phoneStr = "";
            for (int i = 0; i < addressesSize; i++) {
                phoneStr = addressesList.get(i);
                /// M: Support recognizing two kinds of separator. @{
                char[] seperatorArr = {',', ';'};
                int indexOfSeparator = -1;
                int cnt = 0;
                while ((indexOfSeparator == -1) && (cnt <= seperatorArr.length)) {
                    indexOfSeparator = phoneStr.indexOf(seperatorArr[cnt]);
                    cnt++;
                }
                if (indexOfSeparator != -1) {
                    phoneStr = phoneStr.substring(0, indexOfSeparator); /// M: Get string before separator
                } else {
                    continue;
                }
                /// @}
                if (!Patterns.PHONE.matcher(phoneStr).matches()) {
                    Rfc822Token[] tokens = Rfc822Tokenizer.tokenize(phoneStr);
                    phoneStr = (tokens.length > 0 ? tokens[0].getAddress() : phoneStr);
                }
                queryStr += "\""+phoneStr+"\"";
                bindString.append("?");
                if (i < addressesSize - 1) {
                    queryStr += ",";
                    bindString.append(",");
                }
            }
            /// @}
        }

        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Doing reverse lookup for " + addresses.toString());
        }

        Cursor cursor = null;
        if (addressesList.size() > 0) {
            if (addressType == QUERY_TYPE_EMAIL) {
            cursor = context.getContentResolver().query(
                    query.getContentUri(),
                    query.getProjection(),
                    query.getProjection()[Queries.Query.DESTINATION] + " IN (" + bindString.toString()
                        + ")", addresses, null);
            } else {
                /// M: For query phone number with (,),-. @{
                cursor = context.getContentResolver().query(
                        query.getContentUri(),
                        query.getProjection(),
                        query.getProjection()[Queries.Query.DESTINATION] + " IN (" + queryStr
                            + ")", null, null);
                /// @}
            }
        }

        return cursor;
    }
    
    /*
     * M: This method used for fill RecipientEntries with single type addresses.
     */
    private static void fillRecipientEntries(Cursor cursor, HashMap<String, RecipientEntry> recipientEntries) {
        try {
            if (cursor.moveToFirst()) {
                do {
                    String address = cursor.getString(Queries.Query.DESTINATION);
                    recipientEntries.put(address, RecipientEntry.constructTopLevelEntry(
                            cursor.getString(Queries.Query.NAME),
                            cursor.getInt(Queries.Query.DISPLAY_NAME_SOURCE),
                            cursor.getString(Queries.Query.DESTINATION),
                            cursor.getInt(Queries.Query.DESTINATION_TYPE),
                            cursor.getString(Queries.Query.DESTINATION_LABEL),
                            cursor.getLong(Queries.Query.CONTACT_ID),
                            cursor.getLong(Queries.Query.DATA_ID),
                            cursor.getString(Queries.Query.PHOTO_THUMBNAIL_URI)));
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "Received reverse look up information for " + address
                                + " RESULTS: "
                                + " NAME : " + cursor.getString(Queries.Query.NAME)
                                + " CONTACT ID : " + cursor.getLong(Queries.Query.CONTACT_ID)
                                + " ADDRESS :" + cursor.getString(Queries.Query.DESTINATION));
                    }
                } while (cursor.moveToNext());
            }
        } finally {
            cursor.close();
        }
    }
    
    /*
     * M: This method used for fill RecipientEntries with mult-type addresses.
     */
    private static void fillRecipientEntriesCompound(Cursor cEmail, Cursor cPhone, 
            HashMap<String, RecipientEntry> recipientEntries, int addressesSize, int[] addressTypeIndex) {
        //merge two list in one
        try {
            cEmail.moveToFirst();
            cPhone.moveToFirst();
            boolean shouldQueryEmail = true;
            boolean shouldQueryPhone = true;
            for (int i = 0; i < addressesSize; i++) {
                if (addressTypeIndex[i] == TYPE_EMAIL && shouldQueryEmail && cEmail.getCount() != 0) {
                    String address = cEmail.getString(Queries.Query.DESTINATION);
                    recipientEntries.put(address, RecipientEntry.constructTopLevelEntry(
                            cEmail.getString(Queries.Query.NAME),
                            cEmail.getInt(Queries.Query.DISPLAY_NAME_SOURCE),
                            cEmail.getString(Queries.Query.DESTINATION),
                            cEmail.getInt(Queries.Query.DESTINATION_TYPE),
                            cEmail.getString(Queries.Query.DESTINATION_LABEL),
                            cEmail.getLong(Queries.Query.CONTACT_ID),
                            cEmail.getLong(Queries.Query.DATA_ID),
                            cEmail.getString(Queries.Query.PHOTO_THUMBNAIL_URI)));
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "Received reverse look up information for " + address
                                + " RESULTS: "
                                + " NAME : " + cEmail.getString(Queries.Query.NAME)
                                + " CONTACT ID : " + cEmail.getLong(Queries.Query.CONTACT_ID)
                                + " ADDRESS :" + cEmail.getString(Queries.Query.DESTINATION));
                    }
                    shouldQueryEmail = cEmail.moveToNext();
                } else {
                    if (shouldQueryPhone && cPhone.getCount() != 0){
                        String address = cPhone.getString(Queries.Query.DESTINATION);
                        recipientEntries.put(address, RecipientEntry.constructTopLevelEntry(
                                cPhone.getString(Queries.Query.NAME),
                                cPhone.getInt(Queries.Query.DISPLAY_NAME_SOURCE),
                                cPhone.getString(Queries.Query.DESTINATION),
                                cPhone.getInt(Queries.Query.DESTINATION_TYPE),
                                cPhone.getString(Queries.Query.DESTINATION_LABEL),
                                cPhone.getLong(Queries.Query.CONTACT_ID),
                                cPhone.getLong(Queries.Query.DATA_ID),
                                cPhone.getString(Queries.Query.PHOTO_THUMBNAIL_URI)));
                        if (Log.isLoggable(TAG, Log.DEBUG)) {
                            Log.d(TAG, "Received reverse look up information for " + address
                                    + " RESULTS: "
                                    + " NAME : " + cPhone.getString(Queries.Query.NAME)
                                    + " CONTACT ID : " + cPhone.getLong(Queries.Query.CONTACT_ID)
                                    + " ADDRESS :" + cPhone.getString(Queries.Query.DESTINATION));
                        }
                        shouldQueryPhone = cPhone.moveToNext();
                    }
                }
            }
        } finally {
            cEmail.close();
            cPhone.close();
        }
    }

    public RecipientAlternatesAdapter(Context context, long contactId, long currentId, int viewId,
            OnCheckedItemChangedListener listener) {
        this(context, contactId, currentId, viewId, QUERY_TYPE_EMAIL, listener);
    }

    public RecipientAlternatesAdapter(Context context, long contactId, long currentId, int viewId,
            int queryMode, OnCheckedItemChangedListener listener) {
        super(context, getCursorForConstruction(context, contactId, queryMode), 0);
        mLayoutInflater = LayoutInflater.from(context);
        mCurrentId = currentId;
        mCheckedItemChangedListener = listener;

        if (queryMode == QUERY_TYPE_EMAIL) {
            mQuery = Queries.EMAIL;
        } else if (queryMode == QUERY_TYPE_PHONE) {
            mQuery = Queries.PHONE;
        } else {
            mQuery = Queries.EMAIL;
            Log.e(TAG, "Unsupported query type: " + queryMode);
        }
    }

    private static Cursor getCursorForConstruction(Context context, long contactId, int queryType) {
        final Cursor cursor;
        if (queryType == QUERY_TYPE_EMAIL) {
            cursor = context.getContentResolver().query(
                    Queries.EMAIL.getContentUri(),
                    Queries.EMAIL.getProjection(),
                    Queries.EMAIL.getProjection()[Queries.Query.CONTACT_ID] + " =?", new String[] {
                        String.valueOf(contactId)
                    }, null);
        } else {
            cursor = context.getContentResolver().query(
                    Queries.PHONE.getContentUri(),
                    Queries.PHONE.getProjection(),
                    Queries.PHONE.getProjection()[Queries.Query.CONTACT_ID] + " =?", new String[] {
                        String.valueOf(contactId)
                    }, null);
        }
        /// M: Close cursor in case of cursor leak
        final Cursor resultCursor = removeDuplicateDestinations(cursor);
        cursor.close();
        return resultCursor;
    }

    /**
     * @return a new cursor based on the given cursor with all duplicate destinations removed.
     *
     * It's only intended to use for the alternate list, so...
     * - This method ignores all other fields and dedupe solely on the destination.  Normally,
     * if a cursor contains multiple contacts and they have the same destination, we'd still want
     * to show both.
     * - This method creates a MatrixCursor, so all data will be kept in memory.  We wouldn't want
     * to do this if the original cursor is large, but it's okay here because the alternate list
     * won't be that big.
     */
    // Visible for testing
    /* package */ static Cursor removeDuplicateDestinations(Cursor original) {
        final MatrixCursor result = new MatrixCursor(
                original.getColumnNames(), original.getCount());
        final HashSet<String> destinationsSeen = new HashSet<String>();

        original.moveToPosition(-1);
        while (original.moveToNext()) {
            final String destination = original.getString(Query.DESTINATION);
            if (destinationsSeen.contains(destination)) {
                continue;
            }
            destinationsSeen.add(destination);

            result.addRow(new Object[] {
                    original.getString(Query.NAME),
                    original.getString(Query.DESTINATION),
                    original.getInt(Query.DESTINATION_TYPE),
                    original.getString(Query.DESTINATION_LABEL),
                    original.getLong(Query.CONTACT_ID),
                    original.getLong(Query.DATA_ID),
                    original.getString(Query.PHOTO_THUMBNAIL_URI),
                    original.getInt(Query.DISPLAY_NAME_SOURCE)
                    });
        }

        return result;
    }

    @Override
    public long getItemId(int position) {
        Cursor c = getCursor();
        if (c.moveToPosition(position)) {
            c.getLong(Queries.Query.DATA_ID);
        }
        return -1;
    }

    public RecipientEntry getRecipientEntry(int position) {
        Cursor c = getCursor();
        c.moveToPosition(position);
        return RecipientEntry.constructTopLevelEntry(
                c.getString(Queries.Query.NAME),
                c.getInt(Queries.Query.DISPLAY_NAME_SOURCE),
                c.getString(Queries.Query.DESTINATION),
                c.getInt(Queries.Query.DESTINATION_TYPE),
                c.getString(Queries.Query.DESTINATION_LABEL),
                c.getLong(Queries.Query.CONTACT_ID),
                c.getLong(Queries.Query.DATA_ID),
                c.getString(Queries.Query.PHOTO_THUMBNAIL_URI));
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Cursor cursor = getCursor();
        cursor.moveToPosition(position);
        if (convertView == null) {
            convertView = newView();
        }
        if (cursor.getLong(Queries.Query.DATA_ID) == mCurrentId) {
            mCheckedItemPosition = position;
            if (mCheckedItemChangedListener != null) {
                mCheckedItemChangedListener.onCheckedItemChanged(mCheckedItemPosition);
            }
        }
        bindView(convertView, convertView.getContext(), cursor);
        return convertView;
    }

    // TODO: this is VERY similar to the BaseRecipientAdapter. Can we combine
    // somehow?
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        int position = cursor.getPosition();

        TextView display = (TextView) view.findViewById(android.R.id.title);
        ImageView imageView = (ImageView) view.findViewById(android.R.id.icon);
        RecipientEntry entry = getRecipientEntry(position);
        if (position == 0) {
            display.setText(cursor.getString(Queries.Query.NAME));
            display.setVisibility(View.VISIBLE);
            // TODO: see if this needs to be done outside the main thread
            // as it may be too slow to get immediately.
            imageView.setImageURI(entry.getPhotoThumbnailUri());
            imageView.setVisibility(View.VISIBLE);
        } else {
            display.setVisibility(View.GONE);
            imageView.setVisibility(View.GONE);
        }
        TextView destination = (TextView) view.findViewById(android.R.id.text1);
        destination.setText(cursor.getString(Queries.Query.DESTINATION));

        TextView destinationType = (TextView) view.findViewById(android.R.id.text2);
        if (destinationType != null) {
            destinationType.setText(mQuery.getTypeLabel(context.getResources(),
                    cursor.getInt(Queries.Query.DESTINATION_TYPE),
                    cursor.getString(Queries.Query.DESTINATION_LABEL)).toString().toUpperCase());
        }
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return newView();
    }

    private View newView() {
        return mLayoutInflater.inflate(R.layout.chips_recipient_dropdown_item, null);
    }

    /*package*/ static interface OnCheckedItemChangedListener {
        public void onCheckedItemChanged(int position);
    }
    
    /**
     * M: RecipientAlternatesAdapter constructor for phone query with showPhoneAndEmail flag.
     * @hide
     */
    public RecipientAlternatesAdapter(Context context, long contactId, long currentId, int viewId,
            int queryMode, OnCheckedItemChangedListener listener, boolean showPhoneAndEmail) {
        super(context, getCursorForConstruction(context, contactId, queryMode, showPhoneAndEmail), 0);   
        mLayoutInflater = LayoutInflater.from(context);
        mCurrentId = currentId;
        mCheckedItemChangedListener = listener;

        if (queryMode == QUERY_TYPE_EMAIL) {
            mQuery = Queries.EMAIL;
        } else if (queryMode == QUERY_TYPE_PHONE) {
            mQuery = Queries.PHONE;
        } else {
            mQuery = Queries.EMAIL;
            Log.e(TAG, "Unsupported query type: " + queryMode);
        }
    }

    /**
     * M: GetCursorForConstruction for phone query with showPhoneAndEmail flag.
     */
    private static Cursor getCursorForConstruction(Context context, long contactId, int queryType, boolean showPhoneAndEmail) {  
        final Cursor cursor;
        if (!showPhoneAndEmail) {
            cursor = context.getContentResolver().query(
                    Queries.PHONE.getContentUri(),
                    Queries.PHONE.getProjection(),
                    Queries.PHONE.getProjection()[Queries.Query.CONTACT_ID] + " =?", new String[] {
                        String.valueOf(contactId)
                    }, null);
        } else {
            /// M: Show phone number and email simutaneously when select chip
            Cursor[] cursors = new Cursor[2];
            cursors[0] = context.getContentResolver().query(
                    Queries.PHONE.getContentUri(),
                    Queries.PHONE.getProjection(),
                    Queries.PHONE.getProjection()[Queries.Query.CONTACT_ID] + " =?", new String[] {
                        String.valueOf(contactId)
                    }, null);
            cursors[1] =  context.getContentResolver().query(
                    Queries.EMAIL.getContentUri(),
                    Queries.EMAIL.getProjection(),
                    Queries.EMAIL.getProjection()[Queries.Query.CONTACT_ID] + " =?", new String[] {
                        String.valueOf(contactId)
                    }, null);
            cursor = new MergeCursor(cursors);
        }
        /// M: Close cursor in case of cursor leak
        final Cursor resultCursor = removeDuplicateDestinations(cursor);
        cursor.close();
        return resultCursor;
    }

    /**
     * M: Get RecipientEntry by giving phone number (No matter the number is normalized or not, we can still query it out).
     * @hide
     */
    public static RecipientEntry getRecipientEntryByPhoneNumber(Context context, String phoneNumber) {
        final String[] PHONE_LOOKUP_PROJECTION = new String[] {
                    Phone._ID,                      // 0
                    Phone.CONTACT_ID,               // 1
                    Phone.NUMBER,                   // 2
                    Phone.NORMALIZED_NUMBER,        // 3
                    Phone.DISPLAY_NAME,             // 4
                };
        long index = -1;
        String normalizedNumber = PhoneNumberUtils.normalizeNumber(phoneNumber);
        /// M: Query CONTACT_ID by giving phone number
        Cursor cursorNormalize = context.getContentResolver().query(
                        Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, normalizedNumber), PHONE_LOOKUP_PROJECTION, null, null, null);
        if (cursorNormalize.moveToFirst()) {
            do {
                index = cursorNormalize.getLong(1); /// M: Phone.CONTACT_ID
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "[getRecipientEntryByPhoneNumber] Query ID for " + phoneNumber
                            + " RESULTS: "
                            + " NAME : " + cursorNormalize.getString(4)
                            + " CONTACT ID : " + cursorNormalize.getLong(1)
                            + " ADDRESS :" + cursorNormalize.getString(2));
                }
            } while (cursorNormalize.moveToNext());
        }
        cursorNormalize.close();
        /// M: No matched contact
        if (index == -1) {
            return null;
        }
        /// M: Query contact information by giving CONTACT_ID
        RecipientEntry entry = null;
        Cursor cursor = context.getContentResolver().query(
                        Queries.PHONE.getContentUri(),
                        Queries.PHONE.getProjection(),
                        Queries.PHONE.getProjection()[Queries.Query.CONTACT_ID] + " IN (" + String.valueOf(index) + ")", null, null);
        if (cursor.moveToFirst()) {
            do {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "[getRecipientEntryByPhoneNumber] Query detail for " + phoneNumber
                            + " RESULTS: "
                            + " NAME : " + cursor.getString(Queries.Query.NAME)
                            + " CONTACT ID : " + cursor.getLong(Queries.Query.CONTACT_ID)
                            + " ADDRESS :" + cursor.getString(Queries.Query.DESTINATION));
                }
                String currentNumber = cursor.getString(1);  /// M:Phone.NUMBER
                if (PhoneNumberUtils.compare(PhoneNumberUtils.normalizeNumber(currentNumber), normalizedNumber)) {
                    entry = RecipientEntry.constructTopLevelEntry(
                        cursor.getString(Queries.Query.NAME),
                        cursor.getInt(Queries.Query.DISPLAY_NAME_SOURCE),
                        cursor.getString(Queries.Query.DESTINATION),
                        cursor.getInt(Queries.Query.DESTINATION_TYPE),
                        cursor.getString(Queries.Query.DESTINATION_LABEL),
                        cursor.getLong(Queries.Query.CONTACT_ID),
                        cursor.getLong(Queries.Query.DATA_ID),
                        cursor.getString(Queries.Query.PHOTO_THUMBNAIL_URI));
                    break;
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        return entry;
    }
}
