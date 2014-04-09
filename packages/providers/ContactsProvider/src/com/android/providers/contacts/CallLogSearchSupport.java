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

import android.database.MatrixCursor;
import com.android.providers.contacts.ContactsDatabaseHelper.AggregatedPresenceColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.ContactsColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.DataColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.NameLookupColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.NameLookupType;
import com.android.providers.contacts.ContactsDatabaseHelper.RawContactsColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.SearchIndexColumns;
import com.android.providers.contacts.ContactsDatabaseHelper.Tables;
import com.android.providers.contacts.ContactsDatabaseHelper.Views;
import com.android.providers.contacts.SearchIndexManager.FtsQueryBuilder;

import android.R.integer;
import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.StatusUpdates;
import android.provider.Telephony.SIMInfo;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.format.Time;
import android.text.style.StyleSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import com.android.internal.telephony.Phone;
import com.mediatek.providers.contacts.ContactsFeatureConstants.FeatureOption;


/**
 * Support for global search integration for Contacts.
 */
public class CallLogSearchSupport {
    private static final String TAG = "CallLogSearchSupport";

    private static Context mContext;

    private static final String[] SEARCH_SUGGESTIONS_BASED_ON_NAME_COLUMNS = {
            "_id", SearchManager.SUGGEST_COLUMN_TEXT_1, SearchManager.SUGGEST_COLUMN_TEXT_2,
            SearchManager.SUGGEST_COLUMN_ICON_1, SearchManager.SUGGEST_COLUMN_ICON_2,
            SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID, SearchManager.SUGGEST_COLUMN_SHORTCUT_ID
    };

    private interface SearchSuggestionQuery {
        final String SNIPPET_CONTACT_ID = "snippet_contact_id";

        public static final String TABLE = Tables.CALLS + " LEFT JOIN " + Views.DATA + " ON ("
                + Views.DATA + "." + RawContacts._ID + "=" + Tables.CALLS + "." + Calls.DATA_ID
                + ") ";

        public static final String SEARCH_INDEX_JOIN = " LEFT JOIN (" + " SELECT "
                + SearchIndexColumns.CONTACT_ID + " AS " + SNIPPET_CONTACT_ID + " FROM "
                + Tables.SEARCH_INDEX + " WHERE " + SearchIndexColumns.NAME + " MATCH '*?*') "
                + " ON (" + SNIPPET_CONTACT_ID + "=" + Views.DATA + "." + Data.CONTACT_ID + ")";

        public static final String[] COLUMNS = {
                Tables.CALLS + "." + Calls._ID + " as " + Calls._ID,
                Tables.CALLS + "." + Calls.NUMBER + " as " + Calls.NUMBER,
                Tables.CALLS + "." + Calls.DATE + " as " + Calls.DATE,
                Tables.CALLS + "." + Calls.TYPE + " as " + Calls.TYPE,
                Tables.CALLS + "." + Calls.CACHED_NUMBER_TYPE + " as " + Calls.CACHED_NUMBER_TYPE,
                Views.DATA + "." + RawContacts.DISPLAY_NAME_PRIMARY + " as display_name",
                Tables.CALLS + "." + Calls.RAW_CONTACT_ID + " as raw_contact_id",
                Tables.CALLS + "." + Calls.VTCALL + " as " + Calls.VTCALL,
                Views.DATA + "." + RawContacts.INDICATE_PHONE_SIM + " as indicate_phone_sim",
                Views.DATA + "." + RawContacts.IS_SDN_CONTACT + " as is_sdn_contact",
                Views.DATA + "." + Data.PHOTO_URI + " as photo_uri"

        };

        public static final int CALLS_ID = 0;
        public static final int CALLS_NUMBER = 1;
        public static final int CALLS_DATE = 2;
        public static final int CALLS_TYPE = 3;
        public static final int CALLS_NUMBER_TYPE = 4;
        public static final int DISPLAY_NAME = 5;
        public static final int RAW_CONTACT_ID = 6;
        public static final int VT_CALL = 7;
        public static final int INDICATE_PHONE_SIM = 8;
        public static final int IS_SDN_CONTACT = 9;
        public static final int PHOTO_URI = 10;
    }

    private static class SearchSuggestion {
        long callsId;
        String number;
        long date;
        int type;
        String callNumberLabel;
        String sortKey;
        int isVTCall;
        boolean processed;
        String text1;
        String text2;
        String icon1;
        String icon2;
        int slotId = -1; // add by MTK, used for sim contacts.
        int isSdnContact = 0; // add by MTK, used for sdn contacts.
        int calls_raw_contacts_id;
        String photo_uri;

        public SearchSuggestion(long callsId) {
            this.callsId = callsId;
        }

        private void process() {
            if (processed) {
                return;
            }
            Log.i(TAG, "photo_uri : " + photo_uri);
            if (null != photo_uri) {
                icon1 = photo_uri.toString();
            } else if (calls_raw_contacts_id == 0) {
                icon1 = String.valueOf(com.android.internal.R.drawable.ic_contact_picture);
            } else {
                icon1 = getIcon(slotId, isSdnContact);
            }
            // Set the icon
            switch (type) {
                case Calls.INCOMING_TYPE:
                    if (isVTCall == 0) {
                        icon2 = String.valueOf(R.drawable.incoming_search);
                    } else {
                        icon2 = String.valueOf(R.drawable.video_incoming_search);
                    }
                    break;
                case Calls.OUTGOING_TYPE:
                    if (isVTCall == 0) {
                        icon2 = String.valueOf(R.drawable.outing_search);
                    } else {
                        icon2 = String.valueOf(R.drawable.video_outing_search);
                    }
                    break;
                case Calls.MISSED_TYPE:
                    if (isVTCall == 0) {
                        icon2 = String.valueOf(R.drawable.missed_search);
                    } else {
                        icon2 = String.valueOf(R.drawable.video_missed_search);
                    }
                    break;
            }
            processed = true;
        }

        /**
         * Returns key for sorting search suggestions.
         * <p>
         * TODO: switch to new sort key
         */
        public String getSortKey() {
            if (sortKey == null) {
                process();
                sortKey = Long.toString(date);
            }
            return sortKey;
        }

        @SuppressWarnings( {
            "unchecked"
        })
        public ArrayList asList(String[] projection) {
            process();

            ArrayList<Object> list = new ArrayList<Object>();
            if (projection == null) {
                list.add(callsId);
                list.add(text1);
                list.add(text2);
                list.add(icon1);
                list.add(icon2);
                list.add(callsId);
                list.add(callsId);
            } else {
                for (int i = 0; i < projection.length; i++) {
                    addColumnValue(list, projection[i]);
                }
            }
            return list;
        }

        private void addColumnValue(ArrayList<Object> list, String column) {
            if ("_id".equals(column)) {
                list.add(callsId);
            } else if (SearchManager.SUGGEST_COLUMN_TEXT_1.equals(column)) {
                list.add(text1);
            } else if (SearchManager.SUGGEST_COLUMN_TEXT_2.equals(column)) {
                list.add(text2);
            } else if (SearchManager.SUGGEST_COLUMN_ICON_1.equals(column)) {
                list.add(icon1);
            } else if (SearchManager.SUGGEST_COLUMN_ICON_2.equals(column)) {
                list.add(icon2);
            } else if (SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID.equals(column)) {
                list.add(callsId);
            } else if (SearchManager.SUGGEST_COLUMN_SHORTCUT_ID.equals(column)) {
                list.add(callsId);
            } else {
                throw new IllegalArgumentException("Invalid column name: " + column);
            }
        }

        private String mIcon01 = null;
        private String mIcon02 = null;

        // Slot 0,1 SDN contact icon
        private String mIcon03 = null;
        private String mIcon04 = null;

        private String getIcon(int slotId, int isSdnContact) {
            String icon = null;
            Log.i(TAG, "slotId is " + slotId);

            if (FeatureOption.MTK_GEMINI_SUPPORT && slotId == 0 && mIcon01 != null
                    && (isSdnContact < 1)) {
                return mIcon01;
            } else if (FeatureOption.MTK_GEMINI_SUPPORT && slotId == 1 && mIcon02 != null
                    && (isSdnContact < 1)) {
                return mIcon02;
            } else if (FeatureOption.MTK_GEMINI_SUPPORT && slotId == 0 && mIcon03 != null
                    && (isSdnContact > 0)) {
                return mIcon03;
            } else if (FeatureOption.MTK_GEMINI_SUPPORT && slotId == 1 && mIcon04 != null
                    && (isSdnContact > 0)) {
                return mIcon04;
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
                    mIcon01 = icon;
                } else if (slotId == 1 && (isSdnContact < 1)) {
                    mIcon02 = icon;
                } else if (slotId == 0 && (isSdnContact > 0)) {
                    mIcon03 = icon;
                } else if (slotId == 1 && (isSdnContact > 0)) {
                    mIcon04 = icon;
                }

            } else {
                icon = String.valueOf(com.android.internal.R.drawable.ic_contact_picture);
            }
            return icon;
        }
    }

    private final ContentProvider mContactsProvider;

    @SuppressWarnings("all")
    public CallLogSearchSupport(ContentProvider contactsProvider) {
        mContactsProvider = contactsProvider;
    }

    public Cursor handleSearchSuggestionsQuery(SQLiteDatabase db, Uri uri, String limit) {
        if (uri.getPathSegments().size() <= 1) {
            return null;
        }

        final String searchClause = uri.getLastPathSegment();
        return buildCursorForSearchSuggestions(db, searchClause, limit);
    }

    private Cursor buildCursorForSearchSuggestions(SQLiteDatabase db, String filter, String limit) {
        return buildCursorForSearchSuggestions(db, null, null, filter, limit);
    }

    private Cursor buildCursorForSearchSuggestions(SQLiteDatabase db, String[] projection,
            String selection, String filter, String limit) {
        ArrayList<SearchSuggestion> suggestionList = new ArrayList<SearchSuggestion>();
        HashMap<Long, SearchSuggestion> suggestionMap = new HashMap<Long, SearchSuggestion>();

        final boolean haveFilter = !TextUtils.isEmpty(filter);

        String table = SearchSuggestionQuery.TABLE;
        if (haveFilter) {
            String nomalizeName = NameNormalizer.normalize(filter);
            table += SearchSuggestionQuery.SEARCH_INDEX_JOIN.replace("?", nomalizeName);
        }

        String where = null;
        if (!TextUtils.isEmpty(selection) && !"null".equals(selection)) {
            where = selection;
        }
        if (haveFilter) {
            if (TextUtils.isEmpty(where)) {
                where = Tables.CALLS + "." + Calls.NUMBER + " GLOB '*" + filter + "*' " + "OR ("
                        + SearchSuggestionQuery.SNIPPET_CONTACT_ID + ">0 AND " + Tables.CALLS + "."
                        + Calls.RAW_CONTACT_ID + ">0)";
            } else {
                where += " AND " + Tables.CALLS + "." + Calls.NUMBER + " GLOB '*" + filter + "*' "
                        + "OR (" + SearchSuggestionQuery.SNIPPET_CONTACT_ID + ">0 AND "
                        + Tables.CALLS + "." + Calls.RAW_CONTACT_ID + ">0)";
            }
        }

        Cursor c = db.query(false, table, SearchSuggestionQuery.COLUMNS, where, null, Calls._ID,
                null, null, limit);
        Log.d(TAG, "[buildCursorForSearchSuggestions] where = " + where + "; Count =" + c.getCount());
        try {
            while (c.moveToNext()) {
                Context context = mContactsProvider.getContext();
                mContext = context;
                long callsId = c.getLong(SearchSuggestionQuery.CALLS_ID);
                SearchSuggestion suggestion = suggestionMap.get(callsId);
                if (suggestion == null) {
                    suggestion = new SearchSuggestion(callsId);
                    suggestionMap.put(callsId, suggestion);
                }
                suggestion.calls_raw_contacts_id = c.getInt(SearchSuggestionQuery.RAW_CONTACT_ID);
                suggestion.date = c.getLong(SearchSuggestionQuery.CALLS_DATE);
                Time time = new Time();
                time.set(suggestion.date);
                String number = c.getString(SearchSuggestionQuery.CALLS_NUMBER);
                if (suggestion.calls_raw_contacts_id == 0) {
                    Resources r = mContactsProvider.getContext().getResources();
                    suggestion.text1 = r.getString(R.string.unknown);
                    suggestion.text2 = number;
                } else {
                    suggestion.text1 = c.getString(SearchSuggestionQuery.DISPLAY_NAME);
                    String type = c.getString(SearchSuggestionQuery.CALLS_NUMBER_TYPE);
                    String label = (String) CommonDataKinds.Phone.getTypeLabel(mContext
                            .getResources(), (TextUtils.isEmpty(type) ? -1 : Integer.parseInt(type)), null);
                    suggestion.text2 = label + " " + number;
                }
                suggestion.type = c.getInt(SearchSuggestionQuery.CALLS_TYPE);
                suggestion.isVTCall = c.getInt(SearchSuggestionQuery.VT_CALL);
                int indicate_phone_sim = c.getInt(SearchSuggestionQuery.INDICATE_PHONE_SIM);
                suggestion.slotId = SIMInfo.getSlotById(mContext, indicate_phone_sim);
                suggestion.isSdnContact = c.getInt(SearchSuggestionQuery.IS_SDN_CONTACT);
                suggestion.photo_uri = c.getString(SearchSuggestionQuery.PHOTO_URI);
                suggestionList.add(suggestion);
            }
        } catch (Exception e) {
            Log.e(TAG, "[buildCursorForSearchSuggestions] catched exception !!!");
            e.printStackTrace();
        } finally {
            c.close();
        }

        Collections.sort(suggestionList, new Comparator<SearchSuggestion>() {
            public int compare(SearchSuggestion row1, SearchSuggestion row2) {
                return row1.getSortKey().compareTo(row2.getSortKey());
            }
        });

        MatrixCursor retCursor = new MatrixCursor(projection != null ? projection
                : SEARCH_SUGGESTIONS_BASED_ON_NAME_COLUMNS);

        for (int i = 0; i < suggestionList.size(); i++) {
            retCursor.addRow(suggestionList.get(i).asList(projection));
        }
        return retCursor;
    }

    public Cursor handleSearchShortcutRefresh(SQLiteDatabase db, String[] projection,
            String callId, String filter) {

        return buildCursorForSearchSuggestions(db, null, Tables.CALLS + "." + Calls._ID + "="
                + callId, null, null);
    }
}
