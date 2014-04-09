package com.mediatek.contacts;

public class ContactsFeatureConstants {

    public interface FeatureOption {
        boolean MTK_SEARCH_DB_SUPPORT = com.mediatek.common.featureoption.FeatureOption.MTK_SEARCH_DB_SUPPORT;
        boolean MTK_DIALER_SEARCH_SUPPORT =
                com.mediatek.common.featureoption.FeatureOption.MTK_DIALER_SEARCH_SUPPORT;
        boolean MTK_GEMINI_SUPPORT = com.mediatek.common.featureoption.FeatureOption.MTK_GEMINI_SUPPORT;
        boolean MTK_VT3G324M_SUPPORT = com.mediatek.common.featureoption.FeatureOption.MTK_VT3G324M_SUPPORT;
        boolean MTK_GEMINI_3G_SWITCH = com.mediatek.common.featureoption.FeatureOption.MTK_GEMINI_3G_SWITCH;
        boolean MTK_PHONE_NUMBER_GEODESCRIPTION =
                com.mediatek.common.featureoption.FeatureOption.MTK_PHONE_NUMBER_GEODESCRIPTION;
        boolean MTK_THEMEMANAGER_APP = com.mediatek.common.featureoption.FeatureOption.MTK_THEMEMANAGER_APP;
        boolean MTK_BT_PROFILE_BPP = com.mediatek.common.featureoption.FeatureOption.MTK_BT_PROFILE_BPP;
        boolean MTK_DRM_APP = com.mediatek.common.featureoption.FeatureOption.MTK_DRM_APP;
        boolean MTK_BEAM_PLUS_SUPPORT = com.mediatek.common.featureoption.FeatureOption.MTK_BEAM_PLUS_SUPPORT;
        boolean MTK_VVM_SUPPORT = false; //[VVM] vvm is a Google default feature.
        boolean MTK_LCA_SUPPORT = com.mediatek.common.featureoption.FeatureOption.MTK_LCA_RAM_OPTIMIZE;
    }

    public static boolean DBG_DIALER_SEARCH = true;
    public static boolean DBG_CONTACTS_GROUP = true;
}
