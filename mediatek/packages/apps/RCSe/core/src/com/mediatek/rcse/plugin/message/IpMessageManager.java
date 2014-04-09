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

package com.mediatek.rcse.plugin.message;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelUuid;
import android.provider.Telephony;
import android.text.TextUtils;
import android.widget.Toast;

import com.mediatek.mms.ipmessage.IpMessageConsts;
import com.mediatek.mms.ipmessage.MessageManager;
import com.mediatek.mms.ipmessage.message.IpAttachMessage;
import com.mediatek.mms.ipmessage.message.IpMessage;
import com.mediatek.mms.ipmessage.message.IpTextMessage;
import com.mediatek.rcse.activities.widgets.ContactsListManager;
import com.mediatek.rcse.api.Logger;
import com.mediatek.rcse.interfaces.ChatController;
import com.mediatek.rcse.interfaces.ChatView.ISentChatMessage.Status;
import com.mediatek.rcse.mvc.ModelImpl;
import com.mediatek.rcse.service.ApiManager;
import com.mediatek.rcse.service.binder.FileStructForBinder;

import com.orangelabs.rcs.R;
import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.utils.PhoneUtils;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/**
* Provide message management related interface
*/
public class IpMessageManager extends MessageManager {
    private static final String TAG = "IpMessageManager";
    private static HashMap<Long, IpMessage> sCacheRcseMessage = new HashMap<Long, IpMessage>();
    private static HashMap<String, Long> sMessageMap = new HashMap<String, Long>();
    private HashMap<Long, PresentTextMessage> mPreSentMessageList = new HashMap<Long, PresentTextMessage>();
    private static final int SAVE_SUCCESS = 1;
    private static final int SAVE_FAIL = -1;
    private static final Random RANDOM = new Random();
    private static final int MESSAGE_TAG_RANGE = 1000;
    private static final String SPACE = " ";
    private static final int FAILED = 5;
    private String mRejected = null;
    private String mCanceled = null;
    private String mYou = null;
    private String mFailed = null;
    private String mWarningNoFtCapability = null;
    static final String COMMA = ",";
    private Handler mUiHandler = null;

    public IpMessageManager(Context context) {
        super(context);
        initializeStringInRcse();
        mUiHandler = new Handler(Looper.getMainLooper());
        Logger.d(TAG, "MessageManagerExt() entry ");
    }

    private void initializeStringInRcse() {
        mRejected = PluginUtils.getStringInRcse(R.string.file_transfer_rejected);
        mCanceled = PluginUtils.getStringInRcse(R.string.file_transfer_canceled);
        mYou = PluginUtils.getStringInRcse(R.string.file_transfer_you);
        mFailed = PluginUtils.getStringInRcse(R.string.file_transfer_failed);
        mWarningNoFtCapability = PluginUtils
                .getStringInRcse(R.string.warning_no_file_transfer_capability);
    }

    /*
     * Judge message is in the cache
     */
    public static boolean isInCache(Long messageIdInMms) {
        Logger.d(TAG, "isInCache() entry! messageIdInMms = " + messageIdInMms);
        synchronized (sCacheRcseMessage) {
            return (sCacheRcseMessage.containsKey(messageIdInMms));
        }
    }

    /**
     * Remove present message after it has been really sent out
     * @param messageTag The tag of the present message
     * @return TRUE if the message has been removed, otherwise FALSE
     */
    public boolean removePresentMessage(int messageTag) {
        synchronized (mPreSentMessageList) {
            Collection<PresentTextMessage> values = mPreSentMessageList.values();
            for (PresentTextMessage message : values) {
                if (messageTag == message.messageTag) {
                    Logger.d(TAG, "removePresentMessage() messageTag: " + messageTag + " found!");
                    values.remove(message);
                    return true;
                }
            }
            Logger.d(TAG, "removePresentMessage() messageTag: " + messageTag + " not found!");
            return false;
        }
    }

    /*
     * Judge message is in the cache
     */
    public static boolean isInCache(String messageIdInRcse) {
        Logger.d(TAG, "isInCache() entry! messageIdInRcse = " + messageIdInRcse);
        synchronized (sMessageMap) {
            return (sMessageMap.containsKey(messageIdInRcse));
        }
    }

    /*
     * Add message into cache, if it in cache, then update it
     */
    public static void addMessage(Long messageIdInMms, String messageIdInRcse, IpMessage message) {
        Logger.d(TAG, "addMessage() entry! messageIdInMms = " + messageIdInMms + " messageIdInRcse = " + messageIdInRcse
                + " message = " + message);
        synchronized (sCacheRcseMessage) {
            // If the map contains the key(messageIdInMms), will update the
            // value(message)
            sCacheRcseMessage.put(messageIdInMms, message);
        }

        synchronized (sMessageMap) {
            // First remove the entry which value contains messageIdInMms, then
            // add a new entry to the map for update the messageIdInRcse
            sMessageMap.values().remove(messageIdInMms);
            sMessageMap.put(messageIdInRcse, messageIdInMms);
        }
    }

    /*
     * Remove message into cache
     */
    public static void removeMessage(Long messageIdInMms, String messageIdInRcse) {
        Logger.d(TAG, "removeMessage() entry! messageIdInMms = " + messageIdInMms + " messageIdInRcse = " + messageIdInRcse);
        synchronized (sCacheRcseMessage) {
            sCacheRcseMessage.remove(messageIdInMms);
        }
        synchronized (sMessageMap) {
            sMessageMap.remove(messageIdInRcse);
        }
    }

    /*
     * get message from cache
     */
    public static IpMessage getMessage(Long messageIdInMms) {
        synchronized (sCacheRcseMessage) {
            return sCacheRcseMessage.get(messageIdInMms);
        }
    }

    /*
     * get message id from cache
     */
    public static Long getMessageId(String messageIdInRcse) {
        synchronized (sMessageMap) {
            return sMessageMap.get(messageIdInRcse);
        }
    }

    public static void updateCache(String oldTag, FileStructForBinder fileStruct, String remote) {
        String newTag = fileStruct.fileTransferTag;
        Logger.d(TAG, "updateCache(), oldTag = " + oldTag + " newTag = " + newTag);
        long messageIdInMms = getMessageId(oldTag);
        int type = getMessage(messageIdInMms).getType();
        removeMessage(messageIdInMms, oldTag);
        IpMessage message = PluginUtils.exchangeIpMessage(type, fileStruct, remote);
        addMessage(messageIdInMms, newTag, message);
    }

    @Override
    public int getStatus(long msgId) {
        Logger.d(TAG, "getStatus() entry with msgId " + msgId);
        Status status = null;
        IpMessage message = getMessage(msgId);
        if (message == null) {
            Logger.e(TAG, "getStatus(), message is null!");
            return 0;
        }
        if (message instanceof PluginIpTextMessage) {
            status = ((PluginIpTextMessage) message).getMessageStatus();
            return convertToMmsStatus(status);
        } else {
            Logger.w(TAG, "getStatus() ipMessage is " + message);
            int messageType = message.getType();
            switch (messageType) {
                case IpMessageConsts.IpMessageType.PICTURE:
                    if (message instanceof PluginIpImageMessage) {
                        return ((PluginIpImageMessage) message).getStatus();
                    } else {
                        return IpMessageConsts.IpMessageStatus.MO_INVITE;
                    }
                case IpMessageConsts.IpMessageType.VIDEO:
                    if (message instanceof PluginIpVideoMessage) {
                        return ((PluginIpVideoMessage) message).getStatus();
                    } else {
                        return IpMessageConsts.IpMessageStatus.MO_INVITE;
                    }
                case IpMessageConsts.IpMessageType.VOICE:
                    if (message instanceof PluginIpVoiceMessage) {
                        return ((PluginIpVoiceMessage) message).getStatus();
                    } else {
                        return IpMessageConsts.IpMessageStatus.MO_INVITE;
                    }
                case IpMessageConsts.IpMessageType.VCARD:
                    if (message instanceof PluginIpVcardMessage) {
                        return ((PluginIpVcardMessage) message).getStatus();
                    } else {
                        return IpMessageConsts.IpMessageStatus.MO_INVITE;
                    }
                default:
                    return ((PluginIpAttachMessage) message).getStatus();
            }
        }
    }

    /*
     * Get the ip message information
     */
    public IpMessage getIpMsgInfo(long msgId) {
        Logger.d(TAG, "getIpMsgInfo() msgId = " + msgId);
        IpMessage ipMessage = getMessage(msgId);
        if (ipMessage != null) {
            Logger.d(TAG, "getIpMsgInfo() found in message cache");
            return ipMessage;
        } else {
            Logger.e(TAG, "Can not find nessage in the cache!");
            synchronized (mPreSentMessageList) {
                ipMessage = mPreSentMessageList.get(msgId);
            }
            if (ipMessage != null) {
                Logger.d(TAG, "getIpMsgInfo() found in present message list");
                return ipMessage;
            }
            Logger.e(TAG, "getIpMsgInfo() cannot find this message, forget to add it into cache?");
            return null;
        } 
    }
    
    /**
     * Convert status in RCSe to the status corresponding in Mms
     * 
     * @param status The status in RCSe
     * @return status in Mms
     */
    private int convertToMmsStatus(Status status) {
        Logger.d(TAG, "convertToMmsStatus() entry with status is " + status);
        int statusInMms = IpMessageConsts.IpMessageStatus.FAILED;
        if (status == null) {
            return IpMessageConsts.IpMessageStatus.INBOX;
        }
        switch (status) {
            case SENDING:
                statusInMms = IpMessageConsts.IpMessageStatus.OUTBOX;
                break;
            case DELIVERED:
                statusInMms = IpMessageConsts.IpMessageStatus.DELIVERED;
                break;
            case DISPLAYED:
                statusInMms = IpMessageConsts.IpMessageStatus.VIEWED;
                break;
            case FAILED:
                statusInMms = IpMessageConsts.IpMessageStatus.FAILED;
                break;
            default:
                statusInMms = IpMessageConsts.IpMessageStatus.FAILED;
                break;
        }
        Logger.d(TAG, "convertToMmsStatus() entry exit with statusInMms is " + statusInMms);
        return statusInMms;
    }
    
    /*
     * Sent an ip message
     */
    public int saveIpMsg(IpMessage msg, int sendMsgMode) {
        String contact = msg.getTo();
        if (TextUtils.isEmpty(contact)) {
            Logger.w(TAG, "saveIpMsg() invalid contact: " + contact);
            return SAVE_FAIL;
        }

        if (msg instanceof IpTextMessage) {
            String messageBody = ((IpTextMessage) msg).getBody();
            Logger.d(TAG, "saveIpMsg() send a text message: " + messageBody + " to contact: " + contact);
            return saveChatMsg(messageBody, contact);
        } else if (msg instanceof IpAttachMessage) {
            Logger.d(TAG, "saveIpMsg() send a file to contact: " + contact);
            return saveFileTransferMsg(msg, contact);
        } else {
            Logger.w(TAG, "saveIpMsg() unsupported ip message type");
            return SAVE_FAIL;
        }
    }

    /*
     * Get the transferring progress
     */
    @Override
    public int getDownloadProcess(long msgId) {
        Logger.d(TAG, "getDownloadProcess(), msgId = " + msgId);
        IpMessage message = getMessage(msgId);
        if (message == null) {
            Logger.e(TAG, "getDownloadProcess(), message is null!");
            return 0;
        }
        int messageType = message.getType();
        switch (messageType) {
            case IpMessageConsts.IpMessageType.PICTURE:
                return ((PluginIpImageMessage) message).getProgress();
            case IpMessageConsts.IpMessageType.VIDEO:
                return ((PluginIpVideoMessage) message).getProgress();
            case IpMessageConsts.IpMessageType.VOICE:
                return ((PluginIpVoiceMessage) message).getProgress();
            case IpMessageConsts.IpMessageType.VCARD:
                return ((PluginIpVcardMessage) message).getProgress();
            default:
                return ((PluginIpAttachMessage) message).getProgress();
        }
    }

    /*
     * Set transfer status when user click button in mms
     */
    @Override
    public void setIpMessageStatus(long msgId, int msgStatus) {
        Logger.d(TAG, "setIpMessageStatus(), msgId = " + msgId + " msgStatus = " + msgStatus);
        IpMessage message = getMessage(msgId);
        if (message == null) {
            Logger.e(TAG, "setIpMessageStatus(), message is null!");
            return;
        }
        int messageType = message.getType();
        String messageTag;
        // Get file transfer tag
        switch (messageType) {
            case IpMessageConsts.IpMessageType.PICTURE:
                messageTag = ((PluginIpImageMessage) message).getTag();
                break;
            case IpMessageConsts.IpMessageType.VIDEO:
                messageTag = ((PluginIpVideoMessage) message).getTag();
                break;
            case IpMessageConsts.IpMessageType.VOICE:
                messageTag = ((PluginIpVoiceMessage) message).getTag();
                break;
            case IpMessageConsts.IpMessageType.VCARD:
                messageTag = ((PluginIpVcardMessage) message).getTag();
                break;
            default:
                messageTag = ((PluginIpAttachMessage) message).getTag();
                break;
        }

        Logger.d(TAG, "setIpMessageStatus(), messageTag is " + messageTag);
        // Sent message to rcse controller
        Message controllerMessage = null;
        switch (msgStatus) {
            case IpMessageConsts.IpMessageStatus.MO_INVITE:
                controllerMessage = PluginController.obtainMessage(
                        ChatController.EVENT_FILE_TRANSFER_RESENT, message.getFrom(), messageTag);
                break;
            case IpMessageConsts.IpMessageStatus.MO_CANCEL:
                controllerMessage = PluginController.obtainMessage(
                        ChatController.EVENT_FILE_TRANSFER_CANCEL, message.getFrom(), messageTag);
                break;
            case IpMessageConsts.IpMessageStatus.MT_RECEIVING:
                controllerMessage = PluginController.obtainMessage(
                        ChatController.EVENT_FILE_TRANSFER_RECEIVER_ACCEPT, message.getFrom(),
                        messageTag);
                break;
            case IpMessageConsts.IpMessageStatus.MT_REJECT:
                controllerMessage = PluginController.obtainMessage(
                        ChatController.EVENT_FILE_TRANSFER_RECEIVER_REJECT, message.getFrom(),
                        messageTag);
                break;
            default:
                break;
        }

        if (controllerMessage == null) {
            Logger.e(TAG, "setIpMessageStatus(), controllerMessage is null!");
            return;
        }
        controllerMessage.sendToTarget();
    }

    private int saveChatMsg(String message, String contact) {
        if (TextUtils.isEmpty(message)) {
            Logger.e(TAG, "saveChatMsg() invalid message: " + message);
            return SAVE_FAIL;
        }
        Logger.d(TAG, "saveChatMsg() message: " + message + " , contact: " + contact);
        if (!contact.contains(COMMA)) {
            int messageTag = generateMessageTag();
            PresentTextMessage ipMessage = new PresentTextMessage(messageTag, contact, message);
            mPreSentMessageList.put(PluginUtils.storeMessageInMmsDb(messageTag, message, contact,
                    PluginUtils.OUTBOX_MESSAGE, 0), ipMessage);
            sentMessageViaRCSe(message, contact, messageTag);
        } else {
            Set<String> contactSet = collectMultiContact(contact);
            Logger.d(TAG, "saveChatMsg() send chat message to multi contact: " + contactSet);
            long threadId = Telephony.Threads.getOrCreateThreadId(AndroidFactory.getApplicationContext(), contactSet);
            for (String singleContact : contactSet) {
                int messageTag = generateMessageTag();
                sentMessageViaRCSe(message, singleContact, messageTag);
                PresentTextMessage ipMessage = new PresentTextMessage(messageTag, singleContact, message);
                mPreSentMessageList.put(PluginUtils.storeMessageInMmsDb(messageTag, message, singleContact,
                        PluginUtils.OUTBOX_MESSAGE, threadId), ipMessage);
            }
        }
        return SAVE_SUCCESS;
    }

    private void sentMessageViaRCSe(String message, String contact, int messageTag) {
        Logger.d(TAG, "sentMessageViaRCSe() message: " + message + " , contact: " + contact + ", messageTag: " + messageTag);
        Message controllerMessage = PluginController.obtainMessage(ChatController.EVENT_SEND_MESSAGE, PhoneUtils
                .formatNumberToInternational(contact), message);
        controllerMessage.arg1 = messageTag;
        controllerMessage.sendToTarget();
    }

    private int saveFileTransferMsg(IpMessage msg, String contact) {
        IpAttachMessage ipAttachMessage = ((IpAttachMessage) msg);
        String filePath = ipAttachMessage.getPath();
        if (TextUtils.isEmpty(filePath)) {
            Logger.e(TAG, "saveFileTransferMsg() invalid filePath: " + filePath);
            return SAVE_FAIL;
        }
        File file = new File(filePath);
        if (!file.exists()) {
            Logger.e(TAG, "saveFileTransferMsg() file does not exist: " + filePath);
            return SAVE_FAIL;
        }
        long fileSize = file.length();
        long maxFileSize = ApiManager.getInstance().getMaxSizeforFileThransfer();
        if (fileSize >= maxFileSize && maxFileSize != 0) {
            Logger.d(TAG, "saveFileTransferMsg() file is too large, file size is " + fileSize);
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(AndroidFactory.getApplicationContext(),
                            PluginUtils.getStringInRcse(R.string.large_file_repick_message),
                            Toast.LENGTH_SHORT).show();
                }
            });
            return SAVE_FAIL;
        }
        int index = filePath.lastIndexOf("/");
        String fileName = filePath.substring(index + 1);
        String fileTransferString = PluginUtils.getStringInRcse(R.string.file_transfer_title);
        if (!contact.contains(COMMA)) {
            ParcelUuid uuid = new ParcelUuid(UUID.randomUUID());
            long fileTransferIdInMms = PluginUtils.insertDatabase(fileTransferString, contact, Integer.MAX_VALUE,
                    PluginUtils.OUTBOX_MESSAGE);

            FileStructForBinder fileStructForBinder = new FileStructForBinder(filePath, fileName,
                    ipAttachMessage.getSize(), uuid, null);
            IpMessage ipMessage = PluginUtils.analysisFileType(contact, fileStructForBinder);
            addMessage(fileTransferIdInMms, uuid.toString(), ipMessage);
            sentFileViaRCSe(filePath, contact, uuid);
        } else {
            Set<String> contactSet = collectMultiContact(contact);
            Logger.d(TAG, "saveFileTransferMsg() send file to multi contact: " + contactSet);
            long threadId = Telephony.Threads.getOrCreateThreadId(AndroidFactory.getApplicationContext(), contactSet);
            for (String singleContact : contactSet) {
                ParcelUuid uuid = new ParcelUuid(UUID.randomUUID());
                long fileTransferIdInMms = PluginUtils.insertDatabase(fileTransferString, singleContact, Integer.MAX_VALUE,
                        PluginUtils.OUTBOX_MESSAGE, threadId);
                FileStructForBinder fileStructForBinder = new FileStructForBinder(filePath, fileName, ipAttachMessage
                        .getSize(), uuid, null);
                IpMessage ipMessage = PluginUtils.analysisFileType(contact, fileStructForBinder);
                addMessage(fileTransferIdInMms, uuid.toString(), ipMessage);
                sentFileViaRCSe(filePath, singleContact, uuid);
            }
        }
        return SAVE_SUCCESS;
    }

    private void sentFileViaRCSe(String filePath, String contact, ParcelUuid fileTransferTag) {
        Logger.d(TAG, "sentFileViaRCSe() filePath: " + filePath + " , contact: " + contact + " , fileTransferTag: "
                + fileTransferTag);
        Message controllerMessage = PluginController.obtainMessage(ChatController.EVENT_FILE_TRANSFER_INVITATION, contact,
                filePath);
        Bundle data = controllerMessage.getData();
        data.putParcelable(ModelImpl.SentFileTransfer.KEY_FILE_TRANSFER_TAG, fileTransferTag);
        controllerMessage.setData(data);
        controllerMessage.sendToTarget();
    }

    static Set<String> collectMultiContact(String contact) {
        String[] contacts = contact.split(COMMA);
        Set<String> contactSet = new TreeSet<String>();
        for (String singleContact : contacts) {
            contactSet.add(singleContact);
        }
        return contactSet;
    }

    @Override
    public void resendMessage(long msgId, int simId) {
        super.resendMessage(msgId, simId);
        resendMessage(msgId);
    }

    @Override
    public void resendMessage(long msgId) {
        Logger.d(TAG, "resendMessage() msgId + " + msgId);
        super.resendMessage(msgId);
        IpMessage msg = getIpMsgInfo(msgId);
        saveIpMsg(msg, 0);
    }

    @Override
    public String getIpMessageStatusString(long msgId) {
        Logger.d(TAG, "getIpMessageStatusString(), msgId = " + msgId);
        IpMessage message = getMessage(msgId);
        if (message == null) {
            Logger.e(TAG, "getIpMessageStatusString(), message is null!");
            return null;
        }
        int messageType = message.getType();
        String remote = ContactsListManager.getInstance().getDisplayNameByPhoneNumber(
                message.getFrom());
        int transferStatus;
        int rcsStatus;
        String fileName;
        // Get file transfer tag
        switch (messageType) {
            case IpMessageConsts.IpMessageType.PICTURE:
                fileName = ((PluginIpImageMessage) message).getName();
                transferStatus = ((PluginIpImageMessage) message).getStatus();
                rcsStatus = ((PluginIpImageMessage) message).getRcsStatus();
                break;
            case IpMessageConsts.IpMessageType.VIDEO:
                fileName = ((PluginIpVideoMessage) message).getName();
                transferStatus = ((PluginIpVideoMessage) message).getStatus();
                rcsStatus = ((PluginIpVideoMessage) message).getRcsStatus();
                break;
            case IpMessageConsts.IpMessageType.VOICE:
                fileName = ((PluginIpVoiceMessage) message).getName();
                transferStatus = ((PluginIpVoiceMessage) message).getStatus();
                rcsStatus = ((PluginIpVoiceMessage) message).getRcsStatus();
                if (transferStatus == IpMessageConsts.IpMessageStatus.MT_RECEIVED) {
                    if (((PluginIpVoiceMessage) message).getDuration() == 0) {
                        Logger.d(TAG, "getIpMessageStatusString(), need to get duration!");
                        ((PluginIpVoiceMessage) message).analysisAttribute();
                        Intent it = new Intent();
                        it.setAction(IpMessageConsts.IpMessageStatus.ACTION_MESSAGE_STATUS);
                        it.putExtra(IpMessageConsts.STATUS, transferStatus);
                        it.putExtra(IpMessageConsts.IpMessageStatus.IP_MESSAGE_ID, msgId);
                        IpNotificationsManager.notify(it);
                    }
                }
                break;
            case IpMessageConsts.IpMessageType.VCARD:
                fileName = ((PluginIpVcardMessage) message).getName();
                transferStatus = ((PluginIpVcardMessage) message).getStatus();
                rcsStatus = ((PluginIpVcardMessage) message).getRcsStatus();
                break;
            default:
                fileName = ((PluginIpAttachMessage) message).getName();
                transferStatus = ((PluginIpAttachMessage) message).getStatus();
                rcsStatus = ((PluginIpAttachMessage) message).getRcsStatus();
                break;
        }

        switch (transferStatus) {
            case IpMessageConsts.IpMessageStatus.MO_INVITE:
            case IpMessageConsts.IpMessageStatus.MO_SENDING:
            case IpMessageConsts.IpMessageStatus.MO_SENT:
            case IpMessageConsts.IpMessageStatus.MT_RECEIVING:
            case IpMessageConsts.IpMessageStatus.MT_RECEIVED:
                return fileName;
            case IpMessageConsts.IpMessageStatus.MO_REJECTED:
                return remote + SPACE + mRejected + SPACE + fileName;
            case IpMessageConsts.IpMessageStatus.MO_CANCEL:
                if (!PluginUtils.isFtSupportedInRcse(message.getFrom())) {
                    Logger.d(TAG, "getIpMessageStatusString() mWarningNoFtCapability "
                            + mWarningNoFtCapability);
                    return mWarningNoFtCapability;
                } else {
                    Logger.d(TAG, "getIpMessageStatusString() support ft");
                    return mYou + SPACE + mCanceled + SPACE + fileName;
                }
            case IpMessageConsts.IpMessageStatus.MT_CANCEL:
                if (rcsStatus == FAILED) {
                    return fileName + SPACE + mFailed;
                } else {
                    return remote + SPACE + mCanceled + SPACE + fileName;
                }
            case IpMessageConsts.IpMessageStatus.MT_INVITED:
                return fileName;
            case IpMessageConsts.IpMessageStatus.MT_REJECT:
                return mYou + SPACE + mRejected + SPACE + fileName;
            default:
                return null;
        }
    }

    private static int generateMessageTag() {
        int messageTag = RANDOM.nextInt(MESSAGE_TAG_RANGE) + 1;
        messageTag = Integer.MAX_VALUE - messageTag;
        return messageTag;
    }

    /**
     * Use this class to be the cache when a message is in present status
     */
    private static class PresentTextMessage extends IpTextMessage {
        public int messageTag;
        private PresentTextMessage(int chatMessageTag, String contact, String text) {
            this.messageTag = chatMessageTag;
            this.setBody(text);
            this.setType(IpMessageConsts.IpMessageType.TEXT);
        }
    }
}


