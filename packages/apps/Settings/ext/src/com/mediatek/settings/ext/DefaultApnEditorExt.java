package com.mediatek.settings.ext;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.preference.DialogPreference;



public class DefaultApnEditorExt implements IApnEditorExt {
    private static final String TAG = "DefaultWifiApEnablerExt";

    public void setPreferenceState(DialogPreference preference) {
    }
    public Uri getUriFromIntent(Context context, Intent intent) {
        return context.getContentResolver().insert(intent.getData(), new ContentValues());
    }    

    public String[] getApnTypeArray(Context context, int orangeId, int cmccId, int normalId) {
        return context.getResources().getStringArray(normalId);
    }

    public String[] getApnTypeArrayByCard(Context context, String numeric, boolean isTether,
            int tether, int cmcc, int generic, String[] oldArray) {
        String[] resArray = null;
        //if (isCmccCard(numeric)) {
        //    resArray = context.getResources().getStringArray(cmcc);
        //} else {
            resArray = oldArray;
        //}
        return resArray;
    }

    private boolean isCmccCard(String numeric) {
        return CMCC_CARD_1.equals(numeric) || 
               CMCC_CARD_2.equals(numeric) || 
               CMCC_CARD_3.equals(numeric);
    }
}
