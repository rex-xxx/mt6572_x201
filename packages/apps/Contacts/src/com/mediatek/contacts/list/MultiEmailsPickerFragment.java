
package com.mediatek.contacts.list;

import com.android.contacts.list.ContactEntryListAdapter;
import com.android.contacts.list.ContactListFilter;

public class MultiEmailsPickerFragment extends DataKindPickerBaseFragment {

    @Override
    protected ContactEntryListAdapter createListAdapter() {
        MultiEmailsPickerAdapter adapter = new MultiEmailsPickerAdapter(getActivity(),
                getListView());
        adapter.setFilter(ContactListFilter
                .createFilterWithType(ContactListFilter.FILTER_TYPE_ALL_ACCOUNTS));
        return adapter;
    }

}
