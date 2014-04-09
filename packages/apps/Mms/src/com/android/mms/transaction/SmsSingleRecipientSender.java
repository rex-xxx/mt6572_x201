package com.android.mms.transaction;

import java.util.ArrayList;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Telephony.Mms;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsManager;
import android.util.Log;

import com.android.mms.LogTag;
import com.android.mms.MmsConfig;
import com.google.android.mms.MmsException;
import android.provider.Telephony.Sms;

import com.android.mms.data.Conversation;
import com.android.mms.ui.MessageUtils;

/// M:
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.SmsMessage;
import com.mediatek.encapsulation.android.telephony.gemini.EncapsulatedGeminiSmsManager;
import com.mediatek.encapsulation.android.telephony.EncapsulatedSmsManager;

import com.mediatek.encapsulation.com.android.internal.telephony.EncapsulatedPhone;
import com.android.mms.MmsApp;
import com.android.mms.ui.SmsPreferenceActivity;
import com.mediatek.encapsulation.com.mediatek.common.featureoption.EncapsulatedFeatureOption;
import com.mediatek.encapsulation.MmsLog;
import com.mediatek.encapsulation.android.telephony.EncapsulatedTelephony.SIMInfo;

public class SmsSingleRecipientSender extends SmsMessageSender {

    private final boolean mRequestDeliveryReport;
    private String mDest;
    private Uri mUri;
    private static final String TAG = "SmsSingleRecipientSender";

    public SmsSingleRecipientSender(Context context, String dest, String msgText, long threadId,
            boolean requestDeliveryReport, Uri uri) {
        super(context, null, msgText, threadId);
        mRequestDeliveryReport = requestDeliveryReport;
        mDest = dest;
        mUri = uri;
    }

    public boolean sendMessage(long token) throws MmsException {
        if (LogTag.DEBUG_SEND) {
            Log.v(TAG, "sendMessage token: " + token);
        }
        /// M:Code analyze 001, convert sim id to slot id @{
        int slotId = SIMInfo.getSlotById(mContext, mSimId);
        MmsLog.d(MmsApp.TXN_TAG, "SmsSingleRecipientSender: sendMessage() simId=" + mSimId +"slotId=" + slotId);

        if (mMessageText == null) {
            // Don't try to send an empty message, and destination should be just
            // one.
            throw new MmsException("Null message body or have multiple destinations.");
        }

        /// M:Code analyze 002,add a variable to caculate the length of sms @{
        int codingType = SmsMessage.ENCODING_UNKNOWN;
        if (MmsConfig.getSmsEncodingTypeEnabled()) {
            codingType = MessageUtils.getSmsEncodingType(mContext);
        }
        /// @}

        //SmsManager smsManager = SmsManager.getDefault();
        ArrayList<String> messages = null;
        if ((MmsConfig.getEmailGateway() != null) &&
                (Mms.isEmailAddress(mDest) || MessageUtils.isAlias(mDest))) {
            String msgText;
            msgText = mDest + " " + mMessageText;
            mDest = MmsConfig.getEmailGateway();
            /// M:Code analyze 003,add a parameter codingType to caculate length of sms @{
            messages = EncapsulatedSmsManager.divideMessage(msgText, codingType);
            /// @}
        } else {
            /// M:Code analyze 003,add a parameter codingType to caculate length of sms @{
            messages = EncapsulatedSmsManager.divideMessage(mMessageText, codingType);
            /// @}
            // remove spaces and dashes from destination number
            // (e.g. "801 555 1212" -> "8015551212")
            // (e.g. "+8211-123-4567" -> "+82111234567")
            /// M:Code analyze 004, comment the line,using customized striping pattern to mDest @{
            //mDest = PhoneNumberUtils.stripSeparators(mDest);
            /** M: remove spaces from destination number (e.g. "801 555 1212" -> "8015551212") @{ */
            mDest = mDest.replaceAll(" ", "");
            mDest = mDest.replaceAll("-", "");
            /// @}
            mDest = Conversation.verifySingleRecipient(mContext, mThreadId, mDest);
        }
        int messageCount = messages.size();
        /// M:
        MmsLog.d(MmsApp.TXN_TAG, "SmsSingleRecipientSender: sendMessage(), Message Count=" + messageCount);

        if (messageCount == 0) {
            // Don't try to send an empty message.
            throw new MmsException("SmsMessageSender.sendMessage: divideMessage returned " +
                    "empty messages. Original message is \"" + mMessageText + "\"");
        }

        boolean moved = Sms.moveMessageToFolder(mContext, mUri, Sms.MESSAGE_TYPE_OUTBOX, 0);
        if (!moved) {
            throw new MmsException("SmsMessageSender.sendMessage: couldn't move message " +
                    "to outbox: " + mUri);
        }
        if (LogTag.DEBUG_SEND) {
            Log.v(TAG, "sendMessage mDest: " + mDest + " mRequestDeliveryReport: " +
                    mRequestDeliveryReport);
        }

        ArrayList<PendingIntent> deliveryIntents =  new ArrayList<PendingIntent>(messageCount);
        ArrayList<PendingIntent> sentIntents = new ArrayList<PendingIntent>(messageCount);
        for (int i = 0; i < messageCount; i++) {
            if (mRequestDeliveryReport && (i == (messageCount - 1))) {
                // TODO: Fix: It should not be necessary to
                // specify the class in this intent.  Doing that
                // unnecessarily limits customizability.
                /// M:Code analyze 005,change logic for gemini,add slotId info @{
                Intent intent = new Intent(
                                MessageStatusReceiver.MESSAGE_STATUS_RECEIVED_ACTION,
                                mUri,
                                mContext,
                                MessageStatusReceiver.class);
                if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                    intent.putExtra(EncapsulatedPhone.GEMINI_SIM_ID_KEY, slotId);
                }
                //the parameter is used now! not as the google doc says "currently not used"
                deliveryIntents.add(PendingIntent.getBroadcast(mContext, i, intent, 0));
                /// @}
            } else {
                deliveryIntents.add(null);
            }
            Intent intent  = new Intent(SmsReceiverService.MESSAGE_SENT_ACTION,
                    mUri,
                    mContext,
                    SmsReceiver.class);
            /// M:Code analyze 007, comment the line,using different requestCode for every sub_message @{
            //int requestCode = 0;
            /// @}
            if (i == messageCount - 1) {
                // Changing the requestCode so that a different pending intent
                // is created for the last fragment with
                // EXTRA_MESSAGE_SENT_SEND_NEXT set to true.
                /// M:Code analyze 007, comment the line,using different requestCode for every sub_message @{
                //requestCode = 1;
                /// @}
                intent.putExtra(SmsReceiverService.EXTRA_MESSAGE_SENT_SEND_NEXT, true);
            }

            /// M:Code analyze 008, add for concatenation msg @{
            if (messageCount > 1) {
                intent.putExtra(SmsReceiverService.EXTRA_MESSAGE_CONCATENATION, true);
            }
            /// @}
            if (LogTag.DEBUG_SEND) {
                Log.v(TAG, "sendMessage sendIntent: " + intent);
            }
            /// M:Code analyze 009, add slotId for gemini @{
            if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                intent.putExtra(EncapsulatedPhone.GEMINI_SIM_ID_KEY, slotId);
            }
            /// @}
            /// M:Code analyze 007, comment the line,using different requestCode for every sub_message @{
            sentIntents.add(PendingIntent.getBroadcast(mContext, i, intent, 0));
            /// @}
        }
        try {
            /// M:Code analyze 008, print log @{
            MmsLog.d(MmsApp.TXN_TAG, "\t Destination\t= " + mDest);
            MmsLog.d(MmsApp.TXN_TAG, "\t ServiceCenter\t= " + mServiceCenter);
            MmsLog.d(MmsApp.TXN_TAG, "\t Message\t= " + messages);
            MmsLog.d(MmsApp.TXN_TAG, "\t uri\t= " + mUri);
            MmsLog.d(MmsApp.TXN_TAG, "\t slotId\t= "+ slotId);
            MmsLog.d(MmsApp.TXN_TAG, "\t CodingType\t= " + codingType);
            /// @}

            /// M:Code analyze 009, modify logic for gemini,meantime,add a parameter codingType for
            /// sendMultipartTextMessageWithEncodingType to send multiparts messages@{
            if (EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT) {
                if (MmsConfig.getSmsValidityPeriodEnabled()) {
                    SharedPreferences spref = PreferenceManager.getDefaultSharedPreferences(mContext);
                    final String validityKey = Long.toString(slotId) + "_" + SmsPreferenceActivity.SMS_VALIDITY_PERIOD;
                    int vailidity = spref.getInt(validityKey, EncapsulatedSmsManager.VALIDITY_PERIOD_NO_DURATION);
                    Bundle extra = new Bundle();
                    extra.putInt(EncapsulatedSmsManager.EXTRA_PARAMS_VALIDITY_PERIOD, vailidity);
                    EncapsulatedGeminiSmsManager.sendMultipartTextMessageWithExtraParamsGemini(
                            mDest, mServiceCenter, messages, extra, slotId, sentIntents, deliveryIntents);
                } else {
                    EncapsulatedGeminiSmsManager.sendMultipartTextMessageWithEncodingTypeGemini(mDest, mServiceCenter, messages,
                            codingType, slotId/*mSimId*/, sentIntents, deliveryIntents);
                }
            } else {
                EncapsulatedSmsManager.sendMultipartTextMessageWithEncodingType(mDest, mServiceCenter, messages,
                        codingType, sentIntents, deliveryIntents);
            }
            /// @}
        } catch (Exception ex) {
            Log.e(TAG, "SmsMessageSender.sendMessage: caught", ex);
            throw new MmsException("SmsMessageSender.sendMessage: caught " + ex +
                    " from SmsManager.sendTextMessage()");
        }
        if (Log.isLoggable(LogTag.TRANSACTION, Log.VERBOSE) || LogTag.DEBUG_SEND) {
            log("sendMessage: address=" + mDest + ", threadId=" + mThreadId +
                    ", uri=" + mUri + ", msgs.count=" + messageCount);
        }
        return false;
    }

    private void log(String msg) {
        Log.d(LogTag.TAG, "[SmsSingleRecipientSender] " + msg);
    }
}
