package com.android.systemui.statusbar.toolbar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.SystemProperties;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.TelephonyProperties;
import com.android.systemui.R;
import com.android.systemui.statusbar.util.SIMHelper;
import com.mediatek.telephony.SimInfoManager;
import com.mediatek.xlog.Xlog;

import java.util.ArrayList;
import java.util.List;

/**
 * M: Support "Notification toolbar".
 * AlretDialog used for DISPLAY TEXT commands.
 */
public class SimIconsListView extends ListView {
    private static final String TAG = "SimIconsListView";
    private static final boolean DBG = true;
    
    private Context mContext;
    private static final String CUMCCMNC = "46001";
    private List<SimItem> mSimItems = new ArrayList<SimItem>();
    private long mSelectedSimId;
    private String mServiceType;
    private SimInfotListAdapter mSimInfotListAdapter;

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(TelephonyIntents.ACTION_SIM_INDICATOR_STATE_CHANGED)) {
                initSimList();
                notifyDataChange();
            }
        }
    };
    
    public SimIconsListView(Context context, String serviceType) {
        super(context, null);
        mContext = context;
        mServiceType = serviceType;
        initListViews();
    }
    
    private void initListViews() {
        IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(TelephonyIntents.ACTION_SIM_INDICATOR_STATE_CHANGED);
        mContext.registerReceiver(mIntentReceiver, mIntentFilter);
        setCacheColorHint(0);
        initSimList();
        mSimInfotListAdapter = new SimInfotListAdapter(mContext);
        setAdapter(mSimInfotListAdapter);
    }
    
    private static class SimInfoViewHolder {
        TextView mSimType;
        TextView mSimShortNumber;
        ImageView mSimStatus;
        TextView mSimOpName;
        TextView mSimNumber;
        RadioButton mSimSelectedRadio;
        RelativeLayout mSimBg;
        
    }
    
    private class SimInfotListAdapter extends BaseAdapter {
        public SimInfotListAdapter(Context context) {
            mContext = context;
            mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        private Context mContext;
        private LayoutInflater mInflater;

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            SimInfoViewHolder simInfoViewHolder;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.toolbar_dialog_sim_icon, null);
                simInfoViewHolder = new SimInfoViewHolder();
                simInfoViewHolder.mSimBg = (RelativeLayout) convertView.findViewById(R.id.sim_icon_bg);
                simInfoViewHolder.mSimType = (TextView) convertView.findViewById(R.id.sim_type);
                simInfoViewHolder.mSimShortNumber = (TextView) convertView.findViewById(R.id.sim_short_number);
                simInfoViewHolder.mSimStatus = (ImageView) convertView.findViewById(R.id.sim_status);
                simInfoViewHolder.mSimOpName = (TextView) convertView.findViewById(R.id.sim_op_name);
                simInfoViewHolder.mSimNumber = (TextView) convertView.findViewById(R.id.sim_number);
                simInfoViewHolder.mSimSelectedRadio = (RadioButton) convertView.findViewById(R.id.enable_state);
                convertView.setTag(simInfoViewHolder);
            } else {
                simInfoViewHolder = (SimInfoViewHolder)convertView.getTag();
            }
            SimItem simItem = mSimItems.get(position);
            if (!simItem.mIsSim) {
                if (simItem.mColor == 8) {
                    simInfoViewHolder.mSimBg.setVisibility(View.VISIBLE);
                    simInfoViewHolder.mSimBg.setBackgroundResource(com.mediatek.internal.R.drawable.sim_background_sip);
                } else {
                    simInfoViewHolder.mSimBg.setVisibility(View.GONE);
                }
                simInfoViewHolder.mSimOpName.setText(simItem.mName);
                simInfoViewHolder.mSimNumber.setVisibility(View.GONE);
                simInfoViewHolder.mSimBg.setVisibility(View.GONE);
                simInfoViewHolder.mSimType.setVisibility(View.GONE);
            } else {
                simInfoViewHolder.mSimBg.setVisibility(View.VISIBLE);
                simInfoViewHolder.mSimBg.setBackgroundResource(simItem.mColor);
                simInfoViewHolder.mSimOpName.setText(simItem.mName);
                if (simItem.mState == com.android.internal.telephony.PhoneConstants.SIM_INDICATOR_RADIOOFF) {
                    simInfoViewHolder.mSimOpName.setTextColor(Color.GRAY);
                } else {
                    simInfoViewHolder.mSimOpName.setTextColor(Color.WHITE);
                }
                if (simItem.mNumber != null && simItem.mNumber.length() > 0) {
                    simInfoViewHolder.mSimNumber.setVisibility(View.VISIBLE);
                    simInfoViewHolder.mSimNumber.setText(simItem.mNumber);
                    if (simItem.mState == com.android.internal.telephony.PhoneConstants.SIM_INDICATOR_RADIOOFF) {
                        simInfoViewHolder.mSimNumber.setTextColor(Color.GRAY);
                    } else {
                        simInfoViewHolder.mSimNumber.setTextColor(Color.WHITE);
                    }
                } else {
                    simInfoViewHolder.mSimNumber.setVisibility(View.GONE);
                }
                simInfoViewHolder.mSimStatus.setImageResource(SIMHelper.getSIMStateIcon(simItem.mState));
                simInfoViewHolder.mSimShortNumber.setText(simItem.getFormatedNumber());
                simInfoViewHolder.mSimType.setVisibility(View.GONE);
            }
            simInfoViewHolder.mSimSelectedRadio.setChecked(simItem.mSimID == mSelectedSimId);
            if (DBG) {
                Xlog.d(TAG, "getVIew called, simItem's simId is " + simItem.mSimID + ", mSelectedSimId is "
                        + mSelectedSimId);
                Xlog.d(TAG, "getVIew called, simItem's simColor is " + simItem.mColor);
            }

            if (simItem.mIsSim) {
                int simState = SIMHelper.getSimIndicatorStateGemini(simItem.mSlot);
                if ((simState == PhoneConstants.SIM_INDICATOR_RADIOOFF)) {
                    Xlog.d(TAG, "simItem is radio off");
                    simInfoViewHolder.mSimOpName.setEnabled(false);
                    simInfoViewHolder.mSimNumber.setEnabled(false);
                    simInfoViewHolder.mSimSelectedRadio.setEnabled(false);
                    convertView.setEnabled(false);
                } else {
                    Xlog.d(TAG, "simItem is not radio off");
                    simInfoViewHolder.mSimOpName.setEnabled(true);
                    simInfoViewHolder.mSimNumber.setEnabled(true);
                    simInfoViewHolder.mSimSelectedRadio.setEnabled(true);
                    convertView.setEnabled(true);
                }
            }

            if (simItem.mSimID == Settings.System.GPRS_CONNECTION_SIM_SETTING_NEVER) {
                boolean shouldEnable = false;
                for (int i = 0; i < mSimItems.size(); i++) {
                    SimItem simItemTemp = mSimItems.get(i);
                    if (simItemTemp.mIsSim) {
                        if (simItemTemp.mState != PhoneConstants.SIM_INDICATOR_RADIOOFF) {
                            shouldEnable = true;
                            break;
                        }
                    }
                }
                if (!shouldEnable) {
                    simInfoViewHolder.mSimOpName.setTextColor(Color.GRAY);
                } else {
                    simInfoViewHolder.mSimOpName.setTextColor(Color.WHITE);
                }
                simInfoViewHolder.mSimSelectedRadio.setEnabled(shouldEnable);
                convertView.setEnabled(shouldEnable);
            }
            return convertView;
        }

        @Override
        public int getCount() {
            return mSimItems.size();
        }

        @Override
        public SimItem getItem(int position) {
            return mSimItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }
    }
    
    class SimItem {
        public boolean mIsSim = true;
        public String mName = null;
        public String mNumber = null;
        public int mDispalyNumberFormat = 0;
        public int mColor = -1;
        public int mSlot = -1;
        public long mSimID = -1;
        public int mState = PhoneConstants.SIM_INDICATOR_NORMAL;
        public boolean mIsCU = true;

        /// M: Constructor for not real sim
        public SimItem(String name, int color, long simID) {
            mName = name;
            mColor = color;
            mIsSim = false;
            mSimID = simID;
        }

        /// M: constructor for sim
        public SimItem(SimInfoManager.SimInfoRecord siminfo) {
            mIsSim = true;
            mName = siminfo.mDisplayName;
            mNumber = siminfo.mNumber;
            mDispalyNumberFormat = siminfo.mDispalyNumberFormat;
            mColor = siminfo.mSimBackgroundDarkRes;
            mSlot = siminfo.mSimSlotId;
            mSimID = siminfo.mSimInfoId;
        }

        private String getFormatedNumber() {
            if (mNumber == null || mNumber.isEmpty()) {
                return "";
            }
            if (DBG) {
                Xlog.d(TAG, "getFormatedNumber called, mNumber is " + mNumber);
            }
            switch (mDispalyNumberFormat) {
            case (SimInfoManager.DISPLAY_NUMBER_FIRST):
                if (mNumber.length() <= 4) {
                    return mNumber;
                }
                return mNumber.substring(0, 4);
            case (SimInfoManager.DISPLAY_NUMBER_LAST):
                if (mNumber.length() <= 4) {
                    return mNumber;
                }
                return mNumber.substring(mNumber.length() - 4, mNumber.length());
            case (SimInfoManager.DISPALY_NUMBER_NONE):
                return "";
            default:
                return "";
            }
        }
    }
    
    public void initSimList() {
        mSelectedSimId = SIMHelper.getDefaultSIM(mContext, mServiceType);
        /// M: initialize the default sim preferences
        mSimItems.clear();
        SimItem simitem;
        SimInfoManager.SimInfoRecord simInfo;
        
        List<SimInfoManager.SimInfoRecord> simList = SIMHelper.getSIMInfoList(mContext);
        if (simList == null || simList.size() == 0) {
            return;
        }
        for (int i = 0; i < simList.size(); i++) {
            simInfo = simList.get(i);
            String numeric = "";
            /// M: Support GeminiPlus
            if (simInfo.mSimSlotId == PhoneConstants.GEMINI_SIM_2) {
                numeric = SystemProperties.get(TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC_2);
            } else if (simInfo.mSimSlotId == PhoneConstants.GEMINI_SIM_3) {
                ///numeric = SystemProperties.get(TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC_3);
            } else if (simInfo.mSimSlotId == PhoneConstants.GEMINI_SIM_4) {
                ///numeric = SystemProperties.get(TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC_4);
            } else {
                numeric = SystemProperties.get(TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC);
            }
            if (numeric.equals(CUMCCMNC)) {
                Xlog.d(TAG, "IsAllCUCard = true;");
            }
        }

        if (mServiceType.equals(Settings.System.VIDEO_CALL_SIM_SETTING)) {
            simInfo = SimInfoManager.getSimInfoBySlot(mContext, PhoneConstants.GEMINI_SIM_1);
            if (simInfo != null) {
                simitem = new SimItem(simInfo);
                int state = SIMHelper.getSimIndicatorStateGemini(simInfo.mSimSlotId);
                simitem.mState = state;
                mSimItems.add(simitem);
            }
            return;
        }

        for (int i = 0; i < simList.size(); i++) {
            simInfo = simList.get(i);
            if (simInfo != null) {
                simitem = new SimItem(simInfo);
                int state = SIMHelper.getSimIndicatorStateGemini(simInfo.mSimSlotId);
                simitem.mState = state;
                mSimItems.add(simitem);
            }
        }
        
        if (mServiceType.equals(Settings.System.GPRS_CONNECTION_SIM_SETTING)) {
            simitem = new SimItem(mContext.getString(R.string.gemini_default_sim_never), -1,
                    Settings.System.GPRS_CONNECTION_SIM_SETTING_NEVER);
            mSimItems.add(simitem);
        }
    }
    
    public void notifyDataChange() {
        if (mSimInfotListAdapter != null) {
            mSimInfotListAdapter.notifyDataSetChanged();
        }
    }

    public void updateResources() {
        if (mSimItems != null && mSimItems.size() != 0) {
            if (mServiceType.equals(Settings.System.GPRS_CONNECTION_SIM_SETTING)) {
                mSimItems.get(mSimItems.size() - 1).mName = mContext.getString(R.string.gemini_default_sim_never);
            }
        }
    }
}
