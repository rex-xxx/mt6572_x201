package com.mediatek.contacts.widget;

import android.accounts.Account;
import android.content.Context;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.provider.Telephony;
import android.provider.Telephony.SIMInfo;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import com.android.contacts.R;
import com.android.contacts.util.Constants;

import com.android.internal.telephony.ITelephony;
import com.mediatek.contacts.simcontact.SimCardUtils;

import java.util.List;

public class SimPickerAdapter extends BaseAdapter {

    public static final int ITEM_TYPE_UNKNOWN  = -1;
    public static final int ITEM_TYPE_SIM      =  0;
    public static final int ITEM_TYPE_INTERNET =  1;
    public static final int ITEM_TYPE_TEXT     =  2;
    public static final int ITEM_TYPE_ACCOUNT  =  3;
    private static final String TAG = "SimPickerAdapter";
    
    Context mContext;
    long mSuggestedSimId;
    List<ItemHolder> mItems;

    boolean mSingleChoice;
    int mSingleChoiceIndex;
    // fixed bug for ALPS00387288
    boolean mIsDarkBackGround = true;
    
    public SimPickerAdapter(Context context, List<ItemHolder> items, long suggestedSimId) {
        mContext = context;
        mSuggestedSimId = suggestedSimId;
        mItems = items;
        mSingleChoice = false;
        mSingleChoiceIndex = -1;
    }
    
    /*
     * fix bug for ALPS00387288:use light background start
     */
    public SimPickerAdapter(Context context, List<ItemHolder> items, long suggestedSimId, boolean isDark) {
        mContext = context;
        mSuggestedSimId = suggestedSimId;
        mItems = items;
        mSingleChoice = false;
        mSingleChoiceIndex = -1;
        mIsDarkBackGround = isDark;
    }
    /*
     * fix bug for ALPS00387288
     * ends
     */
    
    public int getCount() {
        // TODO Auto-generated method stub
        return mItems.size();
    }

    @Override
    public int getViewTypeCount() {
        return 4;
    }
    
    @Override
    public int getItemViewType(int position) {
        // TODO Auto-generated method stub
        ItemHolder itemHolder = mItems.get(position);
        return itemHolder.type;
    }

    public void setSingleChoice(boolean singleChoice) {
        mSingleChoice = singleChoice;
    }

    public boolean getSingleChoice() {
        return mSingleChoice;
    }

    public void setSingleChoiceIndex(int singleChoiceIndex) {
        mSingleChoiceIndex = singleChoiceIndex;
    }

    public Object getItem(int position) {
        ItemHolder itemHolder = mItems.get(position);
        if (itemHolder.type == ITEM_TYPE_SIM) {
            return Integer.valueOf(((SIMInfo)itemHolder.data).mSlot);
        } else if (itemHolder.type == ITEM_TYPE_INTERNET) {
            return Integer.valueOf((int)Settings.System.VOICE_CALL_SIM_SETTING_INTERNET);
        } else if (itemHolder.type == ITEM_TYPE_TEXT || itemHolder.type == ITEM_TYPE_ACCOUNT) {
            return itemHolder.data;
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
            LayoutInflater inflater = LayoutInflater.from(mContext);
            holder = new ViewHolder();
            
            if (viewType == ITEM_TYPE_SIM) {
                view = inflater.inflate(R.layout.sim_picker_item, null);
                holder.mSimSignal = (TextView)view.findViewById(R.id.simSignal);
                holder.mSimStatus = (ImageView)view.findViewById(R.id.simStatus);
                holder.mShortPhoneNumber = (TextView)view.findViewById(R.id.shortPhoneNumber);
                holder.mDisplayName = (TextView)view.findViewById(R.id.displayName);
                holder.mPhoneNumber = (TextView)view.findViewById(R.id.phoneNumber);
                holder.mSimIcon = view.findViewById(R.id.simIcon);
                holder.mSuggested = (ImageView)view.findViewById(R.id.suggested);
                holder.mRadioButton = (RadioButton)view.findViewById(R.id.select);
            } else if (viewType == ITEM_TYPE_INTERNET) {
                view = inflater.inflate(R.layout.sim_picker_item_internet, null);
                holder.mInternetIcon = (ImageView)view.findViewById(R.id.internetIcon);
                holder.mRadioButton = (RadioButton)view.findViewById(R.id.select);
            } else if (viewType == ITEM_TYPE_TEXT || viewType == ITEM_TYPE_ACCOUNT) {
                view = inflater.inflate(R.layout.sim_picker_item_text, null);
                holder.mText = (TextView)view.findViewById(R.id.text);
                holder.mRadioButton = (RadioButton)view.findViewById(R.id.select);
            }
            view.setTag(holder);
        }
        
        holder = (ViewHolder)view.getTag();

        if (mSingleChoice && holder.mRadioButton != null) {
            holder.mRadioButton.setVisibility(View.VISIBLE);
        } else {
            holder.mRadioButton.setVisibility(View.GONE);
        }

        if (viewType == ITEM_TYPE_SIM) {
            SIMInfo simInfo = (SIMInfo)mItems.get(position).data;
            holder.mDisplayName.setText(simInfo.mDisplayName);
            if (mIsDarkBackGround) {
                holder.mSimIcon.setBackgroundResource(Telephony.SIMBackgroundDarkRes[simInfo.mColor]);
            } else {
                holder.mSimIcon.setBackgroundResource(Telephony.SIMBackgroundLightRes[simInfo.mColor]);
            }
            

            if (simInfo.mSimId == mSuggestedSimId) {
                holder.mSuggested.setVisibility(View.VISIBLE);
            } else {
                holder.mSuggested.setVisibility(View.GONE);
            }

            try {
                String shortNumber = "";
                if (!TextUtils.isEmpty(simInfo.mNumber)) {
                    switch(simInfo.mDispalyNumberFormat) {
                        case android.provider.Telephony.SimInfo.DISPLAY_NUMBER_FIRST:
                            if (simInfo.mNumber.length() <= 4) {
                                shortNumber = simInfo.mNumber;
                            } else {
                                shortNumber = simInfo.mNumber.substring(0, 4);
                            }
                            break;
                        case android.provider.Telephony.SimInfo.DISPLAY_NUMBER_LAST:
                            if (simInfo.mNumber.length() <= 4) {
                                shortNumber = simInfo.mNumber;
                            } else {
                                shortNumber = simInfo.mNumber.substring(
                                        simInfo.mNumber.length() - 4, simInfo.mNumber.length());
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
                if (holder.mRadioButton != null) {
                    if (mSingleChoiceIndex == simInfo.mSimId) {
                        holder.mRadioButton.setChecked(true);
                    } else {
                        holder.mRadioButton.setChecked(false);
                    }
                }
            } catch (Exception e) {
                holder.mShortPhoneNumber.setText("");
            }
            holder.mSimStatus.setImageResource(getSimStatusIcon(simInfo.mSlot));
        } else if (viewType == ITEM_TYPE_INTERNET) {
            holder.mInternetIcon.setBackgroundResource(com.mediatek.internal.R.drawable.sim_background_sip);

            if (holder.mRadioButton != null) {
                if (mSingleChoiceIndex == Constants.FILTER_SIP_CALL) {
                    holder.mRadioButton.setChecked(true);
                } else {
                    holder.mRadioButton.setChecked(false);
                }
            }
        } else if (viewType == ITEM_TYPE_TEXT) {
            String text = (String)mItems.get(position).data;
            holder.mText.setText(text);

            if (holder.mRadioButton != null) {
                if (mSingleChoiceIndex == Constants.FILTER_ALL_RESOURCES) {
                    holder.mRadioButton.setChecked(true);
                } else {
                    holder.mRadioButton.setChecked(false);
                }
            }
        } else if (viewType == ITEM_TYPE_ACCOUNT) {
            Account account = (Account)mItems.get(position).data;
            holder.mText.setText((String)account.name);
        }

        return view;
    }
    
    protected int getSimStatusIcon(int slot) {
        int state = SimCardUtils.getSimIndicatorState(slot);
        Log.d(TAG, "[getSimStatusIcon] sim state is " + state);

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

    private class ViewHolder {
        View      mSimIcon;
        ImageView mSimStatus;
        TextView mSimSignal;
        TextView  mShortPhoneNumber;
        TextView  mDisplayName;
        TextView  mPhoneNumber;
        ImageView  mSuggested;
        TextView  mText;
        ImageView mInternetIcon;
        RadioButton mRadioButton;
    }

    public static class ItemHolder {
        public Object data;
        public int type;
        
        public ItemHolder(Object itemData, int itemType) {
            this.data = itemData;
            this.type = itemType; 
        }
    }
}
