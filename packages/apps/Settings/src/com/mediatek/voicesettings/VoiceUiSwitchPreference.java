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

package com.mediatek.voicesettings;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.android.settings.R;
import com.mediatek.xlog.Xlog;

public class VoiceUiSwitchPreference extends Preference {

    private static final String TAG = "VoiceUiSwitchPreference";
    private static final String VOICE_CONTROL_ENABLED = "voice_control_enabled";
    private boolean mChecked = false;
    private OnCheckedChangeListener mSwitchChangeListener;

    private CompoundButton.OnCheckedChangeListener mSwitchButtonListener = new CompoundButton.OnCheckedChangeListener() {

        @Override
        public void onCheckedChanged(CompoundButton buttonView,
                boolean isChecked) {
            Xlog.d(TAG, "onCheckedChanged isChecked=" + isChecked + " mChecked=" + mChecked);
            // TODO Auto-generated method stub
            if (isChecked != mChecked) {
                setChecked(isChecked);
            }
        }
        
    };

    /**
     * Interface definition for a callback to be invoked when the checked state
     * of a compound button changed.
     */
    public interface OnCheckedChangeListener {
        /**
         * Called when the checked state of a compound button has changed.
         *
         * @param isChecked  The new checked state.
         */
        void onCheckedChanged(boolean isChecked);

        /**
         * previous to the real state changed
         * @param isChecked  The new checked state.
         * @return boolean
         */
        boolean onBeforeCheckedChanged(boolean isChecked);
    }

    /**
     * constructor of class
     * @param context Context
     * @param attrs AttributeSet
     * @param defStyle int
     */
    public VoiceUiSwitchPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setLayoutResource(R.layout.custom_switch_preference);
    }

    /**
     * constructor of class
     * @param context Context
     * @param attrs AttributeSet
     */
    public VoiceUiSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.custom_switch_preference);

    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        Switch actionSwitch = (Switch) view.findViewById(R.id.switch_);
        if (mSwitchButtonListener != null && actionSwitch != null) {
            actionSwitch.setClickable(true);
            actionSwitch.setOnCheckedChangeListener(mSwitchButtonListener);
            actionSwitch.setChecked(mChecked);
        }
    }

    /**
     * set checked
     * @param checked boolean
     */
    public void setChecked(boolean checked) {
        if (mSwitchChangeListener != null &&
                mSwitchChangeListener.onBeforeCheckedChanged(checked)) {
            mChecked = checked;
            mSwitchChangeListener.onCheckedChanged(mChecked);
        }
        notifyChanged();
    }

 /**
     * set checked
     * @param checked boolean
     *  not call back function, fix switch if flash when change language
     */
    public void setChecked(boolean checked, boolean isCallback) {
        if (!isCallback) {
            mChecked = checked;
        }
        notifyChanged();
    }


    /**
     * get status
     * @return boolean
     */
    public boolean isChecked() {
        return mChecked;

    }

    /**
     * set OnCheckedChange Listener
     * @param listener OnCheckedChangeListener
     */
    public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
        Xlog.d(TAG, "setOnCheckedChangeListener");
        if (listener != null) {
            mSwitchChangeListener = listener;
        }
    }
}
