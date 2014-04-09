/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.settings;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.preference.Preference;
import android.provider.Telephony;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RelativeLayout;

import com.android.internal.telephony.PhoneConstants;
import com.mediatek.common.featureoption.FeatureOption;

public class ApnPreference extends Preference implements
        CompoundButton.OnCheckedChangeListener, OnClickListener {
    static final String TAG = "ApnPreference";

    /**
     * @param context
     * @param attrs
     * @param defStyle
     */
    public ApnPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    /**
     * @param context
     * @param attrs
     */
    public ApnPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * @param context
     */
    public ApnPreference(Context context) {
        super(context);
        init();
    }

    private static String sSelectedKey = null;
    private static CompoundButton sCurrentChecked = null;
    private boolean mProtectFromCheckedChange = false;
    private boolean mSelectable = true;
    
    private int mSourceType = 2;
    private int mSimId = 0;
    private static boolean sIsCU = false;
    private boolean mEditable = true;

    @Override
    public View getView(View convertView, ViewGroup parent) {
        View view = super.getView(convertView, parent);

        View widget = view.findViewById(R.id.apn_radiobutton);
        if ((widget != null) && widget instanceof RadioButton) {
            RadioButton rb = (RadioButton) widget;
            if (mSelectable) {
                rb.setOnCheckedChangeListener(this);

                boolean isChecked = getKey().equals(sSelectedKey);
                if (isChecked) {
                    sCurrentChecked = rb;
                    sSelectedKey = getKey();
                }

                mProtectFromCheckedChange = true;
                rb.setChecked(isChecked);
                mProtectFromCheckedChange = false;
            } else {
                rb.setVisibility(View.GONE);
            }
        }

        View textLayout = view.findViewById(R.id.text_layout);
        if ((textLayout != null) && textLayout instanceof RelativeLayout) {
            textLayout.setOnClickListener(this);
        }

        return view;
    }

    private void init() {
        setLayoutResource(R.layout.apn_preference_layout);
    }

    public boolean isChecked() {
        return getKey().equals(sSelectedKey);
    }

    public void setChecked() {
        sSelectedKey = getKey();
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Log.i(TAG, "ID: " + getKey() + " :" + isChecked);
        if (mProtectFromCheckedChange) {
            return;
        }

        if (isChecked) {
            if (sCurrentChecked != null) {
                sCurrentChecked.setChecked(false);
            }
            sCurrentChecked = buttonView;
            sSelectedKey = getKey();
            callChangeListener(sSelectedKey);
        } else {
            sCurrentChecked = null;
            sSelectedKey = null;
        }
    }

    public void onClick(android.view.View v) {
        if ((v != null) && (R.id.text_layout == v.getId())) {
            Context context = getContext();
            if (context != null) {
                int pos = Integer.parseInt(getKey());
                Uri orig;
                if (FeatureOption.MTK_GEMINI_SUPPORT) {
                    switch (mSimId) {
                        case PhoneConstants.GEMINI_SIM_1:
                        orig = Telephony.Carriers.SIM1Carriers.CONTENT_URI;
                        break;
                        case PhoneConstants.GEMINI_SIM_2:
                        orig = Telephony.Carriers.SIM2Carriers.CONTENT_URI;
                        break;
                        case PhoneConstants.GEMINI_SIM_3:
                        orig = Telephony.Carriers.SIM3Carriers.CONTENT_URI;
                        break;
                        case PhoneConstants.GEMINI_SIM_4:
                        orig = Telephony.Carriers.SIM4Carriers.CONTENT_URI;
                        break;
                        default:
                        Log.d(TAG,"error need to check mSimId = " + mSimId);
                        orig = Telephony.Carriers.SIM1Carriers.CONTENT_URI;
                        break;
                    }
                        
                } else {
                    orig = Telephony.Carriers.CONTENT_URI;
                }

                Uri url = ContentUris.withAppendedId(orig, pos);

                Intent it = new Intent(Intent.ACTION_EDIT, url);
                it.putExtra(PhoneConstants.GEMINI_SIM_ID_KEY, mSimId);
                it.putExtra("readOnly",!mEditable);
                context.startActivity(it);                                   
            }
        }
    }

    public void setSelectable(boolean selectable) {
        mSelectable = selectable;
    }

    public boolean getSelectable() {
        return mSelectable;
    }

    public void setSimId(int simid) {
        mSimId = simid;
    }

    public void setApnEditable(boolean isEditable) {
        mEditable = isEditable;
    }

    public int getSourceType() {
        return mSourceType;
    }

    public void setSourceType(int type) {
        mSourceType = type;
    }
    
}
