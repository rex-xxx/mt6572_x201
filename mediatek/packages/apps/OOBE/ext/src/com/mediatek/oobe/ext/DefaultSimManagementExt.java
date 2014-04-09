package com.mediatek.oobe.ext;

import android.preference.DialogPreference;

import com.mediatek.xlog.Xlog;

public class DefaultSimManagementExt implements ISimManagementExt {
    public void updateDefaultSIMSummary(DialogPreference pref, Long simId) {
        return;
    }

    public void setPrefProperty(DialogPreference pref, long simID) {
        return;
    }

    public String getAutoString() {
        return null;
    }

    public boolean isNeedsetAutoItem() {
        return false;
    }
}
