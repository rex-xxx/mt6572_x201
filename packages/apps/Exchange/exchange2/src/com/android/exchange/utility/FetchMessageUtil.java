/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2011. All rights reserved.
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
 */

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

package com.android.exchange.utility;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.zip.DeflaterInputStream;

import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.HttpStatus;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;
import android.text.TextUtils;

import com.android.emailcommon.Logging;
import com.android.emailcommon.internet.MimeUtility;
import com.android.emailcommon.provider.Account;
import com.android.emailcommon.provider.EmailContent;
import com.android.emailcommon.provider.EmailContent.Body;
import com.android.emailcommon.provider.EmailContent.Message;
import com.android.emailcommon.provider.Mailbox;
import com.android.emailcommon.service.EmailServiceStatus;
import com.android.exchange.Eas;
import com.android.exchange.EasResponse;
import com.android.exchange.EasSyncService;
import com.android.exchange.adapter.EmailSyncAdapter;
import com.android.exchange.adapter.EmailSyncAdapter.EasEmailSyncParser;
import com.android.exchange.adapter.Parser;
import com.android.exchange.adapter.Serializer;
import com.android.exchange.adapter.Tags;

/**
 * Implementation of server-side search for EAS using the EmailService API
 */
public class FetchMessageUtil {
    // This value indicate that ContentValue object size would
    // exceed binder share memory limit size.
    private static final int NEED_COMPRESS_BODY_SIZE = 500 * 1024;
    /**
     * Get body of the partial loaded message.
     * @param req the request (message id and response code)
     * @throws IOException
     */
    public static int fetchMessage(Context context, long msgId) throws IOException {
        // Retrieve the message and mailbox; punt if either are null
        int res = EasSyncService.EXIT_DONE;
        Message msg = Message.restoreMessageWithId(context, msgId);
        if (msg == null) {
            return EmailServiceStatus.MESSAGE_NOT_FOUND;
        }

        Account account = Account.restoreAccountWithId(context, msg.mAccountKey);
        if (account == null) {
            return EmailServiceStatus.REMOTE_EXCEPTION;
        }

        EasSyncService svc = EasSyncService.setupServiceForAccount(context, account);
        if (svc == null) {
            return EmailServiceStatus.REMOTE_EXCEPTION;
        }

        Mailbox mailbox = Mailbox.restoreMailboxWithId(context, msg.mMailboxKey);
        // Sanity check; account might have been deleted?
        if (mailbox == null) {
            return EmailServiceStatus.REMOTE_EXCEPTION;
        }
        svc.mMailbox = mailbox;
        svc.mAccount = account;
        Serializer s = new Serializer();
        s.start(Tags.ITEMS_ITEMS).start(Tags.ITEMS_FETCH)
        .data(Tags.ITEMS_STORE, "Mailbox");
        // If this is a search result, use the protocolSearchInfo field to get the
        // correct remote location
        if (!TextUtils.isEmpty(msg.mProtocolSearchInfo)) {
            Logging.d(Logging.LOG_TAG, "Fetch remote searched message: " + msg.mProtocolSearchInfo);
            s.data(Tags.SEARCH_LONG_ID, msg.mProtocolSearchInfo);
        } else {
            Logging.d(Logging.LOG_TAG, "Fetch local messages");
            s.data(Tags.SYNC_COLLECTION_ID, mailbox.mServerId)
            .data(Tags.SYNC_SERVER_ID, msg.mServerId);
        }

        s.start(Tags.ITEMS_OPTIONS);
        if (svc.mProtocolVersionDouble >= Eas.SUPPORTED_PROTOCOL_EX2007_DOUBLE) {
            s.start(Tags.BASE_BODY_PREFERENCE);
            s.data(Tags.BASE_TYPE, Eas.BODY_PREFERENCE_HTML);
            /// M: limited the body fetch size to avoid database or binder buffer OOM.
            s.data(Tags.BASE_TRUNCATION_SIZE,  String.valueOf(MimeUtility.FETCH_BODY_SIZE_LIMIT));
            s.end();
            Logging.d(Logging.LOG_TAG, "Add Sync commands options for EX2007");
        } else {
            s.data(Tags.SYNC_TRUNCATION, Eas.EAS2_5_TRUNCATION_SIZE);
        }
        s.end();
        s.end().end().done();

        int timeout = EasSyncService.COMMAND_TIMEOUT;
        if(Logging.DEBUG) {
            Logging.d(Logging.LOG_TAG, "send ItemOperations Fetch commond: " + s.toString());
        }
        EasResponse resp = svc.sendHttpClientPost("ItemOperations",
                new ByteArrayEntity(s.toByteArray()), timeout);
        try {
            int status = resp.getStatus();
            boolean success = (status == HttpStatus.SC_OK) ? true : false;
            if (status == HttpStatus.SC_OK) {
                if(Logging.DEBUG) {
                    Logging.d(Logging.LOG_TAG, "Fetch response ok");
                }
                InputStream is = resp.getInputStream();
                if (is != null) {
                    try {
                        ItemOperationsFetchParser iop = new ItemOperationsFetchParser(is, svc);
                        iop.parse();
                    } finally {
                        is.close();
                    }
                } else {
                    if(Logging.DEBUG) {
                        Logging.d(Logging.LOG_TAG, "Empty input stream in sync command response");
                    }
                }
            } else {
                if(Logging.DEBUG) {
                    Logging.d(Logging.LOG_TAG, "Sync response error: " + status);
                }
                if (EasResponse.isProvisionError(status)) {
                    res = EmailServiceStatus.SECURITY_FAILURE;
                } else if (EasResponse.isAuthError(status)) {
                    res = EmailServiceStatus.LOGIN_FAILED;
                } else {
                    res = EmailServiceStatus.CONNECTION_ERROR;
                }
            }
        } finally {
            resp.close();
        }
        return res;
    }

    static class ItemOperationsFetchParser extends Parser {
        private int mStatusCode = 0;
        private String mServerId = "";
        private String mProtocolSearchInfo = "";
        private final EasSyncService mService;

        public ItemOperationsFetchParser(InputStream in, EasSyncService service) 
                throws IOException {
            super(in);
            mService = service;
        }

        public int getStatusCode() {
            return mStatusCode;
        }

        private byte[] compressBodyData(String body) throws IOException{
            int readCount;
            int originSize;
            int compressedSize;
            byte[] sourceBytes;
            byte[] compressedBytes;
            byte[] readBuffer = new byte[1024];
            sourceBytes = body.getBytes();
            originSize = sourceBytes.length;
            DeflaterInputStream compressedStr
                    = new DeflaterInputStream(new ByteArrayInputStream(sourceBytes));
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            while ((readCount = compressedStr.read(readBuffer)) != -1) {
                outStream.write(readBuffer, 0, readCount);
            }
            compressedBytes = outStream.toByteArray();
            Logging.d(Logging.LOG_TAG,
                    "Compressing message body: originSize = "
                            + originSize + " compressedBytes = "
                            + compressedBytes.length);
            outStream.close();
            compressedStr.close();
            return compressedBytes;
        }

        private void parseProperties(EasEmailSyncParser parser,
                ArrayList<ContentProviderOperation> ops) throws IOException {
            Message msg = new Message();
            msg.mAccountKey = mService.mAccount.mId;
            msg.mMailboxKey = mService.mMailbox.mId;
            msg.mFlagLoaded = Message.FLAG_LOADED_COMPLETE;
            //parser.pushTag(tag);
            parser.addData(msg, tag);
            msg.mSize += mServerId.length();
            msg.mSize += mProtocolSearchInfo.length();
            /// M: if the message's flagLoaded was still FLAG_LOADED_PARTIAL, that indicated this message's body was
            /// too large.
            if (msg.mFlagLoaded == Message.FLAG_LOADED_PARTIAL) {
                msg.mFlags |= Message.FLAG_BODY_TOO_LARGE;
                msg.mFlagLoaded = Message.FLAG_LOADED_COMPLETE;
            }

            // Find the original message's id (by serverId or longId and mailbox)
            Cursor c = null;
            if (!TextUtils.isEmpty(mProtocolSearchInfo)) {
                Logging.d(Logging.LOG_TAG,
                        "Fetched message of longId: " + mProtocolSearchInfo);
                c = parser.getLongIdCursor(mProtocolSearchInfo, EmailContent.ID_PROJECTION);
            } else {
                c = parser.getServerIdCursor(mServerId, EmailContent.ID_PROJECTION);
            }
            String id = null;
            try {
                if (c.moveToFirst()) {
                    id = c.getString(EmailContent.ID_PROJECTION_COLUMN);
                }
            } finally {
                c.close();
            }

            boolean isNeedCompress = false;
            // Create and save the body
            ContentValues cv = new ContentValues();
            if (msg.mText != null) {
                cv.put(Body.TEXT_CONTENT, msg.mText);
                if (msg.mText.length() > NEED_COMPRESS_BODY_SIZE) {
                    isNeedCompress = true;
                }
            }
            if (msg.mHtml != null) {
                cv.put(Body.HTML_CONTENT, msg.mHtml);
                if (msg.mHtml.length() > NEED_COMPRESS_BODY_SIZE) {
                    isNeedCompress = true;
                }
            }
            if (msg.mTextReply != null) {
                cv.put(Body.TEXT_REPLY, msg.mTextReply);
            }
            if (msg.mHtmlReply != null) {
                cv.put(Body.HTML_REPLY, msg.mHtmlReply);
            }
            if (msg.mSourceKey != 0) {
                cv.put(Body.SOURCE_MESSAGE_KEY, msg.mSourceKey);
            }
            if (msg.mIntroText != null) {
                cv.put(Body.INTRO_TEXT, msg.mIntroText);
            }
            msg.mId = Long.valueOf(id);
            // If we find one, we do two things atomically: 1) set the body text for the
            // message, and 2) mark the message loaded (i.e. completely loaded)
            if (id != null) {
                Logging.d(Logging.LOG_TAG, "Fetched body successfully for " + id
                        + " with flag: " + msg.mFlagLoaded);
                String[] msgKey = new String[1];
                msgKey[0] = id;

                if (isNeedCompress) {
                    if (msg.mText != null) { 
                        cv.put(Body.TEXT_CONTENT, compressBodyData(msg.mText));
                    }
                    if (msg.mHtml != null) { 
                        cv.put(Body.HTML_CONTENT, compressBodyData(msg.mHtml));
                    }
                    ops.add(ContentProviderOperation.newUpdate(Body.CONTENT_LARGE_URI)
                            .withSelection(Body.MESSAGE_KEY + "=?", msgKey)
                            .withValues(cv)
                            .build());
                } else {
                    ops.add(ContentProviderOperation.newUpdate(Body.CONTENT_URI)
                            .withSelection(Body.MESSAGE_KEY + "=?", msgKey)
                            .withValues(cv)
                            .build());
                }
                /// M: restore the message to avoid overwrite message flags.
                Message localMessage = Message.restoreMessageWithId(mService.mContext, msg.mId);
                msg.mFlags |= localMessage.mFlags;
                // commit fetched mails via EAS here to trigger MessageView reload
                if (msg.mFlagLoaded == Message.FLAG_LOADED_COMPLETE) {
                    ops.add(ContentProviderOperation.newUpdate(Message.CONTENT_URI)
                            .withSelection(EmailContent.RECORD_ID + "=?", msgKey)
                            .withValue(Message.FLAG_LOADED, Message.FLAG_LOADED_COMPLETE)
                            .withValue(Message.SIZE, msg.mSize)
                             /// M: add for large body message.
                            .withValue(Message.FLAGS, msg.mFlags)
                            .build());
                }
            }
        }

        private void parseFetch() throws IOException {
            EmailSyncAdapter adapter = new EmailSyncAdapter(mService);
            EasEmailSyncParser parser = adapter.new EasEmailSyncParser(this, adapter);
            ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
            while (nextTag(Tags.ITEMS_FETCH) != END) {
                if (tag == Tags.ITEMS_STATUS) {
                    String status = getValue();
                    Logging.d(Logging.LOG_TAG, "ITEMS_STATUS:" + status);
                } else if (tag == Tags.SYNC_SERVER_ID) {
                    mServerId = getValue();
                } else if (tag == Tags.SEARCH_LONG_ID) {
                    mProtocolSearchInfo = getValue();
                    Logging.d(Logging.LOG_TAG, "ITEMOPERATIONS_FETCH LONGID: " + mProtocolSearchInfo);
                } else if (tag == Tags.ITEMS_PROPERTIES) {
                    parseProperties(parser, ops);
                } else {
                    skipTag();
                }
            }

            try {
                ContentProviderResult[] results = adapter.mContentResolver.applyBatch(EmailContent.AUTHORITY, ops);
                Logging.d(Logging.LOG_TAG, "ITEMS_FETCH Save successfully for " + ops.size()
                         + " operations.");
                for (ContentProviderResult result : results) {
                    Logging.d(Logging.LOG_TAG, "ITEMS_FETCH Save successfully: " + result.toString());
                }
            } catch (RemoteException e) {
                Logging.d(Logging.LOG_TAG, "RemoteException while saving search results.");
            } catch (OperationApplicationException e) {
                Logging.d(Logging.LOG_TAG, "OperationApplicationException while saving search results.");
            }
        }

        private void parseResponse() throws IOException {
            while (nextTag(Tags.ITEMS_RESPONSE) != END) {
                if (tag == Tags.ITEMS_FETCH) {
                    parseFetch();
                } else {
                    skipTag();
                }
            }
        }

        @Override
        public boolean parse() throws IOException {
            boolean res = false;
            if (nextTag(START_DOCUMENT) != Tags.ITEMS_ITEMS) {
                throw new IOException();
            }
            while (nextTag(START_DOCUMENT) != END_DOCUMENT) {
                if (tag == Tags.ITEMS_STATUS) {
                    // Save the status code
                    mStatusCode = getValueInt();
                    Logging.d(Logging.LOG_TAG, "ITEMS_STATUS status: " + mStatusCode);
                } else if (tag == Tags.ITEMS_RESPONSE) {
                    parseResponse();
                } else {
                    skipTag();
                }
            }
            return res;
        }
    }
}
