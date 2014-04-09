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

import android.content.Context;
import android.preference.Preference;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mediatek.oobe.R;
import com.mediatek.oobe.basic.ImportContactsActivity;
import com.mediatek.oobe.utils.OOBEConstants;
import com.mediatek.xlog.Xlog;

import java.util.ArrayList;
import java.util.List;

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
    private static final int DISPLAY_NONE = 0;
    private static final int DISPLAY_FIRST_FOUR = 1;
    private static final int DISPLAY_LAST_FOUR = 2;
    private static final String TAG = "SimInfoPreference";

    private Context mContext;
    private int mProgressCurrNum = 0;
    private int mProgressTotalCount = 0;
    private String mTextImportingProgress = "";
    // flag for contact import progress
    private boolean mIsImporting = false;
    private boolean mFinishImporting = false;

    /**
     * SimInfoPreference
     * @param context Context
     * @param name String
     * @param number String
     * @param simSlot int
     * @param status int
     * @param color int
     * @param displayNumberFormat int
     * @param key long
     * @param needCheckBox boolean
     */
    public SimInfoPreference(Context context, String name, String number, int simSlot, int status, int color,
            int displayNumberFormat, long key, boolean needCheckBox) {
        this(context, name, number, simSlot, status, color, displayNumberFormat, key, needCheckBox, true);

    }

    /**
     * SimInfoPreference
     * @param context Context
     * @param name String
     * @param number String
     * @param simSlot int
     * @param status int
     * @param color int
     * @param displayNumberFormat int
     * @param key long
     * @param needCheckBox true
     * @param needStatus boolean
     */
    public SimInfoPreference(Context context, String name, String number, int simSlot, int status, int color,
            int displayNumberFormat, long key, boolean needCheckBox, boolean needStatus) {

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
        if (needCheckBox) {
            setLayoutResource(R.layout.preference_contact_sim_info);
        } else {
            setLayoutResource(R.layout.preference_sim_info);
        }

        if (mName != null) {
            setTitle(mName);
        }
        if ((mSimNum != null) && (mSimNum.length() != 0)) {
            setSummary(mSimNum);

        }

    }

    @Override
    public View getView(View convertView, ViewGroup parent) { //MTK_CS_IGNORE_THIS_LINE
        // TODO Auto-generated method stub
        View view = super.getView(convertView, parent);

        TextView textTitle = (TextView) view.findViewById(android.R.id.title);

        if ((textTitle != null) && (mName != null)) {
            textTitle.setText(mName);
        }

        TextView textNum = (TextView) view.findViewById(android.R.id.summary);
        if (textNum != null) {
            if ((mSimNum != null) && (mSimNum.length() != 0)) {
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

        RelativeLayout viewSim = (RelativeLayout) view.findViewById(R.id.simIcon);

        if (viewSim != null) {

            int res = GeminiUtils.getSimColorResource(mColor);

            if (res < 0) {
                viewSim.setBackgroundDrawable(null);
            } else {
                viewSim.setBackgroundResource(res);
            }

        }

        // checkbox
        CheckBox ckRadioOn = (CheckBox) view.findViewById(R.id.Check_Enable);
        Xlog.i(TAG, "ckRadioOn.setChecked " + mChecked);
        if (ckRadioOn != null) {
            if (mNeedCheckbox) {
                ckRadioOn.setChecked(mChecked);
                ckRadioOn.setVisibility((mFinishImporting || mIsImporting) ? View.GONE : View.VISIBLE);
            } else {
                ckRadioOn.setVisibility(View.GONE);
            }
        }

        final int formatNum = 4;
        TextView textNumForShort = (TextView) view.findViewById(R.id.simNum);
        if ((textNum != null) && (mSimNum != null)) {

            switch (mNumDisplayFormat) {
            case DISPLAY_NONE: 
                textNumForShort.setVisibility(View.GONE);
                break;
            case DISPLAY_FIRST_FOUR: 
                if (mSimNum.length() >= formatNum) {
                    textNumForShort.setText(mSimNum.substring(0, formatNum));
                } else {
                    textNumForShort.setText(mSimNum);
                }
                break;
            case DISPLAY_LAST_FOUR: 
                if (mSimNum.length() >= formatNum) {
                    textNumForShort.setText(mSimNum.substring(mSimNum.length() - formatNum));
                } else {
                    textNumForShort.setText(mSimNum);
                }
                break;
            default:
                break;

            }
        }
        Xlog.d(TAG, "mNeedCheckbox: " + mNeedCheckbox);
        if (mNeedCheckbox) {
            ProgressBar importingProgressBar = (ProgressBar) view.findViewById(R.id.progress_importing);
            TextView importingProgressTextView = (TextView) view.findViewById(R.id.textView_importing);
            ImageView importingProgressFlag = (ImageView) view.findViewById(R.id.importing_completed);
            TextView summaryView = (TextView) view.findViewById(android.R.id.summary);
            String summaryStr = (summaryView == null ? "" : summaryView.getText().toString());

            importingProgressFlag.setVisibility(mFinishImporting ? View.VISIBLE : View.GONE);
            if (summaryView != null) {
                summaryView.setVisibility(((mIsImporting && !mFinishImporting) || TextUtils.isEmpty(summaryStr)) ? View.GONE
                        : View.VISIBLE);
            }

            if (mIsImporting && !mFinishImporting) {
                importingProgressBar.setVisibility(View.VISIBLE);
                importingProgressBar.setMax(mProgressTotalCount);
                importingProgressBar.setProgress(mProgressCurrNum);

                importingProgressTextView.setVisibility(View.VISIBLE);
                importingProgressTextView.setText(mTextImportingProgress);
            } else {
                importingProgressBar.setVisibility(View.GONE);
                importingProgressTextView.setVisibility(View.GONE);
            }
        }
        return view;
    }

    public void setCheck(boolean bCheck) {
        mChecked = bCheck;
        notifyChanged();
    }

    public boolean getCheck() {
        return mChecked;

    }

    public void setStatus(int status) {
        mStatus = status;
        notifyChanged();
    }

    public void setName(String name) {
        mName = name;
        notifyChanged();

    }

    public void setColor(int color) {
        mColor = color;
        notifyChanged();
    }

    public void setNumDisplayFormat(int format) {
        mNumDisplayFormat = format;
        notifyChanged();
    }

    public void setNumber(String number) {
        mSimNum = number;
        notifyChanged();
    }


    public void setNeedCheckBox(boolean isNeed) {
        mNeedCheckbox = isNeed;
        notifyChanged();
    }

    /**
     * get slot index
     * 
     * @return the slot index
     */
    public int getSlotIndex() {
        return mSlotIndex;
    }
    /**
     * get the status of importing status
     * 
     * @return boolean true or false
     */
    public boolean isImporting() {
        return mIsImporting;
    }

    /**
     * set importing status
     * 
     * @param isImporting
     *            true/false
     */
    public void setImporting(boolean isImporting) {
        this.mIsImporting = isImporting;
        notifyChanged();
    }

    /**
     * is finish importing status
     * 
     * @return true or false
     */
    public boolean isFinishImporting() {
        return mFinishImporting;
    }

    /**
     * set finish importing
     * 
     * @param finish
     *            the status of importing
     */
    public void setFinishImporting(boolean finish) {
        this.mFinishImporting = finish;
        notifyChanged();
    }

    /**
     * init progress bar
     * 
     * @param totalCount
     *            int
     */
    public void initProgressBar(int totalCount) {
        Xlog.d(TAG, mName + "--initProgressBar, totalCount=" + totalCount);
        mIsImporting = true;
        if (mContext == null)
        Xlog.d(TAG, "mContext is null");
        mTextImportingProgress = mContext.getResources().getString(R.string.oobe_note_copy_contacts_waiting);
        mProgressCurrNum = 0;
        mProgressTotalCount = totalCount;
        notifyChanged();
    }

    /**
     * increment progress
     * 
     * @param newProgress
     *            int
     */
    public void incrementProgressTo(int newProgress) {
        Xlog.d(TAG, mName + "--incrementProgressTo " + newProgress);
        mProgressCurrNum = newProgress;
        mTextImportingProgress = String.format(mContext.getResources().getString(R.string.oobe_note_progress_copy_contacts),
                mProgressCurrNum, mProgressTotalCount);
        notifyChanged();
        notifyCallback();
    }

    /**
     * finish progress bar
     */
    public void finishProgressBar() {
        if (mFinishImporting) { //already finished, just return
            return;
        }
        mFinishImporting = true;
        notifyChanged();
        notifyCallback();
    }

    /**
     * deal with cancel button click
     */
    public void dealWithCancel() {
        mFinishImporting = false;
        mIsImporting = false;
        notifyChanged();
        notifyCancelImport();
    }

    private static List<ImportContactsActivity> sListenerList = new ArrayList<ImportContactsActivity>();

    private void notifyCallback() {
        for (int i = 0; i < sListenerList.size(); i++) {
            ImportContactsActivity activity = sListenerList.get(i);
            if (activity == mContext) {
                Xlog.w(TAG, "Contact Import callback============self, do not notify==========");
                continue;
            }
            activity.refreshProgress();
        }
    }

    private void notifyCancelImport() {
        for (int i = 0; i < sListenerList.size(); i++) {
            ImportContactsActivity activity = sListenerList.get(i);
            if (activity == mContext) {
                Xlog.w(TAG, "Contact Import cancel importing============self, do not notify==========");
                continue;
            }
            activity.refreshCancel();
        }
    }

    /**
     * register call back
     * 
     * @param listener
     *            ImportContactsActivity listener
     */
    public static void registerCallback(ImportContactsActivity listener) {
        sListenerList.add(listener);
    }

    /**
     * unregister call back
     * 
     * @param listener
     *            ImportContactsActivity listener
     */
    public static void unregisterCallback(ImportContactsActivity listener) {
        sListenerList.remove(listener);
    }

}
