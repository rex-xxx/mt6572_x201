
package com.mediatek.contacts.list;

import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.net.Uri.Builder;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Directory;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.android.contacts.list.ContactListFilter;
import com.android.contacts.list.ContactListItemView;
import com.android.contacts.list.DefaultContactListAdapter;

import java.util.HashMap;

public class MultiContactsBasePickerAdapter extends DefaultContactListAdapter {

    public static final int FILTER_ACCOUNT_WITH_PHONE_NUMBER_ONLY = 100;
    public static final int FILTER_ACCOUNT_WITH_PHONE_NUMBER_OR_EMAIL = 101;

    private ListView mListView;
    private CursorLoader mLoader;

    private int mFilterAccountOptions;

    public final class PickListItemCache {

        public final class PickListItemData {
            public int contactIndicator;

            public int simIndex;

            public String displayName;

            public String lookupUri;

            public PickListItemData(int contactIndicator2, int simIndex2, String displayName2, String lookupUri2) {
                contactIndicator = contactIndicator2;
                simIndex = simIndex2;
                displayName = displayName2;
                lookupUri = lookupUri2;
            }
        }

        private HashMap<Long, PickListItemData> mMap = new HashMap<Long, PickListItemData>();

        public void add(long id, int contactIndicator, int simIndex, String displayName, String lookupUri) {
            mMap.put(Long.valueOf(id), new PickListItemData(contactIndicator, simIndex, displayName, lookupUri));
        }

        public void add(final Cursor cursor) {
            long id = cursor.getInt(ContactQuery.CONTACT_ID);
            int contactIndicator = cursor.getInt(cursor.getColumnIndexOrThrow(Contacts.INDICATE_PHONE_SIM));
            int simIndex = cursor.getInt(cursor.getColumnIndexOrThrow(Contacts.INDEX_IN_SIM));
            String displayName = cursor.getString(ContactQuery.CONTACT_DISPLAY_NAME);
            String lookupUri = cursor.getString(ContactQuery.CONTACT_LOOKUP_KEY);
            mMap.put(Long.valueOf(id), new PickListItemData(contactIndicator, simIndex, displayName, lookupUri));
        }

        // Clear the cache data
        public void clear() {
            mMap.clear();
        }

        // The cache is empty or not
        public boolean isEmpty() {
            return mMap.isEmpty();
        }

        public int getCacheSize() {
            return mMap.size();
        }

        public PickListItemData getItemData(long id) {
            return mMap.get(Long.valueOf(id));
        }
    }

    private PickListItemCache mPickListItemCache = new PickListItemCache();

    public MultiContactsBasePickerAdapter(Context context, ListView lv) {
        super(context);
        mListView = lv;
    }

    @Override
    protected View newView(Context context, int partition, Cursor cursor, int position,
            ViewGroup parent) {
        final ContactListItemView view = (ContactListItemView) super.newView(context, partition, cursor, position, parent);

        // Enable check box
        view.setCheckable(true);

        // For using list-view's check states
        view.setActivatedStateSupported(true);

        return view;
    }

    @Override
    protected void bindView(View itemView, int partition, Cursor cursor, int position) {
        final ContactListItemView view = (ContactListItemView) itemView;

        view.setHighlightedPrefix(isSearchMode() ? getUpperCaseQueryString() : null);

        if (isSelectionVisible()) {
            view.setActivated(isSelectedContact(partition, cursor));
        }

        bindSectionHeaderAndDivider(view, position, cursor);

        if (isQuickContactEnabled()) {
            bindQuickContact(view, partition, cursor, ContactQuery.CONTACT_PHOTO_ID,
                    ContactQuery.CONTACT_PHOTO_URI, ContactQuery.CONTACT_ID,
                    ContactQuery.CONTACT_LOOKUP_KEY);
        } else {
            if (getDisplayPhotos()) {
                bindPhoto(view, partition, cursor);
            }
        }

        bindName(view, cursor);
        bindPresenceAndStatusMessage(view, cursor);

        if (isSearchMode()) {
            bindSearchSnippet(view, cursor);
        } else {
            view.setSnippet(null);
        }

        Log.d("MultiContactsBasePickerAdapter", "bind view position = " + position
                + " check state = " + mListView.isItemChecked(position));
        view.getCheckBox().setChecked(mListView.isItemChecked(position));
    }

    @Override
    public void configureLoader(CursorLoader loader, long directoryId) {
        super.configureLoader(loader, directoryId);
        /** M: Bug Fix for ALPS00404125 @{ */
        ContactListFilter filter = getFilter();
        if (isSearchMode()) {
            String query = getQueryString();
            if (query == null) {
                query = "";
            }
            query = query.trim();
            if (!TextUtils.isEmpty(query)) {
                configureSelection(loader, directoryId, filter);
            }
            Builder builder = loader.getUri().buildUpon();
            builder.appendQueryParameter("non_filter_ids_arg", Contacts.CONTENT_URI.toString());
            loader.setUri(builder.build());
        }
        /** @} */
        mLoader = loader;
    }

    @Override
    protected void configureSelection(CursorLoader loader, long directoryId, ContactListFilter filter) {
        if (filter == null) {
            return;
        }

        if (directoryId != Directory.DEFAULT) {
            return;
        }

        super.configureSelection(loader, directoryId, filter);
        StringBuilder selection = new StringBuilder();
        selection.append(loader.getSelection());
        if (mFilterAccountOptions == FILTER_ACCOUNT_WITH_PHONE_NUMBER_ONLY) {
            selection.append(" AND " + Contacts.HAS_PHONE_NUMBER + "=1");
        } else if (mFilterAccountOptions == FILTER_ACCOUNT_WITH_PHONE_NUMBER_OR_EMAIL) {
            selection.append(" AND " + Contacts.HAS_PHONE_NUMBER + "=1");
        }
        loader.setSelection(selection.toString());
    }

    public void setFilterAccountOption(int filterAccountOptions) {
        mFilterAccountOptions = filterAccountOptions;
    }

    public int getContactID(int position) {
        Cursor cursor = (Cursor) getItem(position);
        if (cursor == null) {
            return 0;
        }
        return cursor.getInt(cursor.getColumnIndexOrThrow(Contacts._ID));
    }

    public void setDataSetChangedNotifyEnable(boolean enable) {
        if (mLoader != null) {
            if (enable) {
                mLoader.startLoading();
            } else {
                mLoader.stopLoading();
            }
        }
    }

    public void cacheDataItem(Cursor cursor) {
        mPickListItemCache.add(cursor);
    }

    public PickListItemCache getListItemCache() {
        return mPickListItemCache;
    }
}
