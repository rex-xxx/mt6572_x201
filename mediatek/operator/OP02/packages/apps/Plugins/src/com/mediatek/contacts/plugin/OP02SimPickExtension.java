package com.mediatek.contacts.plugin;

import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.android.contacts.ext.SimPickExtension;

public class OP02SimPickExtension extends SimPickExtension {
    private static final String TAG = "OP02ContactEx";

    @Override
    public void setSimSignal(TextView mSimSignal, int mSlot, int m3GCapabilitySIM) {
        if (mSlot == m3GCapabilitySIM) {
            mSimSignal.setVisibility(View.VISIBLE);
            Log.i(TAG, "[setSimSignal] mSimSignal is visible");
        }
        Log.i(TAG, "[setSimSignal] mSimSignal is gone");
    }

}
