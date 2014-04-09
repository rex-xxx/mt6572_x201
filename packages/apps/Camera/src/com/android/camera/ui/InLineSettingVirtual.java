/*
 * Copyright (C) 2011 The Android Open Source Project
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
import android.view.accessibility.AccessibilityEvent;
import android.widget.TextView;

import com.android.camera.Camera;
import com.android.camera.ListPreference;
import com.android.camera.Log;
import com.android.camera.R;
import com.android.camera.SettingUtils;
import com.android.camera.Util;
import com.android.camera.manager.ViewManager;

/**
 * In Virtual item layout:
 * ListPreference.getEntries() return child list preference key
 * ListPreference.getEntryValues() return child list default value
 * ListPreference.getDefaultValue() return default string displayed for final user.
 */
public class InLineSettingVirtual extends InLineSettingItem
        implements SettingVirtualLayout.Listener,
        Camera.OnOrientationListener {
    private static final String TAG = "InLineSettingVirtualItem";
    private static final boolean LOG = Log.LOGV;
    
    private Camera mContext;
    private TextView mEntry;
    private SettingVirtualLayout mSettingLayout;
    private View mSettingContainer;
    private boolean mShowingChildList;
    private ListPreference[] mChildPrefs;
    
    private OnClickListener mOnClickListener = new OnClickListener() {
        
        @Override
        public void onClick(View view) {
            if (LOG) {
                Log.v(TAG, "onClick() mShowingChildList=" + mShowingChildList + ", mPreference=" + mPreference);
            }
            if (!mShowingChildList && mChildPrefs != null && mPreference != null && mPreference.isClickable()) {
                expendChild();
            } else {
                collapseChild();
            }
        }
    };

    public InLineSettingVirtual(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = (Camera)context;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mEntry = (TextView) findViewById(R.id.current_setting);
        setOnClickListener(mOnClickListener);
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        onPopulateAccessibilityEvent(event);
        return true;
    }

    @Override
    public void onPopulateAccessibilityEvent(AccessibilityEvent event) {
        super.onPopulateAccessibilityEvent(event);
        event.getText().add(mPreference.getTitle() + mPreference.getEntry());
    }
    
    @Override
    public void initialize(ListPreference preference) {
        if (LOG) {
            Log.v(TAG, "initialize(" + preference + ")");
        }
        setTitle(preference);
        if (preference == null) {
            mChildPrefs = null;
            return;
        }
        mPreference = preference;
        reloadPreference();
    }
    
    // The value of the preference may have changed. Update the UI.
    @Override
    public void reloadPreference() {
        int len = mPreference.getEntries().length;
        mChildPrefs = new ListPreference[len];
        for (int i = 0; i < len; i++) {
            String key = String.valueOf(mPreference.getEntries()[i]);
            mChildPrefs[i] = mContext.getListPreference(key);
            if (LOG) {
                Log.v(TAG, "reloadPreference() mChildPrefs[" + i + "|" + key + "]=" + mChildPrefs[i]);
            }
        }
        updateView();
    }
    
    @Override
    protected void updateView() {
        if (mPreference == null || mChildPrefs == null) {
            return;
        }
        setOnClickListener(null);
        int len = mChildPrefs.length;
        boolean allDefault = true;
        int enableCount = 0;
        for (int i = 0; i < len; i++) {
            ListPreference pref = mChildPrefs[i];
            if (pref == null) {
                continue;
            }
            String defaultValue = String.valueOf(mPreference.getEntryValues()[i]);
            String value = pref.getOverrideValue();
            if (value == null) {
                value = pref.getValue();
            }
            if (pref.isEnabled()) {
                enableCount++;
            }
            //we assume pref and default value is not null.
            if (allDefault && !defaultValue.equals(value)) {
                allDefault = false;
            }
        }
        if (allDefault) {
            mEntry.setText(mPreference.getDefaultValue());
        } else {
            mEntry.setText("");
        }
        boolean enabled = (enableCount == len);
        mPreference.setEnabled(enabled);
        setEnabled(mPreference.isEnabled());
        setOnClickListener(mOnClickListener);
        if (LOG) {
            Log.v(TAG, "updateView() enableCount=" + enableCount + ", len=" + len);
        }
    }
    
    public boolean expendChild() {
        boolean expend = false;
        if (!mShowingChildList) {
            mShowingChildList = true;
            mSettingLayout = (SettingVirtualLayout)mContext.inflate(R.layout.setting_virtual_layout,
                    ViewManager.VIEW_LAYER_SETTING);
            mSettingContainer = mSettingLayout.findViewById(R.id.container);
            mSettingLayout.initialize(mChildPrefs);
            mContext.addView(mSettingLayout, ViewManager.VIEW_LAYER_SETTING);
            mContext.addOnOrientationListener(this);
            mSettingLayout.setSettingChangedListener(this);
            setOrientation(mContext.getOrientationCompensation(), false);
            fadeIn(mSettingLayout);
            highlight();
            if (mListener != null) {
                mListener.onShow(this);
            }
            expend = true;
        }
        if (LOG) {
            Log.v(TAG, "expendChild() return " + expend);
        }
        return expend;
    }
    
    public boolean collapseChild() {
        boolean collapse = false;
        if (mShowingChildList) {
            mContext.removeOnOrientationListener(this);
            mContext.removeView(mSettingLayout, ViewManager.VIEW_LAYER_SETTING);
            fadeOut(mSettingLayout);
            normalText();
            mSettingLayout = null;
            mShowingChildList = false;
            if (mListener != null) {
                mListener.onDismiss(this);
            }
            collapse = true;
        }
        if (LOG) {
            Log.v(TAG, "collapseChild() return " + collapse);
        }
        return collapse;
    }
    
    private void highlight() {
        if (mTitle != null) {
            mTitle.setTextColor(SettingUtils.getMainColor(getContext()));
        }
        if (mEntry != null) {
            mEntry.setTextColor(SettingUtils.getMainColor(getContext()));
        }
        setBackgroundDrawable(null);
    }
    
    private void normalText() {
        if (mTitle != null) {
            mTitle.setTextColor(getResources().getColor(R.color.setting_item_text_color_normal));
        }
        if (mEntry != null) {
            mEntry.setTextColor(getResources().getColor(R.color.setting_item_text_color_normal_text));
        }
        setBackgroundResource(R.drawable.setting_picker);
    }
    
    @Override
    public void onSettingChanged() {
        if (mListener != null) {
            mListener.onSettingChanged(this);
        }
    }

    @Override
    public void onOrientationChanged(int orientation) {
        setOrientation(orientation, true);
    }
    
    private void setOrientation(int orientation, boolean animation) {
        if (LOG) {
            Log.v(TAG, "setOrientation(" + orientation + ", " + animation + ")");
        }
        if (mShowingChildList) {
            Util.setOrientation(mSettingLayout, orientation, animation);
        }
    }
    
    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (!enabled) {
            collapseChild();
        }
    }
}
