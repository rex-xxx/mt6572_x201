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

package android.net;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo.DetailedState;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Slog;

import com.android.internal.telephony.DctConstants;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.TelephonyProperties;
import com.android.internal.util.AsyncChannel;
import com.mediatek.common.featureoption.FeatureOption;
import java.io.CharArrayWriter;
import java.io.PrintWriter;

import java.util.HashMap;
import java.util.Collection;
import java.util.Iterator;

/**
 * Track the state of mobile data connectivity. This is done by
 * receiving broadcast intents from the Phone process whenever
 * the state of data connectivity changes.
 *
 * {@hide}
 */
public class MobileDataStateTracker implements NetworkStateTracker {

    private static final String TAG = "MobileDataStateTracker";
    private static final boolean DBG = true;
    private static final boolean VDBG = true;

    private PhoneConstants.DataState mMobileDataState;
    private ITelephony mPhoneService;

    private String mApnType;
    private NetworkInfo mNetworkInfo;
    private boolean mTeardownRequested = false;
    private Handler mTarget;
    private Context mContext;
    private LinkProperties mLinkProperties;
    private LinkCapabilities mLinkCapabilities;
    private boolean mPrivateDnsRouteSet = false;
    private boolean mDefaultRouteSet = false;

    // NOTE: these are only kept for debugging output; actual values are
    // maintained in DataConnectionTracker.
    protected boolean mUserDataEnabled = true;
    protected boolean mPolicyDataEnabled = true;

    private Handler mHandler;
    private AsyncChannel mDataConnectionTrackerAc;
    private Messenger mMessenger;

    private static MobileDataStateReceiver mMobileDataStateReceiver;//use the static instance to reduce receiver

    /**
     * Create a new MobileDataStateTracker
     * @param netType the ConnectivityManager network type
     * @param tag the name of this network
     */
    public MobileDataStateTracker(int netType, String tag) {
        mNetworkInfo = new NetworkInfo(netType,
                TelephonyManager.getDefault().getNetworkType(), tag,
                TelephonyManager.getDefault().getNetworkTypeName());
        mApnType = networkTypeToApnType(netType);
    }

    /**
     * Begin monitoring data connectivity.
     *
     * @param context is the current Android context
     * @param target is the Hander to which to return the events.
     */
    public void startMonitoring(Context context, Handler target) {
        mTarget = target;
        mContext = context;

        mHandler = new MdstHandler(target.getLooper(), this);

        if (mMobileDataStateReceiver == null) {
            mMobileDataStateReceiver = new MobileDataStateReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction(TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED_MOBILE);
            filter.addAction(TelephonyIntents.ACTION_DATA_CONNECTION_FAILED);
            filter.addAction(DctConstants.ACTION_DATA_CONNECTION_TRACKER_MESSENGER);
            mContext.registerReceiver(mMobileDataStateReceiver, filter);
        }
        mMobileDataStateReceiver.addTracker(mApnType, this);
        mMobileDataState = PhoneConstants.DataState.DISCONNECTED;
    }

    static class MdstHandler extends Handler {
        private MobileDataStateTracker mMdst;

        MdstHandler(Looper looper, MobileDataStateTracker mdst) {
            super(looper);
            mMdst = mdst;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case AsyncChannel.CMD_CHANNEL_HALF_CONNECTED:
                    if (msg.arg1 == AsyncChannel.STATUS_SUCCESSFUL) {
                        if (VDBG) {
                            mMdst.log("MdstHandler connected");
                        }
                        mMdst.mDataConnectionTrackerAc = (AsyncChannel) msg.obj;
                    } else {
                        if (VDBG) {
                            mMdst.log("MdstHandler %s NOT connected error=" + msg.arg1);
                        }
                    }
                    break;
                case AsyncChannel.CMD_CHANNEL_DISCONNECTED:
                    if (VDBG) mMdst.log("Disconnected from DataStateTracker");
                    mMdst.mDataConnectionTrackerAc = null;
                    break;
                default: {
                    if (VDBG) mMdst.log("Ignorning unknown message=" + msg);
                    break;
                }
            }
        }
    }

    public boolean isPrivateDnsRouteSet() {
        return mPrivateDnsRouteSet;
    }

    public void privateDnsRouteSet(boolean enabled) {
        mPrivateDnsRouteSet = enabled;
    }

    public NetworkInfo getNetworkInfo() {
        if (FeatureOption.MTK_GEMINI_SUPPORT) {
            getPhoneService(true);
            /*
             * If the phone process has crashed in the past, we'll get a
             * RemoteException and need to re-reference the service.
             */
            for (int retry = 0; retry < 2; retry++) {
                if (mPhoneService == null) {
                    loge("Ignoring feature request because could not acquire PhoneService");
                    break;
                }

                try {
                    /** M: update availabe info due to it may not correct 
                      * when we received from ACTION_ANY_DATA_CONNECTION_STATE_CHANGED 
                      */
                    mNetworkInfo.setIsAvailable(mPhoneService.isDataConnectivityPossible());
                    log("getNetworkInfo: updated IsAvailable=" + mNetworkInfo.isAvailable());
                } catch (RemoteException e) {
                    if (retry == 0) getPhoneService(true);
                }
            }
        }
        
        return mNetworkInfo;
    }

    public boolean isDefaultRouteSet() {
        return mDefaultRouteSet;
    }

    public void defaultRouteSet(boolean enabled) {
        mDefaultRouteSet = enabled;
    }

    /**
     * This is not implemented.
     */
    public void releaseWakeLock() {
    }

    private static class MobileDataStateReceiver extends BroadcastReceiver {
        private HashMap<String, MobileDataStateTracker> mTrackerMap = new HashMap<String, MobileDataStateTracker>();

        public void addTracker(String apnType, MobileDataStateTracker tracker) {
            Slog.e(TAG, "MobileDataStateReceiver add target tracker [" + apnType + "]");
            mTrackerMap.put(apnType, tracker);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED_MOBILE)) {
                String apnType = intent.getStringExtra(PhoneConstants.DATA_APN_TYPE_KEY);
                MobileDataStateTracker tracker = mTrackerMap.get(apnType);
                if (tracker == null) {
                    return;
                }

                int slot = 0;
                int curSlot = 0;
                if (FeatureOption.MTK_GEMINI_SUPPORT) {
                    slot = intent.getIntExtra(PhoneConstants.GEMINI_SIM_ID_KEY, PhoneConstants.GEMINI_SIM_1);
                    curSlot = tracker.mNetworkInfo.getSimId();
                }
               
                if (tracker.mMobileDataState == PhoneConstants.DataState.CONNECTED) {
                	if (slot != curSlot) {
                		tracker.log("Receive peer SIM data state.ignor!");
                		return;
                	}
                }
                
                if (VDBG)
                    Slog.d(TAG, "MobileDataStateReceiver received: ACTION_ANY_DATA_CONNECTION_STATE_CHANGED_MOBILE [" + apnType + "]");
                tracker.log("Intent from SIM " + slot + ", current SIM " + curSlot + ", current DataState " + tracker.mMobileDataState);
                
                int oldSubtype = tracker.mNetworkInfo.getSubtype();
                int newSubType = TelephonyManager.NETWORK_TYPE_UNKNOWN;
                String subTypeName;
                if (FeatureOption.MTK_GEMINI_SUPPORT) {
                    newSubType = TelephonyManager.getDefault().getNetworkTypeGemini(tracker.mNetworkInfo.getSimId());
                    subTypeName = TelephonyManager.getDefault().getNetworkTypeNameGemini(tracker.mNetworkInfo.getSimId());
                } else {
                    newSubType = TelephonyManager.getDefault().getNetworkType();
                    subTypeName = TelephonyManager.getDefault().getNetworkTypeName();
                }
				
                tracker.mNetworkInfo.setSubtype(newSubType, subTypeName);
                if (newSubType != oldSubtype && tracker.mNetworkInfo.isConnected()) {
                    Message msg = tracker.mTarget.obtainMessage(EVENT_NETWORK_SUBTYPE_CHANGED,
                                                        oldSubtype, 0, tracker.mNetworkInfo);
                    msg.sendToTarget();
                }
                
                PhoneConstants.DataState state = Enum.valueOf(PhoneConstants.DataState.class,
                        intent.getStringExtra(PhoneConstants.STATE_KEY));
                String reason = intent.getStringExtra(PhoneConstants.STATE_CHANGE_REASON_KEY);
                String apnName = intent.getStringExtra(PhoneConstants.DATA_APN_KEY);
                tracker.mNetworkInfo.setRoaming(intent.getBooleanExtra(PhoneConstants.DATA_NETWORK_ROAMING_KEY, false));
                if (VDBG) {
                    tracker.log(tracker.mApnType + " setting isAvailable to " +
                            !intent.getBooleanExtra(PhoneConstants.NETWORK_UNAVAILABLE_KEY,false));
                }
               tracker.mNetworkInfo.setIsAvailable(!intent.getBooleanExtra(PhoneConstants.NETWORK_UNAVAILABLE_KEY, false));

                if (DBG) {
                    tracker.log("Received state=" + state + ", old=" + tracker.mMobileDataState +
                        ", reason=" + (reason == null ? "(unspecified)" : reason));
                }
                if (tracker.mMobileDataState != state) {
                    tracker.mMobileDataState = state;
                    switch (state) {
                        case DISCONNECTED:
                            if(tracker.isTeardownRequested()) {
                                tracker.setTeardownRequested(false);
                            }
                            if (FeatureOption.MTK_GEMINI_SUPPORT) {
                                tracker.setDetailedStateGemini(DetailedState.DISCONNECTED, reason, apnName, slot);
                            } else {
                                tracker.setDetailedState(DetailedState.DISCONNECTED, reason, apnName);
                            }
                            // can't do this here - ConnectivityService needs it to clear stuff
                            // it's ok though - just leave it to be refreshed next time
                            // we connect.
                            //if (DBG) log("clearing mInterfaceName for "+ mApnType +
                            //        " as it DISCONNECTED");
                            //mInterfaceName = null;
                            break;
                        case CONNECTING:
                            if (FeatureOption.MTK_GEMINI_SUPPORT) {
                                tracker.setDetailedStateGemini(DetailedState.CONNECTING, reason, apnName, slot);
                            } else {
                                tracker.setDetailedState(DetailedState.CONNECTING, reason, apnName);
                            }
                            break;
                        case SUSPENDED:
                            if (FeatureOption.MTK_GEMINI_SUPPORT) {
                                tracker.setDetailedStateGemini(DetailedState.SUSPENDED, reason, apnName, slot);
                            } else {
                                tracker.setDetailedState(DetailedState.SUSPENDED, reason, apnName);
                            }
                            break;
                        case CONNECTED:
                            tracker.mLinkProperties = intent.getParcelableExtra(
                                    PhoneConstants.DATA_LINK_PROPERTIES_KEY);
                            if (tracker.mLinkProperties == null) {
                                tracker.loge("CONNECTED event did not supply link properties.");
                                tracker.mLinkProperties = new LinkProperties();
                            }
                            tracker.mLinkCapabilities = intent.getParcelableExtra(
                                    PhoneConstants.DATA_LINK_CAPABILITIES_KEY);
                            if (tracker.mLinkCapabilities == null) {
                                tracker.loge("CONNECTED event did not supply link capabilities.");
                                tracker.mLinkCapabilities = new LinkCapabilities();
                            }
                            if (FeatureOption.MTK_GEMINI_SUPPORT) {
                                tracker.setDetailedStateGemini(DetailedState.CONNECTED, reason, apnName, slot);
                            } else {
                                tracker.setDetailedState(DetailedState.CONNECTED, reason, apnName);
                            }
                            
                            break;
                    }
                } else {
                    // There was no state change. Check if LinkProperties has been updated.
                    if (TextUtils.equals(reason, PhoneConstants.REASON_LINK_PROPERTIES_CHANGED)) {
                        tracker.mLinkProperties = intent.getParcelableExtra(PhoneConstants.DATA_LINK_PROPERTIES_KEY);
                        if (tracker.mLinkProperties == null) {
                            tracker.loge("No link property in LINK_PROPERTIES change event.");
                            tracker.mLinkProperties = new LinkProperties();
                        }
                        // Just update reason field in this NetworkInfo
                        tracker.mNetworkInfo.setDetailedState(tracker.mNetworkInfo.getDetailedState(), reason,
                                                      tracker.mNetworkInfo.getExtraInfo());
                        Message msg = tracker.mTarget.obtainMessage(EVENT_CONFIGURATION_CHANGED,
                                                            tracker.mNetworkInfo);
                        msg.sendToTarget();
                    }
                    
                    if (reason != null
                            && (reason.equals(Phone.REASON_APN_FAILED) 
                                    || reason.equals(PhoneConstants.REASON_NO_SUCH_PDP))
                            && apnType != null
                            && !apnType.equals(PhoneConstants.APN_TYPE_DEFAULT)) {
                        tracker.log("Handle PhoneConstants.REASON_APN_FAILED OR  PhoneConstants.REASON_NO_SUCH_PDP from GeminiDataSubUtil");
                        if (state == PhoneConstants.DataState.DISCONNECTED) {
                            if (tracker.isTeardownRequested()) {
                                tracker.setTeardownRequested(false);
                            }
                            if (FeatureOption.MTK_GEMINI_SUPPORT) {
                                tracker.setDetailedStateGemini(DetailedState.DISCONNECTED, reason, apnName, slot);
                            } else {
                                tracker.setDetailedState(DetailedState.DISCONNECTED, reason, apnName);
                            }
                        }
                    }
                }
            } else if (intent.getAction().equals(TelephonyIntents.ACTION_DATA_CONNECTION_FAILED)) {
                String apnType = intent.getStringExtra(PhoneConstants.DATA_APN_TYPE_KEY);
                MobileDataStateTracker tracker = mTrackerMap.get(apnType);
                if (tracker == null) {
                    return;
                }
                if (DBG)
                   Slog.d(TAG, "MobileDataStateReceiver received: ACTION_ANY_DATA_CONNECTION_FAILED ignore [" + apnType + "]");

                int slot = 0;
                String reason = intent.getStringExtra(PhoneConstants.FAILURE_REASON_KEY);
                String apnName = intent.getStringExtra(PhoneConstants.DATA_APN_KEY);
                if (DBG) {
                    tracker.log("Received " + intent.getAction() +
                                " broadcast" + reason == null ? "" : "(" + reason + ")");
                }
                if (FeatureOption.MTK_GEMINI_SUPPORT) {
                    slot = intent.getIntExtra(PhoneConstants.GEMINI_SIM_ID_KEY,PhoneConstants.GEMINI_SIM_1);
                    tracker.setDetailedStateGemini(DetailedState.FAILED, reason, apnName, slot);
                } else {
                    tracker.setDetailedState(DetailedState.FAILED, reason, apnName);
                }
            } else if (intent.getAction().equals(DctConstants.ACTION_DATA_CONNECTION_TRACKER_MESSENGER)) {
                if (VDBG) Slog.d(TAG, "MobileDataStateReceiver received: ACTION_DATA_CONNECTION_TRACKER_MESSENGER");
                Messenger messenger = intent.getParcelableExtra(DctConstants.EXTRA_MESSENGER);
                Collection<MobileDataStateTracker> collection = mTrackerMap.values();
                Iterator<MobileDataStateTracker> iter = collection.iterator();
                while (iter.hasNext()) {
                    MobileDataStateTracker tracker = iter.next();
                    tracker.mMessenger = messenger;
                    AsyncChannel ac = new AsyncChannel();
                    ac.connect(tracker.mContext, tracker.mHandler, tracker.mMessenger);
                }
            } else {
                if (DBG) Slog.d(TAG, "MobileDataStateReceiver received: ignore " + intent.getAction());
            }
        }
    }

    private void getPhoneService(boolean forceRefresh) {
        if ((mPhoneService == null) || forceRefresh) {
            mPhoneService = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
        }
    }

    /**
     * Report whether data connectivity is possible.
     */
    public boolean isAvailable() {
        return mNetworkInfo.isAvailable();
    }

    /**
     * Return the system properties name associated with the tcp buffer sizes
     * for this network.
     */
    public String getTcpBufferSizesPropName() {
        int netType;
        String networkTypeStr = "unknown";
        TelephonyManager tm = new TelephonyManager(mContext);
        //TODO We have to edit the parameter for getNetworkType regarding CDMA
        if (FeatureOption.MTK_GEMINI_SUPPORT != true)
        {
            netType = tm.getNetworkType();
        }
        else
        {
            netType = tm.getNetworkTypeGemini(mNetworkInfo.getSimId());
        }

        //TODO We have to edit the parameter for getNetworkType regarding CDMA
        switch(netType) {
        case TelephonyManager.NETWORK_TYPE_GPRS:
            networkTypeStr = "gprs";
            break;
        case TelephonyManager.NETWORK_TYPE_EDGE:
            networkTypeStr = "edge";
            break;
        case TelephonyManager.NETWORK_TYPE_UMTS:
            networkTypeStr = "umts";
            break;
        case TelephonyManager.NETWORK_TYPE_HSDPA:
            networkTypeStr = "hsdpa";
            break;
        case TelephonyManager.NETWORK_TYPE_HSUPA:
            networkTypeStr = "hsupa";
            break;
        case TelephonyManager.NETWORK_TYPE_HSPA:
            networkTypeStr = "hspa";
            break;
        case TelephonyManager.NETWORK_TYPE_HSPAP:
            networkTypeStr = "hspap";
            break;
        case TelephonyManager.NETWORK_TYPE_CDMA:
            networkTypeStr = "cdma";
            break;
        case TelephonyManager.NETWORK_TYPE_1xRTT:
            networkTypeStr = "1xrtt";
            break;
        case TelephonyManager.NETWORK_TYPE_EVDO_0:
            networkTypeStr = "evdo";
            break;
        case TelephonyManager.NETWORK_TYPE_EVDO_A:
            networkTypeStr = "evdo";
            break;
        case TelephonyManager.NETWORK_TYPE_EVDO_B:
            networkTypeStr = "evdo";
            break;
        case TelephonyManager.NETWORK_TYPE_IDEN:
            networkTypeStr = "iden";
            break;
        case TelephonyManager.NETWORK_TYPE_LTE:
            networkTypeStr = "lte";
            break;
        case TelephonyManager.NETWORK_TYPE_EHRPD:
            networkTypeStr = "ehrpd";
            break;
        default:
            loge("unknown network type: " + tm.getNetworkType());
        }
        return "net.tcp.buffersize." + networkTypeStr;
    }

    /**
     * Tear down mobile data connectivity, i.e., disable the ability to create
     * mobile data connections.
     * TODO - make async and return nothing?
     */
    public boolean teardown() {
        setTeardownRequested(true);
        return (setEnableApn(mApnType, false) != PhoneConstants.APN_REQUEST_FAILED);
    }

    @Override
    public void captivePortalCheckComplete() {
        // not implemented
    }

    /**
     * Record the detailed state of a network, and if it is a
     * change from the previous state, send a notification to
     * any listeners.
     * @param state the new {@code DetailedState}
     * @param reason a {@code String} indicating a reason for the state change,
     * if one was supplied. May be {@code null}.
     * @param extraInfo optional {@code String} providing extra information about the state change
     */
    private void setDetailedState(NetworkInfo.DetailedState state, String reason,
            String extraInfo) {
        if (DBG) log("setDetailed state, old ="
                + mNetworkInfo.getDetailedState() + " and new state=" + state);
        if (state != mNetworkInfo.getDetailedState()) {
            boolean wasConnecting = (mNetworkInfo.getState() == NetworkInfo.State.CONNECTING);
            String lastReason = mNetworkInfo.getReason();
            /*
             * If a reason was supplied when the CONNECTING state was entered, and no
             * reason was supplied for entering the CONNECTED state, then retain the
             * reason that was supplied when going to CONNECTING.
             */
            if (wasConnecting && state == NetworkInfo.DetailedState.CONNECTED && reason == null
                    && lastReason != null)
                reason = lastReason;
            mNetworkInfo.setDetailedState(state, reason, extraInfo);
            Message msg = mTarget.obtainMessage(EVENT_STATE_CHANGED, new NetworkInfo(mNetworkInfo));
            msg.sendToTarget();
        } else if(reason != null && (reason.equals(PhoneConstants.REASON_NO_SUCH_PDP) || reason.equals(Phone.REASON_APN_FAILED)) && state == DetailedState.DISCONNECTED){            
            mNetworkInfo.setDetailedState(state, reason, extraInfo);
            Message msg = mTarget.obtainMessage(EVENT_STATE_CHANGED, mNetworkInfo);
            msg.sendToTarget();
        }
    }

    public void setTeardownRequested(boolean isRequested) {
        mTeardownRequested = isRequested;
    }

    public boolean isTeardownRequested() {
        return mTeardownRequested;
    }

    /**
     * Re-enable mobile data connectivity after a {@link #teardown()}.
     * TODO - make async and always get a notification?
     */
    public boolean reconnect() {
        boolean retValue = false; //connected or expect to be?
        
        setTeardownRequested(false);
        switch (setEnableApn(mApnType, true)) {
            case PhoneConstants.APN_ALREADY_ACTIVE:
                // need to set self to CONNECTING so the below message is handled.
                //TODO: need to notify the app
                retValue = true;
                break;
            case PhoneConstants.APN_REQUEST_STARTED:
                // set IDLE here , avoid the following second FAILED not sent out
                if (!mNetworkInfo.isConnectedOrConnecting()) {
                    mNetworkInfo.setDetailedState(DetailedState.IDLE, null, null);
                }
                retValue = true;
                break;
            case PhoneConstants.APN_REQUEST_FAILED:
            case PhoneConstants.APN_TYPE_NOT_AVAILABLE:
                break;
            default:
                loge("Error in reconnect - unexpected response.");
                break;
        }
        return retValue;
    }

    /**
     * Turn on or off the mobile radio. No connectivity will be possible while the
     * radio is off. The operation is a no-op if the radio is already in the desired state.
     * @param turnOn {@code true} if the radio should be turned on, {@code false} if
     */
    public boolean setRadio(boolean turnOn) {
        getPhoneService(false);
        /*
         * If the phone process has crashed in the past, we'll get a
         * RemoteException and need to re-reference the service.
         */
        for (int retry = 0; retry < 2; retry++) {
            if (mPhoneService == null) {
                loge("Ignoring mobile radio request because could not acquire PhoneService");
                break;
            }

            try {
                return mPhoneService.setRadio(turnOn);
            } catch (RemoteException e) {
                if (retry == 0) getPhoneService(true);
            }
        }

        loge("Could not set radio power to " + (turnOn ? "on" : "off"));
        return false;
    }

    @Override
    public void setUserDataEnable(boolean enabled) {
        if (DBG) log("setUserDataEnable: E enabled=" + enabled);
        final AsyncChannel channel = mDataConnectionTrackerAc;
        if (channel != null) {
            channel.sendMessage(DctConstants.CMD_SET_USER_DATA_ENABLE,
                    enabled ? DctConstants.ENABLED : DctConstants.DISABLED);
            mUserDataEnabled = enabled;
        }
        if (VDBG) log("setUserDataEnable: X enabled=" + enabled);
    }

    @Override
    public void setPolicyDataEnable(boolean enabled) {
        if (DBG) log("setPolicyDataEnable(enabled=" + enabled + ")");
        final AsyncChannel channel = mDataConnectionTrackerAc;
        if (channel != null) {
            channel.sendMessage(DctConstants.CMD_SET_POLICY_DATA_ENABLE,
                    enabled ? DctConstants.ENABLED : DctConstants.DISABLED);
            mPolicyDataEnabled = enabled;
        }
    }

    /**
     * carrier dependency is met/unmet
     * @param met
     */
    public void setDependencyMet(boolean met) {
        Bundle bundle = Bundle.forPair(DctConstants.APN_TYPE_KEY, mApnType);
        try {
            if (DBG) log("setDependencyMet: E met=" + met);
            Message msg = Message.obtain();
            msg.what = DctConstants.CMD_SET_DEPENDENCY_MET;
            msg.arg1 = (met ? DctConstants.ENABLED : DctConstants.DISABLED);
            msg.setData(bundle);
            mDataConnectionTrackerAc.sendMessage(msg);
            if (VDBG) log("setDependencyMet: X met=" + met);
        } catch (NullPointerException e) {
            loge("setDependencyMet: X mAc was null" + e);
        }
    }

    @Override
    public String toString() {
        final CharArrayWriter writer = new CharArrayWriter();
        final PrintWriter pw = new PrintWriter(writer);
        pw.print("Mobile data state: "); pw.println(mMobileDataState);
        pw.print("Data enabled: user="); pw.print(mUserDataEnabled);
        pw.print(", policy="); pw.println(mPolicyDataEnabled);
        return writer.toString();
    }

   /**
     * Internal method supporting the ENABLE_MMS feature.
     * @param apnType the type of APN to be enabled or disabled (e.g., mms)
     * @param enable {@code true} to enable the specified APN type,
     * {@code false} to disable it.
     * @return an integer value representing the outcome of the request.
     */
    private int setEnableApn(String apnType, boolean enable) {
        getPhoneService(false);
        /*
         * If the phone process has crashed in the past, we'll get a
         * RemoteException and need to re-reference the service.
         */
        for (int retry = 0; retry < 2; retry++) {
            if (mPhoneService == null) {
                loge("Ignoring feature request because could not acquire PhoneService");
                break;
            }

            try {
                if (enable) {
                    return mPhoneService.enableApnType(apnType);
                } else {
                    return mPhoneService.disableApnType(apnType);
                }
            } catch (RemoteException e) {
                if (retry == 0) getPhoneService(true);
            }
        }

        loge("Could not " + (enable ? "enable" : "disable") + " APN type \"" + apnType + "\"");
        return PhoneConstants.APN_REQUEST_FAILED;
    }

    public static String networkTypeToApnType(int netType) {
        switch(netType) {
            case ConnectivityManager.TYPE_MOBILE:
                return PhoneConstants.APN_TYPE_DEFAULT;  // TODO - use just one of these
            case ConnectivityManager.TYPE_MOBILE_MMS:
                return PhoneConstants.APN_TYPE_MMS;
            case ConnectivityManager.TYPE_MOBILE_SUPL:
                return PhoneConstants.APN_TYPE_SUPL;
            case ConnectivityManager.TYPE_MOBILE_DUN:
                return PhoneConstants.APN_TYPE_DUN;
            case ConnectivityManager.TYPE_MOBILE_HIPRI:
                return PhoneConstants.APN_TYPE_HIPRI;
            case ConnectivityManager.TYPE_MOBILE_FOTA:
                return PhoneConstants.APN_TYPE_FOTA;
            case ConnectivityManager.TYPE_MOBILE_IMS:
                return PhoneConstants.APN_TYPE_IMS;
            case ConnectivityManager.TYPE_MOBILE_CBS:
                return PhoneConstants.APN_TYPE_CBS;
            case ConnectivityManager.TYPE_MOBILE_DM:
                return PhoneConstants.APN_TYPE_DM;
            case ConnectivityManager.TYPE_MOBILE_NET:
                return PhoneConstants.APN_TYPE_NET;
            case ConnectivityManager.TYPE_MOBILE_WAP:
                return PhoneConstants.APN_TYPE_WAP;
            case ConnectivityManager.TYPE_MOBILE_CMMAIL:
                return PhoneConstants.APN_TYPE_CMMAIL;
            case ConnectivityManager.TYPE_MOBILE_RCSE:
                return PhoneConstants.APN_TYPE_RCSE;
            default:
                sloge("Error mapping networkType " + netType + " to apnType.");
                return null;
        }
    }

    /**
     * @see android.net.NetworkStateTracker#getLinkProperties()
     */
    public LinkProperties getLinkProperties() {
        return new LinkProperties(mLinkProperties);
    }

    /**
     * @see android.net.NetworkStateTracker#getLinkCapabilities()
     */
    public LinkCapabilities getLinkCapabilities() {
        return new LinkCapabilities(mLinkCapabilities);
    }


     /**
     * check whether phone is idle or not
     */
    public boolean isIdle(){
        getPhoneService(false);
        
        for (int retry = 0; retry < 2; retry++) {
            if (mPhoneService == null) break;

            try {
                return mPhoneService.isVoiceIdle();
            } catch (RemoteException e) {
                // First-time failed, get the phone service again
                if (retry == 0) getPhoneService(true);
            }
        }
        return false;
    }
    
    private void log(String s) {
        Slog.d(TAG, mApnType + ": " + s);
    }

    private void loge(String s) {
        Slog.e(TAG, mApnType + ": " + s);
    }

    static private void sloge(String s) {
        Slog.e(TAG, s);
    }
    
    /** M:proprietary methods {@ */
     /**
     * Tear down mobile data connectivity, i.e., disable the ability to create
     * mobile data connections.
     * @param radioNum 0:sim1, 1:sim2
     */
    public boolean teardownGemini(int radioNum) {
        setTeardownRequested(true);
        return (setEnableApnGemini(mApnType, false, radioNum) != PhoneConstants.APN_REQUEST_FAILED);
    }

    /**
     * Re-enable mobile data connectivity after a {@link #teardown()}.
     * @param radioNum 0:sim1, 1:sim2
     */
    public boolean reconnectGemini(int radioNum) {
        boolean retValue = false; //false: failed, true: network is connecting or connected

        // reset teardown flag
        setTeardownRequested(false);
        switch (setEnableApnGemini(mApnType, true, radioNum)) {
            case PhoneConstants.APN_ALREADY_ACTIVE:
                // need to set self to CONNECTING so the below message is handled.
                //TODO: need to notify the app
                retValue = true;
                break;
            case PhoneConstants.APN_REQUEST_STARTED:
                // set IDLE here , avoid the following second FAILED not sent out
                if (!mNetworkInfo.isConnectedOrConnecting()) {
                    mNetworkInfo.setDetailedState(DetailedState.IDLE, null, null);
                }
                retValue = true;
                break;
            case PhoneConstants.APN_REQUEST_FAILED:
            case PhoneConstants.APN_TYPE_NOT_AVAILABLE:
                break;
            default:
                loge("Error in reconnect - unexpected response.");
                break;
        }
        return retValue;
    }

    /**
     * check whether phone is idle or not by sim id
     * @param radioNum 0:sim1, 1:sim2
     */
    public boolean isIdleGemini(int radioNum){
        getPhoneService(false);
        
        for (int retry = 0; retry < 2; retry++) {
            if (mPhoneService == null) {
                break;
            }

            try {
                return mPhoneService.isIdleGemini(radioNum);
            } catch (RemoteException e) {
                // First-time failed, get the phone service again
                if (retry == 0) getPhoneService(true);
            }
        }
        return false;
    }

    
   /**
     * enable or disable interface by apn type
     * @param apnType the type of APN to be enabled or disabled (e.g., mms)
     * @param enable {@code true} to enable the specified APN type,
     * {@code false} to disable it.
     * @param radioNum 0:sim1, 1:sim2
     * @return an integer value representing the outcome of the request.
     */
    private int setEnableApnGemini(String apnType, boolean enable, int radioNum) {
        getPhoneService(false);
        /*
         * If the phone process has crashed in the past, we'll get a
         * RemoteException and need to re-reference the service.
         */ 
        for (int retry = 0; retry < 2; retry++) {
            if (mPhoneService == null) {
                log("Ignoring feature request because could not acquire PhoneService");
                break;
            }

            try {
                if (enable) {
                    log("gemini before enableApnTypeGemini() and mApnType is " + mApnType + " ,radioNum is " + radioNum);               
                    return mPhoneService.enableApnTypeGemini(apnType,radioNum);
                } else {
                    return mPhoneService.disableApnTypeGemini(apnType,radioNum);
                }
            } catch (RemoteException e) {
                if (retry == 0) getPhoneService(true);
            }
        }

        log("Could not " + (enable ? "enable" : "disable")
                + " APN type \"" + apnType + "\"");
        return PhoneConstants.APN_REQUEST_FAILED;
    }

    /**
     * Record the detailed state of a network, and if it is a
     * change from the previous state, send a notification to
     * any listeners.
     * @param state the new @{code DetailedState}
     * @param reason a {@code String} indicating a reason for the state change,
     * if one was supplied. May be {@code null}.
     * @param extraInfo optional {@code String} providing extra information about the state change
     * @param simId 0 for sim1 and 1 for sim2
     */
    public void setDetailedStateGemini(NetworkInfo.DetailedState state, String reason, String extraInfo, int simId) {
        if (DBG) log( "setDetailed state, old ="+mNetworkInfo.getDetailedState()+" and new state="+state + " simId is " + simId);
        if (state != mNetworkInfo.getDetailedState()) {
            boolean wasConnecting = (mNetworkInfo.getState() == NetworkInfo.State.CONNECTING);
            String lastReason = mNetworkInfo.getReason();
            /*
             * If a reason was supplied when the CONNECTING state was entered, and no
             * reason was supplied for entering the CONNECTED state, then retain the
             * reason that was supplied when going to CONNECTING.
             */
            if (wasConnecting && state == NetworkInfo.DetailedState.CONNECTED && reason == null
                    && lastReason != null)
                reason = lastReason;
            mNetworkInfo.setDetailedStateGemini(state, reason, extraInfo, simId);
            Message msg = mTarget.obtainMessage(EVENT_STATE_CHANGED, mNetworkInfo);
            msg.sendToTarget();
        }else if(reason != null && (reason.equals(PhoneConstants.REASON_NO_SUCH_PDP) || reason.equals(Phone.REASON_APN_FAILED)) && state == DetailedState.DISCONNECTED){            
            mNetworkInfo.setDetailedStateGemini(state, reason, extraInfo, simId);            
            Message msg = mTarget.obtainMessage(EVENT_STATE_CHANGED, mNetworkInfo);
            msg.sendToTarget();
        }
    }   
}
/** @} */
