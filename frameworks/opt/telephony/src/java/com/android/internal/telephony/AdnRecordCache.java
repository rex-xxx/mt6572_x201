/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
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

package com.android.internal.telephony;

import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.SparseArray;

import com.android.internal.telephony.gsm.UsimPhoneBookManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.mediatek.common.telephony.AlphaTag;
import com.mediatek.common.telephony.UsimGroup;
import com.mediatek.common.telephony.gsm.UsimPBMemInfo;

/**
 * {@hide}
 */
public final class AdnRecordCache extends Handler implements IccConstants {

    static final String LOG_TAG = "GSM";

    //***** Instance Variables

    private IccFileHandler mFh;
    private UsimPhoneBookManager mUsimPhoneBookManager;

    // Indexed by EF ID
    SparseArray<ArrayList<AdnRecord>> adnLikeFiles
        = new SparseArray<ArrayList<AdnRecord>>();

    // People waiting for ADN-like files to be loaded
    SparseArray<ArrayList<Message>> adnLikeWaiters
        = new SparseArray<ArrayList<Message>>();

    // People waiting for adn record to be updated
    SparseArray<Message> userWriteResponse = new SparseArray<Message>();

    //***** Event Constants

    static final int EVENT_LOAD_ALL_ADN_LIKE_DONE = 1;
    static final int EVENT_UPDATE_ADN_DONE = 2;

    //***** Constructor

    public static int MAX_PHB_NAME_LENGTH = 60;
    public static int MAX_PHB_NUMBER_LENGTH = 40;
    public static int MAX_PHB_NUMBER_ANR_LENGTH = 20;

    private int mSimId;

    private Object mLock = new Object();
    boolean mSuccess = false;
    private boolean mLocked = false;
    public AdnRecordCache(IccFileHandler fh) {
        mFh = fh;
        mUsimPhoneBookManager = new UsimPhoneBookManager(mFh, this);
        mSimId = mFh.getMySimId();
    }

    //***** Called from SIMRecords

    /**
     * Called from SIMRecords.onRadioNotAvailable and SIMRecords.handleSimRefresh.
     */
    public void reset() {
        logd("reset");
        adnLikeFiles.clear();
        mUsimPhoneBookManager.reset();

        clearWaiters();
        clearUserWriters();

    }

    private void clearWaiters() {
        int size = adnLikeWaiters.size();
        for (int i = 0; i < size; i++) {
            ArrayList<Message> waiters = adnLikeWaiters.valueAt(i);
            AsyncResult ar = new AsyncResult(null, null, new RuntimeException("AdnCache reset"));
            notifyWaiters(waiters, ar);
        }
        adnLikeWaiters.clear();
    }

    private void clearUserWriters() {
        logd("clearUserWriters,mLocked " + mLocked);
        if (mLocked) {
            synchronized (mLock) {
                mLock.notifyAll();
            }
            mLocked = false;
        }
        int size = userWriteResponse.size();
        for (int i = 0; i < size; i++) {
            sendErrorResponse(userWriteResponse.valueAt(i), "AdnCace reset");
        }
        userWriteResponse.clear();
    }

    /**
     * @return List of AdnRecords for efid if we've already loaded them this
     *         radio session, or null if we haven't
     */
    public ArrayList<AdnRecord> getRecordsIfLoaded(int efid) {
        return adnLikeFiles.get(efid);
    }

    /**
     * Returns extension ef associated with ADN-like EF or -1 if
     * we don't know.
     *
     * See 3GPP TS 51.011 for this mapping
     */
    public int extensionEfForEf(int efid) {
        switch (efid) {
            case EF_MBDN: return EF_EXT6;
            case EF_ADN: return EF_EXT1;
            case EF_SDN: return EF_EXT3;
            case EF_FDN: return EF_EXT2;
            case EF_MSISDN: return EF_EXT1;
            case EF_PBR: return 0; // The EF PBR doesn't have an extension record
            default: return -1;
        }
    }

    private void sendErrorResponse(Message response, String errString) {

        sendErrorResponse(
                response,
                errString,
                RILConstants.GENERIC_FAILURE);
    }

    private void sendErrorResponse(Message response, String errString, int ril_errno) {

        CommandException e = CommandException.fromRilErrno(ril_errno);

        if (response != null) {
            logd(errString);
            AsyncResult.forMessage(response).exception = e;
            response.sendToTarget();
        }
    }

    /**
     * Update an ADN-like record in EF by record index
     *
     * @param efid must be one among EF_ADN, EF_FDN, and EF_SDN
     * @param adn is the new adn to be stored
     * @param recordIndex is the 1-based adn record index
     * @param pin2 is required to update EF_FDN, otherwise must be null
     * @param response message to be posted when done
     *        response.exception hold the exception in error
     */
    public synchronized void updateAdnByIndex(int efid, AdnRecord adn, int recordIndex, String pin2,
            Message response) {

        int extensionEF = extensionEfForEf(efid);
        if (extensionEF < 0) {
            sendErrorResponse(response, "EF is not known ADN-like EF:" + efid);
            return;
        }
        // MTK-START [mtk80601][111215][ALPS00093395]
        if (adn.alphaTag.length() > MAX_PHB_NAME_LENGTH) {

            sendErrorResponse(
                    response,
                    "the input length of alphaTag is too long: " + adn.alphaTag,
                    RILConstants.TEXT_STRING_TOO_LONG);
            return;
        }

        if (adn.additionalNumber != null
                && adn.additionalNumber.length() > MAX_PHB_NUMBER_ANR_LENGTH) {

            sendErrorResponse(
                    response,
                    "the input length of additional number is too long: " + adn.additionalNumber,
                    RILConstants.ADDITIONAL_NUMBER_STRING_TOO_LONG);
            return;
        }

        int num_length = adn.number.length();
        if (adn.number.indexOf('+') != -1) {
            num_length--;
        }

        if (num_length > MAX_PHB_NUMBER_LENGTH) {

            sendErrorResponse(
                    response,
                    "the input length of phoneNumber is too long: " + adn.number,
                    RILConstants.DIAL_STRING_TOO_LONG);

            return;
        }
        // MTK-END [mtk80601][111215][ALPS00093395]
        Message pendingResponse = userWriteResponse.get(efid);
        if (pendingResponse != null) {
            sendErrorResponse(response, "Have pending update for EF:" + efid);
            return;
        }

        userWriteResponse.put(efid, response);
        if (efid == IccConstants.EF_ADN || efid == IccConstants.EF_PBR) {
            if (adn.alphaTag.length() == 0 && adn.number.length() == 0) {
                // delete the group info
                mUsimPhoneBookManager.removeContactGroup(recordIndex);
            }
        }
        synchronized (mLock) {
            new AdnRecordLoader(mFh).updateEF(adn, efid, extensionEF,
                    recordIndex, pin2,
                    obtainMessage(EVENT_UPDATE_ADN_DONE, efid, recordIndex, adn));
            // MTK-START [mtk80601][111215][ALPS00093395]
            mLocked = true;
            try {
                mLock.wait();
            } catch (InterruptedException e) {
                return;
            }
        }
        if (!mSuccess) {
            return;
        }
        // update anr/grpIds/emails if necessary
        if (efid == IccConstants.EF_ADN || efid == IccConstants.EF_PBR) {
            try {
                mUsimPhoneBookManager.updateAnrByAdnIndex(adn.additionalNumber, recordIndex);
                if (mUsimPhoneBookManager.supportVodafone()) {
                    mUsimPhoneBookManager.updateSneByAdnIndex(adn.sne, recordIndex);
                }
                int success = mUsimPhoneBookManager.updateEmailsByAdnIndex(adn.emails, recordIndex);
                if (-1 == success) {
                    sendErrorResponse(
                            response,
                            "drop the email for the limitation of the SIM card",
                            RILConstants.EMAIL_SIZE_LIMIT);
                } else if (-2 == success) {
                    sendErrorResponse(
                            response,
                            "the email string is too long",
                            RILConstants.EMAIL_NAME_TOOLONG);
                    Log.e(LOG_TAG, "haman, by index email too long");
                } else {
                    AsyncResult.forMessage(response, null, null);
                    response.sendToTarget();
                }
            } catch (Exception e) {
                e.printStackTrace();
                // Log.e(LOG_TAG, "exception occured when update anr and email "
                // + e);
                return;
            }
        } else if (efid == IccConstants.EF_FDN) {
            AsyncResult.forMessage(response, null, null);
            response.sendToTarget();
        }
        // MTK-END [mtk80601][111215][ALPS00093395]
    }

    /**
     * Replace oldAdn with newAdn in ADN-like record in EF
     *
     * The ADN-like records must be read through requestLoadAllAdnLike() before
     *
     * @param efid must be one of EF_ADN, EF_FDN, and EF_SDN
     * @param oldAdn is the adn to be replaced
     *        If oldAdn.isEmpty() is ture, it insert the newAdn
     * @param newAdn is the adn to be stored
     *        If newAdn.isEmpty() is true, it delete the oldAdn
     * @param pin2 is required to update EF_FDN, otherwise must be null
     * @param response message to be posted when done
     *        response.exception hold the exception in error
     */
    public synchronized int updateAdnBySearch(int efid, AdnRecord oldAdn, AdnRecord newAdn,
            String pin2, Message response) {
        logd("updateAdnBySearch efid:" + efid + "pin2:" + pin2 + ", oldAdn [" + oldAdn
                + "], new Adn[" + newAdn + "]");
        int index = -1;
        int extensionEF;
        extensionEF = extensionEfForEf(efid);

        if (extensionEF < 0) {
            sendErrorResponse(response, "EF is not known ADN-like EF:" + efid);
            return index;
        }
        // MTK-START [mtk80601][111215][ALPS00093395]
        if (newAdn.alphaTag.length() > MAX_PHB_NAME_LENGTH) {

            sendErrorResponse(
                    response,
                    "the input length of alphaTag is too long: " + newAdn.alphaTag,
                    RILConstants.TEXT_STRING_TOO_LONG);
            return index;
        }

        int num_length = newAdn.number.length();
        if (newAdn.number.indexOf('+') != -1) {
            num_length--;
        }

        if (num_length > MAX_PHB_NUMBER_LENGTH) {

            sendErrorResponse(
                    response,
                    "the input length of phoneNumber is too long: " + newAdn.number,
                    RILConstants.DIAL_STRING_TOO_LONG);

            return index;
        }

        if (newAdn.additionalNumber != null) {
            num_length = newAdn.additionalNumber.length();
            if (newAdn.additionalNumber.indexOf('+') != -1) {
                num_length--;
            }

            if (num_length > MAX_PHB_NUMBER_ANR_LENGTH) {
                sendErrorResponse(
                        response,
                        "the input length of additional number is too long: "
                        + newAdn.additionalNumber,
                        RILConstants.ADDITIONAL_NUMBER_STRING_TOO_LONG);
                return index;
            }
            // MTK-END [mtk80601][111215][ALPS00093395]
        }
        if (!mUsimPhoneBookManager.checkEmailLength(newAdn.emails)) {
            sendErrorResponse(
                    response,
                    "the email string is too long",
                    RILConstants.EMAIL_NAME_TOOLONG);
            return index;
        }

        ArrayList<AdnRecord>  oldAdnList;

        if (efid == EF_PBR) {
            oldAdnList = mUsimPhoneBookManager.loadEfFilesFromUsim();
        } else {
            oldAdnList = getRecordsIfLoaded(efid);
        }

        if (oldAdnList == null) {
            sendErrorResponse(
                    response,
                    "Adn list not exist for EF:" + efid,
                    RILConstants.ADN_LIST_NOT_EXIST);
            return index;
        }

        int count = 1;
        for (Iterator<AdnRecord> it = oldAdnList.iterator(); it.hasNext(); ) {
            if (oldAdn.isEqual(it.next())) {
                index = count;
                break;
            }
            count++;
        }
        logd("updateAdnBySearch index " + index);
        if (index == -1) {
            if (oldAdn.alphaTag.length() == 0 && oldAdn.number.length() == 0) {
                sendErrorResponse(
                        response,
                        "Adn record don't exist for " + oldAdn,
                        RILConstants.SIM_MEM_FULL);
            } else {
                sendErrorResponse(response, "Adn record don't exist for " + oldAdn);
            }
            return index;
        }

        if (efid == EF_PBR) {
            AdnRecord foundAdn = oldAdnList.get(index - 1);
            efid = foundAdn.efid;
            extensionEF = foundAdn.extRecord;
            index = foundAdn.recordNumber;

            newAdn.efid = efid;
            newAdn.extRecord = extensionEF;
            newAdn.recordNumber = index;
        }

        Message pendingResponse = userWriteResponse.get(efid);

        if (pendingResponse != null) {
            sendErrorResponse(response, "Have pending update for EF:" + efid);
            return index;
        }
        if (0 == efid) {
            sendErrorResponse(response, "Abnormal efid: " + efid);
            return index;
        }
        if (!mUsimPhoneBookManager.checkEmailCapacityFree(index, newAdn.emails)) {
            sendErrorResponse(
                    response,
                    "drop the email for the limitation of the SIM card",
                    RILConstants.EMAIL_SIZE_LIMIT);
            return index;
        }
        if (!mUsimPhoneBookManager.checkEmailLength(newAdn.emails)) {
            sendErrorResponse(
                    response,
                    "the email string is too long",
                    RILConstants.EMAIL_NAME_TOOLONG);
            return index;
        }

        userWriteResponse.put(efid, response);

        synchronized (mLock) {
            new AdnRecordLoader(mFh).updateEF(newAdn, efid, extensionEF,
                    index, pin2,
                    obtainMessage(EVENT_UPDATE_ADN_DONE, efid, index, newAdn));
            // MTK-START [mtk80601][111215][ALPS00093395]
            mLocked = true;
            try {
                mLock.wait();
            } catch (InterruptedException e) {
                return index;
            }
        }
        if (!mSuccess) {
            logd("updateAdnBySearch mSuccess:" + mSuccess);
            return index;
        }
        int success = 0;
        if (efid == EF_ADN || efid == EF_PBR) {
            if (mUsimPhoneBookManager.supportVodafone()) {
                mUsimPhoneBookManager.updateSneByAdnIndex(newAdn.sne, index);
            }
            mUsimPhoneBookManager.updateAnrByAdnIndex(newAdn.additionalNumber, index);
            success = mUsimPhoneBookManager.updateEmailsByAdnIndex(newAdn.emails, index);
        }

        if (-1 == success) {
            sendErrorResponse(response,
                    "drop the email for the limitation of the SIM card",
                    RILConstants.EMAIL_SIZE_LIMIT);
        } else if (-2 == success) {
            sendErrorResponse(
                    response,
                    "the email string is too long",
                    RILConstants.EMAIL_NAME_TOOLONG);
            Log.e(LOG_TAG, "haman, by search email too long");
        } else {
            logd("updateAdnBySearch response:" + response);
            AsyncResult.forMessage(response, null, null);
            response.sendToTarget();
        }
        return index;
        // MTK-END [mtk80601][111215][ALPS00093395]

    }

    /**
     * Responds with exception (in response) if efid is not a known ADN-like
     * record
     */
    public void
    requestLoadAllAdnLike (int efid, int extensionEf, Message response) {
        ArrayList<Message> waiters;
        ArrayList<AdnRecord> result;
        logd("requestLoadAllAdnLike " + efid);
        if (efid == EF_PBR) {
            result = mUsimPhoneBookManager.loadEfFilesFromUsim();
        } else {
            result = getRecordsIfLoaded(efid);
        }
        logd("requestLoadAllAdnLike result = null ?" + (result == null));

        // Have we already loaded this efid?
        if (result != null) {
            if (response != null) {
                AsyncResult.forMessage(response).result = result;
                response.sendToTarget();
            }

            return;
        }

        // Have we already *started* loading this efid?

        waiters = adnLikeWaiters.get(efid);

        if (waiters != null) {
            // There's a pending request for this EF already
            // just add ourselves to it

            waiters.add(response);
            return;
        }

        // Start loading efid

        waiters = new ArrayList<Message>();
        waiters.add(response);

        adnLikeWaiters.put(efid, waiters);

        if (extensionEf < 0) {
            // respond with error if not known ADN-like record

            if (response != null) {
                AsyncResult.forMessage(response).exception
                    = new RuntimeException("EF is not known ADN-like EF:" + efid);
                response.sendToTarget();
            }

            return;
        }

        new AdnRecordLoader(mFh).loadAllFromEF(efid, extensionEf,
            obtainMessage(EVENT_LOAD_ALL_ADN_LIKE_DONE, efid, 0));
    }

    //***** Private methods

    private void
    notifyWaiters(ArrayList<Message> waiters, AsyncResult ar) {

        if (waiters == null) {
            return;
        }

        for (int i = 0, s = waiters.size() ; i < s ; i++) {
            Message waiter = waiters.get(i);

            if (waiter != null) {
                AsyncResult.forMessage(waiter, ar.result, ar.exception);
                waiter.sendToTarget();
            }
        }
    }

    //***** Overridden from Handler

    public void
    handleMessage(Message msg) {
        AsyncResult ar;
        int efid;

        switch(msg.what) {
            case EVENT_LOAD_ALL_ADN_LIKE_DONE:
                /* arg1 is efid, obj.result is ArrayList<AdnRecord>*/
                ar = (AsyncResult) msg.obj;
                efid = msg.arg1;
                ArrayList<Message> waiters;

                waiters = adnLikeWaiters.get(efid);
                adnLikeWaiters.delete(efid);

                if (ar.exception == null) {
                    adnLikeFiles.put(efid, (ArrayList<AdnRecord>) ar.result);
                } else {
                    Log.d(LOG_TAG, "EVENT_LOAD_ALL_ADN_LIKE_DONE exception", ar.exception);
                }
                notifyWaiters(waiters, ar);
                break;
            case EVENT_UPDATE_ADN_DONE:
                logd("EVENT_UPDATE_ADN_DONE");
                synchronized (mLock) {
                    ar = (AsyncResult) msg.obj;
                    efid = msg.arg1;
                    int index = msg.arg2;
                    AdnRecord adn = (AdnRecord) (ar.userObj);

                    if (ar.exception == null) {
                        if (null != adn) {
                            adn.setRecordIndex(index);
                            if (adn.efid <= 0) {
                                adn.efid = efid;
                            }
                        }
                        logd("adnLikeFiles changed index:" + index + ",adn:" + adn);
                        if (null != adnLikeFiles && null != adnLikeFiles.get(efid)) {
                            adnLikeFiles.get(efid).set(index - 1, adn);
                        }
                        if ((null != mUsimPhoneBookManager) && (efid != IccConstants.EF_FDN)) {
                            mUsimPhoneBookManager.updateUsimPhonebookRecordsList(index - 1, adn);
                        }
                    }

                    Message response = userWriteResponse.get(efid);
                    userWriteResponse.delete(efid);

                    logd("AdnRecordCacheEx: " + ar.exception);

                    if (ar.exception != null && response != null) {
                        AsyncResult.forMessage(response, null, ar.exception);
                        response.sendToTarget();
                    }
                    mSuccess = ar.exception == null;
                    mLock.notifyAll();
                    mLocked = false;
                }
                break;
                default:
                    break;
        }

    }

    // MTK-START [mtk80601][111215][ALPS00093395]
    protected void logd(String msg) {
        Log.d(LOG_TAG, "[AdnRecordCache" + mSimId + "] " + msg);
    }

    public List<UsimGroup> getUsimGroups() {
        return mUsimPhoneBookManager.getUsimGroups();
    }

    public String getUsimGroupById(int nGasId) {
        return mUsimPhoneBookManager.getUsimGroupById(nGasId);
    }

    public boolean removeUsimGroupById(int nGasId) {
        return mUsimPhoneBookManager.removeUsimGroupById(nGasId);
    }

    public int insertUsimGroup(String grpName) {
        return mUsimPhoneBookManager.insertUsimGroup(grpName);
    }

    public int updateUsimGroup(int nGasId, String grpName) {
        return mUsimPhoneBookManager.updateUsimGroup(nGasId, grpName);
    }

    public boolean addContactToGroup(int adnIndex, int grpIndex) {
        return mUsimPhoneBookManager.addContactToGroup(adnIndex, grpIndex);
    }

    public boolean removeContactFromGroup(int adnIndex, int grpIndex) {
        return mUsimPhoneBookManager.removeContactFromGroup(adnIndex, grpIndex);
    }

    public boolean updateContactToGroups(int adnIndex, int[] grpIdList) {
        return mUsimPhoneBookManager.updateContactToGroups(adnIndex, grpIdList);
    }

    public int hasExistGroup(String grpName) {
        return mUsimPhoneBookManager.hasExistGroup(grpName);
    }

    public int getUsimGrpMaxNameLen() {
        return mUsimPhoneBookManager.getUsimGrpMaxNameLen();
    }

    public int getUsimGrpMaxCount() {
        return mUsimPhoneBookManager.getUsimGrpMaxCount();
    }

    private void dumpAdnLikeFile() {
        int size = adnLikeFiles.size();
        logd("dumpAdnLikeFile size " + size);
        int key;
        for (int i = 0; i < size; i++) {
            key = adnLikeFiles.keyAt(i);

            ArrayList<AdnRecord> records = adnLikeFiles.get(key);
            logd("dumpAdnLikeFile index " + i + " key " + key + "records size " + records.size());
            for (int j = 0; j < records.size(); j++) {
                AdnRecord record = records.get(j);
                logd("adnLikeFiles[" + j + "]=" + record);
            }
        }
    }

    public ArrayList<AlphaTag> getUsimAasList() {
        return mUsimPhoneBookManager.getUsimAasList();
    }

    public String getUsimAasById(int index) {
        // TODO
        return mUsimPhoneBookManager.getUsimAasById(index, 0);
    }

    public boolean removeUsimAasById(int index, int pbrIndex) {
        return mUsimPhoneBookManager.removeUsimAasById(index, pbrIndex);
    }

    public int insertUsimAas(String aasName) {
        return mUsimPhoneBookManager.insertUsimAas(aasName);
    }

    public boolean updateUsimAas(int index, int pbrIndex, String aasName) {
        return mUsimPhoneBookManager.updateUsimAas(index, pbrIndex, aasName);
    }

    /**
     * @param adnIndex: ADN index
     * @param aasIndex: change AAS to the value refered by aasIndex, -1 means
     *            remove
     * @return
     */
    public boolean updateAdnAas(int adnIndex, int aasIndex) {
        return mUsimPhoneBookManager.updateAdnAas(adnIndex, aasIndex);
    }

    public int getAnrCount() {
        return mUsimPhoneBookManager.getAnrCount();
    }

    public int getEmailCount() {
        return mUsimPhoneBookManager.getEmailCount();
    }

    public int getUsimAasMaxCount() {
        return mUsimPhoneBookManager.getUsimAasMaxCount();
    }

    public int getUsimAasMaxNameLen() {
        return mUsimPhoneBookManager.getUsimAasMaxNameLen();
    }

    public boolean hasSne() {
        return mUsimPhoneBookManager.hasSne();
    }

    public int getSneRecordLen() {
        return mUsimPhoneBookManager.getSneRecordLen();
    }

    public UsimPBMemInfo[] getPhonebookMemStorageExt() {
        return mUsimPhoneBookManager.getPhonebookMemStorageExt();
    }
    // MTK-END [mtk80601][111215][ALPS00093395]

}
