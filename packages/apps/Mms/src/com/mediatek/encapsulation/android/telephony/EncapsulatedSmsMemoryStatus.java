
package com.mediatek.encapsulation.android.telephony;

import com.mediatek.common.telephony.IccSmsStorageStatus;
import android.os.Parcel;
import android.os.Parcelable;

import com.mediatek.encapsulation.EncapsulationConstant;

/// M: ALPS00510627, SMS Framewrok API refactoring, SmsMemoryStatus -> IccSmsStorageStatus
public class EncapsulatedSmsMemoryStatus  {
    private IccSmsStorageStatus mSmsMemoryStatus;
    /** M: MTK ADD */
    public int mUsed;
    public int mTotal;

    public EncapsulatedSmsMemoryStatus(IccSmsStorageStatus smsMemoryStatus) {
       if(smsMemoryStatus!=null){
          mSmsMemoryStatus = smsMemoryStatus;
       }
   }

    public IccSmsStorageStatus getSmsMemoryStatus() {
        return mSmsMemoryStatus;
    }

    /** M: MTK ADD */
    public EncapsulatedSmsMemoryStatus(int used, int total) {
        mUsed = used;
        mTotal = total;
    }

    /** M: MTK ADD */
    public int getUsed() {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return mSmsMemoryStatus.getUsed();
        } else {
            return 0;
        }
    }

    /** M: MTK ADD */
    public int getTotal() {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return mSmsMemoryStatus.getTotal();
        } else {
            return 0;
        }
    }
}
