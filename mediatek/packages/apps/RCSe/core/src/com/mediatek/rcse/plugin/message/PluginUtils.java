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

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.media.MediaFile;
import android.net.Uri;
import android.provider.Telephony;
import android.provider.Telephony.Sms;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import com.mediatek.mms.ipmessage.IpMessageConsts;
import com.mediatek.mms.ipmessage.message.IpMessage;
import com.mediatek.rcse.activities.widgets.AsyncGalleryView;
import com.mediatek.rcse.api.Logger;
import com.mediatek.rcse.interfaces.ChatController;
import com.mediatek.rcse.service.CoreApplication;
import com.mediatek.rcse.service.PluginApiManager;
import com.mediatek.rcse.service.Utils;
import com.mediatek.rcse.service.binder.FileStructForBinder;
import com.mediatek.rcse.service.binder.ThreadTranslater;

import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.provider.eventlogs.EventLogData;
import com.orangelabs.rcs.provider.messaging.RichMessagingData;
import com.orangelabs.rcs.utils.PhoneUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Plugin Utils
 */
public class PluginUtils {

    private static final String TAG = "PluginUtils";
    private static final String TYPE = "type";
    private static final String OUTBOX_URI = "content://sms/outbox";
    private static final String INBOX_URI = "content://sms/inbox";
    private static final String CONTENT_URI = "content://sms/";
    private static final String FILE_SCHEMA = "file://";
    private static final String[] PROJECTION_WITH_THREAD = {EventLogData.KEY_EVENT_ROW_ID, Sms.THREAD_ID, Sms.ADDRESS};
    private static final String[] PROJECTION_ONLY_ID = {EventLogData.KEY_EVENT_ROW_ID};
    private static final String SELECTION_RELOAD_IP_MSG = Sms.IPMSG_ID + ">?";
    private static final String SELECTION_IP_MSG_ID = Sms.IPMSG_ID + "=?";
    public static final int INBOX_MESSAGE = 1;
    public static final int OUTBOX_MESSAGE = 2;
    public static final int DEFAULT_MESSAGE_ID = 0x100;
    public static final long DUMMY_SIM_ID = 1L;
    private static long sSimId = DUMMY_SIM_ID;
    public static final int STATUS_IS_READ = 1;
    public static final int STATUS_IS_NOT_READ = 0;
    public static final int SIZE_K = 1024;
    public static final Uri SMS_CONTENT_URI = Uri.parse("content://sms");

    /**
     * Insert a new item into Mms DB
     * @param body The body of this item
     * @param contact The address of this item
     * @param ipMsgId The ipmsg_id of this item
     * @param messageType The type of this item
     * @return The id in Mms DB
     */
    public static Long insertDatabase(String body, String contact, int ipMsgId, int messageType) {
        return insertDatabase(body, contact, ipMsgId, messageType, 0);
    }

    static Long insertDatabase(String body, String contact, int ipMsgId, int messageType, long targetThreadId) {
        Logger.d(TAG, "InsertDatabase(), body = " + body + "contact is " + contact + " , messageType: " + messageType
                + " , threadId: " + targetThreadId);
        Long messageIdInMms = Long.MIN_VALUE;
        String messageUri = null;
        ContentValues cv = new ContentValues();

        cv.put(Sms.ADDRESS, contact);
        cv.put(Sms.BODY, body);
        cv.put(Sms.IPMSG_ID, ipMsgId);
        cv.put(Sms.SIM_ID, sSimId);
        if (targetThreadId > 0) {
            cv.put(Sms.THREAD_ID, targetThreadId);
        }
        ContentResolver contentResolver = AndroidFactory.getApplicationContext().getContentResolver();

        if (messageType == INBOX_MESSAGE) {
            cv.put(TYPE, INBOX_MESSAGE);
            Uri inboxUri = Uri.parse(INBOX_URI);
            messageUri = contentResolver.insert(inboxUri, cv).toString();
        } else if (messageType == OUTBOX_MESSAGE) {
            cv.put(TYPE, OUTBOX_MESSAGE);
            Uri outboxUri = Uri.parse(OUTBOX_URI);
            messageUri = contentResolver.insert(outboxUri, cv).toString();
        }

        if (null == messageUri) {
            Logger.w(TAG, "InsertDatabase() messageUri is null");
            return -1L;
        }
        if (contact.startsWith(PluginGroupChatWindow.GROUP_CONTACT_STRING_BEGINNER)
                && !ThreadTranslater.tagExistInCache(contact)) {
            cacheThreadIdForGroupChat(contact, contentResolver);
        } else {
            Logger.d(TAG, "InsertDatabase() contact not start group identify or not cached");
        }

        String messageId = messageUri.replace(CONTENT_URI, "");
        Logger.d(TAG, "InsertDatabase(), messageId = " + messageId);
        messageIdInMms = Long.valueOf(messageId);

        Logger.d(TAG, "InsertDatabase(), messageIdInMms = " + messageIdInMms);
        return messageIdInMms;
    }

    private static boolean cacheThreadIdForGroupChat(String contact, ContentResolver contentResolver) {
        Cursor cursor = null;
        try {
            String[] args = {
                contact
            };
            String[] projection = {
                Sms.THREAD_ID
            };
            cursor = contentResolver.query(SMS_CONTENT_URI, projection, Sms.ADDRESS + "=?",
                    args, null);

            if (cursor.moveToFirst()) {
                Long threadId = cursor.getLong(cursor.getColumnIndex(Sms.THREAD_ID));
                Logger.d(TAG, "InsertDatabase() the contact is " + contact + ", threadId is " + +threadId);
                String tag = ThreadTranslater.translateThreadId(threadId);
                if (tag == null) {
                    ThreadTranslater.saveThreadandTag(threadId, contact);
                    return true;
                } else {
                    Logger.d(TAG, "InsertDatabase() the thread to tag exist " + tag);
                }
            } else {
                Logger.e(TAG, "InsertDatabase() the cursor.moveToFirst() is false");
            }

        } finally {
            if (null != cursor) {
                cursor.close();
            }
        }
        return false;
    }

    /*
     * Store a message into mms database
     */
    public static Long storeMessageInDatabase(String messageId, String text, String remote, int messageType) {
        Logger.d(TAG, "storeMessageInDatabase() with messageId = " + messageId + " ,text = " + text
                + " ,remote = " + remote + " ,and messageType = " + messageType);
        int ipMsgId = findIdInRcseDb(messageId);
        if (-1 < ipMsgId) {
            return storeMessageInMmsDb(ipMsgId, text, remote, messageType, 0);
        } else {
            Logger.w(TAG, "storeMessageInDatabase() message not found in Rcse DB");
            return -1L;
        }
    }

    static long storeMessageInMmsDb(int ipMsgId, String text, String remote, int messageType, long threadId) {
            long idInMmsDb = getIdInMmsDb(ipMsgId);
            if (-1 != idInMmsDb) {
                Logger.d(TAG, "storeMessageInDatabase() message found in Mms DB, id: " + idInMmsDb);
                return idInMmsDb;
            } else {
                Logger.d(TAG, "storeMessageInDatabase() message not found in Mms DB");
                if (TextUtils.isEmpty(remote)) {
                    Logger.w(TAG, "storeMessageInDatabase() invalid remote: " + remote);
                    return -1L;
                }
                if (remote.startsWith(PluginGroupChatWindow.GROUP_CONTACT_STRING_BEGINNER)) {
                    return insertDatabase(text, remote, ipMsgId, messageType, threadId);
                } else {
                    final String contact = PhoneUtils.extractNumberFromUri(remote);
                    return insertDatabase(text, contact, ipMsgId, messageType, threadId);
                }
            }
    }

    static boolean updatePreSentMessageInMmsDb(String messageId, int messageTag) {
        int ipMsgId = findIdInRcseDb(messageId);
        if (-1 < ipMsgId) {
            ContentResolver contentResolver = AndroidFactory.getApplicationContext().getContentResolver();
            ContentValues values = new ContentValues();
            values.put(Sms.IPMSG_ID, ipMsgId);
            int count = contentResolver.update(SMS_CONTENT_URI, values, Sms.IPMSG_ID + "=?", new String[] {
                Integer.toString(messageTag)
            });
            Logger.d(TAG, "updatePreSentMessageInMmsDb() messageId: " + messageId + " ,messageTag: " + messageTag
                    + " ,count: " + count);
            return count == 1;
        } else {
            Logger.w(TAG, "updatePreSentMessageInMmsDb() ");
            return false;
        }
    }

    static long getIdInMmsDb(int ipMsgId) {
        Logger.d(TAG, "getIdInMmsDb() entry, ipMsgId: " + ipMsgId);
        ContentResolver contentResolver = AndroidFactory.getApplicationContext().getContentResolver();

        Cursor cursor = null;
        try {
            final String[] args = {Integer.toString(ipMsgId)};
            cursor = contentResolver.query(SMS_CONTENT_URI, PROJECTION_WITH_THREAD, SELECTION_IP_MSG_ID, args, null);
            if (cursor.moveToFirst()) {
                long mmsDbId = cursor.getLong(cursor.getColumnIndex(Sms._ID));
                long threadId = cursor.getLong(cursor.getColumnIndex(Sms.THREAD_ID));
                String contact = cursor.getString(cursor.getColumnIndex(Sms.ADDRESS));
                Logger.d(TAG, "getIdInMmsDb() contact is " + contact + " threadId is " + threadId);
                if (contact != null
                        && contact.startsWith(PluginGroupChatWindow.GROUP_CONTACT_STRING_BEGINNER)) {
                    String tag = ThreadTranslater.translateThreadId(threadId);
                    if (tag == null) {
                        Logger.d(TAG, "getIdInMmsDb() the thread to tag not exist ");
                        ThreadTranslater.saveThreadandTag(threadId, contact);
                    } else {
                        Logger.d(TAG, "getIdInMmsDb() the thread to tag exist " + tag);
                    }
                }
                Logger.d(TAG, "getIdInMmsDb() mmsDbId: " + mmsDbId);
                return mmsDbId;
            } else {
                Logger.d(TAG, "getIdInMmsDb() empty cursor");
                return -1l;
            }
        } finally {
            if (null != cursor) {
                cursor.close();
            }
        }
    }

    static int findIdInRcseDb(String msgId) {
        Logger.d(TAG, "findIdInRcseDb() entry, msgId: " + msgId);
        ContentResolver contentResolver = AndroidFactory.getApplicationContext().getContentResolver();

        if (TextUtils.isEmpty(msgId)) {
            Logger.e(TAG, "findIdInRcseDb(), invalid msgId: " + msgId);
            return DEFAULT_MESSAGE_ID;
        }
        String[] argument = {msgId};
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(RichMessagingData.CONTENT_URI, 
                    PROJECTION_ONLY_ID, EventLogData.KEY_EVENT_MESSAGE_ID + "=?", argument, null);
            if (null != cursor && cursor.moveToFirst()) {
                int rowId =  cursor.getInt(cursor.getColumnIndex(EventLogData.KEY_EVENT_ROW_ID));
                Logger.d(TAG, "findIdInRcseDb() row id for message: " + msgId + ", is " + rowId);
                return rowId;
            } else {
                Logger.w(TAG, "findIdInRcseDb() invalid cursor: " + cursor);
            }
        } finally {
            if (null != cursor) {
                cursor.close();
            }
        }
        return -1;
    }

    /*package*/ static void reloadRcseMessages() {
        Logger.d(TAG, "reloadRcseMessages() entry");
        PluginGroupChatWindow.removeAllGroupChatInvitationsInMms();
        
        ArrayList<Integer> messageIdArray = new ArrayList<Integer>();
        ContentResolver contentResolver = AndroidFactory.getApplicationContext().getContentResolver();
        Cursor cursor = null;
        ConcurrentHashMap<String, ArrayList<Integer>> groupMap = new ConcurrentHashMap<String, ArrayList<Integer>>();
        try {
            final String[] args = {"0"};
            cursor = contentResolver.query(SMS_CONTENT_URI, null, SELECTION_RELOAD_IP_MSG, args, null);
            if (cursor.moveToFirst()) {
                do {
                    int id = cursor.getInt(cursor.getColumnIndex(Sms.IPMSG_ID));
                    String address = cursor.getString(cursor.getColumnIndex(Sms.ADDRESS));
                    Logger.d(TAG, "reloadRcseMessages() ipMsgId: " + id + " , address: " + address);
                    if (address != null
                            && address
                                    .startsWith(PluginGroupChatWindow.GROUP_CONTACT_STRING_BEGINNER)) {
                        if (groupMap.containsKey(address)) {
                            ArrayList<Integer> messageList = groupMap.get(address);
                            messageList.add(id);
                        } else {
                            ArrayList<Integer> messageList = new ArrayList<Integer>();
                            messageList.add(id);
                            groupMap.put(address, messageList);
                        }
                    } else {
                        if (id == Integer.MAX_VALUE || id == DEFAULT_MESSAGE_ID) {
                            Logger.d(TAG, "reloadRcseMessages(), it's a pending file transfer or invalid message," +
                                    " no need to rebuild!");
                            int smsId = cursor.getInt(cursor.getColumnIndex(Sms._ID));
                            contentResolver.delete(Uri.parse(SMS_CONTENT_URI + "/" + smsId), null,
                                    null);
                        } else {
                            Logger.d(TAG, "reloadRcseMessages(), need to rebuild!");
                            messageIdArray.add(id);
                        }
                    }
                } while (cursor.moveToNext());
            } else {
                Logger.d(TAG, "reloadRcseMessages() cursor is empty");
            }
        } finally {
            if (null != cursor) {
                cursor.close();
            }
        }
        Logger.i(TAG, "reloadRcseMessages() " + messageIdArray.size() + " Rcse message found: "
                + messageIdArray);
        PluginController.obtainMessage(ChatController.EVENT_RELOAD_MESSAGE, messageIdArray)
                .sendToTarget();
        Set<Entry<String, ArrayList<Integer>>> tagSet = groupMap.entrySet();
        for (Entry<String, ArrayList<Integer>> tag : tagSet) {
            ArrayList<Integer> messageList = tag.getValue();
            Logger.i(TAG, "reloadRcseMessages() tag is  " + tag + " messageList: " + messageList);
            PluginController.obtainMessage(ChatController.EVENT_RELOAD_MESSAGE, tag.getKey(),
                    messageList).sendToTarget();
        }
    }

    static int updateMessageIdInMmsDb(String oldId, String newId) {
        Logger.d(TAG, "updateMessageIdInMmsDb() entry");
        int idInRcse = findIdInRcseDb(newId);
        long idInMms = IpMessageManager.getMessageId(oldId);
        ContentResolver contentResolver = AndroidFactory.getApplicationContext().getContentResolver();
        if (contentResolver != null && idInMms != -1) {
            Uri uri = Uri.parse(SMS_CONTENT_URI + "/" + idInMms);
            ContentValues contentValues = new ContentValues();
            contentValues.put(Sms.IPMSG_ID, idInRcse);
            contentResolver.update(uri, contentValues, null, null);
        } else {
            Logger.e(TAG, "getIdInMmsDb(), cr is null!");
        }
        return idInRcse;
    }

    static boolean onViewFileDetials(String filePath, Context context) {
        Intent intent = generateFileDetailsIntent(filePath);
        if (intent != null) {
            context.startActivity(intent);
            return true;
        } else {
            Logger.w(TAG, "onViewFileDetials() intent is null");
            return false;
        }
    }

    private static Intent generateFileDetailsIntent(String filePath) {
        Logger.d(TAG, "generateFileDetailsIntent() entry, filePath: " + filePath);
        if (!TextUtils.isEmpty(filePath)) {
            Uri fileUri;
            if (!filePath.startsWith(FILE_SCHEMA)) {
                fileUri = Uri.parse(FILE_SCHEMA + filePath);
            } else {
                fileUri = Uri.parse(filePath);
            }
            String mimeType = AsyncGalleryView.getMimeType(filePath);
            if (null != mimeType) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(fileUri, mimeType);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                return intent;
            } else {
                Logger.e(TAG, "generateFileDetailsIntent() mMimeType is null");
            }
        }
        return null;
    }

    /**
     * Initialize sim id from Telephony
     * 
     * @param context Any context will be ok
     */
    public static void initializeSimIdFromTelephony(Context context) {
        Logger.d(TAG, "initializeSimIdFromTelephony() entry");
        int slotCount = getSimSlotCount(context);
        if (slotCount > 0) {
            List<Telephony.SIMInfo> simInfos = Telephony.SIMInfo.getAllSIMList(context);
            int currentSlot = 0;
            while (currentSlot < slotCount) {
                Telephony.SIMInfo simInfo = simInfos.get(0).getSIMInfoBySlot(context, currentSlot);
                if (simInfo != null) {
                    sSimId = simInfo.mSimId;
                    Logger.d(TAG, "initializeSimIdFromTelephony() slot " + currentSlot
                            + " simId is " + sSimId);
                    break;
                } else {
                    Logger.d(TAG, "initializeSimIdFromTelephony() slot " + currentSlot
                            + " simInfo is null");
                }
                currentSlot += 1;
            }
        } else {
            Logger.e(TAG, "initializeSimIdFromTelephony() simCount must more than 0, now is "
                    + slotCount);
        }
        // Error handle if get wrong sim id
        if (sSimId <= 0) {
            Logger.e(TAG, "initializeSimIdFromTelephony() get wrong simId");
            sSimId = DUMMY_SIM_ID;
        }
        Logger.d(TAG, "initializeSimIdFromTelephony() exit simId is " + sSimId);
    }

    /**
     * Get simcard slot count
     * 
     * @param context any context will be ok
     * @return the number of slot count
     */
    private static int getSimSlotCount(Context context) {
        int simCount = Telephony.SIMInfo.getAllSIMCount(context);
        Logger.d(TAG, "getSimSlotCount() simCount is " + simCount);
        return simCount;
    }
    

    /**
     * Return the string value associated with a particular resource ID in RCSe.
     * 
     * @param resourceId The desired resource identifier, as generated by the
     *            aapt tool.
     * @return The string data associated with the resource, stripped of styled
     *         text information.
     */
    public static String getStringInRcse(int resourceId) {
        Resources resource = null;
        String string = null;
        try {
            resource =
                    AndroidFactory.getApplicationContext().getPackageManager()
                            .getResourcesForApplication(CoreApplication.APP_NAME);
        } catch (android.content.pm.PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if (null != resource) {
            string = resource.getString(resourceId);
        } else {
            Logger.e(TAG, "getStringInRcse(), resource is null!");
        }
        return string;
    }
    
    /**
     * Return whether file transfer is supported.
     * 
     * @param number The number whose capability is to be queried.
     * @return True if file transfer is supported, else false.
     */
    public static boolean isFtSupportedInRcse(String contact) {
        Logger.d(TAG, "isFtSupportedInRcse() entry contact is " + contact);
        boolean support = false;
        PluginApiManager apiManager = PluginApiManager.getInstance();
        if (null != apiManager) {
            if (apiManager.isFtSupported(contact)) {
                support = true;
            } else {
                support = false;
            }
        } else {
            support = false;
        }
        Logger.d(TAG, "isFtSupportedInRcse() exit support is " + support);
        return support;
    }


    /**
     * Return the file transfer IpMessage
     * 
     * @param remote remote user
     * @param FileStructForBinder file strut
     * @return The file transfer IpMessage
     */
    public static IpMessage analysisFileType(String remote, FileStructForBinder fileTransfer) {
        String fileName = fileTransfer.fileName;
        if (fileName != null) {
            String mimeType = MediaFile.getMimeTypeForFile(fileName);
            if (mimeType == null) {
                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                        Utils.getFileExtension(fileName));
            }
            if (mimeType != null) {
                if (mimeType.contains(Utils.FILE_TYPE_IMAGE)) {
                    return new PluginIpImageMessage(fileTransfer, remote);
                } else if (mimeType.contains(Utils.FILE_TYPE_AUDIO)
                        || mimeType.contains("application/ogg")) {
                    return new PluginIpVoiceMessage(fileTransfer, remote);
                } else if (mimeType.contains(Utils.FILE_TYPE_VIDEO)) {
                    return new PluginIpVideoMessage(fileTransfer, remote);
                } else if (fileName.toLowerCase().endsWith(".vcf")) {
                    return new PluginIpVcardMessage(fileTransfer, remote);
                } else {
                    // Todo
                    Logger.d(TAG, "analysisFileType() other type add here!");
                }
            }
        } else {
            Logger.w(TAG, "analysisFileType(), file name is null!");
        }
        return new PluginIpAttachMessage(fileTransfer, remote);
    }

    /**
     * Return the file transfer IpMessage
     * 
     * @param IpMessage Ipmessage in mms defined
     * @return IpMessage Ipmessage in plugin defined
     */
    public static IpMessage exchangeIpMessage(int messageType, FileStructForBinder fileStruct,
            String remote) {
        String newTag = fileStruct.fileTransferTag;
        Logger.d(TAG, "exchangeIpMessage() entry, newTag = " + newTag);
        switch (messageType) {
            case IpMessageConsts.IpMessageType.PICTURE:
                PluginIpImageMessage ipImageMessage = new PluginIpImageMessage(fileStruct, remote);
                ipImageMessage.setTag(newTag);
                return ipImageMessage;
            case IpMessageConsts.IpMessageType.VOICE:
                PluginIpVoiceMessage ipVoiceMessage = new PluginIpVoiceMessage(fileStruct, remote);
                ipVoiceMessage.setTag(newTag);
                return ipVoiceMessage;
            case IpMessageConsts.IpMessageType.VIDEO:
                PluginIpVideoMessage ipVideoMessage = new PluginIpVideoMessage(fileStruct, remote);
                ipVideoMessage.setTag(newTag);
                return ipVideoMessage;
            case IpMessageConsts.IpMessageType.VCARD:
                PluginIpVcardMessage ipVcardMessage = new PluginIpVcardMessage(fileStruct, remote);
                ipVcardMessage.setTag(newTag);
                return ipVcardMessage;
            default:
                PluginIpAttachMessage ipAttachMessage = new PluginIpAttachMessage(fileStruct,
                        remote);
                ipAttachMessage.setTag(newTag);
                return ipAttachMessage;
        }
    }
}
