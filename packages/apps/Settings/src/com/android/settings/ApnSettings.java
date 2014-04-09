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

package com.android.settings;

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
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.provider.Settings;
import android.provider.Telephony;
import android.provider.Telephony.SIMInfo;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;


import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.TelephonyProperties;

import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.settings.ext.IRcseOnlyApnExtension;
import com.mediatek.settings.ext.IRcseOnlyApnExtension.OnRcseOnlyApnStateChangedListener;
import com.mediatek.settings.ext.ISettingsMiscExt;
import com.mediatek.xlog.Xlog;

import java.util.ArrayList;
import java.util.List;

public class ApnSettings extends PreferenceActivity implements
        Preference.OnPreferenceChangeListener {
    static final String TAG = "ApnSettings";

    public static final String EXTRA_POSITION = "position";
    public static final String RESTORE_CARRIERS_URI = "content://telephony/carriers/restore";
    // public static final String RESTORE_CARRIERS_URI_GEMINI = "content://telephony/carriers_gemini/restore";
    private static final String RESTORE_CARRIERS_URI_GEMINI_SIM1 = "content://telephony/carriers_sim1/restore";
    private static final String RESTORE_CARRIERS_URI_GEMINI_SIM2 = "content://telephony/carriers_sim2/restore";

    public static final String PREFERRED_APN_URI = "content://telephony/carriers/preferapn";
    private static final String PREFERRED_APN_URI_GEMINI_SIM1 = "content://telephony/carriers_sim1/preferapn";
    private static final String PREFERRED_APN_URI_GEMINI_SIM2 = "content://telephony/carriers_sim2/preferapn";
    ///M: add for GEMINI+
    private static final String RESTORE_CARRIERS_URI_GEMINI_SIM3 = "content://telephony/carriers_sim3/restore";
    private static final String RESTORE_CARRIERS_URI_GEMINI_SIM4 = "content://telephony/carriers_sim4/restore";
    private static final String PREFERRED_APN_URI_GEMINI_SIM3 = "content://telephony/carriers_sim3/preferapn";
    private static final String PREFERRED_APN_URI_GEMINI_SIM4 = "content://telephony/carriers_sim4/preferapn";


    public static final String APN_ID = "apn_id";

    private static final int ID_INDEX = 0;
    private static final int NAME_INDEX = 1;
    private static final int APN_INDEX = 2;
    private static final int TYPES_INDEX = 3;
    private static final int SOURCE_TYPE_INDEX = 4;
    
    public static final int SIM_CARD_SINGLE = -1;
    public static final int SIM_CARD_UNDEFINED = -1;

    protected static final int MENU_NEW = Menu.FIRST;
    private static final int MENU_RESTORE = Menu.FIRST + 1;

    private static final int EVENT_RESTORE_DEFAULTAPN_START = 1;
    private static final int EVENT_RESTORE_DEFAULTAPN_COMPLETE = 2;
    private static final int EVENT_SERVICE_STATE_CHANGED = 5;

    private static final int DIALOG_RESTORE_DEFAULTAPN = 1001;
    private static final int DIALOG_APN_DISABLED = 2000;
    

    protected static final Uri DEFAULTAPN_URI = Uri.parse(RESTORE_CARRIERS_URI);
    private static final Uri PREFERAPN_URI = Uri.parse(PREFERRED_APN_URI);

    //private static final Uri DEFAULTAPN_URI_GEMINI = Uri.parse(RESTORE_CARRIERS_URI_GEMINI);
    private static final Uri DEFAULTAPN_URI_GEMINI_SIM1 = Uri.parse(RESTORE_CARRIERS_URI_GEMINI_SIM1);
    private static final Uri DEFAULTAPN_URI_GEMINI_SIM2 = Uri.parse(RESTORE_CARRIERS_URI_GEMINI_SIM2);

    private static final Uri PREFERAPN_URI_GEMINI_SIM1 = Uri.parse(PREFERRED_APN_URI_GEMINI_SIM1);
    private static final Uri PREFERAPN_URI_GEMINI_SIM2 = Uri.parse(PREFERRED_APN_URI_GEMINI_SIM2);
    ///M: add for GEMINI+
    private static final Uri DEFAULTAPN_URI_GEMINI_SIM3 = Uri.parse(RESTORE_CARRIERS_URI_GEMINI_SIM3);
    private static final Uri DEFAULTAPN_URI_GEMINI_SIM4 = Uri.parse(RESTORE_CARRIERS_URI_GEMINI_SIM4);
    private static final Uri PREFERAPN_URI_GEMINI_SIM3 = Uri.parse(PREFERRED_APN_URI_GEMINI_SIM3);
    private static final Uri PREFERAPN_URI_GEMINI_SIM4 = Uri.parse(PREFERRED_APN_URI_GEMINI_SIM4);



    protected boolean mRestoreDefaultApnMode;
    protected boolean mIsCallStateIdle = true;  
    protected boolean mAirplaneModeEnabled = false; 
    private int mDualSimMode = -1;
    protected String mNumeric;
    protected int mSelectableApnCount = 0;
    

    private String mSpn = "";
    private String mIMSI = "";
    private String mPnn = "";


    private static final String TRANSACTION_START = "com.android.mms.transaction.START";
    private static final String TRANSACTION_STOP = "com.android.mms.transaction.STOP";
    private static boolean sNotInMmsTransation = true;

    protected TelephonyManager mTelephonyManager;

    private RestoreApnUiHandler mRestoreApnUiHandler;
    private RestoreApnProcessHandler mRestoreApnProcessHandler;
    private HandlerThread mRestoreDefaultApnThread;

    protected String mSelectedKey;
    protected int mSimId;
    protected Uri mUri;
    protected Uri mDefaultApnUri;
    protected Uri mRestoreCarrierUri;

    private IntentFilter mMobileStateFilter;
    private BroadcastReceiver mReceiver;

    public static final String TETHER_TYPE = "tethering";
    public static final String APN_TYPE = "apn_type";
    public static final String RCSE_TYPE = "rcse";
    protected boolean mIsTetherApn = false;
    private boolean mIsTetehred = false;

    private ISettingsMiscExt mExt;
    private IRcseOnlyApnExtension mRcseExt;
    private OnRcseOnlyApnStateChangedListener mListener = new OnRcseOnlyApnStateChangedListener() {
        @Override
        public void onRcseOnlyApnStateChanged(boolean isEnabled) {
            Xlog.e(TAG, "onRcseOnlyApnStateChanged()-current state is " + isEnabled);
            fillList(mSimId);
        }
    };

    private final BroadcastReceiver mMobileStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED)) {
                PhoneConstants.DataState state = getMobileDataState(intent);
                switch (state) {
                case CONNECTED:
                    // Gemini: parameter is simId
                    // 0: single sim card & gemini sim1 card
                    // 1: gemini sim2 card
                    if (!FeatureOption.MTK_GEMINI_SUPPORT) {
                        fillList(SIM_CARD_SINGLE);
                    } else {
                        int simId = intent.getIntExtra(PhoneConstants.GEMINI_SIM_ID_KEY, SIM_CARD_SINGLE);
                        Xlog.d(TAG,"Get sim Id in broadcast received is:" + simId);
                        if (simId == mSimId) {
                            fillList(mSimId);
                        }
                    }
                    break;
                default: 
                    break;
                }
            } else if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
                mAirplaneModeEnabled = intent.getBooleanExtra("state", false);
                Xlog.d(TAG, "AIRPLANE_MODE state changed: " + mAirplaneModeEnabled);
                getPreferenceScreen().setEnabled(!mAirplaneModeEnabled && mDualSimMode != 0);
            } else if (action.equals(TelephonyIntents.ACTION_SIM_STATE_CHANGED)) {
                Xlog.d(TAG, "receiver: ACTION_SIM_STATE_CHANGED in ApnSettings");
if ((mNumeric != null && mNumeric.equals("-1")) || 
                    ( (mSpn == null || mSpn.isEmpty())
                      && (mIMSI == null || mIMSI.isEmpty())
                      && (mPnn == null || mPnn.isEmpty())
                    ) ) {
                    initSimState();
                    if (FeatureOption.MTK_MVNO_SUPPORT) {
                        setSpn();
                        setIMSI();
                        setPnn();
                    }

                    if (!mRestoreDefaultApnMode) {
                        fillList(mSimId);
                    } 
                }
            } else if (action.equals(TRANSACTION_START)) {
                Xlog.d(TAG, "receiver: TRANSACTION_START in ApnSettings");
                sNotInMmsTransation = false;
                getPreferenceScreen().setEnabled(false);
            } else if (action.equals(TRANSACTION_STOP)) {
                Xlog.d(TAG, "receiver: TRANSACTION_STOP in ApnSettings");
                sNotInMmsTransation = true;
                getPreferenceScreen().setEnabled(getScreenEnableState());
            } else if (action.equals(Intent.ACTION_DUAL_SIM_MODE_CHANGED)) {
                mDualSimMode  = intent.getIntExtra(Intent.EXTRA_DUAL_SIM_MODE,-1);                
                Xlog.d(TAG,"receiver, new dual sim mode" + mDualSimMode);
                getPreferenceScreen().setEnabled(!mAirplaneModeEnabled && mDualSimMode != 0);
            } else if (action.equals(TelephonyIntents.ACTION_SIM_INFO_UPDATE)) {
                ///M: add for sim hot swap {
                List<SIMInfo> temp = SIMInfo.getInsertedSIMList(ApnSettings.this);
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
            mIsCallStateIdle = mTelephonyManager.getCallState() == TelephonyManager.CALL_STATE_IDLE;
        }

    };

    private static PhoneConstants.DataState getMobileDataState(Intent intent) {
        String str = intent.getStringExtra(PhoneConstants.STATE_KEY);
        if (str != null) {
            return Enum.valueOf(PhoneConstants.DataState.class, str);
        } else {
            return PhoneConstants.DataState.DISCONNECTED;
        }
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.apn_settings);
        getListView().setItemsCanFocus(true);
        
        mExt = Utils.getMiscPlugin(this);
        mRcseExt = Utils.getRcseApnPlugin(this);
        if (mRcseExt != null) {
            mRcseExt.addRcseOnlyApnStateChanged(mListener);
        } else {
            Xlog.d(TAG, "mApnPlugin is null");
        }
        initSimState();
        
        mMobileStateFilter = getIntentFilter();
        mReceiver = getBroadcastReceiver();
        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_SERVICE_STATE);
        if (FeatureOption.MTK_MVNO_SUPPORT) {
            setSpn();
            setIMSI();
            setPnn();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        this.setIntent(intent);

        initSimState();
    }
    
    /** For inheritecne.
    */
    protected IntentFilter getIntentFilter() {
        IntentFilter filter = new IntentFilter(
                TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED);
        
        filter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED); 
        filter.addAction(TelephonyIntents.ACTION_SIM_STATE_CHANGED); 
        filter.addAction(TRANSACTION_START);
        filter.addAction(TRANSACTION_STOP);
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            filter.addAction(Intent.ACTION_DUAL_SIM_MODE_CHANGED);
        }
        ///M: add for hot swap {
        filter.addAction(TelephonyIntents.ACTION_SIM_INFO_UPDATE);
        ///@}
        return filter;
    }
    /** For inheritecne.
    */    
    protected BroadcastReceiver  getBroadcastReceiver() {
        return mMobileStateReceiver;
    }
    
    private void initSimState() {
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            Intent it = getIntent();
            mSimId = it.getIntExtra("simId", SIM_CARD_UNDEFINED);
            if (SIMInfo.getInsertedSIMCount(this) > 1) {
                SIMInfo siminfo = SIMInfo.getSIMInfoBySlot(this, mSimId);
                if (siminfo != null) {
                    setTitle(siminfo.mDisplayName);
                }
            }
            Xlog.d(TAG, "GEMINI_SIM_ID_KEY = " + mSimId);
        } else {
                Xlog.w(TAG,"Not support GEMINI");
                mSimId = SIM_CARD_SINGLE;
        }
        switch (mSimId) {
            case SIM_CARD_SINGLE:
                mUri = Telephony.Carriers.CONTENT_URI;
                mNumeric = SystemProperties.get(TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC, "-1");
                mDefaultApnUri = DEFAULTAPN_URI;
                mRestoreCarrierUri = PREFERAPN_URI;
                break;
            case PhoneConstants.GEMINI_SIM_1:
                mUri = Telephony.Carriers.SIM1Carriers.CONTENT_URI;
                mNumeric = SystemProperties.get(TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC, "-1");
                mDefaultApnUri = DEFAULTAPN_URI_GEMINI_SIM1;
                mRestoreCarrierUri = PREFERAPN_URI_GEMINI_SIM1;
                break;
            case PhoneConstants.GEMINI_SIM_2:
                mUri = Telephony.Carriers.SIM2Carriers.CONTENT_URI;
                mNumeric = SystemProperties.get(TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC_2, "-1");
                mDefaultApnUri = DEFAULTAPN_URI_GEMINI_SIM2;
                mRestoreCarrierUri = PREFERAPN_URI_GEMINI_SIM2;
                break;   
            case PhoneConstants.GEMINI_SIM_3:
                mUri = Telephony.Carriers.SIM3Carriers.CONTENT_URI;
                mNumeric = SystemProperties.get(TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC_3, "-1");
                mDefaultApnUri = DEFAULTAPN_URI_GEMINI_SIM3;
                mRestoreCarrierUri = PREFERAPN_URI_GEMINI_SIM3;
                break;     
            case PhoneConstants.GEMINI_SIM_4:
                mUri = Telephony.Carriers.SIM4Carriers.CONTENT_URI;
                mNumeric = SystemProperties.get(TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC_4, "-1");
                mDefaultApnUri = DEFAULTAPN_URI_GEMINI_SIM4;
                mRestoreCarrierUri = PREFERAPN_URI_GEMINI_SIM4;
                break;     
           
            default:
                Xlog.i(TAG,"Incorrect sim id ");
                if (FeatureOption.MTK_GEMINI_SUPPORT) {
                    Intent intent = new Intent();
                    intent.setClassName("com.android.phone", "com.mediatek.settings.MultipleSimActivity");
                    intent.putExtra("TARGET_CLASS", "com.android.settings.ApnSettings");
                    startActivity(intent);
                }
                finish();
                break;
            }
        
        Xlog.d(TAG, "mNumeric " + mNumeric);
        Xlog.d(TAG, "mUri = " + mUri);        
    }
    @Override
    protected void onResume() {
        super.onResume();

        registerReceiver(mReceiver, mMobileStateFilter);

        mAirplaneModeEnabled =  AirplaneModeEnabler.isAirplaneModeOn(ApnSettings.this);
        mDualSimMode = Settings.System.getInt(getContentResolver(),Settings.System.DUAL_SIM_MODE_SETTING,-1);
        
        sNotInMmsTransation = isMMSNotTransaction();
        if (mAirplaneModeEnabled || mDualSimMode == 0) {
            showDialog(DIALOG_APN_DISABLED);
        }

        if (!mRestoreDefaultApnMode) {
            fillList(mSimId);
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
    protected void onPause() {
        super.onPause();
        
        unregisterReceiver(mReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
        Xlog.d(TAG,"onDestroy set PhoneStateListener.LISTEN_NONE!");

        if (mRestoreDefaultApnThread != null) {
            mRestoreDefaultApnThread.quit();
        }
        if (mRcseExt != null) {
            mRcseExt.removeRcseOnlyApnStateChanged(mListener);
        }
    }

    /** for inheritence.
    */
    protected String getFillListQuery() {
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
        boolean flagImsi = false;
        boolean flagSpn = false;
        boolean flagPnn = false;
        String sqlStr = "";

        Xlog.d(TAG,"[isMVNO =" + isMVNO + "]");
        if (FeatureOption.MTK_MVNO_SUPPORT) {
            if (isMVNO) {
                if (mIMSI != null && !mIMSI.isEmpty()) {
                    flagImsi = true;
                    sqlStr += " imsi=\"" + mIMSI + "\"";
                }
                if (mSpn != null && !mSpn.isEmpty()) {
                    flagSpn = true;
                    if (flagImsi) {
                        sqlStr += " or spn=\"" + mSpn + "\"";
                    } else {
                        sqlStr += " spn=\"" + mSpn + "\"";
                    }
                    
                }
                if (mPnn != null && !mPnn.isEmpty()) {
                    flagPnn = true;
                    if (flagImsi || flagSpn) {
                        sqlStr += " or pnn=\"" + mPnn + "\"";
                    } else {
                        sqlStr += " pnn=\"" + mPnn + "\"";
                    }
                    
                }
            } else {
                sqlStr = "(spn is NULL or spn=\"\") and (imsi is NULL or imsi=\"\") and (pnn is NULL or pnn=\"\") ";
            }
        }
        String result = "numeric=\"" + mNumeric + "\" and ( " + sqlStr + ")";
        Xlog.e(TAG,"getFillListQuery result: " + result);
        return result;
    }

    /** for inheritence.
    */
    protected boolean getScreenEnableState() {        
        boolean simReady = true;
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            simReady = TelephonyManager.SIM_STATE_READY == TelephonyManager.getDefault().getSimStateGemini(mSimId);    
        } else {
            simReady = TelephonyManager.SIM_STATE_READY == TelephonyManager.getDefault().getSimState();    
        }
        mIsCallStateIdle = mTelephonyManager.getCallState() == TelephonyManager.CALL_STATE_IDLE;  
        return sNotInMmsTransation && mIsCallStateIdle && !mAirplaneModeEnabled && simReady;
    }

    private void setSpn() {
        try {
            ITelephony telephony = ITelephony.Stub.asInterface(ServiceManager.getService(Context.TELEPHONY_SERVICE));
            if (FeatureOption.MTK_GEMINI_SUPPORT) {
                mSpn = telephony.getSpNameInEfSpnGemini(mSimId);
            } else {
                mSpn = telephony.getSpNameInEfSpn();
            }
            Xlog.d(TAG, "spn = " + mSpn);
        } catch (android.os.RemoteException e) {
            Xlog.d(TAG, "RemoteException");
        }
    }

    private void setIMSI() {
        try {
            ITelephony telephony = ITelephony.Stub.asInterface(ServiceManager.getService(Context.TELEPHONY_SERVICE));
            if (FeatureOption.MTK_GEMINI_SUPPORT) {
                mIMSI = telephony.isOperatorMvnoForImsiGemini(mSimId);
            } else {
                mIMSI = telephony.isOperatorMvnoForImsi();
            }
            Xlog.d(TAG, "IMSI = " + mIMSI);
        } catch (android.os.RemoteException e) {
            Xlog.d(TAG, "RemoteException");
        }
        Xlog.d(TAG, "IMSI = " + mIMSI);
    }

    private void setPnn() {
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            mPnn = mTelephonyManager.isOperatorMvnoForEfPnnGemini(mSimId);
        } else {
            mPnn = mTelephonyManager.isOperatorMvnoForEfPnn();
        }
        Xlog.d(TAG, "Pnn = " + mPnn);
    }

    private void fillList(int simId) {
        String where = getFillListQuery();
        Xlog.e(TAG,"fillList where: " + where);

        if (mUri == null) {
            Xlog.e(TAG,"fillList, mUri null !");
            finish();
            return;
        }
        Cursor cursor = getContentResolver().query(mUri, new String[] {
                "_id", "name", "apn", "type","sourcetype"}, where, null, null);

        PreferenceGroup apnList = (PreferenceGroup) findPreference("apn_list");
        apnList.removeAll();

        ArrayList<Preference> mmsApnList = new ArrayList<Preference>();
        
        boolean keySetChecked = false;

        mSelectedKey = getSelectedApnKey();
        Xlog.d(TAG, "fillList : mSelectedKey = " + mSelectedKey); 
        
        cursor.moveToFirst();

        while (!cursor.isAfterLast()) {
            String type = cursor.getString(TYPES_INDEX);

            if (mIsTetherApn && !TETHER_TYPE.equals(type)) {
                cursor.moveToNext();
                continue;
            }

            String name = cursor.getString(NAME_INDEX);
            String apn = cursor.getString(APN_INDEX);
            String key = cursor.getString(ID_INDEX);

            int sourcetype = cursor.getInt(SOURCE_TYPE_INDEX);

            if ("cmmail".equals(apn) && sourcetype == 0) {
                cursor.moveToNext();
                continue;
            }
            if (RCSE_TYPE.equals(type) && mRcseExt != null) {
                if (!mRcseExt.isRcseOnlyApnEnabled()) {
                    cursor.moveToNext();
                    Xlog.d(TAG, "Vodafone not matched");
                    continue;
                } else {
                    Xlog.d(TAG, "Vodafone matched");
                }
            }

            ApnPreference pref = new ApnPreference(this);
            
            pref.setSimId(simId);    //set pre sim id info to the ApnEditor
            pref.setKey(key);
            pref.setTitle(name);
            pref.setSummary(apn);
            pref.setPersistent(false);
            pref.setSourceType(sourcetype);
            pref.setOnPreferenceChangeListener(this);

            boolean isEditable = mExt.isAllowEditPresetApn(type, apn, mNumeric);
            pref.setApnEditable((sourcetype != 0) || isEditable);
            
            //All tether apn will be selectable for otthers , mms will not be selectable.
            boolean selectable = true;
            if (TETHER_TYPE.equals(type)) {
                selectable = mIsTetherApn;
            } else {
                selectable = !"mms".equals(type);
            }
            pref.setSelectable(selectable);

            if (selectable) {
                if ((mSelectedKey != null) && mSelectedKey.equals(key)) {
                    pref.setChecked();
                    keySetChecked = true;
                    Xlog.d(TAG, "apn key: " + key + " set.");
                }
                apnList.addPreference(pref);
                Xlog.i(TAG, "key:  " + key + " added!"); 
            } else {
                mmsApnList.add(pref);
            }
            cursor.moveToNext();
        }
        cursor.close();

        mSelectableApnCount = apnList.getPreferenceCount();
        //if no key selected, choose the 1st one.
        if (!keySetChecked && mSelectableApnCount > 0) {
            int[] sourceTypeArray = new int[mSelectableApnCount];
            for (int i = 0; i < mSelectableApnCount; i++) {
                sourceTypeArray[i] = ((ApnPreference)apnList.getPreference(i)).getSourceType();
            }
            ApnPreference apnPref = (ApnPreference)mExt.getApnPref(apnList, mSelectableApnCount, sourceTypeArray);
            if (apnPref != null) {
                setSelectedApnKey(apnPref.getKey());
                apnPref.setChecked();
                Xlog.i(TAG, "Key does not match.Set key: " + apnPref.getKey() + "."); 
            }
        }

        if (!mIsTetherApn) {
            for (Preference preference : mmsApnList) {
                apnList.addPreference(preference);
            }
        }
        getPreferenceScreen().setEnabled(getScreenEnableState());        
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        addMenu(menu);
        return true;
    }
    
    /**For inheritence.
    */
    protected void addMenu(Menu menu) {
        menu.add(0, MENU_NEW, 0,
                getResources().getString(R.string.menu_new))
                .setIcon(android.R.drawable.ic_menu_add);
        menu.add(0, MENU_RESTORE, 0,
                getResources().getString(R.string.menu_restore))
                .setIcon(android.R.drawable.ic_menu_upload);
    }
    
    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        super.onMenuOpened(featureId, menu);
        // if the airplane is enable or call state not idle, then disable the Menu. 
        if (menu != null) {
            menu.setGroupEnabled(0, getScreenEnableState());
        }
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_NEW:
                addNewApn();
                return true;

            case MENU_RESTORE:
                restoreDefaultApn();
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /** for inheritence.
    */
    protected void addNewApn() {
        Intent it = new Intent(Intent.ACTION_INSERT, mUri);
        it.putExtra(PhoneConstants.GEMINI_SIM_ID_KEY, mSimId);
        startActivity(it);
    }
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Log.d(TAG, "onPreferenceChange(): Preference - " + preference
                + ", newValue - " + newValue + ", newValue type - "
                + newValue.getClass());
        if (newValue instanceof String) {
            setSelectedApnKey((String) newValue);
        }

        return true;
    }

    /** For inheritence.
    */
    protected void setSelectedApnKey(String key) {
        mSelectedKey = key;
        ContentResolver resolver = getContentResolver();

        ContentValues values = new ContentValues();
        values.put(APN_ID, mSelectedKey);
        resolver.update(mRestoreCarrierUri, values, null, null);        
    }

    private String getSelectedApnKey() {
        String key = null;

        Cursor cursor = getContentResolver().query(mRestoreCarrierUri, new String[] {"_id"},
                null, null, Telephony.Carriers.DEFAULT_SORT_ORDER);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            key = cursor.getString(ID_INDEX);
        }
        cursor.close();
        return key;
    }

    private boolean restoreDefaultApn() {
        Xlog.w(TAG, "restore Default Apn.");        
        showDialog(DIALOG_RESTORE_DEFAULTAPN);
        mRestoreDefaultApnMode = true;

        if (mRestoreApnUiHandler == null) {
            mRestoreApnUiHandler = new RestoreApnUiHandler();
        }

        if (mRestoreApnProcessHandler == null ||
            mRestoreDefaultApnThread == null) {
            mRestoreDefaultApnThread = new HandlerThread(
                    "Restore default APN Handler: Process Thread");
            mRestoreDefaultApnThread.start();
            mRestoreApnProcessHandler = new RestoreApnProcessHandler(
                    mRestoreDefaultApnThread.getLooper(), mRestoreApnUiHandler);
        }

        mRestoreApnProcessHandler
                .sendEmptyMessage(EVENT_RESTORE_DEFAULTAPN_START);
        return true;
    }

    private class RestoreApnUiHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_RESTORE_DEFAULTAPN_COMPLETE:
                    fillList(mSimId);
                    getPreferenceScreen().setEnabled(true);
                    mRestoreDefaultApnMode = false;
                    removeDialog(DIALOG_RESTORE_DEFAULTAPN);
                    Toast.makeText(
                        ApnSettings.this,
                        getResources().getString(
                                R.string.restore_default_apn_completed),
                        Toast.LENGTH_LONG).show();
                    break;
                default:
                    break;
            }
        }
    }

    private class RestoreApnProcessHandler extends Handler {
        private Handler mRestoreApnUiHandler;

        public RestoreApnProcessHandler(Looper looper, Handler restoreApnUiHandler) {
            super(looper);
            this.mRestoreApnUiHandler = restoreApnUiHandler;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_RESTORE_DEFAULTAPN_START:
                    ContentResolver resolver = getContentResolver();
                    resolver.delete(mDefaultApnUri, null, null);                    
                    mRestoreApnUiHandler
                        .sendEmptyMessage(EVENT_RESTORE_DEFAULTAPN_COMPLETE);
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == DIALOG_RESTORE_DEFAULTAPN) {
            ProgressDialog dialog = new ProgressDialog(this);
            dialog.setMessage(getResources().getString(R.string.restore_default_apn));
            dialog.setCancelable(false);
            return dialog;
        }
        if (id == DIALOG_APN_DISABLED) {
              return new AlertDialog.Builder(ApnSettings.this)
                            .setTitle(R.string.dialog_apn_disabled)
                            .setMessage(R.string.dialog_apn_disabled_msg)
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface a0, int a1) {
                                    finish();
                                }
                            }).create();
        }
        
        return null;
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        if (id == DIALOG_RESTORE_DEFAULTAPN) {
            getPreferenceScreen().setEnabled(false);
        }
    }
}
