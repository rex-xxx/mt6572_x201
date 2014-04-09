package com.hissage.contact;

import java.util.ArrayList;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Data;

import com.hissage.contact.NmsContact.NmsContactType;
import com.hissage.db.NmsContentResolver;
import com.hissage.service.NmsService;
import com.hissage.util.log.NmsLog;

public class NmsContactManager {

    private String TAG = "NmsContactManager";

    public static final int TYPE_ALL = 0;
    public static final int TYPE_HISSAGE = 1;
    public static final int TYPE_NOT_HISSAGE = 2;

    private static NmsContactManager mInstance = null;
    private static Context mContext;

    private ArrayList<NmsUIContact> nmsUiContacts;
    private boolean mIsRefresh;
    private boolean mIsCancel;

    private NmsContactManager() {
        nmsUiContacts = new ArrayList<NmsUIContact>();
        mIsRefresh = true;
        mIsCancel = false;
    }

    public synchronized static NmsContactManager getInstance(Context context) {
        if (context == null) {
            mContext = NmsService.getInstance();
        } else {
            mContext = context;
        }

        if (null == mInstance) {
            mInstance = new NmsContactManager();
        }

        return mInstance;
    }

    public void setRefresh(boolean isRefresh) {
        mIsRefresh = isRefresh;
    }

    public void setCancel(boolean isCancel) {
        mIsCancel = isCancel;
    }

    private void checkContactCache() {
        if (mIsRefresh || mIsCancel) {
            mIsRefresh = false;
            mIsCancel = false;
            initContactsCache();
        }
    }

    public ArrayList<NmsUIContact> getContacts(int type) {
        if (nmsUiContacts == null) {
            NmsLog.error(TAG, "getContacts. nmsUiContacts is null");
            return null;
        }

        checkContactCache();

        ArrayList<NmsUIContact> contacts = new ArrayList<NmsUIContact>();
        switch (type) {
        case TYPE_ALL:
            contacts.addAll(nmsUiContacts);
            break;
        case TYPE_HISSAGE:
        case TYPE_NOT_HISSAGE:
            int typeUser = type == TYPE_HISSAGE ? NmsContactType.HISSAGE_USER
                    : NmsContactType.NOT_HISSAGE_USER;
            for (int i = 0; i < nmsUiContacts.size(); ++i) {
                NmsUIContact contact = nmsUiContacts.get(i);
                if (contact != null && contact.getType() == typeUser) {
                    contacts.add(contact);
                }
            }
            break;
        default:
            break;
        }

        return contacts;
    }

    private synchronized ArrayList<NmsUIContact> initContactsCache() {
        if (nmsUiContacts == null) {
            NmsLog.error(TAG, "initContactsCache. nmsUiContacts is null");
            return null;
        } else {
            nmsUiContacts.clear();
        }

        Cursor cursor = null;

        try {
            cursor = NmsContentResolver.query(mContext.getContentResolver(), Data.CONTENT_URI,
                    new String[] { Data.CONTACT_ID, Data.DISPLAY_NAME, Data.MIMETYPE, Data.DATA1,
                            Data.DATA2, Data.DATA3, Data.SORT_KEY_PRIMARY }, Data.MIMETYPE + "='"
                            + Phone.CONTENT_ITEM_TYPE + "' OR " + Data.MIMETYPE + "='"
                            + Email.CONTENT_ITEM_TYPE + "'", null, Data.SORT_KEY_PRIMARY
                            + " COLLATE LOCALIZED ASC, " + Data.CONTACT_ID + " ASC");
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    NmsUIContact uiContact = NmsUIContact.getUiContactViaCursor(mContext, cursor);
                    if (uiContact != null) {
                        nmsUiContacts.add(uiContact);
                    } else {
                        NmsLog.error(TAG, "uiContact is null");
                    }
                } while (cursor.moveToNext() && !mIsCancel);
            }
        } catch (Exception e) {
            NmsLog.nmsPrintStackTrace(e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return nmsUiContacts;
    }

}
