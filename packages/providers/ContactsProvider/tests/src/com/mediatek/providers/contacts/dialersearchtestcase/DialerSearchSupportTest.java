
package com.mediatek.providers.contacts.dialersearchtestcase;

import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Preferences;
import android.provider.ContactsContract.RawContacts;
import android.text.TextUtils;

import com.android.providers.contacts.BaseContactsProvider2Test;

public class DialerSearchSupportTest extends BaseContactsProvider2Test {

    public void test01QueryContactWithCtFunction() {
        Uri baseUri = Uri.withAppendedPath(ContactsContract.AUTHORITY_URI, "dialer_search");
        executeCheckAction(baseUri, false);
    }

    public void test02QueryContactWithOldFunction() {
        Uri baseUri = Uri.withAppendedPath(ContactsContract.AUTHORITY_URI, "dialer_search")
                .buildUpon().appendQueryParameter("search_behavior", "old").build();
        executeCheckAction(baseUri, true);
    }

    private void executeCheckAction(Uri baseUri, boolean withBehaviorParameter) {
        final String phoneNumber = "18001234567";
        ContentValues values = new ContentValues();
        long rawContactId = createRawContact(values, phoneNumber, 0);
        insertStructuredName(rawContactId, "Terry", "Autumn");

        //init query
        String initQueryUri = "content://com.android.contacts/dialer_search/filter/init#"
                + Preferences.DISPLAY_ORDER_PRIMARY + "#" + Preferences.SORT_ORDER_PRIMARY;
        assertQuery(initQueryUri, null, 0);

        String incrementUri = "content://com.android.contacts/dialer_search/filter/";

        // name increment query
        assertQuery(incrementUri, "8", 1);
        assertQuery(incrementUri, "82", 1);
        assertQuery(incrementUri, "829", 0);

        // empyt query
        String emptyUriString = "content://com.android.contacts/dialer_search/filter/null_input";
        assertQuery(emptyUriString, null, 0);

        // number increment query
        assertQuery(incrementUri, phoneNumber.substring(0, 1), 1);
        assertQuery(incrementUri, phoneNumber.substring(0, 1) + "9", 0);
        assertQuery(incrementUri, phoneNumber.substring(0, 1), 1);

        // empyt query
        assertQuery(emptyUriString, null, 0);

        // simple query
        String simpleUri = "content://com.android.contacts/dialer_search_number/filter/";
        assertQuery(simpleUri, phoneNumber, 1);
    }

    // private void assertQuery(Uri baseUri, String searchKey, int resultCount)
    // {
    private void assertQuery(String baseUriString, String searchKey, int resultCount) {
        int inputLen = searchKey == null ? 0 : searchKey.length();
        Uri queryUri = inputLen == 0 ? Uri.parse(baseUriString) : Uri.parse(baseUriString
                + searchKey);
        Cursor c = mResolver.query(queryUri, null, null, null, null);
        assertEquals(resultCount, c.getCount());
        if (c != null) {
            c.close();
        }
    }

    private long createRawContact(ContentValues values, String phoneNumber, int timesContacted) {
        values.put(RawContacts.CUSTOM_RINGTONE, "beethoven5");
        values.put(RawContacts.TIMES_CONTACTED, timesContacted);
        values.put(RawContacts.SEND_TO_VOICEMAIL, 1);

        Uri insertionUri = RawContacts.CONTENT_URI;
        Uri rawContactUri = mResolver.insert(insertionUri, values);
        long rawContactId = ContentUris.parseId(rawContactUri);
        Uri photoUri = insertPhoto(rawContactId);
        long photoId = ContentUris.parseId(photoUri);
        values.put(Contacts.PHOTO_ID, photoId);
        if (!TextUtils.isEmpty(phoneNumber)) {
            insertPhoneNumber(rawContactId, phoneNumber);
        }
        return rawContactId;
    }

}
