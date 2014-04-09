package com.mediatek.contacts.extension;

import android.content.Intent;
import android.util.Log;

import com.android.contacts.ext.DialtactsExtension;

import java.util.Iterator;
import java.util.LinkedList;

public class DialtactsExtensionContainer extends DialtactsExtension {

    private static final String TAG = "DialtactsExtensionContainer";

    private LinkedList<DialtactsExtension> mSubExtensionList;

    public void add(DialtactsExtension extension) {
        if (null == mSubExtensionList) {
            mSubExtensionList = new LinkedList<DialtactsExtension>();
        }
        mSubExtensionList.add(extension);
    }

    public void remove(DialtactsExtension extension) {
        if (null == mSubExtensionList) {
            return;
        }
        mSubExtensionList.remove(extension);
    }

    public boolean checkComponentName(Intent intent, String commd) {
        Log.i(TAG, "[checkComponentName()]");
        if (null == mSubExtensionList) {
            return false;
        } else {
            Iterator<DialtactsExtension> iterator = mSubExtensionList.iterator();
            while (iterator.hasNext()) {
                boolean result = iterator.next().checkComponentName(intent, commd);
                if (result) {
                    return result;
                }
            }
        }
        return false;
    }

    public boolean startActivity(String commd) {
        Log.i(TAG, "startActivity DialerSearchAdapter: [startActivity()]");
        if (null == mSubExtensionList) {
            return false;
        } else {
            Iterator<DialtactsExtension> iterator = mSubExtensionList.iterator();
            while (iterator.hasNext()) {
                boolean result = iterator.next().startActivity(commd);
                if (result) {
                    return result;
                }
            }
        }
        return false;
    }
}
