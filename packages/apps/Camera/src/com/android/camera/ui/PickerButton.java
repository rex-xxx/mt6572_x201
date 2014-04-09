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

package com.android.camera.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import com.android.camera.IconListPreference;
import com.android.camera.Log;

/**
 * A view for switching the front/back camera.
 */
public class PickerButton extends RotateImageView implements View.OnClickListener {
    private static final String TAG = "PickButton";
    private static final boolean LOG = Log.LOGV;
    
    public interface Listener {
        boolean onPicked(PickerButton button, String key, String newValue);
    }
    
    protected Listener mListener;
    protected IconListPreference mPreference;
    
    public PickerButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    public void setListener(Listener listener) {
        mListener = listener;
    }
    
    public void initialize(IconListPreference pref) {
        if (LOG) {
            Log.v(TAG, "initialize(" + pref + ")");
        }
        mPreference = pref;
        reloadPreference();
    }
    
    public void reloadPreference() {
        if (LOG) {
            Log.v(TAG, "reloadPreference() " + mPreference);
        }
        if (mPreference == null || mPreference.getEntries() == null
                || mPreference.getEntries().length <= 1
                || !mPreference.isEnabled()) {
            setVisibility(View.GONE);
        } else {
            setOnClickListener(null);
            String value = mPreference.getOverrideValue();
            if (value == null) {
                value = mPreference.getValue();
            }
            int index = mPreference.findIndexOfValue(value);
            int[] icons = mPreference.getIconIds();
            if (icons != null) {
                if (index >= 0 && index < icons.length) {
                    setImageResource(icons[index]);
                } else {
                    index = getValidIndexIfNotFind(value);
                    setImageResource(icons[index]);
                }
            } 
            setOnClickListener(this);
            setVisibility(View.VISIBLE);
        }
    }
    
    public void updateView() {
        if (mPreference == null || mPreference.getEntries() == null
                || mPreference.getEntries().length <= 1
                || !mPreference.isEnabled()) {
            setVisibility(View.GONE);
        } else {
            setVisibility(View.VISIBLE);
        }
    }
    
    @Override
    public void onClick(View v) {
        if (mPreference == null) {
            Log.w(TAG, "onClick() why mPreference is null?", new Throwable());
            return;
        }
        // Find the index of next camera.
        String value = mPreference.getOverrideValue();
        if (value == null) {
            value = mPreference.getValue();
        }
        int index = mPreference.findIndexOfValue(value);
        if (index < 0) { //make 0 as default
            index = getValidIndexIfNotFind(value);
        }
        CharSequence[] values = mPreference.getEntryValues();
        //do {
        index = (index + 1) % values.length;
        //} while (!mPreference.isEnabled(index));
        String next = values[index].toString();
        if (mListener != null && mListener.onPicked(this, mPreference.getKey(), next)) {
            //clear override value after user changed it
            mPreference.setOverrideValue(null, false);
            mPreference.setValueIndex(index);//should be checked
            reloadPreference();
        }
    }
    
    public void setValue(String value) {
        if (LOG) {
            Log.v(TAG, "setValue(" + value + ") mPreference=" + mPreference);
        }
        if (mPreference != null && value != null && !value.endsWith(mPreference.getValue())) {
            mPreference.setValue(value);
            reloadPreference();
        }
    }
    
    protected int getValidIndexIfNotFind(String value) {
        return 0;
    }
}
