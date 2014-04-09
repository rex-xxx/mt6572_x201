package com.mediatek.contacts.list;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.android.contacts.group.GroupEditorFragment;
import com.android.contacts.list.ContactEntryListAdapter;
import com.android.contacts.list.ContactListFilter;
import com.android.contacts.list.ContactListAdapter;

public class ContactsGroupAddMultiContactsFragment extends MultiContactsPickerBaseFragment
        implements GroupEditorFragment.ScrubListener {

    private static final String TAG = ContactsGroupAddMultiContactsFragment.class.getSimpleName();

    @Override
    protected ContactListAdapter createListAdapter() {
        ContactsGroupAddMultiContactsAdapter adapter = new ContactsGroupAddMultiContactsAdapter(
                getActivity(), getListView());
        adapter.setFilter(ContactListFilter
                .createFilterWithType(ContactListFilter.FILTER_TYPE_ALL_ACCOUNTS));
        adapter.setSectionHeaderDisplayEnabled(true);
        adapter.setDisplayPhotos(true);
        adapter.setQuickContactEnabled(false);
        adapter.setEmptyListEnabled(true);

        final Intent intent = getArguments().getParcelable(FRAGMENT_ARGS);
        String accountName = (String) intent.getStringExtra("account_name");
        String accountType = (String) intent.getStringExtra("account_type");
        adapter.setGroupAccount(accountName, accountType);
        adapter.setExistMemberList(intent.getLongArrayExtra("member_ids"));
        return adapter;
    }
    
    public boolean isAccountFilterEnable() {
        return false;
    }

    public void scrubAffinity() {
        getActivity().finish();
    }

    @Override
    public void onCreate(Bundle savedState) {
        Log.d(TAG, "onCreate setScrubListener");
        super.onCreate(savedState);
        GroupEditorFragment.setScrubListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        GroupEditorFragment.removeScrubListener(this);
    }
}
