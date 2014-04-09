package com.mediatek.contacts;

import android.accounts.Account;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.text.TextUtils;
import android.util.Log;

import com.android.contacts.ContactsUtils;

import java.util.ArrayList;
import java.util.Set;

public class SubContactsUtils extends ContactsUtils {
   
    private static final String TAG = "SubContactsUtils";

    public static long queryForRawContactId(ContentResolver cr, long contactId) {
        Cursor rawContactIdCursor = null;
        long rawContactId = -1;
        try {
            rawContactIdCursor = cr.query(RawContacts.CONTENT_URI,
                    new String[] {RawContacts._ID},
                    RawContacts.CONTACT_ID + "=" + contactId, null, null);
            if (rawContactIdCursor != null && rawContactIdCursor.moveToFirst()) {
                // Just return the first one.
                rawContactId = rawContactIdCursor.getLong(0);
            }
        } finally {
            if (rawContactIdCursor != null) {
                rawContactIdCursor.close();
            }
        }
        return rawContactId;
    }

    // For index in SIM change feature, we add the 'int indexInSim' argument 
    // into the argument list.

    public static Uri insertToDB(Account mAccount, String name, String number, String email,
            String additionalNumber, ContentResolver resolver, long indicate,
            String simType, long indexInSim, Set<Long> grpAddIds) {
        final ArrayList<ContentProviderOperation> operationList = 
                new ArrayList<ContentProviderOperation>();
        buildInsertOperation(operationList, mAccount, name, number, 
                email, additionalNumber, resolver, indicate, simType,
                indexInSim, grpAddIds);
        return insertToDBApplyBatch(resolver, operationList);
    }

    public static void buildInsertOperation(
            ArrayList<ContentProviderOperation> operationList,
            Account mAccount, String name, String number, String email,
            String additionalNumber, ContentResolver resolver, long indicate,
            String simType, long indexInSim, Set<Long> grpAddIds) {

        if (operationList == null) {
            return;
        }
        int backRef = operationList.size();
        //insert RawContacts info
        insertRawContacts(operationList, mAccount, indicate, indexInSim);
        
        int phoneType = 7;
        String phoneTypeSuffix = "";
        // ALPS00023212
        if (!TextUtils.isEmpty(name)) {
            final NamePhoneTypePair namePhoneTypePair = new NamePhoneTypePair(name);
            name = namePhoneTypePair.name;
            phoneType = namePhoneTypePair.phoneType;
            phoneTypeSuffix = namePhoneTypePair.phoneTypeSuffix;
        }
    
        // insert phone number
        insertPhoneNumber(number, operationList, backRef, phoneTypeSuffix);
        // insert name
        insertName(operationList, name, backRef);
        // if USIM
        if (simType.equals("USIM")) {
            // insert email
            insertEmail(operationList, email, backRef);
            // insert additionalNumber
            insertAdditionalNumber(operationList, additionalNumber, backRef);
            // for USIM Group
            insertGroup(operationList, grpAddIds, backRef);
        }
    }

    // mtk80909 for ALPS00023212
    public static class NamePhoneTypePair {
        public String name;
        public int phoneType;
        public String phoneTypeSuffix;
        public NamePhoneTypePair(String nameWithPhoneType) {
            // Look for /W /H /M or /O at the end of the name signifying the type
            int nameLen = nameWithPhoneType.length();
            if (nameLen - 2 >= 0 && nameWithPhoneType.charAt(nameLen - 2) == '/') {
                char c = Character.toUpperCase(nameWithPhoneType.charAt(nameLen - 1));
                phoneTypeSuffix = String.valueOf(nameWithPhoneType.charAt(nameLen - 1));
                if (c == 'W') {
                    phoneType = Phone.TYPE_WORK;
                } else if (c == 'M' || c == 'O') {
                    phoneType = Phone.TYPE_MOBILE;
                } else if (c == 'H') {
                    phoneType = Phone.TYPE_HOME;
                } else {
                    phoneType = Phone.TYPE_OTHER;
                }
                name = nameWithPhoneType.substring(0, nameLen - 2);
            } else {
                phoneTypeSuffix = "";
                phoneType = Phone.TYPE_OTHER;
                name = nameWithPhoneType;
            }
        }
    }

    public static void insertPhoneNumber(String number, ArrayList<ContentProviderOperation> operationList, int backRef,
            String phoneTypeSuffix) {
        if (!TextUtils.isEmpty(number)) {

            ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
            builder.withValueBackReference(Phone.RAW_CONTACT_ID, backRef);
            builder.withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
            builder.withValue(Phone.NUMBER, number);
            builder.withValue(Data.DATA2, 2);

            if (!TextUtils.isEmpty(phoneTypeSuffix)) {
                builder.withValue(Data.DATA15, phoneTypeSuffix);
            }
            operationList.add(builder.build());
        }
    }

    public static void insertAdditionalNumber(ArrayList<ContentProviderOperation> operationList, String additionalNumber,
            int backRef) {
        if (!TextUtils.isEmpty(additionalNumber)) {
            // additionalNumber =
            // PhoneNumberFormatUtilEx.formatNumber(additionalNumber);
            Log.i(TAG, "additionalNumber is " + additionalNumber);
            ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
            builder.withValueBackReference(Phone.RAW_CONTACT_ID, backRef);
            builder.withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
            // builder.withValue(Phone.TYPE, phoneType);
            builder.withValue(Data.DATA2, 7);
            builder.withValue(Phone.NUMBER, additionalNumber);
            builder.withValue(Data.IS_ADDITIONAL_NUMBER, 1);
            operationList.add(builder.build());
        }
    }

    public static void insertName(ArrayList<ContentProviderOperation> operationList, String name, int backRef) {
        if (!TextUtils.isEmpty(name)) {
            ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
            builder.withValueBackReference(StructuredName.RAW_CONTACT_ID, backRef);
            builder.withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
            builder.withValue(StructuredName.DISPLAY_NAME, name);
            operationList.add(builder.build());
        }
    }

    public static void insertEmail(ArrayList<ContentProviderOperation> operationList,String email,int backRef){
        if (!TextUtils.isEmpty(email)) {
            // for (String emailAddress : emailAddressArray) {
            Log.i(TAG, "In actuallyImportOneSimContact email is " + email);
            ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
            builder.withValueBackReference(Email.RAW_CONTACT_ID, backRef);
            builder.withValue(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE);
            builder.withValue(Email.TYPE, Email.TYPE_MOBILE);
            builder.withValue(Email.DATA, email);
            operationList.add(builder.build());
            // }
        }
    }

    public static void insertGroup(ArrayList<ContentProviderOperation> operationList,Set<Long> grpAddIds,int backRef){
        if (grpAddIds != null && grpAddIds.size() > 0) {
            Long[] grpIdArray = grpAddIds.toArray(new Long[0]);
            for (Long grpId : grpIdArray) {
                ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
                builder.withValueBackReference(Phone.RAW_CONTACT_ID, backRef);
                builder.withValue(Data.MIMETYPE, GroupMembership.CONTENT_ITEM_TYPE);
                builder.withValue(GroupMembership.GROUP_ROW_ID, grpId);
                operationList.add(builder.build());
            }
        }
    }

    public static void insertRawContacts(ArrayList<ContentProviderOperation> operationList, Account mAccount, long indicate,
            long indexInSim) {
        ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(RawContacts.CONTENT_URI);
        ContentValues contactvalues = new ContentValues();
        contactvalues.put(RawContacts.ACCOUNT_NAME, mAccount.name);
        contactvalues.put(RawContacts.ACCOUNT_TYPE, mAccount.type);
        contactvalues.put(RawContacts.INDICATE_PHONE_SIM, indicate);
        contactvalues.put(RawContacts.AGGREGATION_MODE, RawContacts.AGGREGATION_MODE_DISABLED);
        contactvalues.put(RawContacts.INDEX_IN_SIM, indexInSim); // index in SIM
        builder.withValues(contactvalues);
        operationList.add(builder.build());
    }

    public static Uri insertToDBApplyBatch(ContentResolver resolver,ArrayList<ContentProviderOperation> operationList){
        Uri retUri = null;
        try {
            ContentProviderResult[] result = resolver.applyBatch(
                    ContactsContract.AUTHORITY, operationList); //saved in database
            Uri rawContactUri = result[0].uri;
            Log.w(TAG, "[insertToDB]rawContactUri:" + rawContactUri);
            retUri = RawContacts.getContactLookupUri(resolver, rawContactUri);
            Log.w(TAG, "[insertToDB]retUri:" + retUri);
        } catch (RemoteException e) {
            Log.e(TAG, String.format("%s: %s", e.toString(), e
                    .getMessage()));
        } catch (OperationApplicationException e) {
            Log.e(TAG, String.format("%s: %s", e.toString(), e
                    .getMessage()));
        }
        return retUri;
    }

}
