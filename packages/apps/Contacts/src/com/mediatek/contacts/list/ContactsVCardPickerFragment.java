
package com.mediatek.contacts.list;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract.Contacts;
import android.util.Log;

public class ContactsVCardPickerFragment extends MultiContactsPickerBaseFragment {

    private static final String TAG = ContactsVCardPickerFragment.class.getSimpleName();

    private static final String[] LOOKUPPROJECT = new String[] {
        Contacts.LOOKUP_KEY
    };

    @Override
    public void onOptionAction() {

        final long[] idArray = getCheckedItemIds();
        if (idArray == null) {
            return;
        }

        Uri uri = null;
        if (idArray.length == 1) {
            // Get lookup uri for single contact
            uri = getLookupUriForEmail("Single_Contact", idArray);
        } else {
            // Get lookup uri for more than one contacts
            uri = getLookupUriForEmail("Multi_Contact", idArray);
        }

        Log.d(TAG, "The result uri is " + uri);

        final Intent retIntent = new Intent();
        final Activity activity = getActivity();
        retIntent.putExtra(RESULTINTENTEXTRANAME, uri);

        activity.setResult(Activity.RESULT_OK, retIntent);
        activity.finish();
    }

    private Uri getLookupUriForEmail(String type, long[] contactsIds) {

        Cursor cursor = null;
        Uri uri = null;
        Log.i(TAG, "type is " + type);
        if (type.equals("Single_Contact")) {
            Log.i(TAG, "In single contact");
            uri = Uri.withAppendedPath(Contacts.CONTENT_URI, Long.toString(contactsIds[0]));

            cursor = getActivity().getContentResolver().query(uri, LOOKUPPROJECT, null, null, null);

            if (cursor != null && cursor.moveToNext()) {
                Log.i(TAG, "Single_Contact  cursor.getCount() is " + cursor.getCount());

                uri = Uri.withAppendedPath(Contacts.CONTENT_VCARD_URI, cursor.getString(0));

                Log.i(TAG, "Single_Contact  uri is " + uri + " \ncursor.getString(0) is "
                        + cursor.getString(0));
            }
        } else if (type.equals("Multi_Contact")) {
            StringBuilder sb = new StringBuilder("");
            for (long contactId : contactsIds) {
                if (contactId == contactsIds[contactsIds.length - 1]) {
                    sb.append(contactId);
                } else {
                    sb.append(contactId + ",");
                }
            }
            String selection = Contacts._ID + " in (" + sb.toString() + ")";
            Log.d(TAG, "Multi_Contact, selection=" + selection);
            cursor = getActivity().getContentResolver().query(Contacts.CONTENT_URI, LOOKUPPROJECT,
                    selection, null, null);
            if (cursor == null) {
                return null;
            }

            Log.i(TAG, "Multi_Contact  cursor.getCount() is " + cursor.getCount());
            if (!cursor.moveToFirst()) {
                return null;
            }

            StringBuilder uriListBuilder = new StringBuilder();
            int index = 0;
            for (; !cursor.isAfterLast(); cursor.moveToNext()) {
                if (index != 0) {
                    uriListBuilder.append(':');
                }
                uriListBuilder.append(cursor.getString(0));
                index++;
            }
            uri = Uri.withAppendedPath(Contacts.CONTENT_MULTI_VCARD_URI, Uri.encode(uriListBuilder
                    .toString()));
            Log.i(TAG, "Multi_Contact  uri is " + uri);
        }

        if (cursor != null) {
            cursor.close();
        }

        return uri;

    }

}
