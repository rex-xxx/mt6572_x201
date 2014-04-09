
package com.mediatek.contacts.list;

import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.net.Uri;
import android.net.Uri.Builder;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.text.TextUtils;
import android.widget.ListView;

import com.android.contacts.list.ContactListFilter;
import com.android.contacts.list.ContactListItemView;

public class MultiEmailsPickerAdapter extends DataKindPickerBaseAdapter {

    private static final String TAG = MultiEmailsPickerAdapter.class.getSimpleName();

    static final String[] EMAILS_PROJECTION = new String[] {
            Email._ID, // 0
            Email.TYPE, // 1
            Email.LABEL, // 2
            Email.DATA, // 3
            Email.DISPLAY_NAME_PRIMARY, // 4
            Email.DISPLAY_NAME_ALTERNATIVE, // 5
            Email.CONTACT_ID, // 6
            Email.PHOTO_ID, // 7
            Email.PHONETIC_NAME, // 8
            Contacts.INDICATE_PHONE_SIM, // 9
            Contacts.IS_SDN_CONTACT,  //10
    };

    protected static final int EMAIL_ID_COLUMN_INDEX = 0;
    protected static final int EMAIL_TYPE_COLUMN_INDEX = 1;
    protected static final int EMAIL_LABEL_COLUMN_INDEX = 2;
    protected static final int EMAIL_ADDRESS_COLUMN_INDEX = 3;
    protected static final int EMAIL_PRIMARY_DISPLAY_NAME_COLUMN_INDEX = 4;
    protected static final int EMAIL_ALTERNATIVE_DISPLAY_NAME_COLUMN_INDEX = 5;
    protected static final int EMAIL_CONTACT_ID_COLUMN_INDEX = 6;
    protected static final int EMAIL_PHOTO_ID_COLUMN_INDEX = 7;
    protected static final int EMAIL_PHONETIC_NAME_COLUMN_INDEX = 8;
    protected static final int EMAIL_INDICATE_PHONE_SIM_INDEX = 9;
    protected static final int EMAIL_IS_SDN_CONTACT = 10;
    
    private CharSequence mUnknownNameText;
    private int mDisplayNameColumnIndex;
    private int mAlternativeDisplayNameColumnIndex;

    private Context mContext;

    public MultiEmailsPickerAdapter(Context context, ListView lv) {
        super(context, lv);
        mContext = context;
        mUnknownNameText = context.getText(android.R.string.unknownName);
        super.displayPhotoOnLeft();
    }

    protected CharSequence getUnknownNameText() {
        return mUnknownNameText;
    }

    @Override
    public Uri configLoaderUri(long directoryId) {

        final Builder builder;
        Uri uri = null;
        if (isSearchMode()) {
            builder = Email.CONTENT_FILTER_URI.buildUpon();
            String query = getQueryString();
            builder.appendPath(TextUtils.isEmpty(query) ? "" : query);
            builder.appendQueryParameter("non_filter_ids_arg", Email.CONTENT_URI.toString());
        } else {
            builder = Email.CONTENT_URI.buildUpon();
        }
        builder.appendQueryParameter(ContactsContract.DIRECTORY_PARAM_KEY, String
                .valueOf(directoryId));
        uri = builder.build();
        if (isSectionHeaderDisplayEnabled()) {
            uri = buildSectionIndexerUri(uri);
        }

        return uri;
    }

    @Override
    public String[] configProjection() {
        return EMAILS_PROJECTION;
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
            mDisplayNameColumnIndex = EMAIL_PRIMARY_DISPLAY_NAME_COLUMN_INDEX;
            mAlternativeDisplayNameColumnIndex = EMAIL_ALTERNATIVE_DISPLAY_NAME_COLUMN_INDEX;
        } else {
            mDisplayNameColumnIndex = EMAIL_ALTERNATIVE_DISPLAY_NAME_COLUMN_INDEX;
            mAlternativeDisplayNameColumnIndex = EMAIL_PRIMARY_DISPLAY_NAME_COLUMN_INDEX;
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
    public int getContactIDColumnIndex() {
        return EMAIL_CONTACT_ID_COLUMN_INDEX;
    }

    @Override
    public int getDataColumnIndex() {
        return EMAIL_ADDRESS_COLUMN_INDEX;
    }

    @Override
    public int getDataLabelColumnIndex() {
        return EMAIL_LABEL_COLUMN_INDEX;
    }

    @Override
    public int getDataTypeColumnIndex() {
        return EMAIL_TYPE_COLUMN_INDEX;
    }

    /**
     * Builds a {@link Data#CONTENT_URI} for the current cursor position.
     */
    @Override
    public Uri getDataUri(int position) {
        long id = ((Cursor) getItem(position)).getLong(EMAIL_ID_COLUMN_INDEX);
        return ContentUris.withAppendedId(Data.CONTENT_URI, id);
    }

    @Override
    public long getDataId(int position) {
        return ((Cursor) getItem(position)).getLong(EMAIL_ID_COLUMN_INDEX);
    }

    @Override
    public int getPhotoIDColumnIndex() {
        return EMAIL_PHOTO_ID_COLUMN_INDEX;
    }

    @Override
    public int getPhoneticNameColumnIndex() {
        return EMAIL_PHONETIC_NAME_COLUMN_INDEX;
    }

    @Override
    public int getIndicatePhoneSIMColumnIndex() {
        return EMAIL_INDICATE_PHONE_SIM_INDEX;
    }
    
    @Override
    public int getIsSdnContactColumnIndex() {
        return EMAIL_IS_SDN_CONTACT;
    }
}
