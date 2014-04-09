package com.mediatek.contacts.extension;

import android.util.Log;
import android.widget.TextView;

import com.android.contacts.ext.SimPickExtension;

import java.util.Iterator;
import java.util.LinkedList;

public class SimPickExtensionContainer extends SimPickExtension {

    private static final String TAG = "SimPickExtensionContainer";

    private LinkedList<SimPickExtension> mSubExtensionList;

    public void add(SimPickExtension extension) {
        if (null == mSubExtensionList) {
            mSubExtensionList = new LinkedList<SimPickExtension>();
        }
        mSubExtensionList.add(extension);
    }

    public void remove(SimPickExtension extension) {
        if (null == mSubExtensionList) {
            return;
        }
        mSubExtensionList.remove(extension);
    }

    public void setSimSignal(TextView mSimSignal, int mSlot, int m3GCapabilitySIM) {
        Log.i(TAG, "[setSimSignal()]");
        if (null == mSubExtensionList) {
            return;
        }
        Iterator<SimPickExtension> iterator = mSubExtensionList.iterator();
        while (iterator.hasNext()) {
            iterator.next().setSimSignal(mSimSignal, mSlot, m3GCapabilitySIM);
        }
    }
}
