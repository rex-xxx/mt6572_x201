package com.mediatek.providers.contacts;

import android.net.Uri;
import android.provider.CallLog.Calls;

import com.mediatek.providers.contacts.ContactsFeatureConstants.FeatureOption;

public class VvmUtils {

    /**
     * [VVM] whether vvm feature enabled on this device
     * @return ture if allowed to enable
     */
    public static boolean isVvmEnabled() {
        return FeatureOption.MTK_VVM_SUPPORT;
    }
}