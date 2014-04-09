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
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.camera.ListPreference;
import com.android.camera.Log;
import com.android.camera.R;
import com.android.camera.SettingUtils;

/**
 * A one-line camera setting could be one of three types: knob, switch or restore
 * preference button. The setting includes a title for showing the preference
 * title which is initialized in the SimpleAdapter. A knob also includes
 * (ex: Picture size), a previous button, the current value (ex: 5MP),
 * and a next button. A switch, i.e. the preference RecordLocationPreference,
 * has only two values on and off which will be controlled in a switch button.
 * Other setting popup window includes several InLineSettingItem items with
 * different types if possible.
 */
public abstract class InLineSettingItem extends RelativeLayout {
    private static final String TAG = "InLineSettingItem";
    private static final boolean LOG = Log.LOGV;
    
    protected Listener mListener;
    protected ListPreference mPreference;
    protected int mIndex;
    // Scene mode can override the original preference value.
    protected String mOverrideValue;
    protected Animation mFadeIn;
    protected Animation mFadeOut;
    protected TextView mTitle;

    public interface Listener {
        void onSettingChanged(InLineSettingItem item);
        void onShow(InLineSettingItem item);
        void onDismiss(InLineSettingItem item);
    }

    public InLineSettingItem(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mTitle = (TextView) findViewById(R.id.title);
    }

    protected void setTitle(ListPreference preference) {
        if (preference != null) {
            mTitle.setText(preference.getTitle());
        } else {
            mTitle.setText(null);
        }
    }

    public void initialize(ListPreference preference) {
        if (LOG) {
            Log.v(TAG, "initialize(" + preference + ")");
        }
        setTitle(preference);
        if (preference == null || mPreference == preference) { return; }
        mPreference = preference;
        reloadPreference();
    }

    protected abstract void updateView();

    protected boolean changeIndex(int index) {
        if (index >= mPreference.getEntryValues().length || index < 0) { return false; }
        mIndex = index;
        mPreference.setValueIndex(mIndex);
        if (mListener != null) {
            mListener.onSettingChanged(this);
        }
        updateView();
        sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);
        return true;
    }

    // The value of the preference may have changed. Update the UI.
    public void reloadPreference() {
        if (mPreference != null) {
            mPreference.reloadValue();
            mIndex = mPreference.findIndexOfValue(mPreference.getValue());
            updateView();
        }
        if (LOG) {
            Log.v(TAG, "reloadPreference() mPreference=" + mPreference + ", mIndex=" + mIndex);
        }
    }

    public void setSettingChangedListener(Listener listener) {
        mListener = listener;
    }
    
    public ListPreference getPreference() {
        return mPreference;
    }
    
    public boolean expendChild() {
        return false;
    }
    
    public boolean collapseChild() {
        return false;
    }
    
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        SettingUtils.setEnabledState(this, enabled);
    }
    
    public void fadeOut(View view) {
        if (view == null) { return; }
        if (mFadeOut == null) {
            mFadeOut = AnimationUtils.loadAnimation(getContext(),
                    R.anim.setting_popup_shrink_fade_out);
        }
        if (mFadeOut != null) {
            view.startAnimation(mFadeOut);
        }
    }
    
    public void fadeIn(View view) {
        if (view == null) { return; }
        if (mFadeIn == null) {
            mFadeIn = AnimationUtils.loadAnimation(getContext(),
                    R.anim.setting_popup_grow_fade_in);
        }
        if (mFadeIn != null) {
            view.startAnimation(mFadeIn);
        }
    }
}
