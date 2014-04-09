package com.android.contacts.ext;

import android.util.Log;

public class DialPadExtension {
    private static final String TAG = "DialPadExtension";

    public String changeChar(String string, String string2, String commd) {
        Log.i(TAG, "[changeChar] string : " + string + " | string2 : " + string2);
        return string2;

    }

}
