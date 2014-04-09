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

package com.android.systemui.statusbar.policy;

import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.systemui.R;

import com.mediatek.xlog.Xlog;

public class BatteryController extends BroadcastReceiver {
    private static final String TAG = "StatusBar.BatteryController";

    /// M: Support "Battery Percentage Switch"
    private static final String ACTION_BATTERY_PERCENTAGE_SWITCH = "mediatek.intent.action.BATTERY_PERCENTAGE_SWITCH";

    private Context mContext;
    private ArrayList<ImageView> mIconViews = new ArrayList<ImageView>();
    private ArrayList<TextView> mLabelViews = new ArrayList<TextView>();
    /// M: Support "battery percentage". @{
    private boolean mShouldShowBatteryPercentage = false;
    private String mBatteryPercentage = "100%";
    /// @}
    /// M: Support "BATTER_PROTECTION".
    private boolean mBatteryProtection = false;

    private ArrayList<BatteryStateChangeCallback> mChangeCallbacks =
            new ArrayList<BatteryStateChangeCallback>();

    public interface BatteryStateChangeCallback {
        public void onBatteryLevelChanged(int level, boolean pluggedIn);
    }

    public BatteryController(Context context) {
        mContext = context;
        /// M: Support "battery percentage".
        mShouldShowBatteryPercentage = (Settings.Secure.getInt(context
                .getContentResolver(), Settings.Secure.BATTERY_PERCENTAGE, 0) != 0);
        Xlog.d(TAG, "BatteryController mShouldShowBatteryPercentage is "
                + mShouldShowBatteryPercentage);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        /// M: Support "battery percentage".
        filter.addAction(ACTION_BATTERY_PERCENTAGE_SWITCH);
        context.registerReceiver(this, filter);
    }

    public void addIconView(ImageView v) {
        mIconViews.add(v);
    }

    public void addLabelView(TextView v) {
        mLabelViews.add(v);
    }

    /// M: Support "battery percentage". @{
    private  String getBatteryPercentage(Intent batteryChangedIntent) {
        int level = batteryChangedIntent.getIntExtra("level", 0);
        int scale = batteryChangedIntent.getIntExtra("scale", 100);
        return String.valueOf(level * 100 / scale) + "%";
    }
    /// @}

    public void addStateChangedCallback(BatteryStateChangeCallback cb) {
        mChangeCallbacks.add(cb);
    }

    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        Xlog.d(TAG,"BatteryController onReceive action is " + action);
        if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
            final int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            final int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS,
                    BatteryManager.BATTERY_STATUS_UNKNOWN);

            boolean plugged = false;
            switch (status) {
                case BatteryManager.BATTERY_STATUS_CHARGING: 
                case BatteryManager.BATTERY_STATUS_FULL:
                    plugged = true;
                    break;
            }
            /// M: Support "BATTER_PROTECTION" @{
            int status_protection = intent.getIntExtra("status", BatteryManager.BATTERY_STATUS_UNKNOWN);
            Xlog.d(TAG, "status_protection = " + status_protection);
            if (status_protection != BatteryManager.BATTERY_STATUS_DISCHARGING
                    && status_protection != BatteryManager.BATTERY_STATUS_NOT_CHARGING) {
                mBatteryProtection = false;
            } else {
                mBatteryProtection = true;
            }
            final boolean fulled = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) == 100;
            final int icon = (plugged && !fulled && !mBatteryProtection) ? R.drawable.stat_sys_battery_charge 
                                     : R.drawable.stat_sys_battery;
            /// @}
            Xlog.d(TAG,"plugged is " + plugged + " fulled is " + fulled + " mBatteryProtection = "
                    + mBatteryProtection + "  R.drawable.stat_sys_battery_charge is "
                    + R.drawable.stat_sys_battery_charge + " R.drawable.stat_sys_battery is "
                    + R.drawable.stat_sys_battery + "  icon is " + icon);
            int N = mIconViews.size();
            for (int i=0; i<N; i++) {
                ImageView v = mIconViews.get(i);
                v.setImageResource(icon);
                v.setImageLevel(level);
                v.setContentDescription(mContext.getString(R.string.accessibility_battery_level,
                        level));
            }
            N = mLabelViews.size();
            for (int i=0; i<N; i++) {
                TextView v = mLabelViews.get(i);
                v.setText(mContext.getString(R.string.status_bar_settings_battery_meter_format,
                        level));
            }

            for (BatteryStateChangeCallback cb : mChangeCallbacks) {
                cb.onBatteryLevelChanged(level, plugged);
            }

            /// M: Support "battery percentage". @{
            mBatteryPercentage = getBatteryPercentage(intent);
            Xlog.d(TAG,"mBatteryPercentage is " + mBatteryPercentage + " mShouldShowBatteryPercentage is "
                    + mShouldShowBatteryPercentage + " mLabelViews.size() " + mLabelViews.size());
            TextView v = mLabelViews.get(0);
            if (mShouldShowBatteryPercentage) {
                v.setText(mBatteryPercentage);
                v.setVisibility(View.VISIBLE);
            } else {
                v.setVisibility(View.GONE);
            }
            /// M: Support "battery percentage". @}
        }
        /// M: Support "battery percentage". @{
        else if (action.equals(ACTION_BATTERY_PERCENTAGE_SWITCH)) {
            mShouldShowBatteryPercentage = (intent.getIntExtra("state",0) == 1);
            Xlog.d(TAG, " OnReceive from mediatek.intent.ACTION_BATTERY_PERCENTAGE_SWITCH  mShouldShowBatteryPercentage" +
                    " is " + mShouldShowBatteryPercentage);
            TextView v = mLabelViews.get(0);
            if (mShouldShowBatteryPercentage) {
                v.setText(mBatteryPercentage);
                v.setVisibility(View.VISIBLE);
            } else {
                v.setVisibility(View.GONE);
            }
        }
        /// @}
    }
}
