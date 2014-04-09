
package com.mediatek.contacts.list;

import android.content.Loader;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.contacts.R;
import com.android.contacts.list.ContactEntryListAdapter;
import com.android.contacts.list.ContactEntryListFragment;
import com.mediatek.contacts.ContactsFeatureConstants.FeatureOption;
import com.mediatek.contacts.util.LogUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * The Fragment Base class to handle the basic functions.
 */
public abstract class AbstractPickerFragment extends ContactEntryListFragment<ContactEntryListAdapter>
        implements ContactListMultiChoiceListener {

    private static final String TAG = AbstractPickerFragment.class.getSimpleName();

    private static final String KEY_CHECKEDIDS = "checkedids";

    /*
     * The Cursor window allocates 2M bytes memory for each client. If the data
     * size is very big, the cursor window would not allocate the memory for
     * Cursor.moveWindow. To avoid malicious operations, we only allow user to
     * handle 3500 items.
     */
    public static final int ALLOWED_ITEMS_MAX = 3500;

    private String mSlectedItemsFormater;

    private String mSearchString;

    // Show account filter settings
    private View mAccountFilterHeader;

    private TextView mEmptyView;

    // is or is not select all items.
    private boolean mIsSelectedAll = false;
    // is or is not select on item.
    private boolean mIsSelectedNone = true;

    private Set<Long> mCheckedIds = new HashSet<Long>();

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mSlectedItemsFormater = getActivity().getString(R.string.menu_actionbar_selected_items);
        
        showSelectedCount(mCheckedIds.size());
        //here should disable the Ok Button,because if call #onActivityCreated()
        //it says it is first Run this Activity or the previous process has been killed
        //the start this Activity again,then it will load data from DB in #onStart()
        setOkButtonStatus(false);

        //Enable multiple choice mode.
        getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
    }
    
    /**
     * 
     * @param checkedItemsCount The count of items selected to show on the top view.
     */
    private void showSelectedCount(int checkedItemsCount) {
        TextView selectedItemsView = (TextView) getActivity().getActionBar().getCustomView()
                .findViewById(R.id.select_items);
        if (selectedItemsView == null) {
            Log.e(TAG, "Load view resource error!");
            return;
        }
        if (mSlectedItemsFormater == null) {
            Log.e(TAG, "Load string resource error!");
            return;
        }
        selectedItemsView.setText(String.format(mSlectedItemsFormater, String.valueOf(checkedItemsCount)));
    }
    
    /**
     * 
     * @param enable True to enable the OK button on the Top view,false to diable.
     */
    private void setOkButtonStatus(boolean enable) {
        Button optionView = (Button) getActivity().getActionBar().getCustomView().findViewById(R.id.menu_option);
        if (optionView != null) {
            if (enable) {
                optionView.setEnabled(true);
                optionView.setTextColor(Color.WHITE);
            } else {
                optionView.setEnabled(false);
                optionView.setTextColor(Color.LTGRAY);
            }
        }
    }

    @Override
    protected View inflateView(LayoutInflater inflater, ViewGroup container) {
        return inflater.inflate(R.layout.multichoice_contact_list, null);
    }

    @Override
    protected void onCreateView(LayoutInflater inflater, ViewGroup container) {
        super.onCreateView(inflater, container);
        mAccountFilterHeader = getView().findViewById(R.id.account_filter_header_container);

        mEmptyView = (TextView) getView().findViewById(R.id.contact_list_empty);
        if (mEmptyView != null) {
            mEmptyView.setText(R.string.noContacts);
        }
    }

    @Override
    protected void configureAdapter() {
        super.configureAdapter();
        ContactEntryListAdapter adapter = getAdapter();
        if (adapter == null) {
            return;
        }
        adapter.setEmptyListEnabled(true);
        // Show A-Z section index.
        adapter.setSectionHeaderDisplayEnabled(true);
        adapter.setDisplayPhotos(true);
        adapter.setQuickContactEnabled(false);
        super.setPhotoLoaderEnabled(true);
        adapter.setQueryString(mSearchString);
        adapter.setIncludeProfile(false);

        // Apply MTK theme manager
        if (mAccountFilterHeader != null) {
            final TextView headerTextView = (TextView) mAccountFilterHeader
                    .findViewById(R.id.account_filter_header);

            if (FeatureOption.MTK_THEMEMANAGER_APP) {
                Resources res = getContext().getResources();
                int textColor = res.getThemeMainColor();
                if (textColor != 0) {
                    headerTextView.setTextColor(textColor);
                }
            }

            if (headerTextView != null) {
                headerTextView.setText(R.string.contact_list_loading);
                mAccountFilterHeader.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    protected void onItemClick(int position, long id) {
        return;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Log.d(TAG, "onItemClick with adapterView");
        super.onItemClick(parent, view, position, id);

        if (getListView().isItemChecked(position)) {
            if (mCheckedIds.size() >= ALLOWED_ITEMS_MAX) {
                Toast.makeText(getActivity(), R.string.multichoice_contacts_limit, Toast.LENGTH_SHORT)
                        .show();
                getListView().setItemChecked(position, false);
                getListView().setSelection(ALLOWED_ITEMS_MAX - 1);
                return;
            }
            mCheckedIds.add(Long.valueOf(id));
        } else {
            mCheckedIds.remove(Long.valueOf(id));
        }
        /*
         * fix bug for ALPS00123809:check box not enabled start
         */
        getListView().setItemChecked(position, getListView().isItemChecked(position));
        /*
         * fix bug for ALPS00123809:check box not enabled end
         */
        updateSelectedItemsView(mCheckedIds.size());
    }

    @Override
    public void onClearSelect() {
        updateCheckBoxState(false);
    }

    @Override
    public void onSelectAll() {
        updateCheckBoxState(true);
    }

    @Override
    public void restoreSavedState(Bundle savedState) {
        super.restoreSavedState(savedState);

        if (savedState == null) {
            return;
        }

        if (mCheckedIds == null) {
            mCheckedIds = new HashSet<Long>();
        }
        mCheckedIds.clear();

        long[] ids = savedState.getLongArray(KEY_CHECKEDIDS);
        int checkedItemSize = ids.length;
        for (int index = 0; index < checkedItemSize; ++index) {
            mCheckedIds.add(Long.valueOf(ids[index]));
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Log.d(TAG, "onLoadFinished");
        if (getAdapter().isSearchMode()) {
            Log.d(TAG, "SearchMode");
            int[] ids = data.getExtras().getIntArray("non_filter_ids");
            if (ids != null) {
                StringBuilder sb = new StringBuilder();
                sb.append("onLoadFinished ids: ");
                for (int id : ids) {
                    sb.append(String.valueOf(id) + ",");
                }
                Log.d(TAG, sb.toString());

                for (Iterator<Long> it = mCheckedIds.iterator(); it.hasNext();) {
                    Long id = it.next();
                    if (Arrays.binarySearch(ids, id.intValue()) < 0) {
                        it.remove();
                    }
                }
            }
            getListView().setFastScrollEnabled(false);
            getListView().setFastScrollAlwaysVisible(false);
        }

        if (data == null || (data != null && data.getCount() == 0)) {
            if (mEmptyView != null) {
                if (getAdapter().isSearchMode()) {
                    mEmptyView.setText(R.string.listFoundAllContactsZero);
                } else {
                    mEmptyView.setText(R.string.noContacts);
                }
                mEmptyView.setVisibility(View.VISIBLE);
            }
            // Disable fast scroll bar
            getListView().setFastScrollEnabled(false);
            getListView().setFastScrollAlwaysVisible(false);
        } else {
            if (mEmptyView != null) {
                if (getAdapter().isSearchMode()) {
                    mEmptyView.setText(R.string.listFoundAllContactsZero);
                } else {
                    mEmptyView.setText(R.string.noContacts);
                }
                mEmptyView.setVisibility(View.GONE);
            }
            // Enable fast scroll bar
            if (!getAdapter().isSearchMode()) {
                getListView().setFastScrollEnabled(true);
                getListView().setFastScrollAlwaysVisible(true);
            }
        }

        // clear list view choices
        getListView().clearChoices();

        Set<Long> newDataSet = new HashSet<Long>();

        long dataId = -1;
        int position = 0;

        if (data != null) {
            data.moveToPosition(-1);
            while (data.moveToNext()) {
                dataId = -1;
                dataId = data.getInt(0);
                newDataSet.add(dataId);

                if (mCheckedIds.contains(dataId) || mPushedIds.contains(dataId)) {
                    getListView().setItemChecked(position, true);
                }

                ++position;

                handleCursorItem(data);
            }
        }
        if (!getAdapter().isSearchMode()) {
            for (Iterator<Long> it = mCheckedIds.iterator(); it.hasNext();) {
                Long id = it.next();
                if (!newDataSet.contains(id)) {
                    it.remove();
                }
            }
        }
        ///M: fix [ALPS00539605] first onLoadComplete won't load newly pushed id data.
        /// so, the pushed ids should be merged in mCheckedIds after first onLoadFinished finished.
         if (!mPushedIds.isEmpty()) {
            mCheckedIds.addAll(mPushedIds);
            mPushedIds.clear();
        }
        updateSelectedItemsView(mCheckedIds.size());

        //fix [ALPS00578162]
        // the super class will try to restore the list view state if the
        // process been killed
        // in back ground,and the state has been set to the lasted,so clear last
        // state to prevent restore.
        clearListViewLastState();

        // The super function has to be called here.
        super.onLoadFinished(loader, data);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        final int checkedItemsCount = mCheckedIds.size();
        long[] checkedIds = new long[checkedItemsCount];
        int index = 0;
        for (Long id : mCheckedIds) {
            checkedIds[index++] = id;
        }
        outState.putLongArray(KEY_CHECKEDIDS, checkedIds);
    }

    private void updateCheckBoxState(boolean checked) {
        final int count = getListView().getAdapter().getCount();
        long dataId = -1;
        for (int position = 0; position < count; ++position) {
            if (checked) {
                if (mCheckedIds.size() >= ALLOWED_ITEMS_MAX) {
                    Toast.makeText(getActivity(), R.string.multichoice_contacts_limit, 
                            Toast.LENGTH_SHORT).show();
                    getListView().setSelection(ALLOWED_ITEMS_MAX - 1);
                    break;
                }
                if (!getListView().isItemChecked(position)) {
                    getListView().setItemChecked(position, checked);
                    dataId = getListItemDataId(position);
                    mCheckedIds.add(Long.valueOf(dataId));
                }
            } else {
                mCheckedIds.clear();
                getListView().setItemChecked(position, checked);
            }
        }
        updateSelectedItemsView(mCheckedIds.size());
    }

    private void updateSelectedItemsView(int checkedItemsCount) {
        // if there is no item selected, the "OK" button disable.
        Button optionView = (Button) getActivity().getActionBar().getCustomView().findViewById(
                R.id.menu_option);
        if (checkedItemsCount == 0) {
            mIsSelectedNone = true;
        } else {
            mIsSelectedNone = false;
        }
        setOkButtonStatus(!mIsSelectedNone);
        
        if (getAdapter().isSearchMode()) {
            LogUtils.w(TAG, "#updateSelectedItemsView(),isSearchMonde,don't showSelectedCount:" + checkedItemsCount);
            return;
        }
        
        showSelectedCount(checkedItemsCount);
    }

    public void updateSelectedItemsView() {
        final ContactEntryListAdapter adapter = (ContactEntryListAdapter) getAdapter();
        final int count = getListView().getAdapter().getCount();
        final int checkCount = mCheckedIds.size();
        updateSelectedItemsView(checkCount);
        if (count == getListView().getCheckedItemCount() || checkCount >= ALLOWED_ITEMS_MAX) {
            mIsSelectedAll = true;
        } else {
            mIsSelectedAll = false;
        }
    }

    public long[] getCheckedItemIds() {
        return convertSetToPrimitive(mCheckedIds);
    }

    public void startSearch(String searchString) {
        // It could not meet the layout Request. So, we should not use the
        // default search function.

        // Normalize the empty query.
        if (TextUtils.isEmpty(searchString)) {
            searchString = null;
        }

        ContactEntryListAdapter adapter = (ContactEntryListAdapter) getAdapter();
        if (searchString == null) {
            if (adapter != null) {
                mSearchString = null;
                adapter.setQueryString(searchString);
                adapter.setSearchMode(false);
                reloadData();
            }
        } else if (!TextUtils.equals(mSearchString, searchString)) {
            mSearchString = searchString;
            if (adapter != null) {
                adapter.setQueryString(searchString);
                adapter.setSearchMode(true);
                reloadData();
            }
        }
    }

    public void markItemsAsSelectedForCheckedGroups(long[] ids) {
        ///M: ensure the pushed ids merged into mCheckedIds, would not exceed
        /// ALLOWED_ITEMS_MAX
        Set<Long> tempCheckedIds = new HashSet<Long>();
        tempCheckedIds.addAll(mCheckedIds);
        for (long id : ids) {
            if (tempCheckedIds.size() >= ALLOWED_ITEMS_MAX) {
                Toast.makeText(getActivity(), R.string.multichoice_contacts_limit,
                        Toast.LENGTH_SHORT).show();
                return;
            } else {
                tempCheckedIds.add(id);
                mPushedIds.add(id);
            }
        }
    }

    /**
     * @return mIsSelectedAll
     */
    public boolean isSelectedAll() {
        return mIsSelectedAll;
    }

    /**
     * @return mIsSelectedNone
     */
    public boolean isSelectedNone() {
        return mIsSelectedNone;
    }

    public abstract long getListItemDataId(int position);

    /**
     * Long array converters to primitive long array.</br>
     * 
     * @param set a Long array.
     * @return a long array, or null if array is null or empty
     */
    private static long[] convertSetToPrimitive(Set<Long> set) {
        if (set == null) {
            return null;
        }

        final int arraySize = set.size();
        long[] result = new long[arraySize];

        int index = 0;
        for (Long id : set) {
            if (index >= arraySize) {
                break;
            }
            result[index++] = id.longValue();
        }

        return result;
    }

    public void handleCursorItem(Cursor cursor) {
        return;
    }

    /**
     * M: fix [ALPS00539605] It displays three contacts selectd,but displays four contacts in group view.
     */
    private Set<Long> mPushedIds = new HashSet<Long>();
}
