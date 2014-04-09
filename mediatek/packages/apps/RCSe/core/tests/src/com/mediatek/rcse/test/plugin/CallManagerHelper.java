/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2012. All rights reserved.
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

package com.mediatek.rcse.test.plugin;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.telephony.CellLocation;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;

import com.mediatek.rcse.test.Utils;

import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallManager;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.IccFileHandler;
import com.android.internal.telephony.IccPhoneBookInterfaceManager;
import com.android.internal.telephony.IccSmsInterfaceManager;
import com.android.internal.telephony.MmiCode;
import com.android.internal.telephony.OperatorInfo;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneBase;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneNotifier;
import com.android.internal.telephony.PhoneSubInfo;
import com.android.internal.telephony.UUSInfo;
import com.android.internal.telephony.test.SimulatedCommands;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is used by test case to manage the call status
 */
public class CallManagerHelper {
    private MockPhone mMockPhone = null;
    public void initialize(Context context) throws Exception {
        Constructor constructor = Utils.getPrivateConstructor(CallManager.class);
        Field fieldInstance = Utils.getPrivateField(CallManager.class, "INSTANCE");
        fieldInstance.set(null, constructor.newInstance());
        Field fieldDefaultPhone = Utils.getPrivateField(CallManager.class, "mDefaultPhone");
        mMockPhone = new MockPhone(null, context, new SimulatedCommands());
        fieldDefaultPhone.set(CallManager.getInstance(), mMockPhone);
    }

    public void setActiveBgCall(boolean hasActiveBgCall) throws Exception {
        Field fieldRingingCalls = Utils.getPrivateField(CallManager.class, "mBackgroundCalls");
        List ringingCalls = (List) fieldRingingCalls.get(CallManager.getInstance());
        if (hasActiveBgCall) {
            Call call = new MockCall();
            call.state = com.android.internal.telephony.Call.State.ACTIVE;
            ringingCalls.add(call);
        } else {
            ringingCalls.clear();
        }
    }

    public static class MockConnection extends Connection {

        @Override
        public void cancelPostDial() {
            // TODO Auto-generated method stub
            
        }

        @Override
        public String getAddress() {
            return RCSeInCallScreenExtensionTest.MOCK_NUMBER;
        }

        @Override
        public Call getCall() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public long getConnectTime() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public long getCreateTime() {
            return 1;
        }

        @Override
        public DisconnectCause getDisconnectCause() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public long getDisconnectTime() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public long getDurationMillis() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public long getHoldDurationMillis() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public int getNumberPresentation() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public PostDialState getPostDialState() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getRemainingPostDialString() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public UUSInfo getUUSInfo() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void hangup() throws CallStateException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public boolean isIncoming() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isVideo() {
            return false;
        }

        @Override
        public void proceedAfterWaitChar() {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void proceedAfterWildChar(String arg0) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void separate() throws CallStateException {
            // TODO Auto-generated method stub
            
        }

    }

    private class MockCall extends Call {

        @Override
        public List<Connection> getConnections() {
            List<Connection> connections = new ArrayList<Connection>();
            connections.add(new MockConnection());
            return connections;
        }

        @Override
        public Phone getPhone() {
            return mMockPhone;
        }

        @Override
        public void hangup() throws CallStateException {

        }

        @Override
        public boolean isMultiparty() {
            // TODO Auto-generated method stub
            return false;
        }
        
    }
    private class MockPhone extends PhoneBase {

        protected MockPhone(PhoneNotifier notifier, Context context, CommandsInterface ci) {
            super(notifier, context, ci);
        }

        public Call getForegroundCall() {
            Call call = new MockCall();
            call.state = Call.State.ACTIVE;
            return call;
        }

        @Override
        public IccFileHandler getIccFileHandler() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getPhoneName() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public int getPhoneType() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public PhoneConstants.State getState() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void acceptCall() throws CallStateException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void activateCellBroadcastSms(int arg0, Message arg1) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public boolean canConference() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean canTransfer() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public void changeBarringPassword(String arg0, String arg1, String arg2, Message arg3) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void changeBarringPassword(String arg0, String arg1, String arg2, String arg3, Message arg4) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void clearDisconnected() {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void conference() throws CallStateException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public Connection dial(String arg0) throws CallStateException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Connection dial(String arg0, UUSInfo arg1) throws CallStateException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void disableLocationUpdates() {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void enableLocationUpdates() {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void explicitCallTransfer() throws CallStateException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void getAccumulatedCallMeter(Message arg0) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void getAccumulatedCallMeterMaximum(Message arg0) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public String getActiveApnType() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void getAvailableNetworks(Message arg0) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public Call getBackgroundCall() {
            return new MockCall();
        }

        @Override
        public void getCallForwardingOption(int arg0, Message arg1) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void getCallWaiting(Message arg0) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void getCellBroadcastSmsConfig(Message arg0) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public CellLocation getCellLocation() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void getCurrentCallMeter(Message arg0) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public DataActivityState getDataActivityState() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void getDataCallList(Message arg0) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public PhoneConstants.DataState getDataConnectionState(String arg0) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public boolean getDataRoamingEnabled() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public String getDeviceId() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getDeviceSvn() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getEsn() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void getFacilityLock(String arg0, String arg1, Message arg2) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public IccPhoneBookInterfaceManager getIccPhoneBookInterfaceManager() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public IccServiceStatus getIccServiceStatus(IccService arg0) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public IccSmsInterfaceManager getIccSmsInterfaceManager() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getImei() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public int getLastCallFailCause() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public String getLine1AlphaTag() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getLine1Number() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getMeid() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public boolean getMute() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public void getNeighboringCids(Message arg0) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void getOutgoingCallerIdDisplay(Message arg0) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void getPdpContextList(Message arg0) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public List<? extends MmiCode> getPendingMmiCodes() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public PhoneSubInfo getPhoneSubInfo() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void getPpuAndCurrency(Message arg0) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public Call getRingingCall() {
            return new MockCall();
        }

        @Override
        public String getSN() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public ServiceState getServiceState() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public SignalStrength getSignalStrength() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getSubscriberId() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getVoiceMailAlphaTag() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getVoiceMailNumber() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void getVtCallForwardingOption(int arg0, Message arg1) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void getVtCallWaiting(Message arg0) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void getVtFacilityLock(String arg0, String arg1, Message arg2) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public boolean handleInCallMmiCommands(String arg0) throws CallStateException {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean handlePinMmi(String arg0) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public void hangupActiveCall() throws CallStateException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void hangupAll() throws CallStateException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void notifySimMissingStatus(boolean arg0) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void registerForCrssSuppServiceNotification(Handler arg0, int arg1, Object arg2) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void registerForSuppServiceNotification(Handler arg0, int arg1, Object arg2) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void rejectCall() throws CallStateException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void resetAccumulatedCallMeter(String arg0, Message arg1) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void selectNetworkManually(OperatorInfo arg0, Message arg1) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void sendDtmf(char arg0) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void sendUssdResponse(String arg0) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void setAccumulatedCallMeterMaximum(String arg0, String arg1, Message arg2) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void setCRO(int arg0, Message arg1) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void setCallForwardingOption(int arg0, int arg1, String arg2, int arg3, Message arg4) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void setCallWaiting(boolean arg0, Message arg1) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void setCellBroadcastSmsConfig(int[] arg0, Message arg1) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void setDataRoamingEnabled(boolean arg0) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void setFacilityLock(String arg0, boolean arg1, String arg2, Message arg3) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void setLine1Number(String arg0, String arg1, Message arg2) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void setMute(boolean arg0) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void setNetworkSelectionModeAutomatic(Message arg0) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void setOnPostDialCharacter(Handler arg0, int arg1, Object arg2) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void setOutgoingCallerIdDisplay(int arg0, Message arg1) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void setPpuAndCurrency(String arg0, String arg1, String arg2, Message arg3) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void setRadioPower(boolean arg0) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void setRadioPower(boolean arg0, boolean arg1) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void setVoiceMailNumber(String arg0, String arg1, Message arg2) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void setVtCallForwardingOption(int arg0, int arg1, String arg2, int arg3, Message arg4) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void setVtCallWaiting(boolean arg0, Message arg1) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void setVtFacilityLock(String arg0, boolean arg1, String arg2, Message arg3) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void startDtmf(char arg0) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void stopDtmf() {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void switchHoldingAndActive() throws CallStateException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void unregisterForCrssSuppServiceNotification(Handler arg0) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void unregisterForSuppServiceNotification(Handler arg0) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void updateServiceLocation() {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void voiceAccept() throws CallStateException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public Connection vtDial(String arg0) throws CallStateException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Connection vtDial(String arg0, UUSInfo arg1) throws CallStateException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void onUpdateIccAvailability(){
            
        }
    }
}
