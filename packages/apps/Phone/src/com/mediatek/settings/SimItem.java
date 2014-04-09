package com.mediatek.settings;

import android.provider.Telephony.SIMInfo;

import com.android.internal.telephony.PhoneConstants;

public class SimItem {
        public boolean mIsSim = true;
        public SIMInfo mSiminfo;
        public int mState = PhoneConstants.SIM_INDICATOR_NORMAL;
        //constructor for sim
        public SimItem(SIMInfo siminfo) {
                this.mSiminfo = siminfo;
                if (siminfo == null) {
                    mIsSim = false;
                } else {
                    mIsSim = true;
                }
        }
}

