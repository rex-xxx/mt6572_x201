
package com.mediatek.contacts.list;

import android.content.Context;
import android.content.CursorLoader;
import android.net.Uri;
import android.net.Uri.Builder;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Directory;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ListView;

import com.android.contacts.list.ContactListFilter;

public class MultiDataItemsPickerAdapter extends MultiPhoneNumbersPickerAdapter {

    private static final String TAG = MultiDataItemsPickerAdapter.class.getSimpleName();

    private long[] mRestrictPhoneIds;
    
    private String mMimeType;

    private Context mContext;
    
    public static final Uri DATA_OTHERS_URI = Uri
            .parse("content://com.android.contacts/data/others");

    public static final Uri DATA_OTHERS_FILTER_URI = Uri
            .withAppendedPath(DATA_OTHERS_URI, "filter");

    public MultiDataItemsPickerAdapter(Context context, ListView lv) {
        super(context, lv);
        mContext = context;
    }

    @Override
    protected Uri configLoaderUri(long directoryId) {
        Uri uri;

        if (directoryId != Directory.DEFAULT) {
            Log.w(TAG, "MultiDataItemsPickerAdapter is not ready for non-default directory ID ("
                    + "directoryId: " + directoryId + ")");
        }

        if (isSearchMode()) {
            String query = getQueryString();
            Builder builder = DATA_OTHERS_FILTER_URI.buildUpon();
            builder.appendQueryParameter("specified_data_mime_type", mMimeType);
            if (TextUtils.isEmpty(query)) {
                builder.appendPath("");
            } else {
                builder.appendPath(query); // Builder will encode the query
            }

            builder.appendQueryParameter(ContactsContract.DIRECTORY_PARAM_KEY, String
                    .valueOf(directoryId));
            builder.appendQueryParameter("non_filter_ids_arg", DATA_OTHERS_URI.toString());
            uri = builder.build();
        } else {
            uri = DATA_OTHERS_URI.buildUpon().appendQueryParameter(
                    ContactsContract.DIRECTORY_PARAM_KEY, String.valueOf(Directory.DEFAULT))
                    .appendQueryParameter("specified_data_mime_type", mMimeType).build();
            if (isSectionHeaderDisplayEnabled()) {
                uri = buildSectionIndexerUri(uri);
            }
        }
        
        Log.d(TAG, uri.toString());

        return uri;
    }

    protected void configureSelection(CursorLoader loader, long directoryId,
            ContactListFilter filter) {

        super.configureSelection(loader, directoryId, filter);
        StringBuilder selection = new StringBuilder();
        if (mRestrictPhoneIds != null && mRestrictPhoneIds.length > 0) {
            selection.append("( ");
            selection.append(Phone._ID + " IN (");
            for (long id : mRestrictPhoneIds) {
                selection.append(id + ",");
            }
            selection.deleteCharAt(selection.length() - 1);
            selection.append(") )");
        } else {
            selection.append("(0)");
        }
        selection.append(loader.getSelection());
        loader.setSelection(selection.toString());
        Log.d(TAG, selection.toString());
    }

    public void setRestrictList(long[] phoneIds) {
        mRestrictPhoneIds = phoneIds;
    }
    
    public void setMimetype(String mimeType) {
        mMimeType = mimeType;
    }
}
