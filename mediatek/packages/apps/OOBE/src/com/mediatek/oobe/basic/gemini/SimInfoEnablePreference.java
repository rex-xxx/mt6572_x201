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
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import com.mediatek.oobe.R;

public class SimInfoEnablePreference extends SimInfoPreference implements OnClickListener {

    interface OnPreferenceClickCallback {

        void onPreferenceClick(long simid);
    }

    private static final String TAG = "SimInfoEnablePreference";
    private View.OnClickListener mCheckBoxClickListener;
    private boolean mRadioOn;
    private OnPreferenceClickCallback mClickCallback;

    /**
     * SimInfoEnablePreference constructor
     * @param context Context
     * @param name String
     * @param number String
     * @param simSlot int
     * @param status int
     * @param color int
     * @param displayNumberFormat int
     * @param key long
     */
    public SimInfoEnablePreference(Context context, String name, String number, int simSlot, int status, int color,
            int displayNumberFormat, long key) {
        super(context, name, number, simSlot, status, color, displayNumberFormat, key, false);
        mRadioOn = true;
        setLayoutResource(R.layout.preference_sim_info_enabler);
    }

    @Override
    public View getView(View convertView, ViewGroup parent) {
        // TODO Auto-generated method stub
        View view = super.getView(convertView, parent);

        CheckBox ckRadioOn = (CheckBox) view.findViewById(R.id.Check_Enable);

        if (ckRadioOn != null) {
            if (mCheckBoxClickListener != null) {
                ckRadioOn.setClickable(true);
                ckRadioOn.setFocusable(true);
                ckRadioOn.setOnClickListener(mCheckBoxClickListener);

            }
        }
        View siminfoLayout = view.findViewById(R.id.sim_info_layout);
        if ((siminfoLayout != null) && siminfoLayout instanceof LinearLayout) {
            siminfoLayout.setOnClickListener(this);
            siminfoLayout.setFocusable(true);

        }

        return view;
    }

    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub

        if (v == null) {
            return;
        }

        if ((v.getId() != R.id.Check_Enable) && (mClickCallback != null)) {
            mClickCallback.onPreferenceClick(Long.valueOf(getKey()));
        }

    }

    /**
     * Set CheckBox Click Listener
     * @param listerner see @View.OnClickListener
     */
    void setCheckBoxClickListener(View.OnClickListener listerner) {
        mCheckBoxClickListener = listerner;

    }

    void setClickCallback(OnPreferenceClickCallback callBack) {
        mClickCallback = callBack;
    }

    boolean isRadioOn() {
        return mRadioOn;
    }

    void setRadioOn(boolean radioOn) {
        mRadioOn = radioOn;

    }

}
