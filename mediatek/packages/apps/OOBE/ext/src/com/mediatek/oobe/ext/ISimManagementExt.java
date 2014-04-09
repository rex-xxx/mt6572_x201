package com.mediatek.oobe.ext;

import android.preference.DialogPreference;

public interface ISimManagementExt {    
    /**
     *update default SIM Summary.
     *@Param preferenceFragment
     *@dialogID
     **/
    void updateDefaultSIMSummary(DialogPreference pref, Long simID);

    /**
     *set Preference Property.
     *@Param preferenceFragment
     *@dialogID
     **/
    void setPrefProperty(DialogPreference pref, long simId);

    /**
     *get Auto String.
     **/
    String getAutoString();

    /**
     *is Need set Auto Item.
     *@Param preferenceFragment
     *@dialogID
     **/
    public boolean isNeedsetAutoItem();
}
