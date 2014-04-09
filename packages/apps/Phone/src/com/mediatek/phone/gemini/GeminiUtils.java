package com.mediatek.phone.gemini;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import com.mediatek.phone.GeminiConstants;
import com.mediatek.phone.SIMInfoWrapper;
import com.mediatek.telephony.TelephonyManagerEx;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Message;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.provider.Telephony.SIMInfo;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.CallerInfo;
import com.android.internal.telephony.CallerInfoAsyncQuery;
import com.android.internal.telephony.CallerInfoAsyncQuery.OnQueryCompleteListener;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.IccFileHandler;
import com.android.internal.telephony.MmiCode;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneProxy;
import com.android.internal.telephony.TelephonyProperties;
import com.android.internal.telephony.gemini.GeminiNetworkSubUtil;
import com.android.internal.telephony.gemini.GeminiPhone;
import com.android.phone.PhoneGlobals;
import com.android.phone.PhoneUtils;

/**
 * M:Gemini+ For slot information and related methods.
 */
public final class GeminiUtils {
    private static final String TAG = "Gemini";
    private static final boolean DEBUG = true;

    private static GeminiUtils sInstance = new GeminiUtils();

    private static final int GET_SIM_RETRY_EMPTY = -1;

    /**
     * get the slot number of device.
     * 
     * @return
     */
    public static final int getSlotCount() {
        return GeminiConstants.SLOTS.length;
    }

    /**
     * PhoneConstants.GEMINI_SIM_1, PhoneConstants.GEMINI_SIM_2...
     * 
     * @return
     */
    public static int[] getSlots() {
        return GeminiConstants.SLOTS;
    }

    /**
     * @return PhoneConstants.GEMINI_SIM_1
     */
    public static int getDefaultSlot() {
        return GeminiConstants.SLOT_ID_1;
    }

    /**
     * This an temp solution for VIA, the cdma is always in slot 2.
     * 
     * @return PhoneConstants.GEMINI_SIM_1
     * @deprecated
     */
    public static int getCDMASlot() {
        return GeminiConstants.SLOT_ID_2;
    }

    /**
     * check the slotId value.
     * 
     * @param slotId
     * @return
     */
    public static boolean isValidSlot(int slotId) {
        final int[] geminiSlots = getSlots();
        for (int i = 0; i < geminiSlots.length; i++) {
            if (geminiSlots[i] == slotId) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the operator name by slotId.
     * 
     * @param slotId
     * @return
     */
    public static String getOperatorName(int slotId) {
        String operatorName = null;
        if (isGeminiSupport() && isValidSlot(slotId)) {
            SIMInfo simInfo = SIMInfoWrapper.getDefault().getSimInfoBySlot(slotId);
            if (simInfo != null) {
                operatorName = simInfo.mDisplayName;
                log("getOperatorName, operatorName= " + simInfo.mDisplayName);
            }
        } else {
            operatorName = getSystemProperties(TelephonyProperties.PROPERTY_OPERATOR_ALPHA);
        }
        log("getOperatorName, slotId=" + slotId + " operatorName=" + operatorName);
        return operatorName;
    }

    /**
     * get network operator name, read from SystemProperties.
     * 
     * @see PROPERTY_OPERATOR_GEMINI
     * @return
     */
    public static String getNetworkOperatorName() {
        String operatorName = null;
        if (isGeminiSupport()) {
            GeminiPhone gphone = (GeminiPhone) PhoneGlobals.getInstance().phone;
            final int[] geminiSlots = GeminiUtils.getSlots();
            for (int i = 0; i < geminiSlots.length; i++) {
                if (gphone.getStateGemini(geminiSlots[i]) != PhoneConstants.State.IDLE) {
                    operatorName = getSystemProperties(GeminiConstants.PROPERTY_OPERATOR_ALPHAS[i]);
                    log("getNetworkOperatorName operatorName:" + operatorName + ", slotId:"
                            + geminiSlots[i]);
                    break;
                }
            }
            // Give a chance for get mmi information
            if (operatorName == null
                    && PhoneGlobals.getInstance().mCM.getState() == PhoneConstants.State.IDLE) {
                for (int i = 0; i < geminiSlots.length; i++) {
                    if (gphone.getPendingMmiCodesGemini(geminiSlots[i]).size() != 0) {
                        operatorName = getSystemProperties(GeminiConstants.PROPERTY_OPERATOR_ALPHAS[i]);
                        log("getNetworkOperatorName operatorName:" + operatorName + ", slotId:"
                                + geminiSlots[i]);
                        break;
                    }
                }
            }
        } else {
            operatorName = getSystemProperties(TelephonyProperties.PROPERTY_OPERATOR_ALPHA);
        }
        log("getNetworkOperatorName operatorName = " + operatorName);
        return operatorName;
    }

    public static String getNetworkOperatorName(Call call) {
        if (isGeminiSupport() && call != null) {
            String operatorName = null;
            GeminiPhone phone = (GeminiPhone) PhoneGlobals.getInstance().phone;
            SIMInfo info = PhoneUtils.getSimInfoByCall(call);
            if (null != info && call.getState() != Call.State.IDLE) {
                final int slotIndex = getIndexInArray(info.mSlot, getSlots());
                if (slotIndex >= 0) {
                    return getSystemProperties(GeminiConstants.PROPERTY_OPERATOR_ALPHAS[slotIndex]);
                }
            }
        }
        return getNetworkOperatorName();
    }

    /**
     * get VT network operator name. The operator name gets from SystemProperties.
     * {@link PROPERTY_OPERATOR_GEMINI} {@link get3GCapabilitySIM}
     * 
     * @return
     */
    public static String getVTNetworkOperatorName(Call call) {
        String operatorName = null;
        if (isGeminiSupport() && call != null) {
            int slot = -1;
            SIMInfo info = PhoneUtils.getSimInfoByCall(call);
            if (null != call && null != info && call.getState() != Call.State.IDLE) {
                slot = info.mSlot;
            }
            int index = getIndexInArray(slot, getSlots());
            if (index >= 0) {
                operatorName = getSystemProperties(GeminiConstants.PROPERTY_OPERATOR_ALPHAS[index]);
            }
        } else {
            operatorName = getSystemProperties(TelephonyProperties.PROPERTY_OPERATOR_ALPHA);
        }
        log("getVTNetworkOperatorName, operatorName= " + operatorName);
        return operatorName;
    }

    /**
     * get 3G capability slotId by ITelephony.get3GCapabilitySIM();
     * 
     * @return the SIM id which support 3G.
     */
    public static int get3GCapabilitySIM() {
        ITelephony iTelephony = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
        try {
            final int slot3G = iTelephony.get3GCapabilitySIM();
            log("get3GCapabilitySIM, slot3G" + slot3G);
            return iTelephony.get3GCapabilitySIM();
        } catch (android.os.RemoteException re) {
            log("get3GCapabilitySIM, " + re.getMessage() + ", return -1");
            return -1;
        }
    }

    /**
     * get voiceMailNumber by slotId.
     * 
     * @param slotId
     * @return
     */
    public static String getVoiceMailNumber(int slotId) {
        String voiceMailNumber = null;
        if (isGeminiSupport() && isValidSlot(slotId)) {
            //replace deprecated API.
            //voiceMailNumber = telephonyManager.getVoiceMailNumberGemini(slotId);
            voiceMailNumber = TelephonyManagerEx.getDefault().getVoiceMailNumber(slotId);
        } else {
            voiceMailNumber = TelephonyManager.getDefault().getVoiceMailNumber();
        }
        return voiceMailNumber;
    }

    /**
     * get CallerInfo by number & slotId. If GEMINI support, get CallerInfo by
     * CallerInfo.getCallerInfoGemini(..), else CallerInfo.getCallerInfo(..)
     * 
     * @param context
     * @param number
     * @param slotId
     * @return
     */
    public static CallerInfo getCallerInfo(Context context, String number, int slotId) {
        if (isGeminiSupport() && isValidSlot(slotId)) {
            return CallerInfo.getCallerInfoGemini(context, number, slotId);
        }
        return CallerInfo.getCallerInfo(context, number);
    }

    public static Uri getSimFdnUri(int slotId) {
        Uri uri = null;
        final int index = getIndexInArray(slotId, getSlots());
        if (index >= 0) {
            uri = Uri.parse(GeminiConstants.FDN_CONTENT_GEMINI[index]);
        } else {
            uri = Uri.parse(GeminiConstants.FDN_CONTENT);
        }
        return uri;
    }

    public static int getPinRetryNumber(int slotId) {
        final int index = getIndexInArray(slotId, getSlots());
        int number = -1;
        if (index >= 0) {
            number = getIntSystemProperties(GeminiConstants.GSM_SIM_RETRY_PIN_GEMINI[index], -1);
        }
        return number;
    }

    public static int getPin2RetryNumber(int slotId) {
        final int index = getIndexInArray(slotId, getSlots());
        int number = -1;
        if (index >= 0) {
            number = getIntSystemProperties(GeminiConstants.GSM_SIM_RETRY_PIN2_GEMINI[index], -1);
        }
        return number;
    }

    public static int getPuk2RetryNumber(int slotId) {
        final int index = getIndexInArray(slotId, getSlots());
        int number = -1;
        if (index >= 0) {
            number = getIntSystemProperties(GeminiConstants.GSM_SIM_RETRY_PUK2_GEMINI[index], -1);
        }
        return number;
    }

    /**
     * get the index position of value in the array. If the array doesn't
     * contains the value, return -1.
     * 
     * @param value
     * @param array
     * @return
     */
    public static int getIndexInArray(int value, int[] array) {
        for (int i = 0; i < array.length; i++) {
            if (value == array[i]) {
                return i;
            }
        }
        log("getIndexInArray failed, value=" + value + ", array=" + array.toString());
        return -1;
    }

    /**
     * @see FeatureOption.MTK_GEMINI_SUPPORT
     * @see FeatureOption.MTK_GEMINI_3SIM_SUPPORT
     * @see FeatureOption.MTK_GEMINI_4SIM_SUPPORT
     * @return true if the device has 2 or more slots
     */
    public static boolean isGeminiSupport() {
        return GeminiConstants.SOLT_NUM >= 2;
    }

    /**
     * agency method for application to get {@link CallerInfoAsyncQuery}. if
     * GEMINI and isSipPhone is false, it calls
     * {@link CallerInfoAsyncQuery#startQueryGemini}, else
     * {@link CallerInfoAsyncQuery#startQuery}
     *
     * @param token
     * @param context
     * @param number
     * @param listener
     * @param cookie
     * @param simId
     * @param isSipPhone
     * @return
     */
    public static CallerInfoAsyncQuery startQueryGemini(int token, Context context, String number,
            OnQueryCompleteListener listener, Object cookie, int simId, boolean isSipPhone) {
        CallerInfoAsyncQuery asyncQuery = null;
        if (isGeminiSupport() && !isSipPhone) {
            asyncQuery = CallerInfoAsyncQuery.startQueryGemini(token, context, number, listener,
                    cookie, simId);

        } else {
            asyncQuery = CallerInfoAsyncQuery.startQuery(token, context, number, listener, cookie);
        }
        return asyncQuery;
    }

    /**
     * get slotId by phone type
     *
     * @param phontType {@link PhoneConstants.PHONE_TYPE_CDMA},
     *            {@link PhoneConstants.PHONE_TYPE_GSM}
     * @return
     */
    public static int getSlotByPhoneType(int phontType) {
        int slot = PhoneConstants.GEMINI_SIM_1;
        if (phontType == PhoneConstants.PHONE_TYPE_CDMA
                || phontType == PhoneConstants.PHONE_TYPE_GSM) {
            TelephonyManager telephony = TelephonyManager.getDefault();
            final int[] geminiSlots = GeminiUtils.getSlots();
            for (int gs : geminiSlots) {
                if (telephony.getPhoneTypeGemini(gs) == phontType) {
                    slot = gs;
                    break;
                }
            }
        }
        log("getSlotByPhoneType with phontType = " + phontType + " and return slot = " + slot);
        return slot;
    }

    /**
     * Get default Phone from CallManager.
     *
     * @return
     */
    public static Phone getDefaultPhone() {
        if (GeminiUtils.isGeminiSupport()) {
            return PhoneGlobals.getInstance().mCMGemini.getDefaultPhoneGemini();
        } else {
            return PhoneGlobals.getInstance().mCM.getDefaultPhone();
        }
    }

    /**
     * get voice mail number, default gets from Phone.getVoiceMailNumber(), if GEMINI, call by
     * GeminiPhone.
     * 
     * @param slotId
     * @return
     */
    public static String getVoiceMailNumber(Phone phone, int slotId) {
        Assert.assertNotNull(phone);
        String vmNumber;
        if (GeminiUtils.isGeminiSupport() && isValidSlot(slotId)) {
            vmNumber = ((GeminiPhone) phone).getVoiceMailNumberGemini(slotId);
        } else {
            vmNumber = phone.getVoiceMailNumber();
        }
        log("getVoiceMailNumber : vmNumber:" + vmNumber + " slotId=" + slotId);
        return vmNumber;
    }

    /**
     * getIccRecordsLoaded(), if GEMINI, call by GeminiPhone.
     * 
     * @param slotId
     * @return
     */
    public static boolean getIccRecordsLoaded(Phone phone, int slotId) {
        Assert.assertNotNull(phone);
        boolean iccRecordloaded = false;
        if (GeminiUtils.isGeminiSupport() && isValidSlot(slotId)) {
            iccRecordloaded = ((GeminiPhone) phone).getIccRecordsLoadedGemini(slotId);
        } else {
            iccRecordloaded = phone.getIccRecordsLoaded();
        }
        log("getIccRecordsLoaded : iccRecordloaded:" + iccRecordloaded + ", slotId:" + slotId);
        return iccRecordloaded;
    }

    /**
     * get the slotId's IccCard.
     * 
     * @param phone
     * @param slotId
     * @return
     */
    public static IccCard getIccCard(Phone phone, int slotId) {
        Assert.assertNotNull(phone);
        if (GeminiUtils.isGeminiSupport() && isValidSlot(slotId)) {
            return ((GeminiPhone) phone).getIccCardGemini(slotId);
        }
        return phone.getIccCard();
    }

    public static boolean isPhbReady(Phone phone, int slotId) {
        Assert.assertNotNull(phone);
        final boolean isPhbReady = getIccCard(phone, slotId).isPhbReady();
        log("getIccRecordsLoaded : isPhbReady:" + isPhbReady + ", slotId:" + slotId);
        return isPhbReady;
    }

    /**
     * Handles PIN MMI commands (PIN/PIN2/PUK/PUK2), which are initiated without SEND (so dial is
     * not appropriate).
     * 
     * @param phone
     * @param dialString
     * @param slotId
     * @return
     */
    public static boolean handlePinMmi(Phone phone, String dialString, int slotId) {
        Assert.assertNotNull(phone);
        boolean result = false;
        if (GeminiUtils.isGeminiSupport() && isValidSlot(slotId)) {
            result = ((GeminiPhone) phone).handlePinMmiGemini(dialString, slotId);
        } else {
            result = phone.handlePinMmi(dialString);
        }
        log("handlePinMmi : result:" + result + " dialString:" + dialString + ", slotId:" + slotId);
        return result;
    }

    /**
     * Gets the default SMSC address.
     * 
     * @param phone
     * @param msg
     * @param slotId
     */
    public static void getSmscAddress(Phone phone, Message msg, int slotId) {
        Assert.assertNotNull(phone);
        log("getSmscAddress: slotId=" + slotId);
        if (GeminiUtils.isGeminiSupport() && isValidSlot(slotId)) {
            ((GeminiPhone) phone).getSmscAddressGemini(msg, slotId);
        } else {
            phone.getSmscAddress(msg);
        }
    }

    /**
     * Sets the default SMSC address.
     * 
     * @param phone
     * @param scAddr
     * @param msg
     * @param slotId
     */
    public static void setSmscAddress(Phone phone, String scAddr, Message msg, int slotId) {
        Assert.assertNotNull(phone);
        log("setSmscAddress: slotId=" + slotId);
        if (GeminiUtils.isValidSlot(slotId)) {
            if (GeminiUtils.isGeminiSupport()) {
                ((GeminiPhone) phone).setSmscAddressGemini(scAddr, msg, slotId);
            } else {
                phone.setSmscAddress(scAddr, msg);
            }
        }
    }

    /**
     * @param phone
     * @return true if the ring call contains only disconnected connections
     */
    public static boolean isPhoneRingingCallIdle(Phone phone) {
        Assert.assertNotNull(phone);
        log("isPhoneRingingCallIdle :" + phone.getRingingCall().isIdle());
        return phone.getRingingCall().isIdle();
    }

    /**
     * @param phone
     * @return true if the fg call contains only disconnected connections
     */
    public static boolean isPhoneForegroundCallIdle(Phone phone) {
        Assert.assertNotNull(phone);
        log("isPhoneForegroundCallIdle :" + phone.getForegroundCall().isIdle());
        return phone.getForegroundCall().isIdle();
    }

    /**
     * @param phone
     * @return true if the bg call contains only disconnected connections
     */
    public static boolean isPhoneBackgroundCallIdle(Phone phone) {
        Assert.assertNotNull(phone);
        log("isPhoneBackgroundCallIdle :" + phone.getBackgroundCall().isIdle());
        return phone.getBackgroundCall().isIdle();
    }

    /**
     * Returns the name of the network interface used by the specified APN type.
     * 
     * @param phone
     * @param apnType
     * @param slotId
     * @return
     */
    public String getInterfaceName(Phone phone, String apnType, int slotId) {
        Assert.assertNotNull(phone);
        String interfaceName = null;
        ;
        if (GeminiUtils.isGeminiSupport() && isValidSlot(slotId)) {
            interfaceName = ((GeminiPhone) phone).getInterfaceNameGemini(apnType, slotId);
        } else {
            interfaceName = phone.getInterfaceName(apnType);
        }
        log("getGateway, interfaceName:" + interfaceName + " apnType:" + apnType + ", slotId:"
                + slotId);
        return interfaceName;
    }

    public String getIpAddress(Phone phone, String apnType, int slotId) {
        Assert.assertNotNull(phone);
        String ipAddr = null;
        if (GeminiUtils.isGeminiSupport() && isValidSlot(slotId)) {
            ipAddr = ((GeminiPhone) phone).getIpAddressGemini(apnType, slotId);
        } else {
            ipAddr = phone.getIpAddress(apnType);
        }
        log("getGateway, ipAddr:" + ipAddr + " apnType:" + apnType + ", slotId:" + slotId);
        return ipAddr;
    }

    /**
     * Returns the gateway for the network interface used by the specified APN type
     * 
     * @param phone
     * @param apnType
     * @param slotId
     * @return
     */
    public String getGateway(Phone phone, String apnType, int slotId) {
        Assert.assertNotNull(phone);
        String gateWay = null;
        if (GeminiUtils.isGeminiSupport() && isValidSlot(slotId)) {
            gateWay = ((GeminiPhone) phone).getGatewayGemini(apnType, slotId);
        } else {
            gateWay = phone.getGateway(apnType);
        }
        log("getGateway, gateWay:" + gateWay + " apnType:" + apnType + ", slotId:" + slotId);
        return gateWay;
    }

    /**
     * @param phone
     * @param response
     * @param slotId
     */
    public static void getPhbRecordInfo(Phone phone, Message response, int slotId) {
        Assert.assertNotNull(phone);
        if (GeminiUtils.isGeminiSupport() && isValidSlot(slotId)) {
            IccFileHandler filehandle = ((GeminiPhone) phone).getIccFileHandlerGemini(slotId);
            filehandle.getPhbRecordInfo(response);
        } else {
            IccFileHandler filehandle = ((PhoneProxy) phone).getIccFileHandler();
            filehandle.getPhbRecordInfo(response);
        }
        log("getPhbRecordInfo, response:" + response + " slotId:" + slotId);
    }

    /**
     * Get voice message waiting indicator status. No change notification available on this
     * interface. Use PhoneStateNotifier or similar instead.]
     * 
     * @param phone
     * @param slotId
     * @return
     */
    public static boolean getMessageWaitingIndicator(Phone phone, int slotId) {
        Assert.assertNotNull(phone);
        boolean indicator = false;
        if (GeminiUtils.isGeminiSupport() && isValidSlot(slotId)) {
            indicator = ((GeminiPhone) phone).getMessageWaitingIndicatorGemini(slotId);
        } else {
            indicator = phone.getMessageWaitingIndicator();
        }
        log("getMessageWaitingIndicator, indicator:" + indicator);
        return indicator;
    }

    /**
     * Sets the radio power on/off state (off is sometimes called "airplane mode"). Current state
     * can be gotten via getServiceState(). getState(). Note: This request is asynchronous.
     * getServiceState().getState() will not change immediately after this call.
     * registerForServiceStateChanged() to find out when the request is complete.
     * 
     * @param phone
     * @param isRadioOn
     * @param contentResolver
     */
    public static void setRadioMode(Phone phone, boolean isRadioOn, ContentResolver contentResolver) {
        Assert.assertNotNull(phone);
        if (GeminiUtils.isGeminiSupport()) {
            int dualSimModeSetting = GeminiNetworkSubUtil.MODE_FLIGHT_MODE;
            if (!isRadioOn && contentResolver != null) {
                dualSimModeSetting = Settings.Global.getInt(contentResolver,
                        Settings.System.DUAL_SIM_MODE_SETTING, GeminiNetworkSubUtil.MODE_DUAL_SIM);
            }
            ((GeminiPhone) phone).setRadioMode(dualSimModeSetting);
            log("setRadioPower, isRadioOn:" + isRadioOn + ", dualSimModeSetting:"
                    + dualSimModeSetting);
        } else {
            phone.setRadioPower(!isRadioOn);
        }
        log("setRadioPower, isRadioOn:" + isRadioOn);
    }

    /**
     * Sets the radio power on/off state (off is sometimes called "airplane mode"). Current state
     * can be gotten via getServiceState(). getState(). Note: This request is asynchronous.
     * getServiceState().getState() will not change immediately after this call.
     * registerForServiceStateChanged() to find out when the request is complete. If shutdown is
     * true, will turn off the radio and SIM power. Used when shutdown the entire phone
     * 
     * @param phone
     */
    public static void setRadioPowerOFF(Phone phone) {
        Assert.assertNotNull(phone);
        if (GeminiUtils.isGeminiSupport()) {
            ((GeminiPhone) phone).setRadioMode(GeminiNetworkSubUtil.MODE_POWER_OFF);
        } else {
            phone.setRadioPower(false, true);
        }
        log("setRadioPowerOFF");
    }

    /**
     * Requests to set the preferred network type for searching and registering (CS/PS domain, RAT,
     * and operation mode)
     * 
     * @param phone
     * @param modemNetworkMode
     * @param msg
     * @param slotId
     */
    public static void setPreferredNetworkType(Phone phone, int modemNetworkMode, Message msg,
            int slotId) {
        Assert.assertNotNull(phone);
        if (GeminiUtils.isGeminiSupport() && isValidSlot(slotId)) {
            ((GeminiPhone) phone).setPreferredNetworkTypeGemini(modemNetworkMode, msg, slotId);
        } else {
            phone.setPreferredNetworkType(modemNetworkMode, msg);
        }
        log("setPreferredNetworkType, modemNetworkMode:" + modemNetworkMode + " slotId:" + slotId);
    }

    /**
     * return true if the phone's foregroundCall call state is {@link Call#State#DIALING}
     * 
     * @param phone
     * @return
     */
    public static boolean isDialing(Phone phone) {
        Assert.assertNotNull(phone);
        boolean isDialing = false;
        if (GeminiUtils.isGeminiSupport()) {
            isDialing = (((GeminiPhone) phone).getForegroundCall().getState() == Call.State.DIALING);
        } else {
            isDialing = (phone.getForegroundCall().getState() == Call.State.DIALING);
        }
        log("isDialing, isDialing:" + isDialing);
        return isDialing;
    }

    /**
     * send BT SAP profile
     * 
     * @param phone
     * @param action
     * @param type
     * @param data
     * @param callback
     * @param slotId
     */
    public static void sendBTSIMProfile(Phone phone, int action, int type, String data,
            Message callback, int slotId) {
        Assert.assertNotNull(phone);
        log("sendBTSIMProfile, callback:" + callback + ", slotId:" + slotId);
        if (GeminiUtils.isGeminiSupport() && isValidSlot(slotId)) {
            ((GeminiPhone) phone).sendBTSIMProfileGemini(action, type, data, callback, slotId);
        } else {
            phone.sendBTSIMProfile(action, type, data, callback);
        }
    }

    /**
     * Becasue of support G+C, the GeminiPhone may contains CDMAPhone, so must get the exactly phone
     * type:
     * 
     * @param phone
     * @return
     */
    public static int getPhoneType(Phone phone, int slotId) {
        Assert.assertNotNull(phone);
        int phoneType = PhoneConstants.PHONE_TYPE_GSM;
        if (GeminiUtils.isGeminiSupport() && phone instanceof GeminiPhone && isValidSlot(slotId)) {
            phoneType = ((GeminiPhone) phone).getPhonebyId(slotId).getPhoneType();
        } else {
            phoneType = phone.getPhoneType();
        }
        log("getPhoneType, slotId:" + slotId + ", phoneType:" + phoneType);
        return phoneType;
    }

    /**
     * Sends user response to a USSD REQUEST message. An MmiCode instance representing this response
     * is sent to handlers registered with registerForMmiInitiate.
     * 
     * @param phone
     * @param text
     * @param slotId
     */
    public static void sendUssdResponse(Phone phone, String text, int slotId) {
        Assert.assertNotNull(phone);
        log("sendUssdResponse, text:" + text + ", slotId:" + slotId);
        if (GeminiUtils.isGeminiSupport() && isValidSlot(slotId)) {
            ((GeminiPhone) phone).sendUssdResponseGemini(text, slotId);
        } else {
            phone.sendUssdResponse(text);
        }
    }

    /**
     * Returns a list of MMI codes that are pending. (They have initiated but have not yet
     * completed). Presently there is only ever one. Use registerForMmiInitiate and
     * registerForMmiComplete for change notification.
     * 
     * @param phone
     * @param slotId
     * @return
     */
    public static List<? extends MmiCode> getPendingMmiCodes(Phone phone, int slotId) {
        Assert.assertNotNull(phone);
        List<? extends MmiCode> mmiCodes = null;
        if (GeminiUtils.isGeminiSupport() && isValidSlot(slotId)) {
            mmiCodes = ((GeminiPhone) phone).getPendingMmiCodesGemini(slotId);
        } else {
            mmiCodes = phone.getPendingMmiCodes();
        }
        if (mmiCodes == null) {
            mmiCodes = new ArrayList<MmiCode>();
        }
        return mmiCodes;
    }

    /**
     * return true MMI codes size > 0
     * 
     * @see getPendingMmiCodes
     * @param phone
     * @return
     */
    public static boolean hasPendingMmi(Phone phone) {
        Assert.assertNotNull(phone);
        int mmiCount = 0;
        if (GeminiUtils.isGeminiSupport()) {
            GeminiPhone gphone = (GeminiPhone) phone;
            final int[] geminiSlots = GeminiUtils.getSlots();
            log("hasPendingMmi mmiCount slot size:" + geminiSlots.length);
            for (int gs : geminiSlots) {
                mmiCount += gphone.getPendingMmiCodesGemini(gs).size();
            }
        } else {
            mmiCount = phone.getPendingMmiCodes().size();
        }
        log("hasPendingMmi mmiCount=" + mmiCount);
        return mmiCount > 0;
    }

    /**
     * used to release all connections in the MS, release all connections with one reqeust together,
     * not seperated.
     * 
     * @param phone
     */
    public static void hangupAll(Phone phone) {
        Assert.assertNotNull(phone);
        try {
            if (GeminiUtils.isGeminiSupport()) {
                GeminiPhone gphone = (GeminiPhone) phone;
                int[] geminiSlots = GeminiUtils.getSlots();
                for (int gs : geminiSlots) {
                    gphone.hangupAllGemini(gs);
                }
            } else {
                phone.hangupAllEx();
            }
        } catch (CallStateException ex) {
            log("Error, cannot hangup All Calls");
        }
    }

    public static int getSlotNotIdle(Phone phone) {
        Assert.assertNotNull(phone);
        if (phone instanceof GeminiPhone) {
            GeminiPhone gPhone = (GeminiPhone) phone;
            int[] geminiSlots = GeminiUtils.getSlots();
            for (int slot : geminiSlots) {
                if (gPhone.getStateGemini(slot) != PhoneConstants.State.IDLE) {
                    return slot;
                }
            }
        } else {
            if (phone.getState() != PhoneConstants.State.IDLE) {
                return GeminiConstants.SLOT_ID_1;
            }
        }
        return -1;
    }

    private static String getSystemProperties(String key) {
        return SystemProperties.get(key);
    }

    private static int getIntSystemProperties(String key, int defValue) {
        return SystemProperties.getInt(key, defValue);
    }

    private static void log(String msg) {
        if (DEBUG) {
            Log.d(TAG, msg);
        }
    }
}