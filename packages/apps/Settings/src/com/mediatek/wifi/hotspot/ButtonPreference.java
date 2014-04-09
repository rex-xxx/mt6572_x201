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

package com.mediatek.wifi.hotspot;

import android.content.Context;
import android.net.wifi.HotspotClient;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.android.settings.R;

public class ButtonPreference extends Preference implements
        OnClickListener {
    private String mText;
    private Button mButton;
    private HotspotClient mHotspotClient;
    private String mMacAddress;
    private String mIpAddress;
    private OnButtonClickCallback mCallBack;

    interface OnButtonClickCallback {
        void onClick(View v, HotspotClient client);
    }

    public ButtonPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setWidgetLayoutResource(R.layout.preference_button);
    }

    public ButtonPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWidgetLayoutResource(R.layout.preference_button);
    }

    public ButtonPreference(Context context, HotspotClient hotspotClient,OnButtonClickCallback listner) {
        super(context);
        setWidgetLayoutResource(R.layout.preference_button);
        mHotspotClient = hotspotClient;
        mCallBack = listner;
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        mButton = (Button)view.findViewById(R.id.preference_button);
        mButton.setText(mText);
        mButton.setFocusable(false);
        mButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        if (v == null) {
            return;
        }
        if (mCallBack != null) {
            mCallBack.onClick(v,mHotspotClient);
        }
    }

    public void setButtonText(String text) {
        mText = text;
        notifyChanged();
    }

    public void setMacAddress(String macAddress) {
        mMacAddress = macAddress;
        setTitle(mMacAddress);
    }

    public String getMacAddress() {
        return mMacAddress;
    }

    public boolean isBlocked() {
        return mHotspotClient.isBlocked;
    }
}

