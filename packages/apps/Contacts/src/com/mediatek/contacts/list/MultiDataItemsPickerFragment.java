
package com.mediatek.contacts.list;

import android.content.Intent;
import android.os.Bundle;

import com.android.contacts.list.ContactEntryListAdapter;
import com.android.contacts.list.ContactListFilter;

public class MultiDataItemsPickerFragment extends DataKindPickerBaseFragment {

    private Intent mIntent;

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        mIntent = this.getArguments().getParcelable(MultiContactsPickerBaseFragment.FRAGMENT_ARGS);
    }

    @Override
    protected ContactEntryListAdapter createListAdapter() {
        MultiDataItemsPickerAdapter adapter = new MultiDataItemsPickerAdapter(getActivity(),
                getListView());
        adapter.setFilter(ContactListFilter
                .createFilterWithType(ContactListFilter.FILTER_TYPE_ALL_ACCOUNTS));
        if (mIntent != null) {
            long[] phoneIds = mIntent.getLongArrayExtra(ContactListMultiChoiceActivity.RESTRICT_LIST);
            if (phoneIds != null && phoneIds.length > 0) {
                adapter.setRestrictList(phoneIds);
            }
            adapter.setMimetype(mIntent.getType());
        }
        return adapter;
    }

}
