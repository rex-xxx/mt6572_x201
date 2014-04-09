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

package com.android.providers.telephony;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.content.UriMatcher;

import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.Telephony;
import android.provider.Telephony.Mms;
import android.provider.Telephony.MmsSms;
import android.provider.Telephony.Sms;
import android.provider.Telephony.TextBasedSmsColumns;
import android.provider.Telephony.CanonicalAddressesColumns;
import android.provider.Telephony.ThreadsColumns;
import android.provider.Telephony.Threads;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.text.TextUtils;
import android.util.Config;
import android.util.Log;
import com.mediatek.encapsulation.MmsLog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.mediatek.encapsulation.com.mediatek.common.featureoption.EncapsulatedFeatureOption;
import com.mediatek.encapsulation.android.telephony.gemini.EncapsulatedGeminiSmsManager;
import com.mediatek.encapsulation.android.telephony.EncapsulatedSmsMessage;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.SmsHeader;
import com.google.android.mms.pdu.PduHeaders;
import com.mediatek.encapsulation.android.telephony.EncapsulatedTelephony.SIMInfo;
import com.mediatek.encapsulation.android.telephony.EncapsulatedTelephony;

public class SmsProvider extends ContentProvider {
    private static final Uri NOTIFICATION_URI = Uri.parse("content://sms");
    private static final Uri ICC_URI = Uri.parse("content://sms/icc");
    /// M: Code analyze 001, new feature, support for gemini.
    private static final Uri ICC_URI_GEMINI = Uri.parse("content://sms/icc2");
    private static final Uri ICC_URI_THREE = Uri.parse("content://sms/icc3");
    private static final Uri ICC_URI_FOUR = Uri.parse("content://sms/icc4");
    static final String TABLE_SMS = "sms";
    private static final String TABLE_RAW = "raw";
    private static final String TABLE_SR_PENDING = "sr_pending";
    private static final String TABLE_WORDS = "words";
    /// M: Code analyze 002, fix bug ALPS00046358, improve multi-delete speed by use batch
    /// processing. reference from page http://www.erpgear.com/show.php?contentid=1111.
    private static final String FOR_MULTIDELETE = "ForMultiDelete";
    /// M: Code analyze 003, fix bug ALPS00239521, ALPS00244682, improve multi-delete speed
    /// in folder mode.
    private static final String FOR_FOLDERMODE_MULTIDELETE = "ForFolderMultiDelete";
    private static final Integer ONE = Integer.valueOf(1);
    /// M: Code analyze 004, fix bug ALPS00064780, deleting sms is abnormal. use new FW
    /// interface while delete all messages on sim card.
    private static final String ALL_SMS = "999999";
    private static final int PERSON_ID_COLUMN = 0;
    /// M: Code analyze 005, fix bug ALPS00245352, it cost long time to restore messages.
    /// remove useless operation and add transaction while import sms. @{
    private static final int NORMAL_NUMBER_MAX_LENGTH = 15;
    private static final String[] CANONICAL_ADDRESSES_COLUMNS_2 =
    new String[] { CanonicalAddressesColumns._ID, CanonicalAddressesColumns.ADDRESS };
    /// @}
    /// M: Code analyze 006, fix bug ALPS00252799, it cost long time to restore messages.
    /// support batch processing while restore messages. @{
    /**
     * Maximum number of operations allowed in a batch
     */
    private static final int MAX_OPERATIONS_PER_PATCH = 50;
    /// @}
    /**
     * These are the columns that are available when reading SMS
     * messages from the ICC.  Columns whose names begin with "is_"
     * have either "true" or "false" as their values.
     */
    private final static String[] ICC_COLUMNS = new String[] {
        // N.B.: These columns must appear in the same order as the
        // calls to add appear in convertIccToSms.
        "service_center_address",       // getServiceCenterAddress
        "address",                      // getDisplayOriginatingAddress
        "message_class",                // getMessageClass
        "body",                         // getDisplayMessageBody
        "date",                         // getTimestampMillis
        "status",                       // getStatusOnIcc
        "index_on_icc",                 // getIndexOnIcc
        "is_status_report",             // isStatusReportMessage
        "transport_type",               // Always "sms".
        "type",                         // Always MESSAGE_TYPE_ALL.
        "locked",                       // Always 0 (false).
        "error_code",                   // Always 0
        "_id",
        /// M: Code analyze 007, fix bug ALPS00042403, should show the sender's number
        /// in manage SIM card. show concatenation sms in one bubble, set incoming sms
        /// on left and sent sms on right, display sender information for every sms.
        "sim_id"                        // sim id
    };
    /// M: Code analyze 008, fix bug ALPS00262044, not show out unread message
    /// icon after restore messages. notify mms application about unread messages
    /// number after insert operation.
    private static boolean notify = false;
    @Override
    public boolean onCreate() {
        mOpenHelper = MmsSmsDatabaseHelper.getInstance(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri url, String[] projectionIn, String selection,
            String[] selectionArgs, String sort) {
        MmsLog.d(TAG, "query begin uri = " + url);
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        // Generate the body of the query.
        int match = URI_MATCHER.match(url);
        switch (match) {
            case SMS_ALL:
                constructQueryForBox(qb, Sms.MESSAGE_TYPE_ALL);
                break;

            case SMS_UNDELIVERED:
                constructQueryForUndelivered(qb);
                break;

            case SMS_FAILED:
                constructQueryForBox(qb, Sms.MESSAGE_TYPE_FAILED);
                break;

            case SMS_QUEUED:
                constructQueryForBox(qb, Sms.MESSAGE_TYPE_QUEUED);
                break;

            case SMS_INBOX:
                constructQueryForBox(qb, Sms.MESSAGE_TYPE_INBOX);
                break;

            case SMS_SENT:
                constructQueryForBox(qb, Sms.MESSAGE_TYPE_SENT);
                break;

            case SMS_DRAFT:
                constructQueryForBox(qb, Sms.MESSAGE_TYPE_DRAFT);
                break;

            case SMS_OUTBOX:
                constructQueryForBox(qb, Sms.MESSAGE_TYPE_OUTBOX);
                break;

            case SMS_ALL_ID:
                qb.setTables(TABLE_SMS);
                qb.appendWhere("(_id = " + url.getPathSegments().get(0) + ")");
                break;

            case SMS_INBOX_ID:
            case SMS_FAILED_ID:
            case SMS_SENT_ID:
            case SMS_DRAFT_ID:
            case SMS_OUTBOX_ID:
                qb.setTables(TABLE_SMS);
                qb.appendWhere("(_id = " + url.getPathSegments().get(1) + ")");
                break;

            case SMS_CONVERSATIONS_ID:
                int threadID;

                try {
                    threadID = Integer.parseInt(url.getPathSegments().get(1));
                    if (Log.isLoggable(TAG, Log.VERBOSE)) {
                        Log.d(TAG, "query conversations: threadID=" + threadID);
                    }
                }
                catch (Exception ex) {
                    Log.e(TAG,
                          "Bad conversation thread id: "
                          + url.getPathSegments().get(1));
                    return null;
                }

                qb.setTables(TABLE_SMS);
                qb.appendWhere("thread_id = " + threadID);
                break;

            case SMS_CONVERSATIONS:
                qb.setTables("sms, (SELECT thread_id AS group_thread_id, MAX(date)AS group_date,"
                       + "COUNT(*) AS msg_count FROM sms GROUP BY thread_id) AS groups");
                qb.appendWhere("sms.thread_id = groups.group_thread_id AND sms.date ="
                       + "groups.group_date");
                qb.setProjectionMap(sConversationProjectionMap);
                break;

            case SMS_RAW_MESSAGE:
                qb.setTables("raw");
                break;

            case SMS_STATUS_PENDING:
                qb.setTables("sr_pending");
                break;

            case SMS_ATTACHMENT:
                qb.setTables("attachments");
                break;

            case SMS_ATTACHMENT_ID:
                qb.setTables("attachments");
                qb.appendWhere(
                        "(sms_id = " + url.getPathSegments().get(1) + ")");
                break;

            case SMS_QUERY_THREAD_ID:
                qb.setTables("canonical_addresses");
                if (projectionIn == null) {
                    projectionIn = sIDProjection;
                }
                break;

            case SMS_STATUS_ID:
                qb.setTables(TABLE_SMS);
                qb.appendWhere("(_id = " + url.getPathSegments().get(1) + ")");
                break;

            case SMS_ALL_ICC:
                /// M: Code analyze 009, new feature, support for gemini and plugin.
                /// use a flag "showInOne" indicate show long sms in one bubble or not. @{
                return getAllMessagesFromIcc(url, PhoneConstants.GEMINI_SIM_1);
                /// @}
            /// M: Code analyze 010, new feature, support for gemini. @{
            case SMS_ICC:
                String messageIndexString = url.getPathSegments().get(1);
                return getSingleMessageFromIcc(messageIndexString, PhoneConstants.GEMINI_SIM_1);

            case SMS_ALL_ICC_GEMINI:
                /// M: Code analyze 009, new feature, support for plugin.
                /// use a flag "showInOne" indicate show long sms in one bubble or not.
                return getAllMessagesFromIcc(url, PhoneConstants.GEMINI_SIM_2);

            case SMS_ICC_GEMINI:
                String messageIndexString_1 = url.getPathSegments().get(1);

                return getSingleMessageFromIcc(messageIndexString_1, PhoneConstants.GEMINI_SIM_2);
            case SMS_ALL_ICC_THREE:
                return getAllMessagesFromIcc(url, PhoneConstants.GEMINI_SIM_3);
            case SMS_ICC_THREE:
                String messageIndexString_2 = url.getPathSegments().get(1);

                return getSingleMessageFromIcc(messageIndexString_2, PhoneConstants.GEMINI_SIM_3);
            case SMS_ALL_ICC_FOUR:
                return getAllMessagesFromIcc(url, PhoneConstants.GEMINI_SIM_4);
            case SMS_ICC_FOUR:
                String messageIndexString_3 = url.getPathSegments().get(1);

                return getSingleMessageFromIcc(messageIndexString_3, PhoneConstants.GEMINI_SIM_4);
            /// @}
            /// M: Code analyze 011, fix bug ALPS00282321, ANR while delete old messages.
            /// use new process of delete. @{
            case SMS_ALL_THREADID:
                /// M: return all the distinct threadid from sms table
                return getAllSmsThreadIds(selection, selectionArgs);
            /// @}
            default:
                Log.e(TAG, "Invalid request: " + url);
                return null;
        }

        String orderBy = null;

        if (!TextUtils.isEmpty(sort)) {
            orderBy = sort;
        } else if (qb.getTables().equals(TABLE_SMS)) {
            orderBy = Sms.DEFAULT_SORT_ORDER;
        }
        MmsLog.d(TAG, "query getReadbleDatabase");
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        MmsLog.d(TAG, "query getReadbleDatabase qb.query begin");
        Cursor ret = qb.query(db, projectionIn, selection, selectionArgs,
                              null, null, orderBy);
        MmsLog.d(TAG, "query getReadbleDatabase qb.query end");
        // TODO: Since the URLs are a mess, always use content://sms
        ret.setNotificationUri(getContext().getContentResolver(),
                NOTIFICATION_URI);
        return ret;
    }
    /// M: Code analyze 007, fix bug ALPS00042403, should show the sender's number
    /// in manage SIM card. show concatenation sms in one bubble, set incoming sms
    /// on left and sent sms on right, display sender information for every sms. @{
    private Object[] convertIccToSms(SmsMessage message, 
            ArrayList<String> concatSmsIndexAndBody, int id, 
            int simId) {
        // N.B.: These calls must appear in the same order as the
        // columns appear in ICC_COLUMNS.
        Object[] row = new Object[14];
        row[0] = message.getServiceCenterAddress();
        
        // check message status and set address
        if ((message.getStatusOnIcc() == SmsManager.STATUS_ON_ICC_READ) ||
               (message.getStatusOnIcc() == SmsManager.STATUS_ON_ICC_UNREAD)) {
            row[1] = message.getDisplayOriginatingAddress();
        } else {
            row[1] = EncapsulatedSmsMessage.getDestinationAddress(message);
        }

        String concatSmsIndex = null;
        String concatSmsBody = null;
        if (null != concatSmsIndexAndBody) {
            concatSmsIndex = concatSmsIndexAndBody.get(0);
            concatSmsBody = concatSmsIndexAndBody.get(1);
        }
        
        row[2] = String.valueOf(message.getMessageClass());
        row[3] = concatSmsBody == null ? message.getDisplayMessageBody() : concatSmsBody;
        row[4] = message.getTimestampMillis();
        row[5] = message.getStatusOnIcc();
        row[6] = concatSmsIndex == null ? message.getIndexOnIcc() : concatSmsIndex;
        row[7] = message.isStatusReportMessage();
        row[8] = "sms";
        row[9] = TextBasedSmsColumns.MESSAGE_TYPE_ALL;
        row[10] = 0;      // locked
        row[11] = 0;      // error_code
        row[12] = id;
        row[13] = simId;
        return row;
    }
    /// @}
    /**
     * Return a Cursor containing just one message from the ICC.
     */
    private Cursor getSingleMessageFromIcc(String messageIndexString) {
        try {
            int messageIndex = Integer.parseInt(messageIndexString);
            SmsManager smsManager = SmsManager.getDefault();
            ArrayList<SmsMessage> messages = smsManager.getAllMessagesFromIcc();
            /// M: Code analyze 012, unknown, check if "messages" is valid. @{
            if (messages == null || messages.isEmpty()) {
                MmsLog.e(TAG, "getSingleMessageFromIcc messages is null");
                return null;
            }
            /// @}
            SmsMessage message = messages.get(messageIndex);
            if (message == null) {
                throw new IllegalArgumentException(
                        "Message not retrieved. ID: " + messageIndexString);
            }
            MatrixCursor cursor = new MatrixCursor(ICC_COLUMNS, 1);
            /// M: Code analyze 013, unknown, maybe support for gemini.
            cursor.addRow(convertIccToSms(message, 0, 0));
            return withIccNotificationUri(cursor);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Bad SMS ICC ID: " + messageIndexString);
        }
    }

    /**
     * Return a Cursor listing all the messages stored on the ICC.
     */
    /// M: Code analyze 007, fix bug ALPS00042403, should show the sender's number
    /// in manage SIM card. show concatenation sms in one bubble, set incoming sms
    /// on left and sent sms on right, display sender information for every sms. @{
    private Cursor getAllMessagesFromIcc(Uri url) {
        SmsManager smsManager = SmsManager.getDefault();
        ArrayList<SmsMessage> messages = smsManager.getAllMessagesFromIcc();
        /// M: Code analyze 012, unknown, check if "messages" is valid. @{
        if (messages == null || messages.isEmpty()) {
            MmsLog.e(TAG, "getAllMessagesFromIcc messages is null");
            return null;
        }
        /// @}
        final int count = messages.size();
        MatrixCursor cursor = new MatrixCursor(ICC_COLUMNS, count);
        ArrayList<String> concatSmsIndexAndBody = null;
        /// M: Code analyze 009, new feature, support for gemini and plugin.
        /// use a flag "showInOne" indicate show long sms in one bubble or not.
        boolean showInOne = "1".equals(url.getQueryParameter("showInOne"));

        for (int i = 0; i < count; i++) {
            concatSmsIndexAndBody = null;
            SmsMessage message = messages.get(i);
            if (message != null) {
                if (showInOne) {
                    SmsHeader smsHeader = EncapsulatedSmsMessage.getUserDataHeader(message);
                    if (null != smsHeader && null != smsHeader.concatRef) {
                        concatSmsIndexAndBody = getConcatSmsIndexAndBody(messages, i);
                    }
                }
               cursor.addRow(convertIccToSms(message, concatSmsIndexAndBody, i, -1));
            }
        }
        return withIccNotificationUri(cursor);
    }
    /// @}
    private Cursor withIccNotificationUri(Cursor cursor) {
        cursor.setNotificationUri(getContext().getContentResolver(), ICC_URI);
        return cursor;
    }

    private void constructQueryForBox(SQLiteQueryBuilder qb, int type) {
        qb.setTables(TABLE_SMS);

        if (type != Sms.MESSAGE_TYPE_ALL) {
            qb.appendWhere("type=" + type);
        }
    }

    private void constructQueryForUndelivered(SQLiteQueryBuilder qb) {
        qb.setTables(TABLE_SMS);

        qb.appendWhere("(type=" + Sms.MESSAGE_TYPE_OUTBOX +
                       " OR type=" + Sms.MESSAGE_TYPE_FAILED +
                       " OR type=" + Sms.MESSAGE_TYPE_QUEUED + ")");
    }

    @Override
    public String getType(Uri url) {
        switch (url.getPathSegments().size()) {
        case 0:
            return VND_ANDROID_DIR_SMS;
            case 1:
                try {
                    Integer.parseInt(url.getPathSegments().get(0));
                    return VND_ANDROID_SMS;
                } catch (NumberFormatException ex) {
                    return VND_ANDROID_DIR_SMS;
                }
            case 2:
                // TODO: What about "threadID"?
                if (url.getPathSegments().get(0).equals("conversations")) {
                    return VND_ANDROID_SMSCHAT;
                } else {
                    return VND_ANDROID_SMS;
                }
        }
        return null;
    }

    @Override
    public Uri insert(Uri url, ContentValues initialValues) {
        MmsLog.d(TAG, "insert begin");
        ContentValues values;
        long rowID;
        int type = Sms.MESSAGE_TYPE_ALL;
        /// M: Code analyze 005, fix bug ALPS00245352, it cost long time to restore messages.
        /// remove useless operation and add transaction while import sms. @{
        /// M: for import sms only
        boolean importSms = false;
        /// @}
        int match = URI_MATCHER.match(url);
        String table = TABLE_SMS;
        /// M: Code analyze 002, fix bug ALPS00262044, not show out unread message
        /// icon after restore messages. notify mms application about unread messages
        /// number after insert operation. @{
        /// M: do not notify the launcher to show unread message, if there are lots of operations
        notify = true;
        /// @}
        switch (match) {
            case SMS_ALL:
                Integer typeObj = initialValues.getAsInteger(Sms.TYPE);
                if (typeObj != null) {
                    type = typeObj.intValue();
                } else {
                    // default to inbox
                    type = Sms.MESSAGE_TYPE_INBOX;
                }
                break;

            case SMS_INBOX:
                type = Sms.MESSAGE_TYPE_INBOX;
                break;

            case SMS_FAILED:
                type = Sms.MESSAGE_TYPE_FAILED;
                break;

            case SMS_QUEUED:
                type = Sms.MESSAGE_TYPE_QUEUED;
                break;

            case SMS_SENT:
                type = Sms.MESSAGE_TYPE_SENT;
                break;

            case SMS_DRAFT:
                type = Sms.MESSAGE_TYPE_DRAFT;
                break;

            case SMS_OUTBOX:
                type = Sms.MESSAGE_TYPE_OUTBOX;
                break;

            case SMS_RAW_MESSAGE:
                table = "raw";
                break;

            case SMS_STATUS_PENDING:
                table = "sr_pending";
                break;

            case SMS_ATTACHMENT:
                table = "attachments";
                break;

            case SMS_NEW_THREAD_ID:
                table = "canonical_addresses";
                break;

            default:
                Log.e(TAG, "Invalid request: " + url);
                return null;
        }
        MmsLog.d(TAG, "insert match url end"); 
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        MmsLog.d(TAG, "insert mOpenHelper.getWritableDatabase end");
        db.beginTransaction();
        try {
            if (table.equals(TABLE_SMS)) {
                boolean addDate = false;
                boolean addType = false;

                // Make sure that the date and type are set
                if (initialValues == null) {
                    values = new ContentValues(1);
                    addDate = true;
                    addType = true;
                } else {
                    values = new ContentValues(initialValues);

                    if (!initialValues.containsKey(Sms.DATE)) {
                        addDate = true;
                    }

                    if (!initialValues.containsKey(Sms.TYPE)) {
                        addType = true;
                    }
                    /// M: Code analyze 005, fix bug ALPS00245352, it cost long time to restore messages.
                    /// remove useless operation and add transaction while import sms. @{
                    if (initialValues.containsKey("import_sms")) {
                        importSms = true;
                        values.remove("import_sms");
                    }
                    /// @}
                }

                if (addDate) {
                    values.put(Sms.DATE, new Long(System.currentTimeMillis()));
                /// M: Code analyze 014, fix bug ALPS00114870, messages' time were abnormal after restored.
                /// set the date as the right value when import.
                } else {
                    Long date = values.getAsLong(Sms.DATE);
                    values.put(Sms.DATE, date);
                    MmsLog.d(TAG, "insert sms date "+ date);
                /// @}
                }

                if (addType && (type != Sms.MESSAGE_TYPE_ALL)) {
                    values.put(Sms.TYPE, Integer.valueOf(type));
                }

                // thread_id
                Long threadId = values.getAsLong(Sms.THREAD_ID);
                String address = values.getAsString(Sms.ADDRESS);

                if (((threadId == null) || (threadId == 0)) && (!TextUtils.isEmpty(address))) {
                    /// M: Code analyze 005, fix bug ALPS00245352, it cost long time to restore messages.
                    /// remove useless operation and add transaction while import sms. @{
                    MmsLog.d(TAG, "insert sms getThreadId start");
                    long id = 0;
//                    if (importSms){
                        id = getThreadIdInternal(address, db);
//                    } else {
//                        id = EncapsulatedTelephony.Threads.getOrCreateThreadIdInternal(getContext(), address);
//                    }
                    values.put(Sms.THREAD_ID, id);
                    MmsLog.d(TAG, "insert getContentResolver getOrCreateThreadId end id = " + id);
                    /// @}
                }

                // If this message is going in as a draft, it should replace any
                // other draft messages in the thread.  Just delete all draft
                // messages with this thread ID.  We could add an OR REPLACE to
                // the insert below, but we'd have to query to find the old _id
                // to produce a conflict anyway.
                if (values.getAsInteger(Sms.TYPE) == Sms.MESSAGE_TYPE_DRAFT) {
                    db.delete(TABLE_SMS, "thread_id=? AND type=?",
                            new String[] { values.getAsString(Sms.THREAD_ID),
                                           Integer.toString(Sms.MESSAGE_TYPE_DRAFT) });
                }
                /// M: Code analyze 006, fix bug ALPS00252799, it cost long time to restore messages.
                /// support batch processing while restore messages. @{
                if (type != Sms.MESSAGE_TYPE_INBOX) {
                    values.put(Sms.READ, ONE);
                }
                if (!values.containsKey(Sms.PERSON_ID)) {
                    values.put(Sms.PERSON_ID, 0);
                }
                /// @}
            } else {
                if (initialValues == null) {
                    values = new ContentValues(1);
                } else {
                    values = initialValues;
                }
            }

            rowID = db.insert(table, "body", values);
            /// M: Code analyze 005, fix bug ALPS00245352, it cost long time to restore messages.
            /// remove useless operation and add transaction while import sms. @{
            MmsLog.d(TAG, "insert table body end");
            if (!importSms){
                /// M: Code analyze 015, fix bug ALPS00234074, do not delete the thread
                /// when it is in writiing status.
                setThreadStatus(db, values, 0);
            }
            /// @}
            // Don't use a trigger for updating the words table because of a bug
            // in FTS3.  The bug is such that the call to get the last inserted
            // row is incorrect.
            if (table == TABLE_SMS) {
                // Update the words table with a corresponding row.  The words table
                // allows us to search for words quickly, without scanning the whole
                // table;
                MmsLog.d(TAG, "insert TABLE_WORDS begin");
                ContentValues cv = new ContentValues();
                cv.put(Telephony.MmsSms.WordsTable.ID, rowID);
                cv.put(Telephony.MmsSms.WordsTable.INDEXED_TEXT, values.getAsString("body"));
                cv.put(Telephony.MmsSms.WordsTable.SOURCE_ROW_ID, rowID);
                cv.put(Telephony.MmsSms.WordsTable.TABLE_ID, 1);
                db.insert(TABLE_WORDS, Telephony.MmsSms.WordsTable.INDEXED_TEXT, cv);
                MmsLog.d(TAG, "insert TABLE_WORDS end");
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            MmsLog.d(TAG, "insert sms transacton end");
        }
        /// M: Code analyze 002, fix bug ALPS00262044, not show out unread message
        /// icon after restore messages. notify mms application about unread messages
        /// number after insert operation. @{
        if (rowID > 0) {
            Uri uri = Uri.parse("content://" + table + "/" + rowID);

            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.d(TAG, "insert " + uri + " succeeded");
            }
            //now notify the launcher to show unread message.
            notify = false;
            /// M: Code analyze 005, fix bug ALPS00245352, it cost long time to restore messages.
            /// remove useless operation and add transaction while import sms. @{
            if (!importSms){
                notifyChange2(uri);
            }
            /// @}
            return uri;
        } else {
            notify = false;
            Log.e(TAG,"insert: failed! " + values.toString());
        }
        /// @}
        MmsLog.d(TAG, "insert end");
        return null;
    }
    /// M: Code analyze 002, fix bug ALPS00046358, improve multi-delete speed by use batch
    /// processing. reference from page http://www.erpgear.com/show.php?contentid=1111. @{
    @Override
    public int delete(Uri url, String where, String[] whereArgs) {
        int deletedRows = 0;
        Uri deleteUri = null;
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        if (where != null) {
            if (where.equals(FOR_MULTIDELETE)) {
                MmsLog.d(TAG, "delete FOR_MULTIDELETE");
                db.beginTransaction();
                int message_id = 0;
                deletedRows = 0;
                for (int i=0; i<whereArgs.length; i++) {
                    deleteUri = null;
                    if (whereArgs[i] == null) {
                        //return deletedRows;
                    } else {
                        message_id = Integer.parseInt(whereArgs[i]);
                        deleteUri = ContentUris.withAppendedId(url, message_id);
                        Log.i(TAG, "message_id is " + message_id);
                        deletedRows += deleteOnce(deleteUri, null, null);
                    }
                }
                db.setTransactionSuccessful();
                db.endTransaction();
            /// M: Code analyze 003, fix bug ALPS00239521, ALPS00244682, improve multi-delete speed
            /// in folder mode. @{
            } else if (where.equals(FOR_FOLDERMODE_MULTIDELETE)){
                MmsLog.d(TAG, "delete folder mode FOR_MULTIDELETE");
                String boxType = "0";
                if(whereArgs != null && whereArgs.length > 0){
                    if ("4".equals(whereArgs[whereArgs.length-1])) {
                        boxType = "(" + Sms.MESSAGE_TYPE_OUTBOX + "," +
                        Sms.MESSAGE_TYPE_FAILED + "," + Sms.MESSAGE_TYPE_QUEUED +")"; //outbox,failed,queue
                    } else {
                        boxType = "(" + whereArgs[whereArgs.length-1] + ")";
                    }
                }
                String unSelectids = getSmsIds(whereArgs);
                String finalSelection = String.format("type IN %s AND _id NOT IN %s", boxType, unSelectids);
                MmsLog.d(TAG, "delete folder mode FOR_MULTIDELETE finalSelection = "+ finalSelection);
                String unSelectWordids = getWordIds(db, boxType, unSelectids);
                db.execSQL(String.format("delete from words where _id NOT IN %s AND table_to_use=1", unSelectWordids));
                deletedRows = db.delete(TABLE_SMS, finalSelection, null);
                MmsLog.d(TAG, "delete folder mode FOR_MULTIDELETE end unSelectids " + unSelectids);
            /// @}
            } else {
                deletedRows = deleteOnce(url, where, whereArgs);
            }
        } else {
            deletedRows = deleteOnce(url, where, whereArgs);
        }
        if (deletedRows > 0) {
            notifyChange(url);
            /// M: Code analyze 003, fix bug ALPS00239521, ALPS00244682, improve multi-delete speed
            /// in folder mode.
            getContext().getContentResolver().delete(Threads.OBSOLETE_THREADS_URI, null, null);
        }
        return deletedRows;
    }
    /// @}
    /**
     * Delete the message at index from ICC.  Return true iff
     * successful.
     */
    private int deleteMessageFromIcc(String messageIndexString) {
        SmsManager smsManager = SmsManager.getDefault();

        try {
            /// M: Code analyze 004, fix bug ALPS00064780, deleting sms is abnormal. use new FW
            /// interface while delete all messages on sim card. @{
            /// M: For Delete all,MTK FW support delete all using messageIndex = -1;
            if (messageIndexString.equals(ALL_SMS)) {
                return smsManager.deleteMessageFromIcc(-1) ? 1 : 0;
            } else {
                return smsManager.deleteMessageFromIcc(
                        Integer.parseInt(messageIndexString)) ? 1 : 0;
            }
            /// @}
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Bad SMS ICC ID: " + messageIndexString);
        } finally {
            /// M: no need to notify change
        }
    }

    @Override
    public int update(Uri url, ContentValues values, String where, String[] whereArgs) {
        MmsLog.d(TAG, "update begin");
        int count = 0;
        String table = TABLE_SMS;
        String extraWhere = null;
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        switch (URI_MATCHER.match(url)) {
            case SMS_RAW_MESSAGE:
                table = TABLE_RAW;
                break;

            case SMS_STATUS_PENDING:
                table = TABLE_SR_PENDING;
                break;

            case SMS_ALL:
            case SMS_FAILED:
            case SMS_QUEUED:
            case SMS_INBOX:
            case SMS_SENT:
            case SMS_DRAFT:
            case SMS_OUTBOX:
            case SMS_CONVERSATIONS:
                break;

            case SMS_ALL_ID:
                extraWhere = "_id=" + url.getPathSegments().get(0);
                break;

            case SMS_INBOX_ID:
            case SMS_FAILED_ID:
            case SMS_SENT_ID:
            case SMS_DRAFT_ID:
            case SMS_OUTBOX_ID:
                extraWhere = "_id=" + url.getPathSegments().get(1);
                break;

            case SMS_CONVERSATIONS_ID: {
                String threadId = url.getPathSegments().get(1);

                try {
                    Integer.parseInt(threadId);
                } catch (Exception ex) {
                    Log.e(TAG, "Bad conversation thread id: " + threadId);
                    break;
                }

                extraWhere = "thread_id=" + threadId;
                break;
            }

            case SMS_STATUS_ID:
                extraWhere = "_id=" + url.getPathSegments().get(1);
                break;

            default:
                throw new UnsupportedOperationException(
                        "URI " + url + " not supported");
        }

        where = DatabaseUtils.concatenateWhere(where, extraWhere);
        count = db.update(table, values, where, whereArgs);

        if (count > 0) {
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.d(TAG, "update " + url + " succeeded");
            }
            notifyChange(url);
        }
        MmsLog.d(TAG, "update end");
        return count;
    }

    private void notifyChange(Uri uri) {
        ContentResolver cr = getContext().getContentResolver();
        cr.notifyChange(uri, null);
        cr.notifyChange(MmsSms.CONTENT_URI, null);
        cr.notifyChange(Uri.parse("content://mms-sms/conversations/"), null);
        /// M: Code analyze 002, fix bug ALPS00262044, not show out unread message
        /// icon after restore messages. notify mms application about unread messages
        /// number after insert operation.
        MmsLog.d(TAG, "SmsProvider notifyChange");
        MmsSmsProvider.notifyUnreadMessageNumberChanged(getContext());
    }

    private SQLiteOpenHelper mOpenHelper;

    private final static String TAG = "SmsProvider";
    private final static String VND_ANDROID_SMS = "vnd.android.cursor.item/sms";
    private final static String VND_ANDROID_SMSCHAT =
            "vnd.android.cursor.item/sms-chat";
    private final static String VND_ANDROID_DIR_SMS =
            "vnd.android.cursor.dir/sms";

    private static final HashMap<String, String> sConversationProjectionMap =
            new HashMap<String, String>();
    private static final String[] sIDProjection = new String[] { "_id" };

    private static final int SMS_ALL = 0;
    private static final int SMS_ALL_ID = 1;
    private static final int SMS_INBOX = 2;
    private static final int SMS_INBOX_ID = 3;
    private static final int SMS_SENT = 4;
    private static final int SMS_SENT_ID = 5;
    private static final int SMS_DRAFT = 6;
    private static final int SMS_DRAFT_ID = 7;
    private static final int SMS_OUTBOX = 8;
    private static final int SMS_OUTBOX_ID = 9;
    private static final int SMS_CONVERSATIONS = 10;
    private static final int SMS_CONVERSATIONS_ID = 11;
    private static final int SMS_RAW_MESSAGE = 15;
    private static final int SMS_ATTACHMENT = 16;
    private static final int SMS_ATTACHMENT_ID = 17;
    private static final int SMS_NEW_THREAD_ID = 18;
    private static final int SMS_QUERY_THREAD_ID = 19;
    private static final int SMS_STATUS_ID = 20;
    private static final int SMS_STATUS_PENDING = 21;
    private static final int SMS_ALL_ICC = 22;
    private static final int SMS_ICC = 23;
    private static final int SMS_FAILED = 24;
    private static final int SMS_FAILED_ID = 25;
    private static final int SMS_QUEUED = 26;
    private static final int SMS_UNDELIVERED = 27;
    /// M: Code analyze 010, new feature, support for gemini. @{
    private static final int SMS_ALL_ICC_GEMINI = 28;
    private static final int SMS_ALL_ICC_THREE = 29;
    private static final int SMS_ALL_ICC_FOUR = 30;
    private static final int SMS_ICC_GEMINI = 31;
    private static final int SMS_ICC_THREE = 32;
    private static final int SMS_ICC_FOUR = 33;
    /// @}
    /// M: Code analyze 011, fix bug ALPS00282321, ANR while delete old messages.
    /// use new process of delete. @{
    private static final int SMS_ALL_THREADID = 34;
    private static final int SMS_AUTO_DELETE  = 35;
    /// @}
    private static final UriMatcher URI_MATCHER =
            new UriMatcher(UriMatcher.NO_MATCH);

    static {
        URI_MATCHER.addURI("sms", null, SMS_ALL);
        URI_MATCHER.addURI("sms", "#", SMS_ALL_ID);
        URI_MATCHER.addURI("sms", "inbox", SMS_INBOX);
        URI_MATCHER.addURI("sms", "inbox/#", SMS_INBOX_ID);
        URI_MATCHER.addURI("sms", "sent", SMS_SENT);
        URI_MATCHER.addURI("sms", "sent/#", SMS_SENT_ID);
        URI_MATCHER.addURI("sms", "draft", SMS_DRAFT);
        URI_MATCHER.addURI("sms", "draft/#", SMS_DRAFT_ID);
        URI_MATCHER.addURI("sms", "outbox", SMS_OUTBOX);
        URI_MATCHER.addURI("sms", "outbox/#", SMS_OUTBOX_ID);
        URI_MATCHER.addURI("sms", "undelivered", SMS_UNDELIVERED);
        URI_MATCHER.addURI("sms", "failed", SMS_FAILED);
        URI_MATCHER.addURI("sms", "failed/#", SMS_FAILED_ID);
        URI_MATCHER.addURI("sms", "queued", SMS_QUEUED);
        URI_MATCHER.addURI("sms", "conversations", SMS_CONVERSATIONS);
        URI_MATCHER.addURI("sms", "conversations/*", SMS_CONVERSATIONS_ID);
        URI_MATCHER.addURI("sms", "raw", SMS_RAW_MESSAGE);
        URI_MATCHER.addURI("sms", "attachments", SMS_ATTACHMENT);
        URI_MATCHER.addURI("sms", "attachments/#", SMS_ATTACHMENT_ID);
        URI_MATCHER.addURI("sms", "threadID", SMS_NEW_THREAD_ID);
        URI_MATCHER.addURI("sms", "threadID/*", SMS_QUERY_THREAD_ID);
        URI_MATCHER.addURI("sms", "status/#", SMS_STATUS_ID);
        URI_MATCHER.addURI("sms", "sr_pending", SMS_STATUS_PENDING);
        URI_MATCHER.addURI("sms", "icc", SMS_ALL_ICC);
        URI_MATCHER.addURI("sms", "icc/#", SMS_ICC);
        /// M: we keep these for not breaking old applications
        URI_MATCHER.addURI("sms", "sim", SMS_ALL_ICC);
        URI_MATCHER.addURI("sms", "sim/#", SMS_ICC);
        /// M: Code analyze 010, new feature, support for gemini. @{
        URI_MATCHER.addURI("sms", "icc2", SMS_ALL_ICC_GEMINI);
        URI_MATCHER.addURI("sms", "icc2/#", SMS_ICC_GEMINI);
        /// M: we keep these for not breaking old applications
        URI_MATCHER.addURI("sms", "sim2", SMS_ALL_ICC_GEMINI);
        URI_MATCHER.addURI("sms", "sim2/#", SMS_ICC_GEMINI);
        URI_MATCHER.addURI("sms", "icc3", SMS_ALL_ICC_THREE);
        URI_MATCHER.addURI("sms", "icc3/#", SMS_ICC_THREE);
        URI_MATCHER.addURI("sms", "sim3", SMS_ALL_ICC_THREE);
        URI_MATCHER.addURI("sms", "sim3/#", SMS_ICC_THREE);
        URI_MATCHER.addURI("sms", "icc4", SMS_ALL_ICC_FOUR);
        URI_MATCHER.addURI("sms", "icc4/#", SMS_ICC_FOUR);
        URI_MATCHER.addURI("sms", "sim4", SMS_ALL_ICC_FOUR);
        URI_MATCHER.addURI("sms", "sim4/#", SMS_ICC_FOUR);
        /// @}
        /// M: Code analyze 011, fix bug ALPS00282321, ANR while delete old messages.
        /// use new process of delete. @{
        URI_MATCHER.addURI("sms", "all_threadid", SMS_ALL_THREADID);
        URI_MATCHER.addURI("sms", "auto_delete/#", SMS_AUTO_DELETE);
        /// @}
        sConversationProjectionMap.put(Sms.Conversations.SNIPPET,
            "sms.body AS snippet");
        sConversationProjectionMap.put(Sms.Conversations.THREAD_ID,
            "sms.thread_id AS thread_id");
        sConversationProjectionMap.put(Sms.Conversations.MESSAGE_COUNT,
            "groups.msg_count AS msg_count");
        sConversationProjectionMap.put("delta", null);
    }
    /// M: Code analyze 007, fix bug ALPS00042403, should show the sender's number
    /// in manage SIM card. show concatenation sms in one bubble, set incoming sms
    /// on left and sent sms on right, display sender information for every sms. @{
    private Object[] convertIccToSms(SmsMessage message, ArrayList<String> concatSmsIndexAndBody) {
        return convertIccToSms(message, concatSmsIndexAndBody, -1, -1);
    }

    private Object[] convertIccToSms(SmsMessage message,int id, int simId) {
        return convertIccToSms(message, null,id, simId);
    }

    private Object[] convertIccToSms(SmsMessage message) {
        return convertIccToSms(message, null, -1, -1);
    }
    /// @}
    /// M: Code analyze 010, new feature, support for gemini.
    /// fix bug ALPS00042403, show the sender's number in manage SIM card. @{
    /**
     * Return a Cursor containing just one message from the ICC.
     */
    private Cursor getSingleMessageFromIcc(String messageIndexString, int slotId) {
        try {
            int messageIndex = Integer.parseInt(messageIndexString);
            ArrayList<SmsMessage> messages = EncapsulatedGeminiSmsManager.getAllMessagesFromIccGemini(slotId);
            if (messages == null || messages.isEmpty()) {
                MmsLog.e(TAG, "getSingleMessageFromIcc messages is null");
                return null;
            }
            MatrixCursor cursor = new MatrixCursor(ICC_COLUMNS, 1);
            SmsMessage message = messages.get(messageIndex);
            if (message == null) {
                throw new IllegalArgumentException(
                        "Message not retrieved. ID: " + messageIndexString);
            }

            /// M: convert slotId to simId
            SIMInfo si = SIMInfo.getSIMInfoBySlot(getContext(), slotId);
            if (null == si) {
                MmsLog.e(TAG, "getSingleMessageFromIcc:SIMInfo is null for slot " + slotId);
                return null;
            }
            cursor.addRow(convertIccToSms(message, 0, (int)si.getSimId()));
            return withIccNotificationUri(cursor, slotId);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Bad SMS ICC ID: " + messageIndexString);
        }
    }
    /// @}
    /// M: Code analyze 010, new feature, support for gemini.
    /// fix bug ALPS00042403, show the sender's number in manage SIM card. @{
    private Cursor getAllMessagesFromIcc(Uri url, int slotId) {
        MmsLog.d(TAG, "getAllMessagesFromIcc slotId =" + slotId);
        ArrayList<SmsMessage> messages = EncapsulatedGeminiSmsManager.getAllMessagesFromIccGemini(slotId);
        if (messages == null || messages.isEmpty()) {
            MmsLog.e(TAG, "getAllMessagesFromIcc messages is null");
            return null;
        }
        ArrayList<String> concatSmsIndexAndBody = null;

        /// M: convert slotId to simId
        SIMInfo si = SIMInfo.getSIMInfoBySlot(getContext(), slotId);
        if (null == si) {
            MmsLog.d(TAG, "getSingleMessageFromIcc:SIMInfo is null for slot " + slotId);
            return null;
        }
        int count = messages.size();
        MatrixCursor cursor = new MatrixCursor(ICC_COLUMNS, count);
        /// M: Code analyze 009, new feature, support for gemini and plugin.
        /// use a flag "showInOne" indicate show long sms in one bubble or not.
        boolean showInOne = "1".equals(url.getQueryParameter("showInOne"));

        for (int i = 0; i < count; i++) {
            concatSmsIndexAndBody = null;
            SmsMessage message = messages.get(i);
            if (message != null) {
                if (showInOne) {
                    SmsHeader smsHeader = EncapsulatedSmsMessage.getUserDataHeader(message);
                    if (null != smsHeader && null != smsHeader.concatRef) {
                        concatSmsIndexAndBody = getConcatSmsIndexAndBody(messages, i);
                    }
                }
                cursor.addRow(convertIccToSms(message, concatSmsIndexAndBody, i, (int)si.getSimId()));
            }
        }
        return withIccNotificationUri(cursor, slotId);
    }
    /// @}
    /// M: Code analyze 010, new feature, support for gemini.
    /// fix bug ALPS00042403, show the sender's number in manage SIM card. @{
    private Cursor withIccNotificationUri(Cursor cursor, int slotId) {
        if(slotId == PhoneConstants.GEMINI_SIM_1) {
            cursor.setNotificationUri(getContext().getContentResolver(), ICC_URI);
        }
        else {
            cursor.setNotificationUri(getContext().getContentResolver(), Uri.parse("content://sms/icc" + slotId));
        }
        return cursor;
    }
    /// @}
    /// M: Code analyze 007, fix bug ALPS00042403, should show the sender's number
    /// in manage SIM card. show concatenation sms in one bubble, set incoming sms
    /// on left and sent sms on right, display sender information for every sms. @{
    private ArrayList<String> getConcatSmsIndexAndBody(ArrayList<SmsMessage> messages, int index) {
        int totalCount = messages.size();
        int refNumber = 0;
        int msgCount = 0;
        ArrayList<String> indexAndBody = new ArrayList<String>();
        StringBuilder smsIndex = new StringBuilder();
        StringBuilder smsBody = new StringBuilder();
        ArrayList<SmsMessage> concatMsg = null;
        SmsMessage message = messages.get(index);
        if (message != null) {
            SmsHeader smsHeader = EncapsulatedSmsMessage.getUserDataHeader(message);
            if (null != smsHeader && null != smsHeader.concatRef) {
                msgCount = smsHeader.concatRef.msgCount;
                refNumber = smsHeader.concatRef.refNumber;
            }
        }

        concatMsg = new ArrayList<SmsMessage>();
        concatMsg.add(message);

        for (int i = index + 1; i < totalCount; i++) {
            SmsMessage sms = messages.get(i);
            if (sms != null) {
                SmsHeader smsHeader = EncapsulatedSmsMessage.getUserDataHeader(sms);
                if (null != smsHeader && null != smsHeader.concatRef && refNumber == smsHeader.concatRef.refNumber) {
                    concatMsg.add(sms);
                    messages.set(i, null);
                    if (msgCount == concatMsg.size()) {
                        break;
                    }
                }
            }
        }

        int concatCount = concatMsg.size();
        for (int k = 0; k < msgCount; k++) {
            for (int j = 0; j < concatCount; j++) {
                SmsMessage sms = concatMsg.get(j);
                SmsHeader smsHeader = EncapsulatedSmsMessage.getUserDataHeader(sms);
                if (k == smsHeader.concatRef.seqNumber -1) {
                    smsIndex.append(sms.getIndexOnIcc());
                    smsIndex.append(";");
                    smsBody.append(sms.getDisplayMessageBody());
                    break;
                }
            }
        }

        MmsLog.d(TAG, "concatenation sms index:" + smsIndex.toString());
        MmsLog.d(TAG, "concatenation sms body:" + smsBody.toString());
        indexAndBody.add(smsIndex.toString());
        indexAndBody.add(smsBody.toString());

        return indexAndBody;
    }
    /// @}
    /// M: Code analyze 010, new feature, support for gemini.
    /// fix bug ALPS00042403, show the sender's number in manage SIM card. @{
    /**
     * Delete the message at index from ICC.  Return true iff
     * successful.
     */
    private int deleteMessageFromIcc(String messageIndexString, int slotId) {
        try {
            /// M: Code analyze 004, fix bug ALPS00064780, deleting sms is abnormal. use new FW
            /// interface while delete all messages on sim card. @{
            /// M: For Delete all,MTK FW support delete all using messageIndex = -1;
            if (messageIndexString.equals(ALL_SMS)) {
                return EncapsulatedGeminiSmsManager.deleteMessageFromIccGemini(-1, slotId) ? 1 : 0;
            } else {
                return EncapsulatedGeminiSmsManager.deleteMessageFromIccGemini(
                        Integer.parseInt(messageIndexString), slotId) ? 1 : 0;
            }
            /// @}
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Bad SMS ICC ID: " + messageIndexString);
        } finally {
            /// M: no need to notify change
        }
    }
    /// @}
    /// M: Code analyze 006, fix bug ALPS00252799, it cost long time to restore messages.
    /// support batch processing while restore messages. @{
    @Override
    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations)
            throws OperationApplicationException {
        int ypCount = 0;
        int opCount = 0;
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        /// M: Fix ALPS00288517, not use transaction again to avoid ANR because of order of locking db
        db.beginTransaction();
        /// @}
        try {
            final int numOperations = operations.size();
            final ContentProviderResult[] results = new ContentProviderResult[numOperations];
            for (int i = 0; i < numOperations; i++) {
                if (++opCount > MAX_OPERATIONS_PER_PATCH) {
                    throw new OperationApplicationException(
                            "Too many content provider operations between yield points. "
                                    + "The maximum number of operations per yield point is "
                                    + MAX_OPERATIONS_PER_PATCH, ypCount);
                }
                final ContentProviderOperation operation = operations.get(i);
                results[i] = operation.apply(this, results, i);
            }
            /// M: Fix ALPS00288517, not use transaction again to avoid ANR because of order of locking db
            db.setTransactionSuccessful();
            /// @}
            return results;
        } finally {
            /// M: Fix ALPS00288517, not use transaction again to avoid ANR because of order of locking db
            db.endTransaction();
            /// @}
        }
    }
    /// @}
    /// M: Code analyze 015, fix bug ALPS00234074, do not delete the thread
    /// when it is in writiing status. @{
    private void setThreadStatus(SQLiteDatabase db, ContentValues values, int value) {
        ContentValues statusContentValues = new ContentValues(1);
        statusContentValues.put(EncapsulatedTelephony.Threads.STATUS, value);
        db.update("threads", statusContentValues, "_id=" + values.getAsLong(Sms.THREAD_ID), null);
    }
    /// @}
    /// M: Code analyze 011, fix bug ALPS00282321, ANR while delete old messages.
    /// use new process of delete. @{
    private Cursor getAllSmsThreadIds(String selection, String[] selectionArgs) {
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        return db.query("sms",  new String[] {"distinct thread_id"},
                selection, selectionArgs, null, null, null);
    }
    /// @}
    /// M: Code analyze 005, fix bug ALPS00245352, it cost long time to restore messages.
    /// remove useless operation and add transaction while import sms. @{
    private long getThreadIdInternal(String recipient, SQLiteDatabase db) {
        String THREAD_QUERY;
        if (EncapsulatedFeatureOption.MTK_WAPPUSH_SUPPORT) {
            THREAD_QUERY = "SELECT _id FROM threads " + "WHERE type<>"
                    + EncapsulatedTelephony.Threads.WAPPUSH_THREAD + " AND recipient_ids=?";
        } else {
            THREAD_QUERY = "SELECT _id FROM threads " + "WHERE recipient_ids=?";
        }
        long recipientId = getRecipientId(recipient, db);
        MmsLog.d(TAG, "sms insert, getThreadIdInternal, recipientId = " + recipientId);
        String[] selectionArgs = new String[] { String.valueOf(recipientId) };
        Cursor cursor = db.rawQuery(THREAD_QUERY, selectionArgs);
        try {
              if (cursor != null && cursor.getCount() == 0) {
                   Log.d(TAG, "getThreadId: create new thread_id for recipients " + recipient);
                   return insertThread(recipientId, db);
               } else if (cursor.getCount() == 1){
                      if (cursor.moveToFirst()) {
                       return cursor.getLong(0);
                   }
               } else {
                   Log.w(TAG, "getThreadId: why is cursorCount=" + cursor.getCount());
               }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return 0;
    }

    /**
     * Insert a record for a new thread.
     */
    private long insertThread(long recipientIds, SQLiteDatabase db) {
        ContentValues values = new ContentValues(4);

        long date = System.currentTimeMillis();
        values.put(ThreadsColumns.DATE, date - date % 1000);
        values.put(ThreadsColumns.RECIPIENT_IDS, recipientIds);
        values.put(ThreadsColumns.MESSAGE_COUNT, 0);
        return db.insert("threads", null, values);
    }
    private long getRecipientId(String address, SQLiteDatabase db) {
         if (!address.equals(PduHeaders.FROM_INSERT_ADDRESS_TOKEN_STR)) {
             long id = getSingleAddressId(address, db);
             if (id != -1L) {
                 return id;
             } else {
                 Log.e(TAG, "getAddressIds: address ID not found for " + address);
             }
         }
         return 0;
    }
    /**
     * Return the canonical address ID for this address.
     */
    private long getSingleAddressId(String address, SQLiteDatabase db) {
        boolean isEmail = Mms.isEmailAddress(address);
        boolean isPhoneNumber = Mms.isPhoneNumber(address);
        /** M: We lowercase all email addresses, but not addresses that aren't numbers, because
        * that would incorrectly turn an address such as "My Vodafone" into "my vodafone"
        * and the thread title would be incorrect when displayed in the UI.*/
        String refinedAddress = isEmail ? address.toLowerCase() : address;
        String selection = "address=?";
        String[] selectionArgs;
        long retVal = -1L;
        if (!isPhoneNumber || (address != null && address.length() > NORMAL_NUMBER_MAX_LENGTH)) {
            selectionArgs = new String[] { refinedAddress };
        } else {
            boolean useStrictPhoneNumberComparation = getContext().getResources().getBoolean(
                    com.android.internal.R.bool.config_use_strict_phone_number_comparation);
            selection += " OR " + String.format(Locale.ENGLISH, "PHONE_NUMBERS_EQUAL(address, ?, %d)",
                    (useStrictPhoneNumberComparation ? 1 : 0));
            selectionArgs = new String[] { refinedAddress, refinedAddress };
        }
        Cursor cursor = null;

        try {
            cursor = db.query("canonical_addresses", CANONICAL_ADDRESSES_COLUMNS_2,
                    selection, selectionArgs, null, null, null);

            if (cursor.getCount() == 0) {
                retVal = insertCanonicalAddresses(db, refinedAddress);
                MmsLog.d(TAG, "getSingleAddressId: insert new canonical_address for " + address + ", _id=" + retVal);
                return retVal;
            } else {
                while (cursor.moveToNext()) {
                    String currentNumber = cursor.getString(cursor.getColumnIndexOrThrow(CanonicalAddressesColumns.ADDRESS));
                    MmsLog.d(TAG, "getSingleAddressId(): currentNumber != null ? " + (currentNumber != null));
                    if (currentNumber != null) {
                        if (refinedAddress.equals(currentNumber) || currentNumber.length() <= NORMAL_NUMBER_MAX_LENGTH) {
                            retVal = cursor.getLong(cursor.getColumnIndexOrThrow(CanonicalAddressesColumns._ID));
                            MmsLog.d(TAG, "getSingleAddressId(): get exist id=" + retVal);
                            break;
                        }
                    }
                }
                if (retVal == -1) {
                    retVal = insertCanonicalAddresses(db, refinedAddress);
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return retVal;
    }

    private long insertCanonicalAddresses(SQLiteDatabase db, String refinedAddress) {
        MmsLog.d(TAG, "sms insert insertCanonicalAddresses for address = " + refinedAddress);
        ContentValues contentValues = new ContentValues(1);
        contentValues.put(CanonicalAddressesColumns.ADDRESS, refinedAddress);
        return db.insert("canonical_addresses", CanonicalAddressesColumns.ADDRESS, contentValues);
    }
    /// @}
    /// M: Code analyze 002, fix bug ALPS00262044, not show out unread message
    /// icon after restore messages. notify mms application about unread messages
    /// number after insert operation. @{
    private void notifyChange2(Uri uri) {
        ContentResolver cr = getContext().getContentResolver();
        cr.notifyChange(uri, null);
        cr.notifyChange(MmsSms.CONTENT_URI, null);
        cr.notifyChange(Uri.parse("content://mms-sms/conversations/"), null);
        if (!notify){
            MmsLog.d(TAG, "SmsProvider notifyChange2");
            MmsSmsProvider.notifyUnreadMessageNumberChanged(getContext());
        }
    }
    /// @}
    /// M: Code analyze 003, fix bug ALPS00239521, ALPS00244682, improve multi-delete speed
    /// in folder mode. @{
    /// M: get the select id from sms to delete words
    private String getWordIds(SQLiteDatabase db, String boxType, String selectionArgs){
        StringBuffer content = new StringBuffer("(");
        String res = "";
        String rawQuery = String.format("select _id from sms where _id NOT IN (select _id from sms where type IN %s AND _id NOT IN %s)", boxType, selectionArgs);
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(rawQuery, null);
            if (cursor == null || cursor.getCount() == 0){
                return "()";
            }
            if (cursor.moveToFirst()) {
                do {
                    content.append(cursor.getInt(0));
                    content.append(",");
                } while (cursor.moveToNext());
                res = content.toString();
                if (!TextUtils.isEmpty(content) && res.endsWith(",")) {
                    res = res.substring(0, res.lastIndexOf(","));
                }
                res += ")";
            }
            MmsLog.d(TAG, "getWordIds cursor content = " + res + " COUNT " + cursor.getCount());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return res;
    }

    private String getSmsIds(String[] selectionArgs){
        StringBuffer content = new StringBuffer("(");
        String res = "";
        if (selectionArgs == null || selectionArgs.length <= 1){
            return "()";
        }
        for (int i = 0; i < selectionArgs.length - 2; i++){
            if (selectionArgs[i] == null){
                break;
            }
            content.append(selectionArgs[i]);
            content.append(",");
        }
        if (selectionArgs[selectionArgs.length-2] != null){
           content.append(selectionArgs[selectionArgs.length-2]);
        }
        res = content.toString();
        if (res.endsWith(",")) {
            res = res.substring(0, res.lastIndexOf(","));
        }
        res += ")";
        return res;
    }
    /// @}
    /// M: Code analyze 002, fix bug ALPS00046358, improve multi-delete speed by use batch
    /// processing. reference from page http://www.erpgear.com/show.php?contentid=1111. @{
    public int deleteOnce(Uri url, String where, String[] whereArgs) {
        int count = 0;
        int match = URI_MATCHER.match(url);
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        Log.d(TAG, "Delete deleteOnce: " + match);
        switch (match) {
            case SMS_ALL:
                count = db.delete(TABLE_SMS, where, whereArgs);
                if (count != 0) {
                    // Don't update threads unless something changed.
                    /// M: Code analyze 00X, fix bug ALPS00293926, the number is still 2 after
                    /// delete 1 sms via pc sync tool. update all thread while delete sms.
                    MmsSmsDatabaseHelper.updateAllThreads(db, null, null);
                }
                break;

            case SMS_ALL_ID:
                try {
                    int message_id = Integer.parseInt(url.getPathSegments().get(0));
                    count = MmsSmsDatabaseHelper.deleteOneSms(db, message_id);
                } catch (Exception e) {
                    throw new IllegalArgumentException(
                        "Bad message id: " + url.getPathSegments().get(0));
                }
                break;

            case SMS_CONVERSATIONS_ID:
                int threadID;

                try {
                    threadID = Integer.parseInt(url.getPathSegments().get(1));
                } catch (Exception ex) {
                    throw new IllegalArgumentException(
                            "Bad conversation thread id: "
                            + url.getPathSegments().get(1));
                }

                // delete the messages from the sms table
                where = DatabaseUtils.concatenateWhere("thread_id=" + threadID, where);
                count = db.delete(TABLE_SMS, where, whereArgs);
                MmsSmsDatabaseHelper.updateThread(db, threadID);
                break;
            /// M: Code analyze 011, fix bug ALPS00282321, ANR while delete old messages.
            /// use new process of delete. @{
            case SMS_AUTO_DELETE:

                try {
                    threadID = Integer.parseInt(url.getPathSegments().get(1));
                } catch (Exception ex) {
                    throw new IllegalArgumentException(
                            "Bad conversation thread id: "
                            + url.getPathSegments().get(1));
                }

                where = DatabaseUtils.concatenateWhere("thread_id=" + threadID, where);
                /// M: delete the messages from the sms table
                if (whereArgs != null){
                    String selectids = getSmsIds(whereArgs);
                  //  Log.d(TAG, "selectids = "  + selectids);
                    db.execSQL("delete from words where table_to_use=1 and source_id in " + selectids);
                    MmsLog.d(TAG, "delete words end");
                    for (int i = 0; i < whereArgs.length; ){
                        if (i%100 == 0){
                            MmsLog.d(TAG, "delete sms1 beginTransaction i = " + i);
                          // db.beginTransaction();
                        }
                        where = "_id=" + whereArgs[i];
                        count += db.delete(TABLE_SMS, where, null);
                        i++;
//                        if (i%100 == 0 || i == whereArgs.length){
//                            MmsLog.d(TAG, "delete sms1 endTransaction i = " + i);
//                            db.endTransaction();
//                        }
                    }
                } else {
                    if (where != null) {
                        int id = 0;
                        String[] args = where.split("_id<");
                        if (args.length > 1) {
                            String finalid = args[1].replace(")", "");
                            MmsLog.d(TAG, "SMS_CONVERSATIONS_ID args[1] = " + args[1]);
                            id = Integer.parseInt(finalid);
                            MmsLog.d(TAG, "SMS_CONVERSATIONS_ID id = " + id);

                            for (int i = 1; i < id; i++) {
                                if (i % 30 == 0 || i == id - 1) {
                                    MmsLog.d(TAG, "delete sms2 beginTransaction i = " + i);
                                    where = "locked=0 AND type<>3 AND ipmsg_id<=0 AND _id>" + (i-30) + " AND _id<=" + i;
                                    where = DatabaseUtils.concatenateWhere("thread_id=" + threadID, where);
                                    count += db.delete(TABLE_SMS, where, null);
                                    MmsLog.d(TAG, "delete sms2 endTransaction i = " + i + " count=" + count);
                                }
                            }
                        }
                    }
                }
                MmsSmsDatabaseHelper.updateThread(db, threadID);
                break;
            /// @}
            case SMS_RAW_MESSAGE:
                count = db.delete("raw", where, whereArgs);
                break;

            case SMS_STATUS_PENDING:
                count = db.delete("sr_pending", where, whereArgs);
                break;
            /// M: Code analyze 010, new feature, support for gemini. @{
            case SMS_ICC:
                String messageIndexString = url.getPathSegments().get(1);
                if(EncapsulatedFeatureOption.MTK_GEMINI_SUPPORT == true) {
                    Log.i(TAG, "Delete Sim1 SMS id: " + messageIndexString);
                    return deleteMessageFromIcc(messageIndexString, PhoneConstants.GEMINI_SIM_1);
                } else {
                    return deleteMessageFromIcc(messageIndexString);
                }
            case SMS_ICC_GEMINI:
                String messageIndexString_1 = url.getPathSegments().get(1);
                Log.i(TAG, "Delete Sim2 SMS id: " + messageIndexString_1);
                return deleteMessageFromIcc(messageIndexString_1, PhoneConstants.GEMINI_SIM_2);
            /// @}
            case SMS_ICC_THREE:
                String messageIndexString_2 = url.getPathSegments().get(1);
                Log.i(TAG, "Delete Sim3 SMS id: " + messageIndexString_2);
                return deleteMessageFromIcc(messageIndexString_2, PhoneConstants.GEMINI_SIM_3);
            case SMS_ICC_FOUR:
                String messageIndexString_3 = url.getPathSegments().get(1);
                Log.i(TAG, "Delete Sim4 SMS id: " + messageIndexString_3);
                return deleteMessageFromIcc(messageIndexString_3, PhoneConstants.GEMINI_SIM_4);
            default:
                throw new IllegalArgumentException("Unknown URL");
        }
/*
        if (count > 0) {
            notifyChange(url);
        }
        */
        return count;
    }
    /// @}
}
