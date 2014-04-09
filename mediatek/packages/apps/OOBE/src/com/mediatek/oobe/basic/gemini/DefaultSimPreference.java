/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 * 
 * MediaTek Inc. (C) 2010. All rights reserved.
 * 
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package com.mediatek.oobe.basic.gemini;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.preference.DialogPreference;
import android.provider.Settings;
import android.provider.Telephony.SIMInfo;
import android.text.TextUtils;
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

import com.android.internal.telephony.PhoneConstants;

import com.mediatek.CellConnService.CellConnMgr;
import com.mediatek.oobe.R;
import com.mediatek.xlog.Xlog;

import java.util.List;

public class DefaultSimPreference extends DialogPreference implements
        AdapterView.OnItemClickListener {

    private static final String TAG = "DefaultSimPreference";
    static final int IMAGE_GRAY = 75;//30% of 0xff in transparent
    static final int ORIGINAL_IMAGE = 255;
    private LayoutInflater mFlater;

    private List<SimItem> mSimItemList;
    private SelectionListAdapter mAdapter;
    private ListView mListView;
    private int mSelected = -1;
    private int mInitValue = -1;
    private Context mContext;

    private CellConnMgr mCellConnMgr;

    private int mType = -1;

    private static final int DISPLAY_NONE = 0;
    private static final int DISPLAY_FIRST_FOUR = 1;
    private static final int DISPLAY_LAST_FOUR = 2;

    private static final int PIN1_REQUEST_CODE = 302;
    private static final int NUM_WIDTH = 4;
    private static final int COLOR_INTERNET_CALL = 8;

    /**
     * Construct of DefaultSimPreference
     * 
     * @param context
     *            Context
     * @param attrs
     *            AttributeSet
     */
    public DefaultSimPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Construct of DefaultSimPreference
     * 
     * @param context
     *            Context
     * @param attrs
     *            AttributeSet
     * @param defStyle
     *            int
     */
    public DefaultSimPreference(Context context, AttributeSet attrs,
            int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        mFlater = LayoutInflater.from(context);

    }

    /**
     * 
     * @param cellConnmgr
     *            cell connmgr to see the state of sim card
     */
    void setCellConnMgr(CellConnMgr cellConnmgr) {
        mCellConnMgr = cellConnmgr;

    }

    /**
     * Set the data for the list
     * 
     * @param SimItemList
     *            a list inculdes the sim information
     */
    void setData(List<SimItem> simItemList) {
        mSimItemList = simItemList;
        if (getDialog() != null) {
            if (mListView != null) {
                mAdapter = new SelectionListAdapter(mSimItemList);
                mListView.setAdapter(mAdapter);
                mAdapter.notifyDataSetChanged();
            }
        }
    }

    /**
     * Set the type of sim
     * 
     * @param type
     *            GPRS, voice or sms
     */
    void setType(int type) {
        mType = type;
    }

    @Override
    public void onBindView(View view) {

        super.onBindView(view);

        TextView textSummary = (TextView) view
                .findViewById(android.R.id.summary);

        if (textSummary != null) {
            textSummary.setSingleLine();
            textSummary.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        }

        Xlog.i(TAG, "summary is +" + this.getSummary());
    }

    @Override
    protected void onPrepareDialogBuilder(Builder builder) {
        super.onPrepareDialogBuilder(builder);

        mAdapter = new SelectionListAdapter(mSimItemList);
        mListView = new ListView(mContext);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);
        mListView.setItemsCanFocus(false);
        builder.setView(mListView, 0, 0, 0, 0);
        builder.setNegativeButton(android.R.string.cancel, null);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        Xlog.i(TAG, "mSelected = " + mSelected);
        Xlog.i(TAG, "mInitValue = " + mInitValue);
        if (positiveResult && mSelected >= 0 && (mSelected != mInitValue)) {

            Xlog.i(TAG, "callChangeListener");
            long value = mSimItemList.get(mSelected).mSimID;
            callChangeListener(value);
            mInitValue = mSelected;
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        Drawable icon = getIcon();
        if (icon != null) {
            icon.setAlpha(enabled ? ORIGINAL_IMAGE : 
                                    IMAGE_GRAY);
        } else {
            Xlog.d(TAG,"fail to set icon alpha due to icon is null"); 
        }
        super.setEnabled(enabled);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        Xlog.i(TAG, "positon is " + position);
        if (v != null) {
            if (!v.isEnabled()) {
                return;
            } else {
                SimItem simItem = mSimItemList.get(position);
                if ((mType == GeminiUtils.TYPE_GPRS) && (simItem.mIsSim)
                        && (mCellConnMgr != null)
                        && (simItem.mState == PhoneConstants.SIM_INDICATOR_LOCKED)) {
                    mCellConnMgr.handleCellConn(simItem.mSlot,
                            PIN1_REQUEST_CODE);
                } else {
                    mSelected = position;
                    onClick(getDialog(), DialogInterface.BUTTON_POSITIVE);
                    getDialog().dismiss();
                }

            }
        }
    }

    class SelectionListAdapter extends BaseAdapter {

        List<SimItem> mSimItemList;

        public SelectionListAdapter(List<SimItem> simItemList) {
            mSimItemList = simItemList;
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

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = mFlater.inflate(
                        R.layout.preference_sim_default_select, null);
                holder = new ViewHolder();
                setViewHolderId(holder, convertView);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            SimItem simItem = (SimItem) getItem(position);
            setNameAndNum(holder.mTextName, holder.mTextNum, simItem);
            setImageSim(holder.mImageSim, simItem);
            setImageStatus(holder.mImageStatus, simItem);
            setTextNumFormat(holder.mTextNumFormat, simItem);
            holder.mCkRadioOn.setChecked(mSelected == position);
            if ((simItem.mState == PhoneConstants.SIM_INDICATOR_RADIOOFF)
                    || ((mType == GeminiUtils.TYPE_SMS) && (getCount() == 2) && 
                        (simItem.mSimID == Settings.System.DEFAULT_SIM_SETTING_ALWAYS_ASK))
                    || ((mType == GeminiUtils.TYPE_VOICECALL) && (getCount() == 2 || getCount() == 1) && 
                        (simItem.mSimID == Settings.System.DEFAULT_SIM_SETTING_ALWAYS_ASK))) {
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
                        if (simItem.mNumber.length() >= NUM_WIDTH) {
                            textNumFormat.setText(simItem.mNumber.substring(0,
                                    NUM_WIDTH));
                        } else {
                            textNumFormat.setText(simItem.mNumber);
                        }
                        break;
                    case DISPLAY_LAST_FOUR:
                        textNumFormat.setVisibility(View.VISIBLE);
                        if (simItem.mNumber.length() >= NUM_WIDTH) {
                            textNumFormat.setText(simItem.mNumber
                                    .substring(simItem.mNumber.length()
                                            - NUM_WIDTH));
                        } else {
                            textNumFormat.setText(simItem.mNumber);
                        }
                        break;
                    default:
                        break;
                    }
                } else {
                    Xlog.d(TAG,"simItem.mNumber="+simItem.mNumber);
                    textNumFormat.setVisibility(View.GONE);
                }
            } else {
                textNumFormat.setVisibility(View.GONE);
            }

        }

        private void setImageStatus(ImageView imageStatus, SimItem simItem) {
            if (simItem.mIsSim) {
                int res = GeminiUtils.getStatusResource(simItem.mState);
                if (res == -1) {
                    imageStatus.setVisibility(View.GONE);
                } else {
                    imageStatus.setVisibility(View.VISIBLE);
                    imageStatus.setImageResource(res);
                }
            } else {
                imageStatus.setVisibility(View.GONE);   
            }

        }

        private void setImageSim(RelativeLayout imageSim, SimItem simItem) {
            if (simItem.mIsSim) {
                int resColor = GeminiUtils.getSimColorResource(simItem.mColor);
                if (resColor >= 0) {
                    imageSim.setVisibility(View.VISIBLE);
                    imageSim.setBackgroundResource(resColor);
                }
            } else if (simItem.mColor == COLOR_INTERNET_CALL) {
                imageSim.setVisibility(View.VISIBLE);
                imageSim
                        .setBackgroundResource(com.mediatek.internal.R.drawable.sim_background_sip);
            } else {
                imageSim.setVisibility(View.GONE);
            }
        }

        private void setViewHolderId(ViewHolder holder, View convertView) {
            holder.mTextName = (TextView) convertView
                    .findViewById(R.id.simNameSel);
            holder.mTextNum = (TextView) convertView
                    .findViewById(R.id.simNumSel);
            holder.mImageStatus = (ImageView) convertView
                    .findViewById(R.id.simStatusSel);
            holder.mTextNumFormat = (TextView) convertView
                    .findViewById(R.id.simNumFormatSel);
            holder.mCkRadioOn = (RadioButton) convertView
                    .findViewById(R.id.Enable_select);
            holder.mImageSim = (RelativeLayout) convertView
                    .findViewById(R.id.simIconSel);
        }

        private void setNameAndNum(TextView textName, TextView textNum,
                SimItem simItem) {
            if (simItem.mName == null) {
                textName.setVisibility(View.GONE);
            } else {
                textName.setVisibility(View.VISIBLE);
                textName.setText(simItem.mName);
            }
            if ((simItem.mIsSim)
                    && ((simItem.mNumber != null) && (simItem.mNumber.length() != 0))) {
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

    void setInitValue(int value) {
        mInitValue = value;
        mSelected = value;

        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
    }

    long getValue() {
        return mSimItemList.get(mSelected).mSimID;
    }

    void setInitData(List<SimItem> simItemList) {
        mSimItemList = simItemList;
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
    }

    private void updateData() {

        int location = 0;
        for (SimItem simitem : mSimItemList) {

            if (simitem.mIsSim) {
                SIMInfo siminfo = SIMInfo.getSIMInfoById(mContext,
                        simitem.mSimID);

                if (siminfo != null) {
                    SimItem simitemCopy = new SimItem(siminfo);
                    mSimItemList.set(location, simitemCopy);
                }

            }

            location++;
        }
    }

}
