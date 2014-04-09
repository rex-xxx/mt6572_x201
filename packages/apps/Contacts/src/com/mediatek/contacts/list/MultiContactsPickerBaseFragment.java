
package com.mediatek.contacts.list;

import android.app.Activity;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import com.android.contacts.R;
import com.android.contacts.list.ContactEntryListAdapter;
import com.android.contacts.list.ContactListAdapter;
import com.android.contacts.list.ContactListFilter;
import com.android.contacts.list.ContactListFilterController;
import com.android.contacts.list.ProfileAndContactsLoader;
import com.android.contacts.util.AccountFilterUtil;

public class MultiContactsPickerBaseFragment extends AbstractPickerFragment {

    private static final String TAG = MultiContactsPickerBaseFragment.class.getSimpleName();

    public static final String FRAGMENT_ARGS = "intent";

    protected static final String RESULTINTENTEXTRANAME = "com.mediatek.contacts.list.pickcontactsresult";

    private static final String KEY_FILTER = "filter";

    private static final int REQUEST_CODE_ACCOUNT_FILTER = 1;

    // Show account filter settings
    private View mAccountFilterHeader;
    private boolean mShowFilterHeader = true;

    private ContactListFilter mFilter;
    private SharedPreferences mPrefs;

    private class FilterHeaderClickListener implements OnClickListener {
        @Override
        public void onClick(View view) {
            AccountFilterUtil.startAccountFilterActivityForResult(
                    MultiContactsPickerBaseFragment.this, REQUEST_CODE_ACCOUNT_FILTER, mFilter);
        }
    }

    private OnClickListener mFilterHeaderClickListener = new FilterHeaderClickListener();

    // SDN contacts should be use ProfileAndContactsLoader.
    @Override
    public CursorLoader createCursorLoader() {
        return new ProfileAndContactsLoader(getActivity());
    }

    @Override
    protected void onCreateView(LayoutInflater inflater, ViewGroup container) {
        super.onCreateView(inflater, container);

        mAccountFilterHeader = getView().findViewById(R.id.account_filter_header_container);
        if (isAccountFilterEnable()) {
            mAccountFilterHeader.setOnClickListener(mFilterHeaderClickListener);
        } else {
            mAccountFilterHeader.setClickable(false);
        }
        updateFilterHeaderView();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(activity);
        if (isAccountFilterEnable()) {
            restoreFilter();
        }
    }

    private void restoreFilter() {
        mFilter = ContactListFilter.restoreDefaultPreferences(mPrefs);
    }

    @Override
    protected void configureAdapter() {
        super.configureAdapter();
        final ContactEntryListAdapter adapter = getAdapter();

        adapter.setFilter(mFilter);
    }

    @Override
    protected ContactListAdapter createListAdapter() {
        MultiContactsBasePickerAdapter adapter = new MultiContactsBasePickerAdapter(getActivity(),
                getListView());
        adapter.setFilter(ContactListFilter
                .createFilterWithType(ContactListFilter.FILTER_TYPE_ALL_ACCOUNTS));
        return adapter;
    }

    protected void setListFilter(ContactListFilter filter) {
        if (isAccountFilterEnable()) {
            throw new RuntimeException(
                    "The #setListFilter could not be called if #isAccountFilterEnable is true");
        }
        mFilter = filter;
        getAdapter().setFilter(mFilter);
        updateFilterHeaderView();
    }

    /**
     * Check whether or not to show the account filter
     * 
     * @return true: the UI would to follow the user selected, false: the fixed
     *         account to passed and could not changed In the case, the the
     *         filter would to get by the function of
     *         {@link MultiContactsPickerBaseFragment#setListFilter(ContactListFilter)}
     */
    public boolean isAccountFilterEnable() {
        return true;
    }

    @Override
    public void restoreSavedState(Bundle savedState) {
        super.restoreSavedState(savedState);

        if (savedState == null) {
            return;
        }
        mFilter = savedState.getParcelable(KEY_FILTER);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_FILTER, mFilter);
    }

    private void setFilter(ContactListFilter filter) {
        if (mFilter == null && filter == null) {
            return;
        }

        if (mFilter != null && mFilter.equals(filter)) {
            return;
        }

        Log.v(TAG, "New filter: " + filter);

        mFilter = filter;
        saveFilter();
        reloadData();
    }

    private void updateFilterHeaderView() {
        if (!mShowFilterHeader) {
            if (mAccountFilterHeader != null) {
                mAccountFilterHeader.setVisibility(View.GONE);
            }
            return;
        }
        if (mAccountFilterHeader == null) {
            return; // Before onCreateView -- just ignore it.
        }

        if (mFilter != null && !isSearchMode()) {
            final boolean shouldShowHeader = AccountFilterUtil.updateAccountFilterTitleForPeople(
                    mAccountFilterHeader, mFilter, true);
            mAccountFilterHeader.setVisibility(shouldShowHeader ? View.VISIBLE : View.GONE);
        } else {
            mAccountFilterHeader.setVisibility(View.GONE);
        }
    }

    private void saveFilter() {
        ContactListFilter.storeToPreferences(mPrefs, mFilter);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_ACCOUNT_FILTER) {
            if (getActivity() != null) {
                AccountFilterUtil.handleAccountFilterResult(ContactListFilterController
                        .getInstance(getActivity()), resultCode, data);
                if (resultCode == Activity.RESULT_OK) {
                    setFilter(ContactListFilterController.getInstance(getActivity()).getFilter());
                    updateFilterHeaderView();
                }
            } else {
                Log.e(TAG, "getActivity() returns null during Fragment#onActivityResult()");
            }
        }
    }

    public void onOptionAction() {

        final long[] idArray = getCheckedItemIds();
        if (idArray == null) {
            return;
        }

        final Activity activity = getActivity();
        final Intent retIntent = new Intent();
        retIntent.putExtra(RESULTINTENTEXTRANAME, idArray);
        activity.setResult(Activity.RESULT_OK, retIntent);
        activity.finish();
    }

    protected void setDataSetChangedNotifyEnable(boolean enable) {
        MultiContactsBasePickerAdapter adapter = (MultiContactsBasePickerAdapter) getAdapter();
        if (adapter != null) {
            adapter.setDataSetChangedNotifyEnable(enable);
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        updateFilterHeaderView();
        super.onLoadFinished(loader, data);
    }

    public void showFilterHeader(boolean enable) {
        mShowFilterHeader = enable;
    }

    @Override
    public long getListItemDataId(int position) {
        final MultiContactsBasePickerAdapter adapter = (MultiContactsBasePickerAdapter) getAdapter();
        if (adapter != null) {
            return adapter.getContactID(position);
        }
        return -1;
    }

    @Override
    public void handleCursorItem(Cursor cursor) {
        final MultiContactsBasePickerAdapter adapter = (MultiContactsBasePickerAdapter) getAdapter();
        adapter.cacheDataItem(cursor);
    }
}
