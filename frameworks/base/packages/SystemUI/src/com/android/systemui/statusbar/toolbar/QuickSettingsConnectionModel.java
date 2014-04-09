package com.android.systemui.statusbar.toolbar;

import static android.provider.Settings.System.SCREEN_OFF_TIMEOUT;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.graphics.drawable.AnimationDrawable;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManagerGlobal;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.Toast;

import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.TelephonyProperties;
import com.android.systemui.R;
import com.android.systemui.statusbar.phone.PhoneStatusBar;
import com.android.systemui.statusbar.toolbar.SimIconsListView.SimItem;
import com.android.systemui.statusbar.util.SIMHelper;
import com.android.systemui.statusbar.util.StateTracker;
import com.mediatek.audioprofile.AudioProfileManager;
import com.mediatek.audioprofile.AudioProfileManager.Scenario;
import com.mediatek.common.audioprofile.AudioProfileListener;
import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.CellConnService.CellConnMgr;
import com.mediatek.systemui.ext.PluginFactory;
import com.mediatek.telephony.SimInfoManager;
import com.mediatek.xlog.Xlog;

import java.util.ArrayList;
import java.util.List;

/**
 * M: Support "Quick settings".
 */
public final class QuickSettingsConnectionModel {
    private static final String TAG = "QuickSettingsConnectionModel";
    private static final boolean DBG = true;
    
    private static final String TRANSACTION_START = "com.android.mms.transaction.START";
    private static final String TRANSACTION_STOP = "com.android.mms.transaction.STOP";
    private static final String IPO_BOOT = "android.intent.action.ACTION_PREBOOT_IPO";

    private boolean mUpdating = false;
    
    private static final int COUNT = 5;
    private Context mContext;
    private CellConnMgr mCellConnMgr;
    private PhoneStatusBar mStatusBarService;
    private View mWifiTileView;
    private View mBluetoothTileView;
    private View mGpsTileView;
    private View mMobileTileView;
    private View mAirlineModeTileView;
    private View mTimeoutTileView;
    private View mAudioProfileTileView;
    private View mAutoRotateTileView;
    private ImageView mWifiIcon;
    private ImageView mBluetoothIcon;
    private ImageView mGpsIcon;
    private ImageView mMobileIcon;
    private ImageView mAirlineModeIcon;
    private ImageView mTimeoutIcon;
    private ImageView mAutoRotateIcon;
    private ImageView mTimeoutIndicator;
    private ImageView mDataConnSwitchIngGifView;
    private ImageView mBluetoothSwitchIngGifView;
    private ImageView mWifiSwitchIngGifView;
    private ImageView mAudioProfileIcon;
    private ImageView mNormalProfileIcon;
    private ImageView mMettingProfileIcon;
    private ImageView mMuteProfileIcon;
    private ImageView mOutdoorSwitchIcon;
    private FrameLayout mDataConnLayout;
    private FrameLayout mBluetoothLayout;
    private FrameLayout mWifiLayout;
    private WifiStateTracker mWifiStateTracker;
    private BluetoothStateTracker mBluetoothStateTracker;
    private GpsStateTracker mGpsStateTracker;
    private MobileStateTracker mMobileStateTracker;
    private AirlineModeStateTracker mAirlineModeStateTracker;
    private TimeoutStateTracker mTimeoutStateTracker;
    private AutoRotationStateTracker mAutoRotationStateTracker;
    private AlertDialog mSwitchDialog;
    private Dialog mProfileSwitchDialog;
    private SimIconsListView mSwitchListview;
    private Handler mHandler = new Handler();
    /// M: whether the SIMs initialization of framework is ready.
    private boolean mSimCardReady = false;
    /// M: time out message event
    private static final int EVENT_DETACH_TIME_OUT = 2000;
    private static final int EVENT_ATTACH_TIME_OUT = 2001;
    /// M: time out length
    private static final int DETACH_TIME_OUT_LENGTH = 10000; /// TF Timeout is 40s
    private static final int ATTACH_TIME_OUT_LENGTH = 30000;
    
    public static final int MINIMUM_TIMEOUT = 15000;
    public static final int MEDIUM_TIMEOUT = 30000;
    public static final int MAXIMUM_TIMEOUT = 60000;
    /** M: If there is no setting in the provider, use this. */
    private static final int FALLBACK_SCREEN_TIMEOUT_VALUE = 30000;
    
    private static final int PROFILE_SWITCH_DIALOG_LONG_TIMEOUT = 4000;
    private static final int PROFILE_SWITCH_DIALOG_SHORT_TIMEOUT = 2000;
    
    private AudioProfileManager mProfileManager;
    private AudioManager mAudioManager;
    private List<String> mProfileKeys;
    private Scenario mCurrentScenario;

    private Handler mDataTimerHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            long simId;
            switch (msg.what) {
            case EVENT_DETACH_TIME_OUT:
                simId = Settings.System.getLong(mContext.getContentResolver(),
                        Settings.System.GPRS_CONNECTION_SIM_SETTING,
                        Settings.System.DEFAULT_SIM_NOT_SET);
                Xlog.d(TAG, "detach time out......simId is " + simId);
                /// M: only apply if NOT wifi-only device @{
                if (!isWifiOnlyDevice()) {
                    mMobileStateTracker.refresh();
                }
                /// M: }@
                break;
            case EVENT_ATTACH_TIME_OUT:
                simId = Settings.System.getLong(mContext.getContentResolver(),
                        Settings.System.GPRS_CONNECTION_SIM_SETTING,
                        Settings.System.DEFAULT_SIM_NOT_SET);
                Xlog.d(TAG, "attach time out......simId is " + simId);
                /// M: only apply if NOT wifi-only device @{
                if (!isWifiOnlyDevice()) {
                    mMobileStateTracker.refresh();
                }
                /// M: }@
                break;
            default:
                break;
            }
        }
    };
    
    private ContentObserver mMobileStateChangeObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            /// M: only apply if NOT wifi-only device @{
            if (!isWifiOnlyDevice()) {
            /// M: }@
                if (!mMobileStateTracker.getIsUserSwitching()) {
                    mMobileStateTracker.setImageViewResources(mContext);
                }
            /// M: only apply if NOT wifi-only device @{
            }
            /// M: }@
        }
    };
    
    private ContentObserver mMobileStateForSingleCardChangeObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            /// M: only apply if NOT wifi-only device @{
            if (!isWifiOnlyDevice()) {
            /// M: }@
                mMobileStateTracker.onActualStateChange(mContext, null);
                mMobileStateTracker.setImageViewResources(mContext);
            /// M: only apply if NOT wifi-only device @{
            }
            /// M: }@
        }
    };

    private int mServiceState1;
    /// M: Support GeminiPlus
    private int mServiceStateGemini[] = new int[PhoneConstants.GEMINI_SIM_NUM];
    PhoneStateListener mPhoneStateListener1 = new PhoneStateListener() {
        @Override
        public void onServiceStateChanged(ServiceState serviceState) {
            Xlog.d(TAG, "PhoneStateListener1.onServiceStateChanged: serviceState=" + serviceState);
            mServiceState1 = serviceState.getState();
            onAirplaneModeChanged();
        }
    };

    /// M: Support GeminiPlus
    PhoneStateListener mPhoneStateListenerGemini = new PhoneStateListener() {
        @Override
        public void onServiceStateChanged(ServiceState serviceState) {
            final int simId = serviceState.getMySimId();
            if ((PhoneConstants.GEMINI_SIM_1 <= simId) && (simId < PhoneConstants.GEMINI_SIM_1 + PhoneConstants.GEMINI_SIM_NUM)) {
                Xlog.d(TAG, "mPhoneStateListenerGemini.onServiceStateChanged: serviceState = " + serviceState);
                mServiceStateGemini[serviceState.getMySimId()] = serviceState.getState();
                onAirplaneModeChanged();
            } else {
                Xlog.d(TAG, "mPhoneStateListenerGemini.onServiceStateChanged: invalid sim id =" + simId);
            }
        }
    };

    /**
     * M: Used to check weather this device is wifi only.
     */
    private boolean isWifiOnlyDevice() {
      ConnectivityManager cm = (ConnectivityManager)mContext.getSystemService(mContext.CONNECTIVITY_SERVICE);
      return  !(cm.isNetworkSupported(ConnectivityManager.TYPE_MOBILE));
    }

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (DBG) {
                Xlog.d(TAG, "onReceive called, action is " + action);
            }
            if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                mWifiStateTracker.onActualStateChange(context, intent);
                mWifiStateTracker.setImageViewResources(context);
            } else if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                mBluetoothStateTracker.onActualStateChange(context, intent);
                mBluetoothStateTracker.setImageViewResources(context);
            } else if (action.equals(LocationManager.PROVIDERS_CHANGED_ACTION)) {
                mGpsStateTracker.onActualStateChange(context, intent);
                mGpsStateTracker.setImageViewResources(context);
            } else if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
                boolean enabled = intent.getBooleanExtra("state", false);
                if (DBG) {
                    Xlog.d(TAG, "airline mode changed: state is " + enabled);
                }
                /// M: only apply if NOT wifi-only device @{
                if (!isWifiOnlyDevice()) {
                /// M: }@
                    mMobileStateTracker.setAirlineMode(enabled);
                    mMobileTileView.setEnabled(mMobileStateTracker.isClickable());
                    mMobileStateTracker.setImageViewResources(context);

                    /// M: Set icon directly while turning off the airplane mode
                    if (!enabled) {
                        Intent mAirlineintent = new Intent();
                        mAirlineintent.putExtra("state", enabled);
                        mAirlineModeStateTracker.onActualStateChange(mContext, mAirlineintent);
                        mAirlineModeStateTracker.setImageViewResources(mContext);
                    }
                /// M: only apply if NOT wifi-only device @{
                }
                /// M: }@
                /// M: [ALPS00344645] @{
                if (isWifiOnlyDevice()) {
                  Intent intent2 = new Intent();
                  intent2.putExtra("state", enabled);
                  mAirlineModeStateTracker.onActualStateChange(mContext, intent2);
                  mAirlineModeStateTracker.setImageViewResources(mContext);
                }
                /// M: }@
                if (FeatureOption.MTK_WLAN_SUPPORT) {
                    if (PluginFactory.getStatusBarPlugin(mContext).supportDisableWifiAtAirplaneMode()) {
                        mWifiStateTracker.setAirlineMode(enabled);
                        mWifiTileView.setEnabled(mWifiStateTracker.isClickable());
                        }
                }
            } else if (action.equals(TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED)) {
                if (FeatureOption.MTK_GEMINI_SUPPORT) {
                    PhoneConstants.DataState state = getMobileDataState(intent);
                    boolean isApnType = false;
                    String types = intent.getStringExtra(PhoneConstants.DATA_APN_TYPE_KEY);
                    if (types != null) {
                        String[] typeArray = types.split(",");
                        for (String type : typeArray) {
                            if (PhoneConstants.APN_TYPE_DEFAULT.equals(type)) {
                                isApnType = true;
                                break;
                            }
                        }
                    }
                    /// M: only apply if NOT wifi-only device @{
                    if (!isWifiOnlyDevice()) {
                    /// M: }@
                        if (DBG) {
                            Xlog.d(TAG, "isApnType = " + isApnType
                                    + " , state = " + state
                                    + " , mMobileStateTracker.mGprsTargSim = " + mMobileStateTracker.mGprsTargSim
                                    + " , mMobileStateTracker.mIsMmsOngoing = " + mMobileStateTracker.mIsMmsOngoing);
                        }
                        if (isApnType &&
                            ((state == PhoneConstants.DataState.CONNECTED) 
                                    || (state == PhoneConstants.DataState.DISCONNECTED)
                                    && !mMobileStateTracker.mGprsTargSim
                                    && !mMobileStateTracker.mIsMmsOngoing)) {                   
                            mMobileStateTracker.onActualStateChange(context, intent);
                            mMobileStateTracker.setImageViewResources(context);
                            /// M: Refresh the sim indicators
                            if (mSwitchListview != null) {
                                mSwitchListview.initSimList();
                                mSwitchListview.notifyDataChange();
                            }
                        }
                    /// M: only apply if NOT wifi-only device @{
                    }
                    /// M: }@
                }
            } else if (action.equals(TRANSACTION_START)) {
                /// M: only apply if NOT wifi-only device @{
                if (!isWifiOnlyDevice()) {
                /// M: }@
                    mMobileStateTracker.setIsMmsOngoing(true);
                    mMobileTileView.setEnabled(mMobileStateTracker.isClickable());
                    if (mSwitchDialog != null && mSwitchDialog.isShowing()) {
                        dismissDialogs();
                    }
                /// M: only apply if NOT wifi-only device @{
                }
                /// M: }@
            } else if (action.equals(TRANSACTION_STOP)) {
                /// M: only apply if NOT wifi-only device @{
                if (!isWifiOnlyDevice()) {
                /// M: }@
                    mMobileStateTracker.setIsMmsOngoing(false);
                    mMobileTileView.setEnabled(mMobileStateTracker.isClickable());
                    mMobileStateTracker.setImageViewResources(mContext);
                    if (mSwitchDialog != null && mSwitchDialog.isShowing()) {
                        dismissDialogs();
                    }
                /// M: only apply if NOT wifi-only device @{
                }
                /// M: }@
            } else if (action.equals(TelephonyIntents.ACTION_SIM_INDICATOR_STATE_CHANGED)) {
                if (!isWifiOnlyDevice()) {
                    updateForSimReady();
                }
            } else if (action.equals(IPO_BOOT)) {
                Xlog.d(TAG, "IPO Boot: initConfigurationState()");
                initConfigurationState();
            } 
        }
    };
    
    public void updateForSimReady() {
        Xlog.d(TAG, "Panel sim ready called");
        /// M: only apply if NOT wifi-only device @{
        if (!isWifiOnlyDevice()) {
        /// M: }@
            mSimCardReady = true;
            List<SimInfoManager.SimInfoRecord> simInfos = SIMHelper.getSIMInfoList(mContext);
            if (simInfos == null || simInfos.size() <= 0) {
                mMobileStateTracker.setHasSim(false);
            } else {
                mMobileStateTracker.setHasSim(true);
            }
            mMobileTileView.setEnabled(mMobileStateTracker.isClickable());
            mMobileStateTracker.setImageViewResources(mContext);
        /// M: only apply if NOT wifi-only device @{
        }
        /// M: }@
    }
    
    /**
     * M: When siminfo changed, for example siminfo's background resource changed,
     *  need to update data connection button's background.
     * @param intent The intent to use, used to get extra sim id information.
     */
    public void updateSimInfo(Intent intent) {
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            /// M: only apply if NOT wifi-only device @{
            if (!isWifiOnlyDevice()) {
            /// M: }@
                int type = intent.getIntExtra("type", -1);
                if (type == 1) {
                    long simId = intent.getLongExtra("simid", -1);
                    long currentSimId = SIMHelper.getDefaultSIM(mContext,
                            Settings.System.GPRS_CONNECTION_SIM_SETTING);
                    if ((simId == currentSimId) && (currentSimId > 0)) {
                        if (DBG) {
                            Xlog.d(TAG, "sim setting changed, simId is " + simId);
                        }
                        mMobileStateTracker.setImageViewResources(mContext);
                    }
                }
            /// M: only apply if NOT wifi-only device @{
            }
            /// M: }@
        }
    }

    /**
     * M: Called when we've received confirmation that the airplane mode was set.
     */
    private void onAirplaneModeChanged() {
        boolean airplaneModeEnabled = isAirplaneModeOn(mContext);
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            /// M: [ALPS00225004]
            /// M: When AirplaneMode On, make sure both phone1 and phone2 are radio off
            if (airplaneModeEnabled) {
                /// M: Support GeminiPlus
                for (int i = PhoneConstants.GEMINI_SIM_1; i < PhoneConstants.GEMINI_SIM_1 + PhoneConstants.GEMINI_SIM_NUM; i++) {
                    if (mServiceStateGemini[i] != ServiceState.STATE_POWER_OFF) {
                        Xlog.d(TAG, "Unfinish! mServiceStateGemini:" + mServiceStateGemini[i] + " for simId = " + i);
                        return;
                    }
                }
            }
        } else {
            /// M: [ALPS00127431]
            /// M: When AirplaneMode On, make sure phone is radio off
            if (airplaneModeEnabled) {
                if (mServiceState1 != ServiceState.STATE_POWER_OFF) {
                    Xlog.d(TAG, "Unfinish! serviceState:" + mServiceState1);
                    return;
                }
            }
        }
        Xlog.d(TAG, "onServiceStateChanged called, inAirplaneMode is: " + airplaneModeEnabled);
        Intent intent = new Intent();
        intent.putExtra("state", airplaneModeEnabled);
        mAirlineModeStateTracker.onActualStateChange(mContext, intent);
        mAirlineModeStateTracker.setImageViewResources(mContext);
    }

    private static PhoneConstants.DataState getMobileDataState(Intent intent) {
        String str = intent.getStringExtra(PhoneConstants.STATE_KEY);
        if (str != null) {
            return Enum.valueOf(PhoneConstants.DataState.class, str);
        } else {
            return PhoneConstants.DataState.DISCONNECTED;
        }
    }

    public QuickSettingsConnectionModel(Context context) {
        mContext = context;
        mCellConnMgr = new CellConnMgr(null);
        mCellConnMgr.register(mContext);
    }

    public void buildIconViews() {
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mProfileManager = (AudioProfileManager) mContext.getSystemService(Context.AUDIOPROFILE_SERVICE);
        if (FeatureOption.MTK_WLAN_SUPPORT) {
            mWifiStateTracker = new WifiStateTracker();
        }
        if (FeatureOption.MTK_BT_SUPPORT) {
            mBluetoothStateTracker = new BluetoothStateTracker();
        }
        if (FeatureOption.MTK_GPS_SUPPORT) {
            mGpsStateTracker = new GpsStateTracker();
        }
        /// M: only apply if NOT wifi-only device @{
        if (!isWifiOnlyDevice()) {
        /// M: }@
            mMobileStateTracker = new MobileStateTracker();
        /// M: only apply if NOT wifi-only device @{
        }
        /// M: }@
        mAirlineModeStateTracker = new AirlineModeStateTracker();
        mTimeoutStateTracker = new TimeoutStateTracker();
        mAutoRotationStateTracker = new AutoRotationStateTracker();

        if (FeatureOption.MTK_WLAN_SUPPORT) {
            mWifiTileView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!wasItTheMonkey()) {
                        WifiManager mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
                        if (mWifiManager != null) {
                            int wifiApState = mWifiManager.getWifiApState();
                            if ((wifiApState == WifiManager.WIFI_AP_STATE_ENABLING)
                                    || (wifiApState == WifiManager.WIFI_AP_STATE_ENABLED)) {
                                mWifiManager.setWifiApEnabled(null, false);
                            }
                        }
                        mWifiStateTracker.toggleState(mContext);
                    }
                }
            });
        }
        if (FeatureOption.MTK_BT_SUPPORT) {
            mBluetoothTileView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mBluetoothStateTracker.toggleState(mContext);
                }
            });
        }
        if (FeatureOption.MTK_GPS_SUPPORT) {
            mGpsTileView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mGpsStateTracker.toggleState(mContext);
                }
            });
        }

        /// M: only apply if NOT wifi-only device @{
        if (!isWifiOnlyDevice()) {
        /// M: }@
            mMobileTileView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!wasItTheMonkey()) {
                        if (mMobileStateTracker.isDataDialogShown()
                                || (mSwitchDialog != null
                                && mSwitchDialog.isShowing())) {
                            return;
                        }
                        mMobileStateTracker.toggleState(mContext);
                    }
                }
            });
        /// M: only apply if NOT wifi-only device @{
        }
        /// M: }@
        mAirlineModeTileView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!wasItTheMonkey()) {
                    Xlog.d("ClickEvent", "AirPlane button click");
                    mAirlineModeStateTracker.setAirPlaneModeClickable(false);
                    mAirlineModeStateTracker.toggleState(mContext);
                    new Handler().postDelayed(new Runnable() {
                        public void run() {
                            mAirlineModeStateTracker.setAirPlaneModeClickable(true);
                            mAirlineModeTileView.setEnabled(
                                    mAirlineModeStateTracker.isClickable());
                        }
                    }, 600);
                }
            }
        });
        
        mTimeoutTileView.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mTimeoutStateTracker.toggleState(mContext);
            }
        });
        
        mAutoRotateTileView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mAutoRotationStateTracker.toggleState(mContext);
            }
        });
        
        mAudioProfileTileView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showProfileSwitchDialog();
            }
        });
        
        createProfileSwitchDialog();
    }
    
    private boolean wasItTheMonkey() {
        ActivityManager activityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE); 
        if (activityManager.isUserAMonkey()) {
              Xlog.d(TAG,"it was the monkey");
              return true;
        }
        Xlog.d(TAG,"it was an user");
        return false;
    } 
    
    private AlertDialog createDialog(View v, int resId) {
        AlertDialog.Builder b = new AlertDialog.Builder(mContext);
        b.setCancelable(true).setTitle(resId).setView(v, 0, 0, 0, 0)
                .setInverseBackgroundForced(true).setNegativeButton(
                        android.R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                if (mSwitchDialog != null) {
                                    mSwitchDialog.hide();
                                }
                            }
                        });
        AlertDialog alertDialog = b.create();
        alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_STATUS_BAR_PANEL);
        return alertDialog;
    }
    
    public void dismissDialogs() {
        if (mSwitchDialog != null) {
            mSwitchDialog.dismiss();
        }
    }

    public void setUpdates(boolean update) {
        if (update != mUpdating) {
            mProfileKeys = new ArrayList<String>();
            mProfileKeys = mProfileManager.getPredefinedProfileKeys();
            mUpdating = update;
            if (update) {
                IntentFilter filter = new IntentFilter();
                if (FeatureOption.MTK_WLAN_SUPPORT) {
                    filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
                }
                if (FeatureOption.MTK_BT_SUPPORT) {
                    filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
                }
                if (FeatureOption.MTK_GPS_SUPPORT) {
                    filter.addAction(LocationManager.PROVIDERS_CHANGED_ACTION);
                }
                /// M: for mobile config
                filter.addAction(TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED);
                filter.addAction(TelephonyIntents.ACTION_SIM_INDICATOR_STATE_CHANGED);
                filter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
                filter.addAction(TRANSACTION_START);
                filter.addAction(TRANSACTION_STOP);
                filter.addAction(IPO_BOOT);
                mContext.registerReceiver(mIntentReceiver, filter);
                if (FeatureOption.MTK_GEMINI_SUPPORT) {
                    mContext.getContentResolver().registerContentObserver(
                            Settings.System.getUriFor(Settings.System.GPRS_CONNECTION_SIM_SETTING)
                            , true, mMobileStateChangeObserver);
                } else {
                    mContext.getContentResolver().registerContentObserver(
                            Settings.Secure.getUriFor(Settings.Global.MOBILE_DATA)
                            , true, mMobileStateForSingleCardChangeObserver);
                }
                /// M: get notified of phone state changes
                TelephonyManager telephonyManager = (TelephonyManager) mContext
                        .getSystemService(Context.TELEPHONY_SERVICE);
                if (FeatureOption.MTK_GEMINI_SUPPORT) {
                    /// M: Support GeminiPlus
                    for (int i = PhoneConstants.GEMINI_SIM_1; i < PhoneConstants.GEMINI_SIM_1 + PhoneConstants.GEMINI_SIM_NUM; i++) {
                        SIMHelper.listen(mPhoneStateListenerGemini, PhoneStateListener.LISTEN_SERVICE_STATE, i);
                    }
                } else {
                    telephonyManager.listen(mPhoneStateListener1,
                            PhoneStateListener.LISTEN_SERVICE_STATE);
                }
                mContext.getContentResolver().registerContentObserver(
                        Settings.System.getUriFor(Settings.System.SCREEN_OFF_TIMEOUT),
                        true, mTimeoutChangeObserver);
                mContext.getContentResolver().registerContentObserver(
                        Settings.System.getUriFor(Settings.System.ACCELEROMETER_ROTATION),
                        true, mAutoRotationChangeObserver);
                
                /// M: Register for Intent broadcasts for the clock and battery
                Xlog.d(TAG, "setUpdates: listenAudioProfie with mAudioProfileListenr = " + mAudioProfileListenr);
                mProfileManager.listenAudioProfie(mAudioProfileListenr, AudioProfileListener.LISTEN_AUDIOPROFILE_CHANGEG);

            } else {
                mContext.unregisterReceiver(mIntentReceiver);
                if (FeatureOption.MTK_GEMINI_SUPPORT) {
                    mContext.getContentResolver().unregisterContentObserver(
                            mMobileStateChangeObserver);
                } else {
                    mContext.getContentResolver().unregisterContentObserver(
                            mMobileStateForSingleCardChangeObserver);
                }
                TelephonyManager telephonyManager = (TelephonyManager) mContext
                        .getSystemService(Context.TELEPHONY_SERVICE);
                if (FeatureOption.MTK_GEMINI_SUPPORT) {
                    /// M: Support GeminiPlus
                    for (int i = PhoneConstants.GEMINI_SIM_1; i < PhoneConstants.GEMINI_SIM_1 + PhoneConstants.GEMINI_SIM_NUM; i++) {
                        SIMHelper.listen(mPhoneStateListenerGemini, 0, i);
                    }
                } else {
                    telephonyManager.listen(mPhoneStateListener1, 0);
                }
                mContext.getContentResolver().unregisterContentObserver(mTimeoutChangeObserver);
                mContext.getContentResolver().unregisterContentObserver(mAutoRotationChangeObserver);
                mProfileManager.listenAudioProfie(mAudioProfileListenr, AudioProfileListener.LISTEN_NONE);
            }
        }
    }

    /**
     * M: Subclass of StateTracker to get/set Wifi state.
     */
    private final class WifiStateTracker extends StateTracker {
        private boolean mIsAirlineMode = false;

        public void setAirlineMode(boolean enable) {
            if (DBG) {
                Xlog.d(TAG, "Mobile setAirlineMode called, enabled is: " + enable);
            }
            mIsAirlineMode = enable;
        }

        public boolean isClickable() {
            Xlog.d(TAG, "wifi mIsAirlineMode is " + mIsAirlineMode + ", mIsUserSwitching is " + mIsUserSwitching);
            return !mIsAirlineMode && super.isClickable();
        }

        @Override
        public int getActualState(Context context) {
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                return wifiStateToFiveState(wifiManager.getWifiState());
            }
            return STATE_DISABLED;
        }

        @Override
        protected void requestStateChange(Context context, final boolean desiredState) {
            final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            if (wifiManager == null) {
                Xlog.d(TAG, "No wifiManager.");
                setCurrentState(context, STATE_DISABLED);
                return;
            }
            /// M: Actually request the wifi change and persistent settings write off the UI thread, as it can take a
            /// user-noticeable amount of time, especially if there's disk contention.
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... args) {
                    wifiManager.setWifiEnabled(desiredState);
                    return null;
                }
            }.execute();
        }

        @Override
        public void onActualStateChange(Context context, Intent intent) {
            if (!WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction())) {
                return;
            }
            int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1);
            setCurrentState(context, wifiStateToFiveState(wifiState));
        }

        @Override
        public int getDisabledResource() {
            return R.drawable.ic_qs_wifi_off;
        }

        @Override
        public int getEnabledResource() {
            return R.drawable.ic_qs_wifi_enable;
        }

        @Override
        public int getInterMedateResource() {
            return R.drawable.ic_qs_stat_sys_wifi_switch_anim;
        }

        @Override
        public ImageView getImageButtonView() {
            return mWifiIcon;
        }

        public View getTileView() {
            return mWifiTileView;
        }

        @Override
        public ImageView getSwitchingGifView() {
            if (mWifiSwitchIngGifView == null) {
                ViewGroup.LayoutParams layoutParam = new ViewGroup.LayoutParams(
                        LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
                mWifiSwitchIngGifView = new ImageView(mContext);
                mWifiLayout.addView(mWifiSwitchIngGifView, 0);
                mWifiSwitchIngGifView.setVisibility(View.GONE);
            }
            return mWifiSwitchIngGifView;
        }

        /**
         * M: Converts WifiManager's state values into our Wifi/Bluetooth-common state values.
         */
        private int wifiStateToFiveState(int wifiState) {
            switch (wifiState) {
            case WifiManager.WIFI_STATE_DISABLED:
                return STATE_DISABLED;
            case WifiManager.WIFI_STATE_ENABLED:
                return STATE_ENABLED;
            case WifiManager.WIFI_STATE_DISABLING:
                return STATE_TURNING_OFF;
            case WifiManager.WIFI_STATE_ENABLING:
                return STATE_TURNING_ON;
            default:
                return STATE_DISABLED;
            }
        }

        @Override
        public ImageView getIndicatorView() {
            return null;
        }

    }

    /**
     * M: Subclass of StateTracker to get/set Bluetooth state.
     */
    private final class BluetoothStateTracker extends StateTracker {
        @Override
        public int getActualState(Context context) {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter == null) {
                return STATE_DISABLED;
            }
            return bluetoothStateToFiveState(bluetoothAdapter.getState());
        }

        @Override
        protected void requestStateChange(Context context, final boolean desiredState) {
            final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter == null) {
                setCurrentState(context, STATE_DISABLED);
                return;
            }
            /// M: Actually request the Bluetooth change and persistent settings write off the UI thread, as it can take a
            /// user-noticeable amount of time, especially if there's disk contention.
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... args) {
                    if (desiredState) {
                        bluetoothAdapter.enable();
                    } else {
                        bluetoothAdapter.disable();
                    }
                    return null;
                }
            }.execute();
        }

        @Override
        public void onActualStateChange(Context context, Intent intent) {
            if (!BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                return;
            }
            int bluetoothState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
            setCurrentState(context, bluetoothStateToFiveState(bluetoothState));
        }

        /**
         * M: Converts BluetoothAdapter's state values into our Wifi/Bluetooth-common state values.
         */
        private int bluetoothStateToFiveState(int bluetoothState) {
            switch (bluetoothState) {
            case BluetoothAdapter.STATE_OFF:
                return STATE_DISABLED;
            case BluetoothAdapter.STATE_ON:
                return STATE_ENABLED;
            case BluetoothAdapter.STATE_TURNING_ON:
                return STATE_TURNING_ON;
            case BluetoothAdapter.STATE_TURNING_OFF:
                return STATE_TURNING_OFF;
            default:
                return STATE_UNKNOWN;
            }
        }

        public int getDisabledResource() {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter == null) {
                return R.drawable.ic_qs_bluetooth_disable;
            }
            return R.drawable.ic_qs_bluetooth_off;
        }

        public int getEnabledResource() {
            return R.drawable.ic_qs_bluetooth_enable;
        }

        public ImageView getImageButtonView() {
            return mBluetoothIcon;
        }

        public View getTileView() {
            return mBluetoothTileView;
        }

        public int getInterMedateResource() {
            return R.drawable.ic_qs_stat_sys_bt_switch_anim;
        }
        
        @Override
        public ImageView getSwitchingGifView() {
            if (mBluetoothSwitchIngGifView == null) {
                ViewGroup.LayoutParams layoutParam = new ViewGroup.LayoutParams(
                        LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
                mBluetoothSwitchIngGifView = new ImageView(mContext);
                mBluetoothLayout.addView(mBluetoothSwitchIngGifView, 0);
                mBluetoothSwitchIngGifView.setVisibility(View.GONE);
            }
            return mBluetoothSwitchIngGifView;
        }

        @Override
        public ImageView getIndicatorView() {
            return null;
        }
    }

    /**
     * M: Subclass of StateTracker for GPS state.
     */
    private final class GpsStateTracker extends StateTracker {
        @Override
        public int getActualState(Context context) {
            ContentResolver resolver = context.getContentResolver();
            boolean on = Settings.Secure.isLocationProviderEnabled(resolver, LocationManager.GPS_PROVIDER);
            return on ? STATE_ENABLED : STATE_DISABLED;
        }

        @Override
        public void onActualStateChange(Context context, Intent unused) {
            /// M: Note: the broadcast location providers changed intent doesn't
            /// include an extras bundles saying what the new value is.
            setCurrentState(context, getActualState(context));
        }

        @Override
        public void requestStateChange(final Context context, final boolean desiredState) {
            final ContentResolver resolver = context.getContentResolver();
            new AsyncTask<Void, Void, Boolean>() {
                @Override
                protected Boolean doInBackground(Void... args) {
                    Settings.Secure.setLocationProviderEnabled(resolver, LocationManager.GPS_PROVIDER, desiredState);
                    return desiredState;
                }
                @Override
                protected void onPostExecute(Boolean result) {
                    setCurrentState(context, result ? STATE_ENABLED : STATE_DISABLED);
                    setImageViewResources(context);
                }
            }.execute();
        }

        public int getDisabledResource() {
            return R.drawable.ic_qs_gps_off;
        }

        public int getEnabledResource() {
            return R.drawable.ic_qs_gps_enable;
        }

        public ImageView getImageButtonView() {
            return mGpsIcon;
        }

        public View getTileView() {
            return mGpsTileView;
        }

        @Override
        public ImageView getIndicatorView() {
            return null;
        }
    }
    
    /**
     * M: Subclass of StateTracker for Mobile state.
     */
    private final class MobileStateTracker extends StateTracker {
        private boolean mGprsTargSim = false;
        private boolean mIsAirlineMode = false;
        private boolean mHasSim = false;
        private boolean mIsMmsOngoing = false;
        private boolean mIsDataDialogShown = false;

        ///M: Constant for current sim mode       
        private static final int ALL_RADIO_OFF = 0;
        private boolean isAllRadioOff() {
            boolean isAllRadioOff = mIsAirlineMode
                    || (Settings.System.getInt(mContext.getContentResolver(),
                            Settings.System.DUAL_SIM_MODE_SETTING, -1) == ALL_RADIO_OFF);
            Xlog.d(TAG, "isAllRadioOff=" + isAllRadioOff);
            return isAllRadioOff;
        }

        private boolean isRadioOff(int SlotId) {
            boolean isRadioOff = true;
            try {
                ITelephony mTelephony = SIMHelper.getITelephony();
                if (mTelephony != null) {
                    if (FeatureOption.MTK_GEMINI_SUPPORT) {
                        isRadioOff = !mTelephony.isRadioOnGemini(SlotId);
                    } else {
                        isRadioOff = !mTelephony.isRadioOn();
                    }
                }
            } catch (RemoteException e) {
                Xlog.e(TAG, "MobileStateTracker: isRadioOff() mTelephony exception");
            }
            Xlog.d(TAG, "MobileStateTracker: isRadioOff() is " + isRadioOff + ", slotId=" + SlotId);
            return isAllRadioOff() || isRadioOff;
        }

        private void unLockSimPin(int slotId) {
            if (mCellConnMgr != null) {
                Xlog.d(TAG, "MobileStateTracker: unLockSimPin() slotId is " + slotId);
                dismissDialogs();
                mStatusBarService.animateCollapsePanels();
                mCellConnMgr.handleCellConn(slotId, CellConnMgr.REQUEST_TYPE_SIMLOCK);
            } else {
                Xlog.d(TAG, "MobileStateTracker: mCellConnMgr is null");
            }
        }

        private void enableDataRoaming(long value) {
            Xlog.d(TAG, "enableDataRoaming with SimId=" + value);
            try {
                ITelephony mTelephony = SIMHelper.getITelephony();
                if (mTelephony != null) {
                    mTelephony.setDataRoamingEnabledGemini(true, 
                        SimInfoManager.getSlotById(mContext, value));
                }
            } catch (RemoteException e) {
                Xlog.e(TAG, "enableDataRoaming mTelephony exception");
                return;
            }
            SimInfoManager.setDataRoaming(mContext, SimInfoManager.DATA_ROAMING_ENABLE, value);
        }

        private int current3GSlotId() {
            int slot3G = -1;
            try {
                ITelephony mTelephony = SIMHelper.getITelephony();
                if (mTelephony != null) {
                    slot3G = mTelephony.get3GCapabilitySIM();
                }
            } catch (RemoteException e) {
                Xlog.e(TAG, "current3GSlotId mTelephony exception");
            }
            return slot3G;
        }

        private int dataSwitchConfirmDlgMsg(long simId) {
            SimInfoManager.SimInfoRecord siminfo = SIMHelper.getSIMInfo(mContext, simId);
            boolean isInRoaming = SIMHelper.getDefault(mContext).isNetworkRoaming(siminfo.mSimSlotId);
            boolean isRoamingDataAllowed = (siminfo.mDataRoaming == SimInfoManager.DATA_ROAMING_ENABLE);
            int g3SlotId = current3GSlotId();

            Xlog.d(TAG, "dataSwitchConfirmDlgMsg, g3SlotId=" + g3SlotId + " curSlotId=" + siminfo.mSimSlotId);
            Xlog.d(TAG, "dataSwitchConfirmDlgMsg, isInRoaming=" + isInRoaming + 
                " isRoamingDataAllowed=" + isRoamingDataAllowed);
            
            // by support 3G switch when data connection switch
            // and to a slot not current set 3G service
            if (isInRoaming) {
                if (!isRoamingDataAllowed) {
                    if (FeatureOption.MTK_GEMINI_3G_SWITCH) {
                        if (siminfo.mSimSlotId != g3SlotId) {
                            // under roaming but not abled and switch card is not 3G
                            // slot, \
                            // to pormpt user turn on roaming and how to modify to
                            // 3G service
                            return R.string.gemini_3g_disable_warning_case3;
                        } else {
                            // switch card is 3G slot but not able to roaming
                            // so only prompt to turn on roaming
                            return R.string.gemini_3g_disable_warning_case0;
                        }
                    } else {
                        // no support 3G service so only prompt user to turn on
                        // roaming
                        return R.string.gemini_3g_disable_warning_case0;
                    }
                } else {
                    if (FeatureOption.MTK_GEMINI_3G_SWITCH) {
                        if (siminfo.mSimSlotId != g3SlotId) {
                            // by support 3g switch and switched sim is not
                            // 3g slot to prompt user how to modify 3G service
                            return R.string.gemini_3g_disable_warning_case1;
                        }
                    }
                }
            } else {
                if (FeatureOption.MTK_GEMINI_3G_SWITCH
                        && siminfo.mSimSlotId != g3SlotId) {
                    // not in roaming but switched sim is not 3G
                    // slot so prompt user to modify 3G service
                    return R.string.gemini_3g_disable_warning_case1;
                }

            }
            return -1;
        }

        public void refresh() {
            mMobileStateTracker.setCurrentState(mContext, mMobileStateTracker.getActualState(mContext));
            mMobileStateTracker.setIsUserSwitching(false);
            mMobileTileView.setEnabled(mMobileStateTracker.isClickable());
            mMobileIcon.setVisibility(View.VISIBLE);
            stopFrameAnim();
            mMobileStateTracker.setImageViewResources(mContext);
        }

        public void setHasSim(boolean enable) {
            mHasSim = enable;
        }

        public void setAirlineMode(boolean enable) {
            if (DBG) {
                Xlog.d(TAG, "Mobile setAirlineMode called, enabled is: " + enable);
            }
            mIsAirlineMode = enable;
        }

        public void setIsMmsOngoing(boolean enable) {
            mIsMmsOngoing = enable;
        }

        public void setIsUserSwitching(boolean enable) {
            mIsUserSwitching = enable;
        }

        public boolean getIsUserSwitching() {
            return mIsUserSwitching;
        }

        public boolean isDataDialogShown() {
            return mIsDataDialogShown;
        }

        public boolean isClickable() {
            Xlog.d(TAG, "mobile mHasSim is " + mHasSim + ", mIsAirlineMode is "
                    + mIsAirlineMode + ", mIsMmsOngoing is " + mIsMmsOngoing
                    + ", mIsUserSwitching is " + mIsUserSwitching);
            return (mHasSim && !isAllRadioOff() && !mIsMmsOngoing && super.isClickable());
        }

        @Override
        public int getActualState(Context context) {
            if (FeatureOption.MTK_GEMINI_SUPPORT) {
                long simId = SIMHelper.getDefaultSIM(mContext, Settings.System.GPRS_CONNECTION_SIM_SETTING);
                if (DBG) {
                    Xlog.d(TAG, "MobileStateTracker.getActualState called, simId is" + simId);
                }
                return ((simId > 0) && (mHasSim) && (getEnabledResource() != -1)) ? STATE_ENABLED : STATE_DISABLED;
            } else {
                if (!mHasSim) {
                    return STATE_DISABLED;
                }
                ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
                if (cm != null) {
                    return cm.getMobileDataEnabled() ? STATE_ENABLED : STATE_DISABLED;
                } else {
                    return STATE_DISABLED;
                }
            }
        }

        @Override
        public void toggleState(Context context) {
            if (FeatureOption.MTK_GEMINI_SUPPORT) {
                List<SimInfoManager.SimInfoRecord> simInfos = SIMHelper.getSIMInfoList(context);
                Xlog.d(TAG, "toggleState simInfos = " + simInfos + " size = "
                        + ((simInfos != null) ? simInfos.size() : null));
                if (simInfos == null) {
                    return;
                } else if (simInfos.size() == 1) {
                    SimInfoManager.SimInfoRecord simInfo = simInfos.get(0);
                    int state = SIMHelper.getSimIndicatorStateGemini(simInfo.mSimSlotId);
                    Xlog.d(TAG,"toggleState : Siminfo is " + simInfo + " slot is " + simInfo.mSimSlotId + " state is " + state);
                    if (mSwitchListview == null) {
                        mSwitchListview = new SimIconsListView(mContext,Settings.System.GPRS_CONNECTION_SIM_SETTING);
                    }
                    if (mSwitchListview != null && (mSwitchListview.getCount() >= 1)) {
                        final SimItem simDefaultItem = (SimItem) mSwitchListview.getItemAtPosition(0);
                        final SimItem simNeverItem = (SimItem) mSwitchListview.getItemAtPosition(1);
                        if (simDefaultItem != null && simNeverItem != null) {
                            if (SIMHelper.getDefaultSIM(mContext,Settings.System.GPRS_CONNECTION_SIM_SETTING)
                                == Settings.System.GPRS_CONNECTION_SIM_SETTING_NEVER) {
                                if (PhoneConstants.SIM_INDICATOR_RADIOOFF == state) {
                                    Xlog.d(TAG,"toggleState : sim indicator state is radiooff");
                                    return;
                                } else if(PhoneConstants.SIM_INDICATOR_LOCKED == state) {
                                    Xlog.d(TAG,"toggleState : sim indicator state is locked");
                                    unLockSimPin(simInfo.mSimSlotId);
                                    return;
                                } else {
                                    switchDataConnectionMode(simDefaultItem);
                                }
                            } else {
                                switchDataConnectionMode(simNeverItem);
                            }
                        }
                    }
                    return;
                }
                mIsDataDialogShown = true;
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (mSwitchListview == null) {
                            mSwitchListview = new SimIconsListView(mContext,Settings.System.GPRS_CONNECTION_SIM_SETTING);
                        }
                        ViewGroup parent = (ViewGroup) mSwitchListview.getParent();
                        if (parent != null) {
                            parent.removeView(mSwitchListview);
                        }
                        mSwitchDialog = createDialog(mSwitchListview, R.string.mobile);
                        mSwitchListview.initSimList();
                        mSwitchListview.notifyDataChange();
                            mSwitchListview.setOnItemClickListener(new OnItemClickListener() {
                                @Override
                                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                    Xlog.d(TAG, "toggleState, onItemClick");
                                    if (view != null && !view.isEnabled()) {
                                        Xlog.d(TAG, "toggleState, click item is not enabled");
                                        return;
                                    }
                                    final SimItem simItem = (SimItem) parent.getItemAtPosition(position);
                                    if (simItem != null) {
                                        if(simItem.mState == PhoneConstants.SIM_INDICATOR_LOCKED) {
                                            unLockSimPin(simItem.mSlot);
                                            return;
                                        } else if (simItem.mIsSim && simItem.mSimID == SIMHelper.getDefaultSIM(
                                                mContext,
                                                Settings.System.GPRS_CONNECTION_SIM_SETTING)) {
                                            dismissDialogs();
                                            return;
                                        } else if (!simItem.mIsSim && SIMHelper.getDefaultSIM(
                                                mContext,
                                                Settings.System.GPRS_CONNECTION_SIM_SETTING)
                                                == Settings.System.GPRS_CONNECTION_SIM_SETTING_NEVER) {
                                            dismissDialogs();
                                            return;
                                        }
                                        /// M: pop up a warning dialog when the selected is not 3g one.
                                        final int mDataSwitchMsgIndex = (simItem.mSimID > 0)
                                                        ? dataSwitchConfirmDlgMsg(simItem.mSimID)
                                                        : -1;
                                        if (mDataSwitchMsgIndex == -1) {
                                            switchDataConnectionMode(simItem);
                                        } else {
                                            Builder builder = new AlertDialog.Builder(mContext);
                                            builder.setTitle(android.R.string.dialog_alert_title);
                                            builder.setIcon(android.R.drawable.ic_dialog_alert);
                                            builder.setMessage(mContext.getResources().getString(
                                                        mDataSwitchMsgIndex));
                                            builder.setPositiveButton(android.R.string.yes,
                                                    new DialogInterface.OnClickListener() {
                                                        public void onClick(DialogInterface dialog
                                                                , int whichButton) {
                                                            if ((mDataSwitchMsgIndex == 
                                                                R.string.gemini_3g_disable_warning_case0)
                                                                || (mDataSwitchMsgIndex == 
                                                                R.string.gemini_3g_disable_warning_case2)) {
                                                                /// Enable roaming
                                                                enableDataRoaming(simItem.mSimID);
                                                            }
                                                            switchDataConnectionMode(simItem);
                                                        }
                                                    });
                                            builder.setNegativeButton(android.R.string.no,
                                                    new DialogInterface.OnClickListener() {
                                                        public void onClick(DialogInterface dialog
                                                                , int whichButton) {
                                                            dismissDialogs();
                                                        }
                                                    });
                                            dismissDialogs();
                                            mSwitchDialog = builder.create();
                                            mSwitchDialog.getWindow().setType(
                                                    WindowManager.LayoutParams.TYPE_STATUS_BAR_PANEL);
                                            mSwitchDialog.show();
                                        }
                                    } else {
                                        Xlog.e(TAG, "MobileIcon clicked and clicked a null sim item");
                                        return;
                                    }
                                }
                            });
                        mSwitchDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_STATUS_BAR_PANEL);
                        mSwitchDialog.show();
                        mIsDataDialogShown = false;
                    }
                }, ViewConfiguration.getPressedStateDuration());
            } else {
                if (!isWifiOnlyDevice()) {
                    Xlog.d(TAG,"toggleState start");
                    int state = SIMHelper.getSimIndicatorState();
                    Xlog.d(TAG, "MobileStateTracker: toggleState state is " + state);

                    if (PhoneConstants.SIM_INDICATOR_RADIOOFF == state) {
                        Xlog.d(TAG,"toggleState : sim indicator state is radiooff");
                        return;
                    }

                    if (PhoneConstants.SIM_INDICATOR_LOCKED == state) {
                        int currentState = getActualState(context);
                        if (currentState == STATE_DISABLED) {
                            Xlog.d(TAG,"toggleState : sim indicator state is locked");
                            unLockSimPin(0);                            
                            return;
                        }
                    }

                    super.toggleState(mContext);
                    Xlog.d(TAG, "toggleState end");
                }
            }
        }

        private void switchDataConnectionMode(SimItem simItem) {
            /// M: only apply if NOT wifi-only device @{
            if (!isWifiOnlyDevice()) {
            /// M: }@
                mMobileStateTracker.setIsUserSwitching(true);
            /// M: only apply if NOT wifi-only device @{
            }
            /// M: }@
            if (simItem.mIsSim) {
                mGprsTargSim = true;
                mDataTimerHandler.sendEmptyMessageDelayed(EVENT_ATTACH_TIME_OUT, ATTACH_TIME_OUT_LENGTH);
            } else {
                mGprsTargSim = false;
                mDataTimerHandler.sendEmptyMessageDelayed(EVENT_DETACH_TIME_OUT, DETACH_TIME_OUT_LENGTH);
            }
            /// M: only apply if NOT wifi-only device @{
            if (!isWifiOnlyDevice()) {
            /// M: }@
                mMobileIcon.setVisibility(View.GONE);
                int resId = mMobileStateTracker.getInterMedateResource();
                if (resId != -1) {
                    mMobileStateTracker.getSwitchingGifView().setImageResource(resId);
                    mMobileStateTracker.getSwitchingGifView().setVisibility(View.VISIBLE);
                }
                mMobileTileView.setEnabled(false);
            /// M: only apply if NOT wifi-only device @{
            }
            /// M: }@
            AnimationDrawable mFrameDrawable = (AnimationDrawable) getSwitchingGifView().getDrawable();
            if (mFrameDrawable != null && !mFrameDrawable.isRunning()) {
                mFrameDrawable.start();
            }
            Intent intent = new Intent();
            intent.putExtra(PhoneConstants.MULTI_SIM_ID_KEY, simItem.mSimID);
            intent.setAction(Intent.ACTION_DATA_DEFAULT_SIM_CHANGED);
            mContext.sendBroadcast(intent);
            dismissDialogs();
        }

        @Override
        public void onActualStateChange(Context context, Intent intent) {
            if (FeatureOption.MTK_GEMINI_SUPPORT) {
                mIsUserSwitching = false;
                setCurrentState(context, mobileStateToFiveState(intent));
            } else {
                int currentState = getActualState(context);
                if (DBG) {
                    Xlog.d(TAG, "single card onActualStateChange called, currentState is " + currentState);
                }
                setCurrentState(context, currentState);
            }
        }

        private int mobileStateToFiveState(Intent intent) {
            if (FeatureOption.MTK_GEMINI_SUPPORT) {
                PhoneConstants.DataState state = getMobileDataState(intent);
                int simSlotId = intent.getIntExtra(PhoneConstants.GEMINI_SIM_ID_KEY, -1);
                if (DBG) {
                    Xlog.d(TAG, "mobileStateToFiveState simSlotId is : " + simSlotId);
                    Xlog.d(TAG, "mobileStateToFiveState state is : " + state);
                }
                int currentState;
                if (state != null) {
                    switch (state) {
                    case CONNECTED:
                        mDataTimerHandler.removeMessages(EVENT_ATTACH_TIME_OUT);
                        SimInfoManager.SimInfoRecord simInfo = SIMHelper.getSIMInfoBySlot(mContext, simSlotId);
                        if (simInfo == null) {
                            Xlog.e(TAG, "MobileStateTracker mobileStateToFiveState error for simInfo, slotId is "
                                    + simSlotId);
                            return STATE_UNKNOWN;
                        }
                        currentState = STATE_ENABLED;
                        break;
                    case DISCONNECTED:
                        mDataTimerHandler.removeMessages(EVENT_DETACH_TIME_OUT);
                        currentState = STATE_DISABLED;
                        break;
                    default:
                        currentState = STATE_UNKNOWN;
                    }
                } else {
                    currentState = STATE_UNKNOWN;
                }
                return currentState;
            } else {
                return STATE_UNKNOWN;
            }
        }

        @Override
        public void requestStateChange(final Context context,
                final boolean desiredState) {
             if (!FeatureOption.MTK_GEMINI_SUPPORT) {                
                final ContentResolver resolver = context.getContentResolver();
                new AsyncTask<Void, Void, Boolean>() {
                    @Override
                    protected Boolean doInBackground(Void... args) {
                        ConnectivityManager cm = (ConnectivityManager)
                            mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
                        boolean enabled = cm.getMobileDataEnabled();
                        cm.setMobileDataEnabled(!enabled);
                        return null;
                    }
                }.execute();
             }
        }

        public int getDisabledResource() {
            return R.drawable.ic_qs_mobile_off;
        }

        public int getEnabledResource() {
            if (FeatureOption.MTK_GEMINI_SUPPORT) {
                long simId = SIMHelper.getDefaultSIM(mContext, Settings.System.GPRS_CONNECTION_SIM_SETTING);
                if (simId < 0) {
                    Xlog.e(TAG, "Mobile StateTracker getEnabledResource error, selected simId is " + simId);
                    return -1;
                } else if (simId == 0) {
                    return getDisabledResource();
                } else {
                    SimInfoManager.SimInfoRecord simInfo = SIMHelper.getSIMInfo(mContext, simId);
                    if (simInfo == null) {
                        Xlog.e(TAG, "Mobile StateTracker getEnabledResource error, selected simId is " + simId);
                        return -1;
                    }
                    int slotId = simInfo.mSimSlotId;
                    if (isRadioOff(slotId)) {
                        return R.drawable.ic_qs_mobile_disable;
                    } else {
                        return SIMHelper.getDataConnectionIconIdBySlotId(mContext, slotId);
                    }
                }
            } else {
                if (isRadioOff(0)) {
                    return R.drawable.ic_qs_mobile_disable;
                } else {
                    return R.drawable.ic_qs_mobile_enable;
                }
            }
        }

        public ImageView getImageButtonView() {
            return mMobileIcon;
        }

        public View getTileView() {
            return mMobileTileView;
        }

        @Override
        public int getInterMedateResource() {
            return R.drawable.ic_qs_stat_sys_mobile_switch_anim;
        }
        
        @Override
        public ImageView getSwitchingGifView() {
            if (mDataConnSwitchIngGifView == null) {
                ViewGroup.LayoutParams layoutParam = new ViewGroup.LayoutParams(
                        LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
                mDataConnSwitchIngGifView = new ImageView(mContext);
                mDataConnLayout.addView(mDataConnSwitchIngGifView, 0);
                mDataConnSwitchIngGifView.setVisibility(View.GONE);
            }
            return mDataConnSwitchIngGifView;
        }

        @Override
        public ImageView getIndicatorView() {
            return null;
        }
    }

    /**
     * M: Subclass of StateTracker for GPS state.
     */
    private final class AirlineModeStateTracker extends StateTracker {
        private boolean mAirPlaneModeClickable = true;
        public void setAirPlaneModeClickable(boolean enable) {
            if (DBG) {
                Xlog.d(TAG, "setAirPlaneModeClickable called, enabled is: " + enable);
            }
            mAirPlaneModeClickable = enable;
        }

        @Override
        public int getActualState(Context context) {
            return isAirplaneModeOn(mContext) ? STATE_ENABLED : STATE_DISABLED;
        }

        @Override
        public void onActualStateChange(Context context, Intent intent) {
            if (intent == null) {
                return;
            }
            boolean enabled = intent.getBooleanExtra("state", false);
            setCurrentState(context, enabled ? STATE_ENABLED : STATE_DISABLED);
        }
        
        @Override
        public void toggleState(Context context) {
            if (getIsUserSwitching()) {
                Xlog.d(TAG, "toggleState user is swithing, so just return");
                return;
            }
            if (Boolean.parseBoolean(SystemProperties.get(TelephonyProperties.PROPERTY_INECM_MODE))) {
                /// M: Launch ECM exit dialog
                Intent ecmDialogIntent = new Intent(TelephonyIntents.ACTION_SHOW_NOTICE_ECM_BLOCK_OTHERS, null);
                ecmDialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(ecmDialogIntent);
            } else {
                boolean airlineMode = isAirplaneModeOn(mContext);
	            if (FeatureOption.MTK_WLAN_SUPPORT) {
                    if (PluginFactory.getStatusBarPlugin(mContext).supportDisableWifiAtAirplaneMode()) {
                        mWifiStateTracker.setAirlineMode(!airlineMode);
                        mWifiTileView.setEnabled(mWifiStateTracker.isClickable());
                    }
                }
                setIsUserSwitching(true);
                mAirlineModeTileView.setEnabled(isClickable());
                Xlog.d(TAG, "Airplane toogleState: " + isClickable() + ", current airlineMode is " + airlineMode);
                Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON,
                        airlineMode ? 0 : 1);
                Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
                intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
                intent.putExtra("state", !airlineMode);
                mContext.sendBroadcast(intent);
            }
        }

        @Override
        public void requestStateChange(final Context context, final boolean desiredState) {
            /// M: Do nothing, for we have done all operation in toggleState
        }

        public int getDisabledResource() {
            return R.drawable.ic_qs_flight_mode_off;
        }

        public int getEnabledResource() {
            return R.drawable.ic_qs_flight_mode_on;
        }

        public ImageView getImageButtonView() {
            return mAirlineModeIcon;
        }

        public View getTileView() {
            return mAirlineModeTileView;
        }

        public boolean isClickable() {
            Xlog.d(TAG, "mAirPlaneModeClickable is " + mAirPlaneModeClickable + " super.isClickable is "
                    + super.isClickable());
            return mAirPlaneModeClickable && super.isClickable();
        }

        @Override
        public ImageView getIndicatorView() {
            return null;
        }
    }
    
    /**
     * M: Subclass of StateTracker to get/set Bluetooth state.
     */
    private final class TimeoutStateTracker extends StateTracker {
        @Override
        public int getActualState(Context context) {
            return STATE_ENABLED;
        }

        @Override
        protected void requestStateChange(Context context, final boolean desiredState) {
            setCurrentState(context, STATE_ENABLED);
        }

        @Override
        public void onActualStateChange(Context context, Intent intent) {
            setCurrentState(context, STATE_ENABLED);
        }

        public int getDisabledResource() {
            return R.drawable.ic_qs_timeout_off;
        }

        public int getEnabledResource() {
            return R.drawable.ic_qs_timeout_on;
        }

        public ImageView getImageButtonView() {
            return mTimeoutIcon;
        }

        public View getTileView() {
            return mTimeoutTileView;
        }

        @Override
        public ImageView getIndicatorView() {
            int brightness = getTimeout(mContext);
            switch (brightness) {
            case MINIMUM_TIMEOUT:
                mTimeoutIndicator.setImageResource(R.drawable.ic_qs_light_low);
                break;
            case MEDIUM_TIMEOUT:
                mTimeoutIndicator.setImageResource(R.drawable.ic_qs_light_middle);
                break;
            case MAXIMUM_TIMEOUT:
                mTimeoutIndicator.setImageResource(R.drawable.ic_qs_light_fully);
                break;
            default:
                break;
            }
            return mTimeoutIndicator;
        }
        
        @Override
        public void toggleState(Context context) {
            toggleTimeout(context);
        }
    }

    /**
     * M: Subclass of StateTracker for AutoRotation state.
     */
    private final class AutoRotationStateTracker extends StateTracker {
        @Override
        public int getActualState(Context context) {
            int state = Settings.System.getInt(context.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, -1);
            if (state == 1) {
                return STATE_ENABLED;
            } else if (state == 0) {
                return STATE_DISABLED;
            } else {
                return STATE_UNKNOWN;
            }
        }

        @Override
        public void onActualStateChange(Context context, Intent unused) {
            /// M: Note: the broadcast location providers changed intent
            /// doesn't include an extras bundles saying what the new value is.
            setCurrentState(context, getActualState(context));
        }

        @Override
        public void requestStateChange(final Context context, final boolean desiredState) {
            final ContentResolver resolver = context.getContentResolver();
            new AsyncTask<Void, Void, Boolean>() {
                @Override
                protected Boolean doInBackground(Void... args) {
                    Settings.System.putInt(context.getContentResolver(),
                            Settings.System.ACCELEROMETER_ROTATION, desiredState ? 1 : 0);
                    return desiredState;
                }

                @Override
                protected void onPostExecute(Boolean result) {
                    setCurrentState(context, result ? STATE_ENABLED : STATE_DISABLED);
                }
            }.execute();
        }

        public int getDisabledResource() {
            return R.drawable.ic_qs_auto_rotation_off;
        }

        public int getEnabledResource() {
            return R.drawable.ic_qs_auto_rotation_enable;
        }

        public ImageView getImageButtonView() {
            return mAutoRotateIcon;
        }
        
        @Override
        public ImageView getIndicatorView() {
            return null;
        }
        
        public View getTileView() {
            return mAutoRotateTileView;
        }
    }

    public void initConfigurationState() {
        boolean isAirlineModeOn = isAirplaneModeOn(mContext);
        if (FeatureOption.MTK_WLAN_SUPPORT) {
            if (PluginFactory.getStatusBarPlugin(mContext).supportDisableWifiAtAirplaneMode()) {
                mWifiStateTracker.setAirlineMode(isAirlineModeOn);
            }
            mWifiStateTracker.setImageViewResources(mContext);
        }
        if (FeatureOption.MTK_BT_SUPPORT) {
            mBluetoothStateTracker.setImageViewResources(mContext);
        }
        if (FeatureOption.MTK_GPS_SUPPORT) {
            mGpsStateTracker.setImageViewResources(mContext);
        }
        mAirlineModeStateTracker.setImageViewResources(mContext);
        /// M: only apply if NOT wifi-only device @{
        if (!isWifiOnlyDevice()) {
        /// M: }@
            mMobileStateTracker.setAirlineMode(isAirlineModeOn);
            mMobileStateTracker.setHasSim(false);
            mMobileStateTracker.setCurrentState(mContext, StateTracker.STATE_DISABLED);
            mMobileStateTracker.setImageViewResources(mContext);
            
            mSimCardReady = SystemProperties.getBoolean(TelephonyProperties.PROPERTY_SIM_INFO_READY, false);
            if (mSimCardReady) {
                Xlog.d(TAG, "Oops, sim ready, maybe phone is drop down and restarted");
                List<SimInfoManager.SimInfoRecord> simInfos = SIMHelper.getSIMInfoList(mContext);
                if (simInfos == null || simInfos.size() <= 0) {
                    mMobileStateTracker.setHasSim(false);
                } else {
                    mMobileStateTracker.setHasSim(true);
                }
                mMobileTileView.setEnabled(mMobileStateTracker.isClickable());
                mMobileStateTracker.setImageViewResources(mContext);
            }
        /// M: only apply if NOT wifi-only device @{
        }
        /// M: }@
        mTimeoutStateTracker.setImageViewResources(mContext);
        mAutoRotationStateTracker.setImageViewResources(mContext);
        if (mProfileManager.getActiveProfileKey() != null) {
            updateProfileView(AudioProfileManager.getScenario(mProfileManager.getActiveProfileKey()));
        }
    }
    
    public static boolean isAirplaneModeOn(Context context) {
        return Settings.Global.getInt(context.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
    }
    
    public void setStatusBarService(PhoneStatusBar statusBarService) {
        mStatusBarService = statusBarService;
    }
    
    public void updateResources() {
        if (FeatureOption.MTK_WLAN_SUPPORT) {
            TextView wifiText = (TextView) mWifiTileView.findViewById(R.id.wifi_textview);
            wifiText.setText(R.string.wifi);
        }
        if (FeatureOption.MTK_BT_SUPPORT) {
            TextView bluetoothText = (TextView) mBluetoothTileView.findViewById(R.id.bluetooth_textview);
            bluetoothText.setText(R.string.bluetooth);
        }
        if (FeatureOption.MTK_GPS_SUPPORT) {
            TextView gpsText = (TextView) mGpsTileView.findViewById(R.id.gps_textview);
            gpsText.setText(R.string.gps);
        }
        /// M: only apply if NOT wifi-only device @{
        if (!isWifiOnlyDevice()) {
        /// M: }@
            TextView dataConnText = (TextView) mMobileTileView.findViewById(R.id.data_conn_textview);
            dataConnText.setText(R.string.mobile);
        /// M: only apply if NOT wifi-only device @{
        }
        /// M: }@
        TextView timeoutText = (TextView) mTimeoutTileView.findViewById(R.id.timeout_textview);
        timeoutText.setText(R.string.timeout);
        TextView autoRotateText = (TextView) mAutoRotateTileView.findViewById(R.id.auto_rotate_textview);
        autoRotateText.setText(R.string.autorotate);
        TextView audioProfileText = (TextView) mAudioProfileTileView.findViewById(R.id.audio_profile_textview);
        audioProfileText.setText(R.string.audio_profile);
        TextView airPlaneModeText = (TextView) mAirlineModeTileView.findViewById(R.id.airplane_mode_textview);
        airPlaneModeText.setText(R.string.offline);
        if (mSwitchDialog != null) {
            mSwitchDialog.setTitle(R.string.mobile);
        }
        if (mSwitchListview != null) {
            mSwitchListview.updateResources();
        }

        // Reset the dialog
        boolean isProfileSwitchDialogVisible = false;
        if (mProfileSwitchDialog != null) {
            removeAllProfileSwitchDialogCallbacks();

            isProfileSwitchDialogVisible = mProfileSwitchDialog.isShowing();
            mProfileSwitchDialog.dismiss();
        }
        mProfileSwitchDialog = null;
        if (isProfileSwitchDialogVisible) {
            showProfileSwitchDialog();
        }
    }

    public void setQuickSettingsTileView(View wifiTileView, View bluetoothTileView, View gpsTileView, View mobileTileView,
            View airlineModeTileView, View timeoutTileView, View autoRotateTile, View audioProfileTile) {
        mWifiTileView = wifiTileView;
        mBluetoothTileView = bluetoothTileView;
        mGpsTileView = gpsTileView;
        mMobileTileView = mobileTileView;
        mAirlineModeTileView = airlineModeTileView;
        mTimeoutTileView = timeoutTileView;
        mAutoRotateTileView = autoRotateTile;
        mAudioProfileTileView = audioProfileTile;
        initLayout();
        initIconViews();
    }

    private void initLayout() {
        mDataConnLayout = (FrameLayout) mMobileTileView.findViewById(R.id.data_conn_layout);
        mBluetoothLayout = (FrameLayout) mBluetoothTileView.findViewById(R.id.bluetooth_layout);
        mWifiLayout = (FrameLayout) mWifiTileView.findViewById(R.id.wifi_layout);
    }

    private void initIconViews() {
        mWifiIcon = (ImageView) mWifiTileView.findViewById(R.id.wifi_image);
        mBluetoothIcon = (ImageView) mBluetoothTileView.findViewById(R.id.bluetooth_image);
        mGpsIcon = (ImageView) mGpsTileView.findViewById(R.id.gps_image);
        mMobileIcon = (ImageView) mMobileTileView.findViewById(R.id.data_conn_image);
        mAirlineModeIcon = (ImageView) mAirlineModeTileView.findViewById(R.id.airplane_mode_image);
        mTimeoutIcon = (ImageView) mTimeoutTileView.findViewById(R.id.timeout_image);
        mAutoRotateIcon = (ImageView) mAutoRotateTileView.findViewById(R.id.auto_rotate_image);
        mTimeoutIndicator = (ImageView) mTimeoutTileView.findViewById(R.id.on_indicator);
        mAudioProfileIcon = (ImageView) mAudioProfileTileView.findViewById(R.id.audio_profile_image);
    }

    private ContentObserver mTimeoutChangeObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            mTimeoutStateTracker.onActualStateChange(mContext, null);
            mTimeoutStateTracker.setImageViewResources(mContext);
        }
    };
    
    private ContentObserver mAutoRotationChangeObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            mAutoRotationStateTracker.onActualStateChange(mContext, null);
            mAutoRotationStateTracker.setImageViewResources(mContext);
        }
    };
    
    /**
     * M: Increases or decreases the brightness.
     * @param context
     */
    private void toggleTimeout(Context context) {
        try {
            ContentResolver cr = context.getContentResolver();
            int timeout = Settings.System.getInt(cr, SCREEN_OFF_TIMEOUT, FALLBACK_SCREEN_TIMEOUT_VALUE);
            if (DBG) {
                Xlog.d(TAG, "toggleTimeout, before is " + timeout);
            }
            if (timeout <= MINIMUM_TIMEOUT) {
                timeout = MEDIUM_TIMEOUT;
            } else if (timeout <= MEDIUM_TIMEOUT) {
                timeout = MAXIMUM_TIMEOUT;
            } else {
                timeout = MINIMUM_TIMEOUT;
            }
            Settings.System.putInt(cr, Settings.System.SCREEN_OFF_TIMEOUT, timeout);
            if (DBG) {
                Xlog.d(TAG, "toggleTimeout, after is " + timeout);
            }
        } catch (Exception e) {
            Xlog.d(TAG, "toggleTimeout: " + e);
        }
    }
    
    /**
     * M: Gets state of brightness.
     * @param context
     * @return true if more than moderately bright.
     */
    public static int getTimeout(Context context) {
        try {
            int timeout = Settings.System.getInt(context.getContentResolver()
                    , SCREEN_OFF_TIMEOUT, FALLBACK_SCREEN_TIMEOUT_VALUE);
            if (timeout <= MINIMUM_TIMEOUT) {
                timeout = MINIMUM_TIMEOUT;
            } else if (timeout <= MEDIUM_TIMEOUT) {
                timeout = MEDIUM_TIMEOUT;
            } else {
                timeout = MAXIMUM_TIMEOUT;
            }
            return timeout;
        } catch (Exception e) {
            Xlog.d(TAG, "getTimeout: " + e);
        }
        return MEDIUM_TIMEOUT;
    }
    
    private View.OnClickListener mProfileSwitchListener = new View.OnClickListener() {
        public void onClick(View v) {
            for (int i = 0; i < mProfileKeys.size(); i++) {
                if (v.getTag().equals(mProfileKeys.get(i))) {
                    if (DBG) {
                        Xlog.d(TAG, "onClick called, profile clicked is: " + mProfileKeys.get(i));
                    }
                    String key = mProfileKeys.get(i);
                    updateAudioProfile(key);
                    Scenario senario = AudioProfileManager.getScenario(key);
                    updateProfileView(senario);
                    if (mProfileSwitchDialog != null) {
                        mProfileSwitchDialog.dismiss();
                    }
                }
            }
        }
    };

    private AudioProfileListener mAudioProfileListenr = new AudioProfileListener() {
        @Override
        public void onAudioProfileChanged(String profileKey) {
            if (profileKey != null) {
                if (!mUpdating) {
                    /// M: AudioProfile is no ready, so skip update
                    return;
                }
                Scenario senario = AudioProfileManager.getScenario(profileKey);
                if (DBG) {
                    Xlog.d(TAG, "onReceive called, profile type is: " + senario);
                }
                if (senario != null) {
                    updateProfileView(senario);
                }
            }
        }
    };

    private void updateProfileView(Scenario scenario) {
        loadDisabledProfileResouceForAll();
        loadEnabledProfileResource(scenario);
    }

    private void loadDisabledProfileResouceForAll() {
        mNormalProfileIcon.setImageResource(R.drawable.ic_qs_normal_off);
        mMettingProfileIcon.setImageResource(R.drawable.ic_qs_meeting_profile_off);
        mOutdoorSwitchIcon.setImageResource(R.drawable.ic_qs_outdoor_off);
        mMuteProfileIcon.setImageResource(R.drawable.ic_qs_mute_profile_off);
    }

    private void loadEnabledProfileResource(Scenario scenario) {
        if (DBG) {
            Xlog.d(TAG, "loadEnabledProfileResource called, profile is: " + scenario);
        }
        mCurrentScenario = scenario;
        switch (scenario) {
        case GENERAL:
            mNormalProfileIcon.setImageResource(R.drawable.ic_qs_normal_profile_enable);
            mAudioProfileIcon.setImageResource(R.drawable.ic_qs_general_on);
            break;
        case MEETING:
            mMettingProfileIcon.setImageResource(R.drawable.ic_qs_meeting_profile_enable);
            mAudioProfileIcon.setImageResource(R.drawable.ic_qs_meeting_on);
            break;
        case OUTDOOR:
            mOutdoorSwitchIcon.setImageResource(R.drawable.ic_qs_outdoor_profile_enable);
            mAudioProfileIcon.setImageResource(R.drawable.ic_qs_outdoor_on);
            break;
        case SILENT:
            mMuteProfileIcon.setImageResource(R.drawable.ic_qs_mute_profile_enable);
            mAudioProfileIcon.setImageResource(R.drawable.ic_qs_silent_on);
            break;
        case CUSTOM:
            mAudioProfileIcon.setImageResource(R.drawable.ic_qs_custom_on);
        default:
            mAudioProfileIcon.setImageResource(R.drawable.ic_qs_custom_on);
            break;
        }
    }

    private void updateAudioProfile(String key) {
        if (key == null) {
            return;
        }
        if (DBG) {
            Xlog.i(TAG, "updateAudioProfile called, selected profile is: " + key);
        }
        mProfileManager.setActiveProfile(key);
        if (DBG) {
            Xlog.d(TAG, "updateAudioProfile called, setActiveProfile is: " + key);
        }
    }
    
    private void showProfileSwitchDialog() {
        createProfileSwitchDialog();
        if (!mProfileSwitchDialog.isShowing()) {
            try {
                WindowManagerGlobal.getWindowManagerService().dismissKeyguard();
            } catch (RemoteException e) {
            }
            mProfileSwitchDialog.show();
            dismissProfileSwitchDialog(PROFILE_SWITCH_DIALOG_LONG_TIMEOUT);
        }
    }
    
    private void createProfileSwitchDialog() {
        if (mProfileSwitchDialog == null) {
            mProfileSwitchDialog = new Dialog(mContext);
            mProfileSwitchDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            mProfileSwitchDialog.setContentView(R.layout.quick_settings_profile_switch_dialog);
            mProfileSwitchDialog.setCanceledOnTouchOutside(true);
            mProfileSwitchDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_STATUS_BAR_PANEL);
            mProfileSwitchDialog.getWindow().getAttributes().privateFlags |=
                    WindowManager.LayoutParams.PRIVATE_FLAG_SHOW_FOR_ALL_USERS;
            mProfileSwitchDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            
            mMettingProfileIcon = (ImageView) mProfileSwitchDialog.findViewById(R.id.meeting_profile_icon);
            mOutdoorSwitchIcon = (ImageView) mProfileSwitchDialog.findViewById(R.id.outdoor_profile_icon);
            mMuteProfileIcon = (ImageView) mProfileSwitchDialog.findViewById(R.id.mute_profile_icon);
            mNormalProfileIcon = (ImageView) mProfileSwitchDialog.findViewById(R.id.normal_profile_icon);
            View normalProfile = (View) mProfileSwitchDialog.findViewById(R.id.normal_profile);
            normalProfile.setOnClickListener(mProfileSwitchListener);
            normalProfile.setTag(AudioProfileManager.getProfileKey(Scenario.GENERAL));
            View muteProfile = (View) mProfileSwitchDialog.findViewById(R.id.mute_profile);
            muteProfile.setOnClickListener(mProfileSwitchListener);
            muteProfile.setTag(AudioProfileManager.getProfileKey(Scenario.SILENT));
            View meetingProfile = (View) mProfileSwitchDialog.findViewById(R.id.meeting_profile);
            meetingProfile.setOnClickListener(mProfileSwitchListener);
            meetingProfile.setTag(AudioProfileManager.getProfileKey(Scenario.MEETING));
            View outdoorProfile = (View) mProfileSwitchDialog.findViewById(R.id.outdoor_profile);
            outdoorProfile.setOnClickListener(mProfileSwitchListener);
            outdoorProfile.setTag(AudioProfileManager.getProfileKey(Scenario.OUTDOOR));
            if (mCurrentScenario != null) {
                loadEnabledProfileResource(mCurrentScenario);
            }
        }
    }

    private void dismissProfileSwitchDialog(int timeout) {
        removeAllProfileSwitchDialogCallbacks();
        if (mProfileSwitchDialog != null) {
            mHandler.postDelayed(mDismissProfileSwitchDialogRunnable, timeout);
        }
    }

    private Runnable mDismissProfileSwitchDialogRunnable = new Runnable() {
        public void run() {
            if (mProfileSwitchDialog != null && mProfileSwitchDialog.isShowing()) {
                mProfileSwitchDialog.dismiss();
            }
            removeAllProfileSwitchDialogCallbacks();
        };
    };

    private void removeAllProfileSwitchDialogCallbacks() {
        mHandler.removeCallbacks(mDismissProfileSwitchDialogRunnable);
    }

    private void stopFrameAnim() {
        ImageView animView = mMobileStateTracker.getSwitchingGifView();
        if (animView != null) {
            animView.setVisibility(View.GONE);
            AnimationDrawable mFrameDrawable = (AnimationDrawable) animView.getDrawable();
            if (mFrameDrawable != null && mFrameDrawable.isRunning()) {
                mFrameDrawable.stop();
            }
        }
    }

}
