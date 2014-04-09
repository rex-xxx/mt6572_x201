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

package com.android.internal.telephony;

import com.android.internal.telephony.gsm.GsmMmiCode;
import com.android.internal.telephony.sip.SipPhone;
import com.android.internal.telephony.cdma.CdmaMmiCode;
import com.android.internal.telephony.cdma.CDMAPhone;

import android.content.Context;
import android.media.AudioManager;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.RegistrantList;
import android.os.Registrant;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// M: [mtk04070][111121][ALPS00093395]MTK added. @{
import android.os.SystemProperties;
  import com.android.internal.telephony.gemini.*;
import com.android.internal.telephony.gsm.GsmConnection;
import com.android.internal.telephony.gsm.GSMPhone;
import com.mediatek.common.featureoption.FeatureOption;
/* 3G switch start */
import com.android.internal.telephony.ITelephony;
import android.os.ServiceManager;
import android.os.RemoteException;
/* 3G switch end */

import android.telephony.PhoneNumberUtils;

/// M: [ALPS00383541]Update call state to Accdet directly.
import java.io.FileWriter;

import com.mediatek.common.MediatekClassFactory;
import com.mediatek.common.telephony.IPhoneNumberExt;

/// @}

/**
 * @hide
 *
 * CallManager class provides an abstract layer for PhoneApp to access
 * and control calls. It implements Phone interface.
 *
 * CallManager provides call and connection control as well as
 * channel capability.
 *
 * There are three categories of APIs CallManager provided
 *
 *  1. Call control and operation, such as dial() and hangup()
 *  2. Channel capabilities, such as CanConference()
 *  3. Register notification
 *
 *
 */
public final class CallManager {

    private static final String LOG_TAG ="CallManager";
    private static final boolean DBG = true;
    /// M: [mtk04070][111121][ALPS00093395]Set VDBG to true. 
    private static final boolean VDBG = true;

    /// M: For supporting Gemini 2.0+. @{
    private static final int NOTIFICATION_ID_OFFSET = 100;
    private static final int MAXIMUM_SIM_COUNT = 4;
    /// @}

    private static final int EVENT_DISCONNECT = 100;
    private static final int EVENT_PRECISE_CALL_STATE_CHANGED = 101;
    private static final int EVENT_NEW_RINGING_CONNECTION = 102;
    private static final int EVENT_UNKNOWN_CONNECTION = 103;
    private static final int EVENT_INCOMING_RING = 104;
    private static final int EVENT_RINGBACK_TONE = 105;
    private static final int EVENT_IN_CALL_VOICE_PRIVACY_ON = 106;
    private static final int EVENT_IN_CALL_VOICE_PRIVACY_OFF = 107;
    private static final int EVENT_CALL_WAITING = 108;
    private static final int EVENT_DISPLAY_INFO = 109;
    private static final int EVENT_SIGNAL_INFO = 110;
    private static final int EVENT_CDMA_OTA_STATUS_CHANGE = 111;
    private static final int EVENT_RESEND_INCALL_MUTE = 112;
    private static final int EVENT_MMI_INITIATE = 113;
    private static final int EVENT_MMI_COMPLETE = 114;
    private static final int EVENT_ECM_TIMER_RESET = 115;
    private static final int EVENT_SUBSCRIPTION_INFO_READY = 116;
    private static final int EVENT_SUPP_SERVICE_FAILED = 117;
    private static final int EVENT_SERVICE_STATE_CHANGED = 118;
    private static final int EVENT_POST_DIAL_CHARACTER = 119;

    /// M: [mtk04070][111121][ALPS00093395]MTK added. @{
    private static final int EVENT_SPEECH_INFO = 120;
    private static final int EVENT_VT_STATUS_INFO = 121;
    private static final int EVENT_VT_RING_INFO = 122;
    private static final int EVENT_CRSS_SUPP_SERVICE_NOTIFICATION = 123;
    private static final int EVENT_SUPP_SERVICE_NOTIFICATION = 124;
    private static final int EVENT_VT_REPLACE_DISCONNECT = 125;
    private static final int EVENT_VOICE_CALL_INCOMING_INDICATION = 126;
    
    private static final int EVENT_DISCONNECT2 = 200;
    private static final int EVENT_PRECISE_CALL_STATE_CHANGED2 = 201;
    private static final int EVENT_NEW_RINGING_CONNECTION2 = 202;
    private static final int EVENT_UNKNOWN_CONNECTION2 = 203;
    private static final int EVENT_INCOMING_RING2 = 204;
    private static final int EVENT_RINGBACK_TONE2 = 205;
    private static final int EVENT_IN_CALL_VOICE_PRIVACY_ON2 = 206;
    private static final int EVENT_IN_CALL_VOICE_PRIVACY_OFF2 = 207;
    private static final int EVENT_CALL_WAITING2 = 208;
    private static final int EVENT_DISPLAY_INFO2 = 209;
    private static final int EVENT_SIGNAL_INFO2 = 210;
    private static final int EVENT_CDMA_OTA_STATUS_CHANGE2 = 211;
    private static final int EVENT_RESEND_INCALL_MUTE2 = 212;
    private static final int EVENT_MMI_INITIATE2 = 213;
    private static final int EVENT_MMI_COMPLETE2 = 214;
    private static final int EVENT_ECM_TIMER_RESET2 = 215;
    private static final int EVENT_SUBSCRIPTION_INFO_READY2 = 216;
    private static final int EVENT_SUPP_SERVICE_FAILED2 = 217;
    private static final int EVENT_SERVICE_STATE_CHANGED2 = 218;
    private static final int EVENT_POST_DIAL_CHARACTER2 = 219;
    private static final int EVENT_SPEECH_INFO2 = 220;
    private static final int EVENT_VT_STATUS_INFO2 = 221;
    private static final int EVENT_VT_RING_INFO2 = 222;
    private static final int EVENT_CRSS_SUPP_SERVICE_NOTIFICATION2 = 223;
    private static final int EVENT_SUPP_SERVICE_NOTIFICATION2 = 224;
    private static final int EVENT_VT_REPLACE_DISCONNECT2 = 225;
    private static final int EVENT_VOICE_CALL_INCOMING_INDICATION2 = 226;
    /// @}

    // Singleton instance
    private static final CallManager INSTANCE = new CallManager();

    // list of registered phones, which are PhoneBase objs
    private final ArrayList<Phone> mPhones;

    // list of supported ringing calls
    private final ArrayList<Call> mRingingCalls;

    // list of supported background calls
    private final ArrayList<Call> mBackgroundCalls;

    // list of supported foreground calls
    private final ArrayList<Call> mForegroundCalls;

    // empty connection list
    private final ArrayList<Connection> emptyConnections = new ArrayList<Connection>();

    // default phone as the first phone registered, which is PhoneBase obj
    private Phone mDefaultPhone;

    private boolean mSpeedUpAudioForMtCall = false;

    // state registrants
    protected final RegistrantList mPreciseCallStateRegistrants
    = new RegistrantList();

    protected final RegistrantList mNewRingingConnectionRegistrants
    = new RegistrantList();

    protected final RegistrantList mIncomingRingRegistrants
    = new RegistrantList();

    protected final RegistrantList mDisconnectRegistrants
    = new RegistrantList();

    protected final RegistrantList mMmiRegistrants
    = new RegistrantList();

    protected final RegistrantList mUnknownConnectionRegistrants
    = new RegistrantList();

    protected final RegistrantList mRingbackToneRegistrants
    = new RegistrantList();

    protected final RegistrantList mInCallVoicePrivacyOnRegistrants
    = new RegistrantList();

    protected final RegistrantList mInCallVoicePrivacyOffRegistrants
    = new RegistrantList();

    protected final RegistrantList mCallWaitingRegistrants
    = new RegistrantList();

    protected final RegistrantList mDisplayInfoRegistrants
    = new RegistrantList();

    protected final RegistrantList mSignalInfoRegistrants
    = new RegistrantList();

    protected final RegistrantList mCdmaOtaStatusChangeRegistrants
    = new RegistrantList();

    protected final RegistrantList mResendIncallMuteRegistrants
    = new RegistrantList();

    protected final RegistrantList mMmiInitiateRegistrants
    = new RegistrantList();

    protected final RegistrantList mMmiCompleteRegistrants
    = new RegistrantList();

    protected final RegistrantList mEcmTimerResetRegistrants
    = new RegistrantList();

    protected final RegistrantList mSubscriptionInfoReadyRegistrants
    = new RegistrantList();

    protected final RegistrantList mSuppServiceFailedRegistrants
    = new RegistrantList();

    protected final RegistrantList mServiceStateChangedRegistrants
    = new RegistrantList();

    protected final RegistrantList mPostDialCharacterRegistrants
    = new RegistrantList();

    /// M: [mtk04070][111121][ALPS00093395]MTK added. @{
    /* MTK proprietary start */
    protected final RegistrantList mSpeechInfoRegistrants
    = new RegistrantList();

    protected final RegistrantList mVtStatusInfoRegistrants
    = new RegistrantList();

    protected final RegistrantList mVtRingInfoRegistrants
    = new RegistrantList();

    protected final RegistrantList mCrssSuppServiceNotificationRegistrants
    = new RegistrantList();

    protected final RegistrantList mSuppServiceNotificationRegistrants
    = new RegistrantList();

    protected final RegistrantList mVtReplaceDisconnectRegistrants
    = new RegistrantList();

    protected final RegistrantList mPreciseCallStateRegistrants2
    = new RegistrantList();

    protected final RegistrantList mNewRingingConnectionRegistrants2
    = new RegistrantList();

    protected final RegistrantList mIncomingRingRegistrants2
    = new RegistrantList();

    protected final RegistrantList mDisconnectRegistrants2
    = new RegistrantList();

    protected final RegistrantList mMmiRegistrants2
    = new RegistrantList();

    protected final RegistrantList mUnknownConnectionRegistrants2
    = new RegistrantList();

    protected final RegistrantList mRingbackToneRegistrants2
    = new RegistrantList();

    protected final RegistrantList mInCallVoicePrivacyOnRegistrants2
    = new RegistrantList();

    protected final RegistrantList mInCallVoicePrivacyOffRegistrants2
    = new RegistrantList();

    protected final RegistrantList mCallWaitingRegistrants2
    = new RegistrantList();

    protected final RegistrantList mDisplayInfoRegistrants2
    = new RegistrantList();

    protected final RegistrantList mSignalInfoRegistrants2
    = new RegistrantList();

    protected final RegistrantList mCdmaOtaStatusChangeRegistrants2
    = new RegistrantList();

    protected final RegistrantList mResendIncallMuteRegistrants2
    = new RegistrantList();

    protected final RegistrantList mMmiInitiateRegistrants2
    = new RegistrantList();

    protected final RegistrantList mMmiCompleteRegistrants2
    = new RegistrantList();

    protected final RegistrantList mEcmTimerResetRegistrants2
    = new RegistrantList();

    protected final RegistrantList mSubscriptionInfoReadyRegistrants2
    = new RegistrantList();

    protected final RegistrantList mSuppServiceFailedRegistrants2
    = new RegistrantList();

    protected final RegistrantList mServiceStateChangedRegistrants2
    = new RegistrantList();

    protected final RegistrantList mPostDialCharacterRegistrants2
    = new RegistrantList();

    protected final RegistrantList mSpeechInfoRegistrants2
    = new RegistrantList();

    protected final RegistrantList mVtStatusInfoRegistrants2
    = new RegistrantList();

    protected final RegistrantList mVtRingInfoRegistrants2
    = new RegistrantList();

    protected final RegistrantList mCrssSuppServiceNotificationRegistrants2
    = new RegistrantList();

    protected final RegistrantList mSuppServiceNotificationRegistrants2
    = new RegistrantList();

    protected final RegistrantList mVtReplaceDisconnectRegistrants2
    = new RegistrantList();


    /// M: State registrants for supporting Gemini 2.0+. @{
    protected final RegistrantList []mPreciseCallStateRegistrantsGemini = new RegistrantList[MAXIMUM_SIM_COUNT];
    protected final RegistrantList []mNewRingingConnectionRegistrantsGemini = new RegistrantList[MAXIMUM_SIM_COUNT];
    protected final RegistrantList []mIncomingRingRegistrantsGemini = new RegistrantList[MAXIMUM_SIM_COUNT];
    protected final RegistrantList []mDisconnectRegistrantsGemini = new RegistrantList[MAXIMUM_SIM_COUNT];
    protected final RegistrantList []mUnknownConnectionRegistrantsGemini = new RegistrantList[MAXIMUM_SIM_COUNT];
    protected final RegistrantList []mRingbackToneRegistrantsGemini = new RegistrantList[MAXIMUM_SIM_COUNT];
    protected final RegistrantList []mInCallVoicePrivacyOnRegistrantsGemini = new RegistrantList[MAXIMUM_SIM_COUNT];
    protected final RegistrantList []mInCallVoicePrivacyOffRegistrantsGemini = new RegistrantList[MAXIMUM_SIM_COUNT];
    protected final RegistrantList []mCallWaitingRegistrantsGemini = new RegistrantList[MAXIMUM_SIM_COUNT];
    protected final RegistrantList []mDisplayInfoRegistrantsGemini = new RegistrantList[MAXIMUM_SIM_COUNT];
    protected final RegistrantList []mSignalInfoRegistrantsGemini = new RegistrantList[MAXIMUM_SIM_COUNT];
    protected final RegistrantList []mCdmaOtaStatusChangeRegistrantsGemini = new RegistrantList[MAXIMUM_SIM_COUNT];
    protected final RegistrantList []mResendIncallMuteRegistrantsGemini = new RegistrantList[MAXIMUM_SIM_COUNT];
    protected final RegistrantList []mMmiInitiateRegistrantsGemini = new RegistrantList[MAXIMUM_SIM_COUNT];
    protected final RegistrantList []mMmiCompleteRegistrantsGemini = new RegistrantList[MAXIMUM_SIM_COUNT];
    protected final RegistrantList []mEcmTimerResetRegistrantsGemini = new RegistrantList[MAXIMUM_SIM_COUNT];
    protected final RegistrantList []mSubscriptionInfoReadyRegistrantsGemini = new RegistrantList[MAXIMUM_SIM_COUNT];
    protected final RegistrantList []mSuppServiceFailedRegistrantsGemini = new RegistrantList[MAXIMUM_SIM_COUNT];
    protected final RegistrantList []mServiceStateChangedRegistrantsGemini = new RegistrantList[MAXIMUM_SIM_COUNT];
    protected final RegistrantList []mPostDialCharacterRegistrantsGemini = new RegistrantList[MAXIMUM_SIM_COUNT];
    protected final RegistrantList []mSpeechInfoRegistrantsGemini = new RegistrantList[MAXIMUM_SIM_COUNT];
    protected final RegistrantList []mVtStatusInfoRegistrantsGemini = new RegistrantList[MAXIMUM_SIM_COUNT];
    protected final RegistrantList []mVtRingInfoRegistrantsGemini = new RegistrantList[MAXIMUM_SIM_COUNT];
    protected final RegistrantList []mCrssSuppServiceNotificationRegistrantsGemini = new RegistrantList[MAXIMUM_SIM_COUNT];
    protected final RegistrantList []mSuppServiceNotificationRegistrantsGemini = new RegistrantList[MAXIMUM_SIM_COUNT];
    protected final RegistrantList []mVtReplaceDisconnectRegistrantsGemini = new RegistrantList[MAXIMUM_SIM_COUNT];
    /// @}


    private boolean hasSetVtPara = false;

    //Merge DualTalk code
    private int mDualModemCall = 0; /* dual modem call */

    /* 3G Switch start */
    private int m3GSwitchLockForPhoneCall;
    /* 3G Switch end */
    
    /* Solve ALPS00275770, to prevent set audio mode to IN_CALL when ESPEECH info is not received yet */
    private int []espeech_info = new int[MAXIMUM_SIM_COUNT];

    /* Solve ALPS00281513, to avoid DTMF start request is sent, but stop request is ignore due to active is held, mtk04070, 20120515 */
    private boolean dtmfRequestIsStarted = false;
    
    /* For waiting hold request before accept call or hold active call */
    enum WaitingReasonForHold {
         NONE, ACCEPT_CALL, SWITCH_CALL, MAKE_CALL;
    };
    private boolean bWaitingForHoldRequest = false;
    private Call mActiveCallToBeHeld = null;
    private Phone mPhoneForWaitingHoldRequest = null;
    private WaitingReasonForHold mWaitingReasonForHold = WaitingReasonForHold.NONE;

    /* Store current audio mode for recovering it when failed to hold active call */
    private int mCurrentAudioMode = -1;

    /// @}

    //***** MTK class factory
    private static IPhoneNumberExt mPhoneNumberExt;
    static {
        try{
            mPhoneNumberExt = MediatekClassFactory.createInstance(IPhoneNumberExt.class);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private CallManager() {
        mPhones = new ArrayList<Phone>();
        mRingingCalls = new ArrayList<Call>();
        mBackgroundCalls = new ArrayList<Call>();
        mForegroundCalls = new ArrayList<Call>();
        mDefaultPhone = null;
        
        Log.d(LOG_TAG, "Initiate registrants array");
        /// M: Initiate registrants for Gemini 2.0+. @{
        int count = (MAXIMUM_SIM_COUNT < PhoneConstants.GEMINI_SIM_NUM) ? MAXIMUM_SIM_COUNT : PhoneConstants.GEMINI_SIM_NUM;
        for (int i = 0; i < count; i++) {
            mPreciseCallStateRegistrantsGemini[i]  = new RegistrantList(); 
            mNewRingingConnectionRegistrantsGemini[i]  = new RegistrantList();
            mIncomingRingRegistrantsGemini[i]  = new RegistrantList();
            mDisconnectRegistrantsGemini[i]  = new RegistrantList();
            mUnknownConnectionRegistrantsGemini[i]  = new RegistrantList();
            mRingbackToneRegistrantsGemini[i]  = new RegistrantList();
            mInCallVoicePrivacyOnRegistrantsGemini[i]  = new RegistrantList(); 
            mInCallVoicePrivacyOffRegistrantsGemini[i]  = new RegistrantList();
            mCallWaitingRegistrantsGemini[i]  = new RegistrantList();
            mDisplayInfoRegistrantsGemini[i]  = new RegistrantList();
            mSignalInfoRegistrantsGemini[i]  = new RegistrantList();
            mCdmaOtaStatusChangeRegistrantsGemini[i]  = new RegistrantList();
            mResendIncallMuteRegistrantsGemini[i]  = new RegistrantList(); 
            mMmiInitiateRegistrantsGemini[i]  = new RegistrantList();
            mMmiCompleteRegistrantsGemini[i]  = new RegistrantList();
            mEcmTimerResetRegistrantsGemini[i]  = new RegistrantList();
            mSubscriptionInfoReadyRegistrantsGemini[i]  = new RegistrantList();
            mSuppServiceFailedRegistrantsGemini[i]  = new RegistrantList();
            mServiceStateChangedRegistrantsGemini[i]  = new RegistrantList();
            mPostDialCharacterRegistrantsGemini[i]  = new RegistrantList();
            mSpeechInfoRegistrantsGemini[i]  = new RegistrantList();
            mVtStatusInfoRegistrantsGemini[i]  = new RegistrantList();
            mVtRingInfoRegistrantsGemini[i]  = new RegistrantList();
            mCrssSuppServiceNotificationRegistrantsGemini[i] = new RegistrantList();
            mSuppServiceNotificationRegistrantsGemini[i] = new RegistrantList(); 
            mVtReplaceDisconnectRegistrantsGemini[i]  = new RegistrantList();
            espeech_info[i] = 0;
        }
        /// @}
    }

    /**
     * get singleton instance of CallManager
     * @return CallManager
     */
    public static CallManager getInstance() {
        return INSTANCE;
    }

    /**
     * Get the corresponding PhoneBase obj
     *
     * @param phone a Phone object
     * @return the corresponding PhoneBase obj in Phone if Phone
     * is a PhoneProxy obj
     * or the Phone itself if Phone is not a PhoneProxy obj
     */
    private static Phone getPhoneBase(Phone phone) {
        if (phone instanceof PhoneProxy) {
            return phone.getForegroundCall().getPhone();
        }
        return phone;
    }

    /**
     * Check if two phones refer to the same PhoneBase obj
     *
     * Note: PhoneBase, not PhoneProxy, is to be used inside of CallManager
     *
     * Both PhoneBase and PhoneProxy implement Phone interface, so
     * they have same phone APIs, such as dial(). The real implementation, for
     * example in GSM,  is in GSMPhone as extend from PhoneBase, so that
     * foregroundCall.getPhone() returns GSMPhone obj. On the other hand,
     * PhoneFactory.getDefaultPhone() returns PhoneProxy obj, which has a class
     * member of GSMPhone.
     *
     * So for phone returned by PhoneFacotry, which is used by PhoneApp,
     *        phone.getForegroundCall().getPhone() != phone
     * but
     *        isSamePhone(phone, phone.getForegroundCall().getPhone()) == true
     *
     * @param p1 is the first Phone obj
     * @param p2 is the second Phone obj
     * @return true if p1 and p2 refer to the same phone
     */
    public static boolean isSamePhone(Phone p1, Phone p2) {
        return (getPhoneBase(p1) == getPhoneBase(p2));
    }

    /**
     * Returns all the registered phone objects.
     * @return all the registered phone objects.
     */
    public List<Phone> getAllPhones() {
        return Collections.unmodifiableList(mPhones);
    }

    /**
     * Get current coarse-grained voice call state.
     * If the Call Manager has an active call and call waiting occurs,
     * then the phone state is RINGING not OFFHOOK
     *
     */
    public PhoneConstants.State getState() {
        PhoneConstants.State s = PhoneConstants.State.IDLE;

        for (Phone phone : mPhones) {
            if (phone.getState() == PhoneConstants.State.RINGING) {
                s = PhoneConstants.State.RINGING;
            } else if (phone.getState() == PhoneConstants.State.OFFHOOK) {
                if (s == PhoneConstants.State.IDLE) s = PhoneConstants.State.OFFHOOK;
            }
        }
        return s;
    }

    /**
     * @return the service state of CallManager, which represents the
     * highest priority state of all the service states of phones
     *
     * The priority is defined as
     *
     * STATE_IN_SERIVCE > STATE_OUT_OF_SERIVCE > STATE_EMERGENCY > STATE_POWER_OFF
     *
     */

    public int getServiceState() {
        int resultState = ServiceState.STATE_OUT_OF_SERVICE;

        for (Phone phone : mPhones) {
            int serviceState = phone.getServiceState().getState();
            if (serviceState == ServiceState.STATE_IN_SERVICE) {
                // IN_SERVICE has the highest priority
                resultState = serviceState;
                break;
            } else if (serviceState == ServiceState.STATE_OUT_OF_SERVICE) {
                // OUT_OF_SERVICE replaces EMERGENCY_ONLY and POWER_OFF
                // Note: EMERGENCY_ONLY is not in use at this moment
                if ( resultState == ServiceState.STATE_EMERGENCY_ONLY ||
                        resultState == ServiceState.STATE_POWER_OFF) {
                    resultState = serviceState;
                }
            } else if (serviceState == ServiceState.STATE_EMERGENCY_ONLY) {
                if (resultState == ServiceState.STATE_POWER_OFF) {
                    resultState = serviceState;
                }
            }
        }
        return resultState;
    }

    /**
     * Register one phone to call manager.
     * M : This is for BSP package only.
     */
    private boolean registerOnePhone(Phone phone) {
    	  boolean result = false;
        Phone basePhone = getPhoneBase(phone);
        if (basePhone != null && !mPhones.contains(basePhone)) {
            if (DBG) {
                Log.d(LOG_TAG, "[BSPPackage]registerPhone(" + phone.getPhoneName() + " " + phone + ")");
            }

            mPhones.add(basePhone);
            mRingingCalls.add(basePhone.getRingingCall());
            mBackgroundCalls.add(basePhone.getBackgroundCall());
            mForegroundCalls.add(basePhone.getForegroundCall()); 
            result = true;   
        }
        return result;
    }


    /**
     * Register phone to CallManager
     * M: Refine this method for Gemini and BSP package.
     * @param phone to be registered
     * @return true if register successfully
     */
    public boolean registerPhone(Phone phone) {
       if ((FeatureOption.MTK_BSP_PACKAGE == true) && 
	   	   (FeatureOption.MTK_GEMINI_SUPPORT == true) && 
	   	   (!(phone instanceof SipPhone))) {
           int count = (MAXIMUM_SIM_COUNT < PhoneConstants.GEMINI_SIM_NUM) ? MAXIMUM_SIM_COUNT : PhoneConstants.GEMINI_SIM_NUM;
    	   /* registerPhone is called by Google default PhoneAPP */
           for (int i = 0; i < count; i++) {
               Phone p = ((GeminiPhone)phone).getPhonebyId(PhoneConstants.GEMINI_SIM_1 + i);
               registerOnePhone(p);
           }
           
           int default_sim = SystemProperties.getInt(PhoneConstants.GEMINI_DEFAULT_SIM_PROP, PhoneConstants.GEMINI_SIM_1);
           mDefaultPhone = getPhoneBase(((GeminiPhone)phone).getPhonebyId(default_sim));
           Log.d(LOG_TAG, "[BSPPackage]default_sim = " + default_sim);
           Log.d(LOG_TAG, "[BSPPackage]mDefaultPhone = " + mDefaultPhone);
           registerForPhoneStates(getPhoneBase(phone));
           return true;
        }
        else {
        Phone basePhone = getPhoneBase(phone);
        if (basePhone != null && !mPhones.contains(basePhone)) {
            if (DBG) {
                Log.d(LOG_TAG, "registerPhone(" + phone.getPhoneName() + " " + phone + ")");
            }

            if (mPhones.isEmpty()) {
                mDefaultPhone = basePhone;
            }
            mPhones.add(basePhone);
            mRingingCalls.add(basePhone.getRingingCall());
            mBackgroundCalls.add(basePhone.getBackgroundCall());
            mForegroundCalls.add(basePhone.getForegroundCall());
            /// M: [mtk04070][111121][ALPS00093395]Refined for supporting Gemini. @{
            if (FeatureOption.MTK_GEMINI_SUPPORT == false ||
                (FeatureOption.MTK_GEMINI_SUPPORT == true && phone instanceof SipPhone)) {
            registerForPhoneStates(basePhone);
            }
            /// @}
            return true;
        }
        return false;
    }
    }

    /**
     * unregister phone from CallManager
     * @param phone to be unregistered
     */
    public void unregisterPhone(Phone phone) {
        Phone basePhone = getPhoneBase(phone);

        if (basePhone != null && mPhones.contains(basePhone)) {

            if (DBG) {
                Log.d(LOG_TAG, "unregisterPhone(" +
                        phone.getPhoneName() + " " + phone + ")");
            }

            mPhones.remove(basePhone);
            mRingingCalls.remove(basePhone.getRingingCall());
            mBackgroundCalls.remove(basePhone.getBackgroundCall());
            mForegroundCalls.remove(basePhone.getForegroundCall());
            /// M: [mtk04070][111121][ALPS00093395]Refined for supporting Gemini. @{
            if (FeatureOption.MTK_GEMINI_SUPPORT == false ||
                (FeatureOption.MTK_GEMINI_SUPPORT == true && phone instanceof SipPhone)) {
            unregisterForPhoneStates(basePhone);
            }
            /// @}
            if (basePhone == mDefaultPhone) {
                if (mPhones.isEmpty()) {
                    mDefaultPhone = null;
                } else {
                    mDefaultPhone = mPhones.get(0);
                }
            }
        }
    }

    /**
     * return the default phone or null if no phone available
     */
    public Phone getDefaultPhone() {
        return mDefaultPhone;
    }

    /**
     * @return the phone associated with the foreground call
     */
    public Phone getFgPhone() {
        return getActiveFgCall().getPhone();
    }

    /**
     * @return the phone associated with the background call
     */
    public Phone getBgPhone() {
        return getFirstActiveBgCall().getPhone();
    }

    /**
     * @return the phone associated with the ringing call
     */
    public Phone getRingingPhone() {
        return getFirstActiveRingingCall().getPhone();
    }

    /**
     * Set audio mode for idle, ringing and offhook state.
     * 
     * M: Refine this method for Gemini and Dualtalk.
     */
    public void setAudioMode() {
        int mode = AudioManager.MODE_NORMAL;
        switch (getState()) {
            case RINGING:
                // M : remove google code by MR1.1
                /* 
                int curAudioMode = audioManager.getMode();
                if (curAudioMode != AudioManager.MODE_RINGTONE) {
                    // only request audio focus if the ringtone is going to be heard
                    if (audioManager.getStreamVolume(AudioManager.STREAM_RING) > 0) {
                        if (VDBG) Log.d(LOG_TAG, "requestAudioFocus on STREAM_RING");
                        audioManager.requestAudioFocusForCall(AudioManager.STREAM_RING,
                                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
                    }
                    if(!mSpeedUpAudioForMtCall) {
                        audioManager.setMode(AudioManager.MODE_RINGTONE);
                    }
                }

                if (mSpeedUpAudioForMtCall && (curAudioMode != AudioManager.MODE_IN_CALL)) {
                    audioManager.setMode(AudioManager.MODE_IN_CALL);
                }
                */ // M : End
                mode = AudioManager.MODE_RINGTONE;
                break;
            case OFFHOOK:
                Phone offhookPhone = getFgPhone();
                if (getActiveFgCallState() == Call.State.IDLE) {
                    // There is no active Fg calls, the OFFHOOK state
                    // is set by the Bg call. So set the phone to bgPhone.
                    offhookPhone = getBgPhone();
                }

                if (offhookPhone instanceof SipPhone) {
                    // enable IN_COMMUNICATION audio mode for sipPhone
                    mode = AudioManager.MODE_IN_COMMUNICATION;
                } else {
                    // enable IN_CALL audio mode for telephony
                    mode = AudioManager.MODE_IN_CALL;
                }
                // M : remove google code by MR1.1
                /*
                if (audioManager.getMode() != newAudioMode || mSpeedUpAudioForMtCall) {
                    // request audio focus before setting the new mode
                    if (VDBG) Log.d(LOG_TAG, "requestAudioFocus on STREAM_VOICE_CALL");
                    audioManager.requestAudioFocusForCall(AudioManager.STREAM_VOICE_CALL,
                            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
                    audioManager.setMode(newAudioMode);
                }
                mSpeedUpAudioForMtCall = false;
                */  
                // M : end
                break;
                
            // M : remove google code by MR1.1
            /*
            case IDLE:
                if (audioManager.getMode() != AudioManager.MODE_NORMAL) {
                    audioManager.setMode(AudioManager.MODE_NORMAL);
                    if (VDBG) Log.d(LOG_TAG, "abandonAudioFocus");
                    // abandon audio focus after the mode has been set back to normal
                    audioManager.abandonAudioFocusForCall();
                }
                mSpeedUpAudioForMtCall = false;
                break;
            */    
        }

        if ((mode == AudioManager.MODE_IN_CALL) && (FeatureOption.MTK_DT_SUPPORT == true)) {
            int newDualModemCall;
            newDualModemCall = (getFgPhone().getMySimId() == PhoneConstants.GEMINI_SIM_2) ? 1 : 0;
            if (newDualModemCall != mDualModemCall) {
                //Do not need to change to NORMAL mode
                //setAudioModeDualModem(mDualModemCall, AudioManager.MODE_NORMAL);
                mDualModemCall = newDualModemCall;
                Log.d(LOG_TAG, "set mDualModemCall = " + mDualModemCall);
                }
            setAudioModeDualModem(mDualModemCall, AudioManager.MODE_IN_CALL);
        } else {		
           setAudioMode(mode);
        }
    }

    private Context getContext() {
        Phone defaultPhone = getDefaultPhone();
        return ((defaultPhone == null) ? null : defaultPhone.getContext());
    }

    /**
     * Register phone states notificaitons.
     * 
     * M: Refine this method for Gemini and Dualtalk.
     */
    public void registerForPhoneStates(Phone phone) {
        // for common events supported by all phones
        if (FeatureOption.MTK_GEMINI_SUPPORT == true && !(phone instanceof SipPhone)) {
            if(phone instanceof GeminiPhone) {
                int offset;        	
                int count = (MAXIMUM_SIM_COUNT < PhoneConstants.GEMINI_SIM_NUM) ? MAXIMUM_SIM_COUNT : PhoneConstants.GEMINI_SIM_NUM;
                Log.d(LOG_TAG, "[registerForPhoneStates]count = " + count);
                for (int i = 0; i < count; i++) {
                    offset = i * NOTIFICATION_ID_OFFSET;
                    ((GeminiPhone)phone).registerForPreciseCallStateChangedGemini(mHandler, EVENT_PRECISE_CALL_STATE_CHANGED + offset, null, PhoneConstants.GEMINI_SIM_1 + i);
                    ((GeminiPhone)phone).registerForDisconnectGemini(mHandler, EVENT_DISCONNECT + offset, null, PhoneConstants.GEMINI_SIM_1 + i);
                    ((GeminiPhone)phone).registerForNewRingingConnectionGemini(mHandler, EVENT_NEW_RINGING_CONNECTION + offset, null, PhoneConstants.GEMINI_SIM_1 + i);
                    ((GeminiPhone)phone).registerForUnknownConnectionGemini(mHandler, EVENT_UNKNOWN_CONNECTION + offset, null, PhoneConstants.GEMINI_SIM_1 + i);
                    ((GeminiPhone)phone).registerForIncomingRingGemini(mHandler, EVENT_INCOMING_RING + offset, null, PhoneConstants.GEMINI_SIM_1 + i);
                    ((GeminiPhone)phone).registerForRingbackToneGemini(mHandler, EVENT_RINGBACK_TONE + offset, null, PhoneConstants.GEMINI_SIM_1 + i);
                    ((GeminiPhone)phone).registerForInCallVoicePrivacyOnGemini(mHandler, EVENT_IN_CALL_VOICE_PRIVACY_ON + offset, null, PhoneConstants.GEMINI_SIM_1 + i);
                    ((GeminiPhone)phone).registerForInCallVoicePrivacyOffGemini(mHandler, EVENT_IN_CALL_VOICE_PRIVACY_OFF + offset, null, PhoneConstants.GEMINI_SIM_1 + i);
                    ((GeminiPhone)phone).registerForDisplayInfoGemini(mHandler, EVENT_DISPLAY_INFO + offset, null, PhoneConstants.GEMINI_SIM_1 + i);
                    ((GeminiPhone)phone).registerForSignalInfoGemini(mHandler, EVENT_SIGNAL_INFO + offset, null, PhoneConstants.GEMINI_SIM_1 + i);
                    ((GeminiPhone)phone).registerForMmiInitiateGemini(mHandler, EVENT_MMI_INITIATE + offset, null, PhoneConstants.GEMINI_SIM_1 + i);
                    ((GeminiPhone)phone).registerForMmiCompleteGemini(mHandler, EVENT_MMI_COMPLETE + offset, null, PhoneConstants.GEMINI_SIM_1 + i);
                    ((GeminiPhone)phone).registerForSuppServiceFailedGemini(mHandler, EVENT_SUPP_SERVICE_FAILED + offset, null, PhoneConstants.GEMINI_SIM_1 + i);
                    ((GeminiPhone)phone).registerForServiceStateChangedGemini(mHandler, EVENT_SERVICE_STATE_CHANGED + offset, null, PhoneConstants.GEMINI_SIM_1 + i);
                    ((GeminiPhone)phone).setOnPostDialCharacterGemini(mHandler, EVENT_POST_DIAL_CHARACTER + offset, null, PhoneConstants.GEMINI_SIM_1 + i);
                
                    /* MTK proprietary start */
                    ((GeminiPhone)phone).registerForSpeechInfoGemini(mHandler, EVENT_SPEECH_INFO + offset, null, PhoneConstants.GEMINI_SIM_1 + i);
                    ((GeminiPhone)phone).registerForVtStatusInfoGemini(mHandler, EVENT_VT_STATUS_INFO + offset, null, PhoneConstants.GEMINI_SIM_1 + i);
                    ((GeminiPhone)phone).registerForVtRingInfoGemini(mHandler, EVENT_VT_RING_INFO + offset, null, PhoneConstants.GEMINI_SIM_1 + i);
                    ((GeminiPhone)phone).registerForCrssSuppServiceNotificationGemini(mHandler, EVENT_CRSS_SUPP_SERVICE_NOTIFICATION + offset, null, PhoneConstants.GEMINI_SIM_1 + i);
                    ((GeminiPhone)phone).registerForSuppServiceNotificationGemini(mHandler, EVENT_SUPP_SERVICE_NOTIFICATION + offset, null, PhoneConstants.GEMINI_SIM_1 + i);
                    ((GeminiPhone)phone).registerForVtReplaceDisconnectGemini(mHandler, EVENT_VT_REPLACE_DISCONNECT + offset, null, PhoneConstants.GEMINI_SIM_1 + i);
                    ((GeminiPhone)phone).registerForVoiceCallIncomingIndicationGemini(mHandler, EVENT_VOICE_CALL_INCOMING_INDICATION + offset, null, PhoneConstants.GEMINI_SIM_1 + i);
                    /* MTK proprietary end */
                }

                /* 
                   InCallScreen only register notification for one phone, so we need to register notifications of phone 1 for phone 2 
                */
                if (FeatureOption.MTK_BSP_PACKAGE == true) {
                   Log.d(LOG_TAG, "[BSPPackage]Register notification for Phone 2");
                   ((GeminiPhone)phone).registerForPreciseCallStateChangedGemini(mHandler, EVENT_PRECISE_CALL_STATE_CHANGED, null, PhoneConstants.GEMINI_SIM_2);
                   ((GeminiPhone)phone).registerForDisconnectGemini(mHandler, EVENT_DISCONNECT, null, PhoneConstants.GEMINI_SIM_2);
                   ((GeminiPhone)phone).registerForNewRingingConnectionGemini(mHandler, EVENT_NEW_RINGING_CONNECTION, null, PhoneConstants.GEMINI_SIM_2);
                   ((GeminiPhone)phone).registerForUnknownConnectionGemini(mHandler, EVENT_UNKNOWN_CONNECTION, null, PhoneConstants.GEMINI_SIM_2);
                   ((GeminiPhone)phone).registerForIncomingRingGemini(mHandler, EVENT_INCOMING_RING, null, PhoneConstants.GEMINI_SIM_2);
                   ((GeminiPhone)phone).registerForRingbackToneGemini(mHandler, EVENT_RINGBACK_TONE, null, PhoneConstants.GEMINI_SIM_2);
                   ((GeminiPhone)phone).registerForInCallVoicePrivacyOnGemini(mHandler, EVENT_IN_CALL_VOICE_PRIVACY_ON, null, PhoneConstants.GEMINI_SIM_2);
                   ((GeminiPhone)phone).registerForInCallVoicePrivacyOffGemini(mHandler, EVENT_IN_CALL_VOICE_PRIVACY_OFF, null, PhoneConstants.GEMINI_SIM_2);
                   ((GeminiPhone)phone).registerForDisplayInfoGemini(mHandler, EVENT_DISPLAY_INFO, null, PhoneConstants.GEMINI_SIM_2);
                   ((GeminiPhone)phone).registerForSignalInfoGemini(mHandler, EVENT_SIGNAL_INFO, null, PhoneConstants.GEMINI_SIM_2);
                   ((GeminiPhone)phone).registerForMmiInitiateGemini(mHandler, EVENT_MMI_INITIATE, null, PhoneConstants.GEMINI_SIM_2);
                   ((GeminiPhone)phone).registerForMmiCompleteGemini(mHandler, EVENT_MMI_COMPLETE, null, PhoneConstants.GEMINI_SIM_2);
                   ((GeminiPhone)phone).registerForSuppServiceFailedGemini(mHandler, EVENT_SUPP_SERVICE_FAILED, null, PhoneConstants.GEMINI_SIM_2);
                   ((GeminiPhone)phone).registerForServiceStateChangedGemini(mHandler, EVENT_SERVICE_STATE_CHANGED, null, PhoneConstants.GEMINI_SIM_2);
                   ((GeminiPhone)phone).setOnPostDialCharacterGemini(mHandler, EVENT_POST_DIAL_CHARACTER, null, PhoneConstants.GEMINI_SIM_2);
                   ((GeminiPhone)phone).registerForSpeechInfoGemini(mHandler, EVENT_SPEECH_INFO, null, PhoneConstants.GEMINI_SIM_2);
                   ((GeminiPhone)phone).registerForVtStatusInfoGemini(mHandler, EVENT_VT_STATUS_INFO, null, PhoneConstants.GEMINI_SIM_2);
                   ((GeminiPhone)phone).registerForVtRingInfoGemini(mHandler, EVENT_VT_RING_INFO, null, PhoneConstants.GEMINI_SIM_2);
                   ((GeminiPhone)phone).registerForCrssSuppServiceNotificationGemini(mHandler, EVENT_CRSS_SUPP_SERVICE_NOTIFICATION, null, PhoneConstants.GEMINI_SIM_2);
                   ((GeminiPhone)phone).registerForSuppServiceNotificationGemini(mHandler, EVENT_SUPP_SERVICE_NOTIFICATION, null, PhoneConstants.GEMINI_SIM_2);
                   ((GeminiPhone)phone).registerForVtReplaceDisconnectGemini(mHandler, EVENT_VT_REPLACE_DISCONNECT, null, PhoneConstants.GEMINI_SIM_2);
                   ((GeminiPhone)phone).registerForVoiceCallIncomingIndicationGemini(mHandler, EVENT_VOICE_CALL_INCOMING_INDICATION, null, PhoneConstants.GEMINI_SIM_2);
                }
                // for events supported only by CDMA phone
                if (FeatureOption.EVDO_DT_SUPPORT) {
                   ((GeminiPhone)phone).registerForCallWaitingGemini(mHandler, EVENT_CALL_WAITING2, null, PhoneConstants.GEMINI_SIM_2); 
                   ((GeminiPhone)phone).registerForCdmaOtaStatusChangeGemini(mHandler, EVENT_CDMA_OTA_STATUS_CHANGE2, null, PhoneConstants.GEMINI_SIM_2); 
                }
            } else { // non GeminiPhone
                registerSinglePhoneStates(phone);
            }
        } else {
            registerSinglePhoneStates(phone);
        }
    }

    /**
     * Unregister phone states notificaitons.
     * 
     * M: Refine this method for Gemini and Dualtalk.
     */
    public void unregisterForPhoneStates(Phone phone) {
        //  for common events supported by all phones
        if (FeatureOption.MTK_GEMINI_SUPPORT == true && !(phone instanceof SipPhone)) {
            if(phone instanceof GeminiPhone) {
                int count = (MAXIMUM_SIM_COUNT < PhoneConstants.GEMINI_SIM_NUM) ? MAXIMUM_SIM_COUNT : PhoneConstants.GEMINI_SIM_NUM;
                for (int i = 0; i < count; i++) {
                    ((GeminiPhone)phone).unregisterForPreciseCallStateChangedGemini(mHandler, PhoneConstants.GEMINI_SIM_1 + i);
                    ((GeminiPhone)phone).unregisterForDisconnectGemini(mHandler, PhoneConstants.GEMINI_SIM_1 + i);
                    ((GeminiPhone)phone).unregisterForNewRingingConnectionGemini(mHandler, PhoneConstants.GEMINI_SIM_1 + i);
                    ((GeminiPhone)phone).unregisterForUnknownConnectionGemini(mHandler, PhoneConstants.GEMINI_SIM_1 + i);
                    ((GeminiPhone)phone).unregisterForIncomingRingGemini(mHandler, PhoneConstants.GEMINI_SIM_1 + i);
                    ((GeminiPhone)phone).unregisterForRingbackToneGemini(mHandler, PhoneConstants.GEMINI_SIM_1 + i);
                    ((GeminiPhone)phone).unregisterForInCallVoicePrivacyOnGemini(mHandler, PhoneConstants.GEMINI_SIM_1 + i);
                    ((GeminiPhone)phone).unregisterForInCallVoicePrivacyOffGemini(mHandler, PhoneConstants.GEMINI_SIM_1 + i);
                    ((GeminiPhone)phone).unregisterForDisplayInfoGemini(mHandler, PhoneConstants.GEMINI_SIM_1 + i);
                    ((GeminiPhone)phone).unregisterForSignalInfoGemini(mHandler, PhoneConstants.GEMINI_SIM_1 + i);
                    ((GeminiPhone)phone).unregisterForMmiInitiateGemini(mHandler, PhoneConstants.GEMINI_SIM_1 + i);
                    ((GeminiPhone)phone).unregisterForMmiCompleteGemini(mHandler, PhoneConstants.GEMINI_SIM_1 + i);
                    ((GeminiPhone)phone).unregisterForSuppServiceFailedGemini(mHandler, PhoneConstants.GEMINI_SIM_1 + i);
                    ((GeminiPhone)phone).unregisterForServiceStateChangedGemini(mHandler, PhoneConstants.GEMINI_SIM_1 + i);
                    ((GeminiPhone)phone).setOnPostDialCharacterGemini(null, EVENT_POST_DIAL_CHARACTER, null, PhoneConstants.GEMINI_SIM_1 + i);

                    /* MTK proprietary start */
                    ((GeminiPhone)phone).unregisterForSpeechInfoGemini(mHandler, PhoneConstants.GEMINI_SIM_1 + i);
                    ((GeminiPhone)phone).unregisterForVtStatusInfoGemini(mHandler, PhoneConstants.GEMINI_SIM_1 + i);
                    ((GeminiPhone)phone).unregisterForVtRingInfoGemini(mHandler, PhoneConstants.GEMINI_SIM_1 + i);
                    ((GeminiPhone)phone).unregisterForCrssSuppServiceNotificationGemini(mHandler, PhoneConstants.GEMINI_SIM_1 + i);
                    ((GeminiPhone)phone).unregisterForSuppServiceNotificationGemini(mHandler, PhoneConstants.GEMINI_SIM_1 + i);
                    ((GeminiPhone)phone).unregisterForVtReplaceDisconnectGemini(mHandler, PhoneConstants.GEMINI_SIM_1 + i);
                    ((GeminiPhone)phone).unregisterForVoiceCallIncomingIndicationGemini(mHandler, PhoneConstants.GEMINI_SIM_1 + i);
                    /* MTK proprietary end */
                }

                // for events supported only by CDMA phone
                if (FeatureOption.EVDO_DT_SUPPORT) {
                    ((GeminiPhone)phone).unregisterForCallWaitingGemini(mHandler, PhoneConstants.GEMINI_SIM_2); 
                    ((GeminiPhone)phone).unregisterForCdmaOtaStatusChangeGemini(mHandler, PhoneConstants.GEMINI_SIM_2); 
                }
            } else { // non GeminiPhone
                unregisterSinglePhoneStates(phone);
            }
        } else {
            unregisterSinglePhoneStates(phone);
        }
    }

    private void registerSinglePhoneStates(Phone phone) {
        int simId;
        int eventDifference = EVENT_DISCONNECT2-EVENT_DISCONNECT;
        
        if(phone instanceof SipPhone) {
            simId = 0;
        }
        else {
            simId = phone.getMySimId();
        }
        
        /*
         * simId = 0,1,2,3; refer to PhoneConstants.GEMINI_SIM_1/2/3/4
         * the defined event valuses differ between each SIM is 100
         */
        phone.registerForPreciseCallStateChanged(mHandler, EVENT_PRECISE_CALL_STATE_CHANGED + eventDifference*simId, null);
        phone.registerForDisconnect(mHandler, EVENT_DISCONNECT + eventDifference*simId, null);
        phone.registerForNewRingingConnection(mHandler, EVENT_NEW_RINGING_CONNECTION + eventDifference*simId, null);
        phone.registerForUnknownConnection(mHandler, EVENT_UNKNOWN_CONNECTION + eventDifference*simId, null);
        phone.registerForIncomingRing(mHandler, EVENT_INCOMING_RING + eventDifference*simId, null);
        phone.registerForRingbackTone(mHandler, EVENT_RINGBACK_TONE + eventDifference*simId, null);
        phone.registerForInCallVoicePrivacyOn(mHandler, EVENT_IN_CALL_VOICE_PRIVACY_ON + eventDifference*simId, null);
        phone.registerForInCallVoicePrivacyOff(mHandler, EVENT_IN_CALL_VOICE_PRIVACY_OFF + eventDifference*simId, null);
        phone.registerForDisplayInfo(mHandler, EVENT_DISPLAY_INFO + eventDifference*simId, null);
        phone.registerForSignalInfo(mHandler, EVENT_SIGNAL_INFO + eventDifference*simId, null);
        phone.registerForResendIncallMute(mHandler, EVENT_RESEND_INCALL_MUTE + eventDifference*simId, null);
        phone.registerForMmiInitiate(mHandler, EVENT_MMI_INITIATE + eventDifference*simId, null);
        phone.registerForMmiComplete(mHandler, EVENT_MMI_COMPLETE + eventDifference*simId, null);
        phone.registerForSuppServiceFailed(mHandler, EVENT_SUPP_SERVICE_FAILED + eventDifference*simId, null);
        phone.registerForServiceStateChanged(mHandler, EVENT_SERVICE_STATE_CHANGED + eventDifference*simId, null);
        /* MTK proprietary start */
        phone.registerForSpeechInfo(mHandler, EVENT_SPEECH_INFO + eventDifference*simId, null);
        phone.registerForVtStatusInfo(mHandler, EVENT_VT_STATUS_INFO + eventDifference*simId, null);
        phone.registerForVtRingInfo(mHandler, EVENT_VT_RING_INFO + eventDifference*simId, null);
        phone.registerForCrssSuppServiceNotification(mHandler, EVENT_CRSS_SUPP_SERVICE_NOTIFICATION + eventDifference*simId, null);
        phone.registerForSuppServiceNotification(mHandler, EVENT_SUPP_SERVICE_NOTIFICATION + eventDifference*simId, null);
        phone.registerForVtReplaceDisconnect(mHandler, EVENT_VT_REPLACE_DISCONNECT + eventDifference*simId, null);
        phone.registerForVoiceCallIncomingIndication(mHandler, EVENT_VOICE_CALL_INCOMING_INDICATION + eventDifference*simId, null);
        /* MTK proprietary end */
        // for events supported only by GSM and CDMA phone
        if (phone.getPhoneType() == PhoneConstants.PHONE_TYPE_GSM ||
                phone.getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA) {
            phone.setOnPostDialCharacter(mHandler, EVENT_POST_DIAL_CHARACTER + eventDifference*simId, null);
        }

        // for events supported only by CDMA phone
        if (phone.getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA ){
            phone.registerForCdmaOtaStatusChange(mHandler, EVENT_CDMA_OTA_STATUS_CHANGE + eventDifference*simId, null);
            phone.registerForSubscriptionInfoReady(mHandler, EVENT_SUBSCRIPTION_INFO_READY + eventDifference*simId, null);
            phone.registerForCallWaiting(mHandler, EVENT_CALL_WAITING + eventDifference*simId, null);
            phone.registerForEcmTimerReset(mHandler, EVENT_ECM_TIMER_RESET + eventDifference*simId, null);
        }
    }

    private void unregisterSinglePhoneStates(Phone phone) {
        phone.unregisterForPreciseCallStateChanged(mHandler);
        phone.unregisterForDisconnect(mHandler);
        phone.unregisterForNewRingingConnection(mHandler);
        phone.unregisterForUnknownConnection(mHandler);
        phone.unregisterForIncomingRing(mHandler);
        phone.unregisterForRingbackTone(mHandler);
        phone.unregisterForInCallVoicePrivacyOn(mHandler);
        phone.unregisterForInCallVoicePrivacyOff(mHandler);
        phone.unregisterForDisplayInfo(mHandler);
        phone.unregisterForSignalInfo(mHandler);
        phone.unregisterForResendIncallMute(mHandler);
        phone.unregisterForMmiInitiate(mHandler);
        phone.unregisterForMmiComplete(mHandler);
        phone.unregisterForSuppServiceFailed(mHandler);
        phone.unregisterForServiceStateChanged(mHandler);
            /* MTK proprietary start */
            phone.unregisterForSpeechInfo(mHandler);
            phone.unregisterForVtStatusInfo(mHandler);
            phone.unregisterForVtRingInfo(mHandler);
            phone.unregisterForCrssSuppServiceNotification(mHandler);
            phone.unregisterForSuppServiceNotification(mHandler);
            phone.unregisterForVtReplaceDisconnect(mHandler);
        phone.unregisterForVoiceCallIncomingIndication(mHandler);
            /* MTK proprietary end */

        // for events supported only by GSM and CDMA phone
        if (phone.getPhoneType() == PhoneConstants.PHONE_TYPE_GSM ||
                phone.getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA) {
            phone.setOnPostDialCharacter(null, EVENT_POST_DIAL_CHARACTER, null);
        }

        // for events supported only by CDMA phone
        if (phone.getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA ){
            phone.unregisterForCdmaOtaStatusChange(mHandler);
            phone.unregisterForSubscriptionInfoReady(mHandler);
            phone.unregisterForCallWaiting(mHandler);
            phone.unregisterForEcmTimerReset(mHandler);
        }
      }

    /**
     * Answers a ringing or waiting call.
     *
     * Active call, if any, go on hold.
     * If active call can't be held, i.e., a background call of the same channel exists,
     * the active call will be hang up.
     *
     * Answering occurs asynchronously, and final notification occurs via
     * {@link #registerForPreciseCallStateChanged(android.os.Handler, int,
     * java.lang.Object) registerForPreciseCallStateChanged()}.
     *
     * @exception CallStateException when call is not ringing or waiting
     */
    public void acceptCall(Call ringingCall) throws CallStateException {
        Phone ringingPhone = ringingCall.getPhone();

        if (VDBG) {
            Log.d(LOG_TAG, "acceptCall(" +ringingCall + " from " + ringingCall.getPhone() + ")");
            Log.d(LOG_TAG, this.toString());
        }

        if ( hasActiveFgCall() ) {
            Phone activePhone = getActiveFgCall().getPhone();
            boolean hasBgCall = ! (activePhone.getBackgroundCall().isIdle());
            boolean sameChannel = (activePhone == ringingPhone);

            if (VDBG) {
                Log.d(LOG_TAG, "hasBgCall: "+ hasBgCall + "sameChannel:" + sameChannel);
            }
            
            /* Store current audio mode for recovering it when failed to hold active call */
            AudioManager audioManager = (AudioManager)getContext().getSystemService(Context.AUDIO_SERVICE);
            mCurrentAudioMode = audioManager.getMode();

            if (sameChannel && hasBgCall) {
                /// M: Solve ALPS00418813. 
                ///    Only hang up active call and keep held call to accept incoming call.
                activePhone.hangupActiveCall();
            } else if (!sameChannel && !hasBgCall) {
                /// M: Refine for Dualtalk. @{
                if (getActiveFgCallState().isDialing()) {
                    getActiveFgCall().hangup();
                }
                else {
                    /* Hang up the ringing call of active phone */
                    if (activePhone.getRingingCall().isRinging()) {
                       activePhone.getRingingCall().hangup();
                    }   

                    /* Switch active phone to held state */
                activePhone.switchHoldingAndActive();

                    /* Need to waiting for hold response before accepting incoming call */
                    Log.d(LOG_TAG, "Accept incoming call after the active call is held successfully.");
                    bWaitingForHoldRequest = true;
                    mWaitingReasonForHold = WaitingReasonForHold.ACCEPT_CALL;
                    mActiveCallToBeHeld = activePhone.getBackgroundCall();
                    mPhoneForWaitingHoldRequest = ringingPhone;
                }
                /// @}
            } else if (!sameChannel && hasBgCall) {
                /// M: Solve ALPS00418813. 
                ///    Only hang up active call and keep held call to accept incoming call.
                activePhone.hangupActiveCall();
            }
        }
        // M : remove google code by MR1.1
        /*
        Context context = getContext();
        if (context == null) {
            Log.d(LOG_TAG, "Speedup Audio Path enhancement: Context is null");
        } else if (context.getResources().getBoolean(
                com.android.internal.R.bool.config_speed_up_audio_on_mt_calls)) {
            Log.d(LOG_TAG, "Speedup Audio Path enhancement");
            AudioManager audioManager = (AudioManager)
                    context.getSystemService(Context.AUDIO_SERVICE);
            int currMode = audioManager.getMode();
            if ((currMode != AudioManager.MODE_IN_CALL) && !(ringingPhone instanceof SipPhone)) {
                Log.d(LOG_TAG, "setAudioMode Setting audio mode from " +
                                currMode + " to " + AudioManager.MODE_IN_CALL);
                audioManager.setMode(AudioManager.MODE_IN_CALL);
                mSpeedUpAudioForMtCall = true;
            }
        }

        ringingPhone.acceptCall();
        */ 
        // M : End 
        /* Check if need to waiting for hold response before accepting incoming call */
        if (!bWaitingForHoldRequest) {
            setAudioModeEarlierInAcceptCall();
            ringingPhone.acceptCall();
        }   

        if (VDBG) {
            Log.d(LOG_TAG, "End acceptCall(" +ringingCall + ")");
            Log.d(LOG_TAG, this.toString());
        }
    }

    private void setAudioModeEarlierInAcceptCall() {
        Context context = getContext();
        if (context == null) {
            Log.d(LOG_TAG, "Speedup Audio Path enhancement: Context is null");
        } else if (context.getResources().getBoolean(
                com.android.internal.R.bool.config_speed_up_audio_on_mt_calls)) {
            Log.d(LOG_TAG, "Speedup Audio Path enhancement");
            AudioManager audioManager = (AudioManager)
                    context.getSystemService(Context.AUDIO_SERVICE);
            int currMode = audioManager.getMode();
            if ((currMode != AudioManager.MODE_IN_CALL) && !(getRingingPhone() instanceof SipPhone)) {
                int newDualModemCall;
                
                Log.d(LOG_TAG, "setAudioMode Setting audio mode from " +
                                currMode + " to " + AudioManager.MODE_IN_CALL);
                
                newDualModemCall = (getFgPhone().getMySimId() == PhoneConstants.GEMINI_SIM_2) ? 1 : 0;
                if (newDualModemCall != mDualModemCall) {
                    mDualModemCall = newDualModemCall;
                    Log.d(LOG_TAG, "set mDualModemCall = " + mDualModemCall);
                }            
                setAudioModeDualModem(mDualModemCall, AudioManager.MODE_IN_CALL);
                mSpeedUpAudioForMtCall = true;
            }
        }
    }
    
    /**
     * Reject (ignore) a ringing call. In GSM, this means UDUB
     * (User Determined User Busy). Reject occurs asynchronously,
     * and final notification occurs via
     * {@link #registerForPreciseCallStateChanged(android.os.Handler, int,
     * java.lang.Object) registerForPreciseCallStateChanged()}.
     *
     * @exception CallStateException when no call is ringing or waiting
     */
    public void rejectCall(Call ringingCall) throws CallStateException {
        if (VDBG) {
            Log.d(LOG_TAG, "rejectCall(" +ringingCall + ")");
            Log.d(LOG_TAG, this.toString());
        }

        Phone ringingPhone = ringingCall.getPhone();

        ringingPhone.rejectCall();

        if (VDBG) {
            Log.d(LOG_TAG, "End rejectCall(" +ringingCall + ")");
            Log.d(LOG_TAG, this.toString());
        }
    }

    /**
     * Places active call on hold, and makes held call active.
     * Switch occurs asynchronously and may fail.
     *
     * There are 4 scenarios
     * 1. only active call but no held call, aka, hold
     * 2. no active call but only held call, aka, unhold
     * 3. both active and held calls from same phone, aka, swap
     * 4. active and held calls from different phones, aka, phone swap
     *
     * Final notification occurs via
     * {@link #registerForPreciseCallStateChanged(android.os.Handler, int,
     * java.lang.Object) registerForPreciseCallStateChanged()}.
     *
     * @exception CallStateException if active call is ringing, waiting, or
     * dialing/alerting, or heldCall can't be active.
     * In these cases, this operation may not be performed.
     */
    public void switchHoldingAndActive(Call heldCall) throws CallStateException {
        Phone activePhone = null;
        Phone heldPhone = null;

        if (VDBG) {
            Log.d(LOG_TAG, "switchHoldingAndActive(" +heldCall + ")");
            Log.d(LOG_TAG, this.toString());
        }

        if (hasActiveFgCall()) {
            activePhone = getActiveFgCall().getPhone();
        }

        if (heldCall != null) {
            heldPhone = heldCall.getPhone();
        }

        if (activePhone != null) {
            activePhone.switchHoldingAndActive();
        }

        if (heldPhone != null && heldPhone != activePhone) {
            if (activePhone != null) {
                /* Need to waiting for hold response before accepting incoming call */
                Log.d(LOG_TAG, "Switch held call to active one after the active call is held successfully.");
                bWaitingForHoldRequest = true;
                mWaitingReasonForHold = WaitingReasonForHold.SWITCH_CALL;
                mActiveCallToBeHeld = activePhone.getBackgroundCall();
                mPhoneForWaitingHoldRequest = heldPhone;
            }
            else {
                heldPhone.switchHoldingAndActive();
            }
        }

        if (VDBG) {
            Log.d(LOG_TAG, "End switchHoldingAndActive(" +heldCall + ")");
            Log.d(LOG_TAG, this.toString());
        }
    }

    /**
     * Hangup foreground call and resume the specific background call
     *
     * Note: this is noop if there is no foreground call or the heldCall is null
     *
     * @param heldCall to become foreground
     * @throws CallStateException
     */
    public void hangupForegroundResumeBackground(Call heldCall) throws CallStateException {
        Phone foregroundPhone = null;
        Phone backgroundPhone = null;

        if (VDBG) {
            Log.d(LOG_TAG, "hangupForegroundResumeBackground(" +heldCall + ")");
            Log.d(LOG_TAG, this.toString());
        }

        if (hasActiveFgCall()) {
            foregroundPhone = getFgPhone();
            if (heldCall != null) {
                backgroundPhone = heldCall.getPhone();
                if (foregroundPhone == backgroundPhone) {
                    getActiveFgCall().hangup();
                } else {
                // the call to be hangup and resumed belongs to different phones
                    getActiveFgCall().hangup();
                    switchHoldingAndActive(heldCall);
                }
            }
        }

        if (VDBG) {
            Log.d(LOG_TAG, "End hangupForegroundResumeBackground(" +heldCall + ")");
            Log.d(LOG_TAG, this.toString());
        }
    }

    /**
     * Whether or not the phone can conference in the current phone
     * state--that is, one call holding and one call active.
     * @return true if the phone can conference; false otherwise.
     */
    public boolean canConference(Call heldCall) {
        Phone activePhone = null;
        Phone heldPhone = null;

        if (hasActiveFgCall()) {
            activePhone = getActiveFgCall().getPhone();
        }

        if (heldCall != null) {
            heldPhone = heldCall.getPhone();
        }

        /// M: [mtk04070][111121][ALPS00093395]Only calls belong the same phone can be conferenced. @{
        if (heldPhone != null && activePhone != null) {
        return heldPhone.getClass().equals(activePhone.getClass());
        } else {
            return false;
        }
        /// @}
    }

    /**
     * Conferences holding and active. Conference occurs asynchronously
     * and may fail. Final notification occurs via
     * {@link #registerForPreciseCallStateChanged(android.os.Handler, int,
     * java.lang.Object) registerForPreciseCallStateChanged()}.
     *
     * @exception CallStateException if canConference() would return false.
     * In these cases, this operation may not be performed.
     */
    public void conference(Call heldCall) throws CallStateException {

        if (VDBG) {
            Log.d(LOG_TAG, "conference(" +heldCall + ")");
            Log.d(LOG_TAG, this.toString());
        }


        Phone fgPhone = getFgPhone();
        if (fgPhone instanceof SipPhone) {
            ((SipPhone) fgPhone).conference(heldCall);
        } else if (canConference(heldCall)) {
            fgPhone.conference();
        } else {
            throw(new CallStateException("Can't conference foreground and selected background call"));
        }

        if (VDBG) {
            Log.d(LOG_TAG, "End conference(" +heldCall + ")");
            Log.d(LOG_TAG, this.toString());
        }

    }

    /**
     * Initiate a new voice connection. This happens asynchronously, so you
     * cannot assume the audio path is connected (or a call index has been
     * assigned) until PhoneStateChanged notification has occurred.
     *
     * @exception CallStateException if a new outgoing call is not currently
     * possible because no more call slots exist or a call exists that is
     * dialing, alerting, ringing, or waiting.  Other errors are
     * handled asynchronously.
     */
    public Connection dial(Phone phone, String dialString) throws CallStateException {
        Phone basePhone = getPhoneBase(phone);
        Connection result;

        if (VDBG) {
            Log.d(LOG_TAG, " dial(" + basePhone + ", "+ dialString + ")");
            Log.d(LOG_TAG, this.toString());
        }

        /// M: [mtk04070][111121][ALPS00093395]Add a parameter - dialString. @{
        if (!canDial(phone, dialString)) {
            throw new CallStateException("cannot dial in current state");
        }
        /// @}

        if ( hasActiveFgCall() ) {
            Phone activePhone = getActiveFgCall().getPhone();
            boolean hasBgCall = !(activePhone.getBackgroundCall().isIdle());

            if (DBG) {
                Log.d(LOG_TAG, "hasBgCall: "+ hasBgCall + " sameChannel:" + (activePhone == basePhone));
            }

            if (activePhone != basePhone) {
                // check if USSD number
                boolean isUssdNumber = false;
                if(dialString.length() > 2) { // it maybe incall mmi code if length is less than 2
                    if(basePhone.getPhoneType() == PhoneConstants.PHONE_TYPE_GSM) {
                        isUssdNumber = GsmMmiCode.isUssdNumber(dialString, (GSMPhone)basePhone, null);
                    } else if (basePhone.getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA) {
                        CdmaMmiCode cdmaMmiCode = CdmaMmiCode.newFromDialString(dialString, (CDMAPhone)basePhone);
                        if (cdmaMmiCode != null) {
                            isUssdNumber = cdmaMmiCode.isUssdRequest();
                        }
                    }
                }
                if (!isUssdNumber) {
                    // non USSD string
                if (hasBgCall) {
                    Log.d(LOG_TAG, "Hangup");
                    /// M: Solve ALPS00418813. 
                    ///    Only hang up active call and keep held call to make new MO call.
                    activePhone.hangupActiveCall();
                } else {
                    Log.d(LOG_TAG, "Switch");
                    activePhone.switchHoldingAndActive();
                }
            }
        }
        }

        result = basePhone.dial(dialString);

        if (VDBG) {
            Log.d(LOG_TAG, "End dial(" + basePhone + ", "+ dialString + ")");
            Log.d(LOG_TAG, this.toString());
        }

        return result;
    }

    /**
     * Initiate a new voice connection. This happens asynchronously, so you
     * cannot assume the audio path is connected (or a call index has been
     * assigned) until PhoneStateChanged notification has occurred.
     *
     * @exception CallStateException if a new outgoing call is not currently
     * possible because no more call slots exist or a call exists that is
     * dialing, alerting, ringing, or waiting.  Other errors are
     * handled asynchronously.
     */
    public Connection dial(Phone phone, String dialString, UUSInfo uusInfo) throws CallStateException {
        return phone.dial(dialString, uusInfo);
    }

    /**
     * clear disconnect connection for each phone
     */
    public void clearDisconnected() {
        for(Phone phone : mPhones) {
            phone.clearDisconnected();
        }
    }

    /**
     * Phone can make a call only if ALL of the following are true:
     *        - Phone is not powered off
     *        - There's no incoming or waiting call
     *        - There's available call slot in either foreground or background
     *        - The foreground call is ACTIVE or IDLE or DISCONNECTED.
     *          (We mainly need to make sure it *isn't* DIALING or ALERTING.)
     * M: Consider in-call MMI command.
     * @param phone
     * @return true if the phone can make a new call
     */
    private boolean canDial(Phone phone, String dialString) {
        int serviceState = phone.getServiceState().getState();
        boolean hasRingingCall = hasActiveRingingCall();
        boolean hasActiveCall = hasActiveFgCall();
        boolean hasHoldingCall = hasActiveBgCall();
        boolean bIsInCallMmiCommands = isInCallMmiCommands(dialString);
        /* Solve [ALPS00360758]Can not add a call when active call and held call belongs to different phone(sim) */
        boolean allLinesTaken = (hasActiveCall && hasHoldingCall);
        if (FeatureOption.MTK_DT_SUPPORT == true) {
            allLinesTaken = allLinesTaken &&
                            (getFirstActiveCall(mForegroundCalls).getPhone() == getFirstActiveCall(mBackgroundCalls).getPhone());
        }
        /* If one of SIM has 1A1H, new MO call is not allowed even by another SIM */
        if (allLinesTaken && bIsInCallMmiCommands) {
            Phone activePhone = getFirstActiveCall(mForegroundCalls).getPhone();
            Phone dialPhone = getPhoneBase(phone);
            bIsInCallMmiCommands = (dialPhone == activePhone);
        }
        /*
         * DT false + 1A1H in different phone + dial InCallMmi, we reject the InCallMmi;
         * example: 1A in GSM, 1H in SIP, send '2' by GSM. It will cause two holding calls at the same time,
         *          and makes MMI abnormal. Planner suggests to reject the InCallMmi.
         */
        if (FeatureOption.MTK_DT_SUPPORT == false) {
            if(allLinesTaken && bIsInCallMmiCommands) {
                if(getFirstActiveCall(mForegroundCalls).getPhone() != getFirstActiveCall(mBackgroundCalls).getPhone()) {
                    bIsInCallMmiCommands = false;
                }
            }
        }
        Call.State fgCallState = getActiveFgCallState();

        /// M: Solve [ALPS00352152][6577JB][QHD][Call][Free Test]The ECC call will be ended when tap swap. @{
        boolean bECCExists = false;
        /* Can not make MO call when there exists ECC call */
        if (hasActiveCall) { 
           String activeCallAddress = getActiveFgCall().getEarliestConnection().getAddress();
           bECCExists = (PhoneNumberUtils.isEmergencyNumber(activeCallAddress) &&
                         !mPhoneNumberExt.isSpecialEmergencyNumber(activeCallAddress));
        }
		
        boolean result = (!bECCExists &&
                serviceState != ServiceState.STATE_POWER_OFF
                && !(hasRingingCall && !bIsInCallMmiCommands)
                && !(allLinesTaken && !bIsInCallMmiCommands)
                && ((fgCallState == Call.State.ACTIVE)
                    || (fgCallState == Call.State.IDLE)
                    || (fgCallState == Call.State.DISCONNECTED)
                    || (fgCallState == Call.State.ALERTING && isInCallMmiCommands(dialString))));

        if (result == false) {
            Log.d(LOG_TAG, "canDial serviceState=" + serviceState
                            + " hasRingingCall=" + hasRingingCall
                            + " hasActiveCall=" + hasActiveCall
                            + " hasHoldingCall=" + hasHoldingCall
                            + " allLinesTaken=" + allLinesTaken
                            + " fgCallState=" + fgCallState
                            + " bECCExists=" + bECCExists);
        }

        /// @}
        return result;
    }

    /**
     * Whether or not the phone can do explicit call transfer in the current
     * phone state--that is, one call holding and one call active.
     * @return true if the phone can do explicit call transfer; false otherwise.
     */
    public boolean canTransfer(Call heldCall) {
        Phone activePhone = null;
        Phone heldPhone = null;

        if (hasActiveFgCall()) {
            activePhone = getActiveFgCall().getPhone();
        }

        if (heldCall != null) {
            heldPhone = heldCall.getPhone();
        }

        return (heldPhone == activePhone && activePhone.canTransfer());
    }

    /**
     * Connects the held call and active call
     * Disconnects the subscriber from both calls
     *
     * Explicit Call Transfer occurs asynchronously
     * and may fail. Final notification occurs via
     * {@link #registerForPreciseCallStateChanged(android.os.Handler, int,
     * java.lang.Object) registerForPreciseCallStateChanged()}.
     *
     * @exception CallStateException if canTransfer() would return false.
     * In these cases, this operation may not be performed.
     */
    public void explicitCallTransfer(Call heldCall) throws CallStateException {
        if (VDBG) {
            Log.d(LOG_TAG, " explicitCallTransfer(" + heldCall + ")");
            Log.d(LOG_TAG, this.toString());
        }

        if (canTransfer(heldCall)) {
            heldCall.getPhone().explicitCallTransfer();
        }

        if (VDBG) {
            Log.d(LOG_TAG, "End explicitCallTransfer(" + heldCall + ")");
            Log.d(LOG_TAG, this.toString());
        }

    }

    /**
     * Returns a list of MMI codes that are pending for a phone. (They have initiated
     * but have not yet completed).
     * Presently there is only ever one.
     *
     * Use <code>registerForMmiInitiate</code>
     * and <code>registerForMmiComplete</code> for change notification.
     * @return null if phone doesn't have or support mmi code
     */
    public List<? extends MmiCode> getPendingMmiCodes(Phone phone) {
        Log.e(LOG_TAG, "getPendingMmiCodes not implemented");
        return null;
    }

    /**
     * Sends user response to a USSD REQUEST message.  An MmiCode instance
     * representing this response is sent to handlers registered with
     * registerForMmiInitiate.
     *
     * @param ussdMessge    Message to send in the response.
     * @return false if phone doesn't support ussd service
     */
    public boolean sendUssdResponse(Phone phone, String ussdMessge) {
        Log.e(LOG_TAG, "sendUssdResponse not implemented");
        return false;
    }

    /**
     * Mutes or unmutes the microphone for the active call. The microphone
     * is automatically unmuted if a call is answered, dialed, or resumed
     * from a holding state.
     *
     * @param muted true to mute the microphone,
     * false to activate the microphone.
     */

    public void setMute(boolean muted) {
        if (VDBG) {
            Log.d(LOG_TAG, " setMute(" + muted + ")");
            Log.d(LOG_TAG, this.toString());
        }

        if (hasActiveFgCall()) {
            getActiveFgCall().getPhone().setMute(muted);
        }

        if (VDBG) {
            Log.d(LOG_TAG, "End setMute(" + muted + ")");
            Log.d(LOG_TAG, this.toString());
        }
    }

    /**
     * Gets current mute status. Use
     * {@link #registerForPreciseCallStateChanged(android.os.Handler, int,
     * java.lang.Object) registerForPreciseCallStateChanged()}
     * as a change notifcation, although presently phone state changed is not
     * fired when setMute() is called.
     *
     * @return true is muting, false is unmuting
     */
    public boolean getMute() {
        if (hasActiveFgCall()) {
            return getActiveFgCall().getPhone().getMute();
        } else if (hasActiveBgCall()) {
            return getFirstActiveBgCall().getPhone().getMute();
        }
        return false;
    }

    /**
     * Enables or disables echo suppression.
     */
    public void setEchoSuppressionEnabled(boolean enabled) {
        if (VDBG) {
            Log.d(LOG_TAG, " setEchoSuppression(" + enabled + ")");
            //Solve [ALPS00336628]JE issue.
            //Log.d(LOG_TAG, this.toString());
        }

        if (hasActiveFgCall()) {
            getActiveFgCall().getPhone().setEchoSuppressionEnabled(enabled);
        }

        if (VDBG) {
            Log.d(LOG_TAG, "End setEchoSuppression(" + enabled + ")");
            //Solve [ALPS00336628]JE issue.
            //Log.d(LOG_TAG, this.toString());
        }
    }

    /**
     * Play a DTMF tone on the active call.
     *
     * @param c should be one of 0-9, '*' or '#'. Other values will be
     * silently ignored.
     * @return false if no active call or the active call doesn't support
     *         dtmf tone
     */
    public boolean sendDtmf(char c) {
        boolean result = false;

        if (VDBG) {
            Log.d(LOG_TAG, " sendDtmf(" + c + ")");
            Log.d(LOG_TAG, this.toString());
        }

        if (hasActiveFgCall()) {
            getActiveFgCall().getPhone().sendDtmf(c);
            result = true;
        }

        if (VDBG) {
            Log.d(LOG_TAG, "End sendDtmf(" + c + ")");
            Log.d(LOG_TAG, this.toString());
        }
        return result;
    }

    /**
     * Start to paly a DTMF tone on the active call.
     * or there is a playing DTMF tone.
     * @param c should be one of 0-9, '*' or '#'. Other values will be
     * silently ignored.
     *
     * @return false if no active call or the active call doesn't support
     *         dtmf tone
     */
    public boolean startDtmf(char c) {
        boolean result = false;

        if (VDBG) {
            Log.d(LOG_TAG, " startDtmf(" + c + ")");
            Log.d(LOG_TAG, this.toString());
        }

        if (hasActiveFgCall()) {
            getActiveFgCall().getPhone().startDtmf(c);
            result = true;
            /// M: Solve issue. @{
            /* Solve ALPS00281513, to avoid DTMF start request is sent, but stop request is ignore due to active is held, mtk04070, 20120515
               The following scenario may be happened, all hold requests will be cancelled, so need this flag to send stop request.
                 Hold active call(s) request
                 DTMF start request is sent(DTMF flag in ril is set to 1)
                 Hold command return OK
                 DTMF stop request is ignore due to no active call(DTMF flag is still 1)
            */
            Log.d(LOG_TAG, "dtmfRequestIsStarted = true");
            dtmfRequestIsStarted = true;
            /// @}
        }

        if (VDBG) {
            Log.d(LOG_TAG, "End startDtmf(" + c + ")");
            Log.d(LOG_TAG, this.toString());
        }

        return result;
    }

    /**
     * Stop the playing DTMF tone. Ignored if there is no playing DTMF
     * tone or no active call.
     */
    public void stopDtmf() {
        if (VDBG) {
            Log.d(LOG_TAG, " stopDtmf()" );
            Log.d(LOG_TAG, this.toString());
        }

        /// M: Solve issue. @{
        /* Solve ALPS00281513, to avoid DTMF start request is sent, but stop request is ignore due to active is held, mtk04070, 20120515 */
        if (hasActiveFgCall() || dtmfRequestIsStarted) {
            getFgPhone().stopDtmf();
            dtmfRequestIsStarted = false;
            Log.d(LOG_TAG, "dtmfRequestIsStarted = false");
        }
        /// @}

        if (VDBG) {
            Log.d(LOG_TAG, "End stopDtmf()");
            Log.d(LOG_TAG, this.toString());
        }
    }

    /**
     * send burst DTMF tone, it can send the string as single character or multiple character
     * ignore if there is no active call or not valid digits string.
     * Valid digit means only includes characters ISO-LATIN characters 0-9, *, #
     * The difference between sendDtmf and sendBurstDtmf is sendDtmf only sends one character,
     * this api can send single character and multiple character, also, this api has response
     * back to caller.
     *
     * @param dtmfString is string representing the dialing digit(s) in the active call
     * @param on the DTMF ON length in milliseconds, or 0 for default
     * @param off the DTMF OFF length in milliseconds, or 0 for default
     * @param onComplete is the callback message when the action is processed by BP
     *
     */
    public boolean sendBurstDtmf(String dtmfString, int on, int off, Message onComplete) {
        if (hasActiveFgCall()) {
            getActiveFgCall().getPhone().sendBurstDtmf(dtmfString, on, off, onComplete);
            return true;
        }
        return false;
    }

    /**
     * Notifies when a voice connection has disconnected, either due to local
     * or remote hangup or error.
     *
     *  Messages received from this will have the following members:<p>
     *  <ul><li>Message.obj will be an AsyncResult</li>
     *  <li>AsyncResult.userObj = obj</li>
     *  <li>AsyncResult.result = a Connection object that is
     *  no longer connected.</li></ul>
     */
    public void registerForDisconnect(Handler h, int what, Object obj) {
        mDisconnectRegistrants.addUnique(h, what, obj);
    }

    /**
     * Unregisters for voice disconnection notification.
     * Extraneous calls are tolerated silently
     */
    public void unregisterForDisconnect(Handler h){
        mDisconnectRegistrants.remove(h);
    }

    /**
     * Register for getting notifications for change in the Call State {@link Call.State}
     * This is called PreciseCallState because the call state is more precise than the
     * {@link PhoneConstants.State} which can be obtained using the {@link PhoneStateListener}
     *
     * Resulting events will have an AsyncResult in <code>Message.obj</code>.
     * AsyncResult.userData will be set to the obj argument here.
     * The <em>h</em> parameter is held only by a weak reference.
     */
    public void registerForPreciseCallStateChanged(Handler h, int what, Object obj){
        mPreciseCallStateRegistrants.addUnique(h, what, obj);
    }

    /**
     * Unregisters for voice call state change notifications.
     * Extraneous calls are tolerated silently.
     */
    public void unregisterForPreciseCallStateChanged(Handler h){
        mPreciseCallStateRegistrants.remove(h);
    }

    /**
     * Notifies when a previously untracked non-ringing/waiting connection has appeared.
     * This is likely due to some other entity (eg, SIM card application) initiating a call.
     */
    public void registerForUnknownConnection(Handler h, int what, Object obj){
        mUnknownConnectionRegistrants.addUnique(h, what, obj);
    }

    /**
     * Unregisters for unknown connection notifications.
     */
    public void unregisterForUnknownConnection(Handler h){
        mUnknownConnectionRegistrants.remove(h);
    }


    /**
     * Notifies when a new ringing or waiting connection has appeared.<p>
     *
     *  Messages received from this:
     *  Message.obj will be an AsyncResult
     *  AsyncResult.userObj = obj
     *  AsyncResult.result = a Connection. <p>
     *  Please check Connection.isRinging() to make sure the Connection
     *  has not dropped since this message was posted.
     *  If Connection.isRinging() is true, then
     *   Connection.getCall() == Phone.getRingingCall()
     */
    public void registerForNewRingingConnection(Handler h, int what, Object obj){
        mNewRingingConnectionRegistrants.addUnique(h, what, obj);
    }

    /**
     * Unregisters for new ringing connection notification.
     * Extraneous calls are tolerated silently
     */

    public void unregisterForNewRingingConnection(Handler h){
        mNewRingingConnectionRegistrants.remove(h);
    }

    /**
     * Notifies when an incoming call rings.<p>
     *
     *  Messages received from this:
     *  Message.obj will be an AsyncResult
     *  AsyncResult.userObj = obj
     *  AsyncResult.result = a Connection. <p>
     */
    public void registerForIncomingRing(Handler h, int what, Object obj){
        mIncomingRingRegistrants.addUnique(h, what, obj);
    }

    /**
     * Unregisters for ring notification.
     * Extraneous calls are tolerated silently
     */

    public void unregisterForIncomingRing(Handler h){
        mIncomingRingRegistrants.remove(h);
    }

    /**
     * Notifies when out-band ringback tone is needed.<p>
     *
     *  Messages received from this:
     *  Message.obj will be an AsyncResult
     *  AsyncResult.userObj = obj
     *  AsyncResult.result = boolean, true to start play ringback tone
     *                       and false to stop. <p>
     */
    public void registerForRingbackTone(Handler h, int what, Object obj){
        mRingbackToneRegistrants.addUnique(h, what, obj);
    }

    /**
     * Unregisters for ringback tone notification.
     */

    public void unregisterForRingbackTone(Handler h){
        mRingbackToneRegistrants.remove(h);
    }

    /**
     * Registers the handler to reset the uplink mute state to get
     * uplink audio.
     */
    public void registerForResendIncallMute(Handler h, int what, Object obj){
        mResendIncallMuteRegistrants.addUnique(h, what, obj);
    }

    /**
     * Unregisters for resend incall mute notifications.
     */
    public void unregisterForResendIncallMute(Handler h){
        mResendIncallMuteRegistrants.remove(h);
    }

    /**
     * Register for notifications of initiation of a new MMI code request.
     * MMI codes for GSM are discussed in 3GPP TS 22.030.<p>
     *
     * Example: If Phone.dial is called with "*#31#", then the app will
     * be notified here.<p>
     *
     * The returned <code>Message.obj</code> will contain an AsyncResult.
     *
     * <code>obj.result</code> will be an "MmiCode" object.
     */
    public void registerForMmiInitiate(Handler h, int what, Object obj){
        mMmiInitiateRegistrants.addUnique(h, what, obj);
    }

    /**
     * Unregisters for new MMI initiate notification.
     * Extraneous calls are tolerated silently
     */
    public void unregisterForMmiInitiate(Handler h){
        mMmiInitiateRegistrants.remove(h);
    }

    /**
     * Register for notifications that an MMI request has completed
     * its network activity and is in its final state. This may mean a state
     * of COMPLETE, FAILED, or CANCELLED.
     *
     * <code>Message.obj</code> will contain an AsyncResult.
     * <code>obj.result</code> will be an "MmiCode" object
     */
    public void registerForMmiComplete(Handler h, int what, Object obj){
        mMmiCompleteRegistrants.addUnique(h, what, obj);
    }

    /**
     * Unregisters for MMI complete notification.
     * Extraneous calls are tolerated silently
     */
    public void unregisterForMmiComplete(Handler h){
        mMmiCompleteRegistrants.remove(h);
    }

    /**
     * Registration point for Ecm timer reset
     * @param h handler to notify
     * @param what user-defined message code
     * @param obj placed in Message.obj
     */
    public void registerForEcmTimerReset(Handler h, int what, Object obj){
        mEcmTimerResetRegistrants.addUnique(h, what, obj);
    }

    /**
     * Unregister for notification for Ecm timer reset
     * @param h Handler to be removed from the registrant list.
     */
    public void unregisterForEcmTimerReset(Handler h){
        mEcmTimerResetRegistrants.remove(h);
    }

    /**
     * Register for ServiceState changed.
     * Message.obj will contain an AsyncResult.
     * AsyncResult.result will be a ServiceState instance
     */
    public void registerForServiceStateChanged(Handler h, int what, Object obj){
        mServiceStateChangedRegistrants.addUnique(h, what, obj);
    }

    /**
     * Unregisters for ServiceStateChange notification.
     * Extraneous calls are tolerated silently
     */
    public void unregisterForServiceStateChanged(Handler h){
        mServiceStateChangedRegistrants.remove(h);
    }

    /**
     * Register for notifications when a supplementary service attempt fails.
     * Message.obj will contain an AsyncResult.
     *
     * @param h Handler that receives the notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    public void registerForSuppServiceFailed(Handler h, int what, Object obj){
        mSuppServiceFailedRegistrants.addUnique(h, what, obj);
    }

    /**
     * Unregister for notifications when a supplementary service attempt fails.
     * Extraneous calls are tolerated silently
     *
     * @param h Handler to be removed from the registrant list.
     */
    public void unregisterForSuppServiceFailed(Handler h){
        mSuppServiceFailedRegistrants.remove(h);
    }

    /**
     * Register for notifications when a sInCall VoicePrivacy is enabled
     *
     * @param h Handler that receives the notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    public void registerForInCallVoicePrivacyOn(Handler h, int what, Object obj){
        mInCallVoicePrivacyOnRegistrants.addUnique(h, what, obj);
    }

    /**
     * Unregister for notifications when a sInCall VoicePrivacy is enabled
     *
     * @param h Handler to be removed from the registrant list.
     */
    public void unregisterForInCallVoicePrivacyOn(Handler h){
        mInCallVoicePrivacyOnRegistrants.remove(h);
    }

    /**
     * Register for notifications when a sInCall VoicePrivacy is disabled
     *
     * @param h Handler that receives the notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    public void registerForInCallVoicePrivacyOff(Handler h, int what, Object obj){
        mInCallVoicePrivacyOffRegistrants.addUnique(h, what, obj);
    }

    /**
     * Unregister for notifications when a sInCall VoicePrivacy is disabled
     *
     * @param h Handler to be removed from the registrant list.
     */
    public void unregisterForInCallVoicePrivacyOff(Handler h){
        mInCallVoicePrivacyOffRegistrants.remove(h);
    }

    /**
     * Register for notifications when CDMA call waiting comes
     *
     * @param h Handler that receives the notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    public void registerForCallWaiting(Handler h, int what, Object obj){
        mCallWaitingRegistrants.addUnique(h, what, obj);
    }

    /**
     * Unregister for notifications when CDMA Call waiting comes
     * @param h Handler to be removed from the registrant list.
     */
    public void unregisterForCallWaiting(Handler h){
        mCallWaitingRegistrants.remove(h);
    }


    /**
     * Register for signal information notifications from the network.
     * Message.obj will contain an AsyncResult.
     * AsyncResult.result will be a SuppServiceNotification instance.
     *
     * @param h Handler that receives the notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */

    public void registerForSignalInfo(Handler h, int what, Object obj){
        mSignalInfoRegistrants.addUnique(h, what, obj);
    }

    /**
     * Unregisters for signal information notifications.
     * Extraneous calls are tolerated silently
     *
     * @param h Handler to be removed from the registrant list.
     */
    public void unregisterForSignalInfo(Handler h){
        mSignalInfoRegistrants.remove(h);
    }

    /**
     * Register for display information notifications from the network.
     * Message.obj will contain an AsyncResult.
     * AsyncResult.result will be a SuppServiceNotification instance.
     *
     * @param h Handler that receives the notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    public void registerForDisplayInfo(Handler h, int what, Object obj){
        mDisplayInfoRegistrants.addUnique(h, what, obj);
    }

    /**
     * Unregisters for display information notifications.
     * Extraneous calls are tolerated silently
     *
     * @param h Handler to be removed from the registrant list.
     */
    public void unregisterForDisplayInfo(Handler h) {
        mDisplayInfoRegistrants.remove(h);
    }

    /**
     * Register for notifications when CDMA OTA Provision status change
     *
     * @param h Handler that receives the notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    public void registerForCdmaOtaStatusChange(Handler h, int what, Object obj){
        mCdmaOtaStatusChangeRegistrants.addUnique(h, what, obj);
    }

    /**
     * Unregister for notifications when CDMA OTA Provision status change
     * @param h Handler to be removed from the registrant list.
     */
    public void unregisterForCdmaOtaStatusChange(Handler h){
        mCdmaOtaStatusChangeRegistrants.remove(h);
    }

    /**
     * Registration point for subscription info ready
     * @param h handler to notify
     * @param what what code of message when delivered
     * @param obj placed in Message.obj
     */
    public void registerForSubscriptionInfoReady(Handler h, int what, Object obj){
        mSubscriptionInfoReadyRegistrants.addUnique(h, what, obj);
    }

    /**
     * Unregister for notifications for subscription info
     * @param h Handler to be removed from the registrant list.
     */
    public void unregisterForSubscriptionInfoReady(Handler h){
        mSubscriptionInfoReadyRegistrants.remove(h);
    }

    /**
     * Sets an event to be fired when the telephony system processes
     * a post-dial character on an outgoing call.<p>
     *
     * Messages of type <code>what</code> will be sent to <code>h</code>.
     * The <code>obj</code> field of these Message's will be instances of
     * <code>AsyncResult</code>. <code>Message.obj.result</code> will be
     * a Connection object.<p>
     *
     * Message.arg1 will be the post dial character being processed,
     * or 0 ('\0') if end of string.<p>
     *
     * If Connection.getPostDialState() == WAIT,
     * the application must call
     * {@link com.android.internal.telephony.Connection#proceedAfterWaitChar()
     * Connection.proceedAfterWaitChar()} or
     * {@link com.android.internal.telephony.Connection#cancelPostDial()
     * Connection.cancelPostDial()}
     * for the telephony system to continue playing the post-dial
     * DTMF sequence.<p>
     *
     * If Connection.getPostDialState() == WILD,
     * the application must call
     * {@link com.android.internal.telephony.Connection#proceedAfterWildChar
     * Connection.proceedAfterWildChar()}
     * or
     * {@link com.android.internal.telephony.Connection#cancelPostDial()
     * Connection.cancelPostDial()}
     * for the telephony system to continue playing the
     * post-dial DTMF sequence.<p>
     *
     */
    public void registerForPostDialCharacter(Handler h, int what, Object obj){
        mPostDialCharacterRegistrants.addUnique(h, what, obj);
    }

    public void unregisterForPostDialCharacter(Handler h){
        mPostDialCharacterRegistrants.remove(h);
    }

    /* APIs to access foregroudCalls, backgroudCalls, and ringingCalls
     * 1. APIs to access list of calls
     * 2. APIs to check if any active call, which has connection other than
     * disconnected ones, pleaser refer to Call.isIdle()
     * 3. APIs to return first active call
     * 4. APIs to return the connections of first active call
     * 5. APIs to return other property of first active call
     */

    /**
     * @return list of all ringing calls
     */
    public List<Call> getRingingCalls() {
        return Collections.unmodifiableList(mRingingCalls);
    }

    /**
     * @return list of all foreground calls
     */
    public List<Call> getForegroundCalls() {
        return Collections.unmodifiableList(mForegroundCalls);
    }

    /**
     * @return list of all background calls
     */
    public List<Call> getBackgroundCalls() {
        return Collections.unmodifiableList(mBackgroundCalls);
    }

    /**
     * Return true if there is at least one active foreground call
     */
    public boolean hasActiveFgCall() {
        return (getFirstActiveCall(mForegroundCalls) != null);
    }

    /**
     * Return true if there is at least one active background call
     */
    public boolean hasActiveBgCall() {
        // TODO since hasActiveBgCall may get called often
        // better to cache it to improve performance
        return (getFirstActiveCall(mBackgroundCalls) != null);
    }

    /**
     * Return true if there is at least one active ringing call
     *
     */
    public boolean hasActiveRingingCall() {
        return (getFirstActiveCall(mRingingCalls) != null);
    }

    /**
     * return the active foreground call from foreground calls
     *
     * Active call means the call is NOT in Call.State.IDLE
     *
     * 1. If there is active foreground call, return it
     * 2. If there is no active foreground call, return the
     *    foreground call associated with default phone, which state is IDLE.
     * 3. If there is no phone registered at all, return null.
     *
     */
    public Call getActiveFgCall() {
        Call call = getFirstNonIdleCall(mForegroundCalls);
        if (call == null) {
            call = (mDefaultPhone == null)
                    ? null
                    : mDefaultPhone.getForegroundCall();
        }
        return call;
    }

    // Returns the first call that is not in IDLE state. If both active calls
    // and disconnecting/disconnected calls exist, return the first active call.
    private Call getFirstNonIdleCall(List<Call> calls) {
        Call result = null;
        for (Call call : calls) {
            if (!call.isIdle()) {
                return call;
            } else if (call.getState() != Call.State.IDLE) {
                if (result == null) result = call;
            }
        }
        return result;
    }

    /**
     * return one active background call from background calls
     *
     * Active call means the call is NOT idle defined by Call.isIdle()
     *
     * 1. If there is only one active background call, return it
     * 2. If there is more than one active background call, return the first one
     * 3. If there is no active background call, return the background call
     *    associated with default phone, which state is IDLE.
     * 4. If there is no background call at all, return null.
     *
     * Complete background calls list can be get by getBackgroundCalls()
     */
    public Call getFirstActiveBgCall() {
        Call call = getFirstNonIdleCall(mBackgroundCalls);
        if (call == null) {
            call = (mDefaultPhone == null)
                    ? null
                    : mDefaultPhone.getBackgroundCall();
        }
        return call;
    }

    /**
     * return one active ringing call from ringing calls
     *
     * Active call means the call is NOT idle defined by Call.isIdle()
     *
     * 1. If there is only one active ringing call, return it
     * 2. If there is more than one active ringing call, return the first one
     * 3. If there is no active ringing call, return the ringing call
     *    associated with default phone, which state is IDLE.
     * 4. If there is no ringing call at all, return null.
     *
     * Complete ringing calls list can be get by getRingingCalls()
     */
    public Call getFirstActiveRingingCall() {
        Call call = getFirstNonIdleCall(mRingingCalls);
        if (call == null) {
            call = (mDefaultPhone == null)
                    ? null
                    : mDefaultPhone.getRingingCall();
        }
        return call;
    }

    /**
     * @return the state of active foreground call
     * return IDLE if there is no active foreground call
     */
    public Call.State getActiveFgCallState() {
        Call fgCall = getActiveFgCall();

        if (fgCall != null) {
            return fgCall.getState();
        }

        return Call.State.IDLE;
    }

    /**
     * @return the connections of active foreground call
     * return empty list if there is no active foreground call
     */
    public List<Connection> getFgCallConnections() {
        Call fgCall = getActiveFgCall();
        if ( fgCall != null) {
            return fgCall.getConnections();
        }
        return emptyConnections;
    }

    /**
     * @return the connections of active background call
     * return empty list if there is no active background call
     */
    public List<Connection> getBgCallConnections() {
        Call bgCall = getFirstActiveBgCall();
        if ( bgCall != null) {
            return bgCall.getConnections();
        }
        return emptyConnections;
    }

    /**
     * @return the latest connection of active foreground call
     * return null if there is no active foreground call
     */
    public Connection getFgCallLatestConnection() {
        Call fgCall = getActiveFgCall();
        if ( fgCall != null) {
            return fgCall.getLatestConnection();
        }
        return null;
    }

    /**
     * @return true if there is at least one Foreground call in disconnected state
     */
    public boolean hasDisconnectedFgCall() {
        return (getFirstCallOfState(mForegroundCalls, Call.State.DISCONNECTED) != null);
    }

    /**
     * @return true if there is at least one background call in disconnected state
     */
    public boolean hasDisconnectedBgCall() {
        return (getFirstCallOfState(mBackgroundCalls, Call.State.DISCONNECTED) != null);
    }

    /**
     * @return the first active call from a call list
     */
    private  Call getFirstActiveCall(ArrayList<Call> calls) {
        for (Call call : calls) {
            if (!call.isIdle()) {
                return call;
            }
        }
        return null;
    }

    /**
     * @return the first call in a the Call.state from a call list
     */
    private Call getFirstCallOfState(ArrayList<Call> calls, Call.State state) {
        for (Call call : calls) {
            if (call.getState() == state) {
                return call;
            }
        }
        return null;
    }

    private int getRingingCallCount() {
        int count = 0;

        for (Call call : mRingingCalls) {
            if (call.getState().isRinging()) {
                ++count;
            }
        }
        Log.d(LOG_TAG, "getRingingCallCount " + count); 
        return count;
    }

    private boolean hasMoreThanOneRingingCall() {
        
        if(getRingingCallCount() > 1) {
            return true;
        }
        return false;
    }

    private Handler mHandler = new Handler() {

        /// M: [mtk04070][111121][ALPS00093395]MTK modified for handling various events(gemini). @{
        @Override
        public void handleMessage(Message msg) {
            int index;

            if (VDBG) Log.d(LOG_TAG, " handleMessage msgid:" + msg.what);
            
            switch (msg.what) {
                case EVENT_DISCONNECT:
                case EVENT_DISCONNECT + NOTIFICATION_ID_OFFSET:
                case EVENT_DISCONNECT + (NOTIFICATION_ID_OFFSET * 2):
                case EVENT_DISCONNECT + (NOTIFICATION_ID_OFFSET * 3):
                    if (VDBG) Log.d(LOG_TAG, " handleMessage (EVENT_DISCONNECT)");
                    index = (msg.what - EVENT_DISCONNECT) / NOTIFICATION_ID_OFFSET;
                    mDisconnectRegistrantsGemini[index].notifyRegistrants((AsyncResult) msg.obj);
                    mDisconnectRegistrants.notifyRegistrants((AsyncResult) msg.obj);
                    handle3GSwitchLock();
                    /* We will not receive "ESPEECH: 0" during TRM/Modem reset, so need to clear espeech 
                       variables after the state is idle 
                    */
                    clearEspeechInfo();
                    break;
                case EVENT_PRECISE_CALL_STATE_CHANGED:
                case EVENT_PRECISE_CALL_STATE_CHANGED + NOTIFICATION_ID_OFFSET:
                case EVENT_PRECISE_CALL_STATE_CHANGED + (NOTIFICATION_ID_OFFSET * 2):
                case EVENT_PRECISE_CALL_STATE_CHANGED + (NOTIFICATION_ID_OFFSET * 3):
                    if (VDBG) Log.d(LOG_TAG, " handleMessage (EVENT_PRECISE_CALL_STATE_CHANGED)");
                    index = (msg.what - EVENT_PRECISE_CALL_STATE_CHANGED) / NOTIFICATION_ID_OFFSET;
                    mPreciseCallStateRegistrantsGemini[index].notifyRegistrants((AsyncResult) msg.obj);
                    mPreciseCallStateRegistrants.notifyRegistrants((AsyncResult) msg.obj);
                    handle3GSwitchLock();

                    try {
                          checkIfExistsFollowingAction();
                    } catch (Exception e) {
                          //Do nothing.
                    }
                    break;

                case EVENT_VOICE_CALL_INCOMING_INDICATION:
                case EVENT_VOICE_CALL_INCOMING_INDICATION + NOTIFICATION_ID_OFFSET:
                case EVENT_VOICE_CALL_INCOMING_INDICATION + (NOTIFICATION_ID_OFFSET * 2):
                case EVENT_VOICE_CALL_INCOMING_INDICATION + (NOTIFICATION_ID_OFFSET * 3):
                    if (VDBG) Log.d(LOG_TAG, " handleMessage (EVENT_VOICE_CALL_INCOMING_INDICATION)");

                    boolean isECCExists = false;
                    boolean isRejectNewRingCall = false;
                    if ((getActiveFgCall() != null) && 
                        (getActiveFgCall().getEarliestConnection() != null)) {
                        String activeCallAddress = getActiveFgCall().getEarliestConnection().getAddress();
                        isECCExists = (PhoneNumberUtils.isEmergencyNumber(activeCallAddress) &&
                                      !mPhoneNumberExt.isSpecialEmergencyNumber(activeCallAddress));
                        Log.d(LOG_TAG, "Reject incoming call since ECC call exists."); 
                    }
                    if(FeatureOption.MTK_DT_SUPPORT == false) {
                        if(getRingingCallCount() > 0 ) {
                            isRejectNewRingCall = true;
                        }
                    } else {
                        if(getRingingCallCount() > 1) {
                            isRejectNewRingCall = true;
                        }
                    }
                    // only gsmphone sends this event
                    GSMPhone gsmPhone = (GSMPhone) ((AsyncResult) msg.obj).result;
                    if (isECCExists || isRejectNewRingCall) {
                        gsmPhone.setIncomingCallIndicationResponse(false);
                    } else {
                        gsmPhone.setIncomingCallIndicationResponse(true);
                    }
                    break;
                    
                case EVENT_NEW_RINGING_CONNECTION:
                case EVENT_NEW_RINGING_CONNECTION + NOTIFICATION_ID_OFFSET:
                case EVENT_NEW_RINGING_CONNECTION + (NOTIFICATION_ID_OFFSET * 2):
                case EVENT_NEW_RINGING_CONNECTION + (NOTIFICATION_ID_OFFSET * 3):
                    if (VDBG) Log.d(LOG_TAG, " handleMessage (EVENT_NEW_RINGING_CONNECTION)");

                    /// M: For solving [ALPS00352152][6577JB][QHD][Call][Free Test]The ECC call will be ended when tap swap. @{
                    /* Reject incoming call since ECC call exists. */
                    boolean bECCExists = false;
                    if ((getActiveFgCall() != null) && 
                        (getActiveFgCall().getEarliestConnection() != null)) {
                        String activeCallAddress = getActiveFgCall().getEarliestConnection().getAddress();
                        bECCExists = (PhoneNumberUtils.isEmergencyNumber(activeCallAddress) &&
                                      !mPhoneNumberExt.isSpecialEmergencyNumber(activeCallAddress));
                        Log.d(LOG_TAG, "Reject incoming call since ECC call exists."); 
                    }
                    boolean bMoMtConflict = ((FeatureOption.MTK_DT_SUPPORT == false) &&
                                             (getActiveFgCallState().isDialing() || hasMoreThanOneRingingCall()));
                    if (bECCExists || bMoMtConflict) {
                        Connection c = (Connection) ((AsyncResult) msg.obj).result;
                        try {
                            Log.d(LOG_TAG, "silently drop incoming call: " + c.getCall());
                            c.getCall().hangup();
                        } catch (CallStateException e) {
                            Log.w(LOG_TAG, "new ringing connection", e);
                        }
                    } else {
                        index = (msg.what - EVENT_PRECISE_CALL_STATE_CHANGED) / NOTIFICATION_ID_OFFSET;
                        mNewRingingConnectionRegistrantsGemini[index].notifyRegistrants((AsyncResult) msg.obj);
                        mNewRingingConnectionRegistrants.notifyRegistrants((AsyncResult) msg.obj);
                    }
                    /// @}
                    break;
                case EVENT_UNKNOWN_CONNECTION:
                case EVENT_UNKNOWN_CONNECTION + NOTIFICATION_ID_OFFSET:
                case EVENT_UNKNOWN_CONNECTION + (NOTIFICATION_ID_OFFSET * 2):
                case EVENT_UNKNOWN_CONNECTION + (NOTIFICATION_ID_OFFSET * 3):
                    if (VDBG) Log.d(LOG_TAG, " handleMessage (EVENT_UNKNOWN_CONNECTION)");
                    index = (msg.what - EVENT_UNKNOWN_CONNECTION) / NOTIFICATION_ID_OFFSET;
                    mUnknownConnectionRegistrantsGemini[index].notifyRegistrants((AsyncResult) msg.obj);
                    mUnknownConnectionRegistrants.notifyRegistrants((AsyncResult) msg.obj);
                    break;
                case EVENT_INCOMING_RING:
                case EVENT_INCOMING_RING + NOTIFICATION_ID_OFFSET:
                case EVENT_INCOMING_RING + (NOTIFICATION_ID_OFFSET * 2):
                case EVENT_INCOMING_RING + (NOTIFICATION_ID_OFFSET * 3):
                    if (VDBG) Log.d(LOG_TAG, " handleMessage (EVENT_INCOMING_RING)");
                    // The event may come from RIL who's not aware of an ongoing fg call
                    if (!hasActiveFgCall()) {
                       index = (msg.what - EVENT_INCOMING_RING) / NOTIFICATION_ID_OFFSET;
                       mIncomingRingRegistrantsGemini[index].notifyRegistrants((AsyncResult) msg.obj);
                        mIncomingRingRegistrants.notifyRegistrants((AsyncResult) msg.obj);
                    }
                    break;
                case EVENT_RINGBACK_TONE:
                case EVENT_RINGBACK_TONE + NOTIFICATION_ID_OFFSET:
                case EVENT_RINGBACK_TONE + (NOTIFICATION_ID_OFFSET * 2):
                case EVENT_RINGBACK_TONE + (NOTIFICATION_ID_OFFSET * 3):
                    if (VDBG) Log.d(LOG_TAG, " handleMessage (EVENT_RINGBACK_TONE)");
                    index = (msg.what - EVENT_RINGBACK_TONE) / NOTIFICATION_ID_OFFSET;
                    mRingbackToneRegistrantsGemini[index].notifyRegistrants((AsyncResult) msg.obj);
                    mRingbackToneRegistrants.notifyRegistrants((AsyncResult) msg.obj);
                    break;
                case EVENT_IN_CALL_VOICE_PRIVACY_ON:
                case EVENT_IN_CALL_VOICE_PRIVACY_ON + NOTIFICATION_ID_OFFSET:
                case EVENT_IN_CALL_VOICE_PRIVACY_ON + (NOTIFICATION_ID_OFFSET * 2):
                case EVENT_IN_CALL_VOICE_PRIVACY_ON + (NOTIFICATION_ID_OFFSET * 3):
                    if (VDBG) Log.d(LOG_TAG, " handleMessage (EVENT_IN_CALL_VOICE_PRIVACY_ON)");
                    index = (msg.what - EVENT_IN_CALL_VOICE_PRIVACY_ON) / NOTIFICATION_ID_OFFSET;
                    mInCallVoicePrivacyOnRegistrantsGemini[index].notifyRegistrants((AsyncResult) msg.obj);
                    mInCallVoicePrivacyOnRegistrants.notifyRegistrants((AsyncResult) msg.obj);
                    break;
                case EVENT_IN_CALL_VOICE_PRIVACY_OFF:
                case EVENT_IN_CALL_VOICE_PRIVACY_OFF + NOTIFICATION_ID_OFFSET:
                case EVENT_IN_CALL_VOICE_PRIVACY_OFF + (NOTIFICATION_ID_OFFSET * 2):
                case EVENT_IN_CALL_VOICE_PRIVACY_OFF + (NOTIFICATION_ID_OFFSET * 3):
                    if (VDBG) Log.d(LOG_TAG, " handleMessage (EVENT_IN_CALL_VOICE_PRIVACY_OFF)");
                    index = (msg.what - EVENT_IN_CALL_VOICE_PRIVACY_OFF) / NOTIFICATION_ID_OFFSET;
                    mInCallVoicePrivacyOffRegistrantsGemini[index].notifyRegistrants((AsyncResult) msg.obj);
                    mInCallVoicePrivacyOffRegistrants.notifyRegistrants((AsyncResult) msg.obj);
                    break;
                case EVENT_CALL_WAITING:
                case EVENT_CALL_WAITING + NOTIFICATION_ID_OFFSET:
                case EVENT_CALL_WAITING + (NOTIFICATION_ID_OFFSET * 2):
                case EVENT_CALL_WAITING + (NOTIFICATION_ID_OFFSET * 3):
                    if (VDBG) Log.d(LOG_TAG, " handleMessage (EVENT_CALL_WAITING)");
                    index = (msg.what - EVENT_CALL_WAITING) / NOTIFICATION_ID_OFFSET;
                    mCallWaitingRegistrantsGemini[index].notifyRegistrants((AsyncResult) msg.obj);
                    mCallWaitingRegistrants.notifyRegistrants((AsyncResult) msg.obj);
                    break;
                case EVENT_DISPLAY_INFO:
                case EVENT_DISPLAY_INFO + NOTIFICATION_ID_OFFSET:
                case EVENT_DISPLAY_INFO + (NOTIFICATION_ID_OFFSET * 2):
                case EVENT_DISPLAY_INFO + (NOTIFICATION_ID_OFFSET * 3):
                    if (VDBG) Log.d(LOG_TAG, " handleMessage (EVENT_DISPLAY_INFO)");
                    index = (msg.what - EVENT_DISPLAY_INFO) / NOTIFICATION_ID_OFFSET;
                    mDisplayInfoRegistrantsGemini[index].notifyRegistrants((AsyncResult) msg.obj);
                    mDisplayInfoRegistrants.notifyRegistrants((AsyncResult) msg.obj);
                    break;
                case EVENT_SIGNAL_INFO:
                case EVENT_SIGNAL_INFO + NOTIFICATION_ID_OFFSET:
                case EVENT_SIGNAL_INFO + (NOTIFICATION_ID_OFFSET * 2):
                case EVENT_SIGNAL_INFO + (NOTIFICATION_ID_OFFSET * 3):
                    if (VDBG) Log.d(LOG_TAG, " handleMessage (EVENT_SIGNAL_INFO)");
                    index = (msg.what - EVENT_SIGNAL_INFO) / NOTIFICATION_ID_OFFSET;
                    mSignalInfoRegistrantsGemini[index].notifyRegistrants((AsyncResult) msg.obj);
                    mSignalInfoRegistrants.notifyRegistrants((AsyncResult) msg.obj);
                    break;
                case EVENT_CDMA_OTA_STATUS_CHANGE:
                case EVENT_CDMA_OTA_STATUS_CHANGE + NOTIFICATION_ID_OFFSET:
                case EVENT_CDMA_OTA_STATUS_CHANGE + (NOTIFICATION_ID_OFFSET * 2):
                case EVENT_CDMA_OTA_STATUS_CHANGE + (NOTIFICATION_ID_OFFSET * 3):
                    if (VDBG) Log.d(LOG_TAG, " handleMessage (EVENT_CDMA_OTA_STATUS_CHANGE)");
                    index = (msg.what - EVENT_CDMA_OTA_STATUS_CHANGE) / NOTIFICATION_ID_OFFSET;
                    mCdmaOtaStatusChangeRegistrantsGemini[index].notifyRegistrants((AsyncResult) msg.obj);
                    mCdmaOtaStatusChangeRegistrants.notifyRegistrants((AsyncResult) msg.obj);
                    break;
                case EVENT_RESEND_INCALL_MUTE:
                case EVENT_RESEND_INCALL_MUTE + NOTIFICATION_ID_OFFSET:
                case EVENT_RESEND_INCALL_MUTE + (NOTIFICATION_ID_OFFSET * 2):
                case EVENT_RESEND_INCALL_MUTE + (NOTIFICATION_ID_OFFSET * 3):
                    if (VDBG) Log.d(LOG_TAG, " handleMessage (EVENT_RESEND_INCALL_MUTE)");
                    index = (msg.what - EVENT_RESEND_INCALL_MUTE) / NOTIFICATION_ID_OFFSET;
                    mResendIncallMuteRegistrantsGemini[index].notifyRegistrants((AsyncResult) msg.obj);
                    mResendIncallMuteRegistrants.notifyRegistrants((AsyncResult) msg.obj);
                    break;
                case EVENT_MMI_INITIATE:
                case EVENT_MMI_INITIATE + NOTIFICATION_ID_OFFSET:
                case EVENT_MMI_INITIATE + (NOTIFICATION_ID_OFFSET * 2):
                case EVENT_MMI_INITIATE + (NOTIFICATION_ID_OFFSET * 3):
                    if (VDBG) Log.d(LOG_TAG, " handleMessage (EVENT_MMI_INITIATE)");
                    index = (msg.what - EVENT_MMI_INITIATE) / NOTIFICATION_ID_OFFSET;
                    mMmiInitiateRegistrantsGemini[index].notifyRegistrants((AsyncResult) msg.obj);
                    mMmiInitiateRegistrants.notifyRegistrants((AsyncResult) msg.obj);
                    break;
                case EVENT_MMI_COMPLETE:
                case EVENT_MMI_COMPLETE + NOTIFICATION_ID_OFFSET:
                case EVENT_MMI_COMPLETE + (NOTIFICATION_ID_OFFSET * 2):
                case EVENT_MMI_COMPLETE + (NOTIFICATION_ID_OFFSET * 3):
                    if (VDBG) Log.d(LOG_TAG, " handleMessage (EVENT_MMI_COMPLETE)");
                    index = (msg.what - EVENT_MMI_COMPLETE) / NOTIFICATION_ID_OFFSET;
                    mMmiCompleteRegistrantsGemini[index].notifyRegistrants((AsyncResult) msg.obj);
                    mMmiCompleteRegistrants.notifyRegistrants((AsyncResult) msg.obj);
                    break;
                case EVENT_ECM_TIMER_RESET:
                case EVENT_ECM_TIMER_RESET + NOTIFICATION_ID_OFFSET:
                case EVENT_ECM_TIMER_RESET + (NOTIFICATION_ID_OFFSET * 2):
                case EVENT_ECM_TIMER_RESET + (NOTIFICATION_ID_OFFSET * 3):
                    if (VDBG) Log.d(LOG_TAG, " handleMessage (EVENT_ECM_TIMER_RESET)");
                    index = (msg.what - EVENT_ECM_TIMER_RESET) / NOTIFICATION_ID_OFFSET;
                    mEcmTimerResetRegistrantsGemini[index].notifyRegistrants((AsyncResult) msg.obj);
                    mEcmTimerResetRegistrants.notifyRegistrants((AsyncResult) msg.obj);
                    break;
                case EVENT_SUBSCRIPTION_INFO_READY:
                case EVENT_SUBSCRIPTION_INFO_READY + NOTIFICATION_ID_OFFSET:
                case EVENT_SUBSCRIPTION_INFO_READY + (NOTIFICATION_ID_OFFSET * 2):
                case EVENT_SUBSCRIPTION_INFO_READY + (NOTIFICATION_ID_OFFSET * 3):
                    if (VDBG) Log.d(LOG_TAG, " handleMessage (EVENT_SUBSCRIPTION_INFO_READY)");
                    index = (msg.what - EVENT_SUBSCRIPTION_INFO_READY) / NOTIFICATION_ID_OFFSET;
                    mSubscriptionInfoReadyRegistrantsGemini[index].notifyRegistrants((AsyncResult) msg.obj);
                    mSubscriptionInfoReadyRegistrants.notifyRegistrants((AsyncResult) msg.obj);
                    break;
                case EVENT_SUPP_SERVICE_FAILED:
                case EVENT_SUPP_SERVICE_FAILED + NOTIFICATION_ID_OFFSET:
                case EVENT_SUPP_SERVICE_FAILED + (NOTIFICATION_ID_OFFSET * 2):
                case EVENT_SUPP_SERVICE_FAILED + (NOTIFICATION_ID_OFFSET * 3):
                    if (VDBG) Log.d(LOG_TAG, " handleMessage (EVENT_SUPP_SERVICE_FAILED)");
                    index = (msg.what - EVENT_SUPP_SERVICE_FAILED) / NOTIFICATION_ID_OFFSET;
                    mSuppServiceFailedRegistrantsGemini[index].notifyRegistrants((AsyncResult) msg.obj);
                    mSuppServiceFailedRegistrants.notifyRegistrants((AsyncResult) msg.obj);

                    /* Reset WaitingForHoldRequest variables since hold request is finished even if it is failed */
                    Log.d(LOG_TAG, "Reset WaitingForHoldRequest variables since hold request is failed.");
                    mWaitingReasonForHold = WaitingReasonForHold.NONE;
                    bWaitingForHoldRequest = false;

                    /* To recovery previous audio mode when failed to hold active call */
                    if (mCurrentAudioMode != -1) {
                        AudioManager audioManager = (AudioManager)getContext().getSystemService(Context.AUDIO_SERVICE);
                        audioManager.setMode(mCurrentAudioMode);
                        mCurrentAudioMode = -1;
                    }
					
                    break;
                case EVENT_SERVICE_STATE_CHANGED:
                case EVENT_SERVICE_STATE_CHANGED + NOTIFICATION_ID_OFFSET:
                case EVENT_SERVICE_STATE_CHANGED + (NOTIFICATION_ID_OFFSET * 2):
                case EVENT_SERVICE_STATE_CHANGED + (NOTIFICATION_ID_OFFSET * 3):
                    if (VDBG) Log.d(LOG_TAG, " handleMessage (EVENT_SERVICE_STATE_CHANGED)");
                    index = (msg.what - EVENT_SERVICE_STATE_CHANGED) / NOTIFICATION_ID_OFFSET;
                    mServiceStateChangedRegistrantsGemini[index].notifyRegistrants((AsyncResult) msg.obj);
                    mServiceStateChangedRegistrants.notifyRegistrants((AsyncResult) msg.obj);

                    /* We will not receive "ESPEECH: 0" during TRM/Modem reset, so need to clear espeech 
                       variables and switch audio mode to normal when the service state is
                       STATE_POWER_OFF or STATE_OUT_OF_SERVICE. 
                    */
                    clearEspeechInfo();
                    break;
                case EVENT_POST_DIAL_CHARACTER:
                case EVENT_POST_DIAL_CHARACTER + NOTIFICATION_ID_OFFSET:
                case EVENT_POST_DIAL_CHARACTER + (NOTIFICATION_ID_OFFSET * 2):
                case EVENT_POST_DIAL_CHARACTER + (NOTIFICATION_ID_OFFSET * 3):
                    // we need send the character that is being processed in msg.arg1
                    // so can't use notifyRegistrants()
                    if (VDBG) Log.d(LOG_TAG, " handleMessage (EVENT_POST_DIAL_CHARACTER)");
                    
                    index = (msg.what - EVENT_POST_DIAL_CHARACTER) / NOTIFICATION_ID_OFFSET;
                    for(int i=0; i < mPostDialCharacterRegistrantsGemini[index].size(); i++) {
                        Message notifyMsg;
                        notifyMsg = ((Registrant)mPostDialCharacterRegistrantsGemini[index].get(i)).messageForRegistrant();
                        notifyMsg.obj = msg.obj;
                        notifyMsg.arg1 = msg.arg1;
                        notifyMsg.sendToTarget();
                    }
                    for(int i=0; i < mPostDialCharacterRegistrants.size(); i++) {
                            Message notifyMsg;
                            notifyMsg = ((Registrant)mPostDialCharacterRegistrants.get(i)).messageForRegistrant();
                            notifyMsg.obj = msg.obj;
                            notifyMsg.arg1 = msg.arg1;
                            notifyMsg.sendToTarget();
                    }                        
                    break;
                /* MTK proprietary start */
                case EVENT_SPEECH_INFO:
                case EVENT_SPEECH_INFO + NOTIFICATION_ID_OFFSET:
                case EVENT_SPEECH_INFO + (NOTIFICATION_ID_OFFSET * 2):
                case EVENT_SPEECH_INFO + (NOTIFICATION_ID_OFFSET * 3):
                    if (VDBG) Log.d(LOG_TAG, " handleMessage (EVENT_SPEECH_INFO)");
		            handleSpeechInfo(msg);
                    break;
					
                case EVENT_VT_RING_INFO:
                case EVENT_VT_RING_INFO + NOTIFICATION_ID_OFFSET:
                case EVENT_VT_RING_INFO + (NOTIFICATION_ID_OFFSET * 2):
                case EVENT_VT_RING_INFO + (NOTIFICATION_ID_OFFSET * 3):
                    if (VDBG) Log.d(LOG_TAG, " handleMessage (EVENT_VT_RING_INFO)");
                    index = (msg.what - EVENT_VT_RING_INFO) / NOTIFICATION_ID_OFFSET;
                    mVtRingInfoRegistrantsGemini[index].notifyRegistrants((AsyncResult) msg.obj);
                    mVtRingInfoRegistrants.notifyRegistrants((AsyncResult) msg.obj);
                    break;
                case EVENT_VT_STATUS_INFO:
                case EVENT_VT_STATUS_INFO + NOTIFICATION_ID_OFFSET:
                case EVENT_VT_STATUS_INFO + (NOTIFICATION_ID_OFFSET * 2):
                case EVENT_VT_STATUS_INFO + (NOTIFICATION_ID_OFFSET * 3):
                    if (VDBG) Log.d(LOG_TAG, " handleMessage (EVENT_VT_STATUS_INFO)");
                    ///M: Solve [ALPS00418037][MT6589][WG+G][Call]There is no sound in Receiver/Speaker 
                    ///   if answer Voice call when Video call connected.
                    setVTSpeechCall((AsyncResult) msg.obj);
                    index = (msg.what - EVENT_VT_STATUS_INFO) / NOTIFICATION_ID_OFFSET;
                    mVtStatusInfoRegistrantsGemini[index].notifyRegistrants((AsyncResult) msg.obj);
                    mVtStatusInfoRegistrants.notifyRegistrants((AsyncResult) msg.obj);
                    break;
                case EVENT_CRSS_SUPP_SERVICE_NOTIFICATION:
                case EVENT_CRSS_SUPP_SERVICE_NOTIFICATION + NOTIFICATION_ID_OFFSET:
                case EVENT_CRSS_SUPP_SERVICE_NOTIFICATION + (NOTIFICATION_ID_OFFSET * 2):
                case EVENT_CRSS_SUPP_SERVICE_NOTIFICATION + (NOTIFICATION_ID_OFFSET * 3):
                    if (VDBG) Log.d(LOG_TAG, " handleMessage (EVENT_CRSS_SUPP_SERVICE_NOTIFICATION)");
                    index = (msg.what - EVENT_CRSS_SUPP_SERVICE_NOTIFICATION) / NOTIFICATION_ID_OFFSET;
                    mCrssSuppServiceNotificationRegistrantsGemini[index].notifyRegistrants((AsyncResult) msg.obj);
                    mCrssSuppServiceNotificationRegistrants.notifyRegistrants((AsyncResult) msg.obj);
                    break;
                case EVENT_SUPP_SERVICE_NOTIFICATION:
                case EVENT_SUPP_SERVICE_NOTIFICATION + NOTIFICATION_ID_OFFSET:
                case EVENT_SUPP_SERVICE_NOTIFICATION + (NOTIFICATION_ID_OFFSET * 2):
                case EVENT_SUPP_SERVICE_NOTIFICATION + (NOTIFICATION_ID_OFFSET * 3):
                    if (VDBG) Log.d(LOG_TAG, " handleMessage (EVENT_SUPP_SERVICE_NOTIFICATION)");
                    index = (msg.what - EVENT_SUPP_SERVICE_NOTIFICATION) / NOTIFICATION_ID_OFFSET;
                    mSuppServiceNotificationRegistrantsGemini[index].notifyRegistrants((AsyncResult) msg.obj);
                    mSuppServiceNotificationRegistrants.notifyRegistrants((AsyncResult) msg.obj);
                    break;
                case EVENT_VT_REPLACE_DISCONNECT:
                case EVENT_VT_REPLACE_DISCONNECT + NOTIFICATION_ID_OFFSET:
                case EVENT_VT_REPLACE_DISCONNECT + (NOTIFICATION_ID_OFFSET * 2):
                case EVENT_VT_REPLACE_DISCONNECT + (NOTIFICATION_ID_OFFSET * 3):
                    if (VDBG) Log.d(LOG_TAG, " handleMessage (EVENT_VT_REPLACE_DISCONNECT)");
                    index = (msg.what - EVENT_VT_REPLACE_DISCONNECT) / NOTIFICATION_ID_OFFSET;
                    mVtReplaceDisconnectRegistrantsGemini[index].notifyRegistrants((AsyncResult) msg.obj);
                    mVtReplaceDisconnectRegistrants.notifyRegistrants((AsyncResult) msg.obj);
                    break;
                /* MTK proprietary end */
            }
        }
        /// @}
    };

    @Override
    public String toString() {
        Call call;
        StringBuilder b = new StringBuilder();

        b.append("CallManager {");
        b.append("\nstate = " + getState());
        call = getActiveFgCall();
        b.append("\n- Foreground: " + getActiveFgCallState());
        b.append(" from " + call.getPhone());
        b.append("\n  Conn: ").append(getFgCallConnections());
        call = getFirstActiveBgCall();
        b.append("\n- Background: " + call.getState());
        b.append(" from " + call.getPhone());
        b.append("\n  Conn: ").append(getBgCallConnections());
        call = getFirstActiveRingingCall();
        b.append("\n- Ringing: " +call.getState());
        b.append(" from " + call.getPhone());

        for (Phone phone : getAllPhones()) {
            if (phone != null) {
                b.append("\nPhone: " + phone + ", name = " + phone.getPhoneName()
                        + ", state = " + phone.getState());
                call = phone.getForegroundCall();
                b.append("\n- Foreground: ").append(call);
                call = phone.getBackgroundCall();
                b.append(" Background: ").append(call);
                call = phone.getRingingCall();
                b.append(" Ringing: ").append(call);
            }
        }
        b.append("\n}");
        return b.toString();
    }

    /// M: [mtk04070][111121][ALPS00093395]MTK proprietary methods. @{
    private void setAudioMode(int mode) {
        Log.d(LOG_TAG, "setAudioMode enter...");
        Context context = getContext();
        boolean isVTCall = false;
        int curAudioMode;
        
        if (context == null) return;
        AudioManager audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        curAudioMode = audioManager.getMode();
        Log.d(LOG_TAG, "set mode = " + getNewAudioModeIfMDSwap(mode) + ", current mode = " + curAudioMode);
        // calling audioManager.setMode() multiple times in a short period of
        // time seems to break the audio recorder in in-call mode
        // [mtk08470][130123][ALPS00448221] get correct modem id if modem is cross mapping
        if (curAudioMode != getNewAudioModeIfMDSwap(mode)) {
            /* Request audio focus before setting the new mode */
            switch (mode) {
               case AudioManager.MODE_RINGTONE:
                    /// M: Solve [6577JB][Music] After set as "Silent"/"Meeting", the music go on play when incoming call comes. @{
                    // Always request audio focus even if audio profile is set to "Silent".
                    // only request audio focus if the ringtone is going to be heard
                    //if (audioManager.getStreamVolume(AudioManager.STREAM_RING) > 0) {
                        Log.d(LOG_TAG, "requestAudioFocus on STREAM_RING");
                        audioManager.requestAudioFocusForCall(AudioManager.STREAM_RING,
                                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
                    //}
                    /// @}
                    //MTK-START [mtk08470][130121][ALPS00446908] When make VT MO & voice MT call at same time, 
                    // it may cause VTSpeechCall in the incorrect status. We add some code here to fix the incorrect mode
                    if (FeatureOption.MTK_VT3G324M_SUPPORT == true) {
                        if(hasSetVtPara && !getRingingPhone().getRingingCall().getLatestConnection().isVideo()) {
                            int count = getFgCallConnections().size();
                            boolean hasAnyVTCallExist = false;
                            for(int i = 0; i < count; i++) {
                                Connection cn = getFgCallConnections().get(i);
                                if(cn.isVideo() && cn.isAlive()) {
                                    hasAnyVTCallExist = true;
                                    break;
                                }
                            }
                            if (hasAnyVTCallExist == false) {
                                Log.d(LOG_TAG, "[setAudioMode]No any VT connections when set RINGTONE mode, but VTSpeechCall is 1");
                                Log.d(LOG_TAG, "[setAudioMode]SetVTSpeechCall=0");
                                audioManager.setParameters("SetVTSpeechCall=0");
                                hasSetVtPara = false;
                                break;
                            }
                        }
                    }
                    //MTK-END [mtk08470][130121][ALPS00446908]MTK added
                    break;

                case AudioManager.MODE_IN_CALL:
                case AudioManager.MODE_IN_CALL_2:
                case AudioManager.MODE_IN_COMMUNICATION:
                    Log.d(LOG_TAG, "requestAudioFocus on STREAM_VOICE_CALL");
                    audioManager.requestAudioFocusForCall(AudioManager.STREAM_VOICE_CALL,
                            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
                    break;
            }

            if (FeatureOption.MTK_VT3G324M_SUPPORT == true) {
                int count = getFgCallConnections().size();
                if ((AudioManager.MODE_IN_CALL == mode) ||
                    (AudioManager.MODE_IN_CALL_2 == mode)) {
                    for(int i = 0; i < count; i++) {
                        Connection cn = getFgCallConnections().get(i);
                        isVTCall = (cn.isVideo() && cn.isAlive());
                        /* Check if "SetVTSpeechCall" parameter is set */
                        if (!hasSetVtPara && isVTCall) {
                            Log.d(LOG_TAG, "[setAudioMode]SetVTSpeechCall=1");
                            audioManager.setParameters("SetVTSpeechCall=1");
                            hasSetVtPara = true;
                            break;
                        }
                    }
                } else if (AudioManager.MODE_NORMAL == mode) {
                    if (hasSetVtPara) {
                        Log.d(LOG_TAG, "[setAudioMode]SetVTSpeechCall=0");
                        audioManager.setParameters("SetVTSpeechCall=0");
                        hasSetVtPara = false;
                    }
                }
            }

            /* Solve CR - ALPS00264465. Check if the audio mode set to normal is due to call disconnected */
            boolean fgIsAlive = getFgPhone().getForegroundCall().getState().isAlive();
            boolean bgIsAlive = getBgPhone().getBackgroundCall().getState().isAlive();
            //Log.d(LOG_TAG, "getFgPhone().getForegroundCall().getState() = " + getFgPhone().getForegroundCall().getState());
            //Log.d(LOG_TAG, "getBgPhone().getBackgroundCall().getState() = " + getBgPhone().getBackgroundCall().getState());
            if ((AudioManager.MODE_NORMAL == mode) && 
                (fgIsAlive || bgIsAlive) &&
                !isServiceStatePowerOff()) {
                /* Do not need to switch normal mode for held call */
                Log.d(LOG_TAG, "Do not need to switch normal mode for held call");
                return;
                //Log.d(LOG_TAG, "Should not delay 800 msec in audio driver since at least one call is not in idle state.");
                //audioManager.setParameters("SkipEndCallDelay=1");
                //audioManager.setParameters("RILSetHeadsetState=2");
            }
            
            /* Solve ALPS00488851 issue, mtk04070, 2013.03.08 */
            /* Solve ALPS00275770, ignore setting audio mode to IN_CALL if does not receive "ESPEECH: 1" */
            /*if ((isVTCall == false) &&
                ((mode == AudioManager.MODE_IN_CALL) || (mode == AudioManager.MODE_IN_CALL_2))) {
               if (isEspeechTurnedOff(mode)) {
        	        return;
               }   
            }*/

            /* Set headset state parameter for audio manager */
            /* Refer to AudioManager.java
               MODE_NORMAL : 0
               MODE_RINGTONE : 1
               MODE_IN_CALL : 2
               MODE_IN_COMMUNICATION : 3
               MODE_IN_CALL_2 : 4
            */
            String headsetState = "";
            final int value = (mode > AudioManager.MODE_RINGTONE) ? 2 : mode;
            /// M: [ALPS00383541]Update call state to Accdet directly. @{
            //headsetState = "RILSetHeadsetState=" + value;
            //audioManager.setParameters(headsetState);
            //Log.d(LOG_TAG, headsetState);
            /* Create thread to perform file I/O for solving 
               [ALPS00420387][MT6589TD][CMCC MTBF][SingleSim]binder.c:977
            */
            new Thread(new Runnable() {
		public void run() {
			String callStateFilePath = String.format("/sys/devices/platform/Accdet_Driver/driver/accdet_call_state");
			try{
				String state = String.valueOf(value);
				FileWriter fw = new FileWriter(callStateFilePath);
				fw.write(state);
				fw.close();
				Log.d(LOG_TAG, "Call state for Accdet is " + state);
			} catch (Exception e) {
				Log.e(LOG_TAG, "" , e);
			}
			
			//yucong add for set kpd as wake up source when phone calling
			String callStateFilePath2 = String.format("/sys/devices/platform/mtk-kpd/driver/kpd_call_state");
			try{
				String state2 = String.valueOf(value);
				FileWriter fw2 = new FileWriter(callStateFilePath2);
				fw2.write(state2);
				fw2.close();
				Log.d(LOG_TAG, "Call state for kpd is " + state2);
			} catch (Exception e) {
				Log.e(LOG_TAG, "" , e);
			}
                        
                        //Add by mtk01411 to notify the call state for Charging Battery protection
                        String callStateFilePath3 = String.format("/sys/devices/platform/battery/Charging_CallState");
                        try {
                            int call_existed = (value == 2) ? 1 : 0;
                            String state3 = String.valueOf(call_existed);
                            FileWriter fw3 = new FileWriter(callStateFilePath3);
                            fw3.write(state3);
                            fw3.close();
                            Log.d(LOG_TAG, "Call state for Charging Battery (call_existed=1,else=0) is " + state3); 
                        } catch (Exception e) {
                            Log.e(LOG_TAG, "" , e);
                        }                      
		}
            }).start();
            /// @}    
            //MTK-START [mtk08470][130123][ALPS00448221] get correct modem id if modem is cross mapping
            int newMode = getNewAudioModeIfMDSwap(mode);
            
            Log.d(LOG_TAG, "set AudioManager mode " + newMode);

            /// M : merge MR1.1
            if (AudioManager.MODE_RINGTONE == mode) {
                if (mSpeedUpAudioForMtCall) {
                    newMode = AudioManager.MODE_IN_CALL; // reset to default mode

                    // translate to in call mode if DT
                    if(FeatureOption.MTK_DT_SUPPORT == true) {
                        int newDualModemCall;
                        newDualModemCall = (getFgPhone().getMySimId() == PhoneConstants.GEMINI_SIM_2) ? 1 : 0;
                        if (newDualModemCall != mDualModemCall) {
                            mDualModemCall = newDualModemCall;
                            Log.d(LOG_TAG, "set mDualModemCall = " + mDualModemCall);
                        }
                        if (mDualModemCall == 1)
                        {
                           newMode = AudioManager.MODE_IN_CALL_2;
                        }
                        else
                        {
                           newMode = AudioManager.MODE_IN_CALL;
                        }
                    }
                }
            } // M : merge MR1.1 end
            if(curAudioMode != newMode) { // check if the same again
            audioManager.setMode(newMode);
            }
            
            //MTK-END [mtk08470][130123][ALPS00448221]MTK added
            if (AudioManager.MODE_RINGTONE != newMode) {
                /* Reset the variable when changing audio mode to in call or normal */
                mCurrentAudioMode = -1;
                mSpeedUpAudioForMtCall = false;
            }
        }/* audioManager.getMode() != mode */

        if (mode == AudioManager.MODE_NORMAL && getState() == PhoneConstants.State.IDLE) {
            Log.d(LOG_TAG, "abandonAudioFocus");
            // abandon audio focus after the mode has been set back to normal
            audioManager.abandonAudioFocusForCall();
        }
    }

    //MTK-START [mtk08470][130123][ALPS00448221] get correct modem id if modem is cross mapping
    // If MD is in cross mode : SIM1 map to MD2, SIM2 map to MD1, we need to get correct audio mode
    private int getNewAudioModeIfMDSwap(int oldMode) {
        int newMode = oldMode;

            /* Check if in call mode needs to be exchanged */
            if ((FeatureOption.MTK_DT_SUPPORT == true) && 
                (PhoneFactory.getFirstMD() == 2) &&
            (oldMode == AudioManager.MODE_IN_CALL || oldMode == AudioManager.MODE_IN_CALL_2)) {
           newMode = (oldMode == AudioManager.MODE_IN_CALL) ? AudioManager.MODE_IN_CALL_2 : AudioManager.MODE_IN_CALL;
            }
            /* If only modem2 is enabled, replace MODE_IN_CALL with MODE_IN_CALL_2 */
            if ((FeatureOption.MTK_ENABLE_MD1 == false) && 
                (FeatureOption.MTK_ENABLE_MD2 == true) &&
            (oldMode == AudioManager.MODE_IN_CALL)) {
            newMode = AudioManager.MODE_IN_CALL_2;
        }
        return newMode;
    }
    //MTK-END [mtk08470][130123][ALPS00448221]MTK added
            
    //Merge DualTalk code
    /**
     * Set audio mode with device ID. In dual modem atchitecture, shall set devId in the handling of speech_info
     * If devId is not assigned, it will set according to the call status which may not sync to the speech_info
     * ex. speech_info(sim1) on before sim1 call is set to active
     * @param devId audio device id (0 or 1 for dual modem platforms)
     * @param mode audio mode
     */
    private void setAudioModeDualModem(int devId, int mode) {
        Context context = getContext();
        if (context == null) return;
        AudioManager audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        
        if (FeatureOption.MTK_DT_SUPPORT == true) {   
           if (devId == 1)
           {
               mode = AudioManager.MODE_IN_CALL_2;
               //Log.d(LOG_TAG, "SecondModemPhoneCall=1");
               //audioManager.setParameters("SecondModemPhoneCall=1");
           }
           else
           {
               mode = AudioManager.MODE_IN_CALL;
               //Log.d(LOG_TAG, "SecondModemPhoneCall=0");
               //audioManager.setParameters("SecondModemPhoneCall=0");
           }
        }

        setAudioMode(mode);
    }    

    /* MTK proprietary start */
    public void registerForSpeechInfo(Handler h, int what, Object obj) {
        mSpeechInfoRegistrants.addUnique(h, what, obj);
    }

    public void unregisterForSpeechInfo(Handler h) {
        mSpeechInfoRegistrants.remove(h);
    }

    public void registerForVtStatusInfo(Handler h, int what, Object obj)  {
        mVtStatusInfoRegistrants.addUnique(h, what, obj);
    }

    public void unregisterForVtStatusInfo(Handler h)  {
        mVtStatusInfoRegistrants.remove(h);
    }

    public void registerForVtRingInfo(Handler h, int what, Object obj) {
        mVtRingInfoRegistrants.addUnique(h, what, obj);
    }
    
    public void unregisterForVtRingInfo(Handler h) {
        mVtRingInfoRegistrants.remove(h);
    }

    public void registerForCrssSuppServiceNotification(Handler h, int what, Object obj) {
        mCrssSuppServiceNotificationRegistrants.addUnique(h, what, obj);
    }

    public void unregisterForCrssSuppServiceNotification(Handler h) {
        mCrssSuppServiceNotificationRegistrants.remove(h);
    }

    public void registerForSuppServiceNotification(Handler h, int what, Object obj) {
        mSuppServiceNotificationRegistrants.addUnique(h, what, obj);
    }

    public void unregisterForSuppServiceNotification(Handler h){
        mSuppServiceNotificationRegistrants.remove(h);
    }

    public void  registerForVtReplaceDisconnect(Handler h, int what, Object obj) {
        mVtReplaceDisconnectRegistrants.addUnique(h, what, obj);
    }

    public void unregisterForVtReplaceDisconnect(Handler h){
        mVtReplaceDisconnectRegistrants.remove(h);
    }
    /* MTK proprietary end */

    /**
     * Notifies when a voice connection has disconnected, either due to local
     * or remote hangup or error.
     *
     *  Messages received from this will have the following members:<p>
     *  <ul><li>Message.obj will be an AsyncResult</li>
     *  <li>AsyncResult.userObj = obj</li>
     *  <li>AsyncResult.result = a Connection object that is
     *  no longer connected.</li></ul>
     */
    public void registerForDisconnect2(Handler h, int what, Object obj) {
        mDisconnectRegistrants2.addUnique(h, what, obj);
    }

    /**
     * Unregisters for voice disconnection notification.
     * Extraneous calls are tolerated silently
     */
    public void unregisterForDisconnect2(Handler h){
        mDisconnectRegistrants2.remove(h);
    }

    /**
     * Register for getting notifications for change in the Call State {@link Call.State}
     * This is called PreciseCallState because the call state is more precise than the
     * {@link PhoneConstants.State} which can be obtained using the {@link PhoneStateListener}
     *
     * Resulting events will have an AsyncResult in <code>Message.obj</code>.
     * AsyncResult.userData will be set to the obj argument here.
     * The <em>h</em> parameter is held only by a weak reference.
     */
    public void registerForPreciseCallStateChanged2(Handler h, int what, Object obj){
        mPreciseCallStateRegistrants2.addUnique(h, what, obj);
    }

    /**
     * Unregisters for voice call state change notifications.
     * Extraneous calls are tolerated silently.
     */
    public void unregisterForPreciseCallStateChanged2(Handler h){
        mPreciseCallStateRegistrants2.remove(h);
    }

    /**
     * Notifies when a previously untracked non-ringing/waiting connection has appeared.
     * This is likely due to some other entity (eg, SIM card application) initiating a call.
     */
    public void registerForUnknownConnection2(Handler h, int what, Object obj){
        mUnknownConnectionRegistrants2.addUnique(h, what, obj);
    }

    /**
     * Unregisters for unknown connection notifications.
     */
    public void unregisterForUnknownConnection2(Handler h){
        mUnknownConnectionRegistrants2.remove(h);
    }
    
    /**
     * Notifies when a new ringing or waiting connection has appeared.<p>
     *
     *  Messages received from this:
     *  Message.obj will be an AsyncResult
     *  AsyncResult.userObj = obj
     *  AsyncResult.result = a Connection. <p>
     *  Please check Connection.isRinging() to make sure the Connection
     *  has not dropped since this message was posted.
     *  If Connection.isRinging() is true, then
     *   Connection.getCall() == Phone.getRingingCall()
     */
    public void registerForNewRingingConnection2(Handler h, int what, Object obj){
        mNewRingingConnectionRegistrants2.addUnique(h, what, obj);
    }

    /**
     * Unregisters for new ringing connection notification.
     * Extraneous calls are tolerated silently
     */

    public void unregisterForNewRingingConnection2(Handler h){
        mNewRingingConnectionRegistrants2.remove(h);
    }

    /**
     * Notifies when an incoming call rings.<p>
     *
     *  Messages received from this:
     *  Message.obj will be an AsyncResult
     *  AsyncResult.userObj = obj
     *  AsyncResult.result = a Connection. <p>
     */
    public void registerForIncomingRing2(Handler h, int what, Object obj){
        mIncomingRingRegistrants2.addUnique(h, what, obj);
    }

    /**
     * Unregisters for ring notification.
     * Extraneous calls are tolerated silently
     */

    public void unregisterForIncomingRing2(Handler h){
        mIncomingRingRegistrants2.remove(h);
    }

    /**
     * Notifies when out-band ringback tone is needed.<p>
     *
     *  Messages received from this:
     *  Message.obj will be an AsyncResult
     *  AsyncResult.userObj = obj
     *  AsyncResult.result = boolean, true to start play ringback tone
     *                       and false to stop. <p>
     */
    public void registerForRingbackTone2(Handler h, int what, Object obj){
        mRingbackToneRegistrants2.addUnique(h, what, obj);
    }

    /**
     * Unregisters for ringback tone notification.
     */

    public void unregisterForRingbackTone2(Handler h){
        mRingbackToneRegistrants2.remove(h);
    }

    /**
     * Registers the handler to reset the uplink mute state to get
     * uplink audio.
     */
    public void registerForResendIncallMute2(Handler h, int what, Object obj){
        mResendIncallMuteRegistrants2.addUnique(h, what, obj);
    }

    /**
     * Unregisters for resend incall mute notifications.
     */
    public void unregisterForResendIncallMute2(Handler h){
        mResendIncallMuteRegistrants2.remove(h);
    }

    /**
     * Register for notifications of initiation of a new MMI code request.
     * MMI codes for GSM are discussed in 3GPP TS 22.030.<p>
     *
     * Example: If Phone.dial is called with "*#31#", then the app will
     * be notified here.<p>
     *
     * The returned <code>Message.obj</code> will contain an AsyncResult.
     *
     * <code>obj.result</code> will be an "MmiCode" object.
     */
    public void registerForMmiInitiate2(Handler h, int what, Object obj){
        mMmiInitiateRegistrants2.addUnique(h, what, obj);
    }

    /**
     * Unregisters for new MMI initiate notification.
     * Extraneous calls are tolerated silently
     */
    public void unregisterForMmiInitiate2(Handler h){
        mMmiInitiateRegistrants2.remove(h);
    }

    /**
     * Register for notifications that an MMI request has completed
     * its network activity and is in its final state. This may mean a state
     * of COMPLETE, FAILED, or CANCELLED.
     *
     * <code>Message.obj</code> will contain an AsyncResult.
     * <code>obj.result</code> will be an "MmiCode" object
     */
    public void registerForMmiComplete2(Handler h, int what, Object obj){
        mMmiCompleteRegistrants2.addUnique(h, what, obj);
    }

    /**
     * Unregisters for MMI complete notification.
     * Extraneous calls are tolerated silently
     */
    public void unregisterForMmiComplete2(Handler h){
        mMmiCompleteRegistrants2.remove(h);
    }

    /**
     * Registration point for Ecm timer reset
     * @param h handler to notify
     * @param what user-defined message code
     * @param obj placed in Message.obj
     */
    public void registerForEcmTimerReset2(Handler h, int what, Object obj){
        mEcmTimerResetRegistrants2.addUnique(h, what, obj);
    }

    /**
     * Unregister for notification for Ecm timer reset
     * @param h Handler to be removed from the registrant list.
     */
    public void unregisterForEcmTimerReset2(Handler h){
        mEcmTimerResetRegistrants2.remove(h);
    }

    /**
     * Register for ServiceState changed.
     * Message.obj will contain an AsyncResult.
     * AsyncResult.result will be a ServiceState instance
     */
    public void registerForServiceStateChanged2(Handler h, int what, Object obj){
        mServiceStateChangedRegistrants2.addUnique(h, what, obj);
    }

    /**
     * Unregisters for ServiceStateChange notification.
     * Extraneous calls are tolerated silently
     */
    public void unregisterForServiceStateChanged2(Handler h){
        mServiceStateChangedRegistrants2.remove(h);
    }

    /**
     * Register for notifications when a supplementary service attempt fails.
     * Message.obj will contain an AsyncResult.
     *
     * @param h Handler that receives the notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    public void registerForSuppServiceFailed2(Handler h, int what, Object obj){
        mSuppServiceFailedRegistrants2.addUnique(h, what, obj);
    }

    /**
     * Unregister for notifications when a supplementary service attempt fails.
     * Extraneous calls are tolerated silently
     *
     * @param h Handler to be removed from the registrant list.
     */
    public void unregisterForSuppServiceFailed2(Handler h){
        mSuppServiceFailedRegistrants2.remove(h);
    }

    /**
     * Register for notifications when a sInCall VoicePrivacy is enabled
     *
     * @param h Handler that receives the notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    public void registerForInCallVoicePrivacyOn2(Handler h, int what, Object obj){
        mInCallVoicePrivacyOnRegistrants2.addUnique(h, what, obj);
    }

    /**
     * Unregister for notifications when a sInCall VoicePrivacy is enabled
     *
     * @param h Handler to be removed from the registrant list.
     */
    public void unregisterForInCallVoicePrivacyOn2(Handler h){
        mInCallVoicePrivacyOnRegistrants2.remove(h);
    }

    /**
     * Register for notifications when a sInCall VoicePrivacy is disabled
     *
     * @param h Handler that receives the notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    public void registerForInCallVoicePrivacyOff2(Handler h, int what, Object obj){
        mInCallVoicePrivacyOffRegistrants2.addUnique(h, what, obj);
    }

    /**
     * Unregister for notifications when a sInCall VoicePrivacy is disabled
     *
     * @param h Handler to be removed from the registrant list.
     */
    public void unregisterForInCallVoicePrivacyOff2(Handler h){
        mInCallVoicePrivacyOffRegistrants2.remove(h);
    }

    /**
     * Register for notifications when CDMA call waiting comes
     *
     * @param h Handler that receives the notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    public void registerForCallWaiting2(Handler h, int what, Object obj){
        mCallWaitingRegistrants2.addUnique(h, what, obj);
    }

    /**
     * Unregister for notifications when CDMA Call waiting comes
     * @param h Handler to be removed from the registrant list.
     */
    public void unregisterForCallWaiting2(Handler h){
        mCallWaitingRegistrants2.remove(h);
    }


    /**
     * Register for signal information notifications from the network.
     * Message.obj will contain an AsyncResult.
     * AsyncResult.result will be a SuppServiceNotification instance.
     *
     * @param h Handler that receives the notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */

    public void registerForSignalInfo2(Handler h, int what, Object obj){
        mSignalInfoRegistrants2.addUnique(h, what, obj);
    }

    /**
     * Unregisters for signal information notifications.
     * Extraneous calls are tolerated silently
     *
     * @param h Handler to be removed from the registrant list.
     */
    public void unregisterForSignalInfo2(Handler h){
        mSignalInfoRegistrants2.remove(h);
    }

    /**
     * Register for display information notifications from the network.
     * Message.obj will contain an AsyncResult.
     * AsyncResult.result will be a SuppServiceNotification instance.
     *
     * @param h Handler that receives the notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    public void registerForDisplayInfo2(Handler h, int what, Object obj){
        mDisplayInfoRegistrants2.addUnique(h, what, obj);
    }

    /**
     * Unregisters for display information notifications.
     * Extraneous calls are tolerated silently
     *
     * @param h Handler to be removed from the registrant list.
     */
    public void unregisterForDisplayInfo2(Handler h) {
        mDisplayInfoRegistrants2.remove(h);
    }

    /**
     * Register for notifications when CDMA OTA Provision status change
     *
     * @param h Handler that receives the notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    public void registerForCdmaOtaStatusChange2(Handler h, int what, Object obj){
        mCdmaOtaStatusChangeRegistrants2.addUnique(h, what, obj);
    }

    /**
     * Unregister for notifications when CDMA OTA Provision status change
     * @param h Handler to be removed from the registrant list.
     */
    public void unregisterForCdmaOtaStatusChange2(Handler h){
        mCdmaOtaStatusChangeRegistrants2.remove(h);
    }

    /**
     * Registration point for subscription info ready
     * @param h handler to notify
     * @param what what code of message when delivered
     * @param obj placed in Message.obj
     */
    public void registerForSubscriptionInfoReady2(Handler h, int what, Object obj){
        mSubscriptionInfoReadyRegistrants2.addUnique(h, what, obj);
    }

    /**
     * Unregister for notifications for subscription info
     * @param h Handler to be removed from the registrant list.
     */
    public void unregisterForSubscriptionInfoReady2(Handler h){
        mSubscriptionInfoReadyRegistrants2.remove(h);
    }

    /**
     * Sets an event to be fired when the telephony system processes
     * a post-dial character on an outgoing call.<p>
     *
     * Messages of type <code>what</code> will be sent to <code>h</code>.
     * The <code>obj</code> field of these Message's will be instances of
     * <code>AsyncResult</code>. <code>Message.obj.result</code> will be
     * a Connection object.<p>
     *
     * Message.arg1 will be the post dial character being processed,
     * or 0 ('\0') if end of string.<p>
     *
     * If Connection.getPostDialState() == WAIT,
     * the application must call
     * {@link com.android.internal.telephony.Connection#proceedAfterWaitChar()
     * Connection.proceedAfterWaitChar()} or
     * {@link com.android.internal.telephony.Connection#cancelPostDial()
     * Connection.cancelPostDial()}
     * for the telephony system to continue playing the post-dial
     * DTMF sequence.<p>
     *
     * If Connection.getPostDialState() == WILD,
     * the application must call
     * {@link com.android.internal.telephony.Connection#proceedAfterWildChar
     * Connection.proceedAfterWildChar()}
     * or
     * {@link com.android.internal.telephony.Connection#cancelPostDial()
     * Connection.cancelPostDial()}
     * for the telephony system to continue playing the
     * post-dial DTMF sequence.<p>
     *
     */
    public void registerForPostDialCharacter2(Handler h, int what, Object obj){
        mPostDialCharacterRegistrants2.addUnique(h, what, obj);
    }

    /**
     * Unregister for notifications for post-dial character on an outgoing call.
     * @param h handler to notify
     */
    public void unregisterForPostDialCharacter2(Handler h){
        mPostDialCharacterRegistrants2.remove(h);
    }

    /**
     * Sets an event to be fired when the telephony system processes speech URC messages from modem.
     * @param h handler to notify
     * @param what what code of message when delivered
     * @param obj placed in Message.obj
     */
    public void registerForSpeechInfo2(Handler h, int what, Object obj) {
        mSpeechInfoRegistrants2.addUnique(h, what, obj);
    }

    /**
     * Unregister for notifications for espeech URC messages from modem.
     * @param h handler to notify
     */
    public void unregisterForSpeechInfo2(Handler h) {
        mSpeechInfoRegistrants2.remove(h);
    }
    
    /**
     * Sets an event to be fired when the telephony system processes VT status update messages from modem.
     * @param h handler to notify
     * @param what what code of message when delivered
     * @param obj placed in Message.obj
     */
    public void registerForVtStatusInfo2(Handler h, int what, Object obj)  {
        mVtStatusInfoRegistrants2.addUnique(h, what, obj);
    }

    /**
     * Unregister for notifications for VT status update messages from modem.
     * @param h handler to notify
     */
    public void unregisterForVtStatusInfo2(Handler h)  {
        mVtStatusInfoRegistrants2.remove(h);
    }

    /**
     * Sets an event to be fired when the telephony system processes VT incoming ring messages from modem.
     * @param h handler to notify
     * @param what what code of message when delivered
     * @param obj placed in Message.obj
     */
    public void registerForVtRingInfo2(Handler h, int what, Object obj) {
        mVtRingInfoRegistrants2.addUnique(h, what, obj);
    }
    
    /**
     * Unregister for notifications for VT incoming ring messages from modem.
     * @param h handler to notify
     */
    public void unregisterForVtRingInfo2(Handler h) {
        mVtRingInfoRegistrants2.remove(h);
    }

    /**
     * Sets an event to be fired when the telephony system processes call related SS messages from modem.
     * @param h handler to notify
     * @param what what code of message when delivered
     * @param obj placed in Message.obj
     */
    public void registerForCrssSuppServiceNotification2(Handler h, int what, Object obj) {
        mCrssSuppServiceNotificationRegistrants2.addUnique(h, what, obj);
    }

    /**
     * Unregister for notifications for call related SS messages from modem.
     * @param h handler to notify
     */
    public void unregisterForCrssSuppServiceNotification2(Handler h) {
        mCrssSuppServiceNotificationRegistrants2.remove(h);
    }

    /**
     * Sets an event to be fired when the telephony system processes SS messages from modem.
     * @param h handler to notify
     * @param what what code of message when delivered
     * @param obj placed in Message.obj
     */
    public void registerForSuppServiceNotification2(Handler h, int what, Object obj) {
        mSuppServiceNotificationRegistrants2.addUnique(h, what, obj);
    }

    /**
     * Unregister for notifications for SS messages from modem.
     * @param h handler to notify
     */
    public void unregisterForSuppServiceNotification2(Handler h){
        mSuppServiceNotificationRegistrants2.remove(h);
    }

    /**
     * Sets an event to be fired when the telephony system processes voice replacing VT call disconnected messages from modem.
     * @param h handler to notify
     * @param what what code of message when delivered
     * @param obj placed in Message.obj
     */
    public void  registerForVtReplaceDisconnect2(Handler h, int what, Object obj) {
        mVtReplaceDisconnectRegistrants2.addUnique(h, what, obj);
    }

    /**
     * Unregister for notifications for voice replacing VT call disconnected messages from modem.
     * @param h handler to notify
     */
    public void unregisterForVtReplaceDisconnect2(Handler h){
        mVtReplaceDisconnectRegistrants2.remove(h);
    }

    /// M: Registrant methods for Gemini 2.0+. @{
    /** 
     * Return registrants array index according to simId parameter for supporting multiple SIMs.
     *
     * @param simId SIM id for retrieving registrants array index.
     * @return The index of registrants array, if the SIM id is invalid(greater than maximum SIM count), return -1.
     */
    private int getRegistrantsArrayIndex(int simId) {
    	  int index = simId - PhoneConstants.GEMINI_SIM_1;
    	  if ((index >= 0) && 
    	      (index < PhoneConstants.GEMINI_SIM_NUM) && 
    	      (index < MAXIMUM_SIM_COUNT)) {
            return index;
        }
     
        // Invalid SIM id, return error index.   
        return -1;
    }    

    /**
     * Notifies when a voice connection has disconnected, either due to local
     * or remote hangup or error.
     *
     *  Messages received from this will have the following members:<p>
     *  <ul><li>Message.obj will be an AsyncResult</li>
     *  <li>AsyncResult.userObj = obj</li>
     *  <li>AsyncResult.result = a Connection object that is
     *  no longer connected.</li></ul>
     *
     * @param h     Handler that receives the notification message.
     * @param what  User-defined message code.
     * @param obj   User object.
     * @param simId SIM id.
     */
    public void registerForDisconnectEx(Handler h, int what, Object obj, int simId) {
    	  int index = getRegistrantsArrayIndex(simId);
        Log.d(LOG_TAG, "registerForDisconnectEx, simId = " + simId + ", index = " + index);  	  
    	  if (index != -1) {
            mDisconnectRegistrantsGemini[index].addUnique(h, what, obj);
        }
    }

    /**
     * Unregisters for voice disconnection notification.
     * Extraneous calls are tolerated silently
     *
     * @param h     Handler that receives the notification message.
     * @param simId SIM id.
     */
    public void unregisterForDisconnectEx(Handler h, int simId){
    	  int index = getRegistrantsArrayIndex(simId);
    	  if (index != -1) {
            mDisconnectRegistrantsGemini[index].remove(h);
        }
    }

    /**
     * Register for getting notifications for change in the Call State {@link Call.State}
     * This is called PreciseCallState because the call state is more precise than the
     * {@link Phone.State} which can be obtained using the {@link PhoneStateListener}
     *
     * Resulting events will have an AsyncResult in <code>Message.obj</code>.
     * AsyncResult.userData will be set to the obj argument here.
     * The <em>h</em> parameter is held only by a weak reference.
     *
     * @param h     Handler that receives the notification message.
     * @param what  User-defined message code.
     * @param obj   User object.
     * @param simId SIM id.
     */
    public void registerForPreciseCallStateChangedEx(Handler h, int what, Object obj, int simId){
    	  int index = getRegistrantsArrayIndex(simId);
    	  if (index != -1) {
            mPreciseCallStateRegistrantsGemini[index].addUnique(h, what, obj);
        }
    }

    /**
     * Unregisters for voice call state change notifications.
     * Extraneous calls are tolerated silently.
     *
     * @param h     Handler that receives the notification message.
     * @param simId SIM id.
     */
    public void unregisterForPreciseCallStateChangedEx(Handler h, int simId){
    	  int index = getRegistrantsArrayIndex(simId);
    	  if (index != -1) {
            mPreciseCallStateRegistrantsGemini[index].remove(h);
        }
    }

    /**
     * Notifies when a previously untracked non-ringing/waiting connection has appeared.
     * This is likely due to some other entity (eg, SIM card application) initiating a call.
     *
     * @param h     Handler that receives the notification message.
     * @param what  User-defined message code.
     * @param obj   User object.
     * @param simId SIM id.
     */
    public void registerForUnknownConnectionEx(Handler h, int what, Object obj, int simId){
    	  int index = getRegistrantsArrayIndex(simId);
    	  if (index != -1) {
            mUnknownConnectionRegistrantsGemini[index].addUnique(h, what, obj);
        }
    }

    /**
     * Unregisters for unknown connection notifications.
     *
     * @param h     Handler that receives the notification message.
     * @param simId SIM id.
     */
    public void unregisterForUnknownConnectionEx(Handler h, int simId){
    	  int index = getRegistrantsArrayIndex(simId);
    	  if (index != -1) {
            mUnknownConnectionRegistrantsGemini[index].remove(h);
        }
    }
    
    /**
     * Notifies when a new ringing or waiting connection has appeared.<p>
     *
     *  Messages received from this:
     *  Message.obj will be an AsyncResult
     *  AsyncResult.userObj = obj
     *  AsyncResult.result = a Connection. <p>
     *  Please check Connection.isRinging() to make sure the Connection
     *  has not dropped since this message was posted.
     *  If Connection.isRinging() is true, then
     *   Connection.getCall() == Phone.getRingingCall()
     *
     * @param h     Handler that receives the notification message.
     * @param what  User-defined message code.
     * @param obj   User object.
     * @param simId SIM id.
     */
    public void registerForNewRingingConnectionEx(Handler h, int what, Object obj, int simId){
    	  int index = getRegistrantsArrayIndex(simId);
    	  if (index != -1) {
            mNewRingingConnectionRegistrantsGemini[index].addUnique(h, what, obj);
        }
    }

    /**
     * Unregisters for new ringing connection notification.
     * Extraneous calls are tolerated silently
     *
     * @param h     Handler that receives the notification message.
     * @param simId SIM id.
     */
    public void unregisterForNewRingingConnectionEx(Handler h, int simId){
    	  int index = getRegistrantsArrayIndex(simId);
    	  if (index != -1) {
            mNewRingingConnectionRegistrantsGemini[index].remove(h);
        }
    }

    /**
     * Notifies when an incoming call rings.<p>
     *
     *  Messages received from this:
     *  Message.obj will be an AsyncResult
     *  AsyncResult.userObj = obj
     *  AsyncResult.result = a Connection. <p>
     *
     * @param h     Handler that receives the notification message.
     * @param what  User-defined message code.
     * @param obj   User object.
     * @param simId SIM id.
     */
    public void registerForIncomingRingEx(Handler h, int what, Object obj, int simId){
    	  int index = getRegistrantsArrayIndex(simId);
    	  if (index != -1) {
            mIncomingRingRegistrantsGemini[index].addUnique(h, what, obj);
        }
    }

    /**
     * Unregisters for ring notification.
     * Extraneous calls are tolerated silently
     *
     * @param h     Handler that receives the notification message.
     * @param simId SIM id.
     */
    public void unregisterForIncomingRingEx(Handler h, int simId){
    	  int index = getRegistrantsArrayIndex(simId);
    	  if (index != -1) {
            mIncomingRingRegistrantsGemini[index].remove(h);
        }
    }

    /**
     * Notifies when out-band ringback tone is needed.<p>
     *
     *  Messages received from this:
     *  Message.obj will be an AsyncResult
     *  AsyncResult.userObj = obj
     *  AsyncResult.result = boolean, true to start play ringback tone
     *                       and false to stop. <p>
     *
     * @param h     Handler that receives the notification message.
     * @param what  User-defined message code.
     * @param obj   User object.
     * @param simId SIM id.
     */
    public void registerForRingbackToneEx(Handler h, int what, Object obj, int simId){
    	  int index = getRegistrantsArrayIndex(simId);
    	  if (index != -1) {
            mRingbackToneRegistrantsGemini[index].addUnique(h, what, obj);
        }
    }

    /**
     * Unregisters for ringback tone notification.
     *
     * @param h     Handler that receives the notification message.
     * @param simId SIM id.
     */
    public void unregisterForRingbackToneEx(Handler h, int simId){
    	  int index = getRegistrantsArrayIndex(simId);
    	  if (index != -1) {
            mRingbackToneRegistrantsGemini[index].remove(h);
        }
    }

    /**
     * Registers the handler to reset the uplink mute state to get
     * uplink audio.
     *
     * @param h     Handler that receives the notification message.
     * @param what  User-defined message code.
     * @param obj   User object.
     * @param simId SIM id.
     */
    public void registerForResendIncallMuteEx(Handler h, int what, Object obj, int simId){
    	  int index = getRegistrantsArrayIndex(simId);
    	  if (index != -1) {
            mResendIncallMuteRegistrantsGemini[index].addUnique(h, what, obj);
        }
    }

    /**
     * Unregisters for resend incall mute notifications.
     *
     * @param h     Handler that receives the notification message.
     * @param simId SIM id.
     */
    public void unregisterForResendIncallMuteEx(Handler h, int simId){
    	  int index = getRegistrantsArrayIndex(simId);
    	  if (index != -1) {
            mResendIncallMuteRegistrantsGemini[index].remove(h);
        }
    }

    /**
     * Register for notifications of initiation of a new MMI code request.
     * MMI codes for GSM are discussed in 3GPP TS 22.030.<p>
     *
     * Example: If Phone.dial is called with "*#31#", then the app will
     * be notified here.<p>
     *
     * The returned <code>Message.obj</code> will contain an AsyncResult.
     *
     * <code>obj.result</code> will be an "MmiCode" object.
     *
     * @param h     Handler that receives the notification message.
     * @param what  User-defined message code.
     * @param obj   User object.
     * @param simId SIM id.
     */
    public void registerForMmiInitiateEx(Handler h, int what, Object obj, int simId){
    	  int index = getRegistrantsArrayIndex(simId);
    	  if (index != -1) {
            mMmiInitiateRegistrantsGemini[index].addUnique(h, what, obj);
        }
    }

    /**
     * Unregisters for new MMI initiate notification.
     * Extraneous calls are tolerated silently
     *
     * @param h     Handler that receives the notification message.
     * @param simId SIM id.
     */
    public void unregisterForMmiInitiateEx(Handler h, int simId){
    	  int index = getRegistrantsArrayIndex(simId);
    	  if (index != -1) {
            mMmiInitiateRegistrantsGemini[index].remove(h);
        }
    }

    /**
     * Register for notifications that an MMI request has completed
     * its network activity and is in its final state. This may mean a state
     * of COMPLETE, FAILED, or CANCELLED.
     *
     * <code>Message.obj</code> will contain an AsyncResult.
     * <code>obj.result</code> will be an "MmiCode" object
     *
     * @param h     Handler that receives the notification message.
     * @param what  User-defined message code.
     * @param obj   User object.
     * @param simId SIM id.
     */
    public void registerForMmiCompleteEx(Handler h, int what, Object obj, int simId){
    	  int index = getRegistrantsArrayIndex(simId);
    	  if (index != -1) {
            mMmiCompleteRegistrantsGemini[index].addUnique(h, what, obj);
        }
    }

    /**
     * Unregisters for MMI complete notification.
     * Extraneous calls are tolerated silently
     *
     * @param h     Handler that receives the notification message.
     * @param simId SIM id.
     */
    public void unregisterForMmiCompleteEx(Handler h, int simId){
    	  int index = getRegistrantsArrayIndex(simId);
    	  if (index != -1) {
            mMmiCompleteRegistrantsGemini[index].remove(h);
        }
    }

    /**
     * Registration point for Ecm timer reset
     *
     * @param h     Handler that receives the notification message.
     * @param what  User-defined message code.
     * @param obj   User object.
     * @param simId SIM id.
     */
    public void registerForEcmTimerResetEx(Handler h, int what, Object obj, int simId){
    	  int index = getRegistrantsArrayIndex(simId);
    	  if (index != -1) {
            mEcmTimerResetRegistrantsGemini[index].addUnique(h, what, obj);
        }
    }

    /**
     * Unregister for notification for Ecm timer reset
     * @param h Handler to be removed from the registrant list.
     *
     * @param h     Handler that receives the notification message.
     * @param simId SIM id.
     */
    public void unregisterForEcmTimerResetEx(Handler h, int simId){
    	  int index = getRegistrantsArrayIndex(simId);
    	  if (index != -1) {
            mEcmTimerResetRegistrantsGemini[index].remove(h);
        }
    }

    /**
     * Register for ServiceState changed.
     * Message.obj will contain an AsyncResult.
     * AsyncResult.result will be a ServiceState instance
     *
     * @param h     Handler that receives the notification message.
     * @param what  User-defined message code.
     * @param obj   User object.
     * @param simId SIM id.
     */
    public void registerForServiceStateChangedEx(Handler h, int what, Object obj, int simId){
    	  int index = getRegistrantsArrayIndex(simId);
    	  if (index != -1) {
            mServiceStateChangedRegistrantsGemini[index].addUnique(h, what, obj);
        }
    }

    /**
     * Unregisters for ServiceStateChange notification.
     * Extraneous calls are tolerated silently
     *
     * @param h     Handler that receives the notification message.
     * @param simId SIM id.
     */
    public void unregisterForServiceStateChangedEx(Handler h, int simId){
    	  int index = getRegistrantsArrayIndex(simId);
    	  if (index != -1) {
            mServiceStateChangedRegistrantsGemini[index].remove(h);
        }
    }

    /**
     * Register for notifications when a supplementary service attempt fails.
     * Message.obj will contain an AsyncResult.
     *
     * @param h     Handler that receives the notification message.
     * @param what  User-defined message code.
     * @param obj   User object.
     * @param simId SIM id.
     */
    public void registerForSuppServiceFailedEx(Handler h, int what, Object obj, int simId){
    	  int index = getRegistrantsArrayIndex(simId);
    	  if (index != -1) {
            mSuppServiceFailedRegistrantsGemini[index].addUnique(h, what, obj);
        }
    }

    /**
     * Unregister for notifications when a supplementary service attempt fails.
     * Extraneous calls are tolerated silently
     *
     *
     * @param h     Handler that receives the notification message.
     * @param simId SIM id.
     */
    public void unregisterForSuppServiceFailedEx(Handler h, int simId){
    	  int index = getRegistrantsArrayIndex(simId);
    	  if (index != -1) {
            mSuppServiceFailedRegistrantsGemini[index].remove(h);
        }
    }

    /**
     * Register for notifications when a sInCall VoicePrivacy is enabled
     *
     * @param h     Handler that receives the notification message.
     * @param what  User-defined message code.
     * @param obj   User object.
     * @param simId SIM id.
     */
    public void registerForInCallVoicePrivacyOnEx(Handler h, int what, Object obj, int simId){
    	  int index = getRegistrantsArrayIndex(simId);
    	  if (index != -1) {
            mInCallVoicePrivacyOnRegistrantsGemini[index].addUnique(h, what, obj);
        }
    }

    /**
     * Unregister for notifications when a sInCall VoicePrivacy is enabled
     *
     * @param h     Handler that receives the notification message.
     * @param simId SIM id.
     */
    public void unregisterForInCallVoicePrivacyOnEx(Handler h, int simId){
    	  int index = getRegistrantsArrayIndex(simId);
    	  if (index != -1) {
            mInCallVoicePrivacyOnRegistrantsGemini[index].remove(h);
        }
    }

    /**
     * Register for notifications when a sInCall VoicePrivacy is disabled
     *
     * @param h     Handler that receives the notification message.
     * @param what  User-defined message code.
     * @param obj   User object.
     * @param simId SIM id.
     */
    public void registerForInCallVoicePrivacyOffEx(Handler h, int what, Object obj, int simId){
    	  int index = getRegistrantsArrayIndex(simId);
    	  if (index != -1) {
            mInCallVoicePrivacyOffRegistrantsGemini[index].addUnique(h, what, obj);
        }
    }

    /**
     * Unregister for notifications when a sInCall VoicePrivacy is disabled
     *
     * @param h     Handler that receives the notification message.
     * @param simId SIM id.
     */
    public void unregisterForInCallVoicePrivacyOffEx(Handler h, int simId){
    	  int index = getRegistrantsArrayIndex(simId);
    	  if (index != -1) {
            mInCallVoicePrivacyOffRegistrantsGemini[index].remove(h);
        }
    }

    /**
     * Register for notifications when CDMA call waiting comes
     *
     * @param h     Handler that receives the notification message.
     * @param what  User-defined message code.
     * @param obj   User object.
     * @param simId SIM id.
     */
    public void registerForCallWaitingEx(Handler h, int what, Object obj, int simId){
    	  int index = getRegistrantsArrayIndex(simId);
    	  if (index != -1) {
            mCallWaitingRegistrantsGemini[index].addUnique(h, what, obj);
        }
    }

    /**
     * Unregister for notifications when CDMA Call waiting comes
     *
     * @param h     Handler that receives the notification message.
     * @param simId SIM id.
     */
    public void unregisterForCallWaitingEx(Handler h, int simId){
    	  int index = getRegistrantsArrayIndex(simId);
    	  if (index != -1) {
            mCallWaitingRegistrantsGemini[index].remove(h);
        }
    }


    /**
     * Register for signal information notifications from the network.
     * Message.obj will contain an AsyncResult.
     * AsyncResult.result will be a SuppServiceNotification instance.
     *
     * @param h     Handler that receives the notification message.
     * @param what  User-defined message code.
     * @param obj   User object.
     * @param simId SIM id.
     */

    public void registerForSignalInfoEx(Handler h, int what, Object obj, int simId){
    	  int index = getRegistrantsArrayIndex(simId);
    	  if (index != -1) {
            mSignalInfoRegistrantsGemini[index].addUnique(h, what, obj);
        }
    }

    /**
     * Unregisters for signal information notifications.
     * Extraneous calls are tolerated silently
     *
     * @param h     Handler that receives the notification message.
     * @param simId SIM id.
     */
    public void unregisterForSignalInfoEx(Handler h, int simId){
    	  int index = getRegistrantsArrayIndex(simId);
    	  if (index != -1) {
            mSignalInfoRegistrantsGemini[index].remove(h);
        }
    }

    /**
     * Register for display information notifications from the network.
     * Message.obj will contain an AsyncResult.
     * AsyncResult.result will be a SuppServiceNotification instance.
     *
     * @param h     Handler that receives the notification message.
     * @param what  User-defined message code.
     * @param obj   User object.
     * @param simId SIM id.
     */
    public void registerForDisplayInfoEx(Handler h, int what, Object obj, int simId){
    	  int index = getRegistrantsArrayIndex(simId);
    	  if (index != -1) {
            mDisplayInfoRegistrantsGemini[index].addUnique(h, what, obj);
        }
    }

    /**
     * Unregisters for display information notifications.
     * Extraneous calls are tolerated silently
     *
     * @param h     Handler that receives the notification message.
     * @param simId SIM id.
     */
    public void unregisterForDisplayInfoEx(Handler h, int simId) {
    	  int index = getRegistrantsArrayIndex(simId);
    	  if (index != -1) {
            mDisplayInfoRegistrantsGemini[index].remove(h);
        }
    }

    /**
     * Register for notifications when CDMA OTA Provision status change
     *
     * @param h     Handler that receives the notification message.
     * @param what  User-defined message code.
     * @param obj   User object.
     * @param simId SIM id.
     */
    public void registerForCdmaOtaStatusChangeEx(Handler h, int what, Object obj, int simId){
    	  int index = getRegistrantsArrayIndex(simId);
    	  if (index != -1) {
            mCdmaOtaStatusChangeRegistrantsGemini[index].addUnique(h, what, obj);
        }
    }

    /**
     * Unregister for notifications when CDMA OTA Provision status change
     *
     * @param h     Handler that receives the notification message.
     * @param simId SIM id.
     */
    public void unregisterForCdmaOtaStatusChangeEx(Handler h, int simId){
    	  int index = getRegistrantsArrayIndex(simId);
    	  if (index != -1) {
            mCdmaOtaStatusChangeRegistrantsGemini[index].remove(h);
        }
    }

    /**
     * Registration point for subscription info ready
     *
     * @param h     Handler that receives the notification message.
     * @param what  User-defined message code.
     * @param obj   User object.
     * @param simId SIM id.
     */
    public void registerForSubscriptionInfoReadyEx(Handler h, int what, Object obj, int simId){
    	  int index = getRegistrantsArrayIndex(simId);
    	  if (index != -1) {
            mSubscriptionInfoReadyRegistrantsGemini[index].addUnique(h, what, obj);
        }
    }

    /**
     * Unregister for notifications for subscription info
     *
     * @param h     Handler that receives the notification message.
     * @param simId SIM id.
     */
    public void unregisterForSubscriptionInfoReadyEx(Handler h, int simId){
    	  int index = getRegistrantsArrayIndex(simId);
    	  if (index != -1) {
            mSubscriptionInfoReadyRegistrantsGemini[index].remove(h);
        }
    }

    /**
     * Sets an event to be fired when the telephony system processes
     * a post-dial character on an outgoing call.<p>
     *
     * Messages of type <code>what</code> will be sent to <code>h</code>.
     * The <code>obj</code> field of these Message's will be instances of
     * <code>AsyncResult</code>. <code>Message.obj.result</code> will be
     * a Connection object.<p>
     *
     * Message.arg1 will be the post dial character being processed,
     * or 0 ('\0') if end of string.<p>
     *
     * If Connection.getPostDialState() == WAIT,
     * the application must call
     * {@link com.android.internal.telephony.Connection#proceedAfterWaitChar()
     * Connection.proceedAfterWaitChar()} or
     * {@link com.android.internal.telephony.Connection#cancelPostDial()
     * Connection.cancelPostDial()}
     * for the telephony system to continue playing the post-dial
     * DTMF sequence.<p>
     *
     * If Connection.getPostDialState() == WILD,
     * the application must call
     * {@link com.android.internal.telephony.Connection#proceedAfterWildChar
     * Connection.proceedAfterWildChar()}
     * or
     * {@link com.android.internal.telephony.Connection#cancelPostDial()
     * Connection.cancelPostDial()}
     * for the telephony system to continue playing the
     * post-dial DTMF sequence.<p>
     *
     * @param h     Handler that receives the notification message.
     * @param what  User-defined message code.
     * @param obj   User object.
     * @param simId SIM id.
     */
    public void registerForPostDialCharacterEx(Handler h, int what, Object obj, int simId){
    	  int index = getRegistrantsArrayIndex(simId);
    	  if (index != -1) {
            mPostDialCharacterRegistrantsGemini[index].addUnique(h, what, obj);
        }
    }

    /**
     * Unregister for notifications for post-dial character on an outgoing call.
     *
     * @param h     Handler that receives the notification message.
     * @param simId SIM id.
     */
    public void unregisterForPostDialCharacterEx(Handler h, int simId){
    	  int index = getRegistrantsArrayIndex(simId);
    	  if (index != -1) {
            mPostDialCharacterRegistrantsGemini[index].remove(h);
        }
    }

    /**
     * Sets an event to be fired when the telephony system processes speech URC messages from modem.
     *
     * @param h     Handler that receives the notification message.
     * @param what  User-defined message code.
     * @param obj   User object.
     * @param simId SIM id.
     */
    public void registerForSpeechInfoEx(Handler h, int what, Object obj, int simId) {
    	  int index = getRegistrantsArrayIndex(simId);
    	  if (index != -1) {
            mSpeechInfoRegistrantsGemini[index].addUnique(h, what, obj);
        }
    }

    /**
     * Unregister for notifications for espeech URC messages from modem.
     *
     * @param h     Handler that receives the notification message.
     * @param simId SIM id.
     */
    public void unregisterForSpeechInfoEx(Handler h, int simId) {
    	  int index = getRegistrantsArrayIndex(simId);
    	  if (index != -1) {
            mSpeechInfoRegistrantsGemini[index].remove(h);
        }
    }
    
    /**
     * Sets an event to be fired when the telephony system processes VT status update messages from modem.
     *
     * @param h     Handler that receives the notification message.
     * @param what  User-defined message code.
     * @param obj   User object.
     * @param simId SIM id.
     */
    public void registerForVtStatusInfoEx(Handler h, int what, Object obj, int simId)  {
    	  int index = getRegistrantsArrayIndex(simId);
    	  if (index != -1) {
            mVtStatusInfoRegistrantsGemini[index].addUnique(h, what, obj);
        }
    }

    /**
     * Unregister for notifications for VT status update messages from modem.
     *
     * @param h     Handler that receives the notification message.
     * @param simId SIM id.
     */
    public void unregisterForVtStatusInfoEx(Handler h, int simId)  {
    	  int index = getRegistrantsArrayIndex(simId);
    	  if (index != -1) {
            mVtStatusInfoRegistrantsGemini[index].remove(h);
        }
    }

    /**
     * Sets an event to be fired when the telephony system processes VT incoming ring messages from modem.
     *
     * @param h     Handler that receives the notification message.
     * @param what  User-defined message code.
     * @param obj   User object.
     * @param simId SIM id.
     */
    public void registerForVtRingInfoEx(Handler h, int what, Object obj, int simId) {
    	  int index = getRegistrantsArrayIndex(simId);
    	  if (index != -1) {
            mVtRingInfoRegistrantsGemini[index].addUnique(h, what, obj);
        }
    }
    
    /**
     * Unregister for notifications for VT incoming ring messages from modem.
     *
     * @param h     Handler that receives the notification message.
     * @param simId SIM id.
     */
    public void unregisterForVtRingInfoEx(Handler h, int simId) {
    	  int index = getRegistrantsArrayIndex(simId);
    	  if (index != -1) {
            mVtRingInfoRegistrantsGemini[index].remove(h);
        }
    }

    /**
     * Sets an event to be fired when the telephony system processes call related SS messages from modem.
     *
     * @param h     Handler that receives the notification message.
     * @param what  User-defined message code.
     * @param obj   User object.
     * @param simId SIM id.
     */
    public void registerForCrssSuppServiceNotificationEx(Handler h, int what, Object obj, int simId) {
    	  int index = getRegistrantsArrayIndex(simId);
    	  if (index != -1) {
            mCrssSuppServiceNotificationRegistrantsGemini[index].addUnique(h, what, obj);
        }
    }

    /**
     * Unregister for notifications for call related SS messages from modem.
     *
     * @param h     Handler that receives the notification message.
     * @param simId SIM id.
     */
    public void unregisterForCrssSuppServiceNotificationEx(Handler h, int simId) {
    	  int index = getRegistrantsArrayIndex(simId);
    	  if (index != -1) {
            mCrssSuppServiceNotificationRegistrantsGemini[index].remove(h);
        }
    }

    /**
     * Sets an event to be fired when the telephony system processes SS messages from modem.
     *
     * @param h     Handler that receives the notification message.
     * @param what  User-defined message code.
     * @param obj   User object.
     * @param simId SIM id.
     */
    public void registerForSuppServiceNotificationEx(Handler h, int what, Object obj, int simId) {
    	  int index = getRegistrantsArrayIndex(simId);
    	  if (index != -1) {
            mSuppServiceNotificationRegistrantsGemini[index].addUnique(h, what, obj);
        }
    }

    /**
     * Unregister for notifications for SS messages from modem.
     *
     * @param h     Handler that receives the notification message.
     * @param simId SIM id.
     */
    public void unregisterForSuppServiceNotificationEx(Handler h, int simId){
    	  int index = getRegistrantsArrayIndex(simId);
    	  if (index != -1) {
            mSuppServiceNotificationRegistrantsGemini[index].remove(h);
        }
    }

    /**
     * Sets an event to be fired when the telephony system processes voice replacing VT call disconnected messages from modem.
     *
     * @param h     Handler that receives the notification message.
     * @param what  User-defined message code.
     * @param obj   User object.
     * @param simId SIM id.
     */
    public void registerForVtReplaceDisconnectEx(Handler h, int what, Object obj, int simId) {
    	  int index = getRegistrantsArrayIndex(simId);
    	  if (index != -1) {
            mVtReplaceDisconnectRegistrantsGemini[index].addUnique(h, what, obj);
        }
    }

    /**
     * Unregister for notifications for voice replacing VT call disconnected messages from modem.
     *
     * @param h     Handler that receives the notification message.
     * @param simId SIM id.
     */
    public void unregisterForVtReplaceDisconnectEx(Handler h, int simId){
    	  int index = getRegistrantsArrayIndex(simId);
    	  if (index != -1) {
            mVtReplaceDisconnectRegistrantsGemini[index].remove(h);
        }
    }
    /// @}
    

    /**
     * Hang up all calls.
     */
    public void hangupAll() throws CallStateException {
        if (VDBG) {
            Log.d(LOG_TAG, " hangupAll() ");
            Log.d(LOG_TAG, this.toString());
        }

        for(Phone phone : mPhones) {
             if (PhoneConstants.State.IDLE != phone.getState()) {
                 phone.hangupAll();
             }
        }
    }

    /**
     * Hang up all calls.
     */
    public boolean hangupAllEx() throws CallStateException {
        boolean bAtLeastOneNotIdle = false;
		
        if (VDBG) {
            Log.d(LOG_TAG, " hangupAllEx() ");
            Log.d(LOG_TAG, this.toString());
        }

        for(Phone phone : mPhones) {
             Log.d(LOG_TAG, "phone.getState() = " + phone.getState());
             if (PhoneConstants.State.IDLE != phone.getState()) {
                 phone.hangupAllEx();
                 bAtLeastOneNotIdle = true;
             }
        }

        return bAtLeastOneNotIdle;
    }

    /**
     * Hang up the specified active call.
     * @param activeCall active call object to be hang up.
     */
    public void hangupActiveCall(Call activeCall) throws CallStateException {
        if (VDBG) {
            Log.d(LOG_TAG, " hangupActiveCall(" + activeCall + ")");
            Log.d(LOG_TAG, this.toString());
        }

        if ( hasActiveFgCall() ) {
            Phone activePhone = activeCall.getPhone();
            activePhone.hangupActiveCall();
        }
    }

    /**
     * Make a VT call.
     * @param phone specify which phone to make call.
     * @param dialString phone number to be dialed out.
     */
    public Connection vtDial(Phone phone, String dialString) throws CallStateException {
        Phone basePhone = getPhoneBase(phone);
        Connection result;

        if (VDBG) {
            Log.d(LOG_TAG, " vtDial(" + basePhone + ", "+ dialString + ")");
            Log.d(LOG_TAG, this.toString());
        }

        /* If there is an active call exists, hang it up first. */
        if ( hasActiveFgCall() ) {
            Phone activePhone = getActiveFgCall().getPhone();

            if (activePhone instanceof SipPhone) {
                boolean hasBgCall = !(activePhone.getBackgroundCall().isIdle());

                if (DBG) {
                    Log.d(LOG_TAG, "hasBgCall: "+ hasBgCall + " sameChannel:" + (activePhone == basePhone));
                }

                if (hasBgCall) {
                    Log.d(LOG_TAG, "Hangup");
                    getActiveFgCall().hangup();
                } else {
                    Log.d(LOG_TAG, "Switch");
                    activePhone.switchHoldingAndActive();
                }
            } else {
                activePhone.hangupAll();
            }
        }

        result = basePhone.vtDial(dialString);

        if (VDBG) {
            Log.d(LOG_TAG, "End vtDial(" + basePhone + ", "+ dialString + ")");
            Log.d(LOG_TAG, this.toString());
        }

        return result;
    }

    /**
     * Accept a VT call by using voice call connection.
     * @param ringingCall VT incoming call.
     */
    public void voiceAccept(Call ringingCall) throws CallStateException {
        if (VDBG) {
            Log.d(LOG_TAG, "voiceAccept(" +ringingCall + ")");
            Log.d(LOG_TAG, this.toString());
        }

        Phone ringingPhone = ringingCall.getPhone();

        /* Accept VT call as voice call */
        ringingPhone.voiceAccept();

        if (VDBG) {
            Log.d(LOG_TAG, "End voiceAccept(" +ringingCall + ")");
            Log.d(LOG_TAG, this.toString());
        }

    }

    /**
    * check if the dial string is CRSS string.
    * @param dialString dial string which may contain CRSS string.
    */
    private boolean isInCallMmiCommands(String dialString) {
        boolean result = false;
        char ch = dialString.charAt(0);

        switch (ch) {
            case '0':
            case '3':
            case '4':
            case '5':
                if (dialString.length() == 1) {
                    result = true;
                }
                break;

            case '1':
            case '2':
                if (dialString.length() == 1 || dialString.length() == 2) {
                    result = true;
                }
                break;

            default:
                break;
        }

        return result;
    }

    /**
    * Acquire or release 3G switch lock according to current phone state.
    */
    private void handle3GSwitchLock() {
        /* 3G switch start */
        if (FeatureOption.MTK_GEMINI_3G_SWITCH) {
            ITelephony iTelephony = ITelephony.Stub.asInterface(ServiceManager.checkService("phone"));
            PhoneConstants.State state = getState();
            try {
                if (state == PhoneConstants.State.IDLE && iTelephony.is3GSwitchLocked()) {
                    if (VDBG) Log.d(LOG_TAG, "Phone call IDLE, release 3G switch lock [" + m3GSwitchLockForPhoneCall + "]");
                    iTelephony.release3GSwitchLock(m3GSwitchLockForPhoneCall);
                    m3GSwitchLockForPhoneCall = -1;
                } else if (state != PhoneConstants.State.IDLE && !iTelephony.is3GSwitchLocked()) {
                    m3GSwitchLockForPhoneCall = iTelephony.aquire3GSwitchLock();
                    if (VDBG) Log.d(LOG_TAG, "Phone call not IDLE, acquire 3G switch lock [" + m3GSwitchLockForPhoneCall + "]");
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        /* 3G switch end */
    }

    /**
     * Handle speech info URC message sent from modem.
     * mtk04070, 20120704
     *
     * @param msg ESPEECH URC message sent from modem.
     * @return Return true if the message is handled correctly, else return false.
    */
    private boolean handleSpeechInfo(Message msg) {
		AsyncResult ar = (AsyncResult) msg.obj;
		int infoType = ((int[])ar.result)[0];
		boolean result = true;
		switch (infoType) {
		case 0:			/* The call is disconnect */
			int index = (msg.what - EVENT_SPEECH_INFO) / NOTIFICATION_ID_OFFSET;
			if (FeatureOption.MTK_DT_SUPPORT == false) {
				  index = 0;
			}
			int oppositeIndex = (index == 0) ? 1 : 0;
			int oppositeMode = (index == 0) ? AudioManager.MODE_IN_CALL_2 : AudioManager.MODE_IN_CALL;
			/* Solve ALPS00275770, only set audio mode to IN_CALL when in espeech = 1 */
			espeech_info[index] = 0;
			if (espeech_info[oppositeIndex] == 1) {
				 setAudioMode(oppositeMode);
			}
			if ((FeatureOption.MTK_DT_SUPPORT == true) && 
				 isEspeechTurnedOff(AudioManager.MODE_IN_CALL) &&
				 isEspeechTurnedOff(AudioManager.MODE_IN_CALL_2)) {
			   Log.d(LOG_TAG, "Set audio mode to NORMAL");	
			   setAudioMode(AudioManager.MODE_NORMAL);
			}
			break;
			
		case 1:			/* The call is in alerting state or is connected */
			int newDualModemCall = (msg.what - EVENT_SPEECH_INFO) / NOTIFICATION_ID_OFFSET;
			if (FeatureOption.MTK_DT_SUPPORT == false) {
				  newDualModemCall = 0;
			}
			/* Solve ALPS00275770, only set audio mode to IN_CALL when in espeech = 1 */
			espeech_info[newDualModemCall] = 1;
			
			/* Not need to switch to normal mode before change to another in call mode */
			if ((newDualModemCall != mDualModemCall) && (FeatureOption.MTK_DT_SUPPORT == true)) {
				 //setAudioModeDualModem(mDualModemCall, AudioManager.MODE_NORMAL);
				 mDualModemCall = newDualModemCall;
				 Log.d(LOG_TAG, "set mDualModemCall = " + mDualModemCall);
			}
			setAudioModeDualModem(mDualModemCall, AudioManager.MODE_IN_CALL);
			break;

		case 2:			/* The call is in held state(mute) */
		case 3: 		/* The call is in unheld state(unmute) */
			Context context = getContext();
			AudioManager audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
			int modem = (EVENT_SPEECH_INFO2 == msg.what) ? 2 : 1;
			int sid = (infoType == 2) ? 1 : 0;
            //MTK-START [mtk08470][130123][ALPS00448221] get correct modem id if modem is cross mapping
            if ((FeatureOption.MTK_DT_SUPPORT == true) && (PhoneFactory.getFirstMD() == 2)) {
                modem = (modem == 1) ? 2 : 1;
            }
            /* If only modem2 is enabled, set modem to 2 */
            if ((FeatureOption.MTK_ENABLE_MD1 == false) && 
                (FeatureOption.MTK_ENABLE_MD2 == true) &&
                (modem == 1)) {
                modem = 2;
            }
            //MTK-END [mtk08470][130123][ALPS00448221]MTK added
			String para = "SetModem" + modem + "GenerateSID=" + sid;
			audioManager.setParameters(para);
			Log.d(LOG_TAG, "para = " + para);
			break;

		default:
			result = false;
		}
		
		int index = (msg.what - EVENT_SPEECH_INFO) / NOTIFICATION_ID_OFFSET;
		mSpeechInfoRegistrantsGemini[index].notifyRegistrants((AsyncResult) msg.obj);
		mSpeechInfoRegistrants.notifyRegistrants((AsyncResult) msg.obj);

		return result;
    }

    /**
     * Check if the espeech of specific modem is turned off.
     * 
     * @param mode the audio mode to be set.
     * @return Return true if the espeech for specific modem is turned off, else return false.
    */
    private boolean isEspeechTurnedOff(int mode) {
       int i = (mode == AudioManager.MODE_IN_CALL) ? 0 : 1;
	   return (espeech_info[i] == 0);
	   
	   /*for (i = 0; i < ESPEECH_COUNT; i++) {
	   	   if (espeech_info[i] != 0) {
		   	  return false;
	   	   }
	   }
	   return true;	
	   */
    }

    /**
     * Clear espeech info array if the current state is idle.
     * 
    */
    private boolean clearEspeechInfo() {
       int i;
       boolean isCleared = false;
       Log.d(LOG_TAG, "[clearEspeechInfo] state = " + getState());

       for (i = 0; i < MAXIMUM_SIM_COUNT; i++) {
            if (espeech_info[i] != 0) {
               break;
            }
       }

       //[ALPS00558319] Mark the following line by mtk01411: Due to no +ESPEECH is reported for VT case
       //In this way:When an active VT call receives RADIO_OFF STATE due to TRM, CallManager will return from here (espeech_info[] all zero in VT case)
       //instead of reseting audio mode back to normal
       /*
       if (i == MAXIMUM_SIM_COUNT) {
          return isCleared;
       }
       */

       if ((getState() == PhoneConstants.State.IDLE) ||
           isServiceStatePowerOff()) {
           isCleared = true;
           for (i = 0; i < MAXIMUM_SIM_COUNT; i++) {
               espeech_info[i] = 0;
           }
           
           Log.d(LOG_TAG, "[clearEspeechInfo]Set audio mode to Normal");
           setAudioMode(AudioManager.MODE_NORMAL);
       }
       return isCleared;	   
    }

    /**
     * Check if there is another action need to be performed after holding request is done.
     *
     * @return Return true if there exists action need to be perform, else return false.
    */
    private boolean checkIfExistsFollowingAction() throws CallStateException {
        boolean result = false;
        Log.d(LOG_TAG, "checkIfExistsFollowingAction begin");
        Log.d(LOG_TAG, "mActiveCallToBeHeld.getState() = " + mActiveCallToBeHeld.getState());
        Log.d(LOG_TAG, "mWaitingReasonForHold = " + mWaitingReasonForHold);
        if (bWaitingForHoldRequest &&
            ((mActiveCallToBeHeld.getState() == Call.State.HOLDING) || (mActiveCallToBeHeld.getState() == Call.State.DISCONNECTED))) {
            //ALPS00397743: If the original active call is held failed, it should be able to answer another incoming call
            result = true;
            bWaitingForHoldRequest = false;
            switch (mWaitingReasonForHold) {
                case ACCEPT_CALL:
                    setAudioModeEarlierInAcceptCall();                        
                     mPhoneForWaitingHoldRequest.acceptCall();
                     break;

                case SWITCH_CALL:
                     mPhoneForWaitingHoldRequest.switchHoldingAndActive();
                     break;
            }
            mWaitingReasonForHold = WaitingReasonForHold.NONE;
        }
        Log.d(LOG_TAG, "checkIfExistsFollowingAction end");
        return result;
    }

	
    /**
     * Set SetVTSpeechCall according to EVENT_VT_STATUS_INFO URC from modem.
     *
     * @param ar VT status info - AsyncResult.
     *           0 - VT call is active
     *           1 - VT call is disconnected
    */
    private void setVTSpeechCall(AsyncResult ar) {
       int vtStatus = ((int[])ar.result)[0];
       int flag = (vtStatus == 0) ? 1 : 0;

       /* Check if the parameter is set in setAudioMode() */
       if (((flag == 1) && hasSetVtPara) ||
           ((flag == 0) && !hasSetVtPara)) {
          return;
       }
	   
       if (FeatureOption.MTK_VT3G324M_SUPPORT == true) {
           Context context = getContext();
           if (context == null) return;
           AudioManager audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
           audioManager.setParameters("SetVTSpeechCall=" + flag);
           hasSetVtPara = (flag == 1);
           Log.d(LOG_TAG, "[setVTSpeechCall]SetVTSpeechCall=" + flag);
       }
    }

    /**
     * Check if the current service state is power off.
     * @return true for power off, else return false.
     */
    private boolean isServiceStatePowerOff() {
       boolean bIsPowerOff = true;

       for (Phone phone : mPhones) {
           bIsPowerOff = (phone.getServiceState().getState() == ServiceState.STATE_POWER_OFF);
           if (!bIsPowerOff) {
              break;
           }
       }

	   Log.d(LOG_TAG, "[isServiceStatePowerOff]bIsPowerOff = " + bIsPowerOff);
       return bIsPowerOff;
    }
    /// @}
}
