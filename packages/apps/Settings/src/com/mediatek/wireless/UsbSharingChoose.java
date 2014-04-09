/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.mediatek.wireless;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.mediatek.xlog.Xlog;

public class UsbSharingChoose extends SettingsPreferenceFragment implements Button.OnClickListener,
        OnItemSelectedListener {
    public static final String TAG = "UsbSharingChoose";
    public static final String SYSTEM_TYPE = "system_type";
    private Button mBackBtn;
    private Button mNextBtn;
    private Spinner mSelectSystemSpinner;
    
    public UsbSharingChoose() {
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = getActivity().getLayoutInflater().inflate(R.layout.usb_sharing_choose, null);
       
        mSelectSystemSpinner = (Spinner) view.findViewById(R.id.select_system_spinner);
        mSelectSystemSpinner.setOnItemSelectedListener(this);
        Bundle bundle = this.getArguments();
        if (bundle != null) {
            int index = bundle.getInt(UsbSharingChoose.SYSTEM_TYPE);
            mSelectSystemSpinner.setSelection(index);
        }
        
        mBackBtn = (Button) view.findViewById(R.id.panel_button_back);
        mBackBtn.setText(null);
        mNextBtn = (Button) view.findViewById(R.id.panel_button_next);
        mNextBtn.setOnClickListener(this);
        
        LinearLayout mProgressbarLayout = (LinearLayout) view.findViewById(R.id.progressbar_layout);
        ImageView child = (ImageView) mProgressbarLayout.getChildAt(0);
        child.setImageResource(R.drawable.progress_radio_on);
        return view;
    }
    
    @Override
    public void onClick(View v) {
        if (v == mNextBtn) {
            Bundle bundle = new Bundle();
            int selectSystem = mSelectSystemSpinner.getSelectedItemPosition();
            Xlog.d(TAG, "select system is " + selectSystem);
            bundle.putInt(SYSTEM_TYPE, selectSystem);
            startFragment(this, UsbSharingInfo.class.getName(), 0, bundle, R.string.usb_sharing_title);
            getActivity().overridePendingTransition(R.anim.slide_right_in, R.anim.slide_left_out);
            finishFragment();
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent.equals(mSelectSystemSpinner)) {
            mNextBtn.setEnabled(mSelectSystemSpinner.getSelectedItemPosition() != 0);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

}
