/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.internal.telephony;

import static android.Manifest.permission.READ_PHONE_STATE;
import android.app.ActivityManagerNative;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.util.Log;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;

import com.android.internal.telephony.IccCardConstants.State;
import com.android.internal.telephony.IccCardApplicationStatus.AppState;
import com.android.internal.telephony.IccCardApplicationStatus.PersoSubState;
import com.android.internal.telephony.IccCardStatus.CardState;
import com.android.internal.telephony.IccCardStatus.PinState;
import com.android.internal.telephony.cdma.CdmaSubscriptionSourceManager;
import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.gsm.UsimServiceTable;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.gemini.GeminiPhone;
import com.mediatek.common.featureoption.FeatureOption;

import static com.android.internal.telephony.TelephonyProperties.PROPERTY_SIM_STATE;
import static com.android.internal.telephony.TelephonyProperties.PROPERTY_SIM_STATE_2;
import static com.android.internal.telephony.TelephonyProperties.PROPERTY_SIM_STATE_3;
import static com.android.internal.telephony.TelephonyProperties.PROPERTY_SIM_STATE_4;


/**
 * @Deprecated use {@link UiccController}.getUiccCard instead.
 *
 * The Phone App assumes that there is only one icc card, and one icc application
 * available at a time. Moreover, it assumes such object (represented with IccCard)
 * is available all the time (whether {@link RILConstants.RIL_REQUEST_GET_SIM_STATUS} returned
 * or not, whether card has desired application or not, whether there really is a card in the
 * slot or not).
 *
 * UiccController, however, can handle multiple instances of icc objects (multiple
 * {@link UiccCardApplication}, multiple {@link IccFileHandler}, multiple {@link IccRecords})
 * created and destroyed dynamically during phone operation.
 *
 * This class implements the IccCard interface that is always available (right after default
 * phone object is constructed) to expose the current (based on voice radio technology)
 * application on the uicc card, so that external apps won't break.
 */

public class IccCardProxy extends Handler implements IccCard {

    private String[] ICCCARDPROXY_PROPERTY_SIM_STATE = {
         PROPERTY_SIM_STATE,
         PROPERTY_SIM_STATE_2,
         PROPERTY_SIM_STATE_3,
         PROPERTY_SIM_STATE_4
     };

    private static final boolean DBG = true;
    private static final String LOG_TAG = "RIL_IccCardProxy";

    private static final int EVENT_RADIO_OFF_OR_UNAVAILABLE = 1;
    private static final int EVENT_RADIO_ON = 2;
    private static final int EVENT_ICC_CHANGED = 3;
    private static final int EVENT_ICC_ABSENT = 4;
    private static final int EVENT_ICC_LOCKED = 5;
    private static final int EVENT_APP_READY = 6;
    private static final int EVENT_RECORDS_LOADED = 7;
    private static final int EVENT_IMSI_READY = 8;
    private static final int EVENT_NETWORK_LOCKED = 9;
    private static final int EVENT_CDMA_SUBSCRIPTION_SOURCE_CHANGED = 11;

    private static final int EVENT_ICC_RECOVERY = 100;

    private final Object mLock = new Object();
    private Context mContext;
    private CommandsInterface mCi;

    private RegistrantList mAbsentRegistrants = new RegistrantList();
    private RegistrantList mRecoveryRegistrants = new RegistrantList();
    private RegistrantList mPinLockedRegistrants = new RegistrantList();
    private RegistrantList mNetworkLockedRegistrants = new RegistrantList();

    private int mCurrentAppType = UiccController.APP_FAM_3GPP; //default to 3gpp?
    private UiccController mUiccController = null;
    private UiccCard mUiccCard = null;
    private UiccCardApplication mUiccApplication = null;
    private IccRecords mIccRecords = null;
    private CdmaSubscriptionSourceManager mCdmaSSM = null;
    private boolean mRadioOn = false;
    private boolean mQuietMode = false; // when set to true IccCardProxy will not broadcast
                                        // ACTION_SIM_STATE_CHANGED intents
    private boolean mInitialized = false;
    private State mExternalState = State.UNKNOWN;
    private int mNetworkLockState = -1;
    private int mSimId;
    private Phone mPhone;

    public IccCardProxy(Context context, CommandsInterface ci) {
        log("Creating");
        this.mContext = context;
        this.mCi = ci;
        mCdmaSSM = CdmaSubscriptionSourceManager.getInstance(context,
                ci, this, EVENT_CDMA_SUBSCRIPTION_SOURCE_CHANGED, null);
        mUiccController = UiccController.getInstance();
        mUiccController.registerForIccChanged(this, EVENT_ICC_CHANGED, null);
        mUiccController.registerForIccRecovery(this, EVENT_ICC_RECOVERY, null);
        ci.registerForOn(this,EVENT_RADIO_ON, null);
        ci.registerForOffOrNotAvailable(this, EVENT_RADIO_OFF_OR_UNAVAILABLE, null);
        setExternalState(State.NOT_READY);
    }

    public IccCardProxy(Context context, CommandsInterface ci, int simId) {
        log("Creating");
        this.mContext = context;
        this.mCi = ci;
        mSimId = simId;
        mCdmaSSM = CdmaSubscriptionSourceManager.getInstance(context,
                ci, this, EVENT_CDMA_SUBSCRIPTION_SOURCE_CHANGED, null);
        mUiccController = UiccController.getInstance(mSimId);
        mUiccController.registerForIccChanged(this, EVENT_ICC_CHANGED, null);
        mUiccController.registerForIccRecovery(this, EVENT_ICC_RECOVERY, null);
        ci.registerForOn(this,EVENT_RADIO_ON, null);
        ci.registerForOffOrNotAvailable(this, EVENT_RADIO_OFF_OR_UNAVAILABLE, null);
        setExternalState(State.NOT_READY);
    }

    public void dispose() {
        synchronized (mLock) {
            log("Disposing");
            //Cleanup icc references
            mUiccController.unregisterForIccChanged(this);
            mUiccController.unregisterForIccRecovery(this);
            mUiccController = null;
            mCi.unregisterForOn(this);
            mCi.unregisterForOffOrNotAvailable(this);
            mCdmaSSM.dispose(this);
        }
    }

    /*
     * The card application that the external world sees will be based on the
     * voice radio technology only!
     */
    public void setVoiceRadioTech(int radioTech) {
        synchronized (mLock) {
            if (DBG) {
                log("Setting radio tech " + ServiceState.rilRadioTechnologyToString(radioTech));
            }
            if (ServiceState.isGsm(radioTech)) {
                mCurrentAppType = UiccController.APP_FAM_3GPP;
            } else {
                mCurrentAppType = UiccController.APP_FAM_3GPP2;
            }
            updateQuietMode();
        }
    }

    /**
     * In case of 3gpp2 we need to find out if subscription used is coming from
     * NV in which case we shouldn't broadcast any sim states changes.
     */
    private void updateQuietMode() {
        synchronized (mLock) {
            boolean oldQuietMode = mQuietMode;
            boolean newQuietMode;
            int cdmaSource = Phone.CDMA_SUBSCRIPTION_UNKNOWN;
            boolean isLteOnCdmaMode = TelephonyManager.getLteOnCdmaModeStatic()
                    == PhoneConstants.LTE_ON_CDMA_TRUE;
            if (mCurrentAppType == UiccController.APP_FAM_3GPP) {
                newQuietMode = false;
                if (DBG) log("updateQuietMode: 3GPP subscription -> newQuietMode=" + newQuietMode);
            } else {
                if (isLteOnCdmaMode) {
                    log("updateQuietMode: is cdma/lte device, force IccCardProxy into 3gpp mode");
                    mCurrentAppType = UiccController.APP_FAM_3GPP;
                }
                cdmaSource = mCdmaSSM != null ?
                        mCdmaSSM.getCdmaSubscriptionSource() : Phone.CDMA_SUBSCRIPTION_UNKNOWN;

                newQuietMode = (cdmaSource == Phone.CDMA_SUBSCRIPTION_NV)
                        && (mCurrentAppType == UiccController.APP_FAM_3GPP2)
                        && !isLteOnCdmaMode;
            }

            if (mQuietMode == false && newQuietMode == true) {
                // Last thing to do before switching to quiet mode is
                // broadcast ICC_READY
                log("Switching to QuietMode.");
                setExternalState(State.READY);
                mQuietMode = newQuietMode;
            } else if (mQuietMode == true && newQuietMode == false) {
                if (DBG) {
                    log("updateQuietMode: Switching out from QuietMode."
                            + " Force broadcast of current state=" + mExternalState);
                }
                mQuietMode = newQuietMode;
                setExternalState(mExternalState, true);
            }
            if (DBG) {
                log("updateQuietMode: QuietMode is " + mQuietMode + " (app_type="
                    + mCurrentAppType + " isLteOnCdmaMode=" + isLteOnCdmaMode
                    + " cdmaSource=" + cdmaSource + ")");
            }
            mInitialized = true;
            sendMessage(obtainMessage(EVENT_ICC_CHANGED));
        }
    }

    public void handleMessage(Message msg) {
        AsyncResult ar;
        Log.d("IccCardProxy", "receive message " + msg.what );

        switch (msg.what) {
            case EVENT_RADIO_OFF_OR_UNAVAILABLE:
                log("handleMessage (EVENT_RADIO_OFF_OR_UNAVAILABLE)");
                mRadioOn = false;
                setExternalState(State.NOT_READY);
                if (mExternalState != null && mExternalState != State.UNKNOWN && mExternalState != State.NOT_READY){
                    broadcastRadioOffIntent();
                }
                break;
            case EVENT_RADIO_ON:
                mRadioOn = true;
                if (!mInitialized) {
                    updateQuietMode();
                }
                break;
            case EVENT_ICC_CHANGED:
                if (mInitialized) {
                    updateIccAvailability();
                }
                break;
            case EVENT_ICC_RECOVERY:
                mRecoveryRegistrants.notifyRegistrants();
                break;
            case EVENT_ICC_ABSENT:
                mAbsentRegistrants.notifyRegistrants();
                setExternalState(State.ABSENT);
                break;
            case EVENT_ICC_LOCKED:
                processLockedState();
                break;
            case EVENT_APP_READY:
                setExternalState(State.READY);
                break;
            case EVENT_RECORDS_LOADED:
                broadcastIccStateChangedIntent(IccCardConstants.INTENT_VALUE_ICC_LOADED, null);
                broadcastIccStateChangedExtendIntent(IccCardConstants.INTENT_VALUE_ICC_LOADED, null);
                break;
            case EVENT_IMSI_READY:
                broadcastIccStateChangedIntent(IccCardConstants.INTENT_VALUE_ICC_IMSI, null);
                broadcastIccStateChangedExtendIntent(IccCardConstants.INTENT_VALUE_ICC_IMSI, null);
                break;
            case EVENT_NETWORK_LOCKED:
                mNetworkLockedRegistrants.notifyRegistrants();
                setExternalState(State.NETWORK_LOCKED);
                break;
            case EVENT_CDMA_SUBSCRIPTION_SOURCE_CHANGED:
                updateQuietMode();
                break;
            default:
                loge("Unhandled message with number: " + msg.what);
                break;
        }
    }

    private void updateIccAvailability() {
        synchronized (mLock) {
            UiccCard newCard = mUiccController.getUiccCard();
            CardState state = CardState.CARDSTATE_ABSENT;
            UiccCardApplication newApp = null;
            IccRecords newRecords = null;
            if (newCard != null) {
                state = newCard.getCardState();
                newApp = newCard.getApplication(mCurrentAppType);
                if (newApp != null) {
                    newRecords = newApp.getIccRecords();
                }
            }

            if (mIccRecords != newRecords || mUiccApplication != newApp || mUiccCard != newCard) {
                if (DBG) log("Icc changed. Reregestering.");
                unregisterUiccCardEvents();
                mUiccCard = newCard;
                mUiccApplication = newApp;
                mIccRecords = newRecords;
                registerUiccCardEvents();
            }

            updateExternalState();
        }
    }
    
    private void updateExternalState() {

        if (mUiccCard == null) {
            setExternalState(State.NOT_READY);
            return;
        } else if (mUiccCard.getCardState() == CardState.CARDSTATE_ABSENT){
            setExternalState(State.ABSENT);
            return;
        }

/*    [ALPS00438909] We should not set NOT_READY if card slot is absent.
        if (mUiccCard == null || mUiccCard.getCardState() == CardState.CARDSTATE_ABSENT) {
            if (mRadioOn) {
                setExternalState(State.ABSENT);
            } else {
                setExternalState(State.NOT_READY);
            }
            return;
        }
*/

        if (mUiccCard.getCardState() == CardState.CARDSTATE_ERROR ||
                mUiccApplication == null) {
            setExternalState(State.UNKNOWN);
            return;
        }

        switch (mUiccApplication.getState()) {
            case APPSTATE_UNKNOWN:
            case APPSTATE_DETECTED:
                setExternalState(State.UNKNOWN);
                break;
            case APPSTATE_PIN:
                setExternalState(State.PIN_REQUIRED);
                break;
            case APPSTATE_PUK:
                setExternalState(State.PUK_REQUIRED);
                break;
            case APPSTATE_SUBSCRIPTION_PERSO:
                /*  [mtk02772][ALPS00437082]
                    JB MR1 only set network locked when subState is SIM_NETWORK,
                    mediatek platform will set network locked for all of subState (5 type of network locked)
                */
                setExternalState(State.NETWORK_LOCKED);

                /*
                if (mUiccApplication.getPersoSubState() == PersoSubState.PERSOSUBSTATE_SIM_NETWORK) {
                    setExternalState(State.NETWORK_LOCKED);
                } else {
                    setExternalState(State.UNKNOWN);
                }
                */
                break;
            case APPSTATE_READY:
                setExternalState(State.READY);
                break;
        }
    }
    
    private void registerUiccCardEvents() {
        if (mUiccCard != null) mUiccCard.registerForAbsent(this, EVENT_ICC_ABSENT, null);
        if (mUiccApplication != null) {
            mUiccApplication.registerForReady(this, EVENT_APP_READY, null);
            mUiccApplication.registerForLocked(this, EVENT_ICC_LOCKED, null);
            mUiccApplication.registerForNetworkLocked(this, EVENT_NETWORK_LOCKED, null);
        }
        if (mIccRecords != null) {
            mIccRecords.registerForImsiReady(this, EVENT_IMSI_READY, null);
            mIccRecords.registerForRecordsLoaded(this, EVENT_RECORDS_LOADED, null);
        }
    }

    private void unregisterUiccCardEvents() {
        if (mUiccCard != null) mUiccCard.unregisterForAbsent(this);
        if (mUiccApplication != null) mUiccApplication.unregisterForReady(this);
        if (mUiccApplication != null) mUiccApplication.unregisterForLocked(this);
        if (mUiccApplication != null) mUiccApplication.unregisterForNetworkLocked(this);
        if (mIccRecords != null) mIccRecords.unregisterForImsiReady(this);
        if (mIccRecords != null) mIccRecords.unregisterForRecordsLoaded(this);
    }

    private void broadcastIccStateChangedIntent(String value, String reason) {
        synchronized (mLock) {
            if (mQuietMode) {
                log("QuietMode: NOT Broadcasting intent ACTION_SIM_STATE_CHANGED " +  value
                        + " reason " + reason);
                return;
            }

            Intent intent = new Intent(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
            //intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
            intent.putExtra(PhoneConstants.PHONE_NAME_KEY, "Phone");
            intent.putExtra(IccCardConstants.INTENT_KEY_ICC_STATE, value);
            intent.putExtra(IccCardConstants.INTENT_KEY_LOCKED_REASON, reason);
            intent.putExtra(PhoneConstants.GEMINI_SIM_ID_KEY, mSimId);
            if (DBG) log("Broadcasting intent ACTION_SIM_STATE_CHANGED " +  value
                    + " reason " + reason + " sim id " + mSimId);
            ActivityManagerNative.broadcastStickyIntent(intent, READ_PHONE_STATE,
                    UserHandle.USER_ALL);
        }
    }

    public void broadcastIccStateChangedExtendIntent(String value, String reason) {
        synchronized (mLock) {
            if (mQuietMode) {
                log("QuietMode: NOT Broadcasting extend intent ACTION_SIM_STATE_CHANGED " +  value
                        + " reason " + reason);
                return;
            }

            Intent intent = new Intent(TelephonyIntents.ACTION_SIM_STATE_CHANGED_EXTEND);
            //intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
            intent.putExtra(PhoneConstants.PHONE_NAME_KEY, "Phone");
            intent.putExtra(IccCardConstants.INTENT_KEY_ICC_STATE, value);
            intent.putExtra(IccCardConstants.INTENT_KEY_LOCKED_REASON, reason);
            intent.putExtra(PhoneConstants.GEMINI_SIM_ID_KEY, mSimId);
            if (DBG) log("Broadcasting intent ACTION_SIM_STATE_CHANGED_EXTEND " +  value
                    + " reason " + reason + " sim id " + mSimId);
            ActivityManagerNative.broadcastStickyIntent(intent, READ_PHONE_STATE,
                    UserHandle.USER_ALL);
        }
    }

    private void processLockedState() {
        synchronized (mLock) {
            if (mUiccApplication == null) {
                //Don't need to do anything if non-existent application is locked
                return;
            }
            PinState pin1State = mUiccApplication.getPin1State();
            if (pin1State == PinState.PINSTATE_ENABLED_PERM_BLOCKED) {
                setExternalState(State.PERM_DISABLED);
                return;
            }

            AppState appState = mUiccApplication.getState();
            switch (appState) {
                case APPSTATE_PIN:
                    mPinLockedRegistrants.notifyRegistrants();
                    setExternalState(State.PIN_REQUIRED);
                    break;
                case APPSTATE_PUK:
                    setExternalState(State.PUK_REQUIRED);
                    break;
            }
        }
    }
    
    private void setExternalState(State newState, boolean override) {
        synchronized (mLock) {
            if (DBG) log("setExternalState(): mExternalState = " + mExternalState +
                " newState =  " + newState + " override = " + override);
            
            if (!override && newState == mExternalState) {
                if(newState == State.NETWORK_LOCKED && 
                    mNetworkLockState != getNetworkPersoType()) {
                    if (DBG) log("NetworkLockState =  " + mNetworkLockState);
                } else {
                    return;
                }
            }
            mExternalState = newState;
            mNetworkLockState = getNetworkPersoType();

            SystemProperties.set(ICCCARDPROXY_PROPERTY_SIM_STATE[getMySimId()], mExternalState.toString());

            // GeminiPhone initialization is behind of IccCardProxy, 
            // so it needs to get this phone object after constructor
            // we get it after mInitialized, after radio on
            if(mInitialized) {
                if(FeatureOption.MTK_GEMINI_SUPPORT) {
                    mPhone = ((GeminiPhone)(PhoneFactory.getDefaultPhone())).getPhonebyId(mSimId);
                } else {
                    mPhone = PhoneFactory.getDefaultPhone();
                }

                mPhone.updateSimIndicateState();
            }
            broadcastIccStateChangedIntent(getIccStateIntentString(mExternalState),
                    getIccStateReason(mExternalState));
            broadcastIccStateChangedExtendIntent(getIccStateIntentString(mExternalState),
                    getIccStateReason(mExternalState));
        }
    }

    private void setExternalState(State newState) {
        if (DBG) log("setExternalState(): newState =  " + newState);
        setExternalState(newState, false);
    }

    public boolean getIccRecordsLoaded() {
        synchronized (mLock) {
            if (mIccRecords != null) {
                return mIccRecords.getRecordsLoaded();
            }
            return false;
        }
    }

    private String getIccStateIntentString(State state) {
        switch (state) {
            case ABSENT: return IccCardConstants.INTENT_VALUE_ICC_ABSENT;
            case PIN_REQUIRED: return IccCardConstants.INTENT_VALUE_ICC_LOCKED;
            case PUK_REQUIRED: return IccCardConstants.INTENT_VALUE_ICC_LOCKED;
            case NETWORK_LOCKED: return IccCardConstants.INTENT_VALUE_ICC_LOCKED;
            case READY: return IccCardConstants.INTENT_VALUE_ICC_READY;
            case NOT_READY: return IccCardConstants.INTENT_VALUE_ICC_NOT_READY;
            case PERM_DISABLED: return IccCardConstants.INTENT_VALUE_ICC_LOCKED;
            default: return IccCardConstants.INTENT_VALUE_ICC_UNKNOWN;
        }
    }

    /**
     * Locked state have a reason (PIN, PUK, NETWORK, PERM_DISABLED)
     * @return reason
     */
    private String getIccStateReason(State state) {
        switch (state) {
            case PIN_REQUIRED: return IccCardConstants.INTENT_VALUE_LOCKED_ON_PIN;
            case PUK_REQUIRED: return IccCardConstants.INTENT_VALUE_LOCKED_ON_PUK;
            case NETWORK_LOCKED:
                switch (mUiccApplication.getPersoSubState()) {
                    case PERSOSUBSTATE_SIM_NETWORK: return IccCardConstants.INTENT_VALUE_LOCKED_NETWORK;
                    case PERSOSUBSTATE_SIM_NETWORK_SUBSET: return IccCardConstants.INTENT_VALUE_LOCKED_NETWORK_SUBSET;
                    case PERSOSUBSTATE_SIM_CORPORATE: return IccCardConstants.INTENT_VALUE_LOCKED_CORPORATE;
                    case PERSOSUBSTATE_SIM_SERVICE_PROVIDER: return IccCardConstants.INTENT_VALUE_LOCKED_SERVICE_PROVIDER;
                    case PERSOSUBSTATE_SIM_SIM: return IccCardConstants.INTENT_VALUE_LOCKED_SIM;
                    default: return null;
                }                
            case PERM_DISABLED: return IccCardConstants.INTENT_VALUE_ABSENT_ON_PERM_DISABLED;
            default: return null;
       }
    }

    /* IccCard interface implementation */
    // TODO: MTK use ICS solution, there is only three state, radio.on, radio.off, radio.not.available
    @Override
    public State getState() {
        synchronized (mLock) {
            return mExternalState;
        }
    }

    @Override
    public IccRecords getIccRecords() {
        synchronized (mLock) {
            return mIccRecords;
        }
    }

    @Override
    public IccFileHandler getIccFileHandler() {
        synchronized (mLock) {
            if (mUiccApplication != null) {
                return mUiccApplication.getIccFileHandler();
            }
            return null;
        }
    }

    /**
     * Notifies handler of any transition into State.ABSENT
     */
    @Override
    public void registerForAbsent(Handler h, int what, Object obj) {
        synchronized (mLock) {
            Registrant r = new Registrant (h, what, obj);

            mAbsentRegistrants.add(r);

            if (getState() == State.ABSENT) {
                r.notifyRegistrant();
            }
        }
    }
    
    @Override
    public void unregisterForAbsent(Handler h) {
        synchronized (mLock) {
            mAbsentRegistrants.remove(h);
        }
    }

    public void registerForRecovery(Handler h, int what, Object obj) {
        synchronized (mLock) {
            Registrant r = new Registrant (h, what, obj);

            mRecoveryRegistrants.add(r);

            if (getState() == State.READY) {
                r.notifyRegistrant();
            }
        }
    }

    public void unregisterForRecovery(Handler h) {
        synchronized (mLock) {
            mRecoveryRegistrants.remove(h);
        }
    }

    /**
     * Notifies handler of any transition into State.NETWORK_LOCKED
     */
    @Override
    public void registerForNetworkLocked(Handler h, int what, Object obj) {
        synchronized (mLock) {
            Registrant r = new Registrant (h, what, obj);

            mNetworkLockedRegistrants.add(r);

            if (getState() == State.NETWORK_LOCKED) {
                r.notifyRegistrant();
            }
        }
    }

    @Override
    public void unregisterForNetworkLocked(Handler h) {
        synchronized (mLock) {
            mNetworkLockedRegistrants.remove(h);
        }
    }

    /**
     * Notifies handler of any transition into State.isPinLocked()
     */
    @Override
    public void registerForLocked(Handler h, int what, Object obj) {
        synchronized (mLock) {
            Registrant r = new Registrant (h, what, obj);

            mPinLockedRegistrants.add(r);

            if (getState().isPinLocked()) {
                r.notifyRegistrant();
            }
        }
    }

    @Override
    public void unregisterForLocked(Handler h) {
        synchronized (mLock) {
            mPinLockedRegistrants.remove(h);
        }
    }

    @Override
    public void supplyPin(String pin, Message onComplete) {
        synchronized (mLock) {
            if (mUiccApplication != null) {
                mUiccApplication.supplyPin(pin, onComplete);
            } else if (onComplete != null) {
                Exception e = new RuntimeException("ICC card is absent.");
                AsyncResult.forMessage(onComplete).exception = e;
                onComplete.sendToTarget();
                return;
            }
        }
    }

    @Override
    public void supplyPuk(String puk, String newPin, Message onComplete) {
        synchronized (mLock) {
            if (mUiccApplication != null) {
                mUiccApplication.supplyPuk(puk, newPin, onComplete);
            } else if (onComplete != null) {
                Exception e = new RuntimeException("ICC card is absent.");
                AsyncResult.forMessage(onComplete).exception = e;
                onComplete.sendToTarget();
                return;
            }
        }
    }

    @Override
    public void supplyPin2(String pin2, Message onComplete) {
        synchronized (mLock) {
            if (mUiccApplication != null) {
                mUiccApplication.supplyPin2(pin2, onComplete);
            } else if (onComplete != null) {
                Exception e = new RuntimeException("ICC card is absent.");
                AsyncResult.forMessage(onComplete).exception = e;
                onComplete.sendToTarget();
                return;
            }
        }
    }

    @Override
    public void supplyPuk2(String puk2, String newPin2, Message onComplete) {
        synchronized (mLock) {
            if (mUiccApplication != null) {
                mUiccApplication.supplyPuk2(puk2, newPin2, onComplete);
            } else if (onComplete != null) {
                Exception e = new RuntimeException("ICC card is absent.");
                AsyncResult.forMessage(onComplete).exception = e;
                onComplete.sendToTarget();
                return;
            }
        }
    }

    @Override
    public void supplyNetworkDepersonalization(String pin, Message onComplete) {
        synchronized (mLock) {
            if (mUiccApplication != null) {
                mUiccApplication.supplyNetworkDepersonalization(pin, onComplete);
            } else if (onComplete != null) {
                Exception e = new RuntimeException("CommandsInterface is not set.");
                AsyncResult.forMessage(onComplete).exception = e;
                onComplete.sendToTarget();
                return;
            }
        }
    }

    @Override
    public boolean getIccLockEnabled() {
        synchronized (mLock) {
            /* defaults to true, if ICC is absent */
            Boolean retValue = mUiccApplication != null ? mUiccApplication.getIccLockEnabled() : true;
            return retValue;
        }
    }

    @Override
    public boolean getIccFdnEnabled() {
        synchronized (mLock) {
            Boolean retValue = mUiccApplication != null ? mUiccApplication.getIccFdnEnabled() : false;
            return retValue;
        }
    }

    @Override
    public void setIccLockEnabled(boolean enabled, String password, Message onComplete) {
     synchronized (mLock) {
         if (mUiccApplication != null) {
             mUiccApplication.setIccLockEnabled(enabled, password, onComplete);
         } else if (onComplete != null) {
             Exception e = new RuntimeException("ICC card is absent.");
             AsyncResult.forMessage(onComplete).exception = e;
             onComplete.sendToTarget();
             return;
         }
     }
    }

    @Override
    public void setIccFdnEnabled(boolean enabled, String password, Message onComplete) {
     synchronized (mLock) {
         if (mUiccApplication != null) {
             mUiccApplication.setIccFdnEnabled(enabled, password, onComplete);
         } else if (onComplete != null) {
             Exception e = new RuntimeException("ICC card is absent.");
             AsyncResult.forMessage(onComplete).exception = e;
             onComplete.sendToTarget();
             return;
         }
     }
    }

    @Override
    public void changeIccLockPassword(String oldPassword, String newPassword, Message onComplete) {
     synchronized (mLock) {
         if (mUiccApplication != null) {
             mUiccApplication.changeIccLockPassword(oldPassword, newPassword, onComplete);
         } else if (onComplete != null) {
             Exception e = new RuntimeException("ICC card is absent.");
             AsyncResult.forMessage(onComplete).exception = e;
             onComplete.sendToTarget();
             return;
         }
     }
    }

    @Override
    public void changeIccFdnPassword(String oldPassword, String newPassword, Message onComplete) {
        synchronized (mLock) {
            if (mUiccApplication != null) {
                mUiccApplication.changeIccFdnPassword(oldPassword, newPassword, onComplete);
            } else if (onComplete != null) {
                Exception e = new RuntimeException("ICC card is absent.");
                AsyncResult.forMessage(onComplete).exception = e;
                onComplete.sendToTarget();
                return;
            }
        }
    }

    @Override
    public String getServiceProviderName() {
        synchronized (mLock) {
            if (mIccRecords != null) {
                return mIccRecords.getServiceProviderName();
            }
            return null;
        }
    }

    @Override
    public boolean isApplicationOnIcc(IccCardApplicationStatus.AppType type) {
        synchronized (mLock) {
            Boolean retValue = mUiccCard != null ? mUiccCard.isApplicationOnIcc(type) : false;
            return retValue;
        }
    }

    @Override
    public boolean hasIccCard() {
        synchronized (mLock) {
            if (mUiccCard != null && mUiccCard.getCardState() != CardState.CARDSTATE_ABSENT) {
                return true;
            }
            return false;
        }
    }

// ======= mtk02772 start =======

    /**
     * Check whether ICC network lock is enabled
     * This is an async call which returns lock state to applications directly
     */
    public void QueryIccNetworkLock (int category,
            int lockop, String password, String data_imsi, String gid1, String gid2, Message onComplete) {
        if (DBG) log("QueryIccNetworkLock(): category =  " + category);
        synchronized (mLock) {
            if (mUiccApplication != null) {
                mUiccApplication.QueryIccNetworkLock(category, lockop, password, data_imsi, gid1, gid2, onComplete);
            } else if (onComplete != null) {
                Exception e = new RuntimeException("ICC card is absent.");
                AsyncResult.forMessage(onComplete).exception = e;
                onComplete.sendToTarget();
                return;
            }
        }
    }

    /**
     * Set the ICC network lock enabled or disabled
     * When the operation is complete, onComplete will be sent to its handler
     */
    public void setIccNetworkLockEnabled (int category,
            int lockop, String password, String data_imsi, String gid1, String gid2, Message onComplete) {
        if (DBG) log("SetIccNetworkEnabled(): category = " + category
            + " lockop = " + lockop + " password = " + password
            + " data_imsi = " + data_imsi + " gid1 = " + gid1 + " gid2 = " + gid2);
        synchronized (mLock) {
            if (mUiccApplication != null) {
                mUiccApplication.setIccNetworkLockEnabled(category, lockop, password, data_imsi, gid1, gid2, onComplete);
            } else if (onComplete != null) {
                Exception e = new RuntimeException("ICC card is absent.");
                AsyncResult.forMessage(onComplete).exception = e;
                onComplete.sendToTarget();
                return;
            }
        }
    }


    private void broadcastRadioOffIntent() {
        Intent intent = new Intent(TelephonyIntents.ACTION_RADIO_OFF);
        intent.putExtra(PhoneConstants.PHONE_NAME_KEY, "Phone");
        intent.putExtra(TelephonyIntents.INTENT_KEY_ICC_SLOT, getMySimId());
        if(DBG) log("Broadcasting intent ACTION_RADIO_OFF "  
                + " sim id " + getMySimId());
        mContext.sendBroadcast(intent);
    }

    public boolean isPhbReady() {
        boolean isPhbReady = getIccRecords().isPhbReady();
        if (DBG) log("isPhbReady() isPhbReady = " + (isPhbReady ? "true" : "false"));
        return isPhbReady;
    }

    // retrun usim property or use uicccardapplication app type
    public String getIccCardType() {
        if (mUiccCard != null && mUiccCard.getCardState() != CardState.CARDSTATE_ABSENT) {
            return mUiccCard.getIccCardType();
        }
        return "";
    }

    //return the SIM ME Lock type required to unlock
    public int getNetworkPersoType(){
        if (mUiccApplication != null) {
            switch (mUiccApplication.getPersoSubState()){
                case PERSOSUBSTATE_SIM_NETWORK:
                    return 0;
                case PERSOSUBSTATE_SIM_NETWORK_SUBSET:
                    return 1;
                case PERSOSUBSTATE_SIM_SERVICE_PROVIDER:
                    return 2;
                case PERSOSUBSTATE_SIM_CORPORATE:
                    return 3;
                case PERSOSUBSTATE_SIM_SIM:
                    return 4;
                default:
                    break;
            }
        }
        return -1;
    }
    
    public boolean isFDNExist(){
        UsimServiceTable ust= mIccRecords.getUsimServiceTable();
        if(ust!=null && ust.isAvailable(UsimServiceTable.UsimService.FDN))
        {
            if (DBG) log("isFDNExist return true");
            return true;
        }
        else
        {       
            if (DBG) log("isFDNExist return false");
            return false;
        }
    }

    // ALPS00294581
    public void disableSimMissingNotification() {
        return;
    }

    public int getMySimId() {
        return mSimId;
    }

    // NFC SEEK start
    public void exchangeAPDU(int cla, int command, int channel, int p1, int p2,
            int p3, String data, Message onComplete) {
        if (mUiccCard != null && mUiccCard.getCardState() != CardState.CARDSTATE_ABSENT) {
            mUiccCard.exchangeAPDU(cla, command, channel, p1, p2, p3, data, onComplete);
        }
    }

    public void openLogicalChannel(String AID, Message onComplete) {
        if (mUiccCard != null && mUiccCard.getCardState() != CardState.CARDSTATE_ABSENT) {
            mUiccCard.openLogicalChannel(AID, onComplete);
        }
    }

    public void closeLogicalChannel(int channel, Message onComplete) {
        if (mUiccCard != null && mUiccCard.getCardState() != CardState.CARDSTATE_ABSENT) {
            mUiccCard.closeLogicalChannel(channel, onComplete);
        }
    }
    
    public void exchangeSimIO(int fileID, int command,
                                           int p1, int p2, int p3, String pathID, Message onComplete) {
        if (mUiccCard != null && mUiccCard.getCardState() != CardState.CARDSTATE_ABSENT) {
            mUiccCard.exchangeSimIO(fileID, command, p1, p2, p3, pathID,onComplete);
        }
    }   

    public void iccGetATR(Message onComplete) {
        if (mUiccCard != null && mUiccCard.getCardState() != CardState.CARDSTATE_ABSENT) {
            mUiccCard.iccGetATR(onComplete);
        }
    }
    public void openLogicalChannelWithSw(String AID, Message onComplete) {
        if (mUiccCard != null && mUiccCard.getCardState() != CardState.CARDSTATE_ABSENT) {
            mUiccCard.iccOpenChannelWithSw(AID, onComplete);
        }
    }
    // NFC SEEK end

    private void log(String msg) {
        Log.d(LOG_TAG, "[IccCard][SIM" + (getMySimId() == 0?"1":"2") + "] "+ msg);
    }

    private void loge(String msg) {
        Log.e(LOG_TAG, "[IccCard][SIM" + (getMySimId() == 0?"1":"2") + "] "+ msg);
    }
}
