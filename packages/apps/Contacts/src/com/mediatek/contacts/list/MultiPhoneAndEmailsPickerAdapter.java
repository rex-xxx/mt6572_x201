
package com.mediatek.contacts.list;

import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.net.Uri;
import android.net.Uri.Builder;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Directory;
import android.provider.ContactsContract.SearchSnippetColumns;
import android.text.TextUtils;
import android.widget.ListView;

import com.android.contacts.list.ContactListFilter;
import com.android.contacts.list.ContactListItemView;
import com.mediatek.contacts.extension.aassne.SimUtils;

public class MultiPhoneAndEmailsPickerAdapter extends DataKindPickerBaseAdapter {

    private static final String TAG = MultiEmailsPickerAdapter.class.getSimpleName();

    public static final char SNIPPET_START_MATCH = '\u0001';
    public static final char SNIPPET_END_MATCH = '\u0001';
    public static final String SNIPPET_ELLIPSIS = "\u2026";
    public static final int SNIPPET_MAX_TOKENS = 5;

    public static final String SNIPPET_ARGS = SNIPPET_START_MATCH + "," + SNIPPET_END_MATCH + ","
            + SNIPPET_ELLIPSIS + "," + SNIPPET_MAX_TOKENS;

    public static final Uri PICK_PHONE_EMAIL_URI = Uri
            .parse("content://com.android.contacts/data/phone_email");

    public static final Uri PICK_PHONE_EMAIL_FILTER_URI = Uri.withAppendedPath(
            PICK_PHONE_EMAIL_URI, "filter");

    static final String[] PHONE_EMAIL_PROJECTION = new String[] {
            Phone._ID, // 0
            Phone.TYPE, // 1
            Phone.LABEL, // 2
            Phone.NUMBER, // 3
            Phone.DISPLAY_NAME_PRIMARY, // 4
            Phone.DISPLAY_NAME_ALTERNATIVE, // 5
            Phone.CONTACT_ID, // 6
            Phone.PHOTO_ID, // 7
            Phone.PHONETIC_NAME, // 8
            Phone.MIMETYPE, // 9
            Contacts.INDICATE_PHONE_SIM, // 10
            Contacts.IS_SDN_CONTACT, //11
    };

    protected static final int PHONE_EMAIL_ID_INDEX = 0;
    protected static final int PHONE_EMAIL_TYPE_INDEX = 1;
    protected static final int PHONE_EMAIL_LABEL_INDEX = 2;
    protected static final int PHONE_EMAIL_NUMBER_INDEX = 3;
    protected static final int PHONE_EMAIL_PRIMARY_DISPLAY_NAME_INDEX = 4;
    protected static final int PHONE_EMAIL_ALTERNATIVE_DISPLAY_NAME_INDEX = 5;
    protected static final int PHONE_EMAIL_CONTACT_ID_INDEX = 6;
    protected static final int PHONE_EMAIL_PHOTO_ID_INDEX = 7;
    protected static final int PHONE_EMAIL_PHONETIC_NAME_INDEX = 8;
    protected static final int PHONE_EMAIL_MIMETYPE_INDEX = 9;
    protected static final int PHONE_EMAIL_INDICATE_PHONE_SIM_INDEX = 10;
    protected static final int PHONE_EMAIL_IS_SDN_CONTACT = 11;
    
    private CharSequence mUnknownNameText;
    private int mDisplayNameColumnIndex;
    private int mAlternativeDisplayNameColumnIndex;

    private Context mContext;

    public MultiPhoneAndEmailsPickerAdapter(Context context, ListView lv) {
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
        final Builder builder;
        Uri uri = null;

        if (isSearchMode()) {
            String query = getQueryString();
            if (query == null) {
                query = "";
            }
            query = query.trim();
            if (TextUtils.isEmpty(query)) {
                // Regardless of the directory, we don't want anything returned,
                // so let's just send a "nothing" query to the local directory.
                builder = PICK_PHONE_EMAIL_URI.buildUpon();
            } else {
                builder = PICK_PHONE_EMAIL_FILTER_URI.buildUpon();
                builder.appendPath(query);      // Builder will encode the query
                builder.appendQueryParameter(ContactsContract.DIRECTORY_PARAM_KEY,
                        String.valueOf(directoryId));
                if (directoryId != Directory.DEFAULT && directoryId != Directory.LOCAL_INVISIBLE) {
                    builder.appendQueryParameter(ContactsContract.LIMIT_PARAM_KEY,
                            String.valueOf(getDirectoryResultLimit()));
                }
                builder.appendQueryParameter(SearchSnippetColumns.SNIPPET_ARGS_PARAM_KEY,
                        SNIPPET_ARGS);
                builder.appendQueryParameter(SearchSnippetColumns.DEFERRED_SNIPPETING_KEY, "1");
                builder.appendQueryParameter("non_filter_ids_arg", PICK_PHONE_EMAIL_URI.toString());
            }
        } else {
            builder = PICK_PHONE_EMAIL_URI.buildUpon();

            builder.appendQueryParameter(ContactsContract.DIRECTORY_PARAM_KEY, String
                    .valueOf(directoryId));
        }

        uri = builder.build();
        if (isSectionHeaderDisplayEnabled()) {
            uri = buildSectionIndexerUri(uri);
        }
        return uri;
    }

    @Override
    protected String[] configProjection() {
        return PHONE_EMAIL_PROJECTION;
    }

    @Override
    protected void configureSelection(CursorLoader loader, long directoryId,
            ContactListFilter filter) {
        return;
    }

    @Override
    public String getContactDisplayName(int position) {
        return ((Cursor) getItem(position)).getString(mDisplayNameColumnIndex);
    }

    @Override
    public void setContactNameDisplayOrder(int displayOrder) {
        super.setContactNameDisplayOrder(displayOrder);
        if (getContactNameDisplayOrder() == ContactsContract.Preferences.DISPLAY_ORDER_PRIMARY) {
            mDisplayNameColumnIndex = PHONE_EMAIL_PRIMARY_DISPLAY_NAME_INDEX;
            mAlternativeDisplayNameColumnIndex = PHONE_EMAIL_ALTERNATIVE_DISPLAY_NAME_INDEX;
        } else {
            mDisplayNameColumnIndex = PHONE_EMAIL_ALTERNATIVE_DISPLAY_NAME_INDEX;
            mAlternativeDisplayNameColumnIndex = PHONE_EMAIL_PRIMARY_DISPLAY_NAME_INDEX;
        }
    }

    @Override
    public void bindName(ContactListItemView view, Cursor cursor) {
        view.showDisplayName(cursor, mDisplayNameColumnIndex, mAlternativeDisplayNameColumnIndex);
        view.showPhoneticName(cursor, getPhoneticNameColumnIndex());
    }

    @Override
    protected void bindData(ContactListItemView view, Cursor cursor) {
        CharSequence label = null;
        if (!cursor.isNull(getDataTypeColumnIndex())) {
            final int type = cursor.getInt(getDataTypeColumnIndex());
            final String customLabel = cursor.getString(getDataLabelColumnIndex());

            // TODO cache
            final String mimeType = cursor.getString(PHONE_EMAIL_MIMETYPE_INDEX);

            if (mimeType.equals(Email.CONTENT_ITEM_TYPE)) {
                label = Email.getTypeLabel(mContext.getResources(), type, customLabel);
            } else {
                label = Phone.getTypeLabel(mContext.getResources(), type, customLabel);
            }
            /** M:AAS @ { */
            label = SimUtils.getLabelForBindData(mContext.getResources(), type, customLabel, mimeType, cursor, label);
            /** M: @ } */
        }
        view.setLabel(label);
        view.showData(cursor, getDataColumnIndex());
    }

    @Override
    public void bindQuickContact(ContactListItemView view, int partitionIndex, Cursor cursor) {
        // TODO Auto-generated method stub
        return;
    }

    @Override
    public int getContactIDColumnIndex() {
        return PHONE_EMAIL_CONTACT_ID_INDEX;
    }

    @Override
    public int getDataColumnIndex() {
        return PHONE_EMAIL_NUMBER_INDEX;
    }

    @Override
    public int getDataLabelColumnIndex() {
        return PHONE_EMAIL_LABEL_INDEX;
    }

    @Override
    public int getDataTypeColumnIndex() {
        return PHONE_EMAIL_TYPE_INDEX;
    }

    @Override
    public Uri getDataUri(int position) {
        long id = ((Cursor) getItem(position)).getLong(PHONE_EMAIL_ID_INDEX);
        return ContentUris.withAppendedId(Data.CONTENT_URI, id);
    }

    @Override
    public long getDataId(int position) {
        return ((Cursor) getItem(position)).getLong(PHONE_EMAIL_ID_INDEX);
    }

    @Override
    public int getPhoneticNameColumnIndex() {
        return PHONE_EMAIL_PHONETIC_NAME_INDEX;
    }

    @Override
    public int getPhotoIDColumnIndex() {
        return PHONE_EMAIL_PHOTO_ID_INDEX;
    }

    @Override
    public int getIndicatePhoneSIMColumnIndex() {
        return PHONE_EMAIL_INDICATE_PHONE_SIM_INDEX;
    }
    
    @Override
    public int getIsSdnContactColumnIndex() {
        return PHONE_EMAIL_IS_SDN_CONTACT;
    }

}
