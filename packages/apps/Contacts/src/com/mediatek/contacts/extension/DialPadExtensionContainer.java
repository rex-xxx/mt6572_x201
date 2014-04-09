package com.mediatek.contacts.extension;

import android.util.Log;

import com.android.contacts.ext.DialPadExtension;

import java.util.Iterator;
import java.util.LinkedList;

public class DialPadExtensionContainer extends DialPadExtension {

    private static final String TAG = "DialPadExtensionContainer";

    private LinkedList<DialPadExtension> mSubExtensionList;

    public void add(DialPadExtension extension) {
        if (null == mSubExtensionList) {
            mSubExtensionList = new LinkedList<DialPadExtension>();
        }
        mSubExtensionList.add(extension);
    }

    public void remove(DialPadExtension extension) {
        if (null == mSubExtensionList) {
            return;
        }
        mSubExtensionList.remove(extension);
    }

    public String changeChar(String string, String string2, String commd) {
        Log.i(TAG, "[changeChar] string : " + string + " | string2 : " + string2);
        if (null == mSubExtensionList) {
            return string2;
        } else {
            Iterator<DialPadExtension> iterator = mSubExtensionList.iterator();
            while (iterator.hasNext()) {
                final String result = iterator.next().changeChar(string, string2, commd);
                if (result != null) {
                    return result;
                }
            }
        }
        return string2;
    }

}
