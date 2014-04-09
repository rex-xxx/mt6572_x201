package com.mediatek.contacts.extension;

import android.util.Log;

import com.android.contacts.ext.QuickContactExtension;

import java.util.Iterator;
import java.util.LinkedList;

public class QuickContactExtensionContainer extends QuickContactExtension {
    private static final String TAG = "QuickContactExtensionContainer";

    private LinkedList<QuickContactExtension> mSubExtensionList;

    public void add(QuickContactExtension extension) {
        if (null == mSubExtensionList) {
            mSubExtensionList = new LinkedList<QuickContactExtension>();
        }
        mSubExtensionList.add(extension);
    }

    public void remove(QuickContactExtension extension) {
        if (null == mSubExtensionList) {
            return;
        }
        mSubExtensionList.remove(extension);
    }

    public boolean collapseListPhone(final String mimeType, String commd) {
        Log.i(TAG, "[collapseListPhone]");
        if (null == mSubExtensionList) {
            return true;
        } else {
            Iterator<QuickContactExtension> iterator = mSubExtensionList.iterator();
            while (iterator.hasNext()) {
                boolean result = iterator.next().collapseListPhone(mimeType, commd);
                if (!result) {
                    return result;
                }
            }
        }
        return true;
    }
}
