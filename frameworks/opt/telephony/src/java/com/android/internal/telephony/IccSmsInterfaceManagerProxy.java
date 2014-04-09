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

package com.android.internal.telephony;

import android.app.PendingIntent;
import android.os.ServiceManager;
import android.os.Bundle;

import java.util.List;

import android.telephony.SimSmsInsertStatus;
import android.telephony.SmsParameters;
import com.android.internal.telephony.PhoneConstants;
import com.mediatek.common.telephony.IccSmsStorageStatus;

public class IccSmsInterfaceManagerProxy extends ISms.Stub {
    private IccSmsInterfaceManager mIccSmsInterfaceManager;

    public IccSmsInterfaceManagerProxy(IccSmsInterfaceManager
            iccSmsInterfaceManager) {
        // impl
        this(iccSmsInterfaceManager, PhoneConstants.GEMINI_SIM_1);
    }
    
    // mtk added by mtk80589 in 2011.11.16
    public IccSmsInterfaceManagerProxy(IccSmsInterfaceManager
            iccSmsInterfaceManager, int simId) {
        // impl
        this.mIccSmsInterfaceManager = iccSmsInterfaceManager;

        if (PhoneConstants.GEMINI_SIM_1 == simId) {
            if(ServiceManager.getService("isms") == null) {
                ServiceManager.addService("isms", this);
            }
        } else if (PhoneConstants.GEMINI_SIM_2 == simId) {
            if(ServiceManager.getService("isms2") == null) {
                ServiceManager.addService("isms2", this);
            }
        } else if (PhoneConstants.GEMINI_SIM_3 == simId) {
            if(ServiceManager.getService("isms3") == null) {
                ServiceManager.addService("isms3", this);
            }
        } else if (PhoneConstants.GEMINI_SIM_4 == simId) {
            if(ServiceManager.getService("isms4") == null) {
                ServiceManager.addService("isms4", this);
            }
        }
    }
    // mtk added end

    public void setmIccSmsInterfaceManager(IccSmsInterfaceManager iccSmsInterfaceManager) {
        this.mIccSmsInterfaceManager = iccSmsInterfaceManager;
    }

    public boolean
    updateMessageOnIccEf(int index, int status, byte[] pdu) throws android.os.RemoteException {
         return mIccSmsInterfaceManager.updateMessageOnIccEf(index, status, pdu);
    }

    public boolean copyMessageToIccEf(int status, byte[] pdu,
            byte[] smsc) throws android.os.RemoteException {
        return mIccSmsInterfaceManager.copyMessageToIccEf(status, pdu, smsc);
    }

    public List<SmsRawData> getAllMessagesFromIccEf() throws android.os.RemoteException {
        return mIccSmsInterfaceManager.getAllMessagesFromIccEf();
    }

    public void sendData(String destAddr, String scAddr, int destPort,
            byte[] data, PendingIntent sentIntent, PendingIntent deliveryIntent) {
        mIccSmsInterfaceManager.sendData(destAddr, scAddr, destPort, data,
                sentIntent, deliveryIntent);
    }

    public void sendText(String destAddr, String scAddr,
            String text, PendingIntent sentIntent, PendingIntent deliveryIntent) {
        mIccSmsInterfaceManager.sendText(destAddr, scAddr, text, sentIntent, deliveryIntent);
    }

    public void sendMultipartText(String destAddr, String scAddr,
            List<String> parts, List<PendingIntent> sentIntents,
            List<PendingIntent> deliveryIntents) throws android.os.RemoteException {
        mIccSmsInterfaceManager.sendMultipartText(destAddr, scAddr,
                parts, sentIntents, deliveryIntents);
    }

    public boolean enableCellBroadcast(int messageIdentifier) throws android.os.RemoteException {
        return mIccSmsInterfaceManager.enableCellBroadcast(messageIdentifier);
    }

    public boolean disableCellBroadcast(int messageIdentifier) throws android.os.RemoteException {
        return mIccSmsInterfaceManager.disableCellBroadcast(messageIdentifier);
    }

    public boolean enableCellBroadcastRange(int startMessageId, int endMessageId)
            throws android.os.RemoteException {
        return mIccSmsInterfaceManager.enableCellBroadcastRange(startMessageId, endMessageId);
    }

    public boolean disableCellBroadcastRange(int startMessageId, int endMessageId)
            throws android.os.RemoteException {
        return mIccSmsInterfaceManager.disableCellBroadcastRange(startMessageId, endMessageId);
    }

    public boolean activateCellBroadcastSms(boolean activate)
            throws android.os.RemoteException {
        return mIccSmsInterfaceManager.activateCellBroadcastSms(activate);
    }

    public int getPremiumSmsPermission(String packageName) {
        return mIccSmsInterfaceManager.getPremiumSmsPermission(packageName);
    }

    public void setPremiumSmsPermission(String packageName, int permission) {
        mIccSmsInterfaceManager.setPremiumSmsPermission(packageName, permission);
    }

    // mtk added by mtk80589 in 2011.11.16
    public int copyTextMessageToIccCard(String scAddress, String address, List<String> text,
                    int status, long timestamp) throws android.os.RemoteException {
        // impl
        return mIccSmsInterfaceManager.copyTextMessageToIccCard(scAddress, address, text, status, timestamp);
    }
    
    public void sendDataWithOriginalPort(String destAddr, String scAddr, int destPort, int originalPort,
        byte[] data, PendingIntent sentIntent, PendingIntent deliveryIntent) {
        // impl
        mIccSmsInterfaceManager.sendDataWithOriginalPort(destAddr, scAddr, destPort, originalPort,
            data, sentIntent, deliveryIntent);
    }

    public void sendMultipartData(
            String destAddr, String scAddr, int destPort,
            List<SmsRawData> data, List<PendingIntent> sentIntents, 
            List<PendingIntent> deliveryIntents) {
        // impl
        mIccSmsInterfaceManager.sendMultipartData(destAddr, scAddr, destPort, data,
                sentIntents, deliveryIntents);
    }

    public void sendTextWithPort(String destAddr, String scAddr, String text, 
            int destPort, PendingIntent sentIntent, PendingIntent deliveryIntent) {
        // impl
        mIccSmsInterfaceManager.sendTextWithPort(destAddr, scAddr, text, 
                destPort, sentIntent, deliveryIntent);
    }

    public void sendMultipartTextWithPort(String destAddr, String scAddr, List<String> parts,
            int destPort, List<PendingIntent> sentIntents, 
            List<PendingIntent> deliveryIntents) {
        // impl
        mIccSmsInterfaceManager.sendMultipartTextWithPort(destAddr, scAddr,
                parts, destPort, sentIntents, deliveryIntents);
    }

    public void setSmsMemoryStatus(boolean status) {
        mIccSmsInterfaceManager.setSmsMemoryStatus(status);
    }

    public boolean isSmsReady() {
        return mIccSmsInterfaceManager.isSmsReady();
    }

    public IccSmsStorageStatus getSmsSimMemoryStatus() throws android.os.RemoteException {
        return mIccSmsInterfaceManager.getSmsSimMemoryStatus();
    }
    // MTK-END [ALPS000xxxxx] MTK code port to ICS added by mtk80589 in 2011.11.16
    
    // mtk added [ALPS00094531] Orange feature SMS Encoding Type Setting by mtk80589 in 2011.11.22
    public void sendTextWithEncodingType(
            String destAddr,
            String scAddr,
            String text,
            int encodingType,
            PendingIntent sentIntent,
            PendingIntent deliveryIntent) {
        // impl
        mIccSmsInterfaceManager.sendTextWithEncodingType(
            destAddr, scAddr,
            text, encodingType,
            sentIntent, deliveryIntent);
    }
    
    public void sendMultipartTextWithEncodingType(
            String destAddr,
            String scAddr,
            List<String> parts,
            int encodingType,
            List<PendingIntent> sentIntents,
            List<PendingIntent> deliveryIntents) {
        // impl
        mIccSmsInterfaceManager.sendMultipartTextWithEncodingType(
            destAddr, scAddr,
            parts, encodingType,
            sentIntents, deliveryIntents);
    }
    // mtk added end
    
    public SimSmsInsertStatus insertTextMessageToIccCard(String scAddress, String address,
            List<String> text, int status, long timestamp) throws android.os.RemoteException {
        //impl
        return mIccSmsInterfaceManager.insertTextMessageToIccCard(scAddress, address, text, status, timestamp);
    }
    
    public SimSmsInsertStatus insertRawMessageToIccCard(int status, byte[] pdu, byte[] smsc)
            throws android.os.RemoteException {
        //impl
        return mIccSmsInterfaceManager.insertRawMessageToIccCard(status, pdu, smsc);
    }
    
    public void sendTextWithExtraParams(
            String destAddr,
            String scAddr,
            String text,
            Bundle extraParams,
            PendingIntent sentIntent,
            PendingIntent deliveryIntent) {
        // impl
        mIccSmsInterfaceManager.sendTextWithExtraParams(destAddr, scAddr, text,
                extraParams, sentIntent, deliveryIntent);
    }
    
    public void sendMultipartTextWithExtraParams(
            String destAddr,
            String scAddr,
            List<String> parts,
            Bundle extraParams,
            List<PendingIntent> sentIntents,
            List<PendingIntent> deliveryIntents) {
        // impl
        mIccSmsInterfaceManager.sendMultipartTextWithExtraParams(destAddr, scAddr,
                parts, extraParams, sentIntents, deliveryIntents);
    }

    public String getFormat() {
        return mIccSmsInterfaceManager.getFormat();
    }
    
    public SmsParameters getSmsParameters() throws android.os.RemoteException {
        return mIccSmsInterfaceManager.getSmsParameters();
    }
    
    public boolean setSmsParameters(SmsParameters params) throws android.os.RemoteException {
        return mIccSmsInterfaceManager.setSmsParameters(params);
    }
    
    public SmsRawData getMessageFromIccEf(int index) throws android.os.RemoteException {
        return mIccSmsInterfaceManager.getMessageFromIccEf(index);
    }
    
    public boolean setEtwsConfig(int mode) throws android.os.RemoteException {
        return mIccSmsInterfaceManager.setEtwsConfig(mode);
    }
}
