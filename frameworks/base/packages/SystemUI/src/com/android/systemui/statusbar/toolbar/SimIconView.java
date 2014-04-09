package com.android.systemui.statusbar.toolbar;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.TouchDelegate;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.systemui.R;
import com.android.systemui.statusbar.policy.TelephonyIcons;
import com.android.systemui.statusbar.util.SIMHelper;
import com.mediatek.telephony.SimInfoManager;
import com.mediatek.xlog.Xlog;

/**
 * M: Support "Notification toolbar".
 */
public class SimIconView extends LinearLayout {
    private static final String TAG = "SimIconView";
    
    private int mSelectedIconColor = 0;
    private int mSimBackground = 0;
    private TextView mSimName;
    private TextView mSimOpName;
    private TextView mSimType;
    private ImageView mSimIcon;
    private ImageView mSimStateView;
    private ImageView mOnIndicator;
    private boolean mSelected;
    private int mSlotId = -1;

    public SimIconView(Context context) {
        this(context, null);
    }

    public SimIconView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mSelected = false;
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        mSimIcon = (ImageView) findViewById(R.id.sim_icon);
        mSimName = (TextView) findViewById(R.id.sim_name);
        mSimType = (TextView) findViewById(R.id.sim_type);
        mSimStateView = (ImageView) findViewById(R.id.sim_state);
        mSimOpName = (TextView) findViewById(R.id.sim_op_name);
        mOnIndicator = (ImageView) findViewById(R.id.on_indicator);
    }

    public void updateSimIcon(SimInfoManager.SimInfoRecord info) {
        Xlog.d(TAG, "updateSimIcon called, simName is " + info.mDisplayName + ", simNumber is " + info.mNumber);
        if (info.mNumber != null && !info.mNumber.isEmpty()) {
            mSimName.setText(getFormatedNumber(info.mNumber, info.mDispalyNumberFormat));
        } else {
            mSimName.setText("");
        }
        mSimBackground = info.mSimBackgroundDarkRes;
        setSelected(mSelected);
        mSimOpName.setText(info.mDisplayName);
        int slotId = info.mSimSlotId;
        int simState = SIMHelper.getSimIndicatorStateGemini(slotId);
        Xlog.d(TAG, "updateSimIcon called, simState is " + simState + ", slotId is " + slotId);
        updateSimState(simState);
    }
    
    public void updateSimState(int simState) {
        int resId = SIMHelper.getSIMStateIcon(simState);
        if (resId > -1) {
            mSimStateView.setImageResource(resId);
        }
    }

    public void set3GIconVisibility(boolean visible) {
        mSimType.setVisibility(visible ? VISIBLE : GONE);
    }
    
    public void setOpName(int resId) {
        mSimOpName.setText(resId);
    }
    
    public TextView getOpName() {
        return mSimOpName;
    }
    
    public ImageView getSimIcon() {
        return mSimIcon;
    }

    private String getFormatedNumber(String number, int format) {
        switch (format) {
        case (SimInfoManager.DISPLAY_NUMBER_FIRST):
            if (number.length() <= 4) {
                return number;
            }
            return number.substring(0, 4);
        case (SimInfoManager.DISPLAY_NUMBER_LAST):
            if (number.length() <= 4) {
                return number;
            }
            return number.substring(number.length() - 4, number.length());
        case (SimInfoManager.DISPALY_NUMBER_NONE):
            return "";
        default:
            return "";
        }
    }
    
    public void setSimIconViewResource(int resId) {
        mSimBackground = resId;
        setSelected(mSelected);
    }

    public boolean isSelected() {
        return mSelected;
    }


    public void setSelected(boolean selected) {
        mSelected = selected;
        if (selected && mSelectedIconColor >= 0 && mSelectedIconColor < TelephonyIcons.SIM_INDICATOR_BACKGROUND.length) {
            if (mSelectedIconColor > 3) {
                mSimIcon.setBackgroundResource(TelephonyIcons.SIM_INDICATOR_BACKGROUND[mSelectedIconColor]);
            } else if (mSelectedIconColor <= 3 && mSelectedIconColor >= 0) {
                mOnIndicator.setImageResource(TelephonyIcons.SIM_INDICATOR_BACKGROUND[mSelectedIconColor]);
            }
            mOnIndicator.setVisibility(View.VISIBLE);
        } else {
            mSimIcon.setBackgroundResource(mSimBackground);
            mOnIndicator.setVisibility(View.GONE);
        }
    }

    public void setSlotId(int id) {
        mSlotId = id;
    }

    public int getSlotId() {
        return mSlotId;
    }
    
    public void setSimColor(int color) {
        mSelectedIconColor = color;
    }

}
