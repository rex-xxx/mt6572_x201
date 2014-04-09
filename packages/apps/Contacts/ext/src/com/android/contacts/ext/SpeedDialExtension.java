package com.android.contacts.ext;

import android.util.Log;
import android.view.View;

public class SpeedDialExtension {
    private static final String TAG = "SpeedDialExtension";

    public void setView(View view, int viewId, boolean mPrefNumContactState, int sdNumber,
            String commd) {

    }

    public int setAddPosition(int mAddPosition, boolean mNeedRemovePosition, String commd) {
        return mAddPosition;
    }

    public boolean needClearPreState(String commd) {
        return true;
    }

    public boolean showSpeedInputDialog(String commd) {
        return false;
    }

    public boolean needClearSharedPreferences(String commd) {
        return true;
    }

    public boolean clearPrefStateIfNecessary(String commd) {
        Log.i(TAG, "SpeedDialManageActivity: [clearPrefStateIfNecessary]");
        return true;
    }

    public boolean needCheckContacts(String commd) {
        Log.i(TAG, "SpeedDialManageActivity: [needCheckContacts()]");
        return true;
    }

    
}
