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

package com.android.providers.telephony;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.FileUtils;
import android.os.ParcelFileDescriptor;
import android.provider.BaseColumns;
import android.provider.Telephony;
import android.provider.Telephony.CanonicalAddressesColumns;
import android.provider.Telephony.Mms;
import android.provider.Telephony.MmsSms;
import android.provider.Telephony.Mms.Addr;
import android.provider.Telephony.Mms.Part;
import android.provider.Telephony.Mms.Rate;
import android.text.TextUtils;
import android.util.Config;
import android.util.Log;
import com.mediatek.encapsulation.MmsLog;

import com.google.android.mms.pdu.EncodedStringValue;
import com.google.android.mms.pdu.PduHeaders;
import com.google.android.mms.util.DownloadDrmHelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import android.provider.Telephony.Threads;
import com.mediatek.encapsulation.android.telephony.EncapsulatedTelephony;

/**
 * The class to provide base facility to access MMS related content,
 * which is stored in a SQLite database and in the file system.
 */
public class MmsProvider extends ContentProvider {
    static final String TABLE_PDU  = "pdu";
    static final String TABLE_ADDR = "addr";
    static final String TABLE_PART = "part";
    static final String TABLE_RATE = "rate";
    static final String TABLE_DRM  = "drm";
    static final String TABLE_WORDS = "words";
    /// M: Code analyze 001, fix bug ALPS00046358, improve multi-delete speed by use batch
    /// processing. reference from page http://www.erpgear.com/show.php?contentid=1111.
    private static final String FOR_MULTIDELETE = "ForMultiDelete";
    /// M: Code analyze 002, fix bug ALPS00262044, not show out unread message
    /// icon after restore messages. notify mms application about unread messages
    /// number after insert operation.
    private static boolean notifyUnread = false;

    private static final Set<String> COLUMNS = new HashSet<String>();

    private static final String[] ADDR_PDU_COLUMNS = {
            BaseColumns._ID, Addr.MSG_ID, Addr.CONTACT_ID, Addr.ADDRESS, Addr.TYPE, Addr.CHARSET,
            "pdu_id", "delivery_status", Mms.READ_STATUS
    };

    static{
        for (int i = 0; i < ADDR_PDU_COLUMNS.length; i++) {
            COLUMNS.add(ADDR_PDU_COLUMNS[i]);
        }
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = MmsSmsDatabaseHelper.getInstance(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection,
            String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        MmsLog.d(TAG, "query uri = " + uri);
        // Generate the body of the query.
        /// M: Code analyze 003, fix bug ALPS00313325, coding convention correction.
        /// TODO: Is this change necessary? this will lead to more diff blocks.
        int match = URI_MATCHER.match(uri);
        if (LOCAL_LOGV) {
            Log.v(TAG, "Query uri=" + uri + ", match=" + match);
        }

        switch (match) {
            case MMS_ALL:
                constructQueryForBox(qb, Mms.MESSAGE_BOX_ALL);
                break;
            case MMS_INBOX:
                constructQueryForBox(qb, Mms.MESSAGE_BOX_INBOX);
                break;
            case MMS_SENT:
                constructQueryForBox(qb, Mms.MESSAGE_BOX_SENT);
                break;
            case MMS_DRAFTS:
                constructQueryForBox(qb, Mms.MESSAGE_BOX_DRAFTS);
                break;
            case MMS_OUTBOX:
                constructQueryForBox(qb, Mms.MESSAGE_BOX_OUTBOX);
                break;
            case MMS_ALL_ID:
                qb.setTables(TABLE_PDU);
                qb.appendWhere(Mms._ID + "=" + uri.getPathSegments().get(0));
                break;
            case MMS_INBOX_ID:
            case MMS_SENT_ID:
            case MMS_DRAFTS_ID:
            case MMS_OUTBOX_ID:
                qb.setTables(TABLE_PDU);
                qb.appendWhere(Mms._ID + "=" + uri.getPathSegments().get(1));
                qb.appendWhere(" AND " + Mms.MESSAGE_BOX + "="
                        + getMessageBoxByMatch(match));
                break;
            case MMS_ALL_PART:
                qb.setTables(TABLE_PART);
                break;
            case MMS_MSG_PART:
                qb.setTables(TABLE_PART);
                qb.appendWhere(Part.MSG_ID + "=" + uri.getPathSegments().get(0));
                break;
            case MMS_PART_ID:
                qb.setTables(TABLE_PART);
                qb.appendWhere(Part._ID + "=" + uri.getPathSegments().get(1));
                break;
            case MMS_MSG_ADDR:
                qb.setTables(TABLE_ADDR);
                qb.appendWhere(Addr.MSG_ID + "=" + uri.getPathSegments().get(0));
                break;
            case MMS_REPORT_STATUS:
                /*
                   SELECT DISTINCT address,
                                   T.delivery_status AS delivery_status,
                                   T.read_status AS read_status
                   FROM addr
                   INNER JOIN (SELECT P1._id AS id1, P2._id AS id2, P3._id AS id3,
                                      ifnull(P2.st, 0) AS delivery_status,
                                      ifnull(P3.read_status, 0) AS read_status
                               FROM pdu P1
                               INNER JOIN pdu P2
                               ON P1.m_id = P2.m_id AND P2.m_type = 134
                               LEFT JOIN pdu P3
                               ON P1.m_id = P3.m_id AND P3.m_type = 136
                               UNION
                               SELECT P1._id AS id1, P2._id AS id2, P3._id AS id3,
                                      ifnull(P2.st, 0) AS delivery_status,
                                      ifnull(P3.read_status, 0) AS read_status
                               FROM pdu P1
                               INNER JOIN pdu P3
                               ON P1.m_id = P3.m_id AND P3.m_type = 136
                               LEFT JOIN pdu P2
                               ON P1.m_id = P2.m_id AND P2.m_type = 134) T
                   ON (msg_id = id2 AND type = 151)
                   OR (msg_id = id3 AND type = 137)
                   WHERE T.id1 = ?;
                 */
                /*
                qb.setTables("addr INNER JOIN (SELECT P1._id AS id1, P2._id" +
                             " AS id2, P3._id AS id3, ifnull(P2.st, 0) AS" +
                             " delivery_status, ifnull(P3.read_status, 0) AS" +
                             " read_status FROM pdu P1 INNER JOIN pdu P2 ON" +
                             " P1.m_id=P2.m_id AND P2.m_type=134 LEFT JOIN" +
                             " pdu P3 ON P1.m_id=P3.m_id AND P3.m_type=136" +
                             " UNION SELECT P1._id AS id1, P2._id AS id2, P3._id" +
                             " AS id3, ifnull(P2.st, 0) AS delivery_status," +
                             " ifnull(P3.read_status, 0) AS read_status FROM" +
                             " pdu P1 INNER JOIN pdu P3 ON P1.m_id=P3.m_id AND" +
                             " P3.m_type=136 LEFT JOIN pdu P2 ON P1.m_id=P2.m_id" +
                             " AND P2.m_type=134) T ON (msg_id=id2 AND type=151)" +
                             " OR (msg_id=id3 AND type=137)");
                qb.appendWhere("T.id1 = " + uri.getLastPathSegment());
                qb.setDistinct(true);
                */
                 SQLiteQueryBuilder deliveryQueryBuilder = new SQLiteQueryBuilder();
                 SQLiteQueryBuilder readQueryBuilder = new SQLiteQueryBuilder();
                 SQLiteQueryBuilder unionQueryBuilder = new SQLiteQueryBuilder();
                 deliveryQueryBuilder.setTables(" addr inner join (" +
                    " select _id as pdu_id, ifnull(st,0) as delivery_status, ifnull(read_status,0) as read_status from pdu " +
                    " where (m_type=134) and (pdu_id in (select _id from pdu where m_id = (select m_id from pdu where _id = "+
                          uri.getLastPathSegment() +")))" +
                    ") on ( addr.msg_id=pdu_id and addr.type=151)");
                 readQueryBuilder.setTables(" addr inner join (" +
                    " select _id as pdu_id, ifnull(st,0) as delivery_status, ifnull(read_status,0) as read_status from pdu " +
                    " where (m_type=136) and (pdu_id in (select _id from pdu where m_id = (select m_id from pdu where _id = "+
                           uri.getLastPathSegment() +")))" +
                    ") on ( addr.msg_id=pdu_id and addr.type=137)");
                 
                 String[] idColumn = new String[] { Addr.ADDRESS,"delivery_status","read_status"};
                 String deliverySubQuery = deliveryQueryBuilder.buildUnionSubQuery(
                                  "status", ADDR_PDU_COLUMNS,COLUMNS, 0, "delivery",null,null,null);
                 String readSubQuery = readQueryBuilder.buildUnionSubQuery(
                                  "status", ADDR_PDU_COLUMNS,COLUMNS, 0, "readreport",null,null,null);
                 String unionQuery = null;
                 unionQuery = unionQueryBuilder.buildUnionQuery(
                                 new String[] { deliverySubQuery, readSubQuery}, null, null);
                 Log.d(TAG,"unionQuery = " + unionQuery);
                 qb.setTables("(" + unionQuery + ")");
                break;
            case MMS_REPORT_REQUEST:
                /*
                   SELECT address, d_rpt, rr
                   FROM addr join pdu on pdu._id = addr.msg_id
                   WHERE pdu._id = messageId AND addr.type = 151
                 */
                qb.setTables(TABLE_ADDR + " join " +
                        TABLE_PDU + " on pdu._id = addr.msg_id");
                qb.appendWhere("pdu._id = " + uri.getLastPathSegment());
                qb.appendWhere(" AND " + "addr.type = " + PduHeaders.TO);
                break;
            case MMS_SENDING_RATE:
                qb.setTables(TABLE_RATE);
                break;
            case MMS_DRM_STORAGE_ID:
                qb.setTables(TABLE_DRM);
                qb.appendWhere(BaseColumns._ID + "=" + uri.getLastPathSegment());
                break;
            case MMS_THREADS:
                qb.setTables("pdu group by thread_id");
                break;
            default:
                Log.e(TAG, "query: invalid request: " + uri);
                return null;
        }

        String finalSortOrder = null;
        if (TextUtils.isEmpty(sortOrder)) {
            if (qb.getTables().equals(TABLE_PDU)) {
                finalSortOrder = Mms.DATE + " DESC";
            } else if (qb.getTables().equals(TABLE_PART)) {
                finalSortOrder = Part.SEQ;
            }
        } else {
            finalSortOrder = sortOrder;
        }
        MmsLog.d(TAG, "query getReadableDatabase begin");
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        MmsLog.d(TAG, "query getReadableDatabase query begin");
        Cursor ret = qb.query(db, projection, selection,
                selectionArgs, null, null, finalSortOrder);
        MmsLog.d(TAG, "query getReadableDatabase query end");
        if (ret != null){
            MmsLog.d(TAG, "query getReadableDatabase query end cursor count =" + ret.getCount());
        }
        // TODO: Does this need to be a URI for this provider.
        ret.setNotificationUri(getContext().getContentResolver(), uri);
        return ret;
    }

    private void constructQueryForBox(SQLiteQueryBuilder qb, int msgBox) {
        qb.setTables(TABLE_PDU);

        if (msgBox != Mms.MESSAGE_BOX_ALL) {
            qb.appendWhere(Mms.MESSAGE_BOX + "=" + msgBox);
        }
    }

    @Override
    public String getType(Uri uri) {
        /// M: Code analyze 003, fix bug ALPS00313325, coding convention correction.
        /// TODO: Is this change necessary? this will lead to more diff blocks.
        int match = URI_MATCHER.match(uri);
        switch (match) {
            case MMS_ALL:
            case MMS_INBOX:
            case MMS_SENT:
            case MMS_DRAFTS:
            case MMS_OUTBOX:
                return VND_ANDROID_DIR_MMS;
            case MMS_ALL_ID:
            case MMS_INBOX_ID:
            case MMS_SENT_ID:
            case MMS_DRAFTS_ID:
            case MMS_OUTBOX_ID:
                return VND_ANDROID_MMS;
            case MMS_PART_ID: {
                Cursor cursor = mOpenHelper.getReadableDatabase().query(
                        TABLE_PART, new String[] { Part.CONTENT_TYPE },
                        Part._ID + " = ?", new String[] { uri.getLastPathSegment() },
                        null, null, null);
                if (cursor != null) {
                    try {
                        if ((cursor.getCount() == 1) && cursor.moveToFirst()) {
                            return cursor.getString(0);
                        } else {
                            Log.e(TAG, "cursor.count() != 1: " + uri);
                        }
                    } finally {
                        cursor.close();
                    }
                } else {
                    Log.e(TAG, "cursor == null: " + uri);
                }
                return "*/*";
            }
            case MMS_ALL_PART:
            case MMS_MSG_PART:
            case MMS_MSG_ADDR:
            default:
                return "*/*";
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        MmsLog.d(TAG, "insert uri = " + uri);
        int msgBox = Mms.MESSAGE_BOX_ALL;
        boolean notify = true;
        /// M: Code analyze 002, fix bug ALPS00262044, not show out unread message
        /// icon after restore messages. notify mms application about unread messages
        /// number after insert operation.
        notifyUnread = true;
        /// M: Code analyze 003, fix bug ALPS00313325, coding convention correction.
        /// TODO: Is this change necessary? this will lead to more diff blocks.
        int match = URI_MATCHER.match(uri);
        if (LOCAL_LOGV) {
            Log.v(TAG, "Insert uri=" + uri + ", match=" + match);
        }

        String table = TABLE_PDU;
        switch (match) {
            case MMS_ALL:
                Object msgBoxObj = values.getAsInteger(Mms.MESSAGE_BOX);
                if (msgBoxObj != null) {
                    msgBox = (Integer) msgBoxObj;
                }
                else {
                    // default to inbox
                    msgBox = Mms.MESSAGE_BOX_INBOX;
                }
                break;
            case MMS_INBOX:
                /// M: Code analyze 004, fix bug ALPS00249113, if query result is not updated
                /// completely, the result will show abnormal. adjust notify time while
                /// store messages. @{
                if (values.containsKey("need_notify")){
                    notify = values.getAsBoolean("need_notify");
                }
                /// @}
                msgBox = Mms.MESSAGE_BOX_INBOX;
                break;
            case MMS_SENT:
                msgBox = Mms.MESSAGE_BOX_SENT;
                break;
            case MMS_DRAFTS:
                msgBox = Mms.MESSAGE_BOX_DRAFTS;
                break;
            case MMS_OUTBOX:
                msgBox = Mms.MESSAGE_BOX_OUTBOX;
                break;
            case MMS_MSG_PART:
                notify = false;
                table = TABLE_PART;
                break;
            case MMS_MSG_ADDR:
                notify = false;
                table = TABLE_ADDR;
                break;
            case MMS_SENDING_RATE:
                notify = false;
                table = TABLE_RATE;
                break;
            case MMS_DRM_STORAGE:
                notify = false;
                table = TABLE_DRM;
                break;
            /// M: Code analyze 005, fix bug ALPS00275452, count the attachments size. @{
            case MMS_ATTACHMENT_SIZE:
                long size = getAttachmentsSize();
                uri = uri.buildUpon().appendQueryParameter("size", String.valueOf(size)).build();
                return uri;
            /// @}
            default:
                Log.e(TAG, "insert: invalid request: " + uri);
                return null;
        }
        MmsLog.d(TAG, "insert getWritebleDatabase table = " + table);
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        MmsLog.d(TAG, "insert getWritebleDatabase end");
        ContentValues finalValues;
        Uri res = Mms.CONTENT_URI;
        long rowId;
        /// M: Code analyze 004, fix bug ALPS00249113, if query result is not updated
        /// completely, the result will show abnormal. adjust notify time while
        /// store messages. @{
        if (values.containsKey("need_notify")){
            values.remove("need_notify");
        }
        /// @}
        if (table.equals(TABLE_PDU)) {
            boolean addDate = !values.containsKey(Mms.DATE);
            boolean addMsgBox = !values.containsKey(Mms.MESSAGE_BOX);

            // Filter keys we don't support yet.
            filterUnsupportedKeys(values);

            // TODO: Should initialValues be validated, e.g. if it
            // missed some significant keys?
            finalValues = new ContentValues(values);

            long timeInMillis = System.currentTimeMillis();

            if (addDate) {
                finalValues.put(Mms.DATE, timeInMillis / 1000L);
            }

            if (addMsgBox && (msgBox != Mms.MESSAGE_BOX_ALL)) {
                finalValues.put(Mms.MESSAGE_BOX, msgBox);
            }

            if (msgBox != Mms.MESSAGE_BOX_INBOX) {
                // Mark all non-inbox messages read.
                finalValues.put(Mms.READ, 1);
            }

            // thread_id
            Long threadId = values.getAsLong(Mms.THREAD_ID);
            String address = values.getAsString(CanonicalAddressesColumns.ADDRESS);
            if (((threadId == null) || (threadId == 0)) && (!TextUtils.isEmpty(address))) {
                /// M: Code analyze 006, fix bug ALPS00231431, thread should not be delete when
                /// receive mms notification, update status of threads table. @{
                threadId = EncapsulatedTelephony.Threads.getOrCreateThreadIdInternal(getContext(), address);
                finalValues.put(Mms.THREAD_ID, threadId);
                /// @}
            }

            if ((rowId = db.insert(table, null, finalValues)) <= 0) {
                Log.e(TAG, "MmsProvider.insert: failed! " + finalValues);
                return null;
            }
            /// M: Code analyze 006, fix bug ALPS00231431, thread should not be delete when
            /// receive mms notification, update status of threads table.
            setThreadStatus(db, values, 0);
            res = Uri.parse(res + "/" + rowId);

        } else if (table.equals(TABLE_ADDR)) {
            finalValues = new ContentValues(values);
            finalValues.put(Addr.MSG_ID, uri.getPathSegments().get(0));

            ArrayList<String> addresses = null;
            if (values.containsKey("addresses")) {
                addresses = values.getStringArrayList("addresses");
                values.remove("addresses");
            }

            rowId = 0;
            if (addresses != null && addresses.size() > 0) {
                ContentValues v = new ContentValues(4);
                for (int index = 0 ; index < addresses.size();) {
                        values.clear(); // Clear all values first.
                        v.put(Addr.MSG_ID, uri.getPathSegments().get(0));
                        v.put(Addr.ADDRESS, addresses.get(index++));
                        v.put(Addr.CHARSET, addresses.get(index++));
                        v.put(Addr.TYPE, addresses.get(index++));
                        rowId = db.insert(table, null, v);
                }
            } else if ((rowId = db.insert(table, null, finalValues)) <= 0) {
                Log.e(TAG, "Failed to insert address: " + finalValues);
                return null;
            }

            res = Uri.parse(res + "/addr/" + rowId);
        } else if (table.equals(TABLE_PART)) {
            finalValues = new ContentValues(values);

            if (match == MMS_MSG_PART) {
                finalValues.put(Part.MSG_ID, uri.getPathSegments().get(0));
            }

            String contentType = values.getAsString("ct");

            // text/plain and app application/smil store their "data" inline in the
            // table so there's no need to create the file
            boolean plainText = false;
            boolean smilText = false;
            if ("text/plain".equals(contentType)) {
                plainText = true;
            } else if ("application/smil".equals(contentType)) {
                smilText = true;
            }
            if (!plainText && !smilText) {
                // Use the filename if possible, otherwise use the current time as the name.
                String contentLocation = values.getAsString("cl");
                if (!TextUtils.isEmpty(contentLocation)) {
                    File f = new File(contentLocation);
                    contentLocation = "_" + f.getName();
                } else {
                    contentLocation = "";
                }

                // Generate the '_data' field of the part with default
                // permission settings.
                /// M: Code analyze 007, fix bug ALPS00334881, the JE pops up when you add the
                /// image to message. do not use contentLocation as part of path name. @{
                String path = getContext().getDir("parts", 0).getPath()
                        /** M: we do not use contentLocation, this maybe a long string
                         *  the total file length maybe too long, it can not be created!
                         */
                        + "/PART_" + System.currentTimeMillis();// + contentLocation;
                /// @}
                if (DownloadDrmHelper.isDrmConvertNeeded(contentType)) {
                    // Adds the .fl extension to the filename if contentType is
                    // "application/vnd.oma.drm.message"
                    path = DownloadDrmHelper.modifyDrmFwLockFileExtension(path);
                }

                finalValues.put(Part._DATA, path);

                File partFile = new File(path);
                if (!partFile.exists()) {
                    try {
                        if (!partFile.createNewFile()) {
                            throw new IllegalStateException(
                                    "Unable to create new partFile: " + path);
                        }
                        // Give everyone rw permission until we encrypt the file
                        // (in PduPersister.persistData). Once the file is encrypted, the
                        // permissions will be set to 0644.
                        int result = FileUtils.setPermissions(path, 0666, -1, -1);
                        if (LOCAL_LOGV) {
                            Log.d(TAG, "MmsProvider.insert setPermissions result: " + result);
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "createNewFile", e);
                        throw new IllegalStateException(
                                "Unable to create new partFile: " + path);
                    }
                }
            }

            if ((rowId = db.insert(table, null, finalValues)) <= 0) {
                Log.e(TAG, "MmsProvider.insert: failed! " + finalValues);
                return null;
            }

            res = Uri.parse(res + "/part/" + rowId);

            // Don't use a trigger for updating the words table because of a bug
            // in FTS3.  The bug is such that the call to get the last inserted
            // row is incorrect.
            if (plainText) {
                // Update the words table with a corresponding row.  The words table
                // allows us to search for words quickly, without scanning the whole
                // table;
                ContentValues cv = new ContentValues();

                // we're using the row id of the part table row but we're also using ids
                // from the sms table so this divides the space into two large chunks.
                // The row ids from the part table start at 2 << 32.
                cv.put(Telephony.MmsSms.WordsTable.ID, (2 << 32) + rowId);
                cv.put(Telephony.MmsSms.WordsTable.INDEXED_TEXT, values.getAsString("text"));
                cv.put(Telephony.MmsSms.WordsTable.SOURCE_ROW_ID, rowId);
                cv.put(Telephony.MmsSms.WordsTable.TABLE_ID, 2);
                db.insert(TABLE_WORDS, Telephony.MmsSms.WordsTable.INDEXED_TEXT, cv);
            }

        } else if (table.equals(TABLE_RATE)) {
            long now = values.getAsLong(Rate.SENT_TIME);
            long oneHourAgo = now - 1000 * 60 * 60;
            // Delete all unused rows (time earlier than one hour ago).
            db.delete(table, Rate.SENT_TIME + "<=" + oneHourAgo, null);
            db.insert(table, null, values);
        } else if (table.equals(TABLE_DRM)) {
            String path = getContext().getDir("parts", 0).getPath()
                    + "/PART_" + System.currentTimeMillis();
            finalValues = new ContentValues(1);
            finalValues.put("_data", path);

            File partFile = new File(path);
            if (!partFile.exists()) {
                try {
                    if (!partFile.createNewFile()) {
                        throw new IllegalStateException(
                                "Unable to create new file: " + path);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "createNewFile", e);
                    throw new IllegalStateException(
                            "Unable to create new file: " + path);
                }
            }

            if ((rowId = db.insert(table, null, finalValues)) <= 0) {
                Log.e(TAG, "MmsProvider.insert: failed! " + finalValues);
                return null;
            }
            res = Uri.parse(res + "/drm/" + rowId);
        } else {
            throw new AssertionError("Unknown table type: " + table);
        }

        if (notify) {
            MmsLog.d(TAG, "insert getWritebleDatabase notify");
            /// M: Code analyze 002, fix bug ALPS00262044, not show out unread message
            /// icon after restore messages. notify mms application about unread messages
            /// number after insert operation.
            notifyUnread = false;
            notifyChange(uri);
        }
        return res;
    }
    /// M: Code analyze 006, fix bug ALPS00231431, thread should not be delete when
    /// receive mms notification, update status of threads table. @{
    private void setThreadStatus(SQLiteDatabase db, ContentValues values, int value) {
        ContentValues statusContentValues = new ContentValues(1);
        statusContentValues.put(EncapsulatedTelephony.Threads.STATUS, value);
        db.update("threads", statusContentValues, "_id=" + values.getAsLong(Mms.THREAD_ID), null);
    }
    /// @}
    private int getMessageBoxByMatch(int match) {
        switch (match) {
            case MMS_INBOX_ID:
            case MMS_INBOX:
                return Mms.MESSAGE_BOX_INBOX;
            case MMS_SENT_ID:
            case MMS_SENT:
                return Mms.MESSAGE_BOX_SENT;
            case MMS_DRAFTS_ID:
            case MMS_DRAFTS:
                return Mms.MESSAGE_BOX_DRAFTS;
            case MMS_OUTBOX_ID:
            case MMS_OUTBOX:
                return Mms.MESSAGE_BOX_OUTBOX;
            default:
                throw new IllegalArgumentException("bad Arg: " + match);
        }
    }
    /// M: Code analyze 001, fix bug ALPS00046358, improve multi-delete speed by use batch
    /// processing. reference from page http://www.erpgear.com/show.php?contentid=1111. @{
    /// M: change implement for MulitDelete
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        MmsLog.d(TAG, "delete");
        int deletedRows = 0;
        boolean notify = false;
        Uri deleteUri = null;
        /// M: Code analyze 003, fix bug ALPS00313325, coding convention correction.
        /// TODO: Is this change necessary? this will lead to more diff blocks.
        int match = URI_MATCHER.match(uri);
        MmsLog.d(TAG, "delete getWritableDatabase");
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        MmsLog.d(TAG, "delete getWritableDatabase end");
        switch (match) {
            case MMS_ALL_ID:
            case MMS_INBOX_ID:
            case MMS_SENT_ID:
            case MMS_DRAFTS_ID:
            case MMS_OUTBOX_ID:
            case MMS_ALL:
            case MMS_INBOX:
            case MMS_SENT:
            case MMS_DRAFTS:
            case MMS_OUTBOX:
                notify = true;
            break;
        }
        if (selection!=null) {
            if (selection.equals(FOR_MULTIDELETE)) {
                int message_id = 0;
                deletedRows = 0;
                MmsLog.d(TAG, "delete beginTransaction");
                db.beginTransaction();
                for (int i=0; i<selectionArgs.length; i++) {
                    deleteUri = null;
                    if (selectionArgs[i] == null) {
                        //return deletedRows;
                    } else {
                        message_id = Integer.parseInt(selectionArgs[i]);
                        deleteUri = ContentUris.withAppendedId(uri, message_id);
                        //Log.i(TAG, "message_id is " + message_id);
                        deletedRows += deleteOnce(deleteUri, null, null);    
                    }
                }
                db.setTransactionSuccessful();
                MmsLog.d(TAG, "delete endTransaction");
                db.endTransaction();
            } else {
                MmsLog.d(TAG, "delete deleteOnce");
                deletedRows = deleteOnce(uri, selection, selectionArgs);
            }
        } else {
            deletedRows = deleteOnce(uri, selection, selectionArgs);
        }
        
        if ((deletedRows > 0) && notify) {
            notifyChange(uri);
        }
        return deletedRows;
    }
    
    public int deleteOnce(Uri uri, String selection, String[] selectionArgs) {
        /// M: Code analyze 003, fix bug ALPS00313325, coding convention correction.
        /// TODO: Is this change necessary? this will lead to more diff blocks.
        int match = URI_MATCHER.match(uri);
        //if (LOCAL_LOGV) {
            Log.v(TAG, "Delete uri=" + uri + ", match=" + match);
        //}

        String table, extraSelection = null;
        boolean notify = false;

        switch (match) {
            case MMS_ALL_ID:
            case MMS_INBOX_ID:
            case MMS_SENT_ID:
            case MMS_DRAFTS_ID:
            case MMS_OUTBOX_ID:
                notify = true;
                table = TABLE_PDU;
                extraSelection = Mms._ID + "=" + uri.getLastPathSegment();
                break;
            case MMS_ALL:
            case MMS_INBOX:
            case MMS_SENT:
            case MMS_DRAFTS:
            case MMS_OUTBOX:
                notify = true;
                table = TABLE_PDU;
                if (match != MMS_ALL) {
                    int msgBox = getMessageBoxByMatch(match);
                    extraSelection = Mms.MESSAGE_BOX + "=" + msgBox;
                }
                break;
            case MMS_ALL_PART:
                table = TABLE_PART;
                break;
            case MMS_MSG_PART:
                table = TABLE_PART;
                extraSelection = Part.MSG_ID + "=" + uri.getPathSegments().get(0);
                break;
            case MMS_PART_ID:
                table = TABLE_PART;
                extraSelection = Part._ID + "=" + uri.getPathSegments().get(1);
                break;
            case MMS_MSG_ADDR:
                table = TABLE_ADDR;
                extraSelection = Addr.MSG_ID + "=" + uri.getPathSegments().get(0);
                break;
            case MMS_DRM_STORAGE:
                table = TABLE_DRM;
                break;
            default:
                Log.w(TAG, "No match for URI '" + uri + "'");
                return 0;
        }

        String finalSelection = concatSelections(selection, extraSelection);
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int deletedRows = 0;

        if (TABLE_PDU.equals(table)) {
            deletedRows = deleteMessages(getContext(), db, finalSelection,
                                         selectionArgs, uri);
        } else if (TABLE_PART.equals(table)) {
            deletedRows = deleteParts(db, finalSelection, selectionArgs);
        } else if (TABLE_DRM.equals(table)) {
            deletedRows = deleteTempDrmData(db, finalSelection, selectionArgs);
        } else {
            deletedRows = db.delete(table, finalSelection, selectionArgs);
        }
/*
        if ((deletedRows > 0) && notify) {
            notifyChange();
        }
        */
        MmsLog.d(TAG, "deleteOnce end");
        return deletedRows;
    }
    /// @}
    static int deleteMessages(Context context, SQLiteDatabase db,
            String selection, String[] selectionArgs, Uri uri) {
        /// M: Code analyze 008, fix bug ALPS00300658, it cost long time to update thread table
        /// after delete messages. only update one thread record after delete. @{
        Cursor cursor = db.query(TABLE_PDU, new String[] { Mms._ID, Mms.THREAD_ID},
                selection, selectionArgs, null, null, null);
        if (cursor == null) {
            return 0;
        }
        long thread_id = 0;
        try {
            if (cursor.getCount() == 0) {
                return 0;
            }
            MmsLog.d(TAG, "deleteMessages cursor.getCount() " + cursor.getCount());
            while (cursor.moveToNext()) {
                thread_id = cursor.getLong(1);
                MmsLog.d(TAG, "deleteMessages thread_id = " + thread_id);
                deleteParts(db, Part.MSG_ID + " = ?",
                        new String[] { String.valueOf(cursor.getLong(0)) });
            }
        } finally {
            cursor.close();
        }
        /// @}
        int count = db.delete(TABLE_PDU, selection, selectionArgs);
        if (count > 0) {
            Intent intent = new Intent(Mms.Intents.CONTENT_CHANGED_ACTION);
            intent.putExtra(Mms.Intents.DELETED_CONTENTS, uri);
            if (LOCAL_LOGV) {
                Log.v(TAG, "Broadcasting intent: " + intent);
            }
            context.sendBroadcast(intent);
            /// M: Code analyze 008, fix bug ALPS00300658, it cost long time to update thread table
            /// after delete messages. only update one thread record after delete. @{
            /// M: update the thread which the pdu belonged
            if (thread_id > 0){
                MmsSmsDatabaseHelper.updateThread(db, thread_id);
            }
            /// @}
        }
        return count;
    }

    private static int deleteParts(SQLiteDatabase db, String selection,
            String[] selectionArgs) {
        return deleteDataRows(db, TABLE_PART, selection, selectionArgs);
    }

    private static int deleteTempDrmData(SQLiteDatabase db, String selection,
            String[] selectionArgs) {
        return deleteDataRows(db, TABLE_DRM, selection, selectionArgs);
    }

    private static int deleteDataRows(SQLiteDatabase db, String table,
            String selection, String[] selectionArgs) {
        Cursor cursor = db.query(table, new String[] { "_data" },
                selection, selectionArgs, null, null, null);
        if (cursor == null) {
            // FIXME: This might be an error, ignore it may cause
            // unpredictable result.
            return 0;
        }

        try {
            if (cursor.getCount() == 0) {
                return 0;
            }

            while (cursor.moveToNext()) {
                try {
                    // Delete the associated files saved on file-system.
                    String path = cursor.getString(0);
                    if (path != null) {
                        new File(path).delete();
                    }
                } catch (Throwable ex) {
                    Log.e(TAG, ex.getMessage(), ex);
                }
            }
        } finally {
            cursor.close();
        }

        return db.delete(table, selection, selectionArgs);
    }

    @Override
    public int update(Uri uri, ContentValues values,
            String selection, String[] selectionArgs) {
        /// M: Code analyze 003, fix bug ALPS00313325, coding convention correction.
        /// TODO: Is this change necessary? this will lead to more diff blocks.
        int match = URI_MATCHER.match(uri);
        if (LOCAL_LOGV) {
            Log.v(TAG, "Update uri=" + uri + ", match=" + match);
        }
        MmsLog.d(TAG, "update");
        boolean notify = false;
        String msgId = null;
        String table;
        /// M: Code analyze 009, fix bug ALPS00293687, update thread table while update messages.
        long oldThreadId = -1;

        switch (match) {
            case MMS_ALL_ID:
            case MMS_INBOX_ID:
            case MMS_SENT_ID:
            case MMS_DRAFTS_ID:
            case MMS_OUTBOX_ID:
                msgId = uri.getLastPathSegment();
            // fall-through
            case MMS_ALL:
            case MMS_INBOX:
            case MMS_SENT:
            case MMS_DRAFTS:
            case MMS_OUTBOX:
                notify = true;
                table = TABLE_PDU;
                break;
            case MMS_MSG_PART:
            case MMS_PART_ID:
                /// M: Code analyze 004, fix bug ALPS00249113, if query result is not updated
                /// completely, the result will show abnormal. adjust notify time while
                /// store messages. @{
                if (values.containsKey("need_notify")){
                    notify = values.getAsBoolean("need_notify");
                }
                /// @}
                table = TABLE_PART;
                break;

            case MMS_PART_RESET_FILE_PERMISSION:
                String path = getContext().getDir("parts", 0).getPath() + '/' +
                        uri.getPathSegments().get(1);
                // Reset the file permission back to read for everyone but me.
                int result = FileUtils.setPermissions(path, 0644, -1, -1);
                if (LOCAL_LOGV) {
                    Log.d(TAG, "MmsProvider.update setPermissions result: " + result +
                            " for path: " + path);
                }
                return 0;

            default:
                Log.w(TAG, "Update operation for '" + uri + "' not implemented.");
                return 0;
        }
        /// M: Code analyze 004, fix bug ALPS00249113, if query result is not updated
        /// completely, the result will show abnormal. adjust notify time while
        /// store messages. @{
        if (values.containsKey("need_notify")){
            values.remove("need_notify");
        }
        /// @}
        String extraSelection = null;
        ContentValues finalValues;
        if (table.equals(TABLE_PDU)) {
            // Filter keys that we don't support yet.
            filterUnsupportedKeys(values);
            finalValues = new ContentValues(values);

            if (msgId != null) {
                extraSelection = Mms._ID + "=" + msgId;
                /// M: Code analyze 009, fix bug ALPS00293687, update thread table while update
                /// messages. @{
                SQLiteDatabase db = mOpenHelper.getWritableDatabase();
                Cursor cursor = db.query(table, new String[] {"thread_id"}, extraSelection, null, null, null, null);
                try {
                     if (cursor != null && cursor.getCount() > 0) {
                         if (cursor.moveToFirst()) {
                             oldThreadId = cursor.getLong(0);
                         }
                     } 
                } finally {
                    cursor.close();
                }
                /// @}
            }
        } else if (table.equals(TABLE_PART)) {
            finalValues = new ContentValues(values);

            switch (match) {
                case MMS_MSG_PART:
                    extraSelection = Part.MSG_ID + "=" + uri.getPathSegments().get(0);
                    break;
                case MMS_PART_ID:
                    extraSelection = Part._ID + "=" + uri.getPathSegments().get(1);
                    break;
                default:
                    break;
            }
        } else {
            return 0;
        }

        String finalSelection = concatSelections(selection, extraSelection);
        MmsLog.d(TAG, "getWritableDatabase");
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        MmsLog.d(TAG, "getWritableDatabase end");
        int count = db.update(table, finalValues, finalSelection, selectionArgs);
        if (notify && (count > 0)) {
            notifyUnread = false;
            notifyChange(uri);
        }
        /// M: Code analyze 009, fix bug ALPS00293687, update thread table while update messages. @{
        if (count > 0) {
            if (table.equals(TABLE_PDU)) {
                if (finalValues.containsKey(Mms.THREAD_ID)) {
                    long newThreadId = finalValues.getAsLong(Mms.THREAD_ID);
                    if (newThreadId != oldThreadId) {
                        MmsSmsDatabaseHelper.updateThread(db, oldThreadId);
                    }
                }
            }
        }
        /// @}
        return count;
    }
    /// M: Code analyze 010, useless, there is a new solution since android-4.0.1, in file
    /// TempFileProvider.java. TODO: the old solution involve Telephony.java and this file here,
    /// keyword is "scrapSpace", should be removed. @{
    private ParcelFileDescriptor getTempStoreFd() {
        String fileName = EncapsulatedTelephony.Mms.ScrapSpace.SCRAP_FILE_PATH;
        ParcelFileDescriptor pfd = null;

        try {
            File file = new File(fileName);

            /// M: make sure the path is valid and directories created for this file.
            File parentFile = file.getParentFile();
            if (!parentFile.exists() && !parentFile.mkdirs()) {
                Log.e(TAG, "[MmsProvider] getTempStoreFd: " + parentFile.getPath() +
                        "does not exist!");
                return null;
            }

            pfd = ParcelFileDescriptor.open(file,
                    ParcelFileDescriptor.MODE_READ_WRITE
                            | android.os.ParcelFileDescriptor.MODE_CREATE);
        } catch (Exception ex) {
            Log.e(TAG, "getTempStoreFd: error creating pfd for " + fileName, ex);
        }

        return pfd;
    }
    /// @}
    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        /** M: if the url is "content://mms/takePictureTempStore", then it means the requester
        * wants a file descriptor to write image data to.@{*/

        ParcelFileDescriptor fd = null;
        /// M: Code analyze 003, fix bug ALPS00313325, coding convention correction.
        /// TODO: Is this change necessary? this will lead to more diff blocks.
        int match = URI_MATCHER.match(uri);

        /// M: test Google patch JE Issue
        Log.d(TAG, "openFile: uri=" + uri + ", mode=" + mode + ", match=" + match);

        if (match != MMS_PART_ID && match != MMS_SCRAP_SPACE) {
            /// M: test Google patch JE Issue
            Log.v(TAG, "openFile " + "openFile return null");
            return null;
        }

        switch (match) {
            /// M: Code analyze 010, useless, there is a new solution since android-4.0.1, in file
            /// TempFileProvider.java. TODO: the old solution involve Telephony.java and this file here,
            /// keyword is "scrapSpace", should be removed. @{
            case MMS_SCRAP_SPACE:
                fd = getTempStoreFd();
                break;
            /// @}
            case MMS_PART_ID:
                // Verify that the _data path points to mms data
                Cursor c = query(uri, new String[]{"_data"}, null, null, null);
                int count = (c != null) ? c.getCount() : 0;
                if (count != 1) {
                    // If there is not exactly one result, throw an appropriate
                    // exception.
                    if (c != null) {
                        c.close();
                    }
                    if (count == 0) {
                        /// M: test Google patch JE Issue
                        Log.v(TAG, "openfile FileNotFoundException(No entry for)");
                        throw new FileNotFoundException("No entry for " + uri);
                    }
                    /// M: test Google patch JE Issue
                    Log.v(TAG, "openfile FileNotFoundException(Multiple items at)");
                    throw new FileNotFoundException("Multiple items at " + uri);
                }

                c.moveToFirst();
                int i = c.getColumnIndex("_data");
                String path = (i >= 0 ? c.getString(i) : null);
                c.close();

                if (path == null) {
                    /// M: test Google patch JE Issue
                    Log.v(TAG, "openfile path == null " + path);
                    //return null;
                }
                try {
                    File filePath = new File(path);
                    if (!filePath.getCanonicalPath()
                            .startsWith(getContext().getApplicationInfo().dataDir + "/app_parts/")) {
                        /// M: test Google patch JE Issue
                        Log.v(TAG, "openfile !filePath.getCanonicalPath().startsWith()");
                        //return null;
                    }
                } catch (IOException e) {
                    /// M: test Google patch JE Issue
                    Log.v(TAG, "openfile catch (IOException e)");
                    //return null;
                }
                fd = openFileHelper(uri, mode);
                if (fd == null) {
                    /// M: test Google patch JE Issue
                    Log.v(TAG, "openFile " + "MMS_PART_ID fd==null");
                }
                break;
        }
        return fd;
    }

    private void filterUnsupportedKeys(ContentValues values) {
        // Some columns are unsupported.  They should therefore
        // neither be inserted nor updated.  Filter them out.
        values.remove(Mms.DELIVERY_TIME_TOKEN);
        values.remove(Mms.SENDER_VISIBILITY);
        values.remove(Mms.REPLY_CHARGING);
        values.remove(Mms.REPLY_CHARGING_DEADLINE_TOKEN);
        values.remove(Mms.REPLY_CHARGING_DEADLINE);
        values.remove(Mms.REPLY_CHARGING_ID);
        values.remove(Mms.REPLY_CHARGING_SIZE);
        values.remove(Mms.PREVIOUSLY_SENT_BY);
        values.remove(Mms.PREVIOUSLY_SENT_DATE);
        values.remove(Mms.STORE);
        values.remove(Mms.MM_STATE);
        values.remove(Mms.MM_FLAGS_TOKEN);
        values.remove(Mms.MM_FLAGS);
        values.remove(Mms.STORE_STATUS);
        values.remove(Mms.STORE_STATUS_TEXT);
        values.remove(Mms.STORED);
        values.remove(Mms.TOTALS);
        values.remove(Mms.MBOX_TOTALS);
        values.remove(Mms.MBOX_TOTALS_TOKEN);
        values.remove(Mms.QUOTAS);
        values.remove(Mms.MBOX_QUOTAS);
        values.remove(Mms.MBOX_QUOTAS_TOKEN);
        values.remove(Mms.MESSAGE_COUNT);
        values.remove(Mms.START);
        values.remove(Mms.DISTRIBUTION_INDICATOR);
        values.remove(Mms.ELEMENT_DESCRIPTOR);
        values.remove(Mms.LIMIT);
        values.remove(Mms.RECOMMENDED_RETRIEVAL_MODE);
        values.remove(Mms.RECOMMENDED_RETRIEVAL_MODE_TEXT);
        values.remove(Mms.STATUS_TEXT);
        values.remove(Mms.APPLIC_ID);
        values.remove(Mms.REPLY_APPLIC_ID);
        values.remove(Mms.AUX_APPLIC_ID);
        values.remove(Mms.DRM_CONTENT);
        values.remove(Mms.ADAPTATION_ALLOWED);
        values.remove(Mms.REPLACE_ID);
        values.remove(Mms.CANCEL_ID);
        values.remove(Mms.CANCEL_STATUS);

        // Keys shouldn't be inserted or updated.
        values.remove(Mms._ID);
    }

    private void notifyChange(Uri uri) {
        getContext().getContentResolver().notifyChange(
                MmsSms.CONTENT_URI, null);
        getContext().getContentResolver().notifyChange(
                uri, null);
        /// M: Code analyze 002, fix bug ALPS00262044, not show out unread message
        /// icon after restore messages. notify mms application about unread messages
        /// number after insert operation.
        if (!notifyUnread){
            MmsLog.d(TAG, "MmsProvider NOTIFY_CHANGE");
            MmsSmsProvider.notifyUnreadMessageNumberChanged(getContext());
        }
    }
  
    private final static String TAG = "MmsProvider";
    private final static String VND_ANDROID_MMS = "vnd.android/mms";
    private final static String VND_ANDROID_DIR_MMS = "vnd.android-dir/mms";
    private final static boolean DEBUG = false;
    private final static boolean LOCAL_LOGV = false;

    private static final int MMS_ALL                      = 0;
    private static final int MMS_ALL_ID                   = 1;
    private static final int MMS_INBOX                    = 2;
    private static final int MMS_INBOX_ID                 = 3;
    private static final int MMS_SENT                     = 4;
    private static final int MMS_SENT_ID                  = 5;
    private static final int MMS_DRAFTS                   = 6;
    private static final int MMS_DRAFTS_ID                = 7;
    private static final int MMS_OUTBOX                   = 8;
    private static final int MMS_OUTBOX_ID                = 9;
    private static final int MMS_ALL_PART                 = 10;
    private static final int MMS_MSG_PART                 = 11;
    private static final int MMS_PART_ID                  = 12;
    private static final int MMS_MSG_ADDR                 = 13;
    private static final int MMS_SENDING_RATE             = 14;
    private static final int MMS_REPORT_STATUS            = 15;
    private static final int MMS_REPORT_REQUEST           = 16;
    private static final int MMS_DRM_STORAGE              = 17;
    private static final int MMS_DRM_STORAGE_ID           = 18;
    private static final int MMS_THREADS                  = 19;
    /// M: Code analyze 010, useless, there is a new solution since android-4.0.1, in file
    /// TempFileProvider.java. TODO: the old solution involve Telephony.java and this file here,
    /// keyword is "scrapSpace", should be removed.
    private static final int MMS_SCRAP_SPACE              = 20;
    /// M: Code analyze 005, fix bug ALPS00275452, count the attachments size.
    private static final int MMS_ATTACHMENT_SIZE          = 21;
    /// M: Code analyze 011, useless, this value have changed from 20 to 22, because 20 is in used
    /// by MMS_SCRAP_SPACE. TODO: revert it after MMS_SCRAP_SPACE is removed.
    private static final int MMS_PART_RESET_FILE_PERMISSION = 22;
    /// M: Code analyze 003, fix bug ALPS00313325, coding convention correction.
    /// TODO: Is this change necessary? this will lead to more diff blocks.
    private static final UriMatcher
            URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
    /// M: Code analyze 003, fix bug ALPS00313325, coding convention correction.
    /// TODO: Is this change necessary? this will lead to more diff blocks. "URI_MATCHER".@{
    static {
        URI_MATCHER.addURI("mms", null,         MMS_ALL);
        URI_MATCHER.addURI("mms", "#",          MMS_ALL_ID);
        URI_MATCHER.addURI("mms", "inbox",      MMS_INBOX);
        URI_MATCHER.addURI("mms", "inbox/#",    MMS_INBOX_ID);
        URI_MATCHER.addURI("mms", "sent",       MMS_SENT);
        URI_MATCHER.addURI("mms", "sent/#",     MMS_SENT_ID);
        URI_MATCHER.addURI("mms", "drafts",     MMS_DRAFTS);
        URI_MATCHER.addURI("mms", "drafts/#",   MMS_DRAFTS_ID);
        URI_MATCHER.addURI("mms", "outbox",     MMS_OUTBOX);
        URI_MATCHER.addURI("mms", "outbox/#",   MMS_OUTBOX_ID);
        URI_MATCHER.addURI("mms", "part",       MMS_ALL_PART);
        URI_MATCHER.addURI("mms", "#/part",     MMS_MSG_PART);
        URI_MATCHER.addURI("mms", "part/#",     MMS_PART_ID);
        URI_MATCHER.addURI("mms", "#/addr",     MMS_MSG_ADDR);
        URI_MATCHER.addURI("mms", "rate",       MMS_SENDING_RATE);
        URI_MATCHER.addURI("mms", "report-status/#",  MMS_REPORT_STATUS);
        URI_MATCHER.addURI("mms", "report-request/#", MMS_REPORT_REQUEST);
        URI_MATCHER.addURI("mms", "drm",        MMS_DRM_STORAGE);
        URI_MATCHER.addURI("mms", "drm/#",      MMS_DRM_STORAGE_ID);
        URI_MATCHER.addURI("mms", "threads",    MMS_THREADS);
        /// M: Code analyze 010, useless, there is a new solution since android-4.0.1, in file
        /// TempFileProvider.java. TODO: the old solution involve Telephony.java and this file here,
        /// keyword is "scrapSpace", should be removed.
        URI_MATCHER.addURI("mms", "scrapSpace", MMS_SCRAP_SPACE);
        /// M: Code analyze 005, fix bug ALPS00275452, count the attachments size.
        URI_MATCHER.addURI("mms", "attachment_size", MMS_ATTACHMENT_SIZE);
        URI_MATCHER.addURI("mms", "resetFilePerm/*",    MMS_PART_RESET_FILE_PERMISSION);
    }
    /// @}
    private SQLiteOpenHelper mOpenHelper;

    private static String concatSelections(String selection1, String selection2) {
        if (TextUtils.isEmpty(selection1)) {
            return selection2;
        } else if (TextUtils.isEmpty(selection2)) {
            return selection1;
        } else {
            return selection1 + " AND " + selection2;
        }
    }
    /// M: Code analyze 005, fix bug ALPS00275452, count the attachments size. @{
    /// M: get the attachment size
    private long getAttachmentsSize() {
        String[] projs = new String[] {
            Mms.Part._DATA
        };
        // TODO: is there a predefined Uri like Mms.CONTENT_URI?
        final Uri part = Uri.parse("content://mms/part/");
        Cursor cursor = getContext().getContentResolver().query(part, projs, null, null, null);
        long size = 0;
        try {
            if (cursor == null || !cursor.moveToFirst()) {
                MmsLog.e(TAG, "getAttachmentsSize, cursor is empty or null");
                return size;
            }
            MmsLog.d(TAG, "getAttachmentsSize, count " + cursor.getCount());
            do {
                final String data = cursor.getString(0);
                if (data != null) {
                    File file = new File(data);
                    if (file.exists()){
                        size += file.length();
                    }
                }
            } while (cursor.moveToNext());
        } finally {
            MmsLog.d(TAG, "getAttachmentsSize size = " + size);
            if (cursor != null) {
                cursor.close();
            }
        }
        return size;
    }
    /// @}

    /// M: for CMCC delete all messages, improve performance @{
    static int deleteAllMessages(Context context, SQLiteDatabase db,
            String selection, String[] selectionArgs, Uri uri) {
        MmsLog.d(TAG, "deleteAllMessages, start");
        Cursor cursor = db.query(TABLE_PDU, new String[] { Mms._ID, Mms.THREAD_ID},
                selection, selectionArgs, null, null, null);
        if (cursor == null) {
            return 0;
        }
        long thread_id = 0;
        try {
            if (cursor.getCount() == 0) {
                return 0;
            }
            Set<Long> ids = new HashSet<Long>();
            int count = 0;
            while (cursor.moveToNext()) {
                Long id = cursor.getLong(0);
                thread_id = cursor.getLong(1);
                ids.add(id);
                count++;
                if (count % 50 > 0 && !cursor.isLast()) {
                    continue;
                }
                String whereClause = Part.MSG_ID + formatInClause(ids);
                deleteAllParts(db, whereClause, null);
                ids.clear();
                count = 0;
            }
        } finally {
            cursor.close();
        }
        Log.d(TAG, "deleteAllMessages, delete all parts end");
        int count = db.delete(TABLE_PDU, selection, selectionArgs);
        Log.d(TAG, "deleteAllMessages, delete pdu end");
        if (count > 0) {
            Intent intent = new Intent(Mms.Intents.CONTENT_CHANGED_ACTION);
            intent.putExtra(Mms.Intents.DELETED_CONTENTS, uri);
            if (LOCAL_LOGV) {
                Log.v(TAG, "Broadcasting intent: " + intent);
            }
            context.sendBroadcast(intent);
            /// M: Code analyze 008, fix bug ALPS00300658, it cost long time to update thread table
            /// after delete messages. only update one thread record after delete. @{
            /// M: update the thread which the pdu belonged
            if (thread_id > 0){
                MmsSmsDatabaseHelper.updateThread(db, thread_id);
            }
            /// @}
        }
        return count;
    }

    private static String formatInClause(Set<Long> ids) {
        /* to IN sql */
        if (ids == null || ids.size() == 0) {
            return " IN ()";
        }
        String in = " IN ";
        in += ids.toString();
        in = in.replace('[', '(');
        in = in.replace(']', ')');
        return in;
    }

    private static int deleteAllParts(SQLiteDatabase db, String selection,
            String[] selectionArgs) {
        Cursor cursorPart = db.query(TABLE_PART, new String[] { "_data" },
                selection, selectionArgs, null, null, null);
        if (cursorPart == null) {
            return 0;
        }
        try {
            while (cursorPart.moveToNext()) {
                try {
                    // Delete the associated files saved on file-system.
                    String path = cursorPart.getString(0);
                    if (path != null) {
                        new File(path).delete();
                    }
                } catch (Throwable ex) {
                    Log.e(TAG, ex.getMessage(), ex);
                }
            }
        } finally {
            cursorPart.close();
        }
        Log.d(TAG, "deleteAllParts, delete all files end");
        return db.delete(TABLE_PART, selection, selectionArgs);
    }
    // @}
}

