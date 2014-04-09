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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;

import com.android.camera.Camera;
import com.android.camera.CameraSettings;
import com.android.camera.ListPreference;
import com.android.camera.Log;
import com.android.camera.R;
import com.android.camera.SettingUtils;

import java.util.ArrayList;
//import com.mediatek.common.featureoption.FeatureOption;

/* A popup window that contains several camera settings. */
public class SettingListLayout extends FrameLayout implements InLineSettingItem.Listener,
        AdapterView.OnItemClickListener, OnScrollListener {
    @SuppressWarnings("unused")
    private static final String TAG = "SettingListLayout";
    private static final boolean LOG = Log.LOGV;

    private Listener mListener;
    private ArrayList<ListPreference> mListItem = new ArrayList<ListPreference>();
    private ArrayAdapter<ListPreference> mListItemAdapter;
    private InLineSettingItem mLastItem;
    private ListView mSettingList;

    public interface Listener {
        void onSettingChanged(SettingListLayout settingList);
        void onRestorePreferencesClicked();
    }
    
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mSettingList = (ListView) findViewById(R.id.settingList);
    }

    private class SettingsListAdapter extends ArrayAdapter<ListPreference> {
        LayoutInflater mInflater;

        public SettingsListAdapter() {
            super(SettingListLayout.this.getContext(), 0, mListItem);
            mInflater = LayoutInflater.from(getContext());
        }

        private int getSettingLayoutId(ListPreference pref) {
            // If the preference is null, it will be the only item , i.e.
            // 'Restore setting' in the popup window.
            if (pref == null) {
                return R.layout.in_line_setting_restore;
            }

            // Currently, the RecordLocationPreference is the only setting
            // which applies the on/off switch.
            if (isSwitchSettingItem(pref)) {
                return R.layout.in_line_setting_switch;
            } else if (isVirtualSettingItem(pref)) {
                return R.layout.in_line_setting_virtual;
            }
            return R.layout.in_line_setting_sublist;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ListPreference pref = mListItem.get(position);
            if (convertView != null) {
                if (pref == null) {
                    if (!(convertView instanceof InLineSettingRestore)) {
                        convertView = null;
                    }
                } else if (isSwitchSettingItem(pref)) {
                    if (!(convertView instanceof InLineSettingSwitch)) {
                        convertView = null;
                    }
                } else if (isVirtualSettingItem(pref)) {
                    if (!(convertView instanceof InLineSettingVirtual)) {
                        convertView = null;
                    }
                } else {
                    if (!(convertView instanceof InLineSettingSublist)) {
                        convertView = null;
                    }
                }
                if (convertView != null) {
                    ((InLineSettingItem)convertView).initialize(pref);
                    SettingUtils.setEnabledState(convertView, (pref == null ? true : pref.isEnabled()));
                    return convertView;
                }
            }

            int viewLayoutId = getSettingLayoutId(pref);
            InLineSettingItem view = (InLineSettingItem)
                    mInflater.inflate(viewLayoutId, parent, false);
            if (viewLayoutId == R.layout.in_line_setting_restore) {
                view.setId(R.id.restore_default);
            }

            view.initialize(pref); // no init for restore one
            view.setSettingChangedListener(SettingListLayout.this);
            SettingUtils.setEnabledState(convertView, (pref == null ? true : pref.isEnabled()));
            return view;
        }
    }

    private boolean isSwitchSettingItem(ListPreference pref) {
        return CameraSettings.KEY_RECORD_LOCATION.equals(pref.getKey())
                || CameraSettings.KEY_VIDEO_RECORD_AUDIO.equals(pref.getKey())
                || CameraSettings.KEY_VIDEO_EIS.equals(pref.getKey())
                || CameraSettings.KEY_CAMERA_ZSD.equals(pref.getKey())
                || CameraSettings.KEY_VOICE.equals(pref.getKey())
                || CameraSettings.KEY_CAMERA_FACE_DETECT.equals(pref.getKey());
    }

    private boolean isVirtualSettingItem(ListPreference pref) {
        return CameraSettings.KEY_IMAGE_PROPERTIES.equals(pref.getKey())
                || CameraSettings.KEY_FACE_BEAUTY_PROPERTIES.equals(pref.getKey());
    }

    public void setSettingChangedListener(Listener listener) {
        mListener = listener;
    }

    public SettingListLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void initialize(String[] keys, boolean addrestore) {
        Camera context = (Camera)getContext();
        mListItem.clear();
        // Prepare the setting items.
        for (int i = 0; i < keys.length; ++i) {
            ListPreference pref = context.getListPreference(keys[i]);
            if (pref != null) {
                mListItem.add(pref);
            }
        }
        // Prepare the restore setting line.
        if (addrestore) {
            mListItem.add(null);
        }

        mListItemAdapter = new SettingsListAdapter();
        mSettingList.setAdapter(mListItemAdapter);
        mSettingList.setOnItemClickListener(this);
        mSettingList.setSelector(android.R.color.transparent);
        mSettingList.setOnScrollListener(this);
    }

    @Override
    public void onSettingChanged(InLineSettingItem item) {
        if (mLastItem != null && mLastItem != item) {
            mLastItem.collapseChild();
        }
        if (mListener != null) {
            mListener.onSettingChanged(this);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if ((position == mListItem.size() - 1) && (mListener != null)) {
            mListener.onRestorePreferencesClicked();
        }
    }

    public void reloadPreference() {
        int count = mSettingList.getChildCount();
        for (int i = 0; i < count; i++) {
            ListPreference pref = mListItem.get(i);
            if (pref != null) {
                InLineSettingItem settingItem =
                        (InLineSettingItem) mSettingList.getChildAt(i);
                settingItem.reloadPreference();
            }
        }
    }

    @Override
    public void onDismiss(InLineSettingItem item) {
        Log.v(TAG, "onDismiss(" + item + ") mLastItem=" + mLastItem);
        mLastItem = null;
    }

    @Override
    public void onShow(InLineSettingItem item) {
        if (LOG) {
            Log.v(TAG, "onShow(" + item + ") mLastItem=" + mLastItem);
        }
        if (mLastItem != null && mLastItem != item) {
            mLastItem.collapseChild();
        }
        mLastItem = item;
    }
    
    public boolean collapseChild() {
        boolean collapse = false;
        if (mLastItem != null) {
            collapse = mLastItem.collapseChild();
        }
        if (LOG) {
            Log.v(TAG, "collapseChild() return " + collapse);
        }
        return collapse;
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
            int totalItemCount) {
        if (LOG) {
            Log.v(TAG, "onScroll(" + firstVisibleItem + ", " + visibleItemCount
                    + ", " + totalItemCount + ")");
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if (LOG) {
            Log.v(TAG, "onScrollStateChanged(" + scrollState + ")");
        }
        if (scrollState == SCROLL_STATE_TOUCH_SCROLL) {
            collapseChild();
        }
    }
}
