package com.mediatek.encapsulation.com.android.internal.telephony;

import android.content.Context;
import android.os.ServiceManager;
import android.os.Bundle;
import android.os.IBinder;
import android.telephony.NeighboringCellInfo;
import android.telephony.CellInfo;
import com.android.internal.telephony.ITelephony;
import com.mediatek.encapsulation.EncapsulationConstant;
import com.mediatek.encapsulation.MmsLog;
import java.util.List;

//MTK-START [mtk04070][111117][ALPS00093395]MTK added
import android.os.Message;
import android.telephony.BtSimapOperResponse;
//MTK-END [mtk04070][111117][ALPS00093395]MTK added

/**
 * ITelephony used to interact with the phone.  Mostly this is used by the
 * TelephonyManager class.  A few places are still using this directly.
 * Please clean them up if possible and use TelephonyManager instead.
 */
public class EncapsulatedTelephonyService {

    /** M: MTK reference ITelephony */
    private static ITelephony sTelephony;
    private static EncapsulatedTelephonyService sTelephonyService = new EncapsulatedTelephonyService();;

    private EncapsulatedTelephonyService() {}

    synchronized public static EncapsulatedTelephonyService getInstance() {
        if (sTelephony != null) {
            return sTelephonyService;
        } else {
            sTelephony = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
            if (null != sTelephony) {
                return sTelephonyService;
            } else {
                return null;
            }
        }
    }


    /** M: NFC SEEK start */
    /**
     * Returns the response APDU for a command APDU sent to a logical channel
     */
    public String transmitIccLogicalChannel(int cla, int command, int channel,
            int p1, int p2, int p3, String data) throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return sTelephony.transmitIccLogicalChannel(cla, command, channel, p1, p2, p3, data);
        } else {
            return null;
        }
    }

    /**
     * Returns the response APDU for a command APDU sent to a logical channel for Gemini-Card
     */
    public String transmitIccLogicalChannelGemini(int cla, int command, int channel,
            int p1, int p2, int p3, String data, int simId) throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return sTelephony.transmitIccLogicalChannelGemini(cla, command, channel, p1,
                                                                p2, p3, data, simId);
        } else {
            return null;
        }
    }

    /**
     * Returns the response APDU for a command APDU sent to the basic channel
     */
    public String transmitIccBasicChannel(int cla, int command,
            int p1, int p2, int p3, String data) throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return sTelephony.transmitIccBasicChannel(cla, command, p1, p2, p3, data);
        } else {
            return null;
        }
    }

    /**
     * Returns the response APDU for a command APDU sent to the basic channel for Gemini-Card
     */
    public String transmitIccBasicChannelGemini(int cla, int command,
            int p1, int p2, int p3, String data, int simId) throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return sTelephony.transmitIccBasicChannelGemini(cla, command, p1, p2, p3, data, simId);
        } else {
            return null;
        }
    }

    /**
     * Returns the channel id of the logical channel,
     * Returns 0 on error.
     */
    public int openIccLogicalChannel(String AID) throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return sTelephony.openIccLogicalChannel(AID);
        } else {
            return 0;
        }
    }

    /**
     * Returns the channel id of the logical channel for Gemini-Card,
     * Returns 0 on error.
     */
    public int openIccLogicalChannelGemini(String AID, int simId) throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return sTelephony.openIccLogicalChannelGemini(AID, simId);
        } else {
            return 0;
        }
    }

    /**
     * Return true if logical channel was closed successfully
     */
    public boolean closeIccLogicalChannel(int channel) throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return sTelephony.closeIccLogicalChannel(channel);
        } else {
            return false;
        }
    }

    /**
     * Return true if logical channel was closed successfully for Gemini-Card
     */
    public boolean closeIccLogicalChannelGemini(int channel, int simId) throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return sTelephony.closeIccLogicalChannelGemini(channel, simId);
        } else {
            return false;
        }
    }

    /**
     * Returns the error code of the last error occured.
     * Currently only used for openIccLogicalChannel
     */
    public int getLastError() throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return sTelephony.getLastError();
        } else {
            return 0;
        }
    }

    /**
     * Returns the error code of the last error occured for Gemini-Card.
     * Currently only used for openIccLogicalChannel
     */
    public int getLastErrorGemini(int simId) throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return sTelephony.getLastErrorGemini(simId);
        } else {
            return 0;
        }
    }

    /**
     * Returns the response APDU for a command APDU sent through SIM_IO
     */
    public byte[] transmitIccSimIO(int fileID, int command, int p1, int p2,
                                    int p3, String filePath) throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return sTelephony.transmitIccSimIO(fileID, command, p1, p2, p3, filePath);
        } else {
            return null;
        }
    }

    /**
     * Returns the response APDU for a command APDU sent through SIM_IO for Gemini-Card
     */
    public byte[] transmitIccSimIOGemini(int fileID, int command, int p1, int p2,
                                    int p3, String filePath, int simId) throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return sTelephony.transmitIccSimIOGemini(fileID, command, p1, p2, p3, filePath, simId);
        } else {
            return null;
        }
    }

    /**
     * Returns SIM's ATR in hex format
     */
    public String getIccATR() throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return sTelephony.getIccATR();
        } else {
            return null;
        }
    }

    /**
     * Returns SIM's ATR in hex format for Gemini-Card
     */
    public String getIccATRGemini(int simId) throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return sTelephony.getIccATRGemini(simId);
        } else {
            return null;
        }
    }
    // NFC SEEK end

    //MTK-START [mtk04070][111117][ALPS00093395]MTK proprietary methods
    /**
     * Check if the phone is idle for voice call only.
     * @return true if the phone state is for voice call only.
     */
    public boolean isVoiceIdle() throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return sTelephony.isVoiceIdle();
        } else {
            return false;
        }
    }

    /**
     * Returns the IccCard type. Return "SIM" for SIM card or "USIM" for USIM card.
     */
    public String getIccCardType() throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return sTelephony.getIccCardType();
        } else {
            MmsLog.d("Encapsulation issue", "EncapsulatedTelephonyService -- getIccCardType()");
            return null;
        }
    }

    /**
     * Do sim authentication and return the raw data of result.
     * Returns the hex format string of auth result.
     * <p> random string in hex format
     */
    public String simAuth(String strRand) throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return sTelephony.simAuth(strRand);
        } else {
            return null;
        }
    }

    /**
     * Do usim authentication and return the raw data of result.
     * Returns the hex format string of auth result.
     * <p> random string in hex format
     */
    public String uSimAuth(String strRand, String strAutn) throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return sTelephony.uSimAuth(strRand, strAutn);
        } else {
            return null;
        }
    }

    /**
     * Shutdown Radio
     */
    public boolean setRadioOff() throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return sTelephony.setRadioOff();
        } else {
            return false;
        }
    }

    public int getPreciseCallState() throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return sTelephony.getPreciseCallState();
        } else {
            return 0;
        }
    }

    /**
     * Return ture if the ICC card is a test card
     */
    public boolean isTestIccCard() throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return sTelephony.isTestIccCard();
        } else {
            MmsLog.d("Encapsulation issue", "EncapsulatedTelephonyService -- isTestIccCard()");
            return false;
        }
    }

    /**
    * Return true if the FDN of the ICC card is enabled
    */
    public boolean isFDNEnabled() throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return sTelephony.isFDNEnabled();
        } else {
            return false;
        }
    }

    /**
     * refer to dial(String number);
     */
    public void dialGemini(String number, int simId) throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            sTelephony.dialGemini(number, simId);
        } else {
        }
    }

    /**
     * refer to call(String number);
     */
    public void callGemini(String number, int simId) throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            sTelephony.callGemini(number, simId);
        } else {
        }
    }

    /**
     * refer to showCallScreen();
     */
    public boolean showCallScreenGemini(int simId) throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return sTelephony.showCallScreenGemini(simId);
        } else {
            return false;
        }
    }

    /**
     * refer to showCallScreenWithDialpad(boolean showDialpad)
     */
    public boolean showCallScreenWithDialpadGemini(boolean showDialpad, int simId) throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return sTelephony.showCallScreenWithDialpadGemini(showDialpad, simId);
        } else {
            return false;
        }
    }

    /**
     * refer to endCall().
     */
    public boolean endCallGemini(int simId) throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return sTelephony.endCallGemini(simId);
        } else {
            return false;
        }
    }

    /**
     * refer to answerRingingCall();
     */
    public void answerRingingCallGemini(int simId) throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            sTelephony.answerRingingCallGemini(simId);
        } else {
        }
    }

    /**
     * refer to silenceRinger();
     */
    public void silenceRingerGemini(int simId) throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            sTelephony.silenceRingerGemini(simId);
        } else {
        }
    }

    /**
     * refer to isOffhook().
     */
    public boolean isOffhookGemini(int simId) throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return sTelephony.isOffhookGemini(simId);
        } else {
            return false;
        }
    }

    /**
     * refer to isRinging().
     */
    public boolean isRingingGemini(int simId) throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return sTelephony.isRingingGemini(simId);
        } else {
            return false;
        }
    }

    /**
     * refer to isIdle().
     */
    public boolean isIdleGemini(int simId) throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return sTelephony.isIdleGemini(simId);
        } else {
            return false;
        }
    }

    public int getPendingMmiCodesGemini(int simId) throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return sTelephony.getPendingMmiCodesGemini(simId);
        } else {
            return 0;
        }
    }

    /**
     * refer to cancelMissedCallsNotification();
     */
    public void cancelMissedCallsNotificationGemini(int simId) throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            sTelephony.cancelMissedCallsNotificationGemini(simId);
        } else {
        }
    }

    /**
     * refer to getCallState();
     */
     public int getCallStateGemini(int simId) throws android.os.RemoteException {
         if (EncapsulationConstant.USE_MTK_PLATFORM) {
             return sTelephony.getCallStateGemini(simId);
         } else {
             return 0;
         }
     }

    /**
     * refer to getActivePhoneType();
     */
     public int getActivePhoneTypeGemini(int simId) throws android.os.RemoteException {
         if (EncapsulationConstant.USE_MTK_PLATFORM) {
             return sTelephony.getActivePhoneTypeGemini(simId);
         } else {
             return 0;
         }
     }

    /**
     * Check to see if the radio is on or not.
     * @return returns true if the radio is on.
     */
    public boolean isRadioOnGemini(int simId) throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return sTelephony.isRadioOnGemini(simId);
        } else {
            MmsLog.d("Encapsulation issue", "EncapsulatedTelephonyService -- isRadioOnGemini(int)");
            return false;
        }
    }

    /**
     * Supply a pin to unlock the SIM.  Blocks until a result is determined.
     * @param pin The pin to check.
     * @return whether the operation was a success.
     */
    public boolean supplyPinGemini(String pin, int simId) throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return sTelephony.supplyPinGemini(pin, simId);
        } else {
            return false;
        }
    }

    /**
     * Supply a PUK code to unblock the SIM pin lock.  Blocks until a result is determined.
     * @param puk The PUK code
     * @param pin The pin to check.
     * @return whether the operation was a success.
     */
    public boolean supplyPukGemini(String puk, String pin, int simId) throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return sTelephony.supplyPukGemini(puk, pin, simId);
        } else {
            return false;
        }
    }

    /**
     * Handles PIN MMI commands (PIN/PIN2/PUK/PUK2), which are initiated
     * without SEND (so <code>dial</code> is not appropriate).
     *
     * @param dialString the MMI command to be executed.
     * @return true if MMI command is executed.
     */
    public boolean handlePinMmiGemini(String dialString, int simId) throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return sTelephony.handlePinMmiGemini(dialString, simId);
        } else {
            return false;
        }
    }

    /**
     * Returns the IccCard type of Gemini phone. Return "SIM" for SIM card or "USIM" for USIM card.
     */
    public String getIccCardTypeGemini(int simId) throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return sTelephony.getIccCardTypeGemini(simId);
        } else {
            MmsLog.d("Encapsulation issue", "EncapsulatedTelephonyService -- getIccCardTypeGemini(int)");
            return null;
        }
    }

    /**
     * Do sim authentication for gemini and return the raw data of result.
     * Returns the hex format string of auth result.
     * <p> random string in hex format
     * <p> int for simid
     */
    public String simAuthGemini(String strRand, int simId) throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return sTelephony.simAuthGemini(strRand, simId);
        } else {
            return null;
        }
    }

    /**
     * Do usim authentication for gemini and return the raw data of result.
     * Returns the hex format string of auth result.
     * <p> random string in hex format
     * <p> int for simid
     */
    public String uSimAuthGemini(String strRand, String strAutn, int simId) throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return sTelephony.uSimAuthGemini(strRand, strAutn, simId);
        } else {
            return null;
        }
    }

    /**
     * Request to update location information in service state
     */
    public void updateServiceLocationGemini(int simId) throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            sTelephony.updateServiceLocationGemini(simId);
        } else {
        }
    }

    /**
     * Enable location update notifications.
     */
    public void enableLocationUpdatesGemini(int simId) throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            sTelephony.enableLocationUpdatesGemini(simId);
        } else {
        }
    }

    /**
     * Disable location update notifications.
     */
    public void disableLocationUpdatesGemini(int simId) throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            sTelephony.disableLocationUpdatesGemini(simId);
        } else {
        }
    }

    public Bundle getCellLocationGemini(int simId) throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return sTelephony.getCellLocationGemini(simId);
        } else {
            return null;
        }
    }

    /**
     * Returns the neighboring cell information of the device.
     */
    public List<NeighboringCellInfo> getNeighboringCellInfoGemini(int simId) throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return sTelephony.getNeighboringCellInfoGemini(simId);
        } else {
            return null;
        }
    }

    /**
    * Returns true if SIM card inserted
     * This API is valid even if airplane mode is on
    */
    public boolean isSimInsert(int simId) throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return sTelephony.isSimInsert(simId);
        } else {
            MmsLog.d("Encapsulation issue", "EncapsulatedTelephonyService -- isSimInsert(int)");
            return false;
        }
    }

    /**
    * Set GPRS connection type, ALWAYS/WHEN_NEEDED
    */
    public void setGprsConnType(int type, int simId) throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            sTelephony.setGprsConnType(type, simId);
        } else {
        }
    }

    /**
    * Set GPRS transfer type, Call prefer/Data prefer
    */
    public void setGprsTransferType(int type) throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            sTelephony.setGprsTransferType(type);
        } else {
        }
    }

    public void setGprsTransferTypeGemini(int type, int simId) throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            sTelephony.setGprsTransferTypeGemini(type, simId);
        } else {
        }
    }

    /*Add by mtk80372 for Barcode number*/
    public void getMobileRevisionAndIMEI(int type, Message message) throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            sTelephony.getMobileRevisionAndIMEI(type, message);
        } else {
        }
    }

    /*Add by mtk80372 for Barcode number*/
    public String getSN() throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return sTelephony.getSN();
        } else {
            return null;
        }
    }

    /**
    * Set default phone
    */
    public void setDefaultPhone(int simId) throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            sTelephony.setDefaultPhone(simId);
        } else {
        }
    }

    /**
      * Returns the network type
      */
    public int getNetworkTypeGemini(int simId) throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return sTelephony.getNetworkTypeGemini(simId);
        } else {
            return 0;
        }
    }

    public boolean hasIccCardGemini(int simId) throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return sTelephony.hasIccCardGemini(simId);
        } else {
            return false;
        }
    }

    public boolean isTestIccCardGemini(int simId) throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return sTelephony.isTestIccCardGemini(simId);
        } else {
            return false;
        }
    }

    public int enableDataConnectivityGemini(int simId) throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return sTelephony.enableDataConnectivityGemini(simId);
        } else {
            return 0;
        }
    }

    public int enableApnTypeGemini(String type, int simId) throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return sTelephony.enableApnTypeGemini(type, simId);
        } else {
            return 0;
        }
    }

    public int disableApnTypeGemini(String type, int simId) throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return sTelephony.disableApnTypeGemini(type, simId);
        } else {
            return 0;
        }
    }

    public int disableDataConnectivityGemini(int simId) throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return sTelephony.disableDataConnectivityGemini(simId);
        } else {
            return 0;
        }
    }

    public boolean isDataConnectivityPossibleGemini(int simId) throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return sTelephony.isDataConnectivityPossibleGemini(simId);
        } else {
            return false;
        }
    }

    public int getDataStateGemini(int simId) throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return sTelephony.getDataStateGemini(simId);
        } else {
            return 0;
        }
    }

    public int getDataActivityGemini(int simId) throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return sTelephony.getDataActivityGemini(simId);
        } else {
            return 0;
        }
    }

    public int getVoiceMessageCountGemini(int simId) throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return sTelephony.getVoiceMessageCountGemini(simId);
        } else {
            return 0;
        }
    }

    /**
    * Return true if the FDN of the ICC card is enabled
    */
    public boolean isFDNEnabledGemini(int simId) throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return sTelephony.isFDNEnabledGemini(simId);
        } else {
            return false;
        }
    }

    public boolean isVTIdle() throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return sTelephony.isVTIdle();
        } else {
            return false;
        }
    }

   /**
     *send BT SIM profile of Connect SIM
     * @param simId specify which SIM to connect
     * @param btRsp fetch the response data.
     * @return success or error code.
   */
    public int btSimapConnectSIM(int simId, BtSimapOperResponse btRsp) throws android.os.RemoteException {
       if (EncapsulationConstant.USE_MTK_PLATFORM) {
           return sTelephony.btSimapConnectSIM(simId, btRsp);
       } else {
           return 0;
       }
   }

    /**
     *send BT SIM profile of Disconnect SIM
     * @param null
     * @return success or error code.
   */
   public int btSimapDisconnectSIM() throws android.os.RemoteException {
       if (EncapsulationConstant.USE_MTK_PLATFORM) {
           return sTelephony.btSimapDisconnectSIM();
       } else {
           return 0;
       }
   }

   /**
     *Transfer APDU data through BT SAP
     * @param type Indicate which transport protocol is the preferred one
     * @param cmdAPDU APDU data to transfer in hex character format
     * @param btRsp fetch the response data.
     * @return success or error code.
   */
   public int btSimapApduRequest(int type, String cmdAPDU, BtSimapOperResponse btRsp) throws android.os.RemoteException {
       if (EncapsulationConstant.USE_MTK_PLATFORM) {
           return sTelephony.btSimapApduRequest(type, cmdAPDU, btRsp);
       } else {
           return 0;
       }
   }

    /**
     *send BT SIM profile of Reset SIM
     * @param type Indicate which transport protocol is the preferred one
     * @param btRsp fetch the response data.
     * @return success or error code.
   */
   public int btSimapResetSIM(int type, BtSimapOperResponse btRsp) throws android.os.RemoteException {
       if (EncapsulationConstant.USE_MTK_PLATFORM) {
           return sTelephony.btSimapResetSIM(type, btRsp);
       } else {
           return 0;
       }
   }

   /**
     *send BT SIM profile of Power On SIM
     * @param type Indicate which transport protocol is the preferred onet
     * @param btRsp fetch the response data.
     * @return success or error code.
   */
   public int btSimapPowerOnSIM(int type, BtSimapOperResponse btRsp) throws android.os.RemoteException {
       if (EncapsulationConstant.USE_MTK_PLATFORM) {
           return sTelephony.btSimapPowerOnSIM(type, btRsp);
       } else {
           return 0;
       }
   }

   /**
     *send BT SIM profile of PowerOff SIM
     * @return success or error code.
   */
   public int btSimapPowerOffSIM() throws android.os.RemoteException {
       if (EncapsulationConstant.USE_MTK_PLATFORM) {
           return sTelephony.btSimapPowerOffSIM();
       } else {
           return 0;
       }
   }

   /**
     *get the services state for default SIM
     * @return sim indicator state.
     *
    */
   public int getSimIndicatorState() throws android.os.RemoteException {
       if (EncapsulationConstant.USE_MTK_PLATFORM) {
           return sTelephony.getSimIndicatorState();
       } else {
           return 0;
       }
   }

   /**
     *get the services state for specified SIM
     * @param simId Indicate which sim(slot) to query
     * @return sim indicator state.
     *
    */
   public int getSimIndicatorStateGemini(int simId) throws android.os.RemoteException {
       if (EncapsulationConstant.USE_MTK_PLATFORM) {
           return sTelephony.getSimIndicatorStateGemini(simId);
       } else {
           return 0;
       }
   }

   /**
     *get the network service state for default SIM
     * @return service state.
     *
    */
   public Bundle getServiceState() throws android.os.RemoteException {
       if (EncapsulationConstant.USE_MTK_PLATFORM) {
           return sTelephony.getServiceState();
       } else {
           return null;
       }
   }

   /**
     * get the network service state for specified SIM
     * @param simId Indicate which sim(slot) to query
     * @return service state.
     *
    */
   public Bundle getServiceStateGemini(int simId) throws android.os.RemoteException {
       if (EncapsulationConstant.USE_MTK_PLATFORM) {
           return sTelephony.getServiceStateGemini(simId);
       } else {
           return null;
       }
   }

   public String getScAddressGemini(int simId) throws android.os.RemoteException {
       if (EncapsulationConstant.USE_MTK_PLATFORM) {
           return sTelephony.getScAddressGemini(simId);
       } else {
           return null;
       }
   }

   public void setScAddressGemini(String scAddr, int simId) throws android.os.RemoteException {
       if (EncapsulationConstant.USE_MTK_PLATFORM) {
           sTelephony.setScAddressGemini(scAddr, simId);
       } else {
       }
   }

   /**
    * @return SMS default SIM.
    */
   public int getSmsDefaultSim() throws android.os.RemoteException {
       if (EncapsulationConstant.USE_MTK_PLATFORM) {
           return sTelephony.getSmsDefaultSim();
       } else {
           return 0;
       }
   }

   public int get3GCapabilitySIM() throws android.os.RemoteException {
       if (EncapsulationConstant.USE_MTK_PLATFORM) {
           return sTelephony.get3GCapabilitySIM();
       } else {
           MmsLog.d("Encapsulation issue", "EncapsulatedTelephonyService -- get3GCapabilitySIM()");
           return 0;
       }
   }

   public boolean set3GCapabilitySIM(int simId) throws android.os.RemoteException {
       if (EncapsulationConstant.USE_MTK_PLATFORM) {
           return sTelephony.set3GCapabilitySIM(simId);
       } else {
           return false;
       }
   }

   public int aquire3GSwitchLock() throws android.os.RemoteException {
       if (EncapsulationConstant.USE_MTK_PLATFORM) {
           return sTelephony.aquire3GSwitchLock();
       } else {
           return 0;
       }
   }

   public boolean release3GSwitchLock(int lockId) throws android.os.RemoteException {
       if (EncapsulationConstant.USE_MTK_PLATFORM) {
           return sTelephony.release3GSwitchLock(lockId);
       } else {
           return false;
       }
   }

   public boolean is3GSwitchLocked() throws android.os.RemoteException {
       if (EncapsulationConstant.USE_MTK_PLATFORM) {
           return sTelephony.is3GSwitchLocked();
       } else {
           return false;
       }
   }

   public int cleanupApnTypeGemini(String apnType, int simId) throws android.os.RemoteException {
       if (EncapsulationConstant.USE_MTK_PLATFORM) {
           return sTelephony.cleanupApnTypeGemini(apnType, simId);
       } else {
           return 0;
       }
   }

   //MTK-END [mtk04070][111117][ALPS00093395]MTK proprietary methods
   //MTK-START [mtk03851][111117]MTK proprietary methods
   public void registerForSimModeChange(IBinder binder, int what) throws android.os.RemoteException {
       if (EncapsulationConstant.USE_MTK_PLATFORM) {
           sTelephony.registerForSimModeChange(binder, what);
       } else {
       }
   }

   public void unregisterForSimModeChange(IBinder binder) throws android.os.RemoteException {
       if (EncapsulationConstant.USE_MTK_PLATFORM) {
           sTelephony.unregisterForSimModeChange(binder);
       } else {
       }
   }

   public void setDataRoamingEnabledGemini(boolean enable, int simId) throws android.os.RemoteException {
       if (EncapsulationConstant.USE_MTK_PLATFORM) {
           sTelephony.setDataRoamingEnabledGemini(enable, simId);
       } else {
       }
   }

   public void setRoamingIndicatorNeddedProperty(boolean property1, boolean property2) throws android.os.RemoteException {
       if (EncapsulationConstant.USE_MTK_PLATFORM) {
           sTelephony.setRoamingIndicatorNeddedProperty(property1, property2);
       } else {
       }
   }

   /**
     * Get the count of missed call.
     *
     * @return Return the count of missed call.
     */
    public int getMissedCallCount() throws android.os.RemoteException {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return sTelephony.getMissedCallCount();
        } else {
            return 0;
        }
    }

   /**
      Description : Adjust modem radio power for Lenovo SAR requirement.
      AT command format: AT+ERFTX=<op>,<para1>,<para2>
      Description : When <op>=1  -->  TX power reduction
                    <para1>:  2G L1 reduction level, default is 0
                    <para2>:  3G L1 reduction level, default is 0
                    level scope : 0 ~ 64
      Arthur      : mtk04070
      Date        : 2012.01.09
      Return value: True for success, false for failure
    */
   public boolean adjustModemRadioPower(int level_2G, int level_3G) throws android.os.RemoteException {
       if (EncapsulationConstant.USE_MTK_PLATFORM) {
           return sTelephony.adjustModemRadioPower(level_2G, level_3G);
       } else {
           return false;
       }
   }

   /**
      Description      : Adjust modem radio power by band for Lenovo SAR requirement.
      AT command format: AT+ERFTX=<op>,<rat>,<band>,<para1>...<paraX>
      Description : <op>=3   -->  TX power reduction by given band
                    <rat>    -->  1 for 2G, 2 for 3G
                    <band>   -->  2G or 3G band value
                    <para1>~<paraX> -->  Reduction level
                    level scope : 0 ~ 255
      Arthur      : mtk04070
      Date        : 2012.05.31
      Return value: True for success, false for failure
   */
   public boolean adjustModemRadioPowerByBand(int rat, int band, int level) throws android.os.RemoteException {
       if (EncapsulationConstant.USE_MTK_PLATFORM) {
           return sTelephony.adjustModemRadioPowerByBand(rat, band, level);
       } else {
           return false;
       }
   }

   //MTK-END [mtk03851][111117]MTK proprietary methods

   // MVNO-API START
   public String getSpNameInEfSpn() throws android.os.RemoteException {
       if (EncapsulationConstant.USE_MTK_PLATFORM) {
           return sTelephony.getSpNameInEfSpn();
       } else {
           return null;
       }
   }

   public String getSpNameInEfSpnGemini(int simId) throws android.os.RemoteException {
       if (EncapsulationConstant.USE_MTK_PLATFORM) {
           return sTelephony.getSpNameInEfSpnGemini(simId);
       } else {
           return null;
       }
   }

   public String isOperatorMvnoForImsi() throws android.os.RemoteException {
       if (EncapsulationConstant.USE_MTK_PLATFORM) {
           return sTelephony.isOperatorMvnoForImsi();
       } else {
           return null;
       }
   }

   public String isOperatorMvnoForImsiGemini(int simId) throws android.os.RemoteException {
       if (EncapsulationConstant.USE_MTK_PLATFORM) {
           return sTelephony.isOperatorMvnoForImsiGemini(simId);
       } else {
           return null;
       }
   }

   public boolean isIccCardProviderAsMvno() throws android.os.RemoteException {
       if (EncapsulationConstant.USE_MTK_PLATFORM) {
           return sTelephony.isIccCardProviderAsMvno();
       } else {
           return false;
       }
   }

   public boolean isIccCardProviderAsMvnoGemini(int simId) throws android.os.RemoteException {
       if (EncapsulationConstant.USE_MTK_PLATFORM) {
           return sTelephony.isIccCardProviderAsMvnoGemini(simId);
       } else {
           return false;
       }
   }
   // MVNO-API END
   // @}

    /** M: google code */

    /**
     * Dial a number. This doesn't place the call. It displays
     * the Dialer screen.
     * @param number the number to be dialed. If null, this
     * would display the Dialer screen with no number pre-filled.
     */
    public void dial(String number) throws android.os.RemoteException {
        sTelephony.dial(number);
    }

    /**
     * Place a call to the specified number.
     * @param number the number to be called.
     */
    public void call(String number) throws android.os.RemoteException {
        sTelephony.call(number);
    }

    /**
     * If there is currently a call in progress, show the call screen.
     * The DTMF dialpad may or may not be visible initially, depending on
     * whether it was up when the user last exited the InCallScreen.
     *
     * @return true if the call screen was shown.
     */
    public boolean showCallScreen() throws android.os.RemoteException {
        return sTelephony.showCallScreen();
    }

    /**
     * Variation of showCallScreen() that also specifies whether the
     * DTMF dialpad should be initially visible when the InCallScreen
     * comes up.
     *
     * @param showDialpad if true, make the dialpad visible initially,
     *                    otherwise hide the dialpad initially.
     * @return true if the call screen was shown.
     *
     * @see showCallScreen
     */
    public boolean showCallScreenWithDialpad(boolean showDialpad) throws android.os.RemoteException {
        return sTelephony.showCallScreenWithDialpad(showDialpad);
    }

    /**
     * End call if there is a call in progress, otherwise does nothing.
     *
     * @return whether it hung up
     */
    public boolean endCall() throws android.os.RemoteException {
        return sTelephony.endCall();
    }

    /**
     * Answer the currently-ringing call.
     *
     * If there's already a current active call, that call will be
     * automatically put on hold.  If both lines are currently in use, the
     * current active call will be ended.
     *
     * TODO: provide a flag to let the caller specify what policy to use
     * if both lines are in use.  (The current behavior is hardwired to
     * "answer incoming, end ongoing", which is how the CALL button
     * is specced to behave.)
     *
     * TODO: this should be a oneway call (especially since it's called
     * directly from the key queue thread).
     */
    public void answerRingingCall() throws android.os.RemoteException {
        sTelephony.answerRingingCall();
    }

    /**
     * Silence the ringer if an incoming call is currently ringing.
     * (If vibrating, stop the vibrator also.)
     *
     * It's safe to call this if the ringer has already been silenced, or
     * even if there's no incoming call.  (If so, this method will do nothing.)
     *
     * TODO: this should be a oneway call too (see above).
     *       (Actually *all* the methods here that return void can
     *       probably be oneway.)
     */
    public void silenceRinger() throws android.os.RemoteException {
        sTelephony.silenceRinger();
    }

    /**
     * Check if we are in either an active or holding call
     * @return true if the phone state is OFFHOOK.
     */
    public boolean isOffhook() throws android.os.RemoteException {
        return sTelephony.isOffhook();
    }

    /**
     * Check if an incoming phone call is ringing or call waiting.
     * @return true if the phone state is RINGING.
     */
    public boolean isRinging() throws android.os.RemoteException {
        return sTelephony.isRinging();
    }

    /**
     * Check if the phone is idle.
     * @return true if the phone state is IDLE.
     */
    public boolean isIdle() throws android.os.RemoteException {
        return sTelephony.isIdle();
    }

    /**
     * Check to see if the radio is on or not.
     * @return returns true if the radio is on.
     */
    public boolean isRadioOn() throws android.os.RemoteException {
        return sTelephony.isRadioOn();
    }

    /**
     * Check if the SIM pin lock is enabled.
     * @return true if the SIM pin lock is enabled.
     */
    public boolean isSimPinEnabled() throws android.os.RemoteException {
        return sTelephony.isSimPinEnabled();
    }

    /**
     * Cancels the missed calls notification.
     */
    public void cancelMissedCallsNotification() throws android.os.RemoteException {
        sTelephony.cancelMissedCallsNotification();
    }

    /**
     * Supply a pin to unlock the SIM.  Blocks until a result is determined.
     * @param pin The pin to check.
     * @return whether the operation was a success.
     */
    public boolean supplyPin(String pin) throws android.os.RemoteException {
        return sTelephony.supplyPin(pin);
    }

    /**
     * Supply puk to unlock the SIM and set SIM pin to new pin.
     *  Blocks until a result is determined.
     * @param puk The puk to check.
     *        pin The new pin to be set in SIM
     * @return whether the operation was a success.
     */
    public boolean supplyPuk(String puk, String pin) throws android.os.RemoteException {
        return sTelephony.supplyPuk(puk, pin);
    }

    /**
     * Handles PIN MMI commands (PIN/PIN2/PUK/PUK2), which are initiated
     * without SEND (so <code>dial</code> is not appropriate).
     *
     * @param dialString the MMI command to be executed.
     * @return true if MMI command is executed.
     */
    public boolean handlePinMmi(String dialString) throws android.os.RemoteException {
        return sTelephony.handlePinMmi(dialString);
    }

    /**
     * Toggles the radio on or off.
     */
    public void toggleRadioOnOff() throws android.os.RemoteException {
        sTelephony.toggleRadioOnOff();
    }

    /**
     * Set the radio to on or off
     */
    public boolean setRadio(boolean turnOn) throws android.os.RemoteException {
        return sTelephony.setRadio(turnOn);
    }

    /**
     * Request to update location information in service state
     */
    public void updateServiceLocation() throws android.os.RemoteException {
        sTelephony.updateServiceLocation();
    }

    /**
     * Enable location update notifications.
     */
    public void enableLocationUpdates() throws android.os.RemoteException {
        sTelephony.enableLocationUpdates();
    }

    /**
     * Disable location update notifications.
     */
    public void disableLocationUpdates() throws android.os.RemoteException {
        sTelephony.disableLocationUpdates();
    }

    /**
     * Enable a specific APN type.
     */
    public int enableApnType(String type) throws android.os.RemoteException {
        return sTelephony.enableApnType(type);
    }

    /**
     * Disable a specific APN type.
     */
    public int disableApnType(String type) throws android.os.RemoteException {
        return sTelephony.disableApnType(type);
    }

    /**
     * Allow mobile data connections.
     */
    public boolean enableDataConnectivity() throws android.os.RemoteException {
        return sTelephony.enableDataConnectivity();
    }

    /**
     * Disallow mobile data connections.
     */
    public boolean disableDataConnectivity() throws android.os.RemoteException {
        return sTelephony.disableDataConnectivity();
    }

    /**
     * Report whether data connectivity is possible.
     */
    public boolean isDataConnectivityPossible() throws android.os.RemoteException {
        return sTelephony.isDataConnectivityPossible();
    }

    public Bundle getCellLocation() throws android.os.RemoteException {
        return sTelephony.getCellLocation();
    }

    /**
     * Returns the neighboring cell information of the device.
     */
    public List<NeighboringCellInfo> getNeighboringCellInfo() throws android.os.RemoteException {
        return sTelephony.getNeighboringCellInfo();
    }

    public int getCallState() throws android.os.RemoteException {
        return sTelephony.getCallState();
    }

    public int getDataActivity() throws android.os.RemoteException {
        return sTelephony.getDataActivity();
    }

    public int getDataState() throws android.os.RemoteException {
        return sTelephony.getDataState();
    }

    /**
     * Returns the current active phone type as integer.
     * Returns TelephonyManager.PHONE_TYPE_CDMA if RILConstants.CDMA_PHONE
     * and TelephonyManager.PHONE_TYPE_GSM if RILConstants.GSM_PHONE
     */
    public int getActivePhoneType() throws android.os.RemoteException {
        return sTelephony.getActivePhoneType();
    }

    /**
     * Returns the CDMA ERI icon index to display
     */
    public int getCdmaEriIconIndex() throws android.os.RemoteException {
        return sTelephony.getCdmaEriIconIndex();
    }

    /**
     * Returns the CDMA ERI icon mode,
     * 0 - ON
     * 1 - FLASHING
     */
    public int getCdmaEriIconMode() throws android.os.RemoteException {
        return sTelephony.getCdmaEriIconMode();
    }

    /**
     * Returns the CDMA ERI text,
     */
    public String getCdmaEriText() throws android.os.RemoteException {
        return sTelephony.getCdmaEriText();
    }

    /**
     * Returns true if OTA service provisioning needs to run.
     * Only relevant on some technologies, others will always
     * return false.
     */
    public boolean needsOtaServiceProvisioning() throws android.os.RemoteException {
        return sTelephony.needsOtaServiceProvisioning();
    }

    /**
      * Returns the unread count of voicemails
      */
    public int getVoiceMessageCount() throws android.os.RemoteException {
        return sTelephony.getVoiceMessageCount();
    }

    /**
      * Returns the network type
      */
    public int getNetworkType() throws android.os.RemoteException {
        return sTelephony.getNetworkType();
    }

    /**
     * Return true if an ICC card is present
     * This API always return false if airplane mode is on.
     */
    public boolean hasIccCard() throws android.os.RemoteException {
        return sTelephony.hasIccCard();
    }

    /**
     * Return if the current radio is LTE on CDMA. This
     * is a tri-state return value as for a period of time
     * the mode may be unknown.
     *
     * @return {@link Phone#LTE_ON_CDMA_UNKNOWN}, {@link Phone#LTE_ON_CDMA_FALSE}
     * or {@link PHone#LTE_ON_CDMA_TRUE}
     */
    public int getLteOnCdmaMode() throws android.os.RemoteException {
        return sTelephony.getLteOnCdmaMode();
    }

    /**
     * Returns the all observed cell information of the device.
     */
    public List<CellInfo> getAllCellInfo() throws android.os.RemoteException {
        return sTelephony.getAllCellInfo();
    }
}
