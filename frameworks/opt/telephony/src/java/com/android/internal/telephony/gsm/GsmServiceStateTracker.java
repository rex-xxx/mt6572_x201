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

package com.android.internal.telephony.gsm;

import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.DataConnectionTracker;
import com.android.internal.telephony.EventLogTags;
import com.android.internal.telephony.gemini.GeminiPhone;
import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.IccCardStatus;
import com.android.internal.telephony.IccRecords;
import com.android.internal.telephony.MccTable;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.RestrictedState;
import com.android.internal.telephony.RILConstants;
import com.android.internal.telephony.ServiceStateTracker;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.TelephonyProperties;
import com.android.internal.telephony.UiccCard;
import com.android.internal.telephony.UiccCardApplication;
import com.android.internal.telephony.IccCardApplicationStatus.AppState;
import com.android.internal.telephony.uicc.UiccController;

import android.app.Activity;
import android.app.Service;
import android.content.ServiceConnection;
import android.content.ComponentName;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.provider.Telephony.Intents;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.gsm.GsmCellLocation;
import android.text.TextUtils;
import android.util.EventLog;
import android.util.Log;
import android.util.TimeUtils;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.TimeZone;

//MTK-START [mtk03851][111124]MTK added
import com.mediatek.common.featureoption.FeatureOption;
import com.android.internal.telephony.RIL;
import android.provider.Telephony.SIMInfo;
import android.telephony.TelephonyManager;
import com.mediatek.common.MediatekClassFactory;
import com.mediatek.common.telephony.IServiceStateExt;
import com.android.internal.telephony.gemini.GeminiNetworkSubUtil;
import com.android.internal.telephony.PhoneFactory;
import android.net.ConnectivityManager;
//MTK-END [mtk03851][111124]MTK added

 //[ALPS00435948] MTK ADD-START
import com.mediatek.internal.R;
 //[ALPS00435948] MTK ADD-END


/**
 * {@hide}
 */
final class GsmServiceStateTracker extends ServiceStateTracker {
    static final String LOG_TAG = "GSM";
    static final boolean DBG = true;

    GSMPhone phone;
    GsmCellLocation cellLoc;
    GsmCellLocation newCellLoc;
    int mPreferredNetworkType;

    private int gprsState = ServiceState.STATE_OUT_OF_SERVICE;
    private int newGPRSState = ServiceState.STATE_OUT_OF_SERVICE;
    private int mMaxDataCalls = 1;
    private int mNewMaxDataCalls = 1;
    private int mReasonDataDenied = -1;
    private int mNewReasonDataDenied = -1;

    protected static final int EVENT_SET_GPRS_CONN_TYPE_DONE = 51;
    protected static final int EVENT_SET_GPRS_CONN_RETRY = 52;

    private int gprsConnType = 0;
    /**
     * GSM roaming status solely based on TS 27.007 7.2 CREG. Only used by
     * handlePollStateResult to store CREG roaming result.
     */
    private boolean mGsmRoaming = false;

    /**
     * Data roaming status solely based on TS 27.007 10.1.19 CGREG. Only used by
     * handlePollStateResult to store CGREG roaming result.
     */
    private boolean mDataRoaming = false;

    /**
     * Mark when service state is in emergency call only mode
     */
    private boolean mEmergencyOnly = false;

    /**
     * Sometimes we get the NITZ time before we know what country we
     * are in. Keep the time zone information from the NITZ string so
     * we can fix the time zone once know the country.
     */
    private boolean mNeedFixZoneAfterNitz = false;
    private int mZoneOffset;
    private boolean mZoneDst;
    private long mZoneTime;
    private boolean mGotCountryCode = false;
    private ContentResolver cr;

    /** Boolean is true is setTimeFromNITZString was called */
    private boolean mNitzUpdatedTime = false;

    String mSavedTimeZone;
    long mSavedTime;
    long mSavedAtTime;

    /**
     * We can't register for SIM_RECORDS_LOADED immediately because the
     * SIMRecords object may not be instantiated yet.
     */
    // remark for MR1 Migration
    //private boolean mNeedToRegForSimLoaded;

    /** Started the recheck process after finding gprs should registered but not. */
    private boolean mStartedGprsRegCheck = false;

    /** Already sent the event-log for no gprs register. */
    private boolean mReportedGprsNoReg = false;

    /**
     * The Notification object given to the NotificationManager.
     */
    private Notification mNotification;

    /** Wake lock used while setting time of day. */
    private PowerManager.WakeLock mWakeLock;
    private static final String WAKELOCK_TAG = "ServiceStateTracker";

    /** Keep track of SPN display rules, so we only broadcast intent if something changes. */
    private String curSpn = null;
    private String curPlmn = null;

    //MTK-START [ALPS415367]For MR1 Migration
    //private int curSpnRule = 0;
    private boolean curShowPlmn = false;
    private boolean curShowSpn = false;
    //MTK-END [ALPS415367]For MR1 Migration

    /** waiting period before recheck gprs and voice registration. */
    static final int DEFAULT_GPRS_CHECK_PERIOD_MILLIS = 60 * 1000;

    /* mtk01616 ALPS00236452: manufacturer maintained table for specific operator with multiple PLMN id */
    private String[][] customEhplmn = {{"46000","46002","46007"},
                                       {"45400","45402","45418"},
                                       {"45403","45404"},
    	                               {"45416","45419"},
                                       {"45501","45504"},
                                       {"45503","45505"},
                                       {"45002","45008"},
                                       {"52501","52502"},
                                       {"43602","43612"},
                                       {"52010","52099"},
                                       {"26207","26208"},
                                       {"23430","23431","23432"},
                                       {"72402","72403","72404"},
                                       {"72406","72410","72411","72423"},
                                       {"72432","72433","72434"},
                                       {"31026","31031","310160","310200","310210","310220","310230","310240","310250","310260","310270","310660"},
                                       {"310150","310170","310380","310410"}};

    /** Notification type. */
    static final int PS_ENABLED = 1001;            // Access Control blocks data service
    static final int PS_DISABLED = 1002;           // Access Control enables data service
    static final int CS_ENABLED = 1003;            // Access Control blocks all voice/sms service
    static final int CS_DISABLED = 1004;           // Access Control enables all voice/sms service
    static final int CS_NORMAL_ENABLED = 1005;     // Access Control blocks normal voice/sms service
    static final int CS_EMERGENCY_ENABLED = 1006;  // Access Control blocks emergency call service

    /** Notification id. */
    static final int PS_NOTIFICATION = 888;  // Id to update and cancel PS restricted
    static final int CS_NOTIFICATION = 999;  // Id to update and cancel CS restricted

    /** mtk01616_120613 Notification id. */
    static final int REJECT_NOTIFICATION = 890;
    static final int REJECT_NOTIFICATION_2 = 8902;
    boolean dontUpdateNetworkStateFlag = false;

//MTK-START [mtk03851][111124]MTK added
    protected static final int EVENT_SET_AUTO_SELECT_NETWORK_DONE = 50;
    /** Indicate the first radio state changed **/
    private boolean mFirstRadioChange = true;
    private boolean mIs3GTo2G = false;

    /** Auto attach PS service when SIM Ready **/
    private int mAutoGprsAttach = 1;
    private int mSimId;
    /**
     *  Values correspond to ServiceStateTracker.DATA_ACCESS_ definitions.
     */
    private int ps_networkType = 0;
    private int newps_networkType = 0;
    private int DEFAULT_GPRS_RETRY_PERIOD_MILLIS = 30 * 1000;
    private int explict_update_spn = 0;


    private String mLastRegisteredPLMN = null;
    private String mLastPSRegisteredPLMN = null;

    private boolean mEverIVSR = false;	/* ALPS00324111: at least one chance to do IVSR  */

    private RegistrantList ratPsChangedRegistrants = new RegistrantList();
    private RegistrantList ratCsChangedRegistrants = new RegistrantList();

    /** Notification id. */
    static final int PS_NOTIFICATION_2 = 8882;  // Id to update and cancel PS restricted
    static final int CS_NOTIFICATION_2 = 9992;  // Id to update and cancel CS restricted
    private IServiceStateExt mServiceStateExt;
//MTK-END [mtk03851][111124]MTK added

    private boolean mIsImeiLock = false;

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            log("BroadcastReceiver: " + intent.getAction());
            if (intent.getAction().equals(Intent.ACTION_LOCALE_CHANGED)) {
                // update emergency string whenever locale changed
                updateSpnDisplay();
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
            	log("ACTION_SCREEN_ON");
                pollState();
            	log("set explict_update_spn = 1");
                explict_update_spn = 1;
                if (mServiceStateExt.needEMMRRS()) {
                    if (mSimId == getDataConnectionSimId()) {
                        getEINFO(EVENT_ENABLE_EMMRRS_STATUS);
                    }
                }
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                log("ACTION_SCREEN_OFF");
                if (mServiceStateExt.needEMMRRS()) {
                    if (mSimId == getDataConnectionSimId()) {
                        getEINFO(EVENT_DISABLE_EMMRRS_STATUS);
                    }
                }
            } else if (intent.getAction().equals("com.mtk.TEST_TRM")){ //ALPS00242220
                int mode = intent.getIntExtra("mode", 2); 
                int slot = intent.getIntExtra("slot", 0);//RFU
                log("TEST mode"+mode+" slot="+slot);

                if (PhoneFactory.isDualTalkMode()) {
                    if (PhoneFactory.getTelephonyMode() == PhoneFactory.MODE_0_NONE) {
                        if((mode == 2)&&(mSimId == PhoneConstants.GEMINI_SIM_1))
                            phone.setTRM(2,null);
                    } else {
                        if(mode == 2) {
                            if ((PhoneFactory.getFirstMD() == 1 && mSimId == PhoneConstants.GEMINI_SIM_1) ||
                                (PhoneFactory.getFirstMD() == 2 && mSimId == PhoneConstants.GEMINI_SIM_2))
                            {
                                phone.setTRM(mode, null);
                            }
                        } else if (mode == 6) {
                            if ((PhoneFactory.getFirstMD() == 1 && mSimId == PhoneConstants.GEMINI_SIM_2) ||
                                (PhoneFactory.getFirstMD() == 2 && mSimId == PhoneConstants.GEMINI_SIM_1))
                            {
                                phone.setTRM(mode, null);
                            }
                        }
                    }
                } else {
                    if((mode == 2)&&(mSimId == PhoneConstants.GEMINI_SIM_1))
                        phone.setTRM(2,null);
                }
            }
        }
    };

    private ContentObserver mAutoTimeObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            Log.i("GsmServiceStateTracker", "Auto time state changed");
            revertToNitzTime();
        }
    };

    private ContentObserver mAutoTimeZoneObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            Log.i("GsmServiceStateTracker", "Auto time zone state changed");
            revertToNitzTimeZone();
        }
    };

    //MTK-START [MTK80515] [ALPS00368272]
    private ContentObserver mDataConnectionSimSettingObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            log("Data Connection Sim Setting changed");
            if (mServiceStateExt.needEMMRRS()) {
                if (mSimId == getDataConnectionSimId()) {
                    getEINFO(EVENT_ENABLE_EMMRRS_STATUS);
                } else {
                    getEINFO(EVENT_DISABLE_EMMRRS_STATUS);
                }
            }
        }
    };
    //MTK-END [MTK80515] [ALPS00368272]

    public GsmServiceStateTracker(GSMPhone phone) {
        super(phone, phone.mCM, new CellInfoGsm(), phone.getMySimId());

        this.phone = phone;
        cellLoc = new GsmCellLocation();
        newCellLoc = new GsmCellLocation();

//MTK-START [mtk03851][111124]MTK added
        mSimId = phone.getMySimId();
//MTK-START [mtk03851][111124]MTK added

        cm = phone.mCM;
        //MTK-START [ALPS415367]For MR1 Migration
        //ss = new ServiceState(mSimId);
        //newSS = new ServiceState(mSimId);
        //cellLoc = new GsmCellLocation();
        //newCellLoc = new GsmCellLocation();
        //mSignalStrength = new SignalStrength(mSimId);
        //MTK-END [ALPS415367]For MR1 Migration

        PowerManager powerManager =
                (PowerManager)phone.getContext().getSystemService(Context.POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG);

        try{
            mServiceStateExt = MediatekClassFactory.createInstance(IServiceStateExt.class, phone.getContext());
        } catch (Exception e){
            e.printStackTrace();
        }

        cm.registerForAvailable(this, EVENT_RADIO_AVAILABLE, null);
        cm.registerForRadioStateChanged(this, EVENT_RADIO_STATE_CHANGED, null);

        cm.registerForVoiceNetworkStateChanged(this, EVENT_NETWORK_STATE_CHANGED, null);
        cm.registerForPsNetworkStateChanged(this, EVENT_PS_NETWORK_STATE_CHANGED, null);
        cm.setOnNITZTime(this, EVENT_NITZ_TIME, null);

        //MTK-START [ALPS415367]For MR1 Migration
        //cm.setOnSignalStrengthUpdate(this, EVENT_SIGNAL_STRENGTH_UPDATE, null);
        //MTK-END [ALPS415367]For MR1 Migration

        cm.setOnRestrictedStateChanged(this, EVENT_RESTRICTED_STATE_CHANGED, null);
        cm.registerForSIMReady(this, EVENT_SIM_READY, null);
        cm.setGprsDetach(this, EVENT_DATA_CONNECTION_DETACHED, null);
        cm.setInvalidSimInfo(this, EVENT_INVALID_SIM_INFO, null);//ALPS00248788
        if(mServiceStateExt.isImeiLocked())
            cm.registerForIMEILock(this, EVENT_IMEI_LOCK, null);

        // system setting property AIRPLANE_MODE_ON is set in Settings.
        int airplaneMode = Settings.Global.getInt(
                phone.getContext().getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0);
        /* for consistent UI ,SIM Management for single sim project START */
        if (!FeatureOption.MTK_GEMINI_SUPPORT) {
            int simMode = Settings.System.getInt(phone.getContext().getContentResolver(), Settings.System.DUAL_SIM_MODE_SETTING, 1);
            /* ALPS00447303 */
            Log.e(LOG_TAG, "Set mDesiredPowerState in setRadioPowerOn. simMode="+simMode+",airplaneMode="+airplaneMode);
            mDesiredPowerState = (simMode > 0) && (! (airplaneMode > 0));                        						
        }
        /* for consistent UI ,SIM Management for single sim project  END*/
        else{
            mDesiredPowerState = ! (airplaneMode > 0);
        }			
        Log.e(LOG_TAG, "Final mDesiredPowerState for single sim. [" + mDesiredPowerState + "] airplaneMode=" + airplaneMode);

        cr = phone.getContext().getContentResolver();
        cr.registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.AUTO_TIME), true,
                mAutoTimeObserver);
        cr.registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.AUTO_TIME_ZONE), true,
                mAutoTimeZoneObserver);

        setSignalStrengthDefaultValues();
        //MTK-START [ALPS415367]For MR1 Migration
        //mNeedToRegForSimLoaded = true;
        //MTK-END [ALPS415367]For MR1 Migration

        // Monitor locale change
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_LOCALE_CHANGED);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction("com.mtk.TEST_TRM");//ALPS00242220

        phone.getContext().registerReceiver(mIntentReceiver, filter);
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            mAutoGprsAttach = 0;
        }
        // Gsm doesn't support OTASP so its not needed
        phone.notifyOtaspChanged(OTASP_NOT_NEEDED);

        SystemProperties.set(TelephonyProperties.PROPERTY_ROAMING_INDICATOR_NEEDED, "false");
        SystemProperties.set(TelephonyProperties.PROPERTY_ROAMING_INDICATOR_NEEDED_2, "false");

        //MTK-START [MTK80515] [ALPS00368272]
        cr.registerContentObserver(
                Settings.System.getUriFor(Settings.System.GPRS_CONNECTION_SIM_SETTING), true,
                mDataConnectionSimSettingObserver);
        if (mServiceStateExt.needEMMRRS()) {
            if (mSimId == getDataConnectionSimId()) {
                getEINFO(EVENT_ENABLE_EMMRRS_STATUS);
            } else {
                getEINFO(EVENT_DISABLE_EMMRRS_STATUS);
            }
        }
        //MTK-END [MTK80515] [ALPS00368272]
    }

    @Override
    public void dispose() {
        checkCorrectThread();
        // Unregister for all events.
        cm.unregisterForAvailable(this);
        cm.unregisterForRadioStateChanged(this);
        cm.unregisterForVoiceNetworkStateChanged(this);
        if (mUiccApplcation != null) {mUiccApplcation.unregisterForReady(this);}

        if (mIccRecords != null) {mIccRecords.unregisterForRecordsLoaded(this);}
        cm.unSetOnRestrictedStateChanged(this);
        cm.unSetOnNITZTime(this);
        cr.unregisterContentObserver(this.mAutoTimeObserver);
        cr.unregisterContentObserver(this.mAutoTimeZoneObserver);
        if(mServiceStateExt.isImeiLocked())
            cm.unregisterForIMEILock(this);
        phone.getContext().unregisterReceiver(mIntentReceiver);
        super.dispose();
    }

    protected void finalize() {
        if(DBG) log("finalize");
    }

    @Override
    protected Phone getPhone() {
        return phone;
    }

    public void handleMessage (Message msg) {
        AsyncResult ar;
        int[] ints;
        String[] strings;
        Message message;
        int testMode = 0, attachType = 0;

        if (!phone.mIsTheCurrentActivePhone) {
            Log.e(LOG_TAG, "Received message " + msg +
                    "[" + msg.what + "] while being destroyed. Ignoring.");
            return;
        }
        switch (msg.what) {
            case EVENT_GET_SIM_RECOVERY_ON:
                break;
            case EVENT_SET_SIM_RECOVERY_ON:
                break;
            case EVENT_RADIO_AVAILABLE:
                //this is unnecessary
                //setPowerStateToDesired();
                break;

            case EVENT_SIM_READY:
                // Set the network type, in case the radio does not restore it.
                cm.setCurrentPreferredNetworkType();

                //ALPS00279048 START
                long cro_setting = Settings.System.getLong(phone.getContext().getContentResolver(),
		                                        Settings.System.CRO_SETTING,Settings.System.CRO_SETTING_DISABLE);
                log("set CRO setting="+(int)cro_setting);
                phone.setCRO((int)cro_setting,null);
                //ALPS00279048 END

                // ALPS00310187 START
                long hoo_setting = Settings.System.getLong(phone.getContext().getContentResolver(),
		                                        Settings.System.HOO_SETTING,Settings.System.HOO_SETTING_DISABLE);
                log("set HOO setting="+(int)hoo_setting);
                if(hoo_setting == 0)
                    phone.setCRO(2,null);
                else if(hoo_setting == 1)
                    phone.setCRO(3,null);
                // ALPS00310187 END

                // remark for MR1 Migration START
                // The SIM is now ready i.e if it was locked
                // it has been unlocked. At this stage, the radio is already
                // powered on.
                //if (mNeedToRegForSimLoaded) {
                //    phone.mIccRecords.get().registerForRecordsLoaded(this,
                //            EVENT_SIM_RECORDS_LOADED, null);
                //    mNeedToRegForSimLoaded = false;
                //}
                // remark for MR1 Migration END

                // restore the previous network selection.
                // [ALPS00224837], do not restore network selection, modem will decide selection mode
                //phone.restoreSavedNetworkSelection(null);

                // Set GPRS transfer type: 0:data prefer, 1:call prefer
                int transferType = Settings.System.getInt(phone.getContext().getContentResolver(),
                                                                                Settings.System.GPRS_TRANSFER_SETTING,
                                                                                Settings.System.GPRS_TRANSFER_SETTING_DEFAULT);
                cm.setGprsTransferType(transferType, null);
                log("transferType:" + transferType);

                // In non-Gemini project, always set GPRS connection type to ALWAYS
                testMode = SystemProperties.getInt("gsm.gcf.testmode", 0);

                //Check UE is set to test mode or not
                log("testMode:" + testMode);
                Context context = phone.getContext();
                if (testMode == 0) {
                    if (mAutoGprsAttach == 1) {
                        attachType = SystemProperties.getInt("persist.radio.gprs.attach.type", 1);
                        log("attachType:" + attachType);
                        ////if(attachType == 1){
                            /* ALPS00300484 : Remove set gprs connection type here. it's too late */
                            ////  setGprsConnType(1);
                        ////}
                    } else if (mAutoGprsAttach == 2) {
                        if (FeatureOption.MTK_GEMINI_SUPPORT) {
                            //Disable for Gemini Enhancment by MTK03594
                            if(!FeatureOption.MTK_GEMINI_ENHANCEMENT){
                                Intent intent = new Intent(Intents.ACTION_GPRS_CONNECTION_TYPE_SELECT);
                                intent.putExtra(PhoneConstants.GEMINI_SIM_ID_KEY, mSimId);
                                context.sendStickyBroadcast(intent);
                                log("Broadcast: ACTION_GPRS_CONNECTION_TYPE_SELECT");
                            }
                            mAutoGprsAttach = 0;
                        }
                    }

                    if (FeatureOption.MTK_GEMINI_SUPPORT) {
                        ConnectivityManager connectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
                        if (connectivityManager != null && connectivityManager.getMobileDataEnabledGemini(mSimId)) {
                            if (SystemProperties.getInt(GsmDataConnectionTracker.PROPERTY_RIL_TEST_SIM[mSimId], 0) == 1) {
                                log("SIM" + (mSimId+1) + " is a test SIM and data is enabled on it, do PS attach");
                                setGprsConnType(1);
                            } else {
                                log("SIM" + (mSimId+1) + " is not a test SIM");
                            }
                        }
                    }
                }
                pollState();
                // Signal strength polling stops when radio is off
                queueNextSignalStrengthPoll();

                //phone.getSimRecoveryOn(obtainMessage(EVENT_GET_SIM_RECOVERY_ON));
                break;

            case EVENT_RADIO_STATE_CHANGED:
                // This will do nothing in the radio not
                // available case
                setPowerStateToDesired();
                pollState();
                break;

            case EVENT_NETWORK_STATE_CHANGED:
                //ALPS00283717
                ar = (AsyncResult) msg.obj;
                onNetworkStateChangeResult(ar);
                pollState();
                break;

            case EVENT_PS_NETWORK_STATE_CHANGED:
                mIs3GTo2G = false;
                pollState();
                break;

            case EVENT_GET_SIGNAL_STRENGTH:
                // This callback is called when signal strength is polled
                // all by itself

                if (!(cm.getRadioState().isOn())) {
                    // Polling will continue when radio turns back on
                    return;
                }
                ar = (AsyncResult) msg.obj;

                //MTK-START [ALPS415367]For MR1 Migration
                ar = onGsmSignalStrengthResult(ar);
                //MTK-END [ALPS415367]For MR1 Migration

                onSignalStrengthResult(ar, true);
                queueNextSignalStrengthPoll();

                break;

            case EVENT_GET_LOC_DONE:
                ar = (AsyncResult) msg.obj;

                if (ar.exception == null) {
                    String states[] = (String[])ar.result;
                    int lac = -1;
                    int cid = -1;
                    if (states.length >= 3) {
                        try {
                            if (states[1] != null && states[1].length() > 0) {
                                lac = Integer.parseInt(states[1], 16);
                            }
                            if (states[2] != null && states[2].length() > 0) {
                                cid = Integer.parseInt(states[2], 16);
                            }
                        } catch (NumberFormatException ex) {
                            Log.w(LOG_TAG, "error parsing location: " + ex);
                        }
                    }
                    cellLoc.setLacAndCid(lac, cid);
                    phone.notifyLocationChanged();
                }

                // Release any temporary cell lock, which could have been
                // acquired to allow a single-shot location update.
                disableSingleLocationUpdate();
                break;

            case EVENT_UPDATE_SELECTION_MODE:
                ar = (AsyncResult) msg.obj;
                if (ar.exception == null) {
                    ints = (int[])ar.result;
                    if(ints[0] == 1)
                    {
                        /* ALPS00316998 */
                        log("Start manual selection mode reminder service");
                        Intent sIntent = new Intent();
                        sIntent.setClassName("com.android.phone","com.mediatek.settings.NoNetworkPopUpService");
                        phone.getContext().startService(sIntent);
                    }
                }
                break;

            case EVENT_POLL_STATE_REGISTRATION:
            case EVENT_POLL_STATE_GPRS:
            case EVENT_POLL_STATE_OPERATOR:
            case EVENT_POLL_STATE_NETWORK_SELECTION_MODE:
                ar = (AsyncResult) msg.obj;

                handlePollStateResult(msg.what, ar);
                break;

            case EVENT_POLL_SIGNAL_STRENGTH:
                // Just poll signal strength...not part of pollState()

                cm.getSignalStrength(obtainMessage(EVENT_GET_SIGNAL_STRENGTH));
                break;

            case EVENT_NITZ_TIME:
                ar = (AsyncResult) msg.obj;

                String nitzString = (String)((Object[])ar.result)[0];
                long nitzReceiveTime = ((Long)((Object[])ar.result)[1]).longValue();

                setTimeFromNITZString(nitzString, nitzReceiveTime);
                break;

            case EVENT_SIGNAL_STRENGTH_UPDATE:
                // This is a notification from
                // CommandsInterface.setOnSignalStrengthUpdate

                ar = (AsyncResult) msg.obj;
                ar = onGsmSignalStrengthResult(ar);
                onSignalStrengthResult(ar, true);

                // [ALPS00127981]
                // If rssi=99, poll again
                if (ar.result == null) {
                    if (dontPollSignalStrength == true) {
                        dontPollSignalStrength = false;
                        queueNextSignalStrengthPoll();
                    }
                } else {
                dontPollSignalStrength = true;
                }
                break;

            case EVENT_SIM_RECORDS_LOADED:
                // MVNO-API
                if(FeatureOption.MTK_MVNO_SUPPORT) {
                    log("MTK_MVNO_SUPPORT refreshSpnDisplay()");
                    // pollState() result may be faster than load EF complete, so update ss.alphaLongShortName
                    refreshSpnDisplay();
                } else
                    updateSpnDisplay();

                String newImsi = phone.getSubscriberId();
                boolean bImsiChanged = false;
                String imsiSetting = "gsm.sim.imsi";

                if (mSimId == PhoneConstants.GEMINI_SIM_2) { 
                    imsiSetting = "gsm.sim.imsi.2";
                }else if (mSimId == PhoneConstants.GEMINI_SIM_3) { 
                    imsiSetting = "gsm.sim.imsi.3";                    
                }else if (mSimId == PhoneConstants.GEMINI_SIM_4) { 
                    imsiSetting = "gsm.sim.imsi.4";                    
                    }

                String oldImsi = Settings.System.getString(phone.getContext().getContentResolver(), imsiSetting);
                if(oldImsi == null || !oldImsi.equals(newImsi)) {	  
                    Log.d(LOG_TAG, "GSST: Sim"+ (mSimId+1) + " Card changed  lastImsi is " + oldImsi + " newImsi is " + newImsi); 
                    bImsiChanged = true;
                    Settings.System.putString(phone.getContext().getContentResolver(), imsiSetting, newImsi);
                }

		        // if(bImsiChanged && (ss.getState() != ServiceState.STATE_IN_SERVICE) 	&& ss.getIsManualSelection()) {
                if(bImsiChanged && ss.getIsManualSelection()) {
                    Log.d(LOG_TAG, "GSST: service state is out of service with manual network selection mode,  setNetworkSelectionModeAutomatic " );
                    phone.setNetworkSelectionModeAutomatic(obtainMessage(EVENT_SET_AUTO_SELECT_NETWORK_DONE));
                }
                break;

            case EVENT_LOCATION_UPDATES_ENABLED:
                ar = (AsyncResult) msg.obj;

                if (ar.exception == null) {
                    cm.getVoiceRegistrationState(obtainMessage(EVENT_GET_LOC_DONE, null));
                }
                break;

            case EVENT_SET_PREFERRED_NETWORK_TYPE:
                ar = (AsyncResult) msg.obj;
                // Don't care the result, only use for dereg network (COPS=2)
                message = obtainMessage(EVENT_RESET_PREFERRED_NETWORK_TYPE, ar.userObj);
                cm.setPreferredNetworkType(mPreferredNetworkType, message);
                break;

            case EVENT_RESET_PREFERRED_NETWORK_TYPE:
                ar = (AsyncResult) msg.obj;
                if (ar.userObj != null) {
                    AsyncResult.forMessage(((Message) ar.userObj)).exception
                            = ar.exception;
                    ((Message) ar.userObj).sendToTarget();
                }
                break;

            case EVENT_GET_PREFERRED_NETWORK_TYPE:
                ar = (AsyncResult) msg.obj;

                if (ar.exception == null) {
                    mPreferredNetworkType = ((int[])ar.result)[0];
                } else {
                    mPreferredNetworkType = RILConstants.NETWORK_MODE_GLOBAL;
                }

                message = obtainMessage(EVENT_SET_PREFERRED_NETWORK_TYPE, ar.userObj);
                int toggledNetworkType = RILConstants.NETWORK_MODE_GLOBAL;

                cm.setPreferredNetworkType(toggledNetworkType, message);
                break;

            case EVENT_CHECK_REPORT_GPRS:
                if (ss != null && !isGprsConsistent(gprsState, ss.getState())) {

                    // Can't register data service while voice service is ok
                    // i.e. CREG is ok while CGREG is not
                    // possible a network or baseband side error
                    GsmCellLocation loc = ((GsmCellLocation)phone.getCellLocation());
                    EventLog.writeEvent(EventLogTags.DATA_NETWORK_REGISTRATION_FAIL,
                            ss.getOperatorNumeric(), loc != null ? loc.getCid() : -1);
                    mReportedGprsNoReg = true;
                }
                mStartedGprsRegCheck = false;
                break;

            case EVENT_RESTRICTED_STATE_CHANGED:
                // This is a notification from
                // CommandsInterface.setOnRestrictedStateChanged

                if (DBG) log("EVENT_RESTRICTED_STATE_CHANGED");

                ar = (AsyncResult) msg.obj;

                onRestrictedStateChanged(ar);
                break;
            case EVENT_SET_AUTO_SELECT_NETWORK_DONE:
                log("GSST EVENT_SET_AUTO_SELECT_NETWORK_DONE");
                break;
            case EVENT_SET_GPRS_CONN_TYPE_DONE:
                Log.d(LOG_TAG, "GSST EVENT_SET_GPRS_CONN_TYPE_DONE");
                ar = (AsyncResult) msg.obj;
                if(ar.exception != null){
                   sendMessageDelayed(obtainMessage(EVENT_SET_GPRS_CONN_RETRY, null), DEFAULT_GPRS_RETRY_PERIOD_MILLIS);
                }
                break;
            case EVENT_SET_GPRS_CONN_RETRY:
                Log.d(LOG_TAG, "EVENT_SET_GPRS_CONN_RETRY");
                ServiceState ss = phone.getServiceState();
                if (ss == null) break;
                Log.d(LOG_TAG, "GSST EVENT_SET_GPRS_CONN_RETRY ServiceState " + ss.getState());
                if (ss.getState() == ServiceState.STATE_POWER_OFF) {
                    break;
                }
                int airplanMode = Settings.Global.getInt(phone.getContext().getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0);
                Log.d(LOG_TAG, "GSST EVENT_SET_GPRS_CONN_RETRY airplanMode " + airplanMode);
                if (airplanMode > 0) {
                    break;
                }
                setGprsConnType(gprsConnType);
                break;
            case EVENT_DATA_CONNECTION_DETACHED:
                Log.d(LOG_TAG, "EVENT_DATA_CONNECTION_DETACHED: set gprsState=STATE_OUT_OF_SERVICE");
                gprsState = ServiceState.STATE_OUT_OF_SERVICE;
                ps_networkType = DATA_ACCESS_UNKNOWN;
				
                if (mSimId == PhoneConstants.GEMINI_SIM_1) {
                    phone.setSystemProperty(TelephonyProperties.PROPERTY_DATA_NETWORK_TYPE, networkTypeToString(ps_networkType));
                } else if (mSimId == PhoneConstants.GEMINI_SIM_2){
                    phone.setSystemProperty(TelephonyProperties.PROPERTY_DATA_NETWORK_TYPE_2, networkTypeToString(ps_networkType));
                } else if (mSimId == PhoneConstants.GEMINI_SIM_3){
                    phone.setSystemProperty(TelephonyProperties.PROPERTY_DATA_NETWORK_TYPE_3, networkTypeToString(ps_networkType));
                } else if (mSimId == PhoneConstants.GEMINI_SIM_4){
                    phone.setSystemProperty(TelephonyProperties.PROPERTY_DATA_NETWORK_TYPE_4, networkTypeToString(ps_networkType));
                }
                mDetachedRegistrants.notifyRegistrants();
                break;
            case EVENT_INVALID_SIM_INFO: //ALPS00248788
                if (DBG) log("EVENT_INVALID_SIM_INFO");
                ar = (AsyncResult) msg.obj;
                onInvalidSimInfoReceived(ar);
                break;
            case EVENT_IMEI_LOCK: //ALPS00296298
                if (DBG) log("EVENT_IMEI_LOCK");
                mIsImeiLock = true;
                break;
            case EVENT_ENABLE_EMMRRS_STATUS:
                ar= (AsyncResult) msg.obj;
                log("EVENT_ENABLE_EMMRRS_STATUS start");
                if (ar.exception == null) {
                    String data[] = (String [])ar.result;
                    log("EVENT_ENABLE_EMMRRS_STATUS, data[0] is : " + data[0]);
                    log("EVENT_ENABLE_EMMRRS_STATUS, einfo value is : " + data[0].substring(8));
                    int oldValue = Integer.valueOf(data[0].substring(8));
                    int value = oldValue | 0x80;
                    log("EVENT_ENABLE_EMMRRS_STATUS, einfo value change is : " + value);
                    if (oldValue != value) {
                        setEINFO(value, null);
                    }
                }
                log("EVENT_ENABLE_EMMRRS_STATUS end");
                break;
            case EVENT_DISABLE_EMMRRS_STATUS:
                ar= (AsyncResult) msg.obj;
                log("EVENT_DISABLE_EMMRRS_STATUS start");
                if (ar.exception == null) {
                    String data[] = (String [])ar.result;
                    log("EVENT_DISABLE_EMMRRS_STATUS, data[0] is : " + data[0]);
                    log("EVENT_DISABLE_EMMRRS_STATUS, einfo value is : " + data[0].substring(8));
                    int oldValue = Integer.valueOf(data[0].substring(8));
                    int value = oldValue & 0x7f;
                    log("EVENT_DISABLE_EMMRRS_STATUS, einfo value change is : " + value);
                    if (oldValue != value) {
                        setEINFO(value, null);
                    }
                }
                log("EVENT_DISABLE_EMMRRS_STATUS end");
                break;
            default:
                super.handleMessage(msg);
            break;
        }
    }

    protected void setPowerStateToDesired() {
        log("setPowerStateToDesired mDesiredPowerState:" + mDesiredPowerState +
                " current radio state:" + cm.getRadioState() +
    			" mFirstRadioChange:" + mFirstRadioChange);
        // If we want it on and it's off, turn it on
        if (mDesiredPowerState
            && cm.getRadioState() == CommandsInterface.RadioState.RADIO_OFF) {
            if (!FeatureOption.MTK_GEMINI_SUPPORT) {
                /* ALPS00439250 START : For single SIM project ,AP might NOT set the DUAL_SIM_MODE_SETTING
                                 However, for consistent UI, we will check the setting. So make sure to set it in framework */
                Settings.System.putInt(phone.getContext().getContentResolver(), 
                                       Settings.System.DUAL_SIM_MODE_SETTING, 
                                       GeminiNetworkSubUtil.MODE_SIM1_ONLY);
                /* ALPS00439250 END */
				
                setGprsConnType(2);
                cm.setRadioPower(true, null);
                /* ALPS00316998 */
                log("check manual selection mode when setPowerStateToDesired and set dual_sim_mode_setting to 1");
                cm.getNetworkSelectionMode(obtainMessage(EVENT_UPDATE_SELECTION_MODE));
            }
        } else if (!mDesiredPowerState && cm.getRadioState().isOn()) {
            /* ALPS00439250 START : For single SIM project ,AP might NOT set the DUAL_SIM_MODE_SETTING
                          However, for consistent UI, we will check the setting. So make sure to set it in framework */
            if (!FeatureOption.MTK_GEMINI_SUPPORT) {
                int airplanMode = Settings.Global.getInt(phone.getContext().getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0);
                if(airplanMode == 0){				
                    Settings.System.putInt(phone.getContext().getContentResolver(), 
                                           Settings.System.DUAL_SIM_MODE_SETTING, 
                                           GeminiNetworkSubUtil.MODE_FLIGHT_MODE);
                    log("Set dual_sim_mode_setting to 0");				
                }        
            }        
            /* ALPS00439250 END */			
			
            // If it's on and available and we want it off gracefully
            DataConnectionTracker dcTracker = phone.mDataConnectionTracker;
            powerOffRadioSafely(dcTracker);
        } else if (!mDesiredPowerState && !cm.getRadioState().isOn() && mFirstRadioChange) { //mtk added
        	// For boot up in Airplane mode, we would like to startup modem in cfun_state=4
            if (!FeatureOption.MTK_GEMINI_SUPPORT) {
                cm.setRadioPower(false, null);
            }
        }// Otherwise, we're in the desired state

        if (mFirstRadioChange) {
            if (cm.getRadioState() == CommandsInterface.RadioState.RADIO_UNAVAILABLE) {
                log("First radio changed but radio unavailable, not to set first radio change off");
            } else {
                log("First radio changed and radio available, set first radio change off");
                mFirstRadioChange = false;
            }
        }

        // M: remove set GPRS connection type retry when radio off.
        if (!mDesiredPowerState) {
            removeGprsConnTypeRetry();
        }
    }

    @Override
    protected void hangupAndPowerOff() {
        // hang up all active voice calls
        if (phone.isInCall()) {
            log("Hangup call ...");
            phone.mCT.ringingCall.hangupIfAlive();
            phone.mCT.backgroundCall.hangupIfAlive();
            phone.mCT.foregroundCall.hangupIfAlive();
        }

        if (!FeatureOption.MTK_GEMINI_SUPPORT) {
            cm.setRadioPower(false, null);
        }
    }

    /**
     * Handle the result of one of the pollState()-related requests
     */
        protected void handlePollStateResult (int what, AsyncResult ar) {
        int ints[];
        String states[];

        // Ignore stale requests from last poll
        if (ar.userObj != pollingContext) return;

        if (ar.exception != null) {
            CommandException.Error err=null;

            if (ar.exception instanceof CommandException) {
                err = ((CommandException)(ar.exception)).getCommandError();
            }

            if (err == CommandException.Error.RADIO_NOT_AVAILABLE) {
                // Radio has crashed or turned off
                cancelPollState();
                return;
            }

            if (!cm.getRadioState().isOn()) {
                // Radio has crashed or turned off
                cancelPollState();
                return;
            }

            if (err != CommandException.Error.OP_NOT_ALLOWED_BEFORE_REG_NW &&
                    err != CommandException.Error.OP_NOT_ALLOWED_BEFORE_REG_NW) {
                log("RIL implementation has returned an error where it must succeed" + ar.exception);
            }
        } else try {
            switch (what) {
                case EVENT_POLL_STATE_REGISTRATION:
                    states = (String[])ar.result;
                    int lac = -1;
                    int cid = -1;
                    int regState = -1;
                    int reasonRegStateDenied = -1;
                    int psc = -1;
                    if (states.length > 0) {
                        try {
                            regState = Integer.parseInt(states[0]);
                            if (states.length >= 3) {
                                if (states[1] != null && states[1].length() > 0) {
                                    lac = Integer.parseInt(states[1], 16);
                                }
                                if (states[2] != null && states[2].length() > 0) {
                                    cid = Integer.parseInt(states[2], 16);
                                }
                                if (states[3] != null && states[3].length() > 0) {
                                    mNewRilRadioTechnology = Integer.parseInt(states[3]);
                                    newSS.setRadioTechnology(mNewRilRadioTechnology);
                                }
                            }
                            log("EVENT_POLL_STATE_REGISTRATION cs_networkTyp:" + mRilRadioTechnology +
                                    ",regState:" + regState +
                                    ",mNewRadioTechnology:" + mNewRilRadioTechnology +
                                    ",lac:" + lac +
                                    ",cid:" + cid);
                        } catch (NumberFormatException ex) {
                            loge("error parsing RegistrationState: " + ex);
                        }
                    }

                    mGsmRoaming = regCodeIsRoaming(regState);
                    newSS.setState (regCodeToServiceState(regState));
                    newSS.setRegState(regState);
                    // [ALPS00225065] For Gemini special handle,
                    // When SIM blocked, treat as out of service
                    if (FeatureOption.MTK_GEMINI_SUPPORT) {
                        if (cm.getRadioState() == CommandsInterface.RadioState.SIM_LOCKED_OR_ABSENT) {
                            newSS.setState(ServiceState.STATE_OUT_OF_SERVICE);
                        }
                    }
//ALPS00283717: distiguish limited service and no service
/*
                    if (regState == 10 || regState == 12 || regState == 13 || regState == 14) {
                        mEmergencyOnly = true;
                    } else {
                        mEmergencyOnly = false;
                    }
*/
                    // LAC and CID are -1 if not avail. LAC and CID are -1 in OUT_SERVICE
                    if (states.length > 3 || (regState != 1 && regState != 5)) {
                    	log("states.length > 3");

                        /* ALPS00291583: ignore unknown lac or cid value */
                        if(lac==0xfffe || cid==0x0fffffff)
                        {
                            log("unknown lac:"+lac+"or cid:"+cid);
                        }
                        else
                        {
                            newCellLoc.setLacAndCid(lac, cid);
                        }
                    	//if (mSimId == PhoneConstants.GEMINI_SIM_1) {
                    	//	SystemProperties.set(TelephonyProperties.PROPERTY_CS_NETWORK_TYPE, Integer.toString(mNewRadioTechnology));
                    	//	log("PROPERTY_CS_NETWORK_TYPE" + SystemProperties.get(TelephonyProperties.PROPERTY_CS_NETWORK_TYPE));
                    	//} else {
                     //	SystemProperties.set(TelephonyProperties.PROPERTY_CS_NETWORK_TYPE_2, Integer.toString(mNewRadioTechnology));
                    	//	log("PROPERTY_CS_NETWORK_TYPE_2" + SystemProperties.get(TelephonyProperties.PROPERTY_CS_NETWORK_TYPE_2));
                    	//}
                    }
                    newCellLoc.setPsc(psc);
                break;

                case EVENT_POLL_STATE_GPRS:
                    states = (String[])ar.result;

                    regState = -1;
                    mNewReasonDataDenied = -1;
                    mNewMaxDataCalls = 1;
                    if (states.length > 0) {
                        try {
                            regState = Integer.parseInt(states[0]);

                            // states[3] (if present) is the current radio technology
                            if (states.length >= 4 && states[3] != null) {
                                newps_networkType = Integer.parseInt(states[3]);
                            }
                            if (states.length >= 5 && states[4] != null) {
                                log("<cell_data_speed_support> " + states[4]);
                            }
                            if (states.length >= 6 && states[5] != null) {
                                log("<max_data_bearer_capability> " + states[5]);
                            }
                            if ((states.length >= 7 ) && (regState == 3)) {
                                mNewReasonDataDenied = Integer.parseInt(states[6]);
                            }
                            if (states.length >= 8) {
                                mNewMaxDataCalls = Integer.parseInt(states[7]);
                            }
                        } catch (NumberFormatException ex) {
                            loge("error parsing GprsRegistrationState: " + ex);
                        }
                    }
                    newGPRSState = regCodeToServiceState(regState);
                    mDataRoaming = regCodeIsRoaming(regState);
                    mNewRilRadioTechnology = newps_networkType;
                    newSS.setRadioTechnology(newps_networkType);
                break;

                case EVENT_POLL_STATE_OPERATOR:
                    String opNames[] = (String[])ar.result;

                    if (opNames != null && opNames.length >= 3) {
                        log("long:" +opNames[0] + " short:" + opNames[1] + " numeric:" + opNames[2]);
                        newSS.setOperatorName (opNames[0], opNames[1], opNames[2]);
                    }
                break;

                case EVENT_POLL_STATE_NETWORK_SELECTION_MODE:
                    ints = (int[])ar.result;
                    newSS.setIsManualSelection(ints[0] == 1);
                    if((ss.getIsManualSelection() == true) && (newSS.getIsManualSelection() == false)){
                        log("Selection mode change from manual to auto");

                        if (FeatureOption.MTK_GEMINI_SUPPORT) {
                            if (phone instanceof GSMPhone){
                                boolean allPhoneInAutoMode = true;
                                for(int simIdx=PhoneConstants.GEMINI_SIM_1;simIdx<PhoneConstants.GEMINI_SIM_NUM;simIdx++){                     
                                    GSMPhone peerPhone = ((GSMPhone)phone).getPeerPhones(simIdx);
                                    if (peerPhone != null){
                                        if(peerPhone.getServiceState().getIsManualSelection()== true){
                                            log("Phone"+ (simIdx+1)+" is NOT in manual selection mode,shell keep reminder service");										   
                                            allPhoneInAutoMode = false;											   
                                            break;                                       
                                        }
                                    }
                                }		
                                if(allPhoneInAutoMode == true){
                                    log("All sim are NOT in manual selection mode,stop reminder service");
                                    Intent sIntent = new Intent();     
                                    sIntent.setClassName("com.android.phone","com.mediatek.settings.NoNetworkPopUpService"); 
                                    phone.getContext().stopService(sIntent);		
                                }                                
                            }
                        }
                        else{
                            log("Stop manual selection mode reminder service");
                            Intent sIntent = new Intent();
                            sIntent.setClassName("com.android.phone","com.mediatek.settings.NoNetworkPopUpService");
                            phone.getContext().stopService(sIntent);
                        }
                    }
                    else if((ss.getIsManualSelection() == false) && (newSS.getIsManualSelection() == true)){
                        log("Selection mode change from auto to manual");
                        Intent sIntent = new Intent();
                        sIntent.setClassName("com.android.phone","com.mediatek.settings.NoNetworkPopUpService");
                        phone.getContext().startService(sIntent);
                    }
                break;
            }

        } catch (RuntimeException ex) {
            Log.e(LOG_TAG, "Exception while polling service state. "
                            + "Probably malformed RIL response.", ex);
        }

        pollingContext[0]--;

        if (pollingContext[0] == 0) {
            /**
             * [ALPS00006527]
             * Only when CS in service, treat PS as in service
             */
            if (newSS.getState() != ServiceState.STATE_IN_SERVICE) {
                newGPRSState = regCodeToServiceState(0);
                mDataRoaming = regCodeIsRoaming(0);
            }

            /**
             *  Since the roaming states of gsm service (from +CREG) and
             *  data service (from +CGREG) could be different, the new SS
             *  is set roaming while either one is roaming.
             *
             *  There is an exception for the above rule. The new SS is not set
             *  as roaming while gsm service reports roaming but indeed it is
             *  not roaming between operators.
             */
            //BEGIN mtk03923[20120206][ALPS00117799][ALPS00230295]
            //Only check roaming indication from CREG (CS domain)
            //boolean roaming = (mGsmRoaming || mDataRoaming);
            boolean roaming = mGsmRoaming;
            //END   mtk03923[20120206][ALPS00117799][ALPS00230295]
            // [ALPS00220720] remove this particular check.
            // Still display roaming even in the same operator
            /*
            if (mGsmRoaming && !isRoamingBetweenOperators(mGsmRoaming, newSS)) {
                roaming = false;
            }
            */
            newSS.setRoaming(roaming);
            newSS.setEmergencyOnly(mEmergencyOnly);
            pollStateDone();
        }
    }

    private void setSignalStrengthDefaultValues() {
        mSignalStrength = new SignalStrength(mSimId, 99, -1, -1, -1, -1, -1, -1, 99,
                                             SignalStrength.INVALID,
                                             SignalStrength.INVALID,
                                             SignalStrength.INVALID,
                                             SignalStrength.INVALID,
                                             true, 0, 0, 0);
    }

    /**
     * A complete "service state" from our perspective is
     * composed of a handful of separate requests to the radio.
     *
     * We make all of these requests at once, but then abandon them
     * and start over again if the radio notifies us that some
     * event has changed
     */
    private void pollState() {
        pollingContext = new int[1];
        pollingContext[0] = 0;
        log("cm.getRadioState() is " + cm.getRadioState());

        //ALPS00267573
        if(dontUpdateNetworkStateFlag == true)
        {
            log("pollState is ignored!!");
            return;
        }		
        CommandsInterface.RadioState radioState = cm.getRadioState();
        if (radioState == CommandsInterface.RadioState.SIM_LOCKED_OR_ABSENT) {
            //Since when there is no SIM inserted, the radio state is set to locked or absent
            //In this case, the service state will be incorrect if the radio is turned off
            //So we use airplane mode and dualSimMode to deside if the radio state is radio off
            int airplaneMode = Settings.Global.getInt(
                    phone.getContext().getContentResolver(),
                    Settings.Global.AIRPLANE_MODE_ON, 0);
            int dualSimModeSetting = Settings.System.getInt(
                    phone.getContext().getContentResolver(),
                    Settings.System.DUAL_SIM_MODE_SETTING, GeminiNetworkSubUtil.MODE_DUAL_SIM);

            /* ALPS00439915: to prevent shutdown thread can't get RADIO_OFF state */
            int isPowerOff = 0;
            if(!PhoneFactory.isDualTalkMode()){
                /* ALPS00438909 BSP package don't support IPO */
                isPowerOff = SystemProperties.getInt("ril.ipo.radiooff", 0);
            }else{
                /* ALPS00462176 */            
                if(mSimId == PhoneConstants.GEMINI_SIM_2){            
                    isPowerOff = SystemProperties.getInt("ril.ipo.radiooff.2", 0);
                    log("Dualtalk SIM2 isPowerOff="+isPowerOff);
                }else{
                    isPowerOff = SystemProperties.getInt("ril.ipo.radiooff", 0);
                }				
            }		 		

            log("Now airplaneMode="+airplaneMode+",dualSimModeSetting="+dualSimModeSetting+",isPowerOff="+isPowerOff);			

            if ((airplaneMode == 1) || (isPowerOff ==1)) {
                radioState = CommandsInterface.RadioState.RADIO_OFF;
            } else {
                boolean hasPeerSIMInserted = false;
                boolean hasSIMInserted = phone.isSimInsert();

                if(FeatureOption.MTK_GEMINI_SUPPORT){
                    for(int simIdx=PhoneConstants.GEMINI_SIM_1;simIdx<PhoneConstants.GEMINI_SIM_NUM;simIdx++){			
                        if(phone.getPeerPhones(simIdx)!=null){
                            if((phone.getPeerPhones(simIdx).isSimInsert()== true)){
                                hasPeerSIMInserted = true;
                                break;
                            }							
                        }							
                    }											
                }
                if (hasPeerSIMInserted || hasSIMInserted) {
                    if ((dualSimModeSetting & (GeminiNetworkSubUtil.MODE_SIM1_ONLY << phone.getMySimId())) == 0)
                        radioState = CommandsInterface.RadioState.RADIO_OFF;
                } else if (phone.getMySimId() != PhoneConstants.GEMINI_SIM_1){
                    //because when no SIM inserted, we still power on SIM1 for emergency call
                    //if this is not SIM1, we have to transfer state to radio-off
                    radioState = CommandsInterface.RadioState.RADIO_OFF;
                }
            }
            log("pollState is locked or absent, transfer to [" + radioState + "]");
        }

        switch (radioState) {
            case RADIO_UNAVAILABLE:
            case RADIO_OFF:
                newSS.setStateOff();
                newCellLoc.setStateInvalid();
                setSignalStrengthDefaultValues();
                mGotCountryCode = false;
                mNitzUpdatedTime = false;
                mIs3GTo2G = false; /* ALPS00348630 reset flag */
                mGsmRoaming = false;
                mNewReasonDataDenied = -1;
                mNewMaxDataCalls = 1;
                newps_networkType = ServiceState.RIL_RADIO_TECHNOLOGY_UNKNOWN;
                newGPRSState = ServiceState.STATE_POWER_OFF;
                mDataRoaming = false;
                mNewRilRadioTechnology = ServiceState.RIL_RADIO_TECHNOLOGY_UNKNOWN;
                //[ALPS00423362]
                mEmergencyOnly = false;

                //[ALPS00439473] MTK add - START
                dontPollSignalStrength = false;
                setLastSignalStrengthDefaultValues(true);
                //[ALPS00439473] MTK add - END
				
                pollStateDone();
                break;

            case RUIM_NOT_READY:
            case RUIM_READY:
            case RUIM_LOCKED_OR_ABSENT:
            case NV_NOT_READY:
            case NV_READY:
                if (DBG) log("Radio Technology Change ongoing, setting SS to off");
                newSS.setStateOff();
                newCellLoc.setStateInvalid();
                setSignalStrengthDefaultValues();
                mGotCountryCode = false;

                //NOTE: pollStateDone() is not needed in this case
                break;

            default:
                // Issue all poll-related commands at once
                // then count down the responses, which
                // are allowed to arrive out-of-order

                pollingContext[0]++;
                cm.getOperator(
                    obtainMessage(
                        EVENT_POLL_STATE_OPERATOR, pollingContext));

                pollingContext[0]++;
                cm.getDataRegistrationState(
                    obtainMessage(
                        EVENT_POLL_STATE_GPRS, pollingContext));

                pollingContext[0]++;
                cm.getVoiceRegistrationState(
                    obtainMessage(
                        EVENT_POLL_STATE_REGISTRATION, pollingContext));

                pollingContext[0]++;
                cm.getNetworkSelectionMode(
                    obtainMessage(
                        EVENT_POLL_STATE_NETWORK_SELECTION_MODE, pollingContext));
                break;
        }
    }

    private static String networkTypeToString(int type) {
        //Network Type from GPRS_REGISTRATION_STATE
        String ret = "unknown";

        switch (type) {
            case DATA_ACCESS_GPRS:
                ret = "GPRS";
                break;
            case DATA_ACCESS_EDGE:
                ret = "EDGE";
                break;
            case DATA_ACCESS_UMTS:
                ret = "UMTS";
                break;
            case DATA_ACCESS_HSDPA:
                ret = "HSDPA";
                break;
            case DATA_ACCESS_HSUPA:
                ret = "HSUPA";
                break;
            case DATA_ACCESS_HSPA:
                ret = "HSPA";
                break;
            default:
                break;
        }
        Log.e(LOG_TAG, "networkTypeToString: " + ret);
        return ret;
    }

    private void pollStateDone() {
        // PS & CS network type summarize -->
        // From 3G to 2G, CS NW type is ensured responding firstly. Before receiving
        // PS NW type change URC, PS NW type should always take CS NW type.
        if ((mNewRilRadioTechnology > ServiceState.RIL_RADIO_TECHNOLOGY_UNKNOWN &&
                mNewRilRadioTechnology <= ServiceState.RIL_RADIO_TECHNOLOGY_EDGE) &&
                mRilRadioTechnology >= ServiceState.RIL_RADIO_TECHNOLOGY_UMTS) {
            mIs3GTo2G = true;
            log("pollStateDone(): mIs3GTo2G = true");
        }
        if (mIs3GTo2G == true) {
            newps_networkType = mNewRilRadioTechnology;
        } else if (newps_networkType > mNewRilRadioTechnology) {
            mNewRilRadioTechnology = newps_networkType;
            newSS.setRadioTechnology(newps_networkType);
        }
        // <-- end of  PS & CS network type summarize

        if (DBG) {
            log("Poll ServiceState done: " +
                " oldSS=[" + ss + "] newSS=[" + newSS +
                "] oldGprs=" + gprsState + " newData=" + newGPRSState +
                " oldMaxDataCalls=" + mMaxDataCalls +
                " mNewMaxDataCalls=" + mNewMaxDataCalls +
                " oldReasonDataDenied=" + mReasonDataDenied +
                " mNewReasonDataDenied=" + mNewReasonDataDenied +
                " oldType=" + ServiceState.rilRadioTechnologyToString(mRilRadioTechnology) +
                " newType=" + ServiceState.rilRadioTechnologyToString(mNewRilRadioTechnology) +
                " oldGprsType=" + ps_networkType +
                " newGprsType=" + newps_networkType);
        }

        boolean hasRegistered =
            ss.getState() != ServiceState.STATE_IN_SERVICE
            && newSS.getState() == ServiceState.STATE_IN_SERVICE;

        boolean hasDeregistered =
            ss.getState() == ServiceState.STATE_IN_SERVICE
            && newSS.getState() != ServiceState.STATE_IN_SERVICE;

        boolean hasGprsAttached =
                gprsState != ServiceState.STATE_IN_SERVICE
                && newGPRSState == ServiceState.STATE_IN_SERVICE;

        boolean hasPSNetworkTypeChanged = ps_networkType != newps_networkType;

        boolean hasRadioTechnologyChanged = mRilRadioTechnology != mNewRilRadioTechnology;

        boolean hasChanged = !newSS.equals(ss);

        boolean hasRoamingOn = !ss.getRoaming() && newSS.getRoaming();

        boolean hasRoamingOff = ss.getRoaming() && !newSS.getRoaming();

        boolean hasLocationChanged = !newCellLoc.equals(cellLoc);

        boolean hasRegStateChanged = ss.getRegState() != newSS.getRegState();

        boolean hasLacChanged = newCellLoc.getLac() != cellLoc.getLac();

        log("pollStateDone,hasRegistered:"+hasRegistered+",hasDeregistered:"+hasDeregistered+
        		",hasGprsAttached:"+hasGprsAttached+
        		",hasPSNetworkTypeChanged:"+hasPSNetworkTypeChanged+",hasRadioTechnologyChanged:"+hasRadioTechnologyChanged+
        		",hasChanged:"+hasChanged+",hasRoamingOn:"+hasRoamingOn+",hasRoamingOff:"+hasRoamingOff+
        		",hasLocationChanged:"+hasLocationChanged+",hasRegStateChanged:"+hasRegStateChanged+",hasLacChanged:"+hasLacChanged);
        // Add an event log when connection state changes
        if (ss.getState() != newSS.getState() || gprsState != newGPRSState) {
            EventLog.writeEvent(EventLogTags.GSM_SERVICE_STATE_CHANGE,
                ss.getState(), gprsState, newSS.getState(), newGPRSState);
        }

        mServiceStateExt.onPollStateDone(ss, newSS, gprsState, newGPRSState);

        ServiceState tss;
        tss = ss;
        ss = newSS;
        newSS = tss;
        // clean slate for next time
        // newSS.setStateOutOfService();

        // ALPS00277176
        GsmCellLocation tcl = cellLoc;
        cellLoc = newCellLoc;
        newCellLoc = tcl;

        gprsState = newGPRSState;

        ps_networkType = newps_networkType;

        if (hasPSNetworkTypeChanged) {
            if (mSimId == PhoneConstants.GEMINI_SIM_1) {
                phone.setSystemProperty(TelephonyProperties.PROPERTY_DATA_NETWORK_TYPE, networkTypeToString(ps_networkType));
            } else if (mSimId == PhoneConstants.GEMINI_SIM_2){
                phone.setSystemProperty(TelephonyProperties.PROPERTY_DATA_NETWORK_TYPE_2, networkTypeToString(ps_networkType));
            } else if (mSimId == PhoneConstants.GEMINI_SIM_3){
                phone.setSystemProperty(TelephonyProperties.PROPERTY_DATA_NETWORK_TYPE_3, networkTypeToString(ps_networkType));
            } else if (mSimId == PhoneConstants.GEMINI_SIM_4){
                phone.setSystemProperty(TelephonyProperties.PROPERTY_DATA_NETWORK_TYPE_4, networkTypeToString(ps_networkType));
            }
            ratPsChangedRegistrants.notifyRegistrants(new AsyncResult(null, ps_networkType, null));
        }

        // Add an event log when network type switched
        // TODO: we may add filtering to reduce the event logged,
        // i.e. check preferred network setting, only switch to 2G, etc
        if (hasRadioTechnologyChanged) {
            int cid = -1;
            GsmCellLocation loc = ((GsmCellLocation)phone.getCellLocation());
            if (loc != null) cid = loc.getCid();
            EventLog.writeEvent(EventLogTags.GSM_RAT_SWITCHED, cid, mRilRadioTechnology,
                    mNewRilRadioTechnology);
            if (DBG) {
                log("RAT switched " + ServiceState.rilRadioTechnologyToString(mRilRadioTechnology) +
                        " -> " + ServiceState.rilRadioTechnologyToString(mNewRilRadioTechnology) +
                        " at cell " + cid);
            }
			
            if (mSimId == PhoneConstants.GEMINI_SIM_1) {
                SystemProperties.set(TelephonyProperties.PROPERTY_CS_NETWORK_TYPE, Integer.toString(mNewRilRadioTechnology));
            } else if (mSimId == PhoneConstants.GEMINI_SIM_2){
                SystemProperties.set(TelephonyProperties.PROPERTY_CS_NETWORK_TYPE_2, Integer.toString(mNewRilRadioTechnology));
            } else if (mSimId == PhoneConstants.GEMINI_SIM_3){
                SystemProperties.set(TelephonyProperties.PROPERTY_CS_NETWORK_TYPE_3, Integer.toString(mNewRilRadioTechnology));
            } else if (mSimId == PhoneConstants.GEMINI_SIM_4){
                SystemProperties.set(TelephonyProperties.PROPERTY_CS_NETWORK_TYPE_4, Integer.toString(mNewRilRadioTechnology));
            }
            updateSpnDisplay(false);
            ratCsChangedRegistrants.notifyRegistrants(new AsyncResult(null, mNewRilRadioTechnology, null));
        }

        gprsState = newGPRSState;
        mReasonDataDenied = mNewReasonDataDenied;
        mMaxDataCalls = mNewMaxDataCalls;
        mRilRadioTechnology = mNewRilRadioTechnology;
        // this new state has been applied - forget it until we get a new new state
        mNewRilRadioTechnology = 0;

        //newSS.setStateOutOfService(); // clean slate for next time

        if (hasRegistered) {
            mNetworkAttachedRegistrants.notifyRegistrants();
            mLastRegisteredPLMN = ss.getOperatorNumeric() ;
            log("mLastRegisteredPLMN= "+mLastRegisteredPLMN);

            if (DBG) {
                log("pollStateDone: registering current mNitzUpdatedTime=" +
                        mNitzUpdatedTime + " changing to false");
            }
            mNitzUpdatedTime = false;
        }

        if(explict_update_spn ==1)
        {
             /* ALPS00273961 :Screen on, modem explictly send CREG URC , but still not able to update screen due to hasChanged is false
                           In this case , we update SPN display by explict_update_spn */
             if(!hasChanged)
             {
                 log("explict_update_spn trigger to refresh SPN");
                 updateSpnDisplay(true);
             }
             explict_update_spn = 0;
        }

        if (hasChanged) {
            updateSpnDisplay();
            String operatorNumeric = ss.getOperatorNumeric();
            String prevOperatorNumeric;

            if (PhoneConstants.GEMINI_SIM_1 == mSimId) {
                prevOperatorNumeric = SystemProperties.get(TelephonyProperties.PROPERTY_OPERATOR_NUMERIC, "");
                phone.setSystemProperty(TelephonyProperties.PROPERTY_OPERATOR_ALPHA, ss.getOperatorAlphaLong());
                phone.setSystemProperty(TelephonyProperties.PROPERTY_OPERATOR_NUMERIC, ss.getOperatorNumeric());
                phone.setSystemProperty(TelephonyProperties.PROPERTY_OPERATOR_ISROAMING, ss.getRoaming() ? "true" : "false");
            } else if (PhoneConstants.GEMINI_SIM_2 == mSimId){
                prevOperatorNumeric = SystemProperties.get(TelephonyProperties.PROPERTY_OPERATOR_NUMERIC_2, "");
                phone.setSystemProperty(TelephonyProperties.PROPERTY_OPERATOR_ALPHA_2, ss.getOperatorAlphaLong());
                phone.setSystemProperty(TelephonyProperties.PROPERTY_OPERATOR_NUMERIC_2, ss.getOperatorNumeric());
                phone.setSystemProperty(TelephonyProperties.PROPERTY_OPERATOR_ISROAMING_2, ss.getRoaming() ? "true" : "false");
            } else if (PhoneConstants.GEMINI_SIM_3 == mSimId){
                prevOperatorNumeric = SystemProperties.get(TelephonyProperties.PROPERTY_OPERATOR_NUMERIC_3, "");
                phone.setSystemProperty(TelephonyProperties.PROPERTY_OPERATOR_ALPHA_3, ss.getOperatorAlphaLong());                
                phone.setSystemProperty(TelephonyProperties.PROPERTY_OPERATOR_NUMERIC_3, ss.getOperatorNumeric());
                phone.setSystemProperty(TelephonyProperties.PROPERTY_OPERATOR_ISROAMING_3, ss.getRoaming() ? "true" : "false");
            } else {
                prevOperatorNumeric = SystemProperties.get(TelephonyProperties.PROPERTY_OPERATOR_NUMERIC_4, "");
                phone.setSystemProperty(TelephonyProperties.PROPERTY_OPERATOR_ALPHA_4, ss.getOperatorAlphaLong());                
                phone.setSystemProperty(TelephonyProperties.PROPERTY_OPERATOR_NUMERIC_4, ss.getOperatorNumeric());
                phone.setSystemProperty(TelephonyProperties.PROPERTY_OPERATOR_ISROAMING_4, ss.getRoaming() ? "true" : "false");
            }

            if (operatorNumeric == null) {
                if (DBG) log("operatorNumeric is null");
 
                if (PhoneConstants.GEMINI_SIM_1 == mSimId) {
                    phone.setSystemProperty(TelephonyProperties.PROPERTY_OPERATOR_ISO_COUNTRY, "");
                } else if (PhoneConstants.GEMINI_SIM_2 == mSimId){
                    phone.setSystemProperty(TelephonyProperties.PROPERTY_OPERATOR_ISO_COUNTRY_2, "");
                } else if (PhoneConstants.GEMINI_SIM_3 == mSimId){
                    phone.setSystemProperty(TelephonyProperties.PROPERTY_OPERATOR_ISO_COUNTRY_3, "");
                } else if (PhoneConstants.GEMINI_SIM_4 == mSimId){
                    phone.setSystemProperty(TelephonyProperties.PROPERTY_OPERATOR_ISO_COUNTRY_4, "");
                }
                mGotCountryCode = false;
                mNitzUpdatedTime = false;
            } else {
                String iso = "";
                String mcc = operatorNumeric.substring(0, 3);
                try{
                    iso = MccTable.countryCodeForMcc(Integer.parseInt(
                            operatorNumeric.substring(0,3)));
                } catch ( NumberFormatException ex){
                    Log.w(LOG_TAG, "countryCodeForMcc error" + ex);
                } catch ( StringIndexOutOfBoundsException ex) {
                    Log.w(LOG_TAG, "countryCodeForMcc error" + ex);
                }

                if (PhoneConstants.GEMINI_SIM_1 == mSimId) {
                	phone.setSystemProperty(TelephonyProperties.PROPERTY_OPERATOR_ISO_COUNTRY, iso);
                } else if (PhoneConstants.GEMINI_SIM_2 == mSimId){
                    phone.setSystemProperty(TelephonyProperties.PROPERTY_OPERATOR_ISO_COUNTRY_2, iso);
                } else if (PhoneConstants.GEMINI_SIM_3 == mSimId){
                    phone.setSystemProperty(TelephonyProperties.PROPERTY_OPERATOR_ISO_COUNTRY_3, iso);
                } else if (PhoneConstants.GEMINI_SIM_4 == mSimId){
                    phone.setSystemProperty(TelephonyProperties.PROPERTY_OPERATOR_ISO_COUNTRY_4, iso);
                }
                mGotCountryCode = true;

                TimeZone zone = null;

                if (!mNitzUpdatedTime && !mcc.equals("000") && !TextUtils.isEmpty(iso) &&
                        getAutoTimeZone()) {

                    // Test both paths if ignore nitz is true
                    boolean testOneUniqueOffsetPath = SystemProperties.getBoolean(
                                TelephonyProperties.PROPERTY_IGNORE_NITZ, false) &&
                                    ((SystemClock.uptimeMillis() & 1) == 0);

                    ArrayList<TimeZone> uniqueZones = TimeUtils.getTimeZonesWithUniqueOffsets(iso);
                    if ((uniqueZones.size() == 1) || testOneUniqueOffsetPath) {
                        zone = uniqueZones.get(0);
                        if (DBG) {
                           log("pollStateDone: no nitz but one TZ for iso-cc=" + iso +
                                   " with zone.getID=" + zone.getID() +
                                   " testOneUniqueOffsetPath=" + testOneUniqueOffsetPath);
                        }
                        setAndBroadcastNetworkSetTimeZone(zone.getID());
                    } else {
                        if (DBG) {
                            log("pollStateDone: there are " + uniqueZones.size() +
                                " unique offsets for iso-cc='" + iso +
                                " testOneUniqueOffsetPath=" + testOneUniqueOffsetPath +
                                "', do nothing");
                        }
                    }
                }

                if (shouldFixTimeZoneNow(phone, operatorNumeric, prevOperatorNumeric,
                        mNeedFixZoneAfterNitz)) {
                    // If the offset is (0, false) and the timezone property
                    // is set, use the timezone property rather than
                    // GMT.
                    String zoneName = SystemProperties.get(TIMEZONE_PROPERTY);
                    if (DBG) {
                        log("pollStateDone: fix time zone zoneName='" + zoneName +
                            "' mZoneOffset=" + mZoneOffset + " mZoneDst=" + mZoneDst +
                            " iso-cc='" + iso +
                            "' iso-cc-idx=" + Arrays.binarySearch(GMT_COUNTRY_CODES, iso));
                    }

                    if (iso.equals("")){
                        // Country code not found.  This is likely a test network.
                        // Get a TimeZone based only on the NITZ parameters (best guess).
                        zone = getNitzTimeZone(mZoneOffset, mZoneDst, mZoneTime);
                        if (DBG) log("pollStateDone: using NITZ TimeZone");
                    }else if ((mZoneOffset == 0) && (mZoneDst == false) &&
                        (zoneName != null) && (zoneName.length() > 0) &&
                        (Arrays.binarySearch(GMT_COUNTRY_CODES, iso) < 0)) {
                    // "(mZoneOffset == 0) && (mZoneDst == false) &&
                    //  (Arrays.binarySearch(GMT_COUNTRY_CODES, iso) < 0)"
                    // means that we received a NITZ string telling
                    // it is in GMT+0 w/ DST time zone
                    // BUT iso tells is NOT, e.g, a wrong NITZ reporting
                    // local time w/ 0 offset.
                        zone = TimeZone.getDefault();
                        if (mNeedFixZoneAfterNitz) {
                            // For wrong NITZ reporting local time w/ 0 offset,
                        // need adjust time to reflect default timezone setting
                            long ctm = System.currentTimeMillis();
                            long tzOffset = zone.getOffset(ctm);
                            if (DBG) {
                                log("pollStateDone: tzOffset=" + tzOffset + " ltod=" +
                                        TimeUtils.logTimeOfDay(ctm));
                            }
                        if (getAutoTime()) {
                                long adj = ctm - tzOffset;
                                if (DBG) log("pollStateDone: adj ltod=" +
                                        TimeUtils.logTimeOfDay(adj));
                                setAndBroadcastNetworkSetTime(adj);
                        } else {
                            // Adjust the saved NITZ time to account for tzOffset.
                            mSavedTime = mSavedTime - tzOffset;
                        }
                        }
                        if (DBG) log("pollStateDone: using default TimeZone");
                    } else {
                        zone = TimeUtils.getTimeZone(mZoneOffset, mZoneDst, mZoneTime, iso);
                        if (DBG) log("pollStateDone: using getTimeZone(off, dst, time, iso)");
                    }

                    mNeedFixZoneAfterNitz = false;

                    if (zone != null) {
                        log("pollStateDone: zone != null zone.getID=" + zone.getID());
                        if (getAutoTimeZone()) {
                            setAndBroadcastNetworkSetTimeZone(zone.getID());
                        }
                        saveNitzTimeZone(zone.getID());
                    } else {
                        log("pollStateDone: zone == null");
                    }
                }
            }

            if (hasRegStateChanged) {
            	if (ss.getRegState() == ServiceState.REGISTRATION_STATE_UNKNOWN
                && (1 == Settings.Global.getInt(phone.getContext().getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, -1))) {
            		int serviceState = phone.getServiceState().getState();
            		if (serviceState != ServiceState.STATE_POWER_OFF) {
            			ss.setStateOff();
            		}
            	}
            	phone.updateSimIndicateState();
            }
            phone.notifyServiceStateChanged(ss);

            if (hasRegistered) {
                /* ALPS00296741: to handle searching state to registered scenario,we force status bar to refresh signal icon */
                log("force update signal strength after notifyServiceStateChanged");
                phone.notifySignalStrength();
            }
        }
        else
        {
            if((hasLacChanged == true) &&(ss.getState() == ServiceState.STATE_IN_SERVICE))
            {
                /* EONS display might be changed due to LAC changed */
                log("force updateSpnDisplay due to LAC changed");
                updateSpnDisplay();
            }
        }

        if (hasGprsAttached) {
            mAttachedRegistrants.notifyRegistrants();
            mLastPSRegisteredPLMN = ss.getOperatorNumeric() ;
            log("mLastPSRegisteredPLMN= "+mLastPSRegisteredPLMN);
        }

        if (hasRadioTechnologyChanged || hasPSNetworkTypeChanged) {
            phone.notifyDataConnection(Phone.REASON_NW_TYPE_CHANGED);
        }

        if (hasLocationChanged) {
        	phone.notifyLocationChanged();
        }

        if (hasRoamingOn) {
            Settings.System.putInt(phone.getContext().getContentResolver(),
                                            Settings.System.ROAMING_INDICATION_NEEDED,
                                            1);

            if (mSimId == PhoneConstants.GEMINI_SIM_1) {
                SystemProperties.set(TelephonyProperties.PROPERTY_ROAMING_INDICATOR_NEEDED, "true");
            } else if (mSimId == PhoneConstants.GEMINI_SIM_2){
                SystemProperties.set(TelephonyProperties.PROPERTY_ROAMING_INDICATOR_NEEDED_2, "true");
            } else if (mSimId == PhoneConstants.GEMINI_SIM_3){
                SystemProperties.set(TelephonyProperties.PROPERTY_ROAMING_INDICATOR_NEEDED_3, "true");
            } else if (mSimId == PhoneConstants.GEMINI_SIM_4){
                SystemProperties.set(TelephonyProperties.PROPERTY_ROAMING_INDICATOR_NEEDED_4, "true");
            }
            mRoamingOnRegistrants.notifyRegistrants();
        }

        if (hasRoamingOff) {
            Settings.System.putInt(phone.getContext().getContentResolver(),
                                            Settings.System.ROAMING_INDICATION_NEEDED,
                                            0);
			
            if (mSimId == PhoneConstants.GEMINI_SIM_1) {
                SystemProperties.set(TelephonyProperties.PROPERTY_ROAMING_INDICATOR_NEEDED, "false");
            } else if (mSimId == PhoneConstants.GEMINI_SIM_2){
                SystemProperties.set(TelephonyProperties.PROPERTY_ROAMING_INDICATOR_NEEDED_2, "false");
            } else if (mSimId == PhoneConstants.GEMINI_SIM_3){
                SystemProperties.set(TelephonyProperties.PROPERTY_ROAMING_INDICATOR_NEEDED_3, "false");
            } else if (mSimId == PhoneConstants.GEMINI_SIM_4){
                SystemProperties.set(TelephonyProperties.PROPERTY_ROAMING_INDICATOR_NEEDED_4, "false");
            }
            mRoamingOffRegistrants.notifyRegistrants();
        }

        if (! isGprsConsistent(gprsState, ss.getState())) {
            if (!mStartedGprsRegCheck && !mReportedGprsNoReg) {
                mStartedGprsRegCheck = true;

                int check_period = Settings.Global.getInt(
                        phone.getContext().getContentResolver(),
                        Settings.Global.GPRS_REGISTER_CHECK_PERIOD_MS,
                        DEFAULT_GPRS_CHECK_PERIOD_MILLIS);
                sendMessageDelayed(obtainMessage(EVENT_CHECK_REPORT_GPRS),
                        check_period);
            }
        } else {
            mReportedGprsNoReg = false;
        }
    }

    /**
     * Check if GPRS got registered while voice is registered.
     *
     * @param gprsState for GPRS registration state, i.e. CGREG in GSM
     * @param serviceState for voice registration state, i.e. CREG in GSM
     * @return false if device only register to voice but not gprs
     */
    private boolean isGprsConsistent(int gprsState, int serviceState) {
        return !((serviceState == ServiceState.STATE_IN_SERVICE) &&
                (gprsState != ServiceState.STATE_IN_SERVICE));
    }

    /**
     * Returns a TimeZone object based only on parameters from the NITZ string.
     */
    private TimeZone getNitzTimeZone(int offset, boolean dst, long when) {
        TimeZone guess = findTimeZone(offset, dst, when);
        if (guess == null) {
            // Couldn't find a proper timezone.  Perhaps the DST data is wrong.
            guess = findTimeZone(offset, !dst, when);
        }
        if (DBG) log("getNitzTimeZone returning " + (guess == null ? guess : guess.getID()));
        return guess;
    }

    private TimeZone findTimeZone(int offset, boolean dst, long when) {
    	log("[NITZ],findTimeZone,offset:"+offset+",dst:"+dst+",when:"+when);
        int rawOffset = offset;
        if (dst) {
            rawOffset -= 3600000;
        }
        String[] zones = TimeZone.getAvailableIDs(rawOffset);
        TimeZone guess = null;
        Date d = new Date(when);
        for (String zone : zones) {
            TimeZone tz = TimeZone.getTimeZone(zone);
            if (tz.getOffset(when) == offset &&
                tz.inDaylightTime(d) == dst) {
                guess = tz;
                log("[NITZ],find time zone.");
                break;
            }
        }

        return guess;
    }

    private void queueNextSignalStrengthPoll() {
        if (dontPollSignalStrength) {
            // The radio is telling us about signal strength changes
            // we don't have to ask it
            return;
        }

        Message msg;

        msg = obtainMessage();
        msg.what = EVENT_POLL_SIGNAL_STRENGTH;

        long nextTime;

        // TODO Don't poll signal strength if screen is off
        sendMessageDelayed(msg, POLL_PERIOD_MILLIS);
    }


    private void onNetworkStateChangeResult(AsyncResult ar) {
        String info[];
        int state = -1;
        int lac= -1;
        int cid = -1;
        int Act= -1;
        int cause= -1;

        /* Note: There might not be full +CREG URC info when screen off
                   Full URC format: +CREG:  <stat>, <lac>, <cid>, <Act>,<cause> */
        if (ar.exception != null || ar.result == null) {
           loge("onNetworkStateChangeResult exception");
        } else {
            info = (String[])ar.result;

            if (info.length > 0) {

                state = Integer.parseInt(info[0]);

                if (info[1] != null && info[1].length() > 0) {
                   lac = Integer.parseInt(info[1], 16);
                }

                if (info[2] != null && info[2].length() > 0) {
                   cid = Integer.parseInt(info[2], 16);
                }

                if (info[3] != null && info[3].length() > 0) {
                   Act = Integer.parseInt(info[3]);
                }

                if (info[4] != null && info[4].length() > 0) {
                   cause = Integer.parseInt(info[4]);
                }

                log("onNetworkStateChangeResult state:"+state+"lac:"+lac+"cid:"+cid+"Act:"+Act+"cause:"+cause);

                //ALPS00267573 CDR-ONS-245
                if(mServiceStateExt.needIgnoredState(ss.getState(),state,cause) == true)
                {
                    dontUpdateNetworkStateFlag = true;
                    return;
                }
                else
                {
                    dontUpdateNetworkStateFlag = false;
                }

                // ALPS00283696 CDR-NWS-241
                if(mServiceStateExt.needRejectCauseNotification(cause) == true)
                {
                    setRejectCauseNotification(cause);
                }


                // ALPS00283717 CDR-NWS-190
                if(mServiceStateExt.setEmergencyCallsOnly(state,cid) == 1)
                {
                    log("onNetworkStateChangeResult set mEmergencyOnly");
                    mEmergencyOnly = true;
                }
                else if(mServiceStateExt.setEmergencyCallsOnly(state,cid) == 0)
                {
                    if(mEmergencyOnly == true)
                    {
                        log("onNetworkStateChangeResult reset mEmergencyOnly");
                        mEmergencyOnly = false;
                    }
                }

            } else {
                loge("onNetworkStateChangeResult length zero");
            }
        }

        return;
    }


    /**
     *  Send signal-strength-changed notification if changed.
     *  Called both for solicited and unsolicited signal strength updates.
     */
    private AsyncResult onGsmSignalStrengthResult(AsyncResult ar) {
        AsyncResult ret = new AsyncResult (ar.userObj, null, ar.exception);
        int rssi = 99;
        int mGsmBitErrorRate = -1;
        int mGsmRssiQdbm = 0;
        int mGsmRscpQdbm = 0;
        int mGsmEcn0Qdbm = 0;

        if (ar.exception != null) {
            // -1 = unknown
            // most likely radio is resetting/disconnected
            setSignalStrengthDefaultValues();
        } else {
            //int[] ints = (int[])ar.result;
            SignalStrength s = (SignalStrength)ar.result;
            rssi = s.getGsmSignalStrength();
            if (rssi != 99) {
                /* MTK RIL send signal strength information in the following order rssi,ber,rssi_qdbm, rscp_qdbm, ecn0_qdbm*/
                mGsmRssiQdbm = s.getCdmaDbm();
                mGsmRscpQdbm = s.getCdmaEcio();
                mGsmEcn0Qdbm = s.getEvdoDbm();
                SignalStrength mNewSignalStrength = new SignalStrength(mSimId, rssi, -1, -1, -1, -1, -1, -1, 99,
                                                     SignalStrength.INVALID,
                                                     SignalStrength.INVALID,
                                                     SignalStrength.INVALID,
                                                     SignalStrength.INVALID,
                                                     true, mGsmRssiQdbm, mGsmRscpQdbm, mGsmEcn0Qdbm);

                //if (DBG) log("onGsmSignalStrengthResult():mNewSignalStrength="+ mNewSignalStrength.toString());

                ret = new AsyncResult (ar.userObj, mNewSignalStrength, ar.exception);
            }
        }

        //MTK-START [ALPS415367]For MR1 Migration
        //BEGIN mtk03923 [20120115][ALPS00113979]
        //mSignalStrength = new SignalStrength(rssi, -1, -1, -1, -1, -1, -1, lteSignalStrength, lteRsrp, lteRsrq, lteRssnr, lteCqi, true);
        //MTK-START [mtk04258][120308][ALPS00237725]For CMCC
        //mSignalStrength = new SignalStrength(rssi, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, true, rscpQdbm,mSimId);
        //MTK-END [mtk04258][120308][ALPS00237725]For CMCC
        //BEGIN mtk03923 [20120115][ALPS00113979]

        //if (!mSignalStrength.equals(oldSignalStrength)) {
        //    try { // This takes care of delayed EVENT_POLL_SIGNAL_STRENGTH (scheduled after
        //        // POLL_PERIOD_MILLIS) during Radio Technology Change)
        //        //20120317 ALPS_00253948	 ignore unknown RSSI state (99)
        //        if((rssi == 99)&&(ss.getState() == ServiceState.STATE_IN_SERVICE)){
        //            log("Ignore rssi 99(unknown)");
        //        }
        //        else{
        //            phone.notifySignalStrength();
        //        }
        //   } catch (NullPointerException ex) {
        //      log("onSignalStrengthResult() Phone already destroyed: " + ex
        //                + "SignalStrength not notified");
        //   }
        //}
        //MTK-END [ALPS415367]For MR1 Migration

        return ret;
    }

    /**
     * Set restricted state based on the OnRestrictedStateChanged notification
     * If any voice or packet restricted state changes, trigger a UI
     * notification and notify registrants when sim is ready.
     *
     * @param ar an int value of RIL_RESTRICTED_STATE_*
     */
    private void onRestrictedStateChanged(AsyncResult ar) {
        RestrictedState newRs = new RestrictedState();

        if (DBG) log("onRestrictedStateChanged: E rs "+ mRestrictedState);

        if (ar.exception == null) {
            int[] ints = (int[])ar.result;
            int state = ints[0];

            newRs.setCsEmergencyRestricted(
                    ((state & RILConstants.RIL_RESTRICTED_STATE_CS_EMERGENCY) != 0) ||
                    ((state & RILConstants.RIL_RESTRICTED_STATE_CS_ALL) != 0) );
            //ignore the normal call and data restricted state before SIM READY
            if (mUiccApplcation != null && mUiccApplcation.getState() == AppState.APPSTATE_READY) {
                newRs.setCsNormalRestricted(
                        ((state & RILConstants.RIL_RESTRICTED_STATE_CS_NORMAL) != 0) ||
                        ((state & RILConstants.RIL_RESTRICTED_STATE_CS_ALL) != 0) );
                newRs.setPsRestricted(
                        (state & RILConstants.RIL_RESTRICTED_STATE_PS_ALL)!= 0);
            } else {
                log("[DSAC DEB] IccCard state Not ready ");
                if (mRestrictedState.isCsNormalRestricted() &&
                	((state & RILConstants.RIL_RESTRICTED_STATE_CS_NORMAL) == 0 &&
                	(state & RILConstants.RIL_RESTRICTED_STATE_CS_ALL) == 0)) {
                        newRs.setCsNormalRestricted(false);
                }

                if(mRestrictedState.isPsRestricted() && ((state & RILConstants.RIL_RESTRICTED_STATE_PS_ALL) == 0)) {
                    newRs.setPsRestricted(false);
                }
    	    }

            log("[DSAC DEB] new rs "+ newRs);

            if (!mRestrictedState.isPsRestricted() && newRs.isPsRestricted()) {
                mPsRestrictEnabledRegistrants.notifyRegistrants();
                setNotification(PS_ENABLED);
            } else if (mRestrictedState.isPsRestricted() && !newRs.isPsRestricted()) {
                mPsRestrictDisabledRegistrants.notifyRegistrants();
                setNotification(PS_DISABLED);
            }

            /**
             * There are two kind of cs restriction, normal and emergency. So
             * there are 4 x 4 combinations in current and new restricted states
             * and we only need to notify when state is changed.
             */
            if (mRestrictedState.isCsRestricted()) {
                if (!newRs.isCsRestricted()) {
                    // remove all restriction
                    setNotification(CS_DISABLED);
                } else if (!newRs.isCsNormalRestricted()) {
                    // remove normal restriction
                    setNotification(CS_EMERGENCY_ENABLED);
                } else if (!newRs.isCsEmergencyRestricted()) {
                    // remove emergency restriction
                    setNotification(CS_NORMAL_ENABLED);
                }
            } else if (mRestrictedState.isCsEmergencyRestricted() &&
                    !mRestrictedState.isCsNormalRestricted()) {
                if (!newRs.isCsRestricted()) {
                    // remove all restriction
                    setNotification(CS_DISABLED);
                } else if (newRs.isCsRestricted()) {
                    // enable all restriction
                    setNotification(CS_ENABLED);
                } else if (newRs.isCsNormalRestricted()) {
                    // remove emergency restriction and enable normal restriction
                    setNotification(CS_NORMAL_ENABLED);
                }
            } else if (!mRestrictedState.isCsEmergencyRestricted() &&
                    mRestrictedState.isCsNormalRestricted()) {
                if (!newRs.isCsRestricted()) {
                    // remove all restriction
                    setNotification(CS_DISABLED);
                } else if (newRs.isCsRestricted()) {
                    // enable all restriction
                    setNotification(CS_ENABLED);
                } else if (newRs.isCsEmergencyRestricted()) {
                    // remove normal restriction and enable emergency restriction
                    setNotification(CS_EMERGENCY_ENABLED);
                }
            } else {
                if (newRs.isCsRestricted()) {
                    // enable all restriction
                    setNotification(CS_ENABLED);
                } else if (newRs.isCsEmergencyRestricted()) {
                    // enable emergency restriction
                    setNotification(CS_EMERGENCY_ENABLED);
                } else if (newRs.isCsNormalRestricted()) {
                    // enable normal restriction
                    setNotification(CS_NORMAL_ENABLED);
                }
            }

            mRestrictedState = newRs;
        }
        log("onRestrictedStateChanged: X rs "+ mRestrictedState);
    }

    /** code is registration state 0-5 from TS 27.007 7.2 */
    private int regCodeToServiceState(int code) {
        switch (code) {
            case 0:
            case 2: // 2 is "searching"
            case 3: // 3 is "registration denied"
            case 4: // 4 is "unknown" no vaild in current baseband
            case 10:// same as 0, but indicates that emergency call is possible.
            case 12:// same as 2, but indicates that emergency call is possible.
            case 13:// same as 3, but indicates that emergency call is possible.
            case 14:// same as 4, but indicates that emergency call is possible.
                return ServiceState.STATE_OUT_OF_SERVICE;

            case 1:
                return ServiceState.STATE_IN_SERVICE;

            case 5:
                // in service, roam
                return ServiceState.STATE_IN_SERVICE;

            default:
                loge("regCodeToServiceState: unexpected service state " + code);
                return ServiceState.STATE_OUT_OF_SERVICE;
        }
    }


    /**
     * code is registration state 0-5 from TS 27.007 7.2
     * returns true if registered roam, false otherwise
     */
    private boolean regCodeIsRoaming (int code) {
        boolean isRoaming = false;
        // SIMRecords simRecords = (SIMRecords)(phone.mIccRecords.get());
        SIMRecords simRecords = null;
    	  IccRecords r = phone.mIccRecords.get();
    	  if (r != null) {
            simRecords = (SIMRecords)r;
    	  }
    	          
        //String strHomePlmn = simRecords.getSIMOperatorNumeric();
        String strHomePlmn = (simRecords != null) ? simRecords.getSIMOperatorNumeric() : null;
        String strServingPlmn = newSS.getOperatorNumeric();
        boolean isServingPlmnInGroup = false;
        boolean isHomePlmnInGroup = false;

        if(5 == code){
            isRoaming = true;
        }


        /* ALPS00296372 */
        if((mServiceStateExt.ignoreDomesticRoaming() == true) && (isRoaming == true) && (strServingPlmn != null) &&(strHomePlmn != null))
        {
            log("ServingPlmn = "+strServingPlmn+"HomePlmn"+strHomePlmn);

            if(strHomePlmn.substring(0, 3).equals(strServingPlmn.substring(0, 3)))
            {
                log("Same MCC,don't set as roaming");
                isRoaming = false;
            }
        }

        int mccmnc = 0;

        if (phone.getMySimId() == PhoneConstants.GEMINI_SIM_1) {
            mccmnc = SystemProperties.getInt(TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC, 0);
        } else if (phone.getMySimId() == PhoneConstants.GEMINI_SIM_2){
            mccmnc = SystemProperties.getInt(TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC_2, 0);
        } else if (phone.getMySimId() == PhoneConstants.GEMINI_SIM_3){
            mccmnc = SystemProperties.getInt(TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC_3, 0);
        } else if (phone.getMySimId() == PhoneConstants.GEMINI_SIM_4){
            mccmnc = SystemProperties.getInt(TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC_4, 0);
        }
        String numeric = newSS.getOperatorNumeric();
        Log.d(LOG_TAG,"numeric:"+numeric+"mccmnc:"+mccmnc);

        isRoaming = mServiceStateExt.isRegCodeRoaming(isRoaming, mccmnc, numeric);

        /* mtk01616 ALPS00236452: check manufacturer maintained table for specific operator with multiple home PLMN id */
        if((isRoaming == true) && (strServingPlmn != null) &&(strHomePlmn != null)){
            log("strServingPlmn = "+strServingPlmn+"strHomePlmn"+strHomePlmn);

            for(int i=0; i <customEhplmn.length; i++){
                //reset flag
                isServingPlmnInGroup = false;
                isHomePlmnInGroup = false;

                //check if serving plmn or home plmn in this group
                for(int j=0; j<	customEhplmn[i].length;j++){
                    if(strServingPlmn.equals(customEhplmn[i][j])){
                        isServingPlmnInGroup = true;
                    }
                    if(strHomePlmn.equals(customEhplmn[i][j])){
                        isHomePlmnInGroup = true;
                    }
                }

                //if serving plmn and home plmn both in the same group , do NOT treat it as roaming
                if((isServingPlmnInGroup == true) && (isHomePlmnInGroup == true)){
                    isRoaming = false;
                    log("Ignore roaming");
                    break;
                }
            }
        }

        return isRoaming;

////        // 5 is  "in service -- roam"
////        return 5 == code;
    }

    /**
     * Set roaming state when gsmRoaming is true and, if operator mcc is the
     * same as sim mcc, ons is different from spn
     * @param gsmRoaming TS 27.007 7.2 CREG registered roaming
     * @param s ServiceState hold current ons
     * @return true for roaming state set
     */
    private boolean isRoamingBetweenOperators(boolean gsmRoaming, ServiceState s) {
        String spn = SystemProperties.get(TelephonyProperties.PROPERTY_ICC_OPERATOR_ALPHA, "empty");

        String onsl = s.getOperatorAlphaLong();
        String onss = s.getOperatorAlphaShort();
        String simNumeric;
        String  operatorNumeric = s.getOperatorNumeric();

        if (mSimId == PhoneConstants.GEMINI_SIM_1) {
            spn = SystemProperties.get(TelephonyProperties.PROPERTY_ICC_OPERATOR_ALPHA, "empty");
            simNumeric = SystemProperties.get(TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC, "");
        } else {
            spn = SystemProperties.get(TelephonyProperties.PROPERTY_ICC_OPERATOR_ALPHA_2, "empty");
            simNumeric = SystemProperties.get(TelephonyProperties.PROPERTY_ICC_OPERATOR_NUMERIC_2, "");
        }

        boolean equalsOnsl = onsl != null && !onsl.equals("") && spn.equals(onsl);
        boolean equalsOnss = onss != null && !onss.equals("") && spn.equals(onss);

        boolean equalsMcc = true;
        try {
            equalsMcc = simNumeric.substring(0, 3).
                    equals(operatorNumeric.substring(0, 3));
        } catch (Exception e){
            Log.w(LOG_TAG, "simNumeric parsing error: " + simNumeric);
            e.printStackTrace();
        }

        return gsmRoaming && !(equalsMcc && (equalsOnsl || equalsOnss));
    }

    private static int twoDigitsAt(String s, int offset) {
        int a, b;

        a = Character.digit(s.charAt(offset), 10);
        b = Character.digit(s.charAt(offset+1), 10);

        if (a < 0 || b < 0) {

            throw new RuntimeException("invalid format");
        }

        return a*10 + b;
    }

    /**
     * @return The current GPRS state. IN_SERVICE is the same as "attached"
     * and OUT_OF_SERVICE is the same as detached.
     */
    int getCurrentGprsState() {
        return gprsState;
    }

    public int getCurrentDataConnectionState() {
        return gprsState;
    }

    /**
     * @return true if phone is camping on a technology (eg UMTS)
     * that could support voice and data simultaneously.
     */
    public boolean isConcurrentVoiceAndDataAllowed() {
        // return (mRilRadioTechnology >= ServiceState.RIL_RADIO_TECHNOLOGY_UMTS);
        boolean isAllowed = (mRilRadioTechnology >= ServiceState.RIL_RADIO_TECHNOLOGY_UMTS);

        // M: Check peer phone is in call or not
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            for (int i=PhoneConstants.GEMINI_SIM_1; i<PhoneConstants.GEMINI_SIM_NUM; i++) {
                if (i != phone.getMySimId() && phone.getPeerPhones(i).getState() != PhoneConstants.State.IDLE) {
                    if (DBG) log("isConcurrentVoiceAndDataAllowed(): Phone" + i + " is in call");
                    isAllowed = false;
                    break;
                }
            }
        }
        return isAllowed;
    }

    /**
     * Provides the name of the algorithmic time zone for the specified
     * offset.  Taken from TimeZone.java.
     */
    private static String displayNameFor(int off) {
        off = off / 1000 / 60;

        char[] buf = new char[9];
        buf[0] = 'G';
        buf[1] = 'M';
        buf[2] = 'T';

        if (off < 0) {
            buf[3] = '-';
            off = -off;
        } else {
            buf[3] = '+';
        }

        int hours = off / 60;
        int minutes = off % 60;

        buf[4] = (char) ('0' + hours / 10);
        buf[5] = (char) ('0' + hours % 10);

        buf[6] = ':';

        buf[7] = (char) ('0' + minutes / 10);
        buf[8] = (char) ('0' + minutes % 10);

        return new String(buf);
    }

    /**
     * nitzReceiveTime is time_t that the NITZ time was posted
     */
    private void setTimeFromNITZString (String nitz, long nitzReceiveTime) {
        // "yy/mm/dd,hh:mm:ss(+/-)tz"
        // tz is in number of quarter-hours

        long start = SystemClock.elapsedRealtime();
        if (DBG) {log("NITZ: " + nitz + "," + nitzReceiveTime +
                        " start=" + start + " delay=" + (start - nitzReceiveTime));
        }

        try {
            /* NITZ time (hour:min:sec) will be in UTC but it supplies the timezone
             * offset as well (which we won't worry about until later) */
            Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));

            c.clear();
            c.set(Calendar.DST_OFFSET, 0);

            String[] nitzSubs = nitz.split("[/:,+-]");

            int year = 2000 + Integer.parseInt(nitzSubs[0]);
            c.set(Calendar.YEAR, year);

            // month is 0 based!
            int month = Integer.parseInt(nitzSubs[1]) - 1;
            c.set(Calendar.MONTH, month);

            int date = Integer.parseInt(nitzSubs[2]);
            c.set(Calendar.DATE, date);

            int hour = Integer.parseInt(nitzSubs[3]);
            c.set(Calendar.HOUR, hour);

            int minute = Integer.parseInt(nitzSubs[4]);
            c.set(Calendar.MINUTE, minute);

            int second = Integer.parseInt(nitzSubs[5]);
            c.set(Calendar.SECOND, second);

            boolean sign = (nitz.indexOf('-') == -1);

            int tzOffset = Integer.parseInt(nitzSubs[6]);

            int dst = (nitzSubs.length >= 8 ) ? Integer.parseInt(nitzSubs[7])
                                              : 0;

            // The zone offset received from NITZ is for current local time,
            // so DST correction is already applied.  Don't add it again.
            //
            // tzOffset += dst * 4;
            //
            // We could unapply it if we wanted the raw offset.

            tzOffset = (sign ? 1 : -1) * tzOffset * 15 * 60 * 1000;

            TimeZone    zone = null;

            // As a special extension, the Android emulator appends the name of
            // the host computer's timezone to the nitz string. this is zoneinfo
            // timezone name of the form Area!Location or Area!Location!SubLocation
            // so we need to convert the ! into /
            if (nitzSubs.length >= 9) {
                String  tzname = nitzSubs[8].replace('!','/');
                zone = TimeZone.getTimeZone( tzname );
                log("[NITZ] setTimeFromNITZString,tzname:"+tzname+"zone:"+zone);
            }

            String iso;
            if (mSimId == PhoneConstants.GEMINI_SIM_1) {
                iso = SystemProperties.get(TelephonyProperties.PROPERTY_OPERATOR_ISO_COUNTRY);
            } else if (mSimId == PhoneConstants.GEMINI_SIM_2){
                iso = SystemProperties.get(TelephonyProperties.PROPERTY_OPERATOR_ISO_COUNTRY_2);
            } else if (mSimId == PhoneConstants.GEMINI_SIM_3){
                iso = SystemProperties.get(TelephonyProperties.PROPERTY_OPERATOR_ISO_COUNTRY_3);
            } else {
                iso = SystemProperties.get(TelephonyProperties.PROPERTY_OPERATOR_ISO_COUNTRY_4);
            }
            log("[NITZ] setTimeFromNITZString,mGotCountryCode:"+mGotCountryCode);

            if (zone == null) {

                if (mGotCountryCode) {
                    if (iso != null && iso.length() > 0) {
                        zone = TimeUtils.getTimeZone(tzOffset, dst != 0,
                                c.getTimeInMillis(),
                                iso);
                    } else {
                        // We don't have a valid iso country code.  This is
                        // most likely because we're on a test network that's
                        // using a bogus MCC (eg, "001"), so get a TimeZone
                        // based only on the NITZ parameters.
                        zone = getNitzTimeZone(tzOffset, (dst != 0), c.getTimeInMillis());
                    }
                }
            }

            if ((zone == null) || (mZoneOffset != tzOffset) || (mZoneDst != (dst != 0))){
                // We got the time before the country or the zone has changed
                // so we don't know how to identify the DST rules yet.  Save
                // the information and hope to fix it up later.

                mNeedFixZoneAfterNitz = true;
                mZoneOffset  = tzOffset;
                mZoneDst     = dst != 0;
                mZoneTime    = c.getTimeInMillis();
            }

            if (zone != null) {
                if (getAutoTimeZone()) {
                    setAndBroadcastNetworkSetTimeZone(zone.getID());
                }
                saveNitzTimeZone(zone.getID());
            }

            String ignore = SystemProperties.get("gsm.ignore-nitz");
            if (ignore != null && ignore.equals("yes")) {
                log("NITZ: Not setting clock because gsm.ignore-nitz is set");
                return;
            }

            try {
                mWakeLock.acquire();

                if (getAutoTime()) {
                    long millisSinceNitzReceived
                            = SystemClock.elapsedRealtime() - nitzReceiveTime;

                    if (millisSinceNitzReceived < 0) {
                        // Sanity check: something is wrong
                        if (DBG) {
                            log("NITZ: not setting time, clock has rolled "
                                            + "backwards since NITZ time was received, "
                                            + nitz);
                        }
                        return;
                    }

                    if (millisSinceNitzReceived > Integer.MAX_VALUE) {
                        // If the time is this far off, something is wrong > 24 days!
                        if (DBG) {
                            log("NITZ: not setting time, processing has taken "
                                        + (millisSinceNitzReceived / (1000 * 60 * 60 * 24))
                                        + " days");
                        }
                        return;
                    }

                    // Note: with range checks above, cast to int is safe
                    c.add(Calendar.MILLISECOND, (int)millisSinceNitzReceived);

                    if (DBG) {
                        log("NITZ: Setting time of day to " + c.getTime()
                            + " NITZ receive delay(ms): " + millisSinceNitzReceived
                            + " gained(ms): "
                            + (c.getTimeInMillis() - System.currentTimeMillis())
                            + " from " + nitz);
                    }

                    setAndBroadcastNetworkSetTime(c.getTimeInMillis());
                    Log.i(LOG_TAG, "NITZ: after Setting time of day");
                }

                /* Originally, no user to check Android defined property gsm.nitz.time. 
                                 But now Settings need to check if we ever receive NITZ from NW via this property.
                                 This is treated as a only hint to know "if the network support NITZ or not" */				
                if(mSimId == PhoneConstants.GEMINI_SIM_1)
                    SystemProperties.set("gsm.nitz.time", String.valueOf(c.getTimeInMillis()));
                else if(mSimId == PhoneConstants.GEMINI_SIM_2)
                    SystemProperties.set("gsm.nitz.time.2", String.valueOf(c.getTimeInMillis()));
	
                saveNitzTime(c.getTimeInMillis());
                if (DBG) {
                    long end = SystemClock.elapsedRealtime();
                    log("NITZ: end=" + end + " dur=" + (end - start));
                }
                mNitzUpdatedTime = true;
            } finally {
                mWakeLock.release();
            }
        } catch (RuntimeException ex) {
            loge("NITZ: Parsing NITZ time " + nitz + " ex=" + ex);
        }
    }

    private boolean getAutoTime() {
        try {
            return Settings.Global.getInt(phone.getContext().getContentResolver(),
                    Settings.Global.AUTO_TIME) > 0;
        } catch (SettingNotFoundException snfe) {
            return true;
        }
    }

    private boolean getAutoTimeZone() {
        try {
            return Settings.Global.getInt(phone.getContext().getContentResolver(),
                    Settings.Global.AUTO_TIME_ZONE) > 0;
        } catch (SettingNotFoundException snfe) {
            return true;
        }
    }

    private void saveNitzTimeZone(String zoneId) {
        mSavedTimeZone = zoneId;
    }

    private void saveNitzTime(long time) {
        mSavedTime = time;
        mSavedAtTime = SystemClock.elapsedRealtime();
    }

    /**
     * Set the timezone and send out a sticky broadcast so the system can
     * determine if the timezone was set by the carrier.
     *
     * @param zoneId timezone set by carrier
     */
    private void setAndBroadcastNetworkSetTimeZone(String zoneId) {
        if (DBG) log("setAndBroadcastNetworkSetTimeZone: setTimeZone=" + zoneId);
        AlarmManager alarm =
            (AlarmManager) phone.getContext().getSystemService(Context.ALARM_SERVICE);
        alarm.setTimeZone(zoneId);
        Intent intent = new Intent(TelephonyIntents.ACTION_NETWORK_SET_TIMEZONE);
        intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
        intent.putExtra("time-zone", zoneId);
        phone.getContext().sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        if (DBG) {
            log("setAndBroadcastNetworkSetTimeZone: call alarm.setTimeZone and broadcast zoneId=" +
                zoneId);
        }
    }

    /**
     * Set the time and Send out a sticky broadcast so the system can determine
     * if the time was set by the carrier.
     *
     * @param time time set by network
     */
    private void setAndBroadcastNetworkSetTime(long time) {
        if (DBG) log("setAndBroadcastNetworkSetTime: time=" + time + "ms");
        SystemClock.setCurrentTimeMillis(time);
        Intent intent = new Intent(TelephonyIntents.ACTION_NETWORK_SET_TIME);
        intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
        intent.putExtra("time", time);
        phone.getContext().sendStickyBroadcastAsUser(intent, UserHandle.ALL);
    }

    private void revertToNitzTime() {
        if (Settings.Global.getInt(phone.getContext().getContentResolver(),
                Settings.Global.AUTO_TIME, 0) == 0) {
        	log("[NITZ]:revertToNitz,AUTO_TIME is 0");
            return;
        }
       log("[NITZ]:Reverting to NITZ: tz='" + mSavedTimeZone
                + "' mSavedTime=" + mSavedTime
                + " mSavedAtTime=" + mSavedAtTime);
        if (mSavedTime != 0 && mSavedAtTime != 0) {
            /*ALPS00449624 : remove revert time zone , it's moved to revertToNitzTimeZone() */
            setAndBroadcastNetworkSetTime(mSavedTime
                    + (SystemClock.elapsedRealtime() - mSavedAtTime));
        }
    }

    private void revertToNitzTimeZone() {
        if (Settings.Global.getInt(phone.getContext().getContentResolver(),
                Settings.Global.AUTO_TIME_ZONE, 0) == 0) {
            return;
        }
        if (DBG) log("Reverting to NITZ TimeZone: tz='" + mSavedTimeZone);
        if (mSavedTimeZone != null) {
            setAndBroadcastNetworkSetTimeZone(mSavedTimeZone);
        }
    }


    /**
     * Post a notification to NotificationManager for network reject cause
     *
     * @param cause
     */
    private void setRejectCauseNotification(int cause) {

        if (DBG) log("setRejectCauseNotification: create notification " + cause);

//toast notification sample code
/*
        Context context = phone.getContext();
        CharSequence text = "";
        int duration = Toast.LENGTH_LONG;

        switch (cause) {
            case 2:
                text = context.getText(com.mediatek.R.string.MMRejectCause2);;
                break;
            case 3:
                text = context.getText(com.mediatek.R.string.MMRejectCause3);;
                break;
            case 5:
                text = context.getText(com.mediatek.R.string.MMRejectCause5);;
                break;
            case 6:
                text = context.getText(com.mediatek.R.string.MMRejectCause6);;
                break;
            default:
                break;
        }

        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
*/
//status notification

        Context context = phone.getContext();

        mNotification = new Notification();
        mNotification.when = System.currentTimeMillis();
        mNotification.flags = Notification.FLAG_AUTO_CANCEL;
        mNotification.icon = com.android.internal.R.drawable.stat_sys_warning;
        Intent intent = new Intent();
        mNotification.contentIntent = PendingIntent
        .getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        CharSequence details = "";
        CharSequence title = context.getText(com.mediatek.R.string.RejectCauseTitle);
        int notificationId = REJECT_NOTIFICATION;

		if (FeatureOption.MTK_GEMINI_SUPPORT) {
		log("show name log");
        	if (FeatureOption.MTK_GEMINI_ENHANCEMENT == true) {
        		SIMInfo siminfo = SIMInfo.getSIMInfoBySlot(phone.getContext(),mSimId);
        		if (siminfo != null){
        			mNotification.simId = siminfo.mSimId;
        			mNotification.simInfoType = 3;
				if (mSimId != PhoneConstants.GEMINI_SIM_1){
					notificationId = REJECT_NOTIFICATION_2;
				}
        		}
        	}else {
			log("show sim1 log");
        		if (mSimId == PhoneConstants.GEMINI_SIM_1) {
        			title = "SIM1-" + context.getText(com.android.internal.R.string.RestrictedChangedTitle);
        		} else {
        			notificationId = REJECT_NOTIFICATION_2;
        			title = "SIM2-" + context.getText(com.android.internal.R.string.RestrictedChangedTitle);
        		}
            }
        }

        switch (cause) {
            case 2:
                details = context.getText(com.mediatek.R.string.MMRejectCause2);;
                break;
            case 3:
                details = context.getText(com.mediatek.R.string.MMRejectCause3);;
                break;
            case 5:
                details = context.getText(com.mediatek.R.string.MMRejectCause5);;
                break;
            case 6:
                details = context.getText(com.mediatek.R.string.MMRejectCause6);;
                break;
            //[ALPS00435948] MTK ADD-START
            case 13:
                details = context.getText(R.string.MMRejectCause13);
                break;
            //[ALPS00435948] MTK ADD-END
            default:
                break;
        }

        if (DBG) log("setRejectCauseNotification: put notification " + title + " / " +details);
        mNotification.tickerText = title;
        mNotification.setLatestEventInfo(context, title, details,
                mNotification.contentIntent);

        NotificationManager notificationManager = (NotificationManager)
            context.getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(notificationId, mNotification);
    }


    /**
     * Post a notification to NotificationManager for restricted state
     *
     * @param notifyType is one state of PS/CS_*_ENABLE/DISABLE
     */
    private void setNotification(int notifyType) {
    /* ALPS00339508 :Remove restricted access change notification */
/*
        if (DBG) log("setNotification: create notification " + notifyType);
        Context context = phone.getContext();

        mNotification = new Notification();
        mNotification.when = System.currentTimeMillis();
        mNotification.flags = Notification.FLAG_AUTO_CANCEL;
        mNotification.icon = com.android.internal.R.drawable.stat_sys_warning;
        Intent intent = new Intent();
        mNotification.contentIntent = PendingIntent
        .getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        CharSequence details = "";
        CharSequence title = context.getText(com.android.internal.R.string.RestrictedChangedTitle);
        int notificationId = CS_NOTIFICATION;
        if (FeatureOption.MTK_GEMINI_SUPPORT && mSimId == PhoneConstants.GEMINI_SIM_2) {
            notificationId = CS_NOTIFICATION_2;
        }

        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            log("show name log");
            if (FeatureOption.MTK_GEMINI_ENHANCEMENT == true) {
                SIMInfo siminfo = SIMInfo.getSIMInfoBySlot(phone.getContext(),mSimId);
                if (siminfo != null){
                    mNotification.simId = siminfo.mSimId;
                    mNotification.simInfoType = 3;
                    if (mSimId != PhoneConstants.GEMINI_SIM_1){
                        notificationId = CS_NOTIFICATION_2;
                    }
                }
             }else {
                 log("show sim1 log");
                 if (mSimId == PhoneConstants.GEMINI_SIM_1) {
                     //title = context.getText(com.mediatek.R.string.RestrictedChangedTitle_SIM1);
                     title = "SIM1-" + context.getText(com.android.internal.R.string.RestrictedChangedTitle);
                 } else {
                     notificationId = CS_NOTIFICATION_2;
                     //title = context.getText(com.mediatek.R.string.RestrictedChangedTitle_SIM2);
                     title = "SIM2-" + context.getText(com.android.internal.R.string.RestrictedChangedTitle);
                 }
            }
        }

        switch (notifyType) {
        case PS_ENABLED:
            if (FeatureOption.MTK_GEMINI_SUPPORT && mSimId == PhoneConstants.GEMINI_SIM_2) {
                notificationId = PS_NOTIFICATION_2;
            } else {
            	notificationId = PS_NOTIFICATION;
            }
            details = context.getText(com.android.internal.R.string.RestrictedOnData);;
            break;
        case PS_DISABLED:
            if (FeatureOption.MTK_GEMINI_SUPPORT && mSimId == PhoneConstants.GEMINI_SIM_2) {
                notificationId = PS_NOTIFICATION_2;
            } else {
            	notificationId = PS_NOTIFICATION;
            }
            break;
        case CS_ENABLED:
            details = context.getText(com.android.internal.R.string.RestrictedOnAllVoice);;
            break;
        case CS_NORMAL_ENABLED:
            details = context.getText(com.android.internal.R.string.RestrictedOnNormal);;
            break;
        case CS_EMERGENCY_ENABLED:
            details = context.getText(com.android.internal.R.string.RestrictedOnEmergency);;
            break;
        case CS_DISABLED:
            // do nothing and cancel the notification later
            break;
        }

        if (DBG) log("setNotification: put notification " + title + " / " +details);
        mNotification.tickerText = title;
        mNotification.setLatestEventInfo(context, title, details,
                mNotification.contentIntent);

        NotificationManager notificationManager = (NotificationManager)
            context.getSystemService(Context.NOTIFICATION_SERVICE);

        //if (notifyType == PS_DISABLED || notifyType == CS_DISABLED) {
        //this is a temp solution from GB for resolving restricted mode notification problem (not to notify PS restricted)
        if (notifyType == PS_DISABLED || notifyType == CS_DISABLED || notifyType == PS_ENABLED) {
            // cancel previous post notification
            notificationManager.cancel(notificationId);
        } else {
            // update restricted state notification
            if (FeatureOption.MTK_GEMINI_SUPPORT && notifyType == PS_ENABLED) {
                //since we do not have to notice user that PS restricted
                //if default data SIM is not set to current PS restricted SIM
                //or it is in air plane mode or radio power is off
                int airplaneMode = Settings.Global.getInt(context.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0);
                int dualSimMode = Settings.System.getInt(context.getContentResolver(), Settings.System.DUAL_SIM_MODE_SETTING, 0);
                long dataSimID = Settings.System.getLong(context.getContentResolver(), Settings.System.GPRS_CONNECTION_SIM_SETTING, Settings.System.DEFAULT_SIM_NOT_SET);
                int dataSimSlot = SIMInfo.getSlotById(context, dataSimID);
                if (dataSimSlot == mSimId) {
                    if (airplaneMode != 0)
                        log("set notification but air plane mode, skip");
                    else if (phone.isSimInsert() && !((dualSimMode & (mSimId + 1)) == 0))
                        notificationManager.notify(notificationId, mNotification);
                    else
                        log("set notification but sim radio power off, skip");
                } else {
                    log("set notification but not data enabled SIM, skip");
                }
            } else {
                notificationManager.notify(notificationId, mNotification);
            }
        }
		*/
    }

    // ALPS00297554
    public void resetNotification() {
        int notificationId = CS_NOTIFICATION;
        if (mSimId == PhoneConstants.GEMINI_SIM_2)
            notificationId = CS_NOTIFICATION_2;

        Context context = phone.getContext();
        NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(notificationId);
    }

    @Override
    protected void onUpdateIccAvailability() {
        if (mUiccController == null ) {
            return;
        }

        UiccCardApplication newUiccApplication =
                mUiccController.getUiccCardApplication(UiccController.APP_FAM_3GPP);

        if (mUiccApplcation != newUiccApplication) {
            if (mUiccApplcation != null) {
                log("Removing stale icc objects.");
                mUiccApplcation.unregisterForReady(this);
                if (mIccRecords != null) {
                    mIccRecords.unregisterForRecordsLoaded(this);
                }
                mIccRecords = null;
                mUiccApplcation = null;
            }
            if (newUiccApplication != null) {
                log("New card found");
                mUiccApplcation = newUiccApplication;
                mIccRecords = mUiccApplcation.getIccRecords();
                mUiccApplcation.registerForReady(this, EVENT_SIM_READY, null);
                if (mIccRecords != null) {
                    mIccRecords.registerForRecordsLoaded(this, EVENT_SIM_RECORDS_LOADED, null);
                }
            }
        }
    }


    @Override
    protected void log(String s) {
        Log.d(LOG_TAG, "[GsmSST" + mSimId + "] " + s);
    }

    @Override
    protected void loge(String s) {
        Log.e(LOG_TAG, "[GsmSST" + mSimId + "] " + s);
    }

    private static void sloge(String s) {
        Log.e(LOG_TAG, "[GsmSST]" + s);
    }

    @Override
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("GsmServiceStateTracker extends:");
        super.dump(fd, pw, args);
        pw.println(" phone=" + phone);
        pw.println(" cellLoc=" + cellLoc);
        pw.println(" newCellLoc=" + newCellLoc);
        pw.println(" mPreferredNetworkType=" + mPreferredNetworkType);
        pw.println(" gprsState=" + gprsState);
        pw.println(" newGPRSState=" + newGPRSState);
        pw.println(" mMaxDataCalls=" + mMaxDataCalls);
        pw.println(" mNewMaxDataCalls=" + mNewMaxDataCalls);
        pw.println(" mReasonDataDenied=" + mReasonDataDenied);
        pw.println(" mNewReasonDataDenied=" + mNewReasonDataDenied);
        pw.println(" mGsmRoaming=" + mGsmRoaming);
        pw.println(" mDataRoaming=" + mDataRoaming);
        pw.println(" mEmergencyOnly=" + mEmergencyOnly);
        pw.println(" mNeedFixZoneAfterNitz=" + mNeedFixZoneAfterNitz);
        pw.println(" mZoneOffset=" + mZoneOffset);
        pw.println(" mZoneDst=" + mZoneDst);
        pw.println(" mZoneTime=" + mZoneTime);
        pw.println(" mGotCountryCode=" + mGotCountryCode);
        pw.println(" mNitzUpdatedTime=" + mNitzUpdatedTime);
        pw.println(" mSavedTimeZone=" + mSavedTimeZone);
        pw.println(" mSavedTime=" + mSavedTime);
        pw.println(" mSavedAtTime=" + mSavedAtTime);
        //MTK-START [ALPS415367]For MR1 Migration
        //pw.println(" mNeedToRegForSimLoaded=" + mNeedToRegForSimLoaded);
        //MTK-END [ALPS415367]For MR1 Migration
        pw.println(" mStartedGprsRegCheck=" + mStartedGprsRegCheck);
        pw.println(" mReportedGprsNoReg=" + mReportedGprsNoReg);
        pw.println(" mNotification=" + mNotification);
        pw.println(" mWakeLock=" + mWakeLock);
        pw.println(" curSpn=" + curSpn);
        pw.println(" curShowSpn=" + curShowSpn);
        pw.println(" curPlmn=" + curPlmn);
        //MTK-START [ALPS415367]For MR1 Migration
        //pw.println(" curSpnRule=" + curSpnRule);
        //MTK-END [ALPS415367]For MR1 Migration
		pw.println(" curShowPlmn=" + curShowPlmn);
    }

//MTK-START [mtk03851][111124]
    public void setRadioPowerOn() {
        // system setting property AIRPLANE_MODE_ON is set in Settings.
        int airplaneMode = Settings.Global.getInt(
                phone.getContext().getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0);
        /* for consistent UI ,SIM Management for single sim project START */
        if (!FeatureOption.MTK_GEMINI_SUPPORT) {
            int simMode = Settings.System.getInt(phone.getContext().getContentResolver(), Settings.System.DUAL_SIM_MODE_SETTING, 1);
            /* ALPS00447303 */
            Log.e(LOG_TAG, "Set mDesiredPowerState in setRadioPowerOn. simMode="+simMode+",airplaneMode="+airplaneMode);
            mDesiredPowerState = (simMode > 0) && (! (airplaneMode > 0));                        			
        }
        /* for consistent UI ,SIM Management for single sim project  END*/
        else {
            mDesiredPowerState = ! (airplaneMode > 0);
        }			
        Log.e(LOG_TAG, "Final mDesiredPowerState in setRadioPowerOn. [" + mDesiredPowerState + "] airplaneMode=" + airplaneMode);

        //since this will trigger radio power on
        //we should reset first radio change here
        mFirstRadioChange = true;

        log("setRadioPowerOn mDesiredPowerState " + mDesiredPowerState);
        cm.setRadioPowerOn(null);
    }

    public void setEverIVSR(boolean value)
    {
        log("setEverIVSR:" + value);
        mEverIVSR = value;

        /* ALPS00376525 notify IVSR start event */
        if(value == true){
            Intent intent = new Intent(TelephonyIntents.ACTION_IVSR_NOTIFY);
            intent.putExtra(TelephonyIntents.INTENT_KEY_IVSR_ACTION, "start");
            intent.putExtra(PhoneConstants.GEMINI_SIM_ID_KEY, mSimId);

            if (!FeatureOption.MTK_GEMINI_SUPPORT) {
                intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
            }

            log("broadcast ACTION_IVSR_NOTIFY intent");

            phone.getContext().sendStickyBroadcast(intent);
        }
    }

    public void setAutoGprsAttach(int auto) {
        mAutoGprsAttach = auto;
    }

    public void setGprsConnType(int type) {
        log("setGprsConnType:" + type);
        removeGprsConnTypeRetry();
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            DataConnectionTracker dcTracker = phone.mDataConnectionTracker;
            if (type == 0) {
                // Not Gprs Attach (set mMasterDataEnabled as false)
                dcTracker.setDataEnabled(false);
            } else {
                // Auto Gprs Attach then activate the default apn type's pdp context (set mMasterDataEnabled as true)
                dcTracker.setDataEnabled(true);
            }
        }
        
        gprsConnType = type;
        cm.setGprsConnType(type, obtainMessage(EVENT_SET_GPRS_CONN_TYPE_DONE, null));
    }

    private int updateAllOpertorInfo(String plmn){
        if(plmn!=null){
            ss.setOperatorAlphaLong(plmn);

            if (mSimId == PhoneConstants.GEMINI_SIM_1) {
                Log.d(LOG_TAG, "setOperatorAlphaLong and update PROPERTY_OPERATOR_ALPHA to"+ss.getOperatorAlphaLong());
                phone.setSystemProperty(TelephonyProperties.PROPERTY_OPERATOR_ALPHA, ss.getOperatorAlphaLong());
            } else if (mSimId == PhoneConstants.GEMINI_SIM_2){
                Log.d(LOG_TAG, "setOperatorAlphaLong and update PROPERTY_OPERATOR_ALPHA_2 to"+ss.getOperatorAlphaLong());
                phone.setSystemProperty(TelephonyProperties.PROPERTY_OPERATOR_ALPHA_2, ss.getOperatorAlphaLong());
            } else if (mSimId == PhoneConstants.GEMINI_SIM_3){
                Log.d(LOG_TAG, "setOperatorAlphaLong and update PROPERTY_OPERATOR_ALPHA_3 to"+ss.getOperatorAlphaLong());				
                phone.setSystemProperty(TelephonyProperties.PROPERTY_OPERATOR_ALPHA_3, ss.getOperatorAlphaLong());                
            } else if (mSimId == PhoneConstants.GEMINI_SIM_4){
                Log.d(LOG_TAG, "setOperatorAlphaLong and update PROPERTY_OPERATOR_ALPHA_4 to"+ss.getOperatorAlphaLong());				
                phone.setSystemProperty(TelephonyProperties.PROPERTY_OPERATOR_ALPHA_4, ss.getOperatorAlphaLong());                
            }
        }
        return 1;
    }

    public void refreshSpnDisplay() {
        String numeric = ss.getOperatorNumeric();
        String newAlphaLong = null;
        String newAlphaShort = null;
        boolean force = false;

        if (numeric != null) {
            newAlphaLong = cm.lookupOperatorName(numeric, true);
            newAlphaShort = cm.lookupOperatorName(numeric, false);
	     if (mSimId == PhoneConstants.GEMINI_SIM_1) {
                phone.setSystemProperty(TelephonyProperties.PROPERTY_OPERATOR_ALPHA, newAlphaLong);
            } else if(mSimId == PhoneConstants.GEMINI_SIM_2){
                phone.setSystemProperty(TelephonyProperties.PROPERTY_OPERATOR_ALPHA_2, newAlphaLong);
            } else if(mSimId == PhoneConstants.GEMINI_SIM_3){
                phone.setSystemProperty(TelephonyProperties.PROPERTY_OPERATOR_ALPHA_3, newAlphaLong);                
            } else if(mSimId == PhoneConstants.GEMINI_SIM_4){
                phone.setSystemProperty(TelephonyProperties.PROPERTY_OPERATOR_ALPHA_4, newAlphaLong);                
            }
        } else {
            force = true;
        }

        Log.d(LOG_TAG, "refreshSpnDisplay set mSimId=" +mSimId+","+newAlphaLong +","+newAlphaShort+","+numeric);

        ss.setOperatorName(newAlphaLong, newAlphaShort, numeric);
        updateSpnDisplay(force);
    }

    protected void updateSpnDisplay() {
        updateSpnDisplay(false);
    }

    protected void updateSpnDisplay(boolean forceUpdate) {
        //SIMRecords simRecords = (SIMRecords)(phone.mIccRecords.get());
        SIMRecords simRecords = null;
    	  IccRecords r = phone.mIccRecords.get();
    	  if (r != null) {
            simRecords = (SIMRecords)r;
    	  }

        //int rule = simRecords.getDisplayRule(ss.getOperatorNumeric());
        int rule = (simRecords != null) ? simRecords.getDisplayRule(ss.getOperatorNumeric()) : SIMRecords.SPN_RULE_SHOW_PLMN;

        //int rule = SIMRecords.SPN_RULE_SHOW_PLMN;
        String strNumPlmn = ss.getOperatorNumeric();
        //String spn = simRecords.getServiceProviderName();
        String spn = (simRecords != null) ? simRecords.getServiceProviderName() : "";
        //String plmn = ss.getOperatorAlphaLong();
        String sEons = null;

        //MTK-START [ALPS415367]For MR1 Migration
        boolean showPlmn = false;
        //MTK-END [ALPS415367]For MR1 Migration

        try {
            //sEons = simRecords.getEonsIfExist(ss.getOperatorNumeric(), cellLoc.getLac(), true);
            sEons = (simRecords != null) ? simRecords.getEonsIfExist(ss.getOperatorNumeric(), cellLoc.getLac(), true) : null;
        } catch (RuntimeException ex) {
            Log.e(LOG_TAG, "Exception while getEonsIfExist. ", ex);
        }

        String plmn = null;

        //MTK-START [ALPS415367]For MR1 Migration
        String mSimOperatorNumeric = (simRecords != null) ? simRecords.getSIMOperatorNumeric() : "";
        //MTK-END [ALPS415367]For MR1 Migration

        if(sEons != null) {
            plmn = sEons;
        }
        else if (strNumPlmn != null && strNumPlmn.equals(mSimOperatorNumeric)){
	     Log.d(LOG_TAG, "Home PLMN, get CPHS ons");
	     //plmn = simRecords.getSIMCPHSOns();
	     plmn = (simRecords != null) ? simRecords.getSIMCPHSOns() : "";
        }

        if (plmn == null || plmn.equals("")) {
	     Log.d(LOG_TAG, "No matched EONS and No CPHS ONS");
            plmn = ss.getOperatorAlphaLong();
            if (plmn == null || plmn.equals(ss.getOperatorNumeric())) {
                plmn = ss.getOperatorAlphaShort();
            }
        }
        updateAllOpertorInfo(plmn);

        // Do not display SPN before get normal service
        if (ss.getState() != ServiceState.STATE_IN_SERVICE) {
            //MTK-START [ALPS415367]For MR1 Migration
            showPlmn = true;
            //rule = SIMRecords.SPN_RULE_SHOW_PLMN;
            //plmn = null;
            //MTK-END [ALPS415367]For MR1 Migration
            plmn = Resources.getSystem().
                    getText(com.android.internal.R.string.lockscreen_carrier_default).toString();

        }

        log("updateSpnDisplay mEmergencyOnly="+mEmergencyOnly+"getRadioState="+cm.getRadioState().isOn());

        // ALPS00283717 For emergency calls only, pass the EmergencyCallsOnly string via EXTRA_PLMN
        if (mEmergencyOnly && cm.getRadioState().isOn()) {
            log("updateSpnDisplay show mEmergencyOnly");

            //MTK-START [ALPS415367]For MR1 Migration
            showPlmn = true;
            //rule = SIMRecords.SPN_RULE_SHOW_PLMN;
            //MTK-END [ALPS415367]For MR1 Migration
            plmn = Resources.getSystem().
                    getText(com.android.internal.R.string.emergency_calls_only).toString();
        }
		
        //MTK-START [ALPS00446163] remark
        //MTK-START [ALPS415367]For MR1 Migration		
        //if (ss.getState() == ServiceState.STATE_POWER_OFF) {
        //    plmn = null;
        //}
		//MTK-END [ALPS415367]For MR1 Migration

        /**
        * mImeiAbnormal=0, Valid IMEI
        * mImeiAbnormal=1, IMEI is null or not valid format
        * mImeiAbnormal=2, Phone1/Phone2 have same IMEI
        */
        int imeiAbnormal = phone.isDeviceIdAbnormal();
        if (imeiAbnormal == 1) {
            plmn = Resources.getSystem().getText(com.mediatek.R.string.invalid_imei).toString();
            //MTK-START [ALPS415367]For MR1 Migration
            //rule = SIMRecords.SPN_RULE_SHOW_PLMN;
            //MTK-END [ALPS415367]For MR1 Migration
        } else if (imeiAbnormal == 2) {
            plmn = Resources.getSystem().getText(com.mediatek.R.string.same_imei).toString();
            //MTK-START [ALPS415367]For MR1 Migration
            //rule = SIMRecords.SPN_RULE_SHOW_PLMN;
            //MTK-END [ALPS415367]For MR1 Migration
        } else if (imeiAbnormal == 0) {
            plmn = mServiceStateExt.onUpdateSpnDisplay(plmn, mRilRadioTechnology);
        }

        /* ALPS00296298 */
        if (mIsImeiLock){
            plmn = Resources.getSystem().getText(com.mediatek.R.string.invalid_card).toString();
            //MTK-START [ALPS415367]For MR1 Migration
            //plmn = new String("Invalid Card");
            //rule = SIMRecords.SPN_RULE_SHOW_PLMN;
            //MTK-END [ALPS415367]For MR1 Migration
        }

        //MTK-START [ALPS415367]For MR1 Migration
        if (ss.getState() == ServiceState.STATE_IN_SERVICE) {
            showPlmn = !TextUtils.isEmpty(plmn) &&
                    ((rule & SIMRecords.SPN_RULE_SHOW_PLMN)
                            == SIMRecords.SPN_RULE_SHOW_PLMN);
        //MTK-START [ALPS00446163] remark
        //} else if (ss.getState() == ServiceState.STATE_POWER_OFF) {
        //    showPlmn = false;			
        }

        boolean showSpn = !TextUtils.isEmpty(spn)
                && ((rule & SIMRecords.SPN_RULE_SHOW_SPN)
                        == SIMRecords.SPN_RULE_SHOW_SPN);

        //[ALPS00446315]MTK add - START
        if (ss.getState() == ServiceState.STATE_POWER_OFF) {
            showSpn = false;
            spn = null;
        }
        //[ALPS00446315]MTK add - END

        //MTK-END [ALPS415367]For MR1 Migration

        if (showPlmn != curShowPlmn
                || showSpn != curShowSpn
                || !TextUtils.equals(spn, curSpn)
                || !TextUtils.equals(plmn, curPlmn)
                || forceUpdate) {

            //MTK-START [ALPS415367]For MR1 Migration
            //boolean showSpn =
            //    (rule & SIMRecords.SPN_RULE_SHOW_SPN) == SIMRecords.SPN_RULE_SHOW_SPN;
            //boolean showPlmn =  (mEmergencyOnly ||
            //    ((rule & SIMRecords.SPN_RULE_SHOW_PLMN) == SIMRecords.SPN_RULE_SHOW_PLMN));
            //MTK-END [ALPS415367]For MR1 Migration

            Intent intent = new Intent(Intents.SPN_STRINGS_UPDATED_ACTION);
            intent.putExtra(PhoneConstants.GEMINI_SIM_ID_KEY, mSimId);

            // [ALPS00125833]
            // For Gemini, share the same intent, do not replace the other one
            if (!FeatureOption.MTK_GEMINI_SUPPORT) {
                intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
            }

            intent.putExtra(Intents.EXTRA_SHOW_SPN, showSpn);
            intent.putExtra(Intents.EXTRA_SPN, spn);
            intent.putExtra(Intents.EXTRA_SHOW_PLMN, showPlmn);
            intent.putExtra(Intents.EXTRA_PLMN, plmn);
            phone.getContext().sendStickyBroadcast(intent);

            /* ALPS00357573 for consistent operator name display */
            if((showSpn == true) && (showPlmn == false) &&(spn!=null)){
                /* When only <spn> is shown , we update with <spn> */
                log("updateAllOpertorInfo with spn");
                updateAllOpertorInfo(spn);
            }

            log(" showSpn:" + showSpn +
                    " spn:" + spn +
                    " showPlmn:" + showPlmn +
                    " plmn:" + plmn +
                    " rule:" + rule );
        }

        //MTK-START [ALPS415367]For MR1 Migration
        //curSpnRule = rule;
        curShowSpn = showSpn;
        //MTK-END [ALPS415367]For MR1 Migration
        curShowPlmn = showPlmn;
        curSpn = spn;
        curPlmn = plmn;
    }

    void registerForPsRegistrants(Handler h, int what, Object obj) {
        Log.d(LOG_TAG, "[DSAC DEB] " + "registerForCsRegistrants");
        Registrant r = new Registrant(h, what, obj);
        ratPsChangedRegistrants.add(r);
    }

    void unregisterForPsRegistrants(Handler h) {
    	ratPsChangedRegistrants.remove(h);
    }

    void registerForRatRegistrants(Handler h, int what, Object obj) {
        Log.d(LOG_TAG, "[DSAC DEB] " + "registerForRatRegistrants");
        Registrant r = new Registrant(h, what, obj);
        ratCsChangedRegistrants.add(r);
    }

    void unregisterForRatRegistrants(Handler h) {
    	ratCsChangedRegistrants.remove(h);
    }

    //ALPS00248788
    private void onInvalidSimInfoReceived(AsyncResult ar) {
        String[] InvalidSimInfo = (String[]) ar.result;
        String plmn = InvalidSimInfo[0];
        int cs_invalid = Integer.parseInt(InvalidSimInfo[1]);
        int ps_invalid = Integer.parseInt(InvalidSimInfo[2]);
        int cause = Integer.parseInt(InvalidSimInfo[3]);
        int testMode = -1;
        long ivsr_setting = Settings.System.getLong(phone.getContext().getContentResolver(),
			                                        Settings.System.IVSR_SETTING,Settings.System.IVSR_SETTING_DISABLE);

        log("InvalidSimInfo received during ivsr_setting: "+ ivsr_setting);

        // do NOT apply IVSR when EM IVSR setting is disabled
        if(ivsr_setting == Settings.System.IVSR_SETTING_DISABLE)
        {
            return;
        }

        // do NOT apply IVSR when in TEST mode
        testMode = SystemProperties.getInt("gsm.gcf.testmode", 0);
        // there is only one test mode in modem. actually it's not SIM dependent , so remove testmode2 property here

        log("onInvalidSimInfoReceived testMode:" + testMode+" cause:"+cause+" cs_invalid:"+cs_invalid+" ps_invalid:"+ps_invalid+" plmn:"+plmn+"mEverIVSR"+mEverIVSR);

        //Check UE is set to test mode or not	(CTA =1,FTA =2 , IOT=3 ...)
        if(testMode != 0){
            log("InvalidSimInfo received during test mode: "+ testMode);
            return;
        }

        /* check if CS domain ever sucessfully registered to the invalid SIM PLMN */
        if((cs_invalid == 1)&& (mLastRegisteredPLMN != null) && (plmn.equals(mLastRegisteredPLMN)))
        {
            log("InvalidSimInfo set TRM due to CS invalid");
            setEverIVSR(true);
            mLastRegisteredPLMN = null;
            mLastPSRegisteredPLMN = null;
            phone.setTRM(3, null);
            return;
        }

        /* check if PS domain ever sucessfully registered to the invalid SIM PLMN */
        if((ps_invalid == 1)&& (mLastPSRegisteredPLMN != null) && (plmn.equals(mLastPSRegisteredPLMN)))
        {
            log("InvalidSimInfo set TRM due to PS invalid ");
            setEverIVSR(true);
            mLastRegisteredPLMN = null;
            mLastPSRegisteredPLMN = null;
            phone.setTRM(3, null);
            return;
        }

        /* ALPS00324111: to force trigger IVSR */
        /* ALPS00407923  : The following code is to "Force trigger IVSR even when MS never register to the network before" 
                  The code was intended to cover the scenario of "invalid SIM NW issue happen at the first network registeration during boot-up". 
                  However, it might cause false alarm IVSR ex: certain sim card only register CS domain network , but PS domain is invalid. 
                  For such sim card, MS will receive invalid SIM at the first PS domain network registeration
                  In such case , to trigger IVSR will be a false alarm,which will cause CS domain network registeration time longer (due to IVSR impact)
                  It's a tradeoff. Please think about the false alarm impact before using the code below.*/
    /*
        if ((mEverIVSR == false) && (gprsState != ServiceState.STATE_IN_SERVICE) &&(ss.getState() != ServiceState.STATE_IN_SERVICE))
        {
            log("InvalidSimInfo set TRM due to never set IVSR");
            setEverIVSR(true);
            mLastRegisteredPLMN = null;
            mLastPSRegisteredPLMN = null;
            phone.setTRM(3, null);
            return;
        }
        */	

    }

    public void removeGprsConnTypeRetry() {
        removeMessages(EVENT_SET_GPRS_CONN_RETRY);
    }
//MTK-END [mtk03851][111124]
    //MTK-START [MTK80515] [ALPS00368272]
    private void getEINFO(int eventId) {
        phone.invokeOemRilRequestStrings(new String[]{"AT+EINFO?","+EINFO"}, this.obtainMessage(eventId));
        log("getEINFO for EMMRRS");
    }

    private void setEINFO(int value, Message onComplete) {
        String Cmd[] = new String[2];
        Cmd[0] = "AT+EINFO=" + value;
        Cmd[1] = "+EINFO";
        phone.invokeOemRilRequestStrings(Cmd, onComplete);
        log("setEINFO for EMMRRS, ATCmd[0]="+Cmd[0]);
    }

    private int getDataConnectionSimId() {
        int currentDataConnectionSimId = -1;
        if (FeatureOption.MTK_GEMINI_ENHANCEMENT == true) {
            long currentDataConnectionMultiSimId =  Settings.System.getLong(phone.getContext().getContentResolver(), Settings.System.GPRS_CONNECTION_SIM_SETTING, Settings.System.DEFAULT_SIM_NOT_SET);
            if (currentDataConnectionMultiSimId != Settings.System.GPRS_CONNECTION_SIM_SETTING_NEVER && currentDataConnectionMultiSimId != Settings.System.DEFAULT_SIM_NOT_SET) {
                currentDataConnectionSimId = SIMInfo.getSlotById(phone.getContext(), currentDataConnectionMultiSimId);
            }
        }else {
            currentDataConnectionSimId =  Settings.System.getInt(phone.getContext().getContentResolver(), Settings.System.GPRS_CONNECTION_SETTING, Settings.System.GPRS_CONNECTION_SETTING_DEFAULT) - 1;
        }
        log("Default Data Setting value=" + currentDataConnectionSimId);
        return currentDataConnectionSimId;
    }
    //MTK-END [MTK80515] [ALPS00368272]
}
