package com.android.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.mediatek.common.MediatekClassFactory;
import com.mediatek.common.tvout.ITVOUTNative;

public class TVOutReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context arg0, Intent arg1) {
        // TvOut mTvOut = new TvOut();
        ITVOUTNative mTvOut = null;
        if (mTvOut == null) {
            mTvOut = MediatekClassFactory
                    .createInstance(ITVOUTNative.class);
        }
        mTvOut.IPOPowerOff();
    }

}
