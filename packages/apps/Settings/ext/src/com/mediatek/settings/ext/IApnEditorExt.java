package com.mediatek.settings.ext;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.preference.DialogPreference;

public interface IApnEditorExt {

    String CMCC_CARD_1 = "46000";
    String CMCC_CARD_2 = "46002";
    String CMCC_CARD_3 = "46007";

    /**
     * set state of preference 
     * @param preference The parent preference 
     */
    void setPreferenceState(DialogPreference preference);
    /**
     * get Uri from intent
     * @param context The parent context
     * @param intent The parent intent
     */
    Uri getUriFromIntent(Context context, Intent intent);

    /**
     * get array of the apn type 
     * @param context The parent context
     */
    String[] getApnTypeArray(Context context, int orangeId, int cmccId, int normalId);

    /**
     * get array of the apn type by cmcc card
     * @param context The parent context
     */
    String[] getApnTypeArrayByCard(Context context, String numeric, boolean isTether,
            int tether, int cmcc, int generic, String[] oldArray);
}
