
package com.mediatek.contacts.list;

import android.accounts.Account;
import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.net.Uri;
import android.net.Uri.Builder;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.ContactCounts;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Directory;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.SearchSnippetColumns;
import android.text.TextUtils;
import android.view.View;
import android.widget.ListView;
import android.util.Log;

import com.android.contacts.list.ContactListAdapter.ContactQuery;
import com.android.contacts.list.ContactListFilter;
import com.android.contacts.list.ContactListItemView;


public class ContactsGroupAddMultiContactsAdapter extends MultiContactsBasePickerAdapter {
    private static final String TAG = ContactsGroupAddMultiContactsAdapter.class.getSimpleName();

    public final String mMembersSelection =
        " IN "
                + "(SELECT " + RawContacts.CONTACT_ID
                + " FROM " + "view_raw_contacts"
                + " WHERE " + "view_raw_contacts.contact_id" + " NOT IN ";

    private String mAccountName;
    private String mAccountType;
    private long[] mExistMemberContactIds;

    public ContactsGroupAddMultiContactsAdapter(Context context, ListView lv) {
        super(context, lv);
    }    
    
    public void setGroupAccount(String account, String type) {
        mAccountName = account;
        mAccountType = type;
    }
    
    public void setExistMemberList(long[] ids) {
        mExistMemberContactIds = ids;
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
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        for (long id : this.mExistMemberContactIds) {
            sb.append(String.valueOf(id));
            sb.append(",");
        }
		if (mExistMemberContactIds.length > 0) {
        	sb.deleteCharAt(sb.length() - 1);
		}
        sb.append(")");
		Log.d(TAG, "id string " + sb.toString());
        String selection = Contacts._ID + mMembersSelection + sb.toString();
		if (mAccountName != null && mAccountType != null) {
            String accountFilter = " AND view_raw_contacts." + RawContacts.ACCOUNT_NAME + "='" + mAccountName
                + "' AND view_raw_contacts." + RawContacts.ACCOUNT_TYPE + "='" + mAccountType + "'";
            selection += accountFilter;
        } else {
            String accountFilter = " AND view_raw_contacts." + RawContacts.ACCOUNT_NAME + " IS NULL "
                + " AND view_raw_contacts." + RawContacts.ACCOUNT_TYPE + " IS NULL ";
            selection += accountFilter;
        }
        selection += " )";
        Log.d(TAG, "new selection " + selection.toString());
        loader.setSelection(selection);
    }

    @Override
    protected void bindView(View itemView, int partition, Cursor cursor, int position) {
        super.bindView(itemView, partition, cursor, position);
        final ContactListItemView view = (ContactListItemView) itemView;
        if (isSearchMode()) {
            /** M: set snippet show */
            view.showSnippet(cursor, ContactQuery.CONTACT_SNIPPET);
        }
    }
}
