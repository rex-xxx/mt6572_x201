package com.mediatek.settings.ext;

import android.content.Context;
import android.widget.Spinner;

public class DefaultWifiApDialogExt implements IWifiApDialogExt {
    private static final String TAG = "DefaultWifiApDialogExt";

    public void setAdapter(Context context, Spinner spinner, int arrayId) {
    }
    public int getSelection(int index) {
        return index ;
    }
    public int getSecurityType(int position) {
        return position;
    }
}
