package com.mediatek.gemini;

import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.Switch;

import com.android.settings.R;

public class SimInfoEnablePreference extends SimInfoPreference implements
        OnClickListener {

    interface OnPreferenceClickCallback {

        void onPreferenceClick(long simid);
    }

    private static final String TAG = "SimInfoEnablePreference";
    private OnCheckedChangeListener mSwitchChangeListener;;
    private Context mContext;
    private boolean mRadioOn;
    private OnPreferenceClickCallback mClickCallback;
    private boolean mDisableSwitch;

    /**
     * 
     * @param context
     *            Context
     * @param name
     *            String
     * @param number
     *            String
     * @param simSlot
     *            int
     * @param status
     *            int
     * @param color
     *            int
     * @param displayNumberFormat
     *            int
     * @param key
     *            long
     * @param isAirModeOn
     *            boolean
     */
    public SimInfoEnablePreference(Context context, String name, String number,
            int simSlot, int status, int color, int displayNumberFormat,
            long key, boolean isAirModeOn) {
        super(context, name, number, simSlot, status, color,
                displayNumberFormat, key, true);
        mContext = context;
        mRadioOn = true;
        mDisableSwitch = isAirModeOn;
        setLayoutResource(R.layout.preference_sim_info_enabler);
    }

    @Override
    public View getView(View convertView, ViewGroup parent) {
        // TODO Auto-generated method stub
        View view = super.getView(convertView, parent);

        Switch ckRadioOn = (Switch) view.findViewById(R.id.Check_Enable);

        if (ckRadioOn != null) {
            if (mSwitchChangeListener != null) {
                ckRadioOn.setClickable(true);
                ckRadioOn.setEnabled(!mDisableSwitch);
                ckRadioOn.setOnCheckedChangeListener(mSwitchChangeListener);
            }
        }
        View siminfoLayout = view.findViewById(R.id.sim_info_layout);
        if ((siminfoLayout != null) && siminfoLayout instanceof LinearLayout) {
            siminfoLayout.setOnClickListener(this);
            // siminfoLayout.setFocusable(true);
        }

        return view;
    }

    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub

        if (v == null) {
            return;
        }

        if ((v.getId() != R.id.Check_Enable) && (mClickCallback != null)) {
            mClickCallback.onPreferenceClick(Long.valueOf(getKey()));
        }

    }

    void setCheckBoxClickListener(OnCheckedChangeListener listerner) {
        mSwitchChangeListener = listerner;

    }

    void setClickCallback(OnPreferenceClickCallback callBack) {
        mClickCallback = callBack;
    }

    boolean isRadioOn() {
        return mRadioOn;
    }

    void setRadioOn(boolean radioOn) {
        mRadioOn = radioOn;

    }

}
