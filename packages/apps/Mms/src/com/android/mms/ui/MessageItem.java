/*
 * Copyright (C) 2008 Esmertec AG.
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

package com.android.mms.ui;

import java.util.regex.Pattern;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony.Mms;
import android.provider.Telephony.MmsSms;
import android.provider.Telephony.Sms;
import android.text.TextUtils;
import android.util.Log;

import com.android.mms.LogTag;
import com.android.mms.MmsApp;
import com.android.mms.R;
import com.android.mms.data.Contact;
import com.android.mms.model.SlideModel;
import com.android.mms.model.SlideshowModel;
import com.android.mms.model.TextModel;
import com.android.mms.ui.DeliveryReportActivity.MmsReportStatus;
import com.android.mms.ui.MessageListAdapter.ColumnsMap;
import com.android.mms.util.AddressUtils;
import com.android.mms.util.DownloadManager;
import com.android.mms.util.ItemLoadedCallback;
import com.android.mms.util.ItemLoadedFuture;
import com.android.mms.util.PduLoaderManager;
import com.google.android.mms.MmsException;
import com.google.android.mms.pdu.EncodedStringValue;
import com.google.android.mms.pdu.MultimediaMessagePdu;
import com.google.android.mms.pdu.NotificationInd;
import com.google.android.mms.pdu.PduHeaders;
import com.google.android.mms.pdu.PduPersister;
import com.google.android.mms.pdu.RetrieveConf;
import com.google.android.mms.pdu.SendReq;

/// M:
import android.telephony.SmsManager;

import com.android.mms.model.FileAttachmentModel;
import com.mediatek.encapsulation.com.mediatek.common.featureoption.EncapsulatedFeatureOption;
import com.mediatek.encapsulation.MmsLog;
import com.mediatek.ipmsg.util.IpMessageUtils;

import java.util.ArrayList;
import java.util.List;
/// M: add for ipmessage
import android.graphics.Bitmap;

import com.android.mms.MmsConfig;
import com.mediatek.mms.ipmessage.message.IpMessage;
import com.mediatek.mms.ipmessage.IpMessageConsts.IpMessageServiceId;

import java.lang.ref.SoftReference;
//add for attachment enhance
import com.android.mms.model.FileModel;
import com.mediatek.mms.ext.IMmsAttachmentEnhance;
import com.mediatek.mms.ext.MmsAttachmentEnhanceImpl;
import com.mediatek.pluginmanager.PluginManager;
import com.mediatek.pluginmanager.Plugin;
import com.android.mms.MmsPluginManager;

/**
 * Mostly immutable model for an SMS/MMS message.
 *
 * <p>The only mutable field is the cached formatted message member,
 * the formatting of which is done outside this model in MessageListItem.
 */
public class MessageItem {
    private static String TAG = "MessageItem";
    private static String IPMSG_TAG = "Mms/ipmsg/MessageItem";

    public enum DeliveryStatus  { NONE, INFO, FAILED, PENDING, RECEIVED }

    public static int ATTACHMENT_TYPE_NOT_LOADED = -1;

    final Context mContext;
    final String mType;
    final long mMsgId;
    final int mBoxId;

    DeliveryStatus mDeliveryStatus;
    boolean mReadReport;
    boolean mLocked;            // locked to prevent auto-deletion

    String mTimestamp;
    String mAddress;
    String mContact;
    /// M: fix bug ALPS00439894, MTK MR1 new feature: Group Mms
    Contact mGroupContact;
    String mBody; // Body of SMS, first text of MMS.
    String mTextContentType; // ContentType of text of MMS.
    Pattern mHighlight; // portion of message to highlight (from search)

    // The only non-immutable field.  Not synchronized, as access will
    // only be from the main GUI thread.  Worst case if accessed from
    // another thread is it'll return null and be set again from that
    // thread.
    CharSequence mCachedFormattedMessage;

    // The last message is cached above in mCachedFormattedMessage. In the latest design, we
    // show "Sending..." in place of the timestamp when a message is being sent. mLastSendingState
    // is used to keep track of the last sending state so that if the current sending state is
    // different, we can clear the message cache so it will get rebuilt and recached.
    boolean mLastSendingState;

    // Fields for MMS only.
    Uri mMessageUri;
    int mMessageType;
    int mAttachmentType;
    String mSubject;
    SlideshowModel mSlideshow;
    int mMessageSize;
    int mErrorType;
    int mErrorCode;
    int mMmsStatus;
    boolean mHasReadReport = false;
    boolean mHasDeliveryReport = false;
    Cursor mCursor;
    ColumnsMap mColumnsMap;
    private PduLoadedCallback mPduLoadedCallback;
    private ItemLoadedFuture mItemLoadedFuture;

    /// M:
    boolean mSimMsg = false;
    /// M: add for gemini
    int mSimId;
    CharSequence mCachedFormattedTimestamp;
    CharSequence mCachedFormattedSimStatus;
    
    long mSmsDate = 0;
    long mSmsSentDate = 0;
    String mServiceCenter = null;
    
    private boolean mItemSelected = false;
    private boolean mHasDrmContent = false;

    /// M: add for ipmessage
    int mIpMessageId = 0;
    /// M: do not use this, use mLocked. It doesn't record right status for mms.
    boolean mImportant = false;

    boolean mIsDrawTimeDivider = false;
    String mTimeDividerString = "";

    boolean mIsDrawUnreadDivider = false;
    int mUnreadCount = 0;

    boolean mIsDrawOnlineDivider = false;
    String mOnlineString = "";

    boolean mIsDrawLoadAllButton = false;
    IpMessage mIpMessage = null;
    private SoftReference<Bitmap> mBitmapCache = new SoftReference<Bitmap>(null);
    private int mCacheBitmapWidth;
    private int mCacheBitmapHeight;

    public Bitmap getIpMessageBitmap() {
        return mBitmapCache.get();
    }

    public void setIpMessageBitmapCache(Bitmap bitmap) {
        if (null != bitmap) {
            mBitmapCache = new SoftReference<Bitmap>(bitmap);
        }
    }

    MessageItem(Context context, int boxId, int messageType, int simId, int errorType, int locked, int charset,
            long msgId, String type, String subject, String serviceCenter, String deliveryReport, String readReport,
            Pattern highlight, boolean isDrawTimeDivider, boolean isDrawUnreadDivider, int unreadCount,
            boolean isDrawLoadAllMessagesButton, long indDate, int mmsStatus) throws MmsException {
        mContext = context;
        mBoxId = boxId;
        mMessageType = messageType;
        mSimId = simId;
        mErrorType = errorType;
        mLocked = locked != 0;
        if (!TextUtils.isEmpty(subject)) {
            EncodedStringValue v = new EncodedStringValue(charset,
                    PduPersister.getBytes(subject));
            mSubject = v.getString();
        }
        mMsgId = msgId;
        mType = type;
        mServiceCenter = serviceCenter;
        mHighlight = highlight;
        /// M: fix bug ALPS00406912
        mMmsStatus = mmsStatus;

        mMessageUri = ContentUris.withAppendedId(Mms.CONTENT_URI, mMsgId);
        long timestamp = 0L;
        PduPersister p = PduPersister.getPduPersister(mContext);
        if (PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND == mMessageType) {
            mDeliveryStatus = DeliveryStatus.NONE;
            NotificationInd notifInd = (NotificationInd) p.load(mMessageUri);
            interpretFrom(notifInd.getFrom(), mMessageUri);
            /// M: Borrow the mBody to hold the URL of the message.
            mBody = new String(notifInd.getContentLocation());
            mMessageSize = (int) notifInd.getMessageSize();
            timestamp = notifInd.getExpiry() * 1000L;
        } else {
            MultimediaMessagePdu msg = (MultimediaMessagePdu) p.load(mMessageUri);
            mSlideshow = SlideshowModel.createFromPduBody(context, msg.getBody());
            mAttachmentType = MessageUtils.getAttachmentType(mSlideshow);
            /// M: @{
            mHasDrmContent = false;
            mHasDrmContent = mSlideshow.checkDrmContent();
            /// @}

            if (mMessageType == PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF) {
                RetrieveConf retrieveConf = (RetrieveConf) msg;
                interpretFrom(retrieveConf.getFrom(), mMessageUri);
                timestamp = retrieveConf.getDate() * 1000L;
            } else {
                /// M: Use constant string for outgoing messages
                mAddress = context.getString(R.string.messagelist_sender_self);
                mContact = mAddress;
                timestamp = ((SendReq) msg).getDate() * 1000L;
            }

            if ((deliveryReport == null) || !mAddress.equals(context.getString(
                    R.string.messagelist_sender_self))) {
                mDeliveryStatus = DeliveryStatus.NONE;
            } else {
                int reportInt;
                try {
                    reportInt = Integer.parseInt(deliveryReport);
                    if (reportInt == PduHeaders.VALUE_YES) {
                        mDeliveryStatus = DeliveryStatus.RECEIVED;
                    } else {
                        mDeliveryStatus = DeliveryStatus.NONE;
                    }
                } catch (NumberFormatException nfe) {
                    Log.e(TAG, "Value for delivery report was invalid.");
                    mDeliveryStatus = DeliveryStatus.NONE;
                }
            }

            if ((readReport == null) || !mAddress.equals(context.getString(
                    R.string.messagelist_sender_self))) {
                mReadReport = false;
            } else {
                int reportInt;
                try {
                    reportInt = Integer.parseInt(readReport);
                    mReadReport = (reportInt == PduHeaders.VALUE_YES);
                } catch (NumberFormatException nfe) {
                    Log.e(TAG, "Value for read report was invalid.");
                    mReadReport = false;
                }
            }
            if (mBoxId == Mms.MESSAGE_BOX_SENT
                    && (mReadReport || mDeliveryStatus == MessageItem.DeliveryStatus.RECEIVED)) {
                List<MmsReportStatus> mmsReportStatusList = MessageUtils.getMmsReportStatus(context, mMsgId);
                if (mmsReportStatusList != null && mmsReportStatusList.size() > 0) {
                    for (MmsReportStatus mmsReportStatus : mmsReportStatusList) {
                        MmsLog.d(TAG, "MessageItem.init(): readStatus = " + mmsReportStatus.readStatus);
                        if (mmsReportStatus.readStatus == PduHeaders.READ_STATUS_READ) {
                            mHasReadReport = true;
                            break;
                        }
                        MmsLog.d(TAG, "MessageItem.init(): deliveryStatus = " + mmsReportStatus.deliveryStatus);
                        if (mmsReportStatus.deliveryStatus == PduHeaders.STATUS_RETRIEVED) {
                            mHasDeliveryReport = true;
                        }
                    }
                }
            }

            SlideModel slide = mSlideshow.get(0);
            if ((slide != null) && slide.hasText()) {
                TextModel tm = slide.getText();
                mBody = tm.getText();
                mTextContentType = tm.getContentType();
            }

            mMessageSize = mSlideshow.getCurrentSlideshowSize();

            ///M: Because CMCC have add attachment size into CurrentSlideshowSize,
            /// It is NOT necessary to add attach.getAttachSize into mMessageSize

            IMmsAttachmentEnhance mMmsAttachmentEnhancePlugin = (IMmsAttachmentEnhance)MmsPluginManager.getMmsPluginObject(MmsPluginManager.MMS_PLUGIN_TYPE_MMS_ATTACHMENT_ENHANCE);

            if (mMmsAttachmentEnhancePlugin != null ) {
                if (mMmsAttachmentEnhancePlugin.isSupportAttachmentEnhance() == false) {
                    //Not OP01
                    /// M: Must add size of file attachments, if any
                    final ArrayList<FileAttachmentModel> attaches = mSlideshow.getAttachFiles();
                    if (attaches != null) {
                        for (FileAttachmentModel attach : attaches) {
                            mMessageSize += attach.getAttachSize();
                        }
                    }
                } else {
                    //OP01
                    Log.w(TAG, "CMCC: Not add attach.getAttachSize into mMessageSize");
                }
            } else {
            //Not OP01
            /// M: Must add size of file attachments, if any
            final ArrayList<FileAttachmentModel> attaches = mSlideshow.getAttachFiles();
            if (attaches != null) {
                for (FileAttachmentModel attach : attaches) {
                    mMessageSize += attach.getAttachSize();
                }
            }
        }
        }

        if (!isOutgoingMessage()) {
            /// M: add for ipmessage, change showing month-day to showing hour-minutes
            if (PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND == mMessageType) {
                mTimestamp = context.getString(getTimestampStrId(), MessageUtils
                        .formatTimeStampString(context, timestamp));
            } else {
                mTimestamp = IpMessageUtils.getShortTimeString(context, timestamp);
            }
        }

        /// M: add for ipmessage
        mIsDrawTimeDivider = isDrawTimeDivider;
        if (mIsDrawTimeDivider) {
            if (PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND == mMessageType) {
                mTimeDividerString = IpMessageUtils.getTimeDividerString(context, indDate * 1000L);
            } else {
                mTimeDividerString = IpMessageUtils.getTimeDividerString(context, timestamp);
            }
        }
        mIsDrawUnreadDivider = isDrawUnreadDivider;
        mUnreadCount = unreadCount;
        mIsDrawLoadAllButton = isDrawLoadAllMessagesButton;
        MmsLog.d(IPMSG_TAG, "MessageItem(): show time divider ?= " + mIsDrawTimeDivider + ", mTimeDividerString = "
            + mTimeDividerString + ", show unread divider ?= " + mIsDrawUnreadDivider + ", unread count = "
            + mUnreadCount + ", show load all messages button ?= " + isDrawLoadAllMessagesButton);
    }

    MessageItem(Context context, String type, Cursor cursor, ColumnsMap columnsMap, Pattern highlight,
            boolean isDrawTimeDivider, boolean isDrawUnreadDivider, int unreadCount,
            boolean isDrawLoadAllMessagesButton, long indDate) throws MmsException {
        mContext = context;
        /// M: @{
        if (cursor == null) {
            throw new MmsException("Get the null cursor");
        }
        /// @}
        mMsgId = cursor.getLong(columnsMap.mColumnMsgId);
        mHighlight = highlight;
        mType = type;
        mCursor = cursor;
        mColumnsMap = columnsMap;

        /// M: add for ipmessage
        mIpMessageId = cursor.getInt(columnsMap.mColumnSmsIpMessageId);
        /// M: if plugin is not exist, take ipmessage as sms.
        if (!IpMessageUtils.getIpMessagePlugin(context).isActualPlugin()) {
            mIpMessageId = 0;
        }
        if (mIpMessageId > 0 && MmsConfig.getIpMessagServiceId(context) == IpMessageServiceId.ISMS_SERVICE) {
            mIpMessage = IpMessageUtils.getMessageManager(context).getIpMsgInfo(mMsgId);
            if (mIpMessage == null) {
                MmsLog.d(IPMSG_TAG, "MessageItem.init(): ip message is null!");
            }
        }
        /// M: do not use this, use mLocked. It doesn't record right status for mms.
        mImportant = cursor.getInt(columnsMap.mColumnSmsImportant) == 1;
        mIsDrawTimeDivider = isDrawTimeDivider;
        mIsDrawUnreadDivider = isDrawUnreadDivider;
        mUnreadCount = unreadCount;
        mIsDrawLoadAllButton = isDrawLoadAllMessagesButton;
        MmsLog.d(IPMSG_TAG, "MessageItem.init(): mIpMessageId = " + mIpMessageId + ", mImportant = " + mImportant
                + ", mIsDrawTimeDivider = " + mIsDrawTimeDivider + ", mIsDrawUnreadDivider = "
                + mIsDrawUnreadDivider + ", mUnreadCount = " + mUnreadCount + ", show load all messages button ?= "
                + isDrawLoadAllMessagesButton);

        /// M: @{
        mServiceCenter = cursor.getString(columnsMap.mColumnSmsServiceCenter);

        //to filter SIM Message
        mSimId = -1;
        /// @}
        if ("sms".equals(type)) {
            mReadReport = false; // No read reports in sms

            long status = cursor.getLong(columnsMap.mColumnSmsStatus);
            /// M: @{
            if (status == SmsManager.STATUS_ON_ICC_READ
                    || status == SmsManager.STATUS_ON_ICC_UNREAD
                    || status == SmsManager.STATUS_ON_ICC_SENT
                    || status == SmsManager.STATUS_ON_ICC_UNSENT) {
                mSimMsg = true;
            }
            /// @}
            
            /// M: @{
            /*
            if (status == Sms.STATUS_NONE) {
                // No delivery report requested
                mDeliveryStatus = DeliveryStatus.NONE;
            } else if (status >= Sms.STATUS_FAILED) {
                // Failure
                mDeliveryStatus = DeliveryStatus.FAILED;
            } else if (status >= Sms.STATUS_PENDING) {
                // Pending
                mDeliveryStatus = DeliveryStatus.PENDING;
            } else {
                // Success
                mDeliveryStatus = DeliveryStatus.RECEIVED;
            }*/
            if (status >= Sms.STATUS_FAILED) {
                // Failure
                mDeliveryStatus = DeliveryStatus.FAILED;
            } else if (status >= Sms.STATUS_PENDING) {
                // Pending
                mDeliveryStatus = DeliveryStatus.PENDING;
            } else if (status >= Sms.STATUS_COMPLETE && !mSimMsg) {
                // Success
                mDeliveryStatus = DeliveryStatus.RECEIVED;
            } else {
                mDeliveryStatus = DeliveryStatus.NONE;
            }
            /// @}

            mMessageUri = ContentUris.withAppendedId(Sms.CONTENT_URI, mMsgId);
            // Set contact and message body
            /// M: @{
            //mBoxId = cursor.getInt(columnsMap.mColumnSmsType);
            if (mSimMsg) {
                if (status == SmsManager.STATUS_ON_ICC_SENT 
                        || status == SmsManager.STATUS_ON_ICC_UNSENT) {
                    mBoxId = Sms.MESSAGE_TYPE_SENT;
                } else {
                    mBoxId = Sms.MESSAGE_TYPE_INBOX;
                }
            } else {
                mBoxId = cursor.getInt(columnsMap.mColumnSmsType);
            }
            /// @}
            mAddress = cursor.getString(columnsMap.mColumnSmsAddress);
            
            /// M: @{
            //add for gemini and td single
            mSimId = cursor.getInt(columnsMap.mColumnSmsSimId);
            /// @}
            
            /// M: @{
            /*if (Sms.isOutgoingFolder(mBoxId)) {
                String meString = context.getString(
                        R.string.messagelist_sender_self);

                mContact = meString;
            } else {
                // For incoming messages, the ADDRESS field contains the sender.
                mContact = Contact.get(mAddress, false).getName();
            }*/
            if (Sms.isOutgoingFolder(mBoxId) && !mSimMsg) {
                String meString = context.getString(
                        R.string.messagelist_sender_self);

                mContact = meString;
            } else {
                // For incoming messages, the ADDRESS field contains the sender.
                if (!TextUtils.isEmpty(mAddress)) {
                    mGroupContact = Contact.get(mAddress, true);
                    mContact = mGroupContact.getName();
                } else {
                    mContact = context.getString(android.R.string.unknownName);
                }
            }
            /// @}
            
            /// M: @{
            //mBody = cursor.getString(columnsMap.mColumnSmsBody);
            if (mSimMsg) {
                mBody = mContact + " : " + cursor.getString(columnsMap.mColumnSmsBody);
            } else {
                mBody = cursor.getString(columnsMap.mColumnSmsBody);
            }
            /// @}

            // Unless the message is currently in the progress of being sent, it gets a time stamp.
            long date = cursor.getLong(columnsMap.mColumnSmsDate);
            if (!isOutgoingMessage()) {
                // Set "received" or "sent" time stamp
                /// M: @{
                //mTimestamp = MessageUtils.formatTimeStampString(context, date);
                mSmsDate = date;
                if (date != 0) {
                    /// M: add for ipmessage, change showing month-day to showing hour-minutes
                    if (isReceivedMessage()) {
                        if (mSimMsg) {
                            mTimestamp = String.format(context.getString(R.string.received_on),
                                    MessageUtils.formatTimeStampString(context, date));
                        } else {
                            mTimestamp = IpMessageUtils.getShortTimeString(context, date);
                        }
                        mSmsSentDate = cursor.getLong(columnsMap.mColumnSmsDateSent);
                    } else {
//                        mTimestamp = String.format(context.getString(R.string.sent_on),
//                                MessageUtils.formatTimeStampString(context, date));
                        mTimestamp = IpMessageUtils.getShortTimeString(context, date);
                    }
                } else {
                    mTimestamp = "";
                }
            }
            if (mIsDrawTimeDivider) {
                mTimeDividerString = IpMessageUtils.getTimeDividerString(context, date);
            }
            mLocked = cursor.getInt(columnsMap.mColumnSmsLocked) != 0;
            mErrorCode = cursor.getInt(columnsMap.mColumnSmsErrorCode);
        } else if ("mms".equals(type)) {
            mMessageUri = ContentUris.withAppendedId(Mms.CONTENT_URI, mMsgId);
            mBoxId = cursor.getInt(columnsMap.mColumnMmsMessageBox);
            mMessageType = cursor.getInt(columnsMap.mColumnMmsMessageType);
            mErrorType = cursor.getInt(columnsMap.mColumnMmsErrorType);
            String subject = cursor.getString(columnsMap.mColumnMmsSubject);
            if (!TextUtils.isEmpty(subject)) {
                EncodedStringValue v = new EncodedStringValue(
                        cursor.getInt(columnsMap.mColumnMmsSubjectCharset),
                        PduPersister.getBytes(subject));
                /// M: google jb.mr1 patch, group mms
                mSubject = MessageUtils.cleanseMmsSubject(context, v.getString());
            }
            mLocked = cursor.getInt(columnsMap.mColumnMmsLocked) != 0;
            
            /// M: @{
            //add for gemini
            if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                mSimId = cursor.getInt(columnsMap.mColumnMmsSimId);
            }
            /// @}

            mSlideshow = null;
            mAttachmentType = ATTACHMENT_TYPE_NOT_LOADED;
            mDeliveryStatus = DeliveryStatus.NONE;
            mReadReport = false;
            mBody = null;
            mMessageSize = 0;
            mTextContentType = null;
            /// google JB.MR1 patch, Initialize the time stamp to "" instead of null @{
            mTimestamp = "";
            /// @}
            mMmsStatus = cursor.getInt(columnsMap.mColumnMmsStatus);

            // Start an async load of the pdu. If the pdu is already loaded, the callback
            // will get called immediately
            boolean loadSlideshow = mMessageType != PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND;

            mItemLoadedFuture = MmsApp.getApplication().getPduLoaderManager()
                    .getPdu(mMessageUri, loadSlideshow,
                    new PduLoadedMessageItemCallback());

        } else {
            throw new MmsException("Unknown type of the message: " + type);
        }
    }

    private void interpretFrom(EncodedStringValue from, Uri messageUri) {
        if (from != null) {
            mAddress = from.getString();
        } else {
            // In the rare case when getting the "from" address from the pdu fails,
            // (e.g. from == null) fall back to a slower, yet more reliable method of
            // getting the address from the "addr" table. This is what the Messaging
            // notification system uses.
            mAddress = AddressUtils.getFrom(mContext, messageUri);
        }
        /// M: fix bug ALPS00439894, MTK MR1 new feature: Group Mms
        mGroupContact = Contact.get(mAddress, false);
        mContact = TextUtils.isEmpty(mAddress) ? mContext.getString(android.R.string.unknownName)
                : mGroupContact.getName();
    }

    public boolean isMms() {
        return mType.equals("mms");
    }

    public boolean isSms() {
        return mType.equals("sms");
    }

    public boolean isDownloaded() {
        return (mMessageType != PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND);
    }

    /// M: google JB.MR1 patch, group mms
    public boolean isMe() {
        // Logic matches MessageListAdapter.getItemViewType which is used to decide which
        // type of MessageListItem to create: a left or right justified item depending on whether
        // the message is incoming or outgoing.
        boolean isIncomingMms = isMms()
                                    && (mBoxId == Mms.MESSAGE_BOX_INBOX
                                            || mBoxId == Mms.MESSAGE_BOX_ALL);
        boolean isIncomingSms = isSms()
                                    && (mBoxId == Sms.MESSAGE_TYPE_INBOX
                                            || mBoxId == Sms.MESSAGE_TYPE_ALL);
        return !(isIncomingMms || isIncomingSms);
    }

    public boolean isOutgoingMessage() {
        boolean isOutgoingMms = isMms() && (mBoxId == Mms.MESSAGE_BOX_OUTBOX);
        boolean isOutgoingSms = isSms()
                                    && ((mBoxId == Sms.MESSAGE_TYPE_FAILED)
                                            || (mBoxId == Sms.MESSAGE_TYPE_OUTBOX)
                                            || (mBoxId == Sms.MESSAGE_TYPE_QUEUED));
        return isOutgoingMms || isOutgoingSms;
    }

    public boolean isSending() {
        return !isFailedMessage() && isOutgoingMessage();
    }

    public boolean isFailedMessage() {
        boolean isFailedMms = isMms()
                            && (mErrorType >= MmsSms.ERR_TYPE_GENERIC_PERMANENT);
        boolean isFailedSms = isSms()
                            && (mBoxId == Sms.MESSAGE_TYPE_FAILED);
        return isFailedMms || isFailedSms;
    }

    // Note: This is the only mutable field in this class.  Think of
    // mCachedFormattedMessage as a C++ 'mutable' field on a const
    // object, with this being a lazy accessor whose logic to set it
    // is outside the class for model/view separation reasons.  In any
    // case, please keep this class conceptually immutable.
    public void setCachedFormattedMessage(CharSequence formattedMessage) {
        mCachedFormattedMessage = formattedMessage;
    }

    public CharSequence getCachedFormattedMessage() {
        boolean isSending = isSending();
        if (isSending != mLastSendingState) {
            mLastSendingState = isSending;
            mCachedFormattedMessage = null;         // clear cache so we'll rebuild the message
                                                    // to show "Sending..." or the sent date.
        }
        return mCachedFormattedMessage;
    }

    public int getBoxId() {
        return mBoxId;
    }

    public long getMessageId() {
        return mMsgId;
    }

    public int getMmsDownloadStatus() {
        return mMmsStatus & ~DownloadManager.DEFERRED_MASK;
    }

    @Override
    public String toString() {
        //add for gemini
        if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
            return "type: " + mType +
                " box: " + mBoxId +
                " sim: " + mSimId +
                " uri: " + mMessageUri +
                " address: " + mAddress +
                " contact: " + mContact +
                " read: " + mReadReport +
                " delivery status: " + mDeliveryStatus;
        } else {
            return "type: " + mType +
                " box: " + mBoxId +
                " uri: " + mMessageUri +
                " address: " + mAddress +
                " contact: " + mContact +
                " read: " + mReadReport +
                " delivery status: " + mDeliveryStatus;
        }
    }
    

    public class PduLoadedMessageItemCallback implements ItemLoadedCallback {
        public void onItemLoaded(Object result, Throwable exception) {
            if (exception != null) {
                Log.e(TAG, "PduLoadedMessageItemCallback PDU couldn't be loaded: ", exception);
                return;
            }
            /// M: google jb.mr1 pathc, remove and fully reloaded the next time
            /// When a pdu or image is canceled during loading @{
            if (mItemLoadedFuture != null) {
                synchronized(mItemLoadedFuture) {
                    mItemLoadedFuture.setIsDone(true);
                }
            }
            /// @}
            PduLoaderManager.PduLoaded pduLoaded = (PduLoaderManager.PduLoaded)result;
            long timestamp = 0L;
            long indDate = 0L;
            if (PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND == mMessageType) {
                mDeliveryStatus = DeliveryStatus.NONE;
                NotificationInd notifInd = (NotificationInd)pduLoaded.mPdu;
                interpretFrom(notifInd.getFrom(), mMessageUri);
                // Borrow the mBody to hold the URL of the message.
                mBody = new String(notifInd.getContentLocation());
                mMessageSize = (int) notifInd.getMessageSize();
                timestamp = notifInd.getExpiry() * 1000L;
                indDate = mCursor.getLong(mColumnsMap.mColumnMmsDate);
            } else {
                if (mCursor.isClosed()) {
                    return;
                }
                MultimediaMessagePdu msg = (MultimediaMessagePdu)pduLoaded.mPdu;
                mSlideshow = pduLoaded.mSlideshow;
                mAttachmentType = MessageUtils.getAttachmentType(mSlideshow);
                /// M: @{
                mHasDrmContent = false;
                mHasDrmContent = mSlideshow.checkDrmContent();
                /// @}

                if (mMessageType == PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF) {
                    if (msg == null) {
                        interpretFrom(null, mMessageUri);
                    } else {
                        RetrieveConf retrieveConf = (RetrieveConf) msg;
                        interpretFrom(retrieveConf.getFrom(), mMessageUri);
                        timestamp = retrieveConf.getDate() * 1000L;
                    }
                } else {
                    // Use constant string for outgoing messages
                    mContact = mAddress =
                            mContext.getString(R.string.messagelist_sender_self);
                    timestamp = msg == null ? 0 : ((SendReq) msg).getDate() * 1000L;
                }

                SlideModel slide = mSlideshow == null ? null : mSlideshow.get(0);
                if ((slide != null) && slide.hasText()) {
                    TextModel tm = slide.getText();
                    mBody = tm.getText();
                    mTextContentType = tm.getContentType();
                }

                mMessageSize = mSlideshow == null ? 0 : mSlideshow.getCurrentSlideshowSize();

                String report = mCursor.getString(mColumnsMap.mColumnMmsDeliveryReport);
                if ((report == null) || !mAddress.equals(mContext.getString(
                        R.string.messagelist_sender_self))) {
                    mDeliveryStatus = DeliveryStatus.NONE;
                } else {
                    int reportInt;
                    try {
                        reportInt = Integer.parseInt(report);
                        if (reportInt == PduHeaders.VALUE_YES) {
                            mDeliveryStatus = DeliveryStatus.RECEIVED;
                        } else {
                            mDeliveryStatus = DeliveryStatus.NONE;
                        }
                    } catch (NumberFormatException nfe) {
                        Log.e(TAG, "Value for delivery report was invalid.");
                        mDeliveryStatus = DeliveryStatus.NONE;
                    }
                }

                report = mCursor.getString(mColumnsMap.mColumnMmsReadReport);
                if ((report == null) || !mAddress.equals(mContext.getString(
                        R.string.messagelist_sender_self))) {
                    mReadReport = false;
                } else {
                    int reportInt;
                    try {
                        reportInt = Integer.parseInt(report);
                        mReadReport = (reportInt == PduHeaders.VALUE_YES);
                    } catch (NumberFormatException nfe) {
                        Log.e(TAG, "Value for read report was invalid.");
                        mReadReport = false;
                    }
                }
                /// M: Must add size of file attachments, if any
                final ArrayList<FileAttachmentModel> attaches = mSlideshow.getAttachFiles();
                if (attaches != null) {
                    for (FileAttachmentModel attach : attaches) {
                        mMessageSize += attach.getAttachSize();
                    }
                }
            }
            if (!isOutgoingMessage()) {
                /// M: add for ipmessage, change showing month-day to showing hour-minutes
                if (PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND == mMessageType) {
                    mTimestamp = mContext.getString(getTimestampStrId(), MessageUtils
                            .formatTimeStampString(mContext, timestamp));
                } else {
                    mTimestamp = mContext.getString(getTimestampStrId(),
                        IpMessageUtils.getShortTimeString(mContext, timestamp));
                }
            }

            if (mIsDrawTimeDivider) {
                if (PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND == mMessageType) {
                    mTimeDividerString = IpMessageUtils.getTimeDividerString(mContext, indDate * 1000L);
                } else {
                    mTimeDividerString = IpMessageUtils.getTimeDividerString(mContext, timestamp);
                }
            }

            if (mPduLoadedCallback != null) {
                mPduLoadedCallback.onPduLoaded(MessageItem.this);
            }
        }
    }

    public void setOnPduLoaded(PduLoadedCallback pduLoadedCallback) {
        mPduLoadedCallback = pduLoadedCallback;
    }

    public void cancelPduLoading() {
        /// M: google jb.mr1 pathc, remove and fully reloaded the next time
        /// When a pdu or image is canceled during loading @{
        if (mItemLoadedFuture != null && !mItemLoadedFuture.isDone()) {
            if (Log.isLoggable(LogTag.APP, Log.DEBUG)) {
                Log.v(TAG, "cancelPduLoading for: " + this);
            }
            mItemLoadedFuture.cancel(mMessageUri);
            mItemLoadedFuture = null;
        }
        /// @}
    }

    public interface PduLoadedCallback {
        /**
         * Called when this item's pdu and slideshow are finished loading.
         *
         * @param messageItem the MessageItem that finished loading.
         */
        void onPduLoaded(MessageItem messageItem);
    }

    public SlideshowModel getSlideshow() {
        return mSlideshow;
    }

    /// M: @{
    public boolean hasDrmContent() {
        return mHasDrmContent;
    }

    public boolean isSimMsg() {
        return mSimMsg;
    }
    
    public boolean isReceivedMessage() {
        boolean isReceivedMms = isMms() && (mBoxId == Mms.MESSAGE_BOX_INBOX);
        boolean isReceivedSms = isSms() && (mBoxId == Sms.MESSAGE_TYPE_INBOX);
        return isReceivedMms || isReceivedSms || (mBoxId == 0 && isSms());
    }

    public boolean isSentMessage() {
        boolean isSentMms = isMms() && (mBoxId == Mms.MESSAGE_BOX_SENT);
        boolean isSentSms = isSms() && (mBoxId == Sms.MESSAGE_TYPE_SENT);
        return isSentMms || isSentSms;
    }
    
    public void setCachedFormattedTimestamp(CharSequence formattedTimestamp) {
        mCachedFormattedTimestamp = formattedTimestamp;
    }

    public CharSequence getCachedFormattedTimestamp() {
        boolean isSending = isSending();
        if (isSending != mLastSendingState) {
            mLastSendingState = isSending;
            mCachedFormattedTimestamp = null;
        }
        return mCachedFormattedTimestamp;
    }

    public void setCachedFormattedSimStatus(CharSequence formattedSimStatus) {
        mCachedFormattedSimStatus = formattedSimStatus;
    }

    public CharSequence getCachedFormattedSimStatus() {
        boolean isSending = isSending();
        if (isSending != mLastSendingState) {
            mLastSendingState = isSending;
            mCachedFormattedSimStatus = null;
        }
        return mCachedFormattedSimStatus;
    }
    
    /// M: add for gemini
    public int getSimId() {
        return mSimId;
    }

    public boolean isSelected() {
        return mItemSelected;
    }
    
    public void setSelectedState(boolean isSelected) {
        mItemSelected = isSelected;
    }
    
    /// M: Add for vCard begin
    public int getFileAttachmentCount() {
        if (mSlideshow != null) {
            return mSlideshow.sizeOfFilesAttach();
        }
        return 0;
    }
    /// M: Add for vCard end

    public String getServiceCenter() {
        return mServiceCenter;
    }
    
    private int getTimestampStrId() {
        if (PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND == mMessageType) {
            return R.string.expire_on;
        } else {
            if (isReceivedMessage()) {
                return R.string.received_on;
            } else {
                return R.string.sent_on;
            }
        }
    }
    /// @}

    /// M: these 3 methods is matched with setIpMessageBitmapCache.
    public void setIpMessageBitmapSize(int width, int height) {
        mCacheBitmapWidth = width;
        mCacheBitmapHeight = height;
    }

    public int getIpMessageBitmapWidth() {
        return mCacheBitmapWidth;
    }

    public int getIpMessageBitmapHeight() {
        return mCacheBitmapHeight;
    }
}
