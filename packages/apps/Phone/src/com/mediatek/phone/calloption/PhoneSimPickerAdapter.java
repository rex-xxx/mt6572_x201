package com.mediatek.phone.calloption;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.phone.R;
import com.mediatek.calloption.SimPickerAdapter;

public class PhoneSimPickerAdapter extends SimPickerAdapter {

    /*public PhoneSimPickerAdapter() {
    }*/

    public PhoneSimPickerAdapter(Context context, long suggestedSimId, boolean isMultiSim) {
        super(context, suggestedSimId, isMultiSim);
    }

    protected View createView(SimPickerAdapter.ViewHolder holder, final int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = null;
        if (viewType == SimPickerAdapter.ITEM_TYPE_SIM) {
            view = inflater.inflate(R.layout.sim_picker_item, null);
            holder.mSimSignal = (TextView)view.findViewById(R.id.simSignal);
            holder.mSimStatus = (ImageView)view.findViewById(R.id.simStatus);
            holder.mShortPhoneNumber = (TextView)view.findViewById(R.id.shortPhoneNumber);
            holder.mDisplayName = (TextView)view.findViewById(R.id.displayName);
            holder.mPhoneNumber = (TextView)view.findViewById(R.id.phoneNumber);
            holder.mSimIcon = view.findViewById(R.id.simIcon);
            holder.mSuggested = (ImageView)view.findViewById(R.id.suggested);
        } else if (viewType == SimPickerAdapter.ITEM_TYPE_INTERNET) {
            view = inflater.inflate(R.layout.sim_picker_item_internet, null);
            holder.mInternetIcon = (ImageView)view.findViewById(R.id.internetIcon);
        } else if (viewType == SimPickerAdapter.ITEM_TYPE_TEXT
                || viewType == SimPickerAdapter.ITEM_TYPE_ACCOUNT) {
            view = inflater.inflate(R.layout.sim_picker_item_text, null);
            holder.mText = (TextView)view.findViewById(R.id.text);
        }
        return view;
    }
}
