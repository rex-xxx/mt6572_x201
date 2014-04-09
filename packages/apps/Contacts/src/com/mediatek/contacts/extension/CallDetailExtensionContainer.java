package com.mediatek.contacts.extension;

import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.android.contacts.ext.CallDetailExtension;

import java.util.Iterator;
import java.util.LinkedList;

public class CallDetailExtensionContainer extends CallDetailExtension {

    private static final String TAG = "CallDetailExtensionContainer";

    private LinkedList<CallDetailExtension> mSubExtensionList;

    public void add(CallDetailExtension extension) {
        if (null == mSubExtensionList) {
            mSubExtensionList = new LinkedList<CallDetailExtension>();
        }
        mSubExtensionList.add(extension);
    }

    public void remove(CallDetailExtension extension) {
        if (null == mSubExtensionList) {
            return;
        }
        mSubExtensionList.remove(extension);
    }

    public void setTextView(int callType, TextView durationView, String formatDuration, String commd) {
        Log.i(TAG, "[setTextView]");
        if (null != mSubExtensionList) {
            Iterator<CallDetailExtension> iterator = mSubExtensionList.iterator();
            while (iterator.hasNext()) {
                CallDetailExtension extension = iterator.next();
                if (extension.getCommand().equals(commd)) {
                    extension.setTextView(callType, durationView, formatDuration, commd);
                    return ;
                }
            }
        }
        super.setTextView(callType, durationView, formatDuration, commd);
    }

    public boolean isNeedAutoRejectedMenu(boolean isAutoRejectedFilterMode, String commd) {
        Log.i(TAG, "[isNeedAutoRejectedMenu]");
        if (null == mSubExtensionList) {
            return false;
        } else {
            Iterator<CallDetailExtension> iterator = mSubExtensionList.iterator();
            while (iterator.hasNext()) {
                boolean result = iterator.next().isNeedAutoRejectedMenu(isAutoRejectedFilterMode,
                        commd);
                if (result) {
                    return true;
                }
            }
        }
        return false;
    }

    public String setChar(boolean notSPChar, String str, String spChar, int charType,
            boolean secondSelection, String commd) {
        Log.i(TAG, "[setChar]");
        if (null == mSubExtensionList) {
            return null;
        } else {
            Iterator<CallDetailExtension> iterator = mSubExtensionList.iterator();
            while (iterator.hasNext()) {
                final String result = iterator.next().setChar(notSPChar, str, spChar, charType,
                        secondSelection, commd);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    /**
     * if plugin has special view can call this function to set visible
     */
    public void setViewVisibleByActivity(Activity activiy, String commd1, String commd2, int rse1,
            int res2, int res3, int res4, int res5, int res6, int res7, String commd) {
        Log.i(TAG, "[setViewVisibleByActivity]");
        if (null == mSubExtensionList) {
            return;
        }
        Iterator<CallDetailExtension> iterator = mSubExtensionList.iterator();
        while (iterator.hasNext()) {
            iterator.next().setViewVisibleByActivity(activiy, commd1, commd2, rse1, res2, res3,
                    res4, res5, res6, res7, commd);
        }
    }

    /**
     * if plugin has special view can call this function to set visible
     */
    public void setViewVisible(View view, String commd1, String commd2, int rse1, int res2,
            int res3, int res4, int res5, int res6, int res7) {
        Log.i(TAG, "[setViewVisible]");
        if (null == mSubExtensionList) {
            return;
        }
        Iterator<CallDetailExtension> iterator = mSubExtensionList.iterator();
        while (iterator.hasNext()) {
            iterator.next().setViewVisible(view, commd1, commd2, rse1, res2, res3, res4, res5,
                    res6, res7);
        }
    }
}
