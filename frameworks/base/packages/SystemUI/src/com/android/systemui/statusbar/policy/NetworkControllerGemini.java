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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wimax.WimaxManagerConstants;
import android.os.Binder;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.provider.Telephony;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.view.Gravity;
import android.view.IWindowManager;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.internal.app.IBatteryStats;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.TelephonyProperties;
import com.android.internal.telephony.cdma.EriInfo;
import com.android.internal.util.AsyncChannel;
import com.android.server.am.BatteryStatsService;
import com.android.systemui.R;
import com.android.systemui.statusbar.phone.CarrierLabelGemini;
import com.android.systemui.statusbar.util.SIMHelper;
import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.systemui.ext.DataType;
import com.mediatek.systemui.ext.IconIdWrapper;
import com.mediatek.systemui.ext.NetworkType;
import com.mediatek.systemui.ext.PluginFactory;
import com.mediatek.telephony.TelephonyManagerEx;
import com.mediatek.xlog.Xlog;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

/// M: [SystemUI] Support "Dual SIM".
public class NetworkControllerGemini extends BroadcastReceiver {
    // debug
    static final String TAG = "NetworkControllerGemini";
    static final boolean DEBUG = false;
    static final boolean CHATTY = false; // additional diagnostics, but not logspew
    
    private static final String ACTION_BOOT_IPO = "android.intent.action.ACTION_PREBOOT_IPO";
    private boolean[] mIsRoaming;
    private int[] mIsRoamingId;
    
    // telephony
    private boolean mHspaDataDistinguishable;  
    private final TelephonyManagerEx mPhone;
    private boolean[] mDataConnected;
    private int[] mDataNetType;
    private int[] mDataState;
    private int[] mDataActivity;
    private ServiceState[] mServiceState;
    private SignalStrength[] mSignalStrength;
    private IconIdWrapper[][] mDataIconList;
    private String[] mNetworkName;
    private String mNetworkNameDefault;
    private String mNetworkNameSeparator;
    private IconIdWrapper[][] mPhoneSignalIconId;
    private int[] mDataDirectionIconId; // data + data direction on phones
    private IconIdWrapper[] mDataSignalIconId;
    private IconIdWrapper[] mDataTypeIconId;
    private boolean mDataActive;
    private IconIdWrapper[] mMobileActivityIconId; // overlay arrows for data direction
    private boolean mShowAtLeastThreeGees = false;
    private boolean mAlwaysShowCdmaRssi = false;
    private int mGeminiSimNum;
    private String[] mContentDescriptionPhoneSignal;
    private String mContentDescriptionWifi;
    private String mContentDescriptionWimax;
    private String[] mContentDescriptionDataType;
    private NetworkType[] mDataNetType3G;
    private boolean[] mSimIndicatorFlag;
    private int[] mSimIndicatorResId;
    private int mDataIconListNum = 4;
    private int mPhoneSignalIconIdNum = 2;
    private myPhoneStateListener[] mPhoneStateListener; 
    private IccCardConstants.State[] mSimState;

    // wifi
    private final WifiManager mWifiManager;
    private AsyncChannel mWifiChannel;
    private boolean mWifiEnabled;
    private boolean mWifiConnected;
    private int mWifiRssi;
    private int mWifiLevel;
    private String mWifiSsid;
    private int mWifiIconId = 0;
    private int mWifiActivityIconId = 0; // overlay arrows for wifi direction
    private int mWifiActivity = WifiManager.DATA_ACTIVITY_NONE;

    // bluetooth
    private boolean mBluetoothTethered = false;
    private int mBluetoothTetherIconId =
        com.android.internal.R.drawable.stat_sys_tether_bluetooth;

    //wimax
    private boolean mWimaxSupported = false;
    private boolean mIsWimaxEnabled = false;
    private boolean mWimaxConnected = false;
    private boolean mWimaxIdle = false;
    private int mWimaxIconId = 0;
    private int mWimaxSignal = 0;
    private int mWimaxState = 0;
    private int mWimaxExtraState = 0;
    // data connectivity (regardless of state, can we access the internet?)
    // state of inet connection - 0 not connected, 100 connected
    private boolean mConnected = false;
    private int mConnectedNetworkType = ConnectivityManager.TYPE_NONE;
    private String mConnectedNetworkTypeName;
    private int mInetCondition = 0;
    private static final int INET_CONDITION_THRESHOLD = 50;

    private boolean mAirplaneMode = false;
    private boolean mLastAirplaneMode = false;

    // our ui
    private Context mContext;
    private ArrayList<ImageView> mPhoneSignalIconViews = new ArrayList<ImageView>();
    private ArrayList<ImageView> mDataDirectionIconViews = new ArrayList<ImageView>();
    private ArrayList<ImageView> mDataDirectionOverlayIconViews = new ArrayList<ImageView>();
    private ArrayList<ImageView> mWifiIconViews = new ArrayList<ImageView>();
    private ArrayList<ImageView> mWimaxIconViews = new ArrayList<ImageView>();
    private ArrayList<ImageView> mCombinedSignalIconViews = new ArrayList<ImageView>();
    private ArrayList<ImageView> mDataTypeIconViews = new ArrayList<ImageView>();
    private ArrayList<TextView> mCombinedLabelViews = new ArrayList<TextView>();
    private ArrayList<TextView> mMobileLabelViews = new ArrayList<TextView>();
    private ArrayList<TextView> mWifiLabelViews = new ArrayList<TextView>();
    private ArrayList<TextView> mEmergencyLabelViews = new ArrayList<TextView>();
    private ArrayList<SignalCluster> mSignalClusters = new ArrayList<SignalCluster>();
    private int[][] mLastPhoneSignalIconId;
    private int mLastDataDirectionIconId = -1;
    private int mLastDataDirectionOverlayIconId = -1;
    private int mLastWifiIconId = -1;
    private int mLastWimaxIconId = -1;
    private int mLastCombinedSignalIconId = -1;
    private int[] mLastDataTypeIconId;
    private int[] mLastMobileActivityIconId;
    private String mLastCombinedLabel = "";

    private boolean mHasMobileDataFeature;
    private boolean mDataAndWifiStacked = false;
    private boolean mIsScreenLarge = false;

    // yuck -- stop doing this here and put it in the framework
    private IBatteryStats mBatteryStats;
    
    // whether the SIMs initialization of framework is ready.
    private boolean mSimCardReady = false;

    public interface SignalCluster {
        void setWifiIndicators(boolean visible, int strengthIcon, int activityIcon, 
                String contentDescription);
        void setMobileDataIndicators(int slotId, boolean visible, IconIdWrapper []strengthIcon, IconIdWrapper activityIcon,
                IconIdWrapper typeIcon, String contentDescription, String typeContentDescription);
        void setIsAirplaneMode(boolean is);
        void setDataNetType3G(int slotId, NetworkType dataNetType3G);
        void setRoamingFlagandResource(boolean[] roaming, int[] roamingId);
        void setShowSimIndicator(int slotId, boolean showSimIndicator,int resId);
        void apply();
    }
    
    /**
     * Construct this controller object and register for updates.
     */
    public NetworkControllerGemini(Context context) {
        mContext = context;
        final Resources res = context.getResources();

        ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        mHasMobileDataFeature = cm.isNetworkSupported(ConnectivityManager.TYPE_MOBILE);
        mShowAtLeastThreeGees = res.getBoolean(R.bool.config_showMin3G);
        Xlog.d(TAG, "NetworkControllerGemini, mShowAtLeastThreeGees=" + mShowAtLeastThreeGees);

        IWindowManager wm = IWindowManager.Stub.asInterface(
                    ServiceManager.getService(Context.WINDOW_SERVICE));
        try {
           if (wm.hasSystemNavBar()) {
               mIsScreenLarge = true;
           } else {
               mIsScreenLarge = false;
           }
        } catch (RemoteException e) {
           Xlog.w(TAG, "Failing checking whether status bar is visible");
        }

        mAlwaysShowCdmaRssi = res.getBoolean(
                com.android.internal.R.bool.config_alwaysUseCdmaRssi);

        // set up the default wifi icon, used when no radios have ever appeared
        updateWifiIcons();
        updateWimaxIcons();

        // telephony
        mPhone = SIMHelper.getDefault(context);
        mGeminiSimNum = PhoneConstants.GEMINI_SIM_NUM;
        mNetworkNameSeparator = mContext.getString(R.string.status_bar_network_name_separator);
        mNetworkNameDefault = mContext.getString(com.android.internal.R.string.lockscreen_carrier_default);
        
        mIsRoaming = new boolean[mGeminiSimNum]; 
        mIsRoamingId = new int[mGeminiSimNum];  
        mSignalStrength = new SignalStrength[mGeminiSimNum];
        mServiceState = new ServiceState[mGeminiSimNum];
        mDataNetType = new int[mGeminiSimNum];
        mDataState = new int[mGeminiSimNum];
        mDataConnected = new boolean[mGeminiSimNum];
        mSimState = new IccCardConstants.State[mGeminiSimNum];
        mDataDirectionIconId = new int[mGeminiSimNum];
        mDataActivity = new int[mGeminiSimNum];
        mDataNetType3G = new NetworkType[mGeminiSimNum];
        mContentDescriptionPhoneSignal = new String[mGeminiSimNum];
        mDataSignalIconId = new IconIdWrapper[mGeminiSimNum];
        mContentDescriptionDataType = new String[mGeminiSimNum];
        mNetworkName = new String[mGeminiSimNum];
        mPhoneSignalIconId = new IconIdWrapper[mGeminiSimNum][mPhoneSignalIconIdNum];
        mDataTypeIconId = new IconIdWrapper[mGeminiSimNum];
        mMobileActivityIconId = new IconIdWrapper[mGeminiSimNum];
        mDataIconList = new IconIdWrapper[mGeminiSimNum][mDataIconListNum];
        mLastPhoneSignalIconId = new int[mGeminiSimNum][];
        mLastDataTypeIconId = new int[mGeminiSimNum];
        mLastMobileActivityIconId = new int[mGeminiSimNum];
        mSimIndicatorFlag = new boolean[mGeminiSimNum];
        mSimIndicatorResId = new int[mGeminiSimNum];
        mPhoneStateListener = new myPhoneStateListener[mGeminiSimNum];
        
        mSimCardReady = SystemProperties.getBoolean(TelephonyProperties.PROPERTY_SIM_INFO_READY, false);
        int[] iconList = PluginFactory.getStatusBarPlugin(mContext).getDataTypeIconListGemini(false, DataType.Type_G);
        
        // for initialization
        for (int i = 0 ; i < mGeminiSimNum ; i++) {
            mDataNetType[i] = TelephonyManager.NETWORK_TYPE_UNKNOWN;
            mDataState[i] = TelephonyManager.DATA_DISCONNECTED;
            mSimState[i] = IccCardConstants.State.READY;
            mDataActivity[i] = TelephonyManager.DATA_ACTIVITY_NONE;
            mNetworkName[i] = mNetworkNameDefault;
            mLastPhoneSignalIconId[i] = new int[]{-1,-1};
            mLastDataTypeIconId[i] = -1;
            mLastMobileActivityIconId[i] = -1;
            mPhoneStateListener[i] = new myPhoneStateListener();
            mMobileActivityIconId[i] = new IconIdWrapper();
            mDataTypeIconId[i] = new IconIdWrapper(0);
            mDataSignalIconId[i] = new IconIdWrapper(0);
            
            for(int j = 0; j < mDataIconListNum ; j++) {

                mDataIconList[i][j] = new IconIdWrapper(0);
                if (iconList != null) {
                    mDataIconList[i][j].setResources(PluginFactory.getStatusBarPlugin(mContext).getPluginResources());
                    mDataIconList[i][j].setIconId(iconList[j]);
                } else {
                    mDataIconList[i][j].setResources(null);
                    mDataIconList[i][j].setIconId(TelephonyIconsGemini.DATA_G[j]);
                }
            }
            for (int j = 0; j < mPhoneSignalIconIdNum ; j++) {
                mPhoneSignalIconId[i][j] = new IconIdWrapper(0);
            }

            int resId = PluginFactory.getStatusBarPlugin(mContext).getSignalStrengthNullIconGemini(i);
            if (resId != -1) {
                mPhoneSignalIconId[i][0].setResources(PluginFactory.getStatusBarPlugin(mContext).getPluginResources());
                mPhoneSignalIconId[i][0].setIconId(resId);
            } else {
                mPhoneSignalIconId[i][0].setResources(null);
                mPhoneSignalIconId[i][0].setIconId(R.drawable.stat_sys_gemini_signal_null);
            }

            SIMHelper.listen(mPhoneStateListener[i],
                PhoneStateListener.LISTEN_SERVICE_STATE
              | PhoneStateListener.LISTEN_SIGNAL_STRENGTHS
              | PhoneStateListener.LISTEN_CALL_STATE
              | PhoneStateListener.LISTEN_DATA_CONNECTION_STATE
              | PhoneStateListener.LISTEN_DATA_ACTIVITY,
              i);
        }
        
        SIMHelper.updateSimInsertedStatus();
                
        mHspaDataDistinguishable = mContext.getResources().getBoolean(R.bool.config_hspa_data_distinguishable)
                && PluginFactory.getStatusBarPlugin(mContext).isHspaDataDistinguishable();

        // wifi
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        Handler handler = new WifiHandler();
        mWifiChannel = new AsyncChannel();
        Messenger wifiMessenger = mWifiManager.getWifiServiceMessenger();
        if (wifiMessenger != null) {
            mWifiChannel.connect(mContext, handler, wifiMessenger);
        }

        // broadcasts
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        filter.addAction(Telephony.Intents.SPN_STRINGS_UPDATED_ACTION);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(ConnectivityManager.INET_CONDITION_ACTION);
        filter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
        filter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        filter.addAction(ACTION_BOOT_IPO);
        mWimaxSupported = mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_wimaxEnabled);
        if (mWimaxSupported) {
            filter.addAction(WimaxManagerConstants.WIMAX_NETWORK_STATE_CHANGED_ACTION);
            filter.addAction(WimaxManagerConstants.SIGNAL_LEVEL_CHANGED_ACTION);
            filter.addAction(WimaxManagerConstants.NET_4G_STATE_CHANGED_ACTION);
        }
        filter.addAction(Intent.SIM_SETTINGS_INFO_CHANGED);
        filter.addAction(TelephonyIntents.ACTION_SIM_INDICATOR_STATE_CHANGED);
        filter.addAction(TelephonyIntents.ACTION_SIM_INSERTED_STATUS);
        filter.addAction(TelephonyIntents.ACTION_SIM_INFO_UPDATE);
        filter.addAction("android.intent.action.ACTION_SHUTDOWN_IPO");
        context.registerReceiver(this, filter);

        // AIRPLANE_MODE_CHANGED is sent at boot; we've probably already missed it
        updateAirplaneMode();

        // yuck
        mBatteryStats = BatteryStatsService.getService();
    }
    
    public boolean hasMobileDataFeature() {
        return mHasMobileDataFeature;
    }

    public boolean isEmergencyOnly() {
        return (mServiceState[PhoneConstants.GEMINI_SIM_1] != null && mServiceState[PhoneConstants.GEMINI_SIM_1].isEmergencyOnly());
    }

    public void addPhoneSignalIconView(ImageView v) {
        mPhoneSignalIconViews.add(v);
    }

    public void addDataDirectionIconView(ImageView v) {
        mDataDirectionIconViews.add(v);
    }

    public void addDataDirectionOverlayIconView(ImageView v) {
        mDataDirectionOverlayIconViews.add(v);
    }

    public void addWifiIconView(ImageView v) {
        mWifiIconViews.add(v);
    }

    public void addWimaxIconView(ImageView v) {
        mWimaxIconViews.add(v);
    }

    public void addCombinedSignalIconView(ImageView v) {
        mCombinedSignalIconViews.add(v);
    }

    public void addDataTypeIconView(ImageView v) {
        mDataTypeIconViews.add(v);
    }

    public void addCombinedLabelView(TextView v) {
        mCombinedLabelViews.add(v);
    }

    public void addMobileLabelView(TextView v) {
        mMobileLabelViews.add(v);
    }

    public void addWifiLabelView(TextView v) {
        mWifiLabelViews.add(v);
    }

    public void addEmergencyLabelView(TextView v) {
        mEmergencyLabelViews.add(v);
    }

    public void addSignalCluster(SignalCluster cluster) {
        mSignalClusters.add(cluster);
        refreshSignalCluster(cluster);
    }

    public void refreshSignalCluster(SignalCluster cluster) {
        cluster.setRoamingFlagandResource(mIsRoaming, mIsRoamingId);
        cluster.setWifiIndicators(
                mWifiEnabled && (mWifiConnected || !mHasMobileDataFeature), // only show wifi in the cluster if connected
                mWifiIconId,
                mWifiActivityIconId,
                mContentDescriptionWifi);
        if (mIsWimaxEnabled && mWimaxConnected) {
            // wimax is special
            cluster.setMobileDataIndicators(
                    PhoneConstants.GEMINI_SIM_1,
                    true,
                    mAlwaysShowCdmaRssi ? mPhoneSignalIconId[PhoneConstants.GEMINI_SIM_1] :
                        new IconIdWrapper[]{new IconIdWrapper(mWimaxIconId),new IconIdWrapper()}, mMobileActivityIconId[PhoneConstants.GEMINI_SIM_1],
                    mDataTypeIconId[PhoneConstants.GEMINI_SIM_1],
                    mContentDescriptionWimax,
                    mContentDescriptionDataType[PhoneConstants.GEMINI_SIM_1]);
        } else {
            // normal mobile data
            for (int i = 0; i < mGeminiSimNum ; i++) {
                cluster.setMobileDataIndicators(
                    i,
                    mHasMobileDataFeature,
                    mPhoneSignalIconId[i],
                    mMobileActivityIconId[i],
                    mDataTypeIconId[i],
                    mContentDescriptionPhoneSignal[i],
                    mContentDescriptionDataType[i]);
            }
        }
        cluster.setIsAirplaneMode(mAirplaneMode);
        mLastAirplaneMode = mAirplaneMode;
        cluster.apply();
    }

    public void setStackedMode(boolean stacked) {
        mDataAndWifiStacked = true;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        Xlog.d(TAG, "onReceive, intent action is " + action);
        if (action.equals(WifiManager.RSSI_CHANGED_ACTION)
                || action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)
                || action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
            updateWifiState(intent);
            refreshViews();
        } else if (action.equals(TelephonyIntents.ACTION_SIM_STATE_CHANGED)) {
            int slotId = intent.getIntExtra(PhoneConstants.GEMINI_SIM_ID_KEY, PhoneConstants.GEMINI_SIM_1);
            SIMHelper.updateSimInsertedStatus();
            updateTelephonySignalStrength();
            updateDataNetType(slotId);
            updateSimState(slotId, intent);
            updateDataIcon(slotId);
            refreshViews(slotId);
        } else if (action.equals(Telephony.Intents.SPN_STRINGS_UPDATED_ACTION)) {
            int slotId = intent.getIntExtra(PhoneConstants.GEMINI_SIM_ID_KEY, PhoneConstants.GEMINI_SIM_1);
            updateNetworkName(slotId,
                    intent.getBooleanExtra(Telephony.Intents.EXTRA_SHOW_SPN, false),
                    intent.getStringExtra(Telephony.Intents.EXTRA_SPN),
                    intent.getBooleanExtra(Telephony.Intents.EXTRA_SHOW_PLMN, false),
                    intent.getStringExtra(Telephony.Intents.EXTRA_PLMN));
            refreshViews(slotId);
        } else if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION) ||
                 action.equals(ConnectivityManager.INET_CONDITION_ACTION)) {
            if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                NetworkInfo info = (NetworkInfo) intent.getExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
                if (info == null) {
                    Xlog.d(TAG,"onReceive, ConnectivityManager.CONNECTIVITY_ACTION networkinfo is null.");
                    return;
                }
                int type = info.getType();
                Xlog.d(TAG,"onReceive, ConnectivityManager.CONNECTIVITY_ACTION network type is " + type);
                if (type != ConnectivityManager.TYPE_NONE && type != ConnectivityManager.TYPE_MOBILE 
                        && type != ConnectivityManager.TYPE_BLUETOOTH && type != ConnectivityManager.TYPE_WIFI
                        && type != ConnectivityManager.TYPE_ETHERNET) {
                    return;
                }
            }
            updateConnectivity(intent);
            updateOperatorInfo();
            refreshViews();
        } else if (action.equals(Intent.ACTION_CONFIGURATION_CHANGED)) {
            refreshViews();
        } else if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED) || action.equals(ACTION_BOOT_IPO)) {
            updateAirplaneMode();
            refreshViews();
        } else if (action.equals(WimaxManagerConstants.NET_4G_STATE_CHANGED_ACTION) ||
            action.equals(WimaxManagerConstants.SIGNAL_LEVEL_CHANGED_ACTION) ||
            action.equals(WimaxManagerConstants.WIMAX_NETWORK_STATE_CHANGED_ACTION)) {
            updateWimaxState(intent);
            refreshViews();
        } else if (action.equals(Intent.SIM_SETTINGS_INFO_CHANGED)) {
            SIMHelper.updateSIMInfos(context);
            int type = intent.getIntExtra("type", -1);
            long simId = intent.getLongExtra("simid", -1);
            if (type == 1) {
                // color changed
                updateDataNetType();
                updateTelephonySignalStrength();
                updateOperatorInfo();
            }
            refreshViews();
        } else if (action.equals(TelephonyIntents.ACTION_SIM_INDICATOR_STATE_CHANGED)) {
            int slotId = intent.getIntExtra(TelephonyIntents.INTENT_KEY_ICC_SLOT, -1);
            updateDataNetType(slotId);
            updateTelephonySignalStrength(slotId);
            updateOperatorInfo();
            refreshViews();
        } else if (action.equals(TelephonyIntents.ACTION_SIM_INSERTED_STATUS)) {
            SIMHelper.updateSIMInfos(context);
            updateDataNetType();
            updateTelephonySignalStrength();
            updateOperatorInfo();
            refreshViews();
        } else if (action.equals(TelephonyIntents.ACTION_SIM_INFO_UPDATE)) {
            Xlog.d(TAG, "onReceive from TelephonyIntents.ACTION_SIM_INFO_UPDATE");
            mSimCardReady = true;
            SIMHelper.updateSIMInfos(context);
            updateDataNetType();
            updateTelephonySignalStrength();
            updateOperatorInfo();
            refreshViews();
        } else if (action.equals("android.intent.action.ACTION_SHUTDOWN_IPO")) {
            Xlog.d(TAG, "onReceive from android.intent.action.ACTION_SHUTDOWN_IPO");
            mSimCardReady = false;
        }
    }

    // ===== Telephony ==============================================================

    private class myPhoneStateListener extends PhoneStateListener {
        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            int slotId = Arrays.asList(mPhoneStateListener).indexOf(this);
            Xlog.d(TAG, "PhoneStateListener:onSignalStrengthsChanged, sim"+slotId+" before.");
            Xlog.d(TAG, "PhoneStateListener:onSignalStrengthsChanged, signalStrength=" + signalStrength.getLevel());
            mSignalStrength[slotId] = signalStrength;
            updateDataNetType(slotId);
            updateTelephonySignalStrength(slotId);
            refreshViews(slotId);
            Xlog.d(TAG, "PhoneStateListener:onSignalStrengthsChanged, sim"+slotId+" after.");
        }

        @Override
        public void onServiceStateChanged(ServiceState state) {
            int slotId = Arrays.asList(mPhoneStateListener).indexOf(this);
            Xlog.d(TAG, "PhoneStateListener:onServiceStateChanged, sim"+slotId+" before.");
            Xlog.d(TAG, "PhoneStateListener:onServiceStateChanged, state=" + state.getState());
            mServiceState[slotId] = state;
            //BEGIN [20120301][ALPS00245624]
            TelephonyManager telephonyManager = TelephonyManager.getDefault();
            mDataNetType[slotId] = telephonyManager.getNetworkTypeGemini(slotId);
            Xlog.d(TAG,"PhoneStateListener:onServiceStateChanged sim"+slotId+" mDataNetType: "+ mDataNetType[slotId]);
            //END   [20120301][ALPS00245624]
            updateDataNetType(slotId);
            updateTelephonySignalStrength(slotId);
            updateDataIcon(slotId);
            refreshViews(slotId);
            Xlog.d(TAG, "PhoneStateListener:onServiceStateChanged, sim"+slotId+" after.");
        }

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            int slotId = Arrays.asList(mPhoneStateListener).indexOf(this);
            Xlog.d(TAG, "PhoneStateListener:onCallStateChanged, sim"+slotId+" before.");
            Xlog.d(TAG, "PhoneStateListener:onCallStateChanged, state=" + state);
            // In cdma, if a voice call is made, RSSI should switch to 1x.
            if (isCdma(slotId)) {
                updateDataNetType(slotId);
                updateTelephonySignalStrength(slotId);
                refreshViews(slotId);
            }
            if (FeatureOption.MTK_DT_SUPPORT) {
                updateDataNetType(slotId);
                updateDataIcon(slotId);
                refreshViews(slotId);
            } else {
                updateDataIcon();
                updateDataNetType();
                refreshViews();
            }
            Xlog.d(TAG, "PhoneStateListener:onCallStateChanged, sim1 after.");
        }

        @Override
        public void onDataConnectionStateChanged(int state, int networkType) {
            int slotId = Arrays.asList(mPhoneStateListener).indexOf(this);
            Xlog.d(TAG, "PhoneStateListener:onDataConnectionStateChanged, sim"+slotId+" before.");
            Xlog.d(TAG, "PhoneStateListener:onDataConnectionStateChanged, state=" + state + " type=" + networkType);
            mDataState[slotId] = state;
            mDataNetType[slotId] = networkType;
            updateDataNetType(slotId);
            updateDataIcon(slotId);
            refreshViews(slotId);
            Xlog.d(TAG, "PhoneStateListener:onDataConnectionStateChanged, sim"+slotId+" after.");
        }

        @Override
        public void onDataActivity(int direction) {
            int slotId = Arrays.asList(mPhoneStateListener).indexOf(this);
            Xlog.d(TAG, "PhoneStateListener:onDataActivity, sim"+slotId+" before.");
            Xlog.d(TAG, "PhoneStateListener:onDataActivity, direction=" + direction);
            mDataActivity[slotId] = direction;
            updateDataIcon(slotId);
            refreshViews(slotId);
            Xlog.d(TAG, "PhoneStateListener:onDataActivity, sim"+slotId+" after.");
        }
    }


    private final void updateSimState(int slotId, Intent intent) {
        IccCardConstants.State tempSimState = null;

        String stateExtra = intent.getStringExtra(IccCardConstants.INTENT_KEY_ICC_STATE);
        if (IccCardConstants.INTENT_VALUE_ICC_ABSENT.equals(stateExtra)) {
            tempSimState = IccCardConstants.State.ABSENT;
        } else if (IccCardConstants.INTENT_VALUE_ICC_READY.equals(stateExtra)) {
            tempSimState = IccCardConstants.State.READY;
        } else if (IccCardConstants.INTENT_VALUE_ICC_LOCKED.equals(stateExtra)) {
            final String lockedReason = intent.getStringExtra(IccCardConstants.INTENT_KEY_LOCKED_REASON);
            if (IccCardConstants.INTENT_VALUE_LOCKED_ON_PIN.equals(lockedReason)) {
                tempSimState = IccCardConstants.State.PIN_REQUIRED;
            } else if (IccCardConstants.INTENT_VALUE_LOCKED_ON_PUK.equals(lockedReason)) {
                tempSimState = IccCardConstants.State.PUK_REQUIRED;
            } else {
                tempSimState = IccCardConstants.State.NETWORK_LOCKED;
            }
        } else {
            tempSimState = IccCardConstants.State.UNKNOWN;
        }

        if (tempSimState != null) {
            mSimState[slotId] = tempSimState;
        }
    }

    private boolean isCdma(int slotId) {
        SignalStrength tempSignalStrength = mSignalStrength[slotId];

        return (tempSignalStrength != null) && !tempSignalStrength.isGsm();
    }

    private boolean hasService(int slotId) {
        ServiceState tempServiceState;
        tempServiceState = mServiceState[slotId];
        
        if (tempServiceState != null) {
            switch (tempServiceState.getState()) {
                case ServiceState.STATE_OUT_OF_SERVICE:
                case ServiceState.STATE_POWER_OFF:
                    return false;
                default:
                    return true;
            }
        } else {
            return false;
        }
    }

    private void updateAirplaneMode() {
        mAirplaneMode = (Settings.System.getInt(mContext.getContentResolver(),
            Settings.Global.AIRPLANE_MODE_ON, 0) == 1);
    }

    private final void updateTelephonySignalStrength() {
        for (int i=0 ; i < mGeminiSimNum ; i++) {
            updateTelephonySignalStrength(i);
        }    	  
    }

    private final void updateTelephonySignalStrength(int slotId) {
        boolean handled = false;

        boolean tempSIMCUSignVisible = true;
        IconIdWrapper tempPhoneSignalIconId[] = {new IconIdWrapper(), new IconIdWrapper()};
        IconIdWrapper tempDataSignalIconId = new IconIdWrapper();
        ServiceState tempServiceState = null;
        SignalStrength tempSignalStrength = null;
        String tempContentDescriptionPhoneSignal = "";
        int tempLastSignalLevel[] = {-1,-1};

        tempSignalStrength = mSignalStrength[slotId];
        tempServiceState = mServiceState[slotId];
        
        // null signal state
        if (!handled && !SIMHelper.isSimInserted(slotId)) {
            Xlog.d(TAG, "updateTelephonySignalStrength(" + slotId + "), is null signal.");
            int resId = PluginFactory.getStatusBarPlugin(mContext).getSignalStrengthNullIconGemini(slotId);
            if (resId != -1) {
                tempPhoneSignalIconId[0].setResources(PluginFactory.getStatusBarPlugin(mContext).getPluginResources());
                tempPhoneSignalIconId[0].setIconId(resId);
                tempSIMCUSignVisible = false;
            } else {
                tempPhoneSignalIconId[0].setResources(null);
                tempPhoneSignalIconId[0].setIconId(R.drawable.stat_sys_gemini_signal_null);
            }
            
            handled = true;
            Xlog.d(TAG, "updateTelephonySignalStrength(" + slotId + "), null signal");
        }
       
        if (!mSimCardReady) {
            Xlog.d(TAG, "updateTelephonySignalStrength(" + slotId + 
                    "), the SIMs initialization of framework has not been ready.");
            handled = true;
        }
       

        // searching state
        if (!handled && tempServiceState != null) {
            int regState = tempServiceState.getRegState();
            Xlog.d(TAG, "updateTelephonySignalStrength(" + slotId + "), regState=" + regState);
            if (regState == ServiceState.REGISTRATION_STATE_NOT_REGISTERED_AND_SEARCHING) {
                Xlog.d(TAG, " searching state hasService= " + hasService(slotId));
                int resId = PluginFactory.getStatusBarPlugin(mContext).getSignalStrengthSearchingIconGemini(slotId);
                if (resId != -1) {
                    tempPhoneSignalIconId[0].setResources(PluginFactory.getStatusBarPlugin(mContext).getPluginResources());
                    tempPhoneSignalIconId[0].setIconId(resId);
                } else {
                    tempPhoneSignalIconId[0].setResources(null);
                    tempPhoneSignalIconId[0].setIconId(R.drawable.stat_sys_gemini_signal_searching);
                }
                handled = true;
                Xlog.d(TAG, "updateTelephonySignalStrength(" + slotId + "), searching");
            }
        }
        // check radio_off model
        if (!handled  && (tempServiceState == null
                || (!hasService(slotId) && !tempServiceState.isEmergencyOnly()))) {
                Xlog.d(TAG, "updateTelephonySignalStrength(" + slotId + ") tempServiceState = " + tempServiceState);
            if (SIMHelper.isSimInserted(slotId)) {
                Xlog.d(TAG, "SimIndicatorState = " + SIMHelper.getSimIndicatorStateGemini(slotId));
                if (PhoneConstants.SIM_INDICATOR_RADIOOFF == SIMHelper.getSimIndicatorStateGemini(slotId)) {
                    tempSIMCUSignVisible = true;
                    tempPhoneSignalIconId[0].setResources(null);
                    tempPhoneSignalIconId[0].setIconId(R.drawable.stat_sys_gemini_radio_off);
                    tempDataSignalIconId.setResources(null);
                    tempDataSignalIconId.setIconId(R.drawable.stat_sys_gemini_radio_off);
                    handled = true;
                }
            }
        }
        // signal level state
        if (!handled) {
            boolean hasService = hasService(slotId);
            Xlog.d(TAG, "updateTelephonySignalStrength(" + slotId + "), hasService=" + hasService);
            if (!hasService) {
                if (CHATTY) {
                    Xlog.d(TAG, "updateTelephonySignalStrength: !hasService()");
                }
                int resId = PluginFactory.getStatusBarPlugin(mContext).getSignalStrengthNullIconGemini(slotId);
                if (resId != -1) {
                    tempPhoneSignalIconId[0].setResources(PluginFactory.getStatusBarPlugin(mContext).getPluginResources());
                    tempPhoneSignalIconId[0].setIconId(resId);
                    tempSIMCUSignVisible = false;
                } else {
                    tempPhoneSignalIconId[0].setResources(null);
                    tempPhoneSignalIconId[0].setIconId(R.drawable.stat_sys_gemini_signal_null);
                    tempDataSignalIconId.setResources(null);
                    tempDataSignalIconId.setIconId(R.drawable.stat_sys_gemini_signal_0);
                }
            } else {
                if (tempSignalStrength == null) {
                    if (CHATTY) {
                        Xlog.d(TAG, "updateTelephonySignalStrength: mSignalStrength == null");
                    }
                    tempPhoneSignalIconId[0].setResources(null);
                    tempPhoneSignalIconId[0].setIconId(R.drawable.stat_sys_gemini_signal_0);
                    tempDataSignalIconId.setResources(null);
                    tempDataSignalIconId.setIconId(R.drawable.stat_sys_gemini_signal_0);
                    tempContentDescriptionPhoneSignal = mContext
                            .getString(AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH[0]);
                } else {
                    int iconLevel[] = { 0, 0 };
                    int[][] iconList = {{},{}};
                    if (isCdma(slotId) && mAlwaysShowCdmaRssi) {
                        tempLastSignalLevel[0] = iconLevel[0] = tempSignalStrength.getCdmaLevel();
                        Xlog.d(TAG, "mAlwaysShowCdmaRssi=" + mAlwaysShowCdmaRssi
                                + " set to cdmaLevel=" + mSignalStrength[0].getCdmaLevel()
                                + " instead of level=" + mSignalStrength[0].getLevel());
                    } else {
                        tempLastSignalLevel[0] = iconLevel[0] = tempSignalStrength.getLevel();
                    }
                    NetworkType tempDataNetType = mDataNetType3G[slotId];

                    if (tempDataNetType == NetworkType.Type_1X3G) {
                        tempLastSignalLevel[0] = iconLevel[0] = tempSignalStrength.getEvdoLevel();
                        tempLastSignalLevel[1] = iconLevel[1] = tempSignalStrength.getCdmaLevel();
                        Xlog.d(TAG," CT SlotId ("
                            + slotId
                            + ") two signal strength : tempLastSignalLevel[0] = "
                            + ""
                            + tempLastSignalLevel[0]
                            + "  tempLastSignalLevel[1] = "
                            + tempLastSignalLevel[1]);
                    }

                    boolean isRoaming;
                    if (isCdma(slotId)) {
                        isRoaming = isCdmaEri(slotId);
                    } else {
                        // Though mPhone is a Manager, this call is not an IPC
                        isRoaming = mPhone.isNetworkRoaming(slotId);
                    }
                    Xlog.d(TAG, "updateTelephonySignalStrength(" + slotId + "), isRoaming=" + isRoaming + 
                            ", mInetCondition=" + mInetCondition);
                    int simColorId = SIMHelper.getSIMColorIdBySlot(mContext, slotId);
                    if (simColorId == -1) {
                        return;
                    }

                    Xlog.d(TAG, "updateTelephonySignalStrength(" + slotId + "), simColorId=" + simColorId);
                    int signalIcon = PluginFactory.getStatusBarPlugin(mContext).getSignalStrengthIconGemini(simColorId,
                            iconLevel[0], false);
                    if (signalIcon != -1) {
                        tempPhoneSignalIconId[0].setResources(PluginFactory.getStatusBarPlugin(mContext)
                                .getPluginResources());
                        tempPhoneSignalIconId[0].setIconId(signalIcon);
                    } else {
                        iconList[0] = TelephonyIconsGemini.getTelephonySignalStrengthIconList(simColorId, false);
                        tempPhoneSignalIconId[0].setResources(null);
                        if (iconLevel[0] < 5) {
                            tempPhoneSignalIconId[0].setIconId(iconList[0][iconLevel[0]]);
                        }
                    }

                    // op03
                    if (tempDataNetType == NetworkType.Type_1X3G) {
                        int upSignalIcon = PluginFactory.getStatusBarPlugin(mContext).getSignalStrengthIconGemini(
                                simColorId, 0, iconLevel[0], false);
                        if (upSignalIcon != -1) {
                            tempPhoneSignalIconId[0].setResources(PluginFactory.getStatusBarPlugin(mContext)
                                    .getPluginResources());
                            tempPhoneSignalIconId[0].setIconId(upSignalIcon);
                        }
                        int downSignalIcon = PluginFactory.getStatusBarPlugin(mContext).getSignalStrengthIconGemini(
                                simColorId, 1, iconLevel[1], false);
                        if (downSignalIcon != -1) {
                            tempPhoneSignalIconId[1].setResources(PluginFactory.getStatusBarPlugin(mContext)
                                    .getPluginResources());
                            tempPhoneSignalIconId[1].setIconId(downSignalIcon);
                        }
                    }
                    
                    Xlog.d(TAG, "updateTelephonySignalStrength(" + slotId + "), tempDataNetType = " + tempDataNetType
                            + " , simColorId=" + simColorId + "  tempPhoneSignalIconId[0] = " + ""
                            + tempPhoneSignalIconId[0].getIconId() + "  tempPhoneSignalIconId[1] = "
                            + tempPhoneSignalIconId[1].getIconId());

                    String desc = PluginFactory.getStatusBarPlugin(mContext).getSignalStrengthDescription(iconLevel[0]);
                    if (desc != null) {
                        tempContentDescriptionPhoneSignal = desc;
                    } else {
                        if (iconLevel[0] < 5) {
                            tempContentDescriptionPhoneSignal = mContext
                                    .getString(AccessibilityContentDescriptions.PHONE_SIGNAL_STRENGTH[iconLevel[0]]);
                        }
                    }
                    tempDataSignalIconId = tempPhoneSignalIconId[0].clone();
                    
                }
            }
        }

        mDataSignalIconId[slotId] = tempDataSignalIconId.clone();
        mContentDescriptionPhoneSignal[slotId] = tempContentDescriptionPhoneSignal;
        mPhoneSignalIconId[slotId][0] = tempPhoneSignalIconId[0].clone();
        mPhoneSignalIconId[slotId][1] = tempPhoneSignalIconId[1].clone();
        
        Xlog.d(TAG, " updateTelephonySignalStrength(" + slotId + ") tempSIMCUSignVisible= " + tempSIMCUSignVisible);
        if (tempPhoneSignalIconId[0].getIconId() == -1) {
            tempSIMCUSignVisible = false;
        }
    }
    
    private final void updateDataNetType() {
        for (int i = 0; i < mGeminiSimNum; i++) {
    	    updateDataNetType(i);
        }    	      	  
    }

    private final void updateDataNetType(int slotId) {
        int tempDataNetType;
        NetworkType tempDataNetType3G = NetworkType.Type_G;

        tempDataNetType = mDataNetType[slotId];
        
        Xlog.d(TAG, "updateDataNetType(" + slotId + "), DataNetType=" + tempDataNetType + ".");
        int simColorId = SIMHelper.getSIMColorIdBySlot(mContext, slotId);
        if (simColorId == -1) {
            return;
        }
        Xlog.d(TAG, "updateDataNetType(" + slotId + "), simColorId=" + simColorId);

        boolean tempIsRoaming = false;
        if ((isCdma(slotId) && isCdmaEri(slotId))
                || mPhone.isNetworkRoaming(slotId)) {
            int tempRoamingId = 0;
            
            if (simColorId > -1 && simColorId < 4) {
                tempRoamingId = TelephonyIconsGemini.ROAMING[simColorId];
            }
            Xlog.d(TAG, "updateDataNetType(" + slotId + ")  RoamingresId= " + tempRoamingId + " simColorId = " + simColorId);
            mIsRoaming[slotId] = true;
            mIsRoamingId[slotId] = tempRoamingId;
            tempIsRoaming = true;
        } else {
            mIsRoaming[slotId] = false;
            mIsRoamingId[slotId] = 0;
        }

        DataType tempDateType;

        String tempContentDescriptionDataType;
        if (mIsWimaxEnabled && mWimaxConnected) {
            // wimax is a special 4g network not handled by telephony
            tempDateType = DataType.Type_4G;
            tempContentDescriptionDataType = mContext.getString(
                    R.string.accessibility_data_connection_4g);
        } else {
            switch (tempDataNetType) {
                case TelephonyManager.NETWORK_TYPE_UNKNOWN:
                    if (!mShowAtLeastThreeGees) {
                        tempDateType = DataType.Type_G;
                        tempContentDescriptionDataType = mContext.getString(
                                R.string.accessibility_data_connection_gprs);
                        break;
                    } else {
                        // fall through
                    }
                case TelephonyManager.NETWORK_TYPE_EDGE:
                    if (!mShowAtLeastThreeGees) {
                        tempDateType = DataType.Type_E;
                        tempContentDescriptionDataType = mContext.getString(
                                R.string.accessibility_data_connection_edge);
                        break;
                    } else {
                        // fall through
                    }
                case TelephonyManager.NETWORK_TYPE_UMTS:
                    tempDataNetType3G = NetworkType.Type_3G;
                    tempDateType = DataType.Type_3G;
                    tempContentDescriptionDataType = mContext.getString(
                            R.string.accessibility_data_connection_3g);
                    break;
                case TelephonyManager.NETWORK_TYPE_HSDPA:
                case TelephonyManager.NETWORK_TYPE_HSUPA:
                case TelephonyManager.NETWORK_TYPE_HSPA:
                    tempDataNetType3G = NetworkType.Type_3G;
                    if (mHspaDataDistinguishable) {
                        tempDateType = DataType.Type_H;
                        tempContentDescriptionDataType = mContext.getString(
                                R.string.accessibility_data_connection_3_5g);
                    } else {
                        tempDateType = DataType.Type_3G;
                        tempContentDescriptionDataType = mContext.getString(
                                R.string.accessibility_data_connection_3g);
                    }
                    break;
                case TelephonyManager.NETWORK_TYPE_HSPAP:
                    tempDataNetType3G = NetworkType.Type_3G;
                    if (mHspaDataDistinguishable) {
                        tempDateType = DataType.Type_H_PLUS;
                        tempContentDescriptionDataType = mContext.getString(
                                R.string.accessibility_data_connection_3_5g);
                    } else {
                        tempDateType = DataType.Type_3G;
                        tempContentDescriptionDataType = mContext.getString(
                                R.string.accessibility_data_connection_3g);
                    }
                    break;
                case TelephonyManager.NETWORK_TYPE_CDMA:
                    // display 1xRTT for IS95A/B
                    tempDataNetType3G = NetworkType.Type_1X;
                    tempDateType = DataType.Type_1X;
                    tempContentDescriptionDataType = mContext.getString(
                            R.string.accessibility_data_connection_cdma);
                    break;
                case TelephonyManager.NETWORK_TYPE_1xRTT:
                    tempDataNetType3G = NetworkType.Type_1X;
                    tempDateType = DataType.Type_1X;
                    tempContentDescriptionDataType = mContext.getString(
                            R.string.accessibility_data_connection_cdma);
                    break;
                case TelephonyManager.NETWORK_TYPE_EVDO_0: //fall through
                case TelephonyManager.NETWORK_TYPE_EVDO_A:
                case TelephonyManager.NETWORK_TYPE_EVDO_B:
                case TelephonyManager.NETWORK_TYPE_EHRPD:
                    tempDataNetType3G = NetworkType.Type_1X3G;
                    tempDateType = DataType.Type_3G;
                    tempContentDescriptionDataType = mContext.getString(
                            R.string.accessibility_data_connection_3g);
                    break;
                case TelephonyManager.NETWORK_TYPE_LTE:
                    tempDateType = DataType.Type_4G;
                    tempContentDescriptionDataType = mContext.getString(
                            R.string.accessibility_data_connection_4g);
                    break;
                default:
                    if (!mShowAtLeastThreeGees) {
                        tempDataNetType3G = NetworkType.Type_G;
                        tempDateType = DataType.Type_G;
                        tempContentDescriptionDataType = mContext.getString(
                                R.string.accessibility_data_connection_gprs);
                    } else {
                        tempDataNetType3G = NetworkType.Type_3G;
                        tempDateType = DataType.Type_3G;
                        tempContentDescriptionDataType = mContext.getString(
                                R.string.accessibility_data_connection_3g);
                    }
                    break;
            }
        }

        IconIdWrapper[] tempDataIconList = {new IconIdWrapper(),new IconIdWrapper(),new IconIdWrapper(),new IconIdWrapper()};
        IconIdWrapper tempDataTypeIconId = new IconIdWrapper();
        int[] iconList = PluginFactory.getStatusBarPlugin(mContext).getDataTypeIconListGemini(tempIsRoaming, tempDateType);
        if (iconList != null) {
            tempDataIconList[0].setResources(PluginFactory.getStatusBarPlugin(mContext).getPluginResources());
            tempDataIconList[0].setIconId(iconList[0]);
            tempDataIconList[1].setResources(PluginFactory.getStatusBarPlugin(mContext).getPluginResources());
            tempDataIconList[1].setIconId(iconList[1]);
            tempDataIconList[2].setResources(PluginFactory.getStatusBarPlugin(mContext).getPluginResources());
            tempDataIconList[2].setIconId(iconList[2]);
            tempDataIconList[3].setResources(PluginFactory.getStatusBarPlugin(mContext).getPluginResources());
            tempDataIconList[3].setIconId(iconList[3]);
            tempDataTypeIconId.setResources(PluginFactory.getStatusBarPlugin(mContext).getPluginResources());
            tempDataTypeIconId.setIconId(iconList[simColorId]);
        } else {
            iconList = TelephonyIconsGemini.getDataTypeIconListGemini(tempIsRoaming, tempDateType);
            tempDataIconList[0].setResources(null);
            tempDataIconList[0].setIconId(iconList[0]);
            tempDataIconList[1].setResources(null);
            tempDataIconList[1].setIconId(iconList[1]);
            tempDataIconList[2].setResources(null);
            tempDataIconList[2].setIconId(iconList[2]);
            tempDataIconList[3].setResources(null);
            tempDataIconList[3].setIconId(iconList[3]);
            tempDataTypeIconId.setResources(null);
            tempDataTypeIconId.setIconId(iconList[simColorId]);
        }
        if (tempDataNetType == TelephonyManager.NETWORK_TYPE_UNKNOWN || !SIMHelper.isSimInserted(slotId)) {
            if (!mShowAtLeastThreeGees) {
                tempDataTypeIconId.setResources(null);
                tempDataTypeIconId.setIconId(0);
            }
        }

        Xlog.d(TAG, "updateDataNetType(" + slotId + "), DataNetType3G=" + tempDataNetType3G + " tempDataTypeIconId= "
                + tempDataTypeIconId.getIconId() + ".");
        mDataNetType3G[slotId] = tempDataNetType3G;
        mContentDescriptionDataType[slotId] = tempContentDescriptionDataType;
        mDataTypeIconId[slotId] = tempDataTypeIconId.clone(); 
        mDataIconList[slotId] = tempDataIconList;
    }

    boolean isCdmaEri(int slotId) {
        ServiceState tempServiceState = mServiceState[slotId];
        
        if (tempServiceState != null) {
            final int iconIndex = tempServiceState.getCdmaEriIconIndex();
            if (iconIndex != EriInfo.ROAMING_INDICATOR_OFF) {
                final int iconMode = tempServiceState.getCdmaEriIconMode();
                if (iconMode == EriInfo.ROAMING_ICON_MODE_NORMAL
                        || iconMode == EriInfo.ROAMING_ICON_MODE_FLASH) {
                    return true;
                }
            }
        }
        return false;
    }

    private final void updateDataIcon() {
        for(int i = 0; i < mGeminiSimNum ; i++) {
            updateDataIcon(i);
        }
    }

    private final void updateDataIcon(int slotId) {
        int iconId = 0;
        boolean visible = true;
        ITelephony iTelephony = SIMHelper.getITelephony();
        NetworkType tempNetType3G = null;
        IccCardConstants.State tempSimState;
        int tempDataState;
        int tempDataActivity;
        IconIdWrapper[] tempDataIconList = { new IconIdWrapper(),
                new IconIdWrapper(), new IconIdWrapper(), new IconIdWrapper()};

        tempDataState = mDataState[slotId];
        tempSimState = mSimState[slotId];
        tempDataActivity = mDataActivity[slotId];
        tempNetType3G = mDataNetType3G[slotId];

        for ( int i = 0; i < mDataIconListNum; i++) {
            tempDataIconList[i] = mDataIconList[slotId][i].clone();
        }

        Xlog.d(TAG, "updateDataIcon(" + slotId + "), SimState=" + tempSimState + ", DataState=" + tempDataState + 
                ", DataActivity=" + tempDataActivity + ", tempNetType3G=" + tempNetType3G);

        if (!isCdma(slotId)) {
            // GSM case, we have to check also the sim state
            if (tempSimState == IccCardConstants.State.READY || tempSimState == IccCardConstants.State.UNKNOWN) {
                if (FeatureOption.MTK_DT_SUPPORT) {
                    int callState = -1;
                    try {
                        callState = iTelephony.getCallStateGemini(slotId);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Xlog.d(TAG, "updateDataIcon(" + slotId + "), Dual talk callState is " + callState +  ".");
                    
                    if (!(tempNetType3G == NetworkType.Type_3G)) {
                        if (hasService(slotId)
                                && tempDataState == TelephonyManager.DATA_CONNECTED
                                && callState == TelephonyManager.CALL_STATE_IDLE
                                && Settings.System
                                        .getInt(mContext.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) != 1) {

                            int simColorId = SIMHelper.getSIMColorIdBySlot(mContext, slotId);
                            Xlog.d(TAG, "updateDataIcon(" + slotId + "), simColorId=" + simColorId);
                            if (simColorId > -1) {
                                iconId = tempDataIconList[simColorId].getIconId();
                            }
                        } else {
                            iconId = 0;
                            visible = false;
                        }
                    } else {
                        if (hasService(slotId)
                                && tempDataState == TelephonyManager.DATA_CONNECTED
                                && Settings.System
                                        .getInt(mContext.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) != 1) {

                            int simColorId = SIMHelper.getSIMColorIdBySlot(mContext, slotId);
                            Xlog.d(TAG, "updateDataIcon(" + slotId + "), simColorId=" + simColorId);
                            if (simColorId > -1) {
                                iconId = tempDataIconList[simColorId].getIconId();
                            }
                        } else {
                            iconId = 0;
                            visible = false;
                        }
                    }
                } else {
                    int[] callState = new int[mGeminiSimNum];
                    boolean callStateAllIdle = true;                    
                    for(int i = 0; i < mGeminiSimNum; i++) {
                        try {
                            callState[i] = iTelephony.getCallStateGemini(i);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        Xlog.d(TAG, "updateDataIcon(" + slotId +"), callState(" + i + ") is " + callState[i]);
                        if(callState[i] != TelephonyManager.CALL_STATE_IDLE) {
                            callStateAllIdle = false;
                        }                        
                    }
                    
                    if (!(tempNetType3G == NetworkType.Type_3G)) {
                        if (hasService(slotId)
                                && tempDataState == TelephonyManager.DATA_CONNECTED
                                && callStateAllIdle
                                && Settings.System
                                        .getInt(mContext.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) != 1) {
                            int simColorId = SIMHelper.getSIMColorIdBySlot(mContext, slotId);
                            Xlog.d(TAG, "updateDataIcon(" + slotId + "), simColorId=" + simColorId);
                            if (simColorId > -1) {
                                iconId = tempDataIconList[simColorId].getIconId();
                            }
                        } else {
                            iconId = 0;
                            visible = false;
                        }
                    } else {  
                    	  final ITelephony telephony = ITelephony.Stub.asInterface(ServiceManager.checkService("phone"));
                    	  int tempCapabilitySIM = 0;
                    	  callStateAllIdle = true; 
                    	  
                    	  
                        if (telephony != null) {
                            try {
                                tempCapabilitySIM = telephony.get3GCapabilitySIM();                                    
                            } catch (RemoteException ex) {
                                ex.printStackTrace();
                            }
                        }
                        
                        for (int i = 0; i < mGeminiSimNum; i++) {                            
                            if (tempCapabilitySIM == i){
                                continue;
                            }                                
                            if (callState[i] != TelephonyManager.CALL_STATE_IDLE) {
                                callStateAllIdle = false;
                            }
                        }                          
                        
                        if (hasService(slotId)
                                && tempDataState == TelephonyManager.DATA_CONNECTED
                                && callStateAllIdle
                                && Settings.System
                                        .getInt(mContext.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) != 1) {

                            int simColorId = SIMHelper.getSIMColorIdBySlot(mContext, slotId);
                            Xlog.d(TAG, "updateDataIcon(" + slotId + "), simColorId=" + simColorId);
                            if (simColorId > -1) {
                                iconId = tempDataIconList[simColorId].getIconId();
                            }
                        } else {
                            iconId = 0;
                            visible = false;
                        }
                    }
                }
                
            } else {
                iconId = R.drawable.stat_sys_no_sim;
                visible = false; // no SIM? no data
            }
        } else {
            Xlog.d(TAG, "updateDataIcon(" + slotId + "), at cdma mode");
            // CDMA case, mDataActivity can be also DATA_ACTIVITY_DORMANT
            if (hasService(slotId) && tempDataState == TelephonyManager.DATA_CONNECTED) {

                int simColorId = SIMHelper.getSIMColorIdBySlot(mContext, slotId);
                Xlog.d(TAG, "updateDataIcon(" + slotId + "), simColorId=" + simColorId);
                if (simColorId > -1) {
                    iconId = tempDataIconList[simColorId].getIconId();
                }

            } else {
                iconId = 0;
                visible = false;
            }
        }

        // yuck - this should NOT be done by the status bar
        long ident = Binder.clearCallingIdentity();
        try {
            mBatteryStats.notePhoneDataConnectionState(mPhone.getNetworkType(slotId), visible);
        } catch (RemoteException e) {
            Xlog.d(TAG, "RemoteException");
        } finally {
            Binder.restoreCallingIdentity(ident);
        }

        Xlog.d(TAG, "updateDataIcon(" + slotId + "), iconId=" + iconId + ", visible=" + visible);
        mDataConnected[slotId] = visible;
        mDataDirectionIconId[slotId] = iconId;
        if (!FeatureOption.MTK_DT_SUPPORT) {
            if (mDataConnected[slotId]) {
                for (int i = 0 ; i < mGeminiSimNum ; i++) {
                    if ( i == slotId ) continue;
                    mDataConnected[i] = false;
                }
            }
        }
    }

    void updateNetworkName(int slotId, boolean showSpn, String spn, boolean showPlmn, String plmn) {
        Xlog.d(TAG, "updateNetworkName(" + slotId + "), showSpn=" + showSpn + 
                " spn=" + spn + " showPlmn=" + showPlmn + " plmn=" + plmn);

        StringBuilder str = new StringBuilder();
        boolean something = false;
        if (showPlmn && plmn != null) {
            str.append(plmn);
            something = true;
        }
        if (showSpn && spn != null) {
            if (something) {
                str.append(mNetworkNameSeparator);
            }
            str.append(spn);
            something = true;
        }
        
        if (something) {
            mNetworkName[slotId] = str.toString();
        } else {
            mNetworkName[slotId] = mNetworkNameDefault;
        }
        Xlog.d(TAG, "updateNetworkName(" + slotId + "), mNetworkName=" + mNetworkName[slotId]);
    }

    // ===== Wifi ===================================================================

    class WifiHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case AsyncChannel.CMD_CHANNEL_HALF_CONNECTED:
                    if (msg.arg1 == AsyncChannel.STATUS_SUCCESSFUL) {
                        mWifiChannel.sendMessage(Message.obtain(this,
                                AsyncChannel.CMD_CHANNEL_FULL_CONNECTION));
                    } else {
                        Xlog.e(TAG, "Failed to connect to wifi");
                    }
                    break;
                case WifiManager.DATA_ACTIVITY_NOTIFICATION:
                    if (msg.arg1 != mWifiActivity) {
                        mWifiActivity = msg.arg1;
                        refreshViews();
                    }
                    break;
                default:
                    //Ignore
                    break;
            }
        }
    }

    private void updateWifiState(Intent intent) {
        final String action = intent.getAction();
        if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
            mWifiEnabled = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
                    WifiManager.WIFI_STATE_UNKNOWN) == WifiManager.WIFI_STATE_ENABLED;
        } else if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
            final NetworkInfo networkInfo = (NetworkInfo)
                    intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            boolean wasConnected = mWifiConnected;
            mWifiConnected = networkInfo != null && networkInfo.isConnected();
            // If we just connected, grab the inintial signal strength and ssid
            if (mWifiConnected && !wasConnected) {
                // try getting it out of the intent first
                WifiInfo info = (WifiInfo) intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);
                if (info == null) {
                    info = mWifiManager.getConnectionInfo();
                }
                if (info != null) {
                    mWifiSsid = huntForSsid(info);
                } else {
                    mWifiSsid = null;
                }
            } else if (!mWifiConnected) {
                mWifiSsid = null;
            }
            // Apparently the wifi level is not stable at this point even if we've just connected to
            // the network; we need to wait for an RSSI_CHANGED_ACTION for that. So let's just set
            // it to 0 for now
            if (mWifiConnected) {
                WifiInfo wifiInfo = ((WifiManager) mContext
                        .getSystemService(Context.WIFI_SERVICE))
                        .getConnectionInfo();
                if (wifiInfo != null) {
                    int newRssi = wifiInfo.getRssi();
                    int newSignalLevel = WifiManager.calculateSignalLevel(
                            newRssi, WifiIcons.WIFI_LEVEL_COUNT);
                    if (newSignalLevel != mWifiLevel) {
                        mWifiLevel = newSignalLevel;
                    }
                }
            }
        } else if (action.equals(WifiManager.RSSI_CHANGED_ACTION)) {
            mWifiRssi = intent.getIntExtra(WifiManager.EXTRA_NEW_RSSI, -200);
            mWifiLevel = WifiManager.calculateSignalLevel(
                    mWifiRssi, WifiIcons.WIFI_LEVEL_COUNT);
        }
        
        Xlog.d(TAG, "updateWifiState: mWifiLevel = " + mWifiLevel
                + "  mWifiRssi=" + mWifiRssi + " mWifiConnected is " + mWifiConnected);

        updateWifiIcons();
    }

    private void updateWifiIcons() {
        if (mWifiConnected) {
            mWifiIconId = WifiIcons.WIFI_SIGNAL_STRENGTH[mInetCondition][mWifiLevel];
            mContentDescriptionWifi = mContext.getString(
                    AccessibilityContentDescriptions.WIFI_CONNECTION_STRENGTH[mWifiLevel]);
        } else {
            if (mDataAndWifiStacked) {
                mWifiIconId = 0;
            } else {
                mWifiIconId = mWifiEnabled ? R.drawable.stat_sys_wifi_signal_null : 0;
            }
            mContentDescriptionWifi = mContext.getString(R.string.accessibility_no_wifi);
        }
    }

    private String huntForSsid(WifiInfo info) {
        String ssid = info.getSSID();
        if (ssid != null) {
            return ssid;
        }
        // OK, it's not in the connectionInfo; we have to go hunting for it
        List<WifiConfiguration> networks = mWifiManager.getConfiguredNetworks();
        for (WifiConfiguration net : networks) {
            if (net.networkId == info.getNetworkId()) {
                return net.SSID;
            }
        }
        return null;
    }

    // ===== Wimax ===================================================================
    private final void updateWimaxState(Intent intent) {
        final String action = intent.getAction();
        boolean wasConnected = mWimaxConnected;
        if (action.equals(WimaxManagerConstants.NET_4G_STATE_CHANGED_ACTION)) {
            int wimaxStatus = intent.getIntExtra(WimaxManagerConstants.EXTRA_4G_STATE,
                    WimaxManagerConstants.NET_4G_STATE_UNKNOWN);
            mIsWimaxEnabled = (wimaxStatus ==
                    WimaxManagerConstants.NET_4G_STATE_ENABLED);
        } else if (action.equals(WimaxManagerConstants.SIGNAL_LEVEL_CHANGED_ACTION)) {
            mWimaxSignal = intent.getIntExtra(WimaxManagerConstants.EXTRA_NEW_SIGNAL_LEVEL, 0);
        } else if (action.equals(WimaxManagerConstants.WIMAX_NETWORK_STATE_CHANGED_ACTION)) {
            mWimaxState = intent.getIntExtra(WimaxManagerConstants.EXTRA_WIMAX_STATE,
                    WimaxManagerConstants.NET_4G_STATE_UNKNOWN);
            mWimaxExtraState = intent.getIntExtra(
                    WimaxManagerConstants.EXTRA_WIMAX_STATE_DETAIL,
                    WimaxManagerConstants.NET_4G_STATE_UNKNOWN);
            mWimaxConnected = (mWimaxState ==
                    WimaxManagerConstants.WIMAX_STATE_CONNECTED);
            mWimaxIdle = (mWimaxExtraState == WimaxManagerConstants.WIMAX_IDLE);
        }
        updateDataNetType(PhoneConstants.GEMINI_SIM_1);
        updateWimaxIcons();
    }

    private void updateWimaxIcons() {
        if (mIsWimaxEnabled) {
            if (mWimaxConnected) {
                if (mWimaxIdle) {
                    mWimaxIconId = WimaxIcons.WIMAX_IDLE;
                } else {
                    mWimaxIconId = WimaxIcons.WIMAX_SIGNAL_STRENGTH[mInetCondition][mWimaxSignal];
                }
                mContentDescriptionWimax = mContext.getString(
                        AccessibilityContentDescriptions.WIMAX_CONNECTION_STRENGTH[mWimaxSignal]);
            } else {
                mWimaxIconId = WimaxIcons.WIMAX_DISCONNECTED;
                mContentDescriptionWimax = mContext.getString(R.string.accessibility_no_wimax);
            }
        } else {
            mWimaxIconId = 0;
        }
    }


    // ===== Full or limited Internet connectivity ==================================

    private void updateConnectivity(Intent intent) {
        if (CHATTY) {
            Xlog.d(TAG, "updateConnectivity: intent=" + intent);
        }
        final ConnectivityManager connManager = (ConnectivityManager) mContext
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo info = connManager.getActiveNetworkInfo();

        // Are we connected at all, by any interface?
        mConnected = info != null && info.isConnected();
        if (mConnected) {
            mConnectedNetworkType = info.getType();
            mConnectedNetworkTypeName = info.getTypeName();
        } else {
            mConnectedNetworkType = ConnectivityManager.TYPE_NONE;
            mConnectedNetworkTypeName = null;
        }
        int connectionStatus = intent.getIntExtra(ConnectivityManager.EXTRA_INET_CONDITION, 0);

        if (CHATTY) {
            Xlog.d(TAG, "updateConnectivity: networkInfo=" + info);
            Xlog.d(TAG, "updateConnectivity: connectionStatus=" + connectionStatus);
        }

        mInetCondition = (connectionStatus > INET_CONDITION_THRESHOLD ? 1 : 0);
        Xlog.d(TAG, "updateConnectivity, mInetCondition=" + mInetCondition);

        if (info != null && info.getType() == ConnectivityManager.TYPE_BLUETOOTH) {
            mBluetoothTethered = info.isConnected();
        } else {
            mBluetoothTethered = false;
        }

        // We want to update all the icons, all at once, for any condition change
        int slotId = intent.getIntExtra(ConnectivityManager.EXTRA_SIM_ID, PhoneConstants.GEMINI_SIM_1);
        updateDataNetType(slotId);
        updateWimaxIcons();
        updateDataIcon(slotId);
        updateTelephonySignalStrength(slotId);
        updateWifiIcons();
    }


    // ===== Update the views =======================================================

    void refreshViews() {
        for (int i = 0 ; i < mGeminiSimNum ; i++) {
            refreshViews(i);
        }
    }

    void refreshViews(int slotId) {
        Context context = mContext;

        IconIdWrapper combinedSignalIconId = new IconIdWrapper();
        IconIdWrapper combinedActivityIconId = new IconIdWrapper();
        String combinedLabel = "";
        String wifiLabel = "";
        String mobileLabel = "";
        int N;
        final boolean emergencyOnly = isEmergencyOnly();

        boolean tempDataConnected;
        NetworkType tempDataNetType3G;
        String tempNetworkName;
        ServiceState tempServiceState;
        SignalStrength tempSignalStrength;
        IconIdWrapper tempDataSignalIconId = new IconIdWrapper();
        IconIdWrapper tempPhoneSignalIconId[] = { new IconIdWrapper(), new IconIdWrapper() };
        int tempDataActivity;
        String tempContentDescriptionPhoneSignal = "";
        String tempContentDescriptionDataType = "";
        String tempContentDescriptionCombinedSignal = "";

        tempSignalStrength = mSignalStrength[slotId];
        tempServiceState = mServiceState[slotId];
        tempDataConnected = mDataConnected[slotId];
        tempDataActivity = mDataActivity[slotId];
        tempDataNetType3G = mDataNetType3G[slotId];
        tempDataSignalIconId = mDataSignalIconId[slotId].clone();
        tempContentDescriptionPhoneSignal = mContentDescriptionPhoneSignal[slotId];
        tempContentDescriptionDataType = mContentDescriptionDataType[slotId];
        tempPhoneSignalIconId[0] = mPhoneSignalIconId[slotId][0].clone();
        tempPhoneSignalIconId[1] = mPhoneSignalIconId[slotId][1].clone();
        tempNetworkName = mNetworkName[slotId];

        if (!mHasMobileDataFeature) {
            tempDataSignalIconId.setResources(null);
            tempDataSignalIconId.setIconId(0);
            tempPhoneSignalIconId[0].setResources(null);
            tempPhoneSignalIconId[0].setIconId(0);
            tempPhoneSignalIconId[1].setResources(null);
            tempPhoneSignalIconId[1].setIconId(0);
            mobileLabel = "";
        } else {
            // We want to show the carrier name if in service and either:
            //   - We are connected to mobile data, or
            //   - We are not connected to mobile data, as long as the *reason* packets are not
            //     being routed over that link is that we have better connectivity via wifi.
            // If data is disconnected for some other reason but wifi is connected, we show nothing.
            // Otherwise (nothing connected) we show "No internet connection".

            if (!mIsScreenLarge) {
                if (mDataConnected[PhoneConstants.GEMINI_SIM_1]) {
                    mobileLabel = tempNetworkName;
                } else if (mConnected || emergencyOnly) {
                    if (hasService(slotId) || emergencyOnly) {
                        mobileLabel = tempNetworkName;
                    } else {
                        mobileLabel = "";
                    }
                } else {
                    mobileLabel
                        = context.getString(R.string.status_bar_settings_signal_meter_disconnected);
                }
            } else {
               if (hasService(slotId)) {
                   mobileLabel = tempNetworkName;
               } else {
                   mobileLabel = "";
               }
            }

            Xlog.d(TAG, "refreshViews(" + slotId + "), DataConnected=" + tempDataConnected);
        
            if (tempDataConnected) {
                int simColorId = SIMHelper.getSIMColorIdBySlot(mContext, slotId);
                Xlog.d(TAG, "refreshViews(" + slotId + ")" + " simColorId = " + simColorId);
                combinedSignalIconId = tempDataSignalIconId.clone();
                IconIdWrapper tempMobileActivityIconId = new IconIdWrapper();
                int[] iconList = PluginFactory.getStatusBarPlugin(mContext).getDataActivityIconList(simColorId, false);
                if (iconList != null) {
                    tempMobileActivityIconId.setResources(PluginFactory.getStatusBarPlugin(mContext).getPluginResources());
                    tempMobileActivityIconId.setIconId(iconList[tempDataActivity]);
                } else {
                     tempMobileActivityIconId.setResources(null);
                     switch (tempDataActivity) {/// need change in out color
                        case TelephonyManager.DATA_ACTIVITY_IN:
                            tempMobileActivityIconId.setIconId(R.drawable.stat_sys_signal_in);
                            break;
                        case TelephonyManager.DATA_ACTIVITY_OUT:
                            tempMobileActivityIconId.setIconId(R.drawable.stat_sys_signal_out);
                            break;
                        case TelephonyManager.DATA_ACTIVITY_INOUT:
                            tempMobileActivityIconId.setIconId(R.drawable.stat_sys_signal_inout);
                            break;
                        default:
                            tempMobileActivityIconId.setIconId(0);
                            break;
                    }
                }

                combinedLabel = mobileLabel;
                combinedActivityIconId = tempMobileActivityIconId.clone();
                combinedSignalIconId = tempDataSignalIconId.clone(); // set by updateDataIcon()
                tempContentDescriptionCombinedSignal = tempContentDescriptionDataType;
                mMobileActivityIconId[slotId] = tempMobileActivityIconId.clone();

                if (!FeatureOption.MTK_DT_SUPPORT) {
                    for ( int i = 0 ; i < mGeminiSimNum ; i++) {
                         if ( i == slotId) {
                            Xlog.d(TAG, "refreshViews(" + slotId + "), mMobileActivityIconId=" + mMobileActivityIconId[i].getIconId());
                            continue;
                        }
                        mMobileActivityIconId[i].setResources(null);
                        mMobileActivityIconId[i].setIconId(0);
                        Xlog.d(TAG, "refreshViews(" + slotId + "), mMobileActivityIconId=" + mMobileActivityIconId[i].getIconId());
                    }
                }
            } else {
                combinedActivityIconId.setResources(null);
                combinedActivityIconId.setIconId(0);
                mMobileActivityIconId[slotId].setResources(null);
                mMobileActivityIconId[slotId].setIconId(0);
                if (!FeatureOption.MTK_DT_SUPPORT) {
                    for (int i = 0 ; i < mGeminiSimNum ; i++) {
                        if (i == slotId) continue;
                        mMobileActivityIconId[slotId].setResources(null);
                        mMobileActivityIconId[slotId].setIconId(0);
                    }
                }
            }
        }

        if (mWifiConnected) {
            if (mWifiSsid == null) {
                wifiLabel = context.getString(R.string.status_bar_settings_signal_meter_wifi_nossid);
                mWifiActivityIconId = 0; // no wifis, no bits
            } else {
                wifiLabel = mWifiSsid;
                if (DEBUG) {
                    wifiLabel += "xxxxXXXXxxxxXXXX";
                }
                switch (mWifiActivity) {
                    case WifiManager.DATA_ACTIVITY_IN:
                        mWifiActivityIconId = R.drawable.stat_sys_wifi_in;
                        break;
                    case WifiManager.DATA_ACTIVITY_OUT:
                        mWifiActivityIconId = R.drawable.stat_sys_wifi_out;
                        break;
                    case WifiManager.DATA_ACTIVITY_INOUT:
                        mWifiActivityIconId = R.drawable.stat_sys_wifi_inout;
                        break;
                    case WifiManager.DATA_ACTIVITY_NONE:
                        mWifiActivityIconId = 0;
                        break;
                default:
                    break;
                }
            }
            combinedLabel = wifiLabel;
            combinedActivityIconId.setResources(null);
            combinedActivityIconId.setIconId(mWifiActivityIconId);
            combinedSignalIconId.setResources(null);
            combinedSignalIconId.setIconId(mWifiIconId); // set by updateWifiIcons()
            tempContentDescriptionCombinedSignal = mContentDescriptionWifi;
        } else {
            if (mHasMobileDataFeature) {
                wifiLabel = "";
            } else {
                wifiLabel = context.getString(R.string.status_bar_settings_signal_meter_disconnected);
            }
        }

        if (mBluetoothTethered) {
            combinedLabel = mContext.getString(R.string.bluetooth_tethered);
            combinedSignalIconId.setResources(null);
            combinedSignalIconId.setIconId(mBluetoothTetherIconId);
            tempContentDescriptionCombinedSignal = mContext.getString(
                    R.string.accessibility_bluetooth_tether);
        }

        final boolean ethernetConnected = (mConnectedNetworkType == ConnectivityManager.TYPE_ETHERNET);
        if (ethernetConnected) {
            // TODO: icons and strings for Ethernet connectivity
            combinedLabel = mConnectedNetworkTypeName;
        }

        if (mAirplaneMode &&
                (tempServiceState == null || (!hasService(slotId) && !tempServiceState.isEmergencyOnly()))) {
            // Only display the flight-mode icon if not in "emergency calls only" mode.

            // look again; your radios are now airplanes
            Xlog.d(TAG, "refreshViews(" + slotId + "), AirplaneMode=" + mAirplaneMode);
            tempContentDescriptionPhoneSignal = mContext.getString(R.string.accessibility_airplane_mode);
            if (SIMHelper.isSimInserted(slotId)) {
                mDataSignalIconId[slotId].setResources(null);
                mDataSignalIconId[slotId].setIconId(R.drawable.stat_sys_gemini_radio_off);
                tempDataSignalIconId = mDataSignalIconId[slotId].clone();
                mPhoneSignalIconId[slotId][0].setResources(null);
                mPhoneSignalIconId[slotId][0].setIconId(R.drawable.stat_sys_gemini_radio_off);
                mDataTypeIconId[slotId].setResources(null);
                mDataTypeIconId[slotId].setIconId(0);
            }

            // combined values from connected wifi take precedence over airplane mode
            if (mWifiConnected) {
                // Suppress "No internet connection." from mobile if wifi connected.
                mobileLabel = "";
            } else {
                if (mHasMobileDataFeature) {
                    // let the mobile icon show "No internet connection."
                    wifiLabel = "";
                } else {
                    wifiLabel = context.getString(R.string.status_bar_settings_signal_meter_disconnected);
                    combinedLabel = wifiLabel;
                }
                tempContentDescriptionCombinedSignal = tempContentDescriptionPhoneSignal;
                combinedSignalIconId = tempDataSignalIconId.clone();
            }
            
        } else if (!tempDataConnected && !mWifiConnected && !mBluetoothTethered && !mWimaxConnected && !ethernetConnected) {
            // pretty much totally disconnected

            combinedLabel = context.getString(R.string.status_bar_settings_signal_meter_disconnected);
            // On devices without mobile radios, we want to show the wifi icon
            if (!mIsScreenLarge) {
                if (mHasMobileDataFeature) {
                    combinedSignalIconId = tempDataSignalIconId.clone();
                } else {
                    combinedSignalIconId.setResources(null);
                    combinedSignalIconId.setIconId(mWifiIconId);
                }
                tempContentDescriptionCombinedSignal = mHasMobileDataFeature
                       ? tempContentDescriptionDataType : mContentDescriptionWifi;
            } else {
                if (mHasMobileDataFeature) {
                    combinedSignalIconId.setResources(null);
                    combinedSignalIconId.setIconId(mWifiIconId);
                    tempContentDescriptionCombinedSignal = mContentDescriptionWifi;
                } else {
                     if ((slotId == PhoneConstants.GEMINI_SIM_2) && mDataConnected[PhoneConstants.GEMINI_SIM_1]) {
                         combinedLabel = mNetworkName[PhoneConstants.GEMINI_SIM_1];
                         combinedSignalIconId = mDataSignalIconId[PhoneConstants.GEMINI_SIM_1].clone();
                         tempContentDescriptionCombinedSignal = mContentDescriptionDataType[PhoneConstants.GEMINI_SIM_1];
                     } else if ((slotId == PhoneConstants.GEMINI_SIM_1) && mDataConnected[PhoneConstants.GEMINI_SIM_2]) {
                         combinedLabel = mNetworkName[PhoneConstants.GEMINI_SIM_2];
                         combinedSignalIconId = mDataSignalIconId[PhoneConstants.GEMINI_SIM_2].clone();
                         tempContentDescriptionCombinedSignal = mContentDescriptionDataType[PhoneConstants.GEMINI_SIM_2];
                     } else {
                         combinedSignalIconId.setResources(null);
                         combinedSignalIconId.setIconId(mWifiIconId);
                         tempContentDescriptionCombinedSignal = tempContentDescriptionDataType;
                     }
                }
            }

            IccCardConstants.State tempSimState;
            IconIdWrapper cmccDataTypeIconId = new IconIdWrapper();
            tempSimState = mSimState[slotId];
            cmccDataTypeIconId = mDataTypeIconId[slotId].clone();

            int dataTypeIconId = 0;
            if ((isCdma(slotId) && isCdmaEri(slotId)) || mPhone.isNetworkRoaming(slotId)) {

                int simColorId = SIMHelper.getSIMColorIdBySlot(mContext, slotId);
                int tempRoamingId = 0;
                
                if (simColorId > -1 && simColorId < 4) {
                    tempRoamingId = TelephonyIconsGemini.ROAMING[simColorId];
                }
                Xlog.d(TAG, "refreshViews(" + slotId + ")  RoamingresId= " + tempRoamingId + " simColorId = " + simColorId);
                mIsRoaming[slotId] = true;
                mIsRoamingId[slotId] = tempRoamingId;
            } else {
                mIsRoaming[slotId] = false;
                mIsRoamingId[slotId] = 0;
                dataTypeIconId = 0;
            }
            Xlog.d(TAG, "refreshViews(" + slotId + "), dataTypeIconId=" + dataTypeIconId);
            mDataTypeIconId[slotId].setResources(null);
            mDataTypeIconId[slotId].setIconId(dataTypeIconId);

            if (PluginFactory.getStatusBarPlugin(mContext).supportDataTypeAlwaysDisplayWhileOn()) {
                Xlog.d(TAG, "refreshViews(" + slotId + "), SimState=" + tempSimState 
                        + ", mDataTypeIconId=" + cmccDataTypeIconId.getIconId());
                mDataTypeIconId[slotId] = cmccDataTypeIconId.clone();
            }
        }

        int tempDataDirectionIconId;
        IconIdWrapper tempDataTypeIconId = new IconIdWrapper();
        IconIdWrapper tempMobileActivityIconId = new IconIdWrapper();
        tempDataDirectionIconId = mDataDirectionIconId[slotId];
        tempPhoneSignalIconId[0] = mPhoneSignalIconId[slotId][0].clone();
        tempPhoneSignalIconId[1] = mPhoneSignalIconId[slotId][1].clone();
        tempDataTypeIconId = mDataTypeIconId[slotId].clone();
        tempMobileActivityIconId = mMobileActivityIconId[slotId].clone();        
        
        if (DEBUG) {
            Xlog.d(TAG, "refreshViews connected={"
                    + (mWifiConnected ? " wifi" : "")
                    + (tempDataConnected ? " data" : "")
                    + " } level="
                    + ((tempSignalStrength == null) ? "??" : Integer.toString(tempSignalStrength.getLevel()))
                    + " combinedSignalIconId=0x"
                    + Integer.toHexString(combinedSignalIconId.getIconId())
                    + "/" + getResourceName(combinedSignalIconId.getIconId())
                    + " combinedActivityIconId=0x" + Integer.toHexString(combinedActivityIconId.getIconId())
                    + " mobileLabel=" + mobileLabel
                    + " wifiLabel=" + wifiLabel
                    + " combinedLabel=" + combinedLabel
                    + " mAirplaneMode=" + mAirplaneMode
                    + " mDataActivity=" + tempDataActivity
                    + " mPhoneSignalIconId=0x" + Integer.toHexString(tempPhoneSignalIconId[0].getIconId())
                    + " mPhoneSignalIconId2=0x" + Integer.toHexString(tempPhoneSignalIconId[1].getIconId())
                    + " mDataDirectionIconId=0x" + Integer.toHexString(tempDataDirectionIconId)
                    + " mDataSignalIconId=0x" + Integer.toHexString(tempDataSignalIconId.getIconId())
                    + " mDataTypeIconId=0x" + Integer.toHexString(tempDataTypeIconId.getIconId())
                    + " mWifiIconId=0x" + Integer.toHexString(mWifiIconId)
                    + " mBluetoothTetherIconId=0x" + Integer.toHexString(mBluetoothTetherIconId));
        }

        int tempLastPhoneSignalIconId[];
        int tempLastDataTypeIconId;
        int tempLastMobileActivityIconId;
        
        tempLastPhoneSignalIconId = mLastPhoneSignalIconId[slotId];
        tempLastDataTypeIconId = mLastDataTypeIconId[slotId];
        tempLastMobileActivityIconId = mLastMobileActivityIconId[slotId];

        if (tempLastPhoneSignalIconId[0]    != tempPhoneSignalIconId[0].getIconId()
         || tempLastPhoneSignalIconId[1]    != tempPhoneSignalIconId[1].getIconId()
         || mLastDataDirectionOverlayIconId != combinedActivityIconId.getIconId()
         || mLastWifiIconId                 != mWifiIconId
         || mLastWimaxIconId                != mWimaxIconId
         || tempLastDataTypeIconId          != tempDataTypeIconId.getIconId()
         || tempLastMobileActivityIconId          != tempMobileActivityIconId.getIconId()
         || mLastAirplaneMode != mAirplaneMode) {
            
            Xlog.d(TAG, "refreshViews(" + slotId + "), set parameters to signal cluster view.");
            // NB: the mLast*s will be updated later
            for (SignalCluster cluster : mSignalClusters) {
                cluster.setWifiIndicators(
                        mWifiConnected, // only show wifi in the cluster if connected
                        mWifiIconId,
                        mWifiActivityIconId,
                        mContentDescriptionWifi);

                Xlog.d(TAG, "refreshViews(" + slotId + "), tempPhoneSignalIconId.0 = " + tempPhoneSignalIconId[0].getIconId()
                        + "  tempPhoneSignalIconId.1 = " + tempPhoneSignalIconId[1].getIconId()
                        + "  tempMobileActivityIconId= " + tempMobileActivityIconId.getIconId()
                        + "  tempDataTypeIconId= " + tempDataTypeIconId.getIconId());
                cluster.setMobileDataIndicators(
                        slotId,
                        mHasMobileDataFeature,
                        tempPhoneSignalIconId,
                        tempMobileActivityIconId,
                        tempDataTypeIconId,
                        tempContentDescriptionPhoneSignal,
                        tempContentDescriptionDataType);
                cluster.setIsAirplaneMode(mAirplaneMode);
                mLastAirplaneMode = mAirplaneMode;
            }
        }
        for (SignalCluster cluster : mSignalClusters) {
            cluster.setRoamingFlagandResource(mIsRoaming, mIsRoamingId);
            cluster.setShowSimIndicator(slotId, mSimIndicatorFlag[slotId], mSimIndicatorResId[slotId]);           
            cluster.setDataNetType3G(slotId, tempDataNetType3G);
        }
        
        //for cluster apply
        for (SignalCluster cluster : mSignalClusters) {
            cluster.apply();
        }

        // the phone icon on phones
        if (!mIsScreenLarge) {
            if (tempLastPhoneSignalIconId[0] != tempPhoneSignalIconId[0].getIconId() ||
                tempLastPhoneSignalIconId[1] != tempPhoneSignalIconId[1].getIconId()) {
                
                mLastPhoneSignalIconId[slotId][0] = tempPhoneSignalIconId[0].getIconId();
                mLastPhoneSignalIconId[slotId][1] = tempPhoneSignalIconId[1].getIconId();
                
                N = mPhoneSignalIconViews.size();
                for (int i = 0; i < N; i++) {
                  final ImageView v = mPhoneSignalIconViews.get(i);
                  if (tempPhoneSignalIconId[0].getIconId() == 0) {
                    v.setVisibility(View.GONE);
                  } else {
                    v.setVisibility(View.VISIBLE);
                    if (tempPhoneSignalIconId[0].getResources() != null) {
                        v.setImageDrawable(tempPhoneSignalIconId[0].getDrawable());
                    } else {
                        if (tempPhoneSignalIconId[0].getIconId() == 0) {
                            v.setImageDrawable(null);
                        } else {
                            v.setImageResource(tempPhoneSignalIconId[0].getIconId());
                        }
                    }
                    v.setContentDescription(tempContentDescriptionPhoneSignal);
                 }
               }
            }
        } else {
            if (tempLastPhoneSignalIconId[0] != tempPhoneSignalIconId[0].getIconId() ||
                    tempLastPhoneSignalIconId[1] != tempPhoneSignalIconId[1].getIconId()) {
                final ImageView v;
                
                mLastPhoneSignalIconId[slotId][0] = tempPhoneSignalIconId[0].getIconId();
                mLastPhoneSignalIconId[slotId][1] = tempPhoneSignalIconId[1].getIconId();
                v = mPhoneSignalIconViews.get(slotId);
                
                if (v != null) {
                    if ((tempPhoneSignalIconId[0].getIconId() == 0) || (!hasService(slotId))) {
                        v.setVisibility(View.GONE);
                    } else {
                        v.setVisibility(View.VISIBLE);
                        if (tempPhoneSignalIconId[0].getResources() != null) {
                            v.setImageDrawable(tempPhoneSignalIconId[0].getDrawable());
                        } else {
                            if (tempPhoneSignalIconId[0].getIconId() == 0) {
                                v.setImageDrawable(null);
                            } else {
                                v.setImageResource(tempPhoneSignalIconId[0].getIconId());
                            }
                        }
                        v.setContentDescription(tempContentDescriptionPhoneSignal);
                    }
                }
            }
        }

        // the data icon on phones
        if (mLastDataDirectionIconId != tempDataDirectionIconId) {
            mLastDataDirectionIconId = tempDataDirectionIconId;
            N = mDataDirectionIconViews.size();
            for (int i = 0; i < N; i++) {
                final ImageView v = mDataDirectionIconViews.get(i);
                if (tempDataDirectionIconId == 0) {
                   if (!mIsScreenLarge) {
                      v.setVisibility(View.INVISIBLE);
                   } else {
                      v.setVisibility(View.GONE);
                   }
                } else {
                   v.setVisibility(View.VISIBLE);
                   v.setImageResource(tempDataDirectionIconId);
                   v.setContentDescription(tempContentDescriptionDataType);
                }
            }
       }
        // the wifi icon on phones
        if (mLastWifiIconId != mWifiIconId) {
            mLastWifiIconId = mWifiIconId;
            N = mWifiIconViews.size();
            for (int i = 0; i < N; i++) {
                final ImageView v = mWifiIconViews.get(i);
                if (mWifiIconId == 0) {
                    v.setVisibility(View.GONE);
                } else {
                    v.setVisibility(View.VISIBLE);
                    v.setImageResource(mWifiIconId);
                    v.setContentDescription(mContentDescriptionWifi);
                }
            }
        }

        // the wimax icon on phones
        if (mLastWimaxIconId != mWimaxIconId) {
            mLastWimaxIconId = mWimaxIconId;
            N = mWimaxIconViews.size();
            for (int i = 0; i < N; i++) {
                final ImageView v = mWimaxIconViews.get(i);
                if (mWimaxIconId == 0) {
                    v.setVisibility(View.GONE);
                } else {
                    v.setVisibility(View.VISIBLE);
                    v.setImageResource(mWimaxIconId);
                    v.setContentDescription(mContentDescriptionWimax);
                }
           }
        }
        // the combined data signal icon
        if (mLastCombinedSignalIconId != combinedSignalIconId.getIconId()) {
            mLastCombinedSignalIconId = combinedSignalIconId.getIconId();
            N = mCombinedSignalIconViews.size();
            for (int i = 0; i < N; i++) {
                final ImageView v = mCombinedSignalIconViews.get(i);
                if (!mIsScreenLarge) {
                    if (combinedSignalIconId.getResources() != null) {
                        v.setImageDrawable(combinedSignalIconId.getDrawable());
                    } else {
                        if (combinedSignalIconId.getIconId() == 0) {
                            v.setImageDrawable(null);
                        } else {
                            v.setImageResource(combinedSignalIconId.getIconId());
                        }
                    }
                    v.setContentDescription(tempContentDescriptionCombinedSignal);
                } else {
                    if (mWifiConnected) {
                       boolean flag = false;
                       for ( int j = 0; j < mGeminiSimNum ; j++) {
                           if(mDataConnected[j]) {
                               flag = true;
                               break;
                           }
                       }
                       if(flag) {
                           v.setVisibility(View.VISIBLE);
                           if (combinedSignalIconId.getResources() != null) {
                               v.setImageDrawable(combinedSignalIconId.getDrawable());
                           } else {
                               if (combinedSignalIconId.getIconId() == 0) {
                                   v.setImageDrawable(null);
                               } else {
                                   v.setImageResource(combinedSignalIconId.getIconId());
                               }
                           }
                           v.setContentDescription(tempContentDescriptionCombinedSignal);
                       }
                       v.setContentDescription(tempContentDescriptionCombinedSignal);
                    } else {
                       v.setVisibility(View.GONE);
                    }
                }
            }
        }

        // the data network type overlay
        if (!mIsScreenLarge) {
            if ((tempLastDataTypeIconId != tempDataTypeIconId.getIconId()) || (mWifiConnected && mIsScreenLarge)) {
                mLastDataTypeIconId[slotId] = tempDataTypeIconId.getIconId();
                
                N = mDataTypeIconViews.size();
                for (int i = 0; i < N; i++) {
                    final ImageView v = mDataTypeIconViews.get(i);
                    if ((tempDataTypeIconId.getIconId() == 0) && mIsScreenLarge) {
                        v.setVisibility(View.GONE); 
                    } else if (mIsScreenLarge && ((tempDataTypeIconId.getIconId() == 0) || mWifiConnected)) {
                        v.setVisibility(View.GONE);
                    } else {
                        v.setVisibility(View.VISIBLE);
                        if (tempDataTypeIconId.getResources() != null) {
                            v.setImageDrawable(tempDataTypeIconId.getDrawable());
                        } else {
                            if (tempDataTypeIconId.getIconId() == 0) {
                                v.setImageDrawable(null);
                            } else {
                                v.setImageResource(tempDataTypeIconId.getIconId());
                            }
                        }
                        v.setContentDescription(tempContentDescriptionDataType);
                    }
                }
           }
        } else {            
            final ImageView v;
            mLastDataTypeIconId[slotId] = tempDataTypeIconId.getIconId();
            v = mDataTypeIconViews.get(slotId);
            
            if ((tempLastDataTypeIconId != tempDataTypeIconId.getIconId()) || (mWifiConnected && mIsScreenLarge)) {
                if ((tempDataTypeIconId.getIconId() == 0) && mIsScreenLarge) {
                    v.setVisibility(View.GONE); 
                } else if (mIsScreenLarge && ((tempDataTypeIconId.getIconId() == 0) || mWifiConnected)) {
                    v.setVisibility(View.GONE);
                } else {
                    v.setVisibility(View.VISIBLE);
                    if (tempDataTypeIconId.getResources() != null) {
                        v.setImageDrawable(tempDataTypeIconId.getDrawable());
                    } else {
                        if (tempDataTypeIconId.getIconId() == 0) {
                            v.setImageDrawable(null);
                        } else {
                            v.setImageResource(tempDataTypeIconId.getIconId());
                        }
                    }
                    v.setContentDescription(tempContentDescriptionDataType);
                }
            }
        }
        if ((tempLastMobileActivityIconId != tempMobileActivityIconId.getIconId())) {
            mLastMobileActivityIconId[slotId] = tempMobileActivityIconId.getIconId();
       }

        // the data direction overlay
        if (mLastDataDirectionOverlayIconId != combinedActivityIconId.getIconId()) {
            if (DEBUG) {
                Xlog.d(TAG, "changing data overlay icon id to " + combinedActivityIconId.getIconId());
            }
            mLastDataDirectionOverlayIconId = combinedActivityIconId.getIconId();
            N = mDataDirectionOverlayIconViews.size();
            for (int i = 0; i < N; i++) {
                final ImageView v = mDataDirectionOverlayIconViews.get(i);
                if (combinedActivityIconId.getIconId() == 0) {
                    if (!mIsScreenLarge) {
                       v.setVisibility(View.INVISIBLE);
                    } else {
                       v.setVisibility(View.GONE);
                    }
                } else {
                    v.setVisibility(View.VISIBLE);
                    if (combinedActivityIconId.getResources() != null) {
                        v.setImageDrawable(combinedActivityIconId.getDrawable());
                    } else {
                        if (combinedActivityIconId.getIconId() == 0) {
                            v.setImageDrawable(null);
                        } else {
                            v.setImageResource(combinedActivityIconId.getIconId());
                        }
                    }
                    v.setContentDescription(tempContentDescriptionDataType);
                }
            }
        }

        // the combinedLabel in the notification panel
        if (!mLastCombinedLabel.equals(combinedLabel)) {
            mLastCombinedLabel = combinedLabel;
            N = mCombinedLabelViews.size();
            for (int i = 0; i < N; i++) {
                TextView v = mCombinedLabelViews.get(i);
                v.setText(combinedLabel);
            }
        }

        // wifi label
        N = mWifiLabelViews.size();
        for (int i = 0; i < N; i++) {
            TextView v = mWifiLabelViews.get(i);
            if ("".equals(wifiLabel)) {
                v.setVisibility(View.GONE);
            } else {
                v.setVisibility(View.VISIBLE);
                v.setText(wifiLabel);
            }
        }

        // mobile label
        if (!mIsScreenLarge) {
            N = mMobileLabelViews.size();
            for (int i = 0; i < N; i++) {
                TextView v = mMobileLabelViews.get(i);
                if ("".equals(mobileLabel)) {
                    v.setVisibility(View.GONE);
                } else {
                    v.setVisibility(View.VISIBLE);
                    v.setText(mobileLabel);
                }
            }
        } else {
            TextView v;
            if (slotId == PhoneConstants.GEMINI_SIM_1) {
                v = mMobileLabelViews.get(0);
            } else {
                v = mMobileLabelViews.get(1);
            }
            
            if (v != null) {
                if ("".equals(mobileLabel)) {
                    v.setVisibility(View.GONE);
                } else {
                    v.setVisibility(View.VISIBLE);
                    v.setText(mobileLabel);
                }
            }
        }

        // e-call label
        N = mEmergencyLabelViews.size();
        for (int i=0; i<N; i++) {
            TextView v = mEmergencyLabelViews.get(i);
            if (!emergencyOnly) {
                v.setVisibility(View.GONE);
            } else {
                v.setText(mobileLabel); // comes from the telephony stack
                v.setVisibility(View.VISIBLE);
            }
        }

    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("NetworkControllerGemini state:");
        pw.println(String.format("  %s network type %d (%s)", 
                mConnected ? "CONNECTED" : "DISCONNECTED",
                mConnectedNetworkType, mConnectedNetworkTypeName));
        pw.println("------ telephony ------");

        for (int i = 0 ; i < mGeminiSimNum ; i++) {
            pw.println(String.format("====== SlotId: %d ======", i));
            pw.print("  hasService =");
            pw.println(hasService(i));
            pw.print("  mDataConnected =");
            pw.println(mDataConnected[i]);
            pw.print("  mSimState =");
            pw.println(mSimState[i]);
            pw.print("  mDataState =");
            pw.println(mDataState[i]);
            pw.print("  mDataActivity =");
            pw.println(mDataActivity[i]);
            pw.print("  mDataNetType =");
            pw.print(mDataNetType[i]);
            pw.print("/");
            pw.println(TelephonyManager.getNetworkTypeName(mDataNetType[i]));
            pw.print("  mServiceState =");
            pw.println(mServiceState[i]);
            pw.print("  mSignalStrength =");
            pw.println(mSignalStrength[i]);
            pw.print("  mNetworkName =");
            pw.println(mNetworkName[i]);
            pw.print("  mPhoneSignalIconId =0x");
            pw.print(Integer.toHexString(mPhoneSignalIconId[i][0].getIconId()));
            pw.print("/");
            pw.println(getResourceName(mPhoneSignalIconId[i][0].getIconId()));
            pw.print("  mDataDirectionIconId =");
            pw.print(Integer.toHexString(mDataDirectionIconId[i]));
            pw.print("/");
            pw.println(getResourceName(mDataDirectionIconId[i]));
            pw.print("  mDataSignalIconId =");
            pw.print(Integer.toHexString(mDataSignalIconId[i].getIconId()));
            pw.print("/");
            pw.println(getResourceName(mDataSignalIconId[i].getIconId()));
            pw.print("  mDataTypeIconId =");
            pw.print(Integer.toHexString(mDataTypeIconId[i].getIconId()));
            pw.print("/");
            pw.println(getResourceName(mDataTypeIconId[i].getIconId()));

            pw.print("  mLastPhoneSignalIconId[0]=0x");
            pw.print(Integer.toHexString(mLastPhoneSignalIconId[i][0]));
            pw.print("/");
            pw.println(getResourceName(mLastPhoneSignalIconId[i][0]));
            pw.print("  mLastPhoneSignalIconId[1]=0x");
            pw.print(Integer.toHexString(mLastPhoneSignalIconId[i][1]));
            pw.print("/");
            pw.println(getResourceName(mLastPhoneSignalIconId[i][1]));

            pw.print("  mLastDataTypeIconId =0x");
            pw.print(Integer.toHexString(mLastDataTypeIconId[i]));
            pw.print("/");
            pw.println(getResourceName(mLastDataTypeIconId[i]));
        }

        pw.println("------ wifi ------");
        pw.print("  mWifiEnabled=");
        pw.println(mWifiEnabled);
        pw.print("  mWifiConnected=");
        pw.println(mWifiConnected);
        pw.print("  mWifiRssi=");
        pw.println(mWifiRssi);
        pw.print("  mWifiLevel=");
        pw.println(mWifiLevel);
        pw.print("  mWifiSsid=");
        pw.println(mWifiSsid);
        pw.println(String.format("  mWifiIconId=0x%08x/%s",
                    mWifiIconId, getResourceName(mWifiIconId)));
        pw.print("  mWifiActivity=");
        pw.println(mWifiActivity);

        if (mWimaxSupported) {
            pw.println("------ wimax ------");
            pw.print("  mIsWimaxEnabled=");
            pw.println(mIsWimaxEnabled);
            pw.print("  mWimaxConnected=");
            pw.println(mWimaxConnected);
            pw.print("  mWimaxIdle=");
            pw.println(mWimaxIdle);
            pw.println(String.format("  mWimaxIconId=0x%08x/%s",
                        mWimaxIconId, getResourceName(mWimaxIconId)));
            pw.println(String.format("  mWimaxSignal=%d", mWimaxSignal));
            pw.println(String.format("  mWimaxState=%d", mWimaxState));
            pw.println(String.format("  mWimaxExtraState=%d", mWimaxExtraState));
        }

        pw.println("------ Bluetooth ----");
        pw.print("  mBtReverseTethered=");
        pw.println(mBluetoothTethered);

        pw.println("------ connectivity ------");
        pw.print("  mInetCondition=");
        pw.println(mInetCondition);

        pw.println("------ icons ------");
        pw.print("  mLastDataDirectionIconId=0x");
        pw.print(Integer.toHexString(mLastDataDirectionIconId));
        pw.print("/");
        pw.println(getResourceName(mLastDataDirectionIconId));
        pw.print("  mLastDataDirectionOverlayIconId=0x");
        pw.print(Integer.toHexString(mLastDataDirectionOverlayIconId));
        pw.print("/");
        pw.println(getResourceName(mLastDataDirectionOverlayIconId));
        pw.print("  mLastWifiIconId=0x");
        pw.print(Integer.toHexString(mLastWifiIconId));
        pw.print("/");
        pw.println(getResourceName(mLastWifiIconId));
        pw.print("  mLastCombinedSignalIconId=0x");
        pw.print(Integer.toHexString(mLastCombinedSignalIconId));
        pw.print("/");
        pw.println(getResourceName(mLastCombinedSignalIconId));
        pw.print("  mLastCombinedLabel=");
        pw.print(mLastCombinedLabel);
        pw.println("");
    }

    private String getResourceName(int resId) {
        if (resId != 0) {
            final Resources res = mContext.getResources();
            try {
                return res.getResourceName(resId);
            } catch (android.content.res.Resources.NotFoundException ex) {
                return "(unknown)";
            }
        } else {
            return "(null)";
        }
    }
    private CarrierLabelGemini mCarrier1 = null;
    private CarrierLabelGemini mCarrier2 = null;
    private View mCarrierDivider = null;
    // Only for "Dual SIM".
    public void setCarrierGemini(CarrierLabelGemini carrier1, CarrierLabelGemini carrier2, View carrierDivider) {
        mCarrierList.clear();
        mCarrierList.add(carrier1);
        mCarrierList.add(carrier2);
        mDividerList.clear();
        mDividerList.add(carrierDivider);
    }

    public void setCarrierGemini(CarrierLabelGemini carrier1, CarrierLabelGemini carrier2, CarrierLabelGemini carrier3,
                                        View carrierDivider1, View carrierDivider2) {
        mCarrierList.clear();
        mCarrierList.add(carrier1);
        mCarrierList.add(carrier2);
        mCarrierList.add(carrier3);
        mDividerList.clear();
        mDividerList.add(carrierDivider1);
        mDividerList.add(carrierDivider2);
    }

    public void setCarrierGemini(CarrierLabelGemini carrier1, CarrierLabelGemini carrier2,
                                        CarrierLabelGemini carrier3, CarrierLabelGemini carrier4,
                                        View carrierDivider1, View carrierDivider2, View carrierDivider3) {
        mCarrierList.clear();
        mCarrierList.add(carrier1);
        mCarrierList.add(carrier2);
        mCarrierList.add(carrier3);
        mCarrierList.add(carrier4);
        mDividerList.clear();
        mDividerList.add(carrierDivider1);
        mDividerList.add(carrierDivider2);
        mDividerList.add(carrierDivider3);
    }

    /**
    * M: Used to check weather this device is wifi only.
    */
    private boolean isWifiOnlyDevice() {
        ConnectivityManager cm = (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        return  !(cm.isNetworkSupported(ConnectivityManager.TYPE_MOBILE));
    }

    /// M: Support GeminiPlus
    private ArrayList<CarrierLabelGemini> mCarrierList = new ArrayList<CarrierLabelGemini>();
    private ArrayList<View> mDividerList = new ArrayList<View>();

    public void updateOperatorInfo() {
        if(mCarrierList.size() <= 0 || mDividerList.size() <= 0) {
            return;
        }

        for (CarrierLabelGemini mCarrierMember : mCarrierList) {
            if(mCarrierMember == null) {
                return;
            }
        }

        for (View mDividerMemeber : mDividerList) {
            if (mDividerMemeber != null) {
                mDividerMemeber.setVisibility(View.GONE);
            }
        }

        if (isWifiOnlyDevice())
        {
            for (CarrierLabelGemini mCarrierMember : mCarrierList) {
                if (mCarrierMember != null) {
                    mCarrierMember.setVisibility(View.GONE);
                }
            }
            return;
        }

        int mNumOfSIM = 0;
        CarrierLabelGemini mCarrierLeft = null;
        CarrierLabelGemini mCarrierRight = null;
        for (int i = 0; i < mCarrierList.size(); i++) {
            final CarrierLabelGemini mCarrierMember = mCarrierList.get(i);
            if(mCarrierMember != null) {
                final boolean simInserted = SIMHelper.isSimInserted(mCarrierMember.getSlotId());

                Xlog.d(TAG, "updateOperatorInfo, simInserted is " + simInserted + ", SIM slod id is " + mCarrierMember.getSlotId() + ".");

                if(simInserted) {
                    mCarrierMember.setVisibility(View.VISIBLE);
                    mNumOfSIM++;
                    if(mNumOfSIM == 1) {
                        mCarrierLeft = mCarrierMember;
                    } else if(mNumOfSIM == 2) {
                        mCarrierRight = mCarrierMember;
                    }
                    if(mNumOfSIM >= 2 && ((i - 1) >= 0)) {
                        mDividerList.get(i-1).setVisibility(View.VISIBLE);
                    }
                } else {
                    mCarrierMember.setVisibility(View.GONE);
                }
                mCarrierMember.setGravity(Gravity.CENTER);
            }
        }

        if(mNumOfSIM == 2) {
            mCarrierLeft.setGravity(Gravity.RIGHT);
            mCarrierRight.setGravity(Gravity.LEFT);
        } else if(mNumOfSIM == 0) {
            final CarrierLabelGemini v = mCarrierList.get(0);
            if(v != null) {
                v.setVisibility(View.VISIBLE);
            }
            Xlog.d(TAG, "updateOperatorInfo, force the slotId 0 to visible.");
        }
    }

    /// M: [SystemUI] Support "SIM indicator". @{
    
    public void showSimIndicator(int slotId) {
        //set SimIndicatorFlag and refreshViews.
        int simColor = SIMHelper.getSIMColorIdBySlot(mContext, slotId);
        if (simColor > -1 && simColor < 4) {
            mSimIndicatorResId[slotId] = TelephonyIcons.SIM_INDICATOR_BACKGROUND[simColor];
        }
        Xlog.d(TAG,"showSimIndicator slotId is " + slotId + " simColor = " + simColor);
        mSimIndicatorFlag[slotId] = true;
        updateTelephonySignalStrength(slotId);
        updateDataNetType(slotId);
        updateDataIcon(slotId);
        refreshViews(slotId);
    }
    
    public void hideSimIndicator(int slotId) {
        //reset SimIndicatorFlag and refreshViews.
        Xlog.d(TAG,"hideSimIndicator slotId is " + slotId);
        mSimIndicatorFlag[slotId] = false;
        updateTelephonySignalStrength(slotId);
        updateDataNetType(slotId);
        updateDataIcon(slotId);
        refreshViews(slotId);
    }

    /// M: [SystemUI] Support "SIM indicator". }@
}
