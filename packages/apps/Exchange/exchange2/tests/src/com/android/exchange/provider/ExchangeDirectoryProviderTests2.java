package com.android.exchange.provider;

import android.database.MatrixCursor;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Contacts.Data;

import com.android.exchange.provider.ExchangeDirectoryProvider.GalContactRow;
import com.android.exchange.utility.ExchangeTestCase;

public class ExchangeDirectoryProviderTests2 extends ExchangeTestCase {
    // Create a test projection; we should only get back values for display name and email address
    private static final String[] GAL_RESULT_PROJECTION =
        new String[] {Data.MIMETYPE, CommonDataKinds.Email.ADDRESS, Email.TYPE,
        StructuredName.FAMILY_NAME};
    private static final int GAL_RESULT_COLUMN_1 = 0;
    private static final int GAL_RESULT_COLUMN_2 = 1;
    private static final int GAL_RESULT_COLUMN_3 = 2;
    private static final int GAL_RESULT_COLUMN_4 = 3;

    public void testGalContactRow() {
        MatrixCursor cursor = new MatrixCursor(GAL_RESULT_PROJECTION);
        GalContactRow.addEmailAddress(cursor, new ExchangeDirectoryProvider.GalProjection(
                GAL_RESULT_PROJECTION), 1, null, "accountName", "displayName", "test@mediatek.com");
        GalContactRow.addPhoneRow(cursor, new ExchangeDirectoryProvider.GalProjection(
                GAL_RESULT_PROJECTION), 1, null, "accountName", "displayName", Phone.TYPE_MOBILE, "13812345678");
        GalContactRow.addNameRow(cursor, new ExchangeDirectoryProvider.GalProjection(
                GAL_RESULT_PROJECTION), 1, null, "accountName", "MTK", "GANG", "FENG");
        assertNotNull(cursor);
        assertEquals(MatrixCursor.class, cursor.getClass());
        assertEquals(3, cursor.getCount());
        assertTrue(cursor.moveToNext());
        assertEquals(Email.CONTENT_ITEM_TYPE, cursor.getString(GAL_RESULT_COLUMN_1));
        assertEquals("test@mediatek.com", cursor.getString(GAL_RESULT_COLUMN_2));
        assertEquals(Email.TYPE_WORK, cursor.getInt(GAL_RESULT_COLUMN_3));
        assertTrue(cursor.moveToNext());
        assertEquals(Phone.CONTENT_ITEM_TYPE, cursor.getString(GAL_RESULT_COLUMN_1));
        assertEquals("13812345678", cursor.getString(GAL_RESULT_COLUMN_2));
        assertEquals(Phone.TYPE_MOBILE, cursor.getInt(GAL_RESULT_COLUMN_3));
        assertTrue(cursor.moveToNext());
        assertEquals(StructuredName.CONTENT_ITEM_TYPE, cursor.getString(GAL_RESULT_COLUMN_1));
        assertEquals("MTK", cursor.getString(GAL_RESULT_COLUMN_2));
        assertEquals("GANG", cursor.getString(GAL_RESULT_COLUMN_3));
        assertEquals("FENG", cursor.getString(GAL_RESULT_COLUMN_4));
    }
}
