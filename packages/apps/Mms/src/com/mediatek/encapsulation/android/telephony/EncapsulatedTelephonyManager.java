
package com.mediatek.encapsulation.android.telephony;

import android.content.Context;
import android.telephony.TelephonyManager;
import com.mediatek.encapsulation.EncapsulationConstant;

public class EncapsulatedTelephonyManager {

    private static TelephonyManager mTelephonyManager;

    public EncapsulatedTelephonyManager(Context context) {
        if (null == mTelephonyManager && null != context) {
            mTelephonyManager = (TelephonyManager) context
                    .getSystemService(Context.TELEPHONY_SERVICE);
        }
    }

    public EncapsulatedTelephonyManager() {
    }

    /** M: MTK ADD */
    public String getLine1NumberGemini(int simId) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return mTelephonyManager.getLine1NumberGemini(simId);
        } else {
            /** M: Can not complete for this branch. */
            return null;
        }

    }

    /** M: MTK ADD */
    public boolean hasIccCardGemini(int simId) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return TelephonyManager.getDefault().hasIccCardGemini(simId);
        } else {
            /** M: Can not complete for this branch. */
            return false;
        }
    }

    /** M: MTK ADD */
    public boolean isNetworkRoamingGemini(int simId) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return TelephonyManager.getDefault().isNetworkRoamingGemini(simId);
        } else {
            /** M: Can not complete for this branch. */
            return false;
        }
    }

}
