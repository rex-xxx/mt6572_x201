package com.mediatek.gemini;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.DialogPreference;
import android.provider.Telephony.SIMInfo;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.settings.R;

import com.mediatek.xlog.Xlog;

import java.util.ArrayList;
import java.util.List;

public class ColorPickerPreference extends DialogPreference implements
        OnClickListener {
    private static final String TAG = "ColorPicker";
    private final Context mContext;
    private static final int COLOR_SIZE = 8;
    private int mCurrentSelected = -1;
    private int mInitValue = -1;
    private long mSimID = -1;

    private final List<Integer> mCurrentUsed;

    private static final int COLORID[] = {R.id.color_00, R.id.color_01,
            R.id.color_02, R.id.color_03 };

    private static final int SIM_COLOR[] = { R.color.sim_blue,
            R.color.sim_orange, R.color.sim_green, R.color.sim_purple };

    /**
     * Construct of ColorPickerPreference
     * 
     * @param context
     *            the context
     *@param attrs
     *            the property
     * 
     */
    public ColorPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        setLayoutResource(R.layout.preference_color_picker);
        setDialogLayoutResource(R.layout.color_picker);
        setNegativeButtonText(android.R.string.cancel);
        mCurrentUsed = new ArrayList<Integer>();
    }

    /**
     * Set sim id
     * 
     * @param simID
     *            sim id
     */
    public void setSimID(long simID) {
        mSimID = simID;
    }

    @Override
    protected void onBindDialogView(View view) {
        // TODO Auto-generated method stub
        super.onBindDialogView(view);
        List<SIMInfo> simList = SIMInfo.getInsertedSIMList(mContext);
        for (SIMInfo siminfo : simList) {
            if (siminfo != null) {
                Xlog.i(TAG, "current used =" + Integer.valueOf(siminfo.mColor));
                ImageView iconView = (ImageView) view
                        .findViewById(COLORID[siminfo.mColor]);
                if (mSimID == siminfo.mSimId) {
                    mCurrentSelected = siminfo.mColor;
                    mInitValue = siminfo.mColor;
                    iconView.setBackgroundResource(R.drawable.color_selected);
                } else {
                    mCurrentUsed.add(Integer.valueOf(siminfo.mColor));
                    if (siminfo.mColor != mCurrentSelected) {
                        iconView.setBackgroundResource(R.drawable.color_used);
                    }
                }
            }
        }
        for (int k = 0; k < COLORID.length; k++) {
            ImageView iconView = (ImageView) view.findViewById(COLORID[k]);
            iconView.setOnClickListener(this);
        }
    }

    @Override
    protected void onPrepareDialogBuilder(Builder builder) {
        super.onPrepareDialogBuilder(builder);

        builder.setInverseBackgroundForced(true);
        builder.setPositiveButton(null, null);

    }

    @Override
    public void onClick(View arg0) {
        // TODO Auto-generated method stub

        int viewId = arg0.getId();

        if (arg0 instanceof ImageView) {

            for (int k = 0; k < COLORID.length; k++) {

                if (COLORID[k] == viewId) {
                    mCurrentSelected = k;
                    Xlog.i(TAG, "mCurrentSelected is " + k);
                }

            }

            onClick(getDialog(), DialogInterface.BUTTON_POSITIVE);
            getDialog().dismiss();
        }

    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        Xlog.i(TAG, "positiveResult is " + positiveResult
                + " mCurrentSelected is " + mCurrentSelected
                + " mInitValue is " + mInitValue);
        if (positiveResult && mCurrentSelected >= 0
                && (mCurrentSelected != mInitValue)) {

            callChangeListener(mCurrentSelected);
            notifyChanged();
        }
    }

    @Override
    public View getView(View convertView, ViewGroup parent) {
        // TODO Auto-generated method stub

        View view = super.getView(convertView, parent);

        if (view != null) {

            TextView textSummary = (TextView) view
                    .findViewById(android.R.id.summary);

            if (textSummary != null) {
                textSummary.setVisibility(View.GONE);
            }

            ImageView bgColor = (ImageView) view
                    .findViewById(R.id.sim_list_color);

            if (bgColor != null) {
                bgColor.setBackgroundColor(mContext.getResources().getColor(SIM_COLOR[mCurrentSelected]));
            }

        }
        return view;
    }

    /**
     * Set the color for sim card
     * 
     * @param colorIndex
     *            the index color used
     */
    public void setInitValue(int colorIndex) {
        mCurrentSelected = colorIndex;
        mInitValue = colorIndex;
    }

}
