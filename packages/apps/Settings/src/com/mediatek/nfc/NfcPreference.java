/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mediatek.nfc;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.android.settings.R;
import com.mediatek.xlog.Xlog;


public class NfcPreference extends Preference implements
        CompoundButton.OnCheckedChangeListener {
    private static final String TAG = "NfcPreference";

    private TextView mPreferenceTitle = null;
    private Switch mPreferenceSwitch = null;

    private CharSequence mTitleValue = "";
    private boolean mChecked = false;
    private LayoutInflater mInflater;
    private Context mContext;

    public NfcPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;

        if (super.getTitle() != null) {
            mTitleValue = super.getTitle().toString();
        }
    }

    public NfcPreference(Context context) {
        this(context, null);
    }

    public NfcPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    @Override
    public View onCreateView(ViewGroup parent) {
        Xlog.d(TAG, "onCreateView");

        mInflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = mInflater.inflate(R.layout.preference_nfc, null);

        mPreferenceTitle = (TextView) view.findViewById(R.id.nfc_preference_title);
        mPreferenceTitle.setText(mTitleValue);
        mPreferenceSwitch = (Switch) view.findViewById(R.id.nfc_preference_switch);
        mPreferenceSwitch.setOnCheckedChangeListener(this);
        mPreferenceSwitch.setChecked(mChecked);
        return view;
    }

    public boolean isChecked() {
        return mChecked;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Xlog.d(TAG, "oncheckChanged to " + isChecked);
        if (setChecked(isChecked)) {
            callChangeListener(isChecked);
        }
    }

    /**
     * set the preference checked status
     * @param checked the checked status
     * @return true, callChangeListener, it will callback in 
     * MtkNfcEnabler.onPreferceChange()
     */
    public boolean setChecked(boolean checked) {
        if (null == mPreferenceSwitch) {
            Xlog.d(TAG, "setChecked return");
            mChecked = checked;
            return false;
        }

        if (mChecked != checked) {
            mPreferenceSwitch.setChecked(checked);
            mChecked = checked;
            return true;
        }
        return false;
    }

    /**
     * get the switch button in the NfcPreference
     * @return the switch button
     */
    public Switch getSwitch() {
        return mPreferenceSwitch;
    }
}
