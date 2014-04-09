package com.mediatek.gemini;

import android.content.Context;
import android.provider.Telephony;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.settings.R;

import java.util.ArrayList;

public class GeminiSIMTetherAdapter extends BaseAdapter {
    private static final String TAG = "GeminiSIMTetherAdapter";
    public static final int FLAG_SIM_STATUS_1 = 1;
    public static final int FLAG_SIM_STATUS_2 = 2;
    public static final int FLAG_CHECKBOX_STSTUS_NONE = -1;
    public static final int FLAG_CHECKBOX_STSTUS_UNCHECKED = 0;
    public static final int FLAG_CHECKBOX_STSTUS_CHECKED = 1;
    private static final int BGCOLOR_SIM_ABSENT = 10;
    private static final int MAX_COLORSIZE = 7;

    private ArrayList<GeminiSIMTetherItem> mDataList;
    private LayoutInflater mInflater;
    private Context mContext;
    private GeminiSIMTetherItem mItem;
    private boolean mIsShowCheckBox = false;
    private int mSIMCardNamePadding;

    /**
     * Constructor of GeminiSIMTetherAdapter
     * 
     * @param context
     *            Context
     * @param data
     *            ArrayList<GeminiSIMTetherItem>
     * @param showCheckBox
     *            boolean
     */
    public GeminiSIMTetherAdapter(Context context,
            ArrayList<GeminiSIMTetherItem> data, boolean showCheckBox) {
        mContext = context;
        this.mDataList = data;
        this.mIsShowCheckBox = showCheckBox;
        mInflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mSIMCardNamePadding = context.getResources().getDimensionPixelSize(
                            com.mediatek.internal.R.dimen.sim_card_name_padding);

    }

    /**
     * set checkout box state
     * 
     * @param showCheckBox
     *            boolean
     */
    public void setShowCheckBox(boolean showCheckBox) {
        this.mIsShowCheckBox = showCheckBox;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mDataList != null ? mDataList.size() : 0;
    }

    @Override
    public Object getItem(int position) {
        return mDataList != null ? mDataList.get(position) : null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ItemViewHolder holder;
        String simName = "";
        int simColor = -1;
        if (convertView == null) {
            holder = new ItemViewHolder();
            convertView = mInflater.inflate(
                    R.layout.gemini_sim_tether_info_item, null);

            holder.mName = (TextView) convertView
                    .findViewById(R.id.gemini_contact_name);
            holder.mCheckBox = (CheckBox) convertView
                    .findViewById(R.id.gemini_contact_check_btn);
            holder.mPhoneNum = (TextView) convertView
                    .findViewById(R.id.gemini_contact_phone_num);
            holder.mSimInfoLayout = (LinearLayout) convertView
                    .findViewById(R.id.gemini_contact_sim_status_layout);
            holder.mSimInfo = (TextView) convertView
                    .findViewById(R.id.gemini_contact_sim_status);
            holder.mPhoneNumType = (TextView) convertView
                    .findViewById(R.id.gemini_contact_phone_num_type);

            convertView.setTag(holder);
        } else {
            holder = (ItemViewHolder) convertView.getTag();
        }
        mItem = mDataList.get(position);
        simName = mDataList.get(position).getSimName();
        simColor = mDataList.get(position).getSimColor();
        if (mItem != null && holder != null) {
            holder.mName.setText(mItem.getName());
            holder.mPhoneNumType.setText(mItem.getPhoneNumType());
            holder.mPhoneNum.setText(mItem.getPhoneNum());
            if (simName == null || simName.equals("")) {
                holder.mSimInfo.setVisibility(View.GONE);
                holder.mSimInfoLayout.setVisibility(View.GONE);
            } else {
                holder.mSimInfoLayout.setVisibility(View.VISIBLE);
                holder.mSimInfo.setVisibility(View.VISIBLE);
                holder.mSimInfo.setText(simName);
            }

            // set check box status, visible/invisible, checked/unchecked
            int checkStatus = mDataList.get(position).getCheckedStatus();
            if (mIsShowCheckBox) {
                holder.mCheckBox.setVisibility(View.VISIBLE);
                holder.mCheckBox
                        .setChecked(checkStatus == FLAG_CHECKBOX_STSTUS_CHECKED ? true
                                : false);
            } else {
                holder.mCheckBox.setVisibility(View.GONE);
            }

            // set SIM card status background
            if (simColor >= 0 && simColor <= MAX_COLORSIZE) {
                holder.mSimInfo
                        .setBackgroundResource(Telephony.SIMBackgroundDarkRes[simColor]);
                holder.mSimInfo.setPadding(mSIMCardNamePadding, 0, mSIMCardNamePadding, 0);
            } else if (simColor == BGCOLOR_SIM_ABSENT) {
                holder.mSimInfo
                        .setBackgroundResource(com.mediatek.internal.R.drawable.sim_background_locked);
                holder.mSimInfo.setPadding(mSIMCardNamePadding, 0, mSIMCardNamePadding, 0);
            }
        }
        return convertView;
    }

    static class ItemViewHolder {
        TextView mName;
        TextView mPhoneNumType;
        TextView mPhoneNum;
        LinearLayout mSimInfoLayout;
        TextView mSimInfo;
        CheckBox mCheckBox;
    }
}
