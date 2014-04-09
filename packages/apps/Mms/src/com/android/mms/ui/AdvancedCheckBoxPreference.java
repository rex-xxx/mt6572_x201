/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.android.mms.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.CheckBoxPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.mms.R;

/**
 * M: AdvancedCheckBoxPreference
 */
public class AdvancedCheckBoxPreference extends CheckBoxPreference {

    private static final int SHOW_NUMBER_LENGTH = 4;
    // for object reference count;
    private int mCurrentId = 0;
    private static final String TAG = "AdvancedCheckBoxPreference";

    private static TextView sSimName;
    private static TextView sSimNumber;
    private static TextView sSimNumberShort;
    private static TextView sSim3G;
    private static ImageView sSimStatus;
    private static ImageView sSimColor;
    private GetSimInfo mSimInfo;
    private Context mContext;

    public AdvancedCheckBoxPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        TypedArray a = context.obtainStyledAttributes(attrs,
            com.android.internal.R.styleable.CheckBoxPreference, defStyle, 0);
        a.recycle();
    }

    public AdvancedCheckBoxPreference(Context context, AttributeSet attrs) {
        this(context, attrs, com.android.internal.R.attr.checkBoxPreferenceStyle);
    }

    public AdvancedCheckBoxPreference(Context context) {
        this(context, null);
    }

    public void init(Context context, int id) {
        mSimInfo = (GetSimInfo) context;
        mContext = context;
        mCurrentId = id;
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        final LayoutInflater layoutInflater = (LayoutInflater) getContext().getSystemService(
            Context.LAYOUT_INFLATER_SERVICE);
        final View layout = layoutInflater.inflate(R.layout.advanced_checkbox_preference, parent,
            false);

        sSimName = new TextView(mContext);
        sSimNumber = new TextView(mContext);
        sSimNumberShort = new TextView(mContext);
        sSimStatus = new ImageView(mContext);
        sSimColor = new ImageView(mContext);
        sSim3G = new TextView(mContext);

        return layout;
    }

    @Override
    // called when we're binding the view to the preference.
    protected void onBindView(View view) {
        super.onBindView(view);
        sSimName = (TextView) view.findViewById(R.id.simName);
        sSimNumber = (TextView) view.findViewById(R.id.simNumber);
        sSimNumberShort = (TextView) view.findViewById(R.id.simNumberShort);
        sSimStatus = (ImageView) view.findViewById(R.id.simStatus);
        sSimColor = (ImageView) view.findViewById(R.id.simIcon);
        sSim3G = (TextView) view.findViewById(R.id.sim3g);
        // here need change to common usage
        sSimName.setText(mSimInfo.getSimName(mCurrentId));
        if (mSimInfo.getSimNumber(mCurrentId) == null || mSimInfo.getSimNumber(mCurrentId).toString().equals("")) {
            sSimNumber.setVisibility(View.GONE);
        } else {
            sSimNumber.setText(mSimInfo.getSimNumber(mCurrentId));
        }
        String numShow = (String) mSimInfo.getSimNumber(mCurrentId);

        if (mSimInfo.getNumberFormat(mCurrentId) == android.provider.Telephony.SimInfo.DISPLAY_NUMBER_FIRST) {
            if (numShow != null && numShow.length() > SHOW_NUMBER_LENGTH) {
                sSimNumberShort.setText(numShow.substring(0, SHOW_NUMBER_LENGTH));
            } else {
                sSimNumberShort.setText(numShow);
            }
        } else if (mSimInfo.getNumberFormat(mCurrentId) == android.provider.Telephony.SimInfo.DISPLAY_NUMBER_LAST) {
            if (numShow != null && numShow.length() > SHOW_NUMBER_LENGTH) {
                sSimNumberShort.setText(numShow.substring(numShow.length() - SHOW_NUMBER_LENGTH));
            } else {
                sSimNumberShort.setText(numShow);
            }
        } else {
            sSimNumberShort.setText("");
        }

        int simStatusResourceId = MessageUtils
                .getSimStatusResource(mSimInfo.getSimStatus(mCurrentId));
        if (-1 != simStatusResourceId) {
            sSimStatus.setImageResource(simStatusResourceId);
        }
        sSimColor.setBackgroundResource(mSimInfo.getSimColor(mCurrentId));
        sSim3G.setVisibility(View.GONE);
    }

    public void setNotifyChange(Context context) {
        mSimInfo = (GetSimInfo) context;
        notifyChanged();
    }

    public interface GetSimInfo {

        CharSequence getSimNumber(int i);

        CharSequence getSimName(int i);

        int getSimColor(int i);

        int getNumberFormat(int i);

        int getSimStatus(int i);

        boolean is3G(int i);
    }

}
