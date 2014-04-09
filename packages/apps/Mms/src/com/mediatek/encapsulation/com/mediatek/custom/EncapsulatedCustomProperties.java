package com.mediatek.encapsulation.com.mediatek.custom;

import com.mediatek.encapsulation.EncapsulationConstant;
import com.mediatek.custom.CustomProperties;

public class EncapsulatedCustomProperties {

    public static final String MODULE_MMS = EncapsulationConstant.USE_MTK_PLATFORM ?
                                                    CustomProperties.MODULE_MMS : "mms";
    public static final String USER_AGENT = EncapsulationConstant.USE_MTK_PLATFORM ?
                                                    CustomProperties.USER_AGENT : "UserAgent";
    public static final String UAPROF_URL = EncapsulationConstant.USE_MTK_PLATFORM ?
                                                    CustomProperties.UAPROF_URL : "UAProfileURL";

    public static String getString(String module, String name, String defaultValue) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return CustomProperties.getString(module, name, defaultValue);
        } else {
            return defaultValue;
        }
    }
}
