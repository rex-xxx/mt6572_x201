
package com.mediatek.contacts.list;

import android.app.Activity;
import android.content.Intent;
import android.provider.ContactsContract.Intents.UI;
import android.util.Log;

import com.android.contacts.list.ContactsIntentResolver;
import com.android.contacts.list.ContactsRequest;
import com.mediatek.contacts.util.ContactsIntent;

/**
 * A sub class to extend the ContactsIntentResolver parses
 * {@link com.mediatek.contacts.util.ContactsIntent} not defined in Android
 * source code.
 * <p>
 * ContactsIntentResolver parses a Contacts intent, extracting all relevant
 * parts and packaging them as a
 * {@link com.android.contacts.list.ContactsRequest} object.
 * </p>
 */

public class ContactsIntentResolverEx extends ContactsIntentResolver {

    private static final String TAG = "ContactsIntentResolverEx";

    public static final int REQ_TYPE_IMPORT_EXPORT_PICKER = 1;

    private static final int REQ_TYPE_VCARD_PICKER = 3;

    /** Mask for picking multiple contacts of packing vCard */
    public static final int MODE_MASK_VCARD_PICKER = 0x01000000;

    /** Mask for picking multiple contacts of import/export */
    public static final int MODE_MASK_IMPORT_EXPORT_PICKER = 0x02000000;

    public ContactsIntentResolverEx(Activity context) {
        super(context);
    }

    @Override
    public ContactsRequest resolveIntent(Intent intent) {

        if (ContactsIntent.contain(intent)) {

            String action = intent.getAction();

            Log.i(TAG, "Called with action: " + action);
            ContactsRequest request = new ContactsRequest();
            if (ContactsIntent.LIST.ACTION_PICK_MULTICONTACTS.equals(action)) {
                request.setActionCode(ContactsRequest.ACTION_PICK_MULTIPLE_CONTACTS);
                int requestType = intent.getIntExtra("request_type", 0);

                switch (requestType) {
                    case REQ_TYPE_VCARD_PICKER:
                        request.setActionCode(ContactsRequest.ACTION_PICK_MULTIPLE_CONTACTS
                                | MODE_MASK_VCARD_PICKER);
                        break;

                    case REQ_TYPE_IMPORT_EXPORT_PICKER:
                        request.setActionCode(ContactsRequest.ACTION_PICK_MULTIPLE_CONTACTS
                                | MODE_MASK_IMPORT_EXPORT_PICKER);
                        break;

                    default:
                        break;
                }
            } else if (ContactsIntent.LIST.ACTION_PICK_MULTIEMAILS.equals(action)) {
                request.setActionCode(ContactsRequest.ACTION_PICK_MULTIPLE_EMAILS);
            } else if (ContactsIntent.LIST.ACTION_PICK_MULTIPHONES.equals(action)) {
                request.setActionCode(ContactsRequest.ACTION_PICK_MULTIPLE_PHONES);
            } else if (ContactsIntent.LIST.ACTION_DELETE_MULTICONTACTS.equals(action)) {
                request.setActionCode(ContactsRequest.ACTION_DELETE_MULTIPLE_CONTACTS);
            } else if (ContactsIntent.LIST.ACTION_GROUP_MOVE_MULTICONTACTS.equals(action)) {
                request.setActionCode(ContactsRequest.ACTION_GROUP_MOVE_MULTIPLE_CONTACTS);
            } else if (ContactsIntent.LIST.ACTION_PICK_MULTIPHONEANDEMAILS.equals(action)) {
                request.setActionCode(ContactsRequest.ACTION_PICK_MULTIPLE_PHONEANDEMAILS);
            } else if (ContactsIntent.LIST.ACTION_SHARE_MULTICONTACTS.equals(action)) {
                request.setActionCode(ContactsRequest.ACTION_SHARE_MULTIPLE_CONTACTS);
            } else if (ContactsIntent.LIST.ACTION_GROUP_ADD_MULTICONTACTS.equals(action)) {
                request.setActionCode(ContactsRequest.ACTION_GROUP_ADD_MULTIPLE_CONTACTS);
            } else if (ContactsIntent.LIST.ACTION_PICK_MULTIDATAS.equals(action)) {
                request.setActionCode(ContactsRequest.ACTION_PICK_MULTIPLE_DATAS);
            }

            // Allow the title to be set to a custom String using an extra on
            // the intent
            String title = intent.getStringExtra(UI.TITLE_EXTRA_KEY);
            if (title != null) {
                request.setActivityTitle(title);
            }
            return request;
        }

        return super.resolveIntent(intent);
    }
}
