package com.mediatek.wifi;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Telephony;
import android.provider.Telephony.SIMInfo;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.TelephonyProperties;
import com.android.settings.AirplaneModeEnabler;
import com.android.settings.ApnPreference;
import com.android.settings.R;
import com.android.settings.wifi.WifiSettings;

import com.mediatek.CellConnService.CellConnMgr;
import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.gemini.GeminiUtils;
import com.mediatek.telephony.TelephonyManagerEx;
import com.mediatek.xlog.Xlog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WifiGprsSelector extends WifiSettings implements Preference.OnPreferenceChangeListener {
    private static final String TAG = "WifiGprsSelector";
    private static final String KEY_APN_LIST = "apn_list";
    private static final String KEY_ADD_WIFI_NETWORK = "add_network";
    private static final String KEY_DATA_ENABLER = "data_enabler";
    private static final String KEY_DATA_ENABLER_GEMINI = "data_enabler_gemini";
    private static final String KEY_DATA_ENABLER_CATEGORY = "data_enabler_category";
    private static final int DIALOG_WAITING = 1001;
    
    //time out message event
    private static final int EVENT_DETACH_TIME_OUT = 2000;
    private static final int EVENT_ATTACH_TIME_OUT = 2001;
    //time out length
    private static final int DETACH_TIME_OUT_LENGTH = 10000;
    private static final int ATTACH_TIME_OUT_LENGTH = 30000;
 
    private static final int SIM_CARD_1 = 0;
    private static final int SIM_CARD_2 = 1;
    private static final int SIM_CARD_SINGLE = 2;
    private static final int SIM_CARD_UNDEFINED = -1;
    private static final int ID_INDEX = 0;
    private static final int NAME_INDEX = 1;
    private static final int APN_INDEX = 2;
    private static final int TYPES_INDEX = 3;
    private static final int SOURCE_TYPE_INDEX = 4;

    private static final int COLOR_INDEX_ZERO = 0;
    private static final int COLOR_INDEX_SEVEN = 7;
    private static final int COLOR_INDEX_EIGHT = 8;
    private static final int SIM_NUMBER_LEN = 4;

    private static final String[] PROJECTION_ARRAY = new String[] {
        Telephony.Carriers._ID,     // 0
        Telephony.Carriers.NAME,    // 1
        Telephony.Carriers.APN,     // 2
        Telephony.Carriers.TYPE,    // 3
        Telephony.Carriers.SOURCE_TYPE,    // 4
    };

    private boolean mIsCallStateIdle = true;
    private boolean mAirplaneModeEnabled = false;

    private TelephonyManager mTelephonyManager;
    private String mSelectedKey;

    private IntentFilter mMobileStateFilter;
    private static final String TRANSACTION_START = "com.android.mms.transaction.START";
    private static final String TRANSACTION_STOP = "com.android.mms.transaction.STOP";

    private static final String PREFERRED_APN_URI = "content://telephony/carriers/preferapn";
    private static final String PREFERRED_APN_URI_GEMINI_SIM1 = "content://telephony/carriers_sim1/preferapn";
    private static final String PREFERRED_APN_URI_GEMINI_SIM2 = "content://telephony/carriers_sim2/preferapn";
    private static final Uri PREFERAPN_URI = Uri.parse(PREFERRED_APN_URI);
    private static final Uri PREFERAPN_URI_GEMINI_SIM1 = Uri.parse(PREFERRED_APN_URI_GEMINI_SIM1);
    private static final Uri PREFERAPN_URI_GEMINI_SIM2 = Uri.parse(PREFERRED_APN_URI_GEMINI_SIM2);
 
    private static final String APN_ID = "apn_id";

    private int mSimSlot;
    private Uri mUri;
    private Uri mRestoreCarrierUri;
 
    private PreferenceCategory mApnList;
    private Preference mAddWifiNetwork;
    private CheckBoxPreference mDataEnabler;
    private Preference mDataEnablerGemini;
    private boolean mIsSIMExist = true;
    private WifiManager mWifiManager;

    private static final int DISPLAY_NONE = 0;
    private static final int DISPLAY_FIRST_FOUR = 1;
    private static final int DISPLAY_LAST_FOUR = 2;  
    private static final int PIN1_REQUEST_CODE = 302;
    private Map<Long,SIMInfo> mSimMap;
    private List<Long> mSimMapKeyList = null;
    private TelephonyManagerEx mTelephonyManagerEx;
    private CellConnMgr mCellConnMgr;
    private int mInitValue;
    private boolean mScreenEnable = true;
    private boolean mIsGprsSwitching = false;
    
    private final BroadcastReceiver mMobileStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction(); 
            if (action.equals(TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED)) {
                String reason = intent.getStringExtra(PhoneConstants.STATE_CHANGE_REASON_KEY);
                PhoneConstants.DataState state = getMobileDataState(intent);
                Xlog.d(TAG, "catch data change, reason : " + reason + "  state = " + state + ";");
                if (reason == null) {
                    return;
                }
                if (reason.equals(Phone.REASON_DATA_ENABLED) &&
                        (state == PhoneConstants.DataState.CONNECTED) && (mIsGprsSwitching)) {
                    mTimeHandler.removeMessages(EVENT_ATTACH_TIME_OUT);
                    if (isResumed()) {
                        removeDialog(DIALOG_WAITING);
                    }
                    mIsGprsSwitching = false;
                    updateDataEnabler();
                } else if (reason.equals(Phone.REASON_DATA_DISABLED) &&
                        (state == PhoneConstants.DataState.DISCONNECTED) && (mIsGprsSwitching)) {
                    mTimeHandler.removeMessages(EVENT_DETACH_TIME_OUT);
                    if (isResumed()) {
                        removeDialog(DIALOG_WAITING);
                    }
                    mIsGprsSwitching = false;
                    updateDataEnabler();
                }
            } else if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
                mAirplaneModeEnabled = intent.getBooleanExtra("state", false);
                Xlog.d(TAG, "AIRPLANE_MODE state changed: " + mAirplaneModeEnabled + ";");
                mApnList.setEnabled(!mAirplaneModeEnabled);
                updateDataEnabler();
            } else if (action.equals(TRANSACTION_START)) {
                Xlog.d(TAG, "ssr: TRANSACTION_START in ApnSettings" + ";");
                mScreenEnable = false;
                mApnList.setEnabled(!mAirplaneModeEnabled && mScreenEnable);
            } else if (action.equals(TRANSACTION_STOP)) {
                Xlog.d(TAG, "ssr: TRANSACTION_STOP in ApnSettings" + ";");
                mScreenEnable = true;
                mApnList.setEnabled(!mAirplaneModeEnabled && mScreenEnable);
            } else if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                handleWifiStateChanged(intent.getIntExtra(
                        WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN));
            }  else if (TelephonyIntents.ACTION_SIM_INFO_UPDATE.equals(action)){
                Xlog.d(TAG,"receive ACTION_SIM_INFO_UPDATE");
                List<SIMInfo> simList = SIMInfo.getInsertedSIMList(getActivity());
                if (simList != null) {
                    mSimSlot = getSimSlot();
                    updateDataEnabler();
                }
            }
        }
    };

    ContentObserver mGprsConnectObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            Xlog.i(TAG, "Gprs connection changed");
            mSimSlot = getSimSlot();
            updateDataEnabler();
        }
    };
    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {

        @Override
        public void onServiceStateChanged(ServiceState serviceState) {
            super.onServiceStateChanged(serviceState);
            
             mIsCallStateIdle = 
                mTelephonyManager.getCallState() == TelephonyManager.CALL_STATE_IDLE;
        }

    };
    private Runnable mServiceComplete = new Runnable() {
        public void run() {

        }
    };
    Handler mTimeHandler = new Handler() {
        @Override
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case EVENT_ATTACH_TIME_OUT:
                    Xlog.d(TAG, "attach time out......");
                    if (isResumed()) {
                        removeDialog(DIALOG_WAITING);
                    }
                    mIsGprsSwitching = false;
                    updateDataEnabler();
                    break;
                case EVENT_DETACH_TIME_OUT:
                    Xlog.d(TAG, "detach time out......");
                    if (isResumed()) {
                        removeDialog(DIALOG_WAITING);
                    }
                    mIsGprsSwitching = false;
                    updateDataEnabler();
                    break;
                default:
                    break;
            }
        };
    };

    private static PhoneConstants.DataState getMobileDataState(Intent intent) {
        String str = intent.getStringExtra(PhoneConstants.STATE_KEY);
        if (str == null) {
            return PhoneConstants.DataState.DISCONNECTED;
        } else {
            return Enum.valueOf(PhoneConstants.DataState.class, str);
        }
    }
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Xlog.d(TAG, "onActivityCreated()");
        //addPreferencesFromResource(R.xml.wifi_access_points_and_gprs);
        mApnList = (PreferenceCategory)findPreference(KEY_APN_LIST);
        mAddWifiNetwork = findPreference(KEY_ADD_WIFI_NETWORK);
  
        PreferenceCategory dataEnableCategory =
            (PreferenceCategory)findPreference(KEY_DATA_ENABLER_CATEGORY);
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            mDataEnablerGemini = findPreference(KEY_DATA_ENABLER_GEMINI);
            dataEnableCategory.removePreference(findPreference(KEY_DATA_ENABLER));
        } else {
            mDataEnabler = (CheckBoxPreference)findPreference(KEY_DATA_ENABLER);
            mDataEnabler.setOnPreferenceChangeListener(this);
            dataEnableCategory.removePreference(findPreference(KEY_DATA_ENABLER_GEMINI));
        }



        initPhoneState();
        mMobileStateFilter = new IntentFilter(
                TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED);
        mMobileStateFilter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        mMobileStateFilter.addAction(TRANSACTION_START);
        mMobileStateFilter.addAction(TRANSACTION_STOP);
        mMobileStateFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        mMobileStateFilter.addAction(TelephonyIntents.ACTION_SIM_INFO_UPDATE);
        getActivity().setTitle(R.string.wifi_gprs_selector_title);

        init();
        setHasOptionsMenu(false);
    }
    
    @Override
    public void onResume() {
        Xlog.d(TAG,"onResume");
        super.onResume();
        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_SERVICE_STATE);
        getActivity().registerReceiver(mMobileStateReceiver, mMobileStateFilter);
        mAirplaneModeEnabled =  AirplaneModeEnabler.isAirplaneModeOn(getActivity());
        /// M: WifiManager memory leak , change context to getApplicationContext @{
        mWifiManager = (WifiManager) getActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        ///@}
        handleWifiStateChanged(mWifiManager.getWifiState());
        
        mScreenEnable = isMMSNotTransaction();
        
        fillList(mSimSlot);
        updateDataEnabler();
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            mCellConnMgr = new CellConnMgr(mServiceComplete);
            mCellConnMgr.register(getActivity());
            getContentResolver().registerContentObserver(Settings.System.getUriFor(
                Settings.System.GPRS_CONNECTION_SIM_SETTING), false, mGprsConnectObserver);
        }
        if(mIsGprsSwitching) {
            showDialog(DIALOG_WAITING);
        }
    }
    private boolean isMMSNotTransaction() {
        boolean isMMSNotProcess = true;
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo networkInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE_MMS);
            if (networkInfo != null) {
                NetworkInfo.State state = networkInfo.getState();
                Xlog.d(TAG,"mms state = " + state);
                isMMSNotProcess = (state != NetworkInfo.State.CONNECTING
                    && state != NetworkInfo.State.CONNECTED);
            }
        }
        return isMMSNotProcess;
    }
    private boolean init() {         
        Xlog.d(TAG, "init()");
        ITelephony mITelephony = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
        if (null == mITelephony) {
            return false;
        }
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            try {
                mIsSIMExist = mITelephony.isSimInsert(PhoneConstants.GEMINI_SIM_1) ||
                                mITelephony.isSimInsert(PhoneConstants.GEMINI_SIM_2);
            } catch (RemoteException e) {
                Xlog.d(TAG, "RemoteException happens......");
                return false;
            }
        } else {
            try {
                mIsSIMExist = mITelephony.isSimInsert(PhoneConstants.GEMINI_SIM_1);
                Xlog.d(TAG, "Is SIM exist?" + mIsSIMExist + ";");
            } catch (RemoteException e) {
                Xlog.d(TAG, "RemoteException happens......");
                return false;
            }
        }
        return true;
    }
    
    @Override
    public void onPause() {
        Xlog.d(TAG,"onPause");
        super.onPause();
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
        getActivity().unregisterReceiver(mMobileStateReceiver);
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            mCellConnMgr.unregister();
            getContentResolver().unregisterContentObserver(mGprsConnectObserver);
        }
        if(mIsGprsSwitching) {
            removeDialog(DIALOG_WAITING);
        }
    }
    @Override
    public void onDestroy() {
        mTimeHandler.removeMessages(EVENT_ATTACH_TIME_OUT);
        mTimeHandler.removeMessages(EVENT_DETACH_TIME_OUT);
        super.onDestroy();
    }
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

    }
    private void initPhoneState() {
        Xlog.d(TAG, "initPhoneState()");
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            Intent it = getActivity().getIntent();
            mSimSlot = it.getIntExtra(PhoneConstants.GEMINI_SIM_ID_KEY, SIM_CARD_UNDEFINED);

            mTelephonyManagerEx = TelephonyManagerEx.getDefault();
            mSimMap = new HashMap<Long,SIMInfo>();
            initSimMap();

            if (mSimSlot == -1) {
                mSimSlot = getSimSlot();
            }
            Xlog.d(TAG, "GEMINI_SIM_ID_KEY = " + mSimSlot + ";");
        } else {
            Xlog.d(TAG, "Not support GEMINI");
            mSimSlot = SIM_CARD_SINGLE;
        }
    }
    
    private void fillList(int simSlot) {
        mApnList.removeAll();
        if (simSlot < 0 || simSlot > 2) {
            return;
        }
        Xlog.d(TAG, "fillList(), simSlot=" + simSlot + ";");
        String where;

        where = "numeric=\"" + getQueryWhere(simSlot) + "\"";
        Cursor cursor = getActivity().managedQuery(
                mUri, // Telephony.Carriers.CONTENT_URI,
                PROJECTION_ARRAY, where,
                Telephony.Carriers.DEFAULT_SORT_ORDER);

        ArrayList<Preference> mmsApnList = new ArrayList<Preference>();

        boolean keySetChecked = false;
        mSelectedKey = getSelectedApnKey();
        Xlog.d(TAG, "mSelectedKey = " + mSelectedKey + ";"); 
        
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            String name = cursor.getString(NAME_INDEX);
            String apn = cursor.getString(APN_INDEX);
            String key = cursor.getString(ID_INDEX);
            String type = cursor.getString(TYPES_INDEX);
            int sourcetype = cursor.getInt(SOURCE_TYPE_INDEX);


            ApnPreference pref = new ApnPreference(getActivity());

            pref.setSimId(simSlot);   //set pre sim id info to the ApnEditor
            pref.setKey(key);
            pref.setTitle(name);
            pref.setSummary(apn);
            pref.setSourceType(sourcetype);
            pref.setPersistent(false);
            pref.setOnPreferenceChangeListener(this);

            boolean selectable = ((type == null) || (!type.equals("mms") && !type.equals("cmmail")));
            pref.setSelectable(selectable);
            if (selectable) {
                if ((mSelectedKey != null) && mSelectedKey.equals(key)) {
                    setSelectedApnKey(key);
                    pref.setChecked();
                    keySetChecked = true;
                    Xlog.d(TAG, "apn key: " + key + " set." + ";"); 
                }
                Xlog.d(TAG, "key:  " + key + " added!" + ";"); 
                mApnList.addPreference(pref);
                if (FeatureOption.MTK_GEMINI_SUPPORT) {
                    pref.setDependency(KEY_DATA_ENABLER_GEMINI);
                } else {
                    pref.setDependency(KEY_DATA_ENABLER);
                }
            } else {
                mmsApnList.add(pref);
            }
            cursor.moveToNext();
        }

        int mSelectableApnCount = mApnList.getPreferenceCount();
        //if no key selected, choose the 1st one.
        if (!keySetChecked && mSelectableApnCount > 0) {
            ApnPreference apnPref = (ApnPreference) mApnList.getPreference(0);
            if (apnPref != null) {
                setSelectedApnKey(apnPref.getKey());
                apnPref.setChecked();
                Xlog.d(TAG, "Key does not match.Set key: " + apnPref.getKey() + "."); 
            }

        }
        
        mIsCallStateIdle = mTelephonyManager.getCallState() == TelephonyManager.CALL_STATE_IDLE;
        
        switch(mSimSlot) {
            case SIM_CARD_1:
                boolean sim1Ready = TelephonyManager.SIM_STATE_READY == 
                    mTelephonyManagerEx.getSimState(PhoneConstants.GEMINI_SIM_1);
                mApnList.setEnabled(mScreenEnable && mIsCallStateIdle && 
                    !mAirplaneModeEnabled && sim1Ready);
                break;
            case SIM_CARD_2:
                boolean sim2Ready = TelephonyManager.SIM_STATE_READY == 
                    mTelephonyManagerEx.getSimState(PhoneConstants.GEMINI_SIM_2);
                mApnList.setEnabled(mScreenEnable && mIsCallStateIdle && 
                    !mAirplaneModeEnabled && sim2Ready);
                break;
            case SIM_CARD_SINGLE:
                boolean simReady = TelephonyManager.SIM_STATE_READY == 
                    TelephonyManager.getDefault().getSimState();
                mApnList.setEnabled(mScreenEnable && mIsCallStateIdle && 
                    !mAirplaneModeEnabled && simReady);
                break;
            default:
                break;
        }
        
    }
    
    private String getQueryWhere(int simSlot) {
        String where = "";
        
        switch (simSlot) {
            case SIM_CARD_1:
                mUri = Telephony.Carriers.SIM1Carriers.CONTENT_URI;
                where = SystemProperties.get(
                    TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC, "-1");
                mRestoreCarrierUri = PREFERAPN_URI_GEMINI_SIM1;
                break;

            case SIM_CARD_2:
                mUri = Telephony.Carriers.SIM2Carriers.CONTENT_URI;
                where = SystemProperties.get(
                    TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC_2, "-1");
                mRestoreCarrierUri = PREFERAPN_URI_GEMINI_SIM2;
                break;
            
            case SIM_CARD_SINGLE:
                mUri = Telephony.Carriers.CONTENT_URI;
                where = SystemProperties.get(
                    TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC, "");
                mRestoreCarrierUri = PREFERAPN_URI;
                break;
            
            default:
                Toast.makeText(
                        getActivity(), 
                        "Can't get any valid SIM information",
                        Toast.LENGTH_SHORT).show();
                finish();
                break;
        }

        Xlog.d(TAG, "where = " + where + ";");
        Xlog.d(TAG, "mUri = " + mUri + ";");
        return where;
    }

    /** {@inheritDoc} */
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Xlog.d(TAG, "onPreferenceChange(): Preference - " + preference
                + ", newValue - " + newValue + ", newValue type - "
                + newValue.getClass());
        String key = (preference == null ? "" : preference.getKey());
        if (KEY_DATA_ENABLER.equals(key)) {
            final boolean checked = ((Boolean)newValue).booleanValue();
            Xlog.d(TAG, "Data connection enabled?" + checked);
            dealWithConnChange(checked);
        }  else {
            if (newValue instanceof String) {
                setSelectedApnKey((String) newValue);
            }
        }

        return true;
    }
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen screen, Preference preference) {
        String key = preference.getKey();
        if (KEY_ADD_WIFI_NETWORK.equals(key)) {
            if (mWifiManager.isWifiEnabled()) {
                Xlog.d(TAG, "add network");
                super.addNetworkForSelector();
            }
        } else if (KEY_DATA_ENABLER_GEMINI.equals(key)) {
            //connect data connection
            SimItem simitem;
            final List<SimItem> simItemList = new ArrayList<SimItem>();
            for (Long simid: mSimMapKeyList) {
                SIMInfo siminfo = mSimMap.get(simid);

                if (siminfo != null) {
                    simitem = new SimItem(siminfo);
                    try {           
                        ITelephony iTelephony = ITelephony.Stub.asInterface(ServiceManager.getService(Context.TELEPHONY_SERVICE));
                        if (iTelephony != null) {
                            int state = iTelephony.getSimIndicatorStateGemini(siminfo.mSlot);                        
                            simitem.mState = state;
                            simItemList.add(simitem);
                        }
                    } catch (android.os.RemoteException e) {
                            Xlog.d(TAG, "[e = " + e + "]");
                    }
                }
            }
            simitem = new SimItem(this.getString(R.string.gemini_default_sim_never), -1, 
                    Settings.System.GPRS_CONNECTION_SIM_SETTING_NEVER);
            simItemList.add(simitem);    
            final int simListSize = simItemList.size();
            Xlog.d(TAG,"simListSize = " + simListSize);
            int offItem = simListSize - 1;
            int index = -1;
            long dataConnectId = (int)Settings.System.getLong(getContentResolver(),
                    Settings.System.GPRS_CONNECTION_SIM_SETTING,
                    Settings.System.DEFAULT_SIM_NOT_SET);
            Xlog.d(TAG, "getSimSlot,dataConnectId = " + dataConnectId);
            for (int i = 0; i < offItem; i++) {
                if (simItemList.get(i).mSimId == dataConnectId) {
                    index = i;
                }
            }
            mInitValue = index == -1 ? offItem : index;
            Xlog.d(TAG,"mInitValue = " + mInitValue);

            SelectionListAdapter mAdapter = new SelectionListAdapter(simItemList); 
            AlertDialog dialog = new AlertDialog.Builder(getActivity())
            //.setCancelable(false)
            .setSingleChoiceItems(mAdapter, mInitValue, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Xlog.d(TAG,"which = " + which);
                        SimItem simItem = simItemList.get(which);
                        mSimSlot = simItem.mSlot;
                        Xlog.d(TAG,"mSimSlot = " + mSimSlot);
                        Xlog.d(TAG,"mIsSim=" + simItem.mIsSim + ",mState=" + simItem.mState
                            + ",SIM_INDICATOR_LOCKED=" + PhoneConstants.SIM_INDICATOR_LOCKED);

                        if (simItem.mIsSim) {
                            if (mCellConnMgr != null && simItem.mState == PhoneConstants.SIM_INDICATOR_LOCKED) {
                                Xlog.d(TAG,"mCellConnMgr.handleCellConn");
                                mCellConnMgr.handleCellConn(simItem.mSlot, PIN1_REQUEST_CODE);
                            } else {
                                switchGprsDefautlSIM(simItem.mSimId);
                            }
                        } else {
                            switchGprsDefautlSIM(0);
                        }
                        dialog.dismiss();
                    }
                })
            .setTitle(R.string.gemini_data_connection)
            .setNegativeButton(com.android.internal.R.string.no, 
                        new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
            .create();
            dialog.show();
        } else {
            return super.onPreferenceTreeClick(screen, preference);
        }
        return true;
    }
    private int getSimSlot() {
        int slotId = -1;
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            long dataConnectId = (int)Settings.System.getLong(getContentResolver(),
                    Settings.System.GPRS_CONNECTION_SIM_SETTING,
                    Settings.System.DEFAULT_SIM_NOT_SET);
            Xlog.d(TAG, "getSimSlot,dataConnectId = " + dataConnectId);
            if (dataConnectId > 0) {
                slotId = mSimMap.get(dataConnectId).mSlot;
            }
        } else {
            slotId = SIM_CARD_SINGLE;
        }
        return slotId;
    }
    private void handleWifiStateChanged(int state) {
        Xlog.d(TAG, "handleWifiStateChanged(), new state=" + state + ";");
        Xlog.d(TAG, "[0- stoping 1-stoped 2-starting 3-started 4-unknown]");
        if (state == WifiManager.WIFI_STATE_ENABLED) {
            mAddWifiNetwork.setEnabled(true);
        } else {
            mAddWifiNetwork.setEnabled(false);
        }
    }

    private void setSelectedApnKey(String key) {
        mSelectedKey = key;
        ContentResolver resolver = getContentResolver();
        ContentValues values = new ContentValues();
        values.put(APN_ID, mSelectedKey);
        resolver.update(mRestoreCarrierUri, values, null, null);
    }

    private String getSelectedApnKey() {
        String key = null;
        Cursor cursor = getActivity().managedQuery(mRestoreCarrierUri, 
                                    new String[] {"_id"},
                                    null, 
                                    Telephony.Carriers.DEFAULT_SORT_ORDER);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            key = cursor.getString(ID_INDEX);
        }
//        cursor.close();
        return key;
    }
   
    private void updateDataEnabler() {
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            Xlog.d(TAG,"updateDataEnabler, mSimSlot=" + mSimSlot);
            fillList(mSimSlot);
            mDataEnablerGemini.setEnabled(mIsSIMExist && !mAirplaneModeEnabled); 
        } else {
            ConnectivityManager cm = 
                (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) {
                Xlog.d(TAG, "Fail to get ConnectivityManager instance");
                return;
            }
            boolean enabled = cm.getMobileDataEnabled();
            Xlog.d(TAG, "updateDataEnabler(), current state=" + enabled);
            mDataEnabler.setChecked(enabled);
            Xlog.d(TAG,"single card mDataEnabler, true");
            mDataEnabler.setEnabled(mIsSIMExist && !mAirplaneModeEnabled);
        }
    }
    
    /**
     * To enable/disable data connection
     * @param enabled
     */
    private void dealWithConnChange(boolean enabled) {
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            Xlog.d(TAG, "only sigle SIM load can controling data connection");
            return;
        }
        Xlog.d(TAG, "dealWithConnChange(),new request state is enabled?" + enabled + ";");
        ConnectivityManager cm = 
            (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            Xlog.d(TAG, "Fail to get ConnectivityManager instance");
            return;
        }
        cm.setMobileDataEnabled(enabled);
        showDialog(DIALOG_WAITING);
        mIsGprsSwitching = true;
        if (enabled) {
            mTimeHandler.sendEmptyMessageDelayed(EVENT_ATTACH_TIME_OUT, ATTACH_TIME_OUT_LENGTH);
        } else {
            mTimeHandler.sendEmptyMessageDelayed(EVENT_DETACH_TIME_OUT, DETACH_TIME_OUT_LENGTH);
        }
    }
 
    @Override
    public Dialog onCreateDialog(int id) {
        ProgressDialog dialog = new ProgressDialog(getActivity());
        if (id == DIALOG_WAITING) {
            dialog.setMessage(getResources().getString(R.string.data_enabler_waiting_message));
            dialog.setIndeterminate(true);
            dialog.setCancelable(false);
            return dialog;
        } else {
            return super.onCreateDialog(id);
        }
    }
  
    private void initSimMap() {
        
        List<SIMInfo> simList = SIMInfo.getInsertedSIMList(getActivity());
        Collections.sort(simList, new GeminiUtils.SIMInfoComparable());
        mSimMap.clear();
        Xlog.i(TAG, "sim number is " + simList.size());
        for (SIMInfo siminfo:simList) {
            mSimMap.put(Long.valueOf(siminfo.mSimId), siminfo);
        }
        mSimMapKeyList = (List<Long>)(new ArrayList(mSimMap.keySet()));
    }


    /**
     * switch data connection default SIM
     * @param value: sim id of the new default SIM
     */
    private void switchGprsDefautlSIM(long value) {

        if (value < 0) {
            return;
        }
        
        long gprsValue = Settings.System.getLong(getContentResolver(),
                Settings.System.GPRS_CONNECTION_SIM_SETTING,Settings.System.DEFAULT_SIM_NOT_SET);
        Xlog.d(TAG,"value=" + value + ", gprsValue=" + gprsValue + 
            ", valueOfNotSet" + Settings.System.DEFAULT_SIM_NOT_SET);    
        if (value == gprsValue) {
            return;
        }        
        Intent intent = new Intent(Intent.ACTION_DATA_DEFAULT_SIM_CHANGED);
        intent.putExtra("simid", value);
        getActivity().sendBroadcast(intent);
        Xlog.d(TAG,"send gprs switch broadcast");
        showDialog(DIALOG_WAITING);
        mIsGprsSwitching = true;
        if (value > 0) {
            mTimeHandler.sendEmptyMessageDelayed(EVENT_ATTACH_TIME_OUT, ATTACH_TIME_OUT_LENGTH);
            Xlog.d(TAG,"set ATTACH_TIME_OUT");
        } else {
            mTimeHandler.sendEmptyMessageDelayed(EVENT_DETACH_TIME_OUT, DETACH_TIME_OUT_LENGTH);
            Xlog.d(TAG,"set DETACH_TIME_OUT");
        }
    }

    public int getSimColorResource(int color) {
        if ((color >= COLOR_INDEX_ZERO) && (color <= COLOR_INDEX_SEVEN)) {
            return Telephony.SIMBackgroundDarkRes[color];
        } else {
            return -1;
        }
    }

    public int getStatusResource(int state) {
        switch (state) {
        case PhoneConstants.SIM_INDICATOR_RADIOOFF:
            return com.mediatek.internal.R.drawable.sim_radio_off;
        case PhoneConstants.SIM_INDICATOR_LOCKED:
            return com.mediatek.internal.R.drawable.sim_locked;
        case PhoneConstants.SIM_INDICATOR_INVALID:
            return com.mediatek.internal.R.drawable.sim_invalid;
        case PhoneConstants.SIM_INDICATOR_SEARCHING:
            return com.mediatek.internal.R.drawable.sim_searching;
        case PhoneConstants.SIM_INDICATOR_ROAMING:
            return com.mediatek.internal.R.drawable.sim_roaming;
        case PhoneConstants.SIM_INDICATOR_CONNECTED:
            return com.mediatek.internal.R.drawable.sim_connected;
        case PhoneConstants.SIM_INDICATOR_ROAMINGCONNECTED:
            return com.mediatek.internal.R.drawable.sim_roaming_connected;
        default:
            return -1;
        }
    }
    class SelectionListAdapter extends BaseAdapter {
        
        List<SimItem> mSimItemList;
        

        
        public SelectionListAdapter(List<SimItem> simItemList) {
            mSimItemList = simItemList;
        }
        
        public int getCount() {
            return mSimItemList.size();
        }

        public Object getItem(int position) {
            return mSimItemList.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                LayoutInflater mFlater = LayoutInflater.from(getActivity());    
                convertView = mFlater.inflate(R.layout.preference_sim_default_select, null);
                holder = new ViewHolder();
                setViewHolderId(holder,convertView);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder)convertView.getTag();
            }
            SimItem simItem = (SimItem)getItem(position);
            setNameAndNum(holder.mTextName,holder.mTextNum,simItem);
            setImageSim(holder.mImageSim,simItem);
            setImageStatus(holder.mImageStatus,simItem);
            setTextNumFormat(holder.mTextNumFormat,simItem);
            holder.mCkRadioOn.setChecked(mInitValue == position);
            if (simItem.mState == PhoneConstants.SIM_INDICATOR_RADIOOFF) {
                convertView.setEnabled(false);
                holder.mTextName.setEnabled(false);
                holder.mTextNum.setEnabled(false);
                holder.mCkRadioOn.setEnabled(false);
            } else {
                convertView.setEnabled(true);
                holder.mTextName.setEnabled(true);
                holder.mTextNum.setEnabled(true);
                holder.mCkRadioOn.setEnabled(true);
            }
            return convertView;
          }
        private void setTextNumFormat(TextView textNumFormat, SimItem simItem) {
            if (simItem.mIsSim) {
                if (simItem.mNumber != null) {
                    switch (simItem.mDispalyNumberFormat) {
                        case DISPLAY_NONE: 
                            textNumFormat.setVisibility(View.GONE);
                            break;
                        case DISPLAY_FIRST_FOUR:
                            textNumFormat.setVisibility(View.VISIBLE);
                            if (simItem.mNumber.length() >= SIM_NUMBER_LEN) {
                                textNumFormat.setText(simItem.mNumber.substring(0, SIM_NUMBER_LEN));
                            } else {
                                textNumFormat.setText(simItem.mNumber);
                            }
                            break;
                        case DISPLAY_LAST_FOUR:
                            textNumFormat.setVisibility(View.VISIBLE);
                            if (simItem.mNumber.length() >= SIM_NUMBER_LEN) {
                                textNumFormat.setText(simItem.mNumber.substring(
                                    simItem.mNumber.length() - SIM_NUMBER_LEN));
                            } else {
                                textNumFormat.setText(simItem.mNumber);
                            }
                            break;
                        default:
                            break;
                    }           
                }
            }
            
        }
        private void setImageStatus(ImageView imageStatus, SimItem simItem) {
            if (simItem.mIsSim) {
                int res = getStatusResource(simItem.mState);
                if (res == -1) {
                    imageStatus.setVisibility(View.GONE);
                } else {
                    imageStatus.setVisibility(View.VISIBLE);
                    imageStatus.setImageResource(res);
                }
            }
                
        }
        private void setImageSim(RelativeLayout imageSim, SimItem simItem) {
            if (simItem.mIsSim) {
                int resColor = getSimColorResource(simItem.mColor);
                if (resColor >= 0) {
                    imageSim.setVisibility(View.VISIBLE);
                    imageSim.setBackgroundResource(resColor);
                }
            } else if (simItem.mColor == COLOR_INDEX_EIGHT) {
                imageSim.setVisibility(View.VISIBLE);
                imageSim.setBackgroundResource(com.mediatek.internal.R.drawable.sim_background_sip);
            } else {
                imageSim.setVisibility(View.GONE);
            }
        }

        private void setViewHolderId(ViewHolder holder, View convertView) {
            holder.mTextName = (TextView)convertView.findViewById(R.id.simNameSel);
            holder.mTextNum = (TextView)convertView.findViewById(R.id.simNumSel);
            holder.mImageStatus = (ImageView)convertView.findViewById(R.id.simStatusSel);
            holder.mTextNumFormat = (TextView)convertView.findViewById(R.id.simNumFormatSel);
            holder.mCkRadioOn = (RadioButton)convertView.findViewById(R.id.Enable_select);
            holder.mImageSim = (RelativeLayout)convertView.findViewById(R.id.simIconSel);
        }

        private void setNameAndNum(TextView textName,TextView textNum, SimItem simItem) {
            if (simItem.mName == null) {
                textName.setVisibility(View.GONE);
            } else {
                textName.setVisibility(View.VISIBLE);
                textName.setText(simItem.mName);
            }
            if ((simItem.mIsSim) && ((simItem.mNumber != null) &&
                    (simItem.mNumber.length() != 0))) {
                textNum.setVisibility(View.VISIBLE);
                textNum.setText(simItem.mNumber);
            } else {
                textNum.setVisibility(View.GONE);
            }
        }
        class ViewHolder {
            TextView mTextName;
            TextView mTextNum;
            RelativeLayout mImageSim;
            ImageView mImageStatus;
            TextView mTextNumFormat;
            RadioButton mCkRadioOn;
            
        }
    }
    static class SimItem {
        public boolean mIsSim = true;
        public String mName = null;
        public String mNumber = null;
        public int mDispalyNumberFormat = 0;
        public int mColor = -1;
        public int mSlot = -1;
        public int mState = PhoneConstants.SIM_INDICATOR_NORMAL;
        public long mSimId = -1;
        
        //Constructor for not real sim
        public SimItem(String name, int color,long simID) {
            mName = name;
            mColor = color;
            mIsSim = false;
        }
        //constructor for sim
        public SimItem(SIMInfo siminfo) {
            mIsSim = true;
            mName = siminfo.mDisplayName;
            mNumber = siminfo.mNumber;
            mDispalyNumberFormat = siminfo.mDispalyNumberFormat;
            mColor = siminfo.mColor;
            mSlot = siminfo.mSlot;
            mSimId = siminfo.mSimId;
        }
    }
}
