/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

/*
 * Copyright (C) 2007 The Android Open Source Project
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

import android.os.Bundle;
import java.util.List;
import android.telephony.NeighboringCellInfo;
import android.telephony.CellInfo;

//MTK-START [mtk04070][111117][ALPS00093395]MTK added
import android.os.Message;
import android.telephony.BtSimapOperResponse;
//MTK-END [mtk04070][111117][ALPS00093395]MTK added

import com.android.internal.telephony.IPhoneSubInfo;
//MTK-END [mtk02772]sdk api refactoring end

/**
 * Interface used to interact with the phone.  Mostly this is used by the
 * TelephonyManager class.  A few places are still using this directly.
 * Please clean them up if possible and use TelephonyManager insteadl.
 *
 * {@hide}
 */
interface ITelephony {

    /**
     * Dial a number. This doesn't place the call. It displays
     * the Dialer screen.
     * @param number the number to be dialed. If null, this
     * would display the Dialer screen with no number pre-filled.
     */
    void dial(String number);

    /**
     * Place a call to the specified number.
     * @param number the number to be called.
     */
    void call(String number);

    /**
     * If there is currently a call in progress, show the call screen.
     * The DTMF dialpad may or may not be visible initially, depending on
     * whether it was up when the user last exited the InCallScreen.
     *
     * @return true if the call screen was shown.
     */
    boolean showCallScreen();

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
    boolean showCallScreenWithDialpad(boolean showDialpad);

    /**
     * End call if there is a call in progress, otherwise does nothing.
     *
     * @return whether it hung up
     */
    boolean endCall();

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
    void answerRingingCall();

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
    void silenceRinger();

    /**
     * Check if we are in either an active or holding call
     * @return true if the phone state is OFFHOOK.
     */
    boolean isOffhook();

    /**
     * Check if an incoming phone call is ringing or call waiting.
     * @return true if the phone state is RINGING.
     */
    boolean isRinging();

    /**
     * Check if the phone is idle.
     * @return true if the phone state is IDLE.
     */
    boolean isIdle();

    /**
     * Check to see if the radio is on or not.
     * @return returns true if the radio is on.
     */
    boolean isRadioOn();

    /**
     * Check if the SIM pin lock is enabled.
     * @return true if the SIM pin lock is enabled.
     */
    boolean isSimPinEnabled();

    /**
     * Cancels the missed calls notification.
     */
    void cancelMissedCallsNotification();

    /**
     * Supply a pin to unlock the SIM.  Blocks until a result is determined.
     * @param pin The pin to check.
     * @return whether the operation was a success.
     */
    boolean supplyPin(String pin);

    /**
     * Supply puk to unlock the SIM and set SIM pin to new pin.
     *  Blocks until a result is determined.
     * @param puk The puk to check.
     *        pin The new pin to be set in SIM
     * @return whether the operation was a success.
     */
    boolean supplyPuk(String puk, String pin);

    /**
     * Handles PIN MMI commands (PIN/PIN2/PUK/PUK2), which are initiated
     * without SEND (so <code>dial</code> is not appropriate).
     *
     * @param dialString the MMI command to be executed.
     * @return true if MMI command is executed.
     */
    boolean handlePinMmi(String dialString);

    /**
     * Toggles the radio on or off.
     */
    void toggleRadioOnOff();

    /**
     * Set the radio to on or off
     */
    boolean setRadio(boolean turnOn);

    /**
     * Request to update location information in service state
     */
    void updateServiceLocation();

    /**
     * Enable location update notifications.
     */
    void enableLocationUpdates();

    /**
     * Disable location update notifications.
     */
    void disableLocationUpdates();

    /**
     * Enable a specific APN type.
     */
    int enableApnType(String type);

    /**
     * Disable a specific APN type.
     */
    int disableApnType(String type);

    /**
     * Allow mobile data connections.
     */
    boolean enableDataConnectivity();

    /**
     * Disallow mobile data connections.
     */
    boolean disableDataConnectivity();

    /**
     * Report whether data connectivity is possible.
     */
    boolean isDataConnectivityPossible();

    Bundle getCellLocation();

    /**
     * Returns the neighboring cell information of the device.
     */
    List<NeighboringCellInfo> getNeighboringCellInfo();

     int getCallState();
     int getDataActivity();
     int getDataState();

    /**
     * Returns the current active phone type as integer.
     * Returns TelephonyManager.PHONE_TYPE_CDMA if RILConstants.CDMA_PHONE
     * and TelephonyManager.PHONE_TYPE_GSM if RILConstants.GSM_PHONE
     */
    int getActivePhoneType();

    /**
     * Returns the CDMA ERI icon index to display
     */
    int getCdmaEriIconIndex();

    /**
     * Returns the CDMA ERI icon mode,
     * 0 - ON
     * 1 - FLASHING
     */
    int getCdmaEriIconMode();

    /**
     * Returns the CDMA ERI text,
     */
    String getCdmaEriText();

    /**
     * Returns true if OTA service provisioning needs to run.
     * Only relevant on some technologies, others will always
     * return false.
     */
    boolean needsOtaServiceProvisioning();

    /**
      * Returns the unread count of voicemails
      */
    int getVoiceMessageCount();

    /**
      * Returns the network type
      */
    int getNetworkType();

    /**
     * Return true if an ICC card is present
     * This API always return false if airplane mode is on.
     */
    boolean hasIccCard();

    /**
     * Return if the current radio is LTE on CDMA. This
     * is a tri-state return value as for a period of time
     * the mode may be unknown.
     *
     * @return {@link Phone#LTE_ON_CDMA_UNKNOWN}, {@link Phone#LTE_ON_CDMA_FALSE}
     * or {@link PHone#LTE_ON_CDMA_TRUE}
     */
    int getLteOnCdmaMode();

    // NFC SEEK start
    /**
     * Returns the response APDU for a command APDU sent to a logical channel
     */
    String transmitIccLogicalChannel(int cla, int command, int channel,
            int p1, int p2, int p3, String data);

    /**
     * Returns the response APDU for a command APDU sent to a logical channel for Gemini-Card
     */
    String transmitIccLogicalChannelGemini(int cla, int command, int channel,
            int p1, int p2, int p3, String data, int simId);

    /**
     * Returns the response APDU for a command APDU sent to the basic channel
     */
    String transmitIccBasicChannel(int cla, int command,
            int p1, int p2, int p3, String data);

    /**
     * Returns the response APDU for a command APDU sent to the basic channel for Gemini-Card
     */
    String transmitIccBasicChannelGemini(int cla, int command,
            int p1, int p2, int p3, String data, int simId);

    /**
     * Returns the channel id of the logical channel,
     * Returns 0 on error.
     */
    int openIccLogicalChannel(String AID);

    /**
     * Returns the channel id of the logical channel for Gemini-Card,
     * Returns 0 on error.
     */
    int openIccLogicalChannelGemini(String AID, int simId);

    /**
     * Return true if logical channel was closed successfully
     */
    boolean closeIccLogicalChannel(int channel);

    /**
     * Return true if logical channel was closed successfully for Gemini-Card
     */
    boolean closeIccLogicalChannelGemini(int channel, int simId);

    /**
     * Returns the error code of the last error occured.
     * Currently only used for openIccLogicalChannel
     */
    int getLastError();

    /**
     * Returns the error code of the last error occured for Gemini-Card.
     * Currently only used for openIccLogicalChannel
     */
    int getLastErrorGemini(int simId);
    
    /**
     * Returns the response APDU for a command APDU sent through SIM_IO
     */
    byte[] transmitIccSimIO(int fileID, int command,
                                      int p1, int p2, int p3, String filePath);

    /**
     * Returns the response APDU for a command APDU sent through SIM_IO for Gemini-Card
     */
    byte[] transmitIccSimIOGemini(int fileID, int command,
                                      int p1, int p2, int p3, String filePath, int simId);

    /**
     * Returns SIM's ATR in hex format
     */
    String getIccATR();

    /**
     * Returns SIM's ATR in hex format for Gemini-Card
     */
    String getIccATRGemini(int simId);


    /**
     * Returns the channel id of the logical channel, sw1, and sw2,
     * Returns 0 on error.
     */
    byte[] openIccLogicalChannelWithSw(String AID);

    /**
     * Returns the channel id of the logical channel, sw1, and sw2 for Gemini-Card,
     * Returns 0 on error.
     */
    byte[] openIccLogicalChannelWithSwGemini(String AID, int simId);

    // NFC SEEK end

    /**
     * Returns the all observed cell information of the device.
     */
    List<CellInfo> getAllCellInfo();

    //MTK-START [mtk04070][111117][ALPS00093395]MTK proprietary methods
    /**
     * Check if the phone is idle for voice call only.
     * @return true if the phone state is for voice call only.
     */
    boolean isVoiceIdle();

    /**
     * Returns the IccCard type. Return "SIM" for SIM card or "USIM" for USIM card.
     */
    String getIccCardType(); 
	
    /**
     * Do sim authentication and return the raw data of result.
     * Returns the hex format string of auth result.
     * <p> random string in hex format
     */
    String simAuth(String strRand); 

    /**
     * Do usim authentication and return the raw data of result.
     * Returns the hex format string of auth result.
     * <p> random string in hex format
     */
    String uSimAuth(String strRand, String strAutn);

    /**
     * Shutdown Radio
     */
    boolean setRadioOff();

    int getPreciseCallState();
    
    /**
     * Return ture if the ICC card is a test card
     */
    boolean isTestIccCard();

    /**
    * Return true if the FDN of the ICC card is enabled
    */
    boolean isFDNEnabled();

    /**
     * refer to dial(String number);
     */
    void dialGemini(String number, int simId);
    
    /**
     * refer to call(String number);
     */
    void callGemini(String number, int simId);

    /**
     * refer to showCallScreen();
     */
    boolean showCallScreenGemini(int simId);

    /**
     * refer to showCallScreenWithDialpad(boolean showDialpad)
     */
    boolean showCallScreenWithDialpadGemini(boolean showDialpad, int simId);

    /**
     * refer to endCall().
     */
    boolean endCallGemini(int simId);

    /**
     * refer to answerRingingCall();
     */
    void answerRingingCallGemini(int simId);

    /**
     * refer to silenceRinger();
     */
    void silenceRingerGemini(int simId);

    /**
     * refer to isOffhook().
     */
    boolean isOffhookGemini(int simId);

    /**
     * refer to isRinging().
     */
    boolean isRingingGemini(int simId);

    /**
     * refer to isIdle().
     */
    boolean isIdleGemini(int simId);    
    
    int getPendingMmiCodesGemini(int simId);
    
    /**
     * refer to cancelMissedCallsNotification();
     */
    void cancelMissedCallsNotificationGemini(int simId);

    /**
     * refer to getCallState();
     */
     int getCallStateGemini(int simId);
     
    /**
     * refer to getActivePhoneType();
     */
     int getActivePhoneTypeGemini(int simId);    
    
    /**
     * Check to see if the radio is on or not.
     * @return returns true if the radio is on.
     */
    boolean isRadioOnGemini(int simId);
    
    /**
     * Supply a pin to unlock the SIM.  Blocks until a result is determined.
     * @param pin The pin to check.
     * @return whether the operation was a success.
     */
    boolean supplyPinGemini(String pin, int simId);    
    
    /**
     * Supply a PUK code to unblock the SIM pin lock.  Blocks until a result is determined.
	 * @param puk The PUK code
     * @param pin The pin to check.
     * @return whether the operation was a success.
     */
    boolean supplyPukGemini(String puk, String pin, int simId);

    /**
     * Handles PIN MMI commands (PIN/PIN2/PUK/PUK2), which are initiated
     * without SEND (so <code>dial</code> is not appropriate).
     *
     * @param dialString the MMI command to be executed.
     * @return true if MMI command is executed.
     */
    boolean handlePinMmiGemini(String dialString, int simId);

    /**
     * Returns the IccCard type of Gemini phone. Return "SIM" for SIM card or "USIM" for USIM card.
     */
    String getIccCardTypeGemini(int simId); 

    /**
     * Do sim authentication for gemini and return the raw data of result.
     * Returns the hex format string of auth result.
     * <p> random string in hex format
     * <p> int for simid
     */
    String simAuthGemini(String strRand, int simId); 

    /**
     * Do usim authentication for gemini and return the raw data of result.
     * Returns the hex format string of auth result.
     * <p> random string in hex format
     * <p> int for simid
     */
    String uSimAuthGemini(String strRand, String strAutn, int simId); 

    /**
     * Request to update location information in service state
     */
    void updateServiceLocationGemini(int simId);

    /**
     * Enable location update notifications.
     */
    void enableLocationUpdatesGemini(int simId);

    /**
     * Disable location update notifications.
     */
    void disableLocationUpdatesGemini(int simId);
    

    Bundle getCellLocationGemini(int simId);

    /**
     * Returns the neighboring cell information of the device.
     */
    List<NeighboringCellInfo> getNeighboringCellInfoGemini(int simId);    
 
    /**
    * Returns true if SIM card inserted
     * This API is valid even if airplane mode is on
    */
    boolean isSimInsert(int simId);

    /**
    * Set GPRS connection type, ALWAYS/WHEN_NEEDED
    */
    void setGprsConnType(int type, int simId);

    /**
    * Set GPRS transfer type, Call prefer/Data prefer
    */
    void setGprsTransferType(int type);
    void setGprsTransferTypeGemini(int type, int simId);

    /*Add by mtk80372 for Barcode number*/
    void getMobileRevisionAndIMEI(int type, in Message message);

    /*Add by mtk80372 for Barcode number*/
    String getSN();
    /**
    * Set default phone
    */
    void setDefaultPhone(int simId);

    /**
      * Returns the network type
      */
    int getNetworkTypeGemini(int simId);
    
    boolean hasIccCardGemini(int simId);

    int getSimState(int simId);

    String getSimOperator(int simId);
    
    String getSimOperatorName(int simId);

    String getSimCountryIso(int simId);

    IPhoneSubInfo getSubscriberInfo(int simId);

    String getSimSerialNumber(int simId);

    String getSubscriberId(int simId);

    String getLine1Number(int simId);

    String getLine1AlphaTag(int simId);

    String getVoiceMailNumber(int simId);

    String getVoiceMailAlphaTag(int simId);

    boolean isTestIccCardGemini(int simId);
    
    int enableDataConnectivityGemini(int simId);
    
    int enableApnTypeGemini(String type, int simId);
    
    int disableApnTypeGemini(String type, int simId);

    int disableDataConnectivityGemini(int simId);

    boolean isDataConnectivityPossibleGemini(int simId);

    int getDataStateGemini(int simId);

    int getDataActivityGemini(int simId);

    int getVoiceMessageCountGemini(int simId);

    /**
    * Return true if the FDN of the ICC card is enabled
    */
    boolean isFDNEnabledGemini(int simId);

    boolean isVTIdle();

   /**
     *send BT SIM profile of Connect SIM
     * @param simId specify which SIM to connect
     * @param btRsp fetch the response data.
     * @return success or error code.
   */
   int btSimapConnectSIM(int simId,  out BtSimapOperResponse btRsp);

    /**
     *send BT SIM profile of Disconnect SIM
     * @param null
     * @return success or error code.
   */
   int btSimapDisconnectSIM();

   /**
     *Transfer APDU data through BT SAP
     * @param type Indicate which transport protocol is the preferred one
     * @param cmdAPDU APDU data to transfer in hex character format    
     * @param btRsp fetch the response data.
     * @return success or error code.
   */	
   int btSimapApduRequest(int type, String cmdAPDU, out BtSimapOperResponse btRsp);

    /**
     *send BT SIM profile of Reset SIM
     * @param type Indicate which transport protocol is the preferred one
     * @param btRsp fetch the response data.
     * @return success or error code.
   */
   int btSimapResetSIM(int type, out BtSimapOperResponse btRsp);

   /**
     *send BT SIM profile of Power On SIM
     * @param type Indicate which transport protocol is the preferred onet
     * @param btRsp fetch the response data.
     * @return success or error code.
   */	
   int btSimapPowerOnSIM(int type, out BtSimapOperResponse btRsp);

   /**
     *send BT SIM profile of PowerOff SIM
     * @return success or error code.
   */ 
   int btSimapPowerOffSIM();

   /**
     *get the services state for default SIM
     * @return sim indicator state.    
     *
    */ 
   int getSimIndicatorState(); 

   /**
     *get the services state for specified SIM
     * @param simId Indicate which sim(slot) to query
     * @return sim indicator state.
     *
    */ 
   int getSimIndicatorStateGemini(int simId); 

   /**
     *get the network service state for default SIM
     * @return service state.    
     *
    */ 
   Bundle getServiceState(); 

   /**
     * get the network service state for specified SIM
     * @param simId Indicate which sim(slot) to query
     * @return service state.
     *
    */ 
   Bundle getServiceStateGemini(int simId);  

    /**
     * @return true if phone book is ready.    
    */ 
   boolean isPhbReady(); 

   /**
     * @param simId Indicate which sim(slot) to query
     * @return true if phone book is ready. 
     *
    */ 
   boolean isPhbReadyGemini(int simId);  

   
   String getScAddressGemini(in int simId);
   
   void setScAddressGemini(in String scAddr, in int simId);

   /**
    * @return SMS default SIM. 
    */ 
   int getSmsDefaultSim();  

   int get3GCapabilitySIM();
   boolean set3GCapabilitySIM(int simId);
   int aquire3GSwitchLock();
   boolean release3GSwitchLock(int lockId);
   boolean is3GSwitchLocked();
   String getInterfaceName(String apnType);
   String getIpAddress(String apnType);
   String getGateway(String apnType);
   String getInterfaceNameGemini(String apnType, int slot);
   String getIpAddressGemini(String apnType, int slot);
   String getGatewayGemini(String apnType, int slot);
   int[] getAdnStorageInfo(int simId);
   int cleanupApnTypeGemini(String apnType, int simId); 
   //MTK-END [mtk04070][111117][ALPS00093395]MTK proprietary methods
   //MTK-START [mtk03851][111117]MTK proprietary methods
   void registerForSimModeChange(IBinder binder, int what);
   void unregisterForSimModeChange(IBinder binder);
   void setDataRoamingEnabledGemini(boolean enable, int simId);
   void setRoamingIndicatorNeddedProperty(boolean property1, boolean property2);

   /**
     * Get the count of missed call.
     *
     * @return Return the count of missed call. 
     */
    int getMissedCallCount();

   /**
      Description : Adjust modem radio power for Lenovo SAR requirement.
	  AT command format: AT+ERFTX=<op>,<para1>,<para2>
	  Description : When <op>=1	 -->  TX power reduction
				    <para1>:  2G L1 reduction level, default is 0
				    <para2>:  3G L1 reduction level, default is 0
				    level scope : 0 ~ 64
      Arthur      : mtk04070
      Date        : 2012.01.09
      Return value: True for success, false for failure
    */
   boolean adjustModemRadioPower(int level_2G, int level_3G);

   /**
      Description      : Adjust modem radio power by band for Lenovo SAR requirement.
	  AT command format: AT+ERFTX=<op>,<rat>,<band>,<para1>...<paraX>
	  Description : <op>=3	 -->  TX power reduction by given band
                    <rat>    -->  1 for 2G, 2 for 3G
                    <band>   -->  2G or 3G band value
				    <para1>~<paraX> -->  Reduction level
				    level scope : 0 ~ 255
      Arthur      : mtk04070
      Date        : 2012.05.31
      Return value: True for success, false for failure
   */
   boolean adjustModemRadioPowerByBand(int rat, int band, int level);
   
   //MTK-END [mtk03851][111117]MTK proprietary methods

   // MVNO-API START
   String getSpNameInEfSpn();
   String getSpNameInEfSpnGemini(int simId);

   String isOperatorMvnoForImsi();
   String isOperatorMvnoForImsiGemini(int simId);

   String isOperatorMvnoForEfPnn();
   String isOperatorMvnoForEfPnnGemini(int simId);

   boolean isIccCardProviderAsMvno();
   boolean isIccCardProviderAsMvnoGemini(int simId);
   // MVNO-API END

    /**
     * Gemini
     * Returns the alphabetic name of current registered operator.
     * <p>
     * Availability: Only when user is registered to a network. Result may be
     * unreliable on CDMA networks (use {@link #getPhoneType()} to determine if
     * on a CDMA network).
     */
    String getNetworkOperatorNameGemini(int simId);

    /**
     * Gemini
     * Returns the numeric name (MCC+MNC) of current registered operator.
     * <p>
     * Availability: Only when user is registered to a network. Result may be
     * unreliable on CDMA networks (use {@link #getPhoneType()} to determine if
     * on a CDMA network).
     */
    String getNetworkOperatorGemini(int simId);

    /**
     * Gemini
     * Returns true if the device is considered roaming on the current
     * network, for GSM purposes.
     * <p>
     * Availability: Only when user registered to a network.
     */
    boolean isNetworkRoamingGemini(int simId);

    /**
     * Gemini
     * Returns the ISO country code equivilent of the current registered
     * operator's MCC (Mobile Country Code).
     * <p>
     * Availability: Only when user is registered to a network. Result may be
     * unreliable on CDMA networks (use {@link #getPhoneType()} to determine if
     * on a CDMA network).
     */
    String getNetworkCountryIsoGemini(int simId);
}

