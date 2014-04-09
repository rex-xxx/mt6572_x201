package com.mediatek.contacts.extension;

import android.util.Log;
import android.view.View;

import com.android.contacts.ext.SpeedDialExtension;

import java.util.Iterator;
import java.util.LinkedList;

public class SpeedDialExtensionContainer extends SpeedDialExtension {

    private static final String TAG = "SpeedDialExtensionContainer";

    private LinkedList<SpeedDialExtension> mSubExtensionList;

    public void add(SpeedDialExtension extension) {
        if (null == mSubExtensionList) {
            mSubExtensionList = new LinkedList<SpeedDialExtension>();
        }
        mSubExtensionList.add(extension);
    }

    public void remove(SpeedDialExtension extension) {
        if (null == mSubExtensionList) {
            return;
        }
        mSubExtensionList.remove(extension);
    }

    public void setView(View view, int viewId, boolean mPrefNumContactState, int sdNumber,
            String commd) {
        Log.i(TAG, "[setView()]");
        if (null == mSubExtensionList) {
            return;
        }
        Iterator<SpeedDialExtension> iterator = mSubExtensionList.iterator();
        while (iterator.hasNext()) {
            iterator.next().setView(view, viewId, mPrefNumContactState, sdNumber, commd);
        }
    }

    public int setAddPosition(int mAddPosition, boolean mNeedRemovePosition, String commd) {
        Log.i(TAG, "[setAddPosition()]");
        if (null == mSubExtensionList) {
            return mAddPosition;
        } else {
            Iterator<SpeedDialExtension> iterator = mSubExtensionList.iterator();
            while (iterator.hasNext()) {
                int result = iterator.next().setAddPosition(mAddPosition, mNeedRemovePosition,
                        commd);
                if (result != mAddPosition) {
                    return result;
                }
            }
        }
        return mAddPosition;
    }

    public boolean needClearPreState(String commd) {
        Log.i(TAG, "[needClearPreState()]");
        if (null == mSubExtensionList) {
            return true;
        } else {
            Iterator<SpeedDialExtension> iterator = mSubExtensionList.iterator();
            while (iterator.hasNext()) {
                boolean result = iterator.next().needClearPreState(commd);
                if (!result) {
                    return result;
                }
            }
        }
        return true;
    }

    public boolean showSpeedInputDialog(String commd) {
        Log.i(TAG, "[showSpeedInputDialog()]");
        if (null == mSubExtensionList) {
            return false;
        } else {
            Iterator<SpeedDialExtension> iterator = mSubExtensionList.iterator();
            while (iterator.hasNext()) {
                boolean result = iterator.next().showSpeedInputDialog(commd);
                if (result) {
                    return result;
                }
            }
        }
        return false;
    }

    public boolean needClearSharedPreferences(String commd) {
        Log.i(TAG, "needClearSharedPreferences()");
        if (null == mSubExtensionList) {
            Log.i(TAG, "[needClearSharedPreferences()");
            return true;
        } else {
            Iterator<SpeedDialExtension> iterator = mSubExtensionList.iterator();
            while (iterator.hasNext()) {
                boolean result = iterator.next().needClearSharedPreferences(commd);
                if (!result) {
                    Log.i(TAG, "needClearSharedPreferences()]");
                    return result;
                }
            }
        }
        Log.i(TAG, "[needClearSharedPreferences()]");
        return true;
    }

    public boolean clearPrefStateIfNecessary(String commd) {
        Log.i(TAG, "SpeedDialManageActivity: [clearPrefStateIfNecessary]");
        if (null == mSubExtensionList) {
            return true;
        } else {
            Iterator<SpeedDialExtension> iterator = mSubExtensionList.iterator();
            while (iterator.hasNext()) {
                boolean result = iterator.next().clearPrefStateIfNecessary(commd);
                if (!result) {
                    return result;
                }
            }
        }
        return true;
    }

    public boolean needCheckContacts(String commd) {
        Log.i(TAG, "SpeedDialManageActivity: [needCheckContacts]");
        if (null == mSubExtensionList) {
            return true;
        } else {
            Iterator<SpeedDialExtension> iterator = mSubExtensionList.iterator();
            while (iterator.hasNext()) {
                boolean result = iterator.next().needCheckContacts(commd);
                if (!result) {
                    return result;
                }
            }
        }
        return true;
    }    
}
