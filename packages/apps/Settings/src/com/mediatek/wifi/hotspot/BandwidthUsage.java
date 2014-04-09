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

package com.mediatek.wifi.hotspot;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.animation.LayoutTransition;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.IConnectivityManager;
import android.net.NetworkStats;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.INetworkManagementService;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.fuelgauge.Utils;
import com.google.android.collect.Lists;
import com.mediatek.widget.ChartBandwidthUsageView;
import com.mediatek.widget.ChartBandwidthUsageView.BandwidthChartListener;
import com.mediatek.xlog.Xlog;

import java.util.ArrayList;

public class BandwidthUsage extends Fragment {
    private static final String TAG = "BandwidthUsage";
    private static final String TAG_LIMIT_EDITOR = "limitEditor";
    private static final String IFACE = "ap0";
    private static final String NETWORK_INFO = "network_info";
    private static final String NETWORK_LIMIT = "network_limit";

    private static final long KB_IN_BYTES = 1024;
    private static final long MB_IN_BYTES = KB_IN_BYTES * 1024;
    private static final long GB_IN_BYTES = MB_IN_BYTES * 1024;
    private static final int LIMIT_MAX_SIZE = 10;
    private static final int EVENT_TICK = 1;

    private ChartBandwidthUsageView mChart;
    private CheckBox mEnableThrottling;
    private View mEnableThrottlingView;
    private View mTotalTimeView;
    private View mTotalDataView;
    private long mStartTime = 0;
    private IntentFilter mIntentFilter;

    private INetworkManagementService mNetworkService;
    private IConnectivityManager mConnManager;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_TICK:
                    updateBandwidthUsage();
                    sendEmptyMessageDelayed(EVENT_TICK, 1000);
                    break;
                default:
                    break;
            }
        }
    };
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(action)) {
                mHandler.removeMessages(EVENT_TICK);
                boolean isAirplaneMode = Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.AIRPLANE_MODE_ON, 0) != 0;
                if (isAirplaneMode) {
                    mEnableThrottlingView.setEnabled(false);
                    getActivity().onBackPressed();
                }
            } else if (WifiManager.WIFI_AP_STATE_CHANGED_ACTION.equals(action)) {
                int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_AP_STATE, 
                    WifiManager.WIFI_AP_STATE_FAILED);
                if (state != WifiManager.WIFI_AP_STATE_ENABLED) {
                    getActivity().finish();
                }
            }
        }
    };
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mNetworkService = INetworkManagementService.Stub.asInterface(
                ServiceManager.getService(Context.NETWORKMANAGEMENT_SERVICE));
        mConnManager = IConnectivityManager.Stub.asInterface(
                ServiceManager.getService(Context.CONNECTIVITY_SERVICE));
        mIntentFilter = new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        mIntentFilter.addAction(WifiManager.WIFI_AP_STATE_CHANGED_ACTION);
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Xlog.i(TAG,"onCreateView");
        final Context context = inflater.getContext();

        View view = inflater.inflate(R.layout.wifi_ap_bandwidth_usage, container, false);

        LinearLayout enableLayout = (LinearLayout) view.findViewById(R.id.enable_throttling);
        mEnableThrottling = new CheckBox(inflater.getContext());
        mEnableThrottling.setClickable(false);
        mEnableThrottling.setFocusable(false);
        mEnableThrottlingView = inflatePreference(inflater, enableLayout, mEnableThrottling);
        mEnableThrottlingView.setClickable(true);
        mEnableThrottlingView.setFocusable(true);
        mEnableThrottlingView.setOnClickListener(mOnEnableCheckBoxClick);
        enableLayout.addView(mEnableThrottlingView);
        setPreferenceTitle(mEnableThrottlingView, R.string.wifi_ap_bandwidth_enable);

        mChart = (ChartBandwidthUsageView) view.findViewById(R.id.chart);
        mChart.setListener(mChartListener);

        LinearLayout timeLayout = (LinearLayout) view.findViewById(R.id.time);
        mTotalTimeView = inflater.inflate(R.layout.preference, timeLayout, false);
        mTotalTimeView.setClickable(false);
        mTotalTimeView.setFocusable(false);
        timeLayout.addView(mTotalTimeView);

        LinearLayout dataLayout = (LinearLayout) view.findViewById(R.id.data);
        mTotalDataView = inflater.inflate(R.layout.preference, dataLayout, false);
        mTotalDataView.setClickable(false);
        mTotalDataView.setFocusable(false);
        dataLayout.addView(mTotalDataView);

        return view;
    }
 
    @Override
    public void onResume() {
        Xlog.d(TAG,"onResume");
        super.onResume();
        getActivity().registerReceiver(mReceiver, mIntentFilter);
        boolean isAirplaneMode = Settings.System.getInt(getActivity().getContentResolver(),
            Settings.System.AIRPLANE_MODE_ON, 0) != 0;
        if (isAirplaneMode) {
            getActivity().onBackPressed();
        }

        mHandler.sendEmptyMessageDelayed(EVENT_TICK, 1000);
        
        boolean enable = Settings.Secure.getInt(getActivity()
                .getContentResolver(), Settings.Secure.INTERFACE_THROTTLE, 0) == 1;
        Xlog.d(TAG,"onResume,getInterfaceRxThrottle=" + enable);
        mEnableThrottling.setChecked(enable);
        mChart.setLimitState(enable);
        
        SharedPreferences pre = 
                getActivity().getSharedPreferences(NETWORK_INFO,getActivity().MODE_WORLD_READABLE);
        long value = pre.getLong(NETWORK_LIMIT, 0L);
        Xlog.d(TAG,"init limit value=" + value);
        mChart.setLimitBytes(value);
        mChart.updateVertAxisBounds(null);

        mStartTime = Settings.System.getLong(getActivity().getContentResolver(),Settings.System.WIFI_HOTSPOT_START_TIME,
                            Settings.System.WIFI_HOTSPOT_DEFAULT_START_TIME);
        Xlog.d(TAG,"mStartTime:" + mStartTime);
        refreshTimeAndData();
    }

    @Override
    public void onPause() {
        Xlog.d(TAG,"onPause");
        super.onPause();
        getActivity().unregisterReceiver(mReceiver);
        mHandler.removeMessages(EVENT_TICK);
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mEnableThrottlingView = null;
    }
    @Override
    public void onDestroy() {
        Xlog.d(TAG,"onDestory");
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        super.onDestroy();
    }

    /**
     * Listener to setup {@link LayoutTransition} after first layout pass.
     */
    private OnGlobalLayoutListener mFirstLayoutListener = new OnGlobalLayoutListener() {
        /** {@inheritDoc} */
        @Override
        public void onGlobalLayout() {

            final LayoutTransition chartTransition = buildLayoutTransition();
            chartTransition.setStartDelay(LayoutTransition.APPEARING, 0);
            chartTransition.setStartDelay(LayoutTransition.DISAPPEARING, 0);
            chartTransition.setAnimator(LayoutTransition.APPEARING, null);
            chartTransition.setAnimator(LayoutTransition.DISAPPEARING, null);
            mChart.setLayoutTransition(chartTransition);
        }
    };

    private BandwidthChartListener mChartListener = new BandwidthChartListener() {

        public void onLimitChanging() {
            mHandler.removeMessages(EVENT_TICK);
        }

        public void onLimitChanged() {
            setLimitData(true);
            mHandler.sendEmptyMessageDelayed(EVENT_TICK, 1000);
        }

        public void requestLimitEdit() {
            //LimitEditorFragment.show(BandwidthUsage.this);
            LimitEditorFragment dialog = new LimitEditorFragment();
            dialog.setTargetFragment(BandwidthUsage.this, 0);
            dialog.show(getFragmentManager(), TAG_LIMIT_EDITOR);     
        }
    };
    private View.OnClickListener mOnEnableCheckBoxClick = new View.OnClickListener() {
        /** {@inheritDoc} */
        @Override
        public void onClick(View v) {
            if (mEnableThrottling.isChecked()) {
                mEnableThrottling.setChecked(false);
                setThrottleEnabled(false);
                mChart.setLimitState(false);
                setLimitData(false);
            } else {
                mEnableThrottling.setChecked(true);
                setThrottleEnabled(true);
                mChart.setLimitState(true);
                SharedPreferences pre = 
                        getActivity().getSharedPreferences(NETWORK_INFO,getActivity().MODE_WORLD_READABLE);
                long value = pre.getLong(NETWORK_LIMIT, 1L);
                Xlog.d(TAG,"init limit value=" + value);
                mChart.setLimitBytes(value);

                setLimitData(true);
            }
        }
    };

    private void setLimitData(boolean enable) {
        try {
            if (enable) {
                long limit = mChart.getLimitBytes();
                int rxBytes = limit == 0 ? 1 : (int) (limit * 8 * 2) / (3 * 1024);
                int txBytes = limit == 0 ? 1 : (int) (limit * 8) / (3 * 1024);
                Xlog.d(TAG,"setLimitData,setInterfaceThrottle,rxBytes=" + rxBytes + ",txBytes=" + txBytes);
                mNetworkService.setInterfaceThrottle(IFACE,rxBytes,txBytes);

                SharedPreferences.Editor editor = 
                        getActivity().getSharedPreferences(NETWORK_INFO,getActivity().MODE_WORLD_WRITEABLE).edit();
                editor.putLong(NETWORK_LIMIT, limit == 0L ? 1L : limit);
                editor.commit();
            } else {
                mNetworkService.setInterfaceThrottle(IFACE, -1, -1);
            }
        } catch (RemoteException e) {
            Xlog.d(TAG, " RemoteException happens when setInterfaceRxThrottle");
        }
    }

    private LayoutTransition buildLayoutTransition() {
        final LayoutTransition transition = new LayoutTransition();

        transition.setAnimateParentHierarchy(false);
        return transition;
    }
    /**
     * Inflate a {@link Preference} style layout, adding the given {@link View}
     * widget into {@link android.R.id#widget_frame}.
     */
    private View inflatePreference(LayoutInflater inflater, ViewGroup root, View widget) {
        final View view = inflater.inflate(R.layout.preference, root, false);
        final LinearLayout widgetFrame = (LinearLayout) view.findViewById(android.R.id.widget_frame);
        widgetFrame.addView(widget, new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
        return view;
    }
    /**
     * Set {@link android.R.id#title} for a preference view inflated with
     * {@link #inflatePreference(LayoutInflater, ViewGroup, View)}.
     */
    private void setPreferenceTitle(View parent, int resId) {
        final TextView title = (TextView) parent.findViewById(android.R.id.title);
        title.setText(resId);
    }

    private void setPreferenceTitle(View parent, int resId, String data) {
        String text = getActivity().getString(resId, data);
        final TextView title = (TextView) parent.findViewById(android.R.id.title);
        title.setText(text);
    }

    private void refreshTimeAndData() {
        //set time information of wifi hotspot 
        long usedTime = 0;

        if (mStartTime != 0) {
            usedTime = System.currentTimeMillis() - mStartTime;
        }
        String time = Utils.formatElapsedTime(getActivity(), usedTime);
        setPreferenceTitle(mTotalTimeView, R.string.wifi_ap_time_duration, " " + time);

        //set used data information of wifi hotspot 
        long totalData = mChart.getTotalUsedData();
        String unit;
        if (totalData < mChart.MB_IN_BYTES) {
            totalData = totalData / mChart.KB_IN_BYTES;
            unit = " KB";
        } else if (totalData < mChart.GB_IN_BYTES) {
            totalData = totalData / mChart.MB_IN_BYTES;
            unit = " M";
        } else {
            totalData = totalData / mChart.GB_IN_BYTES;
            unit = " G";
        }
        setPreferenceTitle(mTotalDataView, R.string.wifi_ap_total_data, " " + String.valueOf(totalData) + unit);
    }
    private void updateBandwidthUsage() {
        try {
            String[] tetheredIfacePairs = mConnManager.getTetheredIfacePairs();

            String[] availableIfacePairs = checkIfacePairs(tetheredIfacePairs);
            
            if (availableIfacePairs != null) {
                NetworkStats networkStats = mNetworkService.getNetworkStatsTethering(availableIfacePairs);
                mChart.setNetworkStates(networkStats);
            }
            refreshTimeAndData();
        } catch (RemoteException e) {
            Xlog.d(TAG, "RemoteException happens");
        }

    }

    private String[] checkIfacePairs(String[] pairs) {
        final ArrayList<String> list = Lists.newArrayList();
        
        if (pairs.length < 2) {
            return null;
        } else {
            for (int i = 0 ; i < pairs.length ; i += 2) {
                if (pairs[i] != null && !pairs[i].equals(pairs[i + 1])) {
                    list.add(pairs[i]);
                    list.add(pairs[i + 1]);
                }
            }
        }
        return list.toArray(new String[list.size()]);
    }

    /**
     * Dialog to edit {@link NetworkPolicy#limitBytes}.
     */
    public class LimitEditorFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();
            final BandwidthUsage target = (BandwidthUsage) getTargetFragment();


            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            final LayoutInflater dialogInflater = LayoutInflater.from(builder.getContext());

            final View view = dialogInflater.inflate(R.layout.data_usage_bytes_editor, null, false);
            final NumberPicker bytesPicker = (NumberPicker) view.findViewById(R.id.bytes);
            long limitBytes = mChart.getLimitBytes();
            bytesPicker.setMaxValue(LIMIT_MAX_SIZE);
            bytesPicker.setMinValue(0);

            bytesPicker.setValue((int) (limitBytes / MB_IN_BYTES));
            bytesPicker.setWrapSelectorWheel(false);

            builder.setTitle(R.string.data_usage_limit_editor_title);
            builder.setView(view);
            builder.setPositiveButton(R.string.data_usage_cycle_editor_positive,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // clear focus to finish pending text edits
                            bytesPicker.clearFocus();
                            final long bytes = bytesPicker.getValue() * MB_IN_BYTES;
                            mChart.setLimitBytes(bytes);
                            Xlog.d(TAG,"set Limit Bytes=" + bytes);
                            mChart.updateVertAxisBounds(null);
                            setLimitData(true);
                        }
                    });

            return builder.create();
        }
    }
    
    private void setThrottleEnabled(boolean enable) {
        Xlog.d(TAG, "setThrottleEnabled:" + enable);
        int value = (enable ? 1 : 0);
        Settings.Secure.putInt(getActivity().getContentResolver(), Settings.Secure.INTERFACE_THROTTLE, value);           
     }
}


