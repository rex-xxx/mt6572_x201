/*
 * Copyright (C) 2006 The Android Open Source Project
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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.provider.Settings;
import android.provider.Telephony;
import android.provider.Telephony.SIMInfo;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;

import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.TelephonyProperties;

import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.settings.ext.IApnEditorExt;
import com.mediatek.xlog.Xlog;

import java.util.List;

public class ApnEditor extends PreferenceActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener,
                    Preference.OnPreferenceChangeListener {

    private static final String TAG = ApnEditor.class.getSimpleName();
    private static final String SS_TAG = "ssr";
    
    private static final String SAVED_POS = "pos";
    private static final String KEY_AUTH_TYPE = "auth_type";
    private static final String KEY_PROTOCOL = "apn_protocol";
    private static final String KEY_ROAMING_PROTOCOL = "apn_roaming_protocol";
    private static final String KEY_CARRIER_ENABLED = "carrier_enabled";
    private static final String KEY_BEARER = "bearer";
    private static final String KEY_APN_TYPE_LIST = "apn_type_list";
    

    private static final int MENU_DELETE = Menu.FIRST;
    private static final int MENU_SAVE = Menu.FIRST + 1;
    private static final int MENU_CANCEL = Menu.FIRST + 2;
    private static final int ERROR_DIALOG_ID = 0;
    private static final int DIALOG_CONFIRM_CHANGE = 1;

    private static String sNotSet;
    private EditTextPreference mName;
    private EditTextPreference mApn;
    private ApnTypePreference mApnTypeList;    
    private EditTextPreference mProxy;
    private EditTextPreference mPort;
    private EditTextPreference mUser;
    private EditTextPreference mServer;
    private EditTextPreference mPassword;
    private EditTextPreference mMmsc;
    private EditTextPreference mMcc;
    private EditTextPreference mMnc;
    private EditTextPreference mMmsProxy;
    private EditTextPreference mMmsPort;
    private ListPreference mAuthType;
    //Remove this edit text preference , instead , use a list mPanTypeList
    //private EditTextPreference mApnType;
    private ListPreference mProtocol;
    private ListPreference mRoamingProtocol;
    private CheckBoxPreference mCarrierEnabled;
    private ListPreference mBearer;

    private String mCurMnc;
    private String mCurMcc;

    private Uri mUri;
    private Cursor mCursor;
    private boolean mNewApn;
    private boolean mFirstTime;
    private Resources mRes;

    private static final int SOURCE_TYPE_DEFAULT = 0;
    private static final int SOURCE_TYPE_USER_EDIT = 1;
    private static final int SOURCE_TYPE_OMACP = 2;

    private int mSourceType = -1;
    private int mSimId;
    private Uri mProviderUri;
    private boolean mIsCallStateIdle = true;
    private boolean mAirplaneModeEnabled = false; 
    private int mDualSimMode = -1;
    private static final String TRANSACTION_START = "com.android.mms.transaction.START";
    private static final String TRANSACTION_STOP = "com.android.mms.transaction.STOP";
    private boolean mNotInMmsTransaction = true;
    private boolean mReadOnlyMode = false;
    private IntentFilter mIntentFilter;
    
    IApnEditorExt mExt;

    /**
     * Standard projection for the interesting columns of a normal note.
     */
    private static final String[] PROJECTION = new String[] {
            Telephony.Carriers._ID,     // 0
            Telephony.Carriers.NAME,    // 1
            Telephony.Carriers.APN,     // 2
            Telephony.Carriers.PROXY,   // 3
            Telephony.Carriers.PORT,    // 4
            Telephony.Carriers.USER,    // 5
            Telephony.Carriers.SERVER,  // 6
            Telephony.Carriers.PASSWORD, // 7
            Telephony.Carriers.MMSC, // 8
            Telephony.Carriers.MCC, // 9
            Telephony.Carriers.MNC, // 10
            Telephony.Carriers.NUMERIC, // 11
            Telephony.Carriers.MMSPROXY,// 12
            Telephony.Carriers.MMSPORT, // 13
            Telephony.Carriers.AUTH_TYPE, // 14
            Telephony.Carriers.TYPE, // 15
            Telephony.Carriers.PROTOCOL, // 16
            Telephony.Carriers.CARRIER_ENABLED, // 17
            Telephony.Carriers.BEARER, // 18
            Telephony.Carriers.ROAMING_PROTOCOL, // 19
            Telephony.Carriers.SOURCE_TYPE,//20
            
    };

    private static final int ID_INDEX = 0;
    private static final int NAME_INDEX = 1;
    private static final int APN_INDEX = 2;
    private static final int PROXY_INDEX = 3;
    private static final int PORT_INDEX = 4;
    private static final int USER_INDEX = 5;
    private static final int SERVER_INDEX = 6;
    private static final int PASSWORD_INDEX = 7;
    private static final int MMSC_INDEX = 8;
    private static final int MCC_INDEX = 9;
    private static final int MNC_INDEX = 10;
    private static final int MMSPROXY_INDEX = 12;
    private static final int MMSPORT_INDEX = 13;
    private static final int AUTH_TYPE_INDEX = 14;
    private static final int TYPE_INDEX = 15;
    private static final int PROTOCOL_INDEX = 16;
    private static final int CARRIER_ENABLED_INDEX = 17;
    private static final int BEARER_INDEX = 18;
    private static final int ROAMING_PROTOCOL_INDEX = 19;
    private static final int SOURCE_TYPE_INDEX = 20;

    private static final int SIM_CARD_SINGLE = 0;

    private String mSpn = "";
    private String mIMSI = "";
    private String mPnn = "";

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction(); 
            if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
                mAirplaneModeEnabled = intent.getBooleanExtra("state", false);
                if (mAirplaneModeEnabled) {
                    Xlog.d(SS_TAG, "receiver: ACTION_AIRPLANE_MODE_CHANGED in ApnEditor");
                    radioOffExit();
                }
            } else if (action.equals(TRANSACTION_START)) {
                Xlog.d(SS_TAG, "receiver: TRANSACTION_START in ApnEditor");
                mNotInMmsTransaction = false;
                getPreferenceScreen().setEnabled(false);
            } else if (action.equals(TRANSACTION_STOP)) {
                Xlog.d(SS_TAG, "receiver: TRANSACTION_STOP in ApnEditor");
                mNotInMmsTransaction = true;
                setScreenEnabledStatus();
  
                if (ApnSettings.TETHER_TYPE.equals(mApnTypeList.getTypeString())) {
                    mExt.setPreferenceState(mApnTypeList);
                }
            } else if (action.equals(Intent.ACTION_DUAL_SIM_MODE_CHANGED)) {
                mDualSimMode  = intent.getIntExtra(Intent.EXTRA_DUAL_SIM_MODE,-1);                
                Xlog.d(TAG,"receiver, new dual sim mode" + mDualSimMode);
                if (mDualSimMode == 0) {
                    radioOffExit();
                }
            } else if (TelephonyIntents.ACTION_SIM_STATE_CHANGED.equals(action)) {
                if (FeatureOption.MTK_MVNO_SUPPORT) {
                    if (mSpn == null || mSpn.isEmpty()) {
                        setSpn();
                    }
                    if (mIMSI == null || mIMSI.isEmpty()) {
                        setIMSI();
                    }
                    if (mPnn == null || mPnn.isEmpty()) {
                        setPnn();
                    }
                }
            } else if (action.equals(TelephonyIntents.ACTION_SIM_INFO_UPDATE)) {
                ///M: add for sim hot swap {
                List<SIMInfo> temp = SIMInfo.getInsertedSIMList(ApnEditor.this);
                if (temp.size() == 0) {
                    Xlog.d(TAG, "Activity finished");
                    finish();
                } else if (temp.size() == 1 && FeatureOption.MTK_GEMINI_SUPPORT) {
                    if (temp.get(0).mSlot != mSimId) {
                        Xlog.d(TAG, "temp.size()=" + temp.size() + "Activity finished");
                        finish();
                    }
                }
                ///@}
            }
        }
    };

    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {

        @Override
        public void onServiceStateChanged(ServiceState serviceState) {
            super.onServiceStateChanged(serviceState);

            TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            mIsCallStateIdle = telephonyManager.getCallState() == TelephonyManager.CALL_STATE_IDLE;
            Xlog.d(TAG, "[onServiceStateChanged][mIsCallStateIdle="+mIsCallStateIdle+"]");
            invalidateOptionsMenu();
        }
    };

    private PhoneStateListener mPhoneStateListener1 = new PhoneStateListener() {

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);

            TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            mIsCallStateIdle = telephonyManager.getCallState() == TelephonyManager.CALL_STATE_IDLE;
            Xlog.d(TAG, "[onCallStateChanged][mIsCallStateIdle="+mIsCallStateIdle+"]");
            invalidateOptionsMenu();
        }
    };
    
    private ContentObserver mContentObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            Xlog.d(TAG,"background changed apn ");
            mFirstTime = true;
            stopManagingCursor(mCursor);
            mCursor = managedQuery(mUri, PROJECTION, null, null);
            mCursor.moveToFirst();
            fillUi();
        }
     };

    private void radioOffExit() {
        if (mNewApn && (mUri != null)) {
            getContentResolver().delete(mUri, null, null);
        }
        finish();
    }

    private void setSpn() {
        try {
            ITelephony telephony = ITelephony.Stub.asInterface(
                    ServiceManager.getService(Context.TELEPHONY_SERVICE)); //getITelephony();
            if (FeatureOption.MTK_GEMINI_SUPPORT) {
                mSpn = telephony.getSpNameInEfSpnGemini(mSimId);
            } else {
                mSpn = telephony.getSpNameInEfSpn();
            }
        } catch (android.os.RemoteException e) {
            Xlog.d(TAG, "RemoteException");
        }
        Xlog.d(TAG, "spn = " + mSpn);
    }

    private void setIMSI() {
        try {
            ITelephony telephony = ITelephony.Stub.asInterface(ServiceManager.getService(Context.TELEPHONY_SERVICE));
            if (FeatureOption.MTK_GEMINI_SUPPORT) {
                mIMSI = telephony.isOperatorMvnoForImsiGemini(mSimId);
            } else {
                mIMSI = telephony.isOperatorMvnoForImsi();
            }
        } catch (android.os.RemoteException e) {
            Xlog.d(TAG, "RemoteException");
        }
        Xlog.d(TAG, "IMSI = " + mIMSI);
    }

    private void setPnn() {
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            mPnn = telephonyManager.isOperatorMvnoForEfPnnGemini(mSimId);
        } else {
            mPnn = telephonyManager.isOperatorMvnoForEfPnn();
        }
        Xlog.d(TAG, "mPnn = " + mPnn);
    }

    private void setScreenEnabledStatus() {
        if (mReadOnlyMode) {
            Log.d(TAG, "-----------mReadOnlyMode is true -----------");
        }
        if (mAirplaneModeEnabled) {
            Log.d(TAG, "-----------mAirplaneModeEnabled is true -----------");
        }
        if (!mNotInMmsTransaction) {
                    Log.d(TAG, "-----------!mNotInMmsTransaction is true -----------");
        }
        if (mDualSimMode == 0) {
                    Log.d(TAG, "-----------mDualSimMode is 0 -----------");
        }
        getPreferenceScreen().setEnabled(!mReadOnlyMode && !mAirplaneModeEnabled
              && mNotInMmsTransaction && mDualSimMode != 0);
    }
    
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.apn_editor);

        sNotSet = getResources().getString(R.string.apn_not_set);
        mName = (EditTextPreference) findPreference("apn_name");
        mApn = (EditTextPreference) findPreference("apn_apn");
        mProxy = (EditTextPreference) findPreference("apn_http_proxy");
        mPort = (EditTextPreference) findPreference("apn_http_port");
        mUser = (EditTextPreference) findPreference("apn_user");
        mServer = (EditTextPreference) findPreference("apn_server");
        mPassword = (EditTextPreference) findPreference("apn_password");
        mMmsProxy = (EditTextPreference) findPreference("apn_mms_proxy");
        mMmsPort = (EditTextPreference) findPreference("apn_mms_port");
        mMmsc = (EditTextPreference) findPreference("apn_mmsc");
        mMcc = (EditTextPreference) findPreference("apn_mcc");
        mMnc = (EditTextPreference) findPreference("apn_mnc");
        mApnTypeList = (ApnTypePreference) findPreference("apn_type_list");
        mApnTypeList.setOnPreferenceChangeListener(this);
        

        mAuthType = (ListPreference) findPreference(KEY_AUTH_TYPE);
        mAuthType.setOnPreferenceChangeListener(this);

        mProtocol = (ListPreference) findPreference(KEY_PROTOCOL);
        mProtocol.setOnPreferenceChangeListener(this);

        mRoamingProtocol = (ListPreference) findPreference(KEY_ROAMING_PROTOCOL);
        mRoamingProtocol.setOnPreferenceChangeListener(this);

        mCarrierEnabled = (CheckBoxPreference) findPreference(KEY_CARRIER_ENABLED);

        mBearer = (ListPreference) findPreference(KEY_BEARER);
        mBearer.setOnPreferenceChangeListener(this);

        mRes = getResources();
        

        final Intent intent = getIntent();
        final String action = intent.getAction();

        mFirstTime = icicle == null;
        mSimId = intent.getIntExtra(PhoneConstants.GEMINI_SIM_ID_KEY, SIM_CARD_SINGLE);
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            mProviderUri = getGeminiUri(mSimId);    
        } else {
            mProviderUri = Telephony.Carriers.CONTENT_URI;
        }

        mExt = Utils.getApnEditorPlugin(this);
        if (action.equals(Intent.ACTION_EDIT)) {
            mUri = intent.getData();
            mReadOnlyMode = intent.getBooleanExtra("readOnly",false);
            Xlog.d(TAG,"Read only mode : " + mReadOnlyMode);
           
        } else if (action.equals(Intent.ACTION_INSERT)) {
            if (mFirstTime || icicle.getInt(SAVED_POS) == 0) {
                mUri = getContentResolver().insert(intent.getData(), new ContentValues());
            } else {

                mUri = mExt.getUriFromIntent(this, intent);
          
                mUri = ContentUris.withAppendedId(mProviderUri,
                        icicle.getInt(SAVED_POS));
            }
            mNewApn = true;
            // If we were unable to create a new note, then just finish
            // this activity.  A RESULT_CANCELED will be sent back to the
            // original activity if they requested a result.
            if (mUri == null) {
                Log.w(TAG, "Failed to insert new telephony provider into "
                        + getIntent().getData());
                finish();
                return;
            }

            // The new entry was created, so assume all will end well and
            // set the result to be returned.
            setResult(RESULT_OK, (new Intent()).setAction(mUri.toString()));

        } else {
            finish();
            return;
        }

        mIntentFilter = new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        mIntentFilter.addAction(TelephonyIntents.ACTION_SIM_STATE_CHANGED); 
        //M: Add for hot swap {
        mIntentFilter.addAction(TelephonyIntents.ACTION_SIM_INFO_UPDATE);
        ///@}
            
        if (!mReadOnlyMode) {
            mIntentFilter.addAction(TRANSACTION_START);
            mIntentFilter.addAction(TRANSACTION_STOP);
        }
        mCursor = managedQuery(mUri, PROJECTION, null, null);
        mCursor.moveToFirst();

        fillUi();
        if (!mNewApn) {
            getContentResolver().registerContentObserver(mUri, true, mContentObserver);
        }

        if (FeatureOption.MTK_MVNO_SUPPORT) {
            setSpn();
            setIMSI();
            setPnn();
        }
    }
    private String getIccOperator(int simId) {
        String property;
        switch (simId) {
            case PhoneConstants.GEMINI_SIM_1:
            property = TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC;
            break;
            case PhoneConstants.GEMINI_SIM_2:
            property = TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC_2;
            break;
            case PhoneConstants.GEMINI_SIM_3:
            property = TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC_3;
            break;
            case PhoneConstants.GEMINI_SIM_4:
            property = TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC_4;
            break; 
            default:
            Log.d(TAG,"Error need to check simId=" + simId);
            property = TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC;
            break;
        }  
        return property;      
    }
    private Uri getGeminiUri(int simId) {
        Log.d(TAG,"simId=" + simId);
        Uri tempUri;
        switch (simId) {
            case PhoneConstants.GEMINI_SIM_1:
            tempUri = Telephony.Carriers.SIM1Carriers.CONTENT_URI;
            break;
            case PhoneConstants.GEMINI_SIM_2:
            tempUri = Telephony.Carriers.SIM2Carriers.CONTENT_URI;
            break;
            case PhoneConstants.GEMINI_SIM_3:
            tempUri = Telephony.Carriers.SIM3Carriers.CONTENT_URI;
            break;
            case PhoneConstants.GEMINI_SIM_4:
            tempUri = Telephony.Carriers.SIM4Carriers.CONTENT_URI;
            break; 
            default:
            Log.d(TAG,"Error need to check simId=" + simId);
            tempUri = Telephony.Carriers.SIM1Carriers.CONTENT_URI;
            break;
        }
        return tempUri;
    }

    @Override
    public void onDestroy() {
        if (!mNewApn) {
            getContentResolver().unregisterContentObserver(mContentObserver);  
        }
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
        
        registerReceiver(mReceiver, mIntentFilter);
        
        //if the phone not idle state, then disable the preferenceScreen
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        mIsCallStateIdle = telephonyManager.getCallState() == TelephonyManager.CALL_STATE_IDLE;
        telephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_SERVICE_STATE);
        telephonyManager.listen(mPhoneStateListener1, PhoneStateListener.LISTEN_CALL_STATE);
        
        mAirplaneModeEnabled = AirplaneModeEnabler.isAirplaneModeOn(ApnEditor.this);
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            mDualSimMode = Settings.System.getInt(getContentResolver(),Settings.System.DUAL_SIM_MODE_SETTING,-1);
        }
        mNotInMmsTransaction = isMMSNotTransaction();        
        setScreenEnabledStatus();
  
        if (ApnSettings.TETHER_TYPE.equals(mApnTypeList.getTypeString())) {
            mExt.setPreferenceState(mApnTypeList);
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
    @Override
    public void onPause() {
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
        unregisterReceiver(mReceiver);

        TelephonyManager tm = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);
        tm.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
        tm.listen(mPhoneStateListener1, PhoneStateListener.LISTEN_NONE);
        Xlog.d(TAG,"onDestroy set PhoneStateListener.LISTEN_NONE!");

        super.onPause();
    }

    private void fillUi() {
        if (mCursor.getCount() == 0) {
            finish();
            return;
        }        
        if (mFirstTime) {
            mFirstTime = false;
            // Fill in all the values from the db in both text editor and summary
            mName.setText(mCursor.getString(NAME_INDEX));
            mApn.setText(mCursor.getString(APN_INDEX));
            mProxy.setText(mCursor.getString(PROXY_INDEX));
            mPort.setText(mCursor.getString(PORT_INDEX));
            mUser.setText(mCursor.getString(USER_INDEX));
            mServer.setText(mCursor.getString(SERVER_INDEX));
            mPassword.setText(mCursor.getString(PASSWORD_INDEX));
            mMmsProxy.setText(mCursor.getString(MMSPROXY_INDEX));
            mMmsPort.setText(mCursor.getString(MMSPORT_INDEX));
            mMmsc.setText(mCursor.getString(MMSC_INDEX));
            mMcc.setText(mCursor.getString(MCC_INDEX));
            mMnc.setText(mCursor.getString(MNC_INDEX));
            
            String strType = mCursor.getString(TYPE_INDEX);
            mApnTypeList.setType(mCursor.getString(MCC_INDEX), mCursor.getString(MNC_INDEX), getIntent());
            mApnTypeList.setSummary(checkNull(strType));
            mApnTypeList.intCheckState(strType);

            if (mNewApn) {
                String simSysProperty;
                if (FeatureOption.MTK_GEMINI_SUPPORT) {
                    simSysProperty = getIccOperator(mSimId);    
                } else {
                    simSysProperty = TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC;
                }
                String numeric = SystemProperties.get(simSysProperty);
                // MCC is first 3 chars and then in 2 - 3 chars of MNC
                if (numeric != null && numeric.length() > 4) {
                    // Country code
                    String mcc = numeric.substring(0, 3);
                    // Network code
                    String mnc = numeric.substring(3);
                    // Auto populate MNC and MCC for new entries, based on what SIM reports
                    mMcc.setText(mcc);
                    mMnc.setText(mnc);
                    mCurMnc = mnc;
                    mCurMcc = mcc;
                }

                String apnType = getIntent().getStringExtra(ApnSettings.APN_TYPE);

                if (ApnSettings.TETHER_TYPE.equals(apnType)) {
                    mApnTypeList.setSummary("tethering");
                    mApnTypeList.intCheckState("tethering");                    
                } else {
                    mApnTypeList.setSummary("default");
                    mApnTypeList.intCheckState("default");                    
                }
            }
            int authVal = mCursor.getInt(AUTH_TYPE_INDEX);
            if (authVal != -1) {
                mAuthType.setValueIndex(authVal);
            } else {
                mAuthType.setValue(null);
            }

            mProtocol.setValue(mCursor.getString(PROTOCOL_INDEX));
            mRoamingProtocol.setValue(mCursor.getString(ROAMING_PROTOCOL_INDEX));
            mCarrierEnabled.setChecked(mCursor.getInt(CARRIER_ENABLED_INDEX) == 1);
            mBearer.setValue(mCursor.getString(BEARER_INDEX));
            mSourceType = mCursor.getInt(SOURCE_TYPE_INDEX);

        }

        mName.setSummary(checkNull(mName.getText()));
        mApn.setSummary(checkNull(mApn.getText()));
        mProxy.setSummary(checkNull(mProxy.getText()));
        mPort.setSummary(checkNull(mPort.getText()));
        mUser.setSummary(checkNull(mUser.getText()));
        mServer.setSummary(checkNull(mServer.getText()));
        mPassword.setSummary(starify(mPassword.getText()));
        mMmsProxy.setSummary(checkNull(mMmsProxy.getText()));
        mMmsPort.setSummary(checkNull(mMmsPort.getText()));
        mMmsc.setSummary(checkNull(mMmsc.getText()));
        mMcc.setSummary(checkNull(mMcc.getText()));
        mMnc.setSummary(checkNull(mMnc.getText()));



        String authVal = mAuthType.getValue();
        if (authVal != null) {
            int authValIndex = Integer.parseInt(authVal);
            mAuthType.setValueIndex(authValIndex);

            String []values = mRes.getStringArray(R.array.apn_auth_entries);
            mAuthType.setSummary(values[authValIndex]);
        } else {
            mAuthType.setSummary(sNotSet);
        }

        mProtocol.setSummary(
                checkNull(protocolDescription(mProtocol.getValue(), mProtocol)));
        mRoamingProtocol.setSummary(
                checkNull(protocolDescription(mRoamingProtocol.getValue(), mRoamingProtocol)));
        mBearer.setSummary(
                checkNull(bearerDescription(mBearer.getValue())));
    }

    /**
     * Returns the UI choice (e.g., "IPv4/IPv6") corresponding to the given
     * raw value of the protocol preference (e.g., "IPV4V6"). If unknown,
     * return null.
     */
    private String protocolDescription(String raw, ListPreference protocol) {
        int protocolIndex = protocol.findIndexOfValue(raw);
        if (protocolIndex == -1) {
            return null;
        } else {
            String[] values = mRes.getStringArray(R.array.apn_protocol_entries);
            try {
                return values[protocolIndex];
            } catch (ArrayIndexOutOfBoundsException e) {
                return null;
            }
        }
    }

    private String bearerDescription(String raw) {
        int mBearerIndex = mBearer.findIndexOfValue(raw);
        if (mBearerIndex == -1) {
            return null;
        } else {
            String[] values = mRes.getStringArray(R.array.bearer_entries);
            try {
                return values[mBearerIndex];
            } catch (ArrayIndexOutOfBoundsException e) {
                return null;
            }
        }
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        if (KEY_AUTH_TYPE.equals(key)) {
            try {
                int index = Integer.parseInt((String) newValue);
                mAuthType.setValueIndex(index);

                String []values = mRes.getStringArray(R.array.apn_auth_entries);
                mAuthType.setSummary(values[index]);
            } catch (NumberFormatException e) {
                return false;
            }            
        } else if (KEY_PROTOCOL.equals(key)) {
            String protocol = protocolDescription((String) newValue, mProtocol);
            if (protocol == null) {
                return false;
            }
            mProtocol.setValue((String) newValue);
            mProtocol.setSummary(protocol);
        } else if (KEY_ROAMING_PROTOCOL.equals(key)) {
            String protocol = protocolDescription((String) newValue, mRoamingProtocol);
            if (protocol == null) {
                return false;
            }

            // M: firstly call setValue, then call setSummary CR ALPS00572183 {@
            mRoamingProtocol.setValue((String) newValue);
            mRoamingProtocol.setSummary(protocol);
            // @}

        } else if (KEY_BEARER.equals(key)) {
            String bearer = bearerDescription((String) newValue);
            if (bearer == null) {
                return false;
            }
            mBearer.setValue((String) newValue);
            mBearer.setSummary(bearer);
        } else if (KEY_APN_TYPE_LIST.equals(key)) {
            mApnTypeList.setSummary(checkNull(mApnTypeList.getTypeString()));
        }


        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        // If it's a new APN, then cancel will delete the new entry in onPause
        if (mReadOnlyMode) {
            return true;
        }

        if (!mNewApn && mSourceType != 0) {
            menu.add(0, MENU_DELETE, 0, R.string.menu_delete)
                .setIcon(R.drawable.ic_menu_delete_holo_dark);
        }
        menu.add(0, MENU_SAVE, 0, R.string.menu_save)
            .setIcon(android.R.drawable.ic_menu_save);
        menu.add(0, MENU_CANCEL, 0, R.string.menu_cancel)
            .setIcon(android.R.drawable.ic_menu_close_clear_cancel);
        return true;
    }
    
    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        super.onMenuOpened(featureId, menu);
        // if the airplane is enable or call state not idle, then disable the Menu. 
        if (menu != null) {
            menu.setGroupEnabled(0, mNotInMmsTransaction && mIsCallStateIdle && !mAirplaneModeEnabled && mDualSimMode != 0);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_DELETE:
            deleteApn();
            return true;
        case MENU_SAVE:
            if (mSourceType == 0) {
                showDialog(DIALOG_CONFIRM_CHANGE);
            } else if (validateAndSave(false)) {
                finish();
            }
            return true;
        case MENU_CANCEL:
            if (mNewApn && (mUri != null)) {
                getContentResolver().delete(mUri, null, null);
            }
            finish();
            return true;
        default: break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK: 
                if (validateAndSave(false)) {
                    finish();
                }
                return true;
            
            default: 
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onSaveInstanceState(Bundle icicle) {
        super.onSaveInstanceState(icicle);
        if (validateAndSave(true)) {
            ///M: judge whether mCursor's count is 0
            if (mCursor != null) {
                if (mCursor.getCount() > 0) {
                    icicle.putInt(SAVED_POS, mCursor.getInt(ID_INDEX));
                }
            }
        }
    }

    /**
     * Check the key fields' validity and save if valid.
     * @param force save even if the fields are not valid, if the app is
     *        being suspended
     * @return true if the data was saved
     */
    private boolean validateAndSave(boolean force) {
        String name = checkNotSet(mName.getText());
        String apn = checkNotSet(mApn.getText());
        String mcc = checkNotSet(mMcc.getText());
        String mnc = checkNotSet(mMnc.getText());

        if (getErrorMsg() != null && !force) {
            showDialog(ERROR_DIALOG_ID);
            return false;
        }

        if (!mCursor.moveToFirst() && !mNewApn) {
            Log.w(TAG,
                    "Could not go to the first row in the Cursor when saving data.");
            return false;
        }

        // If it's a new APN and a name or apn haven't been entered, then erase the entry
        if (force && mNewApn && name.length() < 1 && apn.length() < 1) {
            if (mUri != null) {
                getContentResolver().delete(mUri, null, null);
                mUri = null;
            }    
            return false;
        }

        ContentValues values = new ContentValues();

        // Add a dummy name "Untitled", if the user exits the screen without adding a name but 
        // entered other information worth keeping.
        values.put(Telephony.Carriers.NAME,
                name.length() < 1 ? getResources().getString(R.string.untitled_apn) : name);
        values.put(Telephony.Carriers.APN, apn);
        values.put(Telephony.Carriers.PROXY, checkNotSet(mProxy.getText()));
        values.put(Telephony.Carriers.PORT, checkNotSet(mPort.getText()));
        values.put(Telephony.Carriers.MMSPROXY, checkNotSet(mMmsProxy.getText()));
        values.put(Telephony.Carriers.MMSPORT, checkNotSet(mMmsPort.getText()));
        values.put(Telephony.Carriers.USER, checkNotSet(mUser.getText()));
        values.put(Telephony.Carriers.SERVER, checkNotSet(mServer.getText()));
        values.put(Telephony.Carriers.PASSWORD, checkNotSet(mPassword.getText()));
        values.put(Telephony.Carriers.MMSC, checkNotSet(mMmsc.getText()));

        boolean isMVNO = false;
        try {
            ITelephony telephony = ITelephony.Stub.asInterface(ServiceManager.getService(Context.TELEPHONY_SERVICE));
            if (FeatureOption.MTK_MVNO_SUPPORT) {
            if (FeatureOption.MTK_GEMINI_SUPPORT) {
                    isMVNO = telephony.isIccCardProviderAsMvnoGemini(mSimId);
                } else {
                    isMVNO = telephony.isIccCardProviderAsMvno();
                }
            }
        } catch (android.os.RemoteException e) {
            Xlog.d(TAG, "RemoteException");
        }
        if (FeatureOption.MTK_MVNO_SUPPORT && isMVNO) {
            if (mIMSI != null && !mIMSI.isEmpty()) {
                values.put(Telephony.Carriers.IMSI, checkNotSet(mIMSI));
            } else if (mSpn != null && !mSpn.isEmpty()){
                values.put(Telephony.Carriers.SPN, checkNotSet(mSpn));
            } else {
                values.put(Telephony.Carriers.PNN, checkNotSet(mPnn));
            }
        }

        String authVal = mAuthType.getValue();
        if (authVal != null) {
            values.put(Telephony.Carriers.AUTH_TYPE, Integer.parseInt(authVal));
        }

        values.put(Telephony.Carriers.PROTOCOL, checkNotSet(mProtocol.getValue()));
        values.put(Telephony.Carriers.ROAMING_PROTOCOL, checkNotSet(mRoamingProtocol.getValue()));

        values.put(Telephony.Carriers.TYPE, checkNotSet(mApnTypeList.getTypeString()));
        values.put(Telephony.Carriers.CARRIER_ENABLED, mCarrierEnabled.isChecked() ? 1 : 0);

        values.put(Telephony.Carriers.MCC, mcc);
        values.put(Telephony.Carriers.MNC, mnc);

        values.put(Telephony.Carriers.NUMERIC, mcc + mnc);

        if (mCurMnc != null && mCurMcc != null) {
            if (mCurMnc.equals(mnc) && mCurMcc.equals(mcc)) {
                values.put(Telephony.Carriers.CURRENT, 1);
            }
        }

        String bearerVal = mBearer.getValue();
        if (bearerVal != null) {
            values.put(Telephony.Carriers.BEARER, Integer.parseInt(bearerVal));
        }
        if (mNewApn) {
            values.put(Telephony.Carriers.SOURCE_TYPE, 1); 
        }

        if (mUri == null) {
            Xlog.i(TAG, "former inserted URI was already deleted, insert a new one");
            mUri = getContentResolver().insert(getIntent().getData(), new ContentValues());
        }
        if (mUri != null) {
            getContentResolver().update(mUri, values, null, null);
        }


        return true;
    }

    private String getErrorMsg() {
        String errorMsg = null;

        String name = checkNotSet(mName.getText());
        String apn = checkNotSet(mApn.getText());
        String mcc = checkNotSet(mMcc.getText());
        String mnc = checkNotSet(mMnc.getText());

        if (name.length() < 1) {
            errorMsg = mRes.getString(R.string.error_name_empty);
        } else if (apn.length() < 1) {
            errorMsg = mRes.getString(R.string.error_apn_empty);
        } else if (mcc.length() != 3) {
            errorMsg = mRes.getString(R.string.error_mcc_not3);
        } else if ((mnc.length() & 0xFFFE) != 2) {
            errorMsg = mRes.getString(R.string.error_mnc_not23);
        }

        return errorMsg;
    }

    @Override
    protected Dialog onCreateDialog(int id) {

        if (id == ERROR_DIALOG_ID) {
            String msg = getErrorMsg();

            return new AlertDialog.Builder(this)
                    .setTitle(R.string.error_title)
                    .setPositiveButton(android.R.string.ok, null)
                    .setMessage(msg)
                    .create();
        } else if (id == DIALOG_CONFIRM_CHANGE) {               
            return  new AlertDialog.Builder(this)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle(R.string.error_title)
            .setMessage(getString(R.string.apn_predefine_change_dialog_notice))
            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog,int which) {
                    if (validateAndSave(false)) {
                        finish();
                    }
                }
            })
            .setNegativeButton(R.string.cancel,null)
            .create();                

        }
        return super.onCreateDialog(id);
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        super.onPrepareDialog(id, dialog);

        if (id == ERROR_DIALOG_ID) {
            String msg = getErrorMsg();

            if (msg != null) {
                ((AlertDialog)dialog).setMessage(msg);
            }
        }
    }

    private void deleteApn() {
        if (mUri != null) {
            getContentResolver().delete(mUri, null, null);
        }
        finish();
    }

    private String starify(String value) {
        if (value == null || value.length() == 0) {
            return sNotSet;
        } else {
            char[] password = new char[value.length()];
            for (int i = 0; i < password.length; i++) {
                password[i] = '*';
            }
            return new String(password);
        }
    }

    private String checkNull(String value) {
        if (value == null || value.length() == 0) {
            return sNotSet;
        } else {
            return value;
        }
    }

    private String checkNotSet(String value) {
        if (value == null || value.equals(sNotSet)) {
            return "";
        } else {
            return value;
        }
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference pref = findPreference(key);
        if (pref != null) {
            if (pref.equals(mPassword)) {
                pref.setSummary(starify(sharedPreferences.getString(key, "")));
            } else if (key.equals("carrier_enabled")) {
                pref.setSummary(checkNull(String.valueOf(sharedPreferences.getBoolean(key, true)))); 
            } else {
                pref.setSummary(checkNull(sharedPreferences.getString(key, ""))); 
            }
        }
    }
}
