package com.mediatek.encapsulation.com.android.internal.telephony;

import android.content.Context;
import android.net.LinkCapabilities;
import android.net.LinkProperties;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;

import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.DataConnection;
import com.android.internal.telephony.ims.IsimRecords;
import com.android.internal.telephony.RILConstants;
import com.android.internal.telephony.test.SimulatedRadioControl;
import com.android.internal.telephony.UUSInfo;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
//MTK-START [mtk04070][111223][ALPS00106134]Merge to ICS 4.0.3
import com.android.internal.telephony.gsm.UsimServiceTable;
//MTK-END [mtk04070][111223][ALPS00106134]Merge to ICS 4.0.3

//MTK-START [mtk04070][111117][ALPS00093395]MTK used
import com.android.internal.telephony.gsm.GsmDataConnection;
import com.android.internal.telephony.gsm.SmsBroadcastConfigInfo;
import com.android.internal.telephony.gsm.NetworkInfoWithAcT;
//MTK-END [mtk04070][111117][ALPS00093395]MTK used

import com.mediatek.encapsulation.EncapsulationConstant;

import java.util.List;

/**
 * Internal interface used to control the phone; SDK developers cannot
 * obtain this interface.
 */
public interface EncapsulatedPhone extends Phone {

    //MTK-START [mtk04070][111117][ALPS00093395]MTK added
    enum IccServiceStatus {
        NOT_EXIST_IN_SIM,
        NOT_EXIST_IN_USIM,
        ACTIVATED,
        INACTIVATED,
        UNKNOWN;
    };

    enum IccService {
        CHV1_DISABLE_FUNCTION ,   //0
        SPN,
        PNN,                                        //PLMN Network Name
        OPL,                                         //Operator PLMN List
        MWIS,                                      //Message Waiting Indication Status
        CFIS,                                        //Call Forwarding Indication Status   5
        SPDI,                                        //Service Provider Display Information
        EPLMN,                                      //Equivalent HPLMN
        UNSUPPORTED_SERVICE;                          //8

        public int getIndex() {
                int nIndex = -1;
                switch(this) {
                   case CHV1_DISABLE_FUNCTION:
                       nIndex = 0;
                       break;
                   case SPN:
                       nIndex = 1;
                       break;
                   case PNN:
                       nIndex = 2;
                       break;
                   case OPL:
                       nIndex = 3;
                       break;
                   case MWIS:
                       nIndex = 4;
                       break;
                   case CFIS:
                       nIndex = 5;
                       break;
                   case SPDI:
                       nIndex = 6;
                       break;
                   case EPLMN:
                       nIndex = 7;
                       break;
                   case UNSUPPORTED_SERVICE:
                       nIndex = 8;
                       break;
                   default:
                       break;
                }
                return nIndex;
        }
    };
    //MTK-END [mtk04070][111117][ALPS00093395]MTK added

    //MTK-START [mtk04070][111117][ALPS00093395]MTK added
    /* Add by vendor: Multiple PDP Context for MobileDataStateTracker usage */
    static final String DISCONNECT_DATA_FLAG = EncapsulationConstant.USE_MTK_PLATFORM ?
                                                      Phone.DISCONNECT_DATA_FLAG : "disconnectPdpFlag";
    /* vt start */
    static final String IS_VT_CALL = EncapsulationConstant.USE_MTK_PLATFORM ?
                                            Phone.IS_VT_CALL : "isVtCall";
    /* vt end */
    //MTK-END [mtk04070][111117][ALPS00093395]MTK added

    //MTK-START [mtk04070][111117][ALPS00093395]MTK added
    static final String FEATURE_ENABLE_DM = EncapsulationConstant.USE_MTK_PLATFORM ?
                                                    Phone.FEATURE_ENABLE_DM : "enableDM";
    static final String FEATURE_ENABLE_WAP = EncapsulationConstant.USE_MTK_PLATFORM ?
                                                    Phone.FEATURE_ENABLE_WAP : "enableWAP";
    static final String FEATURE_ENABLE_NET = EncapsulationConstant.USE_MTK_PLATFORM ?
                                                    Phone.FEATURE_ENABLE_NET : "enableNET";
    static final String FEATURE_ENABLE_CMMAIL = EncapsulationConstant.USE_MTK_PLATFORM ?
                                                    Phone.FEATURE_ENABLE_CMMAIL : "enableCMMAIL";
    //MTK-END [mtk04070][111117][ALPS00093395]MTK added

    /* Return codes for enableDataConnectivity() and disableDataConnectivity() */
    static final int ENABLE_DATA_CONNECTIVITY_INVALID_SIM_ID = EncapsulationConstant.USE_MTK_PLATFORM ?
                                      Phone.ENABLE_DATA_CONNECTIVITY_INVALID_SIM_ID : 0;
    static final int ENABLE_DATA_CONNECTIVITY_STARTED = EncapsulationConstant.USE_MTK_PLATFORM ?
                                      Phone.ENABLE_DATA_CONNECTIVITY_STARTED : 1;
    static final int ENABLE_DATA_CONNECTIVITY_SUCCESS = EncapsulationConstant.USE_MTK_PLATFORM ?
                                      Phone.ENABLE_DATA_CONNECTIVITY_SUCCESS : 2;
    static final int ENABLE_DATA_CONNECTIVITY_FAILED_THIS_SIM_STILL_DETACHING = EncapsulationConstant.USE_MTK_PLATFORM ?
                                      Phone.ENABLE_DATA_CONNECTIVITY_FAILED_THIS_SIM_STILL_DETACHING : 3;
    static final int ENABLE_DATA_CONNECTIVITY_INVALID_STATE = EncapsulationConstant.USE_MTK_PLATFORM ?
                                      Phone.ENABLE_DATA_CONNECTIVITY_INVALID_STATE : 4;
    static final int DISABLE_DATA_CONNECTIVITY_INVALID_SIM_ID = EncapsulationConstant.USE_MTK_PLATFORM ?
                                      Phone.DISABLE_DATA_CONNECTIVITY_INVALID_SIM_ID : 5;
    static final int DISABLE_DATA_CONNECTIVITY_STARTED = EncapsulationConstant.USE_MTK_PLATFORM ?
                                      Phone.DISABLE_DATA_CONNECTIVITY_STARTED : 6;
    static final int DISABLE_DATA_CONNECTIVITY_SUCCESS = EncapsulationConstant.USE_MTK_PLATFORM ?
                                      Phone.DISABLE_DATA_CONNECTIVITY_SUCCESS : 7;
    static final int DISABLE_DATA_CONNECTIVITY_INVALID_STATE = EncapsulationConstant.USE_MTK_PLATFORM ?
                                      Phone.DISABLE_DATA_CONNECTIVITY_INVALID_STATE : 8;
    //MTK-END [mtk04070][111117][ALPS00093395]MTK added

    //MTK-START [mtk04070][111117][ALPS00093395]MTK added
    int NT_MODE_GEMINI = EncapsulationConstant.USE_MTK_PLATFORM ?
                            Phone.NT_MODE_GEMINI : RILConstants.NETWORK_MODE_GEMINI;
    //MTK-END [mtk04070][111117][ALPS00093395]MTK added

    public static final String GEMINI_DEFAULT_SIM_MODE = EncapsulationConstant.USE_MTK_PLATFORM ?
                                    Phone.GEMINI_DEFAULT_SIM_MODE: "persist.radio.default_sim_mode";

    public static final String GEMINI_GPRS_TRANSFER_TYPE = EncapsulationConstant.USE_MTK_PLATFORM ?
                                    Phone.GEMINI_GPRS_TRANSFER_TYPE : "gemini.gprs.transfer.type";
    //MTK-END [mtk04070][111117][ALPS00093395]MTK added

    //via support start
    //for uim status
    static final int UIM_STATUS_NO_CARD_INSERTED = EncapsulationConstant.USE_MTK_PLATFORM ?
                                                    Phone.UIM_STATUS_NO_CARD_INSERTED : 0;
    static final int UIM_STATUS_CARD_INSERTED = EncapsulationConstant.USE_MTK_PLATFORM ?
                                                    Phone.UIM_STATUS_CARD_INSERTED : 1;
    //via support end

    //MTK-START [mtk80601][111212][ALPS00093395]IPO feature
    /**
     * Sets the radio power on after power off for reset.
     */
    void setRadioPowerOn();
    //MTK-END [mtk80601][111212][ALPS00093395]IPO feature

    boolean isCspPlmnEnabled(int simId);

    //MTK-START [mtk04070][111117][ALPS00093395]MTK proprietary methods
    /**
     * added by vend_am00002 for Multiple PDP Context
     * @param apnType
     */
    String getActiveApnType();

    /* Add by vendor for Multiple PDP Context */
    String getApnForType(String type);

    /**
     * Register for Supplementary Service CRSS notifications from the network.
     * Message.obj will contain an AsyncResult.
     * AsyncResult.result will be a SuppCrssNotification instance.
     *
     * @param h Handler that receives the notification message.
     * @param what User-defined message code.
     * @param obj User object.
     */
    public void registerForCrssSuppServiceNotification(Handler h, int what, Object obj);

    /**
     * Unregisters for Supplementary Service CRSS notifications.
     * Extraneous calls are tolerated silently
     *
     * @param h Handler to be removed from the registrant list.
     */
    public void unregisterForCrssSuppServiceNotification(Handler h);

    /* vt start */
    /**
     * Initiate a new video connection. This happens asynchronously, so you
     * cannot assume the audio path is connected (or a call index has been
     * assigned) until PhoneStateChanged notification has occurred.
     *
     * @exception CallStateException if a new outgoing call is not currently
     * possible because no more call slots exist or a call exists that is
     * dialing, alerting, ringing, or waiting.  Other errors are
     * handled asynchronously.
     */
    Connection vtDial(String dialString) throws CallStateException;

    /**
     * Initiate a new video connection with supplementary User to User
     * Information. This happens asynchronously, so you cannot assume the audio
     * path is connected (or a call index has been assigned) until
     * PhoneStateChanged notification has occurred.
     *
     * @exception CallStateException if a new outgoing call is not currently
     *                possible because no more call slots exist or a call exists
     *                that is dialing, alerting, ringing, or waiting. Other
     *                errors are handled asynchronously.
     */
    Connection vtDial(String dialString, UUSInfo uusInfo) throws CallStateException;

    void voiceAccept() throws CallStateException;
    /* vt end */

    /**
     * Sets the radio power on/off state (off is sometimes
     * called "airplane mode"). Current state can be gotten via
     * {@link #getServiceState()}.{@link
     * android.telephony.ServiceState#getState() getState()}.
     * <strong>Note: </strong>This request is asynchronous.
     * getServiceState().getState() will not change immediately after this call.
     * registerForServiceStateChanged() to find out when the
     * request is complete.
     * <p>
     *  If shutdown is true, will turn off the radio and SIM power.
     *   Used when shutdown the entire phone
     *
     * @param power true means "on", false means "off".
     * @param shutdown true means turn off entire phone
     */
      void setRadioPower(boolean power, boolean shutdown);

    /**
     * Get the current active PDP context list
     *
     * @deprecated
     * @param response <strong>On success</strong>, "response" bytes is
     * made available as:
     * (String[])(((AsyncResult)response.obj).result).
     * <strong>On failure</strong>,
     * (((AsyncResult)response.obj).result) == null and
     * (((AsyncResult)response.obj).exception) being an instance of
     * com.android.internal.telephony.gsm.CommandException
     */
    void getPdpContextList(Message response);

    /**
     * Returns the DNS servers for the network interface used by the specified
     * APN type.
     */
    public String[] getDnsServers(String apnType);

    /**
     * Configure cell broadcast SMS.
     * @param chIdList
     *            Channel ID list, fill in the fromServiceId, toServiceId, and selected
     *            in the SmsBroadcastConfigInfo only
     * @param langList
     *            Channel ID list, fill in the fromCodeScheme, toCodeScheme, and selected
     *            in the SmsBroadcastConfigInfo only
     * @param response
     *            Callback message is empty on completion
     */
    public void setCellBroadcastSmsConfig(SmsBroadcastConfigInfo[] chIdList,
            SmsBroadcastConfigInfo[] langList, Message response);

    /**
     * Query if the Cell broadcast is adtivated or not
     * @param response
     *            Callback message is empty on completion
     */
    public void queryCellBroadcastSmsActivation(Message response);

    /**
     * getFacilityLock
     * gets Call Barring States. The return value of
     * (AsyncResult)response.obj).result will be an Integer representing
     * the sum of enabled serivice classes (sum of SERVICE_CLASS_*)
     *
     * @param facility one of CB_FACILTY_*
     * @param password password or "" if not required
     * @param onComplete a callback message when the action is completed.
     */
    void getFacilityLock(String facility, String password, Message onComplete);

    /**
     * setFacilityLock
     * sets Call Barring options.
     *
     * @param facility one of CB_FACILTY_*
     * @param enable true means lock, false means unlock
     * @param password password or "" if not required
     * @param onComplete a callback message when the action is completed.
     */
    void setFacilityLock(String facility, boolean enable, String password, Message onComplete);

    /**
     * changeBarringPassword
     * changes Call Barring related password.
     *
     * @param facility one of CB_FACILTY_*
     * @param oldPwd old password
     * @param newPwd new password
     * @param onComplete a callback message when the action is completed.
     */
    void changeBarringPassword(String facility, String oldPwd, String newPwd, Message onComplete);

    /**
     * changeBarringPassword
     * changes Call Barring related password.
     *
     * @param facility one of CB_FACILTY_*
     * @param oldPwd old password
     * @param newPwd new password
     * @param newCfm
     * @param onComplete a callback message when the action is completed.
     */
    void changeBarringPassword(String facility, String oldPwd, String newPwd, String newCfm, Message onComplete);

    /**
     * used to release all connections in the MS,
     * release all connections with one reqeust together, not seperated.
     */
    void hangupAll() throws CallStateException;

    /**
     * used to release all connections in the MS,
     * release all connections with one reqeust together, not seperated.
     */
    void hangupAllEx() throws CallStateException;

    /**
     * used to release all connections in the foregrond call.
     */
    void hangupActiveCall() throws CallStateException;

    /**
     * used to get CCM.
     *
     * result.obj = AsyncResult ar
     *   ar.exception carries exception on failure
     *   ar.userObject contains the orignal value of result.obj
     *
     * ar.result is a String[1]
     * ar.result[0] contain the value of CCM
     * the value will be 3 bytes of hexadecimal format,
     * ex: "00001E" indicates decimal value 30
     */
    void getCurrentCallMeter(Message result);

    /**
     * used to get ACM.
     *
     * result.obj = AsyncResult ar
     *  ar.exception carries exception on failure
     *  ar.userObject contains the orignal value of result.obj
     *
     * ar.result is a String[1]
     * ar.result[0] contain the value of ACM
     * the value will be 3 bytes of hexadecimal format,
     * ex: "00001E" indicates decimal value 30
     */
    void getAccumulatedCallMeter(Message result);

    /**
     * used to get ACMMAX.
     *
     * result.obj = AsyncResult ar
     *  ar.exception carries exception on failure
     *  ar.userObject contains the orignal value of result.obj
     *
     * ar.result is a String[1]
     * ar.result[0] contain the value of ACMMax
     * the value will be 3 bytes of hexadecimal format,
     * ex: "00001E" indicates decimal value 30
     */
    void getAccumulatedCallMeterMaximum(Message result);

    /**
     * used to get price per unit and currency.
     *
     * result.obj = AsyncResult ar
     *  ar.exception carries exception on failure
     *  ar.userObject contains the orignal value of result.obj
     *
     * ar.result is a String[2]
     * ar.result[0] contain the value of currency, ex: "GBP"
     * ar.result[1] contain the value of ppu, ex: "2.66"
     */
    void getPpuAndCurrency(Message result);

     /**
     * used to set ACMMax.
     *
     * result.obj = AsyncResult ar
     *  ar.exception carries exception on failure
     *  ar.userObject contains the orignal value of result.obj
     *
     * @param acmmax is the maximum value for ACM. ex: "00001E"
     * @param pin2 is necessary parameter.
     */
    void setAccumulatedCallMeterMaximum(String acmmax, String pin2, Message result);

     /**
     * used to reset ACM.
     *
     *result.obj = AsyncResult ar
     *  ar.exception carries exception on failure
     *  ar.userObject contains the orignal value of result.obj
     *
     * @param pin2 is necessary parameter.
     */
    void resetAccumulatedCallMeter(String pin2, Message result);

     /**
     * used to set price per unit and currency.
     *
     *result.obj = AsyncResult ar
     *  ar.exception carries exception on failure
     *  ar.userObject contains the orignal value of result.obj
     *
     * @param currency is value of "currency". ex: "GBP"
     * @param ppu is the value of "price per unit". ex: "2.66"
     * @param pin2 is necessary parameter.
     */
    void setPpuAndCurrency(String currency, String ppu, String pin2, Message result);

    /**
     * Register for Neighboring cell info changed.
     * Message.obj will contain an AsyncResult.
     * AsyncResult.result will be a String[ ] instance
     */
    void registerForNeighboringInfo(Handler h, int what, Object obj);

    /**
     * Unregisters for Neighboring cell info changed notification.
     * Extraneous calls are tolerated silently
     */
    void unregisterForNeighboringInfo(Handler h);

    /**
     * Register for Network info changed.
     * Message.obj will contain an AsyncResult.
     * AsyncResult.result will be a String[ ] instance
     */
    void registerForNetworkInfo(Handler h, int what, Object obj);

    /**
     * Unregisters for Network info changed notification.
     * Extraneous calls are tolerated silently
     */
    void unregisterForNetworkInfo(Handler h);

    /**
     * Refresh Spn Display due to configuration change
     */
    void refreshSpnDisplay();

    /**
     * Request to get my SIM ID
     */
    int getMySimId();

    /**
     * Register for speech on/off indication.
     * Message.obj will contain an AsyncResult.
     * AsyncResult.result will be a String[ ] instance
     */
    void registerForSpeechInfo(Handler h, int what, Object obj);

    /**
     * Unregister for speech on/off indication
     */
    void unregisterForSpeechInfo(Handler h);

    /**
     * Get last call fail cause
     */
    public int getLastCallFailCause();

    /* vt start */
    /**
     * Register for VT status indication.
     * Message.obj will contain an AsyncResult.
     * AsyncResult.result will be a int[ ] instance
     */
    void registerForVtStatusInfo(Handler h, int what, Object obj);

    /**
     * Unregister for VT status indication
     */
    void unregisterForVtStatusInfo(Handler h);

    /**
     * Register for MT VT call indication.
     * Message.obj will contain an AsyncResult.
     */
    void registerForVtRingInfo(Handler h, int what, Object obj);

    /**
     * Unregister for MT VT call indication.
     */
    void unregisterForVtRingInfo(Handler h);

    /**
     * Register for call disconnect message when reject waiting vt/voice cal if active voice/vt call exists.
     * Message.obj will contain an AsyncResult.
     */
    public void registerForVtReplaceDisconnect(Handler h, int what, Object obj);

    /**
     * Unregister for call disconnect message.
     */
    public void unregisterForVtReplaceDisconnect(Handler h);
    /* vt end */

    /**
     * set GPRS transfer type: data prefer/call prefer
     */
    void setGprsTransferType(int type, Message response);

    //Add by mtk80372 for Barcode Number
    /**
     * Request to get Barcode number.
     */
    void getMobileRevisionAndIMEI(int type,Message result);

    //Add by mtk80372 for Barcode Number
    String getSN();

    /**
    *Retrieves the IccServiceStatus for the specic SIM/USIM service
    */
    IccServiceStatus getIccServiceStatus(IccService enService);

    /**
    *send BT SAP profile
    */
    void  sendBTSIMProfile(int nAction, int nType, String strData, Message response);

    /**
     * Request 2G context authentication for SIM/USIM
     */
    void doSimAuthentication (String strRand, Message result);

    /**
     * Request 3G context authentication for USIM
     */
    void doUSimAuthentication (String strRand, String strAutn, Message result);

    /* vt start */
    /**
     * getVtCallForwardingOptions
     * gets a VT call forwarding option. The return value of
     * ((AsyncResult)onComplete.obj) is an array of CallForwardInfo.
     *
     * @param commandInterfaceCFReason is one of the valid call forwarding
     *        CF_REASONS, as defined in
     *        <code>com.android.internal.telephony.CommandsInterface.</code>
     * @param onComplete a callback message when the action is completed.
     *        @see com.android.internal.telephony.CallForwardInfo for details.
     */
    void getVtCallForwardingOption(int commandInterfaceCFReason,
                                   Message onComplete);

    /**
     * setVtCallForwardingOptions
     * sets a VT call forwarding option.
     *
     * @param commandInterfaceCFReason is one of the valid call forwarding
     *        CF_REASONS, as defined in
     *        <code>com.android.internal.telephony.CommandsInterface.</code>
     * @param commandInterfaceCFAction is one of the valid call forwarding
     *        CF_ACTIONS, as defined in
     *       <code>com.android.internal.telephony.CommandsInterface.</code>
     * @param dialingNumber is the target phone number to forward calls to
     * @param timerSeconds is used by CFNRy to indicate the timeout before
     *       forwarding is attempted.
     * @param onComplete a callback message when the action is completed.
     */
    void setVtCallForwardingOption(int commandInterfaceCFReason,
                                   int commandInterfaceCFAction,
                                   String dialingNumber,
                                   int timerSeconds,
                                   Message onComplete);

    /**
     * getVtCallWaiting
     * gets VT call waiting activation state. The return value of
     * ((AsyncResult)onComplete.obj) is an array of int, with a length of 1.
     *
     * @param onComplete a callback message when the action is completed.
     *        @see com.android.internal.telephony.CommandsInterface#queryCallWaiting for details.
     */
    void getVtCallWaiting(Message onComplete);

    /**
     * setVtCallWaiting
     * sets VT call waiting state.
     *
     * @param enable is a boolean representing the state that you are
     *        requesting, true for enabled, false for disabled.
     * @param onComplete a callback message when the action is completed.
     */
    void setVtCallWaiting(boolean enable, Message onComplete);

    /**
     * getVtFacilityLock
     * gets VT Call Barring States. The return value of
     * (AsyncResult)response.obj).result will be an Integer representing
     * the sum of enabled serivice classes (sum of SERVICE_CLASS_*)
     *
     * @param facility one of CB_FACILTY_*
     * @param password password or "" if not required
     * @param onComplete a callback message when the action is completed.
     */
    void getVtFacilityLock(String facility, String password, Message onComplete);

    /**
     * setVtFacilityLock
     * sets VT Call Barring options.
     *
     * @param facility one of CB_FACILTY_*
     * @param enable true means lock, false means unlock
     * @param password password or "" if not required
     * @param onComplete a callback message when the action is completed.
     */
    void setVtFacilityLock(String facility, boolean enable, String password, Message onComplete);
    /* vt end */
    void updateSimIndicateState();
    int getSimIndicateState();

    /**
      *return true if the slot for this phone has SIM/USIM inserted even if airplane mode is on.
      */
    boolean isSimInsert();

    /* 3G Switch start */
    /**
     * get3GCapabilitySIM
     * get SIM with 3G capability.
     *
     * @return the id (slot) with 3G capability (Phone.GEMINI_SIM_ID_1 or Phone.GEMINI_SIM_ID_2).
     */
    int get3GCapabilitySIM();

    /**
     * set3GCapabilitySIM
     * set 3G capability to the SIM.
     *
     * @param the id (slot) of the SIM to have 3G capability.
     * @return the id (slot) with 3G capability (Phone.GEMINI_SIM_ID_1 or Phone.GEMINI_SIM_ID_2).
     */
    boolean set3GCapabilitySIM(int simId);
    /* 3G Switch end */

    void getPOLCapability(Message onComplete);
    void getPreferedOperatorList(Message onComplete);
    void setPOLEntry(NetworkInfoWithAcT networkWithAct, Message onComplete);

    void setCRO(int onoff, Message onComplete); //ALPS00279048
    // ALPS00302702 RAT balancing
    int getEfRatBalancing();
    int getEfRatBalancing(int simId);
    //MTK-END [mtk04070][111117][ALPS00093395]MTK proprietary methods

    // ALPS00294581
    void notifySimMissingStatus(boolean isSimInsert);

    // MVNO-API START
    String getSpNameInEfSpn();
    String getSpNameInEfSpn(int simId);

    String isOperatorMvnoForImsi();
    String isOperatorMvnoForImsi(int simId);

    boolean isIccCardProviderAsMvno();
    boolean isIccCardProviderAsMvno(int simId);
    // MVNO-API END

    /// M: google JB.MR1 patch,  phone's some static final variable move to PhoneConstants @{

    //MTK-START [mtk04070][111117][ALPS00093395]Used in GeminiPhone for SIM1/SIM2 color
    public static final int TOTAL_SIM_COLOR_COUNT = EncapsulationConstant.USE_MTK_PLATFORM ?
                                                        PhoneConstants.TOTAL_SIM_COLOR_COUNT : 4;
    //MTK-END [mtk04070][111117][ALPS00093395]Used in GeminiPhone for SIM1/SIM2 color

    public static final String STATE_KEY = EncapsulationConstant.USE_MTK_PLATFORM ?
                                                        PhoneConstants.STATE_KEY : "state";

    //MTK-START [mtk04070][111117][ALPS00093395]MTK added
    public static final int PHONE_TYPE_GEMINI = EncapsulationConstant.USE_MTK_PLATFORM ?
                                    PhoneConstants.PHONE_TYPE_GEMINI : RILConstants.GEMINI_PHONE;
    //MTK-END [mtk04070][111117][ALPS00093395]MTK added

    public static final String DATA_APN_TYPE_KEY = EncapsulationConstant.USE_MTK_PLATFORM ?
                                    PhoneConstants.DATA_APN_TYPE_KEY : "apnType";

    //MTK-START [mtk04070][111117][ALPS00093395]MTK added
    static final String REASON_GPRS_ATTACHED_TIMEOUT = EncapsulationConstant.USE_MTK_PLATFORM ?
                            PhoneConstants.REASON_GPRS_ATTACHED_TIMEOUT : "gprsAttachedTimeout";
    /* Add by mtk01411 */
    static final String REASON_ON_RADIO_AVAILABLE = EncapsulationConstant.USE_MTK_PLATFORM ?
                            PhoneConstants.REASON_ON_RADIO_AVAILABLE : "onRadioAvailable";
    static final String REASON_ON_RECORDS_LOADED = EncapsulationConstant.USE_MTK_PLATFORM ?
                            PhoneConstants.REASON_ON_RECORDS_LOADED : "onRecordsLoaded";
    static final String REASON_POLL_STATE_DONE = EncapsulationConstant.USE_MTK_PLATFORM ?
                            PhoneConstants.REASON_POLL_STATE_DONE : "pollStateDone";
    static final String REASON_NO_SUCH_PDP = EncapsulationConstant.USE_MTK_PLATFORM ?
                            PhoneConstants.REASON_NO_SUCH_PDP : "noSuchPdp";
    static final String REASON_PDP_NOT_ACTIVE = EncapsulationConstant.USE_MTK_PLATFORM ?
                            PhoneConstants.REASON_PDP_NOT_ACTIVE : "pdpNotActive";
    //MTK-END [mtk04070][111117][ALPS00093395]MTK added

    //MTK-START [mtk04070][111117][ALPS00093395]MTK added
    static final String APN_TYPE_DM = EncapsulationConstant.USE_MTK_PLATFORM ?
                            PhoneConstants.APN_TYPE_DM : "dm";
    static final String APN_TYPE_WAP = EncapsulationConstant.USE_MTK_PLATFORM ?
                            PhoneConstants.APN_TYPE_WAP : "wap";
    static final String APN_TYPE_NET = EncapsulationConstant.USE_MTK_PLATFORM ?
                            PhoneConstants.APN_TYPE_NET : "net";
    static final String APN_TYPE_CMMAIL = EncapsulationConstant.USE_MTK_PLATFORM ?
                            PhoneConstants.APN_TYPE_CMMAIL : "cmmail";
    static final String APN_TYPE_TETHERING = EncapsulationConstant.USE_MTK_PLATFORM ?
                            PhoneConstants.APN_TYPE_TETHERING : "tethering";
    //MTK-END [mtk04070][111117][ALPS00093395]MTK added

    //MTK-START [mtk04070][111117][ALPS00093395]MTK added
    /* Add by mtk01411 */
    static final int APN_REQUEST_FAILED_DUE_TO_RADIO_OFF = EncapsulationConstant.USE_MTK_PLATFORM ?
                            PhoneConstants.APN_REQUEST_FAILED_DUE_TO_RADIO_OFF : 98;
    static final int APN_TYPE_NOT_AVAILABLE_DUE_TO_RECORDS_NOT_LOADED = EncapsulationConstant.USE_MTK_PLATFORM ?
                            PhoneConstants.APN_TYPE_NOT_AVAILABLE_DUE_TO_RECORDS_NOT_LOADED : 99;
    static final int APN_TYPE_DISABLE_ONGOING = EncapsulationConstant.USE_MTK_PLATFORM ?
                            PhoneConstants.APN_TYPE_DISABLE_ONGOING : 100;

    public static final String APN_TYPE_ALL = EncapsulationConstant.USE_MTK_PLATFORM ?
                            PhoneConstants.APN_TYPE_ALL : "*";
    /** APN type for MMS traffic */
    public static final String APN_TYPE_MMS = EncapsulationConstant.USE_MTK_PLATFORM ?
                            PhoneConstants.APN_TYPE_MMS : "mms";
    /**
     * Return codes for <code>enableApnType()</code>
     */
    public static final int APN_ALREADY_ACTIVE  = EncapsulationConstant.USE_MTK_PLATFORM ?
                                            PhoneConstants.APN_ALREADY_ACTIVE : 0;
    public static final int APN_REQUEST_STARTED = EncapsulationConstant.USE_MTK_PLATFORM ?
                                            PhoneConstants.APN_REQUEST_STARTED : 1;
    public static final int APN_TYPE_NOT_AVAILABLE = EncapsulationConstant.USE_MTK_PLATFORM ?
                                            PhoneConstants.APN_TYPE_NOT_AVAILABLE : 2;
    public static final int APN_REQUEST_FAILED     = EncapsulationConstant.USE_MTK_PLATFORM ?
                                            PhoneConstants.APN_REQUEST_FAILED : 3;
    public static final int APN_ALREADY_INACTIVE   = EncapsulationConstant.USE_MTK_PLATFORM ?
                                            PhoneConstants.APN_ALREADY_INACTIVE : 4;

    /**
     * SIM ID for GEMINI
     */
    public static final int GEMINI_SIM_NUM = EncapsulationConstant.USE_MTK_PLATFORM ?
                                              PhoneConstants.GEMINI_SIM_NUM : 1;
    public static final int GEMINI_SIM_1 = EncapsulationConstant.USE_MTK_PLATFORM ?
                                            PhoneConstants.GEMINI_SIM_1 : 0;
    public static final int GEMINI_SIM_2 = EncapsulationConstant.USE_MTK_PLATFORM ?
                                            PhoneConstants.GEMINI_SIM_2 : 1;
    public static final int GEMINI_SIM_3 = EncapsulationConstant.USE_MTK_PLATFORM ?
                                            PhoneConstants.GEMINI_SIM_3 : 2;
    public static final int GEMINI_SIM_4 = EncapsulationConstant.USE_MTK_PLATFORM ?
                                            PhoneConstants.GEMINI_SIM_4 : 3;
    public static final int GEMINI_SIP_CALL = EncapsulationConstant.USE_MTK_PLATFORM ?
                                    PhoneConstants.GEMINI_SIP_CALL : -1; //MTK added for SIP call
    public static final String GEMINI_SIM_ID_KEY = EncapsulationConstant.USE_MTK_PLATFORM ?
                                            PhoneConstants.GEMINI_SIM_ID_KEY : "simId";
    public static final String MULTI_SIM_ID_KEY = EncapsulationConstant.USE_MTK_PLATFORM ?
                                            PhoneConstants.MULTI_SIM_ID_KEY : "simid";
    public static final String GEMINI_DEFAULT_SIM_PROP = EncapsulationConstant.USE_MTK_PLATFORM ?
                            PhoneConstants.GEMINI_DEFAULT_SIM_PROP : "persist.radio.default_sim";

    //MTK-START [mtk04070][111117][ALPS00093395]MTK added
    /** UNKNOWN, invalid value */
    public static final int SIM_INDICATOR_UNKNOWN = EncapsulationConstant.USE_MTK_PLATFORM ?
                                    PhoneConstants.SIM_INDICATOR_UNKNOWN : -1;
    /** ABSENT, no SIM/USIM card inserted for this phone */
    public static final int SIM_INDICATOR_ABSENT = EncapsulationConstant.USE_MTK_PLATFORM ?
                                    PhoneConstants.SIM_INDICATOR_ABSENT : 0;
    /** RADIOOFF,  has SIM/USIM inserted but not in use . */
    public static final int SIM_INDICATOR_RADIOOFF = EncapsulationConstant.USE_MTK_PLATFORM ?
                                    PhoneConstants.SIM_INDICATOR_RADIOOFF : 1;
    /** LOCKED,  has SIM/USIM inserted and the SIM/USIM has been locked. */
    public static final int SIM_INDICATOR_LOCKED = EncapsulationConstant.USE_MTK_PLATFORM ?
                                    PhoneConstants.SIM_INDICATOR_LOCKED : 2;
    /** INVALID : has SIM/USIM inserted and not be locked but failed to register to the network. */
    public static final int SIM_INDICATOR_INVALID = EncapsulationConstant.USE_MTK_PLATFORM ?
                                    PhoneConstants.SIM_INDICATOR_INVALID : 3;
    /** SEARCHING : has SIM/USIM inserted and SIM/USIM state is Ready and is searching for network. */
    public static final int SIM_INDICATOR_SEARCHING = EncapsulationConstant.USE_MTK_PLATFORM ?
                                    PhoneConstants.SIM_INDICATOR_SEARCHING : 4;
    /** NORMAL = has SIM/USIM inserted and in normal service(not roaming and has no data connection). */
    public static final int SIM_INDICATOR_NORMAL = EncapsulationConstant.USE_MTK_PLATFORM ?
                                    PhoneConstants.SIM_INDICATOR_NORMAL : 5;
    /** ROAMING : has SIM/USIM inserted and in roaming service(has no data connection). */
    public static final int SIM_INDICATOR_ROAMING = EncapsulationConstant.USE_MTK_PLATFORM ?
                                    PhoneConstants.SIM_INDICATOR_ROAMING : 6;
    /** CONNECTED : has SIM/USIM inserted and in normal service(not roaming) and data connected. */
    public static final int SIM_INDICATOR_CONNECTED = EncapsulationConstant.USE_MTK_PLATFORM ?
                                    PhoneConstants.SIM_INDICATOR_CONNECTED : 7;
    /** ROAMINGCONNECTED = has SIM/USIM inserted and in roaming service(not roaming) and data connected.*/
    public static final int SIM_INDICATOR_ROAMINGCONNECTED = EncapsulationConstant.USE_MTK_PLATFORM ?
                                    PhoneConstants.SIM_INDICATOR_ROAMINGCONNECTED : 8;
    /// @}
}
