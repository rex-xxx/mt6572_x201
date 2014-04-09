package com.mediatek.settings;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.ServiceManager;
import android.preference.DialogPreference;
import android.provider.Telephony.SIMInfo;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.PhoneConstants;
import com.android.phone.PhoneGlobals;
import com.android.phone.PhoneInterfaceManager;
import com.android.phone.R;
import com.mediatek.phone.Utils;

import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.telephony.TelephonyManagerEx;
import com.mediatek.xlog.Xlog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ServiceSelectList extends DialogPreference 
            implements AdapterView.OnItemClickListener, DialogInterface.OnClickListener {
    
    private static final String TAG = "Settings/ServiceSelectList";
    private LayoutInflater mFlater;
    private String mValue;

    private SelectionListAdapter mAdapter;
    private ListView mListView;
    private List<SimItem> mSimItemList;

    private int mSelected = -1;
    private int mSwitchTo = -1;
    private int mInitValue = -1;    
    private Drawable mIcon;
    private Context mContext;
    private PhoneInterfaceManager mPhoneMgr = null;
    private TelephonyManagerEx mTelephonyManagerEx;
    
    private static final int DISPLAY_NONE = 0;
    private static final int DISPLAY_FIRST_FOUR = 1;
    private static final int DISPLAY_LAST_FOUR = 2;    
    
    private static final int PIN1_REQUEST_CODE = 302;
    
    private CharSequence[] mEntries;
    private CharSequence[] mEntryValues;
    private AlertDialog mAlertDialog = null;
    private int mSwitchID;

    public ServiceSelectList(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }   
    public ServiceSelectList(Context context,AttributeSet attrs, int defStyle) {
       super(context, attrs);
       
        mContext = context;
        mFlater = LayoutInflater.from(context);
        
        TypedArray a = context.obtainStyledAttributes(attrs,
                com.android.internal.R.styleable.ListPreference, 0, 0);
        mEntries = a.getTextArray(com.android.internal.R.styleable.ListPreference_entries);
        mEntryValues = a.getTextArray(com.android.internal.R.styleable.ListPreference_entryValues);
        a.recycle(); 
        
        mPhoneMgr = PhoneGlobals.getInstance().phoneMgr;
        mTelephonyManagerEx = TelephonyManagerEx.getDefault();
    }
    
    @Override
    public void onBindView(View view) {
        super.onBindView(view);
    }

    
    @Override
     protected void onPrepareDialogBuilder(Builder builder) {
        super.onPrepareDialogBuilder(builder);
        refreshList();
        mAdapter = new SelectionListAdapter(this.getContext());
        mListView = new ListView(mContext);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);
        this.mSelected = mAdapter.getHas3GService();
        Xlog.d(TAG,"onPrepareDialogBuilder mSelected = " + mSelected);

        mListView.setItemsCanFocus(false);
        mListView.setCacheColorHint(0);

        builder.setInverseBackgroundForced(true);
        builder.setView(mListView,0,0,0,0);

        builder.setNegativeButton(R.string.cancel, this);
        builder.setPositiveButton(null, null);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        
        Xlog.i(TAG, "onDialogClosed : mSelected = " + mSelected);
        Xlog.i(TAG, "onDialogClosed : mInitValue = " + mInitValue);
        Xlog.i(TAG, "onDialogClosed : mSwitchID = " + mSwitchID);
        if (positiveResult) {
            Xlog.i(TAG, "callChangeListener");
            if (mSwitchID != mSelected && mSimItemList.get(mSwitchID) != null) {
                callChangeListener(mSimItemList.get(mSwitchID).mSimID);
                mSelected = mSwitchID;
                mInitValue = mSelected;
                Xlog.d(TAG,"mSelected is changed after popup dialog so use mSwitchID");
            } else {
                callChangeListener(mSimItemList.get(mSelected).mSimID);
                mInitValue = mSelected;
                Xlog.d(TAG,"Nothing modify after popup confirm dialog");
            }
        }

        this.dismissSelf();
    }
    
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            onDialogClosed(true);
        } else if (which == DialogInterface.BUTTON_NEGATIVE) {
            onDialogClosed(false);
        }
    }

    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        Xlog.i(TAG, "onclick");
        Xlog.i(TAG,"positon is " + position);
        Xlog.i(TAG,"current select is " + mSelected);
        
        if (!v.isEnabled()) {
            return ;
        } else if (position == mSelected) {
            dismissSelf();
            return ;
        } else {
            SimItem simItem = mSimItemList.get(position);
            if (simItem.mSimID == SimItem.DESCRIPTION_LIST_ITEM_SIMID) {
                return ;
            } else {
                mSelected = position;
                mSwitchID = position;
                Xlog.d(TAG,"onPrepareDialogBuilder mSelected = " + mSelected);
                Xlog.i(TAG,"Switch to " + mSelected);
                if (FeatureOption.MTK_VT3G324M_SUPPORT) {
                    int msgId = simItem.mSimID == SimItem.OFF_LIST_ITEM_SIMID 
                            ? R.string.confirm_3g_switch_to_off : R.string.confirm_3g_switch;
                    AlertDialog newDialog = new AlertDialog.Builder(mContext)
                        .setTitle(android.R.string.dialog_alert_title)
                        .setPositiveButton(R.string.buttonTxtContinue, this)
                        .setNegativeButton(R.string.cancel, this)
                        .setCancelable(true)
                        .setMessage(msgId)
                        .create();
                
                        newDialog.show();
                        mAlertDialog = newDialog;
                        onDialogClosed(false);
                } else {
                    onDialogClosed(true);
                }
            }
        }
    }

    void dismissDialogs() {
        Xlog.d(TAG, "disable the 3G switch.");     

        android.app.Dialog dialog = this.getDialog();
        if (dialog != null) {
            dialog.dismiss();
        }
        if (mAlertDialog != null && mAlertDialog.isShowing()) {
            mAlertDialog.dismiss();
        }
    }

    public void refreshList() {
        List<SimItem> itemList = new ArrayList<SimItem>();
        List<SIMInfo> list = SIMInfo.getInsertedSIMList(mContext);
        Collections.sort(list, new CallSettings.SIMInfoComparable());
        SimItem simitem;
        int state = 0;
        boolean isAllRadioOff = true;
        for (SIMInfo info : list) {
            if (info != null) {
                simitem = new SimItem(info);
                try {    
                    ITelephony iTelephony = ITelephony.Stub.asInterface(
                            ServiceManager.getService(Context.TELEPHONY_SERVICE));
                    if (iTelephony != null) {      
                        if (FeatureOption.MTK_GEMINI_SUPPORT) {    
                            state = iTelephony.getSimIndicatorStateGemini(info.mSlot);
                        } else {
                            state = iTelephony.getSimIndicatorState();
                        }
                    }
                } catch (android.os.RemoteException e) {
                    Xlog.d(TAG, "[e = " + e + "]");
                }
                simitem.mState = state;
                itemList.add(simitem);
                if (isAllRadioOff) {
                    isAllRadioOff = state == PhoneConstants.SIM_INDICATOR_RADIOOFF;
                }
            }
        }
        //add off item
        String offText = mContext.getResources().getString(R.string.service_3g_off);
        simitem = new SimItem(offText, 0, SimItem.OFF_LIST_ITEM_SIMID);
        if (isAllRadioOff) {
            simitem.mState = PhoneConstants.SIM_INDICATOR_RADIOOFF;
        }
        itemList.add(simitem);
        mSimItemList = itemList;

        if (mAdapter != null) {
            mSelected = mAdapter.getHas3GService();
            Xlog.d(TAG,"refreshList mSelected = " + mSelected);
            mAdapter.notifyDataSetChanged();
        }
    }

    class SelectionListAdapter extends BaseAdapter {
        
        public SelectionListAdapter(List<SimItem> simItemList) {
            mSimItemList = simItemList;
        }
        
        public SelectionListAdapter(Context ctx) {

        }

        public int getCount() {
            return mSimItemList.size();
        }

        public Object getItem(int position) {
            return mSimItemList.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public int getHas3GService() {
            int index = -1;
            for (int i = 0; i < mSimItemList.size(); ++i) {
                SimItem item = mSimItemList.get(i);
                if (item.mHas3GCapability) {
                    index = i;
                    break;
                }
            }
            return index;
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = mFlater.inflate(R.layout.preference_sim_list, null);
                holder = new ViewHolder();
                setViewHolderId(holder,convertView);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder)convertView.getTag();
            }
            SimItem simItem = (SimItem)getItem(position);
            setNameAndNum(holder.mTextName,holder.mTextNum,simItem);
            setImageSim(holder.mImageSim,simItem);
            setImageStatus(holder.mImageStatus,simItem);
            setTextNumFormat(holder.mTextNumFormat,simItem);
            holder.mCkRadioOn.setChecked(mSelected == position);
            Xlog.d(TAG,"getview, simItem.mState=" + simItem.mState);
            if ((simItem.mState == PhoneConstants.SIM_INDICATOR_RADIOOFF)) {
                convertView.setEnabled(false);
                holder.mTextName.setEnabled(false);
                holder.mTextNum.setEnabled(false);
                holder.mCkRadioOn.setEnabled(false);
            } else {
                convertView.setEnabled(true);
                holder.mTextName.setEnabled(true);
                holder.mTextNum.setEnabled(true);
                holder.mCkRadioOn.setEnabled(true);
            }
            return convertView;
          }
        private void setTextNumFormat(TextView textNumFormat, SimItem simItem) {
            if (simItem.mIsSim) {
                if (simItem.mNumber != null) {
                    switch (simItem.mDispalyNumberFormat) {
                    case DISPLAY_NONE: 
                        textNumFormat.setVisibility(View.GONE);
                        break;
                    case DISPLAY_FIRST_FOUR:
                        textNumFormat.setVisibility(View.VISIBLE);
                        if (simItem.mNumber.length() >= 4) {
                            textNumFormat.setText(simItem.mNumber.substring(0, 4));
                        } else {
                            textNumFormat.setText(simItem.mNumber);
                        }
                        break;
                    case DISPLAY_LAST_FOUR:
                        textNumFormat.setVisibility(View.VISIBLE);
                        if (simItem.mNumber.length() >= 4) {
                            textNumFormat.setText(
                                    simItem.mNumber.substring(simItem.mNumber.length() - 4));
                        } else {
                            textNumFormat.setText(simItem.mNumber);
                        }
                        break;
                    default:
                        break;
                    }           
                }
            }
            
        }
        private void setImageStatus(ImageView imageStatus, SimItem simItem) {
            try {          
                ITelephony iTelephony = ITelephony.Stub.asInterface(ServiceManager.getService(Context.TELEPHONY_SERVICE));
                if (imageStatus != null && iTelephony != null) {
                    int res = -1;
                    if (simItem.mSlot >= 0) {
                        res = getSimStatusImge(iTelephony.getSimIndicatorStateGemini(simItem.mSlot));
                    }
                    if (res == -1) {
                        imageStatus.setVisibility(View.GONE);
                    } else {
                        imageStatus.setVisibility(View.VISIBLE);
                        imageStatus.setImageResource(res);              
                    }
                }
            } catch (android.os.RemoteException e) {
                imageStatus.setVisibility(View.GONE);
            }
        }
        private void setImageSim(RelativeLayout imageSim, SimItem simItem) {
            if (simItem.mIsSim) {
                int resColor = Utils.getSimColorResource(simItem.mColor);
                if (resColor >= 0) {
                    imageSim.setVisibility(View.VISIBLE);
                    imageSim.setBackgroundResource(resColor);
                }
            } else if (simItem.mColor == 8) {
                imageSim.setVisibility(View.VISIBLE);
                imageSim.setBackgroundResource(com.mediatek.internal.R.drawable.sim_background_sip);
            } else {
                imageSim.setVisibility(View.GONE);
            }
        }

        private void setViewHolderId(ViewHolder holder, View convertView) {
            holder.mTextName = (TextView)convertView.findViewById(R.id.simNameSel);
            holder.mTextNum = (TextView)convertView.findViewById(R.id.simNumSel);
            holder.mImageStatus = (ImageView)convertView.findViewById(R.id.simStatusSel);
            holder.mTextNumFormat = (TextView)convertView.findViewById(R.id.simNumFormatSel);
            holder.mCkRadioOn = (RadioButton)convertView.findViewById(R.id.Enable_select);
            holder.mImageSim = (RelativeLayout)convertView.findViewById(R.id.simIconSel);
        }

        private void setNameAndNum(TextView textName,TextView textNum, SimItem simItem) {
            if (simItem.mName != null) {
                textName.setVisibility(View.VISIBLE);
                textName.setText(simItem.mName);
            } else {
                textName.setVisibility(View.GONE);
            }
            
            if ((simItem.mIsSim) && ((simItem.mNumber != null) && (simItem.mNumber.length() != 0))) {
                textNum.setVisibility(View.VISIBLE);
                textNum.setText(simItem.mNumber);
            } else {
                textNum.setVisibility(View.GONE);
            }
        }
        class ViewHolder {
            TextView mTextName;
            TextView mTextNum;
            RelativeLayout mImageSim;
            ImageView mImageStatus;
            TextView mTextNumFormat;
            RadioButton mCkRadioOn;   
        }
    }
    
    void setRadioCheched(int index) {
        int listSize = mListView.getCount();
        
        for (int k = 0; k < listSize; k++) {
            
            View itemView = mListView.getChildAt(k);
            RadioButton btn = (RadioButton)itemView.findViewById(R.id.Enable_select);
            if (btn != null) {
                btn.setChecked((k == index) ? true : false);
            }
        }
    }
    
    void setInitValue(int value) {
        mInitValue = value;
        mSelected = value;
    }
    
    class SimItem {
        public static final long DESCRIPTION_LIST_ITEM_SIMID = -2;
        public static final long OFF_LIST_ITEM_SIMID = -1; 
        public boolean mHas3GCapability = false;
        
        public boolean mIsSim = true;
        public String mName = null;
        public String mNumber = null;
        public int mDispalyNumberFormat = 0;
        public int mColor = -1;
        public int mSlot = -1;
        public long mSimID = -1;
        public int mState = PhoneConstants.SIM_INDICATOR_NORMAL;
        
        //Constructor for not real sim
        public SimItem(String name, int color,long simID) {
            mName = name;
            mColor = color;
            mIsSim = false;
            mSimID = simID;
            if (mPhoneMgr != null) {
                mHas3GCapability = mSlot == mPhoneMgr.get3GCapabilitySIM();
            }
        }
        //constructor for sim
        public SimItem(SIMInfo siminfo) {
            mIsSim = true;
            mName = siminfo.mDisplayName;
            mNumber = siminfo.mNumber;
            mDispalyNumberFormat = siminfo.mDispalyNumberFormat;
            mColor = siminfo.mColor;
            mSlot = siminfo.mSlot;
            mSimID = siminfo.mSimId;
            if (mPhoneMgr != null) {
                mHas3GCapability = mSlot == mPhoneMgr.get3GCapabilitySIM();
            }
        }
    }
    
    void dismissSelf() {
        Xlog.d(TAG, "Dismiss the select list.");     
        AlertDialog dialog = (AlertDialog)this.getDialog();
        if (dialog != null) {
            dialog.dismiss();
        }
    }

    private int getSimStatusImge(int state) {
        switch (state) {
            case PhoneConstants.SIM_INDICATOR_RADIOOFF:
                return com.mediatek.internal.R.drawable.sim_radio_off;
            case PhoneConstants.SIM_INDICATOR_LOCKED:
                return com.mediatek.internal.R.drawable.sim_locked;
            case PhoneConstants.SIM_INDICATOR_INVALID:
                return com.mediatek.internal.R.drawable.sim_invalid;
            case PhoneConstants.SIM_INDICATOR_SEARCHING:
                return com.mediatek.internal.R.drawable.sim_searching;
            case PhoneConstants.SIM_INDICATOR_ROAMING:
                return com.mediatek.internal.R.drawable.sim_roaming;
            case PhoneConstants.SIM_INDICATOR_CONNECTED:
                return com.mediatek.internal.R.drawable.sim_connected;
            case PhoneConstants.SIM_INDICATOR_ROAMINGCONNECTED:
                return com.mediatek.internal.R.drawable.sim_roaming_connected;
            default:
                return -1;
        }
    }
}
