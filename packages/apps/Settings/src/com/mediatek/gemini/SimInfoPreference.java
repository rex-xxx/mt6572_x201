package com.mediatek.gemini;

import android.content.Context;
import android.os.SystemProperties;
import android.preference.Preference;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.android.settings.R;

import com.mediatek.xlog.Xlog;

public class SimInfoPreference extends Preference {

    private int mStatus;

    private String mSimNum;
    protected final int mSlotIndex;
    private String mName;
    private int mColor;
    private int mNumDisplayFormat;
    private boolean mChecked = true;
    private boolean mNeedCheckbox = true;
    private boolean mNeedStatus = true;
    private boolean mUseCheckBox = false;
    private Context mContext;

    private static final int DISPLAY_NONE = 0;
    private static final int DISPLAY_FIRST_FOUR = 1;
    private static final int DISPLAY_LAST_FOUR = 2;
    private static final int NUMFORMAT = 4;
    private static final String TAG = "SimInfoPreference";

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
     * @param needCheckBox
     *            boolean
     */
    public SimInfoPreference(Context context, String name, String number,
            int simSlot, int status, int color, int displayNumberFormat,
            long key, boolean needCheckBox) {
        this(context, name, number, simSlot, status, color,
                displayNumberFormat, key, needCheckBox, true);

    }

    /**
     * because modify the switch to checkbox, but some ui still need to use
     * checkbox, therefore add one more constructor to use the layout with
     * checkbox available.
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
     * @param needCheckBox
     *            boolean
     * @param needStatus
     *            boolean
     * @param useCheckBox
     *            boolean
     */
    public SimInfoPreference(Context context, String name, String number,
            int simSlot, int status, int color, int displayNumberFormat,
            long key, boolean needCheckBox, boolean needStatus,
            boolean useCheckBox) {

        super(context, null);
        mName = name;
        mSimNum = number;
        mSlotIndex = simSlot;
        mStatus = status;
        mColor = color;
        mNumDisplayFormat = displayNumberFormat;
        mNeedCheckbox = needCheckBox;
        mNeedStatus = needStatus;
        mContext = context;
        mUseCheckBox = useCheckBox;
        setKey(String.valueOf(key));

        setLayoutResource(R.layout.preference_sim_info_checkbox);

        if (mName != null) {
            setTitle(mName);
        }
        if ((mSimNum != null) && (mSimNum.length() != 0)) {
            setSummary(mSimNum);

        }

    }

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
     * @param needCheckBox
     *            boolean
     * @param needStatus
     *            boolean
     */
    public SimInfoPreference(Context context, String name, String number,
            int simSlot, int status, int color, int displayNumberFormat,
            long key, boolean needCheckBox, boolean needStatus) {

        super(context, null);
        mName = name;
        mSimNum = number;
        mSlotIndex = simSlot;
        mStatus = status;
        mColor = color;
        mNumDisplayFormat = displayNumberFormat;
        mNeedCheckbox = needCheckBox;
        mNeedStatus = needStatus;
        mContext = context;
        setKey(String.valueOf(key));

        setLayoutResource(R.layout.preference_sim_info);

        if (mName != null) {
            setTitle(mName);
        }
        if ((mSimNum != null) && (mSimNum.length() != 0)) {
            setSummary(mSimNum);

        }

    }

    @Override
    public View getView(View convertView, ViewGroup parent) {
        View view = super.getView(convertView, parent);
        TextView textTitle = (TextView) view.findViewById(android.R.id.title);
        if ((textTitle != null) && (mName != null)) {
            textTitle.setText(mName);
        }
        TextView textNum = (TextView) view.findViewById(android.R.id.summary);
        if (textNum != null) {
            if ((mSimNum != null) && (mSimNum.length() != 0)) {
                if (!textNum.isShown()) {
                    textNum.setVisibility(View.VISIBLE);
                }
                textNum.setText(mSimNum);
            } else {
                textNum.setVisibility(View.GONE);
            }
        }
        ImageView imageStatus = (ImageView) view.findViewById(R.id.simStatus);
        if (imageStatus != null) {
            if (mNeedStatus) {
                int res = GeminiUtils.getStatusResource(mStatus);
                if (res == -1) {
                    imageStatus.setVisibility(View.GONE);
                } else {
                    imageStatus.setImageResource(res);
                }
            } else {
                imageStatus.setVisibility(View.GONE);
            }
        }
        RelativeLayout viewSim = (RelativeLayout) view
                .findViewById(R.id.simIcon);
        if (viewSim != null) {
            int res = GeminiUtils.getSimColorResource(mColor);

            if (res < 0) {
                viewSim.setBackgroundDrawable(null);
            } else {
                viewSim.setBackgroundResource(res);
            }
        }
        Xlog.i(TAG, "mUseCheckBox=" + mUseCheckBox + " mChecked=" + mChecked);
        if (!mUseCheckBox) {
            Switch ckRadioOn = (Switch) view.findViewById(R.id.Check_Enable);
            if (ckRadioOn != null) {
                if (mNeedCheckbox) {
                        ckRadioOn.setChecked(mChecked);
                } else {
                    ckRadioOn.setVisibility(View.GONE);
                }
            }
        } else {
            CheckBox ckRadioOn = (CheckBox) view
                    .findViewById(R.id.Check_Enable);
            if (ckRadioOn != null) {
                if (mNeedCheckbox) {
                    ckRadioOn.setChecked(mChecked);
                } else {
                    ckRadioOn.setVisibility(View.GONE);
                }
            }
        }

        TextView textNumForShort = (TextView) view.findViewById(R.id.simNum);
        if ((textNum != null) && (mSimNum != null)) {
            switch (mNumDisplayFormat) {
            case DISPLAY_NONE:
                textNumForShort.setVisibility(View.GONE);
                break;
            case DISPLAY_FIRST_FOUR:
                if (mSimNum.length() >= NUMFORMAT) {
                    textNumForShort.setText(mSimNum.substring(0, NUMFORMAT));
                } else {
                    textNumForShort.setText(mSimNum);
                }
                break;
            case DISPLAY_LAST_FOUR:
                if (mSimNum.length() >= NUMFORMAT) {
                    textNumForShort.setText(mSimNum.substring(mSimNum.length()
                            - NUMFORMAT));
                } else {
                    textNumForShort.setText(mSimNum);
                }
                break;
            default:
                break;
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

    /**
     * 
     * @param isNeed
     *            boolean
     */
    public void setNeedCheckBox(boolean isNeed) {
        mNeedCheckbox = isNeed;
        notifyChanged();
    }

}
