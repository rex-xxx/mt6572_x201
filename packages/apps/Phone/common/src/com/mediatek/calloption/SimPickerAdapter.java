package com.mediatek.calloption;

import android.accounts.Account;
import android.content.Context;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.provider.Telephony;
import android.provider.Telephony.SIMInfo;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import com.android.internal.telephony.ITelephony;

import java.util.List;

public abstract class SimPickerAdapter extends BaseAdapter {

    public static final int ITEM_TYPE_UNKNOWN  = -1;
    public static final int ITEM_TYPE_SIM      =  0;
    public static final int ITEM_TYPE_INTERNET =  1;
    public static final int ITEM_TYPE_TEXT     =  2;
    public static final int ITEM_TYPE_ACCOUNT  =  3;
    private static final int NUMBER_LENGTH_MINUS = 4;
    private static final int VIEW_TYPE_COUNT = 4;

    protected Context mContext;
    protected long mSuggestedSimId;
    protected List<ItemHolder> mItems;
    protected boolean mIsMultiSim;

    public class ViewHolder {
        public View      mSimIcon;
        public ImageView mSimStatus;
        public TextView  mSimSignal;
        public TextView  mShortPhoneNumber;
        public TextView  mDisplayName;
        public TextView  mPhoneNumber;
        public ImageView  mSuggested;
        public TextView  mText;
        public ImageView mInternetIcon;
        public RadioButton mRadioButton;
    }

    public static class ItemHolder {
        public Object mData;
        public int mType;

        public ItemHolder(Object data, int type) {
            this.mData = data;
            this.mType = type;
        }
    }

    /*public SimPickerAdapter() {
    }*/

    public SimPickerAdapter(Context context, /*List<ItemHolder> items,*/ long suggestedSimId, boolean isMultiSim) {
        mContext = context;
        mSuggestedSimId = suggestedSimId;
        //mItems = items;
        mIsMultiSim = isMultiSim;
    }

    /*public void init(Context context, List<ItemHolder> items, long suggestedSimId, boolean isMultiSim) {
        mContext = context;
        mSuggestedSimId = suggestedSimId;
        mItems = items;
        mIsMultiSim = isMultiSim;
    }*/

    public void setItems(List<ItemHolder> items) {
        mItems = items;
    }

    public int getCount() {
        // TODO Auto-generated method stub
        return mItems.size();
    }

    @Override
    public int getViewTypeCount() {
        return VIEW_TYPE_COUNT;
    }
    
    @Override
    public int getItemViewType(int position) {
        // TODO Auto-generated method stub
        ItemHolder itemHolder = mItems.get(position);
        return itemHolder.mType;
    }

    public Object getItem(int position) {
        ItemHolder itemHolder = mItems.get(position);
        if (itemHolder.mType == ITEM_TYPE_SIM) {
            return Integer.valueOf(((SIMInfo)itemHolder.mData).mSlot);
        } else if (itemHolder.mType == ITEM_TYPE_INTERNET) {
            return Integer.valueOf((int)Settings.System.VOICE_CALL_SIM_SETTING_INTERNET);
        } else if (itemHolder.mType == ITEM_TYPE_TEXT || itemHolder.mType == ITEM_TYPE_ACCOUNT) {
            return itemHolder.mData;
        } else {
            return null;
        }
    }

    public long getItemId(int position) {
        return 0;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        ViewHolder holder = null;
        int viewType = getItemViewType(position);
        if (view == null) {
            holder = new ViewHolder();
            view = createView(holder, viewType);
            view.setTag(holder);
        }

        holder = (ViewHolder)view.getTag();

        if (viewType == ITEM_TYPE_SIM) {
            SIMInfo simInfo = (SIMInfo)mItems.get(position).mData;
            holder.mDisplayName.setText(simInfo.mDisplayName);
            holder.mSimIcon.setBackgroundResource(Telephony.SIMBackgroundDarkRes[simInfo.mColor]);

            if (simInfo.mSimId == mSuggestedSimId) {
                holder.mSuggested.setVisibility(View.VISIBLE);
            } else {
                holder.mSuggested.setVisibility(View.GONE);
            }

            String shortNumber = "";
            if (!TextUtils.isEmpty(simInfo.mNumber)) {
                switch (simInfo.mDispalyNumberFormat) {
                    case android.provider.Telephony.SimInfo.DISPLAY_NUMBER_FIRST:
                        if (simInfo.mNumber.length() <= NUMBER_LENGTH_MINUS) {
                            shortNumber = simInfo.mNumber;
                        } else {
                            shortNumber = simInfo.mNumber.substring(0, NUMBER_LENGTH_MINUS);
                        }
                        break;
                    case android.provider.Telephony.SimInfo.DISPLAY_NUMBER_LAST:
                        if (simInfo.mNumber.length() <= 4) {
                            shortNumber = simInfo.mNumber;
                        } else {
                            shortNumber = simInfo.mNumber.substring(simInfo.mNumber.length()
                                    - NUMBER_LENGTH_MINUS, simInfo.mNumber.length());
                        }
                        break;
                    case 0://android.provider.Telephony.SimInfo.DISPLAY_NUMBER_NONE:
                        shortNumber = "";
                        break;
                    default:
                        break;
                }
                holder.mPhoneNumber.setText(simInfo.mNumber);
                holder.mPhoneNumber.setVisibility(View.VISIBLE);
            } else {
                holder.mPhoneNumber.setVisibility(View.GONE);
            }
            holder.mShortPhoneNumber.setText(shortNumber);
            holder.mSimSignal.setVisibility(View.INVISIBLE);
            holder.mSimStatus.setImageResource(getSimStatusIcon(simInfo.mSlot));
        } else if (viewType == ITEM_TYPE_INTERNET) {
            holder.mInternetIcon
                    .setBackgroundResource(com.mediatek.internal.R.drawable.sim_background_sip);
        } else if (viewType == ITEM_TYPE_TEXT) {
            String text = (String)mItems.get(position).mData;
            holder.mText.setText(text);
        } else if (viewType == ITEM_TYPE_ACCOUNT) {
            Account account = (Account)mItems.get(position).mData;
            holder.mText.setText((String)account.name);
        }

        return view;
    }

    protected abstract View createView(ViewHolder holder, final int viewType);

    protected int getSimStatusIcon(int slot) {
        final ITelephony iTel = ITelephony.Stub.asInterface(ServiceManager
                .getService(Context.TELEPHONY_SERVICE));
        if (iTel == null) {
            return 0;
        }

        int state = -1;
        try {
            if (mIsMultiSim) {
                state = iTel.getSimIndicatorStateGemini(slot);
            } else {
                state = iTel.getSimIndicatorState();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        int resourceId = 0;
        switch (state) {
            case com.android.internal.telephony.PhoneConstants.SIM_INDICATOR_LOCKED:
                resourceId = com.mediatek.internal.R.drawable.sim_locked;
                break;
            case com.android.internal.telephony.PhoneConstants.SIM_INDICATOR_RADIOOFF:
                resourceId = com.mediatek.internal.R.drawable.sim_radio_off;
                break;
            case com.android.internal.telephony.PhoneConstants.SIM_INDICATOR_ROAMING:
                resourceId = com.mediatek.internal.R.drawable.sim_roaming;
                break;
            case com.android.internal.telephony.PhoneConstants.SIM_INDICATOR_SEARCHING:
                resourceId = com.mediatek.internal.R.drawable.sim_searching;
                break;
            case com.android.internal.telephony.PhoneConstants.SIM_INDICATOR_INVALID:
                resourceId = com.mediatek.internal.R.drawable.sim_invalid;
                break;
            case com.android.internal.telephony.PhoneConstants.SIM_INDICATOR_CONNECTED:
                resourceId = com.mediatek.internal.R.drawable.sim_connected;
                break;
            case com.android.internal.telephony.PhoneConstants.SIM_INDICATOR_ROAMINGCONNECTED:
                resourceId = com.mediatek.internal.R.drawable.sim_roaming_connected;
                break;
            default:
                break;
        }
        return resourceId;
    }
}
