package com.mediatek.contacts.extension;

import android.util.Log;
import android.widget.ImageView;

import com.android.contacts.ext.CallListExtension;

import java.util.Iterator;
import java.util.LinkedList;

public class CallListExtensionContainer extends CallListExtension {

    private static final String TAG = "CallListExtensionContainer";

    private LinkedList<CallListExtension> mSubExtensionList;

    public void add(CallListExtension extension) {
        if (null == mSubExtensionList) {
            mSubExtensionList = new LinkedList<CallListExtension>();
        }
        mSubExtensionList.add(extension);
    }

    public void remove(CallListExtension extension) {
        if (null == mSubExtensionList) {
            return;
        }
        mSubExtensionList.remove(extension);
    }

    public int layoutExtentionIcon(int leftBound, int topBound, int bottomBound, int rightBound,
            int mGapBetweenImageAndText, ImageView mExtentionIcon, String commd) {
        Log.i(TAG, "[layoutExtentionIcon]");
        if (null == mSubExtensionList) {
            return rightBound;
        } else {
            Iterator<CallListExtension> iterator = mSubExtensionList.iterator();
            while (iterator.hasNext()) {
                int result = iterator.next().layoutExtentionIcon(leftBound, topBound, bottomBound,
                        rightBound, mGapBetweenImageAndText, mExtentionIcon, commd);
                if (result != rightBound) {
                    return result;
                }
            }
        }
        return rightBound;
    }

    public void measureExtention(ImageView mExtentionIcon, String commd) {
        if (null == mSubExtensionList) {
            return;
        }
        Iterator<CallListExtension> iterator = mSubExtensionList.iterator();
        while (iterator.hasNext()) {
            iterator.next().measureExtention(mExtentionIcon, commd);
        }
    }

    public boolean setExtentionIcon(String number, String commd) {
        Log.i(TAG, "[setExtentionIcon]");
        if (null == mSubExtensionList) {
            return false;
        } else {
            Iterator<CallListExtension> iterator = mSubExtensionList.iterator();
            while (iterator.hasNext()) {
                boolean result = iterator.next().setExtentionIcon(number, commd);
                if (result) {
                    return result;
                }
            }
        }
        return false;
    }

    public void setExtentionImageView(ImageView view, String commd) {
        Log.i(TAG, "[setExtentionIcon]");
        if (null == mSubExtensionList) {
            return;
        }
        Iterator<CallListExtension> iterator = mSubExtensionList.iterator();
        while (iterator.hasNext()) {
            iterator.next().setExtentionImageView(view, commd);
        }
    }

    public boolean checkPluginSupport(String commd) {
        Log.i(TAG, "[checkPluginSupport]");
        if (null == mSubExtensionList) {
            return false;
        } else {
            Iterator<CallListExtension> iterator = mSubExtensionList.iterator();
            while (iterator.hasNext()) {
                boolean result = iterator.next().checkPluginSupport(commd);
                if (result) {
                    return result;
                }
            }
        }
        return false;
    }
}
