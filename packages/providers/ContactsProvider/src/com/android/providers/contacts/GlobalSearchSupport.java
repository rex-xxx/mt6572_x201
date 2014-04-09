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
 * limitations under the License
 */

package com.android.providers.contacts;

import android.app.SearchManager;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Organization;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.SearchSnippetColumns;
import android.provider.ContactsContract.StatusUpdates;
import android.telephony.TelephonyManager;
import android.provider.Telephony.SIMInfo;
import android.text.TextUtils;
import android.util.Log;

import com.android.providers.contacts.ContactsDatabaseHelper.AggregatedPresenceColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.ContactsColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.Tables;
import com.android.providers.contacts.ContactsDatabaseHelper.Views;
import com.mediatek.providers.contacts.ContactsFeatureConstants.FeatureOption;

import java.util.ArrayList;
import java.util.Locale;
import android.os.SystemProperties;
/**
 * Support for global search integration for Contacts.
 */
public class GlobalSearchSupport {

    private static final String[] SEARCH_SUGGESTIONS_COLUMNS = {
            "_id",
            SearchManager.SUGGEST_COLUMN_TEXT_1,
            SearchManager.SUGGEST_COLUMN_TEXT_2,
            SearchManager.SUGGEST_COLUMN_ICON_1,
            SearchManager.SUGGEST_COLUMN_ICON_2,
            SearchManager.SUGGEST_COLUMN_INTENT_DATA,
            SearchManager.SUGGEST_COLUMN_INTENT_ACTION,
            SearchManager.SUGGEST_COLUMN_SHORTCUT_ID,
            SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA,
            SearchManager.SUGGEST_COLUMN_LAST_ACCESS_HINT,
    };

    private static final char SNIPPET_START_MATCH = '\u0001';
    private static final char SNIPPET_END_MATCH = '\u0001';
    private static final String SNIPPET_ELLIPSIS = "\u2026";
    private static final int SNIPPET_MAX_TOKENS = 5;

    private static final String PRESENCE_SQL =
        "(SELECT " + StatusUpdates.PRESENCE +
        " FROM " + Tables.AGGREGATED_PRESENCE +
        " WHERE " + AggregatedPresenceColumns.CONTACT_ID + "=" + ContactsColumns.CONCRETE_ID + ")";

    private static class SearchSuggestion {
        long contactId;
        String photoUri;
        String lookupKey;
        int presence = -1;
        String text1;
        String text2;
        String icon1;
        String icon2;
        String intentData;
        String intentAction;
        String filter;
        String lastAccessTime;
        
        /// M: @{
        int simIndicator = -1;  // add by MTK, used for sim contacts.
        int slotId = -1;        // add by MTK, used for sim contacts.
        int isSdnContact = 0;   // add by MTK, used for sim contacts.
        /// @}
        
        @SuppressWarnings({"unchecked"})
        public ArrayList<?> asList(String[] projection) {
            if (icon1 == null) {
                if (photoUri != null) {
                    icon1 = photoUri.toString();
                } else {
                    /**
                     * M: 
                     * android default code:
                     * icon1 = String.valueOf(com.android.internal.R.drawable.ic_contact_picture);
                     * @{
                     */
                    icon1 = processIcon(slotId, isSdnContact);
                    /**
                     * @} 
                     */
                }
            }

            if (presence != -1) {
                icon2 = String.valueOf(StatusUpdates.getPresenceIconResourceId(presence));
            }
            /**
             * M:
             *   CR ID: ALPS00287211
             *   Descriptions: set icon2 null
             * @{
             */
            else {
                icon2 = null;
            }
            /**
             * @}
             */
            Log.i(TAG," icon2 : "+icon2);
            ArrayList<Object> list = new ArrayList<Object>();
            if (projection == null) {
                list.add(contactId); // _id
                list.add(text1); // text1
                list.add(text2); // text2
                list.add(icon1); // icon1
                list.add(icon2); // icon2
                list.add(intentData == null ? buildUri() : intentData); // intent data
                list.add(intentAction); // intentAction
                list.add(lookupKey); // shortcut id
                list.add(filter); // extra data
                list.add(lastAccessTime); // last access hint
            } else {
                for (int i = 0; i < projection.length; i++) {
                    addColumnValue(list, projection[i]);
                }
            }
            return list;
        }

        private void addColumnValue(ArrayList<Object> list, String column) {
            if ("_id".equals(column)) {
                list.add(contactId);
            } else if (SearchManager.SUGGEST_COLUMN_TEXT_1.equals(column)) {
                list.add(text1);
            } else if (SearchManager.SUGGEST_COLUMN_TEXT_2.equals(column)) {
                list.add(text2);
            } else if (SearchManager.SUGGEST_COLUMN_ICON_1.equals(column)) {
                list.add(icon1);
            } else if (SearchManager.SUGGEST_COLUMN_ICON_2.equals(column)) {
                list.add(icon2);
            } else if (SearchManager.SUGGEST_COLUMN_INTENT_DATA.equals(column)) {
                list.add(intentData == null ? buildUri() : intentData);
            } else if (SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID.equals(column)) {
                list.add(lookupKey);
            } else if (SearchManager.SUGGEST_COLUMN_SHORTCUT_ID.equals(column)) {
                list.add(lookupKey);
            } else if (SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA.equals(column)) {
                list.add(filter);
            } else if (SearchManager.SUGGEST_COLUMN_LAST_ACCESS_HINT.equals(column)) {
                list.add(lastAccessTime);
            } else {
                throw new IllegalArgumentException("Invalid column name: " + column);
            }
        }

        private String buildUri() {
            return Contacts.getLookupUri(contactId, lookupKey).toString();
        }

        public void reset() {
            contactId = 0;
            photoUri = null;
            lookupKey = null;
            presence = -1;
            text1 = null;
            text2 = null;
            icon1 = null;
            icon2 = null;
            intentData = null;
            intentAction = null;
            filter = null;
            lastAccessTime = null;
        }

      /// M: Code added by Mediatek inc. @{
        //Slot 0,1 contact icon
        private String mIcon1 = null;
        private String mIcon2 = null;
        //Slot 0,1 SDN contact icon
        private String mIcon3 = null;
        private String mIcon4 = null;
        private String processIcon(int slotId, int isSdnContact) {
            String icon = null;
            Log.i(TAG, "slotId is " + slotId);

            /*
             * Change Feature by Mediatek Begin. Original Android's code: xxx CR ID:
             * ALPS00269801 Descriptions:
             */

            if (FeatureOption.MTK_GEMINI_SUPPORT && slotId == 0 && mIcon1 != null && (isSdnContact < 1)) {
                return mIcon1;
            } else if (FeatureOption.MTK_GEMINI_SUPPORT && slotId == 1 && mIcon2 != null
                    && (isSdnContact < 1)) {
                return mIcon2;
            } else if (FeatureOption.MTK_GEMINI_SUPPORT && slotId == 0 && mIcon3 != null
                    && (isSdnContact > 0)) {
                return mIcon3;
            } else if (FeatureOption.MTK_GEMINI_SUPPORT && slotId == 1 && mIcon4 != null
                    && (isSdnContact > 0)) {
                return mIcon4;
            }
            /*
             * Change Feature by Mediatek End.
             */
            if (slotId >= 0) {
                /*
                 * Change Feature by Mediatek Begin. CR ID: ALPS00248311
                 * Descriptions: add CU feature
                 */
                Log.i(TAG, "[processIcon] mSlot = " + slotId);

                int i = -1;

                long beforInfor = System.currentTimeMillis();
                SIMInfo simInfo = SIMInfo.getSIMInfoBySlot(mContext, slotId);
                long afterInfor = System.currentTimeMillis();
                Log.i(TAG, "beforInfor : " + beforInfor + " | afterInfor : " + afterInfor
                        + " | TIME : " + (afterInfor - beforInfor));
                if (simInfo != null) {
                    i = simInfo.mColor;
                }
                Log.i(TAG, "[processIcon] i = " + i);

                /*
                 * Change Feature by Mediatek Begin. Original Android's code:
                 * xxx CR ID: ALPS00269801 Descriptions:
                 */
                if (isSdnContact > 0) {
                    if (i == 0) {
                        icon = String.valueOf(R.drawable.ic_contact_picture_sdn_contact_blue);
                    } else if (i == 1) {
                        icon = String.valueOf(R.drawable.ic_contact_picture_sdn_contact_orange);
                    } else if (i == 2) {
                        icon = String.valueOf(R.drawable.ic_contact_picture_sdn_contact_green);
                    } else if (i == 3) {
                        icon = String.valueOf(R.drawable.ic_contact_picture_sdn_contact_purple);
                    } else {
                        icon = String.valueOf(R.drawable.contact_icon_sim);
                    }
                } else {
                    if (i == 0) {
                        icon = String.valueOf(R.drawable.ic_contact_picture_sim_contact_blue);
                    } else if (i == 1) {
                        icon = String.valueOf(R.drawable.ic_contact_picture_sim_contact_orange);
                    } else if (i == 2) {
                        icon = String.valueOf(R.drawable.ic_contact_picture_sim_contact_green);
                    } else if (i == 3) {
                        icon = String.valueOf(R.drawable.ic_contact_picture_sim_contact_purple);
                    } else {
                        icon = String.valueOf(R.drawable.contact_icon_sim);
                    }
                }

                if (slotId == 0 && (isSdnContact < 1)) {
                    mIcon1 = icon;
                } else if (slotId == 1 && (isSdnContact < 1)) {
                    mIcon2 = icon;
                } else if (slotId == 0 && (isSdnContact > 0)) {
                    mIcon3 = icon;
                } else if (slotId == 1 && (isSdnContact > 0)) {
                    mIcon4 = icon;
                }

            } else {
                icon = String.valueOf(com.android.internal.R.drawable.ic_contact_picture);
            }
            return icon;
        }
      /// M: Code added by Mediatek inc. @}
    }

    private final ContactsProvider2 mContactsProvider;

    @SuppressWarnings("all")
    public GlobalSearchSupport(ContactsProvider2 contactsProvider) {
        mContactsProvider = contactsProvider;

        TelephonyManager telman = (TelephonyManager)
                mContactsProvider.getContext().getSystemService(Context.TELEPHONY_SERVICE);

        // To ensure the data column position. This is dead code if properly configured.
        if (Organization.COMPANY != Data.DATA1 || Phone.NUMBER != Data.DATA1
                || Email.DATA != Data.DATA1) {
            throw new AssertionError("Some of ContactsContract.CommonDataKinds class primary"
                    + " data is not in DATA1 column");
        }
    }

    public Cursor handleSearchSuggestionsQuery(
            SQLiteDatabase db, Uri uri, String[] projection, String limit) {
        final MatrixCursor cursor = new MatrixCursor(
                projection == null ? SEARCH_SUGGESTIONS_COLUMNS : projection);

        if (uri.getPathSegments().size() <= 1) {
            // no search term, return empty
        } else {
            String selection = null;
            String searchClause = uri.getLastPathSegment();
            addSearchSuggestionsBasedOnFilter(
                    cursor, db, projection, selection, searchClause, limit);
        }
        log("[handleSearchSuggestionsQuery]result cursor count:"
                + (cursor == null ? "NULL" : cursor.getCount()));
        return cursor;
    }

    /**
     * Returns a search suggestions cursor for the contact bearing the provided lookup key.  If the
     * lookup key cannot be found in the database, the contact name is decoded from the lookup key
     * and used to re-identify the contact.  If the contact still cannot be found, an empty cursor
     * is returned.
     *
     * <p>Note that if {@code lookupKey} is not a valid lookup key, an empty cursor is returned
     * silently.  This would occur with old-style shortcuts that were created using the contact id
     * instead of the lookup key.
     */
    public Cursor handleSearchShortcutRefresh(SQLiteDatabase db, String[] projection,
            String lookupKey, String filter) {
        log("[handleSearchShortcutRefresh]lookupKey:" + lookupKey
                + " ||filter:" + filter);
        long contactId;
        try {
            contactId = mContactsProvider.lookupContactIdByLookupKey(db, lookupKey);
        } catch (IllegalArgumentException e) {
            contactId = -1L;
        }
        log("[handleSearchShortcutRefresh]contactId:" + contactId);
        filter = filter == null ? null : (filter.equals("null") ? null : filter);
        MatrixCursor cursor = new MatrixCursor(
                projection == null ? SEARCH_SUGGESTIONS_COLUMNS : projection);
        /**
         * M:
         * Original android code:
         * return addSearchSuggestionsBasedOnFilter(cursor,
         *      db, projection, ContactsColumns.CONCRETE_ID + "=" + contactId, filter, null);
         * @{
         */
        Cursor c = addSearchSuggestionsBasedOnFilter(cursor,
                db, projection, ContactsColumns.CONCRETE_ID + "=" + contactId, filter, null);
        log("[handleSearchShortcutRefresh]Result cursor count:"
                + (c == null ? "NULL" : c.getCount()));
        if (c != null && c.getCount() == 1) {
            c.moveToFirst();
            int index = c.getColumnIndex(SearchManager.SUGGEST_COLUMN_SHORTCUT_ID);
            if (index >= 0) {
                String lookup = c.getString(index);
                log("[handleSearchShortcutRefresh]new lookupKey:" + lookup
                        + "||It is NE old:" + (lookup != null && !lookup.equals(lookupKey)));
                if (lookup != null && !lookup.equals(lookupKey)) {
                    if (c != null) {
                        c.close();
                    }
                    return new MatrixCursor(
                            projection == null ? SEARCH_SUGGESTIONS_COLUMNS
                                    : projection);
                }
            }
            c.moveToPosition(-1);
        }
        return c;
        /**
         * @}
         */
    }

    private Cursor addSearchSuggestionsBasedOnFilter(MatrixCursor cursor, SQLiteDatabase db,
            String[] projection, String selection, String filter, String limit) {
        StringBuilder sb = new StringBuilder();
        final boolean haveFilter = !TextUtils.isEmpty(filter);
        
        /**
         * M: 
         * 
         * Orignail android code: 
         * sb.append("SELECT " 
         *                 + Contacts._ID + ", " 
         *                 + Contacts.LOOKUP_KEY + ", " 
         *                 + Contacts.PHOTO_THUMBNAIL_URI + ", " 
         *                 + Contacts.DISPLAY_NAME + ", " 
         *                 + PRESENCE_SQL + " AS " + Contacts.CONTACT_PRESENCE + ", " 
         *                 + Contacts.LAST_TIME_CONTACTED);
         * @{
         */
        sb.append("SELECT "
                        + Contacts._ID + ", "
                        + Contacts.LOOKUP_KEY + ", "
                        + Contacts.PHOTO_THUMBNAIL_URI + ", "
                        + Contacts.DISPLAY_NAME + ", "
                        + PRESENCE_SQL + " AS " + Contacts.CONTACT_PRESENCE + ", "
                        + Contacts.LAST_TIME_CONTACTED + ", "
                        + Contacts.INDICATE_PHONE_SIM + ", "
                        + Contacts.IS_SDN_CONTACT
                  );
        /*
         * @}
         */
        
        if (haveFilter) {
            sb.append(", " + SearchSnippetColumns.SNIPPET);
        }
        sb.append(" FROM ");
        sb.append(Views.CONTACTS);
        sb.append(" AS contacts");
        if (haveFilter) {
            mContactsProvider.appendSearchIndexJoin(sb, filter, true,
                    String.valueOf(SNIPPET_START_MATCH), String.valueOf(SNIPPET_END_MATCH),
                    SNIPPET_ELLIPSIS, SNIPPET_MAX_TOKENS, false);
        }
        if (selection != null) {
            sb.append(" WHERE ").append(selection);
        }
        if (limit != null) {
            sb.append(" LIMIT " + limit);
        }
        /// M: @{
        startTime = System.currentTimeMillis();
        /// M: @}
        Cursor c = db.rawQuery(sb.toString(), null);
        SearchSuggestion suggestion = new SearchSuggestion();
        suggestion.filter = filter;
        try {
            while (c.moveToNext()) {
                suggestion.contactId = c.getLong(0);
                suggestion.lookupKey = c.getString(1);
                suggestion.photoUri = c.getString(2);
                suggestion.text1 = c.getString(3);
                suggestion.presence = c.isNull(4) ? -1 : c.getInt(4);
                suggestion.lastAccessTime = c.getString(5);
                
                /*
                 * M:
                 * 
                 * Orignail android code: 
                 * if (haveFilter) {
                 *     suggestion.text2 = shortenSnippet(c.getString(6)); 
                 * }
                 * 
                 * Description:
                 * Get slot id if contacts is a sim contact.
                 * @{
                 */
                suggestion.simIndicator = c.getInt(6);
                suggestion.isSdnContact = c.getInt(7);
                Context context = mContactsProvider.getContext();
                
                
                mContext =  context;
                
                
                if (context != null && suggestion.simIndicator > 0) {
                    suggestion.slotId = SIMInfo.getSlotById(context,
                            suggestion.simIndicator);
                } else {
                    suggestion.slotId = -1;
                }
                if (haveFilter) {
                    suggestion.text2 = shortenSnippet(c.getString(8));
                }
                /*
                 * @}
                 */
                
                cursor.addRow(suggestion.asList(projection));
                suggestion.reset();
            }
        } finally {
            c.close();
        }
        /// M: @{
        endTime = System.currentTimeMillis();
        Log.i(TAG,"startTime : "+startTime+" | endTime : "+endTime+" | time : "+(endTime-startTime));
        /// @}
        return cursor;
    }

    private String shortenSnippet(final String snippet) {
        if (snippet == null) {
            return null;
        }

        int from = 0;
        int to = snippet.length();
        int start = snippet.indexOf(SNIPPET_START_MATCH);
        if (start == -1) {
            return null;
        }

        int firstNl = snippet.lastIndexOf('\n', start);
        if (firstNl != -1) {
            from = firstNl + 1;
        }
        int end = snippet.lastIndexOf(SNIPPET_END_MATCH);
        if (end != -1) {
            int lastNl = snippet.indexOf('\n', end);
            if (lastNl != -1) {
                to = lastNl;
            }
        }

        StringBuilder sb = new StringBuilder();
        for (int i = from; i < to; i++) {
            char c = snippet.charAt(i);
            if (c != SNIPPET_START_MATCH && c != SNIPPET_END_MATCH) {
                sb.append(c);
            }
        }
        return sb.toString();
    }


    
    private void log(String msg) {
        if (DEBUG) {
            Log.d(TAG, msg);
        }
    }
    
    private static final String TAG = "GlobalSearchSupport";
    private static final boolean DEBUG = true;
    
    private static Context mContext;
    private long startTime;
    private long endTime;
    

    
    /// @}
}
