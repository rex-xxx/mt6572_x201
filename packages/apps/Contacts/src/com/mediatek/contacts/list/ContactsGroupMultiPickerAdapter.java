
package com.mediatek.contacts.list;

import android.accounts.Account;
import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.RawContacts;
import android.text.TextUtils;
import android.view.View;
import android.widget.ListView;

import com.android.contacts.list.ContactListFilter;
import com.android.contacts.list.ContactListItemView;

public class ContactsGroupMultiPickerAdapter extends MultiContactsBasePickerAdapter {

    public final String mContactsGroupSelection =
        " IN "
                + "(SELECT " + RawContacts.CONTACT_ID
                + " FROM " + "view_raw_contacts"
                + " WHERE " + "view_raw_contacts._id" + " IN "
                + "(SELECT " + "data." + Data.RAW_CONTACT_ID
                        + " FROM " + "data "
                        + "JOIN mimetypes ON (data.mimetype_id = mimetypes._id)"
                        + " WHERE " + Data.MIMETYPE + "='" + GroupMembership.CONTENT_ITEM_TYPE
                                + "' AND " + GroupMembership.GROUP_ROW_ID + " IN "
                                + "(SELECT " + "groups" + "." + Groups._ID
                                + " FROM " + "groups"
                                + " WHERE " + Groups.DELETED + "=0 AND " + Groups.TITLE + "=?))" 
                                + " AND " + RawContacts.DELETED + "=0 ";

    private String mGroupTitle;
    private Account mAccount;

    public ContactsGroupMultiPickerAdapter(Context context, ListView lv) {
        super(context, lv);
    }
    
    public void setGroupTitle(String groupTitle) {
        mGroupTitle = groupTitle;
    }
    
    public void setGroupAccount(Account account) {
            mAccount = account;
    }

    @Override
    public void configureLoader(CursorLoader loader, long directoryId) {
        super.configureLoader(loader, directoryId);

        if (isSearchMode()) {
            String query = getQueryString();
            if (query == null) {
                query = "";
            }
            query = query.trim();
            if (!TextUtils.isEmpty(query)) {
                configureSelection(loader, directoryId, null);
            }
        }
    }

    @Override
    protected void configureSelection(CursorLoader loader, long directoryId, ContactListFilter filter) {
        String selection = Contacts._ID + mContactsGroupSelection;//.replace("?", "'" + mGroupTitle + "'");
        if (mAccount != null) {
            String accountFilter = " AND view_raw_contacts." + RawContacts.ACCOUNT_NAME + "='" + mAccount.name
                + "' AND view_raw_contacts." + RawContacts.ACCOUNT_TYPE + "='" + mAccount.type + "'";
            selection += accountFilter;
        } else {
            String accountFilter = " AND view_raw_contacts." + RawContacts.ACCOUNT_NAME + " IS NULL "
                + " AND view_raw_contacts." + RawContacts.ACCOUNT_TYPE + " IS NULL ";
            selection += accountFilter;
        }
        selection += " )";
        loader.setSelection(selection);
        if (TextUtils.isEmpty(mGroupTitle)) {
            mGroupTitle = "";
        }
        loader.setSelectionArgs(new String[]{mGroupTitle});
    }

    @Override
    protected void bindView(View itemView, int partition, Cursor cursor, int position) {
        super.bindView(itemView, partition, cursor, position);
        final ContactListItemView view = (ContactListItemView) itemView;
        if (isSearchMode()) {
            view.showSnippet(cursor, ContactQuery.CONTACT_SNIPPET);
        }
    }
}
