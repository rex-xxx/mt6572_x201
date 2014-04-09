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

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.mediatek.xlog.Xlog;

public class UsbSharingInfo extends SettingsPreferenceFragment implements Button.OnClickListener {
    public static final String TAG = "UsbSharingInfo";
    private static final int WIN_XP = 1;
    private static final int WIN_VISTA = 2;
    private static final int WIN_SEVEN = 3;
    private static final int WIN_EIGHT = 4;

    private Button mBackBtn;
    private Button mNextBtn;
    private int mSelectedSystemIndex;
    private ConnectivityManager mConnectivityManager;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mConnectivityManager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        Bundle bundle = this.getArguments();
        Xlog.d(TAG, "onCreate activity,bundle = " + bundle + ",this = " + this);

        if (bundle != null) {
            mSelectedSystemIndex = bundle.getInt(UsbSharingChoose.SYSTEM_TYPE);
        }
        Xlog.d(TAG, "index is " + mSelectedSystemIndex);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = getActivity().getLayoutInflater().inflate(R.layout.usb_sharing_info, null);
        LinearLayout mProgressbarLayout = (LinearLayout) view.findViewById(R.id.progressbar_layout);
        mBackBtn = (Button) view.findViewById(R.id.panel_button_back);
        mBackBtn.setOnClickListener(this);
        mNextBtn = (Button) view.findViewById(R.id.panel_button_next);
        mNextBtn.setText(R.string.wifi_display_options_done);
        mNextBtn.setOnClickListener(this);
        
        ImageView child = (ImageView) mProgressbarLayout.getChildAt(1);
        child.setImageResource(R.drawable.progress_radio_on); 
        return view;
    }
    
    @Override
    public void onClick(View v) {
        if (v == mNextBtn) {
            //set the value to framework
            if (mSelectedSystemIndex == WIN_XP || mSelectedSystemIndex == WIN_VISTA) {
                mConnectivityManager.setUsbInternet(true, ConnectivityManager.USB_INTERNET_SYSTEM_WINXP);
            } else if (mSelectedSystemIndex == WIN_SEVEN || mSelectedSystemIndex == WIN_EIGHT) {
                mConnectivityManager.setUsbInternet(true, ConnectivityManager.USB_INTERNET_SYSTEM_WIN7);
            }
        } else if (v == mBackBtn) {
            Bundle bundle = new Bundle();
            bundle.putInt(UsbSharingChoose.SYSTEM_TYPE, mSelectedSystemIndex);
            startFragment(this, UsbSharingChoose.class.getName(), 0, bundle, R.string.usb_sharing_title);
            getActivity().overridePendingTransition(R.anim.slide_left_in, R.anim.slide_right_out);
        }
        finishFragment();
    }

}
