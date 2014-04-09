package com.mediatek.contacts.quickcontact;

import android.content.Context;
import android.provider.ContactsContract.Data;
import android.util.Log;

import com.android.contacts.model.dataitem.DataItem;
import com.android.contacts.quickcontact.DataAction;

public class QuickDataAction extends DataAction {

    private int mSimId = 0;
    private static final String TAG = "QuickDataAction";

    public QuickDataAction(Context context, DataItem item, boolean isDirectoryEntry) {
        super(context, item);
        if (isDirectoryEntry) {
            mSimId = item.getContentValues().getAsInteger(Data.SIM_ASSOCIATION_ID);
        }
        Log.i(TAG, "mSimId : " + mSimId + " , isDirectoryEntry : " + isDirectoryEntry);
    }

    public int getSimId() {
        return mSimId;
    }
}
