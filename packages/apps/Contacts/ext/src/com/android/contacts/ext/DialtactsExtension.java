package com.android.contacts.ext;

import android.content.Intent;
import android.util.Log;

public class DialtactsExtension {
    private static final String TAG = "DialtactsExtension";

    public boolean checkComponentName(Intent intent, String commd) {
        return false;
    }

    public boolean startActivity(String commd) {
        Log.i(TAG, "DialerSearchAdapter: [startActivity()]");
        return false;
    }
}
