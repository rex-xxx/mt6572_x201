package com.mediatek.phone;

import android.content.Context;
import android.preference.Preference;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.phone.PhoneGlobals;
import com.android.phone.R;
import com.mediatek.phone.Utils;

public class SimInfoPreference extends Preference {

    private int mStatus;

    private String mSimNum;
    protected final int mSlotIndex;
    private String mName;
    private int mColor;
    private int mNumDisplayFormat;
    private boolean mChecked = true;
    private boolean mNeedCheckbox = true;
    private Context mContext;

    private static final int DISPLAY_NONE = 0;
    private static final int DISPLAY_FIRST_FOUR = 1;
    private static final int DISPLAY_LAST_FOUR = 2;
    private static final String TAG = "SimInfoPreference";

    public SimInfoPreference(Context context, String name, String number, int SimSlot, int status, int color,
            int DisplayNumberFormat, long key, boolean needCheckBox) {
        super(context, null);
        mName = name;
        mSimNum = number;
        mSlotIndex = SimSlot;
        mStatus = status;
        mColor = color;
        mNumDisplayFormat = DisplayNumberFormat;
        mNeedCheckbox = needCheckBox;
        mContext = context;
        setKey(String.valueOf(key));

        setLayoutResource(R.layout.preference_sim_info);

        displayNameAndNumber();

    }

    public void updateInfo(String name, String number, int color, int DisplayNumberFormat) {
        mName = name;
        mSimNum = number;

        mColor = color;
        mNumDisplayFormat = DisplayNumberFormat;
        displayNameAndNumber();
    }

    private void displayNameAndNumber() {

        if (mName != null) {
            setTitle(mName);
        }
        if ((mSimNum != null) && (mSimNum.length() != 0)) {
            setSummary(mSimNum);
            Log.i(TAG, "mSimNum is " + mSimNum);
        } else {
            setSummary(null);
            Log.i(TAG, "mSimNum is null");
        }
    }

    @Override
    public View getView(View convertView, ViewGroup parent) {
        // TODO Auto-generated method stub
        View view = super.getView(convertView, parent);
        displayNameAndNumber();

        ImageView imageStatus = (ImageView) view.findViewById(R.id.simStatus);

        if (imageStatus != null) {
            int res = Utils.getStatusResource(mStatus);

            if (res == -1) {
                imageStatus.setVisibility(View.GONE);
            } else {
                imageStatus.setImageResource(res);
            }
        }

        int simId = PhoneGlobals.getInstance().phoneMgr.get3GCapabilitySIM();
        Log.i(TAG, "mSlotIndex is: " + mSlotIndex);
        Log.i(TAG, "simId is: " + simId);

        TextView text3G = (TextView) view.findViewById(R.id.sim3g);
        if (text3G != null) {
            if ((simId == -1) || (simId != mSlotIndex) /*|| Utils.mSupport3G==false*/) {
                text3G.setVisibility(View.GONE);
            }
        }

        RelativeLayout viewSim = (RelativeLayout) view.findViewById(R.id.simIcon);

        if (viewSim != null) {
            int res = Utils.getSimColorResource(mColor);

            if (res < 0) {
                viewSim.setBackgroundDrawable(null);
            } else {
                viewSim.setBackgroundResource(res);
            }
        }

        CheckBox ckRadioOn = (CheckBox) view.findViewById(R.id.Check_Enable);
        Log.i(TAG, "ckRadioOn.setChecked " + mChecked);
        if (ckRadioOn != null) {
            if (mNeedCheckbox == true) {
                ckRadioOn.setChecked(mChecked);
            } else {
                ckRadioOn.setVisibility(View.GONE);
            }
        }

        TextView textNum = (TextView) view.findViewById(R.id.simNum);
        if ((textNum != null) && (mSimNum != null)) {

            switch (mNumDisplayFormat) {
            case DISPLAY_NONE: {
                textNum.setVisibility(View.GONE);
                break;

            }
            case DISPLAY_FIRST_FOUR: {

                if (mSimNum.length() >= 4) {
                    textNum.setText(mSimNum.substring(0, 4));
                } else {
                    textNum.setText(mSimNum);
                }
                break;
            }
            case DISPLAY_LAST_FOUR: {

                if (mSimNum.length() >= 4) {
                    textNum.setText(mSimNum.substring(mSimNum.length() - 4));
                } else {
                    textNum.setText(mSimNum);
                }
                break;
            }
            }
        }

        return view;
    }

    void setCheck(boolean bCheck) {
        mChecked = bCheck;
        notifyChanged();
    }

    boolean getCheck() {
        return mChecked;

    }

    void setStatus(int status) {
        mStatus = status;
        notifyChanged();
    }

    void setName(String name) {
        mName = name;
        notifyChanged();

    }

    void setColor(int color) {
        mColor = color;
        notifyChanged();
    }

    void setNumDisplayFormat(int format) {
        mNumDisplayFormat = format;
        notifyChanged();
    }

    void setNumber(String number) {
        mSimNum = number;
        notifyChanged();
    }
}
