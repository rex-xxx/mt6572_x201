
package com.mediatek.contacts.model;

import android.os.Parcel;
import android.os.Parcelable.Creator;
import android.util.Log;

import com.android.contacts.model.account.AccountType;
import com.android.contacts.model.account.AccountWithDataSet;
import com.android.internal.util.Objects;
import com.mediatek.contacts.simcontact.SimCardUtils;
import com.mediatek.contacts.simcontact.SlotUtils;
import com.mediatek.phone.SIMInfoWrapper;

public class AccountWithDataSetEx extends AccountWithDataSet {
    public static final String TAG = "Contacts/AWDSE";

    public int mSlotId;

    public AccountWithDataSetEx(String name, String type, int slot) {
        this(name, type, null);
        mSlotId = slot;
    }

    public AccountWithDataSetEx(String name, String type, String dataSet) {
        super(name, type, dataSet);
        mSlotId = SlotUtils.getNonSlotId();
        Log.i(TAG, "AccountWithDataSetEx - name:" + name + "type:" + type + " slot:" + mSlotId);
    }

    public AccountWithDataSetEx(Parcel in) {
        super(in);
        this.mSlotId = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(mSlotId);
    }

    // For Parcelable
    public static final Creator<AccountWithDataSetEx> CREATOR = new Creator<AccountWithDataSetEx>() {
        public AccountWithDataSetEx createFromParcel(Parcel source) {
            return new AccountWithDataSetEx(source);
        }

        public AccountWithDataSetEx[] newArray(int size) {
            return new AccountWithDataSetEx[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        return (o instanceof AccountWithDataSetEx)
                && super.equals(o)
                && Objects.equal(Integer.valueOf(((AccountWithDataSetEx) o).mSlotId), Integer
                        .valueOf(mSlotId));
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + (mSlotId == 0 ? 0 : mSlotId);
    }

    @Override
    public String toString() {
        return "AccountWithDataSetEx {name=" + name + ", type=" + type + ", dataSet=" + dataSet
                + ", SlotId=" + mSlotId + "}";
    }

    public int getSlotId() {
        Log.i(TAG, "AccountWithDataSetEx: getSlot - slot:" + mSlotId);
        return mSlotId;
    }

    public String getDisplayName() {
        // TODO: to save in member or not
        String displayName = null;
        SIMInfoWrapper sw = SIMInfoWrapper.getDefault();
        if (null != sw) {
            displayName = sw.getSimDisplayNameBySlotId(mSlotId);
        } else {
            Log.i(TAG, "AccountWithDataSetEx: Error!! - SIMInfoWrapper instance is null!");
        }
        Log.i(TAG, "AccountWithDataSetEx: getDisplayName - displayName:" + displayName 
                + " slotId:" + mSlotId);
        return displayName;
    }

    public static boolean isLocalPhone(String accountType) {
        return AccountType.ACCOUNT_TYPE_LOCAL_PHONE.equals(accountType);
    }

}
