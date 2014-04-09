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

package com.android.providers.telephony;

import android.content.ContentUris;
import android.content.Context;
import android.content.ContentValues;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.test.AndroidTestCase;
import android.util.Log;
import android.provider.BaseColumns;
import android.provider.Telephony.Sms;
import android.provider.Telephony.Mms;
import android.provider.Telephony.Threads;

import com.google.android.mms.pdu.PduParser;
import com.google.android.mms.pdu.PduPersister;
import com.google.android.mms.pdu.RetrieveConf;
import com.mediatek.encapsulation.android.telephony.EncapsulatedTelephony;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile; 

public class TelephonyTest extends AndroidTestCase {
    static final String TAG = "TelephonyTest";
    protected ContentResolver mResolver;
    //sms uri that will be used 
    private static final Uri SMS_ALL_URI = Uri.parse("content://sms/");
    private static final Uri SMS_UNDELIVERED  = Uri.parse("content://sms/undelivered");
    private static final Uri SMS_FAILED  = Uri.parse("content://sms/failed");
    private static final Uri SMS_CONVERSATIONS  = Uri.parse("content://sms/conversations");
    private static final Uri SMS_STATUS_PENDING   = Uri.parse("content://sms/sr_pending");
    private static final Uri SMS_ALL_THREADID   = Uri.parse("content://sms/all_threadid");
    private static final Uri SMS_UNREAD   = Uri.parse("content://mms-sms/unread_count");
    
    /** M: mms uri that will be used  */
    private static final Uri MMS_URI = Uri.parse("content://mms/");
    private static final Uri THREAD_ID_CONTENT_URI = Uri.parse("content://mms-sms/threadID");
    private static final Uri URI_CONVERSATIONS_MESSAGES = Uri.parse("content://mms-sms/conversations");
    private static final Uri URI_OBSOLETE_THREADS = Uri.parse("content://mms-sms/conversations/obsolete");
    private static final Uri URI_CANONICAL_ADDRESS = Uri.parse("content://mms-sms/canonical-address");
    private static final int TEST_SMS_COUNT = 50;
    private static final int TEST_MMS_COUNT = 5;
    
    /** M: one simple sms*/
    private static final String ADDRESS = "10086";
    private static final String DATE = "1326673820718";
    private static final String BODY = "hello, i'm e";
    private static final String UPDATE_BODY = "you are not e";
    private static final String SERVICE_CENTER = "+86138003800";
    /** M: for multi delete  */
    private static final String FOR_MULTIDELETE = "ForMultiDelete";
    /** M: for mms pdu, should push this file in this location first.  */
    private static final String DB_PATH = "/data/data/com.android.providers.telephony/databases/mms_test.zip";
    private static final String DB_NAME = "mms0.pdu";
    //global
    private static String insertSmsId; 
    private static Uri insertSmsUri;
    private static Uri insertMmsUri;
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // This code here is the code that was originally in ProviderTestCase2
        Log.d(TAG, "TelephonyTest setUp");
        mResolver = getContext().getContentResolver();
    }

    @Override
    protected void tearDown() throws Exception {
        Log.d(TAG, "TelephonyTest tearDown");
    }
    //insert a sms
    public void testCase01_insertSms() {
        Log.d(TAG, "testCase01");
        ContentValues values = getSmsValue(Sms.MESSAGE_TYPE_INBOX);
        insertSmsUri = insertSms(Sms.CONTENT_URI, values);
        assert(insertSmsUri != null);
        insertSmsId = insertSmsUri.getLastPathSegment();
        Log.d(TAG, "testCase01 end resultUri = " + insertSmsUri + " id= " + insertSmsId);
    }
    //query a sms
    public void testCase02_querySms() {
        Log.d(TAG, "testCase02");
        Cursor cursor = queryMsg(insertSmsUri, null);
        assertSms(cursor);
        Log.d(TAG, "testCase02 end ");
    }
    //update a sms
    public void testCase03_updateSms() {
        Log.d(TAG, "testCase03"); 
        updateSms(insertSmsUri);
        Log.d(TAG, "testCase03 end");
    }
    //delete a sms
    public void testCase04_deleteSms() {
        Log.d(TAG, "testCase04"); 
        int row = deleteSms(insertSmsId);
        assert(row > 0);
        Log.d(TAG, "testCase04 end deleteSms row = " + row);
    }
    
    //test sms in inbox
    public void testCase05_inboxSms() {
        Log.d(TAG, "testCase05");
        testSmsBox(Sms.MESSAGE_TYPE_INBOX, Sms.Inbox.CONTENT_URI);
        Log.d(TAG, "testCase05 end");
    }
    
    //test sms in sentbox
    public void testCase06_sentSms() {
        Log.d(TAG, "testCase06");
        testSmsBox(Sms.MESSAGE_TYPE_SENT, Sms.Sent.CONTENT_URI);
        Log.d(TAG, "testCase06 end");
    }
    
    //test sms in outbox
    public void testCase07_outboxSms() {
        Log.d(TAG, "testCase07");
        testSmsBox(Sms.MESSAGE_TYPE_OUTBOX, Sms.Outbox.CONTENT_URI);
        Log.d(TAG, "testCase07 end");
    }
    
    //test sms in draftbox
    public void testCase08_draftboxSms() {
        Log.d(TAG, "testCase08");
        testSmsBox(Sms.MESSAGE_TYPE_DRAFT, Sms.Draft.CONTENT_URI);
        Log.d(TAG, "testCase08 end");
    }
     
    //test sms in failedbox
    public void testCase09_failedboxSms() {
        Log.d(TAG, "testCase9");
        testSmsBox(Sms.MESSAGE_TYPE_FAILED, SMS_FAILED);
        Log.d(TAG, "testCase9 end");
    }
    //after inserting a sms a thread will be created
    public void testCase10_conversationSms() {
        Log.d(TAG, "testCase10");
        ContentValues values = getSmsValue(Sms.MESSAGE_TYPE_INBOX);
        insertSmsUri = insertSms(Sms.CONTENT_URI, values);
        assert(insertSmsUri != null);
        long threadid = Threads.getOrCreateThreadId(getContext(), ADDRESS);
        Uri.Builder uriBuilder = THREAD_ID_CONTENT_URI.buildUpon();
        uriBuilder.appendQueryParameter("recipient", ADDRESS);
        Uri uri = uriBuilder.build();
        Cursor cur = queryMsg(uri, null);
        try {
            assert(cur != null && cur.getCount() > 0);
        } finally {
            cur.close();
        }
        deleteMsg(insertSmsUri);
        Log.d(TAG, "testCase10 end");
    }
    //test unread message
    public void testCase11_unreadSms() {
        Log.d(TAG, "testCase11");
        Cursor cur = queryMsg(SMS_UNREAD, null);
        int unread = 0;
        try {
            assert(cur != null && cur.getCount() > 0);
              if (cur.moveToFirst()){
                  unread = cur.getInt(0);
              }
        } finally {
            cur.close();
        }
        ContentValues values = getSmsValue(Sms.MESSAGE_TYPE_INBOX);
        insertSmsUri = insertSms(Sms.CONTENT_URI, values);
        assert(insertSmsUri != null);
        cur = queryMsg(SMS_UNREAD, null);
        int unreadAfter = 0;
        try {
            assert(cur != null && cur.getCount() > 0);
              if (cur.moveToFirst()){
                  unreadAfter = cur.getInt(0);
              }
        } finally {
            cur.close();
        }
        assert((unreadAfter - unread) == 1);
        Log.d(TAG, "testCase11 unread After= " + unreadAfter + " unread=" + unread);
        deleteMsg(insertSmsUri);
        Log.d(TAG, "testCase11 end");
    }
    /**
     *  M: for multi delete 
     */
    public void testCase12_multiDeletSms() {
        Log.d(TAG, "testCase12_multiDeletSms");
        ContentValues values = getSmsValue(Sms.MESSAGE_TYPE_INBOX);
        for(int i = 0; i < TEST_SMS_COUNT; i++){
           insertSms(Sms.CONTENT_URI, values);
        }
        Cursor cur = queryMsg(SMS_ALL_URI, new String[]{BaseColumns._ID});
        try {
            if (cur == null || cur.getCount() <= 0){
            	Log.w(TAG, "testCase12 cur == null || cur.getCount(");
            	return;
            }
            String[] whereArgs = new String[cur.getCount()];
            int index = 0;
            while(cur.moveToNext()) {
            	int id = cur.getInt(0);
            	whereArgs[index] = Integer.toString(id);
            	index++;
            }
            int res = deleteMsg(Sms.CONTENT_URI, FOR_MULTIDELETE, whereArgs);
            assert(res == TEST_SMS_COUNT);
            whereArgs = null;
        } finally {
            if (cur != null){
            	cur.close();
            }
        }
    }
    /**
     *  M: for insert a mms, now use persister to persist a mms pdu
     */
    public void testCase13_insertMms() {
        Log.d(TAG, "testCase_insertMms");
        /// M: fix zip file PIM issue
        // byte[] pduMms = readFileContent(DB_PATH, DB_NAME);
        byte[] pduMms = readFile("mmspdu");
        assert (pduMms != null);
        PduPersister persister = PduPersister.getPduPersister(mContext);
        RetrieveConf retrieveConf = null;
        try {
            retrieveConf = (RetrieveConf)new PduParser(pduMms).parse();
            insertMmsUri = persister.persist(retrieveConf, Mms.Inbox.CONTENT_URI, true, false, null);
            /** M: insertMmsUri is Mms.Inbox.CONTENT_URI append msgid*/
            assert (insertMmsUri != null);
            Log.d(TAG, "testCase_insertMms insertMmsUri = " + insertMmsUri);
        } catch (Exception e) {
            Log.d(TAG, "testCase_insertMms Exception e");
            e.printStackTrace();
        }
    }
    /**
     *  M: for query a mms, after persist a mms pdu.
     *  depend on testcase13.
     */
    public void testCase14_queryMms() {
        Log.d(TAG, "testCase14_queryMms");
        Cursor cur = queryMsg(insertMmsUri, new String[]{BaseColumns._ID});
        try {
            assert(cur != null);
            assert(cur.getCount() > 0);
        } finally {
            cur.close();
        }
    }
    /**
     *  M: for update a mms, after persist a mms pdu.
     *  depend on testcase13.
     */
    public void testCase15_updateMms() {
        Log.d(TAG, "testCase15_updateMms");
        int count = updateOneMms(insertMmsUri);
        assert (count ==1);
        Cursor cur = queryMsg(insertMmsUri, new String[]{Mms.READ, Mms.SEEN});
        try {
            assert(cur != null);
            assert(cur.getCount() > 0);
            assert(cur.getInt(0) == 1);
            assert(cur.getInt(1) == 1);
        } finally {
            cur.close();
        }
    }
    /**
     *  M: for update a mms, after persist a mms pdu.
     *  depend on testcase13.
     */
    public void testCase16_updatePart() {
        Log.d(TAG, "testCase16_updatePart");
        String id = insertMmsUri.getLastPathSegment();
        Uri partUri = Uri.parse("content://mms/" + id + "/part");
        int row = updatePart(partUri);
        assert(row > 0);
        Log.d(TAG, "testCase16_updatePart row = " + row);
    }
    /**
     *  M: for delete a mms, after persist a mms pdu.
     *  depend on testcase13.
     */
    public void testCase17_deletePart() {
        Log.d(TAG, "testCase17_deletePart");
        int row = deleteMsg(insertMmsUri);
        assert(row > 0);
        Log.d(TAG, "testCase17_deletePart row = " + row);
    }
    /**
     *  M: for delete all mms.
     */
    public void testCase18_deleteAllMms() {
        Log.d(TAG, "testCase18_deleteAllMms");
        for (int i = 0; i < TEST_MMS_COUNT; i++){
            testCase13_insertMms();
        }
        Cursor cur = queryMsg(MMS_URI, new String[]{BaseColumns._ID});
        try {
            if (cur == null || cur.getCount() <= 0){
                Log.w(TAG, "testCase18 cur == null || cur.getCount(");
                return;
            }
            String[] whereArgs = new String[cur.getCount()];
            int index = 0;
            while(cur.moveToNext()) {
                int id = cur.getInt(0);
                whereArgs[index] = Integer.toString(id);
                index++;
            }
            int row = deleteMsg(Mms.CONTENT_URI, FOR_MULTIDELETE, whereArgs);
            assert(row >= TEST_MMS_COUNT);
            Log.d(TAG, "testCase18_deleteAllMms row = " + row);
            whereArgs = null;
        } finally {
            if (cur != null){
                cur.close();
            }
        }
    }
    /**
     *  M: for test getOrcreateThreadId
     */
    public void testCase19_getOrCreateThreadId() {
        Log.d(TAG, "testCase19_getOrCreateThreadId");
        long threadid = Threads.getOrCreateThreadId(getContext(), ADDRESS);
        assert(threadid > 0);
        deleteMsg(ContentUris.withAppendedId(URI_CONVERSATIONS_MESSAGES, threadid));
        /** M: delete obsolete thread*/
        deleteMsg(URI_OBSOLETE_THREADS);
        
    }
    /**
     *  M: for update a mms, after persist a mms pdu.
     *  depend on testcase13.
     */
    protected void testSmsBox(int type, Uri box) {
         ContentValues values = getSmsValue(type);
         Uri uri = insertSms(Sms.CONTENT_URI, values);
         assert(uri != null);
         //query
         String id = uri.getLastPathSegment();
         assert(id != null && Long.parseLong(id) > 0);
         Uri uribox = ContentUris.withAppendedId(box, Long.parseLong(id));
         Cursor cursor = queryMsg(uribox, null);
         assert(cursor != null && cursor.getCount() > 0);
         cursor.close();
         //update
         updateSms(uribox);
         //delete
         int row = deleteMsg(uri);
         assert(row > 0);
    }
    
    protected Uri insertSms(Uri uri, ContentValues values) {
        Uri resultUri = mResolver.insert(uri, values);
        return resultUri;
    }
    
    protected Cursor queryMsg(Uri uri, String[] projection) {
        return mResolver.query(uri, projection, null, null, null);
    }
    
    protected Cursor queryMsg(Uri uri, long smsId) {
        return mResolver.query(ContentUris.withAppendedId(uri, smsId),
                null, null, null, null);
    }
    
    protected void updateSms(Uri uri) {
        int row = updateOneSms(uri);
        assert(row > 0);
        Cursor cursor = queryMsg(uri, new String[]{"body"});
        try {
            cursor.moveToFirst();
            String body = cursor.getString(cursor.getColumnIndexOrThrow(Sms.BODY));
            assertTrue(body.equals(UPDATE_BODY));
        } finally {
            if (cursor != null){
                cursor.close();
            }
        }
    }
    
    protected int updateOneSms(Uri uri) {
        String id = uri.getLastPathSegment();
        ContentValues values = new ContentValues();
        values.put(Sms.BODY, UPDATE_BODY);
        return mResolver.update(uri, values, "_id=" + id, null);
    }
    
    protected int updateOneMms(Uri uri) {
        ContentValues values = new ContentValues();
        values.put(Mms.READ, 1);
        values.put(Mms.SEEN, 1);
        return mResolver.update(uri, values, null, null);
    }
    
    protected int updatePart(Uri uri) {
        ContentValues values = new ContentValues();
        values.put(Mms.Part.CHARSET, 96);
        return mResolver.update(uri, values, null, null);
    }
    
    protected void assertSms(Cursor cursor) {
        try {
            assertTrue(cursor.moveToFirst()); 
            int threadId = cursor.getInt(cursor.getColumnIndexOrThrow("thread_id"));
            String address = cursor.getString(cursor.getColumnIndexOrThrow(Sms.ADDRESS));
            String body = cursor.getString(cursor.getColumnIndexOrThrow(Sms.BODY));
            Long dates = cursor.getLong(cursor.getColumnIndexOrThrow(Sms.DATE));
            int simId = cursor.getInt(cursor.getColumnIndexOrThrow(EncapsulatedTelephony.Sms.SIM_ID));
            int read = cursor.getInt(cursor.getColumnIndexOrThrow(Sms.READ));
            String serviceCenter = cursor.getString(cursor.getColumnIndexOrThrow(Sms.SERVICE_CENTER));
            assertTrue(threadId > 0); 
            assertTrue(address.equals(ADDRESS));
            assertTrue(dates == Long.parseLong(DATE)); 
            assertTrue(body.equals(BODY)); 
            assertTrue(simId == 2); 
            assertTrue(read == 0);
            assertTrue(serviceCenter.equals(SERVICE_CENTER));
        } finally {
            cursor.close();
        }
    }
    
    protected ContentValues getSmsValue(int boxType){
        ContentValues insertValues = new ContentValues();
        insertValues.put(Sms.ADDRESS, ADDRESS);
        insertValues.put(Sms.DATE, DATE);
        insertValues.put(Sms.BODY, BODY);
        insertValues.put(EncapsulatedTelephony.Sms.SIM_ID, 2);
        insertValues.put(Sms.TYPE, boxType);
        insertValues.put(Sms.SERVICE_CENTER, SERVICE_CENTER);
        insertValues.put(Sms.READ, 0);
        return insertValues;
    }
    
    protected int deleteSms(String id) {
        Uri deleteUri = ContentUris.withAppendedId(Sms.CONTENT_URI, Long.parseLong(id));
        int row = mResolver.delete(deleteUri, null, null);
        return row;
    }
    
    protected int deleteMsg(Uri deleteUri) {
        int row = mResolver.delete(deleteUri, null, null);
        return row;
    }
    
    protected int deleteMsg(Uri deleteUri, String selection, String[] args) {
        int row = mResolver.delete(deleteUri, selection, args);
        return row;
    }
    /**
     * M: read a mms pdu from zip file.
     * @param zipFileString zip file's name
     * @param fileString the db file's name
     * @return byte[] pdu
     */
    public static byte[] readFileContent(String zipFileString, String fileString){
        Log.v(TAG, "readFileContent ");
        ByteArrayOutputStream baos = null;
        try{
            ZipFile zipFile = new ZipFile(zipFileString);
            ZipEntry zipEntry = zipFile.getEntry(fileString);
            if (zipEntry != null){
                InputStream is = zipFile.getInputStream(zipEntry);      
                baos = new ByteArrayOutputStream(); 
                int len = -1;   
                byte[] buffer = new byte[512];
                while ((len = is.read(buffer,0, 512)) != -1){  
                    baos.write(buffer,0, len);
                }
            }
            
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return baos.toByteArray();
    }

    /// M: fix zip file PIM issue
    public byte[] readFile(String filePath) {
        Log.v(TAG, "readFile");
        ByteArrayOutputStream baos = null;
        Context context = getContext();
        try{
            baos = new ByteArrayOutputStream();
            int len = -1;
            byte[] buffer = new byte[512];
            FileInputStream words = context.openFileInput(filePath);
            while ((len = words.read(buffer,0, 512)) != -1){
                baos.write(buffer,0, len);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return baos.toByteArray();
    }
}
