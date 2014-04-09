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
import android.preference.DialogPreference;
import android.provider.Telephony.SIMInfo;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.mediatek.oobe.R;
import com.mediatek.xlog.Xlog;

import java.util.ArrayList;
import java.util.List;

public class ColorPickerPreference extends DialogPreference implements OnClickListener {
    private static final String TAG = "ColorPicker";
    private final Context mContext;
    private static final int COLOR_SIZE = 8;
    private int mCurrentSelected = -1;
    private int mInitValue = -1;
    private long mSimID = -1;

    private final List<Integer> mCurrentUsed;

    private static final int COLORID[] = {R.id.color_00, R.id.color_01,
            R.id.color_02, R.id.color_03 };

    private static final int SIM_COLOR[] = { R.color.sim_blue,
            R.color.sim_orange, R.color.sim_green, R.color.sim_purple };

    /**
     * Construct of ColorPickerPreference
     * 
     * @param context
     *            the context
     * @param attrs
     *            the property
     * 
     */
    public ColorPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        setLayoutResource(R.layout.preference_color_picker);
        setDialogLayoutResource(R.layout.color_picker);
        setNegativeButtonText(android.R.string.cancel);
        mCurrentUsed = new ArrayList<Integer>();
    }

    /**
     * Set sim id
     * 
     * @param simID
     *            sim id
     */
    public void setSimID(long simID) {
        mSimID = simID;
    }

    @Override
    protected void onBindDialogView(View view) {
        // TODO Auto-generated method stub
        super.onBindDialogView(view);
        List<SIMInfo> simList = SIMInfo.getInsertedSIMList(mContext);
        for (SIMInfo siminfo : simList) {
            if (siminfo != null) {
                Xlog.i(TAG, "current used =" + Integer.valueOf(siminfo.mColor));
                ImageView iconView = (ImageView) view
                        .findViewById(COLORID[siminfo.mColor]);
                if (iconView != null) {
                    if (mSimID == siminfo.mSimId) {
                        mCurrentSelected = siminfo.mColor;
                        mInitValue = siminfo.mColor;
                        iconView.setBackgroundResource(R.drawable.color_selected);
                    } else {
                        mCurrentUsed.add(Integer.valueOf(siminfo.mColor));
                        if (siminfo.mColor != mCurrentSelected) {
                            iconView.setBackgroundResource(R.drawable.color_used);
                        }
                    }
                }
            }
        }
        for (int k = 0; k < COLORID.length; k++) {
            ImageView iconView = (ImageView) view.findViewById(COLORID[k]);
            iconView.setOnClickListener(this);
        }
    }

    @Override
    protected void onPrepareDialogBuilder(Builder builder) {
        super.onPrepareDialogBuilder(builder);

        builder.setInverseBackgroundForced(true);
        builder.setPositiveButton(null, null);

    }

    @Override
    public void onClick(View arg0) {
        // TODO Auto-generated method stub

        int viewId = arg0.getId();

        if (arg0 instanceof ImageView) {

            for (int k = 0; k < COLORID.length; k++) {

                if (COLORID[k] == viewId) {
                    mCurrentSelected = k;
                    Xlog.i(TAG, "mCurrentSelected is " + k);
                }

            }

            onClick(getDialog(), DialogInterface.BUTTON_POSITIVE);
            getDialog().dismiss();
        }

    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        Xlog.i(TAG, "positiveResult is " + positiveResult
                + " mCurrentSelected is " + mCurrentSelected
                + " mInitValue is " + mInitValue);
        if (positiveResult && mCurrentSelected >= 0
                && (mCurrentSelected != mInitValue)) {

            callChangeListener(mCurrentSelected);
            notifyChanged();
        }
    }

    @Override
    public View getView(View convertView, ViewGroup parent) {
        // TODO Auto-generated method stub

        View view = super.getView(convertView, parent);

        if (view != null) {

            TextView textSummary = (TextView) view.findViewById(android.R.id.summary);

            if (textSummary != null) {
                textSummary.setVisibility(View.GONE);
            }

            TextView textColor = (TextView) view.findViewById(R.id.sim_list_color);

            if (textColor != null) {
                textColor.setBackgroundColor(mContext.getResources().getColor(SIM_COLOR[mCurrentSelected]));
            }

        }
        return view;
    }

    /**
     * set init color value
     * 
     * @param colorIndex
     *            int
     */
    public void setInitValue(int colorIndex) {
        mCurrentSelected = colorIndex;
        mInitValue = colorIndex;
    }
}
