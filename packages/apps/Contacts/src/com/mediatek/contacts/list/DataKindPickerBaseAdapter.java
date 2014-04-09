
package com.mediatek.contacts.list;

import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.ContactCounts;
import android.provider.ContactsContract.Contacts;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.android.contacts.list.ContactEntryListAdapter;
import com.android.contacts.list.ContactListFilter;
import com.android.contacts.list.ContactListItemView;
import com.mediatek.contacts.ExtensionManager;
import com.mediatek.phone.SIMInfoWrapper;

public abstract class DataKindPickerBaseAdapter extends ContactEntryListAdapter {

    private static final String TAG = DataKindPickerBaseAdapter.class.getSimpleName();
    private ListView mListView;
    private ContactListItemView.PhotoPosition mPhotoPosition;
    private Context mContext;

    public DataKindPickerBaseAdapter(Context context, ListView lv) {
        super(context);
        mListView = lv;
        mContext = context;
    }

    protected ListView getListView() {
        return mListView;
    }

    @Override
    public final void configureLoader(CursorLoader loader, long directoryId) {

        loader.setUri(configLoaderUri(directoryId));
        loader.setProjection(configProjection());
        configureSelection(loader, directoryId, getFilter());

        // Set the Contacts sort key as sort order
        String sortOrder;
        if (getSortOrder() == ContactsContract.Preferences.SORT_ORDER_PRIMARY) {
            sortOrder = Contacts.SORT_KEY_PRIMARY;
        } else {
            sortOrder = Contacts.SORT_KEY_ALTERNATIVE;
        }

        loader.setSortOrder(sortOrder);
    }

    protected abstract String[] configProjection();

    protected abstract Uri configLoaderUri(long directoryId);

    protected abstract void configureSelection(CursorLoader loader, long directoryId,
            ContactListFilter filter);

    protected static Uri buildSectionIndexerUri(Uri uri) {
        return uri.buildUpon()
                .appendQueryParameter(ContactCounts.ADDRESS_BOOK_INDEX_EXTRAS, "true").build();
    }

    @Override
    protected View newView(Context context, int partition, Cursor cursor, int position,
            ViewGroup parent) {
        ContactListItemView view = new ContactListItemView(context, null);
        view.setUnknownNameText(context.getText(android.R.string.unknownName));
        view.setQuickContactEnabled(isQuickContactEnabled());

        // Enable check box
        view.setCheckable(true);
        if (mPhotoPosition != null) {
            view.setPhotoPosition(mPhotoPosition);
        }
        view.setActivatedStateSupported(true);
        return view;
    }

    public void displayPhotoOnLeft() {
        mPhotoPosition = ContactListItemView.PhotoPosition.LEFT;
    }

    @Override
    protected void bindView(View itemView, int partition, Cursor cursor, int position) {
        final ContactListItemView view = (ContactListItemView) itemView;

        // Look at elements before and after this position, checking if contact
        // IDs are same.
        // If they have one same contact ID, it means they can be grouped.
        //
        // In one group, only the first entry will show its photo and names
        // (display name and
        // phonetic name), and the other entries in the group show just their
        // data (e.g. phone
        // number, email address).
        cursor.moveToPosition(position);
        boolean isFirstEntry = true;
        boolean showBottomDivider = true;
        final long currentContactId = cursor.getLong(getContactIDColumnIndex());
        if (cursor.moveToPrevious() && !cursor.isBeforeFirst()) {
            final long previousContactId = cursor.getLong(getContactIDColumnIndex());
            if (currentContactId == previousContactId) {
                isFirstEntry = false;
            }
        }
        cursor.moveToPosition(position);
        if (cursor.moveToNext() && !cursor.isAfterLast()) {
            final long nextContactId = cursor.getLong(getContactIDColumnIndex());
            if (currentContactId == nextContactId) {
                // The following entry should be in the same group, which means
                // we don't want a
                // divider between them.
                // TODO: we want a different divider than the divider between
                // groups. Just hiding
                // this divider won't be enough.
                showBottomDivider = false;
            }
        }
        cursor.moveToPosition(position);

        bindSectionHeaderAndDivider(view, position, cursor);

        if (isFirstEntry) {
            bindName(view, cursor);
            if (isQuickContactEnabled()) {
                bindQuickContact(view, partition, cursor);
            } else {
                bindPhoto(view, cursor);
            }
        } else {
            unbindName(view);

            view.removePhotoView(true, false);
        }

        bindData(view, cursor);

        if (!isSearchMode()) {
            view.setSnippet(null);
        }

        view.setDividerVisible(showBottomDivider);

        view.getCheckBox().setChecked(mListView.isItemChecked(position));
    }

    protected void bindSectionHeaderAndDivider(ContactListItemView view, int position, Cursor cursor) {
        if (isSectionHeaderDisplayEnabled()) {
            Placement placement = getItemPlacementInSection(position);

            view.setSectionHeader(placement.sectionHeader);
            view.setDividerVisible(!placement.lastInSection);
        } else {
            view.setSectionHeader(null);
            view.setDividerVisible(true);
        }
    }

    protected void bindPhoto(final ContactListItemView view, Cursor cursor) {
        long photoId = 0;
        if (!cursor.isNull(getPhotoIDColumnIndex())) {
            photoId = cursor.getLong(getPhotoIDColumnIndex());
        }

        int indicatePhoneSim = cursor.getInt(getIndicatePhoneSIMColumnIndex());
        if (indicatePhoneSim > 0) {
            photoId = getSimType(indicatePhoneSim, cursor.getInt(getIsSdnContactColumnIndex()));
        }

        getPhotoLoader().loadThumbnail(view.getPhotoView(), photoId, false);
    }

    protected void bindData(ContactListItemView view, Cursor cursor) {
        CharSequence label = null;
        if (!cursor.isNull(getDataTypeColumnIndex())) {
            final int type = cursor.getInt(getDataTypeColumnIndex());
            final String customLabel = cursor.getString(getDataLabelColumnIndex());

            // TODO cache
            /** M:AAS @ { original code:
            label = Phone.getTypeLabel(mContext.getResources(), type, customLabel); */
            int indicateIndex = cursor.getColumnIndex(Contacts.INDICATE_PHONE_SIM);
            int slotId = -1;
            if (indicateIndex != -1) {
                int simId = cursor.getInt(indicateIndex);
                slotId = SIMInfoWrapper.getDefault().getSimSlotById(simId);
            }
            Log.v(TAG, "DataKindPoickerBaseAdapter, the default flow to get label.");
            label = ExtensionManager.getInstance().getContactAccountExtension().getTypeLabel(mContext.getResources(),
                    type, customLabel, slotId, ExtensionManager.COMMD_FOR_AAS);
            /** M: @ } */
        }
        view.setLabel(label);
        view.showData(cursor, getDataColumnIndex());
    }

    protected void unbindName(final ContactListItemView view) {
        view.hideDisplayName();
        view.hidePhoneticName();
    }

    public abstract Uri getDataUri(int position);

    public abstract long getDataId(int position);

    public abstract void bindName(final ContactListItemView view, Cursor cursor);

    public abstract void bindQuickContact(final ContactListItemView view, int partitionIndex,
            Cursor cursor);

    public abstract int getPhotoIDColumnIndex();

    public abstract int getDataTypeColumnIndex();

    public abstract int getDataLabelColumnIndex();

    public abstract int getDataColumnIndex();

    public abstract int getContactIDColumnIndex();

    public abstract int getPhoneticNameColumnIndex();

    public abstract int getIndicatePhoneSIMColumnIndex();
    
    public abstract int getIsSdnContactColumnIndex();

    public boolean hasStableIds() {
        return false;
    }

    public long getItemId(int position) {
        return getDataId(position);
    }
}
