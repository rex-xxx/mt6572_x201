package com.mediatek.contacts.util;

import android.net.Uri;
import android.provider.CallLog.Calls;

import com.mediatek.contacts.ContactsFeatureConstants.FeatureOption;

public class VvmUtils {

    /**
     * [VVM] whether vvm feature enabled on this device
     * @return ture if allowed to enable
     */
    public static boolean isVvmEnabled() {
        return FeatureOption.MTK_VVM_SUPPORT;
    }

    /**
     * To Create a new uri which can be allowed VVM interactions.
     * @param uri
     *            the origin uri
     * @return the new uri
     */
    public static Uri buildVvmAllowedUri(Uri uri) {
        if (uri != null) {
            return uri.buildUpon().appendQueryParameter(Calls.ALLOW_VOICEMAILS_PARAM_KEY, "true").build();
        }
        return uri;
    }
}