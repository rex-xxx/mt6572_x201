
package com.mediatek.contacts.list;

import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.net.Uri;
import android.net.Uri.Builder;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Directory;
import android.provider.ContactsContract.RawContacts;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ListView;

import com.android.contacts.list.ContactListFilter;
import com.android.contacts.list.ContactListItemView;
import com.android.contacts.list.PhoneNumberListAdapter;

import java.util.ArrayList;
import java.util.List;

public class MultiPhoneNumbersPickerAdapter extends DataKindPickerBaseAdapter {

    private static final String TAG = PhoneNumberListAdapter.class.getSimpleName();

    protected static final String[] PHONES_PROJECTION = new String[] {
            Phone._ID, // 0
            Phone.TYPE, // 1
            Phone.LABEL, // 2
            Phone.NUMBER, // 3
            Phone.DISPLAY_NAME_PRIMARY, // 4
            Phone.DISPLAY_NAME_ALTERNATIVE, // 5
            Phone.CONTACT_ID, // 6
            Phone.LOOKUP_KEY, // 7
            Phone.PHOTO_ID, // 8
            Phone.PHONETIC_NAME, // 9
            Contacts.INDICATE_PHONE_SIM, // 10
            Contacts.IS_SDN_CONTACT, //11
    };

    protected static final int PHONE_ID_COLUMN_INDEX = 0;
    protected static final int PHONE_TYPE_COLUMN_INDEX = 1;
    protected static final int PHONE_LABEL_COLUMN_INDEX = 2;
    protected static final int PHONE_NUMBER_COLUMN_INDEX = 3;
    protected static final int PHONE_PRIMARY_DISPLAY_NAME_COLUMN_INDEX = 4;
    protected static final int PHONE_ALTERNATIVE_DISPLAY_NAME_COLUMN_INDEX = 5;
    protected static final int PHONE_CONTACT_ID_COLUMN_INDEX = 6;
    protected static final int PHONE_LOOKUP_KEY_COLUMN_INDEX = 7;
    protected static final int PHONE_PHOTO_ID_COLUMN_INDEX = 8;
    protected static final int PHONE_PHONETIC_NAME_COLUMN_INDEX = 9;
    protected static final int PHONE_INDICATE_PHONE_SIM_INDEX = 10;
    protected static final int PHONE_IS_SDN_CONTACT = 11;
    
    private CharSequence mUnknownNameText;
    private int mDisplayNameColumnIndex;
    private int mAlternativeDisplayNameColumnIndex;

    private Context mContext;

    public MultiPhoneNumbersPickerAdapter(Context context, ListView lv) {
        super(context, lv);
        mContext = context;
        mUnknownNameText = context.getText(android.R.string.unknownName);
        super.displayPhotoOnLeft();
    }

    protected CharSequence getUnknownNameText() {
        return mUnknownNameText;
    }

    @Override
    protected Uri configLoaderUri(long directoryId) {
        Uri uri;

        if (directoryId != Directory.DEFAULT) {
            Log.w(TAG, "PhoneNumberListAdapter is not ready for non-default directory ID ("
                    + "directoryId: " + directoryId + ")");
        }

        if (isSearchMode()) {
            String query = getQueryString();
            Builder builder = Phone.CONTENT_FILTER_URI.buildUpon();
            if (TextUtils.isEmpty(query)) {
                builder.appendPath("");
            } else {
                builder.appendPath(query); // Builder will encode the query
            }

            builder.appendQueryParameter(ContactsContract.DIRECTORY_PARAM_KEY, String
                    .valueOf(directoryId));
            builder.appendQueryParameter("non_filter_ids_arg", Phone.CONTENT_URI.toString());
            uri = builder.build();
        } else {
            uri = Phone.CONTENT_URI.buildUpon().appendQueryParameter(
                    ContactsContract.DIRECTORY_PARAM_KEY, String.valueOf(Directory.DEFAULT))
                    .build();
            if (isSectionHeaderDisplayEnabled()) {
                uri = buildSectionIndexerUri(uri);
            }
        }

        return uri;
    }

    @Override
    protected String[] configProjection() {
        return PHONES_PROJECTION;
    }

    protected void configureSelection(CursorLoader loader, long directoryId,
            ContactListFilter filter) {
        if (filter == null || directoryId != Directory.DEFAULT) {
            return;
        }

        final StringBuilder selection = new StringBuilder();
        final List<String> selectionArgs = new ArrayList<String>();

        switch (filter.filterType) {
            case ContactListFilter.FILTER_TYPE_CUSTOM:
                selection.append(Contacts.IN_VISIBLE_GROUP + "=1");
                selection.append(" AND " + Contacts.HAS_PHONE_NUMBER + "=1");
                break;

            case ContactListFilter.FILTER_TYPE_ACCOUNT:
                selection.append("(");

                selection.append(RawContacts.ACCOUNT_TYPE + "=?" + " AND " + RawContacts.ACCOUNT_NAME + "=?");
                selectionArgs.add(filter.accountType);
                selectionArgs.add(filter.accountName);
                if (filter.dataSet != null) {
                    selection.append(" AND " + RawContacts.DATA_SET + "=?");
                    selectionArgs.add(filter.dataSet);
                } else {
                    selection.append(" AND " + RawContacts.DATA_SET + " IS NULL");
                }
                selection.append(")");
                break;

            case ContactListFilter.FILTER_TYPE_ALL_ACCOUNTS:
            case ContactListFilter.FILTER_TYPE_DEFAULT:
                // No selection needed.
                break;

            case ContactListFilter.FILTER_TYPE_WITH_PHONE_NUMBERS_ONLY:
                // This adapter is always "phone only", so no selection needed either.
                break;

            default:
                Log.w(TAG, "Unsupported filter type came " + "(type: " + filter.filterType + ", toString: " + filter + ")"
                        + " showing all contacts.");
                // No selection.
                break;
        }
        loader.setSelection(selection.toString());
        loader.setSelectionArgs(selectionArgs.toArray(new String[0]));
    }

    @Override
    public String getContactDisplayName(int position) {
        return ((Cursor) getItem(position)).getString(mDisplayNameColumnIndex);
    }

    @Override
    public void setContactNameDisplayOrder(int displayOrder) {
        super.setContactNameDisplayOrder(displayOrder);
        if (getContactNameDisplayOrder() == ContactsContract.Preferences.DISPLAY_ORDER_PRIMARY) {
            mDisplayNameColumnIndex = PHONE_PRIMARY_DISPLAY_NAME_COLUMN_INDEX;
            mAlternativeDisplayNameColumnIndex = PHONE_ALTERNATIVE_DISPLAY_NAME_COLUMN_INDEX;
        } else {
            mDisplayNameColumnIndex = PHONE_ALTERNATIVE_DISPLAY_NAME_COLUMN_INDEX;
            mAlternativeDisplayNameColumnIndex = PHONE_PRIMARY_DISPLAY_NAME_COLUMN_INDEX;
        }
    }

    @Override
    public void bindName(ContactListItemView view, Cursor cursor) {
        view.showDisplayName(cursor, mDisplayNameColumnIndex, mAlternativeDisplayNameColumnIndex);
        view.showPhoneticName(cursor, getPhoneticNameColumnIndex());
    }

    @Override
    public void bindQuickContact(ContactListItemView view, int partitionIndex, Cursor cursor) {
        return;
    }

    @Override
    public int getPhotoIDColumnIndex() {
        return PHONE_PHOTO_ID_COLUMN_INDEX;
    }

    @Override
    public int getDataColumnIndex() {
        return PHONE_NUMBER_COLUMN_INDEX;
    }

    @Override
    public int getDataLabelColumnIndex() {
        return PHONE_LABEL_COLUMN_INDEX;
    }

    @Override
    public int getDataTypeColumnIndex() {
        return PHONE_TYPE_COLUMN_INDEX;
    }

    @Override
    public int getContactIDColumnIndex() {
        return PHONE_CONTACT_ID_COLUMN_INDEX;
    }

    @Override
    public int getPhoneticNameColumnIndex() {
        return PHONE_PHONETIC_NAME_COLUMN_INDEX;
    }

    @Override
    public int getIndicatePhoneSIMColumnIndex() {
        return PHONE_INDICATE_PHONE_SIM_INDEX;
    }

    @Override
    public int getIsSdnContactColumnIndex() {
        return PHONE_IS_SDN_CONTACT;
    }
    
    /**
     * Builds a {@link Data#CONTENT_URI} for the given cursor position.
     * 
     * @return Uri for the data. may be null if the cursor is not ready.
     */
    public Uri getDataUri(int position) {
        Cursor cursor = ((Cursor) getItem(position));
        if (cursor != null) {
            long id = cursor.getLong(PHONE_ID_COLUMN_INDEX);
            return ContentUris.withAppendedId(Data.CONTENT_URI, id);
        } else {
            Log.w(TAG, "Cursor was null in getDataUri() call. Returning null instead.");
            return null;
        }
    }

    public long getDataId(int position) {
        Cursor cursor = ((Cursor) getItem(position));
        if (cursor != null) {
            return cursor.getLong(PHONE_ID_COLUMN_INDEX);
        } else {
            Log.w(TAG, "Cursor was null in getDataId() call. Returning 0 instead.");
            return 0;
        }
    }

}
