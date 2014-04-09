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

package com.android.settings;

import static android.net.ConnectivityManager.TYPE_ETHERNET;
import static android.net.ConnectivityManager.TYPE_MOBILE;
import static android.net.ConnectivityManager.TYPE_WIFI;
import static android.net.ConnectivityManager.TYPE_WIMAX;
import static android.net.NetworkPolicy.LIMIT_DISABLED;
import static android.net.NetworkPolicy.WARNING_DISABLED;
import static android.net.NetworkPolicyManager.EXTRA_NETWORK_TEMPLATE;
import static android.net.NetworkPolicyManager.POLICY_NONE;
import static android.net.NetworkPolicyManager.POLICY_REJECT_METERED_BACKGROUND;
import static android.net.NetworkPolicyManager.computeLastCycleBoundary;
import static android.net.NetworkPolicyManager.computeNextCycleBoundary;
import static android.net.NetworkTemplate.MATCH_MOBILE_3G_LOWER;
import static android.net.NetworkTemplate.MATCH_MOBILE_4G;
import static android.net.NetworkTemplate.MATCH_MOBILE_ALL;
import static android.net.NetworkTemplate.MATCH_WIFI;
import static android.net.NetworkTemplate.buildTemplateEthernet;
import static android.net.NetworkTemplate.buildTemplateMobile3gLower;
import static android.net.NetworkTemplate.buildTemplateMobile4g;
import static android.net.NetworkTemplate.buildTemplateMobileAll;
import static android.net.NetworkTemplate.buildTemplateWifiWildcard;
import static android.net.TrafficStats.GB_IN_BYTES;
import static android.net.TrafficStats.MB_IN_BYTES;
import static android.net.TrafficStats.UID_REMOVED;
import static android.net.TrafficStats.UID_TETHERING;
import static android.telephony.TelephonyManager.SIM_STATE_READY;
import static android.text.format.DateUtils.FORMAT_ABBREV_MONTH;
import static android.text.format.DateUtils.FORMAT_SHOW_DATE;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.animation.LayoutTransition;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.INetworkStatsService;
import android.net.INetworkStatsSession;
import android.net.NetworkPolicy;
import android.net.NetworkPolicyManager;
import android.net.NetworkStats;
import android.net.NetworkStatsHistory;
import android.net.NetworkTemplate;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.INetworkManagementService;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.preference.PreferenceActivity;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.provider.Telephony.SIMInfo;
import android.provider.Telephony.SimInfo;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.format.Formatter;
import android.text.format.Time;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabContentFactory;
import android.widget.TabHost.TabSpec;
import android.widget.TabWidget;
import android.widget.TextView;

import static com.android.internal.util.Preconditions.checkNotNull;
import static com.android.settings.Utils.prepareCustomPreferencesList;

import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.settings.drawable.InsetBoundsDrawable;
import com.android.settings.net.ChartData;
import com.android.settings.net.ChartDataLoader;
import com.android.settings.net.DataUsageMeteredSettings;
import com.android.settings.net.NetworkPolicyEditor;
import com.android.settings.net.SummaryForAllUidLoader;
import com.android.settings.net.UidDetail;
import com.android.settings.net.UidDetailProvider;
import com.android.settings.widget.ChartDataUsageView;
import com.android.settings.widget.ChartDataUsageView.DataUsageChartListener;
import com.android.settings.widget.PieChartView;
import com.google.android.collect.Lists;

import com.mediatek.CellConnService.CellConnMgr;
import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.datausage.OverViewTabAdapter;
import com.mediatek.settings.ext.ISettingsMiscExt;
import com.mediatek.settings.ext.ISimRoamingExt;
import com.mediatek.telephony.TelephonyManagerEx;
import com.mediatek.xlog.Xlog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import libcore.util.Objects;

/**
 * Panel showing data usage history across various networks, including options
 * to inspect based on usage cycle and control through {@link NetworkPolicy}.
 */
public class DataUsageSummary extends Fragment {
    private static final String TAG = "DataUsage";
    private static final boolean LOGD = true;

    // TODO: remove this testing code
    private static final boolean TEST_ANIM = false;
    private static final boolean TEST_RADIOS = false;

    private static final String TEST_RADIOS_PROP = "test.radios";
    private static final String TEST_SUBSCRIBER_PROP = "test.subscriberid";

    private static final String TAB_3G = "3g";
    private static final String TAB_4G = "4g";
    private static final String TAB_MOBILE = "mobile";
    private static final String TAB_WIFI = "wifi";
    private static final String TAB_ETHERNET = "ethernet";
    private static final String TAB_SIM_1 = "sim1";
    private static final String TAB_SIM_2 = "sim2";
    private static final String TAB_SIM_3 = "sim3";

    private static final String TAG_CONFIRM_DATA_DISABLE = "confirmDataDisable";
    private static final String TAG_CONFIRM_DATA_ROAMING = "confirmDataRoaming";
    private static final String TAG_CONFIRM_LIMIT = "confirmLimit";
    private static final String TAG_CYCLE_EDITOR = "cycleEditor";
    private static final String TAG_WARNING_EDITOR = "warningEditor";
    private static final String TAG_LIMIT_EDITOR = "limitEditor";
    private static final String TAG_CONFIRM_RESTRICT = "confirmRestrict";
    private static final String TAG_DENIED_RESTRICT = "deniedRestrict";
    private static final String TAG_CONFIRM_APP_RESTRICT = "confirmAppRestrict";
    private static final String TAG_CONFIRM_AUTO_SYNC_CHANGE = "confirmAutoSyncChange";
    private static final String TAG_APP_DETAILS = "appDetails";
    /** M: CU spec for Modify data usage string @{ */
    private static final String TAG_BG_DATA_SWITCH = "bgDataSwitch";
    private static final String TAG_BG_DATA_SUMMARY = "bgDataSummary";
    private static final String TAG_BG_DATA_APP_DIALOG_TITLE = "bgDataDialogTitle";
    private static final String TAG_BG_DATA_APP_DIALOG_MESSAGE = "bgDataDialogMessage";
    private static final String TAG_BG_DATA_MENU_DIALOG_MESSAGE = "bgDataMenuDialogMessage";
    private static final String TAG_BG_DATA_RESTRICT_DENY_MESSAGE = "bgDataRestrictDenyMessage";
    /** @} */

    private static final int LOADER_CHART_DATA = 2;
    private static final int LOADER_SUMMARY = 3;

    /** M: set limit sweep and warning sweep max value,CR ALPS00325435 @{*/
    private static final int LIMIT_MAX_SIZE = 1022976; //999 * 1024
    private static final int WARNING_MAX_SIZE = 921600; //900 * 1024
    /** @} */

    private INetworkManagementService mNetworkService;
    private INetworkStatsService mStatsService;
    private NetworkPolicyManager mPolicyManager;
    private ConnectivityManager mConnService;

    private INetworkStatsSession mStatsSession;

    private static final String PREF_FILE = "data_usage";
    private static final String PREF_SHOW_WIFI = "show_wifi";
    private static final String PREF_SHOW_ETHERNET = "show_ethernet";

    private SharedPreferences mPrefs;

    private TabHost mTabHost;
    private ViewGroup mTabsContainer;
    private TabWidget mTabWidget;
    private ListView mListView;
    private DataUsageAdapter mAdapter;

    /** Distance to inset content from sides, when needed. */
    private int mInsetSide = 0;

    private ViewGroup mHeader;

    private ViewGroup mNetworkSwitchesContainer;
    private LinearLayout mNetworkSwitches;
    private Switch mDataEnabled;
    private View mDataEnabledView;

    // M: Add for Datausage Enhancement @{
    private Switch mLockScreenEnabled;
    private TextView mLockScreenPrefTitle;
    private View mShowOnLockScreenView;
    private ExpandableListView mOverViewExpList;
    private OverViewTabAdapter mOverviewAdapter;
    private static final String TAB_OVERVIEW = "Overview";
    private boolean mIsAirplaneModeOn;
    private boolean mIsLimitChangeToChecked = false;
    // @ }

    private CheckBox mDisableAtLimit;
    private View mDisableAtLimitView;

    private View mCycleView;
    private Spinner mCycleSpinner;
    private CycleAdapter mCycleAdapter;

    private ChartDataUsageView mChart;
    private TextView mUsageSummary;
    private TextView mEmpty;

    private View mAppDetail;
    private ImageView mAppIcon;
    private ViewGroup mAppTitles;
    private PieChartView mAppPieChart;
    private TextView mAppForeground;
    private TextView mAppBackground;
    private Button mAppSettings;

    private LinearLayout mAppSwitches;
    private CheckBox mAppRestrict;
    private View mAppRestrictView;

    private boolean mShowWifi = false;
    private boolean mShowEthernet = false;

    private NetworkTemplate mTemplate;
    private ChartData mChartData;

    private AppItem mCurrentApp = null;

    private Intent mAppSettingsIntent;

    private NetworkPolicyEditor mPolicyEditor;

    private String mCurrentTab = null;
    private String mIntentTab = null;

    private MenuItem mMenuDataRoaming;
    private MenuItem mMenuRestrictBackground;
    private MenuItem mMenuAutoSync;

    /** Flag used to ignore listeners during binding. */
    private boolean mBinding;

    private UidDetailProvider mUidDetailProvider;

    /** M: For Gemini phone @{ */
    private static final String ACTION_POLICYMGR_CREATED =
            "com.mediatek.server.action.ACTION_POLICY_CREATED";

    private ITelephony mITelephony;
    private TelephonyManagerEx mTelephonyManager;
    private IntentFilter mIntentFilter;    
    private boolean mIsUserEnabled = false;
    private String mSavedCurrentTab = null;
    private CellConnMgr mCellConnMgr;
    private CycleAdapter mCycleAdapterSim1;
    private CycleAdapter mCycleAdapterSim2;
    private CycleAdapter mCycleAdapterSim3;
    private CycleAdapter mCycleAdapterOther;
    private static boolean sIsSwitching = false;
    /** @} */
    /** M: time out message event @{ */
    private static final int EVENT_DETACH_TIME_OUT = 2000;
    private static final int EVENT_ATTACH_TIME_OUT = 2001;
    /** @} */
    /** M: time out length @{ */
    private static final int DETACH_TIME_OUT_LENGTH = 10000;
    private static final int ATTACH_TIME_OUT_LENGTH = 30000;
    /** @} */
    /** M: For none gemini phone,whether there is a simCard inserted */
    private boolean mHaveMobileSim = false;

    /**Fix me : this shall get from somewhere else.
    */
    private static final int PIN1_REQUEST_CODE = 302;

    private static boolean sIsWifiOnly = false;
    
    private static ISimRoamingExt sSimRoamingExt;
    /** M: CU spec for Modify data usage string @{ */
    private static ISettingsMiscExt sSettingsMiscExt;
    /** @} */
    /**
     * M: add a ContentObserver to update status when the Gprs connection SIM changed
     * for the gemini phone.
     */
    private ContentObserver mGprsDefaultSIMObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                Xlog.i(TAG, "Gprs connection SIM changed");
                mIsUserEnabled = false;
                mSavedCurrentTab =  mTabHost.getCurrentTabTag();
                updateBody();
            }
        };

    /**
     * M: add a ContentObserver to sync dataconnection status in dataUsage and title bar in
     *    none gemini mode,CR ALPS00336862
     */
    private ContentObserver mDataConnectionObserver = new ContentObserver(
            new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            Xlog.i(TAG, "Data connection state changed(none gemini mode)"
                    + mConnService.getMobileDataEnabled());
            updatePolicy(false);
        }
    };

    /**
     * M: add a ContentObserver to update status when airplane mode changed 
     * for the none gemini phone.
     */
    private ContentObserver mAirplaneObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                Xlog.i(TAG, "airplane mode changed"); 
                updateBody();
            }
        };

    /**
     * M: add a BroadcastReceiver to update status when the phone states changed 
     * for the gemini phone.
     */
    private BroadcastReceiver mSimReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            Xlog.d(TAG,"Receive broadcast , action =  " + action);
            
            boolean needUpdate = false;

            if (action.equals(TelephonyIntents.ACTION_SIM_INDICATOR_STATE_CHANGED)) {
                int slotId = intent.getIntExtra(TelephonyIntents.INTENT_KEY_ICC_SLOT, -1);
                int simStatus = intent.getIntExtra(TelephonyIntents.INTENT_KEY_ICC_STATE, -1);
                Xlog.i(TAG, "receive notification: state of sim slot " + slotId + " is " + simStatus);
                if ((slotId >= 0) && (simStatus >= 0)) {
                    needUpdate = true;
                }

            } else if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
               mIsAirplaneModeOn = isAirplaneModeOn(getActivity());
               needUpdate = true;
            } else if (action.equals(ACTION_POLICYMGR_CREATED)) {
                mPolicyEditor.read();
                if (mCycleAdapter != null) {
                    updatePolicy(true);
                }

            } else if (action.equals(TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED)) {
                String apnTypeList = intent.getStringExtra(PhoneConstants.DATA_APN_TYPE_KEY);
                PhoneConstants.DataState state;
                String str = intent.getStringExtra(PhoneConstants.STATE_KEY);                
                if (str != null) {
                    state = Enum.valueOf(PhoneConstants.DataState.class, str);
                } else {
                    state = PhoneConstants.DataState.DISCONNECTED;
                }
                int simId = intent.getIntExtra(PhoneConstants.GEMINI_SIM_ID_KEY, -1);

                if ((PhoneConstants.APN_TYPE_DEFAULT.equals(apnTypeList))
                        && (state == PhoneConstants.DataState.CONNECTED) && (sIsSwitching)) {
                    mTimerHandler.removeMessages(EVENT_ATTACH_TIME_OUT);
                    sIsSwitching = false;
                    mDataEnabled.setEnabled(true);
                    if (mCycleAdapter != null) {
                        updatePolicy(true);
                     }
                    Xlog.d(TAG,"attach over");
                }
                if ((PhoneConstants.APN_TYPE_DEFAULT.equals(apnTypeList))
                        && (state == PhoneConstants.DataState.DISCONNECTED) && (sIsSwitching)) {
                    mTimerHandler.removeMessages(EVENT_DETACH_TIME_OUT);
                    sIsSwitching = false;
                    mDataEnabled.setEnabled(true);
                    if (mCycleAdapter != null) {
                        updatePolicy(true);
                    }
                    Xlog.d(TAG,"detach over");
                }
            /** M: add for support sim hot plug in DataUsage @{ */
            } else if (action.equals(TelephonyIntents.ACTION_SIM_INFO_UPDATE)) {
                 mSavedCurrentTab =  mTabHost.getCurrentTabTag();
                 if (FeatureOption.MTK_GEMINI_SUPPORT) {
                     if (isRemainInserted(mSavedCurrentTab)) {
                       Xlog.d(TAG , "set mSavedCurrentTab null when sim hotswap for gemini");
                       mSavedCurrentTab =  null;
                    }
                 } else {
                    mHaveMobileSim = isSimInserted(PhoneConstants.GEMINI_SIM_1);        
                    if ((TAB_MOBILE.equals(mSavedCurrentTab) && !mHaveMobileSim)) {
                       Xlog.d(TAG , "set mSavedCurrentTab null when sim hotswap for non gemini");
                       mSavedCurrentTab =  null;
                    }
                }
                updateTabs();
                updateBody();
            }
            /** @} */
            if (needUpdate) {
                mSavedCurrentTab =  mTabHost.getCurrentTabTag();
                Xlog.d(TAG,"set mSavedCurrentTab = " + mSavedCurrentTab);
                updateBody();
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /** M: set the screen orientation according to the parent,to fix the histogram displayed
            problem when switching screen between portrait and landscape@{ */ 
        int orientation = getActivity().getResources().getConfiguration().orientation;
        int winOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT;
        Xlog.i(TAG,"current config orienation " + orientation);
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            winOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
        }
        getActivity().setRequestedOrientation(winOrientation);
        /** @} */ 

        final Context context = getActivity();

        mNetworkService = INetworkManagementService.Stub.asInterface(
                ServiceManager.getService(Context.NETWORKMANAGEMENT_SERVICE));
        mStatsService = INetworkStatsService.Stub.asInterface(
                ServiceManager.getService(Context.NETWORK_STATS_SERVICE));
        mPolicyManager = NetworkPolicyManager.from(context);
        mConnService = ConnectivityManager.from(context);

        mPrefs = getActivity().getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);

        /** M: get the current sim status and define the IntentFilter for the
               broadcastreceiver when the phone states changed in gemini mode
        @{ */
        mTelephonyManager = TelephonyManagerEx.getDefault();
        mITelephony = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            mIntentFilter = new IntentFilter(TelephonyIntents.ACTION_SIM_INDICATOR_STATE_CHANGED);
            mIntentFilter.addAction(TelephonyIntents.ACTION_SIM_INFO_UPDATE);
            mIntentFilter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
            mIntentFilter.addAction(ACTION_POLICYMGR_CREATED);
            mIntentFilter.addAction(TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED);
        } else {
             mIntentFilter = new IntentFilter(TelephonyIntents.ACTION_SIM_INFO_UPDATE);
             mHaveMobileSim = isSimInserted(PhoneConstants.GEMINI_SIM_1);
        }
        /** @} */

        /** M: Register the CellConnMgr to support the SIMLock issue @{ */
       
        mCellConnMgr = new CellConnMgr(null);
        mCellConnMgr.register(getActivity());
        /** @} */
        mPolicyEditor = new NetworkPolicyEditor(mPolicyManager);
        mPolicyEditor.read();

        mShowWifi = mPrefs.getBoolean(PREF_SHOW_WIFI, false);
        mShowEthernet = mPrefs.getBoolean(PREF_SHOW_ETHERNET, false);

        /** M: to support tablet bug fix,CR ALPS00252219@{ */
        if (Utils.isWifiOnly(getActivity())) {
            sIsWifiOnly = true;
        }
        /** @} */
        sSimRoamingExt = Utils.getSimRoamingExtPlugin(this.getActivity());
        sSettingsMiscExt = Utils.getMiscPlugin(this.getActivity());

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        Xlog.d(TAG,"onCreateView");
        final Context context = inflater.getContext();
        final View view = inflater.inflate(R.layout.data_usage_summary, container, false);

        mUidDetailProvider = new UidDetailProvider(context);

        try {
            mStatsSession = mStatsService.openSession();
            /** M: DataUsage Enhancement, new ExpandableListview adapter @{*/
            if (FeatureOption.MTK_DATAUSAGE_SUPPORT) {
                mOverviewAdapter = new OverViewTabAdapter(getActivity() , mStatsSession);
            }
            /** @} */
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }

        mTabHost = (TabHost) view.findViewById(android.R.id.tabhost);
        mTabsContainer = (ViewGroup) view.findViewById(R.id.tabs_container);
        mTabWidget = (TabWidget) view.findViewById(android.R.id.tabs);
        mListView = (ListView) view.findViewById(android.R.id.list);

        // decide if we need to manually inset our content, or if we should rely
        // on parent container for inset.
        final boolean shouldInset = mListView.getScrollBarStyle()
                == View.SCROLLBARS_OUTSIDE_OVERLAY;
        if (shouldInset) {
            mInsetSide = view.getResources().getDimensionPixelOffset(
                    com.android.internal.R.dimen.preference_fragment_padding_side);
        } else {
            mInsetSide = 0;
        }

        // adjust padding around tabwidget as needed
        prepareCustomPreferencesList(container, view, mListView, true);

        mTabHost.setup();
        mTabHost.setOnTabChangedListener(mTabListener);

        mHeader = (ViewGroup) inflater.inflate(R.layout.data_usage_header, mListView, false);
        mHeader.setClickable(true);

        /** M: DataUsage Enhancement, set ExpandableListview adapter @{*/
        if (FeatureOption.MTK_DATAUSAGE_SUPPORT) {
            mOverViewExpList = (ExpandableListView)
                    view.findViewById(R.id.overview_list);
            mOverViewExpList.setAdapter(mOverviewAdapter);
            //Expand the first category in default
            mOverViewExpList.expandGroup(0);
        }
        /** @} */
        mListView.addHeaderView(mHeader, null, true);
        mListView.setItemsCanFocus(true);

        /** M: Disable the listview's scroll fuction to fixe  Scroll bar does not work problem*/ 
        mListView.setVerticalScrollBarEnabled(false);
        if (mInsetSide > 0) {
            // inset selector and divider drawables
            insetListViewDrawables(mListView, mInsetSide);
            mHeader.setPadding(mInsetSide, 0, mInsetSide, 0);
        }

        {
            // bind network switches
            mNetworkSwitchesContainer = (ViewGroup) mHeader.findViewById(
                    R.id.network_switches_container);
            mNetworkSwitches = (LinearLayout) mHeader.findViewById(R.id.network_switches);

            mDataEnabled = new Switch(inflater.getContext());
            mDataEnabledView = inflatePreference(inflater, mNetworkSwitches, mDataEnabled);
            // M: set focus true to fix CR:ALPS00513259 {@
            mDataEnabledView.setClickable(true);
            mDataEnabledView.setFocusable(true);
            // @}
            mDataEnabled.setOnCheckedChangeListener(mDataEnabledListener);
            mNetworkSwitches.addView(mDataEnabledView);

            mDisableAtLimit = new CheckBox(inflater.getContext());
            mDisableAtLimit.setClickable(false);
            mDisableAtLimit.setFocusable(false);
            mDisableAtLimitView = inflatePreference(inflater, mNetworkSwitches, mDisableAtLimit);
            mDisableAtLimitView.setClickable(true);
            mDisableAtLimitView.setFocusable(true);
            mDisableAtLimitView.setOnClickListener(mDisableAtLimitListener);
            mNetworkSwitches.addView(mDisableAtLimitView);
        }

        /** M: DataUsage_Enhancement  Add lockScreen preference@{*/
        if (FeatureOption.MTK_DATAUSAGE_SUPPORT) {
            inflateLockScreenView(inflater);
        }
        /** @} */

        // bind cycle dropdown
        mCycleView = mHeader.findViewById(R.id.cycles);
        mCycleSpinner = (Spinner) mCycleView.findViewById(R.id.cycles_spinner);
        /** M: Set the usagecycle spinner's adapter to support gemini phone @{ */
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            mCycleAdapterSim1 = new CycleAdapter(context);
            mCycleAdapterSim2 = new CycleAdapter(context);
            if (FeatureOption.MTK_GEMINI_3SIM_SUPPORT) {
                mCycleAdapterSim3 = new CycleAdapter(context);
            }
            mCycleAdapterOther = new CycleAdapter(context);
        } else {
            mCycleAdapter = new CycleAdapter(context);
            mCycleSpinner.setAdapter(mCycleAdapter);
        }
        /** @} */
        mCycleSpinner.setOnItemSelectedListener(mCycleListener);

        mChart = (ChartDataUsageView) mHeader.findViewById(R.id.chart);
        mChart.setListener(mChartListener);
        mChart.bindNetworkPolicy(null);

        {
            // bind app detail controls
            mAppDetail = mHeader.findViewById(R.id.app_detail);
            mAppIcon = (ImageView) mAppDetail.findViewById(R.id.app_icon);
            mAppTitles = (ViewGroup) mAppDetail.findViewById(R.id.app_titles);
            mAppPieChart = (PieChartView) mAppDetail.findViewById(R.id.app_pie_chart);
            mAppForeground = (TextView) mAppDetail.findViewById(R.id.app_foreground);
            mAppBackground = (TextView) mAppDetail.findViewById(R.id.app_background);
            mAppSwitches = (LinearLayout) mAppDetail.findViewById(R.id.app_switches);

            mAppSettings = (Button) mAppDetail.findViewById(R.id.app_settings);
            mAppSettings.setOnClickListener(mAppSettingsListener);

            mAppRestrict = new CheckBox(inflater.getContext());
            mAppRestrict.setClickable(false);
            mAppRestrict.setFocusable(false);
            mAppRestrictView = inflatePreference(inflater, mAppSwitches, mAppRestrict);
            mAppRestrictView.setClickable(true);
            mAppRestrictView.setFocusable(true);
            mAppRestrictView.setOnClickListener(mAppRestrictListener);
            mAppSwitches.addView(mAppRestrictView);
        }

        mUsageSummary = (TextView) mHeader.findViewById(R.id.usage_summary);
        mEmpty = (TextView) mHeader.findViewById(android.R.id.empty);

        mAdapter = new DataUsageAdapter(mUidDetailProvider, mInsetSide);
        mListView.setOnItemClickListener(mListListener);
        mListView.setAdapter(mAdapter);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Xlog.d(TAG,"onResume");
        /** M: add for support sim hot plug in DataUsage , update SIMInfo when back into
            DataUsage*/
        mIsAirplaneModeOn = isAirplaneModeOn(getActivity());
        mIsUserEnabled = false;
        // pick default tab based on incoming intent
        final Intent intent = getActivity().getIntent();
        mIntentTab = computeTabFromIntent(intent);

        // this kicks off chain reaction which creates tabs, binds the body to
        // selected network, and binds chart, cycles and detail list.
        updateTabs();

        // kick off background task to update stats
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    // wait a few seconds before kicking off
                    Thread.sleep(2 * DateUtils.SECOND_IN_MILLIS);
                    mStatsService.forceUpdate();
                } catch (InterruptedException e) {
                } catch (RemoteException e) {
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                if (isAdded()) {
                    updateBody();
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        /** M: Register contentobserver and broadcastreceiver to deal with multiple 
            stutation when phone status changed. @{ */ 
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            getActivity().getContentResolver().registerContentObserver(
                    Settings.System.getUriFor(Settings.System.GPRS_CONNECTION_SIM_SETTING),false, mGprsDefaultSIMObserver);
        } else {
            getActivity().getContentResolver().registerContentObserver(
                    Settings.System.getUriFor(Settings.System.AIRPLANE_MODE_ON),false, mAirplaneObserver);
            /** M: Register ContentObserver to observe data connection status change,CR ALPS00336862 */
            getActivity().getContentResolver().registerContentObserver(
                    Settings.Secure.getUriFor(Settings.Global.MOBILE_DATA),false, mDataConnectionObserver);
        }
        getActivity().registerReceiver(mSimReceiver, mIntentFilter);
        /** @} */
    }

    /**
     * M: UnRegister the contentobserver and broadcastreceiver registered before.
     */
    @Override
    public void onPause() {
        Xlog.d(TAG,"onPause");
        super.onPause();
        mSavedCurrentTab =  mTabHost.getCurrentTabTag();
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            getActivity().getContentResolver().unregisterContentObserver(mGprsDefaultSIMObserver);
        } else {
            getActivity().getContentResolver().unregisterContentObserver(mAirplaneObserver);
            /** M: UnRegister the mDataConnectionObserver ContentObserver,CR ALPS00336862 */
            getActivity().getContentResolver().unregisterContentObserver(mDataConnectionObserver);
        }
        getActivity().unregisterReceiver(mSimReceiver);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.data_usage, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        Xlog.d(TAG, "onPrepareOptionsMenu()");
        final Context context = getActivity();
        final boolean appDetailMode = isAppDetailMode();
        final boolean isOwner = ActivityManager.getCurrentUser() == UserHandle.USER_OWNER;
        
        //** M: get the state of wifi radio and mobile radio @{
        final boolean hasWifiRadio = hasWifiRadio(context);
        final boolean hasReadyMobileRadio =  hasReadyMobileRadio(context);
        // @}
        
        /** M: add the MenuItem to support gemini phone's dataroaming @{ */
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            mMenuDataRoaming = menu.findItem(R.id.data_usage_menu_roaming);
            final String currentTab = mTabHost.getCurrentTabTag();
            //airplane mode on and sim radion off
            final boolean isSimNotReady = isSimStatusNotReady(currentTab);
            if (appDetailMode || isSimNotReady) {
                mMenuDataRoaming.setVisible(false);
            } else if (TAB_SIM_1.equals(currentTab) || TAB_SIM_2.equals(currentTab) ||
                    TAB_SIM_3.equals(currentTab)) {
                int slotId = getCurrentSlot(currentTab);
                final SIMInfo simInfo = SIMInfo.getSIMInfoBySlot(getActivity(), slotId);
                Xlog.d(TAG, "slotId : " + slotId + " simInfo : " + simInfo);
                if (simInfo != null) {
                    final String operatorName = simInfo.mDisplayName;
                    mMenuDataRoaming.setTitle(getString(
                                R.string.data_usage_menu_roaming) + " " + operatorName);
                    mMenuDataRoaming.setChecked(getDataRoaming(slotId));
                    mMenuDataRoaming.setVisible(true);
                } else {
                    //For ACTION_SIM_INFO_UPDATE have time delay,previous slot SIM info may
                    //be empty.
                    mMenuDataRoaming.setVisible(false);
                }
            } else {
                mMenuDataRoaming.setVisible(false);
            }
        /** @} */
        } else {
           if (mHaveMobileSim) {
                mMenuDataRoaming = menu.findItem(R.id.data_usage_menu_roaming);
                mMenuDataRoaming.setVisible(hasReadyMobileRadio && !appDetailMode);
                mMenuDataRoaming.setChecked(getDataRoaming());
            } else {
                menu.findItem(R.id.data_usage_menu_roaming).setVisible(false);
            }
        }

        mMenuRestrictBackground = menu.findItem(R.id.data_usage_menu_restrict_background);
        
        /** M: CU spec for Modify data usage string @{ */
        String menuBgDataSwitch = sSettingsMiscExt.getDataUsageBackgroundStrByTag(
                getString(R.string.data_usage_app_restrict_background), TAG_BG_DATA_SWITCH);
        mMenuRestrictBackground.setTitle(menuBgDataSwitch);
        /** @} */

        /** M: to support tablet bug fix,CR ALPS00252219@{ */
        if (!sIsWifiOnly) {
            mMenuRestrictBackground.setVisible(hasReadyMobileRadio && !appDetailMode);
            mMenuRestrictBackground.setChecked(mPolicyManager.getRestrictBackground());
            mMenuRestrictBackground.setVisible(isOwner);
        } else {
            mMenuRestrictBackground.setVisible(false);
        }
        /** @} */ 

        mMenuAutoSync = menu.findItem(R.id.data_usage_menu_auto_sync);
        mMenuAutoSync.setChecked(ContentResolver.getMasterSyncAutomatically());
        mMenuAutoSync.setVisible(!appDetailMode);

        final MenuItem split4g = menu.findItem(R.id.data_usage_menu_split_4g);
        split4g.setVisible(hasReadyMobile4gRadio(context) && isOwner && !appDetailMode);
        split4g.setChecked(isMobilePolicySplit());

        final MenuItem showWifi = menu.findItem(R.id.data_usage_menu_show_wifi);
        if (hasWifiRadio && hasReadyMobileRadio) {
            showWifi.setVisible(!appDetailMode);
            showWifi.setChecked(mShowWifi);
        } else {
            showWifi.setVisible(false);
        }

        final MenuItem showEthernet = menu.findItem(R.id.data_usage_menu_show_ethernet);
        if (hasEthernet(context) && hasReadyMobileRadio) {
            showEthernet.setVisible(!appDetailMode);
            showEthernet.setChecked(mShowEthernet);
        } else {
            showEthernet.setVisible(false);
        }

        final MenuItem metered = menu.findItem(R.id.data_usage_menu_metered);
        if (hasReadyMobileRadio || hasWifiRadio) {
            metered.setVisible(!appDetailMode);
        } else {
            metered.setVisible(false);
        }

        final MenuItem help = menu.findItem(R.id.data_usage_menu_help);
        String helpUrl;
        if (!TextUtils.isEmpty(helpUrl = getResources().getString(R.string.help_url_data_usage))) {
            HelpUtils.prepareHelpMenuItem(context, help, helpUrl);
        } else {
            help.setVisible(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.data_usage_menu_roaming: {
                final boolean dataRoaming = !item.isChecked();
                if (FeatureOption.MTK_GEMINI_SUPPORT) {
                     final String currentTab = mTabHost.getCurrentTabTag();
                     int slotId = getCurrentSlot(currentTab);
                    if (dataRoaming) {
                        ConfirmDataRoamingFragment.show(this, slotId);
                    } else {
                       // no confirmation to disable roaming
                       setDataRoaming(slotId, false);
                    }
                } else {
                    if (dataRoaming) {
                        ConfirmDataRoamingFragment.show(this);
                    } else {
                        // no confirmation to disable roaming  
                        setDataRoaming(false);
                    }
                }
                return true;
            }
            case R.id.data_usage_menu_restrict_background: {
                final boolean restrictBackground = !item.isChecked();
                if (restrictBackground) {
                    ConfirmRestrictFragment.show(this);
                } else {
                    // no confirmation to drop restriction
                    setRestrictBackground(false);
                }
                return true;
            }
            case R.id.data_usage_menu_split_4g: {
                final boolean mobileSplit = !item.isChecked();
                setMobilePolicySplit(mobileSplit);
                item.setChecked(isMobilePolicySplit());
                updateTabs();
                return true;
            }
            case R.id.data_usage_menu_show_wifi: {
                mShowWifi = !item.isChecked();
                mPrefs.edit().putBoolean(PREF_SHOW_WIFI, mShowWifi).apply();
                item.setChecked(mShowWifi);
                updateTabs();
                return true;
            }
            case R.id.data_usage_menu_show_ethernet: {
                mShowEthernet = !item.isChecked();
                mPrefs.edit().putBoolean(PREF_SHOW_ETHERNET, mShowEthernet).apply();
                item.setChecked(mShowEthernet);
                updateTabs();
                return true;
            }
            case R.id.data_usage_menu_metered: {
                final PreferenceActivity activity = (PreferenceActivity) getActivity();
                activity.startPreferencePanel(DataUsageMeteredSettings.class.getCanonicalName(), null,
                        R.string.data_usage_metered_title, null, this, 0);
                return true;
            }
            case R.id.data_usage_menu_auto_sync: {
                if (ActivityManager.isUserAMonkey()) {
                    Log.d("SyncState", "ignoring monkey's attempt to flip global sync state");
                } else {
                    ConfirmAutoSyncChangeFragment.show(this, !item.isChecked());
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public void onDestroy() {
        Xlog.d(TAG,"onDestory");
        mDataEnabledView = null;
        mDisableAtLimitView = null;

        mUidDetailProvider.clearCache();
        mUidDetailProvider = null;

        TrafficStats.closeQuietly(mStatsSession);

        if (this.isRemoving()) {
            getFragmentManager()
                    .popBackStack(TAG_APP_DETAILS, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }

        /** M: UnRegister the  CellConnMgr to support the SIMLock issue */
        mCellConnMgr.unregister();
        
        /** M: to support tablet UI bug fix,CR ALPS00245958 */ 
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        super.onDestroy();
    }

    /**
     * Build and assign {@link LayoutTransition} to various containers. Should
     * only be assigned after initial layout is complete.
     */
    private void ensureLayoutTransitions() {
        // skip when already setup
        if (mChart.getLayoutTransition() != null) return;

        mTabsContainer.setLayoutTransition(buildLayoutTransition());
        mHeader.setLayoutTransition(buildLayoutTransition());
        mNetworkSwitchesContainer.setLayoutTransition(buildLayoutTransition());

        final LayoutTransition chartTransition = buildLayoutTransition();
        chartTransition.disableTransitionType(LayoutTransition.APPEARING);
        chartTransition.disableTransitionType(LayoutTransition.DISAPPEARING);
        mChart.setLayoutTransition(chartTransition);
    }

    private static LayoutTransition buildLayoutTransition() {
        final LayoutTransition transition = new LayoutTransition();
        if (TEST_ANIM) {
            transition.setDuration(1500);
        }
        transition.setAnimateParentHierarchy(false);
        return transition;
    }

    /**
     * Rebuild all tabs based on {@link NetworkPolicyEditor} and
     * {@link #mShowWifi}, hiding the tabs entirely when applicable. Selects
     * first tab, and kicks off a full rebind of body contents.
     */
    private void updateTabs() {
        Xlog.d(TAG,"updateTabs()");
        final Context context = getActivity();
        mTabHost.clearAllTabs();

        Xlog.d(TAG,"clear All Tabs...");
        
        /// M: move from onCreate() to here for update UI , CR: ALPS00526064 {@
        // override preferences when no mobile radio
        if (!hasReadyMobileRadio(context)) {
            mShowWifi = hasWifiRadio(context);
            mShowEthernet = hasEthernet(context);
        }
        /// @}

        /** M: DataUsage Enhancement, Add OverViewTab @{*/
        if (FeatureOption.MTK_DATAUSAGE_SUPPORT) {
            mTabHost.addTab(buildTabSpec(TAB_OVERVIEW,
                    R.string.mtk_datausage_overview_tab));
            Xlog.d(TAG,"Add OVERVIEW TAB");
        }
        /** @} */
        final boolean mobileSplit = isMobilePolicySplit();
        if (mobileSplit && hasReadyMobile4gRadio(context)) {
            mTabHost.addTab(buildTabSpec(TAB_3G, R.string.data_usage_tab_3g));
            mTabHost.addTab(buildTabSpec(TAB_4G, R.string.data_usage_tab_4g));
        /** M: add datausage tab for gemini phone according to the siminfo @{ */
        } else if (FeatureOption.MTK_GEMINI_SUPPORT) {
            SIMInfo sim1Info = SIMInfo.getSIMInfoBySlot(getActivity(), PhoneConstants.GEMINI_SIM_1);
            if (sim1Info != null) {
                mTabHost.addTab(buildTabSpec(TAB_SIM_1, sim1Info.mDisplayName));
            }
            SIMInfo sim2Info = SIMInfo.getSIMInfoBySlot(getActivity(), PhoneConstants.GEMINI_SIM_2);
            if (sim2Info != null) {
                mTabHost.addTab(buildTabSpec(TAB_SIM_2, sim2Info.mDisplayName));
            }
            if (FeatureOption.MTK_GEMINI_3SIM_SUPPORT) {
                SIMInfo sim3Info = SIMInfo.getSIMInfoBySlot(getActivity(), PhoneConstants.GEMINI_SIM_3);
                if (sim3Info != null) {
                    mTabHost.addTab(buildTabSpec(TAB_SIM_3, sim3Info.mDisplayName));
                }
            }
        /** @} */
        /** M: add a flag to avoid show datausage info when no simcard inserted. */
        } else if (mHaveMobileSim && hasReadyMobileRadio(context)) {
            /** M: DataUsage Enhancement, Ste Tab name as operator name in order to
                keep consist with OverView Tab @{*/
            if (FeatureOption.MTK_DATAUSAGE_SUPPORT) {
                List<SIMInfo> simList = SIMInfo.getInsertedSIMList(getActivity());
                if (simList.size() > 0) {
                    mTabHost.addTab(buildTabSpec(TAB_MOBILE, simList.get(0).mDisplayName));
                }
            /** @} */
            } else {
               mTabHost.addTab(buildTabSpec(TAB_MOBILE, R.string.data_usage_tab_mobile));
            }
        }
        if (mShowWifi && hasWifiRadio(context)) {
            mTabHost.addTab(buildTabSpec(TAB_WIFI, R.string.data_usage_tab_wifi));
        }
        if (mShowEthernet && hasEthernet(context)) {
            mTabHost.addTab(buildTabSpec(TAB_ETHERNET, R.string.data_usage_tab_ethernet));
        }

        final boolean noTabs = mTabWidget.getTabCount() == 0;
        final boolean multipleTabs = mTabWidget.getTabCount() > 1;
        mTabWidget.setVisibility(multipleTabs ? View.VISIBLE : View.GONE);
        Xlog.d(TAG,"mIntentTab "  + mIntentTab + " mSavedCurrentTab " + mSavedCurrentTab);
        if (mIntentTab != null) {
            if (Objects.equal(mIntentTab, mTabHost.getCurrentTabTag())) {
                // already hit updateBody() when added; ignore
                updateBody();
            } else {
                Xlog.d(TAG,"set Intent tab ");
                mTabHost.setCurrentTabByTag(mIntentTab);
            }
            mIntentTab = null;
        } else if (mSavedCurrentTab != null) {
            Xlog.d(TAG,"saved curernt tabs " + mSavedCurrentTab + " ");
            if (!Objects.equal(mSavedCurrentTab, mTabHost.getCurrentTabTag())) {
                mTabHost.setCurrentTabByTag(mSavedCurrentTab);
            }
            mSavedCurrentTab = null;
            updateBody();
        } else if (noTabs) {
            // no usable tabs, so hide body
            updateBody();
        } else {
            // already hit updateBody() when added; ignore
        }
    }

    /**
     * Factory that provide empty {@link View} to make {@link TabHost} happy.
     */
    private TabContentFactory mEmptyTabContent = new TabContentFactory() {
        @Override
        public View createTabContent(String tag) {
            return new View(mTabHost.getContext());
        }
    };

    /**
     * Build {@link TabSpec} with thin indicator, and empty content.
     */
    private TabSpec buildTabSpec(String tag, int titleRes) {
        return mTabHost.newTabSpec(tag).setIndicator(getText(titleRes)).setContent(
                mEmptyTabContent);
    }

    private TabSpec buildTabSpec(String tag, String title) {
    /** M: CMCC spec for Modify data usage SIM name indicator @{ */
        TabSpec tab = mTabHost.newTabSpec(tag).setIndicator(title);
        TabSpec newTab = sSettingsMiscExt
                .DataUsageUpdateTabInfo(getActivity(), tag, tab, mTabWidget, title);
        newTab.setContent(mEmptyTabContent);
        return newTab;
    /** @} */
    }

    private OnTabChangeListener mTabListener = new OnTabChangeListener() {
        @Override
        public void onTabChanged(String tabId) {
            // user changed tab; update body
            Xlog.d(TAG ,"tab changed listener , start to update body");
            updateBody();
        }
    };

    /**
     * Update body content based on current tab. Loads
     * {@link NetworkStatsHistory} and {@link NetworkPolicy} from system, and
     * binds them to visible controls.
     */
    private void updateBody() {
        mBinding = true;
        if (!isAdded()) return;

        final Context context = getActivity();
        final String currentTab = mTabHost.getCurrentTabTag();
        final boolean isOwner = ActivityManager.getCurrentUser() == UserHandle.USER_OWNER;
        
        Xlog.d(TAG, "updateBody(), currentTab = " + currentTab
                + " , mSavedCurrentTab = " + mSavedCurrentTab
                + " , sIsSwitching = " + sIsSwitching);

        if (currentTab == null) {
            Log.w(TAG, "no tab selected; hiding body");
            mListView.setVisibility(View.GONE);
            return;
        } else if (FeatureOption.MTK_DATAUSAGE_SUPPORT && TAB_OVERVIEW.equals(currentTab)) {
            if (mSavedCurrentTab == null || mTabWidget.getTabCount() > 1) {
                mListView.setVisibility(View.GONE);
                mOverViewExpList.setVisibility(View.VISIBLE);
                mOverviewAdapter.updateAdapter();
                mOverviewAdapter.notifyDataSetChanged();
            }
            //Invalidate OptionsMenu
            getActivity().invalidateOptionsMenu();
            return;
        } else {
            if (FeatureOption.MTK_DATAUSAGE_SUPPORT) {
                mOverViewExpList.setVisibility(View.GONE);
                updateLockScreenViewVisibility(currentTab);
            }
            mListView.setVisibility(View.VISIBLE);
        }

        final boolean tabChanged = !currentTab.equals(mCurrentTab);
        mCurrentTab = currentTab;

        mDataEnabledView.setVisibility(isOwner ? View.VISIBLE : View.GONE);

        // TODO: remove mobile tabs when SIM isn't ready
        final TelephonyManager tele = TelephonyManager.from(context);

        /** M: to deal with the gemini phone stutation in body update  @{ */
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            mCycleAdapter = mCycleAdapterOther;
        }
        if (TAB_SIM_1.equals(currentTab)) {
            updateDataSwitchState(mCycleAdapterSim1, PhoneConstants.GEMINI_SIM_1);
        } else if (TAB_SIM_2.equals(currentTab)) {
            updateDataSwitchState(mCycleAdapterSim2, PhoneConstants.GEMINI_SIM_2);
        } else if (TAB_SIM_3.equals(currentTab)) {
            updateDataSwitchState(mCycleAdapterSim3, PhoneConstants.GEMINI_SIM_3);
        } else if (TAB_MOBILE.equals(currentTab)) {
            /** M: to deal with the airplane mode issue in none gemini stutation @{ */
            setPreferenceTitle(mDisableAtLimitView, R.string.data_usage_disable_mobile_limit);
            if (isAirplaneModeOn(getActivity())) {
                mDataEnabledView.setVisibility(View.GONE);
                mDisableAtLimitView.setVisibility(View.GONE);
            } else {
                setPreferenceTitle(mDataEnabledView, R.string.data_usage_enable_mobile);
            }
            /** @} */
            mTemplate = buildTemplateMobileAll(getActiveSubscriberId(context));

        } else if (TAB_3G.equals(currentTab)) {
            setPreferenceTitle(mDataEnabledView, R.string.data_usage_enable_3g);
            setPreferenceTitle(mDisableAtLimitView, R.string.data_usage_disable_3g_limit);
            // TODO: bind mDataEnabled to 3G radio state
            mTemplate = buildTemplateMobile3gLower(getActiveSubscriberId(context));

        } else if (TAB_4G.equals(currentTab)) {
            setPreferenceTitle(mDataEnabledView, R.string.data_usage_enable_4g);
            setPreferenceTitle(mDisableAtLimitView, R.string.data_usage_disable_4g_limit);
            // TODO: bind mDataEnabled to 4G radio state
            mTemplate = buildTemplateMobile4g(getActiveSubscriberId(context));

        } else if (TAB_WIFI.equals(currentTab)) {
            // wifi doesn't have any controls
            mDataEnabledView.setVisibility(View.GONE);
            mDisableAtLimitView.setVisibility(View.GONE);
            mTemplate = buildTemplateWifiWildcard();

        } else if (TAB_ETHERNET.equals(currentTab)) {
            // ethernet doesn't have any controls
            mDataEnabledView.setVisibility(View.GONE);
            mDisableAtLimitView.setVisibility(View.GONE);
            mTemplate = buildTemplateEthernet();

        } else {
            throw new IllegalStateException("unknown tab: " + currentTab);
        }
        mCycleSpinner.setAdapter(mCycleAdapter);
        // kick off loader for network history
        // TODO: consider chaining two loaders together instead of reloading
        // network history when showing app detail.
        getLoaderManager().restartLoader(LOADER_CHART_DATA,
                ChartDataLoader.buildArgs(mTemplate, mCurrentApp), mChartDataCallbacks);

        // detail mode can change visible menus, invalidate
        getActivity().invalidateOptionsMenu();

        mBinding = false;
    }
    /**
     * M: to support the airplane mode issue for none gemini phone issue
     */
    public boolean isAirplaneModeOn(Context context) {
        return Settings.System.getInt(context.getContentResolver(),  
            Settings.System.AIRPLANE_MODE_ON, 0) != 0; 
    }

    private boolean isAppDetailMode() {
        return mCurrentApp != null;
    }

    /**
     * Update UID details panels to match {@link #mCurrentApp}, showing or
     * hiding them depending on {@link #isAppDetailMode()}.
     */
    private void updateAppDetail() {
        Xlog.d(TAG,"updateAppDetail()");
        final Context context = getActivity();
        final PackageManager pm = context.getPackageManager();
        final LayoutInflater inflater = getActivity().getLayoutInflater();

        if (isAppDetailMode()) {
            mAppDetail.setVisibility(View.VISIBLE);
            mCycleAdapter.setChangeVisible(false);
        } else {
            mAppDetail.setVisibility(View.GONE);
            mCycleAdapter.setChangeVisible(true);

            // hide detail stats when not in detail mode
            mChart.bindDetailNetworkStats(null);
            return;
        }

        // remove warning/limit sweeps while in detail mode
        mChart.bindNetworkPolicy(null);

        // show icon and all labels appearing under this app
        final int uid = mCurrentApp.key;
        final UidDetail detail = mUidDetailProvider.getUidDetail(uid, true);
        mAppIcon.setImageDrawable(detail.icon);

        mAppTitles.removeAllViews();
        if (detail.detailLabels != null) {
            for (CharSequence label : detail.detailLabels) {
                mAppTitles.addView(inflateAppTitle(inflater, mAppTitles, label));
            }
        } else {
            mAppTitles.addView(inflateAppTitle(inflater, mAppTitles, detail.label));
        }

        // enable settings button when package provides it
        final String[] packageNames = pm.getPackagesForUid(uid);
        if (packageNames != null && packageNames.length > 0) {
            mAppSettingsIntent = new Intent(Intent.ACTION_MANAGE_NETWORK_USAGE);
            mAppSettingsIntent.addCategory(Intent.CATEGORY_DEFAULT);

            // Search for match across all packages
            boolean matchFound = false;
            for (String packageName : packageNames) {
                mAppSettingsIntent.setPackage(packageName);
                if (pm.resolveActivity(mAppSettingsIntent, 0) != null) {
                    matchFound = true;
                    break;
                }
            }

            mAppSettings.setEnabled(matchFound);
            mAppSettings.setVisibility(View.VISIBLE);

        } else {
            mAppSettingsIntent = null;
            mAppSettings.setVisibility(View.GONE);
        }

        updateDetailData();

        if (UserHandle.isApp(uid) && !mPolicyManager.getRestrictBackground()
                && isBandwidthControlEnabled() && hasReadyMobileRadio(context)) {

            /** M: CU spec for Modify data usage string @{ */
            String appBgDataSwitch = sSettingsMiscExt.getDataUsageBackgroundStrByTag(
                    getString(R.string.data_usage_app_restrict_background), TAG_BG_DATA_SWITCH);
            TextView title = (TextView) mAppRestrictView.findViewById(android.R.id.title);
            title.setText(appBgDataSwitch);
            String appBgDataSummary = sSettingsMiscExt.getDataUsageBackgroundStrByTag(
                    getString(R.string.data_usage_app_restrict_background_summary), TAG_BG_DATA_SUMMARY);
            setPreferenceSummary(mAppRestrictView, appBgDataSummary);
            /** @} */

            mAppRestrictView.setVisibility(View.VISIBLE);
            mAppRestrict.setChecked(getAppRestrictBackground());

        } else {
            mAppRestrictView.setVisibility(View.GONE);
        }
        Xlog.d(TAG,"updateAppDetail done");
    }

    private void setPolicyWarningBytes(long warningBytes) {
        if (LOGD) Log.d(TAG, "setPolicyWarningBytes() , warningBytes = " + warningBytes);
        mPolicyEditor.setPolicyWarningBytes(mTemplate, warningBytes);
        updatePolicy(false);
    }

    private void setPolicyLimitBytes(long limitBytes) {
        if (LOGD) Log.d(TAG, "setPolicyLimitBytes() , limitBytes = " + limitBytes);
        mPolicyEditor.setPolicyLimitBytes(mTemplate, limitBytes);
        updatePolicy(false);
    }

    /**
     * Local cache of value, used to work around delay when
     * {@link ConnectivityManager#setMobileDataEnabled(boolean)} is async.
     */
    private Boolean mMobileDataEnabled;

    private boolean isMobileDataEnabled() {
  /* M: keep the UI behavior with framework ,not user selected. For example,
        users turn on mobile data in DataUsage , then sweep limit over , over limit
        dialog pops up ,frameworks turn off mobile data , so this user fail to turn on
        UI show it's off. CR:ALPS00493059 {@
        if (mMobileDataEnabled != null) {
            // TODO: deprecate and remove this once enabled flag is on policy
            return mMobileDataEnabled;
        } else {*/
            Xlog.d(TAG, "no gemini mobile data state = "
                    + mConnService.getMobileDataEnabled());
            return mConnService.getMobileDataEnabled();
       /// @}
    }
    /**
     * M: overwrite the method to support the gemini phone
     */
    private boolean isMobileDataEnabled(int slotId) {
        boolean result = mConnService.getMobileDataEnabledGemini(slotId);
        Xlog.d(TAG,"isMoblieDataEnabled for slotId " + slotId + " " + result);
        return result;
    }

    private void setMobileDataEnabled(boolean enabled) {
        if (LOGD) Log.d(TAG, "setMobileDataEnabled()");
        mConnService.setMobileDataEnabled(enabled);
        updatePolicy(false);
    }

    /**
     * M: overwrite the method to support the gemini phone
     */
    private void setMobileDataEnabled(int slotId, boolean enabled) {
        Xlog.d(TAG,"setMobileDataEnabled for slotId " + slotId + " " + enabled);
        sIsSwitching = true; 
        if (enabled) { 
            mConnService.setMobileDataEnabledGemini(slotId);
            mTimerHandler.sendEmptyMessageDelayed(EVENT_ATTACH_TIME_OUT, ATTACH_TIME_OUT_LENGTH);
        } else {
            mConnService.setMobileDataEnabledGemini(SimInfo.SLOT_NONE);
            mTimerHandler.sendEmptyMessageDelayed(EVENT_DETACH_TIME_OUT, DETACH_TIME_OUT_LENGTH);
            mIsUserEnabled = false;
        }
        updatePolicy(false);
    }

    private boolean isNetworkPolicyModifiable(NetworkPolicy policy) {
        boolean isSimReady = !isSimStatusNotReady(mCurrentTab);
        Xlog.i(TAG,"isNetworkPolicyModifiable(), policy : " + policy + " , isSimReady " + isSimReady);
        return policy != null && isSimReady && isBandwidthControlEnabled() && mDataEnabled.isChecked()
                && ActivityManager.getCurrentUser() == UserHandle.USER_OWNER;
    }

    private boolean isBandwidthControlEnabled() {
        try {
            return mNetworkService.isBandwidthControlEnabled();
        } catch (RemoteException e) {
            Log.w(TAG, "problem talking with INetworkManagementService: " + e);
            return false;
        }
    }

    private boolean getDataRoaming() {
        final ContentResolver resolver = getActivity().getContentResolver();
        return Settings.Global.getInt(resolver, Settings.Global.DATA_ROAMING, 0) != 0;
    }

    /**
     * M: overwrite the method to support the gemini phone
     */
    private boolean getDataRoaming(int slotId) {
           SIMInfo siminfo = SIMInfo.getSIMInfoBySlot(getActivity(), slotId);
        Xlog.d(TAG,"get data Roaming for slotId = " + slotId + " ,result = " + siminfo.mDataRoaming);
        return siminfo.mDataRoaming == SimInfo.DATA_ROAMING_ENABLE;
    }
    
    private void setDataRoaming(boolean enabled) {
        // TODO: teach telephony DataConnectionTracker to watch and apply
        // updates when changed.
        final ContentResolver resolver = getActivity().getContentResolver();
        Settings.Global.putInt(resolver, Settings.Global.DATA_ROAMING, enabled ? 1 : 0);
        mMenuDataRoaming.setChecked(enabled);
    }

    /**
     * M: overwrite the method to support the gemini phone
     */
    private void setDataRoaming(int slotId, boolean enabled) {
       //mGeminiPhone.setDataRoamingEnabledGemini(enabled, slotId);
       Xlog.d(TAG,"set data Romaing for " + slotId + " result " + enabled);
       if (mITelephony != null) {
            try {
                   mITelephony.setDataRoamingEnabledGemini(enabled,slotId);
            } catch (RemoteException e) {
                Xlog.e(TAG,"data roaming setting remote exception");
            }
        } else {
            Xlog.e(TAG,"iTelephony is null , error !");
        }
        SIMInfo simInfo = SIMInfo.getSIMInfoBySlot(getActivity(), slotId);

        if (enabled) {
            SIMInfo.setDataRoaming(getActivity(),SimInfo.DATA_ROAMING_ENABLE,simInfo.mSimId);
        } else {
            SIMInfo.setDataRoaming(getActivity(),SimInfo.DATA_ROAMING_DISABLE, simInfo.mSimId);
        }
    }

    public void setRestrictBackground(boolean restrictBackground) {
        mPolicyManager.setRestrictBackground(restrictBackground);
        mMenuRestrictBackground.setChecked(restrictBackground);
    }

    private boolean getAppRestrictBackground() {
        final int uid = mCurrentApp.key;
        final int uidPolicy = mPolicyManager.getUidPolicy(uid);
        return (uidPolicy & POLICY_REJECT_METERED_BACKGROUND) != 0;
    }

    private void setAppRestrictBackground(boolean restrictBackground) {
        if (LOGD) Log.d(TAG, "setAppRestrictBackground()");
        final int uid = mCurrentApp.key;
        mPolicyManager.setUidPolicy(
                uid, restrictBackground ? POLICY_REJECT_METERED_BACKGROUND : POLICY_NONE);
        mAppRestrict.setChecked(restrictBackground);
    }

    /**
     * Update chart sweeps and cycle list to reflect {@link NetworkPolicy} for
     * current {@link #mTemplate}.
     */
    private void updatePolicy(boolean refreshCycle) {
        Xlog.d(TAG,"updatePolicy() , refreshCycle = " + refreshCycle
                + " , mIsUserEnabled = " + mIsUserEnabled);
        if (isAppDetailMode()) {
            mNetworkSwitches.setVisibility(View.GONE);
        } else {
            mNetworkSwitches.setVisibility(View.VISIBLE);
        }

        // TODO: move enabled state directly into policy
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            int simSlot = getCurrentSlot(mCurrentTab);
            mBinding = true;
            mDataEnabled.setChecked(isMobileDataEnabled(simSlot) || mIsUserEnabled);
            mBinding = false;
        } else if (TAB_MOBILE.equals(mCurrentTab)) {
            mBinding = true;
            mDataEnabled.setChecked(isMobileDataEnabled());
            mBinding = false;
        }        

        //M: must not call mPolicyEditor.read() in the inside of this function
        //M: because read() will clear mPolicies, will lead to CR: ALPS00532236
        
        final NetworkPolicy policy = mPolicyEditor.getPolicy(mTemplate);
        if (mDisableAtLimitView == null) {
           Xlog.i(TAG,"mDisableAtLimitView should not be null here !!!");
           return;
        }
        if (isNetworkPolicyModifiable(policy)) {
            Xlog.d(TAG,"network policy is modifiable, mobile data checkBox is on");
            mDisableAtLimitView.setVisibility(View.VISIBLE);
            mPolicyEditor.setPolicyActive(policy);
            mDisableAtLimit.setChecked(policy != null && policy.limitBytes != LIMIT_DISABLED);
            if (!isAppDetailMode()) {
                mChart.bindNetworkPolicy(policy);
            }

        } else {
            // controls are disabled; don't bind warning/limit sweeps
            Xlog.d(TAG,"network policy not modifiable, no warning limit/sweeps.");
            mDisableAtLimitView.setVisibility(View.GONE);
            mChart.bindNetworkPolicy(null);
        }

        /** M: update lockScreen state according to policy @{*/
        if (FeatureOption.MTK_DATAUSAGE_SUPPORT) {
            if (TAB_SIM_1.equals(mCurrentTab) || TAB_SIM_2.equals(mCurrentTab)
                    || TAB_MOBILE.equals(mCurrentTab)) {
                updateLockScreenViewState(mTemplate, mCurrentTab);
            }
        }
        /** @} */

        if (refreshCycle) {
            // generate cycle list based on policy and available history
            updateCycleList(policy);
        }
    }

    /**
     * Rebuild {@link #mCycleAdapter} based on {@link NetworkPolicy#cycleDay}
     * and available {@link NetworkStatsHistory} data. Always selects the newest
     * item, updating the inspection range on {@link #mChart}.
     */
    private void updateCycleList(NetworkPolicy policy) {
        // stash away currently selected cycle to try restoring below
        final CycleItem previousItem = (CycleItem) mCycleSpinner.getSelectedItem();
        mCycleAdapter.clear();

        final Context context = mCycleSpinner.getContext();

        long historyStart = Long.MAX_VALUE;
        long historyEnd = Long.MIN_VALUE;
        if (mChartData != null) {
            historyStart = mChartData.network.getStart();
            historyEnd = mChartData.network.getEnd();
        }

        final long now = System.currentTimeMillis();
        if (historyStart == Long.MAX_VALUE) historyStart = now;
        if (historyEnd == Long.MIN_VALUE) historyEnd = now + 1;

        boolean hasCycles = false;
        if (policy != null) {
            // find the next cycle boundary
            long cycleEnd = computeNextCycleBoundary(historyEnd, policy);

            // walk backwards, generating all valid cycle ranges
            while (cycleEnd > historyStart) {
                final long cycleStart = computeLastCycleBoundary(cycleEnd, policy);
                Log.d(TAG, "generating cs=" + cycleStart + " to ce=" + cycleEnd + " waiting for hs="
                        + historyStart);
                mCycleAdapter.add(new CycleItem(context, cycleStart, cycleEnd));
                cycleEnd = cycleStart;
                hasCycles = true;
            }

            // one last cycle entry to modify policy cycle day
            mCycleAdapter.setChangePossible(isNetworkPolicyModifiable(policy));
        }

        if (!hasCycles) {
            // no policy defined cycles; show entry for each four-week period
            long cycleEnd = historyEnd;
            while (cycleEnd > historyStart) {
                final long cycleStart = cycleEnd - (DateUtils.WEEK_IN_MILLIS * 4);
                mCycleAdapter.add(new CycleItem(context, cycleStart, cycleEnd));
                cycleEnd = cycleStart;
            }

            mCycleAdapter.setChangePossible(false);
        }

        // force pick the current cycle (first item)
        if (mCycleAdapter.getCount() > 0) {
            final int position = mCycleAdapter.findNearestPosition(previousItem);
            mCycleSpinner.setSelection(position);

            // only force-update cycle when changed; skipping preserves any
            // user-defined inspection region.
            final CycleItem selectedItem = mCycleAdapter.getItem(position);
            if (!Objects.equal(selectedItem, previousItem)) {
                mCycleListener.onItemSelected(mCycleSpinner, null, position, 0);
            } else {
                // but still kick off loader for detailed list
                updateDetailData();
            }
        } else {
            updateDetailData();
        }
    }

    /**
     * M: add the timehandler to deal with the detach and attach time out issue.
     */
    private Handler mTimerHandler = new Handler() { 
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_DETACH_TIME_OUT:
                case EVENT_ATTACH_TIME_OUT:
                    Xlog.d(TAG , "timer expired update switch enabled");
                    mDataEnabled.setEnabled(true);
                    sIsSwitching = false;
                    break;
                default :
                    break;
            }
        }
    };

    /**
     * M: add the method to deal with dataconnection changed event for gemini phone.
     */
    private void onDataEnableChangeGemini(boolean dataEnabled,int slotId) {
        if (isMobileDataEnabled(slotId) == dataEnabled) {
            return;
        }
        if (dataEnabled) {
            if (getSimIndicatorState(slotId) ==  PhoneConstants.SIM_INDICATOR_LOCKED) {
                mCellConnMgr.handleCellConn(slotId, PIN1_REQUEST_CODE);
                Xlog.d(TAG,"Data enable check change request pin");
                mDataEnabled.setChecked(false);
            } else {
                setMobileDataEnabled(slotId,true);
                mIsUserEnabled = true;
            }
        } else {
            ConfirmDataDisableFragment.show(DataUsageSummary.this,slotId);
        }
    }
    private OnCheckedChangeListener mDataEnabledListener = new OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (mBinding) return;

            final boolean dataEnabled = isChecked;
            final String currentTab = mCurrentTab;
            Xlog.d(TAG,"Data enable check change " + currentTab + " " + dataEnabled);

            if (FeatureOption.MTK_GEMINI_SUPPORT) {
                int simSlot = getCurrentSlot(mCurrentTab);
                onDataEnableChangeGemini(dataEnabled , simSlot);
            } else if (TAB_MOBILE.equals(currentTab)) {
                if (dataEnabled) {
                    /** M: add to deal with SIMLock issue @{ */
                    if (getSimIndicatorState(PhoneConstants.GEMINI_SIM_1) ==  PhoneConstants.SIM_INDICATOR_LOCKED) {
                        mCellConnMgr.handleCellConn(0, PIN1_REQUEST_CODE);
                        Xlog.d(TAG,"Data enable check change request pin single card");
                        mDataEnabled.setChecked(false);
                    /** @} */
                    } else {
                        setMobileDataEnabled(true);
                        if (isNeedtoShowRoamingMsg()) {
                            sSimRoamingExt.showDialog(DataUsageSummary.this.getActivity());
                        }
                    }
                } else {
                    // disabling data; show confirmation dialog which eventually
                    // calls setMobileDataEnabled() once user confirms.
                    ConfirmDataDisableFragment.show(DataUsageSummary.this);
                }
            }

            /** M: fix to deal with the problem that Mobile data label can not switch 
            from OFF to ON when Cancel Disable mobile data at the first use */ 
            updatePolicy(true);
        }
    };
    
    private boolean isNeedtoShowRoamingMsg() {
        TelephonyManager telMgr = (TelephonyManager) this.getActivity().getSystemService(Context.TELEPHONY_SERVICE);
        boolean isInRoaming = telMgr.isNetworkRoaming();
        boolean isRoamingEnabled = getDataRoaming();
        Xlog.d(TAG,"isInRoaming=" + isInRoaming + " isRoamingEnabled=" + isRoamingEnabled);

        return (isInRoaming && !isRoamingEnabled);
    }

    private View.OnClickListener mDisableAtLimitListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final boolean disableAtLimit = !mDisableAtLimit.isChecked();
            if (disableAtLimit) {
                // enabling limit; show confirmation dialog which eventually
                // calls setPolicyLimitBytes() once user confirms.
                ConfirmLimitFragment.show(DataUsageSummary.this);
            } else {
                setPolicyLimitBytes(LIMIT_DISABLED);
            }
        }
    };

    private View.OnClickListener mAppRestrictListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final boolean restrictBackground = !mAppRestrict.isChecked();

            if (restrictBackground) {
                // enabling restriction; show confirmation dialog which
                // eventually calls setRestrictBackground() once user
                // confirms.
                ConfirmAppRestrictFragment.show(DataUsageSummary.this);
            } else {
                setAppRestrictBackground(false);
            }
        }
    };

    private OnClickListener mAppSettingsListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!isAdded()) return;

            // TODO: target torwards entire UID instead of just first package
            startActivity(mAppSettingsIntent);
        }
    };

    private OnItemClickListener mListListener = new OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final Context context = view.getContext();
            final AppItem app = (AppItem) parent.getItemAtPosition(position);

            // TODO: sigh, remove this hack once we understand 6450986
            if (mUidDetailProvider == null || app == null) return;

            final UidDetail detail = mUidDetailProvider.getUidDetail(app.key, true);
            AppDetailsFragment.show(DataUsageSummary.this, app, detail.label);
        }
    };

    private OnItemSelectedListener mCycleListener = new OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            final CycleItem cycle = (CycleItem) parent.getItemAtPosition(position);
            if (cycle instanceof CycleChangeItem) {
                // show cycle editor; will eventually call setPolicyCycleDay()
                // when user finishes editing.
                CycleEditorFragment.show(DataUsageSummary.this);

                // reset spinner to something other than "change cycle..."
                mCycleSpinner.setSelection(0);

            } else {
                if (LOGD) {
                    Log.d(TAG, "showing cycle " + cycle + ", start=" + cycle.start + ", end="
                            + cycle.end + "]");
                }

                // update chart to show selected cycle, and update detail data
                // to match updated sweep bounds.
                mChart.setVisibleRange(cycle.start, cycle.end);

                updateDetailData();
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            // ignored
        }
    };

    /**
     * Update details based on {@link #mChart} inspection range depending on
     * current mode. In network mode, updates {@link #mAdapter} with sorted list
     * of applications data usage, and when {@link #isAppDetailMode()} update
     * app details.
     */
    private void updateDetailData() {
        if (LOGD) Log.d(TAG, "updateDetailData()");

        final long start = mChart.getInspectStart();
        final long end = mChart.getInspectEnd();
        final long now = System.currentTimeMillis();

        final Context context = getActivity();

        NetworkStatsHistory.Entry entry = null;
        if (isAppDetailMode() && mChartData != null && mChartData.detail != null) {
            // bind foreground/background to piechart and labels
            entry = mChartData.detailDefault.getValues(start, end, now, entry);
            final long defaultBytes = entry.rxBytes + entry.txBytes;
            entry = mChartData.detailForeground.getValues(start, end, now, entry);
            final long foregroundBytes = entry.rxBytes + entry.txBytes;

            mAppPieChart.setOriginAngle(175);

            mAppPieChart.removeAllSlices();
            mAppPieChart.addSlice(foregroundBytes, Color.parseColor("#d88d3a"));
            mAppPieChart.addSlice(defaultBytes, Color.parseColor("#666666"));

            mAppPieChart.generatePath();

            mAppBackground.setText(Formatter.formatFileSize(context, defaultBytes));
            mAppForeground.setText(Formatter.formatFileSize(context, foregroundBytes));

            // and finally leave with summary data for label below
            entry = mChartData.detail.getValues(start, end, now, null);

            getLoaderManager().destroyLoader(LOADER_SUMMARY);

        } else {
            if (mChartData != null) {
                entry = mChartData.network.getValues(start, end, now, null);
            }

            // kick off loader for detailed stats
            getLoaderManager().restartLoader(LOADER_SUMMARY,
                    SummaryForAllUidLoader.buildArgs(mTemplate, start, end), mSummaryCallbacks);
        }

        final long totalBytes = entry != null ? entry.rxBytes + entry.txBytes : 0;
        final String totalPhrase = Formatter.formatFileSize(context, totalBytes);
        final String rangePhrase = formatDateRange(context, start, end);

        final int summaryRes;
        if (TAB_MOBILE.equals(mCurrentTab) || TAB_3G.equals(mCurrentApp)
                || TAB_4G.equals(mCurrentApp)) {
            summaryRes = R.string.data_usage_total_during_range_mobile;
        } else {
            summaryRes = R.string.data_usage_total_during_range;
        }

        mUsageSummary.setText(getString(summaryRes, totalPhrase, rangePhrase));

        // initial layout is finished above, ensure we have transitions

        ensureLayoutTransitions();
    }

    private final LoaderCallbacks<ChartData> mChartDataCallbacks = new LoaderCallbacks<
            ChartData>() {
        @Override
        public Loader<ChartData> onCreateLoader(int id, Bundle args) {
            return new ChartDataLoader(getActivity(), mStatsSession, args);
        }

        @Override
        public void onLoadFinished(Loader<ChartData> loader, ChartData data) {
            Xlog.d(TAG, "ChartDataLoader finished ");
            mChartData = data;
            mChart.bindNetworkStats(mChartData.network);
            mChart.bindDetailNetworkStats(mChartData.detail);

            /// M: CR:ALPS00488860, the data limit dismiss after changing sim card {@
            mPolicyEditor.read();
            /// @}

            // calcuate policy cycles based on available data
            updatePolicy(true);
            updateAppDetail();

            // force scroll to top of body when showing detail
            /** M: add to avoid The screen stagnate in half sky after
             stop slide the screen ,CR ALPS00280253*/
            if (mChartData.detail != null && mListView.getScrollY() == 0) {
                mListView.smoothScrollToPosition(0);
            }
        }

        @Override
        public void onLoaderReset(Loader<ChartData> loader) {
            mChartData = null;
            mChart.bindNetworkStats(null);
            mChart.bindDetailNetworkStats(null);
        }
    };

    private final LoaderCallbacks<NetworkStats> mSummaryCallbacks = new LoaderCallbacks<
            NetworkStats>() {
        @Override
        public Loader<NetworkStats> onCreateLoader(int id, Bundle args) {
            return new SummaryForAllUidLoader(getActivity(), mStatsSession, args);
        }

        @Override
        public void onLoadFinished(Loader<NetworkStats> loader, NetworkStats data) {
            Xlog.d(TAG,"SummaryForAllUidLoader finished");
            final int[] restrictedUids = mPolicyManager.getUidsWithPolicy(
                    POLICY_REJECT_METERED_BACKGROUND);
            mAdapter.bindStats(data, restrictedUids);
            updateEmptyVisible();
        }

        @Override
        public void onLoaderReset(Loader<NetworkStats> loader) {
            mAdapter.bindStats(null, new int[0]);
            updateEmptyVisible();
        }

        private void updateEmptyVisible() {
            final boolean isEmpty = mAdapter.isEmpty() && !isAppDetailMode();
            mEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        }
    };

    @Deprecated
    private boolean isMobilePolicySplit() {
        final Context context = getActivity();
        if (hasReadyMobileRadio(context)) {
            final TelephonyManager tele = TelephonyManager.from(context);
            return mPolicyEditor.isMobilePolicySplit(getActiveSubscriberId(context));
        } else {
            return false;
        }
    }

    @Deprecated
    private void setMobilePolicySplit(boolean split) {
        final Context context = getActivity();
        if (hasReadyMobileRadio(context)) {
            final TelephonyManager tele = TelephonyManager.from(context);
            mPolicyEditor.setMobilePolicySplit(getActiveSubscriberId(context), split);
        }
    }

    private static String getActiveSubscriberId(Context context) {
        final TelephonyManager tele = TelephonyManager.from(context);
        final String actualSubscriberId = tele.getSubscriberId();
        return SystemProperties.get(TEST_SUBSCRIBER_PROP, actualSubscriberId);
    }

    /**
     * M: add the method to support the gemini phone
     */
    private String getSubscriberId(int slotId) {
        final TelephonyManagerEx teleEx = TelephonyManagerEx.getDefault();
        String imsiId = teleEx.getSubscriberId(slotId);
        Xlog.d(TAG, "getSubscriberId() slotId : " + slotId + "  imsiId : " + imsiId);
        return imsiId;
    }

    private DataUsageChartListener mChartListener = new DataUsageChartListener() {
        @Override
        public void onInspectRangeChanged() {
            if (LOGD) Log.d(TAG, "onInspectRangeChanged()");
            updateDetailData();
        }

        @Override
        public void onWarningChanged() {
            setPolicyWarningBytes(mChart.getWarningBytes());
        }

        @Override
        public void onLimitChanged() {
            // M: fix limit sweep to less than 1MB , but is not 0 , CR: ALPS00451837 {@
            long limitBytes = mChart.getLimitBytes();
            Xlog.d(TAG,"onLimitChanged(),limitBytes = " + limitBytes);
            if (limitBytes != 0 && limitBytes < MB_IN_BYTES) {
                Xlog.d(TAG,"set limitBytes = 0 , when it < 1MB && != 0");
                limitBytes = 0;
            }
            setPolicyLimitBytes(limitBytes);
            // @}
        }

        @Override
        public void requestWarningEdit() {
            WarningEditorFragment.show(DataUsageSummary.this);
        }

        @Override
        public void requestLimitEdit() {
            LimitEditorFragment.show(DataUsageSummary.this);
        }
    };

    /**
     * List item that reflects a specific data usage cycle.
     */
    public static class CycleItem implements Comparable<CycleItem> {
        public CharSequence label;
        public long start;
        public long end;

        CycleItem(CharSequence label) {
            this.label = label;
        }

        public CycleItem(Context context, long start, long end) {
            this.label = formatDateRange(context, start, end);
            this.start = start;
            this.end = end;
        }

        @Override
        public String toString() {
            return label.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof CycleItem) {
                final CycleItem another = (CycleItem) o;
                return start == another.start && end == another.end;
            }
            return false;
        }

        @Override
        public int compareTo(CycleItem another) {
            return Long.compare(start, another.start);
        }
    }

    private static final StringBuilder sBuilder = new StringBuilder(50);
    private static final java.util.Formatter sFormatter = new java.util.Formatter(
            sBuilder, Locale.getDefault());

    public static String formatDateRange(Context context, long start, long end) {
        final int flags = FORMAT_SHOW_DATE | FORMAT_ABBREV_MONTH;

        synchronized (sBuilder) {
            sBuilder.setLength(0);
            return DateUtils.formatDateRange(context, sFormatter, start, end, flags, null)
                    .toString();
        }
    }

    /**
     * Special-case data usage cycle that triggers dialog to change
     * {@link NetworkPolicy#cycleDay}.
     */
    public static class CycleChangeItem extends CycleItem {
        public CycleChangeItem(Context context) {
            super(context.getString(R.string.data_usage_change_cycle));
        }
    }

    public static class CycleAdapter extends ArrayAdapter<CycleItem> {
        private boolean mChangePossible = false;
        private boolean mChangeVisible = false;

        private final CycleChangeItem mChangeItem;

        public CycleAdapter(Context context) {
            super(context, android.R.layout.simple_spinner_item);
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mChangeItem = new CycleChangeItem(context);
        }

        public void setChangePossible(boolean possible) {
            mChangePossible = possible;
            updateChange();
        }

        public void setChangeVisible(boolean visible) {
            mChangeVisible = visible;
            updateChange();
        }

        private void updateChange() {
            remove(mChangeItem);
            if (mChangePossible && mChangeVisible) {
                add(mChangeItem);
            }
        }

        /**
         * Find position of {@link CycleItem} in this adapter which is nearest
         * the given {@link CycleItem}.
         */
        public int findNearestPosition(CycleItem target) {
            if (target != null) {
                final int count = getCount();
                for (int i = count - 1; i >= 0; i--) {
                    final CycleItem item = getItem(i);
                    if (item instanceof CycleChangeItem) {
                        continue;
                    } else if (item.compareTo(target) >= 0) {
                        return i;
                    }
                }
            }
            return 0;
        }
    }

    public static class AppItem implements Comparable<AppItem>, Parcelable {
        public final int key;
        public boolean restricted;
        public SparseBooleanArray uids = new SparseBooleanArray();
        public long total;

        public AppItem(int key) {
            this.key = key;
        }

        public AppItem(Parcel parcel) {
            key = parcel.readInt();
            uids = parcel.readSparseBooleanArray();
            total = parcel.readLong();
        }

        public void addUid(int uid) {
            uids.put(uid, true);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(key);
            dest.writeSparseBooleanArray(uids);
            dest.writeLong(total);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public int compareTo(AppItem another) {
            return Long.compare(another.total, total);
        }

        public static final Creator<AppItem> CREATOR = new Creator<AppItem>() {
            @Override
            public AppItem createFromParcel(Parcel in) {
                return new AppItem(in);
            }

            @Override
            public AppItem[] newArray(int size) {
                return new AppItem[size];
            }
        };
    }

    /**
     * Adapter of applications, sorted by total usage descending.
     */
    public static class DataUsageAdapter extends BaseAdapter {
        private final UidDetailProvider mProvider;
        private final int mInsetSide;

        private ArrayList<AppItem> mItems = Lists.newArrayList();
        private long mLargest;

        public DataUsageAdapter(UidDetailProvider provider, int insetSide) {
            mProvider = checkNotNull(provider);
            mInsetSide = insetSide;
        }

        /**
         * Bind the given {@link NetworkStats}, or {@code null} to clear list.
         */
        public void bindStats(NetworkStats stats, int[] restrictedUids) {
            mItems.clear();

            final int currentUserId = ActivityManager.getCurrentUser();
            final SparseArray<AppItem> knownItems = new SparseArray<AppItem>();

            NetworkStats.Entry entry = null;
            final int size = stats != null ? stats.size() : 0;
            for (int i = 0; i < size; i++) {
                entry = stats.getValues(i, entry);

                // Decide how to collapse items together
                final int uid = entry.uid;
                final int collapseKey;
                if (UserHandle.isApp(uid)) {
                    if (UserHandle.getUserId(uid) == currentUserId) {
                        collapseKey = uid;
                    } else {
                        collapseKey = UidDetailProvider.buildKeyForUser(UserHandle.getUserId(uid));
                    }
                } else if (uid == UID_REMOVED || uid == UID_TETHERING) {
                    collapseKey = uid;
                } else {
                    collapseKey = android.os.Process.SYSTEM_UID;
                }

                AppItem item = knownItems.get(collapseKey);
                if (item == null) {
                    item = new AppItem(collapseKey);
                    mItems.add(item);
                    knownItems.put(item.key, item);
                }
                item.addUid(uid);
                item.total += entry.rxBytes + entry.txBytes;
            }

            for (int uid : restrictedUids) {
                // Only splice in restricted state for current user
                if (UserHandle.getUserId(uid) != currentUserId) continue;

                AppItem item = knownItems.get(uid);
                if (item == null) {
                    item = new AppItem(uid);
                    item.total = -1;
                    mItems.add(item);
                    knownItems.put(item.key, item);
                }
                item.restricted = true;
            }

            Collections.sort(mItems);
            mLargest = (mItems.size() > 0) ? mItems.get(0).total : 0;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mItems.size();
        }

        @Override
        public Object getItem(int position) {
            return mItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return mItems.get(position).key;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.data_usage_item, parent, false);

                if (mInsetSide > 0) {
                    convertView.setPadding(mInsetSide, 0, mInsetSide, 0);
                }
            }

            final Context context = parent.getContext();

            final TextView text1 = (TextView) convertView.findViewById(android.R.id.text1);
            final ProgressBar progress = (ProgressBar) convertView.findViewById(
                    android.R.id.progress);

            // kick off async load of app details
            final AppItem item = mItems.get(position);
            UidDetailTask.bindView(mProvider, item, convertView);

            if (item.restricted && item.total <= 0) {
                text1.setText(R.string.data_usage_app_restricted);
                progress.setVisibility(View.GONE);
            } else {
                text1.setText(Formatter.formatFileSize(context, item.total));
                progress.setVisibility(View.VISIBLE);
            }

            final int percentTotal = mLargest != 0 ? (int) (item.total * 100 / mLargest) : 0;
            progress.setProgress(percentTotal);

            return convertView;
        }
    }

    /**
     * Empty {@link Fragment} that controls display of UID details in
     * {@link DataUsageSummary}.
     */
    public static class AppDetailsFragment extends Fragment {
        private static final String EXTRA_APP = "app";

        public static void show(DataUsageSummary parent, AppItem app, CharSequence label) {
            if (!parent.isAdded()) return;

            final Bundle args = new Bundle();
            args.putParcelable(EXTRA_APP, app);

            final AppDetailsFragment fragment = new AppDetailsFragment();
            fragment.setArguments(args);
            fragment.setTargetFragment(parent, 0);
            final FragmentTransaction ft = parent.getFragmentManager().beginTransaction();
            ft.add(fragment, TAG_APP_DETAILS);
            ft.addToBackStack(TAG_APP_DETAILS);
            ft.setBreadCrumbTitle(label);
            ft.commitAllowingStateLoss();
        }

        @Override
        public void onStart() {
            super.onStart();
            final DataUsageSummary target = (DataUsageSummary) getTargetFragment();
            target.mCurrentApp = getArguments().getParcelable(EXTRA_APP);
            Xlog.d(TAG,"AppDetailsFragment start ");
            target.updateBody();
        }

        @Override
        public void onStop() {
            super.onStop();
            final DataUsageSummary target = (DataUsageSummary) getTargetFragment();
            target.mCurrentApp = null;
            target.updateBody();
        }
    }

    /**
     * Dialog to request user confirmation before setting
     * {@link NetworkPolicy#limitBytes}.
     */
    public static class ConfirmLimitFragment extends DialogFragment {
        private static final String EXTRA_MESSAGE = "message";
        private static final String EXTRA_LIMIT_BYTES = "limitBytes";

        public static void show(DataUsageSummary parent) {
            if (!parent.isAdded()) return;

            final Resources res = parent.getResources();
            final CharSequence message;
            final long minLimitBytes = (long) (
                    parent.mPolicyEditor.getPolicy(parent.mTemplate).warningBytes * 1.2f);
            final long limitBytes;

            // TODO: customize default limits based on network template
            final String currentTab = parent.mCurrentTab;
            if (TAB_3G.equals(currentTab)) {
                message = res.getString(R.string.data_usage_limit_dialog_mobile);
                limitBytes = Math.max(5 * GB_IN_BYTES, minLimitBytes);
            } else if (TAB_4G.equals(currentTab)) {
                message = res.getString(R.string.data_usage_limit_dialog_mobile);
                limitBytes = Math.max(5 * GB_IN_BYTES, minLimitBytes);
            /** M: add to support gemini sim @{ */
            } else if (TAB_SIM_1.equals(currentTab) ||
                       TAB_SIM_2.equals(currentTab) ||
                       TAB_SIM_3.equals(currentTab)) {
                message = res.getString(R.string.data_usage_limit_dialog_mobile);
                limitBytes = Math.max(5 * GB_IN_BYTES, minLimitBytes);
            /** @} */
            } else if (TAB_MOBILE.equals(currentTab)) {
                message = res.getString(R.string.data_usage_limit_dialog_mobile);
                limitBytes = Math.max(5 * GB_IN_BYTES, minLimitBytes);
            } else {
                throw new IllegalArgumentException("unknown current tab: " + currentTab);
            }

            final Bundle args = new Bundle();
            args.putCharSequence(EXTRA_MESSAGE, message);
            args.putLong(EXTRA_LIMIT_BYTES, limitBytes);

            final ConfirmLimitFragment dialog = new ConfirmLimitFragment();
            dialog.setArguments(args);
            dialog.setTargetFragment(parent, 0);
            dialog.show(parent.getFragmentManager(), TAG_CONFIRM_LIMIT);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();

            final CharSequence message = getArguments().getCharSequence(EXTRA_MESSAGE);
            final long limitBytes = getArguments().getLong(EXTRA_LIMIT_BYTES);

            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(R.string.data_usage_limit_dialog_title);
            builder.setMessage(message);

            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    final DataUsageSummary target = (DataUsageSummary) getTargetFragment();
                    if (target != null) {
                        /** M:DataUsage Enhancement, A flag to identify user check on the limit value@{*/
                        if (FeatureOption.MTK_DATAUSAGE_SUPPORT) {
                            target.mIsLimitChangeToChecked = true;
                        }
                        /** @} */
                        target.setPolicyLimitBytes(limitBytes);
                    }
                }
            });

            return builder.create();
        }
    }

    /**
     * Dialog to edit {@link NetworkPolicy#cycleDay}.
     */
    public static class CycleEditorFragment extends DialogFragment {
        private static final String EXTRA_TEMPLATE = "template";

        public static void show(DataUsageSummary parent) {
            if (!parent.isAdded()) return;

            final Bundle args = new Bundle();
            args.putParcelable(EXTRA_TEMPLATE, parent.mTemplate);

            final CycleEditorFragment dialog = new CycleEditorFragment();
            dialog.setArguments(args);
            dialog.setTargetFragment(parent, 0);
            dialog.show(parent.getFragmentManager(), TAG_CYCLE_EDITOR);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();
            final DataUsageSummary target = (DataUsageSummary) getTargetFragment();
            final NetworkPolicyEditor editor = target.mPolicyEditor;

            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            final LayoutInflater dialogInflater = LayoutInflater.from(builder.getContext());

            final View view = dialogInflater.inflate(R.layout.data_usage_cycle_editor, null, false);
            final NumberPicker cycleDayPicker = (NumberPicker) view.findViewById(R.id.cycle_day);

            final NetworkTemplate template = getArguments().getParcelable(EXTRA_TEMPLATE);
            final int cycleDay = editor.getPolicyCycleDay(template);

            cycleDayPicker.setMinValue(1);
            cycleDayPicker.setMaxValue(31);
            cycleDayPicker.setValue(cycleDay);
            cycleDayPicker.setWrapSelectorWheel(true);

            builder.setTitle(R.string.data_usage_cycle_editor_title);
            builder.setView(view);

            builder.setPositiveButton(R.string.data_usage_cycle_editor_positive,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // clear focus to finish pending text edits
                            cycleDayPicker.clearFocus();

                            final int cycleDay = cycleDayPicker.getValue();
                            final String cycleTimezone = new Time().timezone;
                            editor.setPolicyCycleDay(template, cycleDay, cycleTimezone);
                            target.updatePolicy(true);
                        }
                    });

            return builder.create();
        }
    }

    /**
     * Dialog to edit {@link NetworkPolicy#warningBytes}.
     */
    public static class WarningEditorFragment extends DialogFragment {
        private static final String EXTRA_TEMPLATE = "template";

        public static void show(DataUsageSummary parent) {
            if (!parent.isAdded()) return;

            final Bundle args = new Bundle();
            args.putParcelable(EXTRA_TEMPLATE, parent.mTemplate);

            final WarningEditorFragment dialog = new WarningEditorFragment();
            dialog.setArguments(args);
            dialog.setTargetFragment(parent, 0);
            dialog.show(parent.getFragmentManager(), TAG_WARNING_EDITOR);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();
            final DataUsageSummary target = (DataUsageSummary) getTargetFragment();
            final NetworkPolicyEditor editor = target.mPolicyEditor;

            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            final LayoutInflater dialogInflater = LayoutInflater.from(builder.getContext());

            final View view = dialogInflater.inflate(R.layout.data_usage_bytes_editor, null, false);
            final NumberPicker bytesPicker = (NumberPicker) view.findViewById(R.id.bytes);

            final NetworkTemplate template = getArguments().getParcelable(EXTRA_TEMPLATE);
            final long warningBytes = editor.getPolicyWarningBytes(template);
            final long limitBytes = editor.getPolicyLimitBytes(template);

            bytesPicker.setMinValue(0);
            if (limitBytes != LIMIT_DISABLED) {
                /// M: fix CR:ALPS00456298 , when sweep warning and limit at the same time if their previous
                ///   value is 0 {@
                if (limitBytes == 0) {
                    bytesPicker.setMaxValue(0);
                } else {
                    bytesPicker.setMaxValue((int) (limitBytes / MB_IN_BYTES) - 1);
                }
                /// @}
            } else {
                /** M: set limit sweep and warning sweep max value,CR ALPS00325435*/ 
                bytesPicker.setMaxValue(WARNING_MAX_SIZE);
            }
            bytesPicker.setValue((int) (warningBytes / MB_IN_BYTES));
            bytesPicker.setWrapSelectorWheel(false);

            builder.setTitle(R.string.data_usage_warning_editor_title);
            builder.setView(view);

            builder.setPositiveButton(R.string.data_usage_cycle_editor_positive,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // clear focus to finish pending text edits
                            bytesPicker.clearFocus();

                            final long bytes = bytesPicker.getValue() * MB_IN_BYTES;
                            editor.setPolicyWarningBytes(template, bytes);
                            target.updatePolicy(false);
                        }
                    });

            return builder.create();
        }
    }

    /**
     * Dialog to edit {@link NetworkPolicy#limitBytes}.
     */
    public static class LimitEditorFragment extends DialogFragment {
        private static final String EXTRA_TEMPLATE = "template";

        public static void show(DataUsageSummary parent) {
            if (!parent.isAdded()) return;

            final Bundle args = new Bundle();
            args.putParcelable(EXTRA_TEMPLATE, parent.mTemplate);

            final LimitEditorFragment dialog = new LimitEditorFragment();
            dialog.setArguments(args);
            dialog.setTargetFragment(parent, 0);
            dialog.show(parent.getFragmentManager(), TAG_LIMIT_EDITOR);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();
            final DataUsageSummary target = (DataUsageSummary) getTargetFragment();
            final NetworkPolicyEditor editor = target.mPolicyEditor;

            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            final LayoutInflater dialogInflater = LayoutInflater.from(builder.getContext());

            final View view = dialogInflater.inflate(R.layout.data_usage_bytes_editor, null, false);
            final NumberPicker bytesPicker = (NumberPicker) view.findViewById(R.id.bytes);

            final NetworkTemplate template = getArguments().getParcelable(EXTRA_TEMPLATE);
            final long warningBytes = editor.getPolicyWarningBytes(template);
            final long limitBytes = editor.getPolicyLimitBytes(template);
            
            /** M: set limit sweep and warning sweep max value,CR ALPS00325435*/
            bytesPicker.setMaxValue(LIMIT_MAX_SIZE);
            if (warningBytes != WARNING_DISABLED && limitBytes > 0) {
                bytesPicker.setMinValue((int) (warningBytes / MB_IN_BYTES) + 1);
            } else {
                bytesPicker.setMinValue(0);
            }
            bytesPicker.setValue((int) (limitBytes / MB_IN_BYTES));
            bytesPicker.setWrapSelectorWheel(false);

            builder.setTitle(R.string.data_usage_limit_editor_title);
            builder.setView(view);

            builder.setPositiveButton(R.string.data_usage_cycle_editor_positive,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // clear focus to finish pending text edits
                            bytesPicker.clearFocus();

                            final long bytes = bytesPicker.getValue() * MB_IN_BYTES;
                            editor.setPolicyLimitBytes(template, bytes);
                            target.updatePolicy(false);
                        }
                    });

            return builder.create();
        }
    }
    /**
     * Dialog to request user confirmation before disabling data.
     */
    public static class ConfirmDataDisableFragment extends DialogFragment {
        /** M: overwrite the method to support gemini phone @{ */
        public static void show(DataUsageSummary parent) {
            show(parent , -1);
        }
        public static void show(DataUsageSummary parent,int slotId) {
            if (!parent.isAdded()) return;

            final ConfirmDataDisableFragment dialog = new ConfirmDataDisableFragment();
            dialog.setTargetFragment(parent, slotId);
            dialog.show(parent.getFragmentManager(), TAG_CONFIRM_DATA_DISABLE);
        }
        /** @} */

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();

            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage(R.string.data_usage_disable_mobile);

            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    final DataUsageSummary target = (DataUsageSummary) getTargetFragment();
                    /** M: add to support gemini phone @{ */
                    int slotId = (int)getTargetRequestCode();
                    if (target != null) {
                        // TODO: extend to modify policy enabled flag.
                        if (slotId != SimInfo.SLOT_NONE) {
                            target.setMobileDataEnabled(slotId,false);
                        } else {
                            target.setMobileDataEnabled(false);
                        }
                    /** @} */
                    }
                }
            });
            builder.setNegativeButton(android.R.string.cancel, null);

            return builder.create();
        }
    }

    /**
     * Dialog to request user confirmation before setting
     * {@link android.provider.Settings.Global#DATA_ROAMING}.
     */
    public static class ConfirmDataRoamingFragment extends DialogFragment {
        /** M: overwrite the method to support gemini phone @{ */
        public static void show(DataUsageSummary parent) {
            show(parent,-1);
        }
        public static void show(DataUsageSummary parent,int slotId) {
            if (!parent.isAdded()) return;

            final ConfirmDataRoamingFragment dialog = new ConfirmDataRoamingFragment();
            dialog.setTargetFragment(parent, slotId);
            dialog.show(parent.getFragmentManager(), TAG_CONFIRM_DATA_ROAMING);
        }
        /** @} */

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();

            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            String msg = sSimRoamingExt.getRoamingWarningMsg(this.getActivity(),R.string.roaming_warning);
            builder.setTitle(R.string.roaming_reenable_title);
            if (Utils.hasMultipleUsers(context)) {
                builder.setMessage(R.string.roaming_warning_multiuser);
            } else {
                builder.setMessage(msg);
            }

            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    final DataUsageSummary target = (DataUsageSummary) getTargetFragment();
                    /** M: add to support gemini phone @{ */
                    int simId = getTargetRequestCode();
                    if (target != null) {
                        if (simId != -1) {
                            target.setDataRoaming(simId,true);
                        } else {
                            target.setDataRoaming(true);
                        }
                    }
                    /** @} */
                }
            });
            builder.setNegativeButton(android.R.string.cancel, null);

            return builder.create();
        }
    }

    /**
     * Dialog to request user confirmation before setting
     * {@link INetworkPolicyManager#setRestrictBackground(boolean)}.
     */
    public static class ConfirmRestrictFragment extends DialogFragment {
        public static void show(DataUsageSummary parent) {
            if (!parent.isAdded()) return;

            final ConfirmRestrictFragment dialog = new ConfirmRestrictFragment();
            dialog.setTargetFragment(parent, 0);
            dialog.show(parent.getFragmentManager(), TAG_CONFIRM_RESTRICT);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();

            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            /** M: CU spec for Modify data usage string @{ */
            String title = sSettingsMiscExt.getDataUsageBackgroundStrByTag(
                    getString(R.string.data_usage_restrict_background_title), TAG_BG_DATA_APP_DIALOG_TITLE);
            builder.setTitle(title);

            String message = sSettingsMiscExt.getDataUsageBackgroundStrByTag(
                    getString(R.string.data_usage_restrict_background), TAG_BG_DATA_MENU_DIALOG_MESSAGE);
            if (Utils.hasMultipleUsers(context)) {
                builder.setMessage(R.string.data_usage_restrict_background_multiuser);
            } else {
                builder.setMessage(message);
            }
            /** @} */
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    final DataUsageSummary target = (DataUsageSummary) getTargetFragment();
                    if (target != null) {
                        target.setRestrictBackground(true);
                    }
                }
            });
            builder.setNegativeButton(android.R.string.cancel, null);

            return builder.create();
        }
    }

    /**
     * Dialog to inform user that {@link #POLICY_REJECT_METERED_BACKGROUND}
     * change has been denied, usually based on
     * {@link DataUsageSummary#hasLimitedNetworks()}.
     */
    public static class DeniedRestrictFragment extends DialogFragment {
        public static void show(DataUsageSummary parent) {
            if (!parent.isAdded()) return;

            final DeniedRestrictFragment dialog = new DeniedRestrictFragment();
            dialog.setTargetFragment(parent, 0);
            dialog.show(parent.getFragmentManager(), TAG_DENIED_RESTRICT);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();

            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            /** M: CU spec for Modify data usage string @{ */
            String deniedRestrictBgDataTitle = sSettingsMiscExt.getDataUsageBackgroundStrByTag(
                    getString(R.string.data_usage_app_restrict_background), TAG_BG_DATA_SWITCH);
            builder.setTitle(deniedRestrictBgDataTitle);

            String deniedRestrictBgDataMessage = sSettingsMiscExt.getDataUsageBackgroundStrByTag(
                    getString(R.string.data_usage_restrict_denied_dialog), TAG_BG_DATA_RESTRICT_DENY_MESSAGE);
            builder.setMessage(deniedRestrictBgDataMessage);
            /** @} */
            builder.setPositiveButton(android.R.string.ok, null);

            return builder.create();
        }
    }

    /**
     * Dialog to request user confirmation before setting
     * {@link #POLICY_REJECT_METERED_BACKGROUND}.
     */
    public static class ConfirmAppRestrictFragment extends DialogFragment {
        public static void show(DataUsageSummary parent) {
            if (!parent.isAdded()) return;

            final ConfirmAppRestrictFragment dialog = new ConfirmAppRestrictFragment();
            dialog.setTargetFragment(parent, 0);
            dialog.show(parent.getFragmentManager(), TAG_CONFIRM_APP_RESTRICT);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();

            final AlertDialog.Builder builder = new AlertDialog.Builder(context);

            /** M: CU spec for Modify data usage string @{ */
            String dialogTitle = sSettingsMiscExt.getDataUsageBackgroundStrByTag(
                    getString(R.string.data_usage_app_restrict_dialog_title), TAG_BG_DATA_APP_DIALOG_TITLE);
            String dialogMessage = sSettingsMiscExt.getDataUsageBackgroundStrByTag(
                    getString(R.string.data_usage_app_restrict_dialog), TAG_BG_DATA_APP_DIALOG_MESSAGE);

            builder.setTitle(dialogTitle);
            builder.setMessage(dialogMessage);
            /** @} */

            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    final DataUsageSummary target = (DataUsageSummary) getTargetFragment();
                    if (target != null) {
                        target.setAppRestrictBackground(true);
                    }
                }
            });
            builder.setNegativeButton(android.R.string.cancel, null);

            return builder.create();
        }
    }

    /**
     * Dialog to inform user about changing auto-sync setting
     */
    public static class ConfirmAutoSyncChangeFragment extends DialogFragment {
        private static final String SAVE_ENABLING = "enabling";
        private boolean mEnabling;

        public static void show(DataUsageSummary parent, boolean enabling) {
            if (!parent.isAdded()) return;

            final ConfirmAutoSyncChangeFragment dialog = new ConfirmAutoSyncChangeFragment();
            dialog.mEnabling = enabling;
            dialog.setTargetFragment(parent, 0);
            dialog.show(parent.getFragmentManager(), TAG_CONFIRM_AUTO_SYNC_CHANGE);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();
            if (savedInstanceState != null) {
                mEnabling = savedInstanceState.getBoolean(SAVE_ENABLING);
            }

            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            if (!mEnabling) {
                builder.setTitle(R.string.data_usage_auto_sync_off_dialog_title);
                builder.setMessage(R.string.data_usage_auto_sync_off_dialog);
            } else {
                builder.setTitle(R.string.data_usage_auto_sync_on_dialog_title);
                builder.setMessage(R.string.data_usage_auto_sync_on_dialog);
            }

            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ContentResolver.setMasterSyncAutomatically(mEnabling);
                }
            });
            builder.setNegativeButton(android.R.string.cancel, null);

            return builder.create();
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putBoolean(SAVE_ENABLING, mEnabling);
        }
    }

    /**
     * Compute default tab that should be selected, based on
     * {@link NetworkPolicyManager#EXTRA_NETWORK_TEMPLATE} extra.
     */
    private String computeTabFromIntent(Intent intent) {
        final NetworkTemplate template = intent.getParcelableExtra(EXTRA_NETWORK_TEMPLATE);
        if (template == null) return null;

        switch (template.getMatchRule()) {
            case MATCH_MOBILE_3G_LOWER:
                return TAB_3G;
            case MATCH_MOBILE_4G:
                return TAB_4G;
            case MATCH_MOBILE_ALL:
                /** M: add to support gemini phone for compute tab from intent  @{ */
                if (FeatureOption.MTK_GEMINI_SUPPORT) {
                    String subscriber = template.getSubscriberId();
                    Xlog.d(TAG,"computeTabFromIntent, subscriber " + subscriber);
                    String targetTab  =  TAB_SIM_1;
                    if (subscriber == null) {
                        Xlog.e(TAG,"the subscriber error , null!");
                        targetTab =  TAB_SIM_1;
                    } else {
                        if (subscriber.equals(getSubscriberId(PhoneConstants.GEMINI_SIM_1))) {
                            targetTab =  TAB_SIM_1;
                        } else if (subscriber.equals(getSubscriberId(PhoneConstants.GEMINI_SIM_2))) {
                            targetTab =  TAB_SIM_2;
                        } else if (FeatureOption.MTK_GEMINI_3SIM_SUPPORT && 
                                subscriber.equals(getSubscriberId(PhoneConstants.GEMINI_SIM_3))) {
                            targetTab =  TAB_SIM_3;
                        } else {
                            Xlog.e(TAG,"the subscriber error , no matching!");
                            targetTab =  TAB_SIM_1;
                        }
                      }
                      return targetTab;
                /** @} */
                } else {
                    return TAB_MOBILE;
                }
            case MATCH_WIFI:
                return TAB_WIFI;
            default:
                return null;
        }
    }

    /**
     * Background task that loads {@link UidDetail}, binding to
     * {@link DataUsageAdapter} row item when finished.
     */
    private static class UidDetailTask extends AsyncTask<Void, Void, UidDetail> {
        private final UidDetailProvider mProvider;
        private final AppItem mItem;
        private final View mTarget;

        private UidDetailTask(UidDetailProvider provider, AppItem item, View target) {
            mProvider = checkNotNull(provider);
            mItem = checkNotNull(item);
            mTarget = checkNotNull(target);
        }

        public static void bindView(
                UidDetailProvider provider, AppItem item, View target) {
            final UidDetailTask existing = (UidDetailTask) target.getTag();
            if (existing != null) {
                existing.cancel(false);
            }

            final UidDetail cachedDetail = provider.getUidDetail(item.key, false);
            if (cachedDetail != null) {
                bindView(cachedDetail, target);
            } else {
                target.setTag(new UidDetailTask(provider, item, target).executeOnExecutor(
                        AsyncTask.THREAD_POOL_EXECUTOR));
            }
        }

        private static void bindView(UidDetail detail, View target) {
            final ImageView icon = (ImageView) target.findViewById(android.R.id.icon);
            final TextView title = (TextView) target.findViewById(android.R.id.title);

            if (detail != null) {
                icon.setImageDrawable(detail.icon);
                title.setText(detail.label);
            } else {
                icon.setImageDrawable(null);
                title.setText(null);
            }
        }

        @Override
        protected void onPreExecute() {
            bindView(null, mTarget);
        }

        @Override
        protected UidDetail doInBackground(Void... params) {
            return mProvider.getUidDetail(mItem.key, true);
        }

        @Override
        protected void onPostExecute(UidDetail result) {
            bindView(result, mTarget);
        }
    }

    /**
     * Test if device has a mobile data radio with SIM in ready state.
     */
    public static boolean hasReadyMobileRadio(Context context) {
        if (TEST_RADIOS) {
            return SystemProperties.get(TEST_RADIOS_PROP).contains("mobile");
        }

        final ConnectivityManager conn = ConnectivityManager.from(context);

        boolean isSimStateReady = false;
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            TelephonyManagerEx teleEx = TelephonyManagerEx.getDefault();
            boolean isSim1StateReady = (teleEx.getSimState(PhoneConstants.GEMINI_SIM_1) == SIM_STATE_READY);
            boolean isSim2StateReady = (teleEx.getSimState(PhoneConstants.GEMINI_SIM_2) == SIM_STATE_READY);
            if (FeatureOption.MTK_GEMINI_3SIM_SUPPORT) {
                boolean isSim3StateReady = (teleEx.getSimState(PhoneConstants.GEMINI_SIM_3) == SIM_STATE_READY);
                isSimStateReady = isSim1StateReady || isSim2StateReady || isSim3StateReady;
            } else {
                isSimStateReady = isSim1StateReady || isSim2StateReady;
            }

        } else {
            final TelephonyManager tele = TelephonyManager.from(context);
            isSimStateReady = (tele.getSimState() == SIM_STATE_READY);
        }

        // require both supported network and ready SIM
        return conn.isNetworkSupported(TYPE_MOBILE) && isSimStateReady;
    }

    /**
     * Test if device has a mobile 4G data radio.
     */
    public static boolean hasReadyMobile4gRadio(Context context) {
        if (!NetworkPolicyEditor.ENABLE_SPLIT_POLICIES) {
            return false;
        }
        if (TEST_RADIOS) {
            return SystemProperties.get(TEST_RADIOS_PROP).contains("4g");
        }

        final ConnectivityManager conn = ConnectivityManager.from(context);
        final TelephonyManager tele = TelephonyManager.from(context);

        final boolean hasWimax = conn.isNetworkSupported(TYPE_WIMAX);
        final boolean hasLte = (tele.getLteOnCdmaMode() == PhoneConstants.LTE_ON_CDMA_TRUE)
                && hasReadyMobileRadio(context);
        return hasWimax || hasLte;
    }

    /**
     * Test if device has a Wi-Fi data radio.
     */
    public static boolean hasWifiRadio(Context context) {
        if (TEST_RADIOS) {
            return SystemProperties.get(TEST_RADIOS_PROP).contains("wifi");
        }

        final ConnectivityManager conn = ConnectivityManager.from(context);
        return conn.isNetworkSupported(TYPE_WIFI);
    }

    /**
     * Test if device has an ethernet network connection.
     */
    public boolean hasEthernet(Context context) {
        if (TEST_RADIOS) {
            return SystemProperties.get(TEST_RADIOS_PROP).contains("ethernet");
        }

        final ConnectivityManager conn = ConnectivityManager.from(context);
        final boolean hasEthernet = conn.isNetworkSupported(TYPE_ETHERNET);

        final long ethernetBytes;
        if (mStatsSession != null) {
            try {
                ethernetBytes = mStatsSession.getSummaryForNetwork(
                        NetworkTemplate.buildTemplateEthernet(), Long.MIN_VALUE, Long.MAX_VALUE)
                        .getTotalBytes();
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        } else {
            ethernetBytes = 0;
        }

        // only show ethernet when both hardware present and traffic has occurred
        return hasEthernet && ethernetBytes > 0;
    }

    /**
     * Inflate a {@link Preference} style layout, adding the given {@link View}
     * widget into {@link android.R.id#widget_frame}.
     */
    private static View inflatePreference(LayoutInflater inflater, ViewGroup root, View widget) {
        final View view = inflater.inflate(R.layout.preference, root, false);
        final LinearLayout widgetFrame = (LinearLayout) view.findViewById(
                android.R.id.widget_frame);
        widgetFrame.addView(widget, new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
        return view;
    }

    private static View inflateAppTitle(
            LayoutInflater inflater, ViewGroup root, CharSequence label) {
        final TextView view = (TextView) inflater.inflate(
                R.layout.data_usage_app_title, root, false);
        view.setText(label);
        return view;
    }

    /**
     * Test if any networks are currently limited.
     */
    private boolean hasLimitedNetworks() {
        return !buildLimitedNetworksList().isEmpty();
    }

    /**
     * Build string describing currently limited networks, which defines when
     * background data is restricted.
     */
    @Deprecated
    private CharSequence buildLimitedNetworksString() {
        final List<CharSequence> limited = buildLimitedNetworksList();

        // handle case where no networks limited
        if (limited.isEmpty()) {
            limited.add(getText(R.string.data_usage_list_none));
        }

        return TextUtils.join(limited);
    }

    /**
     * Build list of currently limited networks, which defines when background
     * data is restricted.
     */
    @Deprecated
    private List<CharSequence> buildLimitedNetworksList() {
        final Context context = getActivity();

        // build combined list of all limited networks
        final ArrayList<CharSequence> limited = Lists.newArrayList();

        final TelephonyManager tele = TelephonyManager.from(context);
        if (tele.getSimState() == SIM_STATE_READY) {
            final String subscriberId = getActiveSubscriberId(context);

           /** M: add to support gemini phone limited networks list@{ */
           if (FeatureOption.MTK_GEMINI_SUPPORT) {
               if (isSimInserted(PhoneConstants.GEMINI_SIM_1) &&
                       hasLimitedPolicy(PhoneConstants.GEMINI_SIM_1)) {
                   limited.add(getText(R.string.data_usage_list_mobile));
               }
               if (isSimInserted(PhoneConstants.GEMINI_SIM_2) &&
                       hasLimitedPolicy(PhoneConstants.GEMINI_SIM_2)) {
                   limited.add(getText(R.string.data_usage_list_mobile));
               }
               if (FeatureOption.MTK_GEMINI_3SIM_SUPPORT) {
                   if (isSimInserted(PhoneConstants.GEMINI_SIM_3) &&
                           hasLimitedPolicy(PhoneConstants.GEMINI_SIM_3)) {
                       limited.add(getText(R.string.data_usage_list_mobile));
                   }
               }
            /** @} */
            } else if (mPolicyEditor.hasLimitedPolicy(buildTemplateMobileAll(subscriberId))) {
                limited.add(getText(R.string.data_usage_list_mobile));
            }
            if (mPolicyEditor.hasLimitedPolicy(buildTemplateMobile3gLower(subscriberId))) {
                limited.add(getText(R.string.data_usage_tab_3g));
            }
            if (mPolicyEditor.hasLimitedPolicy(buildTemplateMobile4g(subscriberId))) {
                limited.add(getText(R.string.data_usage_tab_4g));
            }
        }

        if (mPolicyEditor.hasLimitedPolicy(buildTemplateWifiWildcard())) {
            limited.add(getText(R.string.data_usage_tab_wifi));
        }
        if (mPolicyEditor.hasLimitedPolicy(buildTemplateEthernet())) {
            limited.add(getText(R.string.data_usage_tab_ethernet));
        }

        return limited;
    }

    /**
     * Inset both selector and divider {@link Drawable} on the given
     * {@link ListView} by the requested dimensions.
     */
    private static void insetListViewDrawables(ListView view, int insetSide) {
        final Drawable selector = view.getSelector();
        final Drawable divider = view.getDivider();

        // fully unregister these drawables so callbacks can be maintained after
        // wrapping below.
        final Drawable stub = new ColorDrawable(Color.TRANSPARENT);
        view.setSelector(stub);
        view.setDivider(stub);

        view.setSelector(new InsetBoundsDrawable(selector, insetSide));
        view.setDivider(new InsetBoundsDrawable(divider, insetSide));
    }

    /**
     * Set {@link android.R.id#title} for a preference view inflated with
     * {@link #inflatePreference(LayoutInflater, ViewGroup, View)}.
     */
    private static void setPreferenceTitle(View parent, int resId) {
        final TextView title = (TextView) parent.findViewById(android.R.id.title);
        title.setText(resId);
    }

    /**
     * Set {@link android.R.id#summary} for a preference view inflated with
     * {@link #inflatePreference(LayoutInflater, ViewGroup, View)}.
     */
    private static void setPreferenceSummary(View parent, CharSequence string) {
        final TextView summary = (TextView) parent.findViewById(android.R.id.summary);
        summary.setVisibility(View.VISIBLE);
        summary.setText(string);
    }

    private int getSimIndicatorState(int slotId) {
        int simIndicatorState = PhoneConstants.SIM_INDICATOR_UNKNOWN;
        try {
            if (mITelephony != null) {
                simIndicatorState = FeatureOption.MTK_GEMINI_SUPPORT ?
                                       (mITelephony.getSimIndicatorStateGemini(slotId))
                                      : (mITelephony.getSimIndicatorState());
            }
        } catch (RemoteException e) {
            Xlog.e(TAG, "RemoteException");
        } catch (NullPointerException ex) {
            Xlog.e(TAG, "NullPointerException");
        }
        Xlog.d(TAG,"getSimIndicator() slotId = " + slotId 
               + " ,simIndicatorState = " + simIndicatorState);
        return simIndicatorState;
    }

     /**
     * M: DataUsage_Enhancement_fuction checkMtkLockScreenApkExist()
     *    Judge whether MTK lockScreen apk is installed
     */
    private boolean checkMtkLockScreenApkExist() {
        boolean isApkExist = false;
        Context context = getActivity();
        String packageName = "com.mediatek.DataUsageLockScreenClient";
        try {
            ApplicationInfo info = context.getPackageManager()
                    .getApplicationInfo(packageName ,
                            PackageManager.GET_UNINSTALLED_PACKAGES);
            isApkExist = true;
        } catch (NameNotFoundException e) { 
            Xlog.d(TAG , "ClassNotFoundException happens,"
                    + "MTK Keyguard did not install");
        }

        return isApkExist;
    }

     /**
     * M: DataUsage_Enhancement_fuction getEnableStateInProvider
     *    Gte the previous saved lockScreen visibility value
     */
    private boolean getEnableStateInProvider(String currentTab) {
        int previousState = 1;//ON is 1, OFF is 0
        try {
            if (FeatureOption.MTK_GEMINI_SUPPORT) {
                if (TAB_SIM_1.equals(currentTab)) {
                    previousState = Settings.System.getInt(getActivity().getContentResolver(),
                            Settings.System.DATAUSAGE_ON_LOCKSCREEN_SIM1);
                } else if (TAB_SIM_2.equals(currentTab)) {
                    previousState = Settings.System.getInt(getActivity().getContentResolver(),
                            Settings.System.DATAUSAGE_ON_LOCKSCREEN_SIM2);
                }
            } else {
                // TAB_MOBILE
                previousState = Settings.System.getInt(getActivity().getContentResolver(),
                        Settings.System.DATAUSAGE_ON_LOCKSCREEN_SIM1);
            }
        } catch (SettingNotFoundException e) {
            previousState = 0;
            Xlog.d(TAG, "Get data from provider failure");
        }
        Xlog.d(TAG, "currentTab : " + currentTab + " lock screen state ON : " + (previousState == 1));
        return (previousState == 1);
    }

    /**
     * M: DataUsage_Enhancement_fuction isSimStatusReady()
     *    Judge whetehr the SIM is radio off or Airplane Mode on
     */
    private boolean isSimStatusNotReady(String currentTab) {
        boolean isStatusNotReady = true;
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            boolean isSimRdioOff = true;
            if (TAB_SIM_1.equals(currentTab)) {
                isSimRdioOff = (getSimIndicatorState(PhoneConstants.GEMINI_SIM_1) == PhoneConstants.SIM_INDICATOR_RADIOOFF);
            } else if (TAB_SIM_2.equals(currentTab)) {
                isSimRdioOff = (getSimIndicatorState(PhoneConstants.GEMINI_SIM_2) == PhoneConstants.SIM_INDICATOR_RADIOOFF);
            } else if (TAB_SIM_3.equals(currentTab)) {
                isSimRdioOff = (getSimIndicatorState(PhoneConstants.GEMINI_SIM_3) == PhoneConstants.SIM_INDICATOR_RADIOOFF); 
            }
            isStatusNotReady = mIsAirplaneModeOn || isSimRdioOff;
        } else if (TAB_MOBILE.equals(currentTab)) {
            isStatusNotReady = isAirplaneModeOn(getActivity());
        }
        return isStatusNotReady;
    }

    /**
     * M: DataUsage_Enhancement_fuction inflateLockScreenView()
     *    Add lockScreen preference into mNetworkSwitches viewgroup
     */
    private void inflateLockScreenView(LayoutInflater inflater) {
        if (mNetworkSwitches != null) {
            mLockScreenEnabled = new Switch(inflater.getContext());
            mShowOnLockScreenView = inflatePreference(inflater,
                    mNetworkSwitches, mLockScreenEnabled);
            mShowOnLockScreenView.setClickable(true);
            mShowOnLockScreenView.setFocusable(true);
            mLockScreenEnabled.setChecked(getEnableStateInProvider(mTabHost.getCurrentTabTag()));
            mLockScreenEnabled
                    .setOnCheckedChangeListener(new OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView,boolean isChecked) {
                            final String currentTab = mTabHost.getCurrentTabTag();
                            String lockScreenTag = Settings.System.DATAUSAGE_ON_LOCKSCREEN_SIM1;
                            if (FeatureOption.MTK_GEMINI_SUPPORT) {
                                if (TAB_SIM_1.equals(currentTab)) {
                                    lockScreenTag = Settings.System.DATAUSAGE_ON_LOCKSCREEN_SIM1;
                                } else if (TAB_SIM_2.equals(currentTab)) {
                                    lockScreenTag = Settings.System.DATAUSAGE_ON_LOCKSCREEN_SIM2;
                                }
                            } else {
                                // TAB_MOBILE
                                lockScreenTag = Settings.System.DATAUSAGE_ON_LOCKSCREEN_SIM1;
                            }
                            Settings.System.putInt(getActivity().getContentResolver(), lockScreenTag , isChecked ? 1 : 0);
                            Xlog.d(TAG, "lockScreenTag: " + lockScreenTag + " isChecked:" + isChecked);
                        }
                    });
            //Set preference title
            mLockScreenPrefTitle = (TextView) mShowOnLockScreenView.findViewById(android.R.id.title);
            mLockScreenPrefTitle.setText(R.string.mtk_datausage_show_on_lockscreen);
            mNetworkSwitches.addView(mShowOnLockScreenView);
        }
    }

    /**
     * M: DataUsage_Enhancement_fuction updateLockScreenViewState()
     *    Update lock screen view state according to policy
     */
    private void updateLockScreenViewState(NetworkTemplate template ,String currentTab) {
        Xlog.d(TAG , "updateLockScreenViewState()");
        if (mShowOnLockScreenView == null || mLockScreenEnabled == null
             || isSimStatusNotReady(currentTab)) {
            //If SIM status not ready,just hide the lock screen view
            //do not change the state
            return;
        }

        final NetworkPolicy policy = mPolicyEditor.getPolicy(template);
        final long limitBytes = (policy != null) ? policy.limitBytes : 0;
        if (mDataEnabled.isChecked() && !isAppDetailMode()) {
            if (mDisableAtLimit.isChecked()) {
                if (limitBytes == 0) {
                    //disable lockscreen switch when limit value is 0
                    //set lockscreen switch OFF when limit value is 0
                    setLockScreenState(false,false);
                } else {
                    //enable lockscreen switch when limit value is not 0
                    //set lockscreen switch state ON after you set limit checked
                    //set lockscreen switch state according to the previous state
                    //when limit value is not 0
                    boolean isChecked = mIsLimitChangeToChecked ? true : getEnableStateInProvider(currentTab);
                    setLockScreenState(true,isChecked);
                    mIsLimitChangeToChecked = false;
                }
            } else {
                //disable lockscreen switch when limit is not set
                //set lockscreen switch off when limit is not set
                setLockScreenState(false,false);
            }
        } else {
            //disable lockscreen switch when data connection is off
            //set lockscreen switch state according to the previous state
            setLockScreenState(false,getEnableStateInProvider(currentTab));
        }
   }
    /**
     * M: DataUsage_Enhancement_fuction , setLockScreenState
     *    set lock screen view state : is enabled ? checked ?
     */
     private void setLockScreenState(boolean isEnabled,boolean isChecked) {
         mLockScreenEnabled.setEnabled(isEnabled);
         mLockScreenEnabled.setChecked(isChecked);
         mLockScreenPrefTitle.setEnabled(isEnabled);
     }

    /**
     * M: DataUsage_Enhancement_fuction updateLockScreenViewVisibility()
     *    Update lock screen view according to TAB type and SIM status
     */
    private void updateLockScreenViewVisibility(String currentTab) {
        Xlog.d(TAG , "updateLockScreenViewVisibility() currentTab : " + currentTab);
        if (mShowOnLockScreenView == null) {
            return;
        }
        if (TAB_WIFI.equals(currentTab)) {
            // Hide the LockScreenEnabledView when in WIFI tab.
            mShowOnLockScreenView.setVisibility(View.GONE);
        } else {
            if (isSimStatusNotReady(currentTab)) {
                // Hide the LockScreenEnabledView when SIM status not ready
                mShowOnLockScreenView.setVisibility(View.GONE);
            } else {
                //show the LockScreenEnabledView according to whether MTK
                // LockScreen APK is installed
                mShowOnLockScreenView.setVisibility(
                     checkMtkLockScreenApkExist() ? View.VISIBLE : View.GONE);
             }
          }
     }
    
    /**
     * M: get current slot id
     */
    private int getCurrentSlot(String currentTab) {
        int simSlot = PhoneConstants.GEMINI_SIM_1;
        if (TAB_SIM_1.equals(currentTab)) {
          simSlot = PhoneConstants.GEMINI_SIM_1;
        } else if (TAB_SIM_2.equals(currentTab)) {
          simSlot = PhoneConstants.GEMINI_SIM_2;
        } else if (TAB_SIM_3.equals(currentTab)) {
          simSlot = PhoneConstants.GEMINI_SIM_3;
        }
        return simSlot;
    }

    /**
     * M: update switch state
     */
    private void updateDataSwitchState(CycleAdapter cycleAdapter, int slotId) {
        if (mIsAirplaneModeOn || getSimIndicatorState(slotId) == PhoneConstants.SIM_INDICATOR_RADIOOFF) {
            //Sim radio off , cannot set data connection for it.
            mDataEnabledView.setVisibility(View.GONE);
            mDisableAtLimitView.setVisibility(View.GONE);
            Xlog.d(TAG,"disable this sim enable because radio off ,slotId :" + slotId);
        } else {
            setPreferenceTitle(mDataEnabledView, R.string.data_usage_enable_mobile);
            setPreferenceTitle(mDisableAtLimitView, R.string.data_usage_disable_mobile_limit);
        }

        mDataEnabled.setEnabled(!sIsSwitching);
        mCycleAdapter = cycleAdapter;
        mTemplate = buildTemplateMobileAll(getSubscriberId(slotId));
      }

    /**
     * M: judge the slotID is inserted a sim card
     */
     private boolean isSimInserted(int slotId) {
         boolean isSimInserted = false;
         if (mITelephony != null) {
             try {
                 isSimInserted = mITelephony.isSimInsert(slotId);
             } catch (RemoteException e) {
                 Xlog.i(TAG, "RemoteException happens......");
             }
         }
         return isSimInserted;
     }

     /**
      * M: judge the slotID policy is limited
      */
     private boolean hasLimitedPolicy(int slotId) {
         boolean hasLimitedPolicy = false;
         if (mPolicyEditor != null) {
             hasLimitedPolicy = mPolicyEditor.hasLimitedPolicy(
                     buildTemplateMobileAll(getSubscriberId(slotId)));
         }
         return hasLimitedPolicy;
     }
     
     /**
      * M: judge the saved tab is remainly inserted by a sim card for sim hot swap
      */
     private boolean isRemainInserted(String savedTab) {
         boolean clearSavedTab = false;                    
           if (TAB_SIM_1.equals(savedTab)) {
               if (!isSimInserted(PhoneConstants.GEMINI_SIM_1)) {
                   clearSavedTab = true;
               }
           } else if (TAB_SIM_2.equals(savedTab)) {
               if (!isSimInserted(PhoneConstants.GEMINI_SIM_2)) {
                   clearSavedTab = true;
               }
           } else if (TAB_SIM_3.equals(savedTab)) {
               if (!isSimInserted(PhoneConstants.GEMINI_SIM_3)) {
                   clearSavedTab = true;
               }
           } else {
               Xlog.d(TAG,"savedTab is not current cared tab");
           }
           Xlog.d(TAG,"savedTab = " + savedTab + " clearSavedTab = " + clearSavedTab);
           return clearSavedTab;
     }

}
